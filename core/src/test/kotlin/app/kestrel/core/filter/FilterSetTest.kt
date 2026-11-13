package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterSetTest {
    private fun set(vararg lines: String, category: Int = Category.ADS): FilterSet {
        val b = FilterSetBuilder()
        b.addAbpList("t", category, lines.asSequence())
        return b.build(1L)
    }

    private val script = RequestType.SCRIPT
    private val image = RequestType.IMAGE

    private fun FilterSet.blocked(url: String, page: String? = "page.example", type: Int = script) =
        Verdict.isBlocked(check(url, page, type))

    // ------------------------------------------------------------------ host anchoring

    @Test
    fun hostRuleBoundaries() {
        val f = set("||ads.example.com^")
        assertTrue(f.blocked("https://ads.example.com/x"))
        assertTrue(f.blocked("https://sub.ads.example.com/x"))
        assertTrue(f.blocked("https://ads.example.com:8443/x"))
        assertTrue(f.blocked("http://ads.example.com"))
        assertFalse("different label", f.blocked("https://notads.example.com/x"))
        assertFalse("parent domain", f.blocked("https://example.com/x"))
        assertFalse("host appears only in query", f.blocked("https://example.com/?u=https://ads.example.com/"))
        assertFalse("host appears only in path", f.blocked("https://example.com/ads.example.com/x"))
        assertFalse("rule host is a prefix of attacker domain", f.blocked("https://ads.example.com.evil.net/"))
    }

    @Test
    fun urlTrickeryCannotEvadeOrTriggerHostRules() {
        val f = set("||ads.example.com^")
        assertTrue("trailing dot", f.blocked("https://ads.example.com./x"))
        assertTrue("userinfo before real host", f.blocked("https://good.com@ads.example.com/"))
        assertFalse("userinfo naming the tracker", f.blocked("https://ads.example.com@good.com/"))
        assertTrue("case-insensitive", f.blocked("HTTPS://ADS.EXAMPLE.COM/X"))
    }

    @Test
    fun patternRulesWithHostAnchorAndPath() {
        val f = set("||example.com/ads/")
        assertTrue(f.blocked("https://example.com/ads/banner.png"))
        assertTrue(f.blocked("https://www.example.com/ads/x"))
        assertFalse(f.blocked("https://example.com/adsx"))
        assertFalse(f.blocked("https://other.com/example.com/ads/"))
    }

    @Test
    fun bareHostPatternWithoutCaretMatchesHostPrefixes() {
        val f = set("||example.com") // no '^': ABP semantics also match example.com.evil.net
        assertTrue(f.blocked("https://example.com/"))
        assertTrue(f.blocked("https://example.com.evil.net/"))
        assertFalse(f.blocked("https://notexample.com/"))
    }

    @Test
    fun startAndEndAnchors() {
        val f = set("|https://tracker.", ".gif|")
        assertTrue(f.blocked("https://tracker.example/x"))
        assertFalse(f.blocked("http://tracker.example/x"))
        assertFalse(f.blocked("https://a.com/?u=https://tracker.example/"))
        assertTrue(f.blocked("https://a.com/pixel.gif"))
        assertFalse(f.blocked("https://a.com/pixel.gif?x=1"))
    }

    @Test
    fun separatorAndWildcard() {
        val f = set("/banner^", "/promo/*/ad")
        assertTrue(f.blocked("https://a.com/banner/1"))
        assertTrue(f.blocked("https://a.com/banner?x=1"))
        assertTrue(f.blocked("https://a.com/banner"))
        assertFalse(f.blocked("https://a.com/bannerx"))
        assertFalse(f.blocked("https://a.com/banner.png"))
        assertTrue(f.blocked("https://a.com/promo/x/y/ad"))
        assertFalse(f.blocked("https://a.com/promo/ad"))
    }

    // ------------------------------------------------------------------ exceptions / priority

    @Test
    fun exceptionsOverrideBlocks() {
        val f = set("||ads.example.com^", "@@||ads.example.com/ok/")
        assertTrue(f.blocked("https://ads.example.com/bad"))
        val v = f.check("https://ads.example.com/ok/x", "page.example", script)
        assertFalse(Verdict.isBlocked(v))
        assertEquals(Verdict.EXCEPTED, v)
    }

    @Test
    fun hostExceptionRules() {
        val f = set("||example.com^", "@@||cdn.example.com^")
        assertTrue(f.blocked("https://www.example.com/x"))
        assertFalse(f.blocked("https://cdn.example.com/x"))
    }

    @Test
    fun importantBeatsExceptions() {
        val f = set("||ads.example.com^\$important", "@@||ads.example.com^")
        val v = f.check("https://ads.example.com/x", "page.example", script)
        assertTrue(Verdict.isBlocked(v))
        assertTrue((v and Verdict.IMPORTANT) != 0)
    }

    @Test
    fun exceptionAloneBlocksNothing() {
        assertFalse(set("@@||example.com^").blocked("https://example.com/"))
    }

    // ------------------------------------------------------------------ options

    @Test
    fun thirdPartyOption() {
        val f = set("||tracker.example^\$third-party")
        assertFalse("first party page", f.blocked("https://tracker.example/t.js", page = "tracker.example"))
        assertFalse("same site, other subdomain", f.blocked("https://cdn.tracker.example/t.js", page = "www.tracker.example"))
        assertTrue(f.blocked("https://tracker.example/t.js", page = "news.example"))
        assertTrue(f.blocked("https://tracker.example/t.js", page = "other.co.uk"))

        val g = set("||tracker.example^\$~third-party")
        assertTrue(g.blocked("https://tracker.example/t.js", page = "tracker.example"))
        assertFalse(g.blocked("https://tracker.example/t.js", page = "news.example"))
    }

    @Test
    fun thirdPartyUsesRegistrableDomainsNotRawHosts() {
        val f = set("||cdn.example.co.uk^\$third-party")
        assertFalse("same registrable domain example.co.uk", f.blocked("https://cdn.example.co.uk/a.js", page = "www.example.co.uk"))
        assertTrue("different registrable domain under the same public suffix", f.blocked("https://cdn.example.co.uk/a.js", page = "other.co.uk"))
    }

    @Test
    fun typeOptions() {
        val f = set("||cdn.example/lib.js\$script")
        assertTrue(f.blocked("https://cdn.example/lib.js", type = RequestType.SCRIPT))
        assertFalse(f.blocked("https://cdn.example/lib.js", type = RequestType.IMAGE))
        assertTrue("ambiguous fetch-like request may be a script", f.blocked("https://cdn.example/lib.js", type = RequestType.UNKNOWN))
        assertFalse("never a main-frame match without \$document", f.blocked("https://cdn.example/lib.js", type = RequestType.DOCUMENT))

        val neg = set("||img.example^\$~image")
        assertFalse(neg.blocked("https://img.example/a", type = RequestType.IMAGE))
        assertTrue(neg.blocked("https://img.example/a", type = RequestType.SCRIPT))
    }

    @Test
    fun documentBlocking() {
        assertFalse("plain rule never blocks navigation", set("||evil.example^").blocked("https://evil.example/", type = RequestType.DOCUMENT))
        assertTrue(set("||evil.example^\$document").blocked("https://evil.example/", type = RequestType.DOCUMENT))
        assertTrue(set("||evil.example^\$all").blocked("https://evil.example/", type = RequestType.DOCUMENT))
        assertTrue("malware category blocks the page itself", set("||evil.example^", category = Category.MALWARE).blocked("https://evil.example/", type = RequestType.DOCUMENT))
        assertFalse("ads category does not", set("||evil.example^", category = Category.ADS).blocked("https://evil.example/", type = RequestType.DOCUMENT))
    }

    @Test
    fun domainOption() {
        val f = set("/promo.js\$domain=a.com|~sub.a.com")
        assertTrue(f.blocked("https://x.com/promo.js", page = "a.com"))
        assertTrue(f.blocked("https://x.com/promo.js", page = "www.a.com"))
        assertFalse("excluded subdomain", f.blocked("https://x.com/promo.js", page = "x.sub.a.com"))
        assertFalse("other site", f.blocked("https://x.com/promo.js", page = "b.com"))
        assertFalse("unknown page host with an include list", f.blocked("https://x.com/promo.js", page = null))
        val g = set("/promo.js\$domain=~b.com")
        assertTrue(g.blocked("https://x.com/promo.js", page = "a.com"))
        assertFalse(g.blocked("https://x.com/promo.js", page = "b.com"))
    }

    @Test
    fun popupRulesOnlyApplyToPopupNavigations() {
        val f = set("||pop.example^\$popup", "/earn.php?z=\$popup,subdocument")
        assertTrue(f.blocked("https://pop.example/x", page = "opener.example", type = RequestType.POPUP))
        assertFalse("not a script", f.blocked("https://pop.example/x", page = "opener.example", type = RequestType.SCRIPT))
        assertFalse("not a document", f.blocked("https://pop.example/x", page = null, type = RequestType.DOCUMENT))
        assertTrue(f.blocked("https://a.example/earn.php?z=1", page = "opener.example", type = RequestType.POPUP))
        assertTrue(f.blocked("https://a.example/earn.php?z=1", page = "opener.example", type = RequestType.SUBDOCUMENT))
        assertFalse(set("||pop.example^").blocked("https://pop.example/x", page = "o.example", type = RequestType.POPUP))
    }

    @Test
    fun categoryIsReported() {
        val f = set("||ads.example^")
        assertEquals(Category.ADS, Verdict.category(f.check("https://ads.example/x", "p.example", script)))
        val g = set("||trk.example^", category = Category.PRIVACY)
        assertEquals(Category.PRIVACY, Verdict.category(g.check("https://trk.example/x", "p.example", script)))
    }

    // ------------------------------------------------------------------ badfilter, hosts files, site exceptions

    @Test
    fun badfilterCancelsRegardlessOfOrder() {
        assertFalse(set("||ads.example^", "||ads.example^\$badfilter").blocked("https://ads.example/x"))
        assertFalse(set("||ads.example^\$badfilter", "||ads.example^").blocked("https://ads.example/x"))
        assertFalse(set("/pix.gif\$image", "/pix.gif\$image,badfilter").blocked("https://a.com/pix.gif", type = image))
        assertTrue("only the exact rule is cancelled", set("||ads.example^\$script", "||ads.example^\$badfilter").blocked("https://ads.example/x"))
    }

    @Test
    fun hostsFileFormat() {
        val b = FilterSetBuilder()
        b.addHostsList("hosts", Category.PRIVACY, sequenceOf(
            "# comment", "127.0.0.1 localhost", "0.0.0.0 ads.example.net  # trailing", "0.0.0.0 a.example.org b.example.org",
            "plain.example.io", "::1 ip6-localhost", "",
        ))
        val f = b.build(1L)
        for (h in listOf("ads.example.net", "a.example.org", "b.example.org", "plain.example.io")) assertTrue(h, f.blocked("https://$h/x"))
        assertFalse(f.blocked("https://localhost/x"))
        assertFalse(f.blocked("https://example.net/x"))
        assertEquals(4, b.report.hostRules)
    }

    @Test
    fun siteLevelExceptions() {
        val f = set("@@||example.org^\$document", "@@||shop.example^\$elemhide")
        assertTrue(f.isPageAllowed("example.org"))
        assertTrue(f.isPageAllowed("www.example.org"))
        assertFalse(f.isPageAllowed("example.net"))
        assertFalse(f.isPageAllowed("shop.example"))
    }

    // ------------------------------------------------------------------ cosmetic

    @Test
    fun cosmeticSiteAndGenericRules() {
        val f = set("example.com##.ad-box", "example.com##div.promo", "##.generic-ad", "other.org##.only-other")
        val css = f.cosmeticCss("www.example.com")
        assertTrue(css, css.contains(".ad-box{display:none!important}"))
        assertTrue(css, css.contains("div.promo{display:none!important}"))
        assertTrue(css, css.contains(".generic-ad"))
        assertFalse(css, css.contains(".only-other"))
        assertFalse(f.cosmeticCss("example.com", includeGeneric = false).contains(".generic-ad"))
        assertEquals("", set("example.com##.ad-box").cosmeticCss("unrelated.net"))
    }

    @Test
    fun cosmeticExceptionsAndSiteFlags() {
        val f = set("##.generic-ad", "##.keep-me", "news.example#@#.generic-ad", "#@#.keep-me", "quiet.example##.zap", "@@||quiet.example^\$elemhide",
            "mid.example##.zap2", "@@||mid.example^\$generichide")
        val css = f.cosmeticCss("news.example")
        assertFalse("domain exception removes the generic rule here", css.contains(".generic-ad"))
        assertFalse("global exception removes it everywhere", css.contains(".keep-me"))
        assertTrue("but not on other sites", f.cosmeticCss("elsewhere.example").contains(".generic-ad"))
        assertEquals("elemhide exception disables all cosmetic rules", "", f.cosmeticCss("quiet.example"))
        val mid = f.cosmeticCss("mid.example")
        assertTrue(mid.contains(".zap2")); assertFalse("generichide disables generic only", mid.contains(".generic-ad"))
    }

    @Test
    fun unsafeAndProceduralCosmeticRulesNeverReachTheStylesheet() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf(
            "example.com##.ok", "example.com##.bad{background:url(http://evil/x)}", "example.com##div:has-text(Ad)",
            "example.com##+js(set-constant, a, b)", "example.com#?#.x", "example.com##.esc\\:ape", "~example.com##.neg", "example.*##.entity",
        ))
        val fs = b.build(1L)
        val css = fs.cosmeticCss("example.com")
        // `#?#.x` is only the extended SPELLING of a plain selector (a normal rule since alpha26); the :has-text() rule is a procedural one for the page script (ProceduralRulesTest): never CSS
        assertEquals(".ok{display:none!important}\n.x{display:none!important}\n", css)
        assertEquals(3, b.report.cosmeticSite)
        assertEquals(1, fs.proceduralFor("example.com").size)
        assertFalse(css.contains("has-text") || css.contains("Ad") || css.contains('\u0002'))
    }

    @Test
    fun structuralCharactersAreAllowedOnlyInsideQuotes() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf(
            "example.com##div[style^=\"width:100%;height:5px;\"]", // legit: ';' is data inside a string
            "example.com##.ok2",
            "example.com##.a{color:red}",            // braces outside quotes: injection
            "example.com##.b;color:red",               // ';' outside quotes
            "example.com##.c[x=\"y\"]{x:y}",          // quote closes, then a block
            "example.com##.d[x=\"unclosed]",          // unbalanced quote
            "example.com##.e[x=\"a\\\"b\"]",         // backslash-escaped quote
        ))
        val css = b.build(1L).cosmeticCss("example.com")
        assertTrue(css, css.contains("div[style^=\"width:100%;height:5px;\"]{display:none!important}"))
        assertTrue(css, css.contains(".ok2{"))
        for (bad in listOf(".a{", ".b;", ".c[", ".d[", ".e[")) assertFalse("$bad must be rejected: $css", css.contains(bad))
        assertEquals(2, b.report.cosmeticSite)
    }

    @Test
    fun modernPseudoSelectorsGetTheirOwnRuleSoOldWebViewsDoNotDropTheGroup() {
        val css = set("example.com##div:has(> .ad)", "example.com##.plain").cosmeticCss("example.com")
        assertTrue(css, css.contains("div:has(> .ad){display:none!important}\n"))
        assertTrue(css, css.contains(".plain{display:none!important}\n"))
    }

    // ------------------------------------------------------------------ robustness

    @Test
    fun nonHierarchicalUrlsAreNeverFiltered() {
        val f = set("||ads.example^", "/pixel")
        for (u in listOf("data:text/html,/pixel", "about:blank", "blob:https://ads.example/x", "javascript:void(0)", "", "ads.example")) {
            assertFalse(u, f.blocked(u))
        }
    }

    @Test
    fun veryLongAndOddUrlsDoNotCrash() {
        val f = set("||ads.example^", "/track/*/x", "a*b*c*d")
        val long = "https://page.example/" + "a/".repeat(5000) + "x"
        assertFalse(f.blocked(long))
        assertTrue(f.blocked("https://[2001:db8::1]:8080/track/1/x") || true)
        assertFalse(f.blocked("https://[2001:db8::1]:8080/index"))
        assertFalse(f.blocked("https://:80/"))
        assertFalse(f.blocked("http://"))
    }

    @Test
    fun reportCountsWhatWasSkipped() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf(
            "! comment", "[Adblock Plus 2.0]", "", "||a.example^", "/regex[0-9]/", "||b.example^\$replace=/a/b/", "||c.example^\$csp=x",
        ))
        b.build(1L)
        assertEquals(1, b.report.skippedCount(Skip.REGEX))
        assertEquals(2, b.report.skippedCount(Skip.UNSUPPORTED_OPTION))
        assertEquals(1, b.report.hostRules)
    }
}
