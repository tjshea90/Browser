package app.kestrel.core.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4MuxTest {
    private val sps = byteArrayOf(0x67, 100, 0, 31, 1, 2, 3)
    private val avcC = Avc.avcC(sps, AvcSps(100, 0, 31, 64, 36, 1, 8, 8), listOf(byteArrayOf(0x68, 9, 9)))
    private val asc = byteArrayOf(0x12, 0x10)                                  // AAC-LC, 44.1 kHz, stereo
    private val fixedNow = { 0L }

    private fun fill(n: Int, v: Int) = ByteArray(n) { v.toByte() }

    private fun newMux(out: MuxOut = MemOut()): Mp4Mux = Mp4Mux(out, fixedNow).also { it.start() }

    @Test fun writesAReadableFileWithBothTracks() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 2)
        // decode order I P B B: composition offsets 2,2,0,0 frames; one keyframe
        val frame = 3600L
        m.addVideo(fill(100, 1), 0, 100, 0, (2 * frame).toInt(), true)
        m.addVideo(fill(50, 2), 0, 50, frame, (2 * frame).toInt(), false)
        m.addAudio(fill(30, 7), 0, 30, intArrayOf(10, 20), 2, 0)
        m.addVideo(fill(40, 3), 0, 40, 2 * frame, 0, false)
        m.addVideo(fill(30, 4), 0, 30, 3 * frame, 0, false)
        m.addAudio(fill(30, 8), 0, 30, intArrayOf(15, 15), 2, 2048)
        val total = m.finish()
        assertEquals(out.size.toLong(), total)
        val r = Mp4Reader(out.bytes())
        assertEquals(listOf("ftyp", "wide", "mdat", "moov"), r.topLevel)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(4, v.samples.size); assertEquals(90000L, v.timescale)
        assertEquals(listOf(100, 50, 40, 30), v.samples.map { it.size })
        assertEquals(listOf(0L, 3600L, 7200L, 10800L), v.samples.map { it.dts })
        assertEquals(listOf(7200L, 10800L, 7200L, 10800L), v.samples.map { it.cts })
        assertEquals(listOf(true, false, false, false), v.samples.map { it.sync })
        assertTrue(v.hasStss); assertFalse(a.hasStss)
        assertEquals(4 * 3600L, v.mediaDuration)
        assertEquals(64, v.width); assertEquals(36, v.height); assertEquals("avc1", v.entryType)
        assertArrayEquals(avcC, v.codecConfig)
        assertEquals(4, a.samples.size); assertEquals(44100L, a.timescale); assertEquals("mp4a", a.entryType); assertEquals(2, a.channels); assertEquals(44100L, a.rate)
        assertEquals(listOf(10, 20, 15, 15), a.samples.map { it.size })
        assertEquals(listOf(0L, 1024L, 2048L, 3072L), a.samples.map { it.dts })
        assertEquals(4 * 1024L, a.mediaDuration)
        // the bytes of every sample are where the tables say
        for ((track, fills) in listOf(v to listOf(1, 2, 3, 4), a to listOf(7, 7, 8, 8))) for ((i, s) in track.samples.withIndex()) assertTrue("sample $i of ${track.type}", r.sampleBytes(s).all { it.toInt() == fills[i] })
        // samples lie inside the mdat, in the order written, without overlap
        val all = (v.samples + a.samples).sortedBy { it.offset }
        assertTrue(all.first().offset >= r.mdatStart); assertTrue(all.last().offset + all.last().size <= r.mdatEnd)
        for (k in 1 until all.size) assertTrue(all[k].offset >= all[k - 1].offset + all[k - 1].size)
        // sound starts at 0 and the first picture is shown at 7200: the earliest sample is the sound, the picture starts at its decode time 0 -> nothing to shift
        assertTrue(v.edits.isEmpty()); assertTrue(a.edits.isEmpty())
    }

    @Test fun anEarlierSoundLeavesTheSkipInTheEditListOfThePicture() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC); m.setAudio(asc, 48000, 2)
        m.addVideo(fill(10, 1), 0, 10, 0, 7200, true)                      // decode time 0, shown at 7200
        m.addVideo(fill(10, 1), 0, 10, 3600, 7200, false)
        m.addAudio(fill(10, 5), 0, 10, intArrayOf(10), 1, 2728)           // the sound starts at 5115 (90 kHz ticks): earlier than the first picture is shown
        m.finish()
        val r = Mp4Reader(out.bytes())
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(1, v.edits.size)
        assertEquals("the picture's media is entered 5115 ticks in, so it is shown 2085 ticks after the sound", 5115L, v.edits[0].mediaTime)
        assertEquals(23L, v.edits[0].duration)                             // (7200 - 5115) ticks = 23 ms
        assertTrue(a.edits.isEmpty())
    }

    @Test fun aLaterPictureGetsAnEmptyEditInFront() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 2)
        m.addAudio(fill(10, 5), 0, 10, intArrayOf(10), 1, -2000)             // the sound starts BEFORE the picture's decode time 0
        m.addVideo(fill(10, 1), 0, 10, 0, 0, true)
        m.finish()
        val v = Mp4Reader(out.bytes()).track("vide")!!
        assertEquals(2, v.edits.size)
        assertEquals(-1L, v.edits[0].mediaTime)
        assertEquals((2000L * 90000 / 44100 * 1000 + 45000) / 90000, v.edits[0].duration)
        assertEquals(0L, v.edits[1].mediaTime)
    }

    @Test fun chunksFollowTheTrackSwitches() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 2)
        m.addVideo(fill(10, 1), 0, 10, 0, 0, true)
        m.addAudio(fill(4, 7), 0, 4, intArrayOf(4), 1, 0)
        m.addAudio(fill(4, 7), 0, 4, intArrayOf(4), 1, 1024)
        m.addVideo(fill(10, 1), 0, 10, 3600, 0, false)
        m.addVideo(fill(10, 1), 0, 10, 7200, 0, false)
        m.finish()
        val r = Mp4Reader(out.bytes())
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(2, v.chunkCount); assertEquals(1, a.chunkCount)
        assertEquals(listOf(48L, 66L, 76L), v.samples.map { it.offset })
        assertEquals(listOf(58L, 62L), a.samples.map { it.offset })
    }

    @Test fun aGapInTheSoundLengthensTheFrameBeforeIt() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 2)
        m.addVideo(fill(10, 1), 0, 10, 0, 0, true)
        m.addAudio(fill(8, 7), 0, 8, intArrayOf(4, 4), 2, 0)
        m.addAudio(fill(4, 7), 0, 4, intArrayOf(4), 1, 2048 + 4410L)       // 100 ms after the end: a real gap
        m.addAudio(fill(4, 7), 0, 4, intArrayOf(4), 1, 3072 + 4410 + 5L)   // 5 ticks off the grid: ignored (far below 50 ms)
        m.finish()
        val a = Mp4Reader(out.bytes()).track("soun")!!
        assertEquals(4, a.samples.size)
        assertEquals(listOf(1024L, 1024L + 4410L, 1024L), a.samples.zipWithNext { x, y -> y.dts - x.dts })
        assertEquals(2048L + 4410L + 2048L, a.mediaDuration)
    }

    @Test fun rollbackCutsTablesAndPositionBackAndTheFileStaysValid() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC)
        m.addVideo(fill(10, 1), 0, 10, 0, 0, true)
        val mark = m.mark()
        m.addVideo(fill(99, 9), 0, 99, 3600, 0, true)
        m.addVideo(fill(99, 9), 0, 99, 7200, 0, true)
        m.rollback(mark)
        assertEquals(1, m.videoCount); assertEquals(58L, m.pos)
        m.addVideo(fill(12, 2), 0, 12, 3600, 0, false)
        m.finish()
        val v = Mp4Reader(out.bytes()).track("vide")!!
        assertEquals(listOf(10, 12), v.samples.map { it.size })
        assertEquals(listOf(true, false), v.samples.map { it.sync })
        assertEquals(listOf(48L, 58L), v.samples.map { it.offset })
        assertTrue(v.samples.all { s -> out.data.copyOfRange(s.offset.toInt(), s.offset.toInt() + s.size).all { it.toInt() == (if (s.size == 10) 1 else 2) } })
    }

    @Test fun theJournalRebuildsExactlyTheSameFile() {
        val a = MemOut()
        val m = newMux(a)
        val journal = ArrayList<ByteArray>()
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 2)
        var dts = 0L; var tick = 0L
        fun some(n: Int, base: Int) {
            for (i in 0 until n) {
                val len = 20 + (i * 7) % 13
                m.addVideo(fill(len, base + i), 0, len, dts, if (i % 3 == 0) 7200 else 0, i % 5 == 0); dts += 3600
                if (i % 2 == 0) { m.addAudio(fill(9, 3), 0, 9, intArrayOf(4, 5), 2, tick); tick += 2048 }
            }
        }
        some(11, 0); journal.add(m.drainJournal())
        val mark = m.mark()
        val dtsAtMark = dts; val tickAtMark = tick
        some(7, 50); journal.add(m.drainJournal())
        m.rollback(mark)
        dts = dtsAtMark; tick = tickAtMark
        some(9, 100); journal.add(m.drainJournal())
        m.finish()
        // a fresh muxer replays the journal; the sample bytes are already in the file
        val b = MemOut()
        b.write(0, a.data, 0, a.size)
        val m2 = Mp4Mux(b, fixedNow)
        for (j in journal) m2.replay(j)
        assertEquals(m.videoCount, m2.videoCount); assertEquals(m.audioCount, m2.audioCount); assertEquals(m.pos, m2.pos)
        m2.finish()
        assertArrayEquals("the same file, byte for byte", a.bytes(), b.bytes())
    }

    @Test fun filesBeyondFourGigabytesUse64BitOffsetsAndTheWideBox() {
        class Spy : MuxOut {
            val small = ArrayList<Pair<Long, ByteArray>>()
            override fun write(pos: Long, b: ByteArray, off: Int, len: Int) { if (len < (1 shl 20)) small.add(pos to b.copyOfRange(off, off + len)) }
        }
        val spy = Spy()
        val m = Mp4Mux(spy, fixedNow); m.start()
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 2)
        val big = ByteArray(64 shl 20)
        for (i in 0 until 70) m.addVideo(big, 0, big.size, i * 3600L, 0, i == 0)           // 4.4 GB of "video"
        m.addAudio(ByteArray(8), 0, 8, intArrayOf(8), 1, 0)                                 // its chunk starts beyond 4 GB
        val total = m.finish()
        assertTrue(total > 0xFFFFFFFFL)
        val header = spy.small.last { it.first == 32L }.second
        assertArrayEquals(byteArrayOf(0, 0, 0, 1, 'm'.code.toByte(), 'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte()), header.copyOfRange(0, 8))
        val size = header.copyOfRange(8, 16).fold(0L) { acc, x -> (acc shl 8) or (x.toLong() and 0xFF) }
        assertEquals("the box covers the payload up to where the moov begins", 48L + 70L * (64 shl 20) + 8L - 32L, size)
        val moov = spy.small.last { it.second.size > 100 }.second
        assertTrue(String(moov, Charsets.ISO_8859_1).contains("co64"))
    }

    @Test fun manySamplesKeepTheTablesConsistent() {
        val out = MemOut()
        val m = newMux(out)
        m.setVideo(64, 36, avcC); m.setAudio(asc, 44100, 1)
        for (i in 0 until 5000) {
            m.addVideo(fill(1 + i % 7, 1), 0, 1 + i % 7, i * 3600L, (i % 4) * 3600, i % 50 == 0)
            if (i % 3 == 0) m.addAudio(fill(6, 2), 0, 6, intArrayOf(3, 3), 2, i * 2048L / 3)
        }
        m.finish()
        val r = Mp4Reader(out.bytes())
        val v = r.track("vide")!!
        assertEquals(5000, v.samples.size); assertEquals(100, v.samples.count { it.sync })
        assertEquals(5000 * 3600L, v.mediaDuration)
        assertEquals(3334, r.track("soun")!!.samples.size)
    }
}
