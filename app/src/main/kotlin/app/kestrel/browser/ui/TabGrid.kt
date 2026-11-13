package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.kestrel.browser.R
import app.kestrel.browser.web.Tab
import app.kestrel.core.url.UrlParts

/**
 * The tab switcher (owner request): a grid of cards, each with a small preview of the page, like the big browsers. Regular and private tabs are
 * SEPARATE: a segmented control at the top flips between the two lists, the private list is violet, and nothing in it shows while the regular list is open.
 * Tap a card to switch to it, the X to close it. New tab / Done sit at the bottom, within thumb reach. No animation: every change appears in one frame.
 */
class TabGrid(ctx: Context, private var pal: Pal, private val isPrivate: (Tab) -> Boolean, private val l: Listener) : FrameLayout(ctx) {
    interface Listener {
        fun select(tab: Tab)
        fun close(tab: Tab)
        fun newTab(privateTab: Boolean)
        fun closeAll(privateTab: Boolean)
        fun dismiss()
        /** The segmented control flipped: the activity must not allow screenshots while private cards are on screen. */
        fun paneChanged(privateTab: Boolean)
    }

    private val root = LinearLayout(ctx)
    private val seg = LinearLayout(ctx)
    private val segTabs = TextView(ctx)
    private val segPrivate = TextView(ctx)
    private val closeAll = TextView(ctx)
    private val scroller = ScrollView(ctx)
    private val gridBox = LinearLayout(ctx)
    private val bottom = LinearLayout(ctx)
    private val newBtn = TextView(ctx)
    private val doneBtn = TextView(ctx)

    private var tabs: List<Tab> = emptyList()
    private var selectedId = 0L
    var showingPrivate = false
        private set
    private var pendingBuild = false
    private val thumbViews = HashMap<Long, ThumbView>()

    init {
        isClickable = true // swallow touches: nothing reaches the page underneath
        root.orientation = LinearLayout.VERTICAL
        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(12), 0, ctx.dp(6), 0) }
        seg.orientation = LinearLayout.HORIZONTAL
        for (t in listOf(segTabs, segPrivate)) {
            t.gravity = Gravity.CENTER
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            t.setTypeface(t.typeface, Typeface.BOLD)
            t.includeFontPadding = false
            t.minWidth = ctx.dp(112)
            t.setPadding(ctx.dp(16), 0, ctx.dp(16), 0)
            t.isClickable = true
            t.isFocusable = true
            seg.addView(t, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ctx.dp(48)).apply { marginEnd = ctx.dp(8) })
        }
        segTabs.setOnClickListener { setPane(false) }
        segPrivate.setOnClickListener { setPane(true) }
        header.addView(seg, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        closeAll.apply {
            text = "Close all"; gravity = Gravity.CENTER; includeFontPadding = false
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f); setTypeface(typeface, Typeface.BOLD)
            minHeight = ctx.dp(48); setPadding(ctx.dp(14), 0, ctx.dp(14), 0); isClickable = true; isFocusable = true
            setOnClickListener { l.closeAll(showingPrivate) }
        }
        header.addView(closeAll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ctx.dp(48)))
        root.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(64)))

        gridBox.orientation = LinearLayout.VERTICAL
        gridBox.clipChildren = false; gridBox.clipToPadding = false // the selected card's neon glow spills into the gutters
        gridBox.setPadding(ctx.dp(PAD_DP), ctx.dp(4), ctx.dp(PAD_DP), ctx.dp(16))
        scroller.addView(gridBox, ViewGroupLP(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        scroller.isVerticalScrollBarEnabled = false
        scroller.overScrollMode = OVER_SCROLL_NEVER
        root.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.gravity = Gravity.CENTER_VERTICAL
        bottom.setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
        for (b in listOf(newBtn, doneBtn)) {
            b.gravity = Gravity.CENTER; b.includeFontPadding = false
            b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f); b.setTypeface(b.typeface, Typeface.BOLD)
            b.isClickable = true; b.isFocusable = true
        }
        newBtn.setOnClickListener { l.newTab(showingPrivate) }
        doneBtn.setOnClickListener { l.dismiss() }
        doneBtn.text = "Done"
        bottom.addView(newBtn, LinearLayout.LayoutParams(0, ctx.dp(56), 1f).apply { marginEnd = ctx.dp(10) })
        bottom.addView(doneBtn, LinearLayout.LayoutParams(ctx.dp(120), ctx.dp(56)))
        root.addView(bottom, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        applyPal(pal)
    }

    @Suppress("FunctionName")
    private fun ViewGroupLP(w: Int, h: Int) = android.view.ViewGroup.LayoutParams(w, h)

    fun applyPal(p: Pal) {
        pal = p
        setBackgroundColor(0xFF000000.toInt())
        bottom.setBackgroundColor(p.chromeBg)
        closeAll.setTextColor(p.danger)
        paintChrome()
    }

    private fun paintChrome() {
        val ctx = context
        val regular = Pal(ctx, false); val priv = Pal(ctx, true)
        // each segment wears its OWN skin: regular = the regular accent (neon blue since alpha5), private = violet
        fun style(v: TextView, on: Boolean, skin: Pal) {
            v.background = if (on) chamferSurface(ctx, skin.accent, 10f) else chamferSurface(ctx, regular.pill, 10f, strokeColor = skin.divider, strokeDp = 1f)
            v.setTextColor(if (on) skin.accentOn else regular.muted)
        }
        style(segTabs, !showingPrivate, regular)
        style(segPrivate, showingPrivate, priv)
        val skin = if (showingPrivate) priv else regular
        newBtn.background = chamferSurface(ctx, skin.accent, 12f)
        newBtn.setTextColor(skin.accentOn)
        newBtn.text = if (showingPrivate) "+   New private tab" else "+   New tab"
        doneBtn.background = chamferSurface(ctx, skin.pill, 12f, strokeColor = skin.divider, strokeDp = 1.5f)
        doneBtn.setTextColor(skin.fg)
        bottom.setBackgroundColor(skin.chromeBg)
    }

    /** Open on the pane of the tab in front (so a private tab opens the private list), then draw. */
    fun open(all: List<Tab>, selected: Tab?) {
        tabs = all.toList()
        selectedId = selected?.id ?: 0L
        showingPrivate = selected?.let(isPrivate) == true
        l.paneChanged(showingPrivate)
        refresh()
    }

    /** The tab list or a tab's title changed while the grid is open. */
    fun rebind(all: List<Tab>, selected: Tab?) {
        tabs = all.toList()
        selectedId = selected?.id ?: 0L
        refresh()
    }

    private fun setPane(priv: Boolean) {
        if (showingPrivate == priv) return
        showingPrivate = priv
        l.paneChanged(priv)
        refresh()
    }

    /** A preview arrived (capture finished / loaded from disk): swap it into its card without rebuilding the grid. */
    fun thumbChanged(tab: Tab) { if (!tab.homeOverPage) thumbViews[tab.id]?.setBitmap(tab.thumb) } // a tab on Home keeps its Home picture: the page's preview would show the last site again

    /** The Home screen as it looks now (drawn by the activity), for the card of a tab that is on Home. */
    fun homePreview(tab: Tab, b: Bitmap?) { if (tab.homeOverPage) thumbViews[tab.id]?.setBitmap(b) }

    /** Cards need the window width to know their size: build after the layout pass that gave this view its size (adding views from inside it leaves them unmeasured). */
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (w > 0 && (pendingBuild || (w != ow && visibility == VISIBLE))) { pendingBuild = false; post { refresh() } }
    }

    private fun refresh() {
        val ctx = context
        val nRegular = tabs.count { !isPrivate(it) }
        val nPrivate = tabs.size - nRegular
        segTabs.text = "Tabs  $nRegular"
        segPrivate.text = "Private  $nPrivate"
        paintChrome()
        val shown = tabs.filter { isPrivate(it) == showingPrivate }
        closeAll.visibility = if (shown.isEmpty()) GONE else VISIBLE
        gridBox.removeAllViews()
        thumbViews.clear()
        if (width == 0) { pendingBuild = true; return }
        if (shown.isEmpty()) { gridBox.addView(emptyState()); return }

        val avail = width - ctx.dp(PAD_DP) * 2
        val cols = (avail / ctx.dp(MIN_CARD_DP)).coerceIn(2, 4)
        val gap = ctx.dp(GAP_DP)
        val cardW = (avail - gap * (cols - 1)) / cols
        var i = 0
        while (i < shown.size) {
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
            for (c in 0 until cols) {
                val t = shown.getOrNull(i + c)
                val lp = LinearLayout.LayoutParams(cardW, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (c < cols - 1) marginEnd = gap; topMargin = gap }
                if (t == null) row.addView(View(ctx), lp) else row.addView(card(t, cardW), lp)
            }
            gridBox.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            i += cols
        }
    }

    private fun emptyState(): View {
        val ctx = context
        val skin = if (showingPrivate) Pal(ctx, true) else Pal(ctx, false)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(ctx.dp(32), ctx.dp(72), ctx.dp(32), 0) }
        col.addView(ImageView(ctx).apply { setImageResource(if (showingPrivate) R.drawable.ic_mask else R.drawable.ic_tabs); setColorFilter(skin.accent) }, LinearLayout.LayoutParams(ctx.dp(56), ctx.dp(56)))
        col.addView(label(ctx, if (showingPrivate) "No private tabs" else "No tabs", 20f, skin.fg, bold = true).apply { setPadding(0, ctx.dp(16), 0, ctx.dp(6)); includeFontPadding = true })
        col.addView(label(ctx, if (showingPrivate) "Private tabs leave no history, cookies or cache on this device. They are gone when you close them or quit." else "Open a tab to get started.", 14f, skin.muted)
            .apply { gravity = Gravity.CENTER; includeFontPadding = true })
        return col
    }

    private fun card(tab: Tab, cardW: Int): View {
        val ctx = context
        val skin = if (isPrivate(tab)) Pal(ctx, true) else Pal(ctx, false)
        val selected = tab.id == selectedId
        val outer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val inset = ctx.dp(if (selected) 3 else 1)
            setPadding(inset, inset, inset, inset)
            background = ChamferDrawable(skin.card, ctx.dpf(CUT_DP), ChamferDrawable.TL or ChamferDrawable.BR,
                if (selected) skin.accent else skin.divider, ctx.dpf(if (selected) 3f else 1f), gloss = true)
            isClickable = true
            isFocusable = true
            setOnClickListener { l.select(tab) }
            contentDescription = tab.cardTitle
        }
        val host = if (tab.homeOverPage) "" else UrlParts.hostOf(tab.url)?.removePrefix("www.") ?: ""
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(12), 0, 0, 0) }
        val fav = tab.favicon
        if (fav != null && !tab.isNewTab && !tab.homeOverPage) head.addView(ImageView(ctx).apply { setImageBitmap(fav); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(ctx.dp(18), ctx.dp(18)).apply { marginEnd = ctx.dp(8) })
        else head.addView(LetterAvatar(ctx, host.ifEmpty { if (tab.homeOverPage) "home" else "new" }, skin), LinearLayout.LayoutParams(ctx.dp(18), ctx.dp(18)).apply { marginEnd = ctx.dp(8) })
        head.addView(label(ctx, tab.cardTitle, 13f, skin.fg, bold = selected).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(iconButton(ctx, R.drawable.ic_close, "Close tab", skin.muted, 44) { l.close(tab) })
        outer.addView(head, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(44)))
        val thumb = ThumbView(ctx, skin, host, tab.isNewTab, ctx.dpf(CUT_DP - 2f), tab.homeOverPage)
        thumb.setBitmap(if (tab.homeOverPage) null else tab.thumb)
        thumbViews[tab.id] = thumb
        outer.addView(thumb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (cardW * 1.05f).toInt() - ctx.dp(44)))
        if (!selected) return outer.also { it.setOnTouchListener(SwipeToClose(ctx, it) { l.close(tab) }) }
        // the selected card glows (and breathes, see Neon): a GlowView behind it, grown past the card by the glow radius
        val g = ctx.dp(GLOW_DP)
        val wrap = FrameLayout(ctx).apply { clipChildren = false }
        val glow = GlowView(ctx).apply { background = NeonGlowDrawable(skin.accent, ctx.dpf(CUT_DP), ChamferDrawable.TL or ChamferDrawable.BR, ctx.dpf(GLOW_DP.toFloat())) }
        wrap.addView(glow, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply { setMargins(-g, -g, -g, -g) })
        wrap.addView(outer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        Neon.pulse(glow, 0.35f, 1f, 3400L)
        return wrap.also { it.setOnTouchListener(SwipeToClose(ctx, it) { l.close(tab) }) }
    }

    companion object {
        const val PAD_DP = 12
        const val GAP_DP = 12
        const val MIN_CARD_DP = 215
        const val CUT_DP = 14f
        const val GLOW_DP = 10
    }
}

/** The page preview inside a card: the snapshot scaled to the card's width and cropped at the bottom; a letter + host when there is no snapshot yet. */
class ThumbView(ctx: Context, private val pal: Pal, private val host: String, private val isNew: Boolean, private val cut: Float, private val isHome: Boolean = false) : View(ctx) {
    private var bmp: Bitmap? = null
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint().apply { color = 0xFF0A0B0D.toInt() }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.muted; textAlign = Paint.Align.CENTER; textSize = ctx.dpf(13f) }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.accent; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; textSize = ctx.dpf(44f) }
    private val clip = Path()
    private val src = Rect()
    private val dst = RectF()

    fun setBitmap(b: Bitmap?) { if (bmp !== b) { bmp = b; invalidate() } }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        dst.set(0f, 0f, w, h)
        ChamferDrawable.path(dst, cut, ChamferDrawable.BR, clip) // the card's bottom-right corner is cut; the preview follows it
        c.save()
        c.clipPath(clip)
        c.drawRect(0f, 0f, w, h, fill)
        val b = bmp
        if (b != null && b.width > 0) {
            val srcH = minOf(b.height, (b.width * h / w).toInt().coerceAtLeast(1))
            src.set(0, 0, b.width, srcH)
            dst.set(0f, 0f, w, srcH * w / b.width)
            c.drawBitmap(b, src, dst, paint)
        } else if (isHome) {
            // a tab on the Home screen before its picture is drawn: the wordmark of the Home screen, not the page that is behind it
            big.textSize = context.dpf(26f)
            c.drawText("KESTREL", w / 2f, h / 2f - (big.descent() + big.ascent()) / 2f - context.dpf(8f), big)
            c.drawText("Home", w / 2f, h / 2f + context.dpf(34f), text)
        } else {
            val label = if (isNew) "+" else (host.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
            c.drawText(label, w / 2f, h / 2f - (big.descent() + big.ascent()) / 2f - context.dpf(8f), big)
            c.drawText(if (isNew) "New tab" else host, w / 2f, h / 2f + context.dpf(34f), text)
        }
        c.restore()
    }
}


/**
 * Swipe a tab card sideways to close it (promised in three owner replies; the X still works). The card follows the finger (and fades) while it is dragged, which is direct feedback, not
 * an animation: let go past 40 % of its width, or flick it, and the tab closes in that frame; let go earlier and it is back in place in that frame. A tap still selects the tab and a
 * vertical drag still scrolls the grid: only a clearly horizontal drag takes over. [dragged] says whether the last touch was a swipe (the tap must not also fire).
 */
internal class SwipeToClose(ctx: Context, private val card: View, private val onClose: () -> Unit) : View.OnTouchListener {
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var velocity: VelocityTracker? = null

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX; downY = e.rawY; dragging = false
                velocity?.recycle(); velocity = VelocityTracker.obtain().also { it.addMovement(e) }
                return false // the plain tap on the card keeps working
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(e)
                val dx = e.rawX - downX; val dy = e.rawY - downY
                if (!dragging && Math.abs(dx) > slop * 2 && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    dragging = true
                    v.parent?.requestDisallowInterceptTouchEvent(true) // the grid must not take this drag for a scroll
                    val cancel = MotionEvent.obtain(e); cancel.action = MotionEvent.ACTION_CANCEL; v.onTouchEvent(cancel); cancel.recycle() // no tap, no pressed state
                }
                if (dragging) {
                    card.translationX = dx
                    card.alpha = (1f - Math.abs(dx) / (card.width.coerceAtLeast(1) * 0.9f)).coerceIn(0.25f, 1f)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val vt = velocity; velocity = null
                if (!dragging) { vt?.recycle(); return false }
                dragging = false
                vt?.addMovement(e); vt?.computeCurrentVelocity(1000)
                val vx = vt?.xVelocity ?: 0f
                vt?.recycle()
                val dx = e.rawX - downX
                val far = Math.abs(dx) > card.width * 0.4f || (Math.abs(vx) > 1200f && Math.abs(dx) > card.width * 0.15f)
                if (e.actionMasked == MotionEvent.ACTION_UP && far) onClose() else { card.translationX = 0f; card.alpha = 1f }
                return true
            }
        }
        return false
    }
}
