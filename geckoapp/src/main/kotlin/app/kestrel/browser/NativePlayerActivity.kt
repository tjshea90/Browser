package app.kestrel.browser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaCodecList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.diag.Health
import app.kestrel.browser.web.NativePlayerStats
import app.kestrel.browser.web.PlayerControlsView
import app.kestrel.core.media.FullscreenTurn
import app.kestrel.core.player.PlayerModel
import app.kestrel.core.player.PlayerRules

/**
 * ANS52i (owner message 52: "Research how to make the video smooth and optimized for the hardware"; research/VIDEO_ACCELERATION.md plan item 6): Kestrel's OWN player for a film whose address the page showed (an HLS playlist or
 * a plain file): Media3 ExoPlayer on a SurfaceView, decoded by the tablet's hardware decoder and shown as its own layer (what Chrome does with a full-screen film and the engine cannot do on Android), with NO page in the
 * picture's way: no page script in the video path, no engine compositor, and while this screen is in front the page behind is stopped by Android itself (its activity is paused).
 *
 * It is a separate screen (its own window) so a turn of the tablet resizes ONE SurfaceView and nothing else (no engine handshake, no page reflow). The controls are the same as in full screen ([PlayerControlsView] and the pure
 * [PlayerModel] of core/player): a tap shows or hides them, a double tap on a side jumps 10 s, the middle plays or pauses; here the commands go to the player instead of to a page.
 * Media3 1.11.1 is the version GeckoView already carries (no new copy rides in the APK). Never for DRM, never for a stream that has no address of its own (an MSE blob): the page's own player stays the fallback.
 *
 * Started with [launch]; ends with Back or the exit button; sound stops when the screen is left (SND1); a private tab's film is not counted or logged.
 */
class NativePlayerActivity : Activity(), PlayerModel.Host, PlayerControlsView.Listener {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var stage: FrameLayout
    private lateinit var surface: SurfaceView
    private lateinit var controls: PlayerControlsView
    private var player: ExoPlayer? = null
    private val model = PlayerModel(this)
    private var priv = false
    private var ack = 0
    private var muted = false
    private var startedAt = 0L
    private var firstPictureAt = 0L
    private var picW = 0
    private var picH = 0
    private var pixelRatio = 1f
    private var ended = false
    private var downX = 0f
    private var downY = 0f
    private var downT = 0L
    private var framesAtStart = 0L

    private val controlsTick = Runnable { model.controlsTimer(); flush() }
    private val tapTick = Runnable { model.tapTimer(); flush() }
    private val ticker = object : Runnable {
        override fun run() {
            sample()
            main.postDelayed(this, if (model.shown) 250L else 1000L)
        }
    }

    private fun say(event: String, text: String) { if (!priv) Health.log("native", event, text) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val i = intent
        val url = i.getStringExtra(EXTRA_URL).orEmpty()
        priv = i.getBooleanExtra(EXTRA_PRIVATE, false)
        picW = i.getIntExtra(EXTRA_W, 0); picH = i.getIntExtra(EXTRA_H, 0)
        if (url.isEmpty()) { finish(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = Prefs.get(applicationContext)
        if (prefs.fullscreenBars == 1) hideBars()
        turnFor(picW, picH, prefs.fullscreenTurn)

        stage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        surface = SurfaceView(this)
        stage.addView(surface, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        controls = PlayerControlsView(this).apply { listener = this@NativePlayerActivity }
        stage.addView(controls, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        stage.setOnTouchListener { v, e -> onStageTouch(v, e) }
        stage.addOnLayoutChangeListener { _, l, t, r, bt, ol, ot, orr, ob -> if (r - l != orr - ol || bt - t != ob - ot) fit() }
        setContentView(stage)

        try { buildPlayer(url, i.getStringArrayExtra(EXTRA_HEADERS)?.toList().orEmpty(), i.getLongExtra(EXTRA_START_MS, 0L), i.getIntExtra(EXTRA_MAX_H, 720)) }
        catch (e: Throwable) { fail("the player could not start: ${e.javaClass.simpleName}"); return }
        startedAt = SystemClock.uptimeMillis()
        NativePlayerStats.firstPictureMs = -1L
        if (!priv) { NativePlayerStats.plays++; NativePlayerStats.last = "playing" }
        model.start(true)
        controls.visibility = View.VISIBLE
        controls.update(model.view())
        main.post(ticker)
        say("start", "Kestrel's own player started (${if (url.contains(".m3u8", true)) "HLS" else "a file"})")
    }

    // ---------------------------------------------------------------------------------------- the player

    private fun buildPlayer(url: String, headerLines: List<String>, startMs: Long, maxH: Int) {
        val headers = HashMap<String, String>()
        for (l in headerLines) { val c = l.indexOf(':'); if (c > 0) headers[l.substring(0, c).trim()] = l.substring(c + 1).trim() }
        val http = DefaultHttpDataSource.Factory().setUserAgent(userAgent()).setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)
        val data = DefaultDataSource.Factory(this, http)
        val item = MediaItem.fromUri(url)
        val hls = url.substringBefore('?').lowercase().endsWith(".m3u8") || url.contains(".m3u8?", true) || url.contains("/hls/", true)
        val source: MediaSource = if (hls) HlsMediaSource.Factory(data).createMediaSource(item) else ProgressiveMediaSource.Factory(data).createMediaSource(item)
        // hardware decoders first (a software one only when nothing else decodes the format); no extension decoders (software FFmpeg / AV1 on four A53 cores is not a plan)
        val selector = MediaCodecSelector { mime, secure, tunneling -> MediaCodecUtil.getDecoderInfos(mime, secure, tunneling).sortedBy { if (it.hardwareAccelerated && !it.softwareOnly) 0 else 1 } }
        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF).setMediaCodecSelector(selector)
        // buffers: ExoPlayer's defaults allow 50 s on the Java heap; this app's heap class is 128 MB and the tablet has 2 GB: 15 to 30 s, 16 MiB at most
        val load = DefaultLoadControl.Builder().setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)).setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
            .setTargetBufferBytes(16 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false).build()
        val p = ExoPlayer.Builder(this, renderers).setLoadControl(load).setHandleAudioBecomingNoisy(true).setAudioAttributes(AudioAttributes.DEFAULT, true).build()
        val cap = maxH.coerceIn(240, 1080)
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, cap).setMaxVideoFrameRate(60).build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) { ended = true }
                sample()
            }
            override fun onPlayerError(error: PlaybackException) {
                if (!priv) { NativePlayerStats.errors++; NativePlayerStats.last = "error ${error.errorCodeName}" }
                say("error", "${error.errorCodeName}: ${error.message.orEmpty().take(120)}")
                sample()
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    val first = picW == 0
                    picW = size.width; picH = size.height; pixelRatio = if (size.pixelWidthHeightRatio > 0f) size.pixelWidthHeightRatio else 1f
                    if (!priv) { NativePlayerStats.width = picW; NativePlayerStats.height = picH }
                    if (first) turnFor(picW, picH, Prefs.get(applicationContext).fullscreenTurn)
                    fit()
                }
            }
        })
        p.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                val hw = try { MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.firstOrNull { it.name == decoderName }?.isHardwareAccelerated } catch (e: Throwable) { null }
                if (!priv) { NativePlayerStats.decoder = decoderName; NativePlayerStats.decoderHardware = hw }
                say("decoder", "$decoderName ${when (hw) { true -> "(hardware)"; false -> "(software)"; else -> "" }}, ready in $initializationDurationMs ms")
            }
            override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) { if (!priv) NativePlayerStats.framesDropped += droppedFrames }
            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
                if (firstPictureAt == 0L) {
                    firstPictureAt = SystemClock.uptimeMillis()
                    val ms = firstPictureAt - startedAt
                    if (!priv) NativePlayerStats.firstPictureMs = ms
                    say("first-picture", "the first picture came $ms ms after the start")
                }
            }
        })
        p.setVideoSurfaceView(surface)
        p.setMediaSource(source)
        if (startMs > 0L) p.seekTo(startMs)
        p.prepare()
        p.playWhenReady = true
        player = p
    }

    /** The engine's own user agent, so a server that looks at it sees the browser the person is using. */
    private fun userAgent(): String { val v = Flavor.engineVersion.substringBefore('.').filter { it.isDigit() }.ifEmpty { "128" }; return "Mozilla/5.0 (Android ${android.os.Build.VERSION.RELEASE}; Mobile; rv:$v.0) Gecko/$v.0 Firefox/$v.0" }

    private fun fail(why: String) {
        if (!priv) { NativePlayerStats.errors++; NativePlayerStats.last = why }
        say("failed", why)
        finish()
    }

    // ---------------------------------------------------------------------------------------- state -> the controls

    private fun sample() {
        val p = player ?: return
        val state = p.playbackState
        val dur = p.duration
        val live = p.isCurrentMediaItemLive
        val lengthS = if (live) -2.0 else if (dur == C.TIME_UNSET || dur <= 0L) -1.0 else dur / 1000.0
        val size = p.videoSize
        model.onSample(PlayerModel.Sample(
            timeS = p.currentPosition / 1000.0, lengthS = lengthS, bufferedS = p.bufferedPosition / 1000.0,
            paused = !p.playWhenReady, ended = state == Player.STATE_ENDED, muted = muted, volume = if (muted) 0 else 100, rate = p.playbackParameters.speed.toDouble(),
            ready = when (state) { Player.STATE_READY, Player.STATE_ENDED -> 4; Player.STATE_BUFFERING -> 2; else -> 0 }, mse = false, live = live,
            width = size.width, height = size.height, atMs = SystemClock.uptimeMillis(), ack = ack, error = p.playerError != null))
        controls.update(model.view())
    }

    /** The commands the controls made, carried out on the player; the next sample says which was the last one done. */
    private fun flush() {
        for (c in model.drain()) apply(c)
    }

    private fun apply(c: PlayerModel.Cmd) {
        val p = player ?: return
        try {
            when (c.a) {
                "play" -> { if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L); ended = false; p.play() }
                "pause" -> p.pause()
                "seek" -> p.seekTo((c.t * 1000).toLong().coerceAtLeast(0L))
                "skip" -> {
                    val dur = p.duration
                    var to = (p.currentPosition + (c.d * 1000).toLong()).coerceAtLeast(0L)
                    if (dur != C.TIME_UNSET && dur > 0L) to = to.coerceAtMost(dur)
                    p.seekTo(to)
                }
                "mute" -> { muted = c.on; p.volume = if (muted) 0f else 1f }
                "rate" -> p.setPlaybackSpeed(c.r.toFloat())
            }
        } catch (e: Throwable) { say("cmd-failed", "${c.a}: ${e.javaClass.simpleName}") }
        ack = c.id
        say("cmd", c.json())
        sample()
    }

    // ---------------------------------------------------------------------------------------- PlayerModel.Host

    override fun now() = SystemClock.uptimeMillis()
    override fun scheduleControls(ms: Long) { main.removeCallbacks(controlsTick); main.postDelayed(controlsTick, ms) }
    override fun cancelControls() { main.removeCallbacks(controlsTick) }
    override fun scheduleTap(ms: Long) { main.removeCallbacks(tapTick); main.postDelayed(tapTick, ms) }
    override fun cancelTap() { main.removeCallbacks(tapTick) }
    override fun applyControls(shown: Boolean, animate: Boolean) { say(if (shown) "shown" else "hidden", if (animate) "fade" else "at once"); controls.show(shown, animate) }
    override fun changed() { controls.update(model.view()) }
    override fun feedback(zone: PlayerRules.Zone, burst: Int) { controls.flash(zone, burst) }
    override fun log(event: String, text: String) { say(event, text) }

    // ---------------------------------------------------------------------------------------- PlayerControlsView.Listener

    override fun onPlayPause() { model.playPause(); flush() }
    override fun onSkip(forward: Boolean) { model.skipButton(forward); flush() }
    override fun onMute() { model.mute(); flush() }
    override fun onExit() { finish() }
    override fun onSeek(fraction: Double) { model.seekFraction(fraction); flush() }
    override fun onPin(on: Boolean) { model.pin(on) }

    /** A tap on the picture (anything the controls themselves did not take): the model decides show / hide / double-tap seek. */
    private fun onStageTouch(v: View, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; downT = e.eventTime }
            MotionEvent.ACTION_UP -> {
                val slop = 14f * resources.displayMetrics.density
                if (e.eventTime - downT <= 450L && Math.abs(e.x - downX) <= slop && Math.abs(e.y - downY) <= slop && v.width > 0) {
                    model.onTap((e.x * 1000f / v.width).toInt()); flush()
                }
            }
        }
        return true
    }

    // ---------------------------------------------------------------------------------------- the screen

    /** The picture in the middle of the screen at its own shape (black bars around it); the controls fill the whole screen. */
    private fun fit() {
        if (!::stage.isInitialized || !::surface.isInitialized) return
        val w = stage.width; val h = stage.height
        if (w <= 0 || h <= 0 || picW <= 0 || picH <= 0) return
        val pw = picW * pixelRatio
        val scale = Math.min(w / pw, h / picH.toFloat())
        val lp = surface.layoutParams as FrameLayout.LayoutParams
        val nw = (pw * scale).toInt().coerceAtLeast(1); val nh = (picH * scale).toInt().coerceAtLeast(1)
        if (lp.width != nw || lp.height != nh) { lp.width = nw; lp.height = nh; lp.gravity = Gravity.CENTER; surface.layoutParams = lp }
    }

    /** A wide film turns the screen sideways (both landscape ways), any other follows the tablet: the rules of full screen (core FullscreenTurn). */
    private fun turnFor(w: Int, h: Int, mode: Int) {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val plan = FullscreenTurn.onEnter(mode, null, w, h, landscape)
        requestedOrientation = if (plan.request == FullscreenTurn.UNSPECIFIED) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR else plan.request
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        say("config", "orientation=${if (c.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"}")
        fit()
    }

    // ---------------------------------------------------------------------------------------- the end

    override fun onPause() {
        // SND1: no sound when the screen is not in front
        try { player?.pause() } catch (e: Throwable) { /* gone */ }
        sample()
        super.onPause()
    }

    override fun onDestroy() {
        main.removeCallbacks(ticker); main.removeCallbacks(controlsTick); main.removeCallbacks(tapTick)
        val p = player
        player = null
        if (p != null) {
            try {
                val dc = p.videoDecoderCounters
                if (!priv) { NativePlayerStats.framesShown += (dc?.renderedOutputBufferCount ?: 0).toLong(); NativePlayerStats.playedMs += p.currentPosition.coerceAtLeast(0L) }
                lastPositionMs = p.currentPosition.coerceAtLeast(0L)
                if (!priv) NativePlayerStats.last = "ended after ${(SystemClock.uptimeMillis() - startedAt) / 1000} s"
            } catch (e: Throwable) { /* gone */ }
            try { p.release() } catch (e: Throwable) { /* gone */ }
        }
        model.stop()
        say("stop", "Kestrel's own player ended")
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "u"
        const val EXTRA_HEADERS = "h"          // "Name: value" lines
        const val EXTRA_START_MS = "s"
        const val EXTRA_MAX_H = "mh"           // the tallest picture to choose (the stream's own quality list is filtered to it)
        const val EXTRA_PRIVATE = "p"
        const val EXTRA_W = "w"
        const val EXTRA_H = "hh"

        /** Where the last play stopped (for a page that wants to continue from there). */
        @Volatile var lastPositionMs = 0L

        fun launch(from: Context, url: String, headers: List<String>, startMs: Long, maxH: Int, priv: Boolean, w: Int, h: Int) {
            val i = Intent(from, NativePlayerActivity::class.java).putExtra(EXTRA_URL, url).putExtra(EXTRA_HEADERS, headers.toTypedArray()).putExtra(EXTRA_START_MS, startMs)
                .putExtra(EXTRA_MAX_H, maxH).putExtra(EXTRA_PRIVATE, priv).putExtra(EXTRA_W, w).putExtra(EXTRA_H, h)
            from.startActivity(i)
        }
    }
}
