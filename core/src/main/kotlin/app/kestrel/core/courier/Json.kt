package app.kestrel.core.courier

class JsonException(message: String) : Exception(message)

/**
 * The few JSON shapes the courier wire needs: objects ([LinkedHashMap]), arrays ([ArrayList]), strings, numbers (a [Long] when whole and in range, else a [Double]),
 * booleans and null. Strict on input (no comments, no trailing commas, no raw control characters in strings, no duplicate keys, a depth limit) because the bytes
 * come from the network. A request body is signature-checked before it is parsed, but this stays safe on its own. `:core` has no JSON library and `org.json` is not
 * on the JVM test classpath, so the wire format is handled here (research/PHONE_OFFLOAD.md section 5).
 */
object Json {
    fun parse(text: String, maxDepth: Int = 16): Any? {
        val p = Parser(text, maxDepth)
        p.skipWs()
        val v = p.value(0)
        p.skipWs()
        if (p.pos != text.length) p.fail("unexpected characters after the value")
        return v
    }

    fun write(v: Any?): String { val sb = StringBuilder(); put(sb, v, 0); return sb.toString() }

    private fun put(sb: StringBuilder, v: Any?, depth: Int) {
        require(depth <= 64) { "JSON value nested too deeply (a cycle?)" }
        when (v) {
            null -> sb.append("null")
            is String -> quote(sb, v)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Int, is Long, is Short, is Byte -> sb.append(v.toString())
            is Double -> { require(!v.isNaN() && !v.isInfinite()) { "not a JSON number: $v" }; sb.append(number(v)) }
            is Float -> { require(!v.isNaN() && !v.isInfinite()) { "not a JSON number: $v" }; sb.append(number(v.toDouble())) }
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    require(k is String) { "JSON keys must be strings" }
                    if (!first) sb.append(',')
                    first = false
                    quote(sb, k); sb.append(':'); put(sb, x, depth + 1)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (x in v) { if (!first) sb.append(','); first = false; put(sb, x, depth + 1) }
                sb.append(']')
            }
            else -> throw IllegalArgumentException("cannot write ${v.javaClass.name} as JSON")
        }
    }

    private fun number(d: Double): String = if (d == Math.floor(d) && Math.abs(d) < 1.0e15) d.toLong().toString() else d.toString()

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c == '\b' -> sb.append("\\b")
                c == '\u000C' -> sb.append("\\f")
                c < ' ' -> { sb.append("\\u00"); sb.append(HEX[c.code shr 4]); sb.append(HEX[c.code and 15]) }
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }

    private const val HEX = "0123456789abcdef"

    private class Parser(val s: String, val maxDepth: Int) {
        var pos = 0

        fun fail(msg: String): Nothing = throw JsonException("$msg (at $pos)")
        fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'

        fun skipWs() {
            while (pos < s.length) {
                val c = s[pos]
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++ else break
            }
        }

        fun value(depth: Int): Any? {
            if (depth > maxDepth) fail("nested too deeply")
            if (pos >= s.length) fail("unexpected end")
            val c = s[pos]
            return when {
                c == '{' -> obj(depth)
                c == '[' -> arr(depth)
                c == '"' -> str()
                c == 't' -> lit("true", true)
                c == 'f' -> lit("false", false)
                c == 'n' -> lit("null", null)
                c == '-' || c in '0'..'9' -> num()
                else -> fail("unexpected character")
            }
        }

        private fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, pos)) fail("bad literal")
            pos += word.length
            return v
        }

        private fun obj(depth: Int): Map<String, Any?> {
            pos++ // {
            val m = LinkedHashMap<String, Any?>()
            skipWs()
            if (peek() == '}') { pos++; return m }
            while (true) {
                skipWs()
                if (peek() != '"') fail("expected a key")
                val k = str()
                skipWs()
                if (peek() != ':') fail("expected ':'")
                pos++
                skipWs()
                val v = value(depth + 1)
                if (m.containsKey(k)) fail("duplicate key")
                m[k] = v
                skipWs()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return m }
                    else -> fail("expected ',' or '}'")
                }
            }
        }

        private fun arr(depth: Int): List<Any?> {
            pos++ // [
            val l = ArrayList<Any?>()
            skipWs()
            if (peek() == ']') { pos++; return l }
            while (true) {
                skipWs()
                l.add(value(depth + 1))
                skipWs()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return l }
                    else -> fail("expected ',' or ']'")
                }
            }
        }

        private fun str(): String {
            pos++ // opening quote
            var sb: StringBuilder? = null
            var start = pos
            while (true) {
                if (pos >= s.length) fail("unterminated string")
                val c = s[pos]
                if (c == '"') {
                    val r = if (sb == null) s.substring(start, pos) else sb.append(s, start, pos).toString()
                    pos++
                    return r
                }
                if (c < ' ') fail("raw control character in a string")
                if (c == '\\') {
                    if (sb == null) sb = StringBuilder()
                    sb.append(s, start, pos)
                    pos++
                    if (pos >= s.length) fail("unterminated escape")
                    when (s[pos]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 >= s.length) fail("short \\u escape")
                            var cp = 0
                            for (i in 1..4) {
                                val d = Character.digit(s[pos + i], 16)
                                if (d < 0) fail("bad \\u escape")
                                cp = cp * 16 + d
                            }
                            sb.append(cp.toChar())
                            pos += 4
                        }
                        else -> fail("bad escape")
                    }
                    pos++
                    start = pos
                } else pos++
            }
        }

        private fun num(): Any {
            val st = pos
            if (s[pos] == '-') pos++
            if (pos >= s.length) fail("bad number")
            if (s[pos] == '0') pos++
            else if (s[pos] in '1'..'9') { while (pos < s.length && s[pos] in '0'..'9') pos++ }
            else fail("bad number")
            var whole = true
            if (pos < s.length && s[pos] == '.') {
                whole = false; pos++
                val d = pos
                while (pos < s.length && s[pos] in '0'..'9') pos++
                if (pos == d) fail("bad number")
            }
            if (pos < s.length && (s[pos] == 'e' || s[pos] == 'E')) {
                whole = false; pos++
                if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
                val d = pos
                while (pos < s.length && s[pos] in '0'..'9') pos++
                if (pos == d) fail("bad number")
            }
            val t = s.substring(st, pos)
            if (whole) { val l = t.toLongOrNull(); if (l != null) return l }
            val d = t.toDouble()
            if (d.isNaN() || d.isInfinite()) fail("number out of range")
            return d
        }
    }
}

// Typed reads from a parsed object (null when the key is missing or has another type).
fun Map<*, *>.str(k: String): String? = this[k] as? String
fun Map<*, *>.long(k: String): Long? = when (val v = this[k]) {
    is Long -> v
    is Double -> if (v == Math.floor(v) && Math.abs(v) < 9.0e15) v.toLong() else null
    else -> null
}
fun Map<*, *>.int(k: String): Int? = long(k)?.takeIf { it >= Int.MIN_VALUE && it <= Int.MAX_VALUE }?.toInt()
fun Map<*, *>.bool(k: String): Boolean? = this[k] as? Boolean
fun Map<*, *>.obj(k: String): Map<*, *>? = this[k] as? Map<*, *>
fun Map<*, *>.list(k: String): List<*>? = this[k] as? List<*>
