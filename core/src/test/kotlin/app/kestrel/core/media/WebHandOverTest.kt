package app.kestrel.core.media

import app.kestrel.core.media.WebHandOver.Element
import app.kestrel.core.media.WebHandOver.Known
import app.kestrel.core.media.WebHandOver.Why
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** ANS54e: which films a page hands to Kestrel's own player (owner message 54: "I want the web video players to use this"). */
class WebHandOverTest {
    private fun el(src: String = "", blob: Boolean = false, mime: String = "", loop: Boolean = false, w: Int = 640, h: Int = 360, t: Double = 0.0, d: Double = 0.0, ad: Boolean = false) = Element(src, blob, mime, loop, w, h, t, d, ad)
    private val film = Known("https://v.example/film/master.m3u8", MediaKind.HLS, 600_000L, true, master = true)
    private val unknownLength = Known("https://v.example/other/master.m3u8", MediaKind.HLS, 0L, true, master = true)

    @Test fun aMasterBeatsTheRenditionListsNextToIt() {
        // Yahoo / Brightcove (owner message 55: the player showed the time and a black picture): the master and, next to it, the lists of the picture, the sound and the subtitles, every one as long as the film. The
        // shortest address used to win that tie and Kestrel's player was given the subtitles.
        val master = Known("https://edge.api.example/playback/v1/videos/6408451794112/master.m3u8?token=abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnop", MediaKind.HLS, 163_000L, true, master = true, seenAt = 100)
        val video = Known("https://manifest.example/v5/a/b/6s/rendition.m3u8?t=1", MediaKind.HLS, 163_000L, true, seenAt = 101)
        val sound = Known("https://manifest.example/v5/a/c/6s/rendition.m3u8?t=2", MediaKind.HLS, 163_000L, true, seenAt = 102)
        val subs = Known("https://manifest.example/v5/a/d/rendition.m3u8?t=3", MediaKind.HLS, 163_000L, true, seenAt = 103)
        val d = WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(master, video, sound, subs))!!
        assertEquals(master.url, d.url); assertEquals(true, d.note.contains("master"))
        assertEquals("the order they were found in does not matter", master.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(subs, sound, video, master))!!.url)
        assertNull("only renditions known and no master: several bare lists are no film", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(video, sound, subs)))
        assertNull("a bare list that comes late is never taken (it may be the first of several)", WebHandOver.decide(WebHandOver.ALWAYS, Why.LATE, el(blob = true), listOf(video)))
        assertEquals("a bare list that is the only list is the film (a page that plays a media list directly)", video.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(video))!!.url)
    }

    @Test fun aPlayerThatMovesOnToAnotherVideoGetsTheNewestMaster() {
        val a = Known("https://v.example/a/master.m3u8", MediaKind.HLS, 163_000L, true, master = true, seenAt = 100)
        val b = Known("https://v.example/b/master.m3u8", MediaKind.HLS, 90_000L, true, master = true, seenAt = 200)
        assertEquals(b.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(a, b))!!.url)
        assertEquals(b.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(b, a))!!.url)
        val ad = Known("https://v.example/ad/master.m3u8", MediaKind.HLS, 15_000L, true, master = true, seenAt = 300)
        assertEquals("a newer advertisement is not the film", b.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(a, b, ad))!!.url)
        val aAgain = Known(a.url, MediaKind.HLS, 163_000L, true, master = true, seenAt = 400)
        assertEquals("the first video asked for again is the newest news", a.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true), listOf(aAgain, b))!!.url)
    }

    @Test fun theAddressOfAnUnreadPlaylistMayHintAtAMaster() {
        assertEquals(true, WebHandOver.looksLikeMaster("https://x.example/hls/master.m3u8?a=1"))
        assertEquals(true, WebHandOver.looksLikeMaster("https://x.example/master/index.m3u8"))
        assertEquals(true, WebHandOver.looksLikeMaster("https://x.example/a_master_b.m3u8"))
        assertEquals("only the path says it, not the query", false, WebHandOver.looksLikeMaster("https://x.example/rendition.m3u8?name=master"))
        assertEquals("master2, master_hd, masterplaylist: the name starts with master", true, WebHandOver.looksLikeMaster("https://x.example/hls/master2.m3u8") && WebHandOver.looksLikeMaster("https://x.example/hls/master_hd.m3u8") && WebHandOver.looksLikeMaster("https://x.example/hls/masterplaylist.m3u8"))
        assertEquals("the host does not count", false, WebHandOver.looksLikeMaster("https://master.example/hls/a.m3u8"))
    }

    @Test fun aHandOverNeedsAFreshTapOfThePerson() {
        assertEquals(15_000L, WebHandOver.pressWindowMs(Why.PRESS)); assertEquals(15_000L, WebHandOver.pressWindowMs(Why.PLAY)); assertEquals(15_000L, WebHandOver.pressWindowMs(Why.KESTREL))
        assertEquals(30_000L, WebHandOver.pressWindowMs(Why.FULLSCREEN)); assertEquals(30_000L, WebHandOver.pressWindowMs(Why.LATE))
    }

    @Test fun aFilmPressedAgainContinuesWhereThePersonStopped() {
        val u = "https://cdn.example/clip.mp4"
        assertEquals(118_000L, WebHandOver.resumeMs(0L, u, u, 120_000L, 600_000L, 60_000L))
        assertEquals("another film", 0L, WebHandOver.resumeMs(0L, u, "https://cdn.example/other.mp4", 120_000L, 600_000L, 60_000L))
        assertEquals("too long ago", 0L, WebHandOver.resumeMs(0L, u, u, 120_000L, 600_000L, WebHandOver.RESUME_WINDOW_MS + 1))
        assertEquals("the page itself is somewhere else: that wins", 40_000L, WebHandOver.resumeMs(40_000L, u, u, 120_000L, 600_000L, 60_000L))
        assertEquals("it played to its end: from the start again", 0L, WebHandOver.resumeMs(0L, u, u, 595_000L, 600_000L, 60_000L))
        assertEquals("barely started", 0L, WebHandOver.resumeMs(0L, u, u, 3_000L, 600_000L, 60_000L))
        assertEquals("nothing was stopped yet", 0L, WebHandOver.resumeMs(0L, u, "", 0L, 0L, -1L))
        assertEquals("the length is not known: it still continues", 58_000L, WebHandOver.resumeMs(0L, u, u, 60_000L, 0L, 5_000L))
    }

    @Test fun aDirectFileIsHandedOverFromWhereThePageIs() {
        val d = WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/clip.mp4", t = 12.5, d = 300.0), emptyList())!!
        assertEquals("https://cdn.example/clip.mp4", d.url); assertEquals(MediaKind.FILE, d.kind); assertEquals(12_500L, d.startMs)
    }

    @Test fun anHlsAddressOfTheElementIsHandedOver() {
        val d = WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(src = "https://cdn.example/a/index.m3u8?x=1"), emptyList())!!
        assertEquals(MediaKind.HLS, d.kind); assertEquals(0L, d.startMs)
    }

    @Test fun aScriptPlayerGetsTheStreamItsFrameAskedFor() {
        val d = WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(film))!!
        assertEquals(film.url, d.url); assertEquals(MediaKind.HLS, d.kind)
    }

    @Test fun aScriptPlayerWithNoKnownStreamIsLeftToThePage() {
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), emptyList()))
        assertNull("a DRM or decoy stream is no stream", WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(Known(film.url, MediaKind.HLS, 600_000L, false))))
        assertNull("DASH has no module in the APK", WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(Known("https://v.example/a.mpd", MediaKind.DASH, 600_000L, true))))
    }

    @Test fun aLoneShortStreamIsTakenAndWithOthersTheLongestWins() {
        val ad = Known("https://v.example/ad.m3u8", MediaKind.HLS, 15_000L, true)
        assertEquals("nothing else to choose", ad.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(ad))!!.url)
        assertEquals(film.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(ad, film))!!.url)
        assertEquals("a stream whose length is not read yet beats a short one", unknownLength.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(ad, unknownLength))!!.url)
    }

    @Test fun aStreamWhoseLengthIsNotReadYetCounts() {
        assertEquals(unknownLength.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(unknownLength))!!.url)
    }

    @Test fun theLongestFilmWins() {
        val longer = Known("https://v.example/long/master.m3u8", MediaKind.HLS, 5_400_000L, true, master = true)       // (found at the same moment as the others: the longest then)
        assertEquals(longer.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(blob = true), listOf(film, longer, unknownLength))!!.url)
    }

    @Test fun anAdvertisementInTheSameElementGivesWayToTheFilm() {
        val ad = el(src = "https://ads.example/spot.mp4", d = 15.0, t = 3.0)
        val d = WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, ad, listOf(film))!!
        assertEquals(film.url, d.url); assertEquals("the ad's position is not the film's", 0L, d.startMs)
        assertEquals("with no film known the file itself is played", "https://ads.example/spot.mp4", WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, ad, emptyList())!!.url)
        assertEquals("a short stream beside a short file is no film: the file", "https://ads.example/spot.mp4", WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, ad, listOf(Known("https://v.example/clip.m3u8", MediaKind.HLS, 20_000L, true)))!!.url)
    }

    @Test fun loopClipsAndTinyBoxesAreNeverHandedOver() {
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/a.mp4", loop = true), emptyList()))
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/a.mp4", w = 160, h = 90), emptyList()))
        assertNotNull("a box that is not laid out yet (0 x 0) is no thumbnail", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/a.mp4", w = 0, h = 0), emptyList()))
    }

    @Test fun theSettingDecides() {
        val e = el(src = "https://cdn.example/a.mp4")
        assertNull(WebHandOver.decide(WebHandOver.NEVER, Why.PLAY, e, emptyList()))
        assertNull("full screen only: a press is not full screen", WebHandOver.decide(WebHandOver.FULLSCREEN, Why.PLAY, e, emptyList()))
        assertNotNull(WebHandOver.decide(WebHandOver.FULLSCREEN, Why.FULLSCREEN, e, emptyList()))
        assertNotNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.FULLSCREEN, e, emptyList()))
        assertNotNull("an unknown number is the default", WebHandOver.decide(77, Why.PLAY, e, emptyList()))
    }

    @Test fun addressesThatAreNoPlayableFileAreLeftToThePage() {
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(src = "https://cdn.example/a.mpd"), emptyList()))
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(src = "https://cdn.example/watch?id=7"), emptyList()))
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(src = "file:///sdcard/a.mp4"), emptyList()))
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(src = "blob:https://x.example/1234"), emptyList()))
        assertNotNull("no extension but a video type named by <source>", WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(src = "https://cdn.example/watch?id=7", mime = "video/mp4; codecs=avc1"), emptyList()))
    }

    @Test fun anElementWithNoSourceAtAllFollowsTheFilmOnlyAtFullScreenOrLate() {
        assertNull(WebHandOver.decide(WebHandOver.ALWAYS, Why.PLAY, el(), listOf(film)))
        assertEquals(film.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.FULLSCREEN, el(), listOf(film))!!.url)
        assertEquals(film.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.LATE, el(), listOf(film))!!.url)
    }

    // ---- ANS61 (owner message 61: "it played an advertisement video and I couldn't get to the real video")

    @Test fun anAdvertisementOnThePagesElementIsLeftToThePage() {
        // the pre-roll plays on the page's own element: the address at the press is the ad's, the page's player says an ad plays, and no film is known yet
        val ad = "https://cdn.example/media/spot-30.mp4"
        assertNull("the player says an ad plays: the page plays it itself", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = ad, d = 30.0, ad = true), emptyList()))
        assertNull("a 90 s ad is no longer 'short': only the page's own word counts", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = ad, d = 90.0, ad = true), emptyList()))
        assertNull("a script player (blob) in an ad state and no film known", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true, ad = true), emptyList()))
        assertNull("a script player in an ad state and only a SHORT list known: that list is the ad", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true, ad = true), listOf(Known("https://v.example/ad/master.m3u8", MediaKind.HLS, 20_000L, true, master = true))))
        assertNull("full screen with an ad state and a short list", WebHandOver.decide(WebHandOver.ALWAYS, Why.FULLSCREEN, el(ad = true), listOf(Known("https://v.example/ad/master.m3u8", MediaKind.HLS, 20_000L, true, master = true))))
        assertNotNull("the same element without the ad state is handed over as before", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/media/film.mp4", d = 5400.0), emptyList()))
    }

    @Test fun anAdvertisementWithAFilmKnownGivesTheFilm() {
        val ad = "https://cdn.example/media/spot-30.mp4"
        val d = WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = ad, d = 30.0, ad = true), listOf(film))!!
        assertEquals("the ad is not played: the long film of the frame is", film.url, d.url)
        assertEquals(film.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/ads/spot.mp4"), listOf(film))!!.url)
        assertEquals(film.url, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(blob = true, ad = true), listOf(film))!!.url)
    }

    @Test fun anAddressThatNamesAnAdvertisementIsNotHandedOver() {
        for (u in listOf("https://cdn.example/ads/spot.mp4", "https://cdn.example/media/ad/1234.mp4", "https://cdn.example/video_ad_15.mp4", "https://cdn.example/x/preroll-30.mp4", "https://cdn.example/x/pre-roll.mp4",
            "https://cdn.example/advert/1.mp4", "https://cdn.example/adserver/v/1.mp4", "https://cdn.example/a.mp4?vast=1", "https://cdn.example/imasdk/a.mp4", "https://cdn.example/v.mp4?adunit=3", "https://cdn.example/ADS/Spot.MP4")) {
            assertEquals(u, true, WebHandOver.looksLikeAdAddress(u))
            assertNull(u, WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = u, d = 25.0), emptyList()))
        }
        for (u in listOf("https://cdn.example/movies/ad-astra-2019.mp4", "https://cdn.example/film.mp4?ad=0", "https://ads.example.com/film.mp4", "https://cdn.example/shadow/bread.mp4", "https://cdn.example/uploads/head.mp4",
            "https://cdn.example/loading/film_adventure.mp4", "https://cdn.example/Madrid.mp4", "https://cdn.example/")) {
            assertEquals(u, false, WebHandOver.looksLikeAdAddress(u))
        }
        assertNotNull("a film called ad-astra is still handed over", WebHandOver.decide(WebHandOver.ALWAYS, Why.PRESS, el(src = "https://cdn.example/movies/ad-astra-2019.mp4", d = 7000.0), emptyList()))
    }

    @Test fun aClipKestrelsPlayerPlayedToItsEndIsNotHandedOverAgain() {
        val ad = "https://cdn.example/media/spot-30.mp4"
        // the ad ended in Kestrel's player (position = length), the person pressed play again on the page two minutes later: the page plays it itself, so its ad plugin can finish and go on to the film
        assertEquals(true, WebHandOver.playedThrough(ad, ad, 30_000L, 30_000L, 120_000L))
        assertEquals("a token that is new in every ad request does not make it another clip", true, WebHandOver.playedThrough("$ad?tok=2", "$ad?tok=1", 29_500L, 30_000L, 5_000L))
        assertEquals("stopped half way: the person left it, not the end of an ad", false, WebHandOver.playedThrough(ad, ad, 12_000L, 30_000L, 5_000L))
        assertEquals("another clip", false, WebHandOver.playedThrough(ad, "https://cdn.example/media/other.mp4", 30_000L, 30_000L, 5_000L))
        assertEquals("a film (over two minutes) watched to its end and pressed again is still Kestrel's", false, WebHandOver.playedThrough(ad, ad, 600_000L, 600_000L, 5_000L))
        assertEquals("long ago", false, WebHandOver.playedThrough(ad, ad, 30_000L, 30_000L, 11 * 60_000L))
        assertEquals("never stopped", false, WebHandOver.playedThrough(ad, ad, 30_000L, 30_000L, -1L))
        assertEquals("length unknown", false, WebHandOver.playedThrough(ad, ad, 30_000L, 0L, 5_000L))
        assertEquals("no address", false, WebHandOver.playedThrough("", "", 30_000L, 30_000L, 5_000L))
    }

    @Test fun aPublicPageCannotPointKestrelsPlayerAtTheOwnersNetwork() {
        // RVW3 SC1: the film's address comes from the page; the player is the APP (no engine local-network blocking)
        val page = "https://site.example/watch/1"
        for (film in listOf("http://192.168.1.1/cgi-bin/reboot.mp4", "http://127.0.0.1:8080/x.mp4", "http://localhost/x.mp4", "http://nas.local/v.mp4", "http://10.0.0.5/live.m3u8",
            "http://[::ffff:192.168.1.1]/x.mp4", "http://[::1]/x.mp4", "http://127.1/x.mp4", "http://0x7f.0.0.1/x.mp4", "http://2130706433/x.mp4", "http://169.254.169.254/latest/x.mp4", "not a url", ""))
            assertEquals("refused: $film", true, WebHandOver.hostRefused(page, film))
        for (film in listOf("https://cdn.example.com/v/1.mp4", "http://8.8.8.8/x.mp4", "https://[2606:4700:4700::1111]/a.m3u8", "https://v.example/film/master.m3u8"))
            assertEquals("allowed: $film", false, WebHandOver.hostRefused(page, film))
        assertEquals("a page on the router may play its own files", false, WebHandOver.hostRefused("http://192.168.1.1/player.html", "http://192.168.1.1/media/film.mp4"))
        assertEquals("a NAS page may play NAS files", false, WebHandOver.hostRefused("http://nas.local/app", "http://nas.local/v/film.mp4"))
        assertEquals("no page known counts as public", true, WebHandOver.hostRefused("", "http://192.168.1.1/x.mp4"))
    }
}
