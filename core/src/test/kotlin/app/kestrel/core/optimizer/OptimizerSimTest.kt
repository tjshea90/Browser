package app.kestrel.core.optimizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.exp

/** A pretend tablet: every configuration has a true effect per experiment, the numbers wobble like the real ones do (seeded, so a test never flakes). */
class World(
    private val effects: Map<String, Double> = emptyMap(),        // experiment id -> true gain on every scored metric (+0.10 = 10 % better)
    private val memDelta: Map<String, Double> = emptyMap(),       // experiment id -> extra MB
    private val compatBreak: Set<String> = emptySet(),            // experiments that make a feature test fail
    private val crashing: Set<String> = emptySet(),               // experiments whose start kills the app
    private val glitch: Set<String> = emptySet(),                 // experiments where the owner would see a drawing fault
    private val noise: Double = 0.05,
    private val flipDefault: String = "false",
) {
    val defaults: Map<String, String> = Catalog.engineNames().associateWith { flipDefault }
    private val base = mapOf(Metrics.SCRIPT to 2000.0, Metrics.LAYOUT to 500.0, Metrics.FPS_CSS to 30.0, Metrics.FPS_BLUR to 25.0, Metrics.FPS_CLIPS to 20.0, Metrics.LATE to 40.0,
        Metrics.SCROLL to 60.0, Metrics.INPUT to 120.0, Metrics.START to 4000.0, Metrics.CPU to 30000.0, Metrics.COMPAT to 25.0, Metrics.MEM to 600.0)

    fun active(c: Config): List<Experiment> = Catalog.all.filter { e ->
        e.changes.all { ch -> when (ch) {
            is Change.Setting -> c.settings[ch.prefKey] == ch.value
            is Change.Pref -> c.engine[ch.name] == ch.value
            is Change.Flip -> c.engine[ch.name] == (if (defaults[ch.name] == "true") "false" else "true")
        } }
    }.filter { e -> e.changes.any { ch -> !(ch is Change.Setting && BASE_SETTINGS[ch.prefKey] == ch.value) } }

    fun crashes(c: Config) = active(c).any { it.id in crashing }
    fun glitch(id: String) = id in glitch

    fun measure(c: Config, n: Int): Sample {
        val on = active(c)
        val gain = on.sumOf { effects[it.id] ?: 0.0 }
        val rng = Random(c.canonical().hashCode().toLong() * 31 + n)
        val out = HashMap<String, Double>()
        for (m in Metrics.balanced) {
            val dir = if (m.higherIsBetter) 1.0 else -1.0
            out[m.id] = base.getValue(m.id) * exp(dir * gain + rng.nextGaussian() * noise)
        }
        out[Metrics.COMPAT] = base.getValue(Metrics.COMPAT) - (if (on.any { it.id in compatBreak }) 2 else 0)
        out[Metrics.MEM] = base.getValue(Metrics.MEM) + on.sumOf { memDelta[it.id] ?: 0.0 } + rng.nextGaussian() * 5
        return Sample(out)
    }

    companion object { val BASE_SETTINGS = mapOf("draw_trial" to 0, "lab_fps30" to 0, "lab_dither" to 0, "lab_bytecode" to 0, "lab_gcpar" to 0, "scroll_feel" to 1, "limit_clips" to 1) }
}

class Sim(val world: World, priority: Priority = Priority.BALANCED, budgetMin: Int = 10_000, skip: Set<String> = emptySet()) {
    val machine = RunMachine()
    var st: RunState = machine.start("t1", 0L, priority, budgetMin, Config(emptyMap(), World.BASE_SETTINGS), skip, "161.0", "0.3.0-alpha107")
    var samples = 0; var restarts = 0; var asks = 0

    /** Runs to the end. [persist] true = after every step the state goes through its disk text and back (a kill at every possible point). */
    fun run(persist: Boolean = false, maxIter: Int = 20_000): RunState {
        var now = 1000L
        st = machine.onEngineUp(st, Config(emptyMap(), World.BASE_SETTINGS), world.defaults)
        for (i in 0 until maxIter) {
            if (persist) st = RunStateCodec.decode(RunStateCodec.encode(st))!!
            val (s2, a) = machine.next(st, now); st = s2; now += 1000
            when (a) {
                is Action.Restart -> {
                    restarts++
                    st = machine.onProcessStart(st)                                           // the process the run asked for starts
                    if (world.crashes(a.config)) st = machine.onProcessStart(st)             // ... and dies before a sample: the next start finds the mark
                    st = machine.onEngineUp(st, st.bootConfig!!, world.defaults)
                }
                is Action.ApplySettings -> {}
                is Action.Measure -> { st = machine.onSample(st, world.measure(st.running!!, samples++)) }
                is Action.Ask -> { asks++; st = machine.onAnswer(st, a.experimentId, !world.glitch(a.experimentId)) }
                is Action.Pause -> throw AssertionError("unexpected pause: " + a.reason)
                is Action.Finish -> return st
            }
        }
        throw AssertionError("did not finish")
    }
}

class OptimizerSimTest {
    private fun d(s: RunState, id: String) = s.decisions[id]

    @Test fun aCleanRunFindsTheRealGainsAndLeavesTheRestAlone() {
        val world = World(effects = mapOf("scroll0" to 0.10, "gcpar" to 0.09, "stylo2" to 0.08, "fps30" to -0.12, "dither" to 0.0, "bytecode" to 0.0), crashing = setOf("hw1"), compatBreak = setOf("nocaptchaobs"))
        val sim = Sim(world); val end = sim.run()
        assertEquals(Status.DONE, end.status)
        for (id in listOf("scroll0", "gcpar", "stylo2")) assertEquals("$id should be accepted: ${d(end, id)?.why}", Verdict.ACCEPT, d(end, id)?.verdict)
        assertEquals(Verdict.REJECT, d(end, "fps30")?.verdict)
        assertEquals("a feature test that fails rejects it", Verdict.REJECT, d(end, "nocaptchaobs")?.verdict)
        assertEquals("the app died at start: failed, not rejected", Verdict.FAILED, d(end, "hw1")?.verdict)
        for (id in listOf("dither", "bytecode")) assertTrue("$id has no effect: not accepted", d(end, id)?.verdict != Verdict.ACCEPT)
        val fin = end.finalConfig!!
        assertEquals("scroll feel Standard is kept", 0, fin.settings["scroll_feel"])
        assertEquals("parallel GC is kept", "true", fin.engine["javascript.options.mem.gc_parallel_marking"] ?: (if (fin.settings["lab_gcpar"] == 1) "true" else null))
        assertEquals("the harmful setting is not in the final configuration", 0, fin.settings["lab_fps30"])
        assertEquals("a crash in one experiment does not stop the run", 0, end.failures)
    }

    @Test fun anEveryStepKillAndRestoreGivesTheSameRun() {
        val world = World(effects = mapOf("scroll0" to 0.10, "gcpar" to 0.09, "fps30" to -0.12), crashing = setOf("hw2"))
        val a = Sim(world).run(persist = false)
        val b = Sim(world).run(persist = true)
        assertEquals(a.decisions.keys, b.decisions.keys)
        for (k in a.decisions.keys) { assertEquals(k, a.decisions[k]!!.verdict, b.decisions[k]!!.verdict); assertEquals(k, a.decisions[k]!!.gain, b.decisions[k]!!.gain, 1e-12) }
        assertEquals(a.finalConfig, b.finalConfig)
    }

    @Test fun noRealEffectsMeansNothingIsChanged() {
        var accepted = 0
        for (seed in 1..12) {
            val end = Sim(World(noise = 0.05 + 0.002 * seed, effects = emptyMap())).also { it.samples = seed * 1000 }.run()
            accepted += end.decisions.values.count { it.verdict == Verdict.ACCEPT }
        }
        assertTrue("false accepts over 12 runs of 20 experiments with no effect: $accepted (limit 4)", accepted <= 4)
    }

    @Test fun aSmallRealGainIsStillFoundWhenTheTabletIsQuiet() {
        val end = Sim(World(effects = mapOf("stylo2" to 0.06), noise = 0.02)).run()
        assertEquals(end.decisions["stylo2"]!!.why, Verdict.ACCEPT, end.decisions["stylo2"]!!.verdict)
    }

    @Test fun theHardwareTrialNeedsTheOwnersAnswerAndAGlitchRejectsIt() {
        val sim = Sim(World(effects = mapOf("hw1" to 0.30, "hw2" to 0.30), glitch = setOf("hw1")))
        val end = sim.run()
        assertTrue("it asked the owner", sim.asks >= 1)
        assertEquals("a glitch the owner saw fails the experiment", Verdict.FAILED, d(end, "hw1")?.verdict)
        assertEquals("the second variant is a separate question and passes", Verdict.ACCEPT, d(end, "hw2")?.verdict)
        assertEquals("hardware drawing variant 2 is in the final configuration", 2, end.finalConfig!!.settings["draw_trial"])
    }

    @Test fun twoConflictingVariantsNeverEndUpTogether() {
        val end = Sim(World(effects = mapOf("hw1" to 0.20, "hw2" to 0.25, "scroll0" to 0.1, "scroll2" to 0.12))).run()
        val fin = end.finalConfig!!
        assertTrue("draw_trial is one value", fin.settings["draw_trial"] in listOf(0, 1, 2))
        assertTrue("only one of the two accepted draw variants is applied", end.accepted.count { it == "hw1" || it == "hw2" } <= 1)
        assertTrue("only one scroll feel is applied", end.accepted.count { it == "scroll0" || it == "scroll2" } <= 1)
    }

    @Test fun extraMemoryBeyondTheLimitRejectsAFastSetting() {
        val end = Sim(World(effects = mapOf("proc2" to 0.2), memDelta = mapOf("proc2" to 200.0))).run()
        assertEquals(Verdict.REJECT, d(end, "proc2")?.verdict)
        assertTrue(d(end, "proc2")!!.why.contains("memory"))
    }

    @Test fun twoFailedStartsInARowStopTheRunAndRestoreTheBase() {
        val world = World(crashing = setOf("hw1", "hw2", "fps30", "dither", "wrpaths", "wrpaths_swgl", "fontations", "defer_decode", "bytecode", "gcpar", "gcsmall", "nursery", "jitoff", "stylo2", "nocaptchaobs", "noformfill", "proc2"))
        val end = Sim(world).run()
        // the live-only experiments (scroll feel, clips) can still be measured; every restart-experiment crashes. Failures are consecutive only when no sample lands between them.
        assertTrue(end.status == Status.DONE || end.status == Status.ABORTED)
        if (end.status == Status.ABORTED) assertEquals(Config(emptyMap(), World.BASE_SETTINGS), end.bootConfig)
    }

    @Test fun theTimeBudgetLeavesTheRestForTheNextRun() {
        val end = Sim(World(effects = mapOf("scroll0" to 0.1)), budgetMin = 12).run()
        assertEquals(Status.DONE, end.status)
        assertTrue("some experiments were left for next time", end.decisions.values.count { it.why.startsWith("skipped: out of time") } >= 5)
    }

    @Test fun anExperimentSettledEarlierIsNotRepeated() {
        val end = Sim(World(), skip = setOf("scroll0", "hw1")).run()
        assertFalse(end.steps.any { it.experimentId == "scroll0" || it.experimentId == "hw1" })
    }

    @Test fun aClosedAppPausesTheRunAndBootsWithTheOwnersSettings() {
        val m = RunMachine()
        val base = Config(emptyMap(), World.BASE_SETTINGS)
        var s = m.start("r", 0L, Priority.BALANCED, 75, base, emptySet(), "161.0", "x")
        s = m.onEngineUp(s, base, emptyMap())
        s = m.onSample(s, Sample(mapOf(Metrics.SCRIPT to 1.0)))
        val after = m.onProcessStart(s)                      // no restart was asked for: the app was closed
        assertEquals(Status.PAUSED, after.status)
        assertEquals(base, after.bootConfig)
        assertTrue(after.note.contains("closed"))
        val back = m.onResume(after)
        assertEquals(Status.RUNNING, back.status)
    }

    @Test fun aRestartThatNeverSteadiesIsAFailureAndTheNextStartIsTheBase() {
        val m = RunMachine()
        val base = Config(emptyMap(), World.BASE_SETTINGS)
        var s = m.onEngineUp(m.start("r", 0L, Priority.BALANCED, 75, base, emptySet(), "161.0", "x"), base, World(effects = emptyMap()).defaults)
        // run until the first Restart is asked
        var n = 0
        var restartAsked = false
        for (i in 0 until 2000) {
            val (s2, a) = m.next(s, 1000L); s = s2
            if (a is Action.Restart) { restartAsked = true; break }
            if (a is Action.Measure) s = m.onSample(s, World().measure(s.running!!, n++))
            if (a is Action.Finish) throw AssertionError("no restart came")
        }
        assertTrue("a restart was asked for", restartAsked)
        assertTrue(s.bootMark != 0L)
        s = m.onProcessStart(s)                              // the requested start
        assertEquals(1, s.bootStarts)
        s = m.onProcessStart(s)                              // another start: the first never produced a sample
        assertEquals(1, s.failures)
        assertEquals(base, s.bootConfig)
        assertEquals(0L, s.bootMark)
        assertTrue(s.decisions.values.any { it.verdict == Verdict.FAILED })
    }

    @Test fun theStateTextRoundTripsExactly() {
        val sim = Sim(World(effects = mapOf("scroll0" to 0.1)))
        val end = sim.run()
        val text = RunStateCodec.encode(end)
        val back = RunStateCodec.decode(text)
        assertNotNull(back)
        assertEquals(text, RunStateCodec.encode(back!!))
        assertEquals(end.finalConfig, back.finalConfig)
        assertEquals(end.accepted, back.accepted)
    }

    @Test fun anUnreadableStateIsNoRun() {
        assertEquals(null, RunStateCodec.decode(""))
        assertEquals(null, RunStateCodec.decode("garbage\nmore garbage"))
    }
}
