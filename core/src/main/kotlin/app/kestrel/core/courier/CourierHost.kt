package app.kestrel.core.courier

import app.kestrel.core.download.Action
import app.kestrel.core.download.Clock
import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Engine
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.Fetcher
import app.kestrel.core.download.Source
import app.kestrel.core.media.StreamEngine
import app.kestrel.core.media.StreamSource
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.URL
import java.security.SecureRandom

/**
 * The phone side of the courier protocol (research/PHONE_OFFLOAD.md sections 3-6): authenticates the tablet's requests, turns "download this for me" into a job of the
 * phone's own download [Engine], reports its live state, and hands the finished file out under an unguessable per-job address. Pure Kotlin: the phone app only supplies the
 * engine (with a [StagingSinks] folder, a [JsonRecStore], a network gate, a [app.kestrel.core.download.UrlConnectionFetcher] guarded by [Ssrf]) and a socket server.
 *
 * Thread-safe: [handle] runs on the server's connection threads.
 */
class CourierHost(
    private val key: ByteArray,
    private val engine: Engine,
    private val sinks: StagingSinks,
    private val tableFile: File,
    /** Used only for `/v1/probe` (the phone's own internet speed); should be the same guarded fetcher the engine uses. */
    private val probeFetcher: Fetcher,
    private val clock: Clock = Clock.System,
    private val freeBytes: () -> Long,
    private val deviceName: String = "phone",
    private val rng: SecureRandom = SecureRandom(),
    private val log: (String) -> Unit = {},
    private val resolve: (String) -> List<ByteArray> = { InetAddress.getAllByName(it).map { a -> a.address } },
    /** The segment downloader for video streams (HLS, written straight into an MP4); null = this phone only does plain files (a stream job is then refused with 501 and the tablet does it itself). */
    private val streams: StreamEngine? = null,
) {
    companion object {
        const val APP = "kestrel-courier"
        const val VERSION = 1
        const val MAX_JOBS = 64
        /** Room that must stay free on the phone on top of a file (the file itself is reserved up front). */
        const val RESERVE_BYTES = 64L shl 20
        const val DONE_KEEP_MS = 24L * 3600_000
        const val FAILED_KEEP_MS = 7L * 24 * 3600_000
        const val MAX_SPEEDTEST = 64L shl 20
        /** The playlist text the tablet already read (saves the phone a request, and works for playlists only the tablet's cookies can open); longer ones are fetched by the phone. */
        const val MAX_PLAYLIST_CHARS = 1_500_000
        private val FORWARDED = setOf("user-agent", "referer", "origin", "cookie", "accept", "accept-language")
        /** What a row on the tablet may offer while the phone is still working; Open / Share / Delete are the tablet's own once the file is there. */
        private val PHONE_ACTIONS = setOf(Action.PAUSE, Action.RESUME, Action.CANCEL, Action.RETRY, Action.RESTART)
    }

    private class CJob(val id: String, val cid: String, val engineId: Long, val kind: String, val name: String, val nameFixed: Boolean, val mime: String?, val created: Long) {
        @Volatile var token: String? = null
        /** The tablet asked to cancel it. The engine forgets a canceled job at once, so the host remembers (and says CANCELED) until the tablet deletes it. */
        @Volatile var canceled = false
    }

    private val lock = Any()
    private val jobs = LinkedHashMap<String, CJob>()
    private val byCid = HashMap<String, CJob>()
    private val byToken = HashMap<String, CJob>()
    private val replay = ReplayGuard()
    private val throttle = AuthThrottle()

    // ---------------------------------------------------------------------------------------------------------------- persistence
    /** Reads the job table (call after the engine's own `restore()`). Jobs whose engine record is gone are reported as GONE until the tablet deletes them. */
    fun load() {
        synchronized(lock) {
            jobs.clear(); byCid.clear(); byToken.clear()
            for (text in AtomicFiles.candidates(tableFile)) {
                try {
                    val root = Json.parse(text) as? Map<*, *> ?: continue
                    for (x in root.list("jobs") ?: emptyList<Any?>()) {
                        val m = x as? Map<*, *> ?: continue
                        val j = CJob(m.str("id") ?: continue, m.str("cid") ?: continue, m.long("engineId") ?: continue, m.str("kind") ?: "file", m.str("name") ?: "", m.bool("nameFixed") ?: false, m.str("mime"), m.long("created") ?: 0L)
                        j.token = m.str("token")
                        j.canceled = m.bool("canceled") ?: false
                        add(j)
                    }
                    break
                } catch (e: Exception) { log("job table unreadable: ${e.javaClass.simpleName}"); jobs.clear(); byCid.clear(); byToken.clear() }
            }
        }
    }

    private fun add(j: CJob) { jobs[j.id] = j; byCid[j.cid] = j; j.token?.let { byToken[it] = j } }

    private fun save() {
        val list = jobs.values.map { j ->
            linkedMapOf<String, Any?>("id" to j.id, "cid" to j.cid, "engineId" to j.engineId, "kind" to j.kind, "name" to j.name, "nameFixed" to j.nameFixed, "mime" to j.mime, "created" to j.created, "token" to j.token, "canceled" to j.canceled)
        }
        try { AtomicFiles.write(tableFile, Json.write(mapOf("v" to 1, "jobs" to list))) } catch (e: IOException) { log("job table write failed: ${e.message}") }
    }

    // ---------------------------------------------------------------------------------------------------------------- entry point
    fun handle(req: HttpRequest, peer: InetAddress): HttpResponse {
        val p = req.path
        if (p == "/v1/hello") return if (req.method == "GET") hello(req) else HttpResponse.error(405, "method")
        if (p.startsWith("/v1/f/")) return file(req, peer)
        val ip = peer.hostAddress ?: "?"
        val now = clock.now()
        if (throttle.blocked(ip, now)) return HttpResponse(429, mapOf("Retry-After" to "60", "Content-Type" to "application/json; charset=utf-8"), BytesBody("""{"error":"slow_down"}""".toByteArray()), close = true)
        when (Sign.check(key, req.method, req.target, req.headers[Sign.H_TS], req.headers[Sign.H_SIG], req.body, now, replay)) {
            Sign.Result.OK -> throttle.ok(ip)
            Sign.Result.SKEW -> return HttpResponse.json(401, mapOf("error" to "skew", "t" to now))      // a valid signature on a wrong clock: tell the tablet the time instead of counting it as an attack
            else -> { throttle.failed(ip, now); return HttpResponse.error(401, "auth") }
        }
        val seg = p.split('/').filter { it.isNotEmpty() }
        if (seg.size < 2 || seg[0] != "v1") return HttpResponse.error(404, "not_found")
        return when (seg[1]) {
            "status" -> if (req.method == "GET" && seg.size == 2) status() else HttpResponse.error(405, "method")
            "jobs" -> when {
                seg.size == 2 && req.method == "POST" -> create(req)
                seg.size == 2 && req.method == "GET" -> list(req)
                seg.size == 3 && req.method == "DELETE" -> remove(seg[2])
                seg.size == 4 && req.method == "POST" -> act(seg[2], seg[3])
                else -> HttpResponse.error(if (seg.size in 2..4) 405 else 404, "not_found")
            }
            "speedtest" -> if (req.method == "GET" && seg.size == 2) speedtest(req) else HttpResponse.error(405, "method")
            "probe" -> if (req.method == "POST" && seg.size == 2) probe(req) else HttpResponse.error(405, "method")
            else -> HttpResponse.error(404, "not_found")
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- routes
    private fun hello(req: HttpRequest): HttpResponse {
        val c = req.query["c"]
        if (c == null || c.length !in 8..64 || !c.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return HttpResponse.error(400, "challenge")
        val t = clock.now()
        return HttpResponse.json(200, mapOf("app" to APP, "v" to VERSION, "t" to t, "proof" to Sign.helloProof(key, c, t)))
    }

    private fun status(): HttpResponse {
        val c = engine.counts(); val sc = streams?.counts() ?: intArrayOf(0, 0, 0)
        return HttpResponse.json(200, mapOf("app" to APP, "v" to VERSION, "name" to deviceName, "free" to freeBytes(), "jobs" to synchronized(lock) { jobs.size }, "running" to c[0] + sc[0], "queued" to c[1] + sc[1], "t" to clock.now()))
    }

    private fun body(req: HttpRequest): Map<*, *>? = try { Json.parse(String(req.body, Charsets.UTF_8)) as? Map<*, *> } catch (e: JsonException) { null }

    private fun create(req: HttpRequest): HttpResponse {
        val j = body(req) ?: return HttpResponse.error(400, "json")
        val cid = j.str("cid")
        if (cid == null || cid.length !in 8..64 || !cid.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return HttpResponse.error(400, "cid")
        val kind = j.str("kind") ?: "file"
        if (kind != "file" && !(kind == "stream" && streams != null)) return HttpResponse.error(501, "kind")
        val st: Map<*, *>? = if (kind == "stream") (j.obj("stream") ?: return HttpResponse.error(400, "stream")) else null
        val co: Map<*, *>? = st?.obj("companion")                                                                  // picture + sound kept apart (DASH): the sound is a hidden job that is merged at the end
        val url = (if (st != null) st.str("playlistUrl") else j.str("url")) ?: return HttpResponse.error(400, "url")
        val u = try { if (url.length > 8192) null else URL(url) } catch (e: Exception) { null } ?: return HttpResponse.error(400, "url")
        if (u.protocol != "http" && u.protocol != "https") return HttpResponse.error(400, "url")
        val coUrl = if (co == null) null else (co.str("playlistUrl")?.takeIf { it.length <= 8192 } ?: return HttpResponse.error(400, "companion"))
        val coU = if (coUrl == null) null else (try { URL(coUrl) } catch (e: Exception) { null } ?: return HttpResponse.error(400, "companion"))
        if (coU != null && coU.protocol != "http" && coU.protocol != "https") return HttpResponse.error(400, "companion")
        synchronized(lock) { byCid[cid]?.let { return HttpResponse.json(200, wire(it)) } }               // the same request again: the same job
        try { Ssrf.check(u.host.removePrefix("[").removeSuffix("]"), resolve); if (coU != null) Ssrf.check(coU.host.removePrefix("[").removeSuffix("]"), resolve) } catch (e: FetchException) { return HttpResponse.error(403, "local_net") }
        val size = j.long("size") ?: -1L
        val free = freeBytes()
        if (free < RESERVE_BYTES || (size > 0 && free < size + RESERVE_BYTES)) return HttpResponse.json(507, mapOf("error" to "no_space", "free" to free))
        val name = j.str("name")?.take(200)
        val headers = LinkedHashMap<String, String>()
        j.obj("headers")?.forEach { (k, v) ->
            if (k is String && v is String && k.lowercase() in FORWARDED && v.length <= 16384 && v.none { it == '\r' || it == '\n' }) headers[k] = v
        }
        val job: CJob
        synchronized(lock) {
            byCid[cid]?.let { return HttpResponse.json(200, wire(it)) }
            if (jobs.size >= MAX_JOBS) return HttpResponse.error(429, "too_many_jobs")
            val eid: Long
            if (st != null) {
                eid = streams!!.enqueue(streamSource(st, url, headers, if (co != null && coUrl != null) streamSource(co, coUrl, headers, null) else null))
            } else {
                eid = engine.enqueue(Source(url, null, name, j.str("mime"), headers, 0, priv = false, meteredOk = true, risk = 0, destKind = DEST_APP_PRIVATE, nameFixed = j.bool("nameFixed") ?: false))
            }
            job = CJob(newId(), cid, eid, kind, if (st != null) "" else (name ?: ""), j.bool("nameFixed") ?: false, j.str("mime"), clock.now())
            add(job); save()
        }
        return HttpResponse.json(201, wire(job))
    }

    /** One stream request (the picture, or the sound of a DASH picture) as the segment downloader takes it: always the app-private folder (the tablet copies the result), never a private tab. */
    private fun streamSource(st: Map<*, *>, url: String, headers: Map<String, String>, companion: StreamSource?): StreamSource {
        val text = st.str("playlistText")?.takeIf { it.length <= MAX_PLAYLIST_CHARS }
        val stem = (st.str("nameStem") ?: "").take(200).ifBlank { "video" }
        return StreamSource(url, text, st.str("pageUrl")?.takeIf { it.length <= 8192 }, stem, headers, 0, false, true, DEST_APP_PRIVATE, st.long("estimateBytes") ?: -1L, st.str("repId")?.take(200), st.str("ext")?.take(16), companion)
    }

    private fun list(req: HttpRequest): HttpResponse {
        val ids = req.query["ids"]
        val out = ArrayList<Any?>()
        synchronized(lock) {
            if (ids == null) for (j in jobs.values) out.add(wire(j))
            else for (id in ids.split(',').filter { it.isNotEmpty() }.take(200)) { val j = jobs[id]; out.add(if (j != null) wire(j) else mapOf("id" to id, "state" to "GONE")) }
        }
        return HttpResponse.json(200, mapOf("t" to clock.now(), "jobs" to out))
    }

    private fun act(id: String, action: String): HttpResponse {
        val j = synchronized(lock) { jobs[id] } ?: return HttpResponse.error(404, "no_job")
        val stream = j.kind == "stream"
        when (action) {
            "pause" -> if (stream) streams?.pause(j.engineId) else engine.pause(j.engineId)
            "resume" -> if (stream) streams?.resume(j.engineId) else engine.resume(j.engineId)
            "cancel" -> { synchronized(lock) { j.canceled = true; save() }; if (stream) streams?.cancel(j.engineId) else engine.cancel(j.engineId) }
            "restart" -> { synchronized(lock) { if (j.canceled) { j.canceled = false; save() } }; if (stream) streams?.restart(j.engineId) else engine.restart(j.engineId) }
            else -> return HttpResponse.error(404, "action")
        }
        return HttpResponse.json(202, mapOf("ok" to true))
    }

    private fun remove(id: String): HttpResponse {
        val j = synchronized(lock) { jobs[id] } ?: return HttpResponse.json(200, mapOf("ok" to true))        // already gone: fine
        drop(j)
        return HttpResponse.json(200, mapOf("ok" to true))
    }

    /** The job's state in whichever engine holds it (video streams live in the segment downloader, everything else in the single-file engine). */
    private fun viewOf(j: CJob): DownloadView? = if (j.kind == "stream") streams?.view(j.engineId) else engine.view(j.engineId)

    private fun drop(j: CJob) {
        if (j.kind == "stream") streams?.remove(j.engineId, true) else engine.remove(j.engineId, true)
        synchronized(lock) { jobs.remove(j.id); byCid.remove(j.cid); j.token?.let { byToken.remove(it) }; save() }
    }

    private fun speedtest(req: HttpRequest): HttpResponse {
        val n = (req.query["bytes"]?.toLongOrNull() ?: (8L shl 20)).coerceIn(1L, MAX_SPEEDTEST)
        return HttpResponse(200, mapOf("Content-Type" to "application/octet-stream"), object : HttpBody {
            override val length: Long = n
            override fun writeTo(out: OutputStream, from: Long, count: Long) {
                val z = ByteArray(64 * 1024)
                var left = count
                while (left > 0) { val k = minOf(z.size.toLong(), left).toInt(); out.write(z, 0, k); left -= k }
            }
        })
    }

    /** The phone's own speed to the internet: fetch the first bytes of a URL the tablet names and time it. */
    private fun probe(req: HttpRequest): HttpResponse {
        val j = body(req) ?: return HttpResponse.error(400, "json")
        val url = j.str("url") ?: return HttpResponse.error(400, "url")
        val want = (j.long("bytes") ?: (8L shl 20)).coerceIn(1L, 32L shl 20)
        val headers = LinkedHashMap<String, String>()
        j.obj("headers")?.forEach { (k, v) -> if (k is String && v is String && k.lowercase() in FORWARDED && v.none { it == '\r' || it == '\n' }) headers[k] = v }
        val t0 = clock.now()
        try {
            probeFetcher.open(FetchRequest(url, 0, want - 1, null, headers, connectMs = 8_000, readMs = 10_000)).use { r ->
                if (r.status !in 200..299) return HttpResponse.json(200, mapOf("ok" to false, "status" to r.status, "ms" to clock.now() - t0))
                val input = r.body ?: return HttpResponse.json(200, mapOf("ok" to false, "status" to r.status))
                val buf = ByteArray(64 * 1024)
                var got = 0L
                while (got < want && clock.now() - t0 < 20_000) { val n = input.read(buf); if (n < 0) break; got += n }
                val ms = maxOf(1L, clock.now() - t0)
                return HttpResponse.json(200, mapOf("ok" to true, "status" to r.status, "bytes" to got, "ms" to ms, "bps" to got * 1000 / ms))
            }
        } catch (e: FetchException) {
            return HttpResponse.json(200, mapOf("ok" to false, "error" to e.kind.name.lowercase()))
        } catch (e: IOException) {
            return HttpResponse.json(200, mapOf("ok" to false, "error" to "reset"))
        }
    }

    /** `GET /v1/f/<token>/<name>`: the finished file. The token IS the credential (128 random bits, valid for this job only). */
    private fun file(req: HttpRequest, peer: InetAddress): HttpResponse {
        if (req.method != "GET" && req.method != "HEAD") return HttpResponse.error(405, "method")
        val seg = req.path.split('/').filter { it.isNotEmpty() }                 // v1, f, token, name...
        val ip = peer.hostAddress ?: "?"
        val now = clock.now()
        if (throttle.blocked(ip, now)) return HttpResponse(429, mapOf("Retry-After" to "60"), close = true)
        val j = if (seg.size >= 3) synchronized(lock) { byToken[seg[2]] } else null
        if (j == null) { throttle.failed(ip, now); return HttpResponse.error(404, "not_found") }
        val v = viewOf(j)
        val f = if (v != null && v.state == DlState.COMPLETED) sinks.fileOf(v.destUri) else null
        if (f == null || !f.isFile) return HttpResponse.error(404, "not_ready")
        val tag = "\"" + java.lang.Long.toHexString(f.length()) + "-" + java.lang.Long.toHexString(f.lastModified()) + "\""
        val shown = v!!.name
        return HttpResponse(
            200,
            mapOf(
                "Content-Type" to (v.mime ?: j.mime ?: "application/octet-stream"), "ETag" to tag,
                "Content-Disposition" to "attachment; filename=\"" + shown.map { if (it.code in 32..126 && it != '"' && it != '\\') it else '_' }.joinToString("") + "\"; filename*=UTF-8''" + pct(shown),
            ),
            FileBody(f), ranged = true,
        )
    }

    // ---------------------------------------------------------------------------------------------------------------- upkeep
    /** Call about once a minute: removes finished jobs nobody collected (24 h) and failed ones (7 days), and jobs whose engine record vanished. */
    fun tick() {
        val now = clock.now()
        val doomed = ArrayList<CJob>()
        synchronized(lock) {
            for (j in jobs.values) {
                val v = viewOf(j)
                if (v == null) { if (now - j.created > 60_000) doomed.add(j); continue }
                val age = now - maxOf(v.updated, j.created)
                if (v.state == DlState.COMPLETED && age > DONE_KEEP_MS) doomed.add(j)
                else if ((v.state == DlState.FAILED || v.state == DlState.CANCELED) && age > FAILED_KEEP_MS) doomed.add(j)
            }
        }
        for (j in doomed) drop(j)
    }

    /** Cancel from the phone's own screen: the same as the tablet's cancel, so the tablet hears "stopped on the phone" instead of the job simply vanishing. */
    fun cancelLocal(engineId: Long) {
        val j = synchronized(lock) { jobs.values.firstOrNull { it.engineId == engineId } }
        if (j != null) synchronized(lock) { j.canceled = true; save() }
        if (j?.kind == "stream") streams?.cancel(engineId) else engine.cancel(engineId)
    }

    /** The phone's own list (name, state, progress) for its screen. */
    fun summaries(): List<DownloadView> = synchronized(lock) { jobs.values.mapNotNull { viewOf(it) } }
    val jobCount: Int get() = synchronized(lock) { jobs.size }
    fun needsService(): Boolean = engine.needsService() || streams?.needsService() == true

    // ---------------------------------------------------------------------------------------------------------------- wire form
    private fun wire(j: CJob): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        m["id"] = j.id; m["cid"] = j.cid; m["kind"] = j.kind; m["created"] = j.created
        val v = viewOf(j)
        if (v == null) {
            // right after enqueue the engine has not registered the job yet; much later it means the record is gone
            m["state"] = if (j.canceled) "CANCELED" else if (clock.now() - j.created < 15_000) "QUEUED" else "GONE"
            m["reason"] = "NONE"; m["name"] = j.name; m["total"] = -1L; m["done"] = 0L; m["bps"] = 0L; m["eta"] = -1L; m["conns"] = 0; m["actions"] = emptyList<String>()
            return m
        }
        m["state"] = v.state.name; m["reason"] = v.reason.name; m["detail"] = v.detail
        m["name"] = j.name.ifEmpty { v.name }; m["file"] = v.name; m["mime"] = v.mime ?: j.mime
        m["total"] = v.total; m["done"] = v.done; m["bps"] = v.bytesPerSec; m["eta"] = v.etaSec; m["conns"] = v.connections; m["estimated"] = v.estimated; m["note"] = v.note
        m["updated"] = v.updated
        m["actions"] = v.actions.filter { it in PHONE_ACTIONS }.map { it.name }
        if (v.state == DlState.COMPLETED) {
            val f = sinks.fileOf(v.destUri)
            if (f != null && f.isFile) {
                val t = ensureToken(j)
                m["fileUrl"] = "/v1/f/$t/" + pct(v.name); m["size"] = f.length()
            } else { m["state"] = "FAILED"; m["reason"] = "STORAGE"; m["actions"] = listOf("RETRY") }
        }
        return m
    }

    private fun ensureToken(j: CJob): String {
        j.token?.let { return it }
        synchronized(lock) {
            j.token?.let { return it }
            val b = ByteArray(16); rng.nextBytes(b)
            val t = Sign.b64(b)
            j.token = t; byToken[t] = j; save()
            return t
        }
    }

    private fun newId(): String { val b = ByteArray(6); rng.nextBytes(b); return Sign.hex(b) }

    /** RFC 3986 percent-encoding of the UTF-8 bytes (unreserved characters kept). */
    private fun pct(s: String): String {
        val sb = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            if (c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code || c in '0'.code..'9'.code || c == '-'.code || c == '.'.code || c == '_'.code || c == '~'.code) sb.append(c.toChar())
            else { sb.append('%'); sb.append("0123456789ABCDEF"[c shr 4]); sb.append("0123456789ABCDEF"[c and 15]) }
        }
        return sb.toString()
    }
}
