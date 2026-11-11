package app.kestrel.browser.web

import org.mozilla.geckoview.GeckoSession

/** Gecko half of a tab (the shared [Tab] only has an opaque `engineData` slot). */
class GeckoTabData {
    var session: GeckoSession? = null
    var state: GeckoSession.SessionState? = null // updated by ProgressDelegate.onSessionStateChange; used to bring a hibernated tab back
    var privateTab = false
    var delegates: TabDelegates? = null
}

internal val Tab.gecko: GeckoTabData get() = (engineData as? GeckoTabData) ?: GeckoTabData().also { engineData = it }
internal var Tab.session: GeckoSession?
    get() = gecko.session
    set(v) { gecko.session = v }
internal val Tab.isPrivateTab: Boolean get() = gecko.privateTab
