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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
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
import app.kestrel.browser.web.FullscreenTurner
import app.kestrel.browser.web.NativePlayerStats
import app.kestrel.browser.web.PlayerControlsView
import app.kestrel.browser.web.QualitySheet
import app.kestrel.core.media.FullscreenTurn
import app.kestrel.core.media.QualityLabel
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
 * ANS53f (owner message 53, on alpha56: "The video opened Fullscreen in portrait orientation but stretched vertically so the aspect ratio was wrong, but then quickly switched to landscape, even though my tablet was being
 * held in portrait ... It should have played in portrait. There was no option to go from the landscape mode to portrait. Even rotating the tablet didn't change it."): the screen now FOLLOWS THE TABLET from the first
 * moment (no forced landscape for a wide film: the owner holds the tablet the way he wants), a rotate button turns it the other way for good ([FullscreenTurner], the rules of page full screen), the picture is never
 * stretched (the surface is a point until the picture's shape is known, then letterboxed to it). ANS53d: a quality button lists every picture the stream offers (resolution, frame rate, bit rate) and plays the one picked.
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
    private var shownUrl = ""                    // the film this screen plays (kept in lastUrl when it ends, for core WebHandOver.resumeMs)
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
    private lateinit var turner: FullscreenTurner
    private lateinit var sheet: QualitySheet
    private var fromPage = false                              // ANS54e: started for a film of a web page: a failure before the first picture goes back to the page (RESULT_FAILED)
    private var chosen: QualityLabel.Rung? = null          // the picture the person picked (null = Automatic)
    private var capH = 720
    private var wanted: QualityLabel.Rung? = null        // ANS53d.2: the picture picked in the list of the page's film, applied as soon as the stream says what it offers
    private var rowsNow: List<Pair<QualityLabel.Rung, Pair<Tracks.Group, Int>>> = emptyList()

    private val controlsTick = Runnable { model.controlsTimer(); flush() }
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
        fromPage = i.getBooleanExtra(EXTRA_FROM_PAGE, false)
        shownUrl = url
        NativePlayerStats.active = true
        picW = i.getIntExtra(EXTRA_W, 0); picH = i.getIntExtra(EXTRA_H, 0)
        if (url.isEmpty()) { finish(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = Prefs.get(applicationContext)
        if (prefs.fullscreenBars == 1) hideBars()
        turner = FullscreenTurner(this, { FullscreenTurn.MODE_FOLLOW }, { Pair(picW, picH) }, { priv }).also { it.ownedByPlayer = { true } }   // ANS53f: always follow the tablet; the rotate button is drawn by the controls
        turner.enter()

        stage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        surface = SurfaceView(this)
        stage.addView(surface, FrameLayout.LayoutParams(1, 1, Gravity.CENTER))              // ANS53f: a point until the shape of the picture is known (fit()): a surface that fills the screen first stretches the first frames
        controls = PlayerControlsView(this).apply { listener = this@NativePlayerActivity; showRotate = true }
        stage.addView(controls, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        sheet = QualitySheet(this).apply { listener = object : QualitySheet.Listener { override fun onPick(row: QualitySheet.Row) { pickQuality(row) } override fun onDismiss() { model.pin(false) } } }
        stage.addView(sheet, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        stage.setOnTouchListener { v, e -> onStageTouch(v, e) }
        stage.addOnLayoutChangeListener { _, l, t, r, bt, ol, ot, orr, ob -> if (r - l != orr - ol || bt - t != ob - ot) fit() }
        setContentView(stage)

        try { buildPlayer(url, i.getStringArrayExtra(EXTRA_HEADERS)?.toList().orEmpty(), i.getLongExtra(EXTRA_START_MS, 0L), i.getIntExtra(EXTRA_MAX_H, 720), QualityLabel.decode(i.getStringExtra(EXTRA_PICK)), i.getBooleanExtra(EXTRA_AUTO, false)) }
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

    private fun buildPlayer(url: String, headerLines: List<String>, startMs: Long, maxH: Int, pick: QualityLabel.Rung?, auto: Boolean) {
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
        // the picture size to start with: the one the person picked last time (ANS53d, remembered), else what the page's list said (720 when it said nothing). The cap is on the LONGER side of a 16:9 picture so that a
        // portrait film is held to the same class as a landscape one (a cap on the height alone would drop every portrait rung of a 720p class).
        // ANS53d.2: a picture picked in the page's quality list wins over what was remembered (and is remembered itself, like a pick made in this player); Automatic from that list forgets the old pick
        val prefs = Prefs.get(applicationContext)
        val remembered = prefs.nativeMaxH
        capH = when {
            pick != null -> QualityLabel.pClass(pick.w, pick.h).coerceIn(144, 2160)
            auto -> maxOf(maxH, 720).coerceIn(144, 2160)
            else -> (if (remembered > 0) remembered else maxH).coerceIn(144, 2160)
        }
        if (pick != null) { prefs.nativeMaxH = capH; wanted = pick } else if (auto) prefs.nativeMaxH = 0
        if (!priv && (pick != null || auto)) { NativePlayerStats.qualityPicks++; NativePlayerStats.lastQuality = if (pick != null) QualityLabel.text(pick) else "Automatic" }
        say("start-quality", if (pick != null) "picked in the page's list: ${QualityLabel.text(pick)} (cap ${capH}p)" else if (auto) "Automatic, picked in the page's list (cap ${capH}p)" else "no pick (cap ${capH}p)")
        applyCap(p)
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) { ended = true }
                sample()
            }
            override fun onPlayerError(error: PlaybackException) {
                if (!priv) { NativePlayerStats.errors++; NativePlayerStats.last = "error ${error.errorCodeName}" }
                say("error", "${error.errorCodeName}: ${error.message.orEmpty().take(120)}")
                sample()
                if (fromPage && firstPictureAt == 0L) { setResult(RESULT_FAILED); say("back-to-page", "failed before the first picture: the page's own player takes over"); finish() }   // ANS54e
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    picW = size.width; picH = size.height; pixelRatio = if (size.pixelWidthHeightRatio > 0f) size.pixelWidthHeightRatio else 1f
                    if (!priv) { NativePlayerStats.width = picW; NativePlayerStats.height = picH }
                    fit()                                              // (ANS53f: no turning to landscape here any more: the screen follows the tablet)
                }
            }
            override fun onTracksChanged(tracks: Tracks) { updateRows(tracks) }
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

    /** The size cap for Automatic: both sides at most 16:9 of [capH], so portrait and landscape films fall in the same class. */
    private fun applyCap(p: ExoPlayer) {
        val longCap = capH * 16 / 9
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setMaxVideoSize(longCap, longCap).setMaxVideoFrameRate(60).build()
    }

    // ---------------------------------------------------------------------------------------- ANS53d: the quality list

    /** The pictures the stream offers (every video track of every group), with where each sits, biggest first. */
    private fun updateRows(tracks: Tracks) {
        val all = ArrayList<Pair<QualityLabel.Rung, Pair<Tracks.Group, Int>>>()
        for (g in tracks.groups) {
            if (g.type != C.TRACK_TYPE_VIDEO) continue
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                if (f.width <= 0 && f.height <= 0) continue
                all.add(Pair(QualityLabel.Rung(f.width, f.height, f.frameRate, (if (f.averageBitrate > 0) f.averageBitrate else f.bitrate).toLong().coerceAtLeast(0L)), Pair(g, i)))
            }
        }
        rowsNow = all.sortedWith(compareByDescending<Pair<QualityLabel.Rung, Pair<Tracks.Group, Int>>> { QualityLabel.pClass(it.first.w, it.first.h) }.thenByDescending { it.first.fps }.thenByDescending { it.first.bps })
        val distinct = QualityLabel.rows(rowsNow.map { it.first }).size
        controls.showQuality = distinct >= 2              // one picture only (a plain file): nothing to choose
        wanted?.let { w ->
            val idx = QualityLabel.nearest(w, rowsNow.map { it.first })
            if (idx >= 0) { wanted = null; applyRung(rowsNow[idx].first, rowsNow[idx].second); say("pick-applied", "the page's pick ${QualityLabel.text(w)} -> ${QualityLabel.text(rowsNow[idx].first)}") }
        }
        say("tracks", "$distinct picture(s) on offer: ${QualityLabel.rows(rowsNow.map { it.first }).joinToString(" | ") { QualityLabel.text(it) }}")
    }

    private fun qualityRows(): List<QualitySheet.Row> {
        val p = player ?: return emptyList()
        val rows = ArrayList<QualitySheet.Row>()
        val nowFmt = p.videoFormat
        val now = QualityLabel.now(if (nowFmt != null) QualityLabel.Rung(nowFmt.width, nowFmt.height, nowFmt.frameRate, 0L) else null)
        rows.add(QualitySheet.Row("Automatic", (if (chosen == null) now else "") .ifEmpty { "the player picks, at most ${capH}p" }, chosen == null, true, null))
        var seen = HashSet<String>()
        for ((rung, where) in rowsNow) {
            val t = QualityLabel.text(rung)
            if (!seen.add(t)) continue
            val playable = where.first.isTrackSupported(where.second)
            rows.add(QualitySheet.Row(t, if (playable) "" else "this tablet cannot play it smoothly", chosen != null && QualityLabel.text(chosen!!) == t, playable, rung))
        }
        return rows
    }

    private fun openQuality() {
        val rows = qualityRows()
        if (rows.isEmpty()) return
        model.pin(true)
        sheet.show("Quality", rows, "Kestrel remembers your pick for the next films. A smaller picture and a lower frame rate are lighter for this tablet and for the hotspot.")
        say("quality-open", "${rows.size} row(s)")
    }

    private fun pickQuality(row: QualitySheet.Row) {
        val p = player ?: return
        val rung = row.tag as? QualityLabel.Rung
        try {
            val b = p.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            if (rung == null) { chosen = null; Prefs.get(applicationContext).nativeMaxH = 0; capH = maxOf(capH, 720) }
            else {
                val match = rowsNow.firstOrNull { QualityLabel.text(it.first) == row.title }
                if (match != null) b.setOverrideForType(TrackSelectionOverride(match.second.first.mediaTrackGroup, listOf(match.second.second)))
                chosen = rung
                val cls = QualityLabel.pClass(rung.w, rung.h)
                Prefs.get(applicationContext).nativeMaxH = cls
                capH = cls
            }
            p.trackSelectionParameters = b.build()
            applyCap(p)
            if (!priv) { NativePlayerStats.qualityPicks++; NativePlayerStats.lastQuality = if (rung == null) "Automatic" else QualityLabel.text(rung) }
            say("quality", if (rung == null) "Automatic" else row.title)
        } catch (e: Throwable) { say("quality-failed", e.javaClass.simpleName) }
        sheet.dismiss()
    }

    /** ANS53d.2: play exactly this picture of the stream (the one the page's list named, matched against what the stream really offers): a track override, the cap follows it. */
    private fun applyRung(rung: QualityLabel.Rung, where: Pair<Tracks.Group, Int>) {
        val p = player ?: return
        try {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).setOverrideForType(TrackSelectionOverride(where.first.mediaTrackGroup, listOf(where.second))).build()
            chosen = rung
            val cls = QualityLabel.pClass(rung.w, rung.h)
            Prefs.get(applicationContext).nativeMaxH = cls
            capH = cls
            applyCap(p)
        } catch (e: Throwable) { say("quality-failed", e.javaClass.simpleName) }
    }

    /** The engine's own user agent, so a server that looks at it sees the browser the person is using. */
    private fun userAgent(): String { val v = Flavor.engineVersion.substringBefore('.').filter { it.isDigit() }.ifEmpty { "128" }; return "Mozilla/5.0 (Android ${android.os.Build.VERSION.RELEASE}; Mobile; rv:$v.0) Gecko/$v.0 Firefox/$v.0" }

    private fun fail(why: String) {
        if (!priv) { NativePlayerStats.errors++; NativePlayerStats.last = why }
        say("failed", why)
        if (fromPage) setResult(RESULT_FAILED)
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
    override fun onRotate() { turner.rotatePressed(); if (!priv) NativePlayerStats.rotates++; say("rotate", "the rotate button") }
    override fun onQuality() { if (sheet.isOpen) sheet.dismiss() else openQuality() }

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
        if (w <= 0 || h <= 0 || picW <= 0 || picH <= 0) return                  // (shape unknown: the surface stays a point)
        val pw = picW * pixelRatio
        val scale = Math.min(w / pw, h / picH.toFloat())
        val lp = surface.layoutParams as FrameLayout.LayoutParams
        val nw = (pw * scale).toInt().coerceAtLeast(1); val nh = (picH * scale).toInt().coerceAtLeast(1)
        if (lp.width != nw || lp.height != nh) { lp.width = nw; lp.height = nh; lp.gravity = Gravity.CENTER; surface.layoutParams = lp }
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

    /** DEBUG builds only (the emulator lab drives the screen without finger coordinates): `am start --activity-single-top -n .../NativePlayerActivity --es kestrel_debug qopen|qpick|rotate [--es kestrel_arg N]`. */
    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return
        val what = i.getStringExtra("kestrel_debug") ?: return
        val n = (i.getStringExtra("kestrel_arg") ?: "").toIntOrNull() ?: 0
        when (what) {
            "qopen" -> openQuality()
            "qpick" -> { val rows = qualityRows(); if (n in rows.indices) pickQuality(rows[n]) else say("quality-failed", "no row $n of ${rows.size}") }
            "rotate" -> onRotate()
        }
    }

    override fun onBackPressed() { if (::sheet.isInitialized && sheet.isOpen) sheet.dismiss() else super.onBackPressed() }

    override fun onResume() { super.onResume(); if (::turner.isInitialized) turner.resumed() }

    // ---------------------------------------------------------------------------------------- the end

    override fun onPause() {
        // SND1: no sound when the screen is not in front
        try { player?.pause() } catch (e: Throwable) { /* gone */ }
        if (::turner.isInitialized) turner.paused()
        sample()
        super.onPause()
    }

    override fun onDestroy() {
        main.removeCallbacks(ticker); main.removeCallbacks(controlsTick)
        val p = player
        player = null
        if (p != null) {
            try {
                val dc = p.videoDecoderCounters
                if (!priv) { NativePlayerStats.framesShown += (dc?.renderedOutputBufferCount ?: 0).toLong(); NativePlayerStats.playedMs += p.currentPosition.coerceAtLeast(0L) }
                lastPositionMs = p.currentPosition.coerceAtLeast(0L)
                if (priv) lastUrl = "" else { lastUrl = shownUrl; lastDurationMs = p.duration.let { d -> if (d == C.TIME_UNSET || d < 0L) 0L else d }; lastStoppedAt = SystemClock.uptimeMillis() }   // (RAM only, never for a private tab)
                if (!priv) NativePlayerStats.last = "ended after ${(SystemClock.uptimeMillis() - startedAt) / 1000} s"
            } catch (e: Throwable) { /* gone */ }
            try { p.release() } catch (e: Throwable) { /* gone */ }
        }
        model.stop()
        if (::turner.isInitialized) turner.exit()
        say("stop", "Kestrel's own player ended")
        NativePlayerStats.active = false
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "u"
        const val EXTRA_HEADERS = "h"          // "Name: value" lines
        const val EXTRA_START_MS = "s"
        const val EXTRA_MAX_H = "mh"           // the tallest picture to choose (the stream's own quality list is filtered to it)
        const val EXTRA_PRIVATE = "p"
        const val EXTRA_PICK = "pq"            // ANS53d.2: the picture picked in the page's quality list, QualityLabel.encode
        const val EXTRA_AUTO = "au"            // ANS53d.2: Automatic picked in that list (the remembered cap is forgotten)
        const val EXTRA_W = "w"
        const val EXTRA_FROM_PAGE = "fp"        // ANS54e: started for a film of a web page (the result says whether it failed)
        const val RESULT_FAILED = 7001
        const val REQ_PAGE_FILM = 1003
        const val EXTRA_H = "hh"

        /** Where the last play stopped (for a page that wants to continue from there). */
        @Volatile var lastPositionMs = 0L
        /** The film Kestrel's own player showed last (never a private tab's), how long it is and when it ended: a page film pressed again continues from where it stopped (core WebHandOver.resumeMs). */
        @Volatile var lastUrl = ""
        @Volatile var lastDurationMs = 0L
        @Volatile var lastStoppedAt = 0L

        fun launch(from: Context, url: String, headers: List<String>, startMs: Long, maxH: Int, priv: Boolean, w: Int, h: Int, pick: QualityLabel.Rung? = null, auto: Boolean = false, fromPage: Boolean = false) {
            val i = Intent(from, NativePlayerActivity::class.java).putExtra(EXTRA_URL, url).putExtra(EXTRA_HEADERS, headers.toTypedArray()).putExtra(EXTRA_START_MS, startMs)
                .putExtra(EXTRA_MAX_H, maxH).putExtra(EXTRA_PRIVATE, priv).putExtra(EXTRA_W, w).putExtra(EXTRA_H, h)
            if (pick != null) i.putExtra(EXTRA_PICK, QualityLabel.encode(pick))
            if (auto) i.putExtra(EXTRA_AUTO, true)
            NativePlayerStats.launchedAt = SystemClock.uptimeMillis(); NativePlayerStats.launchedUrl = url
            if (fromPage && from is Activity) { i.putExtra(EXTRA_FROM_PAGE, true); from.startActivityForResult(i, REQ_PAGE_FILM) } else from.startActivity(i)
        }
    }
}
