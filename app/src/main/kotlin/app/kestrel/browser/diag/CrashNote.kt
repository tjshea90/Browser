package app.kestrel.browser.diag

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A crash note that SURVIVES the crash (AX2). When the whole app closes, everything the Diagnostics screen knew (the event ring, the counters) dies with it, and "it crashed on this page" is all
 * anyone has. This keeps two small facts on disk so the next Diagnostics can say what happened:
 *  - a Java / Kotlin exception nobody caught (on the UI thread or any other): the exception with its causes, the thread, the version, and the last events of the Health ring, written by the default
 *    uncaught-exception handler (then the previous handler runs as before, so Android still ends the app the way it did);
 *  - the engine's own crash reports that sit in its profile folder (the `.extra` file of a minidump names the process and, for a deliberate abort, the reason).
 * Nothing here leaves the device. The note is plain text, replaced by the next crash and ignored after [KEEP_DAYS] days.
 */
object CrashNote {
    private const val FILE = "last-crash.txt"
    const val KEEP_DAYS = 7
    private const val MAX_CHARS = 14_000
    private const val STACK_LINES = 45

    /** Installs the handler once per process (a second call does nothing). */
    @Synchronized
    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current is Handler) return
        Thread.setDefaultUncaughtExceptionHandler(Handler(app, current))
    }

    private class Handler(private val ctx: Context, private val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            try { write(ctx, t, e) } catch (x: Throwable) { /* never hide the crash behind a failure to describe it */ }
            previous?.uncaughtException(t, e)
        }
    }

    internal fun write(ctx: Context, t: Thread, e: Throwable, now: Long = System.currentTimeMillis()) {
        val sb = StringBuilder(4096)
        sb.append("crash at ").append(stamp(now)).append(" on thread \"").append(t.name).append("\"\n")
        try {
            @Suppress("DEPRECATION") val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            @Suppress("DEPRECATION") sb.append("app ").append(pi.versionName).append(" (code ").append(pi.versionCode).append("), Android ").append(Build.VERSION.RELEASE).append(" API ").append(Build.VERSION.SDK_INT).append('\n')
        } catch (x: Throwable) { /* ignore */ }
        try {
            val rt = Runtime.getRuntime()
            sb.append("app heap ").append((rt.totalMemory() - rt.freeMemory()) shr 20).append('/').append(rt.maxMemory() shr 20).append(" MB\n")
        } catch (x: Throwable) { /* ignore */ }
        var lines = 0
        var cur: Throwable? = e
        var depth = 0
        while (cur != null && depth < 5 && lines < STACK_LINES) {
            sb.append(if (depth == 0) "" else "Caused by: ").append(cur.javaClass.name).append(": ").append(cur.message?.take(300) ?: "").append('\n'); lines++
            for (el in cur.stackTrace) { if (lines >= STACK_LINES) break; sb.append("  at ").append(el.toString()).append('\n'); lines++ }
            cur = cur.cause; depth++
        }
        sb.append("last events:\n")
        for (l in Health.ring.last(30)) sb.append("  ").append(l.take(200)).append('\n')
        File(ctx.filesDir, FILE).writeText(sb.toString().take(MAX_CHARS))
    }

    /** The note of an earlier run that ended in a crash, or null (none, or older than [KEEP_DAYS] days). */
    fun previous(ctx: Context, now: Long = System.currentTimeMillis()): String? {
        val f = File(ctx.filesDir, FILE)
        if (!f.isFile || now - f.lastModified() > KEEP_DAYS * 86_400_000L) return null
        return try { f.readText().ifBlank { null } } catch (e: Exception) { null }
    }

    /** Lines for the Diagnostics report: the earlier crash (the first lines of the note) and the engine's crash reports on disk. */
    fun reportLines(ctx: Context, now: Long = System.currentTimeMillis()): List<String> {
        val out = ArrayList<String>()
        val note = previous(ctx, now)
        if (note != null) {
            val ls = note.lines()
            out.add("EARLIER CRASH of this app (it closed itself): " + ls.take(1).joinToString())
            for (l in ls.drop(1).take(14)) out.add("  $l")
        }
        out.addAll(engineLines(ctx, now))
        return out
    }

    /** The newest crash reports the ENGINE left in its profile folder: process type, reason, when. Empty when there are none. */
    internal fun engineLines(ctx: Context, now: Long): List<String> {
        val moz = File(ctx.filesDir, "mozilla")
        val extras = ArrayList<File>()
        try {
            for (p in moz.listFiles() ?: emptyArray()) {
                if (!p.isDirectory) continue
                val dirs = if (p.name == "Crash Reports") (p.listFiles() ?: emptyArray()).toList() else listOf(File(p, "minidumps"))
                for (d in dirs) for (f in d.listFiles { x -> x.isFile && x.name.endsWith(".extra") } ?: emptyArray()) extras.add(f)
            }
        } catch (e: Exception) { return emptyList() }
        if (extras.isEmpty()) return emptyList()
        extras.sortByDescending { it.lastModified() }
        val out = ArrayList<String>()
        out.add("Engine crash reports on disk: ${extras.size} (newest first)")
        for (f in extras.take(3)) out.add("  " + stamp(f.lastModified()) + " " + summary(f))
        return out
    }

    private fun summary(f: File): String {
        return try {
            val text = f.readText().take(60_000)
            val wanted = listOf("ProcessType", "MozCrashReason", "CrashTime", "OOMAllocationSize", "AvailablePhysicalMemory", "AvailableVirtualMemory", "Notes")
            val parts = ArrayList<String>()
            try {
                val j = org.json.JSONObject(text)
                for (k in wanted) if (j.has(k)) parts.add(k + "=" + j.opt(k).toString().take(120))
            } catch (e: Exception) { // the older key=value form
                for (l in text.lines()) { val i = l.indexOf('='); if (i > 0 && l.substring(0, i) in wanted) parts.add(l.take(140)) }
            }
            if (parts.isEmpty()) "(no readable details in ${f.name})" else parts.joinToString("; ")
        } catch (e: Exception) { "(unreadable ${f.name})" }
    }

    private fun stamp(ms: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))
}
