package app.kestrel.core.livediag

/**
 * PERF12 (owner 2026-11-23: "see if anything can be improved"): the alpha95 uploads carried a site that was loaded 72 times in one run (median 0.4 s, no requests of its own): a page that sends the tab on to the next
 * page again and again (an ad redirect chain, a page that reloads itself) costs the tablet work and the hotspot data and says nothing in the live diagnostics except a big count. [hit] is told every page load of a normal
 * tab; when the same host starts [limit] times inside [windowMs] it says so ONCE per run of such loads (and again after every [limit] further ones), so a `page loop host= n= secs=` line names it. Pure, bounded, no I/O.
 */
class LoopWatch(private val limit: Int = 6, private val windowMs: Long = 60_000L) {
    private class Run(val host: String) { val times = ArrayDeque<Long>(); var told = 0 }
    private val tabs = HashMap<Any, Run>()

    /** One more page load of [host] in [tab] at [now] (any steady clock, ms). Returns how many loads of this host the tab has made inside the window when it is time to say so, else 0. */
    @Synchronized fun hit(tab: Any, host: String, now: Long): Int {
        if (host.isEmpty()) return 0
        var r = tabs[tab]
        if (r == null || r.host != host) { if (tabs.size >= MAX_TABS) tabs.clear(); r = Run(host); tabs[tab] = r }
        r.times.addLast(now)
        while (r.times.isNotEmpty() && now - r.times.first() > windowMs) r.times.removeFirst()
        val n = r.times.size
        if (n < limit) { if (n <= 1) r.told = 0; return 0 }
        val due = r.told == 0 || n >= r.told + limit
        if (!due) return 0
        r.told = n
        return n
    }

    /** The span in seconds covered by the loads now in the window (for the line). */
    @Synchronized fun spanSeconds(tab: Any): Long { val r = tabs[tab] ?: return 0L; return if (r.times.size < 2) 0L else (r.times.last() - r.times.first()) / 1000 }

    @Synchronized fun forget(tab: Any) { tabs.remove(tab) }

    companion object { private const val MAX_TABS = 40 }
}
