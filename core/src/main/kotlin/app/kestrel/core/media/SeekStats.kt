package app.kestrel.core.media

/**
 * YS1 (research/SEEKBAR.md): what the content script seekbar.js has done for taps on a video player's progress bar since the app started, as plain numbers: taps on a bar, how many the PAGE answered by itself (the
 * video jumped before the script had to), how many the script answered (it moved the video to the place of the finger) and how many it left alone, with the last reason in a few words ("live", "ad", "off", "range",
 * "not ready", "gone", "refused"). Never an address. UI thread only; RAM only; private tabs are dropped by the caller before they reach it.
 * Why it exists: on the tablet YouTube's bar did not jump on a tap; whether the page or Kestrel does the jump decides where the next fix goes, and only these numbers can say.
 */
class SeekStats {
    var taps = 0; private set
    var page = 0; private set
    var ours = 0; private set
    var left = 0; private set
    private var reason = ""

    private fun c(n: Int) = n.coerceIn(0, 999)

    fun add(taps: Int, page: Int, ours: Int, left: Int, reason: String) {
        this.taps += c(taps); this.page += c(page); this.ours += c(ours); this.left += c(left)
        val r = reason.filter { it.code in 32..126 }.trim().take(24)
        if (r.isNotEmpty()) this.reason = r
    }

    fun reset() { taps = 0; page = 0; ours = 0; left = 0; reason = "" }

    /** One plain line for Diagnostics; only what happened is mentioned. */
    fun line(): String {
        if (taps == 0) return "Tap on a video bar: none seen yet"
        val sb = StringBuilder("Tap on a video bar: ").append(taps).append(if (taps == 1) " tap" else " taps")
        sb.append(", the page jumped by itself ").append(page).append(", Kestrel jumped ").append(ours)
        if (left > 0) sb.append(", left alone ").append(left)
        if (reason.isNotEmpty()) sb.append("; last reason: ").append(reason)
        return sb.toString()
    }
}
