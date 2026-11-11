package app.kestrel.browser.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.browser.R

/** Colors resolved for the current (light/dark) configuration. */
class Pal(ctx: Context, val isPrivate: Boolean = false) {
    val windowBg = ctx.getColor(R.color.window_bg)
    // The private window gets its own dark-violet chrome so it can never be mistaken for a normal window.
    val chromeBg = if (isPrivate) 0xFF2D1B3D.toInt() else ctx.getColor(R.color.chrome_bg)
    val fg = if (isPrivate) 0xFFF3EDF8.toInt() else ctx.getColor(R.color.chrome_fg)
    val muted = if (isPrivate) 0xFFB9A9C9.toInt() else ctx.getColor(R.color.chrome_fg_muted)
    val pill = if (isPrivate) 0xFF443055.toInt() else ctx.getColor(R.color.pill_bg)
    val divider = ctx.getColor(R.color.divider)
    val accent = ctx.getColor(R.color.accent)
    val accentOn = ctx.getColor(R.color.accent_on)
    val danger = ctx.getColor(R.color.danger)
    val warn = ctx.getColor(R.color.warn)
    val ok = ctx.getColor(R.color.ok)
    val card = ctx.getColor(R.color.card_bg)
    val scrim = ctx.getColor(R.color.scrim)
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun roundedBg(color: Int, radiusPx: Float, strokeColor: Int = 0, strokePx: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx
        if (strokePx > 0) setStroke(strokePx, strokeColor)
    }

/** Touch feedback WITHOUT animation (the owner wants everything instant): the pressed/focused state shows a flat overlay at once and is gone at once. */
fun ripple(color: Int, content: Drawable?, radiusPx: Float = 0f): Drawable {
    val tint = (color and 0x00FFFFFF) or 0x33000000
    fun overlay(): Drawable = if (radiusPx > 0f) roundedBg(tint, radiusPx) else ColorDrawable(tint)
    val states = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), overlay())
        addState(intArrayOf(android.R.attr.state_focused), overlay())
        addState(intArrayOf(), ColorDrawable(0))
        setEnterFadeDuration(0)
        setExitFadeDuration(0)
    }
    return if (content == null) states else LayerDrawable(arrayOf(content, states))
}

fun label(ctx: Context, text: CharSequence, sizeSp: Float, color: Int, bold: Boolean = false): TextView =
    TextView(ctx).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = false
    }

/** Toolbar-style icon button with accessible label and ripple. */
fun iconButton(ctx: Context, icon: Int, description: String, tint: Int, sizeDp: Int = 44, onClick: () -> Unit): ImageButton =
    ImageButton(ctx).apply {
        setImageResource(icon)
        setColorFilter(tint)
        contentDescription = description
        scaleType = ImageView.ScaleType.CENTER
        background = ripple(tint, null, 0f)
        layoutParams = LinearLayout.LayoutParams(ctx.dp(sizeDp), ctx.dp(sizeDp))
        val pad = ctx.dp((sizeDp - 24) / 2)
        setPadding(pad, pad, pad, pad)
        setOnClickListener { onClick() }
    }

/** A filled/outlined rounded "button" TextView. */
fun pillButton(ctx: Context, text: String, bg: Int, fg: Int, onClick: () -> Unit): TextView =
    label(ctx, text, 15f, fg, bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ctx.dp(44)
        minimumWidth = ctx.dp(96)
        setPadding(ctx.dp(20), ctx.dp(10), ctx.dp(20), ctx.dp(10))
        background = ripple(fg, roundedBg(bg, ctx.dpf(22f)), ctx.dpf(22f))
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

fun textButton(ctx: Context, text: String, fg: Int, onClick: () -> Unit): TextView =
    label(ctx, text, 15f, fg, bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ctx.dp(44)
        setPadding(ctx.dp(16), ctx.dp(10), ctx.dp(16), ctx.dp(10))
        background = ripple(fg, null, ctx.dpf(22f))
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

/** Thin load-progress bar (2dp) drawn in the accent color. */
class ProgressLine(ctx: Context, private val color: Int) : View(ctx) {
    private val paint = Paint().apply { this.color = this@ProgressLine.color }
    var progress = 0
        set(v) { if (field != v) { field = v; visibility = if (v in 1..99) VISIBLE else INVISIBLE; invalidate() } }

    override fun onDraw(c: Canvas) {
        c.drawRect(0f, 0f, width * progress / 100f, height.toFloat(), paint)
    }
}

/** The "N tabs" button: a rounded square outline with the count inside (no bitmap assets, themed by color). */
class TabsButton(ctx: Context, private val color: Int) : View(ctx) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ctx.dpf(2f); this.color = this@TabsButton.color
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = ctx.dpf(12f); typeface = Typeface.DEFAULT_BOLD; this.color = this@TabsButton.color
    }
    private val box = RectF()
    var count = 1
        set(v) { field = v; contentDescription = "Tabs, $v open"; invalidate() }

    init {
        background = ripple(color, null, 0f)
        isClickable = true
        isFocusable = true
        contentDescription = "Tabs, 1 open"
    }

    override fun onDraw(c: Canvas) {
        val s = context.dpf(22f)
        val l = (width - s) / 2f
        val t = (height - s) / 2f
        box.set(l, t, l + s, t + s)
        c.drawRoundRect(box, context.dpf(5f), context.dpf(5f), stroke)
        val label = if (count > 99) ":D" else count.toString()
        c.drawText(label, width / 2f, height / 2f - (text.descent() + text.ascent()) / 2f, text)
    }
}

/** Shield icon + blocked-count for the current page; tapping opens the Shields panel. */
class ShieldChip(ctx: Context, private val pal: Pal, onClick: () -> Unit) : LinearLayout(ctx) {
    private val icon = ImageView(ctx).apply {
        setImageResource(R.drawable.ic_shield)
        layoutParams = LayoutParams(ctx.dp(22), ctx.dp(22))
    }
    private val count = label(ctx, "", 12f, pal.fg, bold = true).apply { setPadding(ctx.dp(3), 0, 0, 0) }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        minimumWidth = ctx.dp(44)
        minimumHeight = ctx.dp(44)
        setPadding(ctx.dp(8), 0, ctx.dp(8), 0)
        addView(icon); addView(count)
        background = ripple(pal.fg, null, 0f)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

    fun bind(shieldsUp: Boolean, blocked: Int) {
        icon.setColorFilter(if (shieldsUp) pal.accent else pal.muted)
        count.text = if (blocked > 0) (if (blocked > 999) "999+" else blocked.toString()) else ""
        count.setTextColor(if (shieldsUp) pal.fg else pal.muted)
        contentDescription = if (shieldsUp) "Shields on, $blocked blocked on this page" else "Shields off for this site"
    }
}
