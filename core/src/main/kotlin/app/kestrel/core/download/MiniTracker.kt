package app.kestrel.core.download

/**
 * What the small download window in the corner of the browser lists, and when a "download started" banner is due (owner message 26, AL2). Pure: it is fed a snapshot of the download
 * list about four times a second and says what changed, so the rules are unit-tested without a screen.
 *
 * - A download the engine has never been seen to hold is NEW: it is announced once and listed.
 * - What was already there when watching began (downloads restored from the last run, paused) is not announced and not listed; it joins the window when it starts moving.
 * - A row stays while it is live (moving, waiting, paused by the user, asking). When it finishes or fails it stays [lingerMs] more so the result is seen, then it goes. Canceled or removed rows go at once.
 */
class MiniTracker(private val lingerMs: Long = 6_000) {
    private val known = HashSet<Long>()
    /** id -> the time it ended (finished or failed), or null while it is live. Insertion order = the order of the rows (oldest first, so nothing jumps). */
    private val tracked = LinkedHashMap<Long, Long?>()
    /** Rows the user dismissed with the row's own X (finished ones): they are not listed again. */
    private val dismissed = HashSet<Long>()

    class Update(
        /** Downloads that are new since the last call: show the banner for the last one. */
        val announce: List<DownloadView>,
        /** The rows to show now, oldest first. */
        val rows: List<DownloadView>,
    )

    private fun live(v: DownloadView) = v.state.active || v.state == DlState.QUEUED

    /** Start watching. Everything already in [views] is "old": remembered so it is never announced, and listed only if it is moving right now (the window was just recreated, e.g. a rotation). */
    fun begin(views: List<DownloadView>) {
        known.clear(); tracked.clear(); dismissed.clear()
        for (v in views) { known.add(v.id); if (live(v)) tracked[v.id] = null }
    }

    /** A finished (or stopped) row the user closed with its X. */
    fun dismiss(id: Long) { tracked.remove(id); dismissed.add(id) }

    fun update(views: List<DownloadView>, now: Long): Update {
        val byId = HashMap<Long, DownloadView>(views.size * 2)
        for (v in views) byId[v.id] = v
        val announce = ArrayList<DownloadView>(2)
        for (v in views) {
            if (known.add(v.id)) { if (v.state != DlState.CANCELED) { announce.add(v); tracked[v.id] = null; dismissed.remove(v.id) } }
            else if (v.id !in tracked && v.id !in dismissed && live(v)) tracked[v.id] = null    // started moving later (resumed from the manager)
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
