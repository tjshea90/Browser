package app.kestrel.browser.update

import android.app.PendingIntent
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.os.storage.StorageManager
import android.os.Build
import android.provider.Settings
import app.kestrel.browser.Flavor
import app.kestrel.browser.Io
import app.kestrel.core.download.Source
import app.kestrel.core.download.Format
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.ProcessLife
import app.kestrel.core.update.Release
import app.kestrel.core.update.ReleaseAsset
import app.kestrel.core.update.UpdateLogic
import app.kestrel.core.update.UpdateOffer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.concurrent.CopyOnWriteArrayList
import javax.net.ssl.SSLException

/**
 * In-app updates from GitHub Releases (owner request: "when github makes a new release the app itself will see it and I can update the browser inside of the app").
 *
 * Check: ONE small conditional request to the Releases API when the app opens, at most once a day (an ETag makes "nothing new" a 304 with no body), or when the user
 * taps Check now. Nothing runs in the background, so the metered hotspot is never touched behind the owner's back.
 * Download: only after the user taps it (the dialog states the size, and that the link is metered). Resumable (HTTP Range), checked against the SHA-256 that the
 * release itself publishes, kept in the app's private storage.
 * Install: through Android's PackageInstaller, which only accepts an APK signed with the SAME certificate as the installed app (the pinned key in signing/CERT_SHA256),
 * so even a tampered download could not replace the app. Android asks the user to confirm; "Install unknown apps" must be allowed for this app once.
 * Pure decisions (which release, checksum, resume) live in core/UpdateLogic and are unit-tested.
 */
object UpdateManager {
    enum class Phase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, INSTALLING, NEEDS_PERMISSION, NEEDS_SPACE, ERROR }

    class State(
        val phase: Phase,
        val offer: UpdateOffer? = null,
        val done: Long = 0,
        val total: Long = 0,
        val message: String = "",
        val checkedAt: Long = 0,
        val installedName: String = "",
        /**
         * NEEDS_SPACE only: bytes still to free (rounded up to 10 MB); the free space the installer can count on now (free + caches it may clear); whether the downloaded file is still there;
         * what the install itself takes (APK + unpacked libraries + optimised code); and the part of the storage Android insists on keeping free afterwards (10 % of the partition).
         */
        val shortBy: Long = 0,
        val freeNow: Long = 0,
        val fileKept: Boolean = true,
        val installedBytes: Long = 0,
        val reserveBytes: Long = 0,
    )

    @Volatile var state = State(Phase.IDLE)
        private set

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    @Volatile private var appCtx: Context? = null
    private var ready = false

    private const val DAY_MS = 24L * 3600 * 1000
    private const val API = "https://api.github.com/repos/tjshea90/Browser"
    const val ACTION_RESULT = "app.kestrel.browser.update.RESULT"

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("kestrel_update", Context.MODE_PRIVATE)
    private fun dir(ctx: Context) = File(ctx.applicationContext.filesDir, "updates")

    fun installedCode(ctx: Context): Long = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode } catch (e: Exception) { 0L }
    fun installedName(ctx: Context): String = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }

    private fun set(ctx: Context?, phase: Phase, offer: UpdateOffer? = state.offer, done: Long = 0, total: Long = 0, message: String = "", checkedAt: Long = state.checkedAt) {
        state = State(phase, offer, done, total, message, checkedAt, state.installedName)
        Io.main.post { for (l in listeners) l() }
    }

    /** True while the user should be told about an update: a dot on the menu button. */
    fun pending(): Boolean = state.phase == Phase.AVAILABLE || state.phase == Phase.DOWNLOADING || state.phase == Phase.READY || state.phase == Phase.INSTALLING || state.phase == Phase.NEEDS_SPACE || state.phase == Phase.NEEDS_PERMISSION

    /** Restore what the last session knew (offer, last check) so the dot and the panel are right at once, without any network. */
    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        hookEngine(ctx.applicationContext)
        val s = sp(ctx)
        val installed = installedCode(ctx)
        val offer = s.getString("offer", null)?.let { offerFromJson(it) }
        val checked = s.getLong("checked", 0L)
        state = State(Phase.IDLE, null, 0, 0, "", checked, installedName(ctx))
        if (offer != null && offer.versionCode > installed) {
            val f = File(dir(ctx), offer.apkName)
            val isReady = f.isFile && f.length() == offer.size && s.getString("verified", null) == offer.tag
            set(ctx, if (isReady) Phase.READY else Phase.AVAILABLE, offer, if (isReady) offer.size else 0, offer.size, checkedAt = checked)
        } else {
            if (offer != null) { cleanup(ctx); s.edit().remove("floor").apply() } // the update was installed: drop the leftover APK and what we learned about this tablet's storage
            set(ctx, if (checked > 0) Phase.UP_TO_DATE else Phase.IDLE, null, checkedAt = checked)
        }
    }

    /** True while the process must stay alive for an update: a check or an install handshake is in flight (a download is counted by the download engine). */
    fun busy(): Boolean = state.phase == Phase.CHECKING || state.phase == Phase.INSTALLING

    /** Called when the app comes to the front: the once-a-day check, only if enabled, online, and nothing else is going on. */
    fun maybeAutoCheck(ctx: Context, enabled: Boolean, online: Boolean) {
        if (!enabled || !online) return
        val p = state.phase
        if (p == Phase.CHECKING || p == Phase.DOWNLOADING || p == Phase.INSTALLING) return
        if (System.currentTimeMillis() - state.checkedAt < DAY_MS) return
        check(ctx, manual = false)
    }

    /** DEBUG builds only: `adb shell setprop debug.kestrel.feed http://10.0.2.2:8000/releases.json` points the check at a local test feed (a releases LIST). */
    private fun debugFeed(ctx: Context): String? {
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null
        return try {
            (Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, "debug.kestrel.feed") as? String)?.takeIf { it.isNotBlank() }
        } catch (e: Throwable) { null }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Kestrel-updater/1") // GitHub requires one; it names the app, nothing about the user or device
    }

    private class RateLimited : IOException("rate limited")
    private class HttpError(val code: Int) : IOException("HTTP $code")

    /**
     * GET with the ETag we stored last time: a 304 answers from the stored body, so "nothing new" costs headers only on the metered link (and a 304 is not counted against GitHub's request limit).
     * null = 404 (a tag that has no release). Only a few small bodies are kept (the tag list and the newest releases).
     */
    private fun getCached(app: Context, url: String): String? {
        val s = sp(app)
        val key = java.security.MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }.take(12)
        val etag = s.getString("e.$key", null)
        val old = s.getString("b.$key", null)
        val c = open(url)
        c.setRequestProperty("Accept", "application/vnd.github+json")
        if (etag != null && old != null) c.setRequestProperty("If-None-Match", etag)
        try {
            when (val code = c.responseCode) {
                304 -> return old
                200 -> {
                    val text = c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                    val tag = c.getHeaderField("ETag")
                    if (tag != null && text.length < 300_000) {
                        val keys = (s.getString("cache_keys", "") ?: "").split(',').filter { it.isNotEmpty() && it != key }.toMutableList()
                        keys.add(key)
                        val ed = s.edit().putString("e.$key", tag).putString("b.$key", text)
                        while (keys.size > 12) { val drop = keys.removeAt(0); ed.remove("e.$drop").remove("b.$drop") }
                        ed.putString("cache_keys", keys.joinToString(",")).apply()
                    }
                    return text
                }
                404 -> return null
                403, 429 -> throw RateLimited()
                else -> throw HttpError(code)
            }
        } finally { c.disconnect() }
    }

    /**
     * The releases that can be the newest one of THIS app. GitHub's release list is NOT in date or version order (for this repository it is sorted by tag NAME: alpha9, alpha8, ... alpha2, alpha13,
     * alpha12, alpha11, alpha10), and this used to read only its first 8 entries - so from alpha10 on the newest release was never in them and every version said "up to date" (owner report X1).
     * Now: the tag list (small, ETag-cached), the newest few tags in natural version order, one conditional request per tag; if the tag list is unavailable, the whole list (100 per page).
     * Which release is offered is still decided by [UpdateLogic.choose] from the versionCode each release states, not by any order.
     */
    private fun fetchReleases(app: Context): List<Release> {
        val override = debugFeed(app)
        if (override != null) return parseReleases(getCached(app, override) ?: "[]")
        val prefix = Flavor.updateTagPrefix
        val tags = try {
            UpdateLogic.newestTags(UpdateLogic.tagsFromRefs(getCached(app, "$API/git/matching-refs/tags/$prefix") ?: "[]"), prefix, 4)
        } catch (e: RateLimited) { throw e } catch (e: HttpError) { emptyList() }
        val out = ArrayList<Release>()
        for (t in tags) {
            val body = getCached(app, "$API/releases/tags/$t") ?: continue
            out.addAll(parseReleases("[$body]"))
        }
        if (out.isNotEmpty()) return out
        return parseReleases(getCached(app, "$API/releases?per_page=100") ?: "[]")
    }

    fun check(ctx: Context, manual: Boolean) {
        val p = state.phase
        if (p == Phase.CHECKING || p == Phase.DOWNLOADING || p == Phase.INSTALLING) return
        val app = ctx.applicationContext
        set(app, Phase.CHECKING)
        Thread({
            try {
                val s = sp(app)
                val installed = installedCode(app)
                val now = System.currentTimeMillis()
                // recomputed from the releases against the code installed NOW (a stored answer goes stale when the app was updated or replaced by hand since)
                val offer = UpdateLogic.choose(fetchReleases(app), Flavor.updateTagPrefix, installed)
                s.edit().putString("offer", offer?.let { offerToJson(it) }).putLong("checked", now).apply()
                if (offer != null && offer.versionCode > installed) {
                    // a NEWER release than the file we hold (or are downloading): forget the old one
                    val heldTag = s.getString("job_tag", null)
                    if (heldTag != null && heldTag != offer.tag) { val id = jobId(app); if (id != 0L) { DownloadController.cancel(id); DownloadController.remove(id, true) }; s.edit().remove("job").remove("verified").remove("job_tag").apply(); cleanup(app) }
                    s.edit().putString("job_tag", offer.tag).apply()
                    val f = File(dir(app), offer.apkName)
                    val isReady = f.isFile && f.length() == offer.size && s.getString("verified", null) == offer.tag
                    set(app, if (isReady) Phase.READY else Phase.AVAILABLE, offer, if (isReady) offer.size else 0, offer.size, checkedAt = now)
                    val prefs = Prefs.get(app)
                    when {
                        isReady -> UpdateNotifier.ready(app, offer)
                        prefs.autoUpdateDownload -> download(app, auto = true)
                        !manual -> UpdateNotifier.available(app, offer)
                    }
                } else set(app, Phase.UP_TO_DATE, null, checkedAt = now)
            } catch (e: RateLimited) {
                fail(app, "GitHub is limiting requests right now. Try again in a while.")
            } catch (e: HttpError) {
                fail(app, "GitHub answered ${e.code}.")
            } catch (e: SSLException) {
                fail(app, "Could not verify GitHub's security certificate. This tablet's certificate list may be too old. Open the release page in the browser instead.")
            } catch (e: IOException) {
                fail(app, "No connection to GitHub.")
            } catch (e: Exception) {
                fail(app, "The update check failed (${e.javaClass.simpleName}).")
            }
        }, "kestrel-update-check").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
    }

    private fun fail(ctx: Context, message: String) {
        val o = state.offer
        set(ctx, Phase.ERROR, o, message = message)
    }

    // ------------------------------------------------------------------------------------------- download (the download engine + service do the work)

    private fun jobId(ctx: Context): Long = sp(ctx).getLong("job", 0L)
    private val verifying = java.util.concurrent.atomic.AtomicBoolean(false)
    private var syncHooked = false

    /**
     * Download the update through the app's one download engine (parallel segments, pause and resume, the foreground service with its wake and Wi-Fi locks: it keeps going with the
     * screen off and after the app is swiped away). [auto]: started by the app itself after a check (notification only, no sheet needed).
     */
    fun download(ctx: Context, auto: Boolean = false) {
        val offer = state.offer ?: return
        val p = state.phase
        if (p == Phase.DOWNLOADING || p == Phase.CHECKING || p == Phase.INSTALLING) return
        val app = ctx.applicationContext
        hookEngine(app)
        set(app, Phase.DOWNLOADING, offer, 0, offer.size)
        Thread({
            try {
                // Before spending the hotspot's data: will Android's installer find room for this update once the file is here? (the same numbers its own check uses)
                val old = jobId(app)
                val oldView = if (old != 0L) DownloadController.allViews().firstOrNull { it.id == old } else null
                val reuse = oldView != null && oldView.name == offer.apkName && oldView.state != DlState.FAILED && oldView.state != DlState.CANCELED && oldView.state != DlState.COMPLETED
                val sp0 = space(app, offer, null, pending = if (reuse) (offer.size - (oldView?.done ?: 0)).coerceAtLeast(0) else offer.size)
                if (sp0.shortBy > 0) { needSpace(app, offer, sp0, fileKept = false); if (auto) UpdateNotifier.needsSpace(app, UpdateLogic.roundUpTenMb(sp0.shortBy)); return@Thread }
                if (reuse) { DownloadController.resume(app, old); return@Thread } // paused or interrupted: continue where it stopped
                if (old != 0L) DownloadController.remove(old, true) // a finished / failed / cancelled one: forget it
                val src = Source(
                    url = offer.apkUrl, nameHint = offer.apkName, mime = "application/vnd.android.package-archive",
                    headers = mapOf("User-Agent" to "Kestrel-updater/1"), fetcher = 0,
                    meteredOk = true, // the owner's whole connection is a hotspot: he switched automatic updates on, or tapped Download himself
                    destKind = DEST_APP_PRIVATE,
                )
                DownloadController.enqueue(app, src) { id -> sp(app).edit().putLong("job", id).apply() }
            } catch (e: Exception) { fail(app, "The download could not start (${e.javaClass.simpleName}).") }
        }, "kestrel-update-download").apply { isDaemon = true }.start()
    }

    /** Follow the engine job: the sheet and the notifications show what the download engine reports. */
    private fun hookEngine(app: Context) {
        if (syncHooked) return
        syncHooked = true
        DownloadController.addListener { syncFromEngine(app) }
    }

    private fun syncFromEngine(app: Context) {
        val id = jobId(app)
        if (id == 0L) return
        val offer = state.offer ?: return
        val ph = state.phase
        if (ph == Phase.INSTALLING || ph == Phase.READY || ph == Phase.NEEDS_SPACE || ph == Phase.NEEDS_PERMISSION) return
        val v = DownloadController.allViews().firstOrNull { it.id == id } ?: return
        if (v.name != offer.apkName) return
        when (v.state) {
            DlState.QUEUED, DlState.STARTING, DlState.RUNNING, DlState.WAITING_NETWORK -> set(app, Phase.DOWNLOADING, offer, v.done, if (v.total > 0) v.total else offer.size)
            DlState.PAUSED_USER -> set(app, Phase.AVAILABLE, offer, v.done, offer.size)
            DlState.PAUSED_SYSTEM -> if (Prefs.get(app).autoUpdateDownload) DownloadController.resume(app, id) else set(app, Phase.AVAILABLE, offer, v.done, offer.size) // an interrupted download continues by itself
            DlState.VERIFYING -> set(app, Phase.DOWNLOADING, offer, v.total.coerceAtLeast(0), if (v.total > 0) v.total else offer.size)
            DlState.COMPLETED -> finishDownload(app, offer, id)
            DlState.FAILED -> { fail(app, Format.failure(v) + ". Tap Try again."); UpdateNotifier.failed(app, Format.failure(v)) }
            DlState.CANCELED -> set(app, Phase.AVAILABLE, offer, 0, offer.size)
            DlState.NEEDS_DECISION -> DownloadController.resume(app, id) // the owner asked for this download
        }
    }

    /** The engine wrote the whole file: check it against the checksum published with the release (off the UI thread: hashing 92 MB takes a few seconds). */
    private fun finishDownload(app: Context, offer: UpdateOffer, id: Long) {
        if (!verifying.compareAndSet(false, true)) return
        Thread({
            try {
                val f = File(dir(app), offer.apkName)
                if (!f.isFile || (offer.size > 0 && f.length() != offer.size)) { fail(app, "The downloaded file is incomplete. Tap Try again."); DownloadController.remove(id, true); return@Thread }
                val sum = sha256(f)
                if (offer.sha256 != null && sum != offer.sha256) {
                    DownloadController.remove(id, true)
                    fail(app, "The downloaded file does not match its published checksum, so it was deleted. Try again.")
                    UpdateNotifier.failed(app, "The file did not match its checksum")
                    return@Thread
                }
                sp(app).edit().putString("verified", offer.tag).apply()
                set(app, Phase.READY, offer, f.length(), f.length())
                UpdateNotifier.ready(app, offer)
                maybeInstallByItself(app)
            } finally { verifying.set(false) }
        }, "kestrel-update-verify").apply { isDaemon = true }.start()
    }

    /** Android 12+ may install an update without the confirmation when this app is its installer: let it, while nobody is looking at the app. Android 10 always asks (the notification carries that tap). */
    private fun maybeInstallByItself(app: Context) {
        if (Build.VERSION.SDK_INT < 31 || !Prefs.get(app).autoUpdateInstall || ProcessLife.uiVisible) return
        install(app)
    }

    /** Stop the download but keep what was fetched: Download continues from there. */
    fun pause() { val id = jobId(appCtx ?: return); if (id != 0L) DownloadController.pause(id) }

    fun discard(ctx: Context) {
        val offer = state.offer
        val id = jobId(ctx)
        if (id != 0L) { DownloadController.cancel(id); DownloadController.remove(id, true) }
        sp(ctx).edit().remove("verified").remove("job").apply()
        cleanup(ctx)
        if (offer != null) set(ctx, Phase.AVAILABLE, offer, 0, offer.size)
    }

    private fun cleanup(ctx: Context) { try { dir(ctx).listFiles()?.forEach { it.delete() } } catch (e: Exception) { /* ignore */ } }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ins -> val b = ByteArray(128 * 1024); while (true) { val n = ins.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    // ------------------------------------------------------------------------------------------- install

    /** Free space the installer can count on: what is free plus the caches the system may clear (installd clears every app's cache before it gives up). */
    private fun freeForInstall(ctx: Context): Long = try {
        val sm = ctx.getSystemService(StorageManager::class.java)
        ctx.getSystemService(StorageStatsManager::class.java).getFreeBytes(sm.getUuidForPath(dir(ctx)))
    } catch (e: Exception) { dir(ctx).usableSpace }

    /**
     * What Android insists on keeping free AFTER the install: 10 % of the data partition (measured, research/UPDATE_STORAGE.md: installd "Failed to free up 624066560 on /data" on a 6.2 GB partition).
     * The settings key is honoured when somebody raised it above 10 %.
     */
    private fun reserveBytes(ctx: Context): Long {
        val pct = try { Settings.Global.getInt(ctx.contentResolver, "sys_storage_cache_percentage", 10).coerceIn(10, 50) } catch (e: Exception) { 10 }
        return dir(ctx).totalSpace * pct / 100
    }

    /** Bytes the installer unpacks from the APK: read from the file when we have it, else estimated from the libraries of the app that is installed now. */
    private fun nativeLibBytes(ctx: Context, apk: File?): Long {
        if (apk != null && apk.isFile) try {
            ZipFile(apk).use { z ->
                var sum = 0L
                val en = z.entries()
                while (en.hasMoreElements()) { val e = en.nextElement(); if (!e.isDirectory && e.name.startsWith("lib/")) sum += e.size.coerceAtLeast(0) }
                return sum
            }
        } catch (e: Exception) { /* fall through to the estimate */ }
        return try { File(ctx.applicationInfo.nativeLibraryDir).listFiles()?.sumOf { it.length() } ?: 0L } catch (e: Exception) { 0L }
    }

    /** [floorShort]: what Android's own refusals still ask for beyond our estimate (see [installNeedsSpace]). */
    private class Space(val plan: UpdateLogic.StoragePlan, val free: Long, val installed: Long, val floorShort: Long) {
        val shortBy: Long get() = maxOf(plan.shortBy, floorShort)
    }

    /** [pending]: bytes of the update file that are still to be downloaded (not on the disk yet, but about to be). */
    private fun space(ctx: Context, offer: UpdateOffer, apk: File?, pending: Long = 0): Space {
        val apkBytes = if (apk != null && apk.isFile) apk.length() else offer.size.coerceAtLeast(0)
        val libs = nativeLibBytes(ctx, apk)
        val rawFree = freeForInstall(ctx)
        val free = (rawFree - pending).coerceAtLeast(0) // the download comes first and takes its room
        val floor = sp(ctx).getLong("floor2", 0L) // Android once refused with this much free space plus 150 MB: ask for at least that much
        val plan = UpdateLogic.storagePlan(apkBytes, libs, free, reserveBytes(ctx))
        return Space(plan, rawFree, plan.installTakes, (floor - free).coerceAtLeast(0))
    }

    private fun needSpace(ctx: Context, offer: UpdateOffer, sp: Space, fileKept: Boolean) {
        val short = UpdateLogic.roundUpTenMb(sp.shortBy).coerceAtLeast(10L shl 20)
        state = State(Phase.NEEDS_SPACE, offer, 0, 0, "", state.checkedAt, state.installedName, shortBy = short, freeNow = sp.free, fileKept = fileKept, installedBytes = sp.installed, reserveBytes = sp.plan.reserve)
        Io.main.post { for (l in listeners) l() }
    }

    /** The sheet opened (or the user came back from Storage settings): measure again, and drop the old "not enough space" the moment there is enough. */
    fun recheckSpace(ctx: Context) {
        if (state.phase != Phase.NEEDS_SPACE) return
        val offer = state.offer ?: return
        val app = ctx.applicationContext
        Thread({
            val apk = File(dir(app), offer.apkName)
            val kept = apk.isFile && sp(app).getString("verified", null) == offer.tag
            val partLen = jobId(app).let { id -> if (id == 0L) 0L else (DownloadController.allViews().firstOrNull { it.id == id }?.done ?: 0L) }
            val s = space(app, offer, if (kept) apk else null, pending = if (kept) 0 else (offer.size - partLen).coerceAtLeast(0))
            if (s.shortBy <= 0) set(app, if (kept) Phase.READY else Phase.AVAILABLE, offer, if (kept) offer.size else partLen, offer.size)
            else needSpace(app, offer, s, kept)
        }, "kestrel-update-space").apply { isDaemon = true }.start()
    }

    /** Hand the verified APK to Android's installer. Android asks the user to confirm and refuses anything not signed with this app's own certificate. */
    fun install(ctx: Context) {
        val offer = state.offer ?: return
        val app = ctx.applicationContext
        val apk = File(dir(app), offer.apkName)
        if (!apk.isFile) { fail(app, "The downloaded file is gone. Download it again."); return }
        if (!app.packageManager.canRequestPackageInstalls()) { set(app, Phase.NEEDS_PERMISSION, offer, offer.size, offer.size); return }
        set(app, Phase.INSTALLING, offer, offer.size, offer.size)
        Thread({
            var id = -1
            val pi = app.packageManager.packageInstaller
            try {
                // Will the installer find room? (staged copy + unpacked new version while the old one stays + its reserve). Too little: say how much, keep the file, start nothing.
                val sp0 = space(app, offer, apk)
                if (sp0.shortBy > 0) { needSpace(app, offer, sp0, fileKept = true); return@Thread }
                val dropOwnCopy = sp0.plan.fitsDropping // only enough room if our own copy goes once the installer has its own
                // Sessions left over from earlier tries each still hold a full staged copy of the APK: give that space back first.
                for (old in pi.mySessions) try { pi.abandonSession(old.sessionId) } catch (e: Exception) { /* already gone */ }
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(app.packageName)
                    setSize(apk.length())
                }
                id = pi.createSession(params)
                pi.openSession(id).use { session ->
                    session.openWrite("kestrel-update.apk", 0, apk.length()).use { out ->
                        apk.inputStream().use { it.copyTo(out, 128 * 1024) }
                        session.fsync(out)
                    }
                    if (dropOwnCopy) { apk.delete(); sp(app).edit().remove("verified").apply() } // frees one APK of room for the installer's own check
                    val intent = Intent(app, InstallResultReceiver::class.java).setAction(ACTION_RESULT)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                    session.commit(PendingIntent.getBroadcast(app, id, intent, flags).intentSender)
                }
            } catch (e: Exception) {
                if (id >= 0) try { pi.abandonSession(id) } catch (x: Exception) { /* ignore */ }
                val noRoom = e is IOException && (e.message?.contains("space", true) == true || e.message?.contains("ENOSPC", true) == true)
                if (noRoom) installNeedsSpace(app) else fail(app, "Could not start the installer (${e.javaClass.simpleName}: ${e.message?.take(80)}).")
            }
        }, "kestrel-update-install").apply { isDaemon = true }.start()
    }

    /**
     * From [InstallResultReceiver]: Android itself said INSTALL_FAILED_INSUFFICIENT_STORAGE (possibly although our estimate said there was room). Believe Android: remember
     * "it failed with this much allocatable space", ask for 150 MB MORE than that (a floor that only ever rises), keep the file for a retry.
     */
    fun installNeedsSpace(ctx: Context) {
        val offer = state.offer ?: return
        val app = ctx.applicationContext
        Thread({
            val now = freeForInstall(app)
            sp(app).edit().putLong("floor2", maxOf(sp(app).getLong("floor2", 0L), now) + (150L shl 20)).apply()
            val apk = File(dir(app), offer.apkName)
            val kept = apk.isFile
            val s0 = space(app, offer, if (kept) apk else null, pending = if (kept) 0 else offer.size)
            needSpace(app, offer, s0, kept)
        }, "kestrel-update-space").apply { isDaemon = true }.start()
    }

    /** From [InstallResultReceiver]: Android reported how the installation went. */
    fun installFailed(ctx: Context, message: String?) {
        fail(ctx.applicationContext, "Android could not install the update: ${message ?: "unknown reason"}.")
    }

    fun installCancelled(ctx: Context) {
        val o = state.offer ?: return
        val app = ctx.applicationContext
        val kept = File(dir(app), o.apkName).isFile
        set(app, if (kept) Phase.READY else Phase.AVAILABLE, o, if (kept) o.size else 0, o.size)
    }

    // ------------------------------------------------------------------------------------------- JSON

    internal fun parseReleases(json: String): List<Release> {
        val arr = JSONArray(json)
        val out = ArrayList<Release>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val assets = ArrayList<ReleaseAsset>()
            val aa = o.optJSONArray("assets")
            if (aa != null) for (j in 0 until aa.length()) {
                val a = aa.optJSONObject(j) ?: continue
                assets.add(ReleaseAsset(a.optString("name"), a.optString("browser_download_url"), a.optLong("size", -1), a.optString("digest").takeIf { it.isNotEmpty() && it != "null" }))
            }
            out.add(Release(o.optString("tag_name"), o.optString("name"), o.optString("body"), o.optBoolean("prerelease"), o.optBoolean("draft"), o.optString("html_url"), assets))
        }
        return out
    }

    private fun offerToJson(o: UpdateOffer): String = JSONObject()
        .put("tag", o.tag).put("vn", o.versionName).put("vc", o.versionCode).put("apk", o.apkName).put("url", o.apkUrl)
        .put("size", o.size).put("sha", o.sha256 ?: "").put("notes", o.notes).put("page", o.pageUrl).toString()

    private fun offerFromJson(s: String): UpdateOffer? = try {
        val j = JSONObject(s)
        UpdateOffer(j.getString("tag"), j.getString("vn"), j.getLong("vc"), j.getString("apk"), j.getString("url"), j.getLong("size"), j.optString("sha").takeIf { it.isNotEmpty() }, j.optString("notes"), j.optString("page"))
    } catch (e: Exception) { null }
}
