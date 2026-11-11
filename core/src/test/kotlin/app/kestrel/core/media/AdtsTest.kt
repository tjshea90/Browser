package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdtsTest {
    /** One ADTS frame: 7-byte header (no CRC), AAC-LC, the given sampling index, [payload] raw bytes. */
    private fun frame(payload: Int, rateIdx: Int = 4, crc: Boolean = false): ByteArray {
        val hdr = if (crc) 9 else 7
        val len = hdr + payload
        val h = ByteArray(hdr)
        h[0] = 0xFF.toByte(); h[1] = (0xF0 or (if (crc) 0 else 1)).toByte()
        h[2] = ((1 shl 6) or (rateIdx shl 2)).toByte()                 // profile LC, sampling index, channel bit 0
        h[3] = (((2 and 3) shl 6) or ((len ushr 11) and 3)).toByte()   // channels 2
        h[4] = ((len ushr 3) and 0xFF).toByte()
        h[5] = (((len and 7) shl 5) or 0x1F).toByte()
        h[6] = 0xFC.toByte()
        return h + ByteArray(payload) { (it + 1).toByte() }
    }

    @Test fun framesAreCutOutOfASample() {
        val sample = frame(200) + frame(180, crc = true) + frame(210)
        val fs = Adts.frames(sample, 0, sample.size)
        assertEquals(3, fs.size)
        assertEquals(listOf(200, 180, 210), fs.map { it.payloadLength })
        assertEquals(7, fs[0].payloadOffset); assertEquals(207 + 9, fs[1].payloadOffset)
        assertEquals(44100, fs[0].sampleRate)
        assertEquals(1.toByte(), sample[fs[0].payloadOffset])          // the payload really starts after the header
    }

    @Test fun offsetsAreRelativeToTheArrayNotTheSample() {
        val pad = ByteArray(5) { 9 }
        val sample = pad + frame(100) + frame(100)
        val fs = Adts.frames(sample, 5, sample.size - 5)
        assertEquals(2, fs.size); assertEquals(5 + 7, fs[0].payloadOffset)
    }

    @Test fun rawAacAndDamagedDataGiveNothing() {
        assertTrue(Adts.frames(ByteArray(300) { 0x21 }, 0, 300).isEmpty())            // raw AAC: no sync word
        val cut = frame(200)
        assertTrue(Adts.frames(cut, 0, 100).isEmpty())                                // the frame runs past the end of the sample
        val ok = frame(50) + cut
        assertEquals(1, Adts.frames(ok, 0, 57 + 100).size)                            // the first is whole, the second is cut: only the whole one counts
    }

    @Test fun sampleRateComesFromTheHeader() {
        assertEquals(48000, Adts.frames(frame(10, rateIdx = 3), 0, 17)[0].sampleRate)
        assertEquals(22050, Adts.frames(frame(10, rateIdx = 7), 0, 17)[0].sampleRate)
    }

    @Test fun theClockNeverStepsBackAndKeepsRealGaps() {
        val c = AudioClock()
        c.sync(1_000_000, 44100)
        val t = LongArray(30)
        for (i in 0 until 15) t[i] = c.next(44100)
        for (i in 1 until 15) assertEquals(1_000_000L * 1024 * i / 44100, t[i] - t[0])  // exactly 1024 samples apart
        c.sync(t[14] + 23_219 - 40, 44100)                                               // the next sample's stamp is a few microseconds early: ignored
        val after = c.next(44100)
        assertTrue(after > t[14])
        c.sync(after + 23_219 + 400_000, 44100)                                          // a 400 ms hole in the stream: kept
        val gap = c.next(44100)
        assertTrue("gap kept", gap - after > 400_000)
    }
}
