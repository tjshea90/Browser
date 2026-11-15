package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ROT1 (owner message 46): the video is paused and the page area covered while the screen turns, and let go once it is still. */
class TurnSettleTest {
    @Test fun onlyARealTurnCounts() {
        assertTrue(TurnSettle.isTurn(wasLandscape = false, nowLandscape = true))
        assertTrue(TurnSettle.isTurn(wasLandscape = true, nowLandscape = false))
        assertFalse("a configuration change that kept the orientation (keyboard, locale, dark mode)", TurnSettle.isTurn(true, true))
        assertFalse(TurnSettle.isTurn(false, false))
    }

    @Test fun aTurnMattersInFullScreenOrWhileAVideoPlays() {
        assertTrue(TurnSettle.worthIt(turn = true, fullscreen = true, videoPlaying = false))
        assertTrue(TurnSettle.worthIt(turn = true, fullscreen = false, videoPlaying = true))
        assertFalse("an ordinary page keeps re-laying out as before", TurnSettle.worthIt(turn = true, fullscreen = false, videoPlaying = false))
        assertFalse("no turn, nothing to do", TurnSettle.worthIt(turn = false, fullscreen = true, videoPlaying = true))
    }

    @Test fun theVideoIsPausedOnlyWhenItPlaysAndTheSessionIsKnown() {
        assertTrue(TurnSettle.pauseIt(videoPlaying = true, haveSession = true))
        assertFalse(TurnSettle.pauseIt(videoPlaying = false, haveSession = true))
        assertFalse("a silent video opens no media session: nothing to talk to", TurnSettle.pauseIt(videoPlaying = true, haveSession = false))
    }

    @Test fun itPlaysAgainOnlyWhenWePausedItAndNobodyTookOver() {
        assertTrue(TurnSettle.resumeIt(pausedByUs = true, playingNow = false, sameTabInFront = true, appOnScreen = true, touchedSince = false))
        assertFalse("a video the person had paused stays paused", TurnSettle.resumeIt(pausedByUs = false, playingNow = false, sameTabInFront = true, appOnScreen = true, touchedSince = false))
        assertFalse("it plays already (the person pressed play)", TurnSettle.resumeIt(true, playingNow = true, sameTabInFront = true, appOnScreen = true, touchedSince = false))
        assertFalse("another tab came to the front", TurnSettle.resumeIt(true, false, sameTabInFront = false, appOnScreen = true, touchedSince = false))
        assertFalse("the app was left meanwhile: no sound behind another app (owner message 45)", TurnSettle.resumeIt(true, false, true, appOnScreen = false, touchedSince = false))
        assertFalse("a touch during the turn is the person taking over", TurnSettle.resumeIt(true, false, true, true, touchedSince = true))
    }

    @Test fun theTurnIsOverWhenTheAreaIsStillOrAtTheCap() {
        assertFalse(TurnSettle.settled(sinceLastChangeMs = 100, sinceStartMs = 300))
        assertFalse(TurnSettle.settled(sinceLastChangeMs = TurnSettle.SETTLE_MS - 1, sinceStartMs = 900))
        assertTrue(TurnSettle.settled(sinceLastChangeMs = TurnSettle.SETTLE_MS, sinceStartMs = 900))
        assertTrue("a page that never stops resizing is let go at the cap", TurnSettle.settled(sinceLastChangeMs = 10, sinceStartMs = TurnSettle.CAP_MS))
    }

    // ROT2 (owner message 48): the page has to say it has the new size before the cover goes
    @Test fun aPageThatReportsItsViewportKeepsTheTurnGoingUntilItHasTheNewSize() {
        assertFalse("still for the settle time, but the page has not said it has the new size", TurnSettle.settled(TurnSettle.SETTLE_MS, 800, expectsPage = true, pageConfirmed = false))
        assertTrue(TurnSettle.settled(TurnSettle.SETTLE_MS, 800, expectsPage = true, pageConfirmed = true))
        assertFalse("the page said so but the view was not still yet", TurnSettle.settled(TurnSettle.SETTLE_MS - 1, 800, expectsPage = true, pageConfirmed = true))
        assertFalse("the old cap does not end the wait for such a page", TurnSettle.settled(0, TurnSettle.CAP_MS + 1, expectsPage = true, pageConfirmed = false))
        assertTrue("the page cap does", TurnSettle.settled(0, TurnSettle.PAGE_CAP_MS, expectsPage = true, pageConfirmed = false))
        assertTrue("a page that does not report is judged by the old rule: still", TurnSettle.settled(TurnSettle.SETTLE_MS, 800, expectsPage = false, pageConfirmed = false))
        assertTrue("... and by the old cap", TurnSettle.settled(0, TurnSettle.CAP_MS, expectsPage = false, pageConfirmed = false))
    }

    @Test fun theEngineIsNudgedWhenThePageTakesLongAndTheRingShowsAfterAboutASecond() {
        assertFalse(TurnSettle.nudgeDue(TurnSettle.NUDGE_AFTER_MS - 1)); assertTrue(TurnSettle.nudgeDue(TurnSettle.NUDGE_AFTER_MS))
        assertFalse(TurnSettle.hintDue(TurnSettle.HINT_AFTER_MS - 1)); assertTrue(TurnSettle.hintDue(TurnSettle.HINT_AFTER_MS))
        assertTrue("the ring comes before the nudge, the nudge before the cap", TurnSettle.HINT_AFTER_MS < TurnSettle.NUDGE_AFTER_MS && TurnSettle.NUDGE_AFTER_MS < TurnSettle.PAGE_CAP_MS)
        assertTrue("the wait for the page is longer than the old cap", TurnSettle.PAGE_CAP_MS > TurnSettle.CAP_MS)
    }

    @Test fun theNumbersAreSane() {
        assertTrue("the cap must be longer than the settle time", TurnSettle.CAP_MS > TurnSettle.SETTLE_MS + 2 * TurnSettle.TICK_MS)
        assertEquals(160L, TurnSettle.FADE_MS)
    }
}
