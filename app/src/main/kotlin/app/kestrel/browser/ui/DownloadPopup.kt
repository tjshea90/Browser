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
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.browser.ProcessLife
import app.kestrel.browser.R
import app.kestrel.browser.download.DownloadController
import app.kestrel.core.download.Action
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Format
import app.kestrel.core.download.MiniTracker

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
    }

    /** Add this to the activity's root layout (above the page, below the full-screen screens and dialogs). It is empty (zero height) while there is nothing to show. */
    val view = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }

    private val handler = Handler(Looper.getMainLooper())
    private val tracker = MiniTracker()
    private var attached = false
    private var closedByUser = false
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
    private val more = label(ctx, "", 12f, pal.accent, bold = true).apply { gravity = Gravity.CENTER; includeFontPadding = true; visibility = View.GONE; setPadding(0, ctx.dp(8), 0, ctx.dp(4)); setOnClickListener { l.openManager() } }
    private val window = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL; visibility = View.GONE
        setPadding(ctx.dp(10), ctx.dp(4), ctx.dp(6), ctx.dp(8))
        background = ChamferDrawable(pal.card, ctx.dpf(10f), ChamferDrawable.TL or ChamferDrawable.BR, 0x99000000.toInt() or (pal.accent and 0x00FFFFFF), ctx.dpf(1f), gloss = true)
        isClickable = true // the window never lets a tap fall through to the page behind it
    }

    init {
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setOnClickListener { l.openManager() } }
        titleBox.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_download); setColorFilter(pal.accent) }, LinearLayout.LayoutParams(ctx.dp(16), ctx.dp(16)).apply { marginEnd = ctx.dp(6) })
        titleBox.addView(label(ctx, "Downloads", 12f, pal.fg, bold = true).apply { includeFontPadding = true })
        titleBox.addView(count, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = ctx.dp(6) })
        head.addView(titleBox, LinearLayout.LayoutParams(0, ctx.dp(36), 1f))
        head.addView(smallButton(R.drawable.ic_close, "Hide this window (downloads keep going)", pal.muted) { closedByUser = true; render() })
        window.addView(head, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        window.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        window.addView(more, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        view.addView(banner, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(ctx.dp(8), ctx.dp(8), ctx.dp(8), 0) })
        view.addView(window, LinearLayout.LayoutParams(ctx.dp(WINDOW_DP), LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END; setMargins(0, ctx.dp(8), ctx.dp(8), 0) })
    }

    private val listener: () -> Unit = { render() }

    /** Start following the engines (call once the activity is built). What is already in the list is old news: it is not announced. */
    fun attach() {
        if (attached) return
        attached = true
        DownloadController.init(ctx)
        tracker.begin(DownloadController.views())
        DownloadController.addListener(listener)
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

    private fun hideBanner() { handler.removeCallbacks(hideBannerLater); banner.visibility = View.GONE; refreshContainer() }

    private fun refreshContainer() {
        view.visibility = if (banner.visibility == View.VISIBLE || window.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

    private fun render() {
        if (!attached) return
        val now = System.currentTimeMillis()
        val u = tracker.update(DownloadController.views(), now)
        val shown = ProcessLife.uiVisible && !suppressed
        if (u.announce.isNotEmpty()) {
            closedByUser = false // a new download brings the window back
            if (shown) {
                bannerName.text = u.announce.last().name + if (u.announce.size > 1) "  (+${u.announce.size - 1} more)" else ""
                banner.visibility = View.VISIBLE
                handler.removeCallbacks(hideBannerLater); handler.postDelayed(hideBannerLater, BANNER_MS)
            }
        }
        if (!shown) { banner.visibility = View.GONE; window.visibility = View.GONE; refreshContainer(); return }
        val rowsNow = u.rows
        if (rowsNow.isEmpty() || closedByUser) { window.visibility = View.GONE; refreshContainer(); return }
        val visible = rowsNow.takeLast(MAX_ROWS)
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
        const val MAX_ROWS = 4
        const val BANNER_MS = 3_500L
    }
}
