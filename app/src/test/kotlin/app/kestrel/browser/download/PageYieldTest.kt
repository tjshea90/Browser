package app.kestrel.browser.download

import app.kestrel.core.net.NetProfile
import org.junit.Assert.assertEquals
import org.junit.Test

/** Downloads step aside while the page in front loads (the one place the owner's limit and the page rule meet). */
class PageYieldTest {
    private val page = DownloadController.PAGE_KBPS

    @Test fun withoutAPageLoadingTheOwnersLimitIsUsedAsIs() {
        assertEquals(0, DownloadController.effectiveKBps(0, pageBusy = false, yieldToPages = true))
        assertEquals(1024, DownloadController.effectiveKBps(1024, pageBusy = false, yieldToPages = true))
    }

    @Test fun whileAPageLoadsNoLimitBecomesThePageRate() {
        assertEquals(page, DownloadController.effectiveKBps(0, pageBusy = true, yieldToPages = true))
    }

    @Test fun aLowerOwnerLimitStaysLowerAndAHigherOneIsCut() {
        assertEquals(100, DownloadController.effectiveKBps(100, pageBusy = true, yieldToPages = true))
        assertEquals(page, DownloadController.effectiveKBps(2048, pageBusy = true, yieldToPages = true))
    }

    @Test fun theSwitchOffMeansNoYielding() {
        assertEquals(0, DownloadController.effectiveKBps(0, pageBusy = true, yieldToPages = false))
        assertEquals(500, DownloadController.effectiveKBps(500, pageBusy = true, yieldToPages = false))
    }

    @Test fun neverNegative() {
        assertEquals(0, DownloadController.effectiveKBps(-5, pageBusy = false, yieldToPages = true))
        assertEquals(page, DownloadController.effectiveKBps(-5, pageBusy = true, yieldToPages = true))
    }

    // ---- AB3: behind LinkMeter's tunnel the network profile asks for a bigger page rate

    @Test fun behindLinkMetersTunnelALoadingPageLeavesTheDownloadMoreRoomAndTheOwnersLowerLimitStillWins() {
        val fast = NetProfile.FAST_PAGE_YIELD_KBPS
        assertEquals(fast, DownloadController.effectiveKBps(0, pageBusy = true, yieldToPages = true, pageKBps = fast))
        assertEquals("an owner limit above the page rate is cut to it", fast, DownloadController.effectiveKBps(5000, pageBusy = true, yieldToPages = true, pageKBps = fast))
        assertEquals("a lower owner limit stays", 300, DownloadController.effectiveKBps(300, pageBusy = true, yieldToPages = true, pageKBps = fast))
        assertEquals("no page loading, no cut", 0, DownloadController.effectiveKBps(0, pageBusy = false, yieldToPages = true, pageKBps = fast))
        assertEquals("the owner's switch off means no yielding at all", 0, DownloadController.effectiveKBps(0, pageBusy = true, yieldToPages = false, pageKBps = fast))
        assertEquals("the default rate is the hotspot's", page, DownloadController.effectiveKBps(0, pageBusy = true, yieldToPages = true))
    }

    @Test fun theProfilesRateIsKeptInsideSaneBounds() {
        try {
            DownloadController.setPageYieldKBps(NetProfile.FAST_PAGE_YIELD_KBPS); assertEquals(NetProfile.FAST_PAGE_YIELD_KBPS, DownloadController.pageYieldKBps)
            DownloadController.setPageYieldKBps(1); assertEquals("never starve a download to nothing", 50, DownloadController.pageYieldKBps)
            DownloadController.setPageYieldKBps(1_000_000); assertEquals(4000, DownloadController.pageYieldKBps)
            DownloadController.setPageYieldKBps(NetProfile.PAGE_YIELD_KBPS); assertEquals(page, DownloadController.pageYieldKBps)
        } finally { DownloadController.setPageYieldKBps(page) }
    }

    // ---- RVW3 PB3: downloads also step aside while Kestrel's own player plays a film

    @Test fun whileAFilmPlaysNoLimitBecomesTheFilmRateWhichIsMoreThanThePageRateOnTheCappedHotspot() {
        val film = DownloadController.FILM_KBPS
        assertEquals(250, film)
        org.junit.Assert.assertTrue("a film leaves downloads more than a loading page does on the capped hotspot", film > page)
        assertEquals(film, DownloadController.effectiveKBps(0, pageBusy = false, yieldToPages = true, filmBusy = true))
        assertEquals("the owner's higher limit is cut to it", film, DownloadController.effectiveKBps(2048, pageBusy = false, yieldToPages = true, filmBusy = true))
        assertEquals("the owner's lower limit still wins", 100, DownloadController.effectiveKBps(100, pageBusy = false, yieldToPages = true, filmBusy = true))
    }

    @Test fun aPageLoadingWhileAFilmPlaysTakesTheStricterOfTheTwo() {
        assertEquals(page, DownloadController.effectiveKBps(0, pageBusy = true, yieldToPages = true, filmBusy = true))
        assertEquals(page, DownloadController.effectiveKBps(2048, pageBusy = true, yieldToPages = true, filmBusy = true))
        assertEquals(100, DownloadController.effectiveKBps(100, pageBusy = true, yieldToPages = true, filmBusy = true))
    }

    @Test fun theSwitchOffMeansNoYieldingToAFilmEither() {
        assertEquals(0, DownloadController.effectiveKBps(0, pageBusy = false, yieldToPages = false, filmBusy = true))
        assertEquals(500, DownloadController.effectiveKBps(500, pageBusy = true, yieldToPages = false, filmBusy = true))
    }

    @Test fun behindTheFastTunnelAFilmLeavesAsMuchAsALoadingPageDoes() {
        val fast = NetProfile.FAST_PAGE_YIELD_KBPS
        assertEquals(fast, DownloadController.effectiveKBps(0, pageBusy = false, yieldToPages = true, pageKBps = fast, filmBusy = true))
        assertEquals(fast, DownloadController.effectiveKBps(5000, pageBusy = false, yieldToPages = true, pageKBps = fast, filmBusy = true))
    }

    @Test fun noFilmAndNoPageChangesNothing() {
        assertEquals(0, DownloadController.effectiveKBps(0, pageBusy = false, yieldToPages = true, filmBusy = false))
        assertEquals(1024, DownloadController.effectiveKBps(1024, pageBusy = false, yieldToPages = true, filmBusy = false))
        assertEquals("a negative owner limit means no limit, so the film rate applies", DownloadController.FILM_KBPS, DownloadController.effectiveKBps(-5, pageBusy = false, yieldToPages = true, filmBusy = true))
    }
}
