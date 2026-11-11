package app.kestrel.core.gesture

/**
 * The rules of pull-down-to-refresh, with no Android types so they are unit-tested (PullTrackerTest). The page view feeds it the finger events and, once, the engine's answer for
 * the touch-down; it reports when a pull starts, how far the finger is below where the pull began, and how it ended.
 *
 * A gesture is a pull only if: it is [enabled] at touch-down; the engine says the page cannot scroll up, the page's own script is not handling the touch and overscroll is
 * allowed (the answer may arrive late - or never, then nothing happens); the finger then moves DOWN past [slop] and not mostly sideways; and no second finger joins. Dragging up
 * (that is scrolling the page) or back above the start by more than [slop] ends it.
 */
class PullTracker(private val slop: Float, private val listener: Listener) {
    interface Listener {
        /** The pull began (once per gesture). */
        fun start()
        /** The finger is [dy] below where the pull began (past the slop; never negative). */
        fun move(dy: Float)
        /** The gesture ended: [released] = the finger was lifted, false = cancelled or turned into something else. */
        fun end(released: Boolean)
    }

    private var dead = true
    private var pulling = false
    private var answered = false
    private var eligible = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f

    /** False once this gesture can no longer become a pull: the view need not ask the engine anything for it. */
    val alive: Boolean get() = !dead
    val isPulling: Boolean get() = pulling

    fun down(x: Float, y: Float, enabled: Boolean) {
        downX = x; downY = y; lastX = x; lastY = y
        answered = false; eligible = false; pulling = false
        dead = !enabled
    }

    /** The engine's answer for the touch-down: can the page scroll up, does its script handle the touch, may it overscroll vertically. */
    fun answer(canScrollUp: Boolean, pageHandlesTouch: Boolean, overscrollAllowed: Boolean) {
        if (dead) return
        answered = true
        eligible = !canScrollUp && !pageHandlesTouch && overscrollAllowed
        if (!eligible) { kill(); return }
        track()
    }

    fun move(x: Float, y: Float) {
        if (dead) return
        lastX = x; lastY = y
        track()
    }

    /** A second finger (pinch, two-finger scroll): not a pull. */
    fun pointerDown() = kill()

    fun up(x: Float, y: Float) {
        if (!dead) { lastX = x; lastY = y }
        if (pulling) finish(true)
        dead = true
    }

    fun cancel() { kill() }

    private fun kill() {
        if (pulling) finish(false)
        dead = true
    }

    private fun finish(released: Boolean) {
        pulling = false
        listener.end(released)
    }

    private fun track() {
        val dy = lastY - downY
        val dx = Math.abs(lastX - downX)
        if (!pulling) {
            if (dy < -slop || (dx > 2 * slop && dx > dy)) { dead = true; return } // dragging up = scrolling the page down; mostly sideways = a swipe
            if (dy > slop && answered) {
                pulling = true
                listener.start()
            }
        }
        if (pulling) {
            if (dy < -slop) { kill(); return }
            listener.move(maxOf(0f, dy - slop))
        }
    }
}
