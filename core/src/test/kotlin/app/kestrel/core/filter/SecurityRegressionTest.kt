package app.kestrel.core.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Regression tests for findings of the independent security review (docs/handoff/REVIEW-2026-11-11.md). */
class SecurityRegressionTest {
    private fun set(vararg lines: String, category: Int = Category.ADS): FilterSet {
        val b = FilterSetBuilder()
        b.addAbpList("t", category, lines.asSequence())
        return b.build(1L)
    }

    // ------------------------------------------------------------------ S2: URL truncation must not let userinfo hide the real host

    @Test
    fun aLongFakeUserinfoCannotPushTheRealHostPastTheTruncationPoint() {
        val f = set("||malware.example^", category = Category.MALWARE)
        for (pad in listOf(10, 300, 1500, 2030, 2040, 2100, 5000, 200_000)) {
            val url = "https://" + "a".repeat(pad) + "@malware.example/x"
            assertTrue("document, pad=$pad", Verdict.isBlocked(f.check(url, null, RequestType.DOCUMENT)))
            assertTrue("subframe, pad=$pad", Verdict.isBlocked(f.check(url, "page.example", RequestType.SUBDOCUMENT)))
        }
    }

    @Test
    fun aTrackerNamedOnlyInTheUserinfoIsStillNotBlocked() {
        val f = set("||ads.example^")
        assertFalse(Verdict.isBlocked(f.check("https://ads.example@good.com/", "page.example", RequestType.SCRIPT)))
        assertFalse(Verdict.isBlocked(f.check("https://ads.example@" + "a".repeat(3000) + ".good.com/", "page.example", RequestType.SCRIPT)))
    }

    @Test
    fun absurdLongUrlsStayCheapAndSafe() {
        val f = set("||ads.example^", "/banner/*")
        val huge = "https://good.example/" + "x".repeat(3_000_000)
        assertFalse(Verdict.isBlocked(f.check(huge, "page.example", RequestType.IMAGE)))
        assertTrue(Verdict.isBlocked(f.check("https://ads.example/" + "x".repeat(3_000_000), "page.example", RequestType.IMAGE)))
    }

    // ------------------------------------------------------------------ S4: a bad list must not be able to switch blocking off (or everything on)

    @Test
    fun matchEverythingRulesWithoutADomainAreIgnored() {
        val f = set("||ads.example^", "@@*", "@@\$script", "@@|", "*\$script", "\$image", "*", "|", "||", "^")
        assertTrue("the real rule still works", Verdict.isBlocked(f.check("https://ads.example/a.js", "page.example", RequestType.SCRIPT)))
        assertFalse("nothing else gets blocked by '*' or '\$script'", Verdict.isBlocked(f.check("https://fine.example/a.js", "page.example", RequestType.SCRIPT)))
        assertFalse(Verdict.isBlocked(f.check("https://fine.example/a.png", "page.example", RequestType.IMAGE)))
    }

    @Test
    fun domainScopedMatchEverythingRulesStillWork() {
        // uBlock-style "block all scripts on this one site" / "allow everything on this site" are legitimate
        val f = set("*\$script,domain=noscripts.example", "@@*\$domain=trusted.example")
        assertTrue(Verdict.isBlocked(f.check("https://cdn.example/a.js", "noscripts.example", RequestType.SCRIPT)))
        assertFalse(Verdict.isBlocked(f.check("https://cdn.example/a.js", "other.example", RequestType.SCRIPT)))
    }

    @Test
    fun hugeRuleLinesAreSkippedNotCompiledQuadratically() {
        val big = "/" + (1..40_000).joinToString("/") { "tok$it" } // ~340 KB single line of DISTINCT tokens (the quadratic shape: 5 s to compile before)
        val t0 = System.nanoTime()
        val f = set("||ads.example^", big, "||tracker.example^")
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("compiling a 400 KB line took $ms ms", ms < 1500)
        assertTrue(Verdict.isBlocked(f.check("https://tracker.example/x", "page.example", RequestType.SCRIPT)))
    }

    // ------------------------------------------------------------------ S5: adversarial URLs must not make the request path expensive

    @Test
    fun repeatedHotTokensCostNoMoreThanOneScanPerDistinctToken() {
        val kfs = File("../app/src/main/assets/filters/default.kfs")
        assumeTrue("snapshot not generated", kfs.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes())
        val hot = "https://news.example/" + "event/".repeat(330)
        val normal = "https://news.example/article/2026/11/some-story.html"
        repeat(30) { fs.check(hot, "news.example", RequestType.XHR); fs.check(normal, "news.example", RequestType.XHR) } // warm up
        val t0 = System.nanoTime()
        repeat(40) { fs.check(hot, "news.example", RequestType.XHR) }
        val perHotMs = (System.nanoTime() - t0) / 40.0 / 1e6
        assertTrue("adversarial URL costs $perHotMs ms per request (was ~40 ms before the fix)", perHotMs < 6.0)
    }

    // ------------------------------------------------------------------ S11: cosmetic selectors must be structurally balanced

    @Test
    fun misnestedBracketsAndAtRulesAreRejected() {
        for (bad in listOf("[(])", "div[(]", "a:not(b[)]", ")a(", "}", "a{b}", "@media x", "@import url(x)", "a, @font-face", "a[href=\"x]", "a:is(b,c")) {
            assertFalse("must reject: $bad", CosmeticSelectors.accept(bad))
        }
        for (ok in listOf(".ad-banner", "div[class*=\"sponsor\"]", "a[href^='https://x.example/?a=1;b=2']", "div:not(.keep):not([data-x=\"y\"])", "#a > .b + .c ~ .d",
            "span:is(.x, .y)", "a[href*=\"@\"]", "[data-ad-slot]")) {
            assertTrue("must keep: $ok", CosmeticSelectors.accept(ok))
        }
    }
}
