package app.kestrel.core.filter

/** 64-bit string hashing used for host sets and token buckets (FNV-1a core + murmur3 finaliser for avalanche). */
internal object Hash {
    private const val FNV_OFFSET = -3750763034362895579L // 0xcbf29ce484222325
    private const val FNV_PRIME = 1099511628211L
    private val M1 = 0xff51afd7ed558ccdUL.toLong()
    private val M2 = 0xc4ceb9fe1a85ec53UL.toLong()

    @JvmStatic
    fun h64(s: CharSequence, from: Int, to: Int): Long {
        var h = FNV_OFFSET
        var i = from
        while (i < to) {
            h = (h xor s[i].code.toLong()) * FNV_PRIME
            i++
        }
        h = h xor (h ushr 33); h *= M1
        h = h xor (h ushr 33); h *= M2
        h = h xor (h ushr 33)
        return h
    }

    @JvmStatic
    fun h64(s: CharSequence): Long = h64(s, 0, s.length)

    /** 32-bit token hash for the rule index. */
    @JvmStatic
    fun token32(s: CharSequence, from: Int, to: Int): Int {
        val h = h64(s, from, to)
        return (h xor (h ushr 32)).toInt()
    }

    /** Index of the first element equal to [key] in a sorted array, or -1. */
    @JvmStatic
    fun firstIndexOf(sorted: LongArray, key: Long): Int {
        var lo = 0
        var hi = sorted.size - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val v = sorted[mid]
            if (v < key) lo = mid + 1
            else {
                if (v == key) found = mid
                hi = mid - 1
            }
        }
        return found
    }
}
