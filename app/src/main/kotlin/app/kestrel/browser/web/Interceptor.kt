package app.kestrel.browser.web

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.Shields
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.RequestType
import app.kestrel.core.filter.Verdict
import app.kestrel.core.url.UrlParts

/**
 * The network side of Shields. Runs on WebView IO threads for EVERY request, so it is allocation-light and never throws.
 *  - main frame: learns the page host, resolves the per-site Shields, checks popup rules and dangerous-site (malware) rules;
 *  - subresources: asks the filter engine; blocked requests get a neutered response and are counted.
 */
class Interceptor(private val host: TabHost, private val prefs: Prefs) {

    fun intercept(tab: Tab, req: WebResourceRequest): WebResourceResponse? {
        val uri = req.url
        val scheme = uri.scheme
        if (scheme != "http" && scheme != "https") return null
        val url = uri.toString()
        val headers = req.requestHeaders

        if (req.isForMainFrame) return mainFrame(tab, url)

        val s = tab.shields
        val saver = prefs.dataSaver
        val accept = headers?.get("Accept") ?: headers?.get("accept")
        val type = RequestType.infer(
            isMainFrame = false,
            secFetchDest = headers?.get("Sec-Fetch-Dest"),
            accept = accept,
            path = uri.path ?: "",
            requestedWith = headers?.get("X-Requested-With"),
        )
        if (type == RequestType.FONT && saver.blockFonts && s.shieldsUp) return Neutered.forType(type)

        if (!s.shieldsUp || !s.blockAds || tab.pageAllowed) return null
        val v = Engine.filters().check(url, tab.topHost, type)
        if (!Verdict.isBlocked(v)) return null

        tab.blocked.incrementAndGet(Verdict.category(v))
        tab.pendingLifetime.incrementAndGet()
        if (tab.shieldDirty.compareAndSet(false, true)) {
            Io.main.postDelayed({ tab.shieldDirty.set(false); host.shieldCountChanged(tab) }, 250)
        }
        return Neutered.forType(type)
    }

    /**
     * UI-thread entry for a main-frame URL reached by a server-side redirect (see BrowserWebViewClient.shouldOverrideUrlLoading).
     * Refreshes the per-page state and applies the malware check to the landing host. True = do not load it.
     */
    fun redirectedMainFrame(tab: Tab, url: String): Boolean {
        if (Engine.peek() == null) return false // lists still loading at cold start: do not block the UI thread waiting for them
        return mainFrame(tab, url) != null
    }

    private fun mainFrame(tab: Tab, url: String): WebResourceResponse? {
        val parts = UrlParts.parse(url)
        val h = parts?.host
        tab.topHost = h
        tab.mainUrl = url
        val settings = Shields.effective(prefs, h)
        tab.shields = settings
        val fs = Engine.filters()
        tab.pageAllowed = h != null && fs.isPageAllowed(h)

        if (tab.isFreshPopup) {
            tab.isFreshPopup = false
            if (settings.shieldsUp && settings.blockAds && !tab.pageAllowed &&
                Verdict.isBlocked(fs.check(url, tab.openerHost, RequestType.POPUP))
            ) {
                Io.main.post { host.popupBlocked(tab); host.closeTab(tab) }
                return Neutered.blockedDocument()
            }
        }
        if (h != null && settings.shieldsUp && settings.blockAds && !tab.pageAllowed && h !in tab.dangerOkHosts) {
            val v = fs.check(url, null, RequestType.DOCUMENT)
            if (Verdict.isBlocked(v)) {
                val cat = Verdict.category(v)
                Io.main.post {
                    tab.error = ErrorScreen.Dangerous(url, h, cat)
                    host.errorChanged(tab)
                }
                return Neutered.blockedDocument()
            }
        }
        return null
    }
}
