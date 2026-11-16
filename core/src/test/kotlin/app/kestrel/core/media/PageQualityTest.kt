package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ANS53d.2: the quality list of a film that plays in the page (it is made from the stream's master playlist, a pick starts Kestrel's own player). */
class PageQualityTest {
    private val master = """#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=5000000,AVERAGE-BANDWIDTH=4200000,RESOLUTION=1920x1080,FRAME-RATE=29.970,CODECS="avc1.640028,mp4a.40.2"
1080.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,FRAME-RATE=29.970,CODECS="avc1.4d401f,mp4a.40.2"
720.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=640x360,CODECS="avc1.42c01e,mp4a.40.2"
360.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=128000,CODECS="mp4a.40.2"
audio.m3u8
"""

    private fun variants(text: String): List<Hls.Variant> = ((Hls.parse(text, "https://v.example/a/master.m3u8")) as Hls.MasterPlaylist).master.variants

    @Test fun theRowsAreThePicturesTheMasterOffersBiggestFirstWithoutTheSoundOnlyOne() {
        val rows = PageQuality.rungs(variants(master)).map { QualityLabel.text(it) }
        assertEquals(listOf("1080p  \u00B7  30 fps  \u00B7  4.2 Mbit/s", "720p  \u00B7  30 fps  \u00B7  3.0 Mbit/s", "360p  \u00B7  900 kbit/s"), rows)
    }

    @Test fun aMasterWithoutPictureSizesOffersNoChoice() {
        val bare = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=3000000,CODECS=\"avc1.4d401f,mp4a.40.2\"\na.m3u8\n#EXT-X-STREAM-INF:BANDWIDTH=900000,CODECS=\"avc1.42c01e,mp4a.40.2\"\nb.m3u8\n"
        assertFalse(PageQuality.hasChoice(PageQuality.rungs(variants(bare))))
    }

    @Test fun oneOrNoPictureIsNothingToChooseFrom() {
        val one = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,CODECS=\"avc1.4d401f,mp4a.40.2\"\na.m3u8\n"
        assertFalse(PageQuality.hasChoice(PageQuality.rungs(variants(one))))
        assertTrue(PageQuality.hasChoice(PageQuality.rungs(variants(master))))
    }

    @Test fun theFilmContinuesWhereThePageWasWhenItIsTheSameFilm() {
        assertEquals(83_250L, PageQuality.startMs(83.25, 600.0, 600_000L))
        assertEquals("a length the page does not say", 83_250L, PageQuality.startMs(83.25, -1.0, 600_000L))
        assertEquals("a length the stream does not say", 83_250L, PageQuality.startMs(83.25, 600.0, -1L))
        assertEquals("about the same (a few seconds of slack)", 83_250L, PageQuality.startMs(83.25, 604.0, 600_000L))
    }

    @Test fun anAdvertisementIsNotTheFilmSoTheStreamStartsAtTheBeginning() {
        assertEquals(0L, PageQuality.startMs(12.0, 30.0, 600_000L))
        assertEquals(0L, PageQuality.startMs(12.0, 900.0, 600_000L))
    }

    @Test fun noPositionMeansTheBeginning() {
        assertEquals(0L, PageQuality.startMs(0.0, 600.0, 600_000L)); assertEquals(0L, PageQuality.startMs(-3.0, 600.0, 600_000L)); assertEquals(0L, PageQuality.startMs(Double.NaN, 600.0, 600_000L))
    }
}
