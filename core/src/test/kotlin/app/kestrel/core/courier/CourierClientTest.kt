package app.kestrel.core.courier

import app.kestrel.core.download.FakeServer
import app.kestrel.core.download.FetchException
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.testBytes
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The tablet's side against the real phone side (real server, host and engine on loopback), plus the socket HTTP client against hostile and broken servers. */
class CourierClientTest {
    private val MiB = 1 shl 20
    private val root: File = Files.createTempDirectory("courier-client").toFile()
    private val cleanups = ArrayList<() -> Unit>()
    @After fun cleanUp() { cleanups.reversed().forEach { try { it() } catch (_: Exception) {} }; root.deleteRecursively() }

    private fun phone(origin: FakeServer = FakeServer(testBytes(1000)), clock: Skew = Skew()) = Phone(origin, root, cleanups, clock = clock)
    private fun client(p: Phone, key: ByteArray = TEST_KEY) = CourierClient("127.0.0.1", p.port, key)
    private var n = 0
    private fun cid() = "client-job-%04d".format(++n)

    /** A server that does whatever [script] says with the first connection, then goes away. */
    private fun rawServer(script: (Socket) -> Unit): Int {
        val ss = ServerSocket(0, 5, java.net.InetAddress.getByName("127.0.0.1"))
        cleanups.add { ss.close() }
        Thread { try { ss.accept().use { s -> script(s) } } catch (_: Exception) {} }.also { it.isDaemon = true; it.start() }
        return ss.localPort
    }

    private fun drainRequest(s: Socket) { val i = s.getInputStream(); var last = 0; var run = 0; while (run < 4) { val c = i.read(); if (c < 0) return; run = if ((c == 13 && run % 2 == 0) || (c == 10 && run % 2 == 1)) run + 1 else 0; last = c } }

    // ------------------------------------------------------------------------------------------------------------------ socket HTTP
    @Test fun theSocketClientReadsRealAnswersAndRanges() {
        val data = testBytes(2 * MiB)
        val p = phone(FakeServer(data))
        val c = client(p)
        val job = c.create(JobSpec(cid(), "https://files.test/pub/data.bin", "d.bin", true))
        var done: RemoteJob
        val end = System.currentTimeMillis() + 30_000
        while (true) { done = c.jobs(listOf(job.id))[0]; if (done.state == "COMPLETED" || System.currentTimeMillis() > end) break; Thread.sleep(10) }
        assertEquals("COMPLETED", done.state)
        SocketHttp.open("127.0.0.1", p.port, "GET", done.fileUrl!!, mapOf("Range" to "bytes=1000-2999"), null, 2000, 5000).use { r ->
            assertEquals(206, r.status); assertEquals("bytes 1000-2999/${data.size}", r.header("Content-Range")); assertEquals("2000", r.header("content-length"))
            assertArrayEquals(data.copyOfRange(1000, 3000), r.readAll())
        }
        SocketHttp.open("127.0.0.1", p.port, "HEAD", done.fileUrl!!, emptyMap(), null, 2000, 5000).use { r -> assertEquals(200, r.status); assertEquals(0, r.readAll().size) }
        SocketHttp.open("127.0.0.1", p.port, "GET", done.fileUrl!!, emptyMap(), null, 2000, 5000).use { r -> assertArrayEquals(data, r.readAll(4 * MiB)) }
    }

    @Test fun anAnswerThatStopsEarlyIsAnErrorNotAShortFile() {
        val port = rawServer { s -> drainRequest(s); s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n".toByteArray() + ByteArray(50)); s.getOutputStream().flush() }
        SocketHttp.open("127.0.0.1", port, "GET", "/x", emptyMap(), null, 2000, 3000).use { r ->
            assertEquals(200, r.status)
            try { r.readAll(); fail("a short body must not look complete") } catch (e: EOFException) { assertTrue(e.message!!, e.message!!.contains("50 bytes early")) }
        }
    }

    @Test fun brokenAnswersAreRefused() {
        val cases = listOf(
            "SSH-2.0-OpenSSH_9.0\r\n", "HTTP/1.1 99 Nope\r\n\r\n", "HTTP/1.1 abc OK\r\n\r\n", "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n0\r\n\r\n",
            "HTTP/1.1 200 OK\r\nContent-Length: -5\r\n\r\n", "HTTP/1.1 200 OK\r\nnocolon\r\n\r\n", "HTTP/1.1 200 OK\r\n" + (1..70).joinToString("") { "H$it: v\r\n" } + "\r\n", "HTTP/1.1 200 OK\r\nX: " + "a".repeat(9000) + "\r\n\r\n", "",
        )
        for (text in cases) {
            val port = rawServer { s -> drainRequest(s); s.getOutputStream().write(text.toByteArray()); s.getOutputStream().flush() }
            try { SocketHttp.open("127.0.0.1", port, "GET", "/x", emptyMap(), null, 2000, 3000).close(); fail("should be refused: ${text.take(50)}") } catch (_: IOException) {}
        }
    }

    @Test fun abortFreesAReaderThatIsWaiting() {
        val gate = CountDownLatch(1)
        val port = rawServer { s -> drainRequest(s); s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\nabc".toByteArray()); s.getOutputStream().flush(); gate.await(10, TimeUnit.SECONDS) }
        cleanups.add { gate.countDown() }
        val r = SocketHttp.open("127.0.0.1", port, "GET", "/x", emptyMap(), null, 2000, 20_000)
        val result = CopyOnWriteArrayList<String>()
        val t = Thread { try { r.readAll(); result.add("read all?!") } catch (e: IOException) { result.add("io") } }.also { it.start() }
        Thread.sleep(300); assertTrue("still waiting for the rest", t.isAlive)
        val t0 = System.currentTimeMillis(); r.abort(); t.join(3000)
        assertEquals(listOf("io"), result); assertTrue(System.currentTimeMillis() - t0 < 2500)
    }

    @Test fun refusedConnectionsAndSlowPhonesHaveTheirOwnKinds() {
        val dead = ServerSocket(0).use { it.localPort }                                          // a port nobody listens on
        try { CourierClient("127.0.0.1", dead, TEST_KEY).hello(); fail() } catch (e: CourierException) { assertEquals(CourierException.Kind.UNREACHABLE, e.kind) }
        val port = rawServer { s -> drainRequest(s); Thread.sleep(4000) }                           // accepts, never answers
        val t0 = System.currentTimeMillis()
        try { CourierClient("127.0.0.1", port, TEST_KEY).hello(); fail() } catch (e: CourierException) { assertEquals(CourierException.Kind.TIMEOUT, e.kind) }
        assertTrue("gave up after about the read timeout", System.currentTimeMillis() - t0 in 1000..3900)
        val junk = rawServer { s -> drainRequest(s); s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello".toByteArray()) }
        try { CourierClient("127.0.0.1", junk, TEST_KEY).hello(); fail() } catch (e: CourierException) { assertTrue(e.kind == CourierException.Kind.PROTOCOL || e.kind == CourierException.Kind.IMPOSTOR) }
    }

    // ------------------------------------------------------------------------------------------------------------------ the protocol
    @Test fun helloProvesThePhoneAndLearnsItsClock() {
        val skew = Skew().also { it.ms = 10 * 60_000 }
        val p = phone(clock = skew)
        val c = client(p)
        assertTrue(c.hello() >= 0)
        assertTrue("offset about +10 minutes: ${c.skewMs}", Math.abs(c.skewMs - 600_000) < 3000)
        assertEquals("Test phone", c.status().name)                                       // signed with the corrected clock
    }

    @Test fun aWrongClockIsCorrectedByTheFirstCallItself() {
        val p = phone(clock = Skew().also { it.ms = -5 * 60_000 })
        val c = client(p)
        assertEquals("Test phone", c.status().name)
        assertTrue(Math.abs(c.skewMs + 300_000) < 3000)
    }

    @Test fun aDifferentPairingCodeIsNotOurPhone() {
        val p = phone()
        val c = client(p, Sign.deriveKey(ByteArray(10)))
        try { c.hello(); fail() } catch (e: CourierException) { assertEquals(CourierException.Kind.IMPOSTOR, e.kind) }
        try { c.status(); fail() } catch (e: CourierException) { assertEquals(CourierException.Kind.AUTH, e.kind) }
        var last: CourierException.Kind? = null
        for (i in 1..12) { try { c.status() } catch (e: CourierException) { last = e.kind }; if (last == CourierException.Kind.THROTTLED) break }
        assertEquals("guessing is slowed down", CourierException.Kind.THROTTLED, last)
    }

    @Test(timeout = 90_000) fun aWholeJobThroughTheClient() {
        val data = testBytes(3 * MiB + 17)
        val origin = FakeServer(data).also { it.chunkDelayMs = 12; it.chunk = 16 * 1024 }
        val p = phone(origin)
        val c = client(p)
        val id = cid()
        val made = c.create(JobSpec(id, "https://files.test/pub/data.bin", "Holiday.mp4", true, "video/mp4", data.size.toLong(), mapOf("Referer" to "https://site.test/")))
        assertEquals(id, made.cid); assertFalse(made.terminal)
        assertEquals("a repeat is the same job", made.id, c.create(JobSpec(id, "https://files.test/pub/data.bin")).id)
        var j = c.jobs(listOf(made.id))[0]
        val end = System.currentTimeMillis() + 60_000
        while (j.state != "RUNNING" || j.done == 0L) { if (System.currentTimeMillis() > end) fail("never started: ${j.state}"); Thread.sleep(5); j = c.jobs(listOf(made.id))[0] }
        assertTrue(j.actions.contains("PAUSE"))
        c.action(made.id, "pause")
        while (j.state != "PAUSED_USER") { if (System.currentTimeMillis() > end) fail("never paused: ${j.state}"); Thread.sleep(5); j = c.jobs(listOf(made.id))[0] }
        assertTrue(j.actions.contains("RESUME"))
        c.action(made.id, "resume")
        while (j.state != "COMPLETED") { if (System.currentTimeMillis() > end) fail("never finished: ${j.state}"); Thread.sleep(10); j = c.jobs(listOf(made.id))[0] }
        assertEquals("Holiday.mp4", j.name); assertEquals(data.size.toLong(), j.total); assertEquals(data.size.toLong(), j.size); assertNotNull(j.fileUrl)

        // the tablet's engine copies it with the LAN fetcher: first 100 bytes, then the rest, with a validator
        val f = LanFetcher()
        val url = "http://127.0.0.1:${p.port}${j.fileUrl}"
        val a = f.open(FetchRequest(url, 0, 99, null, emptyMap()))
        assertEquals(206, a.status); val etag = a.header("ETag")!!; assertArrayEquals(data.copyOfRange(0, 100), a.body!!.readBytes()); a.close()
        val b = f.open(FetchRequest(url, 100, -1, app.kestrel.core.download.Cond.IfMatch(etag), emptyMap()))
        assertEquals(206, b.status); assertArrayEquals(data.copyOfRange(100, data.size), b.body!!.readBytes()); b.close()

        c.delete(made.id)
        assertEquals("GONE", c.jobs(listOf(made.id))[0].state)
        assertEquals(emptyList<RemoteJob>(), c.jobs(emptyList()))
        val gone = f.open(FetchRequest(url, 0, 9, null, emptyMap())); assertEquals(404, gone.status); gone.close()
    }

    @Test fun refusalsBecomeKindsTheTabletCanActOn() {
        val p = phone()
        val c = client(p)
        fun kind(spec: JobSpec): CourierException.Kind = try { c.create(spec); fail("should be refused"); throw IllegalStateException() } catch (e: CourierException) { e.kind }
        p.free = 10L * MiB
        assertEquals(CourierException.Kind.NO_SPACE, kind(JobSpec(cid(), "https://a.test/x")))
        p.free = 100L * 1024 * MiB
        p.dns = listOf(byteArrayOf(192.toByte(), 168.toByte(), 1, 1))
        assertEquals(CourierException.Kind.LOCAL_NET, kind(JobSpec(cid(), "https://a.test/x")))
        p.dns = listOf(byteArrayOf(8, 8, 8, 8))
        assertEquals(CourierException.Kind.UNSUPPORTED, kind(JobSpec(cid(), "https://a.test/x", kind = "stream")))
        assertEquals(CourierException.Kind.PROTOCOL, kind(JobSpec("short", "https://a.test/x")))                   // the tablet itself got it wrong
        assertEquals(CourierException.Kind.PROTOCOL, kind(JobSpec(cid(), "ftp://a.test/x")))
        try { c.action("nosuchjob", "pause"); fail() } catch (e: CourierException) { assertEquals(CourierException.Kind.NOT_FOUND, e.kind) }
    }

    @Test fun manyThreadsAskingTheSameThingAllGetAnswers() {
        val p = phone()
        val c = client(p)
        c.hello()
        val errors = CopyOnWriteArrayList<String>()
        val ts = (1..4).map { Thread { try { repeat(60) { c.status() } } catch (e: Exception) { errors.add(e.toString()) } }.also { it.start() } }
        ts.forEach { it.join(30_000) }
        assertTrue("identical signed requests in one millisecond must not collide: $errors", errors.isEmpty())
    }

    @Test fun speedAndProbe() {
        val p = phone(FakeServer(testBytes(2 * MiB)))
        val c = client(p)
        val s = c.speedtest(3_000_000)
        assertEquals(3_000_000L, s.bytes); assertTrue(s.ms >= 1); assertTrue(s.bytesPerSec > 0)
        val r = c.probe("https://files.test/pub/data.bin", 200_000)
        assertTrue(r.ok); assertEquals(200_000L, r.bytes); assertEquals(206, r.status)
        p.origin.failKind = FetchException.Kind.DNS
        val bad = c.probe("https://nowhere.test/x", 1000)
        assertFalse(bad.ok); assertEquals("dns", bad.error)
    }

    @Test fun theLanFetcherNeverLeavesThePrivateNetwork() {
        fun kind(url: String, resolve: (String) -> List<ByteArray>): FetchException.Kind = try { LanFetcher(resolve).open(FetchRequest(url, -1, -1, null, emptyMap())); fail("should refuse $url"); throw IllegalStateException() } catch (e: FetchException) { e.kind }
        val pub = { _: String -> listOf(byteArrayOf(8, 8, 8, 8)) }
        val priv = { _: String -> listOf(byteArrayOf(192.toByte(), 168.toByte(), 43, 1)) }
        assertEquals(FetchException.Kind.BAD_URL, kind("http://example.org/x", pub))
        assertEquals(FetchException.Kind.BAD_URL, kind("https://192.168.43.1/x", priv))
        assertEquals(FetchException.Kind.BAD_URL, kind("ftp://192.168.43.1/x", priv))
        assertEquals(FetchException.Kind.BAD_URL, kind("not a url", priv))
        assertEquals("one public answer among private ones", FetchException.Kind.BAD_URL, kind("http://mixed.test/x", { listOf(byteArrayOf(192.toByte(), 168.toByte(), 43, 1), byteArrayOf(8, 8, 8, 8)) }))
        val dead = ServerSocket(0).use { it.localPort }
        assertEquals(FetchException.Kind.OFFLINE, kind("http://127.0.0.1:$dead/x", { listOf(byteArrayOf(127, 0, 0, 1)) }))
        assertEquals(FetchException.Kind.DNS, kind("http://nowhere.test/x", { throw java.net.UnknownHostException("no") }))
    }

    @Test fun jobsAreParsedLeniently() {
        val j = RemoteJob.parse(mapOf("id" to "x", "state" to "GONE"))!!
        assertTrue(j.terminal); assertEquals(-1L, j.total); assertEquals(0L, j.done); assertEquals(emptyList<String>(), j.actions); assertNull(j.fileUrl)
        assertNull(RemoteJob.parse(mapOf("state" to "RUNNING")))
        val full = RemoteJob.parse(Json.parse("""{"id":"a","cid":"c","kind":"file","state":"RUNNING","reason":"NONE","detail":0,"name":"n","file":"f","mime":"m","total":10,"done":5,"bps":3,"eta":2,"conns":2,"estimated":true,"note":"hi","actions":["PAUSE","CANCEL",7],"created":1,"updated":2,"size":10}""") as Map<*, *>)!!
        assertEquals(listOf("PAUSE", "CANCEL"), full.actions); assertTrue(full.estimated); assertEquals("hi", full.note); assertFalse(full.terminal)
    }
}
