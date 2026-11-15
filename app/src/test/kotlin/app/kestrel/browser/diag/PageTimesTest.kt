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
}
