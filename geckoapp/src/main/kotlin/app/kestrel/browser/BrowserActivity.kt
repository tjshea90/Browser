package app.kestrel.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
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
import android.graphics.Rect
import android.view.Gravity
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
import app.kestrel.browser.R
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
import app.kestrel.browser.ui.NewTabPage
import app.kestrel.browser.ui.Pal
import app.kestrel.browser.ui.Neon
import app.kestrel.browser.ui.NeonBar
import app.kestrel.browser.ui.RestoreOverlay
import app.kestrel.browser.web.ScrollTest
import app.kestrel.browser.ui.SettingsListener
import app.kestrel.browser.ui.SettingsView
import app.kestrel.browser.ui.ShieldsPanel
import app.kestrel.browser.ui.SuggestionList
import app.kestrel.browser.ui.TabGrid
import app.kestrel.browser.ui.DownloadsScreen
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.download.Notifier
import app.kestrel.browser.download.PageProxy
import app.kestrel.browser.download.PhoneBridge
import app.kestrel.core.courier.PhoneMode
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Source
import app.kestrel.browser.ui.UpdatePanel
import app.kestrel.browser.update.UpdateManager
import app.kestrel.browser.update.UpdateNotifier
import app.kestrel.browser.ui.ThumbStore
import app.kestrel.browser.ui.BottomDock
import app.kestrel.browser.ui.DockListener
import app.kestrel.browser.ui.HomeView
import app.kestrel.browser.ui.KeyboardProbe
import app.kestrel.browser.ui.SheetHost
import app.kestrel.browser.ui.menuGrid
import app.kestrel.browser.ui.TopSite
import app.kestrel.browser.ui.dp
import app.kestrel.browser.ui.dpf
import app.kestrel.browser.ui.PullChip
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.browser.web.ExternalIntents
import app.kestrel.browser.web.NavPolicy
import app.kestrel.browser.web.Tab
import app.kestrel.core.session.Session
import app.kestrel.core.session.SessionCodec
import app.kestrel.core.session.TabRecord
import app.kestrel.core.tabs.LiveBudget
import app.kestrel.browser.data.TabStateStore
import app.kestrel.browser.ui.BackPictures
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
open class BrowserActivity : Activity(), DockListener {
    /** True while the SELECTED tab is a private tab (private sessions live in the same window; the engine keeps nothing of them). */
    private val isPrivate get() = current?.isPrivateTab == true
    internal lateinit var prefs: Prefs
    internal lateinit var pal: Pal
    private lateinit var palRegular: Pal
    private lateinit var palPrivate: Pal
    private lateinit var root: FrameLayout
    private lateinit var column: LinearLayout
    private lateinit var dock: BottomDock
    internal lateinit var sheet: SheetHost
    private lateinit var progress: NeonBar
    private lateinit var content: FrameLayout
    private lateinit var home: HomeView
    private lateinit var errorView: ErrorView
    private lateinit var pullChip: PullChip // the pull-down-to-refresh indicator, over the page
    private lateinit var noticeBar: app.kestrel.browser.ui.NoticeBar // "Blocked a pop-up ... Open", over the bottom of the page
    private lateinit var restoreOverlay: RestoreOverlay // over the page view while a page whose engine process died comes back (last preview instead of white)
    private var restoringTab: Tab? = null
    private val restoreTimeout = Runnable { hideRestoring("timeout") }
    private var lastTrim = 0           // the newest onTrimMemory level and when it came: logged with every process death (the evidence for "why did the page reload")
    private var lastTrimAt = 0L
    private lateinit var findBar: FindBar
    private lateinit var suggestions: SuggestionList
    private lateinit var tabGrid: TabGrid
    private lateinit var fullscreen: FrameLayout
    private var settingsView: SettingsView? = null
    private var listPanel: ListPanel? = null

    private val tabs = ArrayList<Tab>()
    internal var current: Tab? = null
        private set
    private val video = app.kestrel.browser.web.VideoFlow(this)

    /** For tests (same module): the selected tab. */
    internal val currentTab: Tab? get() = current
    internal val allTabs: List<Tab> get() = tabs
    private var nextTabId = 1L
    private var db: BrowserDb? = null
    private fun shieldsPanel() = ShieldsPanel(this, pal) { host -> onShieldsChanged(host) } // built per use: it takes the colours of the current (regular or private) skin

    private lateinit var geckoView: KestrelGeckoView
    private var fullscreenOn = false
    private var pendingFilePick: ((List<Uri>) -> Unit)? = null
    private var pendingAndroidPerms: PermissionDelegate.Callback? = null
    private var backCallback: Any? = null
    private var suggestSeq = 0
    @Volatile internal var sessionRestored = false // false until restore() ran: saving earlier could replace the saved session with a one-tab placeholder

    private val netListener: () -> Unit = { onNetworkChanged() }
    private val updateListener: () -> Unit = { if (::dock.isInitialized) dock.setMenuDot(UpdateManager.pending()); Io.main.post { maybeShowUpdateNotice() } }
    private var updateNoticeShowing = false
    private var updateFromDialog = false
    private val saveRunnable = Runnable { saveSessionNow() }

    // ================================================================== lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLife.uiAlive = true; ProcessLife.cancel()
        prefs = Prefs.get(this)
        coldStartClear()
        Shields.init(this)
        palRegular = Pal(this, false)
        palPrivate = Pal(this, true)
        pal = palRegular
        @Suppress("DEPRECATION") run { // no-ops on Android 15+ (edge-to-edge); the bars follow the skin (see applySkin)
            window.statusBarColor = pal.windowBg
            window.navigationBarColor = pal.chromeBg
        }
        buildUi()
        DownloadController.init(this) // restores interrupted downloads (they come back paused) so the list and the menu are right from the start
        UpdateManager.init(this)
        UpdateManager.addListener(updateListener)
        dock.setMenuDot(UpdateManager.pending())
        installBackHandling()
        NetworkMonitor.addListener(netListener)
        db = BrowserDb(this)
        // Start the engine right AFTER the first frame (it takes a moment on this CPU; the native UI is already on screen), pre-launch its content process,
        // then attach the ad/tracker blocking extension to the app's filter engine.
        android.view.Choreographer.getInstance().postFrameCallback {
            Io.main.post {
                Health.log("engine", "runtime-create")
                GeckoEngine.runtime(this)
                GeckoEngine.executorOrNull() // Firefox's network stack for the download manager (created here on the UI thread)
                GeckoEngine.warmUp(this)
                ShieldsBridge.install(this, ShieldsBridge({ prefs }, { tabForPage(it) }, { shieldCountChanged(it) }, { video.seen(it) }, { p, u, w, h, d, l -> video.info(p, u, w, h, d, l) }, { p, kind, u -> Io.main.post { onPopupBlocked(p, kind, u) } }))
                Health.log("engine", "runtime-ready")
            }
        }
        val firstTab = Tab(nextTabId++)
        tabs.add(firstTab)
        show(firstTab)
        val launch = urlFromIntent(intent)
        if (intent?.getBooleanExtra(Notifier.EXTRA_OPEN_DOWNLOADS, false) == true) Io.main.post { showDownloads() }
        handleUpdateIntent(intent)
        Io.fast {
            val text = try { sessionFile().takeIf { it.isFile }?.readText() } catch (e: Exception) { null }
            val session = SessionCodec.decode(text)
            // each regular tab's saved engine state (Back stack + scroll position) is read here, off the UI thread; it is parsed only when the tab is opened
            val states = HashMap<Long, String>()
            if (session != null && TabStateStore.useEngine(applicationContext, Flavor.engineVersion)) for (r in session.tabs) TabStateStore.read(applicationContext, r.id)?.let { states[r.id] = it }
            val selThumb = if (launch == null && session != null) ThumbStore.load(applicationContext, session.selectedId) else null // the preview that covers the page area while the saved page loads
            Io.ui { restore(session, savedInstanceState, launch, states, selThumb) }
        }
        Io.exec { db?.purge(5000, 90); UpdateJob.schedule(applicationContext); app.kestrel.browser.update.ReleaseCheckJob.schedule(applicationContext) } // after the session read: nothing at launch waits for this
    }

    override fun onStart() {
        super.onStart()
        ProcessLife.uiVisible = true; DownloadController.uiVisibilityChanged()
        NetworkMonitor.start(this)
        Neon.foreground(true)
    }

    override fun onResume() {
        super.onResume()
        Health.log("life", "resume"); Health.startWatchdog(); Health.watchFrames(window)
        PageProxy.refresh()                                           // pages through the phone: renew the phone's proxy door now (the app was away; the Wi-Fi may have changed)
        val t = current
        if (t != null && !t.isNewTab && t.session == null) attachContent(t) // hibernated while we were in the background: bring it back
        t?.session?.setActive(true)
        refreshNtpIfShown()
        // once a day, when the app comes to the front: one tiny conditional request (an ETag makes "nothing new" free). Never in the background.
        UpdateManager.maybeAutoCheck(this, prefs.autoUpdateCheck, NetworkMonitor.state != NetworkMonitor.State.OFFLINE) // NO_INTERNET is only a guess (a hotspot can fail Android's connectivity probe and still work): try
        Io.main.postDelayed({ maybeShowUpdateNotice() }, 2500) // news the background look found while the app was closed
        Io.main.postDelayed({ maybeRefreshLists() }, 90_000)   // well after start-up: the page the owner opened first comes first
    }

    private var listRefreshStarted = false

    /** The ad-blocking lists are refreshed every day by a scheduled job; when Android has not run it (a tablet that sleeps most of the day gets its jobs deferred) the app does it itself, once, a minute and a half after it came to the front. */
    private fun maybeRefreshLists() {
        if (isFinishing || isDestroyed || listRefreshStarted || prefs.updatePolicy == 2) return
        if (System.currentTimeMillis() - prefs.lastUpdateMs < 30L * 3600 * 1000 || NetworkMonitor.state == NetworkMonitor.State.OFFLINE) return
        listRefreshStarted = true
        Thread({ FilterUpdater.run(applicationContext, manual = false) }, "kestrel-lists").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
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
        Neon.foreground(false)
        ProcessLife.uiVisible = false; DownloadController.uiVisibilityChanged()
    }

    private var lastTouchAt = 0L
    private var downX = 0f
    private var downY = 0f
    private var dragged = false
    private val touchSlop by lazy { android.view.ViewConfiguration.get(this).scaledTouchSlop }
    private val touchRelease = Runnable { Neon.touching(false) }

    /**
     * Holds the neon still while a finger is down and for 1.5 s after a drag (a fling): the page's scrolling
     * must never share frames with our glow. [lastTouchAt] also keeps the post-load preview capture (a GPU read-back) away from a scroll in progress.
     */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                Neon.touching(true)
                lastTouchAt = SystemClock.uptimeMillis()
                downX = ev.x; downY = ev.y; dragged = false
                Io.main.removeCallbacks(touchRelease)
            }
            android.view.MotionEvent.ACTION_POINTER_DOWN -> dragged = true // a pinch is a gesture too
            android.view.MotionEvent.ACTION_MOVE -> if (!dragged && (Math.abs(ev.x - downX) > touchSlop || Math.abs(ev.y - downY) > touchSlop)) dragged = true
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                lastTouchAt = SystemClock.uptimeMillis()
                // a swipe may leave a fling running (hold the glow still for it); a plain tap leaves nothing behind, so the glow comes back almost at once
                Io.main.removeCallbacks(touchRelease); Io.main.postDelayed(touchRelease, if (dragged) 1500L else 350L)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        UpdateManager.removeListener(updateListener)
        kbProbe.detach()
        NetworkMonitor.removeListener(netListener)
        Io.main.removeCallbacks(saveRunnable)
        for (t in tabs) { try { t.session?.close() } catch (e: Exception) { /* already gone */ }; t.session = null }
        super.onDestroy()
        if (!isChangingConfigurations) { ProcessLife.uiAlive = false; ProcessLife.uiVisible = false; ProcessLife.maybeExit() } // swiped away: leave unless a download is still running
    }

    /**
     * "Clear browsing data when the app is closed": there is no Quit button any more (the user swipes the app away, and Android may end the process before any callback runs), so the
     * promise is kept at the next COLD start - the first screen of a new process - before anything is restored. Nothing is cleared when only the screen is re-created.
     */
    private fun coldStartClear() {
        if (ProcessLife.coldStartHandled) return
        ProcessLife.coldStartHandled = true
        if (!prefs.clearOnExit) return
        sessionRestored = false
        val appCtx = applicationContext
        File(filesDir, "session.txt").delete()
        TabStateStore.wipe(this)
        Io.exec { try { BrowserDb(appCtx).clearHistory() } catch (e: Exception) { /* ignore */ }; ThumbStore.clearAll(appCtx) }
        Io.main.post { try { GeckoEngine.runtime(this).storageController.clearData(StorageController.ClearFlags.ALL) } catch (e: Exception) { /* engine not ready: next time */ } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Health.log("win", "focus", hasFocus.toString())
        if (hasFocus && ::root.isInitialized) kbProbe.attach(root) // the keyboard probe needs the window token: have it ready before the first keystroke
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_DOWNLOADS, false)) showDownloads()
        handleUpdateIntent(intent)
        urlFromIntent(intent)?.let { openInNewTabOrCurrent(it) }
    }

    /** A tap on the update notification: open the Updates sheet; "Install" also starts the install (Android then asks for its one confirmation). */
    private fun handleUpdateIntent(i: Intent?) {
        if (i?.getBooleanExtra(UpdateNotifier.EXTRA_OPEN_UPDATES, false) != true) return
        i.removeExtra(UpdateNotifier.EXTRA_OPEN_UPDATES)
        val install = i.getBooleanExtra(UpdateNotifier.EXTRA_INSTALL_UPDATE, false)
        i.removeExtra(UpdateNotifier.EXTRA_INSTALL_UPDATE)
        Io.main.post {
            dismissOverlays() // a full-screen panel (Settings, Downloads...) would otherwise hide the sheet that opens next
            showUpdates()
            if (install && UpdateManager.state.phase == UpdateManager.Phase.READY) UpdateManager.install(this)
        }
    }

    /** Close every full-screen panel and the tab grid (not the page), so whatever opens next is on top and in plain sight. */
    private fun dismissOverlays() {
        if (sheet.isShowing) sheet.dismiss()
        if (downloadsView?.parent != null) closeDownloads()
        if (diagView?.parent != null) closeDiagnostics()
        if (settingsView?.parent != null) closeSettings()
        if (listPanel?.parent != null) closeList()
        if (tabGrid.visibility == View.VISIBLE) hideTabGrid()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        lastTrim = level; lastTrimAt = SystemClock.elapsedRealtime()
        Health.log("mem", "trim", "level=$level ${memFacts()}"); Health.count("mem.trim"); Health.count("mem.trim.$level")
        // 10/15 = RUNNING_LOW/CRITICAL (we are visible); 20 = UI_HIDDEN (the user left: a quick app switch is not a memory fact); 40+ = background; 60+ = we are a prime kill candidate.
        // LiveBudget.afterTrim decides how many tabs stay live from the level AND the memory the kernel reports as available (it used to close every other tab at any level >= 10).
        if (level >= 10) enforceLiveLimit(LiveBudget.afterTrim(level, Health.memAvailableMb(), liveBudget())) // a popup's opener must survive (window.opener): enforceLiveLimit skips those
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE && !window.decorView.hasWindowFocus()) current?.let { hibernate(it) }
    }

    // ================================================================== UI construction

    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(pal.windowBg)
        root.defaultFocusHighlightEnabled = false // clearUrlFocus() parks the focus on this view: without this the framework tints the WHOLE screen with its focus highlight
        Neon.mode = prefs.neon
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false } // the neon halo rises from the progress line into the page area
        dock = BottomDock(this, pal, this)
        progress = NeonBar(this, pal.accent, pal.accentHi)
        Neon.pulse(progress.halo, 0.3f, 1f, 3400L)
        content = FrameLayout(this)
        geckoView = KestrelGeckoView(this).apply { setAutofillEnabled(false) } // no logins are stored; GeckoView's autofill hooks only add window-focus churn (research/FREEZE.md 4.7)
        // ONE GeckoView, attached once and never removed: detaching it pauses/resumes the compositor through synchronous IPC while the keyboard resizes the window,
        // which is the race family behind "the screen stops updating while typing". Tabs swap with setSession; the Home screen and error screens are opaque overlays above it.
        content.addView(geckoView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        restoreOverlay = RestoreOverlay(this, pal)
        content.addView(restoreOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)) // above the page, below Home / error screens
        pullChip = PullChip(this, pal)
        content.addView(pullChip, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        noticeBar = app.kestrel.browser.ui.NoticeBar(this)
        content.addView(noticeBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(12); leftMargin = dp(12); rightMargin = dp(12) })
        geckoView.pull = pullListener
        geckoView.pullEnabled = {
            val t = current
            prefs.pullRefresh && t != null && !t.isNewTab && !t.showHome && t.error == null && t.session != null && !fullscreenOn && tabGrid.visibility != View.VISIBLE &&
                !sheet.isShowing && restoreOverlay.visibility != View.VISIBLE && !t.url.startsWith("resource://android/assets/scrolltest")
        }
        // The navigation bar is at the BOTTOM (owner request): page on top, a 2 dp load-progress line, then the dock.
        column.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        column.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)))
        column.addView(dock, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        home = HomeView(this, pal, "KESTREL", this)
        errorView = ErrorView(this, pal, errorListener)
        content.addView(home, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        content.addView(errorView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        home.visibility = View.GONE
        errorView.visibility = View.GONE
        findBar = FindBar(this, pal, object : FindBar.Listener {
            override fun query(q: String) { findIn(q, false) }
            override fun next(forward: Boolean) { findIn(findBar.input.text.toString(), !forward) }
            override fun close() { closeFind() }
        }).apply { visibility = View.GONE }
        root.addView(findBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(52))) // find stays at the TOP: the keyboard never covers it
        suggestions = SuggestionList(this, pal, reverse = true) { value -> hideSuggestions(); clearUrlFocus(); current?.let { navigate(it, value) } }.apply { visibility = View.GONE }
        root.addView(suggestions, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        tabGrid = TabGrid(this, pal, { it.isPrivateTab }, object : TabGrid.Listener {
            override fun select(tab: Tab) { hideTabGrid(); show(tab) }
            override fun close(tab: Tab) {
                doCloseTab(tab)
                tabGrid.rebind(tabs, current)
            }
            override fun newTab(privateTab: Boolean) { hideTabGrid(); newTabAndShow(privateTab) }
            override fun closeAll(privateTab: Boolean) {
                val n = tabs.count { it.isPrivateTab == privateTab }
                askYesNo(if (privateTab) "Close all private tabs?" else "Close all tabs?", "$n ${if (n == 1) "tab" else "tabs"} will be closed.", "Close", "Cancel") { yes ->
                    if (yes) { for (t in tabs.filter { it.isPrivateTab == privateTab }) doCloseTab(t); tabGrid.rebind(tabs, current) }
                }
            }
            override fun dismiss() { hideTabGrid() }
            override fun paneChanged(privateTab: Boolean) { applyPrivacyFlags() }
        }).apply { visibility = View.GONE }
        root.addView(tabGrid, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        sheet = SheetHost(this, pal)
        root.addView(sheet, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        // Downloads / Settings / History / Diagnostics cover every glow: the neon makes no frames while one of them is open (the glows are alpha-stepped at 10 Hz, each step redraws the window)
        root.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) { updateNeonCover() }
            override fun onChildViewRemoved(parent: View?, child: View?) { updateNeonCover() }
        })
        fullscreen = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()); visibility = View.GONE; isClickable = true }
        root.addView(fullscreen, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)

        // Edge-to-edge safe: Android 15+ draws behind the system bars for targetSdk 35+; Android 10 reports zero insets here.
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or (if (urlTyping) 0 else WindowInsets.Type.ime()))
                v.setPadding(i.left, i.top, i.right, i.bottom)
                WindowInsets.CONSUMED
            } else {
                @Suppress("DEPRECATION") v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                @Suppress("DEPRECATION") insets.consumeSystemWindowInsets()
            }
        }
    }

    // ================================================================== tabs

    private fun restore(session: Session?, saved: Bundle?, launch: String?, states: Map<Long, String> = emptyMap(), selThumb: android.graphics.Bitmap? = null) {
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
                states[r.id]?.let { t.gecko.stateJson = it; t.gecko.stateWrittenUrl = t.url }
                tabs.add(t)
                nextTabId = maxOf(nextTabId, r.id + 1)
            }
            val sel = tabs.firstOrNull { it.id == session.selectedId } ?: tabs.last()
            // Started by a link from another app: the link's page is what the owner wants NOW. Loading the saved page as well would compete with it for the slow link and the one content process
            // (the saved tab stays in the grid and loads when it is opened). The placeholder tab is gone, so nothing may still point at it.
            if (launch == null) { if (sel.thumb == null) sel.thumb = selThumb; show(sel) } else current = null
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
    /** The tab whose address IS this page (exact, fragment aside); null if none: callers fall back to [tabForPage]. */
    internal fun tabWithUrl(url: String): Tab? { val u = url.substringBefore('#'); if (u.isEmpty()) return null; return tabs.firstOrNull { it.url.substringBefore('#') == u } }

    internal fun tabForPage(url: String): Tab? {
        val h = UrlParts.hostOf(url) ?: return null
        current?.takeIf { it.topHost == h }?.let { return it }
        // Called from the Shields worker thread: never iterate the live list (the UI thread may be opening/closing a tab); a stale answer only costs one uncounted block.
        @Suppress("UNNECESSARY_SAFE_CALL", "SENSELESS_COMPARISON")
        return try { tabs.toTypedArray().firstOrNull { it != null && it.topHost == h } } catch (e: Exception) { null }
    }

    internal fun newTabAndShow(privateTab: Boolean = false): Tab { val t = newTabInternal(null, privateTab); show(t); focusUrlBar(); return t }

    /** Regular <-> private skin: the whole chrome (dock, Home, progress line, system bars) turns violet in private mode, so it can never be mistaken for a normal tab. */
    private var skinPrivate = false
    private var skinApplied = false
    private fun applySkin() {
        val priv = current?.isPrivateTab == true
        if (skinApplied && priv == skinPrivate) return
        skinApplied = true
        skinPrivate = priv
        pal = if (priv) palPrivate else palRegular
        dock.applyPal(pal); home.applyPal(pal); suggestions.setPal(pal); sheet.applyPal(pal); progress.setColors(pal.accent, pal.accentHi); restoreOverlay.setPal(pal); pullChip.setPal(pal)
        @Suppress("DEPRECATION") run {
            window.statusBarColor = if (priv) pal.chromeBg else pal.windowBg
            window.navigationBarColor = pal.chromeBg
        }
    }

    private fun openInNewTabOrCurrent(url: String) {
        val cur = current
        val t = if (cur != null && cur.isNewTab) cur else newTabInternal()
        show(t)
        navigateTo(t, url)
    }

    private fun show(tab: Tab) {
        val prev = current
        if (prev != null && prev !== tab) { prev.session?.setActive(false); tab.showHome = false } // coming back to a tab shows its page, not the Home overlay
        current = tab
        if (restoringTab != null && restoringTab !== tab) hideRestoring("other-tab") // another tab came to the front: the overlay belonged to the one that died
        tab.lastActive = System.currentTimeMillis()
        applySkin()
        attachContent(tab)
        tab.session?.setActive(true)
        applyPrivacyFlags()
        enforceLiveLimit()
        updateUi()
        scheduleSave()
    }

    /** Private tab in front: no screenshots / recents thumbnail, and no autofill service sees what is typed. */
    private var secureWindow = false

    /** DEBUG builds only (never a release): `adb shell setprop debug.kestrel.nosecure 1` lets screenshots show private tabs, so the private skin can be checked on an emulator. */
    private val debugAllowsScreenshots: Boolean by lazy {
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 && try {
            Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, "debug.kestrel.nosecure") == "1"
        } catch (e: Throwable) { false }
    }
    private fun applyPrivacyFlags() {
        val priv = (current?.isPrivateTab == true || (tabGrid.visibility == View.VISIBLE && tabGrid.showingPrivate)) && !debugAllowsScreenshots
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
        val homeNow = homeShown(tab)
        if (homeNow) { if (home.visibility != View.VISIBLE) { home.visibility = View.VISIBLE; home.field.setText("") }; refreshNtp() } // Home always starts with an empty field
        else if (home.visibility != View.GONE) { home.visibility = View.GONE; home.hideSuggestions() }
        val e = tab.error
        if (e != null && !homeNow) { errorView.show(e); if (errorView.visibility != View.VISIBLE) errorView.visibility = View.VISIBLE }
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

    private fun ensureSession(tab: Tab, cover: Boolean = true): GeckoSession {
        tab.session?.let { return it }
        val s = createSession(tab)
        s.open(GeckoEngine.runtime(this))
        tab.session = s
        var state = tab.gecko.state
        if (state == null) tab.gecko.stateJson?.let { state = stateFromDisk(tab, it) } // a tab that was saved by the last run: its Back stack and scroll position come back
        val pending = tab.pendingLoad
        when {
            state != null -> {
                tab.gecko.state = null
                tab.pendingLoad = null
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(tab.url))
                s.restoreState(state!!) // brings back the tab's whole history, not just its last page
            }
            pending != null -> {
                tab.pendingLoad = null
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(pending))
                tab.topHost = UrlParts.hostOf(pending)
                s.loadUri(pending)
            }
        }
        if (cover && (state != null || pending != null)) coverRevive(tab)
        enforceLiveLimit()
        return s
    }

    /**
     * A tab's page is being loaded again (revived after it was hibernated, or restored at start): until the engine paints, the empty page area shows the tab's last preview
     * (dimmed, with the host) instead of the previous tab's leftover frame or a blank panel. It goes away at the first paint, when the load ends, or after 20 s.
     */
    private fun coverRevive(tab: Tab) {
        if (tab !== current || restoringTab === tab || tab.showHome || tab.error != null || !tab.url.startsWith("http")) return
        showRestoring(tab, false, "Opening " + (UrlParts.hostOf(tab.url) ?: "page"))
    }

    /** Parse a tab's saved engine state (tabstate/<id>.json from the last run); null = unusable, the tab then loads its address as before. */
    private fun stateFromDisk(tab: Tab, json: String): GeckoSession.SessionState? {
        tab.gecko.stateJson = null
        val st = try { GeckoSession.SessionState.fromString(json) } catch (e: Throwable) { null }
        if (st == null) { Health.count("tabstate.unreadable"); return null }
        // only a state whose current page is the page the tab says it shows (session.txt and the state file are written together, but a cut-off can leave them apart)
        val i = st.currentIndex
        val uri = if (i in 0 until st.size) st[i].uri else null
        if (uri == null || withoutUserinfo(uri).substringBefore('#') != withoutUserinfo(tab.url).substringBefore('#')) {
            Health.count("tabstate.mismatch"); Health.log("tabs", "state-mismatch", "tab=${tab.id}")
            return null
        }
        Health.count("tabstate.restored"); Health.log("tabs", "state-restored", "tab=${tab.id} entries=${st.size} index=$i")
        tab.gecko.stateWritten = st
        return st
    }

    // ---------------------------------------------------------------- Back / Forward pictures (research/SPEED3.md 6.2)
    // Most Back presses are answered by the engine's own back/forward cache and are instant. A page that forbids caching (webmail, banking, anything with Cache-Control: no-store) or one the
    // cache no longer holds is LOADED again; until it paints, a recent picture of it covers the page area. The picture is taken at the moment the page is left (as the owner last saw it).
    private var lastLeaveShotAt = 0L
    private var backTarget: Triple<Tab, String, android.graphics.Bitmap>? = null
    private val backPictureShow = Runnable {
        val (t, _, pic) = backTarget ?: return@Runnable
        backTarget = null
        if (t !== current || !t.loading || t.error != null || t.showHome || restoringTab != null) return@Runnable // the engine had it already (cache), or the user moved on
        Health.count("back.picture"); Health.log("tabs", "back-picture", "tab=${t.id}")
        showRestoring(t, false, "", pic, 0x33)
    }

    internal fun goBack(t: Tab) { armBackPicture(t, -1); t.session?.goBack() }
    internal fun goForward(t: Tab) { armBackPicture(t, +1); t.session?.goForward() }

    private fun armBackPicture(t: Tab, delta: Int) {
        Io.main.removeCallbacks(backPictureShow); backTarget = null
        if (t !== current || t.session == null) return
        val st = t.gecko.state ?: return
        val i = st.currentIndex + delta
        if (i !in 0 until st.size) return
        val url = st[i].uri ?: return
        val pic = BackPictures.get(t.id, url) ?: return
        backTarget = Triple(t, url, pic)
        Io.main.postDelayed(backPictureShow, 150) // a cache hit is done by then; a page that has to load shows its picture
    }

    /** A navigation of the page in front is about to start (called before the engine loads [toUrl]): take its picture now, the way it is on screen, for a later Back. */
    internal fun leavingPage(tab: Tab, toUrl: String) {
        if (tab !== current || tab.isNewTab || tab.loading || tab.error != null || tab.showHome || !tab.url.startsWith("http")) return
        if (BackPictures.keyOf(toUrl) == BackPictures.keyOf(tab.url)) return // a reload or a #fragment: the same page
        val now = SystemClock.uptimeMillis()
        if (now - lastLeaveShotAt < 1500) return // a chain of redirects or quick taps: one picture is enough
        lastLeaveShotAt = now
        captureThumb(tab, forBack = true)
    }

    private fun hasLiveChild(t: Tab) = tabs.any { it.parentId == t.id && it.session != null }

    private fun detach(v: View) { (v.parent as? ViewGroup)?.removeView(v) }

    private fun updateNeonCover() {
        Neon.covered(listOf<View?>(downloadsView, diagView, settingsView, listPanel).any { it != null && it.parent === root })
    }

    /**
     * How many tabs keep a live engine session right now: follows the memory the kernel reports as available (2 to 8; core LiveBudget, tests in LiveBudgetTest) unless the
     * owner fixed a number in Settings > Tabs kept ready. The old rule was "2 on a low-RAM device", which made every third tab a reload.
     */
    private fun liveBudget(): Int {
        val am = getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        try { am?.getMemoryInfo(mi) } catch (e: Exception) { /* keep the zeros */ }
        val avail = Health.memAvailableMb().let { if (it >= 0) it else if (mi.availMem > 0) (mi.availMem shr 20).toInt() else -1 }
        return LiveBudget.limit(avail, am?.isLowRamDevice != false, mi.lowMemory, prefs.tabsKeptCount)
    }

    /** Keep at most [max] live engine sessions: the least recently used others are hibernated (the tab in front and any tab with a live pop-up child never are). */
    private fun enforceLiveLimit(max: Int = liveBudget()) {
        val live = tabs.filter { it.session != null }.sortedBy { it.lastActive }
        var count = live.size
        for (t in live) {
            if (count <= max) break
            if (t === current || hasLiveChild(t)) continue
            Health.count("tabs.hibernate"); Health.log("tabs", "hibernate", "tab=${t.id} live=$count max=$max avail=${Health.memAvailableMb()}MB")
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
        if (tab.isPrivateTab) { // a private tab keeps no history: its back stack and form data are dropped. The address stays (it is on screen and in the tab grid anyway) so the tab loads it again instead of coming back blank behind a full address bar
            tab.gecko.state = null
            if (tab.url.isNotEmpty()) tab.pendingLoad = tab.url
            return
        }
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
        if (tab === restoringTab) hideRestoring("closed")
        tab.thumb = null
        val appCtx = applicationContext
        Io.exec { ThumbStore.delete(appCtx, tab.id); TabStateStore.delete(appCtx, tab.id) }
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* ignore */ } }
        tab.session = null
        if (current === tab) {
            val next = tabs.firstOrNull { it.id == tab.parentId } ?: tabs.getOrNull(idx) ?: tabs.getOrNull(idx - 1)
            if (next == null) show(newTabInternal()) else show(next)
        } else updateUi()
        scheduleSave()
    }

    private fun showNewTabPage(tab: Tab) {
        tab.gecko.stateJson = null; tab.gecko.stateWritten = null; tab.gecko.stateWrittenUrl = null
        val appCtx = applicationContext; val tid = tab.id
        Io.exec { TabStateStore.delete(appCtx, tid) }
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* ignore */ } }
        tab.session = null
        tab.gecko.state = null
        tab.url = ""; tab.title = ""; tab.error = null; tab.pendingLoad = null; tab.loading = false; tab.progress = 0; tab.showHome = false
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
        tab.prevUrl = tab.url
        tab.pendingNav = url
        tab.showHome = false
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
        tab.media.clear()
        val s = ensureSession(tab, cover = false) // the address typed now replaces whatever the tab held: no preview of the old page
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
    override fun onForward() { current?.let { goForward(it) } }

    /** Home button: the black Home screen over the current page (the page stays alive underneath; Back returns to it). Tapped again: start typing. */
    override fun onHome() {
        val t = current ?: return
        if (homeShown(t)) { focusUrlBar(); return }
        hideKeyboard()
        t.showHome = true
        attachContent(t)
        updateUi()
    }

    /** The + button opens a tab of the CURRENT kind: private mode stays private until you pick a regular tab in the tab list. */
    override fun onNewTab() { newTabAndShow(isPrivate) }

    // ---- pull down to refresh (the finger tracking lives in KestrelGeckoView; here: the chip, the arming distance and the reload)
    private var pullArmed = false
    private val pullListener = object : KestrelGeckoView.PullListener {
        override fun pullStart() { pullArmed = false }
        override fun pullMove(dyPx: Float) {
            val arm = pullChip.context.dpf(PULL_ARM_DP)
            val travel = minOf(dyPx * 0.6f, pullChip.context.dpf(PULL_MAX_DP)) // the chip follows at 60 % of the finger: a pull has weight
            val armed = travel >= arm
            if (armed && !pullArmed) pullChip.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            pullArmed = armed
            pullChip.pull(travel, travel / arm, armed)
        }
        override fun pullEnd(released: Boolean) {
            val fire = released && pullArmed
            pullArmed = false
            pullChip.hide()
            if (fire) pullRefresh()
        }
    }

    private fun pullRefresh() {
        val t = current ?: return
        Health.log("pull", "refresh", t.url.take(60)); Health.count("pull.refresh")
        if (t.error != null) retry(t) else t.session?.reload()
    }

    override fun onReloadOrStop() {
        val t = current ?: return
        if (t.error != null) { retry(t); return }
        val s = t.session ?: return
        if (t.loading) { s.stop(); t.loading = false; t.progress = 0; updateUi() } else s.reload()
    }

    override fun onShields(anchor: View) {
        val t = current ?: return
        val host = t.topHost ?: UrlParts.hostOf(t.url) ?: return
        hideKeyboard()
        shieldsPanel().showIn(sheet, host, t, prefs.globalShields())
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

    override fun onTabs() {
        hideKeyboard()
        val cur = current
        tabGrid.visibility = View.VISIBLE
        tabGrid.open(tabs, cur)
        // fresh preview of the page in front (the others keep the one taken when they were last in front); previews of restored tabs come from the cache directory
        if (cur != null) captureThumb(cur)
        val appCtx = applicationContext
        val missing = tabs.filter { it.thumb == null && !it.isPrivateTab && !it.isNewTab }
        if (missing.isNotEmpty()) Io.fast { for (t in missing) { val b = ThumbStore.load(appCtx, t.id); if (b != null) Io.ui { if (t.thumb == null) { t.thumb = b; t.thumbAt = SystemClock.uptimeMillis(); tabGrid.thumbChanged(t) } } } }
    }

    private fun hideTabGrid() { tabGrid.visibility = View.GONE; applyPrivacyFlags() }

    /**
     * Snapshot the page in front for the tab grid: a GPU read-back (not a screenshot, so it also works under FLAG_SECURE), scaled down on the IO thread.
     * Only for a page that is really on screen - not the Home screen, an error screen or a page that is still loading.
     */
    private fun captureThumb(tab: Tab, forBack: Boolean = false) {
        if (tab !== current || homeShown(tab) || tab.error != null || tab.session == null || geckoView.session !== tab.session) return
        val appCtx = applicationContext
        try {
            geckoView.capturePixels().accept({ full ->
                val lane: (() -> Unit) -> Unit = if (tabGrid.visibility == View.VISIBLE) Io::fast else Io::exec // the grid is open: the user is waiting for this preview
                if (full != null) lane {
                    val small = ThumbStore.shrink(full)
                    if (small != null) {
                        if (!tab.isPrivateTab) ThumbStore.save(appCtx, tab.id, small)
                        Io.ui { tab.thumb = small; tab.thumbAt = SystemClock.uptimeMillis(); ThumbStore.trim(tabs, tab); if (tabGrid.visibility == View.VISIBLE) tabGrid.thumbChanged(tab) }
                    }
                }
            }, { _ -> /* the surface was not ready: keep the previous preview */ })
        } catch (e: Throwable) { /* no session attached right now */ }
    }

    override fun onSubmit(text: String) {
        hideSuggestions(); clearUrlFocus()
        current?.let { navigate(it, text) }
    }

    // ---- typing in the address field / the Home field

    /** The Home screen is showing (new tab, or the Home button was pressed): its big field is the one to type in. */
    private fun homeShown(t: Tab? = current): Boolean = t != null && (t.isNewTab || t.showHome)
    private val activeField: EditText get() = if (homeShown()) home.field else dock.urlField
    private fun typing(): Boolean = dock.urlField.hasFocus() || home.field.hasFocus()

    /** True while an address field has (or just had) the keyboard: the keyboard's insets must not shrink the page then (API 30+ sends them whatever the soft-input mode). */
    @Volatile private var urlTyping = false

    /** While the address bar is being typed in, the page view must NOT resize under the keyboard (compositor resize while typing = the freeze race); pages keep adjustResize. */
    private fun setUrlTypingMode(on: Boolean) {
        val lp = window.attributes
        val want = if (on) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        if ((lp.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != want) {
            window.setSoftInputMode((lp.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or want)
            Health.log("win", "typing-mode", on.toString())
        }
        if (urlTyping != on) { urlTyping = on; if (Build.VERSION.SDK_INT >= 30) root.requestApplyInsets() }
    }

    private var typingModeSince = 0L
    private var focusedAt = 0L
    private var kbTopScreen = 0 // top edge of the keyboard in screen pixels while it is up, else 0
    private val kbProbe by lazy { KeyboardProbe(this) }

    /** Keyboard top edge in screen pixels, 0 = no keyboard (see [KeyboardProbe]: the window itself cannot tell under adjustNothing). */
    private fun keyboardTop(): Int { kbProbe.attach(root); return kbProbe.topScreen(window) }

    /**
     * Follows the keyboard while a field is typed in and until it is gone again (at most 1.5 s after focus is lost): lifts the dock so the address row sits right above
     * it, keeps the suggestion list and the Home field in view. Back to adjustResize only AFTER the keyboard is gone (else the page shrinks once more under the closing keyboard).
     */
    private var lastKbTop = -1
    private var kbStillTicks = 0
    private val typingTick = object : Runnable {
        override fun run() {
            val focused = typing()
            val top = keyboardTop()
            val up = top > 0
            applyKeyboard(top)
            // every look is a call into the window manager on Android 10: 100 ms while the keyboard moves, 300 ms once it has sat still for a second
            if (top == lastKbTop) kbStillTicks++ else { lastKbTop = top; kbStillTicks = 0 }
            if (focused) { Io.main.postDelayed(this, if (kbStillTicks >= 10) 300L else 100L); return }
            if (up && SystemClock.uptimeMillis() - typingModeSince < 1500) { Io.main.postDelayed(this, 80); return }
            applyKeyboard(0)
            setUrlTypingMode(false)
        }
    }

    /** [topScreen]: the keyboard's top edge in screen pixels, 0 = no keyboard. Moves views with translationY only - nothing is re-laid-out, the page keeps its size. */
    private fun applyKeyboard(topScreen: Int) {
        kbTopScreen = topScreen
        updateNeonBusy()
        val loc = IntArray(2)
        var lift = 0
        if (topScreen > 0 && !homeShown()) {
            column.getLocationOnScreen(loc)
            val bandBottom = loc[1] + column.height - dock.actionRowPx // bottom edge of the address row while the dock is not lifted
            lift = maxOf(0, bandBottom - topScreen)
        }
        if (dock.translationY != -lift.toFloat()) dock.translationY = -lift.toFloat()
        if (topScreen > 0 && homeShown()) { home.getLocationOnScreen(loc); home.onKeyboard(topScreen - loc[1]) } else home.onKeyboard(0)
        positionSuggestions()
    }

    /** The dock's suggestion list sits right on top of the address row; the best match is the row nearest to the bar. */
    private fun positionSuggestions() {
        if (suggestions.visibility != View.VISIBLE) return
        val lp = suggestions.layoutParams as FrameLayout.LayoutParams
        val dockTop = dock.top + dock.translationY.toInt() // relative to `column`
        val bottomMargin = (column.height - dockTop).coerceAtLeast(0)
        val h = minOf(suggestions.contentHeightPx(), (dockTop - dp(12)).coerceAtLeast(dp(56)))
        if (lp.bottomMargin != bottomMargin || lp.height != h) { lp.bottomMargin = bottomMargin; lp.height = h; suggestions.layoutParams = lp }
    }

    private fun homeKeyboardTop(): Int {
        if (kbTopScreen <= 0) return 0
        val loc = IntArray(2); home.getLocationOnScreen(loc)
        return kbTopScreen - loc[1]
    }

    override fun onUrlFocus(focused: Boolean) {
        Io.main.removeCallbacks(typingTick)
        if (focused) { setUrlTypingMode(true); focusedAt = SystemClock.uptimeMillis(); updateNeonBusy() } else typingModeSince = SystemClock.uptimeMillis()
        Io.main.post(typingTick)
        if (focused) {
            if (!homeShown()) { dock.urlField.setText(dock.fullUrlForEditing(current)); dock.urlField.selectAll() }
        } else {
            hideSuggestions()
            updateUi()
        }
    }

    private val suggestLater = Runnable { runSuggest() }

    override fun onUrlText(text: String) {
        Io.main.removeCallbacks(suggestLater)
        if (text.trim().isEmpty()) runSuggest() else Io.main.postDelayed(suggestLater, 70) // the user is still typing: the database is asked once for a burst of keys
    }

    private fun runSuggest() {
        val text = if (homeShown()) home.field.text.toString() else dock.urlField.text.toString()
        val q = text.trim()
        val toHome = homeShown()
        if (q.isEmpty() || (!toHome && q == current?.url)) { hideSuggestions(); return }
        val seq = ++suggestSeq
        val dbRef = db
        fun showRows(entries: List<Entry>) {
            if (seq != suggestSeq || !typing()) return
            entries.firstOrNull()?.let { preconnect(it.url) }
            if (toHome) home.bindSuggestions(q, entries, home.suggestionRoom(homeKeyboardTop()))
            else { suggestions.bind(q, entries); suggestions.visibility = View.VISIBLE; positionSuggestions() }
        }
        if (dbRef == null) { showRows(emptyList()); return }
        Io.fast { val entries = dbRef.suggest(q, 6); Io.ui { showRows(entries) } }
    }

    private var warmHost = ""
    private var warmAt = 0L

    /**
     * Opens the connection (DNS, TCP, TLS) to the site the top suggestion names while the owner is still typing, so the page's first bytes do not wait for the handshakes (about 1-3 round trips,
     * a second or more on a hotspot). Sites from history and bookmarks only, https only, never from a private tab, once per host per 30 s; Settings > Network lab can switch it off.
     */
    private fun preconnect(url: String) {
        if (!prefs.preconnect || current?.isPrivateTab == true || !url.startsWith("https://", true)) return
        val host = UrlParts.hostOf(url) ?: return
        val now = SystemClock.uptimeMillis()
        if (host == warmHost && now - warmAt < 30_000) return
        warmHost = host; warmAt = now
        Io.exec { try { GeckoEngine.executorOrNull()?.speculativeConnect("https://$host/") } catch (e: Throwable) { /* only a head start */ } }
    }

    private fun hideSuggestions() { suggestSeq++; suggestions.visibility = View.GONE; home.hideSuggestions() }
    private fun focusUrlBar() { val f = activeField; f.requestFocus(); getSystemService(InputMethodManager::class.java)?.showSoftInput(f, 0) }
    private fun clearUrlFocus() {
        dock.urlField.clearFocus(); home.field.clearFocus()
        root.isFocusableInTouchMode = true; root.requestFocus() // else the framework hands the focus (and the keyboard) to the other text field
        hideKeyboard()
    }
    private fun hideKeyboard() { getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(root.windowToken, 0) }

    // ---- menu
    private object M { const val REFRESH = 14; const val DIAG = 15; const val NEW = 1; const val PRIVATE = 2; const val FIND = 3; const val SHARE = 4; const val BOOKMARK = 5; const val BOOKMARKS = 6; const val HISTORY = 7
        const val DOWNLOADS = 8; const val UPDATE = 17; const val VIDEO = 18; const val DESKTOP = 9; const val IMAGES = 10; const val PRINT = 11; const val SETTINGS = 12; const val CLOSE_ALL = 16; const val PHONE = 19 }

    override fun onMenu(anchor: View) {
        val t = current
        val items = ArrayList<MenuItemSpec>()
        val updateHot = UpdateManager.pending()
        if (updateHot) items.add(MenuItemSpec(M.UPDATE, "Update available", null, R.drawable.ic_update, accent = true)) // first, so it is noticed
        if (video.available(t)) items.add(MenuItemSpec(M.VIDEO, "Download video", null, R.drawable.ic_download, accent = true)) // the page played something that can be saved
        items.add(MenuItemSpec(M.NEW, "New tab", null, R.drawable.ic_plus))
        items.add(MenuItemSpec(M.PRIVATE, "New private tab", null, R.drawable.ic_mask))
        if (t != null && !t.isNewTab) {
            items.add(MenuItemSpec(M.FIND, "Find in page", null, R.drawable.ic_search))
            items.add(MenuItemSpec(M.SHARE, "Share", null, R.drawable.ic_share))
            if (!isPrivate) items.add(MenuItemSpec(M.BOOKMARK, "Add bookmark", null, R.drawable.ic_star))
            items.add(MenuItemSpec(M.DESKTOP, "Desktop site", t.desktop, R.drawable.ic_desktop))
        }
        if (!isPrivate) { items.add(MenuItemSpec(M.BOOKMARKS, "Bookmarks", null, R.drawable.ic_bookmarks)); items.add(MenuItemSpec(M.HISTORY, "History", null, R.drawable.ic_history)) }
        val dl = DownloadController.counts()
        val dlActive = dl[0] + dl[1]
        items.add(MenuItemSpec(M.DOWNLOADS, if (dlActive > 0) "Downloads ($dlActive active)" else "Downloads", null, R.drawable.ic_download, accent = dlActive > 0))
        // the one switch for "hotspot only, the phone does nothing" (saves the phone's battery); before a phone is paired it leads to the pairing screen
        // closed feature (research/PHONE_OFFLOAD.md section 17): the tile only appears once a phone has been paired; before that Settings is the one place that mentions it
        if (PhoneBridge.paired(this)) {
            val helperOn = PhoneBridge.mode(this) != PhoneMode.OFF
            items.add(MenuItemSpec(M.PHONE, if (helperOn && NetworkMonitor.vpn) "Phone helper: resting (Fast Wi-Fi)" else if (helperOn) "Phone helper: on" else "Phone helper: off", helperOn, R.drawable.ic_phone))
        }
        if (t != null && !t.isNewTab) items.add(MenuItemSpec(M.REFRESH, "Refresh screen", null, R.drawable.ic_refresh))
        if (!isPrivate) items.add(MenuItemSpec(M.SETTINGS, "Settings", null, R.drawable.ic_settings))
        items.add(MenuItemSpec(M.DIAG, "Diagnostics", null, R.drawable.ic_pulse))
        if (!updateHot) items.add(MenuItemSpec(M.UPDATE, "Updates", null, R.drawable.ic_update))
        if (tabs.size > 1) items.add(MenuItemSpec(M.CLOSE_ALL, "Close all tabs", null, R.drawable.ic_close))
        hideKeyboard()
        val cols = if (resources.displayMetrics.widthPixels >= dp(700)) 3 else 2
        sheet.show(null, menuGrid(this, pal, items, cols) { id -> sheet.dismiss(); onMenuPick(id) })
    }

    private fun onMenuPick(id: Int) {
        val t = current
        when (id) {
            M.REFRESH -> refreshScreen("menu")
            M.DIAG -> showDiagnostics()
            M.UPDATE -> showUpdates()
            M.VIDEO -> t?.let { video.show(it) }
            M.NEW -> newTabAndShow()
            M.PRIVATE -> newTabAndShow(true)
            M.FIND -> openFind()
            M.SHARE -> t?.takeIf { !it.isNewTab }?.let { share(it.url) }
            M.BOOKMARK -> t?.takeIf { it.url.isNotEmpty() }?.let { tab ->
                Io.exec { db?.let { d -> if (d.isBookmarked(tab.url)) { d.removeBookmark(tab.url); Io.ui { toast("Bookmark removed") } } else { d.addBookmark(tab.url, tab.title); Io.ui { toast("Bookmark added") } } } }
            }
            M.BOOKMARKS -> showList(true)
            M.HISTORY -> showList(false)
            M.DOWNLOADS -> showDownloads()
            M.PHONE -> if (!PhoneBridge.paired(this)) showSettings() else toast(if (PhoneBridge.toggleHelper(this)) "Phone helper on: downloads and pages use your phone" else "Phone helper off: hotspot only, the phone does nothing")
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
        }
    }

    /**
     * The daily look's news as a dialog (owner request X2): a new app version, a newer browser engine from Mozilla, and a line about the ad-blocking lists, with an Update button that downloads
     * in the background and, once the file is checked, an Install button. Shown once per set of news ([app.kestrel.core.update.UpdatePlan]); never while typing or over a sheet.
     */
    private fun maybeShowUpdateNotice() {
        if (isFinishing || isDestroyed || updateNoticeShowing || !prefs.autoUpdateCheck) return
        val st = UpdateManager.state
        // the owner pressed Update / Install in the dialog and it ran into something (no room, Android's "allow from this source", an error): the Updates sheet explains it and offers the way on
        if (updateFromDialog && (st.phase == UpdateManager.Phase.NEEDS_SPACE || st.phase == UpdateManager.Phase.NEEDS_PERMISSION || st.phase == UpdateManager.Phase.ERROR)) { updateFromDialog = false; if (!sheet.isShowing) showUpdates(); return }
        if (updateFromDialog && (st.phase == UpdateManager.Phase.INSTALLING || st.phase == UpdateManager.Phase.READY)) updateFromDialog = false
        val o = st.offer
        val news = when {
            o != null && st.phase == UpdateManager.Phase.AVAILABLE -> app.kestrel.core.update.UpdatePlan.AppNews(o.tag, o.versionName, app.kestrel.core.update.UpdateLogic.formatSize(o.size), false)
            o != null && st.phase == UpdateManager.Phase.READY -> app.kestrel.core.update.UpdatePlan.AppNews(o.tag, o.versionName, app.kestrel.core.update.UpdateLogic.formatSize(o.size), true)
            else -> null
        }
        if (st.phase != UpdateManager.Phase.AVAILABLE && st.phase != UpdateManager.Phase.READY && st.phase != UpdateManager.Phase.UP_TO_DATE && st.phase != UpdateManager.Phase.IDLE) return
        if (urlTyping || sheet.isShowing || tabGrid.visibility == View.VISIBLE || fullscreenOn) { Io.main.postDelayed({ maybeShowUpdateNotice() }, 20_000); return }
        val notice = app.kestrel.core.update.UpdatePlan.build(Flavor.title, UpdateManager.installedName(this), news, Flavor.engineVersion, UpdateManager.engineNewest(this), updateListsLine(), prefs.updateNoticeKey.ifEmpty { null }) ?: return
        prefs.updateNoticeKey = notice.key
        updateNoticeShowing = true
        val b = AlertDialog.Builder(this).setTitle(notice.title).setMessage(notice.lines.joinToString("\n\n"))
        when (notice.action) {
            app.kestrel.core.update.UpdatePlan.Action.DOWNLOAD -> b
                .setPositiveButton("Update") { _, _ -> updateFromDialog = true; UpdateManager.download(this); toast("Downloading in the background. You get a notification when it is ready to install.") }
                .setNegativeButton("Later", null).setNeutralButton("Details") { _, _ -> showUpdates() }
            app.kestrel.core.update.UpdatePlan.Action.INSTALL -> b
                .setPositiveButton("Install now") { _, _ -> updateFromDialog = true; UpdateManager.install(this) }.setNegativeButton("Later", null)
            app.kestrel.core.update.UpdatePlan.Action.NONE -> b.setPositiveButton("OK", null)
        }
        b.setOnDismissListener { updateNoticeShowing = false }.show()
    }

    /** "Ad-blocking lists: refreshed 3 h ago (99,120 rules)." when the lists were looked at within the last day and a half; otherwise null. */
    private fun updateListsLine(): String? {
        val t = prefs.lastUpdateMs
        if (t == 0L || System.currentTimeMillis() - t > 36L * 3600 * 1000) return null
        val rules = Engine.peek()?.meta?.totalNetwork ?: return null
        val m = (System.currentTimeMillis() - t) / 60_000
        val ago = when { m < 1 -> "just now"; m < 60 -> "$m min ago"; else -> "${m / 60} h ago" }
        return "Ad-blocking lists: refreshed $ago (${"%,d".format(rules)} rules)."
    }

    /** The Updates sheet (GitHub Releases): check, download (asks first on a metered link), install through Android's installer. */
    private fun showUpdates() {
        val panel = UpdatePanel(this, pal, prefs, object : UpdatePanel.Actions {
            override fun openPage(url: String) { sheet.dismiss(); val t = newTabInternal(null, isPrivate); show(t); navigateTo(t, url) }
            override fun download() {
                val o = UpdateManager.state.offer ?: return
                val size = app.kestrel.core.update.UpdateLogic.formatSize(o.size)
                if (NetworkMonitor.metered) askYesNo("Download over a metered connection?", "This update is $size and the connection looks metered (a phone hotspot). Download it now?", "Download", "Not now") { yes -> if (yes) UpdateManager.download(this@BrowserActivity) }
                else UpdateManager.download(this@BrowserActivity)
            }
            override fun openInstallSettings() {
                try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) } catch (e: Exception) { toast("Open Settings > Apps > Special access > Install unknown apps") }
            }
            override fun openStorageSettings() {
                try { startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } catch (e: Exception) { toast("Open Settings > Storage") }
            }
        })
        sheet.show("Updates", panel)
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
        Io.fast {
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
        out.add("Engine: GeckoView $ver; live sessions ${tabs.count { it.session != null }} of ${tabs.size} tabs (the memory budget keeps up to ${liveBudget()} ready, MemAvailable ${Health.memAvailableMb()} MB, setting ${listOf("automatic", "few", "many")[prefs.tabsKept]}); current ${current?.url?.take(60) ?: "-"}")
        out.add("Network: ${GeckoEngine.networkProfile} (VPN active: ${NetworkMonitor.vpn})")
        out.add(GeckoEngine.prefReadback)
        out.add(GeckoEngine.prefReadback2)
        out.add(GlInfo.text)
        out.add(app.kestrel.browser.diag.StorageInfo.text)
        out.add(ShieldsBridge.statsLine(prefs.engineInExtension))
        val animScale = try { android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) } catch (e: Exception) { 1f }
        out.add("Neon glow: ${Neon.status()}; system animation scale ${if (animScale == 0f) "off (the glow ignores it)" else "x$animScale"}")
        out.add("Page view: attached=${geckoView.parent != null} session=${geckoView.session != null} windowFocus=${hasWindowFocus()}")
        return out
    }

    private fun showDiagnostics() {
        GeckoEngine.readBackPrefs { diagView?.refresh() }
        Io.exec { GlInfo.read(); app.kestrel.browser.diag.StorageInfo.read(this); Io.ui { diagView?.refresh() } }
        val v = diagView ?: DiagnosticsView(this, pal, ::engineFacts, { refreshScreen("diagnostics") }, { closeDiagnostics() }, { runScrollTest() }, { runScrollTest(compare = true) }, ::blockerLabel, ::toggleBlocker).also { diagView = it }
        if (v.parent == null) root.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)) else v.refresh()
    }

    private fun closeDiagnostics() { diagView?.let { detach(it) } }

    private fun blockerLabel(): String = if (prefs.engineInExtension) "Blocker: fast (tap for compatibility)" else "Blocker: compatibility (tap for fast)"

    /** Flips where ad-blocking decisions are made; the extension learns it with the next page's `cfg` answer, so it applies from the next page load. */
    private fun toggleBlocker() {
        prefs.engineInExtension = !prefs.engineInExtension
        Health.log("blocker", if (prefs.engineInExtension) "fast mode on" else "compatibility mode on")
        android.widget.Toast.makeText(this, if (prefs.engineInExtension) "Fast blocker: applies from the next page" else "Compatibility blocker: applies from the next page", android.widget.Toast.LENGTH_SHORT).show()
    }

    private var scrollTestTab: Tab? = null

    /**
     * Diagnostics > Scroll test: a temporary tab with the bundled long page ([ScrollTest.URL]); once it has loaded, [ScrollTest] scrolls it and reports the frames the
     * compositor produced. The tab is closed afterwards, the numbers are shown and logged (Copy/Share on the Diagnostics page includes them).
     */
    private fun runScrollTest(compare: Boolean = false) {
        closeDiagnostics()
        compareLevels = if (compare) ArrayDeque(listOf(0, 1, 2)) else null
        compareResults.clear(); compareOriginal = prefs.scrollFeel
        val t = newTabInternal()
        t.url = ScrollTest.URL; t.pendingLoad = ScrollTest.URL
        scrollTestTab = t
        show(t)
        toast(if (compare) "Comparing the three scroll feels (about 90 s): loading the test page..." else "Scroll test: loading the test page...")
    }

    /** "Compare 3 feels": the same test run at Standard, Smoother and Smoothest on the same page, one after the other (the prefs are live), then the user's own setting is put back. */
    private var compareLevels: ArrayDeque<Int>? = null
    private val compareResults = ArrayList<String>()
    private var compareOriginal = 0

    private fun startScrollTest(tab: Tab) {
        val s = tab.session
        if (s == null || current !== tab || isFinishing || isDestroyed) { endCompare(); doCloseTab(tab); toast("Scroll test could not start"); return }
        val queue = compareLevels
        val level = queue?.removeFirstOrNull()
        if (level != null) GeckoEngine.applyScrollFeel(level)
        toast(if (level == null) "Scroll test running (about 30 s): do not touch the screen" else "Scroll feel ${3 - (queue?.size ?: 0)} of 3: ${GeckoEngine.scrollFeelNames[level]} - do not touch the screen")
        Health.log("scrolltest", "start", "level=${level ?: "own"} " + memFacts())
        ScrollTest(s, window.decorView, { memFacts() }) { report ->
            Health.log("scrolltest", "result", "level=${level ?: "own"} " + report.lineSequence().take(3).joinToString(" | "))
            if (level != null) {
                compareResults.add(GeckoEngine.scrollFeelNames[level] + ":\n  " + report.lineSequence().filter { it.startsWith("Result:") || it.startsWith("Compositor frames:") }.joinToString("\n  "))
                if (queue != null && queue.isNotEmpty() && !isFinishing && !isDestroyed && current === tab) { Io.main.postDelayed({ startScrollTest(tab) }, 2500); return@ScrollTest }
            }
            val text = if (level != null) { endCompare(); compareResults.joinToString("\n\n") + "\n\nSmoother is what Kestrel G starts with. Settings > Appearance > Scroll feel switches it; your own setting is back in place now." } else report
            doCloseTab(tab)
            if (isFinishing || isDestroyed) return@ScrollTest
            android.app.AlertDialog.Builder(this).setTitle(if (level != null) "Scroll feel comparison" else "Scroll test").setMessage(text)
                .setPositiveButton("OK", null)
                .setNeutralButton("Copy") { _, _ ->
                    (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Kestrel scroll test", text))
                    toast("Copied")
                }
                .show()
        }.start()
    }

    private fun endCompare() { if (compareLevels != null) { compareLevels = null; GeckoEngine.applyScrollFeel(compareOriginal) } }

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
        override fun testNeon() {
            closeSettings(); Neon.mode = prefs.neon
            Io.main.postDelayed({ Neon.test(10_000); toast("Watch the glow around the address bar and above the dock for 10 seconds") }, 300)
        }
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
        val appCtx = applicationContext
        for (t in tabs) { t.gecko.stateJson = null; t.gecko.stateWritten = null; t.gecko.stateWrittenUrl = null } // the saved Back stacks are browsing data too
        Io.exec { db?.clearHistory(); ThumbStore.clearAll(appCtx); TabStateStore.wipe(appCtx) }
        for (t in tabs) t.thumb = null // the previews are page contents too
        toast("Browsing data cleared")
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
        // The engine's own state of each tab (history + scroll position) goes next to the addresses, so a swipe-away or a process kill restores more than the last address.
        // toString() runs here on the UI thread (the state objects belong to it; about 1-3 ms for a typical tab) and only for states that changed since they were last written.
        val appCtx = applicationContext
        val writes = ArrayList<Pair<Long, String>>(); val stale = ArrayList<Long>(); val keepIds = HashSet<Long>()
        for (t in keep) {
            keepIds.add(t.id)
            val g = t.gecko; val st = g.state
            if (st != null) {
                if (st !== g.stateWritten) {
                    val raw = try { st.toString() } catch (e: Throwable) { null }
                    if (raw != null) writes.add(t.id to raw)
                    g.stateWritten = st; g.stateWrittenUrl = t.url
                }
            } else if (g.stateWritten != null && g.stateWrittenUrl != t.url) { stale.add(t.id); g.stateWritten = null } // it shows another page now and the engine has not sent a newer state yet
        }
        Io.exec {
            val tmp = File(f.parentFile, "session.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            for ((id, raw) in writes) {
                val clean = TabStateStore.sanitize(raw)
                if (clean != null) TabStateStore.write(appCtx, id, clean, Flavor.engineVersion) else TabStateStore.delete(appCtx, id)
            }
            for (id in stale) TabStateStore.delete(appCtx, id)
            TabStateStore.retain(appCtx, keepIds)
        }
    }

    private fun flushCounters() {
        var n = 0L
        for (t in tabs) n += t.pendingLifetime.getAndSet(0)
        if (n > 0 && !isPrivate) prefs.blockedTotal = prefs.blockedTotal + n
    }

    private fun refreshNtpIfShown() { if (homeShown()) refreshNtp() }

    /** The Home screen has one data-driven line: what Shields blocked so far. */
    private fun refreshNtp() { home.bindStats(prefs.blockedTotal, NetworkMonitor.metered) }

    private var lastVpn = NetworkMonitor.vpn

    private fun onNetworkChanged() {
        if (NetworkMonitor.vpn != lastVpn) {                          // LinkMeter's Fast Wi-Fi came up or went away: DNS and HTTP/3 follow it (core/net/NetProfile), the Courier rests or returns
            lastVpn = NetworkMonitor.vpn
            GeckoEngine.applyNetwork(prefs)
            PhoneBridge.modeChanged(this)
        }
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
        val priv = t?.isPrivateTab == true
        dock.bind(t, tabs.count { it.isPrivateTab == priv }, canBack || (t != null && (!t.isNewTab || t.showHome)), homeShown(t))
        val p = if (t?.loading == true) t.progress else 0
        if (progress.progress != p) progress.progress = p
        updateNeonBusy()
        refreshDownloadButton()
    }

    /**
     * The download icon in the dock (owner request X17): gray while the page in front has nothing to save, neon blue-teal and breathing while it has. Runs on every UI refresh, so the cheap
     * checks come first and a page that has seen no media never builds a list.
     */
    internal fun refreshDownloadButton() {
        val t = current
        val n = if (t == null || t.isNewTab || t.media.isEmpty()) 0 else t.media.usableCount()
        dock.setDownloadCount(n)
    }

    /** The dock's download icon was tapped: the list of what the page offers, or a hint when nothing was found yet. */
    override fun onDownload() {
        val t = current ?: return
        if (t.isNewTab) { toast("Open a page with a video first."); return }
        video.show(t)
    }

    /**
     * The neon holds still while the screen is already changing: a page is loading, or the keyboard is up (or on its way, 1.2 s after a field got focus). A field that
     * merely HAS focus with the keyboard hidden is not typing: that state (new tab, keyboard dismissed with the keyboard's own back key) used to freeze the glow for good.
     */
    private fun updateNeonBusy() {
        val now = SystemClock.uptimeMillis()
        val t = current
        val keyboardComing = typing() && now - focusedAt < 1200
        // a load keeps the glow still only for its first 3 s (parsing, layout, first paint: the CPU-heavy part); a page that keeps loading for a long time on the hotspot is mostly waiting
        if (t?.loading != true) loadSeen = null
        else if (loadSeen !== t) { loadSeen = t; loadSeenAt = now; Io.main.postDelayed({ updateNeonBusy() }, 3100) }
        val heavyLoad = t?.loading == true && now - loadSeenAt < 3000
        Neon.busy(heavyLoad || kbTopScreen > 0 || keyboardComing)
    }
    private var loadSeen: Tab? = null
    private var loadSeenAt = 0L

    private var gridRebindPending = false
    internal fun tabChanged(tab: Tab) {
        if (tab === current) updateUi()
        // the open tab grid is rebuilt (every card is a handful of views) at most once per turn of the message loop, however many progress / title / address events arrive
        if (tabGrid.visibility == View.VISIBLE && !gridRebindPending) { gridRebindPending = true; Io.main.post { gridRebindPending = false; if (tabGrid.visibility == View.VISIBLE) tabGrid.rebind(tabs, current) } }
    }
    private val countPosted = java.util.concurrent.atomic.AtomicBoolean(false)
    /** A request was blocked (may be called from any thread): the chip refreshes at most 4 times a second, however many requests a page blocks. */
    internal fun shieldCountChanged(tab: Tab) {
        if (tab === current && countPosted.compareAndSet(false, true)) Io.main.postDelayed({ countPosted.set(false); updateUi() }, 250)
    }
    internal fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }

    /**
     * The pop-up and redirect guard refused something ([kind]: "open" a window, "click" a script's click on a link, "overlay" an invisible link, "form" a form that wanted a new window, "redirect" a jump of this
     * tab to another site). Counted with the page's blocked requests, and a notice offers to open it anyway (the guard cannot know every window the user really wanted). UI thread.
     */
    internal fun onPopupBlocked(pageUrl: String, kind: String, url: String) {
        // attribution by the exact page address first (several tabs can be on the same host), then by host, then the tab in front
        val tab = (if (pageUrl.isNotEmpty()) tabWithUrl(pageUrl) ?: tabForPage(pageUrl) else null) ?: current ?: return
        if (kind == "tap") { tab.lastTapAt = SystemClock.uptimeMillis(); tab.lastTapHref = url; return } // a real tap in the page, and the link it was on: the jump guard's evidence
        if (kind == "vmenu") { // a long press on a video or audio element, taken by the page script before the site's own player menu could appear (owner request X14)
            if (tab === current && !isFinishing) video.longPress(tab, url.takeIf { it.startsWith("http", true) }, null) { link -> val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigateTo(t, link) }
            return
        }
        tab.blocked.addAndGet(app.kestrel.core.filter.Category.ANNOYANCE, 1)
        tab.pendingLifetime.addAndGet(1)
        shieldCountChanged(tab)
        Health.count("guard.$kind")
        if (tab !== current) return
        val host = UrlParts.hostOf(url) ?: url.take(40)
        val what = if (kind == "redirect") "Stopped a jump to $host" else "Blocked a pop-up ($host)"
        val canOpen = url.startsWith("http://") || url.startsWith("https://")
        noticeBar.show(pal, what, if (canOpen) "Open" else null, 7000L) {
            if (kind == "redirect") loadInTab(tab, url) else { val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigate(t, url) }
        }
    }

    /**
     * Should a page that is about to send THIS tab to [uri] be stopped? See [app.kestrel.core.guard.JumpGuard]: only a jump to another site right after a real tap that was not on a link to it.
     * Called on the UI thread from onLoadRequest, before the navigation starts.
     */
    internal fun judgeJump(tab: Tab, uri: String): app.kestrel.core.guard.JumpGuard.Verdict {
        val from = tab.topHost
        val to = UrlParts.hostOf(uri)
        if (!prefs.blockPopups || from == null || to == null || !Shields.effective(prefs, from).shieldsUp) return app.kestrel.core.guard.JumpGuard.Verdict.ALLOW
        val now = SystemClock.uptimeMillis()
        return app.kestrel.core.guard.JumpGuard.decide(
            crossSite = !app.kestrel.core.filter.Psl.default.isSameSite(from, to),
            touchAgeMs = now - lastTouchAt,
            tapArrived = tab.lastTapAt >= lastTouchAt - 50,
            tapAgeMs = now - tab.lastTapAt,
            tapHref = tab.lastTapHref,
            uri = uri,
        )
    }

    /**
     * A page asks for a NEW window (a link with target=_blank, window.open): true = refuse it. The page-side guard (guard-main.js) already refuses what a script opens without a real tap on that very link; this
     * is the second wall for what reaches the engine anyway: a window with no tap behind it (the engine's own blocker lets one open for several seconds after any tap) or one that a `$popup` rule names.
     */
    internal fun refusePopup(tab: Tab, uri: String, hasUserGesture: Boolean): Boolean {
        if (!prefs.blockPopups) return false
        val host = tab.topHost
        if (!Shields.effective(prefs, host).shieldsUp) return false
        // (the engine's own "user gesture" flag is false for a window.open inside a tap on some pages, so the app's own record of the last finger touch decides)
        if (SystemClock.uptimeMillis() - lastTouchAt > 2500) { onPopupBlocked(tab.url, "open", uri); return true }
        val fs = Engine.peek()
        if (fs != null && Verdict.isBlocked(fs.check(uri, host, RequestType.POPUP))) { onPopupBlocked(tab.url, "open", uri); return true }
        return false
    }

    internal fun errorChanged(tab: Tab) {
        if (tab === restoringTab && tab.error != null) hideRestoring("error") // the error screen replaces the preview
        if (tab === current) { attachContent(tab); updateUi() }
    }

    internal fun pageFinished(tab: Tab, real: Boolean = true) {
        if (tab === scrollTestTab) { scrollTestTab = null; Io.main.postDelayed({ startScrollTest(tab) }, 2500); return } // the bundled test page: nothing else to do for it
        flushCounters()
        if (real && tab === restoringTab) hideRestoring("page-stop") // the load ended (a page that never reported its first paint must not keep the preview up)
        // A failed or blocked load is not a visit (typos and malware URLs would otherwise fill history and the top sites).
        if (!tab.isPrivateTab && tab.error == null && tab.url.startsWith("http")) { val u = withoutUserinfo(tab.url); val ti = tab.title; Io.exec { db?.recordVisit(u, ti) } }
        scheduleSave()
        if (tab === current) updateUi()
        // a while after the load: the page has settled, nobody is typing -> take the preview the tab grid will show. Each capture is a GPU read-back + scale + a disk write, so not
        // for quick successive navigations (one per 15 s at most) and later than the first paint (research/HW_SPEED.md H3).
        Io.main.postDelayed({
            val now = SystemClock.uptimeMillis()
            if (tab === current && !tab.loading && !typing() && tabGrid.visibility != View.VISIBLE && now - tab.thumbAt > 15_000 && now - lastTouchAt > 3_000) captureThumb(tab)
        }, 4000)
    }

    /** "trim=15@8s": the newest memory-trim level and how long ago (engine compositor events are logged next to it). */
    internal fun sinceTrim(): String = "trim=$lastTrim@" + (if (lastTrimAt == 0L) "-" else "${(SystemClock.elapsedRealtime() - lastTrimAt) / 1000}s")
    internal fun isCurrentTab(tab: Tab): Boolean = tab === current

    /** Free memory as Android sees it, for the Health log: "avail=412MB thr=150MB low=false trim=15@8s". */
    private fun memFacts(): String {
        return try {
            val mi = ActivityManager.MemoryInfo()
            getSystemService(ActivityManager::class.java)?.getMemoryInfo(mi)
            val ago = if (lastTrimAt == 0L) "-" else "${(SystemClock.elapsedRealtime() - lastTrimAt) / 1000}s"
            "avail=${mi.availMem shr 20}MB thr=${mi.threshold shr 20}MB low=${mi.lowMemory} trim=$lastTrim@$ago"
        } catch (e: Exception) { "mem=?" }
    }

    /**
     * The engine's content process died (Android reclaimed the memory = onKill, or it crashed = onCrash). With one content process every live tab dies together.
     * Recovery: the FIRST death in a quiet period brings the page back where it was (the last session state keeps its history AND scroll position) behind a dimmed
     * preview instead of a white screen; a second death within 15 s reloads the plain URL (the state itself may be the cause); a third shows the error screen.
     */
    internal fun sessionGone(tab: Tab, crashed: Boolean) {
        val url = tab.url
        val saved = tab.gecko.state
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* already unusable */ } }
        tab.session = null
        val now = SystemClock.elapsedRealtime()
        tab.crashCount = if (now - tab.lastCrash < 15_000) tab.crashCount + 1 else 1
        tab.lastCrash = now
        val withState = saved != null && tab.crashCount <= 1
        tab.gecko.state = if (withState) saved else null
        tab.pendingLoad = if (withState) null else url.takeIf { it.startsWith("http") }
        Health.log("gecko", if (crashed) "recover-after-crash" else "recover-after-kill", "n=${tab.crashCount} state=$withState current=${tab === current} ${memFacts()} ${url.take(60)}")
        Health.count(if (withState) "gecko.recover.state" else "gecko.recover.url")
        if (tab === current) {
            if (tab.crashCount >= 3) { tab.error = ErrorScreen.Crashed(url); tab.pendingLoad = null; hideRestoring("crash-loop") }
            else if (!ProcessLife.uiVisible) {
                // The app is in the background: Android took the memory back, and starting a new engine process now (150 MB or more) would only take it from the app the user is in. onResume
                // brings the page back (it already revives a current tab that has no session), behind the same preview.
                Health.count("gecko.reviveDeferred"); Health.log("tabs", "revive-deferred", "tab=${tab.id}")
                return
            }
            else if (url.startsWith("http")) showRestoring(tab, crashed)
            attachContent(tab)
            updateUi()
        }
    }

    private fun showRestoring(tab: Tab, crashed: Boolean, message: String? = null, picture: android.graphics.Bitmap? = null, dim: Int = 0xB3) {
        Health.log("tabs", "cover-show", "tab=${tab.id} thumb=${tab.thumb != null} picture=${picture != null}")
        restoringTab = tab
        val thumb = picture ?: tab.thumb
        restoreOverlay.show(thumb, message ?: if (crashed) "The page stopped - bringing it back" else "Page freed to save memory - bringing it back", dim)
        Io.main.removeCallbacks(restoreTimeout)
        Io.main.postDelayed(restoreTimeout, 20_000) // never stay up for ever (a page that never paints, a dead connection)
    }

    private fun hideRestoring(why: String = "") {
        if (restoringTab != null) Health.log("tabs", "cover-hide", "tab=${restoringTab?.id} why=$why")
        Io.main.removeCallbacks(restoreTimeout)
        restoringTab = null
        restoreOverlay.hide()
    }

    /** The engine painted real content for [tab] (called from ContentDelegate.onFirstContentfulPaint). */
    internal fun firstPaint(tab: Tab) { if (tab === restoringTab) hideRestoring("first-paint") }

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
        if (sheet.isShowing) { sheet.dismiss(); return true }
        if (downloadsView?.parent != null) { closeDownloads(); return true }
        if (diagView?.parent != null) { closeDiagnostics(); return true }
        if (settingsView?.parent != null) { closeSettings(); return true }
        if (listPanel?.parent != null) { closeList(); return true }
        if (tabGrid.visibility == View.VISIBLE) { hideTabGrid(); return true }
        if (findBar.visibility == View.VISIBLE) { closeFind(); return true }
        if (suggestions.visibility == View.VISIBLE || home.suggest.visibility == View.VISIBLE || typing()) { hideSuggestions(); clearUrlFocus(); updateUi(); return true }
        val t = current ?: return false
        if (t.showHome && !t.isNewTab) { t.showHome = false; attachContent(t); updateUi(); return true } // Home laid over a page: Back returns to the page
        if (t.error != null) {
            t.error = null
            if (t.canGoBack && t.session != null) { attachContent(t); goBack(t) } else if (t.parentId != 0L && tabs.any { it.id == t.parentId }) doCloseTab(t) else showNewTabPage(t)
            return true
        }
        if (t.canGoBack && t.session != null) { goBack(t); return true }
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
            if (t.canGoBack && t.session != null) { attachContent(t); goBack(t) } else showNewTabPage(t)
        }
        override fun proceedUnsafeSsl(host: String) { current?.let { it.sslBypassHosts.add(host); this@BrowserActivity.retry(it) } }
        override fun continueHttp(httpUrl: String, host: String) { current?.let { it.httpOkHosts.add(host); it.error = null; loadUrl(it, httpUrl, null) } }
        override fun proceedDangerous(url: String, host: String) { current?.let { it.dangerOkHosts.add(host); it.error = null; loadUrl(it, url, null) } }
        override fun openWifiSettings() { try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (e: Exception) { toast("Open Wi-Fi settings from the tablet's Settings app") } }
    }

    // ================================================================== fullscreen video (the engine draws it; we only get out of its way)

    internal fun fullScreenChanged(on: Boolean) {
        fullscreenOn = on
        dock.visibility = if (on) View.GONE else View.VISIBLE
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

    internal fun download(tab: Tab, url: String, userAgent: String?, contentDisposition: String?, mime: String?, length: Long, nameOverride: String? = null) {
        val name = nameOverride ?: Downloads.fileName(url, contentDisposition, mime)
        val limit = prefs.dataSaver.confirmDownloadBytes(NetworkMonitor.metered)
        val big = limit >= 0 && (length < 0 || length >= limit)
        val risky = Downloads.isRisky(name, mime)
        val cleartext = url.startsWith("http://", ignoreCase = true) // on a hotspot the carrier can read or change an unencrypted download
        if (!big && !risky && !cleartext) { startDownload(url, userAgent, contentDisposition, mime, tab, nameOverride); leaveDownloadTab(tab); return }
        val size = if (length >= 0) NewTabPage.fmtBytes(length) else "unknown size"
        val msg = buildString {
            append(name).append("\nFrom: ").append(UrlParts.hostOf(url) ?: "unknown site").append("\nSize: ").append(size)
            if (cleartext) append("\n\nThis download is not encrypted: your mobile carrier or anyone on the network could read or change the file.")
            if (big) append("\n\nYou're on a metered connection (your phone's hotspot). This may use a lot of mobile data.")
            if (risky) append("\n\nThis type of file can install software or run code. Only continue if you trust the site.")
        }
        askYesNo("Download this file?", msg, "Download", "Cancel", { ok -> if (ok) startDownload(url, userAgent, contentDisposition, mime, tab, nameOverride); leaveDownloadTab(tab) })
    }

    /** A tab whose only "page" was the download link has nothing to show: back to the page it came from, to Home, or closed when a page opened it. */
    private fun leaveDownloadTab(tab: Tab) {
        if (tab.pendingNav == null || !tabs.contains(tab)) return // a link on a real page: that page is still there
        tab.pendingNav = null
        tab.loading = false; tab.progress = 0
        when {
            tab.prevUrl.isNotEmpty() -> { tab.url = tab.prevUrl; updateUi() }
            tab.parentId != 0L && tabs.any { it.id == tab.parentId } -> doCloseTab(tab)
            else -> showNewTabPage(tab)
        }
    }

    private fun startDownload(url: String, ua: String?, cd: String?, mime: String?, tab: Tab, nameOverride: String? = null) {
        val go = {
            // Referer = the page's ORIGIN only (never the full URL: it can carry tokens / search terms)
            val headers = HashMap<String, String>()
            UrlParts.parse(tab.url)?.let { headers["Referer"] = "${it.scheme}://${it.hostForAuthority}/" }
            DownloadController.enqueue(this, Source(url, tab.url.takeIf { it.isNotEmpty() }, nameOverride ?: Downloads.fileName(url, cd, mime), mime, headers, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, nameFixed = nameOverride != null))
            askNotificationPermissionOnce()
            toast("Download started: Menu > Downloads")
        }
        if (tab.isPrivateTab) askYesNo("Download from a private tab?", "The file is saved in your Downloads folder and stays there after you close the private tab. Kestrel keeps no record of it.", "Download", "Cancel", { ok -> if (ok) go() })
        else go()
    }

    private var askedNotifications = false
    /** Android 13+: the progress notification needs the user's OK once. The downloads run either way (the service is visible in the task manager). */
    internal fun askNotificationPermissionOnce() {
        if (askedNotifications || Build.VERSION.SDK_INT < 33) return
        askedNotifications = true
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 77)
    }

    // ---- the Downloads screen

    private var downloadsView: DownloadsScreen? = null

    private fun showDownloads() {
        if (downloadsView?.parent != null) return
        hideKeyboard()
        val v = DownloadsScreen(this, pal, object : DownloadsScreen.Listener {
            override fun open(v: DownloadView) { openDownload(v) }
            override fun share(v: DownloadView) { shareDownload(v) }
            override fun confirm(title: String, message: String, yes: String, then: () -> Unit) { askYesNo(title, message, yes, "Keep", { ok -> if (ok) then() }) }
            override fun close() { closeDownloads() }
        })
        downloadsView = v
        root.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun closeDownloads() { downloadsView?.let { root.removeView(it) }; downloadsView = null }

    private fun openDownload(v: DownloadView) {
        val uri = v.destUri ?: run { toast("The file is not available"); return }
        val go = {
            try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), v.mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
            catch (e: Exception) { toast("No app on this tablet can open this file") }
        }
        if (Downloads.isRisky(v.name, v.mime)) askYesNo("Open this file?", "${v.name} can install software or run code. Only open it if you trust where it came from.", "Open", "Cancel", { ok -> if (ok) go() })
        else go()
    }

    private fun shareDownload(v: DownloadView) {
        val uri = v.destUri ?: run { toast("The file is not available"); return }
        val i = Intent(Intent.ACTION_SEND).setType(v.mime ?: "*/*").putExtra(Intent.EXTRA_STREAM, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(Intent.createChooser(i, "Share file")) } catch (e: Exception) { toast("Nothing to share with") }
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
            el.type == ContentDelegate.ContextElement.TYPE_VIDEO || el.type == ContentDelegate.ContextElement.TYPE_AUDIO ->
                video.longPress(tab, src, link) { url -> val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigateTo(t, url) }
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
        if (video.available(tab)) { labels.add("Download video from this page"); acts.add { video.show(tab) } }
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
        if (video.available(tab)) { labels.add("Download video from this page"); acts.add { video.show(tab) } }
        AlertDialog.Builder(this).setTitle(url.take(80)).setItems(labels.toTypedArray()) { _, i -> acts[i]() }.show()
    }

    internal fun copyText(text: String) = copy(text)

    private fun copy(text: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("link", text))
        toast("Copied")
    }

    companion object {
        private const val REQ_FILE = 1001
        private const val REQ_PERM = 1002
        private const val PULL_ARM_DP = 64f // chip travel at which letting go reloads (about 107 dp of finger travel)
        private const val PULL_MAX_DP = 84f
    }
}
