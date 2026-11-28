package app.kestrel.core.download

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * PDL1 (owner, 2026-11-28: a zip from Proton Drive failed with "The site sent a web page instead of the file", Brave saved it): a download the browser engine hands over WITH its own response body
 * (a service worker stream, a blob: file) is saved from that body when a second request cannot give the file. The whole engine against an in-memory server, sink and store.
 */
class HandedTest {
    private val MiB = 1 shl 20
    private val harnesses = ArrayList<H>()
    @After fun cleanup() { harnesses.forEach { it.stop() } }

    private val page = "<!DOCTYPE html><html><head><title>Proton Drive</title></head><body><div id=app></div></body></html>".toByteArray()
    private val swUrl = "https://drive.test/sw/9c1f2b"
    /** What the engine reports for a stream a page made itself: the headers its script set, and no Date. */
    private fun swHeaders(size: Long? = null, name: String = "termux.zip") = HashMap<String, String>().also {
        it["Content-Type"] = "application/octet-stream; charset=utf-8"; it["Content-Disposition"] = "attachment; filename*=UTF-8''$name"
        if (size != null) it["Content-Length"] = size.toString()
    }

    private fun cfg() = Config(
        maxActive = 2, parallel = false, maxConnsPerDownload = 3, maxConnsTotal = 6, windowBytes = 1L shl 20, minGrowRemaining = 1L shl 20, minGrowEtaSec = 0,
        rampMs = 0, evalMs = 25, growGain = 0.0, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4,
    )

    private inner class H(val server: FakeServer, val handed: HandedStreams = HandedStreams(), val sinks: FakeSinks = FakeSinks(), val store: FakeStore = FakeStore()) {
        val net = FakeNet()
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "test-control").also { it.isDaemon = true } }
        val engine = Engine(
            { kind -> when (kind) { FETCHER_HANDED -> HandedFetcher(server, handed); FETCHER_ONE_SHOT -> HandedFetcher(server, handed, onlyHanded = true); else -> server } },
            sinks, store, net, exec, Clock.System, cfg(),
        )
        @Volatile var ticking = true
        init {
            Thread { while (ticking) { engine.tick(); try { Thread.sleep(20) } catch (e: InterruptedException) { return@Thread } } }.also { it.isDaemon = true; it.start() }
            harnesses.add(this)
        }
        fun stop() { ticking = false; exec.shutdownNow() }
        fun view(id: Long) = engine.view(id)
        fun await(id: Long, what: String, ms: Long = 30_000, p: (DownloadView?) -> Boolean): DownloadView? {
            val end = System.currentTimeMillis() + ms
            while (System.currentTimeMillis() < end) { val v = view(id); if (p(v)) return v; Thread.sleep(5) }
            throw AssertionError("timeout waiting for $what; last=${view(id)?.state} ${view(id)?.reason} done=${view(id)?.done}/${view(id)?.total}")
        }
        fun completed(id: Long) = await(id, "completion") { it?.state == DlState.COMPLETED }!!
        fun failed(id: Long) = await(id, "failure") { it?.state == DlState.FAILED }!!
        fun row(id: Long, what: String, p: (Rec?) -> Boolean): Rec {
            val end = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < end) { val r = store.rows[id]; if (p(r)) return r!!; Thread.sleep(5) }
            throw AssertionError("store row never matched: $what; row state=${store.rows[id]?.state}")
        }
        fun sink(v: DownloadView): FakeSink = sinks.sinks[v.destUri]!!
    }

    /** The engine's own stream: records that it was closed. */
    private class Tracked(data: ByteArray) : InputStream() {
        private val inner = ByteArrayInputStream(data)
        @Volatile var closed = false
        override fun read(): Int = inner.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int { if (closed) throw IOException("closed"); return inner.read(b, off, minOf(len, 16 * 1024)) }
        override fun available(): Int = inner.available()
        override fun close() { closed = true }
    }

    /** The engine's own stream that breaks after [breakAt] bytes (the page was closed, the engine was killed). */
    private class Breaking(data: ByteArray, private val breakAt: Int) : InputStream() {
        private val inner = ByteArrayInputStream(data)
        private var n = 0
        override fun read(): Int = throw UnsupportedOperationException()
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (n >= breakAt) throw IOException("cancelled by the engine")
            val k = inner.read(b, off, minOf(len, breakAt - n, 16 * 1024)); if (k > 0) n += k; return k
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- the engine

    @Test(timeout = 60_000) fun aFileOnlyThePageCanMakeIsSavedFromTheEnginesOwnResponse() {
        val data = testBytes(3 * MiB, 7)
        val server = FakeServer(page).also { it.contentType = "text/html; charset=utf-8" } // what a second request to the service worker's address gets: the site's web page
        val h = H(server)
        val own = Tracked(data)
        h.handed.offer(swUrl, 200, swHeaders(data.size.toLong()), own)
        val id = h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED))
        val v = h.completed(id)
        assertEquals("termux.zip", h.sink(v).name)
        assertArrayEquals("the file is exactly what the page streamed", data, h.sink(v).bytes)
        assertTrue(h.sink(v).published)
        assertEquals("one request went to the site: the one that got the web page", 1, server.seen.size)
        assertEquals(0, h.handed.size())
        assertEquals("stored: a retry or a restart knows only the page can make this file again", FETCHER_ONE_SHOT, h.row(id, "completed row") { it?.state == DlState.COMPLETED }.fetcher)
        assertEquals(data.size.toLong(), v.total)
    }

    @Test(timeout = 60_000) fun anOrdinaryLinkIsStillAskedForAndTheEnginesStreamIsClosedUnused() {
        val data = testBytes(3 * MiB, 8)
        val server = FakeServer(data)
        val h = H(server)
        val own = Tracked(testBytes(MiB, 99))
        h.handed.offer("https://files.test/pub/data.bin", 200, mapOf("Date" to "Sat, 28 Nov 2026 08:00:00 GMT", "Content-Type" to "application/octet-stream"), own)
        val id = h.engine.enqueue(Source("https://files.test/pub/data.bin", fetcher = FETCHER_HANDED))
        val v = h.completed(id)
        assertArrayEquals("the file came from the site, with the first bytes we looked at put back", data, h.sink(v).bytes)
        assertTrue("the engine's stream was closed without being read", own.closed)
        assertEquals(0, h.handed.size())
        assertEquals("an ordinary download keeps its kind", FETCHER_HANDED, h.row(id, "completed row") { it?.state == DlState.COMPLETED }.fetcher)
    }

    @Test(timeout = 60_000) fun aSiteThatAnswersAnErrorIsWorkedAroundWithTheEnginesStream() {
        val data = testBytes(MiB, 11)
        val server = FakeServer(page).also { it.statusNext.add(403) } // a one-time link: the engine's own request used it up
        val h = H(server)
        h.handed.offer("https://files.test/once/abc", 200, mapOf("Date" to "Sat, 28 Nov 2026 08:00:00 GMT", "Content-Type" to "application/zip", "Content-Disposition" to "attachment; filename=\"once.zip\""), Tracked(data))
        val v = h.completed(h.engine.enqueue(Source("https://files.test/once/abc", fetcher = FETCHER_HANDED)))
        assertArrayEquals(data, h.sink(v).bytes)
        assertEquals("once.zip", h.sink(v).name)
    }

    @Test(timeout = 60_000) fun aWebPageLabelledAsAFileIsCaughtByItsFirstBytesAndTheEnginesStreamIsUsed() {
        val data = testBytes(MiB, 12)
        val server = FakeServer(page).also { it.contentType = "application/octet-stream" }
        val h = H(server)
        h.handed.offer(swUrl, 200, swHeaders(), Tracked(data))
        val v = h.completed(h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED)))
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun aRefusalForSafetyIsNeverWorkedAroundAndTheEnginesStreamIsClosed() {
        val server = FakeServer(page).also { it.failKind = FetchException.Kind.TLS }
        val h = H(server)
        val own = Tracked(testBytes(MiB, 13))
        h.handed.offer("https://files.test/pub/data.bin", 200, mapOf("Date" to "Sat, 28 Nov 2026 08:00:00 GMT"), own)
        val f = h.failed(h.engine.enqueue(Source("https://files.test/pub/data.bin", nameHint = "data.bin", fetcher = FETCHER_HANDED)))
        assertEquals(Reason.TLS, f.reason)
        assertTrue(own.closed)
        assertEquals(0, h.handed.size())
    }

    @Test(timeout = 60_000) fun aBlobFileNeedsNoSecondRequestAndNamesItsSite() {
        val data = testBytes(2 * MiB, 14)
        val server = FakeServer(page)
        val h = H(server)
        h.handed.offer("blob:https://drive.test/6f1c-77", 200, swHeaders(data.size.toLong(), "export.zip"), Tracked(data))
        val v = h.completed(h.engine.enqueue(Source("blob:https://drive.test/6f1c-77", nameHint = "export.zip", fetcher = FETCHER_ONE_SHOT)))
        assertArrayEquals(data, h.sink(v).bytes)
        assertEquals("a blob: address is never asked for", 0, server.seen.size)
        assertEquals("drive.test", v.host)
    }

    @Test(timeout = 60_000) fun aBlobAddressWithoutAnEngineStreamIsRefusedAndSaysWhy() {
        val h = H(FakeServer(page))
        val f = h.failed(h.engine.enqueue(Source("blob:https://drive.test/gone", nameHint = "x.zip", fetcher = FETCHER_ONE_SHOT)))
        assertEquals(Reason.PAGE_FILE, f.reason)
        assertTrue("no file was created", h.sinks.sinks.isEmpty())
        assertEquals("a page-made file is made again by the page: Retry could only fail again", listOf(Action.DELETE), f.actions)
    }

    @Test(timeout = 60_000) fun aStreamThatBreaksMidWayFailsAsAPageFileAndTheHalfFileGoesAway() {
        val data = testBytes(3 * MiB, 9)
        val h = H(FakeServer(page).also { it.contentType = "text/html" })
        h.handed.offer(swUrl, 200, swHeaders(data.size.toLong()), Breaking(data, MiB))
        val id = h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED))
        val f = h.failed(id)
        assertEquals(Reason.PAGE_FILE, f.reason)
        assertEquals(listOf(Action.DELETE), f.actions)
        h.await(id, "the partial file is gone") { h.sinks.sinks.values.all { it.discarded } }
        assertEquals("said in plain words", "The page made this file itself and its stream stopped, so it cannot be continued. Go back to the page and download it again.", Format.failure(f))
    }

    @Test(timeout = 60_000) fun aStreamThatEndsClearlyShortOfItsStatedLengthIsACutNotAFinishedFile() {
        val data = testBytes(MiB, 15)
        val h = H(FakeServer(page).also { it.contentType = "text/html" })
        h.handed.offer(swUrl, 200, swHeaders(3L * MiB), Tracked(data)) // states 3 MiB, sends 1 MiB and ends cleanly
        val f = h.failed(h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED)))
        assertEquals(Reason.PAGE_FILE, f.reason)
    }

    @Test(timeout = 60_000) fun aStatedLengthThatIsSlightlyOffOrTooSmallDoesNotSpoilAGoodFile() {
        // a site may state the stored size, not the size of what it sends: 1 % short is accepted, longer than stated is read to the end
        val short = testBytes(990_000, 16)
        val a = H(FakeServer(page).also { it.contentType = "text/html" })
        a.handed.offer(swUrl, 200, swHeaders(1_000_000), Tracked(short))
        val va = a.completed(a.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED)))
        assertArrayEquals(short, a.sink(va).bytes)
        assertEquals("the size shown is what the file really is", 990_000L, va.total)

        val long = testBytes(1_200_000, 17)
        val b = H(FakeServer(page).also { it.contentType = "text/html" })
        b.handed.offer(swUrl, 200, swHeaders(1_000_000), Tracked(long))
        val vb = b.completed(b.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED)))
        assertArrayEquals("no truncation at the stated length", long, b.sink(vb).bytes)
    }

    @Test(timeout = 60_000) fun aSpeedLimitNeverThrottlesTheEnginesOwnStream() {
        val data = testBytes(2 * MiB, 18)
        val h = H(FakeServer(page).also { it.contentType = "text/html" })
        h.engine.limiter.bytesPerSec = 1_000L // at this rate a throttled 2 MiB would take half an hour
        h.handed.offer(swUrl, 200, swHeaders(data.size.toLong()), Tracked(data))
        val v = h.completed(h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED)))
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun afterARestartAPageFileCannotBeContinuedAndSaysSoInsteadOfBlamingTheSite() {
        val store = FakeStore(); val sinks = FakeSinks()
        val uri = "mem://1/termux.zip"
        val rec = Rec(1).also {
            it.state = DlState.RUNNING; it.url = swUrl; it.name = "termux.zip"; it.fetcher = FETCHER_ONE_SHOT; it.ranges = 0; it.total = 5L * MiB; it.done = MiB.toLong(); it.destUri = uri
            it.segs.add(Seg(0, 0, -1, MiB.toLong()))
        }
        store.rows[1] = rec
        sinks.sinks[uri] = FakeSink("termux.zip", 0, uri).also { it.bytes = testBytes(MiB, 5); it.durable = it.bytes.copyOf() }
        val server = FakeServer(page).also { it.contentType = "text/html" }
        val h = H(server, sinks = sinks, store = store)
        h.engine.restore(); h.engine.resumeInterrupted()
        val f = h.failed(1)
        assertEquals(Reason.PAGE_FILE, f.reason)
        assertEquals("the site is not asked: a retry through the page's address would only get its web page", 0, server.seen.size)
        h.await(1, "the half file is removed") { sinks.sinks[uri]!!.discarded }
    }

    @Test(timeout = 60_000) fun theTabOfAPageThatIsFeedingADownloadIsReportedWhileItRuns() {
        val data = testBytes(2 * MiB, 19)
        val h = H(FakeServer(page).also { it.contentType = "text/html" })
        assertFalse(h.engine.handedRunning())
        val gate = java.util.concurrent.CountDownLatch(1)
        val slow = object : InputStream() { // the page decrypts block by block: waits for the test before it ends
            private val inner = ByteArrayInputStream(data)
            override fun read(): Int = throw UnsupportedOperationException()
            override fun read(b: ByteArray, off: Int, len: Int): Int { if (inner.available() < data.size / 2) gate.await(); return inner.read(b, off, minOf(len, 8 * 1024)) }
        }
        h.handed.offer(swUrl, 200, swHeaders(data.size.toLong()), slow)
        val id = h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED))
        h.await(id, "running from the page's stream") { it?.state == DlState.RUNNING && it.done > 0 }
        assertTrue("the page is still feeding it: its tab must stay awake", h.engine.handedRunning())
        gate.countDown()
        h.completed(id)
        assertFalse(h.engine.handedRunning())
    }

    @Test(timeout = 60_000) fun everyFailedDownloadIsReportedOnceWithItsReasonAndAFinishedOneNever() {
        val failed = java.util.concurrent.CopyOnWriteArrayList<DownloadView>()
        val h = H(FakeServer(page).also { it.contentType = "text/html" })
        h.engine.addListener(object : Engine.Listener {
            override fun onChanged(id: Long) {}
            override fun onFailed(v: DownloadView) { failed.add(v) }
        })
        fun waitFor(n: Int) { val end = System.currentTimeMillis() + 10_000; while (failed.size < n && System.currentTimeMillis() < end) Thread.sleep(5) }
        h.failed(h.engine.enqueue(Source("https://files.test/get/archive.zip")))                       // a web page where a zip was expected
        waitFor(1)
        h.handed.offer(swUrl, 200, swHeaders(3L * MiB), Breaking(testBytes(3 * MiB, 21), MiB))
        h.failed(h.engine.enqueue(Source(swUrl, nameHint = "termux.zip", fetcher = FETCHER_HANDED)))   // a page's stream that breaks
        waitFor(2)
        h.completed(h.engine.enqueue(Source("https://site.test/docs/page.html")))                       // finished: no report
        Thread.sleep(100)
        assertEquals(listOf(Reason.BAD_CONTENT, Reason.PAGE_FILE), failed.map { it.reason })
        assertEquals("the site is in the snapshot", "files.test", failed[0].host)
        assertEquals("termux.zip", failed[1].name)
        assertFalse(failed.any { it.priv })
    }

    // ---------------------------------------------------------------------------------------------------------------- the registry

    @Test fun anUnclaimedStreamIsClosedWhenItIsTooOldAndAnOlderOfferForTheSameAddressIsClosedWhenReplaced() {
        val clock = ManualClock()
        val reg = HandedStreams(clock, maxHeld = 4, maxAgeMs = 1_000)
        val a = Tracked(ByteArray(10)); val a2 = Tracked(ByteArray(10))
        reg.offer("https://a.test/x", 200, emptyMap(), a)
        reg.offer("https://a.test/x", 200, emptyMap(), a2)
        assertTrue("replaced", a.closed); assertFalse(a2.closed); assertEquals(1, reg.size())
        clock.t += 1_001
        assertFalse("expired: nobody claimed it", reg.has("https://a.test/x"))
        assertTrue(a2.closed)
        assertEquals(0, reg.size())
    }

    @Test fun theRegistryKeepsAtMostFourAndClosesTheOldest() {
        val reg = HandedStreams(ManualClock(), maxHeld = 4, maxAgeMs = 1_000_000)
        val s = (1..6).map { Tracked(ByteArray(1)) }
        s.forEachIndexed { i, t -> reg.offer("https://a.test/$i", 200, emptyMap(), t) }
        assertEquals(4, reg.size())
        assertTrue(s[0].closed && s[1].closed); assertFalse(s[2].closed)
        assertNull(reg.take("https://a.test/0"))
        val taken = reg.take("https://a.test/5")!!
        assertFalse("a claimed stream is the claimer's to read: it stays open", s[5].closed)
        assertEquals(3, reg.size()); assertEquals("https://a.test/5", taken.url)
        reg.drop("https://a.test/3")
        assertTrue(s[3].closed)
    }

    @Test fun onlyAnAnswerWithNoDateOrNoWebAddressCannotBeAskedAgain() {
        assertTrue(Handed.cannotBeAskedAgain("blob:https://drive.test/1", mapOf("Date" to "x")))
        assertTrue(Handed.cannotBeAskedAgain("data:application/zip;base64,AAAA", emptyMap()))
        assertTrue("a service worker's own response has no date", Handed.cannotBeAskedAgain(swUrl, swHeaders(10)))
        assertFalse("a web server states the date of its answer", Handed.cannotBeAskedAgain("https://files.test/a.zip", mapOf("date" to "Sat, 28 Nov 2026 08:00:00 GMT")))
    }

    @Test fun theNewReasonIsStoredAsItsCodeAndHasPlainWords() {
        assertEquals(Reason.PAGE_FILE, Reason.of(20))
        val v = DownloadView(1, DlState.FAILED, Reason.PAGE_FILE, 0, "a.zip", null, null, -1, 0, 0, -1, 0, false, null, 0, 0, emptyList())
        assertTrue(Format.failure(v).contains("download it again"))
    }
}
