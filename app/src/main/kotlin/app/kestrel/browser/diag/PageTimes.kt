package app.kestrel.browser.diag

import android.os.SystemClock

/**
 * The last page loads as THIS tablet saw them, for Diagnostics ("Page loads"): the site's host, milliseconds from the start of the navigation to the first paint and to the end of the load, how many
 * of the requests the Shields blocked, and how often the same host was loaded before (the first load of a heavy single-page app is the slow one; repeats show what the caches save).
 * Bounded (the last 14 loads), memory only, hosts only (never a path or a query), private tabs are not recorded. It exists because "very slow loading Proton Mail" cannot be fixed by guessing:
 * the owner copies the numbers from Diagnostics and the next change is made on them (research/GECKO_TUNING_2.md, "measure first").
 */
object PageTimes {
    private class Load(val host: String, val startedAt: Long, val nth: Int) {
        var paintMs = -1L
        var doneMs = -1L
        var blocked = 0
    }

    private const val KEEP = 14
    private val loads = ArrayList<Load>()
    private val open = HashMap<Any, Load>()
    private val seen = HashMap<String, Int>()

    private fun hostOf(url: String): String {
        val s = url.indexOf("://")
        if (s < 0) return ""
        val rest = url.substring(s + 3)
        val e = rest.indexOfAny(charArrayOf('/', '?', '#', ':'))
        return (if (e < 0) rest else rest.substring(0, e)).lowercase()
    }

    @Synchronized fun start(key: Any, url: String, priv: Boolean) {
        if (priv) { open.remove(key); return }
        val host = hostOf(url)
        if (host.isEmpty()) { open.remove(key); return }
        val n = (seen[host] ?: 0) + 1
        seen[host] = n
        if (seen.size > 200) seen.clear()
        val l = Load(host, SystemClock.elapsedRealtime(), n)
        open[key] = l
        loads.add(l)
        while (loads.size > KEEP) loads.removeAt(0)
    }

    @Synchronized fun paint(key: Any) {
        val l = open[key] ?: return
        if (l.paintMs < 0) l.paintMs = SystemClock.elapsedRealtime() - l.startedAt
    }

    @Synchronized fun stop(key: Any, blocked: Int) {
        val l = open.remove(key) ?: return
        if (l.doneMs < 0) l.doneMs = SystemClock.elapsedRealtime() - l.startedAt
        l.blocked = blocked
    }

    @Synchronized fun lines(): List<String> {
        if (loads.isEmpty()) return listOf("Page loads: (none yet; open a page)")
        val out = ArrayList<String>()
        out.add("Page loads (newest last; time from tap to first paint / to load finished):")
        val now = SystemClock.elapsedRealtime()
        for (l in loads) {
            val paint = if (l.paintMs >= 0) fmt(l.paintMs) else "-"
            val done = if (l.doneMs >= 0) fmt(l.doneMs) else "still loading (${fmt(now - l.startedAt)})"
            out.add("  ${l.host}  paint $paint, done $done, ${l.blocked} blocked" + if (l.nth > 1) ", visit #${l.nth}" else "")
        }
        return out
    }

    private fun fmt(ms: Long): String = if (ms < 10_000) "%.1f s".format(java.util.Locale.US, ms / 1000.0) else "${ms / 1000} s"
}
