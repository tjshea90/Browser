package app.kestrel.core.player

/**
 * VP2 (owner message 49: "each time I tap the screen it should toggle playback controls shown or hidden, just like other popular video players"): when the full-screen controls are shown, pure and testable
 * with a fake clock. The host (the app: `web/PlayerController`) gives the clock and carries out [Host.apply].
 *
 * The rules the big players share:
 *  - a tap on the picture toggles the controls (shown -> hidden, hidden -> shown);
 *  - shown by a tap or by full screen beginning, they hide by themselves [PlayerRules.HIDE_AFTER_MS] later while the film PLAYS;
 *  - they stay while the film is paused, ended, buffering or a finger is on a control (pinned), and they come back when it pauses or ends;
 *  - using a control (a button, the track) restarts the clock;
 *  - while the screen turns nothing shows (the cover is up); afterwards they show for [PlayerRules.AFTER_TURN_MS].
 * State changes are edge-triggered: a report that repeats "paused" every 250 ms must not bring the controls back after the person hid them.
 */
class ControlsMachine(private val h: Host) {
    interface Host {
        fun now(): Long
        /** Call [ControlsMachine.tick] after this long (an earlier request is replaced). */
        fun schedule(delayMs: Long)
        fun cancel()
        /** Show or hide the controls; [animate] = fade (false = at once). */
        fun apply(shown: Boolean, animate: Boolean)
    }

    var shown = false; private set
    /** How long they stay after a touch while the film plays: [PlayerRules.HIDE_AFTER_MS], or more when the person's accessibility setting asks for it. */
    var hideAfterMs = PlayerRules.HIDE_AFTER_MS
    private var active = false
    private var playing = false
    private var buffering = false
    private var ended = false
    private var pinned = false
    private var turning = false
    private var deadline = 0L
    private var shownAt = 0L
    /** RVW6 (owner 2026-11-20: "the player controls pop up very fast then disappear"): how often the controls showed, and how often they went away again within [SHORT_LIFE_MS] and why (Diagnostics). */
    var shows = 0; private set
    var shortLives = 0; private set
    var lastShort = ""; private set
    fun lifeLine(): String = "controls shown $shows times, $shortLives of them hid again within ${SHORT_LIFE_MS / 1000.0} s" + (if (lastShort.isNotEmpty()) " (last: $lastShort)" else "")

    /** Full screen with a video began: the controls show for [PlayerRules.AFTER_ENTER_MS]. */
    fun start(playingNow: Boolean) {
        active = true; playing = playingNow; buffering = false; ended = false; pinned = false; turning = false
        show(scaled(PlayerRules.AFTER_ENTER_MS))
    }

    /** Full screen ended (or the video is gone): everything off at once. */
    fun stop() {
        active = false; pinned = false; turning = false
        h.cancel()
        if (shown) { shown = false; h.apply(false, false) }
    }

    /** A tap on the picture (already told apart from a double tap and from a tap on a control). ANS53c: they come and go AT ONCE, no fade (the owner: "zero delay"); only the clock's own hide fades. */
    fun tap() {
        if (!active || turning) return
        if (shown) hide(false) else show(hideAfterMs, false)
    }

    /**
     * ANS57a (owner message 57: "if I double tap on the right side or left side of a video to fast forward or rewind 10 seconds, it automatically closes the video player controls ... I'm not selecting video
     * controls so the video should stay visible without controls on top"): a seek made by tapping the picture is not a use of the controls. The first tap of the double tap had brought them up (every tap toggles at
     * once, ANS53c), the seek sends them away again at once and they stay away through the burst; the "10 s" flash is alone on the picture. (The seek BUTTONS are another thing: they are controls, the clock restarts.)
     */
    fun seekGesture() {
        if (!active || turning) return
        hide(false, "seek-gesture", count = false)       // by design (ANS57a): the first tap of a double tap had shown them
    }

    /** ANS53c: the app showed or hid the controls at once for a tap, and the page then said that tap was on one of ITS OWN buttons (skip ad ...): they go back to what they were, at once. */
    fun restore(was: Boolean) {
        if (!active || turning) return
        if (was && !shown) show(hideAfterMs, false) else if (!was && shown) hide(false, "put-back")
    }

    /** What the video does now; only a CHANGE is acted on. */
    fun state(playingNow: Boolean, bufferingNow: Boolean, endedNow: Boolean) {
        val wasPlaying = playing; val wasEnded = ended; val could = autoHide()
        playing = playingNow; buffering = bufferingNow; ended = endedNow
        if (!active || turning) return
        if ((wasPlaying && !playingNow) || (!wasEnded && endedNow)) show(0L)          // it paused or ended: the controls come (and stay: no clock while paused)
        else if (!could && autoHide() && shown) restart()                             // it started playing, or stopped buffering: the usual clock
    }

    /** The person used a control (a button, the track): the clock restarts. */
    fun interacted() { if (active && shown && !turning) restart() }

    /** A finger is on a control (scrubbing, holding): no hiding until it lets go. */
    fun pin(on: Boolean) {
        pinned = on
        if (!active) return
        if (on) h.cancel() else if (shown) restart()
    }

    /** The screen turned: the cover is up, the controls go at once. */
    fun turnStarted() { turning = true; pinned = false; h.cancel(); if (shown) { shown = false; h.apply(false, false) } }     // (a finger on a control when the screen turned: its lift will not come)

    /** The cover is gone: the controls show for a moment. */
    fun turnEnded() { turning = false; if (active) show(scaled(PlayerRules.AFTER_TURN_MS)) }

    /** The timer the host was asked for. */
    fun tick() {
        if (!active || !shown || turning) return
        if (!autoHide()) return
        val left = deadline - h.now()
        if (left <= 0L) hide(true, "clock") else h.schedule(left)
    }

    private fun autoHide() = playing && !pinned && !buffering && !ended

    /** A fixed time stays what it is, unless the person's accessibility setting asks for longer. */
    private fun scaled(base: Long) = if (hideAfterMs > PlayerRules.HIDE_AFTER_MS) maxOf(base, hideAfterMs) else base

    private fun show(forMs: Long, animate: Boolean = true) {
        deadline = h.now() + forMs
        if (!shown) { shown = true; shownAt = h.now(); shows++; h.apply(true, animate) }
        if (forMs > 0L && autoHide()) h.schedule(forMs) else h.cancel()
    }

    private fun restart() {
        deadline = h.now() + hideAfterMs
        if (autoHide()) h.schedule(hideAfterMs) else h.cancel()
    }

    private fun hide(animate: Boolean, why: String = "tap", count: Boolean = true) {
        h.cancel()
        if (shown) {
            val life = h.now() - shownAt
            if (count && life < SHORT_LIFE_MS) { shortLives++; lastShort = "$why after $life ms" }
            shown = false; h.apply(false, animate)
        }
    }

    companion object {
        /** Controls that go away within this long of showing cannot have been used by a person. */
        const val SHORT_LIFE_MS = 1500L
    }
}
