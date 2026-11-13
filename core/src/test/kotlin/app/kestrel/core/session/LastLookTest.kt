package app.kestrel.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure rules of the last look (EY-1, research/REVIEW-EXTERNAL-2.md section 3.3): what is stored, when it may be shown, how it fits, when it must go. */
class LastLookTest {
    private fun meta(url: String = "https://news.test/story", sy: Int = 640, w: Int = 600, h: Int = 900, eng: String = "160.0", skin: String = "neon", dark: Boolean = false, desk: Boolean = false) =
        LastLookMeta(url, sy, w, h, 1_700_000_000_000L, eng, skin, dark, desk)

    @Test fun theRecordSurvivesEncodeAndDecode() {
        val m = meta()
        val back = LastLookMeta.decode(m.encode())!!
        assertEquals(m.url, back.url); assertEquals(640, back.scrollY); assertEquals(600, back.w); assertEquals(900, back.h)
        assertEquals(1_700_000_000_000L, back.at); assertEquals("160.0", back.engine); assertEquals("neon", back.skin); assertFalse(back.dark); assertFalse(back.desktop)
    }

    @Test fun anythingThatIsNotAUsableRecordDecodesToNull() {
        assertNull(LastLookMeta.decode(null)); assertNull(LastLookMeta.decode("")); assertNull(LastLookMeta.decode("{"))
        assertNull(LastLookMeta.decode("""{"v":2,"url":"https://a.test/","w":600,"h":900}"""))        // another version
        assertNull(LastLookMeta.decode("""{"v":1,"url":"","w":600,"h":900}"""))                       // no address
        assertNull(LastLookMeta.decode("""{"v":1,"url":"https://a.test/","w":0,"h":900}"""))          // no picture size
        assertNull(LastLookMeta.decode("[1,2]"))
        assertNull(LastLookMeta.decode("x".repeat(9000)))
    }

    @Test fun onlyRealWebPagesAreKept() {
        assertTrue(LastLookRules.worthKeeping("https://news.test/story?id=1#top")); assertTrue(LastLookRules.worthKeeping("http://example.com/"))
        for (u in listOf("about:blank", "data:text/html,hi", "file:///sdcard/x.html", "", "javascript:alert(1)", "https://user:pw@bank.test/", "https://user@bank.test/")) assertFalse(u, LastLookRules.worthKeeping(u))
    }

    @Test fun thePictureIsUsedOnlyForTheSamePageEngineSkinAndChoices() {
        val m = meta()
        assertTrue(LastLookRules.usable(m, "https://news.test/story", "160.0", "neon", false, false))
        assertTrue("the #fragment does not make it another page", LastLookRules.usable(m, "https://news.test/story#comments", "160.0", "neon", false, false))
        assertFalse(LastLookRules.usable(m, "https://news.test/other", "160.0", "neon", false, false))
        assertFalse("an update changed the engine", LastLookRules.usable(m, "https://news.test/story", "161.0", "neon", false, false))
        assertFalse("another skin", LastLookRules.usable(m, "https://news.test/story", "160.0", "plain", false, false))
        assertFalse("dark pages switched on", LastLookRules.usable(m, "https://news.test/story", "160.0", "neon", true, false))
        assertFalse("desktop site switched on", LastLookRules.usable(m, "https://news.test/story", "160.0", "neon", false, true))
    }

    @Test fun thePictureFitsTheWidthExactlyOrScaledOrNotAtAll() {
        assertEquals(LookFit.EXACT, LastLookRules.fit(600, 900, 600, 900))
        assertEquals("two pixels of difference are the same width", LastLookRules.fit(600, 900, 602, 850), LookFit.EXACT)
        assertEquals("another width with the same shape is scaled", LookFit.SCALE, LastLookRules.fit(600, 900, 1200, 1800))
        assertEquals("within five percent of the shape", LookFit.SCALE, LastLookRules.fit(600, 900, 1000, 1520))
        assertEquals("a landscape screen for a portrait picture: today's cover", LookFit.NONE, LastLookRules.fit(600, 900, 973, 600))
        assertEquals(LookFit.NONE, LastLookRules.fit(0, 900, 600, 900)); assertEquals(LookFit.NONE, LastLookRules.fit(600, 900, 0, 0))
    }

    @Test fun thePictureGoesWhenTheLivePageHasPaintedAndIsWhereItWas() {
        val h = LastLookRules::hideNow
        assertFalse("never before the first paint, whatever else is true", h(false, 0, 0, true, 5000))
        assertTrue("a page that was at the top", h(true, 0, 0, false, 0))
        assertFalse("painted but still at the top while the saved place is 640", h(true, 640, 0, false, 100))
        assertTrue("at the saved place", h(true, 640, 640, false, 100)); assertTrue(h(true, 640, 636, false, 100)); assertTrue(h(true, 640, 644, false, 100))
        assertFalse("five pixels off is not there yet", h(true, 640, 635, false, 100))
        assertTrue("the load ended", h(true, 640, 0, true, 100))
        assertTrue("1500 ms after the first paint at the latest", h(true, 640, 0, false, 1500)); assertFalse(h(true, 640, 0, false, 1499))
    }

    @Test fun thePictureOfTheFrontPageAndTheTwoMostRecentOthersStay() {
        val keep = LastLookRules.keep(7L, mapOf(1L to 100L, 2L to 500L, 3L to 300L, 7L to 50L, 9L to 400L))
        assertEquals(setOf(7L, 2L, 9L), keep)
        assertEquals(setOf(5L), LastLookRules.keep(5L, emptyMap()))
    }
}
