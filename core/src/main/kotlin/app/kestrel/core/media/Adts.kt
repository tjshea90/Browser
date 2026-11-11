package app.kestrel.core.media

/**
 * AAC in MPEG-TS is wrapped in ADTS (a 7 or 9 byte header per frame). An MP4 needs the raw frames, one per sample, on an exact 1024-sample grid. Pure helpers for that step; the
 * Android Remuxer does the muxing.
 */
object Adts {
    private val RATES = intArrayOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350)

    /** One frame inside a sample: where its raw payload starts, how long it is, and the sample rate its header names. */
    class Frame(val payloadOffset: Int, val payloadLength: Int, val sampleRate: Int)

    /** The ADTS frames in `b[offset until offset+size]` (offsets are into [b]); empty when the data does not start with an ADTS header (it is raw AAC already). */
    fun frames(b: ByteArray, offset: Int, size: Int): List<Frame> {
        val out = ArrayList<Frame>()
        var pos = 0
        while (pos + 7 <= size) {
            val b0 = b[offset + pos].toInt() and 0xFF; val b1 = b[offset + pos + 1].toInt() and 0xFF
            if (b0 != 0xFF || (b1 and 0xF0) != 0xF0) break
            val hdr = if ((b1 and 1) == 1) 7 else 9                 // protection_absent: no CRC
            val len = ((b[offset + pos + 3].toInt() and 3) shl 11) or ((b[offset + pos + 4].toInt() and 0xFF) shl 3) or ((b[offset + pos + 5].toInt() and 0xFF) ushr 5)
            if (len < hdr || pos + len > size) break
            val rate = RATES.getOrElse((b[offset + pos + 2].toInt() ushr 2) and 0xF) { 44100 }
            out.add(Frame(offset + pos + hdr, len - hdr, rate))
            pos += len
        }
        return out
    }
}

/**
 * The audio timeline of an MP4 made from a stream: AAC frames are exactly 1024 samples apart, so after the first frame every time is computed from a counter instead of copied from the stream
 * (whose 90 kHz stamps round, and an MP4 writer refuses a track whose time ever steps back). A real gap in the stream (more than 50 ms) is kept by moving the start forward.
 */
class AudioClock {
    private var start = -1L
    private var frames = 0L
    var last = -1L
        private set

    /** Call with the stream's own time for the first frame of each sample. */
    fun sync(streamUs: Long, rate: Int) {
        if (start < 0) { start = streamUs; return }
        val expected = start + frames * 1_000_000L * 1024 / rate
        if (streamUs - expected > 50_000) start += streamUs - expected
    }

    /** The time for the next frame (strictly increasing). */
    fun next(rate: Int): Long { val t = start + frames * 1_000_000L * 1024 / rate; frames++; last = t; return t }
}
