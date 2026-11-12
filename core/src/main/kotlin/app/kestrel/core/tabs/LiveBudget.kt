package app.kestrel.core.tabs

/**
 * How many tabs keep a LIVE engine session (the page loaded, its back/forward cache and scroll position in memory) and how many are hibernated (session closed, the page
 * is loaded again from the history when the tab comes back).
 *
 * The old rule was a constant: 2 on a low-RAM device. With three tabs in rotation that is a miss on every switch, and every "the user left the app" callback closed all
 * but one. The tablet usually has 800 MB to 1 GB free, so the budget now follows the memory the kernel says is available (`MemAvailable` in /proc/meminfo, which counts
 * the page cache that can be given back): the more is free, the more tabs stay ready. It is evaluated when a tab is shown or opened, and on the system's memory
 * callbacks; a tab is never hibernated on a timer. Pure logic, no Android imports (JVM tests: LiveBudgetTest).
 */
object LiveBudget {
    /** The least that stays live: the tab in front and the one before it (so Back out of a link and switching back are instant). */
    const val FLOOR = 2

    /** The user's choice in Settings: 0 = automatic (follow free memory), otherwise a fixed number of live tabs. */
    const val AUTO = 0

    /** Available memory (MB) needed to keep n live tabs, starting at 3. Estimates, not measurements: a light page costs 30-80 MB inside the engine process, a heavy one 150-300 MB. */
    private val STEPS = intArrayOf(250, 350, 500, 700, 1000, 1500) // 3, 4, 5, 6, 7, 8 live tabs

    /**
     * @param availMb `MemAvailable` in MB, or a negative number when it cannot be read
     * @param lowRamDevice the device is flagged low-RAM (the fallback when the free memory is unknown: 2 live tabs, 4 on bigger devices)
     * @param systemLow Android itself says memory is low right now (`ActivityManager.MemoryInfo.lowMemory`): back to the floor whatever the setting says
     * @param setting [AUTO] or the user's fixed number
     */
    fun limit(availMb: Int, lowRamDevice: Boolean, systemLow: Boolean, setting: Int = AUTO): Int {
        if (systemLow) return FLOOR
        if (setting >= FLOOR) return setting.coerceAtMost(MAX)
        if (availMb < 0) return if (lowRamDevice) FLOOR else 4
        var n = FLOOR
        for (need in STEPS) { if (availMb >= need) n++ else break }
        return n
    }

    const val MAX = 8

    /**
     * How many live tabs to keep after a memory callback ([level] is `ComponentCallbacks2.onTrimMemory`'s argument), given the [budget] above and the available memory.
     * 10 RUNNING_LOW and 20 UI_HIDDEN ("the user left the app") do not mean the memory is short on a device with this little RAM (Android derives them from how many
     * cached processes there are), so they change nothing by themselves; the levels that say "the system is short" shrink the set only when the free memory agrees.
     */
    fun afterTrim(level: Int, availMb: Int, budget: Int): Int = when {
        level >= 60 -> 1                                                           // TRIM_MEMORY_MODERATE and up: this process is a prime candidate to be killed
        level >= 40 -> if (availMb in 0 until 600) FLOOR.coerceAtMost(budget) else budget // TRIM_MEMORY_BACKGROUND
        level >= 15 -> if (availMb in 0 until 350) FLOOR.coerceAtMost(budget) else budget // TRIM_MEMORY_RUNNING_CRITICAL (the engine also flushes its own caches here)
        else -> budget
    }
}
