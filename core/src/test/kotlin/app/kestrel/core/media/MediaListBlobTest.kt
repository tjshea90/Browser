package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RX3-V4 (research/REVIEW-EXTERNAL-3.md V4): a player that feeds a blob: address (hls.js, shaka, dash.js: most embeds) cannot say which playlist it plays. Its LENGTH and picture HEIGHT can
 * match one of the playlists the early look read; a clip next to a film is not what the owner wants.
 */
class MediaListBlobTest {
    private val page = "https://site.test/watch"
    private fun hls(url: String) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, page)
    private fun dash(url: String) = MediaCandidate(url, MediaKind.DASH, "application/dash+xml", -1, page)
    /** A stream the early look has read: [ms] long, with these picture heights in its variants. */
    private fun MediaList.looked(url: String, ms: Long, vararg heights: Int) { applyStream(url, live = false, drm = false, durationMs = ms, adMs = 0, breaks = 0, heights = heights) }
    private fun film(l: MediaList) = l.find("https://c.test/film.m3u8")!!
    private fun clip(l: MediaList) = l.find("https://ads.test/pre.m3u8")!!

    /** An advertising playlist of [clipMs] and a film of five minutes, both read by the early look. */
    private fun twoStreams(clipMs: Long = 15_000): MediaList {
        val l = MediaList()
        l.add(hls("https://ads.test/pre.m3u8")); l.add(hls("https://c.test/film.m3u8"))
        l.looked("https://ads.test/pre.m3u8", clipMs, 360)
        l.looked("https://c.test/film.m3u8", 300_000, 360, 720)
        return l
    }

    @Test fun aBlobPlayerIsMatchedToThePlaylistOfItsLength() {
        val l = twoStreams()
        assertTrue(l.applyBlob(720, 300_400, VideoRole.PLAYER, played = true, element = 1, areaPx = 200_000))
        assertTrue(film(l).played); assertTrue(film(l).touched); assertEquals(1, film(l).element)
        assertFalse(clip(l).played)
        assertEquals("https://c.test/film.m3u8", l.primary()!!.url)
    }

    @Test fun theToleranceIsOneAndAHalfSecondsOrThreePercent() {
        fun matched(filmMs: Long, elementMs: Long): Boolean {
            val l = MediaList(); l.add(hls("https://c.test/film.m3u8")); l.looked("https://c.test/film.m3u8", filmMs, 720)
            l.applyBlob(720, elementMs, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)
            return film(l).played
        }
        assertTrue("a short clip: 1.4 s off", matched(21_000, 22_400))
        assertFalse("a short clip: 1.6 s off", matched(21_000, 22_600))
        assertTrue("a two hour film: 2.9 percent off", matched(7_200_000, 7_200_000 + 208_000))
        assertFalse("a two hour film: 3.2 percent off", matched(7_200_000, 7_200_000 + 231_000))
        assertFalse("an element with no length matches nothing", matched(300_000, 0))
    }

    @Test fun theHeightChoosesBetweenTwoPlaylistsOfTheSameLength() {
        val l = MediaList()
        l.add(hls("https://c.test/a.m3u8")); l.add(hls("https://c.test/b.m3u8"))
        l.looked("https://c.test/a.m3u8", 300_000, 240, 480)
        l.looked("https://c.test/b.m3u8", 300_000, 360, 720)
        l.applyBlob(720, 300_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)
        assertFalse(l.find("https://c.test/a.m3u8")!!.played)
        assertTrue(l.find("https://c.test/b.m3u8")!!.played)
    }

    @Test fun aHeightNoPlaylistListsDoesNotThrowTheMatchAway() {
        val l = twoStreams()                                                    // the film lists 360 and 720; the player decodes a 1080 picture (an adaptive jump the master never listed)
        l.applyBlob(1080, 300_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)
        assertTrue("height is a tie-breaker, never a reason to lose the match", film(l).played)
    }

    @Test fun aPlaylistWithNoKnownHeightsStillMatchesByLength() {
        val l = MediaList(); l.add(hls("https://c.test/film.m3u8")); l.looked("https://c.test/film.m3u8", 300_000)
        l.applyBlob(720, 300_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)
        assertTrue(film(l).played)
    }

    @Test fun aBlobThatReportedBeforeThePlaylistWasReadIsMatchedWhenTheLookArrives() {
        val l = MediaList(); l.add(hls("https://c.test/film.m3u8"))
        assertFalse("nothing read yet: nothing to match", l.applyBlob(720, 300_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 9))
        assertFalse(film(l).played)
        assertTrue(l.applyStream("https://c.test/film.m3u8", live = false, drm = false, durationMs = 300_000, adMs = 0, breaks = 0, heights = intArrayOf(720)))
        assertTrue(film(l).played); assertEquals(1, film(l).element); assertEquals(9L, film(l).areaPx)
    }

    @Test fun aDashManifestMatchesToo() {
        val l = MediaList(); l.add(dash("https://c.test/film.mpd")); l.looked("https://c.test/film.mpd", 600_000, 1080)
        l.applyBlob(1080, 600_200, VideoRole.PLAYER, played = true, element = 2, areaPx = 5)
        assertTrue(l.find("https://c.test/film.mpd")!!.played)
    }

    @Test fun anAdThenTheFilmOnTheSameBlobElementMakesTheAdAnAd() {
        val l = twoStreams(clipMs = 15_000)
        l.applyBlob(360, 15_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)         // the pre-roll plays first
        l.applyBlob(720, 300_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)         // then the film, same element
        assertEquals(VideoRole.AD, clip(l).role)
        assertEquals("https://c.test/film.m3u8", l.primary()!!.url)
    }

    @Test fun aBlobElementThatIsDecorationOrAnAdMakesItsPlaylistDecoration() {
        val l = twoStreams(clipMs = 90_000)                                      // long enough to be counted by itself: only the page's own word 'ad' takes it out
        assertEquals(2, l.usableCount())
        l.applyBlob(360, 90_000, VideoRole.AD, played = true, element = 3, areaPx = 1)
        assertTrue(clip(l).decoy)
        assertNull("a decoy is never the primary file", l.primary())
        assertEquals(1, l.usableCount())
    }

    @Test fun aLiveElementWithNoLengthMatchesNothingAndTheLastReportOfAnElementWins() {
        val l = twoStreams()
        assertFalse(l.applyBlob(720, 0, VideoRole.PLAYER, played = true, element = 1, areaPx = 1))
        l.applyBlob(360, 15_000, VideoRole.PLAYER, played = false, element = 1, areaPx = 1)
        assertTrue("seen playing nothing yet: the clip is only touched", clip(l).touched); assertFalse(clip(l).played)
        l.applyBlob(720, 300_000, VideoRole.PLAYER, played = false, element = 1, areaPx = 1)
        assertTrue(film(l).touched)
    }

    @Test fun theBlobMemoryIsBounded() {
        val l = MediaList()
        for (e in 1..40) l.applyBlob(720, 100_000L + e, VideoRole.PLAYER, played = true, element = e, areaPx = 1)
        assertTrue(l.blobElements() <= 8)
    }

    // ---- a clip next to a film

    @Test fun aPlaylistUnderAMinuteNextToOneOfFiveMinutesOrMoreIsNotCounted() {
        val l = MediaList()
        l.add(hls("https://ads.test/pre.m3u8")); l.add(hls("https://c.test/film.m3u8"))
        l.looked("https://ads.test/pre.m3u8", 45_000, 360)
        assertEquals("before the film is read nothing says the clip is one", 2, l.usableCount())
        l.looked("https://c.test/film.m3u8", 400_000, 720)
        assertEquals(1, l.usableCount())
        val r = MediaRank.rank(l.usable())
        assertEquals(listOf("https://c.test/film.m3u8"), r.main.map { it.url })
        assertEquals(listOf("https://ads.test/pre.m3u8"), r.other.map { it.url })
    }

    @Test fun theClipRuleSparesWhatPlayedWhatTheOwnerTouchedAndSetsOfShortClips() {
        val l = twoStreams(clipMs = 45_000)
        assertEquals("a 45 s playlist next to a 5 minute one is a clip", 1, l.usableCount())
        l.applyBlob(360, 45_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)
        assertEquals("the clip really played: it is counted until the film plays after it on the same element", 2, l.usableCount())
        l.applyBlob(720, 300_000, VideoRole.PLAYER, played = true, element = 1, areaPx = 1)
        assertEquals(1, l.usableCount())
        val m = MediaList()
        m.add(hls("https://ads.test/pre.m3u8")); m.add(hls("https://c.test/film.m3u8"))
        m.looked("https://ads.test/pre.m3u8", 45_000, 360); m.looked("https://c.test/film.m3u8", 400_000, 720)
        assertTrue(m.touch("https://ads.test/pre.m3u8"))
        assertEquals("the owner pressed it: it counts", 2, m.usableCount())
        val s = MediaList()
        s.add(hls("https://c.test/one.m3u8")); s.add(hls("https://c.test/two.m3u8"))
        s.looked("https://c.test/one.m3u8", 45_000, 360); s.looked("https://c.test/two.m3u8", 50_000, 360)
        assertEquals("two short playlists and no film: both stay", 2, s.usableCount())
    }

    @Test fun aLiveOrProtectedLongStreamIsNotTheFilmThatMakesAClipJunk() {
        val l = MediaList()
        l.add(hls("https://ads.test/pre.m3u8")); l.add(hls("https://c.test/film.m3u8"))
        l.looked("https://ads.test/pre.m3u8", 45_000, 360)
        l.applyStream("https://c.test/film.m3u8", live = false, drm = true, durationMs = 400_000, adMs = 0, breaks = 0, heights = intArrayOf(720))
        assertEquals("the long one cannot be saved (protected): the short one is all there is", 1, l.usableCount())
        assertNotNull(l.usable().firstOrNull { it.url == "https://ads.test/pre.m3u8" })
        assertTrue(MediaRank.rank(l.usable()).main.any { it.url == "https://ads.test/pre.m3u8" })
    }
}
