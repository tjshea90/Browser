package app.kestrel.core.media

import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.FetchResponse
import app.kestrel.core.download.Fetcher
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** A CDN in memory: any number of URLs, Range requests honoured, failures and slowness on demand. */
class FakeCdn : Fetcher {
    val files = ConcurrentHashMap<String, ByteArray>()
    val requests = CopyOnWriteArrayList<String>()
    val failFirst = ConcurrentHashMap<String, Int>()      // url -> fail this many requests with a reset first
    val statusFor = ConcurrentHashMap<String, Int>()      // url -> always answer this status
    val resetAfter = ConcurrentHashMap<String, Int>()    // url -> the FIRST reply of it ends in a reset after this many body bytes (a connection that dies in the middle of a big part)
    val denyFirst = ConcurrentHashMap<String, Int>()      // url -> answer 403 to this many requests first, then serve it (an edge hiccup, a spent token)
    /** Decides per request: a status to answer instead of the file, or null to serve it. Runs before everything else except [failFirst]. */
    @Volatile var gate: ((FetchRequest) -> Int?)? = null
    /** Every request that reached the server: the address and the headers it carried. */
    val seen = CopyOnWriteArrayList<Pair<String, Map<String, String>>>()
    @Volatile var chunkDelayMs = 0L
    @Volatile var chunk = 8 * 1024
    @Volatile var ignoreRange = false
    val redirects = ConcurrentHashMap<String, String>()   // url -> the address the answer really comes from (a 302 the fetcher followed): [FetchResponse.finalUrl] says it
    @Volatile override var windowBytes = 0L
    val concurrent = AtomicInteger()
    val maxConcurrent = AtomicInteger()

    fun count(url: String): Int = requests.count { it.substringBefore(' ') == url }

    override fun open(req: FetchRequest): FetchResponse {
        requests.add(req.url + " " + req.from + "-" + req.toInclusive)
        seen.add(req.url to req.headers)
        failFirst[req.url]?.let { n -> if (n > 0) { failFirst[req.url] = n - 1; throw FetchException(FetchException.Kind.RESET, "fake reset") } }
        gate?.invoke(req)?.let { return Resp(it, req.url, emptyMap(), ByteArray(0), 0, 0, this) }
        denyFirst[req.url]?.let { n -> if (n > 0) { denyFirst[req.url] = n - 1; return Resp(403, req.url, emptyMap(), ByteArray(0), 0, 0, this) } }
        statusFor[req.url]?.let { return Resp(it, req.url, emptyMap(), ByteArray(0), 0, 0, this) }
        val eff = redirects[req.url] ?: req.url
        val d = files[eff] ?: return Resp(404, eff, emptyMap(), ByteArray(0), 0, 0, this)
        val cut = resetAfter.remove(req.url)
        if (req.from >= 0 && !ignoreRange) {
            if (req.from >= d.size) return Resp(416, eff, mapOf("Content-Range" to "bytes */${d.size}"), ByteArray(0), 0, 0, this)
            val last = if (req.toInclusive >= 0) minOf(req.toInclusive, d.size - 1L) else d.size - 1L
            return Resp(206, eff, mapOf("Content-Range" to "bytes ${req.from}-$last/${d.size}"), d, req.from.toInt(), (last - req.from + 1).toInt(), this, cut)
        }
        return Resp(200, eff, emptyMap(), d, 0, d.size, this, cut)
    }

    class Resp(override val status: Int, override val finalUrl: String, private val headers: Map<String, String>, data: ByteArray, off: Int, len: Int, private val cdn: FakeCdn, private val cutAfter: Int? = null) : FetchResponse {
        private var served = 0
        @Volatile private var aborted = false
        private var closed = false
        private val inner = ByteArrayInputStream(data, off, len)
        init { val c = cdn.concurrent.incrementAndGet(); cdn.maxConcurrent.updateAndGet { maxOf(it, c) } }
        override fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value
        override val body: InputStream = object : InputStream() {
            override fun read(): Int { throw UnsupportedOperationException() }
            override fun read(b: ByteArray, o: Int, l: Int): Int {
                if (aborted) throw IOException("aborted")
                if (cdn.chunkDelayMs > 0) try { Thread.sleep(cdn.chunkDelayMs) } catch (e: InterruptedException) { /* ignore */ }
                if (aborted) throw IOException("aborted")
                if (cutAfter != null && served >= cutAfter) throw IOException("fake reset in the middle of the body")
                val want = if (cutAfter != null) minOf(l, cdn.chunk, cutAfter - served) else minOf(l, cdn.chunk)
                val n = inner.read(b, o, want)
                if (n > 0) served += n
                return n
            }
        }
        override fun abort() { aborted = true }
        override fun close() { if (!closed) { closed = true; cdn.concurrent.decrementAndGet() } }
    }
}

/** Job records and staging folders in memory / a temp folder. */
class MemStreamStore : StreamStore {
    val rows = ConcurrentHashMap<Long, StreamRec>()
    private val root = java.nio.file.Files.createTempDirectory("kestrel-stream-test").toFile()
    override fun loadAll(): List<StreamRec> = rows.values.map { it.snapshot() }
    override fun save(r: StreamRec) { rows[r.id] = r.snapshot() }
    override fun delete(id: Long) { rows.remove(id) }
    override fun dir(id: Long): File = File(root, id.toString()).also { it.mkdirs() }
    fun cleanup() { root.deleteRecursively() }
    fun stagedFiles(id: Long): List<String> = dir(id).list()?.toList() ?: emptyList()
}
