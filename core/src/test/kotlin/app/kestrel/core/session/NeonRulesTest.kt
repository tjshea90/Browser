package app.kestrel.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-S1 (research/REVIEW-EXTERNAL-3.md section 9): the neon glow holds still while the screen is busy with something else, now including a playing video. */
class NeonRulesTest {
    @Test fun theGlowHoldsStillWhenAnyOfTheBusyThingsIsTrue() {
        assertFalse(NeonRules.busy(heavyLoad = false, keyboardUp = false, keyboardComing = false, videoPlaying = false))
        assertTrue(NeonRules.busy(heavyLoad = true, keyboardUp = false, keyboardComing = false, videoPlaying = false))
        assertTrue(NeonRules.busy(heavyLoad = false, keyboardUp = true, keyboardComing = false, videoPlaying = false))
        assertTrue(NeonRules.busy(heavyLoad = false, keyboardUp = false, keyboardComing = true, videoPlaying = false))
        assertTrue(NeonRules.busy(heavyLoad = false, keyboardUp = false, keyboardComing = false, videoPlaying = true))
        assertTrue(NeonRules.busy(heavyLoad = true, keyboardUp = true, keyboardComing = true, videoPlaying = true))
    }

    @Test fun aVideoPlaysFromPlayUntilPauseStopOrTheEndOfItsSession() {
        var s = false
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.ACTIVATED); assertFalse("activated alone is not playing: a video that was blocked from starting must not freeze the glow", s)
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.PLAY); assertTrue(s)
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.PAUSE); assertFalse(s)
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.PLAY); assertTrue(s)
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.STOP); assertFalse(s)
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.PLAY); assertTrue(s)
        s = NeonRules.videoAfter(s, NeonRules.MediaEvent.DEACTIVATED); assertFalse(s)
        assertEquals(false, NeonRules.videoAfter(false, NeonRules.MediaEvent.PAUSE))
    }
}
