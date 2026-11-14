package app.kestrel.browser.ui

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** The invisible long-press square in the top right corner (AU1): it only acts on a still, long press inside the square, and keeps the rest of that touch from the page. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class HotCornerTest {
    private class FakeHost : HotCorner.Host {
        var zone: IntArray? = intArrayOf(500, 0, 572, 72)
        val fired = ArrayList<FloatArray>()
        var covered = false
        override fun zone(): IntArray? = zone
        override fun covers(x: Float, y: Float): Boolean = covered
        override fun fired(downTime: Long, x: Float, y: Float) { fired.add(floatArrayOf(downTime.toFloat(), x, y)) }
    }

    private lateinit var host: FakeHost
    private lateinit var corner: HotCorner
    private var t = 1000L

    @Before fun setUp() {
        host = FakeHost()
        corner = HotCorner(Handler(Looper.getMainLooper()), 10f, host)
    }

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun ev(action: Int, x: Float, y: Float, downTime: Long = t): MotionEvent = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
    private fun down(x: Float, y: Float) = corner.observe(ev(MotionEvent.ACTION_DOWN, x, y))
    private fun move(x: Float, y: Float) = corner.observe(ev(MotionEvent.ACTION_MOVE, x, y))
    private fun up(x: Float, y: Float) = corner.observe(ev(MotionEvent.ACTION_UP, x, y))

    @Test fun aStillLongPressInsideTheSquareFiresOnceAndTheRestOfTheTouchIsKeptFromThePage() {
        assertFalse("the touch-down goes to the page as usual", down(540f, 30f))
        idle(HotCorner.LONG_PRESS_MS - 20); assertTrue("not yet", host.fired.isEmpty())
        idle(60)
        assertEquals(1, host.fired.size)
        assertEquals(540f, host.fired[0][1], 0.1f); assertEquals(30f, host.fired[0][2], 0.1f)
        assertTrue("the rest of the touch is ours", move(541f, 31f))
        idle(2000); assertEquals("only once per touch", 1, host.fired.size)
        assertTrue("the lift is ours too", up(541f, 31f))
        assertFalse("the next touch is the page's again", down(100f, 400f))
        assertFalse(up(100f, 400f))
    }

    private fun evAt(action: Int, x: Float, y: Float, eventTime: Long): MotionEvent = MotionEvent.obtain(t, eventTime, action, x, y, 0)

    @Test fun aLongPressWhoseEventsReachUsLateStillFiresFromTheTimestampOfTheLift() {
        // a busy UI thread: the whole touch arrives at once, nothing has idled, but the lift says the finger was down for 500 ms
        assertFalse(corner.observe(evAt(MotionEvent.ACTION_DOWN, 540f, 30f, t)))
        assertTrue("the lift (500 ms after the touch-down) is the long press and is ours", corner.observe(evAt(MotionEvent.ACTION_UP, 540f, 30f, t + 500)))
        assertEquals(1, host.fired.size)
        assertFalse("the next touch is the page's", down(100f, 400f))
    }

    @Test fun aLongPressWhoseMovesReachUsLateFiresFromTheTimestampOfTheMove() {
        corner.observe(evAt(MotionEvent.ACTION_DOWN, 540f, 30f, t))
        assertFalse("an early move: nothing yet", corner.observe(evAt(MotionEvent.ACTION_MOVE, 541f, 30f, t + 100)))
        assertTrue("a move 450 ms in fires and is ours", corner.observe(evAt(MotionEvent.ACTION_MOVE, 541f, 31f, t + 450)))
        assertEquals(1, host.fired.size)
        assertTrue(corner.observe(evAt(MotionEvent.ACTION_UP, 541f, 31f, t + 600)))
        assertEquals("once", 1, host.fired.size)
    }

    @Test fun aLateLiftOfAShortTouchIsStillATap() {
        corner.observe(evAt(MotionEvent.ACTION_DOWN, 540f, 30f, t))
        assertFalse("a lift 120 ms after the touch-down", corner.observe(evAt(MotionEvent.ACTION_UP, 540f, 30f, t + 120)))
        assertTrue(host.fired.isEmpty())
    }

    @Test fun aLongPressOnTheOpenPanelIsNotTheCornersEvenWhenThePanelSitsOverTheSquare() {
        host.covered = true
        assertFalse(down(540f, 30f)); idle(2000)
        assertTrue("nothing fired", host.fired.isEmpty())
        assertFalse(up(540f, 30f))
    }

    @Test fun aTapNeverFiresAndEveryEventGoesToThePage() {
        assertFalse(down(540f, 30f)); idle(120); assertFalse(up(540f, 30f)); idle(2000)
        assertTrue(host.fired.isEmpty())
    }

    @Test fun aDragOutOfTheSlopIsAScrollNotALongPress() {
        down(540f, 30f); idle(100)
        assertFalse(move(540f, 60f)) // 30 px: past the slop of 10
        idle(2000); assertTrue(host.fired.isEmpty())
        assertFalse(up(540f, 60f))
    }

    @Test fun aThumbThatWobblesInsideTheSlopStillCounts() {
        down(540f, 30f)
        for (i in 1..8) { idle(40); move(540f + (i % 3), 30f + (i % 4)) }
        idle(200)
        assertEquals(1, host.fired.size)
    }

    @Test fun aSecondFingerIsAPinchNotALongPress() {
        down(540f, 30f); idle(100)
        corner.observe(ev(MotionEvent.ACTION_POINTER_DOWN, 540f, 30f))
        idle(2000); assertTrue(host.fired.isEmpty())
    }

    @Test fun aTouchOutsideTheSquareNeverArms() {
        down(300f, 30f); idle(2000); assertTrue("left of the square", host.fired.isEmpty()); up(300f, 30f)
        down(540f, 120f); idle(2000); assertTrue("below the square", host.fired.isEmpty()); up(540f, 120f)
    }

    @Test fun whenTheHostSaysNoNothingArms() {
        host.zone = null // a video full screen, a menu over the page, the feature switched off
        down(540f, 30f); idle(2000)
        assertTrue(host.fired.isEmpty())
    }

    @Test fun aCancelledTouchLeavesNothingBehind() {
        down(540f, 30f); idle(100)
        assertFalse(corner.observe(ev(MotionEvent.ACTION_CANCEL, 540f, 30f)))
        idle(2000); assertTrue(host.fired.isEmpty())
        assertFalse("no touch is being kept from the page", corner.swallowing)
    }

    @Test fun theLongPressIsShorterThanThePagesOwn() {
        // the engine's own long press (the context menu) starts at about 500 ms: ours must come first
        assertTrue(HotCorner.LONG_PRESS_MS < 450L)
        assertTrue(HotCorner.LONG_PRESS_MS > 250L) // and longer than any tap
    }

    @Test fun theSquareIsDecentlyLargeForAThumb() {
        assertTrue(HotCorner.SIZE_DP >= 64)
    }
}
