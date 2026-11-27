package app.kestrel.browser.web

import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import app.kestrel.core.media.QualityLabel

/**
 * ANS53d / ANS60c (owner messages 53 and 60: "put the video quality controls in the regular player size when it is not in full screen. Right now I can only select quality in full screen"): the quality list of
 * Kestrel's own player, in ONE place for the full-screen screen ([app.kestrel.browser.NativePlayerActivity]) and the player inside the page ([InlinePlayer]). It knows which pictures the stream offers (every video
 * track of every group), what the person picked, the size cap of Automatic, and what the page's own list asked for; the screens draw the [QualitySheet] and call [rows] / [pick].
 * [remembered] / [remember] = where the person's pick is kept for the next films (the full-screen screen and the player inside the page each keep their own: a pick of 1080p in full screen must not make every
 * small box in a page ask for 1080p); [say] = a line for Diagnostics (nothing for a private tab).
 */
class PlayerQuality(
    private val priv: Boolean,
    private val remembered: () -> Int,
    private val remember: (Int) -> Unit,
    private val say: (String, String) -> Unit,
) {
    /** The picture the person picked (null = Automatic). */
    var chosen: QualityLabel.Rung? = null; private set
    /** The tallest picture Automatic may choose (both sides at most 16:9 of it: see [PlayerFactory.applyCap]). */
    var capH = 720; private set
    /** The picture picked in the list of the page's film, applied as soon as the stream says what it offers. */
    private var wanted: QualityLabel.Rung? = null
    private var rowsNow: List<Pair<QualityLabel.Rung, Pair<Tracks.Group, Int>>> = emptyList()

    /**
     * The picture size to start with: the one the person picked last time (remembered), else what the page's list said ([pick] / [auto]), else [maxH]. The cap is on the LONGER side of a 16:9 picture so that a portrait
     * film is held to the same class as a landscape one (a cap on the height alone would drop every portrait rung of a 720p class). A picture picked in the page's quality list wins over what was remembered (and is
     * remembered itself, like a pick made in the player); Automatic from that list forgets the old pick.
     */
    fun begin(p: ExoPlayer, maxH: Int, pick: QualityLabel.Rung?, auto: Boolean) {
        val mem = remembered()
        capH = when {
            pick != null -> QualityLabel.pClass(pick.w, pick.h).coerceIn(144, 2160)
            auto -> maxOf(maxH, 720).coerceIn(144, 2160)
            else -> (if (mem > 0) mem else maxH).coerceIn(144, 2160)
        }
        if (pick != null) { remember(capH); wanted = pick } else if (auto) remember(0)
        if (!priv && (pick != null || auto)) { NativePlayerStats.qualityPicks++; NativePlayerStats.lastQuality = if (pick != null) QualityLabel.text(pick) else "Automatic" }
        say("start-quality", if (pick != null) "picked in the page's list: ${QualityLabel.text(pick)} (cap ${capH}p)" else if (auto) "Automatic, picked in the page's list (cap ${capH}p)" else "no pick (cap ${capH}p)")
        PlayerFactory.applyCap(p, capH)
    }

    /**
     * The stream said what it offers: the pictures, biggest first. true = there is more than one to choose from (a plain file offers one: nothing to choose, no button). A picture the page's list asked for is
     * applied here, the moment the stream offers it.
     */
    fun onTracks(p: ExoPlayer, tracks: Tracks): Boolean {
        val all = ArrayList<Pair<QualityLabel.Rung, Pair<Tracks.Group, Int>>>()
        for (g in tracks.groups) {
            if (g.type != C.TRACK_TYPE_VIDEO) continue
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                if (f.width <= 0 && f.height <= 0) continue
                all.add(Pair(QualityLabel.Rung(f.width, f.height, f.frameRate, (if (f.averageBitrate > 0) f.averageBitrate else f.bitrate).toLong().coerceAtLeast(0L)), Pair(g, i)))
            }
        }
        rowsNow = all.sortedWith(compareByDescending<Pair<QualityLabel.Rung, Pair<Tracks.Group, Int>>> { QualityLabel.pClass(it.first.w, it.first.h) }.thenByDescending { it.first.fps }.thenByDescending { it.first.bps })
        val distinct = QualityLabel.rows(rowsNow.map { it.first }).size
        wanted?.let { w ->
            val idx = QualityLabel.nearest(w, rowsNow.map { it.first })
            if (idx >= 0) { wanted = null; applyRung(p, rowsNow[idx].first, rowsNow[idx].second); say("pick-applied", "the page's pick ${QualityLabel.text(w)} -> ${QualityLabel.text(rowsNow[idx].first)}") }
        }
        say("tracks", "$distinct picture(s) on offer: ${QualityLabel.rows(rowsNow.map { it.first }).joinToString(" | ") { QualityLabel.text(it) }}")
        return distinct >= 2
    }

    /** The rows of the list: Automatic first, then every picture the stream offers (one row per distinct text), a picture this tablet cannot play smoothly greyed out. */
    fun rows(p: ExoPlayer): List<QualitySheet.Row> {
        val rows = ArrayList<QualitySheet.Row>()
        val nowFmt = p.videoFormat
        val now = QualityLabel.now(if (nowFmt != null) QualityLabel.Rung(nowFmt.width, nowFmt.height, nowFmt.frameRate, 0L) else null)
        rows.add(QualitySheet.Row("Automatic", (if (chosen == null) now else "").ifEmpty { "the player picks, at most ${capH}p" }, chosen == null, true, null))
        val seen = HashSet<String>()
        for ((rung, where) in rowsNow) {
            val t = QualityLabel.text(rung)
            if (!seen.add(t)) continue
            val playable = where.first.isTrackSupported(where.second)
            rows.add(QualitySheet.Row(t, if (playable) "" else "this tablet cannot play it smoothly", chosen != null && QualityLabel.text(chosen!!) == t, playable, rung))
        }
        return rows
    }

    /** The person picked [row]: that picture for this film (a track override) and its size class as the cap, remembered for the next films; Automatic lifts the override. */
    fun pick(p: ExoPlayer, row: QualitySheet.Row) {
        val rung = row.tag as? QualityLabel.Rung
        try {
            val b = p.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            if (rung == null) { chosen = null; remember(0); capH = maxOf(capH, 720) }
            else {
                val match = rowsNow.firstOrNull { QualityLabel.text(it.first) == row.title }
                if (match != null) b.setOverrideForType(TrackSelectionOverride(match.second.first.mediaTrackGroup, listOf(match.second.second)))
                chosen = rung
                val cls = QualityLabel.pClass(rung.w, rung.h)
                remember(cls)
                capH = cls
            }
            p.trackSelectionParameters = b.build()
            PlayerFactory.applyCap(p, capH)
            if (!priv) { NativePlayerStats.qualityPicks++; NativePlayerStats.lastQuality = if (rung == null) "Automatic" else QualityLabel.text(rung) }
            say("quality", if (rung == null) "Automatic" else row.title)
        } catch (e: Throwable) { say("quality-failed", e.javaClass.simpleName) }
    }

    /** ANS53d.2: play exactly this picture of the stream (the one the page's list named, matched against what the stream really offers): a track override, the cap follows it. */
    private fun applyRung(p: ExoPlayer, rung: QualityLabel.Rung, where: Pair<Tracks.Group, Int>) {
        try {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).setOverrideForType(TrackSelectionOverride(where.first.mediaTrackGroup, listOf(where.second))).build()
            chosen = rung
            val cls = QualityLabel.pClass(rung.w, rung.h)
            remember(cls)
            capH = cls
            PlayerFactory.applyCap(p, capH)
        } catch (e: Throwable) { say("quality-failed", e.javaClass.simpleName) }
    }
}
