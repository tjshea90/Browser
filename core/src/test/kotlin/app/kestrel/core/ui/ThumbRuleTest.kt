package app.kestrel.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbRuleTest {
    private val a = "https://news.example/story/1"
    private val b = "https://shop.example/cart"

    @Test fun aPictureOfTheSamePageThatHasSettledIsKept() {
        assertTrue(ThumbRule.accept(ThumbRule.pageOf(a), a, 100L, 100L, false))
        assertTrue("a #fragment is still the same page", ThumbRule.accept(ThumbRule.pageOf(a), "$a#comments", 100L, 100L, false))
    }

    @Test fun aPictureThatArrivesAfterTheTabMovedOnIsThrownAway() {
        assertFalse("the tab is on another page now", ThumbRule.accept(ThumbRule.pageOf(a), b, 100L, 100L, false))
        assertFalse("a new load started since the request (even to the same address)", ThumbRule.accept(ThumbRule.pageOf(a), a, 100L, 250L, false))
        assertFalse("the new page is still loading: the surface may still show the old page", ThumbRule.accept(ThumbRule.pageOf(a), a, 100L, 100L, true))
        assertFalse("another query is another page", ThumbRule.accept(ThumbRule.pageOf("https://tube.example/watch?v=1"), "https://tube.example/watch?v=2", 1L, 1L, false))
    }

    @Test fun noPreviewIsKeptForTheNewTabPageOrNonWebAddresses() {
        for (u in listOf("", "about:blank", "data:text/html,x", "file:///sdcard/x.html")) { assertEquals(u, "", ThumbRule.pageOf(u)); assertFalse(u, ThumbRule.accept(ThumbRule.pageOf(u), u, 1L, 1L, false)) }
    }

    @Test fun aStoredPreviewIsShownOnlyForThePageItWasTakenFor() {
        assertTrue(ThumbRule.stillShows(ThumbRule.pageOf(a), a))
        assertFalse(ThumbRule.stillShows(ThumbRule.pageOf(a), b))
        assertFalse("a preview stored before this rule (no record of its page) cannot be trusted", ThumbRule.stillShows(null, a))
        assertFalse(ThumbRule.stillShows("", a))
    }
}
