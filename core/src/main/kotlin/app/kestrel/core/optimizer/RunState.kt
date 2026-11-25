package app.kestrel.core.optimizer

enum class StepKind { BASELINE, EXPERIMENT, COMBINED }
enum class Status { RUNNING, PAUSED, DONE, ABORTED }

/** One thing the run does, in order. [samples] = passes of the benchmark suite wanted (an experiment may get one more when it is too close to call). [estMin] = minutes it is expected to take. */
class PlanStep(val kind: StepKind, val experimentId: String?, val samples: Int, val estMin: Double) {
    val label: String get() = when (kind) { StepKind.BASELINE -> "baseline"; StepKind.EXPERIMENT -> experimentId ?: "?"; StepKind.COMBINED -> "combined" }
}

/**
 * Everything a run knows, and nothing else: it is written to disk after every step, so a run that is killed, crashes or loses power resumes from exactly here (core RunMachine decides what is next from
 * this and nothing else). [bootConfig] is the configuration the NEXT engine start must use (the app writes it into its settings before the engine exists); [bootMark] is set when a restart was asked for
 * and taken away when the new engine process has produced its first sample: if a further process start finds it still set, the previous start died or hung and that step failed.
 */
data class RunState(
    val runId: String, val startedAt: Long, val priority: Priority, val budgetMin: Int, val steps: List<PlanStep>, val base: Config,
    val cursor: Int = 0, val taken: Int = 0, val extra: Int = 0,
    val samples: Map<Int, List<Sample>> = emptyMap(),
    val decisions: Map<String, Decision> = emptyMap(),
    val answers: Map<String, Boolean> = emptyMap(),
    val defaults: Map<String, String> = emptyMap(),
    val bootConfig: Config? = null, val running: Config? = null, val bootMark: Long = 0L, val bootStarts: Int = 0, val failures: Int = 0,
    val status: Status = Status.RUNNING, val note: String = "",
    val accepted: List<String> = emptyList(), val finalConfig: Config? = null, val finalNote: String = "",
    val doneMin: Double = 0.0, val engineVersion: String = "", val appVersion: String = "", val catalogVersion: Int = Catalog.VERSION,
) {
    val metrics: List<Metric> get() = Metrics.forPriority(priority)
    fun baselinePool(): List<Sample> = steps.indices.filter { steps[it].kind == StepKind.BASELINE }.flatMap { samples[it].orEmpty() }.filter { it.ok }
    val totalEstMin: Double get() = steps.sumOf { it.estMin }
    val remainingEstMin: Double get() = steps.drop(cursor).sumOf { it.estMin }
}

/** The on-disk text of a run: one line per fact, `|`-separated, text fields cleaned of `|` and line breaks. Reading is forgiving (unknown lines are ignored) so a newer app can read an older file. */
object RunStateCodec {
    private fun clean(s: String) = s.replace('|', '/').replace('\n', ' ').replace('\r', ' ')
    private fun num(d: Double) = java.lang.Double.toString(d)

    fun encode(s: RunState): String = buildString {
        fun line(vararg f: Any?) { append(f.joinToString("|") { clean(it?.toString() ?: "") }).append('\n') }
        line("v", 1); line("run", s.runId); line("started", s.startedAt); line("priority", s.priority.name); line("budget", s.budgetMin)
        line("base", s.base.canonical()); line("cursor", s.cursor); line("taken", s.taken); line("extra", s.extra)
        line("bootConfig", s.bootConfig?.canonical()); line("running", s.running?.canonical()); line("bootMark", s.bootMark); line("bootStarts", s.bootStarts); line("failures", s.failures)
        line("status", s.status.name); line("note", s.note); line("accepted", s.accepted.joinToString(",")); line("final", s.finalConfig?.canonical()); line("finalNote", s.finalNote)
        line("doneMin", num(s.doneMin)); line("engine", s.engineVersion); line("app", s.appVersion); line("catalog", s.catalogVersion)
        for (st in s.steps) line("step", st.kind.name, st.experimentId ?: "", st.samples, num(st.estMin))
        for ((i, l) in s.samples.toSortedMap()) for (x in l) line("sample", i, if (x.ok) 1 else 0, x.note, x.values.toSortedMap().entries.joinToString(",") { it.key + "=" + num(it.value) })
        for ((id, d) in s.decisions.toSortedMap()) line("dec", id, d.verdict.name, num(d.gain), num(d.threshold), num(d.noise), d.why)
        for ((id, a) in s.answers.toSortedMap()) line("ans", id, if (a) 1 else 0)
        for ((k, v) in s.defaults.toSortedMap()) line("def", k, v)
    }

    fun decode(text: String): RunState? {
        var runId = ""; var started = 0L; var priority = Priority.BALANCED; var budget = 75; var base = Config()
        var cursor = 0; var taken = 0; var extra = 0; var bootConfig: Config? = null; var running: Config? = null; var bootMark = 0L; var bootStarts = 0; var failures = 0
        var status = Status.RUNNING; var note = ""; var accepted = emptyList<String>(); var finalConfig: Config? = null; var finalNote = ""; var doneMin = 0.0; var engine = ""; var app = ""; var catalog = 0
        val steps = ArrayList<PlanStep>(); val samples = HashMap<Int, MutableList<Sample>>(); val decisions = LinkedHashMap<String, Decision>(); val answers = LinkedHashMap<String, Boolean>(); val defaults = LinkedHashMap<String, String>()
        var version = 0
        for (raw in text.lineSequence()) {
            if (raw.isEmpty()) continue
            val f = raw.split('|')
            fun d(i: Int) = f.getOrNull(i)?.toDoubleOrNull() ?: 0.0
            try {
                when (f[0]) {
                    "v" -> version = f[1].toInt()
                    "run" -> runId = f[1]; "started" -> started = f[1].toLong()
                    "priority" -> priority = Priority.values().firstOrNull { it.name == f[1] } ?: Priority.BALANCED
                    "budget" -> budget = f[1].toInt(); "base" -> base = Config.parse(f.getOrElse(1) { "" })
                    "cursor" -> cursor = f[1].toInt(); "taken" -> taken = f[1].toInt(); "extra" -> extra = f[1].toInt()
                    "bootConfig" -> bootConfig = f.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { Config.parse(it) }
                    "running" -> running = f.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { Config.parse(it) }
                    "bootMark" -> bootMark = f[1].toLong(); "bootStarts" -> bootStarts = f[1].toInt(); "failures" -> failures = f[1].toInt()
                    "status" -> status = Status.values().firstOrNull { it.name == f[1] } ?: Status.PAUSED
                    "note" -> note = f.getOrElse(1) { "" }; "accepted" -> accepted = f.getOrElse(1) { "" }.split(',').filter { it.isNotEmpty() }
                    "final" -> finalConfig = f.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { Config.parse(it) }
                    "finalNote" -> finalNote = f.getOrElse(1) { "" }; "doneMin" -> doneMin = d(1); "engine" -> engine = f.getOrElse(1) { "" }; "app" -> app = f.getOrElse(1) { "" }; "catalog" -> catalog = f[1].toInt()
                    "step" -> steps.add(PlanStep(StepKind.valueOf(f[1]), f[2].ifEmpty { null }, f[3].toInt(), d(4)))
                    "sample" -> {
                        val m = HashMap<String, Double>()
                        for (kv in f.getOrElse(4) { "" }.split(',')) { val e = kv.indexOf('='); if (e > 0) kv.substring(e + 1).toDoubleOrNull()?.let { m[kv.substring(0, e)] = it } }
                        samples.getOrPut(f[1].toInt()) { ArrayList() }.add(Sample(m, f[2] == "1", f.getOrElse(3) { "" }))
                    }
                    "dec" -> decisions[f[1]] = Decision(Verdict.valueOf(f[2]), d(3), d(4), d(5), f.getOrElse(6) { "" })
                    "ans" -> answers[f[1]] = f[2] == "1"
                    "def" -> defaults[f[1]] = f.getOrElse(2) { "" }
                }
            } catch (e: Exception) { /* a line this version cannot read is skipped, the rest is kept */ }
        }
        if (version < 1 || runId.isEmpty() || steps.isEmpty()) return null
        return RunState(runId, started, priority, budget, steps, base, cursor, taken, extra, samples, decisions, answers, defaults, bootConfig, running, bootMark, bootStarts, failures, status, note, accepted, finalConfig, finalNote, doneMin, engine, app, catalog)
    }
}
