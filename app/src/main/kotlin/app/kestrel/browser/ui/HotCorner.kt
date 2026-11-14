package app.kestrel.browser.ui

import android.os.Handler
import android.view.MotionEvent

/**
 * The invisible long-press square in the top right corner of the page area (AU1, owner message 35: "make the top right slide down box ... have no visible button at all. Instead, if I long press the top
 * right corner of the browser (make it a decently large target square to long press, but invisible), it slides open the panel and if I long press above the open panel it slides it back closed").
 *
 * It only WATCHES: the activity shows it every touch event before the page gets it, and the page gets them all (a tap on a link, a scroll or a pinch that starts in the corner works as it always did).
 * Only when a finger has stayed down inside the square, nearly still, for [LONG_PRESS_MS] does it act: the host is told, the host tells the page its touch is over (so the page's own long press menu
 * never starts: ours is shorter than the engine's), and the rest of that touch is kept away from the page. The time is the touch's OWN clock as well as a timer: when the UI thread is busy and the
 * events reach us late (all at once), the timestamps of the move or the lift still say the finger was down long enough (found on the emulator: a late long press was taken for a tap and followed the link under it).
 *
 * The square is the host's to place ([Host.zone]): the panel sits right below it, so the same square is "above the open panel" and closes it again.
 */
class HotCorner(private val handler: Handler, private val slopPx: Float, private val host: Host, private val longPressMs: Long = LONG_PRESS_MS) {
    interface Host {
        /** The square in window pixels (left, top, right, bottom), or null when it must not act now (a video full screen, a menu over the page, the feature switched off). */
        fun zone(): IntArray?
        /** The long press happened: open or close the panel and tell the page its touch is over ([downTime], [x], [y] are the touch's). */
        fun fired(downTime: Long, x: Float, y: Float)
        /** True when the window point is on something that must not start the long press (the open panel, which can sit over the square once it has been moved). */
        fun covers(x: Float, y: Float): Boolean = false
    }

    private var armed = false
    private var swallow = false
    private var downX = 0f; private var downY = 0f; private var downTime = 0L
    private var lastX = 0f; private var lastY = 0f
    private val fire = Runnable { if (armed) { armed = false; swallow = true; host.fired(downTime, lastX, lastY) } }

    /** True while the rest of a touch that fired the long press is being kept from the page. */
    val swallowing: Boolean get() = swallow

    /**
     * Look at one touch event. Returns true when the event is OURS now (the long press already fired: the page must not see it), false when it goes on to the page as usual. Call it with every event, first.
     */
    fun observe(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                disarm(); swallow = false
                val z = host.zone()
                if (z != null && ev.x >= z[0] && ev.x < z[2] && ev.y >= z[1] && ev.y < z[3] && !host.covers(ev.x, ev.y)) {
                    armed = true; downX = ev.x; downY = ev.y; lastX = ev.x; lastY = ev.y; downTime = ev.downTime
                    handler.postDelayed(fire, longPressMs)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> disarm() // a second finger: a pinch, not a long press
            MotionEvent.ACTION_MOVE -> {
                lastX = ev.x; lastY = ev.y
                if (armed) {
                    if (Math.abs(ev.x - downX) > slopPx || Math.abs(ev.y - downY) > slopPx) disarm() // it moved: a scroll or a drag
                    else if (ev.eventTime - downTime >= longPressMs) fire.run()                        // the events were held up (a busy UI thread): the finger HAS been down long enough
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (armed && ev.actionMasked == MotionEvent.ACTION_UP && ev.eventTime - downTime >= longPressMs) { lastX = ev.x; lastY = ev.y; fire.run() } // same: the lift says how long the finger was down
                disarm()
                val was = swallow
                swallow = false
                return was
            }
        }
        return swallow
    }

    /** The touch is over or went elsewhere (the activity paused, a screen opened over the page). */
    fun reset() { disarm(); swallow = false }

    private fun disarm() { armed = false; handler.removeCallbacks(fire) }

    companion object {
        /** The square's side. */
        const val SIZE_DP = 72
        /** Shorter than the page's own long press (about 500 ms) so ours is first, longer than any tap. */
        const val LONG_PRESS_MS = 380L
    }
}
