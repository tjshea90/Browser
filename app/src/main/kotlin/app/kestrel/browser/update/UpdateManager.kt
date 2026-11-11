package app.kestrel.browser.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.os.storage.StorageManager
import android.os.Build
import app.kestrel.browser.Flavor
import app.kestrel.browser.Io
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
    @Volatile private var cancelled = false
    private var ready = false

    private const val DAY_MS = 24L * 3600 * 1000
    private const val FEED = "https://api.github.com/repos/tjshea90/Browser/releases?per_page=8"
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
        val s = sp(ctx)
        val installed = installedCode(ctx)
        val offer = s.getString("offer", null)?.let { offerFromJson(it) }
        val checked = s.getLong("checked", 0L)
        state = State(Phase.IDLE, null, 0, 0, "", checked, installedName(ctx))
        if (offer != null && offer.versionCode > installed) {
            val f = File(dir(ctx), offer.apkName)
            val isReady = f.isFile && f.length() == offer.size && s.getString("verified", null) == offer.tag
            set(ctx, if (isReady) Phase.READY else Phase.AVAILABLE, offer, if (isReady) offer.size else File(dir(ctx), offer.apkName + ".part").length(), offer.size, checkedAt = checked)
        } else {
            if (offer != null) { cleanup(ctx); s.edit().remove("floor").apply() } // the update was installed: drop the leftover APK and what we learned about this tablet's storage
            set(ctx, if (checked > 0) Phase.UP_TO_DATE else Phase.IDLE, null, checkedAt = checked)
        }
    }

    /** Called when the app comes to the front: the once-a-day check, only if enabled, online, and nothing else is going on. */
    fun maybeAutoCheck(ctx: Context, enabled: Boolean, online: Boolean) {
        if (!enabled || !online) return
        val p = state.phase
        if (p == Phase.CHECKING || p == Phase.DOWNLOADING || p == Phase.INSTALLING) return
        if (System.currentTimeMillis() - state.checkedAt < DAY_MS) return
        check(ctx, manual = false)
    }

    private fun feedUrl(ctx: Context): String {
        // DEBUG builds only: `adb shell setprop debug.kestrel.feed http://10.0.2.2:8000/releases.json` points the check at a local test feed.
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            try {
                val v = Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, "debug.kestrel.feed") as? String
                if (!v.isNullOrBlank()) return v
            } catch (e: Throwable) { /* no override */ }
        }
        return FEED
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Kestrel-updater/1") // GitHub requires one; it names the app, nothing about the user or device
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
                val url = feedUrl(app)
                val c = open(url)
                c.setRequestProperty("Accept", "application/vnd.github+json")
                val etag = s.getString("etag", null)
                if (etag != null && s.getString("etag_url", null) == url) c.setRequestProperty("If-None-Match", etag)
                val code = c.responseCode
                val now = System.currentTimeMillis()
                var offer = s.getString("offer", null)?.let { offerFromJson(it) }
                when (code) {
                    304 -> { /* nothing new since the last answer: keep the stored offer */ }
                    200 -> {
                        val text = c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                        offer = UpdateLogic.choose(parseReleases(text), Flavor.updateTagPrefix, installed)
                        s.edit().putString("etag", c.getHeaderField("ETag")).putString("etag_url", url).putString("offer", offer?.let { offerToJson(it) }).apply()
                    }
                    403, 429 -> { fail(app, "GitHub is limiting requests right now. Try again in a while."); return@Thread }
                    else -> { fail(app, "GitHub answered $code."); return@Thread }
                }
                c.disconnect()
                s.edit().putLong("checked", now).apply()
                if (offer != null && offer.versionCode > installed) {
                    val f = File(dir(app), offer.apkName)
                    val isReady = f.isFile && f.length() == offer.size && s.getString("verified", null) == offer.tag
                    set(app, if (isReady) Phase.READY else Phase.AVAILABLE, offer, if (isReady) offer.size else File(dir(app), offer.apkName + ".part").length(), offer.size, checkedAt = now)
                } else set(app, Phase.UP_TO_DATE, null, checkedAt = now)
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

    // ------------------------------------------------------------------------------------------- download

    fun download(ctx: Context) {
        val offer = state.offer ?: return
        val p = state.phase
        if (p == Phase.DOWNLOADING || p == Phase.CHECKING || p == Phase.INSTALLING) return
        val app = ctx.applicationContext
        val d = dir(app); d.mkdirs()
        val part = File(d, offer.apkName + ".part")
        val done = File(d, offer.apkName)
        cancelled = false
        set(app, Phase.DOWNLOADING, offer, part.length(), offer.size)
        Thread({
            try {
                // Before spending the hotspot's data: will Android's installer find room for this update once the file is here? (the same numbers its own check uses)
                val sp0 = space(app, offer, null, pending = (offer.size - part.length()).coerceAtLeast(0))
                if (sp0.shortBy > 0) { needSpace(app, offer, sp0, fileKept = false); return@Thread }
                var have = part.length()
                if (have > offer.size) { part.delete(); have = 0 }
                val c = open(offer.apkUrl)
                c.setRequestProperty("Accept-Encoding", "identity")
                if (have > 0) c.setRequestProperty("Range", "bytes=$have-")
                val code = c.responseCode
                val rangeStart = c.getHeaderField("Content-Range")?.let { Regex("""bytes (\d+)-""").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                if (code !in 200..299) { fail(app, "The download was refused (HTTP $code)."); return@Thread }
                val append = UpdateLogic.canResume(have, code, rangeStart)
                if (!append) have = 0 // the server ignored the offset: start the file again
                val total = if (offer.size > 0) offer.size else (c.contentLengthLong.takeIf { it > 0 }?.let { it + have } ?: -1L)
                var lastTick = 0L
                FileOutputStream(part, append).use { out ->
                    c.inputStream.use { ins ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            if (cancelled) break
                            val n = ins.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            have += n
                            val t = System.nanoTime()
                            if (t - lastTick > 250_000_000L) { lastTick = t; set(app, Phase.DOWNLOADING, offer, have, total) }
                        }
                        out.fd.sync()
                    }
                }
                c.disconnect()
                if (cancelled) { set(app, Phase.AVAILABLE, offer, part.length(), offer.size); return@Thread }
                if (offer.size > 0 && part.length() != offer.size) { fail(app, "The download stopped early (${UpdateLogic.formatSize(part.length())} of ${UpdateLogic.formatSize(offer.size)}). Tap Download to continue."); return@Thread }
                val sum = sha256(part)
                if (offer.sha256 != null && sum != offer.sha256) {
                    part.delete()
                    fail(app, "The downloaded file does not match its published checksum, so it was deleted. Try again.")
                    return@Thread
                }
                done.delete()
                if (!part.renameTo(done)) { fail(app, "Could not store the downloaded file."); return@Thread }
                sp(app).edit().putString("verified", offer.tag).apply()
                set(app, Phase.READY, offer, done.length(), done.length())
            } catch (e: SSLException) {
                fail(app, "Could not verify the download server's security certificate. Open the release page in the browser instead.")
            } catch (e: IOException) {
                fail(app, "Connection lost. Tap Download to continue where it stopped.") // the partial file is kept
            } catch (e: Exception) {
                fail(app, "The download failed (${e.javaClass.simpleName}).")
            }
        }, "kestrel-update-download").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
    }

    /** Stop the download but keep what was fetched: Download continues from there. */
    fun pause() { cancelled = true }

    fun discard(ctx: Context) {
        val offer = state.offer
        cleanup(ctx)
        sp(ctx).edit().remove("verified").apply()
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
        val free = (freeForInstall(ctx) - pending).coerceAtLeast(0)
        val floor = sp(ctx).getLong("floor2", 0L) // Android once refused with this much free space plus 150 MB: ask for at least that much
        val plan = UpdateLogic.storagePlan(apkBytes, libs, free, reserveBytes(ctx))
        return Space(plan, free, plan.installTakes, (floor - free).coerceAtLeast(0))
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
            val partLen = File(dir(app), offer.apkName + ".part").length()
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
