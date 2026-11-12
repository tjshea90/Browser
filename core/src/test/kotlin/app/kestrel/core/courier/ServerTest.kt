package app.kestrel.core.courier

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL

/** The real server over loopback sockets (no mocks): what the Courier app and the tablet will do to each other, minus the wifi. */
class ServerTest {
    private val servers = ArrayList<CourierServer>()
    private val sockets = ArrayList<Socket>()

    @After fun cleanUp() { sockets.forEach { try { it.close() } catch (_: IOException) {} }; servers.forEach { it.stop() } }

    private val fileBytes = ByteArray(100_000) { i -> (i * 31 + 7).toByte() }
    private val file = File.createTempFile("courier-test", ".bin").also { it.deleteOnExit(); it.writeBytes(fileBytes) }

    private fun route(req: HttpRequest): HttpResponse = when (req.path) {
        "/echo" -> HttpResponse.json(200, mapOf("method" to req.method, "path" to req.path, "q" to req.query, "bodyLen" to req.body.size, "body" to String(req.body)))
        "/file" -> HttpResponse(200, mapOf("Content-Type" to "application/octet-stream"), FileBody(file), ranged = true)
        "/huge" -> HttpResponse(200, body = object : HttpBody {
            override val length: Long = 256L shl 20
            override fun writeTo(out: OutputStream, from: Long, count: Long) { val z = ByteArray(65536); var left = count; while (left > 0) { val n = minOf(z.size.toLong(), left).toInt(); out.write(z, 0, n); left -= n } }
        })
        "/boom" -> throw IllegalStateException("kaboom")
        "/deny" -> throw HttpException(403, "no")
        else -> HttpResponse.error(404, "not found")
    }

    private fun start(maxConns: Int = 16, idleMs: Int = 5_000, stallMs: Long = 60_000, sweepMs: Long = 5_000, allowed: (InetAddress) -> Boolean = { it.isLoopbackAddress }): CourierServer {
        val s = CourierServer(0, InetAddress.getByName("127.0.0.1"), allowed, { req, _ -> route(req) }, maxConns, idleMs, stallMs, sweepMs)
        s.start(); servers.add(s); return s
    }

    private fun connect(s: CourierServer): Socket = Socket("127.0.0.1", s.boundPort).also { it.soTimeout = 8000; sockets.add(it) }

    private class Reply(val status: Int, val headers: Map<String, String>, val body: ByteArray)

    private fun readReply(inp: InputStream, head: Boolean = false): Reply {
        fun line(): String {
            val sb = StringBuilder()
            while (true) { val c = inp.read(); if (c < 0) throw EOFException(); if (c == '\n'.code) break; if (c != '\r'.code) sb.append(c.toChar()) }
            return sb.toString()
        }
        val status = line().split(' ')[1].toInt()
        val h = LinkedHashMap<String, String>()
        while (true) { val l = line(); if (l.isEmpty()) break; h[l.substringBefore(':').lowercase()] = l.substringAfter(':').trim() }
        val n = if (head) 0 else (h["content-length"]?.toInt() ?: 0)
        val body = ByteArray(n)
        var off = 0
        while (off < n) { val r = inp.read(body, off, n - off); if (r < 0) throw EOFException(); off += r }
        return Reply(status, h, body)
    }

    private fun send(s: Socket, text: String) { s.getOutputStream().write(text.toByteArray(Charsets.ISO_8859_1)); s.getOutputStream().flush() }

    private fun closedByPeer(s: Socket): Boolean = try { s.getInputStream().read() < 0 } catch (_: IOException) { true }

    @Test fun answersAndKeepsTheConnectionOpen() {
        val s = connect(start())
        send(s, "GET /echo?x=1%202&y=z HTTP/1.1\r\nHost: t\r\n\r\n")
        val a = readReply(s.getInputStream())
        assertEquals(200, a.status); assertEquals("keep-alive", a.headers["connection"])
        val j = Json.parse(String(a.body)) as Map<*, *>
        assertEquals("GET", j["method"]); assertEquals("/echo", j["path"]); assertEquals(mapOf("x" to "1 2", "y" to "z"), j["q"])
        send(s, "POST /echo HTTP/1.1\r\nHost: t\r\nContent-Length: 5\r\n\r\nhello")
        val b = readReply(s.getInputStream())
        assertEquals("hello", (Json.parse(String(b.body)) as Map<*, *>)["body"])
    }

    @Test fun pipelinedRequestsAreAnsweredInOrder() {
        val s = connect(start())
        send(s, "GET /echo?n=1 HTTP/1.1\r\n\r\nGET /nothing HTTP/1.1\r\n\r\nPOST /echo?n=3 HTTP/1.1\r\nContent-Length: 3\r\n\r\nabc")
        val r1 = readReply(s.getInputStream()); val r2 = readReply(s.getInputStream()); val r3 = readReply(s.getInputStream())
        assertEquals(listOf(200, 404, 200), listOf(r1.status, r2.status, r3.status))
        assertEquals("3", ((Json.parse(String(r3.body)) as Map<*, *>)["q"] as Map<*, *>)["n"])
    }

    @Test fun connectionCloseIsHonoured() {
        val s = connect(start())
        send(s, "GET /echo HTTP/1.1\r\nConnection: close\r\n\r\n")
        val r = readReply(s.getInputStream())
        assertEquals("close", r.headers["connection"]); assertTrue(closedByPeer(s))
    }

    @Test fun garbageAndOversizeGetAnErrorAndAClose() {
        val srv = start()
        val a = connect(srv); send(a, "NONSENSE\r\n\r\n")
        assertEquals(400, readReply(a.getInputStream()).status); assertTrue(closedByPeer(a))
        val b = connect(srv); send(b, "POST /echo HTTP/1.1\r\nContent-Length: 999999\r\n\r\n")
        assertEquals(413, readReply(b.getInputStream()).status); assertTrue(closedByPeer(b))
        val c = connect(srv); send(c, "GET /echo HTTP/1.1\r\nX: " + "a".repeat(20_000) + "\r\n\r\n")
        assertEquals(431, readReply(c.getInputStream()).status); assertTrue(closedByPeer(c))
        val d = connect(srv); send(d, "GET /echo HTTP/1.1\r\n\r\n")                       // still healthy afterwards
        assertEquals(200, readReply(d.getInputStream()).status)
    }

    @Test fun filesWithRanges() {
        val s = connect(start())
        val inp = s.getInputStream()
        send(s, "GET /file HTTP/1.1\r\n\r\n")
        val full = readReply(inp)
        assertEquals(200, full.status); assertEquals("bytes", full.headers["accept-ranges"]); assertArrayEquals(fileBytes, full.body)
        send(s, "GET /file HTTP/1.1\r\nRange: bytes=1000-1999\r\n\r\n")
        val p = readReply(inp)
        assertEquals(206, p.status); assertEquals("bytes 1000-1999/100000", p.headers["content-range"]); assertArrayEquals(fileBytes.copyOfRange(1000, 2000), p.body)
        send(s, "GET /file HTTP/1.1\r\nRange: bytes=-10\r\n\r\n")
        assertArrayEquals(fileBytes.copyOfRange(99_990, 100_000), readReply(inp).body)
        send(s, "GET /file HTTP/1.1\r\nRange: bytes=100000-\r\n\r\n")
        val bad = readReply(inp)
        assertEquals(416, bad.status); assertEquals("bytes */100000", bad.headers["content-range"])
        send(s, "HEAD /file HTTP/1.1\r\n\r\n")
        val h = readReply(inp, head = true)
        assertEquals(200, h.status); assertEquals("100000", h.headers["content-length"])
        send(s, "GET /echo HTTP/1.1\r\n\r\n")                                              // the connection survived all of that
        assertEquals(200, readReply(inp).status)
    }

    @Test fun handlerFailuresDoNotKillTheServer() {
        val s = connect(start())
        send(s, "GET /boom HTTP/1.1\r\n\r\n")
        val a = readReply(s.getInputStream()); assertEquals(500, a.status); assertEquals("""{"error":"internal error"}""", String(a.body))
        send(s, "GET /deny HTTP/1.1\r\n\r\n")
        assertEquals(403, readReply(s.getInputStream()).status)
        send(s, "GET /echo HTTP/1.1\r\n\r\n")
        assertEquals(200, readReply(s.getInputStream()).status)
    }

    @Test fun refusedPeersAreDroppedUnread() {
        val s = connect(start(allowed = { false }))
        try { send(s, "GET /echo HTTP/1.1\r\n\r\n") } catch (_: IOException) {}
        assertTrue("no answer at all", closedByPeer(s))
    }

    @Test fun tooManyConnectionsGet503AndRoomComesBack() {
        val srv = start(maxConns = 1)
        val first = connect(srv)
        send(first, "GET /echo HTTP/1.1\r\n\r\n"); assertEquals(200, readReply(first.getInputStream()).status)
        val second = connect(srv)
        val busy = readReply(second.getInputStream())
        assertEquals(503, busy.status); assertEquals("2", busy.headers["retry-after"])
        first.close()
        var ok = false
        for (i in 1..40) {
            val third = connect(srv)
            send(third, "GET /echo HTTP/1.1\r\nConnection: close\r\n\r\n")
            if (readReply(third.getInputStream()).status == 200) { ok = true; break }
            Thread.sleep(50)
        }
        assertTrue("a slot is free again after the first client left", ok)
    }

    @Test fun idleConnectionsAreClosed() {
        val s = connect(start(idleMs = 300))
        val t0 = System.currentTimeMillis()
        assertTrue(closedByPeer(s))
        assertTrue("closed after about the idle time, not instantly and not never", System.currentTimeMillis() - t0 in 200..4000)
    }

    @Test fun aReaderThatStopsReadingDoesNotHoldASlotForever() {
        val srv = start(maxConns = 1, stallMs = 400, sweepMs = 100)
        val hog = connect(srv)
        send(hog, "GET /huge HTTP/1.1\r\n\r\n")                                            // 256 MB, and this client never reads a byte
        Thread.sleep(300)
        var ok = false
        for (i in 1..80) {
            val s = connect(srv)
            send(s, "GET /echo HTTP/1.1\r\nConnection: close\r\n\r\n")
            val r = try { readReply(s.getInputStream()).status } catch (_: IOException) { -1 }
            if (r == 200) { ok = true; break }
            Thread.sleep(100)
        }
        assertTrue("the watchdog freed the slot of the stalled download", ok)
    }

    @Test fun stopClosesEverythingAndTheServerCanStartAgain() {
        val srv = start()
        val s = connect(srv)
        send(s, "GET /echo HTTP/1.1\r\n\r\n"); readReply(s.getInputStream())
        val port = srv.boundPort
        srv.stop()
        assertTrue(closedByPeer(s)); assertFalse(srv.running)
        try { Socket("127.0.0.1", port).close(); fail("the port should be closed") } catch (_: ConnectException) {}
        assertNotNull(srv.start()); assertTrue(srv.running)
        try { srv.start(); fail("a second start without stop") } catch (_: IllegalStateException) {}
        val again = connect(srv); send(again, "GET /echo HTTP/1.1\r\n\r\n"); assertEquals(200, readReply(again.getInputStream()).status)
    }

    @Test fun anOrdinaryHttpClientCanTalkToIt() {
        val srv = start()
        val g = URL("http://127.0.0.1:${srv.boundPort}/echo?x=y%20z").openConnection() as HttpURLConnection
        assertEquals(200, g.responseCode)
        assertEquals("y z", ((Json.parse(String(g.inputStream.readBytes())) as Map<*, *>)["q"] as Map<*, *>)["x"])
        val p = URL("http://127.0.0.1:${srv.boundPort}/echo").openConnection() as HttpURLConnection
        p.requestMethod = "POST"; p.doOutput = true
        p.outputStream.use { it.write("{\"a\":1}".toByteArray()) }
        assertEquals("{\"a\":1}", (Json.parse(String(p.inputStream.readBytes())) as Map<*, *>)["body"])
        val f = URL("http://127.0.0.1:${srv.boundPort}/file").openConnection() as HttpURLConnection
        f.setRequestProperty("Range", "bytes=500-599")
        assertEquals(206, f.responseCode); assertArrayEquals(fileBytes.copyOfRange(500, 600), f.inputStream.readBytes())
        val n = URL("http://127.0.0.1:${srv.boundPort}/missing").openConnection() as HttpURLConnection
        assertEquals(404, n.responseCode)
    }

    @Test fun manyClientsAtOnceGetTheirOwnAnswers() {
        val srv = start()
        val errors = java.util.concurrent.CopyOnWriteArrayList<String>()
        val threads = (1..12).map { n ->
            Thread {
                try {
                    val s = connect(srv)
                    for (i in 1..10) {
                        send(s, "GET /echo?n=$n-$i HTTP/1.1\r\n\r\n")
                        val q = ((Json.parse(String(readReply(s.getInputStream()).body)) as Map<*, *>)["q"] as Map<*, *>)["n"]
                        if (q != "$n-$i") errors.add("client $n got $q for $i")
                    }
                } catch (e: Exception) { errors.add("client $n: $e") }
            }.also { it.start() }
        }
        threads.forEach { it.join(20_000) }
        assertTrue(errors.toString(), errors.isEmpty())
    }
}
