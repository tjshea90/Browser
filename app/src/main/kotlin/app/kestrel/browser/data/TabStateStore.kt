package app.kestrel.browser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/**
 * The engine's saved state of a tab (its back/forward list with titles and addresses, its scroll position) as JSON text, one small file per REGULAR tab in `filesDir/tabstate`.
 * Without it a swiped-away app, or one that Android ended in the background, comes back with only each tab's last address: no Back stack and the page at its top, loaded fresh from the
 * network. With it the tab is restored the way the engine itself restores a tab (history load, scroll position back). Strings only: the Gecko classes stay in the engine module.
 *
 *  - Private tabs are never written (they are not in session.txt either).
 *  - Typed form text ("formdata") is removed before anything is written: unencrypted storage and a field could hold a password or a message.
 *  - A file belongs to ONE engine version: another GeckoView may read old JSON differently, so a version change empties the folder ([useEngine]).
 *  - Everything here is best effort: a missing, oversized or damaged file means "load the address", which is what every start did before.
 */
object TabStateStore {
    private const val DIR = "tabstate"
    private const val MARKER = "engine.txt"

    /** A state bigger than this (characters) is not kept: a tab with a huge history falls back to its address. */
    const val MAX_CHARS = 96 * 1024

    fun dir(ctx: Context): File = File(ctx.filesDir, DIR)

    /**
     * The text that may be written for [raw] (the engine's own JSON): typed form data removed at any depth, or null when the text is not JSON, is too big, or has no history at all
     * (nothing worth restoring).
     */
    fun sanitize(raw: String): String? {
        if (raw.isEmpty() || raw.length > MAX_CHARS * 4) return null // far too big: do not even parse it
        return try {
            val root = JSONTokener(raw).nextValue() as? JSONObject ?: return null
            strip(root)
            val out = root.toString()
            if (out.length > MAX_CHARS || !out.contains("history")) null else out
        } catch (e: Exception) { null }
    }

    private fun strip(v: Any?) {
        when (v) {
            is JSONObject -> {
                v.remove("formdata")
                val keys = ArrayList<String>(); val it = v.keys(); while (it.hasNext()) keys.add(it.next())
                for (k in keys) strip(v.opt(k))
            }
            is JSONArray -> for (i in 0 until v.length()) strip(v.opt(i))
        }
    }

    /** True when the saved states match this engine version; otherwise the folder is emptied and the marker rewritten (the caller then has no states). */
    fun useEngine(ctx: Context, engineVersion: String): Boolean {
        val d = dir(ctx)
        if (!d.isDirectory) return false
        val marker = File(d, MARKER)
        val same = try { marker.takeIf { it.isFile }?.readText() == engineVersion } catch (e: Exception) { false }
        if (!same) { wipe(ctx); return false }
        return true
    }

    /** Atomic write (temp file + rename). Call on an IO thread. */
    fun write(ctx: Context, id: Long, json: String, engineVersion: String) {
        val d = dir(ctx)
        if (!d.isDirectory && !d.mkdirs()) return
        try {
            val marker = File(d, MARKER)
            if (!marker.isFile) marker.writeText(engineVersion)
            val tmp = File(d, "$id.tmp"); val f = File(d, "$id.json")
            tmp.writeText(json)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        } catch (e: Exception) { /* storage full or gone: the address in session.txt is still there */ }
    }

    /** The saved text of a tab, or null. Call on an IO thread. */
    fun read(ctx: Context, id: Long): String? = try {
        File(dir(ctx), "$id.json").takeIf { it.isFile && it.length() in 2..(MAX_CHARS * 3L) }?.readText()
    } catch (e: Exception) { null }

    fun delete(ctx: Context, id: Long) { try { File(dir(ctx), "$id.json").delete(); File(dir(ctx), "$id.tmp").delete() } catch (e: Exception) { /* ignore */ } }

    /** Remove the files of every tab that is not in [ids] (closed tabs, or tabs of an older session). */
    fun retain(ctx: Context, ids: Set<Long>) {
        val files = try { dir(ctx).listFiles() } catch (e: Exception) { null } ?: return
        for (f in files) {
            if (f.name == MARKER) continue
            val id = f.name.substringBefore('.').toLongOrNull()
            if (id == null || id !in ids) try { f.delete() } catch (e: Exception) { /* ignore */ }
        }
    }

    fun wipe(ctx: Context) {
        val files = try { dir(ctx).listFiles() } catch (e: Exception) { null } ?: return
        for (f in files) try { f.delete() } catch (e: Exception) { /* ignore */ }
    }
}
