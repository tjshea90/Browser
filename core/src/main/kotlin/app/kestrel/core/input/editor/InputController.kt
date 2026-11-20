package app.kestrel.core.input.editor

enum class ShiftState { OFF, ONCE, LOCK }

/**
 * What the last glide put into the editor: [text] is everything it inserted (a space before the word when one was needed, the word, a space after it),
 * [word] the word as shown (capitalised when shift was on). One Backspace removes all of [text]; a candidate from the strip replaces it.
 */
class GlideTail(val text: String, val word: String)

/** What a space (or punctuation) put in the place of a typo: [tail] is everything it inserted ("word "), [typed] what the person had typed. One Backspace right after takes it back. */
class Correction(val typed: String, val word: String, val tail: String)

/**
 * The input state machine (plan 6.4), the replacement of MiniKeyboardModel for a SYSTEM keyboard. The app's editor owns the text; this keeps only
 * a short window of it (about 64 units before the cursor, 16 after), where the cursor is, the word being tapped (composing), the last glide and
 * the shift state. Pure Kotlin on [EditorBridge], so it is tested with a fake editor and a fuzzer instead of a device.
 *
 * Three rules keep it honest:
 *  1. PREDICT, THEN VALIDATE. After each own edit it predicts where the selection will be and remembers that in [pending]. Android reports every
 *     selection change back through onUpdateSelection; a report that matches a prediction is "ours". Anything else means the user or the app
 *     changed the text: composing, glide and anything else that assumed the old text is dropped and the window is re-read. (Updates arrive
 *     late, batched or merged, so several predictions may be outstanding and one report may confirm several; an editor in this keyboard's own
 *     process on Android 10 even reports INSIDE the edit call, before the edit is noted: see [early].)
 *  2. VERIFY BEFORE REPLACING. Removing or replacing the last glide first reads the text before the cursor and refuses when it is not what was
 *     put there. A wrong belief therefore costs a missed shortcut, never someone else's text.
 *  3. NEVER ASSUME SUCCESS. Every editor call can fail; a failure re-reads the editor and forgets what was assumed.
 *
 * All offsets are UTF-16 units. Single-threaded: call from the keyboard's UI thread only.
 */
class InputController(editor: EditorBridge, val stats: InputStats = InputStats()) {
    /** Every call into the editor goes through [Guarded], so a selection report that arrives INSIDE a call can be told from a late one (see [early]). */
    private val ed: EditorBridge = Guarded(editor)
    var field: FieldInfo = FieldInfo(); private set
    /** The text before the cursor (at most [WINDOW_BEFORE] units; the composing word, if any, is at its end). */
    var before = ""; private set
    /** The text after the cursor (at most [WINDOW_AFTER] units). */
    var after = ""; private set
    /** The selection as the editor reported it and as edits moved it; -1 = unknown. */
    var selStart = -1; private set
    var selEnd = -1; private set
    /** The word being tapped, shown to the app as composing text; null when none. */
    var composing: String? = null; private set
    var lastGlide: GlideTail? = null
        private set(value) { field = value; lastCorrection = null }     // anything that can invalidate the glide tail invalidates the correction tail too
    /** The automatic fix a Backspace can still take back; null when anything else happened since. */
    var lastCorrection: Correction? = null; private set
    /** The correction the last Backspace took back (null when it took back none): read once by the learning code, which must hear about it. */
    var revertedCorrection: Correction? = null; private set
    var shift = ShiftState.OFF; private set

    /** Options the settings switch (N7). Off by default here; the keyboard turns them on from its settings. */
    var autoCapsOn = true
    /** Capitalise after sentence punctuation even in a text box that did not ask for it (the default; Settings can turn it off). */
    var sentenceCaps = true
    /** A letter typed right after punctuation that touches the word before it ("hello.world") gets a space first ("hello. world"). */
    var autoSpace = false

    private val pending = Pending()
    /** The editor may still hold a composing region this keyboard lost track of (a failed call, a new session): it is closed before the next edit. */
    private var composingUnknown = false
    /** Inside a batch the editor reports only the FINAL selection (and none at all when it did not change): intermediate states must not be expected. */
    private var batchDepth = 0
    /** After Enter (or a failed move) the next few reports are taken as the truth instead of being compared: the result of Enter is unknown (newline, action, nothing). */
    private var adopt = 0
    /** > 0 while one of this keyboard's own calls into the editor is running. */
    private var inCall = 0
    /**
     * Selection reports that arrived while one of our own calls into the editor was still running: (selection start, selection end, composing start, composing end). Android 10 and older run an editor's calls INLINE when the editor
     * lives in the keyboard's own process (the Setup screen's try-it field, the Dictionary screen's field), so the report of the new cursor position comes BEFORE the call returns, before the keyboard has noted the edit. It is the
     * report of that very edit: [expect] matches it as soon as the edit is noted. What no edit explains is handled like a late report at the next opportunity ([drainEarly]). (An editor in another process never gets here.)
     */
    private val early = ArrayList<IntArray>(4)
    /**
     * True once a report has come inside one of our calls: this editor reports inline, so an edit it did not report inside its call will never be reported (an edit that changed nothing, a composing region that stayed
     * where it was). Nothing is then left waiting in [pending] (an entry that is never answered would make a later move of the cursor to the same offset look like our own edit), and every report that comes OUTSIDE a
     * call is somebody else's. Forgotten at the start of every field (the next editor may be in another process).
     */
    private var inlineEditor = false

    // ------------------------------------------------------------------------------------------------------------ session

    /** A new field got the focus (onStartInput / onStartInputView). Forgets everything about the previous one. */
    fun startInput(info: FieldInfo) {
        field = info
        composing = null; lastGlide = null; shift = ShiftState.OFF; composingUnknown = true; adopt = 0
        clearPending("c1"); early.clear(); inlineEditor = false
        selStart = info.initialSelStart; selEnd = info.initialSelEnd
        refresh()
        autoCaps()
    }

    /** The field wants capitals at the cursor (the start of a sentence, a word, or always, as its flags say): Shift goes on for the next letter. Never switches caps lock off. */
    fun autoCaps() {
        if (!autoCapsOn || shift == ShiftState.LOCK) return
        var flags = field.capsFlags
        // Owner, 2026-11-17: capitalise the first letter after . ! ? BY DEFAULT, also in a text box that did not ask for it. Such a box gets the SENTENCE rule only once there is text
        // before the cursor (an empty box stays lowercase, the app did not ask); e-mail, URL, password and number fields never.
        var extra = false
        if (flags == 0 && field.kind == FieldKind.TEXT && sentenceCaps && before.isNotBlank()) { flags = CapFlags.SENTENCES; extra = true }
        if (flags == 0) return
        if (extra && !before.trimEnd().let { it.endsWith(".") || it.endsWith("!") || it.endsWith("?") || it.endsWith("\u2026") || it.endsWith("\n") } ) { if (shift == ShiftState.ONCE && autoShift) shift = ShiftState.OFF; autoShift = shift == ShiftState.ONCE; return }
        if (ed.cursorCapsMode(flags) != 0) { if (shift == ShiftState.OFF) shift = ShiftState.ONCE } else if (shift == ShiftState.ONCE && autoShift) shift = ShiftState.OFF
        autoShift = shift == ShiftState.ONCE
    }
    private var autoShift = false

    /** The field loses the focus (onFinishInput): the word being tapped stays in the text as typed. */
    fun finishInput() { endComposing(); lastGlide = null; clearPending("c2") }

    /**
     * onUpdateSelection. [candStart]/[candEnd] are the editor's composing region (-1 when none). True when the change was caused by this keyboard's
     * own edits, false when the user or the app moved or changed the text (everything assumed about the old text was dropped).
     */
    fun onSelectionUpdate(newSelStart: Int, newSelEnd: Int, candStart: Int = -1, candEnd: Int = -1): Boolean {
        if (inCall > 0) {                                                       // inside our own edit call: the report of that edit, matched when the edit is noted
            if (!inlineEditor) { inlineEditor = true; clearPending("inline") }
            early.add(intArrayOf(newSelStart, newSelEnd, candStart, candEnd)); return true
        }
        if (early.isNotEmpty()) drainEarly()
        if (candStart >= 0 && composing == null) composingUnknown = true           // the editor shows a composing word this keyboard does not know about
        if (pending.matchAndDrop(newSelStart, newSelEnd)) {
            // an older edit was confirmed: while newer own edits are still outstanding the keyboard is AHEAD of this report and keeps what it predicted
            if (pending.isEmpty()) { selStart = newSelStart; selEnd = newSelEnd }
            return true
        }
        // the same selection as believed: usually only the composing region changed; but an app that replaced the text and left the cursor where it was looks the same, so with no own edit outstanding the window is checked
        if (newSelStart == selStart && newSelEnd == selEnd && (!pending.isEmpty() || windowStillTrue())) return true
        if (selStart < 0) {                                           // the first reports after an unknown state are the truth (and the text may have changed with them)
            if (adopt > 0) adopt--
            selStart = newSelStart; selEnd = newSelEnd; refresh()
            if (composing != null && candStart < 0) composing = null                // the editor no longer shows the word being tapped
            lastGlide?.let { g -> if (!before.endsWith(g.text) || selStart != selEnd) lastGlide = null }
            return true
        }
        // somebody else moved the cursor or edited the text
        if (composing != null) ed.finishComposing()
        stats.foreignReports++
        val glide = lastGlide; val wasAt = selStart
        composing = null; lastGlide = null
        clearPending("c3")
        selStart = newSelStart; selEnd = newSelEnd
        refresh()
        // An address bar (Chrome, Brave, ...) completes what was typed INLINE: it appends the rest of a likely address after the cursor and selects it, which is a report we did not predict. The glided word is still
        // right before where the selection starts, so it stays the last glide (a tap in the strip can still replace it); only a selection or a move that starts somewhere else forgets it.
        if (glide != null && wasAt >= 0 && newSelStart == wasAt && before.endsWith(glide.text)) lastGlide = glide
        return false
    }

    // ------------------------------------------------------------------------------------------------------------ typing

    fun tapShift() { shift = if (shift == ShiftState.OFF) ShiftState.ONCE else ShiftState.OFF }
    fun lockShift() { shift = ShiftState.LOCK }
    fun setShift(s: ShiftState) { shift = s }

    /** A key was tapped whose label is [key]: a letter, a digit, punctuation, ".com". Letters follow the shift state. */
    fun type(key: String) {
        if (key.isEmpty()) return
        stats.keys++
        settle()
        if (autoSpace && composing == null && isLetter(key) && needsAutoSpace() && commit(" ")) { lastGlide = null; autoCaps() }   // "hello." "w" -> "hello. w", and the new sentence starts with a capital
        val t = if (shift != ShiftState.OFF && isSingleCodePoint(key)) key.uppercase() else key
        if (isLetter(t) || (composing != null && isWordConnector(t))) {
            if (field.composingAllowed) { typeComposing(t); return }
        }
        endComposing()
        val g = lastGlide
        // right after a glide, ".", ",", "/", ".com" ... close the word instead of standing after the space the glide added ("hello" ".com" = "hello.com")
        if (g != null && key in CLOSES_WORD && g.text.endsWith(" ") && before.endsWith(" ") && selStart == selEnd) {
            val s0 = selStart; val e0 = selEnd
            beginBatch()
            val ok = ed.deleteSurrounding(1, 0)
            if (ok) { before = before.dropLast(1); moveSel(-1) }
            commit(t)
            endBatch(s0, e0)
            if (!ok) resync()
        } else commit(t)
        lastGlide = null
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ensureWindow()
        autoCaps()                                                  // after ". " (or a newline) the next letter is a capital
    }

    fun space() { type(" ") }

    /** Puts [text] in at the cursor exactly as it is (a clipboard item): it replaces a selection, ends the word being tapped, and the glide before it is no longer undoable. False when the editor refused. */
    fun paste(text: String): Boolean {
        if (text.isEmpty()) return false
        settle()
        endComposing()
        lastGlide = null
        val ok = commit(text)
        ensureWindow()
        autoCaps()
        return ok
    }

    /** The previous character is sentence punctuation that touches a word or a number, and the word is not part of an address, an abbreviation or a number: a space belongs after it. */
    private fun needsAutoSpace(): Boolean {
        if (selStart >= 0 && selStart != selEnd) return false
        if (field.kind != FieldKind.TEXT && field.kind != FieldKind.SEARCH) return false
        val n = before.length
        if (n < 2) return false
        val p = before[n - 1]
        if (p !in AUTO_SPACE_AFTER) return false
        val q = before[n - 2]
        if (!(q.isLetterOrDigit() || q in ")]\"”’'")) return false
        val tok = before.substringAfterLast(' ').substringAfterLast('\n')
        if (tok.any { it == '@' || it == '/' || it == '\\' }) return false
        if (p == '.') { val core = tok.dropLast(1); if (core.length <= 1 || core.contains('.')) return false }   // e.g, U.S, example.com, a.m
        if (p == ':' && tok.length > 1 && tok.dropLast(1).all { it.isDigit() }) return false           // 10:30
        return true
    }

    /**
     * A second space right after a word ("hello " " ") becomes ". " (a full stop and one space): the habit of typing a sentence end with two taps. False (nothing changed, the caller types a space) when the text before
     * the cursor is not a word and one space, or something is selected.
     */
    fun doubleSpace(): Boolean {
        settle()
        if (composing != null || selStart != selEnd || before.length < 2 || !before.endsWith(" ")) return false
        val q = before[before.length - 2]
        if (!(q.isLetterOrDigit() || q in ")]\"”’")) return false
        if (field.kind != FieldKind.TEXT && field.kind != FieldKind.SEARCH) return false
        stats.keys++
        val s0 = selStart; val e0 = selEnd
        beginBatch()
        var ok = ed.deleteSurrounding(1, 0)
        if (ok) { before = before.dropLast(1); moveSel(-1); ok = commit(". ") }
        endBatch(s0, e0)
        lastGlide = null
        if (!ok) { resync(); return true }
        ensureWindow()
        autoCaps()
        return true
    }

    /** One press of Backspace. A tapped word loses its last letter; the word a glide just added goes whole; otherwise the editor deletes (selection or one character). */
    fun backspace() { backspaceInner(); autoCaps() }

    private fun backspaceInner() {
        settle()
        stats.backspaces++
        revertedCorrection = null
        val fix = lastCorrection
        if (fix != null && composing == null && selStart == selEnd && verifyBefore(fix.tail)) {   // a space just replaced a typo: this Backspace puts the typo back (as the word being tapped)
            val s0 = selStart; val e0 = selEnd
            beginBatch()
            var ok = ed.deleteSurrounding(fix.tail.length, 0)
            if (ok) {
                before = before.dropLast(fix.tail.length); moveSel(-fix.tail.length)
                ok = ed.setComposingText(fix.typed)
                if (ok) { composing = fix.typed; before = (before + fix.typed).takeLast(WINDOW_BEFORE); moveSel(fix.typed.length) }
            }
            endBatch(s0, e0)
            lastGlide = null                        // (also clears lastCorrection)
            if (!ok) { resync(); return }
            revertedCorrection = fix
            ensureWindow()
            return
        }
        val c = composing
        if (c != null) {
            val shorter = dropLastCodePoint(c)
            val removed = c.length - shorter.length
            if (shorter.isEmpty()) { val ok = ed.setComposingText("") ; ed.finishComposing(); composing = null; if (!ok) { resync(); return } }
            else { if (!ed.setComposingText(shorter)) { resync(); return }; composing = shorter }
            before = before.dropLast(removed)
            moveSel(-removed)
            lastGlide = null
            ensureWindow()
            return
        }
        val g = lastGlide
        if (g != null && selStart == selEnd && verifyBefore(g.text)) {
            ed.beginBatch()
            val ok = ed.deleteSurrounding(g.text.length, 0)
            ed.endBatch()
            lastGlide = null
            if (!ok) { resync(); return }
            before = before.dropLast(g.text.length)
            moveSel(-g.text.length)
            ensureWindow()
            return
        }
        lastGlide = null
        if (selStart != selEnd) {                       // a selection: the editor deletes it, the cursor ends where the selection began
            if (!ed.sendKey(KeyCodes.DEL)) { resync(); return }
            selEnd = selStart; expect(selStart, selEnd)
        } else {
            val n = lastCodePointLength(before)
            if (!ed.sendKey(KeyCodes.DEL)) { resync(); return }
            before = before.dropLast(n)
            if (n > 0) moveSel(-n)                          // (Backspace at the very start of the text changes nothing and the editor reports nothing: no report to wait for, or the entry would stay and make a later move to the same offset look like ours)
        }
        ensureWindow()
    }

    /** Enter: the field's own action when it has one, else a real Enter key. The text after it is unknown, so the window is re-read. */
    fun enter() {
        settle()
        stats.enters++
        endComposing(); lastGlide = null
        val a = field.imeAction
        val useAction = !field.noEnterAction && a != ImeActions.UNSPECIFIED && a != ImeActions.NONE
        val ok = if (useAction) ed.performAction(a) else ed.sendKey(KeyCodes.ENTER)
        if (!ok) { resync(); return }
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        refresh(); selStart = -1; selEnd = -1; clearPending("c4");   // the next selection reports are the truth
        autoCaps()
    }

    /** A glide ended on [word] (already ranked by the decoder). It is inserted at the cursor with a space after it, and one before it when the text does not end in a space. */
    fun commitGlide(word: String) {
        if (word.isEmpty()) return
        settle()
        endComposing()
        val w = when (shift) { ShiftState.LOCK -> word.uppercase(); ShiftState.ONCE -> word.replaceFirstChar { it.uppercase() }; ShiftState.OFF -> word }
        val needsLead = before.isNotEmpty() && !before.last().isWhitespace() && before.last() !in NO_SPACE_AFTER
        val tail = (if (needsLead) " " else "") + w + " "
        if (!commit(tail)) return
        stats.glides++
        lastGlide = GlideTail(tail, w)
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ensureWindow()
    }

    /** A candidate from the strip replaces the word the last glide put in. False (nothing changed) when the last thing was not a glide or the text is no longer there. */
    fun swapGlide(word: String): Boolean {
        val g = lastGlide ?: return false
        if (!verifyBefore(g.text)) { lastGlide = null; return false }          // (a selection that starts right after the word is an address bar's inline completion: the new word replaces it)
        val lead = if (g.text.startsWith(" ")) " " else ""
        val shown = g.word
        val w = if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (shown.firstOrNull()?.isUpperCase() == true) word.replaceFirstChar { it.uppercase() } else word
        val newTail = lead + w + " "
        val s0 = selStart; val e0 = selEnd
        beginBatch()
        val deleted = ed.deleteSurrounding(g.text.length, 0)
        var ok = deleted
        if (deleted) {
            before = before.dropLast(g.text.length); moveSel(-g.text.length)
            ok = commit(newTail)
        }
        endBatch(s0, e0)
        if (!ok) { lastGlide = null; resync(); return false }
        lastGlide = GlideTail(newTail, w)
        stats.swaps++
        ensureWindow()
        return true
    }

    /**
     * A word from the strip was tapped while typing with taps (N4): it replaces the word at the cursor and a space follows. With no word at the cursor it is put in (after a space when the text does not
     * end in one): the likely next word. False (nothing changed) when something is selected or the editor refused.
     */
    fun pickWord(word: String): Boolean {
        if (word.isEmpty() || (selStart >= 0 && selStart != selEnd)) return false
        settle()
        val c = composing
        val stem = wordBeforeCursor() ?: ""
        var w = word
        if (stem.isEmpty()) w = when (shift) { ShiftState.LOCK -> w.uppercase(); ShiftState.ONCE -> w.replaceFirstChar { it.uppercase() }; ShiftState.OFF -> w }
        val s0 = selStart; val e0 = selEnd
        beginBatch()
        var ok = true
        if (c != null) {                                                  // the word being tapped is replaced where it stands
            ok = ed.setComposingText(w)
            if (ok) { before = (before.dropLast(c.length) + w).takeLast(WINDOW_BEFORE); moveSel(w.length - c.length); composing = null; ed.finishComposing() }
        } else if (stem.isNotEmpty()) {                                   // plain letters before the cursor (a word the person went back to): they are replaced
            ok = verifyBefore(stem) && ed.deleteSurrounding(stem.length, 0)
            if (ok) { before = before.dropLast(stem.length); moveSel(-stem.length); ok = commit(w) }
        } else {
            val needsLead = before.isNotEmpty() && !before.last().isWhitespace() && before.last() !in NO_SPACE_AFTER
            ok = commit((if (needsLead) " " else "") + w)
        }
        if (ok) ok = commit(" ")
        endBatch(s0, e0)
        lastGlide = null
        if (!ok) { resync(); return false }
        stats.swaps++
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ensureWindow()
        return true
    }

    /**
     * The word being tapped is a typo and [boundary] (a space, or punctuation) ends it: [fix] goes in instead, then the boundary. One Backspace right after ([lastCorrection], [revertedCorrection]) puts the typed
     * word back. Falls back to the plain key when there is no word being tapped.
     */
    fun typeCorrected(fix: String, boundary: String) {
        val typed = composing
        if (typed == null || fix.isEmpty() || (selStart >= 0 && selStart != selEnd)) { type(boundary); return }
        settle()
        val shown = if (typed.length > 1 && typed.all { it.isUpperCase() }) fix.uppercase() else if (typed.first().isUpperCase()) fix.replaceFirstChar { it.uppercase() } else fix
        if (!ed.setComposingText(shown)) { resync(); type(boundary); return }
        before = (before.dropLast(typed.length) + shown).takeLast(WINDOW_BEFORE)
        moveSel(shown.length - typed.length)    // (the editor reports the changed composing text even when the selection did not move: expect that report)
        composing = shown
        type(boundary)
        val tail = shown + boundary
        if (composing == null && selStart == selEnd && before.endsWith(tail)) lastCorrection = Correction(typed, shown, tail)
    }

    /**
     * The word [old] that was just glided or typed (with up to two spaces after it) is replaced by [word] when the text before the cursor still ends with it: the safety net for when this keyboard lost track of its own
     * glide (an app rewrote the field) but the person still taps the right word in the strip. Verified against the editor first, like every replacement. False (nothing changed) when the text no longer ends with [old].
     */
    fun replaceRecent(old: String, word: String): Boolean {
        if (old.isEmpty() || word.isEmpty()) return false
        settle()
        val k = before.length - before.trimEnd(' ').length
        val core = before.dropLast(k)
        if (k > 2 || !core.endsWith(old, ignoreCase = true)) return false
        val shown = core.takeLast(old.length)
        val n = old.length + k
        if (!verifyBefore(before.takeLast(n))) return false
        val w = if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (shown.first().isUpperCase()) word.replaceFirstChar { it.uppercase() } else word
        val tail = w + " ".repeat(k)
        val s0 = selStart; val e0 = selEnd
        beginBatch()
        var ok = ed.deleteSurrounding(n, 0)
        if (ok) { before = before.dropLast(n); moveSel(-n); ok = commit(tail) }
        endBatch(s0, e0)
        lastGlide = null
        if (!ok) { resync(); return false }
        lastGlide = GlideTail(tail, w)
        stats.swaps++
        ensureWindow()
        return true
    }

    /** Moves the cursor [delta] characters (negative = left), e.g. from sliding a finger over the space key. A tapped word is finished first. */
    fun moveCursor(delta: Int) {
        if (delta == 0) return
        settle()
        stats.cursorMoves++
        endComposing(); lastGlide = null
        val left = delta < 0
        val steps = minOf(Math.abs(delta), 64)
        var predicted = selStart >= 0 && selStart == selEnd   // a selection collapses to one end and the selected text joins the window: re-read instead of predicting
        for (i in 0 until steps) {
            if (!ed.sendKey(if (left) KeyCodes.DPAD_LEFT else KeyCodes.DPAD_RIGHT)) { resync(); return }
            if (!predicted) continue
            if (left) {
                if (before.isEmpty()) { if (selStart != 0) predicted = false; continue }
                val n = lastCodePointLength(before)
                after = (before.takeLast(n) + after).take(WINDOW_AFTER); before = before.dropLast(n); if (selStart >= 0) { selStart -= n; selEnd = selStart; expect(selStart, selEnd) }   // every arrow key is reported on its own
            } else {
                if (after.isEmpty()) { predicted = false; continue }       // we cannot tell whether the field ends here
                val n = firstCodePointLength(after)
                before = (before + after.take(n)).takeLast(WINDOW_BEFORE); after = after.drop(n); if (selStart >= 0) { selStart += n; selEnd = selStart; expect(selStart, selEnd) }
            }
        }
        if (selStart >= 0 && predicted) { selEnd = selStart; ensureWindow() }
        else { selStart = -1; selEnd = -1; clearPending("move"); refresh() }    // one report per arrow key reaches us, none of them predicted: the next report is the truth
    }

    /**
     * The word the cursor stands at the end of, as typed (letters and apostrophes: the tapped word being composed, or plain text before the cursor); null when there is none, when the cursor is inside a word,
     * when something is selected, or when the run of letters touches a digit (a code, not a word).
     */
    fun wordBeforeCursor(): String? {
        if (selStart >= 0 && selStart != selEnd) return null
        if (after.isNotEmpty() && (after[0].isLetter() || after[0] == '\'')) return null
        var i = before.length
        while (i > 0 && (before[i - 1].isLetter() || before[i - 1] == '\'')) i--
        if (i == 0 && before.length >= WINDOW_BEFORE) return null          // the window may cut it
        if (i > 0 && (before[i - 1].isDigit() || before[i - 1] == '@')) return null
        val w = before.substring(i).trimStart('\'')
        return if (w.isEmpty() || w.length > 24) null else w
    }

    /** (the second last, the last) plain word BEFORE the word [word] that ends at the cursor (as [wordBeforeCursor] gave it): the language's context for it. */
    fun contextBefore(word: String): Pair<String?, String?> =
        if (before.endsWith(word)) twoBefore(before.dropLast(word.length)) else null to null

    /** The word before the cursor in the window, lowercase, for the language (null when there is none or it is not a plain word). */
    fun previousWord(): String? {
        val tok = before.trimEnd().substringAfterLast(' ')
        if (tok.isEmpty() || tok.length > 24 || !tok.all { it.isLetter() || it == '\'' }) return null
        return tok.lowercase()
    }

    /** (the second last word, the last word) before the cursor, lowercase: the language's context for the next glide. Both null when the last thing is not a plain word; the first null when only one word precedes. */
    fun previousTwo(): Pair<String?, String?> = twoBefore(before)

    private fun twoBefore(text: String): Pair<String?, String?> {
        val toks = text.trimEnd().split(' ')
        fun plain(t: String): String? = if (t.isNotEmpty() && t.length <= 24 && t.all { it.isLetter() || it == '\'' }) t.lowercase() else null
        val last = toks.lastOrNull()?.let { plain(it) } ?: return null to null
        val second = if (toks.size >= 2) plain(toks[toks.size - 2]) else null
        return second to last
    }

    // ------------------------------------------------------------------------------------------------------------ internals

    private fun typeComposing(t: String) {
        val old = composing ?: ""
        val newC = old + t
        if (!ed.setComposingText(newC)) { resync(); return }
        composing = newC
        before = (before + t).takeLast(WINDOW_BEFORE)
        moveSel(t.length)
        lastGlide = null
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
    }

    private fun endComposing() {
        if (composing != null) { ed.finishComposing(); composing = null; expectReportOfSameSelection() }
    }

    /** Ending composition changes only the composing region, but the editor still reports it (with the unchanged selection): that report is ours too, even when it arrives after later edits. */
    private fun expectReportOfSameSelection() { if (selStart >= 0 && batchDepth == 0) expect(selStart, selEnd) }

    /** Inserts [text] at the cursor (replacing a selection) and updates what is believed. */
    private fun commit(text: String): Boolean {
        if (!ed.commitText(text)) { resync(); return false }
        before = (before + text).takeLast(WINDOW_BEFORE)
        if (selStart >= 0) { selStart += text.length; selEnd = selStart; expectNow() }
        return true
    }

    /** The cursor moved by [delta] units because of an own edit. */
    private fun moveSel(delta: Int) {
        if (selStart < 0) return
        selStart = maxOf(0, selStart + delta); selEnd = selStart
        expectNow()
    }

    /** The editor will report the current selection (outside a batch; inside one only the final state is reported, see [endBatch]). */
    private fun expectNow() { if (batchDepth == 0) expect(selStart, selEnd) }

    /** The editor will report the selection ([s], [e]) for an edit just made, unless it already did, inside the call ([early]): then there is nothing to wait for (the oldest match wins, older reports are explained too). */
    private fun expect(s: Int, e: Int) {
        for (k in early.indices) { val r = early[k]; if (r[0] == s && r[1] == e) { repeat(k + 1) { early.removeAt(0) }; return } }
        if (!inlineEditor) pending.push(s, e)
    }

    /** Reports that arrived inside a call and that no edit explained are taken now like any report (a foreign change is noticed, an unknown selection is adopted). */
    private fun drainEarly() {
        if (early.isEmpty()) return
        val list = ArrayList(early); early.clear()
        for (r in list) onSelectionUpdate(r[0], r[1], r[2], r[3])
    }

    private fun beginBatch() { batchDepth++; ed.beginBatch() }

    /** Ends a batch that started with the selection ([s0], [e0]): the one report the editor sends for it is expected, unless the selection did not change (then it sends none). */
    private fun endBatch(s0: Int, e0: Int) {
        if (batchDepth > 0) batchDepth--
        ed.endBatch()
        if (batchDepth == 0 && selStart >= 0 && (selStart != s0 || selEnd != e0)) expect(selStart, selEnd)
    }

    /** A cheap look at the editor: the few units around the cursor are still what the window says. */
    private fun windowStillTrue(): Boolean {
        val b = before.takeLast(16); val a = after.take(8)
        return ed.textBefore(b.length) == b && ed.textAfter(a.length) == a
    }

    private fun verifyBefore(tail: String): Boolean = ed.textBefore(tail.length) == tail && before.endsWith(tail)

    internal fun pendingForTest(): String = pending.describe() + " (last cleared by " + lastClear + ")"
    private var lastClear = "never"
    private fun clearPending(why: String) { pending.clear(); lastClear = why }

    private fun refresh() { before = ed.textBefore(WINDOW_BEFORE) ?: ""; after = ed.textAfter(WINDOW_AFTER) ?: "" }

    /** Something failed or surprised us: forget every assumption and re-read the editor. */
    private fun resync() {
        stats.resyncs++
        composing = null; lastGlide = null; clearPending("c5"); composingUnknown = true
        selStart = -1; selEnd = -1
        refresh()
    }

    /** Called before every edit: closes a composing region the keyboard lost track of, so an edit can never replace somebody's composing word by accident. */
    private fun settle() {
        drainEarly()
        if (composingUnknown && composing == null && ed.finishComposing()) { composingUnknown = false; expectReportOfSameSelection() }
    }

    /** Edits shorten the window from the front; when text exists before it, read it again. */
    private fun ensureWindow() {
        if (before.length < MIN_BEFORE && (selStart < 0 || selStart > before.length)) before = ed.textBefore(WINDOW_BEFORE) ?: before
    }

    private fun isLetter(s: String): Boolean = isSingleCodePoint(s) && Character.isLetter(s.codePointAt(0))
    private fun isWordConnector(s: String): Boolean = s == "'" || (isSingleCodePoint(s) && Character.isDigit(s.codePointAt(0)))
    private fun isSingleCodePoint(s: String) = s.isNotEmpty() && s.codePointCount(0, s.length) == 1

    /** The editor with a count of the calls in flight ([inCall]): a report that arrives while the count is above zero came inside the call (see [early]). */
    private inner class Guarded(private val e: EditorBridge) : EditorBridge {
        private inline fun <T> call(f: () -> T): T { inCall++; try { return f() } finally { inCall-- } }
        override fun textBefore(max: Int): String? = call { e.textBefore(max) }
        override fun textAfter(max: Int): String? = call { e.textAfter(max) }
        override fun commitText(text: String): Boolean = call { e.commitText(text) }
        override fun setComposingText(text: String): Boolean = call { e.setComposingText(text) }
        override fun finishComposing(): Boolean = call { e.finishComposing() }
        override fun deleteSurrounding(before: Int, after: Int): Boolean = call { e.deleteSurrounding(before, after) }
        override fun setSelection(start: Int, end: Int): Boolean = call { e.setSelection(start, end) }
        override fun sendKey(code: Int): Boolean = call { e.sendKey(code) }
        override fun performAction(action: Int): Boolean = call { e.performAction(action) }
        override fun cursorCapsMode(reqModes: Int): Int = call { e.cursorCapsMode(reqModes) }
        override fun beginBatch(): Boolean = call { e.beginBatch() }
        override fun endBatch(): Boolean = call { e.endBatch() }
    }

    /** The selections this keyboard expects the editor to report back for its own edits, oldest first (a small ring: reports can arrive late or merged). */
    private class Pending {
        private val a = IntArray(CAP); private val b = IntArray(CAP)
        private var head = 0; private var size = 0
        fun clear() { size = 0 }
        fun isEmpty() = size == 0
        fun describe(): String = (0 until size).joinToString(",") { val i = (head + it) % CAP; "${a[i]}" }
        fun push(s: Int, e: Int) {
            if (size == CAP) { head = (head + 1) % CAP; size-- }
            val i = (head + size) % CAP; a[i] = s; b[i] = e; size++
        }
        /**
         * True when (s, e) is one of the expected selections; that one and every older one are forgotten. The OLDEST match wins: reports arrive in the order of the edits, and
         * offsets repeat (type a letter, delete it: the cursor is back where it was), so matching from the newest end would swallow the entries of reports still on their way.
         * (A merged report that skips some states then leaves their entries behind; stale entries only ever make an external move to the same offset look like ours.)
         */
        fun matchAndDrop(s: Int, e: Int): Boolean {
            for (k in 0 until size) {
                val i = (head + k) % CAP
                if (a[i] == s && b[i] == e) { head = (head + k + 1) % CAP; size -= k + 1; return true }
            }
            return false
        }
        companion object { const val CAP = 160 }   // a cursor slide of 64 steps expects 64 reports, plus the late ones of earlier edits
    }

    companion object {
        const val WINDOW_BEFORE = 64
        const val WINDOW_AFTER = 16
        const val MIN_BEFORE = 16
        /** Keys that join the word before them: right after a glide they take away the space the glide added. */
        /** Punctuation after which the next word needs a space (when [autoSpace] is on). */
        const val AUTO_SPACE_AFTER = ".,!?;:"
        val CLOSES_WORD: Set<String> = setOf(".", ",", "?", "!", ":", ";", "/", ".com", ")", "'", "-", "_", "@", "%")
        /** A glide right after one of these starts the word without a space before it. */
        const val NO_SPACE_AFTER = "([{“‘/@#"

        fun dropLastCodePoint(s: String): String = if (s.isEmpty()) s else s.dropLast(lastCodePointLength(s))
        fun lastCodePointLength(s: String): Int =
            if (s.length >= 2 && Character.isLowSurrogate(s[s.length - 1]) && Character.isHighSurrogate(s[s.length - 2])) 2 else if (s.isEmpty()) 0 else 1
        fun firstCodePointLength(s: String): Int =
            if (s.length >= 2 && Character.isHighSurrogate(s[0]) && Character.isLowSurrogate(s[1])) 2 else if (s.isEmpty()) 0 else 1
    }
}
