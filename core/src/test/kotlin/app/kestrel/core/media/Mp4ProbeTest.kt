package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4ProbeTest {
    private fun res(name: String): ByteArray = javaClass.getResourceAsStream("/mp4/$name")!!.readBytes()

    @Test fun aFileWithTheMoovUpFrontIsReadFromItsFirstBytes() {
        val b = res("fast-head.bin")
        val r = Mp4Probe.probe(b, b.size) as Mp4Probe.Result.Found
        assertEquals(1280, r.info.width); assertEquals(720, r.info.height)
        assertEquals("H.264", r.info.videoCodec)
        assertTrue("about 3 s, was ${r.info.durationMs}", r.info.durationMs in 2900..3100)
    }

    @Test fun aFileWithTheMoovAtTheEndSaysWhereToLook() {
        val head = res("slow-head.bin")
        val need = Mp4Probe.probe(head, head.size) as Mp4Probe.Result.NeedMore
        assertEquals(84876L, need.offset)                                                // ftyp 32 + free 8 + mdat 84836
        val moov = res("slow-moov.bin")
        val r = Mp4Probe.probe(moov, moov.size, base = need.offset) as Mp4Probe.Result.Found
        assertEquals(640, r.info.width); assertEquals(360, r.info.height)
        assertTrue(r.info.durationMs in 2900..3100)
    }

    @Test fun aMoovThatIsCutOffAsksForTheWholeBox() {
        val b = res("fast-head.bin")
        val r = Mp4Probe.probe(b, 1000) as Mp4Probe.Result.NeedMore                      // the moov starts at 32 and is 3879 bytes long
        assertEquals(32L, r.offset); assertEquals(3879, r.length)
    }

    @Test fun anythingElseIsNotAnMp4() {
        val junk = ByteArray(4096) { (it * 31).toByte() }
        assertTrue(Mp4Probe.probe(junk, junk.size) is Mp4Probe.Result.NotMp4)
        val html = "<html><body>forbidden</body></html>".toByteArray()
        assertTrue(Mp4Probe.probe(html, html.size) is Mp4Probe.Result.NotMp4)
        assertTrue(Mp4Probe.probe(ByteArray(0), 0) is Mp4Probe.Result.NeedMore)
    }
}
