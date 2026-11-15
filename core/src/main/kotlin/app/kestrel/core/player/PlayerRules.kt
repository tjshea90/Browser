package app.kestrel.core.player

/**
 * VP1 (owner message 49, research/VIDEO_PLAYER_AUDIT.md): the pure rules of Kestrel's own full-screen video controls. Android and GeckoView are not needed here; the app side is
 * `web/PlayerController` (the clock, the views, the messages) and the page side is `kestrel-shields/player.js`.
 *
 * The numbers are the ones the big players agree on (the audit's table), in one place so a test can pin them and a change is one line.
 */
object PlayerRules {
    /** The controls hide after this long without a touch while the film plays (YouTube about 3 s, Chrome 3 s, ExoPlayer 5 s). Never while it is paused, buffering, ended, or a finger is on a control. */
    const val HIDE_AFTER_MS = 3500L

    /** They show for this long when full screen begins, so the person sees that they exist. */
    const val AFTER_ENTER_MS = 3500L

    /** ... and for this long after the screen has turned (the picture was covered meanwhile). */
    const val AFTER_TURN_MS = 2500L

    /** Two taps this close are a double tap (Chrome and YouTube use about this window): the single tap waits this long before it toggles the controls. */
    const val DOUBLE_TAP_MS = 280L

    /** After a double tap that seeked, further taps on the same side seek again at once until this long passes without one (YouTube). */
    const val BURST_MS = 650L

    /** One seek step of a double tap and of the skip buttons, in seconds. */
    const val SKIP_S = 10.0

    /** The left third and the right third of the picture are the seek sides, the middle third plays / pauses on a double tap. */
    const val SIDE = 0.35

    /** A length that has been the same this long is believed (a Media Source stream grows its duration segment by segment, and an ad has its own). */
    const val STABLE_MS = 2000L

    /** A length may be this much smaller than the played / buffered end before it is called wrong (rounding). */
    const val FLOOR_SLACK_S = 0.75

    enum class Zone { LEFT, CENTER, RIGHT }

    fun zone(xFraction: Double): Zone = when {
        xFraction < SIDE -> Zone.LEFT
        xFraction > 1.0 - SIDE -> Zone.RIGHT
        else -> Zone.CENTER
    }

    /** "1:05", "0:07", "1:02:03". Negative, NaN and infinite times read as 0:00. */
    fun formatTime(seconds: Double): String {
        val t = if (seconds.isNaN() || seconds.isInfinite() || seconds < 0) 0L else seconds.toLong()
        val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Where a skip of [delta] seconds lands: never before 0, never past a KNOWN [duration] (null = not known: no upper bound except the seekable end [seekableEnd] when there is one). */
    fun skipTarget(t: Double, delta: Double, duration: Double?, seekableEnd: Double? = null): Double {
        val raw = t + delta
        val top = duration ?: seekableEnd
        val bounded = if (top != null && top > 0.0) minOf(raw, top) else raw
        return maxOf(0.0, bounded)
    }

    /** The place of [t] on a track of [duration] as 0..1. */
    fun fraction(t: Double, duration: Double): Double = if (duration > 0.0) (t / duration).coerceIn(0.0, 1.0) else 0.0

    /** The time a finger at [fraction] of the track means. */
    fun timeAt(fraction: Double, duration: Double): Double = fraction.coerceIn(0.0, 1.0) * duration
}
