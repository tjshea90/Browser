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

    // ---- RX3-V1: a hero loop, hover preview or ad that the page said so about goes under "Other files", however big or long it is

    @Test fun aBigDecoyIsTuckedAwayAndTheRealVideoComesFirst() {
        val hero = file("https://cdn.example/hero.mp4", 40 * MB, h = 1080, dur = 120_000).also { it.role = VideoRole.DECOR }
        val film = file("https://cdn.example/film.mp4", 300 * MB, h = 720, dur = 5_400_000)
        val r = MediaRank.rank(listOf(hero, film))
        assertEquals(listOf(film.url), urls(r.main))
        assertEquals(listOf(hero.url), urls(r.other))
    }

    @Test fun anAdFlaggedByThePageIsTuckedAwayToo() {
        val ad = file("https://cdn.example/pre.mp4", 20 * MB, h = 720, dur = 30_000).also { it.role = VideoRole.AD }
        val film = file("https://cdn.example/film.mp4", 200 * MB)
        assertEquals(listOf(ad.url), urls(MediaRank.rank(listOf(ad, film)).other))
    }

    @Test fun aDecoyTheOwnerTouchedIsNotTuckedAway() {
        val loop = file("https://cdn.example/loop.mp4", 40 * MB, h = 1080, dur = 120_000).also { it.role = VideoRole.DECOR; it.touched = true; it.interest = true }
        val film = file("https://cdn.example/film.mp4", 300 * MB)
        assertTrue(urls(MediaRank.rank(listOf(loop, film)).main).contains(loop.url))
    }

    @Test fun whenTheDecoyIsAllThereIsItStaysOnTopSoTheSheetIsNeverEmpty() {
        val hero = file("https://cdn.example/hero.mp4", 40 * MB).also { it.role = VideoRole.DECOR }
        val r = MediaRank.rank(listOf(hero))
        assertEquals(listOf(hero.url), urls(r.main))
    }

    // ---- RX3-V2: an address nobody has seen (only in the page's text) goes under "Other files"

    @Test fun anAddressFoundOnlyInTheTextIsTuckedAwayNextToARealVideo() {
        val link = MediaCandidate("https://cdn.example/teaser.mp4", MediaKind.FILE, "", -1, page).also { it.fromMarkup = true }
        val film = file("https://cdn.example/film.mp4", 300 * MB)
        val r = MediaRank.rank(listOf(link, film))
        assertEquals(listOf(film.url), urls(r.main)); assertEquals(listOf(link.url), urls(r.other))
    }

    @Test fun aLoneUnverifiedAddressStaysOnTopSoTheSheetIsNeverEmpty() {
        val link = MediaCandidate("https://cdn.example/teaser.mp4", MediaKind.FILE, "", -1, page).also { it.fromMarkup = true }
        assertEquals(listOf(link.url), urls(MediaRank.rank(listOf(link)).main))
    }

    // ---- RX3-V5: a stream whose playlist was read and cannot be saved (live, protected) or is a clip goes under "Other files"

    @Test fun aLiveStreamAndAnAdClipGoUnderOtherFilesNextToTheRealOne() {
        val live = hls("https://cdn.example/live.m3u8").also { it.looked = true; it.live = true }
        val clip = hls("https://cdn.example/ad.m3u8").also { it.looked = true; it.durationMs = 15_000 }
        val film = hls("https://cdn.example/film.m3u8").also { it.looked = true; it.durationMs = 5_400_000 }
        val r = MediaRank.rank(listOf(live, clip, film))
        assertEquals(listOf(film.url), urls(r.main)); assertEquals(setOf(live.url, clip.url), urls(r.other).toSet())
    }

    // ---- RX3-V3: the file that really plays comes first

    @Test fun thePlayedPrimaryFileIsOnTopEvenWhenAnotherFileIsBigger() {
        val big = file("https://cdn.example/other.mp4", 500 * MB, h = 1080, dur = 600_000)
        val film = file("https://cdn.example/film.mp4", 120 * MB, h = 480, dur = 3_000_000).also { it.played = true; it.areaPx = 100_000 }
        val r = MediaRank.rank(listOf(big, film), primary = film)
        assertEquals(film.url, r.main.first().url)
        assertTrue(urls(r.main).contains(big.url))
    }

    @Test fun withoutAPlayedFileTheOrderIsAsBefore() {
        val a = file("https://cdn.example/a.mp4", 100 * MB, h = 480, dur = 600_000); val b = file("https://cdn.example/b.mp4", 300 * MB, h = 1080, dur = 600_000)
        assertEquals(listOf(b.url, a.url), urls(MediaRank.rank(listOf(a, b)).main))
    }

    // ---- RX3-V6: soft words (vast, creative, bumper, intro, hero, trailer ...) count only when the file is ALSO clearly smaller than the biggest one

    @Test fun aSoftWordAloneDoesNotHideAFileButASoftWordAndAClearlySmallerSizeDoes() {
        val film = file("https://cdn.example/v/film.mp4", 100 * MB)
        assertEquals("a trailer of 60 MB is as big as a film: it stays", setOf(film.url, "https://cdn.example/v/trailer.mp4"), urls(MediaRank.rank(listOf(film, file("https://cdn.example/v/trailer.mp4", 60 * MB))).main).toSet())
        val r = MediaRank.rank(listOf(film, file("https://cdn.example/v/trailer.mp4", 20 * MB), file("https://adserver.example/vast/creative/77.mp4", 15 * MB), file("https://cdn.example/v/intro.mp4", 6 * MB)))
        assertEquals(listOf(film.url), urls(r.main))
        assertEquals(3, r.other.size)
    }

    @Test fun aSoftWordNeverHidesTheOnlyOrTheBiggestVideoNorAFileOfUnknownSize() {
        val intro = file("https://cdn.example/v/intro.mp4", 40 * MB)
        assertEquals(listOf(intro.url), urls(MediaRank.rank(listOf(intro)).main))
        val unknown = file("https://cdn.example/v/hero.mp4", -1)
        assertEquals(setOf(intro.url, unknown.url), urls(MediaRank.rank(listOf(intro, unknown)).main).toSet())
    }

    @Test fun aSoftWordNamedFileThatAPlayerPlayedOrThatTheOwnerShowedInterestInStays() {
        val film = file("https://cdn.example/v/film.mp4", 100 * MB)
        val trailer = file("https://cdn.example/v/trailer.mp4", 20 * MB).also { it.touched = true }                                   // a visible player reported it
        val intro = file("https://cdn.example/v/intro.mp4", 10 * MB).also { it.played = true }
        val loop = file("https://cdn.example/v/loop.mp4", 8 * MB).also { it.interest = true; it.touched = true }                      // the owner pressed it
        assertEquals(setOf(film.url, trailer.url, intro.url, loop.url), urls(MediaRank.rank(listOf(film, trailer, intro, loop)).main).toSet())
    }

    @Test fun aSoftWordIsAWholeWordOfThePathOrTheQuery() {
        val film = file("https://cdn.example/v/film.mp4", 100 * MB)
        for (name in listOf("introduction.mp4", "heroes.mp4", "looping.mp4", "backgrounds.mp4", "creatively.mp4", "bgm.mp4")) {
            assertTrue("$name is not a soft word", urls(MediaRank.rank(listOf(film, file("https://cdn.example/v/$name", 20 * MB))).main).contains("https://cdn.example/v/$name"))
        }
        for (u in listOf("https://cdn.example/v/clip.mp4?ad_type=2", "https://cdn.example/v/clip.mp4?x=1&imasdk=1", "https://cdn.example/bg/clip.mp4", "https://cdn.example/v/idle-loop.mp4", "https://cdn.example/v/bumper_12.mp4", "https://cdn.example/v/vpaid.mp4", "https://cdn.example/v/adunit.mp4?adid=5")) {
            assertTrue("$u is a soft word and 20 MB next to 100 MB", urls(MediaRank.rank(listOf(film, file(u, 20 * MB))).other).contains(u))
        }
    }
}
