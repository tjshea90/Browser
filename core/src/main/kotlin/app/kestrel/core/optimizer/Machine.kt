package app.kestrel.core.optimizer

import kotlin.math.max

/** What the app must do next. Everything else (what the benchmark pages measure, how the restart is done) is the Android side's job. */
sealed class Action {
    /** Put the browser into [config]: write its engine preferences and settings, restart the engine process, and come back. */
    class Restart(val config: Config, val why: String) : Action()
    /** Only app settings differ: apply them now. */
    class ApplySettings(val config: Config) : Action()
    /** Run the benchmark suite once and report it with [RunMachine.onSample]. */
    object Measure : Action()
    /** Show the text page and ask the owner whether anything is missing or covered. */
    class Ask(val experimentId: String, val text: String) : Action()
    class Pause(val reason: String) : Action()
    class Finish(val status: Status) : Action()
}

object PlanBuilder {
    private fun cost(e: Experiment) = if (e.restart) 3.0 else 1.6

    /** Experiments in the order they pay off: expected gain per minute, biggest first. */
    fun order(catalog: List<Experiment>): List<Experiment> = catalog.sortedWith(compareByDescending<Experiment> { it.prior / cost(it) }.thenBy { it.id })

    /**
     * Baseline (2 samples), then the experiments in thirds with one baseline sample between the thirds (the drift of the tablet over the hour shows in them), the combination, and a closing baseline (2).
     * [skip] = ids settled in an earlier run on this engine and app version.
     */
    fun build(catalog: List<Experiment>, skip: Set<String> = emptySet()): List<PlanStep> {
        val exps = order(catalog.filter { it.id !in skip })
        val third = max(1, (exps.size + 2) / 3)
        val steps = ArrayList<PlanStep>()
        steps.add(PlanStep(StepKind.BASELINE, null, 2, 2.6))
        exps.forEachIndexed { i, e ->
            if (i > 0 && i % third == 0) steps.add(PlanStep(StepKind.BASELINE, null, 1, 1.4))
            steps.add(PlanStep(StepKind.EXPERIMENT, e.id, 2, cost(e) + 1.2))
        }
        steps.add(PlanStep(StepKind.COMBINED, null, 2, 4.2))
        steps.add(PlanStep(StepKind.BASELINE, null, 2, 4.0))
        return steps
    }
}

/**
 * The whole decision logic of a run as pure functions on [RunState]: no clock, no files, no Android. The app feeds it events (process start, a sample, an answer) and does what [next] says; because
 * everything it needs is in the state, a run that is killed at ANY point resumes to the same decisions (core tests kill and restore it at every step).
 */
/** What a planned restart costs in real time (stop, start, warm-up page): about 25 s on the tablet. */
const val RESTART_MIN = 0.4

class RunMachine(private val catalog: List<Experiment> = Catalog.all) {
    private fun exp(id: String?) = catalog.firstOrNull { it.id == id }

    fun start(runId: String, now: Long, priority: Priority, budgetMin: Int, base: Config, skip: Set<String>, engineVersion: String, appVersion: String, only: Set<String> = emptySet()): RunState =
        RunState(runId, now, priority, budgetMin, PlanBuilder.build(catalog.filter { only.isEmpty() || it.id in only }, skip), base, bootConfig = base, running = base, engineVersion = engineVersion, appVersion = appVersion)

    // ---------------------------------------------------------------------------------------------------------------- events

    /**
     * A NEW app process started (the guard calls this before the engine exists) and the answer's [RunState.bootConfig] is what the engine must start with.
     *  - a start the run asked for (the mark is set) is normal the first time;
     *  - a second process start while the mark is still set means the previous one never produced a sample: the step it was for FAILED and the next start uses the base configuration; two such
     *    failures in a row stop the run;
     *  - a start with NO restart asked for means the app was closed or died in the middle of a run: the run PAUSES and the browser starts with the person's own settings (never left in a test setting);
     *  - a paused or aborted run always boots with the base configuration; a finished one leaves the settings as the run applied them.
     */
    fun onProcessStart(s: RunState): RunState {
        if (s.status == Status.DONE) return s
        if (s.status != Status.RUNNING) return s.copy(bootConfig = s.base, running = null)
        if (s.bootMark == 0L) return s.copy(status = Status.PAUSED, note = "the app was closed in the middle of the run", bootConfig = s.base, running = null, samples = s.samples - s.cursor, taken = 0, extra = 0)
        if (s.bootStarts == 0) return s.copy(bootStarts = 1)
        val f = s.failures + 1
        var t = s
        if (s.cursor < s.steps.size) {
            val step = s.steps[s.cursor]
            t = when (step.kind) {
                StepKind.BASELINE -> t.copy(note = "a start with the baseline settings did not finish")
                else -> failStep(t, "the app died or hung while starting with this setting")
            }
        }
        t = t.copy(bootConfig = s.base, running = null, bootMark = 0L, bootStarts = 0, failures = f)
        return if (f >= 2) t.copy(status = Status.ABORTED, note = "two starts in a row failed: the tablet is not stable enough to test, nothing was changed") else t
    }

    /** The engine is up with [actual] (the app read the preferences back) and [defaults] are the engine's own values of the names the catalog can flip. */
    fun onEngineUp(s: RunState, actual: Config, defaults: Map<String, String>): RunState = s.copy(running = actual, defaults = defaults + s.defaults)

    fun onSample(s: RunState, sample: Sample): RunState {
        if (s.cursor >= s.steps.size) return s
        val list = s.samples[s.cursor].orEmpty() + sample
        // the budget is REAL work time: a sample adds the time its suite took (suite_s, about 44 s on the tablet; 45 s when a test does not say). The first real run was planned at 4 minutes a step and 75 in all, took 31, and skipped three experiments as 'out of time'.
        val addMin = (sample.values["suite_s"] ?: 45.0) / 60.0
        return s.copy(samples = s.samples + (s.cursor to list), taken = s.taken + 1, bootMark = 0L, bootStarts = 0, failures = if (sample.ok) 0 else s.failures, doneMin = s.doneMin + addMin)
    }

    fun onAnswer(s: RunState, experimentId: String, ok: Boolean): RunState = s.copy(answers = s.answers + (experimentId to ok))

    /** The engine did not run with the wanted preferences (read back after the start): the setting could not be tested. */
    fun onMismatch(s: RunState, why: String): RunState = if (s.cursor < s.steps.size && s.steps[s.cursor].kind != StepKind.BASELINE) failStep(s, why) else s.copy(note = why)

    fun onPause(s: RunState, reason: String): RunState = if (s.status == Status.RUNNING) s.copy(status = Status.PAUSED, note = reason) else s
    fun onResume(s: RunState): RunState = if (s.status == Status.PAUSED) s.copy(status = Status.RUNNING, note = "") else s
    fun onAbort(s: RunState, reason: String): RunState = if (s.status == Status.RUNNING || s.status == Status.PAUSED) s.copy(status = Status.ABORTED, note = reason) else s

    // ---------------------------------------------------------------------------------------------------------------- what is next

    /** What the app should do now (and the state, possibly with steps skipped or decided along the way). Safe to call again after any interruption. */
    fun next(s0: RunState, now: Long): Pair<RunState, Action> {
        var s = s0
        for (guard in 0 until 1000) {
            when (s.status) { Status.ABORTED -> return s to Action.Finish(Status.ABORTED); Status.DONE -> return s to Action.Finish(Status.DONE); Status.PAUSED -> return s to Action.Pause(s.note); else -> {} }
            if (s.cursor >= s.steps.size) { s = finalizeRun(s); continue }
            val step = s.steps[s.cursor]
            val e = exp(step.experimentId)
            if (step.kind == StepKind.EXPERIMENT) {
                if (e == null) { s = skipStep(s, "unknown experiment"); continue }
                if (s.taken == 0 && s.doneMin >= s.budgetMin) { s = skipStep(s, "out of time: it waits for the next run"); continue }
                val r = skipReason(s, e); if (r != null) { s = skipStep(s, r); continue }
            }
            if (step.kind == StepKind.COMBINED && s.taken == 0) {
                s = redecideAll(s)
                if (s.accepted.isEmpty()) { s = skipStep(s, "nothing was accepted, so there is nothing to combine"); continue }
            }
            val want = wanted(s, step, e)
            if (want == null) { s = failStep(s, "the engine's own value of that setting could not be read"); continue }
            val running = s.running ?: return s.copy(bootConfig = want, bootMark = now, bootStarts = 0, doneMin = s.doneMin + RESTART_MIN) to Action.Restart(want, "the engine must start with: " + want.enginePart().ifEmpty { "its own settings" })
            if (want.enginePart() != running.enginePart()) return s.copy(bootConfig = want, running = null, bootMark = now, bootStarts = 0, doneMin = s.doneMin + RESTART_MIN) to Action.Restart(want, "the next test needs the engine to start with: " + want.enginePart().ifEmpty { "its own settings" })
            if (want.livePart() != running.livePart()) return s.copy(running = Config(running.engine, running.settings.filterKeys { it !in want.settings } + want.settings)) to Action.ApplySettings(want)
            val needed = step.samples + s.extra
            if (s.taken < needed) return s to Action.Measure
            when (step.kind) {
                StepKind.BASELINE -> { s = advance(s); continue }
                StepKind.EXPERIMENT -> {
                    val cand = s.samples[s.cursor].orEmpty()
                    val pool = s.baselinePool()
                    val g = Guards(maxMemMb = e!!.memLimitMb.toDouble())
                    if (cand.any { !it.ok }) { s = decide(s, e.id, Decider.decide(pool, cand, s.metrics, g)); continue }
                    if (Decider.borderline(pool, cand, s.metrics) && s.extra == 0) return s.copy(extra = 1) to Action.Measure
                    val prelim = Decider.decide(pool, cand, s.metrics, g)
                    if (e.askGlitch && e.id !in s.answers && prelim.verdict != Verdict.REJECT && prelim.verdict != Verdict.FAILED && prelim.gain > 0.0)
                        return s to Action.Ask(e.id, "Look at the screen. Is any text cut off, missing or covered by a block, or does anything look wrong?")
                    s = decide(s, e.id, Decider.decide(pool, cand, s.metrics, g, s.answers[e.id]))
                    continue
                }
                StepKind.COMBINED -> { s = advance(finishCombined(s)); continue }
            }
        }
        return s.copy(status = Status.ABORTED, note = "internal loop guard") to Action.Finish(Status.ABORTED)
    }

    // ---------------------------------------------------------------------------------------------------------------- pieces

    private fun advance(s: RunState): RunState {
        val step = s.steps[s.cursor]
        return s.copy(cursor = s.cursor + 1, taken = 0, extra = 0)      // (doneMin counts REAL minutes since alpha108: each sample and each restart adds its own, see onSample / next)
    }
    private fun decide(s: RunState, id: String, d: Decision): RunState = advance(s.copy(decisions = s.decisions + (id to d)))
    private fun failStep(s: RunState, why: String): RunState {
        if (s.cursor >= s.steps.size) return s.copy(note = why)
        val step = s.steps[s.cursor]
        val id = step.experimentId ?: step.label
        return advance(s.copy(decisions = s.decisions + (id to Decision(Verdict.FAILED, 0.0, Decider.MIN_THRESHOLD, 0.0, why))))
    }
    private fun skipStep(s: RunState, why: String): RunState {
        val step = s.steps[s.cursor]
        val id = step.experimentId ?: step.label
        return advance(s.copy(decisions = s.decisions + (id to Decision(Verdict.INCONCLUSIVE, 0.0, Decider.MIN_THRESHOLD, 0.0, "skipped: $why"))))
    }

    private fun skipReason(s: RunState, e: Experiment): String? {
        val hardwareNow = (s.base.settings["draw_trial"] ?: 0) > 0
        if (e.needsSoftware && hardwareNow) return "it is for processor drawing and hardware drawing is on"
        if (e.hardwareOnly && !hardwareNow) return "it is for hardware drawing, which is off"
        if (e.id == "hw2" && s.decisions["hw1"]?.let { it.verdict == Verdict.FAILED && it.why.contains("died") } == true) return "hardware drawing already stopped the app in the first test"
        if (e.changes.all { c -> when (c) { is Change.Setting -> s.base.settings[c.prefKey] == c.value; is Change.Pref -> s.base.engine[c.name] == c.value; is Change.Flip -> false } }) return "you already have it this way"
        return null
    }

    private fun wanted(s: RunState, step: PlanStep, e: Experiment?): Config? = when (step.kind) {
        StepKind.BASELINE -> s.base
        StepKind.EXPERIMENT -> s.base.with(e!!.changes, s.defaults)
        StepKind.COMBINED -> s.base.with(s.accepted.mapNotNull { exp(it) }.flatMap { it.changes }, s.defaults)
    }

    /** Re-judge every measured experiment against ALL baseline samples taken so far (more samples = a fairer noise estimate), and list the accepted ones, best first, without two that change the same thing. */
    private fun redecideAll(s: RunState): RunState {
        val pool = s.baselinePool()
        var dec = s.decisions
        for ((i, step) in s.steps.withIndex()) {
            if (step.kind != StepKind.EXPERIMENT) continue
            val id = step.experimentId ?: continue
            val cand = s.samples[i].orEmpty()
            if (cand.isEmpty() || dec[id]?.why?.startsWith("skipped") == true) continue    // skipped, or failed before any sample (died, setting not reached): nothing to re-judge
            val e = exp(id) ?: continue
            dec = dec + (id to Decider.decide(pool, cand, s.metrics, Guards(maxMemMb = e.memLimitMb.toDouble()), s.answers[id]))
        }
        val ranked = dec.entries.filter { it.value.verdict == Verdict.ACCEPT }.sortedByDescending { it.value.gain }.mapNotNull { exp(it.key) }
        val claimed = HashSet<String>(); val chosen = ArrayList<String>()
        val hardware = ranked.any { it.id == "hw1" || it.id == "hw2" }
        for (e in ranked) {
            if (e.needsSoftware && hardware) continue
            if (e.changes.any { it.key in claimed }) continue
            chosen.add(e.id); e.changes.forEach { claimed.add(it.key) }
        }
        return s.copy(decisions = dec, accepted = chosen)
    }

    /** The combination was measured: keep it only if it is at least as good as the best single change (an interaction can cancel gains). */
    private fun finishCombined(s: RunState): RunState {
        val pool = s.baselinePool()
        val cand = s.samples[s.cursor].orEmpty()
        val d = Decider.decide(pool, cand, s.metrics, Guards(maxMemMb = 200.0), null)
        val best = s.accepted.mapNotNull { id -> s.decisions[id]?.let { id to it } }.maxByOrNull { it.second.gain }
        val combined = s.base.with(s.accepted.mapNotNull { exp(it) }.flatMap { it.changes }, s.defaults)
        if (d.verdict == Verdict.ACCEPT && combined != null && (best == null || d.gain >= best.second.gain - d.threshold / 2))
            return s.copy(decisions = s.decisions + ("combined" to d), finalConfig = combined, finalNote = "all ${s.accepted.size} accepted changes together: ${d.why}")
        val single = best?.let { exp(it.first) }?.let { s.base.with(it.changes, s.defaults) }
        return s.copy(decisions = s.decisions + ("combined" to d), finalConfig = single ?: s.base,
            finalNote = if (single != null) "the changes did not add up (${d.why}): only '${best.first}' is kept" else "nothing is kept: ${d.why}")
    }

    private fun finalizeRun(s: RunState): RunState {
        var t = redecideAll(s)
        // the combined result stays valid only if the accepted set is still what was combined
        val combinedIds = t.finalConfig
        if (combinedIds == null) {
            val single = t.accepted.firstOrNull()?.let { exp(it) }?.let { t.base.with(it.changes, t.defaults) }
            t = t.copy(finalConfig = single ?: t.base, finalNote = if (single != null) "only '${t.accepted.first()}' is kept" else "nothing was proven better: your settings stay as they are")
        } else {
            val wantedSet = t.base.with(t.accepted.mapNotNull { exp(it) }.flatMap { it.changes }, t.defaults)
            if (wantedSet != combinedIds && t.finalNote.startsWith("all ")) {
                val single = t.accepted.firstOrNull()?.let { exp(it) }?.let { t.base.with(it.changes, t.defaults) }
                t = t.copy(finalConfig = single ?: t.base, finalNote = if (single != null) "after the closing baseline only '${t.accepted.first()}' stayed proven" else "after the closing baseline nothing stayed proven")
            }
        }
        return t.copy(status = Status.DONE, note = "")
    }
}
