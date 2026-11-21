package app.kestrel.core.media

/**
 * VD1 (owner 2026-11-21: "diagnose why Instagram reels don't load and some videos don't load on websites"): what happened to the videos of each SITE in this run. The Diagnostics screen already counted what the press-to-play
 * gate held back and what the format policy refused, but summed over every page, so it could not say which site was affected. The extension now tells the app the numbers with the page they came from; this table keeps them
 * by site (host names only, at most [max] sites, the least recently touched is dropped), memory only, and says them in one sentence for the Diagnostics screen. Never fed from a private tab (the caller checks).
 */
class VideoSites(private val max: Int = 16) {
    private class S {
        var held = 0L; var kept = 0L; var refused = 0L; var opened = 0L
        var sizeNo = 0L; var modeNo = 0L; var fpsNo = 0L; var last = ""
        val errors = LinkedHashMap<String, Int>()
        fun problems(): Long = held + kept + refused + sizeNo + modeNo + fpsNo + errors.values.sum()
    }

    private val sites = object : LinkedHashMap<String, S>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, S>?): Boolean = size > max
    }

    private fun of(host: String): S = sites.getOrPut(host) { S() }

    /** What the gate did on a page of [host]: held back (markup preload / autoplay), kept script players from fetching, refused plays nobody pressed, opened by the person. */
    @Synchronized fun gate(host: String, held: Int, opened: Int, kept: Int, refused: Int) {
        val s = of(host); s.held += held; s.opened += opened; s.kept += kept; s.refused += refused
    }

    /** What the format policy answered no to on a page of [host]: above the chip's size, the H.264-only mode, a frame rate above 30; [last] is the short word of the last refused format. */
    @Synchronized fun codec(host: String, size: Int, mode: Int, fps: Int, last: String) {
        val s = of(host); s.sizeNo += size; s.modeNo += mode; s.fpsNo += fps; if (last.isNotEmpty()) s.last = last.take(30)
    }

    /** A media element on a page of [host] failed: the code (MediaError 1..4, 9 = a <source> child) and where its file comes from (none / blob / hls / dash / http). */
    @Synchronized fun error(host: String, code: Int, src: String) {
        val s = of(host); val k = "code $code $src"; s.errors[k] = (s.errors[k] ?: 0) + 1
        while (s.errors.size > 6) s.errors.remove(s.errors.keys.first())
    }

    @Synchronized fun isEmpty(): Boolean = sites.values.none { it.problems() > 0 || it.opened > 0 }

    @Synchronized fun line(): String {
        val rows = sites.entries.filter { it.value.problems() > 0 }.sortedByDescending { it.value.problems() }.take(6)
        if (rows.isEmpty()) return "Videos by site (this run): no site had a video held back, refused or failed yet"
        return "Videos by site (this run; most affected first): " + rows.joinToString(" | ") { (h, s) ->
            val parts = ArrayList<String>()
            if (s.held + s.kept + s.refused > 0) parts.add("press-to-play rule held ${s.held} (markup), kept ${s.kept} script players from fetching, refused ${s.refused} plays, opened by a press ${s.opened}")
            if (s.sizeNo + s.modeNo + s.fpsNo > 0) parts.add("format answered no: size ${s.sizeNo}, H.264-only ${s.modeNo}, 30 fps ${s.fpsNo}" + if (s.last.isNotEmpty()) " (last ${s.last})" else "")
            if (s.errors.isNotEmpty()) parts.add("errors " + s.errors.entries.joinToString(", ") { "${it.key} x${it.value}" })
            "$h: " + parts.joinToString("; ")
        }
    }
}
