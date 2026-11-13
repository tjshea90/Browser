package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaNamesTest {
    private fun file(url: String, cd: String = "") = MediaCandidate(url, MediaKind.FILE, "video/mp4", 10_000_000, "https://site.example/p", "", 0).also { it.sourceName = cd }

    @Test fun theNameTheServerGaveWinsOverTheAddress() {
        assertEquals("Summer Trip 2026", MediaNames.sourceStem(file("https://cdn.example/dl.php?id=9", cd = "Summer Trip 2026.mp4")))
    }

    @Test fun theLastPartOfTheAddressIsTheNameWhenItIsOne() {
        assertEquals("Holiday_Clip_1080p", MediaNames.sourceStem(file("https://cdn.example/v/Holiday_Clip_1080p.mp4?sig=1")))
        assertEquals("my holiday", MediaNames.sourceStem(file("https://cdn.example/v/my%20holiday.mp4")))
    }

    @Test fun roleWordsAndTokensAreNotNames() {
        for (u in listOf("https://s.example/get.php?id=1", "https://s.example/hls/master.m3u8", "https://s.example/videoplayback?x=1", "https://s.example/v/index.mp4", "https://s.example/a/get_file.php",
            "https://s.example/9f8e7d6c5b4a39281716.mp4", "https://s.example/v/1234567890.mp4", "https://s.example/v/master2.mp4")) {
            assertEquals("not a name: $u", "", MediaNames.sourceStem(file(u)))
        }
    }

    @Test fun withoutARealSourceNameThePageTitleAndQualityAreUsed() {
        assertEquals("Great Movie - 720p", MediaNames.defaultStem(file("https://s.example/get.php?id=1"), "Great Movie", "s.example", "720p"))
        assertEquals("s.example", MediaNames.defaultStem(file("https://s.example/get.php?id=1"), "", "s.example", ""))
    }

    @Test fun theSourcesNameIsUsedAsIsForAFile() {
        assertEquals("Holiday_Clip", MediaNames.defaultStem(file("https://cdn.example/v/Holiday_Clip.mp4"), "Page title", "cdn.example", "720p"))
    }

    @Test fun namesAreCleanedForAFileSystem() {
        assertEquals("a_b_c", MediaNames.clean("a/b:c"))
        assertEquals(120, MediaNames.clean("x".repeat(300)).length)
        assertTrue(MediaNames.isRealName("Cats and dogs")); assertFalse(MediaNames.isRealName("ab")); assertFalse(MediaNames.isRealName("12345678"))
    }

    @Test fun anExtensionIsCutOnlyWhenThereIsOne() {
        assertEquals("clip", MediaNames.stemOf("clip.mp4")); assertEquals("Mr. Smith goes", MediaNames.stemOf("Mr. Smith goes")); assertEquals("a.b", MediaNames.stemOf("a.b.mkv"))
    }

    // ---- AL6 (owner message 26): intuitive default names
    @Test fun anEpisodeThePageNamesBeatsASourceNameThatDoesNotSayWhich() {
        assertEquals("Ozark S02E05", MediaNames.defaultStem(file("https://cdn.example/v/video_8431.mp4"), "Watch Ozark Season 2 Episode 5 Online Free | Doraby", "doraby.com", "720p", "https://doraby.com/watch/ozark"))
        assertEquals("Ozark S02E05", MediaNames.defaultStem(file("https://cdn.example/v/The_Badlands.mp4"), "Ozark - Doraby", "doraby.com", "1080p", "https://doraby.com/tv/ozark/season-2/episode-5"))
    }

    @Test fun aReleaseStyleSourceNameIsMadeReadable() {
        assertEquals("Ozark S02E05", MediaNames.defaultStem(file("https://cdn.example/Ozark.S02E05.1080p.WEB.H264-NTb.mkv"), "Some page", "cdn.example", "1080p"))
        assertEquals("Dune Part Two (2024)", MediaNames.defaultStem(file("https://cdn.example/Dune.Part.Two.2024.1080p.WEB-DL.H.264-FLUX.mp4"), "", "cdn.example", ""))
    }

    @Test fun aFilmPageLosesTheSiteNoiseButKeepsTheQuality() {
        assertEquals("Dune - Part Two (2024) - 720p", MediaNames.defaultStem(file("https://s.example/get.php?id=1"), "Watch Dune: Part Two (2024) Full Movie Online Free - Doraby", "doraby.com", "720p"))
        assertEquals("Dune - Part Two (2024) - 480p", MediaNames.streamStem("Watch Dune: Part Two (2024) Full Movie Online Free - Doraby", "https://doraby.com/movie/dune-2", "doraby.com", "480p"))
    }

    @Test fun streamQualityRowsOfAnEpisodeShareTheEpisodeName() {
        assertEquals("Ozark S02E05", MediaNames.streamStem("Ozark S02E05 - Doraby", "https://doraby.com/x", "doraby.com", "720p"))
        assertEquals("Great Movie - 720p", MediaNames.streamStem("Great Movie", "https://s.example/m", "s.example", "720p"))
        assertEquals("s.example", MediaNames.streamStem("", "", "s.example", ""))
    }

    @Test fun plainVideoFilesAreRenamedOnlyWhenTheNameSaysNothingOrIsAReleaseName() {
        assertEquals("Ozark S02E05.mkv", MediaNames.friendlyFileName("Ozark.S02E05.1080p.WEB.H264-NTb.mkv", "video/x-matroska", "x", "", "h"))
        assertEquals("Ozark S02E05.mp4", MediaNames.friendlyFileName("video.mp4", "video/mp4", "Ozark Season 2 Episode 5", "https://doraby.com/x", "doraby.com"))
        assertEquals("Trailer.mp4", MediaNames.friendlyFileName("Trailer.mp4", "video/mp4", "Ozark Season 2 Episode 5", "https://doraby.com/x", "doraby.com"))
        assertEquals("report.pdf", MediaNames.friendlyFileName("report.pdf", "application/pdf", "Ozark Season 2 Episode 5", "https://doraby.com/x", "doraby.com"))
        assertEquals("Holiday_Clip.mp4", MediaNames.friendlyFileName("Holiday_Clip.mp4", "video/mp4", "Page", "https://x.test/", "x.test"))
        assertEquals("noext", MediaNames.friendlyFileName("noext", "video/mp4", "Ozark Season 2 Episode 5", "", ""))
    }
}

