package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D2: the class / id a generic selector can be filed under, and the lazy token path through a real [FilterSet]. */
class CosmeticTokensTest {
    private fun keys(s: String) = CosmeticTokens.keysOf(s)

    // ---------------------------------------------------------------- which tokens a selector REQUIRES

    @Test fun simpleClassesAndIdsAreTheirOwnKey() {
        assertEquals(listOf(".ad-banner"), keys(".ad-banner"))
        assertEquals(listOf("#sponsor"), keys("#sponsor"))
        assertEquals(listOf(".promo"), keys("div.promo"))
        assertEquals(listOf(".promo", "#top"), keys("div.promo > a#top"))
        assertEquals(listOf(".a", ".b"), keys(".a.b"))
        assertEquals(listOf(".ad-box", ".title"), keys("section.ad-box .title:hover"))
    }

    @Test fun keysAreLowerCasedAndRepeatsCollapse() {
        assertEquals(listOf(".adbox", "#top"), keys(".AdBox #Top .adBOX"))
    }

    @Test fun tokensInsideBracketsParenthesesAndQuotesAreNeverUsed() {
        assertEquals(emptyList<String>(), keys("a[href*=\".ad\"]"))
        assertEquals(emptyList<String>(), keys("a[href=\"#top\"]"))
        assertEquals(emptyList<String>(), keys("div:not(.keep)"))
        assertEquals(emptyList<String>(), keys("div:is(.a, .b)"))
        assertEquals(emptyList<String>(), keys("div:has(> .ad)"))
        assertEquals(listOf(".real"), keys("div.real:not(.no)[data-x=\".x\"]"))
        assertEquals(listOf("#id"), keys("[class*=\"a.b\"] #id"))
    }

    @Test fun aSelectorListIsNotIndexedByOneOfItsParts() {
        assertEquals(emptyList<String>(), keys(".a, .b"))
        assertEquals(emptyList<String>(), keys(".a,div"))
        assertEquals("a comma inside :is() or a value is not a list", listOf(".c"), keys(".c:is(.x,.y)"))
        assertEquals(listOf(".c"), keys("a.c[title=\"x,y\"]"))
    }

    @Test fun selectorsWithoutAnyClassOrIdHaveNoKey() {
        assertEquals(emptyList<String>(), keys("div"))
        assertEquals(emptyList<String>(), keys("a[href^=\"http://ads.\"]"))
        assertEquals(emptyList<String>(), keys("ins > iframe"))
        assertEquals(emptyList<String>(), keys("*"))
    }

    @Test fun oddInputNeverThrows() {
        for (s in listOf("", ".", "#", "..a", "##a", ".a.", "[", "]", "(", ")", "\"", "'", ".été", "#a-1_b", ".-x", "a.b)c.d")) keys(s)
        assertEquals(listOf(".été"), keys(".Été".replace("É", "é")))
        assertEquals(listOf(".b", ".d"), keys("a.b)c.d"))
    }

    @Test fun overlongNamesAreNotIndexed() {
        assertEquals(emptyList<String>(), keys("." + "a".repeat(CosmeticTokens.MAX_LEN + 1)))
        assertEquals(1, keys("." + "a".repeat(CosmeticTokens.MAX_LEN)).size)
    }

    @Test fun pageTokensAreValidatedBeforeTheyTouchTheIndex() {
        assertEquals(".ad", CosmeticTokens.normalise(".AD"))
        assertEquals("#x-1", CosmeticTokens.normalise("#X-1"))
        assertNull(CosmeticTokens.normalise("ad"))
        assertNull(CosmeticTokens.normalise("."))
        assertNull(CosmeticTokens.normalise(".a b"))
        assertNull(CosmeticTokens.normalise(".a{}"))
        assertNull(CosmeticTokens.normalise(".a,.b"))
        assertNull(CosmeticTokens.normalise("." + "x".repeat(CosmeticTokens.MAX_LEN + 1)))
        assertNull(CosmeticTokens.normalise("[x]"))
    }

    // ---------------------------------------------------------------- through a real FilterSet

    private fun fs(vararg rules: String): FilterSet {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.ADS, rules.asSequence())
        return b.build(1L)
    }

    private val generic = arrayOf(
        "##.ad-banner", "###sponsor", "##div.promo > a", "##.card .sponsored-xyz", "##.card .plain", "##a[href^=\"http://ads.\"]", "##ins.adsbygoogle", "##.a, .b",
        "##div:not(.keep)",
    )

    @Test fun onlyTheSelectorsAPageCanMatchAreReturned() {
        val f = fs(*generic)
        val css = f.genericCssForTokens("news.example", listOf(".ad-banner"))
        assertTrue(css, css.contains(".ad-banner") && css.contains("display:none!important"))
        assertFalse(css, css.contains("#sponsor") || css.contains("promo") || css.contains("adsbygoogle"))
        val css2 = f.genericCssForTokens("news.example", listOf("#sponsor", ".promo", ".adsbygoogle"))
        assertTrue(css2, css2.contains("#sponsor") && css2.contains("div.promo > a") && css2.contains("ins.adsbygoogle"))
        assertEquals("", f.genericCssForTokens("news.example", listOf(".not-in-any-list", "#nothing")))
        assertEquals("", f.genericCssForTokens("news.example", emptyList()))
    }

    @Test fun aSelectorIsFiledUnderItsRarestTokenSoACommonClassDoesNotPullInEveryRuleOnIt() {
        val f = fs("##.card .sponsored-xyz", "##.card .plain-one", "##.card .plain-two", "##.card .plain-three")
        val common = f.genericCssForTokens("a.example", listOf(".card"))
        assertEquals("only the page's .card: nothing pulled in", "", common)
        val rare = f.genericCssForTokens("a.example", listOf(".card", ".sponsored-xyz"))
        assertTrue(rare, rare.contains(".card .sponsored-xyz") && !rare.contains("plain"))
    }

    @Test fun matchingIsCaseInsensitiveBothWays() {
        val f = fs("##.AdBox", "###TopAd")
        val css = f.genericCssForTokens("a.example", listOf(".adbox", "#TOPAD"))
        assertTrue(css, css.contains(".AdBox") && css.contains("#TopAd"))
    }

    @Test fun selectorListsAndSelectorsWithoutATokenStayInTheUpFrontCss() {
        val f = fs(*generic)
        val up = f.cosmeticCss("news.example", includeGeneric = true, tokenMode = true)
        assertTrue("marker first", up.startsWith(CosmeticIndex.TOKEN_MARKER))
        assertTrue(up, up.contains("a[href^=\"http://ads.\"]") && up.contains("div:not(.keep)") && up.contains(".a, .b"))
        assertFalse("token selectors wait for the page's tokens: $up", up.contains("#sponsor") || up.contains(".ad-banner") || up.contains("adsbygoogle"))
    }

    @Test fun withoutTokenModeTheOldBehaviourIsUnchangedAndCarriesNoMarker() {
        val f = fs(*generic)
        val old = f.cosmeticCss("news.example")
        assertFalse(old.contains(CosmeticIndex.TOKEN_MARKER))
        assertTrue(old, old.contains("#sponsor") && old.contains(".ad-banner") && old.contains("div:not(.keep)"))
    }

    @Test fun siteRulesStillComeFirstAndOnlyForTheirSite() {
        val f = fs("news.example##.site-only", *generic)
        assertTrue(f.cosmeticCss("news.example", tokenMode = true).contains(".site-only"))
        assertFalse(f.cosmeticCss("other.example", tokenMode = true).contains(".site-only"))
    }

    @Test fun exceptionsAndGenericHideApplyToTheLazyPathToo() {
        val f = fs("##.ad-banner", "###sponsor", "news.example#@#.ad-banner", "@@||quiet.example^\$generichide", "@@||off.example^\$elemhide")
        val news = f.genericCssForTokens("news.example", listOf(".ad-banner", "#sponsor"))
        assertFalse("excepted on this site: $news", news.contains(".ad-banner"))
        assertTrue(news, news.contains("#sponsor"))
        assertTrue(f.genericCssForTokens("other.example", listOf(".ad-banner")).contains(".ad-banner"))
        assertEquals("generic hiding is off there", "", f.genericCssForTokens("quiet.example", listOf(".ad-banner", "#sponsor")))
        assertEquals("element hiding is off there", "", f.genericCssForTokens("off.example", listOf(".ad-banner")))
        assertFalse("no marker either", f.cosmeticCss("quiet.example", tokenMode = true).contains(CosmeticIndex.TOKEN_MARKER))
    }

    @Test fun aGlobalExceptionRemovesTheSelectorFromEveryPage() {
        val f = fs("##.ad-banner", "###sponsor", "#@#.ad-banner")
        val css = f.genericCssForTokens("any.example", listOf(".ad-banner", "#sponsor"))
        assertFalse(css, css.contains(".ad-banner"))
        assertTrue(css, css.contains("#sponsor"))
    }

    @Test fun oneBatchNeverReturnsMoreThanTheCap() {
        val many = (0 until 900).map { "##.t$it" }.toTypedArray()
        val f = fs(*many)
        val tokens = (0 until 900).map { ".t$it" }
        val css = f.genericCssForTokens("a.example", tokens, max = 200)
        val n = Regex("\\.t\\d+").findAll(css).count()
        assertEquals(200, n)
    }

    @Test fun hostileTokensFromAPageAreIgnoredNeverReachTheCss() {
        val f = fs("##.ad-banner")
        val css = f.genericCssForTokens("a.example", listOf(".x{}body{display:none}", ".a b", "ad-banner", "", "\u0000", ".ad-banner"))
        assertTrue(css, css.contains(".ad-banner"))
        assertFalse(css, css.contains("body{") || css.contains("x{}"))
    }

    @Test fun anEmptyFilterSetAnswersEverythingWithNothing() {
        assertEquals("", FilterSet.EMPTY.genericCssForTokens("a.example", listOf(".ad")))
        assertEquals("", FilterSet.EMPTY.cosmeticCss("a.example", tokenMode = true))
    }

    @Test fun theTokenIndexSurvivesTheBinaryRoundTrip() {
        val f = fs(*generic)
        val g = FilterSetCodec.read(FilterSetCodec.write(f))
        assertEquals(f.genericCssForTokens("a.example", listOf(".ad-banner", "#sponsor")), g.genericCssForTokens("a.example", listOf(".ad-banner", "#sponsor")))
        assertEquals(f.cosmeticCss("a.example", tokenMode = true), g.cosmeticCss("a.example", tokenMode = true))
    }
}
