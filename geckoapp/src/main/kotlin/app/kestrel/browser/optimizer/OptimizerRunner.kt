package app.kestrel.browser.optimizer

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import app.kestrel.browser.Flavor
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.diag.Health
import app.kestrel.browser.diag.LiveDiag
import app.kestrel.browser.diag.PrivateGuard
import app.kestrel.browser.GeckoEngine
import app.kestrel.core.optimizer.Action
import app.kestrel.core.optimizer.Catalog
import app.kestrel.core.optimizer.Config
import app.kestrel.core.optimizer.Metrics
import app.kestrel.core.optimizer.OptReport
import app.kestrel.core.optimizer.Priority
import app.kestrel.core.optimizer.RunMachine
import app.kestrel.core.optimizer.RunState
import app.kestrel.core.optimizer.Sample
import app.kestrel.core.optimizer.Status
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.PanZoomController
import org.mozilla.geckoview.ScreenLength
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * OPT1 (research/OPTIMIZER.md): carries out what the pure run machine (core RunMachine) says, one step at a time, and writes the state to disk before and after each. It owns the engine session the
 * benchmark pages run in. Everything it measures goes to the machine as a [Sample]; every decision is the machine's, so this class has no judgement in it, only plumbing: restart, apply, measure, ask.
 */
class OptimizerRunner(private val act: Activity, private val view: GeckoView, private val ui: Ui) {
    interface Ui {
        fun status(title: String, detail: String, done: Double, total: Double)
        fun ask(text: String, yes: String, no: String, answer: (Boolean) -> Unit)
        fun paused(reason: String)
        fun report(lines: List<String>, canUndo: Boolean, restartNeeded: Boolean)
        fun preflight(last: List<String>?)
        fun say(text: String)
    }

    private val ctx = act.applicationContext
    private val prefs = Prefs.get(ctx)
    private val machine = RunMachine()
    private val server = BenchServer(ctx)
    private var session: GeckoSession? = null
    private var state: RunState? = null
    private var generation = 0                     // a stop or a pause bumps it: a suite thread that sees another number throws its sample away
    private var seq = 0
    private var currentPage = ""
    private var scrollPulses = 0
    private var startMs = -1.0
    private var firstPaintAt = 0L
    @Volatile private var pageDied = false
    private var engineUp = false
    /** True from the moment a restart is asked for: the screen going away is then part of the restart, and must not be saved as "the person left" (the new process would find a paused run). */
    @Volatile private var restarting = false

    // ---------------------------------------------------------------------------------------------------- entry points

    /** The activity is up: continue a run, show a paused one, or show the start screen (with the last report, if any). */
    fun begin() {
        server.start()
        openSession()
        val st = OptimizerStore.load(ctx)
        state = st
        when {
            st == null -> ui.preflight(OptimizerStore.last(ctx)?.let { OptReport.words(it) })
            st.status == Status.RUNNING -> engineReady()
            st.status == Status.PAUSED -> ui.paused(st.note)
            else -> finish(st.status)
        }
    }

    /** [quick] = the emulator lab's short run (debuggable builds only): three experiments, fast pages. */
    fun startRun(priority: Priority, retestAll: Boolean, quick: Boolean = false) {
        if (PrivateGuard.active()) { ui.say("Close your private tabs first: Auto-tune never runs while one is open."); return }
        val base = OptimizerApply.snapshot(prefs)
        val skip = if (retestAll) emptySet() else OptReport.settledIds(OptimizerStore.history(ctx), Flavor.engineVersion, Catalog.VERSION)
        val runId = (if (quick) "q-" else "") + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        state = machine.start(runId, System.currentTimeMillis(), priority, 75, base, skip, Flavor.engineVersion, appVersion(), if (quick) setOf("scroll0", "gcpar", "fps30") else emptySet())
        persist(); Health.log("opt", "start", "run=$runId priority=$priority skip=${skip.size}")
        // a CLEAN process for the run: the browser's own tabs and pages must not share the tablet's 2 GB with the benchmarks, so the first step is a planned restart into this screen alone
        state = state!!.copy(bootMark = System.currentTimeMillis(), bootStarts = 0, running = null, bootConfig = base)
        persist()
        ui.say("Restarting Kestrel for a clean start. Your tabs come back afterwards.")
        restarting = true
        ProcessRestart.restartInto(ctx, Intent(ctx, OptimizerActivity::class.java).putExtra("resume", true), act)
    }

    fun cont() { val st = state ?: return; state = machine.onResume(st); persist(); engineReady() }

    /** The person left the app or pressed pause: nothing is lost, the run waits. */
    fun pause(reason: String) {
        val st = state ?: return
        if (restarting || st.status != Status.RUNNING) return
        generation++
        state = machine.onPause(st, reason); persist(); Health.log("opt", "pause", reason)
        ui.paused(reason)
    }

    fun stop() {
        val st = state ?: return
        generation++
        state = machine.onAbort(st, "you stopped it"); persist(); finish(Status.ABORTED)
    }

    fun undo() {
        val last = OptimizerStore.last(ctx) ?: return
        OptimizerApply.write(prefs, last.base)
        ui.say("Your earlier settings are back. Restarting Kestrel to apply them.")
        restartIntoBrowser()
    }

    fun sendLast() {
        val json = OptimizerStore.lastJson(ctx) ?: return ui.say("There is nothing to send yet.")
        upload(json, force = true)
    }

    fun restartIntoBrowser() { restarting = true; ProcessRestart.restartInto(ctx, Intent(ctx, app.kestrel.browser.MainActivity::class.java), act) }

    fun close() { generation++; server.stop(); try { view.releaseSession() } catch (e: Exception) { /* none */ }; try { session?.close() } catch (e: Exception) { /* closed */ }; session = null }

    // ---------------------------------------------------------------------------------------------------- the engine session

    private fun openSession() {
        val s = GeckoSession(org.mozilla.geckoview.GeckoSessionSettings.Builder().usePrivateMode(false).useTrackingProtection(true).build())
        s.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                if (title == "bench-ready scroll" && currentPage == "scroll") startScroll()
                if (title != null && title.startsWith("bench-done")) scrollPulses = 0
            }
            override fun onCrash(session: GeckoSession) { pageDied("the page process crashed") }
            override fun onKill(session: GeckoSession) { pageDied("the page process was ended by the system") }
            override fun onFirstContentfulPaint(session: GeckoSession) { if (firstPaintAt == 0L) firstPaintAt = SystemClock.elapsedRealtime() }
        }
        s.promptDelegate = object : GeckoSession.PromptDelegate {}
        s.open(GeckoEngine.runtime(ctx))
        view.setSession(s)
        session = s
    }

    private fun pageDied(why: String) {
        Health.log("opt", "page-died", why); pageDied = true
        Io.main.post { try { view.releaseSession(); session?.close() } catch (e: Exception) { /* gone */ }; openSession() }
        server.fail(currentPage, seq, why)
    }

    // ---------------------------------------------------------------------------------------------------- start of a process

    /** The engine of THIS process runs with what the guard wrote. Read what it really runs with, tell the machine, and go on. */
    private fun engineReady() {
        val st = state ?: return
        if (st.status != Status.RUNNING) return
        ProcessRestart.clearFallback(ctx)
        val actual = OptimizerApply.snapshot(prefs)
        val flipNames = Catalog.all.flatMap { it.changes }.filterIsInstance<app.kestrel.core.optimizer.Change.Flip>().map { it.name }.filter { it !in actual.engine }
        val verify = verifyList(actual)
        val names = (flipNames + verify.keys).distinct()
        Io.main.post {
            try {
                GeckoPreferenceController.getGeckoPrefs(names).accept({ list ->
                    val got = HashMap<String, String>(); list?.forEach { got[it.pref] = it.value?.toString() ?: "" }
                    val defaults = flipNames.mapNotNull { n -> got[n]?.takeIf { it == "true" || it == "false" }?.let { n to it } }.toMap()
                    var s = machine.onEngineUp(state ?: st, actual, defaults)
                    val bad = verify.entries.firstOrNull { got[it.key] != null && got[it.key] != it.value }
                    if (bad != null) s = machine.onMismatch(s, "the setting did not reach the engine (${bad.key} is ${got[bad.key]}, wanted ${bad.value})")
                    state = s; persist(); engineUp = true; warmUpThenLoop()
                }, { e -> Health.log("opt", "read-prefs-failed", e.toString().take(100)); state = machine.onEngineUp(state ?: st, actual, emptyMap()); persist(); engineUp = true; warmUpThenLoop() })
            } catch (e: Exception) { Health.log("opt", "read-prefs-failed", e.toString().take(100)); state = machine.onEngineUp(state ?: st, actual, emptyMap()); persist(); engineUp = true; warmUpThenLoop() }
        }
    }

    /** What each configuration says the engine must be running with, by name (booleans and numbers as the engine reports them). */
    private fun verifyList(c: Config): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        m.putAll(c.engine)
        if ((c.settings["draw_trial"] ?: 0) > 0) m["gfx.webrender.use-optimized-shaders"] = "false"
        if (c.settings["lab_fps30"] == 1) m["layout.frame_rate"] = "30"
        if (c.settings["lab_dither"] == 1) m["gfx.webrender.dithering"] = "false"
        if (c.settings["lab_bytecode"] == 1) m["dom.script_loader.bytecode_cache.strategy"] = "-1"
        if (c.settings["lab_gcpar"] == 1) m["javascript.options.mem.gc_parallel_marking"] = "true"
        return m
    }

    /** The first page of a process also installs the extension and warms the engine: it is loaded, waited for, and not measured (its time is the start time of this process). */
    private fun warmUpThenLoop() {
        val bridge = app.kestrel.browser.web.ShieldsBridge(prefs = { prefs }, tabForPage = { null }, blockedOnTab = { })
        try { app.kestrel.browser.web.ShieldsBridge.install(ctx, bridge) } catch (e: Exception) { Health.log("opt", "shields-install-failed", e.toString().take(100)) }
        if (startMs < 0) {
            firstPaintAt = 0L; currentPage = "text"; seq++
            session?.loadUri(server.url("text", seq))
            val born = Process.getStartElapsedRealtime()
            val deadline = SystemClock.elapsedRealtime() + 20_000
            val poll = object : Runnable { override fun run() {
                if (firstPaintAt != 0L) { startMs = (firstPaintAt - born).toDouble(); Io.main.postDelayed({ loop() }, 3500) }
                else if (SystemClock.elapsedRealtime() > deadline) { startMs = (SystemClock.elapsedRealtime() - born).toDouble(); loop() }
                else Io.main.postDelayed(this, 250)
            } }
            Io.main.postDelayed(poll, 250)
        } else loop()
    }

    // ---------------------------------------------------------------------------------------------------- the loop

    private fun persist() { state?.let { OptimizerStore.save(ctx, it) } }

    private fun loop() {
        val st0 = state ?: return
        val (st1, action) = machine.next(st0, System.currentTimeMillis())
        state = st1; persist(); showStatus(st1, action)
        when (action) {
            is Action.Restart -> { Health.log("opt", "restart", action.why.take(140)); OptimizerApply.write(prefs, action.config); persist(); restarting = true; ProcessRestart.restartInto(ctx, Intent(ctx, OptimizerActivity::class.java).putExtra("resume", true), act) }
            is Action.ApplySettings -> { OptimizerApply.write(prefs, action.config); applyLive(); loop() }
            is Action.Measure -> { val why = pauseReason(); if (why != null) pause(why) else measure() }
            is Action.Ask -> ask(action)
            is Action.Pause -> ui.paused(action.reason)
            is Action.Finish -> finish(action.status)
        }
    }

    private fun applyLive() { try { GeckoEngine.applyScrollFeel(prefs.scrollFeel) } catch (e: Exception) { /* the next page start picks it up */ } }

    private fun showStatus(st: RunState, a: Action) {
        val step = st.steps.getOrNull(st.cursor)
        val what = when (a) {
            is Action.Restart -> "Restarting the engine for the next test"
            is Action.Measure -> "Measuring"
            is Action.Ask -> "Asking you"
            else -> "Working"
        }
        val name = step?.let { s -> s.experimentId?.let { Catalog.byId(it)?.title } ?: if (s.kind.name == "BASELINE") "baseline (your settings as they are)" else "all kept changes together" } ?: "finishing"
        val left = st.remainingEstMin.toInt()
        ui.status("Test ${minOf(st.cursor + 1, st.steps.size)} of ${st.steps.size}: $name", "$what. About $left min left. Leave the tablet plugged in and the screen on; Kestrel needs to stay in front.", st.cursor.toDouble(), st.steps.size.toDouble())
    }

    /** Battery, heat and Battery Saver: a reason to wait is a pause, never a stop. */
    private fun pauseReason(): String? {
        try {
            val bm = ctx.getSystemService(BatteryManager::class.java)
            val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            val charging = ctx.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)?.let { it != 0 } ?: false
            if (!charging && pct < 20) return "the battery is at $pct % and not charging: plug the tablet in and continue"
            val pm = ctx.getSystemService(PowerManager::class.java)
            if (pm != null && pm.isPowerSaveMode) return "Battery Saver is on and slows the tablet: turn it off and continue"
            if (Build.VERSION.SDK_INT >= 29 && pm != null && pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) return "the tablet is hot: let it cool and continue"
        } catch (e: Exception) { /* no reading: carry on */ }
        return null
    }

    // ---------------------------------------------------------------------------------------------------- ask the owner

    private fun ask(a: Action.Ask) {
        currentPage = "text"; seq++
        session?.loadUri(server.url("text", seq))
        Io.main.postDelayed({ ui.ask(a.text, "Everything looks right", "Something is wrong") { ok -> val st = state ?: return@ask; state = machine.onAnswer(st, a.experimentId, ok); persist(); Health.log("opt", "answer", "${a.experimentId}=$ok"); loop() } }, 2500)
    }

    // ---------------------------------------------------------------------------------------------------- the benchmark suite

    private fun fastPages() = state?.runId?.startsWith("q-") == true

    private val pages = listOf("js", "layout", "anim", "scroll", "input", "compat")
    private val limits = mapOf("js" to 150_000L, "layout" to 90_000L, "anim" to 90_000L, "scroll" to 120_000L, "input" to 90_000L, "compat" to 40_000L)

    private fun measure() {
        val gen = generation
        pageDied = false
        val cpu0 = cpuTicks(); val t0 = SystemClock.elapsedRealtime()
        Thread({
            val metrics = HashMap<String, Double>(); val notes = StringBuilder(); var ok = true
            for (p in pages) {
                if (gen != generation) return@Thread
                val my = synchronized(this) { ++seq }
                Io.main.post { currentPage = p; scrollPulses = 0; session?.loadUri(server.url(p, my, if (fastPages()) "&fast=1" else "")) }
                val r = server.await(p, my, limits.getValue(p))
                if (gen != generation) return@Thread
                when {
                    r == null -> { ok = false; notes.append("$p timed out; ") }
                    r.err.isNotEmpty() -> { ok = false; notes.append("$p: ${r.err}; ") }
                    else -> { metrics.putAll(r.m); if (r.note.isNotEmpty()) notes.append(r.note).append("; ") }
                }
                if (!ok) break
            }
            val cpuMs = ((cpuTicks() - cpu0) * 1000.0 / ticksPerSecond())
            if (ok) { metrics[Metrics.CPU] = cpuMs; if (startMs > 0) metrics[Metrics.START] = startMs; metrics[Metrics.MEM] = pssMb(); metrics["suite_s"] = (SystemClock.elapsedRealtime() - t0) / 1000.0 }
            val sample = Sample(metrics, ok, notes.toString().trim().take(200))
            Io.main.post {
                if (gen != generation) return@post
                val st = state ?: return@post
                state = machine.onSample(st, sample); persist()
                Health.log("opt", "sample", "ok=$ok step=${st.cursor} ${metrics.entries.take(6).joinToString(" ") { it.key + "=" + String.format(Locale.US, "%.1f", it.value) }} ${sample.note}")
                loop()
            }
        }, "bench-suite").apply { isDaemon = true; start() }
    }

    private fun startScroll() {
        scrollPulses = 0
        val pulse = object : Runnable { override fun run() {
            if (currentPage != "scroll" || scrollPulses >= 14) return
            scrollPulses++
            try { view.panZoomController.scrollBy(ScreenLength.zero(), ScreenLength.fromVisualViewportHeight(0.9), PanZoomController.SCROLL_BEHAVIOR_SMOOTH) } catch (e: Exception) { return }
            Io.main.postDelayed(this, 380)
        } }
        Io.main.postDelayed(pulse, 400)
    }

    // ---------------------------------------------------------------------------------------------------- what the app itself used

    private fun appPids(): List<Int> {
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return listOf(Process.myPid())
        val pkg = ctx.packageName
        return am.runningAppProcesses.orEmpty().filter { (it.processName == pkg || it.processName.startsWith("$pkg:")) && !it.processName.endsWith(":restart") }.map { it.pid }
    }
    private fun ticksPerSecond(): Long = try { Os.sysconf(OsConstants._SC_CLK_TCK) } catch (e: Exception) { 100L }
    private fun cpuTicks(): Long {
        var sum = 0L
        for (pid in appPids()) try {
            val f = File("/proc/$pid/stat").readText(); val rest = f.substring(f.lastIndexOf(')') + 2).split(' ')
            sum += (rest.getOrNull(11)?.toLongOrNull() ?: 0L) + (rest.getOrNull(12)?.toLongOrNull() ?: 0L)
        } catch (e: Exception) { /* a process that just ended */ }
        return sum
    }
    private fun pssMb(): Double {
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return 0.0
        return try { am.getProcessMemoryInfo(appPids().toIntArray()).sumOf { it.totalPss } / 1024.0 } catch (e: Exception) { 0.0 }
    }

    // ---------------------------------------------------------------------------------------------------- the end

    private fun finish(status: Status) {
        val st = state ?: return
        val finalCfg = if (status == Status.DONE) (st.finalConfig ?: st.base) else st.base
        OptimizerApply.write(prefs, finalCfg); applyLive()
        val json = OptReport.json(st, deviceFacts(), mapOf("answers" to st.answers.entries.joinToString(",") { it.key + "=" + it.value }))
        OptimizerStore.archive(ctx, st, json)
        OptimizerStore.clearActive(ctx)
        Health.log("opt", "finish", "status=$status final=${finalCfg.canonical().take(160)}")
        val restartNeeded = finalCfg.enginePart() != (st.running?.enginePart() ?: "")
        state = null
        upload(json, force = false)
        ui.report(OptReport.words(st), canUndo = finalCfg != st.base, restartNeeded = restartNeeded)
    }

    private fun upload(json: String, force: Boolean) {
        if (!LiveDiag.isEnabled() || !LiveDiag.hasToken()) { if (force) ui.say("Diagnostics sending is off or has no token (Settings > Diagnostics): the results stay on this tablet."); return }
        val run = OptimizerStore.last(ctx)?.runId ?: "run"
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val time = SimpleDateFormat("HHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        LiveDiag.sendFile("optimizer/$day/$time-$run.json", json) { msg -> Io.main.post { ui.say(msg) } }
    }

    private fun deviceFacts(): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        val am = ctx.getSystemService(ActivityManager::class.java); val mi = ActivityManager.MemoryInfo(); am?.getMemoryInfo(mi)
        m["model"] = Build.MODEL ?: "?"; m["sdk"] = Build.VERSION.SDK_INT.toString(); m["cores"] = Runtime.getRuntime().availableProcessors().toString()
        m["ram_mb"] = (mi.totalMem / 1048576).toString(); m["avail_mb"] = (mi.availMem / 1048576).toString(); m["lowram"] = (am?.isLowRamDevice == true).toString()
        try { val pm = ctx.getSystemService(PowerManager::class.java); m["power_save"] = (pm?.isPowerSaveMode == true).toString(); if (Build.VERSION.SDK_INT >= 29) m["thermal"] = (pm?.currentThermalStatus ?: -1).toString() } catch (e: Exception) { /* none */ }
        try { val b = ctx.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)); m["battery_pct"] = (b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1).toString(); m["plugged"] = ((b?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0).toString() } catch (e: Exception) { /* none */ }
        return m
    }

    private fun appVersion(): String = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
}
