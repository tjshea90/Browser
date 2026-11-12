package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The CSS text around the hiding selectors must be well-formed whatever mix of selectors it is given: a browser drops a whole rule for one bad selector, and every list is third-party input. */
class CosmeticCssShapeTest {
    private val hide = "{display:none!important}\n"

    @Test fun plainSelectorsAreGroupedAndAnIsolatedOneCloseTheOpenGroupFirst() {
        val css = CosmeticSelectors.toCss(listOf("a", "b", ".x:not(.y)", "c", "d"), 10)
        assertEquals("a,b$hide.x:not(.y)$hide" + "c,d$hide", css)
    }

    @Test fun anIsolatedSelectorNeverFusesWithItsNeighbourNorLeavesALeadingComma() {
        // the real case that hid nothing: ... #google_ads then .Ad-Container:not(.adsbygoogle) then ins.adsbygoogle[...]
        val css = CosmeticSelectors.toCss(listOf(".a", "#google_ads", ".Ad-Container:not(.adsbygoogle)", "ins.adsbygoogle[data-ad-client]", "ins.adsbygoogle[data-ad-slot]"), 10)
        assertFalse(css, css.contains("#google_ads.Ad-Container"))
        assertFalse(css, css.contains("\n,") || css.startsWith(","))
        assertEquals(".a,#google_ads$hide.Ad-Container:not(.adsbygoogle)$hide" + "ins.adsbygoogle[data-ad-client],ins.adsbygoogle[data-ad-slot]$hide", css)
    }

    @Test fun everyRuleIsWellFormedForAnyMixAndAnyGroupSize() {
        val pool = listOf("a", ".b", "#c", "div:is(.x,.y)", "p:has(> img)", "span[data-k=\"v\"]", "ul li", "section:not(.keep)", "i")
        val rnd = java.util.Random(11)
        for (round in 0 until 300) {
            val n = 1 + rnd.nextInt(25)
            val sel = (0 until n).map { pool[rnd.nextInt(pool.size)] }
            for (chunk in intArrayOf(1, 2, 3, 10)) {
                val css = CosmeticSelectors.toCss(sel, chunk)
                val rules = css.split(hide).filter { it.isNotEmpty() }
                assertEquals("every selector appears once: $css", sel.size, rules.sumOf { r -> splitTop(r).size })
                for (r in rules) {
                    assertFalse("empty selector in: $r", r.isEmpty() || r.startsWith(",") || r.endsWith(",") || r.contains(",,"))
                    // a grouped rule never contains an isolated selector (one with parentheses)
                    if (splitTop(r).size > 1) assertTrue("an isolated selector was grouped: $r", splitTop(r).none { CosmeticSelectors.needsIsolation(it) })
                }
                assertEquals("rules come back out in the order given", sel, rules.flatMap { splitTop(it) })
            }
        }
    }

    @Test fun emptyInputAndTheSingleRuleModeStayAsTheyWere() {
        assertEquals("", CosmeticSelectors.toCss(emptyList(), 10))
        assertEquals("a$hide" + "b$hide", CosmeticSelectors.toCss(listOf("a", "b"), 1))
    }

    /** The top-level comma-separated parts of a rule's selector text (a comma inside parentheses, brackets or quotes is not a separator). */
    private fun splitTop(rule: String): List<String> {
        val out = ArrayList<String>(); var depth = 0; var q = 0.toChar(); var s = 0
        for (i in rule.indices) {
            val c = rule[i]
            if (q != 0.toChar()) { if (c == q) q = 0.toChar(); continue }
            when (c) { '"', '\'' -> q = c; '(', '[' -> depth++; ')', ']' -> depth--; ',' -> if (depth == 0) { out.add(rule.substring(s, i)); s = i + 1 } }
        }
        out.add(rule.substring(s))
        return out
    }
}
