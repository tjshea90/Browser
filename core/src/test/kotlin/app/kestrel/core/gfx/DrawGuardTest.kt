package app.kestrel.core.gfx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** GL2 (owner 2026-11-26: "Today i saw undrawn cut off pictures and text"): the drawing modes, the way out of the failing one, and the report button's rule. */
class DrawGuardTest {
    @Test fun threeSavedValuesGiveThreeModes() {
        assertEquals(DrawGuard.Mode.PROCESSOR, DrawGuard.mode(labSoft = true, trial = 0))
        assertEquals(DrawGuard.Mode.FIXES, DrawGuard.mode(labSoft = false, trial = 1))
        assertEquals(DrawGuard.Mode.PLAIN, DrawGuard.mode(labSoft = false, trial = 0))
        // a trial and CPU drawing exclude each other (PERF25): the trial wins
        assertEquals(DrawGuard.Mode.FIXES, DrawGuard.mode(labSoft = true, trial = 1))
    }

    @Test fun onlyThePlainModeIsLeft() {
        assertEquals(DrawTrial.FIXES, DrawGuard.leavePlain(labSoft = false, trial = 0))
        assertNull(DrawGuard.leavePlain(labSoft = true, trial = 0))
        assertNull(DrawGuard.leavePlain(labSoft = false, trial = 1))
        assertNull(DrawGuard.leavePlain(labSoft = true, trial = 1))
    }

    @Test fun afterLeavingTheModeIsNeverPlainAgain() {
        for (soft in listOf(true, false)) for (trial in 0..1) {
            val to = DrawGuard.leavePlain(soft, trial) ?: trial
            assertTrue("soft=$soft trial=$trial", DrawGuard.mode(soft, to) != DrawGuard.Mode.PLAIN)
        }
    }

    @Test fun theSettingsChoiceCanOnlyMakeTheTwoGoodModes() {
        for (c in 0..1) {
            val (soft, trial) = DrawGuard.valuesOf(c)
            assertTrue(DrawGuard.mode(soft, trial) != DrawGuard.Mode.PLAIN)
            assertEquals(c, DrawGuard.choiceOf(soft, trial))
        }
        // the plain mode reads as the graphics chip (it is moved there at the next start)
        assertEquals(1, DrawGuard.choiceOf(labSoft = false, trial = 0))
        assertEquals(0, DrawGuard.choiceOf(labSoft = true, trial = 0))
    }

    @Test fun aTrialThatSwitchesItselfOffFallsBackToTheProcessor() {
        assertTrue(DrawGuard.TRIAL_FALLBACK_SOFT)
        // trial off + Software drawing on is the processor, not the plain mode
        assertEquals(DrawGuard.Mode.PROCESSOR, DrawGuard.mode(DrawGuard.TRIAL_FALLBACK_SOFT, DrawTrial.OFF))
    }

    private val day = 24L * 60 * 60 * 1000

    @Test fun oneReportNotesAndTwoInThreeDaysFallBackWhileTheGraphicsChipDraws() {
        val t0 = 1_000_000_000_000L
        val one = PaintReports.add("", t0)
        assertEquals(1, PaintReports.count(one, t0))
        assertFalse(PaintReports.fallBack(DrawGuard.Mode.FIXES, one, t0))
        val two = PaintReports.add(one, t0 + 2 * day)
        assertEquals(2, PaintReports.count(two, t0 + 2 * day))
        assertTrue(PaintReports.fallBack(DrawGuard.Mode.FIXES, two, t0 + 2 * day))
        assertTrue(PaintReports.fallBack(DrawGuard.Mode.PLAIN, two, t0 + 2 * day))
    }

    @Test fun reportsOlderThanThreeDaysDoNotCount() {
        val t0 = 1_000_000_000_000L
        val old = PaintReports.add("", t0)
        val later = PaintReports.add(old, t0 + 4 * day)
        assertEquals(1, PaintReports.count(later, t0 + 4 * day))
        assertFalse(PaintReports.fallBack(DrawGuard.Mode.FIXES, later, t0 + 4 * day))
        assertFalse("the old one is dropped from the saved list too", later.contains(t0.toString()))
    }

    @Test fun theProcessorIsNeverMovedAgainAndTheAnswerSaysWhatHappens() {
        val t0 = 1_000_000_000_000L
        val two = PaintReports.add(PaintReports.add("", t0), t0 + 1000)
        assertFalse(PaintReports.fallBack(DrawGuard.Mode.PROCESSOR, two, t0 + 1000))
        assertTrue(PaintReports.answer(DrawGuard.Mode.PROCESSOR, two, t0 + 1000).contains("processor is already drawing"))
        assertTrue(PaintReports.answer(DrawGuard.Mode.FIXES, two, t0 + 1000).contains("draw pages with the processor from the next start"))
        assertTrue(PaintReports.answer(DrawGuard.Mode.FIXES, PaintReports.add("", t0), t0).contains("within 3 days"))
    }

    @Test fun theListStaysSmallAndSurvivesJunk() {
        var enc = "x,,-5,abc"
        val t0 = 1_000_000_000_000L
        for (i in 0 until 20) enc = PaintReports.add(enc, t0 + i)
        assertTrue(PaintReports.parse(enc).size <= PaintReports.KEEP)
        assertTrue(PaintReports.parse("x,,-5,abc").isEmpty())
    }

    @Test fun theLogLineNamesTheModeAndNoSite() {
        val t0 = 1_000_000_000_000L
        val line = PaintReports.logLine(DrawGuard.Mode.FIXES, PaintReports.add("", t0), t0)
        assertEquals("mode=graphics-chip-with-fixes n=1 window=3d fallback=false", line)
    }
}
