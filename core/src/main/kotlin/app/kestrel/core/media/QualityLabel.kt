package app.kestrel.core.media

/**
 * ANS53d (owner message 53: "put a setting for video quality in the player controls for as many videos as possible ... as many quality options as available, such as resolution, bitrate, and frames per second"):
 * what a quality row says. One row per picture a stream offers: the picture class (the SHORTER side: 1920 x 1080 and 1080 x 1920 are both 1080p), the frames a second and the bit rate, in the words the owner reads.
 * Pure, so the rule is pinned by a test; the Android side (Kestrel's own player, the sheet) only feeds it numbers.
 */
object QualityLabel {
    /** One picture of a stream: [w] x [h] pixels, [fps] frames a second (<= 0 = not said), [bps] bits a second (<= 0 = not said). */
    data class Rung(val w: Int, val h: Int, val fps: Float, val bps: Long)

    /** The picture class: the shorter side ("1080" for 1920 x 1080 and for 1080 x 1920). A side that is not known gives the other one; nothing known gives 0. */
    fun pClass(w: Int, h: Int): Int = if (w > 0 && h > 0) minOf(w, h) else maxOf(w, h, 0)

    /** "4.8 Mbit/s", "850 kbit/s"; empty when the stream does not say. */
    fun bitrate(bps: Long): String = when {
        bps <= 0L -> ""
        bps >= 1_000_000L -> { val t = (bps + 50_000L) / 100_000L; "${t / 10}.${t % 10} Mbit/s" }
        else -> "${(bps + 500L) / 1000L} kbit/s"
    }

    /** "30 fps" (29.97 and 30.0 both say 30); empty when the stream does not say. */
    fun fps(f: Float): String = if (f >= 1f && f <= 240f) "${Math.round(f)} fps" else ""

    /** "720p  ·  30 fps  ·  2.5 Mbit/s": whatever is known, in that order. */
    fun text(r: Rung): String {
        val p = pClass(r.w, r.h)
        return listOf(if (p > 0) "${p}p" else "", fps(r.fps), bitrate(r.bps)).filter { it.isNotEmpty() }.joinToString("  ·  ")
    }

    /** The rows of a quality list: biggest picture first, then the higher frame rate, then the higher bit rate; two rungs that would read the same are one row. */
    fun rows(rungs: List<Rung>): List<Rung> {
        val seen = HashSet<String>()
        return rungs.filter { pClass(it.w, it.h) > 0 }
            .sortedWith(compareByDescending<Rung> { pClass(it.w, it.h) }.thenByDescending { it.fps }.thenByDescending { it.bps })
            .filter { seen.add(text(it)) }
    }

    /** What the Automatic row says about what plays now, e.g. "now 720p  ·  30 fps". */
    fun now(current: Rung?): String = if (current == null || pClass(current.w, current.h) <= 0) "" else "now " + text(Rung(current.w, current.h, current.fps, 0L))

    // ---- ANS53d.2: a pick made in the list of a page's film travels to Kestrel's own player as text in an Intent, and is matched there against the pictures the player really finds

    /** "w,h,fps,bps" with whole numbers (the frame rate in thousandths), ASCII only: what an Intent carries. */
    fun encode(r: Rung): String = "${r.w},${r.h},${if (r.fps > 0f) Math.round(r.fps * 1000f) else 0},${maxOf(r.bps, 0L)}"

    /** The inverse of [encode]; null for anything else (an Intent from outside is never trusted). */
    fun decode(s: String?): Rung? {
        val p = s?.split(',') ?: return null
        if (p.size != 4) return null
        val w = p[0].toIntOrNull() ?: return null; val h = p[1].toIntOrNull() ?: return null
        val f = p[2].toIntOrNull() ?: return null; val b = p[3].toLongOrNull() ?: return null
        if (w < 0 || h < 0 || w > 16384 || h > 16384 || f < 0 || f > 1_000_000 || b < 0L) return null
        if (pClass(w, h) <= 0) return null                           // a pick always names a picture
        return Rung(w, h, if (f > 0) f / 1000f else -1f, b)
    }

    /**
     * The picture of [offered] that comes nearest to what the person picked ([want]): the same picture class first, else the biggest class below it, else the smallest above; then the nearest frame rate, then the
     * nearest bit rate (a rate that one side does not say counts as no difference). The index into [offered], or -1 when nothing with a picture is offered.
     */
    fun nearest(want: Rung, offered: List<Rung>): Int {
        val wc = pClass(want.w, want.h)
        var best = -1; var bestKey = DoubleArray(3)
        for ((i, o) in offered.withIndex()) {
            val oc = pClass(o.w, o.h)
            if (oc <= 0) continue
            val cls = if (oc == wc) 0.0 else if (oc < wc) 1_000_000.0 + (wc - oc) else 2_000_000.0 + (oc - wc)
            val fps = if (want.fps > 0f && o.fps > 0f) Math.abs(want.fps - o.fps).toDouble() else 0.0
            val bps = if (want.bps > 0L && o.bps > 0L) Math.abs(want.bps - o.bps).toDouble() else 0.0
            val key = doubleArrayOf(cls, fps, bps)
            if (best < 0 || less(key, bestKey)) { best = i; bestKey = key }
        }
        return best
    }

    private fun less(a: DoubleArray, b: DoubleArray): Boolean { for (i in a.indices) { if (a[i] < b[i]) return true; if (a[i] > b[i]) return false }; return false }
}
