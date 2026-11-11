package app.kestrel.browser.web

import app.kestrel.browser.diag.Health
import app.kestrel.browser.BrowserActivity
import app.kestrel.browser.shield.Shields
import app.kestrel.core.filter.Category
import app.kestrel.core.url.RedirectUnwrapper
import app.kestrel.core.url.UrlParts
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.SlowScriptResponse
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

/**
 * Everything the Firefox engine tells us about ONE tab, translated into the shared [Tab] model, the shared native screens (error, prompts) and
 * Kestrel's own policies (HTTPS-first, tracking-parameter stripping, redirector unwrapping, malware pages, external links). The WebView build does
 * the same job in BrowserWebViewClient / BrowserChromeClient.
 */
class TabDelegates(private val a: BrowserActivity, private val tab: Tab) :
    NavigationDelegate, ProgressDelegate, ContentDelegate, PromptDelegate, PermissionDelegate, ContentBlocking.Delegate {

    private val allow = AllowOrDeny.ALLOW
    private val deny = AllowOrDeny.DENY

    // ------------------------------------------------------------------ navigation

    override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
        // Only COMMITTED pages reach the address bar. Engine-internal pages (about:neterror, our blank error backdrop) never do.
        if (url == null || url.startsWith("about:") || url.startsWith("data:")) return
        tab.url = url
        tab.pendingNav = null
        val host = UrlParts.hostOf(url)
        tab.topHost = host
        tab.mainUrl = url
        tab.shields = Shields.effective(a.prefs, host)
        // The upgraded page is up: a later flaky reload is a network problem, not "this site has no HTTPS".
        if (tab.error == null) { tab.upgradedFrom = null; tab.upgradedTo = null }
        a.tabChanged(tab)
    }

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) { tab.canGoBack = canGoBack; a.tabChanged(tab) }
    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) { tab.canGoForward = canGoForward; a.tabChanged(tab) }

    override fun onLoadRequest(session: GeckoSession, request: NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
        val url = request.uri
        val lower = url.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            val host = UrlParts.hostOf(url)
            val s = Shields.effective(a.prefs, host)
            var next = url
            if (s.shieldsUp && s.blockAds) RedirectUnwrapper.unwrap(next)?.let { next = it }
            val prep = NavPolicy.prepare(a.prefs, next, tab)
            if (prep.url != url) {
                // A server answering https://x/ with a redirect back to http://x/ would loop forever (upgrade -> redirect -> upgrade ...).
                val up = tab.upgradedTo
                if (request.isRedirect && up != null && prep.upgradedFrom != null && UrlParts.hostOf(up) == host) { a.httpsUnavailable(tab, up); return GeckoResult.fromValue(deny) }
                tab.upgradedFrom = prep.upgradedFrom
                tab.upgradedTo = if (prep.upgradedFrom != null) prep.url else null
                a.loadInTab(tab, prep.url)
                return GeckoResult.fromValue(deny)
            }
            if (request.target == NavigationDelegate.TARGET_WINDOW_CURRENT && a.dangerousDocument(tab, url)) return GeckoResult.fromValue(deny)
            if (request.target == NavigationDelegate.TARGET_WINDOW_CURRENT) a.noteMainFrame(tab, url)
            return GeckoResult.fromValue(allow)
        }
        // about:, blob:, data:, javascript:, view-source: and the engine's own schemes are the engine's business; everything else (mailto:,
        // tel:, intent:, market:, custom app schemes) is an external-app request that only a real tap may start.
        if (lower.startsWith("about:") || lower.startsWith("blob:") || lower.startsWith("data:") || lower.startsWith("javascript:") ||
            lower.startsWith("resource:") || lower.startsWith("moz-extension:") || lower.startsWith("view-source:")) return GeckoResult.fromValue(allow)
        a.externalLaunch(tab, url, request.hasUserGesture)
        return GeckoResult.fromValue(deny)
    }

    override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? = GeckoResult.fromValue(a.newPopupSession(tab, uri))

    override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
        val failing = uri ?: tab.url
        val host = UrlParts.hostOf(failing) ?: ""
        tab.loading = false
        tab.progress = 0
        when (error.code) {
            WebRequestError.ERROR_SECURITY_SSL, WebRequestError.ERROR_SECURITY_BAD_CERT, WebRequestError.ERROR_BAD_HSTS_CERT -> {
                // Certificate problems: the engine's own page knows the details and implements "accept the risk" correctly; show it.
                val up = tab.upgradedTo
                if (up != null && UrlParts.hostOf(up) == host && a.httpsUnavailable(tab, up)) return GeckoResult.fromValue(BLANK)
                return null
            }
            WebRequestError.ERROR_HTTPS_ONLY -> tab.error = ErrorScreen.HttpsUnavailable(failing, host)
            WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI, WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI,
            WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI, WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> tab.error = ErrorScreen.Dangerous(failing, host, Category.MALWARE)
            WebRequestError.ERROR_CONTENT_CRASHED -> tab.error = ErrorScreen.Crashed(failing)
            else -> {
                val code = webViewCode(error.code)
                // https:// we chose ourselves did not work (and the hotspot itself is fine): fall back (UPGRADE) or ask (STRICT).
                val up = tab.upgradedTo
                if (tab.savedCopyOnce) { tab.savedCopyOnce = false; a.toast("No saved copy of this page") }
                else if (up != null && UrlParts.hostOf(up) == host && (code == ERROR_CONNECT || code == ERROR_TIMEOUT || code == ERROR_IO) &&
                    app.kestrel.browser.net.NetworkMonitor.state != app.kestrel.browser.net.NetworkMonitor.State.OFFLINE && a.httpsUnavailable(tab, up)) return GeckoResult.fromValue(BLANK)
                tab.error = ErrorScreen.Network(failing, code, "Firefox engine error ${error.code}")
            }
        }
        a.errorChanged(tab)
        return GeckoResult.fromValue(BLANK) // an empty page behind our native error screen
    }

    // ------------------------------------------------------------------ progress

    override fun onPageStart(session: GeckoSession, url: String) {
        tab.loading = true
        tab.progress = maxOf(tab.progress, 5)
        tab.resetPageCounters()
        tab.media.clear() // a new page: what the old one played is no longer on offer
        tab.dialogCount = 0
        tab.dialogsSuppressed = false
        // A new real navigation clears a previous error screen (but not the blank backdrop we load behind one).
        if (tab.error != null && !url.startsWith("data:") && !url.startsWith("about:")) { tab.error = null; a.errorChanged(tab) }
        a.tabChanged(tab)
    }

    override fun onPageStop(session: GeckoSession, success: Boolean) {
        tab.loading = false
        tab.progress = 100
        tab.savedCopyOnce = false
        a.pageFinished(tab)
    }

    override fun onProgressChange(session: GeckoSession, progress: Int) {
        val before = tab.progress
        tab.progress = progress
        // the progress line is redrawn in steps of 3 %: a page load fires this many times, and each used to run the whole UI refresh
        if (progress >= 100 || progress <= 5 || progress < before || progress / 3 != before / 3) a.tabChanged(tab)
    }

    override fun onSessionStateChange(session: GeckoSession, sessionState: GeckoSession.SessionState) { tab.gecko.state = sessionState }

    // ------------------------------------------------------------------ content

    override fun onTitleChange(session: GeckoSession, title: String?) { tab.title = title ?: ""; a.tabChanged(tab) }
    override fun onCloseRequest(session: GeckoSession) { a.closeTab(tab) }
    override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) { a.fullScreenChanged(fullScreen) }
    override fun onContextMenu(session: GeckoSession, screenX: Int, screenY: Int, element: ContentDelegate.ContextElement) { a.longPress(tab, element) }
    override fun onCrash(session: GeckoSession) { Health.log("gecko", "onCrash", tab.url.take(80)); Health.count("gecko.crash"); a.sessionGone(tab, true) }
    override fun onFirstComposite(session: GeckoSession) { if (a.isCurrentTab(tab)) { Health.log("gecko", "first-composite", a.sinceTrim()); Health.count("gecko.firstComposite") } } // a repeat without a navigation = the compositor restarted
    override fun onPaintStatusReset(session: GeckoSession) { if (a.isCurrentTab(tab)) { Health.log("gecko", "paint-reset", a.sinceTrim()); Health.count("gecko.paintReset") } }
    override fun onFirstContentfulPaint(session: GeckoSession) { a.firstPaint(tab) } // a page that is coming back after its process died: take the preview down
    override fun onKill(session: GeckoSession) { Health.log("gecko", "onKill", tab.url.take(80)); Health.count("gecko.kill"); a.sessionGone(tab, false) }

    override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
        val h = response.headers
        fun header(n: String): String? = h.entries.firstOrNull { it.key.equals(n, ignoreCase = true) }?.value
        val length = header("Content-Length")?.toLongOrNull() ?: -1L
        // The channel starts suspended and resumes on the first read: closing without reading costs no data and frees the connection. The download manager makes its own
        // (Range-capable, resumable) request through the same network stack, with the same cookies.
        try { response.body?.close() } catch (e: Exception) { /* ignore */ }
        a.download(tab, response.uri, null, header("Content-Disposition"), header("Content-Type")?.substringBefore(';')?.trim(), length)
    }

    override fun onSlowScript(session: GeckoSession, scriptFileName: String): GeckoResult<SlowScriptResponse>? = a.askSlowScript(tab)

    // ------------------------------------------------------------------ shields counters (engine-side tracking protection)

    override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
        val c = event.antiTrackingCategory
        val category = if ((c and ContentBlocking.AntiTracking.AD) != 0) Category.ADS else Category.PRIVACY
        tab.blocked.incrementAndGet(category)
        tab.pendingLifetime.incrementAndGet()
        a.shieldCountChanged(tab)
    }

    // ------------------------------------------------------------------ prompts (JS dialogs, forms, files, pop-ups)

    override fun onAlertPrompt(session: GeckoSession, prompt: PromptDelegate.AlertPrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.jsDialog(tab, tab.url, prompt.message ?: "", null, false, { r.complete(prompt.dismiss()) }, { r.complete(prompt.dismiss()) })
        return r
    }

    override fun onButtonPrompt(session: GeckoSession, prompt: PromptDelegate.ButtonPrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.jsDialog(tab, tab.url, prompt.message ?: "", null, true,
            { r.complete(prompt.confirm(PromptDelegate.ButtonPrompt.Type.POSITIVE)) }, { r.complete(prompt.confirm(PromptDelegate.ButtonPrompt.Type.NEGATIVE)) })
        return r
    }

    override fun onTextPrompt(session: GeckoSession, prompt: PromptDelegate.TextPrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.jsDialog(tab, tab.url, prompt.message ?: "", prompt.defaultValue ?: "", true, { r.complete(prompt.confirm(it ?: "")) }, { r.complete(prompt.dismiss()) })
        return r
    }

    override fun onBeforeUnloadPrompt(session: GeckoSession, prompt: PromptDelegate.BeforeUnloadPrompt): GeckoResult<PromptDelegate.PromptResponse>? =
        GeckoResult.fromValue(prompt.confirm(allow)) // never trap the user on a page

    override fun onRepostConfirmPrompt(session: GeckoSession, prompt: PromptDelegate.RepostConfirmPrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.askYesNo("Resend form data?", "Reloading will send the information you entered again.", "Resend", "Cancel") { ok -> r.complete(prompt.confirm(if (ok) allow else deny)) }
        return r
    }

    override fun onPopupPrompt(session: GeckoSession, prompt: PromptDelegate.PopupPrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        a.toast("Pop-up blocked")
        return GeckoResult.fromValue(prompt.confirm(deny))
    }

    override fun onAuthPrompt(session: GeckoSession, prompt: PromptDelegate.AuthPrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.askAuth(UrlParts.hostOf(tab.url) ?: "this site", prompt.message ?: "", { u, p -> r.complete(prompt.confirm(u, p)) }, { r.complete(prompt.dismiss()) })
        return r
    }

    override fun onChoicePrompt(session: GeckoSession, prompt: PromptDelegate.ChoicePrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.showChoice(prompt.choices, prompt.type == PromptDelegate.ChoicePrompt.Type.MULTIPLE, { picked -> r.complete(prompt.confirm(picked)) }, { r.complete(prompt.dismiss()) })
        return r
    }

    override fun onFilePrompt(session: GeckoSession, prompt: PromptDelegate.FilePrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val r = GeckoResult<PromptDelegate.PromptResponse>()
        a.pickFiles(prompt.mimeTypes, prompt.type == PromptDelegate.FilePrompt.Type.MULTIPLE) { uris ->
            if (uris.isEmpty()) r.complete(prompt.dismiss()) else r.complete(prompt.confirm(a, uris.toTypedArray()))
        }
        return r
    }

    // ------------------------------------------------------------------ permissions

    override fun onAndroidPermissionsRequest(session: GeckoSession, permissions: Array<out String>?, callback: PermissionDelegate.Callback) {
        a.requestAndroidPermissions(permissions ?: emptyArray(), callback)
    }

    override fun onContentPermissionRequest(session: GeckoSession, perm: PermissionDelegate.ContentPermission): GeckoResult<Int>? {
        val r = GeckoResult<Int>()
        // Only location is worth asking about here. Notifications, persistent storage, autoplay, DRM, tracking/storage-access and local-network
        // requests are refused: they are bloat, data drains or privacy leaks on this device.
        if (perm.permission != PermissionDelegate.PERMISSION_GEOLOCATION) return GeckoResult.fromValue(PermissionDelegate.ContentPermission.VALUE_DENY)
        val host = UrlParts.hostOf(perm.uri) ?: perm.uri
        a.askYesNo("$host wants to know your location", "Only allow this if you trust the site.", "Allow", "Block") { ok ->
            r.complete(if (ok) PermissionDelegate.ContentPermission.VALUE_ALLOW else PermissionDelegate.ContentPermission.VALUE_DENY)
        }
        return r
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession, uri: String, video: Array<out PermissionDelegate.MediaSource>?, audio: Array<out PermissionDelegate.MediaSource>?,
        callback: PermissionDelegate.MediaCallback,
    ) {
        val what = listOfNotNull(if (!video.isNullOrEmpty()) "camera" else null, if (!audio.isNullOrEmpty()) "microphone" else null)
        if (what.isEmpty()) { callback.reject(); return }
        val host = UrlParts.hostOf(uri) ?: uri
        a.askYesNo("$host wants to use your ${what.joinToString(" and ")}", "Only allow this if you trust the site.", "Allow", "Block") { ok ->
            if (ok) callback.grant(video?.firstOrNull(), audio?.firstOrNull()) else callback.reject()
        }
    }

    private companion object {
        const val BLANK = "data:text/html;charset=utf-8,%3Chtml%3E%3Cbody%20style%3D%22background%3A%23121212%22%3E%3C%2Fbody%3E%3C%2Fhtml%3E"
        // the WebViewClient.ERROR_* values the shared ErrorView understands
        const val ERROR_HOST_LOOKUP = -2
        const val ERROR_CONNECT = -6
        const val ERROR_IO = -7
        const val ERROR_TIMEOUT = -8
        const val ERROR_REDIRECT_LOOP = -9
        const val ERROR_UNSUPPORTED_SCHEME = -10
        const val ERROR_BAD_URL = -12
        const val ERROR_UNKNOWN = -1

        fun webViewCode(code: Int): Int = when (code) {
            WebRequestError.ERROR_UNKNOWN_HOST -> ERROR_HOST_LOOKUP
            WebRequestError.ERROR_NET_TIMEOUT -> ERROR_TIMEOUT
            WebRequestError.ERROR_CONNECTION_REFUSED, WebRequestError.ERROR_PORT_BLOCKED -> ERROR_CONNECT
            WebRequestError.ERROR_NET_INTERRUPT, WebRequestError.ERROR_NET_RESET, WebRequestError.ERROR_OFFLINE -> ERROR_IO
            WebRequestError.ERROR_REDIRECT_LOOP -> ERROR_REDIRECT_LOOP
            WebRequestError.ERROR_UNKNOWN_PROTOCOL -> ERROR_UNSUPPORTED_SCHEME
            WebRequestError.ERROR_MALFORMED_URI -> ERROR_BAD_URL
            else -> ERROR_UNKNOWN
        }
    }
}
