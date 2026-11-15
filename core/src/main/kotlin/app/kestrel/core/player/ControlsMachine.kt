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
    private var active = false
    private var playing = false
    private var buffering = false
    private var ended = false
    private var pinned = false
    private var turning = false
    private var deadline = 0L

    /** Full screen with a video began: the controls show for [PlayerRules.AFTER_ENTER_MS]. */
    fun start(playingNow: Boolean) {
        active = true; playing = playingNow; buffering = false; ended = false; pinned = false; turning = false
        show(PlayerRules.AFTER_ENTER_MS)
    }

    /** Full screen ended (or the video is gone): everything off at once. */
    fun stop() {
        active = false; pinned = false; turning = false
        h.cancel()
        if (shown) { shown = false; h.apply(false, false) }
    }

    /** A tap on the picture (already told apart from a double tap and from a tap on a control). */
    fun tap() {
        if (!active || turning) return
        if (shown) hide(true) else show(PlayerRules.HIDE_AFTER_MS)
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
    fun turnStarted() { turning = true; h.cancel(); if (shown) { shown = false; h.apply(false, false) } }

    /** The cover is gone: the controls show for a moment. */
    fun turnEnded() { turning = false; if (active) show(PlayerRules.AFTER_TURN_MS) }

    /** The timer the host was asked for. */
    fun tick() {
        if (!active || !shown || turning) return
        if (!autoHide()) return
        val left = deadline - h.now()
        if (left <= 0L) hide(true) else h.schedule(left)
    }

    private fun autoHide() = playing && !pinned && !buffering && !ended

    private fun show(forMs: Long) {
        deadline = h.now() + forMs
        if (!shown) { shown = true; h.apply(true, true) }
        if (forMs > 0L && autoHide()) h.schedule(forMs) else h.cancel()
    }

    private fun restart() {
        deadline = h.now() + PlayerRules.HIDE_AFTER_MS
        if (autoHide()) h.schedule(PlayerRules.HIDE_AFTER_MS) else h.cancel()
    }

    private fun hide(animate: Boolean) {
        h.cancel()
        if (shown) { shown = false; h.apply(false, animate) }
    }
}
