package app.kestrel.browser.web

import android.content.Context
import org.mozilla.geckoview.GeckoView
import app.kestrel.browser.diag.Health

/**
 * The one GeckoView of the activity, with timeline hooks: every surface-related event lands in the Diagnostics log, so a "the screen stopped updating"
 * report shows whether the page view was detached, resized or given a new surface around that moment (research/FREEZE.md 5.1). No behaviour change.
 */
class KestrelGeckoView(ctx: Context) : GeckoView(ctx) {
    override fun requestNewSurface() {
        Health.log("gecko", "requestNewSurface"); Health.count("gecko.newSurface")
        super.requestNewSurface()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Health.log("gecko", "view-attached")
    }

    override fun onDetachedFromWindow() {
        Health.log("gecko", "view-detached"); Health.count("gecko.detached")
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Health.log("gecko", "size", "${w}x$h (was ${oldw}x$oldh)")
    }
}
