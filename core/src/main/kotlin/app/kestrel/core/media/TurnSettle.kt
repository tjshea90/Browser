package app.kestrel.core.media

/**
 * ROT1 (owner message 46: "when I rotate the tablet from portrait to landscape and back, it is laggy and glitchy. It may benefit from pausing the video shortly until the orientation is set").
 *
 * The pure rules of what the app does while the screen TURNS under a playing video: the video is paused (the decoder and the compositor then have only the new layout to do), the page area is covered with
 * black (the half-laid-out frames of a turn are never seen), and both are let go once the screen has been still for a moment. The app side is `web/TurnSettler`; Android and GeckoView are not needed here.
 *
 * The decisions, in plain words:
 *  - a turn matters when a page is full screen or the video on the front tab plays (an ordinary page re-lays out as it always did);
 *  - the video is paused only when the engine's media session says it plays and the app holds that session;
 *  - it is played again only when the APP paused it (never one the person paused), it is still paused, the same tab is in front, the app is on screen and nobody touched the screen meanwhile
 *    (a touch during the turn is the person taking over: their play / pause stands);
 *  - the turn is over when the page area stopped changing size for [SETTLE_MS], or at the latest [CAP_MS] after it began (a page that keeps resizing must not keep the picture black for ever).
 */
object TurnSettle {
    /** The screen counts as still when no size change of the page area came for this long. */
    const val SETTLE_MS = 650L

    /** Never longer than this from the first change, whatever the engine does. */
    const val CAP_MS = 2200L

    /** The black cover fades out over this long. */
    const val FADE_MS = 160L

    /** How often the settle check looks (it is a cheap timer, not a polling loop on the page). */
    const val TICK_MS = 120L

    /** Android's orientation of the screen: [landscape] true / false. A change of this is a turn; a keyboard, locale or dark-mode change that reaches the activity is not. */
    fun isTurn(wasLandscape: Boolean, nowLandscape: Boolean): Boolean = wasLandscape != nowLandscape

    /** A turn is worth a pause and a cover when a page is full screen or the front tab's video plays. */
    fun worthIt(turn: Boolean, fullscreen: Boolean, videoPlaying: Boolean): Boolean = turn && (fullscreen || videoPlaying)

    /** Pause the video for the turn: it plays and the app holds the engine's media session of that tab. */
    fun pauseIt(videoPlaying: Boolean, haveSession: Boolean): Boolean = videoPlaying && haveSession

    /** Play it again: the app paused it, it is still paused, the same tab is in front, the app is on screen and nobody touched the screen since the turn began. */
    fun resumeIt(pausedByUs: Boolean, playingNow: Boolean, sameTabInFront: Boolean, appOnScreen: Boolean, touchedSince: Boolean): Boolean =
        pausedByUs && !playingNow && sameTabInFront && appOnScreen && !touchedSince

    /** The turn is over: still for [SETTLE_MS], or [CAP_MS] since the start. Times in milliseconds. */
    fun settled(sinceLastChangeMs: Long, sinceStartMs: Long): Boolean = sinceLastChangeMs >= SETTLE_MS || sinceStartMs >= CAP_MS
}
