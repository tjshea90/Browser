package app.kestrel.browser

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.diag.Health
import app.kestrel.browser.mem.TrimGateContext
import app.kestrel.core.mem.TrimGate

/**
 * PERF20 (research/RAM_FOR_SPEED.md): Android's memory signals on their way to the page engine. The engine drops its warm caches and compiled script code for every critical signal; this tablet sends them with 340 to
 * 800 MB free (core TrimGate has the numbers and the rule). Each signal the engine would react to is judged here: passed on (and counted `mem.trim.sent`) or held (`mem.trim.held`), with the reason in one Health line.
 * Nothing about a page or a tab is read. Only the UI thread calls [forward].
 */
internal object EngineTrimGate : TrimGateContext.Gate {
    private var appCtx: Context? = null
    private val heldAt = ArrayDeque<Long>()          // uptime of the signals held in the last half hour
    @Volatile var lastLine = "no memory signal since the start"
    @Volatile var context: TrimGateContext? = null  // the context the engine was created with (its `registered` name proves the gate is in place)

    fun install(ctx: Context): TrimGateContext {
        appCtx = ctx.applicationContext
        return TrimGateContext(ctx, this).also { context = it }
    }

    override fun forward(level: Int): Boolean {
        val ctx = appCtx ?: return true
        try {
            val now = SystemClock.uptimeMillis()
            while (heldAt.isNotEmpty() && now - heldAt.first() > TrimGate.SAFETY_WINDOW_MS) heldAt.removeFirst()
            val mi = ActivityManager.MemoryInfo()
            (ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(mi) ?: return true
            val avail = mi.availMem shr 20
            val prefs = Prefs.get(ctx)
            val killAt = prefs.lastEngineKillAt
            val since = if (killAt <= 0L) Long.MAX_VALUE else (System.currentTimeMillis() - killAt).coerceAtLeast(0L)
            val v = TrimGate.decide(prefs.holdFalseTrims, level, avail, mi.lowMemory, since, heldAt.size)
            if (TrimGate.engineReacts(level)) {
                if (v.forward) Health.count("mem.trim.sent") else { heldAt.addLast(now); Health.count("mem.trim.held") }
                val word = if (v.forward) "trim-sent" else "trim-held"
                lastLine = "level $level, ${avail} MB available: " + (if (v.forward) "passed on to the engine" else "held") + " (" + v.why + ")"
                Health.log("mem", word, "level=$level avail=${avail}MB why=${v.why}")
            }
            return v.forward
        } catch (e: Throwable) {
            return true          // anything unexpected: the engine hears everything, as before
        }
    }

    /** The Diagnostics line. */
    fun line(prefs: Prefs): String =
        "Memory signals to the engine (PERF20): " + (if (prefs.holdFalseTrims) "holding false alarms (350 MB or more available, no engine kill in 6 h)" else "off: every signal passed on") +
            "; gate registered on: " + (context?.registered ?: "not created") + "; held ${Health.counter("mem.trim.held")}, passed on ${Health.counter("mem.trim.sent")}; last: $lastLine"
}
