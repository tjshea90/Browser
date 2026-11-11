package app.kestrel.browser.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.PowerManager
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.pow

/**
 * The neon (owner request 4): a very subtle pulsing blue glow in a few logical places - the edge above the dock, the Home field, the selected tab card - like the
 * glow around a phone screen while an assistant talks. Everything else in the app stays instant.
 *
 * Cost design: a pulse is ONLY an alpha animation of a view that holds a static, pre-built glow drawable. [View.animate] on a hardware-accelerated view runs on the
 * RenderThread (RenderNodeAnimator): no per-frame work on the UI thread, no bitmaps re-drawn, no layout. The UI thread wakes twice per period to chain the next half.
 * Pulses stop by themselves while typing or loading, in battery-saver, when the system's animations are off, and when the app is not on screen.
 */
object Neon {
    const val OFF = 0
    const val STATIC = 1
    const val PULSE = 2

    /** 0 = no glow at all, 1 = steady glow, 2 = subtle pulse (default). Set from Settings. */
    @JvmField @Volatile var mode = PULSE

    private class Pulser(val v: View, val lo: Float, val hi: Float, val halfMs: Long) { var running = false }

    private val pulsers = ArrayList<Pulser>() // UI thread only
    private val ease = AccelerateDecelerateInterpolator()
    private var held = false // the app is busy (typing / loading) or in the background: hold the glow still
    private var powerSave = false

    /** Make [v] breathe between alpha [lo] and [hi]. Attach once per view; it follows the view's attach/detach by itself. */
    fun pulse(v: View, lo: Float = 0.5f, hi: Float = 1f, periodMs: Long = 4200L) {
        val p = Pulser(v, lo, hi, periodMs / 2)
        pulsers.add(p)
        v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { apply(p) }
            override fun onViewDetachedFromWindow(view: View) { stop(p); pulsers.remove(p) }
        })
        if (v.isAttachedToWindow) apply(p)
    }

    /** Hold every glow still (typing, loading, app in the background) or let them breathe again. */
    fun hold(on: Boolean) { if (held != on) { held = on; refresh() } }

    /** Re-read the mode / power state and start or stop every pulse. Call after a settings change and on resume. */
    fun refresh(ctx: Context? = null) {
        if (ctx != null) powerSave = try { ctx.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true } catch (e: Exception) { false }
        for (p in pulsers.toList()) apply(p)
    }

    private fun animated(): Boolean = mode == PULSE && !held && !powerSave && ValueAnimator.areAnimatorsEnabled()

    private fun apply(p: Pulser) {
        when {
            mode == OFF -> { stop(p); p.v.alpha = 0f }
            animated() && p.v.isShown -> start(p)
            else -> { stop(p); p.v.alpha = if (mode == OFF) 0f else p.lo + (p.hi - p.lo) * 0.7f } // a steady glow at about 70 % of the peak
        }
    }

    private fun start(p: Pulser) {
        if (p.running) return
        p.running = true
        p.v.alpha = p.lo
        step(p, true)
    }

    private fun stop(p: Pulser) {
        if (!p.running) return
        p.running = false
        p.v.animate().cancel()
    }

    private fun step(p: Pulser, up: Boolean) {
        if (!p.running) return
        p.v.animate().alpha(if (up) p.hi else p.lo).setDuration(p.halfMs).setInterpolator(ease).withEndAction { step(p, !up) }.start()
    }
}

/** A view that only holds a glow drawable: no overlapping rendering, so alpha is applied without an off-screen layer. */
class GlowView(ctx: Context) : View(ctx) {
    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun hasOverlappingRendering(): Boolean = false
}

/**
 * The bloom around a chamfered edge: four strokes, wide and faint to narrow and strong, inside the bounds (the shape is inset by [glowPx]).
 * Built from plain strokes - no blur, no RenderEffect - so it costs one cheap draw list.
 */
class NeonGlowDrawable(private val color: Int, private val cutPx: Float, private val corners: Int, private val glowPx: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val box = RectF()

    override fun onBoundsChange(b: Rect) {
        box.set(b.left + glowPx, b.top + glowPx, b.right - glowPx, b.bottom - glowPx)
        ChamferDrawable.path(box, cutPx, corners, path)
    }

    override fun draw(c: Canvas) {
        val rgb = color and 0x00FFFFFF
        paint.color = (0x0E shl 24) or rgb; paint.strokeWidth = glowPx * 2f; c.drawPath(path, paint)
        paint.color = (0x1A shl 24) or rgb; paint.strokeWidth = glowPx * 1.35f; c.drawPath(path, paint)
        paint.color = (0x30 shl 24) or rgb; paint.strokeWidth = glowPx * 0.8f; c.drawPath(path, paint)
        paint.color = (0x66 shl 24) or rgb; paint.strokeWidth = glowPx * 0.38f; c.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * The 2 dp line between the page and the dock: load progress in neon blue, and a soft halo that rises above it into the page (drawn above the view's own bounds:
 * its parent must not clip children). The halo is what pulses ([Neon.pulse] on this view).
 */
class NeonProgress(ctx: Context, private var accent: Int, private var hi: Int) : View(ctx) {
    private val paint = Paint()
    private val haloPx = ctx.dpf(16f)
    private var halo: LinearGradient? = null
    var progress = 0
        set(v) { if (field != v) { field = v; invalidate() } }

    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun setColors(accent: Int, hi: Int) { this.accent = accent; this.hi = hi; halo = null; invalidate() }
    override fun hasOverlappingRendering(): Boolean = false

    override fun onDraw(c: Canvas) {
        if (halo == null) halo = LinearGradient(0f, -haloPx, 0f, 0f, accent and 0x00FFFFFF, (accent and 0x00FFFFFF) or 0x3C000000, Shader.TileMode.CLAMP)
        paint.shader = halo; c.drawRect(0f, -haloPx, width.toFloat(), 0f, paint)
        paint.shader = null
        paint.color = (accent and 0x00FFFFFF) or 0x59000000; c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint) // the resting line
        if (progress in 1..99) {
            val x = width * progress / 100f
            paint.color = accent; c.drawRect(0f, 0f, x, height.toFloat(), paint)
            paint.color = hi; c.drawRect(maxOf(0f, x - context.dpf(26f)), 0f, x, height.toFloat(), paint) // the bright head
        }
    }
}

/**
 * The Home screen's backdrop, drawn from the owner's reaper photo as a MOOD, not a copy: pure black, a faint vignette, two ghostly blue flames rising from the
 * bottom corners (a procedural mask generated once, 40 KB) and the thin glowing arc of the scythe in the top-left. Static; only its alpha breathes (very slowly).
 */
class HomeBackdrop(ctx: Context, private var accent: Int, private var deep: Int, private var hi: Int) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var wisps: Bitmap? = null
    private val dst = RectF()
    private val arc = Path()
    private var wispShader: LinearGradient? = null
    private var arcShader: LinearGradient? = null
    private var vignette: RadialGradient? = null

    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun hasOverlappingRendering(): Boolean = false

    fun setColors(accent: Int, deep: Int, hi: Int) { this.accent = accent; this.deep = deep; this.hi = hi; wispShader = null; arcShader = null; invalidate() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (wisps == null) Thread({ val m = Wisps.mask(); post { wisps = m; invalidate() } }, "kestrel-wisps").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        wispShader = null; arcShader = null
        vignette = RadialGradient(w / 2f, h * 0.45f, maxOf(w, h) * 0.75f, intArrayOf(0x00000000, 0x00000000, 0xCC000000.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        arc.reset()
        arc.moveTo(w * 0.04f, h * 0.20f)
        arc.cubicTo(w * 0.16f, h * 0.085f, w * 0.40f, h * 0.045f, w * 0.70f, h * 0.075f)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        // flames: two mirrored copies of the mask in the bottom corners, tinted deep blue at the base to ice at the tips
        wisps?.let { m ->
            val fw = w * 0.74f; val fh = h * 0.46f
            if (wispShader == null) wispShader = LinearGradient(0f, h - fh, 0f, h, intArrayOf(hi, accent, deep), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            paint.shader = wispShader; paint.alpha = 150
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
        vignette?.let { paint.shader = it; c.drawRect(0f, 0f, w, h, paint); paint.shader = null }
    }
}

/** Ghostly flame wisps as an ALPHA_8 mask: warped fractal value noise that thickens toward the bottom and fades toward the inner side (the caller mirrors it). */
object Wisps {
    fun mask(w: Int = 240, h: Int = 150): Bitmap {
        val px = ByteArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val nx = x / w.toFloat(); val ny = y / h.toFloat()
            val wx = nx * 3.2f + 0.9f * fbm(nx * 2.3f, ny * 2.3f - 7f)
            val wy = ny * 3.6f - 1.1f * fbm(nx * 2.0f + 5f, ny * 2.0f)
            val n = fbm(wx * 1.6f, wy * 1.6f)
            val rise = ny.pow(1.15f)
            val side = (1f - nx).pow(0.8f)
            var a = ((n - 0.40f) * 2.7f).coerceIn(0f, 1f) * rise * side
            a = a * a * (3f - 2f * a)
            px[y * w + x] = (a * 255f).toInt().coerceIn(0, 255).toByte()
        }
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        bm.copyPixelsFromBuffer(ByteBuffer.wrap(px))
        return bm
    }

    private fun hash(ix: Int, iy: Int): Float {
        var n = ix * 374761393 + iy * 668265263
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0xFFFF) / 65535f
    }

    private fun vnoise(x: Float, y: Float): Float {
        val ix = floor(x).toInt(); val iy = floor(y).toInt()
        val fx = x - ix; val fy = y - iy
        val sx = fx * fx * (3f - 2f * fx); val sy = fy * fy * (3f - 2f * fy)
        val a = hash(ix, iy); val b = hash(ix + 1, iy); val c = hash(ix, iy + 1); val d = hash(ix + 1, iy + 1)
        return (a + (b - a) * sx) + ((c + (d - c) * sx) - (a + (b - a) * sx)) * sy
    }

    private fun fbm(x: Float, y: Float): Float {
        var amp = 0.5f; var f = 1f; var sum = 0f
        repeat(4) { sum += amp * vnoise(x * f, y * f); f *= 2.03f; amp *= 0.5f }
        return sum / 0.9375f
    }
}
