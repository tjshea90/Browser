package app.kestrel.browser.download

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
}
