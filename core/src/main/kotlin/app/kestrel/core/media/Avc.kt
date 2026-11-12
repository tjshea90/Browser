package app.kestrel.core.media

/** MSB-first bit reader over an RBSP (emulation-prevention bytes already removed). Throws [IllegalStateException] when it runs out: the callers treat that as "not parseable". */
internal class BitReader(private val b: ByteArray, private val len: Int) {
    var pos = 0
        private set

    fun bit(): Int {
        check(pos < len * 8) { "end of data" }
        val v = (b[pos shr 3].toInt() ushr (7 - (pos and 7))) and 1
        pos++
        return v
    }

    fun bits(n: Int): Int { var v = 0; for (i in 0 until n) v = (v shl 1) or bit(); return v }

    fun skip(n: Int) { check(pos + n <= len * 8) { "end of data" }; pos += n }

    /** Unsigned Exp-Golomb. */
    fun ue(): Int {
        var zeros = 0
        while (bit() == 0) { zeros++; check(zeros <= 30) { "bad exp-golomb" } }
        return if (zeros == 0) 0 else (1 shl zeros) - 1 + bits(zeros)
    }

    /** Signed Exp-Golomb. */
    fun se(): Int { val k = ue(); return if (k and 1 == 1) (k + 1) / 2 else -(k / 2) }
}

/** What an H.264 SPS says that an MP4 needs. [profile] / [compat] / [level] are bytes 1..3 of the SPS (they go into `avcC` unchanged). */
class AvcSps(val profile: Int, val compat: Int, val level: Int, val width: Int, val height: Int, val chromaFormat: Int, val bitDepthLuma: Int, val bitDepthChroma: Int)

/** H.264 byte-stream helpers: NAL splitting, SPS parsing, and the Annex-B to length-prefixed (AVCC) conversion an MP4 sample wants. Pure functions, unit-tested against ffmpeg-made streams. */
object Avc {
    const val NAL_SLICE = 1
    const val NAL_IDR = 5
    const val NAL_SEI = 6
    const val NAL_SPS = 7
    const val NAL_PPS = 8
    const val NAL_AUD = 9

    /** Calls [f] with (start, end) of every NAL unit in `b[off until off+len]` (the start code and trailing zero bytes are not part of it). */
    inline fun forEachNal(b: ByteArray, off: Int, len: Int, f: (Int, Int) -> Unit) {
        val end = off + len
        var i = off
        var start = -1
        while (i + 2 < end) {
            if ((b[i + 2].toInt() and 0xFF) > 1) { i += 3; continue }      // no start code can begin at i, i+1 or i+2
            if (b[i].toInt() == 0 && b[i + 1].toInt() == 0 && b[i + 2].toInt() == 1) {
                if (start >= 0) f(start, trimZeros(b, start, i))
                start = i + 3
                i += 3
            } else i++
        }
        if (start in 0 until end) f(start, trimZeros(b, start, end))
    }

    fun trimZeros(b: ByteArray, start: Int, endExclusive: Int): Int {
        var e = endExclusive
        while (e > start && b[e - 1].toInt() == 0) e--
        return e
    }

    /** `00 00 03` -> `00 00`: the RBSP of a NAL unit. */
    fun unescape(src: ByteArray, off: Int, len: Int): ByteArray {
        val out = ByteArray(len)
        var n = 0; var zeros = 0
        for (i in off until off + len) {
            val v = src[i].toInt()
            if (zeros >= 2 && v == 3) { zeros = 0; continue }
            out[n++] = src[i]
            zeros = if (v == 0) zeros + 1 else 0
        }
        return if (n == len) out else out.copyOf(n)
    }

    private val HIGH_PROFILES = intArrayOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135)

    /** Parses the SPS NAL unit at `b[off until off+len]` (the NAL header byte included). Null when it cannot be read. */
    fun parseSps(b: ByteArray, off: Int, len: Int): AvcSps? = try { parse(b, off, len) } catch (e: IllegalStateException) { null }

    private fun parse(b: ByteArray, off: Int, len: Int): AvcSps? {
        if (len < 5 || (b[off].toInt() and 0x1F) != NAL_SPS) return null
        val r = unescape(b, off + 1, len - 1)
        val rd = BitReader(r, r.size)
        val profile = rd.bits(8); val compat = rd.bits(8); val level = rd.bits(8)
        rd.ue()                                                              // seq_parameter_set_id
        var chroma = 1; var bdl = 8; var bdc = 8
        if (profile in HIGH_PROFILES) {
            chroma = rd.ue()
            if (chroma == 3) rd.bit()                                         // separate_colour_plane_flag
            bdl = rd.ue() + 8; bdc = rd.ue() + 8
            rd.bit()                                                          // qpprime_y_zero_transform_bypass_flag
            if (rd.bit() == 1) {                                              // seq_scaling_matrix_present_flag
                val lists = if (chroma != 3) 8 else 12
                for (i in 0 until lists) if (rd.bit() == 1) skipScalingList(rd, if (i < 6) 16 else 64)
            }
        }
        rd.ue()                                                               // log2_max_frame_num_minus4
        when (rd.ue()) {                                                      // pic_order_cnt_type
            0 -> rd.ue()
            1 -> { rd.bit(); rd.se(); rd.se(); val n = rd.ue(); check(n <= 255); for (i in 0 until n) rd.se() }
        }
        rd.ue(); rd.bit()                                                     // max_num_ref_frames, gaps_in_frame_num_value_allowed_flag
        val wMbs = rd.ue() + 1; val hMapUnits = rd.ue() + 1
        val frameMbsOnly = rd.bit()
        if (frameMbsOnly == 0) rd.bit()                                       // mb_adaptive_frame_field_flag
        rd.bit()                                                              // direct_8x8_inference_flag
        var cl = 0; var cr = 0; var ct = 0; var cb = 0
        if (rd.bit() == 1) { cl = rd.ue(); cr = rd.ue(); ct = rd.ue(); cb = rd.ue() }
        val subW = if (chroma == 1 || chroma == 2) 2 else 1
        val subH = if (chroma == 1) 2 else 1
        val unitX = if (chroma == 0) 1 else subW
        val unitY = (if (chroma == 0) 1 else subH) * (2 - frameMbsOnly)
        val w = wMbs * 16 - (cl + cr) * unitX
        val h = (2 - frameMbsOnly) * hMapUnits * 16 - (ct + cb) * unitY
        if (w <= 0 || h <= 0 || w > 16384 || h > 16384) return null
        return AvcSps(profile, compat, level, w, h, chroma, bdl, bdc)
    }

    private fun skipScalingList(rd: BitReader, size: Int) {
        var last = 8; var next = 8
        for (j in 0 until size) {
            if (next != 0) next = (last + rd.se() + 256) % 256
            if (next != 0) last = next
        }
    }

    /**
     * The first slice header's slice_type of the AU's first slice NAL (0..9), or -1 when it cannot be read: 2 and 7 are I slices. Used to accept a non-IDR picture that begins a new
     * recovery point (open-GOP streams never carry an IDR).
     */
    fun sliceType(b: ByteArray, off: Int, len: Int): Int = try {
        val r = unescape(b, off + 1, minOf(len - 1, 32))
        val rd = BitReader(r, r.size)
        rd.ue()                                                               // first_mb_in_slice
        rd.ue()                                                               // slice_type
            .let { it }
    } catch (e: IllegalStateException) { -1 }

    /** True when the SEI NAL at `b[off until off+len]` carries a recovery-point message (payload type 6). */
    fun hasRecoveryPoint(b: ByteArray, off: Int, len: Int): Boolean {
        var i = off + 1; val end = off + len
        while (i < end - 1) {
            var type = 0
            while (i < end && (b[i].toInt() and 0xFF) == 0xFF) { type += 255; i++ }
            if (i >= end) return false
            type += b[i].toInt() and 0xFF; i++
            var size = 0
            while (i < end && (b[i].toInt() and 0xFF) == 0xFF) { size += 255; i++ }
            if (i >= end) return false
            size += b[i].toInt() and 0xFF; i++
            if (type == 6) return true
            i += size
            if (i < end && (b[i].toInt() and 0xFF) == 0x80) return false     // rbsp trailing bits
        }
        return false
    }

    /** The `avcC` box body (without its header) for one SPS and its PPS list. */
    fun avcC(sps: ByteArray, spsInfo: AvcSps, pps: List<ByteArray>): ByteArray {
        val o = java.io.ByteArrayOutputStream()
        o.write(1); o.write(spsInfo.profile); o.write(spsInfo.compat); o.write(spsInfo.level)
        o.write(0xFF)                                                         // 6 reserved bits + lengthSizeMinusOne = 3 (4-byte lengths)
        o.write(0xE1)                                                         // 3 reserved bits + one SPS
        o.write(sps.size shr 8); o.write(sps.size and 0xFF); o.write(sps)
        o.write(pps.size)
        for (p in pps) { o.write(p.size shr 8); o.write(p.size and 0xFF); o.write(p) }
        if (spsInfo.profile in intArrayOf(100, 110, 122, 144)) {
            o.write(0xFC or spsInfo.chromaFormat)
            o.write(0xF8 or (spsInfo.bitDepthLuma - 8))
            o.write(0xF8 or (spsInfo.bitDepthChroma - 8))
            o.write(0)                                                        // no SPS extensions
        }
        return o.toByteArray()
    }
}
