package app.kestrel.core.media

import app.kestrel.core.filter.Psl
import java.net.URI

/**
 * RX3-V5 (research/REVIEW-EXTERNAL-3.md part V, research/FINDER_LAB.md section 3): which segments of a stream playlist are advertising, worked out from the playlist alone, BEFORE a byte of the video is fetched. Until now
 * an ad run was dropped only after its first segment was downloaded and the MP4 writer saw another codec setup, and server-side ads from the same encoder passed untouched. Two signals:
 *  1. the playlist's own markers ([Hls.Seg.ad]: CUE-OUT .. CUE-IN, SCTE-35, DATERANGE), found by [AdMarkers];
 *  2. a RUN (the segments between two discontinuities) that comes from another source than the run holding most of the playing time: another registrable domain or another directory, at most two minutes long and
 *     at most a third of what the dominant source holds.
 * It never marks the whole playlist, never marks when no source holds 60 % of the time (a two-part film), and gives the markers up when they cover more than half of the time (a mistake in the playlist).
 * Pure, no Android. A wrong guess leaves part of a video out (the download's note says so), a miss costs some megabytes: both are bounded and neither is silent.
 */
object AdRuns {
    const val MAX_RUN_MS = 120_000L
    const val DOMINANT_SHARE = 0.6
    const val MAX_SKIPPED_SHARE = 0.5

    /** Segment index ranges (inclusive) to leave out, and the playing time they hold. */
    class Result(val ranges: List<IntRange>, val skippedMs: Long) {
        val breaks: Int get() = ranges.size
        fun contains(i: Int): Boolean = ranges.any { i in it }
    }
    val NONE = Result(emptyList(), 0)

    fun find(media: Hls.Media): Result {
        val segs = media.segments
        val total = media.durationMs
        if (segs.size < 2 || total <= 0) return NONE
        val mark = BooleanArray(segs.size)
        // 1. the playlist's own markers; more than half of the time marked is not believed
        var marked = 0L
        for (i in segs.indices) if (segs[i].ad) marked += segs[i].durationMs
        if (marked > 0 && marked <= total * MAX_SKIPPED_SHARE) for (i in segs.indices) if (segs[i].ad) mark[i] = true
        // 2. runs from another source than the one that holds most of the time
        val runs = runsOf(segs)
        if (runs.size > 1) {
            val bySource = HashMap<String, Long>()
            for (r in runs) bySource[r.source] = (bySource[r.source] ?: 0L) + r.ms
            val dominant = bySource.entries.firstOrNull { it.value >= total * DOMINANT_SHARE }
            if (dominant != null) {
                val longest = runs.maxByOrNull { it.ms }
                for (r in runs) {
                    if (r.source == dominant.key || r === longest) continue
                    if (r.ms <= MAX_RUN_MS && r.ms * 3 <= dominant.value) for (i in r.from until r.to) mark[i] = true
                }
            }
        }
        // the ranges; the union may not be most of the playlist and never all of it
        val ranges = ArrayList<IntRange>(); var ms = 0L; var i = 0
        while (i < mark.size) {
            if (!mark[i]) { i++; continue }
            var j = i; while (j + 1 < mark.size && mark[j + 1]) j++
            ranges.add(i..j); for (k in i..j) ms += segs[k].durationMs; i = j + 1
        }
        if (ranges.isEmpty() || ms > total * MAX_SKIPPED_SHARE || ms >= total) return NONE
        return Result(ranges, ms)
    }

    /** The plain sentence for the download's row, or null when nothing was left out: "Left out 1:30 of advertising (2 breaks)". */
    fun note(r: Result): String? {
        if (r.ranges.isEmpty()) return null
        val s = (r.skippedMs + 500) / 1000
        return "Left out ${s / 60}:${(s % 60).toString().padStart(2, '0')} of advertising (${r.breaks} break${if (r.breaks == 1) "" else "s"})"
    }

    /**
     * The playlist as the download should see it: the same segments, but the first segment AFTER each left-out range starts a new run (discontinuity), because the time stamps of what comes back do not
     * continue those of what came before. The segments themselves are not touched: the engine never fetches the ranges.
     */
    fun withBreaks(media: Hls.Media, r: Result): Hls.Media {
        if (r.ranges.isEmpty()) return media
        val after = r.ranges.map { it.last + 1 }.filter { it < media.segments.size }.toSet()
        if (after.isEmpty()) return media
        val segs = media.segments.mapIndexed { i, s -> if (i in after && !s.discontinuity) Hls.Seg(s.url, s.durationMs, s.from, s.len, s.key, s.seq, true, s.ad) else s }
        return Hls.Media(segs, media.keys, media.init, media.endList, media.targetMs, media.mediaSequence, media.problem, media.format)
    }

    private class Run(val from: Int, val to: Int, val ms: Long, val source: String)

    private fun runsOf(segs: List<Hls.Seg>): List<Run> {
        val out = ArrayList<Run>(); var from = 0
        for (i in 1..segs.size) {
            if (i == segs.size || segs[i].discontinuity) {
                var ms = 0L; for (k in from until i) ms += segs[k].durationMs
                out.add(Run(from, i, ms, sourceOf(segs[from].url))); from = i
            }
        }
        return out
    }

    /** "registrable domain|directory with the digits folded": another site, or another place on the same site, is another source. */
    private fun sourceOf(url: String): String = try {
        val u = URI(url)
        val host = (u.host ?: "").lowercase()
        val dir = (u.path ?: "").substringBeforeLast('/').replace(Regex("[0-9]+"), "#")
        (Psl.default.siteOf(host) ?: host) + "|" + dir
    } catch (e: Exception) { "?" }
}
