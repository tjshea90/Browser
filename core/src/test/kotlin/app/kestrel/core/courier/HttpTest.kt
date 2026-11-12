package app.kestrel.core.courier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream

class HttpTest {
    private fun stream(s: String): BufferedInputStream = BufferedInputStream(ByteArrayInputStream(s.toByteArray(Charsets.ISO_8859_1)))

    /** Gives the parser one byte per read, like a slow wifi link. */
    private class OneByte(val s: InputStream) : InputStream() {
        override fun read(): Int = s.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = if (len == 0) 0 else { val c = s.read(); if (c < 0) -1 else { b[off] = c.toByte(); 1 } }
    }

    private fun refused(text: String, status: Int) {
        try { HttpParser.read(stream(text)); fail("should have been refused with $status: ${text.take(60)}") } catch (e: HttpException) { assertEquals(text.take(60), status, e.status) }
    }

    @Test fun parsesAGetWithQueryAndHeaders() {
        val r = HttpParser.read(stream("GET /v1/jobs?ids=a%2Cb&x=1+2&flag HTTP/1.1\r\nHost: 192.168.43.1:8765\r\nX-K-Ts: 123\r\nX-K-SIG: abc\r\n\r\n"))!!
        assertEquals("GET", r.method); assertEquals("/v1/jobs", r.path); assertEquals("/v1/jobs?ids=a%2Cb&x=1+2&flag", r.target)
        assertEquals(mapOf("ids" to "a,b", "x" to "1 2", "flag" to ""), r.query)
        assertEquals("123", r.header("X-K-Ts")); assertEquals("abc", r.headers[Sign.H_SIG]); assertEquals("192.168.43.1:8765", r.header("host"))
        assertEquals(0, r.body.size); assertTrue(r.http11); assertTrue(r.keepAlive)
    }

    @Test fun keepAliveRules() {
        fun ka(v: String, conn: String?) = HttpParser.read(stream("GET / $v\r\n" + (if (conn != null) "Connection: $conn\r\n" else "") + "\r\n"))!!.keepAlive
        assertTrue(ka("HTTP/1.1", null)); assertFalse(ka("HTTP/1.1", "close")); assertFalse(ka("HTTP/1.1", "Close"))
        assertFalse(ka("HTTP/1.0", null)); assertTrue(ka("HTTP/1.0", "keep-alive"))
    }

    @Test fun readsABodyAndToleratesBareLineFeedsAndStrayBlankLines() {
        val r = HttpParser.read(stream("\r\n\r\nPOST /v1/jobs HTTP/1.1\nContent-Length: 5\n\nhello"))!!
        assertEquals("POST", r.method); assertEquals("hello", String(r.body))
    }

    @Test fun sizeOfTheReadsDoesNotChangeTheResult() {
        val text = "POST /a%20b?q=1 HTTP/1.1\r\nContent-Length: 11\r\nX-Y: z\r\n\r\nhello world" + "GET /next HTTP/1.1\r\n\r\n"
        val whole = BufferedInputStream(ByteArrayInputStream(text.toByteArray()))
        val slow = BufferedInputStream(OneByte(ByteArrayInputStream(text.toByteArray())), 1)
        for (s in listOf(whole, slow)) {
            val a = HttpParser.read(s)!!; assertEquals("/a b", a.path); assertEquals("hello world", String(a.body)); assertEquals("z", a.header("x-y"))
            val b = HttpParser.read(s)!!; assertEquals("/next", b.path)
            assertNull("clean end of stream", HttpParser.read(s))
        }
    }

    @Test fun pipelinedRequestsComeOutInOrder() {
        val s = stream("GET /1 HTTP/1.1\r\n\r\nPOST /2 HTTP/1.1\r\nContent-Length: 2\r\n\r\nabGET /3 HTTP/1.1\r\n\r\n")
        assertEquals(listOf("/1", "/2", "/3"), listOf(HttpParser.read(s)!!.path, HttpParser.read(s)!!.path, HttpParser.read(s)!!.path))
    }

    @Test fun truncatedInputIsAnIoErrorNotARequest() {
        for (t in listOf("GET / HTT", "GET / HTTP/1.1\r\nHost: x", "GET / HTTP/1.1\r\nHost: x\r\n", "POST / HTTP/1.1\r\nContent-Length: 10\r\n\r\nabc")) {
            try { HttpParser.read(stream(t)); fail("truncated: $t") } catch (_: EOFException) {}
        }
        assertNull(HttpParser.read(stream("")))
    }

    @Test fun refusesBadRequestLines() {
        refused("GET /\r\n\r\n", 400)
        refused("GET / HTTP/1.1 extra\r\n\r\n", 400)
        refused("GET  / HTTP/1.1\r\n\r\n", 400)
        refused("GET / HTTP/2\r\n\r\n", 505)
        refused("get / HTTP/1.1\r\n\r\n", 501)
        refused("BREW / HTTP/1.1\r\n\r\n", 501)
        refused("GET v1 HTTP/1.1\r\n\r\n", 400)
        refused("GET /a\\b HTTP/1.1\r\n\r\n", 400)
        refused("GET /a\u0001b HTTP/1.1\r\n\r\n", 400)
        refused("GET /a%00b HTTP/1.1\r\n\r\n", 400)
        refused("GET /a%zz HTTP/1.1\r\n\r\n", 400)
        refused("GET /a% HTTP/1.1\r\n\r\n", 400)
        refused("\r\n\r\n\r\n\r\n\r\nGET / HTTP/1.1\r\n\r\n", 400)
    }

    @Test fun refusesBadHeadersAndBodies() {
        refused("GET / HTTP/1.1\r\nBad Name: x\r\n\r\n", 400)
        refused("GET / HTTP/1.1\r\n: x\r\n\r\n", 400)
        refused("GET / HTTP/1.1\r\nNoColon\r\n\r\n", 400)
        refused("GET / HTTP/1.1\r\nA: b\r\n folded\r\n\r\n", 400)
        refused("POST / HTTP/1.1\r\n\r\n", 411)
        refused("POST / HTTP/1.1\r\nContent-Length: -1\r\n\r\n", 400)
        refused("POST / HTTP/1.1\r\nContent-Length: 12abc\r\n\r\n", 400)
        refused("POST / HTTP/1.1\r\nContent-Length: +5\r\n\r\n", 400)
        refused("POST / HTTP/1.1\r\nContent-Length: 5\r\nContent-Length: 6\r\n\r\nhello!", 400)
        refused("POST / HTTP/1.1\r\nContent-Length: 999999999999\r\n\r\n", 400)
        refused("POST / HTTP/1.1\r\nContent-Length: ${HttpParser.MAX_BODY + 1}\r\n\r\n", 413)
        refused("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\n0\r\n\r\n", 501)
        val ok = HttpParser.read(stream("POST / HTTP/1.1\r\nContent-Length: 2\r\nContent-Length: 2\r\n\r\nhi"))!!
        assertEquals("hi", String(ok.body))
    }

    @Test fun limitsLinesAndHeaderCounts() {
        refused("GET /" + "a".repeat(9000) + " HTTP/1.1\r\n\r\n", 431)
        refused("GET / HTTP/1.1\r\nX: " + "a".repeat(9000) + "\r\n\r\n", 431)
        val many = (1..65).joinToString("") { "H$it: v\r\n" }
        refused("GET / HTTP/1.1\r\n$many\r\n", 431)
        val big = (1..40).joinToString("") { "H$it: " + "v".repeat(500) + "\r\n" }
        refused("GET / HTTP/1.1\r\n$big\r\n", 431)
        val sixtyFour = (1..64).joinToString("") { "H$it: v\r\n" }
        assertEquals(64, HttpParser.read(stream("GET / HTTP/1.1\r\n$sixtyFour\r\n"))!!.headers.size)
    }

    @Test fun duplicateHeadersAreJoined() {
        val r = HttpParser.read(stream("GET / HTTP/1.1\r\nAccept: a\r\nAccept: b\r\n\r\n"))!!
        assertEquals("a, b", r.header("accept"))
    }

    @Test fun percentDecoding() {
        assertEquals("A b", HttpParser.percentDecode("%41%20b", false))
        assertEquals("a+b", HttpParser.percentDecode("a+b", false)); assertEquals("a b", HttpParser.percentDecode("a+b", true))
        assertEquals("é", HttpParser.percentDecode("%C3%A9", false)); assertEquals("é中", HttpParser.percentDecode("é中", false))
        assertEquals("😀", HttpParser.percentDecode("%F0%9F%98%80", false)); assertEquals("😀", HttpParser.percentDecode("😀", false))
        assertEquals("100%", HttpParser.percentDecode("100%25", false))
    }

    @Test fun queryLimits() {
        assertEquals(32, HttpParser.parseQuery((1..32).joinToString("&") { "k$it=v" }).size)
        try { HttpParser.parseQuery((1..33).joinToString("&") { "k$it=v" }); fail() } catch (e: HttpException) { assertEquals(400, e.status) }
        assertEquals(mapOf("a" to "2"), HttpParser.parseQuery("a=1&&a=2"))
        assertTrue(HttpParser.parseQuery("").isEmpty())
    }

    // ------------------------------------------------------------------------------------------------------ ranges
    private fun range(h: String, len: Long = 1000): String = when (val r = RangeSpec.parse(h, len)) {
        is RangeSpec.Part -> "${r.from}-${r.to}"; RangeSpec.Unsatisfiable -> "416"; RangeSpec.Ignore -> "ignore"
    }

    @Test fun rangeTable() {
        assertEquals("0-99", range("bytes=0-99")); assertEquals("900-999", range("bytes=900-")); assertEquals("900-999", range("bytes=-100"))
        assertEquals("0-999", range("bytes=-5000")); assertEquals("0-0", range("bytes=0-0")); assertEquals("999-999", range("bytes=999-999"))
        assertEquals("0-999", range("bytes=0-5000")); assertEquals("0-99", range(" Bytes= 0 - 99 "))
        assertEquals("416", range("bytes=1000-")); assertEquals("416", range("bytes=1000-2000")); assertEquals("416", range("bytes=-0"))
        assertEquals("416", range("bytes=0-", 0)); assertEquals("416", range("bytes=-5", 0))
        for (h in listOf("bytes=5-2", "bytes=0-1,5-9", "items=0-5", "bytes=a-b", "bytes=+5-9", "bytes=5-+9", "bytes", "bytes=", "bytes=-", "bytes=--5", "bytes=0-99999999999999999999", "")) assertEquals(h, "ignore", range(h))
    }

    // ------------------------------------------------------------------------------------------------------ writer
    private class Answer(val status: Int, val headers: Map<String, String>, val body: ByteArray)

    private fun answer(req: HttpRequest?, resp: HttpResponse, keep: Boolean = true): Pair<Answer, Boolean> {
        val out = ByteArrayOutputStream()
        val open = HttpWriter.write(out, req, resp, keep)
        val raw = out.toByteArray()
        val sep = String(raw, Charsets.ISO_8859_1).indexOf("\r\n\r\n")
        val head = String(raw, 0, sep, Charsets.ISO_8859_1).split("\r\n")
        val headers = head.drop(1).associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
        return Answer(head[0].split(' ')[1].toInt(), headers, raw.copyOfRange(sep + 4, raw.size)) to open
    }

    private fun get(range: String? = null, method: String = "GET") =
        HttpParser.read(stream("$method /f HTTP/1.1\r\n" + (if (range != null) "Range: $range\r\n" else "") + "\r\n"))!!

    private val data = ByteArray(1000) { (it * 7).toByte() }

    @Test fun writesJsonWithLengthAndConnection() {
        val (a, open) = answer(get(), HttpResponse.json(200, mapOf("ok" to true)))
        assertEquals(200, a.status); assertEquals("""{"ok":true}""", String(a.body)); assertEquals("11", a.headers["content-length"])
        assertEquals("keep-alive", a.headers["connection"]); assertEquals("nosniff", a.headers["x-content-type-options"]); assertEquals("no-store", a.headers["cache-control"])
        assertTrue(open)
        val (b, open2) = answer(get(), HttpResponse(200, close = true))
        assertEquals("close", b.headers["connection"]); assertFalse(open2); assertEquals("0", b.headers["content-length"])
        assertFalse(answer(get(), HttpResponse(200), keep = false).second)
    }

    @Test fun servesRangesOnlyWhenTheAnswerAllowsIt() {
        val file = HttpResponse(200, mapOf("ETag" to "\"x\""), BytesBody(data), ranged = true)
        val (full, _) = answer(get(), file)
        assertEquals(200, full.status); assertEquals("bytes", full.headers["accept-ranges"]); assertEquals(data.toList(), full.body.toList()); assertEquals("\"x\"", full.headers["etag"])
        val (p, _) = answer(get("bytes=10-19"), file)
        assertEquals(206, p.status); assertEquals("bytes 10-19/1000", p.headers["content-range"]); assertEquals("10", p.headers["content-length"]); assertEquals(data.copyOfRange(10, 20).toList(), p.body.toList())
        val (tail, _) = answer(get("bytes=-5"), file)
        assertEquals(206, tail.status); assertEquals(data.copyOfRange(995, 1000).toList(), tail.body.toList())
        val (open, _) = answer(get("bytes=990-"), file)
        assertEquals("bytes 990-999/1000", open.headers["content-range"])
        val (bad, _) = answer(get("bytes=1000-"), file)
        assertEquals(416, bad.status); assertEquals("bytes */1000", bad.headers["content-range"]); assertEquals(0, bad.body.size)
        val (odd, _) = answer(get("bytes=0-1,5-6"), file)
        assertEquals("a range the server does not understand is answered with the whole file", 200, odd.status); assertEquals(1000, odd.body.size)
        val (json, _) = answer(get("bytes=0-3"), HttpResponse.json(200, mapOf("a" to 1)))
        assertEquals("a Range on a non-file answer is ignored", 200, json.status)
    }

    @Test fun headHasLengthButNoBody() {
        val (h, _) = answer(get(method = "HEAD"), HttpResponse(200, body = BytesBody(data), ranged = true))
        assertEquals(200, h.status); assertEquals("1000", h.headers["content-length"]); assertEquals(0, h.body.size)
        val (hp, _) = answer(get("bytes=0-9", "HEAD"), HttpResponse(200, body = BytesBody(data), ranged = true))
        assertEquals(206, hp.status); assertEquals("10", hp.headers["content-length"]); assertEquals(0, hp.body.size)
    }

    @Test fun errorsBeforeARequestExistAreStillAnswered() {
        val (a, open) = answer(null, HttpResponse.error(400, "bad request"), keep = false)
        assertEquals(400, a.status); assertFalse(open); assertEquals("""{"error":"bad request"}""", String(a.body))
    }

    @Test fun headerInjectionIsRefused() {
        for (resp in listOf(HttpResponse(200, mapOf("X" to "a\r\nSet-Cookie: b")), HttpResponse(200, mapOf("X\nY" to "a")), HttpResponse(200, mapOf("X:Y" to "a")))) {
            try { answer(get(), resp); fail("header injection must be refused") } catch (_: IllegalArgumentException) {}
        }
    }
}
