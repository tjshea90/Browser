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

    private val rng = SecureRandom()

    internal fun config(fingerprint: Boolean, saveData: Boolean, noPreload: Boolean, seed: Int = sessionSeed, token: Int = 0): String =
        "var __KST_CFG={\"fp\":${if (fingerprint) 1 else 0},\"sd\":${if (saveData) 1 else 0},\"pn\":${if (noPreload) 1 else 0},\"seed\":$seed,\"tok\":$token};"

    /** Name of the idempotence marker the script leaves on window (shields.js builds the same name from the same token). */
    internal fun markerName(token: Int): String = "__kst" + (token.toLong() and 0xffffffffL).toString(36)

    /** The config is a local variable of a wrapper function, never a global: pages must not be able to read the session seed. */
    internal fun wrap(config: String, source: String): String = "(function(){" + config + "\n" + source + "\n})();"

    fun inject(view: WebView, tab: Tab, prefs: Prefs) {
        val s = tab.shields
        val saver = prefs.dataSaver
        val fp = s.shieldsUp && s.blockFingerprinting
        val sd = saver.saveDataSignal
        val pn = saver.mediaPreloadNone
        if (!fp && !sd && !pn) return
        if (tab.injectToken == 0) tab.injectToken = rng.nextInt() or 1 // fresh for every page load (reset in onPageStarted), never 0
        view.evaluateJavascript(wrap(config(fp, sd, pn, token = tab.injectToken), source(view.context)), null)
    }

    /** Re-inject only when the page lost it (the script marks window[markerName]). */
    fun verify(view: WebView, tab: Tab, prefs: Prefs) {
        val token = tab.injectToken
        if (token == 0) return // nothing was injected for this page (all protections off)
        view.evaluateJavascript("!!window.${markerName(token)}") { r -> if (r == "false") inject(view, tab, prefs) }
    }
}
