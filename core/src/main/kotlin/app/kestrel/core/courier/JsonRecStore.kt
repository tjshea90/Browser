package app.kestrel.core.courier

import app.kestrel.core.download.DlState
import app.kestrel.core.download.Reason
import app.kestrel.core.download.Rec
import app.kestrel.core.download.Seg
import app.kestrel.core.download.Store
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** [Rec] as a JSON object and back (every persisted field; runtime-only [Seg] fields are not stored). */
object RecJson {
    fun encode(r: Rec): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        m["id"] = r.id; m["state"] = r.state.code; m["reason"] = r.reason.code; m["detail"] = r.detail; m["prio"] = r.prio
        m["url"] = r.url; m["finalUrl"] = r.finalUrl; m["finalAt"] = r.finalAt; m["pageUrl"] = r.pageUrl
        m["headers"] = LinkedHashMap<String, Any?>(r.headers)
        m["name"] = r.name; m["nameFixed"] = r.nameFixed; m["mime"] = r.mime; m["destKind"] = r.destKind; m["destUri"] = r.destUri
        m["total"] = r.total; m["etag"] = r.etag; m["lastMod"] = r.lastMod; m["dateHdr"] = r.dateHdr; m["ranges"] = r.ranges
        m["fetcher"] = r.fetcher; m["risk"] = r.risk; m["meteredOk"] = r.meteredOk; m["done"] = r.done; m["fails"] = r.fails
        m["connsPeak"] = r.connsPeak; m["crashes"] = r.crashes; m["priv"] = r.priv
        m["created"] = r.created; m["updated"] = r.updated; m["lastProgress"] = r.lastProgress; m["finished"] = r.finished
        m["segs"] = r.segs.map { s -> listOf(s.idx.toLong(), s.start, s.end, s.cursor) }
        return m
    }

    fun decode(m: Map<*, *>): Rec? {
        val id = m.long("id") ?: return null
        val r = Rec(id)
        r.state = DlState.of(m.int("state") ?: return null); r.reason = Reason.of(m.int("reason") ?: 0); r.detail = m.int("detail") ?: 0; r.prio = m.int("prio") ?: 0
        r.url = m.str("url") ?: return null; r.finalUrl = m.str("finalUrl"); r.finalAt = m.long("finalAt") ?: 0L; r.pageUrl = m.str("pageUrl")
        val h = LinkedHashMap<String, String>()
        m.obj("headers")?.forEach { (k, v) -> if (k is String && v is String) h[k] = v }
        r.headers = h
        r.name = m.str("name") ?: ""; r.nameFixed = m.bool("nameFixed") ?: false; r.mime = m.str("mime"); r.destKind = m.int("destKind") ?: 0; r.destUri = m.str("destUri")
        r.total = m.long("total") ?: -1L; r.etag = m.str("etag"); r.lastMod = m.str("lastMod"); r.dateHdr = m.str("dateHdr"); r.ranges = m.int("ranges") ?: -1
        r.fetcher = m.int("fetcher") ?: 0; r.risk = m.int("risk") ?: 0; r.meteredOk = m.bool("meteredOk") ?: false; r.done = m.long("done") ?: 0L; r.fails = m.int("fails") ?: 0
        r.connsPeak = m.int("connsPeak") ?: 0; r.crashes = m.int("crashes") ?: 0; r.priv = m.bool("priv") ?: false
        r.created = m.long("created") ?: 0L; r.updated = m.long("updated") ?: 0L; r.lastProgress = m.long("lastProgress") ?: 0L; r.finished = m.long("finished") ?: 0L
        for (s in m.list("segs") ?: emptyList<Any?>()) {
            val a = s as? List<*> ?: continue
            if (a.size != 4) continue
            val nums = a.map { (it as? Number)?.toLong() ?: return null }
            r.segs.add(Seg(nums[0].toInt(), nums[1], nums[2], nums[3]))
        }
        return r
    }
}

/**
 * The engine's [Store] as one JSON file, replaced atomically (temp file + rename, the old file kept as `.bak` until the new one is in place), so a kill in the middle of a
 * write leaves the previous complete state. The phone app uses this instead of SQLite: it is pure JDK, so the JVM tests exercise the same code as the phone, including restarts.
 * Records are small and few (a handful of jobs), so every save rewrites the file.
 */
class JsonRecStore(private val file: File, private val log: (String) -> Unit = {}) : Store {
    private val recs = LinkedHashMap<Long, Rec>()

    @Synchronized override fun loadAll(): List<Rec> {
        recs.clear()
        for (text in AtomicFiles.candidates(file)) {
            try {
                val root = Json.parse(text) as? Map<*, *> ?: continue
                for (x in root.list("downloads") ?: emptyList<Any?>()) { val r = (x as? Map<*, *>)?.let { RecJson.decode(it) } ?: continue; recs[r.id] = r }
                break
            } catch (e: Exception) { log("store file ${file.name} unreadable: ${e.javaClass.simpleName}"); recs.clear() }
        }
        return recs.values.map { it.snapshot() }
    }

    @Synchronized override fun insert(r: Rec) { recs[r.id] = r.snapshot(); flush() }
    @Synchronized override fun save(r: Rec) { recs[r.id] = r.snapshot(); flush() }
    @Synchronized override fun delete(id: Long) { if (recs.remove(id) != null) flush() }

    private fun flush() {
        try { AtomicFiles.write(file, Json.write(mapOf("v" to 1, "downloads" to recs.values.map { RecJson.encode(it) }))) }
        catch (e: IOException) { log("store write failed: ${e.message}") }
    }
}
