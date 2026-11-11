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

/** Colors for the CERE identity. The private skin is violet end to end (chrome, accent, progress) so it can never be mistaken for a normal tab. */
class Pal(ctx: Context, val isPrivate: Boolean = false) {
    val windowBg = ctx.getColor(R.color.window_bg)
    val chromeBg = if (isPrivate) 0xFF130D21.toInt() else ctx.getColor(R.color.chrome_bg)
    val fg = if (isPrivate) 0xFFF2ECFF.toInt() else ctx.getColor(R.color.chrome_fg)
    val muted = if (isPrivate) 0xFFA797C8.toInt() else ctx.getColor(R.color.chrome_fg_muted)
    val pill = if (isPrivate) 0xFF231939.toInt() else ctx.getColor(R.color.pill_bg)
    val divider = if (isPrivate) 0xFF33264F.toInt() else ctx.getColor(R.color.divider)
    val accent = if (isPrivate) 0xFFB794FF.toInt() else ctx.getColor(R.color.accent)
    val accentOn = if (isPrivate) 0xFF120A22.toInt() else ctx.getColor(R.color.accent_on)
    val danger = ctx.getColor(R.color.danger)
    val warn = ctx.getColor(R.color.warn)
    val ok = ctx.getColor(R.color.ok)
    val card = if (isPrivate) 0xFF1A1230.toInt() else ctx.getColor(R.color.card_bg)
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

/** Touch feedback WITHOUT animation (the owner wants everything instant): the pressed state shows a flat overlay at once and is gone at once. */
fun ripple(color: Int, content: Drawable?, radiusPx: Float = 0f): Drawable {
    val tint = (color and 0x00FFFFFF) or 0x33000000
    fun overlay(): Drawable = if (radiusPx > 0f) roundedBg(tint, radiusPx) else ColorDrawable(tint)
    val states = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), overlay())
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

/** A filled chamfered button (the primary action of a screen or dialog). 48dp tall, instant pressed state. */
fun pillButton(ctx: Context, text: String, bg: Int, fg: Int, onClick: () -> Unit): TextView =
    label(ctx, text, 16f, fg, bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ctx.dp(48)
        minimumWidth = ctx.dp(96)
        setPadding(ctx.dp(22), ctx.dp(10), ctx.dp(22), ctx.dp(10))
        background = chamferSurface(ctx, bg, 9f)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

/** A quiet chamfered button with an outline (secondary action). */
fun outlineButton(ctx: Context, text: String, fill: Int, stroke: Int, fg: Int, onClick: () -> Unit): TextView =
    label(ctx, text, 16f, fg, bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ctx.dp(48)
        minimumWidth = ctx.dp(96)
        setPadding(ctx.dp(22), ctx.dp(10), ctx.dp(22), ctx.dp(10))
        background = chamferSurface(ctx, fill, 9f, strokeColor = stroke, strokeDp = 1.5f)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

fun textButton(ctx: Context, text: String, fg: Int, onClick: () -> Unit): TextView =
    label(ctx, text, 16f, fg, bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ctx.dp(48)
        setPadding(ctx.dp(16), ctx.dp(10), ctx.dp(16), ctx.dp(10))
        background = ripple(fg, null, 0f)
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

/** The "N tabs" button: a chamfered square outline with the count inside (no bitmap assets, themed by color). A private-mode dot sits on its corner. */
class TabsButton(ctx: Context, color: Int) : View(ctx) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = ctx.dpf(2f); strokeJoin = Paint.Join.MITER; this.color = color
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = ctx.dpf(13f); typeface = Typeface.DEFAULT_BOLD; this.color = color
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private val outline = android.graphics.Path()
    var count = 1
        set(v) { if (field != v) { field = v; contentDescription = "Tabs, $v open"; invalidate() } }
    /** True in the private skin: a filled dot marks the button (the number then counts private tabs). */
    var privateDot = false
        set(v) { if (field != v) { field = v; invalidate() } }

    init {
        background = ripple(color, null, 0f)
        isClickable = true
        isFocusable = true
        contentDescription = "Tabs, 1 open"
    }

    fun setPalette(fg: Int, accent: Int) {
        stroke.color = fg; text.color = fg; dot.color = accent
        background = ripple(fg, null, 0f)
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val s = context.dpf(24f)
        val l = (width - s) / 2f
        val t = (height - s) / 2f
        box.set(l, t, l + s, t + s)
        ChamferDrawable.path(box, context.dpf(6f), ChamferDrawable.TL or ChamferDrawable.BR, outline)
        c.drawPath(outline, stroke)
        val label = if (count > 99) ":D" else count.toString()
        c.drawText(label, width / 2f, height / 2f - (text.descent() + text.ascent()) / 2f, text)
        if (privateDot) c.drawCircle(box.right, box.top, context.dpf(5f), dot)
    }
}

/** Shield icon + blocked-count for the current page; tapping opens the Shields panel. */
class ShieldChip(ctx: Context, private var pal: Pal, onClick: () -> Unit) : LinearLayout(ctx) {
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

    private var lastUp = true
    private var lastBlocked = 0

    /** Skin change (regular <-> private): repaint with the new palette. */
    fun setPal(p: Pal) {
        pal = p
        background = ripple(p.fg, null, 0f)
        bind(lastUp, lastBlocked)
    }

    fun bind(shieldsUp: Boolean, blocked: Int) {
        lastUp = shieldsUp; lastBlocked = blocked
        icon.setColorFilter(if (shieldsUp) pal.accent else pal.muted)
        count.text = if (blocked > 0) (if (blocked > 999) "999+" else blocked.toString()) else ""
        count.setTextColor(if (shieldsUp) pal.fg else pal.muted)
        contentDescription = if (shieldsUp) "Shields on, $blocked blocked on this page" else "Shields off for this site"
    }
}
