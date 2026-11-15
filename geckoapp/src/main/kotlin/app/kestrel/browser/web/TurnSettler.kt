package app.kestrel.browser.web

import android.os.SystemClock
import android.view.View
import app.kestrel.browser.Io
import app.kestrel.browser.diag.Health
import app.kestrel.core.media.TurnSettle
import app.kestrel.core.media.TurnSettleMachine

/**
 * ROT1 (owner message 46): "when I rotate the tablet from portrait to landscape and back, it is laggy and glitchy. It may benefit from pausing the video shortly until the orientation is set".
 *
 * While the screen TURNS under a full-screen page or a playing video: the video is paused through the engine's media session (the decoder and the compositor then only have the new layout to do), the page
 * area is covered with black (the half-laid-out frames of a turn are never seen), and both are let go once the page area has stopped changing size for [TurnSettle.SETTLE_MS] (at the latest
 * [TurnSettle.CAP_MS] after the turn began). A video the person had paused stays paused, a touch during the turn is the person taking over (nothing is played for them), and leaving the app lets everything
 * go without playing anything (owner message 45: no sound behind another app). ROT2 (owner message 48): when the full-screen page reports its viewport, the cover also waits until the page says it HAS the
 * new size (the engine hands it over later than the app's own view changes). The decisions are the pure [TurnSettleMachine] / [TurnSettle] in core (`TurnSettleMachineTest`, `TurnSettleTest`); this class only
 * gives the machine the facts of the moment and carries out what it decides with the views and the engine. UI thread only.
 */
class TurnSettler(
    /** The tab in front. */
    private val front: () -> Tab?,
    /** A page is full screen now. */
    private val isFullscreen: () -> Boolean,
    /** The black cover over the page area (a plain view, GONE while nothing turns). */
    private val cover: () -> View?,
    /** The app is on screen (resumed and not paused). */
    private val isOnScreen: () -> Boolean,
    /** ROT2: the full-screen page in front has told its viewport before (its report can say when it has a turn's new size). */
    private val expectsPage: () -> Boolean = { false },
    /** ROT2: give the engine one real size change (the view one pixel shorter for a moment). */
    private val nudge: () -> Unit = {},
    /** ROT2: the small progress ring on the cover. */
    private val hintView: () -> View? = { null },
) {
    private val tick = Runnable { machine.tick() }

    private val machine: TurnSettleMachine<Tab> = TurnSettleMachine(object : TurnSettleMachine.Host<Tab> {
        override fun now() = SystemClock.uptimeMillis()
        override fun schedule(delayMs: Long) { Io.main.removeCallbacks(tick); Io.main.postDelayed(tick, delayMs) }
        override fun cancelSchedule() { Io.main.removeCallbacks(tick) }
        override fun cover(show: Boolean, fade: Boolean) {
            val c = cover() ?: return
            c.animate().cancel()
            if (show) { c.alpha = 1f; c.visibility = View.VISIBLE; return }
            if (!fade) { c.alpha = 0f; c.visibility = View.GONE; return }
            c.animate().alpha(0f).setDuration(TurnSettle.FADE_MS).withEndAction { c.visibility = View.GONE }.start()
        }
        override fun fullscreen() = isFullscreen()
        // a tab without a live session has nothing that plays, whatever it last said (the session setter forgets it too)
        override fun frontPlaying() = front()?.let { it.session != null && it.gecko.videoPlaying } == true
        override fun frontHasSession() = front()?.gecko?.mediaSession != null
        override fun pauseFront(): Tab? {
            val t = front() ?: return null
            val ms = t.gecko.mediaSession ?: return null
            return try { ms.pause(); t } catch (e: Throwable) { Health.log("fullscreen", "turn-pause-failed", e.toString().take(80)); null }
        }
        override fun playingNow(handle: Tab) = handle.gecko.videoPlaying
        override fun isFront(handle: Tab) = front() === handle
        override fun onScreen() = isOnScreen()
        override fun resume(handle: Tab): Boolean {
            val ms = handle.gecko.mediaSession ?: return false
            return try { ms.play(); true } catch (e: Throwable) { Health.log("fullscreen", "turn-resume-failed", e.toString().take(80)); false }
        }
        override fun log(event: String, text: String) { Health.log("fullscreen", event, text) }
        override fun expectsPageReport() = expectsPage()
        override fun nudgeEngine() = nudge()
        override fun hint(show: Boolean) { hintView()?.visibility = if (show) View.VISIBLE else View.GONE }
    })

    /** Where the screen is at the start (the first turn must count too). */
    fun init(landscape: Boolean) = machine.init(landscape)

    /** The activity's configuration changed: [landscape] is the orientation now. Called from onConfigurationChanged BEFORE the engine is told. */
    fun configChanged(landscape: Boolean) = machine.configChanged(landscape)

    /** The page area changed size (KestrelGeckoView.onSizeChanged): the turn is not over yet. */
    fun sizeChanged() = machine.sizeChanged()

    /** ROT2: the full-screen page reported its viewport; [matchesView] = it is the size of the page area now. */
    fun pageReported(matchesView: Boolean) = machine.pageReported(matchesView)

    /** ROT2: a turn is going on (the cover is up). */
    val turning: Boolean get() = machine.turning

    /** A finger came down: the person takes over, nothing is played for them afterwards. */
    fun touchedScreen() = machine.touchedScreen()

    /** The app left the screen (or is going away): everything is let go at once and nothing is played. */
    fun abandon() = machine.abandon()

    /** One Diagnostics line. */
    fun line(): String = machine.line()
}
