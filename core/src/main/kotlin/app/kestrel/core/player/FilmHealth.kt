package app.kestrel.core.player

/**
 * ANS51e (owner message 51: the real tablet's Diagnostics could not say WHY a full-screen film was slow: "CPU by process" sees only the app's own process on Android 10, and "Real video" said "nothing played yet" for a film that
 * had played for a minute because the old video finder never sees a film inside a shadow root): what the full-screen player script can measure from INSIDE the page, as numbers.
 *  - `lt`: how late the script's own timer ran since its last report, in ms. A page whose script thread is busy (the tablet's Yahoo page was quiet for 3.5 to 14 s after a turn) runs its timers late by that much.
 *  - `pc`: what the script's own work cost in one report (fit, put away the page's controls, read the state), in ms. Where this is large, Kestrel itself is the load.
 *  - `tf` / `df`: the frames the film has shown / dropped so far (`getVideoPlaybackQuality()`), cumulative for its element.
 * Pure and testable; the controller ([app.kestrel.browser.web.PlayerController]) feeds it from `vst` reports, Diagnostics prints [line], and [takeLook] hands the frames to the "Real video" statistics in looks of at least
 * [LOOK_MS] (a look per report would flood that line's bounded history).
 */
class FilmHealth {
    /** Frames and time since the last look: what "Real video" adds up. */
    class Look(val frames: Int, val dropped: Int, val seconds: Int, val w: Int, val h: Int)

    var reports = 0; private set
    var stalls = 0; private set            // reports that came with a timer at least [STALL_MS] late
    var stallWorstMs = 0L; private set
    var stallLastMs = 0L; private set
    var ownWorstMs = 0L; private set
    private var ownSumMs = 0L
    var frames = 0L; private set
    var dropped = 0L; private set
    var pictureChanges = 0; private set    // the stream stepped to another picture size (the tablet's film went 1920x1080 -> 640x360)
    private var lastTf = -1; private var lastDf = 0
    private var lookFrames = 0; private var lookDropped = 0; private var lookMs = 0L; private var prevMs = -1L
    private var lastW = 0; private var lastH = 0

    /** A new full screen: the film's element and its counters are new (the old totals stay, they are the run's). */
    fun newSession() { lastTf = -1; lastDf = 0; lookFrames = 0; lookDropped = 0; lookMs = 0L; prevMs = -1L; lastW = 0; lastH = 0 }

    /** One report. [nowMs] is a monotonic clock. Out-of-range numbers are ignored by the caller's relay; here a counter that goes DOWN is a new element (its baseline is taken again). */
    fun onReport(lateMs: Int, ownMs: Int, totalFrames: Int, droppedFrames: Int, w: Int, h: Int, nowMs: Long) {
        reports++
        val dt = if (prevMs < 0L) 0L else (nowMs - prevMs).coerceIn(0L, MAX_STEP_MS); prevMs = nowMs   // (film time: what passed since the last report, a long silence counts for at most a step)
        if (lateMs >= STALL_MS) { stalls++; stallLastMs = lateMs.toLong(); if (lateMs > stallWorstMs) stallWorstMs = lateMs.toLong() }
        if (ownMs > 0) { ownSumMs += ownMs; if (ownMs > ownWorstMs) ownWorstMs = ownMs.toLong() }
        if (w > 0 && h > 0) {
            if (lastW > 0 && (w != lastW || h != lastH)) pictureChanges++
            lastW = w; lastH = h
        }
        if (totalFrames < 0 || droppedFrames < 0) return
        if (lastTf < 0 || totalFrames < lastTf || droppedFrames < lastDf) { lastTf = totalFrames; lastDf = droppedFrames; return }   // the first look at this element (or a new one): only a baseline
        val df = totalFrames - lastTf; val dd = droppedFrames - lastDf
        lastTf = totalFrames; lastDf = droppedFrames
        // a film cannot show more frames than a display can draw: a jump like that is another element (an ad element at 150 frames, then the film's element at 1800: the page swapped elements without telling us), only a new baseline
        if (df > MAX_FPS * maxOf(1L, dt / 1000L) + 60L) return
        if (df <= 0) return
        frames += df; dropped += dd.coerceAtMost(df)
        lookFrames += df; lookDropped += dd.coerceAtMost(df); lookMs += dt
    }

    /** The frames and seconds gathered since the last take, once at least [LOOK_MS] of film time have passed (or [force]: the full screen ended); null when there is nothing to say. */
    fun takeLook(force: Boolean): Look? {
        if (lookFrames <= 0) return null
        if (!force && lookMs < LOOK_MS) return null
        val l = Look(lookFrames, lookDropped, ((lookMs + 500L) / 1000L).toInt().coerceIn(1, 600), lastW, lastH)
        lookFrames = 0; lookDropped = 0; lookMs = 0L
        return l
    }

    /** The part of the Diagnostics line 'Full-screen video controls' that says how the page and the film did. */
    fun line(): String {
        if (reports == 0) return "page health: no report yet"
        val avg = ownSumMs / reports
        val pct = if (frames <= 0L) "-" else { val t = (dropped * 1000 + frames / 2) / frames; "${t / 10}.${t % 10} %" }
        return "page script stalls over ${STALL_MS / 1000} s: $stalls (longest $stallWorstMs ms, last $stallLastMs ms); the player script's own work per report: average $avg ms, worst $ownWorstMs ms; " +
            "the film: $frames frames shown, $dropped dropped ($pct), picture size changed $pictureChanges times"
    }

    companion object {
        /** A timer this late means the page's script thread was busy for about that long. */
        const val STALL_MS = 1000
        /** A look for the "Real video" statistics covers at least this much film time (the extension's own looks are 15 s apart). */
        const val LOOK_MS = 15_000L
        /** One report counts for at most this much film time (a stall of the page is not film time). */
        const val MAX_STEP_MS = 10_000L
        /** No film shows more frames per second than this: a bigger jump of the counter is a swapped element. */
        const val MAX_FPS = 120L
    }
}
