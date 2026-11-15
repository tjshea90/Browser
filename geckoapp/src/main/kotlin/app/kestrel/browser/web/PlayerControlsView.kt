package app.kestrel.browser.web

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.player.DurationView
import app.kestrel.core.player.PlayerModel
import app.kestrel.core.player.PlayerRules

/**
 * VP1 (owner message 49: "controls easily accessible but each time I tap the screen it should toggle playback controls shown or hidden, just like other popular video players"): Kestrel's own controls for a
 * full-screen video, ONE custom view drawn with Canvas (no layout pass, no child views, nothing in the page). It lies over the engine's view; while the controls are hidden it takes no touch at all (the
 * page and `player.js` get every tap: that is how a tap shows them), while they show it takes only the touches that land on a control.
 *
 * What it draws, from the pure [PlayerModel.View]: a scrim along the bottom and one at the top, the middle cluster (10 s back, play / pause, 10 s forward, 48-76 dp targets), the track with the buffered part, the
 * thumb and both times (a length only when the model believes one; LIVE for a live stream), mute, and exit full screen. While buffering a ring turns over the picture, shown or not. A double tap on a side
 * flashes "10 s" there. It animates only while something changes (the fade of the controls, a flash, the ring), never otherwise: nothing runs while the controls are hidden and the film plays.
 */
class PlayerControlsView(ctx: Context) : View(ctx) {
    interface Listener {
        fun onPlayPause()
        fun onSkip(forward: Boolean)
        fun onMute()
        fun onExit()
        /** The finger let go on the track at [fraction] (0..1). */
        fun onSeek(fraction: Double)
        /** A finger is on a control (true) or let go / was cancelled (false). */
        fun onPin(on: Boolean)
    }

    var listener: Listener? = null
    private val d = resources.displayMetrics.density
    private fun dp(x: Float) = x * d

    private var model: PlayerModel.View? = null
    private var shownTarget = false
    private var alphaNow = 0f                     // the fade of the controls, 0..1
    private var fade: ValueAnimator? = null
    private var scrub = -1.0                      // while a finger drags the thumb: where (0..1), else -1
    private var pressed = Hit.NONE
    private var flashZone: PlayerRules.Zone? = null
    private var flashBurst = 0
    private var flashAmount = 0f
    private var flash: ValueAnimator? = null
    private var spin = 0f
    private var bottomInset = 0f                  // the navigation bar pulled up over the film (Hide mode): the controls sit above it

    private enum class Hit { NONE, PLAY, BACK, FORWARD, MUTE, EXIT, TRACK }

    private val bottomScrim = Paint()               // the two scrims keep their gradient (made once per size, not once per frame: a fade draws ten frames, a report every 250 ms one more)
    private val topScrim = Paint()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x73000000 }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = dp(2.2f) }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = dp(14f) * resources.configuration.fontScale.coerceIn(1f, 1.3f); fontFeatureSettings = "tnum" }
    private val path = Path()
    private val rect = RectF()

    // the places of the controls (px), computed from the size
    private val play = RectF(); private val back = RectF(); private val forward = RectF(); private val mute = RectF(); private val exit = RectF(); private val trackBox = RectF()
    private var trackL = 0f; private var trackR = 0f; private var rowY = 0f
    private var curX = 0f; private var durRight = 0f

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    // ---------------------------------------------------------------------------------------- what the controller tells

    /** The numbers to draw changed. */
    fun update(v: PlayerModel.View) {
        val wasBuffering = model?.buffering == true
        model = v
        val changed = lookChanged(v)                                          // a report every 250 ms and a ticker every 250 ms mostly say what is already on the screen
        if (wasBuffering && !v.buffering) { invalidate(); return }            // the ring goes (it may be the only thing drawn)
        if (changed && (alphaNow > 0f || v.buffering)) invalidate()
    }

    // what the last update said, in the units that are DRAWN (whole seconds of the texts, whole pixels of the track): an update that changes none of them needs no new frame
    private var dSec = -1L; private var dLen = -1L; private var dPlayPx = -1; private var dBufPx = -1; private var dBits = -1
    private fun lookChanged(v: PlayerModel.View): Boolean {
        val known = v.kind == DurationView.Kind.KNOWN
        val span = trackR - trackL
        val sec = v.timeS.toLong()
        val len = if (known) v.lengthS.toLong() else -1L
        val pp = if (known) (PlayerRules.fraction(v.timeS, v.lengthS) * span).toInt() else 0
        val bp = if (known) (PlayerRules.fraction(v.bufferedS, v.lengthS) * span).toInt() else 0
        val bits = (if (v.playing) 1 else 0) or (if (v.muted) 2 else 0) or (if (v.buffering) 4 else 0) or (v.kind.ordinal shl 3)
        val changed = sec != dSec || len != dLen || pp != dPlayPx || bp != dBufPx || bits != dBits
        dSec = sec; dLen = len; dPlayPx = pp; dBufPx = bp; dBits = bits
        return changed
    }

    /** Fade the controls in or out ([animate] false = at once). */
    fun show(shown: Boolean, animate: Boolean) {
        shownTarget = shown
        fade?.cancel()
        if (!animate) { alphaNow = if (shown) 1f else 0f; if (!shown) { pressed = Hit.NONE; scrub = -1.0 }; invalidate(); return }
        val to = if (shown) 1f else 0f
        fade = ValueAnimator.ofFloat(alphaNow, to).apply {
            duration = PlayerRules_FADE_MS
            addUpdateListener { alphaNow = it.animatedValue as Float; invalidate() }
            start()
        }
        if (!shown) { pressed = Hit.NONE; scrub = -1.0 }
    }

    /** "10 s" flashes on the side of a double tap ([burst] 1 = the double tap itself; later ones count up). */
    fun flash(zone: PlayerRules.Zone, burst: Int) {
        flashZone = zone; flashBurst = burst
        flash?.cancel()
        flash = ValueAnimator.ofFloat(1f, 0f).apply { duration = 520L; addUpdateListener { flashAmount = it.animatedValue as Float; invalidate() }; start() }
    }

    /** The navigation bar was pulled up over the film: the controls move up by [px]. */
    fun setBottomInset(px: Int) { if (bottomInset != px.toFloat()) { bottomInset = px.toFloat(); layoutParts(); invalidate() } }

    val controlsShown: Boolean get() = shownTarget

    // ---------------------------------------------------------------------------------------- geometry

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { layoutParts() }

    private fun buildScrims(w: Float, h: Float) {
        val bottomH = dp(150f) + bottomInset
        bottomScrim.shader = LinearGradient(0f, h - bottomH, 0f, h, 0x00000000, 0xB3000000.toInt(), Shader.TileMode.CLAMP)
        topScrim.shader = LinearGradient(0f, 0f, 0f, dp(90f), 0x8C000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
    }

    private fun layoutParts() {
        bottomScrim.shader = null; topScrim.shader = null                    // (the size or the lifted bottom changed: the next draw makes them again)
        val w = width.toFloat(); val h = height.toFloat() - bottomInset
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f; val cy = h / 2f
        val pr = dp(38f); val sr = dp(28f); val gap = dp(30f)
        play.set(cx - pr, cy - pr, cx + pr, cy + pr)
        back.set(cx - pr - gap - 2 * sr, cy - sr, cx - pr - gap, cy + sr)
        forward.set(cx + pr + gap, cy - sr, cx + pr + gap + 2 * sr, cy + sr)
        val er = dp(24f); val margin = dp(14f)
        exit.set(w - margin - 2 * er, margin, w - margin, margin + 2 * er)
        rowY = h - dp(38f)
        mute.set(w - margin - 2 * er, rowY - er, w - margin, rowY + er)
        curX = dp(20f)
        val curW = text.measureText("0:00:00") + dp(2f)
        durRight = mute.left - dp(6f)
        trackL = curX + curW + dp(12f)
        trackR = durRight - text.measureText("0:00:00") - dp(12f)
        if (trackR < trackL + dp(80f)) trackR = trackL + dp(80f)
        trackBox.set(trackL - dp(14f), rowY - dp(24f), trackR + dp(14f), rowY + dp(24f))
    }

    // ---------------------------------------------------------------------------------------- drawing

    override fun onDraw(c: Canvas) {
        val v = model
        if (v == null) return
        val w = width.toFloat(); val h = height.toFloat()
        if (alphaNow > 0.01f) drawControls(c, v, w, h)
        drawFlash(c, w, h)
        if (v.buffering) drawRing(c, w, h)
    }

    private fun drawControls(c: Canvas, v: PlayerModel.View, w: Float, h: Float) {
        val a = (alphaNow * 255).toInt().coerceIn(0, 255)
        // the scrims: dark at the edges, so white controls read over any picture
        val bottomH = dp(150f) + bottomInset
        if (bottomScrim.shader == null) buildScrims(w, h)
        bottomScrim.alpha = a
        c.drawRect(0f, h - bottomH, w, h, bottomScrim)
        topScrim.alpha = a
        c.drawRect(0f, 0f, w, dp(90f), topScrim)
        fill.alpha = (0x73 * alphaNow).toInt(); ink.alpha = a; line.alpha = a; text.alpha = a

        // the middle cluster
        drawDisc(c, play, pressed == Hit.PLAY); drawPlayIcon(c, play, v.playing)
        drawDisc(c, back, pressed == Hit.BACK); drawSkipIcon(c, back, false)
        drawDisc(c, forward, pressed == Hit.FORWARD); drawSkipIcon(c, forward, true)
        // top right: leave full screen
        drawDisc(c, exit, pressed == Hit.EXIT); drawExitIcon(c, exit)
        // the row at the bottom: time, track, length, mute
        val shownT = if (scrub >= 0.0 && v.kind == DurationView.Kind.KNOWN) PlayerRules.timeAt(scrub, v.lengthS) else v.timeS
        c.drawText(PlayerRules.formatTime(shownT), curX, rowY + dp(5f), text)
        when (v.kind) {
            DurationView.Kind.KNOWN -> {
                val dur = PlayerRules.formatTime(v.lengthS)
                c.drawText(dur, durRight - text.measureText(dur), rowY + dp(5f), text)
                drawTrack(c, v)
            }
            DurationView.Kind.LIVE -> {
                val s = "LIVE"; c.drawText(s, durRight - text.measureText(s), rowY + dp(5f), text)
            }
            DurationView.Kind.UNKNOWN -> { /* no length believed (yet): only the position */ }
        }
        drawDisc(c, mute, pressed == Hit.MUTE); drawMuteIcon(c, mute, v.muted)
    }

    private fun drawDisc(c: Canvas, r: RectF, down: Boolean) {
        val old = fill.alpha
        if (down) fill.alpha = (0xB0 * alphaNow).toInt()
        c.drawCircle(r.centerX(), r.centerY(), r.width() / 2f, fill)
        fill.alpha = old
    }

    private fun drawTrack(c: Canvas, v: PlayerModel.View) {
        val f = if (scrub >= 0.0) scrub else PlayerRules.fraction(v.timeS, v.lengthS)
        val bf = PlayerRules.fraction(v.bufferedS, v.lengthS)
        val dragging = scrub >= 0.0
        val tw = if (dragging) dp(5f) else dp(3.4f)
        val len = trackR - trackL
        trackPaint.strokeWidth = tw
        trackPaint.color = 0x59FFFFFF; trackPaint.alpha = (0x59 * alphaNow).toInt()
        c.drawLine(trackL, rowY, trackR, rowY, trackPaint)
        trackPaint.color = 0x99FFFFFF.toInt(); trackPaint.alpha = (0x99 * alphaNow).toInt()
        c.drawLine(trackL, rowY, trackL + len * bf.toFloat(), rowY, trackPaint)
        trackPaint.color = ACCENT; trackPaint.alpha = (255 * alphaNow).toInt()
        c.drawLine(trackL, rowY, trackL + len * f.toFloat(), rowY, trackPaint)
        ink.color = 0xFFFFFFFF.toInt(); ink.alpha = (255 * alphaNow).toInt()
        c.drawCircle(trackL + len * f.toFloat(), rowY, if (dragging) dp(10f) else dp(6.5f), ink)
    }

    private fun drawPlayIcon(c: Canvas, r: RectF, playing: Boolean) {
        val cx = r.centerX(); val cy = r.centerY(); val u = r.width() / 2f
        path.reset()
        if (playing) {                                   // pause: two bars
            rect.set(cx - 0.36f * u, cy - 0.42f * u, cx - 0.10f * u, cy + 0.42f * u); c.drawRoundRect(rect, dp(2f), dp(2f), ink)
            rect.set(cx + 0.10f * u, cy - 0.42f * u, cx + 0.36f * u, cy + 0.42f * u); c.drawRoundRect(rect, dp(2f), dp(2f), ink)
        } else {                                         // play: a triangle (its middle sits a little right of the centre)
            path.moveTo(cx - 0.28f * u, cy - 0.46f * u); path.lineTo(cx - 0.28f * u, cy + 0.46f * u); path.lineTo(cx + 0.52f * u, cy); path.close()
            c.drawPath(path, ink)
        }
    }

    private fun drawSkipIcon(c: Canvas, r: RectF, forward: Boolean) {
        val cx = r.centerX(); val cy = r.centerY(); val rad = r.width() * 0.30f
        c.save()
        if (!forward) c.scale(-1f, 1f, cx, cy)
        rect.set(cx - rad, cy - rad, cx + rad, cy + rad)
        c.drawArc(rect, -70f, 290f, false, line)            // almost a full turn, clockwise
        // the arrow head at the open end (top, left of the start)
        val ex = cx + rad * Math.cos(Math.toRadians(-70.0)).toFloat(); val ey = cy + rad * Math.sin(Math.toRadians(-70.0)).toFloat()
        path.reset(); path.moveTo(ex - dp(1f), ey - dp(5.5f)); path.lineTo(ex + dp(6f), ey + dp(1.5f)); path.lineTo(ex - dp(4f), ey + dp(3f)); path.close()
        c.drawPath(path, ink)
        c.restore()
        val oldSize = text.textSize; text.textSize = dp(11f)
        val s = "10"; c.drawText(s, cx - text.measureText(s) / 2f, cy + dp(4f), text)
        text.textSize = oldSize
    }

    private fun drawMuteIcon(c: Canvas, r: RectF, muted: Boolean) {
        val cx = r.centerX() - dp(3f); val cy = r.centerY(); val u = dp(1f)
        path.reset()
        path.moveTo(cx - 7 * u, cy - 3 * u); path.lineTo(cx - 3 * u, cy - 3 * u); path.lineTo(cx + 2 * u, cy - 7 * u); path.lineTo(cx + 2 * u, cy + 7 * u); path.lineTo(cx - 3 * u, cy + 3 * u); path.lineTo(cx - 7 * u, cy + 3 * u); path.close()
        c.drawPath(path, ink)
        if (muted) { c.drawLine(cx + 5 * u, cy - 4 * u, cx + 12 * u, cy + 4 * u, line); c.drawLine(cx + 12 * u, cy - 4 * u, cx + 5 * u, cy + 4 * u, line) }
        else { rect.set(cx - 2 * u, cy - 5 * u, cx + 6 * u, cy + 5 * u); c.drawArc(rect, -50f, 100f, false, line); rect.set(cx - 5 * u, cy - 9 * u, cx + 11 * u, cy + 9 * u); c.drawArc(rect, -50f, 100f, false, line) }
    }

    private fun drawExitIcon(c: Canvas, r: RectF) {     // four corners pointing in: leave full screen
        val cx = r.centerX(); val cy = r.centerY(); val u = dp(1f); val a = 4 * u; val b = 8 * u
        for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
            c.drawLine(cx + sx * a, cy + sy * b, cx + sx * a, cy + sy * a, line)
            c.drawLine(cx + sx * b, cy + sy * a, cx + sx * a, cy + sy * a, line)
        }
    }

    private fun drawFlash(c: Canvas, w: Float, h: Float) {
        val z = flashZone ?: return
        if (flashAmount <= 0f) return
        val cx = if (z == PlayerRules.Zone.LEFT) w * 0.18f else w * 0.82f
        val cy = (h - bottomInset) / 2f
        val oldA = fill.alpha
        fill.alpha = (0x80 * flashAmount).toInt()
        c.drawCircle(cx, cy, dp(54f), fill)
        fill.alpha = oldA
        text.alpha = (255 * flashAmount).toInt()
        val s = (if (z == PlayerRules.Zone.LEFT) "-" else "+") + (PlayerRules.SKIP_S.toInt() * flashBurst) + " s"
        c.drawText(s, cx - text.measureText(s) / 2f, cy + dp(5f), text)
    }

    private fun drawRing(c: Canvas, w: Float, h: Float) {
        spin = (SystemClock.uptimeMillis() % RING_TURN_MS) * 360f / RING_TURN_MS      // by the clock: the speed is the same at any frame rate
        val cx = w / 2f; val cy = (h - bottomInset) / 2f; val r = dp(22f)
        rect.set(cx - r, cy - r, cx + r, cy + r)
        line.alpha = 220
        c.drawArc(rect, spin, 250f, false, line)
        // the next frame in 50 ms and only of the ring's own box (a spinner needs no 60 frames a second, and the film behind it is decoded by a tablet that has none to spare)
        val pad = (line.strokeWidth + dp(2f)).toInt()
        postInvalidateDelayed(RING_FRAME_MS, (cx - r).toInt() - pad, (cy - r).toInt() - pad, (cx + r).toInt() + pad, (cy + r).toInt() + pad)
    }

    // ---------------------------------------------------------------------------------------- touch

    private fun hitTest(x: Float, y: Float): Hit {
        fun inside(r: RectF, pad: Float) = x >= r.left - pad && x <= r.right + pad && y >= r.top - pad && y <= r.bottom + pad
        val v = model
        return when {
            inside(play, dp(6f)) -> Hit.PLAY
            inside(back, dp(8f)) -> Hit.BACK
            inside(forward, dp(8f)) -> Hit.FORWARD
            inside(exit, dp(8f)) -> Hit.EXIT
            inside(mute, dp(6f)) -> Hit.MUTE
            v != null && v.kind == DurationView.Kind.KNOWN && inside(trackBox, 0f) -> Hit.TRACK
            else -> Hit.NONE
        }
    }

    private fun fractionAt(x: Float): Double = ((x - trackL) / (trackR - trackL)).toDouble().coerceIn(0.0, 1.0)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (alphaNow < 0.5f || !shownTarget) return false          // hidden (or fading out): the page gets every touch; a tap there is how they come back
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = hitTest(e.x, e.y)
                if (hit == Hit.NONE) return false                    // not on a control: the page (player.js) takes the tap
                pressed = hit
                listener?.onPin(true)
                if (hit == Hit.TRACK) scrub = fractionAt(e.x)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressed == Hit.TRACK) { scrub = fractionAt(e.x); invalidate() }
                return pressed != Hit.NONE
            }
            MotionEvent.ACTION_UP -> {
                val hit = pressed; pressed = Hit.NONE
                val up = hitTest(e.x, e.y)
                val f = scrub; scrub = -1.0
                listener?.onPin(false)
                invalidate()
                when (hit) {
                    Hit.TRACK -> if (f >= 0.0) listener?.onSeek(f)
                    Hit.PLAY -> if (up == Hit.PLAY) listener?.onPlayPause()
                    Hit.BACK -> if (up == Hit.BACK) listener?.onSkip(false)
                    Hit.FORWARD -> if (up == Hit.FORWARD) listener?.onSkip(true)
                    Hit.MUTE -> if (up == Hit.MUTE) listener?.onMute()
                    Hit.EXIT -> if (up == Hit.EXIT) listener?.onExit()
                    Hit.NONE -> {}
                }
                return hit != Hit.NONE
            }
            MotionEvent.ACTION_CANCEL -> {
                val was = pressed != Hit.NONE
                pressed = Hit.NONE; scrub = -1.0
                if (was) listener?.onPin(false)
                invalidate()
                return was
            }
        }
        return pressed != Hit.NONE
    }

    override fun onDetachedFromWindow() { fade?.cancel(); flash?.cancel(); super.onDetachedFromWindow() }

    companion object {
        private const val ACCENT = 0xFF36E6F2.toInt()
        private const val PlayerRules_FADE_MS = 160L
        private const val RING_TURN_MS = 900L           // one turn of the buffering ring
        private const val RING_FRAME_MS = 50L           // and a new frame of it this often
    }
}
