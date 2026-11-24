package app.kestrel.browser.optimizer

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * OPT1 (research/OPTIMIZER.md): the benchmark pages of Auto-tune are served by the app itself, on 127.0.0.1 only (a random port, a random token as the first part of the path). A page reports its numbers
 * with `POST /<token>/r`. Nothing else is served: files come from assets/bench, a wrong token or name is a 404. The sockets never leave the machine: the server is bound to the loopback address.
 */
class BenchServer(private val ctx: Context) {
    class Result(val page: String, val n: String, val m: Map<String, Double>, val err: String, val note: String)

    val token: String = ByteArray(9).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    @Volatile var port: Int = 0; private set
    private var server: ServerSocket? = null
    private val results = ConcurrentHashMap<String, Result>()
    private val lock = Object()

    fun start() {
        if (server != null) return
        val s = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        server = s; port = s.localPort
        Thread({
            while (!s.isClosed) {
                try { val c = s.accept(); Thread({ handle(c) }, "bench-conn").apply { isDaemon = true }.start() } catch (e: Exception) { if (s.isClosed) break }
            }
        }, "bench-server").apply { isDaemon = true; start() }
    }

    fun stop() { try { server?.close() } catch (e: Exception) { /* closing */ }; server = null }

    fun url(page: String, seq: Int, extra: String = ""): String = "http://127.0.0.1:$port/$token/$page.html?n=$seq$extra"

    /** Waits up to [timeoutMs] for the result of page [page] with sequence number [seq] (a stale answer of an earlier load of the same page never counts). */
    fun await(page: String, seq: Int, timeoutMs: Long): Result? {
        val key = "$page#$seq"; val end = System.currentTimeMillis() + timeoutMs
        synchronized(lock) {
            while (true) {
                results[key]?.let { return it }
                val left = end - System.currentTimeMillis(); if (left <= 0) return null
                try { lock.wait(minOf(left, 500L)) } catch (e: InterruptedException) { return null }
            }
        }
    }

    /** The page cannot answer (its process died): the waiter gets an error result at once instead of a timeout. */
    fun fail(page: String, seq: Int, why: String) {
        results["$page#$seq"] = Result(page, seq.toString(), emptyMap(), why, "")
        synchronized(lock) { lock.notifyAll() }
    }

    private fun handle(c: Socket) {
        try {
            c.soTimeout = 10_000
            val inp = c.getInputStream(); val out = c.getOutputStream()
            val head = readHead(inp) ?: return
            val lines = head.split("\r\n")
            val req = lines[0].split(' ')
            if (req.size < 2) return respond(out, 400, "text/plain", "bad".toByteArray())
            val method = req[0]; val rawPath = req[1]
            val path = rawPath.substringBefore('?')
            val parts = path.split('/').filter { it.isNotEmpty() }
            if (parts.isEmpty() || parts[0] != token) return respond(out, 404, "text/plain", "no".toByteArray())
            val headers = HashMap<String, String>()
            for (l in lines.drop(1)) { val i = l.indexOf(':'); if (i > 0) headers[l.substring(0, i).trim().lowercase()] = l.substring(i + 1).trim() }
            if (method == "POST" && parts.size == 2 && parts[1] == "r") {
                val len = (headers["content-length"] ?: "0").toIntOrNull()?.coerceIn(0, 65536) ?: 0
                val body = readBody(inp, len)
                try {
                    val j = JSONObject(String(body, Charsets.UTF_8)); val m = HashMap<String, Double>()
                    val mj = j.optJSONObject("m"); if (mj != null) for (k in mj.keys()) { val d = mj.optDouble(k, Double.NaN); if (!d.isNaN() && !d.isInfinite()) m[k] = d }
                    val r = Result(j.optString("page"), j.optString("n"), m, j.optString("err"), j.optString("note"))
                    results[r.page + "#" + r.n] = r
                    synchronized(lock) { lock.notifyAll() }
                } catch (e: Exception) { /* a malformed report is ignored: the page times out and the sample fails with a reason */ }
                return respond(out, 200, "text/plain", "ok".toByteArray())
            }
            if (method != "GET" || parts.size != 2) return respond(out, 404, "text/plain", "no".toByteArray())
            val name = parts[1]
            if (!name.matches(Regex("[a-z0-9_]+\\.(html|js|mp4)"))) return respond(out, 404, "text/plain", "no".toByteArray())
            val data = try { ctx.assets.open("bench/$name").use { it.readBytes() } } catch (e: Exception) { return respond(out, 404, "text/plain", "missing".toByteArray()) }
            val type = when (name.substringAfterLast('.')) { "html" -> "text/html; charset=utf-8"; "js" -> "text/javascript; charset=utf-8"; else -> "video/mp4" }
            val range = headers["range"]
            if (range != null && range.startsWith("bytes=")) {
                val a = range.substring(6).substringBefore('-').toIntOrNull() ?: 0
                val b = range.substringAfter('-', "").toIntOrNull() ?: (data.size - 1)
                val from = a.coerceIn(0, data.size - 1); val to = b.coerceIn(from, data.size - 1)
                return respond(out, 206, type, data.copyOfRange(from, to + 1), "Content-Range: bytes $from-$to/${data.size}\r\n")
            }
            respond(out, 200, type, data)
        } catch (e: Exception) { /* a dropped connection */ } finally { try { c.close() } catch (e: Exception) { /* closed */ } }
    }

    private fun respond(out: java.io.OutputStream, code: Int, type: String, body: ByteArray, extra: String = "") {
        val status = when (code) { 200 -> "OK"; 206 -> "Partial Content"; 400 -> "Bad Request"; 404 -> "Not Found"; else -> "Error" }
        out.write(("HTTP/1.1 $code $status\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nAccept-Ranges: bytes\r\n${extra}Connection: close\r\n\r\n").toByteArray())
        out.write(body); out.flush()
    }

    /** Reads up to the blank line that ends the request head (at most 8 KB); bytes after it that came in the same read are kept for [readBody]. */
    private var carry = ThreadLocal<ByteArray>()
    private fun readHead(inp: InputStream): String? {
        val buf = ByteArrayOutputStream(); val one = ByteArray(1024)
        while (buf.size() < 8192) {
            val n = inp.read(one); if (n < 0) return null
            buf.write(one, 0, n)
            val b = buf.toByteArray(); val i = indexOf(b, "\r\n\r\n".toByteArray())
            if (i >= 0) { carry.set(b.copyOfRange(i + 4, b.size)); return String(b, 0, i, Charsets.ISO_8859_1) }
        }
        return null
    }
    private fun readBody(inp: InputStream, len: Int): ByteArray {
        val have = carry.get() ?: ByteArray(0); val out = ByteArrayOutputStream(); out.write(have)
        val one = ByteArray(4096)
        while (out.size() < len) { val n = inp.read(one); if (n < 0) break; out.write(one, 0, n) }
        return out.toByteArray().let { if (it.size > len) it.copyOf(len) else it }
    }
    private fun indexOf(a: ByteArray, p: ByteArray): Int { outer@ for (i in 0..a.size - p.size) { for (j in p.indices) if (a[i + j] != p[j]) continue@outer; return i }; return -1 }
}
