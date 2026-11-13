package app.kestrel.core.media

import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.Action
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.FakeNet
import app.kestrel.core.download.FakeSinks
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The segment downloader writing an MP4 while the segments arrive (research/MP4_MUX.md): real ffmpeg-made transport-stream segments through the whole engine. */
class StreamEngineMp4Test {
    private val harnesses = ArrayList<H>()
    @After fun cleanup() { harnesses.forEach { it.stop() } }

    private fun cfg(directMp4: Boolean = true, conns: Int = 3) = Config(maxActive = 2, maxConnsPerDownload = conns, maxConnsTotal = 6, windowBytes = 0, checkpointMs = 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4, directMp4 = directMp4)

    private inner class H(val cdn: FakeCdn = FakeCdn(), val sinks: FakeSinks = FakeSinks(), val store: MemStreamStore = MemStreamStore(), config: Config = cfg()) {
        val net = FakeNet()
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "test-control").also { it.isDaemon = true } }
        val engine = StreamEngine({ cdn }, sinks, store, net, exec, Clock.System, config)
        init { harnesses.add(this) }
        fun stop() { exec.shutdownNow(); store.cleanup() }
        fun view(id: Long) = engine.view(id)
        fun await(id: Long, what: String, ms: Long = 30_000, p: (DownloadView?) -> Boolean): DownloadView? {
            val end = System.currentTimeMillis() + ms
            while (System.currentTimeMillis() < end) { val v = view(id); if (p(v)) return v; Thread.sleep(5) }
            throw AssertionError("timeout waiting for $what; last=${view(id)?.state} ${view(id)?.reason} done=${view(id)?.done}/${view(id)?.total}")
        }
        fun completed(id: Long) = await(id, "completion") { it?.state == DlState.COMPLETED }!!
        fun eventually(what: String, cond: () -> Boolean) { val end = System.currentTimeMillis() + 10_000; while (System.currentTimeMillis() < end) { if (cond()) return; Thread.sleep(5) }; throw AssertionError("timeout waiting for $what") }
        fun out(v: DownloadView): ByteArray = sinks.sinks[v.destUri]!!.bytes
        fun enqueue(playlist: String, stem: String = "My clip") = engine.enqueue(StreamSource("https://cdn.test/v/index.m3u8", playlist, "https://site.test/watch", stem))
    }

    /** Registers the fixture segments on the fake CDN (under s0.ts, s1.ts, ...) and returns the playlist; [disc] are the indexes that begin a new run, [key] encrypts them with AES-128. */
    private fun tsStream(h: H, names: List<String>, disc: Set<Int> = emptySet(), key: ByteArray? = null, patch: (Int, ByteArray) -> ByteArray = { _, b -> b }): String {
        val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:2\n#EXT-X-MEDIA-SEQUENCE:0\n")
        if (key != null) { h.cdn.files["https://cdn.test/v/key.bin"] = key; sb.append("#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n") }
        for ((i, n) in names.withIndex()) {
            var data = patch(i, Fixtures.bytes(n))
            if (key != null) { val c = Cipher.getInstance("AES/CBC/PKCS5Padding"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(Hls.sequenceIv(i.toLong()))); data = c.doFinal(data) }
            h.cdn.files["https://cdn.test/v/s$i.ts"] = data
            if (i in disc) sb.append("#EXT-X-DISCONTINUITY\n")
            sb.append("#EXTINF:2.000,\ns$i.ts\n")
        }
        sb.append("#EXT-X-ENDLIST\n")
        return sb.toString()
    }

    private val plain = listOf("a_0.ts", "a_1.ts", "a_2.ts")

    private fun summary(file: ByteArray): List<Any> {
        val r = Mp4Reader(file)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        return listOf(v.samples.map { it.size }, v.samples.map { it.sync }, a.samples.map { it.size }, v.width, v.height, a.rate)
    }

    @Test(timeout = 60_000) fun aTransportStreamIsWrittenStraightIntoAnMp4() {
        val h = H(); h.cdn.chunkDelayMs = 1
        val id = h.enqueue(tsStream(h, plain))
        // the row says .mp4 from the first moment: no .ts is ever written
        val early = h.await(id, "the row has its name") { it != null && it.name.isNotEmpty() && it.state != DlState.QUEUED && it.state != DlState.STARTING }!!
        assertEquals("My clip.mp4", early.name)
        val v = h.completed(id)
        assertEquals("My clip.mp4", v.name); assertEquals("video/mp4", v.mime)
        assertEquals(1, h.sinks.sinks.size); assertTrue(h.sinks.sinks[v.destUri]!!.published)
        assertNull(v.note)
        val file = h.out(v)
        assertEquals(file.size.toLong(), v.total)
        val r = Mp4Reader(file)
        assertEquals(listOf("ftyp", "wide", "mdat", "moov"), r.topLevel)
        assertEquals(150, r.track("vide")!!.samples.size); assertEquals(260, r.track("soun")!!.samples.size)
        if (Fixtures.ffmpeg) assertEquals("", Fixtures.ffmpegDecode(file))
        assertTrue("the staged segments and the journal are gone", h.store.stagedFiles(id).isEmpty())
    }

    @Test(timeout = 60_000) fun anEncryptedTransportStreamGivesTheSameMp4() {
        val h1 = H(); val v1 = h1.completed(h1.enqueue(tsStream(h1, plain)))
        val h2 = H(); val v2 = h2.completed(h2.enqueue(tsStream(h2, plain, key = ByteArray(16) { (it * 7 + 3).toByte() })))
        assertEquals(summary(h1.out(v1)), summary(h2.out(v2)))
        assertEquals("My clip.mp4", v2.name)
    }

    @Test(timeout = 60_000) fun anAdvertisementRunIsLeftOutAndTheRowSaysSo() {
        val h = H()
        val v = h.completed(h.enqueue(tsStream(h, listOf("a_0.ts", "a_1.ts", "ad_0.ts", "a_0.ts", "a_1.ts"), disc = setOf(2, 3))))
        assertNotNull(v.note); assertTrue(v.note!!, v.note!!.startsWith("1 part"))
        val r = Mp4Reader(h.out(v))
        assertEquals(200, r.track("vide")!!.samples.size); assertEquals(128, r.track("vide")!!.width)
    }

    @Test(timeout = 60_000) fun theRestOfALeftOutAdvertisementIsNotFetched() {
        // one connection: the first segment of the advertisement is judged (another format = left out) before the next one is asked for
        val h = H(config = cfg(conns = 1))
        val names = listOf("a_0.ts", "a_1.ts", "ad_0.ts", "ad_0.ts", "ad_0.ts", "ad_0.ts", "a_0.ts", "a_1.ts")
        val v = h.completed(h.enqueue(tsStream(h, names, disc = setOf(2, 6))))
        assertNotNull(v.note); assertTrue(v.note!!, v.note!!.startsWith("1 part"))
        val r = Mp4Reader(h.out(v))
        assertEquals("the film is whole: two runs of two segments", 200, r.track("vide")!!.samples.size)
        val asked = h.cdn.seen.map { it.url.substringAfterLast('/') }.toSet()
        assertTrue("segment 2 (the first of the advertisement) was fetched to judge it: $asked", "s2.ts" in asked)
        assertTrue("the rest of the advertisement (3, 4, 5) was never asked for: $asked", listOf("s3.ts", "s4.ts", "s5.ts").none { it in asked })
        assertTrue("the film around it was fetched: $asked", listOf("s0.ts", "s1.ts", "s6.ts", "s7.ts").all { it in asked })
    }

    @Test(timeout = 60_000) fun streamsThatAreNotH264WithAacStayTsFilesAndSayWhy() {
        val h = H()
        val junk = ByteArray(40_000) { (it * 31 + 5).toByte() }
        for (i in 0 until 3) h.cdn.files["https://cdn.test/v/s$i.ts"] = junk
        val pl = "#EXTM3U\n#EXT-X-TARGETDURATION:2\n#EXTINF:2,\ns0.ts\n#EXTINF:2,\ns1.ts\n#EXTINF:2,\ns2.ts\n#EXT-X-ENDLIST\n"
        val v = h.completed(h.enqueue(pl))
        assertEquals("My clip.ts", v.name)
        assertNotNull(v.note); assertTrue(v.note!!, v.note!!.startsWith("Kept as a .ts file because"))
        assertEquals(3 * junk.size, h.out(v).size)
    }

    @Test(timeout = 60_000) fun theSwitchOffKeepsTheOldWay() {
        val h = H(config = cfg(directMp4 = false))
        val v = h.completed(h.enqueue(tsStream(h, plain)))
        assertEquals("My clip.ts", v.name)
        assertEquals(plain.sumOf { Fixtures.bytes(it).size }, h.out(v).size)
    }

    @Test(timeout = 60_000) fun anMp4WriterThatCannotFindAPictureRestartsTheDownloadTheOldWay() {
        // the first segment passes the look (H.264, parameters present) but no picture in the stream is a keyframe: the muxer has nothing to write at the end
        fun noKeyframes(data: ByteArray): ByteArray {
            val b = data.copyOf()
            var i = 0
            while (i + 4 < b.size) { if (b[i].toInt() == 0 && b[i + 1].toInt() == 0 && b[i + 2].toInt() == 1 && (b[i + 3].toInt() and 0x1F) == 5) b[i + 3] = ((b[i + 3].toInt() and 0xE0) or 1).toByte(); i++ }
            return b
        }
        val h = H()
        val pl = tsStream(h, plain, patch = { _, d -> noKeyframes(d) })
        val v = h.completed(h.enqueue(pl))
        assertEquals("My clip.ts", v.name)
        assertEquals("Kept as a .ts file because the MP4 writer could not handle this stream.", v.note)
        assertEquals(plain.sumOf { Fixtures.bytes(it).size }, h.out(v).size)
        assertTrue("every segment was fetched a second time for the restart", h.cdn.count("https://cdn.test/v/s0.ts") >= 2)
    }

    @Test(timeout = 90_000) fun pausedKilledAndResumedGivesTheSameFile() {
        val whole = H().let { h -> h.out(h.completed(h.enqueue(tsStream(h, plain + plain, disc = setOf(3))))) }
        val cdn = FakeCdn().also { it.chunkDelayMs = 3; it.chunk = 2048 }
        val sinks = FakeSinks(); val store = MemStreamStore()
        val h1 = H(cdn, sinks, store)
        val pl = tsStream(h1, plain + plain, disc = setOf(3))
        val id = h1.enqueue(pl)
        h1.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 100_000 }
        h1.engine.pauseAllSystem()
        h1.await(id, "stopped") { it?.state == DlState.PAUSED_SYSTEM }
        h1.exec.shutdownNow()                                                          // the process dies; the store, the staging folder (with the journal) and the output survive
        assertTrue("the journal is kept next to the staged segments", File(store.dir(id), "mux.jnl").isFile)
        cdn.chunkDelayMs = 0
        val h2 = H(cdn, sinks, store)
        h2.engine.restore(); h2.engine.resumeInterrupted()
        val v = h2.completed(id)
        assertEquals(summary(whole), summary(h2.out(v)))
        assertEquals(1, sinks.sinks.size)
        if (Fixtures.ffmpeg) assertEquals("", Fixtures.ffmpegDecode(h2.out(v)))
    }

    @Test(timeout = 90_000) fun aJournalOneCheckpointBehindIsTrustedOverTheSavedRecord() {
        val whole = H().let { h -> h.out(h.completed(h.enqueue(tsStream(h, plain)))) }
        val cdn = FakeCdn().also { it.chunkDelayMs = 4; it.chunk = 2048 }
        val sinks = FakeSinks(); val store = MemStreamStore()
        val h1 = H(cdn, sinks, store)
        val id = h1.enqueue(tsStream(h1, plain))
        h1.await(id, "progress") { it != null && it.state == DlState.RUNNING && it.done > 50_000 }
        h1.engine.pauseAllSystem()
        h1.await(id, "stopped") { it?.state == DlState.PAUSED_SYSTEM }
        h1.exec.shutdownNow()
        // the process was killed between "segment appended, record saved" and "journal checkpoint": drop the journal's last block
        val jf = File(store.dir(id), "mux.jnl")
        val blocks = FileMuxJournal(jf).recover()
        assertTrue("at least two checkpoints exist", blocks.size >= 1)
        jf.delete()
        val j2 = FileMuxJournal(jf); for (b in blocks.dropLast(1)) j2.append(b)
        cdn.chunkDelayMs = 0
        val h2 = H(cdn, sinks, store)
        h2.engine.restore(); h2.engine.resumeInterrupted()
        val v = h2.completed(id)
        assertEquals("segments the journal had not seen are fetched and written again", summary(whole), summary(h2.out(v)))
        assertFalse(h2.out(v).isEmpty())
    }

    // ---------------------------------------------------------------------------------------------------------------- "Make MP4" on a finished .ts

    @Test(timeout = 60_000) fun aFinishedTsCanBeMadeAnMp4AndTheTsGoes() {
        val h = H(config = cfg(directMp4 = false))                                        // the old way: a joined .ts, as the alpha14-16 downloads on the tablet
        val v = h.completed(h.enqueue(tsStream(h, plain)))
        assertEquals("My clip.ts", v.name); assertTrue(v.actions.contains(Action.MAKE_MP4))
        h.engine.makeMp4(v.id)
        val v2 = h.await(v.id, "the mp4") { it != null && it.state == DlState.COMPLETED && it.name.endsWith(".mp4") }!!
        assertEquals("My clip.mp4", v2.name); assertEquals("video/mp4", v2.mime)
        assertFalse(v2.actions.contains(Action.MAKE_MP4)); assertNull(v2.note)
        assertTrue("the .ts is deleted", h.sinks.deleted.contains(v.destUri))
        val file = h.out(v2)
        assertEquals(file.size.toLong(), v2.total)
        val r = Mp4Reader(file)
        assertEquals(150, r.track("vide")!!.samples.size); assertEquals(260, r.track("soun")!!.samples.size)
        if (Fixtures.ffmpeg) assertEquals("", Fixtures.ffmpegDecode(file))
        h.eventually("the record is stored as the mp4") { h.store.rows[v.id]?.name == "My clip.mp4" && h.store.rows[v.id]?.mux == MUX_MP4 }
    }

    @Test(timeout = 60_000) fun makeMp4UsesTheRecordedRunsAndLeavesAnAdvertisementOut() {
        val h = H(config = cfg(directMp4 = false))
        val v = h.completed(h.enqueue(tsStream(h, listOf("a_0.ts", "a_1.ts", "ad_0.ts", "a_0.ts", "a_1.ts"), disc = setOf(2, 3))))
        assertEquals("My clip.ts", v.name)
        h.engine.makeMp4(v.id)
        val v2 = h.await(v.id, "the mp4") { it != null && it.state == DlState.COMPLETED && it.name.endsWith(".mp4") }!!
        assertNotNull(v2.note); assertTrue(v2.note!!, v2.note!!.startsWith("1 part"))
        assertFalse("no seconds are claimed when only byte weights are known: ${v2.note}", v2.note!!.contains(" s)"))
        val r = Mp4Reader(h.out(v2))
        assertEquals(200, r.track("vide")!!.samples.size); assertEquals(128, r.track("vide")!!.width)
    }

    @Test(timeout = 60_000) fun makeMp4OfSomethingItCannotWrapSaysWhyAndKeepsTheTs() {
        val h = H()
        val junk = ByteArray(30_000) { (it * 13 + 7).toByte() }
        for (i in 0 until 2) h.cdn.files["https://cdn.test/v/s$i.ts"] = junk
        val v = h.completed(h.enqueue("#EXTM3U\n#EXT-X-TARGETDURATION:2\n#EXTINF:2,\ns0.ts\n#EXTINF:2,\ns1.ts\n#EXT-X-ENDLIST\n"))
        assertTrue(v.actions.contains(Action.MAKE_MP4))
        h.engine.makeMp4(v.id)
        val v2 = h.await(v.id, "the note") { it != null && it.state == DlState.COMPLETED && it.note?.startsWith("Could not make an MP4") == true }!!
        assertEquals("My clip.ts", v2.name); assertTrue(v2.actions.contains(Action.MAKE_MP4))
        assertFalse("nothing was deleted", h.sinks.deleted.contains(v.destUri))
    }

    @Test(timeout = 60_000) fun anMp4RowDoesNotOfferMakeMp4() {
        val h = H()
        val v = h.completed(h.enqueue(tsStream(h, plain)))
        assertEquals("My clip.mp4", v.name); assertFalse(v.actions.contains(Action.MAKE_MP4))
    }
}
