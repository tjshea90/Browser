package app.kestrel.core.media

/**
 * YS1 (research/SEEKBAR.md): what the content script seekbar.js has done for taps on a video player's progress bar since the app started, as plain numbers: taps on a bar, how many the PAGE answered by itself (the
 * video jumped before the script had to), how many the script answered (it moved the video to the place of the finger) and how many it left alone, with the last reason in a few words ("live", "ad", "off", "range",
 * "not ready", "gone", "refused"). It also counts taps that landed on a thin wide element inside a player that the script did NOT take for a bar, with that element's tag and first class name (for example
 * "div.some-bar"): if the owner's player has a bar the script does not know, this is how its name reaches the next fix. Never an address. UI thread only; RAM only; private tabs are dropped by the caller before they reach it.
 * Why it exists: on the tablet YouTube's bar did not jump on a tap; whether the page or Kestrel does the jump decides where the next fix goes, and only these numbers can say.
 */
class SeekStats {
    var taps = 0; private set
    var page = 0; private set
    var ours = 0; private set
    var left = 0; private set
    var unknown = 0; private set
    private var reason = ""
    private var unknownName = ""

    private fun c(n: Int) = n.coerceIn(0, 999)

    fun add(taps: Int, page: Int, ours: Int, left: Int, reason: String, unknown: Int = 0, name: String = "") {
        this.taps += c(taps); this.page += c(page); this.ours += c(ours); this.left += c(left); this.unknown += c(unknown)
        val r = reason.filter { it.code in 32..126 }.trim().take(24)
        if (r.isNotEmpty()) this.reason = r
        val n = name.filter { it.isLetterOrDigit() && it.code < 128 || it == '-' || it == '_' || it == '.' }.take(40)
        if (n.isNotEmpty()) unknownName = n
    }

    fun reset() { taps = 0; page = 0; ours = 0; left = 0; unknown = 0; reason = ""; unknownName = "" }

    /** One plain line for Diagnostics; only what happened is mentioned. */
    fun line(): String {
        val sb = StringBuilder("Tap on a video bar: ")
        if (taps == 0) sb.append("none seen yet")
        else {
            sb.append(taps).append(if (taps == 1) " tap" else " taps")
            sb.append(", the page jumped by itself ").append(page).append(", Kestrel jumped ").append(ours)
            if (left > 0) sb.append(", left alone ").append(left)
            if (reason.isNotEmpty()) sb.append("; last reason: ").append(reason)
        }
        if (unknown > 0) sb.append(if (taps == 0) "; " else ", ").append(unknown).append(" tap").append(if (unknown == 1) "" else "s").append(" on a bar-like strip Kestrel did not know")
            .append(if (unknownName.isNotEmpty()) " (last: $unknownName)" else "")
        return sb.toString()
    }
}
