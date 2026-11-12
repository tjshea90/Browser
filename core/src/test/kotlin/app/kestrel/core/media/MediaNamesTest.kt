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
}
