package app.kestrel.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PERF17b (owner 2026-11-24: "Do the lazy restore fix if it is logical and what major browsers do"): the rule that decides when the page in front waits for a tap. */
class LazyStartTest {
    private val hour = 3_600_000L
    private fun ok(away: Long = 5 * hour, url: String = "https://example.org/a", link: Boolean = false, fresh: Boolean = true, home: Boolean = false, on: Boolean = true) =
        LazyStart.applies(on, away, url, link, fresh, home)

    @Test fun aBreakOfFourHoursOrMoreMakesThePageWaitAndAShorterOneDoesNot() {
        assertTrue(ok(away = 4 * hour))
        assertTrue(ok(away = 30 * hour))
        assertFalse(ok(away = 4 * hour - 1))
        assertFalse(ok(away = 10 * 60_000L))
        assertFalse("never stamped or the clock went back: the page loads as before", ok(away = 0L))
    }

    @Test fun everyOtherConditionKeepsTheOldBehaviour() {
        assertFalse("switched off in Settings", ok(on = false))
        assertFalse("started by a link or a share: that page is wanted now", ok(link = true))
        assertFalse("the system re-creating the screen of a running app", ok(fresh = false))
        assertFalse("already left on Home", ok(home = true))
        assertFalse("not a web page", ok(url = "about:blank"))
        assertFalse(ok(url = ""))
        assertFalse(ok(url = "file:///sdcard/x.html"))
        assertTrue(ok(url = "http://example.org/"))
    }

    @Test fun theAwayTimeIsZeroWhenUnknownOrWhenTheClockWentBack() {
        assertEquals(0L, LazyStart.awayMs(0L, 1_000_000L))
        assertEquals(0L, LazyStart.awayMs(2_000_000L, 1_000_000L))
        assertEquals(5 * hour, LazyStart.awayMs(1_000L, 1_000L + 5 * hour))
    }

    @Test fun theContinueLineNamesThePageShortly() {
        assertEquals("Continue: My chat", LazyStart.label("  My   chat ", "https://claude.example/chat/1"))
        assertEquals("Continue: shop.example", LazyStart.label("", "https://www.shop.example/a?b=1#c"))
        assertEquals("Continue: the last page", LazyStart.label("", ""))
        val long = LazyStart.label("x".repeat(200), "https://a.example/")
        assertTrue(long, long.length <= "Continue: ".length + LazyStart.MAX_TITLE && long.endsWith("…"))
    }

    @Test fun theBreakIsDescribedInPlainWords() {
        assertEquals("no break known", LazyStart.describe(0L))
        assertEquals("12 min", LazyStart.describe(12 * 60_000L))
        assertEquals("5 h 30 min", LazyStart.describe(5 * hour + 30 * 60_000L))
    }
}
