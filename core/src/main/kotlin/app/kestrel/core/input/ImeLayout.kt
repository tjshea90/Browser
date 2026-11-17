package app.kestrel.core.input

import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions

enum class Page { LETTERS, SYMBOLS }

/**
 * The keys of the SYSTEM keyboard (the browser's MiniKeyboardLayout is for an address bar and stays as the engine's reference layout). Rows 1-3 of the
 * letters page are the browser's own, so the glide decoder sees the same geometry; only the bottom row changes with the field (text, URL, e-mail) and
 * number / phone fields get a keypad. The action key ([Key.Go]) is labelled for what Enter does in the field (KB-104 decides which action).
 */
object ImeLayout {
    private fun ch(s: String, w: Float = 1f) = KeySpec(s, Key.Ch(s), w)
    private fun chars(s: String): List<KeySpec> = s.map { ch(it.toString()) }
    /**
     * The globe (switch keyboard). Owner, 2026-11-17: "remove the bottom left button to change keyboards": it is OFF by default now (Settings > Layout brings it back, and the crash-loop safe mode always shows it, plan 12 KB-106);
     * without it, HOLDING the space bar opens the keyboard picker. Its label is only a name; the view draws a globe.
     */
    const val GLOBE = "globe"
    private val globe = KeySpec(GLOBE, Key.NextKeyboard, 1f)
    private val back = KeySpec("⌫", Key.Backspace, 1.5f)
    private fun action(label: String, w: Float) = KeySpec(label, Key.Go, w)

    /** The label of the action key: what Enter does in the field right now. */
    fun actionLabel(imeAction: Int, noEnterAction: Boolean): String {
        if (noEnterAction) return "⏎"
        return when (imeAction) {
            ImeActions.GO -> "Go"
            ImeActions.SEARCH -> "Search"
            ImeActions.SEND -> "Send"
            ImeActions.NEXT -> "Next"
            ImeActions.PREVIOUS -> "Prev"
            ImeActions.DONE -> "Done"
            else -> "⏎"
        }
    }

    /** The number row is a little thinner than the letter rows (it is only ever tapped, never glided over). */
    const val NUMBER_ROW_WEIGHT = 0.72f

    /** Row heights for [MiniKeyboardGeometry.boxes]: pass the SAME list to the view and to the decoder. */
    fun rowWeights(rows: List<List<KeySpec>>): List<Float> = if (hasNumberRow(rows)) List(rows.size) { if (it == 0) NUMBER_ROW_WEIGHT else 1f } else List(rows.size) { 1f }
    fun hasNumberRow(rows: List<List<KeySpec>>): Boolean = rows.size == 5

    /**
     * The symbols of the "123" page, also offered as a grid when the full stop or the comma is HELD (see KeysView). (The owner asked to remove the 123 key and then took it back: "i need a way to get to the special
     * symbols": the key and the page stay, the grid is a shortcut.)
     */
    private const val SYMBOL_ROW_3 = "<>[]{}\\|\u20AC\u00A3\u00A5"
    val symbolRows: List<List<String>> = listOf("@#$%&-+()/", "*\"':;!?_=~", SYMBOL_ROW_3).map { r -> r.map { it.toString() } }

    /**
     * Every text row is this many units wide. (Owner, 2026-11-17, M1: the keyboard is modelled on Gboard's large layout: Backspace at the end of the first letter row, the action key at the end of the home row,
     * a Shift key at both ends of the bottom letter row, and under it ONLY comma, space, full stop and the key for special characters at the bottom right: no globe, no emoji, no voice key, no bar under the space bar.)
     */
    const val ROW_UNITS = 11.5f

    fun rows(page: Page, shift: Boolean, kind: FieldKind, actionLabel: String = "⏎", numberRow: Boolean = true, showGlobe: Boolean = false): List<List<KeySpec>> {
        if (kind == FieldKind.NUMBER || kind == FieldKind.PHONE) return keypad(kind, actionLabel, showGlobe)
        val digits = if (numberRow) listOf("1234567890".map { ch(it.toString(), ROW_UNITS / 10f) }) else emptyList()
        fun letters(s: String) = chars(if (shift) s.uppercase() else s)
        // the bottom row: [globe if asked] [one or two small keys] [space: ALL the rest] [full stop, .com] [special characters / abc, bottom right]
        fun bottom(vararg fixed: KeySpec, last: KeySpec): List<KeySpec> {
            val g = if (showGlobe) listOf(globe) else emptyList()
            val used = (g + fixed + last).sumOf { it.weight.toDouble() }.toFloat()
            val space = KeySpec("space", Key.Ch(" "), ROW_UNITS - used)
            val head = fixed.takeWhile { it.label != "." && it.label != ".com" }
            val tail = fixed.drop(head.size)
            return g + head + space + tail + last
        }
        fun act() = action(actionLabel, 2f)
        if (page == Page.SYMBOLS) return digits + listOf(
            chars("@#$%&-+()/") + KeySpec(back.label, back.key, 1.5f),
            chars("*\"':;!?_=~") + action(actionLabel, 1.5f),
            listOf(KeySpec("", null, 0.25f)) + chars(SYMBOL_ROW_3) + KeySpec("", null, 0.25f),
            bottom(ch(",", 1.5f), ch(".", 1.5f), last = KeySpec("abc", Key.ToLetters, 2.5f)),
        )
        val toSymbols = KeySpec("?123", Key.ToSymbols, 2.5f)
        val bottomRow = when (kind) {
            FieldKind.URL -> bottom(ch("/", 1.25f), ch(".", 1.25f), ch(".com", 1.75f), last = toSymbols)
            FieldKind.EMAIL -> bottom(ch("@", 1.25f), ch(".", 1.25f), ch(".com", 1.75f), last = toSymbols)
            else -> bottom(ch(",", 1.5f), ch(".", 1.5f), last = toSymbols)
        }
        return digits + listOf(
            letters("qwertyuiop") + back,
            listOf(KeySpec("", null, 0.5f)) + letters("asdfghjkl") + act(),
            listOf(KeySpec("⇧", Key.Shift, 2.25f)) + letters("zxcvbnm") + KeySpec("⇧", Key.Shift, 2.25f),
            bottomRow,
        )
    }

    private fun keypad(kind: FieldKind, actionLabel: String, showGlobe: Boolean): List<List<KeySpec>> {
        val phone = kind == FieldKind.PHONE
        val g = if (showGlobe) listOf(KeySpec(GLOBE, Key.NextKeyboard, 1f)) else emptyList()
        return listOf(
            listOf(ch("1"), ch("2"), ch("3"), KeySpec("⌫", Key.Backspace, 1f)),
            listOf(ch("4"), ch("5"), ch("6"), ch(if (phone) "+" else "-")),
            listOf(ch("7"), ch("8"), ch("9"), ch(if (phone) "#" else ".")),
            if (showGlobe) g + listOf(ch("0"), if (phone) ch(",") else KeySpec("space", Key.Ch(" "), 1f), action(actionLabel, 1f))
            else listOf(ch("0", if (phone) 1.5f else 2f), if (phone) ch(",", 1f) else KeySpec("space", Key.Ch(" "), 1f), action(actionLabel, if (phone) 1.5f else 1f)),
        )
    }
}
