package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TsDemuxTest {
    private class Got : TsDemux.Out {
        val vPts = ArrayList<Long>(); val vDts = ArrayList<Long>(); val vSizes = ArrayList<Int>(); val vNals = ArrayList<List<Int>>()
        val aPts = ArrayList<Long>(); var aFrames = 0
        override fun video(es: ByteArray, len: Int, pts: Long, dts: Long) {
            vPts.add(pts); vDts.add(dts); vSizes.add(len)
            val t = ArrayList<Int>(); Avc.forEachNal(es, 0, len) { a, _ -> t.add(es[a].toInt() and 0x1F) }; vNals.add(t)
        }
        override fun audio(es: ByteArray, len: Int, pts: Long) { aPts.add(pts); aFrames += Adts.frames(es, 0, len).size }
    }

    private fun run(data: ByteArray, piece: Int, got: Got = Got()): Pair<Got, TsDemux> {
        val d = TsDemux(got)
        var i = 0
        while (i < data.size) { val n = minOf(piece, data.size - i); d.feed(data, i, n); i += n }
        d.endSegment()
        return got to d
    }

    @Test fun demuxesAFixtureSegmentExactly() {
        val (g, d) = run(Fixtures.bytes("a_0.ts"), 1 shl 20)
        assertEquals(0x1B, d.videoType); assertEquals(0x0F, d.audioType); assertTrue(d.programKnown); assertNull(d.problem)
        assertEquals(50, g.vPts.size)
        assertEquals(133200L, g.vPts[0]); assertEquals(126000L, g.vDts[0]); assertEquals(302400L, g.vDts.last())
        assertEquals(2, g.vNals.count { 5 in it })                    // two keyframes
        assertTrue("every picture starts with an access unit delimiter", g.vNals.all { it[0] == 9 })
        assertTrue("keyframes carry SPS and PPS", g.vNals.filter { 5 in it }.all { 7 in it && 8 in it })
        assertEquals(84, g.aFrames); assertEquals(131110L, g.aPts[0])
        assertEquals(0L, d.junkBytes)
    }

    @Test fun pieceSizesDoNotChangeTheResult() {
        val data = Fixtures.bytes("a_1.ts")
        val (ref, _) = run(data, 1 shl 20)
        for (piece in intArrayOf(1, 7, 100, 187, 188, 189, 1000, 4097)) {
            val (g, _) = run(data, piece)
            assertEquals("pieces of $piece", ref.vDts, g.vDts); assertEquals(ref.vPts, g.vPts); assertEquals(ref.vSizes, g.vSizes)
            assertEquals(ref.aPts, g.aPts); assertEquals(ref.aFrames, g.aFrames)
        }
    }

    @Test fun stereoSegmentWithoutBFramesHasEqualPtsAndDts() {
        val (g, _) = run(Fixtures.bytes("n_0.ts"), 5000)
        assertEquals(60, g.vPts.size); assertEquals(g.vPts, g.vDts)
        assertEquals(95, g.aFrames)
    }

    @Test fun aPacketWithTheErrorFlagOrALostPacketCostsOnlyThatPicture() {
        val data = Fixtures.bytes("a_0.ts")
        // find the second video PES (the 2nd packet with PUSI on PID 256) and flag one of its continuation packets as damaged
        var pes = 0; var victim = -1
        var i = 0
        while (i < data.size) {
            val pid = ((data[i + 1].toInt() and 0x1F) shl 8) or (data[i + 2].toInt() and 0xFF)
            if (pid == 256 && (data[i + 1].toInt() and 0x40) != 0) { pes++; if (pes == 2) { victim = i + 188; break } }
            i += 188
        }
        assertTrue(victim > 0)
        val damaged = data.copyOf(); damaged[victim + 1] = (damaged[victim + 1].toInt() or 0x80).toByte()    // transport_error_indicator
        val (g, _) = run(damaged, 4096)
        assertEquals("one picture is dropped, the others are untouched", 49, g.vPts.size)
        // a lost packet (missing from the stream) is noticed by the continuity counter
        val lost = data.copyOfRange(0, victim) + data.copyOfRange(victim + 188, data.size)
        val (g2, _) = run(lost, 4096)
        assertEquals(49, g2.vPts.size)
    }

    @Test fun stuffingBeforeTheFirstPacketIsSkipped() {
        val data = ByteArray(333) { 0x55 } + Fixtures.bytes("n_1.ts")
        val (g, d) = run(data, 777)
        assertEquals(60, g.vPts.size); assertEquals(94, g.aFrames)
        assertTrue(d.junkBytes >= 333)
    }

    @Test fun timeStampsPastTheWrapOfTheCounterKeepCountingUp() {
        // shift every PTS/DTS of a fixture to just below 2^33 so the stream wraps inside the segment
        val data = Fixtures.bytes("a_0.ts").copyOf()
        val shift = (1L shl 33) - 200_000L
        fun put(o: Int, marker: Int, v: Long) {
            data[o] = (marker or (((v shr 30) and 7).toInt() shl 1) or 1).toByte(); data[o + 1] = ((v shr 22) and 0xFF).toByte()
            data[o + 2] = ((((v shr 15) and 0x7F).toInt() shl 1) or 1).toByte(); data[o + 3] = ((v shr 7) and 0xFF).toByte(); data[o + 4] = (((v and 0x7F).toInt() shl 1) or 1).toByte()
        }
        fun get(o: Int): Long = (((data[o].toLong() shr 1) and 7) shl 30) or ((data[o + 1].toLong() and 0xFF) shl 22) or (((data[o + 2].toLong() and 0xFF) shr 1) shl 15) or ((data[o + 3].toLong() and 0xFF) shl 7) or ((data[o + 4].toLong() and 0xFF) shr 1)
        var i = 0
        while (i < data.size) {
            val pid = ((data[i + 1].toInt() and 0x1F) shl 8) or (data[i + 2].toInt() and 0xFF)
            if ((pid == 256 || pid == 257) && (data[i + 1].toInt() and 0x40) != 0) {
                var o = i + 4; if ((data[i + 3].toInt() shr 4) and 2 != 0) o += 1 + (data[o].toInt() and 0xFF)
                val flags = data[o + 7].toInt() and 0xFF
                if (flags and 0x80 != 0) put(o + 9, if (flags and 0x40 != 0) 0x30 else 0x20, (get(o + 9) + shift) and ((1L shl 33) - 1))
                if (flags and 0x40 != 0) put(o + 14, 0x10, (get(o + 14) + shift) and ((1L shl 33) - 1))
            }
            i += 188
        }
        val (g, _) = run(data, 4096)
        assertEquals(50, g.vDts.size)
        for (k in 1 until g.vDts.size) assertTrue("dts must keep rising across the wrap at $k", g.vDts[k] > g.vDts[k - 1])
        assertTrue(g.vDts.last() > (1L shl 33))
        assertEquals(302400L - 126000L, g.vDts.last() - g.vDts[0])
    }

    @Test fun parsesTheSpsOfEveryFixture() {
        fun sps(name: String): AvcSps {
            val (g, _) = run(Fixtures.bytes(name), 1 shl 20)
            var out: AvcSps? = null
            val d = TsDemux(object : TsDemux.Out {
                override fun video(es: ByteArray, len: Int, pts: Long, dts: Long) { if (out == null) Avc.forEachNal(es, 0, len) { a, e -> if ((es[a].toInt() and 0x1F) == 7 && out == null) out = Avc.parseSps(es, a, e - a) } }
                override fun audio(es: ByteArray, len: Int, pts: Long) {}
            })
            val data = Fixtures.bytes(name); d.feed(data, 0, data.size); d.endSegment()
            assertTrue(g.vPts.isNotEmpty())
            assertNotNull(out)
            return out!!
        }
        val a = sps("a_0.ts"); assertEquals(128, a.width); assertEquals(72, a.height); assertEquals(100, a.profile); assertEquals(1, a.chromaFormat); assertEquals(8, a.bitDepthLuma)
        val n = sps("n_0.ts"); assertEquals(96, n.width); assertEquals(54, n.height)
        val ad = sps("ad_0.ts"); assertEquals(64, ad.width); assertEquals(36, ad.height)
    }

    @Test fun splitsNalUnitsAtThreeAndFourByteStartCodes() {
        val b = byteArrayOf(0, 0, 0, 1, 0x09, 0x10, 0, 0, 1, 0x67, 1, 2, 3, 0, 0, 0, 0, 1, 0x65, 9, 9)
        val got = ArrayList<Pair<Int, Int>>()
        Avc.forEachNal(b, 0, b.size) { a, e -> got.add(a to e) }
        assertEquals(listOf(4 to 6, 9 to 13, 18 to 21), got)
        assertTrue(Avc.unescape(byteArrayOf(0, 0, 3, 1, 0, 0, 3, 0, 5), 0, 9).contentEquals(byteArrayOf(0, 0, 1, 0, 0, 0, 5)))
    }

    @Test fun probeAcceptsH264WithAacAndRefusesTheRest() {
        val ok = Fixtures.bytes("a_0.ts")
        assertNull(TsToMp4.probe(ok, 0, ok.size))
        assertNull("a prefix of the segment is enough", TsToMp4.probe(ok, 0, 20_000))
        assertNotNull("no data at all", TsToMp4.probe(ByteArray(0), 0, 0))
        assertNotNull("not a transport stream", TsToMp4.probe(ByteArray(5000) { 0x11 }, 0, 5000))
        // flip the video stream type of the PMT to H.265: refused with a reason
        val hevc = ok.copyOf()
        var i = 0
        while (i < hevc.size) {
            val pid = ((hevc[i + 1].toInt() and 0x1F) shl 8) or (hevc[i + 2].toInt() and 0xFF)
            if (pid == 4096 && (hevc[i + 1].toInt() and 0x40) != 0) { for (k in i + 4 until i + 188) if (hevc[k].toInt() == 0x1B && hevc[k + 1].toInt() == 0xE1.toByte().toInt()) { hevc[k] = 0x24; break }; break }
            i += 188
        }
        val why = TsToMp4.probe(hevc, 0, hevc.size)
        assertNotNull(why); assertTrue(why!!, why.contains("H.264"))
    }
}
