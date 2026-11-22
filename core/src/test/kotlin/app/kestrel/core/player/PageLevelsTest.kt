package app.kestrel.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** QL1: what the app takes from a page's own quality list. */
class PageLevelsTest {
    private fun ids(l: List<PageLevels.Level>) = l.map { it.id }

    @Test fun automaticComesFirstAndIsAlwaysCalledThat() {
        val l = PageLevels.clean(listOf("hd1080" to "1080p", "auto" to "whatever the page calls it", "hd720" to "720p"))
        assertEquals(listOf("auto", "hd1080", "hd720"), ids(l))
        assertEquals("Automatic", l[0].label)
    }

    @Test fun anOddIdAnEmptyLabelAndARepeatedIdAreDropped() {
        val l = PageLevels.clean(listOf("a b" to "x", "hd720" to "720p", "hd720" to "again", "q1" to "  ", "" to "none", "tiny" to "144p"))
        assertEquals(listOf("hd720", "tiny"), ids(l))
        assertEquals("720p", l[0].label)
    }

    @Test fun labelsAreCutAndCleanedOfMarkupAndLineBreaks() {
        val l = PageLevels.clean(listOf("a" to "<b>1080p</b>\n60 fps " + "x".repeat(60), "b" to "720p"))
        assertTrue(l[0].label, l[0].label.length <= PageLevels.MAX_LABEL)
        assertTrue(l[0].label, !l[0].label.contains('<') && !l[0].label.contains('\n'))
    }

    @Test fun anIdLongerThanTheLimitIsDroppedNotCut() {
        val l = PageLevels.clean(listOf("z".repeat(40) to "a", "ok" to "b", "ok2" to "c"))
        assertEquals(listOf("ok", "ok2"), ids(l))
    }

    @Test fun atMostTwelveRowsAndFewerThanTwoIsNoChoice() {
        assertEquals(12, PageLevels.clean((1..30).map { "q$it" to "${it}p" }).size)
        assertTrue(PageLevels.clean(listOf("auto" to "Automatic")).isEmpty())
        assertTrue(PageLevels.clean(listOf("hd720" to "720p")).isEmpty())
        assertTrue(PageLevels.clean(emptyList()).isEmpty())
    }

    @Test fun aPickCanOnlyNameARowOfTheHeldList() {
        val l = PageLevels.clean(listOf("auto" to "x", "hd720" to "720p"))
        assertEquals("hd720", PageLevels.valid("hd720", l)?.id)
        assertNull(PageLevels.valid("hd1080", l))
        assertNull(PageLevels.valid("\"};alert(1);//", l))
    }

    @Test fun theCheckMarkFollowsThePersonsPickElseThePagesOwnState() {
        val l = PageLevels.clean(listOf("auto" to "x", "hd1080" to "1080p", "hd720" to "720p"))
        assertEquals("auto", PageLevels.selectedId("auto", "hd720", l))
        assertEquals("hd720", PageLevels.selectedId(null, "hd720", l))
        assertNull(PageLevels.selectedId(null, "tiny", l))
    }
}
