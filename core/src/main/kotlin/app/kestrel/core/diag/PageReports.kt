package app.kestrel.core.diag

/** One page's report as the extension sent it. The lines are already short ("script cdn.example/lib.js?.. CONNECTION_RESET"); this class only caps and formats them. */
class PageReport(
    val page: String,
    val requests: Int,
    val blockedCount: Int,
    val blocked: List<String>,
    val failed: List<String>,
    val errors: List<String>,
    val at: Long,
)

/**
 * The last few pages' reports for Diagnostics > Page report (what Shields blocked, what failed to load, which script threw). A page that is reported again (the extension sends up to three
 * updates per page) replaces its older entry. Pure (no Android imports) so it is unit-tested; the bridge decides what may be kept: never a private tab's page.
 *
 * Why it exists: a page that is half empty in Kestrel and fine in another browser is almost always one file that did not arrive or one script that threw, and nothing on the screen says which.
 */
class PageReports(private val max: Int = 6) {
    private val list = ArrayList<PageReport>()

    @Synchronized fun add(r: PageReport) {
        list.removeAll { it.page == r.page }
        list.add(0, r)
        while (list.size > max) list.removeAt(list.size - 1)
    }

    @Synchronized fun clear() { list.clear() }

    @Synchronized fun snapshot(): List<PageReport> = ArrayList(list)

    /** The block for the Diagnostics text. */
    fun text(now: Long): String {
        val all = snapshot()
        val head = "Page report (the last pages: what Shields blocked, what failed to load, scripts that threw; a private tab is never listed)"
        if (all.isEmpty()) return "$head: nothing yet. Open a page, wait a few seconds, then open Diagnostics."
        val sb = StringBuilder(head).append(':')
        for (r in all) {
            val bits = ArrayList<String>()
            bits.add("${r.requests} requests")
            bits.add("${r.blockedCount} blocked by Shields")
            bits.add(if (r.failed.isEmpty()) "no failed loads" else "${r.failed.size} FAILED")
            bits.add(if (r.errors.isEmpty()) "no script errors" else "${r.errors.size} script error" + (if (r.errors.size == 1) "" else "s"))
            sb.append("\n  ").append(r.page.removePrefix("https://").removePrefix("http://").take(90)).append(" (").append(ago(now - r.at)).append("): ").append(bits.joinToString(", "))
            if (r.blocked.isNotEmpty()) sb.append("\n    blocked: ").append(r.blocked.take(10).joinToString("; ")).append(if (r.blockedCount > 10) "; ... ${r.blockedCount - 10} more" else "")
            if (r.failed.isNotEmpty()) sb.append("\n    FAILED: ").append(r.failed.take(20).joinToString("; "))
            if (r.errors.isNotEmpty()) sb.append("\n    script errors: ").append(r.errors.take(8).joinToString("; "))
        }
        return sb.toString()
    }

    private fun ago(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return if (s < 90) "$s s ago" else "${s / 60} min ago"
    }
}
