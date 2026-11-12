package app.kestrel.browser.web

import android.content.Context
import android.util.Log
import app.kestrel.browser.GeckoEngine
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.Shields
import app.kestrel.core.filter.ExtImage
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
 * The bridge between the built-in "Kestrel Shields" WebExtension (assets/kestrel-shields) and Kestrel's own filter engine.
 * Since D1 the extension decides requests itself (engine.js over an image of the app's lists, research/D1-DESIGN.md) and only asks the app per PAGE:
 *  - `cfg`: does blocking apply to this site (the per-site Shields switches live here) and which list generation is current,
 *  - `img`: the list image in chunks, `cnt`: blocked counts per category (batched), `st`: the engine's own timing numbers (Diagnostics),
 *  - `css`: element-hiding CSS of a host, `media`/`vid`: the video sniffer.
 * `req` (is THIS request blocked?) remains for the moments before the engine has loaded and for the compatibility switch in Diagnostics.
 * Same lists, same per-site switches, same counters as the WebView build - only the transport differs.
 */
class ShieldsBridge(
    private val prefs: () -> Prefs,
    private val tabForPage: (String) -> Tab?,
    private val blockedOnTab: (Tab) -> Unit,
    /** A video / audio file / playlist was loaded by a page (called on the worker thread: the receiver posts to the UI thread). */
    private val mediaSeen: (MediaCandidate) -> Unit = {},
    /** The page's own player told us about a file: page URL, file URL, picture width and height, length in ms, the label next to its <source>. */
    private val mediaInfo: (String, String, Int, Int, Long, String?) -> Unit = { _, _, _, _, _, _ -> },
    /** The pop-up and redirect guard refused something: page address, kind ("open", "click", "overlay", "form", "redirect"), the address it wanted (called on the worker thread). */
    private val popupBlocked: (String, String, String) -> Unit = { _, _, _ -> },
) : WebExtension.MessageDelegate {

    // GeckoView delivers native messages on the Android UI thread and Gecko HOLDS each request until the reply arrives: any work here freezes painting and typing.
    // So onMessage only hands the message to ONE worker and returns a GeckoResult at once; the worker parses, decides and completes it (research/FREEZE.md 4.2).
    // NORMAL priority on purpose: Java priority 4 (NORM_PRIORITY - 1) is nice 10 and Android moves such a thread into the BACKGROUND scheduler group, where it competes with a
    // fraction of one core while Gecko holds every request until this thread answers (research/HW_SPEED.md H1).
    // A HandlerThread (not an executor) so the GeckoResult can be created WITH its handler: its callbacks (the reply to Gecko) then run on this thread instead of being posted back to the
    // Android UI thread (the third UI task of every request; sendSuccess / sendError are dispatched to Gecko's own thread by the engine, any thread may call them).
    private val workerThread = android.os.HandlerThread("kestrel-shield", android.os.Process.THREAD_PRIORITY_DEFAULT).also { it.start() }
    private val workerHandler = android.os.Handler(workerThread.looper)
    // Element-hiding CSS is slow the first time a host is seen (the generic selector set is built once, about 2 s on the emulator): it must not hold up the verdicts queued behind it.
    private val cosmeticThread = android.os.HandlerThread("kestrel-cosmetic", android.os.Process.THREAD_PRIORITY_DEFAULT).also { it.start() }
    private val cosmeticHandler = android.os.Handler(cosmeticThread.looper)

    override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
        // Only OUR extension's background page may ask (content scripts relay through it with runtime.sendMessage; web pages cannot reach native messaging at all).
        if (nativeApp != "kestrel" || sender.webExtension.id != EXTENSION_ID || sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) return null
        val raw = message
        val slow = (raw is String && (raw.startsWith("{\"k\":\"css\"") || raw.startsWith("{\"k\":\"img\""))) // slow work: its own thread, never behind the page's verdicts
        val handler = if (slow) cosmeticHandler else workerHandler
        val result = GeckoResult<Any>(handler)
        handler.post {
            val reply: Any = try {
                val m = parse(raw)
                if (m == null) "" else when (m.optString("k")) {
                    "req" -> if (shouldBlock(m.optString("u"), m.optString("t"), m.optString("p"))) "1" else "0"
                    "cfg" -> pageConfig(m.optString("h"))
                    "img" -> imageChunk(m.optInt("g"), m.optInt("i", -1))
                    "cnt" -> { counts(m); "" }
                    "st" -> { stats = EngineStats.from(m); "" }
                    "pop" -> { onPop(m); "" }
                    "css" -> cosmeticCss(m.optString("h"), m.optBoolean("top", true))
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

    /** The guard in the page (or the redirect check in the extension) refused something: "<kind>|<address>". */
    private fun onPop(m: JSONObject) {
        val d = m.optString("d")
        val c = d.indexOf('|')
        val kind = if (c > 0) d.substring(0, c) else d
        val url = if (c > 0) d.substring(c + 1) else ""
        popupBlocked(m.optString("p"), kind, url)
    }

    /** A response the extension's sniffer thinks is media: judged again here (the rules live in :core and are tested), then handed on. */
    private fun onMedia(m: JSONObject) {
        val url = m.optString("u")
        val ct = m.optString("c")
        val kind = MediaSniff.classify(url, ct, 200) ?: return
        val page = m.optString("p")
        // an address found in the page's own markup (not loaded yet): ad networks are dropped here, the same lists that block their requests
        if (m.optString("s") == "dom") {
            val fs = Engine.peek()
            if (fs != null && Verdict.isBlocked(fs.check(url, UrlParts.hostOf(page), RequestType.MEDIA))) return
        }
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

    /**
     * `cfg`: "<mode>:<generation>:<content id>:<flags>" (flags: 1 = the pop-up and redirect guard applies to this site). mode 1 = block on pages of this host, 0 = Shields off / site allowed, L = ask per request (compatibility mode).
     * While the lists are still loading (cold start) the answer is provisional - generation 0, no id, the site allow-list unknown - so the extension can already judge requests with the
     * copy it saved last time and asks again within seconds; before this, those first requests went unfiltered.
     */
    private fun pageConfig(host: String): String {
        val snap = Engine.snapshot()
        val gen = snap?.generation ?: 0
        if (host.isEmpty()) return (if (prefs().engineInExtension) "0:" else "L:") + gen + "::0"
        val s = Shields.effective(prefs(), host)
        val guard = if (prefs().blockPopups && s.shieldsUp) 1 else 0 // the pop-up and redirect guard follows the Shields master switch and its own setting
        if (!prefs().engineInExtension) return "L:$gen::$guard"
        val on = s.shieldsUp && s.blockAds && snap?.fs?.isPageAllowed(host) != true
        return (if (on) "1:" else "0:") + gen + ":" + (if (snap != null) contentId(snap) else "") + ":" + guard
    }

    /** Set by [install]: the app build, part of the image identity (a new build may ship a new Public Suffix List). */
    @Volatile var salt: Long = 0L
    private var idCache: Pair<Int, String>? = null
    private fun contentId(snap: Engine.Snap): String {
        val c = idCache
        if (c != null && c.first == snap.generation) return c.second
        return ExtImage.contentId(snap.fs, Psl.default, salt).also { idCache = Pair(snap.generation, it) }
    }

    /** `img`: chunk [index] of the KEX1 image of generation [gen] (format: [ExtImage.chunkReply]), or "" when that generation is gone. */
    private fun imageChunk(gen: Int, index: Int): String {
        val snap = Engine.snapshot() ?: return ""
        if (snap.generation != gen) return ""
        return ExtImage.chunkReply(images.get(snap), index, gen, images.encodeMs, contentId(snap))
    }

    private val images = ImageCache()

    /** The encoded image of the current generation, kept softly (the garbage collector may drop it; it is rebuilt in a few tens of milliseconds). */
    private class ImageCache {
        private var ref: java.lang.ref.SoftReference<Pair<Int, ByteArray>>? = null
        @Volatile var encodeMs = 0L
        fun get(snap: Engine.Snap): ByteArray {
            val cur = ref?.get()
            if (cur != null && cur.first == snap.generation) return cur.second
            val t0 = android.os.SystemClock.elapsedRealtime()
            val bytes = ExtImage.encode(snap.fs, Psl.default, snap.generation)
            encodeMs = android.os.SystemClock.elapsedRealtime() - t0
            ref = java.lang.ref.SoftReference(Pair(snap.generation, bytes))
            return bytes
        }
    }

    /** `cnt`: either the batched per-category counts of the engine in the extension (`c`: ads, trackers, malware, annoyance) or the older single number `n` (trackers). */
    private fun counts(m: JSONObject) {
        val page = m.optString("p")
        val c = m.optJSONArray("c")
        if (c == null) { countBlocked(page, Category.PRIVACY_INDEX, m.optInt("n", 1)); return }
        for (cat in 0 until minOf(4, c.length())) { val n = c.optInt(cat); if (n > 0) countBlocked(page, cat, n) }
    }

    private fun countBlocked(pageUrl: String, category: Int, n: Int = 1) {
        val t = tabForPage(pageUrl) ?: return
        t.blocked.addAndGet(category, n)
        t.pendingLifetime.addAndGet(n.toLong())
        blockedOnTab(t)
    }

    /** Element-hiding CSS for a frame of [host]. Only the top document gets the big generic set (up to 1,200 selectors); an iframe, usually an ad itself, gets the rules written for its own site. */
    private fun cosmeticCss(host: String, top: Boolean): String {
        if (host.isEmpty()) return ""
        val fs = Engine.peek() ?: return ""
        val s = Shields.effective(prefs(), host)
        if (!s.shieldsUp || !s.blockAds || !s.cosmetic) return ""
        return fs.cosmeticCss(host, includeGeneric = top)
    }

    /** What the extension's engine reports about itself (`st`), shown in Diagnostics. */
    class EngineStats(
        val generation: Int, val checks: Long, val totalUs: Long, val maxUs: Long, val slow: Long, val perRequestAsks: Long, val perRequestUs: Long, val errors: Int,
        val loadMs: Long, val imageBytes: Int, val transfers: Int, val off: Boolean, val note: String, val at: Long,
    ) {
        companion object {
            fun from(m: JSONObject) = EngineStats(
                m.optInt("g"), m.optLong("n"), m.optLong("us"), m.optLong("mx"), m.optLong("sl"), m.optLong("lg"), m.optLong("lu"), m.optInt("er"),
                m.optLong("ld"), m.optInt("by"), m.optInt("pl"), m.optInt("off") != 0, m.optString("w").take(400), System.currentTimeMillis(),
            )
        }
    }

    companion object {
        const val EXTENSION_ID = "shields@kestrel.local"

        /** The latest numbers from the extension's engine (null until it has judged a request or finished a transfer). */
        @Volatile var stats: EngineStats? = null

        /** One line for Diagnostics: where the blocking decisions are made and how long they take. */
        fun statsLine(inExtension: Boolean): String {
            if (!inExtension) {
                val c = stats
                val held = if (c != null && c.perRequestAsks > 0) " ${c.perRequestAsks} requests asked the app one by one, each held ${c.perRequestUs / c.perRequestAsks} us on average." else ""
                return "Blocker: compatibility mode - every request asks the app (slower). Tap the Blocker button to go back to fast mode.$held"
            }
            val s = stats ?: return "Blocker: fast mode (decisions inside the page engine). No numbers yet: open a page, then press Update."
            val avg = if (s.checks > 0) s.totalUs / s.checks else 0L
            val state = when {
                s.off -> "OFF this session (too many errors)"
                s.generation > 0 && s.imageBytes > 0 -> "list #${s.generation}, ${s.imageBytes / 1024} KB, loaded in ${s.loadMs} ms, ${s.transfers} transfer(s)"
                else -> "list not loaded yet"
            }
            return "Blocker: fast mode, $state. ${s.checks} requests judged in the engine (average ${avg} us, slowest ${s.maxUs} us, ${s.slow} over 2 ms), ${s.perRequestAsks} asked the app one by one${if (s.perRequestAsks > 0) " (each held ${s.perRequestUs / s.perRequestAsks} us on average)" else ""}, ${s.errors} errors. ${s.note}".trim()
        }

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
            bridge.salt = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode } catch (e: Exception) { 0L }
            val controller = GeckoEngine.runtime(ctx).webExtensionController
            controller.ensureBuiltIn(ASSET_URI, EXTENSION_ID).accept(
                { ext ->
                    ext?.setMessageDelegate(bridge, "kestrel")
                    // Blocking must cover private tabs too. Built-in extensions are allowed there already; asking again RELOADS the running extension (a full shutdown and start of its background page,
                    // measured in the engine's own code), which this used to do at every launch and every Activity re-creation, right while the first page loaded. Ask only if it is really off.
                    if (ext != null && !ext.metaData.allowedInPrivateBrowsing) controller.setAllowedInPrivateBrowsing(ext, true)
                },
                { e -> Log.w(Io.TAG, "Kestrel Shields extension failed to install", e) },
            )
        }
    }

    private object Category { const val PRIVACY_INDEX = app.kestrel.core.filter.Category.PRIVACY }
}
