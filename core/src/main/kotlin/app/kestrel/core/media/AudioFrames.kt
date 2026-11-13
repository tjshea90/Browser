package app.kestrel.core.media

/** One audio frame inside a PES payload: where it starts and how many bytes it has (the whole frame, header included). */
class AFrame(val offset: Int, val length: Int)

/** What the first frame of a sound stream says that an MP4 needs. [config] is the codec's own configuration (AC-3: the `dac3` body, E-AC-3: the `dec3` body, MP3: the MPEG audio object type). */
class AudioInfo(val sampleRate: Int, val channels: Int, val frameSamples: Int, val config: ByteArray)

/** AC-3 (Dolby Digital, A/52): syncframes of 1536 samples. MP4 sample entry `ac-3` with a `dac3` box; one frame per sample. */
object Ac3 {
    private val RATES = intArrayOf(48000, 44100, 32000)
    private val BITRATES = intArrayOf(32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512, 576, 640)
    private val CHANNELS = intArrayOf(2, 1, 2, 3, 3, 4, 4, 5)

    /** The size in bytes of the syncframe starting at `b[o]`, or -1 when no valid AC-3 header is there. */
    fun frameSize(b: ByteArray, o: Int, end: Int): Int {
        if (o + 7 > end || b[o].toInt() != 0x0B || b[o + 1].toInt() != 0x77) return -1
        val fscod = (b[o + 4].toInt() and 0xFF) shr 6
        val code = b[o + 4].toInt() and 0x3F
        val bsid = (b[o + 5].toInt() and 0xFF) shr 3
        if (fscod == 3 || code > 37 || bsid > 8) return -1                  // bsid above 10 is E-AC-3
        val br = BITRATES[code shr 1]
        val words = when (fscod) { 0 -> br * 2; 2 -> br * 3; else -> (br.toLong() * 1536 * 1000 / (44100L * 16)).toInt() + (code and 1) }
        return words * 2
    }

    /** Splits a PES payload into its syncframes; stops at the first byte that does not start one. */
    fun frames(b: ByteArray, off: Int, len: Int): List<AFrame> {
        val out = ArrayList<AFrame>()
        var p = off; val end = off + len
        while (p < end) {
            val n = frameSize(b, p, end)
            if (n <= 0 || p + n > end) break
            out.add(AFrame(p, n)); p += n
        }
        return out
    }

    fun info(b: ByteArray, o: Int, end: Int): AudioInfo? {
        val size = frameSize(b, o, end)
        if (size <= 0 || o + size > end) return null
        val fscod = (b[o + 4].toInt() and 0xFF) shr 6
        val code = b[o + 4].toInt() and 0x3F
        val bsid = (b[o + 5].toInt() and 0xFF) shr 3
        val bsmod = b[o + 5].toInt() and 7
        val rd = BitReader(b.copyOfRange(o + 6, minOf(o + 12, end)), minOf(6, end - o - 6))
        return try {
            val acmod = rd.bits(3)
            if (acmod and 1 != 0 && acmod != 1) rd.skip(2)                  // cmixlev
            if (acmod and 4 != 0) rd.skip(2)                                // surmixlev
            if (acmod == 2) rd.skip(2)                                      // dsurmod
            val lfe = rd.bit()
            // dac3: fscod(2) bsid(5) bsmod(3) acmod(3) lfeon(1) bit_rate_code(5) reserved(5)
            val v = (fscod shl 22) or (bsid shl 17) or (bsmod shl 14) or (acmod shl 11) or (lfe shl 10) or ((code shr 1) shl 5)
            AudioInfo(RATES[fscod], CHANNELS[acmod] + lfe, 1536, byteArrayOf((v shr 16).toByte(), (v shr 8).toByte(), v.toByte()))
        } catch (e: IllegalStateException) { null }
    }
}

/** E-AC-3 (Dolby Digital Plus): syncframes of 256 x blocks samples. MP4 sample entry `ec-3` with a `dec3` box; a dependent substream is kept inside the sample of the frame it belongs to. */
object Eac3 {
    private val RATES = intArrayOf(48000, 44100, 32000)
    private val RATES2 = intArrayOf(24000, 22050, 16000)
    private val BLOCKS = intArrayOf(1, 2, 3, 6)
    private val CHANNELS = intArrayOf(2, 1, 2, 3, 3, 4, 4, 5)

    private class Head(val strmtyp: Int, val substream: Int, val size: Int, val rate: Int, val blocks: Int, val acmod: Int, val lfe: Int, val bsid: Int, val fscod: Int)

    private fun head(b: ByteArray, o: Int, end: Int): Head? {
        if (o + 8 > end || b[o].toInt() != 0x0B || b[o + 1].toInt() != 0x77) return null
        val rd = BitReader(b.copyOfRange(o + 2, minOf(o + 8, end)), minOf(6, end - o - 2))
        return try {
            val strmtyp = rd.bits(2); val sub = rd.bits(3); val frmsiz = rd.bits(11)
            val fscod = rd.bits(2)
            val rate: Int; val blocks: Int
            if (fscod == 3) { val f2 = rd.bits(2); if (f2 == 3) return null; rate = RATES2[f2]; blocks = 6 }
            else { rate = RATES[fscod]; blocks = BLOCKS[rd.bits(2)] }
            val acmod = rd.bits(3); val lfe = rd.bit(); val bsid = rd.bits(5)
            if (bsid < 11 || bsid > 16) return null                           // 6..8 is plain AC-3
            Head(strmtyp, sub, (frmsiz + 1) * 2, rate, blocks, acmod, lfe, bsid, fscod)
        } catch (e: IllegalStateException) { null }
    }

    /** One entry per access unit: an independent frame plus the dependent frames that follow it. */
    fun frames(b: ByteArray, off: Int, len: Int): List<AFrame> {
        val out = ArrayList<AFrame>()
        var p = off; val end = off + len
        while (p < end) {
            val h = head(b, p, end) ?: break
            if (h.size < 8 || p + h.size > end) break
            if ((h.strmtyp == 1 || h.substream != 0) && out.isNotEmpty()) { val last = out.removeAt(out.size - 1); out.add(AFrame(last.offset, last.length + h.size)) }
            else out.add(AFrame(p, h.size))
            p += h.size
        }
        return out
    }

    fun info(b: ByteArray, o: Int, end: Int): AudioInfo? {
        val h = head(b, o, end) ?: return null
        val samples = h.blocks * 256
        val rateKbps = ((h.size.toLong() * 8 * h.rate) / samples / 1000).toInt()
        // dec3: data_rate(13) num_ind_sub(3) | per independent substream: fscod(2) bsid(5) reserved(1) asvc(1) bsmod(3) acmod(3) lfeon(1) reserved(3) num_dep_sub(4) reserved(1) = 40 bits
        var v = 0L
        fun put(value: Long, width: Int) { v = (v shl width) or (value and ((1L shl width) - 1)) }
        put(rateKbps.toLong().coerceIn(0, 8191), 13); put(0, 3)                // one independent substream
        put(h.fscod.toLong(), 2); put(h.bsid.toLong(), 5); put(0, 1); put(0, 1); put(0, 3) // asvc and bsmod unknown
        put(h.acmod.toLong(), 3); put(h.lfe.toLong(), 1); put(0, 3); put(0, 4); put(0, 1) // num_dep_sub = 0: a dependent substream only widens the layout
        val cfg = ByteArray(5) { k -> (v shr (8 * (4 - k))).toByte() }
        return AudioInfo(h.rate, CHANNELS[h.acmod] + h.lfe, samples, cfg)
    }
}

/** MPEG audio layer III (MP3). MP4 sample entry `mp4a` with an `esds` whose object type is 0x6B (MPEG-1) or 0x69 (MPEG-2 / 2.5); one frame per sample. Layers I and II are not written (Android has no decoder for them). */
object Mp3 {
    private val BR1 = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
    private val BR2 = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0)
    private val SR = arrayOf(intArrayOf(11025, 12000, 8000), intArrayOf(0, 0, 0), intArrayOf(22050, 24000, 16000), intArrayOf(44100, 48000, 32000))

    /** Frame length in bytes of the layer III frame whose header starts at `b[o]`; -1 when it is not one. */
    fun frameSize(b: ByteArray, o: Int, end: Int): Int {
        if (o + 4 > end) return -1
        val h = ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
        if ((h ushr 21) != 0x7FF) return -1
        val ver = (h ushr 19) and 3; val layer = (h ushr 17) and 3
        if (ver == 1 || layer != 1) return -1                                  // layer III only
        val br = (if (ver == 3) BR1 else BR2)[(h ushr 12) and 15]
        val sr = SR[ver][(h ushr 10) and 3]
        if (br == 0 || sr == 0) return -1
        val pad = (h ushr 9) and 1
        return (if (ver == 3) 144 else 72) * br * 1000 / sr + pad
    }

    fun frames(b: ByteArray, off: Int, len: Int): List<AFrame> {
        val out = ArrayList<AFrame>()
        var p = off; val end = off + len
        while (p < end) {
            val n = frameSize(b, p, end)
            if (n <= 4 || p + n > end) break
            out.add(AFrame(p, n)); p += n
        }
        return out
    }

    fun info(b: ByteArray, o: Int, end: Int): AudioInfo? {
        val n = frameSize(b, o, end)
        if (n <= 0) return null
        val h = ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
        val ver = (h ushr 19) and 3
        val mono = ((h ushr 6) and 3) == 3
        return AudioInfo(SR[ver][(h ushr 10) and 3], if (mono) 1 else 2, if (ver == 3) 1152 else 576, byteArrayOf(if (ver == 3) 0x6B else 0x69))
    }
}
