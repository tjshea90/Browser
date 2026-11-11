package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import app.kestrel.browser.web.Tab
import java.io.File

/**
 * Page previews for the tab grid. A preview is a ~240 px wide RGB_565 bitmap (about 150 KB) kept on the tab; regular tabs also keep a lossy WebP in the
 * cache directory (about 10 KB) so the grid is not empty after a restart. A private tab's preview lives in memory only and dies with the tab.
 */
object ThumbStore {
    const val WIDTH = 240
    private const val MAX_IN_MEMORY = 20

    /** Scale a full-size capture down to a preview (call off the UI thread). The capture is recycled. */
    fun shrink(full: Bitmap): Bitmap? = try {
        if (full.width <= 0 || full.height <= 0) null else {
            val h = (full.height.toLong() * WIDTH / full.width).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(full, WIDTH, h, true)
            val out = scaled.copy(Bitmap.Config.RGB_565, false) ?: scaled
            if (out !== scaled) scaled.recycle()
            if (full !== scaled && !full.isRecycled) full.recycle()
            out
        }
    } catch (e: Throwable) { null }

    private fun dir(ctx: Context) = File(ctx.cacheDir, "thumbs")

    @Suppress("DEPRECATION")
    fun save(ctx: Context, id: Long, bmp: Bitmap) {
        try {
            val d = dir(ctx); d.mkdirs()
            File(d, "$id.webp").outputStream().use {
                bmp.compress(if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP, 60, it)
            }
        } catch (e: Exception) { /* a missing preview is harmless */ }
    }

    fun load(ctx: Context, id: Long): Bitmap? = try {
        val f = File(dir(ctx), "$id.webp")
        if (!f.isFile) null else BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
    } catch (e: Throwable) { null }

    fun delete(ctx: Context, id: Long) { try { File(dir(ctx), "$id.webp").delete() } catch (e: Exception) { /* ignore */ } }

    fun clearAll(ctx: Context) { try { dir(ctx).listFiles()?.forEach { it.delete() } } catch (e: Exception) { /* ignore */ } }

    /** Keep at most [MAX_IN_MEMORY] previews in RAM: the oldest ones (except [keep]) are dropped; they reload from disk when the grid needs them. */
    fun trim(tabs: List<Tab>, keep: Tab?) {
        val have = tabs.filter { it.thumb != null && it !== keep }
        if (have.size + (if (keep?.thumb != null) 1 else 0) <= MAX_IN_MEMORY) return
        for (t in have.sortedBy { it.thumbAt }.take(have.size + 1 - MAX_IN_MEMORY)) t.thumb = null
    }
}
