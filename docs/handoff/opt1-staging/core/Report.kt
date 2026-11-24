package app.kestrel.core.optimizer

import kotlin.math.abs
import kotlin.math.exp

/** What the run says about itself: plain words for the screen, one JSON text for Claude, and the lines that remember what was settled. */
object OptReport {
    private fun title(id: String): String = Catalog.byId(id)?.title ?: when (id) { "combined" -> "All accepted changes together"; "baseline" -> "Baseline"; else -> id }

    private fun verdictWord(v: Verdict) = when (v) {
        Verdict.ACCEPT -> "KEPT"; Verdict.REJECT -> "dropped (worse or unsafe)"; Verdict.INCONCLUSIVE -> "no clear difference"; Verdict.FAILED -> "could not be tested"
    }

    /** How the final configuration did against the baselines, per metric, as a signed percentage (+ = better), for the metrics both have. */
    fun effects(s: RunState): List<Pair<Metric, Double>> {
        val stepIdx = s.steps.indexOfFirst { it.kind == StepKind.COMBINED }
        if (stepIdx < 0) return emptyList()
        val cand = s.samples[stepIdx].orEmpty().filter { it.ok }
        if (cand.isEmpty()) return emptyList()
        val ref = Decider.reference(s.baselinePool()); val c = Decider.reference(cand)
        return s.metrics.filter { it.id in ref && it.id in c }.map { it to (exp(it.gain(ref.getValue(it.id), c.getValue(it.id))) - 1.0) }
    }

    fun headline(s: RunState): String {
        val kept = s.accepted.size
        return when (s.status) {
            Status.DONE -> if (s.finalConfig == null || s.finalConfig == s.base) "Auto-tune finished: nothing was proven better, your settings stay as they are."
                           else "Auto-tune finished: ${kept} change${if (kept == 1) "" else "s"} proven better and applied."
            Status.ABORTED -> "Auto-tune stopped: ${s.note.ifEmpty { "it could not finish" }}. Your settings are as they were."
            Status.PAUSED -> "Auto-tune is paused: ${s.note.ifEmpty { "waiting for you" }}."
            Status.RUNNING -> "Auto-tune is running."
        }
    }

    /** The lines of the report screen. */
    fun words(s: RunState): List<String> {
        val out = ArrayList<String>()
        out.add(headline(s))
        if (s.finalNote.isNotEmpty()) out.add(s.finalNote.replaceFirstChar { it.uppercase() } + ".")
        val fx = effects(s)
        if (fx.isNotEmpty() && s.finalConfig != null && s.finalConfig != s.base) {
            out.add("")
            out.add("What changed, measured on this tablet (best settings against your own):")
            for ((m, g) in fx.sortedByDescending { it.second }) out.add("  " + m.label + ": " + (if (g >= 0) "+" else "") + String.format(java.util.Locale.US, "%.0f", g * 100) + " %" + (if (abs(g) < 0.03) " (within the noise)" else ""))
        }
        out.add("")
        out.add("Every test:")
        for (st in s.steps) {
            val id = st.experimentId ?: continue
            val d = s.decisions[id] ?: continue
            out.add("  " + title(id) + ": " + verdictWord(d.verdict) + " - " + d.why)
        }
        s.decisions["combined"]?.let { out.add("  Combined: " + verdictWord(it.verdict) + " - " + it.why) }
        val noise = Decider.noise(s.baselinePool(), s.metrics)
        out.add("")
        out.add("The tablet's own measurement noise this run: about " + String.format(java.util.Locale.US, "%.1f", noise * 100) + " % (a change must beat three times that to be kept).")
        return out
    }

    private fun js(x: String): String {
        val b = StringBuilder("\"")
        for (c in x) when { c == '"' -> b.append("\\\""); c == '\\' -> b.append("\\\\"); c == '\n' -> b.append("\\n"); c == '\r' -> {}; c < ' ' -> b.append(' '); else -> b.append(c) }
        return b.append('"').toString()
    }
    private fun num(d: Double) = if (d.isNaN() || d.isInfinite()) "null" else java.lang.Double.toString(d)

    /** The whole run as JSON for the private repo (everything measured, every decision). [device] and [extra] are facts from the Android side (RAM, battery, thermal, owner's answers). */
    fun json(s: RunState, device: Map<String, String>, extra: Map<String, String>): String {
        val b = StringBuilder("{")
        b.append("\"kind\":\"kestrel-autotune\",\"version\":1,\"run\":").append(js(s.runId)).append(",\"startedAt\":").append(s.startedAt)
        b.append(",\"status\":").append(js(s.status.name)).append(",\"note\":").append(js(s.note)).append(",\"priority\":").append(js(s.priority.name))
        b.append(",\"engine\":").append(js(s.engineVersion)).append(",\"app\":").append(js(s.appVersion)).append(",\"catalog\":").append(s.catalogVersion).append(",\"budgetMin\":").append(s.budgetMin)
        b.append(",\"base\":").append(js(s.base.canonical())).append(",\"final\":").append(js(s.finalConfig?.canonical() ?: "")).append(",\"finalNote\":").append(js(s.finalNote))
        b.append(",\"accepted\":[").append(s.accepted.joinToString(",") { js(it) }).append("]")
        b.append(",\"noise\":").append(num(Decider.noise(s.baselinePool(), s.metrics)))
        b.append(",\"device\":{").append(device.entries.joinToString(",") { js(it.key) + ":" + js(it.value) }).append("}")
        b.append(",\"extra\":{").append(extra.entries.joinToString(",") { js(it.key) + ":" + js(it.value) }).append("}")
        b.append(",\"answers\":{").append(s.answers.entries.joinToString(",") { js(it.key) + ":" + it.value }).append("}")
        b.append(",\"defaults\":{").append(s.defaults.entries.joinToString(",") { js(it.key) + ":" + js(it.value) }).append("}")
        b.append(",\"decisions\":[").append(s.decisions.entries.joinToString(",") { (id, d) ->
            "{\"id\":" + js(id) + ",\"verdict\":" + js(d.verdict.name) + ",\"gain\":" + num(d.gain) + ",\"threshold\":" + num(d.threshold) + ",\"noise\":" + num(d.noise) + ",\"why\":" + js(d.why) + "}" }).append("]")
        b.append(",\"steps\":[")
        b.append(s.steps.withIndex().joinToString(",") { (i, st) ->
            "{\"i\":" + i + ",\"kind\":" + js(st.kind.name) + ",\"id\":" + js(st.experimentId ?: "") + ",\"samples\":[" +
                s.samples[i].orEmpty().joinToString(",") { x -> "{\"ok\":" + x.ok + ",\"note\":" + js(x.note) + ",\"m\":{" + x.values.toSortedMap().entries.joinToString(",") { js(it.key) + ":" + num(it.value) } + "}}" } + "]}" })
        b.append("]}")
        return b.toString()
    }

    /** `id|VERDICT|engine|catalog` for every experiment that has a verdict: what the next run may skip. */
    fun settledLines(s: RunState): List<String> = s.decisions.filter { Catalog.byId(it.key) != null }.map { "${it.key}|${it.value.verdict.name}|${s.engineVersion}|${s.catalogVersion}" }

    /** Experiments proven (kept or dropped) for this engine and catalog: not repeated unless the person asks for everything again. */
    fun settledIds(lines: List<String>, engineVersion: String, catalogVersion: Int): Set<String> =
        lines.mapNotNull { l -> val f = l.split('|'); if (f.size >= 4 && f[2] == engineVersion && f[3] == catalogVersion.toString() && (f[1] == "ACCEPT" || f[1] == "REJECT")) f[0] else null }.toSet()
}
