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
}
