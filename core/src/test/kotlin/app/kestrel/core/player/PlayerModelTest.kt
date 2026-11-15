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
        var tapAt: Long? = null
        var shown = false
        var redraws = 0
        val feedback = ArrayList<String>()
        override fun now() = clock
        override fun scheduleControls(ms: Long) { controlsAt = ms }
        override fun cancelControls() { controlsAt = null }
        override fun scheduleTap(ms: Long) { tapAt = ms }
        override fun cancelTap() { tapAt = null }
        override fun applyControls(shown: Boolean, animate: Boolean) { this.shown = shown }
        override fun changed() { redraws++ }
        override fun feedback(zone: Zone, burst: Int) { feedback.add("${zone.name} $burst") }
    }

    private fun sample(h: Host, t: Double = 0.0, d: Double = 65.0, b: Double = 20.0, paused: Boolean = false, ended: Boolean = false, muted: Boolean = false, ready: Int = 4, mse: Boolean = false, live: Boolean = false, rate: Double = 1.0) =
        PlayerModel.Sample(t, d, b, paused, ended, muted, 100, rate, ready, mse, live, 1280, 720, h.clock)
    private fun model(h: Host, playing: Boolean = true) = PlayerModel(h).also { it.start(playing) }
    private fun Host.tickTap(m: PlayerModel, ms: Long) { clock += ms; if (tapAt != null) { tapAt = null; m.tapTimer() } }
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

    @Test fun aSingleTapToggleTheControlsAfterTheDoubleTapWindowAndADoubleTapOnASideSeeksInstead() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        assertTrue("full screen shows them", m.shown)
        m.onTap(500); assertTrue("not yet: the double tap window", m.shown)
        h.tickTap(m, PlayerRules.DOUBLE_TAP_MS); assertFalse("a single tap hides them", m.shown)
        m.onTap(500); h.tickTap(m, PlayerRules.DOUBLE_TAP_MS); assertTrue("and the next shows them", m.shown)
        val before = m.drain().size
        m.onTap(900); h.clock += 100; m.onTap(900)
        val cmds = m.drain()
        assertEquals(1, cmds.size); assertEquals("skip", cmds[0].a); assertEquals(10.0, cmds[0].d, 0.0)
        assertEquals(listOf("RIGHT 1"), h.feedback)
        h.clock += 200; m.onTap(100); h.clock += 50
        m.onTap(200)
        assertEquals("another side: a first tap and a second", before, 0)
    }

    @Test fun theLeftSideSeeksBackAndTheMiddleDoubleTapPlaysAndPauses() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 30.0))
        m.onTap(100); h.clock += 100; m.onTap(100)
        assertEquals(-10.0, m.drain()[0].d, 0.0)
        h.clock += 2000
        m.onTap(500); h.clock += 100; m.onTap(500)
        val c = m.drain()
        assertEquals("pause", c[0].a)
        assertFalse("the picture says paused at once (the page answers later)", m.view().playing)
    }

    @Test fun theButtonsMakeCommandsAndTheStateIsOptimisticUntilThePageConfirms() {
        val h = Host(); val m = model(h)
        m.onSample(sample(h, t = 5.0))
        m.playPause()
        assertEquals("pause", m.drain()[0].a); assertFalse(m.view().playing)
        h.clock += 300; m.onSample(sample(h, t = 5.3, paused = false))                   // the page has not paused yet: the wait is not over
        assertFalse("still shown as paused", m.view().playing)
        h.clock += 300; m.onSample(sample(h, t = 5.3, paused = true))                    // now it did
        assertFalse(m.view().playing)
        m.playPause(); assertEquals("play", m.drain()[0].a); assertTrue(m.view().playing)
        h.clock += 2000; m.onSample(sample(h, t = 5.3, paused = true))
        assertFalse("a page that never did it: its own state counts again after 1.2 s", m.view().playing)
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
        m.onTap(500); h.tickTap(m, 400); assertFalse("a tap under the cover does nothing", m.shown)
        m.turnEnded(); assertTrue(m.shown)
        m.playPause(); m.stop()
        assertTrue(m.drain().isEmpty()); assertFalse(m.shown); assertFalse(m.running)
        m.onSample(sample(h)); assertEquals("a report after the end is nobody's", 0.0, m.view().timeS, 0.0)
    }
}
