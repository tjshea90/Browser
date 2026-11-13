package app.kestrel.browser.diag

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import app.kestrel.browser.Flavor
import app.kestrel.browser.data.TabStateStore
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.ui.LastLook
import app.kestrel.browser.ui.ThumbStore
import app.kestrel.browser.update.UpdateManager
import app.kestrel.core.download.DlState
import app.kestrel.core.update.ApkFiles
import app.kestrel.core.update.UpdateLogic
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the tablet's storage from filling with things nobody needs any more (AO4, owner message 29: "make sure the app properly deletes old install files and apks from storage after updates and once the
 * update is installed ... Maintain the app cache and storage so that very old or stale items don't waste storage. The cache and storage may be used freely for current browsing and speed, but it
 * shouldn't waste storage on old items").
 *
 * What goes, and when it is looked at (about once a day, and at once after an update):
 *  - the update folder: everything except the installer of an update that is still ahead of the installed version; the installer is gone as soon as the new version runs
 *  - installers of THIS app in Downloads (a release APK the browser downloaded, or the copy "install the other way" saved) that are the installed version or older
 *  - installer sessions Android still holds for this app from an install that was never finished
 *  - the engine's disk cache entries nobody has rewritten for [CACHE_DAYS] days, its dropped entries, and its crash reports older than [CRASH_DAYS] days
 *  - previews, saved page states and last-look pictures of tabs that no longer exist
 *  - failed or stopped downloads that have not moved for [PARTIAL_DAYS] days (with what they had already written)
 * What stays: everything a current page or a current download needs, bookmarks, history, logins and site data. The engine's files are pruned as the engine starts ([pruneEngineFiles]); the rest
 * runs off the UI thread a little after the first frame ([runIfDue]).
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
            "; it looks at update files, old installers, engine cache older than $CACHE_DAYS days, crash reports older than $CRASH_DAYS days, previews of closed tabs and stopped downloads older than $PARTIAL_DAYS days, about once a day and right after an update"
    }

    /** True when a tidy is due: 20 hours since the last one, or the app is a different version than at the last one (an update was installed). */
    fun due(ctx: Context): Boolean {
        val p = sp(ctx)
        return System.currentTimeMillis() - p.getLong("last", 0L) >= EVERY_MS || p.getLong("code", 0L) != UpdateManager.installedCode(ctx)
    }

    // ---------------------------------------------------------------- the engine's own files

    /**
     * The engine's own files, tidied as the engine starts (off the UI thread, only when a tidy is due, so most starts pay nothing): cache entries not rewritten for [CACHE_DAYS] days, the entries it
     * dropped and crash reports older than [CRASH_DAYS] days. Returns the bytes freed. Only files are unlinked; the engine treats an entry that is gone as a miss and rebuilds its index around it.
     */
    fun pruneEngineFiles(ctx: Context, now: Long = System.currentTimeMillis()): Long {
        val app = ctx.applicationContext
        var freed = 0L
        try {
            for (c2 in cache2Dirs(app.cacheDir, 3)) {
                freed += deleteOlderThan(File(c2, "entries"), now - CACHE_DAYS * DAY_MS)
                freed += deleteOlderThan(File(c2, "doomed"), Long.MAX_VALUE)
                for (t in c2.listFiles { f -> f.isDirectory && f.name.startsWith("trash") } ?: emptyArray()) freed += deleteOlderThan(t, Long.MAX_VALUE, recurse = true)
            }
            val moz = File(app.filesDir, "mozilla")
            val cutoff = now - CRASH_DAYS * DAY_MS
            freed += deleteOlderThan(File(moz, "Crash Reports"), cutoff, recurse = true)
            for (profile in moz.listFiles() ?: emptyArray()) if (profile.isDirectory && profile.name != "Crash Reports") {
                freed += deleteOlderThan(File(profile, "minidumps"), cutoff, recurse = true)
                freed += deleteOlderThan(File(profile, "crashes"), cutoff, recurse = true)
            }
        } catch (e: Exception) { /* a tidy never breaks the start */ }
        if (freed > 0) Health.log("storage", "engine-files", "freed ${UpdateLogic.formatSize(freed)}")
        return freed
    }

    /** The engine's `cache2` folders under [dir], looked for [depth] levels down (its profile folder sits directly in the cache directory; this does not rely on the profile's name). */
    internal fun cache2Dirs(dir: File, depth: Int): List<File> {
        val out = ArrayList<File>()
        for (k in dir.listFiles() ?: return out) {
            if (!k.isDirectory) continue
            if (k.name == "cache2") out.add(k) else if (depth > 1) out.addAll(cache2Dirs(k, depth - 1))
        }
        return out
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
     * Runs the tidy when it is due (or [force]d). Call it off the UI thread. [liveTabs]: the ids of every tab that exists (the pictures and saved states of any other id are stale); null leaves those
     * alone (the tabs are not all restored yet, so the list would be incomplete). Returns the plain sentence it recorded, or null when nothing was due or another run is going.
     */
    fun runIfDue(ctx: Context, liveTabs: Set<Long>?, force: Boolean = false): String? {
        val app = ctx.applicationContext
        if (!force && !due(app)) return null
        synchronized(this) { if (running) return null; running = true }
        try {
            val after = sp(app).getLong("code", 0L).let { it != 0L && it != UpdateManager.installedCode(app) }
            val parts = LinkedHashMap<String, Long>()
            fun add(what: String, bytes: Long) { if (bytes > 0) parts[what] = (parts[what] ?: 0L) + bytes }
            add("update files", UpdateManager.sweepFiles(app))
            add("old installers", sweepInstallers(app))
            val savedLeft = IntArray(1)
            add("old installers", sweepSavedInstallers(app, savedLeft))
            val sessions = abandonStaleInstallSessions(app)
            if (liveTabs != null) add("previews of closed tabs", sweepTabFiles(app, liveTabs))
            add("stopped downloads", sweepStoppedDownloads())
            add("engine files", pruneEngineFiles(app))
            add("engine cache", StorageInfo.purgeDoomedCache(app))
            val total = parts.values.sum()
            val text = (if (after) "after an update, " else "") +
                (if (parts.isEmpty() && sessions == 0) "nothing to remove" else "freed " + UpdateLogic.formatSize(total) +
                    (if (parts.isEmpty()) "" else " (" + parts.entries.joinToString(", ") { it.key + " " + UpdateLogic.formatSize(it.value) } + ")") +
                    (if (sessions > 0) ", $sessions unfinished install(s) abandoned" else "")) +
                (if (savedLeft[0] > 0) "; ${savedLeft[0]} old installer(s) in Downloads were saved by an earlier install of this app and Android only lets that one delete them: remove them in Files" else "")
            summary = text
            sp(app).edit().putLong("last", System.currentTimeMillis()).putLong("code", UpdateManager.installedCode(app)).putString("summary", text).apply()
            Health.log("storage", "tidy", text)
            return text
        } catch (e: Exception) {
            Health.log("storage", "tidy-failed", e.toString().take(100))
            return null
        } finally { running = false }
    }

    /** Installers of THIS app that the browser downloaded itself (a release APK opened from a page) and that are the installed version or older: the download and its file go. Returns the bytes freed. */
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
            val uri = v.destUri
            val byContent = !byName && uri != null && ApkFiles.tagOf(v.name, prefix) == null && archiveCodeOfThisApp(app, uri)?.let { it <= installedCode } == true
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

    /**
     * Installers in the public Downloads folder that this app can see (on Android 10 only the ones it saved itself, such as the copy "install the other way" wrote) and that are named after this
     * app's release of the installed version or older. [couldNot][0] counts the ones Android would not let it delete (saved by an earlier install of the app). Returns the bytes freed.
     */
    private fun sweepSavedInstallers(app: Context, couldNot: IntArray): Long {
        var freed = 0L
        try {
            val prefix = Flavor.updateTagPrefix
            val installedName = UpdateManager.installedName(app)
            val resolver = app.contentResolver
            val base = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val used = ArrayList<Pair<Uri, Long>>()
            resolver.query(base, arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE), MediaStore.Downloads.DISPLAY_NAME + " LIKE ?", arrayOf("%.apk"), null)?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    if (ApkFiles.isUsedUp(name, prefix, installedName)) used.add(ContentUris.withAppendedId(base, c.getLong(0)) to c.getLong(2))
                }
            }
            for ((u, size) in used) {
                try { if (app.contentResolver.delete(u, null, null) > 0) freed += size.coerceAtLeast(0L) else couldNot[0]++ }
                catch (e: SecurityException) { couldNot[0]++ } // saved by an earlier install of this app: only that install (or the user, in Files) may delete it
            }
        } catch (e: Exception) { /* no media provider: nothing to look at */ }
        return freed
    }

    /**
     * Install sessions Android still holds for this app (an unfinished install keeps its staged copy of the whole APK): abandoned when no update is on offer or in flight. While one is on offer the updater
     * clears them itself at the start of every install attempt.
     */
    private fun abandonStaleInstallSessions(app: Context): Int {
        if (UpdateManager.busy() || UpdateManager.pending()) return 0
        var n = 0
        try {
            val pi = app.packageManager.packageInstaller
            for (s in pi.mySessions) if (!s.isActive) { pi.abandonSession(s.sessionId); n++ }
        } catch (e: Exception) { /* not allowed or already gone: nothing to free */ }
        return n
    }

    /** Previews, saved page states and last-look pictures of tabs that are not open any more. Returns the bytes freed. */
    private fun sweepTabFiles(app: Context, live: Set<Long>): Long {
        val dirs = listOf(File(app.cacheDir, "thumbs"), TabStateStore.dir(app), File(app.filesDir, "lastlook"))
        val before = dirs.sumOf { bytesIn(it) }
        ThumbStore.retain(app, live)
        TabStateStore.retain(app, live)
        LastLook.retain(app, live)
        return (before - dirs.sumOf { bytesIn(it) }).coerceAtLeast(0L)
    }

    internal fun bytesIn(dir: File): Long {
        var sum = 0L
        for (f in dir.listFiles() ?: return 0L) sum += if (f.isDirectory) bytesIn(f) else f.length()
        return sum
    }

    /** Failed or stopped downloads nobody has touched for [PARTIAL_DAYS] days: the record goes with what was written. Returns the bytes freed. */
    private fun sweepStoppedDownloads(): Long {
        var freed = 0L
        val cutoff = System.currentTimeMillis() - PARTIAL_DAYS * DAY_MS
        for (v in DownloadController.views()) {
            val stopped = v.state == DlState.FAILED || v.state == DlState.PAUSED_USER || v.state == DlState.PAUSED_SYSTEM || v.state == DlState.WAITING_NETWORK || v.state == DlState.NEEDS_DECISION
            if (!stopped || v.priv || v.updated <= 0 || v.updated > cutoff) continue
            DownloadController.remove(v.id, true); freed += v.done.coerceAtLeast(0L)
        }
        return freed
    }
}
