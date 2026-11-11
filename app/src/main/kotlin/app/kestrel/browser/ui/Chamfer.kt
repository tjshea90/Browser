package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable

/**
 * The CERE shape language (docs/DESIGN.md): a rectangle with 45-degree cut corners instead of rounded ones - the cut sits on the top-left and bottom-right
 * by default, like a feather tip. One shape for the address field, the Home field, buttons, tab cards and sheets; nothing else in the app is a pill.
 */
class ChamferDrawable(
    private val fill: Int,
    private val cutPx: Float,
    private val corners: Int = TL or BR,
    private val strokeColor: Int = 0,
    private val strokePx: Float = 0f,
) : Drawable() {
    companion object {
        const val TL = 1; const val TR = 2; const val BR = 4; const val BL = 8
        const val ALL = 15

        /** The outline of [r] with the given corners cut by [cut]; shared with views that draw the shape themselves. */
        fun path(r: RectF, cut: Float, corners: Int, out: Path) {
            val c = minOf(cut, (r.right - r.left) / 2f, (r.bottom - r.top) / 2f)
            out.reset()
            if (corners and TL != 0) { out.moveTo(r.left, r.top + c); out.lineTo(r.left + c, r.top) } else out.moveTo(r.left, r.top)
            if (corners and TR != 0) { out.lineTo(r.right - c, r.top); out.lineTo(r.right, r.top + c) } else out.lineTo(r.right, r.top)
            if (corners and BR != 0) { out.lineTo(r.right, r.bottom - c); out.lineTo(r.right - c, r.bottom) } else out.lineTo(r.right, r.bottom)
            if (corners and BL != 0) { out.lineTo(r.left + c, r.bottom); out.lineTo(r.left, r.bottom - c) } else out.lineTo(r.left, r.bottom)
            out.close()
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val box = RectF()

    override fun onBoundsChange(b: Rect) {
        val h = strokePx / 2f
        box.set(b.left + h, b.top + h, b.right - h, b.bottom - h)
        path(box, cutPx, corners, path)
    }

    override fun draw(c: Canvas) {
        if (Color.alpha(fill) != 0) { paint.style = Paint.Style.FILL; paint.color = fill; c.drawPath(path, paint) }
        if (strokePx > 0f && Color.alpha(strokeColor) != 0) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = strokePx; paint.strokeJoin = Paint.Join.MITER; paint.color = strokeColor
            c.drawPath(path, paint)
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** [base] moved toward white (dark colours) or black (light colours): the instant pressed state of a filled surface. */
fun pressedShade(base: Int): Int {
    val lum = (0.299 * Color.red(base) + 0.587 * Color.green(base) + 0.114 * Color.blue(base)) / 255.0
    val to = if (lum > 0.5) 0 else 255
    val t = if (lum > 0.5) 0.14f else 0.16f
    fun mix(a: Int) = (a + (to - a) * t).toInt().coerceIn(0, 255)
    return Color.argb(Color.alpha(base), mix(Color.red(base)), mix(Color.green(base)), mix(Color.blue(base)))
}

/** A chamfered surface with an instant pressed state (no animation: the state list swaps drawables at once). */
fun chamferSurface(
    ctx: Context, fill: Int, cutDp: Float = 8f, corners: Int = ChamferDrawable.TL or ChamferDrawable.BR,
    strokeColor: Int = 0, strokeDp: Float = 0f, pressable: Boolean = true,
): Drawable {
    val cut = ctx.dpf(cutDp); val sw = ctx.dpf(strokeDp)
    val normal = ChamferDrawable(fill, cut, corners, strokeColor, sw)
    if (!pressable) return normal
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), ChamferDrawable(pressedShade(fill), cut, corners, strokeColor, sw))
        addState(intArrayOf(), normal)
        setEnterFadeDuration(0)
        setExitFadeDuration(0)
    }
}
