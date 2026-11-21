package app.kestrel.core.download

/**
 * When a launch intent that carries "open the Downloads list" (the tap on a download notification) really means it (owner message 2026-11-18, DM1: the download manager popped up on a restart
 * and at other times). The notification's launch intent stays on the app's task: after a crash, after Android took the app for memory, or when it is brought back from Recents, Android
 * starts the activity AGAIN with that same intent. That replay is not a tap, so it must not open the list.
 */
object ManagerIntent {
    /**
     * @param extra the intent carries the open-Downloads flag
     * @param restoredState the activity is being re-created from saved state (a restart after the process went, a configuration change): savedInstanceState != null
     * @param fromHistory the intent has FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY: the system started it from Recents, not a notification tap
     */
    fun shouldOpen(extra: Boolean, restoredState: Boolean, fromHistory: Boolean): Boolean = extra && !restoredState && !fromHistory
}
