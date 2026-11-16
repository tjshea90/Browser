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
import app.kestrel.browser.ui.DownloadPopup
import app.kestrel.browser.diag.Health
import app.kestrel.browser.diag.StartTimeline
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.DataMeter
import app.kestrel.browser.data.Entry
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.net.ApkInspect
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
import app.kestrel.browser.ui.LastLook
import app.kestrel.browser.ui.ThumbStore
import app.kestrel.core.session.BackLab
import app.kestrel.core.session.BackScrollRules
import app.kestrel.core.session.LastLookMeta
import app.kestrel.core.session.LastLookRules
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
import app.kestrel.core.media.MediaNames
import app.kestrel.core.url.UrlParts
import app.kestrel.browser.web.ShieldsBridge
import app.kestrel.browser.web.TabDelegates
import app.kestrel.browser.web.gecko
import app.kestrel.browser.web.isPrivateTab
import app.kestrel.browser.web.session
import app.kestrel.core.filter.RequestType
import app.kestrel.core.filter.Verdict
import app.kestrel.core.url.HttpsPolicy
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.OrientationController
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.SlowScriptResponse
import org.mozilla.geckoview.StorageController
import java.io.File

private typealias PopupVerdict = app.kestrel.core.guard.PopupGuard.Verdict

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
    private lateinit var blockedChip: app.kestrel.browser.ui.BlockedChip // AR1: a page was blocked: one very small icon at the bottom middle for one second; a press opens the page in a new tab
    private lateinit var miniNav: app.kestrel.browser.ui.MiniNav          // AR2: the one-hand navigation panel (Back, Home, Forward, an address bar with its own keyboard) at the top right

    private val miniNavListener = object : app.kestrel.browser.ui.MiniNav.Listener {
        override fun navBack() { val t = current; if (t != null && (t.canGoBack || !t.isNewTab || t.showHome || t.parentId != 0L)) handleBack() } // never sends the whole app to the background from this small panel
        override fun navHome() { if (!homeShown()) onHome() }
        override fun navForward() { current?.let { goForward(it) } }
        override fun navSubmit(text: String) { current?.let { navigate(it, text) } }
        override fun navOpenChanged(open: Boolean) { prefs.miniNavOpen = open }
        // glide typing on the mini keyboard (AT1): the switch, the thumb habit it learned, the words typed that the word list lacked, and the names of the sites visited
        override fun navGlideEnabled() = prefs.glideTyping
        override fun navGlideHabit(): FloatArray? = prefs.glideHabit
        override fun navGlideHabitChanged(habit: FloatArray) { prefs.glideHabit = habit }
        override fun navGlideWords(): String = prefs.glideWords
        override fun navGlideWordsChanged(text: String) { prefs.glideWords = text }
        // what the keyboard learned from the person's choices and typing (AU2): one small text file in the app's own storage, never sent anywhere
        private val learnedFile get() = java.io.File(applicationContext.filesDir, "glide_personal.txt")
        override fun navGlideLearned(): String? = try { learnedFile.takeIf { it.exists() }?.readText(Charsets.UTF_8) } catch (e: Exception) { null }
        override fun navGlideLearnedSave(text: String) {
            try {
                val f = learnedFile; val tmp = java.io.File(f.parentFile, f.name + ".tmp")
                if (text.isEmpty()) { f.delete(); return }
                tmp.writeText(text, Charsets.UTF_8); if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            } catch (e: Exception) { /* the next change tries again */ }
        }
        // typing into the page's own text field (AU3)
        override fun navPageField(): app.kestrel.browser.ui.PageField? {
            val t = current ?: return null
            val ses = t.session ?: return null
            if (geckoView.session !== ses || homeShown(t) || t.error != null || !browserInFront() || currentFocus is android.widget.EditText) return null // a field of the browser's own (address bar, find bar) has the keyboard
            return t.gecko.fieldInput?.probe()
        }
        override fun navKeyboardChanged(showing: Boolean) { if (showing) hideKeyboard() } // the system keyboard goes down while the mini one is up
        override fun navPrivateTab(): Boolean = current?.isPrivateTab == true
        // where the person put the panel and how big the keyboard is (AV1)
        override fun navKeyHaptics(): Boolean = prefs.keyHaptics
        override fun navGeometry(): IntArray? = prefs.miniGeom
        override fun navGeometryChanged(g: IntArray?) { prefs.miniGeom = g }
        override fun navGlideTitles(done: (List<String>) -> Unit) {
            val d = db ?: return
            Io.exec {
                val titles = try { (d.history(300) + d.bookmarks()).map { it.title }.filter { it.isNotBlank() } } catch (ex: Exception) { emptyList() }
                runOnUiThread { done(titles) }
            }
        }
        override fun navGlideHosts(done: (List<String>) -> Unit) {
            val d = db ?: return
            Io.exec {
                val hosts = try { (d.history(300) + d.bookmarks()).mapNotNull { e -> try { java.net.URI(e.url).host } catch (ex: Exception) { null } }.distinct() } catch (ex: Exception) { emptyList() }
                runOnUiThread { done(hosts) }
            }
        }
    }
    /** AU1: the browser itself is in front (no screen, sheet, tab grid or full-screen video over the page): the only time the invisible corner acts. */
    private fun browserInFront(): Boolean =
        !fullscreenOn && tabGrid.visibility != View.VISIBLE && !sheet.isShowing && listOf<View?>(downloadsView, diagView, settingsView, listPanel).none { it != null && it.parent === root }

    /**
     * A web page whose scripts can answer: its own long press tells open space from a link, a picture or text (AY2, content.js), so the touch detector below stays out of the way. Home, an error screen, a
     * page the extension does not run in, and a page stuck in a slow script (it cannot answer anything) use the detector.
     */
    private fun pageAnswersLongPress(): Boolean {
        val t = current ?: return false
        if (homeShown(t) || t.error != null || t.session == null) return false
        if (!t.url.startsWith("http://", ignoreCase = true) && !t.url.startsWith("https://", ignoreCase = true)) return false
        if (t.url.substringBefore('#').substringBefore('?').endsWith(".pdf", ignoreCase = true)) return false // the engine's own PDF viewer is not a page the extension runs in
        val slow = t.slowScriptAt
        if (slow != 0L && SystemClock.uptimeMillis() - slow < SLOW_PAGE_MS) return false
        return true
    }

    /**
     * The window point is on something of the app's own that acts on a press or a long press: the open mini panel, Home's search box, its Go button and its suggestions, a button of the error screen.
     * Not open space (AY2): the touch detector leaves it alone.
     */
    private fun ownActionAt(x: Float, y: Float): Boolean {
        if (::miniNav.isInitialized && miniNav.coversWindowPoint(x, y)) return true
        val t = current ?: return false
        if (homeShown(t)) return ownViewAt(home, x, y)
        if (t.error != null) return ownViewAt(errorView, x, y)
        return false
    }

    /** A clickable, long-clickable or editable view below [root] (not [root] itself: Home and the error screen swallow touches as a whole) is under the window point. */
    private fun ownViewAt(root: android.view.ViewGroup, x: Float, y: Float): Boolean {
        val loc = IntArray(2)
        for (i in 0 until root.childCount) {
            val c = root.getChildAt(i)
            if (c.visibility != View.VISIBLE) continue
            c.getLocationInWindow(loc)
            if (x < loc[0] || x >= loc[0] + c.width || y < loc[1] || y >= loc[1] + c.height) continue
            if (c is android.widget.EditText || c.isClickable || c.isLongClickable) return true
            if (c is android.view.ViewGroup && ownViewAt(c, x, y)) return true
        }
        return false
    }

    private val hotCornerHost = object : app.kestrel.browser.ui.HotCorner.Host {
        override fun zone(): IntArray? {
            if (!::miniNav.isInitialized || !miniNav.available || !browserInFront() || pageAnswersLongPress()) return null
            val loc = IntArray(2); content.getLocationInWindow(loc)
            return intArrayOf(loc[0], loc[1], loc[0] + content.width, loc[1] + content.height) // the whole page area: a long press anywhere puts the panel under the finger
        }
        override fun covers(x: Float, y: Float): Boolean = ownActionAt(x, y)
        override fun fired(downTime: Long, x: Float, y: Float) {
            // the page was given the touch-down: tell it the touch is over, so its own long press (context menu, text selection) never starts
            val cancel = android.view.MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), android.view.MotionEvent.ACTION_CANCEL, x, y, 0)
            window.superDispatchTouchEvent(cancel); cancel.recycle()
            root.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            miniNav.showAt(x, y)
        }
    }

    // ---- a long press on OPEN space of a web page puts the panel under the finger (AY2, owner message 39). The page's own script tells the app that the press was on open space (content.js, message "os");
    // the app remembers where the finger went down.
    private var pressActive = false
    private var pressX = 0f; private var pressY = 0f; private var pressDownTime = 0L; private var pressEndAt = 0L
    private var pressMovedFar = false
    private var pressSwallow = false
    private var pressOnPanel = false // the touch went down on the mini panel: the page never saw it, so no answer from the page can be about it

    /** True while the rest of a touch that showed the panel (AY2) is being kept from the page; the touch's end ends it, and so does the start of the next touch. */
    private fun pressSwallowing(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) pressSwallow = false
        if (!pressSwallow) return false
        if (ev.actionMasked == android.view.MotionEvent.ACTION_UP || ev.actionMasked == android.view.MotionEvent.ACTION_CANCEL) { pressSwallow = false; pressActive = false; pressEndAt = SystemClock.uptimeMillis() }
        return true
    }

    internal fun onOpenSpace(page: String) {
        if (!::miniNav.isInitialized || !miniNav.available || !prefs.miniNavToggle || !browserInFront()) return
        val t = current ?: return
        val h = UrlParts.hostOf(page)
        if (h == null || t.topHost != h) return // the press was on another tab's page
        val now = SystemClock.uptimeMillis()
        if (pressMovedFar || (!pressActive && now - pressEndAt > OPEN_SPACE_FRESH_MS)) { Health.log("panel", "open-space-dropped", "moved=$pressMovedFar active=$pressActive late=${now - pressEndAt}"); return } // not the press that was just made
        if (pressActive) { // the finger is still down: the page's touch is over (nothing is clicked or selected when it lifts), and the rest of it is ours
            val cancel = android.view.MotionEvent.obtain(pressDownTime, now, android.view.MotionEvent.ACTION_CANCEL, pressX, pressY, 0)
            window.superDispatchTouchEvent(cancel); cancel.recycle()
            pressSwallow = true
        }
        root.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        Health.count("panel.openSpace")
        if (!(tabForPage(page)?.isPrivateTab ?: current?.isPrivateTab ?: false)) Health.log("panel", "open-space") // the lab tells a panel from the video menu by this line
        Health.log("panel", "open-space", "x=${pressX.toInt()} y=${pressY.toInt()} finger=$pressActive")
        miniNav.showAt(pressX, pressY)
    }
    private val hotCorner by lazy { app.kestrel.browser.ui.HotCorner(Io.main, touchSlop.toFloat(), hotCornerHost) }

    private lateinit var restoreOverlay: RestoreOverlay // over the page view while a page whose engine process died comes back (last preview instead of white)
    private var restoringTab: Tab? = null
    // ---- the last look (EY-1, research/REVIEW-EXTERNAL-2.md section 3.3): the page as it was left, sharp, in the first frame of a start while the live page loads under it. It rides on the restoring cover
    // ([restoringTab] is set too, so every way the cover goes away also takes the look down).
    private var lookTab: Tab? = null                // the tab whose saved picture covers the page area
    private var lookMeta: LastLookMeta? = null
    private var lookPaintAt = 0L                    // uptime ms of the live page's first paint (0 = not yet)
    private var lookStopped = false                 // the live page's load ended
    @Volatile private var lookCapturing = false
    private var lastLookAt = 0L                     // uptime ms of the last capture
    private var lookScrollAt = 0L
    private var lookScrollArmed = false
    private val lookCheckTick = Runnable { lookCheck("tick") }
    private val lookNoteShow = Runnable { if (lookTab != null) restoreOverlay.setLookNote("Saved view - still loading") }
    private val lookScrollTimer: Runnable = object : Runnable {
        override fun run() {
            val quiet = SystemClock.uptimeMillis() - lookScrollAt
            if (quiet < LastLookRules.SETTLE_MS) { Io.main.postDelayed(this, LastLookRules.SETTLE_MS - quiet); return }
            lookScrollArmed = false
            captureLook(current, "settled")
        }
    }
    private val restoreTimeout = Runnable { hideRestoring("timeout") }
    private var lastTrim = 0           // the newest onTrimMemory level and when it came: logged with every process death (the evidence for "why did the page reload")
    private var lastTrimAt = 0L
    private lateinit var findBar: FindBar
    private lateinit var suggestions: SuggestionList
    /** The "Download started" banner and the small download window in the top right (owner message 26, AL2). */
    private lateinit var downloadPopup: DownloadPopup
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
    /** FS1 (owner message 43): the screen follows the tablet (and the rotate button) while a page is full screen; the picture size comes from what the page told about its playing video. */
    private val fsTurner = app.kestrel.browser.web.FullscreenTurner(this, { prefs.fullscreenTurn }, { current?.gecko?.let { Pair(it.pictureW, it.pictureH) } ?: Pair(0, 0) })
    internal fun fullscreenPictureKnown() { fsTurner.pictureKnown() }
    /** ROT1 (owner message 46): while the screen turns under a full-screen page or a playing video the video is paused, the page area is covered with black, and both are let go once the area is still. */
    private var turnCover: View? = null
    private var onScreenNow = false
    private var turnHint: View? = null
    private lateinit var playerView: app.kestrel.browser.web.PlayerControlsView   // VP1: Kestrel's own full-screen controls (view and controller)
    private lateinit var player: app.kestrel.browser.web.PlayerController
    private var labTurnWaitOff = false // debug builds only (labIntent "turnwait" off): the lab plays the old rule beside the new one
    private val turnSettler = app.kestrel.browser.web.TurnSettler({ current }, { fullscreenOn }, { turnCover }, { onScreenNow },
        { !labTurnWaitOff && fullscreenOn && fullscreenTab?.gecko?.vpOk == true }, { resyncEngineSize() }, { turnHint }, { turnHolding || turnResizeAwaited },
        { shown -> if (::player.isInitialized) { if (shown) player.turnStarted() else player.turnEnded() } }) // ROT2: a page that reports its viewport says when it has a turn's new size

    // ---- ANS52a (owner message 52: after a turn "the Android symbols at the top such as the battery and wifi symbols were stuck in the middle of the screen and the video was cropped to about the same point for several moments"):
    // when the window turns, the system keeps showing the OLD frames (the portrait buffers, 600 px wide, in the 1024 px wide landscape display: the status bar icons stay where the old right edge was, the video is cut at the old
    // width) until the app's window has drawn its first frame at the new size. GeckoView's own size change (SurfaceView surfaceChanged -> a synchronous compositor resize) blocks the UI thread for 0.9 to 2 s on the tablet
    // (Health: 'ui slow 887 ms', 'ui late-beat 2021 ms' right after 'gecko size 1024x523'), and it ran BEFORE that first frame. So for a turn that gets the cover (a full-screen page or a playing video) the engine's view is held
    // at its old size for the first traversal (the cover and the bars are drawn at the new size at once), and gets its new size right after that frame: the blocking part happens behind the black cover, not in front of the screen.
    private var turnHolding = false           // the engine's view is held at its old size
    private var turnResizeAwaited = false     // ... and its new size has not been laid out yet
    private var turnHoldAt = 0L
    private var turnRootW = 0; private var turnRootH = 0
    private var turnFrameWatch: android.view.ViewTreeObserver.OnDrawListener? = null
    private val turnHoldTimeout = Runnable { releaseGeckoHold("no frame came") }
    private val turnAwaitTimeout = Runnable { turnResizeAwaited = false }

    private fun holdGeckoView() {
        if (turnHolding || !::geckoView.isInitialized || !::root.isInitialized) return
        val lp = geckoView.layoutParams as? FrameLayout.LayoutParams ?: return
        val w = geckoView.width; val h = geckoView.height
        if (w <= 0 || h <= 0 || !geckoView.isAttachedToWindow) return
        turnHolding = true; turnResizeAwaited = true; turnHoldAt = SystemClock.uptimeMillis()
        lp.width = w; lp.height = h; geckoView.layoutParams = lp
        turnRootW = root.width; turnRootH = root.height
        val vto = root.viewTreeObserver
        if (vto.isAlive) {
            val l = object : android.view.ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    if (turnFrameWatch !== this) return
                    if (root.width == turnRootW && root.height == turnRootH) return        // a draw at the old size (an animation): not the turn's frame yet
                    turnFrameWatch = null
                    Io.main.post {                                                          // (a listener is never removed inside its own callback)
                        try { root.viewTreeObserver.removeOnDrawListener(this) } catch (e: Throwable) { /* gone with the window */ }
                        try { turnSettler.firstFrame() } catch (e: Throwable) { Health.log("fullscreen", "first-frame-failed", e.toString().take(80)) }
                        releaseGeckoHold("the first frame at the new size was drawn")
                    }
                }
            }
            turnFrameWatch = l
            vto.addOnDrawListener(l)
        }
        Io.main.removeCallbacks(turnHoldTimeout); Io.main.postDelayed(turnHoldTimeout, app.kestrel.core.media.TurnSettle.HOLD_MAX_MS)
        Io.main.removeCallbacks(turnAwaitTimeout); Io.main.postDelayed(turnAwaitTimeout, app.kestrel.core.media.TurnSettle.HOLD_MAX_MS + 1500L)
        Health.log("fullscreen", "turn-hold", "the engine's view stays ${w}x$h for the first frame of the turn (the cover and the bars are drawn at the new size at once)")
    }

    private fun releaseGeckoHold(why: String) {
        Io.main.removeCallbacks(turnHoldTimeout)
        turnFrameWatch?.let { try { root.viewTreeObserver.removeOnDrawListener(it) } catch (e: Throwable) { /* gone with the window */ }; turnFrameWatch = null }
        if (!turnHolding) return
        turnHolding = false
        val lp = geckoView.layoutParams as? FrameLayout.LayoutParams
        if (lp != null) { lp.width = FrameLayout.LayoutParams.MATCH_PARENT; lp.height = FrameLayout.LayoutParams.MATCH_PARENT; geckoView.layoutParams = lp }
        val vto = geckoView.viewTreeObserver
        if (vto.isAlive) vto.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() { try { geckoView.viewTreeObserver.removeOnGlobalLayoutListener(this) } catch (e: Throwable) { /* gone */ }; turnResizeAwaited = false }
        }) else turnResizeAwaited = false
        Health.log("fullscreen", "turn-hold-released", "$why, ${SystemClock.uptimeMillis() - turnHoldAt} ms after the turn: the engine's view gets its new size now (behind the cover)")
    }
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
        // RV8: an activity that is recreated while a full screen had forced an orientation must not start with it (the system keeps the request of the old record)
        if (requestedOrientation != android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        turnSettler.init(resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) // ROT1: the first turn counts too
        StartTimeline.activityCreated() // EY-2: the start-up timeline (Diagnostics)
        Health.log("start", "heap", "java heap limit ${Runtime.getRuntime().maxMemory() shr 20} MB, memory class ${getSystemService(ActivityManager::class.java)?.memoryClass ?: -1} MB") // (a lab run died of a 16 MB heap limit: the limit is on record now)
        window.setBackgroundDrawableResource(R.color.window_bg) // the cold-start window showed the shield; from here on it is plain black behind every page and panel
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
        if (savedInstanceState == null) { // SP1 (owner message 41): the app's own symbol glows for about 0.4 s over the first frame, after the launch window that shows it; it takes no touch and nothing waits for it
            root.addView(app.kestrel.browser.ui.LaunchLogo(this, debugLogoStretch()) { v -> root.removeView(v) }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
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
            StartTimeline.mark("frame")
            setRecentsLook() // AO3: the icon and colour of this app's card in the Recents list
            Io.main.post {
                Health.log("engine", "runtime-create")
                GeckoEngine.runtime(this).orientationController.delegate = object : OrientationController.OrientationDelegate { // FS1: screen.orientation.lock() from a full-screen player
                    override fun onOrientationLock(orientation: Int): GeckoResult<AllowOrDeny>? = GeckoResult.fromValue(if (fsTurner.siteLocked(orientation)) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
                    override fun onOrientationUnlock() { fsTurner.siteUnlocked() }
                }
                GeckoEngine.executorOrNull() // Firefox's network stack for the download manager (created here on the UI thread)
                GeckoEngine.warmUp(this)
                ShieldsBridge.install(this, ShieldsBridge({ prefs }, { tabForPage(it) }, { shieldCountChanged(it) }, { video.seen(it) }, { p, u, w, h, d, l, f, played, el -> video.info(p, u, w, h, d, l, f, played, el) }, { p, kind, u -> Io.main.post { onPopupBlocked(p, kind, u) } }, { o, p, w -> Io.main.post { onWarmLink(o, p, w) } }, { url -> pageIsPrivate(url) }, mediaMaybe = { video.maybe(it) }, mediaGone = { p, docs -> video.gone(p, docs) }, pageScrolled = { p, y -> Io.main.post { tabForPage(p)?.let { lookScrolled(it, y, p) } } }, openSpace = { p -> Io.main.post { onOpenSpace(p) } }, videoBar = { m -> onVideoBar(m) }, labBuild = { labBuild }, labProbe = { m -> onLabProbe(m) }, viewport = { m -> Io.main.post { onPageViewport(m) } }, player = { m -> playerMessage(m) }, backPlace = { p -> backPlaceFor(p) }, backShown = { p, kept, nt, ts, y -> Io.main.post { onBackShown(p, kept, nt, ts, y) } }, adSkips = { m -> val priv = pageIsPrivate(m.optString("p")); Health.log("ads", "report-in", "w=${m.optString("w")} ads=${m.optInt("a")} ${if (priv) "dropped (private or unknown page)" else "kept"}"); if (!priv) Io.main.post { onAdSkips(m) } }, seekTaps = { m -> if (!pageIsPrivate(m.optString("p"))) Io.main.post { onSeekTaps(m) } else Health.count("seekbar.dropped") }, noStore = { p -> if (!pageIsPrivate(p)) Io.main.post { backLab.noStore(p) } }, leftPending = { p, n -> if (!pageIsPrivate(p)) Io.main.post { backLab.pending(p, n) } }, videoQuality = { m -> if (!pageIsPrivate(m.optString("p"))) Io.main.post { onVideoQuality(m) } }, scrollGaps = { m -> if (prefs.measureScroll && !pageIsPrivate(m.optString("p"))) Io.main.post { onScrollGaps(m) } }))
                Health.log("engine", "runtime-ready"); StartTimeline.mark("runtime")
            }
        }
        val firstTab = Tab(nextTabId++)
        tabs.add(firstTab)
        show(firstTab)
        val launch = urlFromIntent(intent)
        if (prefs.preconnect && launch != null) GeckoEngine.warmOrigin(launch)?.let { GeckoEngine.warmAtStart(it) } // the link that launched the app: its handshakes overlap with the engine starting (EX-2 B)
        if (launch == null) earlyCover() // closed on a page, now starting without a link: "Opening <host>" from the first frame, not the Home screen for a moment
        if (intent?.getBooleanExtra(Notifier.EXTRA_OPEN_DOWNLOADS, false) == true) Io.main.post { showDownloads() }
        handleUpdateIntent(intent)
        Io.fast {
            val text = try { sessionFile().takeIf { it.isFile }?.readText() } catch (e: Exception) { null }
            val session = SessionCodec.decode(text)
            if (prefs.preconnect && launch == null && session != null) { // the page that was in front loads at once: warm its origin while the engine starts (EX-2 B)
                session.tabs.firstOrNull { it.id == session.selectedId && !it.homeOverPage }?.url?.let { u -> GeckoEngine.warmOrigin(u)?.let { GeckoEngine.warmAtStart(it) } } // not when Home was in front: that page does not load
            }
            // each regular tab's saved engine state (Back stack + scroll position) is read here, off the UI thread; it is parsed only when the tab is opened
            val states = HashMap<Long, String>()
            if (session != null && TabStateStore.useEngine(applicationContext, Flavor.engineVersion)) for (r in session.tabs) TabStateStore.read(applicationContext, r.id)?.let { states[r.id] = it }
            val selThumb = if (launch == null && session != null) ThumbStore.load(applicationContext, session.selectedId) else null // the preview that covers the page area while the saved page loads
            Io.ui { restore(session, savedInstanceState, launch, states, selThumb) }
        }
        Io.exec { db?.purge(5000, 90); UpdateJob.schedule(applicationContext); app.kestrel.browser.update.ReleaseCheckJob.schedule(applicationContext) } // after the session read: nothing at launch waits for this
    }

    /**
     * AO3 (owner message 29): the card of this app in Android's Recents list shows its icon and colour. Some Recents screens (Android 10 Go on the tablet) do not draw an adaptive icon from the manifest and show the
     * platform's generic robot tile instead; an icon handed over as a plain picture is drawn by every Recents screen. Drawn once from the same adaptive icon the launcher uses.
     */
    @Suppress("DEPRECATION")
    private fun setRecentsLook() {
        try {
            val icon = getDrawable(R.mipmap.ic_launcher) ?: return
            val size = 192
            val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, size, size); icon.draw(android.graphics.Canvas(bmp))
            setTaskDescription(android.app.ActivityManager.TaskDescription(applicationInfo.loadLabel(packageManager).toString(), bmp, pal.chromeBg or 0xFF000000.toInt()))
        } catch (e: Throwable) { /* a plain card in Recents is harmless */ }
    }

    override fun onStart() {
        super.onStart()
        Io.main.removeCallbacks(strayCheck)
        ProcessLife.uiVisible = true; DownloadController.uiVisibilityChanged()
        downloadPopup.attach(); downloadPopup.resumed()
        val appCtx = applicationContext
        Io.fast { NetworkMonitor.start(appCtx) } // binder calls and the first preferences read: not on the UI thread (start and stop share this lane, so they stay in order)
        Neon.foreground(true)
    }

    override fun onResume() {
        super.onResume()
        onScreenNow = true
        fsTurner.resumed()
        Health.log("life", "resume"); Health.startWatchdog(); Health.watchFrames(window); app.kestrel.browser.diag.CpuMeter.start(packageName)
        flushStateWanted = false
        warmFrontOrigin()
        app.kestrel.browser.net.WifiBoost.set(this, prefs.labWifiBoost) // Network lab: Wi-Fi kept out of power save while the browser is on screen (EX-7)
        PageProxy.refresh()                                           // pages through the phone: renew the phone's proxy door now (the app was away; the Wi-Fi may have changed)
        val t = current
        if (t != null && !t.isNewTab && t.session == null) attachContent(t) // hibernated while we were in the background: bring it back
        t?.session?.setActive(true)
        refreshNtpIfShown()
        // once a day, when the app comes to the front: one tiny conditional request (an ETag makes "nothing new" free). Never in the background.
        UpdateManager.maybeAutoCheck(this, prefs.autoUpdateCheck, NetworkMonitor.state != NetworkMonitor.State.OFFLINE) // NO_INTERNET is only a guess (a hotspot can fail Android's connectivity probe and still work): try
        Io.main.postDelayed({ maybeShowUpdateNotice() }, 2500) // news the background look found while the app was closed
        Io.main.postDelayed({ maybeRefreshLists() }, 90_000)   // well after start-up: the page the owner opened first comes first
        Io.main.removeCallbacks(tidyRunnable); Io.main.postDelayed(tidyRunnable, 25_000) // AO4: old update files, stale cache, closed tabs' pictures; due about once a day and after an update
    }

    private val tidyRunnable = Runnable { runStorageTidy(false) }

    /** AO4: the storage tidy. Off the UI thread; [force] = the Settings button (always runs, and answers with what it did). */
    private fun runStorageTidy(force: Boolean) {
        val c = applicationContext
        val ids: Set<Long>? = if (sessionRestored) tabs.map { it.id }.toSet() else null // an incomplete tab list would make live tabs look closed
        Io.exec {
            val text = app.kestrel.browser.diag.StorageTidy.runIfDue(c, ids, force)
            if (force) Io.main.post { toast(text?.let { "Storage tidy: $it" } ?: "A tidy is already running") }
        }
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
        onScreenNow = false; turnSettler.abandon(); releaseGeckoHold("the app was left"); turnResizeAwaited = false
        Health.log("life", "pause"); fsTurner.paused()
        captureLook(current, "pause", LastLookRules.LEAVE_MIN_AGE_MS.toLong()) // EY-1: the page as it is left, before anything else (the read-back is a few hundred ms at most)
        app.kestrel.browser.net.WifiBoost.set(this, false)
        pausedAt = SystemClock.uptimeMillis()
        flushCounters()
        saveSessionNow()
        // EX-18 (research/REVIEW-EXTERNAL-1.md 9 row 8): ask the front tab for its newest history + scroll state now (GeckoView 144+). The answer arrives on onSessionStateChange and is written at once,
        // so the file is current before Android can kill the process while the app is away. Never for a private tab (nothing of it is ever written).
        current?.takeIf { !it.isPrivateTab }?.session?.let { try { it.flushSessionState(); flushStateWanted = true } catch (e: Throwable) { /* an older engine: the periodic state is all there is */ } }
    }

    @Volatile private var flushStateWanted = false

    /** The engine's answer to the flush asked for in [onPause]: write the sessions file now (UI thread). */
    internal fun stateFlushArrived(t: Tab) {
        if (!flushStateWanted || t !== current) return
        flushStateWanted = false
        saveSessionNow()
    }

    override fun onStop() {
        super.onStop()
        Health.log("life", "stop"); Health.stopWatchdog(); Health.unwatchFrames(window); app.kestrel.browser.diag.CpuMeter.stop()
        current?.session?.setActive(false) // stops rendering and throttles the page: saves battery and mobile data
        silenceStraysSoon() // SND1
        Io.fast { NetworkMonitor.stop() }
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
        // AU1 / AY2: a long press that showed the panel has told the page its touch is over: the page must not see the rest of it
        val cornerOurs = hotCorner.observe(ev)
        val pressOurs = pressSwallowing(ev)
        if (cornerOurs || pressOurs) {
            if (ev.actionMasked == android.view.MotionEvent.ACTION_UP || ev.actionMasked == android.view.MotionEvent.ACTION_CANCEL) { // the touch ends here: the glows are let go as after any touch (they were held still when the finger went down)
                lastTouchAt = SystemClock.uptimeMillis()
                Io.main.removeCallbacks(touchRelease); Io.main.postDelayed(touchRelease, 350L)
            }
            return true
        }
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                turnSettler.touchedScreen() // ROT1: a finger during a turn is the person taking over: nothing is played for them
                pressOnPanel = ::miniNav.isInitialized && miniNav.coversWindowPoint(ev.x, ev.y)
                pressActive = !pressOnPanel; pressX = ev.x; pressY = ev.y; pressDownTime = ev.downTime; pressMovedFar = false
                if (pressOnPanel) pressEndAt = 0L
                Neon.touching(true)
                lastTouchAt = SystemClock.uptimeMillis()
                downX = ev.x; downY = ev.y; dragged = false
                Io.main.removeCallbacks(touchRelease)
            }
            android.view.MotionEvent.ACTION_POINTER_DOWN -> dragged = true // a pinch is a gesture too
            android.view.MotionEvent.ACTION_MOVE -> {
                if (!dragged && (Math.abs(ev.x - downX) > touchSlop || Math.abs(ev.y - downY) > touchSlop)) dragged = true
                if (!pressMovedFar && (Math.abs(ev.x - pressX) > touchSlop * 3 || Math.abs(ev.y - pressY) > touchSlop * 3)) pressMovedFar = true
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                pressActive = false; if (!pressOnPanel) pressEndAt = SystemClock.uptimeMillis()
                lastTouchAt = SystemClock.uptimeMillis()
                if (fullscreenOn) fsTurner.touched() // FS1: the rotate button shows again for a moment
                // a swipe may leave a fling running (hold the glow still for it); a plain tap leaves nothing behind, so the glow comes back almost at once
                Io.main.removeCallbacks(touchRelease); Io.main.postDelayed(touchRelease, if (dragged) 1500L else 350L)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /** FS1: the screen turned (the tablet, the rotate button, a page's lock): the engine is told so `screen.orientation` and a full-screen video's layout follow. The activity handles the change itself (manifest configChanges). */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        val cfgAt = SystemClock.uptimeMillis()
        super.onConfigurationChanged(newConfig)
        val landscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        try { turnSettler.configChanged(landscape); if (turnSettler.turning) holdGeckoView() } catch (e: Throwable) { Health.log("fullscreen", "turn-settler-failed", e.toString().take(80)); try { turnSettler.abandon() } catch (x: Throwable) { /* nothing more to do */ } } // ROT1: pause the video and cover the page BEFORE the engine re-lays out (a failure must never leave the cover up)
        GeckoEngine.runtimeOrNull()?.let { try { it.orientationChanged() } catch (e: Throwable) { Health.log("fullscreen", "orientation-notify-failed", e.toString().take(80)) } }
        Health.log("life", "config", "orientation=${if (landscape) "landscape" else "portrait"} fullscreen=$fullscreenOn" + (SystemClock.uptimeMillis() - cfgAt).let { if (it >= 50L) ", the app's own handling took $it ms" else "" })   // ANS51f
        if (fullscreenOn) { Io.main.removeCallbacks(rehideAfterTurn); Io.main.postDelayed(rehideAfterTurn, 150L) } // FSB1: a turn can bring the bars back on some systems
    }

    override fun onDestroy() {
        try { fsTurner.exit() } catch (e: Exception) { /* RV8: leave the orientation as Android had it */ }
        if (::player.isInitialized) player.fullscreen(false)
        turnSettler.abandon(); releaseGeckoHold("closing"); Io.main.removeCallbacks(turnAwaitTimeout); Io.main.removeCallbacks(strayCheck); Io.main.removeCallbacks(barsCheck); Io.main.removeCallbacks(rehideAfterTurn); Io.main.removeCallbacks(vpUndo) // alpha49 review: no timer of ours may fire against a dead activity
        if (::downloadPopup.isInitialized) downloadPopup.detach()
        UpdateManager.removeListener(updateListener)
        kbProbe.detach()
        NetworkMonitor.removeListener(netListener)
        Io.main.removeCallbacks(saveRunnable)
        Io.main.removeCallbacks(lookScrollTimer); Io.main.removeCallbacks(lookCheckTick); Io.main.removeCallbacks(lookNoteShow)
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
        Io.exec { try { BrowserDb(appCtx).clearHistory() } catch (e: Exception) { /* ignore */ }; ThumbStore.clearAll(appCtx); LastLook.clearAll(appCtx) }
        Io.main.post { try { GeckoEngine.runtime(this).storageController.clearData(StorageController.ClearFlags.ALL) } catch (e: Exception) { /* engine not ready: next time */ } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Health.log("win", "focus", hasFocus.toString())
        if (hasFocus && ::root.isInitialized) kbProbe.attach(root) // the keyboard probe needs the window token: have it ready before the first keystroke
        if (hasFocus && fullscreenOn && prefs.fullscreenBars == 1) { applyImmersive(true); checkBarsSoon() } // FSB1: a system dialog, the volume panel or Recents may have brought the bars back (only when the person chose Hide): take them away again
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_DOWNLOADS, false)) showDownloads()
        handleUpdateIntent(intent)
        labIntent(intent)
        urlFromIntent(intent)?.let { openInNewTabOrCurrent(it) }
    }

    /**
     * SOAK1 / RX4-E1 (the GitHub emulator lab, tools/emu/soak.py): two things a script cannot do through the screen, and only in a DEBUGGABLE build (a release build ignores the extra):
     * `am start -n <package>/app.kestrel.browser.MainActivity --es kestrel_debug dump` writes the Diagnostics text to files/debug-diag.txt (read with `run-as`); `... closetabs` closes every tab (a fresh one opens).
     */
    private fun labIntent(i: Intent?) {
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return
        val what = i?.getStringExtra("kestrel_debug") ?: return
        i.removeExtra("kestrel_debug")
        when (what) {
            "dump" -> {
                try { GeckoEngine.readBackPrefs { } } catch (e: Throwable) { /* the engine is not up: the line says "not read yet" */ } // what Diagnostics > Engine prefs would show (screen.orientation lock, Safe Browsing ...)
                Io.main.postDelayed({
                    val text = try { Health.report(this, engineFacts(), 400) } catch (e: Throwable) { "dump failed: $e" }
                    Io.exec { try { java.io.File(filesDir, "debug-diag.txt").writeText(text); Health.log("lab", "dump", "${text.length} bytes") } catch (e: Exception) { Health.log("lab", "dump-failed", e.toString().take(80)) } }
                }, 2500L)
            }
            "clearcache" -> Io.main.post { try { GeckoEngine.runtime(this).storageController.clearData(StorageController.ClearFlags.ALL_CACHES); Health.log("lab", "clearcache", "done") } catch (e: Exception) { Health.log("lab", "clearcache-failed", e.toString().take(80)) } } // LAB2: both Shields passes start from a cold cache
            "bars" -> { val arg = i.getStringExtra("kestrel_arg") ?: ""; i.removeExtra("kestrel_arg"); prefs.fullscreenBars = if (arg == "hide") 1 else 0; Health.log("lab", "bars", if (arg == "hide") "hide" else "keep") } // FB1: the lab plays both settings
            "turnwait" -> { val arg = i.getStringExtra("kestrel_arg") ?: ""; i.removeExtra("kestrel_arg"); labTurnWaitOff = arg == "off"; Health.log("lab", "turnwait", if (labTurnWaitOff) "off (the cover lifts when the view is still: alpha51/52 behaviour)" else "on") } // ROT2: the lab plays a turn with and without the wait for the page
            "nolook" -> { prefs.lastLook = false; Health.log("lab", "nolook", "last look off") } // LAB2: the emulator's software GL makes the picture read-back stall the UI thread for seconds (an ANR on Yahoo in run 43747605584)
            "measure" -> { prefs.measureScroll = true; Health.log("lab", "measure", "on") } // LAB2: the scroll-gap counters on real pages are on for the whole run
            "shields" -> { // LAB2: "<on|off>|host,host,...": Shields down (or back to the default) for these sites, as if the person had turned the site's Shields switch (compatibility A/B)
                val arg = i.getStringExtra("kestrel_arg") ?: ""; i.removeExtra("kestrel_arg")
                val mode = arg.substringBefore('|'); val hosts = arg.substringAfter('|').split(',').map { it.trim() }.filter { it.isNotEmpty() }
                for (h in hosts) Shields.setOverride(h, if (mode == "off") app.kestrel.core.shield.SiteShields(0, 0).with(app.kestrel.core.shield.ShieldFlag.SHIELDS, false) else app.kestrel.core.shield.SiteShields(0, 0))
                Io.main.post { for (t in tabs) { t.shields = Shields.effective(prefs, t.topHost); syncEngineProtection(t) }; Health.log("lab", "shields", "$mode ${hosts.size} sites") }
            }
            "closetabs" -> Io.main.post { for (t in tabs.toList()) doCloseTab(t); Health.log("lab", "closetabs", "tabs=${tabs.size}") }
            "answer" -> { // SOAK1: press a button of the question on screen (debug builds only)
                val yes = i.getStringExtra("kestrel_arg") == "yes"; i.removeExtra("kestrel_arg")
                Io.main.post {
                    val d = labDialog
                    if (d != null && d.isShowing) { Health.log("dialog", "answered", if (yes) "yes" else "no"); d.getButton(if (yes) android.content.DialogInterface.BUTTON_POSITIVE else android.content.DialogInterface.BUTTON_NEGATIVE)?.performClick() }
                    else Health.log("dialog", "none-to-answer")
                }
            }
            "exitfs" -> Io.main.post { Health.log("lab", "exitfs", "fullscreenOn=$fullscreenOn"); try { current?.session?.exitFullScreen() } catch (e: Exception) { Health.log("lab", "exitfs-failed", e.toString().take(80)) } }
            "videodl" -> Io.main.post { current?.let { video.labDownloadBest(it) } } // SOAK1: "find the video" and one tap on the first row of the Save video sheet
            "apkcheck" -> { // RX4-X1: what the Downloads list would do with this file (a file in the app's own folder: a copy of an installer, a disguised copy, a plain video)
                val name = (i.getStringExtra("kestrel_arg") ?: "").substringAfterLast('/'); i.removeExtra("kestrel_arg")
                Io.exec {
                    val f = java.io.File(filesDir, name)
                    val manifest = ApkInspect.hasManifest(f.path)
                    val info = if (manifest) try { ApkInspect.inspectPath(applicationContext, f.path) } catch (e: Throwable) { null } else null
                    val verdict = info?.let { app.kestrel.core.download.ApkIdentity.verdict(it.pkg, packageName, false) }
                    Health.log("download", "apk-verdict", "file=$name nameCanLie=${app.kestrel.core.download.ApkIdentity.extensionCanLie(name)} manifest=$manifest pkg=${info?.pkg} verdict=$verdict permissions=${info?.permissions?.size}")
                }
            }
        }
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
        if (level >= 15) BackPictures.clear() // the system wants memory: the pictures are the first thing to give back
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
        geckoView.pull = pullListener
        geckoView.pullEnabled = {
            val t = current
            prefs.pullRefresh && t != null && !t.isNewTab && !t.showHome && t.error == null && t.session != null && !fullscreenOn && tabGrid.visibility != View.VISIBLE &&
                !sheet.isShowing && restoreOverlay.visibility != View.VISIBLE
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
        // above Home and the error screens (so they show there too), below the find bar, sheets and every full screen
        blockedChip = app.kestrel.browser.ui.BlockedChip(this)
        app.kestrel.browser.diag.DebugProps.get(this, "debug.kestrel.chipms")?.toLongOrNull()?.let { blockedChip.lifetimeMs = it.coerceIn(200L, 60_000L) } // emulator rig only: a release build always gets null
        content.addView(blockedChip, FrameLayout.LayoutParams(dp(app.kestrel.browser.ui.BlockedChip.TOUCH_DP), dp(app.kestrel.browser.ui.BlockedChip.TOUCH_DP), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(6) })
        miniNav = app.kestrel.browser.ui.MiniNav(this, pal, miniNavListener)
        content.addView(miniNav, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)) // the whole page area, not clickable itself: the panel places itself in it (AV1)
        miniNav.available = prefs.miniNavToggle
        miniNav.setGlideEnabled(prefs.glideTyping)
        miniNav.setOpen(prefs.miniNavOpen, animate = false, notify = false)
        val rotateButton = app.kestrel.browser.web.FullscreenTurner.RotateButton(this) // FS1: shows for a moment after a touch while a page is full screen
        content.addView(rotateButton, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply { leftMargin = dp(12) })
        fsTurner.attachButton(rotateButton)
        // VP1 (owner message 49): Kestrel's own controls for a full-screen video: one view over the engine's, drawn only while a film is full screen (the page script reports it)
        playerView = app.kestrel.browser.web.PlayerControlsView(this).apply { visibility = View.GONE }
        content.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        player = app.kestrel.browser.web.PlayerController(playerView,
            { exitFullscreenFromControls() },
            { shown -> fsTurner.controlledShow(shown) })
        fsTurner.ownedByPlayer = { player.active }
        player.onLook = { page, frames, dropped, seconds, w, h -> filmLook(page, frames, dropped, seconds, w, h) }
        val cover = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()); visibility = View.GONE; alpha = 0f; isClickable = true; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO } // ROT1: topmost in the page area; it takes the touch (review of alpha53: the tap that ends a long wait must not reach the page nobody can see)
        // ROT2: a small ring after about a second, so a black screen that waits for the engine does not look frozen
        val ring = android.widget.ProgressBar(this).apply { isIndeterminate = true; visibility = View.GONE; indeterminateTintList = android.content.res.ColorStateList.valueOf(0xFF9A9A9A.toInt()) }
        cover.addView(ring, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER))
        turnCover = cover; turnHint = ring
        content.addView(cover, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        geckoView.sizeListener = { turnSettler.sizeChanged() }
        findBar = FindBar(this, pal, object : FindBar.Listener {
            override fun query(q: String) { findIn(q, false) }
            override fun next(forward: Boolean) { findIn(findBar.input.text.toString(), !forward) }
            override fun close() { closeFind() }
        }).apply { visibility = View.GONE }
        root.addView(findBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(52))) // find stays at the TOP: the keyboard never covers it
        suggestions = SuggestionList(this, pal, reverse = true) { value -> hideSuggestions(); clearUrlFocus(); current?.let { navigate(it, value) } }.apply { visibility = View.GONE }
        root.addView(suggestions, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        // above the page, below the tab grid, sheets and every full screen: a download that starts shows a banner at the top and a small window of all downloads in the top right
        downloadPopup = DownloadPopup(this, pal, object : DownloadPopup.Listener {
            override fun openManager() { showDownloads() }
            override fun confirm(title: String, message: String, yes: String, then: () -> Unit) { askYesNo(title, message, yes, "Keep", { ok -> if (ok) then() }) }
        })
        root.addView(downloadPopup.view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
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

        // Edge-to-edge safe: Android 15+ draws behind the system bars for targetSdk 35+; Android 10 reports the real insets here.
        // FSB1 (owner message 46, with screenshots): the padding ALWAYS follows the real insets, also while a page is full screen. A system that hides its bars reports zero, so the picture is full-bleed; the
        // owner's tablet KEEPS both bars in full screen, reports their size, and the page is laid out above the navigation bar, so a player's controls are never under it. (alpha48 zeroed the padding while a page
        // was full screen and, on Android 10, also asked for the LAYOUT_* flags: right only where the bars really go away; there the controls sat under the visible navigation bar.)
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or (if (urlTyping) 0 else WindowInsets.Type.ime()))
                v.setPadding(i.left, i.top, i.right, i.bottom)
                barsSeen(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsets.CONSUMED
            } else {
                @Suppress("DEPRECATION") v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                @Suppress("DEPRECATION") barsSeen(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                @Suppress("DEPRECATION") insets.consumeSystemWindowInsets()
            }
        }
    }

    // ================================================================== tabs

    /** The saved session is a few KB: read it here (no thread hop) to know which page the app was on. The tab's preview joins the cover when the restore runs a moment later. */
    private fun earlyCover() {
        val sel = try {
            sessionFile().takeIf { it.isFile && it.length() < 262_144 }?.readText()?.let { SessionCodec.decode(it) }?.let { s -> s.tabs.firstOrNull { it.id == s.selectedId } ?: s.tabs.lastOrNull() }
        } catch (e: Exception) { null } ?: return
        if (sel.homeOverPage) { Health.log("tabs", "early-cover-skipped", "left on Home"); return } // AQ1: the Home screen is the page that was in front: no picture of the site, no "Opening ..."
        if (!sel.url.startsWith("http")) return
        Health.log("tabs", "early-cover", UrlParts.hostOf(sel.url) ?: "")
        // EY-1: the page as it was left, sharp, in this very first frame (the read and decode are about 20 ms on this CPU; the start-up timeline's "look" mark says so)
        val look = savedLook(sel.id, sel.url, sel.desktopSite)
        if (look != null) {
            val fallback = "Opening " + (UrlParts.hostOf(sel.url) ?: "page")
            restoreOverlay.showLook(look.bitmap, fallback, onDrawn = { StartTimeline.mark("look"); Health.log("tabs", "look-drawn", "tab=${sel.id} start") })
            lookTab = null; lookMeta = look.meta; lookPending = sel.id; lookPaintAt = 0L; lookStopped = false
            Health.count("look.start"); Health.log("tabs", "look-shown", "tab=${sel.id} ${look.meta.w}x${look.meta.h} y=${look.meta.scrollY}")
            Io.main.removeCallbacks(restoreTimeout); Io.main.postDelayed(restoreTimeout, LastLookRules.CAP_MS.toLong()); Io.main.removeCallbacks(lookNoteShow); Io.main.postDelayed(lookNoteShow, LastLookRules.NOTE_AFTER_MS.toLong())
            return
        }
        restoreOverlay.show(null, "Opening " + (UrlParts.hostOf(sel.url) ?: "page"))
        Io.main.removeCallbacks(restoreTimeout); Io.main.postDelayed(restoreTimeout, 25_000) // never stays for ever, whatever happens to the restore
    }

    /** EY-1: the saved picture for tab [id] when it may be shown for [url] now (same page, engine, appearance, desktop-site choice); null otherwise. Reads and decodes on the calling thread. */
    private fun savedLook(id: Long, url: String, desktop: Boolean): LastLook.Look? {
        if (!prefs.lastLook || !LastLookRules.worthKeeping(url)) return null
        val appCtx = applicationContext
        val meta = LastLook.meta(appCtx, id) ?: return null
        if (!LastLookRules.usable(meta, url, Flavor.engineVersion, LastLook.appearance(appCtx, prefs.webDark), prefs.webDark == 1, desktop)) return null
        return LastLook.load(appCtx, id, meta)
    }

    /** The tab id the early cover's look belongs to until [restore] has made the tab (then it becomes [lookTab]). */
    private var lookPending = 0L

    private fun restore(session: Session?, saved: Bundle?, launch: String?, states: Map<Long, String> = emptyMap(), selThumb: android.graphics.Bitmap? = null) {
        sessionRestored = true // from now on saving cannot clobber the saved session
        if (session != null && session.tabs.isNotEmpty()) {
            // Replace the placeholder tab. Nothing is loaded here except the selected tab (lazy: no network at startup).
            for (t in tabs.toList()) { try { t.session?.close() } catch (e: Exception) { /* ignore */ }; t.session = null }
            tabs.clear()
            current = null // the placeholder tab is gone and nothing "comes back" from it: show() would otherwise clear the Home flag of the tab that was left on Home (AQ1)
            for (r in session.tabs) {
                val t = Tab(r.id)
                t.url = if (r.url == "about:blank") "" else r.url
                t.title = r.title
                t.parentId = r.parentId
                t.desktop = r.desktopSite
                t.lastActive = r.lastActiveMillis
                t.showHome = r.homeOverPage && t.url.isNotEmpty() // AQ1: the app was left on the Home screen: it comes back to Home, the page stays this tab's address and loads when Back is pressed
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
        if (restoringTab == null) { restoreOverlay.hide(); clearLook() } // the early cover is only for a restore that really covers its page: nothing took over, so it must not stay
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

    /**
     * A real touch (a tap, a long press on a video) can only come from the tab in front. When another tab holds the same address (the same page open twice, or a restored tab plus the same link opened
     * again) [tabWithUrl] would name the FIRST one in the list, and the long press on the page the owner is looking at was dropped as "not the current tab": no Save video sheet at all.
     */
    private fun tabOfTouch(pageUrl: String, kind: String): Tab? =
        if (kind == "tap" || kind == "vmenu") current?.takeIf { it.url.substringBefore('#') == pageUrl.substringBefore('#') } else null

    /** True when a private tab shows a page of this address's host (Shields worker thread: a snapshot, never the live list). */
    internal fun pageIsPrivate(url: String): Boolean {
        val h = UrlParts.hostOf(url) ?: return true
        @Suppress("UNNECESSARY_SAFE_CALL", "SENSELESS_COMPARISON")
        return try { tabs.toTypedArray().any { it != null && it.topHost == h && it.isPrivateTab } } catch (e: Exception) { true }
    }

    internal fun tabForPage(url: String): Tab? {
        val h = UrlParts.hostOf(url) ?: return null
        current?.takeIf { it.topHost == h }?.let { return it }
        // Called from the Shields worker thread: never iterate the live list (the UI thread may be opening/closing a tab); a stale answer only costs one uncounted block.
        @Suppress("UNNECESSARY_SAFE_CALL", "SENSELESS_COMPARISON")
        return try { tabs.toTypedArray().firstOrNull { it != null && it.topHost == h } } catch (e: Exception) { null }
    }

    /** A new page began in [tab]: what the old page offered to save is no longer on offer (TabDelegates.onPageStart). */
    internal fun pageStarted(tab: Tab) { video.clearTab(tab); tab.scrollY = 0; tab.gecko.vpOk = false } // (a new page has not told its size yet: the turn wait needs the proof again)

    internal fun newTabAndShow(privateTab: Boolean = false): Tab { val t = newTabInternal(null, privateTab); show(t); focusUrlBar(); return t }

    /** Regular <-> private skin: the whole chrome (dock, Home, progress line, system bars) turns violet in private mode, so it can never be mistaken for a normal tab. */
    private var skinPrivate = false
    private var skinApplied = false
    private fun applySkin() {
        val priv = current?.isPrivateTab == true
        if (skinApplied && priv == skinPrivate) return
        skinApplied = true
        skinPrivate = priv
        GeckoEngine.setFrontPrivate(priv, prefs) // the link look-ups of the Network lab pause while a private tab is in front
        pal = if (priv) palPrivate else palRegular
        dock.applyPal(pal); home.applyPal(pal); miniNav.applyPal(pal); suggestions.setPal(pal); sheet.applyPal(pal); progress.setColors(pal.accent, pal.accentHi); restoreOverlay.setPal(pal); pullChip.setPal(pal)
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
        if (fullscreenOn && fullscreenTab !== tab) endFullScreenOf(null, "another tab came to the front")
        val prev = current
        if (prev != null && prev !== tab) prev.session?.setActive(false) // HM1 (owner message 41): a tab that was left on the Home screen comes back as the Home screen (its page stays alive underneath; Back returns to it); only a new address, Back or the Home screen's own search shows the page again
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
        if (prev != null && prev !== tab) silenceStraysSoon() // SND1: the tab that was left must not go on making sound
    }

    // ---- SND1 (owner message 45): "Sound should not keep playing if I am not currently on the Kestrel tab."
    // The engine suspends the media of a session that is set inactive (suspendMediaWhenInactive; proven in the emulator lab: the clip's clock stood still while the app was left and the media session said
    // paused). This is the safety net for a video it does NOT suspend: 1.5 s after the app left the screen or another tab came to the front, and at once when the Home screen covers a page, every tab that is not
    // showing its page and still reports a playing video is paused through its media session (a silent video has none and makes no sound).
    private val strayCheck = Runnable { silenceStrays() }
    private var strayPaused = 0
    private var strayLast = "nothing needed pausing yet"

    private fun silenceStraysSoon() { Io.main.removeCallbacks(strayCheck); Io.main.postDelayed(strayCheck, 1500L) }

    private fun silenceStrays() {
        val away = !onScreenNow
        for (t in tabs) {
            if (!t.gecko.videoPlaying || t.session == null) continue // (a tab without a live session has nothing that plays, whatever it last said)
            if (!away && t === current && !homeShown(t)) continue // the page in front, on screen, with nothing over it: it plays
            val ms = t.gecko.mediaSession ?: continue
            val why = if (away) "the app is not on screen" else if (t === current) "the Home screen covers the page" else "another tab is in front"
            try { ms.pause(); strayPaused++; Health.count("sound.pausedAway"); strayLast = "tab ${t.id}: $why"; Health.log("sound", "paused-away", "tab=${t.id} $why") }
            catch (e: Throwable) { Health.log("sound", "pause-failed", e.toString().take(80)) }
        }
    }

    private fun strayLine() = "Sound while the page is not in front: the engine suspends it itself; the app paused $strayPaused video(s) the engine left playing; last: $strayLast"

    /** Private tab in front: no screenshots / recents thumbnail, and no autofill service sees what is typed. */
    private var secureWindow = false

    /** DEBUG builds only (never a release): `adb shell setprop debug.kestrel.logostretch 25` makes the start-up logo flourish 25 times slower, so an emulator screenshot can catch it. 1 in every release. */
    private fun debugLogoStretch(): Float {
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return 1f
        return try { ((Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, "debug.kestrel.logostretch") as? String)?.toFloatOrNull() ?: 1f).coerceIn(1f, 60f) } catch (e: Throwable) { 1f }
    }

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
        if (!tab.isNewTab && !(tab.showHome && tab.session == null)) { // AQ1: a tab restored on Home has no engine session yet and gets none until Back: nothing loads behind the Home screen at start
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
            .useTrackingProtection(true)                       // the engine's own tracking protection; syncEngineProtection() switches it off for sites whose Shields are down
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
        s.setMediaSessionDelegate(d)   // RX3-S1: whether the page's video plays
        // AU3: the mini keyboard types into the page's own text field: this wrapper keeps the system keyboard down while the mini one is up and tells the panel when a field gains or loses focus
        val input = PageFieldInput(s, s.textInput.delegate, held = { ::miniNav.isInitialized && miniNav.keyboardShowing }, changed = { Io.main.post { if (::miniNav.isInitialized) miniNav.pageFieldChanged() } })
        s.textInput.setDelegate(input) // (a setter and getter of different nullability: Kotlin has no property for it)
        tab.gecko.fieldInput = input
        return s
    }

    private fun ensureSession(tab: Tab, cover: Boolean = true): GeckoSession {
        tab.session?.let { return it }
        val s = createSession(tab)
        s.open(GeckoEngine.runtime(this))
        tab.session = s
        if (tab === current) StartTimeline.mark("open")
        var state = tab.gecko.state
        if (state == null) tab.gecko.stateJson?.let { state = stateFromDisk(tab, it) } // a tab that was saved by the last run: its Back stack and scroll position come back
        val pending = tab.pendingLoad
        when {
            state != null -> {
                tab.gecko.state = null
                tab.pendingLoad = null
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(tab.url))
                syncEngineProtection(tab)
                s.restoreState(state!!) // brings back the tab's whole history, not just its last page
            }
            pending != null -> {
                tab.pendingLoad = null
                tab.shields = Shields.effective(prefs, UrlParts.hostOf(pending))
                tab.topHost = UrlParts.hostOf(pending)
                syncEngineProtection(tab)
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
    private class BackArm(val tab: Tab, val fromUrl: String, val picture: android.graphics.Bitmap, val savedY: Int)
    private var backTarget: BackArm? = null
    private var backBusyUntil = 0L
    private val backPictureShow = Runnable {
        val b = backTarget ?: return@Runnable
        backTarget = null
        val t = b.tab
        // 150 ms after Back the tab still shows the page it left: the engine has not committed the page Back asked for (its back/forward cache would have, by now), so that page has to be
        // loaded. (Not "is the tab loading": the engine reports the start of a load later than this on a slow CPU.) Its picture covers the page area until it paints.
        val stillThere = BackPictures.keyOf(t.url) == BackPictures.keyOf(b.fromUrl)
        if (t !== current || t.error != null || t.showHome || restoringTab != null || !stillThere) { Health.log("tabs", "back-picture-skipped", "same-page=$stillThere cover=${restoringTab != null}"); return@Runnable }
        Health.count("back.picture"); Health.log("tabs", "back-picture", "tab=${t.id}")
        showRestoring(t, false, "", b.picture, 0x33) // stays until the page is painted AND back at its place (backCheck, 20 s at most): taking it away earlier would show the page the owner just left, or the top of this one
        backCover = BackCover(t, b.savedY)
    }

    internal fun goBack(t: Tab) { armBackPicture(t, -1); t.session?.goBack() }
    internal fun goForward(t: Tab) { armBackPicture(t, +1); t.session?.goForward() }

    private fun armBackPicture(t: Tab, delta: Int) {
        Io.main.removeCallbacks(backPictureShow); backTarget = null
        if (t !== current || t.session == null) return
        val st = t.gecko.state
        if (st == null) { Health.log("tabs", "back-arm", "no state"); return }
        val i = st.currentIndex + delta
        if (i !in 0 until st.size) { Health.log("tabs", "back-arm", "index $i of ${st.size}"); return }
        val url = st[i].uri ?: return
        val pic = BackPictures.get(t.id, url)
        val savedY = t.scrollByUrl.get(url)
        val why = if (t.isPrivateTab) "" else backLab.notKeptWhy(url)
        val notKept = why.isNotEmpty()
        Health.log("tabs", "back-arm", "tab=${t.id} to=${UrlParts.hostOf(url)} picture=${pic != null} pictures=${BackPictures.size()} y=${savedY ?: -1} notKept=${why.ifEmpty { "no" }}")
        // a second Back within two seconds: the engine's history index has not moved yet, so the page this one leads to is not known from here - show nothing rather than the wrong picture
        val now = SystemClock.uptimeMillis()
        val rapid = now < backBusyUntil
        backBusyUntil = now + 2_000
        if (rapid) { if (restoringTab === t) hideRestoring("second-back"); return }
        // RX3-BK1: the lab counts the press (never for a private tab). The place itself is answered from the tab's memory when the page asks (backPlaceFor): it does not depend on this press having found its target
        if (!t.isPrivateTab) { backLab.press(now, url, delta > 0, savedY ?: -1); Io.main.removeCallbacks(backLabClose) }
        if (pic == null) return
        backTarget = BackArm(t, t.url, pic, savedY ?: 0)
        Io.main.postDelayed(backPictureShow, BackScrollRules.pictureDelayMs(notKept)) // a cache hit is done by then; a page that has to load shows its picture (at once when the engine cannot have kept it)
    }

    /** A navigation of the page in front is about to start (called before the engine loads [toUrl]): take its picture now, the way it is on screen, for a later Back. */
    internal fun leavingPage(tab: Tab, toUrl: String) {
        // Not required: that the page has finished loading. What is on screen when the user leaves is what Back should show (a page that is still loading, or keeps loading for ever, is the common case on a slow link).
        if (tab !== current || tab.isNewTab || tab.error != null || tab.showHome || !tab.url.startsWith("http")) { Health.log("tabs", "leave-skip", "loading=${tab.loading}"); return }
        if (BackPictures.keyOf(toUrl) == BackPictures.keyOf(tab.url)) return // a reload or a #fragment: the same page
        if (!tab.isPrivateTab) backLab.left(tab.url, tab.loading, tab.media.hasStream()) // RX3-BK2: whether the engine can have kept the page that is being left (still loading, a stream player, no-store)
        val now = SystemClock.uptimeMillis()
        if (now - lastLeaveShotAt < 1500) return // a chain of redirects or quick taps: one picture is enough
        lastLeaveShotAt = now
        Health.log("tabs", "leave-shot", "tab=${tab.id} from=${UrlParts.hostOf(tab.url)}")
        captureThumb(tab, forBack = true)
    }

    // ---------------------------------------------------------------- RX3-Y2: the YouTube ad skipper's numbers (youtube.js reports differences; never an address; private tabs are dropped before)
    private val adSkipStats = app.kestrel.core.media.AdSkipStats()
    /** RX3-G7: the same numbers from the script for video players other than YouTube (players.js sends w = "p"). */
    private val playerSkipStats = app.kestrel.core.media.AdSkipStats("Other players' ads")
    private fun onAdSkips(m: org.json.JSONObject) {
        val others = m.optString("w") == "p"
        (if (others) playerSkipStats else adSkipStats).add(m.optInt("a"), m.optInt("s"), m.optInt("c"), m.optInt("o"), m.optInt("d"), m.optInt("f"), m.optInt("g"), m.optString("r"))
        val tag = if (others) "playerads" else "ytads"
        Health.count("$tag.reports")
        Health.log(tag, "report", "ads=${m.optInt("a")} skipped=${m.optInt("s")} buttons=${m.optInt("c")} overlays=${m.optInt("o")} dialogs=${m.optInt("d")} jumps=${m.optInt("f")} gaveUp=${m.optInt("g")} ${m.optString("r")}".trim())
    }

    // ---------------------------------------------------------------- RX4-M0a: how real videos play (content.js reads getVideoPlaybackQuality every 15 s and sends the DIFFERENCE as numbers; private tabs are dropped before)
    private val videoQuality = app.kestrel.core.media.VideoQuality()
    /** ANS51e: the frames a film shown by Kestrel's own full-screen player showed and dropped (its page script reads `getVideoPlaybackQuality`): the old video finder never sees a film inside a shadow root, and "Real video" said "nothing played yet" for a film of a minute. */
    private fun filmLook(page: String, frames: Int, dropped: Int, seconds: Int, w: Int, h: Int) {
        val host = UrlParts.hostOf(page) ?: return
        videoQuality.add(host, frames, dropped, 0, seconds, w, h, SystemClock.uptimeMillis())
        Health.log("video", "quality", "$host frames=$frames dropped=$dropped corrupt=0 secs=$seconds ${w}x$h (the full-screen player)")
    }
    private fun onVideoQuality(m: org.json.JSONObject) {
        val host = UrlParts.hostOf(m.optString("p")) ?: return
        videoQuality.add(host, m.optInt("t"), m.optInt("d"), m.optInt("c"), m.optInt("s"), m.optInt("w"), m.optInt("h"), SystemClock.uptimeMillis())
        Health.log("video", "quality", "$host frames=${m.optInt("t")} dropped=${m.optInt("d")} corrupt=${m.optInt("c")} secs=${m.optInt("s")} ${m.optInt("w")}x${m.optInt("h")}")
    }

    // ---------------------------------------------------------------- RX4-M0e: the late screen updates of a scroll burst on a real page (only while the Settings switch is on; private tabs are dropped before)
    private val scrollGaps = app.kestrel.core.media.ScrollGaps()
    private fun onScrollGaps(m: org.json.JSONObject) {
        val host = UrlParts.hostOf(m.optString("p")) ?: return
        scrollGaps.add(host, m.optInt("n"), m.optInt("a"), m.optInt("b"), m.optInt("mx"))
        if (labBuild) Health.log("scroll", "burst", "host=$host n=${m.optInt("n")} a=${m.optInt("a")} b=${m.optInt("b")} mx=${m.optInt("mx")}") // the emulator lab reads these per page
    }

    /** LAB2: a DEBUG build (the emulator lab) asks every page for a page-health probe and mirrors it as one Health line. Never in a release build. */
    private val labBuild: Boolean by lazy { (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    private fun onLabProbe(m: org.json.JSONObject) {
        if (!labBuild || pageIsPrivate(m.optString("p"))) return
        Health.log("probe", "page", "host=${m.optString("h").take(60)} t=${m.optInt("t")} im=${m.optInt("im")} ib=${m.optInt("ib")} iv=${m.optInt("iv")} fr=${m.optInt("fr")} vd=${m.optInt("vd")} vp=${m.optInt("vp")} h=${m.optInt("hh")} w=${m.optInt("ww")} iw=${m.optInt("iw")} sc=${m.optInt("sc")} nd=${m.optInt("nd")} rs=${m.optInt("rs")} rd=${m.optString("rd")}")
    }

    // ---------------------------------------------------------------- YS1: taps on a video player's bar (seekbar.js reports differences; never an address; private tabs are dropped before)
    private val seekStats = app.kestrel.core.media.SeekStats()
    /** VC1 (owner message 44): what Kestrel's own video bar (videobar.js) did on a page. Worker thread; never for a private tab, numbers only. */
    private fun onVideoBar(m: org.json.JSONObject) {
        if (pageIsPrivate(m.optString("p"))) return
        val e = m.optString("e")
        val a = m.optString("a")
        when (e) {
            "shown" -> Health.count("videobar.shown")
            "own" -> Health.count("videobar.pageHasControls")
            "act" -> if (a.isNotEmpty()) Health.count("videobar.act.$a")
            "turn" -> { Health.count("turn.page.turns"); if (a.isNotEmpty()) Health.count("turn.page.$a") } // FB2: the page paused its videos for a turn of the screen and played them again (or left them: the person touched, or the page started them itself)
            else -> return
        }
        Health.log("videobar", e, if (e == "turn") "$a n=${m.optInt("n")}" else a)
    }
    // ---- FB4 (owner message 47; lab run 43760546128 on the real engine): GeckoView sizes a page that goes full screen to the DISPLAY (1024 px) at the moment it enters, while the view between the system bars is 947 px, and
    // keeps that size until the view changes size again (a race between the engine's own full-screen resize and the app's relayout: some entries were right, the first one of a run was wrong): the page's own bottom controls were cut
    // off even with the bars kept, and a turn can leave the old height behind (the owner's third screenshot: a 575 px box in a 947 px view). The page (turn.js) tells the viewport it really got; when it is not the view's, the app gives
    // the engine a REAL size change (the view one pixel shorter for 120 ms), up to three times per full screen, and writes down what it saw.
    private var vpChecks = 0
    private var vpMismatches = 0
    private var vpResyncs = 0
    private var vpTries = 0
    private var vpLastResyncAt = 0L
    private var vpLast = "no full-screen page has told its size yet"
    private var vpIdleMatch = 0   // ROT2: the one report a page makes outside full screen: it was the view's size ...
    private var vpIdleMiss = 0    // ... or it was not (such a page is not waited for during a turn)
    private var vpIdleLast = ""
    private val vpUndo = Runnable { (geckoView.layoutParams as? FrameLayout.LayoutParams)?.let { if (it.bottomMargin != 0) { it.bottomMargin = 0; geckoView.layoutParams = it } } }

    /**
     * The tab a page's viewport report came from. Two tabs can hold the same address (lab run 43766649065: the same lab page opened again, a restored tab plus the same link): the FIRST one in the list is the wrong one
     * then, and the report of the full-screen page was credited to it and lost. So: the full-screen tab, else the tab in front, when their address is the page's; else the first tab with that address; else (review of
     * alpha53, a single-page site such as m.youtube.com or finance.yahoo.com: the address the extension knows is the one the page was LOADED with, the tab's address follows pushState, so they differ) the full-screen
     * or front tab of the same host. The report carries at most 300 characters of the address: both sides are cut the same way.
     */
    private fun tabOfReport(pageUrl: String): Tab? {
        val u = pageUrl.substringBefore('#').take(300)
        if (u.isEmpty()) return null
        fun same(t: Tab?) = t != null && t.url.substringBefore('#').take(300) == u
        (if (fullscreenOn) fullscreenTab?.takeIf { same(it) } else null)?.let { return it }
        current?.takeIf { same(it) }?.let { return it }
        tabs.firstOrNull { same(it) }?.let { return it }
        val host = UrlParts.hostOf(u) ?: return null
        fun sameHost(t: Tab?) = t != null && UrlParts.hostOf(t.url) == host
        return (if (fullscreenOn) fullscreenTab?.takeIf { sameHost(it) } else null) ?: current?.takeIf { sameHost(it) }
    }

    /** VP1: the page's report of a full-screen film or a tap on it (a Shields worker thread): the UI thread takes it, the ANSWER (what the controls want done) is read from the controller's queue right now. */
    private fun playerMessage(m: org.json.JSONObject): String {
        if (prefs.fsControls != 0) return "{\"on\":0}"                        // the person chose the page's own controls (Settings): the page script ends its session and gives the page its bar and its taps back
        Io.main.post { if (fullscreenOn && fullscreenTab != null && tabOfReport(m.optString("p")) === fullscreenTab) { player.onMessage(m); filmPicture(m) } }
        return if (::player.isInitialized) player.reply() else "{\"s\":0}"
    }

    /**
     * ANS51b (owner message 51: the tablet's Diagnostics said "full screen starts (video 0x0 ...)" for a film in a shadow root that the old video finder never sees, so Automatic turning did nothing and the owner turned the
     * tablet by hand twice): the film's picture size from the full-screen player's own report (its video element's size, 2.3 s into the full screen on the tablet) is what FS1 needs. Main thread.
     */
    private fun filmPicture(m: org.json.JSONObject) {
        if (m.optString("k") != "vst") return
        val t = fullscreenTab ?: return
        val w = m.optInt("w"); val h = m.optInt("h")
        if (w <= 0 || h <= 0) return
        val first = t.gecko.pictureW == 0 || !t.gecko.picturePlayed
        t.gecko.pictureW = w; t.gecko.pictureH = h; t.gecko.picturePlayed = true      // the film that plays in full screen is the picture that counts (a stream that steps down to a smaller size keeps its shape)
        if (first) {
            if (!t.isPrivateTab) Health.log("fullscreen", "film-picture", "${w}x$h from the full-screen player's report")
            fullscreenPictureKnown()
        }
    }

    internal fun onPageViewport(m: org.json.JSONObject) {
        val tab = tabOfReport(m.optString("p")) ?: return
        val pw = m.optInt("w"); val ph = m.optInt("h")
        val vw = geckoView.width; val vh = geckoView.height
        if (pw <= 0 || ph <= 0 || vw <= 0 || vh <= 0) return
        val matches = Math.abs(pw - vw) <= 3 && Math.abs(ph - vh) <= 3
        // ROT2: the evidence that this page can say when it has a turn's new size: it reports and its viewport is the view's (a page whose viewport never is, in desktop mode or zoomed, is not waited for)
        if (matches) tab.gecko.vpOk = true else if (!turnSettler.turning) tab.gecko.vpOk = false
        val quiet = tab.isPrivateTab                                           // a private tab keeps no counter and no log line of its page (the numbers still drive the turn)
        if (!fullscreenOn || fullscreenTab !== tab) {                          // the rest is for the page that IS full screen
            if (!quiet) { if (matches) vpIdleMatch++ else { vpIdleMiss++; vpIdleLast = "page ${pw}x$ph, view ${vw}x$vh" } }
            return
        }
        if (!quiet) vpChecks++
        turnSettler.pageReported(matches)                                     // ROT2: a turn waits for this (the cover stays until the page has the new size)
        if (matches) {
            if (!quiet && (vpTries > 0 || vpLast.startsWith("no "))) Health.log("fullscreen", "viewport", "right: page ${pw}x$ph, view ${vw}x$vh")
            if (!quiet) vpLast = "right (page ${pw}x$ph = view ${vw}x$vh)"
            vpTries = 0
            return
        }
        if (!quiet) { vpMismatches++; vpLast = "WRONG (page ${pw}x$ph, view ${vw}x$vh)" + (if (turnSettler.turning) " while the screen turns" else "") }
        if (turnSettler.turning) return                                       // ROT2: the engine is still handing the turn's size to the page: the turn waits (and nudges once if it takes long)
        // the race the nudge cures leaves ONE axis wrong (the display height against the view's); a page whose viewport is off on both axes by more than a tenth (desktop mode, a page with no viewport meta: a
        // 980 px wide layout) is not that, and a size change would only flicker (review of alpha53)
        if (Math.abs(pw - vw) > vw / 10 && Math.abs(ph - vh) > vh / 10) { if (!quiet) Health.log("fullscreen", "viewport", "not the display race (both axes off): page ${pw}x$ph, view ${vw}x$vh"); return }
        val now = SystemClock.uptimeMillis()
        if (vpTries >= 3 || now - vpLastResyncAt < 1200L) { if (!quiet) Health.log("fullscreen", "viewport", "still wrong: page ${pw}x$ph, view ${vw}x$vh (tries $vpTries)"); return }
        vpTries++; vpLastResyncAt = now; if (!quiet) vpResyncs++
        if (!quiet) Health.log("fullscreen", "viewport", "mismatch: page ${pw}x$ph, view ${vw}x$vh: the engine is given a real size change (try $vpTries)")
        resyncEngineSize()
    }

    /** The view one pixel shorter for 120 ms, then as it was: the engine takes the size from the view when the view's size really changes. */
    private fun resyncEngineSize() {
        if (turnHolding) return                                                // ANS52a: the view is held at its old size for the turn's first frame; a size change now would be a second one
        val lp = geckoView.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.bottomMargin = 1; geckoView.layoutParams = lp
        Io.main.removeCallbacks(vpUndo); Io.main.postDelayed(vpUndo, 120L)
    }

    private fun viewportLine() = "Full-screen page size (the engine sizes a page that goes full screen to the display; the page tells what it got): $vpChecks checks, $vpMismatches wrong, the engine was given a real size change $vpResyncs times; last: $vpLast; reports outside full screen: $vpIdleMatch were the view's size, $vpIdleMiss were not" + (if (vpIdleLast.isNotEmpty()) " (last: $vpIdleLast)" else "")

    private fun turnPageLine() = "Turning, the page's own pause (every video, also a muted one): ${Health.counter("turn.page.turns")} turns met by pages, videos played again ${Health.counter("turn.page.played")}, left paused ${Health.counter("turn.page.skipped")}, turns with nothing to pause ${Health.counter("turn.page.none")}"
    private fun videoBarLine(): String {
        val mode = listOf("Automatic", "always", "off")[prefs.videoControls]
        val acts = listOf("play", "pause", "seek", "mute", "full").joinToString(", ") { "$it ${Health.counter("videobar.act.$it")}" }
        return "Video controls ($mode): Kestrel's bar shown ${Health.counter("videobar.shown")} times, the page had controls of its own ${Health.counter("videobar.pageHasControls")} times; used: $acts"
    }
    private fun onSeekTaps(m: org.json.JSONObject) {
        seekStats.add(m.optInt("t"), m.optInt("s"), m.optInt("f"), m.optInt("o"), m.optString("r"), m.optInt("u"), m.optString("n"))
        Health.count("seekbar.reports")
        Health.log("seekbar", "report", "taps=${m.optInt("t")} page=${m.optInt("s")} ours=${m.optInt("f")} left=${m.optInt("o")} unknown=${m.optInt("u")} ${m.optString("r")} ${m.optString("n")}".trim())
    }

    // ---------------------------------------------------------------- RX3-BK1 / RX3-BK2: Back keeps the place (research/REVIEW-EXTERNAL-3.md section 3, research/BACK_SCROLL.md)
    /** What every Back / Forward press turned out to be (kept by the engine, loaded again from the cache or the network, time to the first paint, whether the place was reached). UI thread; never fed from a private tab. */
    private val backLab = BackLab { Health.log("back", "press", it) }
    private class BackCover(val tab: Tab, val savedY: Int) { var paintAt = 0L }
    private var backCover: BackCover? = null
    private val backCheckTick = Runnable { backCheck("tick") }
    private val backLabClose = Runnable { backLab.finish(SystemClock.uptimeMillis()) }

    /** The back picture is up and the page painted: it goes once the page is at the place it was left at (1.5 s after the paint at the latest). */
    private fun backPainted(why: String = "paint") {
        val c = backCover ?: return
        if (c.paintAt == 0L) c.paintAt = SystemClock.uptimeMillis()
        backCheck(why)
    }

    private fun backCheck(why: String) {
        val c = backCover ?: return
        val painted = c.paintAt != 0L
        val since = if (painted) SystemClock.uptimeMillis() - c.paintAt else 0L
        if (BackScrollRules.coverGoes(painted, c.savedY, c.tab.scrollY, since)) { hideRestoring("back-$why y=${c.tab.scrollY}/${c.savedY} after=${since}ms"); return }
        if (painted) { Io.main.removeCallbacks(backCheckTick); Io.main.postDelayed(backCheckTick, maxOf(50L, LastLookRules.AFTER_PAINT_MS - since)) } // the engine may paint before it has scrolled: look again at the latest 1500 ms after the paint
    }

    /**
     * A page the engine loaded again for a history move (its content script asks only then) asks where the owner left it (worker thread): the place this tab's memory holds for the page's own address, as text,
     * or "" when it holds none (then the engine's own place stands). It does not depend on the app's copy of the engine's history, which can lag behind a quick Back.
     */
    private fun backPlaceFor(page: String): String {
        var y = -1
        try { for (t in tabs.toTypedArray()) { val p = t.scrollByUrl.get(page) ?: -1; if (p > 0) { y = p; break } } } catch (e: Exception) { /* the list changed under us: no answer, the engine's own place stands */ }
        if (y > 0) Health.count("back.pull")
        return if (y > 0) y.toString() else ""
    }

    /** The engine committed [url] in the tab (UI thread): the first sighting of the target's address after a press is the lab's "address showed" time. */
    internal fun backLocated(url: String) { if (backLab.openKey() == BackScrollRules.keyOf(url)) backLab.located(SystemClock.uptimeMillis()) }

    /** The content script of the page in front reported a Back / Forward show (UI thread). */
    private fun onBackShown(page: String, kept: Boolean, navType: String, transfer: Long, y: Int) {
        // the engine's cache restored the page: it is up and at its exact place, and a picture put up for it (the guess "cannot have been kept" was wrong) goes at once. A kept page has no first paint to wait for
        if (kept) backCover?.let { c -> if (BackScrollRules.keyOf(c.tab.url) == BackScrollRules.keyOf(page)) hideRestoring("back-kept") }
        if (backLab.openKey() != BackScrollRules.keyOf(page)) return
        backLab.pageshow(SystemClock.uptimeMillis(), page, kept, navType, transfer, y)
        Io.main.removeCallbacks(backLabClose); Io.main.postDelayed(backLabClose, BackLab.CLOSE_AFTER_MS)
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
        endFullScreenOf(tab, "the tab was put to sleep")
        BackPictures.dropTab(tab.id)
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
        t.shields = Shields.effective(prefs, UrlParts.hostOf(t.url)) // the engine loads the new page by itself: its Shields (and Firefox's own protection with them) follow the address it was opened with
        syncEngineProtection(t)
        Io.main.postDelayed({ if (tabs.contains(t)) show(t) }, 60) // after the engine has opened the new session
        return s
    }

    internal fun closeTab(tab: Tab) { Io.main.post { doCloseTab(tab) } }

    private fun doCloseTab(tab: Tab) {
        val idx = tabs.indexOf(tab)
        if (idx < 0) return
        endFullScreenOf(tab, "the tab was closed")
        tabs.removeAt(idx)
        if (tab === restoringTab) hideRestoring("closed")
        tab.thumb = null
        val appCtx = applicationContext
        BackPictures.dropTab(tab.id)
        Io.exec { ThumbStore.delete(appCtx, tab.id); TabStateStore.delete(appCtx, tab.id); LastLook.delete(appCtx, tab.id) }
        tab.session?.let { s -> if (geckoView.session === s) geckoView.releaseSession(); try { s.close() } catch (e: Exception) { /* ignore */ } }
        tab.session = null
        if (current === tab) {
            val next = tabs.firstOrNull { it.id == tab.parentId } ?: tabs.getOrNull(idx) ?: tabs.getOrNull(idx - 1)
            if (next == null) show(newTabInternal()) else show(next)
        } else updateUi()
        scheduleSave()
    }

    private fun showNewTabPage(tab: Tab) {
        endFullScreenOf(tab, "the tab went back to the new tab page")
        tab.gecko.stateJson = null; tab.gecko.stateWritten = null; tab.gecko.stateWrittenUrl = null
        val appCtx = applicationContext; val tid = tab.id
        Io.exec { TabStateStore.delete(appCtx, tid); LastLook.delete(appCtx, tid) }
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
        tab.pendingNavAt = SystemClock.uptimeMillis()
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
        video.clearTab(tab)
        val s = ensureSession(tab, cover = false) // the address typed now replaces whatever the tab held: no preview of the old page
        syncEngineProtection(tab)
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
        silenceStrays() // SND1: the page stays alive underneath the Home screen, but nobody is looking at it: its video must not go on making sound
        updateUi()
        scheduleSave() // AQ1: the session file says "on Home" at once, not only when the app next goes to the background
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

    /**
     * Menu > Reload without cache (research/DORABY.md): the page AND every file it uses are fetched from the network again. The answer to "this site is half empty here and fine elsewhere" when a stale or
     * damaged cached file (the disk cache keeps files for months) or one bad answer from a CDN is the cause; the Page report in Diagnostics says which file failed.
     */
    private fun reloadFresh() {
        val t = current ?: return
        val s = t.session
        if (s == null || t.error != null) { retry(t); return }
        t.loading = true; updateUi()
        s.reload(org.mozilla.geckoview.GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
        Toast.makeText(this, "Reloading without the cache", Toast.LENGTH_SHORT).show()
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
        if (t.loading) { s.stop(); t.loading = false; t.progress = 0; t.pendingNav = null; updateUi() } else s.reload()
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
                syncEngineProtection(t) // BEFORE the reload: Firefox's own protection follows the site's Shields too
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
        if (cur != null && cur.homeOverPage) tabGrid.homePreview(cur, homeSnapshot()) // the tab is on the Home screen: its card shows Home, not the page behind it
        val appCtx = applicationContext
        val missing = tabs.filter { it.thumb == null && !it.isPrivateTab && !it.isNewTab }
        if (missing.isNotEmpty()) Io.fast { for (t in missing) { val b = ThumbStore.load(appCtx, t.id); if (b != null) Io.ui { if (t.thumb == null) { t.thumb = b; t.thumbAt = SystemClock.uptimeMillis(); tabGrid.thumbChanged(t) } } } }
    }

    private fun hideTabGrid() { tabGrid.visibility = View.GONE; applyPrivacyFlags() }

    /** The Home screen drawn into a small picture (about 240 px wide, like the page previews) for the tab grid; null when it is not laid out. Memory only: never written to disk. */
    private fun homeSnapshot(): android.graphics.Bitmap? = try {
        val w = home.width; val h = home.height
        if (w <= 0 || h <= 0 || home.visibility != View.VISIBLE) null else {
            val outW = 240; val scale = outW / w.toFloat(); val outH = (h * scale).toInt().coerceAtLeast(1)
            val b = android.graphics.Bitmap.createBitmap(outW, outH, android.graphics.Bitmap.Config.RGB_565)
            val c = android.graphics.Canvas(b)
            c.drawColor(android.graphics.Color.BLACK); c.scale(scale, scale); home.draw(c)
            b
        }
    } catch (e: Throwable) { null }

    private var readBackBlockedUntil = 0L

    /**
     * The one place that calls `GeckoView.capturePixels()` (RX4 lab finding: it is a synchronous call into the compositor that held the UI thread for 2 s and, on a starved emulator, for 20 s). The time the call takes
     * is measured; after a slow one nothing is read back for a while ([LastLookRules.readBackBackoffMs]) except for the tab grid ([force]). null = skipped.
     */
    private fun readBack(why: String, force: Boolean = false): GeckoResult<android.graphics.Bitmap>? {
        val t0 = SystemClock.uptimeMillis()
        val priv = current?.isPrivateTab == true // RV9: a private tab keeps no counters and no log lines
        if (!force && t0 < readBackBlockedUntil) { if (!priv) Health.count("look.readBackSkipped"); return null }
        val r = geckoView.capturePixels()
        val took = SystemClock.uptimeMillis() - t0
        val back = LastLookRules.readBackBackoffMs(took)
        if (back > 0) { readBackBlockedUntil = t0 + took + back; if (!priv) { Health.count("look.readBackSlow"); Health.log("look", "slow-read-back", "$why took ${took} ms: no more read-backs for ${back / 1000} s") } }
        return r
    }

    /**
     * Snapshot the page in front for the tab grid: a GPU read-back (not a screenshot, so it also works under FLAG_SECURE), scaled down on the IO thread.
     * Only for a page that is really on screen - not the Home screen, an error screen or a page that is still loading.
     */
    private fun captureThumb(tab: Tab, forBack: Boolean = false) {
        if (tab !== current || homeShown(tab) || tab.error != null || tab.session == null || geckoView.session !== tab.session) return
        val appCtx = applicationContext
        val leftUrl = tab.url // the address the picture belongs to (the tab may already show another one when the capture arrives)
        try {
            (readBack("thumb", force = tabGrid.visibility == View.VISIBLE) ?: return).accept({ full ->
                val lane: (() -> Unit) -> Unit = if (tabGrid.visibility == View.VISIBLE) Io::fast else Io::exec // the grid is open: the user is waiting for this preview
                if (full != null) lane {
                    val both = if (forBack) ThumbStore.shrinkWithPicture(full) else null // one capture, two sizes
                    val small = if (both != null) both.first else ThumbStore.shrink(full)
                    val picture = both?.second
                    if (picture != null) BackPictures.put(tab.id, leftUrl, picture)
                    if (forBack) Health.log("tabs", "leave-shot-done", "picture=${picture != null}")
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
    private val kbProbe by lazy { KeyboardProbe(this).also { it.onChange = { onKeyboardMoved() } } }

    /** The keyboard's probe window was laid out again (it showed, hid or changed height): look at once instead of waiting for the next poll (EX-11). Only while an address field has (or just had) the keyboard. */
    private var kbWatching = false
    private fun onKeyboardMoved() {
        if (!kbWatching) return
        Io.main.removeCallbacks(typingTick)
        typingTick.run() // re-posts itself
    }

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
            // once the probe has delivered an event on this device the poll is only a safety net (EX-11); until then (and on Android 11+, which has no probe window) the old cadence stays
            val proven = kbProbe.changes > 0
            if (focused) { Io.main.postDelayed(this, if (proven) 1000L else if (kbStillTicks >= 10) 300L else 100L); return }
            if (up && SystemClock.uptimeMillis() - typingModeSince < 1500) { Io.main.postDelayed(this, if (proven) 250L else 80L); return }
            kbWatching = false
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
        if (focused) { kbWatching = true; setUrlTypingMode(true); focusedAt = SystemClock.uptimeMillis(); updateNeonBusy() } else typingModeSince = SystemClock.uptimeMillis()
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

    private val warmPolicy = app.kestrel.core.net.WarmPolicy()
    private var pausedAt = 0L

    /**
     * EX-15 (research/REVIEW-EXTERNAL-1.md 4.2): Gecko closes its idle connections when the app goes to the background, so the first tap after coming back to a page starts with cold connections
     * (DNS, TCP, TLS: 2-3 round trips). After more than a few seconds away, the origin of the page in front is warmed again. Same rules as the typed suggestion: the "Warm up" switch, https only, never a
     * private tab, once per origin per 30 s.
     */
    private fun warmFrontOrigin() {
        val t = current ?: return
        if (pausedAt == 0L || SystemClock.uptimeMillis() - pausedAt < 5_000 || t.isNewTab || t.showHome) return
        preconnect(t.url, app.kestrel.core.net.WarmPolicy.Kind.RESUME)
    }

    /**
     * Opens the connection (DNS, TCP, TLS) to the site the top suggestion names while the owner is still typing, so the page's first bytes do not wait for the handshakes (about 1-3 round trips,
     * a second or more on a hotspot). Sites from history and bookmarks only, https only, never from a private tab, once per host per 30 s; Settings > Network lab can switch it off.
     */
    private fun preconnect(url: String, kind: app.kestrel.core.net.WarmPolicy.Kind = app.kestrel.core.net.WarmPolicy.Kind.SUGGESTION) {
        if (!url.startsWith("https://", true)) return
        val origin = GeckoEngine.warmOrigin(url) ?: return // the origin the page will really use: a suggestion with its own port is warmed on THAT port, not on 443
        if (!warmAllowed(kind, origin, null)) return
        Io.exec { GeckoEngine.speculativeConnect(origin) } // dispatched onto the Gecko thread: the public GeckoWebExecutor call is dropped by GeckoView 160 (EX-2)
    }

    /** The one place that decides whether a connection head start may be asked for (core WarmPolicy, tested): [page] is the address of the page whose script asked, null for the app's own triggers. */
    private fun warmAllowed(kind: app.kestrel.core.net.WarmPolicy.Kind, origin: String?, page: String?): Boolean {
        val front = current
        return warmPolicy.allow(app.kestrel.core.net.WarmPolicy.Ask(kind, origin, enabled = prefs.preconnect, saver = prefs.dataSaver != app.kestrel.core.shield.DataSaver.OFF,
            viaProxy = GeckoEngine.pagesViaProxy, frontPrivate = front?.isPrivateTab == true, pagePrivate = page != null && tabs.any { it.isPrivateTab && it.url == page },
            pageIsFront = page == null || front?.url == page, now = SystemClock.uptimeMillis()))
    }

    /**
     * EX-15 triggers 2 and 3 (research/REVIEW-EXTERNAL-1.md 4.2): the page's own script (extension content.js) names an origin the owner is likely to open next - the link a finger is on, or one of the first
     * three search results. Same rules as every warm-up (WarmPolicy): the Warm-up switch, https only, never a private tab, no Data saver, once per origin per 30 s, a touch only on the page in front.
     */
    private fun onWarmLink(origin: String, page: String, kind: String) {
        val o = GeckoEngine.warmOrigin(origin) ?: return
        val k = if (kind == "result") app.kestrel.core.net.WarmPolicy.Kind.RESULT else app.kestrel.core.net.WarmPolicy.Kind.TOUCH
        if (!warmAllowed(k, o, page)) return
        Io.exec { GeckoEngine.speculativeConnect(o) } // on the Gecko thread: the public call is dropped by GeckoView 160 (EX-2)
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
        const val DOWNLOADS = 8; const val UPDATE = 17; const val VIDEO = 18; const val DESKTOP = 9; const val IMAGES = 10; const val PRINT = 11; const val SETTINGS = 12; const val CLOSE_ALL = 16; const val PHONE = 19; const val RELOAD = 20 }

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
        if (t != null && !t.isNewTab) items.add(MenuItemSpec(M.RELOAD, "Reload without cache", null, R.drawable.ic_refresh))
        if (t != null && !t.isNewTab) items.add(MenuItemSpec(M.REFRESH, "Refresh screen", null, R.drawable.ic_refresh))
        items.add(MenuItemSpec(M.SETTINGS, "Settings", null, R.drawable.ic_settings)) // in private windows too (owner 2026-11-13: it must not disappear there); Back up / Restore decline by themselves in a private window
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
            M.RELOAD -> reloadFresh()
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
            M.PHONE -> if (!PhoneBridge.paired(this)) showSettings() else toast(if (PhoneBridge.toggleHelper(this)) "Phone helper on: downloads, videos and pages use your phone" else "Phone helper off: everything, pages included, goes straight through the hotspot")
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
            override fun clearCache(done: (Long) -> Unit) {
                // the engine's network and image caches (the bulk of the app's cache directory); the bytes that went are measured on the directory a moment later, off the UI thread
                val appCtx = applicationContext
                Io.exec {
                    val before = app.kestrel.browser.diag.StorageInfo.cacheBytes(appCtx)
                    Io.main.post {
                        try { GeckoEngine.runtime(this@BrowserActivity).storageController.clearData(StorageController.ClearFlags.ALL_CACHES) } catch (e: Exception) { Health.log("engine", "clear-cache-failed", e.toString().take(100)) }
                        // the engine moves what it dropped to cache2/doomed and unlinks it only at its next start: give that room back now (a second pass catches a big cache that was still being moved)
                        Io.main.postDelayed({ Io.exec { app.kestrel.browser.diag.StorageInfo.purgeDoomedCache(appCtx); val after = app.kestrel.browser.diag.StorageInfo.cacheBytes(appCtx); Io.main.post { done((before - after).coerceAtLeast(0L)) } } }, 2500)
                        Io.main.postDelayed({ Io.exec { app.kestrel.browser.diag.StorageInfo.purgeDoomedCache(appCtx) } }, 9000)
                    }
                }
            }
            override fun openDownloads() { sheet.dismiss(); showDownloads() }
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

    private var settingsSkinPrivate = false

    private fun showSettings() {
        // the colours are fixed when the view is built: Settings now opens in private windows too, so a view built for the other skin is thrown away and built again
        if (settingsView != null && settingsSkinPrivate != skinPrivate) { settingsView?.let { detach(it) }; settingsView = null }
        val v = settingsView ?: SettingsView(this, pal, prefs, settingsListener).also { settingsView = it; settingsSkinPrivate = skinPrivate }
        v.rebuild()
        if (v.parent == null) root.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun closeSettings() { settingsView?.let { detach(it) } }

    // ------------------------------------------------------------------ freeze evidence + manual recovery (research/FREEZE.md 5)
    private var diagView: DiagnosticsView? = null

    private fun engineFacts(): List<String> {
        val out = ArrayList<String>()
        val ver = try { Class.forName("org.mozilla.geckoview.BuildConfig").getField("MOZ_APP_VERSION").get(null) } catch (e: Exception) { "?" }
        out.addAll(StartTimeline.lines())
        out.add("Last look (the saved picture of the page, EY-1): " + (if (prefs.lastLook) "on" else "off") + ", ${LastLook.count(applicationContext)} stored, shown at start ${Health.counter("look.start")}, shown after a revive ${Health.counter("look.revive")}, taken ${Health.counter("look.saved")}, hidden ${Health.counter("look.hide")}")
        out.add(backLab.stats.line())
        out.add(adSkipStats.line() + if (prefs.skipVideoAds) "" else " (the Settings switch is off)")
        out.add(playerSkipStats.line() + if (prefs.skipVideoAds) "" else " (the Settings switch is off)")
        out.add(seekStats.line() + if (prefs.tapSeek) "" else " (the Settings switch is off)")
        out.add(fsTurner.line())
        out.add(fsBarsLine())
        out.add(turnSettler.line())
        out.add(turnPageLine())
        out.add(viewportLine())
        out.add(player.line())
        out.add(strayLine())
        out.add(videoBarLine())
        out.add(videoQuality.line(GeckoEngine.videoPolicyText))
        out.add(scrollGaps.line(prefs.measureScroll))
        out.add(app.kestrel.browser.shield.ScriptletSummary.read(applicationContext).ifEmpty { "Scriptlets in the lists: no compile report yet (the bundled lists are used until the first update)" })
        Engine.peek()?.let { fs -> out.add("YouTube scriptlet calls loaded: www.youtube.com " + fs.scriptletsFor("www.youtube.com").size + ", m.youtube.com " + fs.scriptletsFor("m.youtube.com").size) }
        out.add("Engine: GeckoView $ver; live sessions ${tabs.count { it.session != null }} of ${tabs.size} tabs (the memory budget keeps up to ${liveBudget()} ready, MemAvailable ${Health.memAvailableMb()} MB, setting ${listOf("automatic", "few", "many")[prefs.tabsKept]}); current ${current?.url?.take(60) ?: "-"}")
        out.add("Network: ${GeckoEngine.networkProfile} (VPN active: ${NetworkMonitor.vpn}, LinkMeter tunnel recognised: ${NetworkMonitor.fastTunnel}, downloads yield to a loading page at ${DownloadController.pageYieldKBps} KB/s)")
        out.add(app.kestrel.browser.download.DownloadService.wifiLockLine() + ". " + app.kestrel.browser.net.WifiBoost.line())
        out.add(DownloadController.windowGapLine())
        out.add(DownloadController.sizeLine())
        out.add(GeckoEngine.prefReadback)
        out.add(GeckoEngine.prefReadback2)
        out.add(GeckoEngine.videoPolicyText)
        out.add(app.kestrel.browser.diag.StorageInfo.text)
        out.add(app.kestrel.browser.diag.StorageTidy.line(applicationContext))
        out.add(app.kestrel.browser.diag.CpuMeter.line())
        if (::miniNav.isInitialized) { out.add(miniNav.glideLine()); out.add(miniNav.fieldLine()) }
        out.add(ShieldsBridge.statsLine())
        out.add(ShieldsBridge.pageReports.text(System.currentTimeMillis()))
        current?.takeIf { !it.isNewTab }?.let { t -> val b = ArrayList(t.engineBlocks); out.add("Firefox's own tracking protection, this page: " + (if (t.session?.settings?.useTrackingProtection == false) "OFF (Shields are down for this site); " else "on; ") + if (b.isEmpty()) "blocked nothing" else "blocked ${b.size}: " + b.take(12).joinToString("; ") + " (a page that is missing pictures or buttons: switch Shields off for this site from the shield icon)") }
        val animScale = try { android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) } catch (e: Exception) { 1f }
        out.add("Neon glow: ${Neon.status()}; system animation scale ${if (animScale == 0f) "off (the glow ignores it)" else "x$animScale"}")
        out.add("Page view: attached=${geckoView.parent != null} session=${geckoView.session != null} windowFocus=${hasWindowFocus()}")
        return out
    }

    private fun showDiagnostics() {
        GeckoEngine.readBackPrefs { diagView?.refresh() }
        Io.exec { if (DeviceVideo.read(applicationContext, force = true)) GeckoEngine.applyVideo(applicationContext, prefs); app.kestrel.browser.diag.StorageInfo.read(this); Io.ui { diagView?.refresh() } }
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

    /** Settings > Back up & restore: what only this activity knows (its tabs and dialogs); the flow itself is shared with the other browser (ui/BackupFlow). */
    private val backupHost = object : app.kestrel.browser.ui.BackupFlow.Host {
        override fun database() = db
        override fun isPrivateWindow() = isPrivate
        override fun tabsForBackup(): List<app.kestrel.core.backup.TabRef> = tabs.filter { !it.isPrivateTab && (it.url.startsWith("http://") || it.url.startsWith("https://")) }.map { app.kestrel.core.backup.TabRef(it.url, it.title) }
        override fun openTabs(refs: List<app.kestrel.core.backup.TabRef>): Int {
            // restored tabs wait like the tabs of a closed session: nothing loads until one is opened (no burst of pages over the hotspot)
            val open = tabs.filter { !it.isPrivateTab }.map { it.url.substringBefore('#') }.toHashSet()
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
            for (t in tabs) { t.shields = Shields.effective(prefs, t.topHost); syncEngineProtection(t) }
            GeckoEngine.applyPrefs(applicationContext)
            Io.exec { UpdateJob.schedule(applicationContext) }
            settingsView?.rebuild()
            refreshNtpIfShown()
            updateUi()
        }
        override fun appName(): String = Flavor.title + " " + (try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" })
    }
    private val backup by lazy { app.kestrel.browser.ui.BackupFlow(this, backupHost) }

    /** Settings > Export / Import what the keyboard learned (AX1): the file flow is shared with nothing else; the keyboard itself answers what is in it and takes what is in a file. */
    private val keyboardHost = object : app.kestrel.browser.ui.KeyboardFlow.Host {
        override fun isPrivateWindow() = isPrivate
        override fun exportText(done: (String) -> Unit) { if (::miniNav.isInitialized) miniNav.exportLearned(done) else done("") }
        override fun importText(text: String, done: (app.kestrel.core.input.glide.LearnedExport.Summary?) -> Unit) { if (::miniNav.isInitialized) miniNav.importLearned(text, done) else done(null) }
        override fun say(message: String) { Toast.makeText(this@BrowserActivity, message, Toast.LENGTH_LONG).show() }
        override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) { askYesNo(title, message, yes, no, result) }
    }
    private val keyboardFlow by lazy { app.kestrel.browser.ui.KeyboardFlow(this, keyboardHost) }

    private val settingsListener = object : SettingsListener {
        override fun closeSettings() { this@BrowserActivity.closeSettings() }
        override fun prefsChanged() {
            for (t in tabs) { t.shields = Shields.effective(prefs, t.topHost); syncEngineProtection(t) }
            GeckoEngine.applyPrefs(applicationContext)
            miniNav.available = prefs.miniNavToggle && !fullscreenOn
            miniNav.setGlideEnabled(prefs.glideTyping)
            app.kestrel.browser.net.WifiBoost.set(this@BrowserActivity, prefs.labWifiBoost) // the Network lab switch takes effect at once (the browser is on screen: this is its Settings)
            app.kestrel.browser.download.PageProxy.refresh()                                // "Keep the phone helper under Fast Wi-Fi" may have changed what applies
            Io.exec { UpdateJob.schedule(applicationContext) } // the update schedule may have changed
        }
        override fun clearBrowsingData() { this@BrowserActivity.clearBrowsingData() }
        override fun lastLookOff() { val c = applicationContext; if (lookTab != null) hideRestoring("look-off"); Io.exec { LastLook.clearAll(c) } }
        override fun tidyStorage() { runStorageTidy(true) }
        override fun forgetGlide() {
            prefs.glideWords = ""; prefs.glideHabit = floatArrayOf(0f, 0f)
            if (::miniNav.isInitialized) miniNav.forgetLearned() else Io.exec { try { java.io.File(applicationContext.filesDir, "glide_personal.txt").delete() } catch (e: Exception) { } }
            toast("The keyboard forgot what it learned")
        }
        override fun resetMiniGeometry() { prefs.miniGeom = null; if (::miniNav.isInitialized) miniNav.resetGeometry(); toast("The mini keyboard is back under the top right corner") }
        override fun resetSiteShields() { Shields.clearAll(); toast("Per-site Shields reset") }
        override fun backupNow() { backup.backupNow() }
        override fun restoreFromFile() { backup.restoreFromFile() }
        override fun exportKeyboard() { keyboardFlow.exportNow() }
        override fun importKeyboard() { keyboardFlow.importFromFile() }
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
        out.add("Browser engine" to "Firefox engine (GeckoView 160). Pages are drawn like in current Firefox, independent of this tablet's old Android System WebView. " +
            "The engine is updated by installing a newer Kestrel build.")
        out.add("User agent" to "Chosen by the engine (a current Firefox for Android identity)")
        val am = getSystemService(ActivityManager::class.java)
        val ram = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }.totalMem
        out.add("Device" to "RAM ${"%.1f".format(ram / 1_073_741_824.0)} GB, app memory class ${am?.memoryClass} MB, low-RAM mode: ${am?.isLowRamDevice}")
        out.add("Safe Browsing" to "Firefox's own Safe Browsing works in this browser without Google Play services: the engine keeps Mozilla's and Google's lists of dangerous sites up to date by itself and shows a warning page before a phishing or malware page opens (tested on both companies' test pages in an Android 10 emulator, research/EMU_LAB.md). Kestrel's own blocking lists, which update daily, add to it. Files you save with the download manager are not looked at by the engine's download protection; Kestrel asks before it saves a file a page started by itself and looks inside an installer before it opens it.")
        out.add("Licenses" to "Filter lists: EasyList and EasyPrivacy (GPLv3+ / CC BY-SA 3.0), uBlock Origin filters (GPLv3), Fanboy Cookiemonster (CC BY 3.0 / GPLv3). Public Suffix List (MPL-2.0). Personal, non-commercial use.")
        return out
    }

    // ================================================================== data / state

    private fun clearBrowsingData() {
        GeckoEngine.runtime(this).storageController.clearData(StorageController.ClearFlags.ALL)
        val appCtx = applicationContext
        BackPictures.clear()
        for (t in tabs) { t.gecko.stateJson = null; t.gecko.stateWritten = null; t.gecko.stateWrittenUrl = null } // the saved Back stacks are browsing data too
        Io.exec { db?.clearHistory(); ThumbStore.clearAll(appCtx); TabStateStore.wipe(appCtx); LastLook.clearAll(appCtx) }
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
        val records = keep.map { TabRecord(it.id, withoutUserinfo(it.url).ifEmpty { "about:blank" }, it.title, it.lastActive, it.parentId, it.desktop, it.homeOverPage) }
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
    private fun refreshNtp() { home.bindStats(prefs.blockedTotal, NetworkMonitor.metered, NetworkMonitor.vpn) }

    private var lastVpn = NetworkMonitor.vpn
    private var lastTunnel = NetworkMonitor.fastTunnel

    private fun onNetworkChanged() {
        if (NetworkMonitor.vpn != lastVpn || NetworkMonitor.fastTunnel != lastTunnel) {   // LinkMeter's Fast Wi-Fi came up or went away (its own tunnel is recognised a moment after the VPN itself): DNS, HTTP/3, IPv6 and the download page rate follow it (core/net/NetProfile), the Courier rests or returns
            lastVpn = NetworkMonitor.vpn; lastTunnel = NetworkMonitor.fastTunnel
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
        miniNav.sync(canBack || (t != null && (!t.isNewTab || t.showHome)), t?.canGoForward == true, homeShown(t), if (t == null || homeShown(t) || !t.url.startsWith("http")) "" else t.url)
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
        Neon.busy(app.kestrel.core.session.NeonRules.busy(heavyLoad, kbTopScreen > 0, keyboardComing, t?.gecko?.videoPlaying == true))   // RX3-S1: a playing video holds the glow still too
    }
    /** RX3-S1: the page's video started or stopped; only the tab in front matters for the glow. UI thread (the engine's delegates run there). */
    internal fun videoStateChanged(tab: Tab) { Health.log("neon", "video", "playing=${tab.gecko.videoPlaying} front=${tab === current}"); if (tab === current) updateNeonBusy() }
    private var loadSeen: Tab? = null
    private var loadSeenAt = 0L

    private var gridRebindPending = false
    internal fun tabChanged(tab: Tab) {
        if (tab === lookTab) lookCheck("tab") // a redirect changes the address: the saved picture then shows another page
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
        val tab = (if (pageUrl.isNotEmpty()) tabOfTouch(pageUrl, kind) ?: current?.takeIf { it.url.substringBefore('#') == pageUrl.substringBefore('#') } ?: tabWithUrl(pageUrl) ?: tabForPage(pageUrl) else null) ?: current ?: return // the same address may be open in several tabs: the one in front is the one the owner tapped in
        if (kind == "tap") { tab.lastTapAt = SystemClock.uptimeMillis(); tab.lastTapHref = url; return } // a real tap in the page, and the link it was on: the jump guard's evidence
        if (kind == "vmenu") { // a long press on a video or audio element, taken by the page script before the site's own player menu could appear (owner request X14)
            if (!tab.isPrivateTab) Health.log("video", "long-press-menu", if (url.startsWith("http", true)) "with its address" else "no address")
            if (tab === current && !isFinishing) video.longPress(tab, url.takeIf { it.startsWith("http", true) }, null) { link -> val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigateTo(t, link) }
            return
        }
        tab.blocked.addAndGet(app.kestrel.core.filter.Category.ANNOYANCE, 1)
        tab.pendingLifetime.addAndGet(1)
        shieldCountChanged(tab)
        Health.count("guard.$kind")
        if (tab !== current) return
        // AR1: one very small icon at the bottom middle for one second (a second block keeps it and the newest address wins); a press opens the blocked address in a NEW tab, nothing asked.
        // A forced redirect is opened the same way: the page the owner is on stays where it is.
        Health.log("guard", "chip-show", "$kind ${UrlParts.hostOf(url) ?: url.take(40)}")
        blockedChip.show(pal, url) { link -> Health.log("guard", "chip-open", UrlParts.hostOf(link) ?: link.take(40)); val t = newTabInternal(tab, tab.isPrivateTab); show(t); navigate(t, link) }
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
        val cross = !app.kestrel.core.filter.Psl.default.isSameSite(from, to)
        val listed = cross && listedAdHost(from, to)
        val v = app.kestrel.core.guard.JumpGuard.decide(
            crossSite = cross,
            touchAgeMs = now - lastTouchAt,
            tapArrived = tab.lastTapAt >= lastTouchAt - 50,
            tapAgeMs = now - tab.lastTapAt,
            tapHref = tab.lastTapHref,
            uri = uri,
            adHost = listed,
        )
        if (cross) Health.log("guard", "jump-judged", "to=$to listed=$listed touchAge=${now - lastTouchAt} ms -> $v")
        return v
    }

    /** RX3-G6: is [host] (a destination the page wants to jump to, third-party to [pageHost]) one the block lists name as an ad or tracker host? Host rules only: one hash lookup, no page address, no query. */
    private fun listedAdHost(pageHost: String, host: String): Boolean {
        val fs = app.kestrel.browser.shield.Engine.peek() ?: return false
        return try { app.kestrel.core.filter.Verdict.isBlocked(fs.check("https://$host/", pageHost, app.kestrel.core.filter.RequestType.SCRIPT)) } catch (e: Exception) { false }
    }

    /**
     * A page asks for a NEW window (a link with target=_blank, window.open). The page-side guard (guard-main.js) already refuses what a script opens without a real tap on that very link; this is the second wall
     * for what reaches the engine anyway (an iframe race, the guard not yet in a frame). RX3-G5: it uses the same evidence ([app.kestrel.core.guard.PopupGuard]): the same site, a sign-in / payment host, or a real
     * tap on a link to exactly this address; before, ANY window that came within 2.5 s of ANY touch (the play press) was allowed. The verdict is STOP (the caller shows the blocked-window icon and refuses) or WAIT
     * (the tap report is still on its way through the extension: ask again for at most [app.kestrel.core.guard.PopupGuard.WAIT_MS]). A `$popup` rule of the lists stops it in every case.
     */
    internal fun judgePopup(tab: Tab, uri: String): app.kestrel.core.guard.PopupGuard.Verdict {
        if (!prefs.blockPopups) return PopupVerdict.ALLOW
        val host = tab.topHost
        if (!Shields.effective(prefs, host).shieldsUp) return PopupVerdict.ALLOW
        val to = UrlParts.hostOf(uri) ?: return PopupVerdict.ALLOW
        val fs = Engine.peek()
        if (fs != null && Verdict.isBlocked(fs.check(uri, host, RequestType.POPUP))) return PopupVerdict.STOP
        // (the engine's own "user gesture" flag is false for a window.open inside a tap on some pages, so the app's own record of the last finger touch decides)
        val now = SystemClock.uptimeMillis()
        return app.kestrel.core.guard.PopupGuard.decide(
            sameSite = host != null && app.kestrel.core.filter.Psl.default.isSameSite(host, to),
            touchAgeMs = now - lastTouchAt,
            tapArrived = tab.lastTapAt >= lastTouchAt - 50,
            tapAgeMs = now - tab.lastTapAt,
            tapHref = tab.lastTapHref,
            uri = uri,
            authHost = app.kestrel.core.guard.PopupGuard.isAuthHost(to),
            guardReady = true,   // the guard is declared in the manifest since RX3-G1: it is in every document from its first script; the app has no per-page signal to ask
        )
    }

    internal fun errorChanged(tab: Tab) {
        if (tab.error != null && !tab.isPrivateTab) Health.log("page", "error", "host=${UrlParts.hostOf(tab.url) ?: ""} kind=${tab.error?.javaClass?.simpleName}") // SOAK1: the emulator lab counts these
        if (tab === restoringTab && tab.error != null) hideRestoring("error") // the error screen replaces the preview
        if (tab === current) { attachContent(tab); updateUi() }
    }

    internal fun pageFinished(tab: Tab, real: Boolean = true) {
        if (tab === current) StartTimeline.mark("stop")
        flushCounters()
        if (real && tab === restoringTab) {
            // the load ended (a page that never reported its first paint must not keep the preview up). The back cover (RX3-BK2) counts the end of the load as the paint and then still waits for the place: it never goes at the end of the load alone
            if (tab === backCover?.tab) backPainted("stop") else hideRestoring("page-stop")
        }
        // A failed or blocked load is not a visit (typos and malware URLs would otherwise fill history and the top sites).
        if (!tab.isPrivateTab && tab.error == null && tab.url.startsWith("http")) { val u = withoutUserinfo(tab.url); val ti = tab.title; Io.exec { db?.recordVisit(u, ti) } }
        scheduleSave()
        if (tab === current) updateUi()
        if (real) Io.main.postDelayed({ if (tab === current && !tab.loading) captureLook(tab, "page-stop") }, LastLookRules.AFTER_STOP_MS.toLong()) // EY-1: the page as it settled
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
        endFullScreenOf(tab, if (crashed) "the page's process crashed" else "the page's process was killed")
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

    private fun showRestoring(tab: Tab, crashed: Boolean, message: String? = null, picture: android.graphics.Bitmap? = null, dim: Int = 0xB3, timeoutMs: Long = 20_000) {
        Health.log("tabs", "cover-show", "tab=${tab.id} thumb=${tab.thumb != null} picture=${picture != null}")
        restoringTab = tab
        if (picture == null && lookFor(tab, message ?: if (crashed) "The page stopped - bringing it back" else "Page freed to save memory - bringing it back")) {
            Io.main.removeCallbacks(restoreTimeout); Io.main.postDelayed(restoreTimeout, LastLookRules.CAP_MS.toLong()) // a look is the page itself: it is held as long as the cap says
            return
        }
        val thumb = picture ?: tab.thumb
        restoreOverlay.show(thumb, message ?: if (crashed) "The page stopped - bringing it back" else "Page freed to save memory - bringing it back", dim)
        Io.main.removeCallbacks(restoreTimeout)
        Io.main.postDelayed(restoreTimeout, timeoutMs) // never stay up for ever (a page that never paints, a dead connection)
    }

    private fun hideRestoring(why: String = "") {
        if (restoringTab != null) Health.log("tabs", "cover-hide", "tab=${restoringTab?.id} why=$why")
        Io.main.removeCallbacks(restoreTimeout); Io.main.removeCallbacks(backCheckTick); backCover = null
        if (lookTab != null) { Health.count("look.hide"); Health.log("tabs", "look-hide", "tab=${lookTab?.id} why=$why") }
        restoringTab = null
        restoreOverlay.hide()
        clearLook()
    }

    // ---------------------------------------------------------------- the last look (EY-1)

    private fun clearLook() {
        Io.main.removeCallbacks(lookNoteShow); Io.main.removeCallbacks(lookCheckTick)
        lookTab = null; lookMeta = null; lookPending = 0L; lookPaintAt = 0L; lookStopped = false
    }

    /**
     * A saved look is up for [tab]: either the early cover's (put up in the very first frame; the restore has now made its tab) or, for a tab that comes back after being freed or after its process died,
     * the newest saved picture of that page when it matches. [note] is the slim line at the bottom for the second case (the owner is still told the page had to be loaded again).
     */
    private fun lookFor(tab: Tab, note: String): Boolean {
        val pm = lookMeta
        if (restoreOverlay.showingLook && lookPending == tab.id && pm != null && LastLookRules.samePage(pm.url, tab.url)) { lookTab = tab; lookPending = 0L; Health.log("tabs", "look-adopted", "tab=${tab.id}"); return true }
        val look = savedLook(tab.id, tab.url, tab.desktop) ?: return false
        restoreOverlay.showLook(look.bitmap, note, note, onDrawn = { Health.log("tabs", "look-drawn", "tab=${tab.id} revive") })
        lookTab = tab; lookMeta = look.meta; lookPending = 0L; lookPaintAt = 0L; lookStopped = false
        Health.count("look.revive"); Health.log("tabs", "look-shown", "tab=${tab.id} revive ${look.meta.w}x${look.meta.h} y=${look.meta.scrollY}")
        return true
    }

    /** The live page painted: the picture goes once the page is where the picture was (research/REVIEW-EXTERNAL-2.md "Hide rule"). */
    private fun lookPainted() { if (lookPaintAt == 0L) lookPaintAt = SystemClock.uptimeMillis(); lookCheck("paint") }

    private fun lookCheck(why: String) {
        val t = lookTab ?: return; val m = lookMeta ?: return
        if (!LastLookRules.samePage(t.url, m.url)) { hideRestoring("look-other-page"); return } // a redirect: the picture shows another page than the one that is coming
        val painted = lookPaintAt != 0L
        val since = if (painted) SystemClock.uptimeMillis() - lookPaintAt else 0L
        if (LastLookRules.hideNow(painted, m.scrollY, t.scrollY, false, since)) { hideRestoring("look-$why y=${t.scrollY}/${m.scrollY} after=${since}ms"); return }
        if (painted) { Io.main.removeCallbacks(lookCheckTick); Io.main.postDelayed(lookCheckTick, maxOf(50L, LastLookRules.AFTER_PAINT_MS - since)) } // the engine may paint before it has scrolled: look again at the latest 1500 ms after the paint
    }

    /**
     * The page (top frame) says where it is scrolled (UI thread; the page sends it 300 ms after its last scroll and once after its load, so a place the engine restored by itself is reported too). While a look is
     * up this is the live page moving to its place; otherwise it arms the "settled" capture (700 ms after the last scroll in all).
     */
    private fun lookScrolled(tab: Tab, y: Int, page: String) {
        tab.scrollByUrl.put(page, y) // RX3-BK2: where the owner is on this page. The key is the report's OWN address, so a late report of the page that was just left never lands on the next page
        if (!LastLookRules.samePage(page, tab.url)) return // a late report of another page: not the place of the page in front
        tab.scrollY = y
        if (backLab.openKey() == BackScrollRules.keyOf(page)) backLab.scrolled(SystemClock.uptimeMillis(), y) // RX3-BK1
        if (tab !== current) return
        if (backCover?.tab === tab) { backCheck("scroll"); return }
        if (lookTab === tab) { lookCheck("scroll"); return }
        if (!prefs.lastLook) return
        lookScrollAt = SystemClock.uptimeMillis() - LastLookRules.PAGE_DEBOUNCE_MS
        if (!lookScrollArmed) { lookScrollArmed = true; Io.main.postDelayed(lookScrollTimer, (LastLookRules.SETTLE_MS - LastLookRules.PAGE_DEBOUNCE_MS).toLong()) }
    }

    /**
     * Take the page's picture for the next start (a regular tab, a real web page, not loading, nothing over it). [why] names the trigger in the Health list. [minGapMs]: how old the last picture must be.
     * The read-back is the tab grid's own call; the encode and the write are on a worker. Never for a private tab.
     */
    private fun captureLook(tab: Tab?, why: String, minGapMs: Long = LastLookRules.MIN_GAP_MS.toLong()) {
        if (fullscreenOn) return // ANS51g (owner message 51: the tablet's log shows 'tabs look-saved' twice WHILE a film was full screen): the picture read back then is the film's frame, not the page's look, and the read-back and the encode compete with the decoder on a slow tablet
        if (tab == null || !prefs.lastLook || tab !== current || tab.isPrivateTab || homeShown(tab) || tab.error != null || tab.loading || tab.session == null || geckoView.session !== tab.session) return
        if (restoringTab != null || lookTab != null || lookCapturing || tabGrid.visibility == View.VISIBLE || sheet.isShowing || !LastLookRules.worthKeeping(tab.url)) return
        if (why != "pause" && typing()) return // nobody is typing into the address field (leaving the page is the one moment that is taken whatever is on screen: the Recents screen shows the same)
        val now = SystemClock.uptimeMillis()
        if (now - lastLookAt < minGapMs) return
        if (geckoView.width <= 0 || geckoView.height <= 0) return
        lastLookAt = now; lookCapturing = true
        val appCtx = applicationContext
        val id = tab.id; val url = tab.url; val sy = tab.scrollY; val desktop = tab.desktop
        val appearance = LastLook.appearance(appCtx, prefs.webDark); val dark = prefs.webDark == 1
        val keep = LastLookRules.keep(id, tabs.filter { !it.isPrivateTab && !it.isNewTab }.associate { it.id to it.lastActive })
        try {
            (readBack("look-$why") ?: run { lookCapturing = false; return }).accept({ full ->
                if (full == null) lookCapturing = false
                else Io.exec {
                    try {
                        val ok = LastLook.save(appCtx, id, full, LastLookMeta(url, sy, full.width, full.height, System.currentTimeMillis(), Flavor.engineVersion, appearance, dark, desktop))
                        if (ok) { Health.count("look.saved"); LastLook.retain(appCtx, keep) }
                        Health.log("tabs", "look-saved", "tab=$id why=$why ok=$ok y=$sy")
                    } finally { if (!full.isRecycled) full.recycle(); lookCapturing = false }
                }
            }, { _ -> lookCapturing = false /* the surface was not ready: the previous picture stays */ })
        } catch (e: Throwable) { lookCapturing = false }
    }

    /** The engine painted real content for [tab] (called from ContentDelegate.onFirstContentfulPaint). */
    internal fun firstPaint(tab: Tab) {
        if (tab === current) StartTimeline.mark("paint")
        if (backLab.openKey() == BackScrollRules.keyOf(tab.url)) backLab.painted(SystemClock.uptimeMillis()) // RX3-BK1: the press this paint belongs to
        if (tab === restoringTab) { if (tab === lookTab) lookPainted() else if (tab === backCover?.tab) backPainted() else hideRestoring("first-paint") }
    }

    // ================================================================== back handling (works on Android 10 and on 16's predictive back)

    private fun installBackHandling() {
        if (Build.VERSION.SDK_INT >= 33) {
            val cb = android.window.OnBackInvokedCallback { if (!handleBack()) moveTaskToBack(true) }
            backCallback = cb
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb)
        }
    }

    /** Debug builds only (the emulator lab): where does a Back key go? */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP && (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            Health.log("key", "back-up", "fullscreenOn=$fullscreenOn canceled=${event.isCanceled}")
        return super.dispatchKeyEvent(event)
    }

    // Serves Android 10-12. On Android 13+ (incl. 16's predictive back) the OnBackInvokedCallback registered in installBackHandling() runs instead.
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Used below API 33; API 33+ uses OnBackInvokedCallback")
    override fun onBackPressed() { if (!handleBack()) moveTaskToBack(true) }

    /** @return true if the back press did something. */
    /** The controls' exit button: the same as Back (ask the page, and end it here when the engine does not answer, RV2). */
    private fun exitFullscreenFromControls() {
        val ft = fullscreenTab ?: return
        Health.log("fullscreen", "exit-button", "asking the page to leave full screen")
        try { ft.session?.exitFullScreen() } catch (e: Exception) { Health.log("fullscreen", "exit-failed", e.toString().take(80)) }
        Io.main.postDelayed({ if (fullscreenOn && fullscreenTab === ft) endFullScreenOf(ft, "the engine did not answer the exit button") }, 600L)
    }

    private fun handleBack(): Boolean {
        // FS1: Back leaves full screen first, like every video player (it used to go back in the page's history, which only left full screen by leaving the page)
        if (fullscreenOn) {
            val ft = fullscreenTab
            if (ft != null && ft === current && ft.session != null) {
                Health.log("fullscreen", "back-exit", "asking the page to leave full screen")
                try { ft.session?.exitFullScreen() } catch (e: Exception) { Health.log("fullscreen", "exit-failed", e.toString().take(80)) }
                // the engine answers with "full screen ends" within moments; when it does not (a page that refuses, a dying process) Back would do nothing for good: end it here (RV2)
                Io.main.postDelayed({ if (fullscreenOn && fullscreenTab === ft) endFullScreenOf(ft, "the engine did not answer Back") }, 600L)
                return true
            }
            endFullScreenOf(null, "Back: the full-screen page is not in front or has no session") // then Back does its ordinary job
        }
        if (::miniNav.isInitialized && miniNav.onBackPressed()) return true // the small keyboard closes first
        if (sheet.isShowing) { sheet.dismiss(); return true }
        if (downloadsView?.parent != null) { closeDownloads(); return true }
        if (diagView?.parent != null) { closeDiagnostics(); return true }
        if (settingsView?.parent != null) { closeSettings(); return true }
        if (listPanel?.parent != null) { closeList(); return true }
        if (tabGrid.visibility == View.VISIBLE) { hideTabGrid(); return true }
        if (findBar.visibility == View.VISIBLE) { closeFind(); return true }
        if (suggestions.visibility == View.VISIBLE || home.suggest.visibility == View.VISIBLE || typing()) { hideSuggestions(); clearUrlFocus(); updateUi(); return true }
        val t = current ?: return false
        if (t.showHome && !t.isNewTab) { t.showHome = false; attachContent(t); updateUi(); scheduleSave(); return true } // Home laid over a page: Back returns to the page
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

    /** The tab whose page is full screen now (RV2: only an engine "ends" from THIS tab, or the app itself when the page is gone, ends it). */
    private var fullscreenTab: Tab? = null

    /**
     * RV2 (independent review): the page that was full screen is gone or no longer in front (its session was killed, hibernated, closed or replaced): GeckoSession.close() and a dead content process never
     * send "full screen ends", so the dock, the immersive bars and the forced orientation would stay. The app ends it itself ([tab] null = whatever tab it was).
     */
    private fun endFullScreenOf(tab: Tab?, why: String) {
        val ft = fullscreenTab
        if (!fullscreenOn || ft == null || (tab != null && tab !== ft)) return
        Health.log("fullscreen", "ended-by-app", why)
        try { ft.session?.exitFullScreen() } catch (e: Exception) { /* the session is already gone */ }
        fullScreenChanged(ft, false)
    }

    internal fun fullScreenChanged(tab: Tab, on: Boolean) {
        if (!on && fullscreenTab != null && fullscreenTab !== tab) return // the late "ends" of a tab we already left: the full screen of the tab in front goes on
        fullscreenTab = if (on) tab else null
        fullscreenOn = on
        miniNav.available = !on && prefs.miniNavToggle
        dock.visibility = if (on) View.GONE else View.VISIBLE
        progress.visibility = if (on) View.GONE else View.VISIBLE
        applyImmersive(on)
        root.requestApplyInsets() // FSB1: the padding follows the bars: zero while the system hides them, their size while it keeps them
        vpTries = 0; Io.main.removeCallbacks(vpUndo); vpUndo.run() // FB4: a new full screen starts clean (the view is back to its full height)
        player.quiet = fullscreenTab?.isPrivateTab == true || (!on && player.quiet)   // a private tab's full screen is not logged or counted (kept until it has ended)
        player.fullscreen(on, prefs.fsControls == 0)                          // VP1: Kestrel's controls over the film (drawn once the page reports one)
        if (!on) player.quiet = false
        if (on) { fsTurner.enter(); if (prefs.fullscreenBars == 1) checkBarsSoon() else { fsBarsNote = "kept: Settings > System bars in full-screen video is on Keep (the default); the page is laid out between the bars" ; Health.log("fullscreen", "bars", fsBarsNote) } } else { fsTurner.exit(); Io.main.removeCallbacks(barsCheck) } // FS1: the screen follows the tablet while a page is full screen
    }

    /**
     * FB1 (owner message 47): the system bars are hidden while a page is full screen ONLY when the person chose "Hide" (Settings > System bars in full-screen video); by default they stay and the page is laid out
     * between them, exactly as for every ordinary page, where the dock is never under a bar. alpha48 to alpha50 asked for hidden bars (alpha49 without the layout flags) and on the owner's tablet nothing changed:
     * the tablet's window manager laid the window out over the whole screen for the request while its System UI kept drawing both bars on top, and no inset says so (the insets report the bars as gone).
     * Not asking is the only layout that cannot go wrong. Debug builds only: `setprop debug.kestrel.nohide 1` never asks, whatever the setting.
     */
    private fun applyImmersive(on: Boolean) {
        if (app.kestrel.browser.diag.DebugProps.get(this, "debug.kestrel.nohide") == "1") { Health.log("fullscreen", "bars", "debug switch nohide: the system bars are NOT asked to hide (on=$on)"); return }
        val hide = on && prefs.fullscreenBars == 1
        if (Build.VERSION.SDK_INT >= 30) {
            val c = window.insetsController ?: return
            if (hide) { c.hide(WindowInsets.Type.systemBars()); c.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } else c.show(WindowInsets.Type.systemBars())
            if (::player.isInitialized) player.setBottomInset(if (hide) navBarPx() else 0)       // VP3, see navBarPx
        } else {
            @Suppress("DEPRECATION")
            // Read-modify-write: assigning a whole new value would also clear the light status/navigation bar flags the theme set (icons invisible afterwards).
            val immersive = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            @Suppress("DEPRECATION")
            val older = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // (alpha48 set these too: cleared if they are still on)
            val dv = window.decorView
            @Suppress("DEPRECATION")
            dv.systemUiVisibility = if (hide) (dv.systemUiVisibility and older.inv()) or immersive else dv.systemUiVisibility and (immersive or older).inv()
            if (::player.isInitialized) player.setBottomInset(if (hide) navBarPx() else 0)       // VP3, see navBarPx
        }
    }

    /**
     * VP3 (owner message 49, on alpha50: "if I pull up the navigation bar, it covers the video controls"): while the system bars are hidden for a full-screen film (Settings > System bars: Hide), a swipe from the
     * bottom edge pulls the navigation bar up OVER the picture, and the controls under it cannot be used. The pulled-up bar is a TRANSIENT one (sticky immersive): Android 10 does not tell the app when it
     * comes (lab run 43776782679: no visibility callback, no inset change; its picture kp-11 shows the bar over the film), so the controls' bottom row always keeps the bar's height free while the bars are hidden.
     * The size is the one the system reports as stable (it stays the same whether the bar shows or not), else the system's dimension, else 48 dp.
     */
    private fun navBarPx(): Int {
        val wi = try { window.decorView.rootWindowInsets } catch (e: Throwable) { null }
        @Suppress("DEPRECATION")
        val fromInsets = if (wi == null) 0 else if (Build.VERSION.SDK_INT >= 30) wi.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom else wi.stableInsetBottom
        if (fromInsets > 0) return fromInsets
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else (48 * resources.displayMetrics.density).toInt()
    }

    // ---- FSB1: what the system really did with its bars while a page is full screen (Diagnostics says it, the owner's next screenshot is no longer needed)
    private val fsBars = IntArray(4)                  // left, top, right, bottom of the system bars as the last insets reported them
    private var fsBarsNote = "no full screen yet"
    private var barsRehidden = false
    private val barsCheck = Runnable { checkBars() }
    private val rehideAfterTurn = Runnable { if (fullscreenOn && prefs.fullscreenBars == 1) { applyImmersive(true); checkBarsSoon() } }

    private fun barsSeen(l: Int, t: Int, r: Int, b: Int) { fsBars[0] = l; fsBars[1] = t; fsBars[2] = r; fsBars[3] = b }

    private fun checkBarsSoon() { barsRehidden = false; Io.main.removeCallbacks(barsCheck); Io.main.postDelayed(barsCheck, 700L) }

    /** 700 ms after full screen began: a bar that still takes room is asked to go once more (some systems miss the first request, made while the screen turns); what remains is written down. */
    /**
     * What the system bars take of the window RIGHT NOW. The decor of an Android 10 window pads itself and consumes the insets before they reach [root] (its listener always hears zero there), so the raw
     * insets of the decor view are read: the same source as the keyboard probe. Where they are not available, what the listener last heard.
     */
    private fun systemBarsNow() {
        val wi = try { window.decorView.rootWindowInsets } catch (e: Throwable) { null } ?: return
        if (Build.VERSION.SDK_INT >= 30) { val i = wi.getInsets(WindowInsets.Type.systemBars()); barsSeen(i.left, i.top, i.right, i.bottom) }
        else @Suppress("DEPRECATION") barsSeen(wi.systemWindowInsetLeft, wi.systemWindowInsetTop, wi.systemWindowInsetRight, wi.systemWindowInsetBottom)
    }

    private fun checkBars() {
        if (!fullscreenOn) return
        systemBarsNow()
        val shown = fsBars[0] > 0 || fsBars[1] > 0 || fsBars[2] > 0 || fsBars[3] > 0
        if (shown && !barsRehidden) {
            barsRehidden = true
            Health.log("fullscreen", "bars", "still showing after 700 ms (left ${fsBars[0]} top ${fsBars[1]} right ${fsBars[2]} bottom ${fsBars[3]} px): asked again")
            applyImmersive(true); root.requestApplyInsets()
            Io.main.postDelayed(barsCheck, 1500L)
            return
        }
        fsBarsNote = if (shown) "KEPT by this tablet (left ${fsBars[0]}, top ${fsBars[1]}, right ${fsBars[2]}, bottom ${fsBars[3]} px): the page is laid out inside them, so nothing of a player is under a bar"
                     else "hidden (the picture is full-bleed)"
        Health.log("fullscreen", "bars", fsBarsNote)
    }

    private fun fsBarsLine() = "Full-screen system bars: $fsBarsNote"

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
        syncEngineProtection(tab)
        tab.pageAllowed = h != null && Engine.peek()?.isPageAllowed(h) == true
    }

    /**
     * Firefox's own tracking protection (the engine's Disconnect-list blocking: a second blocker beside ours) follows the site's Shields: it is off while Shields are down for the site or its
     * "Block ads & trackers" switch is off, so a page that protection breaks (doraby.com, owner message 19) can be fixed from the Shields panel without a new release. The per-session flag
     * (GeckoSessionSettings.useTrackingProtection) is applied by the engine to the next request: PROVEN on the emulator with a tracker test entry (flag on: the third-party script is refused and
     * the shield counts it; flag off: it loads), research/TABLET_DIAG_2026-11-13.md section 2. Called BEFORE every load starts and before the panel reloads. Debug builds: `setprop debug.kestrel.notp 1` forces it off.
     */
    internal fun syncEngineProtection(tab: Tab) {
        val st = tab.session?.settings ?: return
        val on = tab.shields.let { it.shieldsUp && it.blockAds } && app.kestrel.browser.diag.DebugProps.get(this, "debug.kestrel.notp") != "1"
        if (st.useTrackingProtection != on) {
            st.useTrackingProtection = on
            Health.log("engine", "etp", "${tab.topHost ?: "?"} ${if (on) "on" else "off"}")
        }
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
        if (backup.onResult(requestCode, resultCode, data)) return
        if (keyboardFlow.onResult(requestCode, resultCode, data)) return
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
        val dlg = AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton(yes) { _, _ -> if (!answered) { answered = true; result(true) } }
            .setNegativeButton(no) { _, _ -> if (!answered) { answered = true; result(false) } }
            .setOnCancelListener { if (!answered) { answered = true; result(false) } }
            .show()
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) { // the emulator lab reads the question and answers it (uiautomator cannot see this window while the page animates)
            labDialog = dlg
            Health.log("dialog", "shown", "title=${title.take(70)} | ${message.replace('\n', ' ').take(260)} | $yes/$no")
        }
    }

    private var labDialog: AlertDialog? = null

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

    /**
     * A script on a page has been running for a long time (AY1, owner message 39: "I see this prompt ... too much. Automatically have the browser wait without showing a prompt. If it is waiting too long I
     * will close the tab myself"): the answer is always WAIT and nothing is shown. The engine asks again after the next interval if the script is still busy, and every answer is counted for Diagnostics
     * (`gecko.slowScript.waited`) so a page that keeps the engine busy is visible there.
     */
    internal fun askSlowScript(tab: Tab): GeckoResult<SlowScriptResponse> {
        tab.slowScriptAt = SystemClock.uptimeMillis()
        val n = Health.count("gecko.slowScript.waited")
        if (n == 1 || n % 20 == 0) Health.log("gecko", "slow-script-waited", "n=$n " + (if (tab.isPrivateTab) "private tab" else tab.url.take(60)))
        return GeckoResult.fromValue(SlowScriptResponse.CONTINUE)
    }

    // ================================================================== downloads, external links, long-press

    internal fun download(tab: Tab, url: String, userAgent: String?, contentDisposition: String?, mime: String?, length: Long, nameOverride: String? = null, headers: Map<String, String>? = null, redirected: Boolean = false) {
        val name = nameOverride ?: Downloads.fileName(url, contentDisposition, mime)
        val limit = prefs.dataSaver.confirmDownloadBytes(NetworkMonitor.metered)
        val big = limit >= 0 && (length < 0 || length >= limit)
        val risky = Downloads.isRisky(name, mime)
        val cleartext = url.startsWith("http://", ignoreCase = true) // on a hotspot the carrier can read or change an unencrypted download
        // RX4-X2: a download nobody tapped (a timer clicking a hidden link, a redirect to a file) asks first; the owner's own address and own buttons never count as "by itself"
        val now = SystemClock.uptimeMillis()
        val guard = app.kestrel.core.download.DownloadGuardRules.Input(
            touchAgeMs = if (lastTouchAt == 0L) Long.MAX_VALUE else now - lastTouchAt, ownerNavigation = tab.pendingNav != null && now - tab.pendingNavAt < 15_000L, risky = risky, big = big, cleartext = cleartext,
            fromBlob = url.startsWith("blob:", ignoreCase = true), ownRequest = nameOverride != null || headers != null, declinedAgoMs = autoDownloadDeclinedAt[declineKey(tab, url)]?.let { now - it } ?: Long.MAX_VALUE,
            touchToPageStartMs = if (tab.gecko.pageStartedAt == 0L || lastTouchAt == 0L) Long.MIN_VALUE else tab.gecko.pageStartedAt - lastTouchAt, pageItself = url == tab.gecko.pageStartUrl || redirected) // RV4: a tapped link that redirects to the file (a release asset, a drive link) is still the page's own link
        when (app.kestrel.core.download.DownloadGuardRules.decide(guard)) {
            app.kestrel.core.download.DownloadGuardRules.Verdict.GO -> { startDownload(url, userAgent, contentDisposition, mime, tab, nameOverride, headers); leaveDownloadTab(tab); return }
            app.kestrel.core.download.DownloadGuardRules.Verdict.REFUSE_QUIETLY -> {
                if (!tab.isPrivateTab) { Health.count("download.autoRefused"); Health.log("download", "auto-refused", UrlParts.hostOf(url) ?: "") }
                toast("Blocked another download this page started by itself"); leaveDownloadTab(tab); return
            }
            app.kestrel.core.download.DownloadGuardRules.Verdict.ASK -> { /* the question below */ }
        }
        val byItself = app.kestrel.core.download.DownloadGuardRules.byItself(guard)
        if (byItself && !tab.isPrivateTab) { Health.count("download.autoAsked"); Health.log("download", "auto-asked", "${UrlParts.hostOf(url) ?: ""} age=${if (guard.touchAgeMs == Long.MAX_VALUE) -1 else guard.touchAgeMs}ms") }
        val size = if (length >= 0) NewTabPage.fmtBytes(length) else "unknown size"
        val msg = buildString {
            val note = app.kestrel.core.download.DownloadGuardRules.note(guard)
            if (note.isNotEmpty()) append(note).append("\n\n")
            append(name).append("\nFrom: ").append(UrlParts.hostOf(url) ?: "unknown site").append("\nSize: ").append(size)
            if (cleartext) append("\n\nThis download is not encrypted: your mobile carrier or anyone on the network could read or change the file.")
            if (big) append("\n\nYou're on a metered connection (your phone's hotspot). This may use a lot of mobile data.")
            if (risky) append("\n\nThis type of file can install software or run code. Only continue if you trust the site.")
        }
        // RV7: one such question per tab at a time: a page that fires file after file would stack a dialog for each; while one is open the rest are refused quietly
        if (byItself) {
            if (!autoQuestionTabs.add(tab.id)) { Health.count("download.autoRefused"); toast("Blocked another download this page started by itself"); leaveDownloadTab(tab); return }
        }
        askYesNo(if (byItself) "A page started a download" else "Download this file?", msg, "Download", "Cancel", { ok ->
            if (byItself) autoQuestionTabs.remove(tab.id)
            if (ok) startDownload(url, userAgent, contentDisposition, mime, tab, nameOverride, headers)
            else if (byItself) autoDownloadDeclinedAt[declineKey(tab, url)] = SystemClock.uptimeMillis() // the next self-started downloads of this SITE in this tab are refused quietly for a minute
            leaveDownloadTab(tab)
        })
    }

    /** RV7: the decline is remembered per tab AND site (the next site opened in the tab starts clean); the tabs that have a "page started a download" question open now. */
    private val autoDownloadDeclinedAt = HashMap<String, Long>()
    private val autoQuestionTabs = HashSet<Long>()
    private fun declineKey(tab: Tab, url: String) = "${tab.id}|${UrlParts.hostOf(url) ?: ""}"

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

    private fun startDownload(url: String, ua: String?, cd: String?, mime: String?, tab: Tab, nameOverride: String? = null, given: Map<String, String>? = null) {
        val go = {
            // Referer = the page's ORIGIN only (never the full URL: it can carry tokens / search terms); a video file found by the sniffer brings the headers its player used (a server that checks "who is asking")
            val headers = HashMap<String, String>()
            if (given != null) headers.putAll(given)
            else UrlParts.parse(tab.url)?.let { headers["Referer"] = "${it.scheme}://${it.hostForAuthority}/" }
            // a video named like a release, or one whose own name says nothing on a page that names the episode, is saved as "Ozark S02E05.mp4" (owner message 26, AL6); every other file keeps the server's name
            val raw = nameOverride ?: Downloads.fileName(url, cd, mime)
            val named = if (nameOverride == null) MediaNames.friendlyFileName(raw, mime, tab.title, tab.url, UrlParts.hostOf(tab.url) ?: "") else raw
            DownloadController.enqueue(this, Source(url, tab.url.takeIf { it.isNotEmpty() }, named, mime, headers, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, nameFixed = nameOverride != null || named != raw))
            askNotificationPermissionOnce() // the banner at the top says "Download started" (DownloadPopup); no toast any more
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
        downloadPopup.suppress(true) // the full list is open: the small window would only repeat it
    }

    private fun closeDownloads() { downloadsView?.let { root.removeView(it) }; downloadsView = null; if (::downloadPopup.isInitialized) downloadPopup.suppress(false) }

    private val inspecting = HashSet<Long>() // downloads whose file is being looked into before it opens (RV6)

    private fun openDownload(v: DownloadView) {
        val uri = v.destUri ?: run { toast("The file is not available"); return }
        val go = {
            try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), v.mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
            catch (e: Exception) { toast("No app on this tablet can open this file") }
        }
        val installerMaybe = v.name.endsWith(".apk", true) || v.name.endsWith(".xapk", true) || v.mime == "application/vnd.android.package-archive" || app.kestrel.core.download.ApkIdentity.extensionCanLie(v.name)
        val risky = Downloads.isRisky(v.name, v.mime)
        if (!installerMaybe) {
            if (risky) askYesNo("Open this file?", "${v.name} can install software or run code. Only open it if you trust where it came from.", "Open", "Cancel", { ok -> if (ok) go() }) else go()
            return
        }
        // RX4-X1: look inside before anything opens: what is it, and does it pretend to be Kestrel? (IO thread: the signature check of a big file takes a moment)
        val appCtx = applicationContext
        // RV6: the user-awaited lane (Io.exec is the one nice-13 thread of the tidy-up and the picture saves), and one look at a time per file: a second tap while it reads must not start the player twice
        if (!inspecting.add(v.id)) return
        Io.fast {
            val info = try { ApkInspect.read(appCtx, Uri.parse(uri)) } catch (e: Throwable) { null }
            Io.ui {
                inspecting.remove(v.id)
                if (info == null) {
                    if (risky) askYesNo("Open this file?", "${v.name} can install software or run code. Only open it if you trust where it came from.", "Open", "Cancel", { ok -> if (ok) go() }) else go()
                    return@ui
                }
                val offer = UpdateManager.state.offer
                // RV3: a page controls a file's name and size (Content-Disposition, Content-Length: both are public on the release), so they prove nothing. What a page cannot set is WHERE the file was saved:
                // only the Updates screen asks the download manager for the app-private place.
                val fromUpdater = offer != null && v.kind == app.kestrel.core.download.DEST_APP_PRIVATE && v.name == offer.apkName
                when (app.kestrel.core.download.ApkIdentity.verdict(info.pkg, packageName, fromUpdater)) {
                    app.kestrel.core.download.ApkIdentity.Verdict.OWN_UPDATE_OK -> go()
                    app.kestrel.core.download.ApkIdentity.Verdict.REFUSE_LOOKALIKE -> {
                        if (!v.priv) { Health.count("download.lookalikeRefused"); Health.log("download", "lookalike-refused", info.pkg) } // RV9: nothing kept for a private download
                        askYesNo(app.kestrel.core.download.ApkIdentity.LOOKALIKE_TITLE, app.kestrel.core.download.ApkIdentity.LOOKALIKE_TEXT, "Delete the file", "Leave it", { del -> if (del) DownloadController.remove(v.id, true) })
                    }
                    app.kestrel.core.download.ApkIdentity.Verdict.SHOW_FACTS -> {
                        val words = app.kestrel.core.download.ApkIdentity.dangerousWords(info.permissions)
                        askYesNo("Open this app installer?", app.kestrel.core.download.ApkIdentity.facts(info.label, info.pkg, info.permissions.size, words, info.installedLabel, info.sameSigner), "Open", "Cancel", { ok -> if (ok) go() })
                    }
                }
            }
        }
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
        if (web) { labels.add("Open in new tab"); acts.add { openInBackground(tab, url) } }
        if (web && !tab.isPrivateTab) { labels.add("Open in private tab"); acts.add { val t = newTabInternal(null, privateTab = true); show(t); navigateTo(t, url) } }
        labels.add("Copy link address"); acts.add { copy(url) }
        labels.add("Share link"); acts.add { share(url) }
        if (imageUrl != null && NavPolicy.isWebUrl(imageUrl)) { labels.add("Download image"); acts.add { startDownload(imageUrl, null, null, null, tab) } }
        if (video.available(tab)) { labels.add("Download video from this page"); acts.add { video.show(tab) } }
        AlertDialog.Builder(this).setTitle(url.take(80)).setItems(labels.toTypedArray()) { _, i -> acts[i]() }.show()
    }

    /**
     * "Open in new tab" from a long-press: the page loads in a tab BEHIND the one in front and the foreground stays exactly where it is (owner 2026-11-13). The new tab is a regular session that is
     * told it is not visible (less memory, the engine still loads it); it counts as recently used so the live-tab limit hibernates an older tab first, never this one and never the tab in front.
     */
    private fun openInBackground(from: Tab, url: String) {
        val t = newTabInternal(from, from.isPrivateTab) // a private tab opens a private tab: it lives in the same window kind
        t.lastActive = System.currentTimeMillis()
        navigateTo(t, url)                              // creates the session and loads; attachContent() runs only for the tab in front, so the view does not change
        t.session?.setActive(false)
        enforceLiveLimit()
        updateUi()                                      // the tab counter in the dock
        scheduleSave()
        toast("Opened in a new tab")
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
        /** A page that said a script has been running a long time cannot answer a long press: the touch detector takes over (AY2). The engine asks again every 20 s while the script runs, so a stuck page keeps this fresh, and a page whose script finished is normal again after this long. */
        private const val SLOW_PAGE_MS = 30_000L
        /** A long press that the page reports this long after the finger lifted is still the press that was just made (the page's answer takes a moment on a slow page). */
        private const val OPEN_SPACE_FRESH_MS = 2_000L
    }
}
