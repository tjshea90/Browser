package app.kestrel.core.filter

/**
 * The index keys of element-hiding selectors (D2, research/ADBLOCK_ENGINE.md).
 *
 * The lists carry ~29,000 GENERIC selectors (they apply on every site). Injecting them all as one style sheet would cost a few MB and a few hundred milliseconds of style parsing on every page on this
 * tablet, so only the first 1,200 were ever used. Almost all of them name a class or an id (`.ad-banner`, `#sponsor`, `div.promo > a`): such a selector can match only on a page that HAS an element
 * with that class or id. So each selector is filed under one class/id it needs, the page tells the app which classes and ids it contains, and only the selectors that can possibly match are injected.
 *
 * Soundness (a selector is never withheld from a page it could match on): the key is a class or id that the selector REQUIRES. Class and id tokens inside `[...]` (attribute values), `(...)`
 * (`:not(.x)`, `:is(.a,.b)`, `:has(.c)` hold tokens that are NOT required, or not of the subject) and quotes are therefore never used, and a selector LIST (`.a, .b`: either may match) is not indexed
 * at all. Keys are lower-cased: in a quirks-mode document class and id names are matched without regard to case, and a lower-cased key on both sides can only ever return too much, never too little.
 */
internal object CosmeticTokens {
    /** Longest class/id the page may send, and the longest one that is indexed. */
    const val MAX_LEN = 80

    /** Every top-level class/id token of [selector] as ".class" / "#id", lower-cased, in order, without repeats; empty when it has none or is a selector list. */
    fun keysOf(selector: String): List<String> {
        var out: ArrayList<String>? = null
        var br = 0; var par = 0
        var quote = 0.toChar()
        var i = 0
        val n = selector.length
        while (i < n) {
            val c = selector[i]
            if (quote != 0.toChar()) { if (c == quote) quote = 0.toChar(); i++; continue }
            when (c) {
                '"', '\'' -> quote = c
                '[' -> br++
                ']' -> if (br > 0) br--
                '(' -> par++
                ')' -> if (par > 0) par--
                ',' -> if (br == 0 && par == 0) return emptyList() // a selector list: not indexed by one token
                '.', '#' -> if (br == 0 && par == 0) {
                    var j = i + 1
                    while (j < n && isIdent(selector[j])) j++
                    if (j > i + 1 && j - i - 1 <= MAX_LEN) {
                        val key = (c + selector.substring(i + 1, j)).lowercase()
                        if (out == null) out = ArrayList(2)
                        if (key !in out) out.add(key)
                    }
                    i = j - 1
                }
            }
            i++
        }
        return out ?: emptyList()
    }

    /** Characters of a CSS identifier (escapes never reach here: [CosmeticSelectors.accept] refuses a backslash). */
    private fun isIdent(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c.code >= 0x80

    /** The key a page must contain for the browser to need [tokens]-based generic CSS: only the form the page script sends (".class" / "#id", lower case, bounded). */
    fun normalise(token: String): String? {
        if (token.length < 2 || token.length > MAX_LEN + 1) return null
        val c = token[0]
        if (c != '.' && c != '#') return null
        for (k in 1 until token.length) if (!isIdent(token[k])) return null
        return token.lowercase()
    }
}
