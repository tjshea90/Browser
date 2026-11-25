package app.kestrel.core.gfx

/**
 * PERF25 (owner 2026-11-24: "When i go to the parlayapi site in brave browser, it is smooth"; research/BRAVE_SMOOTH.md).
 *
 * Kestrel G draws every page with the PROCESSOR ("Software drawing", on since 2026-11-23) because the tablet's graphics chip (Mali-G52) lost bands of text in Firefox's graphics
 * renderer (research/PAINT_GLITCH.md). The cost, measured on the owner's tablet: pages draw about 6 pictures a second right after they load, a heavy page paints for the first time
 * after 14.6 s instead of 2.0 s, and typing, scrolling and the page animations all wait for the processor. A browser that draws on the graphics chip (Brave, Chrome) does not pay it.
 *
 * Mozilla's bug history names the paths of this renderer that Mali parts got wrong, and TWO of the switches for them exist in GeckoView 160 and were never tried here:
 *  - `gfx.webrender.use-optimized-shaders` (bug 1689064: on a Mali-T628 the "optimized" shader variants gave completely broken rendering; Mozilla blocked them on all Mali-T6xx),
 *  - `gfx.webrender.draw-calls-for-texture-copy` (bugs 1669494 / 1669960 / 1677757: copying and resizing the renderer's texture caches with the driver's blit call corrupted text on Mali-G parts),
 *  - `gfx.webrender.program-binary-disk` (the driver's saved compiled programs: a stale or wrong one would draw wrongly from the second start on, never the first).
 * A trial is "hardware drawing with those off". Level 2 also runs the renderer without its separate helper process and without compiling all programs at start.
 *
 * It is a TRIAL: Settings > Network lab, default OFF (= what the owner has now), applied at the next start, and it can never trap the person in a browser that does not start (see [startFailed]).
 * Pure Kotlin so the rules are tested on the JVM; the Android side only reads and writes the two numbers.
 */
object DrawTrial {
    const val OFF = 0
    const val FIXES = 1
    const val NO_HELPER = 2
    const val MAX = 2

    /** A start counts as good once the engine has drawn this long without dying; the mark of the start is taken away then. */
    const val STABLE_MS = 15_000L

    private val base = listOf(
        "gfx.webrender.use-optimized-shaders" to "false",
        "gfx.webrender.draw-calls-for-texture-copy" to "true",
        "gfx.webrender.program-binary-disk" to "false",
    )

    /** Engine preferences (name to value) for the config file. Never includes software drawing: a trial IS the other way of drawing. */
    fun lines(trial: Int): List<Pair<String, String>> = when {
        trial >= NO_HELPER -> base + ("layers.gpu-process.enabled" to "false")
        trial >= FIXES -> base
        else -> emptyList()
    }

    /** Names that gecko-prefs.txt already sets (a duplicate key would be a config error): they replace that line, `name=value`. */
    fun overrides(trial: Int): List<Pair<String, String>> =
        if (trial >= NO_HELPER) listOf("gfx.webrender.precache-shaders" to "false") else emptyList()

    /** Every preference a trial can set, for the Diagnostics read-back and the "exists in this engine" check. */
    val names: List<String> = (lines(MAX) + overrides(MAX)).map { it.first }

    /** The CPU-drawing switch counts only while no trial runs. */
    fun softwareDrawing(labSoft: Boolean, trial: Int): Boolean = labSoft && trial <= OFF

    /**
     * The previous start of this trial never reached a steady picture (the mark taken at its start was still there when the next start began): the drawing path killed the app or hung it.
     * The trial then switches itself off, so a driver that cannot do it costs the person one failed start, never a browser that will not open.
     */
    fun startFailed(trial: Int, startMark: Long): Boolean = trial > OFF && startMark > 0L

    /** A crash of the page process this soon after the process started, with a trial on, ends the trial too (taking effect at the next start). */
    const val EARLY_CRASH_MS = 3 * 60_000L
    fun crashEndsTrial(trial: Int, sinceProcessStartMs: Long): Boolean = trial > OFF && sinceProcessStartMs in 0 until EARLY_CRASH_MS

    fun label(trial: Int): String = when {
        trial >= NO_HELPER -> "hardware drawing, fixes, no helper process"
        trial >= FIXES -> "hardware drawing, fixes"
        else -> "off"
    }
}
