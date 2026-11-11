package app.kestrel.browser.web

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.PanZoomController
import org.mozilla.geckoview.ScreenLength
import java.util.Locale

/**
 * Diagnostics > "Scroll test": scrolls the bundled long page with 40 smooth scrolls (20 down, 20 back up) and counts the frames the compositor really produced for them
 * ([GeckoSession.CompositorScrollDelegate] is called once per composited frame whose scroll offset changed). A smooth device shows about 50-60 frames a second with
 * small gaps; stutter shows as long gaps and a low rate. Offline, no network, a few seconds: the owner can run it before and after a change and compare numbers.
 */
class ScrollTest(private val session: GeckoSession, private val memNow: () -> String, private val finished: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val frames = LongArray(MAX_FRAMES)
    @Volatile private var n = 0
    private val stepStart = LongArray(STEPS + 1)
    private var step = 0
    private var memBefore = ""
    private var startedAt = 0L

    private val delegate = object : GeckoSession.CompositorScrollDelegate {
        override fun onScrollChanged(session: GeckoSession, update: GeckoSession.ScrollPositionUpdate) { // compositor thread
            val i = n
            if (i < MAX_FRAMES) { frames[i] = SystemClock.uptimeMillis(); n = i + 1 }
        }
    }

    fun start() {
        memBefore = memNow()
        startedAt = SystemClock.uptimeMillis()
        session.compositorScrollDelegate = delegate
        session.panZoomController.scrollToTop()
        main.postDelayed({ n = 0; next() }, 900) // let the jump to the top settle, then count only the test's own frames
    }

    private fun next() {
        if (step >= STEPS) { stepStart[STEPS] = SystemClock.uptimeMillis(); main.postDelayed({ finish() }, 700); return }
        stepStart[step] = SystemClock.uptimeMillis()
        val dy = if (step < STEPS / 2) STEP_PX else -STEP_PX
        try { session.panZoomController.scrollBy(ScreenLength.zero(), ScreenLength.fromPixels(dy), PanZoomController.SCROLL_BEHAVIOR_SMOOTH) } catch (e: Exception) { /* session gone: the result will show 0 frames */ }
        step++
        main.postDelayed({ next() }, STEP_MS)
    }

    private fun finish() {
        try { session.compositorScrollDelegate = null } catch (e: Exception) { /* ignore */ }
        val total = n
        val gaps = ArrayList<Long>()
        val firsts = ArrayList<Long>()
        var motionMs = 0L
        var slowSteps = 0
        var idx = 0
        for (s in 0 until STEPS) {
            val a = stepStart[s]; val b = stepStart[s + 1]
            var first = -1L; var prev = -1L; var worst = 0L
            while (idx < total && frames[idx] < b) {
                val t = frames[idx]
                if (t >= a) {
                    if (first < 0) first = t
                    if (prev >= 0) { val g = t - prev; gaps.add(g); if (g > worst) worst = g }
                    prev = t
                }
                idx++
            }
            if (first >= 0) { firsts.add(first - a); motionMs += prev - first }
            if (worst > 100) slowSteps++
        }
        gaps.sort(); firsts.sort()
        fun pct(l: List<Long>, p: Double): Long = if (l.isEmpty()) 0 else l[((l.size - 1) * p).toInt()]
        val fps = if (motionMs > 0) total * 1000.0 / motionMs else 0.0
        val over50 = gaps.count { it > 50 }
        val out = StringBuilder()
        out.append(String.format(Locale.US, "%d smooth scrolls of %d px, %d frames in %.1f s of motion = %.0f frames/s\n", STEPS, STEP_PX.toInt(), total, motionMs / 1000.0, fps))
        out.append(String.format(Locale.US, "Gap between frames: median %d ms, 95%% %d ms, worst %d ms; gaps over 50 ms: %d (scrolls with a gap over 100 ms: %d of %d)\n", pct(gaps, 0.5), pct(gaps, 0.95), gaps.lastOrNull() ?: 0, over50, slowSteps, STEPS))
        out.append(String.format(Locale.US, "From the scroll command to the first frame: median %d ms, worst %d ms\n", pct(firsts, 0.5), firsts.lastOrNull() ?: 0))
        out.append("Memory before: ").append(memBefore).append("\nMemory after:  ").append(memNow())
        finished(out.toString())
    }

    companion object {
        const val URL = "resource://android/assets/scrolltest/index.html"
        private const val STEPS = 40
        private const val STEP_PX = 450.0
        private const val STEP_MS = 520L
        private const val MAX_FRAMES = 6000
    }
}
