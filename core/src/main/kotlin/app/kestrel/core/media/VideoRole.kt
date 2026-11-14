package app.kestrel.core.media

/**
 * RX3-V1 (research/REVIEW-EXTERNAL-3.md part V, research/FINDER_LAB.md section 1): what a page's own <video> element is FOR. Before this, every element that loaded its metadata made its file "a real video" (the page's
 * player reported it), so hover previews, muted looping hero videos, GIF-style loops and 15-30 s pre-roll ads stayed in the download badge. Pure logic, no Android: VideoRoleTest.
 */
enum class VideoRole {
    /** Something a person watches: counts as a video the owner may want to save. */
    PLAYER,
    /** Decoration: a hover preview, a hero loop, a GIF-style clip, a thumbnail-sized box. Kept in the list for "Other files", never counted. */
    DECOR,
    /** The page itself says it is an advertisement (an ad-state class or id around the element). Same treatment as [DECOR]. */
    AD,
}

/** What content.js sends with a metadata report. Numbers only; [fromBits] never trusts them (they come from a web page). */
class VideoFacts(
    val muted: Boolean,
    val loop: Boolean,
    val autoplay: Boolean,
    /** The native controls attribute, or a known player's own control bar around the element (the page script decides). */
    val controls: Boolean,
    val visible: Boolean,
    /** An ancestor says "ad" (whole word) or carries an ad-state class of a known player. */
    val ad: Boolean,
    /** The element's rendered box in CSS pixels; 0 = not laid out. */
    val widthPx: Int,
    val heightPx: Int,
    /** Percent of the box that lies inside the visible part of the page, 0..100. */
    val viewPercent: Int,
) {
    companion object {
        const val MUTED = 1
        const val LOOP = 2
        const val AUTOPLAY = 4
        const val CONTROLS = 8
        const val VISIBLE = 16
        const val AD = 32

        fun fromBits(bits: Int, widthPx: Int, heightPx: Int, viewPercent: Int): VideoFacts = VideoFacts(
            muted = bits and MUTED != 0, loop = bits and LOOP != 0, autoplay = bits and AUTOPLAY != 0, controls = bits and CONTROLS != 0,
            visible = bits and VISIBLE != 0, ad = bits and AD != 0,
            widthPx = widthPx.coerceIn(0, 20000), heightPx = heightPx.coerceIn(0, 20000), viewPercent = viewPercent.coerceIn(0, 100),
        )
    }
}

object VideoRoleRules {
    /** A muted autoplaying clip with no controls up to this long is a preview (a news site's silent teaser); longer ones may be the film. */
    const val PREVIEW_MAX_MS = 30_000L
    /** A box smaller than this (area under 160 x 90 CSS pixels) is an icon-sized clip, not something to watch. */
    const val MIN_WIDTH = 160
    const val MIN_HEIGHT = 90

    /** No facts (an older extension, a report from somewhere else): [VideoRole.PLAYER], exactly what was assumed before. */
    fun decide(f: VideoFacts?, durationMs: Long): VideoRole {
        if (f == null) return VideoRole.PLAYER
        if (f.ad) return VideoRole.AD
        if (f.loop && f.muted && !f.controls) return VideoRole.DECOR
        if (f.muted && f.autoplay && !f.controls && durationMs in 1..PREVIEW_MAX_MS) return VideoRole.DECOR
        val area = f.widthPx.toLong() * f.heightPx
        if (area > 0 && area < MIN_WIDTH.toLong() * MIN_HEIGHT) return VideoRole.DECOR
        return VideoRole.PLAYER
    }
}
