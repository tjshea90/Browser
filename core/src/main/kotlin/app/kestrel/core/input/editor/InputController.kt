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
    /**
     * Owner R20c (2026-11-18): the pronoun is always a capital I: i, i'm, i'll, i've, i'd. Applied to every word this controller writes (a glide, a strip word, a replacement) and to a lone i that a space, punctuation or Enter
     * ends in a box that shows no composing text; the tapped-letters path of the other boxes is fixed by the keyboard before the boundary key ([typeCorrected]). Plain text and search boxes only.
     */
    var capitalizeI = false
    private fun pronounKind() = capitalizeI && (field.kind == FieldKind.TEXT || field.kind == FieldKind.SEARCH)
    private fun written(w: String): String = if (pronounKind()) pronounI(w) else w
    /**
     * The way a glided or picked word is written inside a sentence when it is not the start of one (R18b: "Monday", "Sarah", "iPhone"): given the lowercase word and the lowercase word before it (or null), the form or null. Set by the keyboard from its case book
     * (built-in names plus what this person writes with capitals); the default writes every word as the decoder gave it.
     */
    var caseForm: (String, String?) -> String? = { _, _ -> null }

    /**
     * Owner R19b (2026-11-18): "if i put a period then deliberately backspace ... do not let the keyboard automatically add a space if i deliberately delete the space". The cursor offset where the owner took away the
     * space after punctuation with Backspace: no space is put in there again (not for a tapped letter, a glide or a strip word) until the cursor is somewhere else. -1 = none.
     */
    private var keepTight = -1
    private fun tightHere() = keepTight >= 0 && selStart == keepTight && selStart == selEnd
    /**
     * Owner R21: the cursor offset right after the space a glide or a strip word added behind itself, until anything else is done. A closing quote typed exactly here takes that space away ("this" and not "this "); a space the
     * person typed is never touched. -1 = none.
     */
    private var autoTrailAt = -1
    /**
     * Owner R23b (2026-11-18): "never at any time allow the keyboard to learn and save anything from the clipboard. Just because i copy and paste something doesn't mean it is a word to learn." The end of the text before the
     * cursor that was TYPED on this keyboard by key presses, key by key (Backspace takes from its end; a paste, an app, a glide, a strip word, a cursor move or a new field empty it). Only a word that lies wholly inside it is ever
     * learned from tapping ([typedWord]).
     */
    private var typedTail = ""
    /** The ends of the last pastes (the keyboard's own paste: the copied-text box, the clipboard panel): words before them are not the language the person is writing, so they are no context for what comes after ([afterPaste]). */
    private val pastedTails = ArrayList<String>(3)

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
        composing = null; lastGlide = null; shift = ShiftState.OFF; composingUnknown = true; adopt = 0; keepTight = -1; autoTrailAt = -1; typedTail = ""; pastedTails.clear()
        clearPending("c1"); early.clear(); inlineEditor = false
        selStart = info.initialSelStart; selEnd = info.initialSelEnd
        refresh()
        autoCaps()
    }

    /**
     * The field wants capitals at the cursor (the start of a sentence, a word, or always, as its flags say): Shift goes on for the next letter. Never switches caps lock off.
     * A text box that asked for nothing gets the keyboard's own rule (owner R18b, after 2026-11-17): a capital at the start of the box, after a newline and after . ! ? followed by a space ([startsSentence]);
     * e-mail, URL, password, number, search and address boxes never.
     */
    fun autoCaps() {
        if (!autoCapsOn || shift == ShiftState.LOCK) return
        val flags = field.capsFlags
        if (flags == 0) {
            if (field.kind != FieldKind.TEXT || !sentenceCaps) return
            if (startsSentence(before, selStart)) { if (shift == ShiftState.OFF) shift = ShiftState.ONCE } else if (shift == ShiftState.ONCE && autoShift) shift = ShiftState.OFF
            autoShift = shift == ShiftState.ONCE
            return
        }
        // owner R19a ("sometimes it doesn't capitalize new sentences"): the field's own answer is not trusted alone (web views and custom editors answer "no capitals" whatever the text is): the keyboard's rule counts too
        val wanted = ed.cursorCapsMode(flags) != 0 || (sentenceCaps && field.kind == FieldKind.TEXT && startsSentence(before, selStart))
        if (wanted) { if (shift == ShiftState.OFF) shift = ShiftState.ONCE } else if (shift == ShiftState.ONCE && autoShift) shift = ShiftState.OFF
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
            keepTight = -1; autoTrailAt = -1; typedTail = ""
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
        composing = null; lastGlide = null; keepTight = -1; autoTrailAt = -1; typedTail = ""
        clearPending("c3")
        selStart = newSelStart; selEnd = newSelEnd
        refresh()
        // An address bar (Chrome, Brave, ...) completes what was typed INLINE: it appends the rest of a likely address after the cursor and selects it, which is a report we did not predict. The glided word is still
        // right before where the selection starts, so it stays the last glide (a tap in the strip can still replace it); only a selection or a move that starts somewhere else forgets it.
        if (glide != null && wasAt >= 0 && newSelStart == wasAt && before.endsWith(glide.text)) lastGlide = glide
        autoCaps()                                                  // the cursor stands somewhere else now: a sentence start asks for a capital, the middle of a sentence does not
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
        if (composing == null && key.length == 1 && (key[0] == ' ' || key[0] in PRONOUN_BOUNDARY)) fixLoneI()
        val t = if (shift != ShiftState.OFF && isSingleCodePoint(key)) key.uppercase() else key
        typedTail = (typedTail + t).takeLast(TYPED_TAIL_MAX)                                                                       // (R23b: what was typed key by key, ending at the cursor)
        if (isLetter(t) || (composing != null && isWordConnector(t))) {
            if (field.composingAllowed) { typeComposing(t); return }
        }
        endComposing()
        val g = lastGlide
        val glideTrail = g != null && g.text.endsWith(" ")
        val ownTrail = glideTrail || (autoTrailAt >= 0 && autoTrailAt == selStart)
        // owner R21: a closing quote also takes away the space our own glide or strip word left behind it ("this" and not "this ")
        val closingQuote = ownTrail && ((key == "\"" && insideQuotes(false)) || (key == "\u201D" && insideQuotes(true)))
        // right after a glide, ".", ",", "/", ".com" ... close the word instead of standing after the space the glide added ("hello" ".com" = "hello.com")
        if (((glideTrail && key in CLOSES_WORD) || closingQuote) && before.endsWith(" ") && selStart == selEnd) {
            val s0 = selStart; val e0 = selEnd
            beginBatch()
            val ok = ed.deleteSurrounding(1, 0)
            if (ok) { before = before.dropLast(1); moveSel(-1) }
            commit(t)
            endBatch(s0, e0)
            if (!ok) resync()
        } else commit(t)
        lastGlide = null; autoTrailAt = -1
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
        lastGlide = null; keepTight = -1; autoTrailAt = -1; typedTail = ""
        text.takeLast(PASTE_TAIL).takeIf { it.isNotBlank() }?.let { pastedTails.add(it); while (pastedTails.size > 3) pastedTails.removeAt(0) }       // (R23b: nothing before this counts as context)
        val ok = commit(text)
        ensureWindow()
        autoCaps()
        return ok
    }

    /** The previous character is sentence punctuation that touches a word or a number, and the word is not part of an address, an abbreviation or a number: a space belongs after it. */
    private fun needsAutoSpace(): Boolean {
        if (selStart >= 0 && selStart != selEnd) return false
        if (tightHere()) return false                                 // the owner deleted this very space on purpose (R19b)
        if (field.kind != FieldKind.TEXT && field.kind != FieldKind.SEARCH) return false
        return wantsSpaceBefore(before)
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
    fun backspace() {
        val had = typedTail; val s0 = selStart
        backspaceInner()
        // R23b: what Backspace takes off the end shortens the typed tail (a typo fixed on the way, or a space taken back into the word, is still typed text); a bigger change ends it
        typedTail = if (had.isEmpty() || s0 < 0 || selStart < 0) "" else { val d = s0 - selStart; if (d in 1..had.length) had.dropLast(d) else "" }
        autoCaps()
    }

    private fun backspaceInner() {
        settle(); autoTrailAt = -1
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
            val spaceAfterPunct = n == 1 && before.length >= 2 && before.last() == ' ' && before[before.length - 2] in AUTO_SPACE_AFTER
            if (!ed.sendKey(KeyCodes.DEL)) { resync(); return }
            before = before.dropLast(n)
            if (n > 0) moveSel(-n)
            if (spaceAfterPunct && selStart >= 0) keepTight = selStart          // R19b: taken away on purpose, so it is not put back where it was                          // (Backspace at the very start of the text changes nothing and the editor reports nothing: no report to wait for, or the entry would stay and make a later move to the same offset look like ours)
        }
        ensureWindow()
    }

    /**
     * Owner R23a: Backspace held long enough deletes a WHOLE WORD per repeat ([app.kestrel.core.input.BackspaceRepeat]): the spaces before the cursor and the word (letters, digits, apostrophes) in front of them, or one mark and
     * the spaces before it. A word still being tapped goes whole; a glide just made goes whole (as one Backspace does); a selection is deleted. Nothing before the cursor in the window: one plain Backspace.
     */
    fun deleteWord() {
        settle(); typedTail = ""
        val c = composing
        if (c != null && lastGlide == null && selStart == selEnd) {                       // the word being tapped goes whole
            stats.backspaces++; revertedCorrection = null
            val ok = ed.setComposingText(""); ed.finishComposing(); composing = null
            if (!ok) { resync(); return }
            before = before.dropLast(c.length); moveSel(-c.length)
            ensureWindow(); autoCaps(); return
        }
        if (c != null || lastGlide != null || selStart != selEnd) { backspaceInner(); autoCaps(); return }
        stats.backspaces++
        revertedCorrection = null
        val b = before; var i = b.length
        while (i > 0 && b[i - 1].isWhitespace()) i--
        if (i > 0) {
            if (b[i - 1].isLetterOrDigit() || b[i - 1] == '\'') { while (i > 0 && (b[i - 1].isLetterOrDigit() || b[i - 1] == '\'')) i-- }
            else i -= lastCodePointLength(b.substring(0, i))
        }
        val n = b.length - i
        if (n <= 0) { backspaceInner(); autoCaps(); return }
        val s0 = selStart; val e0 = selEnd
        beginBatch()
        var ok = verifyBefore(b.takeLast(n)) && ed.deleteSurrounding(n, 0)
        if (ok) { before = before.dropLast(n); moveSel(-n) }
        endBatch(s0, e0)
        lastGlide = null; autoTrailAt = -1
        if (!ok) { resync(); return }
        ensureWindow()
        autoCaps()
    }

    /** Enter: the field's own action when it has one, else a real Enter key. The text after it is unknown, so the window is re-read. */
    fun enter() {
        settle()
        stats.enters++
        endComposing(); lastGlide = null; keepTight = -1; autoTrailAt = -1; typedTail = ""
        fixLoneI()
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
        val form = caseForm(word, previousWord())
        val lead = leadForNewWord()
        val w = written(when (shiftWithLead(lead)) { ShiftState.LOCK -> word.uppercase(); ShiftState.ONCE -> if (form != null && form.first().isUpperCase()) form else (form ?: word).replaceFirstChar { it.uppercase() }; ShiftState.OFF -> form ?: word })
        val tail = lead + w + " "
        typedTail = ""
        if (!commit(tail)) return
        stats.glides++
        lastGlide = GlideTail(tail, w); autoTrailAt = if (selStart >= 0) selStart else -1
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ensureWindow()
    }

    /** A candidate from the strip replaces the word the last glide put in. False (nothing changed) when the last thing was not a glide or the text is no longer there. */
    fun swapGlide(word: String): Boolean {
        val g = lastGlide ?: return false
        if (!verifyBefore(g.text)) { lastGlide = null; return false }          // (a selection that starts right after the word is an address bar's inline completion: the new word replaces it)
        val lead = if (g.text.startsWith(" ")) " " else ""
        val shown = g.word
        val form = caseForm(word.lowercase(), wordEndingAt(before.dropLast(g.text.length)))
        val w = written(if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (shown.firstOrNull()?.isUpperCase() == true) (if (form != null && form.first().isUpperCase()) form else word.replaceFirstChar { it.uppercase() }) else form ?: word)
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
        val lead = if (c == null && stem.isEmpty()) leadForNewWord() else ""
        if (stem.isEmpty()) w = when (shiftWithLead(lead)) { ShiftState.LOCK -> w.uppercase(); ShiftState.ONCE -> w.replaceFirstChar { it.uppercase() }; ShiftState.OFF -> w }
        w = written(w)
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
            ok = commit(lead + w)
        }
        if (ok) ok = commit(" ")
        endBatch(s0, e0)
        lastGlide = null; autoTrailAt = if (ok && selStart >= 0) selStart else -1; typedTail = ""
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
        typedTail = ""
        if (old.isEmpty() || word.isEmpty()) return false
        settle()
        val k = before.length - before.trimEnd(' ').length
        val core = before.dropLast(k)
        if (k > 2 || !core.endsWith(old, ignoreCase = true)) return false
        val shown = core.takeLast(old.length)
        val n = old.length + k
        if (!verifyBefore(before.takeLast(n))) return false
        val w = written(if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (shown.first().isUpperCase()) word.replaceFirstChar { it.uppercase() } else word)
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
        endComposing(); lastGlide = null; keepTight = -1; autoTrailAt = -1; typedTail = ""
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

    /** The word the cursor is in or touches: [text] as written and [left], how many of its letters are before the cursor (the rest, [right], are after it). */
    class WordSpan(val text: String, val left: Int) { val right: Int get() = text.length - left }

    /**
     * R18a: the whole word at the cursor, wherever the cursor is in it (before its first letter, inside it, after its last): what a tap on an old word means. Null when something is selected, when the cursor is not
     * at a word, when the run of letters touches a digit or an @ (a code, an address), when the window may cut it, or when it is not 2 to 24 letters.
     */
    fun wordAround(): WordSpan? {
        if (selStart >= 0 && selStart != selEnd) return null
        var i = before.length
        while (i > 0 && isWordChar(before[i - 1])) i--
        if (i == 0 && before.length >= WINDOW_BEFORE) return null
        if (i > 0 && (before[i - 1].isDigit() || before[i - 1] == '@')) return null
        var j = 0
        while (j < after.length && isWordChar(after[j])) j++
        if (j == after.length && after.length >= WINDOW_AFTER) return null
        if (j < after.length && (after[j].isDigit() || after[j] == '@')) return null
        val l = before.substring(i).trimStart('\'')                      // (a leading apostrophe is a quote)
        val r = after.substring(0, j).trimEnd('\'')
        if (r.isEmpty() && l.endsWith("'")) return null
        val w = l + r
        if (w.length !in 2..24 || w.none { it.isLetter() }) return null
        return WordSpan(w, l.length)
    }
    private fun isWordChar(c: Char) = c.isLetter() || c == '\''

    /** (the second last word, the last word, the next word) around [span], lowercase: the language's context for the word. A neighbour is null when it is not a plain word or the window cuts it. */
    fun contextAround(span: WordSpan): Triple<String?, String?, String?> {
        val (p2, p1) = twoBefore(before.dropLast(span.left))
        val tail = after.drop(span.right)
        val t2 = tail.trimStart(' ')
        val gap = tail.length - t2.length
        val tok = t2.takeWhile { isWordChar(it) }
        val next = if (gap in 1..3 && tok.length in 1..24 && !(tok.length == t2.length && after.length >= WINDOW_AFTER)) tok.lowercase() else null      // (not a word the window may have cut)
        return Triple(p2, p1, next)
    }

    /**
     * R18a: the word at the cursor ([wordAround]) is replaced by [word] where it stands: no space is added and the cursor ends after it. Verified against the editor first (what is removed is really there);
     * false (nothing changed) when there is no word, the editor shows something else, or it refuses.
     */
    fun replaceWordAround(word: String): Boolean {
        typedTail = ""
        if (word.isEmpty()) return false
        settle()
        endComposing()
        val span = wordAround() ?: return false
        val l = span.left; val r = span.right
        if (ed.textBefore(l) != before.takeLast(l) || ed.textAfter(r) != after.take(r)) return false
        val s0 = selStart; val e0 = selEnd
        beginBatch()
        var ok = ed.deleteSurrounding(l, r)
        if (ok) { before = before.dropLast(l); moveSel(-l); after = after.drop(r); ok = commit(word) }
        endBatch(s0, e0)
        lastGlide = null
        if (!ok) { resync(); return false }
        after = ed.textAfter(WINDOW_AFTER) ?: after
        stats.swaps++
        ensureWindow()
        autoCaps()
        return true
    }

    /** (the second last, the last) plain word BEFORE the word [word] that ends at the cursor (as [wordBeforeCursor] gave it): the language's context for it. */
    fun contextBefore(word: String): Pair<String?, String?> =
        if (before.endsWith(word)) twoBefore(before.dropLast(word.length)) else null to null

    /** The word before the cursor in the window, lowercase, for the language (null when there is none or it is not a plain word). */
    fun previousWord(): String? = wordEndingAt(before)

    /** The plain word that [text] ends with (a trailing space is skipped), lowercase; null when it ends in something else. */
    fun wordEndingAt(text: String): String? {
        val tok = afterPaste(text).trimEnd().substringAfterLast(' ').trimStart { it in OPENING_MARKS }       // (R21: a word just inside a quote or a bracket is still a word: "hello and (hello)
        if (tok.isEmpty() || tok.length > 24 || !tok.all { it.isLetter() || it == '\'' }) return null
        return tok.lowercase()
    }

    /** (the second last word, the last word) before the cursor, lowercase: the language's context for the next glide. Both null when the last thing is not a plain word; the first null when only one word precedes. */
    fun previousTwo(): Pair<String?, String?> = twoBefore(before)

    /**
     * R23b: the part of [text] that stands after the last paste whose end is in it (a paste is not the language the person is writing: the words before it are no context for the words after it). A text with no paste in it is
     * returned whole.
     */
    private fun afterPaste(text: String): String {
        var cut = -1
        for (t in pastedTails) { val i = text.lastIndexOf(t); if (i >= 0 && i + t.length > cut) cut = i + t.length }
        return if (cut > 0) text.substring(cut) else text
    }

    /**
     * R23b: the word before the cursor when ALL of it was typed on this keyboard, key by key (a typo fixed with Backspace on the way is still typed); null when any of it came from elsewhere: a paste, an app, a glide, a strip
     * word, a word the cursor was moved to. This is the only kind of word tapping ever teaches the keyboard.
     */
    fun typedWord(): String? {
        val w = wordBeforeCursor() ?: return null
        return if (typedTail.endsWith(w)) w else null
    }

    /** R23b: [word] stands inside one of the last pastes (so a correction of it is a correction of the clipboard's text, which teaches nothing). */
    fun wasPasted(word: String): Boolean = word.isNotEmpty() && pastedTails.any { it.contains(word, ignoreCase = true) }

    private fun twoBefore(text: String): Pair<String?, String?> {
        val toks = afterPaste(text).trimEnd().split(' ')
        fun plain(raw: String): String? { val t = raw.trimStart { it in OPENING_MARKS }; return if (t.isNotEmpty() && t.length <= 24 && t.all { it.isLetter() || it == '\'' }) t.lowercase() else null }       // (R21: skip an opening quote or bracket)
        val last = toks.lastOrNull()?.let { plain(it) } ?: return null to null
        val second = if (toks.size >= 2) plain(toks[toks.size - 2]) else null
        return second to last
    }

    // ------------------------------------------------------------------------------------------------------------ the space before a new word, and the capital after it (R19a, R19b)

    /** The space a word put in at the cursor now (a glide, a strip word) brings along: one unless the text already ends in space or an opening mark, and unless the owner took this very space away on purpose (R19b). */
    private fun leadForNewWord(): String {
        val last = before.lastOrNull() ?: return ""
        if (last.isWhitespace() || last in NO_SPACE_AFTER) return ""
        if (last in AUTO_SPACE_AFTER && tightHere()) return ""
        if (last == '"' && quoteOpens()) return ""                   // owner R21: no space between an opening quote and the word inside it
        return " "
    }

    /** The keyboard's own capitals apply here: the setting is on and this is a plain text box (the field's own request is [autoCaps]'s business). */
    private fun ownCapsApply() = autoCapsOn && field.kind == FieldKind.TEXT && (sentenceCaps || field.capsFlags != 0)

    /** The shift state a word starting after [lead] is written in: a full stop and the space that comes with the word make Shift once (owner R19a: "once i put a period, it should add a space and capitalize the next letter"). */
    private fun shiftWithLead(lead: String): ShiftState =
        if (shift == ShiftState.OFF && lead.isNotEmpty() && ownCapsApply() && startsSentence(before + lead, if (selStart >= 0) selStart + lead.length else -1)) ShiftState.ONCE else shift

    /** True when a word put in at the cursor now (typed, glided or picked) begins a sentence: the text before it ends a sentence, counting the space a glide or a strip word adds after a full stop that touches the word before. */
    fun nextWordStartsSentence(): Boolean {
        if (selStart >= 0 && selStart != selEnd) return false
        val lead = leadForNewWord()
        return startsSentence(before + lead, if (selStart >= 0) selStart + lead.length else -1)
    }

    private fun quoteOpens(): Boolean = quoteOpensAt(before)

    private fun insideQuotes(curly: Boolean): Boolean = insideQuotesAt(before, curly, before.length >= WINDOW_BEFORE)

    /**
     * A lone i (or i'm, i'll, i've, i'd) stands before the cursor as plain text and the key about to be typed ends the word: it becomes a capital. Only a whole word counts: it must begin the text, a line, or stand after a space or
     * an opening mark, never after a dot, a hyphen or a letter ("a.i.", "x-i", "wi-fi"). Verified against the editor first, like every replacement. True when it changed the text.
     */
    private fun fixLoneI(): Boolean {
        if (!pronounKind() || (selStart >= 0 && selStart != selEnd) || composing != null) return false
        val w = wordBeforeCursor() ?: return false
        if (!isPronounI(w) || w[0] != 'i') return false
        val at = before.length - w.length
        val prev = if (at > 0) before[at - 1] else null
        if (prev != null && !prev.isWhitespace() && prev !in PRONOUN_OPENERS) return false
        if (prev == null && before.length >= WINDOW_BEFORE) return false
        val fixed = pronounI(w); val s0 = selStart; val e0 = selEnd
        beginBatch()
        var ok = verifyBefore(w) && ed.deleteSurrounding(w.length, 0)
        if (ok) { before = before.dropLast(w.length); moveSel(-w.length); ok = commit(fixed) }
        endBatch(s0, e0)
        if (!ok) { resync(); return false }
        return true
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
        /** Closing quotes and brackets: a sentence can end inside them ("Stop." He said). */
        private const val CLOSERS = "\"'\u201D\u2019)]}\u00BB"
        /** Words that end in a full stop without ending the sentence (compared in lowercase). */
        private val ABBREVIATIONS = setOf("e.g.", "i.e.", "mr.", "mrs.", "ms.", "dr.", "vs.", "prof.", "st.", "jr.", "sr.", "inc.", "ltd.", "approx.", "cf.", "fig.", "dept.")

        /**
         * True when the next letter typed after [text] (the text before the cursor, [selStart] the cursor's offset or -1) starts a sentence: the box is empty, the text ends with a newline, or it ends with . ! ? or an
         * ellipsis (inside closing quotes or brackets too) followed by a space. Not after an abbreviation (e.g. i.e. Mr. Dr. vs.) or an initial (J.), and not when the full stop is not followed by a space yet (3.5, www.x).
         */
        fun startsSentence(text: String, selStart: Int = -1): Boolean {
            if (text.isBlank()) return (text.isEmpty() && selStart <= 0) || (text.isNotEmpty() && text.length < WINDOW_BEFORE)
            if (text.endsWith("\n")) return true                                // a new line or paragraph
            val trimmed = text.trimEnd(' ', '\t')
            if (trimmed.length == text.length) return false                    // nothing after the punctuation yet
            if (trimmed.endsWith("\n")) return true
            val t = trimmed.trimEnd { it in CLOSERS }
            if (t.isEmpty()) return false
            val last = t.last()
            if (last != '.' && last != '!' && last != '?' && last != '\u2026') return false
            if (last == '.') {
                val raw = t.substringAfterLast(' ').substringAfterLast('\n').substringAfterLast('\t').trimStart { it in "\"'\u201C\u2018([{" }
                val tok = raw.lowercase()
                if (raw == "I." || raw == "i.") return true                   // the pronoun ends a sentence ("so did I."); any other single letter is an initial
                if (tok in ABBREVIATIONS) return false
                val parts = tok.split('.').filter { it.isNotEmpty() }
                if (parts.isNotEmpty() && parts.all { it.length == 1 && it[0].isLetter() }) return false          // an initial (j.) or dotted letters (u.s., a.m.)
            }
            return true
        }

        /** Punctuation after which a letter that follows at once needs a space (when the person has that option on). */
        private const val AUTO_SPACE_AFTER_CHARS = ".,!?;:"

        /**
         * [before] (the text before the cursor) ends in sentence punctuation that touches a word or a number, and that word is not part of an address, an abbreviation or a number: a letter typed now needs a space in front of it
         * ("hello." "w" -> "hello. w"). Pure, shared with the small keyboard of the Kestrel browser ([app.kestrel.core.input.MiniKeyboardModel]).
         */
        fun wantsSpaceBefore(before: String): Boolean {
            val n = before.length
            if (n < 2) return false
            val p = before[n - 1]
            if (p !in AUTO_SPACE_AFTER_CHARS) return false
            val q = before[n - 2]
            if (!(q.isLetterOrDigit() || q in ")]\"\u201D\u2019'")) return false
            val tok = before.substringAfterLast(' ').substringAfterLast('\n')
            if (tok.any { it == '@' || it == '/' || it == '\\' }) return false
            if (p == '.') { val core = tok.dropLast(1); if ((core.length <= 1 && core != "I" && core != "i") || core.contains('.')) return false }   // e.g, U.S, example.com, a.m (but "so did I." ends a sentence)
            if (p == ':' && tok.length > 1 && tok.dropLast(1).all { it.isDigit() }) return false           // 10:30
            return true
        }

        /** The straight quote that ends [text] OPENS a quotation: it stands at the start of the text, or after a space, a newline, an opening bracket or a dash (not after a word or a closing mark). */
        fun quoteOpensAt(text: String): Boolean {
            val n = text.length
            if (n == 0 || text[n - 1] != '"') return false
            if (n == 1) return true
            val prev = text[n - 2]
            return prev.isWhitespace() || prev in "([{\u2014\u2013"
        }

        /**
         * [text] ends inside a pair of quotes that was opened within it: its last quote mark is an opening one ([curly]: an opening \u201C that no closing \u201D follows). A straight quote opens when it stands at the start of the text
         * (known only when the text is not [cut] off at its start) or after a space, a newline, an opening bracket or a dash. Unknown counts as "not inside".
         */
        fun insideQuotesAt(text: String, curly: Boolean, cut: Boolean): Boolean {
            if (curly) return text.lastIndexOf('\u201C') > text.lastIndexOf('\u201D')
            val i = text.lastIndexOf('"')
            if (i < 0) return false
            if (i == 0) return !cut
            val prev = text[i - 1]
            return prev.isWhitespace() || prev in "([{\u2014\u2013"
        }

        /** Marks that open a quotation or a bracket: a word right after one is still a word for the language's context. */
        private const val OPENING_MARKS = "\"\u201C\u2018([{"
        private val PRONOUN_ENDINGS = setOf("m", "ll", "ve", "d")
        /** The marks that may stand right before a pronoun i ("(i think", "\"i said"). */
        private const val PRONOUN_OPENERS = "([{\"\u201C\u2018\u00AB\u00A1"
        /** The punctuation that ends a word the way a space does (R20c). */
        const val PRONOUN_BOUNDARY = ".,!?;:"
        /** i, i'm, i'll, i've, i'd: the words that are always written with a capital I (R20c). */
        fun isPronounI(w: String): Boolean = w == "i" || (w.length in 3..4 && w[0] == 'i' && (w[1] == '\'' || w[1] == '\u2019') && w.substring(2) in PRONOUN_ENDINGS)
        fun pronounI(w: String): String = if (isPronounI(w)) "I" + w.substring(1) else w

        /** How much of what was typed is remembered as typed (a few words: enough to take a finished word back). */
        private const val TYPED_TAIL_MAX = 120
        /** How much of the end of a paste is remembered to tell where it stood. */
        private const val PASTE_TAIL = 30
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
