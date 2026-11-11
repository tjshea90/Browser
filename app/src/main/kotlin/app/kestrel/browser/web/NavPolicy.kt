package app.kestrel.browser.web

import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Shields
import app.kestrel.core.url.HttpsPolicy
import app.kestrel.core.url.TrackingParams
import app.kestrel.core.url.UrlParts

/** Rewrites applied to every top-level navigation (typed, tapped, or redirected): tracking-parameter stripping + HTTPS-first. */
object NavPolicy {
    class Prepared(val url: String, val upgradedFrom: String?)

    /** The only kind of address a tab may ever be navigated to by this app's own code (menus, links, session, intents all funnel through it). */
    fun isWebUrl(url: String): Boolean = url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

    fun prepare(prefs: Prefs, url: String, tab: Tab?): Prepared {
        val host = UrlParts.hostOf(url)
        val s = Shields.effective(prefs, host)
        var u = url
        if (s.shieldsUp && s.blockAds) TrackingParams.strip(u)?.let { u = it }
        var from: String? = null
        if (s.https != HttpsPolicy.Mode.OFF && host != null && tab?.httpOkHosts?.contains(host) != true) {
            HttpsPolicy.upgradeOf(u)?.let { from = u; u = it }
        }
        return Prepared(u, from)
    }
}
