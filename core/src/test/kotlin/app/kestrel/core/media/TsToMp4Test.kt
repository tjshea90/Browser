package app.kestrel.core.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class TsToMp4Test {
    private class Seg(val name: String, val disc: Boolean = false, val data: ByteArray = Fixtures.bytes(name))
    private class Run(val out: MemOut, val journal: MemJournal, val m: TsToMp4)

    private val now = { 1_700_000_000_000L }

    /** The playlist time of the run a segment begins: 2 s per segment up to the next discontinuity. */
    private fun runMs(segs: List<Seg>, i: Int): Long { var t = 0L; var k = i; do { t += 2000; k++ } while (k < segs.size && !segs[k].disc); return t }

    private fun newRun(segs: List<Seg>, out: MemOut = MemOut(), journal: MemJournal = MemJournal()) = Run(out, journal, TsToMp4(out, journal, {}, 2000L * segs.size, now))

    private fun feedSegment(r: Run, segs: List<Seg>, i: Int, piece: Int) {
        r.m.beginSegment(i, segs[i].disc, runMs(segs, i))
        val d = segs[i].data
        var p = 0
        while (p < d.size) { val n = minOf(piece, d.size - p); r.m.feed(d, p, n); p += n }
        r.m.endSegment()
    }

    private fun mux(segs: List<Seg>, piece: Int = 4096): Pair<ByteArray, Pair<TsToMp4.Finished, Run>> {
        val r = newRun(segs)
        r.m.startFresh()
        for (i in segs.indices) feedSegment(r, segs, i, piece)
        val fin = r.m.finish()
        return r.out.bytes() to (fin to r)
    }

    private fun assertDecodes(file: ByteArray) {
        if (!Fixtures.ffmpeg) return
        val said = Fixtures.ffmpegDecode(file)
        assertEquals("ffmpeg reports problems decoding the file:\n$said", "", said)
    }

    private fun checkVideoSamples(r: Mp4Reader, v: Mp4Reader.Track) {
        val file = r.sampleBytes(Mp4Reader.Sample(0, 0, 0, 0, true))                                    // (no-op: keeps the reader API in use)
        assertEquals(0, file.size)
        assertTrue("the first picture is a keyframe", v.samples.first().sync)
        for ((i, s) in v.samples.withIndex()) {
            val b = r.sampleBytes(s)
            var p = 0
            var nals = 0
            while (p < b.size) {
                assertTrue("sample $i: room for a length at $p", p + 4 <= b.size)
                val len = ((b[p].toInt() and 0xFF) shl 24) or ((b[p + 1].toInt() and 0xFF) shl 16) or ((b[p + 2].toInt() and 0xFF) shl 8) or (b[p + 3].toInt() and 0xFF)
                assertTrue("sample $i: NAL length $len at $p", len > 0 && p + 4 + len <= b.size)
                assertEquals("sample $i: forbidden_zero_bit", 0, b[p + 4].toInt() and 0x80)
                val type = b[p + 4].toInt() and 0x1F
                assertTrue("sample $i: NAL type $type", type in 1..8 && type != 9)
                p += 4 + len; nals++
            }
            assertEquals(b.size, p)
            assertTrue(nals > 0)
        }
        // decode order: strictly rising decode times; every picture's composition time is not before its decode time
        for (k in 1 until v.samples.size) assertTrue("dts must rise at $k", v.samples[k].dts > v.samples[k - 1].dts)
        assertTrue(v.samples.all { it.cts >= it.dts })
    }

    private fun checkAudioSamples(r: Mp4Reader, a: Mp4Reader.Track) {
        for ((i, s) in a.samples.withIndex()) {
            val b = r.sampleBytes(s)
            assertTrue("audio sample $i is a raw frame, not ADTS", b.size >= 2 && !((b[0].toInt() and 0xFF) == 0xFF && (b[1].toInt() and 0xF0) == 0xF0))
        }
        for (k in 1 until a.samples.size) assertTrue(a.samples[k].dts > a.samples[k - 1].dts)
    }

    // ---------------------------------------------------------------------------------------------------------------- the plain cases

    @Test fun aStreamWithBFramesBecomesAnMp4() {
        val (file, res) = mux(listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("a_2.ts")))
        val (fin, _) = res
        assertNull(fin.warning)
        assertEquals(file.size.toLong(), fin.length)
        val r = Mp4Reader(file)
        assertEquals(listOf("ftyp", "wide", "mdat", "moov"), r.topLevel)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(150, v.samples.size); assertEquals(128, v.width); assertEquals(72, v.height)
        assertEquals(6, v.samples.count { it.sync })
        assertEquals(150 * 3600L, v.mediaDuration)
        assertEquals("the first picture is decoded at 0 and shown two frames later (B-frames)", 7200L, v.samples[0].cts)
        assertEquals(260, a.samples.size); assertEquals(44100L, a.timescale); assertEquals(1, a.channels)
        assertTrue(a.samples.zipWithNext { x, y -> y.dts - x.dts }.all { it == 1024L })
        checkVideoSamples(r, v); checkAudioSamples(r, a)
        // the sound began 2090 ticks (23 ms) before the first picture is shown: the picture's edit enters its media 5110 ticks in (decode 126000 -> sound at 131110)
        assertEquals(1, v.edits.size); assertEquals(5110L, v.edits[0].mediaTime); assertTrue(a.edits.isEmpty())
        assertDecodes(file)
    }

    @Test fun ffprobeAgreesWithOurTables() {
        if (!Fixtures.ffmpeg) return
        val (file, _) = mux(listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("a_2.ts")))
        val v = Fixtures.ffprobe(file, "video", "codec_name,width,height,nb_read_frames,start_time,duration")
        assertEquals("h264", v["codec_name"]); assertEquals("128", v["width"]); assertEquals("72", v["height"]); assertEquals("150", v["nb_read_frames"])
        val a = Fixtures.ffprobe(file, "audio", "codec_name,sample_rate,channels,nb_read_frames,start_time")
        assertEquals("aac", a["codec_name"]); assertEquals("44100", a["sample_rate"]); assertEquals("1", a["channels"])
        // lip sync: in the source the picture is shown 23.2 ms after the first sound
        val skew = v["start_time"]!!.toDouble() - a["start_time"]!!.toDouble()
        assertEquals(0.0232, skew, 0.004)
    }

    @Test fun stereoStreamWithoutBFramesAndAnEarlierSoundStart() {
        val (file, _) = mux(listOf(Seg("n_0.ts"), Seg("n_1.ts")))
        val r = Mp4Reader(file)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(120, v.samples.size); assertEquals(96, v.width); assertEquals(54, v.height)
        assertEquals("no B-frames: shown when decoded", v.samples.map { it.dts }, v.samples.map { it.cts })
        assertEquals(30 * 3000L / 30, 3000L); assertEquals(120 * 3000L, v.mediaDuration)
        assertEquals(189, a.samples.size); assertEquals(48000L, a.timescale); assertEquals(2, a.channels)
        // the sound is 1920 ticks (21 ms) ahead of the first picture: the picture gets an empty edit in front
        assertEquals(2, v.edits.size); assertEquals(-1L, v.edits[0].mediaTime)
        checkVideoSamples(r, v); checkAudioSamples(r, a)
        assertDecodes(file)
    }

    @Test fun pieceSizesGiveByteIdenticalFiles() {
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"))
        val ref = mux(segs, 1 shl 20).first
        for (piece in intArrayOf(1, 7, 187, 188, 189, 1000, 65536)) assertArrayEquals("pieces of $piece", ref, mux(segs, piece).first)
    }

    // ---------------------------------------------------------------------------------------------------------------- runs, advertisements, clocks

    @Test fun anAdvertisementRunInTheMiddleIsLeftOut() {
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("ad_0.ts", disc = true), Seg("a_0.ts", disc = true), Seg("a_1.ts"))
        val (file, res) = mux(segs)
        val (fin, _) = res
        assertNotNull(fin.warning); assertTrue(fin.warning!!, fin.warning!!.startsWith("1 part"))
        val r = Mp4Reader(file)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(200, v.samples.size); assertEquals(128, v.width)
        assertEquals(344, a.samples.size)
        // the second run follows the first on one clock: no big gap, no overlap
        val deltas = v.samples.zipWithNext { x, y -> y.dts - x.dts }
        assertTrue("largest step ${deltas.max()}", deltas.max() <= 3600 + 6000)
        assertTrue(a.samples.zipWithNext { x, y -> y.dts - x.dts }.all { it in 1024L..1024L + 7000L })
        checkVideoSamples(r, v); checkAudioSamples(r, a)
        assertDecodes(file)
    }

    @Test fun aLongerDifferentRunReplacesAnAdvertisementBeforeTheFilm() {
        val segs = listOf(Seg("ad_0.ts"), Seg("a_0.ts", disc = true), Seg("a_1.ts"), Seg("a_2.ts"))
        val (file, res) = mux(segs)
        assertNotNull(res.first.warning)
        val r = Mp4Reader(file)
        val v = r.track("vide")!!
        assertEquals(128, v.width); assertEquals(150, v.samples.size)
        assertEquals(44100L, r.track("soun")!!.timescale)
        assertEquals(0L, v.samples[0].dts)
        checkVideoSamples(r, v)
        assertDecodes(file)
    }

    @Test fun aRunThatDoesNotFitAndIsNotLongerIsLeftOutEvenWhenItComesFirst() {
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("ad_0.ts", disc = true))
        val (file, res) = mux(segs)
        assertNotNull(res.first.warning)
        assertEquals(100, Mp4Reader(file).track("vide")!!.samples.size)
    }

    @Test fun aClockThatRestartsWithoutATagStartsANewRun() {
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("a_0.ts"), Seg("a_1.ts"))       // no discontinuity flag, but the stamps jump back
        val (file, res) = mux(segs)
        assertNull(res.first.warning)
        val r = Mp4Reader(file)
        val v = r.track("vide")!!
        assertEquals(200, v.samples.size)
        val deltas = v.samples.zipWithNext { x, y -> y.dts - x.dts }
        assertTrue("largest step ${deltas.max()}", deltas.max() <= 3600 + 6000)
        checkVideoSamples(r, v); checkAudioSamples(r, r.track("soun")!!)
        assertDecodes(file)
    }

    @Test fun picturesBeforeTheFirstKeyframeAndSoundBeforeThemAreDropped() {
        // take segment 0 and cut the first picture (the keyframe) out of it: it now begins with 24 pictures that cannot be decoded
        val src = Fixtures.bytes("a_0.ts")
        val cut = java.io.ByteArrayOutputStream()
        var pes = 0
        var i = 0
        while (i < src.size) {
            val pid = ((src[i + 1].toInt() and 0x1F) shl 8) or (src[i + 2].toInt() and 0xFF)
            if (pid == 256 && (src[i + 1].toInt() and 0x40) != 0) pes++
            if (!(pid == 256 && pes == 1)) cut.write(src, i, 188)
            i += 188
        }
        val segs = listOf(Seg("a_0.ts", data = cut.toByteArray()), Seg("a_1.ts"))
        val (file, _) = mux(segs)
        val r = Mp4Reader(file)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals("the second keyframe starts the file: 25 + 50 pictures", 75, v.samples.size)
        assertTrue(v.samples.first().sync)
        assertTrue("the sound of the dropped second is gone too: ${a.samples.size}", a.samples.size in 150..200)
        checkVideoSamples(r, v); checkAudioSamples(r, a)
        assertDecodes(file)
    }

    @Test fun aStreamWithNoPictureIsRefusedAtTheEnd() {
        val r = newRun(listOf(Seg("n_0.ts")))
        r.m.startFresh()
        try { r.m.finish(); fail("no picture") } catch (e: MuxException) { assertTrue(e.message!!, e.message!!.contains("picture")) }
    }

    @Test fun unsupportedRunsAreLeftOutAndTheFirstSupportedStreamStillWorks() {
        // the PMT of the 'ad' run says H.265: the run cannot be written and is left out
        val hevc = Fixtures.bytes("ad_0.ts").copyOf()
        var i = 0
        while (i < hevc.size) {
            val pid = ((hevc[i + 1].toInt() and 0x1F) shl 8) or (hevc[i + 2].toInt() and 0xFF)
            if (pid == 4096 && (hevc[i + 1].toInt() and 0x40) != 0) { for (k in i + 4 until i + 188) if (hevc[k].toInt() == 0x1B && hevc[k + 1].toInt() == 0xE1.toByte().toInt()) { hevc[k] = 0x24; break } }
            i += 188
        }
        val segs = listOf(Seg("ad_0.ts", data = hevc, disc = true), Seg("a_0.ts", disc = true), Seg("a_1.ts"))
        val r = newRun(segs)
        r.m.startFresh()
        for (k in segs.indices) feedSegment(r, segs, k, 4096)
        val fin = r.m.finish()
        assertNotNull(fin.warning)
        assertEquals(100, Mp4Reader(r.out.bytes()).track("vide")!!.samples.size)
    }

    // ---------------------------------------------------------------------------------------------------------------- kill and resume

    @Test fun aKilledDownloadCarriesOnFromTheLastCheckpoint() {
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("a_2.ts"))
        val whole = mux(segs).first
        val ref = Mp4Reader(whole)
        for (killAfter in 1..2) {
            val out = MemOut(); val journal = MemJournal()
            val first = Run(out, journal, TsToMp4(out, journal, {}, 6000, now))
            first.m.startFresh()
            for (k in 0 until killAfter) feedSegment(first, segs, k, 3000)
            first.m.checkpoint(killAfter)
            // the process dies half way through the next segment: bytes beyond the checkpoint are already in the file
            first.m.beginSegment(killAfter, false, 0)
            first.m.feed(segs[killAfter].data, 0, segs[killAfter].data.size / 2)
            val staleEnd = first.m.position
            assertTrue(staleEnd > 0)
            // a new process: same file, same journal
            val second = Run(out, journal, TsToMp4(out, journal, {}, 6000, now))
            assertEquals("continue with the segment after the checkpoint", killAfter, second.m.recover())
            for (k in killAfter until segs.size) feedSegment(second, segs, k, 3000)
            val fin = second.m.finish()
            assertNull(fin.warning)
            val file = out.bytes()
            assertEquals(fin.length, file.size.toLong())
            val r = Mp4Reader(file)
            val v = r.track("vide")!!; val a = r.track("soun")!!
            assertEquals("pictures after a kill at $killAfter", 150, v.samples.size)
            assertEquals(ref.track("vide")!!.samples.map { it.size }, v.samples.map { it.size })
            assertEquals(ref.track("soun")!!.samples.map { it.size }, a.samples.map { it.size })
            // the clock may differ by a few milliseconds where the run was cut (the muxer lays the resumed part after the end of the sound)
            assertTrue(v.samples.indices.all { Math.abs(v.samples[it].dts - ref.track("vide")!!.samples[it].dts) <= 1500 })
            checkVideoSamples(r, v); checkAudioSamples(r, a)
            assertDecodes(file)
        }
    }

    @Test fun nothingToRecoverMeansStartingOver() {
        val out = MemOut(); val journal = MemJournal()
        val m = TsToMp4(out, journal, {}, 2000, now)
        assertNull(m.recover())
    }

    @Test fun theJournalFileSurvivesATornWrite() {
        val f = File.createTempFile("mux", ".jnl")
        try {
            f.delete()
            val j = FileMuxJournal(f)
            j.append(byteArrayOf(1, 2, 3)); j.append(byteArrayOf(4, 5, 6, 7))
            val good = f.length()
            f.appendBytes(byteArrayOf(0xB1.toByte(), 0, 0, 0, 50, 1, 2, 3, 4, 9, 9))        // a block cut off by power loss
            val blocks = j.recover()
            assertEquals(2, blocks.size); assertArrayEquals(byteArrayOf(4, 5, 6, 7), blocks[1])
            assertEquals("the damaged tail is removed", good, f.length())
            j.append(byteArrayOf(8)); assertEquals(3, j.recover().size)
            // a flipped bit inside a block drops that block and everything after it
            val all = f.readBytes(); all[12] = (all[12].toInt() xor 1).toByte(); f.writeBytes(all)
            assertEquals(1, j.recover().size)
            j.reset(); assertTrue(j.recover().isEmpty())
        } finally { f.delete() }
    }

    @Test fun theWarningIsAbsentForACleanStream() {
        val (_, res) = mux(listOf(Seg("a_0.ts"), Seg("a_1.ts")))
        assertNull(res.first.warning)
        assertFalse(res.second.out.size == 0)
    }
}
