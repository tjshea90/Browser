package app.kestrel.core.input.glide

/**
 * What the strip offers while the person TAPS (N4, owner: "include word suggestions even if i don't glide type"): [words] best first (completions of the letters typed so far, fixes of a typo, or,
 * when nothing is being typed, the likely next words), [typedKnown] (the letters typed are a word the keyboard or the person knows), and [autoCorrect], the one word that a space may put in the place of
 * the typed one (null almost always: only a typo nobody could mean otherwise, see [Suggester.AUTO_MARGIN]).
 */
class Suggestions(val words: List<String>, val typedKnown: Boolean, val autoCorrect: String?, val typed: String) {
    companion object { val NONE = Suggestions(emptyList(), true, null, "") }
}

/**
 * The language side of tap typing (plan 8): one pure function over the shipped word list and what this person has learned, so it is tested without a device and runs on the engine's thread.
 * The score of a word is the same blend the glide decoder uses (how common it is, plus the person's own use and recent use, mixed with how likely it is after the last one or two words), so what
 * the keyboard learns from typing makes glides better and the other way round. A typo fix pays a price in nats that depends on the kind of slip (swapped letters are cheapest, then a neighbouring
 * key, then a missing or extra letter) and that a correction the person made before lowers.
 */
object Suggester {
    /** How much of the probability comes from the word before (the decoder uses 0.4; a suggestion has no finger to lean on, so the context counts for more). */
    const val CONTEXT_MIX = 0.75
    const val SWAP = 1.8f
    const val NEIGHBOUR = 2.2f
    const val MISSING = 2.8f
    const val EXTRA = 2.8f
    /** A letter left out or put in that the word has next to it already (adress, realy, tomorow, occurrred): the slip people make most, so it costs less than other missing or extra letters. */
    const val MISSING_DOUBLE = 2.1f
    const val EXTRA_DOUBLE = 2.1f
    const val FAR = 4.2f
    /** A fix is applied by a space only when it beats the next best word by this many nats (about 2.7 times as likely). */
    const val AUTO_MARGIN = 1.0f
    /** A contraction replaces a real word spelled the same only when it is this many nats (about 20 times) more likely. */
    const val CONTRACTION_MARGIN = 3.0f
    /** Words the person used this many times are theirs: never "corrected". */
    const val OWN_WORD_USES = 2f
    private const val POOL = 48
    /** A rare word (see [Rarity]) loses this many nats per nat its probability is below the rarity floor, so a completion, a next word or a typo fix is never a rare word while a common one fits; a word the person uses has the mass of its own use added first and so is not rare. */
    const val RARE_W = 0.7f
    /** A typed word this improbable (ln) is not trusted as "known" on its own: a one-slip fix that is far more likely may replace it (still needs the usual margin; Backspace puts it back). Words the person typed twice or more are theirs. */
    const val DEEP_TAIL = -16f

    private class Cand(val text: String, val lower: String, var score: Float, val fix: Boolean, val bonus: Float)

    fun suggest(main: BinaryLexicon, personal: PersonalModel?, keys: GlideKeys?, typed: String, prev: String?, prev2: String?, limit: Int = 3): Suggestions {
        val t = typed.lowercase()
        if (t.isEmpty()) return nextWords(main, personal, prev, prev2, limit)
        if (!t.all { it in 'a'..'z' || it == '\'' } || t.startsWith("'")) return Suggestions(emptyList(), true, null, typed)
        val pm = personal?.takeIf { !it.isEmpty }
        val total = main.totalCount
        val prevId = if (prev.isNullOrEmpty()) -1 else main.idOf(prev)
        val found = HashMap<String, Cand>()
        fun offer(text: String, score: Float, fix: Boolean, bonus: Float = 0f) {
            val l = text.lowercase()
            val old = found[l]
            if (old == null) found[l] = Cand(text, l, score, fix, bonus)
            else if (score > old.score) { old.score = score }
        }

        // 1. completions: the most common words that start with the letters typed (a short list is enough: the person's own words are added below)
        val range = main.prefixIds(t)
        val topIds = IntArray(POOL); val topLp = FloatArray(POOL) { -Float.MAX_VALUE }; var minAt = 0
        for (id in range) {
            val lp = main.logProb(id)
            if (lp > topLp[minAt]) {
                topLp[minAt] = lp; topIds[minAt] = id
                minAt = 0; for (i in 1 until POOL) if (topLp[i] < topLp[minAt]) minAt = i
            }
        }
        for (i in 0 until POOL) if (topLp[i] > -Float.MAX_VALUE) { val id = topIds[i]; offer(main.text(id), logP(main, pm, id, null, prevId, prev, prev2, total), false) }
        // the words that follow the one before and start with these letters (a pair the shipped list knows can be rarer than the top of the list)
        if (prevId >= 0) for (id in main.successors(prevId)) if (id in range) offer(main.text(id), logP(main, pm, id, null, prevId, prev, prev2, total), false)

        // 2. the person's own words with these letters (also those the shipped list lacks)
        if (pm != null) for (w in pm.completions(t, 12, 1f)) {
            val id = main.idOf(w).takeIf { it >= 0 && main.text(it).lowercase() == w } ?: -1
            val knownToMain = id >= 0
            if (!knownToMain && pm.weightOf(w) < OWN_WORD_USES) continue   // a word typed once that nobody knows may be a slip
            offer(if (knownToMain) main.text(id) else w, logP(main, pm, id, w, prevId, prev, prev2, total), false)
        }

        // 3. typo fixes: one slip away (needs two letters; the keys' positions say which slips are likely)
        val letters = t.all { it in 'a'..'z' }
        if (t.length >= 2 && letters) for (v in variants(t, keys)) {
            val id = main.idOf(v.text)
            if (id < 0) continue
            val shown = main.text(id)
            if (shown.lowercase() == t) continue
            val conf = pm?.confusionWeight(t, shown.lowercase()) ?: 0f
            val bonus = if (conf > 0f) Math.min(2.5f, 1.2f * Math.log(1.0 + conf).toFloat()) else 0f
            offer(shown, logP(main, pm, id, null, prevId, prev, prev2, total) - v.cost + bonus, true, bonus)
        }
        // what the person corrected this exact string to before, even when it is further away than one slip
        if (pm != null) for ((w, c) in pm.correctionsOf(t)) {
            val id = main.idOf(w).takeIf { it >= 0 && main.text(it).lowercase() == w } ?: -1
            val bonus = Math.min(2.5f, 1.2f * Math.log(1.0 + c).toFloat())
            offer(if (id >= 0) main.text(id) else w, logP(main, pm, id, w, prevId, prev, prev2, total) - FAR + bonus, true, bonus)
        }

        val exactId = main.idOf(t)
        val mainKnows = exactId >= 0 && main.text(exactId).lowercase() == t && (main.logProb(exactId) >= DEEP_TAIL || (pm?.weightOf(t) ?: 0f) > 0f)
        val own = pm?.weightOf(t) ?: 0f
        val typedKnown = mainKnows || own >= OWN_WORD_USES
        val ranked = found.values.sortedByDescending { it.score }
        var auto: String? = null
        // the same letters WITH an apostrophe ("dont" -> "don't", "im" -> "I'm", "cant" -> "can't"): people mean the contraction. A real word that happens to be spelled the same ("ill", "were") is only changed when the
        // contraction is about twenty times as likely; one Backspace puts the typed word back and the keyboard never changes it again.
        if (letters) {
            val contraction = ranked.firstOrNull { it.lower != t && it.lower.replace("'", "") == t }
            if (contraction != null) {
                val own = ranked.firstOrNull { it.lower == t }?.score
                if (own == null || contraction.score >= own + CONTRACTION_MARGIN) auto = contraction.text
            }
        }
        if (auto == null && !typedKnown && t.length >= 3 && ranked.isNotEmpty()) {
            val best = ranked[0]
            val margin = best.score - (ranked.getOrNull(1)?.score ?: -1e9f)
            if (best.fix && (margin >= AUTO_MARGIN || best.bonus >= 1.5f)) auto = best.text
        }
        return Suggestions(ranked.take(limit).map { shape(it.text, typed) }, typedKnown, auto?.let { shape(it, typed) }, typed)
    }

    /** What may come next when nothing is being typed: the words the person wrote after the last one or two words, then what the shipped list says follows the last word. */
    fun nextWords(main: BinaryLexicon, personal: PersonalModel?, prev: String?, prev2: String?, limit: Int): Suggestions {
        val pm = personal?.takeIf { !it.isEmpty }
        val total = main.totalCount
        val out = HashMap<String, Float>()
        val shown = HashMap<String, String>()
        if (prev == null) {
            // the start of a sentence: the words the person starts with most, else the usual few
            for (w in (pm?.topWords(6) ?: emptyList())) { val id = main.idOf(w); out[w] = logP(main, pm, id.takeIf { it >= 0 && main.text(it).lowercase() == w } ?: -1, w, -1, null, null, total); shown[w] = w }
            for (w in STARTERS) if (w.lowercase() !in out) { val id = main.idOf(w); if (id >= 0) { out[w.lowercase()] = main.logProb(id) - 1f; shown[w.lowercase()] = main.text(id) } }
        } else {
            val prevId = main.idOf(prev)
            if (pm != null) for (w in pm.nextWords(prev, prev2, 10)) {
                val id = main.idOf(w).takeIf { it >= 0 && main.text(it).lowercase() == w } ?: -1
                if (id < 0 && pm.weightOf(w) < OWN_WORD_USES) continue
                out[w] = logP(main, pm, id, w, prevId, prev, prev2, total); shown[w] = if (id >= 0) main.text(id) else w
            }
            if (prevId >= 0) {
                val s = main.successors(prevId)
                val order = s.sortedByDescending { main.bigramLogProb(prevId, it) }.take(12)
                for (id in order) { val w = main.text(id).lowercase(); if (w !in out) { out[w] = logP(main, pm, id, null, prevId, prev, prev2, total); shown[w] = main.text(id) } }
            }
        }
        val words = out.entries.sortedByDescending { it.value }.take(limit).map { shown[it.key] ?: it.key }
        return Suggestions(words, true, null, "")
    }

    private val STARTERS = listOf("I", "The", "I'm", "It", "We")

    /** ln P(word | the words before): its own share (plus the person's use and recent use), mixed with how likely it is after them. [id] < 0 = not in the shipped list: only what the person wrote counts. */
    private fun logP(main: BinaryLexicon, pm: PersonalModel?, id: Int, own: String?, prevId: Int, prev: String?, prev2: String?, total: Double): Float {
        val text = (if (id >= 0) main.text(id) else own ?: return -30f).lowercase()
        var pu = if (id >= 0) Math.exp(main.logProb(id).toDouble()) else 0.0
        if (pm != null) pu += pm.unigramMass(text, total) + pm.recentMass(text, total)
        var pb = 0.0
        var known = false
        if (prevId >= 0 && id >= 0) { known = true; val b = main.bigramLogProb(prevId, id); if (!b.isNaN()) pb = Math.exp(b.toDouble()) }
        if (pm != null && prev != null) {
            val lam = pm.pairWeight(prev)
            if (lam > 0f) { pb = (1.0 - lam) * pb + lam * pm.pairProb(prev, text); known = true }
            if (prev2 != null) { val lt = pm.tripleWeight(prev2, prev); if (lt > 0f) { pb = (1.0 - lt) * pb + lt * pm.tripleProb(prev2, prev, text); known = true } }
        }
        val p = if (known) (1.0 - CONTEXT_MIX) * pu + CONTEXT_MIX * pb else pu
        if (p <= 0.0) return -30f
        val lp = Math.log(p).toFloat()
        return lp - RARE_W * Rarity.below(lp)
    }

    private class Variant(val text: String, val cost: Float)

    /** Every string one slip away from [t] with the price of that slip: two letters swapped, a letter beside the right one, a letter left out, a letter too many. */
    private fun variants(t: String, keys: GlideKeys?): List<Variant> {
        val out = ArrayList<Variant>(t.length * 40)
        val n = t.length
        for (i in 0 until n - 1) if (t[i] != t[i + 1]) out.add(Variant(t.substring(0, i) + t[i + 1] + t[i] + t.substring(i + 2), SWAP))
        for (i in 0 until n) out.add(Variant(t.substring(0, i) + t.substring(i + 1), if ((i > 0 && t[i - 1] == t[i]) || (i + 1 < n && t[i + 1] == t[i])) EXTRA_DOUBLE else EXTRA))      // one letter too many
        for (i in 0..n) for (c in 'a'..'z') out.add(Variant(t.substring(0, i) + c + t.substring(i), if ((i > 0 && t[i - 1] == c) || (i < n && t[i] == c)) MISSING_DOUBLE else MISSING))  // a letter left out
        for (i in 0 until n) {
            val a = t[i]
            for (c in 'a'..'z') if (c != a) out.add(Variant(t.substring(0, i) + c + t.substring(i + 1), if (near(keys, a, c)) NEIGHBOUR else if (n >= 4 && i > 0) FAR else Float.MAX_VALUE))
        }
        return out.filter { it.cost < Float.MAX_VALUE }
    }

    /** True when the keys of [a] and [b] touch (or nearly): the thumb may have meant one and hit the other. */
    private fun near(keys: GlideKeys?, a: Char, b: Char): Boolean {
        if (keys == null || a !in 'a'..'z' || b !in 'a'..'z') return false
        val dx = keys.cx[a - 'a'] - keys.cx[b - 'a']; val dy = keys.cy[a - 'a'] - keys.cy[b - 'a']
        return dx * dx + dy * dy <= 2.25f * keys.pitch * keys.pitch
    }

    /** The word dressed like what was typed: Capital first letter after a capital, all capitals after capitals. */
    private fun shape(word0: String, typed: String): String {
        val word = if (word0 == "i" || word0.startsWith("i'")) "I" + word0.substring(1) else word0        // "i'm" is always "I'm"
        if (typed.length > 1 && typed.all { it.isUpperCase() || !it.isLetter() } && typed.any { it.isUpperCase() }) return word.uppercase()
        if (typed.firstOrNull()?.isUpperCase() == true) return word.replaceFirstChar { it.uppercase() }
        return word
    }
}
