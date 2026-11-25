package app.kestrel.core.session

/**
 * PERF17b (owner 2026-11-24: "Do the lazy restore fix if it is logical and what major browsers do"; research/LAZY_RESTORE.md): after a LONG break the page that was in front is not loaded at the start.
 *
 * What the major browsers do (read 2026-11-24): every one of them restores the tab list at once and loads only the tab in front, the others when they are opened (Firefox `browser.sessionstore.restore_on_demand`,
 * Chrome's frozen tabs; Kestrel already did this: only the selected tab got its page). And two of them go one step further for the tab in front after a long break: Firefox for Android opens on its homepage when the
 * app was left four hours ago or more ("Choose your opening screen": last tab for less than four hours, homepage after four hours), Chrome opens its start page with a card for the last tab. The old page is
 * a click away, and a start does not spend CPU on a page nobody asked for. On this tablet that CPU is large: a heavy web app costs 12 to 27 s of the one content thread at every start.
 *
 * Kestrel's version: the tab in front comes back on the Home screen (the mechanism of AQ1: the tab keeps its address and Back history, nothing loads), with one line "Continue: <title>" under the search field.
 * A tap on it (or Back, or a new address) shows the page. Less than four hours away: the page loads at once, as before.
 */
object LazyStart {
    /** Away this long (the Firefox for Android rule) or longer: the page in front waits for a tap. */
    const val AWAY_MS = 4L * 60 * 60 * 1000

    /** Longest title shown on the Continue line. */
    const val MAX_TITLE = 48

    /** Milliseconds between the moment the app was left and [now]; 0 when it was never stamped or the clock went back (then the page simply loads, as before). */
    fun awayMs(leftAt: Long, now: Long): Long = if (leftAt <= 0L || now < leftAt) 0L else now - leftAt

    /**
     * True when the tab in front must wait for a tap. Every condition keeps the old behaviour (load at once) when it does not hold:
     * the switch is on, the break was long enough, the page is a real web page, the app was not started by a link or a share (that page is what the person wants now), the start is a fresh one (not the system
     * re-creating the screen of a running app) and the tab was not already left on Home.
     */
    fun applies(enabled: Boolean, awayMs: Long, url: String, startedByLink: Boolean, freshStart: Boolean, homeOverPage: Boolean): Boolean =
        enabled && awayMs >= AWAY_MS && freshStart && !startedByLink && !homeOverPage && (url.startsWith("http://") || url.startsWith("https://"))

    /** The text of the Continue line: the page's title, or its site when it has none, cut to a short line. */
    fun label(title: String, url: String): String {
        val t = title.trim().replace(Regex("\\s+"), " ")
        val host = url.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore('#').removePrefix("www.")
        val what = if (t.isNotEmpty()) t else host.ifEmpty { "the last page" }
        val cut = if (what.length > MAX_TITLE) what.substring(0, MAX_TITLE - 1).trimEnd() + "…" else what
        return "Continue: $cut"
    }

    /** A short plain description of the break, for the diagnostics line. */
    fun describe(awayMs: Long): String = when {
        awayMs <= 0L -> "no break known"
        awayMs < 60L * 60_000 -> "${awayMs / 60_000} min"
        else -> "${awayMs / 3_600_000} h ${(awayMs % 3_600_000) / 60_000} min"
    }
}
