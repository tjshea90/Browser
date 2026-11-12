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
