package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.kestrel.browser.R
import app.kestrel.browser.download.DownloadController
import app.kestrel.core.download.Action
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Format

/**
 * The Downloads screen (owner request: a real download manager, never the file manager). One card per download with a progress bar, one status line and big
 * buttons: Pause / Resume / Cancel while it runs, Open / Share / Delete when it is done. It follows the engine live (about 4 updates a second) and keeps no state of its own.
 */
class DownloadsScreen(ctx: Context, private var pal: Pal, private val l: Listener) : LinearLayout(ctx) {
    interface Listener {
        fun open(v: DownloadView)
        fun share(v: DownloadView)
        fun confirm(title: String, message: String, yes: String, then: () -> Unit)
        fun close()
    }

    private val title = label(ctx, "Downloads", 20f, pal.fg, bold = true)
    private val clear = textButton(ctx, "Clear finished", pal.muted) { clearFinished() }
    private val scroller = ScrollView(ctx)
    private val list = LinearLayout(ctx)
    private val empty = LinearLayout(ctx)
    private var signature = NONE
    private val rows = HashMap<Long, Row>()
    private val listener: () -> Unit = { render() }

    init {
        orientation = VERTICAL
        setBackgroundColor(pal.windowBg)
        isClickable = true
        val header = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(pal.chromeBg); setPadding(ctx.dp(4), 0, ctx.dp(8), 0) }
        header.addView(iconButton(ctx, R.drawable.ic_back, "Back", pal.fg, 52) { l.close() })
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ctx.dp(8) })
        header.addView(clear)
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(56)))
        list.orientation = VERTICAL
        list.setPadding(ctx.dp(12), ctx.dp(4), ctx.dp(12), ctx.dp(24))
        scroller.addView(list, android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        scroller.isVerticalScrollBarEnabled = false
        scroller.overScrollMode = OVER_SCROLL_NEVER
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        empty.orientation = VERTICAL
        empty.gravity = Gravity.CENTER_HORIZONTAL
        empty.setPadding(ctx.dp(32), ctx.dp(96), ctx.dp(32), 0)
        empty.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_download); setColorFilter(pal.accent) }, LayoutParams(ctx.dp(56), ctx.dp(56)))
        empty.addView(label(ctx, "No downloads yet", 20f, pal.fg, bold = true).apply { setPadding(0, ctx.dp(16), 0, ctx.dp(6)); includeFontPadding = true })
        empty.addView(label(ctx, "Files you download appear here. Big ones can be paused and picked up again, even after the app was closed.", 14f, pal.muted).apply { gravity = Gravity.CENTER; includeFontPadding = true })
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); DownloadController.init(context); DownloadController.addListener(listener); signature = NONE; render() }
    override fun onDetachedFromWindow() { DownloadController.removeListener(listener); super.onDetachedFromWindow() }

    fun refresh() { signature = NONE; render() }

    private fun clearFinished() {
        l.confirm("Clear the finished list?", "The downloaded files stay on your tablet; only this list is cleared.", "Clear") { DownloadController.clearFinished() }
    }

    private fun render() {
        val views = DownloadController.views()
        val running = views.filter { it.state != DlState.COMPLETED }.sortedByDescending { it.created }
        val done = views.filter { it.state == DlState.COMPLETED }.sortedByDescending { it.updated }
        clear.visibility = if (views.any { it.state == DlState.COMPLETED || it.state == DlState.FAILED }) VISIBLE else GONE
        val sig = (running + done).joinToString("|") { "${it.id}:${it.state}:${it.reason}:${it.actions}:${it.note}" }
        if (sig == signature) { for (v in running) rows[v.id]?.bind(v); return } // same cards: just move the bars of the ones that are not finished (a finished row never changes)
        signature = sig
        val keep = scroller.scrollY
        list.removeAllViews(); rows.clear()
        if (views.isEmpty()) { list.addView(empty); return }
        if (running.isNotEmpty()) { list.addView(section("In progress")); for (v in running) addRow(v) }
        if (done.isNotEmpty()) { list.addView(section("Finished")); for (v in done) addRow(v) }
        scroller.post { scroller.scrollTo(0, keep) }
    }

    private fun section(text: String): View = label(context, text.uppercase(), 12f, pal.muted, bold = true).apply { letterSpacing = 0.14f; setPadding(context.dp(4), context.dp(18), 0, context.dp(6)); includeFontPadding = true }

    private fun addRow(v: DownloadView) {
        val r = Row(context, v)
        rows[v.id] = r
        list.addView(r.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(10) })
    }

    private companion object { const val NONE = "\u0000" } // "nothing drawn yet": differs from the signature of an EMPTY list

    private inner class Row(ctx: Context, v: DownloadView) {
        val root = LinearLayout(ctx)
        private val name = label(ctx, v.name, 16f, pal.fg, bold = true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true }
        private val sub = label(ctx, "", 12f, pal.muted).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true }
        private val status = label(ctx, "", 14f, pal.fg).apply { includeFontPadding = true }
        /** One plain sentence about how the file came to be what it is (kept as a .ts and why; a part left out of an MP4). */
        private val note = label(ctx, "", 13f, pal.warn).apply { includeFontPadding = true; visibility = GONE }
        private val bar = Bar(ctx, pal)

        init {
            root.orientation = LinearLayout.VERTICAL
            root.setPadding(ctx.dp(14), ctx.dp(14), ctx.dp(14), ctx.dp(12))
            root.background = ChamferDrawable(pal.card, ctx.dpf(14f), ChamferDrawable.TL or ChamferDrawable.BR, pal.divider, ctx.dpf(1f), gloss = true)
            val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            top.addView(ExtTile(ctx, pal, extOf(v.name, v.mime)), LinearLayout.LayoutParams(ctx.dp(48), ctx.dp(48)).apply { marginEnd = ctx.dp(12) })
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            col.addView(name); col.addView(sub)
            top.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            root.addView(top)
            val showBar = v.state != DlState.COMPLETED && v.state != DlState.CANCELED
            if (showBar) root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(8)).apply { topMargin = ctx.dp(12) })
            root.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(8) })
            root.addView(note, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(4) })
            if (v.actions.isNotEmpty()) {
                val btns = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                for ((i, a) in v.actions.withIndex()) btns.addView(button(ctx, a, i == 0, v), LinearLayout.LayoutParams(0, ctx.dp(if (i == 0) 56 else 52), 1f).apply { if (i > 0) marginStart = ctx.dp(8) })
                root.addView(btns, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(12) })
            }
            bind(v)
        }

        fun bind(v: DownloadView) {
            bar.fraction = v.fraction
            bar.indeterminate = v.state == DlState.RUNNING && v.total <= 0
            val st = Format.status(v)
            if (status.text.toString() != st) status.text = st // setText rebuilds the text layout even for the same words
            status.setTextColor(when (v.state) { DlState.FAILED -> pal.danger; DlState.NEEDS_DECISION, DlState.WAITING_NETWORK -> pal.warn; DlState.COMPLETED -> pal.ok; else -> pal.fg })
            val line = listOfNotNull(v.host?.removePrefix("www."), if (v.total >= 0) (if (v.estimated) "about " else "") + Format.bytes(v.total) else null, if (v.priv) "private" else null).joinToString("  ·  ")
            if (sub.text.toString() != line) sub.text = line
            val n = v.note.orEmpty()
            if (note.text.toString() != n) note.text = n
            note.visibility = if (n.isEmpty()) GONE else VISIBLE
        }
    }

    private fun button(ctx: Context, a: Action, primary: Boolean, v: DownloadView): TextView {
        val text = when (a) { Action.PAUSE -> "Pause"; Action.RESUME -> "Resume"; Action.CANCEL -> "Cancel"; Action.RETRY -> "Retry"; Action.RESTART -> "Restart"; Action.OPEN -> "Open"; Action.SHARE -> "Share"; Action.DELETE -> "Delete"; Action.MAKE_MP4 -> "Make MP4"; Action.DOWNLOAD_HERE -> "Download here" }
        val click: () -> Unit = {
            when (a) {
                Action.PAUSE -> DownloadController.pause(v.id)
                Action.RESUME -> DownloadController.resume(ctx, v.id)
                Action.RETRY -> DownloadController.resume(ctx, v.id)
                Action.RESTART -> DownloadController.restart(ctx, v.id)
                Action.CANCEL -> { val cur = DownloadController.view(v.id) ?: v; if (cur.done > 0) l.confirm("Cancel this download?", "The part already downloaded (${Format.bytes(cur.done)}) is deleted.", "Cancel download") { DownloadController.cancel(v.id) } else DownloadController.cancel(v.id) }
                Action.OPEN -> l.open(v)
                Action.SHARE -> l.share(v)
                Action.MAKE_MP4 -> DownloadController.makeMp4(ctx, v.id)
                Action.DOWNLOAD_HERE -> DownloadController.restart(ctx, v.id)
                Action.DELETE -> { val cur = DownloadController.view(v.id) ?: v; l.confirm("Delete this download?", if (cur.state == DlState.COMPLETED) "The file ${cur.name} is deleted from your tablet." else "The unfinished file is deleted.", "Delete") { DownloadController.remove(v.id, true) } }
            }
        }
        val danger = a == Action.DELETE || a == Action.CANCEL
        return if (primary && !danger) pillButton(ctx, text, pal.accent, pal.accentOn, click)
        else outlineButton(ctx, text, pal.pill, pal.divider, if (danger) pal.danger else pal.fg, click)
    }

    private fun extOf(name: String, mime: String?): String {
        val e = name.substringAfterLast('.', "")
        return (if (e.isNotEmpty() && e.length <= 4) e else mime?.substringAfter('/')?.take(4) ?: "FILE").uppercase()
    }

    /** A chamfered tile with the file extension: tells a PDF from a ZIP at a glance. */
    private class ExtTile(ctx: Context, private val pal: Pal, private val ext: String) : View(ctx) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.pill }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.accent; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        private val box = RectF()
        private val path = Path()
        override fun onDraw(c: Canvas) {
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            ChamferDrawable.path(box, height * 0.28f, ChamferDrawable.TL or ChamferDrawable.BR, path)
            c.drawPath(path, fill)
            text.textSize = height * (if (ext.length > 3) 0.26f else 0.3f)
            c.drawText(ext, width / 2f, height / 2f - (text.descent() + text.ascent()) / 2f, text)
        }
    }

    /** Flat chamfered progress bar; unknown size shows a solid stripe (no animation anywhere). */
    private class Bar(ctx: Context, private val pal: Pal) : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val box = RectF()
        private val path = Path()
        var fraction = 0f
            set(v) { val c = v.coerceIn(0f, 1f); if (c != field) { field = c; invalidate() } }
        var indeterminate = false
            set(v) { if (v != field) { field = v; invalidate() } }

        override fun onDraw(c: Canvas) {
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            ChamferDrawable.path(box, context.dpf(3f), ChamferDrawable.TL or ChamferDrawable.BR, path)
            paint.color = pal.pill; c.drawPath(path, paint)
            c.save(); c.clipPath(path)
            paint.color = pal.accent
            if (indeterminate) c.drawRect(width * 0.25f, 0f, width * 0.75f, height.toFloat(), paint) else c.drawRect(0f, 0f, width * fraction, height.toFloat(), paint)
            c.restore()
        }
    }
}
