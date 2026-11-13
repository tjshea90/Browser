package app.kestrel.browser.diag

import android.content.Context
import android.net.Uri
import app.kestrel.browser.Flavor
import app.kestrel.browser.data.TabStateStore
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.ui.LastLook
import app.kestrel.browser.ui.ThumbStore
import app.kestrel.browser.update.UpdateManager
import app.kestrel.core.download.DlState
import app.kestrel.core.session.LastLookRules
import app.kestrel.core.update.ApkFiles
import app.kestrel.core.update.UpdateLogic
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the tablet's storage from filling with things nobody needs any more (AO4, owner message 29: "make sure the app properly deletes old install files and apks ... Maintain the app cache and storage so
 * that very old or stale items don't waste storage. The cache and storage may be used freely for current browsing and speed, but it shouldn't waste storage on old items").
 *
 * What goes, and when it is looked at (about once a day, and at once after an update):
 *  - the update folder: everything except the installer of an update that is still ahead of the installed version; the installer is gone as soon as the new version runs
 *  - installers of THIS app that the browser downloaded into Downloads (a release APK opened from GitHub in a page) and that are the installed version or older
 *  - installer sessions Android still holds for this app from an install that was never finished
 *  - the engine's disk cache entries nobody has rewritten for [CACHE_DAYS] days, its dropped entries, and its crash reports older than [CRASH_DAYS] days
 *  - previews, saved page states and last-look pictures of tabs that no longer exist
 *  - failed or stopped downloads that have not moved for [PARTIAL_DAYS] days (with what they had already written)
 * What stays: everything a current page or a current download needs, bookmarks, history, logins and site data. The engine's cache is pruned BEFORE the engine starts ([pruneEngineFiles]); the rest runs off
 * the UI thread a little after the first frame.
 */
object StorageTidy {
    const val CACHE_DAYS = 21
    const val CRASH_DAYS = 7
    const val PARTIAL_DAYS = 14
    private const val EVERY_MS = 20L * 3600 * 1000
    private const val DAY_MS = 24L * 3600 * 1000

    @Volatile private var summary = ""
    @Volatile private var running = false

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("kestrel_tidy", Context.MODE_PRIVATE)

    /** The Diagnostics line. */
    fun line(ctx: Context): String {
        val last = sp(ctx).getLong("last", 0L)
        val s = summary.ifEmpty { sp(ctx).getString("summary", "") ?: "" }
        return "Storage tidy: " + (if (last == 0L) "not run yet" else "last " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(last)) + (if (s.isNotEmpty()) ": $s" else "")) +
            "; looks at update files, old installers, stale engine cache (over $CACHE_DAYS days), crash reports (over $CRASH_DAYS days), previews of closed tabs and stopped downloads (over $PARTIAL_DAYS days) about once a day and right after an update"
    }

    /** True when a tidy is due: a day since the last one, or the app is a different version than at the last one (an update was installed). */
    fun due(ctx: Context): Boolean {
        val p = sp(ctx)
        return System.currentTimeMillis() - p.getLong("last", 0L) >= EVERY_MS || p.getLong("code", 0L) != UpdateManager.installedCode(ctx)
    }

    // ---------------------------------------------------------------- before the engine starts

    /**
     * The engine's own files, tidied while it is NOT running (called from Application.onCreate on a background thread, before the runtime exists): cache entries not rewritten for [CACHE_DAYS] days, the
     * entries it dropped and crash reports older than [CRASH_DAYS] days. Returns the bytes freed. Only files are touched; the engine rebuilds its index around whatever is missing.
     */
    fun pruneEngineFiles(ctx: Context, now: Long = System.currentTimeMillis()): Long {
        val app = ctx.applicationContext
        var freed = 0L
        try {
            for (profile in app.cacheDir.listFiles() ?: emptyArray()) {
                val c2 = File(profile, "cache2")
                if (!c2.isDirectory) continue
                freed += deleteOlderThan(File(c2, "entries"), now - CACHE_DAYS * DAY_MS)
                freed += deleteOlderThan(File(c2, "doomed"), Long.MAX_VALUE)
                for (t in c2.listFiles { f -> f.isDirectory && f.name.startsWith("trash") } ?: emptyArray()) freed += deleteOlderThan(t, Long.MAX_VALUE)
            }
            val moz = File(app.filesDir, "mozilla")
            val cutoff = now - CRASH_DAYS * DAY_MS
            freed += deleteOlderThan(File(moz, "Crash Reports"), cutoff, recurse = true)
            for (profile in moz.listFiles() ?: emptyArray()) if (profile.isDirectory && profile.name != "Crash Reports" && profile.name != "Pending Pings") {
                freed += deleteOlderThan(File(profile, "minidumps"), cutoff)
                freed += deleteOlderThan(File(profile, "crashes"), cutoff, recurse = true)
            }
        } catch (e: Exception) { /* a tidy never breaks the start */ }
        return freed
    }

    /** Deletes the regular files in [dir] (and below it when [recurse]) whose last change is before [cutoff]; returns the bytes freed. */
    internal fun deleteOlderThan(dir: File, cutoff: Long, recurse: Boolean = false): Long {
        var freed = 0L
        val kids = dir.listFiles() ?: return 0L
        for (k in kids) {
            if (k.isDirectory) { if (recurse) freed += deleteOlderThan(k, cutoff, true); continue }
            if (k.lastModified() < cutoff) { val n = k.length(); if (k.delete()) freed += n }
        }
        return freed
    }

    // ---------------------------------------------------------------- the rest (browser running)

    /**
     * Runs the tidy if it is due (or [force]d); off the UI thread. [liveTabs]: the ids of the tabs that exist (the pictures and saved states of any other id are stale); null leaves those alone.
     * Returns the plain sentence it recorded, or null when nothing was due or another run is going.
     */
    fun runIfDue(ctx: Context, liveTabs: Set<Long>?, force: Boolean = false): String? {
        val app = ctx.applicationContext
        if (!force && !due(app)) return null
        if (running) return null
        running = true
        try {
            val after = sp(app).getLong("code", 0L).let { it != 0L && it != UpdateManager.installedCode(app) }
            val parts = LinkedHashMap<String, Long>()
            fun add(what: String, bytes: Long) { if (bytes > 0) parts[what] = (parts[what] ?: 0L) + bytes }
            add("update files", UpdateManager.sweepFiles(app))
            add("old installers", sweepInstallers(app))
            val sessions = abandonStaleInstallSessions(app)
            if (liveTabs != null) add("previews of closed tabs", sweepTabFiles(app, liveTabs))
            add("stopped downloads", sweepStoppedDownloads(app))
            add("engine cache", StorageInfo.purgeDoomedCache(app))
            val total = parts.values.sum()
            val text = (if (after) "after an update, " else "") + if (parts.isEmpty() && sessions == 0) "nothing to remove" else
                "freed " + UpdateLogic.formatSize(total) + (if (parts.isEmpty()) "" else " (" + parts.entries.joinToString(", ") { it.key + " " + UpdateLogic.formatSize(it.value) } + ")") + (if (sessions > 0) ", $sessions unfinished install(s) abandoned" else "")
            summary = text
            sp(app).edit().putLong("last", System.currentTimeMillis()).putLong("code", UpdateManager.installedCode(app)).putString("summary", text).apply()
            Health.log("storage", "tidy", text)
            return text
        } catch (e: Exception) {
            Health.log("storage", "tidy-failed", e.toString().take(100))
            return null
        } finally { running = false }
    }

    /** Installers of THIS app that the browser itself downloaded into Downloads and that are the installed version or older. Returns the bytes freed. */
    private fun sweepInstallers(app: Context): Long {
        var freed = 0L
        val prefix = Flavor.updateTagPrefix
        val installedName = UpdateManager.installedName(app)
        val installedCode = UpdateManager.installedCode(app)
        for (v in DownloadController.views()) {
            if (v.state != DlState.COMPLETED || v.priv) continue
            val apk = v.name.endsWith(".apk", true) || v.mime == "application/vnd.android.package-archive"
            if (!apk) continue
            val byName = ApkFiles.isUsedUp(v.name, prefix, installedName)
            val byContent = !byName && v.destUri != null && ApkFiles.tagOf(v.name, prefix) == null && archiveCodeOfThisApp(app, v.destUri!!)?.let { it <= installedCode } == true
            if (byName || byContent) { DownloadController.remove(v.id, true); freed += v.total.coerceAtLeast(0L) }
        }
        return freed
    }

    /** The version code of the installer behind [uri] when it is an installer of THIS app (read from the file itself, so a renamed copy is recognised); null otherwise. */
    private fun archiveCodeOfThisApp(app: Context, uri: String): Long? = try {
        app.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
            val pi = app.packageManager.getPackageArchiveInfo("/proc/self/fd/" + pfd.fd, 0)
            if (pi != null && pi.packageName == app.packageName) pi.longVersionCode else null
        }
    } catch (e: Exception) { null }

    /** Install sessions Android still holds for this app (an install that was never finished keeps its staged copy): abandoned unless an install is in flight. */
    private fun abandonStaleInstallSessions(app: Context): Int {
        if (UpdateManager.busy()) return 0
        var n = 0
        try {
            val pi = app.packageManager.packageInstaller
            for (s in pi.mySessions) if (!s.isActive && !UpdateManager.pending()) { pi.abandonSession(s.sessionId); n++ }
        } catch (e: Exception) { /* not allowed or gone: nothing to free */ }
        return n
    }

    /** Previews, saved page states and last-look pictures of tabs that are not open any more. Returns the bytes freed. */
    private fun sweepTabFiles(app: Context, live: Set<Long>): Long {
        var freed = 0L
        freed += retainOnly(File(app.cacheDir, "thumbs"), live)
        freed += retainOnly(TabStateStore.dir(app), live, keepNames = setOf("engine.txt"))
        freed += retainOnly(File(app.filesDir, "lastlook"), LastLookRules.keep(live.firstOrNull() ?: -1L, live.associateWith { 0L }).let { live })
        return freed
    }

    /** Deletes the files of [dir] named `<id>.<ext>` whose id is not in [live] (anything else in it too, except [keepNames]). */
    internal fun retainOnly(dir: File, live: Set<Long>, keepNames: Set<String> = emptySet()): Long {
        var freed = 0L
        for (f in dir.listFiles() ?: return 0L) {
            if (f.name in keepNames || f.isDirectory) continue
            val id = f.name.substringBefore('.').toLongOrNull()
            if (id == null || id !in live) { val n = f.length(); if (f.delete()) freed += n }
        }
        return freed
    }

    /** Failed or stopped downloads nobody has touched for [PARTIAL_DAYS] days: the record goes with what was written. Returns the bytes freed. */
    private fun sweepStoppedDownloads(app: Context): Long {
        var freed = 0L
        val cutoff = System.currentTimeMillis() - PARTIAL_DAYS * DAY_MS
        for (v in DownloadController.views()) {
            val stopped = v.state == DlState.FAILED || v.state == DlState.PAUSED_USER || v.state == DlState.PAUSED_SYSTEM || v.state == DlState.WAITING_NETWORK || v.state == DlState.NEEDS_DECISION
            if (!stopped || v.updated <= 0 || v.updated > cutoff) continue
            DownloadController.remove(v.id, true); freed += v.done.coerceAtLeast(0L)
        }
        return freed
    }
}
