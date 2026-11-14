package app.kestrel.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-BK2 (research/REVIEW-EXTERNAL-3.md section 3): Back and Forward put the page back where the owner left it, and the saved picture stays until the page is painted AND back at that place. */
class BackScrollTest {

    // ---- the place of every page the owner left (RAM only)

    @Test fun aPlaceIsKeptPerPageWithoutTheFragment() {
        val m = ScrollMemory()
        m.put("https://a.test/story#comments", 1800)
        assertEquals(1800, m.get("https://a.test/story"))
        assertEquals(1800, m.get("https://a.test/story#top"))
        assertNull(m.get("https://a.test/other"))
        assertNull(m.get("https://a.test/story?page=2"))      // a different query is a different page
    }

    @Test fun theLatestPlaceWinsAndNonsenseIsIgnored() {
        val m = ScrollMemory()
        m.put("https://a.test/", 300); m.put("https://a.test/", 950)
        assertEquals(950, m.get("https://a.test/"))
        m.put("https://a.test/", -1)                           // "unknown" must never overwrite a real place
        m.put("", 40)
        assertEquals(950, m.get("https://a.test/"))
        assertEquals(1, m.size)
    }

    @Test fun onlyTheLastThirtyTwoPagesAreKeptLeastRecentlyUsedOut() {
        val m = ScrollMemory()
        for (i in 1..32) m.put("https://a.test/p$i", i * 10)
        assertEquals(32, m.size)
        assertEquals(10, m.get("https://a.test/p1"))           // reading p1 makes it the most recent
        m.put("https://a.test/p33", 330)
        assertEquals(32, m.size)
        assertNull(m.get("https://a.test/p2"))                 // p2 was the least recently used
        assertEquals(10, m.get("https://a.test/p1"))
        assertEquals(330, m.get("https://a.test/p33"))
    }

    @Test fun clearForgetsEverything() {
        val m = ScrollMemory(); m.put("https://a.test/", 5); m.clear()
        assertNull(m.get("https://a.test/")); assertEquals(0, m.size)
    }

    // ---- the place the page may ask for after Back (the content script's pull)

    @Test fun theArmedPlaceIsOnlyAnsweredForThatPageAndOnlyForTenSeconds() {
        val t = BackTarget("https://a.test/story", 1800, until = 11_000L)
        assertEquals(1800, t.placeFor("https://a.test/story", 5_000L))
        assertEquals(1800, t.placeFor("https://a.test/story#c", 11_000L))   // the fragment does not matter, the last moment still counts
        assertEquals(-1, t.placeFor("https://a.test/story", 11_001L))      // expired
        assertEquals(-1, t.placeFor("https://evil.test/story", 5_000L))    // another page never learns the place
        assertEquals(-1, t.placeFor("https://a.test/story?x=1", 5_000L))
    }

    @Test fun aTargetIsBuiltFromTheMemoryAndExpiresAfterTheTimeToLive() {
        val m = ScrollMemory(); m.put("https://a.test/story", 700)
        val t = BackScrollRules.arm(m, "https://a.test/story#x", now = 1_000L)
        assertEquals(700, t.y); assertEquals(1_000L + BackScrollRules.TARGET_TTL_MS, t.until)
        assertEquals(0, BackScrollRules.arm(m, "https://unknown.test/", now = 0L).y)   // no memory: the top, which is what the cover already assumed
    }

    // ---- when the cover goes

    @Test fun theCoverNeverGoesBeforeThePaint() {
        assertFalse(BackScrollRules.coverGoes(painted = false, savedY = 0, currentY = 0, msSincePaint = 0))
        assertFalse(BackScrollRules.coverGoes(painted = false, savedY = 1800, currentY = 1800, msSincePaint = 5_000))
    }

    @Test fun aPageThatWasAtTheTopShowsAtFirstPaint() {
        assertTrue(BackScrollRules.coverGoes(painted = true, savedY = 0, currentY = 0, msSincePaint = 0))
    }

    @Test fun aPageThatWasScrolledStaysCoveredUntilItIsBackAtItsPlace() {
        // painted, but the engine has not scrolled yet (it paints the top first): do NOT show the top of the page
        assertFalse(BackScrollRules.coverGoes(true, 1800, 0, 100))
        assertFalse(BackScrollRules.coverGoes(true, 1800, 1700, 100))
        assertTrue(BackScrollRules.coverGoes(true, 1800, 1800, 100))
        assertTrue(BackScrollRules.coverGoes(true, 1800, 1803, 100))         // within the 4 px slop
        assertFalse(BackScrollRules.coverGoes(true, 1800, 1810, 100))
    }

    @Test fun theEndOfTheLoadAloneNeverHidesTheCoverButTheBoundAfterThePaintDoes() {
        // the caller passes stopped = false (see lookCheck): a load that ends while the page sits at y = 0 must not uncover the top
        assertFalse(BackScrollRules.coverGoes(true, 1800, 0, 1499))
        assertTrue(BackScrollRules.coverGoes(true, 1800, 0, 1500))
    }

    // ---- when the picture is put up

    @Test fun aTargetThatTheEngineCannotHaveKeptGetsItsPictureAtOnce() {
        assertEquals(150L, BackScrollRules.pictureDelayMs(notKept = false))
        assertEquals(0L, BackScrollRules.pictureDelayMs(notKept = true))
    }
}
