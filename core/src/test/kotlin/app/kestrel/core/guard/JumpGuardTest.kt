package app.kestrel.core.guard

import app.kestrel.core.guard.JumpGuard.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class JumpGuardTest {
    private val ad = "https://ad-landing.example/promo"

    @Test fun aJumpOnTheSameSiteIsNeverJudged() {
        assertEquals(Verdict.ALLOW, JumpGuard.decide(crossSite = false, touchAgeMs = 10, tapArrived = true, tapAgeMs = 10, tapHref = "", uri = ad))
    }

    @Test fun aTapOnThePlayLayerFollowedByAJumpToAnotherSiteIsStopped() {
        assertEquals(Verdict.STOP, JumpGuard.decide(true, 40, true, 30, "", ad))
        assertEquals("the tap was on a link to somewhere else", Verdict.STOP, JumpGuard.decide(true, 40, true, 30, "https://news.example/story", ad))
    }

    @Test fun tappingALinkAndGoingToThatLinkIsNeverStopped() {
        assertEquals(Verdict.ALLOW, JumpGuard.decide(true, 40, true, 30, ad, ad))
        assertEquals("a fragment does not make it another address", Verdict.ALLOW, JumpGuard.decide(true, 40, true, 30, "$ad#top", ad))
    }

    @Test fun withoutAnyRecentTouchNothingIsJudged() {
        assertEquals(Verdict.ALLOW, JumpGuard.decide(true, 2600, true, 2600, "", ad))
        assertEquals("a timer redirect long after the last touch", Verdict.ALLOW, JumpGuard.decide(true, 60_000, false, 60_000, "", ad))
    }

    @Test fun whenTheTapReportHasNotArrivedYetTheJumpWaitsForIt() {
        assertEquals(Verdict.WAIT, JumpGuard.decide(true, 30, false, 5000, "", ad))
    }

    @Test fun anOldTapReportDoesNotStopAJumpThatFollowsALaterTouch() {
        assertEquals(Verdict.ALLOW, JumpGuard.decide(true, 100, true, 2600, "", ad))
    }

    @Test fun theWindowsAreTheOnesTheScriptAndTheTestsAssume() {
        assertEquals(2500L, JumpGuard.TAP_WINDOW_MS)
        assertEquals(150L, JumpGuard.WAIT_MS)
    }
}
