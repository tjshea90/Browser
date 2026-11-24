package app.kestrel.core.optimizer

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/** One pass of the whole benchmark suite: every number it produced. [ok] false = a page failed or timed out (its numbers are missing, the sample cannot be trusted). */
class Sample(val values: Map<String, Double>, val ok: Boolean = true, val note: String = "")

enum class Verdict { ACCEPT, REJECT, INCONCLUSIVE, FAILED }

/**
 * @param gain the fractional improvement the weighted score shows (+0.08 = 8 % better, whichever way each metric runs)
 * @param threshold the gain needed to believe it (twice the tablet's own noise that hour, at least [Decider.MIN_THRESHOLD])
 */
class Decision(val verdict: Verdict, val gain: Double, val threshold: Double, val noise: Double, val why: String)

/** Limits for the metrics that are only compared, never scored. */
class Guards(val maxMemMb: Double = 60.0, val maxSingleLoss: Double = 0.20)

object Stats {
    fun stdev(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val m = xs.sum() / xs.size
        return sqrt(xs.sumOf { (it - m) * (it - m) } / (xs.size - 1))
    }
    fun median(xs: List<Double>): Double {
        if (xs.isEmpty()) return 0.0
        val s = xs.sorted(); val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }
    /** Median absolute deviation scaled to a standard deviation (1.4826), the noise measure that one odd sample cannot ruin. */
    fun madSigma(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val m = median(xs)
        return 1.4826 * median(xs.map { abs(it - m) })
    }
}

object Decider {
    const val MIN_THRESHOLD = 0.025
    /** Per-metric noise never counts as less than this (a few repeats can agree by luck). */
    const val NOISE_FLOOR = 0.03

    /** The median of every metric across the baseline samples that are ok: the reference everything is compared with. */
    fun reference(baselines: List<Sample>): Map<String, Double> {
        val ok = baselines.filter { it.ok }
        val ids = ok.flatMap { it.values.keys }.toSet()
        return ids.associateWith { id -> Stats.median(ok.mapNotNull { it.values[id] }) }
    }

    /** How many noise units a gain must clear: three, because a tool that CHANGES settings must be far more afraid of a lucky number than of a missed small gain. */
    const val K = 3.0

    private fun metricNoise(m: Metric, baselines: List<Sample>): Double {
        val logs = baselines.filter { it.ok }.mapNotNull { it.values[m.id] }.map { ln(it + m.eps) }
        return max(NOISE_FLOOR, max(Stats.madSigma(logs), Stats.stdev(logs)))      // MAD ignores one odd sample, the standard deviation does not: with few samples the larger of the two
    }

    /** The score noise: how much the weighted gain moves between samples of the SAME configuration. */
    fun noise(baselines: List<Sample>, metrics: List<Metric>): Double {
        val ids = reference(baselines).keys
        val used = metrics.filter { it.id in ids }
        val wsum = used.sumOf { it.weight }
        if (wsum <= 0.0) return NOISE_FLOOR
        return sqrt(used.sumOf { (it.weight * metricNoise(it, baselines)).let { x -> x * x } }) / wsum
    }

    /** Weighted gain of a candidate (the median of its samples per metric) over the reference. + = better. */
    fun gain(baselines: List<Sample>, candidate: List<Sample>, metrics: List<Metric>): Double {
        val ref = reference(baselines)
        val cand = reference(candidate)
        val used = metrics.filter { it.id in ref && it.id in cand }
        val wsum = used.sumOf { it.weight }
        if (wsum <= 0.0) return 0.0
        return exp(used.sumOf { it.weight * it.gain(ref.getValue(it.id), cand.getValue(it.id)) } / wsum) - 1.0
    }

    /** The metric that got worst, as (metric, loss as a fraction), for the "hurts one thing a lot" guard. */
    fun worst(baselines: List<Sample>, candidate: List<Sample>, metrics: List<Metric>): Pair<Metric, Double>? {
        val ref = reference(baselines); val cand = reference(candidate)
        return metrics.filter { it.id in ref && it.id in cand }.map { it to (1.0 - exp(it.gain(ref.getValue(it.id), cand.getValue(it.id)))) }.maxByOrNull { it.second }
    }

    /**
     * The gain needed to believe a candidate measured [nCand] times against these baselines: [K] noise units of the DIFFERENCE of two averages (the baseline has its own noise, the candidate too),
     * and never under [MIN_THRESHOLD].
     */
    fun threshold(baselines: List<Sample>, nCand: Int, metrics: List<Metric>): Double {
        val nb = max(1, baselines.count { it.ok })
        return max(MIN_THRESHOLD, K * noise(baselines, metrics) * sqrt(1.0 / max(1, nCand) + 1.0 / nb))
    }

    /** True when the verdict is too close to call and one more sample (no extra restart: same engine session) is worth its minute. */
    fun borderline(baselines: List<Sample>, candidate: List<Sample>, metrics: List<Metric>): Boolean {
        val thr = threshold(baselines, candidate.size, metrics)
        val g = abs(gain(baselines, candidate, metrics))
        return candidate.size < 3 && g > thr / 2 && g < thr * 2
    }

    fun decide(baselines: List<Sample>, candidate: List<Sample>, metrics: List<Metric>, guards: Guards, ownerOk: Boolean? = null): Decision {
        val okBase = baselines.filter { it.ok }
        if (okBase.size < 2) return Decision(Verdict.INCONCLUSIVE, 0.0, MIN_THRESHOLD, 0.0, "not enough baseline samples to judge")
        if (candidate.isEmpty()) return Decision(Verdict.FAILED, 0.0, MIN_THRESHOLD, 0.0, "no sample was taken")
        val bad = candidate.firstOrNull { !it.ok }
        if (bad != null) return Decision(Verdict.FAILED, 0.0, MIN_THRESHOLD, 0.0, "a benchmark page failed: " + bad.note.ifEmpty { "no reason given" })
        if (ownerOk == false) return Decision(Verdict.FAILED, 0.0, MIN_THRESHOLD, 0.0, "you saw something wrong on the screen")
        val noise = noise(baselines, metrics)
        val thr = threshold(baselines, candidate.size, metrics)
        val g = gain(baselines, candidate, metrics)
        val everySampleBetter = candidate.all { gain(baselines, listOf(it), metrics) > 0.0 }
        val ref = reference(baselines); val cand = reference(candidate)
        val compatBase = ref[Metrics.COMPAT]; val compatCand = cand[Metrics.COMPAT]
        if (compatBase != null && compatCand != null && compatCand < compatBase) return Decision(Verdict.REJECT, g, thr, noise, "a feature test that passed before fails (${compatCand.toInt()} of ${compatBase.toInt()})")
        val memBase = ref[Metrics.MEM]; val memCand = cand[Metrics.MEM]
        if (memBase != null && memCand != null && memCand - memBase > guards.maxMemMb) return Decision(Verdict.REJECT, g, thr, noise, "it uses ${(memCand - memBase).toInt()} MB more memory (limit ${guards.maxMemMb.toInt()})")
        val w = worst(baselines, candidate, metrics)
        if (w != null && w.second > guards.maxSingleLoss) return Decision(Verdict.REJECT, g, thr, noise, "it makes '${w.first.label}' ${(w.second * 100).toInt()} % worse")
        return when {
            g >= thr && !everySampleBetter -> Decision(Verdict.INCONCLUSIVE, g, thr, noise, "better by ${pct(g)} on average, but not in every sample: not proven, left as it was")
            g >= thr -> Decision(Verdict.ACCEPT, g, thr, noise, "better by ${pct(g)} (needs ${pct(thr)})")
            g <= -thr -> Decision(Verdict.REJECT, g, thr, noise, "worse by ${pct(-g)} (needs ${pct(thr)})")
            else -> Decision(Verdict.INCONCLUSIVE, g, thr, noise, "${if (g >= 0) "better" else "worse"} by ${pct(abs(g))}, inside the noise of ${pct(thr)}: not proven, left as it was")
        }
    }

    fun pct(x: Double): String = String.format(java.util.Locale.US, "%.1f %%", x * 100)
}
