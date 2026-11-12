package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** D2 on the REAL bundled lists (app/src/main/assets/filters/default.kfs): how much of the generic set the token index covers, how big a bucket gets, what a page pays. */
class RealCosmeticTokenIndexTest {
    private fun load(): FilterSet? {
        var d: File? = File("").absoluteFile
        while (d != null) { val f = File(d, "app/src/main/assets/filters/default.kfs"); if (f.isFile) return FilterSetCodec.read(f.readBytes()); d = d.parentFile }
        return null
    }

    @Test fun theRealGenericSetIsCoveredAndPagesPullInOnlyAFewSelectors() {
        val fs = load()
        assumeTrue("default.kfs not found", fs != null)
        fs!!
        val t0 = System.nanoTime()
        fs.warmCosmetic()
        val buildMs = (System.nanoTime() - t0) / 1_000_000
        val stats = fs.cosmeticDebugStats()
        println("D2 stats: " + stats + " buildMs=" + buildMs)
        println("D2 sample keys: " + fs.cosmeticDebugKeys(2500).joinToString(" "))
        println("D2 well-known: " + listOf(".adsbygoogle", "#adbanner", ".ad-banner", ".ad-container", "#google_ads", ".sponsored", ".advert").joinToString(" ") { k -> k + "=" + fs.genericCssForTokens("a.example", listOf(k)).length })
        for (k in listOf(".ad-unit-wrapper", "#siteadheader", ".advert__leaderboard", "#google_ads", ".ad-container", ".adsbygoogle")) println("D2 css for " + k + ": " + fs.genericCssForTokens("a.example", listOf(k)).replace("\n", " "))
        // the up-front CSS in token mode versus the old 1,200-selector CSS
        val old = fs.cosmeticCss("news.example", tokenMode = false)
        val up = fs.cosmeticCss("news.example", tokenMode = true)
        println("D2 css bytes: old up-front=" + old.length + " token-mode up-front=" + up.length)
        // a typical page: a few hundred distinct classes / ids from a real-looking mix
        val common = listOf(".container", ".row", ".col", ".header", ".footer", ".nav", ".menu", ".content", ".main", ".sidebar", ".wrapper", ".button", ".btn", ".title", ".text", ".image", ".link", ".item", ".list", "#header", "#footer", "#content", "#main", "#nav", "#wrapper", "#page", ".ad", ".ads", ".advert", ".banner", "#ad", "#ads", ".sponsored", ".promo", "#sponsor", ".adsbygoogle", ".ad-container", ".ad-banner", ".ad-slot")
        val css = fs.genericCssForTokens("news.example", common)
        println("D2 typical token batch: css bytes=" + css.length + " selectors=" + css.count { it == ',' } )
        assertTrue("the index must exist and be built quickly (ms)", buildMs < 5000)
        assertTrue(css.length < 60_000)
        assertEquals(true, up.startsWith(CosmeticIndex.TOKEN_MARKER) || stats.contains("keyed=0"))
    }
}
