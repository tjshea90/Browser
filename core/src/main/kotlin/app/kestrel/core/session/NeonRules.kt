package app.kestrel.core.session

/**
 * RX3-S1 (research/REVIEW-EXTERNAL-3.md section 9): when the neon glow stands still. The pulse redraws the window ten times a second; while the screen is already changing (a page is loading, the keyboard
 * is up or on its way) or a video plays next to the decoder and the compositor, that is wasted work on a 2 GB tablet (on the emulator the animation alone kept the parent process near 100 % of a core).
 * Pure, no Android: NeonRulesTest.
 */
object NeonRules {
    fun busy(heavyLoad: Boolean, keyboardUp: Boolean, keyboardComing: Boolean, videoPlaying: Boolean): Boolean = heavyLoad || keyboardUp || keyboardComing || videoPlaying

    /** What the engine's media session says about the page's video: only a PLAY makes it "playing", a pause, a stop or the end of the session makes it not. */
    enum class MediaEvent { ACTIVATED, PLAY, PAUSE, STOP, DEACTIVATED }

    fun videoAfter(playing: Boolean, event: MediaEvent): Boolean = when (event) {
        MediaEvent.PLAY -> true
        MediaEvent.PAUSE, MediaEvent.STOP, MediaEvent.DEACTIVATED -> false
        MediaEvent.ACTIVATED -> playing    // a session exists, nothing is said about playing yet
    }
}
