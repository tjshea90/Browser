package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.pow

/**
 * The neon (owner request 4): a very subtle pulsing blue glow in a few logical places - the edge above the dock, the Home field, the selected tab card - like the
 * glow around a phone screen while an assistant talks. Everything else in the app stays instant.
 *
 * Cost design: a pulse is ONLY an alpha change of a [GlowView] that holds a static, pre-built glow drawable (a RenderNode property: no bitmap re-drawn, no layout, no
 * off-screen layer). It is stepped at 10 Hz instead of animated at the display's 60 Hz: a slow breath changes ~1-2 % of alpha per step, which the eye cannot tell
 * from a smooth ramp, and every step costs one tiny frame - six times fewer frames than a RenderThread animation on a tablet whose GPU would otherwise idle.
 * While a glow runs it is cached as a small hardware layer (the strokes are drawn once; a step only re-blends a texture); at rest the layer is dropped.
 * All glows share one clock, so they breathe together. The glows hold still (a steady glow, no frames) while the keyboard is up, during the first seconds of a page load, while a finger is on the screen (and
 * after a swipe, for the fling), in battery-saver, when the app is not on screen, and after [IDLE_MS] without a touch while a page is in front - so a tablet left on a page lets the screen go quiet and the SoC idle.
 * NOT gated on the system's animation scale (the owner runs with animations off and still wants this).
 */
object Neon {
    const val OFF = 0
    const val STATIC = 1
    const val PULSE = 2
    /** No touch for this long, with a page in front: the glows settle to a steady glow until the next touch (never on Home, the tab grid or a sheet: see [canRest]). */
    const val IDLE_MS = 120_000L

    /** 0 = no glow at all, 1 = steady glow, 2 = subtle pulse (default). Set from Settings. */
    @JvmField @Volatile var mode = PULSE

    /** Set by the activity: false while Home / the tab grid / a sheet is in front. There nothing else happens on screen, so the glow keeps breathing. */
    @JvmField var canRest: () -> Boolean = { true }

    /** One breath every 3.4 s, stepped every 100 ms (10 Hz). */
    private const val TICK_MS = 100L

    private class Pulser(val v: GlowView, val lo: Float, val hi: Float, val periodMs: Long) { var running = false }

    private val pulsers = ArrayList<Pulser>() // UI thread only
    private var ticking = false
    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            var any = false
            for (p in pulsers) if (p.running) {
                any = true
                val phase = (now % p.periodMs) / p.periodMs.toFloat() // one shared clock: every glow breathes in step
                val k = 0.5f - 0.5f * cos(phase * 6.2831855f) // 0..1..0, eased at both ends
                p.v.alpha = p.lo + (p.hi - p.lo) * k
            }
            ticking = any
            if (any) handler.postDelayed(this, TICK_MS)
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false // typing or loading: the screen is already changing, hold the glow still
    private var touching = false // a finger is on the screen or a fling is still running: never compete with the page's scrolling for frames
    private var away = true // the app is not on screen
    private var covered = false // a full-screen page (Downloads, Settings, History, Diagnostics) hides every glow: nothing to animate
    private var resting = false // no touch for IDLE_MS
    private var powerSave = false
    private var lastTouch = 0L
    private var restPosted = false
    private val restCheck = Runnable {
        restPosted = false
        val left = IDLE_MS - (SystemClock.uptimeMillis() - lastTouch)
        if (away) return@Runnable
        if (left > 0) postRestCheck(left) else if (!resting) { if (canRest()) { resting = true; applyAll() } else postRestCheck(IDLE_MS) }
    }

    /** Make [v] breathe between alpha [lo] and [hi]. Attach once per view; it follows the view's attach/detach/visibility by itself. */
    fun pulse(v: GlowView, lo: Float = 0.5f, hi: Float = 1f, periodMs: Long = 4200L) {
        val p = Pulser(v, lo, hi, periodMs)
        pulsers.add(p)
        v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { apply(p, view.isShown) }
            override fun onViewDetachedFromWindow(view: View) { stop(p); pulsers.remove(p) }
        })
        if (v.isAttachedToWindow) apply(p, v.isShown)
    }

    /** Typing or loading: hold every glow still (true) or let them breathe again (false). */
    fun busy(on: Boolean) { if (busy != on) { busy = on; applyAll() } }

    /** A finger went down (true) / the last one is up and the fling has had time to end (false). The glows hold still while true. */
    fun touching(on: Boolean) { if (touching != on) { touching = on; applyAll() } }

    /** A full-screen page covers the glows (true) / the glows are in view again (false): no frames for glows nobody can see. */
    fun covered(on: Boolean) { if (covered != on) { covered = on; applyAll() } }

    /** The app came to the front / went away. Re-reads battery-saver. */
    fun foreground(on: Boolean, ctx: Context? = null) {
        if (ctx != null) powerSave = try { ctx.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true } catch (e: Exception) { false }
        away = !on
        handler.removeCallbacks(restCheck); restPosted = false
        if (on) { lastTouch = SystemClock.uptimeMillis(); resting = false; postRestCheck(IDLE_MS) }
        applyAll()
    }

    /** Any touch on the screen (cheap: a clock read; the rest timer is re-armed lazily). */
    fun touched() {
        lastTouch = SystemClock.uptimeMillis()
        if (away) return
        if (resting) { resting = false; applyAll() }
        if (!restPosted) postRestCheck(IDLE_MS)
    }

    /** Re-read the mode and start or stop every pulse (after a settings change). */
    fun refresh() = applyAll()

    internal fun visibility(v: GlowView, visible: Boolean) { for (p in pulsers) if (p.v === v) apply(p, visible) }

    private fun postRestCheck(delay: Long) { restPosted = true; handler.postDelayed(restCheck, delay) }
    private fun applyAll() { for (p in pulsers.toList()) apply(p, p.v.isShown) }
    // NOT gated on the system's animation scale (Developer options): the owner switches system animations off and still asked for this pulse; Settings > Neon glow is the switch.
    private fun animated(): Boolean = mode == PULSE && !busy && !touching && !away && !resting && !powerSave && !covered

    /** One line for Diagnostics / Settings: is the glow breathing, and if not, why. */
    fun status(): String {
        if (mode == OFF) return "off (Settings)"
        if (mode == STATIC) return "steady (Settings)"
        val why = ArrayList<String>()
        if (busy) why.add("keyboard up or page just started loading"); if (touching) why.add("finger on screen"); if (away) why.add("app not in front")
        if (resting) why.add("idle rest"); if (powerSave) why.add("battery saver"); if (covered) why.add("a full-screen page covers them")
        val running = pulsers.count { it.running }
        return if (why.isEmpty()) "pulsing ($running glows running)" else "holding still: " + why.joinToString(", ")
    }

    private fun apply(p: Pulser, visible: Boolean) {
        when {
            mode == OFF -> { stop(p); p.v.alpha = 0f }
            visible && animated() -> start(p)
            else -> { stop(p); p.v.alpha = p.lo + (p.hi - p.lo) * 0.7f } // a steady glow at about 70 % of the peak
        }
    }

    private fun start(p: Pulser) {
        if (p.running) return
        p.running = true
        p.v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (!ticking) { ticking = true; handler.post(tick) }
    }

    private fun stop(p: Pulser) { // the tick loop ends by itself when nobody is running
        if (!p.running) return
        p.running = false
        p.v.setLayerType(View.LAYER_TYPE_NONE, null)
    }
}

/** A view that only holds a glow drawable: no overlapping rendering (alpha is applied without an off-screen layer) and no size of its own (it never makes its parent grow). */
class GlowView(ctx: Context) : View(ctx) {
    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun hasOverlappingRendering(): Boolean = false
    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(if (MeasureSpec.getMode(w) == MeasureSpec.EXACTLY) MeasureSpec.getSize(w) else 0, if (MeasureSpec.getMode(h) == MeasureSpec.EXACTLY) MeasureSpec.getSize(h) else 0)
    }
    override fun onVisibilityAggregated(isVisible: Boolean) { super.onVisibilityAggregated(isVisible); Neon.visibility(this, isVisible) }
}

/**
 * The neon around a chamfered edge: three bloom strokes, wide and faint to narrow and strong, inside the bounds (the shape is inset by [glowPx]), and - when [coreColor]
 * is given - a crisp 1.6 dp ring just OUTSIDE the shape (the neon tube itself: when the view's alpha breathes, this is the line the eye follows).
 * Built from plain strokes - no blur, no RenderEffect - so it costs one cheap draw list.
 */
class NeonGlowDrawable(private val color: Int, private val cutPx: Float, private val corners: Int, private val glowPx: Float, private val coreColor: Int = 0, private val coreWidthPx: Float = 0f) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val corePath = Path()
    private val box = RectF()
    private val coreBox = RectF()

    override fun onBoundsChange(b: Rect) {
        box.set(b.left + glowPx, b.top + glowPx, b.right - glowPx, b.bottom - glowPx)
        ChamferDrawable.path(box, cutPx, corners, path)
        val d = coreWidthPx * 0.5f
        coreBox.set(box.left - d, box.top - d, box.right + d, box.bottom + d)
        ChamferDrawable.path(coreBox, cutPx + d * 0.42f, corners, corePath)
    }

    override fun draw(c: Canvas) {
        val rgb = color and 0x00FFFFFF
        paint.color = (0x18 shl 24) or rgb; paint.strokeWidth = glowPx * 2f; c.drawPath(path, paint)
        paint.color = (0x2C shl 24) or rgb; paint.strokeWidth = glowPx * 1.3f; c.drawPath(path, paint)
        paint.color = (0x50 shl 24) or rgb; paint.strokeWidth = glowPx * 0.7f; c.drawPath(path, paint)
        if (coreColor != 0 && coreWidthPx > 0f) { paint.color = (0xF0 shl 24) or (coreColor and 0x00FFFFFF); paint.strokeWidth = coreWidthPx; c.drawPath(corePath, paint) }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * The 2 dp line between the page and the dock - load progress in neon blue over a faint resting hairline - plus a soft halo that rises above it into the page
 * (drawn above this view's bounds: the parent must not clip children). Only [halo] pulses ([Neon.pulse]); the line itself never changes alpha.
 */
class NeonBar(ctx: Context, private var accent: Int, private var hi: Int) : FrameLayout(ctx) {
    val halo = GlowView(ctx)
    private val line = Line(ctx)
    private val haloPx = ctx.dp(HALO_DP)

    var progress = 0
        set(v) { if (field != v) { field = v; line.invalidate() } }

    init {
        clipChildren = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(halo, LayoutParams(LayoutParams.MATCH_PARENT, haloPx, Gravity.TOP).apply { topMargin = -haloPx })
        addView(line, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        paintHalo()
    }

    fun setColors(accent: Int, hi: Int) { this.accent = accent; this.hi = hi; paintHalo(); line.invalidate() }

    private fun paintHalo() {
        val rgb = accent and 0x00FFFFFF
        halo.background = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setColors(intArrayOf(rgb, rgb or 0x24000000, rgb or 0x80000000.toInt()), floatArrayOf(0f, 0.55f, 1f)) // quiet far from the line, brightest at it
        }
    }

    private inner class Line(ctx: Context) : View(ctx) {
        private val paint = Paint()
        init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        override fun hasOverlappingRendering(): Boolean = false
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            paint.color = (accent and 0x00FFFFFF) or 0x66000000; c.drawRect(0f, 0f, w, h, paint) // the resting line
            if (progress in 1..99) {
                val x = w * progress / 100f
                paint.color = accent; c.drawRect(0f, 0f, x, h, paint)
                paint.color = hi; c.drawRect(maxOf(0f, x - context.dpf(26f)), 0f, x, h, paint) // the bright head
            }
        }
    }

    companion object { const val HALO_DP = 16 }
}

/**
 * The Home screen's backdrop, drawn from the owner's reaper photo as a MOOD, not a copy: pure black, two ghostly blue flames rising from the bottom corners (a
 * procedural mask generated once, 40 KB) and the thin glowing arc of the scythe in the top-left. Static: no animation, no per-frame work beyond replaying a few draw ops.
 */
class HomeBackdrop(ctx: Context, private var accent: Int, private var deep: Int, private var hi: Int) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var wisps: Bitmap? = null
    private val dst = RectF()
    private val arc = Path()
    private var wispShader: LinearGradient? = null
    private var arcShader: LinearGradient? = null

    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun hasOverlappingRendering(): Boolean = false

    fun setColors(accent: Int, deep: Int, hi: Int) { this.accent = accent; this.deep = deep; this.hi = hi; wispShader = null; arcShader = null; invalidate() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (wisps == null) Thread({ val m = Wisps.mask(); post { wisps = m; invalidate() } }, "kestrel-wisps").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        wispShader = null; arcShader = null
        arc.reset()
        arc.moveTo(w * 0.04f, h * 0.20f)
        arc.cubicTo(w * 0.16f, h * 0.085f, w * 0.40f, h * 0.045f, w * 0.70f, h * 0.075f)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        // flames: two mirrored copies of the mask in the bottom corners, tinted deep blue at the base to ice at the tips
        wisps?.let { m ->
            val fw = w * 0.58f; val fh = h * 0.46f
            if (wispShader == null) wispShader = LinearGradient(0f, h - fh, 0f, h, intArrayOf(hi, accent, deep), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            paint.shader = wispShader; paint.alpha = 125
            dst.set(0f, h - fh, fw, h); c.drawBitmap(m, null, dst, paint)
            c.save(); c.scale(-1f, 1f, w / 2f, 0f); dst.set(0f, h - fh, fw, h); c.drawBitmap(m, null, dst, paint); c.restore()
            paint.shader = null; paint.alpha = 255
        }
        // the scythe: a faint wide bloom, a thin bright edge that fades toward its tip
        if (arcShader == null) arcShader = LinearGradient(w * 0.04f, 0f, w * 0.70f, 0f, accent or 0xFF000000.toInt(), accent and 0x00FFFFFF, Shader.TileMode.CLAMP)
        paint.style = Paint.Style.STROKE; paint.shader = arcShader; paint.strokeCap = Paint.Cap.ROUND
        paint.alpha = 40; paint.strokeWidth = context.dpf(9f); c.drawPath(arc, paint)
        paint.alpha = 90; paint.strokeWidth = context.dpf(3.5f); c.drawPath(arc, paint)
        paint.alpha = 230; paint.strokeWidth = context.dpf(1.4f); c.drawPath(arc, paint)
        paint.shader = null; paint.style = Paint.Style.FILL; paint.alpha = 255
    }
}

/**
 * Ghostly flame tongues as an ALPHA_8 mask (square, drawn once on a background thread, 40 KB): fifteen curved plumes rising from the bottom edge, leaning toward the
 * screen centre, each a tapering filled shape drawn twice (a wide faint bloom, then a softer-edged body). Overlap combines like screen blend (SRC_OVER on alpha).
 * x = 0 is the outer screen edge; the caller mirrors the mask for the other side and tints it with a gradient.
 */
object Wisps {
    fun mask(size: Int = 200): Bitmap {
        val s = size.toFloat()
        val bm = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
        val c = Canvas(bm)
        val rnd = java.util.Random(11)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val path = Path()
        val steps = 28
        repeat(15) {
            val x0 = rnd.nextFloat().toDouble().pow(1.3).toFloat() * 0.92f // 0 = outer edge, 1 = toward the centre
            val reach = (1f - x0).pow(0.55f) * (0.45f + 0.55f * rnd.nextFloat()) // tongue height as a fraction of the mask
            val lean = 0.05f + 0.20f * rnd.nextFloat()
            val sway = (0.015f + 0.04f * rnd.nextFloat()) * (if (rnd.nextFloat() < 0.5f) 1f else -1f)
            val freq = 2.2f + 2.6f * rnd.nextFloat()
            val ph = rnd.nextFloat() * 6.28f
            val w0 = (0.020f + 0.030f * rnd.nextFloat()) * (1f - 0.4f * x0)
            val alpha = 90 + (90 * rnd.nextFloat()).toInt()
            val soft = 2.0f + 1.6f * rnd.nextFloat()
            path.reset()
            val rx = FloatArray(steps + 1); val ry = FloatArray(steps + 1)
            for (k in 0..steps) {
                val t = k / steps.toFloat()
                val cx = x0 + lean * t.pow(1.5f) + sway * kotlin.math.sin(t * freq + ph) * (0.3f + t)
                val cy = 1.02f - t * reach
                val hw = w0 * (1f - t).pow(0.85f) + 0.0015f
                if (k == 0) path.moveTo((cx - hw) * s, cy * s) else path.lineTo((cx - hw) * s, cy * s)
                rx[k] = (cx + hw) * s; ry[k] = cy * s
            }
            for (k in steps downTo 0) path.lineTo(rx[k], ry[k])
            path.close()
            paint.maskFilter = BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL); paint.alpha = (alpha * 0.45f).toInt(); c.drawPath(path, paint) // the wide faint bloom
            paint.maskFilter = BlurMaskFilter(soft, BlurMaskFilter.Blur.NORMAL); paint.alpha = alpha; c.drawPath(path, paint) // the body
        }
        // a low glow along the bottom edge so the tongues grow out of something, and a soft fade at the inner side (no hard edge at the mask's border)
        val px = ByteArray(size * size)
        bm.copyPixelsToBuffer(ByteBuffer.wrap(px))
        for (y in 0 until size) {
            val u = 1f - y / s
            val rise = (1f - u / 0.16f).coerceIn(0f, 1f).pow(1.8f)
            for (x in 0 until size) {
                val fx = x / s
                val base = rise * (1f - fx).pow(1.2f) * 55f
                var f = ((1f - fx) / 0.30f).coerceIn(0f, 1f); f = f * f * (3f - 2f * f)
                val a = ((px[y * size + x].toInt() and 0xFF) + base).coerceAtMost(255f) * f
                px[y * size + x] = a.toInt().coerceIn(0, 255).toByte()
            }
        }
        bm.copyPixelsFromBuffer(ByteBuffer.wrap(px))
        return bm
    }
}
