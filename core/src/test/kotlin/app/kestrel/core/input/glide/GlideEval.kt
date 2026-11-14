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

    /** Paths that are not a word at all: a random wiggle over the keyboard. */
    fun garbage(keys: GlideKeys, rnd: Random): GestureSim.Path {
        val m = 3 + rnd.nextInt(6)
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        var px = keys.cx[rnd.nextInt(26)]; var py = keys.cy[rnd.nextInt(26)]
        for (j in 0 until m) {
            val tx = keys.cx[rnd.nextInt(26)]; val ty = keys.cy[rnd.nextInt(26)]
            for (s in 0..12) { xs.add(px + (tx - px) * s / 12f + rnd.nextGaussian().toFloat() * 2f); ys.add(py + (ty - py) * s / 12f + rnd.nextGaussian().toFloat() * 2f) }
            px = tx; py = ty
        }
        return GestureSim.Path(xs.toFloatArray(), ys.toFloatArray())
    }

    class Result(val n: Int, val top1: Int, val top3: Int, val recall: Int, val millis: Double, val worstMillis: Double, val byLen: Map<Int, IntArray>) {
        /** Cost per letter of the right answer when it was first (sorted), for choosing the point where a glide counts as "no word". */
        var hitCosts: List<Float> = emptyList()
        /** When the right word was second or third: how much worse its path fit than the first guess's (cost per letter, pitch squared), sorted. Where a correction habit may lift a word (GlideParams.confMaxGap). */
        var gaps: List<Float> = emptyList()
        /** How many words the careful pass looked at, per glide (sorted): the width of the candidate list. */
        var widths: List<Int> = emptyList()
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
        val hitCosts = ArrayList<Float>(); val gaps = ArrayList<Float>(); val widths = ArrayList<Int>()
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
            widths.add(surv.size)
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
            if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == key) { t1++; a[1]++; hitCosts.add(c[0].perLetter) }
            if (c.any { String(GlideLetters.keyOf(it.text)) == key }) { t3++; a[2]++ }
            if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) != key) { val j = c.indexOfFirst { String(GlideLetters.keyOf(it.text)) == key }; if (j > 0) gaps.add(c[j].perLetter - c[0].perLetter) }
        }
        return Result(n, t1, t3, rec, nanos / 1e6 / n, worst / 1e6, byLen).also { it.hitCosts = hitCosts.sorted(); it.gaps = gaps.sorted(); it.widths = widths.sorted() }
    }

    /**
     * A person with habits (AU2d): favourite words that are not among the commonest, and phrases they write again and again. They glide [glides] words; the first guess is judged in [blocks] equal
     * stretches of use. With [learn] the keyboard's personal model hears what the person did: a word that was first is kept (1), one they had to pick from the three guesses counts more (3), one
     * that was not there at all they type letter by letter (1).
     */
    class Curve(val blocks: List<Double>, val favouriteBlocks: List<Double>, val known: Int, val pairs: Int)

    fun learningCurve(lex: BinaryLexicon, keys: GlideKeys, params: GlideParams, noise: SimNoise, glides: Int, blocks: Int, learn: Boolean, seed: Long, habitPercent: Int = 70, favourites: Int = 150, corrections: Boolean = true, burst: Int = 0, twoWords: Boolean = true, recency: Boolean = true): Curve {
        val rnd = Random(seed * 31 + 7)
        val sim = GestureSim(keys, Random(seed))
        val dec = GlideDecoder(keys, params)
        val model = if (learn) PersonalModel(recency = recency) else null
        dec.personal = model
        // the person: 150 favourite words from ranks 1500..15000, phrases of two or three of them
        val ranked = (0 until lex.size).filter { distinct(lex, it) >= 3 }.sortedByDescending { lex.logProb(it) }.drop(1500).take(13500)
        val fav = List(favourites) { lex.text(ranked[rnd.nextInt(ranked.size)]) }.distinct()
        val phrases = List(40) { val n = 2 + rnd.nextInt(2); List(n) { fav[rnd.nextInt(fav.size)] } }
        val base = frequencyPick(lex, 4000, Random(seed * 13), 3).map { lex.text(it.id) }
        val lexicons = listOf<GlideLexicon>(lex)
        val hits = IntArray(blocks); val seen = IntArray(blocks); val favHits = IntArray(blocks); val favSeen = IntArray(blocks)
        // topics (burst > 0): the person works on one subject for [burst] glides, and then another: the words of that subject come back again and again for a while
        val topics = List(10) { List(14) { (fav + base.take(300))[rnd.nextInt(fav.size + 300)] } }
        var topic = 0
        var done = 0
        while (done < glides) {
            if (burst > 0 && done % burst == 0) topic = rnd.nextInt(topics.size)
            val q: List<String> = if (burst > 0 && rnd.nextInt(100) < 55) List(2 + rnd.nextInt(3)) { topics[topic][rnd.nextInt(topics[topic].size)] }
                else if (rnd.nextInt(100) < habitPercent) phrases[rnd.nextInt(phrases.size)] else List(1 + rnd.nextInt(3)) { base[rnd.nextInt(base.size)] }
            var prev: String? = null; var prev2: String? = null
            for (w in q) {
                if (done >= glides) break
                val path = sim.path(w, noise) ?: continue
                val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, if (twoWords) prev2 else null)
                val b = (done * blocks / glides).coerceAtMost(blocks - 1)
                val key = String(GlideLetters.keyOf(w))
                val first = c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == key
                val inThree = c.any { String(GlideLetters.keyOf(it.text)) == key }
                seen[b]++; if (first) hits[b]++
                if (w in fav) { favSeen[b]++; if (first) favHits[b]++ }
                // what the person did, and so what the keyboard learned
                model?.add(prev, w, if (first) 1f else if (inThree) 3f else 1f, if (twoWords) prev2 else null)
                if (model != null && corrections && !first && inThree && c.isNotEmpty()) model.addConfusion(c[0].text, w) // the person picked the word they meant over the keyboard's first guess
                prev2 = prev; prev = w; done++
            }
        }
        return Curve(List(blocks) { 100.0 * hits[it] / seen[it].coerceAtLeast(1) }, List(blocks) { 100.0 * favHits[it] / favSeen[it].coerceAtLeast(1) }, model?.wordCount ?: 0, model?.pairCount ?: 0)
    }

    fun parse(args: Array<String>): Map<String, String> = args.mapNotNull { a -> a.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

    fun paramsFrom(a: Map<String, String>): GlideParams {
        val d = GlideParams()
        return GlideParams(
            samples = a["samples"]?.toInt() ?: d.samples, endReach2 = a["endReach2"]?.toFloat() ?: d.endReach2, visitCap = a["visitCap"]?.toFloat() ?: d.visitCap,
            missLimit2 = a["missLimit2"]?.toFloat() ?: d.missLimit2, keep = a["keep"]?.toInt() ?: d.keep, quickEnds = a["quickEnds"]?.toFloat() ?: d.quickEnds,
            quickPrior = a["quickPrior"]?.toFloat() ?: d.quickPrior, shapeW = a["shapeW"]?.toFloat() ?: d.shapeW, endW = a["endW"]?.toFloat() ?: d.endW,
            lmW = a["lmW"]?.toFloat() ?: d.lmW, confGain = a["confGain"]?.toFloat() ?: d.confGain, confCap = a["confCap"]?.toFloat() ?: d.confCap, confMaxGap = a["confMaxGap"]?.toFloat() ?: d.confMaxGap, lmA = a["lmA"]?.toFloat() ?: d.lmA, lmMin = a["lmMin"]?.toFloat() ?: d.lmMin, lmMax = a["lmMax"]?.toFloat() ?: d.lmMax, bigramMix = a["bigramMix"]?.toFloat() ?: d.bigramMix, minLength = a["minLength"]?.toFloat() ?: d.minLength, slack = a["slack"]?.toInt() ?: d.slack, biasKeep = a["biasKeep"]?.toInt() ?: d.biasKeep, biasGain = a["biasGain"]?.toFloat() ?: d.biasGain, biasW = a["biasW"]?.toFloat() ?: d.biasW, maxPerLetter = a["maxPerLetter"]?.toFloat() ?: d.maxPerLetter,
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
        if (a["curve"] != null) {
            val g = a["glides"]?.toInt() ?: 1800; val nb = a["blocks"]?.toInt() ?: 6
            for (nm in (a["noise"] ?: "normal,sloppy").split(',')) {
                val noise = noiseFrom(a + mapOf("noise" to nm))
                val hp = a["habit"]?.toInt() ?: 70; val fv = a["favs"]?.toInt() ?: 150
                val conf = (a["confs"] ?: "1") == "1"; val burst = a["burst"]?.toInt() ?: 0; val two = (a["two"] ?: "1") == "1"; val rec = (a["rec"] ?: "1") == "1"
                val off = learningCurve(lex, keys, paramsFrom(a), noise, g, nb, false, seed, hp, fv, conf, burst, two, rec)
                val on = learningCurve(lex, keys, paramsFrom(a), noise, g, nb, true, seed, hp, fv, conf, burst, two, rec)
                println("${noise.name}, a person who writes $hp percent of what they type from their own phrases, $g glides in $nb stretches (first guess right, %):")
                println("   without learning: " + off.blocks.joinToString(" ") { "%.1f".format(it) } + "   own favourites: " + off.favouriteBlocks.joinToString(" ") { "%.1f".format(it) })
                println("   with learning:    " + on.blocks.joinToString(" ") { "%.1f".format(it) } + "   own favourites: " + on.favouriteBlocks.joinToString(" ") { "%.1f".format(it) } + "   (knows ${on.known} words, ${on.pairs} pairs)")
            }
            return
        }
        if (a["garbage"] != null) {
            val dec = GlideDecoder(keys, paramsFrom(a)); val rnd2 = Random(5)
            val costs = ArrayList<Float>(); var none = 0; val total = 800
            repeat(total) { val g = garbage(keys, rnd2); val c = dec.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 1); if (c.isEmpty()) none++ else costs.add(c[0].perLetter) }
            costs.sort()
            println("garbage paths: %d of %d got no word at all; for the rest the best word's cost per letter: min %.2f p10 %.2f median %.2f".format(none, total, costs.firstOrNull() ?: 0f, if (costs.isEmpty()) 0f else costs[costs.size / 10], if (costs.isEmpty()) 0f else costs[costs.size / 2]))
        }
        for (nm in (a["noise"] ?: "calm,normal,sloppy,bad").split(',')) {
            val noise = noiseFrom(a + mapOf("noise" to nm))
            val r = run(lex, keys, paramsFrom(a), noise, picks, seed, debug = a["dbg"]?.toInt() ?: 0, trace = a["trace"], learn = a["learn"] == "1")
            println("${noise.name}: $r")
            if (a["lens"] != null) println("    ${r.lengths()}")
            if (a["gaps"] != null) { val g = r.gaps; val w = r.widths
                println("    right word second/third: %d times; its extra cost per letter vs the first guess: p10 %.2f median %.2f p90 %.2f max %.2f; careful-pass width (words per glide): min %d median %d max %d".format(g.size, if (g.isEmpty()) 0f else g[g.size / 10], if (g.isEmpty()) 0f else g[g.size / 2], if (g.isEmpty()) 0f else g[g.size * 9 / 10], g.lastOrNull() ?: 0f, w.firstOrNull() ?: 0, if (w.isEmpty()) 0 else w[w.size / 2], w.lastOrNull() ?: 0)) }
            if (a["costs"] != null) { val h = r.hitCosts; if (h.isNotEmpty()) println("    cost per letter of the right word: median %.2f  p90 %.2f  p99 %.2f  max %.2f".format(h[h.size / 2], h[h.size * 9 / 10], h[h.size * 99 / 100], h.last())) }
        }
    }
}
