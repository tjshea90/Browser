package app.kestrel.core.input.glide

/**
 * What the keyboard has learned about THIS person's words (AU2, owner message 35: "make the new glide type keyboard learn from my typing. I can choose words that I meant to type if it predicts
 * incorrectly, and over time it will become more accurate by learning my more common word patterns").
 *
 * Two small tables, kept on the tablet and never sent anywhere: how much each word was used, and how much each word followed each other word. The decoder adds them to the shipped list's
 * statistics: a word the person uses a lot gets the probability of a more common word (growing slowly: ten uses make an ordinary word, a hundred a very common one), and a pair the person
 * writes often makes the second word much more likely after the first. Nothing here can take over: the boost has a ceiling and old habits fade (all weights halve when the total gets large).
 */
class PersonalModel(private val capWords: Int = 4000, private val capPairs: Int = 8000) {
    private val words = HashMap<String, Float>()
    private val pairs = HashMap<String, Float>()      // "previous word" -> weight
    private val pairTotals = HashMap<String, Float>() // previous word -> weight of everything seen after it
    private var total = 0f

    val wordCount: Int @Synchronized get() = words.size
    val pairCount: Int @Synchronized get() = pairs.size
    val isEmpty: Boolean @Synchronized get() = words.isEmpty()

    /** [word] was kept (or meant), after [prev] when that is known. [weight]: 1 for a word that was kept, more for one the person CHOSE over the keyboard's first guess. */
    @Synchronized fun add(prev: String?, word: String, weight: Float = 1f) {
        val w = norm(word) ?: return
        if (weight <= 0f) return
        words[w] = (words[w] ?: 0f) + weight
        total += weight
        val p = prev?.let { norm(it) }
        if (p != null) {
            val k = "$p $w"
            pairs[k] = (pairs[k] ?: 0f) + weight
            pairTotals[p] = (pairTotals[p] ?: 0f) + weight
        }
        if (words.size > capWords || pairs.size > capPairs) trim()
        if (total > DECAY_AT) decay()
    }

    /** How much the person uses [word] (0 when never). */
    @Synchronized fun weightOf(word: String): Float = words[norm(word) ?: return 0f] ?: 0f

    /**
     * Extra probability for [word] on the scale of a list whose counts add up to [listTotal]: every use is worth [UNIGRAM_COUNT] occurrences of that list, up to [MAX_USES] uses.
     * (A word used once is about as likely as the 10 000th most common one; ten uses, the 1 500th; a hundred, the 150th.)
     */
    @Synchronized fun unigramMass(word: String, listTotal: Double): Double {
        val w = words[word] ?: return 0.0
        return UNIGRAM_COUNT * Math.min(w, MAX_USES).toDouble() / listTotal
    }

    /** The share of what the person wrote after [prev] that was [word] (0 when the pair was never seen). */
    @Synchronized fun pairProb(prev: String, word: String): Float {
        val n = pairTotals[prev] ?: return 0f
        if (n <= 0f) return 0f
        return (pairs["$prev $word"] ?: 0f) / n
    }

    /** How much to trust the person's own pairs after [prev]: nothing before it was seen, half after a few times, never more than [MAX_PAIR_TRUST]. */
    @Synchronized fun pairWeight(prev: String): Float {
        val n = pairTotals[prev] ?: return 0f
        return Math.min(MAX_PAIR_TRUST, n / (n + PAIR_HALF))
    }

    @Synchronized fun clear() { words.clear(); pairs.clear(); pairTotals.clear(); total = 0f }

    // ------------------------------------------------------------------ keeping it small

    /** Too many entries: the least used tenth goes. */
    private fun trim() {
        if (words.size > capWords) {
            val cut = words.values.sorted()[words.size / 10]
            val it = words.entries.iterator(); while (it.hasNext()) { val e = it.next(); if (e.value <= cut && words.size > capWords * 9 / 10) { total -= e.value; it.remove() } }
        }
        if (pairs.size > capPairs) {
            val cut = pairs.values.sorted()[pairs.size / 10]
            val it = pairs.entries.iterator(); while (it.hasNext()) { val e = it.next(); if (e.value <= cut && pairs.size > capPairs * 9 / 10) it.remove() }
            rebuildTotals()
        }
    }

    /** Old habits fade: everything counts half, what fell under a fifth of a use is forgotten. */
    private fun decay() {
        val w = words.entries.iterator(); total = 0f
        while (w.hasNext()) { val e = w.next(); e.setValue(e.value * 0.5f); if (e.value < 0.2f) w.remove() else total += e.value }
        val p = pairs.entries.iterator()
        while (p.hasNext()) { val e = p.next(); e.setValue(e.value * 0.5f); if (e.value < 0.2f) p.remove() }
        rebuildTotals()
    }

    private fun rebuildTotals() {
        pairTotals.clear()
        for ((k, v) in pairs) { val p = k.substring(0, k.indexOf(' ')); pairTotals[p] = (pairTotals[p] ?: 0f) + v }
    }

    // ------------------------------------------------------------------ saving

    /** One line per entry: `w word weight` and `p previous word weight`. */
    @Synchronized fun serialize(): String {
        val sb = StringBuilder(words.size * 16 + pairs.size * 24)
        for ((k, v) in words) sb.append("w ").append(k).append(' ').append("%.2f".format(v)).append('\n')
        for ((k, v) in pairs) sb.append("p ").append(k).append(' ').append("%.2f".format(v)).append('\n')
        return sb.toString()
    }

    companion object {
        /** One use is worth this many occurrences on the shipped list's scale. */
        const val UNIGRAM_COUNT = 2.0e7
        /** Uses beyond this do not count any more (the boost has a ceiling). */
        const val MAX_USES = 200f
        const val MAX_PAIR_TRUST = 0.7f
        const val PAIR_HALF = 4f
        const val DECAY_AT = 6000f

        /** A word as the model keeps it: lowercase letters (and the apostrophe of a contraction), 2 to 24 long; null when it is not a word. */
        fun norm(word: String): String? {
            val w = word.lowercase()
            if (w.length !in 2..24 || !w.all { it in 'a'..'z' || it == '\'' } || w.startsWith("'") || w.endsWith("'")) return null
            return w
        }

        fun parse(text: String, capWords: Int = 4000, capPairs: Int = 8000): PersonalModel {
            val m = PersonalModel(capWords, capPairs)
            synchronized(m) {
                for (line in text.lineSequence()) {
                    val p = line.trim().split(' ')
                    val v = p.lastOrNull()?.toFloatOrNull() ?: continue
                    if (v <= 0f) continue
                    when {
                        p.size == 3 && p[0] == "w" -> { val w = norm(p[1]) ?: continue; m.words[w] = v; m.total += v }
                        p.size == 4 && p[0] == "p" -> { val a = norm(p[1]) ?: continue; val b = norm(p[2]) ?: continue; m.pairs["$a $b"] = v }
                    }
                }
                m.rebuildTotals()
            }
            return m
        }
    }
}
