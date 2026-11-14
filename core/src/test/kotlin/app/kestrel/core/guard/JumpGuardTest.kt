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

    // ---- RX3-G6: a scripted jump to a host the lists block as an ad or tracker is stopped at ANY delay

    private val adHostUri = "https://ads.adnetwork.example/landing"

    @Test fun aJumpToAListedAdHostLongAfterTheLastTouchIsStopped() {
        assertEquals("play, wait 5 s, the page jumps to an ad", Verdict.STOP, JumpGuard.decide(true, 5000, true, 5000, "", adHostUri, adHost = true))
        assertEquals(Verdict.STOP, JumpGuard.decide(true, 600_000, false, 600_000, "", adHostUri, adHost = true))
        assertEquals("the same jump to a host that is not listed is still not this guard's business", Verdict.ALLOW, JumpGuard.decide(true, 5000, true, 5000, "", adHostUri, adHost = false))
    }

    @Test fun aTapOnALinkToExactlyThatAddressStillOpensIt() {
        assertEquals(Verdict.ALLOW, JumpGuard.decide(true, 40, true, 30, adHostUri, adHostUri, adHost = true))
        assertEquals(Verdict.ALLOW, JumpGuard.decide(true, 40, true, 30, "$adHostUri#x", adHostUri, adHost = true))
        assertEquals("a tap on another link does not excuse it", Verdict.STOP, JumpGuard.decide(true, 40, true, 30, "https://news.example/story", adHostUri, adHost = true))
        assertEquals("a tap that was not on a link does not either", Verdict.STOP, JumpGuard.decide(true, 40, true, 30, "", adHostUri, adHost = true))
    }

    @Test fun aTapThatIsTooOldDoesNotExcuseTheJump() {
        assertEquals(Verdict.STOP, JumpGuard.decide(true, 100, true, 2600, adHostUri, adHostUri, adHost = true))   // the report belongs to an earlier touch
        assertEquals(Verdict.STOP, JumpGuard.decide(true, 3000, true, 3000, adHostUri, adHostUri, adHost = true))
    }

    @Test fun withATouchJustNowTheJumpToAnAdHostWaitsForTheTapReport() {
        assertEquals(Verdict.WAIT, JumpGuard.decide(true, 30, false, 5000, "", adHostUri, adHost = true))
    }

    @Test fun aJumpWithinTheSameSiteIsNeverJudgedEvenWhenThatHostIsListed() {
        assertEquals(Verdict.ALLOW, JumpGuard.decide(false, 5000, true, 5000, "", adHostUri, adHost = true))
    }
}
