package app.kestrel.browser.web

import android.os.SystemClock
import android.view.View
import app.kestrel.browser.Io
import app.kestrel.browser.diag.Health
import app.kestrel.core.media.TurnSettle

/**
 * ROT1 (owner message 46): "when I rotate the tablet from portrait to landscape and back, it is laggy and glitchy. It may benefit from pausing the video shortly until the orientation is set".
 *
 * While the screen TURNS under a full-screen page or a playing video: the video is paused through the engine's media session (the decoder and the compositor then only have the new layout to do), the page
 * area is covered with black (the half-laid-out frames of a turn are never seen), and both are let go once the page area has stopped changing size for [TurnSettle.SETTLE_MS] (at the latest
 * [TurnSettle.CAP_MS] after the turn began). A video the person had paused stays paused, a touch during the turn is the person taking over (nothing is played for them), and leaving the app lets everything
 * go without playing anything (owner message 45: no sound behind another app). The rules are pure and tested in core (`TurnSettleTest`); this class only holds the clock and the views. UI thread only.
 */
class TurnSettler(
    /** The tab in front. */
    private val front: () -> Tab?,
    /** A page is full screen now. */
    private val fullscreen: () -> Boolean,
    /** The black cover over the page area (a plain view, GONE while nothing turns). */
    private val cover: () -> View?,
    /** The app is on screen (resumed and not paused). */
    private val onScreen: () -> Boolean,
    /** Called once when a turn is over (the engine may be nudged then). */
    private val settledCallback: () -> Unit = {},
) {
    private var wasLandscape: Boolean? = null
    private var active = false
    private var startedAt = 0L
    private var lastChangeAt = 0L
    private var pausedTab: Tab? = null
    private var touched = false

    // what Diagnostics says
    private var turns = 0
    private var pausedCount = 0
    private var resumedCount = 0
    private var skippedResume = 0
    private var lastMs = 0L
    private var last = "no turn under a video yet"

    private val tick = Runnable { check() }

    /** The activity's configuration changed: [landscape] is the orientation now. Called from onConfigurationChanged BEFORE the engine is told. */
    fun configChanged(landscape: Boolean) {
        val before = wasLandscape
        wasLandscape = landscape
        if (before == null) return // the first configuration the settler sees only tells it where the screen is
        val t = front()
        val playing = t?.gecko?.videoPlaying == true
        if (!TurnSettle.worthIt(TurnSettle.isTurn(before, landscape), fullscreen(), playing)) return
        turns++
        val now = SystemClock.uptimeMillis()
        if (!active) { active = true; startedAt = now; touched = false }
        lastChangeAt = now
        showCover()
        if (pausedTab == null && t != null) {
            val ms = t.gecko.mediaSession
            if (TurnSettle.pauseIt(playing, ms != null)) {
                try { ms!!.pause(); pausedTab = t; pausedCount++; Health.log("fullscreen", "turn-pause", "the video is paused while the screen turns (${if (landscape) "landscape" else "portrait"})") }
                catch (e: Throwable) { Health.log("fullscreen", "turn-pause-failed", e.toString().take(80)) }
            } else if (playing) Health.log("fullscreen", "turn-no-session", "the video plays but the engine has no media session to pause (a silent video)")
        }
        schedule()
    }

    /** The page area changed size (KestrelGeckoView.onSizeChanged): the turn is not over yet. */
    fun sizeChanged() { if (active) lastChangeAt = SystemClock.uptimeMillis() }

    /** A finger came down: the person takes over, nothing is played for them afterwards. */
    fun touchedScreen() { if (active) touched = true }

    /** The app left the screen: everything is let go at once and nothing is played. */
    fun abandon() {
        if (!active && pausedTab == null) return
        Io.main.removeCallbacks(tick)
        active = false
        pausedTab = null
        hideCover(fade = false)
        last = "the app was left during a turn: nothing was played"
        Health.log("fullscreen", "turn-abandoned", "left the screen during a turn")
    }

    private fun schedule() { Io.main.removeCallbacks(tick); Io.main.postDelayed(tick, TurnSettle.TICK_MS) }

    private fun check() {
        if (!active) return
        val now = SystemClock.uptimeMillis()
        if (TurnSettle.settled(now - lastChangeAt, now - startedAt)) finish(now) else schedule()
    }

    private fun finish(now: Long) {
        active = false
        lastMs = now - startedAt
        hideCover(fade = true)
        val p = pausedTab; pausedTab = null
        var resumed = false
        if (p != null) {
            val ok = TurnSettle.resumeIt(pausedByUs = true, playingNow = p.gecko.videoPlaying, sameTabInFront = front() === p, appOnScreen = onScreen(), touchedSince = touched)
            if (ok) {
                try { p.gecko.mediaSession?.play(); resumed = true; resumedCount++ } catch (e: Throwable) { Health.log("fullscreen", "turn-resume-failed", e.toString().take(80)) }
            } else skippedResume++
        }
        last = "settled after $lastMs ms" + (if (p == null) ", nothing was paused" else if (resumed) ", the video was paused and played again" else ", the video was paused and left paused (touched=$touched)")
        Health.log("fullscreen", "turn-settled", last)
        try { settledCallback() } catch (e: Throwable) { /* never break the turn */ }
    }

    private fun showCover() {
        val c = cover() ?: return
        c.animate().cancel()
        c.alpha = 1f
        c.visibility = View.VISIBLE
    }

    private fun hideCover(fade: Boolean) {
        val c = cover() ?: return
        c.animate().cancel()
        if (!fade) { c.alpha = 0f; c.visibility = View.GONE; return }
        c.animate().alpha(0f).setDuration(TurnSettle.FADE_MS).withEndAction { c.visibility = View.GONE }.start()
    }

    /** One Diagnostics line. */
    fun line(): String = "Turning under a video: $turns turns met a full-screen page or a playing video, video paused $pausedCount times, played again $resumedCount, left paused $skippedResume; last: $last"
}
