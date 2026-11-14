package app.kestrel.core.input

/**
 * What changed between two states of the text the mini keyboard builds when everything happens at the END of it (a letter typed, Backspace, a glided word, a guess swapped for another): how many
 * characters to take off the end and what to put there instead (AU3: the same edit is sent to the page's own text field, which keeps whatever came before).
 */
class TextEdit(val remove: Int, val insert: String) {
    val isEmpty: Boolean get() = remove == 0 && insert.isEmpty()

    companion object {
        /** The edit that turns [old] into [new]. A pair of surrogate halves (an emoji) is never split. */
        fun between(old: String, new: String): TextEdit {
            val n = minOf(old.length, new.length)
            var c = 0
            while (c < n && old[c] == new[c]) c++
            if (c > 0 && c < old.length && Character.isHighSurrogate(old[c - 1]) && Character.isLowSurrogate(old[c])) c--
            else if (c > 0 && c < new.length && Character.isHighSurrogate(new[c - 1]) && Character.isLowSurrogate(new[c])) c--
            return TextEdit(old.length - c, new.substring(c))
        }
    }
}
