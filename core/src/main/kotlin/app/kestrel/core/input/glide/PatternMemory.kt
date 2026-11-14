package app.kestrel.core.input.glide

/**
 * The SHAPE of a glide, small enough to keep and compare (AX1, owner message 38: "learn ... the words I use most depending on the pattern drawn even if it is sloppy ... I always change fir to for but it
 * still glide types fir").
 *
 * A glide is cut into [K] points spaced evenly along the path, and each point is written in KEY units, not pixels: u = how many keys right of the centre of Q, v = how many rows below it. So the same
 * hand gives the same numbers whatever size the keyboard was dragged to, and a file of them can be read by another keyboard with the same letter layout (docs/KEYBOARD-EXPORT.md).
 */
object Fingerprint {
    const val K = 16
    /** A path shorter than this many keys is a tap, not a glide. */
    const val MIN_LENGTH = 1.2f

    /** The glide of the first [count] touch points ([xs], [ys] in view pixels) as 2 x [K] numbers (u0, v0, u1, v1 ...), or null for a tap or a keyboard whose geometry is unusable. */
    fun of(xs: FloatArray, ys: FloatArray, count: Int, keys: GlideKeys): FloatArray? {
        if (count < 2 || count > xs.size || count > ys.size) return null
        val ox = keys.cx['q' - 'a']; val oy = keys.cy['q' - 'a']
        val rowStep = (keys.cy['z' - 'a'] - keys.cy['q' - 'a']) / 2f
        if (!(keys.pitch > 1e-3f) || !(rowStep > 1e-3f)) return null
        val iu = 1f / keys.pitch; val iv = 1f / rowStep
        val px = FloatArray(count); val py = FloatArray(count)
        var m = 0
        for (i in 0 until count) {
            val u = (xs[i] - ox) * iu; val v = (ys[i] - oy) * iv
            if (u.isNaN() || v.isNaN()) return null
            if (m == 0 || sq(u - px[m - 1], v - py[m - 1]) > 1e-6f) { px[m] = u; py[m] = v; m++ }
        }
        if (m < 2) return null
        var len = 0f
        for (i in 1 until m) len += dist(px[i] - px[i - 1], py[i] - py[i - 1])
        if (len < MIN_LENGTH) return null
        val out = FloatArray(2 * K)
        var seg = 1; var segStart = 0f
        var segLen = dist(px[1] - px[0], py[1] - py[0])
        for (k in 0 until K) {
            val target = len * k / (K - 1)
            while (seg < m - 1 && target > segStart + segLen) {
                segStart += segLen; seg++
                segLen = dist(px[seg] - px[seg - 1], py[seg] - py[seg - 1])
            }
            val t = if (segLen > 0f) ((target - segStart) / segLen).coerceIn(0f, 1f) else 0f
            out[2 * k] = px[seg - 1] + (px[seg] - px[seg - 1]) * t
            out[2 * k + 1] = py[seg - 1] + (py[seg] - py[seg - 1]) * t
        }
        return out
    }

    /** The shape of [word] drawn exactly: straight lines between the centres of its letters (null when it has fewer than two different letters in a row or something that is not a letter). */
    fun ofWord(word: String, keys: GlideKeys): FloatArray? {
        val k = GlideLetters.keyOf(word)
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        for (b in k) { val i = b.toInt(); if (xs.isEmpty() || xs.last() != keys.cx[i] || ys.last() != keys.cy[i]) { xs.add(keys.cx[i]); ys.add(keys.cy[i]) } }
        if (xs.size < 2) return null
        return of(xs.toFloatArray(), ys.toFloatArray(), xs.size, keys)
    }

    /** How far apart two shapes are: the root of the mean squared distance of their points, in key units. */
    fun distance(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in 0 until 2 * K step 2) s += sq(a[i] - b[i], a[i + 1] - b[i + 1])
        return Math.sqrt((s / K).toDouble()).toFloat()
    }

    private fun sq(dx: Float, dy: Float) = dx * dx + dy * dy
    private fun dist(dx: Float, dy: Float) = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
}

/**
 * What this person's glides LOOK like, word by word: for every word a few shapes (clusters of near-identical glides) with how often each was confirmed. The decoder asks "how much does this glide
 * look like the shapes this person drew for [word]?" and lifts the word by it, so a pattern the person always corrects is learned after one correction and a word they keep is confirmed.
 * Nothing leaves the tablet; it is bounded ([capWords] words, [capPatterns] shapes, [perWord] per word), the least confirmed go first, and old ones fade with the rest of the person's model.
 */
class PatternMemory(private val capWords: Int = 1200, private val capPatterns: Int = 3000, private val perWord: Int = 4) {
    private class Pat(val pts: FloatArray, var n: Float)
    private val byWord = HashMap<String, ArrayList<Pat>>()
    private var count = 0

    val isEmpty: Boolean @Synchronized get() = byWord.isEmpty()
    val wordCount: Int @Synchronized get() = byWord.size
    val patternCount: Int @Synchronized get() = count

    /** [word] was meant by a glide of this [print] ([weight]: 1 it was kept, 3 the person chose or retyped it). Near-identical shapes are merged into one with a running mean. */
    @Synchronized fun add(word: String, print: FloatArray, weight: Float = 1f) {
        val w = PersonalModel.norm(word) ?: return
        if (print.size != 2 * Fingerprint.K || weight <= 0f || print.any { it.isNaN() }) return
        val list = byWord.getOrPut(w) { ArrayList(2) }
        var nearest: Pat? = null; var nd = Float.MAX_VALUE
        for (p in list) { val d = Fingerprint.distance(p.pts, print); if (d < nd) { nd = d; nearest = p } }
        if (nearest != null && (nd <= MERGE_D || (list.size >= perWord && nd <= MERGE_D * 2f))) {
            val n0 = nearest.n; val n1 = n0 + weight
            for (i in print.indices) nearest.pts[i] = (nearest.pts[i] * n0 + print[i] * weight) / n1
            nearest.n = Math.min(n1, MAX_N)
        } else if (list.size < perWord) {
            list.add(Pat(print.copyOf(), Math.min(weight, MAX_N))); count++
        } else {
            val weakest = list.minByOrNull { it.n }!!
            list.remove(weakest); list.add(Pat(print.copyOf(), Math.min(weight, MAX_N)))
        }
        if (count > capPatterns || byWord.size > capWords) trim()
    }

    /** The shapes of [word] counted by how much they look like [print]: the sum of (how often confirmed x how close), 0 when the word has no shape or none is close. */
    @Synchronized fun evidence(word: String, print: FloatArray): Float {
        val list = byWord[word] ?: return 0f
        var e = 0f
        for (p in list) e += Math.min(p.n, MAX_N) * similarity(Fingerprint.distance(p.pts, print))
        return e
    }

    /**
     * The glide of [print] turned out to mean [meant]: every OTHER word whose shapes look like it is less sure now (what was guessed is the first to lose: "fir" after the person changed it to "for").
     * Shapes that fall under a third of a confirmation are forgotten.
     */
    @Synchronized fun contradict(meant: String, print: FloatArray, amount: Float = 2f) {
        val m = PersonalModel.norm(meant)
        val dead = ArrayList<String>()
        for ((w, list) in byWord) {
            if (w == m) continue
            val it = list.iterator()
            while (it.hasNext()) {
                val p = it.next()
                val s = similarity(Fingerprint.distance(p.pts, print))
                if (s < CONTRADICT_MIN) continue
                p.n -= amount * s
                if (p.n < 0.34f) { it.remove(); count-- }
            }
            if (list.isEmpty()) dead.add(w)
        }
        for (w in dead) byWord.remove(w)
    }

    /** How much a shape at [d] key units counts: 1 on top of it, about 0.6 at 0.45, about 0.13 at 0.9, nothing much beyond. */
    fun similarity(d: Float): Float { val z = d / SIGMA; return Math.exp((-0.5f * z * z).toDouble()).toFloat() }

    @Synchronized fun clear() { byWord.clear(); count = 0 }

    /** Old habits fade: every shape counts half, the ones that fell under a third of a confirmation are forgotten. */
    @Synchronized fun decay() {
        val dead = ArrayList<String>()
        for ((w, list) in byWord) {
            val it = list.iterator()
            while (it.hasNext()) { val p = it.next(); p.n *= 0.5f; if (p.n < 0.34f) { it.remove(); count-- } }
            if (list.isEmpty()) dead.add(w)
        }
        for (w in dead) byWord.remove(w)
    }

    /** Too many: the least confirmed tenth goes. */
    private fun trim() {
        val all = ArrayList<Float>(count)
        for (list in byWord.values) for (p in list) all.add(p.n)
        if (all.isEmpty()) return
        all.sort()
        val cut = all[all.size / 10]
        val dead = ArrayList<String>()
        for ((w, list) in byWord) {
            val it = list.iterator()
            while (it.hasNext()) { val p = it.next(); if (p.n <= cut && count > capPatterns * 9 / 10) { it.remove(); count-- } }
            if (list.isEmpty()) dead.add(w)
        }
        for (w in dead) byWord.remove(w)
        if (byWord.size > capWords) { // still too many words: the ones with the least confirmed shapes
            val order = byWord.entries.sortedBy { e -> e.value.sumOf { it.n.toDouble() } }
            var i = 0
            while (byWord.size > capWords * 9 / 10 && i < order.size) { count -= order[i].value.size; byWord.remove(order[i].key); i++ }
        }
    }

    // ------------------------------------------------------------------ keeping it

    /** One line per shape: `s word confirmations u0 v0 u1 v1 ...` with the numbers in tenths of a key (whole numbers), [Fingerprint.K] points. */
    @Synchronized fun serialize(): String {
        val sb = StringBuilder(count * 150)
        for ((w, list) in byWord) for (p in list) {
            sb.append("s ").append(w).append(' ').append("%.2f".format(p.n))
            for (x in p.pts) sb.append(' ').append(Math.round(x * 10f))
            sb.append('\n')
        }
        return sb.toString()
    }

    /** One `s` line ([tokens] already split); false when it is not a usable shape. */
    @Synchronized fun parseLine(tokens: List<String>): Boolean {
        if (tokens.size != 3 + 2 * Fingerprint.K || tokens[0] != "s") return false
        val w = PersonalModel.norm(tokens[1]) ?: return false
        val n = tokens[2].toFloatOrNull() ?: return false
        if (!(n > 0f) || n.isNaN()) return false
        val pts = FloatArray(2 * Fingerprint.K)
        for (i in pts.indices) { val t = tokens[3 + i].toIntOrNull() ?: return false; if (Math.abs(t) > 4000) return false; pts[i] = t / 10f }
        val list = byWord.getOrPut(w) { ArrayList(2) }
        if (list.size >= perWord || byWord.size > capWords || count >= capPatterns) { if (list.isEmpty()) byWord.remove(w); return false }
        list.add(Pat(pts, Math.min(n, MAX_N))); count++
        return true
    }

    /** Adds the shapes of [other] to these (an import): the same merging as when one is learned, so a file imported twice does not double what it holds more than the confirmations cap allows. */
    @Synchronized fun mergeFrom(other: PatternMemory) {
        val copy = other.snapshot()
        for ((w, list) in copy) for ((pts, n) in list) add(w, pts, n)
    }

    private fun snapshot(): List<Pair<String, List<Pair<FloatArray, Float>>>> = synchronized(this) { byWord.map { (w, l) -> w to l.map { it.pts.copyOf() to it.n } } }

    companion object {
        /** How wide "looks like" is (key units): the typical difference between two glides of the same word by the same hand is about 0.25 to 0.4. */
        const val SIGMA = 0.5f
        /** Two shapes this close are one (their average is kept, with both confirmations). */
        const val MERGE_D = 0.3f
        /** A shape counts at most this many confirmations. */
        const val MAX_N = 12f
        /** A shape must look at least this much like the glide for a correction to weaken it. */
        const val CONTRADICT_MIN = 0.3f
    }
}
