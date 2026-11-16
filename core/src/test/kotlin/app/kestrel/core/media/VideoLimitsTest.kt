package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ANS52g: the text the page-side video format policy reads (the deciding is tested in web-tests/codec.test.js with the same numbers). */
class VideoLimitsTest {
    // the owner's tablet (research/VIDEO_ACCEL_DEVICE.md): H.264 and H.265 2048x1088 at 145 fps at 720p, VP9 2048x1088 at 68 fps at 720p, no hardware AV1
    private val avc = VideoLimits.Limit.of(2048, 1088, 1280, 720, 145.0)!!
    private val vp9 = VideoLimits.Limit.of(2048, 1088, 1280, 720, 68.0)!!
    private val tablet = VideoLimits.Limits(avc, avc, vp9, null)

    @Test fun picturesAreCutUpInWholeBlocks() {
        assertEquals(3600, VideoLimits.blocks(1280, 720))
        assertEquals("1080 lines are 68 rows of blocks, not 67.5", 8160, VideoLimits.blocks(1920, 1080))
        assertEquals(1, VideoLimits.blocks(1, 1))
    }

    @Test fun theTabletsNumbersGiveTheLimitsTheResearchInferred() {
        assertEquals("2048x1088@244800", vp9.encode())
        assertEquals("VP9 is 1080p30 exactly", 30, vp9.fps1080())
        assertEquals("2048x1088@522000", avc.encode())
        assertEquals("H.264 level 4.2: 1080p about 64", 63, avc.fps1080())
    }

    @Test fun aLimitComesFromAnySizeOrder() {
        val a = VideoLimits.Limit.of(1088, 2048, 1280, 720, 68.0)!!
        assertEquals(2048, a.longSide); assertEquals(1088, a.shortSide)
        // measured at 1080p instead of 720p: the same macroblock rate
        assertEquals(244800, VideoLimits.Limit.of(2048, 1088, 1920, 1080, 30.0)!!.blocksPerSecond)
    }

    @Test fun numbersThatCannotBeRightGiveNoLimit() {
        assertNull(VideoLimits.Limit.of(0, 1088, 1280, 720, 68.0))
        assertNull(VideoLimits.Limit.of(2048, 1088, 0, 720, 68.0))
        assertNull(VideoLimits.Limit.of(2048, 1088, 1280, 720, 0.0))
        assertNull(VideoLimits.Limit.of(2048, 1088, 1280, 720, -3.0))
        assertNull(VideoLimits.Limit.of(2048, 1088, 1280, 720, Double.NaN))
        assertNull(VideoLimits.Limit.of(2048, 1088, 1280, 720, 1e9))
    }

    @Test fun aVendorThatDeclaresAHugeRateCannotOverflowTheText() {
        val l = VideoLimits.Limit.of(2048, 1088, 1280, 720, 99999.0)!!   // VP8 declared 960; 99999 is the most the rule believes
        assertTrue(l.blocksPerSecond <= 900_000_000)
        assertTrue(l.encode(), l.encode().length <= 24)
    }

    @Test fun twoHardwareDecodersOfOneCodecGiveTheBetterOfBoth() {
        val big = VideoLimits.Limit(4096, 2304, 600000)
        val m = vp9.max(big)
        assertEquals(4096, m.longSide); assertEquals(2304, m.shortSide); assertEquals(600000, m.blocksPerSecond)
        assertEquals(vp9.encode(), vp9.max(vp9).encode())
    }

    @Test fun limitsRoundTripThroughTheStoredText() {
        val s = tablet.encode()
        assertEquals("2048x1088@522000;2048x1088@522000;2048x1088@244800;-", s)
        val back = VideoLimits.Limits.decode(s)!!
        assertEquals(s, back.encode())
        assertNull(back.av1)
        assertEquals(30, back.vp9!!.fps1080())
        for (bad in listOf(null, "", "-", "-;-;-", "-;-;-;-;-", "1x1@1;-;-;x", "0x0@0;-;-;-", "99999999x1@1;-;-;-")) assertNull("should not read: $bad", VideoLimits.Limits.decode(bad))
        assertNotNull(VideoLimits.Limits.decode("-;-;-;-"))
    }

    @Test fun thePolicyTextIsWhatThePageScriptReads() {
        assertEquals("00|2048x1088@522000|2048x1088@522000|2048x1088@244800|-", VideoLimits.policy(VideoLimits.AUTO, false, tablet))
        assertEquals("10|2048x1088@522000|2048x1088@522000|2048x1088@244800|-", VideoLimits.policy(VideoLimits.H264_ONLY, false, tablet))
        assertEquals("21|2048x1088@522000|2048x1088@522000|2048x1088@244800|-", VideoLimits.policy(VideoLimits.EVERYTHING, true, tablet))
        // not read yet: no limit for any codec (the engine answers), the modes that need none still work
        assertEquals("00|-|-|-|-", VideoLimits.policy(VideoLimits.AUTO, false, null))
        assertEquals("11|-|-|-|-", VideoLimits.policy(VideoLimits.H264_ONLY, true, null))
        // a setting that is out of range is Automatic
        assertEquals("00|-|-|-|-", VideoLimits.policy(7, false, null))
        assertEquals("00|-|-|-|-", VideoLimits.policy(-1, false, null))
    }

    @Test fun everyTextTheAppCanSendFitsTheScriptsGrammarAndItsLengthLimit() {
        val grammar = Regex("""[012][01](\|(-|\d{1,5}x\d{1,5}@\d{1,9})){4}""") // the same shape codec-main.js parses; it refuses anything over 160 characters
        for (m in -1..3) for (f in listOf(false, true)) for (l in listOf(null, tablet, VideoLimits.Limits(null, null, null, null), VideoLimits.Limits(VideoLimits.Limit(99999, 99999, 900_000_000), null, null, null))) {
            val t = VideoLimits.policy(m, f, l)
            assertTrue(t, grammar.matches(t))
            assertTrue(t, t.length <= 160)
        }
    }

    @Test fun diagnosticsWordsSayWhatTheSettingDoesOnThisDevice() {
        val auto = VideoLimits.explain(VideoLimits.AUTO, false, tablet)
        assertTrue(auto, auto.startsWith("Automatic"))
        assertTrue(auto, "VP9 up to 2048x1088, 1080p 30 fps" in auto)
        assertTrue(auto, "AV1 none" in auto)
        assertTrue(auto, "frame rate not capped" in auto)
        val h = VideoLimits.explain(VideoLimits.H264_ONLY, true, null)
        assertTrue(h, h.startsWith("H.264 only"))
        assertTrue(h, "hardware limits not read yet" in h && "above 30 are answered no" in h)
        assertTrue("no codec is answered no" in VideoLimits.explain(VideoLimits.EVERYTHING, false, tablet))
        assertTrue("the limits mean nothing in the mode that refuses no codec", "1080p" !in VideoLimits.explain(VideoLimits.EVERYTHING, false, tablet))
    }
}
