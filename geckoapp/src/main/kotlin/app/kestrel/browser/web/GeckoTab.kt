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
    var fieldInput: app.kestrel.browser.PageFieldInput? = null // AU3: sees the page's text field gain and lose focus; the mini keyboard types through it
}

internal val Tab.gecko: GeckoTabData get() = (engineData as? GeckoTabData) ?: GeckoTabData().also { engineData = it }
internal var Tab.session: GeckoSession?
    get() = gecko.session
    set(v) { gecko.session = v }
internal val Tab.isPrivateTab: Boolean get() = gecko.privateTab
