package app.kestrel.core.mem

/**
 * PERF20 (owner 2026-11-24: "Do even more research how the browser can be as fast as possible utilizing as much ram as possible for speed within reason for a low ram tablet"; research/RAM_FOR_SPEED.md).
 *
 * Android tells every app, through onTrimMemory, how the SYSTEM judges its memory. On this Android 10 Go tablet the judgement is not about free memory: the owner's uploads show level 80 ("this process is next to be
 * ended") within a second of every time the app is left and level 15 ("running critically low") four times a day, with 340 to 800 MB reported free (all 33 level-80 signals had at least 340 MB; 37 of the 53 level-15
 * signals had at least 300 MB). The engine takes both at face value: its memory-pressure handler (GeckoView `MemoryController`: level 15 and 40 or more, plus onLowMemory) makes it drop its decoded pictures, texture
 * and memory caches, its frozen back/forward pages and, through a shrinking garbage collection, the compiled script code that makes a heavy page fast. That is a speed loss paid for a false alarm.
 *
 * This rule decides, for each signal, whether the engine hears it. A signal is HELD only when the evidence says the memory is not short; everything else is passed on exactly as before. Pure and tested; the Android part
 * only reads the numbers and calls it.
 */
object TrimGate {
    /** A critical signal with at least this much memory available (MemAvailable, in MB) is a false alarm on this tablet. */
    const val HOLD_MIN_AVAIL_MB = 350L

    /** After an engine kill or crash the system has shown it will end us: everything is passed on for this long. */
    const val AFTER_KILL_MS = 6L * 60 * 60 * 1000

    /** This many held signals within [SAFETY_WINDOW_MS] and the next one is passed on, so a long stretch of use still gets its clean-up. */
    const val SAFETY_HELD = 6
    const val SAFETY_WINDOW_MS = 30L * 60 * 1000

    /** Android's trim levels the engine reacts to (GeckoView MemoryController: 15 = RUNNING_CRITICAL, 40 and up = BACKGROUND, MODERATE, COMPLETE). Levels 5, 10, 20 and 60 are no-ops there or not sent to apps like this. */
    fun engineReacts(level: Int): Boolean = level == 15 || level >= 40

    class Verdict(val forward: Boolean, val why: String)

    /**
     * @param enabled the Settings switch (on by default)
     * @param level Android's trim level
     * @param availMb memory available to the system now (ActivityManager.MemoryInfo.availMem, in MB; -1 unknown)
     * @param systemLow ActivityManager.MemoryInfo.lowMemory
     * @param sinceKillMs milliseconds since the engine was last killed or crashed (Long.MAX_VALUE: never)
     * @param heldRecently how many signals were held in the last [SAFETY_WINDOW_MS]
     */
    fun decide(enabled: Boolean, level: Int, availMb: Long, systemLow: Boolean, sinceKillMs: Long, heldRecently: Int): Verdict = when {
        !enabled -> Verdict(true, "switch off")
        !engineReacts(level) -> Verdict(true, "the engine ignores level $level")
        systemLow -> Verdict(true, "the system says low memory")
        availMb < 0L -> Verdict(true, "free memory unknown")
        availMb < HOLD_MIN_AVAIL_MB -> Verdict(true, "only $availMb MB available")
        sinceKillMs in 0 until AFTER_KILL_MS -> Verdict(true, "the engine was ended " + (sinceKillMs / 60_000) + " min ago")
        heldRecently >= SAFETY_HELD -> Verdict(true, "$heldRecently signals held in 30 min: clean up now")
        else -> Verdict(false, "$availMb MB available: a false alarm")
    }
}
