package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Random

class FilterSetCodecAndSeedTest {
    private val lines = listOf(
        "||ads.example.com^", "||tracker.example^\$third-party", "/banner^", "|https://t.", ".gif|", "||a.example/p*q", "@@||ads.example.com/ok/",
        "/promo.js\$domain=a.com|~b.com", "||evil.example^\$important", "@@||site.example^\$document", "@@||q.example^\$elemhide",
        "example.com##.ad-box", "##.generic", "other.com#@#.generic", "||zip^\$document",
    )

    private fun build(): FilterSet {
        val b = FilterSetBuilder()
        b.addAbpList("list-one", Category.ADS, lines.asSequence())
        b.addHostsList("list-two", Category.MALWARE, sequenceOf("0.0.0.0 malware.example.net"))
        return b.build(1_700_000_000_000L)
    }

    private val probes = listOf(
        "https://ads.example.com/x" to RequestType.SCRIPT, "https://ads.example.com/ok/x" to RequestType.SCRIPT,
        "https://tracker.example/t.js" to RequestType.SCRIPT, "https://news.org/banner/1" to RequestType.IMAGE,
        "https://t.example/a" to RequestType.XHR, "https://x.org/p.gif" to RequestType.IMAGE, "https://a.example/pzzq" to RequestType.SCRIPT,
        "https://evil.example/x" to RequestType.SCRIPT, "https://malware.example.net/" to RequestType.DOCUMENT,
        "https://nothing.org/index.html" to RequestType.DOCUMENT, "https://x.zip/" to RequestType.DOCUMENT, "https://x.com/promo.js" to RequestType.SCRIPT,
    )

    @Test
    fun roundTripPreservesEveryVerdictAndMeta() {
        val a = build()
        val b = FilterSetCodec.read(FilterSetCodec.write(a))
        for ((u, t) in probes) for (page in listOf("a.com", "b.com", "tracker.example", null)) {
            assertEquals("$u / $page", a.check(u, page, t), b.check(u, page, t))
        }
        assertEquals(a.cosmeticCss("example.com"), b.cosmeticCss("example.com"))
        assertEquals(a.cosmeticCss("other.com"), b.cosmeticCss("other.com"))
        assertEquals(a.isPageAllowed("site.example"), b.isPageAllowed("site.example"))
        assertTrue(b.isPageAllowed("site.example"))
        assertEquals(a.meta.lists, b.meta.lists)
        assertEquals(1_700_000_000_000L, b.meta.createdAtMillis)
        assertEquals(a.meta.hostRules, b.meta.hostRules)
        assertEquals(a.meta.patternRules, b.meta.patternRules)
        assertTrue("bare TLD rule works", Verdict.isBlocked(b.check("https://x.zip/", null, RequestType.DOCUMENT)))
    }

    @Test
    fun emptySetRoundTrips() {
        val e = FilterSetCodec.read(FilterSetCodec.write(FilterSet.EMPTY))
        assertEquals(Verdict.NONE, e.check("https://ads.example/x", "p.example", RequestType.SCRIPT))
        assertEquals("", e.cosmeticCss("example.com"))
    }

    @Test
    fun anyCorruptionIsRejectedNeverSilentlyAccepted() {
        val good = FilterSetCodec.write(build())
        val rnd = Random(7)
        repeat(300) {
            val bad = good.copyOf()
            val i = rnd.nextInt(bad.size)
            bad[i] = (bad[i].toInt() xor (1 shl rnd.nextInt(8))).toByte()
            try { FilterSetCodec.read(bad); fail("single bit flip at $i went undetected") } catch (e: IllegalArgumentException) { /* expected */ }
        }
        for (cut in listOf(0, 5, 20, good.size / 2, good.size - 1)) {
            try { FilterSetCodec.read(good.copyOf(cut)); fail("truncation to $cut accepted") } catch (e: IllegalArgumentException) { /* expected */ }
        }
    }

    @Test
    fun structurallyInvalidButChecksummedDataIsRejectedByValidation() {
        // Re-sign a payload whose invariants are broken: the CRC passes, validate() must still refuse it.
        val good = FilterSetCodec.write(build())
        val body = good.copyOf(good.size - 8)
        // zero the entire middle: lengths become inconsistent/garbage
        for (i in 60 until body.size - 40) body[i] = 0x7f
        val crc = java.util.zip.CRC32().also { it.update(body, 0, body.size) }.value
        val bb = java.nio.ByteBuffer.allocate(body.size + 8).put(body).putLong(crc).array()
        try { FilterSetCodec.read(bb); fail("garbage with a valid CRC was accepted") } catch (e: IllegalArgumentException) { /* expected */ }
    }

    // ------------------------------------------------------------------ seed

    @Test
    fun seedBlocksKnownAdAndTrackerEndpoints() {
        val s = SeedLists.build()
        for (u in listOf(
            "https://securepubads.g.doubleclick.net/gampad/ads", "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            "https://www.google-analytics.com/analytics.js", "https://static.criteo.net/js/ld/publishertag.js", "https://cdn.taboola.com/libtrc/x.js",
            "https://www.facebook.com/tr?id=1&ev=PageView", "https://connect.facebook.net/en_US/fbevents.js",
        )) assertTrue(u, Verdict.isBlocked(s.check(u, "news.example", RequestType.SCRIPT)))
    }

    @Test
    fun seedDoesNotBreakMainstreamSites() {
        val s = SeedLists.build()
        for (u in listOf(
            "https://www.google.com/search?q=hi", "https://www.googletagmanager.com/gtm.js?id=GTM-X", "https://www.gstatic.com/recaptcha/api2/x.js",
            "https://fonts.googleapis.com/css?family=Roboto", "https://cdn.jsdelivr.net/npm/x.js", "https://www.youtube.com/watch?v=abc",
            "https://connect.facebook.net/en_US/sdk.js", "https://www.paypal.com/", "https://example.com/", "https://github.com/",
        )) assertFalse(u, Verdict.isBlocked(s.check(u, "news.example", RequestType.SCRIPT)))
        for (u in listOf("https://www.google.com/", "https://example.com/")) {
            assertFalse(u, Verdict.isBlocked(s.check(u, null, RequestType.DOCUMENT)))
        }
    }

    @Test
    fun seedHidesCommonAdContainers() {
        val css = SeedLists.build().cosmeticCss("any.example")
        assertTrue(css, css.contains(".adsbygoogle"))
        assertTrue(css, css.contains("display:none!important"))
    }
}
