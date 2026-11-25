package app.kestrel.core.input.glide

import java.io.File
import java.util.Random

/**
 * The yardstick for "the keyboard must not type rare words" (owner: it kept offering "tui"). The same simulated glides of words people really type are decoded and judged two ways: how often
 * the right word is first / in the first three, and how often a RARE word (by the shipped list's own probabilities, see [Rarity]) came first although the person meant a common one (a "steal").
 *
 *   java -cp <core main classes>:<core test classes>:<kotlin stdlib> app.kestrel.core.input.glide.RareWordEval new=<en.klx> [old=<en.klx>] n=3000 noise=normal,sloppy rareW=0.45
 *
 * Words are drawn the way people use them: by the frequency of the OLD list (the previous word list is a fixed yardstick that does not change when the new one does), and only those both lists know.
 */
object RareWordEval {
    class Stats(val n: Int, val top1: Int, val top3: Int, val steals: Int, val rareFirst: Int, val millis: Double, val deepSteals: Int = 0) {
        fun pct(x: Int) = 100.0 * x / n.coerceAtLeast(1)
        override fun toString() = "n=$n top1=%.1f%% top3=%.1f%% rareSteals=%d (%.2f%%) deepSteals=%d rareFirstAny=%d decode %.2f ms".format(pct(top1), pct(top3), steals, pct(steals), deepSteals, rareFirst, millis)
    }

    /** Very rare: below this the word is in the long tail (rank beyond about 30 000 of 140 000). */
    const val DEEP = -14f

    fun isRare(lex: BinaryLexicon, word: String, floor: Float = Rarity.FLOOR): Boolean { val id = lex.idOf(word); return id < 0 || lex.logProb(id) < floor }

    /** Words by the frequency of [yardstick], that [lex] also knows; with the word before them for the context runs. */
    fun commonWords(yardstick: BinaryLexicon, lex: BinaryLexicon, n: Int, seed: Long, minLetters: Int = 3): List<String> {
        val ids = (0 until yardstick.size).filter { id -> distinct(yardstick, id) >= minLetters && lex.idOf(yardstick.text(id)).let { it >= 0 && lex.text(it) == yardstick.text(id) } }
        val cum = DoubleArray(ids.size); var s = 0.0
        for ((i, id) in ids.withIndex()) { s += Math.exp(yardstick.logProb(id).toDouble()); cum[i] = s }
        val rnd = Random(seed)
        return List(n) { val r = rnd.nextDouble() * s; var lo = 0; var hi = ids.size - 1; while (lo < hi) { val mid = (lo + hi) ushr 1; if (cum[mid] < r) lo = mid + 1 else hi = mid }; yardstick.text(ids[lo]) }
    }

    /** Words uniformly out of the [top] most common of [yardstick] that [lex] also knows: a harder mix than real use (as many middling words as very common ones). */
    fun midWords(yardstick: BinaryLexicon, lex: BinaryLexicon, n: Int, seed: Long, top: Int = 15000, minLetters: Int = 3): List<String> {
        val ids = (0 until yardstick.size).filter { id -> distinct(yardstick, id) >= minLetters && lex.idOf(yardstick.text(id)).let { it >= 0 && lex.text(it) == yardstick.text(id) } }
            .sortedByDescending { yardstick.logProb(it) }.take(top)
        val rnd = Random(seed)
        return List(n) { yardstick.text(ids[rnd.nextInt(ids.size)]) }
    }

    /** Valid but uncommon words of [lex] (probability between [lo] and [hi] nats), uniformly. */
    fun rareWords(lex: BinaryLexicon, n: Int, seed: Long, lo: Float = -16f, hi: Float = Rarity.FLOOR, minLetters: Int = 4): List<String> {
        val ids = (0 until lex.size).filter { lex.logProb(it) in lo..hi && distinct(lex, it) >= minLetters && lex.text(it).all { c -> c in 'a'..'z' } }
        val rnd = Random(seed)
        return List(n) { lex.text(ids[rnd.nextInt(ids.size)]) }
    }

    private fun distinct(lex: GlideLexicon, id: Int): Int { var m = 0; var p = -1; for (i in 0 until lex.length(id)) { val l = lex.letter(id, i); if (l != p) { m++; p = l } }; return m }

    /** Decode a simulated glide of every word with [lex] and [params]; [judge] is the list whose probabilities decide what is rare. */
    fun run(lex: BinaryLexicon, judge: BinaryLexicon, keys: GlideKeys, params: GlideParams, noise: SimNoise, words: List<String>, seed: Long, prevs: List<String?>? = null, debug: Int = 0): Stats {
        var shown = 0
        val sim = GestureSim(keys, Random(seed))
        val dec = GlideDecoder(keys, params)
        var n = 0; var t1 = 0; var t3 = 0; var steals = 0; var deep = 0; var rareFirst = 0; var nanos = 0L
        for ((i, w) in words.withIndex()) {
            val path = sim.path(w, noise) ?: continue
            val t0 = System.nanoTime()
            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), prevs?.get(i), 3)
            nanos += System.nanoTime() - t0
            n++
            val key = String(GlideLetters.keyOf(w))
            val first = c.firstOrNull()
            val hit1 = first != null && String(GlideLetters.keyOf(first.text)) == key
            if (hit1) t1++
            if (c.any { String(GlideLetters.keyOf(it.text)) == key }) t3++
            if (first != null && !hit1 && isRare(judge, first.text)) {
                rareFirst++
                if (!isRare(judge, w)) { steals++; if (isRare(judge, first.text, DEEP)) deep++; if (shown++ < debug) println("      STEAL meant '%s' (lp %.1f) got %s lm %.2f".format(w, judge.logProb(judge.idOf(w)), c.joinToString(" | ") { "%s cost %.2f prior %.1f score %.2f".format(it.text, it.cost, it.prior, it.score) }, dec.lastLm)) }
            }
        }
        return Stats(n, t1, t3, steals, rareFirst, nanos / 1e6 / n.coerceAtLeast(1), deep)
    }

    fun load(path: String): BinaryLexicon = LexiconFile.read(File(path).readBytes())

    @JvmStatic fun main(args: Array<String>) {
        val a = GlideEval.parse(args)
        val new = load(a["new"] ?: GlideEval.findAsset().path)
        val old = load(a["old"] ?: a["new"] ?: GlideEval.findAsset().path)
        val keys = GlideEval.keysFor()
        val n = a["n"]?.toInt() ?: 3000
        val seed = a["seed"]?.toLong() ?: 1L
        // who says which words people type: the old list by default, or any other list (truth=<en.klx>, e.g. one made from subtitle counts only)
        val truth = a["truth"]?.let { load(it) } ?: old
        val common = (if (a["pop"] == "mid") midWords(truth, new, n * 2, seed * 7919) else commonWords(truth, new, n * 2, seed * 7919)).filter { old.idOf(it) >= 0 }.take(n)
        val rare = rareWords(new, a["nr"]?.toInt() ?: 1500, seed * 31)
        val base = GlideEval.paramsFrom(a)
        val d = GlideParams()
        val params = GlideParams(
            samples = base.samples, endReach2 = base.endReach2, visitCap = base.visitCap, missLimit2 = base.missLimit2, keep = a["keep"]?.toInt() ?: d.keep, quickEnds = base.quickEnds, quickPrior = a["quickPrior"]?.toFloat() ?: d.quickPrior,
            shapeW = base.shapeW, endW = base.endW, lmW = base.lmW, lmA = base.lmA, lmMin = base.lmMin, lmMax = base.lmMax, bigramMix = base.bigramMix, minLength = base.minLength, slack = base.slack,
            biasKeep = base.biasKeep, biasGain = base.biasGain, biasW = base.biasW, maxPerLetter = base.maxPerLetter, confGain = base.confGain, confCap = base.confCap, confMaxGap = base.confMaxGap,
            patGain = base.patGain, patCap = base.patCap, patMaxGap = base.patMaxGap,
            rareFloor = a["rareFloor"]?.toFloat() ?: d.rareFloor, rareW = a["rareW"]?.toFloat() ?: d.rareW, rareCap = a["rareCap"]?.toFloat() ?: d.rareCap, rareMaxPerLetter = a["rareMaxPerLetter"]?.toFloat() ?: d.rareMaxPerLetter, rareFitSpan = a["rareFitSpan"]?.toFloat() ?: d.rareFitSpan,
        )
        for (nm in (a["noise"] ?: "calm,normal,sloppy,bad").split(',')) {
            val noise = GlideEval.noiseFrom(a + mapOf("noise" to nm))
            println("${noise.name}")
            if (a["old"] != null) println("   OLD list+params: " + run(old, new, keys, GlideParams(rareW = 0f, keep = 200, quickPrior = 0.12f), noise, common, seed))
            println("   common words : " + run(new, new, keys, params, noise, common, seed, debug = a["dbg"]?.toInt() ?: 0))
            if (a["rare"] != "0") println("   rare words   : " + run(new, new, keys, params, noise, rare, seed + 1))
        }
    }
}
