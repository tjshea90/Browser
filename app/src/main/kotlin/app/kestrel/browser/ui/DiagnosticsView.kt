package app.kestrel.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.kestrel.browser.diag.Health

/**
 * Full-screen Diagnostics: device and engine facts, counters and the last events of the freeze watchdog (research/FREEZE.md). Large buttons:
 * "Refresh screen" re-creates the page surface by hand (what switching apps does), Copy/Share put the report on the clipboard or in a message.
 * Nothing leaves the tablet unless the owner shares it.
 */
class DiagnosticsView(ctx: Context, private val pal: Pal, private val facts: () -> List<String>, private val onRefreshScreen: () -> Unit, private val onClose: () -> Unit) : LinearLayout(ctx) {
    private val body = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = Typeface.MONOSPACE
        setTextColor(pal.fg)
        setTextIsSelectable(true)
        setPadding(ctx.dp(14), ctx.dp(10), ctx.dp(14), ctx.dp(24))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(pal.windowBg)
        isClickable = true // swallow touches: the page below must not scroll through this screen
        val head = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(16), ctx.dp(6), ctx.dp(8), ctx.dp(6)); setBackgroundColor(pal.chromeBg) }
        head.addView(label(ctx, "Diagnostics", 20f, pal.fg, bold = true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        head.addView(textButton(ctx, "Close", pal.accent) { onClose() }.apply { minHeight = ctx.dp(52) })
        addView(head, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(60)))

        val row = LinearLayout(ctx).apply { orientation = HORIZONTAL; setPadding(ctx.dp(10), ctx.dp(10), ctx.dp(10), ctx.dp(6)) }
        fun big(text: String, bg: Int, fg: Int, click: () -> Unit) = pillButton(ctx, text, bg, fg, click).apply { minHeight = ctx.dp(54); minimumWidth = 0 }
        val lp = { w: Float -> LayoutParams(0, LayoutParams.WRAP_CONTENT, w).apply { setMargins(ctx.dp(4), 0, ctx.dp(4), 0) } }
        row.addView(big("Refresh screen", pal.accent, pal.accentOn) { onRefreshScreen(); refresh() }, lp(1.7f))
        row.addView(big("Update", pal.pill, pal.fg) { refresh() }, lp(1f))
        row.addView(big("Copy", pal.pill, pal.fg) { copy() }, lp(1f))
        row.addView(big("Share", pal.pill, pal.fg) { share() }, lp(1f))
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val hint = label(ctx, "If the screen freezes while typing, tap Refresh screen, then tell Claude whether it helped. Copy/Share this page when you report it.", 12f, pal.muted)
        hint.setPadding(ctx.dp(16), ctx.dp(2), ctx.dp(16), ctx.dp(6))
        hint.includeFontPadding = true
        addView(hint, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(ScrollView(ctx).apply { addView(body) }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        refresh()
    }

    fun refresh() { body.text = Health.report(context, facts()) }

    private fun copy() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Kestrel diagnostics", Health.report(context, facts(), 400)))
        Toast.makeText(context, "Report copied", Toast.LENGTH_SHORT).show()
    }

    private fun share() {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, Health.report(context, facts(), 400)).putExtra(Intent.EXTRA_SUBJECT, "Kestrel diagnostics")
        try { context.startActivity(Intent.createChooser(send, "Share report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { Toast.makeText(context, "Nothing can share text here", Toast.LENGTH_SHORT).show() }
    }

    override fun onVisibilityChanged(changed: View, visibility: Int) { super.onVisibilityChanged(changed, visibility); if (visibility == VISIBLE) refresh() }
}
