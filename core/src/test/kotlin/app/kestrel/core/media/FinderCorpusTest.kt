package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RX3-V7 (research/REVIEW-EXTERNAL-3.md V7, research/FINDER_LAB.md section 6): a corpus of pages as the video finder sees them (what the sniffer reported, what the page's own elements said, what the early look read)
 * with the files a person really wants marked, and two numbers per page: PRECISION (real videos counted / videos counted) and RECALL (real videos counted / real videos). What is counted is what the dock badge
 * counts. A change that lowers recall on any page is rejected here; a change that lowers precision below the recorded floor is too. The table goes to build/finder-corpus.txt (copied into FINDER_LAB.md when it changes).
 * The pages are MODELS of what real sites do (the sandbox gets no real site's ads): they pin the decisions V1-V6 were built for, they do not measure real sites.
 */
class FinderCorpusTest {
    private val MB = 1L shl 20
    private val page = "https://site.test/watch"
    private fun file(url: String, len: Long, mime: String = "video/mp4") = MediaCandidate(url, MediaKind.FILE, mime, len, page)
    private fun hls(url: String) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, page)
    private fun MediaList.read(url: String, ms: Long, vararg heights: Int, live: Boolean = false, drm: Boolean = false) { applyStream(url, live, drm, ms, 0, 0, heights) }
    /** The page's own element says: "this is a [role]" (the metadata report), optionally that it really plays. */
    private fun MediaList.element(url: String, role: VideoRole, ms: Long, el: Int, played: Boolean = false, area: Long = 100_000) { applyInfo(url, 0, 0, ms, null, role, played, el, area) }

    private class Case(val name: String, val real: Set<String>, val minPrecision: Double, val fill: MediaList.() -> Unit)

    private val F = "https://cdn.test/film.mp4"
    private val H = "https://cdn.test/film.m3u8"
    private val AD = "https://ads.test/pre.m3u8"

    private val cases = listOf(
        Case("plain page: a film, a tracking script, an ad clip", setOf(F), 1.0) {
            add(file("https://site.test/track.php?id=3", 300 * 1024)); add(file(F, 48 * MB)); add(file("https://ads.test/ad.mp4", 2 * MB))
        },
        Case("a feed: the film and twelve hover previews", setOf(F), 1.0) {
            add(file(F, 80 * MB)); element(F, VideoRole.PLAYER, 600_000, 1, played = true)
            for (i in 1..12) { val u = "https://cdn.test/prev$i.mp4"; add(file(u, 3 * MB)); element(u, VideoRole.DECOR, 4_000, 10 + i) }
        },
        Case("a hero loop over the page and a player", setOf(F), 1.0) {
            add(file("https://cdn.test/hero.mp4", 30 * MB)); element("https://cdn.test/hero.mp4", VideoRole.DECOR, 20_000, 1)
            add(file(F, 90 * MB)); element(F, VideoRole.PLAYER, 900_000, 2, played = true)
        },
        Case("two links in the page text: a teaser and a real file (nothing played)", setOf("https://cdn.test/big.mp4"), 1.0) {
            add(file("https://cdn.test/teaser.mp4", -1).also { it.fromMarkup = true })
            add(file("https://cdn.test/big.mp4", 700 * 1024).also { it.fromMarkup = true })
        },
        Case("hls.js page: an ad playlist then the film on ONE blob player", setOf(H), 1.0) {
            add(hls(AD)); add(hls(H)); read(AD, 45_000, 360); read(H, 5_400_000, 360, 720)
            applyBlob(360, 45_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 100_000)
            applyBlob(720, 5_400_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 100_000)
        },
        Case("a trailer playlist next to a feature, nobody played anything yet", setOf(H), 1.0) {
            add(hls("https://cdn.test/trailer.m3u8")); add(hls(H)); read("https://cdn.test/trailer.m3u8", 50_000, 720); read(H, 5_400_000, 720)
        },
        Case("a live stream and a film", setOf(H), 1.0) {
            add(hls("https://cdn.test/live.m3u8")); add(hls(H)); read("https://cdn.test/live.m3u8", 0, live = true); read(H, 3_600_000, 720)
        },
        Case("a protected film only (nothing can be saved)", emptySet(), 1.0) {
            add(hls(H)); read(H, 5_400_000, 1080, drm = true)
        },
        Case("a two-part film: two long playlists", setOf("https://cdn.test/part1.m3u8", "https://cdn.test/part2.m3u8"), 1.0) {
            add(hls("https://cdn.test/part1.m3u8")); add(hls("https://cdn.test/part2.m3u8")); read("https://cdn.test/part1.m3u8", 2_400_000, 720); read("https://cdn.test/part2.m3u8", 2_300_000, 720)
        },
        Case("a short clip site: ONE 45 s playlist and nothing to compare it with", setOf("https://cdn.test/short.m3u8"), 1.0) {
            add(hls("https://cdn.test/short.m3u8")); read("https://cdn.test/short.m3u8", 45_000, 720)
        },
        Case("soft words: an intro, a trailer file and the film", setOf(F), 1.0) {
            add(file("https://cdn.test/v/intro.mp4", 3 * MB)); add(file("https://cdn.test/v/trailer.mp4", 20 * MB)); add(file(F, 100 * MB))
        },
        Case("an advertising server's creative next to the film", setOf(F), 1.0) {
            add(file("https://adserver.test/vast/creative/77.mp4", 15 * MB)); add(file(F, 100 * MB))
        },
        Case("two real videos on one page (a series page)", setOf("https://cdn.test/ep1.mp4", "https://cdn.test/ep2.mp4"), 1.0) {
            add(file("https://cdn.test/ep1.mp4", 60 * MB)); add(file("https://cdn.test/ep2.mp4", 55 * MB))
        },
        Case("a blob player whose playlist was read AFTER the player reported (the slow tablet)", setOf(H), 1.0) {
            add(hls(AD)); add(hls(H))
            applyBlob(720, 5_400_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 100_000)
            read(AD, 45_000, 360); read(H, 5_400_000, 360, 720)
        },
    )

    private fun counted(l: MediaList): List<MediaCandidate> = l.usable().filter { it.counts() && !MediaList.clipNextToFilm(it, l.usable()) }

    @Test fun everyPageKeepsItsRecallAndItsPrecisionFloor() {
        val table = StringBuilder("%-82s %9s %9s %s\n".format("page", "precision", "recall", "counted"))
        var sumP = 0.0; var sumR = 0.0
        val problems = ArrayList<String>()
        for (c in cases) {
            val l = MediaList(); c.fill(l)
            val shown = counted(l).map { it.url }
            val hit = shown.count { it in c.real }
            val precision = if (shown.isEmpty()) (if (c.real.isEmpty()) 1.0 else 0.0) else hit.toDouble() / shown.size
            val recall = if (c.real.isEmpty()) 1.0 else c.real.count { it in shown }.toDouble() / c.real.size
            sumP += precision; sumR += recall
            table.append("%-82s %9.2f %9.2f %d of %d\n".format(c.name.take(82), precision, recall, shown.size, l.size))
            if (recall < 1.0) problems.add("${c.name}: recall $recall (a real video is not counted)")
            if (precision < c.minPrecision) problems.add("${c.name}: precision $precision is under the floor ${c.minPrecision}")
            // the top of the sheet is a real video whenever there is one
            if (c.real.isNotEmpty()) {
                val top = MediaRank.rank(l.usable(), null, l.primary()).main.firstOrNull()?.url
                if (top !in c.real) problems.add("${c.name}: the top of the sheet is $top")
            }
        }
        table.append("%-82s %9.2f %9.2f\n".format("MEAN over ${cases.size} pages", sumP / cases.size, sumR / cases.size))
        File("build").mkdirs(); File("build/finder-corpus.txt").writeText(table.toString())
        assertTrue("the finder corpus:\n$table\n${problems.joinToString("\n")}", problems.isEmpty())
        assertEquals(1.0, sumR / cases.size, 1e-9)
    }
}
