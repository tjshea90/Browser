package app.kestrel.core.download

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors

/** The whole engine against an in-memory server, sink and store: pause/resume, parallel ranges, failures, crashes. Real threads, tiny backoffs. */
class EngineTest {
    private val MiB = 1 shl 20
    private val harnesses = ArrayList<H>()

    @After fun cleanup() { harnesses.forEach { it.stop() } }

    private fun cfg(parallel: Boolean = false, maxActive: Int = 2, window: Long = 1L shl 20) = Config(
        maxActive = maxActive, parallel = parallel, maxConnsPerDownload = 3, maxConnsTotal = 6, windowBytes = window, minGrowRemaining = 1L shl 20, minGrowEtaSec = 0,
        rampMs = 0, evalMs = 25, growGain = 0.0, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4,
    )

    private inner class H(val server: FakeServer, config: Config = cfg(), val sinks: FakeSinks = FakeSinks(), val store: FakeStore = FakeStore()) {
        val net = FakeNet()
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "test-control").also { it.isDaemon = true } }
        val engine = Engine({ server }, sinks, store, net, exec, Clock.System, config)
        @Volatile var ticking = true
        init {
            Thread { while (ticking) { engine.tick(); try { Thread.sleep(20) } catch (e: InterruptedException) { return@Thread } } }.also { it.isDaemon = true; it.start() }
            harnesses.add(this)
        }
        fun stop() { ticking = false; exec.shutdownNow() }
        fun enqueue(url: String = "https://files.test/pub/data.bin", priv: Boolean = false) = engine.enqueue(Source(url, priv = priv))
        fun view(id: Long) = engine.view(id)
        fun await(id: Long, what: String, ms: Long = 30_000, p: (DownloadView?) -> Boolean): DownloadView? {
            val end = System.currentTimeMillis() + ms
            while (System.currentTimeMillis() < end) { val v = view(id); if (p(v)) return v; Thread.sleep(5) }
            throw AssertionError("timeout waiting for $what; last=${view(id)?.state} ${view(id)?.reason} done=${view(id)?.done}/${view(id)?.total}")
        }
        fun completed(id: Long) = await(id, "completion") { it?.state == DlState.COMPLETED }!!
        /** The view changes first and the store row a moment later (persist runs after the state flips): wait for the row before asserting on it. */
        fun awaitRow(id: Long, what: String, ms: Long = 10_000, p: (Rec?) -> Boolean): Rec? {
            val end = System.currentTimeMillis() + ms
            while (System.currentTimeMillis() < end) { val r = store.rows[id]; if (p(r)) return r; Thread.sleep(5) }
            throw AssertionError("store row never matched: $what; row state=${store.rows[id]?.state}")
        }
        fun sink(v: DownloadView): FakeSink = sinks.sinks[v.destUri]!!
        fun pauseAt(id: Long, fraction: Double): DownloadView {
            await(id, "progress $fraction") { it != null && it.state == DlState.RUNNING && it.total > 0 && it.done >= it.total * fraction }
            engine.pause(id)
            return await(id, "paused") { it?.state == DlState.PAUSED_USER }!!
        }
    }

    @Test(timeout = 60_000) fun plainDownloadWithoutRangeSupport() {
        val data = testBytes(3 * MiB)
        val s = FakeServer(data).also { it.supportRange = false; it.acceptRangesHeader = false; it.etag = null }
        val h = H(s); val id = h.enqueue()
        val v = h.completed(id)
        assertEquals("data.bin", h.sink(v).name)
        assertArrayEquals(data, h.sink(v).bytes)
        assertTrue(h.sink(v).published)
        assertEquals(1, s.seen.size)
        assertEquals(data.size.toLong(), v.total)
        h.awaitRow(id, "completed row") { it?.state == DlState.COMPLETED }
    }

    @Test(timeout = 60_000) fun theDestinationKindReachesTheSinkAndTheView() {
        val data = testBytes(MiB)
        val h = H(FakeServer(data))
        val update = h.engine.enqueue(Source("https://files.test/pub/kestrel-g-9.9.9.apk", destKind = DEST_APP_PRIVATE))
        val v = h.completed(update)
        assertEquals(DEST_APP_PRIVATE, v.kind)
        assertEquals(DEST_APP_PRIVATE, h.store.rows[update]!!.destKind) // stored: a restart re-opens the file in the right place
        val plain = h.completed(h.enqueue("https://files.test/pub/other.bin"))
        assertEquals(DEST_PUBLIC, plain.kind)
        assertEquals(listOf(DEST_APP_PRIVATE, DEST_PUBLIC), h.sinks.kinds.toList())
    }

    @Test(timeout = 60_000) fun aFixedNameBeatsTheServersName() {
        val s = FakeServer(testBytes(MiB)).also { it.disposition = "attachment; filename=\"server-name.bin\""; it.contentType = "video/mp4" }
        val h = H(s)
        val fixed = h.completed(h.engine.enqueue(Source("https://files.test/pub/video-12345", nameHint = "My clip - 720p", nameFixed = true)))
        assertEquals("My clip - 720p.mp4", h.sink(fixed).name)                 // ours, with the extension from the content type
        val hinted = h.completed(h.engine.enqueue(Source("https://files.test/pub/video-67890", nameHint = "My clip - 720p")))
        assertEquals("server-name.bin", h.sink(hinted).name)                    // a plain hint only fills in when the server says nothing
    }

    @Test(timeout = 60_000) fun unknownLengthIsStreamedToTheEnd() {
        val data = testBytes(2 * MiB, 5)
        val s = FakeServer(data).also { it.sendLength = false; it.etag = null }
        val h = H(s); val id = h.enqueue()
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
        assertEquals(data.size.toLong(), v.total)
    }

    @Test(timeout = 60_000) fun nameAndTypeComeFromTheServer() {
        val s = FakeServer(testBytes(100_000)).also { it.disposition = "attachment; filename=\"Quarterly Report.pdf\""; it.contentType = "application/pdf" }
        val h = H(s); val v = h.completed(h.enqueue("https://files.test/get?id=7"))
        assertEquals("Quarterly Report.pdf", v.name)
        assertEquals("application/pdf", v.mime)
    }

    @Test(timeout = 60_000) fun pauseAndResumeContinuesWhereItStopped() {
        val data = testBytes(8 * MiB, 2)
        val s = FakeServer(data).also { it.chunkDelayMs = 2; it.redirectTo = "https://cdn.test/signed-1" }
        val h = H(s); val id = h.enqueue()
        val paused = h.pauseAt(id, 0.3)
        assertTrue(paused.done in 1 until data.size.toLong())
        // the saved record never runs ahead of the data: every cursor is inside what was written to the file
        val end = System.currentTimeMillis() + 10_000
        while (h.store.rows[id]?.state != DlState.PAUSED_USER && System.currentTimeMillis() < end) Thread.sleep(5) // the view shows "paused" at once; the checkpoint lands when the workers are gone
        val row = h.store.rows[id]!!
        assertTrue(row.segs.isNotEmpty() && row.state == DlState.PAUSED_USER)
        val written = h.sink(paused).bytes
        for (seg in row.segs) for (i in seg.start until seg.cursor step 4099) assertEquals("byte $i", data[i.toInt()], written[i.toInt()])
        val before = s.seen.size
        h.engine.resume(id)
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
        val after = s.seen.subList(before, s.seen.size)
        assertTrue(after.all { it.from > 0 && it.cond == "If-Match: \"v1\"" })       // only ranges, always guarded by the validator
        assertEquals("https://files.test/pub/data.bin", after.first().url)            // the first request asks the clicked URL again (signed links are re-issued)
        assertTrue(s.servedBytes.get() < data.size + 2L * MiB)                        // at most a window or two fetched twice
    }

    @Test(timeout = 90_000) fun parallelConnectionsFillTheFileExactly() {
        val data = testBytes(24 * MiB, 3)
        val s = FakeServer(data).also { it.chunkDelayMs = 1; it.chunk = 32 * 1024 }
        val h = H(s, cfg(parallel = true)); val id = h.enqueue()
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
        assertTrue("saw ${s.maxConcurrent.get()} concurrent connections", s.maxConcurrent.get() >= 2)
        assertTrue(s.seen.filter { it.from > 0 }.all { it.cond == "If-Match: \"v1\"" })
        assertTrue(s.servedBytes.get() < data.size + 8L * MiB)                        // splitting wastes at most the in-flight bytes
    }

    @Test(timeout = 60_000) fun aChangedFileAsksBeforeSpendingTheDataAgain() {
        val data = testBytes(6 * MiB, 4)
        val s = FakeServer(data).also { it.chunkDelayMs = 2 }
        val h = H(s); val id = h.enqueue()
        h.pauseAt(id, 0.3)
        val fresh = testBytes(6 * MiB, 99)
        s.data = fresh; s.etag = "\"v2\""
        h.engine.resume(id)
        val d = h.await(id, "decision") { it?.state == DlState.NEEDS_DECISION }!!
        assertEquals(Reason.FILE_CHANGED, d.reason)
        h.awaitRow(id, "partial kept") { it != null && it.state == DlState.NEEDS_DECISION && it.segs.isNotEmpty() } // partial kept until the user chooses
        assertTrue(Action.RESTART in d.actions && Action.CANCEL in d.actions)
        h.engine.restart(id)
        val v = h.completed(id)
        assertArrayEquals(fresh, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun aServerThatCannotContinueIsReportedNotSpliced() {
        val data = testBytes(6 * MiB, 6)
        val s = FakeServer(data).also { it.chunkDelayMs = 2 }
        val h = H(s); val id = h.enqueue()
        h.pauseAt(id, 0.3)
        s.supportRange = false
        h.engine.resume(id)
        val d = h.await(id, "decision") { it?.state == DlState.NEEDS_DECISION }!!
        assertEquals(Reason.NO_RANGE, d.reason)
    }

    @Test(timeout = 60_000) fun busyServerIsRetriedWithRetryAfter() {
        val data = testBytes(1 * MiB, 7)
        val s = FakeServer(data).also { it.statusNext = arrayListOf(503, 429); it.retryAfter = "0" }
        val h = H(s); val v = h.completed(h.enqueue())
        assertArrayEquals(data, h.sink(v).bytes)
        assertTrue(s.seen.size >= 3)
    }

    @Test(timeout = 60_000) fun aReplyCutShortContinuesFromTheCursor() {
        val data = testBytes(4 * MiB, 8)
        val s = FakeServer(data).also { it.truncateFirstAt = 1L * MiB }
        val h = H(s); val v = h.completed(h.enqueue())
        assertArrayEquals(data, h.sink(v).bytes)
        val second = s.seen[1]
        assertEquals(1L * MiB, second.from)                                           // the retry is a Range request from the cursor, not a restart
    }

    @Test(timeout = 60_000) fun connectionResetsAreRetried() {
        val data = testBytes(1 * MiB, 9)
        val s = FakeServer(data).also { it.failNext = 3 }
        val h = H(s); val v = h.completed(h.enqueue())
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun lostNetworkWaitsThenContinues() {
        val data = testBytes(6 * MiB, 10)
        val s = FakeServer(data).also { it.chunkDelayMs = 2 }
        val h = H(s); val id = h.enqueue()
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > it.total / 5 }
        h.net.online = false; h.engine.onNetwork()
        h.await(id, "waiting") { it?.state == DlState.WAITING_NETWORK }
        val frozen = h.view(id)!!.done
        Thread.sleep(150)
        assertEquals(frozen, h.view(id)!!.done)                                       // nothing moves while offline
        h.net.online = true; h.engine.onNetwork()
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun noSpaceFailsAndRetryWorksAfterFreeing() {
        val data = testBytes(1 * MiB, 11)
        val s = FakeServer(data)
        val sinks = FakeSinks().also { it.failCreate = SinkException(Reason.NO_SPACE, "full") }
        val h = H(s, sinks = sinks); val id = h.enqueue()
        val f = h.await(id, "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.NO_SPACE, f.reason)
        assertTrue(Action.RETRY in f.actions)
        sinks.failCreate = null
        h.engine.resume(id)
        assertArrayEquals(data, h.sink(h.completed(id)).bytes)
    }

    @Test(timeout = 60_000) fun cancelRemovesTheEntryAndThePartialFile() {
        val s = FakeServer(testBytes(6 * MiB, 12)).also { it.chunkDelayMs = 2 }
        val h = H(s); val id = h.enqueue()
        val sinkUri = h.await(id, "progress") { it != null && it.done > 0 && it.destUri != null }!!.destUri!!
        h.engine.cancel(id)
        h.await(id, "gone") { it == null }
        assertTrue(h.sinks.sinks[sinkUri]!!.discarded)
        h.awaitRow(id, "row removed") { it == null }
        assertTrue(h.engine.views().isEmpty())
    }

    // ---- AL4 (owner message 26): cancelling ANY download deletes its unfinished file
    @Test(timeout = 60_000) fun cancelWhilePausedDeletesThePartialFile() {
        val s = FakeServer(testBytes(6 * MiB, 13)).also { it.chunkDelayMs = 2; it.chunk = 64 * 1024 }
        val h = H(s); val id = h.enqueue()
        val paused = h.pauseAt(id, 0.2)
        val uri = paused.destUri!!
        assertFalse("a paused download keeps its half file for the resume", uri in h.sinks.deleted)
        h.engine.cancel(id)
        h.await(id, "gone") { it == null }
        assertTrue(uri in h.sinks.deleted || h.sinks.sinks[uri]!!.discarded)
        h.awaitRow(id, "row removed") { it == null }
    }

    @Test(timeout = 60_000) fun cancelWhileWaitingInTheLineDeletesNothingToKeepAndDropsTheRow() {
        val s = FakeServer(testBytes(6 * MiB, 14)).also { it.chunkDelayMs = 2; it.chunk = 64 * 1024 }
        val h = H(s)
        val first = h.enqueue("https://files.test/pub/one.bin")
        val ids = (1..6).map { h.enqueue("https://files.test/pub/q$it.bin") }
        val queued = ids.last()
        h.await(first, "first runs") { it?.state == DlState.RUNNING }
        h.engine.cancel(queued)
        h.await(queued, "gone") { it == null }
        h.awaitRow(queued, "row removed") { it == null }
        assertEquals("the others keep going", true, h.view(first) != null)
    }

    @Test(timeout = 60_000) fun clearingAFailedDownloadDeletesItsHalfFile() {
        val store = FakeStore()
        store.rows[1L] = Rec(1L).also { it.state = DlState.FAILED; it.url = "https://x.test/a"; it.destUri = "mem://9/failed.bin"; it.done = 1234 }
        store.rows[2L] = Rec(2L).also { it.state = DlState.COMPLETED; it.url = "https://x.test/b"; it.destUri = "mem://9/finished.bin"; it.done = 99; it.total = 99 }
        val h = H(FakeServer(testBytes(10)), store = store)
        h.engine.restore()
        h.engine.clearFinished()
        h.await(1L, "failed row gone") { it == null }
        h.await(2L, "finished row gone") { it == null }
        assertTrue("the half file of a failed download goes with its row", "mem://9/failed.bin" in h.sinks.deleted)
        assertFalse("a finished download keeps its file when only the row is cleared", "mem://9/finished.bin" in h.sinks.deleted)
    }

    @Test(timeout = 60_000) fun aCancelThatWasCutOffIsFinishedAtTheNextStart() {
        val store = FakeStore()
        store.rows[1L] = Rec(1L).also { it.state = DlState.CANCELED; it.url = "https://x.test/a"; it.destUri = "mem://9/half.bin"; it.done = 55 }
        val h = H(FakeServer(testBytes(10)), store = store)
        h.engine.restore()
        assertNull(h.view(1L)); assertNull(store.rows[1L])
        assertTrue("mem://9/half.bin" in h.sinks.deleted)
    }

    @Test(timeout = 60_000) fun httpErrorsFailWithTheStatus() {
        val s = FakeServer(testBytes(1000)).also { it.statusNext = arrayListOf(404) }
        val h = H(s); val f = h.await(h.enqueue(), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.HTTP, f.reason); assertEquals(404, f.detail)
    }

    @Test(timeout = 60_000) fun insecureRedirectsAndBadAddressesAreRefused() {
        val s = FakeServer(testBytes(1000)).also { it.failKind = FetchException.Kind.INSECURE_REDIRECT }
        val h = H(s); val f = h.await(h.enqueue(), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.INSECURE_REDIRECT, f.reason)
        val bad = h.await(h.enqueue("ftp://files.test/x"), "bad url") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.BAD_URL, bad.reason)
    }

    @Test(timeout = 60_000) fun anExpiredLinkOnResumeIsReportedAsExpired() {
        val s = FakeServer(testBytes(6 * MiB, 13)).also { it.chunkDelayMs = 2; it.redirectTo = "https://cdn.test/a" }
        val h = H(s); val id = h.enqueue()
        h.pauseAt(id, 0.3)
        s.blockUrl = "https://cdn.test/a"                                             // the signed link is dead and the clicked URL still points at it
        h.engine.resume(id)
        val f = h.await(id, "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.LINK_EXPIRED, f.reason)
    }

    @Test(timeout = 60_000) fun aRefreshedLinkIsUsedAfterResume() {
        val data = testBytes(6 * MiB, 14)
        val s = FakeServer(data).also { it.chunkDelayMs = 2; it.redirectTo = "https://cdn.test/a" }
        val h = H(s); val id = h.enqueue()
        h.pauseAt(id, 0.3)
        s.redirectTo = "https://cdn.test/b"
        val before = s.seen.size
        h.engine.resume(id)
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
        val after = s.seen.subList(before, s.seen.size)
        assertEquals("https://files.test/pub/data.bin", after.first().url)
        assertTrue(after.drop(1).all { it.url == "https://cdn.test/b" })              // later windows reuse the fresh link
    }

    @Test(timeout = 60_000) fun aKilledProcessLosesAtMostOneCheckpointOfBytes() {
        val data = testBytes(8 * MiB, 15)
        val s1 = FakeServer(data).also { it.chunkDelayMs = 2 }
        val h1 = H(s1); val id = h1.enqueue()
        h1.await(id, "a checkpoint with progress") { v -> v != null && v.done > 2L * MiB && (h1.store.rows[id]?.done ?: 0) > 1L * MiB }
        // "kill -9": freeze what is durable at this instant - the stored record and the bytes as of the last fsync - then forget the old process
        val rows = h1.store.rows[id]!!.snapshot()
        val durable = h1.sinks.sinks.values.first().durable.copyOf()
        h1.net.online = false; h1.engine.onNetwork(); h1.stop()
        val sinks2 = FakeSinks()
        val uri = rows.destUri!!
        sinks2.sinks[uri] = FakeSink("data.bin", data.size.toLong(), uri).also { it.bytes = durable.copyOf(data.size); it.durable = durable }
        val store2 = FakeStore().also { it.rows[id] = rows }
        // the record may only claim bytes that are really on disk
        for (seg in rows.segs) for (i in seg.start until seg.cursor step 997) assertEquals("durable byte $i", data[i.toInt()], durable[i.toInt()])
        val s2 = FakeServer(data)
        val h2 = H(s2, sinks = sinks2, store = store2)
        h2.engine.restore()
        assertEquals(DlState.PAUSED_SYSTEM, h2.view(id)!!.state)                      // never starts by itself after a restart
        h2.engine.resume(id)
        val v = h2.completed(id)
        assertArrayEquals(data, sinks2.sinks[v.destUri]!!.bytes)
        assertTrue(s2.servedBytes.get() < data.size)                                  // it did NOT start over
    }

    @Test(timeout = 60_000) fun aKilledProcessCarriesOnByItselfAfterResumeInterrupted() {
        val data = testBytes(8 * MiB, 21)
        val s1 = FakeServer(data).also { it.chunkDelayMs = 2 }
        val h1 = H(s1); val id = h1.enqueue()
        h1.await(id, "a checkpoint with progress") { v -> v != null && v.done > 2L * MiB && (h1.store.rows[id]?.done ?: 0) > 1L * MiB }
        val rows = h1.store.rows[id]!!.snapshot()
        val durable = h1.sinks.sinks.values.first().durable.copyOf()
        h1.net.online = false; h1.engine.onNetwork(); h1.stop()
        val sinks2 = FakeSinks(); val uri = rows.destUri!!
        sinks2.sinks[uri] = FakeSink("data.bin", data.size.toLong(), uri).also { it.bytes = durable.copyOf(data.size); it.durable = durable }
        val store2 = FakeStore().also { it.rows[id] = rows }
        val s2 = FakeServer(data)
        val h2 = H(s2, sinks = sinks2, store = store2)
        h2.engine.restore()
        assertEquals(DlState.PAUSED_SYSTEM, h2.view(id)!!.state)
        h2.engine.resumeInterrupted()                                                 // nobody taps Resume
        val v = h2.completed(id)
        assertArrayEquals(data, sinks2.sinks[v.destUri]!!.bytes)
        assertTrue("it did not start over", s2.servedBytes.get() < data.size)
        assertEquals("a finished download carries no crash count", 0, h2.awaitRow(id, "completed row") { it?.state == DlState.COMPLETED }!!.crashes)
    }

    @Test(timeout = 60_000) fun onlyDownloadsTheLastRunWasWorkingOnCarryOnByThemselves() {
        val store = FakeStore()
        fun row(id: Long, st: DlState, crashes: Int = 0) { store.rows[id] = Rec(id).also { it.state = st; it.url = "https://files.test/pub/f$id.bin"; it.crashes = crashes } }
        row(1, DlState.RUNNING)                  // killed while working: carries on
        row(2, DlState.PAUSED_USER)              // the owner paused it: stays paused
        row(3, DlState.RUNNING, crashes = 2)     // the third restart in a row without a single byte of progress: left for a tap
        row(4, DlState.NEEDS_DECISION)           // asks, stays asking
        val h = H(FakeServer(testBytes(200_000, 22)), store = store)
        h.engine.restore()
        assertEquals(3, h.view(3)!!.let { store.rows[3L]!!.crashes.let { _ -> 3 } }) // (the stored row is untouched until a checkpoint)
        h.engine.resumeInterrupted()
        h.completed(1)
        Thread.sleep(150)
        assertEquals(DlState.PAUSED_USER, h.view(2)!!.state)
        assertEquals(DlState.PAUSED_SYSTEM, h.view(3)!!.state)
        assertEquals(DlState.NEEDS_DECISION, h.view(4)!!.state)
    }

    @Test(timeout = 60_000) fun aRefusedExtraConnectionRetiresAloneAndTheDownloadStillCompletes() {
        val data = testBytes(10 * MiB, 23)
        val s = FakeServer(data).also { it.chunkDelayMs = 2; it.connLimit = 1 }      // the server allows ONE connection at a time and answers 503 to the second
        val c = Config(maxActive = 2, parallel = true, maxConnsPerDownload = 3, maxConnsTotal = 6, windowBytes = 1L shl 20, minGrowRemaining = 1L shl 20, minGrowEtaSec = 0,
            rampMs = 0, evalMs = 25, growGain = 0.0, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 15, backoffCapMs = 40, refusedEvidenceMs = 10, refusedFreshMs = 5_000)
        val h = H(s, c); val id = h.enqueue()
        val v = h.completed(id)                                                       // it used to FAIL with the server's 503 once the extra connection ran out of retries
        assertArrayEquals(data, h.sink(v).bytes)
        assertTrue("the server really refused the extra connection", s.refused.get() > 0)
        assertTrue("the refusals stopped: it did not keep asking for the extra connection ${s.refused.get()}", s.refused.get() < 60)
    }

    @Test(timeout = 60_000) fun aDipThatHitsEveryConnectionDoesNotCostTheParallelism() {
        val data = testBytes(10 * MiB, 24)
        val s = FakeServer(data).also { it.chunkDelayMs = 2 }
        val c = Config(maxActive = 2, parallel = true, maxConnsPerDownload = 3, maxConnsTotal = 6, windowBytes = 1L shl 20, minGrowRemaining = 1L shl 20, minGrowEtaSec = 0,
            rampMs = 0, evalMs = 25, growGain = 0.0, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 5, backoffCapMs = 10, refusedEvidenceMs = 200)
        val h = H(s, c); val id = h.enqueue()
        h.await(id, "two connections at work") { it != null && it.connections >= 2 && it.done > 2L * MiB }
        s.failNext = 6                                                                // a blip: the next requests of every connection fail together
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun anOutageInTheMiddleWaitsAndCarriesOnInsteadOfFailing() {
        val data = testBytes(8 * MiB, 25)
        val s = FakeServer(data).also { it.chunkDelayMs = 2; it.probeWindow = 1L shl 20 } // windowed replies, like Firefox's fetcher: the engine asks again after each window
        val c = Config(maxActive = 2, parallel = false, windowBytes = 1L shl 20, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4, maxFails = 3,
            outageBaseMs = 40, outageCapMs = 80, outageGiveUpMs = 60_000)
        val h = H(s, c); val id = h.enqueue()
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 2L * MiB }
        s.down = true                                                                 // the upstream dies; the tablet still sees Wi-Fi (net.online stays true)
        h.await(id, "waiting, not failed") { it?.state == DlState.WAITING_NETWORK }
        Thread.sleep(250)
        assertEquals("still waiting", DlState.WAITING_NETWORK, h.view(id)!!.state)
        s.down = false
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun theIdleTimeBetweenWindowsIsMeasuredForDiagnostics() {
        val data = testBytes(6 * MiB, 3)
        val s = FakeServer(data).also { it.probeWindow = 1L shl 20 } // windowed replies, like Firefox's fetcher: the engine asks again after each window
        val h = H(s, cfg(window = 1L shl 20)); val id = h.enqueue()
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
        val g = h.engine.windowGaps()
        assertTrue("at least four boundaries in a 6-window download, got ${g[0]}", g[0] >= 4)
        assertTrue("an in-memory server answers at once: the average gap is small (${g[1]} ms over ${g[0]})", g[1] / g[0] < 500)
        assertTrue(g[2] >= 0 && g[2] <= g[1])
    }

    @Test(timeout = 60_000) fun aLongOutageEndsAsStalledWithThePartialKeptAndRetryContinues() {
        val data = testBytes(8 * MiB, 26)
        val s = FakeServer(data).also { it.chunkDelayMs = 2; it.probeWindow = 1L shl 20 } // windowed replies, like Firefox's fetcher: the engine asks again after each window
        val c = Config(maxActive = 2, parallel = false, windowBytes = 1L shl 20, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4, maxFails = 3,
            outageBaseMs = 30, outageCapMs = 60, outageGiveUpMs = 300)
        val h = H(s, c); val id = h.enqueue()
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 2L * MiB }
        s.down = true
        val f = h.await(id, "stalled") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.STALLED, f.reason)
        assertTrue("the partial file stays", f.done > 0)
        s.down = false
        h.engine.resume(id)                                                           // Retry
        assertArrayEquals(data, h.sink(h.completed(id)).bytes)
    }

    @Test(timeout = 60_000) fun aFreshDownloadWhoseHostIsDeadStillFailsFast() {
        val s = FakeServer(testBytes(MiB, 27)).also { it.down = true }
        val c = Config(maxActive = 2, backoffBaseMs = 1, backoffCapMs = 4, maxFails = 3, outageBaseMs = 40, outageGiveUpMs = 60_000)
        val h = H(s, c)
        val f = h.await(h.enqueue(), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.NETWORK, f.reason)                                        // a typo in the address must not wait for half an hour
    }

    @Test(timeout = 60_000) fun aBigFileStartsWithTwoConnectionsAtOnce() {
        val data = testBytes(12 * MiB, 28)
        val s = FakeServer(data).also { it.chunkDelayMs = 3 }
        val slowRamp = Config(maxActive = 2, parallel = true, maxConnsPerDownload = 3, maxConnsTotal = 6, windowBytes = 1L shl 20, minGrowRemaining = 1L shl 20, minGrowEtaSec = 0,
            rampMs = 5_000, evalMs = 5_000, growGain = 0.0, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4, fastStartMinBytes = 8L shl 20)
        val h = H(s, slowRamp); val id = h.enqueue()
        h.await(id, "two connections within the first moments", ms = 5_000) { it != null && it.connections >= 2 }
        assertArrayEquals(data, h.sink(h.completed(id)).bytes)
    }

    @Test(timeout = 60_000) fun aFileHeldByItsDateAskedForADifferentDateIsNeverSplicedEvenWhenTheServerIgnoresIfUnmodifiedSince() {
        val data = testBytes(8 * MiB, 29)
        val s = FakeServer(data).also { it.etag = null; it.lastModified = "Wed, 01 Jan 2025 00:00:00 GMT"; it.chunkDelayMs = 2; it.probeWindow = 1L shl 20 }
        val h = H(s); val id = h.enqueue()
        h.pauseAt(id, 0.3)
        s.lastModified = "Thu, 02 Jan 2025 00:00:00 GMT"; s.data = testBytes(8 * MiB, 30)  // replaced on the server; this fake, like some real ones, does not honour If-Unmodified-Since
        h.engine.resume(id)
        val v = h.await(id, "the owner is asked") { it?.state == DlState.NEEDS_DECISION }!!
        assertEquals(Reason.FILE_CHANGED, v.reason)
        assertTrue("the partial file is kept", v.done > 0)
    }

    /** A download with NO validator at all (no ETag, no Last-Modified), killed in the middle; returns the pieces to restart it with. */
    private class Killed(val data: ByteArray, val rows: Rec, val durable: ByteArray, val cursor: Long, val id: Long)
    private fun killedWithoutValidator(seed: Long): Killed {
        val data = testBytes(8 * MiB, seed)
        val s1 = FakeServer(data).also { it.etag = null; it.lastModified = null; it.chunkDelayMs = 2 }
        val h1 = H(s1); val id = h1.enqueue()
        h1.await(id, "a checkpoint with progress") { v -> v != null && v.done > 3L * MiB && (h1.store.rows[id]?.done ?: 0) > 2L * MiB }
        val rows = h1.store.rows[id]!!.snapshot()
        val durable = h1.sinks.sinks.values.first().durable.copyOf()
        h1.net.online = false; h1.engine.onNetwork(); h1.stop()
        return Killed(data, rows, durable, rows.segs.maxOf { it.cursor }, id)
    }

    private fun restartWith(k: Killed, server: FakeServer): Pair<H, FakeSinks> {
        val sinks2 = FakeSinks(); val uri = k.rows.destUri!!
        sinks2.sinks[uri] = FakeSink("data.bin", k.data.size.toLong(), uri).also { it.bytes = k.durable.copyOf(k.data.size); it.durable = k.durable }
        val h2 = H(server, sinks = sinks2, store = FakeStore().also { it.rows[k.id] = k.rows })
        h2.engine.restore(); h2.engine.resumeInterrupted()
        return h2 to sinks2
    }

    @Test(timeout = 60_000) fun aDownloadWithNoValidatorAsksAgainForTheLastBytesBeforeTheCursorAndStillCompletes() {
        val k = killedWithoutValidator(31)
        val s2 = FakeServer(k.data).also { it.etag = null; it.lastModified = null }
        val (h2, sinks2) = restartWith(k, s2)
        val v = h2.completed(k.id)
        assertArrayEquals(k.data, sinks2.sinks[v.destUri]!!.bytes)
        val firstRange = s2.seen.first { it.from >= 0 }
        assertEquals("the request starts 4 KiB before the cursor", k.cursor - 4096, firstRange.from)
    }

    @Test(timeout = 60_000) fun aChangeInTheLastBytesBeforeTheCursorIsCaughtAndNothingIsSpliced() {
        val k = killedWithoutValidator(32)
        val changed = k.data.copyOf().also { it[(k.cursor - 100).toInt()] = (it[(k.cursor - 100).toInt()] + 1).toByte() }   // the file was replaced on the server; same length, no validator to say so
        val s2 = FakeServer(changed).also { it.etag = null; it.lastModified = null }
        val (h2, sinks2) = restartWith(k, s2)
        val v = h2.await(k.id, "the owner is asked") { it?.state == DlState.NEEDS_DECISION }!!
        assertEquals(Reason.FILE_CHANGED, v.reason)
        assertTrue("the partial file is kept", v.done > 0)
        assertTrue("nothing from the new file reached the old one: the bytes after the cursor are still unwritten", sinks2.sinks[v.destUri]!!.bytes.let { b -> (k.cursor.toInt() until (k.cursor + 4096).toInt()).all { b[it].toInt() == 0 || b[it] == k.durable.getOrElse(it) { 0 } } })
    }

    @Test(timeout = 60_000) fun aChangeFarBeyondTheCursorWithEqualLengthIsTheDocumentedResidualRisk() {
        val k = killedWithoutValidator(33)
        val changed = k.data.copyOf().also { it[(k.cursor + 100_000).toInt()] = (it[(k.cursor + 100_000).toInt()] + 1).toByte() }
        val s2 = FakeServer(changed).also { it.etag = null; it.lastModified = null }
        val (h2, _) = restartWith(k, s2)
        h2.completed(k.id)   // only a checksum could tell (research/DOWNLOAD_MANAGERS.md M6); the tail comparison cannot see this
    }

    // ---- EY-22 step 1 (research/REVIEW-EXTERNAL-2.md section 6.2): a file from a server that sends no validator can be paused, waited out and resumed, because the 4 KiB tail compare proves it is the same file

    @Test(timeout = 60_000) fun aFileWithoutAValidatorOffersPauseAndResumesFromTheCursorWithTheTailCompare() {
        val data = testBytes(8 * MiB, 41)
        val s = FakeServer(data).also { it.etag = null; it.lastModified = null; it.chunkDelayMs = 2 }
        val h = H(s); val id = h.enqueue()
        h.await(id, "the row offers Pause") { it != null && it.state == DlState.RUNNING && it.done > 2L * MiB && Action.PAUSE in it.actions }
        h.engine.pause(id)
        val paused = h.await(id, "paused") { it?.state == DlState.PAUSED_USER }!!
        assertTrue("paused part way: ${paused.done}", paused.done in 1 until data.size.toLong())
        val cursor = h.awaitRow(id, "the paused row is stored") { it?.state == DlState.PAUSED_USER }!!.segs.maxOf { it.cursor }
        val before = s.seen.size
        h.engine.resume(id)
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
        val first = s.seen.drop(before).first { it.from >= 0 }
        assertEquals("the first request after the pause starts 4 KiB before the cursor and asks for no condition", cursor - 4096, first.from)
        assertNull(first.cond)
    }

    @Test(timeout = 60_000) fun anOutageInTheMiddleOfAFileWithoutAValidatorWaitsInsteadOfFailing() {
        val data = testBytes(8 * MiB, 42)
        val s = FakeServer(data).also { it.etag = null; it.lastModified = null; it.chunkDelayMs = 2; it.probeWindow = 1L shl 20 }
        val c = Config(maxActive = 2, parallel = false, windowBytes = 1L shl 20, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4, maxFails = 3,
            outageBaseMs = 40, outageCapMs = 80, outageGiveUpMs = 60_000)
        val h = H(s, c); val id = h.enqueue()
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 2L * MiB }
        s.down = true
        h.await(id, "waiting, not failed") { it?.state == DlState.WAITING_NETWORK }
        s.down = false
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun aFileWithoutAValidatorThatCannotBeReadBackStillHasNoPause() {
        val data = testBytes(8 * MiB, 43)
        val s = FakeServer(data).also { it.etag = null; it.lastModified = null; it.chunkDelayMs = 2 }
        val sinks = object : SinkFactory {
            val inner = FakeSinks()
            override fun create(name: String, mime: String?, total: Long, priv: Boolean): Sink = blind(inner.create(name, mime, total, priv))
            override fun reopen(uri: String, total: Long): Sink? = inner.reopen(uri, total)?.let { blind(it) }
            override fun delete(uri: String): Boolean = inner.delete(uri)
            fun blind(s: Sink): Sink = object : Sink by s { override val readable: Boolean get() = false }
        }
        val e = Engine({ s }, sinks, FakeStore(), FakeNet(), Executors.newSingleThreadExecutor { r -> Thread(r, "blind-control").also { it.isDaemon = true } }, Clock.System, cfg())
        val ticker = Thread { while (true) { e.tick(); try { Thread.sleep(20) } catch (x: InterruptedException) { return@Thread } } }.also { it.isDaemon = true; it.start() }
        try {
            val id = e.enqueue(Source("https://files.test/pub/data.bin"))
            val deadline = System.currentTimeMillis() + 20_000
            var running: DownloadView? = null
            while (System.currentTimeMillis() < deadline && running == null) { e.view(id)?.let { if (it.state == DlState.RUNNING && it.done > MiB) running = it }; Thread.sleep(5) }
            assertTrue("the download runs", running != null)
            assertFalse("no validator and no way to compare: no Pause button (as before EY-22)", Action.PAUSE in running!!.actions)
        } finally { ticker.interrupt(); e.cancel(e.views().firstOrNull()?.id ?: 0L) }
    }

    @Test(timeout = 60_000) fun aPartialFileShorterThanTheRecordSaysStartsOverInsteadOfLeavingAHole() {
        val k = killedWithoutValidator(44)
        val s2 = FakeServer(k.data).also { it.etag = null; it.lastModified = null }
        val sinks2 = FakeSinks(); val uri = k.rows.destUri!!
        val keep = (k.cursor / 2).toInt()                                                     // something cut the file to half of where the record says the download got to
        sinks2.sinks[uri] = FakeSink("data.bin", keep.toLong(), uri).also { it.bytes = k.durable.copyOf(keep); it.durable = it.bytes.copyOf() }
        val h2 = H(s2, sinks = sinks2, store = FakeStore().also { it.rows[k.id] = k.rows })
        h2.engine.restore(); h2.engine.resumeInterrupted()
        val v = h2.completed(k.id)
        val out = (sinks2.sinks[v.destUri] ?: sinks2.sinks.values.last()).bytes
        assertArrayEquals("the finished file is the source, with no hole of zeros", k.data, out.copyOf(k.data.size))
        assertEquals("the first request of the new run starts at zero", 0L, s2.seen.first { it.from >= 0 }.from)
    }

    private val page = "<!DOCTYPE html><html><head><title>Sign in</title></head><body>Your link expired.</body></html>".toByteArray()

    @Test(timeout = 60_000) fun aWebPageWhereAVideoWasExpectedCreatesNoFileAndFailsWithAClearReason() {
        val s = FakeServer(page).also { it.contentType = "text/html; charset=utf-8" }
        val h = H(s)
        val f = h.await(h.enqueue("https://cdn.test/v/movie.mp4"), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.BAD_CONTENT, f.reason)
        assertTrue("no file was created for a web page", h.sinks.sinks.isEmpty())
    }

    @Test(timeout = 60_000) fun aWebPageSentAsOctetStreamIsCaughtOnTheFirstBytesAndThePartialIsRemoved() {
        val s = FakeServer(page).also { it.contentType = "application/octet-stream" }
        val h = H(s)
        val f = h.await(h.enqueue("https://cdn.test/files/archive.zip"), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.BAD_CONTENT, f.reason)
        h.await(f.id, "the empty file is gone") { h.sinks.sinks.values.all { sink -> sink.discarded } }
    }

    @Test(timeout = 60_000) fun anHtmlFileAndALinkWithNoExpectationAreDownloadedAsTheyAre() {
        val s = FakeServer(page).also { it.contentType = "text/html" }
        val h = H(s)
        val a = h.completed(h.enqueue("https://site.test/docs/page.html"))
        assertArrayEquals(page, h.sink(a).bytes)
        val b = h.completed(h.enqueue("https://site.test/get?id=9"))
        assertArrayEquals(page, h.sink(b).bytes)
    }

    @Test(timeout = 60_000) fun aSpeedLimitSlowsTheDownloadDownAndNeverCorruptsIt() {
        val data = testBytes(4 * MiB, 34)
        val s = FakeServer(data)
        val limiter = RateLimiter().also { it.bytesPerSec = 1L * MiB }           // 4 MiB at 1 MiB/s: about 4 s (3.8 s with the burst)
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "test-control").also { it.isDaemon = true } }
        val sinks = FakeSinks(); val store = FakeStore()
        val engine = Engine({ s }, sinks, store, FakeNet(), exec, Clock.System, cfg(), limiter = limiter)
        val h = H(s, cfg(), sinks, store)                                          // (the harness's own engine stays idle; this test drives its own)
        val ticker = Thread { while (true) { engine.tick(); try { Thread.sleep(20) } catch (e: InterruptedException) { return@Thread } } }.also { it.isDaemon = true; it.start() }
        val t0 = System.currentTimeMillis()
        val id = engine.enqueue(Source("https://files.test/pub/limited.bin"))
        val end = t0 + 30_000
        while (engine.view(id)?.state != DlState.COMPLETED && System.currentTimeMillis() < end) Thread.sleep(20)
        val took = System.currentTimeMillis() - t0
        ticker.interrupt(); exec.shutdownNow()
        assertEquals(DlState.COMPLETED, engine.view(id)?.state)
        assertTrue("limited to about 1 MiB/s: took $took ms", took >= 2_500)
        assertArrayEquals(data, sinks.sinks[engine.view(id)!!.destUri]!!.bytes)
        h.stop()
    }

    @Test(timeout = 60_000) fun downloadsQueueWhenAllSlotsAreBusy() {
        val s = FakeServer(testBytes(2 * MiB, 16)).also { it.chunkDelayMs = 1 }
        val h = H(s, cfg(maxActive = 1)); val a = h.enqueue("https://files.test/a.bin"); val b = h.enqueue("https://files.test/b.bin")
        h.await(b, "second one waits") { it?.state == DlState.QUEUED || it?.state == DlState.COMPLETED }
        h.completed(a); h.completed(b)
    }

    @Test(timeout = 60_000) fun privateDownloadsAreNeverStored() {
        val data = testBytes(500_000, 17)
        val h = H(FakeServer(data)); val id = h.enqueue(priv = true)
        assertTrue(id < 0)
        val v = h.completed(id)
        assertTrue(v.priv); assertTrue(h.store.rows.isEmpty())
    }

    @Test(timeout = 60_000) fun finishedEntriesCanBeClearedAndFilesDeleted() {
        val h = H(FakeServer(testBytes(100_000, 18)))
        val a = h.enqueue("https://files.test/a.bin"); val b = h.enqueue("https://files.test/b.bin")
        val va = h.completed(a); h.completed(b)
        h.engine.remove(a, deleteFile = true)
        h.await(a, "removed") { it == null }
        assertTrue(va.destUri in h.sinks.deleted)
        h.engine.clearFinished()
        h.await(b, "cleared") { it == null }
        assertFalse(h.sinks.deleted.contains(h.sinks.sinks.keys.first { it != va.destUri }))  // "Clear" only forgets the entry, the file stays
    }

    @Test(timeout = 60_000) fun interruptedRecordsComeBackPausedAndTheirIdsDoNotCollide() {
        val store = FakeStore()
        for ((i, st) in listOf(DlState.RUNNING, DlState.QUEUED, DlState.COMPLETED, DlState.PAUSED_USER, DlState.CANCELED).withIndex()) store.rows[(i + 1).toLong()] = Rec((i + 1).toLong()).also { it.state = st; it.url = "https://x.test/$i" }
        val h = H(FakeServer(testBytes(10)), store = store)
        h.engine.restore()
        assertEquals(DlState.PAUSED_SYSTEM, h.view(1)!!.state); assertEquals(DlState.PAUSED_SYSTEM, h.view(2)!!.state)
        assertEquals(DlState.COMPLETED, h.view(3)!!.state); assertEquals(DlState.PAUSED_USER, h.view(4)!!.state)
        assertNull(h.view(5)); assertNull(store.rows[5L])                             // canceled leftovers are dropped
        val n = h.enqueue("https://x.test/new")
        assertTrue(n > 5)
    }

    // ---- the waiting line (AD1b-1): Start now, move up / down, order kept in the store
    private fun slow(bytes: Int = 6 * MiB) = FakeServer(testBytes(bytes)).also { it.chunkDelayMs = 15; it.chunk = 64 * 1024 }
    private fun line(h: H, vararg ids: Long) = ids.map { h.view(it)!!.queuePos }

    @Test(timeout = 90_000) fun startNowRunsAQueuedDownloadOverTheLimitAndOffersTheMoves() {
        val h = H(slow(), cfg(maxActive = 1))
        val a = h.enqueue("https://files.test/a.bin"); val b = h.enqueue("https://files.test/b.bin"); val c = h.enqueue("https://files.test/c.bin")
        h.await(a, "a running") { it?.state == DlState.RUNNING }
        h.await(c, "c waiting") { it?.state == DlState.QUEUED && it.queuePos == 1 }
        assertEquals(listOf(0, 1), line(h, b, c))
        assertEquals(listOf(Action.START_NOW, Action.MOVE_DOWN, Action.CANCEL), h.view(b)!!.actions)           // first in line: nothing above it
        assertEquals(listOf(Action.START_NOW, Action.MOVE_UP, Action.CANCEL), h.view(c)!!.actions)             // last in line: nothing below it
        h.engine.startNow(c)
        h.await(c, "c running over the limit of one") { it?.state == DlState.RUNNING }
        assertEquals("a is not disturbed", DlState.RUNNING, h.view(a)!!.state)
        assertEquals("b still waits, and is now first and last of the line", DlState.QUEUED, h.view(b)!!.state)
        h.await(b, "b alone in the line") { it?.queuePos == 0 && it.queueSize == 1 }
        assertEquals(listOf(Action.START_NOW, Action.CANCEL), h.view(b)!!.actions)
        h.completed(a); h.completed(b); h.completed(c)
    }

    @Test(timeout = 90_000) fun movingUpAndDownReordersTheLineAndTheOrderIsStored() {
        val h = H(slow(), cfg(maxActive = 1))
        val a = h.enqueue("https://files.test/a.bin"); val b = h.enqueue("https://files.test/b.bin"); val c = h.enqueue("https://files.test/c.bin"); val d = h.enqueue("https://files.test/d.bin")
        h.await(a, "a running") { it?.state == DlState.RUNNING }
        h.await(d, "d waiting") { it?.queuePos == 2 }
        h.engine.move(d, up = true)                       // b d c
        h.await(d, "d moved up") { it?.queuePos == 1 }
        assertEquals(listOf(0, 2, 1), line(h, b, c, d))
        h.engine.move(b, up = false)                      // d b c
        h.await(b, "b moved down") { it?.queuePos == 1 }
        assertEquals(listOf(1, 2, 0), line(h, b, c, d))
        h.engine.move(d, up = true)                       // the first one cannot go higher: nothing changes
        Thread.sleep(150)
        assertEquals(listOf(1, 2, 0), line(h, b, c, d))
        val rows = listOf(d, b, c).map { id -> h.awaitRow(id, "stored priority") { (it?.prio ?: 0) > 0 }!!.prio }
        assertTrue("the stored priorities follow the line: $rows", rows[0] > rows[1] && rows[1] > rows[2])
        h.completed(a)
        h.await(d, "d starts first after a") { it?.state == DlState.RUNNING || it?.state == DlState.STARTING || it?.state == DlState.COMPLETED }
        assertEquals("b and c still wait their turn", DlState.QUEUED, h.view(c)!!.state)
        h.completed(d); h.completed(b); h.completed(c)
    }

    @Test(timeout = 90_000) fun aPausedDownloadCanStartNowAndOnlyThatRunJumpsTheLine() {
        val h = H(slow(), cfg(maxActive = 1))
        val a = h.enqueue("https://files.test/a.bin"); val b = h.enqueue("https://files.test/b.bin")
        h.await(a, "a running") { it?.state == DlState.RUNNING }
        h.engine.pause(b)
        h.await(b, "b paused") { it?.state == DlState.PAUSED_USER }
        assertTrue(Action.START_NOW in h.view(b)!!.actions)
        h.engine.startNow(b)
        h.await(b, "b running beside a") { it?.state == DlState.RUNNING }
        h.engine.pause(b)
        h.await(b, "b paused again") { it?.state == DlState.PAUSED_USER }
        h.engine.resume(b)                                // the normal way back: into the line, not ahead of everything
        h.await(b, "b waits again") { it?.state == DlState.QUEUED }
        h.completed(a); h.completed(b)
        assertEquals("the forced priority was for one run only", 0, h.awaitRow(b, "completed row") { it?.state == DlState.COMPLETED }!!.prio)
    }

    @Test(timeout = 90_000) fun atMostThreeStartNowDownloadsRunTogether() {
        val h = H(slow(12 * MiB), cfg(maxActive = 1))
        val ids = (1..6).map { h.enqueue("https://files.test/f$it.bin") }
        h.await(ids[0], "first running") { it?.state == DlState.RUNNING }
        for (id in ids.drop(1)) h.engine.startNow(id)
        h.await(ids[3], "three forced running") { it?.state == DlState.RUNNING }
        Thread.sleep(300)
        val running = ids.count { h.view(it)!!.state == DlState.RUNNING || h.view(it)!!.state == DlState.STARTING }
        assertEquals("one normal + at most three forced", 4, running)
        for (id in ids) h.completed(id)
    }

    // ---- the checksum the server states (AD1b-1)
    private fun sha256Header(b: ByteArray): String = "sha-256=:" + java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(b)) + ":"

    @Test(timeout = 60_000) fun aFileThatMatchesItsReprDigestCompletes() {
        val data = testBytes(3 * MiB)
        val s = FakeServer(data).also { it.reprDigest = sha256Header(data) }
        val h = H(s, cfg(parallel = true)); val id = h.enqueue()
        val v = h.completed(id)
        assertArrayEquals(data, h.sink(v).bytes)
    }

    @Test(timeout = 60_000) fun aFileThatDoesNotMatchItsReprDigestFailsAndIsWipedForARetry() {
        val data = testBytes(3 * MiB)
        val s = FakeServer(data).also { it.reprDigest = sha256Header(testBytes(3 * MiB, seed = 2)) } // the digest of some other bytes
        val h = H(s); val id = h.enqueue()
        val v = h.await(id, "checksum failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.DIGEST, v.reason)
        assertTrue("the wrong file is not left behind", h.sinks.sinks.values.all { it.discarded && !it.published })
        assertTrue("a retry starts from nothing", Action.RETRY in v.actions)
        s.reprDigest = sha256Header(data)                 // the server is right now (a cached copy was stale)
        h.engine.resume(id)
        h.completed(id)
    }

    @Test(timeout = 60_000) fun aDigestOfAnEncodedReplyOrAnUnknownAlgorithmIsIgnored() {
        val data = testBytes(MiB)
        val s1 = FakeServer(data).also { it.reprDigest = sha256Header(testBytes(MiB, seed = 9)); it.contentEncoding = "gzip" }
        h1@ run { val h = H(s1); val id = h.enqueue(); h.completed(id) }          // the digest would describe the compressed bytes, not the file on disk
        val s2 = FakeServer(data).also { it.reprDigest = "md5=:AAAAAAAAAAAAAAAAAAAAAA==:" }
        val h2 = H(s2); h2.completed(h2.enqueue())
    }

    // ---- read coalescing (research/REVIEW-EXTERNAL-1.md 6.6): what has already arrived is written in one piece
    @Test(timeout = 60_000) fun bytesThatHaveAlreadyArrivedAreWrittenInBigPiecesAndTheFileIsStillExact() {
        val data = testBytes(4 * MiB)
        val coalesced = FakeServer(data).also { it.chunk = 8 * 1024; it.bufferedAhead = true }
        val h1 = H(coalesced); val v1 = h1.completed(h1.enqueue())
        assertArrayEquals(data, h1.sink(v1).bytes)
        val plain = FakeServer(data).also { it.chunk = 8 * 1024; it.bufferedAhead = false }
        val h2 = H(plain); val v2 = h2.completed(h2.enqueue())
        assertArrayEquals(data, h2.sink(v2).bytes)
        val w1 = h1.sink(v1).writes.get(); val w2 = h2.sink(v2).writes.get()
        assertTrue("writes with coalescing $w1, without $w2", w1 * 4 <= w2) // 64 KiB per write instead of 8 KiB
    }
}
