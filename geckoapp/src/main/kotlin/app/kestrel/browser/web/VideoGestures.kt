package app.kestrel.browser.web

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import app.kestrel.core.player.GestureMath
import app.kestrel.core.player.PlayerGestures

/**
 * ANS57 (owner message 57; research/ANS57_VLC_GESTURES.md): the Android side of the film's touch rules ([PlayerGestures], pure, in core): it turns the finger's events into the engine's calls, rings the long-press
 * timer, changes the real volume and brightness ([VideoLevels]) and tells the view what to show. One per player; the player says where the film is and carries out the seek. [holdFirst] = the player inside the page,
 * where a plain drag scrolls the PAGE: every gesture needs the long press first.
 */
class VideoGestures(private val activity: Activity, private val s: Surface, private val holdFirst: Boolean, private val edgeDp: Float = 24f) : PlayerGestures.Host {
    interface Surface {
        val viewWidth: Int
        val viewHeight: Int
        fun positionMs(): Long
        fun lengthMs(): Long
        /** A short touch that went nowhere. */
        fun tap(xFraction: Double) {}
        /** The precise seek ended: move the film by [deltaMs]. */
        fun seekBy(deltaMs: Long)
        /** The text of the seek (null = none) / the level bar of brightness or volume, 0..1. */
        fun showInfo(text: String?)
        fun showLevel(kind: GestureMath.Level, fraction: Float)
        /** A short tick of the phone (the long press armed the seek). */
        fun haptic()
        /** The drag is the page's (hold-first mode). */
        fun passThrough() {}
        /** The finger rested on up to the second stage. */
        fun longHold() {}
        /** A gesture of ours began (true) or ended (false): the page full screen takes its own touch back at the beginning. */
        fun gestureChanged(active: Boolean) {}
    }

    private val main = Handler(Looper.getMainLooper())
    private val dm = activity.resources.displayMetrics
    val levels = VideoLevels(activity)
    private val engine = PlayerGestures(this, config())
    private val holdRun = Runnable { engine.hold() }
    private val clearInfo = Runnable { s.showInfo(null) }
    private var vol = -1f
    private var br = -1f
    private var committed = false
    private var began = false                              // gestureChanged(true) was said for this touch

    private fun config(): PlayerGestures.Config {
        val xdpi = if (dm.xdpi in 40f..800f) dm.xdpi else dm.densityDpi.toFloat()
        val range = Math.min(dm.widthPixels, dm.heightPixels).toFloat()
        return PlayerGestures.Config(s.viewWidth.toFloat(), s.viewHeight.toFloat(), xdpi, ViewConfiguration.get(activity).scaledTouchSlop.toFloat(), edgeDp * dm.density, range, holdFirst)
    }

    /** True while a gesture of ours has the touch. */
    val active: Boolean get() = engine.active

    /** Feeds one event of ONE finger; returns true when this event belongs to a gesture of ours (the host must not give it to anyone else). */
    fun onTouch(e: MotionEvent): Boolean {
        val was = engine.active
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { main.removeCallbacks(clearInfo); engine.cfg = config(); engine.down(e.x, e.y) }
            MotionEvent.ACTION_MOVE -> engine.move(e.x, e.y)
            MotionEvent.ACTION_UP -> engine.up(e.x, e.y)
            MotionEvent.ACTION_CANCEL -> engine.cancel()
            MotionEvent.ACTION_POINTER_DOWN -> if (engine.active) engine.cancel()           // a second finger ends our gesture (a pinch is not ours)
        }
        return was || engine.active
    }

    /** The view is going away or the film ended: nothing is left running. */
    fun stop() {
        main.removeCallbacks(holdRun); main.removeCallbacks(clearInfo)
        engine.cancel()
        s.showInfo(null)
        levels.restore()
    }

    // ---------------------------------------------------------------------------------------- PlayerGestures.Host

    override fun now() = SystemClock.uptimeMillis()
    override fun scheduleHold(ms: Long) { main.removeCallbacks(holdRun); main.postDelayed(holdRun, ms) }
    override fun cancelHold() { main.removeCallbacks(holdRun) }
    override fun tap(xFraction: Double) { s.tap(xFraction) }
    override fun armed() { s.haptic(); s.showInfo(HINT); if (!began) { began = true; s.gestureChanged(true) } }
    override fun longHold() { s.longHold() }
    override fun positionMs() = s.positionMs()
    override fun lengthMs() = s.lengthMs()
    override fun seekPreview(jumpMs: Long, targetMs: Long, coef: Double) { s.showInfo(GestureMath.infoText(jumpMs, targetMs, coef)) }
    override fun seekCommit(jumpMs: Long) {
        committed = true
        s.seekBy(jumpMs)
        s.showInfo(GestureMath.infoText(jumpMs, (s.positionMs() + jumpMs).coerceAtLeast(0L), 1.0).substringBefore(" x"))
    }
    override fun seekCancel() { s.showInfo(null) }
    override fun seekUnavailable() { s.showInfo("This film cannot be moved") }
    override fun passThrough() { s.passThrough() }

    override fun level(level: GestureMath.Level, delta: Double) {
        if (!began) { began = true; s.gestureChanged(true) }
        when (level) {
            GestureMath.Level.VOLUME -> { if (vol < 0f) vol = levels.volume(); vol = (vol + delta.toFloat()).coerceIn(0f, 1f); levels.setVolume(vol); s.showLevel(level, vol) }
            GestureMath.Level.BRIGHTNESS -> { if (br < 0f) br = levels.brightness(); br = (br + delta.toFloat()).coerceIn(0.01f, 1f); levels.setBrightness(br); s.showLevel(level, br) }
        }
    }

    override fun finished() {
        vol = -1f; br = -1f; began = false
        if (committed) { committed = false; main.removeCallbacks(clearInfo); main.postDelayed(clearInfo, RESULT_MS) }       // the jump stays on the picture for a moment after the finger lets go (VLC)
        else s.showInfo(null)
        s.gestureChanged(false)
    }

    private companion object {
        const val HINT = "Slide left or right to seek"
        const val RESULT_MS = 900L
    }
}
