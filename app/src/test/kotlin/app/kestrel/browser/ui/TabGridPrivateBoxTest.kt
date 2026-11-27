package app.kestrel.browser.ui

import android.view.View
import app.kestrel.browser.web.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * PT1, owner message 41 (item 2): "when I close all private tabs, make the private tab box at the top of the tab selector disappear, because there are no more private tabs, then automatically default to the
 * regular "tabs" page."
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TabGridPrivateBoxTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val panes = ArrayList<Boolean>()
    private val privateIds = HashSet<Long>()

    private val listener = object : TabGrid.Listener {
        override fun select(tab: Tab) {}
        override fun close(tab: Tab) {}
        override fun newTab(privateTab: Boolean) {}
        override fun closeAll(privateTab: Boolean) {}
        override fun dismiss() {}
        override fun paneChanged(privateTab: Boolean) { panes.add(privateTab) }
    }

    private fun tab(id: Long, priv: Boolean): Tab { if (priv) privateIds.add(id); return Tab(id).apply { url = "https://example.com/$id"; title = "Page $id" } }

    private fun grid(): TabGrid {
        val g = TabGrid(ctx, Pal(ctx, false), { privateIds.contains(it.id) }, listener)
        g.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(973, View.MeasureSpec.EXACTLY))
        g.layout(0, 0, 600, 973)
        return g
    }

    @Test fun withNoPrivateTabsThereIsNoPrivateBoxAndTheRegularTabsShow() {
        val a = tab(1, false); val b = tab(2, false)
        val g = grid(); g.open(listOf(a, b), a)
        assertFalse("no private tab: no Private box", g.privateBoxShownForTest)
        assertFalse(g.showingPrivate)
    }

    @Test fun aPrivateTabBringsTheBoxBack() {
        val a = tab(1, false); val p = tab(2, true)
        val g = grid(); g.open(listOf(a, p), a)
        assertTrue(g.privateBoxShownForTest); assertFalse("the regular tab is in front: the regular list", g.showingPrivate)
    }

    @Test fun closingTheLastPrivateTabHidesTheBoxAndShowsTheRegularTabsAndTellsTheActivity() {
        val a = tab(1, false); val p = tab(2, true); val q = tab(3, true)
        val g = grid(); g.open(listOf(a, p, q), p)
        assertTrue(g.showingPrivate); assertTrue(g.privateBoxShownForTest)
        g.rebind(listOf(a, q), q)                                   // one private tab is still there: nothing changes
        assertTrue(g.showingPrivate); assertTrue(g.privateBoxShownForTest)
        g.rebind(listOf(a), a)                                      // the last private tab was closed (X, or Close all)
        assertFalse("the Private box is gone", g.privateBoxShownForTest)
        assertFalse("and the regular tabs page is the one shown", g.showingPrivate)
        assertEquals("the activity was told the private pane left (screenshots are allowed again)", listOf(true, false), panes)
    }

    @Test fun theBoxStaysGoneUntilAPrivateTabExistsAgainAndANewOneDoesNotYankTheGridToTheirPane() {
        val a = tab(1, false); val p = tab(2, true)
        val g = grid(); g.open(listOf(a, p), p)
        g.rebind(listOf(a), a)
        assertFalse(g.privateBoxShownForTest)
        val p2 = tab(3, true)
        g.rebind(listOf(a, p2), a)                                  // a private tab was opened from the menu while the grid was open
        assertTrue(g.privateBoxShownForTest); assertFalse("the regular list stays in front", g.showingPrivate)
    }
}
