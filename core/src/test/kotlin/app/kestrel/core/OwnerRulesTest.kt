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

    // AY3, owner message 39: "make the navigation panel keyboard space key able to move the text cursor if I press and hold it then move my finger left or right, just like other keyboard apps do"
    @Test fun theSpaceKeyMovesTheTextCursorWhenSlid() {
        val nav = source("app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt")
        assertTrue(nav.contains("keys.onCursor = { n -> cursorSlid(n) }"))
        val slid = body(nav, "private fun cursorSlid(delta: Int) {")
        assertTrue("the address bar's cursor and the page field's caret both move: $slid", slid.contains("model.moveCursor") && slid.contains("moveCursor(delta)"))
    }
}
