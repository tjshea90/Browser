package app.kestrel.browser.web

import android.content.Context
import android.webkit.WebView
import app.kestrel.browser.data.Prefs
import java.security.SecureRandom

/**
 * Injects assets/js/shields.js (fingerprint farbling, data-saver signal, no media preload/autoplay).
 *
 * Honest limitation: without AndroidX WebKit's document-start scripts, the earliest safe moment is onPageCommitVisible (the new document
 * exists and has painted), so a script that fingerprints in an inline <script> at the very top of the page can run first. Third-party
 * fingerprinting scripts, the common case, are mostly blocked at the network layer before they ever load.
 */
object ScriptInjector {
    @Volatile private var script: String? = null

    /** One random value per process = per browsing session; mixed with the site inside the script. */
    private val sessionSeed: Int = SecureRandom().nextInt()

    private fun source(ctx: Context): String =
        script ?: ctx.assets.open("js/shields.js").bufferedReader().use { it.readText() }.also { script = it }

    internal fun config(fingerprint: Boolean, saveData: Boolean, noPreload: Boolean, seed: Int = sessionSeed): String =
        "var __KST_CFG={\"fp\":${if (fingerprint) 1 else 0},\"sd\":${if (saveData) 1 else 0},\"pn\":${if (noPreload) 1 else 0},\"seed\":$seed};"

    fun inject(view: WebView, tab: Tab, prefs: Prefs) {
        val s = tab.shields
        val saver = prefs.dataSaver
        val fp = s.shieldsUp && s.blockFingerprinting
        val sd = saver.saveDataSignal
        val pn = saver.mediaPreloadNone
        if (!fp && !sd && !pn) return
        view.evaluateJavascript(config(fp, sd, pn) + source(view.context), null)
    }

    /** Re-inject only when the page lost it (the script marks window.__kst). */
    fun verify(view: WebView, tab: Tab, prefs: Prefs) {
        view.evaluateJavascript("!!window.__kst") { r -> if (r == "false") inject(view, tab, prefs) }
    }
}
