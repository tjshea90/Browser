package app.kestrel.browser.web

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * ANS53d (owner message 53: "put a setting for video quality in the player controls ... as many quality options as available, such as resolution, bitrate, and frames per second"): the list a person picks a
 * quality from, drawn over the picture by Kestrel itself (framework views only, no dialog window: a dialog would bring the system bars back over a full-screen film). One dark panel at the top right, one row per
 * choice (the check mark on the one in force), a few words of explanation at the bottom; a tap outside it, or Back, closes it. The rows are made by the caller (Kestrel's own player lists the pictures its stream
 * offers; a page's film lists the pictures the app knows from its playlist).
 */
class QualitySheet(ctx: Context) : FrameLayout(ctx) {
    class Row(val title: String, val detail: String, val selected: Boolean, val enabled: Boolean, val tag: Any?)

    interface Listener {
        fun onPick(row: Row)
        fun onDismiss()
    }

    var listener: Listener? = null
    private val d = resources.displayMetrics.density
    private fun dp(x: Float) = (x * d).toInt()
    private val panel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    private val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    private val titleView = TextView(ctx)
    private val closeView = TextView(ctx)
    private val noteView = TextView(ctx)
    private val scroll = ScrollView(ctx)

    init {
        setBackgroundColor(0x66000000)
        isClickable = true
        visibility = GONE
        panel.background = GradientDrawable().apply { setColor(0xF2161C20.toInt()); cornerRadius = 14f * d }
        panel.setPadding(0, dp(10f), 0, dp(10f))
        titleView.apply { setTextColor(0xFFFFFFFF.toInt()); textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(18f), dp(4f), dp(18f), dp(8f)) }
        noteView.apply { setTextColor(0xFFA6B0B8.toInt()); textSize = 12.5f; setPadding(dp(18f), dp(8f), dp(18f), dp(4f)) }
        scroll.addView(list, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        // ANS54e.2 (owner message 54: "pressed the x button to close the resolution options box, it didn't close"): a box of options has an x, on the title row, 48 dp to touch (Back and a tap outside still close it too)
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(titleView, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        closeView.apply { text = "\u2715"; textSize = 20f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; contentDescription = "Close"; setOnClickListener { dismiss() } }
        head.addView(closeView, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        panel.addView(head, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        panel.addView(scroll, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(noteView, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(panel, LayoutParams(dp(330f), LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { topMargin = dp(70f); rightMargin = dp(14f); leftMargin = dp(14f); bottomMargin = dp(18f) })
    }

    val isOpen: Boolean get() = visibility == VISIBLE

    /** Fill the panel and show it. */
    fun show(title: String, rows: List<Row>, note: String) {
        titleView.text = title
        noteView.text = note
        noteView.visibility = if (note.isEmpty()) GONE else VISIBLE
        list.removeAllViews()
        for (r in rows) list.addView(rowView(r), LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        visibility = VISIBLE
        bringToFront()
    }

    fun dismiss() { if (visibility != VISIBLE) return; visibility = GONE; listener?.onDismiss() }

    private fun rowView(r: Row): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18f), dp(10f), dp(14f), dp(10f)); minimumHeight = dp(52f)
            isEnabled = r.enabled
            if (r.enabled) setOnClickListener { listener?.onPick(r) }
        }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val t = TextView(context).apply { text = r.title; textSize = 16f; setTextColor(if (!r.enabled) 0x66FFFFFF else if (r.selected) ACCENT else 0xFFFFFFFF.toInt()) }
        texts.addView(t)
        if (r.detail.isNotEmpty()) texts.addView(TextView(context).apply { text = r.detail; textSize = 12.5f; setTextColor(if (r.enabled) 0xFFA6B0B8.toInt() else 0x55FFFFFF) })
        row.addView(texts, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (r.selected) row.addView(TextView(context).apply { text = "✓"; textSize = 19f; setTextColor(ACCENT); setPadding(dp(10f), 0, 0, 0) })
        return row
    }

    /** A tap anywhere outside the panel closes the sheet (the dim layer is the whole view); touches never reach the picture or its controls underneath. */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val loc = IntArray(2); panel.getLocationOnScreen(loc)
            val x = e.rawX; val y = e.rawY
            if (x < loc[0] || x > loc[0] + panel.width || y < loc[1] || y > loc[1] + panel.height) dismiss()
        }
        return true
    }

    companion object { private val ACCENT = Color.parseColor("#36E6F2") }
}
