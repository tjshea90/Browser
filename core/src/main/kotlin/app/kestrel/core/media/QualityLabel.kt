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
}
