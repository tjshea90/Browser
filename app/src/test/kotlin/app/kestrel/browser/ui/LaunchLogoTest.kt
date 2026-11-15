package app.kestrel.browser.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import app.kestrel.core.session.LaunchAnim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SP1, owner message 41: the app's own symbol glows once at the start, very quickly, and never takes a touch. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LaunchLogoTest {
    private lateinit var frame: FrameLayout
    private var now = 5000L
    private var pending: Runnable? = null
    private var done = 0
    private lateinit var logo: LaunchLogo

    @Before fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        frame = FrameLayout(activity)
        logo = LaunchLogo(activity, 1f) { v -> done++; frame.removeView(v) }
        logo.clock = { now }; logo.nextFrame = { pending = it }
        frame.addView(logo, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        frame.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(973, View.MeasureSpec.EXACTLY))
        frame.layout(0, 0, 600, 973)
    }

    private fun drawNow() { logo.draw(Canvas(Bitmap.createBitmap(600, 973, Bitmap.Config.ARGB_8888))) }

    @Test fun itNeverTakesATouchAndIsInvisibleToAccessibility() {
        assertFalse(logo.isClickable); assertFalse(logo.isFocusable)
        val t = android.os.SystemClock.uptimeMillis()
        assertFalse("the screen under it gets the touch", logo.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 300f, 400f, 0)))
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, logo.importantForAccessibility)
    }

    @Test fun theClockStartsAtTheFirstDrawNotWhenTheViewWasMade() {
        now = 5000L
        now += 3000L                                   // building the screen took 3 s: the flourish has not started yet
        drawNow()
        assertFalse("first frame: it just started", logo.isFinished); assertEquals(0, done)
        now += LaunchAnim.TOTAL_MS / 2; drawNow()
        assertFalse("half way", logo.isFinished); assertTrue("and it asks for the next frame", pending != null)
    }

    @Test fun itDrawsEveryMomentOfTheFlourishAndRemovesItselfWhenItIsOver() {
        drawNow()
        for (step in 1..19) { now += LaunchAnim.TOTAL_MS / 20; drawNow(); assertFalse("still playing at step $step", logo.isFinished) }
        now += LaunchAnim.TOTAL_MS; drawNow()
        assertTrue(logo.isFinished); assertEquals("told its owner exactly once", 1, done)
        assertNull("and removed", logo.parent)
        drawNow(); assertEquals("a view that is gone does not tell again", 1, done)
    }

    @Test fun aSlowFrameDoesNotMakeItLastLonger() {
        drawNow()
        now += 5000L                                   // the UI thread was busy for 5 s: the next frame sees the end of the flourish at once
        drawNow()
        assertTrue(logo.isFinished); assertEquals(1, done)
    }

    @Test fun theFadeDoesNotLeaveTheSameSymbolFadedWhereElseItIsUsed() {
        // a vector drawable shares its alpha with every drawable made from the same resource: the Home screen's brand mark is one (found on the emulator: it stayed dark after the flourish)
        val other = logo.context.getDrawable(app.kestrel.browser.R.drawable.ic_launcher_foreground)!!
        drawNow()
        now += (LaunchAnim.TOTAL_MS * 0.9f).toLong(); drawNow()                  // nearly faded out
        assertEquals("the other copy of the symbol is untouched", 255, other.alpha)
        now += LaunchAnim.TOTAL_MS; drawNow()
        assertEquals(255, other.alpha)
    }

    @Test fun aDebugStretchMakesItLastLongerAndNothingElseChanges() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var finished = 0
        val slow = LaunchLogo(activity, 10f) { finished++ }
        slow.clock = { now }; slow.nextFrame = { pending = it }
        val f = FrameLayout(activity); f.addView(slow, FrameLayout.LayoutParams(600, 973))
        f.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(973, View.MeasureSpec.EXACTLY)); f.layout(0, 0, 600, 973)
        val canvas = Canvas(Bitmap.createBitmap(600, 973, Bitmap.Config.ARGB_8888))
        slow.draw(canvas)
        now += LaunchAnim.TOTAL_MS * 5; slow.draw(canvas)
        assertFalse("half way through the stretched flourish", slow.isFinished)
        now += LaunchAnim.TOTAL_MS * 6; slow.draw(canvas)
        assertTrue(slow.isFinished); assertEquals(1, finished)
    }
}
