package app.kestrel.browser.ui

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import app.kestrel.browser.R

/**
 * A bottom sheet drawn as an overlay inside the activity (not a PopupWindow: no second window, no focus theft, appears in the same frame).
 * The navigation lives at the bottom, so everything it opens - the menu, the Shields panel - rises from the bottom too, within thumb reach.
 */
class SheetHost(ctx: Context, private var pal: Pal) : FrameLayout(ctx) {
    private val panel = LinearLayout(ctx)
    private val scroller = ScrollView(ctx)
    var onDismiss: (() -> Unit)? = null
    val isShowing: Boolean get() = visibility == VISIBLE

    init {
        visibility = GONE
        isClickable = true
        setOnClickListener { dismiss() } // a tap on the dimmed area closes the sheet
        panel.orientation = LinearLayout.VERTICAL
        panel.isClickable = true // taps inside the sheet never reach the scrim
        scroller.isVerticalScrollBarEnabled = false
        scroller.overScrollMode = OVER_SCROLL_NEVER
        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        applyPal(pal)
    }

    fun applyPal(p: Pal) {
        pal = p
        setBackgroundColor(p.scrim)
        panel.background = ChamferDrawable(p.card, context.dpf(20f), ChamferDrawable.TL, p.divider, context.dpf(1f))
    }

    /** Show [content] (scrolls if taller than the screen allows) under an optional [title] row with a close button. */
    fun show(title: String?, content: View) {
        panel.removeAllViews()
        scroller.removeAllViews()
        (content.parent as? ViewGroup)?.removeView(content)
        val ctx = context
        if (title != null) {
            val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(20), 0, ctx.dp(4), 0) }
            header.addView(label(ctx, title, 18f, pal.fg, bold = true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            header.addView(iconButton(ctx, R.drawable.ic_close, "Close", pal.muted, 52) { dismiss() })
            panel.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(56)))
        }
        scroller.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        panel.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        visibility = VISIBLE
        scroller.scrollTo(0, 0)
    }

    fun dismiss() {
        if (!isShowing) return
        visibility = GONE
        panel.removeAllViews()
        scroller.removeAllViews()
        onDismiss?.invoke()
    }

    /** The panel is at most 640 dp wide on a wide window and never taller than 84 % of the screen. */
    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        val height = MeasureSpec.getSize(h)
        setMeasuredDimension(width, height)
        val panelW = minOf(width, context.dp(640))
        panel.measure(MeasureSpec.makeMeasureSpec(panelW, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((height * 0.84f).toInt(), MeasureSpec.AT_MOST))
    }
}

/** Menu entries as big tiles (two columns, three on a wide window): icon + label, 56 dp tall, a check mark for toggles that are on. */
fun menuGrid(ctx: Context, pal: Pal, items: List<MenuItemSpec>, columns: Int, onPick: (Int) -> Unit): View {
    val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(ctx.dp(12), ctx.dp(16), ctx.dp(12), ctx.dp(12)) }
    var i = 0
    while (i < items.size) {
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        for (c in 0 until columns) {
            val it = items.getOrNull(i + c)
            if (it == null) { row.addView(View(ctx), LinearLayout.LayoutParams(0, ctx.dp(56), 1f).apply { setMargins(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(4)) }); continue }
            row.addView(tile(ctx, pal, it, onPick), LinearLayout.LayoutParams(0, ctx.dp(56), 1f).apply { setMargins(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(4)) })
        }
        col.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        i += columns
    }
    return col
}

private fun tile(ctx: Context, pal: Pal, spec: MenuItemSpec, onPick: (Int) -> Unit): View {
    val on = spec.checked == true
    val t = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ctx.dp(14), 0, ctx.dp(10), 0)
        background = chamferSurface(ctx, pal.pill, 9f, strokeColor = if (on) pal.accent else 0, strokeDp = if (on) 1.5f else 0f)
        isClickable = true
        isFocusable = true
        setOnClickListener { onPick(spec.id) }
        contentDescription = spec.text
    }
    if (spec.icon != 0) t.addView(ImageView(ctx).apply { setImageResource(spec.icon); setColorFilter(if (on) pal.accent else pal.fg) },
        LinearLayout.LayoutParams(ctx.dp(24), ctx.dp(24)).apply { marginEnd = ctx.dp(12) })
    t.addView(label(ctx, spec.text, 15f, pal.fg, bold = on).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true },
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    if (spec.checked == true) t.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_check); setColorFilter(pal.accent) }, LinearLayout.LayoutParams(ctx.dp(20), ctx.dp(20)))
    return t
}
