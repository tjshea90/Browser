package app.kestrel.browser.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.graphics.Canvas
import android.graphics.Paint
import app.kestrel.core.ui.FloatBox
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.browser.ProcessLife
import app.kestrel.browser.R
import app.kestrel.browser.download.DownloadController
import app.kestrel.core.download.Action
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Format

/**
 * The small download window (owner message 26, AL2): "if I start any download at all, whether video or files, put a pop up at the top of the browser that a download has started, and
 * then put a small progress window in the top right of the browser of all downloads. A mini status window that is not overly distracting and I can close it with an x button on the
 * top right, but this doesn't stop the downloads. There should also be mini pause (and resume when paused) buttons next to each download, a status bar, and an x button to cancel and
 * delete the unfinished download."
 *
 * - A banner across the top says "Download started" with the file's name for a few seconds (tap it to open the Downloads list).
 * - Below it, at the right, a small window lists the downloads of this run: name, pause / resume, X, and the same neon bar as the Downloads list with the numbers inside.
 *   The X on a row cancels that download and DELETES its unfinished file (asking first when part of it is already on the tablet); on a finished row it only closes the row. The X in the
 *   header only hides the window (the downloads go on); the next new download brings it back.
 * - It follows [DownloadController] (about four updates a second), draws nothing while the app is not on screen, and what it lists is decided by [MiniTracker] (tested without a screen).
 * - No animation anywhere: things appear and disappear at once.
 */
class DownloadPopup(private val ctx: Context, private val pal: Pal, private val l: Listener) {
    interface Listener {
        fun openManager()
        fun confirm(title: String, message: String, yes: String, then: () -> Unit)
        /** FD1: where the person put the window and how big it is (gap to the right edge, gap to the top, width, height of the list, in dp); null = the first place and size. */
        fun geometry(): IntArray? = null
        fun geometryChanged(g: IntArray?) {}
    }

    /** Add this to the activity's root layout (above the page, below the full-screen screens and dialogs). It is empty (zero height) while there is nothing to show. */
    val view = FrameLayout(ctx).apply { visibility = View.GONE }   // FD1: it fills the page area; empty places let every touch through (it is not clickable), the window floats inside it

    private val handler = Handler(Looper.getMainLooper())
    private val tracker = DownloadController.mini      // DM1: one memory for the whole process, so a re-created window knows what is old news
    private var attached = false
    private var closedByUser: Boolean
        get() = DownloadController.miniHidden
        set(v) { DownloadController.miniHidden = v }
    private var suppressed = false
    private var lastSig = ""
    private val rows = HashMap<Long, MiniRow>()

    // ---- the banner
    private val bannerTitle = label(ctx, "Download started", 13f, pal.fg, bold = true).apply { includeFontPadding = true }
    private val bannerName = label(ctx, "", 12f, pal.muted).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true }
    private val banner = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; visibility = View.GONE
        setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
        background = ChamferDrawable(pal.card, ctx.dpf(10f), ChamferDrawable.TL or ChamferDrawable.BR, pal.accent, ctx.dpf(1.5f), gloss = true)
        isClickable = true
        setOnClickListener { hideBanner(); l.openManager() }
        addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_download); setColorFilter(pal.accent) }, LinearLayout.LayoutParams(ctx.dp(26), ctx.dp(26)).apply { marginEnd = ctx.dp(10) })
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(bannerTitle); col.addView(bannerName)
        addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
    private val hideBannerLater = Runnable { hideBanner() }

    // ---- the window
    private val count = label(ctx, "", 12f, pal.muted).apply { includeFontPadding = true }
    private val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    /** FD1: the list scrolls inside the height the person gave the window. */
    private inner class LimitedScroll(c: Context) : ScrollView(c) {
        var maxPx = 0
        override fun onMeasure(w: Int, h: Int) { super.onMeasure(w, if (maxPx > 0) MeasureSpec.makeMeasureSpec(maxPx, MeasureSpec.AT_MOST) else h) }
    }
    private val scroll = LimitedScroll(ctx).apply { isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
    private val more = label(ctx, "", 12f, pal.accent, bold = true).apply { gravity = Gravity.CENTER; includeFontPadding = true; visibility = View.GONE; setPadding(0, ctx.dp(8), 0, ctx.dp(4)); setOnClickListener { l.openManager() } }
    private val window = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL; visibility = View.GONE
        setPadding(ctx.dp(10), ctx.dp(4), ctx.dp(6), ctx.dp(8))
        background = ChamferDrawable(pal.card, ctx.dpf(10f), ChamferDrawable.TL or ChamferDrawable.BR, 0x99000000.toInt() or (pal.accent and 0x00FFFFFF), ctx.dpf(1f), gloss = true)
        isClickable = true // the window never lets a tap fall through to the page behind it
    }

    private val density = ctx.resources.displayMetrics.density
    private fun px(dp: Float) = Math.round(dp * density)
    private val limits = FloatBox.Limits(minW = 200f, maxW = 420f, minH = 90f, maxH = 640f)
    private val defaults = FloatBox.Default(right = 8f, top = 8f, w = WINDOW_DP.toFloat(), h = DEFAULT_LIST_DP.toFloat())
    private var geo: FloatBox.Geo = FloatBox.load(l.geometry(), defaults)
    private var lit = false
    private val handle = object : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = ctx.dpf(2f); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        override fun onDraw(c: Canvas) {
            paint.color = if (lit) pal.accentHi else pal.accent; paint.alpha = if (lit) 255 else 170
            val w = width.toFloat(); val h = height.toFloat(); val p = ctx.dpf(5f)
            for (k in 0..2) { val o = p + k * ctx.dpf(4.5f); c.drawLine(p, h - o, o, h - p, paint) }
        }
    }.apply { contentDescription = "Drag to resize the downloads window; double tap to put it back" }
    private val box = FloatFrame(ctx)

    init {
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setOnClickListener { l.openManager() } }
        titleBox.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_download); setColorFilter(pal.accent) }, LinearLayout.LayoutParams(ctx.dp(16), ctx.dp(16)).apply { marginEnd = ctx.dp(6) })
        titleBox.addView(label(ctx, "Downloads", 12f, pal.fg, bold = true).apply { includeFontPadding = true })
        titleBox.addView(count, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = ctx.dp(6) })
        head.addView(titleBox, LinearLayout.LayoutParams(0, ctx.dp(36), 1f))
        head.addView(smallButton(R.drawable.ic_close, "Hide this window (downloads keep going)", pal.muted) { closedByUser = true; render() })
        window.addView(head, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        scroll.addView(list, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        window.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        window.addView(more, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        box.addView(window, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        box.addView(handle, FrameLayout.LayoutParams(ctx.dp(HANDLE_DP), ctx.dp(HANDLE_DP), Gravity.BOTTOM or Gravity.START))
        view.addView(banner, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP).apply { setMargins(ctx.dp(8), ctx.dp(8), ctx.dp(8), 0) })
        view.addView(box, FrameLayout.LayoutParams(px(geo.w), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))
        view.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or2, ob -> if (r - l != or2 - ol || b - t != ob - ot) place() }
        place()
    }

    // ---- FD1: the window floats: moved by a finger on its title bar or by a long press anywhere on it, resized from the corner at its bottom left, remembered, brought back by a double tap on that corner

    /** The page area in dp (0 until the first layout). */
    private fun areaW() = view.width / density
    private fun areaH() = view.height / density
    private fun boxH() = if (box.height > 0) box.height / density else 140f

    /** The place and size in the layout parameters: the stored choice kept inside the page area (the stored numbers stay as they are: a screen that is smaller for a moment must not move the window for good). */
    private fun place() {
        val lp = box.layoutParams as? FrameLayout.LayoutParams ?: return
        val aw = areaW(); val ah = areaH()
        val g = if (aw > 0f && ah > 0f) FloatBox.clamp(geo, aw, ah, boxH(), limits) else geo
        val shift = if (banner.visibility == View.VISIBLE && FloatBox.isDefault(geo, defaults)) banner.height + ctx.dp(8) else 0   // at its first place it sits below the banner while the banner shows
        lp.width = px(g.w); lp.rightMargin = px(g.right); lp.topMargin = px(g.top) + shift
        scroll.maxPx = px(g.h)
        box.layoutParams = lp
    }

    // tests: the window shown without any download, and where it is
    internal fun showForTest() { window.visibility = View.VISIBLE; refreshContainer(); place() }
    internal fun boxForTest(): View = box
    internal fun handleForTest(): View = handle
    internal fun headerBottomForTest(): Int = window.top + ctx.dp(36)
    internal fun geoForTest(): FloatBox.Geo = geo

    /** Settings > "Reset the downloads window" and a double tap on its corner: the first place and size. */
    fun resetGeometry() {
        geo = defaults.geo(); l.geometryChanged(null)
        app.kestrel.browser.diag.Health.log("downloads", "window-reset", "")
        place(); box.requestLayout()
    }

    private fun gesture(newGeo: FloatBox.Geo) { geo = newGeo; place() }
    private fun gestureEnd(what: String) {
        l.geometryChanged(FloatBox.store(geo, defaults))
        app.kestrel.browser.diag.Health.log("downloads", what, geo.toString())
    }

    private inner class FloatFrame(c: Context) : FrameLayout(c) {
        private val slop = ViewConfiguration.get(c).scaledTouchSlop
        private var mode = 0                        // 0 none, 1 a finger is down (a long press may still come), 2 carrying, 3 resizing
        private var downX = 0f; private var downY = 0f
        private var start = geo
        private var lastTapAt = 0L
        private val pickRun = Runnable { if (mode == 1) pickUp() }

        private fun pickUp() { mode = 2; start = geo; lit = true; handle.invalidate(); performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); parent?.requestDisallowInterceptTouchEvent(true) }
        private fun onHandle(ev: MotionEvent): Boolean { val h = handle; return ev.x >= h.left && ev.x <= h.right && ev.y >= h.top && ev.y <= h.bottom }
        private fun onHeader(ev: MotionEvent): Boolean = ev.y <= window.top + ctx.dp(44)

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            System.err.println("DBGI act=${ev.actionMasked} x=${ev.x} y=${ev.y} raw=${ev.rawX},${ev.rawY} mode=$mode down=$downX,$downY slop=$slop hdr=${window.top + ctx.dp(44)}")
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY; start = geo
                    if (onHandle(ev)) { mode = 3; lit = true; handle.invalidate(); return true }
                    mode = 1; postDelayed(pickRun, PICK_MS)
                }
                MotionEvent.ACTION_MOVE -> if (mode == 1) {
                    val far = Math.hypot((ev.rawX - downX).toDouble(), (ev.rawY - downY).toDouble()) > slop
                    if (far) { removeCallbacks(pickRun); if (onHeader(ev)) { mode = 2; start = geo; lit = true; handle.invalidate(); return true } else mode = 0 }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { removeCallbacks(pickRun); if (mode == 1) mode = 0 }
            }
            return mode == 2 || mode == 3
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            val dx = (ev.rawX - downX) / density; val dy = (ev.rawY - downY) / density
            System.err.println("DBGT act=${ev.actionMasked} raw=${ev.rawX},${ev.rawY} mode=$mode dx=$dx dy=$dy")
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (mode == 2) gesture(FloatBox.moved(start, dx, dy, areaW(), areaH(), boxH(), limits))
                    else if (mode == 3) gesture(FloatBox.resizedFromBottomLeft(start, dx, dy, areaW(), areaH(), boxH(), limits))
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (mode == 3 && Math.hypot((ev.rawX - downX).toDouble(), (ev.rawY - downY).toDouble()) <= slop && ev.actionMasked == MotionEvent.ACTION_UP) {       // a tap on the corner: two of them put the window back
                        val now = ev.eventTime
                        if (now - lastTapAt < DOUBLE_TAP_MS) { lastTapAt = 0L; resetGeometry() } else lastTapAt = now
                    } else if (mode == 2 || mode == 3) gestureEnd(if (mode == 2) "window-moved" else "window-resized")
                    mode = 0; lit = false; handle.invalidate()
                }
            }
            return mode != 0 || super.onTouchEvent(ev)
        }
    }

    private val listener: () -> Unit = { render() }

    /** Start following the engines (call once the activity is built). What is already in the list is old news: it is not announced. */
    fun attach() {
        if (attached) return
        attached = true
        DownloadController.init(ctx)
        DownloadController.addListener(listener)   // the first render that sees the last run's rows fully read starts the tracker (render)
        render()
    }

    fun detach() {
        if (!attached) return
        attached = false
        DownloadController.removeListener(listener)
        handler.removeCallbacksAndMessages(null)
    }

    /** True while a full-screen screen (the Downloads list, Settings, the tab grid...) covers the browser: nothing is shown meanwhile. */
    fun suppress(on: Boolean) { if (suppressed != on) { suppressed = on; render() } }

    /** The app came back to the front: bring the rows up to date (nothing was drawn while it was away). */
    fun resumed() { render() }

    private fun hideBanner() { handler.removeCallbacks(hideBannerLater); banner.visibility = View.GONE; refreshContainer(); place() }

    private fun refreshContainer() {
        box.visibility = window.visibility                                       // FD1: the frame (and its corner mark) is there only while the window is
        view.visibility = if (banner.visibility == View.VISIBLE || window.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

    private fun render() {
        if (!attached) return
        if (!DownloadController.restored) return                       // DM1: the rows of the last run are still being read: each of them would look like a new download
        if (!tracker.began) tracker.begin(DownloadController.views())  // everything in the list now is old news
        val now = System.currentTimeMillis()
        val u = tracker.update(DownloadController.views(), now)
        val shown = ProcessLife.uiVisible && !suppressed
        if (u.announce.isNotEmpty()) {
            app.kestrel.browser.diag.Health.log("downloads", "banner", "new=${u.announce.size} shown=$shown")   // DM1: the evidence for "it popped up by itself" (no names)
            closedByUser = false // a new download brings the window back
            if (shown) {
                bannerName.text = u.announce.last().name + if (u.announce.size > 1) "  (+${u.announce.size - 1} more)" else ""
                banner.visibility = View.VISIBLE; banner.post { place() }
                handler.removeCallbacks(hideBannerLater); handler.postDelayed(hideBannerLater, BANNER_MS)
            }
        }
        if (!shown) { banner.visibility = View.GONE; window.visibility = View.GONE; refreshContainer(); return }
        val rowsNow = u.rows
        if (rowsNow.isEmpty() || closedByUser) { window.visibility = View.GONE; refreshContainer(); return }
        val visible = rowsNow.takeLast(MAX_ROWS)   // (the list scrolls inside the window: FD1)
        val sig = visible.joinToString("|") { "${it.id}:${it.state}:${it.actions}:${it.name}" } + "/" + rowsNow.size
        if (sig != lastSig) {
            lastSig = sig
            list.removeAllViews(); rows.clear()
            for (v in visible) { val r = MiniRow(v); rows[v.id] = r; list.addView(r.root, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)) }
            val extra = rowsNow.size - visible.size
            more.visibility = if (extra > 0) View.VISIBLE else View.GONE
            if (extra > 0) more.text = "+$extra more  ·  open Downloads"
        }
        for (v in visible) rows[v.id]?.bind(v)
        count.text = if (rowsNow.size > 1) "·  ${rowsNow.size}" else ""
        if (window.visibility != View.VISIBLE) app.kestrel.browser.diag.Health.log("downloads", "window", "rows=${rowsNow.size} states=" + rowsNow.joinToString(",") { it.state.name })   // DM1
        window.visibility = View.VISIBLE
        refreshContainer()
        // a finished or stopped row leaves by itself a few seconds later: look again then, even if the engines stay quiet
        if (tracker.hasLingering()) { handler.removeCallbacks(lingerTick); handler.postDelayed(lingerTick, 1_500) }
    }

    private val lingerTick = Runnable { render() }

    private fun smallButton(icon: Int, description: String, tint: Int, onClick: () -> Unit): ImageButton =
        iconButton(ctx, icon, description, tint, sizeDp = 36, onClick = onClick)

    private inner class MiniRow(first: DownloadView) {
        val id = first.id
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, ctx.dp(2), 0, ctx.dp(4)) }
        private val name = label(ctx, first.name, 12f, pal.fg, bold = true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true }
        private val bar = DownloadBar(ctx, pal, textSp = 11f, cutDp = 5f)
        private val pause = smallButton(R.drawable.ic_pause, "Pause", pal.fg) { onPause() }
        private val close = smallButton(R.drawable.ic_close, "Cancel and delete", pal.danger) { onClose() }
        @Volatile private var cur: DownloadView = first

        init {
            val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            top.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = ctx.dp(4) })
            top.addView(pause); top.addView(close)
            root.addView(top, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(36)))
            root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(BAR_DP)).apply { marginEnd = ctx.dp(4) })
            top.setOnClickListener { l.openManager() }
            bind(first)
        }

        fun bind(v: DownloadView) {
            cur = v
            if (name.text.toString() != v.name) name.text = v.name
            val finished = v.state == DlState.COMPLETED
            bar.visibility = View.VISIBLE
            bar.fraction = if (finished) 1f else v.fraction
            bar.indeterminate = v.state == DlState.RUNNING && v.total <= 0
            bar.tone = when (v.state) { DlState.FAILED -> DownloadBar.Tone.DANGER; DlState.NEEDS_DECISION, DlState.WAITING_NETWORK -> DownloadBar.Tone.WARN; else -> DownloadBar.Tone.NORMAL }
            bar.text = when (v.state) {
                DlState.COMPLETED -> "Done  ·  " + Format.bytes(v.total.takeIf { it >= 0 } ?: v.done)
                DlState.FAILED -> Format.failure(v)
                else -> Format.barText(v, compact = true)
            }
            when {
                Action.PAUSE in v.actions -> { pause.visibility = View.VISIBLE; pause.setImageResource(R.drawable.ic_pause); pause.contentDescription = "Pause" }
                Action.RESUME in v.actions || Action.RETRY in v.actions -> { pause.visibility = View.VISIBLE; pause.setImageResource(R.drawable.ic_play); pause.contentDescription = if (v.state == DlState.FAILED) "Try again" else "Resume" }
                else -> pause.visibility = View.GONE
            }
            close.contentDescription = if (finished) "Close this row" else "Cancel and delete the unfinished file"
            close.setColorFilter(if (finished) pal.muted else pal.danger)
        }

        private fun onPause() {
            val v = DownloadController.view(id) ?: cur
            if (Action.PAUSE in v.actions) DownloadController.pause(id) else DownloadController.resume(ctx, id)
        }

        private fun onClose() {
            val v = DownloadController.view(id) ?: cur
            when {
                v.state == DlState.COMPLETED -> { tracker.dismiss(id); lastSig = ""; render() }
                v.done > 0 && v.state != DlState.FAILED -> l.confirm("Cancel this download?", "The part already downloaded (${Format.bytes(v.done)}) is deleted.", "Cancel download") { DownloadController.cancel(id) }
                v.state == DlState.FAILED || Action.DELETE in v.actions && Action.CANCEL !in v.actions -> DownloadController.remove(id, true)
                else -> DownloadController.cancel(id)
            }
        }
    }

    companion object {
        const val WINDOW_DP = 300
        const val BAR_DP = 22
        const val MAX_ROWS = 12
        const val DEFAULT_LIST_DP = 270
        const val HANDLE_DP = 30
        const val PICK_MS = 400L
        const val DOUBLE_TAP_MS = 350L
        const val BANNER_MS = 3_500L
    }
}
