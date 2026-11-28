package app.kestrel.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SP1, owner message 41: a very quick, lightweight logo flourish at the start; nothing waits for it. */
class LaunchAnimTest {
    private val steps = (0..100).map { it / 100f }

    @Test fun itIsVeryShort() {
        assertTrue("under half a second: ${LaunchAnim.TOTAL_MS}", LaunchAnim.TOTAL_MS in 200L..450L)
    }

    @Test fun itStartsAsTheLaunchWindowWasAndEndsWithNothingOnTheScreen() {
        assertEquals("black cover at the start (the launch window was black)", 1f, LaunchAnim.cover(0f), 0.001f)
        assertEquals("the symbol at full strength", 1f, LaunchAnim.logoAlpha(0f), 0.001f)
        assertEquals("the symbol has the launch picture's size, a touch smaller", 0.97f, LaunchAnim.scale(0f), 0.01f)
        assertEquals("no halo yet", 0f, LaunchAnim.glow(0f), 0.001f)
        assertEquals("cover gone", 0f, LaunchAnim.cover(1f), 0.001f)
        assertEquals("symbol gone", 0f, LaunchAnim.logoAlpha(1f), 0.001f)
        assertEquals("no halo left", 0f, LaunchAnim.glow(1f), 0.001f)
    }

    @Test fun theCoverOnlyClearsAndTheSymbolOnlyFades() {
        var c = 2f; var l = 2f
        for (t in steps) {
            assertTrue("cover never comes back at $t", LaunchAnim.cover(t) <= c + 1e-6f); c = LaunchAnim.cover(t)
            assertTrue("the symbol never comes back at $t", LaunchAnim.logoAlpha(t) <= l + 1e-6f); l = LaunchAnim.logoAlpha(t)
        }
    }

    @Test fun theHaloSwellsOnceAndFadesWithAPeakBeforeTheMiddle() {
        val peak = steps.maxByOrNull { LaunchAnim.glow(it) }!!
        assertTrue("peak at $peak", peak in 0.25f..0.5f); assertEquals(1f, LaunchAnim.glow(peak), 0.01f)
        var up = true; var last = 0f
        for (t in steps) { val g = LaunchAnim.glow(t); if (t <= peak) assertTrue("rises until the peak ($t)", g >= last - 1e-6f) else assertTrue("then only falls ($t)", g <= last + 1e-6f); last = g }
    }

    @Test fun everyValueStaysInRange() {
        for (t in steps + listOf(-1f, 2f)) {
            for (v in listOf(LaunchAnim.cover(t), LaunchAnim.logoAlpha(t), LaunchAnim.glow(t))) assertTrue("0..1 at $t: $v", v in 0f..1f)
            assertTrue("the size stays sane at $t", LaunchAnim.scale(t) in 0.9f..1.15f); assertTrue(LaunchAnim.haloRadius(t) in 0.5f..2f)
        }
    }

    @Test fun theScreenIsAlreadyMostlyVisibleBeforeTheEnd() {
        assertTrue("half way the cover is mostly gone: ${LaunchAnim.cover(0.6f)}", LaunchAnim.cover(0.6f) < 0.45f)
    }
}
