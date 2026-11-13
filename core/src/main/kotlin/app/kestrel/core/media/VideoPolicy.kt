package app.kestrel.core.media

/**
 * Which video codecs a website may be OFFERED, decided from what the device's own decoders are (research/VIDEO_AUDIT.md).
 *
 * A player picks its codec by asking the engine what it can play (`MediaSource.isTypeSupported`, `canPlayType`, `MediaCapabilities`). The engine says "yes" to VP9 / AV1 / H.265 whenever it can decode them at all,
 * which includes in SOFTWARE; four Cortex-A53 cores cannot software-decode 720p VP9 at 30 frames a second next to a page, so the picture stutters, the player's own buffering indicator flickers or stays up,
 * and the CPU is pinned. H.264 on the other hand goes through the tablet's hardware decoder. Saying "no" to a codec that would run in software makes the site send H.264 instead (YouTube, Twitch and
 * most players do), which is the cheapest thing that can be done about choppy video without touching the site.
 *
 * Pure (no Android imports) so the rule is unit-tested; the Android side only fills in [Hardware] from `MediaCodecList` and applies the answer to the engine's preferences.
 */
object VideoPolicy {
    const val AUTO = 0          // follow the device: a codec that has no hardware decoder is not offered (H.264 stays)
    const val H264_ONLY = 1     // never offer VP9, AV1 or H.265: the smoothest and the most compatible choice
    const val EVERYTHING = 2    // offer all the engine can decode (software included): smallest downloads, highest CPU

    /** Whether the device has a HARDWARE decoder (MediaCodecInfo.isHardwareAccelerated) for each codec. */
    class Hardware(val avc: Boolean, val hevc: Boolean, val vp9: Boolean, val av1: Boolean) {
        /** Stored in the preferences so a start does not have to ask Android again: four letters, upper case = present. */
        fun encode(): String = (if (avc) "A" else "a") + (if (hevc) "H" else "h") + (if (vp9) "V" else "v") + (if (av1) "I" else "i")
        companion object {
            fun decode(s: String?): Hardware? {
                if (s == null || s.length != 4) return null
                val ok = s[0] in "Aa" && s[1] in "Hh" && s[2] in "Vv" && s[3] in "Ii"
                return if (ok) Hardware(s[0] == 'A', s[1] == 'H', s[2] == 'V', s[3] == 'I') else null
            }
        }
    }

    /** `null` = leave the engine's own value (it is cleared from the user branch); true / false = force it. */
    class Decision(val vp9: Boolean?, val av1: Boolean?, val hevc: Boolean?, val why: String)

    fun decide(mode: Int, hw: Hardware?): Decision = when (mode) {
        H264_ONLY -> Decision(vp9 = false, av1 = false, hevc = false, why = "H.264 only (your choice)")
        EVERYTHING -> Decision(vp9 = true, av1 = true, hevc = true, why = "all codecs offered (your choice)")
        else -> autoDecision(hw)
    }

    private fun autoDecision(hw: Hardware?): Decision {
        // unknown device (the probe failed): do not guess, keep what the engine would do, apart from AV1 which only ever runs in software on a device this size
        if (hw == null) return Decision(vp9 = null, av1 = false, hevc = null, why = "device decoders unknown: engine defaults, no AV1")
        val parts = ArrayList<String>()
        // VP9 saves about a third of the bytes of H.264, so it stays whenever the hardware decodes it, and also when there is no hardware H.264 either (both would be software, and VP9 is the smaller download)
        val vp9 = if (hw.vp9 || !hw.avc) null else false
        parts.add(if (vp9 == false) "no VP9 (software only here)" else "VP9 as the engine decides")
        val av1 = hw.av1
        parts.add(if (av1) "AV1 on (hardware)" else "no AV1 (software only here)")
        val hevc = if (hw.hevc) null else false
        parts.add(if (hevc == false) "no H.265 (software only here)" else "H.265 as the engine decides")
        return Decision(vp9 = vp9, av1 = av1, hevc = hevc, why = parts.joinToString(", "))
    }
}
