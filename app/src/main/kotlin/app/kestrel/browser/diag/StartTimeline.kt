package app.kestrel.browser.diag

import android.os.Process
import android.os.SystemClock

/**
 * The start-up timeline (research/REVIEW-EXTERNAL-2.md, EY-2): where the time goes between "the owner tapped the icon" and "the page is on screen". Everything that tries to make the last page appear
 * sooner (the saved view, a lighter first frame, an earlier engine start) is judged by these marks, not by feel.
 *
 * A COLD start (a new process) is measured in milliseconds since the process started; a WARM start (the process was still alive, a new activity was created) in milliseconds since that activity
 * was created. Each mark is taken once per run. Marks: app (Application.onCreate), activity (Activity.onCreate), frame (the first frame after it), look (a saved picture of the page was drawn),
 * runtime (the engine is up), open (the page's session was opened), paint (the page's first paint), stop (its load ended). The line is in the Diagnostics report (Copy), and every mark is a
 * `start` event in its list. Memory only; nothing leaves the tablet.
 */
object StartTimeline {
    private val NAMES = listOf("app", "activity", "frame", "look", "runtime", "open", "paint", "stop")

    private class Run(val cold: Boolean, val base: Long) { val at = LongArray(NAMES.size) { -1L } }

    @Volatile private var appAt = -1L          // ms since process start when Application.onCreate ran (the cold run's first mark)
    private var current: Run? = null
    private var lastCold: Run? = null
    private var lastWarm: Run? = null
    private var activities = 0

    /** Application.onCreate of the UI process. */
    @Synchronized fun appCreated() { if (appAt < 0) appAt = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime() }

    /** A new activity was created: the first one of the process is the cold start, later ones are warm. */
    @Synchronized fun activityCreated() {
        val cold = activities++ == 0
        val now = SystemClock.elapsedRealtime()
        val r = Run(cold, if (cold) Process.getStartElapsedRealtime() else now)
        if (cold && appAt >= 0) r.at[0] = appAt
        current = r
        if (cold) lastCold = r else lastWarm = r
        mark("activity")
    }

    /** Take mark [name] for the run in progress (the first time only). Cheap: one array write and one event. */
    @Synchronized fun mark(name: String) {
        val r = current ?: return
        val i = NAMES.indexOf(name)
        if (i < 0 || r.at[i] >= 0) return
        val ms = SystemClock.elapsedRealtime() - r.base
        r.at[i] = ms
        Health.log("start", name, (if (r.cold) "cold " else "warm ") + ms + " ms")
    }

    private fun line(r: Run, title: String): String =
        title + ": " + NAMES.indices.joinToString(" | ") { NAMES[it] + " " + (if (r.at[it] >= 0) r.at[it].toString() else "-") }

    /** The Diagnostics lines: the cold start (ms since the process started) and the latest warm start (ms since the activity was created). */
    @Synchronized fun lines(): List<String> {
        val out = ArrayList<String>(2)
        out.add(lastCold?.let { line(it, "Cold start (ms since the process started)") } ?: "Cold start: (this process was not started by the browser's own activity)")
        lastWarm?.let { out.add(line(it, "Latest warm start (ms since the activity was created)")) }
        return out
    }
}
