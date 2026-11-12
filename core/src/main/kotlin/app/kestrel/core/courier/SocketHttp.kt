package app.kestrel.core.courier

import app.kestrel.core.download.Cond
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.FetchResponse
import app.kestrel.core.download.Fetcher
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** One HTTP answer whose body is still on the wire. Closing it (or [abort], from another thread) closes the connection. */
class HttpReply(val status: Int, val headers: Map<String, String>, val body: InputStream, private val socket: Socket) : Closeable {
    fun header(name: String): String? = headers[name.lowercase()]
    fun abort() { try { socket.close() } catch (_: IOException) {} }
    override fun close() = abort()

    /** The whole body as bytes, at most [max] (a longer answer is an error: these are small JSON messages). */
    fun readAll(max: Int = 1 shl 20): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = body.read(buf)
            if (n < 0) break
            if (out.size() + n > max) throw IOException("answer too long")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}

/**
 * A minimal HTTP/1.1 client on a plain socket, one request per connection (`Connection: close`). The tablet talks to the phone with this instead of
 * HttpURLConnection on purpose: Android refuses cleartext HTTP for HttpURLConnection by default, and the phone's address is a LAN address that cannot be listed in a
 * network security configuration. Using it keeps the app's "HTTPS only" platform policy untouched for everything else. It never uses a proxy and never follows redirects.
 */
object SocketHttp {
    private const val MAX_LINE = 8192
    private const val MAX_HEADERS = 64

    @Throws(IOException::class)
    fun open(host: String, port: Int, method: String, target: String, headers: Map<String, String>, body: ByteArray?, connectMs: Int, readMs: Int): HttpReply {
        val s = Socket()
        try {
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(host, port), connectMs)
            s.soTimeout = readMs
            val sb = StringBuilder()
            sb.append(method).append(' ').append(target).append(" HTTP/1.1\r\nHost: ").append(host).append(':').append(port).append("\r\nConnection: close\r\n")
            if (body != null || method == "POST" || method == "PUT") sb.append("Content-Length: ").append(body?.size ?: 0).append("\r\n")
            for ((k, v) in headers) {
                require(k.none { it == '\r' || it == '\n' || it == ':' } && v.none { it == '\r' || it == '\n' }) { "bad header" }
                sb.append(k).append(": ").append(v).append("\r\n")
            }
            sb.append("\r\n")
            val out = BufferedOutputStream(s.getOutputStream(), 8192)
            out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
            if (body != null) out.write(body)
            out.flush()

            val input = BufferedInputStream(s.getInputStream(), 32 * 1024)
            val status = statusOf(readLine(input) ?: throw EOFException("the connection closed without an answer"))
            val h = LinkedHashMap<String, String>()
            while (true) {
                val l = readLine(input) ?: throw EOFException("the answer ended in the headers")
                if (l.isEmpty()) break
                if (h.size >= MAX_HEADERS) throw IOException("too many headers")
                val c = l.indexOf(':')
                if (c <= 0) throw IOException("bad header line")
                val k = l.substring(0, c).lowercase()
                val v = l.substring(c + 1).trim()
                h[k] = if (h.containsKey(k)) h[k] + ", " + v else v
            }
            if (h.containsKey("transfer-encoding")) throw IOException("chunked answers are not supported")
            val len = h["content-length"]?.let { it.toLongOrNull()?.takeIf { n -> n >= 0 } ?: throw IOException("bad content-length") }
            val bodyLen = if (method == "HEAD" || status in 100..199 || status == 204 || status == 304) 0L else len ?: -1L
            return HttpReply(status, h, Bounded(input, bodyLen), s)
        } catch (e: Throwable) {
            try { s.close() } catch (_: IOException) {}
            throw e
        }
    }

    private fun statusOf(line: String): Int {
        if (!line.startsWith("HTTP/1.")) throw IOException("not an HTTP answer")
        val p = line.split(' ', limit = 3)
        return p.getOrNull(1)?.toIntOrNull()?.takeIf { it in 100..599 } ?: throw IOException("bad status line")
    }

    private fun readLine(i: InputStream): String? {
        val sb = StringBuilder()
        var n = 0
        while (true) {
            val c = i.read()
            if (c < 0) { if (n == 0) return null else throw EOFException("the answer ended in the middle of a line") }
            if (c == '\n'.code) { if (sb.isNotEmpty() && sb[sb.length - 1] == '\r') sb.setLength(sb.length - 1); return sb.toString() }
            if (++n > MAX_LINE) throw IOException("line too long")
            sb.append(c.toChar())
        }
    }

    /** Exactly [limit] bytes (or to the end of the connection when unknown, -1); an answer that stops early is an [IOException], never a short file. */
    private class Bounded(val i: InputStream, val limit: Long) : InputStream() {
        private var left = limit
        override fun read(): Int { val b = ByteArray(1); return if (read(b, 0, 1) < 0) -1 else b[0].toInt() and 0xFF }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (limit < 0) return i.read(b, off, len)
            if (left <= 0) return -1
            val n = i.read(b, off, minOf(len.toLong(), left).toInt())
            if (n < 0) throw EOFException("the connection closed $left bytes early")
            left -= n
            return n
        }
        override fun close() { i.close() }
    }
}

/** The client side of [CourierClient]: one request, answered with the body still streaming. Tests replace it. */
interface CourierTransport {
    @Throws(IOException::class) fun open(method: String, target: String, headers: Map<String, String>, body: ByteArray?, connectMs: Int, readMs: Int): HttpReply
}

class SocketTransport(private val host: String, private val port: Int) : CourierTransport {
    override fun open(method: String, target: String, headers: Map<String, String>, body: ByteArray?, connectMs: Int, readMs: Int): HttpReply =
        SocketHttp.open(host, port, method, target, headers, body, connectMs, readMs)
}

/**
 * A [Fetcher] for the finished file on the phone: plain HTTP over [SocketHttp], so the tablet's ordinary download engine (ranges, pause, resume, kill-safe) copies it.
 * It refuses any address that is not on a private network or this device, so a cleartext request can never leave the LAN through it.
 */
class LanFetcher(private val resolve: (String) -> List<ByteArray> = { InetAddress.getAllByName(it).map { a -> a.address } }) : Fetcher {
    override fun open(req: FetchRequest): FetchResponse {
        val u = try { URL(req.url) } catch (e: Exception) { throw FetchException(FetchException.Kind.BAD_URL, "bad address") }
        if (u.protocol != "http") throw FetchException(FetchException.Kind.BAD_URL, "not a LAN address")
        val host = u.host.removePrefix("[").removeSuffix("]")
        val addrs = try { resolve(host) } catch (e: UnknownHostException) { throw FetchException(FetchException.Kind.DNS, e.message, e) }
        if (addrs.isEmpty() || addrs.any { !PeerGuard.isPrivate(it) && !PeerGuard.isLoopback(it) }) throw FetchException(FetchException.Kind.BAD_URL, "not on the local network")
        val h = LinkedHashMap<String, String>(req.headers)
        if (req.from >= 0) h["Range"] = "bytes=${req.from}-" + (if (req.toInclusive >= 0) req.toInclusive.toString() else "")
        when (val c = req.cond) {
            is Cond.IfMatch -> h["If-Match"] = c.etag
            is Cond.IfUnmodifiedSince -> h["If-Unmodified-Since"] = c.date
            null -> {}
        }
        try {
            val r = SocketHttp.open(host, if (u.port > 0) u.port else 80, "GET", u.file.ifEmpty { "/" }, h, null, minOf(req.connectMs, 5_000), req.readMs)
            return Resp(r, req.url)
        } catch (e: SocketTimeoutException) { throw FetchException(FetchException.Kind.TIMEOUT, e.message, e) }
        catch (e: ConnectException) { throw FetchException(FetchException.Kind.OFFLINE, e.message, e) }
        catch (e: NoRouteToHostException) { throw FetchException(FetchException.Kind.OFFLINE, e.message, e) }
        catch (e: UnknownHostException) { throw FetchException(FetchException.Kind.DNS, e.message, e) }
        catch (e: IOException) { throw FetchException(FetchException.Kind.RESET, e.message, e) }
    }

    private class Resp(val r: HttpReply, override val finalUrl: String) : FetchResponse {
        override val status: Int get() = r.status
        override fun header(name: String): String? = r.header(name)
        override val body: InputStream? get() = r.body
        override fun abort() = r.abort()
        override fun close() = r.close()
    }
}
