package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View

/**
 * The progress bar of a download (owner message 26, AL3: "make the status bar about 3-4 times taller then put the status text inside the bar itself centered. The bar should be
 * transparent but it fills up with the neon blue bar as it is downloading. Once the neon blue bar begins crossing the text, make sure the color of the text is well contrasted").
 *
 * - See-through track (a faint tint and a thin outline in the fill colour), the fill grows from the left; unknown size = a solid stripe in the middle (no animation anywhere).
 * - The words sit centred INSIDE the bar and are drawn twice: in the light text colour on the part the fill has not reached, and in the dark "on accent" colour on the filled part. The two
 *   draws are clipped to complementary rectangles, so a letter the fill crosses changes colour exactly at the fill's edge and is readable on both sides (light-on-dark about 17:1,
 *   dark-on-neon about 11:1) - never a light letter on the neon blue.
 * - Text that is wider than the bar shrinks (down to [MIN_SP]) and then gets an ellipsis.
 */
class DownloadBar(ctx: Context, private val pal: Pal, private val textSp: Float = 13f, private val cutDp: Float = 8f) : View(ctx) {
    /** The fill colour family: neon accent normally, red when it stopped with an error, amber when it needs a decision. */
    enum class Tone { NORMAL, WARN, DANGER }

    var fraction = 0f
        set(v) { val c = v.coerceIn(0f, 1f); if (c != field) { field = c; invalidate() } }
    var indeterminate = false
        set(v) { if (v != field) { field = v; invalidate() } }
    var tone = Tone.NORMAL
        set(v) { if (v != field) { field = v; invalidate() } }
    var text = ""
        set(v) { if (v != field) { field = v; fitted = null; contentDescription = v; invalidate() } }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val box = RectF()
    private val path = Path()
    private val stroke = ctx.dpf(1.5f)
    private var fitted: String? = null
    private var fittedWidth = -1

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }

    /** Where the fill ends ([fillRight]) and starts ([fillLeft]) in pixels, for the current size: the same numbers decide the fill and where the text changes colour. */
    private fun fillLeft(w: Float) = if (indeterminate) w * 0.25f else 0f
    private fun fillRight(w: Float) = if (indeterminate) w * 0.75f else w * fraction

    private fun toneColor(): Int = when (tone) { Tone.NORMAL -> pal.accent; Tone.WARN -> pal.warn; Tone.DANGER -> pal.danger }
    private fun alpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a shl 24)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val half = stroke / 2f
        box.set(half, half, w - half, h - half)
        ChamferDrawable.path(box, context.dpf(cutDp), ChamferDrawable.TL or ChamferDrawable.BR, path)
        fitted = null
    }

    /** The text that is drawn, shrunk (and cut) to fit the bar; worked out once per text or width, not per frame. */
    private fun fit(): String {
        fitted?.let { if (fittedWidth == width) return it }
        val avail = (width - context.dpf(16f)).coerceAtLeast(1f)
        var sp = textSp
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
        val want = textPaint.measureText(text)
        if (want > avail) {
            sp = (textSp * avail / want).coerceAtLeast(MIN_SP)
            textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
        }
        val shown = if (textPaint.measureText(text) > avail) TextUtils.ellipsize(text, textPaint, avail, TextUtils.TruncateAt.END).toString() else text
        fitted = shown; fittedWidth = width
        return shown
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val color = toneColor()
        // 1. the see-through track: a whisper of the colour, so the empty part still reads as a bar on the dark card
        paint.style = Paint.Style.FILL; paint.color = alpha(color, 0x14); c.drawPath(path, paint)
        // 2. the fill, cut to the chamfered shape
        val left = fillLeft(w); val right = fillRight(w)
        if (right > left) {
            c.save(); c.clipPath(path)
            paint.color = color; c.drawRect(left, 0f, right, h, paint)
            c.restore()
        }
        // 3. the outline
        paint.style = Paint.Style.STROKE; paint.strokeWidth = stroke; paint.strokeJoin = Paint.Join.MITER; paint.color = alpha(color, 0xC0); c.drawPath(path, paint)
        // 4. the words, centred, in two colours split at the edge of the fill
        if (text.isEmpty()) return
        val shown = fit()
        val x = w / 2f
        val y = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        c.save()
        if (right > left) c.clipOutRect(left, 0f, right, h)
        textPaint.color = pal.fg; c.drawText(shown, x, y, textPaint)
        c.restore()
        if (right > left) {
            c.save(); c.clipRect(left, 0f, right, h)
            textPaint.color = pal.accentOn; c.drawText(shown, x, y, textPaint)
            c.restore()
        }
    }

    companion object { const val MIN_SP = 9f }
}
