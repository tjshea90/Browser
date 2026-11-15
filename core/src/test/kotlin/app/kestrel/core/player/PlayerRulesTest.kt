package app.kestrel.core.player

import app.kestrel.core.player.PlayerRules.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** VP1 (owner message 49): the numbers and the small pure rules of Kestrel's full-screen video controls. */
class PlayerRulesTest {
    @Test fun timesReadLikeAPlayer() {
        assertEquals("0:00", PlayerRules.formatTime(0.0))
        assertEquals("0:07", PlayerRules.formatTime(7.9))
        assertEquals("1:05", PlayerRules.formatTime(65.0))
        assertEquals("59:59", PlayerRules.formatTime(3599.9))
        assertEquals("1:00:00", PlayerRules.formatTime(3600.0))
        assertEquals("1:02:03", PlayerRules.formatTime(3723.0))
        assertEquals("a time that makes no sense reads 0:00", "0:00", PlayerRules.formatTime(-5.0))
        assertEquals("0:00", PlayerRules.formatTime(Double.NaN))
        assertEquals("0:00", PlayerRules.formatTime(Double.POSITIVE_INFINITY))
    }

    @Test fun theLeftThirdAndTheRightThirdSeekTheMiddlePlaysAndPauses() {
        assertEquals(Zone.LEFT, PlayerRules.zone(0.0)); assertEquals(Zone.LEFT, PlayerRules.zone(0.34))
        assertEquals(Zone.CENTER, PlayerRules.zone(0.35)); assertEquals(Zone.CENTER, PlayerRules.zone(0.5)); assertEquals(Zone.CENTER, PlayerRules.zone(0.65))
        assertEquals(Zone.RIGHT, PlayerRules.zone(0.66)); assertEquals(Zone.RIGHT, PlayerRules.zone(1.0))
    }

    @Test fun aSkipNeverLeavesTheFilm() {
        assertEquals(20.0, PlayerRules.skipTarget(10.0, 10.0, 65.0), 0.0)
        assertEquals("not before the start", 0.0, PlayerRules.skipTarget(4.0, -10.0, 65.0), 0.0)
        assertEquals("not past a known end", 65.0, PlayerRules.skipTarget(60.0, 10.0, 65.0), 0.0)
        assertEquals("an unknown length is bounded by what can be sought", 33.0, PlayerRules.skipTarget(30.0, 10.0, null, 33.0), 0.0)
        assertEquals("no bound at all: just forward", 40.0, PlayerRules.skipTarget(30.0, 10.0, null, null), 0.0)
    }

    @Test fun theTrackMapsBothWays() {
        assertEquals(0.5, PlayerRules.fraction(30.0, 60.0), 1e-9)
        assertEquals("no length: the track is empty", 0.0, PlayerRules.fraction(30.0, 0.0), 0.0)
        assertEquals("past the end is the end", 1.0, PlayerRules.fraction(90.0, 60.0), 0.0)
        assertEquals(15.0, PlayerRules.timeAt(0.25, 60.0), 1e-9)
        assertEquals("a finger past the end of the track", 60.0, PlayerRules.timeAt(1.7, 60.0), 0.0)
    }

    @Test fun theClocksAreTheOnesTheBigPlayersUse() {
        assertTrue("hides after 3 to 5 seconds while playing", PlayerRules.HIDE_AFTER_MS in 3000L..5000L)
        assertTrue("a double tap window of about a quarter second", PlayerRules.DOUBLE_TAP_MS in 200L..350L)
        assertTrue("a burst lasts longer than the double tap window", PlayerRules.BURST_MS > PlayerRules.DOUBLE_TAP_MS)
        assertEquals(10.0, PlayerRules.SKIP_S, 0.0)
        assertTrue("the middle zone is wide enough for a thumb", 1.0 - 2 * PlayerRules.SIDE > 0.25)
    }
}
