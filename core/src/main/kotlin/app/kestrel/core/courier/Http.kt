package app.kestrel.core.courier

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile

/** A request the server turns down with [status] (the parser throws it, the connection loop answers and closes). */
class HttpException(val status: Int, message: String) : Exception(message)

class HttpRequest(
    val method: String,
    /** As sent: the path plus an optional `?query`. This exact text is what the signature covers. */
    val target: String,
    /** Percent-decoded path without the query. */
    val path: String,
    val query: Map<String, String>,
    /** Header names in lower case. */
    val headers: Map<String, String>,
    val body: ByteArray,
    val http11: Boolean,
) {
    fun header(name: String): String? = headers[name.lowercase()]

    val keepAlive: Boolean
        get() {
            val c = headers["connection"]?.lowercase()
            return if (c != null && c.contains("close")) false else if (http11) true else c != null && c.contains("keep-alive")
        }
}

/** Reads HTTP/1.1 requests from a (buffered) stream with hard limits, because the peer is a device on a wifi network, not a friend. */
object HttpParser {
    const val MAX_LINE = 8192
    const val MAX_HEAD = 16 * 1024
    const val MAX_HEADERS = 64
    const val MAX_BODY = 2 * 1024 * 1024
    const val MAX_QUERY_PAIRS = 32
    private val METHODS = setOf("GET", "HEAD", "POST", "DELETE", "PUT", "OPTIONS")
    private val EMPTY = ByteArray(0)

    /** The next request on [input], or null when the peer closed the connection cleanly before sending anything. */
    @Throws(IOException::class, HttpException::class)
    fun read(input: InputStream): HttpRequest? {
        var budget = MAX_HEAD
        var line = readLine(input, budget) ?: return null
        budget -= line.length + 2
        var blanks = 0
        while (line.isEmpty()) {                                  // RFC 7230 3.5: ignore a few stray empty lines before the request line
            if (++blanks > 4) throw HttpException(400, "empty request")
            line = readLine(input, budget) ?: return null
            budget -= line.length + 2
        }
        val parts = line.split(' ')
        if (parts.size != 3) throw HttpException(400, "bad request line")
        val method = parts[0]; val target = parts[1]; val ver = parts[2]
        if (method !in METHODS) throw HttpException(501, "method not supported")
        if (ver != "HTTP/1.1" && ver != "HTTP/1.0") throw HttpException(505, "http version not supported")
        if (target.isEmpty() || target[0] != '/') throw HttpException(400, "bad target")
        for (c in target) if (c < '!' || c == '\u007f' || c == '\\') throw HttpException(400, "bad character in the target")

        val headers = LinkedHashMap<String, String>()
        var count = 0
        while (true) {
            val h = readLine(input, budget) ?: throw EOFException("connection closed in the headers")
            budget -= h.length + 2
            if (budget < 0) throw HttpException(431, "headers too large")
            if (h.isEmpty()) break
            if (++count > MAX_HEADERS) throw HttpException(431, "too many headers")
            if (h[0] == ' ' || h[0] == '\t') throw HttpException(400, "folded headers are not supported")
            val colon = h.indexOf(':')
            if (colon <= 0) throw HttpException(400, "bad header")
            val name = h.substring(0, colon)
            if (!name.all { isTokenChar(it) }) throw HttpException(400, "bad header name")
            val value = h.substring(colon + 1).trim()
            val key = name.lowercase()
            val old = headers[key]
            if (old == null) headers[key] = value
            else if (key == "content-length") { if (old != value) throw HttpException(400, "conflicting content-length") }
            else headers[key] = "$old, $value"
        }

        if (headers.containsKey("transfer-encoding")) throw HttpException(501, "chunked bodies are not supported")
        var body = EMPTY
        val cl = headers["content-length"]
        if (cl != null) {
            if (cl.isEmpty() || !cl.all { it in '0'..'9' } || cl.length > 9) throw HttpException(400, "bad content-length")
            val n = cl.toInt()
            if (n > MAX_BODY) throw HttpException(413, "body too large")
            if (n > 0) { body = ByteArray(n); readFully(input, body) }
        } else if (method == "POST" || method == "PUT") throw HttpException(411, "length required")

        val q = target.indexOf('?')
        val rawPath = if (q < 0) target else target.substring(0, q)
        val path = percentDecode(rawPath, false)
        if (path.indexOf('\u0000') >= 0) throw HttpException(400, "bad path")
        return HttpRequest(method, target, path, if (q < 0) emptyMap() else parseQuery(target.substring(q + 1)), headers, body, ver == "HTTP/1.1")
    }

    fun parseQuery(raw: String): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        if (raw.isEmpty()) return m
        var n = 0
        for (pair in raw.split('&')) {
            if (pair.isEmpty()) continue
            if (++n > MAX_QUERY_PAIRS) throw HttpException(400, "too many query parameters")
            val eq = pair.indexOf('=')
            val k = percentDecode(if (eq < 0) pair else pair.substring(0, eq), true)
            val v = if (eq < 0) "" else percentDecode(pair.substring(eq + 1), true)
            m[k] = v
        }
        return m
    }

    fun percentDecode(s: String, plusAsSpace: Boolean): String {
        if (s.indexOf('%') < 0 && !(plusAsSpace && s.indexOf('+') >= 0)) return s
        val out = ByteArrayOutputStream(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%') {
                if (i + 2 >= s.length) throw HttpException(400, "bad percent escape")
                val hi = Character.digit(s[i + 1], 16); val lo = Character.digit(s[i + 2], 16)
                if (hi < 0 || lo < 0) throw HttpException(400, "bad percent escape")
                out.write(hi * 16 + lo); i += 3
            } else if (c == '+' && plusAsSpace) { out.write(' '.code); i++ }
            else if (c.code < 0x80) { out.write(c.code); i++ }
            else { val cp = s.codePointAt(i); val b = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8); out.write(b, 0, b.size); i += Character.charCount(cp) }
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    private fun isTokenChar(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || "!#$%&'*+-.^_`|~".indexOf(c) >= 0

    /** One line without its terminator; null when the stream ended before the first byte of the line. */
    private fun readLine(input: InputStream, max: Int): String? {
        val sb = StringBuilder()
        var n = 0
        while (true) {
            val c = input.read()
            if (c < 0) { if (n == 0) return null else throw EOFException("connection closed in the middle of a line") }
            if (c == '\n'.code) {
                if (sb.isNotEmpty() && sb[sb.length - 1] == '\r') sb.setLength(sb.length - 1)
                return sb.toString()
            }
            if (++n > max || n > MAX_LINE) throw HttpException(431, "line too long")
            sb.append(c.toChar())                                   // bytes as Latin-1: the protocol's headers are ASCII
        }
    }

    private fun readFully(input: InputStream, b: ByteArray) {
        var off = 0
        while (off < b.size) {
            val n = input.read(b, off, b.size - off)
            if (n < 0) throw EOFException("connection closed in the body")
            off += n
        }
    }
}

/** What goes after the headers. [writeTo] is asked for a byte range so one implementation serves 200 and 206. */
interface HttpBody {
    val length: Long
    @Throws(IOException::class) fun writeTo(out: OutputStream, from: Long, count: Long)
}

class BytesBody(private val bytes: ByteArray) : HttpBody {
    override val length: Long get() = bytes.size.toLong()
    override fun writeTo(out: OutputStream, from: Long, count: Long) { out.write(bytes, from.toInt(), count.toInt()) }
}

/** A finished file, streamed from disk (the length is fixed when the body is made: a COMPLETED job's file never changes). */
class FileBody(private val file: File, override val length: Long = file.length()) : HttpBody {
    override fun writeTo(out: OutputStream, from: Long, count: Long) {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(from)
            val buf = ByteArray(64 * 1024)
            var left = count
            while (left > 0) {
                val n = raf.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                if (n < 0) throw EOFException("the file is shorter than announced")
                out.write(buf, 0, n)
                left -= n
            }
        }
    }
}

class HttpResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: HttpBody? = null,
    /** Close the connection after this answer. */
    val close: Boolean = false,
    /** A `Range` request header is honoured for this answer (files), turning it into 206 or 416. */
    val ranged: Boolean = false,
) {
    companion object {
        fun json(status: Int, value: Any?): HttpResponse =
            HttpResponse(status, mapOf("Content-Type" to "application/json; charset=utf-8"), BytesBody(Json.write(value).toByteArray(Charsets.UTF_8)))

        fun error(status: Int, why: String): HttpResponse = json(status, mapOf("error" to why))
    }
}

sealed class RangeSpec {
    class Part(val from: Long, val to: Long) : RangeSpec()
    object Unsatisfiable : RangeSpec()
    object Ignore : RangeSpec()

    companion object {
        /** RFC 7233 for a single byte range; anything odd (other units, several ranges, nonsense) is ignored, which is a valid reason to answer 200. */
        fun parse(header: String, length: Long): RangeSpec {
            val h = header.trim()
            if (!h.startsWith("bytes=", ignoreCase = true)) return Ignore
            val spec = h.substring(6).trim()
            if (spec.indexOf(',') >= 0) return Ignore
            val dash = spec.indexOf('-')
            if (dash < 0) return Ignore
            val a = spec.substring(0, dash).trim(); val b = spec.substring(dash + 1).trim()
            if (a.isEmpty()) {                                       // suffix: the last n bytes
                if (b.isEmpty() || !b.all { it in '0'..'9' } || b.length > 18) return Ignore
                val n = b.toLong()
                if (n == 0L || length == 0L) return Unsatisfiable
                return Part(maxOf(0L, length - n), length - 1)
            }
            if (!a.all { it in '0'..'9' } || a.length > 18) return Ignore
            val from = a.toLong()
            if (b.isEmpty()) return if (from >= length) Unsatisfiable else Part(from, length - 1)     // open-ended: "from here to the end"
            if (!b.all { it in '0'..'9' } || b.length > 18) return Ignore
            val to = b.toLong()
            if (to < from) return Ignore
            if (from >= length) return Unsatisfiable
            return Part(from, minOf(to, length - 1))
        }
    }
}

object HttpWriter {
    private val REASONS = mapOf(
        200 to "OK", 204 to "No Content", 206 to "Partial Content", 400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden", 404 to "Not Found",
        405 to "Method Not Allowed", 409 to "Conflict", 411 to "Length Required", 413 to "Payload Too Large", 416 to "Range Not Satisfiable",
        429 to "Too Many Requests", 431 to "Request Header Fields Too Large", 500 to "Internal Server Error", 501 to "Not Implemented",
        503 to "Service Unavailable", 505 to "HTTP Version Not Supported", 507 to "Insufficient Storage",
    )

    /** Writes one answer. [req] may be null for errors raised before a request was understood. Returns whether the connection stays open. */
    @Throws(IOException::class)
    fun write(out: OutputStream, req: HttpRequest?, resp: HttpResponse, keepAlive: Boolean): Boolean {
        var status = resp.status
        val headers = LinkedHashMap<String, String>(resp.headers)
        var body = resp.body
        var from = 0L
        var count = body?.length ?: 0L
        val head = req?.method == "HEAD"
        if (body != null && resp.ranged) headers["Accept-Ranges"] = "bytes"
        if (body != null && resp.ranged && req != null && status == 200 && (req.method == "GET" || req.method == "HEAD")) {
            val r = req.headers["range"]
            if (r != null) {
                when (val spec = RangeSpec.parse(r, body.length)) {
                    is RangeSpec.Part -> { status = 206; from = spec.from; count = spec.to - spec.from + 1; headers["Content-Range"] = "bytes ${spec.from}-${spec.to}/${body.length}" }
                    RangeSpec.Unsatisfiable -> { headers["Content-Range"] = "bytes */${body.length}"; status = 416; body = null; count = 0 }
                    RangeSpec.Ignore -> {}
                }
            }
        }
        val open = keepAlive && !resp.close
        headers["Content-Length"] = count.toString()
        headers["Connection"] = if (open) "keep-alive" else "close"
        if (!headers.containsKey("Cache-Control")) headers["Cache-Control"] = "no-store"
        headers["X-Content-Type-Options"] = "nosniff"
        val sb = StringBuilder("HTTP/1.1 ").append(status).append(' ').append(REASONS[status] ?: "Status").append("\r\n")
        for ((k, v) in headers) {
            require(k.none { it == '\r' || it == '\n' || it == ':' } && v.none { it == '\r' || it == '\n' }) { "bad header" }
            sb.append(k).append(": ").append(v).append("\r\n")
        }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        if (!head && body != null && count > 0) body.writeTo(out, from, count)
        out.flush()
        return open
    }
}
