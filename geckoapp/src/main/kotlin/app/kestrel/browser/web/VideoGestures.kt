package app.kestrel.browser.web

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import app.kestrel.core.player.GestureHint
import app.kestrel.core.player.GestureMath
import app.kestrel.core.player.PlayerGestures

/**
 * ANS57 (owner message 57; research/ANS57_VLC_GESTURES.md): the Android side of the film's touch rules ([PlayerGestures], pure, in core): it turns the finger's events into the engine's calls, rings the long-press
 * timer, changes the real volume and brightness ([VideoLevels]) and tells the view what to show. One per player; the player says where the film is and carries out the seek.
 * ANS60a (owner message 60): no long press is needed any more. [middlePasses] = the player inside the page: a vertical drag that begins in the middle strip is the PAGE's (it scrolls). [holdFirst] = the older rule
 * (Settings > Gestures inside the page need a long press): a plain drag is the page's and every gesture needs the long press first.
 */
class VideoGestures(private val activity: Activity, private val s: Surface, private val holdFirst: Boolean, private val middlePasses: Boolean = false, private val edgeDp: Float = 24f) : PlayerGestures.Host {
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
        /** A line for Diagnostics / the emulator lab (the host decides whether it may be logged: never for a private tab). */
        fun note(event: String, text: String) {}
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
        return PlayerGestures.Config(s.viewWidth.toFloat(), s.viewHeight.toFloat(), xdpi, ViewConfiguration.get(activity).scaledTouchSlop.toFloat(), edgeDp * dm.density, range, holdFirst, middlePasses)
    }

    /** True while a gesture of ours has the touch. */
    val active: Boolean get() = engine.active

    /** Feeds one event of ONE finger; returns true when this event belongs to a gesture of ours (the host must not give it to anyone else). */
    fun onTouch(e: MotionEvent): Boolean {
        val was = engine.active
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { main.removeCallbacks(clearInfo); if (engine.active) engine.cancel(); engine.cfg = config(); engine.down(e.x, e.y, e.eventTime) }       // (a touch whose end was lost: its info goes)
            MotionEvent.ACTION_MOVE -> engine.move(e.x, e.y, e.eventTime)
            MotionEvent.ACTION_UP -> engine.up(e.x, e.y, e.eventTime)
            MotionEvent.ACTION_CANCEL -> engine.cancel()
            MotionEvent.ACTION_POINTER_DOWN -> { main.removeCallbacks(holdRun); engine.cancel() }      // a second finger ends our gesture, also a rest that was still undecided (a pinch is not ours)
        }
        return was || engine.active
    }

    /** The touch was given to the page: our timer and our judgement of it end (no hint may appear over a page scroll). */
    fun abandon() { main.removeCallbacks(holdRun); engine.cancel() }

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
    private var hintShown = false
    override fun hint(hint: GestureHint?, armed: Boolean) {
        if (hint == null) { if (hintShown) { hintShown = false; s.showInfo(null) }; return }
        hintShown = true
        if (armed) { s.haptic(); beginOnce() }                                          // the long-press-first rule: the rest armed the gestures
        s.showInfo(GestureMath.hintText(hint))
        s.note(if (armed) "gesture-armed" else "gesture-hint", "${hint.name.lowercase()}: ${GestureMath.hintText(hint)} (xdpi ${"%.0f".format(engine.cfg.xdpi)}, view ${s.viewWidth}x${s.viewHeight}, range ${engine.cfg.rangePx.toInt()} px)")
    }
    private fun beginOnce() { if (!began) { began = true; s.gestureChanged(true) } }
    override fun longHold() { s.longHold() }
    override fun positionMs() = s.positionMs()
    override fun lengthMs() = s.lengthMs()
    override fun seekPreview(jumpMs: Long, targetMs: Long, coef: Double) { beginOnce(); s.showInfo(GestureMath.infoText(jumpMs, targetMs, coef)) }
    override fun seekCommit(jumpMs: Long) {
        committed = true
        val target = (s.positionMs() + jumpMs).coerceAtLeast(0L)                          // (BEFORE the seek: afterwards the player already reports the target as its position, review of alpha61 found the pill saying twice the jump)
        s.seekBy(jumpMs)
        s.showInfo(GestureMath.infoText(jumpMs, target, 1.0).substringBefore(" x"))
    }
    override fun seekCancel() { s.showInfo(null) }
    override fun seekUnavailable() { beginOnce(); s.showInfo("This film cannot be moved") }
    override fun passThrough() { s.passThrough() }

    override fun level(level: GestureMath.Level, delta: Double) {
        beginOnce()
        when (level) {
            GestureMath.Level.VOLUME -> { if (vol < 0f) vol = levels.volume(); vol = (vol + delta.toFloat()).coerceIn(0f, 1f); levels.setVolume(vol); s.showLevel(level, vol); noteLevel(level, vol) }
            GestureMath.Level.BRIGHTNESS -> { if (br < 0f) br = levels.brightness(); br = (br + delta.toFloat()).coerceIn(0.01f, 1f); levels.setBrightness(br); s.showLevel(level, br); noteLevel(level, br) }
        }
    }

    private var noteAt = 0L
    private var notedPct = -100
    private var lastKind: GestureMath.Level? = null
    private var lastFrac = 0f
    /** At most one line per 5 points or 250 ms (a drag makes sixty moves a second); the last value is written when the finger lets go. */
    private fun noteLevel(kind: GestureMath.Level, f: Float) {
        lastKind = kind; lastFrac = f
        val pct = Math.round(f * 100); val now = SystemClock.uptimeMillis()
        if (Math.abs(pct - notedPct) >= 5 || now - noteAt >= 250L) { noteAt = now; notedPct = pct; s.note("gesture-level", "${kind.name.lowercase()} $pct %") }
    }

    override fun finished() {
        lastKind?.let { k -> val pct = Math.round(lastFrac * 100); if (pct != notedPct) s.note("gesture-level", "${k.name.lowercase()} $pct %") }
        lastKind = null; notedPct = -100
        vol = -1f; br = -1f; began = false; hintShown = false
        if (committed) { committed = false; main.removeCallbacks(clearInfo); main.postDelayed(clearInfo, RESULT_MS) }       // the jump stays on the picture for a moment after the finger lets go (VLC)
        else s.showInfo(null)
        s.gestureChanged(false)
    }

    private companion object {
        const val RESULT_MS = 900L
    }
}
