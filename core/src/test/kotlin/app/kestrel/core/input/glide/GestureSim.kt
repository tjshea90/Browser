package app.kestrel.core.input.glide

import java.util.Random

/**
 * A made-up finger (AT1d). There is no recorded glide data to test accuracy on, so a seeded simulator draws paths the way a thumb does over the REAL key layout: it aims at every letter with some
 * scatter, rounds the corners off, drifts a little, is offset as a whole, and touches the glass at uneven steps. The accuracy numbers (research/GLIDE_TYPING.md) are only as honest as these
 * assumptions; the settings below are chosen so that the bad cases are worse than a careful person (the tablet's keys are about 4.7 mm apart).
 *
 * All distances are in dp (the keyboard is laid out in dp here), so the same noise can be tried on a smaller or a bigger keyboard.
 */
class SimNoise(
    /** Standard deviation of where the finger aims, per letter, in dp. */
    val aimDp: Float,
    /** How far before and after a letter the finger starts rounding the corner, in dp (the finger slows down and swings round the letter, so a sharp turn is cut by about a quarter of this). */
    val cut: Float,
    /** Standard deviation of a shift of the whole path, in dp. */
    val biasDp: Float,
    /** Touch noise per sample, in dp. */
    val jitterDp: Float,
    /** Amplitude of a slow drift, in dp. */
    val wobbleDp: Float,
    val name: String,
) {
    companion object {
        val CALM = SimNoise(5f, 8f, 2f, 0.8f, 2f, "calm (aim 5 dp)")
        val NORMAL = SimNoise(8f, 14f, 3f, 1.2f, 3f, "normal (aim 8 dp)")
        val SLOPPY = SimNoise(11f, 20f, 4.5f, 1.6f, 4f, "sloppy (aim 11 dp)")
        val BAD = SimNoise(14f, 26f, 6f, 2f, 5f, "bad (aim 14 dp)")
        fun aim(dp: Float) = SimNoise(dp, dp * 1.7f, dp * 0.4f, dp * 0.15f, dp * 0.4f, "aim $dp dp")
    }
}

class GestureSim(private val keys: GlideKeys, private val rnd: Random) {
    class Path(val xs: FloatArray, val ys: FloatArray)

    /** The touch points of one glide over [word]'s letters (px of the keyboard view), or null when the word has fewer than two different letters in a row. */
    fun path(word: String, noise: SimNoise): Path? {
        val k = GlideLetters.keyOf(word)
        val ls = ArrayList<Int>()
        for (b in k) if (ls.isEmpty() || ls.last() != b.toInt()) ls.add(b.toInt())
        if (ls.size < 2) return null
        val bx = rnd.nextGaussian().toFloat() * noise.biasDp; val by = rnd.nextGaussian().toFloat() * noise.biasDp
        val px = FloatArray(ls.size); val py = FloatArray(ls.size)
        for (j in ls.indices) {
            px[j] = keys.cx[ls[j]] + rnd.nextGaussian().toFloat() * noise.aimDp + bx
            py[j] = keys.cy[ls[j]] + rnd.nextGaussian().toFloat() * noise.aimDp + by
        }
        // a dense curve: straight between the aim points, with every corner rounded by a quadratic curve that starts and ends [cut] dp from the corner
        val dense = ArrayList<FloatArray>()
        val m = ls.size
        if (m == 2) {
            val bow = rnd.nextGaussian().toFloat() * noise.aimDp * 0.5f
            val dx = px[1] - px[0]; val dy = py[1] - py[0]; val len = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            for (s in 0..40) { val t = s / 40f; val off = bow * 4f * t * (1f - t); dense.add(floatArrayOf(px[0] + dx * t - dy / len * off, py[0] + dy * t + dx / len * off)) }
        } else {
            fun len(a: Int, b: Int) = Math.hypot((px[b] - px[a]).toDouble(), (py[b] - py[a]).toDouble()).toFloat().coerceAtLeast(1e-3f)
            var curX = px[0]; var curY = py[0]
            dense.add(floatArrayOf(curX, curY))
            for (j in 1 until m - 1) {
                val inL = len(j - 1, j); val outL = len(j, j + 1)
                val r = minOf(noise.cut, 0.45f * inL, 0.45f * outL)
                val ex = px[j] + (px[j - 1] - px[j]) / inL * r; val ey = py[j] + (py[j - 1] - py[j]) / inL * r
                val xx = px[j] + (px[j + 1] - px[j]) / outL * r; val xy = py[j] + (py[j + 1] - py[j]) / outL * r
                for (s in 1..20) { val t = s / 20f; dense.add(floatArrayOf(curX + (ex - curX) * t, curY + (ey - curY) * t)) }
                for (s in 1..12) { val t = s / 12f; val u = 1f - t; dense.add(floatArrayOf(u * u * ex + 2 * u * t * px[j] + t * t * xx, u * u * ey + 2 * u * t * py[j] + t * t * xy)) }
                curX = xx; curY = xy
            }
            for (s in 1..20) { val t = s / 20f; dense.add(floatArrayOf(curX + (px[m - 1] - curX) * t, curY + (py[m - 1] - curY) * t)) }
        }
        // arc length of the dense curve
        val cum = FloatArray(dense.size)
        for (i in 1 until dense.size) cum[i] = cum[i - 1] + Math.hypot((dense[i][0] - dense[i - 1][0]).toDouble(), (dense[i][1] - dense[i - 1][1]).toDouble()).toFloat()
        val total = cum.last()
        if (total < 1f) return null
        // touches at uneven steps, with jitter and a slow drift
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        var s = 0f; var seg = 1
        val phx = rnd.nextFloat() * 6.28f; val phy = rnd.nextFloat() * 6.28f
        val freq = 0.6f + rnd.nextFloat() * 1.2f
        while (true) {
            val at = s.coerceAtMost(total)
            while (seg < dense.size - 1 && cum[seg] < at) seg++
            val span = (cum[seg] - cum[seg - 1]).coerceAtLeast(1e-4f)
            val t = ((at - cum[seg - 1]) / span).coerceIn(0f, 1f)
            val x = dense[seg - 1][0] + (dense[seg][0] - dense[seg - 1][0]) * t
            val y = dense[seg - 1][1] + (dense[seg][1] - dense[seg - 1][1]) * t
            val ph = at / total * 6.28f * freq
            xs.add(x + rnd.nextGaussian().toFloat() * noise.jitterDp + noise.wobbleDp * Math.sin((ph + phx).toDouble()).toFloat())
            ys.add(y + rnd.nextGaussian().toFloat() * noise.jitterDp + noise.wobbleDp * Math.sin((ph + phy).toDouble()).toFloat())
            if (at >= total) break
            s += 1.5f + rnd.nextFloat() * 7f // dp between two touch events
        }
        return Path(xs.toFloatArray(), ys.toFloatArray())
    }
}
