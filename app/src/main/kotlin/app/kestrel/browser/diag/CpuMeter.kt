package app.kestrel.browser.diag

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import app.kestrel.browser.Io
import app.kestrel.core.diag.CpuReport
import app.kestrel.core.diag.CpuSeries
import app.kestrel.core.diag.ProcStat
import java.io.File

/**
 * Which process, and which thread, is working flat out when a page or a video lags (AO1, owner message 29: "the video player and web page animations ... are still slow and laggy").
 *
 * Every [EVERY_MS] while the browser is on screen it reads the CPU time of each of the app's own processes (the parent, the page process `tab`, the graphics process, ...) from `/proc/<pid>/stat`: five tiny
 * file reads, no network, nothing at all while the browser is in the background. A process that used more than [BUSY_PCT] % of a core in a reading is looked at thread by thread for one second, so the
 * Diagnostics line can say "tab 98 %: GeckoMain 71, DOM Worker 20" (the page's own script and layout are the limit) or "MediaPDecoder 60" (a video decoded in software). The last five minutes are kept; the
 * line is built when Diagnostics opens, so the lag is still in it a minute after it happened.
 */
object CpuMeter {
    private const val EVERY_MS = 5_000L
    private const val BUSY_PCT = 70.0
    private const val SNAP_MS = 1_000L
    private const val MIN_GAP_MS = 2_000L
    private const val WINDOW_MS = 300_000L

    private class Proc(val name: String) {
        var ticks = -1L
        var at = 0L
        var snapping = false
        var loggedAt = -1_000_000L
        val series = CpuSeries(60)
    }

    private class Moment(val at: Long, val who: String, val pct: Double, val threads: String)

    private val clk: Int = try { Os.sysconf(OsConstants._SC_CLK_TCK).toInt().coerceAtLeast(1) } catch (e: Throwable) { 100 }
    private val lock = Any()
    private val procs = HashMap<Int, Proc>()
    private val moments = ArrayList<Moment>()
    @Volatile private var running = false
    @Volatile private var pkg = ""
    private val cores = Runtime.getRuntime().availableProcessors()

    private val queued = java.util.concurrent.atomic.AtomicBoolean(false)

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            // one reading waiting at a time: the IO lane is a single low-priority thread, and on a loaded tablet readings that piled up would run back to back, a few milliseconds apart
            if (queued.compareAndSet(false, true)) Io.exec { try { sample() } catch (e: Exception) { /* a meter never breaks the browser */ } finally { queued.set(false) } }
            Io.main.postDelayed(this, EVERY_MS)
        }
    }

    /** Called when the browser comes to the front. */
    fun start(packageName: String) {
        pkg = packageName
        if (running) return
        running = true
        Io.main.postDelayed(tick, EVERY_MS)
    }

    /** Called when it goes to the background: no wake-ups at all. The readings kept stay. */
    fun stop() {
        running = false
        Io.main.removeCallbacks(tick)
        synchronized(lock) { for (p in procs.values) { p.ticks = -1L } } // the gap while away is not a reading
    }

    private fun readText(path: String): String? = try { File(path).readText() } catch (e: Exception) { null }

    private fun nameOf(pid: Int): String? {
        val raw = readText("/proc/$pid/cmdline") ?: return null
        val n = raw.substringBefore('\u0000')
        return if (pkg.isNotEmpty() && n.startsWith(pkg)) n else null
    }

    /** `app.kestrel.browser.gecko:sandboxed_tab_disable_art_image_39` -> `tab`; the main process -> `parent`. */
    internal fun short(name: String): String = name.substringAfter(':', "").let { n ->
        if (n.isEmpty()) "parent" else n.removePrefix("sandboxed_").replace(Regex("_disable_art_image_\\d*$"), "").ifEmpty { n }
    }

    private fun sample() {
        val now = SystemClock.elapsedRealtime()
        val dirs = File("/proc").list() ?: return
        val seen = HashSet<Int>()
        for (d in dirs) {
            val pid = d.toIntOrNull() ?: continue
            var p = synchronized(lock) { procs[pid] }
            if (p == null) { val n = nameOf(pid) ?: continue; p = Proc(n); synchronized(lock) { procs[pid] = p } }
            val t = ProcStat.ticks(readText("/proc/$pid/stat") ?: continue) ?: continue
            seen.add(pid)
            if (p.ticks >= 0 && now - p.at < MIN_GAP_MS) continue // too soon after the last reading: the kernel counts in 10 ms ticks, so a short gap would turn one tick into hundreds of percent
            if (p.ticks >= 0) {
                val pct = minOf(ProcStat.percent(t - p.ticks, now - p.at, clk), cores * 100.0)
                synchronized(lock) { p.series.add(now, pct) }
                if (pct >= BUSY_PCT && !p.snapping) snapshot(pid, p, now, pct)
            }
            p.ticks = t; p.at = now
        }
        synchronized(lock) { procs.keys.retainAll(seen) }
    }

    /** The threads of a process that has been busy: their CPU time now and a second later. */
    private fun snapshot(pid: Int, p: Proc, now: Long, pct: Double) {
        p.snapping = true
        val before = threadTicks(pid)
        val t0 = SystemClock.elapsedRealtime()
        Io.main.postDelayed({
            Io.exec {
                try {
                    val after = threadTicks(pid)
                    val dt = SystemClock.elapsedRealtime() - t0
                    val top = after.mapNotNull { (tid, a) -> val b = before[tid] ?: return@mapNotNull null; a.first to minOf(ProcStat.percent(a.second - b.second, dt, clk), 100.0) }
                        .filter { it.second >= 3.0 }.sortedByDescending { it.second }.take(3)
                        .joinToString(", ") { "${it.first} ${it.second.toInt()}" }
                    val said = top.ifEmpty { "no single thread stood out" }
                    synchronized(lock) {
                        moments.add(Moment(now, short(p.name), pct, said))
                        while (moments.size > 6) moments.removeAt(0)
                    }
                    if (now - p.loggedAt > 30_000) { p.loggedAt = now; Health.log("cpu", "busy", "${short(p.name)} ${pct.toInt()} % [$said]") }
                } catch (e: Exception) { /* gone */ } finally { p.snapping = false }
            }
        }, SNAP_MS)
    }

    /** thread id -> (thread name, ticks) of one process. */
    private fun threadTicks(pid: Int): Map<Int, Pair<String, Long>> {
        val out = HashMap<Int, Pair<String, Long>>()
        for (t in File("/proc/$pid/task").list() ?: return out) {
            val tid = t.toIntOrNull() ?: continue
            val s = readText("/proc/$pid/task/$tid/stat") ?: continue
            val ticks = ProcStat.ticks(s) ?: continue
            out[tid] = (ProcStat.comm(s) ?: "?") to ticks
        }
        return out
    }

    /** The Diagnostics line. */
    fun line(): String {
        val now = SystemClock.elapsedRealtime()
        synchronized(lock) {
            val rows = procs.values.filter { it.series.size > 0 }.sortedByDescending { it.series.mean(now, WINDOW_MS) }
                .map { p -> CpuReport.Row(short(p.name), p.series.mean(now, WINDOW_MS).toInt(), (p.series.peak()?.second ?: 0.0).toInt()) }
            val ms = moments.map { m -> CpuReport.Moment(now - m.at, m.who, m.pct.toInt(), m.threads) }
            return CpuReport.line(cores, (EVERY_MS / 1000).toInt(), BUSY_PCT.toInt(), rows, ms)
        }
    }
}
