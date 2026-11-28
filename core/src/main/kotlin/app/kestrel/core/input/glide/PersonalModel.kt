package app.kestrel.core.input.glide

/**
 * What the keyboard has learned about THIS person's words (AU2, owner message 35: "make the new glide type keyboard learn from my typing. I can choose words that I meant to type if it predicts
 * incorrectly, and over time it will become more accurate by learning my more common word patterns").
 *
 * Two small tables, kept on the tablet and never sent anywhere: how much each word was used, and how much each word followed each other word. The decoder adds them to the shipped list's
 * statistics: a word the person uses a lot gets the probability of a more common word (growing slowly: ten uses make an ordinary word, a hundred a very common one), and a pair the person
 * writes often makes the second word much more likely after the first. Nothing here can take over: the boost has a ceiling and old habits fade (all weights halve when the total gets large).
 */
class PersonalModel(private val capWords: Int = 6000, private val capPairs: Int = 14000, private val capTriples: Int = 10000, private val capConfusions: Int = 3000, private val recency: Boolean = true, private val capCases: Int = 2000) {
    private val words = HashMap<String, Float>()
    private val pairs = HashMap<String, Float>()      // "previous word" -> weight
    private val pairTotals = HashMap<String, Float>() // previous word -> weight of everything seen after it
    private val triples = HashMap<String, Float>()      // "second-last last word" -> weight (AV2: the last TWO words as context)
    private val tripleTotals = HashMap<String, Float>() // "second-last last" -> weight of everything seen after that pair
    private val confusions = HashMap<String, Float>()   // "guessed meant" -> how often the keyboard guessed the first and the person meant the second (AV2: learning from corrections)
    private val recent = HashMap<String, Float>()       // word -> how often and how LATELY it was used (every use of any word fades all of them a little: a short memory next to the long one)
    private val cases = HashMap<String, HashMap<String, Float>>()   // lowercase word -> (the way it was written -> how often): names, places, brands with their capitals, and words the person takes the keyboard's capital back from (R18b)
    private var caseEntries = 0
    private val starts = HashMap<String, Float>()      // word -> how often the person BEGAN a sentence with it (R18d: "good morning", "thanks", "i")
    private var startTotal = 0f
    private var fixMade = 0f; private var fixUndone = 0f   // automatic fixes the keyboard made, and how many of them the person took back with Backspace (R18d: the keyboard gets stricter or bolder)
    private var total = 0f

    /** What this person's glides LOOK like for the words they meant (AX1): kept, saved, faded and forgotten together with the rest of what was learned. */
    val patterns = PatternMemory()

    val wordCount: Int @Synchronized get() = words.size
    val pairCount: Int @Synchronized get() = pairs.size
    val isEmpty: Boolean @Synchronized get() = words.isEmpty()

    /** [word] was kept (or meant), after [prev] when that is known. [weight]: 1 for a word that was kept, more for one the person CHOSE over the keyboard's first guess. */
    @Synchronized fun add(prev: String?, word: String, weight: Float = 1f, prev2: String? = null) {
        val w = norm(word) ?: return
        if (weight <= 0f) return
        words[w] = (words[w] ?: 0f) + weight
        total += weight
        val p = prev?.let { ctx(it) }
        if (p != null) {
            val k = "$p $w"
            pairs[k] = (pairs[k] ?: 0f) + weight
            pairTotals[p] = (pairTotals[p] ?: 0f) + weight
            val p2 = prev2?.let { ctx(it) }
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

    /**
     * The person took a word back (deleted it after it was learned): what [add] gave it is removed again, so a typo that was typed once and erased does not become a favourite. Never below zero.
     */
    @Synchronized fun unlearn(prev: String?, word: String, weight: Float = 1f, prev2: String? = null) {
        val w = norm(word) ?: return
        if (weight <= 0f) return
        val have = words[w] ?: return
        val take = Math.min(weight, have)
        if (have - take < 0.05f) words.remove(w) else words[w] = have - take
        total = Math.max(0f, total - take)
        val p = prev?.let { ctx(it) } ?: return
        val k = "$p $w"
        pairs[k]?.let { v -> val t = Math.min(weight, v); if (v - t < 0.05f) pairs.remove(k) else pairs[k] = v - t; pairTotals[p] = Math.max(0f, (pairTotals[p] ?: 0f) - t) }
        val p2 = prev2?.let { ctx(it) } ?: return
        val tk = "$p2 $p $w"
        triples[tk]?.let { v -> val t = Math.min(weight, v); if (v - t < 0.05f) triples.remove(tk) else triples[tk] = v - t; tripleTotals["$p2 $p"] = Math.max(0f, (tripleTotals["$p2 $p"] ?: 0f) - t) }
    }

    /** Words the person used that start with [prefix] (lowercase; apostrophes allowed), most used (and most lately used) first, only those used at least [minWeight] times: up to [limit]. */
    @Synchronized fun completions(prefix: String, limit: Int, minWeight: Float = 1f): List<String> {
        val out = ArrayList<Pair<String, Float>>()
        for ((k, v) in words) if (v >= minWeight && k.startsWith(prefix)) out.add(k to (v + 3f * (recent[k] ?: 0f)))
        out.sortByDescending { it.second }
        return out.take(limit).map { it.first }
    }

    /** What the person wrote after [prev] (and after the pair [prev2] [prev], counted double), most often first: up to [limit]. */
    @Synchronized fun nextWords(prev: String, prev2: String?, limit: Int): List<String> {
        val p = ctx(prev) ?: return emptyList()
        val acc = HashMap<String, Float>()
        val pre = "$p "
        for ((k, v) in pairs) if (k.startsWith(pre)) acc[k.substring(pre.length)] = v
        val p2 = prev2?.let { ctx(it) }
        if (p2 != null) { val tp = "$p2 $p "; for ((k, v) in triples) if (k.startsWith(tp)) { val w = k.substring(tp.length); acc[w] = (acc[w] ?: 0f) + 2f * v } }
        return acc.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    /** The words used most (and most lately), for a start-of-sentence guess when nothing came before. */
    @Synchronized fun topWords(limit: Int): List<String> = words.entries.sortedByDescending { it.value + 3f * (recent[it.key] ?: 0f) }.take(limit).map { it.key }

    /** The keyboard guessed [guessed] and the person meant [meant] (they picked it from the strip, or deleted the guess and wrote this): the next time [guessed] is the first guess and [meant] is among the others, [meant] gets a lift. */
    @Synchronized fun addConfusion(guessed: String, meant: String, weight: Float = 1f) {
        val a = norm(guessed) ?: return; val b = norm(meant) ?: return
        if (a == b || weight <= 0f) return
        val k = "$a $b"
        confusions[k] = (confusions[k] ?: 0f) + weight
        if (confusions.size > capConfusions) { val cut = confusions.values.sorted()[confusions.size / 10]; val it = confusions.entries.iterator(); while (it.hasNext()) { val e = it.next(); if (e.value <= cut && confusions.size > capConfusions * 9 / 10) it.remove() } }
    }

    /** What the person meant when the keyboard offered or typed [guessed] (or they typed it and corrected it): the words and how often, most often first. */
    @Synchronized fun correctionsOf(guessed: String): List<Pair<String, Float>> {
        val pre = "$guessed "
        val out = ArrayList<Pair<String, Float>>()
        for ((k, v) in confusions) if (k.startsWith(pre)) out.add(k.substring(pre.length) to v)
        out.sortByDescending { it.second }
        return out.take(4)
    }

    /** How often [meant] was meant when [guessed] was the first guess. */
    @Synchronized fun confusionWeight(guessed: String, meant: String): Float = confusions["$guessed $meant"] ?: 0f
    val confusionCount: Int @Synchronized get() = confusions.size
    val caseCount: Int @Synchronized get() = caseEntries

    /**
     * [shown] is the way the person wrote a word INSIDE a sentence (not at its start, where a capital says nothing): "Sarah", "iPhone", or "monday" when they took the keyboard's capital back. After [CASE_MIN] such
     * uses (and not mostly the other way) the word is written that way for them everywhere: [caseForm].
     */
    @Synchronized fun addCase(shown: String, weight: Float = 1f) {
        val lower = shown.lowercase()
        if (weight <= 0f || norm(lower) != lower) return
        val m = cases.getOrPut(lower) { HashMap(2) }
        if (!m.containsKey(shown)) caseEntries++
        m[shown] = Math.min(MAX_WEIGHT, (m[shown] ?: 0f) + weight)
        if (caseEntries > capCases) trimCases()
    }

    /** The way the person writes [lower] (a capital form such as "Sarah"), the empty string when they write it small although the keyboard's list would capitalise it, null when they have no habit yet. */
    @Synchronized fun caseForm(lower: String): String? {
        val m = cases[lower] ?: return null
        val small = m[lower] ?: 0f
        var best: String? = null; var bw = 0f
        for ((f, w) in m) if (f != lower && w > bw) { best = f; bw = w }
        if (best != null && bw >= CASE_MIN && bw >= 1.5f * small) return best
        if (small >= CASE_MIN && small > 1.5f * bw) return ""
        return null
    }

    /** True when the person has any habit recorded for [lower] (the learner then also counts the times they write it small). */
    @Synchronized fun hasCase(lower: String): Boolean = cases.containsKey(lower)

    /** [word] began a sentence (the first word of the box, or the first after . ! ? or a new line). Kept apart from the rest: what people START with is not what they write most. */
    @Synchronized fun addStart(word: String, weight: Float = 1f) {
        val w = ctx(word) ?: return
        if (weight <= 0f) return
        starts[w] = Math.min(MAX_WEIGHT, (starts[w] ?: 0f) + weight); startTotal += weight
        if (starts.size > CAP_STARTS) trimStarts()
    }

    /**
     * The words this person begins sentences with, most often first, each with its share of all their sentence starts (0..1), trusted only as much as there is data: nothing before a few starts, about half after
     * [START_HALF] of them. At most [limit].
     */
    @Synchronized fun startWords(limit: Int): List<Pair<String, Float>> {
        if (startTotal <= 0f) return emptyList()
        val trust = startTotal / (startTotal + START_HALF)
        return starts.entries.sortedByDescending { it.value }.take(limit).map { it.key to trust * it.value / startTotal }
    }
    val startCount: Int @Synchronized get() = starts.size

    private fun trimStarts() {
        val cut = starts.values.sorted()[starts.size / 10]
        val it = starts.entries.iterator()
        while (it.hasNext()) { val e = it.next(); if (e.value <= cut && starts.size > CAP_STARTS * 9 / 10) { startTotal -= e.value; it.remove() } }
        if (startTotal < 0f) startTotal = 0f
    }

    /** An automatic fix was made ([undone] false), or the person took one back with Backspace ([undone] true). */
    @Synchronized fun addFix(undone: Boolean) { if (undone) fixUndone += 1f else fixMade += 1f }

    /**
     * How much stricter (positive, in nats of margin) or bolder (negative) the automatic fix should be for THIS person: it follows the share of the fixes they took back, against an expected one in ten. Someone who
     * undoes a third of them gets a margin about 1.4 nats higher (only the plain slips are fixed); someone who never undoes one gets 0.3 nats lower. Bounded both ways; it fades with everything else that is learned.
     */
    @Synchronized fun autoMarginShift(): Float {
        val made = fixMade + FIX_PRIOR_MADE; val undone = fixUndone + FIX_PRIOR_UNDONE
        return ((undone / made - 0.10f) * 6f).coerceIn(-0.3f, 1.5f)
    }

    private fun trimCases() {
        val all = ArrayList<Float>(caseEntries); for (m in cases.values) all.addAll(m.values)
        all.sort(); val cut = all[all.size / 10]
        val it = cases.entries.iterator()
        while (it.hasNext()) { val e = it.next(); val i2 = e.value.entries.iterator(); while (i2.hasNext()) { val x = i2.next(); if (x.value <= cut && caseEntries > capCases * 9 / 10) { i2.remove(); caseEntries-- } }; if (e.value.isEmpty()) it.remove() }
    }
    val tripleCount: Int @Synchronized get() = triples.size

    /**
     * Brings everything [other] learned into this (an imported file, AX1). A count that is in both stays the LARGER of the two, so importing the same file twice changes nothing, and a file made on
     * another device only adds what this one has not seen (or saw less often); the short memory keeps the larger value; shapes merge like learned ones. A file cannot grow the tables past their
     * caps: the usual trimming runs afterwards.
     */
    @Synchronized fun mergeFrom(other: PersonalModel) {
        if (other === this) return
        synchronized(other) {
            for ((k, v) in other.words) words[k] = Math.max(words[k] ?: 0f, v)
            for ((k, v) in other.pairs) pairs[k] = Math.max(pairs[k] ?: 0f, v)
            for ((k, v) in other.triples) triples[k] = Math.max(triples[k] ?: 0f, v)
            for ((k, v) in other.confusions) confusions[k] = Math.max(confusions[k] ?: 0f, v)
            for ((k, v) in other.recent) recent[k] = Math.max(recent[k] ?: 0f, v)
            for ((k, v) in other.starts) starts[k] = Math.max(starts[k] ?: 0f, v)
            fixMade = Math.max(fixMade, other.fixMade); fixUndone = Math.max(fixUndone, other.fixUndone)
            for ((lower, m) in other.cases) { val mine = cases.getOrPut(lower) { HashMap(2) }; for ((f, w) in m) { if (!mine.containsKey(f)) caseEntries++; mine[f] = Math.max(mine[f] ?: 0f, w) } }
            patterns.mergeFrom(other.patterns)
        }
        total = 0f; for (v in words.values) total += v
        rebuildTotals()
        if (words.size > capWords || pairs.size > capPairs || triples.size > capTriples) trim()
        if (caseEntries > capCases) trimCases()
        startTotal = 0f; for (v in starts.values) startTotal += v
        if (starts.size > CAP_STARTS) trimStarts()
        if (confusions.size > capConfusions) { val cut = confusions.values.sorted()[confusions.size - capConfusions]; val it = confusions.entries.iterator(); while (it.hasNext() && confusions.size > capConfusions) { val e = it.next(); if (e.value < cut) it.remove() } }
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

    @Synchronized fun clear() { words.clear(); pairs.clear(); pairTotals.clear(); triples.clear(); tripleTotals.clear(); confusions.clear(); recent.clear(); cases.clear(); caseEntries = 0; starts.clear(); startTotal = 0f; fixMade = 0f; fixUndone = 0f; total = 0f; patterns.clear() }

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
        val si = starts.entries.iterator(); startTotal = 0f
        while (si.hasNext()) { val e = si.next(); e.setValue(e.value * 0.5f); if (e.value < 0.2f) si.remove() else startTotal += e.value }
        fixMade *= 0.5f; fixUndone *= 0.5f
        val ci = cases.entries.iterator()
        while (ci.hasNext()) { val e = ci.next(); val i2 = e.value.entries.iterator(); while (i2.hasNext()) { val x = i2.next(); x.setValue(x.value * 0.5f); if (x.value < 0.2f) { i2.remove(); caseEntries-- } }; if (e.value.isEmpty()) ci.remove() }
        patterns.decay()
        rebuildTotals()
    }

    private fun rebuildTotals() {
        pairTotals.clear()
        for ((k, v) in pairs) { val p = k.substring(0, k.indexOf(' ')); pairTotals[p] = (pairTotals[p] ?: 0f) + v }
        tripleTotals.clear()
        for ((k, v) in triples) { val p = k.substring(0, k.lastIndexOf(' ')); tripleTotals[p] = (tripleTotals[p] ?: 0f) + v }
    }

    // ------------------------------------------------------------------ saving

    /** One line per entry: `w word weight`, `p previous word weight`, `t second-last last word weight`, `c guessed meant weight`, `r word recency` and `b word weight` (how often the person began a sentence with the word, R18d), `f made undone` (automatic fixes made and taken back, R18d), `k word Form weight` (the way a word is written inside a sentence: a name, a place, or small after the keyboard's capital was taken back, R18b), `s word confirmations u0 v0 ... u15 v15` (the shape of this person's glides for the word, AX1; a file written before AV2 has only the first two kinds, and an older keyboard skips the lines it does not know). */
    @Synchronized fun serialize(): String {
        val sb = StringBuilder(words.size * 16 + pairs.size * 24 + triples.size * 32 + confusions.size * 24 + recent.size * 16)
        for ((k, v) in words) sb.append("w ").append(k).append(' ').append(f2(v)).append('\n')
        for ((k, v) in pairs) sb.append("p ").append(k).append(' ').append(f2(v)).append('\n')
        for ((k, v) in triples) sb.append("t ").append(k).append(' ').append(f2(v)).append('\n')
        for ((k, v) in confusions) sb.append("c ").append(k).append(' ').append(f2(v)).append('\n')
        for ((k, v) in recent) sb.append("r ").append(k).append(' ').append(f3(v)).append('\n')
        for ((k, v) in starts) sb.append("b ").append(k).append(' ').append(f2(v)).append('\n')
        if (fixMade > 0f || fixUndone > 0f) sb.append("f ").append(f2(fixMade)).append(' ').append(f2(fixUndone)).append('\n')
        for ((lower, m) in cases) for ((f, w) in m) sb.append("k ").append(lower).append(' ').append(f).append(' ').append(f2(w)).append('\n')
        sb.append(patterns.serialize())
        return sb.toString()
    }

    companion object {
        /** One use is worth this many occurrences on the shipped list's scale. */
        const val UNIGRAM_COUNT = 2.0e7
        /** Uses beyond this do not count any more (the boost has a ceiling). */
        const val MAX_USES = 200f
        const val MAX_PAIR_TRUST = 0.7f
        const val PAIR_HALF = 4f
        const val DECAY_AT = 15000f
        /** The short memory: every word the person writes fades all the recent ones by this much (a half-life of about 86 words), and a word used lately counts like [RECENT_COUNT] occurrences per use. */
        const val RECENT_FADE = 0.992f
        const val RECENT_COUNT = 1.5e7
        const val MAX_RECENT = 6f
        const val MAX_TRIPLE_TRUST = 0.8f
        const val TRIPLE_HALF = 3f
        /** A way of writing a word counts once it was seen this often inside sentences. */
        const val CASE_MIN = 2f
        const val CAP_STARTS = 600
        /** The share of sentence starts is trusted halfway after this many starts. */
        const val START_HALF = 6f
        /** What the keyboard assumes before it has seen the person: ten fixes made, one taken back. */
        const val FIX_PRIOR_MADE = 10f
        const val FIX_PRIOR_UNDONE = 1f

        /** A word as CONTEXT for the next one: like [norm] but one letter is enough ("a", "i" come before many words). */
        fun ctx(word: String): String? {
            val w = word.lowercase()
            if (w.length !in 1..24 || !w.all { it in 'a'..'z' || it == '\'' } || w.startsWith("'") || w.endsWith("'") || stretched(w)) return null
            return w
        }

        /** A word as the model keeps it: lowercase letters (and the apostrophe of a contraction), 2 to 24 long; null when it is not a word (or is typed noise such as "awwww"). */
        fun norm(word: String): String? {
            val w = word.lowercase()
            if (w.length !in 2..24 || !w.all { it in 'a'..'z' || it == '\'' } || w.startsWith("'") || w.endsWith("'") || stretched(w)) return null
            return w
        }

        /** True when one letter stands three or more times in a row ("awwww", "sooooo", "hmmm"): typed noise or a stretched word, never learned (AV2: the keyboard must not fill with them). */
        fun stretched(w: String): Boolean {
            var run = 1
            for (i in 1 until w.length) { if (w[i] == w[i - 1]) { run++; if (run >= 3) return true } else run = 1 }
            return false
        }

        /** Numbers in the file are always written with a dot, whatever the tablet's language is (a comma would make the file unreadable, and an exported one unportable). */
        internal fun f2(v: Float): String = String.format(java.util.Locale.ROOT, "%.2f", v)
        internal fun f3(v: Float): String = String.format(java.util.Locale.ROOT, "%.3f", v)

        /** The largest weight a line may carry (a damaged or hostile file cannot make one word overwhelm the model). */
        const val MAX_WEIGHT = 100_000f

        /**
         * Reads what [serialize] wrote (or an export file's body). Every kind of line is bounded by its table's cap and every number must be finite and positive, so a damaged or hand-made file can
         * fill the tables only as far as the person's own use could.
         */
        fun parse(text: String, capWords: Int = 6000, capPairs: Int = 14000): PersonalModel {
            val m = PersonalModel(capWords, capPairs)
            synchronized(m) {
                var nw = 0; var np = 0; var nt = 0; var nc = 0; var nr = 0; var nk = 0; var nb = 0
                for (line in text.lineSequence()) {
                    val p = line.trim().split(' ')
                    if (p.size > 3 && p[0] == "s") { m.patterns.parseLine(p); continue } // a shape's last number may be zero or negative: it is not a weight
                    if (p.size == 3 && p[0] == "f") { val a = p[1].toFloatOrNull(); val b = p[2].toFloatOrNull(); if (a != null && b != null && a >= 0f && b >= 0f && a <= MAX_WEIGHT && b <= MAX_WEIGHT) { m.fixMade = a; m.fixUndone = b }; continue }
                    val v = p.lastOrNull()?.toFloatOrNull() ?: continue
                    if (!(v > 0f) || v > MAX_WEIGHT) continue
                    when {
                        p.size == 3 && p[0] == "w" -> { if (nw >= m.capWords) continue; val w = norm(p[1]) ?: continue; m.words[w] = v; m.total += v; nw++ }
                        p.size == 4 && p[0] == "p" -> { if (np >= m.capPairs) continue; val a = ctx(p[1]) ?: continue; val b = norm(p[2]) ?: continue; m.pairs["$a $b"] = v; np++ }
                        p.size == 5 && p[0] == "t" -> { if (nt >= m.capTriples) continue; val a = ctx(p[1]) ?: continue; val b = ctx(p[2]) ?: continue; val c = norm(p[3]) ?: continue; m.triples["$a $b $c"] = v; nt++ }
                        p.size == 4 && p[0] == "c" -> { if (nc >= m.capConfusions) continue; val a = norm(p[1]) ?: continue; val b = norm(p[2]) ?: continue; if (a != b) { m.confusions["$a $b"] = v; nc++ } }
                        p.size == 4 && p[0] == "k" -> { if (nk >= m.capCases) continue; val lower = norm(p[1]) ?: continue; val form = p[2]; if (form.lowercase() != lower || form.length > 24 || !form.all { it.isLetter() || it == '\'' }) continue; val mm = m.cases.getOrPut(lower) { HashMap(2) }; if (!mm.containsKey(form)) { m.caseEntries++; nk++ }; mm[form] = v }
                        p.size == 3 && p[0] == "b" -> { if (nb >= CAP_STARTS) continue; val w = ctx(p[1]) ?: continue; m.starts[w] = v; m.startTotal += v; nb++ }
                        p.size == 3 && p[0] == "r" -> { if (nr >= m.capWords) continue; val w = norm(p[1]) ?: continue; m.recent[w] = v; nr++ }
                    }
                }
                m.rebuildTotals()
            }
            return m
        }
    }
}
