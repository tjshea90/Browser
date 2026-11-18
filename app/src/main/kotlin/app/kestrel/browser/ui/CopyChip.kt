package app.kestrel.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.kestrel.browser.R

/**
 * UC1 (owner 2026-11-18: "include a fast way to copy any url in the address bar. For example, a copy button pops up when i press on the url bar"): a small Copy button at the right end of the address pill.
 * It is there only while the address field is pressed (has focus) and holds an address; one tap puts exactly what the field holds on the clipboard (the whole address, not the shortened text shown at rest, or what
 * was typed or pasted) and the button says "Copied" for a moment. It never takes the focus, so the keyboard and the selection stay as they are.
 */
class CopyChip(ctx: Context, private var pal: Pal, private val field: EditText, private val restAddress: () -> String) : LinearLayout(ctx) {
    private val icon = ImageView(ctx)
    private val label = TextView(ctx)
    private val handler = Handler(Looper.getMainLooper())
    private val reset = Runnable { label.text = "Copy"; icon.setImageResource(R.drawable.ic_copy); contentDescription = "Copy the address" }

    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; visibility = GONE
        setPadding(ctx.dp(8), 0, ctx.dp(10), 0)
        isClickable = true; isFocusable = false                       // (it must never take the focus, and with it the keyboard, away from the address field)
        contentDescription = "Copy the address"
        icon.setImageResource(R.drawable.ic_copy)
        addView(icon, LayoutParams(ctx.dp(18), ctx.dp(18)))
        label.text = "Copy"; label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); label.setTypeface(label.typeface, Typeface.BOLD)
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = ctx.dp(4) })
        setOnClickListener { copy() }
        setPal(pal)
    }

    /** The regular or the private skin. */
    fun setPal(p: Pal) {
        pal = p
        background = roundedBg(p.card, context.dpf(14f), p.accent, context.dp(1))
        icon.setColorFilter(p.accent); label.setTextColor(p.accent)
    }

    /** The address to copy: what the field holds while it is pressed (nothing in it = nothing to copy), else the page's own address. */
    fun addressToCopy(): String {
        val typed = field.text?.toString()?.trim() ?: ""
        return if (field.hasFocus()) typed else restAddress()
    }

    /** Show the button while the field is pressed and has something in it; hide it (and its "Copied") otherwise. Called on focus, text and press changes. */
    fun refresh() {
        val show = field.hasFocus() && addressToCopy().isNotEmpty()
        if (show) { if (visibility != VISIBLE) visibility = VISIBLE }
        else if (visibility != GONE) { visibility = GONE; handler.removeCallbacks(reset); reset.run() }
    }

    fun copy() {
        val text = addressToCopy()
        if (text.isEmpty()) return
        try { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("address", text)) } catch (e: Exception) { return }
        label.text = "Copied"; icon.setImageResource(R.drawable.ic_check); contentDescription = "Address copied"
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        handler.removeCallbacks(reset); handler.postDelayed(reset, 1300)
        app.kestrel.browser.diag.Health.log("address", "copied", "chars=${text.length}")       // (no address in the log)
    }
}
