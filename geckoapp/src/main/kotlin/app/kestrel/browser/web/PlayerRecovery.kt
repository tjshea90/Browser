package app.kestrel.browser.web

import android.os.Handler
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import app.kestrel.core.player.RetryPlan

/**
 * RVW3 PB1: Kestrel's own player (full screen and inside the page) used to END THE FILM when the link dropped in the middle of it: Media3 gave up after three quick retries, went IDLE, and the Play button
 * of the "This video stopped" state called play() on an idle player, which does nothing. One instance per player. The plan itself (waits, which errors are worth it) is [RetryPlan], tested in core.
 *  - [onError]: a fatal error AFTER the first picture that is the network's fault schedules up to [RetryPlan.MAX_TRIES] prepare() calls (the position is kept); [pending] says one is waiting, so the controls
 *    show the buffering ring and not "stopped";
 *  - [onReady]: the player plays again: the tries start afresh for the next dropout;
 *  - [playPressed]: the person pressed Play on the error state: prepare at once;
 *  - [stop]: the player is going away.
 * Before the first picture nothing here runs: a failure then hands the film back to the page's own player (ANS54e).
 */
class PlayerRecovery(private val handler: Handler, private val say: (String, String) -> Unit) {
    private var tries = 0
    private var armed: Runnable? = null

    /** An automatic try is waiting: the film is not "stopped", it is trying again. */
    val pending: Boolean get() = armed != null

    /** Which kind of failure this is (Media3's codes; an HTTP status inside it decides first). */
    fun kindOf(e: PlaybackException): RetryPlan.Kind {
        var c: Throwable? = e.cause
        var depth = 0
        while (c != null && depth++ < 6) {
            if (c is HttpDataSource.InvalidResponseCodeException) return RetryPlan.httpKind(c.responseCode)
            c = c.cause
        }
        return when (e.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT -> RetryPlan.Kind.NETWORK
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> RetryPlan.Kind.LIVE_WINDOW
            else -> RetryPlan.Kind.FINAL
        }
    }

    /** A fatal error after the first picture. True = a try is scheduled (the person sees the ring, not "stopped"); false = it is final. */
    fun onError(p: ExoPlayer, e: PlaybackException): Boolean {
        if (armed != null) return true
        val kind = kindOf(e)
        if (kind == RetryPlan.Kind.FINAL) { say("recover-final", "${e.errorCodeName}: not worth a retry"); return false }
        val wait = RetryPlan.delayMs(tries)
        if (wait < 0L) { say("recover-gave-up", "${e.errorCodeName}: $tries tries did not bring it back"); return false }
        tries++
        val n = tries
        val r = Runnable {
            armed = null
            try {
                if (kind == RetryPlan.Kind.LIVE_WINDOW) p.seekToDefaultPosition()
                if (p.playerError != null) p.prepare()
                say("recover-try", "try $n of ${RetryPlan.MAX_TRIES}: prepared again at ${p.currentPosition / 1000} s")
            } catch (x: Throwable) { say("recover-failed", x.javaClass.simpleName) }
        }
        armed = r
        handler.postDelayed(r, wait)
        say("recover-wait", "${e.errorCodeName}: try $n of ${RetryPlan.MAX_TRIES} in ${wait / 1000} s")
        return true
    }

    /** The player is READY again. */
    fun onReady() {
        if (tries > 0) say("recovered", "playing again after $tries ${if (tries == 1) "try" else "tries"}")
        tries = 0
    }

    /** The person pressed Play while the player is in the error state: prepare at once (play() alone does nothing on an idle player). */
    fun playPressed(p: ExoPlayer) {
        if (p.playerError == null) return
        cancel()
        tries = 0
        try { p.prepare(); say("recover-manual", "Play pressed after an error: prepared again at ${p.currentPosition / 1000} s") } catch (x: Throwable) { say("recover-failed", x.javaClass.simpleName) }
    }

    /** The player is going away: no try may run on a released player. */
    fun stop() { cancel() }

    private fun cancel() { armed?.let { handler.removeCallbacks(it) }; armed = null }
}
