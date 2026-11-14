package app.kestrel.core.input

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

    fun open(current: String) {
        text = current.take(MAX_CHARS)
        allSelected = text.isNotEmpty()
        symbols = false
        shift = false; shiftLock = false
        glideTail = null
        glided.clear()
    }

    /** Shift was tapped twice quickly: caps lock on. */
    fun lockShift() { shiftLock = true; shift = true }

    fun press(key: Key): KeyResult {
        when (key) {
            is Key.Ch -> {
                val t = if (shift && key.text.length == 1) key.text.uppercase() else key.text
                // right after a glide the word has a space after it: a full stop, a comma, a slash, ".com" ... closes the word instead of standing after the space ("doraby" ".com" = "doraby.com")
                val glideSpace = !allSelected && glideTail?.endsWith(" ") == true && text.endsWith(" ") && key.text in CLOSES_WORD
                text = if (allSelected) t else ((if (glideSpace) text.dropLast(1) else text) + t).take(MAX_CHARS)
                allSelected = false
                shift = shiftLock
                glideTail = null
            }
            Key.Backspace -> {
                val tail = glideTail
                val whole = tail != null && !allSelected && text.endsWith(tail)
                if (whole && glided.isNotEmpty()) glided.removeAt(glided.size - 1)
                text = if (allSelected) "" else if (whole) text.removeSuffix(tail!!) else text.dropLast(1)
                allSelected = false
                glideTail = null
            }
            Key.Shift -> if (shiftLock) { shiftLock = false; shift = false } else shift = !shift
            Key.ToSymbols -> { symbols = true; shift = shiftLock; glideTail = null }
            Key.ToLetters -> symbols = false
            Key.Go -> { val t = text.trim(); if (t.isNotEmpty()) return KeyResult.Submit(t) }
        }
        return KeyResult.None
    }

    /** A glide ended on [word]: it goes in after what is there, with a space after it (and one before it when the text does not end in one); the first thing typed replaces a selected address. Shift makes it start with a capital. */
    fun commitGlide(word: String) {
        if (word.isEmpty()) return
        val w = if (shiftLock) word.uppercase() else if (shift) word.replaceFirstChar { it.uppercase() } else word
        val base = if (allSelected) "" else text
        val tail = (if (base.isNotEmpty() && !base.endsWith(' ')) " " else "") + w + " "
        if (base.length + tail.length > MAX_CHARS) return
        text = base + tail
        glideTail = tail
        glided.add(w.lowercase())
        allSelected = false
        shift = shiftLock
    }

    /** The candidate [word] from the strip replaces the word the last glide put in. False when the last thing was not a glide. */
    fun swapGlide(word: String): Boolean {
        val tail = glideTail ?: return false
        if (!text.endsWith(tail)) { glideTail = null; return false }
        val lead = if (tail.startsWith(" ")) " " else ""
        val shown = tail.trim()
        val capital = shown.firstOrNull()?.isUpperCase() == true
        val w = if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (capital) word.replaceFirstChar { it.uppercase() } else word
        val newTail = lead + w + " "
        text = text.removeSuffix(tail) + newTail
        glideTail = newTail
        if (glided.isNotEmpty()) glided[glided.size - 1] = w.lowercase()
        return true
    }

    /** The word before the place a glide would go, lowercase, for the language (null when there is none or it is not a plain word: an address, a number). */
    fun previousWord(): String? {
        if (allSelected) return null
        val tok = text.trimEnd().substringAfterLast(' ')
        if (tok.isEmpty() || tok.length > 24 || !tok.all { it.isLetter() || it == '\'' }) return null
        return tok.lowercase()
    }

    /** The two words before the place a glide would go, (second last, last), lowercase: the language's context. Both null when the last thing is not a plain word; the first null when only one word precedes. */
    fun previousTwo(): Pair<String?, String?> {
        if (allSelected) return null to null
        val toks = text.trimEnd().split(' ')
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

    companion object {
        const val MAX_CHARS = 2048
        /** Keys that join the word before them: right after a glide they take away the space the glide added. */
        val CLOSES_WORD: Set<String> = setOf(".", ",", "?", "!", ":", ";", "/", ".com", ")", "'", "-", "_", "@", "%")
    }
}
