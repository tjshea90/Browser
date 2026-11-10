package app.kestrel.browser.ui

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import app.kestrel.browser.R
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.browser.web.Tab
import app.kestrel.core.url.UrlParts

interface ToolbarListener {
    fun onBack()
    fun onForward()
    fun onReloadOrStop()
    fun onShields(anchor: View)
    fun onTabs()
    fun onMenu(anchor: View)
    fun onSubmit(text: String)
    fun onUrlFocus(focused: Boolean)
    fun onUrlText(text: String)
}

/** Back / forward / reload, address pill (security icon + editable URL), Shields chip, tab counter, menu. 52dp tall. */
class Toolbar(ctx: Context, private val pal: Pal, private val l: ToolbarListener) : LinearLayout(ctx) {
    private val back = iconButton(ctx, R.drawable.ic_back, "Back", pal.fg) { l.onBack() }
    private val forward = iconButton(ctx, R.drawable.ic_forward, "Forward", pal.fg) { l.onForward() }
    private val reload = iconButton(ctx, R.drawable.ic_refresh, "Reload", pal.fg) { l.onReloadOrStop() }
    private val lock = ImageView(ctx).apply { layoutParams = LayoutParams(ctx.dp(18), ctx.dp(18)); setColorFilter(pal.muted) }
    val urlField = EditText(ctx)
    private val chip: ShieldChip = ShieldChip(ctx, pal) { l.onShields(chip) }
    private val tabs = TabsButton(ctx, pal.fg).apply { layoutParams = LayoutParams(ctx.dp(44), ctx.dp(44)); setOnClickListener { l.onTabs() } }
    private val menu: ImageButton = iconButton(ctx, R.drawable.ic_more, "Menu", pal.fg) { }.also { it.setOnClickListener { _ -> l.onMenu(it) } }
    private val pill = LinearLayout(ctx)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(pal.chromeBg)
        minimumHeight = ctx.dp(52)
        setPadding(ctx.dp(4), 0, ctx.dp(2), 0)

        urlField.apply {
            setSingleLine()
            hint = if (pal.isPrivate) "Private: search or type address" else "Search or type address"
            setHintTextColor(pal.muted)
            setTextColor(pal.fg)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = null
            setPadding(0, 0, 0, 0)
            setSelectAllOnFocus(true)
            isSaveEnabled = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                (if (pal.isPrivate) EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else 0)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || (ev != null && ev.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN)) {
                    l.onSubmit(text.toString()); true
                } else false
            }
            setOnFocusChangeListener { _, focused -> l.onUrlFocus(focused) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { if (hasFocus()) l.onUrlText(s?.toString() ?: "") }
            })
        }
        pill.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(pal.pill, ctx.dpf(20f))
            setPadding(ctx.dp(12), 0, ctx.dp(12), 0)
            addView(lock)
            addView(View(ctx), LayoutParams(ctx.dp(8), 1))
            addView(urlField)
            val lp = LayoutParams(0, ctx.dp(40), 1f)
            lp.marginStart = ctx.dp(2); lp.marginEnd = ctx.dp(2)
            layoutParams = lp
        }
        addView(back); addView(forward); addView(reload)
        addView(pill)
        addView(chip); addView(tabs); addView(menu)
    }

    /** Hide the forward button on narrow windows (split-screen / small phones); the tablet's 564dp width shows it. */
    override fun onMeasure(w: Int, h: Int) {
        forward.visibility = if (MeasureSpec.getSize(w) < context.dp(420)) GONE else VISIBLE
        super.onMeasure(w, MeasureSpec.makeMeasureSpec(context.dp(52), MeasureSpec.EXACTLY))
    }

    fun bind(tab: Tab?, tabCount: Int, canGoBack: Boolean) {
        tabs.count = tabCount
        back.isEnabled = canGoBack
        back.alpha = if (canGoBack) 1f else 0.38f
        forward.isEnabled = tab?.canGoForward == true
        forward.alpha = if (forward.isEnabled) 1f else 0.38f
        val loading = tab?.loading == true
        reload.setImageResource(if (loading) R.drawable.ic_close else R.drawable.ic_refresh)
        reload.contentDescription = if (loading) "Stop loading" else "Reload"
        chip.bind(tab?.shields?.shieldsUp ?: true, tab?.blockedTotal() ?: 0)
        chip.visibility = if (tab == null || tab.isNewTab) GONE else VISIBLE
        if (!urlField.hasFocus()) urlField.setText(display(tab))
        val url = tab?.url ?: ""
        when {
            tab == null || tab.isNewTab -> { lock.setImageResource(R.drawable.ic_search); lock.setColorFilter(pal.muted) }
            tab.error != null && tab.error !is ErrorScreen.Network -> { lock.setImageResource(R.drawable.ic_warning); lock.setColorFilter(pal.danger) }
            url.startsWith("https://") -> { lock.setImageResource(R.drawable.ic_lock); lock.setColorFilter(pal.muted) }
            else -> { lock.setImageResource(R.drawable.ic_warning); lock.setColorFilter(pal.danger) }
        }
        lock.contentDescription = if (url.startsWith("https://")) "Secure connection" else "Connection is not secure"
    }

    /** What the pill shows while not being edited: host + path, no scheme/"www." for https, the full URL when insecure. */
    private fun display(tab: Tab?): String {
        val url = tab?.url ?: return ""
        if (url.isEmpty()) return ""
        val p = UrlParts.parse(url) ?: return url
        val hostShown = if (p.host.startsWith("www.")) p.host.substring(4) else p.host
        val rest = (if (p.path == "/") "" else p.path) + (p.query?.let { "?$it" } ?: "")
        return if (p.scheme == "https") hostShown + rest else "${p.scheme}://${p.host}${p.path}${p.query?.let { "?$it" } ?: ""}"
    }

    fun fullUrlForEditing(tab: Tab?): String = tab?.url ?: ""
}
