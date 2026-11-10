package app.kestrel.core.url

/** Safe embedding of arbitrary text (filter-list CSS, URLs) in a JavaScript string literal for evaluateJavascript. */
object JsEscape {
    /** Returns [s] as a double-quoted JS string literal. Escapes everything that could end the literal or the script. */
    @JvmStatic
    fun quote(s: String): String {
        val sb = StringBuilder(s.length + 16)
        sb.append('"')
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c == ' ' -> sb.append("\\u2028") // line terminators are illegal inside JS string literals
                c == ' ' -> sb.append("\\u2029")
                c == '<' && i + 1 < s.length && s[i + 1] == '/' -> sb.append("<\\") // "<" + "\" here; the "/" is appended next iteration: never emits "</script" / "</style"
                c < ' ' -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
            i++
        }
        return sb.append('"').toString()
    }
}
