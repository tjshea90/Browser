package app.kestrel.browser.web

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.PanZoomController
import app.kestrel.browser.diag.Health

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

    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var gesture = 0 // counts touch-downs: a late engine answer for an earlier gesture is ignored
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var topKnown = false // the engine has answered for this gesture's touch-down
    private var atTop = false
    private var pageHandles = false
    private var overscrollOk = true
    private var dead = true // this gesture is not a pull (decided at touch-down, or by what the finger did)
    private var pulling = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (session == null) return super.onTouchEvent(event)
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                gesture++
                downX = event.x; downY = event.y; lastX = event.x; lastY = event.y
                topKnown = false; atTop = false; pageHandles = false; overscrollOk = true; pulling = false
                dead = pull == null || !pullEnabled()
            }
            MotionEvent.ACTION_POINTER_DOWN -> { dead = true; if (pulling) endPull(false) } // a pinch or a second finger: not a pull
            MotionEvent.ACTION_MOVE -> if (!dead) { lastX = event.x; lastY = event.y; track() }
            MotionEvent.ACTION_UP -> { lastX = event.x; lastY = event.y; if (pulling) endPull(true); dead = true }
            MotionEvent.ACTION_CANCEL -> { if (pulling) endPull(false); dead = true }
        }
        if (action == MotionEvent.ACTION_DOWN && !dead) {
            val g = gesture
            super.onTouchEventForDetailResult(event).accept { d ->
                if (d == null || g != gesture) return@accept
                topKnown = true
                atTop = (d.scrollableDirections() and PanZoomController.SCROLLABLE_FLAG_TOP) == 0
                pageHandles = d.handledResult() == PanZoomController.INPUT_RESULT_HANDLED_CONTENT
                overscrollOk = (d.overscrollDirections() and PanZoomController.OVERSCROLL_FLAG_VERTICAL) != 0
                Health.log("pull", "down", "scrollable=${d.scrollableDirections()} overscroll=${d.overscrollDirections()} handled=${d.handledResult()} -> top=$atTop")
                if (!atTop || pageHandles || !overscrollOk) { dead = true; if (pulling) endPull(false) } else if (!dead) track()
            }
        } else {
            super.onTouchEventForDetailResult(event) // the engine gets every touch event, answer unused
        }
        return true
    }

    /** Decides whether the finger has started a pull, and reports its distance. Needs the engine's answer for the touch-down before it will start one. */
    private fun track() {
        val dy = lastY - downY
        val dx = Math.abs(lastX - downX)
        if (!pulling) {
            if (dy < -slop || dx > 2 * slop && dx > dy) { dead = true; return } // dragging up = scrolling the page down; mostly sideways = a swipe, not a pull
            if (dy > slop && topKnown) {
                if (!(atTop && !pageHandles && overscrollOk)) { dead = true; return }
                pulling = true
                pull?.pullStart()
            }
        }
        if (pulling) {
            if (dy < -slop) { endPull(false); dead = true; return }
            pull?.pullMove(maxOf(0f, dy - slop))
        }
    }

    private fun endPull(released: Boolean) {
        if (!pulling) return
        pulling = false
        pull?.pullEnd(released)
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
