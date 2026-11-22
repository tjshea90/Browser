package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** PERF2a (owner 2026-11-21): the two list rules that change pornhub.com's own layout are dropped for that site only; everything else is left alone. */
class CosmeticFixesTest {
    private val sep = CosmeticSelectors.STYLE_SEP

    @Test fun onlyTheNamedSelectorsOfTheNamedSiteAreDropped() {
        assertTrue(CosmeticFixes.drops("www.pornhub.com", "div.container$sep" + "margin-top:0px !important"))
        assertTrue(CosmeticFixes.drops("pornhub.com", ".video-wrapper > #player ~ .hd.clear"))
        assertFalse("another rule of the same site stays", CosmeticFixes.drops("www.pornhub.com", ".sideAds"))
        assertTrue(CosmeticFixes.drops("www.pornhub.com", ".watchpageAd"))
        assertFalse("the same selector on another site stays", CosmeticFixes.drops("news.example", "div.container$sep" + "margin-top:0px !important"))
        assertFalse("a look-alike host is not the site", CosmeticFixes.drops("notpornhub.com", "div.container"))
        val rules = listOf(".a", "div.container$sep" + "margin-top:0px !important", ".b")
        assertEquals(listOf(".a", ".b"), CosmeticFixes.filter("www.pornhub.com", rules))
        assertTrue("the same list object when nothing is dropped", CosmeticFixes.filter("news.example", rules) === rules)
    }

    @Test fun theRealBundledListsNoLongerChangePornhubsLayoutButKeepTheirOtherRules() {
        var d: File? = File("").absoluteFile; var fs: FilterSet? = null
        while (d != null) { val f = File(d, "app/src/main/assets/filters/default.kfs"); if (f.isFile) { fs = FilterSetCodec.read(f.readBytes()); break }; d = d.parentFile }
        assumeTrue("default.kfs not found", fs != null)
        val css = fs!!.cosmeticCss("www.pornhub.com", includeGeneric = false)
        assertFalse(css, css.contains("div.container{margin-top"))
        assertFalse(css, css.contains(".video-wrapper > #player ~ .hd.clear"))
        for (gone in listOf(".watchpageAd{", ".mgp_overlayContainer{", ".middleAdContainer{", ".noBottom{", ".ad-tabSplit{", ".clear.hd:has(.adsbytrafficjunky)")) assertFalse("$gone is dropped: $css", css.contains(gone))
        assertTrue("the other ad rules of the site are still there: $css", css.contains(".sideAds{display:none!important}") && css.contains("trafficjunky") && css.contains("#globalCookieBanner{display:none!important}"))
        val other = fs.cosmeticCss("www.example.org", includeGeneric = false)
        assertFalse("nothing of pornhub's leaks onto another host", other.contains(".sideAds"))
    }
}
