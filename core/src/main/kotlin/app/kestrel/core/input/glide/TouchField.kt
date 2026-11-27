package app.kestrel.core.input.glide

/**
 * Owner R20d (2026-11-18): "I want it to be very good at predicting my glide typing." The decoder already learns ONE shift of the whole path (where the thumb lands beside the key it aims at). A thumb does not land the same
 * way everywhere: the left thumb of a two-thumb typist and the right thumb land differently, and a one-handed thumb falls short at the far side of the keyboard. This is a plane through those differences: how far the thumb
 * lands from the key it aims at, as a function of where on the keyboard the key is (f = w0 + w1 (u - 0.5) + w2 (v - 0.5), for x and for y: 6 numbers), fitted by ridge regression on what the keyboard sees, and taken off every
 * path before it is judged. research/R20-glide-personalization.md 4.1 (affine rather than a grid: with 18 numbers a hand with no pattern was made worse, with 6 it is not).
 *
 * Evidence is only what people aim at carefully: the first and last letter of a glide that was kept ([GlideDecoder.learnFrom]); the letters in between are biased by corner cutting. Old evidence fades ([KEEP]) and the plane
 * is trusted in proportion to how much evidence there is ([TRUST_HALF]), so it does nothing at first and never jumps. Coordinates are normalised to the letter keys ([u] across, [v] down, 0..1); offsets are in key pitches.
 * Pure Kotlin, a few doubles, nothing allocated per use.
 */
class TouchField {
    private val a = DoubleArray(9)          // sum of phi phi^T (3 x 3, row major), phi = (1, u - 0.5, v - 0.5)
    private val bx = DoubleArray(3)         // sum of phi * rx
    private val by = DoubleArray(3)         // sum of phi * ry
    private val w = DoubleArray(6)          // the plane: x then y, (w0, w1, w2) each
    /** Evidence so far (observations, fading). */
    var n = 0.0; private set
    private var sinceSolve = 0

    val isEmpty: Boolean get() = n < 1.0

    private fun trust() = n / (n + TRUST_HALF)

    /** The offset at ([u], [v]) in [out] (x, y), in pitches: what to take off a touch there. Zero while there is no evidence. */
    fun at(u: Float, v: Float, out: FloatArray) {
        val t = trust(); val du = u.coerceIn(0f, 1f) - 0.5; val dv = v.coerceIn(0f, 1f) - 0.5
        out[0] = (t * (w[0] + w[1] * du + w[2] * dv)).toFloat(); out[1] = (t * (w[3] + w[4] * du + w[5] * dv)).toFloat()
    }

    /**
     * One observation: at the key at ([u], [v]) the touch was ([rx], [ry]) pitches from the key's centre, beyond the single shift of the whole path. A wild one (more than [CLIP] pitches off: the matching put the letter in
     * the wrong place) is not trusted at all.
     */
    fun add(u: Float, v: Float, rx: Float, ry: Float) {
        if (Math.abs(rx) > CLIP || Math.abs(ry) > CLIP || rx.isNaN() || ry.isNaN()) return
        val p0 = 1.0; val p1 = u.coerceIn(0f, 1f) - 0.5; val p2 = v.coerceIn(0f, 1f) - 0.5
        val phi = doubleArrayOf(p0, p1, p2)
        for (i in 0 until 9) a[i] = KEEP * a[i] + phi[i / 3] * phi[i % 3]
        for (i in 0 until 3) { bx[i] = KEEP * bx[i] + phi[i] * rx; by[i] = KEEP * by[i] + phi[i] * ry }
        n = KEEP * n + 1.0
        if (++sinceSolve >= SOLVE_EVERY) solve()
    }

    /** Ridge solve of the two 3 x 3 systems: (A + diag(LAM_CONST, LAM_SLOPE, LAM_SLOPE)) w = b. */
    private fun solve() {
        sinceSolve = 0
        val m = DoubleArray(9) { a[it] }
        m[0] += LAM_CONST; m[4] += LAM_SLOPE; m[8] += LAM_SLOPE
        val det = m[0] * (m[4] * m[8] - m[5] * m[7]) - m[1] * (m[3] * m[8] - m[5] * m[6]) + m[2] * (m[3] * m[7] - m[4] * m[6])
        if (Math.abs(det) < 1e-9) return
        val inv = DoubleArray(9)
        inv[0] = (m[4] * m[8] - m[5] * m[7]) / det; inv[1] = (m[2] * m[7] - m[1] * m[8]) / det; inv[2] = (m[1] * m[5] - m[2] * m[4]) / det
        inv[3] = (m[5] * m[6] - m[3] * m[8]) / det; inv[4] = (m[0] * m[8] - m[2] * m[6]) / det; inv[5] = (m[2] * m[3] - m[0] * m[5]) / det
        inv[6] = (m[3] * m[7] - m[4] * m[6]) / det; inv[7] = (m[1] * m[6] - m[0] * m[7]) / det; inv[8] = (m[0] * m[4] - m[1] * m[3]) / det
        for (i in 0 until 3) {
            w[i] = (inv[i * 3] * bx[0] + inv[i * 3 + 1] * bx[1] + inv[i * 3 + 2] * bx[2]).coerceIn(-MAX, MAX)
            w[3 + i] = (inv[i * 3] * by[0] + inv[i * 3 + 1] * by[1] + inv[i * 3 + 2] * by[2]).coerceIn(-MAX, MAX)
        }
    }

    fun copyFrom(o: TouchField) {
        System.arraycopy(o.a, 0, a, 0, 9); System.arraycopy(o.bx, 0, bx, 0, 3); System.arraycopy(o.by, 0, by, 0, 3); System.arraycopy(o.w, 0, w, 0, 6); n = o.n; sinceSolve = o.sinceSolve
    }
    fun clear() { a.fill(0.0); bx.fill(0.0); by.fill(0.0); w.fill(0.0); n = 0.0; sinceSolve = 0 }

    /** The state as numbers for the private file: the plane (6), the evidence count (1), the two sums (9 + 3 + 3). */
    fun toArray(): FloatArray {
        val r = FloatArray(SIZE); var k = 0
        for (v in w) r[k++] = v.toFloat(); r[k++] = n.toFloat(); for (v in a) r[k++] = v.toFloat(); for (v in bx) r[k++] = v.toFloat(); for (v in by) r[k++] = v.toFloat()
        return r
    }
    /** True when [r] had the right length and sane numbers and was taken. */
    fun fromArray(r: FloatArray): Boolean {
        if (r.size != SIZE || r.any { it.isNaN() || it.isInfinite() } || r[6] < 0f) return false
        var k = 0
        for (i in 0 until 6) w[i] = r[k++].toDouble().coerceIn(-MAX, MAX); n = r[k++].toDouble().coerceAtMost(1e4)
        for (i in 0 until 9) a[i] = r[k++].toDouble(); for (i in 0 until 3) bx[i] = r[k++].toDouble(); for (i in 0 until 3) by[i] = r[k++].toDouble()
        sinceSolve = 0; return true
    }

    companion object {
        /** Numbers in [toArray]. */
        const val SIZE = 6 + 1 + 9 + 3 + 3
        /** No weight is larger than this (pitches), like the single shift's limit. */
        const val MAX = 0.6
        /** An observation further than this from its key (pitches) is not used. */
        const val CLIP = 0.9f
        /** How much of old evidence is kept per new observation (about 500 observations of memory). */
        const val KEEP = 0.998
        /** Evidence at which the plane is trusted by half. */
        const val TRUST_HALF = 30.0
        const val LAM_CONST = 0.5
        const val LAM_SLOPE = 2.0
        const val SOLVE_EVERY = 8
    }
}
