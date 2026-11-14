package app.kestrel.core.filter

/**
 * Element-hiding rules, compact: per-domain selector groups in a sorted-hash table backed by one byte pool each, plus the
 * generic selectors. All selectors passed [CosmeticSelectors.accept] at build time.
 */
internal class CosmeticIndex(
    @JvmField val siteHash: LongArray, // sorted, unique
    @JvmField val siteOff: IntArray, // siteHash.size + 1 offsets into sitePool
    @JvmField val sitePool: ByteArray, // selectors of one domain joined by '\n' (UTF-8)
    @JvmField val excHash: LongArray,
    @JvmField val excOff: IntArray,
    @JvmField val excPool: ByteArray,
    @JvmField val genericPool: ByteArray,
    @JvmField val genericCount: Int,
    @JvmField val genericExcPool: ByteArray,
) {
    val isEmpty: Boolean get() = siteHash.isEmpty() && genericCount == 0

    @Volatile private var genericCssCache: String? = null

    /**
     * @param tokenMode D2 (the Firefox build's extension): the generic selectors that name a class or an id are NOT part of this CSS; the page sends the classes and ids it has ([genericCssForTokens])
     * and only the selectors that can match are injected. This CSS then carries the site's own rules, the generic selectors that name neither (attribute selectors, tags: at most [genericLimit]), and
     * the [TOKEN_MARKER] that tells the page script to start sending tokens.
     */
    fun cssFor(host: String, includeGeneric: Boolean, genericLimit: Int, tokenMode: Boolean = false, scriptlets: Boolean = false): String {
        val domainExc = domainExceptions(host)
        val sb = StringBuilder()
        val site = group(host, siteHash, siteOff, sitePool)
        if (tokenMode && includeGeneric && genericCount > 0 && tokens().byKey.isNotEmpty()) sb.append(TOKEN_MARKER)
        val siteRules = if (domainExc.isEmpty()) site else site.filter { it !in domainExc }
        if (tokenMode && siteRules.any { ProceduralRules.isStored(it) }) sb.append(ProceduralRules.MARKER) // the page script asks for the procedural rules of this host (AD1b-4)
        if (tokenMode && scriptlets) { // AW1b: the calls for the page-world library ride in a comment right after the markers (the page script cuts it out before injecting the CSS)
            val payload = ScriptletRules.payload(scriptletsOf(siteRules, domainExc))
            if (payload.isNotEmpty()) sb.append(ScriptletRules.MARKER_START).append(payload).append(ScriptletRules.MARKER_END)
        }
        sb.append(CosmeticSelectors.toCss(siteRules, 1))
        if (includeGeneric && genericCount > 0 && tokenMode) {
            sb.append(looseCss(genericLimit, globalExceptions() + domainExc))
        } else if (includeGeneric && genericCount > 0) {
            if (domainExc.isEmpty() && genericLimit == DEFAULT_GENERIC_LIMIT) {
                var c = genericCssCache
                if (c == null) { c = buildGeneric(genericLimit, globalExceptions()); genericCssCache = c }
                sb.append(c)
            } else {
                sb.append(buildGeneric(genericLimit, globalExceptions() + domainExc))
            }
        }
        return sb.toString()
    }

    /** The scriptlet calls of [host] in stored form ([ScriptletRules]), minus the ones a `#@#+js(...)` rule excepts; none at all when the site has `#@#+js()`. */
    fun scriptletsFor(host: String): List<String> {
        val domainExc = domainExceptions(host)
        return scriptletsOf(group(host, siteHash, siteOff, sitePool), domainExc)
    }

    private fun scriptletsOf(siteRules: List<String>, domainExc: Set<String>): List<String> {
        if (ScriptletRules.ALL_STORED in domainExc) return emptyList()
        var out: ArrayList<String>? = null
        for (s in siteRules) if (ScriptletRules.isStored(s) && s !in domainExc && s != ScriptletRules.ALL_STORED) {
            val o = out ?: ArrayList<String>(4).also { out = it }
            if (s !in o) o.add(s)
        }
        return out ?: emptyList()
    }

    /** The procedural rules of [host] (stored form without the leading marker character), minus the ones a `#@#` rule excepts. */
    fun proceduralFor(host: String): List<String> {
        val domainExc = domainExceptions(host)
        val out = ArrayList<String>(4)
        for (s in group(host, siteHash, siteOff, sitePool)) if (ProceduralRules.isStored(s) && s !in domainExc) out.add(s.substring(1))
        return out
    }

    private fun buildGeneric(limit: Int, exc: Set<String>): String {
        val all = decode(genericPool, 0, genericPool.size)
        val picked = ArrayList<String>(minOf(limit, all.size))
        for (s in all) {
            if (picked.size >= limit) break
            if (s !in exc) picked.add(s)
        }
        return CosmeticSelectors.toCss(picked, GENERIC_CHUNK)
    }

    // ------------------------------------------------------------------ D2: the generic selectors by the class / id they need

    /** The generic selectors as lines of [genericPool] ([lo]..[hi]), filed under the rarest class/id each one requires; [loose] = those that name no class or id at all (or are selector lists). */
    private class TokenIndex(val lo: IntArray, val hi: IntArray, val byKey: HashMap<String, IntArray>, val loose: IntArray, val looseTag: IntArray)
    @Volatile private var tokenIndex: TokenIndex? = null
    @Volatile private var looseCssCache: String? = null

    /** Built once, on first use (about 30,000 short selectors); [warm] lets the app pay for it before the first page asks. */
    private fun tokens(): TokenIndex {
        tokenIndex?.let { return it }
        synchronized(this) {
            tokenIndex?.let { return it }
            val pool = genericPool
            val los = ArrayList<Int>(genericCount + 16); val his = ArrayList<Int>(genericCount + 16)
            var s = 0
            for (i in 0..pool.size) {
                if (i == pool.size || pool[i] == '\n'.code.toByte()) { if (i > s) { los.add(s); his.add(i) }; s = i + 1 }
            }
            val n = los.size
            val keys = arrayOfNulls<List<String>>(n)
            val freq = HashMap<String, Int>(n)
            for (k in 0 until n) {
                val ks = CosmeticTokens.keysOf(String(pool, los[k], his[k] - los[k], Charsets.UTF_8))
                keys[k] = ks
                for (key in ks) freq[key] = (freq[key] ?: 0) + 1
            }
            val lists = HashMap<String, ArrayList<Int>>(freq.size)
            val loose = ArrayList<Int>(); val looseTag = ArrayList<Int>()
            for (k in 0 until n) {
                val ks = keys[k]!!
                if (ks.isEmpty()) {
                    loose.add(k)
                    // led by a tag name (`a[href^=...]`, `ins[...]`): the browser tests only elements of that tag. Led by `[` / `*` / `:` it would be tested against EVERY element of EVERY page: not injected up front
                    val c = pool[los[k]].toInt().toChar()
                    if (c != '[' && c != '*' && c != ':') looseTag.add(k)
                    continue
                }
                var best = ks[0]; var bestN = freq[best] ?: Int.MAX_VALUE
                for (j in 1 until ks.size) { val c = freq[ks[j]] ?: Int.MAX_VALUE; if (c < bestN) { best = ks[j]; bestN = c } }
                lists.getOrPut(best) { ArrayList(2) }.add(k)
            }
            val byKey = HashMap<String, IntArray>(lists.size * 2)
            for ((key, l) in lists) byKey[key] = l.toIntArray()
            val t = TokenIndex(los.toIntArray(), his.toIntArray(), byKey, loose.toIntArray(), looseTag.toIntArray())
            tokenIndex = t
            return t
        }
    }

    /** Builds the token index now (start-up warm-up), so the first page does not wait for it. */
    fun warm() { if (genericCount > 0) tokens() }

    /** Every [step]-th key of the index (tests: real keys to build a page from). */
    fun debugKeys(step: Int): List<String> { val t = tokens(); return t.byKey.keys.sorted().filterIndexed { i, _ -> i % step == 0 } }

    /** One line of numbers about the token index (tests and Diagnostics): how many generic selectors, how many are filed under a token, the biggest bucket. */
    fun debugStats(): String {
        if (genericCount == 0) return "generic=0"
        val t = tokens()
        var keyed = 0; var biggest = 0
        for (v in t.byKey.values) { keyed += v.size; if (v.size > biggest) biggest = v.size }
        return "generic=" + t.lo.size + " keyed=" + keyed + " keys=" + t.byKey.size + " loose=" + t.loose.size + " (tag-led, injected up front: " + t.looseTag.size + ") biggestBucket=" + biggest
    }

    private fun looseCss(limit: Int, exc: Set<String>): String {
        val cached = looseCssCache
        if (cached != null && exc.isEmpty() && limit == DEFAULT_GENERIC_LIMIT) return cached
        val t = tokens()
        val picked = ArrayList<String>(minOf(limit, t.looseTag.size))
        for (k in t.looseTag) {
            if (picked.size >= limit) break
            val sel = String(genericPool, t.lo[k], t.hi[k] - t.lo[k], Charsets.UTF_8)
            if (sel !in exc) picked.add(sel)
        }
        val css = CosmeticSelectors.toCss(picked, GENERIC_CHUNK)
        if (exc.isEmpty() && limit == DEFAULT_GENERIC_LIMIT) looseCssCache = css
        return css
    }

    /**
     * CSS for the generic selectors that can match a page holding the classes and ids in [pageTokens] (".class" / "#id", any case; anything else is ignored), minus the selectors a `#@#` rule
     * excepts for [host], at most [max] selectors.
     */
    fun genericCssForTokens(host: String, pageTokens: Collection<String>, max: Int): String {
        if (genericCount == 0 || pageTokens.isEmpty()) return ""
        val t = tokens()
        val exc = globalExceptions() + domainExceptions(host)
        val picked = LinkedHashSet<Int>()
        for (raw in pageTokens) {
            val key = CosmeticTokens.normalise(raw) ?: continue
            val hits = t.byKey[key] ?: continue
            for (k in hits) { picked.add(k); if (picked.size >= max) break }
            if (picked.size >= max) break
        }
        if (picked.isEmpty()) return ""
        val out = ArrayList<String>(picked.size)
        for (k in picked) {
            val sel = String(genericPool, t.lo[k], t.hi[k] - t.lo[k], Charsets.UTF_8)
            if (sel !in exc) out.add(sel)
        }
        return CosmeticSelectors.toCss(out, GENERIC_CHUNK)
    }

    private fun globalExceptions(): Set<String> =
        if (genericExcPool.isEmpty()) emptySet() else decode(genericExcPool, 0, genericExcPool.size).toHashSet()

    private fun domainExceptions(host: String): Set<String> {
        if (excHash.isEmpty()) return emptySet()
        val l = group(host, excHash, excOff, excPool)
        return if (l.isEmpty()) emptySet() else l.toHashSet()
    }

    /** The hashes a host's rules are filed under: the host and every parent (`a.b.com`, `b.com`, `com`), then its entity keys (AW1d): `www.site.*` and `site.*` for `www.site.co.uk`, whatever the public suffix. */
    private fun hostKeys(host: String): LongArray {
        val base = FilterSet.suffixHashes(host)
        val keys = entityKeys(host) ?: return base
        return base + keys
    }

    private fun group(host: String, hashes: LongArray, offs: IntArray, pool: ByteArray): List<String> {
        if (hashes.isEmpty()) return emptyList()
        var out: ArrayList<String>? = null
        for (h in hostKeys(host)) {
            val i = Hash.firstIndexOf(hashes, h)
            if (i < 0) continue
            val l = decode(pool, offs[i], offs[i + 1])
            if (out == null) out = ArrayList(l) else out.addAll(l)
        }
        return out ?: emptyList()
    }

    companion object {
        /** `www.site.*` and `site.*` for `www.site.co.uk` (the labels before the public suffix, each suffix of them followed by `.*`), hashed like the entity entries of the lists; null for an address that has no public suffix (an IP). */
        internal fun entityKeys(host: String, psl: Psl = Psl.default): LongArray? {
            if (host.isEmpty() || host.indexOf(':') >= 0) return null
            var allNumeric = true
            for (c in host) if (c != '.' && c !in '0'..'9') { allNumeric = false; break }
            if (allNumeric) return null
            val site = psl.siteOf(host) ?: return null
            val dot = site.indexOf('.')
            if (dot <= 0) return null
            val prefixEnd = host.length - (site.length - dot - 1) - 1 // host without ".<public suffix>"
            if (prefixEnd <= 0 || prefixEnd > host.length) return null
            val out = ArrayList<Long>(3)
            var start = 0
            while (true) {
                out.add(Hash.h64(host.substring(start, prefixEnd) + ".*"))
                val d = host.indexOf('.', start)
                if (d < 0 || d + 1 >= prefixEnd) break
                start = d + 1
            }
            return out.toLongArray()
        }

        const val DEFAULT_GENERIC_LIMIT = 1200
        private const val GENERIC_CHUNK = 10
        /** The first thing in the CSS of a page that should send its classes and ids (a CSS comment: harmless to a browser that does not know it). */
        const val TOKEN_MARKER = "/*kg*/"
        /** The most generic selectors injected for one batch of tokens. */
        const val TOKEN_BATCH_MAX = 500

        val EMPTY = CosmeticIndex(LongArray(0), IntArray(1), ByteArray(0), LongArray(0), IntArray(1), ByteArray(0), ByteArray(0), 0, ByteArray(0))

        fun decode(pool: ByteArray, from: Int, to: Int): List<String> {
            if (to <= from) return emptyList()
            return String(pool, from, to - from, Charsets.UTF_8).split('\n').filter { it.isNotEmpty() }
        }
    }
}
