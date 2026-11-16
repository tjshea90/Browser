package app.kestrel.browser

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaDrm
import android.os.Build
import app.kestrel.core.media.VideoPolicy
import java.util.UUID

/**
 * What the tablet's own decoders are (research/VIDEO_AUDIT.md): the Android MediaCodec list with the hardware flag of each decoder, and whether the platform has Widevine for protected video.
 * Two uses: [hardware] feeds the codec rule (core/media/VideoPolicy: a codec with no hardware decoder is not offered to websites, so they send H.264), and [report] shows the whole list in Diagnostics
 * so a choppy video can be explained from the tablet itself.
 *
 * The probe asks Android's media service (tens of ms, more on the first call after boot), so it runs on the IO thread and its answer is kept in the preferences for this build of the system image: a
 * normal start reads four letters instead of asking again. Diagnostics > Video > "Read the decoders again" forces it.
 */
object DeviceVideo {
    private class Dec(val name: String, val mime: String, val hardware: Boolean, val vendor: Boolean, val secure: Boolean, val size: String)

    private val CODECS = listOf(
        "video/avc" to "H.264", "video/hevc" to "H.265", "video/x-vnd.on2.vp9" to "VP9", "video/x-vnd.on2.vp8" to "VP8", "video/av01" to "AV1", "video/mp4v-es" to "MPEG-4", "video/mpeg2" to "MPEG-2",
        "audio/mp4a-latm" to "AAC", "audio/opus" to "Opus", "audio/vorbis" to "Vorbis", "audio/mpeg" to "MP3", "audio/ac3" to "AC-3", "audio/eac3" to "E-AC-3", "audio/flac" to "FLAC",
    )

    @Volatile private var list: List<Dec>? = null
    @Volatile private var widevine = false
    @Volatile private var hw: VideoPolicy.Hardware? = null
    @Volatile private var readMs = -1L

    private const val KEY_HW = "vid_hw"
    private const val KEY_WV = "vid_wv"
    private const val KEY_FP = "vid_fp"
    private const val KEY_DESC = "vid_desc"

    /** Hardware decoders as known right now: from this run's probe, else from the stored answer for this system build, else null (unknown). Cheap; safe on any thread. */
    fun hardware(ctx: Context): VideoPolicy.Hardware? {
        hw?.let { return it }
        val p = ctx.getSharedPreferences("kestrel_video", Context.MODE_PRIVATE)
        if (p.getString(KEY_FP, null) != Build.FINGERPRINT) return null
        return VideoPolicy.Hardware.decode(p.getString(KEY_HW, null))?.also { hw = it; widevine = p.getBoolean(KEY_WV, false) }
    }

    /** True once a probe found the Widevine scheme (the DRM prompt is only worth showing then). Unknown (never probed) counts as false until [read] ran. */
    fun widevineAvailable(): Boolean = widevine

    /** Asks Android (blocking: call off the UI thread). [force] re-reads even when this run already did. Returns true when the answer changed what [hardware] says. */
    fun read(ctx: Context, force: Boolean = false): Boolean {
        if (!force && list != null) return false
        val t0 = System.nanoTime()
        val decs = ArrayList<Dec>()
        try {
            for (info in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
                if (info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (CODECS.none { it.first.equals(type, true) }) continue
                    val caps = try { info.getCapabilitiesForType(type) } catch (e: Exception) { null }
                    var size = ""
                    var secure = false
                    if (caps != null) {
                        secure = try { caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback) } catch (e: Exception) { false }
                        val vc = caps.videoCapabilities
                        if (vc != null) size = sizeText(vc)
                    }
                    decs.add(Dec(info.name, type.lowercase(), hardware = info.isHardwareAccelerated, vendor = info.isVendor, secure = secure, size = size))
                }
            }
        } catch (e: Throwable) { app.kestrel.browser.diag.Health.log("video", "probe-failed", e.toString().take(140)) }
        list = decs
        readDrm()
        readMs = (System.nanoTime() - t0) / 1_000_000
        val fresh = VideoPolicy.Hardware(avc = has(decs, "video/avc"), hevc = has(decs, "video/hevc"), vp9 = has(decs, "video/x-vnd.on2.vp9"), av1 = has(decs, "video/av01"))
        val changed = hw?.encode() != fresh.encode()
        if (decs.isNotEmpty()) { // a probe that found nothing (service not ready) is not remembered
            hw = fresh
            ctx.getSharedPreferences("kestrel_video", Context.MODE_PRIVATE).edit().putString(KEY_HW, fresh.encode()).putBoolean(KEY_WV, widevine).putString(KEY_FP, Build.FINGERPRINT).putString(KEY_DESC, textOf(decs).take(1500)).apply()
        }
        app.kestrel.browser.diag.Health.log("video", "decoders", "${fresh.encode()} ${decs.size} decoder(s) in $readMs ms, widevine=$widevine")
        return changed
    }

    private fun has(decs: List<Dec>, mime: String) = decs.any { it.mime == mime && it.hardware }

    /**
     * ANS52 (owner message 52: "optimized for the hardware"): what the decoder CAN do at the sizes that matter, as the vendor declares it: the largest size, whether 720p and 1080p are supported at all, the highest frame
     * rate it claims there and, where the vendor measured it (media_codecs_performance.xml), the range it really achieves, plus the performance points of API 29. Words only, no address.
     */
    private fun sizeText(vc: MediaCodecInfo.VideoCapabilities): String {
        val sb = StringBuilder()
        try { sb.append(vc.supportedWidths.upper).append('x').append(vc.supportedHeights.upper) } catch (e: Exception) { sb.append('?') }
        for ((w, h, lab) in listOf(Triple(1280, 720, "720p"), Triple(1920, 1080, "1080p"))) {
            val ok = try { vc.isSizeSupported(w, h) } catch (e: Exception) { false }
            if (!ok) { sb.append(", ").append(lab).append(" not supported"); continue }
            val sup = try { vc.getSupportedFrameRatesFor(w, h).upper.toInt() } catch (e: Exception) { -1 }
            val ach = try { vc.getAchievableFrameRatesFor(w, h) } catch (e: Exception) { null }
            sb.append(", ").append(lab).append(" up to ").append(sup).append(" fps")
            if (ach != null) sb.append(" (measured ").append(ach.lower.toInt()).append('-').append(ach.upper.toInt()).append(')')
        }
        try { val pp = vc.supportedPerformancePoints; if (pp != null && pp.isNotEmpty()) sb.append(", points ").append(pp.take(3).joinToString(" ") { it.toString() }) } catch (e: Throwable) { /* not declared */ }
        return sb.toString()
    }

    /** One Diagnostics line: the video decoders of this tablet with what each can do (hardware first). Empty list = not read yet. */
    fun describe(ctx: Context): String {
        val l = list
        if (l == null) {
            val p = ctx.getSharedPreferences("kestrel_video", Context.MODE_PRIVATE)
            return if (p.getString(KEY_FP, null) == Build.FINGERPRINT) (p.getString(KEY_DESC, null) ?: "not read yet") else "not read yet"
        }
        return textOf(l)
    }

    private fun textOf(decs: List<Dec>): String {
        val video = decs.filter { d -> CODECS.take(7).any { it.first == d.mime } }
        if (video.isEmpty()) return "no video decoder found"
        fun label(m: String) = CODECS.firstOrNull { it.first == m }?.second ?: m
        val hardware = video.filter { it.hardware }.joinToString(" | ") { "${label(it.mime)} ${it.name}${if (it.secure) " (secure ok)" else ""}: ${it.size}" }
        val software = video.filter { !it.hardware }.joinToString(", ") { "${label(it.mime)} ${it.name}" }
        return "hardware: " + (hardware.ifEmpty { "none" }) + "; software only: " + (software.ifEmpty { "none" })
    }

    private fun readDrm() {
        val uuid = UUID(java.lang.Long.parseUnsignedLong("EDEF8BA979D64ACE", 16), java.lang.Long.parseUnsignedLong("A3C827DCD51D21ED", 16))
        widevine = try { MediaDrm.isCryptoSchemeSupported(uuid) } catch (e: Throwable) { false }
    }
}
