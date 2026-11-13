package app.kestrel.browser.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import app.kestrel.browser.R

/**
 * A page the guard refused, shown as ONE very small icon at the bottom middle for one second (AR1, owner message 32: "instead of the notification on the bottom, put a very small icon in the bottom middle that is not
 * distracting that symbolizes a page blocked, and have it disappear in one second. If I press the icon before it disappears, it still open the blocked page in a new tab without any further prompts").
 *
 * The icon is 26 dp; the part that takes a press is 44 dp so a thumb can still hit it inside that second. A second block inside the second keeps the icon up for another full second and the NEWEST address
 * is the one a press opens. No fade, no sheet, no question: a press opens the address in a new tab through [onOpen] and the icon is gone. An address the browser cannot open (no http / https address, for
 * example an empty window) still shows the icon, so the owner knows something was stopped, and a press just dismisses it.
 */
class BlockedChip(ctx: Context) : FrameLayout(ctx) {
    private val bubble = ImageView(ctx)
    private val handler = Handler(Looper.getMainLooper())
    private val hideLater = Runnable { hide() }
    private var target: String? = null
    private var onOpen: ((String) -> Unit)? = null

    init {
        visibility = GONE
        isClickable = true
        contentDescription = "A page was blocked. Tap to open it in a new tab."
        elevation = ctx.dpf(4f)
        bubble.setImageResource(R.drawable.ic_page_blocked)
        bubble.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val pad = ctx.dp(5)
        bubble.setPadding(pad, pad, pad, pad)
        addView(bubble, LayoutParams(ctx.dp(28), ctx.dp(28), Gravity.CENTER))
        setOnClickListener { open() }
    }

    /** The address a press would open now (null when there is none). */
    val pendingUrl: String? get() = target

    val showing: Boolean get() = visibility == VISIBLE

    /** Shows the icon for [LIFETIME_MS] more; [url] (http / https only, else null) is what a press opens, through [onOpen]. */
    fun show(pal: Pal, url: String?, onOpen: (String) -> Unit) {
        handler.removeCallbacks(hideLater)
        bubble.background = roundedBg(0xCC000000.toInt(), context.dpf(14f), pal.deep, context.dp(1))
        bubble.setColorFilter(pal.muted)
        target = url?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        this.onOpen = onOpen
        visibility = VISIBLE
        handler.postDelayed(hideLater, LIFETIME_MS)
    }

    fun hide() {
        handler.removeCallbacks(hideLater)
        visibility = GONE
        target = null
        onOpen = null
    }

    private fun open() {
        val u = target; val f = onOpen
        hide()
        if (u != null) f?.invoke(u)
    }

    companion object {
        /** How long the icon stays after the latest block. */
        const val LIFETIME_MS = 1000L
        /** The size of the part that takes a press, in dp (the icon itself is smaller). */
        const val TOUCH_DP = 44
    }
}
