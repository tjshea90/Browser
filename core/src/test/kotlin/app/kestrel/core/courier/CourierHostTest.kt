package app.kestrel.core.courier

import app.kestrel.core.download.FakeServer
import app.kestrel.core.download.testBytes
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

/**
 * The phone side end to end: a real [CourierServer] on loopback, the real [CourierHost], the real download [Engine] with real files in a temp folder; only the origin
 * website is faked ([FakeServer]). The "tablet" is a small signing HTTP client in this file.
 */
class CourierHostTest {
    private val MiB = 1 shl 20
    private val secret = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    private val key = TEST_KEY
    private val root: File = Files.createTempDirectory("courier-host").toFile()
    private val cleanups = ArrayList<() -> Unit>()

    @After fun cleanUp() { cleanups.reversed().forEach { try { it() } catch (_: Exception) {} }; root.deleteRecursively() }

    private fun phone(origin: FakeServer, hostKey: ByteArray = key, dir: File = File(root, "phone"), clock: Skew = Skew()) = Phone(origin, root, cleanups, hostKey, dir, clock)

    private inner class Tablet(val base: String, val key: ByteArray = this@CourierHostTest.key) {
        var skew = 0L
        private var lastTs = 0L
        /** Strictly increasing, like the real client: two identical requests in one millisecond must not get one signature (the phone treats that as a replay). */
        @Synchronized fun nextTs(): Long {
            val now = System.currentTimeMillis() + skew
            val t = if (now > lastTs) now else if (lastTs - now < 1000) lastTs + 1 else now      // a big step back (the clock was corrected) is honoured
            lastTs = t; return t
        }
        fun raw(method: String, target: String, body: String? = null, sign: Boolean = true, signWith: ByteArray = key, ts: Long = nextTs()): Triple<Int, String, Map<String, List<String>>> {
            val c = URL(base + target).openConnection() as HttpURLConnection
            c.requestMethod = method; c.connectTimeout = 5000; c.readTimeout = 15_000
            val bytes = body?.toByteArray() ?: ByteArray(0)
            if (sign) { c.setRequestProperty("X-K-Ts", ts.toString()); c.setRequestProperty("X-K-Sig", Sign.sign(signWith, method, target, ts, bytes)) }
            if (body != null || method == "POST") { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json"); c.outputStream.use { it.write(bytes) } }
            val code = c.responseCode
            val text = String((if (code >= 400) c.errorStream else c.inputStream)?.readBytes() ?: ByteArray(0), Charsets.UTF_8)
            return Triple(code, text, c.headerFields.filterKeys { it != null })
        }
        fun call(method: String, target: String, body: String? = null) = raw(method, target, body).let { it.first to (Json.parse(it.second) as Map<*, *>) }
        fun create(cid: String, url: String = "https://files.test/pub/data.bin", extra: String = "") = call("POST", "/v1/jobs", """{"cid":"$cid","url":"$url"$extra}""")
        /** A signed GET whose body is only counted, not kept. */
        fun countBytes(target: String): Long {
            val ts = nextTs()
            val c = URL(base + target).openConnection() as HttpURLConnection
            c.setRequestProperty("X-K-Ts", ts.toString()); c.setRequestProperty("X-K-Sig", Sign.sign(key, "GET", target, ts, ByteArray(0)))
            var n = 0L
            c.inputStream.use { i -> val b = ByteArray(65536); while (true) { val k = i.read(b); if (k < 0) break; n += k } }
            return n
        }
        fun job(id: String): Map<*, *> = ((call("GET", "/v1/jobs?ids=$id").second.list("jobs"))!![0] as Map<*, *>)
        fun await(id: String, what: String, ms: Long = 30_000, p: (Map<*, *>) -> Boolean): Map<*, *> {
            val end = System.currentTimeMillis() + ms
            while (System.currentTimeMillis() < end) { val j = job(id); if (p(j)) return j; Thread.sleep(10) }
            throw AssertionError("timeout waiting for $what; last=${job(id)}")
        }
        fun finished(id: String) = await(id, "completion") { it.str("state") == "COMPLETED" }
        fun fetch(path: String, range: String? = null, method: String = "GET"): Triple<Int, ByteArray, Map<String, List<String>>> {
            val c = URL(base + path).openConnection() as HttpURLConnection
            c.requestMethod = method
            if (range != null) c.setRequestProperty("Range", range)
            val code = c.responseCode
            return Triple(code, (if (code >= 400) c.errorStream else c.inputStream)?.readBytes() ?: ByteArray(0), c.headerFields.filterKeys { it != null })
        }
    }

    private fun cid(n: Int) = "tablet-job-%04d".format(n)

    // ------------------------------------------------------------------------------------------------------------------ auth
    @Test fun helloProvesThePhoneKnowsTheKey() {
        val p = phone(FakeServer(testBytes(1000)))
        val t = Tablet(p.base)
        val c = Sign.b64(ByteArray(16) { it.toByte() })
        val (code, body, _) = t.raw("GET", "/v1/hello?c=$c", sign = false)
        assertEquals(200, code)
        val j = Json.parse(body) as Map<*, *>
        assertEquals("kestrel-courier", j.str("app")); assertEquals(1L, j.long("v"))
        assertTrue(Sign.helloProofOk(key, c, j.long("t")!!, j.str("proof")))
        assertFalse("an impostor with another key cannot produce the proof", Sign.helloProofOk(Sign.deriveKey(ByteArray(10)), c, j.long("t")!!, j.str("proof")))
        assertEquals(400, t.raw("GET", "/v1/hello", sign = false).first)
        assertEquals(400, t.raw("GET", "/v1/hello?c=x", sign = false).first)
        assertEquals(405, t.raw("POST", "/v1/hello?c=$c", sign = false).first)
    }

    @Test fun unsignedAndWronglySignedRequestsAreRefusedAndGuessingIsSlowed() {
        val p = phone(FakeServer(testBytes(1000)))
        val t = Tablet(p.base)
        assertEquals(401, t.raw("GET", "/v1/status", sign = false).first)
        assertEquals(401, t.raw("GET", "/v1/status", signWith = Sign.deriveKey(ByteArray(10))).first)
        assertEquals("a good request in between", 200, t.raw("GET", "/v1/status").first)                  // and it wipes the count of bad ones
        for (i in 1..2) assertEquals(401, t.raw("GET", "/v1/status", sign = false).first)
        assertEquals(401, t.raw("GET", "/v1/status", signWith = Sign.deriveKey(ByteArray(10))).first)
        assertEquals(401, t.raw("GET", "/v1/status", sign = false).first)
        assertEquals(200, t.raw("GET", "/v1/status").first)                                                  // four bad ones so far: still open
        for (i in 1..5) t.raw("GET", "/v1/status", sign = false)
        val blocked = t.raw("GET", "/v1/status")
        assertEquals("five bad requests in a row block the address for a minute, even for a good one", 429, blocked.first)
        assertEquals("60", blocked.third["Retry-After"]?.firstOrNull())
    }

    @Test fun aWrongClockIsReportedNotPunished() {
        val p = phone(FakeServer(testBytes(1000)))
        val t = Tablet(p.base)
        t.skew = 5 * 60_000
        for (i in 1..8) {
            val (code, body, _) = t.raw("GET", "/v1/status")
            assertEquals(401, code)
            val j = Json.parse(body) as Map<*, *>
            assertEquals("skew", j.str("error")); assertTrue(Math.abs(j.long("t")!! - System.currentTimeMillis()) < 5000)
        }
        t.skew = 0
        assertEquals("the clock was corrected: not blocked", 200, t.raw("GET", "/v1/status").first)
    }

    @Test fun aSignedRequestCannotBePlayedTwice() {
        val p = phone(FakeServer(testBytes(1000)))
        val t = Tablet(p.base)
        val ts = System.currentTimeMillis()
        assertEquals(200, t.raw("GET", "/v1/status", ts = ts).first)
        assertEquals(401, t.raw("GET", "/v1/status", ts = ts).first)
    }

    // ------------------------------------------------------------------------------------------------------------------ jobs
    @Test(timeout = 90_000) fun aDownloadRunsOnThePhoneAndTheTabletCollectsTheFile() {
        val data = testBytes(3 * MiB + 123)
        val p = phone(FakeServer(data))
        val t = Tablet(p.base)
        val (code, j) = t.create(cid(1), extra = ""","name":"My Video.mp4","nameFixed":true,"size":${data.size}""")
        assertEquals(201, code)
        val id = j.str("id")!!
        assertEquals(cid(1), j.str("cid")); assertEquals("file", j.str("kind"))
        val done = t.finished(id)
        assertEquals(data.size.toLong(), done.long("total")); assertEquals(data.size.toLong(), done.long("done")); assertEquals(data.size.toLong(), done.long("size"))
        assertEquals("My Video.mp4", done.str("name")); assertEquals("My Video.mp4", done.str("file")); assertEquals("NONE", done.str("reason"))
        val fileUrl = done.str("fileUrl")!!
        assertTrue(fileUrl, fileUrl.startsWith("/v1/f/") && fileUrl.endsWith("/My%20Video.mp4"))
        val (c1, body, h1) = t.fetch(fileUrl)
        assertEquals(200, c1); assertArrayEquals(data, body)
        assertEquals("bytes", h1["Accept-Ranges"]?.first()); assertTrue(h1["ETag"]!!.first().startsWith("\""))
        assertTrue(h1["Content-Disposition"]!!.first().contains("filename*=UTF-8''My%20Video.mp4"))
        val (c2, part, h2) = t.fetch(fileUrl, "bytes=1000-1999")
        assertEquals(206, c2); assertArrayEquals(data.copyOfRange(1000, 2000), part); assertEquals("bytes 1000-1999/${data.size}", h2["Content-Range"]!!.first())
        val (c3, none, h3) = t.fetch(fileUrl, method = "HEAD")
        assertEquals(200, c3); assertEquals(0, none.size); assertEquals(data.size.toString(), h3["Content-Length"]!!.first())
        assertEquals("anyone without the token gets nothing", 404, t.fetch("/v1/f/AAAAAAAAAAAAAAAAAAAAAA/My%20Video.mp4").first)
        assertEquals(404, t.fetch("/v1/f/").first)

        // the tablet has it: delete on the phone frees the file and kills the address
        assertEquals(200, t.raw("DELETE", "/v1/jobs/$id").first)
        assertEquals("GONE", t.job(id).str("state"))
        assertEquals(404, t.fetch(fileUrl).first)
        awaitEmpty(File(p.dir, "staging"))
        assertEquals("deleting twice is fine", 200, t.raw("DELETE", "/v1/jobs/$id").first)
    }

    /** The engine removes files on its own thread a moment after the answer went out. */
    private fun awaitEmpty(dir: File) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) { if ((dir.listFiles() ?: emptyArray()).isEmpty()) return; Thread.sleep(10) }
        throw AssertionError("the staging folder was not emptied: ${dir.walkTopDown().toList()}")
    }

    @Test(timeout = 60_000) fun theSameRequestTwiceIsTheSameJob() {
        val origin = FakeServer(testBytes(MiB))
        val p = phone(origin)
        val t = Tablet(p.base)
        val a = t.create(cid(2)); val b = t.create(cid(2))
        assertEquals(201, a.first); assertEquals(200, b.first); assertEquals(a.second.str("id"), b.second.str("id"))
        t.finished(a.second.str("id")!!)
        assertEquals("one job, one download", 1, p.host.jobCount)
        assertNotEquals(a.second.str("id"), t.create(cid(3)).second.str("id"))
    }

    @Test(timeout = 60_000) fun theRowButtonsReachThePhone() {
        val data = testBytes(4 * MiB)
        val origin = FakeServer(data).also { it.chunkDelayMs = 10; it.chunk = 16 * 1024 }
        val p = phone(origin)
        val t = Tablet(p.base)
        val id = t.create(cid(4)).second.str("id")!!
        val running = t.await(id, "progress") { it.str("state") == "RUNNING" && (it.long("done") ?: 0) > 0 }
        assertTrue((running.list("actions") ?: emptyList<Any?>()).containsAll(listOf("PAUSE", "CANCEL")))
        assertEquals(202, t.raw("POST", "/v1/jobs/$id/pause").first)
        val paused = t.await(id, "paused") { it.str("state") == "PAUSED_USER" }
        assertTrue(paused.list("actions")!!.contains("RESUME"))
        val at = paused.long("done")!!
        Thread.sleep(150)
        assertEquals("nothing moves while paused", at, t.job(id).long("done"))
        assertEquals(202, t.raw("POST", "/v1/jobs/$id/resume").first)
        val done = t.finished(id)
        val (_, body, _) = t.fetch(done.str("fileUrl")!!)
        assertArrayEquals("pause and resume on the phone give an intact file", data, body)

        val other = t.create(cid(5), "https://files.test/pub/other.bin").second.str("id")!!
        t.await(other, "progress") { (it.long("done") ?: 0) > 0 }
        assertEquals(202, t.raw("POST", "/v1/jobs/$other/cancel").first)
        t.await(other, "canceled") { it.str("state") == "CANCELED" }
        assertEquals(404, t.raw("POST", "/v1/jobs/$id/explode").first)
        assertEquals(404, t.raw("POST", "/v1/jobs/nope/pause").first)
    }

    @Test fun badRequestsAreRefusedBeforeAnythingStarts() {
        val origin = FakeServer(testBytes(1000))
        val p = phone(origin)
        val t = Tablet(p.base)
        assertEquals(400, t.raw("POST", "/v1/jobs", "not json").first)
        assertEquals(400, t.raw("POST", "/v1/jobs", "[1]").first)
        assertEquals(400, t.raw("POST", "/v1/jobs", """{"url":"https://a.test/x"}""").first)
        assertEquals("cid too short", 400, t.raw("POST", "/v1/jobs", """{"cid":"abc","url":"https://a.test/x"}""").first)
        assertEquals("cid with a slash", 400, t.raw("POST", "/v1/jobs", """{"cid":"aaaaaaaa/bbbb","url":"https://a.test/x"}""").first)
        assertEquals(400, t.raw("POST", "/v1/jobs", """{"cid":"${cid(6)}"}""").first)
        for (u in listOf("ftp://a.test/x", "file:///etc/passwd", "javascript:alert(1)", "not a url", "")) assertEquals(u, 400, t.create(cid(7), u).first)
        assertEquals(400, t.create(cid(7), "https://a.test/" + "a".repeat(9000)).first)
        assertEquals("streams are not offered yet: the tablet falls back", 501, t.create(cid(8), extra = ""","kind":"stream"""").first)
        assertEquals(0, p.host.jobCount); assertEquals(0, origin.seen.size)
    }

    @Test fun addressesOnPrivateNetworksAreRefused() {
        val origin = FakeServer(testBytes(1000))
        val p = phone(origin)
        val t = Tablet(p.base)
        for (ip in listOf(byteArrayOf(127, 0, 0, 1), byteArrayOf(192.toByte(), 168.toByte(), 43, 1), byteArrayOf(10, 0, 0, 5), byteArrayOf(100, 64, 0, 1))) {
            p.dns = listOf(ip)
            val (code, j) = t.create(cid(9), "https://innocent.example/file.bin")
            assertEquals(403, code); assertEquals("local_net", j.str("error"))
        }
        p.dns = listOf(byteArrayOf(8, 8, 8, 8), byteArrayOf(192.toByte(), 168.toByte(), 1, 1))
        assertEquals("one private answer among public ones is enough to refuse", 403, t.create(cid(9), "https://rebind.example/f").first)
        assertEquals(0, origin.seen.size)
    }

    @Test fun aSoundAtAPrivateAddressIsRefusedLikeThePicture() {
        val p = Phone(FakeServer(testBytes(1000)), root, cleanups, cdn = app.kestrel.core.media.FakeCdn())
        val t = Tablet(p.base)
        val stream = ""","kind":"stream","stream":{"playlistUrl":"https://cdn.example/v/index.m3u8","nameStem":"Clip","companion":{"playlistUrl":"URL","nameStem":"Clip","ext":"m4a"}}"""
        p.dns = listOf(byteArrayOf(93, 184.toByte(), 216.toByte(), 34))
        p.dnsFor["sound.example"] = listOf(byteArrayOf(192.toByte(), 168.toByte(), 43, 1))
        assertEquals("a sound whose name points at the local network", 403, t.create(cid(11), "https://cdn.example/v/index.m3u8", stream.replace("URL", "https://sound.example/a.m3u8")).first)
        assertEquals("a sound at a local address", 403, t.create(cid(15), "https://cdn.example/v/index.m3u8", stream.replace("URL", "http://127.0.0.1/a.m3u8")).first)
        assertEquals("a sound that is not a web address", 400, t.create(cid(12), "https://cdn.example/v/index.m3u8", stream.replace("URL", "ftp://a.test/a.m3u8")).first)
        assertEquals("a sound without an address", 400, t.create(cid(13), "https://cdn.example/v/index.m3u8", ""","kind":"stream","stream":{"playlistUrl":"https://cdn.example/v/index.m3u8","companion":{"nameStem":"x"}}""").first)
        assertEquals("both public: accepted", 201, t.create(cid(14), "https://cdn.example/v/index.m3u8", stream.replace("URL", "https://cdn.example/a/index.m3u8")).first)
        assertEquals(1, p.host.jobCount)
    }

    @Test fun aFullPhoneSaysSoUpFront() {
        val p = phone(FakeServer(testBytes(1000)))
        val t = Tablet(p.base)
        p.free = 10L * MiB
        val (c, j) = t.create(cid(10))
        assertEquals(507, c); assertEquals("no_space", j.str("error")); assertEquals(10L * MiB, j.long("free"))
        p.free = 500L * MiB
        assertEquals(507, t.create(cid(11), extra = ""","size":${600L * MiB}""").first)
        assertEquals(201, t.create(cid(12), extra = ""","size":${100L * MiB}""").first)
    }

    @Test(timeout = 60_000) fun onlyHarmlessHeadersReachTheOrigin() {
        val origin = FakeServer(testBytes(MiB))
        val p = phone(origin)
        val t = Tablet(p.base)
        val id = t.create(cid(13), extra = ""","headers":{"Referer":"https://site.test/page","User-Agent":"Tablet/1.0","Cookie":"a=b","Authorization":"Bearer x","Host":"evil.test","X-Evil":"1","accept":"*/*","Origin":"https://site.test","Cookie2":"x","Accept-Language":"en"}""").second.str("id")!!
        t.finished(id)
        val sent = p.spy.headers.first()
        val names = sent.keys.map { it.lowercase() }.toSet()
        assertTrue(names.toString(), names.containsAll(listOf("referer", "user-agent", "cookie", "accept", "origin", "accept-language")))
        for (forbidden in listOf("authorization", "host", "x-evil", "cookie2")) assertFalse("$forbidden must not be forwarded", forbidden in names)
        assertEquals("a=b", sent.entries.first { it.key.equals("cookie", true) }.value)
        val bad = t.raw("POST", "/v1/jobs", """{"cid":"${cid(14)}","url":"https://a.test/x","headers":{"Cookie":"a=b\r\nX-Injected: 1"}}""")
        assertEquals(201, bad.first)
        val id2 = (Json.parse(bad.second) as Map<*, *>).str("id")!!
        t.await(id2, "done or failed") { it.str("state") == "COMPLETED" || it.str("state") == "FAILED" }
        assertTrue("a header value with a line break is dropped", p.spy.headers.last().keys.none { it.equals("cookie", true) })
    }

    // ------------------------------------------------------------------------------------------------------------------ restart and upkeep
    @Test(timeout = 90_000) fun aRestartedPhoneStillKnowsItsFinishedJobs() {
        val data = testBytes(2 * MiB)
        val p1 = phone(FakeServer(data))
        val t1 = Tablet(p1.base)
        val id = t1.create(cid(20), extra = ""","name":"keep.bin"""").second.str("id")!!
        val before = t1.finished(id)
        p1.stop()
        val p2 = phone(FakeServer(data), dir = p1.dir)                           // the app was killed and started again on the same folders
        val t2 = Tablet(p2.base)
        val after = t2.finished(id)                                                  // (if the kill came before the engine wrote its last record, the job carries on by itself and finishes again)
        assertEquals(before.str("fileUrl"), after.str("fileUrl"))
        assertArrayEquals(data, t2.fetch(after.str("fileUrl")!!).second)
        assertEquals("a repeated create after the restart is the same job", id, t2.create(cid(20)).second.str("id"))
    }

    @Test(timeout = 90_000) fun finishedJobsNobodyCollectedAreRemovedAfterADay() {
        val p = phone(FakeServer(testBytes(MiB)))
        val t = Tablet(p.base)
        val id = t.create(cid(21)).second.str("id")!!
        val url = t.finished(id).str("fileUrl")!!
        p.host.tick()
        assertEquals("still there after a minute", "COMPLETED", t.job(id).str("state"))
        p.clock.ms = 25L * 3600_000
        t.skew = p.clock.ms                                                                                   // the tablet's clock follows the phone's (as it does after a hello)
        p.host.tick()
        assertEquals("GONE", t.job(id).str("state")); assertEquals(404, t.fetch(url).first)
        awaitEmpty(File(p.dir, "staging"))
    }

    @Test fun statusSpeedtestAndProbe() {
        val origin = FakeServer(testBytes(2 * MiB))
        val p = phone(origin)
        val t = Tablet(p.base)
        val (c, s) = t.call("GET", "/v1/status")
        assertEquals(200, c); assertEquals("Test phone", s.str("name")); assertEquals(p.free, s.long("free")); assertEquals(0L, s.long("jobs"))
        val (_, z, h) = t.raw("GET", "/v1/speedtest?bytes=1000000").let { Triple(it.first, it.second.length, it.third) }
        assertEquals(1_000_000, z); assertEquals("1000000", h["Content-Length"]!!.first())
        assertEquals("a speed test needs the key too", 401, t.raw("GET", "/v1/speedtest?bytes=10", sign = false).first)
        assertEquals("the size is capped", 64L * MiB, t.countBytes("/v1/speedtest?bytes=999999999999"))
        val (pc, pj) = t.call("POST", "/v1/probe", """{"url":"https://files.test/pub/data.bin","bytes":100000}""")
        assertEquals(200, pc); assertEquals(true, pj.bool("ok")); assertEquals(100000L, pj.long("bytes")); assertEquals(206L, pj.long("status")); assertNotNull(pj.long("bps"))
        origin.failKind = app.kestrel.core.download.FetchException.Kind.DNS
        assertEquals(false, t.call("POST", "/v1/probe", """{"url":"https://nowhere.test/x","bytes":1000}""").second.bool("ok"))
    }

    @Test fun unknownRoutesAndMethods() {
        val p = phone(FakeServer(testBytes(1000)))
        val t = Tablet(p.base)
        assertEquals(404, t.raw("GET", "/").first)
        assertEquals(404, t.raw("GET", "/v2/status").first)
        assertEquals(404, t.raw("GET", "/v1/nothing").first)
        assertEquals(405, t.raw("PUT", "/v1/jobs", "{}").first)
        assertEquals(405, t.raw("DELETE", "/v1/status").first)
        assertEquals(404, t.raw("GET", "/v1/jobs/a/b/c/d").first)
        assertEquals("a list without ids lists everything (nothing yet)", emptyList<Any?>(), (Json.parse(t.raw("GET", "/v1/jobs").second) as Map<*, *>).list("jobs"))
    }
}
