package app.kestrel.browser

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import app.kestrel.browser.ui.PageField
import org.mozilla.geckoview.GeckoSession

/**
 * The mini keyboard types into the web page's own text field (AU3, owner message 35: "if I already have a text field open on a web site, make the mini keyboard enter text into that field instead of
 * searching Google"). Everything here is built from what GeckoView 160 offers (read from its classes, see research/MINI_NAV.md):
 *
 * * It WRAPS the session's text-input delegate. The page tells the delegate when a field gains or loses focus; everything is passed on to the library's own delegate (which talks to Android's keyboard
 *   service), except that while the mini keyboard is up ([held]) the request to show or restart the SYSTEM keyboard is held back, so Gboard never covers the mini one.
 * * [probe] asks the session for its input connection, the same call Android's keyboard makes when it binds: null (or an editor type of 0) = no field has focus. A field that has focus gives a [PageField]
 *   whose keys are real input: committed text, and Delete / Enter as key events, run on the connection's own handler (the library asserts its input thread, exactly as it does for a keyboard).
 *
 * All callbacks and [probe] belong to the UI thread.
 */
class PageFieldInput(
    private val session: GeckoSession,
    private val inner: GeckoSession.TextInputDelegate,
    /** True while the mini keyboard is up: the system keyboard stays down. */
    private val held: () -> Boolean,
    /** A field gained or lost focus (the page's, so the panel can follow it). */
    private val changed: () -> Unit,
) : GeckoSession.TextInputDelegate {
    private var focusCount = 0
    private var cached: Field? = null

    override fun restartInput(session: GeckoSession, reason: Int) {
        cached = null
        if (reason != GeckoSession.TextInputDelegate.RESTART_REASON_CONTENT_CHANGE) focusCount++ // focus moved: another field (or none); a change of the same field's type does not
        if (!held()) inner.restartInput(session, reason)
        changed()
    }

    override fun showSoftInput(session: GeckoSession) {
        cached = null
        if (!held()) inner.showSoftInput(session)
    }

    override fun hideSoftInput(session: GeckoSession) {
        cached = null
        inner.hideSoftInput(session)
    }

    override fun updateSelection(session: GeckoSession, selStart: Int, selEnd: Int, compositionStart: Int, compositionEnd: Int) =
        inner.updateSelection(session, selStart, selEnd, compositionStart, compositionEnd)

    override fun updateExtractedText(session: GeckoSession, request: android.view.inputmethod.ExtractedTextRequest, text: android.view.inputmethod.ExtractedText) =
        inner.updateExtractedText(session, request, text)

    override fun updateCursorAnchorInfo(session: GeckoSession, info: android.view.inputmethod.CursorAnchorInfo) =
        inner.updateCursorAnchorInfo(session, info)

    /** The page's focused text field, or null when no field has focus (or the page asked for no keyboard: `inputmode=none`). */
    fun probe(): PageField? {
        cached?.let { return it }
        val info = EditorInfo()
        val ic = try { session.textInput.onCreateInputConnection(info) } catch (e: Exception) { null } ?: return null
        if (info.inputType == InputType.TYPE_NULL) return null
        return Field(ic, info, focusCount).also { cached = it }
    }

    private class Field(private val ic: InputConnection, info: EditorInfo, override val identity: Int) : PageField {
        private val handler: Handler by lazy { ic.handler ?: Handler(Looper.getMainLooper()) } // also tells the library's editable which thread its input runs on
        private val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        override val password: Boolean = isPassword(info.inputType)
        override val enterSubmits: Boolean = action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_DONE
        override val privateTab: Boolean = (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        override val label: String = (if (password) "password" else when (info.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER -> "number"; InputType.TYPE_CLASS_PHONE -> "phone"; else -> when (info.inputType and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_TEXT_VARIATION_URI -> "address"; InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> "email"; else -> "text" } }) +
            ", Enter = " + when (action) { EditorInfo.IME_ACTION_GO -> "go"; EditorInfo.IME_ACTION_SEARCH -> "search"; EditorInfo.IME_ACTION_SEND -> "send"; EditorInfo.IME_ACTION_DONE -> "done"
                EditorInfo.IME_ACTION_NEXT -> "next"; EditorInfo.IME_ACTION_PREVIOUS -> "previous"; else -> "new line" } + if (privateTab) ", private" else ""

        override fun insert(text: String) { run { ic.commitText(text, 1) } }

        override fun deleteBefore(count: Int) {
            if (count <= 0) return
            run { if (count == 1) key(KeyEvent.KEYCODE_DEL, 0) else ic.deleteSurroundingText(count, 0) }
        }

        override fun enter() { run { key(KeyEvent.KEYCODE_ENTER, KeyEvent.FLAG_EDITOR_ACTION) } }

        private fun run(op: () -> Unit) { handler.post { try { op() } catch (e: Exception) { android.util.Log.w("PageField", "input failed: " + e) } } }

        /** A key press as a keyboard's soft key makes it: the page sees the same key events a hardware keyboard sends (a search box submits, a selection is deleted). */
        private fun key(code: Int, extra: Int) {
            val t = SystemClock.uptimeMillis()
            val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE or extra
            ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
            ic.sendKeyEvent(KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        }

        private companion object {
            fun isPassword(inputType: Int): Boolean {
                val cls = inputType and InputType.TYPE_MASK_CLASS
                val v = inputType and InputType.TYPE_MASK_VARIATION
                return (cls == InputType.TYPE_CLASS_TEXT && (v == InputType.TYPE_TEXT_VARIATION_PASSWORD || v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD || v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) ||
                    (cls == InputType.TYPE_CLASS_NUMBER && v == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            }
        }
    }
}
