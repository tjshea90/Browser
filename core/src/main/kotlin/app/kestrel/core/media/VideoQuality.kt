package app.kestrel.core.media

/**
 * RX4-M0a (research/REVIEW-EXTERNAL-4.md): how smoothly do REAL videos play on REAL pages on this tablet?
 *
 * The lab clips prove the decoders; what the owner sees is a site's player (ad scripts, hls.js, anti-adblock) on a CPU three to five times slower than a phone. The page's own
 * `video.getVideoPlaybackQuality()` counts the frames it showed and the frames it dropped; the extension reads it every 15 s while a video plays (and when it pauses or the page hides) and
 * sends only the DIFFERENCE since its last look, as numbers: frames, dropped, corrupted, seconds, the picture size. This class adds them up for one Diagnostics line. Bounded (20 sites, 200 looks),
 * memory only, host names only (never a path or a query), nothing is kept for a private tab (the app drops those before they get here).
 *
 * How to read the line (the decision rule of the review): dropped over 2 % while "CPU by process" shows the page's process near 100 % = the page's own scripts / layout (block more, Calm pages);
 * dropped high with low CPU = decoding / compositing / the codec (compare "H.264 only" with the automatic choice); dropped near 0 but the owner still sees stutter = the stream or the link.
 */
class VideoQuality {
    private class Site(val host: String) {
        var looks = 0; var frames = 0L; var dropped = 0L; var corrupt = 0L; var seconds = 0L
        var worstPct = 0; var worstSize = ""; var lastAt = 0L
    }

    private val sites = LinkedHashMap<String, Site>()
    private var looks = 0
    private var totalFrames = 0L; private var totalDropped = 0L; private var totalCorrupt = 0L; private var totalSeconds = 0L

    /** One look from a page: [frames] / [dropped] / [corrupt] since its last look, over [seconds]; the picture was [w] x [h]. */
    @Synchronized fun add(host: String, frames: Int, dropped: Int, corrupt: Int, seconds: Int, w: Int, h: Int, now: Long = 0L) {
        if (host.isEmpty() || frames < 0 || dropped < 0 || frames > MAX_FRAMES || dropped > frames + 5) return
        val s = sites.remove(host) ?: Site(host)
        sites[host] = s // most recent last
        s.looks++; s.frames += frames; s.dropped += dropped; s.corrupt += corrupt; s.seconds += seconds.coerceIn(0, MAX_SECONDS); s.lastAt = now
        if (frames >= MIN_FRAMES_FOR_WORST) {
            val pct = percent(dropped.toLong(), frames.toLong())
            if (pct >= s.worstPct) { s.worstPct = pct; s.worstSize = if (w > 0 && h > 0) "${w}x$h" else "" }
        }
        looks++; totalFrames += frames; totalDropped += dropped; totalCorrupt += corrupt; totalSeconds += seconds.coerceIn(0, MAX_SECONDS)
        while (sites.size > MAX_SITES) sites.remove(sites.keys.first())
        if (looks > MAX_LOOKS) { looks = MAX_LOOKS / 2; totalFrames /= 2; totalDropped /= 2; totalCorrupt /= 2; totalSeconds /= 2; for (x in sites.values) { x.frames /= 2; x.dropped /= 2; x.corrupt /= 2; x.seconds /= 2; x.looks /= 2 } }
    }

    fun clear() = synchronized(this) { sites.clear(); looks = 0; totalFrames = 0; totalDropped = 0; totalCorrupt = 0; totalSeconds = 0 }

    /** One Diagnostics line. [policy] = what the engine was told to offer (GeckoEngine.videoPolicyText). */
    @Synchronized fun line(policy: String): String {
        if (looks == 0) return "Real video: nothing played yet this run (the numbers fill in while a video plays on a page); $policy"
        val worst = sites.values.filter { it.frames >= MIN_FRAMES_FOR_WORST && it.worstPct > 0 }.maxByOrNull { it.worstPct }
        val total = percent10(totalDropped, totalFrames)
        return "Real video: $looks looks at ${sites.size} site(s), ${(totalSeconds + 30) / 60} min watched, $totalFrames frames, dropped $total %" +
            (if (totalCorrupt > 0) ", corrupt $totalCorrupt" else "") +
            (if (worst != null) " (worst: ${worst.host} ${worst.worstPct} %${if (worst.worstSize.isNotEmpty()) " at " + worst.worstSize else ""})" else "") + "; $policy"
    }

    companion object {
        const val MAX_SITES = 20
        const val MAX_LOOKS = 200
        const val MAX_FRAMES = 20_000
        const val MAX_SECONDS = 600
        /** A look with fewer frames than this is too short to say anything about the worst moment. */
        const val MIN_FRAMES_FOR_WORST = 60
        private fun percent(d: Long, f: Long): Int = if (f <= 0) 0 else ((d * 100 + f / 2) / f).toInt()
        private fun percent10(d: Long, f: Long): String = if (f <= 0) "0.0" else { val t = (d * 1000 + f / 2) / f; "${t / 10}.${t % 10}" }
    }
}
