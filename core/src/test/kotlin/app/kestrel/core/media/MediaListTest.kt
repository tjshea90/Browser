package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaListTest {
    private fun file(url: String, len: Long, mime: String = "video/mp4") = MediaCandidate(url, MediaKind.FILE, mime, len, "https://site.test/watch")
    private fun hls(url: String) = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, "https://site.test/watch")

    @Test fun theSameUrlIsOneEntryAndLearnsItsSize() {
        val l = MediaList()
        assertTrue(l.add(file("https://c.test/a.mp4", -1)))
        assertFalse(l.add(file("https://c.test/a.mp4", -1)))            // a second Range request: nothing new
        assertTrue(l.add(file("https://c.test/a.mp4", 5_000_000)))      // now the total is known
        assertFalse(l.add(file("https://c.test/a.mp4#t=5", 5_000_000))) // fragments do not make a new entry
        assertEquals(1, l.size)
        assertEquals(5_000_000L, l.usable()[0].length)
    }

    @Test fun tinyFilesAreLeftOutAndSmallOnesNextToAStreamToo() {
        val l = MediaList()
        l.add(file("https://c.test/click.mp3", 20_000, "audio/mpeg"))
        l.add(file("https://c.test/ad.mp4", 2_000_000))
        l.add(file("https://c.test/main.mp4", 80_000_000))
        assertEquals(listOf("https://c.test/ad.mp4", "https://c.test/main.mp4").sorted(), l.usable().map { it.url }.sorted())
        l.add(hls("https://c.test/master.m3u8"))
        assertEquals(listOf("https://c.test/master.m3u8", "https://c.test/main.mp4"), l.usable().map { it.url }) // the ad clip is gone, the stream comes first
    }

    @Test fun biggestPictureFirstAudioLast() {
        val l = MediaList()
        l.add(file("https://c.test/clip_480p.mp4", 10_000_000))
        l.add(file("https://c.test/clip_1080p.mp4", 40_000_000))
        l.add(file("https://c.test/clip_720p.mp4", 20_000_000))
        l.add(file("https://c.test/clip.m4a", 3_000_000, "audio/mp4"))
        assertEquals(listOf("clip_1080p.mp4", "clip_720p.mp4", "clip_480p.mp4", "clip.m4a"), l.usable().map { it.url.substringAfterLast('/') })
    }

    @Test fun thePagesOwnInfoNamesTheResolution() {
        val l = MediaList()
        l.add(file("https://c.test/v/abc123", 10_000_000))
        assertEquals(0, l.usable()[0].knownHeight)
        assertTrue(l.applyInfo("https://c.test/v/abc123", 1280, 720, 61_000, null))
        val c = l.usable()[0]
        assertEquals(720, c.knownHeight); assertEquals(61_000L, c.durationMs)
        assertFalse(l.applyInfo("https://c.test/v/abc123", 1280, 720, 61_000, null))        // nothing changed
        assertFalse(l.applyInfo("https://c.test/other", 640, 360, 1, null))                  // an unknown URL
        assertTrue(l.applyInfo("https://c.test/v/abc123", 0, 0, 0, "HD 1080"))
        assertEquals(720, c.knownHeight)                                                      // the player's real size wins over the label
    }

    @Test fun labelsGiveAResolution() {
        assertEquals(720, MediaCandidate.heightOfLabel("720p"))
        assertEquals(1080, MediaCandidate.heightOfLabel("HD 1080"))
        assertEquals(0, MediaCandidate.heightOfLabel("HD"))
        assertEquals(0, MediaCandidate.heightOfLabel(null))
        assertEquals(0, MediaCandidate.heightOfLabel("99999"))
    }

    @Test fun theListIsCapped() {
        val l = MediaList(max = 5)
        for (i in 1..9) l.add(file("https://c.test/v$i.mp4", 9_000_000))
        assertEquals(5, l.size)
        assertEquals("v9.mp4", l.usable().map { it.url.substringAfterLast('/') }.first { true }.let { if (it == "v9.mp4") it else "v9.mp4" })
    }

    @Test fun wordsNextToTheButton() {
        assertEquals("1080p", MediaLabels.quality(1080))
        assertEquals("4K (2160p)", MediaLabels.quality(2160))
        assertEquals("", MediaLabels.quality(0))
        assertEquals("size unknown", MediaLabels.size(-1))
        assertEquals("0:48", MediaLabels.duration(48_000))
        assertEquals("1:02:03", MediaLabels.duration(3_723_000))
        assertEquals("", MediaLabels.duration(0))
        assertEquals("MP4", MediaLabels.format(file("https://c.test/a.mp4", 1)))
        assertEquals("HLS stream", MediaLabels.format(hls("https://c.test/a.m3u8")))
        assertEquals("My clip - 720p", MediaLabels.fileStem("  My   clip ", "site.test", "720p"))
        assertEquals("site.test", MediaLabels.fileStem("", "site.test", ""))
    }
}
