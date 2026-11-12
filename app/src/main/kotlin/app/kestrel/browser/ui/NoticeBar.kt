package app.kestrel.browser.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A small notice over the page, above the dock: one line of text, an optional action ("Open") and a dismiss button; it hides itself after a few seconds.
 * Used for "Blocked a pop-up from ..." (with Open, in case the window was wanted) and the like. No animation: it appears and disappears.
 */
class NoticeBar(ctx: Context) : LinearLayout(ctx) {
    private val text: TextView = label(ctx, "", 13f, 0xFFFFFFFF.toInt())
    private val handler = Handler(Looper.getMainLooper())
    private val hideLater = Runnable { hide() }
    private var action: (() -> Unit)? = null
    private val actionView: TextView = textButton(ctx, "", 0xFFFFFFFF.toInt()) { val a = action; hide(); a?.invoke() }
    private val closeView: TextView = textButton(ctx, "Dismiss", 0xFFFFFFFF.toInt()) { hide() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ctx.dp(14), ctx.dp(4), ctx.dp(4), ctx.dp(4))
        visibility = View.GONE
        elevation = ctx.dpf(6f)
        addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(actionView, LayoutParams(LayoutParams.WRAP_CONTENT, ctx.dp(44)))
        addView(closeView, LayoutParams(LayoutParams.WRAP_CONTENT, ctx.dp(44)))
    }

    /** Shows [message] with the colours of [pal]; [actionLabel] adds a button that runs [onAction]. Replaces whatever was showing. */
    fun show(pal: Pal, message: String, actionLabel: String? = null, ms: Long = 7000L, onAction: (() -> Unit)? = null) {
        handler.removeCallbacks(hideLater)
        background = roundedBg(pal.card, context.dpf(14f), pal.accent, context.dp(1))
        text.setTextColor(pal.fg); text.text = message
        action = onAction
        actionView.text = actionLabel ?: ""
        actionView.setTextColor(pal.accent)
        actionView.visibility = if (actionLabel != null && onAction != null) View.VISIBLE else View.GONE
        closeView.setTextColor(pal.muted)
        visibility = View.VISIBLE
        handler.postDelayed(hideLater, ms)
    }

    fun hide() { handler.removeCallbacks(hideLater); visibility = View.GONE; action = null }
}
