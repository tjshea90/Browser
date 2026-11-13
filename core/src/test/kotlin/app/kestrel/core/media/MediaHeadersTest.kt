package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner (2026-11-13): "a movie from doraby.com on the vidsrc server couldn't be downloaded in kestrel, but video downloader app can download it". The stream sat in a player embedded in the movie
 * page; its server wants the PLAYER's Referer/Origin, and Kestrel sent the movie page's. These pin the order of what is tried.
 */
class MediaHeadersTest {
    private fun hls(url: String, page: String, frame: String = "") = MediaCandidate(url, MediaKind.HLS, "application/vnd.apple.mpegurl", -1, page, frame)

    @Test fun anAddressGivesItsOriginOnly() {
        assertEquals(mapOf("Referer" to "https://player.test/", "Origin" to "https://player.test"), MediaHeaders.fromAddress("https://player.test/embed/movie?id=7&token=secret#x"))
        assertEquals(mapOf("Referer" to "http://10.0.2.2:8003/", "Origin" to "http://10.0.2.2:8003"), MediaHeaders.fromAddress("http://10.0.2.2:8003/e?x=1"))
        assertEquals(emptyMap<String, String>(), MediaHeaders.fromAddress("blob:https://player.test/1234"))
        assertEquals(emptyMap<String, String>(), MediaHeaders.fromAddress(""))
    }

    @Test fun whatTheBrowserSentComesFirstThenThePlayerThenThePage() {
        val c = hls("https://cdn.test/pl/master.m3u8", page = "https://movies.test/watch/1", frame = "https://player.test/embed/7")
        c.referer = "https://player.test/"; c.origin = "https://player.test"
        val sets = MediaHeaders.sets(c, "https://movies.test/watch/1")
        assertEquals(mapOf("Referer" to "https://player.test/", "Origin" to "https://player.test"), sets[0])        // exactly what the player sent
        assertEquals(mapOf("Referer" to "https://movies.test/", "Origin" to "https://movies.test"), sets[1])      // the web page is only the next guess (the player's origin is not repeated)
        assertTrue(sets.contains(mapOf("Referer" to "https://player.test/")))                                      // a server that dislikes an Origin it did not expect
        assertEquals(emptyMap<String, String>(), sets.last())                                                      // and nothing at all
        assertEquals(sets.size, sets.toSet().size)                                                                 // no set twice
    }

    @Test fun withoutACapturedRequestTheEmbeddedPlayersPageIsUsedBeforeTheWebPage() {
        val c = hls("https://cdn.test/pl/master.m3u8", page = "https://movies.test/watch/1", frame = "https://player.test/embed/7?hash=abc")
        val sets = MediaHeaders.sets(c, "https://movies.test/watch/1")
        assertEquals(mapOf("Referer" to "https://player.test/", "Origin" to "https://player.test"), sets[0])
        assertEquals(mapOf("Referer" to "https://movies.test/", "Origin" to "https://movies.test"), sets[1])
        assertFalse(sets.any { it.values.any { v -> v.contains("hash=abc") || v.contains("/embed/") } }) // never a path or a query
    }

    @Test fun aFileOnThePageItselfStillGetsThePageOrigin() {
        val c = hls("https://cdn.test/a.m3u8", page = "https://movies.test/watch/1")
        assertEquals(mapOf("Referer" to "https://movies.test/", "Origin" to "https://movies.test"), MediaHeaders.sets(c, "https://movies.test/watch/1")[0])
    }

    @Test fun aSetThatWorkedIsTriedFirstFromThenOn() {
        val c = hls("https://cdn.test/a.m3u8", page = "https://movies.test/watch/1", frame = "https://player.test/e")
        val good = mapOf("Referer" to "https://movies.test/")
        c.viaHeaders = good
        assertEquals(good, MediaHeaders.sets(c, "https://movies.test/watch/1")[0])
    }

    @Test fun theListLearnsWhatTheBrowserSentWhenItComesLater() {
        val l = MediaList()
        val first = hls("https://cdn.test/a.m3u8", "https://movies.test/watch/1")
        assertTrue(l.add(first))
        val again = hls("https://cdn.test/a.m3u8", "https://movies.test/watch/1")
        again.referer = "https://player.test/"; again.origin = "https://player.test"
        assertTrue(l.add(again))
        assertEquals("https://player.test/", l.find("https://cdn.test/a.m3u8")!!.referer)
        assertFalse(l.add(again)) // nothing new the second time
    }
}
