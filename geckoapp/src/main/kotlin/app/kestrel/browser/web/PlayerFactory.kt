package app.kestrel.browser.web

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import app.kestrel.browser.Flavor

/**
 * ANS55f: the ONE place that builds Kestrel's own ExoPlayer, for the full-screen screen ([app.kestrel.browser.NativePlayerActivity]) and for the player inside the page ([InlinePlayer]): the stream's source
 * (HLS or a file, with the page's request headers), the tablet's hardware decoders first, the small buffers of this 2 GB tablet. No surface, no listeners and not prepared: the caller adds those.
 */
object PlayerFactory {
    class Built(val player: ExoPlayer, val source: MediaSource)

    /** Is this address a HLS playlist? (the same test for both players) */
    fun isHls(url: String): Boolean = url.substringBefore('?').lowercase().endsWith(".m3u8") || url.contains(".m3u8?", true) || url.contains("/hls/", true)

    fun create(ctx: Context, url: String, headerLines: List<String>): Built {
        val headers = HashMap<String, String>()
        for (l in headerLines) { val c = l.indexOf(':'); if (c > 0) headers[l.substring(0, c).trim()] = l.substring(c + 1).trim() }
        val http = DefaultHttpDataSource.Factory().setUserAgent(userAgent()).setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)
        val data = DefaultDataSource.Factory(ctx, http)
        val item = MediaItem.fromUri(url)
        val source: MediaSource = if (isHls(url)) HlsMediaSource.Factory(data).createMediaSource(item) else ProgressiveMediaSource.Factory(data).createMediaSource(item)
        // hardware decoders first (a software one only when nothing else decodes the format); no extension decoders (software FFmpeg / AV1 on four A53 cores is not a plan)
        val selector = MediaCodecSelector { mime, secure, tunneling -> MediaCodecUtil.getDecoderInfos(mime, secure, tunneling).sortedBy { if (it.hardwareAccelerated && !it.softwareOnly) 0 else 1 } }
        val renderers = DefaultRenderersFactory(ctx).setEnableDecoderFallback(true).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF).setMediaCodecSelector(selector)
        // buffers: ExoPlayer's defaults allow 50 s on the Java heap; this app's heap class is 128 MB and the tablet has 2 GB: 15 to 30 s, 16 MiB at most
        val load = DefaultLoadControl.Builder().setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)).setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
            .setTargetBufferBytes(16 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).build()
        val p = ExoPlayer.Builder(ctx, renderers).setLoadControl(load).setHandleAudioBecomingNoisy(true).setAudioAttributes(AudioAttributes.DEFAULT, true).build()
        return Built(p, source)
    }

    /** The size cap for Automatic: both sides at most 16:9 of [capH], so portrait and landscape films fall in the same class. */
    fun applyCap(p: ExoPlayer, capH: Int) {
        val longCap = capH * 16 / 9
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setMaxVideoSize(longCap, longCap).setMaxVideoFrameRate(60).build()
    }

    /** The engine's own user agent, so a server that looks at it sees the browser the person is using. */
    fun userAgent(): String { val v = Flavor.engineVersion.substringBefore('.').filter { it.isDigit() }.ifEmpty { "128" }; return "Mozilla/5.0 (Android ${android.os.Build.VERSION.RELEASE}; Mobile; rv:$v.0) Gecko/$v.0 Firefox/$v.0" }
}
