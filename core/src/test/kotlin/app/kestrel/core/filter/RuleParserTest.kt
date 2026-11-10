package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleParserTest {
    private val p = RuleParser()
    private fun net(line: String) = p.parseNetwork(line)
    private fun skipOf(line: String): Skip { assertNull("should skip: $line", net(line)); return p.skip }

    @Test
    fun domainAnchoredRule() {
        val r = net("||example.com^")!!
        assertTrue(r.leftHost); assertFalse(r.leftAnchor); assertFalse(r.rightAnchor); assertFalse(r.exception)
        assertEquals("example.com^", r.pattern)
        assertEquals(RequestType.ALL_SUBRESOURCES, r.types)
        assertEquals(0, r.party)
    }

    @Test
    fun exceptionWithOptions() {
        val r = net("@@||example.com/ok\$script,third-party")!!
        assertTrue(r.exception)
        assertEquals(RequestType.SCRIPT, r.types)
        assertEquals(1, r.party)
        assertEquals("example.com/ok", r.pattern)
    }

    @Test
    fun anchorsAndWildcards() {
        val r = net("|http://x.com/a|")!!
        assertTrue(r.leftAnchor); assertTrue(r.rightAnchor); assertEquals("http://x.com/a", r.pattern)
        assertEquals("/ads/", net("/ads/*")!!.pattern) // trailing wildcard is redundant
        assertEquals("/a*b", net("/a***b")!!.pattern)
        assertEquals("ads", net("*ads")!!.pattern)
        assertFalse(net("|*ads")!!.leftAnchor)
        assertEquals("", net("*\$script")!!.pattern) // options-only rule
    }

    @Test
    fun patternIsLowerCased() {
        assertEquals("example.com/ads^", net("||Example.COM/Ads^")!!.pattern)
    }

    @Test
    fun typeOptions() {
        assertEquals(RequestType.IMAGE, net("||e.com^\$image,~third-party")!!.types)
        assertEquals(2, net("||e.com^\$image,~third-party")!!.party)
        assertEquals(RequestType.ALL_SUBRESOURCES and RequestType.SCRIPT.inv(), net("||e.com^\$~script")!!.types)
        assertEquals(RequestType.ALL, net("||e.com^\$all")!!.types)
        assertEquals(RequestType.DOCUMENT, net("||e.com^\$document")!!.types)
        assertEquals(RequestType.XHR, net("||e.com^\$xmlhttprequest")!!.types)
        assertEquals(RequestType.XHR or RequestType.SCRIPT, net("||e.com^\$xhr,script")!!.types)
        assertEquals(1, net("||e.com^\$3p")!!.party)
        assertEquals(2, net("||e.com^\$1p")!!.party)
    }

    @Test
    fun domainOption() {
        val r = net("||e.com^\$domain=a.com|~b.com")!!
        assertEquals(1, r.include!!.size)
        assertEquals(1, r.exclude!!.size)
        assertEquals(Hash.h64("a.com"), r.include!![0])
        assertEquals(Hash.h64("b.com"), r.exclude!![0])
        assertEquals(Skip.ENTITY_DOMAIN, skipOf("||e.com^\$domain=google.*"))
    }

    @Test
    fun unsupportedConstructsAreSkippedNotMisread() {
        assertEquals(Skip.REGEX, skipOf("/banner/"))
        assertEquals(Skip.REGEX, skipOf("/ad[sz]/\$script"))
        for (o in listOf("csp=script-src 'none'", "redirect=noopjs", "removeparam=x", "match-case", "replace=/a/b/", "header=x", "inline-script", "denyallow=x.com"))
            assertEquals("option $o", Skip.UNSUPPORTED_OPTION, skipOf("||e.com^\$$o"))
        assertEquals(Skip.NON_ASCII, skipOf("||bücher.example^"))
        assertEquals(Skip.BAD_SYNTAX, skipOf("||e.com/a b^"))
        assertEquals(Skip.BAD_SYNTAX, skipOf("|\$script"))
        assertEquals(Skip.EMPTY, skipOf("   "))
    }

    @Test
    fun popupRules() {
        assertEquals(RequestType.POPUP, net("||pop.example^\$popup")!!.types)
        assertEquals(RequestType.POPUP or RequestType.SUBDOCUMENT, net("/earn.php?z=\$popup,subdocument")!!.types)
        assertEquals("a plain rule never applies to popups", 0, net("||pop.example^")!!.types and RequestType.POPUP)
        assertEquals("nor does \$all", 0, net("||pop.example^\$all")!!.types and RequestType.POPUP)
    }

    @Test
    fun siteLevelExceptions() {
        assertEquals(SITE_DOCUMENT_ALLOW, net("@@||example.com^\$document")!!.siteFlags)
        assertEquals(SITE_ELEMHIDE, net("@@||example.com^\$elemhide")!!.siteFlags)
        assertEquals(SITE_GENERICHIDE, net("@@||example.com^\$generichide")!!.siteFlags)
        assertEquals(SITE_ELEMHIDE or SITE_GENERICHIDE, net("@@||example.com^\$elemhide,generichide")!!.siteFlags)
        assertEquals(Skip.UNSUPPORTED_OPTION, skipOf("||example.com^\$elemhide")) // only valid on exceptions
        assertEquals(Skip.UNSUPPORTED_OPTION, skipOf("@@/some/path\$document")) // must be a host rule
    }

    @Test
    fun badfilterKeyMatchesTheRuleItCancels() {
        val a = net("||x.com^\$script")!!
        val b = net("||x.com^\$script,badfilter")!!
        assertTrue(b.badfilter)
        assertEquals(a.key, b.key)
        assertNotNull(net("||x.com^"))
        assertFalse(net("||x.com^")!!.key == a.key)
    }

    @Test
    fun cosmeticRules() {
        val a = p.parseCosmetic("example.com##.ad-box")!!
        assertEquals(listOf("example.com"), a.domains); assertEquals(".ad-box", a.selector); assertFalse(a.exception)
        assertTrue(p.parseCosmetic("##.ad")!!.domains.isEmpty())
        assertEquals(listOf("a.com", "b.com"), p.parseCosmetic("a.com,b.com##.x")!!.domains)
        assertTrue(p.parseCosmetic("example.com#@#.ad")!!.exception)

        fun skipC(l: String): Skip { assertNull("should skip: $l", p.parseCosmetic(l)); return p.skip }
        assertEquals(Skip.COSMETIC_EXCLUSIONS, skipC("~a.com##.x"))
        assertEquals(Skip.ENTITY_DOMAIN, skipC("example.*##.x"))
        assertEquals(Skip.COSMETIC_UNSUPPORTED, skipC("example.com##+js(aopr, x)"))
        assertEquals(Skip.COSMETIC_UNSUPPORTED, skipC("example.com#?#div:has-text(x)"))
        assertEquals(Skip.COSMETIC_UNSUPPORTED, skipC("example.com##div:has-text(x)"))
        assertEquals(Skip.COSMETIC_UNSUPPORTED, skipC("example.com#\$#body { x }"))
        assertEquals(Skip.COSMETIC_UNSAFE, skipC("##.a{background:url(http://evil/)}"))
        assertEquals(Skip.COSMETIC_UNSAFE, skipC("##.a\\:b"))
        assertEquals(Skip.COSMETIC_UNSAFE, skipC("##a[href=\"x]"))
    }

    @Test
    fun cosmeticDetection() {
        for (l in listOf("##.a", "a.com##.a", "a.com#@#.a", "a.com#?#.a", "a.com#\$#x", "a.com#%#x")) assertTrue(l, RuleParser.looksCosmetic(l))
        for (l in listOf("||a.com^", "/path#frag", "||a.com/x#y")) assertFalse(l, RuleParser.looksCosmetic(l))
    }

    @Test
    fun hostNames() {
        for (h in listOf("example.com", "a-b.example.co.uk", "x_y.example", "123.example")) assertTrue(h, RuleParser.isHostName(h))
        for (h in listOf("", ".example.com", "example.com.", "ex..ample", "-x.com", "a b.com", "A.com", "a*.com", "a/b.com")) assertFalse(h, RuleParser.isHostName(h))
    }
}
