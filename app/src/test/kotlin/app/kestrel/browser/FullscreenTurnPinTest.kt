package app.kestrel.browser

import android.content.pm.ActivityInfo
import app.kestrel.core.media.FullscreenTurn
import org.junit.Assert.assertEquals
import org.junit.Test

/** FS1: core has no Android imports, so it writes Android's orientation numbers down; this is where they are held to the platform's own constants. */
class FullscreenTurnPinTest {
    @Test fun theNumbersInCoreAreTheOnesAndroidUses() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, FullscreenTurn.UNSPECIFIED)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, FullscreenTurn.LANDSCAPE)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, FullscreenTurn.PORTRAIT)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, FullscreenTurn.SENSOR_LANDSCAPE)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, FullscreenTurn.SENSOR_PORTRAIT)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, FullscreenTurn.REVERSE_LANDSCAPE)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, FullscreenTurn.REVERSE_PORTRAIT)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR, FullscreenTurn.FULL_SENSOR)
    }
}
