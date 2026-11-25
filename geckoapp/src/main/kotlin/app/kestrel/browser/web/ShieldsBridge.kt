package app.kestrel.browser.web

import android.content.Context
import android.util.Log
import app.kestrel.browser.GeckoEngine
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.Shields
import app.kestrel.core.filter.BridgeLane
import app.kestrel.core.filter.ExtImage
import app.kestrel.core.filter.Psl
import app.kestrel.core.filter.RequestType
import app.kestrel.core.filter.Verdict
import app.kestrel.core.media.MediaCandidate
import app.kestrel.core.media.MediaHeaders
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
    /** page, address, picture width, height, length in ms, label, and (RX3-V1) what the element is for; the facts are null when the page script sent none (an older one). */
    private val mediaInfo: (String, String, Int, Int, Long, String?, app.kestrel.core.media.VideoFacts?, Boolean, Int) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    /** The pop-up and redirect guard refused something: page address, kind ("open", "click", "overlay", "form", "redirect"), the address it wanted (called on the worker thread). */
    private val popupBlocked: (String, String, String) -> Unit = { _, _, _ -> },
    /** The page's script named an origin the owner is likely to open next (EX-15): `https://host[:port]/`, the page's address, "touch" (finger on a link) or "result" (a top search result). Worker thread. */
    private val warmLink: (String, String, String) -> Unit = { _, _, _ -> },
    /** True when a PRIVATE tab shows a page of this address's host (its report must never be kept). Defaults to "yes, drop everything" so an unwired bridge keeps nothing. */
    private val pageIsPrivate: (String) -> Boolean = { true },
    /** A response that says nothing about being media (text/plain, octet-stream, a "picture" a script fetched) but might be a playlist or a video in disguise: the whole message (u, c, l, p, f, rf, og, ty). Worker thread. */
    private val mediaMaybe: (JSONObject) -> Unit = {},
    /** A player frame of the page went to another document (a server switch): the page's address and the documents that are gone (their candidates must be dropped). Worker thread. */
    private val mediaGone: (String, List<String>) -> Unit = { _, _ -> },
    /** The page (top frame) says where it is scrolled: the page's address and the place in CSS pixels (EY-1, the saved view). Worker thread. */
    private val pageScrolled: (String, Int) -> Unit = { _, _ -> },
    /** A long press landed on OPEN space of the page (no link, picture, video, field or text under the finger; AY2): the page's address. Worker thread. */
    private val openSpace: (String) -> Unit = {},
    /** RX3-Y2: the YouTube ad skipper reports numbers (ads seen, skipped, ...) and the page of its tab: the whole message (a, s, c, o, d, f, g, r, p). Worker thread. */
    private val adSkips: (JSONObject) -> Unit = {},
    /** YS1: seekbar.js reports numbers (taps on a video bar, answered by the page, answered by the script, left alone) and the page of its tab: the whole message (t, s, f, o, r, p). Worker thread. */
    private val seekTaps: (JSONObject) -> Unit = {},
    /** VC1: videobar.js reports what Kestrel's own video bar did (e = shown / own / act, a = play, pause, seek, mute, full) and the page of its tab. Worker thread. */
    private val videoBar: (JSONObject) -> Unit = {},
    /** RX3-BK2: a page the engine loaded again for Back / Forward asks where the owner left it (the page's address). The answer is the place in CSS pixels as text, or "" when nothing is armed for that page. Worker thread: reads only immutable values. */
    private val backPlace: (String) -> String = { "" },
    /** RX3-BK1: a Back / Forward show: the page's address, kept by the engine's cache, the navigation type, the bytes its file cost (0 = from cache), the place at the show. Worker thread. */
    private val backShown: (String, Boolean, String, Long, Int) -> Unit = { _, _, _, _, _ -> },
    /** RX3-BK1: the main response of this page said Cache-Control: no-store, so no cache may keep it. Worker thread. */
    private val noStore: (String) -> Unit = {},
    /** RX3-BK1: the page that was just left still had this many requests open (the page's address, the count). Worker thread. */
    private val leftPending: (String, Int) -> Unit = { _, _ -> },
    /** RX4-M0a: how a playing video did since the page's last look: frames shown (t), dropped (d), corrupt (c), seconds (s), picture width and height (w, h) and the page of its tab (p). Numbers only. Worker thread. */
    private val videoQuality: (JSONObject) -> Unit = {},
    /** RX4-M0e: one scroll burst of a real page (only while the Settings switch is on): frames (n), gaps over 50 ms (a), over 100 ms (b), the longest gap in ms (mx) and the page of its tab (p). Worker thread. */
    private val scrollGaps: (JSONObject) -> Unit = {},
    /** LAB2 (owner message 45): is this a DEBUG build (the emulator lab)? Then pages are asked to send a page-health probe (page-config flag 256). Release builds: always false, the script does nothing. */
    private val labBuild: () -> Boolean = { false },
    /** LAB2: the page-health probe of a page (numbers only: text length, images, broken images, frames, videos, size, nodes). Worker thread; only ever sent by a debug build. */
    private val labProbe: (JSONObject) -> Unit = {},
    /** FB4: the viewport a full-screen page really got (turn.js, top frame): width (w) and height (h) in device pixels, f = 1, and the page of its tab (p). Numbers only. Worker thread. */
    private val viewport: (JSONObject) -> Unit = {},
    /** VP1: the page's report of a full-screen film (`vst`) or a tap on its picture (`vtap`): the answer is what the controls want done (JSON). Worker thread. */
    private val player: (JSONObject) -> String = { "" },
    /** ANS52g: the video format policy the pages are told (core/media/VideoLimits.policy: the owner's two settings and the hardware limits of this device); the 5th field of every page answer. Worker thread, cheap. */
    private val videoPolicy: () -> String = { "" },
    /** ANS52h: the picture size cap for HLS master playlists in pixels of the longer side (core/media/PictureCap; 0 = none); the 6th field of every page answer. Worker thread, cheap. */
    private val pictureCap: () -> Int = { 0 },
    /** ANS54e: the page's gate asks whether Kestrel's own player takes the film the person pressed (`vh`: y = play | press | kestrel, s = the element's address, b = a blob: source, ... p = the page, f = the frame): true = it was started, keep the page's element closed. UI thread (the answer waits for it). */
    private val handOver: (JSONObject) -> Boolean = { false },
    /** NM1: what the page says about WHICH video is on (names.js: show, season, episode, episode title) and the page of its tab (p). Text only, cut by the app. Worker thread. */
    private val nameClues: (JSONObject) -> Unit = {},
) : WebExtension.MessageDelegate {

    // GeckoView delivers native messages on the Android UI thread and Gecko HOLDS each request until the reply arrives: any work here freezes painting and typing.
    // So onMessage only hands the message to ONE worker and returns a GeckoResult at once; the worker parses, decides and completes it (research/FREEZE.md 4.2).
    // NORMAL priority on purpose: Java priority 4 (NORM_PRIORITY - 1) is nice 10 and Android moves such a thread into the BACKGROUND scheduler group, where it competes with a
    // fraction of one core while Gecko holds every request until this thread answers (research/HW_SPEED.md H1).
    // A HandlerThread (not an executor) so the GeckoResult can be created WITH its handler: its callbacks (the reply to Gecko) then run on this thread instead of being posted back to the
    // Android UI thread (the third UI task of every request; sendSuccess / sendError are dispatched to Gecko's own thread by the engine, any thread may call them).
    @Volatile private var lastExtState = ""
    private val workerThread = android.os.HandlerThread("kestrel-shield", android.os.Process.THREAD_PRIORITY_DEFAULT).also { it.start() }
    private val workerHandler = android.os.Handler(workerThread.looper)
    // Element-hiding CSS is slow the first time a host is seen (the generic selector set is built once, about 2 s on the emulator): it must not hold up the verdicts queued behind it.
    private val cosmeticThread = android.os.HandlerThread("kestrel-cosmetic", android.os.Process.THREAD_PRIORITY_DEFAULT).also { it.start() }
    private val cosmeticHandler = android.os.Handler(cosmeticThread.looper)
    // The engine image (D1) goes to the extension in chunks right after a cold start and the first build of it takes a while: on its own thread the first `css` answer of the first page
    // (and the `gen` batches after it) never queue behind those chunks (AA3-3b; seen on the emulator as a delayed first hiding after a cold start).
    private val imageThread = android.os.HandlerThread("kestrel-image", android.os.Process.THREAD_PRIORITY_DEFAULT).also { it.start() }
    private val imageHandler = android.os.Handler(imageThread.looper)

    override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
        // Only OUR extension's background page may ask (content scripts relay through it with runtime.sendMessage; web pages cannot reach native messaging at all).
        if (nativeApp != "kestrel" || sender.webExtension.id != EXTENSION_ID || sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) return null
        val raw = message
        val handler = handlerFor(raw)
        val result = GeckoResult<Any>(handler)
        handler.post {
            val early = try { parse(raw) } catch (e: Exception) { null }
            if (early != null && early.optString("k") == "vh") {          // ANS54e: decided on the UI thread (the tabs and the sniffed streams live there); the engine's request waits for the answer, the page script gives up after 700 ms
                Io.ui { val ok = try { handOver(early) } catch (e: Exception) { Log.w(Io.TAG, "hand over", e); false }; result.complete(if (ok) "{\"h\":1}" else "{\"h\":0}") }
                return@post
            }
            val reply: Any = try {
                val m = early ?: parse(raw)                                // (one parse per message: this is the lane of every request the blocker judges)
                if (m == null) "" else when (m.optString("k")) {
                    "req" -> if (shouldBlock(m.optString("u"), m.optString("t"), m.optString("p"))) "1" else "0"
                    "cfg" -> pageConfig(m.optString("h"))
                    "img" -> imageChunk(m.optInt("g"), m.optInt("i", -1))
                    "cnt" -> { counts(m); "" }
                    "st" -> { stats = EngineStats.from(m); m.optString("w").let { w -> if (w != lastExtState) { lastExtState = w; app.kestrel.browser.diag.Health.log("shields", "ext-state", w) } }; "" }
                    "pop" -> { onPop(m); "" }
                    "sc" -> scriptletCalls(m.optString("h"))
                    "css" -> cosmeticCss(m.optString("h"), m.optBoolean("top", true))
                    "gen" -> genericCss(m.optString("h"), m.optJSONArray("t"))
                    "proc" -> proceduralRules(m.optString("h"))
                    "media" -> { onMedia(m); "" }
                    "maybe" -> { mediaMaybe(m); "" }
                    "gone" -> { m.optJSONArray("d")?.let { arr -> mediaGone(m.optString("p"), (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }) }; "" }
                    "vid" -> { onVid(m); "" }
                    "scroll" -> { pageScrolled(m.optString("p"), m.optInt("y")); "" }
                    "warm" -> { warmLink(m.optString("o"), m.optString("p"), m.optString("w")); "" }
                    "os" -> { openSpace(m.optString("p")); "" }
                    "yts" -> { adSkips(m); "" }
                    "sbk" -> { seekTaps(m); "" }
                    "vbk" -> { videoBar(m); "" }
                    "vq" -> { videoQuality(m); "" }
                    "sg" -> { scrollGaps(m); "" }
                    "probe" -> { if (labBuild()) labProbe(m); "" }
                    "vpt" -> { viewport(m); "" }
                    "vst", "vtap" -> player(m)
                    "bk" -> backPlace(m.optString("p"))
                    "bf" -> { backShown(m.optString("p"), m.optInt("ps") == 1, m.optString("nt"), m.optLong("ts"), m.optInt("y")); "" }
                    "ns" -> { noStore(m.optString("p")); "" }
                    "nm" -> { nameClues(m); "" }
                    "pend" -> { leftPending(m.optString("p"), m.optInt("n")); "" }
                    "rep" -> { onPageReport(m); "" }
                    else -> ""
                }
            } catch (e: Exception) {
                Log.w(Io.TAG, "shields bridge", e)
                ""
            } catch (e: OutOfMemoryError) { // a background lane must never take the whole browser down (the engine image goes out in 256 KB chunks; the lab run of 2026-11-15 died of this at a 16 MB heap): the extension asks again
                Log.w(Io.TAG, "shields bridge: out of memory")
                app.kestrel.browser.diag.Health.count("shields.oom")
                ""
            }
            result.complete(reply)
        }
        return result
    }

    /** Which thread answers a message: the engine image chunks, the slow element-hiding work and the page's quick verdicts each have their own, so none waits behind another. */
    private fun handlerFor(raw: Any): android.os.Handler = when (BridgeLane.of(raw)) {
        BridgeLane.IMAGE -> imageHandler
        BridgeLane.COSMETIC -> cosmeticHandler
        else -> workerHandler
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
        val cand = MediaCandidate(url, kind, MediaSniff.baseType(ct), len, page, m.optString("f").takeIf { it != page } ?: "", System.currentTimeMillis())
        cand.frameChain = app.kestrel.core.media.MediaNames.frameChainOf(m.optString("fa"))   // NM1: the frames around the player that asked (their addresses name the season and episode)
        cand.fromMarkup = m.optString("s") == "dom" // RX3-V2: found in the page's text, never requested: not counted for the badge until it is checked or played
        cand.referer = MediaHeaders.refererOf(m.optString("rf")); cand.origin = MediaHeaders.originOf(m.optString("og")) // what the browser itself sent: the player's own Referer / Origin (origins only)
        cand.sourceName = app.kestrel.core.download.FileNames.fromContentDisposition(m.optString("n").takeIf { it.isNotBlank() })?.let { app.kestrel.core.download.FileNames.sanitize(it) } ?: "" // the name the server gave it
        mediaSeen(cand)
    }

    private fun onVid(m: JSONObject) {
        val url = m.optString("u")
        if (url.isEmpty()) return
        val facts = if (m.has("f")) app.kestrel.core.media.VideoFacts.fromBits(m.optInt("f"), m.optInt("rw"), m.optInt("rh"), m.optInt("vp")) else null
        // RX3-V3: ev = "played": the element really played this file for 3 s; el = the element's number on its page (0 = not told)
        mediaInfo(m.optString("p"), url, m.optInt("w"), m.optInt("h"), m.optLong("d"), m.optString("l").takeIf { it.isNotBlank() }, facts, m.optString("ev") == "played", m.optInt("el"))
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
     * `cfg`: "<mode>:<generation>:<content id>:<flags>:<video policy>:<picture cap>" (flags: 1 = the pop-up and redirect guard applies to this site; the video policy is [app.kestrel.core.media.VideoLimits.policy], ANS52g; the picture cap is [app.kestrel.core.media.PictureCap.pixels], ANS52h). mode 1 = block on pages of this host, 0 = Shields off / site allowed, L = ask per request (compatibility mode).
     * While the lists are still loading (cold start) the answer is provisional - generation 0, no id, the site allow-list unknown - so the extension can already judge requests with the
     * copy it saved last time and asks again within seconds; before this, those first requests went unfiltered.
     */
    private fun pageConfig(host: String): String {
        val snap = Engine.snapshot()
        val gen = snap?.generation ?: 0
        if (host.isEmpty()) return "0:" + gen + "::0"
        val s = Shields.effective(prefs(), host)
        // flags for the page, a bit field: 1 = the pop-up and redirect guard applies (it follows the Shields master switch and its own setting), 2 = video and social embeds wait for a tap (EX-10),
        // 4 = harmless stand-ins may answer blocked files (`$redirect=` rules, AW1c; follows Shields and the Settings switch "Fix ad and anti-adblock scripts", like the scriptlets),
        // 8 = the ad skipper may run in video players (RX3-Y2; follows Shields and the Settings switch "Skip ads inside video players"),
        // 256 = a DEBUG build (the emulator lab): the page sends a page-health probe once it has loaded (LAB2; never set in a release build),
        // 64 = Kestrel's own video bar may show on a tapped video whose page has no controls, 128 = also where the page has some (VC1, videobar.js; follows ONLY the Settings row "Video controls"),
        // 512 = Kestrel's own controls over a FULL-SCREEN video (VP1, player.js; follows ONLY the Settings row "Full-screen video controls"),
        // 1024 = no video loads or starts before the person presses it (ANS53a / ANS53b, gate-main.js; follows ONLY the Settings switch "Videos start only when I press play": it is not a Shields feature, every site has it),
        // 2048 = the page's gate may ASK the app whether Kestrel's own player takes the film the person pressed (ANS54e, gate-main.js; needs the press-to-play rule and a Settings row "Web videos play in" other than "The page's own player"),
        // 16 = a tap on a video player's bar may move the video when the page does not (YS1, seekbar.js; follows ONLY the Settings switch "Tap the video bar to jump": it is not a Shields feature, a site with Shields off keeps it)
        val guard = (if (prefs().blockPopups && s.shieldsUp) 1 else 0) or (if (s.shieldsUp && s.waitEmbeds) 2 else 0) or (if (s.shieldsUp && prefs().shieldScripts) 4 else 0) or (if (s.shieldsUp && prefs().skipVideoAds) 8 else 0) or (if (prefs().tapSeek) 16 else 0) or (if (prefs().measureScroll) 32 else 0) or (if (prefs().fsControls == 0) 512 else 0) or (if (prefs().videoControls != 2) 64 else 0) or (if (prefs().videoControls == 1) 128 else 0) or (if (labBuild()) 256 else 0) or (if (prefs().videoPressToPlay) 1024 else 0) or (if (prefs().videoPressToPlay && prefs().webVideoMode != app.kestrel.core.media.WebHandOver.NEVER) 2048 else 0)
        val on = s.shieldsUp && s.blockAds && snap?.fs?.isPageAllowed(host) != true
        return (if (on) "1:" else "0:") + gen + ":" + (if (snap != null) contentId(snap) else "") + ":" + guard + ":" + videoPolicy() + ":" + pictureCap()
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

    /**
     * `sc` (AW1b+): "<list generation>:<json>" - the scriptlet calls of [host] for the extension to register BEFORE a document of that host loads, so they run ahead of the page's own scripts (the
     * copy that rides in the CSS answer arrives after the page has started and stays as the fallback). The json is empty when the host has no calls, the Settings switch or the site's Shields are off,
     * or the lists allow-list the page. Generation 0 = the lists are still loading: the extension remembers nothing about that answer.
     */
    private fun scriptletCalls(host: String): String {
        val snap = Engine.snapshot()
        val gen = snap?.generation ?: 0
        val fs = snap?.fs
        if (host.isEmpty() || fs == null) return "$gen:"
        val p = prefs()
        if (!p.shieldScripts) return "$gen:"
        val s = Shields.effective(p, host)
        if (!s.shieldsUp || !s.blockAds || fs.isPageAllowed(host)) return "$gen:"
        return "$gen:" + fs.scriptletPayload(host)
    }

    /** Element-hiding CSS for a frame of [host]. Only the top document gets the big generic set (up to 1,200 selectors); an iframe, usually an ad itself, gets the rules written for its own site. */
    private fun cosmeticCss(host: String, top: Boolean): String {
        if (host.isEmpty()) return ""
        val fs = Engine.peek() ?: return ""
        val p = prefs()
        val s = Shields.effective(p, host)
        if (!s.shieldsUp || !s.blockAds) return ""
        val scripts = p.shieldScripts // AW1b: the site's scriptlet calls ride in this answer (a comment after the markers); the Shields switches of the site and the master switch decide, element hiding being off does not
        if (!s.cosmetic) return if (scripts) fs.scriptletComment(host) else ""
        // D2: the generic selectors that name a class or an id are not sent up front; the page sends its classes and ids ([genericCss]) when this CSS starts with the marker
        val css = fs.cosmeticCss(host, includeGeneric = top, tokenMode = true, scriptlets = scripts)
        if (cssLogged < 40) { cssLogged++; app.kestrel.browser.diag.Health.log("cosmetic", "css", "$host top=$top len=${css.length} tokens=${css.startsWith(app.kestrel.core.filter.FilterSet.COSMETIC_TOKEN_MARKER)} scriptlets=${css.contains("/" + "*ks")}") }
        return css
    }
    private var cssLogged = 0

    /** AD1b-4: the procedural cosmetic rules of a host (`:has-text()`, `:upward()`, `:remove()` ...), one per line in the stored form of [app.kestrel.core.filter.FilterSet.proceduralFor]; the page script runs them. Same Shields switches as [cosmeticCss]. */
    private fun proceduralRules(host: String): String {
        if (host.isEmpty()) return ""
        val fs = Engine.peek() ?: return ""
        val s = Shields.effective(prefs(), host)
        if (!s.shieldsUp || !s.blockAds || !s.cosmetic) return ""
        return fs.proceduralFor(host).joinToString("\n")
    }

    /** D2: CSS for the generic selectors that can match a page holding these classes and ids (".class" / "#id"; at most [MAX_TOKENS] are looked at), under the same Shields switches as [cosmeticCss]. */
    private fun genericCss(host: String, tokens: org.json.JSONArray?): String {
        if (host.isEmpty() || tokens == null || tokens.length() == 0) return ""
        val fs = Engine.peek() ?: return ""
        val s = Shields.effective(prefs(), host)
        if (!s.shieldsUp || !s.blockAds || !s.cosmetic) return ""
        val n = minOf(tokens.length(), MAX_TOKENS)
        val list = ArrayList<String>(n)
        for (i in 0 until n) list.add(tokens.optString(i, ""))
        val css = fs.genericCssForTokens(host, list)
        if (genLogged < 40) { genLogged++; app.kestrel.browser.diag.Health.log("cosmetic", "gen", "$host tokens=$n css=${css.length}") } // the first batches of a session, for Diagnostics
        return css
    }
    private var genLogged = 0

    /** What the extension's engine reports about itself (`st`), shown in Diagnostics. */
    class EngineStats(
        val generation: Int, val checks: Long, val totalUs: Long, val maxUs: Long, val slow: Long, val perRequestAsks: Long, val perRequestUs: Long, val errors: Int,
        val loadMs: Long, val imageBytes: Int, val transfers: Int, val off: Boolean, val note: String, val at: Long,
        /** RX3-S2: how often each web request listener of the extension ran and how long it held the thread ("req 120 (5400 us), rcv 14 (200 us)"); empty until one ran. Its own field: the app logs `note` whenever it changes. */
        val listeners: String = "",
        /** RX4-M0b / M0c: revalidations (304), cache hits and the scriptlet wait before a page, as the extension counted them (text). */
        val revalidation: String = "",
        /** ANS52g: what the pages' video format policy refused (the extension's counts, text). */
        val videoFormats: String = "",
        /** ANS52h: what the picture size cap did to HLS master playlists (the extension's counts, text). */
        val pictureCap: String = "",
        /** ANS53a / ANS53b: what the page-side gate held back and opened (the extension's counts, text). */
        val startGate: String = "",
    ) {
        companion object {
            fun from(m: JSONObject) = EngineStats(
                m.optInt("g"), m.optLong("n"), m.optLong("us"), m.optLong("mx"), m.optLong("sl"), m.optLong("lg"), m.optLong("lu"), m.optInt("er"),
                m.optLong("ld"), m.optInt("by"), m.optInt("pl"), m.optInt("off") != 0, m.optString("w").take(400), System.currentTimeMillis(), m.optString("lt").take(240), m.optString("rv").take(320), m.optString("vx").take(240), m.optString("hx").take(300), m.optString("vg").take(300),
            )
        }
    }

    /** The extension's page report (Diagnostics > Page report): kept only for a page a normal tab shows, never for a private one. */
    private fun onPageReport(m: JSONObject) {
        val page = m.optString("p")
        if (page.isEmpty() || tabForPage(page) == null || pageIsPrivate(page)) return
        fun list(k: String): List<String> { val a = m.optJSONArray(k) ?: return emptyList(); val out = ArrayList<String>(a.length()); for (i in 0 until minOf(a.length(), 40)) out.add(a.optString(i).take(200)); return out }
        pageReports.add(app.kestrel.core.diag.PageReport(page.take(200), m.optInt("n"), m.optInt("nb"), list("b"), list("f"), list("e"), System.currentTimeMillis()))
    }

    companion object {
        const val EXTENSION_ID = "shields@kestrel.local"
        /** The last pages' reports for Diagnostics (see [app.kestrel.core.diag.PageReports]). */
        val pageReports = app.kestrel.core.diag.PageReports()
        /** The most classes / ids of one batch the app looks at (the page script sends at most this many per message). */
        const val MAX_TOKENS = 400

        /** The latest numbers from the extension's engine (null until it has judged a request or finished a transfer). */
        @Volatile var stats: EngineStats? = null

        /** One line for Diagnostics: where the blocking decisions are made and how long they take. */
        fun statsLine(): String {
            val s = stats ?: return "Blocker: fast mode (decisions inside the page engine). No numbers yet: open a page, then press Update."
            val avg = if (s.checks > 0) s.totalUs / s.checks else 0L
            val state = when {
                s.off -> "OFF this session (too many errors)"
                s.generation > 0 && s.imageBytes > 0 && s.transfers == 0 -> "list #${s.generation}, ${s.imageBytes / 1024} KB, engine ready from the saved copy in ${s.loadMs} ms (no transfer from the app)" // RX4-M0d
                s.generation > 0 && s.imageBytes > 0 -> "list #${s.generation}, ${s.imageBytes / 1024} KB, loaded in ${s.loadMs} ms, ${s.transfers} transfer(s)"
                else -> "list not loaded yet"
            }
            return "Blocker: fast mode, $state. ${s.checks} requests judged in the engine (average ${avg} us, slowest ${s.maxUs} us, ${s.slow} over 2 ms), ${s.perRequestAsks} asked the app one by one${if (s.perRequestAsks > 0) " (each held ${s.perRequestUs / s.perRequestAsks} us on average)" else ""}, ${s.errors} errors.${if (s.listeners.isNotEmpty()) " Extension listeners (calls, time held): ${s.listeners}." else ""}${if (s.revalidation.isNotEmpty()) " Revalidations and waits: ${s.revalidation}." else ""} ${s.note}".trim()
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
