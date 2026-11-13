package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(listOf("v9.mp4", "v8.mp4", "v7.mp4", "v6.mp4", "v5.mp4"), l.usable().map { it.url.substringAfterLast('/') }) // the oldest went, newest first
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

    @Test fun usableCountAlwaysAgreesWithTheList() {
        val l = MediaList()
        assertEquals(0, l.usableCount())
        l.add(file("https://c.test/tiny.mp4", 5_000)) // under 100 KB: not offered
        assertEquals(l.usable().size, l.usableCount())
        l.add(file("https://c.test/main.mp4", 5_000_000))
        assertEquals(1, l.usableCount())
        l.add(file("https://c.test/unknown.mp4", -1))
        assertEquals(l.usable().size, l.usableCount())
        assertEquals(2, l.usableCount())
        l.add(file("https://c.test/unknown.mp4", 9_000_000)) // the size became known: the same entry, a better size
        assertEquals(l.usable().size, l.usableCount())
        l.add(hls("https://c.test/master.m3u8")) // a stream arrives: the 5 MB clip stays (under 4 MB would be dropped next to a stream)
        assertEquals(l.usable().size, l.usableCount())
        l.add(file("https://c.test/clip.mp4", 3_000_000)) // under 4 MB next to a stream: an ad or preview
        assertEquals(l.usable().size, l.usableCount())
        l.clear()
        assertEquals(0, l.usableCount())
    }

    @Test fun labelHeightStillParses() {
        assertEquals(720, MediaCandidate.heightOfLabel("HD 720"))
        assertEquals(1080, MediaCandidate.heightOfLabel("1080p"))
        assertEquals(0, MediaCandidate.heightOfLabel("fast"))
        assertEquals(0, MediaCandidate.heightOfLabel(null))
    }
}

class MediaListFramesTest {
    private fun hls(url: String, frame: String = "") = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, "https://site.test/watch", frame)

    @Test fun whatAGoneDocumentOfferedIsDroppedAndNothingElse() {
        val l = MediaList()
        l.add(hls("https://cdn.test/one.m3u8", "https://player-one.test/embed"))   // server one
        l.add(hls("https://cdn.test/side.m3u8", "https://sidebar.test/s"))         // another frame, still there
        l.add(hls("https://cdn.test/page.m3u8"))                                   // the page's own (frame "")
        assertEquals(listOf("https://cdn.test/one.m3u8"), l.dropFrames(listOf("https://player-one.test/embed")))
        assertEquals(setOf("https://cdn.test/side.m3u8", "https://cdn.test/page.m3u8"), l.keys().toSet())
        assertTrue(l.dropFrames(listOf("https://player-one.test/embed")).isEmpty())  // already gone
        assertTrue(l.dropFrames(listOf("")).isEmpty())                                 // the page itself is never dropped by a frame
        assertEquals(2, l.size)
    }

    // ---- EY-17 (research/REVIEW-EXTERNAL-2.md): rank, not age
    @Test fun aRealVideoIsNotPushedOutBySixtyPreviews() {
        val l = MediaList()
        val big = MediaCandidate("https://cdn.test/movie.mp4", MediaKind.FILE, "video/mp4", 300L * 1024 * 1024, "https://site.test/watch")
        l.add(big)                                                                    // the real video came FIRST (the oldest entry)
        for (i in 1..60) l.add(MediaCandidate("https://ads.test/p$i.mp4", MediaKind.FILE, "video/mp4", 200L * 1024, "https://site.test/watch")) // then sixty previews / ads
        assertEquals(48, l.size)
        assertNotNull("the oldest, biggest entry stays", l.find("https://cdn.test/movie.mp4"))
        assertNotNull("and so does the newest preview", l.find("https://ads.test/p60.mp4"))
        // a real one that arrives LAST is never the one dropped
        val late = MediaCandidate("https://cdn.test/late.mp4", MediaKind.FILE, "video/mp4", 120L * 1024 * 1024, "https://site.test/watch")
        l.add(late)
        assertEquals(48, l.size); assertNotNull(l.find("https://cdn.test/late.mp4")); assertNotNull(l.find("https://cdn.test/movie.mp4"))
    }

    @Test fun aStreamAndAPlayedOrTouchedEntryAreNeverEvicted() {
        val l = MediaList(max = 6)
        val stream = MediaCandidate("https://cdn.test/master.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch")
        val played = MediaCandidate("https://cdn.test/clip.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch")
        l.add(stream); l.add(played)
        l.applyInfo("https://cdn.test/clip.mp4", 640, 360, 30_000, null)              // the page's player reported it: touched
        for (i in 1..20) l.add(MediaCandidate("https://ads.test/p$i.mp4", MediaKind.FILE, "video/mp4", 150L * 1024, "https://site.test/watch"))
        assertEquals(6, l.size)
        assertNotNull(l.find("https://cdn.test/master.m3u8")); assertNotNull(l.find("https://cdn.test/clip.mp4"))
        assertTrue("the played 3 MB clip is shown next to the stream (not hidden as a preview)", l.usable().any { it.url == "https://cdn.test/clip.mp4" })
        // an untouched 3 MB file next to a stream IS still hidden
        val l2 = MediaList()
        l2.add(MediaCandidate("https://cdn.test/master.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch"))
        l2.add(MediaCandidate("https://cdn.test/preview.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch"))
        assertTrue(l2.usable().none { it.url == "https://cdn.test/preview.mp4" })
        // pressing it (or choosing it in the sheet) makes it a real one too
        l2.touch("https://cdn.test/preview.mp4")
        assertTrue(l2.usable().any { it.url == "https://cdn.test/preview.mp4" })
        assertFalse("touching an address the list does not hold changes nothing", l2.touch("https://cdn.test/nothing.mp4"))
        // sharp (480p+) or long (20 s+) files are shown too
        val sharp = MediaCandidate("https://cdn.test/sharp.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch").also { it.height = 720 }
        val long = MediaCandidate("https://cdn.test/long.mp4", MediaKind.FILE, "video/mp4", 3L * 1024 * 1024, "https://site.test/watch").also { it.durationMs = 25_000 }
        l2.add(sharp); l2.add(long)
        assertTrue(l2.usable().any { it.url == "https://cdn.test/sharp.mp4" } && l2.usable().any { it.url == "https://cdn.test/long.mp4" })
    }

    @Test fun theNewestSightingNamesTheDocumentThatAsks() {
        // an address found in server one's script (the document that holds it), asked for later by server two's player: it belongs to the second one now
        val l = MediaList()
        val first = MediaCandidate("https://ads.test/preroll.mp4", MediaKind.FILE, "", -1, "https://site.test/watch", "https://player-one.test/embed", 1)
        l.add(first)
        val second = MediaCandidate("https://ads.test/preroll.mp4", MediaKind.FILE, "video/mp4", 3_000_000, "https://site.test/watch", "https://player-two.test/e/9", 2)
        assertTrue(l.add(second))
        assertEquals(1, l.size)
        assertEquals("https://player-two.test/e/9", first.frame)
        assertEquals(3_000_000L, first.length)
        assertTrue("server one going away leaves it alone", l.dropFrames(listOf("https://player-one.test/embed")).isEmpty())
        assertEquals(listOf("https://ads.test/preroll.mp4"), l.dropFrames(listOf("https://player-two.test/e/9")))
        // a sighting that names no document never takes the frame away
        val l2 = MediaList()
        val a = MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch", "https://player-one.test/embed", 1)
        l2.add(a); l2.add(MediaCandidate("https://cdn.test/a.m3u8", MediaKind.HLS, "", -1, "https://site.test/watch", "", 2))
        assertEquals("https://player-one.test/embed", a.frame)
    }

    @Test fun theDownloadCountFollowsTheDrop() {
        val l = MediaList()
        l.add(hls("https://cdn.test/one.m3u8", "https://player-one.test/embed"))
        assertEquals(1, l.usableCount())
        l.dropFrames(listOf("https://player-one.test/embed"))
        assertEquals(0, l.usableCount())
        assertTrue(l.isEmpty())
    }
}
