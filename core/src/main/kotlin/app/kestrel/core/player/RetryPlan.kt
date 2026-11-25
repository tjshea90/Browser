package app.kestrel.core.player

/**
 * RVW3 PB1 (research/REVIEW_2026-11-21_PLAYBACK.md): what Kestrel's own player does when the link drops in the MIDDLE of a film.
 *
 * Media3 retries a failed load three times (waits of 0, 1 and 2 s) and then raises a fatal error and goes IDLE. On a flaky, carrier-routed hotspot a 15-second dropout used up all three in about three seconds
 * and the film ended in "This video stopped", whose Play button did nothing (play() on an IDLE player needs prepare() first). Now:
 *  1. once a picture has been shown, a failed load is retried [SETTLED_LOAD_RETRIES] times (about 25 s with the waits Media3 adds up), still silently, the buffered seconds keep playing meanwhile;
 *  2. a fatal error after the first picture that is the NETWORK's fault is answered by up to [MAX_TRIES] automatic prepare() calls after [delayMs], the position kept, the ring shown instead of "stopped";
 *  3. the Play button of the error state prepares again at once.
 * A fatal error that no retry can cure (an expired link: 403, 404, 410; a broken file; a decoder that cannot start) stays final.
 * Before the first picture nothing changes: a failure then hands the film back to the page's own player (ANS54e).
 * Pure; the glue ([app.kestrel.browser.web.PlayerRecovery]) maps Media3's error codes to a [Kind].
 */
object RetryPlan {
    enum class Kind {
        /** The network failed (no connection, a timeout, a 5xx, 408, 429): worth trying again. */
        NETWORK,
        /** A live stream fell behind the window the server keeps: jump to the live edge, then prepare again. */
        LIVE_WINDOW,
        /** Nothing a retry can cure. */
        FINAL,
    }

    /** How many times a failed load of one segment is tried again once a picture has been shown (Media3's own default is 3). */
    const val SETTLED_LOAD_RETRIES = 8

    /** The waits before automatic try 1, 2, 3 and 4 after a fatal network error (ms). */
    private val DELAYS_MS = longArrayOf(2_000L, 4_000L, 8_000L, 8_000L)

    val MAX_TRIES: Int get() = DELAYS_MS.size

    /** The wait before automatic try number [tryIndex] (0 = the first), or -1 = the tries are used up. */
    fun delayMs(tryIndex: Int): Long = if (tryIndex in DELAYS_MS.indices) DELAYS_MS[tryIndex] else -1L

    /** The kind of an HTTP status the server answered a segment or a playlist with. */
    fun httpKind(status: Int): Kind = when {
        status == 408 || status == 429 || status in 500..599 -> Kind.NETWORK
        else -> Kind.FINAL                                // 403 / 404 / 410: an expired or removed link, a retry only waits
    }
}
