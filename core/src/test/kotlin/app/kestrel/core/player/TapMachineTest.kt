package app.kestrel.core.player

import app.kestrel.core.player.PlayerRules.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** VP2 / ANS53c (owner message 53: "zero delay"): every tap toggles at once; a second tap on a side within the window seeks; the burst after it. */
class TapMachineTest {
    private class Host : TapMachine.Host {
        var clock = 0L
        val events = ArrayList<String>()
        override fun now() = clock
        override fun single() { events.add("single") }
        override fun seek(zone: Zone, burst: Int) { events.add("seek ${zone.name} x$burst") }
    }

    @Test fun aTapTogglesAtOnceNothingWaits() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.5)
        assertEquals("the very tap is the toggle: no timer, no window to wait for", listOf("single"), h.events)
        h.clock += 5000; t.tap(0.5)
        assertEquals(listOf("single", "single"), h.events)
    }

    @Test fun aSecondTapOnASideWithinTheWindowSeeksInsteadOfTogglingAgain() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.1); h.clock += 150; t.tap(0.15)
        assertEquals("the first toggled at once, the second is the double tap", listOf("single", "seek LEFT x1"), h.events)
        val g = Host(); val u = TapMachine(g)
        u.tap(0.9); g.clock += 100; u.tap(0.85)
        assertEquals(listOf("single", "seek RIGHT x1"), g.events)
    }

    @Test fun theMiddleHasNoDoubleTapMeaningTwoQuickTapsAreTwoToggles() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.5); h.clock += 120; t.tap(0.55)
        assertEquals(listOf("single", "single"), h.events)
    }

    @Test fun aSecondTapAfterTheWindowOrOnAnotherSideIsAFirstTapAgain() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.1); h.clock += PlayerRules.DOUBLE_TAP_MS + 1; t.tap(0.1)
        assertEquals("too slow: two toggles", listOf("single", "single"), h.events)
        t.tap(0.9)                                                        // another side, at once after the last
        assertEquals(listOf("single", "single", "single"), h.events)
    }

    @Test fun furtherTapsOnTheSameSideSeekAtOnceUntilTheBurstEnds() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.9); h.clock += 100; t.tap(0.9)
        h.clock += 300; t.tap(0.8); h.clock += 300; t.tap(0.95)
        assertEquals(listOf("single", "seek RIGHT x1", "seek RIGHT x2", "seek RIGHT x3"), h.events)
        h.clock += PlayerRules.BURST_MS + 1; t.tap(0.9)
        assertEquals("after the burst a tap is a new first tap and toggles at once", "single", h.events.last()); assertEquals(5, h.events.size)
    }

    @Test fun resetForgetsTheLastTapAndTheBurst() {
        val h = Host(); val t = TapMachine(h)
        t.tap(0.1); t.reset(); h.clock += 50; t.tap(0.1)
        assertEquals("no double tap across a reset", listOf("single", "single"), h.events)
        assertTrue(h.events.none { it.startsWith("seek") })
    }
}
