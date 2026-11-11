package app.kestrel.browser.ui

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.browser.R

/**
 * The "Download video" sheet: one big row per thing that can be saved ("1080p", "MP4 - 48.2 MB"), grouped under small headings. Rows are plain data ([Choice]); the activity decides what a tap does
 * and can replace the content at any time (a stream's quality list arrives after its playlist has been read).
 */
class VideoPanel(ctx: Context, private val pal: Pal, private val onPick: (Choice) -> Unit) : LinearLayout(ctx) {
    /** One thing to save. [tag] comes back untouched in the callback. A disabled row is shown but does nothing (a reason is in [detail]). */
    class Choice(val title: String, val detail: String, val tag: Any? = null, val enabled: Boolean = true)
    class Section(val heading: String?, val choices: List<Choice>)

    init {
        orientation = VERTICAL
        setPadding(ctx.dp(16), ctx.dp(2), ctx.dp(16), ctx.dp(16))
    }

    /** [source]: where it is from (the page title); [note]: a quiet line at the bottom (what could not be offered, and why). */
    fun show(source: String?, sections: List<Section>, note: String? = null, busy: String? = null) {
        removeAllViews()
        val ctx = context
        if (!source.isNullOrBlank()) addView(label(ctx, source, 13f, pal.muted).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true; setPadding(ctx.dp(4), 0, ctx.dp(4), ctx.dp(8)) })
        for (s in sections) {
            if (s.choices.isEmpty()) continue
            if (!s.heading.isNullOrBlank()) addView(label(ctx, s.heading.uppercase(), 11f, pal.accent, bold = true).apply { includeFontPadding = true; letterSpacing = 0.08f; setPadding(ctx.dp(4), ctx.dp(8), 0, ctx.dp(4)) })
            for (c in s.choices) addView(row(c), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = ctx.dp(8) })
        }
        if (busy != null) addView(label(ctx, busy, 14f, pal.muted).apply { includeFontPadding = true; setPadding(ctx.dp(4), ctx.dp(8), ctx.dp(4), ctx.dp(8)) })
        if (!note.isNullOrBlank()) addView(label(ctx, note, 13f, pal.muted).apply { includeFontPadding = true; setPadding(ctx.dp(4), ctx.dp(6), ctx.dp(4), 0) })
    }

    private fun row(c: Choice): View {
        val ctx = context
        val r = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ctx.dp(64)
            setPadding(ctx.dp(16), ctx.dp(10), ctx.dp(12), ctx.dp(10))
            background = ripple(pal.fg, ChamferDrawable(pal.pill, ctx.dpf(12f), ChamferDrawable.TL or ChamferDrawable.BR, if (c.enabled) pal.deep else pal.divider, ctx.dpf(1f)), 0f)
            isClickable = c.enabled
            alpha = if (c.enabled) 1f else 0.55f
            if (c.enabled) setOnClickListener { onPick(c) }
        }
        val texts = LinearLayout(ctx).apply { orientation = VERTICAL }
        texts.addView(label(ctx, c.title, 18f, pal.fg, bold = true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        if (c.detail.isNotEmpty()) texts.addView(label(ctx, c.detail, 14f, pal.muted).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        r.addView(texts, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (c.enabled) r.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_download); setColorFilter(pal.accent); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(ctx.dp(28), ctx.dp(28)).apply { marginStart = ctx.dp(8) })
        r.contentDescription = c.title + ", " + c.detail
        return r
    }
}
