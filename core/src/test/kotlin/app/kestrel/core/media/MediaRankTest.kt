package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Owner, 2026-11-12: a long press on a video offered several files, "a couple of small files from php addresses that are obviously not the video". */
class MediaRankTest {
    private val page = "https://site.example/watch/42"
    private fun file(url: String, len: Long, mime: String = "video/mp4", frame: String = "", h: Int = 0, dur: Long = 0): MediaCandidate =
        MediaCandidate(url, MediaKind.FILE, mime, len, page, frame, 0).also { it.height = h; it.durationMs = dur }
    private fun hls(url: String) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, page)
    private fun urls(l: List<MediaCandidate>) = l.map { it.url }
    private val MB = 1L shl 20

    @Test fun smallPhpFilesAreTuckedAwayAndTheVideoComesFirst() {
        val video = file("https://cdn.example/v/movie.mp4", 48 * MB)
        val r = MediaRank.rank(listOf(
            file("https://site.example/track.php?id=3", 300 * 1024), video, file("https://site.example/get.php?x=9", 700 * 1024), file("https://ads.example/pre.php?c=1", 2 * MB)))
        assertEquals(listOf(video.url), urls(r.main))
        assertEquals(3, r.other.size)
        assertNull(r.exact)
    }

    @Test fun allQualitiesOfTheVideoStayAndAreOrderedBestFirst() {
        val r = MediaRank.rank(listOf(
            file("https://cdn.example/v/movie_480p.mp4", 40 * MB, h = 480), file("https://cdn.example/v/movie_1080p.mp4", 150 * MB, h = 1080),
            file("https://cdn.example/v/movie_720p.mp4", 80 * MB, h = 720), file("https://site.example/preview.php?v=1", 3 * MB)))
        assertEquals(listOf("https://cdn.example/v/movie_1080p.mp4", "https://cdn.example/v/movie_720p.mp4", "https://cdn.example/v/movie_480p.mp4"), urls(r.main))
        assertEquals(1, r.other.size)
    }

    @Test fun lowerQualitiesOfTheSameVideoStayEvenWhenTheyAreMuchSmaller() {
        val r = MediaRank.rank(listOf(
            file("https://cdn.example/v/clip_1080p.mp4", 150 * MB, h = 1080), file("https://cdn.example/v/clip_360p.mp4", 12 * MB, h = 360), // 8 % of the big one: a quality, not a clip
            file("https://site.example/track.php?id=3", 300 * 1024), file("https://cdn.example/v/clip_preview.mp4", 2 * MB)))
        assertEquals(listOf("https://cdn.example/v/clip_1080p.mp4", "https://cdn.example/v/clip_360p.mp4"), urls(r.main))
        assertEquals(2, r.other.size)
    }

    @Test fun aScriptEndpointThatIsReallyBigIsStillTheVideo() {
        val r = MediaRank.rank(listOf(file("https://site.example/get_file.php?id=7&token=abc", 60 * MB), file("https://site.example/track.php?x", 200 * 1024)))
        assertEquals(1, r.main.size); assertTrue(r.main[0].url.contains("get_file.php")); assertEquals(1, r.other.size)
    }

    @Test fun aScriptEndpointOfUnknownSizeIsNotTrustedNextToABigFile() {
        val r = MediaRank.rank(listOf(file("https://cdn.example/v/a.mp4", 30 * MB), file("https://site.example/stream.php?id=2", -1)))
        assertEquals(listOf("https://cdn.example/v/a.mp4"), urls(r.main)); assertEquals(1, r.other.size)
    }

    @Test fun theLongPressedElementsOwnFileIsExactAndAloneWithItsOtherQualities() {
        val p720 = file("https://cdn.example/v/movie_720p.mp4", 80 * MB, h = 720)
        val r = MediaRank.rank(listOf(
            file("https://cdn.example/v/movie_1080p.mp4", 150 * MB, h = 1080), p720, file("https://cdn.example/v/other_show_720p.mp4", 90 * MB, h = 720), file("https://site.example/track.php?id=3", 300 * 1024)),
            pressedUrl = "https://cdn.example/v/movie_720p.mp4#t=5")
        assertEquals(p720.url, r.exact?.url)
        assertEquals("the pressed one first, then its other quality", listOf(p720.url, "https://cdn.example/v/movie_1080p.mp4"), urls(r.main))
        assertEquals("an unrelated video of the same size is not offered on top", 2, r.other.size)
    }

    @Test fun twoFilesOfTheSameLengthOfTimeAreTheSameVideo() {
        val a = file("https://cdn1.example/a/9f8e.mp4", 50 * MB, dur = 120_000, h = 720)
        val b = file("https://cdn2.example/b/77aa.mp4", 100 * MB, dur = 120_900, h = 1080)
        val c = file("https://cdn3.example/c/33cc.mp4", 20 * MB, dur = 30_000)
        val r = MediaRank.rank(listOf(a, b, c), pressedUrl = a.url)
        assertEquals(listOf(a.url, b.url), urls(r.main)); assertEquals(listOf(c.url), urls(r.other))
    }

    @Test fun whenNothingWasPressedTheBestGuessIsUsedAndABlobPressChangesNothing() {
        val v = file("https://cdn.example/v/clip.mp4", 25 * MB)
        val r = MediaRank.rank(listOf(v, file("https://site.example/t.php?a", 200 * 1024)), pressedUrl = "blob:https://site.example/1234")
        assertEquals(listOf(v.url), urls(r.main)); assertNull(r.exact)
    }

    @Test fun whenEverythingIsSmallTheBiggestIsStillOffered() {
        val big = file("https://cdn.example/s/short.mp4", 900 * 1024)
        val r = MediaRank.rank(listOf(file("https://cdn.example/s/a.mp4", 200 * 1024), big, file("https://site.example/b.php?z", 300 * 1024)))
        assertTrue("never an empty sheet", r.main.isNotEmpty()); assertEquals(big.url, r.main[0].url)
    }

    @Test fun pieceParametersAndTokensDoNotMakeNewFiles() {
        val r = MediaRank.rank(listOf(
            file("https://cdn.example/v/movie.mp4?id=7&bytestart=0&byteend=999", 40 * MB), file("https://cdn.example/v/movie.mp4?id=7&bytestart=1000&byteend=1999", 40 * MB), file("https://cdn.example/v/movie.mp4?id=8", 40 * MB)))
        assertEquals(2, r.main.size)
    }

    @Test fun streamsStayOnTopOfTheListWithTheFiles() {
        val r = MediaRank.rank(listOf(file("https://cdn.example/v/a.mp4", 30 * MB), hls("https://cdn.example/hls/master.m3u8")))
        assertEquals(2, r.main.size); assertEquals(MediaKind.HLS, r.main[0].kind)
    }

    @Test fun anAdvertFramesSmallClipIsHiddenNextToThePagesOwnVideo() {
        val v = file("https://cdn.example/v/a.mp4", 60 * MB)
        val r = MediaRank.rank(listOf(v, file("https://adsrv.example/c/clip9.mp4", 10 * MB, frame = "https://adsrv.example/frame")))
        assertEquals(listOf(v.url), urls(r.main)); assertEquals(1, r.other.size)
    }

    @Test fun aJingleNextToAVideoIsTuckedAway() {
        val r = MediaRank.rank(listOf(file("https://cdn.example/v/a.mp4", 60 * MB), file("https://cdn.example/s/ding.mp3", 400 * 1024, mime = "audio/mpeg")))
        assertEquals(1, r.main.size); assertEquals(1, r.other.size)
    }

    @Test fun emptyInIsEmptyOut() {
        val r = MediaRank.rank(emptyList(), "https://x.example/a.mp4")
        assertTrue(r.main.isEmpty() && r.other.isEmpty()); assertFalse(r.exact != null)
    }
}
