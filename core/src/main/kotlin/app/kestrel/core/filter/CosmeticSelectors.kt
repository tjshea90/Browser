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
    /** Forbidden anywhere, even inside quotes. */
    private val FORBIDDEN = arrayOf("\\", "<", "url(", "@import", "/*", "expression(", "javascript:", "image-set(", "src(")

    /** Forbidden outside quoted strings only: inside `[style^="width:100%;"]` a semicolon is data, not syntax. */
    private const val STRUCTURAL = "{};"

    // ---- `site##selector:style(declarations)` (AA3-3b): list authors restyle instead of hiding (scroll unlocks such as `body:style(overflow: auto !important)`, and "unbreak" fixes that undo an over-hiding rule).
    // Stored in the site's selector group as "selector" + STYLE_SEP + "declarations" (a control character never occurs in an accepted selector) and written out as `selector{declarations}` by [toCss].

    /** Separates the selector from the declarations of a restyling rule inside a stored site entry. */
    const val STYLE_SEP = '\u0001'

    /** `sel:style(decls)` -> (sel, decls); null when the text has no `:style(` suffix. */
    fun splitStyle(selector: String): Pair<String, String>? {
        val i = selector.lastIndexOf(":style(")
        if (i <= 0 || !selector.endsWith(")")) return null
        return Pair(selector.substring(0, i).trim(), selector.substring(i + 7, selector.length - 1))
    }

    private val STYLE_FORBIDDEN = arrayOf("\\", "<", "url(", "@", "/*", "expression(", "javascript:", "image-set(", "src(", "{", "}", "\"", "'", "`", "behavior", "-moz-binding")

    /**
     * The declarations of a `:style()` rule if they are safe, as `prop:value;prop:value`; null otherwise. Lists are third-party input that ends up in a `<style>` element: no braces, quotes, escapes,
     * url(), at-rules or comments, plain lower-case property names, short values made of ordinary characters, balanced parentheses.
     */
    fun acceptDeclarations(raw: String): String? {
        val d = raw.trim()
        if (d.isEmpty() || d.length > 300) return null
        for (c in d) if (c.code < 0x20 || c.code == 0x7f) return null
        val low = d.lowercase()
        for (f in STYLE_FORBIDDEN) if (low.contains(f)) return null
        val out = ArrayList<String>(4)
        for (part in d.split(';')) {
            val p = part.trim()
            if (p.isEmpty()) continue
            val colon = p.indexOf(':')
            if (colon <= 0) return null
            val name = p.substring(0, colon).trim().lowercase()
            val value = p.substring(colon + 1).trim()
            if (name.isEmpty() || name.length > 40 || !name[0].isLetter() && name[0] != '-' || name.any { !(it in 'a'..'z' || it == '-' || it in '0'..'9') }) return null
            if (value.isEmpty() || value.length > 120) return null
            var depth = 0
            for (c in value) {
                val ok = c.isLetterOrDigit() || c == ' ' || c == '#' || c == '%' || c == '.' || c == ',' || c == '!' || c == '-' || c == '_' || c == '/' || c == '+' || c == '*' || c == '(' || c == ')'
                if (!ok) return null
                if (c == '(') depth++ else if (c == ')') { depth--; if (depth < 0) return null }
            }
            if (depth != 0) return null
            out.add("$name:$value")
        }
        return if (out.isEmpty()) null else out.joinToString(";")
    }

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

    /**
     * One pass with quote state: brackets/parens must balance, quotes must close, and the structural characters
     * `{ } ;` may appear only INSIDE quotes. (Backslashes are rejected globally, so a quote can never be escaped and
     * this scanner's idea of "inside a string" always agrees with the CSS parser's.)
     */
    private fun balanced(s: String): Boolean {
        // A stack, not two counters: "[(])" has equal counts but is misnested, and one invalid selector makes the browser drop
        // the WHOLE rule that groups it (or, for some shapes, the rest of the stylesheet).
        val stack = CharArray(s.length)
        var depth = 0
        var quote = 0.toChar()
        for (c in s) {
            if (quote != 0.toChar()) { if (c == quote) quote = 0.toChar(); continue }
            if (STRUCTURAL.indexOf(c) >= 0 || c == '@') return false // '@' outside quotes is only ever an at-rule
            when (c) {
                '"', '\'' -> quote = c
                '[' -> stack[depth++] = ']'
                '(' -> stack[depth++] = ')'
                ']', ')' -> { if (depth == 0 || stack[--depth] != c) return false }
            }
        }
        return quote == 0.toChar() && depth == 0
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
            val sep = sel.indexOf(STYLE_SEP)
            if (sep >= 0) { // a restyling rule: its own `selector{declarations}`, never grouped
                if (inGroup > 0) { sb.append("{display:none!important}\n"); inGroup = 0 }
                sb.append(sel, 0, sep).append('{').append(sel, sep + 1, sel.length).append("}\n")
                continue
            }
            if (chunk <= 1 || needsIsolation(sel)) {
                // an isolated selector between two grouped ones: close the open group FIRST. (It used to be written straight after the group's last selector, which fused the two into one compound
                // selector that matches nothing, and left the NEXT group starting with a comma, which makes the browser drop that whole rule.)
                if (inGroup > 0) { sb.append("{display:none!important}\n"); inGroup = 0 }
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
