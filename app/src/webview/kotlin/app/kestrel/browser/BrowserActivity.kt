package app.kestrel.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Parcel
import android.os.Process
import android.os.SystemClock
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.HttpAuthHandler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.DataMeter
import app.kestrel.browser.data.Entry
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.net.Downloads
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.FilterUpdater
import app.kestrel.browser.shield.UpdateJob
import app.kestrel.browser.shield.Shields
import app.kestrel.browser.ui.ErrorView
import app.kestrel.browser.ui.FindBar
import app.kestrel.browser.ui.ListPanel
import app.kestrel.browser.ui.MenuItemSpec
import app.kestrel.browser.ui.MenuPopup
import app.kestrel.browser.ui.NewTabPage
import app.kestrel.browser.ui.Pal
import app.kestrel.browser.ui.ProgressLine
import app.kestrel.browser.ui.SettingsListener
import app.kestrel.browser.ui.SettingsView
import app.kestrel.browser.ui.ShieldsPanel
import app.kestrel.browser.ui.SuggestionList
import app.kestrel.browser.ui.TabSwitcher
import app.kestrel.browser.ui.Toolbar
import app.kestrel.browser.ui.ToolbarListener
import app.kestrel.browser.ui.TopSite
import app.kestrel.browser.ui.dp
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.browser.web.ExternalIntents
import app.kestrel.browser.web.Interceptor
import app.kestrel.browser.web.NavPolicy
import app.kestrel.browser.web.Tab
import app.kestrel.browser.web.TabHost
import app.kestrel.browser.web.WebViewFactory
import app.kestrel.core.session.Session
import app.kestrel.core.session.SessionCodec
import app.kestrel.core.session.TabRecord
import app.kestrel.core.url.Nav
import app.kestrel.core.url.Omnibox
import app.kestrel.core.url.UrlParts
import java.io.File

/**
 * The browser window: owns the tab list, swaps what is shown (new-tab page / WebView / error screen), and implements everything the
 * per-tab WebView clients need from the UI. [MainActivity] is the normal window; [PrivateActivity] runs in its own process.
 */
open class BrowserActivity : Activity(), TabHost, ToolbarListener {
    override val activity: Activity get() = this

    private val isPrivate get() = Proc.isPrivate
    private lateinit var prefs: Prefs
    private lateinit var pal: Pal
    private lateinit var root: FrameLayout
    private lateinit var toolbar: Toolbar
    private lateinit var progress: ProgressLine
    private lateinit var content: FrameLayout
    private lateinit var ntp: NewTabPage
    private lateinit var errorView: ErrorView
    private lateinit var findBar: FindBar
    private lateinit var suggestions: SuggestionList
    private lateinit var tabSwitcher: TabSwitcher
    private lateinit var fullscreen: FrameLayout
    private var settingsView: SettingsView? = null
    private var listPanel: ListPanel? = null

    private val tabs = ArrayList<Tab>()
    private var current: Tab? = null

    /** For tests (same module): the selected tab. */
    internal val currentTab: Tab? get() = current
    internal val allTabs: List<Tab> get() = tabs
    private var nextTabId = 1L
    private var db: BrowserDb? = null
    private val interceptor by lazy { Interceptor(this, prefs) }
    private val shieldsPanel by lazy { ShieldsPanel(this, pal) { host -> onShieldsChanged(host) } }

    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var pendingFiles: ValueCallback<Array<Uri>>? = null
    private var pendingWeb: Pair<PermissionRequest, Array<String>>? = null
    private var pendingGeo: Pair<String, GeolocationPermissions.Callback>? = null
    private var backCallback: Any? = null
    private var suggestSeq = 0
    @Volatile internal var sessionRestored = false // false until restore() ran: saving earlier could replace the saved session with a one-tab placeholder

    private val netListener: () -> Unit = { onNetworkChanged() }
    private val saveRunnable = Runnable { saveSessionNow() }

    // ================================================================== lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.window_bg) // the cold-start window showed the shield; from here on it is plain black behind every page and panel
        prefs = Prefs.get(this)
        Shields.init(this)
        pal = Pal(this, isPrivate)
        if (isPrivate) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        @Suppress("DEPRECATION") run { // no-ops on Android 15+ (edge-to-edge); needed for the private window's colored bars on older versions
            window.statusBarColor = pal.chromeBg
            window.navigationBarColor = pal.chromeBg
        }
        buildUi()
        installBackHandling()
        NetworkMonitor.addListener(netListener)
        if (!isPrivate) db = BrowserDb(this)
        val firstTab = Tab(nextTabId++)
        tabs.add(firstTab)
        show(firstTab)
        val launch = urlFromIntent(intent)
        if (isPrivate) {
            launch?.let { openInNewTabOrCurrent(it) }
        } else {
            Io.exec {
                val text = try { sessionFile().takeIf { it.isFile }?.readText() } catch (e: Exception) { null }
                val session = SessionCodec.decode(text)
                Io.ui { restore(session, savedInstanceState, launch) }
            }
            Io.exec { db?.purge(5000, 90); UpdateJob.schedule(applicationContext) } // after the session read: nothing at launch waits for this
        }
    }

    override fun onStart() {
        super.onStart()
        NetworkMonitor.start(this)
    }

    override fun onResume() {
        super.onResume()
        val t = current
        if (t != null && !t.isNewTab && t.webView == null) attachContent(t) // hibernated while we were in the background: bring it back (from cache)
        current?.webView?.onResume()
        current?.webView?.resumeTimers()
        refreshNtpIfShown()
    }

    override fun onPause() {
        super.onPause()
        flushCounters()
        Io.exec { CookieManager.getInstance().flush() }
        saveSessionNow()
    }

    override fun onStop() {
        super.onStop()
        current?.webView?.onPause()
        current?.webView?.pauseTimers() // stops background JS timers: saves battery and mobile data
        NetworkMonitor.stop()
    }

    /** Cookies, storage and HTTP cache of the private process. (App.wipePrivateData still sweeps leftovers at the next private start.) */
    private fun wipePrivateSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        clearHttpCache()
    }

    override fun onDestroy() {
        if (isPrivate && isFinishing) wipePrivateSession()
        NetworkMonitor.removeListener(netListener)
        Io.main.removeCallbacks(saveRunnable)
        for (t in tabs) t.webView?.let { detach(it); WebViewFactory.destroy(it) }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        urlFromIntent(intent)?.let { openInNewTabOrCurrent(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val t = current ?: return
        val wv = t.webView ?: return
        if (isPrivate) return
        // Only the selected tab's WebView state goes in the Bundle: the system keeps it across process death, and the
        // Binder transaction limit (~1 MB) makes saving every tab a crash risk.
        val b = Bundle()
        wv.saveState(b)
        val p = Parcel.obtain()
        val size = try { b.writeToParcel(p, 0); p.dataSize() } finally { p.recycle() }
        if (size in 1..200_000) outState.putBundle("ws_${t.id}", b)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 10/15 = RUNNING_LOW/CRITICAL (we are visible); 20 = UI_HIDDEN (user left); 60+ = we are a prime kill candidate.
        if (level >= 10) { for (t in tabs.toList()) if (t !== current && !hasLiveChild(t)) hibernate(t) } // a popup's opener must survive (window.opener)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE && !window.decorView.hasWindowFocus()) current?.let { hibernate(it) }
    }

    // ================================================================== UI construction

    private fun buildUi() {
        root = FrameLayout(this)
        // Private window: no autofill service may see what is typed here or offer to save a password for it.
        if (isPrivate) root.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        root.setBackgroundColor(pal.windowBg)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        toolbar = Toolbar(this, pal, this)
        progress = ProgressLine(this, pal.accent).apply { visibility = View.INVISIBLE }
        content = FrameLayout(this)
        column.addView(toolbar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)))
        column.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)))
        column.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        ntp = NewTabPage(this, pal, object : NewTabPage.Listener {
            override fun onSearchTap() { focusUrlBar() }
            override fun onOpen(url: String) { current?.let { navigateTo(it, url) } }
        })
        errorView = ErrorView(this, pal, errorListener)
        findBar = FindBar(this, pal, object : FindBar.Listener {
            override fun query(q: String) { current?.webView?.let { if (q.isEmpty()) it.clearMatches() else it.findAllAsync(q) } }
            override fun next(forward: Boolean) { current?.webView?.findNext(forward) }
            override fun close() { closeFind() }
        }).apply { visibility = View.GONE }
        root.addView(findBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(54) })
        suggestions = SuggestionList(this, pal) { value -> hideSuggestions(); clearUrlFocus(); current?.let { navigate(it, value) } }.apply { visibility = View.GONE }
        root.addView(suggestions, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(54) })
        tabSwitcher = TabSwitcher(this, pal, object : TabSwitcher.Listener {
            override fun select(tab: Tab) { hideTabSwitcher(); show(tab) }
            override fun close(tab: Tab) {
                val wasLast = tabs.size == 1
                doCloseTab(tab)
                if (wasLast) hideTabSwitcher() else tabSwitcher.bind(tabs, current)
            }
            override fun newTab() { hideTabSwitcher(); newTabAndShow() }
            override fun dismiss() { hideTabSwitcher() }
        }).apply { visibility = View.GONE }
        root.addView(tabSwitcher, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        fullscreen = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()); visibility = View.GONE; isClickable = true }
        root.addView(fullscreen, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)

        // Edge-to-edge safe: Android 15+ draws behind the system bars for targetSdk 35+; Android 10 reports zero insets here.
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                v.setPadding(i.left, i.top, i.right, i.bottom)
                WindowInsets.CONSUMED
            } else {
                @Suppress("DEPRECATION") v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                @Suppress("DEPRECATION") insets.consumeSystemWindowInsets()
            }
        }
    }

    // ================================================================== tabs

    private fun restore(session: Session?, saved: Bundle?, launch: String?) {
        sessionRestored = true // from now on saving cannot clobber the saved session
        if (session != null && session.tabs.isNotEmpty()) {
            // Replace the placeholder tab. Nothing is loaded here except the selected tab (lazy: no network at startup).
            for (t in tabs.toList()) t.webView?.let { detach(it); WebViewFactory.destroy(it) }
            tabs.clear()
            for (r in session.tabs) {
                val t = Tab(r.id)
                t.url = if (r.url == "about:blank") "" else r.url
                t.title = r.title
                t.parentId = r.parentId
                t.desktop = r.desktopSite
                t.lastActive = r.lastActiveMillis
                if (t.url.isNotEmpty()) t.pendingLoad = t.url
                tabs.add(t)
                nextTabId = maxOf(nextTabId, r.id + 1)
            }
            val sel = tabs.firstOrNull { it.id == session.selectedId } ?: tabs.last()
            saved?.getBundle("ws_${sel.id}")?.let { sel.savedState = it }
            show(sel)
        }
        if (launch != null) openInNewTabOrCurrent(launch)
    }

    private fun newTabInternal(parent: Tab? = null): Tab {
        val t = Tab(nextTabId++)
        t.parentId = parent?.id ?: 0L
        tabs.add(t)
        return t
    }

    internal fun newTabAndShow(): Tab { val t = newTabInternal(); show(t); focusUrlBar(); return t }

    private fun openInNewTabOrCurrent(url: String) {
        val cur = current
        val t = if (cur != null && cur.isNewTab) cur else newTabInternal()
        show(t)
        navigateTo(t, url)
    }

    private fun show(tab: Tab) {
        val prev = current
        if (prev != null && prev !== tab) { prev.webView?.let { it.onPause(); detach(it) } }
        current = tab
        tab.lastActive = System.currentTimeMillis()
        attachContent(tab)
        tab.webView?.onResume()
        enforceLiveLimit()
        updateUi()
        scheduleSave()
    }

    private fun attachContent(tab: Tab) {
        content.removeAllViews()
        if (tab.isNewTab) {
            content.addView(ntp, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            refreshNtp()
        } else {
            val wv = ensureWebView(tab)
            detach(wv)
            content.addView(wv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val e = tab.error
        if (e != null) {
            errorView.show(e)
            content.addView(errorView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        findBar.visibility = if (tab.findOpen) View.VISIBLE else View.GONE
    }

    private fun ensureWebView(tab: Tab): WebView {
        tab.webView?.let { return it }
        val wv = WebViewFactory.create(this, tab, prefs, interceptor)
        tab.webView = wv
        val state = tab.savedState
        val pending = tab.pendingLoad
        when {
            state != null -> {
                tab.savedState = null
                tab.savedCopyOnce = false
                tab.cacheFirstOnce = true // restoring costs no data: prefer the HTTP cache
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(tab.url))
                WebViewFactory.configure(wv, tab, prefs)
                if (wv.restoreState(state) == null && tab.url.isNotEmpty()) wv.loadUrl(tab.url)
            }
            pending != null -> {
                tab.pendingLoad = null
                tab.savedCopyOnce = false
                tab.cacheFirstOnce = true
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(pending))
                tab.topHost = UrlParts.hostOf(pending)
                WebViewFactory.configure(wv, tab, prefs)
                wv.loadUrl(pending)
            }
        }
        enforceLiveLimit() // WebViews are also created by the omnibox pre-warm and by navigating from a new tab, not only by show()
        return wv
    }

    private fun hasLiveChild(t: Tab) = tabs.any { it.parentId == t.id && it.webView != null }

    private fun detach(v: View) { (v.parent as? ViewGroup)?.removeView(v) }

    /** Low-RAM policy: at most 2 live WebViews (4 on bigger devices); the least recently used others are hibernated. */
    private fun enforceLiveLimit() {
        val low = getSystemService(ActivityManager::class.java)?.isLowRamDevice != false
        val max = if (low) 2 else 4
        val live = tabs.filter { it.webView != null }.sortedBy { it.lastActive }
        var count = live.size
        for (t in live) {
            if (count <= max) break
            if (t === current || hasLiveChild(t)) continue
            hibernate(t)
            count--
        }
    }

    /** Free a tab's WebView but keep everything needed to bring it back (state bundle, or at least its URL). */
    private fun hibernate(tab: Tab) {
        val wv = tab.webView ?: return
        if (tab.isNewTab && tab.url.isEmpty() && wv.url == null) { // prewarmed spare: nothing worth keeping
            detach(wv); WebViewFactory.destroy(wv); tab.webView = null; return
        }
        val b = Bundle()
        wv.saveState(b)
        tab.savedState = b
        tab.cacheFirstOnce = true
        detach(wv)
        WebViewFactory.destroy(wv)
        tab.webView = null
    }

    override fun newPopupTab(opener: Tab): Tab {
        val t = newTabInternal(opener)
        t.url = "about:blank"
        t.isFreshPopup = true
        t.openerHost = opener.topHost
        t.webView = WebViewFactory.create(this, t, prefs, interceptor)
        show(t)
        return t
    }

    override fun closeTab(tab: Tab) { Io.main.post { doCloseTab(tab) } }

    private fun doCloseTab(tab: Tab) {
        val idx = tabs.indexOf(tab)
        if (idx < 0) return
        tabs.removeAt(idx)
        tab.webView?.let { detach(it); WebViewFactory.destroy(it) }
        tab.webView = null
        if (current === tab) {
            val next = tabs.firstOrNull { it.id == tab.parentId } ?: tabs.getOrNull(idx) ?: tabs.getOrNull(idx - 1)
            if (next == null) show(newTabInternal()) else show(next)
        } else updateUi()
        scheduleSave()
    }

    private fun showNewTabPage(tab: Tab) {
        tab.webView?.let { detach(it); WebViewFactory.destroy(it) }
        tab.webView = null
        tab.url = ""; tab.title = ""; tab.error = null; tab.pendingLoad = null; tab.savedState = null; tab.loading = false; tab.progress = 0
        if (current === tab) { attachContent(tab); updateUi() }
        scheduleSave()
    }

    // ================================================================== navigation

    private fun navigate(tab: Tab, input: String) {
        when (val r = Omnibox.resolve(input, prefs.searchTemplate)) {
            is Nav.Go -> if (r.url == "about:blank") showNewTabPage(tab) else navigateTo(tab, r.url)
            is Nav.Search -> navigateTo(tab, r.url)
            is Nav.Rejected -> toast(r.reason)
            Nav.Empty -> {}
        }
    }

    private fun navigateTo(tab: Tab, url: String, savedCopy: Boolean = false) {
        // Single choke point for every way a URL reaches a tab (omnibox, links, menus, intents, session): only web pages. Long-press menus feed
        // page-controlled text (javascript:, data:, file:///android_asset/...) in here, which the omnibox allow-list never saw.
        if (!NavPolicy.isWebUrl(url)) { toast("Can't open that kind of address"); return }
        val prep = NavPolicy.prepare(prefs, url, tab)
        loadUrl(tab, prep.url, prep.upgradedFrom, savedCopy)
    }

    /** [savedCopy]: load from the HTTP cache only (the flag is per navigation, so any other navigation clears it). */
    private fun loadUrl(tab: Tab, url: String, upgradedFrom: String?, savedCopy: Boolean = false) {
        tab.savedCopyOnce = savedCopy
        tab.error = null
        tab.pendingLoad = null
        tab.savedState = null
        tab.upgradedFrom = upgradedFrom
        tab.upgradedTo = if (upgradedFrom != null) url else null
        tab.url = url
        tab.title = ""
        tab.favicon = null
        tab.loading = true
        tab.progress = 5
        tab.imagesAllowedOnce = false
        val host = UrlParts.hostOf(url)
        tab.topHost = host
        tab.shields = Shields.effective(prefs, host)
        tab.resetPageCounters()
        val wv = ensureWebView(tab)
        if (current === tab) attachContent(tab)
        WebViewFactory.configure(wv, tab, prefs)
        wv.loadUrl(url)
        updateUi()
    }

    private fun retry(tab: Tab) {
        val target = when (val e = tab.error) {
            is ErrorScreen.Network -> e.failingUrl
            is ErrorScreen.Crashed -> e.url
            is ErrorScreen.HttpsUnavailable -> e.httpUrl
            is ErrorScreen.Dangerous -> e.url
            is ErrorScreen.Ssl -> e.url
            null -> tab.url
        }
        tab.error = null
        tab.cacheFirstOnce = false
        if (target.isNotEmpty()) navigateTo(tab, target) // clears savedCopyOnce via loadUrl
    }

    private fun urlFromIntent(i: Intent?): String? = try { urlFromIntentUnchecked(i) } catch (e: Exception) { null } // exported activity: a hostile extra (bad Parcelable) must not crash it

    private fun urlFromIntentUnchecked(i: Intent?): String? {
        i ?: return null
        return when (i.action) {
            Intent.ACTION_VIEW -> i.dataString?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            Intent.ACTION_WEB_SEARCH -> i.getStringExtra(SearchManager.QUERY)?.takeIf { it.isNotBlank() }?.let { Omnibox.searchUrl(it, prefs.searchTemplate) }
            Intent.ACTION_SEND -> i.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                when (val r = Omnibox.resolve(text.trim().take(2000), prefs.searchTemplate)) { is Nav.Go -> r.url; is Nav.Search -> r.url; else -> null }
            }
            else -> null
        }?.takeIf { it != "about:blank" }
    }

    // ================================================================== toolbar callbacks

    override fun onBack() { if (!handleBack()) moveTaskToBack(true) }
    override fun onForward() { current?.webView?.goForward() }

    override fun onReloadOrStop() {
        val t = current ?: return
        if (t.error != null) { retry(t); return }
        val wv = t.webView ?: return
        if (t.loading) { wv.stopLoading(); t.loading = false; t.progress = 0; updateUi() } else {
            t.cacheFirstOnce = false
            WebViewFactory.configure(wv, t, prefs)
            wv.reload()
        }
    }

    override fun onShields(anchor: View) {
        val t = current ?: return
        val host = t.topHost ?: UrlParts.hostOf(t.url) ?: return
        shieldsPanel.show(anchor, host, t, prefs.globalShields())
    }

    private fun onShieldsChanged(host: String) {
        for (t in tabs) {
            if ((t.topHost ?: UrlParts.hostOf(t.url)) == host) {
                t.shields = Shields.effective(prefs, host)
                t.webView?.let { wv -> WebViewFactory.configure(wv, t, prefs); if (t === current) wv.reload() }
            }
        }
        updateUi()
    }

    override fun onTabs() { hideKeyboard(); tabSwitcher.bind(tabs, current); tabSwitcher.visibility = View.VISIBLE }
    private fun hideTabSwitcher() { tabSwitcher.visibility = View.GONE }

    override fun onSubmit(text: String) {
        hideSuggestions(); clearUrlFocus()
        current?.let { navigate(it, text) }
    }

    override fun onUrlFocus(focused: Boolean) {
        if (focused) {
            toolbar.urlField.setText(toolbar.fullUrlForEditing(current))
            toolbar.urlField.selectAll()
            val t = current
            if (t != null && t.isNewTab && t.webView == null) Io.main.post { if (current === t && t.isNewTab && t.webView == null) ensureWebView(t) } // prewarm: first load starts ~200 ms sooner
        } else {
            hideSuggestions()
            updateUi()
        }
    }

    override fun onUrlText(text: String) {
        val q = text.trim()
        if (q.isEmpty() || q == current?.url) { hideSuggestions(); return }
        val seq = ++suggestSeq
        val dbRef = db
        if (dbRef == null) { suggestions.bind(q, emptyList()); suggestions.visibility = View.VISIBLE; return }
        Io.exec {
            val entries = dbRef.suggest(q, 6)
            Io.ui { if (seq == suggestSeq && toolbar.urlField.hasFocus()) { suggestions.bind(q, entries); suggestions.visibility = View.VISIBLE } }
        }
    }

    private fun hideSuggestions() { suggestSeq++; suggestions.visibility = View.GONE }
    private fun focusUrlBar() { toolbar.urlField.requestFocus(); getSystemService(InputMethodManager::class.java)?.showSoftInput(toolbar.urlField, 0) }
    private fun clearUrlFocus() { toolbar.urlField.clearFocus(); hideKeyboard() }
    private fun hideKeyboard() { getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(root.windowToken, 0) }

    // ---- menu
    private object M { const val NEW = 1; const val PRIVATE = 2; const val FIND = 3; const val SHARE = 4; const val BOOKMARK = 5; const val BOOKMARKS = 6; const val HISTORY = 7
        const val DOWNLOADS = 8; const val DESKTOP = 9; const val IMAGES = 10; const val PRINT = 11; const val SETTINGS = 12; const val QUIT = 13; const val CLOSE_ALL = 14 }

    override fun onMenu(anchor: View) {
        val t = current
        val items = ArrayList<MenuItemSpec>()
        items.add(MenuItemSpec(M.NEW, "New tab"))
        if (!isPrivate) items.add(MenuItemSpec(M.PRIVATE, "New private window"))
        if (t != null && !t.isNewTab) {
            items.add(MenuItemSpec(M.FIND, "Find in page"))
            items.add(MenuItemSpec(M.SHARE, "Share…"))
            if (!isPrivate) items.add(MenuItemSpec(M.BOOKMARK, "Add bookmark"))
            items.add(MenuItemSpec(M.DESKTOP, "Desktop site", t.desktop))
            if (prefs.dataSaver.blockImages) items.add(MenuItemSpec(M.IMAGES, "Load images on this page"))
            items.add(MenuItemSpec(M.PRINT, "Print / save as PDF"))
        }
        if (!isPrivate) { items.add(MenuItemSpec(M.BOOKMARKS, "Bookmarks")); items.add(MenuItemSpec(M.HISTORY, "History")) }
        items.add(MenuItemSpec(M.DOWNLOADS, "Downloads"))
        if (!isPrivate) items.add(MenuItemSpec(M.SETTINGS, "Settings"))
        if (tabs.size > 1) items.add(MenuItemSpec(M.CLOSE_ALL, "Close all tabs"))
        items.add(MenuItemSpec(M.QUIT, if (isPrivate) "Close private window" else "Quit"))
        MenuPopup.show(this, pal, anchor, items) { id -> onMenuPick(id) }
    }

    private fun onMenuPick(id: Int) {
        val t = current
        when (id) {
            M.NEW -> newTabAndShow()
            M.PRIVATE -> startActivity(Intent(this, PrivateActivity::class.java))
            M.FIND -> openFind()
            M.SHARE -> t?.takeIf { !it.isNewTab }?.let { share(it.url) }
            M.BOOKMARK -> t?.takeIf { it.url.isNotEmpty() }?.let { tab ->
                Io.exec { db?.let { d -> if (d.isBookmarked(tab.url)) { d.removeBookmark(tab.url); Io.ui { toast("Bookmark removed") } } else { d.addBookmark(tab.url, tab.title); Io.ui { toast("Bookmark added") } } } }
            }
            M.BOOKMARKS -> showList(true)
            M.HISTORY -> showList(false)
            M.DOWNLOADS -> try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { toast("No downloads app found") }
            M.DESKTOP -> t?.let { tab -> tab.desktop = !tab.desktop; tab.webView?.let { wv -> WebViewFactory.configure(wv, tab, prefs); wv.reload() } }
            M.IMAGES -> t?.let { tab -> tab.imagesAllowedOnce = true; tab.webView?.let { wv -> WebViewFactory.configure(wv, tab, prefs); wv.reload() } }
            M.PRINT -> t?.webView?.let { wv ->
                val name = (t.title.ifBlank { "page" }).take(40)
                getSystemService(PrintManager::class.java)?.print(name, wv.createPrintDocumentAdapter(name), PrintAttributes.Builder().build())
            }
            M.SETTINGS -> showSettings()
            M.CLOSE_ALL -> { for (x in tabs.toList()) { x.webView?.let { detach(it); WebViewFactory.destroy(it) } }; tabs.clear(); current = null; show(newTabInternal()) }
            M.QUIT -> quit()
        }
    }

    private fun share(url: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(i, "Share link"))
    }

    // ================================================================== find / lists / settings panels

    private fun openFind() {
        val t = current ?: return
        t.findOpen = true
        findBar.visibility = View.VISIBLE
        findBar.input.setText("")
        findBar.input.requestFocus()
        t.webView?.setFindListener { active, total, _ -> findBar.setResult(active, total) }
        getSystemService(InputMethodManager::class.java)?.showSoftInput(findBar.input, 0)
    }

    private fun closeFind() {
        current?.let { it.findOpen = false; it.webView?.clearMatches() }
        findBar.visibility = View.GONE
        hideKeyboard()
    }

    private fun showList(bookmarks: Boolean) {
        val panel = listPanel ?: ListPanel(this, pal, object : ListPanel.Listener {
            override fun open(url: String) { closeList(); current?.let { navigateTo(it, url) } }
            override fun delete(url: String) { Io.exec { if (listIsBookmarks) db?.removeBookmark(url) else db?.deleteHistory(url); reloadList() } }
            override fun clearAll() { Io.exec { db?.clearHistory(); reloadList() } }
            override fun close() { closeList() }
        }).also { listPanel = it }
        listIsBookmarks = bookmarks
        if (panel.parent == null) root.addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        reloadList()
    }

    private var listIsBookmarks = false

    private fun reloadList() {
        val bm = listIsBookmarks
        Io.exec {
            val entries: List<Entry> = if (bm) db?.bookmarks() ?: emptyList() else db?.history(300) ?: emptyList()
            Io.ui { listPanel?.bind(if (bm) "Bookmarks" else "History", entries, !bm, if (bm) "No bookmarks yet. Use the menu → Add bookmark." else "No history.") }
        }
    }

    private fun closeList() { listPanel?.let { detach(it) } }

    private fun showSettings() {
        val v = settingsView ?: SettingsView(this, pal, prefs, settingsListener).also { settingsView = it }
        v.rebuild()
        if (v.parent == null) root.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun closeSettings() { settingsView?.let { detach(it) } }

    /** Settings > Back up & restore: what only this activity knows (its tabs and dialogs); the flow itself is shared with the other browser (ui/BackupFlow). */
    private val backupHost = object : app.kestrel.browser.ui.BackupFlow.Host {
        override fun database() = db
        override fun isPrivateWindow() = isPrivate
        override fun tabsForBackup(): List<app.kestrel.core.backup.TabRef> = tabs.filter { it.url.startsWith("http://") || it.url.startsWith("https://") }.map { app.kestrel.core.backup.TabRef(it.url, it.title) }
        override fun openTabs(refs: List<app.kestrel.core.backup.TabRef>): Int {
            // restored tabs wait like the tabs of a closed session: nothing loads until one is opened (no burst of pages over the hotspot)
            val open = tabs.map { it.url.substringBefore('#') }.toHashSet()
            var n = 0
            for (r in refs) {
                if (!open.add(r.url.substringBefore('#'))) continue
                val t = Tab(nextTabId++)
                t.url = r.url; t.title = r.title; t.pendingLoad = r.url
                tabs.add(t); n++
            }
            if (n > 0) { updateUi(); saveSessionNow() }
            return n
        }
        override fun say(message: String) { Toast.makeText(this@BrowserActivity, message, Toast.LENGTH_LONG).show() }
        override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) { askYesNo(title, message, yes, no, result) }
        override fun afterRestore() {
            for (t in tabs) { t.shields = Shields.effective(prefs, t.topHost); t.webView?.let { WebViewFactory.configure(it, t, prefs) } }
            Io.exec { UpdateJob.schedule(applicationContext) }
            settingsView?.rebuild()
            refreshNtpIfShown()
            updateUi()
        }
        override fun appName(): String = "Kestrel " + (try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" })
    }
    private val backup by lazy { app.kestrel.browser.ui.BackupFlow(this, backupHost) }

    private val settingsListener = object : SettingsListener {
        override fun closeSettings() { this@BrowserActivity.closeSettings() }
        override fun prefsChanged() {
            for (t in tabs) { t.shields = Shields.effective(prefs, t.topHost); t.webView?.let { WebViewFactory.configure(it, t, prefs) } }
            if (!isPrivate) Io.exec { UpdateJob.schedule(applicationContext) } // the update schedule may have changed
        }
        override fun clearBrowsingData() { this@BrowserActivity.clearBrowsingData() }
        override fun resetSiteShields() { Shields.clearAll(); toast("Per-site Shields reset") }
        override fun backupNow() { backup.backupNow() }
        override fun restoreFromFile() { backup.restoreFromFile() }
        override fun updateFiltersNow() {
            toast("Checking the filter lists…")
            // own thread: compiling the lists can take several seconds on this CPU and must not hold up history writes on the IO thread
            Thread({
                val r = FilterUpdater.run(applicationContext, manual = true)
                Io.ui { toast(r.message); settingsView?.rebuild() }
            }, "kestrel-update-now").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
        }
        override fun resetDataCounter() { DataMeter.reset(prefs) }
        override fun filterStatus(): String {
            val fs = Engine.peek()
            val rules = fs?.meta?.let { it.totalNetwork } ?: 0
            return "${Engine.sourceDescription}: ${"%,d".format(rules)} network rules" + (fs?.queryRuleCount?.takeIf { it > 0 }?.let { ", ${"%,d".format(it)} address-cleaning rules" } ?: "") +
                (if (prefs.lastUpdateMs > 0) "\nLast successful check: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(prefs.lastUpdateMs))}" else "") +
                (if (prefs.lastUpdateNote.isNotEmpty()) "\n${prefs.lastUpdateNote}" else "")
        }
        override fun dataUsed(): String = DataMeter.totalBytes(prefs).let { if (it < 0) "not available on this device" else NewTabPage.fmtBytes(it) }
        override fun aboutLines(): List<Pair<String, String>> = this@BrowserActivity.aboutLines()
    }

    private fun hasPlayServices(): Boolean = try { packageManager.getPackageInfo("com.google.android.gms", 0); true } catch (e: Exception) { false }

    private fun aboutLines(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        out.add("Kestrel" to "Version $ver")
        val wvp = WebView.getCurrentWebViewPackage()
        out.add("Browser engine" to (if (wvp != null) "${wvp.packageName} ${wvp.versionName}" else "No WebView installed") +
            "\nThis is the tablet's Android System WebView. Pages render as well as that engine allows, and it is only as secure as its version. " +
            "Without Google Play it only updates when you install a newer Android System WebView APK yourself (see the README, \"Updating the web engine\").")
        out.add("User agent" to WebViewFactory.userAgent(this, false))
        val am = getSystemService(ActivityManager::class.java)
        val ram = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }.totalMem
        out.add("Device" to "RAM ${"%.1f".format(ram / 1_073_741_824.0)} GB, app memory class ${am?.memoryClass} MB, low-RAM mode: ${am?.isLowRamDevice}")
        out.add("Safe Browsing" to if (hasPlayServices()) "Google Safe Browsing is available on this tablet, on top of Kestrel's built-in malware list."
            else "Google Safe Browsing needs Google Play services, which this tablet doesn't have. Protection against malware and phishing sites comes only from Kestrel's built-in list, which updates weekly.")
        out.add("Licenses" to "Filter lists: EasyList and EasyPrivacy (GPLv3+ / CC BY-SA 3.0), uBlock Origin filters (GPLv3), Fanboy Cookiemonster (CC BY 3.0 / GPLv3). Public Suffix List (MPL-2.0). Personal, non-commercial use.")
        return out
    }

    // ================================================================== data / state

    /** The HTTP cache is shared by every WebView of the process, but clearing needs ONE instance: use a live one, else a throwaway. */
    private fun clearHttpCache() {
        val live = tabs.firstNotNullOfOrNull { it.webView }
        try { if (live != null) live.clearCache(true) else WebView(this).apply { clearCache(true); destroy() } } catch (e: Exception) { /* no WebView provider at all */ }
    }

    private fun clearBrowsingData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        clearHttpCache()
        @Suppress("DEPRECATION") WebViewDatabase.getInstance(this).apply { clearHttpAuthUsernamePassword(); clearFormData() }
        Io.exec { db?.clearHistory() }
        toast("Browsing data cleared")
    }

    private fun quit() {
        if (isPrivate) wipePrivateSession()
        else if (prefs.clearOnExit) {
            CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush(); WebStorage.getInstance().deleteAllData()
            clearHttpCache()
            sessionRestored = false // nothing may re-create the tab list after this point
            Io.exec { db?.clearHistory(); sessionFile().delete() }
        } else saveSessionNow()
        finishAndRemoveTask()
        // Free the memory right away: this tablet is low-RAM and the user asked to quit. The IO queue is flushed first.
        Io.exec { Io.main.post { Process.killProcess(Process.myPid()) } }
    }

    private fun sessionFile() = File(filesDir, "session.txt")

    /** `https://user:pass@host/` -> `https://host/`: credentials in a URL must not be written to history or the session file (storage is unencrypted). */
    private fun withoutUserinfo(url: String): String {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return url
        var authEnd = schemeEnd + 3
        while (authEnd < url.length && url[authEnd] != '/' && url[authEnd] != '?' && url[authEnd] != '#') authEnd++
        val at = url.lastIndexOf('@', authEnd - 1)
        return if (at > schemeEnd + 2) url.substring(0, schemeEnd + 3) + url.substring(at + 1) else url
    }

    // CookieManager.flush() blocks and does disk I/O: never on the UI thread, and not after every page.
    private val cookieFlush = Runnable { Io.exec { CookieManager.getInstance().flush() } }
    private fun scheduleCookieFlush() { Io.main.removeCallbacks(cookieFlush); Io.main.postDelayed(cookieFlush, 3000) }

    private fun scheduleSave() {
        if (isPrivate) return
        Io.main.removeCallbacks(saveRunnable)
        Io.main.postDelayed(saveRunnable, 600)
    }

    internal fun saveSessionNow() {
        if (isPrivate || tabs.isEmpty() || !sessionRestored) return
        val sel = current?.id ?: tabs.last().id
        val records = tabs.map { TabRecord(it.id, withoutUserinfo(it.url).ifEmpty { "about:blank" }, it.title, it.lastActive, it.parentId, it.desktop) }
        val text = SessionCodec.encode(Session(records, sel))
        val f = sessionFile()
        Io.exec {
            val tmp = File(f.parentFile, "session.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }
    }

    private fun flushCounters() {
        var n = 0L
        for (t in tabs) n += t.pendingLifetime.getAndSet(0)
        if (n > 0 && !isPrivate) prefs.blockedTotal = prefs.blockedTotal + n
    }

    private fun refreshNtpIfShown() { if (current?.isNewTab == true) refreshNtp() }

    private fun refreshNtp() {
        val dbRef = db
        Io.exec {
            val sites = dbRef?.topSites(8)?.map { TopSite(it.first, it.second) } ?: emptyList()
            val data = if (isPrivate) -1L else DataMeter.totalBytes(prefs)
            Io.ui {
                ntp.bindSites(sites)
                ntp.bindStats(prefs.blockedTotal, data, Engine.sourceDescription, NetworkMonitor.metered)
                ntp.bindNotices(webViewNotices())
            }
        }
    }

    private fun webViewNotices(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val pkg = WebView.getCurrentWebViewPackage()
        val major = pkg?.versionName?.substringBefore('.')?.toIntOrNull()
        if (pkg == null) out.add("No web engine found" to "This tablet has no Android System WebView installed, so web pages cannot be shown.")
        else if (major != null && major < 100) out.add("Old web engine (version $major)" to "Many websites may not display correctly, and this engine is missing years of security fixes. This tablet has no Google Play to update it: install a newer \"Android System WebView\" APK yourself (README, \"Updating the web engine\").")
        return out
    }

    private fun onNetworkChanged() {
        for (t in tabs) t.webView?.let { WebViewFactory.configure(it, t, prefs); it.setNetworkAvailable(NetworkMonitor.state != NetworkMonitor.State.OFFLINE) }
        val t = current
        if (NetworkMonitor.state == NetworkMonitor.State.ONLINE && t?.error is ErrorScreen.Network) retry(t) // the hotspot is back: reload by itself
        else if (t?.error != null) errorView.show(t.error!!)
        refreshNtpIfShown()
    }

    // ================================================================== UI refresh

    private fun updateUi() {
        val t = current
        val canBack = t?.webView?.canGoBack() == true || (t != null && t.parentId != 0L && tabs.any { it.id == t.parentId })
        toolbar.bind(t, tabs.size, canBack || (t != null && !t.isNewTab))
        progress.progress = if (t?.loading == true) t.progress else 0
    }

    override fun tabChanged(tab: Tab) { if (tab === current) updateUi() else if (tabSwitcher.visibility == View.VISIBLE) tabSwitcher.bind(tabs, current) }
    override fun shieldCountChanged(tab: Tab) { if (tab === current) updateUi() }
    override fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    override fun popupBlocked(tab: Tab) { toast("Pop-up blocked") }

    override fun errorChanged(tab: Tab) {
        if (tab === current) { attachContent(tab); updateUi() }
    }

    override fun pageFinished(tab: Tab) {
        flushCounters()
        // A failed or blocked load is not a visit (typos and malware URLs would otherwise fill history and the top sites).
        if (!isPrivate && tab.error == null && tab.url.startsWith("http")) { val u = withoutUserinfo(tab.url); val ti = tab.title; Io.exec { db?.recordVisit(u, ti) } }
        scheduleCookieFlush()
        scheduleSave()
        if (tab === current) updateUi()
    }

    override fun rendererGone(tab: Tab, view: WebView, crashed: Boolean) {
        val url = (try { view.url } catch (e: Exception) { null }) ?: tab.url
        detach(view)
        try { view.destroy() } catch (e: Exception) { /* already unusable */ }
        if (tab.webView === view) tab.webView = null
        tab.savedState = null
        tab.pendingLoad = url.takeIf { it.startsWith("http") }
        tab.cacheFirstOnce = true
        val now = SystemClock.elapsedRealtime()
        tab.crashCount = if (now - tab.lastCrash < 15_000) tab.crashCount + 1 else 1
        tab.lastCrash = now
        if (tab === current) {
            if (tab.crashCount >= 3) { tab.error = ErrorScreen.Crashed(url); tab.pendingLoad = null }
            attachContent(tab)
            updateUi()
        }
    }

    // ================================================================== back handling (works on Android 10 and on 16's predictive back)

    private fun installBackHandling() {
        if (Build.VERSION.SDK_INT >= 33) {
            val cb = android.window.OnBackInvokedCallback { if (!handleBack()) moveTaskToBack(true) }
            backCallback = cb
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb)
        }
    }

    // Serves Android 10-12. On Android 13+ (incl. 16's predictive back) the OnBackInvokedCallback registered in installBackHandling() runs instead.
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Used below API 33; API 33+ uses OnBackInvokedCallback")
    override fun onBackPressed() { if (!handleBack()) moveTaskToBack(true) }

    /** @return true if the back press did something. */
    private fun handleBack(): Boolean {
        if (customView != null) { exitFullscreen(); return true }
        if (settingsView?.parent != null) { closeSettings(); return true }
        if (listPanel?.parent != null) { closeList(); return true }
        if (tabSwitcher.visibility == View.VISIBLE) { hideTabSwitcher(); return true }
        if (findBar.visibility == View.VISIBLE) { closeFind(); return true }
        if (suggestions.visibility == View.VISIBLE || toolbar.urlField.hasFocus()) { hideSuggestions(); clearUrlFocus(); updateUi(); return true }
        val t = current ?: return false
        val wv = t.webView
        if (t.error != null) {
            t.error = null
            if (wv != null && wv.canGoBack()) { attachContent(t); wv.goBack() } else if (t.parentId != 0L && tabs.any { it.id == t.parentId }) doCloseTab(t) else showNewTabPage(t)
            return true
        }
        if (wv != null && wv.canGoBack()) { wv.goBack(); return true }
        if (t.parentId != 0L && tabs.any { it.id == t.parentId }) { doCloseTab(t); return true }
        if (!t.isNewTab) { showNewTabPage(t); return true }
        return false
    }

    // ================================================================== error screen actions

    private val errorListener = object : ErrorView.Listener {
        override fun retry() { current?.let { this@BrowserActivity.retry(it) } }
        override fun openSavedCopy() {
            val t = current ?: return
            val url = (t.error as? ErrorScreen.Network)?.failingUrl ?: return
            navigateTo(t, url, savedCopy = true)
        }
        override fun backToSafety() {
            val t = current ?: return
            t.error = null
            val wv = t.webView
            if (wv != null && wv.canGoBack()) { attachContent(t); wv.goBack() } else showNewTabPage(t)
        }
        override fun proceedUnsafeSsl(host: String) { current?.let { it.sslBypassHosts.add(host); this@BrowserActivity.retry(it) } }
        override fun continueHttp(httpUrl: String, host: String) { current?.let { it.httpOkHosts.add(host); it.error = null; loadUrl(it, httpUrl, null) } }
        override fun proceedDangerous(url: String, host: String) { current?.let { it.dangerOkHosts.add(host); it.error = null; loadUrl(it, url, null) } }
        override fun openWifiSettings() { try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (e: Exception) { toast("Open Wi-Fi settings from the tablet's Settings app") } }
    }

    // ================================================================== fullscreen video

    override fun enterFullscreen(view: View, cb: WebChromeClient.CustomViewCallback) {
        if (customView != null) { cb.onCustomViewHidden(); return }
        customView = view
        customCallback = cb
        fullscreen.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        fullscreen.visibility = View.VISIBLE
        applyImmersive(true)
    }

    override fun exitFullscreen() {
        val v = customView ?: return
        fullscreen.removeView(v)
        fullscreen.visibility = View.GONE
        customView = null
        applyImmersive(false)
        customCallback?.onCustomViewHidden()
        customCallback = null
    }

    private fun applyImmersive(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 30) {
            val c = window.insetsController ?: return
            if (on) { c.hide(WindowInsets.Type.systemBars()); c.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } else c.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            // Read-modify-write: assigning a whole new value would also clear the light status/navigation bar flags the theme set (icons invisible afterwards).
            val immersive = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            val dv = window.decorView
            dv.systemUiVisibility = if (on) dv.systemUiVisibility or immersive else dv.systemUiVisibility and immersive.inv()
        }
    }

    // ================================================================== files, permissions

    override fun chooseFiles(cb: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        pendingFiles?.onReceiveValue(null)
        pendingFiles = cb
        return try { startActivityForResult(params.createIntent(), REQ_FILE); true } catch (e: Exception) { pendingFiles = null; false }
    }

    @Deprecated("framework callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (backup.onResult(requestCode, resultCode, data)) return
        if (requestCode == REQ_FILE) {
            // Only content:// results are acceptable. The picker is another app: a rogue one could answer with file:///data/data/<this app>/...
            // and the page would then upload the browser's own private files.
            val picked = WebChromeClient.FileChooserParams.parseResult(resultCode, data)?.filter { it.scheme == "content" }?.toTypedArray()
            pendingFiles?.onReceiveValue(picked?.takeIf { it.isNotEmpty() })
            pendingFiles = null
        }
    }

    override fun webPermission(tab: Tab, request: PermissionRequest) {
        val grantable = ArrayList<String>()
        val needed = ArrayList<String>()
        val names = ArrayList<String>()
        for (r in request.resources) when (r) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> { grantable.add(r); needed.add(android.Manifest.permission.CAMERA); names.add("camera") }
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> { grantable.add(r); needed.add(android.Manifest.permission.RECORD_AUDIO); names.add("microphone") }
        }
        if (grantable.isEmpty()) { request.deny(); return }
        val host = request.origin.host ?: request.origin.toString()
        askYesNo("$host wants to use your ${names.joinToString(" and ")}", "Only allow this if you trust the site.", "Allow", "Block", { allow ->
            if (!allow) { request.deny(); return@askYesNo }
            val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) request.grant(grantable.toTypedArray()) else {
                pendingWeb = request to grantable.toTypedArray()
                requestPermissions(missing.toTypedArray(), REQ_WEB)
            }
        })
    }

    override fun webPermissionCanceled(request: PermissionRequest) { if (pendingWeb?.first === request) pendingWeb = null }

    override fun geolocation(tab: Tab, origin: String, cb: GeolocationPermissions.Callback) {
        val host = UrlParts.hostOf(origin) ?: origin
        askYesNo("$host wants to know your location", "Only allow this if you trust the site.", "Allow", "Block", { allow ->
            if (!allow) { cb.invoke(origin, false, false); return@askYesNo }
            // Android 12+ ignores a FINE-only request (the user must be offered approximate too), so ask for both and accept either.
            val perms = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            if (perms.any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) cb.invoke(origin, true, false) else {
                pendingGeo = origin to cb
                requestPermissions(perms, REQ_GEO)
            }
        })
    }

    @Deprecated("framework callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val ok = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when (requestCode) {
            REQ_WEB -> { pendingWeb?.let { (req, res) -> if (ok) req.grant(res) else req.deny() }; pendingWeb = null }
            REQ_GEO -> { val any = grantResults.any { it == PackageManager.PERMISSION_GRANTED }; pendingGeo?.let { (origin, cb) -> cb.invoke(origin, any, false) }; pendingGeo = null }
        }
    }

    // ================================================================== dialogs

    private fun askYesNo(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) {
        if (isFinishing || isDestroyed) { result(false); return }
        var answered = false
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton(yes) { _, _ -> if (!answered) { answered = true; result(true) } }
            .setNegativeButton(no) { _, _ -> if (!answered) { answered = true; result(false) } }
            .setOnCancelListener { if (!answered) { answered = true; result(false) } }
            .show()
    }

    /** JS dialogs: origin shown, only for the visible tab, and a "stop this page" checkbox after the third one. */
    internal fun jsDialog(tab: Tab, url: String, message: String, input: String?, hasCancel: Boolean, onOk: (String?) -> Unit, onCancel: () -> Unit): Boolean {
        if (tab.dialogsSuppressed || isFinishing || isDestroyed || tab !== current) { onCancel(); return true }
        tab.dialogCount++
        val host = UrlParts.hostOf(url) ?: url
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        box.addView(TextView(this).apply { text = if (message.length > 2000) message.take(2000) + "…" else message; textSize = 16f; setTextColor(pal.fg) })
        var field: EditText? = null
        if (input != null) { field = EditText(this).apply { setText(input); setSingleLine() }; box.addView(field) }
        var stop: CheckBox? = null
        if (tab.dialogCount >= 3) { stop = CheckBox(this).apply { text = "Stop this page from showing more dialogs"; setTextColor(pal.muted) }; box.addView(stop) }
        var answered = false
        val b = AlertDialog.Builder(this).setTitle("$host says").setView(box)
            .setPositiveButton("OK") { _, _ -> if (!answered) { answered = true; if (stop?.isChecked == true) tab.dialogsSuppressed = true; onOk(field?.text?.toString()) } }
            .setOnCancelListener { if (!answered) { answered = true; onCancel() } }
        if (hasCancel) b.setNegativeButton("Cancel") { _, _ -> if (!answered) { answered = true; if (stop?.isChecked == true) tab.dialogsSuppressed = true; onCancel() } }
        b.show()
        return true
    }

    override fun jsAlert(tab: Tab, url: String, message: String, r: JsResult): Boolean = jsDialog(tab, url, message, null, false, { r.confirm() }, { r.cancel() })
    override fun jsConfirm(tab: Tab, url: String, message: String, r: JsResult): Boolean = jsDialog(tab, url, message, null, true, { r.confirm() }, { r.cancel() })
    override fun jsPrompt(tab: Tab, url: String, message: String, default: String?, r: JsPromptResult): Boolean =
        jsDialog(tab, url, message, default ?: "", true, { r.confirm(it ?: "") }, { r.cancel() })

    override fun httpAuth(tab: Tab, handler: HttpAuthHandler, host: String, realm: String) {
        if (isFinishing || isDestroyed) { handler.cancel(); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        val user = EditText(this).apply { hint = "Username"; setSingleLine() }
        val pass = EditText(this).apply { hint = "Password"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        box.addView(user); box.addView(pass)
        var answered = false
        AlertDialog.Builder(this).setTitle("Sign in to $host").setMessage(realm.ifBlank { null }).setView(box)
            .setPositiveButton("Sign in") { _, _ -> if (!answered) { answered = true; handler.proceed(user.text.toString(), pass.text.toString()) } }
            .setNegativeButton("Cancel") { _, _ -> if (!answered) { answered = true; handler.cancel() } }
            .setOnCancelListener { if (!answered) { answered = true; handler.cancel() } }
            .show()
    }

    override fun confirmResubmit(resend: Message, dontResend: Message) {
        askYesNo("Resend form data?", "Reloading will send the information you entered again.", "Resend", "Cancel", { ok -> if (ok) resend.sendToTarget() else dontResend.sendToTarget() })
    }

    // ================================================================== downloads, external links, long-press

    override fun download(tab: Tab, url: String, userAgent: String?, contentDisposition: String?, mime: String?, length: Long) {
        val name = Downloads.fileName(url, contentDisposition, mime)
        val limit = prefs.dataSaver.confirmDownloadBytes(NetworkMonitor.metered)
        val big = limit >= 0 && (length < 0 || length >= limit)
        val risky = Downloads.isRisky(name, mime)
        val cleartext = url.startsWith("http://", ignoreCase = true) // on a hotspot the carrier can read or change an unencrypted download
        if (!big && !risky && !cleartext) { startDownload(url, userAgent, contentDisposition, mime, tab); return }
        val size = if (length >= 0) NewTabPage.fmtBytes(length) else "unknown size"
        val msg = buildString {
            append(name).append("\nFrom: ").append(UrlParts.hostOf(url) ?: "unknown site").append("\nSize: ").append(size)
            if (cleartext) append("\n\nThis download is not encrypted: your mobile carrier or anyone on the network could read or change the file.")
            if (big) append("\n\nYou're on a metered connection (your phone's hotspot). This may use a lot of mobile data.")
            if (risky) append("\n\nThis type of file can install software or run code. Only continue if you trust the site.")
        }
        askYesNo("Download this file?", msg, "Download", "Cancel", { ok -> if (ok) startDownload(url, userAgent, contentDisposition, mime, tab) })
    }

    private fun startDownload(url: String, ua: String?, cd: String?, mime: String?, tab: Tab) {
        val go = { toast(if (Downloads.enqueue(this, url, ua, cd, mime, tab.url)) "Download started" else "Can't download this link") }
        // The system DownloadManager keeps the file, its address and a notification OUTSIDE the private window.
        if (isPrivate) askYesNo("Download from a private window?", "The file, its web address and a notification are kept on this tablet by Android, outside the private window.", "Download", "Cancel", { ok -> if (ok) go() })
        else go()
    }

    override fun externalLaunch(tab: Tab, url: String, userGesture: Boolean): Boolean {
        if (!userGesture) return true // a page cannot launch apps on its own (redirect / script): ignored, and not even parsed
        val plan = ExternalIntents.plan(this, url)
        if (plan == null) { toast("Blocked a link that isn't a web page"); return true }
        val go = {
            try { startActivity(plan.intent) } catch (e: Exception) {
                val fb = plan.fallbackUrl
                if (fb != null) navigateTo(tab, fb) else toast("No app can open this link")
            }
        }
        if (!plan.needsConfirm) { go(); return true }
        val host = UrlParts.hostOf(tab.url) ?: "This page"
        askYesNo("Open ${plan.appLabel ?: "another app"}?", "$host wants to leave the browser and open an app.", "Open", "Cancel", { ok -> if (ok) go() })
        return true
    }

    override fun hitTestLongPress(tab: Tab, view: WebView): Boolean {
        val hit = view.hitTestResult
        val extra = hit.extra
        when (hit.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> if (extra != null) { linkMenu(tab, extra); return true }
            WebView.HitTestResult.IMAGE_TYPE -> if (extra != null) { imageMenu(tab, extra); return true }
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> if (extra != null) {
                val handler = Handler(Looper.getMainLooper()) { m ->
                    val link = m.data?.getString("url")
                    if (!link.isNullOrEmpty()) linkMenu(tab, link, extra) else imageMenu(tab, extra)
                    true
                }
                view.requestFocusNodeHref(handler.obtainMessage())
                return true
            }
        }
        return false
    }

    private fun linkMenu(tab: Tab, url: String, imageUrl: String? = null) {
        val labels = ArrayList<String>(); val acts = ArrayList<() -> Unit>()
        val web = NavPolicy.isWebUrl(url)
        if (web) { labels.add("Open in new tab"); acts.add { val t = newTabInternal(tab); show(t); navigateTo(t, url) } }
        if (web && !isPrivate) { labels.add("Open in private window"); acts.add { startActivity(Intent(this, PrivateActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(url))) } }
        labels.add("Copy link address"); acts.add { copy(url) }
        labels.add("Share link"); acts.add { share(url) }
        if (imageUrl != null) { labels.add("Download image"); acts.add { startDownload(imageUrl, null, null, null, tab) } }
        AlertDialog.Builder(this).setTitle(url.take(80)).setItems(labels.toTypedArray()) { _, i -> acts[i]() }.show()
    }

    private fun imageMenu(tab: Tab, url: String) {
        val web = NavPolicy.isWebUrl(url)
        val labels = ArrayList<String>(); val acts = ArrayList<() -> Unit>()
        if (web) {
            labels.add("Open image in new tab"); acts.add { val t = newTabInternal(tab); show(t); navigateTo(t, url) }
            labels.add("Download image"); acts.add { startDownload(url, null, null, null, tab) }
        }
        labels.add("Copy image address"); acts.add { copy(url) } // a data: image can only be copied, never opened or downloaded from here
        AlertDialog.Builder(this).setTitle(url.take(80)).setItems(labels.toTypedArray()) { _, i -> acts[i]() }.show()
    }

    private fun copy(text: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("link", text))
        toast("Copied")
    }

    companion object {
        private const val REQ_FILE = 1001
        private const val REQ_WEB = 1002
        private const val REQ_GEO = 1003
    }
}
