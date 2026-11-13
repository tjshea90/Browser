package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Procedural cosmetic filters (AD1b-4): `:has-text()`, `:upward()`, `:remove()` ... for a site. Third-party input: the subset is small and strict, anything else is skipped, never half applied. */
class ProceduralRulesTest {
    private val P = ProceduralRules.PREFIX
    private val O = ProceduralRules.OP
    private val A = ProceduralRules.ARG

    private fun enc(s: String) = ProceduralRules.encode(s)
    private fun parse(line: String): CosmeticRule? = RuleParser().parseCosmetic(line)
    private fun skip(line: String): Skip { val p = RuleParser(); assertNull("should be skipped: $line", p.parseCosmetic(line)); return p.skip }

    @Test fun theCommonShapesAreEncodedWithTheirOperators() {
        assertEquals("${P}div[role=\"feed\"] > div${O}t${A}Sponsored", enc("div[role=\"feed\"] > div:has-text(Sponsored)"))
        assertEquals("${P}.card${O}t${A}/promoted/i${O}u${A}2", enc(".card:has-text(/promoted/i):upward(2)"))
        assertEquals("${P}div.overlay${O}t${A}Subscribe${O}r$A", enc("div.overlay:has-text(Subscribe):remove()"))
        assertEquals("aliases of has-text", "${P}li${O}t${A}Ad", enc("li:contains(Ad)"))
        assertEquals("aliases of has-text", "${P}li${O}t${A}Ad", enc("li:-abp-contains(Ad)"))
        assertEquals("${P}span.x${O}t${A}Ad${O}u$A.post", enc("span.x:has-text(Ad):upward(.post)"))
        assertEquals("${P}img${O}t${A}x${O}a${A}style", enc("img:has-text(x):remove-attr(style)"))
        assertEquals("${P}div${O}t${A}x${O}c${A}blur", enc("div:has-text(x):remove-class(blur)"))
        assertEquals("a regex may hold parentheses and escapes", "${P}p${O}t$A/(Ad|Sponsored)\\s+post/", enc("p:has-text(/(Ad|Sponsored)\\s+post/)"))
        assertEquals("a native :has() in the base is fine", "${P}div:has(> a.x)${O}t${A}Ad", enc("div:has(> a.x):has-text(Ad)"))
    }

    @Test fun theStoredFormDecodesBack() {
        val r = ProceduralRules.decode(enc(".card:has-text(/promoted/i):upward(2):remove()")!!)!!
        assertEquals(".card", r.base)
        assertEquals(listOf("t" to "/promoted/i", "u" to "2", "r" to ""), r.ops.map { it.name to it.arg })
        assertNull(ProceduralRules.decode(".plain"))
    }

    @Test fun whatIsOutsideTheSubsetOrUnsafeIsRefused() {
        for (bad in listOf(
            ":has-text(x)", "div:xpath(//a)", "div:matches-css(color: red)", "div:others()", "div:watch-attr(class)",            // not supported / no base
            "a:has-text(x) > b", "a:has-text(x) span", "a:has-text(x):nth-child(2)",                                            // a selector after an operator
            "a:remove():has-text(x)", "a:remove-attr(x):remove()",                                                              // an action ends the chain
            "a:has-text(x):has-text(y):has-text(z):upward(1):has-text(w)",                                                      // too many operators
            "a:upward(1)", "a:upward(2):upward(3)",                                                                             // nothing but moving up decides nothing
            "a:has-text()", "a:has-text(\u0001)", "a:upward(0)", "a:upward(11)", "a:upward(-1)", "a:upward(.x{y})",
            "a:remove(x)", "a:remove-attr()", "a:remove-attr(1x)", "a:remove-attr(on click)", "a:remove-class(a b)",
            "a{b}:has-text(x)", "a:has-text(/x/):has-text(", "a:has-text(\"unclosed)",
            "a:has-text(/\\x41/)", "a:has-text(/[/)", "a:has-text(/x/zz)", "a:has-text(/<script>/)", "a:has-text(a\\b)",
            "a:has-text(" + "x".repeat(121) + ")", "a:has-text(/" + "x".repeat(161) + "/)",
        )) assertNull("must be refused: $bad", enc(bad))
    }

    @Test fun aSiteRuleIsAcceptedAndAGenericOneIsNot() {
        val r = parse("facebook.com##div[role=\"feed\"] > div:has-text(Sponsored)")!!
        assertEquals(listOf("facebook.com"), r.domains)
        assertTrue(ProceduralRules.isStored(r.selector))
        assertFalse(r.exception)
        assertEquals("the ABP spelling of an extended selector", r.selector, parse("facebook.com#?#div[role=\"feed\"] > div:-abp-contains(Sponsored)")!!.selector)
        assertEquals("every site is too broad", Skip.COSMETIC_UNSUPPORTED, skip("##.x:has-text(Ad)"))
        assertEquals("an unsupported operator", Skip.COSMETIC_UNSUPPORTED, skip("a.example##div:xpath(//a)"))
        assertEquals("an unsafe base", Skip.COSMETIC_UNSAFE, skip("a.example##div{x}"))
    }

    @Test fun aPlainCssRuleWrittenWithTheExtendedSpellingStaysACssRule() {
        val r = parse("a.example#?#.banner")!!
        assertEquals(".banner", r.selector)
    }

    @Test fun theCssOfASiteNeverContainsTheRulesButCarriesTheMarkerAndTheRulesCanBeAsked() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf("news.example##.sticky-ad", "news.example##article:has-text(Sponsored):upward(1)", "other.example##.x:has-text(Ad):remove()", "##.generic:has-text(Ad)"))
        val fs = b.build(1L)
        assertEquals("a generic procedural rule is skipped", 2, b.report.cosmeticSite - 1 + 1)
        val css = fs.cosmeticCss("news.example", tokenMode = true)
        assertTrue(css, css.contains(".sticky-ad{display:none!important}"))
        assertTrue("the page script is told to ask: $css", css.contains(ProceduralRules.MARKER))
        assertFalse("the rule is not CSS: $css", css.contains("Sponsored") || css.contains('\u0002'))
        val rules = fs.proceduralFor("news.example")
        assertEquals(1, rules.size)
        assertEquals("article${O}t${A}Sponsored${O}u${A}1", rules[0])
        assertEquals("subdomains inherit", rules, fs.proceduralFor("m.news.example"))
        assertEquals(listOf(".x${O}t${A}Ad${O}r$A"), fs.proceduralFor("other.example"))
        assertTrue("a site without such rules gets no marker", !fs.cosmeticCss("elsewhere.example", tokenMode = true).contains(ProceduralRules.MARKER))
        assertEquals(emptyList<String>(), fs.proceduralFor("elsewhere.example"))
    }

    @Test fun anExceptionRemovesTheRuleForThatSiteOnlyAndElemhideTurnsThemAllOff() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf(
            "a.example,b.example##.card:has-text(Ad)", "b.example#@#.card:has-text(Ad)", "c.example##.card:has-text(Ad)", "@@||c.example^$" + "elemhide",
        ))
        val fs = b.build(1L)
        assertEquals(1, fs.proceduralFor("a.example").size)
        assertEquals("the same rule, excepted on b.example (`#@#` spelled like the rule)", emptyList<String>(), fs.proceduralFor("b.example"))
        assertEquals("\$elemhide on the site", emptyList<String>(), fs.proceduralFor("c.example"))
        assertFalse(fs.cosmeticCss("b.example", tokenMode = true).contains(ProceduralRules.MARKER))
    }

    @Test fun theRulesSurviveTheBinaryFile() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf("news.example##article:has-text(/sponsor(ed)?/i):upward(1)"))
        val back = FilterSetCodec.read(FilterSetCodec.write(b.build(1L)))
        val rules = back.proceduralFor("news.example")
        assertEquals(1, rules.size)
        assertNotNull(ProceduralRules.decode(ProceduralRules.PREFIX + rules[0]))
        assertTrue(back.cosmeticCss("news.example", tokenMode = true).contains(ProceduralRules.MARKER))
    }
}
