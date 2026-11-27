package app.kestrel.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ANS51e (owner message 51): what the full-screen player script measures inside the page, as numbers (stalls, its own cost, frames shown and dropped). */
class FilmHealthTest {
    @Test fun frameCountsAreDifferencesAndTheFirstLookIsOnlyABaseline() {
        val f = FilmHealth()
        f.onReport(0, 4, 100, 2, 1920, 1080, 0L)
        assertEquals("the first look at an element only sets the baseline", 0L, f.frames)
        f.onReport(0, 4, 220, 6, 1920, 1080, 1000L)
        assertEquals(120L, f.frames); assertEquals(4L, f.dropped)
        f.onReport(0, 4, 220, 6, 1920, 1080, 2000L)
        assertEquals("a paused film shows no new frames", 120L, f.frames)
        assertTrue(f.line(), f.line().contains("120 frames shown, 4 dropped (3.3 %)"))
    }

    @Test fun aCounterThatGoesDownIsANewElementNotNegativeFrames() {
        val f = FilmHealth()
        f.onReport(0, 1, 500, 10, 640, 360, 0L); f.onReport(0, 1, 560, 12, 640, 360, 2000L)
        assertEquals(60L, f.frames)
        f.onReport(0, 1, 40, 0, 640, 360, 3000L)                       // the page swapped its element: its counters start again
        assertEquals("nothing is taken from the totals, the baseline is taken again", 60L, f.frames)
        f.onReport(0, 1, 70, 1, 640, 360, 4000L)
        assertEquals(90L, f.frames); assertEquals(3L, f.dropped)
        f.newSession()
        f.onReport(0, 1, 9000, 0, 640, 360, 5000L)                       // a new full screen: a new baseline, the run's totals stay
        assertEquals(90L, f.frames)
    }

    @Test fun aJumpOfTheCounterThatNoDisplayCouldShowIsASwappedElementNotFrames() {
        val f = FilmHealth()
        f.onReport(0, 1, 150, 0, 640, 360, 0L); f.onReport(0, 1, 180, 0, 640, 360, 1000L)       // an ad element: 30 frames in a second
        assertEquals(30L, f.frames)
        f.onReport(0, 1, 1800, 0, 640, 360, 2000L)                                               // the page swapped to the film's element, already at 1800: not 1620 frames in one second
        assertEquals("only a new baseline", 30L, f.frames)
        f.onReport(0, 1, 1830, 0, 640, 360, 3000L)
        assertEquals(60L, f.frames)
        f.onReport(0, 1, 2100, 2, 640, 360, 13_000L)                                              // a long gap (a stalled page): 270 frames over the 10 s step are fine
        assertEquals(330L, f.frames)
    }

    @Test fun droppedNeverExceedsShown() {
        val f = FilmHealth()
        f.onReport(0, 1, 10, 0, 640, 360, 0L)
        f.onReport(0, 1, 20, 50, 640, 360, 1000L)                        // (an engine counting oddly)
        assertEquals(10L, f.frames); assertEquals(10L, f.dropped)
    }

    @Test fun aTimerThatRanASecondLateIsAStallAndLessIsNot() {
        val f = FilmHealth()
        f.onReport(300, 4, 0, 0, 0, 0, 0L); f.onReport(999, 4, 0, 0, 0, 0, 1000L)
        assertEquals(0, f.stalls)
        f.onReport(3500, 4, 0, 0, 0, 0, 2000L); f.onReport(14000, 4, 0, 0, 0, 0, 3000L); f.onReport(1200, 4, 0, 0, 0, 0, 4000L)
        assertEquals(3, f.stalls); assertEquals(14000L, f.stallWorstMs); assertEquals(1200L, f.stallLastMs)
        assertTrue(f.line(), f.line().contains("stalls over 1 s: 3 (longest 14000 ms, last 1200 ms)"))
    }

    @Test fun theOwnCostIsAnAverageAndAWorstCase() {
        val f = FilmHealth()
        f.onReport(0, 2, 0, 0, 0, 0, 0L); f.onReport(0, 10, 0, 0, 0, 0, 1000L); f.onReport(0, 6, 0, 0, 0, 0, 2000L)
        assertEquals(10L, f.ownWorstMs)
        assertTrue(f.line(), f.line().contains("average 6 ms, worst 10 ms"))
    }

    @Test fun aStreamThatStepsDownIsCountedAsAPictureChange() {
        val f = FilmHealth()
        f.onReport(0, 1, 0, 0, 1920, 1080, 0L); f.onReport(0, 1, 0, 0, 1920, 1080, 1000L)
        assertEquals(0, f.pictureChanges)
        f.onReport(0, 1, 0, 0, 640, 360, 2000L); f.onReport(0, 1, 0, 0, 0, 0, 3000L); f.onReport(0, 1, 0, 0, 640, 360, 4000L)
        assertEquals("one step (an unknown size between is nothing)", 1, f.pictureChanges)
    }

    @Test fun theRealVideoStatisticsGetOneLookEveryFifteenSecondsNotOnePerReport() {
        val f = FilmHealth()
        f.onReport(0, 1, 100, 0, 1920, 1080, 0L)
        var t = 0L; var tf = 100; var df = 0
        for (i in 1..14) { t += 1000L; tf += 30; df += if (i == 5) 3 else 0; f.onReport(0, 1, tf, df, 1920, 1080, t); assertNull("not before 15 s: $t", f.takeLook(false)) }
        t += 1000L; tf += 30; f.onReport(0, 1, tf, df, 1920, 1080, t)
        val l = f.takeLook(false)
        assertNotNull(l)
        assertEquals(450, l!!.frames); assertEquals(3, l.dropped); assertEquals(15, l.seconds); assertEquals(1920, l.w); assertEquals(1080, l.h)
        assertNull("what was taken is gone", f.takeLook(true))
        t += 5000L; tf += 150; f.onReport(0, 1, tf, df, 1920, 1080, t)
        val last = f.takeLook(true)                                    // the full screen ended: the rest is told too
        assertNotNull(last); assertEquals(150, last!!.frames); assertEquals(5, last.seconds)
    }

    @Test fun beforeAnyReportTheLineSaysSo() {
        assertEquals("page health: no report yet", FilmHealth().line())
    }
}
