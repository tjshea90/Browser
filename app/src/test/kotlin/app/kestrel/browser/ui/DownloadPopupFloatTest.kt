package app.kestrel.browser.ui

import android.app.Activity
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** FD1 (owner: "make the download manager float and be resizeable and moved around the screen just like the keyboard"): the small downloads window floats, moves, resizes, is remembered and comes back to its place. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DownloadPopupFloatTest {
    private class L(var stored: IntArray? = null) : DownloadPopup.Listener {
        var saves = 0
        override fun openManager() {}
        override fun confirm(title: String, message: String, yes: String, then: () -> Unit) {}
        override fun geometry(): IntArray? = stored
        override fun geometryChanged(g: IntArray?) { stored = g; saves++ }
    }

    private val W = 600; private val H = 900

    private fun open(l: L, w: Int = W, h: Int = H): Pair<DownloadPopup, Activity> {
        val a = Robolectric.buildActivity(Activity::class.java).setup().get()
        val p = DownloadPopup(a, Pal(a), l)
        val root = FrameLayout(a)
        a.setContentView(root)
        root.addView(p.view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        p.showForTest()
        p.view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        p.view.layout(0, 0, w, h)
        shadowOf(Looper.getMainLooper()).idle()
        p.view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        p.view.layout(0, 0, w, h)
        return p to a
    }

    private fun ev(action: Int, x: Float, y: Float, down: Long, now: Long) = MotionEvent.obtain(down, now, action, x, y, 0)
    /** A finger on [target] (in the popup view's coordinates): down, a pause of [holdMs], moves of ([dx], [dy]) in four steps, up. */
    private fun finger(p: DownloadPopup, x: Float, y: Float, dx: Float, dy: Float, holdMs: Long = 0) {
        val t0 = SystemClock.uptimeMillis()
        p.view.dispatchTouchEvent(ev(MotionEvent.ACTION_DOWN, x, y, t0, t0))
        if (holdMs > 0) shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(holdMs))
        for (i in 1..4) p.view.dispatchTouchEvent(ev(MotionEvent.ACTION_MOVE, x + dx * i / 4f, y + dy * i / 4f, t0, t0 + 20L * i))
        p.view.dispatchTouchEvent(ev(MotionEvent.ACTION_UP, x + dx, y + dy, t0, t0 + 120L))
    }

    private fun margins(p: DownloadPopup): IntArray { val lp = p.boxForTest().layoutParams as FrameLayout.LayoutParams; return intArrayOf(lp.rightMargin, lp.topMargin, lp.width) }

    @Test fun theFirstPlaceIsTheTopRightCornerAtTheFirstSize() {
        val (p, _) = open(L())
        val m = margins(p)
        assertEquals(8, m[0]); assertEquals(8, m[1]); assertEquals(300, m[2])
    }

    @Test fun aStoredPlaceAndSizeAreUsedAndKeptInsideASmallScreenWithoutChangingWhatWasStored() {
        val l = L(intArrayOf(300, 100, 260, 200))
        val (p, _) = open(l)
        val m = margins(p)
        assertEquals(300, m[0]); assertEquals(100, m[1]); assertEquals(260, m[2])
        val (q, _) = open(L(intArrayOf(300, 100, 260, 200)), 360, 500)
        assertTrue("kept inside 360 dp: right ${margins(q)[0]}", margins(q)[0] <= 360 - 260)
        assertEquals("the stored numbers stay", 300, q.geoForTest().right.toInt())
    }

    @Test fun aFingerOnTheTitleBarCarriesTheWindowAndTheNewPlaceIsRemembered() {
        val l = L()
        val (p, _) = open(l)
        val box = p.boxForTest()
        val x = (box.left + 60).toFloat(); val y = (box.top + 14).toFloat()
        System.err.println("DBG box=${box.left},${box.top},${box.right},${box.bottom} view=${p.view.width}x${p.view.height} vis=${box.visibility}/${p.view.visibility} dens=${p.view.resources.displayMetrics.density} hb=${p.headerBottomForTest()}")
        finger(p, x, y, dx = -120f, dy = 150f)
        val g = p.geoForTest()
        assertEquals(128f, g.right, 1f); assertEquals(158f, g.top, 1f)
        assertNotNull(l.stored); assertEquals(128, l.stored!![0]); assertEquals(158, l.stored!![1])
    }

    @Test fun aLongPressOnTheBodyPicksTheWindowUpAndTheFingerCarriesIt() {
        val l = L()
        val (p, _) = open(l)
        val box = p.boxForTest()
        val x = (box.left + 80).toFloat(); val y = (box.bottom - 40).toFloat()
        finger(p, x, y, dx = 0f, dy = 60f, holdMs = 500)
        assertEquals(68f, p.geoForTest().top, 1f)
        assertTrue(l.saves >= 1)
    }

    @Test fun aMovedDownTouchBeforeTheLongPressTimeIsAScrollOfTheListNotACarry() {
        val l = L()
        val (p, _) = open(l)
        val box = p.boxForTest()
        finger(p, (box.left + 80).toFloat(), (box.bottom - 40).toFloat(), dx = 0f, dy = 60f, holdMs = 0)
        assertEquals(8f, p.geoForTest().top, 0.1f)
        assertNull(l.stored)
    }

    @Test fun theCornerAtTheBottomLeftResizesAndTheSizeIsRemembered() {
        val l = L()
        val (p, _) = open(l)
        val box = p.boxForTest(); val h = p.handleForTest()
        val x = (box.left + h.left + 10).toFloat(); val y = (box.top + h.top + 10).toFloat()
        finger(p, x, y, dx = -60f, dy = 80f)
        val g = p.geoForTest()
        assertEquals(360f, g.w, 1f); assertEquals(350f, g.h, 1f)
        assertEquals(8f, g.right, 0.5f)
        assertNotNull(l.stored); assertEquals(360, l.stored!![2])
    }

    @Test fun aDoubleTapOnTheCornerPutsTheWindowBack() {
        val l = L(intArrayOf(120, 200, 340, 300))
        val (p, _) = open(l)
        val box = p.boxForTest(); val h = p.handleForTest()
        val x = (box.left + h.left + 10).toFloat(); val y = (box.top + h.top + 10).toFloat()
        finger(p, x, y, 0f, 0f); finger(p, x, y, 0f, 0f)
        assertNull("nothing is stored for the first place", l.stored)
        assertEquals(8f, p.geoForTest().right, 0.5f); assertEquals(300f, p.geoForTest().w, 0.5f)
    }

    @Test fun resetGeometryIsWhatSettingsCalls() {
        val l = L(intArrayOf(120, 200, 340, 300))
        val (p, _) = open(l)
        p.resetGeometry()
        assertNull(l.stored); assertEquals(8, margins(p)[0]); assertEquals(300, margins(p)[2])
    }
}
