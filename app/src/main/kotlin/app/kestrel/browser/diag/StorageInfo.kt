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
                "this app: package ${f(apk)} + unpacked libraries ${f(libs)} + data ${f(files)} + cache ${f(cache)}; an install or update needs free >= reserve + 2 x package + libraries + about 48 MB"
        } catch (e: Exception) { "Storage: unavailable (${e.javaClass.simpleName})" }
    }

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
