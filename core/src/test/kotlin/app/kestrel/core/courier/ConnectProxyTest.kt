package app.kestrel.core.courier

import app.kestrel.core.download.Clock
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.nio.file.Files
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/** The CONNECT proxy of "pages through the phone" against real sockets on loopback: only the website (a tiny echo server) and the name lookups are faked. */
class ConnectProxyTest {
    private val loop = InetAddress.getByName("127.0.0.1")
    private val cleanups = CopyOnWriteArrayList<() -> Unit>()
    @After fun tearDown() { for (c in cleanups.reversed()) try { c() } catch (_: Exception) {}; cleanups.clear() }

    /** The "website": echoes everything back (or, with [greeting], speaks first; with [replyAfterEof], answers only once the client has finished sending). */
    private inner class Site(val greeting: ByteArray? = null, val replyAfterEof: ByteArray? = null) {
        val server = ServerSocket(0, 50, loop)
        val port = server.localPort
        val connections = AtomicInteger()
        init {
            val t = Thread({
                while (true) {
                    val c = try { server.accept() } catch (e: IOException) { break }
                    connections.incrementAndGet()
                    Thread({ serve(c) }, "site").also { it.isDaemon = true; it.start() }
                }
            }, "site-accept")
            t.isDaemon = true; t.start()
            cleanups.add { server.close() }
        }
        private fun serve(c: Socket) {
            try {
                c.tcpNoDelay = true
                if (greeting != null) c.getOutputStream().write(greeting)
                val i = c.getInputStream(); val o = c.getOutputStream(); val b = ByteArray(16384)
                while (true) {
                    val n = i.read(b); if (n < 0) break
                    if (replyAfterEof == null) { o.write(b, 0, n) }
                }
                if (replyAfterEof != null) { o.write(replyAfterEof) }
                try { c.shutdownOutput() } catch (_: IOException) {}
            } catch (_: IOException) { } finally { try { c.close() } catch (_: IOException) {} }
        }
    }

    private fun readLine(i: InputStream): String? {
        val sb = StringBuilder()
        while (true) { val c = i.read(); if (c < 0) return if (sb.isEmpty()) null else sb.toString(); if (c == '\n'.code) return sb.toString().trimEnd('\r'); sb.append(c.toChar()) }
    }

    /** Reads the status line and the headers of the proxy's answer; returns (status, header lines). */
    private fun readHeadOf(i: InputStream): Pair<Int, List<String>> {
        val status = readLine(i) ?: return 0 to emptyList()
        val hs = ArrayList<String>()
        while (true) { val l = readLine(i) ?: break; if (l.isEmpty()) break; hs.add(l) }
        return (status.split(' ').getOrNull(1)?.toIntOrNull() ?: 0) to hs
    }

    private class Started(val proxy: ConnectProxy, val port: Int)

    private fun start(
        dialer: ConnectProxy.Dialer = GuardedDialer({ listOf(loop.address) }, { false }),
        allowed: (InetAddress, InetAddress) -> Boolean = { _, _ -> true },
        maxTunnels: Int = 256, headTimeoutMs: Int = 8000, idleMs: Long = 600_000L, clock: Clock = Clock.System,
    ): Started {
        val p = ConnectProxy(0, loop, allowed, dialer, maxTunnels, headTimeoutMs, idleMs, 3000, clock)
        val port = p.start()
        cleanups.add { p.stop() }
        return Started(p, port)
    }

    private fun tunnel(pr: Started, host: String, port: Int, extraAfterHead: ByteArray = ByteArray(0)): Triple<Socket, Int, List<String>> {
        val s = Socket(loop, pr.port); s.soTimeout = 10_000; s.tcpNoDelay = true
        cleanups.add { s.close() }
        s.getOutputStream().write(("CONNECT $host:$port HTTP/1.1\r\nHost: $host:$port\r\nProxy-Connection: keep-alive\r\n\r\n").toByteArray() + extraAfterHead)
        val (code, hs) = readHeadOf(s.getInputStream())
        return Triple(s, code, hs)
    }

    private fun readFully(i: InputStream, n: Int): ByteArray { val out = ByteArray(n); var o = 0; while (o < n) { val k = i.read(out, o, n - o); if (k < 0) throw IOException("eof after $o of $n"); o += k }; return out }

    @Test fun bytesFlowBothWaysThroughATunnel() {
        val site = Site(); val pr = start()
        val (s, code, _) = tunnel(pr, "files.example", site.port)
        assertEquals(200, code)
        val data = ByteArray(1 shl 20).also { Random(1).nextBytes(it) }
        val t = Thread { s.getOutputStream().write(data) }; t.start()
        assertArrayEquals(data, readFully(s.getInputStream(), data.size)); t.join()
        assertEquals(1, site.connections.get())
    }

    @Test fun aClientThatDoesNotWaitForThe200StillGetsItsBytesThrough() {
        val site = Site(); val pr = start()
        val (s, code, _) = tunnel(pr, "files.example", site.port, extraAfterHead = "hello-early".toByteArray())
        assertEquals(200, code)
        assertEquals("hello-early", String(readFully(s.getInputStream(), 11)))
    }

    @Test fun aHalfCloseIsPassedOnAndTheAnswerStillArrives() {
        val site = Site(replyAfterEof = "all-done".toByteArray()); val pr = start()
        val (s, code, _) = tunnel(pr, "files.example", site.port)
        assertEquals(200, code)
        s.getOutputStream().write("request".toByteArray()); s.shutdownOutput()
        val all = ByteArrayOutputStream(); val b = ByteArray(100)
        while (true) { val k = s.getInputStream().read(b); if (k < 0) break; all.write(b, 0, k) }
        assertEquals("all-done", all.toString())
    }

    @Test fun aServerThatSpeaksFirstIsRelayedWithoutTheClientSayingAnything() {
        val site = Site(greeting = "220 hello\r\n".toByteArray()); val pr = start()
        val (s, code, _) = tunnel(pr, "mail.example", site.port)
        assertEquals(200, code)
        assertEquals("220 hello\r\n", String(readFully(s.getInputStream(), 11)))
    }

    @Test fun privateDestinationsAreForbiddenAndNeverTouched() {
        val site = Site()
        for (ip in listOf(byteArrayOf(127, 0, 0, 1), byteArrayOf(192.toByte(), 168.toByte(), 43, 1), byteArrayOf(10, 1, 2, 3), byteArrayOf(100, 64, 0, 9), byteArrayOf(169.toByte(), 254.toByte(), 1, 1))) {
            val pr = start(dialer = GuardedDialer({ listOf(ip) }))
            val (s, code, _) = tunnel(pr, "innocent.example", site.port)
            assertEquals(ip.joinToString("."), 403, code); s.close()
        }
        val pr = start(dialer = GuardedDialer({ listOf(byteArrayOf(8, 8, 8, 8), byteArrayOf(192.toByte(), 168.toByte(), 1, 1)) }))
        assertEquals("one private answer among public ones is enough", 403, tunnel(pr, "rebind.example", site.port).second)
        assertEquals(0, site.connections.get())
    }

    @Test fun failuresToReachTheWebsiteAreTold() {
        val pr = start(dialer = GuardedDialer({ throw UnknownHostException("nope") }, { false }))
        assertEquals("a name that does not exist", 502, tunnel(pr, "no-such.example", 443).second)
        val dead = ServerSocket(0, 1, loop).use { it.localPort }                                    // a port nobody listens on any more
        val pr2 = start()
        assertEquals("a closed port", 502, tunnel(pr2, "closed.example", dead).second)
        val slow = object : ConnectProxy.Dialer { override fun dial(host: String, port: Int, timeoutMs: Int): Socket { throw SocketTimeoutException("connect timed out") } }
        assertEquals("a connect that never finishes", 504, tunnel(start(dialer = slow), "slow.example", 443).second)
        val st = pr.proxy.stats(); assertEquals(1L, st.failed); assertEquals(0L, st.tunnels)
    }

    @Test fun aPeerThatIsNotAllowedIsResetBeforeAnyAnswer() {
        val pr = start(allowed = { _, _ -> false })
        val s = Socket(loop, pr.port); s.soTimeout = 5000
        cleanups.add { s.close() }
        try { s.getOutputStream().write("CONNECT a.example:443 HTTP/1.1\r\n\r\n".toByteArray()) } catch (_: IOException) {}
        var bytes = 0
        try { bytes = s.getInputStream().read() } catch (e: IOException) { bytes = -1 }               // a reset or a plain close: both are "no proxy answer"
        assertTrue("no HTTP answer for a stranger (got $bytes)", bytes < 0)
        assertEquals(1L, pr.proxy.stats().refused)
    }

    @Test fun onlyConnectIsServed() {
        val pr = start()
        val s = Socket(loop, pr.port); s.soTimeout = 5000; cleanups.add { s.close() }
        s.getOutputStream().write("GET http://example.com/ HTTP/1.1\r\nHost: example.com\r\n\r\n".toByteArray())
        val (code, hs) = readHeadOf(s.getInputStream())
        assertEquals(405, code); assertTrue(hs.toString(), hs.any { it.equals("Allow: CONNECT", true) })
    }

    @Test fun malformedRequestsGetAnErrorAndAClose() {
        val pr = start(headTimeoutMs = 400)
        fun send(raw: String): Int { val s = Socket(loop, pr.port); s.soTimeout = 5000; try { s.getOutputStream().write(raw.toByteArray()); return readHeadOf(s.getInputStream()).first } finally { s.close() } }
        assertEquals(400, send("NONSENSE\r\n\r\n"))
        assertEquals(400, send("CONNECT example.com HTTP/1.1\r\n\r\n"))                         // no port
        assertEquals(400, send("CONNECT example.com:0 HTTP/1.1\r\n\r\n"))
        assertEquals(400, send("CONNECT example.com:70000 HTTP/1.1\r\n\r\n"))
        assertEquals(400, send("CONNECT exa mple.com:443 HTTP/1.1\r\n\r\n"))                    // a space makes the request line four words
        assertEquals(400, send("CONNECT [::1:443 HTTP/1.1\r\n\r\n"))
        assertEquals(400, send("CONNECT user@example.com:443 HTTP/1.1\r\n\r\n"))
        assertEquals(431, send("CONNECT example.com:443 HTTP/1.1\r\nX: " + "a".repeat(9000) + "\r\n\r\n"))
        // a client that connects and says nothing is let go
        val s = Socket(loop, pr.port); s.soTimeout = 5000; cleanups.add { s.close() }
        val t0 = System.currentTimeMillis()
        val r = try { s.getInputStream().read() } catch (e: IOException) { -1 }
        assertEquals(-1, r); assertTrue("closed after the head timeout, not never", System.currentTimeMillis() - t0 < 4000)
    }

    @Test fun ipv6LiteralsAreAccepted() {
        var seen: Pair<String, Int>? = null
        val d = object : ConnectProxy.Dialer { override fun dial(host: String, port: Int, timeoutMs: Int): Socket { seen = host to port; throw UnknownHostException(host) } }
        val pr = start(dialer = d)
        assertEquals(502, tunnel(pr, "[2001:db8::1]", 8443).second)
        assertEquals("2001:db8::1" to 8443, seen)
    }

    @Test(timeout = 60_000) fun manyTunnelsAtOnce() {
        val site = Site(); val pr = start()
        val ok = AtomicInteger()
        val threads = (1..100).map { n ->
            Thread {
                val (s, code, _) = tunnel(pr, "h$n.example", site.port)
                if (code == 200) { val d = ByteArray(10_000).also { Random(n.toLong()).nextBytes(it) }; s.getOutputStream().write(d); if (readFully(s.getInputStream(), d.size).contentEquals(d)) ok.incrementAndGet() }
                s.close()
            }.also { it.start() }
        }
        threads.forEach { it.join() }
        assertEquals(100, ok.get())
        val st = pr.proxy.stats(); assertEquals(100L, st.tunnels)
        val end = System.currentTimeMillis() + 10_000
        while (pr.proxy.stats().active > 0 && System.currentTimeMillis() < end) Thread.sleep(20)
        assertEquals("every tunnel was closed", 0, pr.proxy.stats().active)
    }

    @Test fun tooManyTunnelsIs503() {
        val site = Site(); val pr = start(maxTunnels = 2)
        val a = tunnel(pr, "a.example", site.port); val b = tunnel(pr, "b.example", site.port)
        assertEquals(200, a.second); assertEquals(200, b.second)
        assertEquals(503, tunnel(pr, "c.example", site.port).second)
        a.first.close()
        val end = System.currentTimeMillis() + 5000
        var code = 0
        while (System.currentTimeMillis() < end) { code = tunnel(pr, "d.example", site.port).second; if (code == 200) break; Thread.sleep(50) }
        assertEquals("room again once a tunnel closed", 200, code)
    }

    @Test(timeout = 90_000) fun aBigTransferArrivesByteForByte() {
        val site = Site(); val pr = start()
        val (s, code, _) = tunnel(pr, "big.example", site.port)
        assertEquals(200, code)
        val total = 48 * 1024 * 1024
        val src = MessageDigest.getInstance("SHA-256"); val dst = MessageDigest.getInstance("SHA-256")
        val t0 = System.currentTimeMillis()
        val w = Thread { val r = Random(7); val b = ByteArray(64 * 1024); var left = total; while (left > 0) { r.nextBytes(b); val n = minOf(left, b.size); src.update(b, 0, n); s.getOutputStream().write(b, 0, n); left -= n } }
        w.start()
        val b = ByteArray(64 * 1024); var got = 0
        while (got < total) { val k = s.getInputStream().read(b); if (k < 0) throw IOException("eof at $got"); dst.update(b, 0, k); got += k }
        w.join()
        assertArrayEquals(src.digest(), dst.digest())
        val ms = System.currentTimeMillis() - t0
        assertTrue("48 MB each way took $ms ms: far too slow for a relay", ms < 40_000)
        val st = pr.proxy.stats(); assertTrue(st.up >= total && st.down >= total)
    }

    @Test fun anIdleTunnelIsClosedButABusyOneIsNot() {
        var now = 0L
        val clock = object : Clock { override fun now() = now }
        val site = Site(); val pr = start(idleMs = 1_000L, clock = clock)
        val (s, code, _) = tunnel(pr, "idle.example", site.port)
        assertEquals(200, code)
        // the poll interval of the relay is real time (30 s); the idle decision uses the injected clock: this only checks the path does not close an active tunnel early
        s.getOutputStream().write("ping".toByteArray()); assertEquals("ping", String(readFully(s.getInputStream(), 4)))
        now += 500
        s.getOutputStream().write("pong".toByteArray()); assertEquals("pong", String(readFully(s.getInputStream(), 4)))
    }

    @Test fun theActivityCallbackReportsHowManyTunnelsAreOpen() {
        val site = Site(); val seen = CopyOnWriteArrayList<Int>()
        val p = ConnectProxy(0, loop, { _, _ -> true }, GuardedDialer({ listOf(loop.address) }, { false }), onActive = { seen.add(it) })
        val port = p.start(); cleanups.add { p.stop() }
        val s = Socket(loop, port); s.soTimeout = 10_000; cleanups.add { s.close() }
        s.getOutputStream().write("CONNECT x.example:${site.port} HTTP/1.1\r\n\r\n".toByteArray())
        assertEquals(200, readHeadOf(s.getInputStream()).first)
        assertTrue("the first tunnel was announced as 1: $seen", seen.contains(1))
        s.close()
        val end = System.currentTimeMillis() + 5000
        while (!seen.contains(0) && System.currentTimeMillis() < end) Thread.sleep(20)
        assertTrue("and the last one closing as 0: $seen", seen.last() == 0)
    }

    @Test fun statsCountTunnelsAndBytes() {
        val site = Site(); val pr = start()
        val (s, _, _) = tunnel(pr, "stats.example", site.port)
        s.getOutputStream().write(ByteArray(1000)); readFully(s.getInputStream(), 1000)
        val st = pr.proxy.stats()
        assertEquals(1L, st.tunnels); assertEquals(1, st.active); assertTrue(st.up >= 1000 && st.down >= 1000)
    }

    @Test fun stoppingTheProxyEndsEveryTunnelAndTheListener() {
        val site = Site(); val pr = start()
        val (s, code, _) = tunnel(pr, "stop.example", site.port)
        assertEquals(200, code)
        pr.proxy.stop()
        val r = try { s.getInputStream().read() } catch (e: IOException) { -1 }
        assertEquals(-1, r)
        var refused = false
        try { Socket().use { it.connect(InetSocketAddress(loop, pr.port), 500) } } catch (e: IOException) { refused = true }
        assertTrue("nobody listens any more", refused)
    }

    @Test fun theGateArmsExpiresAndRecognisesTheSameAddressInEveryForm() {
        var now = 1_000L
        val gate = ProxyGate(object : Clock { override fun now() = now })
        val ip = InetAddress.getByName("192.168.43.7")
        assertFalse(gate.isArmed(ip))
        gate.arm(ip, 60_000)
        assertTrue(gate.isArmed(ip))
        assertTrue("an IPv4-mapped IPv6 address is the same tablet", gate.isArmed(InetAddress.getByAddress(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0xFF.toByte(), 0xFF.toByte(), 192.toByte(), 168.toByte(), 43, 7))))
        assertFalse(gate.isArmed(InetAddress.getByName("192.168.43.8")))
        now += 59_000; assertTrue(gate.isArmed(ip))
        now += 2_000; assertFalse("expired", gate.isArmed(ip))
        gate.arm(ip, 10 * 3600_000L); now += 16 * 60_000L
        assertFalse("a huge ttl is cut to 15 minutes", gate.isArmed(ip))
        gate.arm(ip, 60_000); gate.disarmAll(); assertFalse(gate.isArmed(ip))
    }

    @Test(timeout = 90_000) fun aRealHttpsClientGetsAWebsiteThroughTheProxy() {
        val ks = makeKeystore()
        assumeTrue("keytool is needed to make a certificate for the test", ks != null)
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).also { it.init(ks!!, "changeit".toCharArray()) }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also { it.init(ks) }
        val serverCtx = SSLContext.getInstance("TLS").also { it.init(kmf.keyManagers, null, null) }
        val ss = serverCtx.serverSocketFactory.createServerSocket(0, 10, loop) as javax.net.ssl.SSLServerSocket
        cleanups.add { ss.close() }
        Thread({
            while (true) {
                val c = try { ss.accept() } catch (e: IOException) { break }
                Thread({
                    try {
                        val i = c.getInputStream(); val buf = ByteArray(4096); val head = StringBuilder()
                        while (!head.endsWith("\r\n\r\n")) { val k = i.read(buf); if (k < 0) break; head.append(String(buf, 0, k)) }
                        val body = "hello through the phone".toByteArray()
                        c.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray() + body)
                        c.getOutputStream().flush(); c.close()
                    } catch (_: IOException) {}
                }, "tls-site").also { it.isDaemon = true; it.start() }
            }
        }, "tls-accept").also { it.isDaemon = true; it.start() }
        val pr = start()
        val clientCtx = SSLContext.getInstance("TLS").also { it.init(null, tmf.trustManagers, null) }
        val url = URL("https://localhost:${ss.localPort}/page")
        val con = url.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress(loop, pr.port))) as HttpsURLConnection
        con.sslSocketFactory = clientCtx.socketFactory
        con.connectTimeout = 10_000; con.readTimeout = 10_000
        assertEquals(200, con.responseCode)
        assertEquals("hello through the phone", con.inputStream.readBytes().toString(Charsets.UTF_8))
        assertEquals("the page really came through the tunnel", 1L, pr.proxy.stats().tunnels)
    }

    /** A self-signed certificate for localhost, made with the JDK's own keytool; null when keytool cannot be run here. */
    private fun makeKeystore(): KeyStore? = try {
        val dir = Files.createTempDirectory("proxy-tls").toFile(); cleanups.add { dir.listFiles()?.forEach { it.delete() }; dir.delete() }
        val f = File(dir, "k.p12")
        val keytool = File(System.getProperty("java.home"), "bin/keytool").path
        val p = ProcessBuilder(keytool, "-genkeypair", "-alias", "t", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-dname", "CN=localhost", "-ext", "san=dns:localhost,ip:127.0.0.1",
            "-storetype", "PKCS12", "-keystore", f.path, "-storepass", "changeit", "-keypass", "changeit").redirectErrorStream(true).start()
        p.inputStream.readBytes()
        if (p.waitFor() != 0) null else KeyStore.getInstance("PKCS12").also { ks -> f.inputStream().use { ks.load(it, "changeit".toCharArray()) } }
    } catch (e: Exception) { null }
}
