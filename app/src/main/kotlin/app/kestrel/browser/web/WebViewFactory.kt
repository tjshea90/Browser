package app.kestrel.browser.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import app.kestrel.browser.Proc
import app.kestrel.browser.R
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.core.url.UserAgents

/** Private-window WebView: asks the keyboard not to learn from anything typed into pages. */
private class PrivateWebView(ctx: Context) : WebView(ctx) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        return ic
    }
}

/** Builds hardened WebViews. Static hardening happens once in [create]; prefs/network-dependent settings in [configure]. */
object WebViewFactory {
    @Volatile private var defaultUa: String? = null

    fun userAgent(ctx: Context, desktop: Boolean): String {
        val d = defaultUa ?: WebSettings.getDefaultUserAgent(ctx).also { defaultUa = it }
        return if (desktop) UserAgents.desktop(d) else UserAgents.mobile(d)
    }

    @SuppressLint("SetJavaScriptEnabled") // a browser must run page scripts; the user can turn them off per site (Shields)
    fun create(host: TabHost, tab: Tab, prefs: Prefs, interceptor: Interceptor): WebView {
        val ctx = host.activity
        val wv: WebView = if (Proc.isPrivate) PrivateWebView(ctx) else WebView(ctx)
        wv.setBackgroundColor(ctx.getColor(R.color.window_bg))
        wv.overScrollMode = View.OVER_SCROLL_NEVER
        wv.isLongClickable = true

        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        // --- hardening: a web page must never reach local files, content providers, or insecure subresources
        s.allowFileAccess = false
        s.allowContentAccess = false
        @Suppress("DEPRECATION") run {
            s.allowFileAccessFromFileURLs = false
            s.allowUniversalAccessFromFileURLs = false
            s.setSaveFormData(false)
        }
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.safeBrowsingEnabled = true
        s.setGeolocationEnabled(true) // gated by our own per-request prompt
        // --- popups: window.open only with a user gesture; we receive those in onCreateWindow
        s.javaScriptCanOpenWindowsAutomatically = false
        s.setSupportMultipleWindows(true)
        // --- data frugality
        s.mediaPlaybackRequiresUserGesture = true // no autoplay (video preloads are the biggest silent data drain)
        // --- rendering
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false

        // Android 8+: renderer is IMPORTANT while this WebView is visible and WAIVED (first to be killed) when hidden:
        // exactly the right trade-off on a low-RAM Go device.
        wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)

        wv.webViewClient = BrowserWebViewClient(host, tab, prefs, interceptor)
        wv.webChromeClient = BrowserChromeClient(host, tab)
        wv.setDownloadListener { url, ua, cd, mime, len -> host.download(tab, url, ua, cd, mime, len) }
        wv.setOnLongClickListener { host.hitTestLongPress(tab, wv) }

        configure(wv, tab, prefs)
        return wv
    }

    /** Settings that depend on preferences, the page's Shields, and connectivity. Cheap; safe to call any time on the UI thread. */
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(wv: WebView, tab: Tab, prefs: Prefs) {
        val ctx = wv.context
        val s = wv.settings
        val saver = prefs.dataSaver
        val shields = tab.shields
        s.userAgentString = userAgent(ctx, tab.desktop)
        s.textZoom = prefs.textZoom
        s.javaScriptEnabled = !shields.blockScripts
        val blockImages = saver.blockImages && !tab.imagesAllowedOnce
        s.blockNetworkImage = blockImages
        s.loadsImagesAutomatically = !blockImages
        // Offline / hotspot without internet: serve from cache even if stale, so already-visited pages still open.
        s.cacheMode = if (NetworkMonitor.state == NetworkMonitor.State.ONLINE && !tab.cacheFirstOnce) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !shields.blockThirdPartyCookies)
        applyDark(wv, prefs, ctx)
    }

    @Suppress("DEPRECATION")
    private fun applyDark(wv: WebView, prefs: Prefs, ctx: Context) {
        val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val mode = prefs.webDark // 0 auto, 1 force dark, 2 never
        if (Build.VERSION.SDK_INT >= 33) {
            wv.settings.isAlgorithmicDarkeningAllowed = mode == 1 || (mode == 0 && night)
        } else {
            wv.settings.forceDark = when (mode) {
                1 -> WebSettings.FORCE_DARK_ON
                2 -> WebSettings.FORCE_DARK_OFF
                else -> if (night) WebSettings.FORCE_DARK_AUTO else WebSettings.FORCE_DARK_OFF
            }
        }
    }

    /** Fully release a WebView (must be detached first). */
    fun destroy(wv: WebView) {
        wv.stopLoading()
        wv.webViewClient = android.webkit.WebViewClient()
        wv.webChromeClient = WebChromeClientStub
        wv.setDownloadListener(null)
        wv.setOnLongClickListener(null)
        wv.loadUrl("about:blank")
        wv.removeAllViews()
        wv.destroy()
    }

    private object WebChromeClientStub : android.webkit.WebChromeClient()
}
