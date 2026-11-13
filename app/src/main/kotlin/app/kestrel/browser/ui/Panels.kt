package app.kestrel.browser.ui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import app.kestrel.browser.R
import app.kestrel.browser.data.Entry
import app.kestrel.browser.web.Tab

/** Full-screen list of open tabs (title + host only: no thumbnails, which would cost RAM on a 128 MB-class device). */
class TabSwitcher(ctx: Context, private val pal: Pal, private val l: Listener) : LinearLayout(ctx) {
    interface Listener {
        fun select(tab: Tab)
        fun close(tab: Tab)
        fun newTab()
        fun dismiss()
    }

    private val title = label(ctx, "Tabs", 20f, pal.fg, bold = true)
    private val list = ListView(ctx)
    private var tabs: List<Tab> = emptyList()
    private var selectedId = 0L

    init {
        orientation = VERTICAL
        setBackgroundColor(pal.windowBg)
        isClickable = true // swallow touches so they never reach the page underneath
        val header = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(pal.chromeBg)
            setPadding(ctx.dp(16), 0, ctx.dp(8), 0)
        }
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(pillButton(ctx, "New tab", pal.accent, pal.accentOn) { l.newTab() }.apply { minimumWidth = ctx.dp(80); minHeight = ctx.dp(40); setPadding(ctx.dp(16), ctx.dp(8), ctx.dp(16), ctx.dp(8)) })
        header.addView(iconButton(ctx, R.drawable.ic_close, "Close tab list", pal.fg) { l.dismiss() })
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(56)))
        list.divider = ColorDrawable(pal.divider)
        list.dividerHeight = 1
        list.adapter = object : BaseAdapter() {
            override fun getCount() = tabs.size
            override fun getItem(i: Int) = tabs[i]
            override fun getItemId(i: Int) = tabs[i].id
            override fun getView(i: Int, convert: View?, parent: ViewGroup): View = row(tabs[i])
        }
        addView(list, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun bind(tabs: List<Tab>, selected: Tab?) {
        this.tabs = tabs.toList()
        selectedId = selected?.id ?: 0L
        title.text = if (tabs.size == 1) "1 tab" else "${tabs.size} tabs"
        (list.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private fun row(tab: Tab): View {
        val ctx = context
        val r = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(16), ctx.dp(6), ctx.dp(4), ctx.dp(6))
            background = ripple(pal.fg, if (tab.id == selectedId) roundedBg(pal.pill, 0f) else null, 0f)
            isClickable = true
            setOnClickListener { l.select(tab) }
        }
        val host = if (tab.homeOverPage) "" else app.kestrel.core.url.UrlParts.hostOf(tab.url) ?: ""
        r.addView(LetterAvatar(ctx, host.ifEmpty { if (tab.homeOverPage) "home" else "new" }, pal), LayoutParams(ctx.dp(36), ctx.dp(36)))
        val col = LinearLayout(ctx).apply { orientation = VERTICAL; setPadding(ctx.dp(14), 0, ctx.dp(8), 0) }
        col.addView(label(ctx, tab.cardTitle, 15f, pal.fg, bold = tab.id == selectedId).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        col.addView(label(ctx, if (tab.isNewTab) "New tab" else if (tab.homeOverPage) "Home screen" else host.removePrefix("www."), 12f, pal.muted).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        r.addView(col, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        r.addView(iconButton(ctx, R.drawable.ic_close, "Close tab", pal.muted) { l.close(tab) })
        return r
    }
}

/** History / bookmarks screen. */
class ListPanel(ctx: Context, private val pal: Pal, private val l: Listener) : LinearLayout(ctx) {
    interface Listener {
        fun open(url: String)
        fun delete(url: String)
        fun clearAll()
        fun close()
    }

    private val title = label(ctx, "", 20f, pal.fg, bold = true)
    private val clear = textButton(ctx, "Clear all", pal.danger) { l.clearAll() }
    private val list = ListView(ctx)
    private val empty = label(ctx, "", 15f, pal.muted).apply { gravity = Gravity.CENTER; setPadding(ctx.dp(24), ctx.dp(48), ctx.dp(24), 0); includeFontPadding = true }
    private var items: List<Entry> = emptyList()

    init {
        orientation = VERTICAL
        setBackgroundColor(pal.windowBg)
        isClickable = true
        val header = LinearLayout(ctx).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(pal.chromeBg); setPadding(ctx.dp(4), 0, ctx.dp(8), 0) }
        header.addView(iconButton(ctx, R.drawable.ic_back, "Back", pal.fg) { l.close() })
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ctx.dp(8) })
        header.addView(clear)
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, ctx.dp(56)))
        list.divider = ColorDrawable(pal.divider)
        list.dividerHeight = 1
        list.adapter = object : BaseAdapter() {
            override fun getCount() = items.size
            override fun getItem(i: Int) = items[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, convert: View?, parent: ViewGroup): View = row(items[i])
        }
        addView(empty, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(list, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun bind(titleText: String, entries: List<Entry>, canClearAll: Boolean, emptyText: String) {
        title.text = titleText
        clear.visibility = if (canClearAll && entries.isNotEmpty()) VISIBLE else GONE
        items = entries
        empty.text = emptyText
        empty.visibility = if (entries.isEmpty()) VISIBLE else GONE
        list.visibility = if (entries.isEmpty()) GONE else VISIBLE
        (list.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private fun row(e: Entry): View {
        val ctx = context
        val r = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(16), ctx.dp(6), ctx.dp(4), ctx.dp(6))
            background = ripple(pal.fg, null, 0f)
            isClickable = true
            setOnClickListener { l.open(e.url) }
        }
        val col = LinearLayout(ctx).apply { orientation = VERTICAL }
        col.addView(label(ctx, e.title.ifBlank { e.url }, 15f, pal.fg).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        col.addView(label(ctx, e.url, 12f, pal.muted).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
        r.addView(col, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        r.addView(iconButton(ctx, R.drawable.ic_delete, "Delete", pal.muted) { l.delete(e.url) })
        return r
    }
}

/** Find-in-page bar. */
class FindBar(ctx: Context, private val pal: Pal, private val l: Listener) : LinearLayout(ctx) {
    interface Listener {
        fun query(q: String)
        fun next(forward: Boolean)
        fun close()
    }

    val input = EditText(ctx)
    private val count = label(ctx, "", 13f, pal.muted)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(pal.chromeBg)
        setPadding(ctx.dp(12), 0, ctx.dp(4), 0)
        elevation = ctx.dpf(4f)
        input.apply {
            setSingleLine()
            hint = "Find in page"
            setHintTextColor(pal.muted)
            setTextColor(pal.fg)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = null
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            isSaveEnabled = false
            setOnEditorActionListener { _, _, _ -> l.next(true); true }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { l.query(s?.toString() ?: "") }
            })
        }
        addView(input, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(count)
        addView(iconButton(ctx, R.drawable.ic_forward, "Previous match", pal.fg) { l.next(false) }.apply { rotation = -90f })
        addView(iconButton(ctx, R.drawable.ic_forward, "Next match", pal.fg) { l.next(true) }.apply { rotation = 90f })
        addView(iconButton(ctx, R.drawable.ic_close, "Close find", pal.fg) { l.close() })
    }

    fun setResult(active: Int, total: Int) { count.text = if (total == 0) (if (input.text.isNullOrEmpty()) "" else "0/0") else "${active + 1}/$total"; count.setPadding(0, 0, context.dp(6), 0) }
}

/** Omnibox dropdown: 56 dp rows (icon, title, address). In the bottom dock the best match sits nearest the address bar, so that list is reversed. */
class SuggestionList(ctx: Context, private var pal: Pal, private val reverse: Boolean = false, private val onPick: (String) -> Unit) : ListView(ctx) {
    private class Row(val primary: String, val secondary: String, val value: String, val first: Boolean)

    private var rows: List<Row> = emptyList()
    private val rowPx = ctx.dp(56)

    init {
        selector = ColorDrawable(0)
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        isFocusable = false
        adapter = object : BaseAdapter() {
            override fun getCount() = rows.size
            override fun getItem(i: Int) = rows[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, convert: View?, parent: ViewGroup): View {
                val r = rows[i]
                val v = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = rowPx
                    setPadding(ctx.dp(16), ctx.dp(6), ctx.dp(16), ctx.dp(6))
                    background = ripple(pal.fg, null, 0f)
                    layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, rowPx)
                }
                v.addView(ImageView(ctx).apply {
                    setImageResource(if (r.first) R.drawable.ic_search else R.drawable.ic_history)
                    setColorFilter(if (r.first) pal.accent else pal.muted)
                }, LinearLayout.LayoutParams(ctx.dp(22), ctx.dp(22)).apply { marginEnd = ctx.dp(14) })
                val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
                col.addView(label(ctx, r.primary, 16f, pal.fg).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
                if (r.secondary.isNotEmpty()) col.addView(label(ctx, r.secondary, 12f, pal.muted).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; includeFontPadding = true })
                v.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                return v
            }
        }
        setOnItemClickListener { _, _, i, _ -> onPick(rows[i].value) }
        setPal(pal)
    }

    fun setPal(p: Pal) {
        pal = p
        setBackgroundColor(p.card)
        divider = ColorDrawable(p.divider)
        dividerHeight = 1
        (adapter as BaseAdapter).notifyDataSetChanged()
    }

    fun bind(typed: String, entries: List<Entry>) {
        val out = ArrayList<Row>()
        out.add(Row(typed, "Search or go to address", typed, true))
        for (e in entries) out.add(Row(e.title.ifBlank { e.url }, e.url, e.url, false))
        rows = if (reverse) out.reversed() else out
        (adapter as BaseAdapter).notifyDataSetChanged()
        if (reverse) setSelection(rows.size - 1)
    }

    fun contentHeightPx(): Int = rows.size * rowPx + maxOf(0, rows.size - 1) * dividerHeight
}

class MenuItemSpec(val id: Int, val text: String, val checked: Boolean? = null, val icon: Int = 0, val accent: Boolean = false)

/** Light popup menu anchored under a button (framework PopupWindow: no AppCompat). */
object MenuPopup {
    fun show(ctx: Context, pal: Pal, anchor: View, items: List<MenuItemSpec>, onPick: (Int) -> Unit): PopupWindow {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, ctx.dp(6), 0, ctx.dp(6)) }
        val width = ctx.dp(248)
        val popup = PopupWindow(ScrollView(ctx).apply { addView(col) }, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.setBackgroundDrawable(roundedBg(pal.card, ctx.dpf(12f), pal.divider, ctx.dp(1)))
        popup.elevation = ctx.dpf(12f)
        popup.isOutsideTouchable = true
        popup.animationStyle = 0   // no fade: everything must feel instant
        for (it in items) {
            val prefix = when (it.checked) { true -> "✓  "; false -> "    "; null -> "" }
            col.addView(label(ctx, prefix + it.text, 15f, pal.fg).apply {
                gravity = Gravity.CENTER_VERTICAL
                minHeight = ctx.dp(46)
                setPadding(ctx.dp(18), ctx.dp(12), ctx.dp(18), ctx.dp(12))
                includeFontPadding = true
                background = ripple(pal.fg, null, 0f)
                isClickable = true
                setOnClickListener { _ -> popup.dismiss(); onPick(it.id) }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        popup.showAsDropDown(anchor, -(width - anchor.width) , 0)
        return popup
    }
}
