package app.kestrel.core.input.glide

/**
 * R18a, owner 2026-11-18: "if i press on any word that i already typed, even several words ago, put suggested words in the top to change the word to, in case it is an error. the suggested words should be words
 * with close glide typing patterns."
 *
 * The words to offer in place of a word that is already in the text. The word is drawn the way a thumb would glide it (straight lines through its letters' keys) and decoded like any glide: that finds the words
 * whose glide looks like this one (a neighbouring key, a letter left out or put in). The tap model's typo fixes (two letters swapped, a doubled letter) join them, because a swap is a typo of the fingers but
 * a long way off as a glide. Every word is then ranked by how close its glide SHAPE is to the written word's ([Fingerprint], in key units), how likely it is after the words before and before the word after
 * (the next word is known here, unlike while typing), and what this person has used and corrected before. The word itself is never offered.
 */
object Alternatives {
    /** Nats lost per key unit of shape distance: one key apart (a clear neighbouring-key slip) costs about as much as a word being 55 times rarer. */
    const val SHAPE_W = 4.0f
    /** Weight of ln P(word | the words before it): the strip's own score (own use, recent use, rarity price included). */
    const val LM_W = 1.0f
    /** Weight of ln P(the word after | this word). */
    const val NEXT_W = 0.30f
    /**
     * A fix the tap model proposes (two letters swapped, a neighbouring key, a letter left out or put in, a correction the person made before) starts ahead by this much if it is the tap model's first choice, less for
     * each place further down: a swap is the commonest slip of the fingers but a long way off as a glide shape.
     */
    const val FIX_BONUS = 2.4f
    const val FIX_STEP = 0.5f
    private const val POOL = 24
    private const val FAR_SHAPE = 3.5f
    private const val UNKNOWN_NEXT = -12f
    /** A tap-model fix is not held to the glide shape beyond this many keys: two swapped letters are a slip of the fingers even when their glide is a long way off. */
    private const val FIX_SHAPE_CAP = 1.0f

    /** Tests: sees every candidate with the parts of its score (shape distance, ln P in context, next-word term, fix bonus). */
    internal var trace: ((String) -> Unit)? = null

    private class Cand(val text: String, var fix: Boolean) { var rank = 0 }

    fun find(
        main: BinaryLexicon, extra: GlideLexicon?, personal: PersonalModel?, decoder: GlideDecoder?, keys: GlideKeys,
        word: String, prev2: String?, prev: String?, next: String?, limit: Int, blocked: (String) -> Boolean = { false },
    ): List<String> {
        val t = word.lowercase()
        if (t.length < 2 || !t.all { it in 'a'..'z' || it == '\'' }) return emptyList()
        val typedShape = Fingerprint.ofWord(t, keys)
        val pool = LinkedHashMap<String, Cand>()
        // 1. the words whose glide looks like this word's
        if (decoder != null && typedShape != null) {
            val path = idealPath(t, keys)
            if (path != null) {
                val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(main); extra?.let { lexicons.add(it) }
                val bx = decoder.biasX; val by = decoder.biasY
                decoder.biasX = 0f; decoder.biasY = 0f                      // (a path drawn exactly is not moved by where this thumb usually lands)
                try { for (c in decoder.decode(path.first, path.second, path.first.size, lexicons, prev, POOL, prev2, relax = false)) pool.getOrPut(c.text.lowercase()) { Cand(c.text.lowercase(), false) } }
                finally { decoder.biasX = bx; decoder.biasY = by }
            }
        }
        // 2. what the tap model would fix it to
        for ((rank, w) in Suggester.suggest(main, personal, keys, t, prev, prev2, 8, fixesOnly = true).words.withIndex()) pool.getOrPut(w.lowercase()) { Cand(w.lowercase(), true) }.also { it.fix = true; it.rank = rank }
        // 3. rank
        val scored = ArrayList<Pair<String, Float>>(pool.size)
        for (c in pool.values) {
            val w = c.text
            if (w == t || w.length < 2 || blocked(w)) continue
            val shape = Fingerprint.ofWord(w, keys)
            val d = if (shape != null && typedShape != null) Fingerprint.distance(typedShape, shape) else FAR_SHAPE
            val lp = Suggester.contextLogP(main, personal, w, prev, prev2)
            val nx = nextTerm(main, personal, w, next); val fb = if (c.fix) Math.max(0.4f, FIX_BONUS - FIX_STEP * c.rank) else 0f
            val sc = -SHAPE_W * (if (c.fix) Math.min(d, FIX_SHAPE_CAP) else d) + LM_W * lp + NEXT_W * nx + fb
            trace?.invoke("%-10s total %6.2f  shape %.2f lp %6.2f next %6.2f fix %.1f".format(w, sc, d, lp, nx, fb))
            scored.add(w to sc)
        }
        return scored.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    /** ln P(next | word), from the shipped pairs and the person's own, floored at a value for "never seen together" so that an unknown pair costs a little and a known one earns a lot. */
    private fun nextTerm(main: BinaryLexicon, pm: PersonalModel?, word: String, next: String?): Float {
        if (next.isNullOrEmpty()) return 0f
        var best = UNKNOWN_NEXT
        val a = main.idOf(word); val b = main.idOf(next)
        if (a >= 0 && b >= 0) { val lp = main.bigramLogProb(a, b); if (!lp.isNaN()) best = Math.max(best, lp) }
        val own = pm?.pairProb(word, next) ?: 0f
        if (own > 0f) best = Math.max(best, Math.log(own.toDouble()).toFloat())
        return best
    }

    /** The word's letters joined by straight lines, a point every tenth of a segment: what a careful glide of it would draw (null with fewer than two different keys in a row). */
    internal fun idealPath(word: String, keys: GlideKeys): Pair<FloatArray, FloatArray>? {
        val k = GlideLetters.keyOf(word)
        val cx = ArrayList<Float>(); val cy = ArrayList<Float>()
        for (b in k) { val i = b.toInt(); if (cx.isEmpty() || cx.last() != keys.cx[i] || cy.last() != keys.cy[i]) { cx.add(keys.cx[i]); cy.add(keys.cy[i]) } }
        if (cx.size < 2) return null
        val steps = 10
        val xs = FloatArray((cx.size - 1) * steps + 1); val ys = FloatArray(xs.size)
        var n = 0
        for (s in 0 until cx.size - 1) for (j in 0 until steps) { val f = j / steps.toFloat(); xs[n] = cx[s] + (cx[s + 1] - cx[s]) * f; ys[n] = cy[s] + (cy[s + 1] - cy[s]) * f; n++ }
        xs[n] = cx.last(); ys[n] = cy.last()
        return xs to ys
    }
}
