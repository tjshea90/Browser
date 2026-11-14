package app.kestrel.core.input.glide

/**
 * What the keyboard has learned about THIS person's words (AU2, owner message 35: "make the new glide type keyboard learn from my typing. I can choose words that I meant to type if it predicts
 * incorrectly, and over time it will become more accurate by learning my more common word patterns").
 *
 * Two small tables, kept on the tablet and never sent anywhere: how much each word was used, and how much each word followed each other word. The decoder adds them to the shipped list's
 * statistics: a word the person uses a lot gets the probability of a more common word (growing slowly: ten uses make an ordinary word, a hundred a very common one), and a pair the person
 * writes often makes the second word much more likely after the first. Nothing here can take over: the boost has a ceiling and old habits fade (all weights halve when the total gets large).
 */
class PersonalModel(private val capWords: Int = 4000, private val capPairs: Int = 8000, private val capTriples: Int = 6000, private val capConfusions: Int = 3000, private val recency: Boolean = true) {
    private val words = HashMap<String, Float>()
    private val pairs = HashMap<String, Float>()      // "previous word" -> weight
    private val pairTotals = HashMap<String, Float>() // previous word -> weight of everything seen after it
    private val triples = HashMap<String, Float>()      // "second-last last word" -> weight (AV2: the last TWO words as context)
    private val tripleTotals = HashMap<String, Float>() // "second-last last" -> weight of everything seen after that pair
    private val confusions = HashMap<String, Float>()   // "guessed meant" -> how often the keyboard guessed the first and the person meant the second (AV2: learning from corrections)
    private val recent = HashMap<String, Float>()       // word -> how often and how LATELY it was used (every use of any word fades all of them a little: a short memory next to the long one)
    private var total = 0f

    val wordCount: Int @Synchronized get() = words.size
    val pairCount: Int @Synchronized get() = pairs.size
    val isEmpty: Boolean @Synchronized get() = words.isEmpty()

    /** [word] was kept (or meant), after [prev] when that is known. [weight]: 1 for a word that was kept, more for one the person CHOSE over the keyboard's first guess. */
    @Synchronized fun add(prev: String?, word: String, weight: Float = 1f, prev2: String? = null) {
        val w = norm(word) ?: return
        if (weight <= 0f) return
        words[w] = (words[w] ?: 0f) + weight
        total += weight
        val p = prev?.let { norm(it) }
        if (p != null) {
            val k = "$p $w"
            pairs[k] = (pairs[k] ?: 0f) + weight
            pairTotals[p] = (pairTotals[p] ?: 0f) + weight
            val p2 = prev2?.let { norm(it) }
            if (p2 != null) {
                val t = "$p2 $p $w"
                triples[t] = (triples[t] ?: 0f) + weight
                tripleTotals["$p2 $p"] = (tripleTotals["$p2 $p"] ?: 0f) + weight
            }
        }
        if (recency) { // the short memory: everything fades, this word was just used
            val it = recent.entries.iterator()
            while (it.hasNext()) { val e = it.next(); e.setValue(e.value * RECENT_FADE); if (e.value < 0.02f) it.remove() }
            recent[w] = (recent[w] ?: 0f) + 1f
        }
        if (words.size > capWords || pairs.size > capPairs || triples.size > capTriples) trim()
        if (total > DECAY_AT) decay()
    }

    /** The keyboard guessed [guessed] and the person meant [meant] (they picked it from the strip, or deleted the guess and wrote this): the next time [guessed] is the first guess and [meant] is among the others, [meant] gets a lift. */
    @Synchronized fun addConfusion(guessed: String, meant: String, weight: Float = 1f) {
        val a = norm(guessed) ?: return; val b = norm(meant) ?: return
        if (a == b || weight <= 0f) return
        val k = "$a $b"
        confusions[k] = (confusions[k] ?: 0f) + weight
        if (confusions.size > capConfusions) { val cut = confusions.values.sorted()[confusions.size / 10]; val it = confusions.entries.iterator(); while (it.hasNext()) { val e = it.next(); if (e.value <= cut && confusions.size > capConfusions * 9 / 10) it.remove() } }
    }

    /** How often [meant] was meant when [guessed] was the first guess. */
    @Synchronized fun confusionWeight(guessed: String, meant: String): Float = confusions["$guessed $meant"] ?: 0f
    val confusionCount: Int @Synchronized get() = confusions.size
    val tripleCount: Int @Synchronized get() = triples.size

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

    /** Extra probability for a word used lately, on the same scale as [unigramMass]: a few uses in the last hundred words are worth about as much as one long-term use. */
    @Synchronized fun recentMass(word: String, listTotal: Double): Double {
        val r = recent[word] ?: return 0.0
        return RECENT_COUNT * Math.min(r, MAX_RECENT).toDouble() / listTotal
    }

    /** The share of what the person wrote after the two words [prev2] [prev] that was [word] (0 when never seen). */
    @Synchronized fun tripleProb(prev2: String, prev: String, word: String): Float {
        val n = tripleTotals["$prev2 $prev"] ?: return 0f
        if (n <= 0f) return 0f
        return (triples["$prev2 $prev $word"] ?: 0f) / n
    }

    /** How much to trust the person's own triples after the pair [prev2] [prev]: nothing before it was seen, half after a few times, never more than [MAX_TRIPLE_TRUST]. */
    @Synchronized fun tripleWeight(prev2: String, prev: String): Float {
        val n = tripleTotals["$prev2 $prev"] ?: return 0f
        return Math.min(MAX_TRIPLE_TRUST, n / (n + TRIPLE_HALF))
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

    @Synchronized fun clear() { words.clear(); pairs.clear(); pairTotals.clear(); triples.clear(); tripleTotals.clear(); confusions.clear(); recent.clear(); total = 0f }

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
        if (triples.size > capTriples) {
            val cut = triples.values.sorted()[triples.size / 10]
            val it = triples.entries.iterator(); while (it.hasNext()) { val e = it.next(); if (e.value <= cut && triples.size > capTriples * 9 / 10) it.remove() }
            rebuildTotals()
        }
    }

    /** Old habits fade: everything counts half, what fell under a fifth of a use is forgotten. */
    private fun decay() {
        val w = words.entries.iterator(); total = 0f
        while (w.hasNext()) { val e = w.next(); e.setValue(e.value * 0.5f); if (e.value < 0.2f) w.remove() else total += e.value }
        val p = pairs.entries.iterator()
        while (p.hasNext()) { val e = p.next(); e.setValue(e.value * 0.5f); if (e.value < 0.2f) p.remove() }
        val t = triples.entries.iterator()
        while (t.hasNext()) { val e = t.next(); e.setValue(e.value * 0.5f); if (e.value < 0.2f) t.remove() }
        val c = confusions.entries.iterator()
        while (c.hasNext()) { val e = c.next(); e.setValue(e.value * 0.7f); if (e.value < 0.2f) c.remove() } // a correction is remembered a little longer than a word count
        rebuildTotals()
    }

    private fun rebuildTotals() {
        pairTotals.clear()
        for ((k, v) in pairs) { val p = k.substring(0, k.indexOf(' ')); pairTotals[p] = (pairTotals[p] ?: 0f) + v }
        tripleTotals.clear()
        for ((k, v) in triples) { val p = k.substring(0, k.lastIndexOf(' ')); tripleTotals[p] = (tripleTotals[p] ?: 0f) + v }
    }

    // ------------------------------------------------------------------ saving

    /** One line per entry: `w word weight`, `p previous word weight`, `t second-last last word weight`, `c guessed meant weight` and `r word recency` (a file written before AV2 has only the first two kinds). */
    @Synchronized fun serialize(): String {
        val sb = StringBuilder(words.size * 16 + pairs.size * 24 + triples.size * 32 + confusions.size * 24 + recent.size * 16)
        for ((k, v) in words) sb.append("w ").append(k).append(' ').append("%.2f".format(v)).append('\n')
        for ((k, v) in pairs) sb.append("p ").append(k).append(' ').append("%.2f".format(v)).append('\n')
        for ((k, v) in triples) sb.append("t ").append(k).append(' ').append("%.2f".format(v)).append('\n')
        for ((k, v) in confusions) sb.append("c ").append(k).append(' ').append("%.2f".format(v)).append('\n')
        for ((k, v) in recent) sb.append("r ").append(k).append(' ').append("%.3f".format(v)).append('\n')
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
        /** The short memory: every word the person writes fades all the recent ones by this much (a half-life of about 86 words), and a word used lately counts like [RECENT_COUNT] occurrences per use. */
        const val RECENT_FADE = 0.992f
        const val RECENT_COUNT = 1.5e7
        const val MAX_RECENT = 6f
        const val MAX_TRIPLE_TRUST = 0.8f
        const val TRIPLE_HALF = 3f

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
                        p.size == 5 && p[0] == "t" -> { val a = norm(p[1]) ?: continue; val b = norm(p[2]) ?: continue; val c = norm(p[3]) ?: continue; m.triples["$a $b $c"] = v }
                        p.size == 4 && p[0] == "c" -> { val a = norm(p[1]) ?: continue; val b = norm(p[2]) ?: continue; if (a != b) m.confusions["$a $b"] = v }
                        p.size == 3 && p[0] == "r" -> { val w = norm(p[1]) ?: continue; m.recent[w] = v }
                    }
                }
                m.rebuildTotals()
            }
            return m
        }
    }
}
