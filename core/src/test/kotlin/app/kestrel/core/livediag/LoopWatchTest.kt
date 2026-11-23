package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Test

/** PERF12: the same site loaded again and again in one tab is named once, then again after every further batch; calm browsing never is. */
class LoopWatchTest {
    @Test fun sixLoadsOfOneSiteInAMinuteAreNamedOnceAndThenAgainAfterSixMore() {
        val w = LoopWatch()
        val tab = Any()
        var said = ArrayList<Int>()
        for (i in 0 until 14) { val n = w.hit(tab, "ads.example", i * 4_000L); if (n > 0) said.add(n) }
        assertEquals("said at the 6th load, and again after six more would need 12 in the window (14 loads in 52 s)", listOf(6, 12), said)
    }

    @Test fun calmBrowsingNeverSaysAnything() {
        val w = LoopWatch()
        val tab = Any()
        for (i in 0 until 40) assertEquals(0, w.hit(tab, "site$i.example", i * 3_000L))             // a different site every time
        for (i in 0 until 40) assertEquals(0, w.hit(tab, "news.example", 200_000L + i * 20_000L))   // the same site, one load every 20 s: never 6 inside a minute
    }

    @Test fun eachTabIsWatchedOnItsOwnAndAnEmptyHostIsIgnored() {
        val w = LoopWatch()
        val a = Any(); val b = Any()
        for (i in 0 until 5) { w.hit(a, "x.example", i * 1_000L); w.hit(b, "x.example", i * 1_000L) }
        assertEquals("five in each of two tabs is not six in one", 0, w.hit(Any(), "", 6_000L))
        assertEquals(6, w.hit(a, "x.example", 5_000L))
        assertEquals(5, w.spanSeconds(a))
        assertEquals(6, w.hit(b, "x.example", 5_000L))
    }
}
