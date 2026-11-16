package app.kestrel.core.media

/**
 * ROT1 (owner message 46): the clock and the state of "the screen turns under a full-screen page or a playing video", pure and testable. The host (the app: `web/TurnSettler`) gives the facts of the moment
 * and carries out the actions (pause / play through the engine's media session, a black cover over the page, a timer); this class decides WHEN, by the rules of [TurnSettle].
 *
 * One turn, in order: [configChanged] (the screen turned: cover the page, pause the video if the app can) -> [sizeChanged] while the page area is still being laid out -> [tick] every [TurnSettle.TICK_MS]
 * until the area has been still for [TurnSettle.SETTLE_MS] (or [TurnSettle.CAP_MS] since the start) -> the cover fades out and the video plays again only if [TurnSettle.resumeIt] says so. A second turn
 * while one is going extends it (the video is paused once). [touchedScreen] = the person takes over: nothing is played for them. [abandon] = the app left the screen: everything goes, nothing is played.
 *
 * ROT2 (owner message 48): when the host says a full-screen page reports its viewport ([Host.expectsPageReport]), the turn is not over until [pageReported] said the page has the view's size
 * (a later size change takes that back), at the latest [TurnSettle.PAGE_CAP_MS] after the start; after [TurnSettle.NUDGE_AFTER_MS] without it the engine is nudged once ([Host.nudgeEngine]); after
 * [TurnSettle.HINT_AFTER_MS] the host shows a small progress ring ([Host.hint]). How long the page took is kept for Diagnostics.
 */
class TurnSettleMachine<T : Any>(private val h: Host<T>) {
    private companion object { const val LATE_MAX_MS = 30_000L }

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
        /** ROT2: the full-screen page in front reports its viewport (it has done so before), so its report can say when it has the new size. */
        fun expectsPageReport(): Boolean = false
        /** ROT2: give the engine one real size change (the view one pixel shorter for a moment): it has not handed the turn's size to the page. */
        fun nudgeEngine() {}
        /** ROT2: a small progress ring on the cover ([show]) while the page takes long to catch up. */
        fun hint(show: Boolean) {}
    }

    private var wasLandscape: Boolean? = null
    private var active = false
    private var startedAt = 0L
    private var lastChangeAt = 0L
    private var paused: T? = null
    private var touched = false
    private var confirmed = false
    private var confirmedAt = 0L
    private var heard = 0                 // reports of the page since this turn began (any: right or wrong size)
    private var nudged = false
    private var hinted = false

    // what Diagnostics says
    var turns = 0; private set
    var pausedCount = 0; private set
    var resumedCount = 0; private set
    var skippedResume = 0; private set
    var lastMs = 0L; private set
    var lagTurns = 0; private set          // turns whose page said it had the new size
    var lagLastMs = 0L; private set        // from the start of the turn to that report
    var lagWorstMs = 0L; private set
    var gaveUp = 0; private set            // turns that waited the whole cap for a page that never said it
    var nudges = 0; private set
    var silent = 0; private set            // ANS51c: turns whose page had said NOTHING when the old rule let the cover go ([TurnSettle.SILENT_MS])
    var lateCount = 0; private set         // ... of which the page spoke later (after the cover had gone) and the time was measured
    var lateLastMs = 0L; private set       // from the start of that turn to the page's first word
    var lateWorstMs = 0L; private set
    private var lateFrom = -1L             // the start of the last silent turn while its page has not spoken yet (-1 = none waits)
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
        // a turn while one goes on is a new turn for the clocks (review of alpha53: a rotation at 6.9 s of the first one got 0.1 s of cover, and a touch counted from the first): the video is paused once
        if (!active) { active = true; touched = false; hinted = false }
        lateFrom = -1L                                                     // (a page that was still silent from the turn before is now measured by THIS turn)
        startedAt = t; confirmedAt = 0L; nudged = false; heard = 0
        lastChangeAt = t
        confirmed = false                                                  // a new turn (or one more while it goes on): the page has to say it has the new size again
        h.cover(true, false)
        if (paused == null) {
            if (TurnSettle.pauseIt(playing, h.frontHasSession())) {
                val handle = h.pauseFront()
                if (handle != null) { paused = handle; pausedCount++; h.log("turn-pause", "the video is paused while the screen turns (${if (landscape) "landscape" else "portrait"})") }
            } else if (playing) h.log("turn-no-session", "the video plays but the engine has no media session to pause (a silent video)")
        }
        h.schedule(TurnSettle.TICK_MS)
    }

    /** The page area changed size: the turn is not over while this keeps coming, and a report that came before this change no longer says the page has the view's size. */
    fun sizeChanged() { if (active) { lastChangeAt = h.now(); confirmed = false } }

    /** ROT2: the full-screen page reported its viewport; [matchesView] = it is the view's size now. Only a matching report that comes after the last size change confirms the turn. */
    fun pageReported(matchesView: Boolean) {
        if (!active) { lateWord(); return }
        heard++
        if (matchesView) { if (!confirmed) { confirmed = true; confirmedAt = h.now() } } else confirmed = false
    }

    /** ANS51c (owner message 51: the tablet's page said its first word 3.6 s and 7.8 s after a turn, and the line said "0 ms, 0 turns measured"): the first report of a page whose turn was let go while it was still silent. */
    private fun lateWord() {
        if (lateFrom < 0L) return
        val lag = h.now() - lateFrom
        lateFrom = -1L
        if (lag < 0L || lag > LATE_MAX_MS) return                           // (a report that comes half a minute later has nothing to do with that turn)
        lateCount++; lateLastMs = lag; if (lag > lateWorstMs) lateWorstMs = lag
        h.log("turn-late-word", "the page said its first word $lag ms after the turn began (the cover had gone after $lastMs ms)")
    }

    /** A finger came down: the person takes over (nothing is played for them). After about a second of waiting for the picture it also ends the wait: the cover goes. */
    fun touchedScreen() {
        if (!active) return
        touched = true
        val n = h.now()
        if (TurnSettle.touchEnds(n - startedAt)) { h.cancelSchedule(); finish(n, h.expectsPageReport(), byTouch = true) }
    }

    /** The app left the screen: everything goes at once and nothing is played. */
    fun abandon() {
        if (!active && paused == null) return
        h.cancelSchedule()
        active = false
        paused = null
        h.cover(false, false)
        if (hinted) { hinted = false; h.hint(false) }
        last = "the app was left during a turn: nothing was played"
        h.log("turn-abandoned", "left the screen during a turn")
    }

    /** The timer the host was asked for ([Host.schedule]) came. */
    fun tick() {
        if (!active) return
        val n = h.now()
        val expects = h.expectsPageReport() && !(heard == 0 && n - startedAt >= TurnSettle.SILENT_MS)       // a page that said nothing at all for 3 s is not reporting: the old rule
        if (TurnSettle.settled(n - lastChangeAt, n - startedAt, expects, confirmed)) { finish(n, expects); return }
        if (expects && !confirmed && !nudged && TurnSettle.nudgeDue(n - lastChangeAt)) {
            nudged = true; nudges++
            h.log("turn-nudge", "the page has not taken the new size ${n - lastChangeAt} ms after the last size change: the engine is given a real size change")
            h.nudgeEngine()
        }
        if (!hinted && TurnSettle.hintDue(n - startedAt)) { hinted = true; h.hint(true) }
        h.schedule(TurnSettle.TICK_MS)
    }

    private fun finish(n: Long, expects: Boolean = false, byTouch: Boolean = false) {
        active = false
        lastMs = n - startedAt
        h.cover(false, true)
        if (hinted) { hinted = false; h.hint(false) }
        if (expects) {
            if (confirmed && confirmedAt > 0L) { val lag = confirmedAt - startedAt; lagTurns++; lagLastMs = lag; if (lag > lagWorstMs) lagWorstMs = lag }
            else if (!byTouch) gaveUp++
        } else if (!byTouch && heard == 0 && h.expectsPageReport()) { silent++; lateFrom = startedAt }   // ANS51c: the page was expected to speak and said nothing: counted, and its first word later is timed
        val p = paused; paused = null
        var resumed = false
        if (p != null) {
            if (TurnSettle.resumeIt(pausedByUs = true, playingNow = h.playingNow(p), sameTabInFront = h.isFront(p), appOnScreen = h.onScreen(), touchedSince = touched)) {
                resumed = h.resume(p)
                if (resumed) resumedCount++
            } else skippedResume++
        }
        last = "settled after $lastMs ms" + (if (p == null) ", nothing was paused" else if (resumed) ", the video was paused and played again" else ", the video was paused and left paused (touched=$touched)") +
            (if (expects) (if (confirmed) ", the page had the new size after ${confirmedAt - startedAt} ms" else if (byTouch) ", ended by a touch before the page had the new size" else ", the page never said it had the new size (gave up)") else "")
        h.log("turn-settled", last)
    }

    /** One Diagnostics line. */
    fun line(): String = "Turning under a video: $turns turns met a full-screen page or a playing video, video paused $pausedCount times, played again $resumedCount, left paused $skippedResume; " +
        "the page took ${lagLastMs} ms to have the new size after the last turn (worst $lagWorstMs ms, $lagTurns turns measured, $gaveUp gave up after ${TurnSettle.PAGE_CAP_MS / 1000} s, engine nudged $nudges times); " +
        "silent pages: $silent turns where the page said nothing for ${TurnSettle.SILENT_MS / 1000} s, of which $lateCount spoke later (first word ${lateLastMs} ms after the turn began, worst $lateWorstMs ms); last: $last"
}
