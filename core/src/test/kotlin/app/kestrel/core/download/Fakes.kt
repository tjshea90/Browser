package app.kestrel.core.download

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Deterministic pseudo-random bytes (so a wrong offset is visible as a wrong byte). */
fun testBytes(n: Int, seed: Long = 1): ByteArray { val r = java.util.Random(seed); val b = ByteArray(n); r.nextBytes(b); return b }

/** One request the fake server saw. */
class Seen(val url: String, val from: Long, val to: Long, val cond: String?)

/** A tiny in-memory HTTP server: honours Range/If-Match, can lie, fail, truncate and be slow. */
class FakeServer(@Volatile var data: ByteArray) : Fetcher {
    @Volatile var supportRange = true
    @Volatile var acceptRangesHeader = true
    @Volatile var etag: String? = "\"v1\""
    @Volatile var lastModified: String? = null
    @Volatile var sendLength = true
    @Volatile var chunkDelayMs = 0L
    @Volatile var chunk = 16 * 1024
    @Volatile var failNext = 0                    // the next N requests throw FetchException(RESET)
    @Volatile var statusNext = ArrayList<Int>()   // statuses to answer the next requests with (consumed in order)
    @Volatile var retryAfter: String? = null
    @Volatile var truncateFirstAt = -1L           // the first successful reply ends after this many bytes
    @Volatile var disposition: String? = null
    @Volatile var contentType: String? = "application/octet-stream"
    @Volatile var blockUrl: String? = null        // requests to this exact URL get a 403 (an expired signed link)
    @Volatile var redirectTo: String? = null
    @Volatile var failKind: FetchException.Kind? = null
    @Volatile var connLimit = 0                   // > 0: a request that arrives while this many replies are open is refused with [overLimitStatus] (a server that caps connections per client)
    @Volatile var overLimitStatus = 503
    @Volatile var down = false                    // every request throws a timeout (the hotspot's upstream is dead while Wi-Fi stays up)
    val refused = AtomicInteger()
    @Volatile override var probeWindow = 0L
    @Volatile override var windowBytes = 0L
    val seen = CopyOnWriteArrayList<Seen>()
    val servedBytes = java.util.concurrent.atomic.AtomicLong()
    val concurrent = AtomicInteger()
    val maxConcurrent = AtomicInteger()
    private val truncated = AtomicBoolean()

    override fun open(req: FetchRequest): FetchResponse {
        val cond = when (val c = req.cond) { is Cond.IfMatch -> "If-Match: ${c.etag}"; is Cond.IfUnmodifiedSince -> "If-Unmodified-Since: ${c.date}"; null -> null }
        seen.add(Seen(req.url, req.from, req.toInclusive, cond))
        failKind?.let { throw FetchException(it, "fake failure") }
        if (down) throw FetchException(FetchException.Kind.TIMEOUT, "fake outage")
        if (connLimit > 0 && concurrent.get() >= connLimit) { refused.incrementAndGet(); return Resp(overLimitStatus, req.url, mapOf("Retry-After" to "0"), ByteArray(0), 0, 0) }
        if (failNext > 0) { failNext--; throw FetchException(FetchException.Kind.RESET, "fake reset") }
        val d = data
        val finalUrl = redirectTo ?: req.url
        statusNext.let { l -> synchronized(l) { if (l.isNotEmpty()) { val st = l.removeAt(0); return Resp(st, finalUrl, mapOf("Retry-After" to (retryAfter ?: "0")), ByteArray(0), 0, 0) } } }
        if (blockUrl != null && finalUrl == blockUrl) return Resp(403, finalUrl, emptyMap(), ByteArray(0), 0, 0)
        val h = HashMap<String, String>()
        etag?.let { h["ETag"] = it }
        lastModified?.let { h["Last-Modified"] = it; h["Date"] = "Thu, 01 Jan 2026 00:00:00 GMT" }
        disposition?.let { h["Content-Disposition"] = it }
        contentType?.let { h["Content-Type"] = it }
        // If-Match mismatch -> 412 (no body), like a real server
        if (req.cond is Cond.IfMatch && etag != null && (req.cond as Cond.IfMatch).etag != etag) return Resp(412, finalUrl, emptyMap(), ByteArray(0), 0, 0)
        if (req.from >= 0 && supportRange) {
            if (req.from >= d.size) { h["Content-Range"] = "bytes */${d.size}"; return Resp(416, finalUrl, h, ByteArray(0), 0, 0) }
            val last = if (req.toInclusive >= 0) minOf(req.toInclusive, d.size - 1L) else d.size - 1L
            h["Content-Range"] = "bytes ${req.from}-$last/${d.size}"
            if (acceptRangesHeader) h["Accept-Ranges"] = "bytes"
            h["Content-Length"] = (last - req.from + 1).toString()
            return Resp(206, finalUrl, h, d, req.from.toInt(), (last + 1).toInt())
        }
        if (acceptRangesHeader && supportRange) h["Accept-Ranges"] = "bytes"
        if (sendLength) h["Content-Length"] = d.size.toString()
        var end = d.size
        if (truncateFirstAt >= 0 && truncated.compareAndSet(false, true)) end = minOf(end.toLong(), truncateFirstAt).toInt()
        return Resp(200, finalUrl, h, d, 0, end)
    }

    private inner class Resp(override val status: Int, override val finalUrl: String, val headers: Map<String, String>, val src: ByteArray, val from: Int, val to: Int) : FetchResponse {
        @Volatile var aborted = false
        private var closedCount = false
        override fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value
        override val body: InputStream = object : InputStream() {
            var pos = from
            override fun read(): Int { val b = ByteArray(1); return if (read(b, 0, 1) < 0) -1 else b[0].toInt() and 0xff }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (aborted) throw IOException("aborted")
                if (pos >= to) return -1
                if (chunkDelayMs > 0) { try { Thread.sleep(chunkDelayMs) } catch (e: InterruptedException) { throw IOException("interrupted") }; if (aborted) throw IOException("aborted") }
                val n = minOf(len, to - pos, chunk)
                System.arraycopy(src, pos, b, off, n)
                pos += n
                servedBytes.addAndGet(n.toLong())
                return n
            }
        }
        init { val c = concurrent.incrementAndGet(); maxConcurrent.updateAndGet { maxOf(it, c) } }
        override fun abort() { aborted = true }
        override fun close() { if (!closedCount) { closedCount = true; concurrent.decrementAndGet() } }
    }
}

class FakeSink(val name: String, total: Long, val uriValue: String) : Sink {
    @Volatile var bytes = ByteArray(if (total > 0) total.toInt() else 0)
    @Volatile var durable = ByteArray(0)
    @Volatile var published = false
    @Volatile var discarded = false
    @Volatile var failWrites: SinkException? = null
    override val uri: String? get() = uriValue
    override fun pwrite(pos: Long, b: ByteArray, off: Int, len: Int) {
        failWrites?.let { throw it }
        synchronized(this) {
            val end = pos.toInt() + len
            if (end > bytes.size) bytes = bytes.copyOf(end)
            System.arraycopy(b, off, bytes, pos.toInt(), len)
        }
    }
    override fun sync() { synchronized(this) { durable = bytes.copyOf() } }
    override fun length(): Long = bytes.size.toLong()
    override fun publish(): String? { published = true; return uriValue }
    override fun discard() { discarded = true }
}

class FakeSinks : SinkFactory {
    val sinks = ConcurrentHashMap<String, FakeSink>()
    @Volatile var failCreate: SinkException? = null
    val deleted = CopyOnWriteArrayList<String>()
    /** The destination kind of every sink the engine asked for, in order. */
    val kinds = CopyOnWriteArrayList<Int>()
    private val n = AtomicInteger()
    override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink {
        failCreate?.let { throw it }
        val uri = "mem://${n.incrementAndGet()}/$name"
        return FakeSink(name, total, uri).also { sinks[uri] = it }
    }
    override fun create(name: String, mime: String?, total: Long, priv: Boolean, kind: Int): Sink { kinds.add(kind); return create(name, mime, total, priv) }
    override fun reopen(uri: String, total: Long): Sink? = sinks[uri]?.takeIf { !it.discarded }
    override fun delete(uri: String): Boolean { deleted.add(uri); sinks[uri]?.discarded = true; return true }
}

class FakeStore : Store {
    val rows = ConcurrentHashMap<Long, Rec>()
    override fun loadAll(): List<Rec> = rows.values.map { it.snapshot() }
    override fun insert(r: Rec) { rows[r.id] = r.snapshot() }
    override fun save(r: Rec) { rows[r.id] = r.snapshot() }
    override fun delete(id: Long) { rows.remove(id) }
}

class FakeNet : NetGate { @Volatile override var online = true; override val metered = true }

/** A manual clock for the planner tests. */
class ManualClock(var t: Long = 1_000_000L) : Clock { override fun now() = t }
