package app.kestrel.browser.ui

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.PopupWindow

/**
 * Where is the keyboard? The browser window uses adjustNothing while an address field is typed in (the page view must never resize under the keyboard), and
 * under adjustNothing Android 10 no longer reports the keyboard through the window's own visible frame. So:
 *  - Android 11+ : the window insets say it (IME inset), whatever the soft-input mode;
 *  - Android 10  : a 0-pixel-wide, full-height, untouchable popup window that is ITSELF adjustResize - the keyboard shrinks that probe (never the browser
 *    window), and the probe's visible frame ends where the keyboard begins.
 */
class KeyboardProbe(private val activity: Activity) {
    private var popup: PopupWindow? = null
    private var view: View? = null

    /** Call once the window is attached (it retries by itself on the next call if the window token is not ready yet). */
    fun attach(anchor: View) {
        if (Build.VERSION.SDK_INT >= 30 || popup != null) return
        try {
            val v = View(activity)
            val p = PopupWindow(v, 0, ViewGroup.LayoutParams.MATCH_PARENT)
            p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            p.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
            p.isTouchable = false
            p.isFocusable = false
            p.setBackgroundDrawable(ColorDrawable(0))
            p.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0)
            popup = p; view = v
        } catch (e: Exception) { popup = null; view = null } // window not ready yet: the next attach() call tries again
    }

    fun detach() { try { popup?.dismiss() } catch (e: Exception) { /* already gone */ }; popup = null; view = null }

    /** Top edge of the keyboard in screen pixels; 0 = no keyboard (or unknown). */
    fun topScreen(window: Window): Int {
        val screenH = activity.resources.displayMetrics.heightPixels
        val min = (120 * activity.resources.displayMetrics.density).toInt()
        if (Build.VERSION.SDK_INT >= 30) {
            val decor = window.decorView
            val wi = decor.rootWindowInsets ?: return 0
            if (!wi.isVisible(WindowInsets.Type.ime())) return 0
            val h = wi.getInsets(WindowInsets.Type.ime()).bottom
            if (h < min) return 0
            val loc = IntArray(2); decor.getLocationOnScreen(loc)
            return loc[1] + decor.height - h
        }
        val v = view ?: return 0
        val r = Rect(); v.getWindowVisibleDisplayFrame(r)
        return if (screenH - r.bottom > min) r.bottom else 0
    }
}
