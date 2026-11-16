package app.kestrel.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Standing rules the owner gave that live in code the JVM tests cannot run (the engine module has no unit-test source set): each one is pinned by reading the source, so a later edit that quietly undoes
 * a rule fails here. The text of the rule is the owner's own.
 */
class OwnerRulesTest {
    private fun source(path: String): String {
        val f = listOf(path, "../$path").map { File(it) }.firstOrNull { it.exists() } ?: throw IllegalStateException("$path not found (run from the repository root or core/)")
        return f.readText()
    }

    /** The text of the function that starts at [signature]: up to the line that closes it (a line holding only the closing brace at the same indent). */
    private fun body(text: String, signature: String): String {
        val start = text.indexOf(signature)
        assertTrue("$signature exists", start >= 0)
        val indent = text.lastIndexOf('\n', start).let { text.substring(it + 1, start).takeWhile { c -> c == ' ' }.length }
        val close = "\n" + " ".repeat(indent) + "}\n"
        val end = text.indexOf(close, start)
        assertTrue("the end of $signature", end > start)
        return text.substring(start, end + close.length)
    }

    // AY1, owner message 39: "I see this prompt ... too much. Automatically have the browser wait without showing a prompt. If it is waiting too long I will close the tab myself."
    @Test fun aSlowScriptIsWaitedForWithoutAnyPrompt() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val fn = body(act, "internal fun askSlowScript(")
        assertTrue(fn, fn.contains("SlowScriptResponse.CONTINUE"))
        for (banned in listOf("askYesNo", "AlertDialog", "SlowScriptResponse.STOP", "Toast", "show(")) assertFalse("a slow script must never ask the person ($banned): $fn", fn.contains(banned))
        val delegate = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/TabDelegates.kt")
        assertTrue("the engine's question goes straight to that answer", delegate.contains("override fun onSlowScript(session: GeckoSession, scriptFileName: String): GeckoResult<SlowScriptResponse>? = a.askSlowScript(tab)"))
    }

    // AY2, owner message 39: "long press anywhere in the browser and it will show up under where I long pressed, but only if I am long pressing an open space. If I'm long pressing a link, give me the options for that link, not the navigation panel."
    @Test fun aLongPressOnOpenSpaceShowsThePanelUnderTheFingerAndALinkKeepsItsOwnMenu() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val zone = body(act, "override fun zone(): IntArray? {")
        assertFalse("the detector's zone is the whole page area, not a square in the corner: $zone", zone.contains("SIZE_DP"))
        assertTrue("the detector stands aside for a page that can answer for itself: $zone", zone.contains("pageAnswersLongPress()"))
        val fired = body(act, "override fun fired(downTime: Long, x: Float, y: Float) {")
        assertTrue("the panel comes under the finger: $fired", fired.contains("miniNav.showAt(x, y)"))
        assertFalse("and nothing toggles it any more: $fired", fired.contains("toggle"))
        val open = body(act, "internal fun onOpenSpace(page: String) {")
        assertTrue(open, open.contains("miniNav.showAt(pressX, pressY)"))
        val content = source("geckoapp/src/main/assets/kestrel-shields/content.js")
        for (kind in listOf("'A'", "IMG", "VIDEO", "INPUT", "TEXTAREA", "BUTTON", "isContentEditable", "cursor === 'pointer'")) assertTrue("content.js must treat $kind as not open space", content.contains(kind))
        assertTrue("a link keeps the app's own menu: the press on a link never reaches the panel", content.contains("if (interactive(els[i])) return;"))
    }

    // AY4, owner message 39: "Make the navigation panel a floating panel which I can move around easily by long pressing it and dragging it around"
    @Test fun thePanelFloatsWhereItIsPutAndALongPressPicksItUp() {
        val nav = source("app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt")
        val finish = body(nav, "private fun finish(ev: MotionEvent) {")
        assertFalse("no snapping back to the corner when it is let go: $finish", finish.contains("dock()") || finish.contains("SNAP"))
        assertFalse("no snap distance any more", nav.contains("SNAP_DP"))
        val pick = body(nav, "private fun pickUp(at: Long) {")
        assertTrue(pick, pick.contains("mode = DRAG_MOVE"))
        assertTrue("the panel is picked up by a long press on anything that is not a key", nav.contains("ZONE_BODY -> if (!onKeys(ev.x, ev.y)) armPick(ev)"))
    }

    // MN1 / MN2, owner message 41: "it doesn't close anymore. I should be able to long press again above where it is on the screen and close it. Also ... if I long press and drag the panel, a large x icon appears
    // at the bottom of the screen and if I drag it into the x, it closes the panel and hides it until I long press somewhere to open it again"
    @Test fun thePanelClosesByALongPressAboveItAndByDraggingItIntoALargeXAtTheBottom() {
        val nav = source("app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt")
        val show = body(nav, "fun showAt(xWin: Float, yWin: Float) {")
        assertTrue("a long press above the open panel closes it: $show", show.contains("val above = isAbovePanel(yWin)") && show.contains("if (above) { setOpen(false, true); return }"))
        val zone = body(nav, "internal fun zoneAt(x: Float, y: Float): Int {")
        assertTrue("only points inside the panel are the panel's (the page gets everything above it): $zone", zone.contains("if (x < 0f || y < 0f || x > w || y > h) return ZONE_NONE"))
        val finish = body(nav, "private fun finish(ev: MotionEvent) {")
        assertTrue("letting go over the X closes and hides the panel: $finish", finish.contains("val dropped = m == DRAG_MOVE && ev.actionMasked == MotionEvent.ACTION_UP && closeTarget.hot") && finish.contains("setOpen(false, animate = false)"))
        assertTrue("a cancelled touch never closes it (only ACTION_UP counts)", !finish.contains("ACTION_CANCEL"))
        val pick = body(nav, "private fun pickUp(at: Long) {")
        assertTrue("the X shows the moment the panel is picked up: $pick", pick.contains("showCloseTarget(true)"))
        assertTrue("the X is large", Regex("""const val CLOSE_SIZE_DP = (\d+)""").find(nav)!!.groupValues[1].toInt() >= 64)
        assertTrue("the X takes no touch of its own", nav.contains("init { isClickable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }"))
    }

    // HM1, owner message 41, item 1: "I just pressed the home button to put a tab on the home screen, switched tabs to a different tab, then came back to the tab ... but instead of the home screen it loaded the site I was on
    // before the home screen. Since I was on the home screen last on this tab, it should have loaded the home screen."
    @Test fun aTabThatWasLeftOnTheHomeScreenComesBackAsTheHomeScreen() {
        val a = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val show = body(a, "private fun show(tab: Tab) {")
        assertFalse("switching to a tab never clears its Home screen: $show", show.contains("showHome = false"))
        assertTrue("the Home button still puts the Home screen over the page", body(a, "override fun onHome() {").contains("t.showHome = true"))
    }

    // YS1, owner message 42: "I want the seek bar for videos inside the browser to act like other browsers and video player apps-- if I press a spot on the video player bar, it should jump the video to that part of the bar."
    @Test fun aTapOnAPlayersBarJumpsTheVideoUnlessThePageDidItAndTheOwnerCanSwitchItOff() {
        val manifest = source("geckoapp/src/main/assets/kestrel-shields/manifest.json")
        val entry = Regex("""\{[^{}]*"seekbar\.js"[^{}]*\}""").find(manifest)?.value ?: ""
        assertTrue("seekbar.js runs in every http(s) page and frame from document start: $entry", entry.contains("\"all_frames\": true") && entry.contains("document_start") && entry.contains("https://*/*"))
        assertFalse("and on YouTube too (it is the owner's case): $entry", entry.contains("exclude_matches"))
        val js = source("geckoapp/src/main/assets/kestrel-shields/seekbar.js")
        for (banned in listOf("preventDefault", "stopPropagation", "stopImmediatePropagation", "dispatchEvent", ".click(", "fastSeek")) assertFalse("the script never cancels, stops or fakes a page event ($banned)", js.contains(banned))
        assertTrue("the video is moved only after the app said yes", body(js, "function settle(d, x) {").contains("askApp(function (yes) {") && js.contains("r.seek === true"))
        assertTrue("and never while the page already moved it", body(js, "function settle(d, x) {").contains("pageMoved(d)"))
        val bridge = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt")
        val flags = bridge.lines().first { it.contains("val guard = ") }
        assertTrue("flag 16 follows only the Settings switch (a site with Shields off keeps tap-to-seek): $flags", flags.contains("or (if (prefs().tapSeek) 16 else 0)"))
        assertTrue("the extension relays the numbers", source("geckoapp/src/main/assets/kestrel-shields/background.js").contains("m.k !== 'sbk'") && bridge.contains("\"sbk\" -> { seekTaps(m); \"\" }"))
        assertTrue("the switch is on by default", source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("""var tapSeek: Boolean get() = bool("tap_seek", true)"""))
        assertTrue("Settings has the row", source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("""toggle("Tap the video bar to jump""""))
        assertTrue("Diagnostics shows the numbers", source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt").contains("out.add(seekStats.line()"))
    }

    // AY3, owner message 39: "make the navigation panel keyboard space key able to move the text cursor if I press and hold it then move my finger left or right, just like other keyboard apps do"
    @Test fun theSpaceKeyMovesTheTextCursorWhenSlid() {
        val nav = source("app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt")
        assertTrue(nav.contains("keys.onCursor = { n -> cursorSlid(n) }"))
        val slid = body(nav, "private fun cursorSlid(delta: Int) {")
        assertTrue("the address bar's cursor and the page field's caret both move: $slid", slid.contains("model.moveCursor") && slid.contains("moveCursor(delta)"))
    }
    // VC1 + LP1, owner message 44: "I played a video on finance.yahoo.com in Kestrel, and the player had no controls at all ... Anywhere I can start a video in the browser, I should be able to control it ... the
    // download options should come up for videos when I long press them instead of the floating notification panel." The player is a shadow-root web component: every video feature has to look INSIDE shadow roots.
    @Test fun aVideoWithNoControlsGetsKestrelsBarAndALongPressOnAVideoIsTheVideoMenuNotThePanel() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val manifest = source(dir + "manifest.json")
        assertTrue("vfind.js loads before content.js, and videobar.js is declared", manifest.indexOf("\"vfind.js\"") in 0 until manifest.indexOf("\"content.js\"") && manifest.contains("\"videobar.js\""))
        val content = source(dir + "content.js")
        assertTrue("the video-menu long press finds a video inside a shadow root", content.contains("if (!media && window.KestrelVF) media = window.KestrelVF.at(e.clientX, e.clientY);"))
        assertTrue("the open-space (panel) long press never fires on a video, wherever it hides", content.contains("if (window.KestrelVF && window.KestrelVF.at(e.clientX, e.clientY)) return;"))
        assertTrue("a long press inside a frame asks the page above before it reports open space", content.contains("k: 'ft', t: 'lp'"))
        val vfind = source(dir + "vfind.js")
        assertTrue("shadow roots, closed ones too", vfind.contains("openOrClosedShadowRoot") && vfind.contains("collect(sr, vids, budget, 1, 'video')"))
        val bar = source(dir + "videobar.js")
        assertTrue("the bar is judged after a tap and only when the app allows it", bar.contains("r.vb === true") && bar.contains("T = { judge: 450"))
        assertTrue("the bar is built with DOM calls and CSSOM only (a page's content-security-policy cannot block it)", !bar.contains("innerHTML") && !bar.contains("<style") && bar.contains("style.cssText"))
        val bg = source(dir + "background.js")
        assertTrue("the page-config answer carries the Settings choice", bg.contains("vb: (f & 64) !== 0, vba: (f & 128) !== 0"))
        assertTrue("frames are relayed to the top frame", bg.contains("m.k !== 'ft'") && bg.contains("k: 'ftq'"))
        val bridge = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt")
        assertTrue("flags 64 (Automatic or Always) and 128 (Always) follow ONLY the Settings row", bridge.contains("(if (prefs().videoControls != 2) 64 else 0) or (if (prefs().videoControls == 1) 128 else 0)"))
        assertTrue("the Settings row exists", source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("choice(\"Video controls\""))
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        assertTrue("a private tab leaves no trace of the bar", body(act, "private fun onVideoBar(m: org.json.JSONObject) {").contains("if (pageIsPrivate(m.optString(\"p\"))) return"))
    }

    // FS1, owner message 43: "make it so if I watch videos in full screen, then rotate the tablet or press a rotate button, the video switches to landscape mode just like regular video player apps."
    @Test fun fullScreenVideoTurnsWithTheTabletAndTheRotateButton() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val fs = body(act, "internal fun fullScreenChanged(tab: Tab, on: Boolean) {")
        assertTrue(fs, fs.contains("fsTurner.enter()") && fs.contains("fsTurner.exit()"))
        // RV2 (independent review): the page that was full screen can vanish without the engine saying so (session closed, killed, hibernated, replaced): the app ends the full screen itself, and Back forces the exit
        val ends = body(act, "private fun endFullScreenOf(tab: Tab?, why: String) {")
        assertTrue(ends, ends.contains("fullScreenChanged(ft, false)"))
        for (spot in listOf("private fun hibernate(tab: Tab) {", "private fun doCloseTab(tab: Tab) {", "private fun showNewTabPage(tab: Tab) {", "internal fun sessionGone(tab: Tab, crashed: Boolean) {", "private fun show(tab: Tab) {"))
            assertTrue("$spot ends a full screen of its tab", body(act, spot).contains("endFullScreenOf("))
        assertTrue("Back forces the exit when the engine does not answer", body(act, "private fun handleBack(): Boolean {").contains("endFullScreenOf(ft, \"the engine did not answer Back\")"))
        assertTrue("a page's screen.orientation.lock() has somebody who answers it", act.contains("OrientationController.OrientationDelegate") && act.contains("fsTurner.siteLocked(orientation)") && act.contains("fsTurner.siteUnlocked()"))
        assertTrue("a finger lifted in full screen shows the rotate button", act.contains("if (fullscreenOn) fsTurner.touched()"))
        assertTrue("Back leaves full screen before anything else", body(act, "private fun handleBack(): Boolean {").lines().drop(1).first { it.isNotBlank() && !it.trim().startsWith("//") }.contains("if (fullscreenOn)"))
        assertTrue("Diagnostics shows the numbers", act.contains("out.add(fsTurner.line())"))
        val turner = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/FullscreenTurner.kt")
        assertTrue("the turner really asks Android to turn the screen", turner.contains("a.requestedOrientation = o"))
        assertTrue("and puts back what it was when full screen ends", body(turner, "    fun exit() {").contains("setRequested(saved"))
        val rule = source("core/src/main/kotlin/app/kestrel/core/media/FullscreenTurn.kt")
        assertTrue("the screen follows the sensor, whatever Android's rotation lock says", rule.contains("Plan(FULL_SENSOR, false)") && rule.contains("const val FULL_SENSOR = 10"))
        assertTrue("Settings has the choice", source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("""choice("Turn the screen for full-screen video""""))
        assertTrue("it is on (automatic) by default", source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("""sp.getInt("fs_turn", 0)"""))
        assertFalse("the manifest never pins the screen: the page, the person and the rotate button decide", source("geckoapp/src/main/AndroidManifest.xml").contains("screenOrientation"))
    }

    // FSB1 / ROT1, owner message 46 (3 screenshots): "the controls in full screen get covered up by the Android bottom navigation bar", "when I rotate the tablet ... it is laggy and glitchy. It may benefit from
    // pausing the video shortly until the orientation is set", "I don't see any video player controls in full screen landscape mode". The tablet KEEPS both system bars in full screen and alpha48 laid the page
    // out under them (root padding zeroed while a page is full screen + the LAYOUT_* flags on Android 10).
    @Test fun aFullScreenPageIsNeverLaidOutUnderASystemBarThatIsStillShowing() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val listener = act.substringAfter("root.setOnApplyWindowInsetsListener { v, insets ->").take(1400)
        assertFalse("the padding follows the real insets also in full screen: no zeroing for fullscreenOn ($listener)", listener.contains("fullscreenOn"))
        assertTrue("the insets listener tells what the system did with its bars", listener.contains("barsSeen("))
        val immersive = body(act, "private fun applyImmersive(on: Boolean) {")
        assertTrue(immersive, immersive.contains("val immersive = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN\n"))
        assertFalse("no layout flags are ASKED for (they put the window under the bars whether or not the system hides them)", immersive.contains("dv.systemUiVisibility or older") || immersive.contains("or immersive or older"))
        assertTrue("the flags of an older build are cleared", immersive.contains("and older.inv()"))
        assertTrue("a system dialog or Recents may bring the bars back: they are asked to go again when the window has focus (only when the person chose Hide)", body(act, "override fun onWindowFocusChanged(hasFocus: Boolean) {").contains("if (hasFocus && fullscreenOn && prefs.fullscreenBars == 1) { applyImmersive(true)"))
        assertTrue("and after a turn", body(act, "override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {").contains("rehideAfterTurn") && act.contains("private val rehideAfterTurn = Runnable { if (fullscreenOn && prefs.fullscreenBars == 1) { applyImmersive(true)"))
        val check = body(act, "private fun checkBars() {")
        assertTrue("a bar that still takes room is asked once more and what remains is written down", check.contains("barsRehidden") && check.contains("fsBarsNote"))
        assertTrue("the bars are read from the decor's raw insets (an Android 10 decor consumes them before the root's listener: alpha49 review)", check.contains("systemBarsNow()") && body(act, "private fun systemBarsNow() {").contains("window.decorView.rootWindowInsets"))
        assertTrue("Diagnostics says what the system did with its bars", act.contains("out.add(fsBarsLine())"))
        assertTrue("a debug switch plays a tablet that keeps its bars (the emulator lab)", act.contains("debug.kestrel.nohide"))
    }

    @Test fun theVideoIsPausedAndThePageCoveredWhileTheScreenTurnsAndLetGoWhenItIsStill() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val config = body(act, "override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {")
        assertTrue("the turn is met BEFORE the engine is told", config.indexOf("turnSettler.configChanged(landscape)") in 0 until config.indexOf("orientationChanged()"))
        assertTrue("the app leaving the screen lets everything go without playing", body(act, "override fun onPause() {").contains("turnSettler.abandon()"))
        assertTrue("a finger during a turn is the person taking over", act.contains("turnSettler.touchedScreen()"))
        assertTrue("the page area's size changes keep the turn going", act.contains("geckoView.sizeListener = { turnSettler.sizeChanged() }"))
        assertTrue("Diagnostics shows the numbers", act.contains("out.add(turnSettler.line())"))
        val settler = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/TurnSettler.kt")
        val machine = source("core/src/main/kotlin/app/kestrel/core/media/TurnSettleMachine.kt")
        assertTrue("the video is paused through the engine's media session", settler.contains("ms.pause()") && machine.contains("TurnSettle.pauseIt("))
        assertTrue("it is played again only by the rule: we paused it, nobody touched, the same tab is in front, the app is on screen", machine.contains("TurnSettle.resumeIt(pausedByUs = true"))
        assertTrue("the cover is black and fades out", settler.contains("TurnSettle.FADE_MS"))
        assertTrue("a tab without a live session has nothing that plays", settler.contains("it.session != null && it.gecko.videoPlaying"))
        assertTrue("the activity going away lets the turn and its timers go", body(act, "override fun onDestroy() {").let { it.contains("turnSettler.abandon()") && it.contains("removeCallbacks(strayCheck)") && it.contains("removeCallbacks(barsCheck)") })
        assertTrue("a closed or replaced session forgets its media state", source("geckoapp/src/main/kotlin/app/kestrel/browser/web/GeckoTab.kt").contains("g.videoPlaying = false; g.mediaSession = null"))
        val delegate = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/TabDelegates.kt")
        assertTrue("the delegate keeps the media session of the tab", delegate.contains("tab.gecko.mediaSession = mediaSession") && delegate.contains("tab.gecko.mediaSession = null"))
    }

    // FB1, owner message 47: "The controls are still under Android navigation bar. There are no controls in full screen ... Make the controls available for both orientations and never underneath the Android navigation bar".
    // alpha48 - alpha50 asked the system to hide its bars and nothing changed on the tablet (its window manager laid the window out over the whole screen for the request while its System UI kept drawing the bars):
    // by default the app does not ask, the page stays between the bars.
    @Test fun aFullScreenPageKeepsTheSystemBarsAndStaysBetweenThemUnlessThePersonChoseHide() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val immersive = body(act, "private fun applyImmersive(on: Boolean) {")
        assertTrue("the bars are asked to go only for the choice Hide", immersive.contains("val hide = on && prefs.fullscreenBars == 1"))
        assertTrue("both API paths follow it", immersive.contains("if (hide) { c.hide(WindowInsets.Type.systemBars())") && immersive.contains("= if (hide) (dv.systemUiVisibility and older.inv()) or immersive else"))
        assertTrue("the default is Keep", source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("""sp.getInt("fs_bars", 0).coerceIn(0, 1)"""))
        assertTrue("Settings has the row", source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("""choice("System bars in full-screen video""""))
        assertTrue("the lab can play both choices", act.contains("\"bars\" -> { val arg"))
        assertTrue("Diagnostics says which one is on", body(act, "internal fun fullScreenChanged(tab: Tab, on: Boolean) {").contains("Settings > System bars in full-screen video is on Keep"))
    }

    // FB2 / FB3, owner message 47: "The video didn't pause when switching orientation ... for a few seconds the video is the wrong size ... Make it pause the video and properly set up the size and orientation smoothly then resume."
    // The engine opens a media session only for audible media, so the app's own pause (alpha49) never touched the muted Yahoo video: the PAGE pauses every video (turn.js, every frame).
    @Test fun theVideosOfAPageArePausedForATurnWhetherOrNotTheyMakeSound() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val manifest = source(dir + "manifest.json")
        assertTrue("turn.js runs in every frame after vfind.js", manifest.contains("\"turn.js\"") && manifest.lastIndexOf("\"vfind.js\"", manifest.indexOf("\"turn.js\"")) in 0 until manifest.lastIndexOf("\"videobar.js\"", manifest.indexOf("\"turn.js\"")) && manifest.lastIndexOf("\"videobar.js\"", manifest.indexOf("\"turn.js\"")) < manifest.indexOf("\"turn.js\""))
        val turn = source(dir + "turn.js")
        assertTrue("every video, also inside shadow roots", turn.contains("VF.all('video')"))
        assertTrue("paused when the shape of the window flips, not for a keyboard", turn.contains("if (now === landscape) return;") && turn.contains("vids[i].pause()"))
        assertTrue("one more resize once it is still, then play what we paused", turn.contains("window.dispatchEvent(new Event('resize'))") && turn.contains("var p = v.play();"))
        assertTrue("a touch is the person taking over", turn.contains("e.isTrusted) state.touched = true"))
        assertTrue("never started when it was not paused by us / the page started it itself", turn.contains("s.touched || !v.isConnected || !v.paused || v.ended"))
        assertTrue("the numbers reach the app as vbk 'turn'", source(dir + "background.js").contains("m.e === 'turn'") && source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt").contains("\"turn\" -> { Health.count(\"turn.page.turns\")"))
    }

    // FB4: the engine sizes a page that goes full screen to the display, not to the view (lab run 43760546128): the page tells the viewport it got, the app corrects it.
    @Test fun aFullScreenPageThatGotTheWrongViewportIsGivenARealSizeChange() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val turn = source(dir + "turn.js")
        assertTrue("the top frame tells its viewport in device pixels, only in full screen", turn.contains("k: 'vpt'") && turn.contains("if (!document.fullscreenElement) return;") && turn.contains("window.innerHeight * dpr"))
        assertTrue("and relayed by the background page with clamped numbers", source(dir + "background.js").contains("m.k !== 'vpt'") && source(dir + "background.js").contains("sender.frameId !== 0"))
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val fn = body(act, "internal fun onPageViewport(m: org.json.JSONObject) {")
        assertTrue(fn, fn.contains("tabOfReport(m.optString(\"p\")) ?: return") && fn.contains("fullscreenTab !== tab") && fn.contains("vpTries >= 3") && fn.contains("resyncEngineSize()"))
        assertTrue("the real size change is the view one pixel shorter, then back", body(act, "private fun resyncEngineSize() {").contains("lp.bottomMargin = 1") && act.contains("it.bottomMargin = 0"))
        assertTrue("a new full screen starts clean and the activity going away cancels it", body(act, "internal fun fullScreenChanged(tab: Tab, on: Boolean) {").contains("vpUndo.run()") && body(act, "override fun onDestroy() {").contains("removeCallbacks(vpUndo)"))
        assertTrue("Diagnostics says it", act.contains("out.add(viewportLine())"))
        assertTrue("the bridge passes it on", source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt").contains("\"vpt\" -> { viewport(m); \"\" }"))
    }

    // VP1, owner message 49: "each time I tap the screen it should toggle playback controls shown or hidden ... controls easily accessible". Kestrel's own full-screen controls: the pieces that must stay connected.
    @Test fun kestrelsOwnFullScreenControlsAreConnectedFromThePageToTheScreen() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val vp = source(dir + "player.js")
        assertTrue("taps on the picture are told to the app and kept from the page, the page's own buttons (skip ad, captions) are not", vp.contains("e.stopImmediatePropagation()") && vp.contains("send({ k: 'vtap'") && vp.contains("return CTRL.test(nameOf(el)) ? 'film' : 'page'") && !vp.contains("|skip|"))
        assertTrue("it only runs when the app says so and only while a film is full screen", vp.contains("r.kp === true") && vp.contains("document.addEventListener('fullscreenchange', onFullscreen, true)"))
        assertTrue("the manifest loads it after vfind.js", source(dir + "manifest.json").let { it.indexOf("\"player.js\"") > it.lastIndexOf("\"vfind.js\"") })
        val bg = source(dir + "background.js")
        assertTrue("the background relays the film's state and the taps and answers with the app's JSON, and the guard answer carries kp", bg.contains("m.k !== 'vst' && m.k !== 'vtap'") && bg.contains("kp: (f & 512) !== 0") && bg.contains("JSON.parse(r)"))
        val bridge = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt")
        assertTrue("flag 512 follows the Settings row and the messages reach the player", bridge.contains("prefs().fsControls == 0) 512") && bridge.contains("\"vst\", \"vtap\" -> player(m)"))
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        assertTrue("full screen arms the controller; the report of a film begins it; the cover hides it", act.contains("player.fullscreen(on, prefs.fsControls == 0)") && act.contains("player.turnStarted()") && act.contains("fsTurner.ownedByPlayer = { player.active }"))
        assertTrue("only the page that IS full screen drives the controls", act.contains("tabOfReport(m.optString(\"p\")) === fullscreenTab"))
        val ctl = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/PlayerController.kt")
        assertTrue("nothing is drawn until a film reports, and a page that stops reporting takes the controls away", ctl.contains("if (m.optString(\"k\") != \"vst\") return; begin()") && ctl.contains("PlayerRules.pageGone("))
        // lab run 43776782679 (second press on the big button asked "pause" again): the relay put the page ADDRESS into `p` over the film's paused flag, so the app read every film as "not paused". The paused flag is `pa`
        // in everything the app gets, `p` is the address, and the number of the last carried-out command is `ak` all the way to the model.
        assertTrue("the relay names the paused flag pa, keeps ak, and leaves p for the address", bg.contains("pa: m.p === 1 ? 1 : 0") && bg.contains("ak: n(m.ak, 0, 2000000000)") && bg.contains("o.p = String(page).slice(0, 300)"))
        assertTrue("the controller reads pa (never p: that is the address) and ak", ctl.contains("m.optInt(\"pa\") == 1") && ctl.contains("m.optInt(\"ak\", 0)") && !ctl.contains("optInt(\"p\")"))
        assertTrue("the page script sends ak and numbers the commands it carried out", vp.contains("ak: S ? S.ack | 0 : 0") && vp.contains("if (c.i > s.ack) s.ack = c.i | 0;"))
        assertTrue("the app tells the page whether the controls own the film (a), and a page that is refused four times in a row gives its bar and its taps back", ctl.contains(",\\\"a\\\":") && vp.contains("++s.refused >= C.giveUp"))
        val vb = source(dir + "videobar.js")
        assertTrue("the injected bar draws nothing while Kestrel's controls own the film", vb.contains("if (kpOwns()) { if (bar) hide(); return; }") && vb.contains("if (kpOwns()) return;"))
        assertTrue("the pref defaults to Kestrel's", source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("sp.getInt(\"fs_controls\", 0)"))
    }

    // ROT3 (found in lab run 43763822027, 2026-11-15): handing a one-time turn back to the tablet the instant the raw sensor shows the new position made the screen turn to the position Android still believed in
    // (a portrait flash, then landscape 0.45 s later: two engine re-layouts for one turn of the owner's hand). The hand-back waits until the tablet has stayed that way for a moment.
    @Test fun aOneTimeTurnIsHandedBackOnlyAfterTheTabletHasStayedInItsPositionForAMoment() {
        val f = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/FullscreenTurner.kt")
        val moved = body(f, "private fun moved(degrees: Int) {")
        assertTrue(moved, moved.contains("Io.main.postDelayed(releaseNow, RELEASE_SETTLE_MS)") && moved.contains("else cancelRelease()") && !moved.contains("apply(base()"))
        val rel = body(f, "private fun releaseIfStillHeld() {")
        assertTrue(rel, rel.contains("FullscreenTurn.release(requested, posture)") && rel.contains("apply(base(), false,"))
        assertTrue("leaving full screen and the app going away cancel it", body(f, "fun exit() {").contains("cancelRelease()") && f.contains("fun paused() { cancelRelease(); stopSensor() }"))
        assertTrue("about half a second, long enough for Android's own rotation sensing", f.contains("const val RELEASE_SETTLE_MS = 650L"))
    }

    // ROT2, owner message 48: two screenshots of a page still laid out for the OLD orientation a few seconds after a turn. The cover and the pause of a turn wait until the full-screen page says it HAS the view's new size.
    @Test fun aTurnUnderAFullScreenPageWaitsForThePageToHaveTheNewSize() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val fn = body(act, "internal fun onPageViewport(m: org.json.JSONObject) {")
        assertTrue(fn, fn.contains("if (matches) tab.gecko.vpOk = true") && fn.contains("turnSettler.pageReported(matches)") && fn.contains("if (turnSettler.turning) return"))
        assertTrue("a page can confirm a turn when it is full screen, reports and its viewport is the view's", act.contains("{ !labTurnWaitOff && fullscreenOn && fullscreenTab?.gecko?.vpOk == true }"))
        assertTrue("the nudge the turn asks for is the real size change of FB4, and the ring is a view of the cover", act.contains("{ resyncEngineSize() }, { turnHint }") && act.contains("turnCover = cover; turnHint = ring"))
        val machine = source("core/src/main/kotlin/app/kestrel/core/media/TurnSettleMachine.kt")
        assertTrue(machine.contains("TurnSettle.settled(n - lastChangeAt, n - startedAt, expects, confirmed)") && machine.contains("fun pageReported(matchesView: Boolean)"))
        assertTrue("a size change takes a confirmation back", machine.contains("fun sizeChanged() { if (active) { lastChangeAt = h.now(); confirmed = false } }"))
        val turn = source("geckoapp/src/main/assets/kestrel-shields/turn.js")
        assertTrue("the top frame says its viewport once soon after the page is up", turn.contains("if (TOP) setTimeout(function () { try { sendViewport(document.fullscreenElement ? 1 : 0); }"))
        assertTrue("a new session forgets that the page reports", source("geckoapp/src/main/kotlin/app/kestrel/browser/web/GeckoTab.kt").contains("g.vpOk = false"))
        val pick = body(act, "private fun tabOfReport(pageUrl: String): Tab? {")
        assertTrue("two tabs with the same address: the full-screen tab, then the tab in front, before the first one in the list", pick.indexOf("fullscreenTab?.takeIf { same(it) }") in 0 until pick.indexOf("current?.takeIf { same(it) }") && pick.indexOf("current?.takeIf { same(it) }") < pick.indexOf("tabs.firstOrNull { same(it) }"))
        assertTrue("a single-page site (the tab's address follows pushState, the page's is the one it loaded with): the same host", pick.contains("fun sameHost(t: Tab?)") && pick.indexOf("tabs.firstOrNull { same(it) }") < pick.indexOf("sameHost(it)") && pick.contains(".take(300)"))
        assertTrue("a new page has not told its size yet", act.contains("tab.gecko.vpOk = false } // (a new page has not told its size yet"))
    }

    // SND1, owner message 45: "Sound should not keep playing if I am not currently on the Kestrel tab." The engine suspends the media of an inactive session (proven in the emulator lab); the app adds a safety net.
    @Test fun aVideoThatIsNotShownDoesNotGoOnMakingSound() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        assertTrue("every session is created with suspendMediaWhenInactive", act.contains(".suspendMediaWhenInactive(true)"))
        assertTrue("the app leaving the screen sets the page inactive and starts the safety net", body(act, "override fun onStop() {").let { it.contains("setActive(false)") && it.contains("silenceStraysSoon()") })
        assertTrue("a tab switch starts the safety net", body(act, "private fun show(tab: Tab) {").contains("silenceStraysSoon()"))
        assertTrue("the Home screen over a playing page pauses it at once", body(act, "override fun onHome() {").contains("silenceStrays()"))
        val net = body(act, "private fun silenceStrays() {")
        assertTrue(net, net.contains("ms.pause()") && net.contains("t.gecko.videoPlaying") && net.contains("!homeShown(t)"))
        assertTrue("coming back to the app cancels the check", body(act, "override fun onStart() {").contains("removeCallbacks(strayCheck)"))
        assertTrue("Diagnostics shows the numbers", act.contains("out.add(strayLine())"))
    }

    // the second independent review of the video bar (research/REVIEW_ALPHA48.md addendum)
    @Test fun theVideoBarScriptsKeepTheirReviewFixes() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val bar = source(dir + "videobar.js")
        val vfind = source(dir + "vfind.js")
        val content = source(dir + "content.js")
        val bg = source(dir + "background.js")
        assertFalse("RW7: a page cannot name a layer 'the bar'", bar.contains("data-kestrel-bar") || content.contains("data-kestrel-bar"))
        assertTrue("RW7: the bar is known by identity", bar.contains("api.isBar = function (el) { return !!bar && el === bar.host; }") && content.contains("window.KestrelVB.isBar(e.target)"))
        assertTrue("RW7: a script's own pointer events are nobody's touch", bar.contains("window.addEventListener('pointerdown', function (e) {\n      if (!e.isTrusted) return;"))
        assertTrue("RW5: a tap is shorter than the engine's long press", bar.contains("tap: 450"))
        assertTrue("RW1: a video that is not in the stack of elements under the finger counts only under a bare layer", vfind.contains("if (els.length && bare(els[0]))"))
        assertTrue("RW6: two frames of one size are ambiguous", vfind.contains("if (mine.length !== 1) return null;"))
        assertTrue("RW2: a full-screen web component's host gets the bar inside its shadow tree", bar.contains("return VF.rootOf(fe) || fe;"))
        assertTrue("RW4: the frame sends the video menu itself and only while the panel was not told", content.contains("if (told) return;") && bg.contains("r.src.length <= 2000"))
        assertTrue("RW11: an answer that did not come is not kept", bar.contains("done({ on: false, always: false }, false)"))
    }

    /** ANS51 (owner message 51: the real tablet's Diagnostics of alpha55): what was changed because of those numbers stays changed. */
    @Test fun theNumbersOfTheTabletsDiagnosticsAreActedOn() {
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        // b: the entry said "video 0x0" for a film in a shadow root, so automatic turning did nothing: the player's own report tells the picture
        val msg = body(act, "private fun playerMessage(m: org.json.JSONObject): String {")
        assertTrue(msg, msg.contains("player.onMessage(m); filmPicture(m)"))
        val fp = body(act, "private fun filmPicture(m: org.json.JSONObject) {")
        assertTrue(fp, fp.contains("t.gecko.pictureW = w; t.gecko.pictureH = h; t.gecko.picturePlayed = true") && fp.contains("fullscreenPictureKnown()") && fp.contains("m.optString(\"k\") != \"vst\""))
        // c: a silent page is counted and its late first word is measured (the line said "0 ms, 0 turns measured")
        val machine = source("core/src/main/kotlin/app/kestrel/core/media/TurnSettleMachine.kt")
        assertTrue(machine.contains("if (!active) { lateWord(); return }") && machine.contains("silent++; lateFrom = startedAt") && machine.contains("silent pages: "))
        assertTrue("the silent limit is 6 s (owner message 52: a stale layout after 3 s) and the nudge waits 2.5 s", source("core/src/main/kotlin/app/kestrel/core/media/TurnSettle.kt").let { it.contains("const val SILENT_MS = 6000L") && it.contains("const val NUDGE_AFTER_MS = 2500L") })
        assertTrue("only a page that SPOKE with a wrong size is nudged (ANS52)", machine.contains("if (expects && heard > 0 && !confirmed && !nudged && TurnSettle.nudgeDue(n - lastChangeAt))"))
        // d: a page that is only busy does not end the session (it did three times in 160 s and every restart popped the controls up)
        val pc = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/PlayerController.kt")
        assertTrue(pc.contains("PlayerRules.pageGone(SystemClock.uptimeMillis() - lastReportAt)") && !pc.contains("SILENT_END_MS"))
        // e: stalls, the script's own cost and the film's frames travel from the page to Diagnostics and to "Real video"
        val vp = source(dir + "player.js")
        assertTrue(vp.contains("m.lt = Math.round(s.late); s.late = 0;") && vp.contains("m.pc = Math.round(mono() - t0);") && vp.contains("function mono() { try { return performance.now();") && vp.contains("if (document.hidden) return;") && vp.contains("tf: tf, df: df") && vp.contains("getVideoPlaybackQuality"))
        assertTrue(source(dir + "background.js").contains("lt: n(m.lt, 0, 60000), pc: n(m.pc, 0, 60000), tf: n(m.tf, 0, 2000000000), df: n(m.df, 0, 2000000000), sh: m.sh === 1 ? 1 : 0"))
        assertTrue(pc.contains("health.onReport(m.optInt(\"lt\"), m.optInt(\"pc\"), m.optInt(\"tf\", -1), m.optInt(\"df\", -1)") && pc.contains("${'$'}{health.line()}") && pc.contains("if (quiet) return"))
        assertTrue("the frames reach the Real video line", act.contains("player.onLook = { page, frames, dropped, seconds, w, h -> filmLook(page, frames, dropped, seconds, w, h) }") && act.contains("videoQuality.add(host, frames, dropped, 0, seconds, w, h"))
        // ANS52a: the engine's view is held at its old size for the first frame of a turn that gets the cover, and released right after that frame (the blocking resize then happens behind the cover)
        assertTrue(body(act, "override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {").contains("if (turnSettler.turning) holdGeckoView()"))
        val hold = body(act, "private fun holdGeckoView() {")
        assertTrue(hold, hold.contains("lp.width = w; lp.height = h; geckoView.layoutParams = lp") && hold.contains("addOnDrawListener(l)") && hold.contains("turnSettler.firstFrame()") && hold.contains("TurnSettle.HOLD_MAX_MS"))
        val rel = body(act, "private fun releaseGeckoHold(why: String) {")
        assertTrue(rel, rel.contains("lp.width = FrameLayout.LayoutParams.MATCH_PARENT; lp.height = FrameLayout.LayoutParams.MATCH_PARENT") && rel.contains("addOnGlobalLayoutListener"))
        assertTrue("the cover stays while the view waits for its size", act.contains("{ turnHolding || turnResizeAwaited }"))
        assertTrue("a leave of the app and the closing give the view its size back", act.contains("releaseGeckoHold(\"the app was left\")") && act.contains("releaseGeckoHold(\"closing\")"))
        assertTrue("the 1 px resync does not add a second size change while the view is held", body(act, "private fun resyncEngineSize() {").contains("if (turnHolding) return"))
        assertTrue(source("core/src/main/kotlin/app/kestrel/core/media/TurnSettleMachine.kt").contains("if (h.resizePending()) lastChangeAt = n"))
        // review of alpha56: a second film on the same page turns too (the first report of EVERY full screen), a private tab's film size is never written down, only a film in a shadow tree adds to Real video, the page gives up on an app that does not answer only after SECONDS of it
        assertTrue(act.contains("val first = !filmPictureTold") && act.contains("if (on) { filmPictureTold = false; fsTurner.enter();"))
        assertTrue(source("geckoapp/src/main/kotlin/app/kestrel/browser/web/FullscreenTurner.kt").let { it.contains("private val isPrivate: () -> Boolean") && it.contains("video ${'$'}{shape(w, h)}") && it.contains("became known (${'$'}{shape(w, h)})") } && act.contains("{ current?.isPrivateTab == true })"))
        assertTrue(pc.contains("if (lookShadow) onLook(") && pc.contains("lookShadow = m.optInt(\"sh\") == 1"))
        assertTrue(source("core/src/main/kotlin/app/kestrel/core/player/FilmHealth.kt").contains("if (df > MAX_FPS * maxOf(1L, dt / 1000L) + 60L) return"))
        assertTrue(vp.contains("mono() - s.refusedAt >= C.giveUpMs") && vp.contains("giveUpMs: 8000"))
        // g: no page look is read back while a page is full screen (the tablet's log: two 'look-saved' during a film; the read-back competes with the decoder and saves the film's frame as the page's look)
        assertTrue(body(act, "private fun captureLook(tab: Tab?, why: String, minGapMs: Long = LastLookRules.MIN_GAP_MS.toLong()) {").contains("if (fullscreenOn) return"))
        // f: how long the app's own part and the engine's part of a turn take is written down
        assertTrue(source("geckoapp/src/main/kotlin/app/kestrel/browser/web/KestrelGeckoView.kt").let { it.contains("override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int)") && it.contains("the engine view's own layout") } && act.contains("the app's own handling took"))
    }

    /** ANS52i (owner message 52): Kestrel's own player is offered only for what it can play, never touches DRM or a stream without an address, keeps its memory small and its sound off when the screen is left. */
    @Test fun kestrelsOwnPlayerIsOfferedOnlyForWhatItCanPlayAndStaysSmall() {
        val flow = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/VideoFlow.kt")
        val target = body(flow, "private fun nativeTarget(choice: VideoPanel.Choice): NativeTarget? {")
        assertTrue(target, target.contains("t.kind != MediaKind.FILE || t.audioOnly") && target.contains("c.kind != MediaKind.HLS || t.audioOnly") && !target.contains("MediaKind.DASH"))
        assertTrue("the sheet's second button comes only from the build that can play", flow.contains("override fun canPlay(choice: VideoPanel.Choice): Boolean = nativeTarget(choice) != null"))
        assertTrue(source("app/src/main/kotlin/app/kestrel/browser/ui/VideoPanel.kt").contains("fun canPlay(choice: Choice): Boolean = false"))
        val player = source("geckoapp/src/main/kotlin/app/kestrel/browser/NativePlayerActivity.kt")
        assertTrue("hardware decoders first, no software extension decoders", player.contains("it.hardwareAccelerated && !it.softwareOnly") && player.contains("EXTENSION_RENDERER_MODE_OFF"))
        assertTrue("small buffers: 15 to 30 s, 16 MiB at most (the app heap class is 128 MB)", player.contains("setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)") && player.contains("setTargetBufferBytes(16 * 1024 * 1024)"))
        assertTrue("sound off when the screen is left, audio focus and headphones handled", player.contains("override fun onPause() {") && player.contains("player?.pause()") && player.contains("AudioAttributes.DEFAULT, true") && player.contains("setHandleAudioBecomingNoisy(true)"))
        assertTrue("a private tab's film is not counted or logged", player.contains("private fun say(event: String, text: String) { if (!priv) Health.log") && player.contains("if (!priv) { NativePlayerStats.plays++"))
        val manifest = source("geckoapp/src/main/AndroidManifest.xml")
        assertTrue("its own window, not exported, handles the turn itself", manifest.contains("android:name=\".NativePlayerActivity\"") && manifest.substringAfter("android:name=\".NativePlayerActivity\"").substringBefore("/>").let { it.contains("android:exported=\"false\"") && it.contains("orientation|screenSize") })
        assertTrue("Media3 is the version the engine carries", source("geckoapp/build.gradle.kts").contains("val media3Version = \"1.11.1\""))
    }

    /** ANS52g (owner message 52, "optimized for the hardware"): the engine's preferences cannot hide VP9 on the tablet, so the PAGE answers "no" for what the settings and the chips rule out; a policy that is not known is never applied. */
    @Test fun theVideoFormatPolicyIsEnforcedInThePageAndFollowsTheSettings() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val manifest = source(dir + "manifest.json")
        assertTrue("declared (not registered at run time) in the page's own world, every frame, from the first script", manifest.substringAfter("\"codec-main.js\"").substringBefore("}").let { it.contains("\"world\": \"MAIN\"") && it.contains("\"all_frames\": true") && it.contains("\"run_at\": \"document_start\"") })
        val js = source(dir + "codec-main.js")
        assertTrue("the three questions a player asks are answered by the policy", js.contains("replace(MediaSource, 'isTypeSupported'") && js.contains("replace(HTMLMediaElement.prototype, 'canPlayType'") && js.contains("replace(MediaCapabilities.prototype, 'decodingInfo'"))
        assertTrue("a policy that is not known is never applied, and a question without a size is never refused for its size", js.contains("typeof t === 'string' && policy") && js.contains("if (!(w > 0 && h > 0)) return false;"))
        assertTrue("the isolated world's copy does nothing", js.contains("typeof browser !== 'undefined' && browser && browser.runtime"))
        val content = source(dir + "content.js")
        assertTrue("the content script hands over what the app said and relays only checked numbers, ten at most", content.contains("typeof r.cx === 'string'") && content.contains("cReports >= 10") && content.contains("p.length !== 5"))
        val bg = source(dir + "background.js")
        assertTrue("the policy rides in the fifth field of the page answer and comes back as cx; an older app keeps the last one", bg.contains("if (parts.length > 4) codecCfg =") && bg.contains("cx: codecCfg") && bg.contains("vx: vcxText()"))
        val bridge = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt")
        assertTrue("every page answer carries the policy", bridge.contains("\":\" + guard + \":\" + videoPolicy()"))
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        assertTrue("the activity builds it from the two Settings rows and the device's limits, and Diagnostics shows it", act.contains("videoPolicy = { app.kestrel.browser.web.VideoFormats.policy(applicationContext, prefs) }") && act.contains("app.kestrel.browser.web.VideoFormats.line(applicationContext, prefs)"))
        assertTrue("the limits come from the hardware decoders only, and the probe also runs when only the limits are missing", source("geckoapp/src/main/kotlin/app/kestrel/browser/DeviceVideo.kt").contains("it.mime == mime && it.hardware") && source("geckoapp/src/main/kotlin/app/kestrel/browser/GeckoEngine.kt").contains("DeviceVideo.hardware(ctx) == null || DeviceVideo.limits(ctx) == null"))
        val settings = source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt")
        assertTrue("both rows exist: the formats row (Automatic by default) and the 30 frames switch (off by default)", settings.contains("choice(\"Video formats offered to sites\"") && settings.contains("toggle(\"At most 30 frames a second\"") && source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("bool(\"vid_fps30\", false)"))
    }

    /** ANS52h (owner message 52): the Yahoo film's page picked the 1920 x 1080 rung on a 973 x 600 screen. The extension takes the rungs above a cap out of HLS MASTER playlists and nothing else, and a doubt is never the page's problem. */
    @Test fun thePictureSizeCapOnlyTouchesMasterPlaylistsAndNeverBreaksThePage() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        val bg = source(dir + "background.js")
        assertTrue("narrow and blocking: only addresses that name a .m3u8 are held, only a 200 GET from a tab, only while the app's cap is on", bg.contains("urls: ['http://*/*.m3u8*', 'https://*/*.m3u8*']") && bg.contains("}, ['blocking', 'responseHeaders']);") && bg.contains("if (!(hlsCap > 0) || d.statusCode !== 200 || d.method !== 'GET' || d.tabId < 0) return {};"))
        assertTrue("any doubt hands the bytes over as they came: not text, too big, no master list, a stream error, a throw", bg.contains("{ fatal: true }") && bg.contains("hls.notText++; hlsWrite(f, chunks); f.close(); return;") && bg.contains("var HLS_MAX_BYTES = 524288;") && bg.contains("f.onerror = function () { hls.err++; try { f.disconnect(); }"))
        assertTrue("the cap is the sixth field of the page answer, and nonsense means off", bg.contains("if (parts.length > 5) { var hc = parseInt(parts[5], 10); hlsCap = (hc >= 320 && hc <= 20000) ? hc : 0; }"))
        val manifest = source(dir + "manifest.json")
        assertTrue("the pure rewrite is loaded before the background page that uses it", manifest.indexOf("\"hlscap.js\"") in 1 until manifest.indexOf("\"background.js\""))
        val cap = source(dir + "hlscap.js")
        assertTrue("only a master playlist, the smallest rungs always stay, a rung without a size stays", cap.contains("text.indexOf(STREAM_INF) < 0) return null") && cap.contains("r.size > limit && r.size > floor") && cap.contains("return (a > 0 && b > 0) ? Math.max(a, b) : 0;"))
        assertTrue("every page answer carries the cap after the video policy", source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt").contains("videoPolicy() + \":\" + pictureCap()"))
        assertTrue("the activity tells the extension what the setting and the screen make of it", source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt").let { it.contains("pictureCap = { app.kestrel.browser.web.VideoFormats.cap(applicationContext, prefs) }") && it.contains("VideoFormats.capLine(applicationContext, prefs)") })
        assertTrue("the setting is Automatic by default and has its row", source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("sp.getInt(\"video_cap\", 0).coerceIn(0, 4)") && source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("choice(\"Picture size from sites\""))
    }

    /** ANS53a-f (owner message 53, on alpha56): nothing starts or loads before a press, the controls are instant, the download button follows the page in front, Kestrel's own player follows the tablet and lists its pictures. */
    @Test fun ownerMessage53IsActedOn() {
        val dir = "geckoapp/src/main/assets/kestrel-shields/"
        // a, b: the gate
        val manifest = source(dir + "manifest.json")
        assertTrue("declared in the page's own world, every frame, from the first script", manifest.substringAfter("\"gate-main.js\"").substringBefore("}").let { it.contains("\"world\": \"MAIN\"") && it.contains("\"all_frames\": true") && it.contains("\"run_at\": \"document_start\"") })
        val gate = source(dir + "gate-main.js")
        assertTrue("a MediaSource source is held, play() without an activation is refused like the engine would, preload is none, autoplay is not applied", gate.contains("isBlob(v)") && gate.contains("NotAllowedError") && gate.contains("s.call(this, 'none')") && gate.contains("openEl(this, 'play')"))
        assertTrue("a real tap on the picture and Kestrel's own controls open an element; a stream is never held", gate.contains("pressAt(e.clientX, e.clientY)") && gate.contains("OPEN_EVENT") && gate.contains("rec(this).open = true"))
        assertTrue("closed until the content script says the rule is off", gate.contains("var on = true;") && gate.contains("d === '0'"))
        for (f in listOf("content.js", "videobar.js", "player.js")) assertTrue("$f tells the gate before its own play()", source(dir + f).contains("gate("))
        assertTrue("the content script hands over the rule (gt) and relays seven checked numbers", source(dir + "content.js").let { it.contains("r.gt === false") && it.contains("p.length !== 7") && it.contains("k: 'vgt'") })
        assertTrue("the background page reads flag 1024 and counts what was held", source(dir + "background.js").let { it.contains("gt: (f & 1024) !== 0") && it.contains("vg: vgtText()") })
        val bridge = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/ShieldsBridge.kt")
        assertTrue("flag 1024 follows the Settings switch only", bridge.contains("(if (prefs().videoPressToPlay) 1024 else 0)"))
        assertTrue("the engine is told: autoplay 5, preload none", source("geckoapp/src/main/kotlin/app/kestrel/browser/GeckoEngine.kt").let { it.contains("triInt(\"media.autoplay.default\", if (prefs.videoPressToPlay) 5 else null)") && it.contains("triInt(\"media.preload.default\"") && it.contains("triInt(\"media.preload.auto\"") })
        assertTrue("a muted start is refused too while the rule is on", source("geckoapp/src/main/kotlin/app/kestrel/browser/web/TabDelegates.kt").contains("if (!a.prefs.videoPressToPlay && a.prefs.videoMutedAutoplay) PermissionDelegate.ContentPermission.VALUE_ALLOW else PermissionDelegate.ContentPermission.VALUE_DENY"))
        assertTrue("on by default, with its switch", source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("bool(\"vid_press_play\", true)") && source("app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt").contains("toggle(\"Videos start only when I press play\""))
        // c: instant controls
        val tap = source("core/src/main/kotlin/app/kestrel/core/player/TapMachine.kt")
        assertTrue("a tap toggles at once: no timer in the tap machine", !tap.contains("schedule") && tap.contains("        h.single()\n"))
        assertTrue("and without a fade", source("core/src/main/kotlin/app/kestrel/core/player/ControlsMachine.kt").contains("if (shown) hide(false) else show(hideAfterMs, false)"))
        val act = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")
        assertTrue("the activity answers a tap on the glass itself", act.contains("player.appTap((downX * 1000f / window.decorView.width.coerceAtLeast(1)).toInt()"))
        val ctl = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/PlayerController.kt")
        assertTrue("the page's copy of that tap is matched by the time of the touch and dropped, a tap on a page button is put back", ctl.contains("matchesAppTap(ts)") && ctl.contains("model.undoTap(prevShown)"))
        assertTrue("the page script names the time of the touch and the taps on its own buttons", source(dir + "player.js").let { it.contains("ts: e ? stamp(e) : 0") && it.contains("pg: 1") })
        // e: the download button
        assertTrue("the button, the sheet and the menu entry ignore a page under the Home screen", body(act, "internal fun refreshDownloadButton() {").contains("homeShown(t)") && body(act, "override fun onDownload() {").contains("homeShown(t)") && source("geckoapp/src/main/kotlin/app/kestrel/browser/web/VideoFlow.kt").contains("!a.onHomeScreen(t)"))
        // f, d: Kestrel's own player
        val np = source("geckoapp/src/main/kotlin/app/kestrel/browser/NativePlayerActivity.kt")
        assertTrue("it follows the tablet (no forced landscape), has the rotate button and a surface that is a point until the shape is known", np.contains("FullscreenTurner(this, { FullscreenTurn.MODE_FOLLOW }") && !np.contains("SENSOR_LANDSCAPE") && np.contains("FrameLayout.LayoutParams(1, 1, Gravity.CENTER)") && np.contains("showRotate = true") && np.contains("override fun onRotate() { turner.rotatePressed()"))
        assertTrue("it lists every picture of the stream and carries the pick out as a track override, remembered", np.contains("TrackSelectionOverride(") && np.contains("Prefs.get(applicationContext).nativeMaxH = cls") && np.contains("controls.showQuality = distinct >= 2"))
        // the independent review of the gate (research/ANS53_GATE_REVIEW.md)
        assertTrue("only VIDEO is gated (a music player's next track must play), only a MediaSource's blob: address is held, a stream gets its autoplay back", gate.contains("x.nodeName === 'VIDEO'") && !gate.contains("'AUDIO'") && gate.contains("defMethod(URL, 'createObjectURL'") && gate.contains("openEl(this, 'stream')"))
        assertTrue("a tap opens the video it landed on or in, a tap on the player counts 30 s, a gesture 1.2 s although the activation was used up, a play() before the app's answer waits for it", gate.contains("sameBox(el, target)") && gate.contains("pressedRecently(this)") && gate.contains("lastGesture < 1200") && gate.contains("else if (!told)"))
        assertTrue("an app that did not answer leaves the gate closed", source(dir + "background.js").contains("gt: !told || (f & 1024) !== 0"))
        // lab run 43812820865: the rule OFF must give the page's own autoplay back; the lab drives the player screen through a DEBUG-only exported activity, the release stays closed
        assertTrue("the rule off gives the page's autoplay back (markup and script)", gate.contains("why === 'off' && wantAuto") && gate.contains("dAutoplay.set.call(el, true)"))
        val mainManifest = source("geckoapp/src/main/AndroidManifest.xml")
        val npDecl = mainManifest.substring(mainManifest.indexOf("android:name=\".NativePlayerActivity\""), mainManifest.indexOf("android:name=\".NativePlayerActivity\"") + 500)
        assertTrue("the release manifest keeps the player screen closed to other apps", npDecl.contains("android:exported=\"false\""))
        assertTrue("only the DEBUG variant opens it (the lab's am start)", source("geckoapp/src/debug/AndroidManifest.xml").let { it.contains("app.kestrel.browser.NativePlayerActivity") && it.contains("android:exported=\"true\"") })
        // d.2: the page's own full-screen controls get the list too (a hand-over: the page's player cannot be told to change its picture)
        val flow = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/VideoFlow.kt")
        assertTrue("the pictures come from the film's master playlist and a pick starts Kestrel's own player at the page's second with that picture", flow.contains("PageQuality.rungs(master.variants)") && flow.contains("fun pageFilm(tab: Tab): PageFilm?") && flow.contains("NativePlayerActivity.launch(a, c.url, headers, startMs, 720, tab.isPrivateTab, tab.gecko.pictureW, tab.gecko.pictureH, rung, rung == null)"))
        assertTrue("the controls draw the button only while the film offers a choice, and tell the activity where the film is", ctl.contains("view.showQuality = qualityAvailable()") && ctl.contains("override fun onQuality()") && ctl.contains("qualityPressed(v.timeS, v.lengthS)"))
        assertTrue("the activity opens the list over the controls, closes it first on Back, and a tap on the sheet is not a tap on the picture", act.contains("qualitySheet.show(\"Quality\"") && act.contains("if (::qualitySheet.isInitialized && qualitySheet.isOpen) { qualitySheet.dismiss(); return true }") && ctl.contains("fun controlAt(wx: Float, wy: Float): Boolean = sheetOpen() ||"))
        assertTrue("a pick from the page's list wins over the remembered cap and is matched against what the stream really offers", np.contains("pick != null -> QualityLabel.pClass(pick.w, pick.h)") && np.contains("QualityLabel.nearest(w, rowsNow.map { it.first })") && np.contains("fun launch(from: Context, url: String, headers: List<String>, startMs: Long, maxH: Int, priv: Boolean, w: Int, h: Int, pick: QualityLabel.Rung? = null, auto: Boolean = false)"))
    }
}
