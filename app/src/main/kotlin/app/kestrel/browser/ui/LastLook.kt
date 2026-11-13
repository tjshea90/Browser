package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import app.kestrel.core.session.LastLookMeta
import java.io.File

/**
 * The "last look" store (research/REVIEW-EXTERNAL-2.md EY-1): a sharp picture of the page the owner left, at the full size of the page area, so the browser can open showing that page in its first frame
 * while the live one loads under it. `filesDir/lastlook/<tabId>.webp` (the picture, lossy WebP) and `<tabId>.json` ([LastLookMeta]: address, scroll place, size, engine version...). The json exists only when
 * the picture it describes is whole: it is removed before a new picture is written and written after it.
 *
 * Never in the cache folder (Android may empty that when space is low). Never for a private tab. Never shared: the manifests forbid backup and device transfer of the app's files, and nothing here is
 * written to external storage. It shows what the Recents screen already shows, no more. Everything here may block: call it off the UI thread (the one decode at start-up excepted).
 */
object LastLook {
    /** Names the look of the app's chrome; the picture was taken under it. Change it when a skin change would make the old pictures wrong. */
    const val SKIN = "reaper1"

    class Look(val bitmap: Bitmap, val meta: LastLookMeta)

    /** What the page looked like under: the skin, the "Dark pages" choice and the system's night mode. A picture taken under another appearance is not shown. */
    fun appearance(ctx: Context, webDark: Int): String =
        SKIN + ":" + webDark + ":" + (ctx.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)

    private fun dir(ctx: Context) = File(ctx.filesDir, "lastlook")

    @Suppress("DEPRECATION")
    private val FORMAT get() = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

    /** Writes the picture and its record; false when anything failed (nothing partial stays). [full] is NOT recycled. */
    fun save(ctx: Context, id: Long, full: Bitmap, meta: LastLookMeta): Boolean {
        val d = dir(ctx)
        return try {
            d.mkdirs()
            val json = File(d, "$id.json"); val img = File(d, "$id.webp")
            json.delete()
            val tmpImg = File(d, "$id.webp.tmp")
            val ok = tmpImg.outputStream().use { full.compress(FORMAT, 72, it) }
            if (!ok || !tmpImg.renameTo(img)) { tmpImg.delete(); return false }
            val tmpJson = File(d, "$id.json.tmp")
            tmpJson.writeText(meta.encode())
            if (!tmpJson.renameTo(json)) { tmpJson.delete(); img.delete(); return false }
            true
        } catch (e: Exception) {
            try { File(d, "$id.json").delete(); File(d, "$id.webp").delete() } catch (x: Exception) { /* ignore */ }
            false
        }
    }

    /** The record alone (small): null when there is none or it cannot be used. */
    fun meta(ctx: Context, id: Long): LastLookMeta? = try {
        val f = File(dir(ctx), "$id.json")
        if (!f.isFile || f.length() > 8192) null else LastLookMeta.decode(f.readText())
    } catch (e: Exception) { null }

    /** The picture of [meta] decoded as RGB_565 (half the memory of the engine's own format; shown only while the live page loads). */
    fun load(ctx: Context, id: Long, meta: LastLookMeta): Look? = try {
        val f = File(dir(ctx), "$id.webp")
        if (!f.isFile) null else BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })?.let { Look(it, meta) }
    } catch (e: Throwable) { null }

    fun delete(ctx: Context, id: Long) {
        try { val d = dir(ctx); File(d, "$id.json").delete(); File(d, "$id.webp").delete(); File(d, "$id.json.tmp").delete(); File(d, "$id.webp.tmp").delete() } catch (e: Exception) { /* ignore */ }
    }

    /** Every picture goes (clearing browsing data, the "clear on exit" start, the switch turned off). */
    fun clearAll(ctx: Context) { try { dir(ctx).listFiles()?.forEach { it.delete() } } catch (e: Exception) { /* ignore */ } }

    /** Only the pictures of [keep] stay (and anything that is not a picture of a tab goes). */
    fun retain(ctx: Context, keep: Set<Long>) {
        try {
            for (f in dir(ctx).listFiles() ?: return) {
                val id = f.name.substringBefore('.').toLongOrNull()
                if (id == null || id !in keep) f.delete()
            }
        } catch (e: Exception) { /* ignore */ }
    }

    /** How many pictures are stored (the Diagnostics line). */
    fun count(ctx: Context): Int = try { dir(ctx).listFiles { f -> f.name.endsWith(".json") }?.size ?: 0 } catch (e: Exception) { 0 }
}
