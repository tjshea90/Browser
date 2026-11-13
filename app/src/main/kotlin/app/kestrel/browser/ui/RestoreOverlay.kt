package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.session.LastLookRules
import app.kestrel.core.session.LookFit

/**
 * Laid over the page view while a page whose engine process died is being brought back (Android reclaimed the memory, or the process crashed): the tab's last
 * preview, dimmed, with one line saying what is happening - never a bare white screen. It disappears at the page's first paint (or after a timeout).
 * Instant like everything else here: no fade, no spinner.
 *
 * The LOOK mode (EY-1, research/REVIEW-EXTERNAL-2.md): the browser opens showing the page exactly as it was left - a sharp picture at the page area's full size, drawn 1:1 from the top-left (or scaled to the width
 * when the screen shape changed), undimmed, no chip - with a thin static line along the top edge saying the live page is still loading, and (when asked) a slim note at the bottom. The page under it is not there yet:
 * touches stop here, and a tap on the picture makes the line brighter (a cue, never a message).
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

    // ---- look mode (EY-1)
    private var look: Bitmap? = null
    private var lookFallback = ""
    private var lookNote = ""
    private var lookDrawn: (() -> Unit)? = null
    private var lineBright = false
    private val line = Paint()

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

    /**
     * EY-1: show the saved picture of the page as it was left. [fallback] is the plain cover's text for a picture that does not fit this screen (rotation, split screen); [note] is the slim line of text at the
     * bottom ("" = none); [onDrawn] runs once after the first draw that really shows the picture (the start-up timeline's "look" mark).
     */
    fun showLook(picture: Bitmap, fallback: String, note: String = "", onDrawn: (() -> Unit)? = null) {
        bmp = null; text = fallback
        look = picture; lookFallback = fallback; lookNote = note; lookDrawn = onDrawn; lineBright = false
        contentDescription = fallback
        visibility = VISIBLE
        invalidate()
    }

    /** The slim note at the bottom of a look ("" = none). Ignored when no look is showing. */
    fun setLookNote(note: String) { if (look != null && lookNote != note) { lookNote = note; invalidate() } }

    val showingLook: Boolean get() = look != null && visibility == VISIBLE

    fun hide() {
        if (visibility != VISIBLE) return
        visibility = GONE
        bmp = null
        val l = look; look = null; lookDrawn = null; lookNote = ""
        if (l != null && !l.isRecycled) l.recycle()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (look != null && e.actionMasked == MotionEvent.ACTION_DOWN && !lineBright) { lineBright = true; invalidate() }
        return super.onTouchEvent(e)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(pal.windowBg)
        val lk = look
        if (lk != null && !lk.isRecycled) {
            when (LastLookRules.fit(lk.width, lk.height, width, height)) {
                LookFit.EXACT -> { c.drawBitmap(lk, 0f, 0f, null); drawLookChrome(c, w, h); fireDrawn(); return }
                LookFit.SCALE -> { val sc = w / lk.width; dst.set(0f, 0f, w, lk.height * sc); c.drawBitmap(lk, null, dst, picture); drawLookChrome(c, w, h); fireDrawn(); return }
                LookFit.NONE -> { text = lookFallback } // a picture of another shape: the plain cover below
            }
        }
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
        drawChip(c, text, w, h * 0.38f, 22f, 48f, 15f)
    }

    /** The static line along the top edge (the live page is loading; no animation) and the slim note at the bottom. */
    private fun drawLookChrome(c: Canvas, w: Float, h: Float) {
        line.color = if (lineBright) pal.accentHi else pal.accent
        c.drawRect(0f, 0f, w, context.dpf(if (lineBright) 4f else 2f), line)
        if (lookNote.isNotEmpty()) drawChip(c, lookNote, w, h - context.dpf(44f), 14f, 34f, 13f)
    }

    private fun drawChip(c: Canvas, message: String, w: Float, centerY: Float, padDp: Float, heightDp: Float, sizeSp: Float) {
        label.color = pal.fg; label.textSize = context.dpf(sizeSp)
        val padX = context.dpf(padDp); val boxH = context.dpf(heightDp)
        val boxW = label.measureText(message) + 2 * padX
        box.set((w - boxW) / 2f, centerY - boxH / 2f, (w + boxW) / 2f, centerY + boxH / 2f)
        ChamferDrawable.path(box, context.dpf(if (heightDp > 40f) 12f else 9f), ChamferDrawable.TL or ChamferDrawable.BR, shape)
        chip.color = pal.card; c.drawPath(shape, chip)
        chipEdge.color = pal.accent; c.drawPath(shape, chipEdge)
        c.drawText(message, box.centerX(), box.centerY() - (label.descent() + label.ascent()) / 2f, label)
    }

    private fun fireDrawn() { val f = lookDrawn ?: return; lookDrawn = null; f() } // in the draw itself: the start-up timeline's "look" mark must not wait for what else is queued
}
