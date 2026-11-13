package app.kestrel.browser.diag

import android.content.Context
import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
import app.kestrel.core.update.UpdateLogic
import java.io.File

/**
 * The numbers behind "the update will not install" (owner: updates never install over the old version): how much room the tablet has, how much Android's installer may use
 * (free space + caches it can clear - its reserve), and how big this app's own install, data and cache are. Read off the UI thread; shown in Diagnostics so a copied report has them.
 */
object StorageInfo {
    @Volatile var text: String = "Storage: (not read yet)"

    fun read(ctx: Context) {
        val app = ctx.applicationContext
        val f = UpdateLogic::formatSize
        text = try {
            val dir = app.filesDir
            val sm = app.getSystemService(StorageManager::class.java)
            val installerFree = try { app.getSystemService(StorageStatsManager::class.java).getFreeBytes(sm.getUuidForPath(dir)) } catch (e: Exception) { -1L }
            val apk = File(app.applicationInfo.sourceDir).length()
            val libs = try { File(app.applicationInfo.nativeLibraryDir).listFiles()?.sumOf { it.length() } ?: 0L } catch (e: Exception) { 0L }
            val files = size(app.filesDir)
            val cache = size(app.cacheDir)
            "Storage: ${f(dir.usableSpace)} free of ${f(dir.totalSpace)}; free + caches Android can clear: ${if (installerFree >= 0) f(installerFree) else "?"}; Android keeps ${f(dir.totalSpace / 10)} free after an install (a tenth); " +
                "this app: package ${f(apk)} + unpacked libraries ${f(libs)} + data ${f(files)} + cache ${f(cache)}; an install or update needs free >= reserve + 2 x package + libraries + about 48 MB. " + updateVerdict(app, apk, libs, if (installerFree >= 0) installerFree else dir.usableSpace, dir.totalSpace / 10)
        } catch (e: Exception) { "Storage: unavailable (${e.javaClass.simpleName})" }
    }

    /**
     * What an uninstall of this app hands back: [0] = package + unpacked libraries + optimised code + data + cache, [1] = the cache part of it. Android's own figures for this package
     * (an app may always ask about itself); zeros when it will not say.
     */
    fun ownFootprint(ctx: Context): LongArray = try {
        val app = ctx.applicationContext
        val sm = app.getSystemService(StorageManager::class.java)
        val st = app.getSystemService(StorageStatsManager::class.java).queryStatsForPackage(sm.getUuidForPath(app.filesDir), app.packageName, android.os.Process.myUserHandle())
        longArrayOf(st.appBytes + st.dataBytes, maxOf(st.cacheBytes, size(app.cacheDir))) // getDataBytes() already includes the cache; Android's cache figure lags (0 for a while after the engine filled it), the folder does not
    } catch (e: Exception) { longArrayOf(0L, 0L) }

    /** The same arithmetic as the Updates sheet, for an update the size of the installed package that is not downloaded yet: how far short the tablet is in place and after an uninstall. */
    private fun updateVerdict(app: Context, apk: Long, libs: Long, free: Long, reserve: Long): String {
        val f = UpdateLogic::formatSize
        val plan = UpdateLogic.storagePlan(apk, libs, (free - apk).coerceAtLeast(0L), reserve) // the download takes its room first
        val own = ownFootprint(app)
        val other = UpdateLogic.otherWayPlan(plan, own[0], apk, fileKept = false)
        return "An update of this size would need about ${f(plan.needDrop)} free: " + (if (plan.shortBy > 0) "this tablet is about ${f(plan.shortBy)} short" else "it fits") +
            "; uninstalling first would leave about ${f(other.freeAfter)} against ${f(other.need)} (" + (if (other.fits) "that works" else "still ${f(other.shortBy)} short") + ")."
    }

    /**
     * After the engine cleared its caches the entries it dropped sit in `cache2/doomed` (and `cache2/trash*`) until the engine's next start: nothing holds them open (checked with /proc/<pid>/fd on the
     * emulator: 48 MB doomed, no process with an open descriptor), they only wait. Unlinking them gives the room back now. Off the UI thread. Returns the bytes removed.
     */
    fun purgeDoomedCache(ctx: Context): Long {
        var freed = 0L
        val stack = ArrayDeque<File>(); stack.add(ctx.applicationContext.cacheDir)
        var n = 0
        while (stack.isNotEmpty() && n < 100_000) {
            val d = stack.removeLast(); n++
            val kids = d.listFiles() ?: continue
            val doomedHere = d.parentFile?.name == "cache2" && (d.name == "doomed" || d.name.startsWith("trash"))
            for (k in kids) {
                if (k.isDirectory) { stack.add(k); continue }
                if (doomedHere) { val len = k.length(); if (k.delete()) freed += len }
            }
        }
        return freed
    }

    /** Bytes in the app's cache directory (the engine's network and image caches, previews): what "Clear cache" can give back. Off the UI thread. */
    fun cacheBytes(ctx: Context): Long = size(ctx.applicationContext.cacheDir)

    private fun size(d: File): Long {
        var sum = 0L
        val stack = ArrayDeque<File>(); stack.add(d)
        var n = 0
        while (stack.isNotEmpty() && n < 200_000) {
            val x = stack.removeLast(); n++
            val kids = x.listFiles() ?: continue
            for (k in kids) if (k.isDirectory) stack.add(k) else sum += k.length()
        }
        return sum
    }
}
