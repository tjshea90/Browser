package app.kestrel.browser.ui

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MT1 (owner 2026-11-18): switch, close and open tabs from the floating navigation panel (research/MT1_MINI_TABS.md). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MiniNavTabsTest {
    private class Host : MiniNav.Listener {
        var tabs = ArrayList<NavTab>()
        val calls = ArrayList<String>()
        override fun navBack() {}; override fun navHome() {}; override fun navForward() {}; override fun navSubmit(text: String) {}; override fun navOpenChanged(open: Boolean) {}
        override fun navTabs(): List<NavTab> = ArrayList(tabs)
        override fun navSwitchTab(id: Long) { calls.add("switch:$id"); tabs = ArrayList(tabs.map { NavTab(it.id, it.title, it.host, it.thumb, it.id == id, it.isPrivate) }) }
        override fun navCloseTab(id: Long) { calls.add("close:$id"); tabs = ArrayList(tabs.filter { it.id != id }) }
        override fun navNewTab() { calls.add("new"); tabs = ArrayList(tabs.map { NavTab(it.id, it.title, it.host, it.thumb, false, it.isPrivate) } + NavTab(99, "", "", null, true, false)) }
        val keyboard = ArrayList<Boolean>()
        override fun navKeyboardChanged(showing: Boolean) { keyboard.add(showing) }
    }

    private lateinit var activity: Activity
    private lateinit var frame: FrameLayout
    private lateinit var nav: MiniNav
    private val host = Host()

    private fun tab(id: Long, title: String, hostName: String, current: Boolean = false) = NavTab(id, title, hostName, null, current, false)

    @Before fun setUp() {
        host.tabs = arrayListOf(tab(1, "News of the day", "news.example"), tab(2, "Shop cart", "shop.example", current = true), tab(3, "", "video.example"))
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        frame = FrameLayout(activity); activity.setContentView(frame)
        nav = MiniNav(activity, Pal(activity), host)
        frame.addView(nav, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        relayout()
        nav.setOpen(true, animate = false); relayout()
    }

    private fun relayout() {
        frame.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
        frame.layout(0, 0, 600, 900)
    }

    /** A tap in the middle of row [i] (dp inside the tray); [closeMark] = on its x. */
    private fun tapRow(i: Int, closeMark: Boolean = false) { val w = nav.trayWidthDpForTest(); nav.trayTapForTest(if (closeMark) w - 12f else w / 3f, i * 40f + 20f) }
    private fun tapFooter(rows: Int) { nav.trayTapForTest(nav.trayWidthDpForTest() / 2f, rows * 40f + 20f) }

    @Test fun theTabsButtonOpensTheTrayWithOneRowPerTabAndTheTabInFrontIsMarked() {
        assertFalse(nav.trayShownForTest)
        nav.pressTabsForTest(); relayout()
        assertTrue(nav.trayOpen); assertTrue(nav.trayShownForTest)
        assertEquals(3, nav.trayRowsForTest().size); assertEquals("3", nav.tabsBadgeForTest)
        assertEquals(2L, nav.trayRowsForTest().first { it.current }.id)
        assertEquals("three rows and the New tab row", 4 * 40, activity.resources.displayMetrics.let { (nav.trayHeightForTest / it.density).toInt() })
        nav.pressTabsForTest(); assertFalse("the button again puts it away", nav.trayOpen)
    }

    @Test fun aTapOnARowSwitchesToThatTabAndPutsTheTrayAway() {
        nav.pressTabsForTest(); relayout()
        tapRow(0)
        assertEquals(listOf("switch:1"), host.calls)
        assertFalse(nav.trayOpen); assertFalse(nav.trayShownForTest)
    }

    @Test fun aTapOnTheXClosesThatTabAtOnceAndTheTrayStaysForTheNextOne() {
        nav.pressTabsForTest(); relayout()
        tapRow(0, closeMark = true)
        assertEquals(listOf("close:1"), host.calls)
        assertTrue(nav.trayOpen)
        assertEquals(listOf(2L, 3L), nav.trayRowsForTest().map { it.id }); assertEquals("2", nav.tabsBadgeForTest)
        relayout()
        assertEquals("the tray is shorter by a row", 3 * 40, (nav.trayHeightForTest / activity.resources.displayMetrics.density).toInt())
    }

    @Test fun newTabOpensATabAndThePanelsKeysSoTheAddressCanBeTypedAtOnce() {
        nav.pressTabsForTest(); relayout()
        tapFooter(3)
        assertEquals(listOf("new"), host.calls)
        assertFalse("the tray is away", nav.trayOpen)
        assertTrue("the panel's own keys are up on the new tab's address", nav.keyboardShowing)
        assertEquals(listOf(true), host.keyboard)
    }

    @Test fun theTrayAndTheKeysTakeTheSamePlaceSoOneClosesTheOther() {
        nav.pressTabsForTest(); relayout()
        nav.openKeyboard(); relayout()
        assertFalse("the keys closed the tray", nav.trayOpen); assertTrue(nav.keyboardShowing)
        nav.pressTabsForTest(); relayout()
        assertTrue(nav.trayOpen); assertFalse("the tray closed the keys", nav.keyboardShowing)
    }

    @Test fun backClosesTheTrayFirstAndClosingThePanelTakesTheTrayWithIt() {
        nav.pressTabsForTest()
        assertTrue(nav.onBackPressed()); assertFalse(nav.trayOpen)
        assertFalse("nothing left for Back to close", nav.onBackPressed())
        nav.pressTabsForTest(); nav.setOpen(false, animate = false)
        assertFalse(nav.trayOpen)
    }

    @Test fun whenTheHostsTabListChangesTheOpenTrayFollows() {
        nav.pressTabsForTest(); relayout()
        host.tabs.add(tab(4, "Mail", "mail.example"))
        nav.tabsChanged()
        assertEquals(4, nav.trayRowsForTest().size); assertEquals("4", nav.tabsBadgeForTest)
        host.tabs.removeAt(0); host.tabs.removeAt(0)
        nav.tabsChanged()
        assertEquals(listOf(3L, 4L), nav.trayRowsForTest().map { it.id })
    }

    @Test fun aLongListScrollsByDraggingAndATapStillHitsTheRowUnderTheFinger() {
        host.tabs = ArrayList((1L..9L).map { tab(it, "Tab $it", "site$it.example", current = it == 1L) })
        nav.pressTabsForTest(); relayout()
        val w = nav.trayWidthDpForTest()
        nav.trayTouchForTest(MotionEvent.ACTION_DOWN, w / 3f, 150f)
        nav.trayTouchForTest(MotionEvent.ACTION_MOVE, w / 3f, 90f)
        nav.trayTouchForTest(MotionEvent.ACTION_MOVE, w / 3f, 30f)
        nav.trayTouchForTest(MotionEvent.ACTION_UP, w / 3f, 30f)
        assertTrue("a drag is not a tap", host.calls.isEmpty())
        nav.trayTapForTest(w / 3f, 10f)                       // the top row in view is now a later one
        assertEquals(1, host.calls.size)
        assertTrue("a row further down than the first was switched to: ${host.calls}", host.calls[0] != "switch:1")
    }

    @Test fun thePanelGrewByTheTrayAndTheOutlineFollows() {
        val before = nav.outlineForTest()
        nav.pressTabsForTest(); relayout()
        val after = nav.outlineForTest()
        assertTrue("taller by the tray: ${before[3] - before[1]} -> ${after[3] - after[1]}", after[3] - after[1] > before[3] - before[1] + 3 * 40 * activity.resources.displayMetrics.density)
    }
}
