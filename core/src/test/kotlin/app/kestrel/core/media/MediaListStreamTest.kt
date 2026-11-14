package app.kestrel.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RX3-BK2: a page that loaded a stream playlist holds a video player (MSE/EME), which the engine's back/forward cache cannot keep. */
class MediaListStreamTest {
    private val page = "https://site.test/watch"

    @Test fun onlyAStreamPlaylistMakesItAStreamPage() {
        val l = MediaList()
        assertFalse(l.hasStream())
        l.add(MediaCandidate("https://cdn.test/a.mp4", MediaKind.FILE, "video/mp4", 5_000_000, page))
        assertFalse("a plain file is not a player", l.hasStream())
        l.add(MediaCandidate("https://cdn.test/master.m3u8", MediaKind.HLS, "application/vnd.apple.mpegurl", -1, page))
        assertTrue(l.hasStream())
        l.clear()
        assertFalse("a new page starts with an empty list", l.hasStream())
    }

    @Test fun aDashManifestCountsToo() {
        val l = MediaList()
        l.add(MediaCandidate("https://cdn.test/manifest.mpd", MediaKind.DASH, "application/dash+xml", -1, page))
        assertTrue(l.hasStream())
    }
}
