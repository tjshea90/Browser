package app.kestrel.core.filter

/** Why a line was not turned into a rule (surfaced in the build report so list coverage is measurable). */
enum class Skip {
    NONE, EMPTY, REGEX, UNSUPPORTED_OPTION, NON_ASCII, BAD_SYNTAX, ENTITY_DOMAIN, COSMETIC_UNSUPPORTED, COSMETIC_EXCLUSIONS, COSMETIC_UNSAFE, SCRIPTLET_UNSUPPORTED,
}

/** A parsed network rule in normalised form (see FilterSetBuilder for how it is stored). */
internal class NetRule(
    @JvmField val pattern: String,
    @JvmField val exception: Boolean,
    @JvmField val leftHost: Boolean,
    @JvmField val leftAnchor: Boolean,
    @JvmField val rightAnchor: Boolean,
    @JvmField val types: Int,
    @JvmField val party: Int, // 0 any, 1 third-party only, 2 first-party only
    @JvmField val important: Boolean,
    @JvmField val include: LongArray?,
    @JvmField val exclude: LongArray?,
    @JvmField val badfilter: Boolean,
    @JvmField val siteFlags: Int, // SITE_* for @@||host^$document / $elemhide / $generichide
    @JvmField val key: Long, // canonical-text hash, used to apply $badfilter
    @JvmField val redirect: Int = 0, // AW1c: id of the stand-in resource ([RedirectResources]) a `$redirect=` / `$redirect-rule=` rule names, 0 = none
    @JvmField val redirectOnly: Boolean = false, // `$redirect-rule=`: blocks nothing by itself, only picks the stand-in for a request that something else blocks
)

internal class CosmeticRule(
    @JvmField val domains: List<String>, // empty = generic; an entry `site.*` is an entity domain (any registrable domain with that name, whatever its public suffix)
    @JvmField val selector: String,
    @JvmField val exception: Boolean,
    @JvmField val excluded: List<String> = emptyList(), // `~site.com`: the rule is off on these (filed as an exception of the same text)
)

internal const val SITE_DOCUMENT_ALLOW = 1
internal const val SITE_ELEMHIDE = 2
internal const val SITE_GENERICHIDE = 4

/**
 * Parser for the Adblock Plus / uBlock Origin / Brave filter syntax subset documented in docs/ARCHITECTURE.md section 6.
 * Single-threaded; [skip] holds the reason for the most recent null result.
 */
internal class RuleParser {
    @JvmField var skip: Skip = Skip.NONE

    fun parseNetwork(rawLine: String): NetRule? {
        skip = Skip.NONE
        if (rawLine.length > MAX_LINE) return fail(Skip.BAD_SYNTAX) // lists are third-party input: no megabyte lines
        var s = rawLine.trim()
        if (s.isEmpty()) return fail(Skip.EMPTY)
        var exception = false
        if (s.startsWith("@@")) { exception = true; s = s.substring(2) }

        var optStr: String? = null
        val dollar = s.lastIndexOf('$')
        if (dollar >= 0) {
            optStr = s.substring(dollar + 1)
            s = s.substring(0, dollar)
        }
        if (s.length >= 2 && s.startsWith("/") && s.endsWith("/")) return fail(Skip.REGEX)

        var types = 0
        var negTypes = 0
        var party = 0
        var important = false
        var badfilter = false
        var siteFlags = 0
        var redirect = 0
        var redirectOnly = false
        var include: ArrayList<Long>? = null
        var exclude: ArrayList<Long>? = null
        val canonicalOpts = StringBuilder()

        if (optStr != null) {
            for (opt in optStr.split(',')) {
                var o = opt.trim().lowercase()
                if (o.isEmpty()) continue
                var neg = false
                if (o.startsWith("~")) { neg = true; o = o.substring(1) }
                val eq = o.indexOf('=')
                val name = if (eq >= 0) o.substring(0, eq) else o
                val value = if (eq >= 0) o.substring(eq + 1) else null
                if (name != "badfilter") { if (canonicalOpts.isNotEmpty()) canonicalOpts.append(','); canonicalOpts.append(opt.trim().lowercase()) }
                when (name) {
                    "third-party", "3p" -> party = if (neg) 2 else 1
                    "first-party", "1p" -> party = if (neg) 1 else 2
                    "important" -> important = true
                    "badfilter" -> badfilter = true
                    "all" -> types = RequestType.ALL
                    "elemhide", "ehide" -> siteFlags = siteFlags or SITE_ELEMHIDE
                    "generichide", "ghide" -> siteFlags = siteFlags or SITE_GENERICHIDE
                    "redirect", "redirect-rule" -> {
                        if (value == null || neg || exception) return fail(Skip.UNSUPPORTED_OPTION) // `@@...$redirect=` cancels a stand-in: not understood, the stand-in simply stays
                        val id = RedirectResources.idOf(value)
                        if (name == "redirect-rule") { if (id == 0) return fail(Skip.UNSUPPORTED_OPTION); redirectOnly = true }
                        redirect = id // `redirect=` with a resource we do not have is a plain block rule (id 0)
                    }
                    "domain" -> {
                        if (value == null || neg) return fail(Skip.BAD_SYNTAX)
                        for (d in value.split('|')) {
                            var dom = d.trim()
                            if (dom.isEmpty()) continue
                            var ex = false
                            if (dom.startsWith("~")) { ex = true; dom = dom.substring(1) }
                            if (dom.indexOf('*') >= 0 && !isEntity(dom)) return fail(Skip.ENTITY_DOMAIN) // AW1d: `site.*` is understood (any public suffix); a star anywhere else is not
                            val hash = Hash.h64(dom.trimEnd('.'))
                            if (ex) (exclude ?: ArrayList<Long>().also { exclude = it }).add(hash)
                            else (include ?: ArrayList<Long>().also { include = it }).add(hash)
                        }
                    }
                    else -> {
                        val t = RequestType.fromName(name)
                        if (t == 0) return fail(Skip.UNSUPPORTED_OPTION) // popup, csp, redirect, removeparam, match-case, ...
                        if (neg) negTypes = negTypes or t else types = types or t
                    }
                }
            }
        }

        var leftHost = false
        var leftAnchor = false
        var rightAnchor = false
        if (s.startsWith("||")) { leftHost = true; s = s.substring(2) }
        else if (s.startsWith("|")) { leftAnchor = true; s = s.substring(1) }
        if (s.endsWith("|")) { rightAnchor = true; s = s.substring(0, s.length - 1) }
        s = s.lowercase()
        for (c in s) if (c.code > 126 || c.code < 33) return fail(if (c.code > 126) Skip.NON_ASCII else Skip.BAD_SYNTAX)
        s = collapseStars(s)
        if (s.startsWith("*")) { s = s.trimStart('*'); leftHost = false; leftAnchor = false }
        if (s.endsWith("*")) { s = s.trimEnd('*'); rightAnchor = false }
        if (s.isEmpty() && (leftHost || leftAnchor || rightAnchor)) return fail(Skip.BAD_SYNTAX)
        if (s.length > MAX_PATTERN) return fail(Skip.BAD_SYNTAX)
        // A poisoned or broken list must not be able to block (or allow) everything: a rule with no usable pattern, or a tiny
        // unanchored one ("e", "a^"), is only accepted when it is confined to specific sites with domain=.
        if (include == null && siteFlags == 0 && !leftHost && !leftAnchor) {
            var literals = 0
            for (c in s) if (c != '*' && c != '^') literals++
            if (literals < MIN_LITERALS) return fail(Skip.BAD_SYNTAX)
        }
        if (include == null && siteFlags == 0 && s.isEmpty()) return fail(Skip.BAD_SYNTAX) // "||"-less "$script", "@@$image" and friends

        // Site-level exceptions: @@||host^$document|$elemhide|$generichide
        if (exception && types == RequestType.DOCUMENT && negTypes == 0) { siteFlags = siteFlags or SITE_DOCUMENT_ALLOW; types = 0 }
        if (siteFlags != 0) {
            if (!exception) return fail(Skip.UNSUPPORTED_OPTION)
            val host = hostOnlyPattern(leftHost, leftAnchor, rightAnchor, s) ?: return fail(Skip.UNSUPPORTED_OPTION)
            if (host.isEmpty()) return fail(Skip.BAD_SYNTAX)
        }

        val finalTypes = when {
            types == 0 && negTypes == 0 -> RequestType.ALL_SUBRESOURCES
            types == 0 -> RequestType.ALL_SUBRESOURCES and negTypes.inv()
            else -> types and negTypes.inv()
        }
        if (finalTypes == 0 && siteFlags == 0) return fail(Skip.BAD_SYNTAX)

        val canon = (if (exception) "@@" else "") + (if (leftHost) "||" else if (leftAnchor) "|" else "") + s +
            (if (rightAnchor) "|" else "") + (if (canonicalOpts.isNotEmpty()) "$$canonicalOpts" else "")
        return NetRule(
            pattern = s, exception = exception, leftHost = leftHost, leftAnchor = leftAnchor, rightAnchor = rightAnchor,
            types = finalTypes, party = party, important = important,
            include = include?.toLongArray(), exclude = exclude?.toLongArray(),
            badfilter = badfilter, siteFlags = siteFlags, key = Hash.h64(canon), redirect = redirect, redirectOnly = redirectOnly,
        )
    }

    /** If the normalised pattern is exactly `||host^` (or `||host`), returns the host; else null. */
    fun hostOnlyPattern(leftHost: Boolean, leftAnchor: Boolean, rightAnchor: Boolean, pattern: String): String? {
        if (!leftHost || leftAnchor || rightAnchor) return null
        val body = if (pattern.endsWith("^")) pattern.substring(0, pattern.length - 1) else pattern
        if (body.isEmpty() || !isHostName(body)) return null
        return body
    }

    private fun collapseStars(s: String): String {
        if (!s.contains("**")) return s
        val sb = StringBuilder(s.length)
        var prevStar = false
        for (c in s) {
            if (c == '*') { if (!prevStar) sb.append(c); prevStar = true } else { sb.append(c); prevStar = false }
        }
        return sb.toString()
    }

    private fun fail(reason: Skip): NetRule? { skip = reason; return null }

    // ------------------------------------------------------------------ cosmetic

    /**
     * Parses `[domains]##selector` and `[domains]#@#selector`. Returns null (with [skip] set) for the unsupported procedural / HTML-filtering / CSS-injection forms and unsafe selectors. `+js(...)` scriptlet
     * calls (AW1b) need domains and are stored in their own form ([ScriptletRules]). Domains may be entity domains (`site.*`, AW1d) and may carry `~site.com` exclusions.
     */
    fun parseCosmetic(line: String): CosmeticRule? {
        skip = Skip.NONE
        val i = line.indexOf('#')
        if (i < 0) return fail2(Skip.BAD_SYNTAX)
        var exception = false
        val selStart: Int
        when {
            line.startsWith("##", i) -> selStart = i + 2
            line.startsWith("#@#", i) -> { exception = true; selStart = i + 3 }
            line.startsWith("#?#", i) -> selStart = i + 3                              // the ABP / uBO spelling of an extended (procedural) selector; a plain CSS selector here is just a normal rule
            line.startsWith("#@?#", i) -> { exception = true; selStart = i + 4 }
            else -> return fail2(Skip.COSMETIC_UNSUPPORTED) // #$# #%# ...
        }
        var selector = line.substring(selStart).trim()
        if (selector.startsWith("^") || selector.isEmpty()) return fail2(Skip.COSMETIC_UNSUPPORTED)
        var styled = false
        var procedural = false
        var scriptlet = false
        if (selector.startsWith("+js(")) {
            if (!selector.endsWith(")")) return fail2(Skip.SCRIPTLET_UNSUPPORTED)
            val inner = selector.substring(4, selector.length - 1)
            selector = (if (inner.isBlank()) (if (exception) ScriptletRules.ALL_STORED else null) else ScriptletRules.encode(inner)) ?: return fail2(Skip.SCRIPTLET_UNSUPPORTED)
            scriptlet = true
        } else {
            val st = CosmeticSelectors.splitStyle(selector)
            if (st != null) { // `site##selector:style(declarations)`: a restyling rule for a site (never an exception, never generic: checked below)
                if (exception) return fail2(Skip.COSMETIC_UNSUPPORTED)
                val decls = CosmeticSelectors.acceptDeclarations(st.second) ?: return fail2(Skip.COSMETIC_UNSAFE)
                if (!CosmeticSelectors.accept(st.first)) return fail2(if (CosmeticSelectors.isProcedural(st.first)) Skip.COSMETIC_UNSUPPORTED else Skip.COSMETIC_UNSAFE)
                selector = st.first + CosmeticSelectors.STYLE_SEP + decls
                styled = true
            } else if (!CosmeticSelectors.accept(selector)) {
                if (!CosmeticSelectors.isProcedural(selector)) return fail2(Skip.COSMETIC_UNSAFE)
                selector = ProceduralRules.encode(selector) ?: return fail2(Skip.COSMETIC_UNSUPPORTED)   // outside the supported subset (xpath, matches-css, a selector after an operator ...)
                procedural = true
            }
        }

        val domains = ArrayList<String>()
        val excluded = ArrayList<String>()
        if (i > 0) {
            for (d in line.substring(0, i).split(',')) {
                var t = d.trim().lowercase()
                if (t.isEmpty()) continue
                var ex = false
                if (t.startsWith("~")) { ex = true; t = t.substring(1) }
                if (t.indexOf('*') >= 0 && !isEntity(t)) return fail2(Skip.ENTITY_DOMAIN)   // a star anywhere but at the end ("*.site.com", "site.*.uk") is not understood
                if (!isHostName(if (t.endsWith(".*")) t.substring(0, t.length - 2) else t)) return fail2(Skip.BAD_SYNTAX)
                if (ex) excluded.add(t) else domains.add(t)
            }
            if (domains.isEmpty() && excluded.isEmpty()) return fail2(Skip.BAD_SYNTAX)
        }
        if (scriptlet && domains.isEmpty()) return fail2(Skip.COSMETIC_UNSUPPORTED) // a scriptlet for every site: never taken from a list
        if (styled && domains.isEmpty()) return fail2(Skip.COSMETIC_UNSUPPORTED) // a restyling rule for every site is too broad to take from a list
        if (procedural && domains.isEmpty()) return fail2(Skip.COSMETIC_UNSUPPORTED) // a script that runs on every page for a generic procedural rule: not worth the cost on this device
        if (exception && excluded.isNotEmpty()) return fail2(Skip.COSMETIC_EXCLUSIONS)  // an exception that is itself off on some sites: no list writes this
        return CosmeticRule(domains, selector, exception, excluded)
    }

    private fun fail2(reason: Skip): CosmeticRule? { skip = reason; return null }

    companion object {
        private const val MAX_LINE = 16_384 // legit uBlock lines with long domain= lists reach a few KB
        private const val MAX_PATTERN = 1_000 // legit URL patterns are < 300 chars; keeps token indexing linear
        private const val MIN_LITERALS = 3

        /** `site.*`: a name followed by `.*` (the name itself a plain host name), nothing else starred. */
        fun isEntity(t: String): Boolean = t.length > 2 && t.endsWith(".*") && t.indexOf('*') == t.length - 1

        /** Lower-case hostname characters only (letters, digits, '.', '-'); no empty labels. */
        fun isHostName(s: String): Boolean {
            if (s.isEmpty() || s.length > 253 || s[0] == '.' || s[0] == '-' || s.endsWith(".")) return false
            var prevDot = false
            for (c in s) {
                val ok = (c in 'a'..'z') || (c in '0'..'9') || c == '-' || c == '.' || c == '_'
                if (!ok) return false
                if (c == '.') { if (prevDot) return false; prevDot = true } else prevDot = false
            }
            return true
        }

        /** True for lines that carry cosmetic syntax (checked before network parsing). */
        fun looksCosmetic(line: String): Boolean {
            val i = line.indexOf('#')
            if (i < 0) return false
            return line.startsWith("##", i) || line.startsWith("#@#", i) || line.startsWith("#?#", i) ||
                line.startsWith("#$#", i) || line.startsWith("#%#", i) || line.startsWith("#@?#", i) || line.startsWith("#@$#", i)
        }
    }
}
