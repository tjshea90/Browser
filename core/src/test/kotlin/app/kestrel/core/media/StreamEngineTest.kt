package app.kestrel.core.media

import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.FakeNet
import app.kestrel.core.download.FakeSinks
import app.kestrel.core.download.Reason
import app.kestrel.core.download.testBytes
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The segment downloader against an in-memory CDN: order, parallelism, AES-128, byte ranges, pause / resume / kill, retries and the things that must be refused. */
class StreamEngineTest {
    private val harnesses = ArrayList<H>()
    @After fun cleanup() { harnesses.forEach { it.stop() } }

    private fun cfg(conns: Int = 3, window: Long = 0) = Config(maxActive = 2, maxConnsPerDownload = conns, maxConnsTotal = 6, windowBytes = window, checkpointMs = 40, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4)

    private inner class H(val cdn: FakeCdn = FakeCdn(), val sinks: FakeSinks = FakeSinks(), val store: MemStreamStore = MemStreamStore(), config: Config = cfg(), finisher: StreamFinisher? = null) {
        val net = FakeNet()
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "test-control").also { it.isDaemon = true } }
        val engine = StreamEngine({ cdn }, sinks, store, net, exec, Clock.System, config, finisher = finisher)
        init { harnesses.add(this) }
        fun stop() { exec.shutdownNow(); store.cleanup() }
        fun view(id: Long) = engine.view(id)
        fun await(id: Long, what: String, ms: Long = 30_000, p: (DownloadView?) -> Boolean): DownloadView? {
            val end = System.currentTimeMillis() + ms
            while (System.currentTimeMillis() < end) { val v = view(id); if (p(v)) return v; Thread.sleep(5) }
            throw AssertionError("timeout waiting for $what; last=${view(id)?.state} ${view(id)?.reason} done=${view(id)?.done}/${view(id)?.total}")
        }
        fun completed(id: Long) = await(id, "completion") { it?.state == DlState.COMPLETED }!!
        fun awaitStoredState(id: Long): DlState { val end = System.currentTimeMillis() + 10_000; while (System.currentTimeMillis() < end) { store.rows[id]?.state?.let { if (it == DlState.COMPLETED) return it }; Thread.sleep(5) }; return store.rows[id]?.state ?: DlState.FAILED }
        fun out(v: DownloadView): ByteArray = sinks.sinks[v.destUri]!!.bytes
        fun enqueue(playlist: String, url: String = "https://cdn.test/v/index.m3u8", stem: String = "My clip", est: Long = -1) =
            engine.enqueue(StreamSource(url, playlist, "https://site.test/watch", stem, estimateBytes = est))
    }

    /** n segments of [size] bytes under https://cdn.test/v/sN.ts and the playlist that lists them. */
    private fun plainStream(h: H, n: Int, size: Int, extra: String = ""): Pair<String, ByteArray> {
        val all = java.io.ByteArrayOutputStream()
        val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:4\n#EXT-X-MEDIA-SEQUENCE:0\n$extra")
        for (i in 0 until n) { val b = testBytes(size, 100L + i); h.cdn.files["https://cdn.test/v/s$i.ts"] = b; all.write(b); sb.append("#EXTINF:4.0,\ns$i.ts\n") }
        sb.append("#EXT-X-ENDLIST\n")
        return sb.toString() to all.toByteArray()
    }

    @Test(timeout = 60_000) fun segmentsAreJoinedInOrderAndFetchedInParallel() {
        val h = H(); h.cdn.chunkDelayMs = 2
        val (pl, expected) = plainStream(h, 12, 40_000)
        val id = h.enqueue(pl)
        val v = h.completed(id)
        assertArrayEquals(expected, h.out(v))
        assertEquals("My clip.ts", h.sinks.sinks[v.destUri]!!.name)
        assertEquals("video/mp2t", v.mime)
        assertTrue("several segments were in flight at once", h.cdn.maxConcurrent.get() >= 2)
        assertEquals(expected.size.toLong(), v.total); assertFalse(v.estimated)
        assertTrue(h.sinks.sinks[v.destUri]!!.published)
        assertTrue("staging is cleaned up", h.store.stagedFiles(id).isEmpty())
        assertEquals(DlState.COMPLETED, h.store.rows[id]!!.state)
    }

    @Test(timeout = 60_000) fun aes128SegmentsAreDecrypted() {
        val h = H()
        val key = testBytes(16, 7)
        h.cdn.files["https://cdn.test/v/key.bin"] = key
        val plain = java.io.ByteArrayOutputStream()
        val sb = StringBuilder("#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXT-X-MEDIA-SEQUENCE:5\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n")   // no IV: derived from the sequence number
        for (i in 0 until 6) {
            val p = testBytes(30_000 + i * 17, 300L + i); plain.write(p)
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(Hls.sequenceIv(5L + i)))
            h.cdn.files["https://cdn.test/v/e$i.ts"] = c.doFinal(p); sb.append("#EXTINF:4,\ne$i.ts\n")
        }
        sb.append("#EXT-X-ENDLIST\n")
        val v = h.completed(h.enqueue(sb.toString()))
        assertArrayEquals(plain.toByteArray(), h.out(v))
        assertEquals(1, h.cdn.count("https://cdn.test/v/key.bin"))                    // the key is fetched once
    }

    @Test(timeout = 60_000) fun aWrongKeyFailsInsteadOfWritingGarbage() {
        val h = H()
        h.cdn.files["https://cdn.test/v/key.bin"] = testBytes(16, 1)
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(testBytes(16, 2), "AES"), IvParameterSpec(Hls.sequenceIv(0)))
        h.cdn.files["https://cdn.test/v/e0.ts"] = c.doFinal(testBytes(1000, 3))
        val v = h.await(h.enqueue("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXTINF:4,\ne0.ts\n#EXT-X-ENDLIST\n"), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.UNSUPPORTED, v.reason)
    }

    @Test(timeout = 60_000) fun fragmentedMp4FromOneFileUsesByteRanges() {
        val h = H()
        val file = testBytes(5_000, 9)
        h.cdn.files["https://cdn.test/v/main.mp4"] = file
        val pl = "#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXT-X-MAP:URI=\"main.mp4\",BYTERANGE=\"700@0\"\n#EXTINF:6,\n#EXT-X-BYTERANGE:1500@700\nmain.mp4\n#EXTINF:6,\n#EXT-X-BYTERANGE:1800\nmain.mp4\n#EXTINF:2,\n#EXT-X-BYTERANGE:1000\nmain.mp4\n#EXT-X-ENDLIST\n"
        val v = h.completed(h.enqueue(pl))
        assertArrayEquals(file, h.out(v))                                             // init + three ranges = the whole file
        assertEquals("My clip.mp4", h.sinks.sinks[v.destUri]!!.name); assertEquals("video/mp4", v.mime)
    }

    @Test(timeout = 60_000) fun aServerThatIgnoresRangeStillGivesTheRightBytes() {
        val h = H(); h.cdn.ignoreRange = true
        val file = testBytes(4_000, 11)
        h.cdn.files["https://cdn.test/v/main.mp4"] = file
        val pl = "#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\n#EXT-X-BYTERANGE:1000@0\nmain.mp4\n#EXTINF:6,\n#EXT-X-BYTERANGE:1500@1000\nmain.mp4\n#EXTINF:2,\n#EXT-X-BYTERANGE:1500@2500\nmain.mp4\n#EXT-X-ENDLIST\n"
        val v = h.completed(h.enqueue(pl))
        assertArrayEquals(file, h.out(v))
    }

    @Test(timeout = 60_000) fun gecko_likeBoundedWindowsAreStitched() {
        val h = H(config = cfg(window = 4096)); h.cdn.windowBytes = 4096
        val (pl, expected) = plainStream(h, 5, 10_000)                                // each segment needs 3 windows
        val v = h.completed(h.enqueue(pl))
        assertArrayEquals(expected, h.out(v))
        assertTrue(h.cdn.requests.any { it.endsWith(" 4096-8191") })
    }

    @Test(timeout = 60_000) fun pauseKeepsFinishedSegmentsAndResumeFinishesTheJob() {
        val h = H(); h.cdn.chunkDelayMs = 6; h.cdn.chunk = 2048
        val (pl, expected) = plainStream(h, 10, 60_000)
        val id = h.enqueue(pl)
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 260_000 }
        h.engine.pause(id)
        val paused = h.await(id, "paused") { it?.state == DlState.PAUSED_USER }!!
        assertTrue("paused with ${paused.done} of ${expected.size} on the tablet", paused.done in 1 until expected.size.toLong())
        val before = (0 until 10).associate { i -> i to h.cdn.count("https://cdn.test/v/s$i.ts") }
        h.engine.resume(id)
        val v = h.completed(id)
        assertArrayEquals(expected, h.out(v))
        // a segment that was finished before the pause is not fetched again: at most the ones that were cut off
        val refetched = (0 until 10).count { i -> h.cdn.count("https://cdn.test/v/s$i.ts") > maxOf(1, before[i]!!) }
        assertTrue("only cut-off segments may be fetched twice, got $refetched", refetched <= 3)
    }

    @Test(timeout = 90_000) fun aKilledAppResumesFromTheStoreAndTheStagingFolder() {
        val cdn = FakeCdn().also { it.chunkDelayMs = 4; it.chunk = 2048 }
        val sinks = FakeSinks(); val store = MemStreamStore()
        val h1 = H(cdn, sinks, store)
        val (pl, expected) = plainStream(h1, 10, 30_000)
        val id = h1.enqueue(pl)
        h1.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 100_000 }
        h1.engine.pauseAllSystem()
        h1.await(id, "stopped") { it?.state == DlState.PAUSED_SYSTEM }
        h1.exec.shutdownNow()                                                         // the process dies; only the store, the staging folder and the output survive
        val row = store.rows[id]!!
        assertTrue(row.next > 0 || store.stagedFiles(id).isNotEmpty())
        cdn.chunkDelayMs = 0
        val h2 = H(cdn, sinks, store)
        h2.engine.restore()
        assertEquals(DlState.PAUSED_SYSTEM, h2.view(id)!!.state)                      // never starts by itself
        h2.engine.resume(id)
        val v = h2.completed(id)
        assertArrayEquals(expected, h2.out(v))
    }

    @Test(timeout = 60_000) fun transientFailuresAreRetried() {
        val h = H()
        val (pl, expected) = plainStream(h, 4, 20_000)
        h.cdn.failFirst["https://cdn.test/v/s2.ts"] = 2
        h.cdn.statusFor["https://cdn.test/v/s1.ts"] = 0 // placeholder removed below
        h.cdn.statusFor.remove("https://cdn.test/v/s1.ts")
        val v = h.completed(h.enqueue(pl))
        assertArrayEquals(expected, h.out(v))
        assertEquals(3, h.cdn.count("https://cdn.test/v/s2.ts"))
    }

    @Test(timeout = 60_000) fun serverErrorsAreRetriedButMissingAndForbiddenFailAtOnce() {
        val h = H()
        val (pl, _) = plainStream(h, 3, 5_000)
        h.cdn.statusFor["https://cdn.test/v/s1.ts"] = 403
        val expired = h.await(h.enqueue(pl), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.LINK_EXPIRED, expired.reason)
        assertEquals(1, h.cdn.count("https://cdn.test/v/s1.ts"))
        val h2 = H()
        val (pl2, _) = plainStream(h2, 3, 5_000)
        h2.cdn.statusFor["https://cdn.test/v/s0.ts"] = 404
        val missing = h2.await(h2.enqueue(pl2), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.HTTP, missing.reason); assertEquals(404, missing.detail)
        val h3 = H()
        val (pl3, _) = plainStream(h3, 2, 5_000)
        h3.cdn.statusFor["https://cdn.test/v/s0.ts"] = 503
        val busy = h3.await(h3.enqueue(pl3), "failure", 60_000) { it?.state == DlState.FAILED }!!
        assertEquals(Reason.HTTP, busy.reason); assertEquals(503, busy.detail)
        assertEquals(6, h3.cdn.count("https://cdn.test/v/s0.ts"))                      // retried, then given up
    }

    @Test(timeout = 60_000) fun liveDrmAndUnreadablePlaylistsAreRefused() {
        val h = H()
        val live = h.await(h.enqueue("#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXTINF:4,\na.ts\n"), "live") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.LIVE, live.reason)
        val drm = h.await(h.enqueue("#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"skd://x\",KEYFORMAT=\"com.apple.streamingkeydelivery\"\n#EXTINF:4,\na.ts\n#EXT-X-ENDLIST\n"), "drm") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.DRM, drm.reason)
        val junk = h.await(h.enqueue("<html>forbidden</html>"), "junk") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.BAD_PLAYLIST, junk.reason)
        assertTrue(h.sinks.sinks.isEmpty())                                           // nothing was created for any of them
    }

    @Test(timeout = 60_000) fun losingTheNetworkWaitsAndTheReturnContinues() {
        val h = H(); h.cdn.chunkDelayMs = 4; h.cdn.chunk = 2048
        val (pl, expected) = plainStream(h, 8, 30_000)
        val id = h.enqueue(pl)
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 40_000 }
        h.net.online = false; h.engine.onNetwork()
        h.await(id, "waiting") { it?.state == DlState.WAITING_NETWORK }
        h.cdn.chunkDelayMs = 0
        h.net.online = true; h.engine.onNetwork()
        val v = h.completed(id)
        assertArrayEquals(expected, h.out(v))
    }

    @Test(timeout = 60_000) fun cancelRemovesTheOutputAndTheStaging() {
        val h = H(); h.cdn.chunkDelayMs = 4; h.cdn.chunk = 2048
        val (pl, _) = plainStream(h, 8, 30_000)
        val id = h.enqueue(pl)
        val uri = h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 40_000 && it.destUri != null }!!.destUri!!
        h.engine.cancel(id)
        h.await(id, "gone") { it == null }
        assertTrue(h.sinks.sinks[uri]!!.discarded || h.sinks.deleted.contains(uri))
        assertNull(h.store.rows[id])
        assertTrue(h.store.stagedFiles(id).isEmpty())
    }

    /** A finisher that "converts" by making a new output of its own and counting how often it was asked. */
    private class FakeFinisher(val sinks: FakeSinks, val result: ((StreamFinisher.Joined) -> StreamFinisher.Converted?)? = null) : StreamFinisher {
        val asked = java.util.concurrent.CopyOnWriteArrayList<StreamFinisher.Joined>()
        val merges = java.util.concurrent.CopyOnWriteArrayList<StreamFinisher.Merge>()
        @Volatile var mergeResult: ((StreamFinisher.Merge) -> StreamFinisher.Converted?)? = null
        override fun merge(m: StreamFinisher.Merge): StreamFinisher.Converted? {
            merges.add(m)
            mergeResult?.let { return it(m) }
            val sink = sinks.create(m.stem + ".mp4", "video/mp4", -1, m.priv)
            sink.pwrite(0, ByteArray(20), 0, 20)
            return StreamFinisher.Converted(sink.publish()!!, m.stem + ".mp4", "video/mp4", 20)
        }
        override fun convert(j: StreamFinisher.Joined): StreamFinisher.Converted? {
            asked.add(j)
            result?.let { return it(j) }
            val sink = sinks.create(j.stem + ".mp4", "video/mp4", -1, j.priv)
            sink.pwrite(0, ByteArray(10), 0, 10)
            return StreamFinisher.Converted(sink.publish()!!, j.stem + ".mp4", "video/mp4", 10)
        }
    }

    @Test(timeout = 60_000) fun aTsStreamIsHandedToTheFinisherAndReplacedByItsResult() {
        val sinks = FakeSinks(); val fin = FakeFinisher(sinks)
        val h = H(sinks = sinks, finisher = fin)
        val (pl, expected) = plainStream(h, 4, 20_000)
        val v = h.completed(h.enqueue(pl))
        assertEquals(1, fin.asked.size); assertEquals("ts", fin.asked[0].container); assertEquals(expected.size.toLong(), fin.asked[0].size)
        assertEquals("My clip.mp4", v.name); assertEquals("video/mp4", v.mime); assertEquals(10L, v.total)
        assertTrue("the .ts was deleted", sinks.deleted.contains(fin.asked[0].uri))
        assertEquals(DlState.COMPLETED, h.awaitStoredState(v.id))
    }

    @Test(timeout = 60_000) fun aRefusingOrFailingFinisherKeepsTheTs() {
        for (mode in listOf("null", "throws")) {
            val sinks = FakeSinks()
            val fin = FakeFinisher(sinks) { if (mode == "throws") throw IllegalStateException("muxer said no") else null }
            val h = H(sinks = sinks, finisher = fin)
            val (pl, expected) = plainStream(h, 3, 10_000)
            val v = h.completed(h.enqueue(pl))
            assertEquals(1, fin.asked.size)
            assertEquals("My clip.ts", v.name); assertEquals("video/mp2t", v.mime)
            assertArrayEquals(expected, h.out(v))
            assertTrue(sinks.deleted.isEmpty())
        }
    }

    @Test(timeout = 60_000) fun fragmentedMp4AndStreamsWithDiscontinuitiesAreNotConverted() {
        val sinks = FakeSinks(); val fin = FakeFinisher(sinks)
        val h = H(sinks = sinks, finisher = fin)
        h.cdn.files["https://cdn.test/v/main.mp4"] = testBytes(3_000, 5)
        h.completed(h.enqueue("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXT-X-MAP:URI=\"main.mp4\",BYTERANGE=\"500@0\"\n#EXTINF:6,\n#EXT-X-BYTERANGE:1000@500\nmain.mp4\n#EXT-X-ENDLIST\n"))
        val (pl, _) = plainStream(h, 3, 5_000, extra = "")
        h.completed(h.enqueue(pl.replace("#EXTINF:4.0,\ns1.ts", "#EXT-X-DISCONTINUITY\n#EXTINF:4.0,\ns1.ts")))
        assertTrue("neither was handed over", fin.asked.isEmpty())
    }

    @Test(timeout = 60_000) fun aDashRepresentationIsDownloadedFromItsManifest() {
        val h = H()
        val init = testBytes(700, 1); val segs = (1..4).map { testBytes(20_000 + it * 100, 40L + it) }
        h.cdn.files["https://cdn.test/d/vid/init.mp4"] = init
        segs.forEachIndexed { i, b -> h.cdn.files["https://cdn.test/d/vid/seg-${i + 1}.m4s"] = b }
        val aInit = testBytes(500, 2); val aSegs = (1..4).map { testBytes(5_000 + it, 80L + it) }
        h.cdn.files["https://cdn.test/d/aud/init.mp4"] = aInit
        aSegs.forEachIndexed { i, b -> h.cdn.files["https://cdn.test/d/aud/seg-${i + 1}.m4s"] = b }
        val mpd = """<MPD type="static" mediaPresentationDuration="PT8S"><Period>
            <AdaptationSet mimeType="video/mp4"><SegmentTemplate timescale="1000" duration="2000" initialization="vid/init.mp4" media="vid/seg-${'$'}Number${'$'}.m4s"/><Representation id="v1" bandwidth="800000" width="640" height="360" codecs="avc1.42c01e"/></AdaptationSet>
            <AdaptationSet mimeType="audio/mp4"><SegmentTemplate timescale="1000" duration="2000" initialization="aud/init.mp4" media="aud/seg-${'$'}Number${'$'}.m4s"/><Representation id="a1" bandwidth="96000" codecs="mp4a.40.2"/></AdaptationSet>
            </Period></MPD>"""
        h.cdn.files["https://cdn.test/d/manifest.mpd"] = mpd.toByteArray()
        val v = h.completed(h.engine.enqueue(StreamSource("https://cdn.test/d/manifest.mpd", null, null, "Clip - 360p", repId = "v1")))
        assertArrayEquals(init + segs.reduce { a, b -> a + b }, h.out(v))
        assertEquals("Clip - 360p.mp4", h.sinks.sinks[v.destUri]!!.name)
        val a = h.completed(h.engine.enqueue(StreamSource("https://cdn.test/d/manifest.mpd", null, null, "Clip - sound", repId = "a1", ext = "m4a")))
        assertArrayEquals(aInit + aSegs.reduce { x, y -> x + y }, h.out(a))
        assertEquals("Clip - sound.m4a", h.sinks.sinks[a.destUri]!!.name); assertEquals("audio/mp4", a.mime)
    }

    /** A DASH-like pair: a picture representation and a sound representation of one video, in one manifest. */
    private fun dashPair(h: H): Triple<String, ByteArray, ByteArray> {
        val vSegs = (1..4).map { testBytes(30_000, 500L + it) }; val aSegs = (1..4).map { testBytes(6_000, 600L + it) }
        val vi = testBytes(600, 7); val ai = testBytes(400, 8)
        h.cdn.files["https://cdn.test/p/v/init.mp4"] = vi; h.cdn.files["https://cdn.test/p/a/init.mp4"] = ai
        vSegs.forEachIndexed { i, b -> h.cdn.files["https://cdn.test/p/v/s${i + 1}.m4s"] = b }
        aSegs.forEachIndexed { i, b -> h.cdn.files["https://cdn.test/p/a/s${i + 1}.m4s"] = b }
        val mpd = "<MPD type=\"static\" mediaPresentationDuration=\"PT8S\"><Period>" +
            "<AdaptationSet mimeType=\"video/mp4\"><SegmentTemplate timescale=\"1000\" duration=\"2000\" initialization=\"v/init.mp4\" media=\"v/s${'$'}Number${'$'}.m4s\"/><Representation id=\"v\" bandwidth=\"800000\" width=\"640\" height=\"360\" codecs=\"avc1.42c01e\"/></AdaptationSet>" +
            "<AdaptationSet mimeType=\"audio/mp4\"><SegmentTemplate timescale=\"1000\" duration=\"2000\" initialization=\"a/init.mp4\" media=\"a/s${'$'}Number${'$'}.m4s\"/><Representation id=\"a\" bandwidth=\"96000\" codecs=\"mp4a.40.2\"/></AdaptationSet></Period></MPD>"
        h.cdn.files["https://cdn.test/p/m.mpd"] = mpd.toByteArray()
        return Triple("https://cdn.test/p/m.mpd", vi + vSegs.reduce { a, b -> a + b }, ai + aSegs.reduce { a, b -> a + b })
    }

    private fun pairSource(url: String) = StreamSource(url, null, null, "Clip - 360p", repId = "v", companion = StreamSource(url, null, null, "Clip - sound", repId = "a", ext = "m4a"))

    @Test(timeout = 60_000) fun pictureAndSoundAreDownloadedTogetherAndMergedIntoOneFile() {
        val sinks = FakeSinks(); val fin = FakeFinisher(sinks)
        val h = H(sinks = sinks, finisher = fin); h.cdn.chunkDelayMs = 2
        val (url, video, sound) = dashPair(h)
        val id = h.engine.enqueue(pairSource(url))
        assertEquals("one row for the pair", 1, h.engine.views().size)
        val v = h.completed(id)
        assertEquals(1, fin.merges.size)
        val m = fin.merges[0]
        assertArrayEquals(video, sinks.sinks[m.videoUri]!!.bytes); assertArrayEquals(sound, sinks.sinks[m.audioUri]!!.bytes)   // the merger got both finished files
        assertEquals("Clip - 360p.mp4", v.name); assertEquals(20L, v.total)
        assertTrue(sinks.deleted.containsAll(listOf(m.videoUri, m.audioUri)))                                                  // the two parts were removed
        assertEquals("only the merged file is a download", 1, h.engine.views().size)
        assertEquals(1, h.store.rows.values.count { it.parentId == 0L && it.id == id }); assertTrue(h.store.rows.size == 1)  // the hidden sound record is gone too
    }

    @Test(timeout = 60_000) fun aMergeThatIsRefusedKeepsBothFiles() {
        val sinks = FakeSinks(); val fin = FakeFinisher(sinks).also { it.mergeResult = { null } }
        val h = H(sinks = sinks, finisher = fin)
        val (url, video, sound) = dashPair(h)
        val v = h.completed(h.engine.enqueue(pairSource(url)))
        assertArrayEquals(video, h.out(v)); assertEquals("Clip - 360p.mp4", v.name)
        val all = h.engine.views()
        assertEquals("the sound is now a download of its own", 2, all.size)
        val a = all.first { it.id != v.id }
        assertEquals("Clip - sound.m4a", a.name); assertArrayEquals(sound, h.out(a)); assertEquals(DlState.COMPLETED, a.state)
    }

    @Test(timeout = 60_000) fun aFailingSoundFailsThePictureToo() {
        val h = H(); val (url, _, _) = dashPair(h)
        h.cdn.statusFor["https://cdn.test/p/a/s2.m4s"] = 404
        val v = h.await(h.engine.enqueue(pairSource(url)), "failure") { it?.state == DlState.FAILED }!!
        assertEquals(Reason.HTTP, v.reason); assertEquals(404, v.detail)
        assertEquals(1, h.engine.views().size)
    }

    @Test(timeout = 90_000) fun pauseAndResumeCoverTheWholePair() {
        val sinks = FakeSinks(); val fin = FakeFinisher(sinks)
        val h = H(sinks = sinks, finisher = fin); h.cdn.chunkDelayMs = 6; h.cdn.chunk = 2048
        val (url, _, _) = dashPair(h)
        val id = h.engine.enqueue(pairSource(url))
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 20_000 }
        h.engine.pause(id)
        h.await(id, "paused") { it?.state == DlState.PAUSED_USER }
        Thread.sleep(300)
        val n = h.cdn.requests.size; Thread.sleep(300)
        assertEquals("both halves stopped", n, h.cdn.requests.size)
        h.cdn.chunkDelayMs = 0
        h.engine.resume(id)
        h.completed(id)
        assertEquals(1, fin.merges.size)
    }

    @Test(timeout = 60_000) fun cancelRemovesBothHalves() {
        val h = H(); h.cdn.chunkDelayMs = 6; h.cdn.chunk = 2048
        val (url, _, _) = dashPair(h)
        val id = h.engine.enqueue(pairSource(url))
        h.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 20_000 }
        h.engine.cancel(id)
        h.await(id, "gone") { it == null }
        val end = System.currentTimeMillis() + 5_000
        while (h.store.rows.isNotEmpty() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("no hidden record left", h.store.rows.isEmpty()); assertTrue(h.engine.views().isEmpty())
    }

    @Test(timeout = 60_000) fun theEstimateTracksTheRealSizeWhileItRuns() {
        val h = H(); h.cdn.chunkDelayMs = 5; h.cdn.chunk = 4096
        val (pl, expected) = plainStream(h, 16, 40_000)
        val id = h.enqueue(pl)                                                          // no size guess: it comes from the segments fetched so far
        val mid = h.await(id, "past half way") { it != null && it.state == DlState.RUNNING && it.done > expected.size * 6 / 10 }!!
        assertTrue(mid.estimated)
        val ratio = mid.total.toDouble() / expected.size
        assertTrue("the estimate was $ratio of the real size", ratio in 0.85..1.2)     // a counter that kept counting appended segments twice made it half
        h.completed(id)
    }

    @Test(timeout = 60_000) fun theSizeIsAnEstimateUntilItIsKnown() {
        val h = H(); h.cdn.chunkDelayMs = 3; h.cdn.chunk = 4096
        val (pl, expected) = plainStream(h, 10, 40_000)
        val id = h.enqueue(pl, est = 500_000)
        val early = h.await(id, "an estimate") { it != null && it.total > 0 }!!
        assertTrue(early.estimated)
        val v = h.completed(id)
        assertFalse(v.estimated); assertEquals(expected.size.toLong(), v.total)
    }
}
