package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ANS52h: the cap, in pixels of the longer side, the extension's hlscap.js works with (web-tests/hlscap.test.js uses the same numbers). */
class PictureCapTest {
    @Test fun automaticKeeps720pOnTheTabletAndDrops1080p() {
        for (screen in listOf(973, 1024)) {
            val cap = PictureCap.pixels(PictureCap.AUTO, screen)
            assertTrue("a 720p rung (1280 px) must stay on a $screen px screen, cap $cap", 1280 <= cap * 1.02)
            assertTrue("a 1080p rung (1920 px) must go on a $screen px screen, cap $cap", 1920 > cap * 1.02)
        }
        assertEquals(1314, PictureCap.pixels(PictureCap.AUTO, 973))
    }

    @Test fun theNamedCapsAreTheirOwnWidths() {
        assertEquals(0, PictureCap.pixels(PictureCap.OFF, 973))
        assertEquals(854, PictureCap.pixels(PictureCap.P480, 973))
        assertEquals(1280, PictureCap.pixels(PictureCap.P720, 973))
        assertEquals(1920, PictureCap.pixels(PictureCap.P1080, 973))
        assertEquals("a setting out of range is Automatic", PictureCap.pixels(PictureCap.AUTO, 973), PictureCap.pixels(9, 973))
        assertEquals(PictureCap.pixels(PictureCap.AUTO, 973), PictureCap.pixels(-3, 973))
    }

    @Test fun aBigScreenCapsNothingAndAnUnknownScreenCapsNothing() {
        assertEquals("a phone's 2400 px: 3240 px, 4K rungs (3840) still go", 3240, PictureCap.pixels(PictureCap.AUTO, 2400))
        assertEquals("a 4K-class display: no cap", 0, PictureCap.pixels(PictureCap.AUTO, 3840))
        assertEquals(0, PictureCap.pixels(PictureCap.AUTO, 0))
        assertEquals(0, PictureCap.pixels(PictureCap.AUTO, -1))
    }

    @Test fun aTinyScreenNeverCapsBelow360p() {
        assertEquals(640, PictureCap.pixels(PictureCap.AUTO, 320))
    }

    @Test fun theTextAnswersWhatTheCapDoes() {
        assertTrue(PictureCap.explain(PictureCap.AUTO, 973).contains("above 1314 px"))
        assertTrue(PictureCap.explain(PictureCap.OFF, 973).contains("no rung is taken out"))
        assertTrue(PictureCap.explain(PictureCap.P720, 973).startsWith("720p"))
    }

    @Test fun theNumberTheExtensionGetsIsInItsOwnRange() {
        // background.js ignores anything outside 320..20000 and treats it as "off"
        for (mode in -1..5) for (screen in listOf(0, 320, 600, 973, 1024, 2400, 3840, 7680)) {
            val px = PictureCap.pixels(mode, screen)
            assertTrue("mode $mode screen $screen gave $px", px == 0 || px in 320..20000)
        }
    }

    @Test fun theLongSideOfA16By9PictureIsRoundedUpSoTheUsual854By480RungFitsThe480Cap() {
        // RVW3 PB5: capH * 16 / 9 gave 853 for 480 and left the 854 x 480 rung out
        assertEquals(854, PictureCap.longSide(480))
        assertEquals(PictureCap.pixels(PictureCap.P480, 0), PictureCap.longSide(480))
        assertEquals(1280, PictureCap.pixels(PictureCap.P720, 0)); assertEquals(1280, PictureCap.longSide(720))
        assertEquals(1920, PictureCap.longSide(1080)); assertEquals(640, PictureCap.longSide(360)); assertEquals(960, PictureCap.longSide(540))
        assertEquals(256, PictureCap.longSide(144)); assertEquals(427, PictureCap.longSide(240)); assertEquals(2560, PictureCap.longSide(1440)); assertEquals(3840, PictureCap.longSide(2160))
        // the rungs real ladders use at 480 lines, and the ones at 360 that must NOT slip under a 360 cap
        for (w in listOf(842, 848, 852, 853, 854)) org.junit.Assert.assertTrue("a ${w}x480 rung fits the 480 cap", w <= PictureCap.longSide(480))
        org.junit.Assert.assertTrue("a 855 wide rung does not", 855 > PictureCap.longSide(480))
        for (h in 144..2160 step 12) org.junit.Assert.assertTrue("never below the exact 16:9 width at $h", PictureCap.longSide(h) * 9 >= h * 16 && (PictureCap.longSide(h) - 1) * 9 < h * 16)
    }
}
