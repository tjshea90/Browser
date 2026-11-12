package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * Laid over the page view while a page whose engine process died is being brought back (Android reclaimed the memory, or the process crashed): the tab's last
 * preview, dimmed, with one line saying what is happening - never a bare white screen. It disappears at the page's first paint (or after a timeout).
 * Instant like everything else here: no fade, no spinner.
 */
class RestoreOverlay(ctx: Context, private var pal: Pal) : View(ctx) {
    private var bmp: Bitmap? = null
    private var text = ""
    private val picture = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dim = Paint().apply { color = 0xB3000000.toInt() }
    private var dimAlpha = 0xB3
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; textSize = ctx.dpf(15f) }
    private val chip = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chipEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = ctx.dpf(1.5f) }
    private val src = Rect()
    private val dst = RectF()
    private val box = RectF()
    private val shape = Path()

    init {
        visibility = GONE
        isClickable = true // the page under it is not there yet: touches stop here
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setPal(p: Pal) { pal = p; invalidate() }

    /** Show [preview] (may be null: then a plain dark panel) with [message] on a chip in the middle. */
    fun show(preview: Bitmap?, message: String, dimAlpha: Int = 0xB3) {
        bmp = preview
        this.dimAlpha = dimAlpha
        dim.color = dimAlpha shl 24
        text = message
        contentDescription = message
        visibility = VISIBLE
        invalidate()
    }

    fun hide() {
        if (visibility != VISIBLE) return
        visibility = GONE
        bmp = null
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(pal.windowBg)
        val b = bmp
        if (b != null && b.width > 0 && !b.isRecycled) {
            // scaled to the full width, top-aligned, bottom cropped (the same way the tab grid shows it)
            val scale = w / b.width
            val srcH = minOf(b.height, (h / scale).toInt().coerceAtLeast(1))
            src.set(0, 0, b.width, srcH)
            dst.set(0f, 0f, w, srcH * scale)
            c.drawBitmap(b, src, dst, picture)
            c.drawRect(0f, 0f, w, h, dim)
        }
        if (text.isEmpty()) return
        label.color = pal.fg
        val padX = context.dpf(22f); val boxH = context.dpf(48f)
        val boxW = label.measureText(text) + 2 * padX
        box.set((w - boxW) / 2f, h * 0.38f - boxH / 2f, (w + boxW) / 2f, h * 0.38f + boxH / 2f)
        ChamferDrawable.path(box, context.dpf(12f), ChamferDrawable.TL or ChamferDrawable.BR, shape)
        chip.color = pal.card; c.drawPath(shape, chip)
        chipEdge.color = pal.accent; c.drawPath(shape, chipEdge)
        c.drawText(text, box.centerX(), box.centerY() - (label.descent() + label.ascent()) / 2f, label)
    }
}
