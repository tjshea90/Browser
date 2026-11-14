package app.kestrel.core.filter

/**
 * Scriptlet rules (AW1b): the `site.example##+js(name, arg, arg)` lines of the uBlock Origin lists. A scriptlet is a small function that runs INSIDE the page before the page's own scripts
 * (anti-adblock defusers, pop-up killers, ad-script neutering, countdown skippers). The functions themselves are our own code (geckoapp/.../kestrel-shields/scriptlets-main.js, written from the
 * behaviour the public uBO wiki documents); this file only decides which lines are understood and how a call is stored and sent.
 *
 * A line is accepted when the name is one of ours (aliases folded to one canonical name), the number and kind of arguments fit that name, and nothing in it is a control character. Arguments follow
 * the lists' own syntax: separated by commas, optionally wrapped in quotes (a quoted argument may hold commas), `\,` is a comma. Everything else is skipped, never half applied.
 *
 * Stored form, kept in the SITE group of each domain next to the plain selectors (no format change; [CosmeticSelectors.toCss] and [ProceduralRules] ignore it):
 * `PREFIX name (SEP argument)*`. `site#@#+js(...)` is stored in the exception group in the same form (one call off for that site); `site#@#+js()` is stored as [ALL_STORED] (no scriptlet at all on that site).
 */
internal object ScriptletRules {
    /** First character of a stored scriptlet call. */
    const val PREFIX = '\u0005'
    const val SEP = '\u0006'
    /** `site#@#+js()`: every scriptlet is off for the site. */
    const val ALL_STORED = "\u0005*"

    /** In the CSS answer of a host that has scriptlets: a CSS comment that opens with these characters, holds the JSON of the calls and closes with [MARKER_END] (harmless to anything that does not know it). The page script cuts it out and hands the JSON to the page-world library. */
    const val MARKER_START = "/" + "*ks"
    const val MARKER_END = "*" + "/"

    const val MAX_ARGS = 10
    const val MAX_ARG = 500
    const val MAX_TOTAL = 1500
    /** A host's calls beyond these are dropped (a page that matches dozens of rules is a list gone wrong, and the payload rides on every answer). */
    const val MAX_CALLS = 40
    const val MAX_PAYLOAD = 16_000

    fun isStored(s: String): Boolean = s.isNotEmpty() && s[0] == PREFIX

    class Call(val name: String, val args: List<String>)

    private enum class Need { NONE, ONE, TWO, ONE_OF_TWO }

    /** canonical name -> how many arguments make the call meaningful. A call that would act on EVERYTHING when its arguments are empty (every listener, every timer, every fetch) must name something. */
    private val NEED = mapOf(
        "set" to Need.TWO,                       // chain, value
        "acs" to Need.ONE, "aopr" to Need.ONE, "aopw" to Need.ONE, "aost" to Need.ONE,
        "aeld" to Need.ONE_OF_TWO,               // type, pattern (a type or a pattern; both empty would defuse every listener)
        "nostif" to Need.ONE_OF_TWO, "nosiif" to Need.ONE_OF_TWO, // needle, delay
        "nano-stb" to Need.NONE, "nano-sib" to Need.NONE,          // an empty needle means "every timer that has this delay"
        "nowoif" to Need.NONE,                   // no argument: no window is opened by script at all (what the lists mean by it)
        "noeval" to Need.NONE, "noeval-if" to Need.ONE,
        "no-fetch-if" to Need.ONE, "no-xhr-if" to Need.ONE,
        "json-prune" to Need.ONE, "json-prune-fetch-response" to Need.ONE, "json-prune-xhr-response" to Need.ONE,
        "ra" to Need.ONE, "rc" to Need.ONE,
        "rmnt" to Need.TWO, "rpnt" to Need.TWO,  // node name, text
        "nowebrtc" to Need.NONE,
        "set-cookie" to Need.TWO, "trusted-set-cookie" to Need.TWO, "remove-cookie" to Need.ONE,
        "set-local-storage-item" to Need.TWO, "trusted-set-local-storage-item" to Need.TWO, "set-session-storage-item" to Need.TWO,
    )

    private val ALIAS = mapOf(
        "set-constant" to "set",
        "abort-current-script" to "acs", "abort-current-inline-script" to "acs", "acis" to "acs",
        "abort-on-property-read" to "aopr", "abort-on-property-write" to "aopw", "abort-on-stack-trace" to "aost",
        "addeventlistener-defuser" to "aeld",
        "no-settimeout-if" to "nostif", "settimeout-defuser" to "nostif", "no-setinterval-if" to "nosiif", "setinterval-defuser" to "nosiif",
        "adjust-settimeout" to "nano-stb", "adjust-setinterval" to "nano-sib",
        "no-window-open-if" to "nowoif", "window.open-defuser" to "nowoif",
        "silent-noeval" to "noeval", "no-eval" to "noeval",
        "remove-attr" to "ra", "remove-class" to "rc", "remove-node-text" to "rmnt", "replace-node-text" to "rpnt",
        "no-webrtc" to "nowebrtc", "cookie-remover" to "remove-cookie",
    )

    /** The canonical name for a name written in a list (case-insensitive, an old `.js` suffix allowed), or null when it is not one of ours. */
    fun canonical(written: String): String? {
        var n = written.trim().lowercase()
        if (n.endsWith(".js")) n = n.substring(0, n.length - 3)
        val c = ALIAS[n] ?: n
        return if (NEED.containsKey(c)) c else null
    }

    /** The arguments of a `+js(...)` call split the way the lists write them (see the class comment); null when the text cannot be split sensibly. The first element is the scriptlet's name. */
    fun splitArgs(s: String): List<String>? {
        val out = ArrayList<String>(4)
        val n = s.length
        var pos = 0
        while (true) {
            var b = pos
            while (b < n && (s[b] == ' ' || s[b] == '\t')) b++
            if (b < n && (s[b] == '"' || s[b] == '\'' || s[b] == '`')) {
                val q = s[b]
                var e = b + 1
                var found = -1
                while (e < n) {
                    val c = s[e]
                    if (c == '\\') { e += 2; continue }
                    if (c == q) {
                        var f = e + 1
                        while (f < n && (s[f] == ' ' || s[f] == '\t')) f++
                        if (f >= n || s[f] == ',') { found = e; break }
                    }
                    e++
                }
                if (found >= 0) {
                    val sb = StringBuilder(found - b)
                    var i = b + 1
                    while (i < found) { if (s[i] == '\\' && i + 1 < found && s[i + 1] == q) { sb.append(q); i += 2 } else { sb.append(s[i]); i++ } }
                    out.add(sb.toString())
                    var f = found + 1
                    while (f < n && (s[f] == ' ' || s[f] == '\t')) f++
                    if (f >= n) break
                    pos = f + 1
                    if (out.size > MAX_ARGS + 1) return null
                    continue
                }
            }
            val sb = StringBuilder()
            var e = b
            while (e < n) {
                val c = s[e]
                if (c == '\\' && e + 1 < n && s[e + 1] == ',') { sb.append(','); e += 2; continue }
                if (c == ',') break
                sb.append(c); e++
            }
            out.add(sb.toString().trimEnd())
            if (e >= n) break
            pos = e + 1
            if (out.size > MAX_ARGS + 1) return null
        }
        return out
    }

    /** The stored form of the text inside `+js(` ... `)`, or null when it is not a call we understand. */
    fun encode(inner: String): String? {
        if (inner.length > MAX_TOTAL) return null
        val parts = splitArgs(inner) ?: return null
        if (parts.isEmpty()) return null
        val name = canonical(parts[0]) ?: return null
        val args = parts.subList(1, parts.size).toMutableList()
        if (args.size > MAX_ARGS) return null
        var total = 0
        for (a in args) {
            if (a.length > MAX_ARG) return null
            for (c in a) if (c.code < 0x20 || c.code == 0x7f) return null
            total += a.length
        }
        if (total > MAX_TOTAL) return null
        // the arguments that make the call mean something (see NEED)
        fun arg(i: Int) = args.getOrNull(i)?.isNotEmpty() == true
        val ok = when (NEED.getValue(name)) {
            Need.NONE -> true
            Need.ONE -> arg(0)
            Need.TWO -> arg(0) && args.size >= 2
            Need.ONE_OF_TWO -> arg(0) || arg(1)
        }
        if (!ok) return null
        while (args.isNotEmpty() && args.last().isEmpty()) args.removeAt(args.size - 1) // trailing empty arguments carry nothing
        val sb = StringBuilder(inner.length + 8)
        sb.append(PREFIX).append(name)
        for (a in args) sb.append(SEP).append(a)
        return sb.toString()
    }

    fun decode(stored: String): Call? {
        if (!isStored(stored) || stored == ALL_STORED) return null
        val parts = stored.substring(1).split(SEP)
        return Call(parts[0], parts.subList(1, parts.size))
    }

    /** `[["name","arg",...],...]` as the CSS comment body: ASCII only, and no `*` anywhere (written `*`), so the text can never end the comment. At most [MAX_CALLS] calls and [MAX_PAYLOAD] characters. */
    fun payload(stored: List<String>): String {
        val sb = StringBuilder(64)
        sb.append('[')
        var n = 0
        for (s in stored) {
            val c = decode(s) ?: continue
            val before = sb.length
            if (n > 0) sb.append(',')
            sb.append('[')
            jsonString(sb, c.name)
            for (a in c.args) { sb.append(','); jsonString(sb, a) }
            sb.append(']')
            if (sb.length + 1 > MAX_PAYLOAD) { sb.setLength(before); break }
            n++
            if (n >= MAX_CALLS) break
        }
        if (n == 0) return ""
        sb.append(']')
        return sb.toString()
    }

    private fun jsonString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '*' -> sb.append("\\u002a")
                c.code < 0x20 || c.code > 0x7e -> sb.append("\\u").append(String.format("%04x", c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }
}
