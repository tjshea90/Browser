package app.kestrel.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-BK1 (research/REVIEW-EXTERNAL-3.md section 3, BK1): what every Back / Forward press turned out to be, so the real hit rate is known before anything is tuned. */
class BackLabTest {
    private val B = "https://b.test/story"

    /** One press and its page's own report; [at] is the time of the press. */
    private fun press(lab: BackLab, at: Long = 1_000L, saved: Int = 1800, url: String = B) = lab.press(at, url, forward = false, savedY = saved)

    @Test fun aPageTheEngineKeptIsAnInstantHit() {
        val lines = ArrayList<String>(); val lab = BackLab { lines += it }
        press(lab)
        lab.pageshow(1_030L, B, persisted = true, navType = "back_forward", transferSize = 0, y = 1800)
        lab.finish(4_000L)
        assertEquals(1, lab.stats.presses); assertEquals(1, lab.stats.kept); assertEquals(0, lab.stats.fromCache); assertEquals(0, lab.stats.fromNetwork)
        assertEquals(1, lab.stats.scrollKnown); assertEquals(1, lab.stats.scrollOk)       // the place at the show is a sample too: the engine's cache keeps it exact
        assertEquals(1, lines.size); assertTrue(lines[0], lines[0].startsWith("kind=kept "))
    }

    @Test fun aPageLoadedAgainFromTheHttpCacheIsToldApartFromOneLoadedFromTheNetwork() {
        val lab = BackLab {}
        press(lab); lab.pageshow(1_600L, B, persisted = false, navType = "back_forward", transferSize = 0, y = 0)
        press(lab, at = 10_000L); lab.pageshow(11_900L, B, persisted = false, navType = "back_forward", transferSize = 48_000, y = 0)
        lab.finish(20_000L)
        assertEquals(2, lab.stats.presses); assertEquals(0, lab.stats.kept); assertEquals(1, lab.stats.fromCache); assertEquals(1, lab.stats.fromNetwork)
    }

    @Test fun aPageTheEngineLoadedAsANewNavigationIsOtherNotACacheHit() {
        val lab = BackLab {}
        press(lab); lab.pageshow(1_500L, B, persisted = false, navType = "navigate", transferSize = 1000, y = 0)
        lab.finish(5_000L)
        assertEquals(1, lab.stats.other); assertEquals(0, lab.stats.kept + lab.stats.fromCache + lab.stats.fromNetwork)
    }

    @Test fun aPressWithNoReportIsOtherAndAReportForAnotherPageIsIgnored() {
        val lab = BackLab {}
        press(lab); lab.pageshow(1_200L, "https://elsewhere.test/", persisted = true, navType = "back_forward", transferSize = 0, y = 0)  // a redirect or a late old page
        lab.finish(9_000L)
        assertEquals(1, lab.stats.presses); assertEquals(1, lab.stats.other); assertEquals(0, lab.stats.kept)
    }

    @Test fun aNewPressClosesTheOpenOneAndTheFirstReportCountsOnce() {
        val lab = BackLab {}
        press(lab, at = 1_000L)
        lab.pageshow(1_100L, B, true, "back_forward", 0, 1800)
        lab.pageshow(1_150L, B, false, "back_forward", 900, 0)     // a second report for the same press must not count twice
        press(lab, at = 2_000L, url = "https://c.test/")           // the next press ends the first
        lab.finish(9_000L)
        assertEquals(2, lab.stats.presses); assertEquals(1, lab.stats.kept); assertEquals(0, lab.stats.fromNetwork); assertEquals(1, lab.stats.other)
    }

    @Test fun timeToFirstPaintIsTheMedianOfTheReloadsOnly() {
        val lab = BackLab {}
        for ((i, ms) in listOf(700L, 300L, 1_900L).withIndex()) {
            val t0 = 1_000L + i * 10_000L
            press(lab, at = t0); lab.pageshow(t0 + 50, B, false, "back_forward", 0, 0); lab.painted(t0 + ms)
        }
        press(lab, at = 50_000L); lab.pageshow(50_020L, B, true, "back_forward", 0, 1800)    // a kept page paints nothing new: not part of the paint numbers
        lab.finish(60_000L)
        assertEquals(700L, lab.stats.medianPaintMs())
    }

    @Test fun theMedianOfAnEvenCountIsTheMiddlePair() {
        val lab = BackLab {}
        for ((i, ms) in listOf(300L, 500L).withIndex()) { val t0 = 1_000L + i * 10_000L; press(lab, at = t0); lab.pageshow(t0 + 10, B, false, "back_forward", 0, 0); lab.painted(t0 + ms) }
        lab.finish(30_000L)
        assertEquals(400L, lab.stats.medianPaintMs())
    }

    @Test fun theScrollPlaceIsJudgedFromTheLatestReportAfterTheShow() {
        val lab = BackLab {}
        press(lab, saved = 1800); lab.pageshow(1_500L, B, false, "back_forward", 0, 0); lab.painted(1_700L)
        lab.scrolled(1_900L, 0)                                    // the engine painted the top first
        lab.scrolled(2_600L, 1_799)                                // then the place was reached (or restored)
        press(lab, at = 20_000L, saved = 900); lab.pageshow(20_400L, B, false, "back_forward", 0, 0); lab.painted(20_500L)
        lab.scrolled(21_000L, 0)                                   // this one stayed at the top
        lab.finish(30_000L)
        assertEquals(2, lab.stats.scrollKnown); assertEquals(1, lab.stats.scrollOk)
    }

    @Test fun anUnknownPlaceIsNotCountedEitherWay() {
        val lab = BackLab {}
        press(lab, saved = -1); lab.pageshow(1_100L, B, false, "back_forward", 0, 0); lab.scrolled(2_000L, 400)
        lab.finish(9_000L)
        assertEquals(0, lab.stats.scrollKnown); assertEquals(0, lab.stats.scrollOk)
    }

    @Test fun aTopOfThePageThatStaysAtTheTopIsKept() {
        val lab = BackLab {}
        press(lab, saved = 0); lab.pageshow(1_100L, B, false, "back_forward", 0, 0); lab.scrolled(2_000L, 2)
        lab.finish(9_000L)
        assertEquals(1, lab.stats.scrollKnown); assertEquals(1, lab.stats.scrollOk)
    }

    // ---- facts about pages that were left (they decide whether the picture goes up at once)

    @Test fun aPageThatWasStillLoadingWhenLeftIsKnownNotToBeKept() {
        val lab = BackLab {}
        lab.left("https://a.test/loading#x", loading = true)
        lab.left("https://a.test/done", loading = false)
        assertTrue(lab.notKept("https://a.test/loading"))
        assertFalse(lab.notKept("https://a.test/done"))
        assertFalse(lab.notKept("https://a.test/never-seen"))
    }

    @Test fun aNoStorePageAndAStreamPlayerPageAreKnownNotToBeKept() {
        val lab = BackLab {}
        lab.noStore("https://bank.test/account")
        lab.left("https://bank.test/account", loading = false)
        lab.left("https://movies.test/watch", loading = false, stream = true)
        assertTrue(lab.notKept("https://bank.test/account#top"))
        assertTrue(lab.notKept("https://movies.test/watch"))
        assertTrue("seen before it was left", lab.noStoreSeen("https://bank.test/account"))
    }

    @Test fun theKnowledgeIsBoundedAndANewerAnswerReplacesAnOlderOne() {
        val lab = BackLab {}
        lab.left("https://a.test/p", loading = true); assertTrue(lab.notKept("https://a.test/p"))
        lab.left("https://a.test/p", loading = false); assertFalse("the page finished loading when it was left the second time", lab.notKept("https://a.test/p"))
        for (i in 1..80) lab.left("https://a.test/n$i", loading = true)
        assertFalse("only the most recent pages are remembered", lab.notKept("https://a.test/n1"))
        assertTrue(lab.notKept("https://a.test/n80"))
    }

    @Test fun aPageLeftWithRequestsStillOpenIsKnownNotToBeKeptWhateverTheLeaveSaid() {
        val lab = BackLab {}
        lab.left("https://a.test/feed", loading = false)       // the engine said the page had finished loading when it was left
        assertFalse(lab.notKept("https://a.test/feed"))
        lab.pending("https://a.test/feed#x", 3)                // but the extension saw 3 requests still open when the next page began
        assertTrue(lab.notKept("https://a.test/feed"))
        lab.pending("https://a.test/other", 0)                 // nothing open: nothing to remember
        lab.pending("", 5)
        assertFalse(lab.notKept("https://a.test/other"))
    }

    @Test fun theReasonAPageCannotBeKeptIsRemembered() {
        val lab = BackLab {}
        lab.left("https://a.test/1", loading = true); assertEquals("loading", lab.notKeptWhy("https://a.test/1"))
        lab.left("https://a.test/2", loading = false, stream = true); assertEquals("stream", lab.notKeptWhy("https://a.test/2"))
        lab.noStore("https://a.test/3"); lab.left("https://a.test/3", loading = false); assertEquals("no-store", lab.notKeptWhy("https://a.test/3"))
        lab.left("https://a.test/4", loading = false); lab.pending("https://a.test/4", 2); assertEquals("pending 2", lab.notKeptWhy("https://a.test/4"))
        lab.left("https://a.test/5", loading = false); assertEquals("", lab.notKeptWhy("https://a.test/5"))
        assertEquals("", lab.notKeptWhy("https://a.test/never-seen"))
        lab.left("https://a.test/1", loading = false); assertEquals("a newer answer replaces the older reason", "", lab.notKeptWhy("https://a.test/1"))
    }

    @Test fun theTimeTheTargetAddressShowedIsInTheLine() {
        val lines = ArrayList<String>(); val lab = BackLab { lines += it }
        press(lab, at = 1_000L); lab.located(1_250L); lab.located(1_900L)          // the first sighting counts
        lab.pageshow(4_000L, B, true, "back_forward", 0, 1800); lab.finish(8_000L)
        assertTrue(lines[0], lines[0].contains(" loc=250 "))
        assertTrue(lines[0], lines[0].contains(" show=3000 "))
    }

    @Test fun aPressThatNeverSawItsAddressSaysSo() {
        val lines = ArrayList<String>(); val lab = BackLab { lines += it }
        press(lab); lab.finish(5_000L)
        assertTrue(lines[0], lines[0].contains(" loc=-1 "))
    }

    // ---- the Diagnostics line

    @Test fun theLineSaysPlainlyWhatHappened() {
        val lab = BackLab {}
        assertEquals("Back: no presses yet", lab.stats.line())
        for (i in 0 until 4) {
            val t0 = 1_000L + i * 10_000L
            press(lab, at = t0, saved = 1000)
            when (i) {
                0, 1 -> lab.pageshow(t0 + 20, B, true, "back_forward", 0, 1000)
                2 -> { lab.pageshow(t0 + 30, B, false, "back_forward", 0, 0); lab.painted(t0 + 500); lab.scrolled(t0 + 1_500, 1000) }
                else -> { lab.pageshow(t0 + 30, B, false, "back_forward", 60_000, 0); lab.painted(t0 + 900); lab.scrolled(t0 + 2_500, 0) }
            }
        }
        lab.finish(60_000L)
        assertEquals("Back: 4 presses, 50% kept (instant), 25% reloaded from the cache, 25% from the network, median 700 ms to first paint, scroll kept in 3 of 4", lab.stats.line())
    }

    @Test fun nothingIsKeptOrCountedAfterReset() {
        val lab = BackLab {}
        press(lab); lab.pageshow(1_100L, B, true, "back_forward", 0, 1800); lab.finish(5_000L)
        lab.left("https://a.test/", loading = true)
        lab.reset()
        assertEquals(0, lab.stats.presses); assertFalse(lab.notKept("https://a.test/")); assertNull(lab.openKey())
    }
}
