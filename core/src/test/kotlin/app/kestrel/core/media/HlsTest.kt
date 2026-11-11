package app.kestrel.core.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsTest {
    private val base = "https://cdn.example.com/video/abc/master.m3u8?token=1"

    private fun media(text: String, b: String = "https://cdn.example.com/video/abc/720p/index.m3u8"): Hls.Media = (Hls.parse(text, b) as Hls.MediaPlaylist).media
    private fun master(text: String): Hls.Master = (Hls.parse(text, base) as Hls.MasterPlaylist).master

    @Test fun masterPlaylistListsTheQualities() {
        val m = master("""
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="English",DEFAULT=YES,AUTOSELECT=YES,LANGUAGE="en",URI="audio/en.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,AVERAGE-BANDWIDTH=2100000,RESOLUTION=1280x720,FRAME-RATE=29.970,CODECS="avc1.64001f,mp4a.40.2",AUDIO="aud"
            720p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,CODECS="avc1.4d401e,mp4a.40.2",AUDIO="aud"
            360p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080,FRAME-RATE=60,CODECS="hvc1.1.6.L120.90,mp4a.40.2"
            https://other.example.com/1080/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=128000,CODECS="mp4a.40.2"
            audio-only.m3u8
        """.trimIndent())
        assertEquals(4, m.variants.size)
        val v720 = m.variants[0]
        assertEquals("https://cdn.example.com/video/abc/720p/index.m3u8", v720.url)
        assertEquals(720, v720.height); assertEquals(1280, v720.width)
        assertEquals("720p", v720.label); assertEquals("H.264", v720.videoCodec)
        assertEquals(2100000L / 8, v720.bytesPerSecond)                        // average wins over the peak
        assertEquals(800000L / 8, m.variants[1].bytesPerSecond)                // no average: the peak
        assertEquals("1080p60", m.variants[2].label); assertEquals("H.265", m.variants[2].videoCodec)
        assertEquals("https://other.example.com/1080/index.m3u8", m.variants[2].url)
        assertTrue(m.variants[3].audioOnly); assertEquals("Audio only", m.variants[3].label)
        assertEquals("https://cdn.example.com/video/abc/audio/en.m3u8", m.audioFor(v720)?.url)
        assertNull(m.audioFor(m.variants[1].let { Hls.Variant(it.url, it.bandwidth, 0, it.width, it.height, it.codecs, 0.0, null) }))
    }

    @Test fun aVodMediaPlaylist() {
        val md = media("""
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:100
            #EXT-X-PLAYLIST-TYPE:VOD
            #EXTINF:6.006,
            seg100.ts
            #EXTINF:6.006,
            seg101.ts?x=1
            #EXTINF:3.5,
            /abs/seg102.ts
            #EXT-X-ENDLIST
        """.trimIndent())
        assertNull(md.problem)
        assertEquals(3, md.segments.size)
        assertEquals("https://cdn.example.com/video/abc/720p/seg100.ts", md.segments[0].url)
        assertEquals("https://cdn.example.com/video/abc/720p/seg101.ts?x=1", md.segments[1].url)
        assertEquals("https://cdn.example.com/abs/seg102.ts", md.segments[2].url)
        assertEquals(6006L, md.segments[0].durationMs); assertEquals(15512L, md.durationMs)
        assertEquals(100L, md.segments[0].seq); assertEquals(102L, md.segments[2].seq)
        assertEquals("ts", md.container)
        assertEquals(-1L, md.exactBytes)
    }

    @Test fun noEndListMeansLive() {
        val md = media("#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXT-X-MEDIA-SEQUENCE:5\n#EXTINF:4,\na.ts\n#EXTINF:4,\nb.ts\n")
        assertEquals(Hls.Problem.LIVE, md.problem)
        val vod = media("#EXTM3U\n#EXT-X-PLAYLIST-TYPE:VOD\n#EXTINF:4,\na.ts\n")   // some servers forget ENDLIST but say VOD
        assertNull(vod.problem)
    }

    @Test fun aes128KeysAndIvs() {
        val md = media("""
            #EXTM3U
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:7
            #EXT-X-KEY:METHOD=AES-128,URI="key.bin",IV=0x000102030405060708090a0b0c0d0e0f
            #EXTINF:6,
            s7.ts
            #EXT-X-KEY:METHOD=AES-128,URI="https://keys.example.com/k2"
            #EXTINF:6,
            s8.ts
            #EXT-X-KEY:METHOD=NONE
            #EXTINF:6,
            s9.ts
            #EXT-X-ENDLIST
        """.trimIndent())
        assertNull(md.problem)
        assertEquals(2, md.keys.size)
        assertEquals("https://cdn.example.com/video/abc/720p/key.bin", md.keys[0].url)
        assertArrayEquals(ByteArray(16) { it.toByte() }, md.keys[0].iv)
        assertNull(md.keys[1].iv)                                                // derive from the sequence number
        assertEquals(listOf(0, 1, -1), md.segments.map { it.key })
        val iv = Hls.sequenceIv(8)
        assertEquals(16, iv.size); assertEquals(8.toByte(), iv[15]); assertEquals(0.toByte(), iv[14])
        assertArrayEquals(ByteArray(16).also { it[14] = 1; it[15] = 0 }, Hls.sequenceIv(256))
    }

    @Test fun drmAndSampleAesAreRefused() {
        val fair = media("#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"skd://x\",KEYFORMAT=\"com.apple.streamingkeydelivery\",KEYFORMATVERSIONS=\"1\"\n#EXTINF:6,\na.ts\n#EXT-X-ENDLIST\n")
        assertEquals(Hls.Problem.DRM, fair.problem)
        val widevine = media("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"data:text/plain;base64,AAAA\",KEYFORMAT=\"urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed\"\n#EXTINF:6,\na.ts\n#EXT-X-ENDLIST\n")
        assertEquals(Hls.Problem.DRM, widevine.problem)
        val sample = media("#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"k\"\n#EXTINF:6,\na.ts\n#EXT-X-ENDLIST\n")
        assertEquals(Hls.Problem.SAMPLE_AES, sample.problem)
    }

    @Test fun fragmentedMp4WithByteRanges() {
        val md = media("""
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-TARGETDURATION:6
            #EXT-X-MAP:URI="main.mp4",BYTERANGE="720@0"
            #EXTINF:6.0,
            #EXT-X-BYTERANGE:1000000@720
            main.mp4
            #EXTINF:6.0,
            #EXT-X-BYTERANGE:900000
            main.mp4
            #EXTINF:2.0,
            #EXT-X-BYTERANGE:50000
            main.mp4
            #EXT-X-ENDLIST
        """.trimIndent(), "https://cdn.example.com/v/x.m3u8")
        assertNull(md.problem)
        assertEquals("https://cdn.example.com/v/main.mp4", md.init?.url)
        assertEquals(0L, md.init?.from); assertEquals(720L, md.init?.len)
        assertEquals(listOf(720L, 1000720L, 1900720L), md.segments.map { it.from })   // each continues after the previous range
        assertEquals(listOf(1000000L, 900000L, 50000L), md.segments.map { it.len })
        assertEquals("mp4", md.container)
        assertEquals(720L + 1000000 + 900000 + 50000, md.exactBytes)                  // every size is known: the total is exact
    }

    @Test fun discontinuitiesAndGaps() {
        val md = media("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\na.ts\n#EXT-X-DISCONTINUITY\n#EXTINF:6,\nb.ts\n#EXT-X-GAP\n#EXTINF:6,\nc.ts\n#EXTINF:6,\nd.ts\n#EXT-X-ENDLIST\n")
        assertEquals(3, md.segments.size)                                             // the gap segment is not fetched
        assertEquals(1, md.discontinuities)
        assertTrue(md.segments[1].discontinuity)
        assertEquals(listOf(0L, 1L, 3L), md.segments.map { it.seq })                  // sequence numbers still count the gap (they feed the AES IV)
    }

    @Test fun notAPlaylist() {
        assertNull(Hls.parse("<html><body>Forbidden</body></html>", base))
        assertNull(Hls.parse("", base))
        assertEquals(Hls.Problem.EMPTY, media("#EXTM3U\n#EXT-X-ENDLIST\n").problem)
        assertNotNull(Hls.parse("﻿#EXTM3U\n#EXTINF:1,\na.ts\n#EXT-X-ENDLIST", base))     // a byte order mark in front is tolerated
    }

    @Test fun attributeListsWithQuotedCommas() {
        val a = Hls.attrs("""BANDWIDTH=1000,CODECS="avc1.4d401f,mp4a.40.2",RESOLUTION=640x360,NAME="a=b",URI="x""")
        assertEquals("1000", a["BANDWIDTH"]); assertEquals("avc1.4d401f,mp4a.40.2", a["CODECS"]); assertEquals("640x360", a["RESOLUTION"]); assertEquals("a=b", a["NAME"]); assertEquals("x", a["URI"])
    }

    @Test fun addressesAreResolvedLikeABrowserDoes() {
        val b = "https://cdn.example.com/a/b/master.m3u8?t=1"
        assertEquals("https://cdn.example.com/a/b/x.ts", Hls.resolve(b, "x.ts"))
        assertEquals("https://cdn.example.com/a/x.ts", Hls.resolve(b, "../x.ts"))
        assertEquals("https://cdn.example.com/x.ts", Hls.resolve(b, "/x.ts"))
        assertEquals("https://other.example.com/x.ts", Hls.resolve(b, "//other.example.com/x.ts"))
        assertEquals("http://plain.example.com/x.ts", Hls.resolve(b, "http://plain.example.com/x.ts"))
        assertEquals("https://cdn.example.com/a/b/with space.ts", Hls.resolve(b, "with space.ts"))   // URI rejects the space: the fallback keeps it
    }
}
