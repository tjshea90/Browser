package app.kestrel.browser.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.browser.shield.Shields
import app.kestrel.core.url.HttpsPolicy
import app.kestrel.core.url.RedirectUnwrapper
import app.kestrel.core.url.UrlParts

/** Navigation, error and interception callbacks for one tab's WebView. */
class BrowserWebViewClient(
    private val host: TabHost,
    private val tab: Tab,
    private val prefs: Prefs,
    private val interceptor: Interceptor,
) : WebViewClient() {

    // ------------------------------------------------------------------ interception

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        try { interceptor.intercept(tab, request) } catch (t: Throwable) { null } // never crash a WebView thread; at worst this one request is unfiltered

    // ------------------------------------------------------------------ navigation policy

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val scheme = request.url.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            if (!request.isForMainFrame || request.method != "GET") return false
            val targetHost = UrlParts.hostOf(url)
            val s = Shields.effective(prefs, targetHost)
            var next = url
            if (s.shieldsUp && s.blockAds) RedirectUnwrapper.unwrap(next)?.let { next = it }
            val prep = NavPolicy.prepare(prefs, next, tab)
            if (prep.url != url) {
                tab.upgradedFrom = prep.upgradedFrom
                tab.upgradedTo = if (prep.upgradedFrom != null) prep.url else null
                load(view, prep.url)
                return true
            }
            return false
        }
        return host.externalLaunch(tab, url, request.hasGesture())
    }

    private fun load(view: WebView, url: String) {
        tab.shields = Shields.effective(prefs, UrlParts.hostOf(url))
        WebViewFactory.configure(view, tab, prefs)
        view.loadUrl(url)
    }

    // ------------------------------------------------------------------ page lifecycle

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        tab.url = if (url == "about:blank" && tab.url.isNotEmpty()) tab.url else url
        tab.loading = true
        tab.progress = maxOf(tab.progress, 5)
        tab.resetPageCounters()
        tab.dialogCount = 0
        tab.dialogsSuppressed = false
        tab.shields = Shields.effective(prefs, UrlParts.hostOf(url))
        WebViewFactory.configure(view, tab, prefs)
        val failing = (tab.error as? ErrorScreen.Network)?.failingUrl
        if (tab.error != null && (failing == null || failing != url)) { tab.error = null; host.errorChanged(tab) }
        host.tabChanged(tab)
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        CosmeticInjector.inject(view, tab)
    }

    override fun onPageFinished(view: WebView, url: String) {
        tab.loading = false
        tab.progress = 100
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        if (tab.cacheFirstOnce) { tab.cacheFirstOnce = false; WebViewFactory.configure(view, tab, prefs) }
        CosmeticInjector.verify(view, tab)
        host.pageFinished(tab)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        if (url != "about:blank") tab.url = url
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        host.tabChanged(tab)
    }

    // ------------------------------------------------------------------ errors

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (!request.isForMainFrame) return
        val failing = request.url.toString()
        if (httpsFallback(view, failing, error.errorCode)) return
        tab.loading = false
        tab.error = ErrorScreen.Network(failing, error.errorCode, error.description?.toString() ?: "")
        host.errorChanged(tab)
    }

    /** We rewrote http->https and https did not work: fall back (UPGRADE) or ask (STRICT). Never when the whole link is down. */
    private fun httpsFallback(view: WebView, failing: String, code: Int): Boolean {
        val up = tab.upgradedTo ?: return false
        if (!sameDocument(up, failing)) return false
        if (NetworkMonitor.state != NetworkMonitor.State.ONLINE) return false // hotspot problem, not an HTTPS problem
        val https = code == ERROR_CONNECT || code == ERROR_TIMEOUT || code == ERROR_FAILED_SSL_HANDSHAKE || code == ERROR_IO
        if (!https) return false
        return handleHttpsUnavailable(view, up)
    }

    private fun handleHttpsUnavailable(view: WebView, httpsUrl: String): Boolean {
        val httpUrl = tab.upgradedFrom ?: HttpsPolicy.downgradeOf(httpsUrl) ?: return false
        val h = UrlParts.hostOf(httpUrl) ?: return false
        tab.upgradedTo = null
        if (Shields.effective(prefs, h).https == HttpsPolicy.Mode.UPGRADE) {
            tab.httpOkHosts.add(h)
            view.loadUrl(httpUrl)
        } else {
            tab.loading = false
            tab.error = ErrorScreen.HttpsUnavailable(httpUrl, h)
            host.errorChanged(tab)
        }
        return true
    }

    // proceed() is only reached after the user explicitly chose "Proceed (unsafe)" for this host on the native warning screen.
    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val url = error.url ?: ""
        val h = UrlParts.hostOf(url) ?: ""
        if (h in tab.sslBypassHosts) { handler.proceed(); return }
        handler.cancel()
        val main = tab.mainUrl
        if (main == null || !sameDocument(main, url)) return // a broken subresource: just don't load it
        val up = tab.upgradedTo
        if (up != null && sameDocument(up, url) && handleHttpsUnavailable(view, up)) return
        tab.loading = false
        tab.error = ErrorScreen.Ssl(url, h, sslSummary(error.primaryError), error.certificate?.issuedBy?.cName ?: "")
        host.errorChanged(tab)
    }

    private fun sslSummary(primary: Int) = when (primary) {
        SslError.SSL_EXPIRED -> "The site's security certificate has expired."
        SslError.SSL_NOTYETVALID -> "The site's security certificate is not valid yet (check this tablet's date and time)."
        SslError.SSL_IDMISMATCH -> "The certificate was issued for a different website name."
        SslError.SSL_UNTRUSTED -> "The certificate was not issued by a trusted authority."
        SslError.SSL_DATE_INVALID -> "The certificate's dates are invalid (check this tablet's date and time)."
        else -> "The site's security certificate is invalid."
    }

    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, hostName: String, realm: String) {
        host.httpAuth(tab, handler, hostName, realm)
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        host.confirmResubmit(resend, dontResend)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        host.rendererGone(tab, view, detail.didCrash())
        return true // handled: do NOT let the whole app die with the renderer (common on a low-RAM device)
    }

    /** Same page ignoring a trailing slash difference and fragment. */
    private fun sameDocument(a: String, b: String): Boolean {
        fun norm(u: String) = u.substringBefore('#').trimEnd('/')
        return norm(a).equals(norm(b), ignoreCase = true)
    }
}
