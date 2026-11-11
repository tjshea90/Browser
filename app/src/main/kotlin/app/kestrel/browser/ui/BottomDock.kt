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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import app.kestrel.browser.R
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.browser.web.Tab
import app.kestrel.core.url.UrlParts

/** What the address field and the Home field report. */
interface AddressListener {
    fun onSubmit(text: String)
    fun onUrlFocus(focused: Boolean)
    fun onUrlText(text: String)
}

interface DockListener : AddressListener {
    fun onBack()
    fun onForward()
    fun onHome()
    fun onReloadOrStop()
    fun onShields(anchor: View)
    fun onNewTab()
    fun onTabs()
    fun onMenu(anchor: View)
}

/** What the address field shows while it is not being edited: host + path, no scheme / "www." for https, the full URL when insecure. */
fun prettyUrl(url: String): String {
    if (url.isEmpty()) return ""
    val p = UrlParts.parse(url) ?: return url
    // A non-default port is part of the site's identity (https://bank.example:8443 is not https://bank.example): always show it.
    val port = if (p.port > 0 && !(p.scheme == "https" && p.port == 443) && !(p.scheme == "http" && p.port == 80)) ":${p.port}" else ""
    val hostShown = (if (p.host.startsWith("www.")) p.host.substring(4) else p.host) + port
    val rest = (if (p.path == "/") "" else p.path) + (p.query?.let { "?$it" } ?: "")
    return if (p.scheme == "https") hostShown + rest else "${p.scheme}://${p.host}$port${p.path}${p.query?.let { "?$it" } ?: ""}"
}

/**
 * The whole navigation bar, at the BOTTOM of the screen (owner request): in portrait two 56 dp rows - the address row on top, the six big buttons
 * (back, forward, home, new tab, tabs, menu) below, each about 94 dp wide - and one row on a wide window. While the address is being typed the activity
 * lifts the dock so the address row sits right above the keyboard (the button row stays behind the keyboard); the page is never resized.
 * On the Home screen the address row is hidden: Home has its own big field.
 */
class BottomDock(ctx: Context, private var pal: Pal, private val l: DockListener) : LinearLayout(ctx) {
    private val hairline = View(ctx)
    private val urlRow = LinearLayout(ctx)
    private val actionRow = LinearLayout(ctx)
    private val pill = LinearLayout(ctx)
    /** The address pill inside a frame that also holds its neon glow (a [GlowView] behind it): the pill's edge breathes while you browse. */
    private val pillHolder = FrameLayout(ctx)
    private val pillGlow = GlowView(ctx)

    private val back = actionButton(R.drawable.ic_back, "Back") { l.onBack() }
    private val forward = actionButton(R.drawable.ic_forward, "Forward") { l.onForward() }
    private val home = actionButton(R.drawable.ic_home, "Home") { l.onHome() }
    private val newTab = actionButton(R.drawable.ic_plus, "New tab") { l.onNewTab() }
    private val tabs = TabsButton(ctx, pal.fg).apply { setOnClickListener { l.onTabs() } }
    private val menu = DotImageButton(ctx).apply {
        setImageResource(R.drawable.ic_more); contentDescription = "Menu"; scaleType = ImageView.ScaleType.FIT_CENTER
        setPadding(0, ctx.dp(15), 0, ctx.dp(15))
        setOnClickListener { l.onMenu(this) }
    }
    private val mask = ImageView(ctx).apply { setImageResource(R.drawable.ic_mask); contentDescription = "Private tab"; visibility = GONE }
    private val lock = ImageView(ctx)
    val urlField = EditText(ctx)
    private val chip: ShieldChip = ShieldChip(ctx, pal) { l.onShields(chip) }
    private val reload = ImageButton(ctx).apply {
        setImageResource(R.drawable.ic_refresh); contentDescription = "Reload"; scaleType = ImageView.ScaleType.CENTER
        setOnClickListener { l.onReloadOrStop() }
    }
    private val spacer = View(ctx) // fills the middle of the one-row layout while the address pill is hidden (Home)

    private var wide = false
    private var homeMode = false
    private var privateSkin = pal.isPrivate

    /** Height (px) of the part of the dock that must stay visible above the keyboard while typing: the address row in portrait, the whole row on a wide window. */
    val typingBandPx: Int get() = if (wide) context.dp(ROW_DP) + 1 else context.dp(ROW_DP)
    val actionRowPx: Int get() = if (wide) 0 else context.dp(ROW_DP)

    init {
        orientation = VERTICAL
        urlField.apply {
            setSingleLine()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = null
            setPadding(0, 0, 0, 0)
            setSelectAllOnFocus(true)
            isSaveEnabled = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || (ev != null && ev.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN)) {
                    l.onSubmit(text.toString()); true
                } else false
            }
            setOnFocusChangeListener { _, focused -> paintPill(focused); l.onUrlFocus(focused) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { if (hasFocus()) l.onUrlText(s?.toString() ?: "") }
            })
        }
        pill.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(12), 0, ctx.dp(2), 0)
        }
        mask.layoutParams = LayoutParams(ctx.dp(22), ctx.dp(22)).apply { marginEnd = ctx.dp(8) }
        lock.layoutParams = LayoutParams(ctx.dp(20), ctx.dp(20)).apply { marginEnd = ctx.dp(10) }
        reload.layoutParams = LayoutParams(ctx.dp(46), ctx.dp(46))
        pill.addView(mask); pill.addView(lock); pill.addView(urlField); pill.addView(chip); pill.addView(reload)
        pillHolder.clipChildren = false
        pillHolder.addView(pillGlow, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        pillHolder.addView(pill, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ctx.dp(44), Gravity.CENTER).apply { marginStart = ctx.dp(GLOW_DP); marginEnd = ctx.dp(GLOW_DP) })
        Neon.pulse(pillGlow, 0.3f, 1f, 3400L)
        urlRow.apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(ctx.dp(10), 0, ctx.dp(10), 0) }
        actionRow.apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        addView(hairline, LayoutParams(LayoutParams.MATCH_PARENT, 1))
        layoutFor(false)
        tint()
    }

    private fun actionButton(icon: Int, desc: String, click: () -> Unit): ImageButton = ImageButton(context).apply {
        setImageResource(icon)
        contentDescription = desc
        scaleType = ImageView.ScaleType.FIT_CENTER
        setPadding(0, context.dp(15), 0, context.dp(15))
        setOnClickListener { click() }
    }

    /** Portrait: [address row] over [6 equal buttons]. Wide: one row [back forward home | address | new tabs menu]. */
    private fun layoutFor(wideNow: Boolean) {
        wide = wideNow
        urlRow.removeAllViews(); actionRow.removeAllViews()
        // children are re-parented below; remove from any previous parent first
        for (v in listOf<View>(back, forward, home, newTab, tabs, menu, pillHolder, spacer, urlRow, actionRow)) (v.parent as? android.view.ViewGroup)?.removeView(v)
        while (childCount > 1) removeViewAt(1)
        val rowH = context.dp(ROW_DP)
        if (!wideNow) {
            urlRow.addView(pillHolder, LayoutParams(0, rowH, 1f).apply { marginStart = -context.dp(GLOW_DP); marginEnd = -context.dp(GLOW_DP) })
            addView(urlRow, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
            for (b in listOf(back, forward, home, newTab, tabs, menu)) actionRow.addView(b, LayoutParams(0, rowH, 1f))
            addView(actionRow, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
        } else {
            val w = context.dp(52)
            for (b in listOf(back, forward, home)) actionRow.addView(b, LayoutParams(w, rowH))
            actionRow.addView(pillHolder, LayoutParams(0, rowH, 1f).apply { marginStart = context.dp(6) - context.dp(GLOW_DP); marginEnd = context.dp(6) - context.dp(GLOW_DP) })
            actionRow.addView(spacer, LayoutParams(0, 1, 1f))
            for (b in listOf(newTab, tabs, menu)) actionRow.addView(b, LayoutParams(w, rowH))
            addView(actionRow, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
        }
        applyVisibility()
    }

    override fun onMeasure(w: Int, h: Int) {
        val wideNow = MeasureSpec.getSize(w) >= context.dp(WIDE_DP)
        if (wideNow != wide) layoutFor(wideNow)
        super.onMeasure(w, h)
    }

    /** Set BOTH layouts' states every time: the pill keeps the visibility the wide layout gave it, so rotating landscape -> portrait on Home left an empty address row once a page opened. */
    private fun applyVisibility() {
        if (wide) { pillHolder.visibility = if (homeMode) GONE else VISIBLE; spacer.visibility = if (homeMode) VISIBLE else GONE; urlRow.visibility = VISIBLE }
        else { urlRow.visibility = if (homeMode) GONE else VISIBLE; pillHolder.visibility = VISIBLE; spacer.visibility = GONE }
    }

    /** Re-colour everything for the regular or the private skin. */
    fun applyPal(p: Pal) {
        pal = p
        privateSkin = p.isPrivate
        tint()
        shown = null
    }

    private fun tint() {
        background = GlossDrawable(pal.glossTop, pal.glossBottom, pal.accent, context.dpf(26f), context.dpf(1f))
        hairline.setBackgroundColor(0)
        for (b in listOf(back, forward, home, newTab, menu)) { b.setColorFilter(pal.fg); b.background = ripple(pal.fg, null, 0f) }
        tabs.setPalette(pal.fg, pal.accent)
        reload.setColorFilter(pal.fg); reload.background = ripple(pal.fg, null, 0f)
        mask.setColorFilter(pal.accent)
        menu.dotColor = pal.accent
        chip.setPal(pal)
        urlField.setTextColor(pal.fg)
        urlField.setHintTextColor(pal.muted)
        urlField.hint = if (pal.isPrivate) "Private: search or type address" else "Search or type address"
        urlField.imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI or (if (pal.isPrivate) EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else 0)
        lock.setColorFilter(pal.muted)
        paintPill(urlField.hasFocus())
    }

    private fun paintPillGlow() {
        pillGlow.background = NeonGlowDrawable(pal.accent, context.dpf(10f), ChamferDrawable.TL or ChamferDrawable.BR, context.dpf(GLOW_DP.toFloat()), coreColor = pal.accentHi, coreWidthPx = context.dpf(1.4f))
    }

    private fun paintPill(focused: Boolean) {
        paintPillGlow()
        pill.background = ChamferDrawable(pal.pill, context.dpf(10f), ChamferDrawable.TL or ChamferDrawable.BR,
            if (focused) pal.accent else pal.divider, if (focused) context.dpf(2f) else context.dpf(1f), gloss = true)
    }

    /** Everything bind() shows. When nothing changed since the last call the dock is left alone (no setImageResource / setText / relayout on the UI thread). */
    private data class Shown(
        val tabCount: Int, val canGoBack: Boolean, val canGoForward: Boolean, val loading: Boolean, val shieldsUp: Boolean, val blocked: Int,
        val noTab: Boolean, val urlText: String, val urlFocused: Boolean, val errorKind: Int, val secure: Boolean, val home: Boolean, val priv: Boolean,
    )
    private var shown: Shown? = null

    /** [tabCount]: the tabs of the CURRENT mode (regular or private). [homeShown]: the Home screen covers the page. */
    fun bind(tab: Tab?, tabCount: Int, canGoBack: Boolean, homeShown: Boolean) {
        val now = Shown(
            tabCount, canGoBack, tab?.canGoForward == true, tab?.loading == true, tab?.shields?.shieldsUp ?: true, tab?.blockedTotal() ?: 0,
            tab == null, prettyUrl(tab?.url ?: ""), urlField.hasFocus(),
            when { tab?.error == null -> 0; tab.error is ErrorScreen.Network -> 1; else -> 2 }, tab?.url?.startsWith("https://") == true, homeShown, privateSkin,
        )
        if (now == shown) return
        shown = now
        if (homeShown != homeMode) { homeMode = homeShown; applyVisibility() }
        tabs.count = tabCount
        tabs.privateDot = privateSkin
        back.isEnabled = canGoBack
        back.alpha = if (canGoBack) 1f else 0.35f
        forward.isEnabled = tab?.canGoForward == true && !homeShown
        forward.alpha = if (forward.isEnabled) 1f else 0.35f
        home.setColorFilter(if (homeShown) pal.accent else pal.fg)
        val loading = tab?.loading == true
        reload.setImageResource(if (loading) R.drawable.ic_close else R.drawable.ic_refresh)
        reload.contentDescription = if (loading) "Stop loading" else "Reload"
        val noPage = tab == null || tab.isNewTab
        chip.bind(tab?.shields?.shieldsUp ?: true, tab?.blockedTotal() ?: 0)
        chip.visibility = if (noPage) GONE else VISIBLE
        reload.visibility = if (noPage) GONE else VISIBLE
        mask.visibility = if (privateSkin) VISIBLE else GONE
        if (!urlField.hasFocus()) urlField.setText(prettyUrl(tab?.url ?: ""))
        val url = tab?.url ?: ""
        when {
            noPage -> { lock.setImageResource(R.drawable.ic_search); lock.setColorFilter(pal.muted) }
            tab!!.error != null && tab.error !is ErrorScreen.Network -> { lock.setImageResource(R.drawable.ic_warning); lock.setColorFilter(pal.danger) }
            url.startsWith("https://") -> { lock.setImageResource(R.drawable.ic_lock); lock.setColorFilter(pal.muted) }
            else -> { lock.setImageResource(R.drawable.ic_warning); lock.setColorFilter(pal.danger) }
        }
        lock.contentDescription = if (url.startsWith("https://")) "Secure connection" else "Connection is not secure"
    }

    fun fullUrlForEditing(tab: Tab?): String = tab?.url ?: ""

    /** An accent dot on the menu button: an app update is waiting. */
    fun setMenuDot(on: Boolean) { menu.dot = on }

    companion object {
        const val GLOW_DP = 6
        const val ROW_DP = 56
        const val WIDE_DP = 760
    }
}

/** An ImageButton with an optional accent dot at its top-right (e.g. "an update is waiting" on the menu button). */
class DotImageButton(ctx: Context) : ImageButton(ctx) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    var dotColor = 0
    var dot = false
        set(v) { if (field != v) { field = v; invalidate() } }

    override fun onDraw(c: android.graphics.Canvas) {
        super.onDraw(c)
        if (dot) { paint.color = dotColor; c.drawCircle(width / 2f + context.dpf(10f), height / 2f - context.dpf(11f), context.dpf(5f), paint) }
    }
}
