package app.kestrel.core.input.glide

import java.util.Random

/**
 * R27b: builds a [ClassModel] from the shipped word list's own pair data (test side: the app only READS the finished asset). Words are described by how likely each of the [contexts] commonest words
 * is to come right before them; k-means groups the [vocab] commonest words into [k] classes; transitions and class masses come from the whole pair list weighted by the word before's share.
 */
object ClassModelBuilder {
    fun build(lex: BinaryLexicon, vocab: Int = 8000, k: Int = 48, contexts: Int = 250, seed: Long = 1L, iterations: Int = 12): ClassModel {
        val n = lex.size
        val order = (0 until n).sortedByDescending { lex.logProb(it) }
        val words = order.take(vocab); val ctx = order.take(contexts)
        // description: sqrt of P(word | context c), L2-normalised (so frequent and rare words of one role look alike)
        val x = Array(words.size) { FloatArray(contexts) }
        for ((wi, w) in words.withIndex()) {
            var s = 0.0
            for ((ci, c) in ctx.withIndex()) { val b = lex.bigramLogProb(c, w); if (!b.isNaN()) { val v = Math.sqrt(Math.exp(b.toDouble())); x[wi][ci] = v.toFloat(); s += v * v } }
            if (s > 0) { val inv = (1.0 / Math.sqrt(s)).toFloat(); for (ci in 0 until contexts) x[wi][ci] *= inv }
        }
        val rnd = Random(seed)
        val cent = Array(k) { x[rnd.nextInt(x.size)].copyOf() }
        val assign = IntArray(x.size)
        for (it in 0 until iterations) {
            for (wi in x.indices) { var best = 0; var bd = Float.MAX_VALUE; for (c in 0 until k) { var d = 0f; val cc = cent[c]; val xv = x[wi]; for (j in 0 until contexts) { val t = xv[j] - cc[j]; d += t * t }; if (d < bd) { bd = d; best = c } }; assign[wi] = best }
            val sum = Array(k) { FloatArray(contexts) }; val cnt = IntArray(k)
            for (wi in x.indices) { cnt[assign[wi]]++; for (j in 0 until contexts) sum[assign[wi]][j] += x[wi][j] }
            for (c in 0 until k) if (cnt[c] > 0) for (j in 0 until contexts) cent[c][j] = sum[c][j] / cnt[c] else cent[c] = x[rnd.nextInt(x.size)].copyOf()
        }
        val classOf = ByteArray(n) { 255.toByte() }
        for ((wi, w) in words.withIndex()) classOf[w] = assign[wi].toByte()
        // transitions: sum over every known pair (w1, w2) of P(w1) * P(w2 | w1), by the classes of the two words; masses: the share of each class
        val trans = DoubleArray(k * k); val mass = DoubleArray(k); val rowSum = DoubleArray(k)
        for (w in words) { val c = classOf[w].toInt() and 0xFF; mass[c] += Math.exp(lex.logProb(w).toDouble()) }
        for (w1 in words) {
            val c1 = classOf[w1].toInt() and 0xFF; val p1 = Math.exp(lex.logProb(w1).toDouble())
            for (w2 in lex.successors(w1)) { val c2 = classOf[w2]; if ((c2.toInt() and 0xFF) >= k) continue; val b = lex.bigramLogProb(w1, w2); if (b.isNaN()) continue; trans[c1 * k + (c2.toInt() and 0xFF)] += p1 * Math.exp(b.toDouble()) }
        }
        for (c in 0 until k) { var s = 0.0; for (d in 0 until k) s += trans[c * k + d] + 1e-9; rowSum[c] = s }
        val tf = FloatArray(k * k) { ((trans[it] + 1e-9) / rowSum[it / k]).toFloat() }
        return ClassModel(classOf, k, tf, FloatArray(k) { mass[it].toFloat() })
    }

    /** The commonest words of each class, for a look at what the classes are. */
    fun describe(lex: BinaryLexicon, m: ClassModel, perClass: Int = 8): String {
        val byClass = HashMap<Int, ArrayList<Int>>()
        for (id in 0 until lex.size) { val c = m.classOfId(id); if (c >= 0) byClass.getOrPut(c) { ArrayList() }.add(id) }
        return (0 until m.k).joinToString("\n") { c -> "  class %2d (%5d words): ".format(c, byClass[c]?.size ?: 0) + (byClass[c]?.sortedByDescending { lex.logProb(it) }?.take(perClass)?.joinToString(" ") { lex.text(it) } ?: "") }
    }
}
