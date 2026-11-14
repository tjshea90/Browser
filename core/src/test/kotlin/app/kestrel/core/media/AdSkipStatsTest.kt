package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-Y2: the numbers the YouTube ad skipper reports (differences since its last report, numbers only) and the one plain Diagnostics line made of them. */
class AdSkipStatsTest {
    @Test fun nothingSeenSaysSo() {
        assertEquals("YouTube ads: none seen yet", AdSkipStats().line())
    }

    @Test fun reportsAreAddedUpAndTheLineSaysWhatHappened() {
        val s = AdSkipStats()
        s.add(seen = 2, skipped = 2, clicks = 1, overlays = 0, dialogs = 0, jumps = 2, gaveUp = 0, reason = "")
        s.add(seen = 3, skipped = 2, clicks = 1, overlays = 2, dialogs = 1, jumps = 1, gaveUp = 1, reason = "an ad was still showing after 15 s")
        assertEquals(5, s.seen); assertEquals(4, s.skipped)
        val line = s.line()
        assertEquals("YouTube ads: 5 seen, 4 skipped (3 fast-forwarded, 2 skip buttons pressed), 2 overlays closed, 1 enforcement dialog removed, 1 gave up; last problem: an ad was still showing after 15 s", line)
    }

    @Test fun onlyWhatHappenedIsMentionedAndPluralsAreRight() {
        val s = AdSkipStats()
        s.add(seen = 1, skipped = 1, clicks = 0, overlays = 1, dialogs = 0, jumps = 1, gaveUp = 0, reason = "")
        assertEquals("YouTube ads: 1 seen, 1 skipped (1 fast-forwarded), 1 overlay closed", s.line())
    }

    @Test fun anOverlayOrDialogAloneIsReportedEvenWithoutAnAd() {
        val s = AdSkipStats()
        s.add(seen = 0, skipped = 0, clicks = 0, overlays = 0, dialogs = 2, jumps = 0, gaveUp = 0, reason = "")
        assertEquals("YouTube ads: none seen yet, 2 enforcement dialogs removed", s.line())
    }

    @Test fun nonsenseNumbersAreClampedAndTheProblemTextIsBounded() {
        val s = AdSkipStats()
        s.add(seen = -5, skipped = 1_000_000, clicks = 3, overlays = 0, dialogs = 0, jumps = 0, gaveUp = 0, reason = "x".repeat(500) + "\né")
        assertEquals(0, s.seen); assertEquals(999, s.skipped)
        val line = s.line()
        assertTrue(line, line.length < 330 && line.none { it == '\n' || it.code > 126 })
    }

    @Test fun theLastProblemIsReplacedByANewerOneAndKeptWhenTheNextReportHasNone() {
        val s = AdSkipStats()
        s.add(1, 0, 0, 0, 0, 0, 1, "speed refused")
        s.add(1, 1, 0, 0, 0, 1, 0, "")
        assertTrue(s.line(), s.line().endsWith("last problem: speed refused"))
        s.add(0, 0, 0, 0, 0, 0, 0, "seek refused")
        assertTrue(s.line(), s.line().endsWith("last problem: seek refused"))
    }

    @Test fun resetForgetsEverything() {
        val s = AdSkipStats(); s.add(1, 1, 1, 1, 1, 1, 1, "x"); s.reset()
        assertEquals("YouTube ads: none seen yet", s.line())
    }
}
