package app.kestrel.core.gfx

/**
 * GL2 (owner 2026-11-26: "Today i saw undrawn cut off pictures and text"; research/PAINT_GLITCH.md, section 2026-11-26).
 *
 * The newest uploads showed the tablet drawing with the graphics chip and EVERY engine shortcut at its default (Safe drawing off, Software drawing off, hardware trial off): the mode that cut text and
 * pictures into bands in alpha89-91 and is the only mode of the three that has never been seen clean. Three saved numbers (labSoft, trial, and the old Safe drawing switch) made that mode reachable by
 * accident, and a hardware trial that switched itself off fell back into it. This object is the one place that names the modes and the rules that keep the person out of the failing one.
 * Pure Kotlin so the rules are tested on the JVM; the Android side only reads and writes the saved numbers.
 */
object DrawGuard {
    /** What the engine will do for a pair of saved values. */
    enum class Mode(val label: String) {
        /** The processor draws every page (gfx.webrender.software): slower, and the only mode that has never left a part of a page undrawn on this tablet. */
        PROCESSOR("processor"),
        /** The graphics chip draws, with the Safe drawing switches and the Mali fixes of [DrawTrial] (hardware trial 1). Smooth; not yet proven free of undrawn parts. */
        FIXES("graphics chip with fixes"),
        /** The graphics chip draws with the engine's own defaults (trial off, Software drawing off). The failing mode: only an old backup or an old setting can still land here. */
        PLAIN("plain graphics chip"),
    }

    fun mode(labSoft: Boolean, trial: Int): Mode = when {
        DrawTrial.softwareDrawing(labSoft, trial) -> Mode.PROCESSOR
        trial > DrawTrial.OFF -> Mode.FIXES
        else -> Mode.PLAIN
    }

    /** What a start finds in the plain mode: the trial is turned on (the fixes), so the person never draws with the failing mode. Null = nothing to do. */
    fun leavePlain(labSoft: Boolean, trial: Int): Int? = if (mode(labSoft, trial) == Mode.PLAIN) DrawTrial.FIXES else null

    /** The sentence Settings and the log use when [leavePlain] acted. */
    const val LEFT_PLAIN_NOTE = "the graphics chip was drawing with the engine's own shortcuts, the way that left parts of pages undrawn on this tablet; it now draws with the fixes (hardware drawing trial 1)"

    /** The choice the Settings row shows: 0 = the processor draws, 1 = the graphics chip with the fixes. The plain mode reads as 1 (it is moved there at the next start). */
    fun choiceOf(labSoft: Boolean, trial: Int): Int = if (mode(labSoft, trial) == Mode.PROCESSOR) 0 else 1

    /** The two saved values for a Settings choice: (labSoft, trial). */
    fun valuesOf(choice: Int): Pair<Boolean, Int> = if (choice == 0) true to DrawTrial.OFF else false to DrawTrial.FIXES

    /** A hardware trial that switches itself off (a start that never steadied, an early crash of the page process) lands on the processor, never on the plain graphics chip. */
    const val TRIAL_FALLBACK_SOFT = true
}

/**
 * GL2c: the "Parts of the page are missing" button. Each tap is written down with its time; two taps inside [WINDOW_MS] while the graphics chip draws move the tablet to the processor (the mode that never
 * lost a part of a page). The list is a comma-separated string of epoch milliseconds in the saved settings, oldest first, at most [KEEP] entries.
 */
object PaintReports {
    const val WINDOW_MS = 3L * 24 * 60 * 60 * 1000
    const val LIMIT = 2
    const val KEEP = 8

    fun parse(encoded: String): List<Long> = encoded.split(',').mapNotNull { it.trim().toLongOrNull() }.filter { it > 0 }

    /** [encoded] with [now] added and everything older than the window dropped. */
    fun add(encoded: String, now: Long): String =
        (parse(encoded).filter { now - it in 0..WINDOW_MS } + now).takeLast(KEEP).joinToString(",")

    fun count(encoded: String, now: Long): Int = parse(encoded).count { now - it in 0..WINDOW_MS }

    /** True when the tablet should now move to the processor: it draws with the graphics chip and the person has reported undrawn parts [LIMIT] times in the window. */
    fun fallBack(mode: DrawGuard.Mode, encoded: String, now: Long): Boolean = mode != DrawGuard.Mode.PROCESSOR && count(encoded, now) >= LIMIT

    /** The plain-words sentence for the person after a tap: what was noted and what happens next. */
    fun answer(mode: DrawGuard.Mode, encoded: String, now: Long): String = when {
        fallBack(mode, encoded, now) -> "Noted twice. Kestrel will draw pages with the processor from the next start (close it from the recent-apps list and open it again): slower, but it never leaves a part undrawn."
        mode == DrawGuard.Mode.PROCESSOR -> "Noted. The processor is already drawing the pages; the next upload carries this."
        else -> "Noted, and the screen was redrawn. If parts go missing again within 3 days, Kestrel switches to drawing with the processor."
    }

    /** What the report line says: the mode, the count in the window and whether the fall back happened. */
    fun logLine(mode: DrawGuard.Mode, encoded: String, now: Long): String =
        "mode=" + mode.label.replace(' ', '-') + " n=" + count(encoded, now) + " window=3d fallback=" + fallBack(mode, encoded, now)
}
