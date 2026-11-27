package app.kestrel.core.input

import app.kestrel.core.input.editor.InputController

/**
 * The app's own small keyboard for the address (AR2, owner message 32: "a small floating keyboard ... at the top right of the screen so I can type in the next url ... using my right hand thumb at the top of
 * the tablet"). Android's own keyboard cannot be moved by an app, so the browser draws one; this file is what a key DOES to the text and what keys there are, with no drawing in it so that it can be tested.
 */
sealed class Key {
    /** A letter, a digit, a symbol, a space or the ready-made ".com": [text] is what it adds. */
    class Ch(val text: String) : Key()
    object Backspace : Key()
    object Shift : Key()
    object ToSymbols : Key()
    object ToLetters : Key()
    object Go : Key()
    /** Switches to the next keyboard (the globe key of a system keyboard). Never typed, never shown by the browser's address-bar keyboard. */
    object NextKeyboard : Key()
}

/** One key of a row as drawn: [weight] is its width in units (a row adds up to [MiniKeyboardLayout.ROW_UNITS]); a null [key] is only a gap. */
class KeySpec(val label: String, val key: Key?, val weight: Float)

object MiniKeyboardLayout {
    const val ROW_UNITS = 10f

    private fun chars(s: String): List<KeySpec> = s.map { KeySpec(it.toString(), Key.Ch(it.toString()), 1f) }
    private val slash = KeySpec("/", Key.Ch("/"), 1f)
    private val dot = KeySpec(".", Key.Ch("."), 1f)
    private val dotCom = KeySpec(".com", Key.Ch(".com"), 1.5f)
    private val space = KeySpec("space", Key.Ch(" "), 3f)
    private val go = KeySpec("Go", Key.Go, 2f)
    private val back = KeySpec("⌫", Key.Backspace, 1.5f)

    private val comma = KeySpec(",", Key.Ch(","), 1f)
    private val apostrophe = KeySpec("'", Key.Ch("'"), 1f)
    private val question = KeySpec("?", Key.Ch("?"), 1f)
    private val spaceShort = KeySpec("space", Key.Ch(" "), 2.5f)

    /** The four rows of [symbols] (false = letters); with [shift] the letters are upper case; [forField] (typing into a page's text field) puts a comma, an apostrophe and a question mark on the letters page where the address keys (slash and .com) are for the address bar. */
    fun rows(symbols: Boolean, shift: Boolean = false, forField: Boolean = false): List<List<KeySpec>> {
        if (symbols) return listOf(
            chars("1234567890"),
            chars("-_:?=&%#@+"),
            chars("~!*'(),;") + KeySpec("⌫", Key.Backspace, 2f),
            listOf(KeySpec("abc", Key.ToLetters, 1.5f), slash, dot, space, dotCom, go),
        )
        fun letters(s: String) = chars(if (shift) s.uppercase() else s)
        return listOf(
            letters("qwertyuiop"),
            listOf(KeySpec("", null, 0.5f)) + letters("asdfghjkl") + KeySpec("", null, 0.5f),
            listOf(KeySpec("⇧", Key.Shift, 1.5f)) + letters("zxcvbnm") + back,
            if (forField) listOf(KeySpec("123", Key.ToSymbols, 1.5f), comma, dot, spaceShort, apostrophe, question, go)
            else listOf(KeySpec("123", Key.ToSymbols, 1.5f), slash, dot, space, dotCom, go),
        )
    }
}

/** What a key press asks the host to do besides changing the text. */
sealed class KeyResult {
    object None : KeyResult()
    /** Open [text] (an address, or words to search for). */
    class Submit(val text: String) : KeyResult()
}

/**
 * The text being typed and the keyboard's state. [open] starts a session with the address that was in the bar and everything SELECTED: the first key typed replaces it, Backspace clears it (the way
 * the normal address bar behaves when it is pressed).
 */
class MiniKeyboardModel {
    var text = ""; private set
    /**
     * Where typing goes in [text] (AY3, owner message 39: "make the navigation panel keyboard space key able to move the text cursor if I press and hold it then move my finger left or right"): at the end
     * until [moveCursor] puts it elsewhere. Typing, Backspace and glided words all work at the cursor.
     */
    var cursor = 0; private set
    var allSelected = false; private set
    var symbols = false; private set
    var shift = false; private set
    /** Caps lock (a double tap on Shift): every letter and glided word comes out in capitals until Shift is tapped again. */
    var shiftLock = false; private set
    /** What the last glide added (a space before it when one was needed, the word, a space after it): ONE Backspace takes all of it away, and a candidate from the strip can replace it. */
    private var glideTail: String? = null
    /** The words glides put in (in order): they were learned when they were kept, so [typedPairs] leaves them out. */
    private val glided = ArrayList<String>()
    /** True while the last thing that happened was a glide (the candidate strip is showing for it). */
    val glideActive: Boolean get() = glideTail != null

    /**
     * Owner R22 (2026-11-18, "put all the updates into the kestrel keyboard"): the text habits the Swoop keyboard has, for typing SENTENCES into a web page's text field. Off (the default) for the address bar, where nothing
     * may be added or capitalised. On: Shift comes on by itself after a sentence end (not at the start of a box: what the page field already holds is unknown); a lone i and i'm, i'll, i've, i'd become a capital I; a glided word
     * after a full stop gets its space and its capital; no spaces between a quotation mark and the word inside it (the same rules as [InputController]); a letter right after , . ! ? ; : gets its space (a space the person
     * deletes on purpose is not put back). The rules are the ones of [InputController], shared as pure functions there.
     */
    var smartText = false
    private var autoShifted = false
    /** The cursor offset where the person took away the space after punctuation with Backspace: no automatic space is put in there again. -1 = none. */
    private var keepTight = -1

    fun open(current: String) {
        text = current.take(MAX_CHARS)
        cursor = text.length
        allSelected = text.isNotEmpty()
        symbols = false
        shift = false; shiftLock = false
        glideTail = null
        glided.clear()
        autoShifted = false; keepTight = -1
    }

    /** Shift was tapped twice quickly: caps lock on. */
    fun lockShift() { shiftLock = true; shift = true }

    /** The text before the cursor and the text after it. */
    private fun before() = text.substring(0, cursor)
    private fun after() = text.substring(cursor)

    fun press(key: Key): KeyResult {
        when (key) {
            is Key.Ch -> {
                if (smartText && !allSelected && key.text.length == 1) smartBefore(key.text[0])
                val t = if (shift && key.text.length == 1) key.text.uppercase() else key.text
                if (allSelected) { text = t; cursor = t.length }
                else {
                    // right after a glide the word has a space after it: a full stop, a comma, a slash, ".com" ... closes the word instead of standing after the space ("doraby" ".com" = "doraby.com"); a closing quote too (R22)
                    val b = before()
                    val ownTrail = glideTail?.endsWith(" ") == true && b.endsWith(" ")
                    val closingQuote = smartText && ownTrail && ((key.text == "\"" && InputController.insideQuotesAt(b, false, false)) || (key.text == "\u201D" && InputController.insideQuotesAt(b, true, false)))
                    val glideSpace = (ownTrail && key.text in CLOSES_WORD) || closingQuote
                    val head = if (glideSpace) b.dropLast(1) else b
                    text = (head + t + after()).take(MAX_CHARS)
                    cursor = (head.length + t.length).coerceAtMost(text.length)
                }
                allSelected = false
                shift = shiftLock
                glideTail = null
                // Owner, 2026-11-17 (M5, like Gboard): after ONE special character the keyboard goes straight back to the letters (digits stay: several are typed in a row; Backspace and Shift change nothing)
                if (symbols && !(key.text.length == 1 && key.text[0].isDigit())) symbols = false
                autoShift()
            }
            Key.Backspace -> {
                val tail = glideTail
                val b = before()
                val whole = tail != null && !allSelected && b.endsWith(tail)
                if (whole && glided.isNotEmpty()) glided.removeAt(glided.size - 1)
                if (allSelected) { text = ""; cursor = 0 }
                else if (whole) { val nb = b.removeSuffix(tail!!); text = nb + after(); cursor = nb.length }
                else if (cursor > 0) {
                    val nb = b.dropLast(1)
                    if (smartText && b.last() == ' ' && nb.isNotEmpty() && nb.last() in PUNCT_SPACE) keepTight = nb.length          // R22: a space taken away on purpose after punctuation stays away
                    text = nb + after(); cursor = nb.length
                }
                allSelected = false
                glideTail = null
                autoShift()
            }
            Key.Shift -> { autoShifted = false; if (shiftLock) { shiftLock = false; shift = false } else shift = !shift }
            Key.ToSymbols -> { symbols = true; shift = shiftLock; glideTail = null }
            Key.ToLetters -> { symbols = false; autoShift() }                                         // (R22: back on the letters, a sentence start asks for its capital again)
            Key.NextKeyboard -> {}                                                                   // (only the system keyboard has it)
            Key.Go -> { val t = text.trim(); if (t.isNotEmpty()) return KeyResult.Submit(t) }
        }
        return KeyResult.None
    }

    /**
     * Owner R23a (held Backspace, shared with the Kestrel keyboard): after a long hold every repeat takes a WHOLE word: the spaces before the cursor and the word (letters, digits, apostrophes) in front of them, or one mark and
     * the spaces before it. A selected text, a glide just made (as one Backspace does) or nothing before the cursor: one plain Backspace.
     */
    fun deleteWord() {
        if (allSelected || cursor <= 0 || glideTail != null) { press(Key.Backspace); return }
        val b = before(); var i = b.length
        while (i > 0 && b[i - 1].isWhitespace()) i--
        if (i > 0) {
            if (b[i - 1].isLetterOrDigit() || b[i - 1] == '\'') { while (i > 0 && (b[i - 1].isLetterOrDigit() || b[i - 1] == '\'')) i-- }
            else i -= if (i >= 2 && Character.isLowSurrogate(b[i - 1]) && Character.isHighSurrogate(b[i - 2])) 2 else 1
        }
        if (b.length - i <= 0) { press(Key.Backspace); return }
        text = b.substring(0, i) + after(); cursor = i
        keepTight = -1; glideTail = null
        autoShift()
    }

    /**
     * The cursor moves [delta] characters (negative = left; one step of the finger sliding over the space key is one character). A selected address is first collapsed to the end the finger went towards;
     * the glide that was just typed stops being "the last thing" (Backspace takes one character from now on). True when something changed.
     */
    fun moveCursor(delta: Int): Boolean {
        if (delta == 0) return false
        if (allSelected) { allSelected = false; cursor = if (delta < 0) 0 else text.length; glideTail = null; return true }
        var n = (cursor.toLong() + delta).coerceIn(0L, text.length.toLong()).toInt()
        // never between the two halves of one character (an emoji is two chars)
        if (n in 1 until text.length && Character.isHighSurrogate(text[n - 1]) && Character.isLowSurrogate(text[n])) n += if (delta < 0) -1 else 1
        if (n == cursor) return false
        cursor = n
        glideTail = null; keepTight = -1
        autoShift()
        return true
    }

    /** The page's own text field took the caret somewhere else (the cursor was moved there): what was typed so far no longer sits before it, so the keyboard starts from an empty text again. */
    fun restartText() { text = ""; cursor = 0; allSelected = false; glideTail = null; glided.clear(); keepTight = -1; autoShift() }

    /** A glide ended on [word]: it goes in at the cursor, with a space after it (and one before it when the text does not end in one there); the first thing typed replaces a selected address. Shift makes it start with a capital. */
    fun commitGlide(word: String) {
        if (word.isEmpty()) return
        val base = if (allSelected) "" else before()
        val rest = if (allSelected) "" else after()
        val lead = if (smartText) smartLead(base) else (if (base.isNotEmpty() && !base.endsWith(' ')) " " else "")
        val capital = shift || (smartText && lead.isNotEmpty() && !allSelected && InputController.startsSentence(base + lead, base.length + lead.length))     // R22: the word after a full stop starts a sentence
        val w0 = if (shiftLock) word.uppercase() else if (capital) word.replaceFirstChar { it.uppercase() } else word
        val w = if (smartText) InputController.pronounI(w0) else w0
        val tail = lead + w + " "
        if (base.length + tail.length + rest.length > MAX_CHARS) return
        text = base + tail + rest
        cursor = base.length + tail.length
        glideTail = tail
        glided.add(w.lowercase())
        allSelected = false
        shift = shiftLock; keepTight = -1
        autoShift()
    }

    /**
     * A word from the strip that is not a replacement of the glided word (R22: the predicted NEXT word) goes in at the cursor with a space after it, and one before it when the text does not end in one (the same rules as a glide for
     * the space and the capital). It is not a glide: one Backspace takes one character. False when it does not fit.
     */
    fun insertWord(word: String): Boolean {
        if (word.isEmpty()) return false
        val base = if (allSelected) "" else before()
        val rest = if (allSelected) "" else after()
        val lead = if (smartText) smartLead(base) else (if (base.isNotEmpty() && !base.endsWith(' ')) " " else "")
        val capital = shift || (smartText && lead.isNotEmpty() && !allSelected && InputController.startsSentence(base + lead, base.length + lead.length))
        val w0 = if (shiftLock) word.uppercase() else if (capital) word.replaceFirstChar { it.uppercase() } else word
        val w = if (smartText) InputController.pronounI(w0) else w0
        val tail = lead + w + " "
        if (base.length + tail.length + rest.length > MAX_CHARS) return false
        text = base + tail + rest
        cursor = base.length + tail.length
        glideTail = null; allSelected = false
        shift = shiftLock; keepTight = -1
        autoShift()
        return true
    }

    /** The candidate [word] from the strip replaces the word the last glide put in. False when the last thing was not a glide. */
    fun swapGlide(word: String): Boolean {
        val tail = glideTail ?: return false
        val b = before()
        if (!b.endsWith(tail)) { glideTail = null; return false }
        val lead = if (tail.startsWith(" ")) " " else ""
        val shown = tail.trim()
        val capital = shown.firstOrNull()?.isUpperCase() == true
        val w0 = if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (capital) word.replaceFirstChar { it.uppercase() } else word
        val w = if (smartText) InputController.pronounI(w0) else w0
        val newTail = lead + w + " "
        val nb = b.removeSuffix(tail) + newTail
        text = nb + after()
        cursor = nb.length
        glideTail = newTail
        if (glided.isNotEmpty()) glided[glided.size - 1] = w.lowercase()
        return true
    }

    /** The word before the place a glide would go (the cursor), lowercase, for the language (null when there is none or it is not a plain word: an address, a number). */
    fun previousWord(): String? {
        if (allSelected) return null
        val tok = before().trimEnd().substringAfterLast(' ')
        if (tok.isEmpty() || tok.length > 24 || !tok.all { it.isLetter() || it == '\'' }) return null
        return tok.lowercase()
    }

    /** The two words before the place a glide would go, (second last, last), lowercase: the language's context. Both null when the last thing is not a plain word; the first null when only one word precedes. */
    fun previousTwo(): Pair<String?, String?> {
        if (allSelected) return null to null
        val toks = before().trimEnd().split(' ')
        fun plain(t: String): String? = if (t.isNotEmpty() && t.length <= 24 && t.all { it.isLetter() || it == '\'' }) t.lowercase() else null
        val last = toks.lastOrNull()?.let { plain(it) } ?: return null to null
        val second = if (toks.size >= 2) plain(toks[toks.size - 2]) else null
        return second to last
    }

    /** (the second last word, the word before, the word) for every plain word of the text that was typed letter by letter: what the person writes, with the last two words as the context for it. */
    fun typedContexts(): List<Triple<String?, String?, String>> {
        val glidedLeft = HashMap<String, Int>()
        for (g in glided) glidedLeft[g] = (glidedLeft[g] ?: 0) + 1
        val out = ArrayList<Triple<String?, String?, String>>()
        var prev: String? = null; var prev2: String? = null
        for (tok in text.split(' ')) {
            val w = tok.lowercase()
            if (w.length !in 2..24 || !w.all { it in 'a'..'z' || it == '\'' } || w.startsWith("'") || w.endsWith("'")) { prev = null; prev2 = null; continue }
            val left = glidedLeft[w]
            if (left != null && left > 0) { glidedLeft[w] = left - 1; prev2 = prev; prev = w; continue }
            out.add(Triple(prev2, prev, w)); prev2 = prev; prev = w
        }
        return out
    }

    /** (the word before, the word) for every plain word of the text that was typed letter by letter: what the person writes, for the keyboard to learn (glided words were learned when they were kept). */
    fun typedPairs(): List<Pair<String?, String>> {
        val glidedLeft = HashMap<String, Int>()
        for (g in glided) glidedLeft[g] = (glidedLeft[g] ?: 0) + 1
        val out = ArrayList<Pair<String?, String>>()
        var prev: String? = null
        for (tok in text.split(' ')) {
            val w = tok.lowercase()
            if (w.length !in 2..24 || !w.all { it in 'a'..'z' || it == '\'' } || w.startsWith("'") || w.endsWith("'")) { prev = null; continue }
            val left = glidedLeft[w]
            if (left != null && left > 0) { glidedLeft[w] = left - 1; prev = w; continue }
            out.add(prev to w); prev = w
        }
        return out
    }

    /** Plain words in what is typed (for the words the keyboard remembers): letters only, three to 24 of them. */
    fun plainWords(): List<String> = text.split(' ').map { it.lowercase() }.filter { w -> w.length in 3..24 && w.all { it in 'a'..'z' } }

    // ------------------------------------------------------------------ smart text (R22): only while [smartText] is on

    /** Before a single typed character [c] goes in: a lone i before a word boundary becomes I, and a letter right after punctuation gets its space. */
    private fun smartBefore(c: Char) {
        if (c == ' ' || c in InputController.PRONOUN_BOUNDARY) fixLoneI()
        if (c.isLetter() && keepTight != cursor && InputController.wantsSpaceBefore(before())) {
            text = (before() + " " + after()).take(MAX_CHARS); cursor = (cursor + 1).coerceAtMost(text.length); keepTight = -1
            autoShift()
        }
    }

    /** The word before the cursor is a lone i, i'm, i'll, i've or i'd that stands after a space, an opening mark or the start of the text: it becomes a capital I (the length does not change). */
    private fun fixLoneI() {
        val b = before(); var i = b.length
        while (i > 0 && (b[i - 1].isLetter() || b[i - 1] == '\'')) i--
        val w = b.substring(i)
        if (w.isEmpty() || w[0] != 'i' || !InputController.isPronounI(w)) return
        val prev = if (i > 0) b[i - 1] else null
        if (prev != null && !prev.isWhitespace() && prev !in "([{\"\u201C\u2018\u00AB\u00A1") return
        text = b.substring(0, i) + InputController.pronounI(w) + after()
    }

    /** The space a glided word brings in front of itself: none after a space, an opening mark or an opening quote, and none where the person deleted it on purpose. */
    private fun smartLead(base: String): String {
        val last = base.lastOrNull() ?: return ""
        if (last.isWhitespace() || last in "([{\u201C\u2018/@#") return ""
        if (last == '"' && InputController.quoteOpensAt(base)) return ""
        if (last in PUNCT_SPACE && keepTight == base.length) return ""
        return " "
    }

    /** Shift comes on by itself where a sentence starts (after . ! ? and a space, or a new line; never at the start of the text, which is not known to be the start of the box) and goes off by itself where it no longer does. */
    private fun autoShift() {
        if (!smartText || shiftLock || allSelected || symbols) return
        val b = before()
        val want = b.isNotBlank() && InputController.startsSentence(b, cursor)
        if (want) { if (!shift) { shift = true; autoShifted = true } } else if (autoShifted && shift) { shift = false; autoShifted = false }
    }

    companion object {
        const val MAX_CHARS = 2048
        private const val PUNCT_SPACE = ".,!?;:"
        /** Keys that join the word before them: right after a glide they take away the space the glide added. */
        val CLOSES_WORD: Set<String> = setOf(".", ",", "?", "!", ":", ";", "/", ".com", ")", "'", "-", "_", "@", "%")
    }
}
