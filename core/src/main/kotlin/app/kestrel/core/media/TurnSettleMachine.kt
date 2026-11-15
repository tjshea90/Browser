package app.kestrel.core.media

/**
 * ROT1 (owner message 46): the clock and the state of "the screen turns under a full-screen page or a playing video", pure and testable. The host (the app: `web/TurnSettler`) gives the facts of the moment
 * and carries out the actions (pause / play through the engine's media session, a black cover over the page, a timer); this class decides WHEN, by the rules of [TurnSettle].
 *
 * One turn, in order: [configChanged] (the screen turned: cover the page, pause the video if the app can) -> [sizeChanged] while the page area is still being laid out -> [tick] every [TurnSettle.TICK_MS]
 * until the area has been still for [TurnSettle.SETTLE_MS] (or [TurnSettle.CAP_MS] since the start) -> the cover fades out and the video plays again only if [TurnSettle.resumeIt] says so. A second turn
 * while one is going extends it (the video is paused once). [touchedScreen] = the person takes over: nothing is played for them. [abandon] = the app left the screen: everything goes, nothing is played.
 */
class TurnSettleMachine<T : Any>(private val h: Host<T>) {
    interface Host<T : Any> {
        /** A monotonic clock in milliseconds. */
        fun now(): Long
        /** Call [TurnSettleMachine.tick] after this long (an earlier request is replaced). */
        fun schedule(delayMs: Long)
        /** Forget the request made with [schedule]. */
        fun cancelSchedule()
        /** The black cover over the page area: shown at once, or let go (faded out when [fade]). */
        fun cover(show: Boolean, fade: Boolean)
        /** A page is full screen now. */
        fun fullscreen(): Boolean
        /** The video of the tab in front plays (as the engine's media session reported it). */
        fun frontPlaying(): Boolean
        /** The app holds the engine's media session of the tab in front (it exists only for audible media). */
        fun frontHasSession(): Boolean
        /** Pause the video of the tab in front through its session; returns a handle for that tab, or null when it could not be asked. */
        fun pauseFront(): T?
        /** The video of [handle] plays now. */
        fun playingNow(handle: T): Boolean
        /** [handle] is the tab in front. */
        fun isFront(handle: T): Boolean
        /** The app is on screen. */
        fun onScreen(): Boolean
        /** Play the video of [handle] again; true = asked. */
        fun resume(handle: T): Boolean
        /** One line for the event log (never a page address). */
        fun log(event: String, text: String) {}
    }

    private var wasLandscape: Boolean? = null
    private var active = false
    private var startedAt = 0L
    private var lastChangeAt = 0L
    private var paused: T? = null
    private var touched = false

    // what Diagnostics says
    var turns = 0; private set
    var pausedCount = 0; private set
    var resumedCount = 0; private set
    var skippedResume = 0; private set
    var lastMs = 0L; private set
    var last = "no turn under a video yet"; private set

    /** A turn is going on (the cover is up). */
    val turning: Boolean get() = active

    /** Where the screen is at the start: the first turn counts too. */
    fun init(landscape: Boolean) { wasLandscape = landscape }

    /** The activity's configuration changed; [landscape] is the orientation now. Called BEFORE the engine is told. */
    fun configChanged(landscape: Boolean) {
        val before = wasLandscape
        wasLandscape = landscape
        if (before == null) return // nobody told where the screen was: this configuration only says where it is now
        val turn = TurnSettle.isTurn(before, landscape)
        if (!turn) return                                                  // (a keyboard, a locale or a dark-mode change that reached the activity)
        val playing = h.frontPlaying()
        // a turn that goes on (the video was paused by us a moment ago, so it no longer "plays") is extended by the next one whatever the page shows
        if (!active && !TurnSettle.worthIt(turn, h.fullscreen(), playing)) return
        turns++
        val t = h.now()
        if (!active) { active = true; startedAt = t; touched = false }
        lastChangeAt = t
        h.cover(true, false)
        if (paused == null) {
            if (TurnSettle.pauseIt(playing, h.frontHasSession())) {
                val handle = h.pauseFront()
                if (handle != null) { paused = handle; pausedCount++; h.log("turn-pause", "the video is paused while the screen turns (${if (landscape) "landscape" else "portrait"})") }
            } else if (playing) h.log("turn-no-session", "the video plays but the engine has no media session to pause (a silent video)")
        }
        h.schedule(TurnSettle.TICK_MS)
    }

    /** The page area changed size: the turn is not over while this keeps coming. */
    fun sizeChanged() { if (active) lastChangeAt = h.now() }

    /** A finger came down: the person takes over. */
    fun touchedScreen() { if (active) touched = true }

    /** The app left the screen: everything goes at once and nothing is played. */
    fun abandon() {
        if (!active && paused == null) return
        h.cancelSchedule()
        active = false
        paused = null
        h.cover(false, false)
        last = "the app was left during a turn: nothing was played"
        h.log("turn-abandoned", "left the screen during a turn")
    }

    /** The timer the host was asked for ([Host.schedule]) came. */
    fun tick() {
        if (!active) return
        val n = h.now()
        if (TurnSettle.settled(n - lastChangeAt, n - startedAt)) finish(n) else h.schedule(TurnSettle.TICK_MS)
    }

    private fun finish(n: Long) {
        active = false
        lastMs = n - startedAt
        h.cover(false, true)
        val p = paused; paused = null
        var resumed = false
        if (p != null) {
            if (TurnSettle.resumeIt(pausedByUs = true, playingNow = h.playingNow(p), sameTabInFront = h.isFront(p), appOnScreen = h.onScreen(), touchedSince = touched)) {
                resumed = h.resume(p)
                if (resumed) resumedCount++
            } else skippedResume++
        }
        last = "settled after $lastMs ms" + (if (p == null) ", nothing was paused" else if (resumed) ", the video was paused and played again" else ", the video was paused and left paused (touched=$touched)")
        h.log("turn-settled", last)
    }

    /** One Diagnostics line. */
    fun line(): String = "Turning under a video: $turns turns met a full-screen page or a playing video, video paused $pausedCount times, played again $resumedCount, left paused $skippedResume; last: $last"
}
