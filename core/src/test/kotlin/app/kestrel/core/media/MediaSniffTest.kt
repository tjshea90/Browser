package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSniffTest {
    private fun kind(url: String, ct: String?, status: Int = 200) = MediaSniff.classify(url, ct, status)

    @Test fun mediaTypesAreFiles() {
        assertEquals(MediaKind.FILE, kind("https://cdn.test/a/clip.mp4", "video/mp4"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/a/clip.mp4", "video/mp4; codecs=\"avc1.64001f, mp4a.40.2\""))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/v?id=7", "video/webm", 206))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/song", "audio/mpeg"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/o", "application/ogg"))
    }

    @Test fun genericTypesNeedAMediaExtension() {
        assertEquals(MediaKind.FILE, kind("https://cdn.test/dl/clip.MP4?token=1", "application/octet-stream"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/dl/clip.webm", null))
        assertNull(kind("https://cdn.test/dl/archive.zip", "application/octet-stream"))
        assertNull(kind("https://cdn.test/dl/blob", "application/octet-stream"))
    }

    @Test fun playlistsAndManifests() {
        assertEquals(MediaKind.HLS, kind("https://cdn.test/live/master", "application/vnd.apple.mpegurl"))
        assertEquals(MediaKind.HLS, kind("https://cdn.test/x/index.m3u8?sig=abc", "application/x-mpegURL"))
        assertEquals(MediaKind.HLS, kind("https://cdn.test/x/index.m3u8", "application/octet-stream"))
        assertEquals(MediaKind.HLS, kind("https://cdn.test/x/index.m3u8", "text/plain"))
        assertEquals(MediaKind.DASH, kind("https://cdn.test/x/manifest.mpd", "application/dash+xml"))
        assertEquals(MediaKind.DASH, kind("https://cdn.test/x/manifest.mpd", "application/octet-stream"))
        assertEquals(MediaKind.HLS, kind("https://radio.test/stream.m3u", "audio/x-mpegurl"))
    }

    @Test fun segmentsAreNeverCandidates() {
        assertNull(kind("https://cdn.test/x/seg-001.ts", "video/mp2t"))
        assertNull(kind("https://cdn.test/x/seg-001.ts", "application/octet-stream"))
        assertNull(kind("https://cdn.test/x/chunk-12.m4s", "video/mp4"))
        assertNull(kind("https://cdn.test/x/init", "video/iso.segment"))
    }

    @Test fun otherThingsAreNot() {
        assertNull(kind("https://cdn.test/x/index.m3u8", "text/html")) // an error page with the playlist's name
        assertNull(kind("https://cdn.test/x/pic.jpg", "image/jpeg"))
        assertNull(kind("https://cdn.test/x/app.js", "application/javascript"))
        assertNull(kind("https://cdn.test/x/api", "application/json"))
        assertNull(kind("blob:https://site.test/1234", "video/mp4"))
        assertNull(kind("data:video/mp4;base64,AAAA", "video/mp4"))
    }

    @Test fun onlyFullAndPartialReplies() {
        assertNull(kind("https://cdn.test/a.mp4", "video/mp4", 404))
        assertNull(kind("https://cdn.test/a.mp4", "video/mp4", 403))
        assertNull(kind("https://cdn.test/a.mp4", "video/mp4", 304))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/a.mp4", "video/mp4", 206))
    }

    @Test fun saveExtensionPrefersTheUrlThenTheType() {
        assertEquals("mp4", MediaSniff.saveExtension("https://x.test/a/clip.mp4?x=1", "video/mp4"))
        assertEquals("webm", MediaSniff.saveExtension("https://x.test/videoplayback?id=1", "video/webm"))
        assertEquals("m4a", MediaSniff.saveExtension("https://x.test/a", "audio/mp4"))
        assertEquals("mp3", MediaSniff.saveExtension("https://x.test/a", "audio/mpeg"))
        assertEquals("mp4", MediaSniff.saveExtension("https://x.test/a", null))
        assertEquals("mkv", MediaSniff.saveExtension("https://x.test/file.mkv", "application/octet-stream"))
    }

    @Test fun audioOnly() {
        assertTrue(MediaSniff.isAudio("https://x.test/a", "audio/mpeg"))
        assertTrue(MediaSniff.isAudio("https://x.test/a.m4a", "application/octet-stream"))
        assertFalse(MediaSniff.isAudio("https://x.test/a.mp4", "video/mp4"))
    }

    @Test fun heightFromTheUrl() {
        assertEquals(720, MediaSniff.heightFromUrl("https://x.test/a/clip_720p.mp4"))
        assertEquals(1080, MediaSniff.heightFromUrl("https://x.test/1080p/index.mp4"))
        assertEquals(480, MediaSniff.heightFromUrl("https://x.test/a/video-480.mp4"))
        assertEquals(1080, MediaSniff.heightFromUrl("https://x.test/a/file_2021_1080.mp4"))
        assertEquals(360, MediaSniff.heightFromUrl("https://x.test/a/b.mp4?quality=360p"))
        assertEquals(0, MediaSniff.heightFromUrl("https://x.test/a/id_1234.mp4"))   // not a resolution
        assertEquals(0, MediaSniff.heightFromUrl("https://x.test/a/video.mp4"))
        assertEquals(0, MediaSniff.heightFromUrl("https://x.test/a/12345p67.mp4"))
    }

    @Test fun extensionIgnoresQueriesAndOddNames() {
        assertEquals("mp4", MediaSniff.extension("https://x.test/a/b.mp4#t=10"))
        assertEquals("", MediaSniff.extension("https://x.test/a/b"))
        assertEquals("", MediaSniff.extension("https://x.test/a/b."))
        assertEquals("", MediaSniff.extension("https://x.test/"))
        assertEquals("", MediaSniff.extension("https://x.test"))
        assertEquals("", MediaSniff.extension("https://x.test/a/thisisaverylongextension.abcdefghij"))
    }

    // ---- by the first bytes (a playlist served as text/plain under ".txt", a video under ".bin")

    private fun bytes(s: String) = s.toByteArray(Charsets.ISO_8859_1)
    private fun sniff(s: String) = MediaSniff.classifyBytes(bytes(s))
    private fun raw(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test fun aPlaylistIsRecognisedByItsFirstBytesWhateverItIsCalled() {
        assertEquals(MediaKind.HLS, sniff("#EXTM3U\n#EXT-X-VERSION:3\n")!!.kind)
        assertEquals(MediaKind.HLS, sniff("\uFEFF#EXTM3U\r\n#EXT-X-STREAM-INF:BANDWIDTH=1\n".replace("\uFEFF", "\u00EF\u00BB\u00BF"))!!.kind) // a byte order mark first
        assertEquals(MediaKind.HLS, sniff("  \n\t#EXTM3U\n#EXTINF:6,\nseg.ts\n")!!.kind)
        assertEquals("m3u8", sniff("#EXTM3U\n#EXTINF:6,\nseg.ts\n")!!.ext)
        assertEquals(MediaKind.DASH, sniff("<?xml version=\"1.0\"?>\n<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" type=\"static\">")!!.kind)
        assertEquals("mpd", sniff("<MPD type=\"static\"></MPD>")!!.ext)
    }

    @Test fun webPagesAndDataAreNotMedia() {
        assertNull(sniff("<!DOCTYPE html><html><head><title>x</title></head>"))
        assertNull(sniff("<html><body>#EXTM3U <MPD</body></html>"))
        assertNull(sniff("{\"url\":\"https://a.test/x.m3u8\"}"))
        assertNull(sniff("just some text that is not a playlist at all"))
        assertNull(MediaSniff.classifyBytes(raw(1, 2, 3)))                                        // too short to say
        assertNull(MediaSniff.classifyBytes(raw(0x89, 'P'.code, 'N'.code, 'G'.code, 13, 10, 26, 10, 0, 0, 0, 13))) // a picture
        assertNull(MediaSniff.classifyBytes(raw(0x47, 0x40, 0, 0x10) + ByteArray(200)))            // a bare MPEG-TS is a segment, not a candidate
    }

    @Test fun filesAreRecognisedByTheirSignature() {
        val mp4 = raw(0, 0, 0, 0x20) + bytes("ftypisom") + ByteArray(20)
        assertEquals(MediaSniff.Sniffed(MediaKind.FILE, "video/mp4", "mp4").let { Triple(it.kind, it.mime, it.ext) }, MediaSniff.classifyBytes(mp4)!!.let { Triple(it.kind, it.mime, it.ext) })
        assertEquals("mov", MediaSniff.classifyBytes(raw(0, 0, 0, 0x14) + bytes("ftypqt  ") + ByteArray(8))!!.ext)
        assertEquals("m4a", MediaSniff.classifyBytes(raw(0, 0, 0, 0x14) + bytes("ftypM4A ") + ByteArray(8))!!.ext)
        assertEquals("webm", MediaSniff.classifyBytes(raw(0x1A, 0x45, 0xDF, 0xA3) + ByteArray(20))!!.ext)
        assertEquals("ogv", MediaSniff.classifyBytes(bytes("OggS") + ByteArray(20))!!.ext)
        assertEquals("flv", MediaSniff.classifyBytes(raw('F'.code, 'L'.code, 'V'.code, 1, 5, 0, 0, 0, 9, 0, 0, 0))!!.ext)
        assertEquals("avi", MediaSniff.classifyBytes(bytes("RIFF") + ByteArray(4) + bytes("AVI LIST"))!!.ext)
        assertEquals("mp3", MediaSniff.classifyBytes(bytes("ID3") + raw(4, 0, 0, 0, 0, 0, 0x21, 0))!!.ext)
        assertEquals("mp3", MediaSniff.classifyBytes(raw(0xFF, 0xFB, 0x90, 0x64) + ByteArray(20))!!.ext)
        assertEquals("mpg", MediaSniff.classifyBytes(raw(0, 0, 1, 0xBA) + ByteArray(20))!!.ext)
        assertNull(MediaSniff.classifyBytes(ByteArray(64)))                                         // zeros
    }

    // ---- RVW1 (research/REVIEW_2026-11-20_FINDER.md): F10 the extension and the app agree, F14 YouTube slices, F15 numbered segments, F22 sizes in the address

    @Test fun aDownloadScriptThatNamesTheFileInTheQueryIsRecognisedLikeTheBrowserExtensionDoes() {
        assertEquals(MediaKind.FILE, kind("https://cdn.test/dl.php?file=movie.mp4", "application/octet-stream"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/get?id=7&name=My%20Film.MKV&t=1", "application/octet-stream"))
        assertEquals(MediaKind.HLS, kind("https://cdn.test/proxy?f=x.m3u8", "text/plain"))
        assertEquals(MediaKind.HLS, kind("https://cdn.test/p.php?u=https%3A%2F%2Fa.test%2Fb%2Findex.m3u8", "application/octet-stream"))
        assertEquals(MediaKind.DASH, kind("https://cdn.test/m?src=/v/manifest.mpd", "text/plain"))
        assertNull("not media: a zip", kind("https://cdn.test/dl.php?file=archive.zip", "application/octet-stream"))
        assertNull("not media: a page", kind("https://cdn.test/watch.php?v=abc", "text/html"))
        assertNull("a JSON answer is never media", kind("https://cdn.test/api?file=x.mp4", "application/json"))
        assertEquals("mkv", MediaSniff.saveExtension("https://cdn.test/get?name=My%20Film.MKV", "application/octet-stream"))
        assertEquals("the path's own media extension still wins", "mp4", MediaSniff.effectiveExtension("https://cdn.test/a/clip.mp4?file=other.mkv"))
        assertEquals("", MediaSniff.queryExtension("https://cdn.test/a?x=1&y=2"))
    }

    @Test fun youtubeVideoplaybackSlicesAreNotCandidates() {
        assertNull(kind("https://rr3---sn-abc.googlevideo.com/videoplayback?expire=1&id=o-AB&itag=137&range=0-999999", "video/mp4"))
        assertNull(kind("https://rr1---sn-xyz.googlevideo.com/videoplayback?itag=140", "audio/mp4"))
        assertTrue(MediaSniff.isVideoPlaybackSlice("https://googlevideo.com/videoplayback?x=1"))
        assertFalse("another path on that host is left to the other rules", MediaSniff.isVideoPlaybackSlice("https://rr1.googlevideo.com/other?x=1"))
        assertFalse("another site's videoplayback", MediaSniff.isVideoPlaybackSlice("https://cdn.test/videoplayback?x=1"))
        assertFalse("a look-alike host", MediaSniff.isVideoPlaybackSlice("https://notgooglevideo.com/videoplayback"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/film.mp4", "video/mp4"))
    }

    @Test fun numberedPartsOfAStreamAreNotCandidatesButAPersonsOwnSplitFilesAre() {
        assertNull(kind("https://cdn.test/v/seg_00012.mp4", "video/mp4"))
        assertNull(kind("https://cdn.test/v/chunk-3-v1-a1.m4s", "video/mp4"))
        assertNull(kind("https://cdn.test/v/frag12.webm", "video/webm"))
        assertNull(kind("https://cdn.test/v/segment-7.mp4?token=1", "application/octet-stream"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/v/part1.mp4", "video/mp4"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/v/video1.mp4", "video/mp4"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/v/segments-of-a-film.mp4", "video/mp4"))
        assertEquals(MediaKind.FILE, kind("https://cdn.test/v/Seg.mp4", "video/mp4"))
    }

    @Test fun aWidthByHeightInTheAddressGivesTheHeight() {
        assertEquals(720, MediaSniff.heightFromUrl("https://video.twimg.com/ext_tw_video/1/vid/1280x720/abc.mp4"))
        assertEquals(1080, MediaSniff.heightFromUrl("https://cdn.test/v/clip_1920x1080.mp4"))
        assertEquals("a portrait picture: the second number is not a known height", 0, MediaSniff.heightFromUrl("https://cdn.test/v/720x1280/a.mp4"))
        assertEquals("the old rule still wins", 480, MediaSniff.heightFromUrl("https://cdn.test/v/clip_480p.mp4"))
        assertEquals(0, MediaSniff.heightFromUrl("https://cdn.test/v/a.mp4"))
    }
}
