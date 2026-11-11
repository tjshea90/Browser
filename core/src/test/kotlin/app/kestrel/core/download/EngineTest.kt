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
        assertEquals(DlState.COMPLETED, h.store.rows[id]!!.state)
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
        assertTrue(h.store.rows[id]!!.segs.isNotEmpty())                              // partial kept until the user chooses
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
        assertNull(h.store.rows[id])
        assertTrue(h.engine.views().isEmpty())
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
}
