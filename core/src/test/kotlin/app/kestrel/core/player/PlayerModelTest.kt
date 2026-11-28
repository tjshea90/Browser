package app.kestrel.core.player

import app.kestrel.core.player.PlayerRules.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VP1 (owner message 49): the controls' model: what it shows of the film's state and what a tap or a control makes the page do. */
class PlayerModelTest {
    private class Host : PlayerModel.Host {
        var clock = 0L
        var controlsAt: Long? = null
        var shown = false
        var redraws = 0
        val feedback = ArrayList<String>()
        override fun now() = clock
        override fun scheduleControls(ms: Long) { controlsAt = ms }
        override fun cancelControls() { controlsAt = null }
        var lastAnimate = true
        override fun applyControls(shown: Boolean, animate: Boolean) { this.shown = shown; lastAnimate = animate }
        override fun changed() { redraws++ }
        override fun feedback(zone: Zone, burst: Int) { feedback.add("${zone.name} $burst") }
    }

    private fun sample(h: Host, t: Double = 0.0, d: Double = 65.0, b: Double = 20.0, paused: Boolean = false, ended: Boolean = false, muted: Boolean = false, ready: Int = 4, mse: Boolean = false, live: Boolean = false, rate: Double = 1.0, ack: Int = 0) =
        PlayerModel.Sample(t, d, b, paused, ended, muted, 100, rate, ready, mse, live, 1280, 720, h.clock, ack)
    private fun model(h: Host, playing: Boolean = true) = PlayerModel(h).also { it.start(playing) }
    private fun Host.tickControls(m: PlayerModel, ms: Long) { clock += ms; if (controlsAt != null) { controlsAt = null; m.controlsTimer() } }

    @Test fun thePositionRunsOnBetweenReportsAndStopsWhenPausedOrBuffering() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 10.0))
        h.clock += 500
        assertEquals(10.5, m.view().timeS, 1e-6)
        m.onSample(sample(h, t = 10.4, paused = true)); h.clock += 3000
        assertEquals("paused: it stands", 10.4, m.view().timeS, 1e-6)
        m.onSample(sample(h, t = 20.0, ready = 1)); h.clock += 3000
        assertEquals("buffering: it stands", 20.0, m.view().timeS, 1e-6); assertTrue(m.view().buffering)
        m.onSample(sample(h, t = 64.9, rate = 2.0)); h.clock += 2000
        assertEquals("never past a believed end", 65.0, m.view().timeS, 1e-6)
    }

    @Test fun aLengthShowsOnlyWhenItIsBelievedAndLiveSaysLive() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, d = 4.0, b = 4.0, mse = true))
        assertEquals(DurationView.Kind.UNKNOWN, m.view().kind)
        h.clock += 2500; m.onSample(sample(h, t = 1.0, d = 4.0, b = 4.0, mse = true))
        assertEquals(DurationView.Kind.KNOWN, m.view().kind); assertEquals(4.0, m.view().lengthS, 0.0)
        val g = Host(); val l = model(g); l.onSample(sample(g, d = -2.0, live = true))
        assertEquals(DurationView.Kind.LIVE, l.view().kind)
    }

    @Test fun everyTapTogglesTheControlsAtOnceAndADoubleTapOnASideSeeksOnTheSecondTap() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        assertTrue("full screen shows them", m.shown)
        m.onTap(500); assertFalse("ANS53c: a tap hides them AT ONCE (no double-tap wait)", m.shown); assertFalse("and without a fade", h.lastAnimate)
        h.clock += 600
        m.onTap(500); assertTrue("and the next shows them at once, too", m.shown); assertFalse(h.lastAnimate)
        m.drain()
        h.clock += 600
        m.onTap(900); assertFalse("the first tap of a side double tap toggled already", m.shown)
        h.clock += 100; m.onTap(900)
        val cmds = m.drain()
        assertEquals(1, cmds.size); assertEquals("skip", cmds[0].a); assertEquals(10.0, cmds[0].d, 0.0)
        assertEquals(listOf("RIGHT 1"), h.feedback)
        assertFalse("the second tap seeks and does not toggle again", m.shown)
        h.clock += 200; m.onTap(100)                                   // the burst is on the right: a tap on the left is a first tap of its own
        assertTrue("it toggles at once and seeks nothing", m.drain().isEmpty()); assertTrue(m.shown)
    }

    // ANS57a (owner message 57): a double tap that only seeks must not leave the controls on top of the film
    @Test fun aDoubleTapSeekClosesTheControlsAndTheyStayClosedThroughTheBurst() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        h.clock += 4000; h.tickControls(m, 0)
        m.onTap(500); if (m.shown) m.onTap(500)                        // (whatever they were: hidden now)
        assertFalse(m.shown)
        h.clock += 600
        m.onTap(900)                                                    // the first tap of the double tap brings them up at once ...
        assertTrue("ANS53c still holds: the first tap shows them at once", m.shown)
        h.clock += 100; m.onTap(900)                                    // ... the second tap seeks and sends them away at once
        assertEquals(listOf("RIGHT 1"), h.feedback); assertEquals(10.0, m.drain()[0].d, 0.0)
        assertFalse("the video stays visible without controls on top", m.shown); assertFalse("and they went at once, no fade", h.lastAnimate)
        h.clock += 200; m.onTap(900); m.drain()                         // the burst: a third tap seeks again, the controls stay away
        assertFalse(m.shown); assertEquals(listOf("RIGHT 1", "RIGHT 2"), h.feedback)
        h.clock += 5000; m.onTap(100)                                   // a later single tap on the left brings them up as ever (nothing is stuck)
        assertTrue(m.shown)
    }

    @Test fun aGestureThatBeginsPutsTheControlsAway() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        assertTrue(m.shown)
        m.gestureBegan()
        assertFalse("the long press that armed the precise seek, or a brightness drag, keeps the picture clear", m.shown); assertFalse(h.lastAnimate)
        m.gestureBegan(); assertFalse(m.shown)
    }

    @Test fun theLeftSeekAlsoClosesThemAndTheSkipButtonsDoNot() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        assertTrue(m.shown)
        m.skipButton(true); m.drain()
        assertTrue("the 10 s BUTTON is a control: the controls stay up (their clock restarts)", m.shown)
        m.onTap(100); assertFalse("the first tap of a double tap hides them as they were up", m.shown)
        h.clock += 100; m.onTap(100)
        assertEquals(-10.0, m.drain()[0].d, 0.0)
        assertFalse(m.shown)
    }

    @Test fun theLeftSideSeeksBackAndTheMiddleHasNoDoubleTapMeaning() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        m.onTap(100); h.clock += 100; m.onTap(100)
        assertEquals(-10.0, m.drain()[0].d, 0.0)
        h.clock += 2000
        val before = m.shown
        m.onTap(500); h.clock += 100; m.onTap(500)
        assertTrue("two quick taps in the middle are two toggles and make no command (ANS53c: the owner's own test of instant on / instant off never pauses the film)", m.drain().isEmpty())
        assertEquals("shown, hidden: back where it was", before, m.shown)
    }

    @Test fun theButtonsMakeCommandsAndTheStateIsOptimisticUntilThePageConfirms() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 5.0))
        m.playPause()
        assertEquals("pause", m.drain()[0].a); assertFalse(m.view().playing)
        h.clock += 300; m.onSample(sample(h, t = 5.3, paused = false))                   // the page has not paused yet: the wait is not over
        assertFalse("still shown as paused", m.view().playing)
        h.clock += 300; m.onSample(sample(h, t = 5.3, paused = true, ack = 1))           // now it did (and says so: command 1 is carried out)
        assertFalse(m.view().playing)
        m.playPause(); assertEquals("play", m.drain()[0].a); assertTrue(m.view().playing)
        h.clock += 3100; m.onSample(sample(h, t = 5.3, paused = true, ack = 1))
        assertFalse("a page that never did it: its own state counts again after 3 s", m.view().playing)
        m.mute(); val mc = m.drain()[0]; assertEquals("mute", mc.a); assertTrue(mc.on); assertTrue(m.view().muted)
        m.skipButton(false); assertEquals(-10.0, m.drain()[0].d, 0.0)
        m.seekFraction(0.5); val sc = m.drain()[0]; assertEquals("seek", sc.a); assertEquals(32.5, sc.t, 1e-9)
        val h2 = Host(); val m2 = model(h2); m2.onSample(sample(h2, d = -1.0))
        m2.seekFraction(0.5); assertTrue("no believed length: the track does nothing", m2.drain().isEmpty())
    }

    @Test fun pausedOrEndedShowsTheControlsAndAPlayingFilmHidesThemByItself() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h))
        h.tickControls(m, PlayerRules.AFTER_ENTER_MS); assertFalse(m.shown)
        m.onSample(sample(h, paused = true)); assertTrue(m.shown)
        m.onSample(sample(h, paused = false)); h.tickControls(m, PlayerRules.HIDE_AFTER_MS); assertFalse(m.shown)
        m.onSample(sample(h, ended = true)); assertTrue(m.shown)
    }

    @Test fun aLateReportTakenBeforeTheFilmPausedDoesNotUndoTheButtonAndTheSecondPressPlays() {
        // lab run 43776782679: press 1 asked for "pause", the film paused, but a report taken BEFORE the pause was handled late (a busy tablet); it ended the wait and a second press asked for "pause" again
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 15.0))
        m.playPause(); val first = m.drain()[0]; assertEquals("pause", first.a); assertEquals(1, first.id)
        h.clock += 1500; m.onSample(sample(h, t = 15.2, paused = false, ack = 0))              // old: the page had not carried out command 1 when it was taken
        assertFalse("still shown as paused: the report is older than the command", m.view().playing)
        h.clock += 100; m.onSample(sample(h, t = 15.3, paused = true, ack = 1))
        m.playPause(); assertEquals("the second press plays", "play", m.drain()[0].a)
    }

    @Test fun aSeekIsShownAtOnceAndHeldUntilThePageSaysItDidIt() {
        // review of alpha55: the thumb sprang back to the old place for a few hundred ms (up to a second) after the finger let go of the track, because the position is only the page's
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0, d = 600.0))
        h.clock += 100
        m.seekFraction(0.5)
        assertEquals("the track: shown where it was asked to go, at once", 300.0, m.view().timeS, 1e-6)
        h.clock += 200; m.onSample(sample(h, t = 30.4, d = 600.0, ack = 0))                    // a report taken before the page carried the seek out
        assertEquals("it does not pull the thumb back", 300.0, m.view().timeS, 1e-6)
        h.clock += 100; m.onSample(sample(h, t = 300.1, d = 600.0, ack = 1))                   // carried out: the page's own position counts again
        assertEquals(300.1, m.view().timeS, 1e-6)
        m.skipButton(true)
        assertEquals("a 10 s button: shown 10 s on at once", 310.1, m.view().timeS, 1e-6)
        m.skipButton(true)
        assertEquals("and again from there", 320.1, m.view().timeS, 1e-6)
        h.clock += 3100; m.onSample(sample(h, t = 300.9, d = 600.0, ack = 1))                  // a page that never did it: its own position counts again after 3 s
        assertEquals(300.9, m.view().timeS, 1e-6)
        m.seekFraction(2.0); assertEquals("never past the end", 600.0, m.view().timeS, 1e-6)
        m.skip(-1000.0); assertEquals("never before the start", 0.0, m.view().timeS, 1e-6)
    }

    @Test fun aFilmThatFailedIsNotPlayingHasNoRingAndKeepsTheControlsUp() {
        // research: the controls stay while a film is paused, ended or FAILED; a failed film used to read as "buffering" (a ring that spins for ever)
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 12.0))
        h.tickControls(m, PlayerRules.HIDE_AFTER_MS + 100)
        assertFalse("a playing film hides them after a while", m.shown)
        h.clock += 500; m.onSample(PlayerModel.Sample(12.0, 65.0, 20.0, false, false, false, 100, 1.0, 0, false, false, 1280, 720, h.clock, 0, true))   // readyState 0 and an error
        val v = m.view()
        assertTrue(v.failed); assertFalse("not playing", v.playing); assertFalse("and no ring", v.buffering)
        assertTrue("the controls come and stay", m.shown)
        h.tickControls(m, 60_000)
        assertTrue("for as long as it stays failed", m.shown)
    }

    @Test fun aReportThatNamesTheCommandCountsAtOnceAndEverythingIsNumbered() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 2.0))
        m.playPause(); m.mute(); m.skipButton(true)
        val c = m.drain()
        assertEquals(listOf(1, 2, 3), c.map { it.id })
        assertEquals("{\"a\":\"pause\",\"i\":1}", c[0].json()); assertEquals("{\"a\":\"mute\",\"on\":true,\"i\":2}", c[1].json()); assertEquals("{\"a\":\"skip\",\"d\":10,\"i\":3}", c[2].json())
        h.clock += 200; m.onSample(sample(h, t = 2.2, paused = false, muted = false, ack = 3))   // the page carried all three out: a film that plays and is not muted is what it IS (the page refused)
        assertTrue(m.view().playing); assertFalse(m.view().muted)
        m.start(true); m.playPause(); assertEquals("the count goes on across a new full screen", 4, m.drain()[0].id)
    }

    @Test fun theCommandJsonIsWhatThePageScriptReads() {
        assertEquals("{\"a\":\"play\"}", PlayerModel.Cmd("play").json())
        assertEquals("{\"a\":\"seek\",\"t\":32.50}", PlayerModel.Cmd("seek", t = 32.5).json())
        assertEquals("{\"a\":\"skip\",\"d\":-10}", PlayerModel.Cmd("skip", d = -10.0).json())
        assertEquals("{\"a\":\"mute\",\"on\":true}", PlayerModel.Cmd("mute", on = true).json())
        assertEquals("{\"a\":\"rate\",\"r\":1.50}", PlayerModel.Cmd("rate", r = 1.5).json())
    }

    @Test fun stopAndAScreenTurnClearEverything() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h)); m.turnStarted(); assertFalse("the controls go under the cover", m.shown)
        h.clock += 400; m.onTap(500); assertFalse("a tap under the cover does nothing", m.shown)
        m.turnEnded(); assertTrue(m.shown)
        m.playPause(); m.stop()
        assertTrue(m.drain().isEmpty()); assertFalse(m.shown); assertFalse(m.running)
        m.onSample(sample(h)); assertEquals("a report after the end is nobody's", 0.0, m.view().timeS, 0.0)
    }
}
