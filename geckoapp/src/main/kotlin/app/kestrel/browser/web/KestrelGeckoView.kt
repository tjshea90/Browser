package app.kestrel.browser.web

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.PanZoomController
import app.kestrel.browser.diag.Health
import app.kestrel.core.gesture.PullTracker

/**
 * The one GeckoView of the activity, with timeline hooks: every surface-related event lands in the Diagnostics log, so a "the screen stopped updating"
 * report shows whether the page view was detached, resized or given a new surface around that moment (research/FREEZE.md 5.1).
 *
 * It also watches the finger for pull-down-to-refresh WITHOUT taking the gesture from the page: every touch still goes to the engine's pan/zoom controller (through
 * onTouchEventForDetailResult, which sends the event exactly like onTouchEvent and also reports what the page does with it). When a touch starts on a page that
 * cannot scroll up any further, is not handled by the page's own script, and allows overscroll, and the finger then drags DOWN, [pull] is told how far.
 */
class KestrelGeckoView(ctx: Context) : GeckoView(ctx) {
    interface PullListener {
        /** The pull began (called once per gesture). */
        fun pullStart()
        /** The finger is [dyPx] below where the pull began (never negative). */
        fun pullMove(dyPx: Float)
        /** The gesture ended: [released] = the finger was lifted (false = cancelled or turned into something else: a second finger, an upward drag). */
        fun pullEnd(released: Boolean)
    }

    var pull: PullListener? = null
    /** Asked at every touch-down: false = this gesture is never a pull (Home shown, error page, a test running, the setting is off). */
    var pullEnabled: () -> Boolean = { false }

    private var gesture = 0 // counts touch-downs: a late engine answer for an earlier gesture is ignored
    private val tracker = PullTracker(ViewConfiguration.get(ctx).scaledTouchSlop.toFloat(), object : PullTracker.Listener {
        override fun start() { pull?.pullStart() }
        override fun move(dy: Float) { pull?.pullMove(dy) }
        override fun end(released: Boolean) { pull?.pullEnd(released) }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (session == null) return super.onTouchEvent(event)
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> { gesture++; tracker.down(event.x, event.y, pull != null && pullEnabled()) }
            MotionEvent.ACTION_POINTER_DOWN -> tracker.pointerDown()
            MotionEvent.ACTION_MOVE -> tracker.move(event.x, event.y)
            MotionEvent.ACTION_UP -> tracker.up(event.x, event.y)
            MotionEvent.ACTION_CANCEL -> tracker.cancel()
        }
        // The engine gets every touch event either way; only the touch-DOWN of a possible pull needs its answer (what the page does with it), so only that one asks for it. The detail variant
        // allocates a result and crosses into native for every event of a drag (60-120 a second); the plain one does not.
        if (action != MotionEvent.ACTION_DOWN || !tracker.alive) { super.onTouchEvent(event); return true }
        val result = super.onTouchEventForDetailResult(event)
        run {
            val g = gesture
            result.accept { d ->
                if (d == null || g != gesture) return@accept
                val up = (d.scrollableDirections() and PanZoomController.SCROLLABLE_FLAG_TOP) != 0
                Health.log("pull", "down", "scrollable=${d.scrollableDirections()} overscroll=${d.overscrollDirections()} handled=${d.handledResult()} -> canScrollUp=$up")
                tracker.answer(up, d.handledResult() == PanZoomController.INPUT_RESULT_HANDLED_CONTENT, (d.overscrollDirections() and PanZoomController.OVERSCROLL_FLAG_VERTICAL) != 0)
            }
        }
        return true
    }

    override fun requestNewSurface() {
        Health.log("gecko", "requestNewSurface"); Health.count("gecko.newSurface")
        super.requestNewSurface()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Health.log("gecko", "view-attached")
    }

    override fun onDetachedFromWindow() {
        Health.log("gecko", "view-detached"); Health.count("gecko.detached")
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Health.log("gecko", "size", "${w}x$h (was ${oldw}x$oldh)")
    }
}
