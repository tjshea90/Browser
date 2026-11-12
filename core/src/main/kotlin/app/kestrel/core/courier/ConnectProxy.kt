package app.kestrel.core.courier

import app.kestrel.core.download.Clock
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Which tablets may use the proxy right now: those that proved the pairing key (a signed call) from that address in the last few minutes. */
class ProxyGate(private val clock: Clock = Clock.System) {
    private val armed = ConcurrentHashMap<String, Long>()
    fun arm(ip: InetAddress, ttlMs: Long) { armed[key(ip)] = clock.now() + ttlMs.coerceIn(1_000L, MAX_TTL_MS); sweep() }
    fun disarm(ip: InetAddress) { armed.remove(key(ip)) }
    fun isArmed(ip: InetAddress): Boolean { val k = key(ip); val until = armed[k] ?: return false; if (clock.now() < until) return true; armed.remove(k, until); return false }
    fun disarmAll() = armed.clear()
    val armedCount: Int get() { sweep(); return armed.size }
    private fun sweep() { val now = clock.now(); armed.entries.removeIf { it.value <= now } }
    private fun key(ip: InetAddress): String = (try { InetAddress.getByAddress(PeerGuard.normalize(ip.address)) } catch (e: Exception) { ip }).hostAddress.substringBefore('%')

    companion object { const val MAX_TTL_MS = 15 * 60_000L }
}

/** What the phone host needs to know about the proxy: who may use it, where it listens, and what it has done (for `status` and the tablet's Diagnostics). */
class ProxyControl(val gate: ProxyGate, val port: Int, val stats: () -> ConnectProxy.Stats)

/**
 * An HTTP CONNECT proxy: the page-load path of "pages through the phone" (research/SPEED2.md section 4). The tablet's browser opens a tunnel to host:port through it, the phone opens the real
 * connection with ITS OWN network stack (so the carrier sees the phone's own traffic, not the capped hotspot's) and copies the bytes both ways. TLS stays end to end between the browser and the
 * website: the phone never sees what is inside, only which host and port. Only CONNECT is served (plain http:// pages are left to go direct), and only to a peer the [peerAllowed] check lets in.
 *
 * Refusals: a peer that is not allowed gets a reset before a single byte is read (so the browser's own fallback, "DIRECT", takes over at once); a destination on a private network is a 403; a
 * name that does not resolve is a 502, a connect that takes too long a 504, too many tunnels a 503. One thread per direction of each tunnel: simple and fast enough for a hotspot.
 */
class ConnectProxy(
    private val port: Int,
    private val bindAddress: InetAddress?,
    private val peerAllowed: (remote: InetAddress, local: InetAddress) -> Boolean,
    private val dialer: Dialer,
    private val maxTunnels: Int = 256,
    private val headTimeoutMs: Int = 8_000,
    private val idleMs: Long = 10 * 60_000L,
    private val connectTimeoutMs: Int = 9_000,
    private val clock: Clock = Clock.System,
    private val log: (String) -> Unit = {},
    /** DEBUG builds of the phone app: one log line per refused peer and per tunnel (host names are private: never on in a release). */
    private val verbose: Boolean = false,
    /** Called with the number of open tunnels each time it changes (the phone app keeps its CPU awake while there are any). */
    private val onActive: (Int) -> Unit = {},
) {
    /** Opens the real connection. Throws [Blocked] for a destination the proxy must not reach, [UnknownHostException] / other [IOException] otherwise. */
    interface Dialer { @Throws(IOException::class) fun dial(host: String, port: Int, timeoutMs: Int): Socket }
    class Blocked(msg: String) : IOException(msg)

    class Stats(val accepted: Long, val refused: Long, val tunnels: Long, val active: Int, val up: Long, val down: Long, val failed: Long, val avgConnectMs: Long)

    private val accepted = AtomicLong(); private val refused = AtomicLong(); private val tunnels = AtomicLong(); private val failed = AtomicLong()
    private val active = AtomicInteger(); private val up = AtomicLong(); private val down = AtomicLong()
    private val connectMsTotal = AtomicLong(); private val connectCount = AtomicLong()
    private val open = ConcurrentHashMap.newKeySet<Socket>()
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "proxy-tunnel").also { it.isDaemon = true } }
    @Volatile private var server: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var stopping = false

    fun stats(): Stats = Stats(accepted.get(), refused.get(), tunnels.get(), active.get(), up.get(), down.get(), failed.get(), if (connectCount.get() == 0L) 0L else connectMsTotal.get() / connectCount.get())

    /** Binds and starts accepting; returns the port (the real one when 0 was asked for). */
    @Throws(IOException::class)
    fun start(): Int {
        val s = ServerSocket()
        s.reuseAddress = true
        s.bind(InetSocketAddress(bindAddress, port), 128)
        server = s
        acceptThread = Thread({ acceptLoop(s) }, "proxy-accept").also { it.isDaemon = true; it.start() }
        return s.localPort
    }

    fun stop() {
        stopping = true
        try { server?.close() } catch (_: IOException) {}
        try { acceptThread?.join(2000) } catch (_: InterruptedException) {}
        for (s in open.toList()) closeQuietly(s)
        pool.shutdownNow()
    }

    private fun acceptLoop(s: ServerSocket) {
        while (!stopping) {
            val c = try { s.accept() } catch (e: IOException) { if (stopping || s.isClosed) return; try { Thread.sleep(50) } catch (_: InterruptedException) { return }; continue }
            accepted.incrementAndGet()
            try { pool.execute { handle(c) } } catch (e: Exception) { closeQuietly(c) }
        }
    }

    private fun handle(client: Socket) {
        open.add(client)
        var upstream: Socket? = null
        onActive(active.incrementAndGet())
        try {
            val remote = client.inetAddress; val local = client.localAddress
            if (remote == null || local == null || !peerAllowed(remote, local)) { refused.incrementAndGet(); if (verbose) log("proxy: turned away ${remote?.hostAddress}"); reset(client); return }
            client.tcpNoDelay = true
            client.soTimeout = headTimeoutMs
            val head = readHead(client) ?: return
            val line = head.line
            val parts = line.split(' ')
            if (parts.size != 3 || !parts[2].startsWith("HTTP/1.")) { reply(client, 400, "Bad Request"); return }
            if (parts[0] != "CONNECT") { reply(client, 405, "Method Not Allowed", "Allow: CONNECT"); return }
            val dest = parseAuthority(parts[1]) ?: run { reply(client, 400, "Bad Request"); return }
            if (active.get() > maxTunnels) { reply(client, 503, "Service Unavailable"); return }
            val t0 = clock.now()
            try {
                upstream = dialer.dial(dest.first, dest.second, connectTimeoutMs)
            } catch (e: Blocked) { failed.incrementAndGet(); reply(client, 403, "Forbidden"); return
            } catch (e: UnknownHostException) { failed.incrementAndGet(); reply(client, 502, "Bad Gateway"); return
            } catch (e: SocketTimeoutException) { failed.incrementAndGet(); reply(client, 504, "Gateway Timeout"); return
            } catch (e: IOException) { failed.incrementAndGet(); reply(client, 502, "Bad Gateway"); return }
            connectMsTotal.addAndGet(clock.now() - t0); connectCount.incrementAndGet(); tunnels.incrementAndGet()
            if (verbose) log("proxy: tunnel to ${dest.first}:${dest.second} open after ${clock.now() - t0} ms")
            open.add(upstream)
            upstream.tcpNoDelay = true
            client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\nProxy-Agent: kestrel-courier\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
            client.getOutputStream().flush()
            if (head.extra.isNotEmpty()) { upstream.getOutputStream().write(head.extra); up.addAndGet(head.extra.size.toLong()) }   // a client that did not wait for the 200
            relay(client, upstream)
        } catch (e: IOException) {
            // the tunnel or the head broke: nothing to report to a browser that is gone
        } finally {
            closeQuietly(client); if (upstream != null) closeQuietly(upstream)
            open.remove(client); if (upstream != null) open.remove(upstream)
            onActive(active.decrementAndGet())
        }
    }

    /** Both directions, until each side has said it is done (half-close is passed on) or the tunnel has been quiet for [idleMs]. */
    private fun relay(a: Socket, b: Socket) {
        val last = AtomicLong(clock.now())
        a.soTimeout = POLL_MS; b.soTimeout = POLL_MS
        val t = Thread({ pump(b, a, down, last) }, "proxy-down")
        t.isDaemon = true; t.start()
        pump(a, b, up, last)
        try { t.join() } catch (_: InterruptedException) {}
    }

    private fun pump(from: Socket, to: Socket, counter: AtomicLong, last: AtomicLong) {
        val buf = ByteArray(BUF)
        try {
            val i = from.getInputStream(); val o = to.getOutputStream()
            while (true) {
                val n = try { i.read(buf) } catch (e: SocketTimeoutException) {
                    if (clock.now() - last.get() > idleMs) throw e
                    continue                                                    // quiet in this direction only: the other one may be busy
                }
                if (n < 0) break
                o.write(buf, 0, n); counter.addAndGet(n.toLong()); last.set(clock.now())
            }
            try { to.shutdownOutput() } catch (_: IOException) {}                // pass the half-close on, keep the other direction going
        } catch (e: IOException) {
            closeQuietly(from); closeQuietly(to)                                // a reset, a timeout, a closed socket: end the whole tunnel
        }
    }

    private class Head(val line: String, val extra: ByteArray)

    /** Reads up to the blank line that ends the request head (at most [MAX_HEAD] bytes); null = the peer was answered with an error or went away. */
    private fun readHead(c: Socket): Head? {
        val buf = ByteArray(MAX_HEAD)
        var n = 0
        val i = c.getInputStream()
        while (true) {
            val k = try { i.read(buf, n, buf.size - n) } catch (e: SocketTimeoutException) { return null }
            if (k < 0) return null
            n += k
            var e = indexOfBlankLine(buf, n)
            if (e >= 0) {
                val lineEnd = (0 until n).firstOrNull { buf[it] == '\n'.code.toByte() } ?: e
                val line = String(buf, 0, lineEnd, Charsets.ISO_8859_1).trimEnd('\r', '\n')
                return Head(line, buf.copyOfRange(e, n))
            }
            if (n >= buf.size) { reply(c, 431, "Request Header Fields Too Large"); return null }
        }
    }

    /** Index just after the first CRLFCRLF (or LFLF) in the first [n] bytes, -1 when there is none yet. */
    private fun indexOfBlankLine(b: ByteArray, n: Int): Int {
        for (j in 0 until n - 1) {
            if (b[j] == '\n'.code.toByte()) {
                if (b[j + 1] == '\n'.code.toByte()) return j + 2
                if (j + 2 < n && b[j + 1] == '\r'.code.toByte() && b[j + 2] == '\n'.code.toByte()) return j + 3
            }
        }
        return -1
    }

    private fun parseAuthority(a: String): Pair<String, Int>? {
        if (a.isEmpty() || a.length > 300) return null
        val host: String; val portS: String
        if (a.startsWith("[")) {
            val e = a.indexOf(']'); if (e < 0 || e + 1 >= a.length || a[e + 1] != ':') return null
            host = a.substring(1, e); portS = a.substring(e + 2)
        } else {
            val c = a.lastIndexOf(':'); if (c <= 0) return null
            host = a.substring(0, c); portS = a.substring(c + 1)
        }
        val p = portS.toIntOrNull() ?: return null
        if (p !in 1..65535 || host.isEmpty() || host.length > 253) return null
        if (host.any { it.code <= 32 || it == '/' || it == '\\' || it == '@' || it == '?' || it == '#' }) return null
        return host to p
    }

    private fun reply(c: Socket, code: Int, reason: String, extra: String? = null) {
        try {
            val sb = StringBuilder("HTTP/1.1 $code $reason\r\n")
            if (extra != null) sb.append(extra).append("\r\n")
            sb.append("Content-Length: 0\r\nConnection: close\r\nProxy-Agent: kestrel-courier\r\n\r\n")
            c.getOutputStream().write(sb.toString().toByteArray(Charsets.ISO_8859_1)); c.getOutputStream().flush()
        } catch (_: IOException) {}
    }

    /** Close with a TCP reset: the browser sees a failed connection to its proxy (not a proxy error page) and falls back to a direct connection. */
    private fun reset(c: Socket) { try { c.setSoLinger(true, 0) } catch (_: IOException) {}; closeQuietly(c) }

    private fun closeQuietly(s: Socket) { try { s.close() } catch (_: IOException) {} }

    companion object {
        const val MAX_HEAD = 8192
        const val BUF = 32 * 1024
        const val POLL_MS = 30_000
    }
}

/** The proxy's way out: resolve the name, refuse every private answer (one is enough: a name that also points inward is an attack), then try the addresses in turn. */
class GuardedDialer(
    private val resolve: (String) -> List<ByteArray>,
    private val blocked: (ByteArray) -> Boolean = { Ssrf.blocked(it) },
) : ConnectProxy.Dialer {
    override fun dial(host: String, port: Int, timeoutMs: Int): Socket {
        val addrs = resolve(host)
        if (addrs.isEmpty()) throw UnknownHostException(host)
        for (a in addrs) if (blocked(a)) throw ConnectProxy.Blocked("address on a private network")
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: IOException? = null
        for ((idx, a) in addrs.withIndex()) {
            val left = (deadline - System.currentTimeMillis()).toInt()
            if (left <= 0) break
            val s = Socket()
            try {
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(InetAddress.getByAddress(a), port), minOf(left, if (idx == 0) 4_000 else 2_500))
                return s
            } catch (e: IOException) { try { s.close() } catch (_: IOException) {}; last = e }
        }
        throw last ?: SocketTimeoutException("connect timed out")
    }
}
