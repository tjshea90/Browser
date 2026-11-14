package app.kestrel.core.input.glide

import java.util.Random

/**
 * AX1 (owner message 38): "I often type the word for yet Gboard often inputs the word fir ... I always change fir to for but it still glide types fir."
 *
 * The question this lab answers with numbers: for a PERSON whose hand draws the same sloppy shape every time for a word (a hairpin turn that stops short, a thumb that lands on the neighbour
 * key), how many corrections does the keyboard need before it stops giving the wrong word, and how accurate is it over a long stretch of use? Nothing here is real data: it is a seeded simulated
 * hand (see [GestureSim]); the numbers are only as honest as that, and the tablet's Diagnostics line is the real measure.
 *
 *   java -cp core/build/classes/kotlin/main:core/build/classes/kotlin/test:<kotlin-stdlib jar> app.kestrel.core.input.glide.GlideHabitEval for
 */
object GlideHabitEval {
    /** A hand with a habit per word: the same shift of each letter's aim point every time that word is glided (drawn once, on first use), on top of the usual scatter of [noise]. */
    class Hand(private val keys: GlideKeys, private val noise: SimNoise, seed: Long, private val habitDp: Float, private val fixed: Map<String, List<FloatArray?>> = emptyMap()) {
        private val sim = GestureSim(keys, Random(seed))
        private val rnd = Random(seed * 977 + 13)
        private val shifts = HashMap<String, List<FloatArray?>>()

        private fun shiftFor(word: String): List<FloatArray?> = shifts.getOrPut(word) {
            fixed[word] ?: run {
                val m = distinctLetters(word)
                List(m) { floatArrayOf(rnd.nextGaussian().toFloat() * habitDp, rnd.nextGaussian().toFloat() * habitDp) }
            }
        }

        fun path(word: String): GestureSim.Path? = sim.path(word, noise, shiftFor(word))
    }

    fun distinctLetters(word: String): Int { var m = 0; var p = -1; for (b in GlideLetters.keyOf(word)) { if (b.toInt() != p) { m++; p = b.toInt() } }; return m }

    private fun keyOf(s: String) = String(GlideLetters.keyOf(s))

    /** The 'o' of "for" lands [pitches] of a key toward 'i' (a hairpin turn that stops short), nothing else is off. */
    fun forHabit(keys: GlideKeys, pitches: Float): Map<String, List<FloatArray?>> {
        val dx = (keys.cx['i' - 'a'] - keys.cx['o' - 'a']) * pitches
        return mapOf("for" to listOf(null, floatArrayOf(dx, 0f), null))
    }


    /** What one person does after a glide: keep the first guess when it is right, pick the word they meant from the strip when it is among the three, otherwise type it letter by letter (nothing to learn from the shape then). */
    class Outcome(val first: String, val firstRight: Boolean, val inThree: Boolean)

    /**
     * The same hand glides [word] [n] times in a row (the person writes it again and again), the keyboard learns from what the person does ([patterns] on or off: the correction memory of alpha36 is
     * always on). Returns what came first each time.
     */
    fun repeatWord(lex: BinaryLexicon, keys: GlideKeys, hand: Hand, word: String, n: Int, patterns: Boolean, params: GlideParams = GlideParams()): List<Outcome> {
        val dec = GlideDecoder(keys, if (patterns) params else GlideParams(patGain = 0f))
        val model = PersonalModel()
        dec.personal = model
        val out = ArrayList<Outcome>()
        repeat(n) {
            val path = hand.path(word) ?: return@repeat
            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), null, 3)
            val first = c.firstOrNull()?.text ?: "-"
            val right = first == word
            val three = c.any { it.text == word }
            out.add(Outcome(first, right, three))
            val print = dec.lastPrint
            if (right) { model.add(null, word, 1f); if (print != null) { model.patterns.add(word, print, 1f); model.patterns.contradict(word, print, 0.7f) } }
            else if (three) { model.add(null, word, 3f); if (first != "-") model.addConfusion(first, word); if (print != null) { model.patterns.add(word, print, 3f); model.patterns.contradict(word, print, 2f) } }
            else { model.add(null, word, 1f) } // typed letter by letter
        }
        return out
    }


    class HabitCurve(val blocks: List<Double>, val favouriteBlocks: List<Double>, val shapes: Int, val shapeWords: Int, val flips: Int, val good: Int, val bad: Int, val glides: Int, val early: Double, val late: Double, val lateMissRecall: Int = 0, val lateMissRank: Int = 0, val msShapes: Double = 0.0, val msPlain: Double = 0.0)

    /**
     * A long stretch of use by a person whose hand has a habit for every word it writes ([habitDp] dp of fixed shift per letter, on top of the finger's scatter): 70 % of what they write comes from
     * their own phrases, the rest from everyday words. What the keyboard learns is switched on step by step: [words] the word / pair / correction memory of alpha36-38, [shapes] the new glide
     * shapes. First guess right, in [blocks] equal stretches. With shapes on, every glide is also decoded WITHOUT them, so the lab can count the first guesses the shapes mended and the ones they spoiled.
     */
    fun habitCurve(lex: BinaryLexicon, keys: GlideKeys, noise: SimNoise, glides: Int, blocks: Int, words: Boolean, shapes: Boolean, habitDp: Float, seed: Long, params: GlideParams = GlideParams(), sigma: Float = PatternMemory.SIGMA, mergeD: Float = PatternMemory.MERGE_D, habitPercent: Int = 70, favourites: Int = 150): HabitCurve {
        val rnd = Random(seed * 31 + 7)
        val hand = Hand(keys, noise, seed, habitDp)
        val dec = GlideDecoder(keys, if (shapes) params else GlideParams(patGain = 0f))
        val plain = GlideDecoder(keys, GlideParams(patGain = 0f))
        val model = if (words || shapes) PersonalModel() else null
        model?.patterns?.sigma = sigma; model?.patterns?.mergeD = mergeD
        dec.personal = model; plain.personal = model
        val ranked = (0 until lex.size).filter { distinctLetters(lex.text(it)) >= 3 }.sortedByDescending { lex.logProb(it) }.drop(1500).take(13500)
        val fav = List(favourites) { lex.text(ranked[rnd.nextInt(ranked.size)]) }.distinct()
        val phrases = List(40) { val n = 2 + rnd.nextInt(2); List(n) { fav[rnd.nextInt(fav.size)] } }
        val base = GlideEval.frequencyPick(lex, 4000, Random(seed * 13), 3).map { lex.text(it.id) }
        val lexicons = listOf<GlideLexicon>(lex)
        val hits = IntArray(blocks); val seen = IntArray(blocks); val favHits = IntArray(blocks); val favSeen = IntArray(blocks)
        val times = HashMap<String, Int>(); var eHit = 0; var eSeen = 0; var lHit = 0; var lSeen = 0; var missRecall = 0; var missRank = 0
        var done = 0; var flips = 0; var good = 0; var bad = 0; var nsShapes = 0L; var nsPlain = 0L; var nDec = 0
        while (done < glides) {
            val q: List<String> = if (rnd.nextInt(100) < habitPercent) phrases[rnd.nextInt(phrases.size)] else List(1 + rnd.nextInt(3)) { base[rnd.nextInt(base.size)] }
            var prev: String? = null; var prev2: String? = null
            for (w in q) {
                if (done >= glides) break
                val path = hand.path(w) ?: continue
                val t0 = System.nanoTime()
                val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
                val t1 = System.nanoTime(); nsShapes += t1 - t0; nDec++
                val key = keyOf(w)
                val first = c.isNotEmpty() && keyOf(c[0].text) == key
                if (shapes) {
                    val c0 = plain.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
                    nsPlain += System.nanoTime() - t1
                    val first0 = c0.isNotEmpty() && keyOf(c0[0].text) == key
                    if (dec.lastPatternFlipped) flips++
                    if (first && !first0) good++
                    if (!first && first0) bad++
                }
                val inThree = c.any { keyOf(it.text) == key }
                val b = (done * blocks / glides).coerceAtMost(blocks - 1)
                seen[b]++; if (first) hits[b]++
                if (w in fav) { favSeen[b]++; if (first) favHits[b]++ }
                val k = times.merge(w, 1, Int::plus)!!
                if (k <= 2) { eSeen++; if (first) eHit++ } else if (k >= 6) { lSeen++; if (first) lHit++ else if (w in dec.survivorTexts(lexicons)) missRank++ else missRecall++ }
                val print = dec.lastPrint
                if (words) {
                    model!!.add(prev, w, if (first) 1f else if (inThree) 3f else 1f, prev2)
                    if (!first && inThree && c.isNotEmpty()) model.addConfusion(c[0].text, w)
                }
                if (shapes && model != null && print != null) {
                    if (first) { model.patterns.add(w, print, 1f); model.patterns.contradict(w, print, 0.7f) }
                    else if (inThree) { model.patterns.add(w, print, 3f); model.patterns.contradict(w, print, 2f) }
                    else { // deleted, typed letter by letter: the shape is kept when the word could have been drawn like that
                        val ideal = Fingerprint.ofWord(w, keys)
                        if (ideal != null && Fingerprint.distance(ideal, print) <= GlideTyping.FIT_D) { model.patterns.add(w, print, 3f); model.patterns.contradict(w, print, 2f) }
                    }
                }
                prev2 = prev; prev = w; done++
            }
        }
        return HabitCurve(List(blocks) { 100.0 * hits[it] / seen[it].coerceAtLeast(1) }, List(blocks) { 100.0 * favHits[it] / favSeen[it].coerceAtLeast(1) }, model?.patterns?.patternCount ?: 0, model?.patterns?.wordCount ?: 0, flips, good, bad, glides, 100.0 * eHit / eSeen.coerceAtLeast(1), 100.0 * lHit / lSeen.coerceAtLeast(1), missRecall, missRank, nsShapes / 1e6 / nDec.coerceAtLeast(1), nsPlain / 1e6 / nDec.coerceAtLeast(1))
    }

    @JvmStatic fun main(args: Array<String>) {
        val a = GlideEval.parse(args.filter { it.contains('=') }.toTypedArray())
        val lex = GlideEval.loadLexicon()
        val keys = GlideEval.keysFor()
        val noise = GlideEval.noiseFrom(a + mapOf("noise" to (a["noise"] ?: "normal")))
        val n = a["n"]?.toInt() ?: 200
        if (args.contains("learn")) {
            val pitches = a["shift"]?.toFloat() ?: 1.0f
            val word = a["word"] ?: "for"
            val trials = a["trials"]?.toInt() ?: 12
            println("a hand whose o lands %.1f key toward i for '%s', %s; every row is %d fresh hands (different seeds); first guess right, %% of hands, per glide number".format(pitches, word, noise.name, trials))
            for (on in listOf(false, true)) {
                val right = IntArray(30); val wrongNames = HashMap<String, Int>()
                for (seed in 1..trials) {
                    val hand = Hand(keys, noise, seed.toLong(), 0f, forHabit(keys, pitches))
                    val r = repeatWord(lex, keys, hand, word, 30, on)
                    for ((i, o) in r.withIndex()) { if (o.firstRight) right[i]++ else wrongNames.merge(o.first, 1, Int::plus) }
                }
                println("  shapes %s: ".format(if (on) "ON " else "off") + right.joinToString(" ") { "%3d".format(100 * it / trials) } + "   wrong first guesses: " + wrongNames.entries.sortedByDescending { it.value }.take(5).joinToString { "${it.key} ${it.value}" })
            }
            return
        }
        if (args.contains("curve")) {
            val g = a["glides"]?.toInt() ?: 3000; val nb = a["blocks"]?.toInt() ?: 6
            for (hd in (a["habit"] ?: "0,10,16").split(',').map { it.toFloat() }) {
                println("%s, every word has its own fixed hand habit of %.0f dp per letter; %d glides in %d stretches; first guess right, %% (own favourite words in brackets):".format(noise.name, hd, g, nb))
                for ((label, w, sh) in listOf(Triple("nothing learned          ", false, false), Triple("words/pairs/corrections ", true, false), Triple("+ glide shapes          ", true, true))) {
                    if (a["only"] != null && a["only"] != (if (sh) "shapes" else if (w) "words" else "none")) continue
                    val c = habitCurve(lex, keys, noise, g, nb, w, sh, hd, a["seed"]?.toLong() ?: 3L, GlideEval.paramsFrom(a), a["sigma"]?.toFloat() ?: PatternMemory.SIGMA, a["merge"]?.toFloat() ?: PatternMemory.MERGE_D)
                    println("   $label " + c.blocks.joinToString(" ") { "%.1f".format(it) } + "  (" + c.favouriteBlocks.joinToString(" ") { "%.0f".format(it) } + ")  words seen 1-2 times: %.1f %%, 6+ times: %.1f %%".format(c.early, c.late) + if (sh) "   shapes kept ${c.shapes} for ${c.shapeWords} words, changed the first guess ${c.flips} times (mended ${c.good}, spoiled ${c.bad}); words seen 6+ times that were wrong: not among the ${GlideParams().keep} candidates ${c.lateMissRecall}, ranked below the first ${c.lateMissRank}; decode avg %.1f ms with the memory, %.1f ms without".format(c.msShapes, c.msPlain) else "")
                }
            }
            return
        }
        val forId = lex.idOf("for"); val firId = lex.idOf("fir")
        println("for: ln P = %.2f   fir: ln P = %.2f".format(lex.logProb(forId), lex.logProb(firId)))
        for (pitches in listOf(0.0f, 0.4f, 0.6f, 0.8f, 1.0f, 1.2f)) {
            val hand = Hand(keys, noise, 7L, 0f, forHabit(keys, pitches))
            val dec = GlideDecoder(keys, GlideParams())
            var forFirst = 0; var firFirst = 0; var other = 0; var none = 0; var inThree = 0
            val others = HashMap<String, Int>()
            repeat(n) {
                val p = hand.path("for") ?: return@repeat
                val c = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)
                if (c.isEmpty()) none++ else when (c[0].text) { "for" -> forFirst++; "fir" -> firFirst++; else -> { other++; others.merge(c[0].text, 1, Int::plus) } }
                if (c.any { it.text == "for" }) inThree++
            }
            println("o lands %.1f key toward i (%s): for first %d / fir first %d / other first %d / no word %d  (for among the first 3: %d of %d)  other: %s".format(pitches, noise.name, forFirst, firFirst, other, none, inThree, n, others.entries.sortedByDescending { it.value }.take(4).joinToString { "${it.key} ${it.value}" }))
        }
    }
}
