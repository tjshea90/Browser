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
 *  - the turn is over when the page area stopped changing size for [SETTLE_MS], or at the latest [CAP_MS] after it began (a page that keeps resizing must not keep the picture black for ever);
 *  - ROT2 (owner message 48: "The video size and frame is awkward for a few seconds"; two screenshots of a page still laid out for the OLD orientation): the engine hands the new window size to the PAGE later
 *    than the app's own view changes (about a second on the emulator, a few seconds on the tablet). When the full-screen page reports its viewport (`vpt`, see `kestrel-shields/turn.js`), the cover and the
 *    pause stay until the page says it HAS the view's new size, not only until the app's own view is still: [PAGE_CAP_MS] is the longest that wait may take, [NUDGE_AFTER_MS] the time after which the engine
 *    is given one real size change if the page still has not caught up, [HINT_AFTER_MS] the time after which a small progress ring shows on the cover so the black does not look frozen.
 */
object TurnSettle {
    /** The screen counts as still when no size change of the page area came for this long. */
    const val SETTLE_MS = 650L

    /** Never longer than this from the first change, whatever the engine does. */
    const val CAP_MS = 2200L

    /** ROT2: the longest a turn may wait for the page to report that it has the new size (the engine of the tablet needs a few seconds; a hung page must not keep the picture black for ever). */
    const val PAGE_CAP_MS = 7000L

    /** ROT2: the page has not caught up this long after the view's last size change: the engine is given one real size change (the 1 px nudge of FB4). */
    const val NUDGE_AFTER_MS = 2500L

    /** ROT2 (review of alpha53): a page that told its size before but has said NOTHING this long into a turn is not reporting (its report was dropped, its script is gone): the old rule decides. */
    const val SILENT_MS = 3000L

    /** ROT2: after this long a small progress ring shows on the cover. */
    const val HINT_AFTER_MS = 900L

    /** ROT2: a finger that comes down this long after the turn began is the person waiting for the picture: the cover goes (an earlier one is the hand that turned the tablet). */
    const val TOUCH_ENDS_AFTER_MS = 900L

    /**
     * ANS52a (owner message 52: after a turn "the status bar icons stuck in the middle of the screen and the video cropped at about the same point for several moments"): the engine's view is kept at its OLD size for the
     * first frame of a turn so the app's window can draw (the black cover, the bars) at the new size at once; GeckoView's own size change blocks the UI thread for 0.9 to 2 s on the tablet and, done first, kept the system
     * showing the old portrait buffers through the new landscape display. This is the longest the view may be held when no frame comes (the app is not drawing: it was left, the screen is off).
     */
    const val HOLD_MAX_MS = 1200L

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

    /**
     * ROT2: the same, for a full-screen page that reports its viewport ([expectsPage]): still for [SETTLE_MS] AND the page said it has the view's size ([pageConfirmed]); or [PAGE_CAP_MS] since the
     * start, whatever the page does. A page that does not report is judged by the old rule.
     */
    fun settled(sinceLastChangeMs: Long, sinceStartMs: Long, expectsPage: Boolean, pageConfirmed: Boolean): Boolean =
        if (!expectsPage) settled(sinceLastChangeMs, sinceStartMs) else (sinceLastChangeMs >= SETTLE_MS && pageConfirmed) || sinceStartMs >= PAGE_CAP_MS

    /** ROT2: the page still has not caught up: time to give the engine one real size change. */
    fun nudgeDue(sinceLastChangeMs: Long): Boolean = sinceLastChangeMs >= NUDGE_AFTER_MS

    /** ROT2: a touch ends the wait for the page (the person takes over) once the turn has lasted this long. */
    fun touchEnds(sinceStartMs: Long): Boolean = sinceStartMs >= TOUCH_ENDS_AFTER_MS

    /** ROT2: the turn takes long: show the progress ring. */
    fun hintDue(sinceStartMs: Long): Boolean = sinceStartMs >= HINT_AFTER_MS
}
