package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ROT1 (owner message 46; alpha49 review: the clock and the state of a turn had no test): the turn state machine with a fake host and a fake clock. */
class TurnSettleMachineTest {
    private class Host : TurnSettleMachine.Host<String> {
        var clock = 0L
        var scheduled: Long? = null
        var coverShown = false
        var coverFaded = false
        var fullscreen = false
        var playing = false
        var hasSession = true
        var pauseResult: String? = "tab1"
        var front = "tab1"
        var onScreen = true
        val paused = ArrayList<String>()
        val resumed = ArrayList<String>()
        val logs = ArrayList<String>()
        override fun now() = clock
        override fun schedule(delayMs: Long) { scheduled = delayMs }
        override fun cancelSchedule() { scheduled = null }
        override fun cover(show: Boolean, fade: Boolean) { coverShown = show; coverFaded = fade }
        override fun fullscreen() = fullscreen
        override fun frontPlaying() = playing
        override fun frontHasSession() = hasSession
        override fun pauseFront(): String? { val r = pauseResult; if (r != null) { paused.add(r); playing = false } /* the engine's media session answers "paused" */; return r }
        override fun playingNow(handle: String) = playing
        override fun isFront(handle: String) = front == handle
        override fun onScreen() = onScreen
        override fun resume(handle: String): Boolean { resumed.add(handle); playing = true; return true }
        override fun log(event: String, text: String) { logs.add("$event: $text") }
    }

    private fun machine(h: Host, landscape: Boolean = false) = TurnSettleMachine(h).also { it.init(landscape) }

    /** What the app's timer does: wait [ms], then call tick() if a tick was asked for. */
    private fun Host.wait(m: TurnSettleMachine<String>, ms: Long) { clock += ms; if (scheduled != null) m.tick() }

    @Test fun theFirstConfigurationOnlySaysWhereTheScreenIs() {
        val h = Host().apply { fullscreen = true; playing = true }
        val m = TurnSettleMachine(h)            // never told: no init
        m.configChanged(true)
        assertEquals(0, m.turns)
        assertFalse(h.coverShown)
        m.configChanged(false)                  // now it knows it was landscape: a real turn
        assertEquals(1, m.turns)
        assertTrue(h.coverShown)
    }

    @Test fun anOrdinaryPageIsLeftAlone() {
        val h = Host()                          // not full screen, nothing plays
        val m = machine(h)
        m.configChanged(true)
        assertEquals(0, m.turns); assertFalse(h.coverShown); assertTrue(h.paused.isEmpty()); assertNull(h.scheduled)
        h.playing = true; m.configChanged(true) // same orientation again (a keyboard, a locale): not a turn
        assertEquals(0, m.turns); assertFalse(h.coverShown)
    }

    @Test fun aTurnUnderAPlayingVideoPausesCoversAndPlaysAgainWhenStill() {
        val h = Host().apply { playing = true }
        val m = machine(h)
        m.configChanged(true)
        assertTrue(h.coverShown); assertFalse(h.coverFaded)
        assertEquals(listOf("tab1"), h.paused)
        assertEquals(TurnSettle.TICK_MS, h.scheduled)
        h.wait(m, 300); m.sizeChanged()                      // the page area is still being laid out
        h.wait(m, TurnSettle.SETTLE_MS - 1)
        assertTrue("one millisecond short of still", h.coverShown); assertTrue(h.resumed.isEmpty())
        h.wait(m, 1)
        assertFalse(h.coverShown); assertTrue("the cover fades out", h.coverFaded)
        assertEquals(listOf("tab1"), h.resumed)
        assertEquals(1, m.resumedCount); assertEquals(1, m.pausedCount); assertTrue(m.last.contains("played again"))
        assertFalse(m.turning)
    }

    @Test fun aSecondTurnWhileOneGoesOnExtendsItAndPausesOnce() {
        val h = Host().apply { playing = true }
        val m = machine(h)
        m.configChanged(true)
        h.wait(m, 300)
        m.configChanged(false)                               // turned back before it settled
        assertEquals("paused once", listOf("tab1"), h.paused)
        assertEquals(2, m.turns)
        h.wait(m, TurnSettle.SETTLE_MS - 1)
        assertTrue(h.coverShown)
        h.wait(m, 1)
        assertFalse(h.coverShown)
        assertEquals("played once", listOf("tab1"), h.resumed)
    }

    @Test fun aPageThatNeverStopsResizingIsLetGoAtTheCap() {
        val h = Host().apply { fullscreen = true; playing = true }
        val m = machine(h)
        m.configChanged(true)
        var t = 0L
        while (m.turning && t < 10_000) { h.wait(m, 100); t += 100; if (m.turning) m.sizeChanged() }
        assertTrue("let go at the cap, not before: " + m.lastMs, m.lastMs >= TurnSettle.CAP_MS && m.lastMs < TurnSettle.CAP_MS + TurnSettle.TICK_MS)
        assertFalse(h.coverShown)
    }

    @Test fun aTouchDuringTheTurnLeavesTheVideoPaused() {
        val h = Host().apply { playing = true }
        val m = machine(h)
        m.configChanged(true)
        m.touchedScreen()
        h.wait(m, TurnSettle.SETTLE_MS)
        assertTrue(h.resumed.isEmpty())
        assertEquals(1, m.skippedResume)
        assertFalse("the cover still goes", h.coverShown)
        assertTrue(m.last.contains("left paused"))
        // the next turn starts clean: a touch long before it does not count
        h.playing = true
        m.configChanged(false)
        h.wait(m, TurnSettle.SETTLE_MS)
        assertEquals(listOf("tab1"), h.resumed)
    }

    @Test fun aVideoThePersonStartedAgainMeanwhileIsNotStartedTwice() {
        val h = Host().apply { playing = true }
        val m = machine(h)
        m.configChanged(true)
        h.playing = true                                     // the person pressed play on the page's own button
        h.wait(m, TurnSettle.SETTLE_MS)
        assertTrue(h.resumed.isEmpty())
        assertEquals(1, m.skippedResume)
    }

    @Test fun anotherTabInFrontOrTheAppAwayMeansNothingIsPlayed() {
        var h = Host().apply { playing = true }
        var m = machine(h)
        m.configChanged(true); h.front = "tab2"; h.wait(m, TurnSettle.SETTLE_MS)
        assertTrue("another tab came to the front", h.resumed.isEmpty())
        h = Host().apply { playing = true }
        m = machine(h)
        m.configChanged(true); h.onScreen = false; h.wait(m, TurnSettle.SETTLE_MS)
        assertTrue("the app is not on screen: no sound behind another app", h.resumed.isEmpty())
    }

    @Test fun leavingTheAppDuringATurnLetsEverythingGoAndPlaysNothing() {
        val h = Host().apply { playing = true }
        val m = machine(h)
        m.configChanged(true)
        m.abandon()
        assertFalse(h.coverShown); assertFalse("no fade: the screen is going away", h.coverFaded); assertNull(h.scheduled)
        h.wait(m, 5_000)
        assertTrue(h.resumed.isEmpty()); assertFalse(m.turning)
        m.abandon()                                          // twice is fine
        assertTrue(m.last.contains("left during a turn"))
    }

    @Test fun aSilentVideoIsCoveredButNotPausedAndAFullScreenPageWithNothingPlayingIsOnlyCovered() {
        var h = Host().apply { fullscreen = true; playing = true; hasSession = false }
        var m = machine(h)
        m.configChanged(true)
        assertTrue(h.coverShown); assertTrue("no media session to talk to", h.paused.isEmpty())
        assertTrue(h.logs.any { it.startsWith("turn-no-session") })
        h.wait(m, TurnSettle.SETTLE_MS)
        assertFalse(h.coverShown); assertTrue(h.resumed.isEmpty()); assertTrue(m.last.contains("nothing was paused"))
        h = Host().apply { fullscreen = true }
        m = machine(h)
        m.configChanged(true)
        assertTrue(h.coverShown); assertTrue(h.paused.isEmpty())
    }

    @Test fun aPauseTheEngineCouldNotBeAskedForIsNotCounted() {
        val h = Host().apply { playing = true; pauseResult = null }
        val m = machine(h)
        m.configChanged(true)
        assertEquals(0, m.pausedCount)
        h.wait(m, TurnSettle.SETTLE_MS)
        assertEquals(0, m.resumedCount); assertTrue(h.resumed.isEmpty())
        assertFalse(h.coverShown)
    }

    @Test fun sizeChangesAndTouchesOutsideATurnDoNothing() {
        val h = Host()
        val m = machine(h)
        m.sizeChanged(); m.touchedScreen(); m.tick()
        assertFalse(h.coverShown); assertNull(h.scheduled); assertEquals(0, m.turns)
    }
}
