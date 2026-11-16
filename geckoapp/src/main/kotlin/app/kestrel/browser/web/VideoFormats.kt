package app.kestrel.browser.web

import android.content.Context
import app.kestrel.browser.DeviceVideo
import app.kestrel.browser.data.Prefs
import app.kestrel.core.media.VideoLimits

/**
 * ANS52g (research/VIDEO_ACCELERATION.md section "ANS52g design"): the video format policy the pages are told. The engine's own preferences cannot hide VP9 on this tablet (Gecko forces it when a hardware decoder
 * exists), so the page script (codec-main.js, world MAIN) answers `isTypeSupported`, `canPlayType` and `mediaCapabilities.decodingInfo` with "no" for what the settings and the chips say. Here only the TEXT it reads is
 * built (core/media/VideoLimits) from the two Settings rows and the limits [DeviceVideo] read from `MediaCodecInfo.VideoCapabilities`, and the Diagnostics line is written.
 */
object VideoFormats {
    /** The 5th field of the page answer. Read at every answer, so a Settings change reaches the next page that asks (the extension asks again at least once a minute). */
    fun policy(ctx: Context, prefs: Prefs): String = VideoLimits.policy(prefs.videoCodecs, prefs.videoFps30, DeviceVideo.limits(ctx))

    /** One Diagnostics line: what the setting means on this tablet, the limits it works with, and what the pages' script refused so far (its numbers, no address). */
    fun line(ctx: Context, prefs: Prefs): String {
        val said = ShieldsBridge.stats?.videoFormats.orEmpty()
        return "Video formats offered to sites (a page script answers \"no\" for what the settings and the tablet's chips rule out): " + VideoLimits.explain(prefs.videoCodecs, prefs.videoFps30, DeviceVideo.limits(ctx)) +
            "; told to pages: " + policy(ctx, prefs) + "; refused so far: " + (if (said.isEmpty()) "nothing yet (no page asked for a format outside the limits since the app started)" else said)
    }
}
