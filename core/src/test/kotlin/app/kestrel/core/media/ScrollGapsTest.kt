package app.kestrel.core.media

import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollGapsTest {
    @Test fun offAndEmptySaysHowToTurnItOn() {
        val l = ScrollGaps().line(false)
        assertTrue(l, l.startsWith("Real scrolling: off") && l.contains("Measure scrolling on real pages"))
    }
    @Test fun onAndEmptyAsksForAScroll() { assertTrue(ScrollGaps().line(true).contains("measuring")) }

    @Test fun burstsAddUpAndTheWorstSiteIsNamed() {
        val g = ScrollGaps()
        g.add("a.example", 100, 4, 1, 120)
        g.add("a.example", 100, 6, 3, 410)
        g.add("b.example", 200, 0, 0, 40)
        val l = g.line(true)
        assertTrue(l, l.contains("3 bursts on 2 site(s), 400 frames"))
        assertTrue(l, l.contains("gaps over 50 ms in 2.5 %, over 100 ms in 1.0 %"))   // 10 / 400 and 4 / 400
        assertTrue(l, l.contains("(worst: a.example 410 ms)"))
        assertTrue(l, l.contains("not the engine's own scrolling"))
    }

    @Test fun nonsenseIsIgnored() {
        val g = ScrollGaps()
        g.add("", 100, 1, 1, 100)
        g.add("x.example", 3, 0, 0, 10)            // too few frames
        g.add("x.example", 100, 5, 6, 10)          // more long gaps than gaps
        g.add("x.example", 100, 200, 1, 10)        // more gaps than frames
        g.add("x.example", 99999, 1, 1, 10)
        assertTrue(g.line(true).contains("measuring"))
    }

    @Test fun onlyTheNewestTwentySitesAreKept() {
        val g = ScrollGaps()
        for (i in 1..30) g.add("s$i.example", 60, 1, 0, 60)
        assertTrue(g.line(true), g.line(true).contains("on 20 site(s)"))
    }
}
