package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.kestrel.browser.R
import app.kestrel.core.session.LaunchAnim

/**
 * SP1 (owner message 41, item 3): "when Kestrel first loads, it shows a shield icon. Instead of this shield, make it match the symbol from the app drawer. If possible, give it a very quick lightweight animation ...
 * very fast so I don't have to wait." The launch window (black with the app's own symbol, drawn by Android before this process runs) is followed, in the first frame of the app, by this view: the same symbol at the same
 * size and place, over the same black, glowing once in the app's neon blue while the black clears and the screen comes up under it (core [LaunchAnim], about 0.4 s). It never takes a touch, and nothing waits for it: the app
 * underneath is already built and starts its engine as usual. The owner runs Android with animations turned OFF, so it keeps its own clock (like the mini panel's slide) instead of a system animator. The clock starts at the
 * FIRST DRAW (the first frame of the app), not when the view is made: building the screen takes longer than the flourish.
 */
class LaunchLogo(ctx: Context, private val onDone: (LaunchLogo) -> Unit) : View(ctx) {
    private val logo = ctx.getDrawable(R.drawable.ic_launcher_foreground)
    private val accent = ctx.getColor(R.color.accent)
    private val coverPaint = Paint()
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var startAt = -1L
    private var finished = false
    private var haloFor = 0f

    /** Where the clock and the next frame come from: the real ones, unless a test steps them by hand. */
    internal var clock: () -> Long = { SystemClock.uptimeMillis() }
    internal var nextFrame: (Runnable) -> Unit = { postOnAnimation(it) }
    private val again = Runnable { if (!finished) invalidate() }

    init {
        isClickable = false; isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    /** True once the flourish is over (the view has told its owner to remove it). */
    internal val isFinished: Boolean get() = finished

    /** The view never takes a touch: the screen under it works from the first moment. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean = false

    override fun onDraw(c: Canvas) {
        if (finished) return
        if (startAt < 0) startAt = clock()
        val t = (clock() - startAt).toFloat() / LaunchAnim.TOTAL_MS
        if (t >= 1f) { finished = true; onDone(this); return }
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val size = LOGO_DP * resources.displayMetrics.density

        val cover = LaunchAnim.cover(t)
        if (cover > 0.002f) { coverPaint.color = Color.argb((cover * 255f).toInt().coerceIn(0, 255), 0, 0, 0); c.drawRect(0f, 0f, w, h, coverPaint) }

        val glow = LaunchAnim.glow(t)
        if (glow > 0.01f) {
            val maxR = size / 2f * HALO_MAX
            if (haloFor != maxR) { // one gradient at the largest radius; the pulse scales the canvas, no allocation per frame
                haloFor = maxR
                haloPaint.shader = RadialGradient(cx, cy, maxR, intArrayOf(withAlpha(accent, 0xB0), withAlpha(accent, 0x40), withAlpha(accent, 0)), floatArrayOf(0.3f, 0.65f, 1f), Shader.TileMode.CLAMP)
            }
            haloPaint.alpha = (glow * 255f).toInt().coerceIn(0, 255)
            val k = LaunchAnim.haloRadius(t) / HALO_MAX
            c.save(); c.scale(k, k, cx, cy); c.drawCircle(cx, cy, maxR, haloPaint); c.restore()
        }

        val a = LaunchAnim.logoAlpha(t)
        val d = logo
        if (d != null && a > 0.01f) {
            val half = (size / 2f).toInt()
            d.setBounds(cx.toInt() - half, cy.toInt() - half, cx.toInt() + half, cy.toInt() + half)
            d.alpha = (a * 255f).toInt().coerceIn(0, 255)
            val s = LaunchAnim.scale(t)
            c.save(); c.scale(s, s, cx, cy); d.draw(c); c.restore()
        }
        nextFrame(again)
    }

    private fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a shl 24)

    companion object {
        /** The symbol's canvas: the same 148 dp as the launch window (drawable/launch_window.xml). */
        const val LOGO_DP = 148f
        /** The largest radius of the halo, in half sizes of the symbol (LaunchAnim.haloRadius stays under it). */
        private const val HALO_MAX = 1.7f
    }
}
