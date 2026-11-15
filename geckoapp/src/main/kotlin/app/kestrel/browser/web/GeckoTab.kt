package app.kestrel.browser.web

import org.mozilla.geckoview.GeckoSession

/** Gecko half of a tab (the shared [Tab] only has an opaque `engineData` slot). */
class GeckoTabData {
    var session: GeckoSession? = null
    var state: GeckoSession.SessionState? = null // updated by ProgressDelegate.onSessionStateChange; used to bring a hibernated tab back
    var privateTab = false
    var stateJson: String? = null // the saved state read from tabstate/<id>.json at start; parsed only when the tab is opened (a tab nobody opens costs nothing)
    var stateWritten: GeckoSession.SessionState? = null // the state object whose text is in tabstate/<id>.json now (the same object is not written twice)
    var stateWrittenUrl: String? = null // the address the tab showed when that file was written: a different address with no newer state = the file is out of date
    var delegates: TabDelegates? = null
    /** RX3-S1: the page's video is playing (the engine's media session said PLAY and has not said pause / stop / end since): the neon glow holds still meanwhile. UI thread. */
    var videoPlaying = false
    /** ROT1: the engine's media session of this page while it is active (it exists only for AUDIBLE media), so the app can pause the video while the screen turns. UI thread. */
    var mediaSession: org.mozilla.geckoview.MediaSession? = null
    /** FS1: the picture size (pixels) of the video that plays on this page, as its element reported it ([picturePlayed] = from an element that really played); 0 = not known. A new page forgets it ([VideoFlow.clearTab]). */
    var pictureW = 0
    /** RX4-X2: when (uptime ms) and with which address the page's navigation began: a touch BEFORE it is the tap that opened the page, not a touch of the page. */
    var pageStartedAt = 0L
    var pageStartUrl = ""
    var pictureH = 0
    var picturePlayed = false
    var fieldInput: app.kestrel.browser.PageFieldInput? = null // AU3: sees the page's text field gain and lose focus; the mini keyboard types through it
}

internal val Tab.gecko: GeckoTabData get() = (engineData as? GeckoTabData) ?: GeckoTabData().also { engineData = it }
internal var Tab.session: GeckoSession?
    get() = gecko.session
    // a session that is closed, hibernated or replaced sends no "deactivated": what it said about its media ("playing", the MediaSession object) must go with it (alpha49 review: otherwise a turn of the screen
    // covered a page with no video, "paused" a dead session and counted it, and the old session stayed reachable from the tab)
    set(v) { val g = gecko; if (v == null || v !== g.session) { g.videoPlaying = false; g.mediaSession = null }; g.session = v }
internal val Tab.isPrivateTab: Boolean get() = gecko.privateTab
