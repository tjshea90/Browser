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
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
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
        // ANS60d: a connection that goes quiet on the carrier-routed hotspot is given up after 10 s (it was 20) and the player asks again: the wait before a stalled segment is tried again halves
        val http = DefaultHttpDataSource.Factory().setUserAgent(userAgent()).setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(10_000).setReadTimeoutMs(10_000)
        // RVW3 SC1: HTTP(S) only. DefaultDataSource.Factory would also open file:, content:, asset: and rawresource: addresses, and a playlist the PAGE serves can name any of them as a segment or a key (the cross-protocol
        // redirect above stays: some CDNs send https to http on purpose and a film is not secret; the host of the FIRST address is checked before the player starts, WebHandOver.hostRefused)
        val data: DataSource.Factory = http
        val item = MediaItem.fromUri(url)
        val source: MediaSource = if (isHls(url)) HlsMediaSource.Factory(data).createMediaSource(item) else ProgressiveMediaSource.Factory(data).createMediaSource(item)
        // hardware decoders first (a software one only when nothing else decodes the format); no extension decoders (software FFmpeg / AV1 on four A53 cores is not a plan)
        val selector = MediaCodecSelector { mime, secure, tunneling -> MediaCodecUtil.getDecoderInfos(mime, secure, tunneling).sortedBy { if (it.hardwareAccelerated && !it.softwareOnly) 0 else 1 } }
        val renderers = DefaultRenderersFactory(ctx).setEnableDecoderFallback(true).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF).setMediaCodecSelector(selector)
        // buffers: ExoPlayer's defaults allow 50 s on the Java heap; this app's heap class is 128 MB and the tablet has 2 GB: 15 to 30 s, 16 MiB at most
        // ANS60d: ten seconds that were already played stay in memory (from the keyframe before them), so the 10 s back button, a double tap on the left and a drag to the left (the precise seek) land at once instead of
        // fetching the same seconds again over the 5 Mbit/s hotspot (about 3 MB at 2.5 Mbit/s: it fits in the same 16 MiB)
        val load = DefaultLoadControl.Builder().setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)).setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
            .setTargetBufferBytes(16 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).setBackBuffer(BACK_BUFFER_MS, true).build()
        // ANS60d: the player's first guess of the link before it has measured it: the owner's hotspot is capped at 5 Mbit/s (ExoPlayer would guess about 5 to 6 on Wi-Fi and start one step too high, a long first wait)
        val meter = bandwidthMeter(ctx)
        // WAKE_MODE_NETWORK: while it plays the player holds a Wi-Fi lock (and a CPU wake lock) so that the Wi-Fi chip does not drop into power save between two segments (WAKE_LOCK is in the manifest)
        val p = ExoPlayer.Builder(ctx, renderers).setLoadControl(load).setBandwidthMeter(meter).setWakeMode(C.WAKE_MODE_NETWORK).setHandleAudioBecomingNoisy(true).setAudioAttributes(AudioAttributes.DEFAULT, true).build()
        return Built(p, source)
    }

    // ONE meter for the whole run (like ExoPlayer's own singleton): what the first film measured is the next film's first guess, instead of the fixed one
    private var sharedMeter: DefaultBandwidthMeter? = null
    @Synchronized private fun bandwidthMeter(ctx: Context): DefaultBandwidthMeter =
        sharedMeter ?: DefaultBandwidthMeter.Builder(ctx.applicationContext).setInitialBitrateEstimate(C.NETWORK_TYPE_WIFI, INITIAL_LINK_BPS).build().also { sharedMeter = it }

    /** ANS60d: how much already-played film is kept for backward seeks, and the link speed assumed until the player has measured one (bit/s). */
    const val BACK_BUFFER_MS = 10_000
    const val INITIAL_LINK_BPS = 4_000_000L

    /**
     * ANS60d: the first film of a run pays for asking Android's codec list (about 100 to 600 ms per question on this tablet: Diagnostics 'video decoders ... in N ms'), because Media3 asks it for every format it
     * meets and keeps the answer for the rest of the run. Asking for H.264 and AAC (what nearly every web film is) while the app is idle moves that cost out of the first press. Background thread; nothing is shown.
     */
    fun warm() {
        val tid = android.os.Process.myTid()
        val was = try { android.os.Process.getThreadPriority(tid) } catch (e: Throwable) { android.os.Process.THREAD_PRIORITY_DEFAULT }
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            MediaCodecUtil.getDecoderInfos("video/avc", false, false)
            MediaCodecUtil.getDecoderInfos("audio/mp4a-latm", false, false)
        } catch (e: Throwable) { /* nothing to warm: the player asks for itself */ }
        finally { try { android.os.Process.setThreadPriority(was) } catch (e: Throwable) { /* gone */ } }          // (a pool thread goes on to other work: its own priority back)
    }

    /** The size cap for Automatic: both sides at most 16:9 of [capH], so portrait and landscape films fall in the same class. */
    fun applyCap(p: ExoPlayer, capH: Int) {
        val longCap = capH * 16 / 9
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setMaxVideoSize(longCap, longCap).setMaxVideoFrameRate(60).build()
    }

    /** The engine's own user agent, so a server that looks at it sees the browser the person is using. */
    fun userAgent(): String { val v = Flavor.engineVersion.substringBefore('.').filter { it.isDigit() }.ifEmpty { "128" }; return "Mozilla/5.0 (Android ${android.os.Build.VERSION.RELEASE}; Mobile; rv:$v.0) Gecko/$v.0 Firefox/$v.0" }
}
