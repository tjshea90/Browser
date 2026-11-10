package app.kestrel.core.filter

/**
 * Cosmetic (element-hiding) selector hygiene. Filter lists are third-party input that ends up inside a `<style>` element
 * on every page, so selectors are validated against a conservative grammar before they are ever stored:
 * no braces/semicolons/escapes (CSS-injection and exfiltration tricks), no url()/@import, balanced brackets and quotes, and
 * no procedural extensions (those are not CSS and would silently invalidate a whole rule in the browser).
 */
internal object CosmeticSelectors {
    private val PROCEDURAL = arrayOf(
        ":has-text(", ":contains(", ":-abp-", ":xpath(", ":matches-", ":min-text-length(", ":nth-ancestor(", ":others(",
        ":upward(", ":watch-attr(", ":remove(", ":style(", ":if(", ":if-not(", ":not-has(", ":has-text", ":-ext-",
    )
    private val FORBIDDEN = arrayOf("{", "}", ";", "\\", "<", "url(", "@import", "/*", "expression(", "javascript:", "image-set(", "src(")

    fun isProcedural(selector: String): Boolean {
        val low = selector.lowercase()
        for (p in PROCEDURAL) if (low.contains(p)) return true
        return false
    }

    fun accept(selector: String): Boolean {
        if (selector.isEmpty() || selector.length > 300) return false
        for (c in selector) if (c.code < 0x20 || c.code == 0x7f) return false
        val low = selector.lowercase()
        for (f in FORBIDDEN) if (low.contains(f)) return false
        for (p in PROCEDURAL) if (low.contains(p)) return false
        val first = selector[0]
        if (first == '>' || first == '+' || first == '~' || first == ',') return false
        return balanced(selector)
    }

    private fun balanced(s: String): Boolean {
        var square = 0
        var paren = 0
        var quote = 0.toChar()
        for (c in s) {
            if (quote != 0.toChar()) { if (c == quote) quote = 0.toChar(); continue }
            when (c) {
                '"', '\'' -> quote = c
                '[' -> square++
                ']' -> { square--; if (square < 0) return false }
                '(' -> paren++
                ')' -> { paren--; if (paren < 0) return false }
            }
        }
        return quote == 0.toChar() && square == 0 && paren == 0
    }

    /** Modern pseudo-classes (`:has()`, `:is()`, ...) get a rule of their own so an old WebView can't void a whole group. */
    fun needsIsolation(selector: String): Boolean = selector.indexOf('(') >= 0

    /**
     * CSS text hiding [selectors]. Selectors needing isolation, and every selector when [chunk] is 1, get their own rule;
     * the rest are grouped [chunk] at a time.
     */
    fun toCss(selectors: List<String>, chunk: Int): String {
        if (selectors.isEmpty()) return ""
        val sb = StringBuilder(selectors.size * 40)
        var inGroup = 0
        for (sel in selectors) {
            if (chunk <= 1 || needsIsolation(sel)) {
                sb.append(sel).append("{display:none!important}\n")
            } else {
                if (inGroup > 0) sb.append(',')
                sb.append(sel)
                inGroup++
                if (inGroup >= chunk) { sb.append("{display:none!important}\n"); inGroup = 0 }
            }
        }
        if (inGroup > 0) sb.append("{display:none!important}\n")
        return sb.toString()
    }
}
