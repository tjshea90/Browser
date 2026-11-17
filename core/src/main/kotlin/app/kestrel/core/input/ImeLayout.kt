package app.kestrel.core.input

import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions

/** The letters, the first page of symbols (digits and the everyday ones) and the second page of symbols (the rest). */
enum class Page { LETTERS, SYMBOLS, SYMBOLS2 }

/**
 * The keys of the SYSTEM keyboard (the browser's MiniKeyboardLayout is for an address bar and stays as the engine's reference layout).
 *
 * Owner, 2026-11-17, M1: "model the keyboard layout exactly like the attached photo (Gboard), except without the bottom bar, down arrow and globe (nothing under the space bar), and no emoji or voice button".
 * The photo, measured: ten letter keys across (a digit hint in the corner of each), the home row indented half a key at both ends, Shift (1.5 keys) at the left and Backspace (1.5 keys) at the right
 * of the bottom letter row, and under it ?123 (1.5, round, at the LEFT), comma, a very wide space bar, full stop and Enter (1.5, round, at the RIGHT). Every row is [ROW_UNITS] units wide, so a key
 * of one unit is exactly one letter key and the three letter rows share one key pitch (see [MiniKeyboardGeometry.slots]). No number row unless the person switches it on (the digits are the
 * hints on the top row, and a held top-row key types its digit).
 *
 * The action key ([Key.Go]) is labelled for what Enter does in the field (KB-104 decides which action). Number / phone fields get a keypad.
 */
object ImeLayout {
    private fun ch(s: String, w: Float = 1f) = KeySpec(s, Key.Ch(s), w)
    private fun chars(s: String): List<KeySpec> = s.map { ch(it.toString()) }
    private fun gap(w: Float) = KeySpec("", null, w)
    /**
     * The globe (switch keyboard). Owner, 2026-11-17: "remove the bottom left button to change keyboards": it is OFF by default (Settings > Layout brings it back, and the crash-loop safe mode always shows it, plan 12 KB-106);
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

    /** The number row (only when switched on in Settings) is a little thinner than the letter rows (it is only ever tapped, never glided over). */
    const val NUMBER_ROW_WEIGHT = 0.72f

    /** Row heights for [MiniKeyboardGeometry.slots]: pass the SAME list to the view and to the decoder. */
    fun rowWeights(rows: List<List<KeySpec>>): List<Float> = if (hasNumberRow(rows)) List(rows.size) { if (it == 0) NUMBER_ROW_WEIGHT else 1f } else List(rows.size) { 1f }
    fun hasNumberRow(rows: List<List<KeySpec>>): Boolean = rows.size == 5

    /**
     * The symbols of the hold-the-full-stop grid (the quick way to a special character: hold "." or ",", slide to the symbol, let go). Every symbol of the two symbols pages is in it.
     */
    private const val SYMBOL_ROW_3 = "<>[]{}\\|€£¥"
    val symbolRows: List<List<String>> = listOf("@#$%&-+()/", "*\"':;!?_=~", SYMBOL_ROW_3).map { r -> r.map { it.toString() } }

    /** The key on the first symbols page that opens the second one (Gboard writes it "=\<"). On the second page the same key (labelled ?123) goes back. Both carry [Key.ToSymbols]: the view cycles the symbols pages. */
    const val MORE_SYMBOLS = "=\\<"

    /** Every text row is this many units wide: the photo's ten letter keys. */
    const val ROW_UNITS = 10f

    /** The width of the round keys (?123 / abc at the left, Enter at the right of the bottom row) and of Shift and Backspace: the photo has them 1.5 letter keys wide. */
    const val WIDE_KEY = 1.5f

    fun rows(page: Page, shift: Boolean, kind: FieldKind, actionLabel: String = "⏎", numberRow: Boolean = false, showGlobe: Boolean = false): List<List<KeySpec>> {
        if (kind == FieldKind.NUMBER || kind == FieldKind.PHONE) return keypad(kind, actionLabel, showGlobe)
        val digits = if (numberRow) listOf(chars("1234567890")) else emptyList()
        fun letters(s: String) = chars(if (shift) s.uppercase() else s)
        // the bottom row, as in the photo: [?123 / abc, round] [globe if asked] [small keys] [space: ALL the rest] [small keys] [action key, round]
        fun bottom(first: KeySpec, before: List<KeySpec>, after: List<KeySpec>): List<KeySpec> {
            val g = if (showGlobe) listOf(globe) else emptyList()
            val act = action(actionLabel, WIDE_KEY)
            val used = (listOf(first) + g + before + after + act).sumOf { it.weight.toDouble() }.toFloat()
            return listOf(first) + g + before + KeySpec("space", Key.Ch(" "), ROW_UNITS - used) + after + act
        }
        val toLetters = KeySpec("abc", Key.ToLetters, WIDE_KEY)
        if (page != Page.LETTERS) {
            val stdBottom = bottom(toLetters, listOf(ch(",")), listOf(ch(".")))
            // The pages are four rows tall like the letters page (the keyboard must not change height when the page changes); a number row, when it is on, sits above all of them.
            val body = if (page == Page.SYMBOLS) {
                if (numberRow) listOf(
                    chars("@#$%&-+()/"),
                    chars("*\"':;!?_=~"),
                    listOf(KeySpec(MORE_SYMBOLS, Key.ToSymbols, WIDE_KEY)) + chars("<>[]{}|") + back,
                    stdBottom,
                ) else listOf(
                    chars("1234567890"),
                    chars("@#$%&-+()/"),
                    listOf(KeySpec(MORE_SYMBOLS, Key.ToSymbols, WIDE_KEY)) + chars("*\"':;!?") + back,
                    stdBottom,
                )
            } else listOf(
                chars("~`|•√π÷×¶∆"),
                chars("£¢€¥^°={}\\"),
                listOf(KeySpec("?123", Key.ToSymbols, WIDE_KEY)) + chars("_<>[]©®") + back,
                stdBottom,
            )
            return digits + body
        }
        val toSymbols = KeySpec("?123", Key.ToSymbols, WIDE_KEY)
        val bottomRow = when (kind) {
            FieldKind.URL -> bottom(toSymbols, listOf(ch("/")), listOf(ch("."), ch(".com", WIDE_KEY)))
            FieldKind.EMAIL -> bottom(toSymbols, listOf(ch("@")), listOf(ch("."), ch(".com", WIDE_KEY)))
            else -> bottom(toSymbols, listOf(ch(",")), listOf(ch(".")))
        }
        return digits + listOf(
            letters("qwertyuiop"),
            listOf(gap(0.5f)) + letters("asdfghjkl") + gap(0.5f),
            listOf(KeySpec("⇧", Key.Shift, WIDE_KEY)) + letters("zxcvbnm") + back,
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
