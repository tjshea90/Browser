package app.kestrel.core.filter

/** Rule categories (also the blocked-count buckets shown in the UI). */
object Category {
    const val ADS = 0
    const val PRIVACY = 1 // trackers
    const val MALWARE = 2 // malware + phishing; also blocks main-frame navigations
    const val ANNOYANCE = 3
}

/** Packed result of [FilterSet.check]: zero allocation on the hot path. */
object Verdict {
    const val NONE = 0
    const val BLOCK = 1
    const val EXCEPTED = 2 // a block rule matched but an exception rule allowed the request
    const val IMPORTANT = 16 // $important block (beats exceptions)

    @JvmStatic fun isBlocked(v: Int): Boolean = (v and BLOCK) != 0
    @JvmStatic fun category(v: Int): Int = (v ushr 2) and 3
}

/** Bit layout shared by host-rule flags and pattern-rule meta words. */
internal object Bits {
    const val TYPES = 0x1FFF // 13 request-type bits (12 network types + POPUP)
    const val PARTY_SHIFT = 13 // 2 bits: 0 any, 1 third-party only, 2 first-party only
    const val IMPORTANT = 1 shl 15
    const val EXCEPTION = 1 shl 16
    const val CAT_SHIFT = 17 // 2 bits
    const val LEFT_HOST = 1 shl 19
    const val LEFT_ANCHOR = 1 shl 20
    const val RIGHT_ANCHOR = 1 shl 21

    fun pack(types: Int, party: Int, important: Boolean, category: Int): Int =
        (types and TYPES) or (party shl PARTY_SHIFT) or (if (important) IMPORTANT else 0) or (category shl CAT_SHIFT)
}

/** Sorted-hash table of pure `||host^` rules: 12 bytes per rule, binary-searched. Duplicate hashes allowed. */
internal class HostTable(@JvmField val hashes: LongArray, @JvmField val flags: IntArray) {
    companion object { val EMPTY = HostTable(LongArray(0), IntArray(0)) }
}

/** Flat, token-indexed table of all other network rules (no per-rule objects). */
internal class PatternTable(
    @JvmField val count: Int,
    @JvmField val patBytes: ByteArray,
    @JvmField val patOff: IntArray, // count + 1
    @JvmField val meta: IntArray, // Bits.* per rule
    @JvmField val dom: IntArray, // 0 = no domain constraint, else 1 + offset into domPool
    @JvmField val domPool: LongArray, // [nInclude, nExclude, includeHashes..., excludeHashes...]
    @JvmField val tabKey: IntArray, // open-addressing table: whole-URL-token hash -> bucket of rule ids
    @JvmField val tabStart: IntArray,
    @JvmField val tabLen: IntArray, // 0 = empty slot
    @JvmField val ruleIds: IntArray,
    @JvmField val gramKey: IntArray, // same structure keyed by a 4-char literal gram, for rules with no safe whole token
    @JvmField val gramStart: IntArray,
    @JvmField val gramLen: IntArray,
    @JvmField val gramRuleIds: IntArray,
    @JvmField val noToken: IntArray, // rules with neither (e.g. "a*b*c"): scanned for every request, so keep this tiny
) {
    companion object {
        val EMPTY = PatternTable(
            0, ByteArray(0), IntArray(1), IntArray(0), IntArray(0), LongArray(0),
            IntArray(16), IntArray(16), IntArray(16), IntArray(0), IntArray(16), IntArray(16), IntArray(16), IntArray(0), IntArray(0),
        )
    }
}

class FilterMeta(
    @JvmField val createdAtMillis: Long,
    @JvmField val lists: List<String>,
    @JvmField val hostRules: Int,
    @JvmField val hostExceptions: Int,
    @JvmField val patternRules: Int,
    @JvmField val siteRules: Int,
    @JvmField val cosmeticSite: Int,
    @JvmField val cosmeticGeneric: Int,
) {
    val totalNetwork: Int get() = hostRules + hostExceptions + patternRules + siteRules
    companion object { val EMPTY = FilterMeta(0L, emptyList(), 0, 0, 0, 0, 0, 0) }
}

/**
 * Immutable compiled filter set. Thread-safe: build once, publish via a volatile reference, call [check] from any thread.
 *
 * Request evaluation order (ABP/uBO semantics): a `$important` block wins; otherwise a matching exception (`@@`) allows
 * the request; otherwise a matching block rule blocks it. Block rules are looked up first; exceptions are only searched
 * once something would be blocked, so the common "nothing matches" case does the minimum work.
 */
class FilterSet internal constructor(
    internal val hosts: HostTable,
    internal val hostExceptions: HostTable,
    internal val siteHash: LongArray, // sorted
    internal val siteFlags: IntArray,
    internal val patterns: PatternTable,
    internal val cosmetic: CosmeticIndex,
    @JvmField val meta: FilterMeta,
) {
    private class Ctx(
        @JvmField val url: String,
        @JvmField val host: String,
        @JvmField val hostStart: Int,
        @JvmField val hostEnd: Int,
        @JvmField val pageHost: String?,
        @JvmField val typeMask: Int,
        @JvmField val thirdParty: Boolean,
    ) {
        @JvmField var blocked = false
        @JvmField var important = false
        @JvmField var excepted = false
        @JvmField var category = 0
        private var pageHashes: LongArray? = null

        fun pageSuffixHashes(): LongArray {
            var a = pageHashes
            if (a == null) {
                a = suffixHashes(pageHost)
                pageHashes = a
            }
            return a
        }
    }

    /**
     * @param url full request URL
     * @param pageHost lower-case host of the top-level page (null if unknown / this IS the page)
     * @param typeMask [RequestType] bits (use [RequestType.infer])
     * @return a [Verdict] value
     */
    fun check(url: String, pageHost: String?, typeMask: Int, psl: Psl = Psl.default): Int {
        val ctx = makeCtx(url, pageHost, typeMask, psl) ?: return Verdict.NONE

        scanHosts(hosts, ctx, exception = false)
        if (!ctx.important) scanPatterns(ctx)
        if (!ctx.blocked) return Verdict.NONE
        if (ctx.important) return Verdict.BLOCK or Verdict.IMPORTANT or (ctx.category shl 2)
        if (!ctx.excepted) scanHosts(hostExceptions, ctx, exception = true)
        return if (ctx.excepted) Verdict.EXCEPTED else Verdict.BLOCK or (ctx.category shl 2)
    }

    /** True if a `@@||host^$document` rule says Shields must be off for pages on [pageHost]. */
    fun isPageAllowed(pageHost: String): Boolean = (siteFlagsFor(pageHost) and SITE_DOCUMENT_ALLOW) != 0

    /**
     * CSS that hides ad containers on [pageHost] (site-specific rules + a capped generic set), or "".
     * Honors `$elemhide` / `$generichide` / `$document` exceptions and `#@#` cosmetic exceptions.
     */
    fun cosmeticCss(pageHost: String, includeGeneric: Boolean = true, genericLimit: Int = CosmeticIndex.DEFAULT_GENERIC_LIMIT): String {
        if (cosmetic.isEmpty) return ""
        val flags = siteFlagsFor(pageHost)
        if ((flags and (SITE_ELEMHIDE or SITE_DOCUMENT_ALLOW)) != 0) return ""
        return cosmetic.cssFor(pageHost, includeGeneric && (flags and SITE_GENERICHIDE) == 0, genericLimit)
    }

    // ------------------------------------------------------------------ internals

    private fun siteFlagsFor(host: String): Int {
        if (siteHash.isEmpty()) return 0
        var flags = 0
        for (h in suffixHashes(host)) {
            val i = Hash.firstIndexOf(siteHash, h)
            if (i >= 0) { var k = i; while (k < siteHash.size && siteHash[k] == h) { flags = flags or siteFlags[k]; k++ } }
        }
        return flags
    }

    private fun scanHosts(t: HostTable, ctx: Ctx, exception: Boolean) {
        if (t.hashes.isEmpty()) return
        val h = ctx.host
        var start = 0
        while (true) {
            val hash = Hash.h64(h, start, h.length)
            var idx = Hash.firstIndexOf(t.hashes, hash)
            if (idx >= 0) {
                while (idx < t.hashes.size && t.hashes[idx] == hash) {
                    val f = t.flags[idx]
                    if (flagsApply(f, ctx)) {
                        if (exception) { ctx.excepted = true; return }
                        ctx.blocked = true
                        ctx.category = (f ushr Bits.CAT_SHIFT) and 3
                        if ((f and Bits.IMPORTANT) != 0) { ctx.important = true; return }
                    }
                    idx++
                }
            }
            val dot = h.indexOf('.', start)
            if (dot < 0) break
            start = dot + 1 // walk down to the bare TLD too: lists do contain TLD rules such as ||zip^
        }
    }

    private fun flagsApply(f: Int, ctx: Ctx): Boolean {
        if ((f and Bits.TYPES and ctx.typeMask) == 0) return false
        val party = (f ushr Bits.PARTY_SHIFT) and 3
        if (party == 1 && !ctx.thirdParty) return false
        if (party == 2 && ctx.thirdParty) return false
        return true
    }

    private fun scanPatterns(ctx: Ctx) {
        val t = patterns
        if (t.count == 0) return
        val url = ctx.url
        val n = url.length
        var i = 0
        var tokens = 0
        while (i < n) {
            val c = url[i]
            if (isAlnum(c)) {
                var j = i + 1
                while (j < n && isAlnum(url[j])) j++
                val len = j - i
                if (len in MIN_TOKEN..MAX_TOKEN_LEN) {
                    scanBucket(t, t.tabKey, t.tabStart, t.tabLen, t.ruleIds, Hash.token32(url, i, j), ctx)
                    if (ctx.important) return
                    if (++tokens >= MAX_URL_TOKENS) break
                }
                i = j
            } else {
                i++
            }
        }
        if (t.gramRuleIds.isNotEmpty() && n >= 4) {
            for (g in 0..n - 4) {
                scanBucket(t, t.gramKey, t.gramStart, t.gramLen, t.gramRuleIds, Hash.gram(url, g), ctx)
                if (ctx.important) return
            }
        }
        for (id in t.noToken) {
            evalRule(t, id, ctx)
            if (ctx.important) return
        }
    }

    private fun scanBucket(t: PatternTable, keys: IntArray, starts: IntArray, lens: IntArray, ids: IntArray, key: Int, ctx: Ctx) {
        val mask = keys.size - 1
        var slot = Hash.slot(key, mask)
        while (true) {
            val len = lens[slot]
            if (len == 0) return
            if (keys[slot] == key) {
                val st = starts[slot]
                for (k in st until st + len) {
                    evalRule(t, ids[k], ctx)
                    if (ctx.important) return
                }
                return
            }
            slot = (slot + 1) and mask
        }
    }

    private fun evalRule(t: PatternTable, id: Int, ctx: Ctx) {
        val w = t.meta[id]
        if ((w and Bits.TYPES and ctx.typeMask) == 0) return
        val party = (w ushr Bits.PARTY_SHIFT) and 3
        if (party == 1 && !ctx.thirdParty) return
        if (party == 2 && ctx.thirdParty) return
        val isExc = (w and Bits.EXCEPTION) != 0
        if (isExc) { if (ctx.excepted) return } else if (ctx.blocked && (w and Bits.IMPORTANT) == 0) return
        val d = t.dom[id]
        if (d != 0 && !domainOk(t.domPool, d - 1, ctx)) return
        if (!matchPattern(t, id, w, ctx)) return
        if (isExc) {
            ctx.excepted = true
        } else {
            ctx.blocked = true
            ctx.category = (w ushr Bits.CAT_SHIFT) and 3
            if ((w and Bits.IMPORTANT) != 0) ctx.important = true
        }
    }

    private fun domainOk(pool: LongArray, off: Int, ctx: Ctx): Boolean {
        val nInc = pool[off].toInt()
        val nExc = pool[off + 1].toInt()
        val ph = ctx.pageSuffixHashes()
        if (nInc > 0) {
            var ok = false
            for (k in 0 until nInc) if (contains(ph, pool[off + 2 + k])) { ok = true; break }
            if (!ok) return false
        }
        for (k in 0 until nExc) if (contains(ph, pool[off + 2 + nInc + k])) return false
        return true
    }

    private fun matchPattern(t: PatternTable, id: Int, w: Int, ctx: Ctx): Boolean {
        val s = t.patOff[id]
        val e = t.patOff[id + 1]
        if (s == e) return true // options-only rule
        val url = ctx.url
        val right = (w and Bits.RIGHT_ANCHOR) != 0
        val pb = t.patBytes
        if ((w and Bits.LEFT_HOST) != 0) {
            var p = ctx.hostStart
            val he = ctx.hostEnd
            while (true) {
                if (glob(pb, s, e, url, p, right)) return true
                val dot = url.indexOf('.', p)
                if (dot < 0 || dot + 1 >= he) return false
                p = dot + 1
            }
        }
        if ((w and Bits.LEFT_ANCHOR) != 0) return glob(pb, s, e, url, 0, right)
        val first = pb[s].toInt() and 0xFF
        if (first != STAR && first != CARET) {
            var p = url.indexOf(first.toChar())
            while (p >= 0) {
                if (glob(pb, s, e, url, p, right)) return true
                p = url.indexOf(first.toChar(), p + 1)
            }
            return false
        }
        for (p in 0..url.length) if (glob(pb, s, e, url, p, right)) return true
        return false
    }

    companion object {
        val EMPTY = FilterSet(HostTable.EMPTY, HostTable.EMPTY, LongArray(0), IntArray(0), PatternTable.EMPTY, CosmeticIndex.EMPTY, FilterMeta.EMPTY)

        private const val STAR = '*'.code
        private const val CARET = '^'.code
        private const val MIN_TOKEN = 3
        private const val MAX_TOKEN_LEN = 64
        private const val MAX_URL_TOKENS = 256

        /** URLs are inspected up to this length: bounds worst-case matching cost against adversarial long URLs. */
        const val MAX_URL = 2048

        internal fun isAlnum(c: Char): Boolean = (c in 'a'..'z') || (c in '0'..'9')

        /** ABP separator: anything except letters, digits and `_ - . %`. */
        private fun isSeparator(c: Char): Boolean = !(isAlnum(c) || c == '_' || c == '-' || c == '.' || c == '%')

        private fun contains(sortedOrNot: LongArray, key: Long): Boolean {
            for (v in sortedOrNot) if (v == key) return true
            return false
        }

        /** Hashes of [host] and EVERY parent suffix down to the bare TLD ("a.b.com" -> a.b.com, b.com, com). */
        internal fun suffixHashes(host: String?): LongArray {
            if (host.isNullOrEmpty()) return LongArray(0)
            var count = 1
            for (c in host) if (c == '.') count++
            val out = LongArray(count)
            var n = 0
            var start = 0
            while (true) {
                out[n++] = Hash.h64(host, start, host.length)
                val dot = host.indexOf('.', start)
                if (dot < 0) break
                start = dot + 1
            }
            return if (n == out.size) out else out.copyOf(n)
        }

        /** Glob match of pattern bytes [ps, pe) against [url] starting at [start]; `*` = any run, `^` = separator or end. */
        internal fun glob(pat: ByteArray, ps: Int, pe: Int, url: String, start: Int, rightAnchor: Boolean): Boolean {
            val n = url.length
            var p = ps
            var u = start
            var starP = -1
            var starU = -1
            while (true) {
                if (p == pe) {
                    if (!rightAnchor || u == n) return true
                    if (starP < 0) return false
                    starU++
                    if (starU > n) return false
                    u = starU; p = starP + 1
                    continue
                }
                val pc = pat[p].toInt() and 0xFF
                if (pc == STAR) { starP = p; starU = u; p++; continue }
                if (pc == CARET) {
                    if (u == n) { p++; continue }
                    if (isSeparator(url[u])) { p++; u++; continue }
                } else if (u < n && url[u].code == pc) {
                    p++; u++; continue
                }
                if (starP < 0) return false
                starU++
                if (starU > n) return false
                u = starU; p = starP + 1
            }
        }

        private fun makeCtx(rawUrl: String, pageHost: String?, typeMask: Int, psl: Psl): Ctx? {
            var s = if (rawUrl.length > MAX_URL) rawUrl.substring(0, MAX_URL) else rawUrl
            s = s.lowercase()
            // Only real network schemes are filtered; data:, blob:, about:, javascript:, file: ... never reach the network.
            var hs = when {
                s.startsWith("https://") -> 8
                s.startsWith("http://") -> 7
                s.startsWith("wss://") -> 6
                s.startsWith("ws://") -> 5
                else -> return null
            }
            var authEnd = hs
            while (authEnd < s.length) {
                val c = s[authEnd]
                if (c == '/' || c == '?' || c == '#') break
                authEnd++
            }
            val at = s.lastIndexOf('@', authEnd - 1)
            if (at >= hs) hs = at + 1
            var he = authEnd
            if (hs < authEnd && s[hs] == '[') {
                val rb = s.indexOf(']', hs)
                if (rb in hs until authEnd) he = rb + 1
            } else {
                val colon = s.lastIndexOf(':', authEnd - 1)
                if (colon > hs && colon < authEnd && allDigits(s, colon + 1, authEnd)) he = colon
            }
            if (he <= hs) return null
            // FQDN trailing dots ("ads.example.com.") must not dodge anchored patterns
            var trimmed = he
            while (trimmed > hs && s[trimmed - 1] == '.') trimmed--
            if (trimmed != he) {
                s = s.substring(0, trimmed) + s.substring(he)
                he = trimmed
            }
            val host = s.substring(hs, he)
            val third = typeMask != RequestType.DOCUMENT && pageHost != null && !psl.isSameSite(host, pageHost)
            return Ctx(s, host, hs, he, pageHost, typeMask, third)
        }

        private fun allDigits(s: String, from: Int, to: Int): Boolean {
            if (from >= to) return true
            for (i in from until to) if (s[i] !in '0'..'9') return false
            return true
        }
    }
}
