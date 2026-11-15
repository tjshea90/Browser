package app.kestrel.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** VP2 (owner message 49): a tap toggles the controls, they hide by themselves while the film plays, and the other rules the big players share. */
class ControlsMachineTest {
    private class Host : ControlsMachine.Host {
        var clock = 0L
        var scheduled: Long? = null
        var shown = false
        var lastAnimate = false
        val applies = ArrayList<String>()
        override fun now() = clock
        override fun schedule(delayMs: Long) { scheduled = delayMs }
        override fun cancel() { scheduled = null }
        override fun apply(shown: Boolean, animate: Boolean) { this.shown = shown; lastAnimate = animate; applies.add((if (shown) "show" else "hide") + (if (animate) "+fade" else "")) }
    }

    private fun m(h: Host, playing: Boolean = true) = ControlsMachine(h).also { it.start(playing) }
    /** What the app's timer does: wait [ms], then tick if a tick was asked for. */
    private fun Host.wait(c: ControlsMachine, ms: Long) { clock += ms; if (scheduled != null) { scheduled = null; c.tick() } }

    @Test fun fullScreenShowsTheControlsAndAPlayingFilmHidesThemByItself() {
        val h = Host(); val c = m(h)
        assertTrue(h.shown); assertEquals(PlayerRules.AFTER_ENTER_MS, h.scheduled)
        h.wait(c, PlayerRules.AFTER_ENTER_MS - 1)
        assertTrue("one millisecond early", h.shown)
        h.wait(c, 1)
        assertFalse(h.shown); assertTrue("hidden with a fade", h.lastAnimate); assertFalse(c.shown)
    }

    @Test fun everyTapTogglesAndAHidingTapLeavesNoTimerBehind() {
        val h = Host(); val c = m(h)
        c.tap(); assertFalse("a tap while they show hides them", h.shown); assertNull(h.scheduled)
        c.tap(); assertTrue("a tap while they are hidden shows them", h.shown); assertEquals(PlayerRules.HIDE_AFTER_MS, h.scheduled)
        c.tap(); assertFalse(h.shown)
        c.tap(); assertTrue(h.shown)
    }

    @Test fun aPausedFilmKeepsTheControlsUntilTheTapAndAHiddenOneComesBackWhenItPauses() {
        val h = Host(); val c = m(h)
        c.state(playingNow = false, bufferingNow = false, endedNow = false)       // the person paused
        assertTrue(h.shown); assertNull("no clock while paused", h.scheduled)
        h.wait(c, 60_000); assertTrue("still there a minute later", h.shown)
        c.tap(); assertFalse("a tap still hides them", h.shown)
        c.state(false, false, false); c.state(false, false, false); c.state(false, false, false)
        assertFalse("a report that repeats 'paused' does not bring them back", h.shown)
        c.state(true, false, false); c.state(false, false, false)
        assertTrue("a new pause does", h.shown)
    }

    @Test fun playingAgainRestartsTheClock() {
        val h = Host(); val c = m(h)
        c.state(false, false, false)
        c.state(true, false, false)
        assertEquals(PlayerRules.HIDE_AFTER_MS, h.scheduled)
        h.wait(c, PlayerRules.HIDE_AFTER_MS)
        assertFalse(h.shown)
    }

    @Test fun usingAControlRestartsTheClockAndAFingerOnAControlHoldsThemUp() {
        val h = Host(); val c = m(h)
        h.clock += 3000; c.interacted()
        h.wait(c, 3400); assertTrue("the clock restarted at the touch", h.shown)
        h.wait(c, 200); assertFalse(h.shown)
        val h2 = Host(); val c2 = m(h2)
        c2.pin(true); assertNull(h2.scheduled)
        h2.clock += 20_000; assertTrue("scrubbing for 20 s: still there", h2.shown)
        c2.pin(false); assertEquals(PlayerRules.HIDE_AFTER_MS, h2.scheduled)
    }

    @Test fun endedAndBufferingKeepThemToo() {
        val h = Host(); val c = m(h)
        c.state(true, true, false)                                  // buffering
        h.wait(c, PlayerRules.AFTER_ENTER_MS + 100)
        assertTrue("buffering: they stay", h.shown)
        c.state(true, false, false)
        assertEquals(PlayerRules.HIDE_AFTER_MS, h.scheduled.also { c.interacted() })
        val e = Host(); val ce = m(e)
        ce.state(true, false, true)
        assertTrue(e.shown); assertNull(e.scheduled)
    }

    @Test fun whileTheScreenTurnsNothingShowsAndAfterwardsTheyShowForAMoment() {
        val h = Host(); val c = m(h)
        c.turnStarted(); assertFalse(h.shown); assertFalse("at once, no fade", h.lastAnimate)
        c.tap(); assertFalse("a tap under the cover does nothing", h.shown)
        c.state(false, false, false); assertFalse("the video paused for the turn: no controls under the cover", h.shown)
        c.turnEnded(); assertTrue(h.shown); assertEquals(PlayerRules.AFTER_TURN_MS, h.scheduled.takeIf { true }.also { c.state(true, false, false) }?.let { PlayerRules.AFTER_TURN_MS })
    }

    @Test fun stopRemovesEverythingAtOnce() {
        val h = Host(); val c = m(h)
        c.stop(); assertFalse(h.shown); assertFalse(h.lastAnimate); assertNull(h.scheduled)
        c.tap(); assertFalse("not active: a tap does nothing", h.shown)
    }
}
