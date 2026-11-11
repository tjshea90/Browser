package app.kestrel.browser.web

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.mozilla.geckoview.GeckoSession
import java.util.Locale

/**
 * Diagnostics > "Scroll test": drives the bundled long page with 32 finger swipes (16 scrolling down, 16 back up) through the SAME touch path as a real finger (the
 * activity's decor view -> GeckoView -> APZ, fling included) and counts the frames the engine produced for them. A smooth device shows 50-60 frames a second with small
 * gaps; stutter shows as a low rate and long gaps. Two engine signals are recorded and both reported, so a number that does not move is visible as such:
 * the compositor-thread scroll callback and the main-thread scroll callback. Offline, about 25 s, the owner can run it before and after a change and compare.
 */
class ScrollTest(private val session: GeckoSession, private val touchTarget: View, private val memNow: () -> String, private val finished: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val comp = LongArray(MAX_FRAMES)
    @Volatile private var nComp = 0
    private val ui = LongArray(MAX_FRAMES)
    @Volatile private var nUi = 0
    private val stepStart = LongArray(STEPS + 1)
    private var step = 0
    private var memBefore = ""

    private val compositorDelegate = object : GeckoSession.CompositorScrollDelegate {
        override fun onScrollChanged(session: GeckoSession, update: GeckoSession.ScrollPositionUpdate) { // compositor thread
            val i = nComp
            if (i < MAX_FRAMES) { comp[i] = SystemClock.uptimeMillis(); nComp = i + 1 }
        }
    }
    private val uiDelegate = object : GeckoSession.ScrollDelegate {
        override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) { // UI thread
            val i = nUi
            if (i < MAX_FRAMES) { ui[i] = SystemClock.uptimeMillis(); nUi = i + 1 }
        }
    }

    fun start() {
        memBefore = memNow()
        session.compositorScrollDelegate = compositorDelegate
        session.scrollDelegate = uiDelegate
        session.panZoomController.scrollToTop()
        main.postDelayed({ nComp = 0; nUi = 0; next() }, 1200) // let the jump to the top settle, then count only the test's own frames
    }

    private fun next() {
        if (step >= STEPS) { stepStart[STEPS] = SystemClock.uptimeMillis(); main.postDelayed({ finish() }, 900); return }
        stepStart[step] = SystemClock.uptimeMillis()
        swipe(if (step < STEPS / 2) 1 else -1)
        step++
        main.postDelayed({ next() }, STEP_MS)
    }

    /** One finger swipe of about a quarter of the screen height in 112 ms: dir +1 = finger moves up (the page scrolls down). */
    private fun swipe(dir: Int) {
        val w = touchTarget.width.toFloat(); val h = touchTarget.height.toFloat()
        val x = w * 0.5f
        val y0 = h * (if (dir > 0) 0.62f else 0.38f)
        val y1 = h * (if (dir > 0) 0.38f else 0.62f)
        val t0 = SystemClock.uptimeMillis()
        fun send(action: Int, y: Float) {
            val ev = MotionEvent.obtain(t0, SystemClock.uptimeMillis(), action, x, y, 0)
            try { touchTarget.dispatchTouchEvent(ev) } finally { ev.recycle() }
        }
        send(MotionEvent.ACTION_DOWN, y0)
        val moves = 7
        for (i in 1..moves) main.postDelayed({ send(MotionEvent.ACTION_MOVE, y0 + (y1 - y0) * i / moves) }, 16L * i)
        main.postDelayed({ send(MotionEvent.ACTION_UP, y1) }, 16L * moves + 8)
    }

    private fun finish() {
        try { session.compositorScrollDelegate = null; session.scrollDelegate = null } catch (e: Exception) { /* ignore */ }
        val out = StringBuilder()
        out.append(String.format(Locale.US, "%d finger swipes through the real touch path (page scrolled down, then back up)\n", STEPS))
        out.append("Compositor frames: ").append(summary(comp, nComp)).append('\n')
        out.append("Main-thread updates: ").append(summary(ui, nUi)).append('\n')
        out.append("Memory before: ").append(memBefore).append("\nMemory after:  ").append(memNow())
        finished(out.toString())
    }

    /** "612 in 9.8 s = 62/s; gap median 16 ms, 95% 33 ms, worst 214 ms (7 over 50 ms); first frame after touch: median 21 ms, worst 130 ms; swipes with a gap over 100 ms: 2". */
    private fun summary(t: LongArray, total: Int): String {
        val gaps = ArrayList<Long>(); val firsts = ArrayList<Long>()
        var motionMs = 0L; var slow = 0; var idx = 0
        for (s in 0 until STEPS) {
            val a = stepStart[s]; val b = stepStart[s + 1]
            var first = -1L; var prev = -1L; var worst = 0L
            while (idx < total && t[idx] < b) {
                val x = t[idx]
                if (x >= a) {
                    if (first < 0) first = x
                    if (prev >= 0) { val g = x - prev; gaps.add(g); if (g > worst) worst = g }
                    prev = x
                }
                idx++
            }
            if (first >= 0) { firsts.add(first - a); motionMs += prev - first }
            if (worst > 100) slow++
        }
        if (total == 0 || gaps.isEmpty()) return "$total callbacks (no usable data)"
        gaps.sort(); firsts.sort()
        fun pct(l: List<Long>, p: Double): Long = if (l.isEmpty()) 0 else l[((l.size - 1) * p).toInt()]
        val fps = if (motionMs > 0) total * 1000.0 / motionMs else 0.0
        return String.format(Locale.US, "%d in %.1f s of motion = %.0f/s; gap median %d ms, 95%% %d ms, worst %d ms (%d over 50 ms); first frame after touch: median %d ms, worst %d ms; swipes with a gap over 100 ms: %d of %d",
            total, motionMs / 1000.0, fps, pct(gaps, 0.5), pct(gaps, 0.95), gaps.last(), gaps.count { it > 50 }, pct(firsts, 0.5), firsts.lastOrNull() ?: 0, slow, STEPS)
    }

    companion object {
        const val URL = "resource://android/assets/scrolltest/index.html"
        private const val STEPS = 32
        private const val STEP_MS = 800L
        private const val MAX_FRAMES = 8000
    }
}
