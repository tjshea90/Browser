package app.kestrel.browser.diag

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import app.kestrel.browser.Io
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ND1e (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"; research/DIAG_2026-11-27.md section 4): how much memory EACH process of the app holds. The page process (where
 * claude.ai lives) was never measured on the tablet: Android 10 hides other users' processes from `/proc`, so the app's own readings (`rssSumMb` equals the main process, `cpu...` has only `parent`) saw only
 * itself, and every question about the engine being killed or the tablet running out of memory was answered without the biggest number. The system's own memory service (ActivityManager.getProcessMemoryInfo)
 * may be asked about the app's own processes, isolated ones included.
 *
 * Every [EVERY_MS] while the browser is on screen, one such call on the IO lane (about 10 to 40 ms; nothing at all while the app is in the background). PSS = the process's share of memory as Android counts it
 * (shared pages divided between the processes that use them). The last reading and the peak of this run are kept per process kind (`parent`, `tab` = the page process, `gpu`, `crashhelper`) and go into the
 * live-diagnostics device numbers (`pssParentMb`, `pssTabMb`, `pssTabPeakMb`, ..., `pssSumMb`) and the Diagnostics line.
 */
object ProcMem {
    private const val EVERY_MS = 60_000L
    private const val FIRST_MS = 6_000L

    private val last = ConcurrentHashMap<String, Int>()
    private val peak = ConcurrentHashMap<String, Int>()
    @Volatile private var sumLast = 0
    @Volatile private var sumPeak = 0
    @Volatile private var readAt = 0L
    @Volatile private var running = false
    @Volatile private var ctx: Context? = null
    private val queued = AtomicBoolean(false)

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (queued.compareAndSet(false, true)) Io.exec { try { sample() } catch (e: Exception) { /* a meter never breaks the browser */ } finally { queued.set(false) } }
            Io.main.postDelayed(this, EVERY_MS)
        }
    }

    fun start(c: Context) {
        ctx = c.applicationContext
        if (running) return
        running = true
        Io.main.postDelayed(tick, FIRST_MS)
    }

    fun stop() { running = false; Io.main.removeCallbacks(tick) }

    private fun sample() {
        val c = ctx ?: return
        val am = c.getSystemService(ActivityManager::class.java) ?: return
        val pkg = c.packageName
        val procs = am.runningAppProcesses.orEmpty().filter { it.processName == pkg || it.processName.startsWith("$pkg:") }
        if (procs.isEmpty()) return
        val infos = am.getProcessMemoryInfo(procs.map { it.pid }.toIntArray())
        var sum = 0
        for (i in procs.indices) {
            val mb = infos.getOrNull(i)?.totalPss?.div(1024) ?: continue
            val n = kind(procs[i].processName)
            last[n] = mb
            peak.merge(n, mb) { a, b -> if (a > b) a else b }
            sum += mb
        }
        sumLast = sum; if (sum > sumPeak) sumPeak = sum
        readAt = SystemClock.elapsedRealtime()
        last.keys.retainAll(procs.map { kind(it.processName) }.toSet())          // a process that ended is no longer "held"
    }

    /** `app.kestrel.browser.gecko:sandboxed_tab_disable_art_image_39` -> `tab`, the main process -> `parent`, `...:gpu_...` -> `gpu`, `...:crashhelper_...` -> `crashhelper`. */
    internal fun kind(processName: String): String = CpuMeter.short(processName)

    /** The device numbers for the live diagnostics: `pss<Kind>Mb` (last reading), `pss<Kind>PeakMb` (largest this run) and the sums. Empty before the first reading. */
    fun numbers(): Map<String, Long> {
        if (readAt == 0L) return emptyMap()
        val out = LinkedHashMap<String, Long>()
        for ((k, v) in last) {
            val n = k.filter { it.isLetterOrDigit() }.replaceFirstChar { it.uppercase() }.take(12)
            if (n.isEmpty()) continue
            out["pss${n}Mb"] = v.toLong(); out["pss${n}PeakMb"] = (peak[k] ?: v).toLong()
        }
        out["pssSumMb"] = sumLast.toLong(); out["pssSumPeakMb"] = sumPeak.toLong()
        return out
    }

    /** The Diagnostics line. */
    fun line(): String {
        if (readAt == 0L) return "Memory by process (PSS, one reading a minute while the app is in front): no reading yet"
        val parts = last.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key} ${it.value} MB (peak ${peak[it.key] ?: it.value})" }
        return "Memory by process (PSS, one reading a minute while the app is in front; the page process is 'tab'): $parts; together $sumLast MB (peak $sumPeak MB), read ${(SystemClock.elapsedRealtime() - readAt) / 1000} s ago"
    }
}
