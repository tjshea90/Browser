package app.kestrel.browser.diag

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.FrameMetrics
import android.view.Window
import app.kestrel.core.health.RingLog
import java.io.File
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * The evidence layer for "the screen stops updating while I type" (research/FREEZE.md): a timeline ring buffer, counters, a UI-thread stall watchdog,
 * frame-jank counting and a plain-text report for the Diagnostics screen. No network, no telemetry: the owner copies or shares the text by hand.
 * Everything here is cheap and bounded; the watchdog only runs while the activity is in front.
 */
object Health {
    val ring = RingLog(512)
    private val startedAt = SystemClock.elapsedRealtime()
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    const val WARN_MS = 700L
    const val STALL_MS = 2000L

    fun log(layer: String, event: String, detail: String = "") {
        val ms = SystemClock.elapsedRealtime() - startedAt // "+12.345s ...": built by hand, String.format costs about 50-100 us on this CPU and this runs for every touch that may start a pull
        val sb = StringBuilder(48 + detail.length).append('+').append(ms / 1000).append('.')
        val frac = (ms % 1000).toInt(); if (frac < 100) sb.append('0'); if (frac < 10) sb.append('0'); sb.append(frac)
        sb.append("s ").append(layer).append(' ').append(event)
        if (detail.isNotEmpty()) sb.append(' ').append(detail)
        val line = sb.toString()
        ring.add(line)
        if (mirror) android.util.Log.d("KestrelHealth", line)   // DEBUG builds only (the emulator test rig reads `adb logcat -s KestrelHealth`: no menu taps on a slow screen)
    }

    /** Set once at start-up from the debuggable flag: a release build never mirrors the event list to the system log. */
    @Volatile var mirror = false

    fun count(name: String): Int = counters.getOrPut(name) { AtomicInteger() }.incrementAndGet()
    /** The current value of a counter (0 if it was never counted). */
    fun counter(name: String): Int = counters[name]?.get() ?: 0

    // ------------------------------------------------------------------ UI-thread watchdog (L0)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var watching = false
    @Volatile private var ackAt = 0L
    private var dog: Thread? = null

    /** A daemon thread posts a token to the main Looper every ~500 ms and measures how long the main thread takes to run it. */
    @Synchronized
    fun startWatchdog() {
        if (dog != null) return
        watching = true
        dog = Thread({
            var postedAt = 0L; var warned = false; var stacked = false
            while (watching) {
                val now = SystemClock.uptimeMillis()
                if (postedAt == 0L) { postedAt = now; ackAt = 0L; main.post { ackAt = SystemClock.uptimeMillis() } }
                try { Thread.sleep(250) } catch (e: InterruptedException) { break }
                val n = SystemClock.uptimeMillis()
                if (ackAt != 0L) {
                    val lag = ackAt - postedAt
                    if (lag >= WARN_MS) { log("ui", "late-beat", "${lag}ms"); count("ui.late") }
                    postedAt = 0L; warned = false; stacked = false
                    try { Thread.sleep(250) } catch (e: InterruptedException) { break }
                } else {
                    val waiting = n - postedAt
                    if (waiting >= STALL_MS && !stacked) {
                        stacked = true; count("ui.stall")
                        log("ui", "STALL", "${waiting}ms: the UI thread is blocked at")
                        try { Looper.getMainLooper().thread.stackTrace.take(12).forEach { log("ui", "  at", it.toString()) } } catch (e: Exception) { /* best effort */ }
                    } else if (waiting >= WARN_MS && !warned) { warned = true; log("ui", "slow", "${waiting}ms") }
                }
            }
        }, "kestrel-watchdog").apply { isDaemon = true; priority = Thread.MIN_PRIORITY; start() }
        log("health", "watchdog-start")
    }

    @Synchronized
    fun stopWatchdog() {
        watching = false
        dog?.interrupt(); dog = null
        log("health", "watchdog-stop")
    }

    // ------------------------------------------------------------------ frame jank (UI thread late for vsync / slow frames)
    private var frameThread: HandlerThread? = null
    private var frameListener: Window.OnFrameMetricsAvailableListener? = null

    @Synchronized
    fun watchFrames(window: Window) {
        if (frameListener != null) return
        val ht = HandlerThread("kestrel-frames").also { it.start() }
        frameThread = ht
        var frames = 0; var jank = 0; var worst = 0L; var since = SystemClock.elapsedRealtime()
        val l = Window.OnFrameMetricsAvailableListener { _, fm, _ ->
            val ms = fm.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000L
            frames++; if (ms > 32) jank++; if (ms > worst) worst = ms
            val now = SystemClock.elapsedRealtime()
            if (now - since >= 10_000) {
                if (jank > 0) { log("frames", "10s", "frames=$frames jank=$jank worst=${worst}ms"); counters.getOrPut("frames.jank") { AtomicInteger() }.addAndGet(jank) }
                frames = 0; jank = 0; worst = 0; since = now
            }
        }
        frameListener = l
        try { window.addOnFrameMetricsAvailableListener(l, Handler(ht.looper)) } catch (e: Exception) { log("health", "frame-metrics-unavailable", e.toString()) }
    }

    @Synchronized
    fun unwatchFrames(window: Window) {
        frameListener?.let { try { window.removeOnFrameMetricsAvailableListener(it) } catch (e: Exception) { /* window gone */ } }
        frameListener = null
        frameThread?.quitSafely(); frameThread = null
    }

    // ------------------------------------------------------------------ the report
    private fun meminfo(key: String): String? =
        try { File("/proc/meminfo").useLines { ls -> ls.firstOrNull { it.startsWith("$key:") }?.substringAfter(':')?.trim() } } catch (e: Exception) { null }

    /** `MemAvailable` from /proc/meminfo in MB (what the kernel says a new allocation can get without swapping: free memory plus the page cache that can be given back), or -1 when it cannot be read. About 0.2 ms. */
    fun memAvailableMb(): Int =
        try { (meminfo("MemAvailable")?.substringBefore(' ')?.trim()?.toLongOrNull()?.let { it / 1024 } ?: -1L).toInt() } catch (e: Exception) { -1 }

    /** Plain-text report for the Diagnostics screen. [engineFacts] are lines only the engine module knows (version, sessions, ...). */
    fun report(ctx: Context, engineFacts: List<String> = emptyList(), lastEvents: Int = 150): String {
        val sb = StringBuilder()
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        sb.append("Kestrel diagnostics  ").append(fmt.format(java.util.Date())).append("  (app running ").append((SystemClock.elapsedRealtime() - startedAt) / 1000).append(" s)\n")
        try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            @Suppress("DEPRECATION") sb.append("App: ").append(ctx.packageName).append(' ').append(pi.versionName).append(" (code ").append(pi.versionCode).append(")\n")
        } catch (e: Exception) { /* ignore */ }
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(", Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append("), ").append(Build.SUPPORTED_ABIS.joinToString("/")).append('\n')
        val dm = ctx.resources.displayMetrics
        sb.append("Display: ").append(dm.widthPixels).append('x').append(dm.heightPixels).append(" px, ").append(dm.densityDpi).append(" dpi, font scale ").append(ctx.resources.configuration.fontScale).append('\n')
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val rt = Runtime.getRuntime()
        sb.append("Memory: total ").append(mi.totalMem shr 20).append(" MB, available ").append(mi.availMem shr 20).append(" MB, low=").append(mi.lowMemory)
            .append(", threshold ").append(mi.threshold shr 20).append(" MB; MemAvailable ").append(meminfo("MemAvailable") ?: "?").append("; low-RAM device=").append(am.isLowRamDevice)
            .append("; app heap ").append((rt.totalMemory() - rt.freeMemory()) shr 20).append('/').append(rt.maxMemory() shr 20).append(" MB\n")
        try { sb.append("Keyboard: ").append(Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: "?").append('\n') } catch (e: Exception) { /* ignore */ }
        try {
            val procs = am.runningAppProcesses?.filter { it.processName.startsWith(ctx.packageName) }?.joinToString(", ") { it.processName.removePrefix(ctx.packageName).ifEmpty { "main" } + "#" + it.pid }
            sb.append("Processes: ").append(procs ?: "?").append('\n')
        } catch (e: Exception) { /* ignore */ }
        try { CrashNote.reportLines(ctx).forEach { sb.append(it).append('\n') } } catch (e: Exception) { /* a report must never fail */ } // AX2: what the LAST run left behind if it crashed
        engineFacts.forEach { sb.append(it).append('\n') }
        PageTimes.lines().forEach { sb.append(it).append('\n') }
        try { app.kestrel.browser.download.PhoneBridge.diagLines(ctx).forEach { sb.append(it).append('\n') } } catch (e: Exception) { /* a report must never fail */ }
        val c = counters.entries.sortedBy { it.key }.joinToString("  ") { "${it.key}=${it.value.get()}" }
        sb.append("Counters: ").append(if (c.isEmpty()) "(none yet)" else c).append('\n')
        sb.append("\n-- last ").append(lastEvents).append(" events (").append(ring.totalAdded()).append(" recorded) --\n")
        ring.last(lastEvents).forEach { sb.append(it).append('\n') }
        return sb.toString()
    }
}
