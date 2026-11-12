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
    fun cssFor(host: String, includeGeneric: Boolean, genericLimit: Int, tokenMode: Boolean = false): String {
        val domainExc = domainExceptions(host)
        val sb = StringBuilder()
        val site = group(host, siteHash, siteOff, sitePool)
        if (tokenMode && includeGeneric && genericCount > 0 && tokens().byKey.isNotEmpty()) sb.append(TOKEN_MARKER)
        sb.append(CosmeticSelectors.toCss(if (domainExc.isEmpty()) site else site.filter { it !in domainExc }, 1))
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
    private class TokenIndex(val lo: IntArray, val hi: IntArray, val byKey: HashMap<String, IntArray>, val loose: IntArray)
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
            val loose = ArrayList<Int>()
            for (k in 0 until n) {
                val ks = keys[k]!!
                if (ks.isEmpty()) { loose.add(k); continue }
                var best = ks[0]; var bestN = freq[best] ?: Int.MAX_VALUE
                for (j in 1 until ks.size) { val c = freq[ks[j]] ?: Int.MAX_VALUE; if (c < bestN) { best = ks[j]; bestN = c } }
                lists.getOrPut(best) { ArrayList(2) }.add(k)
            }
            val byKey = HashMap<String, IntArray>(lists.size * 2)
            for ((key, l) in lists) byKey[key] = l.toIntArray()
            val t = TokenIndex(los.toIntArray(), his.toIntArray(), byKey, loose.toIntArray())
            tokenIndex = t
            return t
        }
    }

    /** Builds the token index now (start-up warm-up), so the first page does not wait for it. */
    fun warm() { if (genericCount > 0) tokens() }

    private fun looseCss(limit: Int, exc: Set<String>): String {
        val cached = looseCssCache
        if (cached != null && exc.isEmpty() && limit == DEFAULT_GENERIC_LIMIT) return cached
        val t = tokens()
        val picked = ArrayList<String>(minOf(limit, t.loose.size))
        for (k in t.loose) {
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

    private fun group(host: String, hashes: LongArray, offs: IntArray, pool: ByteArray): List<String> {
        if (hashes.isEmpty()) return emptyList()
        var out: ArrayList<String>? = null
        for (h in FilterSet.suffixHashes(host)) {
            val i = Hash.firstIndexOf(hashes, h)
            if (i < 0) continue
            val l = decode(pool, offs[i], offs[i + 1])
            if (out == null) out = ArrayList(l) else out.addAll(l)
        }
        return out ?: emptyList()
    }

    companion object {
        const val DEFAULT_GENERIC_LIMIT = 1200
        private const val GENERIC_CHUNK = 10

        val EMPTY = CosmeticIndex(LongArray(0), IntArray(1), ByteArray(0), LongArray(0), IntArray(1), ByteArray(0), ByteArray(0), 0, ByteArray(0))

        fun decode(pool: ByteArray, from: Int, to: Int): List<String> {
            if (to <= from) return emptyList()
            return String(pool, from, to - from, Charsets.UTF_8).split('\n').filter { it.isNotEmpty() }
        }
    }
}
