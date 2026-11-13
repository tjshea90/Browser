package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPolicyTest {
    private val tablet = VideoPolicy.Hardware(avc = true, hevc = true, vp9 = false, av1 = false) // what a MediaTek tablet of this class usually reports (the probe, not this test, is the truth)

    @Test fun autoTakesVp9AndAv1AwayWhenTheyWouldRunInSoftware() {
        val d = VideoPolicy.decide(VideoPolicy.AUTO, tablet)
        assertEquals(false, d.vp9)
        assertEquals(false, d.av1)
        assertNull("hardware H.265 stays as the engine decides", d.hevc)
    }

    @Test fun autoLeavesVp9AloneWhenTheHardwareDecodesIt() {
        val d = VideoPolicy.decide(VideoPolicy.AUTO, VideoPolicy.Hardware(avc = true, hevc = true, vp9 = true, av1 = false))
        assertNull(d.vp9)
    }

    @Test fun autoKeepsVp9WhenThereIsNoHardwareH264EitherBecauseBothAreSoftware() {
        val d = VideoPolicy.decide(VideoPolicy.AUTO, VideoPolicy.Hardware(avc = false, hevc = false, vp9 = false, av1 = false))
        assertNull(d.vp9)
        assertEquals(false, d.hevc)
    }

    @Test fun autoTurnsAv1OnOnlyWithAHardwareDecoder() {
        assertEquals(true, VideoPolicy.decide(VideoPolicy.AUTO, VideoPolicy.Hardware(true, true, true, true)).av1)
        assertEquals(false, VideoPolicy.decide(VideoPolicy.AUTO, tablet).av1)
    }

    @Test fun anUnknownDeviceKeepsTheEngineDefaultsExceptAv1() {
        val d = VideoPolicy.decide(VideoPolicy.AUTO, null)
        assertNull(d.vp9); assertNull(d.hevc); assertEquals(false, d.av1)
    }

    @Test fun h264OnlyAndEverythingIgnoreTheHardware() {
        val a = VideoPolicy.decide(VideoPolicy.H264_ONLY, VideoPolicy.Hardware(true, true, true, true))
        assertEquals(false, a.vp9); assertEquals(false, a.av1); assertEquals(false, a.hevc)
        val b = VideoPolicy.decide(VideoPolicy.EVERYTHING, tablet)
        assertEquals(true, b.vp9); assertEquals(true, b.av1); assertEquals(true, b.hevc)
    }

    @Test fun aNonsenseModeIsAuto() {
        assertEquals(VideoPolicy.decide(VideoPolicy.AUTO, tablet).why, VideoPolicy.decide(99, tablet).why)
    }

    @Test fun theStoredFormRoundTripsAndRejectsJunk() {
        for (a in listOf(true, false)) for (h in listOf(true, false)) for (v in listOf(true, false)) for (i in listOf(true, false)) {
            val hw = VideoPolicy.Hardware(a, h, v, i)
            val back = VideoPolicy.Hardware.decode(hw.encode())
            assertNotNull(back)
            assertEquals(hw.avc, back!!.avc); assertEquals(hw.hevc, back.hevc); assertEquals(hw.vp9, back.vp9); assertEquals(hw.av1, back.av1)
        }
        assertNull(VideoPolicy.Hardware.decode(null)); assertNull(VideoPolicy.Hardware.decode("")); assertNull(VideoPolicy.Hardware.decode("AHV")); assertNull(VideoPolicy.Hardware.decode("AHVX"))
        assertTrue(VideoPolicy.decide(VideoPolicy.AUTO, tablet).why.isNotEmpty()); assertFalse(VideoPolicy.decide(VideoPolicy.AUTO, null).why.isEmpty())
    }
}
