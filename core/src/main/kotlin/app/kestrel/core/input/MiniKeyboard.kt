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

    /** The four rows of [symbols] (false = letters); with [shift] the letters are upper case. */
    fun rows(symbols: Boolean, shift: Boolean = false): List<List<KeySpec>> {
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
            listOf(KeySpec("123", Key.ToSymbols, 1.5f), slash, dot, space, dotCom, go),
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

    fun open(current: String) {
        text = current.take(MAX_CHARS)
        allSelected = text.isNotEmpty()
        symbols = false
        shift = false
    }

    fun press(key: Key): KeyResult {
        when (key) {
            is Key.Ch -> {
                val t = if (shift && key.text.length == 1) key.text.uppercase() else key.text
                text = if (allSelected) t else (text + t).take(MAX_CHARS)
                allSelected = false
                shift = false
            }
            Key.Backspace -> { text = if (allSelected) "" else text.dropLast(1); allSelected = false }
            Key.Shift -> shift = !shift
            Key.ToSymbols -> { symbols = true; shift = false }
            Key.ToLetters -> symbols = false
            Key.Go -> { val t = text.trim(); if (t.isNotEmpty()) return KeyResult.Submit(t) }
        }
        return KeyResult.None
    }

    companion object { const val MAX_CHARS = 2048 }
}
