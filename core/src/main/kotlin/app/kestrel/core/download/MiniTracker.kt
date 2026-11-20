package app.kestrel.core.download

/**
 * What the small download window in the corner of the browser lists, and when a "download started" banner is due (owner message 26, AL2). Pure: it is fed a snapshot of the download
 * list about four times a second and says what changed, so the rules are unit-tested without a screen.
 *
 * - A download the engine has never been seen to hold is NEW: it is announced once and listed.
 * - What was already there when watching began (downloads restored from the last run: finished, paused, or carried on by themselves after a restart) is NOT announced and NOT listed, moving or not
 *   (owner message 2026-11-18, DM1: "the download manager shouldn't pop up by itself unless there is a new download"). Such a row joins the window only when the PERSON asks for it to move
 *   ([requested]: Resume / Retry / Restart in the Downloads list or the window). Something that resumes by itself (after a restart, when the network comes back) stays out.
 * - A row stays while it is live (moving, waiting, paused by the user, asking). When it finishes or fails it stays [lingerMs] more so the result is seen, then it goes. Canceled or removed rows go at once.
 */
class MiniTracker(private val lingerMs: Long = 6_000) {
    private val known = HashSet<Long>()
    /** id -> the time it ended (finished or failed), or null while it is live. Insertion order = the order of the rows (oldest first, so nothing jumps). */
    private val tracked = LinkedHashMap<Long, Long?>()
    /** Rows the user dismissed with the row's own X (finished ones): they are not listed again. */
    private val dismissed = HashSet<Long>()
    /** Old rows the person asked to move (Resume / Retry / Restart): they may join the window when they move. Written from the UI thread and the service thread. */
    private val wanted = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
    /** True once [begin] has run: the window must not begin before the rows of the last run have been read, or every one of them looks like a new download. */
    @Volatile var began = false
        private set

    class Update(
        /** Downloads that are new since the last call: show the banner for the last one. */
        val announce: List<DownloadView>,
        /** The rows to show now, oldest first. */
        val rows: List<DownloadView>,
    )

    private fun live(v: DownloadView) = v.state.active || v.state == DlState.QUEUED

    /** Start watching. Everything already in [views] is "old": remembered so it is never announced and never listed, moving or not (DM1). */
    fun begin(views: List<DownloadView>) {
        known.clear(); tracked.clear(); dismissed.clear()
        for (v in views) known.add(v.id)
        began = true
    }

    /** The person asked for this old download to move (Resume, Retry, Restart): when it moves it may join the window. */
    fun requested(id: Long) { wanted.add(id) }

    /** A finished (or stopped) row the user closed with its X. */
    fun dismiss(id: Long) { tracked.remove(id); dismissed.add(id) }

    fun update(views: List<DownloadView>, now: Long): Update {
        val byId = HashMap<Long, DownloadView>(views.size * 2)
        for (v in views) byId[v.id] = v
        val announce = ArrayList<DownloadView>(2)
        for (v in views) {
            if (known.add(v.id)) {
                if (v.state == DlState.CANCELED) continue
                if (v.state != DlState.FAILED) announce.add(v) // a download that failed at once (a bad address) is listed with its reason but did not "start"
                tracked[v.id] = null; dismissed.remove(v.id)
            }
            else if (v.id !in tracked && v.id !in dismissed && live(v) && wanted.remove(v.id)) tracked[v.id] = null    // the person resumed it (Downloads list, window, notification): not one that carried on by itself
            else if (v.id in dismissed && live(v)) { dismissed.remove(v.id); tracked[v.id] = null } // a closed row that was retried is a live download again
        }
        val it = tracked.entries.iterator()
        val rows = ArrayList<DownloadView>(tracked.size)
        while (it.hasNext()) {
            val e = it.next()
            val v = byId[e.key]
            if (v == null || v.state == DlState.CANCELED) { it.remove(); continue }
            if (v.state == DlState.COMPLETED || v.state == DlState.FAILED) {
                val ended = e.value ?: now.also { e.setValue(it) }
                if (now - ended > lingerMs) { it.remove(); continue }
            } else if (e.value != null) e.setValue(null) // failed or finished earlier, running again (Retry, Restart)
            rows.add(v)
        }
        return Update(announce, rows)
    }

    /** True while any listed row still needs a look from a timer (a finished or failed row waits to go). */
    fun hasLingering(): Boolean = tracked.values.any { it != null }
}
