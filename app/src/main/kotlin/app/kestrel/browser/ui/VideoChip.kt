package app.kestrel.browser.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.browser.R

/**
 * "Save video": the always-visible way to the download list while the page has something to save (owner request X10: "force a download button in addition to a video's own player menu").
 * A site's own player can swallow a long press and shows its own menu, so the browser offers its button on the page itself. A tap opens the list; the cross hides it for this page.
 */
class VideoChip(ctx: Context) : LinearLayout(ctx) {
    private val icon = ImageView(ctx).apply { setImageResource(R.drawable.ic_download) }
    private val text: TextView = label(ctx, "Save video", 14f, 0xFFFFFFFF.toInt(), bold = true)
    private val cross: TextView = textButton(ctx, "✕", 0xFFFFFFFF.toInt()) { onHide?.invoke() }
    var onOpen: (() -> Unit)? = null
    var onHide: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ctx.dp(12), 0, 0, 0)
        elevation = ctx.dpf(6f)
        visibility = View.GONE
        addView(icon, LayoutParams(ctx.dp(22), ctx.dp(22)))
        addView(text, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = ctx.dp(8); marginEnd = ctx.dp(6) })
        addView(cross, LayoutParams(ctx.dp(40), ctx.dp(44)))
        setOnClickListener { onOpen?.invoke() }
        contentDescription = "Save video"
    }

    private var shownPal: Pal? = null
    private var shownCount = -1

    /** Called on every UI refresh while a page has media: nothing is rebuilt when the colours and the count are what is already on screen. */
    fun show(pal: Pal, count: Int) {
        if (visibility == View.VISIBLE && pal === shownPal && count == shownCount) return
        if (pal !== shownPal) {
            background = roundedBg(pal.card, context.dpf(24f), pal.accent, context.dp(2))
            icon.setColorFilter(pal.accent)
            text.setTextColor(pal.accent)
            cross.setTextColor(pal.muted)
            shownPal = pal
        }
        if (count != shownCount) { text.text = if (count > 1) "Save video ($count)" else "Save video"; shownCount = count }
        visibility = View.VISIBLE
    }

    fun hide() { if (visibility != View.GONE) visibility = View.GONE }
}
