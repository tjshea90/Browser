package app.kestrel.core.filter

/**
 * Procedural cosmetic filters (AD1b-4): the subset of uBlock Origin's "extended CSS" that real lists use most and that a content script can run safely.
 *
 *     site.example##div[role="feed"] > div:has-text(Sponsored)
 *     site.example##.card:has-text(/promoted/i):upward(2)
 *     site.example##div.overlay:has-text(Subscribe):remove()
 *
 * A rule is a normal CSS selector (the base, checked by [CosmeticSelectors.accept] like any other) followed by operators applied in order to every element the base matches:
 *  - `:has-text(text)` / `:has-text(/regex/flags)` (aliases `:contains(`, `:-abp-contains(`): keeps the element when its text holds the text (plain text is case-sensitive),
 *  - `:upward(n)` / `:upward(selector)`: moves to the n-th parent / the closest parent that matches the selector,
 *  - an action, last: `:remove()` removes the element, `:remove-attr(name)` / `:remove-class(name)` change it; without an action the element is hidden.
 * Everything else (`:xpath`, `:matches-css`, `:others`, selectors after an operator, rules for every site) is refused: the rule is skipped, never half applied.
 *
 * Stored form, kept in the SITE group of the domain next to the plain selectors (no format change: [CosmeticSelectors.toCss] skips it, the extension asks for these rules separately):
 * `PREFIX base (OP name ARG arg)*` with the control characters below, which can never occur in an accepted selector or argument. Names: `t` has-text, `u` upward, `r` remove, `a` remove-attr, `c` remove-class.
 */
internal object ProceduralRules {
    /** First character of a stored procedural rule. */
    const val PREFIX = '\u0002'
    const val OP = '\u0003'
    const val ARG = '\u0004'
    /** In the CSS of a host that has procedural rules (a comment: harmless to a browser that does not know it); the page script then asks for them. */
    const val MARKER = "/*kp*/"

    private const val MAX_OPS = 4
    private const val MAX_TEXT = 120
    private const val MAX_REGEX = 160

    private val OPS = arrayOf(":has-text(", ":contains(", ":-abp-contains(", ":upward(", ":remove(", ":remove-attr(", ":remove-class(")
    private val NAME = Regex("[A-Za-z_][-A-Za-z0-9_:.]{0,40}")
    private val REGEX_FLAGS = Regex("[gimsuy]{0,5}")
    private const val REGEX_ESCAPES = "dDwWsSbB.()[]{}\\/+*?$^|-"

    fun isStored(s: String): Boolean = s.isNotEmpty() && s[0] == PREFIX

    class Op(val name: String, val arg: String)
    class Rule(val base: String, val ops: List<Op>)

    /** The stored form of a procedural selector, or null when it is not one, uses anything outside the subset, or is unsafe. */
    fun encode(selector: String): String? {
        val low = selector.lowercase()
        var first = -1
        for (o in OPS) { val i = low.indexOf(o); if (i >= 0 && (first < 0 || i < first)) first = i }
        if (first <= 0) return null                                               // no operator, or no base selector in front of it
        val base = selector.substring(0, first).trim()
        if (base.isEmpty() || !CosmeticSelectors.accept(base)) return null
        val ops = ArrayList<Op>(2)
        var p = first
        while (p < selector.length) {
            if (selector[p] != ':') return null
            val open = selector.indexOf('(', p)
            if (open < 0) return null
            val name = selector.substring(p + 1, open).lowercase()
            val close = matchParen(selector, open)
            if (close < 0) return null
            val arg = selector.substring(open + 1, close)
            val op = makeOp(name, arg) ?: return null
            if (ops.size >= MAX_OPS) return null
            if (ops.isNotEmpty() && isAction(ops.last().name)) return null          // an action ends the chain
            ops.add(op)
            p = close + 1
        }
        if (ops.isEmpty()) return null
        if (ops.all { it.name == "u" }) return null                                 // :upward alone selects nothing to decide on
        val sb = StringBuilder(selector.length + 16)
        sb.append(PREFIX).append(base)
        for (o in ops) sb.append(OP).append(o.name).append(ARG).append(o.arg)
        return sb.toString()
    }

    fun decode(stored: String): Rule? {
        if (!isStored(stored)) return null
        val parts = stored.substring(1).split(OP)
        val ops = ArrayList<Op>(parts.size - 1)
        for (i in 1 until parts.size) {
            val j = parts[i].indexOf(ARG)
            if (j < 0) return null
            ops.add(Op(parts[i].substring(0, j), parts[i].substring(j + 1)))
        }
        return Rule(parts[0], ops)
    }

    private fun isAction(name: String) = name == "r" || name == "a" || name == "c"

    private fun makeOp(name: String, arg: String): Op? = when (name) {
        "has-text", "contains", "-abp-contains" -> textArg(arg)?.let { Op("t", it) }
        "upward" -> upwardArg(arg)?.let { Op("u", it) }
        "remove" -> if (arg.isEmpty()) Op("r", "") else null
        "remove-attr" -> if (NAME.matches(arg)) Op("a", arg) else null
        "remove-class" -> if (NAME.matches(arg)) Op("c", arg) else null
        else -> null
    }

    /** Plain text (any printable characters except the ones the stored form uses) or a `/regex/flags` made of ordinary regex syntax. */
    private fun textArg(arg: String): String? {
        if (arg.isEmpty()) return null
        for (c in arg) if (c.code < 0x20 || c.code == 0x7f) return null
        if (arg.startsWith("/") && arg.length >= 3 && arg.lastIndexOf('/') > 0) {
            val end = arg.lastIndexOf('/')
            val src = arg.substring(1, end)
            if (src.isEmpty() || src.length > MAX_REGEX || !REGEX_FLAGS.matches(arg.substring(end + 1))) return null
            var i = 0
            while (i < src.length) {
                val c = src[i]
                if (c == '\\') { if (i + 1 >= src.length || REGEX_ESCAPES.indexOf(src[i + 1]) < 0) return null; i += 2; continue }
                if (c == '<' || c == '`') return null
                i++
            }
            try { java.util.regex.Pattern.compile(src) } catch (e: Exception) { return null }
            return arg
        }
        return if (arg.length <= MAX_TEXT && arg.indexOf('\\') < 0) arg else null
    }

    private fun upwardArg(arg: String): String? {
        val n = arg.trim().toIntOrNull()
        if (n != null) return if (n in 1..10) n.toString() else null
        return if (arg.length <= 100 && CosmeticSelectors.accept(arg.trim())) arg.trim() else null
    }

    /** Index of the `)` that closes the `(` at [open]; quotes and backslash escapes inside are skipped (a regex may hold parentheses); -1 when it never closes. */
    private fun matchParen(s: String, open: Int): Int {
        var depth = 0
        var quote = 0.toChar()
        var i = open
        while (i < s.length) {
            val c = s[i]
            if (c == '\\') { i += 2; continue }
            if (quote != 0.toChar()) { if (c == quote) quote = 0.toChar() }
            else if (c == '"' || c == '\'') quote = c
            else if (c == '(') depth++
            else if (c == ')') { depth--; if (depth == 0) return i }
            i++
        }
        return -1
    }
}
