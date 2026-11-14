package app.kestrel.core.media

/**
 * RX3-Y2 (research/YOUTUBE_ADS.md section 4): what the YouTube ad skipper (content script youtube.js) has done since the app started, as plain numbers. The script reports DIFFERENCES (ads it saw, ads it saw
 * end while it was working, skip buttons pressed, overlay ads closed, enforcement dialogs removed, ads moved to their end, ads it gave up on) and, in a few words, the last thing that went wrong.
 * Never an address. UI thread only; RAM only; private tabs are dropped by the caller before they reach it.
 * RX3-G7: the same numbers come from the script for other video players (players.js); the app keeps a second instance for them with its own [label].
 */
class AdSkipStats(private val label: String = "YouTube ads") {
    var seen = 0; private set
    var skipped = 0; private set
    private var clicks = 0; private var overlays = 0; private var dialogs = 0; private var jumps = 0; private var gaveUp = 0
    private var problem = ""

    private fun c(n: Int) = n.coerceIn(0, 999)

    fun add(seen: Int, skipped: Int, clicks: Int, overlays: Int, dialogs: Int, jumps: Int, gaveUp: Int, reason: String) {
        this.seen += c(seen); this.skipped += c(skipped); this.clicks += c(clicks); this.overlays += c(overlays); this.dialogs += c(dialogs); this.jumps += c(jumps); this.gaveUp += c(gaveUp)
        val r = reason.filter { it.code in 32..126 }.trim().take(60)
        if (r.isNotEmpty()) problem = r
    }

    fun reset() { seen = 0; skipped = 0; clicks = 0; overlays = 0; dialogs = 0; jumps = 0; gaveUp = 0; problem = "" }

    /** One plain line for Diagnostics; only what happened is mentioned. */
    fun line(): String {
        val sb = StringBuilder(label).append(": ")
        if (seen == 0) sb.append("none seen yet")
        else {
            sb.append(seen).append(" seen, ").append(skipped).append(" skipped")
            val how = ArrayList<String>()
            if (jumps > 0) how.add("$jumps fast-forwarded")
            if (clicks > 0) how.add("$clicks skip button${if (clicks == 1) "" else "s"} pressed")
            if (how.isNotEmpty()) sb.append(" (").append(how.joinToString(", ")).append(')')
        }
        if (overlays > 0) sb.append(", ").append(overlays).append(" overlay").append(if (overlays == 1) "" else "s").append(" closed")
        if (dialogs > 0) sb.append(", ").append(dialogs).append(" enforcement dialog").append(if (dialogs == 1) "" else "s").append(" removed")
        if (gaveUp > 0) sb.append(", ").append(gaveUp).append(" gave up")
        if (problem.isNotEmpty()) sb.append("; last problem: ").append(problem)
        return sb.toString()
    }
}
