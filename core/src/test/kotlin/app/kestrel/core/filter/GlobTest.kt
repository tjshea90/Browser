package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Test

/** Direct tests of the pattern matcher against hand-derived ABP semantics (no engine involved). */
class GlobTest {
    private fun m(pattern: String, url: String, start: Int = 0, right: Boolean = false): Boolean {
        val b = pattern.toByteArray(Charsets.US_ASCII)
        return FilterSet.glob(b, 0, b.size, url, start, right)
    }

    /** Unanchored search, like a rule with no anchors. */
    private fun anywhere(pattern: String, url: String, right: Boolean = false): Boolean =
        (0..url.length).any { m(pattern, url, it, right) }

    @Test
    fun literals() {
        assertEquals(true, anywhere("/ads/", "https://x.com/ads/y"))
        assertEquals(false, anywhere("/ads/", "https://x.com/adz/y"))
        assertEquals(true, m("https://x", "https://x.com/"))
        assertEquals(false, m("https://x", "http://x.com/"))
    }

    @Test
    fun separatorMatchesPunctuationAndEndButNotWordChars() {
        for (sep in listOf("/", "?", "&", "=", ":", ";", "#", "@", ",", "!", "~", "+")) {
            assertEquals("sep $sep", true, anywhere("/banner^", "https://x.com/banner${sep}z"))
        }
        assertEquals("end of url", true, anywhere("/banner^", "https://x.com/banner"))
        for (word in listOf("a", "7", "_", "-", ".", "%")) {
            assertEquals("word $word", false, anywhere("/banner^", "https://x.com/banner${word}z"))
        }
    }

    @Test
    fun wildcards() {
        assertEquals(true, anywhere("/ads/*/banner", "https://x.com/ads/a/b/banner.png"))
        assertEquals(true, anywhere("/ads/*/banner", "https://x.com/ads//banner"))
        assertEquals(false, anywhere("/ads/*/banner", "https://x.com/ads/banner"))
        assertEquals(true, anywhere("a*b*c", "xxaxxbxxcxx"))
        assertEquals(false, anywhere("a*b*c", "xxaxxcxxbxx"))
    }

    @Test
    fun rightAnchorNeedsTheEnd() {
        assertEquals(true, anywhere(".gif", "https://x.com/a.gif?x", right = false))
        assertEquals(false, anywhere(".gif", "https://x.com/a.gif?x", right = true))
        assertEquals(true, anywhere(".gif", "https://x.com/a.gif", right = true))
        assertEquals(true, anywhere("a*b", "xxaxxbxxb", right = true)) // star must absorb the first 'b'
    }

    @Test
    fun backtrackingThroughTheStar() {
        assertEquals(true, anywhere("a*ab", "aaab"))
        assertEquals(true, anywhere("*ab", "xxabab"))
        assertEquals(true, m("a*a*a*b", "aaaab"))
        assertEquals(false, m("a*a*a*b", "aaaa"))
    }

    @Test
    fun caretAtEndConsumesNothing() {
        assertEquals(true, m("x^", "x"))
        assertEquals(true, m("x^", "x/"))
        assertEquals(false, m("x^", "xa"))
        assertEquals(true, m("x^^", "x"))
    }

    @Test
    fun adversarialPatternsTerminateQuickly() {
        val url = "a".repeat(2000) + "b"
        val t0 = System.nanoTime()
        assertEquals(false, anywhere("a*a*a*a*a*a*c", url))
        val ms = (System.nanoTime() - t0) / 1_000_000
        assert(ms < 3000) { "pathological pattern took ${ms}ms" }
    }
}
