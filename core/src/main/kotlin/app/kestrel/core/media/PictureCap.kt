package app.kestrel.core.media

/**
 * ANS52h (research/VIDEO_ACCELERATION.md section "ANS52h design"): the picture size cap for HLS pages. A site's player picks its picture from the rungs of the stream's master playlist, and its default on a tablet
 * with a 973 x 600 screen is still the 1920 x 1080 rung. The extension answers the playlist request without the rungs whose LONGER side is more than 2 % above the cap (extension: hlscap.js); this object decides
 * the cap, in pixels of that longer side, from the owner's setting and the screen. Pure (no Android imports).
 */
object PictureCap {
    const val AUTO = 0      // follow the screen: no picture bigger than 1.35 times the screen's longer side (the glass cannot show more, and a little more is kept for sharpness)
    const val OFF = 1       // no cap
    const val P480 = 2      // 854 pixels (854 x 480)
    const val P720 = 3      // 1280 pixels (1280 x 720)
    const val P1080 = 4     // 1920 pixels (1920 x 1080)

    /** What "a bit more than the screen" is: a 720p picture on a 973 px tablet is wider than the glass and is kept, 1080p is not. */
    const val AUTO_FACTOR = 1.35
    private const val AUTO_MIN = 640         // never below 360p: a cap that small means a screen that is not a screen
    private const val NO_CAP_FROM = 3840     // an automatic cap this big (a 4K-class display) is no cap

    /** The cap in pixels of the longer side, or 0 = none. [screenLongSidePx] is the display's longer side (0 = unknown: Automatic then caps nothing). */
    fun pixels(mode: Int, screenLongSidePx: Int): Int = when (mode) {
        OFF -> 0
        P480 -> 854
        P720 -> 1280
        P1080 -> 1920
        else -> auto(screenLongSidePx)
    }

    fun auto(screenLongSidePx: Int): Int {
        if (screenLongSidePx <= 0) return 0
        val v = Math.round(screenLongSidePx * AUTO_FACTOR).toInt().coerceAtLeast(AUTO_MIN)
        return if (v >= NO_CAP_FROM) 0 else v
    }

    /** Words for Diagnostics. */
    fun explain(mode: Int, screenLongSidePx: Int): String {
        val px = pixels(mode, screenLongSidePx)
        val what = when (mode) {
            OFF -> "off"
            P480 -> "480p"
            P720 -> "720p"
            P1080 -> "1080p"
            else -> "Automatic (screen $screenLongSidePx px)"
        }
        return "$what: " + (if (px == 0) "no rung is taken out of an HLS master playlist" else "an HLS rung whose longer side is above $px px is taken out (not below the smallest)")
    }
}
