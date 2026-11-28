package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** NM1: the owner's case: doraby's Ozark page is titled "Ozark" whatever episode is on; the episode is in the player frame's address and in the page's list. */
class EpisodeCluesTest {
    private val title = "Watch Ozark Online Free - Doraby"
    private val page = "https://doraby.com/ozark/"
    private val host = "doraby.com"

    private fun cand(url: String, frame: String = "", chain: List<String> = emptyList()) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, page, frame, 0).also { it.frameChain = chain }

    @Test fun theEpisodeInThePlayerFramesAddressNamesTheFile() {
        for ((frame, want) in listOf(
            "https://vidsrc.xyz/embed/tv/69740/1/1" to "Ozark S01E01",
            "https://vidfast.pro/tv/69740/1/2?autoPlay=true" to "Ozark S01E02",
            "https://embed.vidrift.net/embed/tv/69740/2/10" to "Ozark S02E10",
            "https://play.xpass.top/e/tv/69740/3/4" to "Ozark S03E04",
            "https://player.example/embed?tmdb=69740&season=1&episode=3" to "Ozark S01E03",
            "https://player.example/e?id=69740&s=2&e=5" to "Ozark S02E05",
            "https://www.vidy.st/tv/69740-1-6" to "Ozark S01E06",
        )) {
            assertEquals(frame, want, MediaNames.streamStem(title, page, host, "720p", cand("https://cdn.example/x/master.m3u8", frame)))
        }
    }

    @Test fun twoEpisodesOfTheSamePageGetTwoNames() {
        val a = MediaNames.streamStem(title, page, host, "", cand("https://cdn.example/master.m3u8", "https://vidsrc.xyz/embed/tv/69740/1/1"))
        val b = MediaNames.streamStem(title, page, host, "", cand("https://cdn.example/master.m3u8", "https://vidsrc.xyz/embed/tv/69740/1/2"))
        assertEquals("Ozark S01E01", a); assertEquals("Ozark S01E02", b)
    }

    @Test fun anEmbedInsideAnEmbedIsReadThroughTheFramesAroundIt() {
        val c = cand("https://tmstr.cloudnestra.com/pl/abc/master.m3u8", "https://cloudnestra.com/prorcp/xyz", listOf("https://vidsrc.xyz/embed/tv/69740/1/7"))
        assertEquals("Ozark S01E07", MediaNames.streamStem(title, page, host, "", c))
    }

    @Test fun theActiveItemOfThePagesEpisodeListWinsOverTheAddress() {
        val clues = NameClues(season = 1, episode = 4, episodeTitle = "Kaleidoscope")
        val c = cand("https://cdn.example/master.m3u8", "https://vidsrc.xyz/embed/tv/69740/1/2")
        assertEquals("Ozark S01E04 - Kaleidoscope", MediaNames.streamStem(title, page, host, "", c, clues))
    }

    @Test fun anEpisodeWithoutASeasonTakesTheSeasonOfTheTitleOrTheAddress() {
        assertEquals("Ozark S02E03", MediaNames.streamStem("Watch Ozark Season 2 Online Free - Doraby", page, host, "", null, NameClues(episode = 3)))
        assertEquals("Ozark S03E01", MediaNames.streamStem("Ozark - Doraby", "https://doraby.com/ozark/season-3/", host, "", null, NameClues(episode = 1)))
    }

    @Test fun thePagesOwnHeadingNamesTheShowWhenTheTitleDoesNot() {
        assertEquals("Better Call Saul S01E02", MediaNames.streamStem("Doraby - Home", "https://doraby.com/watch?id=77", host, "", cand("https://c/x.m3u8", "https://vidfast.pro/tv/1/1/2"), NameClues(show = "Better Call Saul")))
    }

    @Test fun aFilmKeepsTheOldNameAndNothingIsInvented() {
        val c = cand("https://cdn.example/master.m3u8", "https://vidsrc.xyz/embed/movie/693134")
        assertEquals("Dune - Part Two (2024) - 720p", MediaNames.streamStem("Watch Dune: Part Two (2024) Full Movie Online Free - Doraby", "https://doraby.com/dune-part-two/", host, "720p", c))
        assertNull(EpisodeNames.fromClues(null, listOf("https://vidsrc.xyz/embed/movie/1"), "", title, page, host))
        assertNull(EpisodeNames.fromClues(NameClues(), emptyList(), "", title, page, host))
    }

    @Test fun aSourceThatNamesItselfLikeAReleaseStillWins() {
        val c = cand("https://cdn.example/Ozark.S02E05.1080p.WEB.H264-NTb.mp4", "https://vidsrc.xyz/embed/tv/69740/1/1").also { it.sourceName = "" }
        assertEquals("Ozark S02E05", MediaNames.defaultStem(c, title, host, "1080p", page))
    }

    @Test fun textFromAPageIsCutShortAndCleaned() {
        val s = NameClues.clean("  <b>Ozark</b>\n" + "x".repeat(200))
        assert(s.length <= NameClues.MAX_TEXT && !s.contains('<') && !s.contains('\n')) { s }
        assertEquals(0, NameClues.episode(0)); assertEquals(0, NameClues.season(500)); assertEquals(4, NameClues.episode(4))
    }

    @Test fun anIdThatLooksLikeAnEpisodeIsNotOne() {
        // a media file with 1920x1080 in its name or a long numeric id must not become an episode
        assertNull(EpisodeNames.urlMark("https://cdn.example/video_1920x1080.m3u8"))
        assertNull(EpisodeNames.urlMark("https://player.example/embed/69740"))
        assertNull(EpisodeNames.urlMark("https://player.example/embed/movie/693134"))
    }
}
