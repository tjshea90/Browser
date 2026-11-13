package app.kestrel.browser.download

import app.kestrel.core.download.DlState
import app.kestrel.core.download.Reason
import app.kestrel.core.media.StreamRec
import app.kestrel.core.media.StreamStore
import org.json.JSONObject
import java.io.File

/**
 * Stream downloads on disk: `<id>.json` (the record, rewritten at every checkpoint, small) and `<id>.m3u8` (the playlist text, written once), plus the staging folder `<id>.d` with the
 * segments that are fetched but not yet appended. Private downloads never get a record; their staging lives in the cache folder and is wiped at every start ([wipePrivate]).
 */
class StreamStoreFiles(private val root: File, private val privateRoot: File) : StreamStore {
    init { root.mkdirs() }

    override fun loadAll(): List<StreamRec> {
        val out = ArrayList<StreamRec>()
        for (f in root.listFiles { x -> x.name.endsWith(".json") } ?: return out) {
            try {
                val j = JSONObject(f.readText())
                val r = StreamRec(j.getLong("id"))
                r.state = DlState.of(j.optInt("state", DlState.FAILED.code)); r.reason = Reason.of(j.optInt("reason")); r.detail = j.optInt("detail")
                r.url = j.optString("url"); r.pageUrl = j.optString("pageUrl").takeIf { it.isNotEmpty() }
                r.headers = j.optJSONObject("headers")?.let { h -> h.keys().asSequence().associateWith { k -> h.optString(k) } } ?: emptyMap()
                r.fetcher = j.optInt("fetcher"); r.meteredOk = j.optBoolean("meteredOk")
                r.nameStem = j.optString("nameStem"); r.name = j.optString("name"); r.mime = j.optString("mime").takeIf { it.isNotEmpty() }
                r.destKind = j.optInt("destKind"); r.destUri = j.optString("destUri").takeIf { it.isNotEmpty() }
                r.estimate = j.optLong("estimate", -1); r.repId = j.optString("repId").takeIf { it.isNotEmpty() }; r.ext = j.optString("ext").takeIf { it.isNotEmpty() }; r.appended = j.optLong("appended"); r.next = j.optInt("next"); r.breaks = j.optString("breaks"); r.crashes = j.optInt("crashes"); r.mux = j.optInt("mux"); r.note = j.optString("note").takeIf { it.isNotEmpty() }; r.initDone = j.optBoolean("initDone")
                r.total = j.optLong("total", -1); r.created = j.optLong("created"); r.updated = j.optLong("updated"); r.finished = j.optLong("finished")
                r.masterUrl = j.optString("masterUrl").takeIf { it.isNotEmpty() }; r.frame = j.optString("frame").takeIf { it.isNotEmpty() }; r.fp = j.optString("fp").takeIf { it.isNotEmpty() }
                r.madeAt = j.optLong("madeAt"); r.expiresAt = j.optLong("expiresAt")
                j.optJSONArray("headerSets")?.let { a -> val l = ArrayList<Map<String, String>>(); for (i in 0 until minOf(a.length(), 8)) a.optJSONObject(i)?.let { o -> l.add(o.keys().asSequence().associateWith { k -> o.optString(k) }) }; r.headerSets = l }
                val pl = File(root, "${r.id}.m3u8")
                if (pl.isFile) r.playlistText = pl.readText()
                out.add(r)
            } catch (e: Exception) { try { f.delete() } catch (x: Exception) { /* a record that cannot be read is dropped */ } }
        }
        return out.sortedBy { it.id }
    }

    override fun save(r: StreamRec) {
        val j = JSONObject()
        j.put("id", r.id); j.put("state", r.state.code); j.put("reason", r.reason.code); j.put("detail", r.detail)
        j.put("url", r.url); j.put("pageUrl", r.pageUrl ?: ""); j.put("headers", JSONObject(r.headers as Map<*, *>))
        j.put("fetcher", r.fetcher); j.put("meteredOk", r.meteredOk); j.put("nameStem", r.nameStem); j.put("name", r.name); j.put("mime", r.mime ?: "")
        j.put("destKind", r.destKind); j.put("destUri", r.destUri ?: ""); j.put("estimate", r.estimate); j.put("repId", r.repId ?: ""); j.put("ext", r.ext ?: ""); j.put("appended", r.appended); j.put("next", r.next); j.put("breaks", r.breaks); j.put("crashes", r.crashes); j.put("mux", r.mux); j.put("note", r.note ?: "")
        j.put("initDone", r.initDone); j.put("total", r.total); j.put("created", r.created); j.put("updated", r.updated); j.put("finished", r.finished)
        j.put("masterUrl", r.masterUrl ?: ""); j.put("frame", r.frame ?: ""); j.put("fp", r.fp ?: ""); j.put("madeAt", r.madeAt); j.put("expiresAt", r.expiresAt)
        if (r.headerSets.isNotEmpty()) j.put("headerSets", org.json.JSONArray(r.headerSets.map { JSONObject(it as Map<*, *>) }))
        val text = r.playlistText
        val pl = File(root, "${r.id}.m3u8")
        if (text != null) { if (!pl.isFile || pl.length() != text.toByteArray().size.toLong()) atomic(pl, text) } else if (pl.exists()) pl.delete()
        atomic(File(root, "${r.id}.json"), j.toString())
    }

    override fun delete(id: Long) {
        File(root, "$id.json").delete(); File(root, "$id.m3u8").delete()
        dir(id).let { d -> d.listFiles()?.forEach { it.delete() }; d.delete() }
    }

    override fun dir(id: Long): File = (if (id < 0) File(privateRoot, (-id).toString()) else File(root, "$id.d")).also { it.mkdirs() }

    /** Private downloads do not survive the process: whatever their staging left behind goes. */
    fun wipePrivate() {
        privateRoot.listFiles()?.forEach { d -> d.listFiles()?.forEach { it.delete() }; d.delete() }
    }

    private fun atomic(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }
}
