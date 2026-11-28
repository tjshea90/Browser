package app.kestrel.core.media

/**
 * ANS53d.2 (owner message 53: "put a setting for video quality in the player controls for as many videos as possible"): the quality list of a film that plays in the PAGE. The page's own player cannot be told to change
 * its picture (no common interface: hls.js, video.js, Shaka, dash.js, the sites' own), so the list is a hand-over: the pictures the film's HLS master playlist offers, one row each, and a pick starts Kestrel's own
 * player (which CAN choose) at the film's position with that picture. Pure, so the rules are pinned by a test; the Android side only feeds it the playlist and the page's report.
 */
object PageQuality {
    /** The pictures a master playlist offers, one row each, biggest first. A variant without a picture size (audio only, or no RESOLUTION) is not a picture. */
    fun rungs(variants: List<Hls.Variant>): List<QualityLabel.Rung> = QualityLabel.rows(
        variants.filter { !it.audioOnly && (it.width > 0 || it.height > 0) }
            .map { QualityLabel.Rung(it.width, it.height, if (it.frameRate > 0.0) it.frameRate.toFloat() else -1f, if (it.avgBandwidth > 0L) it.avgBandwidth else it.bandwidth) })

    /** A list is only worth a button when there is something to choose between. */
    fun hasChoice(rungs: List<QualityLabel.Rung>): Boolean = rungs.size >= 2

    /**
     * Where Kestrel's own player starts: the page's position, but only when the page is playing THIS stream. The page reports the length of what it plays ([pageLengthS], <= 0 = not known) and the playlist
     * says its own ([streamLengthMs], <= 0 = not known); lengths that differ by more than a tenth (or 10 s) mean something else plays (an advertisement) and the start of the stream is the only safe place.
     */
    fun startMs(timeS: Double, pageLengthS: Double, streamLengthMs: Long): Long {
        if (timeS.isNaN() || timeS <= 0.0) return 0L
        if (pageLengthS > 0.0 && streamLengthMs > 0L) {
            val s = streamLengthMs / 1000.0
            if (Math.abs(s - pageLengthS) > Math.max(10.0, 0.1 * s)) return 0L
        }
        return (timeS * 1000.0).toLong().coerceAtLeast(0L)
    }
}
