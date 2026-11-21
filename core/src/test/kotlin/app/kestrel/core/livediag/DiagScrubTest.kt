package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LD1: what may leave the tablet. The cases are the kinds of line the app really writes, plus the things that must NEVER get out. */
class DiagScrubTest {
    private fun s(x: String) = DiagScrub.line(x)

    @Test fun aLinkIsReducedToItsHostAndNothingElse() {
        assertEquals("+12.3s page loaded news.example.org in 2100 ms", s("+12.3s page loaded https://news.example.org/politics/2026/story?id=7#top in 2100 ms"))
        assertEquals("from news.example.org", s("from https://user:pw@news.example.org:8443/a/b?c=d"))
        assertEquals("feed.example.net up", s("wss://feed.example.net/socket up"))
        assertEquals("a <local> b", s("a content://com.android.providers/downloads/12 b"))
        assertEquals("<local>", s("blob:https://www.example.org/1234-abcd"))
        assertEquals("a link with a token in its query is a sensitive site: not named", "page <sensitive-site> end", s("page https://news.example.org/story?id=7&token=abc#top end"))
    }

    @Test fun aSensitiveSiteIsNeverNamedNotEvenAsABareHost() {
        assertEquals("page loaded <sensitive-site> in 900 ms", s("page loaded https://mail.google.com/mail/u/0/#inbox in 900 ms"))
        assertEquals("to=<sensitive-site> reload", s("to=www.chase.com reload"))
        assertEquals("<sensitive-site> error", s("https://example.com/login?next=%2F error"))
        assertEquals("to=<sensitive-site>", s("to=secure.example.org"))
        assertEquals("to=reddit.com", s("to=reddit.com"))
    }

    @Test fun passwordsTokensKeysCookiesAndAuthHeadersAreRemoved() {
        assertEquals("field password=<redacted> rejected", s("field password=hunter2 rejected"))
        assertEquals("cookie: <redacted>", s("cookie: sessionid=abc123def456"))
        assertEquals("Bearer <secret> failed", s("Bearer abcdefghijklmnop12345 failed"))
        val ghp = "ghp_" + "A1b2C3d4E5f6G7h8I9j0K1l2M3n4"
        assertEquals("token <secret> end", s("token $ghp end"))
        assertEquals("k <secret>", s("k eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc123_-"))
        assertEquals("<secret>", s("-----BEGIN " + "PRIVATE KEY-----"))   // FAKE fixture: a PEM header, not a key (the scanner skips lines marked FAKE)
        assertEquals("id <secret> x", s("id " + "a".repeat(44) + " x"))
        assertEquals("api_key=<redacted>", s("api_key=AIzaSyA-123456789012345678901234567890"))
    }

    @Test fun cardNumbersAreRemovedButOrdinaryNumbersAreNot() {
        assertEquals("paid <card> ok", s("paid 4111 1111 1111 1111 ok"))
        assertEquals("<card>", s("4111-1111-1111-1111"))
        assertEquals("<card>", s("5500005555555559"))
        assertFalse("a card-shaped run that fails the Luhn check is still partly masked: the safe side", s("a 4111 1111 1111 1112 b")!!.contains("4111 1111 1111"))
        assertEquals("+3.456s mem avail=721012 kB heap=15/512 MB", s("+3.456s mem avail=721012 kB heap=15/512 MB"))
        assertEquals("frames worst=605ms late-beat 812ms", s("frames worst=605ms late-beat 812ms"))
        assertTrue(DiagScrub.looksLikeCard("4111111111111111")); assertFalse(DiagScrub.looksLikeCard("4111111111111112")); assertFalse(DiagScrub.looksLikeCard("12345"))
    }

    @Test fun emailAddressesAndPhoneNumbersAreRemoved() {
        assertEquals("from <email> sent", s("from jane.doe+shop@example.com sent"))
        assertEquals("call <phone> now", s("call +1 415-555-0132 now"))
        assertEquals("call <phone> now", s("call (415) 555 0132 now"))
        assertEquals("call <phone> now", s("call 415-555-0132 now"))
    }

    @Test fun ourOwnVersionNumbersAndClassNamesSurvive() {
        assertEquals("build 0.3.0-alpha80 code 181", s("build 0.3.0-alpha80 code 181"))
        assertEquals("at app.kestrel.browser.BrowserActivity.onResume(BrowserActivity.kt:615)", s("at app.kestrel.browser.BrowserActivity.onResume(BrowserActivity.kt:615)"))
        assertEquals("engine 160.0.20261104181552", s("engine 160.0.20261104181552"))
    }

    @Test fun blankLinesAreDroppedAndLongLinesAreCut() {
        assertNull(s("")); assertNull(s("   "))
        val long = s("x ".repeat(500))!!
        assertTrue(long.length <= DiagScrub.MAX_LINE + 3 && long.endsWith("..."))
        assertNotNull(s("ok"))
    }

    @Test fun theSensitiveSiteRuleMatchesTheExtensionsCorpus() {
        // the same addresses as web-tests/background.test.js (BK10): kept private there, kept private here
        val kept = listOf("https://mail.google.com/mail/u/0/", "https://outlook.live.com/mail/0/", "https://mail.proton.me/u/0/inbox", "https://www.chase.com/personal", "https://secure.bankofamerica.com/login",
            "https://accounts.google.com/signin/v2", "https://www.paypal.com/myaccount/summary", "https://shop.example.com/checkout/step2", "https://example.com/login?next=%2F", "https://example.com/account/orders",
            "https://example.com/auth/callback?code=abc", "https://example.com/page?token=xyz", "https://portal.example.edu/grades", "https://www.irs.gov/refunds", "https://myhealth.example.org/",
            "https://www.firstcreditunion.example/", "https://id.example.com/", "https://example.com/settings/privacy", "https://web.coinbase.com/", "https://www.icloud.com/photos")
        for (u in kept) assertEquals(u, DiagScrub.SENSITIVE, DiagScrub.site(u))
        val open = listOf("https://www.reddit.com/r/android/", "https://x.com/home", "https://gemini.google.com/app", "https://www.facebook.com/", "https://news.example.org/politics/2026/story", "https://doraby.com/ozark/", "https://www.youtube.com/watch?v=abc")
        for (u in open) assertEquals(u, DiagScrub.hostOf(u), DiagScrub.site(u))
    }

    @Test fun onlyWhitelistedLayersAreJournalledAndTitlesNeverAre() {
        assertTrue(DiagPolicy.allows("page", "loaded")); assertTrue(DiagPolicy.allows("gecko", "onKill")); assertTrue(DiagPolicy.allows("video", "stall")); assertTrue(DiagPolicy.allows("mem", "trim"))
        for (l in listOf("user", "key", "field", "address", "dialog", "download", "downloads", "panel", "look", "pull", "probe", "lab")) assertFalse(l, DiagPolicy.allows(l, "x"))
        assertFalse(DiagPolicy.allows("media", "name-clues")); assertFalse(DiagPolicy.allows("page", "title")); assertFalse(DiagPolicy.allows("tabs", "title-changed"))
    }
}
