package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/** Stream parts that wear a picture header ("part1.jpg" that is a transport stream with a tiny real PNG / GIF / JPEG in front). */
class SegmentPrefixTest {
    /** [packets] transport stream packets: 0x47, PID, counter, filler; the pattern a demuxer needs. */
    private fun ts(packets: Int): ByteArray {
        val b = ByteArray(packets * 188)
        for (p in 0 until packets) { b[p * 188] = 0x47; b[p * 188 + 1] = 0x01; b[p * 188 + 2] = 0x00; b[p * 188 + 3] = (0x10 or (p and 15)).toByte(); for (i in 4 until 188) b[p * 188 + i] = ((p * 31 + i * 7) and 0xFF).toByte().let { if (it == 0x47.toByte()) 0x48 else it } }
        return b
    }
    private fun be32(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
    private fun chunk(type: String, data: ByteArray) = be32(data.size) + type.toByteArray(Charsets.ISO_8859_1) + data + byteArrayOf(0, 0, 0, 0) // the CRC is not checked here
    private val png: ByteArray get() = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + chunk("IHDR", ByteArray(13) { if (it == 3) 1 else 0 }) + chunk("IDAT", ByteArray(12) { 7 }) + chunk("IEND", ByteArray(0))
    private val gif: ByteArray get() = "GIF89a".toByteArray() + byteArrayOf(1, 0, 1, 0, 0x80.toByte(), 0, 0) + ByteArray(6) /* 2-colour table */ + byteArrayOf(0x21, 0xF9.toByte(), 4, 1, 0, 0, 0, 0) + byteArrayOf(0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0) + byteArrayOf(2, 2, 0x44, 1, 0) + byteArrayOf(0x3B)
    private val jpg: ByteArray get() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 16) + ByteArray(14) { 3 } + byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    @Test fun aPartThatStartsWithItsMediaIsLeftAlone() {
        val part = ts(10)
        assertEquals(0, SegmentPrefix.length(part))
        assertEquals(0, SegmentPrefix.imageLength(part))
    }

    @Test fun aPictureHeaderInFrontOfTheTransportStreamIsSkippedWhateverThePicture() {
        for ((name, head) in listOf("png" to png, "gif" to gif, "jpeg" to jpg)) {
            val part = head + ts(10)
            assertEquals("$name header", head.size, SegmentPrefix.length(part))
            assertEquals("$name header (images only)", head.size, SegmentPrefix.imageLength(part))
        }
    }

    @Test fun anUnnamedRunOfGarbageIsSkippedByFindingTheSync() {
        val junk = ByteArray(137) { (it * 3 + 11).toByte().let { v -> if (v == 0x47.toByte()) 0x01 else v } }
        assertEquals(137, SegmentPrefix.length(junk + ts(10)))
        assertEquals(0, SegmentPrefix.imageLength(junk + ts(10)))   // not a picture: only the sync scan finds it
    }

    @Test fun anEncryptedPartCanOnlyLoseAPictureHeader() {
        // the bytes after the header are cipher text: no sync to find, but the PNG header is clear (imageLength is what the engine asks for an encrypted part)
        val cipher = ByteArray(2048) { (it * 5 + 3).toByte() }
        assertEquals(png.size, SegmentPrefix.imageLength(png + cipher, ts = false))
        assertEquals(0, SegmentPrefix.imageLength(cipher, ts = false))
    }

    @Test fun aPartThatIsNotATransportStreamKeepsItsBytesUnlessItStartsWithAPicture() {
        val mp4 = byteArrayOf(0, 0, 0, 0x18) + "ftypiso5".toByteArray() + ByteArray(600)
        assertEquals(0, SegmentPrefix.length(mp4, ts = false))
        assertEquals(png.size, SegmentPrefix.length(png + mp4, ts = false))
    }

    @Test fun aPictureThatIsNotFollowedByTransportStreamIsNotATrickAndIsLeftAlone() {
        val realPicture = png + ByteArray(2000) { 9 }   // a real PNG with data after it that is not a transport stream (>= 377 bytes: enough to tell)
        assertEquals(0, SegmentPrefix.length(realPicture))
    }

    @Test fun truncatedOrShortDataNeverThrows() {
        assertEquals(0, SegmentPrefix.length(ByteArray(0)))
        assertEquals(0, SegmentPrefix.length(ByteArray(5)))
        assertEquals(0, SegmentPrefix.length(png.copyOf(30)))
        assertEquals(0, SegmentPrefix.length(gif.copyOf(20)))
        assertEquals(0, SegmentPrefix.length(jpg.copyOf(10)))
    }
}
