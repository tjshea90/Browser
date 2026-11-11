package app.kestrel.browser.web

import android.content.Context
import android.util.Log
import app.kestrel.browser.GeckoEngine
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.Shields
import app.kestrel.core.filter.Psl
import app.kestrel.core.filter.RequestType
import app.kestrel.core.filter.Verdict
import app.kestrel.core.url.UrlParts
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension

/**
 * The bridge between the built-in "Kestrel Shields" WebExtension (assets/kestrel-shields) and Kestrel's own filter engine: the extension's
 * webRequest listener asks [onMessage] "block this request?", and its content script asks for the element-hiding CSS of the page's host.
 * Same lists, same per-site Shields switches, same counters as the WebView build - only the transport differs.
 */
class ShieldsBridge(
    private val prefs: () -> Prefs,
    private val tabForPage: (String) -> Tab?,
    private val blockedOnTab: (Tab) -> Unit,
) : WebExtension.MessageDelegate {

    override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
        // Only OUR extension's background page may ask (content scripts relay through it with runtime.sendMessage; web pages cannot reach native messaging at all).
        if (nativeApp != "kestrel" || sender.webExtension.id != EXTENSION_ID || sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) return null
        val m = parse(message) ?: return null
        return try {
            when (m.optString("k")) {
                "req" -> GeckoResult.fromValue(if (shouldBlock(m.optString("u"), m.optString("t"), m.optString("p"))) "1" else "0")
                "cnt" -> { countBlocked(m.optString("p"), Category.PRIVACY_INDEX); GeckoResult.fromValue("") }
                "css" -> GeckoResult.fromValue(cosmeticCss(m.optString("h")))
                else -> null
            }
        } catch (e: Exception) {
            Log.w(Io.TAG, "shields bridge", e)
            GeckoResult.fromValue("")
        }
    }

    /** The extension sends JSON text; tolerate the engine handing it over already parsed. */
    private fun parse(message: Any): JSONObject? = try {
        when (message) {
            is JSONObject -> message
            is org.mozilla.gecko.util.GeckoBundle -> message.toJSONObject()
            else -> JSONObject(message.toString())
        }
    } catch (e: Exception) { null }

    private fun shouldBlock(url: String, type: String, pageUrl: String): Boolean {
        val fs = Engine.peek() ?: return false // lists still loading at cold start
        val pageHost = UrlParts.hostOf(pageUrl) ?: return false
        val s = Shields.effective(prefs(), pageHost)
        if (!s.shieldsUp || !s.blockAds || fs.isPageAllowed(pageHost)) return false
        val v = fs.check(url, pageHost, requestType(type), Psl.default)
        if (!Verdict.isBlocked(v)) return false
        countBlocked(pageUrl, Verdict.category(v))
        return true
    }

    private fun countBlocked(pageUrl: String, category: Int) {
        val t = tabForPage(pageUrl) ?: return
        t.blocked.incrementAndGet(category)
        t.pendingLifetime.incrementAndGet()
        blockedOnTab(t)
    }

    private fun cosmeticCss(host: String): String {
        if (host.isEmpty()) return ""
        val fs = Engine.peek() ?: return ""
        val s = Shields.effective(prefs(), host)
        if (!s.shieldsUp || !s.blockAds || !s.cosmetic) return ""
        return fs.cosmeticCss(host)
    }

    companion object {
        const val EXTENSION_ID = "shields@kestrel.local"
        private const val ASSET_URI = "resource://android/assets/kestrel-shields/"

        /** webRequest resource type -> filter-engine request type. */
        internal fun requestType(t: String): Int = when (t) {
            "script" -> RequestType.SCRIPT
            "stylesheet" -> RequestType.STYLESHEET
            "image", "imageset" -> RequestType.IMAGE
            "media" -> RequestType.MEDIA
            "font" -> RequestType.FONT
            "xmlhttprequest", "fetch" -> RequestType.XHR
            "websocket" -> RequestType.WEBSOCKET
            "sub_frame" -> RequestType.SUBDOCUMENT
            "object", "object_subrequest" -> RequestType.OBJECT
            "ping", "beacon" -> RequestType.PING
            else -> RequestType.OTHER
        }

        /** Installs the built-in extension (idempotent) and (re)attaches [bridge]; call from every Activity instance. */
        fun install(ctx: Context, bridge: ShieldsBridge) {
            val controller = GeckoEngine.runtime(ctx).webExtensionController
            controller.ensureBuiltIn(ASSET_URI, EXTENSION_ID).accept(
                { ext ->
                    ext?.setMessageDelegate(bridge, "kestrel")
                    if (ext != null) controller.setAllowedInPrivateBrowsing(ext, true) // blocking must cover private tabs too (built-ins usually are, this makes it certain)
                },
                { e -> Log.w(Io.TAG, "Kestrel Shields extension failed to install", e) },
            )
        }
    }

    private object Category { const val PRIVACY_INDEX = app.kestrel.core.filter.Category.PRIVACY }
}
