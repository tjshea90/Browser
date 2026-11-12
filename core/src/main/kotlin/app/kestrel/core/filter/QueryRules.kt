package app.kestrel.core.filter

/**
 * `$removeparam` (AA3-3b): query parameters that the lists name as trackers, removed from the address of a TOP-LEVEL navigation (uBlock Origin, AdGuard and Brave do the same with their
 * "URL Tracking Protection" lists). Complements the short built-in list of [app.kestrel.core.url.TrackingParams], which stays as the first line of defence.
 *
 * Supported (measured on the real lists, research/ADBLOCK_ENGINE.md section 7: 97 % of the 2,990 rules are plain names):
 *  - `$removeparam=name`, anywhere (generic) or confined with `domain=` / `to=` (include and `~exclude`), also `||host^$removeparam=name` and `||host/path/prefix$removeparam=name`
 *  - a bare `||host^$removeparam` (drop the whole query of that site)
 *  - exceptions `@@||host^$removeparam=name` and `@@||host^$removeparam` (the site keeps every parameter)
 *  - `doc` / `document`, `badfilter`, `important`, `1p`
 * Entity hosts (`||bbc.*^`, the shorthand `||bbc.`, `domain=site.*`) mean the site under any public suffix and are compared with the host minus its suffix ([Psl]).
 * Left out on purpose: regular-expression values (ReDoS), rules for sub-resources (`xhr`, `script`, `image`...: nothing here is a top-level navigation),
 * `3p`, wildcards inside a path, and every other option. A generic rule may never name a parameter that sites need to work (see [PROTECTED]) and a bare rule needs a host.
 *
 * Names are matched exactly (parameter names are case-sensitive); hosts and path prefixes are lower case like the rest of the engine.
 */
class QueryRules internal constructor(private val pool: ByteArray, val count: Int) {

    /** Rules by the host they are written for ("" = every site), built on first use (a few milliseconds for 3,000 rules; the engine warms it off the UI thread). */
    private val index: Map<String, Array<QueryRule>> by lazy(LazyThreadSafetyMode.PUBLICATION) { decode(pool) }

    fun warm() { if (count > 0) index.size }

    internal fun pool(): ByteArray = pool

    /**
     * The address with the parameters that the rules name taken out, or null when nothing changed (callers can then skip any redirect).
     * Only http(s) addresses with a query are looked at; the fragment is kept; an emptied query loses its '?'.
     */
    fun strip(url: String): String? {
        if (count == 0 || url.length > MAX_URL) return null
        val q = url.indexOf('?')
        if (q < 0) return null
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 4 || schemeEnd > q) return null
        val hostStart = schemeEnd + 3
        var authEnd = q
        val slash = url.indexOf('/', hostStart)
        if (slash in hostStart until authEnd) authEnd = slash
        var hostFrom = hostStart
        val at = url.lastIndexOf('@', authEnd - 1)
        if (at >= hostStart) hostFrom = at + 1
        var hostTo = authEnd
        if (hostFrom < hostTo && url[hostFrom] == '[') return null // IPv6 literal: no rule is written for one
        val colon = url.lastIndexOf(':', hostTo - 1)
        if (colon > hostFrom) hostTo = colon
        if (hostTo <= hostFrom) return null
        val host = url.substring(hostFrom, hostTo).lowercase().trimEnd('.')
        if (host.isEmpty()) return null
        val path = if (slash in hostStart until q) url.substring(slash, q) else ""

        val hash = url.indexOf('#', q)
        val queryEnd = if (hash >= 0) hash else url.length
        if (q + 1 >= queryEnd) return null

        val verdict = Outcome()
        collect(host, path, verdict)
        if (verdict.allowAll) return null
        if (!verdict.removeAll && verdict.names.isEmpty()) return null
        if (verdict.removeAll) {
            return url.substring(0, q) + (if (hash >= 0) url.substring(hash) else "")
        }

        val kept = StringBuilder()
        var removed = false
        var pos = q + 1
        while (pos <= queryEnd) {
            var amp = url.indexOf('&', pos)
            if (amp < 0 || amp > queryEnd) amp = queryEnd
            if (amp > pos) {
                val eq = url.indexOf('=', pos)
                val keyEnd = if (eq in pos until amp) eq else amp
                val key = url.substring(pos, keyEnd)
                if (key in verdict.names && key !in verdict.kept) removed = true
                else { if (kept.isNotEmpty()) kept.append('&'); kept.append(url, pos, amp) }
            }
            pos = amp + 1
        }
        if (!removed) return null
        val sb = StringBuilder(url.length)
        sb.append(url, 0, q)
        if (kept.isNotEmpty()) sb.append('?').append(kept)
        if (hash >= 0) sb.append(url, hash, url.length)
        return sb.toString()
    }

    private class Outcome {
        @JvmField var removeAll = false
        @JvmField var allowAll = false
        @JvmField val names = HashSet<String>(8)
        @JvmField val kept = HashSet<String>(4)
    }

    private fun collect(host: String, path: String, v: Outcome) {
        var lowerPath: String? = null
        val base = entityBase(host) // "a.b.bbc" for a.b.bbc.co.uk: what entity rules ("bbc.*") are compared with
        fun scan(rules: Array<QueryRule>?) {
            if (rules == null) return
            for (r in rules) {
                if (r.path.isNotEmpty()) {
                    val lp = lowerPath ?: path.lowercase().also { lowerPath = it }
                    if (!lp.startsWith(r.path)) continue
                }
                if (r.only.isNotEmpty() && !insideAny(host, base, r.only)) continue
                if (r.except.isNotEmpty() && insideAny(host, base, r.except)) continue
                when (r.kind) {
                    QueryRule.REMOVE -> v.names.add(r.name)
                    QueryRule.REMOVE_ALL -> v.removeAll = true
                    QueryRule.KEEP -> v.kept.add(r.name)
                    else -> v.allowAll = true
                }
            }
        }
        scan(index[""])
        var h = host
        while (true) {
            scan(index[h])
            val dot = h.indexOf('.')
            if (dot < 0) break
            val rest = h.substring(dot + 1)
            if (rest.indexOf('.') < 0) break // never a bare "com"
            h = rest
        }
        if (base != null) {
            h = base
            while (true) {
                scan(index["$h.*"])
                val dot = h.indexOf('.')
                if (dot < 0) break
                h = h.substring(dot + 1)
            }
        }
    }

    /** The host without its public suffix ("a.b.bbc.co.uk" -> "a.b.bbc"), or null for an address and a host that is itself a suffix. */
    private fun entityBase(host: String): String? {
        val site = Psl.default.siteOf(host) ?: return null
        val dot = site.indexOf('.')
        if (dot < 0) return null // single label or IP literal
        val suffixLen = site.length - dot // ".co.uk"
        if (suffixLen >= host.length) return null
        return host.substring(0, host.length - suffixLen)
    }

    companion object {
        const val MAX_URL = 8192
        const val MAX_RULES = 40_000
        private const val MAX_POOL = 4 shl 20

        val EMPTY = QueryRules(ByteArray(0), 0)

        /**
         * Parameters a site needs to work: a generic rule (one that is not tied to a site) may never name them, whatever a list says. (A poisoned or broken list must not break every
         * search box, login and video link.)
         */
        internal val PROTECTED: Set<String> = hashSetOf(
            "q", "query", "s", "search", "id", "v", "p", "u", "url", "uri", "code", "state", "token", "redirect", "redirect_uri", "return", "returnurl", "next", "continue",
            "client_id", "response_type", "scope", "nonce", "page", "lang", "locale", "session", "sessionid", "sid", "key", "t", "list", "tab", "view", "type", "name", "file",
        )

        /** True when [host] is one of [doms] or a subdomain of it; an entry "site.*" stands for the site under any public suffix and is compared with [base] (the host without its suffix). */
        internal fun insideAny(host: String, base: String?, doms: Array<String>): Boolean {
            for (d in doms) {
                if (d.endsWith(".*")) {
                    val core = d.substring(0, d.length - 2)
                    if (base != null && (base == core || (base.length > core.length && base.endsWith(core) && base[base.length - core.length - 1] == '.'))) return true
                } else if (host == d || (host.length > d.length && host.endsWith(d) && host[host.length - d.length - 1] == '.')) return true
            }
            return false
        }

        internal fun compile(rules: List<QueryRule>): QueryRules {
            if (rules.isEmpty()) return EMPTY
            val seen = LinkedHashSet<String>()
            for (r in rules) { seen.add(r.line()); if (seen.size >= MAX_RULES) break }
            val bytes = seen.joinToString("\n").toByteArray(Charsets.UTF_8)
            return QueryRules(bytes, seen.size)
        }

        /** Reads the pool of a compiled file back; @throws IllegalArgumentException when it is not a pool this version wrote. */
        internal fun fromPool(bytes: ByteArray): QueryRules {
            require(bytes.size <= MAX_POOL) { "query pool too big" }
            if (bytes.isEmpty()) return EMPTY
            val rules = decode(bytes)
            var n = 0
            for (a in rules.values) n += a.size
            require(n in 1..MAX_RULES) { "query rule count" }
            return QueryRules(bytes, n)
        }

        private fun decode(bytes: ByteArray): Map<String, Array<QueryRule>> {
            val by = HashMap<String, ArrayList<QueryRule>>()
            val text = String(bytes, Charsets.UTF_8)
            var start = 0
            while (start <= text.length) {
                var end = text.indexOf('\n', start)
                if (end < 0) end = text.length
                if (end > start) {
                    val r = QueryRule.fromLine(text.substring(start, end)) ?: throw IllegalArgumentException("bad query rule")
                    by.getOrPut(r.host) { ArrayList() }.add(r)
                }
                start = end + 1
            }
            val out = HashMap<String, Array<QueryRule>>(by.size * 2)
            for ((h, l) in by) out[h] = l.toTypedArray()
            return out
        }
    }
}

/** One parsed `$removeparam` rule in the form it is stored and evaluated. */
internal class QueryRule(
    @JvmField val kind: Char,            // REMOVE, REMOVE_ALL, KEEP, ALLOW_ALL
    @JvmField val name: String,          // "" for the two whole-query kinds
    @JvmField val host: String,          // "" = every site; otherwise the host the rule is written for (it also covers the subdomains)
    @JvmField val path: String,          // lower-case literal path prefix, "" = any path
    @JvmField val only: Array<String>,   // the address' host must be (inside) one of these; empty = any
    @JvmField val except: Array<String>, // ... and none of these
    @JvmField val badfilter: Boolean,
    @JvmField val key: Long,             // hash of the rule's canonical text, to apply $badfilter
) {
    fun line(): String = buildString {
        append(kind).append('\t').append(name).append('\t').append(host).append('\t').append(path).append('\t')
        append(only.joinToString(",")).append('\t').append(except.joinToString(","))
    }

    companion object {
        const val REMOVE = 'r'
        const val REMOVE_ALL = 'R'
        const val KEEP = 'e'
        const val ALLOW_ALL = 'E'

        fun fromLine(l: String): QueryRule? {
            val f = l.split('\t')
            if (f.size != 6 || f[0].length != 1) return null
            val kind = f[0][0]
            if (kind != REMOVE && kind != REMOVE_ALL && kind != KEEP && kind != ALLOW_ALL) return null
            val named = kind == REMOVE || kind == KEEP
            if (named != f[1].isNotEmpty()) return null
            if (f[1].length > 64 || f[2].length > 253 || f[3].length > 200) return null
            return QueryRule(kind, f[1], f[2], f[3], list(f[4]), list(f[5]), false, 0L)
        }

        private fun list(s: String): Array<String> = if (s.isEmpty()) emptyArray() else s.split(',').toTypedArray()
    }
}

/** Turns one list line that carries `removeparam` into a [QueryRule] (or says why not). Stateless. */
internal object QueryRuleParser {
    class Result(@JvmField val rule: QueryRule?, @JvmField val skip: Skip)

    private val FAIL_SYNTAX = Result(null, Skip.BAD_SYNTAX)
    private val FAIL_OPTION = Result(null, Skip.UNSUPPORTED_OPTION)
    private val FAIL_REGEX = Result(null, Skip.REGEX)
    private val FAIL_ENTITY = Result(null, Skip.ENTITY_DOMAIN)
    private const val MAX_LINE = 4096
    private const val MAX_DOMAINS = 300

    /** Cheap test before parsing: an option named removeparam after a '$'. */
    fun looks(line: String): Boolean {
        val i = line.indexOf("removeparam")
        return i > 0 && line.lastIndexOf('$', i) >= 0
    }

    fun parse(raw: String): Result {
        if (raw.length > MAX_LINE) return FAIL_SYNTAX
        var s = raw.trim()
        var exception = false
        if (s.startsWith("@@")) { exception = true; s = s.substring(2) }
        val at = s.indexOf("removeparam")
        if (at <= 0) return FAIL_SYNTAX
        val dollar = s.lastIndexOf('$', at)
        if (dollar < 0) return FAIL_SYNTAX
        val pat = s.substring(0, dollar).lowercase()
        val options = s.substring(dollar + 1)

        var haveRemove = false
        var name: String? = null
        var doc = false
        var typed = false
        var badfilter = false
        var domainOpt = false
        val only = ArrayList<String>()
        val except = ArrayList<String>()
        val canonOpts = ArrayList<String>()
        for (opt in options.split(',')) {
            val o = opt.trim()
            if (o.isEmpty()) continue
            var neg = false
            var body = o
            if (body.startsWith("~")) { neg = true; body = body.substring(1) }
            val eq = body.indexOf('=')
            val key = (if (eq >= 0) body.substring(0, eq) else body).lowercase()
            val value = if (eq >= 0) body.substring(eq + 1) else null
            if (key != "badfilter") canonOpts.add(o)
            when (key) {
                "removeparam" -> {
                    if (haveRemove || neg) return FAIL_SYNTAX
                    haveRemove = true
                    if (value != null) {
                        if (value.startsWith("/") || value.startsWith("~/")) return FAIL_REGEX
                        if (!goodName(value)) return FAIL_SYNTAX
                        name = value
                    }
                }
                "doc", "document" -> { if (neg) return FAIL_OPTION; doc = true }
                "domain", "to" -> {
                    if (value == null || neg || domainOpt) return if (value == null || neg) FAIL_SYNTAX else FAIL_OPTION
                    domainOpt = true
                    for (d in value.split('|')) {
                        var dom = d.trim().lowercase()
                        if (dom.isEmpty()) continue
                        var ex = false
                        if (dom.startsWith("~")) { ex = true; dom = dom.substring(1) }
                        if (dom.endsWith(".*")) {
                            val core = dom.substring(0, dom.length - 2)
                            if (!RuleParser.isHostName(core)) return FAIL_SYNTAX
                        } else if (dom.indexOf('*') >= 0) return FAIL_ENTITY
                        else if (!RuleParser.isHostName(dom)) return FAIL_SYNTAX
                        (if (ex) except else only).add(dom)
                        if (only.size + except.size > MAX_DOMAINS) return FAIL_SYNTAX
                    }
                }
                "third-party", "3p" -> if (!neg) return FAIL_OPTION // a top-level navigation is never third-party
                "first-party", "1p" -> if (neg) return FAIL_OPTION
                "important", "all" -> { }
                "badfilter" -> badfilter = true
                else -> {
                    if (RequestType.fromName(key) == 0) return FAIL_OPTION // csp, redirect, match-case, denyallow, header, method, cname, ...
                    if (!neg) typed = true // image, script, xhr...: a request a page makes, not a navigation
                }
            }
        }
        if (!haveRemove) return FAIL_OPTION
        if (typed && !doc) return FAIL_OPTION

        var host = ""
        var path = ""
        if (pat.isNotEmpty() && pat != "*") {
            if (!pat.startsWith("||")) return FAIL_OPTION // "|https://", "/path", "?query=" and friends
            val body = pat.substring(2)
            val slash = body.indexOf('/')
            var hostPart = if (slash >= 0) body.substring(0, slash) else body
            val rest = if (slash >= 0) body.substring(slash) else ""
            if (hostPart.endsWith("^")) { hostPart = hostPart.substring(0, hostPart.length - 1); if (rest.isNotEmpty()) return FAIL_OPTION }
            // "||www.amazon.*" and the shorthand "||bbc." both mean the site under any public suffix; stored as "bbc.*"
            var entity = false
            if (hostPart.endsWith(".*")) { entity = true; hostPart = hostPart.substring(0, hostPart.length - 2) }
            else if (hostPart.endsWith(".") && hostPart.length > 1) { entity = true; hostPart = hostPart.substring(0, hostPart.length - 1) }
            if (hostPart.indexOf('*') >= 0) return FAIL_ENTITY
            if (!RuleParser.isHostName(hostPart) || (!entity && hostPart.indexOf('.') < 0)) return FAIL_SYNTAX
            host = if (entity) "$hostPart.*" else hostPart
            if (rest.isNotEmpty()) {
                if (rest.length > 200) return FAIL_SYNTAX
                for (c in rest) if (c == '*' || c == '^' || c == '|' || c == '?' || c == '$' || c.code <= 32 || c.code > 126) return FAIL_OPTION
                path = if (rest == "/") "" else rest
            }
        }

        // A list may not make the browser strip every query everywhere, or let every site keep everything.
        if (host.isEmpty() && only.isEmpty() && (name == null || exception)) return FAIL_SYNTAX
        if (host.isEmpty() && only.isEmpty() && name != null && name.lowercase() in QueryRules.PROTECTED) return FAIL_OPTION

        val kind = when {
            exception -> if (name == null) QueryRule.ALLOW_ALL else QueryRule.KEEP
            else -> if (name == null) QueryRule.REMOVE_ALL else QueryRule.REMOVE
        }
        val canon = (if (exception) "@@" else "") + pat + "$" + canonOpts.joinToString(",")
        return Result(QueryRule(kind, name ?: "", host, path, only.toTypedArray(), except.toTypedArray(), badfilter, Hash.h64(canon)), Skip.NONE)
    }

    private fun goodName(n: String): Boolean {
        if (n.isEmpty() || n.length > 64) return false
        for (c in n) {
            val ok = (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || c == '_' || c == '-' || c == '.' || c == '~' || c == '%' || c == ':' || c == '[' || c == ']' || c == ';'
            if (!ok) return false
        }
        return true
    }
}
