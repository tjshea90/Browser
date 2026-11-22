package app.kestrel.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RVW3 PB1: the plan for a link that drops in the middle of a film. */
class RetryPlanTest {
    @Test fun fourAutomaticTriesWithGrowingWaitsThenItGivesUp() {
        assertEquals(4, RetryPlan.MAX_TRIES)
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 8_000L), (0 until RetryPlan.MAX_TRIES).map { RetryPlan.delayMs(it) })
        assertEquals("used up", -1L, RetryPlan.delayMs(4))
        assertEquals(-1L, RetryPlan.delayMs(99))
        assertEquals("a negative index is no try", -1L, RetryPlan.delayMs(-1))
    }

    @Test fun theWaitsNeverGetShorterAndTheWholeEffortIsLongEnoughForARealDropout() {
        val waits = (0 until RetryPlan.MAX_TRIES).map { RetryPlan.delayMs(it) }
        assertEquals(waits.sorted(), waits)
        assertTrue("at least 10 s of waiting on top of the load retries", waits.sum() >= 10_000L)
        assertTrue("but not minutes of a spinner before it says so", waits.sum() <= 60_000L)
    }

    @Test fun aSegmentLoadIsTriedMoreOftenOnceAPictureWasShownThanMedia3Does() {
        assertTrue("Media3's own default is 3", RetryPlan.SETTLED_LOAD_RETRIES > 3)
    }

    @Test fun onlyTheNetworksFaultIsWorthARetry() {
        for (s in listOf(408, 429, 500, 502, 503, 504, 599)) assertEquals("worth retrying: $s", RetryPlan.Kind.NETWORK, RetryPlan.httpKind(s))
        for (s in listOf(400, 401, 403, 404, 410, 416, 451, 200, 301, 0, -1, 600)) assertEquals("final: $s", RetryPlan.Kind.FINAL, RetryPlan.httpKind(s))
    }
}
