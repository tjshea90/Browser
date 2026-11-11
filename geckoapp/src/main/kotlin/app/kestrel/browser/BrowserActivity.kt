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
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.kestrel.browser.web.KestrelGeckoView
import app.kestrel.browser.ui.DiagnosticsView
import app.kestrel.browser.diag.Health
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
import app.kestrel.browser.web.NavPolicy
import app.kestrel.browser.web.Tab
import app.kestrel.core.session.Session
import app.kestrel.core.session.SessionCodec
import app.kestrel.core.session.TabRecord
import app.kestrel.core.url.Nav
import app.kestrel.core.url.Omnibox
import app.kestrel.core.url.UrlParts
import app.kestrel.browser.web.ShieldsBridge
import app.kestrel.browser.web.TabDelegates
import app.kestrel.browser.web.gecko
import app.kestrel.browser.web.isPrivateTab
import app.kestrel.browser.web.session
import app.kestrel.core.filter.RequestType
import app.kestrel.core.filter.Verdict
import app.kestrel.core.url.HttpsPolicy
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.SlowScriptResponse
import org.mozilla.geckoview.StorageController
import java.io.File

/**
 * The browser window: owns the tab list, swaps what is shown (new-tab page / WebView / error screen), and implements everything the
 * per-tab WebView clients need from the UI. [MainActivity] is the normal window; [PrivateActivity] runs in its own process.
 */
open class BrowserActivity : Activity(), ToolbarListener {
    /** True while the SELECTED tab is a private tab (private sessions live in the same window; the engine keeps nothing of them). */
    private val isPrivate get() = current?.isPrivateTab == true
    internal lateinit var prefs: Prefs
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
    private val shieldsPanel by lazy { ShieldsPanel(this, pal) { host -> onShieldsChanged(host) } }

    private lateinit var geckoView: GeckoView
    private var fullscreenOn = false
    private var pendingFilePick: ((List<Uri>) -> Unit)? = null
    private var pendingAndroidPerms: PermissionDelegate.Callback? = null
    private var backCallback: Any? = null
    private var suggestSeq = 0
    @Volatile internal var sessionRestored = false // false until restore() ran: saving earlier could replace the saved session with a one-tab placeholder

    private val netListener: () -> Unit = { onNetworkChanged() }
    private val saveRunnable = Runnable { saveSessionNow() }

    // ================================================================== lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.get(this)
        Shields.init(this)
        pal = Pal(this, isPrivate)
        @Suppress("DEPRECATION") run { // no-ops on Android 15+ (edge-to-edge); needed for the private window's colored bars on older versions
            window.statusBarColor = pal.chromeBg
            window.navigationBarColor = pal.chromeBg
        }
        buildUi()
        installBackHandling()
        NetworkMonitor.addListener(netListener)
        db = BrowserDb(this)
        // Start the engine right AFTER the first frame (it takes a moment on this CPU; the native UI is already on screen), pre-launch its content process,
        // then attach the ad/tracker blocking extension to the app's filter engine.
        android.view.Choreographer.getInstance().postFrameCallback {
            Io.main.post {
                Health.log("engine", "runtime-create")
                GeckoEngine.runtime(this)
                GeckoEngine.warmUp(this)
                ShieldsBridge.install(this, ShieldsBridge({ prefs }, { tabForPage(it) }, { shieldCountChanged(it) }))
                Health.log("engine", "runtime-ready")
            }
        }
        val firstTab = Tab(nextTabId++)
        tabs.add(firstTab)
        show(firstTab)
        val launch = urlFromIntent(intent)
        Io.exec {
            val text = try { sessionFile().takeIf { it.isFile }?.readText() } catch (e: Exception) { null }
            val session = SessionCodec.decode(text)
            Io.ui { restore(session, savedInstanceState, launch) }
        }
        Io.exec { db?.purge(5000, 90); UpdateJob.schedule(applicationContext) } // after the session read: nothing at launch waits for this
    }

    override fun onStart() {
        super.onStart()
        NetworkMonitor.start(this)
    }

    override fun onResume() {
        super.onResume()
        Health.log("life", "resume"); Health.startWatchdog(); Health.watchFrames(window)
        val t = current
        if (t != null && !t.isNewTab && t.session == null) attachContent(t) // hibernated while we were in the background: bring it back
        t?.session?.setActive(true)
        refreshNtpIfShown()
    }

    override fun onPause() {
        super.onPause()
        Health.log("life", "pause")
        flushCounters()
        saveSessionNow()
    }

    override fun onStop() {
        super.onStop()
        Health.log("life", "stop"); Health.stopWatchdog(); Health.unwatchFrames(window)
        current?.session?.setActive(false) // stops rendering and throttles the page: saves battery and mobile data
        NetworkMonitor.stop()
    }

    override fun onDestroy() {
        NetworkMonitor.removeListener(netListener)
        Io.main.removeCallbacks(saveRunnable)
        for (t in tabs) { try { t.session?.close() } catch (e: Exception) { /* already gone */ }; t.session = null }
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Health.log("win", "focus", hasFocus.toString())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        urlFromIntent(intent)?.let { openInNewTabOrCurrent(it) }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Health.log("mem", "trim", "level=$level"); Health.count("mem.trim")
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
        geckoView = KestrelGeckoView(this).apply { setAutofillEnabled(false) } // no logins are stored; GeckoView's autofill hooks only add window-focus churn (research/FREEZE.md 4.7)
        // ONE GeckoView, attached once and never removed: detaching it pauses/resumes the compositor through synchronous IPC while the keyboard resizes the window,
        // which is the race family behind "the screen stops updating while typing". Tabs swap with setSession; the new-tab page and error screens are opaque overlays above it.
        content.addView(geckoView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        column.addView(toolbar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)))
        column.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)))
        column.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        ntp = NewTabPage(this, pal, object : NewTabPage.Listener {
            override fun onSearchTap() { focusUrlBar() }
            override fun onOpen(url: String) { current?.let { navigateTo(it, url) } }
        })
        errorView = ErrorView(this, pal, errorListener)
        content.addView(ntp, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        content.addView(errorView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        ntp.visibility = View.GONE
        errorView.visibility = View.GONE
        findBar = FindBar(this, pal, object : FindBar.Listener {
            override fun query(q: String) { findIn(q, false) }
            override fun next(forward: Boolean) { findIn(findBar.input.text.toString(), !forward) }
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
            for (t in tabs.toList()) { try { t.session?.close() } catch (e: Exception) { /* ignore */ }; t.session = null }
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
            show(sel)
        }
        if (launch != null) openInNewTabOrCurrent(launch)
    }

    private fun newTabInternal(parent: Tab? = null, privateTab: Boolean = false): Tab {
        val t = Tab(nextTabId++)
        t.gecko.privateTab = privateTab
        t.parentId = parent?.id ?: 0L
        tabs.add(t)
        return t
    }

    /** The tab showing a page of this URL's host (the filter-engine bridge only knows page URLs): the selected tab first. */
    internal fun tabForPage(url: String): Tab? {
        val h = UrlParts.hostOf(url) ?: return null
        current?.takeIf { it.topHost == h }?.let { return it }
        return tabs.firstOrNull { it.topHost == h }
    }

    internal fun newTabAndShow(): Tab { val t = newTabInternal(); show(t); focusUrlBar(); return t }

    private fun newPrivateTab() { val t = newTabInternal(null, privateTab = true); show(t); focusUrlBar() }

    private fun openInNewTabOrCurrent(url: String) {
        val cur = current
        val t = if (cur != null && cur.isNewTab) cur else newTabInternal()
        show(t)
        navigateTo(t, url)
    }

    private fun show(tab: Tab) {
        val prev = current
        if (prev != null && prev !== tab) prev.session?.setActive(false)
        current = tab
        tab.lastActive = System.currentTimeMillis()
        attachContent(tab)
        tab.session?.setActive(true)
        applyPrivacyFlags()
        enforceLiveLimit()
        updateUi()
        scheduleSave()
    }

    /** Private tab in front: no screenshots / recents thumbnail, and no autofill service sees what is typed. */
    private var secureWindow = false
    private fun applyPrivacyFlags() {
        val priv = current?.isPrivateTab == true
        if (priv != secureWindow) { // a window flag change relayouts the window: only when the private state flips
            secureWindow = priv
            if (priv) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            root.importantForAutofill = if (priv) View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS else View.IMPORTANT_FOR_AUTOFILL_AUTO
        }
    }

    private fun attachContent(tab: Tab) {
        if (!tab.isNewTab) {
            val s = ensureSession(tab)
            if (geckoView.session !== s) { geckoView.releaseSession(); geckoView.setSession(s) }
        }
        if (tab.isNewTab) { if (ntp.visibility != View.VISIBLE) ntp.visibility = View.VISIBLE; refreshNtp() }
        else if (ntp.visibility != View.GONE) ntp.visibility = View.GONE
        val e = tab.error
        if (e != null) { errorView.show(e); if (errorView.visibility != View.VISIBLE) errorView.visibility = View.VISIBLE }
        else if (errorView.visibility != View.GONE) errorView.visibility = View.GONE
        findBar.visibility = if (tab.findOpen) View.VISIBLE else View.GONE
    }

    private fun createSession(tab: Tab): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(tab.isPrivateTab)
            .useTrackingProtection(true)
            .suspendMediaWhenInactive(true)
            .userAgentMode(if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()
        val s = GeckoSession(settings)
        val d = TabDelegates(this, tab)
        tab.gecko.delegates = d // the session only holds a weak-ish view of its delegates in some versions: keep them alive with the tab
        s.navigationDelegate = d
        s.progressDelegate = d
        s.contentDelegate = d
        s.promptDelegate = d
        s.permissionDelegate = d
        s.contentBlockingDelegate = d
        return s
    }

    private fun ensureSession(tab: Tab): GeckoSession {
        tab.session?.let { return it }
        val s = createSession(tab)
        s.open(GeckoEngine.runtime(this))
        tab.session = s
        val state = tab.gecko.state
        val pending = tab.pendingLoad
        when {
            state != null -> {
                tab.gecko.state = null
                tab.pendingLoad = null
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(tab.url))
                s.restoreState(state) // brings back the tab's whole history, not just its last page
            }
            pending != null -> {
                tab.pendingLoad = null
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(pending))
                tab.topHost = UrlParts.hostOf(pending)
                s.loadUri(pending)
            }
        }
        enforceLiveLimit()
        return s
    }

    private fun hasLiveChild(t: Tab) = tabs.any { it.parentId == t.id && it.session != null }

    private fun detach(v: View) { (v.parent as? ViewGroup)?.removeView(v) }

    /** Low-RAM policy: at most 2 live engine sessions (4 on bigger devices); the least recently used others are hibernated. */
    private fun enforceLiveLimit() {
        val low = getSystemService(ActivityManager::class.java)?.isLowRamDevice != false
        val max = if (low) 2 else 4
        val live = tabs.filter { it.session != null }.sortedBy { it.lastActive }
        var count = live.size
        for (t in live) {
            if (count <= max) break
            if (t === current || hasLiveChild(t)) continue
            hibernate(t)
            count--
        }
    }

    /** Free a tab's engine session but keep what is needed to bring it back (session state with its history, or at least its URL). */
    private fun hibernate(tab: Tab) {
        val s = tab.session ?: return
        if (geckoView.session === s) geckoView.releaseSession()
        try { s.close() } catch (e: Exception) { /* already gone */ }
        tab.session = null
        if (tab.isPrivateTab) { tab.gecko.state = null; return } // a private tab is not kept anywhere: it comes back blank rather than leaving history in memory
        if (tab.gecko.state == null && tab.url.isNotEmpty()) tab.pendingLoad = tab.url
    }

    /** A page opened a window (window.open / target=_blank): a new tab whose session the engine opens itself. */
    internal fun newPopupSession(opener: Tab, uri: String): GeckoSession {
        val t = newTabInternal(opener, opener.isPrivateTab)
        t.url = if (uri.isBlank()) "about:blank" else uri
        t.openerHost = opener.topHost
        val s = createSession(t)
        t.session = s
        Io.main.postDelayed({ if (tabs.contains(t)) show(t) }, 60) // after the engine has opened the new session
        return s
    }

    internal fun closeTab(tab: Tab) { Io.main.post { doCloseTab(tab) } }

    private fun doCloseTab(tab: Tab) {
        val idx = tabs.indexOf(tab)
        if (idx < 0) return
        tabs.removeAt(idx)
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* ignore */ } }
        tab.session = null
        if (current === tab) {
            val next = tabs.firstOrNull { it.id == tab.parentId } ?: tabs.getOrNull(idx) ?: tabs.getOrNull(idx - 1)
            if (next == null) show(newTabInternal()) else show(next)
        } else updateUi()
        scheduleSave()
    }

    private fun showNewTabPage(tab: Tab) {
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* ignore */ } }
        tab.session = null
        tab.gecko.state = null
        tab.url = ""; tab.title = ""; tab.error = null; tab.pendingLoad = null; tab.loading = false; tab.progress = 0
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
        tab.gecko.state = null
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
        val s = ensureSession(tab)
        if (current === tab) attachContent(tab)
        s.loadUri(url)
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
    override fun onForward() { current?.session?.goForward() }

    override fun onReloadOrStop() {
        val t = current ?: return
        if (t.error != null) { retry(t); return }
        val s = t.session ?: return
        if (t.loading) { s.stop(); t.loading = false; t.progress = 0; updateUi() } else s.reload()
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
                if (t === current) t.session?.reload()
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

    /** While the address bar is being typed in, the page view must NOT resize under the keyboard (compositor resize while typing = the freeze race); pages keep adjustResize. */
    private fun setUrlTypingMode(on: Boolean) {
        val lp = window.attributes
        val want = if (on) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        if ((lp.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != want) {
            window.setSoftInputMode((lp.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or want)
            Health.log("win", "typing-mode", on.toString())
        }
    }

    private val endTypingMode = Runnable { if (!toolbar.urlField.hasFocus()) setUrlTypingMode(false) }

    override fun onUrlFocus(focused: Boolean) {
        Io.main.removeCallbacks(endTypingMode)
        if (focused) setUrlTypingMode(true) else Io.main.postDelayed(endTypingMode, 700) // keep adjustNothing while the keyboard is still closing
        if (focused) {
            toolbar.urlField.setText(toolbar.fullUrlForEditing(current))
            toolbar.urlField.selectAll()
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
    private object M { const val REFRESH = 14; const val DIAG = 15; const val NEW = 1; const val PRIVATE = 2; const val FIND = 3; const val SHARE = 4; const val BOOKMARK = 5; const val BOOKMARKS = 6; const val HISTORY = 7
        const val DOWNLOADS = 8; const val DESKTOP = 9; const val IMAGES = 10; const val PRINT = 11; const val SETTINGS = 12; const val QUIT = 13; const val CLOSE_ALL = 14 }

    override fun onMenu(anchor: View) {
        val t = current
        val items = ArrayList<MenuItemSpec>()
        items.add(MenuItemSpec(M.NEW, "New tab"))
        items.add(MenuItemSpec(M.PRIVATE, "New private tab"))
        if (t != null && !t.isNewTab) {
            items.add(MenuItemSpec(M.FIND, "Find in page"))
            items.add(MenuItemSpec(M.SHARE, "Share…"))
            if (!isPrivate) items.add(MenuItemSpec(M.BOOKMARK, "Add bookmark"))
            items.add(MenuItemSpec(M.DESKTOP, "Desktop site", t.desktop))
        }
        if (!isPrivate) { items.add(MenuItemSpec(M.BOOKMARKS, "Bookmarks")); items.add(MenuItemSpec(M.HISTORY, "History")) }
        items.add(MenuItemSpec(M.DOWNLOADS, "Downloads"))
        if (t != null && !t.isNewTab) items.add(MenuItemSpec(M.REFRESH, "Refresh screen"))
        if (!isPrivate) items.add(MenuItemSpec(M.SETTINGS, "Settings"))
        items.add(MenuItemSpec(M.DIAG, "Diagnostics"))
        if (tabs.size > 1) items.add(MenuItemSpec(M.CLOSE_ALL, "Close all tabs"))
        items.add(MenuItemSpec(M.QUIT, if (isPrivate) "Close private window" else "Quit"))
        MenuPopup.show(this, pal, anchor, items) { id -> onMenuPick(id) }
    }

    private fun onMenuPick(id: Int) {
        val t = current
        when (id) {
            M.REFRESH -> refreshScreen("menu")
            M.DIAG -> showDiagnostics()
            M.NEW -> newTabAndShow()
            M.PRIVATE -> newPrivateTab()
            M.FIND -> openFind()
            M.SHARE -> t?.takeIf { !it.isNewTab }?.let { share(it.url) }
            M.BOOKMARK -> t?.takeIf { it.url.isNotEmpty() }?.let { tab ->
                Io.exec { db?.let { d -> if (d.isBookmarked(tab.url)) { d.removeBookmark(tab.url); Io.ui { toast("Bookmark removed") } } else { d.addBookmark(tab.url, tab.title); Io.ui { toast("Bookmark added") } } } }
            }
            M.BOOKMARKS -> showList(true)
            M.HISTORY -> showList(false)
            M.DOWNLOADS -> try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { toast("No downloads app found") }
            M.DESKTOP -> t?.let { tab ->
                tab.desktop = !tab.desktop
                tab.session?.let { s ->
                    s.settings.userAgentMode = if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                    s.settings.viewportMode = if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                    s.reload()
                }
            }
            M.SETTINGS -> showSettings()
            M.CLOSE_ALL -> { for (x in tabs.toList()) { x.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* ignore */ } }; x.session = null }; tabs.clear(); current = null; show(newTabInternal()) }
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
        getSystemService(InputMethodManager::class.java)?.showSoftInput(findBar.input, 0)
    }

    private fun findIn(q: String, backwards: Boolean) {
        val f = current?.session?.finder ?: return
        if (q.isEmpty()) { f.clear(); findBar.setResult(0, 0); return }
        f.find(q, if (backwards) org.mozilla.geckoview.GeckoSession.FINDER_FIND_BACKWARDS else 0).accept { r -> if (r != null) findBar.setResult(maxOf(r.current - 1, 0), r.total) }
    }

    private fun closeFind() {
        current?.let { it.findOpen = false; it.session?.finder?.clear() }
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

    // ------------------------------------------------------------------ freeze evidence + manual recovery (research/FREEZE.md 5)
    private var diagView: DiagnosticsView? = null

    private fun engineFacts(): List<String> {
        val out = ArrayList<String>()
        val ver = try { Class.forName("org.mozilla.geckoview.BuildConfig").getField("MOZ_APP_VERSION").get(null) } catch (e: Exception) { "?" }
        out.add("Engine: GeckoView $ver; live sessions ${tabs.count { it.session != null }} of ${tabs.size} tabs; current ${current?.url?.take(60) ?: "-"}")
        out.add(GeckoEngine.prefReadback)
        out.add("Page view: attached=${geckoView.parent != null} session=${geckoView.session != null} windowFocus=${hasWindowFocus()}")
        return out
    }

    private fun showDiagnostics() {
        GeckoEngine.readBackPrefs { diagView?.refresh() }
        val v = diagView ?: DiagnosticsView(this, pal, ::engineFacts, { refreshScreen("diagnostics") }, { closeDiagnostics() }).also { diagView = it }
        if (v.parent == null) root.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)) else v.refresh()
    }

    private fun closeDiagnostics() { diagView?.let { detach(it) } }

    /**
     * What switching apps and coming back does to the page, on demand: re-send the focus and keyboard state (R0), toggle the session's active state (R1),
     * then ask GeckoView for a brand-new rendering surface (R2). Logged so a report shows which step cured it.
     */
    private fun refreshScreen(why: String) {
        Health.log("user", "refresh-screen", why); Health.count("user.refresh")
        val s = current?.session ?: return
        try {
            s.setFocused(false); s.setFocused(true)
            (getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)?.restartInput(geckoView)
            s.setActive(false); s.setActive(true)
        } catch (e: Exception) { Health.log("user", "refresh-R0R1-failed", e.toString()) }
        Io.main.postDelayed({ try { geckoView.requestNewSurface() } catch (e: Exception) { Health.log("user", "refresh-R2-failed", e.toString()) } }, 150)
    }

    private val settingsListener = object : SettingsListener {
        override fun closeSettings() { this@BrowserActivity.closeSettings() }
        override fun prefsChanged() {
            for (t in tabs) t.shields = Shields.effective(prefs, t.topHost)
            GeckoEngine.applyPrefs(applicationContext)
            Io.exec { UpdateJob.schedule(applicationContext) } // the update schedule may have changed
        }
        override fun clearBrowsingData() { this@BrowserActivity.clearBrowsingData() }
        override fun resetSiteShields() { Shields.clearAll(); toast("Per-site Shields reset") }
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
            return "${Engine.sourceDescription}: ${"%,d".format(rules)} network rules" +
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
        out.add("Browser engine" to "Firefox engine (GeckoView 160). Pages are drawn like in current Firefox, independent of this tablet's old Android System WebView. " +
            "The engine is updated by installing a newer Kestrel G build.")
        out.add("User agent" to "Chosen by the engine (a current Firefox for Android identity)")
        val am = getSystemService(ActivityManager::class.java)
        val ram = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }.totalMem
        out.add("Device" to "RAM ${"%.1f".format(ram / 1_073_741_824.0)} GB, app memory class ${am?.memoryClass} MB, low-RAM mode: ${am?.isLowRamDevice}")
        out.add("Safe Browsing" to if (hasPlayServices()) "Google Safe Browsing is available on this tablet, on top of Kestrel's built-in malware list."
            else "Google Safe Browsing needs Google Play services, which this tablet doesn't have. Protection against malware and phishing sites comes only from Kestrel's built-in list, which updates weekly.")
        out.add("Licenses" to "Filter lists: EasyList and EasyPrivacy (GPLv3+ / CC BY-SA 3.0), uBlock Origin filters (GPLv3), Fanboy Cookiemonster (CC BY 3.0 / GPLv3). Public Suffix List (MPL-2.0). Personal, non-commercial use.")
        return out
    }

    // ================================================================== data / state

    private fun clearBrowsingData() {
        GeckoEngine.runtime(this).storageController.clearData(StorageController.ClearFlags.ALL)
        Io.exec { db?.clearHistory() }
        toast("Browsing data cleared")
    }

    private fun quit() {
        if (prefs.clearOnExit) {
            GeckoEngine.runtime(this).storageController.clearData(StorageController.ClearFlags.ALL)
            sessionRestored = false // nothing may re-create the tab list after this point
            Io.exec { db?.clearHistory(); sessionFile().delete() }
        } else saveSessionNow()
        finishAndRemoveTask()
        // Free the memory right away: this tablet is low-RAM and the user asked to quit. The engine gets a moment to flush its profile first.
        Io.main.postDelayed({ Process.killProcess(Process.myPid()) }, 1500)
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

    private fun scheduleSave() {
        Io.main.removeCallbacks(saveRunnable)
        Io.main.postDelayed(saveRunnable, 600)
    }

    internal fun saveSessionNow() {
        val keep = tabs.filter { !it.isPrivateTab } // private tabs are never written anywhere
        if (keep.isEmpty() || !sessionRestored) return
        val sel = current?.takeIf { !it.isPrivateTab }?.id ?: keep.last().id
        val records = keep.map { TabRecord(it.id, withoutUserinfo(it.url).ifEmpty { "about:blank" }, it.title, it.lastActive, it.parentId, it.desktop) }
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
            val data = DataMeter.totalBytes(prefs)
            Io.ui {
                ntp.bindSites(sites)
                ntp.bindStats(prefs.blockedTotal, data, Engine.sourceDescription, NetworkMonitor.metered)
                ntp.bindNotices(emptyList())
            }
        }
    }

    private fun onNetworkChanged() {
        val t = current
        if (NetworkMonitor.state == NetworkMonitor.State.ONLINE && t?.error is ErrorScreen.Network) retry(t) // the hotspot is back: reload by itself
        else if (t?.error != null) errorView.show(t.error!!)
        refreshNtpIfShown()
    }

    // ================================================================== UI refresh

    // Coalesced: any number of state changes in one burst (progress ticks, title, blocked-count) cost ONE toolbar pass on the next looper turn.
    private var uiPosted = false
    private val uiRunnable = Runnable { uiPosted = false; applyUi() }
    private fun updateUi() { if (!uiPosted) { uiPosted = true; Io.main.post(uiRunnable) } }

    private fun applyUi() {
        val t = current
        val canBack = t?.canGoBack == true || (t != null && t.parentId != 0L && tabs.any { it.id == t.parentId })
        toolbar.bind(t, tabs.size, canBack || (t != null && !t.isNewTab))
        val p = if (t?.loading == true) t.progress else 0
        if (progress.progress != p) progress.progress = p
    }

    internal fun tabChanged(tab: Tab) { if (tab === current) updateUi() else if (tabSwitcher.visibility == View.VISIBLE) tabSwitcher.bind(tabs, current) }
    private val countPosted = java.util.concurrent.atomic.AtomicBoolean(false)
    /** A request was blocked (may be called from any thread): the chip refreshes at most 4 times a second, however many requests a page blocks. */
    internal fun shieldCountChanged(tab: Tab) {
        if (tab === current && countPosted.compareAndSet(false, true)) Io.main.postDelayed({ countPosted.set(false); updateUi() }, 250)
    }
    internal fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }

    internal fun errorChanged(tab: Tab) {
        if (tab === current) { attachContent(tab); updateUi() }
    }

    internal fun pageFinished(tab: Tab) {
        flushCounters()
        // A failed or blocked load is not a visit (typos and malware URLs would otherwise fill history and the top sites).
        if (!tab.isPrivateTab && tab.error == null && tab.url.startsWith("http")) { val u = withoutUserinfo(tab.url); val ti = tab.title; Io.exec { db?.recordVisit(u, ti) } }
        scheduleSave()
        if (tab === current) updateUi()
    }

    /** The engine's content process died (or the system killed it). Come back from the URL; a crash loop (3 in 15 s) shows the error screen instead. */
    internal fun sessionGone(tab: Tab, crashed: Boolean) {
        val url = tab.url
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* already unusable */ } }
        tab.session = null
        tab.gecko.state = null
        tab.pendingLoad = url.takeIf { it.startsWith("http") }
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
        if (diagView?.parent != null) { closeDiagnostics(); return true }
        if (settingsView?.parent != null) { closeSettings(); return true }
        if (listPanel?.parent != null) { closeList(); return true }
        if (tabSwitcher.visibility == View.VISIBLE) { hideTabSwitcher(); return true }
        if (findBar.visibility == View.VISIBLE) { closeFind(); return true }
        if (suggestions.visibility == View.VISIBLE || toolbar.urlField.hasFocus()) { hideSuggestions(); clearUrlFocus(); updateUi(); return true }
        val t = current ?: return false
        if (t.error != null) {
            t.error = null
            if (t.canGoBack && t.session != null) { attachContent(t); t.session?.goBack() } else if (t.parentId != 0L && tabs.any { it.id == t.parentId }) doCloseTab(t) else showNewTabPage(t)
            return true
        }
        if (t.canGoBack && t.session != null) { t.session?.goBack(); return true }
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
            if (t.canGoBack && t.session != null) { attachContent(t); t.session?.goBack() } else showNewTabPage(t)
        }
        override fun proceedUnsafeSsl(host: String) { current?.let { it.sslBypassHosts.add(host); this@BrowserActivity.retry(it) } }
        override fun continueHttp(httpUrl: String, host: String) { current?.let { it.httpOkHosts.add(host); it.error = null; loadUrl(it, httpUrl, null) } }
        override fun proceedDangerous(url: String, host: String) { current?.let { it.dangerOkHosts.add(host); it.error = null; loadUrl(it, url, null) } }
        override fun openWifiSettings() { try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (e: Exception) { toast("Open Wi-Fi settings from the tablet's Settings app") } }
    }

    // ================================================================== fullscreen video (the engine draws it; we only get out of its way)

    internal fun fullScreenChanged(on: Boolean) {
        fullscreenOn = on
        toolbar.visibility = if (on) View.GONE else View.VISIBLE
        progress.visibility = if (on) View.GONE else View.VISIBLE
        applyImmersive(on)
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

    // ================================================================== policy helpers used by the per-tab delegates

    /** Main-frame page on the malware / phishing list: show the warning screen instead (no shouldInterceptRequest in this engine to do it). */
    internal fun dangerousDocument(tab: Tab, url: String): Boolean {
        val fs = Engine.peek() ?: return false // lists still loading at cold start: never hold the UI thread
        val h = UrlParts.hostOf(url) ?: return false
        val s = Shields.effective(prefs, h)
        if (!s.shieldsUp || !s.blockAds || h in tab.dangerOkHosts || fs.isPageAllowed(h)) return false
        val v = fs.check(url, null, RequestType.DOCUMENT)
        if (!Verdict.isBlocked(v)) return false
        tab.loading = false
        tab.error = ErrorScreen.Dangerous(url, h, Verdict.category(v))
        errorChanged(tab)
        return true
    }

    /** Per-page state for the filter engine / Shields chip: which host this page is. */
    internal fun noteMainFrame(tab: Tab, url: String) {
        val h = UrlParts.hostOf(url)
        tab.topHost = h
        tab.mainUrl = url
        tab.shields = Shields.effective(prefs, h)
        tab.pageAllowed = h != null && Engine.peek()?.isPageAllowed(h) == true
    }

    /** The https:// we chose ourselves does not work: load http:// (UPGRADE mode) or ask (STRICT). */
    internal fun httpsUnavailable(tab: Tab, httpsUrl: String): Boolean {
        val httpUrl = tab.upgradedFrom ?: HttpsPolicy.downgradeOf(httpsUrl) ?: return false
        val h = UrlParts.hostOf(httpUrl) ?: return false
        tab.upgradedTo = null
        if (Shields.effective(prefs, h).https == HttpsPolicy.Mode.UPGRADE) {
            tab.httpOkHosts.add(h)
            loadInTab(tab, httpUrl)
        } else {
            tab.loading = false
            tab.error = ErrorScreen.HttpsUnavailable(httpUrl, h)
            errorChanged(tab)
        }
        return true
    }

    internal fun loadInTab(tab: Tab, url: String) { Io.main.post { tab.session?.loadUri(url) } }

    // ================================================================== files, permissions

    internal fun pickFiles(mimeTypes: Array<String>?, multiple: Boolean, done: (List<Uri>) -> Unit) {
        pendingFilePick?.invoke(emptyList())
        pendingFilePick = done
        val i = Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
        val types = mimeTypes?.filter { it.isNotBlank() && it.contains('/') }?.takeIf { it.isNotEmpty() }
        if (types != null) i.putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
        try { startActivityForResult(Intent.createChooser(i, "Choose file"), REQ_FILE) } catch (e: Exception) { pendingFilePick = null; done(emptyList()) }
    }

    @Deprecated("framework callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            val cb = pendingFilePick
            pendingFilePick = null
            val uris = ArrayList<Uri>()
            if (resultCode == RESULT_OK && data != null) {
                data.data?.let { uris.add(it) }
                val clip = data.clipData
                if (clip != null) for (k in 0 until clip.itemCount) clip.getItemAt(k).uri?.let { uris.add(it) }
            }
            // content:// only: a rogue picker app could answer with file:///data/data/<this app>/... and the page would upload the browser's own files
            cb?.invoke(uris.filter { it.scheme == "content" }.distinct())
        }
    }

    internal fun requestAndroidPermissions(perms: Array<out String>, cb: PermissionDelegate.Callback) {
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) { cb.grant(); return }
        pendingAndroidPerms?.reject()
        pendingAndroidPerms = cb
        requestPermissions(missing.toTypedArray(), REQ_PERM)
    }

    @Deprecated("framework callback")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERM) {
            val cb = pendingAndroidPerms
            pendingAndroidPerms = null
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) cb?.grant() else cb?.reject()
        }
    }

    // ================================================================== dialogs

    internal fun askYesNo(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) {
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

    internal fun askAuth(host: String, realm: String, onOk: (String, String) -> Unit, onCancel: () -> Unit) {
        if (isFinishing || isDestroyed) { onCancel(); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        val user = EditText(this).apply { hint = "Username"; setSingleLine() }
        val pass = EditText(this).apply { hint = "Password"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        box.addView(user); box.addView(pass)
        var answered = false
        AlertDialog.Builder(this).setTitle("Sign in to $host").setMessage(realm.ifBlank { null }).setView(box)
            .setPositiveButton("Sign in") { _, _ -> if (!answered) { answered = true; onOk(user.text.toString(), pass.text.toString()) } }
            .setNegativeButton("Cancel") { _, _ -> if (!answered) { answered = true; onCancel() } }
            .setOnCancelListener { if (!answered) { answered = true; onCancel() } }
            .show()
    }

    /** `<select>` menus and option lists (the engine hands us the choices; option groups are flattened). */
    internal fun showChoice(choices: Array<PromptDelegate.ChoicePrompt.Choice>, multiple: Boolean, onPick: (Array<PromptDelegate.ChoicePrompt.Choice>) -> Unit, onCancel: () -> Unit) {
        if (isFinishing || isDestroyed) { onCancel(); return }
        val flat = ArrayList<PromptDelegate.ChoicePrompt.Choice>()
        fun add(c: PromptDelegate.ChoicePrompt.Choice) {
            val sub = c.items
            if (sub != null) sub.forEach { add(it) } else if (!c.separator) flat.add(c)
        }
        choices.forEach { add(it) }
        val labels = flat.map { it.label ?: "" }.toTypedArray()
        var answered = false
        val b = AlertDialog.Builder(this).setOnCancelListener { if (!answered) { answered = true; onCancel() } }
        if (multiple) {
            val checked = BooleanArray(flat.size) { flat[it].selected }
            b.setMultiChoiceItems(labels, checked) { _, i, on -> checked[i] = on }
                .setPositiveButton("OK") { _, _ -> if (!answered) { answered = true; onPick(flat.filterIndexed { i, _ -> checked[i] }.toTypedArray()) } }
                .setNegativeButton("Cancel") { _, _ -> if (!answered) { answered = true; onCancel() } }
        } else {
            b.setItems(labels) { _, i -> if (!answered) { answered = true; onPick(arrayOf(flat[i])) } }
        }
        b.show()
    }

    internal fun askSlowScript(tab: Tab): GeckoResult<SlowScriptResponse> {
        val r = GeckoResult<SlowScriptResponse>()
        askYesNo("This page is slow", "A script on this page is taking very long. Stop it?", "Stop it", "Wait") { stop ->
            r.complete(if (stop) SlowScriptResponse.STOP else SlowScriptResponse.CONTINUE)
        }
        return r
    }

    // ================================================================== downloads, external links, long-press

    internal fun download(tab: Tab, url: String, userAgent: String?, contentDisposition: String?, mime: String?, length: Long) {
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
        // The system DownloadManager keeps the file, its address and a notification OUTSIDE the private tab.
        if (tab.isPrivateTab) askYesNo("Download from a private tab?", "The file, its web address and a notification are kept on this tablet by Android, outside the private tab.", "Download", "Cancel", { ok -> if (ok) go() })
        else go()
    }

    internal fun externalLaunch(tab: Tab, url: String, userGesture: Boolean): Boolean {
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

    internal fun longPress(tab: Tab, el: ContentDelegate.ContextElement) {
        val link = el.linkUri
        val src = el.srcUri
        when {
            link != null -> linkMenu(tab, link, if (el.type == ContentDelegate.ContextElement.TYPE_IMAGE) src else null)
            el.type == ContentDelegate.ContextElement.TYPE_IMAGE && src != null -> imageMenu(tab, src)
        }
    }

    private fun linkMenu(tab: Tab, url: String, imageUrl: String? = null) {
        val labels = ArrayList<String>(); val acts = ArrayList<() -> Unit>()
        val web = NavPolicy.isWebUrl(url)
        if (web) { labels.add("Open in new tab"); acts.add { val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigateTo(t, url) } }
        if (web && !tab.isPrivateTab) { labels.add("Open in private tab"); acts.add { val t = newTabInternal(null, privateTab = true); show(t); navigateTo(t, url) } }
        labels.add("Copy link address"); acts.add { copy(url) }
        labels.add("Share link"); acts.add { share(url) }
        if (imageUrl != null && NavPolicy.isWebUrl(imageUrl)) { labels.add("Download image"); acts.add { startDownload(imageUrl, null, null, null, tab) } }
        AlertDialog.Builder(this).setTitle(url.take(80)).setItems(labels.toTypedArray()) { _, i -> acts[i]() }.show()
    }

    private fun imageMenu(tab: Tab, url: String) {
        val web = NavPolicy.isWebUrl(url)
        val labels = ArrayList<String>(); val acts = ArrayList<() -> Unit>()
        if (web) {
            labels.add("Open image in new tab"); acts.add { val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigateTo(t, url) }
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
        private const val REQ_PERM = 1002
    }
}
