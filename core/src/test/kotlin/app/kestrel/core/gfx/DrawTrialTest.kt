package app.kestrel.core.gfx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawTrialTest {
    @Test fun offWritesNothing() {
        assertTrue(DrawTrial.lines(0).isEmpty())
        assertTrue(DrawTrial.overrides(0).isEmpty())
        assertTrue(DrawTrial.lines(-3).isEmpty())
    }

    @Test fun levelOneTurnsTheMaliPathsOffAndNeverSwitchesOnCpuDrawing() {
        val m = DrawTrial.lines(1).toMap()
        assertEquals("false", m["gfx.webrender.use-optimized-shaders"])
        assertEquals("true", m["gfx.webrender.draw-calls-for-texture-copy"])
        assertEquals("false", m["gfx.webrender.program-binary-disk"])
        assertFalse(m.containsKey("gfx.webrender.software"))
        assertFalse(m.containsKey("layers.gpu-process.enabled"))
        assertTrue(DrawTrial.overrides(1).isEmpty())
    }

    @Test fun levelTwoAlsoDropsTheHelperProcessAndTheShaderPrecompile() {
        val m = DrawTrial.lines(2).toMap()
        assertEquals("false", m["layers.gpu-process.enabled"])
        assertEquals(setOf("gfx.webrender.use-optimized-shaders", "gfx.webrender.draw-calls-for-texture-copy", "gfx.webrender.program-binary-disk", "layers.gpu-process.enabled"), m.keys)
        assertEquals(listOf("gfx.webrender.precache-shaders" to "false"), DrawTrial.overrides(2))
        assertTrue(DrawTrial.lines(9).size == DrawTrial.lines(2).size)      // a number past the top acts as the top
    }

    @Test fun softwareDrawingCountsOnlyWhileNoTrialRuns() {
        assertTrue(DrawTrial.softwareDrawing(true, 0))
        assertFalse(DrawTrial.softwareDrawing(true, 1))
        assertFalse(DrawTrial.softwareDrawing(true, 2))
        assertFalse(DrawTrial.softwareDrawing(false, 0))
    }

    @Test fun aStartThatNeverSteadiedEndsTheTrial() {
        assertTrue(DrawTrial.startFailed(1, 1_000L))
        assertTrue(DrawTrial.startFailed(2, 5L))
        assertFalse(DrawTrial.startFailed(1, 0L))       // the last start was good (mark taken away)
        assertFalse(DrawTrial.startFailed(0, 1_000L))   // no trial, nothing to end
    }

    @Test fun anEarlyCrashEndsTheTrialButALateOneDoesNot() {
        assertTrue(DrawTrial.crashEndsTrial(1, 20_000L))
        assertTrue(DrawTrial.crashEndsTrial(2, DrawTrial.EARLY_CRASH_MS - 1))
        assertFalse(DrawTrial.crashEndsTrial(1, DrawTrial.EARLY_CRASH_MS))
        assertFalse(DrawTrial.crashEndsTrial(0, 1_000L))
        assertFalse(DrawTrial.crashEndsTrial(1, -1L))   // an unknown start time never ends it
    }

    @Test fun everyNameIsListedOnceForTheReadBack() {
        assertEquals(DrawTrial.names.size, DrawTrial.names.toSet().size)
        assertTrue(DrawTrial.names.contains("gfx.webrender.precache-shaders"))
        assertTrue(DrawTrial.names.all { it.startsWith("gfx.webrender.") || it.startsWith("layers.") })
    }

    @Test fun labelsAreWords() {
        assertEquals("off", DrawTrial.label(0))
        assertTrue(DrawTrial.label(1).contains("hardware"))
        assertTrue(DrawTrial.label(2).contains("no helper"))
    }
}
