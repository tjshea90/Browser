package app.kestrel.browser.web

import android.webkit.WebView
import app.kestrel.browser.shield.Engine
import app.kestrel.core.url.JsEscape

/**
 * Hides ad containers by adding a <style> element to the page. Timing: the document does not exist yet at onPageStarted, so we inject
 * at onPageCommitVisible (first paint of the NEW document) and re-check at onPageFinished with a tiny probe instead of resending the CSS.
 */
object CosmeticInjector {
    private const val PROBE = "(function(){return !!document.getElementById('__kst')})()"

    fun inject(view: WebView, tab: Tab) {
        val s = tab.shields
        if (!s.shieldsUp || !s.cosmetic || tab.pageAllowed) return
        val host = tab.topHost ?: return
        val fs = Engine.peek() ?: return
        val css = fs.cosmeticCss(host)
        if (css.isEmpty()) return
        view.evaluateJavascript(
            "(function(){try{var d=document;if(d.getElementById('__kst'))return;var s=d.createElement('style');s.id='__kst';" +
                "s.textContent=" + JsEscape.quote(css) + ";(d.head||d.documentElement).appendChild(s)}catch(e){}})()",
            null,
        )
    }

    /** Re-inject only if the style element is missing (e.g. commit-visible fired before the new document existed). */
    fun verify(view: WebView, tab: Tab) {
        val s = tab.shields
        if (!s.shieldsUp || !s.cosmetic || tab.pageAllowed) return
        view.evaluateJavascript(PROBE) { r -> if (r == "false") inject(view, tab) }
    }
}
