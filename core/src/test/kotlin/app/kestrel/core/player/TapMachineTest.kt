package app.kestrel.core.player

import app.kestrel.core.player.PlayerRules.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** VP2 (owner message 49): single tap, double tap and the burst after a double tap. */
class TapMachineTest {
    private class Host : TapMachine.Host {
        var clock = 0L
        var scheduled: Long? = null
        val events = ArrayList<String>()
        override fun now() = clock
        override fun schedule(delayMs: Long) { scheduled = delayMs }
        override fun cancel() { scheduled = null }
        override fun single() { events.add("single") }
        override fun seek(zone: Zone, burst: Int) { events.add("seek ${zone.name} x$burst") }
        override fun playPause() { events.add("playPause") }
    }
    private fun Host.wait(t: TapMachine, ms: Long) { clock += ms; if (scheduled != null) { scheduled = null; t.tick() } }

    @Test fun oneTapIsASingleTapOnlyAfterTheDoubleTapWindow() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.5)
        assertTrue("nothing yet", h.events.isEmpty()); assertEquals(PlayerRules.DOUBLE_TAP_MS, h.scheduled)
        h.wait(t, PlayerRules.DOUBLE_TAP_MS)
        assertEquals(listOf("single"), h.events)
    }

    @Test fun aDoubleTapOnTheSidesSeeksAndNeverToggles() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.1); h.clock += 150; t.tap(0.15)
        assertEquals(listOf("seek LEFT x1"), h.events); assertNull("the pending single tap is gone", h.scheduled)
        h.wait(t, 1000)
        assertEquals("and no single tap comes later", listOf("seek LEFT x1"), h.events)
        val g = Host(); val u = TapMachine(g)
        u.tap(0.9); g.clock += 100; u.tap(0.85)
        assertEquals(listOf("seek RIGHT x1"), g.events)
    }

    @Test fun furtherTapsOnTheSameSideSeekAtOnceUntilTheBurstEnds() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.9); h.clock += 100; t.tap(0.9)
        h.clock += 300; t.tap(0.8); h.clock += 300; t.tap(0.95)
        assertEquals(listOf("seek RIGHT x1", "seek RIGHT x2", "seek RIGHT x3"), h.events)
        h.clock += PlayerRules.BURST_MS + 1; t.tap(0.9)
        assertEquals("after the burst a tap is a new first tap", 3, h.events.size); assertEquals(PlayerRules.DOUBLE_TAP_MS, h.scheduled)
        h.wait(t, PlayerRules.DOUBLE_TAP_MS)
        assertEquals("single", h.events.last())
    }

    @Test fun aDoubleTapInTheMiddlePlaysOrPauses() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.5); h.clock += 120; t.tap(0.55)
        assertEquals(listOf("playPause"), h.events)
    }

    @Test fun twoTapsOnDifferentSidesAreTwoSeparateTapsAndSlowTapsToo() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.1); h.clock += 100; t.tap(0.9)
        assertEquals("the first became a single tap, the second is waiting", listOf("single"), h.events)
        h.wait(t, PlayerRules.DOUBLE_TAP_MS); assertEquals(listOf("single", "single"), h.events)
        t.tap(0.5); h.wait(t, PlayerRules.DOUBLE_TAP_MS + 10); t.tap(0.5); h.wait(t, PlayerRules.DOUBLE_TAP_MS)
        assertEquals("two taps far apart are two single taps", listOf("single", "single", "single", "single"), h.events)
    }

    @Test fun resetDropsAPendingTap() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.5); t.reset()
        assertNull(h.scheduled); h.wait(t, 1000); assertTrue(h.events.isEmpty())
    }
}
