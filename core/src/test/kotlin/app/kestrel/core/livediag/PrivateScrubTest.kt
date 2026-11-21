package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PV1 (owner 2026-11-21): a line written while a private tab exists keeps what happened and loses every site name. */
class PrivateScrubTest {
    private fun s(x: String) = PrivateScrub.line(x)

    @Test fun everySiteNameAndAddressBecomesASiteMarkerAndTheRestStays() {
        assertEquals("+90.0s cosmetic css <site> top=true len=234 tokens=false", s("+90.0s cosmetic css gemini.google.com top=true len=234 tokens=false"))
        assertEquals("media maybe <site> type=application/octet-stream len=5000", s("media maybe r2---sn-8xgp1vo-nx5e.googlevideo.com type=application/octet-stream len=5000"))
        assertEquals("page loaded host=<site> paint=14722ms done=28523ms", s("page loaded host=claude.ai paint=14722ms done=28523ms"))
        assertEquals("opened <site> and <site> then", s("opened https://www.example.com/watch/42?t=30s and http://cdn.example.net:8080/a.js then"))
        assertEquals("warm <site>", s("warm github.com:443"))
        assertEquals("slow=<site>:js:9800,<site>:xhr:5200", s("slow=cdn.example.com:js:9800,a.b.example.org:xhr:5200"))
    }

    @Test fun addressesMailAndNetworkNumbersAreMaskedToo() {
        assertEquals("from <email> via <ip>", s("from someone@example.org via 10.97.182.19:8780"))
        assertEquals("<local> and <site>", s("blob:https://www.example.com/1234-5678 and www.example.com/some/path?q=1"))
    }

    @Test fun theAppsOwnNamesNumbersAndTimesStay() {
        val keep = "+12.345s life resume app.kestrel.browser.gecko 0.3.0-alpha85 (code 186) org.mozilla.geckoview.GeckoSession java.lang.IllegalStateException 600x973 1.5 s 3.2MB avail=420MB"
        assertEquals(keep, s(keep))
        assertEquals("", s(""))
    }

    @Test fun noSiteNameSurvivesInAnyOfTheRealShapesOfTheRing() {
        val lines = listOf("+1.0s tabs early-cover claude.ai", "+2.0s engine warm-start https://m.youtube.com", "+3.0s video quality m.youtube.com frames=149 dropped=0", "+4.0s shields ext-state page guard on www.google.com 0 blocked",
            "+5.0s page timing host=gemini.google.com ttfb=800 slow=cdn.example.com:js:9800 | shields=up net=fast-wifi", "+6.0s media file r5---sn-8xgp1vo-cvnd.googlevideo.com referer=www.youtube.com")
        for (l in lines) { val t = s(l); assertFalse(t, Regex("""[a-z0-9-]+\.(com|net|org|ai)\b""").containsMatchIn(t)); assertTrue(t, t.startsWith("+")) }
    }
}

class PrivateWindowTest {
    @Test fun itIsOpenWhileAPrivateTabExistsAndForTwoMinutesAfterTheLastOneWentAway() {
        val w = PrivateWindow(120_000L)
        assertFalse(w.active(0L)); assertEquals(0L, w.msLeft(0L))
        w.setOpen(true, 1_000L)
        assertTrue(w.active(1_000L)); assertTrue(w.active(10_000_000L)); assertEquals(-1L, w.msLeft(5L))
        w.setOpen(false, 50_000L)
        assertTrue(w.active(50_000L)); assertTrue(w.active(169_999L)); assertEquals(70_001L, w.msLeft(100_000L))
        assertFalse(w.active(170_000L)); assertEquals(0L, w.msLeft(200_000L))
    }

    @Test fun aNewPrivateTabReopensItAtOnceAndClosingWithoutOpeningChangesNothing() {
        val w = PrivateWindow(120_000L)
        w.setOpen(false, 5_000L)
        assertFalse("never opened: nothing to close", w.active(5_001L))
        w.setOpen(true, 1_000_000L); w.setOpen(false, 1_050_000L)
        assertTrue(w.active(1_100_000L))
        w.setOpen(true, 1_100_000L)
        assertTrue(w.active(9_000_000L))
    }
}
