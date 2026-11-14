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
