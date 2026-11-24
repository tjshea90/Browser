package app.kestrel.core.optimizer

import kotlin.math.ln

/**
 * OPT1 (research/OPTIMIZER.md): the numbers the benchmark pages and the runner report, and how much each one counts.
 * A metric is `higherIsBetter` (frames a second) or lower is better (milliseconds). [eps] keeps a ratio defined when a value is near zero (late frames: 0 %).
 */
class Metric(val id: String, val label: String, val unit: String, val higherIsBetter: Boolean, val weight: Double, val eps: Double = 1.0) {
    /** The change from [base] to [now] as a signed log ratio: positive = better, whichever direction the metric runs. */
    fun gain(base: Double, now: Double): Double = if (higherIsBetter) ln((now + eps) / (base + eps)) else ln((base + eps) / (now + eps))
}

/** What the owner said matters most (the question before a run). Shifts the weights; the guards never move. */
enum class Priority(val label: String) {
    BALANCED("A bit of everything"), LOADING("Pages loading fast"), SMOOTH("Smooth scrolling, typing and animation"), BATTERY("Battery and heat");
}

object Metrics {
    const val SCRIPT = "script_ms"; const val LAYOUT = "layout_ms"; const val FPS_CSS = "fps_css"; const val FPS_BLUR = "fps_blur"; const val FPS_CLIPS = "fps_clips"
    const val LATE = "late_pct"; const val SCROLL = "scroll_gap_p95"; const val INPUT = "input_p95"; const val START = "start_ms"; const val CPU = "cpu_ms"
    /** Guards: never scored, only compared. */
    const val COMPAT = "compat_pass"; const val MEM = "mem_mb"

    private fun m(id: String, label: String, unit: String, higher: Boolean, w: Double) = Metric(id, label, unit, higher, w)

    /** The scored metrics with the BALANCED weights (they sum to 1.0). */
    val balanced: List<Metric> = listOf(
        m(SCRIPT, "Script speed", "ms", false, 0.18), m(LAYOUT, "Page layout", "ms", false, 0.09),
        m(FPS_CSS, "Animation smoothness", "fps", true, 0.10), m(FPS_BLUR, "Gradient and blur smoothness", "fps", true, 0.10), m(FPS_CLIPS, "Looping clip strip smoothness", "fps", true, 0.14),
        m(LATE, "Late frames", "%", false, 0.05), m(SCROLL, "Scrolling hitches", "ms", false, 0.10), m(INPUT, "Typing and tap wait", "ms", false, 0.14),
        m(START, "Start of the engine and first page", "ms", false, 0.05), m(CPU, "Processor time used", "ms", false, 0.05),
    )

    private val boost = mapOf(
        Priority.BALANCED to emptyMap<String, Double>(),
        Priority.LOADING to mapOf(SCRIPT to 2.0, LAYOUT to 2.0, START to 3.0, FPS_CLIPS to 0.5),
        Priority.SMOOTH to mapOf(FPS_CSS to 1.6, FPS_BLUR to 1.6, FPS_CLIPS to 1.4, SCROLL to 2.0, INPUT to 2.0, LATE to 2.0, START to 0.5),
        Priority.BATTERY to mapOf(CPU to 6.0, LATE to 1.5, FPS_CLIPS to 0.6),
    )

    /** The metrics for a priority, weights scaled so they still sum to 1.0. */
    fun forPriority(p: Priority): List<Metric> {
        val b = boost[p].orEmpty()
        val raw = balanced.map { it to it.weight * (b[it.id] ?: 1.0) }
        val sum = raw.sumOf { it.second }
        return raw.map { (m, w) -> Metric(m.id, m.label, m.unit, m.higherIsBetter, w / sum, m.eps) }
    }

    fun byId(list: List<Metric>): Map<String, Metric> = list.associateBy { it.id }
}
