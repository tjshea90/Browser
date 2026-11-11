package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import app.kestrel.browser.R

/**
 * The pull-down-to-refresh indicator: a 52 dp chamfered chip with the refresh arrow that rides down from the top edge of the page while a finger pulls, and turns
 * accent-filled (with a short tick from the haptic motor, if the tablet has one) once the pull is long enough to reload. Drawn over the page - the page view itself
 * never moves - and instant like everything here: it only follows the finger, nothing animates on its own.
 */
class PullChip(ctx: Context, private var pal: Pal) : View(ctx) {
    private val size = ctx.dp(52)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val icon: Drawable? = ctx.getDrawable(R.drawable.ic_refresh)?.mutate()
    private val path = Path()
    private val box = RectF()
    private var turn = 0f
    private var armed = false

    init {
        visibility = GONE
        isClickable = false; isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        val pad = ctx.dpf(6f)
        box.set(pad, pad, size - pad, size - pad)
        ChamferDrawable.path(box, ctx.dpf(12f), ChamferDrawable.TL or ChamferDrawable.BR, path)
    }

    fun setPal(p: Pal) { pal = p; invalidate() }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(size, size)

    /** [travelPx]: how far the chip's top edge has come down past the page's top edge; [progress]: 0..1 toward the reload threshold; [isArmed]: letting go now reloads. */
    fun pull(travelPx: Float, progress: Float, isArmed: Boolean) {
        translationY = travelPx - size
        val t = progress.coerceIn(0f, 1f) * 280f
        if (t != turn || isArmed != armed) { turn = t; armed = isArmed; invalidate() }
        if (visibility != VISIBLE) visibility = VISIBLE
    }

    fun hide() { if (visibility == VISIBLE) { visibility = GONE; translationY = -size.toFloat() } }

    override fun onDraw(c: Canvas) {
        val bloom = pal.accent and 0x00FFFFFF
        edge.color = (0x2C shl 24) or bloom; edge.strokeWidth = context.dpf(7f); c.drawPath(path, edge)
        edge.color = (0x50 shl 24) or bloom; edge.strokeWidth = context.dpf(3.5f); c.drawPath(path, edge)
        fill.color = if (armed) pal.accent else pal.card; c.drawPath(path, fill)
        edge.color = if (armed) pal.accentHi else pal.accent; edge.strokeWidth = context.dpf(1.6f); c.drawPath(path, edge)
        val d = icon ?: return
        val s = context.dp(24)
        val l = (size - s) / 2
        d.setBounds(l, l, l + s, l + s)
        d.setTint(if (armed) pal.windowBg else pal.accentHi)
        c.save(); c.rotate(turn, size / 2f, size / 2f); d.draw(c); c.restore()
    }
}
