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
import app.kestrel.core.media.MediaCandidate
import app.kestrel.core.media.MediaSniff
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
    /** A video / audio file / playlist was loaded by a page (called on the worker thread: the receiver posts to the UI thread). */
    private val mediaSeen: (MediaCandidate) -> Unit = {},
    /** The page's own player told us about a file: page URL, file URL, picture width and height, length in ms, the label next to its <source>. */
    private val mediaInfo: (String, String, Int, Int, Long, String?) -> Unit = { _, _, _, _, _, _ -> },
) : WebExtension.MessageDelegate {

    // GeckoView delivers native messages on the Android UI thread and Gecko HOLDS each request until the reply arrives: any work here freezes painting and typing.
    // So onMessage only hands the message to ONE worker and returns a GeckoResult at once; the worker parses, decides and completes it (research/FREEZE.md 4.2).
    // NORMAL priority on purpose: Java priority 4 (NORM_PRIORITY - 1) is nice 10 and Android moves such a thread into the BACKGROUND scheduler group, where it competes with a
    // fraction of one core while Gecko holds every request until this thread answers (research/HW_SPEED.md H1).
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "kestrel-shield").apply { isDaemon = true } }

    override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
        // Only OUR extension's background page may ask (content scripts relay through it with runtime.sendMessage; web pages cannot reach native messaging at all).
        if (nativeApp != "kestrel" || sender.webExtension.id != EXTENSION_ID || sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) return null
        val result = GeckoResult<Any>()
        val raw = message
        worker.execute {
            val reply: Any = try {
                val m = parse(raw)
                if (m == null) "" else when (m.optString("k")) {
                    "req" -> if (shouldBlock(m.optString("u"), m.optString("t"), m.optString("p"))) "1" else "0"
                    "cnt" -> { countBlocked(m.optString("p"), Category.PRIVACY_INDEX); "" }
                    "css" -> cosmeticCss(m.optString("h"))
                    "media" -> { onMedia(m); "" }
                    "vid" -> { onVid(m); "" }
                    else -> ""
                }
            } catch (e: Exception) {
                Log.w(Io.TAG, "shields bridge", e)
                ""
            }
            result.complete(reply)
        }
        return result
    }

    /** A response the extension's sniffer thinks is media: judged again here (the rules live in :core and are tested), then handed on. */
    private fun onMedia(m: JSONObject) {
        val url = m.optString("u")
        val ct = m.optString("c")
        val kind = MediaSniff.classify(url, ct, 200) ?: return
        val page = m.optString("p")
        val len = if (kind == app.kestrel.core.media.MediaKind.FILE) m.optLong("l", -1L) else -1L
        mediaSeen(MediaCandidate(url, kind, MediaSniff.baseType(ct), len, page, m.optString("f").takeIf { it != page } ?: "", System.currentTimeMillis()))
    }

    private fun onVid(m: JSONObject) {
        val url = m.optString("u")
        if (url.isEmpty()) return
        mediaInfo(m.optString("p"), url, m.optInt("w"), m.optInt("h"), m.optLong("d"), m.optString("l").takeIf { it.isNotBlank() })
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
