package app.kestrel.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabTrayModelTest {
    private val w = 160f
    private fun idx(h: TabTrayModel.Hit): String = when (h) { is TabTrayModel.Hit.Row -> "row${h.index}"; is TabTrayModel.Hit.Close -> "close${h.index}"; TabTrayModel.Hit.New -> "new"; TabTrayModel.Hit.None -> "none" }

    @Test fun theTrayIsAsTallAsItsRowsPlusTheFooterAndShowsAtMostFive() {
        val m = TabTrayModel()
        m.setCount(0); assertEquals(40f, m.height, 0f)
        m.setCount(3); assertEquals(160f, m.height, 0f)
        m.setCount(5); assertEquals(240f, m.height, 0f)
        m.setCount(12); assertEquals(240f, m.height, 0f); assertEquals(5, m.visibleRows); assertEquals(7 * 40f, m.maxScroll, 0f)
    }

    @Test fun aPressOnTheRightEdgeClosesTheRowAndAnywhereElseSwitchesAndTheFooterOpensANewTab() {
        val m = TabTrayModel(); m.setCount(3)
        assertEquals("row0", idx(m.hit(20f, 10f, w)))
        assertEquals("row1", idx(m.hit(100f, 50f, w)))
        assertEquals("close1", idx(m.hit(w - 10f, 50f, w)))
        assertEquals("close2", idx(m.hit(w - 1f, 119f, w)))
        assertEquals("new", idx(m.hit(30f, 130f, w)))
        assertEquals("new", idx(m.hit(w - 5f, 150f, w)))      // the footer has no close mark
        assertEquals("none", idx(m.hit(-1f, 10f, w))); assertEquals("none", idx(m.hit(10f, 170f, w)))
    }

    @Test fun draggingTheListMovesItWithinItsEndsAndAHitFollowsTheScroll() {
        val m = TabTrayModel(); m.setCount(9)
        assertFalse("already at the top", m.scrollBy(30f))
        assertTrue(m.scrollBy(-90f)); assertEquals(90f, m.scroll, 0f)
        assertEquals("row2", idx(m.hit(20f, 0f, w)))           // 90 px scrolled: the top row in view is row 2 (80..120)
        assertTrue(m.scrollBy(-1000f)); assertEquals(m.maxScroll, m.scroll, 0f); assertEquals(160f, m.maxScroll, 0f)
        assertEquals("row8", idx(m.hit(20f, 199f, w)))
        assertTrue(m.scrollBy(1000f)); assertEquals(0f, m.scroll, 0f)
    }

    @Test fun theTabInFrontIsScrolledIntoViewAndAShrinkingListClampsTheScroll() {
        val m = TabTrayModel(); m.setCount(9)
        m.ensureVisible(8); assertEquals(160f, m.scroll, 0f); assertEquals(0f, m.rowTop(4), 0f)
        m.ensureVisible(0); assertEquals(0f, m.scroll, 0f)
        m.ensureVisible(6); assertEquals(80f, m.scroll, 0f)
        m.setCount(6); assertTrue("one row more than fits: the scroll cannot pass the end", m.scroll <= m.maxScroll); assertEquals(40f, m.maxScroll, 0f)
        m.setCount(2); assertEquals(0f, m.scroll, 0f)
        m.ensureVisible(99)                                      // not a row: nothing happens
        assertEquals(0f, m.scroll, 0f)
    }
}
