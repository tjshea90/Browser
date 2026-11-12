package app.kestrel.core.courier

import app.kestrel.core.download.Clock
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom

/** Why a call to the phone did not work; the tablet decides from this whether to wait, tell the owner something, or download by itself. */
class CourierException(val kind: Kind, val status: Int = 0, message: String? = null, cause: Throwable? = null) : Exception(message ?: kind.name, cause) {
    enum class Kind {
        /** Nothing answered (hotspot off, Courier not running, wrong address). */
        UNREACHABLE,
        /** Something answered too slowly. */
        TIMEOUT,
        /** Something listens there but cannot prove it knows the pairing code: not our phone. */
        IMPOSTOR,
        /** The phone refused the signature: the pairing code on the tablet is not the one the phone shows. */
        AUTH,
        /** The clocks differ and correcting did not help. */
        SKEW,
        /** The phone will not fetch that address (it points into a private network). */
        LOCAL_NET,
        /** Another refusal. */
        REFUSED,
        NO_SPACE, TOO_MANY, THROTTLED, UNSUPPORTED, NOT_FOUND,
        /** The phone failed in itself. */
        SERVER,
        /** The answer was not what the protocol says. */
        PROTOCOL,
    }
}

/** One download as the phone reports it (the wire form of the phone's `wire()`). Unknown or missing fields get harmless defaults. */
class RemoteJob(
    val id: String,
    val cid: String,
    val kind: String,
    val state: String,
    val reason: String,
    val detail: Int,
    val name: String,
    val file: String?,
    val mime: String?,
    val total: Long,
    val done: Long,
    val bps: Long,
    val etaSec: Long,
    val conns: Int,
    val estimated: Boolean,
    val note: String?,
    val actions: List<String>,
    val created: Long,
    val updated: Long,
    /** Path on the phone (`/v1/f/<token>/<name>`) once the file is complete. */
    val fileUrl: String?,
    val size: Long,
) {
    val terminal: Boolean get() = state == "COMPLETED" || state == "FAILED" || state == "CANCELED" || state == "GONE"

    companion object {
        fun parse(m: Map<*, *>): RemoteJob? {
            val id = m.str("id") ?: return null
            return RemoteJob(
                id, m.str("cid") ?: "", m.str("kind") ?: "file", m.str("state") ?: "FAILED", m.str("reason") ?: "NONE", m.int("detail") ?: 0,
                m.str("name") ?: "", m.str("file"), m.str("mime"), m.long("total") ?: -1L, m.long("done") ?: 0L, m.long("bps") ?: 0L, m.long("eta") ?: -1L, m.int("conns") ?: 0,
                m.bool("estimated") ?: false, m.str("note"), (m.list("actions") ?: emptyList<Any?>()).filterIsInstance<String>(), m.long("created") ?: 0L, m.long("updated") ?: 0L,
                m.str("fileUrl"), m.long("size") ?: -1L,
            )
        }
    }
}

/** A video stream (an HLS playlist the owner picked a quality of): exactly what the phone's segment downloader needs (research/PHONE_OFFLOAD.md section 15). */
class StreamSpec(
    val playlistUrl: String,
    /** The playlist text the tablet already read (saves the phone a request; null = the phone fetches it). */
    val playlistText: String?,
    val pageUrl: String?,
    /** File name without extension: the phone adds the right one (.mp4 when it can write one). */
    val nameStem: String,
    val estimateBytes: Long = -1L,
    val repId: String? = null,
    val ext: String? = null,
    /** DASH keeps picture and sound in two files: this is the sound's own request; the phone downloads both and makes ONE MP4 of them. */
    val companion: StreamSpec? = null,
)

/** What the tablet asks the phone to download. */
class JobSpec(
    val cid: String,
    val url: String,
    val name: String? = null,
    val nameFixed: Boolean = false,
    val mime: String? = null,
    val size: Long = -1L,
    val headers: Map<String, String> = emptyMap(),
    val kind: String = "file",
    /** Set for a video stream; then [kind] is sent as "stream" and [url] only names the playlist for display. */
    val stream: StreamSpec? = null,
)

class PhoneStatus(val name: String, val free: Long, val jobs: Int, val running: Int, val queued: Int)
class SpeedResult(val bytes: Long, val ms: Long) { val bytesPerSec: Long get() = if (ms <= 0) 0 else bytes * 1000 / ms }
class ProbeResult(val ok: Boolean, val status: Int, val bytes: Long, val ms: Long, val bps: Long, val error: String?)

/**
 * The tablet's side of the courier protocol (research/PHONE_OFFLOAD.md section 5). One instance talks to one phone address with the key derived from the pairing code.
 * It signs every request, keeps its own timestamps strictly increasing (the phone rejects an exact repeat as a replay), learns the phone's clock offset from `hello`
 * and from a `skew` answer, and turns every failure into a [CourierException] kind. Thread-safe.
 */
class CourierClient(
    private val key: ByteArray,
    private val transport: CourierTransport,
    /** `http://host:port`: where the finished file's address (`/v1/f/...`) is reached. */
    val baseUrl: String = "",
    private val clock: Clock = Clock.System,
    private val rng: SecureRandom = SecureRandom(),
) {
    /** The phone's clock minus the tablet's, in milliseconds (0 until learned). */
    @Volatile var skewMs: Long = 0L
        private set
    private var lastTs = 0L

    constructor(host: String, port: Int, key: ByteArray) : this(key, SocketTransport(host, port), "http://$host:$port")

    @Synchronized private fun nextTs(): Long {
        val now = clock.now() + skewMs
        val t = if (now > lastTs) now else if (lastTs - now < 1000) lastTs + 1 else now         // equal or a hair behind: one more; a real step back (the clock was corrected) is honoured
        lastTs = t
        return t
    }

    // ------------------------------------------------------------------------------------------------------------------ calls
    /** Asks whether our phone is there: only a phone that knows the key can answer correctly. Learns the clock offset. */
    fun hello(): Long {
        val b = ByteArray(16); rng.nextBytes(b)
        val c = Sign.b64(b)
        val t0 = clock.now()
        val r = guard { transport.open("GET", "/v1/hello?c=$c", emptyMap(), null, 700, 5000) }          // a phone that was asleep answers its first request slowly: wait for it (a closed port still answers at once)
        val t1 = clock.now()
        r.use {
            val j = parse(it)
            if (j.str("app") != CourierHost.APP || j.long("v") != CourierHost.VERSION.toLong() || !Sign.helloProofOk(key, c, j.long("t") ?: 0, j.str("proof"))) throw CourierException(CourierException.Kind.IMPOSTOR)
            skewMs = (j.long("t") ?: 0) - (t0 + (t1 - t0) / 2)
            return t1 - t0
        }
    }

    fun status(): PhoneStatus {
        val j = json("GET", "/v1/status", null)
        return PhoneStatus(j.str("name") ?: "phone", j.long("free") ?: 0L, j.int("jobs") ?: 0, j.int("running") ?: 0, j.int("queued") ?: 0)
    }

    private fun streamMap(s: StreamSpec): LinkedHashMap<String, Any?> {
        val sm = LinkedHashMap<String, Any?>()
        sm["playlistUrl"] = s.playlistUrl; s.playlistText?.let { sm["playlistText"] = it }; s.pageUrl?.let { sm["pageUrl"] = it }; sm["nameStem"] = s.nameStem
        if (s.estimateBytes > 0) sm["estimateBytes"] = s.estimateBytes
        s.repId?.let { sm["repId"] = it }; s.ext?.let { sm["ext"] = it }
        s.companion?.let { sm["companion"] = streamMap(it) }
        return sm
    }

    fun create(spec: JobSpec): RemoteJob {
        val m = LinkedHashMap<String, Any?>()
        m["cid"] = spec.cid; m["kind"] = if (spec.stream != null) "stream" else spec.kind; m["url"] = spec.url
        spec.stream?.let { m["stream"] = streamMap(it) }
        spec.name?.let { m["name"] = it }
        if (spec.nameFixed) m["nameFixed"] = true
        spec.mime?.let { m["mime"] = it }
        if (spec.size > 0) m["size"] = spec.size
        if (spec.headers.isNotEmpty()) m["headers"] = spec.headers
        return RemoteJob.parse(json("POST", "/v1/jobs", Json.write(m).toByteArray(Charsets.UTF_8), connectMs = 1500, readMs = 8000)) ?: throw CourierException(CourierException.Kind.PROTOCOL)
    }

    /** The state of the named jobs; a job the phone no longer knows comes back with state `GONE`. */
    fun jobs(ids: List<String>): List<RemoteJob> {
        if (ids.isEmpty()) return emptyList()
        val out = ArrayList<RemoteJob>()
        for (chunk in ids.chunked(100)) {
            val j = json("GET", "/v1/jobs?ids=" + chunk.joinToString(","), null)
            for (x in j.list("jobs") ?: throw CourierException(CourierException.Kind.PROTOCOL)) { out.add(RemoteJob.parse(x as? Map<*, *> ?: continue) ?: continue) }
        }
        return out
    }

    /** `pause`, `resume`, `cancel` or `restart`. */
    fun action(id: String, action: String) { json("POST", "/v1/jobs/$id/$action", ByteArray(0)) }

    fun delete(id: String) { json("DELETE", "/v1/jobs/$id", null) }

    /** Pulls [bytes] bytes over the LAN link and times it. */
    fun speedtest(bytes: Long, readMs: Int = 15_000): SpeedResult {
        val t0 = clock.now()
        var n = 0L
        signed("GET", "/v1/speedtest?bytes=$bytes", null, 1500, readMs).use { r ->
            fail(r)
            val buf = ByteArray(64 * 1024)
            try { while (true) { val k = r.body.read(buf); if (k < 0) break; n += k } } catch (e: IOException) { throw map(e) }
        }
        return SpeedResult(n, maxOf(1L, clock.now() - t0))
    }

    /** Asks the phone to fetch the first [bytes] bytes of [url] and report its own speed. */
    fun probe(url: String, bytes: Long, headers: Map<String, String> = emptyMap()): ProbeResult {
        val body = Json.write(mapOf("url" to url, "bytes" to bytes, "headers" to headers)).toByteArray(Charsets.UTF_8)
        val j = json("POST", "/v1/probe", body, connectMs = 1500, readMs = 30_000)
        return ProbeResult(j.bool("ok") ?: false, j.int("status") ?: 0, j.long("bytes") ?: 0L, j.long("ms") ?: 0L, j.long("bps") ?: 0L, j.str("error"))
    }

    // ------------------------------------------------------------------------------------------------------------------ plumbing
    private fun json(method: String, target: String, body: ByteArray?, connectMs: Int = 1500, readMs: Int = 6000): Map<*, *> =
        signed(method, target, body, connectMs, readMs).use { r -> fail(r); parse(r) }

    private fun parse(r: HttpReply): Map<*, *> = try {
        Json.parse(String(r.readAll(), Charsets.UTF_8)) as? Map<*, *> ?: throw CourierException(CourierException.Kind.PROTOCOL, r.status)
    } catch (e: JsonException) { throw CourierException(CourierException.Kind.PROTOCOL, r.status, e.message) }
    catch (e: IOException) { throw map(e) }

    /** Sends one signed request; a `skew` answer corrects the clock offset and the request is sent again once. */
    private fun signed(method: String, target: String, body: ByteArray?, connectMs: Int, readMs: Int): HttpReply {
        var again = true
        while (true) {
            val ts = nextTs()
            val h = LinkedHashMap<String, String>()
            h["X-K-Ts"] = ts.toString(); h["X-K-Sig"] = Sign.sign(key, method, target, ts, body ?: ByteArray(0))
            if (body != null && body.isNotEmpty()) h["Content-Type"] = "application/json"
            val r = guard { transport.open(method, target, h, body, connectMs, readMs) }
            if (r.status == 401 && again) {
                val j = try { Json.parse(String(r.readAll(), Charsets.UTF_8)) as? Map<*, *> } catch (e: Exception) { null } finally { r.close() }
                val t = j?.long("t")
                if (j?.str("error") == "skew" && t != null) { skewMs = t - clock.now(); again = false; continue }
                throw CourierException(if (j?.str("error") == "skew") CourierException.Kind.SKEW else CourierException.Kind.AUTH, 401)
            }
            return r
        }
    }

    /** Throws for every status that is not a success. Consumes the body for the message. */
    private fun fail(r: HttpReply) {
        if (r.status in 200..299) return
        val j = try { Json.parse(String(r.readAll(), Charsets.UTF_8)) as? Map<*, *> } catch (e: Exception) { null }
        val why = j?.str("error")
        throw CourierException(
            when (r.status) {
                401 -> if (why == "skew") CourierException.Kind.SKEW else CourierException.Kind.AUTH
                403 -> if (why == "local_net") CourierException.Kind.LOCAL_NET else CourierException.Kind.REFUSED
                404 -> CourierException.Kind.NOT_FOUND
                429 -> if (why == "too_many_jobs") CourierException.Kind.TOO_MANY else CourierException.Kind.THROTTLED
                501 -> CourierException.Kind.UNSUPPORTED
                507 -> CourierException.Kind.NO_SPACE
                in 500..599 -> CourierException.Kind.SERVER
                else -> CourierException.Kind.PROTOCOL
            },
            r.status, why,
        )
    }

    private inline fun guard(block: () -> HttpReply): HttpReply = try { block() } catch (e: IOException) { throw map(e) }

    private fun map(e: IOException): CourierException = when (e) {
        is SocketTimeoutException -> CourierException(CourierException.Kind.TIMEOUT, 0, e.message, e)
        is ConnectException, is NoRouteToHostException, is UnknownHostException -> CourierException(CourierException.Kind.UNREACHABLE, 0, e.message, e)
        is java.io.EOFException -> CourierException(CourierException.Kind.UNREACHABLE, 0, e.message, e)
        else -> CourierException(CourierException.Kind.UNREACHABLE, 0, e.message, e)
    }
}
