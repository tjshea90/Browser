package app.kestrel.core.input.editor

/**
 * An editor that behaves like android.view.inputmethod.InputConnection over an EditText: the semantics the keyboard relies on (commit and
 * composing replace the composing region, else the selection; deleteSurroundingText works around the selection; DEL deletes a code point;
 * Enter inserts a newline in a multi-line field) and the part that bites keyboards: selection reports (onUpdateSelection) are QUEUED, one per
 * change (one per batch), and the test decides when each is delivered. [user] and [app] simulate changes the keyboard did not make.
 */
class FakeEditor(text: String = "", sel: Int = text.length, val multiline: Boolean = true) : EditorBridge {
    private val sb = StringBuilder(text)
    var selS = sel; private set
    var selE = sel; private set
    var compS = -1; private set
    var compE = -1; private set
    private var batch = 0
    private var last = intArrayOf(selS, selE, compS, compE)
    /** The queued onUpdateSelection reports: (newSelStart, newSelEnd, composingStart, composingEnd). */
    val updates = ArrayDeque<IntArray>()
    val actions = ArrayList<Int>()
    /** While true every call fails (returns false / null): the editor is gone. */
    var failing = false
    var calls = 0; private set

    val text: String get() = sb.toString()
    fun beforeText(n: Int): String = sb.substring(maxOf(0, minOf(selS, selE) - n), minOf(selS, selE))
    fun afterText(n: Int): String = sb.substring(maxOf(selS, selE), minOf(sb.length, maxOf(selS, selE) + n))

    /**
     * When set, a report is delivered AT ONCE, INSIDE the call that caused it, instead of being queued in [updates]: what Android 10 and older do for an editor in the keyboard's own process (the calls run inline).
     * (Like InputMethodManager, a report is sent only when the selection or the composing region changed.)
     */
    var inlineSink: ((IntArray) -> Unit)? = null

    private fun emit() {
        if (batch > 0) return
        val now = intArrayOf(selS, selE, compS, compE)
        if (!now.contentEquals(last)) { last = now; val sink = inlineSink; if (sink != null) sink(now) else updates.addLast(now) }
    }
    private fun lo() = minOf(selS, selE)
    private fun hi() = maxOf(selS, selE)
    private fun region(): IntArray = if (compS >= 0) intArrayOf(compS, compE) else intArrayOf(lo(), hi())
    private fun put(text: String, composing: Boolean) {
        val (s, e) = region().let { it[0] to it[1] }
        sb.replace(s, e, text)
        selS = s + text.length; selE = selS
        if (composing && text.isNotEmpty()) { compS = s; compE = s + text.length } else { compS = -1; compE = -1 }
    }
    private fun alive(): Boolean { calls++; return !failing }

    override fun textBefore(max: Int): String? = if (!alive()) null else beforeText(max)
    override fun textAfter(max: Int): String? = if (!alive()) null else afterText(max)
    override fun commitText(text: String): Boolean { if (!alive()) return false; put(text, false); emit(); return true }
    override fun setComposingText(text: String): Boolean { if (!alive()) return false; put(text, true); emit(); return true }
    override fun finishComposing(): Boolean { if (!alive()) return false; compS = -1; compE = -1; emit(); return true }
    override fun deleteSurrounding(before: Int, after: Int): Boolean {
        if (!alive()) return false
        val s = lo(); val e = hi()
        val from = maxOf(0, s - before); val to = minOf(sb.length, e + after)
        sb.delete(e, to); sb.delete(from, s)
        val len = e - s
        selS = from; selE = from + len
        compS = -1; compE = -1
        emit(); return true
    }
    override fun setSelection(start: Int, end: Int): Boolean {
        if (!alive()) return false
        selS = start.coerceIn(0, sb.length); selE = end.coerceIn(0, sb.length); emit(); return true
    }
    override fun sendKey(code: Int): Boolean {
        if (!alive()) return false
        when (code) {
            KeyCodes.DEL -> if (selS != selE) { sb.delete(lo(), hi()); selS = lo(); selE = selS; compS = -1; compE = -1 }
                else if (selS > 0) { val n = if (selS >= 2 && Character.isLowSurrogate(sb[selS - 1]) && Character.isHighSurrogate(sb[selS - 2])) 2 else 1; sb.delete(selS - n, selS); selS -= n; selE = selS; compS = -1; compE = -1 }
            KeyCodes.ENTER -> if (multiline) { sb.replace(lo(), hi(), "\n"); selS = lo() + 1; selE = selS; compS = -1; compE = -1 }
            KeyCodes.DPAD_LEFT -> if (selS != selE) { selS = lo(); selE = selS } else if (selS > 0) { val n = if (selS >= 2 && Character.isLowSurrogate(sb[selS - 1]) && Character.isHighSurrogate(sb[selS - 2])) 2 else 1; selS -= n; selE = selS }
            KeyCodes.DPAD_RIGHT -> if (selS != selE) { selS = hi(); selE = selS } else if (selS < sb.length) { val n = if (selS + 2 <= sb.length && Character.isHighSurrogate(sb[selS]) && Character.isLowSurrogate(sb[selS + 1])) 2 else 1; selS += n; selE = selS }
        }
        emit(); return true
    }
    /** TextUtils.getCapsMode, simplified: characters = always; words = at the start or after whitespace; sentences = at the start, after a newline, or after . ? ! and a space. */
    /** An app whose text field answers "no capitals needed" whatever the text is (some web views and custom editors do): the keyboard must not depend on the answer alone (owner R19a). */
    var capsModeBroken = false
    override fun cursorCapsMode(reqModes: Int): Int {
        if (!alive() || capsModeBroken) return 0
        val b = beforeText(64)
        var mode = 0
        if (reqModes and CapFlags.CHARACTERS != 0) mode = mode or CapFlags.CHARACTERS
        if (reqModes and CapFlags.WORDS != 0 && (b.isEmpty() || b.last().isWhitespace())) mode = mode or CapFlags.WORDS
        if (reqModes and CapFlags.SENTENCES != 0) {
            val t = b.trimEnd(' ', '\t')
            if (t.isEmpty() || t.endsWith("\n") || (t.length < b.length && (t.endsWith(".") || t.endsWith("?") || t.endsWith("!")))) mode = mode or CapFlags.SENTENCES
        }
        return mode
    }
    override fun performAction(action: Int): Boolean { if (!alive()) return false; actions.add(action); return true }
    override fun beginBatch(): Boolean { if (!alive()) return false; batch++; return true }
    override fun endBatch(): Boolean { if (!alive()) return false; if (batch > 0) batch--; emit(); return true }

    // ---- changes the keyboard did not make
    /** The user taps somewhere else in the text. */
    fun userMove(pos: Int) { selS = pos.coerceIn(0, sb.length); selE = selS; emit() }
    /** The user selects a range. */
    fun userSelect(a: Int, b: Int) { selS = a.coerceIn(0, sb.length); selE = b.coerceIn(0, sb.length); emit() }
    /** The user (or the app) inserts [t] at the cursor, e.g. a paste or the app's own autofill. */
    fun userInsert(t: String) { sb.replace(lo(), hi(), t); selS = lo() + t.length; selE = selS; compS = -1; compE = -1; emit() }
    /** An address bar's inline completion: [suffix] is appended after the cursor and SELECTED (the cursor stays where the person typed). */
    fun appAutocomplete(suffix: String) { val at = lo(); sb.insert(hi(), suffix); selS = at; selE = at + suffix.length; compS = -1; compE = -1; emit() }
    /** The app replaces everything with [t] and puts the cursor at the end. */
    fun appSetText(t: String) {
        // An app that replaces the text and leaves the cursor at the same offset sends NO report at all (real Android only reports changes of the
        // selection or the composing region), so nothing could ever detect it: the fake never does that, it moves the cursor by one in that case.
        val old = selS
        sb.setLength(0); sb.append(if (t.length == old) "$t." else t); selS = sb.length; selE = selS; compS = -1; compE = -1; emit()
    }
}
