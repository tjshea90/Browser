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
import app.kestrel.browser.data.Entry

/**
 * The Home screen (owner request): pure black, one large stylized field in the middle. Words search Google, an address opens that address.
 * Nothing else competes with it - a small wordmark above, one faint line of what Shields saved below. The field is a glossy chamfered slab with a
 * neon-blue search block and a soft breathing glow; its edge goes ice-bright while it has focus. It is also the new-tab page.
 */
class HomeView(ctx: Context, private var pal: Pal, private val brand: String, private val l: AddressListener) : FrameLayout(ctx) {
    val field = EditText(ctx)
    private val column = LinearLayout(ctx)
    private val brandIcon = ImageView(ctx)
    private val brandText = label(ctx, brand, 15f, pal.fg, bold = true)
    private val badge = label(ctx, "PRIVATE", 11f, pal.accentOn, bold = true)
    private val box = LinearLayout(ctx)
    private val block = ImageView(ctx)
    private val go = ImageButton(ctx)
    private val stats = label(ctx, "", 12f, pal.muted)
    private val hint = label(ctx, "", 13f, pal.muted)
    val suggest = SuggestionList(ctx, pal, reverse = false) { value -> l.onSubmit(value) }
    private var lastFieldFocus = false
    /** Behind everything: the mood of the owner's reaper photo (blue flames from the bottom corners, the scythe's arc). Static. */
    private val backdrop = HomeBackdrop(ctx, pal.accent, pal.deep, pal.accentHi)
    /** The glow around the field (a sibling BEHIND the column, so the layout of the column is untouched); it breathes via [Neon]. */
    private val glow = GlowView(ctx)

    init {
        isClickable = true // swallow touches: the page underneath must never receive them
        isFocusableInTouchMode = false

        field.apply {
            setSingleLine()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = null
            setPadding(ctx.dp(16), 0, ctx.dp(4), 0)
            setSelectAllOnFocus(true)
            isSaveEnabled = false
            gravity = Gravity.CENTER_VERTICAL
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || (ev != null && ev.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN)) {
                    l.onSubmit(text.toString()); true
                } else false
            }
            setOnFocusChangeListener { _, focused -> lastFieldFocus = focused; paintBox(); l.onUrlFocus(focused) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    go.visibility = if (s.isNullOrEmpty()) GONE else VISIBLE
                    if (hasFocus()) l.onUrlText(s?.toString() ?: "")
                }
            })
        }
        block.apply { setImageResource(R.drawable.ic_search); scaleType = ImageView.ScaleType.CENTER; contentDescription = "Search" }
        go.apply {
            setImageResource(R.drawable.ic_forward); scaleType = ImageView.ScaleType.CENTER; contentDescription = "Go"; visibility = GONE
            setOnClickListener { l.onSubmit(field.text.toString()) }
        }
        box.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(2), ctx.dp(2), ctx.dp(2), ctx.dp(2))
            addView(block, LinearLayout.LayoutParams(ctx.dp(64), LinearLayout.LayoutParams.MATCH_PARENT))
            addView(field, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            addView(go, LinearLayout.LayoutParams(ctx.dp(56), LinearLayout.LayoutParams.MATCH_PARENT))
        }
        brandText.letterSpacing = 0.28f
        badge.letterSpacing = 0.16f
        badge.setPadding(ctx.dp(8), ctx.dp(3), ctx.dp(8), ctx.dp(3))
        brandIcon.setImageResource(R.drawable.ic_shield)
        val brandRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(brandIcon, LinearLayout.LayoutParams(ctx.dp(26), ctx.dp(26)).apply { marginEnd = ctx.dp(12) })
            addView(brandText)
            addView(badge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = ctx.dp(12) })
        }
        column.orientation = LinearLayout.VERTICAL
        column.gravity = Gravity.CENTER_HORIZONTAL
        column.addView(brandRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ctx.dp(34)))
        column.addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ctx.dp(FIELD_DP)).apply { topMargin = ctx.dp(GAP_DP) })
        column.addView(hint, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(14) })
        suggest.visibility = GONE
        column.addView(suggest, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(10) })
        addView(backdrop, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(glow, LayoutParams(0, 0, Gravity.TOP or Gravity.START))
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        addView(stats, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = ctx.dp(22) })
        tint()
        Neon.pulse(glow, 0.5f, 1f, 4200L)
    }

    fun applyPal(p: Pal) { pal = p; tint() }

    private fun tint() {
        setBackgroundColor(pal.windowBg)
        brandIcon.setColorFilter(pal.accent)
        brandText.setTextColor(pal.fg)
        badge.setTextColor(pal.accentOn)
        badge.background = ChamferDrawable(pal.accent, context.dpf(5f), ChamferDrawable.TL or ChamferDrawable.BR)
        badge.visibility = if (pal.isPrivate) VISIBLE else GONE
        field.setTextColor(pal.fg)
        field.setHintTextColor(pal.muted)
        field.hint = if (pal.isPrivate) "Search privately or type a URL" else "Search Google or type a URL"
        field.imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI or (if (pal.isPrivate) EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else 0)
        block.background = ChamferDrawable(pal.accent, context.dpf(15f), ChamferDrawable.TL)
        block.setColorFilter(pal.accentOn)
        go.setColorFilter(pal.accent)
        go.background = ripple(pal.fg, null, 0f)
        stats.setTextColor(pal.muted)
        hint.setTextColor(pal.muted)
        hint.text = if (pal.isPrivate) "Private tabs leave no history, cookies or cache on this device" else ""
        hint.visibility = if (pal.isPrivate) VISIBLE else GONE
        suggest.setPal(pal)
        backdrop.setColors(pal.accent, pal.deep, pal.accentHi)
        glow.background = NeonGlowDrawable(pal.accent, context.dpf(FIELD_CUT_DP), ChamferDrawable.TL or ChamferDrawable.BR, context.dpf(GLOW_DP.toFloat()))
        paintBox()
    }

    private fun paintBox() {
        val ctx = context
        box.background = ChamferDrawable(pal.pill, ctx.dpf(FIELD_CUT_DP), ChamferDrawable.TL or ChamferDrawable.BR,
            if (lastFieldFocus) pal.accentHi else pal.deep, ctx.dpf(if (lastFieldFocus) 2f else 1.5f), gloss = true)
    }

    /** A faint line under everything: what Shields did (the only "stat" on Home). */
    fun bindStats(blocked: Long, metered: Boolean) {
        val n = java.text.NumberFormat.getIntegerInstance().format(blocked)
        stats.text = (if (blocked > 0) "$n ads & trackers blocked" else "Shields are on") + (if (metered) "  ·  metered connection" else "")
    }

    fun bindSuggestions(typed: String, entries: List<Entry>, maxHeightPx: Int) {
        suggest.bind(typed, entries)
        suggest.layoutParams = (suggest.layoutParams as LinearLayout.LayoutParams).also { it.height = minOf(suggest.contentHeightPx(), maxHeightPx) }
        suggest.visibility = VISIBLE
    }

    fun hideSuggestions() { suggest.visibility = GONE }

    /** Sizes the column: the field sits slightly above the middle of the screen, and never moves when the suggestion list appears below it. */
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        placeColumn()
    }

    private fun placeColumn() {
        val ctx = context
        val contentH = ctx.dp(34 + GAP_DP + FIELD_DP)
        val top = ((height * 0.44f) - contentH / 2f).toInt().coerceAtLeast(ctx.dp(24))
        val sideMax = ctx.dp(MAX_W_DP)
        val side = maxOf(ctx.dp(20), (width - sideMax) / 2)
        (column.layoutParams as LayoutParams).let { it.topMargin = top; it.leftMargin = side; it.rightMargin = side }
        column.requestLayout()
        val g = ctx.dp(GLOW_DP)
        (glow.layoutParams as LayoutParams).let {
            it.width = width - 2 * side + 2 * g; it.height = ctx.dp(FIELD_DP) + 2 * g
            it.leftMargin = side - g; it.topMargin = top + ctx.dp(34 + GAP_DP) - g
        }
        glow.requestLayout()
    }

    /**
     * The keyboard covers the bottom of the screen from [keyboardTop] (px in this view's coordinates; <= 0 = no keyboard). The page is never resized for it, so
     * the field slides up instead: to the top of the screen while the keyboard is up, so the suggestions below it have room.
     */
    fun onKeyboard(keyboardTop: Int) {
        val ctx = context
        if (keyboardTop <= 0 || keyboardTop >= height) { column.translationY = 0f; glow.translationY = 0f; return }
        val top = (column.layoutParams as LayoutParams).topMargin
        // room for the wordmark (and its PRIVATE badge) above the field while the visible area is tall enough; on a short window only the field matters
        val keepBrand = keyboardTop > ctx.dp(400)
        val anchor = if (keepBrand) top else top + ctx.dp(34 + GAP_DP)
        column.translationY = -(anchor - ctx.dp(16)).coerceAtLeast(0).toFloat()
        glow.translationY = column.translationY
    }

    /** Pixels between the field and the keyboard (or the bottom edge): the room the suggestion list may use. */
    fun suggestionRoom(keyboardTop: Int): Int {
        val ctx = context
        val bottom = if (keyboardTop in 1 until height) keyboardTop else height
        val fieldBottomShifted = (column.layoutParams as LayoutParams).topMargin + ctx.dp(34 + GAP_DP + FIELD_DP) + column.translationY.toInt()
        return (bottom - fieldBottomShifted - ctx.dp(24)).coerceAtLeast(ctx.dp(56))
    }

    companion object {
        const val FIELD_DP = 72
        const val GAP_DP = 26
        const val MAX_W_DP = 560
        const val GLOW_DP = 10
        const val FIELD_CUT_DP = 18f
    }
}
