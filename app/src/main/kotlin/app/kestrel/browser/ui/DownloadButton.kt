package app.kestrel.browser.ui

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import app.kestrel.browser.R

/**
 * The download icon of the navigation bar (owner request X17): GRAY while the page has nothing to save, NEON BLUE-TEAL with a breathing glow around it while the page has videos or audio to
 * download. A tap opens the list of what was found (names, sizes, resolutions, file types). The glow is the same kind of [GlowView] as the address field's and breathes on the same clock
 * ([Neon]), so it follows Settings > Neon glow (Off = the colour change alone, Steady = no breathing) and holds still while a page loads or the keyboard is up, like every other glow.
 */
class DownloadButton(ctx: Context, private var pal: Pal, private val onTap: () -> Unit) : FrameLayout(ctx) {
    private val glow = GlowView(ctx)
    private val icon = ImageButton(ctx)
    private var count = 0

    init {
        clipChildren = false
        clipToPadding = false
        // the ring around the icon: a 44 x 34 dp tube with its bloom inside a 54 x 44 dp view, centred in the cell
        addView(glow, LayoutParams(ctx.dp(54), ctx.dp(44), Gravity.CENTER))
        icon.apply {
            setImageResource(R.drawable.ic_download)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0, ctx.dp(15), 0, ctx.dp(15))
            setOnClickListener { onTap() }
        }
        addView(icon, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        Neon.pulse(glow, 0.3f, 1f, 3400L)
        glow.visibility = GONE
        paint()
    }

    /** How many things the page offers right now (0 = gray and calm). */
    fun setCount(n: Int) {
        val c = n.coerceAtLeast(0)
        if (c == count) return
        val was = count > 0
        count = c
        if ((c > 0) != was) paint() else icon.contentDescription = description()
    }

    fun setPal(p: Pal) { pal = p; paint() }

    private fun description(): String = if (count > 0) "Download video: $count found" else "Download video: nothing found on this page yet"

    private fun paint() {
        val on = count > 0
        icon.setColorFilter(if (on) pal.accentHi else pal.muted)
        icon.alpha = if (on) 1f else 0.55f
        icon.background = ripple(pal.fg, null, 0f)
        icon.contentDescription = description()
        glow.background = NeonGlowDrawable(pal.accent, context.dpf(8f), ChamferDrawable.TL or ChamferDrawable.BR, context.dpf(5f), coreColor = pal.accentHi, coreWidthPx = context.dpf(1.4f))
        glow.visibility = if (on) VISIBLE else GONE
    }
}
