package app.kestrel.browser.diag

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SOAK1: the page-load numbers of Diagnostics. A fast page paints AFTER its load ended: that paint is still recorded (it used to show as "paint -"). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PageTimesTest {
    @Test fun aPaintThatComesAfterTheEndOfTheLoadIsStillRecordedAndSaid() {
        val key = Any()
        PageTimes.start(key, "https://fast.example/page", false)
        PageTimes.stop(key, 2)
        assertTrue("before the paint: no paint time yet", PageTimes.lines().any { it.contains("fast.example") && it.contains("paint -") })
        PageTimes.paint(key)
        val line = PageTimes.lines().first { it.contains("fast.example") }
        assertFalse(line, line.contains("paint -"))
        assertTrue("the Health list says it too", Health.ring.last(20).any { it.contains("page painted host=fast.example") })
    }

    @Test fun aPrivateTabIsNeverRecorded() {
        val key = Any()
        PageTimes.start(key, "https://secret.example/", true)
        PageTimes.stop(key, 0); PageTimes.paint(key)
        assertFalse(PageTimes.lines().any { it.contains("secret.example") })
    }

    @Test fun theFinishedLoadLineNamesTheHostAndNothingElse() {
        val key = Any()
        PageTimes.start(key, "https://www.news.example/story?id=77&token=abc", false)
        PageTimes.paint(key); PageTimes.stop(key, 5)
        val l = Health.ring.last(20).last { it.contains("page loaded") }
        assertTrue(l, l.contains("host=www.news.example") && !l.contains("story") && !l.contains("token"))
    }

    /** PERF12: a load cut short (a redirect hop, a tap on a link) says `cut=1` next to ok=false; a load that really failed does not. */
    @Test fun aLoadCutShortIsSaidSoAndARealFailureIsNot() {
        val a = Any(); val b = Any()
        PageTimes.start(a, "https://cut.example/", false); PageTimes.stop(a, 0, ok = false, cut = true)
        PageTimes.start(b, "https://broken.example/", false); PageTimes.stop(b, 0, ok = false, cut = false)
        val ring = Health.ring.last(30)
        assertTrue(ring.any { it.contains("page loaded host=cut.example") && it.contains("ok=false cut=1") })
        assertTrue(ring.any { it.contains("page loaded host=broken.example") && it.endsWith("ok=false") })
    }

    /** PERF12: the same site loaded six times in one tab inside a minute is named in one `page loop` line, and the count of it is kept. */
    @Test fun aSiteLoadedAgainAndAgainIsNamedInOneLoopLine() {
        val key = Any()
        for (i in 0 until 7) { PageTimes.start(key, "https://hop.example/step$i", false); PageTimes.stop(key, 0) }
        val loops = Health.ring.last(60).filter { it.contains("page loop host=hop.example") }
        assertTrue(loops.toString(), loops.size == 1 && loops[0].contains("n=6"))
    }
}
