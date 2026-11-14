package app.kestrel.core.input.glide

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The words a glide can turn into (AT1, owner message 34: "glide typing like Gboard ... make the glide word predictions accurate"). A word is a run of letters 0..25 ('a'..'z', the keys the finger
 * passes over) with a probability: the decoder asks only for these few things, so the big list and the small personal one can sit behind the same interface and neither has to build a String per word.
 */
interface GlideLexicon {
    val size: Int
    /** Number of letters of word [id]. */
    fun length(id: Int): Int
    /** Letter [i] of word [id], 0..25. */
    fun letter(id: Int, i: Int): Int
    /** ln of the word's share of all words (always below 0). */
    fun logProb(id: Int): Float
    /** What gets typed (an apostrophe word like "don't" has the key sequence of "dont"). */
    fun text(id: Int): String
    /** The ids of the words that begin with letter [first] and end with letter [last]. Never null; do not modify. */
    fun bucket(first: Int, last: Int): IntArray
    /** The id of [word] (exactly as typed), or -1. */
    fun idOf(word: String): Int
    /** ln P(word [id] follows word [prevId] in THIS lexicon), or NaN when the pair is not known. */
    fun bigramLogProb(prevId: Int, id: Int): Float
}

object GlideLetters {
    /** The letters of [s] as 0..25 (everything that is not a-z is dropped: the apostrophe of "don't"). */
    fun keyOf(s: String): ByteArray {
        var n = 0
        for (ch in s) { val c = ch.lowercaseChar(); if (c in 'a'..'z') n++ }
        val out = ByteArray(n)
        var i = 0
        for (ch in s) { val c = ch.lowercaseChar(); if (c in 'a'..'z') out[i++] = (c - 'a').toByte() }
        return out
    }

    fun compare(a: ByteArray, b: ByteArray): Int {
        val n = minOf(a.size, b.size)
        for (i in 0 until n) { val d = a[i] - b[i]; if (d != 0) return d }
        return a.size - b.size
    }
}

/** A small lexicon held in plain arrays: the words the person typed or the sites they visit, and the lists the tests build. No bigrams. */
class ArrayLexicon(entries: List<Pair<String, Long>>) : GlideLexicon {
    private val texts: Array<String>
    private val keys: Array<ByteArray>
    private val lp: FloatArray
    private val byText = HashMap<String, Int>()
    private val buckets = HashMap<Int, IntArray>()

    init {
        val merged = LinkedHashMap<String, Long>()
        for ((w, c) in entries) { if (GlideLetters.keyOf(w).isEmpty() || c <= 0L) continue; merged[w] = maxOf(c, merged[w] ?: 0L) }
        texts = merged.keys.toTypedArray()
        keys = Array(texts.size) { GlideLetters.keyOf(texts[it]) }
        val total = merged.values.sum().toDouble().coerceAtLeast(1.0)
        lp = FloatArray(texts.size) { Math.log(merged[texts[it]]!! / total).toFloat() }
        val lists = HashMap<Int, ArrayList<Int>>()
        for (i in texts.indices) {
            byText[texts[i]] = i
            val k = keys[i]
            lists.getOrPut(k[0] * 26 + k[k.size - 1]) { ArrayList() }.add(i)
        }
        for ((b, l) in lists) buckets[b] = l.toIntArray()
    }

    override val size get() = texts.size
    override fun length(id: Int) = keys[id].size
    override fun letter(id: Int, i: Int) = keys[id][i].toInt()
    override fun logProb(id: Int) = lp[id]
    override fun text(id: Int) = texts[id]
    override fun bucket(first: Int, last: Int): IntArray = buckets[first * 26 + last] ?: EMPTY
    override fun idOf(word: String) = byText[word] ?: -1
    override fun bigramLogProb(prevId: Int, id: Int) = Float.NaN

    companion object { val EMPTY = IntArray(0) }
}

/**
 * The shipped word list, read straight from the file's bytes into a few flat arrays (about 3 MB for 80 000 words and their pairs; no object per word). Words are stored in alphabetical order of
 * their keys, so a word is found by binary search and the file needs no index.
 */
class BinaryLexicon internal constructor(
    private val len: ByteArray,
    private val blob: ByteArray,
    private val lp: ShortArray,
    private val displayIds: IntArray,
    private val displayTexts: Array<String>,
    private val biOff: IntArray,
    private val biId: IntArray,
    private val biLp: ShortArray,
) : GlideLexicon {
    private val off = IntArray(len.size + 1)
    private val buckets: Array<IntArray>

    init {
        for (i in len.indices) off[i + 1] = off[i] + len[i]
        val counts = IntArray(26 * 26)
        for (i in len.indices) counts[blob[off[i]] * 26 + blob[off[i + 1] - 1]]++
        buckets = Array(26 * 26) { IntArray(counts[it]) }
        val fill = IntArray(26 * 26)
        for (i in len.indices) { val b = blob[off[i]] * 26 + blob[off[i + 1] - 1]; buckets[b][fill[b]++] = i }
    }

    override val size get() = len.size
    override fun length(id: Int) = len[id].toInt()
    override fun letter(id: Int, i: Int) = blob[off[id] + i].toInt()
    override fun logProb(id: Int) = lp[id] / SCALE
    override fun bucket(first: Int, last: Int): IntArray = buckets[first * 26 + last]

    override fun text(id: Int): String {
        val d = java.util.Arrays.binarySearch(displayIds, id)
        if (d >= 0) return displayTexts[d]
        val n = len[id].toInt(); val o = off[id]
        val sb = StringBuilder(n)
        for (i in 0 until n) sb.append('a' + blob[o + i].toInt())
        return sb.toString()
    }

    private fun keyAt(id: Int) = blob.copyOfRange(off[id], off[id + 1])

    /** The first id whose key is >= [key] (alphabetical), len.size when none. */
    private fun lowerBound(key: ByteArray): Int {
        var lo = 0; var hi = len.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compareKey(mid, key) < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun compareKey(id: Int, key: ByteArray): Int {
        val o = off[id]; val n = len[id].toInt()
        val m = minOf(n, key.size)
        for (i in 0 until m) { val d = blob[o + i] - key[i]; if (d != 0) return d }
        return n - key.size
    }

    override fun idOf(word: String): Int {
        val key = GlideLetters.keyOf(word)
        if (key.isEmpty()) return -1
        var i = lowerBound(key)
        var first = -1
        while (i < len.size && compareKey(i, key) == 0) {
            if (first < 0) first = i
            if (text(i) == word) return i
            i++
        }
        return first
    }

    override fun bigramLogProb(prevId: Int, id: Int): Float {
        if (prevId < 0 || prevId >= len.size) return Float.NaN
        var lo = biOff[prevId]; var hi = biOff[prevId + 1] - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val v = biId[mid]
            if (v < id) lo = mid + 1 else if (v > id) hi = mid - 1 else return biLp[mid] / SCALE
        }
        return Float.NaN
    }

    val bigramCount: Int get() = biId.size

    companion object { const val SCALE = 128f }
}

/**
 * The file format ("KGL1", big-endian) and the code that writes it from plain text lists, so what the compile tool writes and what the tablet reads are tested together.
 *
 *   "KGL1" | n | blobLen | nDisplay | nPairs | len[n] bytes | blob (letters 0..25) | logProb[n] shorts (1/128 nat) | display: (id int, len byte, utf8) x nDisplay |
 *   pairOff[n+1] ints | pairId[nPairs] ints | pairLogProb[nPairs] shorts (ln P(second | first) over the listed pairs)
 */
object LexiconFile {
    class Entry(val text: String, val count: Long)
    class Pair(val first: String, val second: String, val count: Long)

    private const val MAGIC = 0x4B474C31 // "KGL1"
    private const val SCALE = BinaryLexicon.SCALE

    private fun q(lnp: Double): Short = Math.round(lnp * SCALE).coerceIn(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()).toShort()

    fun build(entries: List<Entry>, pairs: List<Pair>): ByteArray {
        // one entry per exact text (the biggest count wins), a-z keys only, in alphabetical order of key then text
        val best = HashMap<String, Long>()
        for (e in entries) { val k = GlideLetters.keyOf(e.text); if (k.isEmpty() || k.size > 30 || e.count <= 0L) continue; best[e.text] = maxOf(e.count, best[e.text] ?: 0L) }
        val keyed = best.keys.map { it to GlideLetters.keyOf(it) }.sortedWith { a, b -> val c = GlideLetters.compare(a.second, b.second); if (c != 0) c else a.first.compareTo(b.first) }
        val n = keyed.size
        val idOfText = HashMap<String, Int>(n * 2)
        for ((i, p) in keyed.withIndex()) idOfText[p.first] = i
        val total = best.values.sum().toDouble()

        var blobLen = 0
        for (p in keyed) blobLen += p.second.size
        val display = ArrayList<Int>()
        for ((i, p) in keyed.withIndex()) if (!p.first.all { it in 'a'..'z' }) display.add(i)

        // pairs: only between known words; P(second | first) over the pairs that were listed
        class P(val a: Int, val b: Int, val c: Long)
        val ps = ArrayList<P>()
        for (p in pairs) { val a = idOfText[p.first] ?: continue; val b = idOfText[p.second] ?: continue; if (p.count > 0L) ps.add(P(a, b, p.count)) }
        ps.sortWith(compareBy<P> { it.a }.thenBy { it.b })
        val sumFor = LongArray(n)
        for (p in ps) sumFor[p.a] += p.c
        val pairOff = IntArray(n + 1)
        for (p in ps) pairOff[p.a + 1]++
        for (i in 0 until n) pairOff[i + 1] += pairOff[i]

        val out = java.io.ByteArrayOutputStream(blobLen + n * 4 + ps.size * 6 + 4096)
        val d = java.io.DataOutputStream(out)
        d.writeInt(MAGIC); d.writeInt(n); d.writeInt(blobLen); d.writeInt(display.size); d.writeInt(ps.size)
        for (p in keyed) d.writeByte(p.second.size)
        for (p in keyed) d.write(p.second)
        for (p in keyed) d.writeShort(q(Math.log(best[p.first]!! / total)).toInt())
        for (id in display) { val bytes = keyed[id].first.toByteArray(Charsets.UTF_8); d.writeInt(id); d.writeByte(bytes.size); d.write(bytes) }
        for (i in 0..n) d.writeInt(pairOff[i])
        for (p in ps) d.writeInt(p.b)
        for (p in ps) d.writeShort(q(Math.log(p.c.toDouble() / sumFor[p.a])).toInt())
        d.flush()
        return out.toByteArray()
    }

    fun read(bytes: ByteArray): BinaryLexicon {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        require(b.int == MAGIC) { "not a glide word list" }
        val n = b.int; val blobLen = b.int; val nDisplay = b.int; val nPairs = b.int
        val len = ByteArray(n); b.get(len)
        val blob = ByteArray(blobLen); b.get(blob)
        val lp = ShortArray(n); b.asShortBuffer().get(lp); b.position(b.position() + n * 2)
        val dIds = IntArray(nDisplay); val dTexts = Array(nDisplay) { "" }
        for (i in 0 until nDisplay) { dIds[i] = b.int; val l = b.get().toInt() and 0xFF; val t = ByteArray(l); b.get(t); dTexts[i] = String(t, Charsets.UTF_8) }
        val pairOff = IntArray(n + 1); b.asIntBuffer().get(pairOff); b.position(b.position() + (n + 1) * 4)
        val pairId = IntArray(nPairs); b.asIntBuffer().get(pairId); b.position(b.position() + nPairs * 4)
        val pairLp = ShortArray(nPairs); b.asShortBuffer().get(pairLp)
        return BinaryLexicon(len, blob, lp, dIds, dTexts, pairOff, pairId, pairLp)
    }
}
