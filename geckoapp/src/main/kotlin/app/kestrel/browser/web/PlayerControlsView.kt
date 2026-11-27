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
import app.kestrel.core.player.GestureMath
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
        /** ANS53f: the rotate button (drawn only while [showRotate]). */
        fun onRotate() {}
        /** ANS53d: the quality button (drawn only while [showQuality]). */
        fun onQuality() {}
        /** ANS55f: the full-screen button of the player inside the page (drawn only while [inline]). */
        fun onFullscreen() {}
    }

    /**
     * ANS55f (owner message 55: the player inside the website): the controls of Kestrel's player drawn over the page's own video box. The exit button becomes a close x, a full-screen button sits left of it, and everything
     * is scaled to the size of the box (a 530 x 300 box gets the full-size controls, a 260 x 150 one the smaller ones). ANS60c: the quality button sits left of the full-screen button. No rotate button here (the full-screen player has it).
     */
    var inline = false
        set(v) { if (field != v) { field = v; layoutParts(); invalidate() } }

    /** ANS53f: Kestrel's own player shows a rotate button at the top left (the page's full screen has its own, a separate view). */
    var showRotate = false
        set(v) { if (field != v) { field = v; invalidate() } }
    /** ANS53d: a quality button left of the exit button, where the film has a quality list to pick from. */
    var showQuality = false
        set(v) { if (field != v) { field = v; invalidate() } }

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

    private enum class Hit { NONE, PLAY, BACK, FORWARD, MUTE, EXIT, TRACK, ROTATE, QUALITY, FULL }

    private val bottomScrim = Paint()               // the two scrims keep their gradient (made once per size, not once per frame: a fade draws ten frames, a report every 250 ms one more)
    private val topScrim = Paint()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x73000000 }
    private val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB3000000.toInt() }     // ANS57: the background of the gesture texts and level bars
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = dp(2.2f) }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = dp(14f) * resources.configuration.fontScale.coerceIn(1f, 1.3f); fontFeatureSettings = "tnum" }
    private val path = Path()
    private val rect = RectF()

    // the places of the controls (px), computed from the size
    private val play = RectF(); private val back = RectF(); private val forward = RectF(); private val mute = RectF(); private val exit = RectF(); private val trackBox = RectF(); private val rotate = RectF(); private val quality = RectF()
    private val full = RectF()                  // ANS55f: the full-screen button of the inline player (left of the close x)
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
        val bits = (if (v.playing) 1 else 0) or (if (v.muted) 2 else 0) or (if (v.buffering) 4 else 0) or (v.kind.ordinal shl 3) or (if (v.failed) 32 else 0)
        val changed = sec != dSec || len != dLen || pp != dPlayPx || bp != dBufPx || bits != dBits
        dSec = sec; dLen = len; dPlayPx = pp; dBufPx = bp; dBits = bits
        return changed
    }

    /** Fade the controls in or out ([animate] false = at once). */
    fun show(shown: Boolean, animate: Boolean) {
        shownTarget = shown
        fade?.cancel()
        if (!shown && pressed != Hit.NONE) { pressed = Hit.NONE; scrub = -1.0; listener?.onPin(false) }   // a finger was on a control when the controls went (a turn of the screen): its lift would be dropped, so let go of the pin here
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

    /** ANS55f lab: the centres of the close x, of the full-screen button and (ANS60c) of the quality button in SCREEN pixels (x, y, x, y, x, y): the emulator lab taps them like a finger would. */
    fun buttonCentres(): IntArray {
        val loc = IntArray(2); getLocationOnScreen(loc)
        return intArrayOf((loc[0] + exit.centerX()).toInt(), (loc[1] + exit.centerY()).toInt(), (loc[0] + full.centerX()).toInt(), (loc[1] + full.centerY()).toInt(), (loc[0] + quality.centerX()).toInt(), (loc[1] + quality.centerY()).toInt())
    }

    /** ANS53c: is [wx], [wy] (window coordinates) on one of the SHOWN controls? Then the touch is theirs and is not a tap on the picture. */
    fun controlAtWindow(wx: Float, wy: Float): Boolean {
        if (alphaNow < 0.5f || !shownTarget) return false
        val loc = IntArray(2); getLocationInWindow(loc)
        return hitTest(wx - loc[0], wy - loc[1]) != Hit.NONE
    }

    // ---------------------------------------------------------------------------------------- geometry

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { layoutParts() }

    private var sc = 1f                                       // ANS55f: the size of the controls against the full-size ones (1 = full screen; smaller in a small box)
    private fun bottomScrimH() = (if (inline) Math.min(dp(150f) * sc, height * 0.42f) else dp(150f)) + bottomInset
    private fun topScrimH() = if (inline) Math.min(dp(90f) * sc, height * 0.28f) else dp(90f)

    private fun buildScrims(w: Float, h: Float) {
        val bottomH = bottomScrimH()
        bottomScrim.shader = LinearGradient(0f, h - bottomH, 0f, h, 0x00000000, 0xB3000000.toInt(), Shader.TileMode.CLAMP)
        topScrim.shader = LinearGradient(0f, 0f, 0f, topScrimH(), 0x8C000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
    }

    private fun layoutParts() {
        bottomScrim.shader = null; topScrim.shader = null                    // (the size or the lifted bottom changed: the next draw makes them again)
        val w = width.toFloat(); val h = height.toFloat() - bottomInset
        if (w <= 0f || h <= 0f) return
        sc = if (inline) (Math.min(w, h * 1.7f) / dp(520f)).coerceIn(0.62f, 1f) else 1f
        val cx = w / 2f; val cy = h / 2f
        val pr = dp(38f) * sc; val sr = dp(28f) * sc; val gap = dp(30f) * sc
        play.set(cx - pr, cy - pr, cx + pr, cy + pr)
        back.set(cx - pr - gap - 2 * sr, cy - sr, cx - pr - gap, cy + sr)
        forward.set(cx + pr + gap, cy - sr, cx + pr + gap + 2 * sr, cy + sr)
        val er = dp(24f) * sc; val margin = dp(14f) * sc
        exit.set(w - margin - 2 * er, margin, w - margin, margin + 2 * er)
        quality.set(exit.left - dp(10f) * sc - 2 * er, margin, exit.left - dp(10f) * sc, margin + 2 * er)
        full.set(quality)
        if (inline) quality.offset(-(2 * er + dp(10f) * sc), 0f)            // ANS60c: inside the page the full-screen button takes the first place left of the x and the quality button the next
        rotate.set(margin, margin, margin + 2 * er, margin + 2 * er)
        rowY = h - dp(38f) * sc
        mute.set(w - margin - 2 * er, rowY - er, w - margin, rowY + er)
        curX = dp(20f) * sc
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
        val w = width.toFloat(); val h = height.toFloat()
        if (v != null) {
            if (alphaNow > 0.01f) drawControls(c, v, w, h)
            drawFlash(c, w, h)
            if (v.buffering) drawRing(c, w, h)
        }
        drawGestureInfo(c, w, h)
    }

    // ---------------------------------------------------------------------------------------- ANS57: what the gestures show (VLC style)

    private var infoText: String? = null
    private var levelKind: GestureMath.Level? = null
    private var levelValue = 0f
    private var levelUntil = 0L
    private val levelClear = Runnable { if (levelKind != null && SystemClock.uptimeMillis() >= levelUntil) { levelKind = null; invalidate() } }

    /** The text of a precise seek in progress ("+0:25 (1:23:45)", VLC's own) or its hint; null = none. */
    fun showInfo(text: String?) { if (infoText != text) { infoText = text; invalidate() } }

    /** The level bar of brightness (left) or volume (right), [fraction] 0..1; it goes a moment after the last change. */
    fun showLevel(kind: GestureMath.Level, fraction: Float) {
        levelKind = kind; levelValue = fraction.coerceIn(0f, 1f); levelUntil = SystemClock.uptimeMillis() + LEVEL_SHOW_MS
        invalidate(); removeCallbacks(levelClear); postDelayed(levelClear, LEVEL_SHOW_MS + 40L)
    }

    private fun drawGestureInfo(c: Canvas, w: Float, h: Float) {
        val sz = Math.max(sc, 0.8f)
        infoText?.let { t ->
            val oldSize = text.textSize; val oldA = text.alpha
            text.textSize = dp(17f) * sz * resources.configuration.fontScale.coerceIn(1f, 1.3f); text.alpha = 255
            val tw = text.measureText(t); val padX = dp(16f) * sz; val padY = dp(10f) * sz
            val cx = w / 2f; val cy = h * 0.28f
            rect.set(cx - tw / 2f - padX, cy - text.textSize / 2f - padY, cx + tw / 2f + padX, cy + text.textSize / 2f + padY)
            c.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, pill)
            c.drawText(t, cx - tw / 2f, cy + text.textSize * 0.35f, text)
            text.textSize = oldSize; text.alpha = oldA
        }
        val kind = levelKind ?: return
        val panelW = dp(54f) * sz; val panelH = Math.min(dp(210f) * sz, h * 0.7f)
        val left = if (kind == GestureMath.Level.BRIGHTNESS) dp(22f) * sz else w - dp(22f) * sz - panelW
        val top = (h - panelH) / 2f
        rect.set(left, top, left + panelW, top + panelH)
        c.drawRoundRect(rect, dp(16f), dp(16f), pill)
        val cx = left + panelW / 2f
        val icon = RectF(cx - dp(14f) * sz, top + dp(10f) * sz, cx + dp(14f) * sz, top + dp(10f) * sz + dp(28f) * sz)
        ink.alpha = 255; line.alpha = 255; ink.color = 0xFFFFFFFF.toInt()
        if (kind == GestureMath.Level.BRIGHTNESS) drawSunIcon(c, icon) else drawMuteIcon(c, icon, levelValue <= 0.001f)
        val oldSize = text.textSize; val oldA = text.alpha
        text.textSize = dp(13f) * sz; text.alpha = 255
        val pct = "${Math.round(levelValue * 100)}%"
        c.drawText(pct, cx - text.measureText(pct) / 2f, top + panelH - dp(10f) * sz, text)
        text.textSize = oldSize; text.alpha = oldA
        val trackTop = icon.bottom + dp(10f) * sz; val trackBottom = top + panelH - dp(14f) * sz - dp(13f) * sz
        val tw = dp(5f) * sz
        trackPaint.strokeWidth = tw; trackPaint.color = 0x59FFFFFF; trackPaint.alpha = 0x59
        c.drawLine(cx, trackTop, cx, trackBottom, trackPaint)
        trackPaint.color = ACCENT; trackPaint.alpha = 255
        val fillTop = trackBottom - (trackBottom - trackTop) * levelValue
        c.drawLine(cx, trackBottom, cx, fillTop, trackPaint)
        ink.color = 0xFFFFFFFF.toInt(); ink.alpha = 255
        c.drawCircle(cx, fillTop, dp(6f) * sz, ink)
    }

    private fun drawSunIcon(c: Canvas, r: RectF) {       // a disc with eight rays: brightness
        val cx = r.centerX(); val cy = r.centerY(); val u = r.width() / 28f
        ink.alpha = 255; line.alpha = 255
        c.drawCircle(cx, cy, 5.5f * u, ink)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0); val cs = Math.cos(a).toFloat(); val sn = Math.sin(a).toFloat()
            c.drawLine(cx + cs * 8.5f * u, cy + sn * 8.5f * u, cx + cs * 12f * u, cy + sn * 12f * u, line)
        }
    }

    private fun drawControls(c: Canvas, v: PlayerModel.View, w: Float, h: Float) {
        val a = (alphaNow * 255).toInt().coerceIn(0, 255)
        // the scrims: dark at the edges, so white controls read over any picture
        val bottomH = bottomScrimH()
        if (bottomScrim.shader == null) buildScrims(w, h)
        bottomScrim.alpha = a
        c.drawRect(0f, h - bottomH, w, h, bottomScrim)
        topScrim.alpha = a
        c.drawRect(0f, 0f, w, topScrimH(), topScrim)
        fill.alpha = (0x73 * alphaNow).toInt(); ink.alpha = a; line.alpha = a; text.alpha = a

        // the middle cluster
        drawDisc(c, play, pressed == Hit.PLAY); drawPlayIcon(c, play, v.playing)
        if (v.failed) {                                                  // the film stopped with an error (a flaky link): say so under the buttons instead of a ring that spins for ever
            val note = "This video stopped"
            text.alpha = a
            c.drawText(note, w / 2f - text.measureText(note) / 2f, play.bottom + dp(40f), text)
        }
        drawDisc(c, back, pressed == Hit.BACK); drawSkipIcon(c, back, false)
        drawDisc(c, forward, pressed == Hit.FORWARD); drawSkipIcon(c, forward, true)
        // top right: leave full screen (inline: close the player, and a full-screen button left of it)
        drawDisc(c, exit, pressed == Hit.EXIT); if (inline) drawCloseIcon(c, exit) else drawExitIcon(c, exit)
        if (inline) { drawDisc(c, full, pressed == Hit.FULL); drawFullIcon(c, full) }
        if (showQuality) { drawDisc(c, quality, pressed == Hit.QUALITY); drawQualityIcon(c, quality) }
        if (showRotate && !inline) { drawDisc(c, rotate, pressed == Hit.ROTATE); drawRotateIcon(c, rotate) }
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

    private fun drawCloseIcon(c: Canvas, r: RectF) {    // ANS55f: an x: close the player inside the page
        val cx = r.centerX(); val cy = r.centerY(); val u = dp(1f) * Math.max(sc, 0.8f); val a = 6.5f * u
        c.drawLine(cx - a, cy - a, cx + a, cy + a, line); c.drawLine(cx - a, cy + a, cx + a, cy - a, line)
    }

    private fun drawFullIcon(c: Canvas, r: RectF) {     // ANS55f: four corners pointing out: go full screen
        val cx = r.centerX(); val cy = r.centerY(); val u = dp(1f) * Math.max(sc, 0.8f); val a = 3f * u; val b = 8f * u
        for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
            c.drawLine(cx + sx * b, cy + sy * b, cx + sx * b, cy + sy * a, line)
            c.drawLine(cx + sx * b, cy + sy * b, cx + sx * a, cy + sy * b, line)
        }
    }

    private fun drawQualityIcon(c: Canvas, r: RectF) {   // three sliders with a knob each: "settings"
        val cx = r.centerX(); val cy = r.centerY()
        for (i in -1..1) {
            val y = cy + i * dp(6.5f)
            c.drawLine(cx - dp(9f), y, cx + dp(9f), y, line)
            c.drawCircle(cx + i * dp(4.5f), y, dp(2.8f), ink)
        }
    }

    private fun drawRotateIcon(c: Canvas, r: RectF) {    // a phone with an arrow that goes round it
        val cx = r.centerX(); val cy = r.centerY()
        rect.set(cx - dp(4.5f), cy - dp(7.5f), cx + dp(4.5f), cy + dp(7.5f)); c.drawRoundRect(rect, dp(1.6f), dp(1.6f), line)
        val ar = dp(12.5f); val start = 215f; val sweep = 250f
        rect.set(cx - ar, cy - ar, cx + ar, cy + ar); c.drawArc(rect, start, sweep, false, line)
        val e = Math.toRadians((start + sweep).toDouble()); val cs = Math.cos(e).toFloat(); val sn = Math.sin(e).toFloat()
        val px = cx + ar * cs; val py = cy + ar * sn; val len = dp(5f); val wid = dp(3.6f)
        path.reset(); path.moveTo(px - sn * len, py + cs * len); path.lineTo(px + cs * wid, py + sn * wid); path.lineTo(px - cs * wid, py - sn * wid); path.close()
        c.drawPath(path, ink)
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
            inline && inside(full, dp(8f)) -> Hit.FULL
            showQuality && inside(quality, dp(8f)) -> Hit.QUALITY
            showRotate && !inline && inside(rotate, dp(8f)) -> Hit.ROTATE
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
                    Hit.QUALITY -> if (up == Hit.QUALITY) listener?.onQuality()
                    Hit.ROTATE -> if (up == Hit.ROTATE) listener?.onRotate()
                    Hit.FULL -> if (up == Hit.FULL) listener?.onFullscreen()
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
        private const val PlayerRules_FADE_MS = 200L           // the research: Media3 250, NewPipe 300, Gecko 200
        private const val RING_TURN_MS = 900L           // one turn of the buffering ring
        private const val RING_FRAME_MS = 50L           // and a new frame of it this often
        private const val LEVEL_SHOW_MS = 900L          // ANS57: a level bar stays this long after the last change
    }
}
