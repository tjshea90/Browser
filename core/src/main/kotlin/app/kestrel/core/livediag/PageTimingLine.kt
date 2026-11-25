package app.kestrel.core.livediag

/**
 * LD4: the ONE line of the live diagnostics that says where a page load's time went. Pure text assembly (the app gives it the numbers pagetime.js measured and what the tablet was doing), so the key names, their
 * order and the places the digest and DiagStats read them from are tested. Counts and milliseconds only; the host is the site's own name (the journal's scrub turns a sensitive one into `<sensitive-site>`), the three slowest
 * requests are `host:kind:ms` (a host name, never a path).
 * Keys: nt navigation kind (0 new, 1 reload, 2 back/forward); dns con tls (ms); fb = ms from the start of the navigation to the first byte, ttfb = the server's wait; fcp lcp dcl load (ms since the navigation began, -1 = none);
 * rn requests, rc from the cache, kb over the network, r1 / r3 took 1 s / 3 s or more, rsum their summed ms; h2 h1 h3 protocols; js css img xhr oth kinds; lag = worst lateness / sum of lateness over 50 ms / how many
 * timers; nodes scr fr = elements, scripts, frames; rd redirects; hid = the person looked away (the numbers say nothing about speed); pt = report cut short; at = ms since the start when it was sent.
 */
object PageTimingLine {
    class Slow(val host: String, val kind: String, val ms: Long)

    private val KEYS = listOf("nt", "dns", "con", "tls", "fb", "ttfb", "fcp", "lcp", "dcl", "load", "rn", "rc", "kb", "r1", "r3", "rsum", "h2", "h1", "h3", "js", "css", "img", "xhr", "oth")
    private val TAIL = listOf("nodes", "scr", "fr", "rd", "hid", "pt", "at")

    /** [v] = the numbers by key (missing = -1), [lag] = the three lateness numbers, [context] = what the tablet was doing ("shields=up net=fast-wifi avail=420 cpu=88 tabs=1"). */
    fun build(host: String, v: Map<String, Long>, lag: List<Long>, slow: List<Slow>, context: String): String {
        val sb = StringBuilder(380)
        sb.append("host=").append(host)
        for (k in KEYS) sb.append(' ').append(k).append('=').append(v[k] ?: -1L)
        sb.append(" lag=").append(lag.getOrElse(0) { 0L }).append('/').append(lag.getOrElse(1) { 0L }).append('/').append(lag.getOrElse(2) { 0L })
        for (k in TAIL) sb.append(' ').append(k).append('=').append(v[k] ?: -1L)
        if (slow.isNotEmpty()) sb.append(" slow=").append(slow.take(3).joinToString(",") { it.host + ":" + it.kind + ":" + it.ms })
        if (context.isNotBlank()) sb.append(" | ").append(context.trim())
        return sb.toString()
    }
}
