package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FS1 (owner message 43): the screen turns with the tablet while a video is full screen, like in a video player app. */
class FullscreenTurnTest {
    // ---- what a site asks for: screen.orientation.lock(...) arrives as GeckoView's bit set (portrait 1|2, landscape 4|8)
    @Test fun aSiteLockMapsToTheAndroidOrientationItMeans() {
        assertEquals(FullscreenTurn.PORTRAIT, FullscreenTurn.fromSite(1))
        assertEquals(FullscreenTurn.REVERSE_PORTRAIT, FullscreenTurn.fromSite(2))
        assertEquals(FullscreenTurn.SENSOR_PORTRAIT, FullscreenTurn.fromSite(3))
        assertEquals(FullscreenTurn.LANDSCAPE, FullscreenTurn.fromSite(4))
        assertEquals(FullscreenTurn.REVERSE_LANDSCAPE, FullscreenTurn.fromSite(8))
        assertEquals("'landscape' lets the sensor choose between the two landscapes", FullscreenTurn.SENSOR_LANDSCAPE, FullscreenTurn.fromSite(12))
        assertEquals("'any'", FullscreenTurn.FULL_SENSOR, FullscreenTurn.fromSite(15))
    }

    @Test fun aSiteLockWeDoNotUnderstandIsRefused() {
        assertNull(FullscreenTurn.fromSite(0))
        assertNull("only the engine's DEFAULT bit", FullscreenTurn.fromSite(16))
        assertNull("a negative number is not a bit set", FullscreenTurn.fromSite(-1))
    }

    @Test fun aMixedSiteLockMeansAnyOrientation() {
        assertEquals(FullscreenTurn.FULL_SENSOR, FullscreenTurn.fromSite(5)) // portrait-primary + landscape-primary
        assertEquals(FullscreenTurn.FULL_SENSOR, FullscreenTurn.fromSite(14))
    }

    // ---- what to ask for when a page goes full screen
    @Test fun autoTurnsAWideVideoSidewaysWhenTheTabletIsHeldUpright() {
        val p = FullscreenTurn.onEnter(FullscreenTurn.MODE_AUTO, null, 1920, 1080, screenLandscape = false)
        assertEquals(FullscreenTurn.SENSOR_LANDSCAPE, p.request)
        assertTrue("let go again once the tablet really is sideways", p.release)
    }

    @Test fun autoJustFollowsTheTabletWhenItIsAlreadySideways() {
        val p = FullscreenTurn.onEnter(FullscreenTurn.MODE_AUTO, null, 1920, 1080, screenLandscape = true)
        assertEquals(FullscreenTurn.FULL_SENSOR, p.request)
        assertFalse(p.release)
    }

    @Test fun autoLeavesATallVideoUpright() {
        val p = FullscreenTurn.onEnter(FullscreenTurn.MODE_AUTO, null, 1080, 1920, screenLandscape = false)
        assertEquals(FullscreenTurn.FULL_SENSOR, p.request)
        assertFalse(p.release)
    }

    @Test fun autoDoesNotTurnForAnUnknownOrSquareVideo() {
        assertEquals(FullscreenTurn.FULL_SENSOR, FullscreenTurn.onEnter(FullscreenTurn.MODE_AUTO, null, 0, 0, false).request)
        assertEquals(FullscreenTurn.FULL_SENSOR, FullscreenTurn.onEnter(FullscreenTurn.MODE_AUTO, null, 1000, 1000, false).request)
        assertEquals("a 4:3 picture is wide enough", FullscreenTurn.SENSOR_LANDSCAPE, FullscreenTurn.onEnter(FullscreenTurn.MODE_AUTO, null, 640, 480, false).request)
    }

    @Test fun followOnlyNeverForcesTheScreen() {
        val p = FullscreenTurn.onEnter(FullscreenTurn.MODE_FOLLOW, null, 1920, 1080, screenLandscape = false)
        assertEquals(FullscreenTurn.FULL_SENSOR, p.request)
        assertFalse(p.release)
    }

    @Test fun offTouchesNothingUnlessTheSiteAsks() {
        assertEquals(FullscreenTurn.UNSPECIFIED, FullscreenTurn.onEnter(FullscreenTurn.MODE_OFF, null, 1920, 1080, false).request)
        assertEquals(FullscreenTurn.SENSOR_LANDSCAPE, FullscreenTurn.onEnter(FullscreenTurn.MODE_OFF, FullscreenTurn.SENSOR_LANDSCAPE, 1920, 1080, false).request)
    }

    @Test fun aSiteLockBeatsEveryMode() {
        for (m in listOf(FullscreenTurn.MODE_AUTO, FullscreenTurn.MODE_FOLLOW, FullscreenTurn.MODE_OFF)) {
            val p = FullscreenTurn.onEnter(m, FullscreenTurn.PORTRAIT, 1920, 1080, false)
            assertEquals(FullscreenTurn.PORTRAIT, p.request)
            assertFalse("the site owns the lock, nothing to let go", p.release)
        }
    }

    @Test fun anUnknownModeIsTheDefault() {
        assertEquals(FullscreenTurn.MODE_AUTO, FullscreenTurn.cleanMode(-5))
        assertEquals(FullscreenTurn.MODE_AUTO, FullscreenTurn.cleanMode(9))
        assertEquals(FullscreenTurn.MODE_OFF, FullscreenTurn.cleanMode(2))
    }

    // ---- the rotate button
    @Test fun theRotateButtonTurnsToTheOtherDirectionAndLetsGoLater() {
        val toLand = FullscreenTurn.rotateButton(screenLandscape = false)
        assertEquals(FullscreenTurn.SENSOR_LANDSCAPE, toLand.request); assertTrue(toLand.release)
        val toPort = FullscreenTurn.rotateButton(screenLandscape = true)
        assertEquals(FullscreenTurn.SENSOR_PORTRAIT, toPort.request); assertTrue(toPort.release)
    }

    // ---- how the tablet is held (orientation sensor degrees) and when a one-time turn is let go
    @Test fun postureOfATabletWhoseNaturalPositionIsPortrait() {
        assertEquals(false, FullscreenTurn.postureLandscape(0, naturalPortrait = true))
        assertEquals(false, FullscreenTurn.postureLandscape(180, true))
        assertEquals(false, FullscreenTurn.postureLandscape(20, true))
        assertEquals(true, FullscreenTurn.postureLandscape(90, true))
        assertEquals(true, FullscreenTurn.postureLandscape(270, true))
        assertEquals(true, FullscreenTurn.postureLandscape(100, true))
    }

    @Test fun postureOfATabletWhoseNaturalPositionIsLandscapeIsTheOtherWayRound() {
        assertEquals(true, FullscreenTurn.postureLandscape(0, naturalPortrait = false))
        assertEquals(false, FullscreenTurn.postureLandscape(90, naturalPortrait = false))
    }

    @Test fun halfwayAndFlatAreNotAPosture() {
        assertNull(FullscreenTurn.postureLandscape(-1, true))
        assertNull(FullscreenTurn.postureLandscape(45, true))
        assertNull(FullscreenTurn.postureLandscape(135, true))
        assertNull(FullscreenTurn.postureLandscape(315, true))
    }

    @Test fun aTurnToLandscapeIsLetGoWhenTheTabletIsHeldSideways() {
        assertTrue(FullscreenTurn.release(FullscreenTurn.SENSOR_LANDSCAPE, true))
        assertFalse(FullscreenTurn.release(FullscreenTurn.SENSOR_LANDSCAPE, false))
        assertFalse("not sure how it is held: keep the turn", FullscreenTurn.release(FullscreenTurn.SENSOR_LANDSCAPE, null))
    }

    @Test fun aTurnToPortraitIsLetGoWhenTheTabletIsHeldUpright() {
        assertTrue(FullscreenTurn.release(FullscreenTurn.SENSOR_PORTRAIT, false))
        assertFalse(FullscreenTurn.release(FullscreenTurn.SENSOR_PORTRAIT, true))
    }

    @Test fun onlyAOneTimeTurnCanBeLetGo() {
        assertFalse(FullscreenTurn.release(FullscreenTurn.FULL_SENSOR, true))
        assertFalse(FullscreenTurn.release(FullscreenTurn.UNSPECIFIED, false))
    }

    @Test fun theAndroidNumbersAreTheOnesThePlatformUses() {
        // ActivityInfo.SCREEN_ORIENTATION_*: the app module pins these against the real constants (FullscreenTurnerTest); here they are written down once
        assertEquals(-1, FullscreenTurn.UNSPECIFIED); assertEquals(0, FullscreenTurn.LANDSCAPE); assertEquals(1, FullscreenTurn.PORTRAIT)
        assertEquals(6, FullscreenTurn.SENSOR_LANDSCAPE); assertEquals(7, FullscreenTurn.SENSOR_PORTRAIT)
        assertEquals(8, FullscreenTurn.REVERSE_LANDSCAPE); assertEquals(9, FullscreenTurn.REVERSE_PORTRAIT); assertEquals(10, FullscreenTurn.FULL_SENSOR)
    }

    @Test fun namesForDiagnostics() {
        assertEquals("follows the tablet", FullscreenTurn.name(FullscreenTurn.FULL_SENSOR))
        assertEquals("sideways", FullscreenTurn.name(FullscreenTurn.SENSOR_LANDSCAPE))
        assertEquals("upright", FullscreenTurn.name(FullscreenTurn.SENSOR_PORTRAIT))
        assertEquals("as Android had it", FullscreenTurn.name(FullscreenTurn.UNSPECIFIED))
    }
}
