package app.kestrel.core.mem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PERF20: Android's "critical" signals with plenty of memory free are held; every sign of real shortage passes them on. */
class TrimGateTest {
    private val never = Long.MAX_VALUE
    private fun v(level: Int = 80, avail: Long = 450, low: Boolean = false, kill: Long = never, held: Int = 0, on: Boolean = true) = TrimGate.decide(on, level, avail, low, kill, held)

    @Test fun aCriticalSignalWithPlentyOfFreeMemoryIsHeld() {
        assertFalse(v(level = 80, avail = 442).forward)       // the median of the owner's 33 level-80 signals
        assertFalse(v(level = 15, avail = 369).forward)       // the median of the 53 level-15 signals
        assertFalse(v(level = 40, avail = 400).forward)
        assertFalse(v(level = 60, avail = 400).forward)
        assertTrue(v(level = 80, avail = 442).why.contains("false alarm"))
    }

    @Test fun everySignOfRealShortagePassesItOn() {
        assertTrue("below the limit", v(avail = 349).forward)
        assertFalse("at the limit", v(avail = 350).forward)
        assertTrue("195 MB was the lowest level-15 reading", v(level = 15, avail = 195).forward)
        assertTrue("the system's own flag", v(low = true).forward)
        assertTrue("unknown", v(avail = -1).forward)
        assertTrue("an engine kill 10 minutes ago", v(kill = 10 * 60_000L).forward)
        assertFalse("a kill 7 hours ago is old news", v(kill = 7 * 3_600_000L).forward)
        assertTrue("six held already: clean up", v(held = 6).forward)
        assertFalse(v(held = 5).forward)
    }

    @Test fun theSwitchAndTheLevelsTheEngineIgnoresPassThroughUntouched() {
        assertTrue("switch off: exactly the old behaviour", v(on = false).forward)
        for (l in listOf(5, 10, 20)) assertTrue("level $l is a no-op in the engine anyway", v(level = l).forward)
        assertTrue(TrimGate.engineReacts(15)); assertTrue(TrimGate.engineReacts(40)); assertTrue(TrimGate.engineReacts(80))
        assertFalse(TrimGate.engineReacts(20)); assertFalse(TrimGate.engineReacts(10)); assertFalse(TrimGate.engineReacts(5))
    }

    @Test fun theReasonIsAlwaysSaid() {
        assertEquals("only 200 MB available", v(avail = 200).why)
        assertTrue(v(kill = 90 * 60_000L).why.startsWith("the engine was ended 90 min ago"))
    }
}
