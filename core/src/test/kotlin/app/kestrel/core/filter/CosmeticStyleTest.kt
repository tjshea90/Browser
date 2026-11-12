package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `site##selector:style(declarations)` (AA3-3b): lists restyle instead of hiding (scroll unlocks, "unbreak" fixes). Third-party input: only plain, safe declarations get through. */
class CosmeticStyleTest {
    private val sep = CosmeticSelectors.STYLE_SEP

    private fun parse(line: String): CosmeticRule? = RuleParser().parseCosmetic(line)
    private fun skip(line: String): Skip { val p = RuleParser(); assertNull("should be skipped: $line", p.parseCosmetic(line)); return p.skip }

    @Test fun aSiteStyleRuleIsKeptAsSelectorAndDeclarations() {
        val r = parse("alliptvlinks.com##body:style(overflow: auto !important)")!!
        assertEquals(listOf("alliptvlinks.com"), r.domains)
        assertEquals("body${sep}overflow:auto !important", r.selector)
        assertFalse(r.exception)
        val many = parse("apkmirror.com##.gooWidget:style(opacity: 0 !important; visibility: collapse !important;)")!!
        assertEquals(".gooWidget${sep}opacity:0 !important;visibility:collapse !important", many.selector)
        assertEquals("[id^=\"adtester-container\"]${sep}position:absolute !important", parse("apkmirror.com##[id^=\"adtester-container\"]:style(position: absolute !important;)")!!.selector)
        assertNotNull(parse("a.example,b.example##div.x > p:style(display:initial!important)"))
    }

    @Test fun whatWouldBeUnsafeOrTooBroadIsLeftOut() {
        assertEquals("every site", Skip.COSMETIC_UNSUPPORTED, skip("##body:style(overflow:auto!important)"))
        assertEquals("an exception cannot restyle", Skip.COSMETIC_UNSUPPORTED, skip("a.example#@#.x:style(display:none)"))
        for (bad in listOf(
            "a.example##.x:style(background: url(https://evil.example/t.gif))", "a.example##.x:style(background-image: image-set(a 1x))", "a.example##.x:style(color: red; } body { display: none)",
            "a.example##.x:style(content: \"hi\")", "a.example##.x:style(width: expression(alert(1)))", "a.example##.x:style(behavior: foo)", "a.example##.x:style(a: b\\c)", "a.example##.x:style(@import x)",
            "a.example##.x:style(position: fixed; z-index: 1; top: calc(1px )", "a.example##.x:style()", "a.example##.x:style( ; )", "a.example##.x:style(novalue)", "a.example##.x:style(Co lor: red)",
        )) assertEquals("unsafe: $bad", Skip.COSMETIC_UNSAFE, skip(bad))
        assertEquals("a procedural base is not CSS", Skip.COSMETIC_UNSUPPORTED, skip("a.example##.x:has-text(Ad):style(display:none)"))
        assertEquals("an unsafe base", Skip.COSMETIC_UNSAFE, skip("a.example##.x{y}:style(display:none)"))
    }

    @Test fun declarationsAreNormalisedAndLimited() {
        assertEquals("display:none !important", CosmeticSelectors.acceptDeclarations(" Display : none !important ;"))
        assertEquals("height:calc(100% - 10px);margin:0 auto", CosmeticSelectors.acceptDeclarations("height: calc(100% - 10px); margin: 0 auto"))
        assertEquals("transform:translateY(-50%)", CosmeticSelectors.acceptDeclarations("transform:translateY(-50%)"))
        assertNull(CosmeticSelectors.acceptDeclarations("x".repeat(301)))
        assertNull(CosmeticSelectors.acceptDeclarations("width: " + "9".repeat(130)))
        assertNull("an unbalanced parenthesis would swallow the rest of the stylesheet", CosmeticSelectors.acceptDeclarations("width: calc(1px"))
        assertNull(CosmeticSelectors.acceptDeclarations("width: 1px)"))
        assertEquals("a control character is never accepted", null, CosmeticSelectors.acceptDeclarations("width: 1px\u0001"))
    }

    @Test fun theCssIsOneRuleOfItsOwnAndNeverFusesWithTheHidingGroupAround() {
        val css = CosmeticSelectors.toCss(listOf(".a", ".b", "body${sep}overflow:auto !important", ".c"), 1)
        assertEquals(".a{display:none!important}\n.b{display:none!important}\nbody{overflow:auto !important}\n.c{display:none!important}\n", css)
        val grouped = CosmeticSelectors.toCss(listOf("a", "b", "x${sep}top:0", "c"), 10)
        assertEquals("a,b{display:none!important}\nx{top:0}\nc{display:none!important}\n", grouped)
    }

    @Test fun aSiteGetsItsRestylingRulesNextToItsHidingRulesAndSubdomainsInheritThem() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf("news.example##.sticky-ad", "news.example##body:style(overflow: auto !important; position: static !important)", "other.example##.x:style(display:none)", "! a comment"))
        val fs = b.build(1L)
        assertEquals("both kinds count as site rules", 3, b.report.cosmeticSite)
        val css = fs.cosmeticCss("news.example")
        assertTrue(css, css.contains(".sticky-ad{display:none!important}"))
        assertTrue(css, css.contains("body{overflow:auto !important;position:static !important}"))
        assertTrue("subdomains inherit", fs.cosmeticCss("m.news.example").contains("body{overflow:auto !important"))
        assertFalse("another site's rule stays there", css.contains(".x{"))
        assertEquals("a site without rules gets nothing", "", fs.cosmeticCss("elsewhere.example"))
    }

    @Test fun theRulesSurviveTheBinaryFile() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf("news.example##body:style(overflow: auto !important)"))
        val back = FilterSetCodec.read(FilterSetCodec.write(b.build(1L)))
        assertTrue(back.cosmeticCss("news.example").contains("body{overflow:auto !important}"))
    }

    @Test fun aSiteExceptionForAnotherSelectorDoesNotRemoveTheRestylingRule() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, sequenceOf("news.example##body:style(overflow:auto!important)", "news.example##.ad", "news.example#@#.ad"))
        val css = b.build(1L).cosmeticCss("news.example")
        assertTrue(css, css.contains("body{overflow:auto!important}"))
        assertFalse(css, css.contains(".ad{"))
    }
}
