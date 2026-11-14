package app.kestrel.core.input.glide

import app.kestrel.core.input.MiniKeyboardGeometry
import app.kestrel.core.input.MiniKeyboardLayout
import java.io.File
import java.util.Random

/**
 * The accuracy yardstick for glide typing (AT1d): draws words, lets the simulated finger glide them over the real key layout, decodes, and counts how often the word is first / in the first three.
 * Used by the test (stated thresholds) and, from the command line, to tune the decoder:
 *
 *   java -cp <core main classes>:<core test classes>:<kotlin stdlib> app.kestrel.core.input.glide.GlideEval noise=normal n=3000 pop=freq lmW=0.3
 */
object GlideEval {
    fun findAsset(): File {
        for (p in listOf("geckoapp/src/main/assets/glide/en.klx", "../geckoapp/src/main/assets/glide/en.klx")) { val f = File(p); if (f.exists()) return f }
        throw IllegalStateException("glide word list not found (run from the repository root or core/)")
    }

    fun loadLexicon(): BinaryLexicon = LexiconFile.read(findAsset().readBytes())

    /** The letter page laid out like the keyboard view lays it out: [widthDp] x [heightDp], 3 dp gaps. */
    fun keysFor(widthDp: Float = 292f, heightDp: Float = 161f): GlideKeys {
        val rows = MiniKeyboardLayout.rows(false)
        return GlideKeys.from(rows, MiniKeyboardGeometry.boxes(rows, widthDp, heightDp, 3f))
    }

    class Pick(val id: Int, val prev: Int)

    /** Words as often as people use them (the frequency of the list). */
    fun frequencyPick(lex: BinaryLexicon, n: Int, rnd: Random, minLetters: Int = 2): List<Pick> {
        val ids = (0 until lex.size).filter { distinct(lex, it) >= minLetters }
        val cum = DoubleArray(ids.size); var s = 0.0
        for ((i, id) in ids.withIndex()) { s += Math.exp(lex.logProb(id).toDouble()); cum[i] = s }
        return List(n) { val r = rnd.nextDouble() * s; var lo = 0; var hi = ids.size - 1; while (lo < hi) { val mid = (lo + hi) ushr 1; if (cum[mid] < r) lo = mid + 1 else hi = mid }; Pick(ids[lo], -1) }
    }

    /** Words uniformly out of the [top] most common ones: a harder mix than real use (it has as many rare words as common ones). */
    fun rankPick(lex: BinaryLexicon, n: Int, rnd: Random, top: Int = 20000, minLetters: Int = 3): List<Pick> {
        val ids = (0 until lex.size).filter { distinct(lex, it) >= minLetters }.sortedByDescending { lex.logProb(it) }.take(top)
        return List(n) { Pick(ids[rnd.nextInt(ids.size)], -1) }
    }

    /** Pairs of words as they follow each other in the pair list: the word before is known to the decoder. */
    fun contextPick(lex: BinaryLexicon, n: Int, rnd: Random, minLetters: Int = 2): List<Pick> {
        val firsts = (0 until lex.size).filter { lex.successors(it).isNotEmpty() }
        val cum = DoubleArray(firsts.size); var s = 0.0
        for ((i, id) in firsts.withIndex()) { s += Math.exp(lex.logProb(id).toDouble()); cum[i] = s }
        val out = ArrayList<Pick>()
        while (out.size < n) {
            val r = rnd.nextDouble() * s; var lo = 0; var hi = firsts.size - 1
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (cum[mid] < r) lo = mid + 1 else hi = mid }
            val prev = firsts[lo]
            val succ = lex.successors(prev)
            val w = DoubleArray(succ.size) { Math.exp(lex.bigramLogProb(prev, succ[it]).toDouble()) }
            val tot = w.sum(); var x = rnd.nextDouble() * tot; var pick = succ.last()
            for (i in succ.indices) { x -= w[i]; if (x <= 0) { pick = succ[i]; break } }
            if (distinct(lex, pick) >= minLetters) out.add(Pick(pick, prev))
        }
        return out
    }

    private fun distinct(lex: GlideLexicon, id: Int): Int { var m = 0; var p = -1; for (i in 0 until lex.length(id)) { val l = lex.letter(id, i); if (l != p) { m++; p = l } }; return m }

    class Result(val n: Int, val top1: Int, val top3: Int, val recall: Int, val millis: Double, val worstMillis: Double, val byLen: Map<Int, IntArray>) {
        fun pct(x: Int) = 100.0 * x / n
        override fun toString() = "n=$n top1=%.1f%% top3=%.1f%% recall=%.1f%% decode avg %.2f ms worst %.1f ms".format(pct(top1), pct(top3), pct(recall), millis, worstMillis)
        fun lengths(): String = byLen.toSortedMap().entries.joinToString("  ") { (l, a) -> "len$l:%.0f/%.0f%%(n=%d)".format(100.0 * a[1] / a[0], 100.0 * a[2] / a[0], a[0]) }
    }

    fun run(lex: BinaryLexicon, keys: GlideKeys, params: GlideParams, noise: SimNoise, picks: List<Pick>, seed: Long, extra: List<GlideLexicon> = emptyList(), debug: Int = 0, trace: String? = null, learn: Boolean = false): Result {
        var shown = 0
        val sim = GestureSim(keys, Random(seed))
        val dec = GlideDecoder(keys, params)
        val lexicons = listOf<GlideLexicon>(lex) + extra
        var t1 = 0; var t3 = 0; var rec = 0; var n = 0; var nanos = 0L; var worst = 0L
        val byLen = HashMap<Int, IntArray>()
        for (pk in picks) {
            val word = lex.text(pk.id)
            val path = sim.path(word, noise) ?: continue
            val t0 = System.nanoTime()
            val prevText = if (pk.prev >= 0) lex.text(pk.prev) else null
            val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, prevText, 3)
            val dt = System.nanoTime() - t0
            nanos += dt; if (dt > worst) worst = dt
            n++
            val surv = dec.survivorTexts(lexicons)
            if (word in surv) rec++
            val l = distinct(lex, pk.id).coerceAtMost(8)
            val a = byLen.getOrPut(l) { IntArray(3) }
            a[0]++
            if (debug > 0 && !(c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == String(GlideLetters.keyOf(word))) && shown < debug) {
                shown++
                val tc = if (word in surv) dec.debugCost(lex, pk.id) else Float.NaN
                println("    MISS '%s' (prior %.1f, cost %.3f, survived=%b) -> %s".format(word, lex.logProb(pk.id), tc, word in surv, c.joinToString(" | ") { "%s cost %.3f prior %.1f".format(it.text, it.cost, it.prior) }))
            }
            val key = String(GlideLetters.keyOf(word))
            if (learn && c.any { String(GlideLetters.keyOf(it.text)) == key }) dec.learnFrom(lex, pk.id) // the person accepted this word (top choice or picked from the strip)
            if (trace != null && word == trace) { for (cand in listOf(word) + c.map { it.text }) { val ci = lex.idOf(cand); if (ci >= 0) println("    TRACE $cand: " + dec.debugTrace(lex, ci)) } }
            if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == key) { t1++; a[1]++ }
            if (c.any { String(GlideLetters.keyOf(it.text)) == key }) { t3++; a[2]++ }
        }
        return Result(n, t1, t3, rec, nanos / 1e6 / n, worst / 1e6, byLen)
    }

    fun parse(args: Array<String>): Map<String, String> = args.mapNotNull { a -> a.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

    fun paramsFrom(a: Map<String, String>): GlideParams {
        val d = GlideParams()
        return GlideParams(
            samples = a["samples"]?.toInt() ?: d.samples, endReach2 = a["endReach2"]?.toFloat() ?: d.endReach2, visitCap = a["visitCap"]?.toFloat() ?: d.visitCap,
            missLimit2 = a["missLimit2"]?.toFloat() ?: d.missLimit2, keep = a["keep"]?.toInt() ?: d.keep, quickEnds = a["quickEnds"]?.toFloat() ?: d.quickEnds,
            quickPrior = a["quickPrior"]?.toFloat() ?: d.quickPrior, shapeW = a["shapeW"]?.toFloat() ?: d.shapeW, endW = a["endW"]?.toFloat() ?: d.endW,
            lmW = a["lmW"]?.toFloat() ?: d.lmW, lmA = a["lmA"]?.toFloat() ?: d.lmA, lmMin = a["lmMin"]?.toFloat() ?: d.lmMin, lmMax = a["lmMax"]?.toFloat() ?: d.lmMax, bigramMix = a["bigramMix"]?.toFloat() ?: d.bigramMix, minLength = a["minLength"]?.toFloat() ?: d.minLength, slack = a["slack"]?.toInt() ?: d.slack, biasKeep = a["biasKeep"]?.toInt() ?: d.biasKeep, biasGain = a["biasGain"]?.toFloat() ?: d.biasGain, biasW = a["biasW"]?.toFloat() ?: d.biasW,
        )
    }

    fun noiseFrom(a: Map<String, String>): SimNoise = (a["ubias"]?.toFloat()?.let { u -> noiseFrom0(a["noise"] ?: "normal").withUserBias(u) }) ?: noiseFrom0(a["noise"] ?: "normal")

    private fun noiseFrom0(name: String): SimNoise = when (val n = name) {
        "calm" -> SimNoise.CALM; "normal" -> SimNoise.NORMAL; "sloppy" -> SimNoise.SLOPPY; "bad" -> SimNoise.BAD
        else -> if (n.startsWith("c:")) { val v = n.removePrefix("c:").split(':').map { it.toFloat() }; SimNoise(v[0], v[1], v[2], v[3], v[4], "custom aim ${v[0]} cut ${v[1]} bias ${v[2]} jitter ${v[3]} wobble ${v[4]}") } else SimNoise.aim(n.removePrefix("aim").toFloat())
    }

    @JvmStatic fun main(args: Array<String>) {
        val a = parse(args)
        val lex = loadLexicon()
        val width = a["width"]?.toFloat() ?: 292f
        val keys = keysFor(width, a["height"]?.toFloat() ?: 161f)
        val n = a["n"]?.toInt() ?: 2000
        val seed = a["seed"]?.toLong() ?: 1L
        val rnd = Random(seed * 7919)
        val picks = when (a["pop"] ?: "freq") { "rank" -> rankPick(lex, n, rnd); "ctx" -> contextPick(lex, n, rnd); else -> frequencyPick(lex, n, rnd) }
        for (nm in (a["noise"] ?: "calm,normal,sloppy,bad").split(',')) {
            val noise = noiseFrom(a + mapOf("noise" to nm))
            val r = run(lex, keys, paramsFrom(a), noise, picks, seed, debug = a["dbg"]?.toInt() ?: 0, trace = a["trace"], learn = a["learn"] == "1")
            println("${noise.name}: $r")
            if (a["lens"] != null) println("    ${r.lengths()}")
        }
    }
}
