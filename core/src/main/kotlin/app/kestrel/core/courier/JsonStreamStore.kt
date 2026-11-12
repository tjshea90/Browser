package app.kestrel.core.courier

import app.kestrel.core.download.DlState
import app.kestrel.core.download.Reason
import app.kestrel.core.media.StreamRec
import app.kestrel.core.media.StreamStore
import java.io.File
import java.io.IOException

/**
 * The segment downloader's records as files, the way the browser keeps them but pure JDK (the phone app and the JVM tests use the same class): `<id>.json` (the record, replaced atomically
 * at every checkpoint, the old one kept as `.bak`), `<id>.m3u8` (the playlist text, written once, so the segment list is the same after a restart) and `<id>.d` (the fetched segments not yet
 * appended). Private downloads never reach the phone, so nothing here is wiped at start.
 */
class JsonStreamStore(private val root: File, private val log: (String) -> Unit = {}) : StreamStore {
    init { root.mkdirs() }

    override fun loadAll(): List<StreamRec> {
        val out = ArrayList<StreamRec>()
        for (f in root.listFiles { x -> x.isFile && x.name.endsWith(".json") } ?: return out) {
            var rec: StreamRec? = null
            for (text in AtomicFiles.candidates(f)) {
                try { rec = decode(Json.parse(text) as? Map<*, *> ?: continue); if (rec != null) break } catch (e: Exception) { log("stream record ${f.name} unreadable: ${e.javaClass.simpleName}") }
            }
            if (rec == null) continue
            val pl = File(root, "${rec.id}.m3u8")
            if (pl.isFile) try { rec.playlistText = pl.readText(Charsets.UTF_8) } catch (e: IOException) { /* fetched again on resume */ }
            out.add(rec)
        }
        return out.sortedBy { it.id }
    }

    override fun save(r: StreamRec) {
        try {
            val text = r.playlistText
            val pl = File(root, "${r.id}.m3u8")
            if (text != null) { if (!pl.isFile || pl.length() != text.toByteArray(Charsets.UTF_8).size.toLong()) AtomicFiles.write(pl, text) } else if (pl.exists()) pl.delete()
            AtomicFiles.write(File(root, "${r.id}.json"), Json.write(encode(r)))
        } catch (e: IOException) { log("stream record ${r.id} not saved: ${e.message}") }
    }

    override fun delete(id: Long) {
        for (n in listOf("$id.json", "$id.json.bak", "$id.json.tmp", "$id.m3u8", "$id.m3u8.bak", "$id.m3u8.tmp")) try { File(root, n).delete() } catch (_: SecurityException) {}
        val d = File(root, "$id.d")
        d.listFiles()?.forEach { try { it.delete() } catch (_: SecurityException) {} }
        try { d.delete() } catch (_: SecurityException) {}
    }

    override fun dir(id: Long): File = File(root, "$id.d").also { it.mkdirs() }

    companion object {
        fun encode(r: StreamRec): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            m["id"] = r.id; m["state"] = r.state.code; m["reason"] = r.reason.code; m["detail"] = r.detail; m["url"] = r.url; m["pageUrl"] = r.pageUrl
            m["headers"] = LinkedHashMap<String, Any?>(r.headers); m["fetcher"] = r.fetcher; m["meteredOk"] = r.meteredOk; m["nameStem"] = r.nameStem; m["name"] = r.name; m["mime"] = r.mime
            m["destKind"] = r.destKind; m["destUri"] = r.destUri; m["estimate"] = r.estimate; m["repId"] = r.repId; m["ext"] = r.ext; m["companionId"] = r.companionId; m["parentId"] = r.parentId
            m["appended"] = r.appended; m["next"] = r.next; m["breaks"] = r.breaks; m["crashes"] = r.crashes; m["mux"] = r.mux; m["note"] = r.note; m["initDone"] = r.initDone
            m["total"] = r.total; m["created"] = r.created; m["updated"] = r.updated; m["finished"] = r.finished
            return m
        }

        fun decode(m: Map<*, *>): StreamRec? {
            val r = StreamRec(m.long("id") ?: return null)
            r.state = DlState.of(m.int("state") ?: return null); r.reason = Reason.of(m.int("reason") ?: 0); r.detail = m.int("detail") ?: 0
            r.url = m.str("url") ?: return null; r.pageUrl = m.str("pageUrl")
            val h = LinkedHashMap<String, String>()
            m.obj("headers")?.forEach { (k, v) -> if (k is String && v is String) h[k] = v }
            r.headers = h
            r.fetcher = m.int("fetcher") ?: 0; r.meteredOk = m.bool("meteredOk") ?: false; r.nameStem = m.str("nameStem") ?: ""; r.name = m.str("name") ?: ""; r.mime = m.str("mime")
            r.destKind = m.int("destKind") ?: 0; r.destUri = m.str("destUri"); r.estimate = m.long("estimate") ?: -1L; r.repId = m.str("repId"); r.ext = m.str("ext")
            r.companionId = m.long("companionId") ?: 0L; r.parentId = m.long("parentId") ?: 0L
            r.appended = m.long("appended") ?: 0L; r.next = m.int("next") ?: 0; r.breaks = m.str("breaks") ?: ""; r.crashes = m.int("crashes") ?: 0; r.mux = m.int("mux") ?: 0
            r.note = m.str("note"); r.initDone = m.bool("initDone") ?: false
            r.total = m.long("total") ?: -1L; r.created = m.long("created") ?: 0L; r.updated = m.long("updated") ?: 0L; r.finished = m.long("finished") ?: 0L
            return r
        }
    }
}
