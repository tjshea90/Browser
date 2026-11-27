package app.kestrel.core.media

/**
 * RX4-M0e (research/REVIEW-EXTERNAL-4.md): how smooth is scrolling on REAL pages? The owner's Scroll test page was removed at his request and `Health.watchFrames` measures the app's window, not the page,
 * so this is the passive instrument: ONLY while the Settings switch "Measure scrolling on real pages" is on (default off), the page script counts the frames between a page's first scroll event and 200 ms
 * after its last one, and reports the burst as numbers (frames, gaps over 50 ms, gaps over 100 ms, the longest gap). This class adds them up for one Diagnostics line. Bounded (20 hosts), memory only, hosts
 * only, nothing for a private tab (dropped before). A frame callback makes the page's own thread tick every frame during the burst, so the numbers say how well the PAGE keeps up (what leaves blank patches),
 * not how smooth the engine's own scrolling is; the line says so.
 */
class ScrollGaps {
    private class Site(val host: String) { var bursts = 0; var frames = 0L; var g50 = 0L; var g100 = 0L; var worst = 0 }

    private val sites = LinkedHashMap<String, Site>()
    private var bursts = 0; private var frames = 0L; private var g50 = 0L; private var g100 = 0L

    @Synchronized fun add(host: String, frames: Int, gaps50: Int, gaps100: Int, worstMs: Int) {
        if (host.isEmpty() || frames < MIN_FRAMES || frames > MAX_FRAMES || gaps50 < 0 || gaps100 < 0 || gaps100 > gaps50 || gaps50 > frames) return
        val s = sites.remove(host) ?: Site(host)
        sites[host] = s
        s.bursts++; s.frames += frames; s.g50 += gaps50; s.g100 += gaps100; if (worstMs.coerceIn(0, MAX_GAP_MS) > s.worst) s.worst = worstMs.coerceIn(0, MAX_GAP_MS)
        bursts++; this.frames += frames; g50 += gaps50; g100 += gaps100
        while (sites.size > MAX_SITES) sites.remove(sites.keys.first())
    }

    fun clear() = synchronized(this) { sites.clear(); bursts = 0; frames = 0; g50 = 0; g100 = 0 }

    /** One Diagnostics line; [on] = the Settings switch. */
    @Synchronized fun line(on: Boolean): String {
        if (bursts == 0) return "Real scrolling: " + if (on) "measuring (the switch is on): scroll a page and the numbers appear here" else "off (Settings > Measure scrolling on real pages turns it on for a while)"
        val worst = sites.values.maxByOrNull { it.worst }
        return "Real scrolling: $bursts bursts on ${sites.size} site(s), $frames frames, gaps over 50 ms in ${pct(g50, frames)} %, over 100 ms in ${pct(g100, frames)} %" +
            (if (worst != null && worst.worst > 0) " (worst: ${worst.host} ${worst.worst} ms)" else "") +
            "; measured by a frame callback that runs only while a page scrolls, so it shows how well the PAGE keeps up (blank patches), not the engine's own scrolling" + if (on) "" else " (the switch is off now)"
    }

    companion object {
        const val MAX_SITES = 20
        const val MIN_FRAMES = 5
        const val MAX_FRAMES = 5_000
        const val MAX_GAP_MS = 10_000
        private fun pct(a: Long, b: Long): String = if (b <= 0) "0.0" else { val t = (a * 1000 + b / 2) / b; "${t / 10}.${t % 10}" }
    }
}
