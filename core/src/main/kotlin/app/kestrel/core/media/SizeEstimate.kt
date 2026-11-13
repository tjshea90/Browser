package app.kestrel.core.media

/**
 * How big a stream will be, before it is downloaded and while it is (AO4's sibling AO2, owner message 29: "the video downloader is inaccurate at estimating file size. Is there any way to improve it?").
 *
 * A stream's playlist does not say how big the file is. It says how much bandwidth the player may need: HLS `BANDWIDTH` is the PEAK of any part, which for variable-rate video is often 20 to 50 % above what
 * the file really averages, so `BANDWIDTH x length` overshoots (and `AVERAGE-BANDWIDTH`, when a site sends it, is better but still the encoder's word). What does not lie is the size of the parts themselves:
 *  - BEFORE a download a few parts are measured (a one-byte Range request answers with the whole length of the part), spread over the timeline, and the declared rate of every quality is scaled by how far the
 *    measured quality was off ([calibration]);
 *  - WHILE it downloads the finished parts are weighed against how much of the video they cover ([running]), weighted by length not by count (the last part is short), and the figure shown at the start
 *    gives way to that measurement as more of the video is in.
 */
object SizeEstimate {
    /** An MPEG-TS stream saved as MP4 comes out a little smaller than the stream (no 188-byte packet framing, no ADTS headers). */
    const val TS_TO_MP4 = 0.95

    /** How many parts are worth measuring before a download starts: each costs one tiny request, and variable-rate video differs by a third from part to part, so four were not enough (error about 10 %). */
    const val SAMPLES = 8

    /**
     * Which parts to measure out of [n]: spread evenly over the timeline (the first part often carries a logo, the last is short; neither is typical). Every part when there are no more than [max].
     */
    fun sampleIndexes(n: Int, max: Int = SAMPLES): List<Int> {
        if (n <= 0 || max <= 0) return emptyList()
        if (n <= max) return (0 until n).toList()
        return (0 until max).map { (((it + 0.5) / max) * n).toInt().coerceIn(0, n - 1) }.distinct()
    }

    /** Bytes per second of the parts that were measured, each weighted by its length (a part that could not be measured, size <= 0, is left out); -1.0 when nothing is usable. */
    fun measuredRate(sizes: List<Long>, durationsMs: List<Long>): Double {
        var bytes = 0L; var ms = 0L
        for (i in sizes.indices) {
            val d = durationsMs.getOrElse(i) { 0L }
            if (sizes[i] > 0 && d > 0) { bytes += sizes[i]; ms += d }
        }
        return if (ms <= 0) -1.0 else bytes * 1000.0 / ms
    }

    /** What the finished file will weigh at [rate] bytes per second over [totalMs]; [container] "ts" parts are saved as MP4 ([TS_TO_MP4]). */
    fun total(rate: Double, totalMs: Long, container: String): Long {
        if (rate <= 0 || totalMs <= 0) return -1L
        val factor = if (container == "ts") TS_TO_MP4 else 1.0
        return (rate * totalMs / 1000.0 * factor).toLong()
    }

    /** How far the playlist's declared rate was off for the quality that was measured: measured / declared, kept to a sane band; 1.0 when either is unknown. */
    fun calibration(measuredRate: Double, declaredBytesPerSec: Long): Double {
        if (measuredRate <= 0 || declaredBytesPerSec <= 0) return 1.0
        return (measuredRate / declaredBytesPerSec).coerceIn(0.3, 1.25)
    }

    /** A quality's size from its declared rate, scaled by the [calibration] of the one that was measured. */
    fun calibrated(declaredBytesPerSec: Long, calibration: Double, totalMs: Long, container: String): Long =
        total(declaredBytesPerSec * calibration, totalMs, container)

    /**
     * The size while a download runs: what the finished parts weigh ([written] bytes covering [doneMs] of [totalMs]) scaled up to the whole length, blended with the [prior] (the figure shown at the start, -1 =
     * none). The measurement takes over completely once [FULL_TRUST] of the video is in; with no prior it is used at once.
     */
    fun running(prior: Long, written: Long, doneMs: Long, totalMs: Long): Long {
        if (doneMs <= 0 || totalMs <= 0 || written <= 0) return prior
        val measured = written.toDouble() * totalMs / doneMs
        if (prior <= 0) return measured.toLong()
        val w = (doneMs.toDouble() / totalMs / FULL_TRUST).coerceIn(0.0, 1.0)
        return (measured * w + prior * (1.0 - w)).toLong()
    }

    /** The share of the video that has to be in before the figure is only the measurement. */
    const val FULL_TRUST = 0.125
}
