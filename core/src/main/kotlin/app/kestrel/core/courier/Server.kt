package app.kestrel.core.courier

import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A small HTTP/1.1 server on a plain [ServerSocket] (no library: the Courier app depends on kotlin-stdlib and `:core` only).
 * One thread per connection, at most [maxConns] at once; a peer the [peerAllowed] rule refuses is closed before a byte is read;
 * keep-alive; a connection that makes no progress for [idleMs] (reading) or [stallMs] (writing a big answer to a stalled reader) is closed.
 * The request handler runs on the connection's thread and must be quick or stream its body (files).
 */
class CourierServer(
    private val port: Int,
    private val bindAddress: InetAddress? = null,
    /** (who connected, which local address they connected to): a connection this refuses is closed before a byte is read. */
    private val peerAllowed: (InetAddress, InetAddress) -> Boolean,
    private val handler: (HttpRequest, InetAddress) -> HttpResponse,
    private val maxConns: Int = 16,
    private val idleMs: Int = 30_000,
    private val stallMs: Long = 60_000,
    private val sweepMs: Long = 5_000,
    private val log: (String) -> Unit = {},
) {
    private class Conn(val socket: Socket) { @Volatile var lastIo = System.currentTimeMillis() }

    @Volatile private var server: ServerSocket? = null
    private val conns = ConcurrentHashMap<Socket, Conn>()
    private val active = AtomicInteger()
    private var pool = Executors.newCachedThreadPool(named("courier-conn"))
    private var watchdog: ScheduledExecutorService? = null
    private var acceptor: Thread? = null

    val running: Boolean get() = server?.isClosed == false
    val boundPort: Int get() = server?.localPort ?: -1

    /** Starts listening; returns the port really bound (useful when [port] is 0). Throws [IOException] when the port is taken. */
    @Synchronized @Throws(IOException::class)
    fun start(): Int {
        check(server == null) { "already started" }
        val ss = ServerSocket()
        ss.reuseAddress = true
        ss.bind(java.net.InetSocketAddress(bindAddress, port), 32)
        server = ss
        pool = Executors.newCachedThreadPool(named("courier-conn"))
        val wd = Executors.newSingleThreadScheduledExecutor(named("courier-watch"))
        wd.scheduleWithFixedDelay({ sweep() }, sweepMs, sweepMs, TimeUnit.MILLISECONDS)
        watchdog = wd
        acceptor = Thread({ acceptLoop(ss) }, "courier-accept").also { it.isDaemon = true; it.start() }
        return ss.localPort
    }

    @Synchronized fun stop() {
        val ss = server ?: return
        server = null
        try { ss.close() } catch (_: IOException) {}
        // close() returns while the accepting thread may still be inside accept(); until it has left, the old listening socket can finish a handshake.
        // Waiting for it makes "stopped" mean "the port is closed".
        val a = acceptor
        if (a != null && a !== Thread.currentThread()) try { a.join(3000) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        acceptor = null
        watchdog?.shutdownNow(); watchdog = null
        for (c in conns.keys.toList()) closeQuietly(c)
        pool.shutdownNow()
    }

    private fun acceptLoop(ss: ServerSocket) {
        while (!ss.isClosed) {
            val s = try { ss.accept() } catch (e: IOException) { if (ss.isClosed) return else { log("accept failed: ${e.message}"); continue } }
            try {
                if (!peerAllowed(s.inetAddress, s.localAddress)) { closeQuietly(s); continue }          // dropped unread
                if (active.get() >= maxConns) { busy(s); continue }
                s.tcpNoDelay = true
                s.soTimeout = idleMs
                val c = Conn(s)
                conns[s] = c
                active.incrementAndGet()
                pool.execute { try { serve(c) } finally { conns.remove(s); active.decrementAndGet(); closeQuietly(s) } }
            } catch (e: Exception) { log("connection refused: ${e.message}"); closeQuietly(s) }
        }
    }

    private fun busy(s: Socket) {
        try {
            s.soTimeout = 1000
            HttpWriter.write(s.getOutputStream(), null, HttpResponse(503, mapOf("Retry-After" to "2"), close = true), false)
        } catch (_: Exception) {} finally { closeQuietly(s) }
    }

    private fun serve(c: Conn) {
        val s = c.socket
        val peer = s.inetAddress
        try {
            val input = BufferedInputStream(s.getInputStream(), 8192)
            val out = Touching(s.getOutputStream(), c)
            while (!s.isClosed) {
                val req: HttpRequest
                try {
                    req = HttpParser.read(input) ?: return
                } catch (e: HttpException) {
                    HttpWriter.write(out, null, HttpResponse.error(e.status, e.message ?: "bad request").let { HttpResponse(it.status, it.headers, it.body, close = true) }, false)
                    return
                }
                c.lastIo = System.currentTimeMillis()
                val resp = try { handler(req, peer) } catch (e: HttpException) { HttpResponse.error(e.status, e.message ?: "error") }
                catch (e: Exception) { log("handler failed for ${req.method} ${shortPath(req.path)}: ${e.javaClass.simpleName}"); HttpResponse.error(500, "internal error") }
                val keep = HttpWriter.write(out, req, resp, req.keepAlive)
                c.lastIo = System.currentTimeMillis()
                if (!keep) return
            }
        } catch (_: SocketTimeoutException) {
        } catch (_: IOException) {
        } catch (e: Exception) { log("connection failed: ${e.javaClass.simpleName}") }
    }

    /** The first two path segments only: the rest can be a file token and must not reach a log. */
    private fun shortPath(p: String): String = "/" + p.split('/').filter { it.isNotEmpty() }.take(2).joinToString("/")

    /** Closes connections that have made no progress for too long (a reader that stopped reading a big file blocks its writer thread forever otherwise). */
    private fun sweep() {
        val now = System.currentTimeMillis()
        for (c in conns.values) if (now - c.lastIo > maxOf(stallMs, idleMs.toLong())) closeQuietly(c.socket)
    }

    private fun closeQuietly(c: Socket) { try { c.close() } catch (_: IOException) {} }

    /** Counts every write as progress for the watchdog. */
    private class Touching(val o: OutputStream, val c: Conn) : OutputStream() {
        override fun write(b: Int) { o.write(b); c.lastIo = System.currentTimeMillis() }
        override fun write(b: ByteArray, off: Int, len: Int) { o.write(b, off, len); c.lastIo = System.currentTimeMillis() }
        override fun flush() { o.flush() }
    }

    private fun named(prefix: String) = object : ThreadFactory {
        private val n = AtomicInteger()
        override fun newThread(r: Runnable): Thread = Thread(r, "$prefix-${n.incrementAndGet()}").also { it.isDaemon = true }
    }
}
