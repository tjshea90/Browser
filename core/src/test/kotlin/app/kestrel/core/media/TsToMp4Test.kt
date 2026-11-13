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
        // KESTREL_MUX_DUMP=<dir> keeps every file the tests make, for ffprobe / a player
        System.getenv("KESTREL_MUX_DUMP")?.let { File(it, "mux-" + segs.joinToString("-") { s -> s.name.removeSuffix(".ts") + (if (s.disc) "d" else "") } + ".mp4").writeBytes(r.out.bytes()) }
        return r.out.bytes() to (fin to r)
    }

    private fun assertDecodes(file: ByteArray) {
        if (!Fixtures.ffmpeg) return
        val said = Fixtures.ffmpegDecode(file)
        assertEquals("ffmpeg reports problems decoding the file:\n$said", "", said)
    }

    private fun checkVideoSamples(r: Mp4Reader, v: Mp4Reader.Track) {
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
        assertEditsCover(v)
    }

    /** What a player shows is what the edit list covers: every picture's composition span must lie inside it (an edit that ends early makes players drop the last B-frames). */
    private fun assertEditsCover(t: Mp4Reader.Track) {
        if (t.edits.isEmpty()) return
        val last = t.samples.size - 1
        var end = 0L
        for ((i, s) in t.samples.withIndex()) { val dur = if (i < last) t.samples[i + 1].dts - s.dts else (if (last > 0) s.dts - t.samples[i - 1].dts else 3000L); end = maxOf(end, s.cts + dur) }
        val main = t.edits.last()
        val covered = main.mediaTime + main.duration * t.timescale / 1000
        assertTrue("edit list ends at $covered ticks, the last picture is shown until $end", covered + t.timescale / 1000 >= end)
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
        // the sound begins 2090 ticks (23 ms) before the first picture is shown: an empty edit of 23 ms, then the media from the first composition time 7200 for 6 s (what ffmpeg writes for this stream)
        assertEquals(2, v.edits.size); assertEquals(-1L to 23L, v.edits[0].mediaTime to v.edits[0].duration); assertEquals(7200L to 6000L, v.edits[1].mediaTime to v.edits[1].duration)
        assertTrue(a.edits.isEmpty())
        assertDecodes(file)
    }

    @Test fun ffprobeAgreesWithOurTables() {
        if (!Fixtures.ffmpeg) return
        val (file, _) = mux(listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("a_2.ts")))
        val v = Fixtures.ffprobe(file, "video", "codec_name,width,height,nb_read_frames,start_time,duration")
        assertEquals("h264", v["codec_name"]); assertEquals("128", v["width"]); assertEquals("72", v["height"]); assertEquals("150", v["nb_read_frames"])
        val a = Fixtures.ffprobe(file, "audio", "codec_name,sample_rate,channels,nb_read_frames,start_time")
        assertEquals("aac", a["codec_name"]); assertEquals("44100", a["sample_rate"]); assertEquals("1", a["channels"])
        // lip sync: in the source the picture is shown 23.2 ms after the first sound; the first packets' presentation times must keep that
        val first = Fixtures.firstPacketTimes(file)
        assertEquals(0.0232, first[0]!! - first[1]!!, 0.004)
    }

    @Test fun stereoStreamWithoutBFramesAndAnEarlierSoundStart() {
        val (file, _) = mux(listOf(Seg("n_0.ts"), Seg("n_1.ts")))
        val r = Mp4Reader(file)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals(120, v.samples.size); assertEquals(96, v.width); assertEquals(54, v.height)
        assertEquals("no B-frames: shown when decoded", v.samples.map { it.dts }, v.samples.map { it.cts })
        assertEquals(120 * 3000L, v.mediaDuration)
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
        assertEquals(2 * (84 + 86), a.samples.size)
        // the second run follows the first on one clock: no big gap, no overlap
        val deltas = v.samples.zipWithNext { x, y -> y.dts - x.dts }
        assertTrue("largest step ${deltas.max()}", deltas.max() <= 3600 + 6000)
        assertTrue(a.samples.zipWithNext { x, y -> y.dts - x.dts }.all { it in 1024L..1024L + 7000L })
        checkVideoSamples(r, v); checkAudioSamples(r, a)
        assertDecodes(file)
    }

    @Test fun anUnmarkedSpliceOfAnotherFormatIsLeftOutAndTheFilmCarriesOn() {
        // no #EXT-X-DISCONTINUITY anywhere: the advertisement is recognised by its other picture size, and the film is recognised again when it returns
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("ad_0.ts"), Seg("a_0.ts"), Seg("a_1.ts"))
        val (file, res) = mux(segs)
        assertNotNull(res.first.warning)
        val r = Mp4Reader(file)
        val v = r.track("vide")!!
        assertEquals(128, v.width); assertEquals(200, v.samples.size)
        assertEquals(344 - 4, r.track("soun")!!.samples.size)
        val deltas = v.samples.zipWithNext { x, y -> y.dts - x.dts }
        assertTrue("largest step ${deltas.max()}", deltas.max() <= 3600 + 6000)
        checkVideoSamples(r, v); checkAudioSamples(r, r.track("soun")!!)
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
        assertTrue("the sound of the dropped second is gone too: ${a.samples.size}", a.samples.size in 110..150)
        checkVideoSamples(r, v); checkAudioSamples(r, a)
        assertDecodes(file)
    }

    @Test fun aStreamWithNoPictureIsRefusedAtTheEnd() {
        val r = newRun(listOf(Seg("n_0.ts")))
        r.m.startFresh()
        try { r.m.finish(); fail("no picture") } catch (e: MuxException) { assertTrue(e.message!!, e.message!!.contains("picture")) }
    }

    @Test fun unsupportedRunsAreLeftOutAndTheFirstSupportedStreamStillWorks() {
        // the PMT of the 'ad' run says MPEG-2 video: the run cannot be written and is left out
        val hevc = Fixtures.bytes("ad_0.ts").copyOf()
        var i = 0
        while (i < hevc.size) {
            val pid = ((hevc[i + 1].toInt() and 0x1F) shl 8) or (hevc[i + 2].toInt() and 0xFF)
            if (pid == 4096 && (hevc[i + 1].toInt() and 0x40) != 0) { for (k in i + 4 until i + 188) if (hevc[k].toInt() == 0x1B && hevc[k + 1].toInt() == 0xE1.toByte().toInt()) { hevc[k] = 0x02; break } }
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

    // ---------------------------------------------------------------------------------------------------------------- H.265 and other sound (AD1b-2: "mp4 always")

    /** Every sample of an H.265 track is a run of length-prefixed NAL units without parameter sets / delimiters (they live in `hvcC`); the first sample is an IRAP picture. */
    private fun checkHevcSamples(r: Mp4Reader, v: Mp4Reader.Track) {
        assertTrue("the first picture is a keyframe", v.samples.first().sync)
        for ((i, s) in v.samples.withIndex()) {
            val b = r.sampleBytes(s)
            var p = 0
            while (p < b.size) {
                assertTrue("sample $i: room for a length at $p", p + 4 <= b.size)
                val len = ((b[p].toInt() and 0xFF) shl 24) or ((b[p + 1].toInt() and 0xFF) shl 16) or ((b[p + 2].toInt() and 0xFF) shl 8) or (b[p + 3].toInt() and 0xFF)
                assertTrue("sample $i: NAL length $len at $p", len > 2 && p + 4 + len <= b.size)
                assertEquals("sample $i: forbidden_zero_bit", 0, b[p + 4].toInt() and 0x80)
                val type = (b[p + 4].toInt() shr 1) and 0x3F
                assertTrue("sample $i: NAL type $type is not a parameter set or delimiter", type !in 32..38)
                p += 4 + len
            }
            assertEquals("sample $i ends exactly at its last NAL", b.size, p)
        }
    }

    @Test fun anH265StreamBecomesAnMp4WithHvc1() {
        val segs = listOf(Seg("hevc_0.ts"), Seg("hevc_1.ts"))
        assertNull("the writer accepts it", TsToMp4.probe(segs[0].data, 0, segs[0].data.size))
        val (file, res) = mux(segs)
        assertNull(res.first.warning)
        val r = Mp4Reader(file)
        assertEquals(listOf("ftyp", "wide", "mdat", "moov"), r.topLevel)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals("hvc1", v.entryType); assertEquals(128, v.width); assertEquals(72, v.height)
        assertEquals(50, v.samples.size)
        assertTrue("one keyframe per second", v.samples.count { it.sync } >= 2)
        assertEquals("mp4a", a.entryType)
        checkHevcSamples(r, v)
        assertDecodes(file)
        if (Fixtures.ffmpeg) {
            val p = Fixtures.ffprobe(file, "video", "codec_name,width,height,nb_read_frames,profile")
            assertEquals("hevc", p["codec_name"]); assertEquals("128", p["width"]); assertEquals("72", p["height"]); assertEquals("50", p["nb_read_frames"])
        }
    }

    @Test fun ac3SoundIsKeptAsAc3SamplesOnTheExactGrid() {
        val segs = listOf(Seg("ac3_0.ts"), Seg("ac3_1.ts"))
        assertNull(TsToMp4.probe(segs[0].data, 0, segs[0].data.size))
        val (file, res) = mux(segs)
        assertNull(res.first.warning)
        val r = Mp4Reader(file)
        val v = r.track("vide")!!; val a = r.track("soun")!!
        assertEquals("avc1", v.entryType); assertEquals("ac-3", a.entryType); assertEquals(48000L, a.timescale); assertEquals(1, a.channels)
        assertTrue("about two seconds of sound: ${a.samples.size} frames", a.samples.size in 60..64)
        assertTrue(a.samples.zipWithNext { x, y -> y.dts - x.dts }.all { it == 1536L })
        for (s in a.samples) { val b = r.sampleBytes(s); assertEquals("every sample is one whole syncframe", 0x0B, b[0].toInt()); assertEquals(0x77, b[1].toInt()) }
        assertDecodes(file)
        if (Fixtures.ffmpeg) { val p = Fixtures.ffprobe(file, "audio", "codec_name,sample_rate,channels"); assertEquals("ac3", p["codec_name"]); assertEquals("48000", p["sample_rate"]); assertEquals("1", p["channels"]) }
    }

    @Test fun eac3SoundIsKeptAsEc3Samples() {
        val segs = listOf(Seg("eac3_0.ts"), Seg("eac3_1.ts"))
        assertNull(TsToMp4.probe(segs[0].data, 0, segs[0].data.size))
        val (file, res) = mux(segs)
        assertNull(res.first.warning)
        val r = Mp4Reader(file)
        val a = r.track("soun")!!
        assertEquals("ec-3", a.entryType); assertEquals(48000L, a.timescale)
        assertTrue("about two seconds of sound: ${a.samples.size} frames", a.samples.size in 60..64)
        assertTrue(a.samples.zipWithNext { x, y -> y.dts - x.dts }.all { it == 1536L })
        assertDecodes(file)
        if (Fixtures.ffmpeg) { val p = Fixtures.ffprobe(file, "audio", "codec_name,sample_rate"); assertEquals("eac3", p["codec_name"]); assertEquals("48000", p["sample_rate"]) }
    }

    @Test fun mp3SoundIsKeptAsMp4aWithTheMpegAudioObjectType() {
        val segs = listOf(Seg("mp3_0.ts"), Seg("mp3_1.ts"))
        assertNull(TsToMp4.probe(segs[0].data, 0, segs[0].data.size))
        val (file, res) = mux(segs)
        assertNull(res.first.warning)
        val r = Mp4Reader(file)
        val a = r.track("soun")!!
        assertEquals("mp4a", a.entryType); assertEquals(48000L, a.timescale)
        assertTrue("every frame lasts 1152 samples", a.samples.zipWithNext { x, y -> y.dts - x.dts }.all { it == 1152L })
        assertTrue("object type 0x6B (MPEG-1 audio) in the esds: ${a.codecConfig.joinToString(",") { "%02x".format(it) }}", a.codecConfig.any { it.toInt() == 0x6B })
        assertDecodes(file)
        if (Fixtures.ffmpeg) { val p = Fixtures.ffprobe(file, "audio", "codec_name,sample_rate"); assertEquals("mp3", p["codec_name"]); assertEquals("48000", p["sample_rate"]) }
    }

    @Test fun aKilledH265DownloadCarriesOnFromItsCheckpoint() {
        // the journal knows the new picture and sound configs: a resumed file equals an uninterrupted one
        val segs = listOf(Seg("hevc_0.ts"), Seg("hevc_1.ts"))
        val (whole, _) = mux(segs)
        val out = MemOut(); val journal = MemJournal()
        val first = TsToMp4(out, journal, {}, 4000L, now); first.startFresh()
        first.beginSegment(0, false, 4000L); val d0 = segs[0].data; var p = 0
        while (p < d0.size) { val n = minOf(4096, d0.size - p); first.feed(d0, p, n); p += n }
        first.endSegment(); first.checkpoint(1)
        val second = TsToMp4(out, journal, {}, 4000L, now)           // a new process: nothing but the file and the journal
        assertEquals(1, second.recover())
        second.beginSegment(1, false, 4000L); val d1 = segs[1].data; p = 0
        while (p < d1.size) { val n = minOf(4096, d1.size - p); second.feed(d1, p, n); p += n }
        second.endSegment()
        second.finish()
        assertArrayEquals(whole, out.bytes())
    }

    // ---------------------------------------------------------------------------------------------------------------- kill and resume

    @Test fun aKilledDownloadCarriesOnFromTheLastCheckpoint() {
        val segs = listOf(Seg("a_0.ts"), Seg("a_1.ts"), Seg("a_2.ts"))
        val whole = mux(segs).first
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
            System.getenv("KESTREL_MUX_DUMP")?.let { File(it, "resume-$killAfter.mp4").writeBytes(file) }
            assertEquals(fin.length, file.size.toLong())
            assertArrayEquals("a download killed after segment $killAfter ends as the very same file as an uninterrupted one", whole, file)
            val r = Mp4Reader(file)
            checkVideoSamples(r, r.track("vide")!!); checkAudioSamples(r, r.track("soun")!!)
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
