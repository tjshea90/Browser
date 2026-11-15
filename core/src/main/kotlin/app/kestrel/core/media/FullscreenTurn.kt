package app.kestrel.core.media

/**
 * FS1 / RX4-V1 (owner message 43: "if I watch videos in full screen, then rotate the tablet or press a rotate button, the video switches to landscape mode just like regular video player apps").
 *
 * How the SCREEN turns while a page shows a video full screen. Why this exists: the Android tablet's rotation lock was the only thing that could turn the app, full screen hides the system bars
 * (so the bar's own rotate button cannot be reached), and the engine's `screen.orientation.lock()` request had nobody to answer it. Pure rules, no Android imports: BrowserActivity (through
 * `FullscreenTurner`) applies them with `Activity.requestedOrientation`. The numbers below are `ActivityInfo.SCREEN_ORIENTATION_*`; the app module pins them against the platform's own constants.
 *
 * The rules in one paragraph: while a page is full screen the activity FOLLOWS THE SENSOR (Android's "full sensor" setting ignores the system rotation lock, so turning the tablet always turns
 * the picture, also with auto-rotate off). On a wide video with the tablet held upright, entering full screen turns the screen sideways at once (what players do), and as soon as the tablet really
 * is sideways the one-time turn is let go, so turning it back upright turns the screen back. A site's own `screen.orientation.lock()` wins while it lasts. The rotate button does the same one-time
 * turn in the other direction. Leaving full screen puts back whatever the activity had before.
 */
object FullscreenTurn {
    // ActivityInfo.SCREEN_ORIENTATION_*
    const val UNSPECIFIED = -1
    const val LANDSCAPE = 0
    const val PORTRAIT = 1
    const val SENSOR_LANDSCAPE = 6
    const val SENSOR_PORTRAIT = 7
    const val REVERSE_LANDSCAPE = 8
    const val REVERSE_PORTRAIT = 9
    const val FULL_SENSOR = 10

    /** The Settings choice "Turn the screen for full-screen video". */
    const val MODE_AUTO = 0    // follow the tablet, and turn a wide video sideways when the tablet is held upright
    const val MODE_FOLLOW = 1  // follow the tablet only
    const val MODE_OFF = 2     // only what a site asks for
    const val MODES = 3

    fun cleanMode(m: Int): Int = if (m in 0 until MODES) m else MODE_AUTO

    /** What to ask Android for, and whether that request is a one-time turn that should be let go once the tablet is held that way ([release]). */
    class Plan(val request: Int, val release: Boolean)

    /**
     * A site's `screen.orientation.lock(...)` as GeckoView 160 hands it to OrientationDelegate.onOrientationLock: an ANDROID `SCREEN_ORIENTATION_*` number, NOT the engine's bit set (the engine converts
     * first: `GeckoRuntime.toAndroidOrientation`, read in the 160 bytecode by the independent review RV1: portrait-primary 1, portrait-secondary 9, landscape-primary 0, landscape-secondary 8, portrait
     * 7, landscape 6, any 10, natural -1, anything else 1). Returns the number to ask Android for; null = nothing to apply (natural, or a number nobody sends): the lock is refused.
     */
    fun fromSite(androidOrientation: Int): Int? = when (androidOrientation) {
        LANDSCAPE, PORTRAIT, SENSOR_LANDSCAPE, SENSOR_PORTRAIT, REVERSE_LANDSCAPE, REVERSE_PORTRAIT, FULL_SENSOR -> androidOrientation
        else -> null
    }

    fun isLandscape(o: Int) = o == LANDSCAPE || o == SENSOR_LANDSCAPE || o == REVERSE_LANDSCAPE
    fun isPortrait(o: Int) = o == PORTRAIT || o == SENSOR_PORTRAIT || o == REVERSE_PORTRAIT

    /** A picture clearly wider than tall (4:3 counts, a square one does not). */
    fun isWide(w: Int, h: Int) = w > 0 && h > 0 && w * 10 > h * 11

    /**
     * A page just went full screen. [siteLock] = what the page already asked for (Android number), [videoW] x [videoH] = the playing video's picture (0 = not known yet),
     * [screenLandscape] = the screen is landscape right now.
     */
    fun onEnter(mode: Int, siteLock: Int?, videoW: Int, videoH: Int, screenLandscape: Boolean): Plan {
        if (siteLock != null) return Plan(siteLock, false)
        return when (cleanMode(mode)) {
            MODE_OFF -> Plan(UNSPECIFIED, false)
            MODE_FOLLOW -> Plan(FULL_SENSOR, false)
            else -> if (!screenLandscape && isWide(videoW, videoH)) Plan(SENSOR_LANDSCAPE, true) else Plan(FULL_SENSOR, false)
        }
    }

    /** What the rotate button asks for: the other direction, as a one-time turn. */
    fun rotateButton(screenLandscape: Boolean): Plan = if (screenLandscape) Plan(SENSOR_PORTRAIT, true) else Plan(SENSOR_LANDSCAPE, true)

    /**
     * How the tablet is held, from the orientation sensor ([degrees] 0..359, -1 = flat or unknown; 0 and 180 = the way up the tablet was built for, 90 and 270 = on its side).
     * [naturalPortrait] = the tablet's built-in position is portrait (this one: 600 x 1024). true = sideways, false = upright, null = halfway or unknown (no change of mind).
     */
    fun postureLandscape(degrees: Int, naturalPortrait: Boolean): Boolean? {
        if (degrees < 0) return null
        val d = degrees % 360
        val onNaturalAxis = d <= 30 || d >= 330 || d in 150..210
        val onOtherAxis = d in 60..120 || d in 240..300
        return when {
            onNaturalAxis -> !naturalPortrait
            onOtherAxis -> naturalPortrait
            else -> null
        }
    }

    /** A one-time turn ([request]) is let go (back to following the tablet) as soon as the tablet is held the way it asked for. */
    fun release(request: Int, postureLandscape: Boolean?): Boolean {
        if (postureLandscape == null) return false
        return when {
            isLandscape(request) -> postureLandscape
            isPortrait(request) -> !postureLandscape
            else -> false
        }
    }

    /** For Diagnostics and Health lines. */
    fun name(o: Int): String = when (o) {
        FULL_SENSOR -> "follows the tablet"
        UNSPECIFIED -> "as Android had it"
        LANDSCAPE, SENSOR_LANDSCAPE, REVERSE_LANDSCAPE -> "sideways"
        PORTRAIT, SENSOR_PORTRAIT, REVERSE_PORTRAIT -> "upright"
        else -> "orientation $o"
    }
}
