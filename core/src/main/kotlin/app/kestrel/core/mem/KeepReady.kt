package app.kestrel.core.mem

/**
 * KR1 (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"; research/DIAG_2026-11-27.md section 2.3): the engine's content process was killed 7 times in 17 hours,
 * always while Kestrel was in the background (15 to 69 minutes after it was left, with 540 to 980 MB available around the kill: Android 10 Go's killer takes CACHED processes first), and every return then
 * reloaded claude.ai for 25 to 30 seconds and lost the page's state. An app with a foreground service is not cached: it dies near the end of the list instead of near the front. So Kestrel G holds one
 * quiet foreground service while it is away, for the time the person chose, and lets go of it when the app comes back, when the time is up, when the task is swiped away or when "Let go" is tapped.
 * It loads nothing and sends nothing; it only keeps the process out of the cached class.
 *
 * This is the pure rule (what the choices mean, when to hold). The Android side is geckoapp `KeepReadyService`.
 */
object KeepReady {
    const val OFF = 0
    const val QUARTER = 1
    const val HOUR = 2
    const val LONG = 3
    /** On by default for one hour: the owner's own diagnostics show every kill came 15 to 69 minutes after leaving. */
    const val DEFAULT = HOUR

    private val MINUTES = intArrayOf(0, 15, 60, 180)

    fun valid(choice: Int): Int = choice.coerceIn(OFF, LONG)
    fun minutes(choice: Int): Int = MINUTES[valid(choice)]
    fun millis(choice: Int): Long = minutes(choice) * 60_000L

    /** The words of the choice, for Settings and the Diagnostics line. */
    fun words(choice: Int): String = when (valid(choice)) { OFF -> "off"; QUARTER -> "15 minutes"; HOUR -> "1 hour"; else -> "3 hours" }

    /**
     * Hold the page ready now? [hasPage]: some tab has a live engine session on a real page (not Home, not blank). [lowMemory]: the system itself says memory is short right now
     * (ActivityManager.MemoryInfo.lowMemory): the tablet then needs the room more than the page does. [finishing]: the screen is going away for good (the person closed it).
     */
    fun applies(choice: Int, hasPage: Boolean, lowMemory: Boolean, finishing: Boolean): Boolean = valid(choice) != OFF && hasPage && !lowMemory && !finishing

    /** Why a hold did not start (for the Health line); empty when it should. */
    fun whyNot(choice: Int, hasPage: Boolean, lowMemory: Boolean, finishing: Boolean): String = when {
        valid(choice) == OFF -> "off"
        finishing -> "closing"
        !hasPage -> "no page"
        lowMemory -> "memory short"
        else -> ""
    }

    /** The hold ends by itself after [choice]'s time: has it run out? [heldMs] is how long the service has run. */
    fun expired(choice: Int, heldMs: Long): Boolean = valid(choice) != OFF && heldMs >= millis(choice)
}
