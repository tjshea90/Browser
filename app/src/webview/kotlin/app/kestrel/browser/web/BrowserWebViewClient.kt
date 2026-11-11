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
                // A server that answers https://x/ with a redirect back to http://x/ would loop forever (upgrade -> redirect -> upgrade ...):
                // seeing our own upgraded target bounce back means that site has no usable HTTPS.
                val up = tab.upgradedTo
                if (request.isRedirect && up != null && prep.upgradedFrom != null && sameHostAndPort(up, url) && handleHttpsUnavailable(view, up)) return true
                tab.upgradedFrom = prep.upgradedFrom
                tab.upgradedTo = if (prep.upgradedFrom != null) prep.url else null
                load(view, prep.url)
                return true
            }
            // shouldInterceptRequest is NOT called for server-side redirect targets, so the landing page of a link shortener / tracking
            // redirector / "example.com -> www.other.com" never had its host, Shields or malware verdict looked at. Do it here.
            if (request.isRedirect && interceptor.redirectedMainFrame(tab, url)) return true
            return false
        }
        return try { host.externalLaunch(tab, url, request.hasGesture()) } catch (e: Exception) { true } // never let a page's link crash the UI thread
    }

    private fun sameHostAndPort(a: String, b: String): Boolean {
        val pa = UrlParts.parse(a) ?: return false
        val pb = UrlParts.parse(b) ?: return false
        return pa.host == pb.host
    }

    private fun load(view: WebView, url: String) {
        tab.shields = Shields.effective(prefs, UrlParts.hostOf(url))
        WebViewFactory.configure(view, tab, prefs)
        view.loadUrl(url)
    }

    // ------------------------------------------------------------------ page lifecycle

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        // The address bar shows only COMMITTED pages (doUpdateVisitedHistory below). A page-initiated navigation that never commits
        // (black-holed port, endless load) must not make the bar claim we are already on a lookalike address. Navigations the user
        // starts set tab.url themselves before loading.
        if (tab.url.isEmpty() && url != "about:blank") tab.url = url
        tab.loading = true
        tab.injectToken = 0 // new page load -> new marker token
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
        // The upgraded page is up. A later hiccup (flaky hotspot on Reload) is a network problem, not proof that the site has no HTTPS.
        if (tab.error == null) { tab.upgradedFrom = null; tab.upgradedTo = null }
        CosmeticInjector.inject(view, tab)
        ScriptInjector.inject(view, tab, prefs)
    }

    override fun onPageFinished(view: WebView, url: String) {
        tab.loading = false
        tab.progress = 100
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        if (tab.cacheFirstOnce || tab.savedCopyOnce) { tab.cacheFirstOnce = false; tab.savedCopyOnce = false; WebViewFactory.configure(view, tab, prefs) }
        CosmeticInjector.verify(view, tab)
        ScriptInjector.verify(view, tab, prefs)
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
        mainFrameFailed(view, request.url.toString(), error.errorCode, error.description?.toString() ?: "")
    }

    /** The page itself (not a subresource) failed to load. Split out of [onReceivedError] because WebResourceError cannot be built in a JVM test. */
    internal fun mainFrameFailed(view: WebView, failing: String, code: Int, description: String) {
        if (tab.savedCopyOnce) { // "Open saved copy" found nothing in the cache: back to normal loading, keep the error page up
            tab.savedCopyOnce = false
            WebViewFactory.configure(view, tab, prefs)
            host.toast("No saved copy of this page")
        } else if (httpsFallback(view, failing, code)) return
        tab.loading = false
        tab.error = ErrorScreen.Network(failing, code, description)
        host.errorChanged(tab)
    }

    /** We rewrote http->https and https did not work: fall back (UPGRADE) or ask (STRICT). Never when the whole link is down. */
    private fun httpsFallback(view: WebView, failing: String, code: Int): Boolean {
        val up = tab.upgradedTo ?: return false
        if (!sameDocument(up, failing)) return false
        if (NetworkMonitor.state == NetworkMonitor.State.OFFLINE) return false // no link at all: not an HTTPS problem
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
        SslError.SSL_EXPIRED -> "The site's security certificate has expired. If many sites say this, the tablet's date and time are probably wrong."
        SslError.SSL_NOTYETVALID -> "The site's security certificate is not valid yet (check this tablet's date and time)."
        SslError.SSL_IDMISMATCH -> "The certificate was issued for a different website name."
        // This tablet gets no system updates, so its list of trusted certificate authorities stops at its last update.
        SslError.SSL_UNTRUSTED -> "The certificate was not issued by an authority this tablet trusts. On a hotspot this can mean someone is intercepting the connection. The tablet's list of trusted authorities is also old, so a brand-new site can fail this way, but do not continue on any page where you sign in or pay."
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
