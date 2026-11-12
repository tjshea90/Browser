package app.kestrel.browser.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.core.media.MediaNames

/**
 * The "Save video" sheet (owner request AB2): the NAME the file will get (the one the source gave it; tap it to rename), one row per format with its resolution, type, size and length
 * (the best one already chosen), and one big Download button. Files that are small or look like trackers are not rows of the list: they sit under "Other files on this page", closed.
 * Rows are plain data ([Choice]); the activity decides what Download does and may replace the content at any time (a stream's quality list arrives after its playlist is read): the
 * choice, a name the owner typed and whether "Other files" is open all survive that.
 */
class VideoPanel(ctx: Context, private val pal: Pal, private val listener: Listener) : LinearLayout(ctx) {
    interface Listener {
        /** Download [choice] under [stem] (the name without its extension; already cleaned for a file system). */
        fun download(choice: Choice, stem: String)
    }

    /** One thing to save. [id] stays the same when the sheet is redrawn; [tag] comes back untouched. A disabled row is shown but cannot be chosen (the reason is in [detail]). */
    class Choice(val id: String, val title: String, val detail: String, val stem: String, val ext: String, val tag: Any? = null, val enabled: Boolean = true, val badge: String? = null)

    class Model(
        val source: String?,
        val rows: List<Choice>,
        val other: List<Choice> = emptyList(),
        val note: String? = null,
        val busy: String? = null,
        /** Small text buttons at the bottom ("Copy video address"). */
        val extras: List<Pair<String, () -> Unit>> = emptyList(),
        /** The size to show on the Download button for a row (bytes, or -1). */
        val sizeText: (Choice) -> String = { "" },
    )

    private var model: Model? = null
    private var selectedId: String? = null
    private var typedStem: String? = null          // what the owner typed; null = follow the chosen row's own name
    private var otherOpen = false

    init {
        orientation = VERTICAL
        setPadding(ctx.dp(16), ctx.dp(2), ctx.dp(16), ctx.dp(16))
    }

    fun show(m: Model) {
        model = m
        // keep the choice across a redraw when that row still exists and can be chosen; else the first row that can
        val all = m.rows + m.other
        if (selectedId == null || all.none { it.id == selectedId && it.enabled }) selectedId = m.rows.firstOrNull { it.enabled }?.id ?: m.other.firstOrNull { it.enabled }?.id
        render()
    }

    private val chosen: Choice? get() = model?.let { m -> (m.rows + m.other).firstOrNull { it.id == selectedId } }
    private fun currentStem(): String = typedStem?.takeIf { it.isNotBlank() } ?: chosen?.stem ?: "video"

    private fun render() {
        val m = model ?: return
        removeAllViews()
        val ctx = context
        if (!m.source.isNullOrBlank()) addView(label(ctx, m.source, 13f, pal.muted).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true; setPadding(ctx.dp(4), 0, ctx.dp(4), ctx.dp(10)) })

        // 1. the name: shown whole, one tap to change it
        addView(heading("NAME"))
        addView(nameCard(), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = ctx.dp(6) })

        // 2. the formats
        if (m.rows.isNotEmpty() || m.busy != null) addView(heading(if (m.rows.size > 1) "CHOOSE A FORMAT" else "FORMAT"))
        for (c in m.rows) addView(choiceRow(c, small = false), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = ctx.dp(8) })
        if (m.busy != null) addView(label(ctx, m.busy, 14f, pal.muted).apply { includeFontPadding = true; setPadding(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(8)) })

        // 3. the one big button
        val sel = chosen
        val size = sel?.let { m.sizeText(it) }.orEmpty()
        val go = pillButton(ctx, if (size.isEmpty()) "Download" else "Download  ·  $size", pal.accent, pal.accentOn) { chosen?.let { listener.download(it, MediaNames.clean(currentStem())) } }
        go.isEnabled = sel != null; go.alpha = if (sel != null) 1f else 0.5f
        addView(go, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(54)).apply { topMargin = ctx.dp(6); bottomMargin = ctx.dp(6) })

        // 4. the files that are probably not the video, closed
        if (m.other.isNotEmpty()) {
            addView(label(ctx, (if (otherOpen) "▾  " else "▸  ") + "Other files on this page (${m.other.size})", 14f, pal.muted, bold = true).apply {
                includeFontPadding = true; minimumHeight = ctx.dp(44); gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(4))
                background = ripple(pal.fg, null, 0f); isClickable = true
                setOnClickListener { otherOpen = !otherOpen; render() }
                contentDescription = "Other files on this page, ${m.other.size}, " + if (otherOpen) "open" else "closed"
            })
            if (otherOpen) {
                addView(label(ctx, "Small or odd files the page loaded as well: ads, previews, trackers. Probably not what you pressed.", 12f, pal.muted).apply { includeFontPadding = true; setPadding(ctx.dp(4), 0, ctx.dp(4), ctx.dp(8)) })
                for (c in m.other) addView(choiceRow(c, small = true), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = ctx.dp(6) })
            }
        }
        if (!m.note.isNullOrBlank()) addView(label(ctx, m.note, 13f, pal.muted).apply { includeFontPadding = true; setPadding(ctx.dp(4), ctx.dp(6), ctx.dp(4), 0) })
        for ((text, act) in m.extras) addView(label(ctx, text, 14f, pal.accent, bold = true).apply {
            includeFontPadding = true; minimumHeight = ctx.dp(44); gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(4), ctx.dp(4), ctx.dp(4), ctx.dp(4))
            background = ripple(pal.fg, null, 0f); isClickable = true; setOnClickListener { act() }
        })
    }

    private fun heading(text: String): TextView = label(context, text, 11f, pal.accent, bold = true).apply { includeFontPadding = true; letterSpacing = 0.08f; setPadding(context.dp(4), context.dp(6), 0, context.dp(6)) }

    private fun nameCard(): View {
        val ctx = context
        val ext = chosen?.ext.orEmpty()
        val card = LinearLayout(ctx).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = ctx.dp(56)
            setPadding(ctx.dp(16), ctx.dp(8), ctx.dp(8), ctx.dp(8))
            background = ripple(pal.fg, ChamferDrawable(pal.pill, ctx.dpf(12f), ChamferDrawable.TL or ChamferDrawable.BR, pal.deep, ctx.dpf(1f)), 0f)
            isClickable = true
            setOnClickListener { renameDialog() }
        }
        val text = label(ctx, currentStem() + if (ext.isEmpty()) "" else ".$ext", 16f, pal.fg, bold = true).apply { maxLines = 3; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true }
        card.addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        card.addView(label(ctx, "RENAME", 12f, pal.accent, bold = true).apply { includeFontPadding = true; setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(10), ctx.dp(10)) })
        card.contentDescription = "File name " + text.text + ". Tap to rename."
        return card
    }

    /** The name is edited in a dialog, not in the sheet: a keyboard opening inside the sheet would resize the page behind it. */
    private fun renameDialog() {
        val ctx = context
        val input = EditText(ctx).apply {
            setText(currentStem()); setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS; imeOptions = EditorInfo.IME_ACTION_DONE; selectAll()
            setPadding(ctx.dp(20), ctx.dp(12), ctx.dp(20), ctx.dp(12))
        }
        val ext = chosen?.ext.orEmpty()
        AlertDialog.Builder(ctx).setTitle("File name").setMessage(if (ext.isEmpty()) null else "It is saved as .$ext").setView(input)
            .setPositiveButton("OK") { _, _ ->
                val v = input.text.toString().trim()
                typedStem = if (v.isEmpty() || v == chosen?.stem) null else v
                render()
            }
            .setNeutralButton("Use the original") { _, _ -> typedStem = null; render() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun choiceRow(c: Choice, small: Boolean): View {
        val ctx = context
        val on = c.id == selectedId
        val r = LinearLayout(ctx).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ctx.dp(if (small) 52 else 64)
            setPadding(ctx.dp(14), ctx.dp(8), ctx.dp(12), ctx.dp(8))
            background = ripple(pal.fg, ChamferDrawable(pal.pill, ctx.dpf(12f), ChamferDrawable.TL or ChamferDrawable.BR, if (on) pal.accent else if (c.enabled) pal.deep else pal.divider, ctx.dpf(if (on) 2f else 1f)), 0f)
            isClickable = c.enabled
            alpha = if (c.enabled) 1f else 0.55f
            if (c.enabled) setOnClickListener {
                selectedId = c.id
                render()
            }
        }
        if (c.enabled) r.addView(Radio(ctx, on), LayoutParams(ctx.dp(24), ctx.dp(24)).apply { marginEnd = ctx.dp(14) })
        val texts = LinearLayout(ctx).apply { orientation = VERTICAL }
        val head = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(label(ctx, c.title, if (small) 14f else 16f, pal.fg, bold = true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        if (c.badge != null) head.addView(label(ctx, c.badge, 10f, pal.accentOn, bold = true).apply {
            includeFontPadding = true; letterSpacing = 0.06f; setPadding(ctx.dp(7), ctx.dp(2), ctx.dp(7), ctx.dp(2)); background = chamferSurface(ctx, pal.accent, 4f, pressable = false)
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = ctx.dp(8) })
        texts.addView(head)
        if (c.detail.isNotEmpty()) texts.addView(label(ctx, c.detail, if (small) 12f else 14f, pal.muted).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        r.addView(texts, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        r.contentDescription = c.title + ", " + c.detail + if (c.enabled) (if (on) ", chosen" else ", tap to choose") else ""
        return r
    }

    /** A round radio mark, drawn once: an accent ring, filled when chosen. */
    private inner class Radio(ctx: Context, private val on: Boolean) : View(ctx) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        override fun onDraw(c: Canvas) {
            val cx = width / 2f; val cy = height / 2f; val r = minOf(width, height) / 2f - context.dpf(2f)
            p.style = Paint.Style.STROKE; p.strokeWidth = context.dpf(2f); p.color = if (on) pal.accent else pal.muted
            c.drawCircle(cx, cy, r, p)
            if (on) { p.style = Paint.Style.FILL; p.color = pal.accent; c.drawCircle(cx, cy, r - context.dpf(4f), p) }
        }
    }
}
