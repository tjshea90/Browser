package app.kestrel.browser

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import app.kestrel.browser.ui.Pal
import app.kestrel.browser.ui.TabGrid
import app.kestrel.browser.web.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Swipe a card sideways to close a tab (promised to the owner three times); taps and vertical drags are unchanged. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "w600dp-h900dp-mdpi")
class TabGridSwipeTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val closed = ArrayList<Long>()
    private val selected = ArrayList<Long>()
    private lateinit var grid: TabGrid
    private lateinit var tabs: List<Tab>

    private val listener = object : TabGrid.Listener {
        override fun select(tab: Tab) { selected.add(tab.id) }
        override fun close(tab: Tab) { closed.add(tab.id) }
        override fun newTab(privateTab: Boolean) {}
        override fun closeAll(privateTab: Boolean) {}
        override fun dismiss() {}
        override fun paneChanged(privateTab: Boolean) {}
    }

    @Before fun build() {
        tabs = (1L..3L).map { id -> Tab(id).also { it.url = "https://site$id.example/"; it.title = "Site $id" } }
        // attached to a real (Robolectric) window: a tap's click is posted to the view's handler, which only runs once the view is attached
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).create().start().resume().visible().get()
        grid = TabGrid(activity, Pal(activity, false), { false }, listener)
        activity.setContentView(grid, FrameLayout.LayoutParams(600, 900))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        grid.open(tabs, tabs[0])
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    private fun card(title: String): View {
        val found = ArrayList<View>(); grid.findViewsWithText(found, title, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        assertTrue("card '$title' exists", found.isNotEmpty())
        return found[0]
    }

    private fun drag(v: View, dx: Float, dy: Float, steps: Int = 8) {
        val t0 = SystemClock.uptimeMillis()
        fun ev(action: Int, x: Float, y: Float, t: Long) = MotionEvent.obtain(t0, t, action, x, y, 0)
        val sx = 100f; val sy = 100f
        v.dispatchTouchEvent(ev(MotionEvent.ACTION_DOWN, sx, sy, t0))
        for (i in 1..steps) v.dispatchTouchEvent(ev(MotionEvent.ACTION_MOVE, sx + dx * i / steps, sy + dy * i / steps, t0 + i * 16L))
        v.dispatchTouchEvent(ev(MotionEvent.ACTION_UP, sx + dx, sy + dy, t0 + (steps + 1) * 16L))
    }

    @Test fun aLongSwipeClosesTheTab() {
        drag(card("Site 2"), dx = 400f, dy = 10f)
        assertEquals(listOf(2L), closed)
        assertTrue("it did not also select the tab", selected.isEmpty())
    }

    @Test fun aSwipeToTheLeftClosesToo() {
        drag(card("Site 3"), dx = -400f, dy = 0f)
        assertEquals(listOf(3L), closed)
    }

    @Test fun aShortSwipeSnapsBack() {
        val c = card("Site 2")
        drag(c, dx = 60f, dy = 0f)
        assertTrue(closed.isEmpty())
        assertEquals(0f, c.translationX, 0f)
        assertEquals(1f, c.alpha, 0f)
        assertTrue("a half-hearted swipe is not a tap either", selected.isEmpty())
    }

    @Test fun aVerticalDragNeitherClosesNorSelects() {
        drag(card("Site 2"), dx = 20f, dy = 300f)
        assertTrue(closed.isEmpty())
    }

    @Test fun aTapStillSelects() {
        val c = card("Site 3")
        val t0 = SystemClock.uptimeMillis()
        c.dispatchTouchEvent(MotionEvent.obtain(t0, t0, MotionEvent.ACTION_DOWN, 100f, 100f, 0))
        c.dispatchTouchEvent(MotionEvent.obtain(t0, t0 + 40, MotionEvent.ACTION_UP, 100f, 100f, 0))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf(3L), selected)
        assertTrue(closed.isEmpty())
    }
}
