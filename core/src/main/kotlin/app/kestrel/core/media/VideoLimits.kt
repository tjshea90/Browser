package app.kestrel.core.media

/**
 * ANS52g (research/VIDEO_ACCELERATION.md section "ANS52g design"): what the page-side video format policy (codec-main.js) is told. The page script says "no" to a video format that the device's HARDWARE decoder
 * cannot do (a picture bigger than it declares, or more macroblocks a second than it declares), and, as the owner's setting, to every codec but H.264 or to every frame rate above 30; this object only turns the
 * device's numbers and the two Settings into the one short text the script reads. The deciding itself is in the script (tested in headless Chromium by web-tests/codec.test.js); here the text is built and
 * explained for Diagnostics. Pure Kotlin (no Android imports): the Android side reads `MediaCodecInfo.VideoCapabilities` and fills in [Limit].
 *
 * The text: `<mode><fps30>|<avc>|<hevc>|<vp9>|<av1>`. mode 0 = Automatic (a codec is refused above its hardware limit), 1 = H.264 only, 2 = everything; fps30 1 = no frame rate above 30. Each codec is
 * `-` (no hardware decoder, or not known: the engine's own answer stands) or `<longest side>x<shortest side>@<macroblocks per second>`.
 */
object VideoLimits {
    const val AUTO = VideoPolicy.AUTO
    const val H264_ONLY = VideoPolicy.H264_ONLY
    const val EVERYTHING = VideoPolicy.EVERYTHING

    /** The most a hardware decoder declares for one codec: the longest and the shortest side of the largest picture, and how many 16 x 16 macroblocks a second (1080p30 = 8160 * 30 = 244800). */
    class Limit(val longSide: Int, val shortSide: Int, val blocksPerSecond: Int) {
        fun encode(): String = "${longSide}x${shortSide}@$blocksPerSecond"

        /** The larger of two limits of the same codec (two hardware decoders: the better one is what a site can get). */
        fun max(o: Limit): Limit = Limit(maxOf(longSide, o.longSide), maxOf(shortSide, o.shortSide), maxOf(blocksPerSecond, o.blocksPerSecond))

        /** 1080p at about N frames a second, for words: how many frames a second this limit allows at 1920 x 1088 (rounded down). */
        fun fps1080(): Int = blocksPerSecond / blocks(1920, 1080)

        companion object {
            private val SHAPE = Regex("""(\d{1,5})x(\d{1,5})@(\d{1,9})""")
            fun decode(s: String?): Limit? {
                val m = SHAPE.matchEntire(s ?: return null) ?: return null
                val a = m.groupValues[1].toInt(); val b = m.groupValues[2].toInt(); val c = m.groupValues[3].toInt()
                return if (a > 0 && b > 0 && c > 0) Limit(maxOf(a, b), minOf(a, b), c) else null
            }

            /**
             * From what Android reports for one decoder: the largest picture (width and height) and the highest frame rate it declares at one REFERENCE size (the declared rate times that size's
             * macroblocks is the decoder's macroblock rate). Numbers that cannot be right give null (the engine's own answer then stands).
             */
            fun of(maxWidth: Int, maxHeight: Int, refWidth: Int, refHeight: Int, fpsAtRef: Double): Limit? {
                if (maxWidth <= 0 || maxHeight <= 0 || refWidth <= 0 || refHeight <= 0 || !(fpsAtRef > 0.0) || fpsAtRef > 100000.0) return null
                val bps = Math.round(fpsAtRef * blocks(refWidth, refHeight)).coerceAtMost(900_000_000L)
                if (bps <= 0L) return null
                return Limit(maxOf(maxWidth, maxHeight), minOf(maxWidth, maxHeight), bps.toInt())
            }
        }
    }

    /** What the hardware decoders of this device can do per codec; null = no hardware decoder of that codec (or not known). */
    class Limits(val avc: Limit?, val hevc: Limit?, val vp9: Limit?, val av1: Limit?) {
        /** Stored between runs: the four limits, `;` apart, `-` for none. */
        fun encode(): String = listOf(avc, hevc, vp9, av1).joinToString(";") { it?.encode() ?: "-" }

        companion object {
            fun decode(s: String?): Limits? {
                val p = (s ?: return null).split(';')
                if (p.size != 4) return null
                val l = p.map { if (it == "-") null else (Limit.decode(it) ?: return null) }
                return Limits(l[0], l[1], l[2], l[3])
            }
        }
    }

    /** The 16 x 16 macroblocks of a picture (a picture is cut up in whole blocks: 1080 lines are 68 rows). */
    fun blocks(width: Int, height: Int): Int = ((width + 15) / 16) * ((height + 15) / 16)

    /** The text for the page script. [limits] null (not read yet) = no limit known for any codec: the modes that need none still work. */
    fun policy(mode: Int, fps30: Boolean, limits: Limits?): String {
        val m = if (mode in AUTO..EVERYTHING) mode else AUTO
        fun f(l: Limit?) = l?.encode() ?: "-"
        return "$m${if (fps30) 1 else 0}|${f(limits?.avc)}|${f(limits?.hevc)}|${f(limits?.vp9)}|${f(limits?.av1)}"
    }

    /** Words for Diagnostics: the setting, what it means on this device and the limits it works with. */
    fun explain(mode: Int, fps30: Boolean, limits: Limits?): String {
        val parts = ArrayList<String>()
        parts.add(when (mode) {
            H264_ONLY -> "H.264 only (every other codec is answered no)"
            EVERYTHING -> "everything the engine can decode (no codec is answered no)"
            else -> "Automatic (a codec is answered no above what its hardware decoder declares)"
        })
        if (mode != EVERYTHING) {
            if (limits == null) parts.add("hardware limits not read yet")
            else {
                fun d(name: String, l: Limit?) = if (l == null) "$name none" else "$name up to ${l.longSide}x${l.shortSide}, 1080p ${l.fps1080()} fps"
                parts.add(listOf(d("H.264", limits.avc), d("H.265", limits.hevc), d("VP9", limits.vp9), d("AV1", limits.av1)).joinToString(", "))
            }
        }
        parts.add(if (fps30) "frame rates above 30 are answered no" else "frame rate not capped")
        return parts.joinToString("; ")
    }
}
