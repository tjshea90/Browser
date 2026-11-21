package app.kestrel.core.input.glide

/**
 * Word -> emoji (owner: "if i type a word with a corresponding emoji available, put the emoji in the recommended words ... if i select an emoji this way, erase the written word and replace it with the emoji").
 * The list is a small text asset (one "word TAB emoji" per line, see ime/src/main/assets/emoji/LICENSE.txt). [forWord] knows plurals ("clowns" -> clown). Pure Kotlin: whether the phone's font can draw an emoji is the
 * caller's check ([forWord]'s [drawable]).
 */
class EmojiMap(text: String) {
    private val map = HashMap<String, String>()
    init { for (line in text.lineSequence()) { val t = line.indexOf('\t'); if (t > 0 && t < line.length - 1) map[line.substring(0, t)] = line.substring(t + 1).trim() } }
    val size: Int get() = map.size

    /** The emoji for [word] (any case), or null. [drawable] says whether the font can draw it. */
    fun forWord(word: String, drawable: (String) -> Boolean = { true }): String? {
        val w = word.lowercase()
        if (w.length < 3 || !w.all { it in 'a'..'z' }) return null
        val e = map[w] ?: (if (w.length > 3 && w.endsWith("s")) map[w.dropLast(1)] else null) ?: (if (w.length > 4 && w.endsWith("es")) map[w.dropLast(2)] else null)
        return if (e != null && drawable(e)) e else null
    }

    companion object {
        /** True for a string that is an emoji and not a word: no letter or digit in it, and something outside plain punctuation. */
        fun isEmoji(s: String): Boolean {
            if (s.isEmpty()) return false
            var any = false
            var i = 0
            while (i < s.length) { val cp = s.codePointAt(i); i += Character.charCount(cp); if (Character.isLetterOrDigit(cp)) return false; if (cp > 0x2000) any = true }
            return any
        }
    }
}
