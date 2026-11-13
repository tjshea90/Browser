package app.kestrel.core.media

/**
 * What an H.265 (HEVC) SPS says that an MP4 needs. The profile / tier / level part goes into `hvcC` unchanged: [compat] holds the 32 general_profile_compatibility_flags, [constraint] the 48 bits
 * that follow them (progressive / interlaced / non-packed / frame-only flags and the reserved bits).
 */
class HevcSps(
    val profileSpace: Int, val tierFlag: Int, val profileIdc: Int, val compat: Long, val constraint: Long, val levelIdc: Int,
    val width: Int, val height: Int, val chromaFormat: Int, val bitDepthLuma: Int, val bitDepthChroma: Int,
    val maxSubLayers: Int, val temporalIdNested: Int,
)

/** H.265 byte-stream helpers for the TS-to-MP4 writer: NAL types, SPS parsing and the `hvcC` box (MP4 sample entry `hvc1`). Pure functions, tested against ffmpeg-made streams. */
object Hevc {
    const val NAL_VPS = 32
    const val NAL_SPS = 33
    const val NAL_PPS = 34
    const val NAL_AUD = 35
    const val NAL_EOS = 36
    const val NAL_EOB = 37
    const val NAL_FILLER = 38

    /** The NAL unit type of the unit whose first header byte is `b[i]` (6 bits after the forbidden-zero bit). */
    fun nalType(b: ByteArray, i: Int): Int = (b[i].toInt() shr 1) and 0x3F

    /** BLA, IDR and CRA pictures (types 16..21) can start decoding: they are the "keyframes". */
    fun isIrap(type: Int): Boolean = type in 16..21

    /** Parses the SPS NAL unit at `b[off until off+len]` (both header bytes included). Null when it cannot be read. */
    fun parseSps(b: ByteArray, off: Int, len: Int): HevcSps? = try { parse(b, off, len) } catch (e: IllegalStateException) { null }

    private fun parse(b: ByteArray, off: Int, len: Int): HevcSps? {
        if (len < 8 || nalType(b, off) != NAL_SPS) return null
        val r = Avc.unescape(b, off + 2, len - 2)
        val rd = BitReader(r, r.size)
        rd.bits(4)                                                            // sps_video_parameter_set_id
        val maxSubLayersMinus1 = rd.bits(3)
        val nested = rd.bit()
        // profile_tier_level(1, maxSubLayersMinus1)
        val space = rd.bits(2); val tier = rd.bit(); val profile = rd.bits(5)
        val compat = (rd.bits(16).toLong() shl 16) or rd.bits(16).toLong()
        val constraint = (rd.bits(16).toLong() shl 32) or (rd.bits(16).toLong() shl 16) or rd.bits(16).toLong()
        val level = rd.bits(8)
        val subProfile = BooleanArray(8); val subLevel = BooleanArray(8)
        for (i in 0 until maxSubLayersMinus1) { subProfile[i] = rd.bit() == 1; subLevel[i] = rd.bit() == 1 }
        if (maxSubLayersMinus1 > 0) for (i in maxSubLayersMinus1 until 8) rd.skip(2)
        for (i in 0 until maxSubLayersMinus1) { if (subProfile[i]) rd.skip(88); if (subLevel[i]) rd.skip(8) }
        rd.ue()                                                               // sps_seq_parameter_set_id
        val chroma = rd.ue()
        if (chroma == 3) rd.bit()                                             // separate_colour_plane_flag
        val picW = rd.ue(); val picH = rd.ue()
        var cl = 0; var cr = 0; var ct = 0; var cb = 0
        if (rd.bit() == 1) { cl = rd.ue(); cr = rd.ue(); ct = rd.ue(); cb = rd.ue() } // conformance window (cropping), in chroma units
        val bdl = rd.ue() + 8; val bdc = rd.ue() + 8
        val subW = if (chroma == 1 || chroma == 2) 2 else 1
        val subH = if (chroma == 1) 2 else 1
        val w = picW - subW * (cl + cr)
        val h = picH - subH * (ct + cb)
        if (chroma > 3 || w <= 0 || h <= 0 || w > 16384 || h > 16384 || bdl > 16 || bdc > 16) return null
        return HevcSps(space, tier, profile, compat, constraint, level, w, h, chroma, bdl, bdc, maxSubLayersMinus1 + 1, nested)
    }

    /** The `hvcC` box body (without its header): the decoder configuration with the VPS (when the stream has one), the SPS and the PPS list, 4-byte NAL lengths in the samples. */
    fun hvcC(vps: ByteArray?, sps: ByteArray, info: HevcSps, pps: List<ByteArray>): ByteArray {
        val o = java.io.ByteArrayOutputStream()
        fun u8(v: Int) = o.write(v and 0xFF)
        fun u16(v: Int) { u8(v shr 8); u8(v) }
        u8(1)                                                                 // configurationVersion
        u8((info.profileSpace shl 6) or (info.tierFlag shl 5) or info.profileIdc)
        u16((info.compat ushr 16).toInt()); u16(info.compat.toInt())
        u16((info.constraint ushr 32).toInt()); u16((info.constraint ushr 16).toInt()); u16(info.constraint.toInt())
        u8(info.levelIdc)
        u16(0xF000)                                                           // min_spatial_segmentation_idc: unknown
        u8(0xFC)                                                              // parallelismType: unknown
        u8(0xFC or info.chromaFormat)
        u8(0xF8 or (info.bitDepthLuma - 8))
        u8(0xF8 or (info.bitDepthChroma - 8))
        u16(0)                                                                // avgFrameRate: unspecified
        u8((0 shl 6) or ((info.maxSubLayers and 7) shl 3) or (info.temporalIdNested shl 2) or 3) // constantFrameRate 0, numTemporalLayers, temporalIdNested, lengthSizeMinusOne = 3
        val arrays = ArrayList<Pair<Int, List<ByteArray>>>()
        if (vps != null) arrays.add(NAL_VPS to listOf(vps))
        arrays.add(NAL_SPS to listOf(sps))
        if (pps.isNotEmpty()) arrays.add(NAL_PPS to pps)
        u8(arrays.size)
        for ((type, nals) in arrays) {
            u8(0x80 or type)                                                  // array_completeness = 1
            u16(nals.size)
            for (n in nals) { u16(n.size); o.write(n) }
        }
        return o.toByteArray()
    }
}
