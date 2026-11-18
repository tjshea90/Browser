package app.kestrel.browser.diag

import android.content.Context
import android.os.SystemClock
import app.kestrel.browser.Io
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * CR1 (owner message 2026-11-18: "The app crashed when i went to youtube ... i'm just concerned that it crashed in the first place. Investigate."): how did the LAST run end?
 *
 * Android 10 has no exit-reason API, [CrashNote] only sees an uncaught Java exception and the engine's own crash reports only see a native crash inside the engine, so an app that
 * Android ended for memory (or a main-process crash that left no report) left NOTHING: everything in the Health ring died with the process. This keeps a few lines on disk, written only
 * when something notable happens (the screen comes or goes, a page finished loading, Android asked for memory, the engine's page process died): no timer, no polling, a file of about 2 KB
 * replaced through the IO thread. The next start reads it and Diagnostics says whether the last run ended in front of the person (not an ordinary close), in the background (Android
 * took it back: ordinary) or on purpose, with the facts that sort a crash from a kill: the crash note, the engine's reports, the last memory figures, a restart of the whole tablet.
 * Nothing leaves the device; a private tab writes none of these lines (the page lines come from [PageTimes], which never starts for a private tab).
 */
object LastRun {
    private const val FILE = "last-run.txt"
    private const val ENDED = "last-run-ended.txt"
    private val ENDED_FILES = arrayOf("last-run-ended.txt", "last-run-ended-2.txt", "last-run-ended-3.txt")   // CR1f: the three newest runs that did not end quietly (a second abnormal end used to overwrite the first)
    private const val HEADER = "kestrel-last-run 1"
    const val KEEP_DAYS = 7
    private const val MAX_EVENTS = 40
    private const val LAZY_MS = 1500L
    private const val MAX_CHARS = 6_000

    private val lock = Any()
    /** CR1f (owner 2026-11-18, doraby: the note stopped 1.3 s after the start and could not say crash or freeze): the lazy flush used to wait on the MAIN looper, so a frozen screen thread stopped the note too. It has its own daemon thread now. */
    private val timer = java.util.concurrent.ScheduledThreadPoolExecutor(1) { r -> Thread(r, "kestrel-lastrun").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }
        .apply { setKeepAliveTime(5, java.util.concurrent.TimeUnit.SECONDS); allowCoreThreadTimeOut(true) }
    @Volatile internal var lazyMs = LAZY_MS
    private var dir: File? = null
    private var pid = 0
    private var build = ""
    private var startedAt = 0L
    private var state = "starting"
    private var stateAt = 0L
    private val events = ArrayDeque<String>()
    private var flushQueued = false
    private var previous: String? = null
    private var previousAbnormal = false

    /** The Health lines worth keeping on disk; everything else (touches, frames, scrolling) stays in the RAM ring. */
    fun wants(layer: String, event: String): Boolean = when (layer) {
        "life" -> true
        "mem" -> event == "trim"
        "page" -> event == "loaded" || event == "error"
        "gecko" -> event == "onKill" || event == "onCrash" || event.startsWith("recover-")
        "ui" -> event == "STALL" || event.trim() == "at"                       // CR1f: the screen thread is blocked, and the stack it is blocked in (written by the watchdog thread, not by the blocked one)
        "start" -> event == "paint" || event == "runtime"                      // how far the start got
        "tabs" -> event == "early-cover"
        "engine" -> event == "warm-start" || event == "runtime-ready"
        else -> false
    }

    /** Called by [Health.log] for the lines [wants] accepts. Cheap: a lock and a queue; the file is written later on the IO thread. */
    fun note(layer: String, event: String, line: String, now: Long = System.currentTimeMillis()) {
        var urgent = false
        synchronized(lock) {
            if (dir == null) return
            if (layer == "life") {
                when (event) {
                    "resume" -> { state = "fg"; stateAt = now; urgent = true }
                    "pause", "stop" -> { state = "bg"; stateAt = now; urgent = true }
                }
            }
            if (layer == "gecko" || (layer == "ui" && event == "STALL")) urgent = true
            if (events.size >= MAX_EVENTS) events.removeFirst()
            events.addLast(line.take(220))
            if (!urgent && flushQueued) return
            if (!urgent) flushQueued = true
        }
        if (urgent) Io.exec { flush() }
        else try { timer.schedule({ Io.exec { flush() } }, lazyMs, java.util.concurrent.TimeUnit.MILLISECONDS) } catch (e: Exception) { Io.exec { flush() } }
    }

    /**
     * First thing in the UI process (Application.onCreate): the file of the run that just ended is read on the IO thread, and kept for [KEEP_DAYS] days when it did not end quietly
     * (the next start would overwrite it), then this run's own file is started. The crash facts are decided HERE because only now is the window of the earlier run known.
     */
    fun begin(ctx: Context, now: Long = System.currentTimeMillis()) {
        val app = ctx.applicationContext
        val d = app.filesDir
        synchronized(lock) {
            dir = d; pid = android.os.Process.myPid(); startedAt = now; state = "starting"; stateAt = now; events.clear(); flushQueued = false
            build = try {
                @Suppress("DEPRECATION") val pi = app.packageManager.getPackageInfo(app.packageName, 0)
                @Suppress("DEPRECATION") "${pi.versionName} (code ${pi.versionCode})"
            } catch (e: Exception) { "?" }
        }
        Io.exec {
            try {
                val old = File(d, FILE)
                if (old.isFile) {
                    val bootAt = now - SystemClock.elapsedRealtime()
                    val text = old.readText().take(MAX_CHARS) + "\nnextStart=$now\nbootAt=$bootAt\ncrashAt=${CrashNote.noteTime(app)}\nengineAt=${CrashNote.newestEngineReportAt(app)}\n"
                    val abnormal = read(text).abnormal
                    synchronized(lock) { previous = text; previousAbnormal = abnormal }
                    if (abnormal) keep(d, text)
                }
            } catch (e: Exception) { /* the report is a help, never a risk */ }
            flush()
        }
    }

    /** CR1f: the newest abnormal run goes first; the two before it move down; the oldest falls off. */
    private fun keep(d: File, text: String) {
        try {
            File(d, ENDED_FILES[2]).delete()
            File(d, ENDED_FILES[1]).renameTo(File(d, ENDED_FILES[2]))
            File(d, ENDED_FILES[0]).renameTo(File(d, ENDED_FILES[1]))
            File(d, ENDED_FILES[0]).writeText(text)
        } catch (e: Exception) { /* the report is a help, never a risk */ }
    }

    /** The process ends on purpose (nothing left to do, [app.kestrel.browser.ProcessLife]): written at once, on the caller's thread, because the next line kills the process. */
    fun markExit(now: Long = System.currentTimeMillis()) {
        synchronized(lock) { if (dir == null) return; state = "exit"; stateAt = now }
        flush(now)
    }

    private fun flush(now: Long = System.currentTimeMillis()) {
        val avail = try { Health.memAvailableMb() } catch (e: Exception) { -1 }   // read before the lock is taken: it opens /proc/meminfo
        val text: String
        val d: File
        synchronized(lock) {
            d = dir ?: return
            flushQueued = false
            val sb = StringBuilder(2048)
            sb.append(HEADER).append('\n').append("pid=").append(pid).append('\n').append("build=").append(build).append('\n')
                .append("started=").append(startedAt).append('\n').append("state=").append(state).append('\n').append("stateAt=").append(stateAt).append('\n').append("saved=").append(now).append('\n')
            sb.append("availMb=").append(avail).append('\n')
            for (e in events) sb.append("ev=").append(e).append('\n')
            text = sb.toString()
        }
        try {
            val tmp = File(d, "$FILE.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(File(d, FILE))) File(d, FILE).writeText(text)
        } catch (e: Exception) { /* a full disk must not hurt the browser */ }
    }

    /** What a stored file says. [abnormal] = the app was in front of the person when it ended and did not close on purpose. */
    internal class Ended(val lines: List<String>, val abnormal: Boolean)

    private fun kv(text: String, key: String): String? { val p = "$key="; for (l in text.lineSequence()) if (l.startsWith(p)) return l.substring(p.length); return null }
    private fun kvLong(text: String, key: String): Long = kv(text, key)?.toLongOrNull() ?: 0L

    internal fun read(text: String): Ended {
        if (!text.startsWith(HEADER)) return Ended(listOf("Last run: no readable note"), false)
        val st = kv(text, "state") ?: "?"
        val started = kvLong(text, "started"); val stateAt = kvLong(text, "stateAt"); val saved = kvLong(text, "saved")
        val next = kvLong(text, "nextStart"); val bootAt = kvLong(text, "bootAt"); val crashAt = kvLong(text, "crashAt"); val engineAt = kvLong(text, "engineAt")
        val avail = kvLong(text, "availMb")
        val last = maxOf(saved, stateAt)
        val ranMin = if (last > started && started > 0) (last - started) / 60_000 else 0
        val out = ArrayList<String>()
        val abnormal = st == "fg"
        val head = when (st) {
            "exit" -> "Last run ended on purpose (the app was closed with nothing left to do)."
            "bg" -> "Last run was in the background when it ended (Android takes back background apps when it needs the memory: an ordinary end, not a crash)."
            "starting" -> "Last run never reached the screen (a background job such as the daily check, or it ended while starting)."
            else -> "Last run ended WHILE ON SCREEN: that is not an ordinary close."
        }
        out.add(head)
        out.add("  build ${kv(text, "build") ?: "?"}, started ${stamp(started)}, last note ${stamp(last)} (ran about $ranMin min), memory free at the last note: ${if (avail >= 0) "$avail MB" else "?"}")
        if (abnormal) {
            val crashed = crashAt in started..(if (next > 0) next else Long.MAX_VALUE) && crashAt > 0
            val engine = engineAt in started..(if (next > 0) next else Long.MAX_VALUE) && engineAt > 0
            val rebooted = bootAt > 0 && saved > 0 && bootAt > saved + 5_000   // a tablet that restarted came back at least some seconds after the last note
            out.add("  " + when {
                crashed -> "Cause: an uncaught Java exception (the crash note is shown below as EARLIER CRASH)."
                rebooted -> "Cause: the whole tablet restarted after the last note (empty battery or a restart), not the app."
                engine -> "Cause: probably the engine: it wrote crash report(s) during this run (see Engine crash reports below; a page process crash alone does not close the app, a main process one does)."
                else -> "Cause: NO crash note and NO engine crash report, so the process was most likely ended from outside: Android's memory killer (the tablet has 2 GB), or Force stop."
            })
        }
        val ev = text.lineSequence().filter { it.startsWith("ev=") }.map { it.substring(3) }.toList()
        if (ev.isNotEmpty()) { out.add("  last events (newest last):"); for (e in ev.takeLast(12)) out.add("    $e") }
        return Ended(out, abnormal)
    }

    /** Lines for the Diagnostics report: the earlier run that did not end quietly (kept [KEEP_DAYS] days), or one line about the run before this one. */
    fun reportLines(ctx: Context, now: Long = System.currentTimeMillis()): List<String> {
        val out = ArrayList<String>()
        val prev: String?
        val prevAbnormal: Boolean
        synchronized(lock) { prev = previous; prevAbnormal = previousAbnormal }
        val keptFile = File(ctx.filesDir, ENDED)
        val kept = try { if (keptFile.isFile && now - keptFile.lastModified() <= KEEP_DAYS * 86_400_000L) keptFile.readText() else null } catch (e: Exception) { null }
        if (prev != null && prevAbnormal) out.addAll(read(prev).lines)
        else {
            if (prev != null) out.add(read(prev).lines.first())
            if (kept != null) { out.add("An EARLIER run (before the one above) did not end quietly - kept $KEEP_DAYS days:"); out.addAll(read(kept).lines.map { "  $it" }) }
        }
        if (prev == null && kept == null) out.add("Last run: no earlier note (first start since this version, or the files were cleared).")
        return out
    }

    private fun stamp(ms: Long): String = if (ms <= 0) "?" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

    /** Test hook: forget this process's state so a test can start a fresh "run". */
    internal fun resetForTest() { synchronized(lock) { dir = null; previous = null; previousAbnormal = false; events.clear(); flushQueued = false; state = "starting" } }
}
