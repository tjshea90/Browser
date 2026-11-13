package app.kestrel.core.net

import app.kestrel.core.net.WarmPolicy.Ask
import app.kestrel.core.net.WarmPolicy.Kind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WarmPolicyTest {
    private fun ask(kind: Kind = Kind.TOUCH, origin: String? = "https://a.example/", now: Long = 1_000, enabled: Boolean = true, saver: Boolean = false, viaProxy: Boolean = false,
                    frontPrivate: Boolean = false, pagePrivate: Boolean = false, pageIsFront: Boolean = true) =
        Ask(kind, origin, enabled, saver, viaProxy, frontPrivate, pagePrivate, pageIsFront, now)

    @Test fun anOpenAskIsAllowedOnceAndThenHeldBackFor30Seconds() {
        val p = WarmPolicy()
        assertTrue(p.allow(ask(now = 1_000)))
        assertFalse(p.allow(ask(now = 1_000 + 29_999)))
        assertTrue(p.allow(ask(now = 1_000 + 30_000)))
    }

    @Test fun eachOriginHasItsOwnClockAndTheKindDoesNotMatterForIt() {
        val p = WarmPolicy()
        assertTrue(p.allow(ask(Kind.SUGGESTION, "https://a.example/", 0)))
        assertTrue(p.allow(ask(Kind.RESULT, "https://b.example/", 1)))
        assertFalse(p.allow(ask(Kind.TOUCH, "https://a.example/", 2)))   // the suggestion already opened it
        assertFalse(p.allow(ask(Kind.RESUME, "https://b.example/", 3)))
    }

    @Test fun theSwitchDataSaverAndAProxyStopEverything() {
        for (k in Kind.values()) {
            assertFalse(WarmPolicy().allow(ask(k, enabled = false)), "switch off $k")
            assertFalse(WarmPolicy().allow(ask(k, saver = true)), "Data saver $k")
            assertFalse(WarmPolicy().allow(ask(k, viaProxy = true)), "proxy $k")
        }
    }

    @Test fun neverFromAPrivateTab() {
        for (k in Kind.values()) {
            assertFalse(WarmPolicy().allow(ask(k, frontPrivate = true)), "front private $k")
            assertFalse(WarmPolicy().allow(ask(k, pagePrivate = true)), "page held by a private tab $k")
        }
    }

    @Test fun aRefusedAskDoesNotUseUpTheOrigin() {
        val p = WarmPolicy()
        assertFalse(p.allow(ask(frontPrivate = true, now = 0)))
        assertTrue(p.allow(ask(frontPrivate = false, now = 1)), "the private refusal was not remembered")
    }

    @Test fun aTouchOnlyCountsOnThePageInFront() {
        assertFalse(WarmPolicy().allow(ask(Kind.TOUCH, pageIsFront = false)))
        assertTrue(WarmPolicy().allow(ask(Kind.RESULT, pageIsFront = false)), "a result list may finish loading just after a tab switch")
    }

    @Test fun onlyHttpsOriginsAreAccepted() {
        val p = WarmPolicy()
        assertFalse(p.allow(ask(origin = null)))
        assertFalse(p.allow(ask(origin = "http://a.example/")))
        assertFalse(p.allow(ask(origin = "https://")))
        assertFalse(p.allow(ask(origin = "")))
        assertTrue(p.allow(ask(origin = "HTTPS://A.example:8443/")))
    }

    @Test fun theMemoryIsBounded() {
        val p = WarmPolicy(maxKnown = 4)
        for (i in 0 until 4) assertTrue(p.allow(ask(origin = "https://s$i.example/", now = i.toLong())))
        assertTrue(p.allow(ask(origin = "https://s4.example/", now = 10)))
        assertTrue(p.allow(ask(origin = "https://s0.example/", now = 11)), "the table was emptied when it filled, so an old origin may be asked again")
    }

    @Test fun aClockThatWentBackwardsDoesNotBlockForever() {
        val p = WarmPolicy()
        assertTrue(p.allow(ask(now = 100_000)))
        assertTrue(p.allow(ask(now = 5)), "now < before: treated as a new epoch")
    }
}
