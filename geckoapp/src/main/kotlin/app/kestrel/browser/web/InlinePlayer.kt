package app.kestrel.browser.web

import android.content.Context
import android.graphics.Color
import android.media.MediaCodecList
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import app.kestrel.browser.NativePlayerActivity
import app.kestrel.browser.diag.Health
import app.kestrel.core.media.InlineBox
import app.kestrel.core.player.GestureMath
import app.kestrel.core.player.PlayerModel
import app.kestrel.core.player.PlayerRules

/**
 * ANS55f (owner message 55: "Can the player be inside the website so the video plays but I still see the website around it?"; research/ANS55_INLINE_PLAYER.md): Kestrel's own player drawn OVER the page's own video box.
 * The same engine as the full-screen screen ([NativePlayerActivity]: Media3 ExoPlayer on a SurfaceView, the tablet's hardware decoder, the same [PlayerModel] and [PlayerControlsView]), but a view in the page area:
 * the picture is a SurfaceView with the media-overlay z-order (above the engine's own surface, below every other view of the window), the controls are Canvas drawing over it, and [InlineHost] moves the whole
 * view as the page scrolls (the engine's compositor says how far, never the page's script). It takes the touches of its box (a tap shows or hides the controls, a double tap on a side jumps 10 s) and gives a
 * drag or a pinch that starts on it to the page, so the page still scrolls. A close x ends it, the full-screen button hands the film to the full-screen screen at the same second.
 *
 * Never started without the person's tap (VideoFlow checks that). A picture that never comes (7 s of playing) or an error before the first picture ends it and the page's own player takes the film.
 * Sound stops when it ends (it is released at once: SND1). A private tab's film is not counted or logged.
 */
class InlinePlayer(ctx: Context) : FrameLayout(ctx), PlayerModel.Host, PlayerControlsView.Listener {
    interface Listener {
        /** The player ended. [fullscreen] = the full-screen button was pressed (the film goes on in the full-screen screen), [failed] = no picture ever came (the page's own player takes the film). */
        fun onEnded(why: String, positionMs: Long, fullscreen: Boolean, failed: Boolean)
        /** A drag or a pinch began on the picture: [e] is the touch in this view's own coordinates, to give to the page (the host moves it into the engine view's). */
        fun toPage(e: MotionEvent)
        /** A finger rested on the picture (a long press, LP1): the download options of the film open, as for a long press on the page's own video. */
        fun onLongPress()
    }

    var host: Listener? = null
    private val main = Handler(Looper.getMainLooper())
    private val surface = SurfaceView(ctx).apply { setZOrderMediaOverlay(true) }   // above the engine's own surface (a regular one), still below the window's own views
    private val controls = PlayerControlsView(ctx).apply { inline = true }
    private var player: ExoPlayer? = null
    private val model = PlayerModel(this)
    private var priv = false
    var url = ""; private set
    private var ack = 0
    private var muted = false
    private var startedAt = 0L
    private var firstPictureAt = 0L
    private var playingSince = 0L
    private var picW = 0
    private var picH = 0
    private var pixelRatio = 1f
    private var pausedByEdge = false
    private var downX = 0f
    private var downY = 0f
    private var downT = 0L
    private var passing = false
    private var downCopy: MotionEvent? = null
    private var longFired = false
    // ANS57 (owner message 57): VLC-style gestures. In the page a plain drag scrolls the PAGE, so every gesture here needs the long press first (VideoGestures holdFirst)
    private var gestures: VideoGestures? = null
    private fun gesturesOrNull(): VideoGestures? {
        if (!app.kestrel.browser.data.Prefs.get(context.applicationContext).videoGestures) return null
        gestures?.let { return it }
        val a = context as? android.app.Activity ?: return null
        val g = VideoGestures(a, object : VideoGestures.Surface {
            override val viewWidth: Int get() = this@InlinePlayer.width
            override val viewHeight: Int get() = this@InlinePlayer.height
            override fun positionMs(): Long = this@InlinePlayer.positionMs
            override fun lengthMs(): Long { val p = player ?: return 0L; if (p.isCurrentMediaItemLive) return 0L; val d = p.duration; return if (d == C.TIME_UNSET || d <= 0L) 0L else d }
            override fun tap(xFraction: Double) { model.onTap((xFraction * 1000).toInt()); flush() }
            override fun seekBy(deltaMs: Long) { model.skip(deltaMs / 1000.0); flush(); say("gesture-seek", "the precise seek moved the film by ${deltaMs / 1000} s") }
            override fun showInfo(text: String?) { controls.showInfo(text) }
            override fun showLevel(kind: GestureMath.Level, fraction: Float) { controls.showLevel(kind, fraction); say("gesture-level", "${kind.name.lowercase()} ${Math.round(fraction * 100)} %") }
            override fun haptic() { performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS) }
            override fun gestureChanged(active: Boolean) { if (active) { model.gestureBegan() } }
            override fun passThrough() { startPass() }
            override fun longHold() { if (!longFired) { longFired = true; performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); say("long-press", "a finger rested on the picture: the download options"); host?.onLongPress() } }
        }, holdFirst = true, edgeDp = 6f)
        gestures = g
        return g
    }
    private val density = resources.displayMetrics.density
    private val longPressRun = Runnable { if (!passing && player != null && !longFired) { longFired = true; performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); say("long-press", "a finger rested on the picture: the download options"); host?.onLongPress() } }

    val active: Boolean get() = player != null
    val positionMs: Long get() = try { player?.currentPosition?.coerceAtLeast(0L) ?: 0L } catch (e: Throwable) { 0L }

    private val controlsTick = Runnable { model.controlsTimer(); flush() }
    private val ticker = object : Runnable {
        override fun run() {
            sample()
            if (player != null) main.postDelayed(this, if (model.shown) 250L else 1000L)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        visibility = View.GONE
        isClickable = true                         // the touches of its box are its own: a tap that reached the page's own (paused) element would start the page's player again
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(surface, LayoutParams(1, 1, Gravity.CENTER))               // a point until the shape of the picture is known (fit()): a surface that fills the box first would show a stretched frame
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        controls.listener = this
        addOnLayoutChangeListener { _, l, t, r, b, ol, ot, orr, ob -> if (r - l != orr - ol || b - t != ob - ot) fit() }
    }

    private fun say(event: String, text: String) { if (!priv) Health.log("inline", event, text) }

    // ---------------------------------------------------------------------------------------- start / stop

    /** Starts [url] over [box] (view pixels), [startMs] in. false = could not start (the caller uses the full-screen screen). [capH]: the tallest picture to choose (the box is small: 480 is plenty). */
    fun start(url: String, headerLines: List<String>, startMs: Long, priv: Boolean, box: InlineBox.Rect, capH: Int): Boolean {
        if (player != null) stop("a new film")
        this.url = url; this.priv = priv
        ack = 0; muted = false; firstPictureAt = 0L; playingSince = 0L; picW = 0; picH = 0; pixelRatio = 1f; pausedByEdge = false; passing = false
        startedAt = SystemClock.uptimeMillis()
        place(box)
        var p: ExoPlayer? = null
        try {
            val built = PlayerFactory.create(context, url, headerLines)
            p = built.player
            PlayerFactory.applyCap(p, capH)
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { sample() }
                override fun onPlayerError(error: PlaybackException) {
                    if (!priv) { NativePlayerStats.errors++; NativePlayerStats.last = "error ${error.errorCodeName}" }
                    say("error", "${error.errorCodeName}: ${error.message.orEmpty().take(120)}")
                    sample()
                    if (firstPictureAt == 0L) stop("failed before the first picture", failed = true)         // ANS54e rule: the page's own player takes the film
                }
                override fun onVideoSizeChanged(size: VideoSize) {
                    if (size.width > 0 && size.height > 0) {
                        picW = size.width; picH = size.height; pixelRatio = if (size.pixelWidthHeightRatio > 0f) size.pixelWidthHeightRatio else 1f
                        if (!priv) { NativePlayerStats.width = picW; NativePlayerStats.height = picH }
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
            p.setMediaSource(built.source)
            if (startMs > 0L) p.seekTo(startMs)
            p.prepare()
            p.playWhenReady = true
        } catch (e: Throwable) {
            say("failed", "the player could not start: ${e.javaClass.simpleName}")
            try { p?.release() } catch (x: Throwable) { /* gone */ }
            return false
        }
        player = p
        NativePlayerStats.firstPictureMs = -1L
        if (!priv) { NativePlayerStats.plays++; NativePlayerStats.last = "playing inside the page"; WebHandStats.inline++ }
        keepScreenOn = true
        visibility = View.VISIBLE
        model.start(true)
        controls.update(model.view())
        main.post(ticker)
        say("start", "Kestrel's own player started inside the page (${if (PlayerFactory.isHls(url)) "HLS" else "a file"}) over ${box} from ${startMs / 1000} s")
        return true
    }

    /** Ends the film: released at once (no sound, no decoder). [fullscreen] and [failed] go to the host with the position, which is also kept for "press again continues" (RAM only, never a private tab's). */
    fun stop(why: String, fullscreen: Boolean = false, failed: Boolean = false) {
        val p = player ?: return
        player = null
        main.removeCallbacks(ticker); main.removeCallbacks(controlsTick); main.removeCallbacks(longPressRun)
        gestures?.stop()                                                           // ANS57: a gesture in progress ends, the window's brightness is the tablet's again
        var pos = 0L
        try {
            pos = p.currentPosition.coerceAtLeast(0L)
            val dc = p.videoDecoderCounters
            if (!priv) NativePlayerStats.framesShown += (dc?.renderedOutputBufferCount ?: 0).toLong(); if (!priv) NativePlayerStats.playedMs += pos
            NativePlayerActivity.lastPositionMs = pos
            if (priv) NativePlayerActivity.lastUrl = "" else { NativePlayerActivity.lastUrl = url; NativePlayerActivity.lastDurationMs = p.duration.let { d -> if (d == C.TIME_UNSET || d < 0L) 0L else d }; NativePlayerActivity.lastStoppedAt = SystemClock.uptimeMillis() }
            if (!priv) NativePlayerStats.last = "ended inside the page after ${(SystemClock.uptimeMillis() - startedAt) / 1000} s"
        } catch (e: Throwable) { /* gone */ }
        try { p.release() } catch (e: Throwable) { /* gone */ }
        model.stop()
        keepScreenOn = false
        visibility = View.GONE
        downCopy?.recycle(); downCopy = null; passing = false
        say("stop", "$why (at ${pos / 1000} s)")
        host?.onEnded(why, pos, fullscreen, failed)
        url = ""
    }

    // ---------------------------------------------------------------------------------------- where it sits

    /** The box (view pixels, in the page area) the whole view covers; the picture is letterboxed inside it. */
    fun place(box: InlineBox.Rect) {
        val lp = (layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(box.w, box.h, Gravity.TOP or Gravity.LEFT)
        if (lp.width != box.w || lp.height != box.h || lp.gravity != (Gravity.TOP or Gravity.LEFT)) { lp.width = box.w; lp.height = box.h; lp.gravity = Gravity.TOP or Gravity.LEFT; layoutParams = lp }
        translationX = box.l.toFloat(); translationY = box.t.toFloat()
    }

    /** The page scrolled: the same box, somewhere else (no layout, only the place changes). */
    fun move(box: InlineBox.Rect) { translationX = box.l.toFloat(); translationY = box.t.toFloat() }

    /** What the box's place in the view asks of the film: keep playing, pause (most of it is out of view), or end it (all of it is). */
    fun edge(e: InlineBox.Edge) {
        val p = player ?: return
        when (e) {
            InlineBox.Edge.PLAY -> if (pausedByEdge) { pausedByEdge = false; try { p.play() } catch (x: Throwable) { /* gone */ }; say("edge", "back in view: playing again") }
            InlineBox.Edge.PAUSE -> if (!pausedByEdge && p.playWhenReady) { pausedByEdge = true; try { p.pause() } catch (x: Throwable) { /* gone */ }; say("edge", "mostly out of view: paused") }
            InlineBox.Edge.STOP -> stop("scrolled out of view")
        }
    }

    /** The picture in the middle of the box at its own shape (black bars around it); the controls fill the whole box. */
    private fun fit() {
        val w = width; val h = height
        if (w <= 0 || h <= 0 || picW <= 0 || picH <= 0) return
        val r = InlineBox.letterbox(InlineBox.Rect(0, 0, w, h), picW, picH, pixelRatio) ?: return
        val lp = surface.layoutParams as FrameLayout.LayoutParams
        if (lp.width != r.w || lp.height != r.h) { lp.width = r.w.coerceAtLeast(1); lp.height = r.h.coerceAtLeast(1); lp.gravity = Gravity.CENTER; surface.layoutParams = lp }
    }

    // ---------------------------------------------------------------------------------------- state -> the controls

    private fun sample() {
        val p = player ?: return
        // ANS55 rule: the clock runs and no picture ever came = a stream with nothing to show: 7 s of playing without a first frame and the page's own player takes the film back
        if (firstPictureAt == 0L && p.isPlaying) {
            val now = SystemClock.uptimeMillis()
            if (playingSince == 0L) playingSince = now
            else if (now - playingSince > 7_000L) { playingSince = 0L; say("failed", "no picture: the clock ran for 7 s and no picture came (a stream with no video)"); stop("no picture", failed = true); return }
        } else if (!p.isPlaying) playingSince = 0L
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

    private fun flush() { for (c in model.drain()) apply(c) }

    private fun apply(c: PlayerModel.Cmd) {
        val p = player ?: return
        try {
            when (c.a) {
                "play" -> { if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L); pausedByEdge = false; p.play() }
                "pause" -> { pausedByEdge = false; p.pause() }
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
        sample()
    }

    // ---------------------------------------------------------------------------------------- PlayerModel.Host

    override fun now() = SystemClock.uptimeMillis()
    override fun scheduleControls(ms: Long) { main.removeCallbacks(controlsTick); main.postDelayed(controlsTick, ms) }
    override fun cancelControls() { main.removeCallbacks(controlsTick) }
    override fun applyControls(shown: Boolean, animate: Boolean) {
        controls.show(shown, animate)
        if (!priv) { val b = controls.buttonCentres(); say(if (shown) "shown" else "hidden", if (shown) "close at ${b[0]},${b[1]} full screen at ${b[2]},${b[3]} (screen pixels)" else "at once") }
    }
    override fun changed() { controls.update(model.view()) }
    override fun feedback(zone: PlayerRules.Zone, burst: Int) { controls.flash(zone, burst) }
    override fun log(event: String, text: String) { say(event, text) }

    // ---------------------------------------------------------------------------------------- PlayerControlsView.Listener

    override fun onPlayPause() { model.playPause(); flush() }
    override fun onSkip(forward: Boolean) { model.skipButton(forward); flush() }
    override fun onMute() { model.mute(); flush() }
    override fun onExit() { stop("the close button") }
    override fun onSeek(fraction: Double) { model.seekFraction(fraction); flush() }
    override fun onPin(on: Boolean) { model.pin(on) }
    override fun onFullscreen() { stop("the full-screen button", fullscreen = true) }

    // ---------------------------------------------------------------------------------------- touch

    /** A tap on the picture (anything the controls themselves did not take): the model decides show / hide / double-tap seek. A drag or a second finger goes to the page, which scrolls (or zooms) as if nothing lay over it. */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val g = gesturesOrNull()
        if (g != null) return onGestureTouch(g, e)
        val slop = 12f * density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; downT = e.eventTime; passing = false; longFired = false; downCopy?.recycle(); downCopy = MotionEvent.obtain(e); main.removeCallbacks(longPressRun); main.postDelayed(longPressRun, android.view.ViewConfiguration.getLongPressTimeout().toLong()) }
            MotionEvent.ACTION_MOVE -> if (!passing && !longFired && (Math.abs(e.x - downX) > slop || Math.abs(e.y - downY) > slop)) startPass()
            MotionEvent.ACTION_POINTER_DOWN -> if (!passing) startPass()
        }
        if (passing) {
            host?.toPage(e)
            if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) { passing = false }
        } else if (e.actionMasked == MotionEvent.ACTION_UP) {
            main.removeCallbacks(longPressRun)
            if (!longFired && e.eventTime - downT <= 450L && Math.abs(e.x - downX) <= slop && Math.abs(e.y - downY) <= slop && width > 0) { model.onTap((e.x * 1000f / width).toInt()); flush() }
            downCopy?.recycle(); downCopy = null
        } else if (e.actionMasked == MotionEvent.ACTION_CANCEL) { main.removeCallbacks(longPressRun); downCopy?.recycle(); downCopy = null }
        return true
    }

    /** ANS57: the touch through the gesture rules: a tap is the controls', the long press arms the precise seek, a drag that moves BEFORE the hold is the page's (it scrolls), a second finger is the page's (it zooms). */
    private fun onGestureTouch(g: VideoGestures, e: MotionEvent): Boolean {
        if (passing) {
            host?.toPage(e)
            if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) { passing = false; downCopy?.recycle(); downCopy = null }
            return true
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { longFired = false; downCopy?.recycle(); downCopy = MotionEvent.obtain(e); g.onTouch(e) }
            MotionEvent.ACTION_POINTER_DOWN -> { if (g.active) g.onTouch(e) else { g.onTouch(e); startPass(); host?.toPage(e) } }
            else -> {
                g.onTouch(e)
                if (passing) host?.toPage(e)                                              // (the move that made the touch the page's is the page's first)
                if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) { if (!passing) { downCopy?.recycle(); downCopy = null } else passing = false }
            }
        }
        return true
    }

    private fun startPass() {
        passing = true
        main.removeCallbacks(longPressRun)
        downCopy?.let { host?.toPage(it); it.recycle() }                   // the page never saw the finger go down: it is told now, then it gets every move
        downCopy = null
    }

    override fun onDetachedFromWindow() { if (player != null) stop("the view left the window"); super.onDetachedFromWindow() }
}
