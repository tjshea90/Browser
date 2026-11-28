package app.kestrel.core.guard

import app.kestrel.core.guard.PopupGuard.Verdict
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-G5 (research/REVIEW-EXTERNAL-3.md part G): the app-level wall for new windows uses the tap evidence, like the page-side guard. */
class PopupGuardTest {
    private val ad = "https://ads.adnetwork.example/landing"
    private fun d(sameSite: Boolean = false, touchAge: Long = 40, arrived: Boolean = true, tapAge: Long = 30, tapHref: String = "", uri: String = ad, auth: Boolean = false, ready: Boolean = true) =
        PopupGuard.decide(sameSite, touchAge, arrived, tapAge, tapHref, uri, auth, ready)

    @Test fun aWindowWithNoTouchBehindItIsStopped() {
        assertEquals(Verdict.STOP, d(touchAge = 2600))
        assertEquals("even on the same site: timers and page load do not open windows", Verdict.STOP, d(sameSite = true, touchAge = 60_000))
    }

    @Test fun aWindowOnTheSameSiteIsAllowedRightAfterATouch() {
        assertEquals(Verdict.ALLOW, d(sameSite = true))
    }

    @Test fun signInAndPaymentWindowsAreAllowedFromAnySite() {
        assertEquals(Verdict.ALLOW, d(auth = true, uri = "https://accounts.google.com/o/oauth2/auth"))
    }

    @Test fun aWindowForTheVeryLinkThatWasTappedIsAllowed() {
        assertEquals(Verdict.ALLOW, d(tapHref = ad))
        assertEquals("a fragment does not make it another address", Verdict.ALLOW, d(tapHref = "$ad#top"))
    }

    @Test fun aWindowOnAnotherSiteAfterATapThatWasNotOnThatLinkIsStopped() {
        assertEquals("the tap was on a button, not a link", Verdict.STOP, d(tapHref = ""))
        assertEquals("the tap was on another link", Verdict.STOP, d(tapHref = "https://news.example/story"))
        assertEquals("the report belongs to an earlier touch", Verdict.STOP, d(tapAge = 2600, tapHref = ad))
    }

    @Test fun whenTheTapReportHasNotArrivedYetTheWindowWaitsForIt() {
        assertEquals(Verdict.WAIT, d(arrived = false, tapAge = 5000))
        assertEquals("a window that needs no tap evidence does not wait", Verdict.ALLOW, d(sameSite = true, arrived = false))
        assertEquals(Verdict.ALLOW, d(auth = true, arrived = false))
    }

    @Test fun whenThePageNeverAnsweredTheGuardCrossSiteWindowsStopButTheRestStaysAllowed() {
        assertEquals(Verdict.STOP, d(ready = false, tapHref = ad))
        assertEquals(Verdict.ALLOW, d(ready = false, sameSite = true))
        assertEquals(Verdict.ALLOW, d(ready = false, auth = true))
    }

    @Test fun theSignInHostsAreAnchoredSoALookAlikeIsNotOne() {
        assertTrue(PopupGuard.isAuthHost("accounts.google.com"))
        assertTrue(PopupGuard.isAuthHost("login.microsoftonline.com"))
        assertTrue(PopupGuard.isAuthHost("www.paypal.com"))
        assertTrue("a subdomain of a listed host", PopupGuard.isAuthHost("secure.paypal.com"))
        assertFalse(PopupGuard.isAuthHost("accounts.google.com.evil.example"))
        assertFalse(PopupGuard.isAuthHost("evilpaypal.com.example"))
        assertFalse(PopupGuard.isAuthHost("ads.adnetwork.example"))
    }

    @Test fun theKotlinCopyOfTheSignInHostsEqualsTheOneInThePageGuard() {
        val js = File("../geckoapp/src/main/assets/kestrel-shields/guard-main.js")
        assertTrue("run from the core module: ${js.absolutePath}", js.isFile)
        val line = js.readLines().first { it.trimStart().startsWith("var AUTH = /") }
        val source = line.substringAfter("var AUTH = /").substringBeforeLast("/;")
        assertEquals("guard-main.js AUTH and PopupGuard.AUTH_SOURCE must stay equal (a sign-in pop-up would break otherwise)", source, PopupGuard.AUTH_SOURCE)
    }
}
