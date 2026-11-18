package app.kestrel.browser.web

import android.app.AlertDialog
import app.kestrel.browser.BrowserActivity
import app.kestrel.browser.Io
import app.kestrel.browser.NativePlayerActivity
import app.kestrel.browser.diag.Health
import app.kestrel.browser.ui.VideoPanel
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.core.media.AdRuns
import app.kestrel.core.media.Dash
import app.kestrel.core.media.Hls
import app.kestrel.core.media.MediaCandidate
import app.kestrel.core.media.MediaHeaders
import app.kestrel.core.media.MediaKind
import app.kestrel.core.media.MediaList
import app.kestrel.core.media.MediaLabels
import app.kestrel.core.media.MediaNames
import app.kestrel.core.media.MediaRank
import app.kestrel.core.media.MediaSniff
import app.kestrel.core.media.Mp4Probe
import app.kestrel.core.media.PageQuality
import app.kestrel.core.media.WebHandOver
import org.json.JSONObject
import android.os.SystemClock
import app.kestrel.core.media.QualityLabel
import app.kestrel.core.media.SizeEstimate
import app.kestrel.core.media.StreamSource
import app.kestrel.core.url.UrlParts

/**
 * "Download video": what the page's sniffed media becomes in the UI. The extension reports every media file / playlist a page loads ([seen]), the page's own players report what they
 * know about them ([info]); a long press on a video or the menu entry opens the sheet ([show]) and a tap on a row starts an ordinary download, so pause / resume / notifications / the
 * metered-link questions are the ones every download already has. Design and limits: research/VIDEO_DOWNLOADER.md.
 */
class VideoFlow(private val a: BrowserActivity) {
    /** One quality of an HLS stream, ready to download. */
    private companion object { const val LOOKS_PER_PAGE = 6 }
    private class StreamPick(val label: String, val url: String, val text: String?, val estimate: Long, val audioUrl: String?, val audioOnly: Boolean, val container: String, val repId: String? = null, val audioRepId: String? = null, val ext: String? = null, val headers: Map<String, String>? = null, val cand: MediaCandidate? = null)
    private sealed class Loaded {
        object Loading : Loaded()
        class Failed(val message: String) : Loaded()
        class Ready(val rows: List<Pair<VideoPanel.Choice, StreamPick>>, val note: String?) : Loaded()
    }
    private val streamInfo = HashMap<String, Loaded>()   // candidate key -> what its playlist turned out to be
    private val probed = HashSet<String>()              // files whose first bytes were already read to learn their picture size
    private val sized = HashSet<String>()               // files whose length was already asked for (found in the page markup, never requested)
    private val children = HashSet<String>()            // addresses of the variant / audio playlists of masters we have read: they are not rows of their own
    private val childUrls = HashMap<String, List<String>>() // master key -> its children (forgotten together with it)
    private val masterRungs = HashMap<String, List<QualityLabel.Rung>>() // ANS53d.2: master key -> the pictures its playlist offers (the quality list of a film that plays in the page)
    private val masterAsked = HashSet<String>()          // ANS53d.2: masters whose pictures were already asked for
    private var panel: VideoPanel? = null
    private var panelTab: Tab? = null
    private var panelPressed: String? = null
    private var panelExtras: List<Pair<String, () -> Unit>> = emptyList()

    /**
     * The tab a sniffed address belongs to (owner 2026-11-13: "only include downloadable videos for the page I am currently viewing"): the selected tab when it is on exactly that page (two tabs on one page:
     * the one being looked at), else the first tab on it, else a tab on that site. NEVER simply "the tab in front": a late answer of a page the tab has already left is not news for the page it is on now.
     */
    private fun tabOf(page: String): Tab? {
        if (page.isEmpty()) return a.current
        val u = page.substringBefore('#')
        return a.current?.takeIf { it.url.substringBefore('#') == u } ?: a.tabWithUrl(page) ?: a.tabForPage(page)
    }

    /** What the tab's list held is gone (a new page): the list is emptied and everything learned about its entries is forgotten, so the same address on the next page is read afresh. UI thread. */
    fun clearTab(tab: Tab) { tab.gecko.pictureW = 0; tab.gecko.pictureH = 0; tab.gecko.picturePlayed = false; val keys = tab.media.keys(); tab.media.clear(); forget(keys); looks.remove(tab.id); val p = tab.id.toString() + "|"; maybeAsked.removeAll { it.startsWith(p) }; if (lastRefused?.tabId == tab.id) lastRefused = null }

    private fun forget(keys: List<String>) {
        for (k in keys) { lookedKeys.remove(k); streamInfo.remove(k); probed.remove(k); sized.remove(k); masterRungs.remove(k); masterAsked.remove(k); childUrls.remove(k)?.forEach { children.remove(it) } }
    }

    /**
     * The player frame of the page went to another document (the owner switched servers): what that document offered is removed and the sheet, if it is open, shows what is left. Called on the Shields
     * worker thread. An advertisement in the same player, or an ad frame of its own, never comes here: the extension only says "gone" for a frame that is REPLACED.
     */
    fun gone(page: String, docs: List<String>) = Io.ui {
        val t = tabOf(page) ?: run { Health.log("media", "frame-gone-no-tab", "docs=${docs.size}"); return@ui }
        val keys = t.media.dropFrames(docs)
        // those documents are gone: an address asked about in one of them (even one that turned out not to be media) is news again when a new document asks for it
        val p = t.id.toString() + "|"
        maybeAsked.removeAll { k -> docs.any { d -> k.startsWith("$p$d|") } }
        Health.log("media", "frame-gone", "docs=${docs.joinToString(",") { it.substringAfter("://").substringBefore('?').take(40) }} dropped=${keys.size} left=${t.media.keys().size} [${t.media.describe()}]") // always, so a report shows whether the extension's message arrived and what stayed
        if (keys.isEmpty()) return@ui
        forget(keys)
        Health.log("media", "server-switch", "dropped=${keys.size} frames=${docs.size}")
        a.refreshDownloadButton(); refresh(t)
    }

    /** Called on the Shields worker thread. */
    fun seen(c: MediaCandidate) = Io.ui {
        val t = tabOf(c.page) ?: return@ui
        if (t.media.add(c)) { a.refreshDownloadButton(); if (c.fromMarkup) verifySoon(t); if (c.kind == MediaKind.HLS) lookSoon(t) }
    }

    // ---- RX3-V5 (research/FINDER_LAB.md section 3): the early look. A stream playlist the page asked for is read at idle (a few KB: the master and its lowest quality) so that live, protected and clip-sized streams
    // stop counting for the badge and the advertising the playlist gives away is known before anybody opens the sheet. The first few playlists of a page only; never while the page loads.
    private val lookPending = HashSet<Long>()
    private val looks = HashMap<Long, Int>()                 // tab id -> playlists looked at on this page
    private val lookedKeys = HashSet<String>()
    private fun lookSoon(t: Tab) {
        if ((looks[t.id] ?: 0) >= LOOKS_PER_PAGE || !lookPending.add(t.id)) return
        Io.main.postDelayed({ lookNow(t, 0) }, 3_000)
    }
    private fun lookNow(t: Tab, tries: Int) {
        if (t.loading && tries < 6) { Io.main.postDelayed({ lookNow(t, tries + 1) }, 2_000); return }
        lookPending.remove(t.id)
        if (t.isNewTab || t.loading) return
        for (c in t.media.streamsToLook(3)) {
            val n = looks[t.id] ?: 0
            if (n >= LOOKS_PER_PAGE) break
            if ((c.kind != MediaKind.HLS && c.kind != MediaKind.DASH) || !lookedKeys.add(c.key)) continue
            looks[t.id] = n + 1
            if (c.kind == MediaKind.DASH) { // RX3-V4: a DASH manifest says its length and its picture heights in its first bytes
                fetchPlaylist(t, c, c.url, { Dash.parse(it, c.url) != null }) { text, _ ->
                    val m = text?.let { Dash.parse(it, c.url) }
                    if (m != null) noteStream(t, c, m.problem, m.durationMs, 0L, 0, m.video.map { it.height }.filter { it > 0 }.distinct().toIntArray())
                }
                continue
            }
            fetchPlaylist(t, c, c.url, { Hls.parse(it, c.url) != null }) { text, _ ->
                when (val pl = text?.let { Hls.parse(it, c.url) }) {
                    is Hls.MediaPlaylist -> noteLook(t, c, pl.media, IntArray(0))
                    is Hls.MasterPlaylist -> {
                        c.master = true
                        noteMaster(c, pl.master)
                        val vs = pl.master.variants
                        val heights = vs.filter { !it.audioOnly }.map { it.height }.filter { it > 0 }.distinct().toIntArray()   // RX3-V4: what a blob: player's picture height is matched against
                        val probe = vs.filter { !it.audioOnly }.minByOrNull { it.bandwidth } ?: vs.minByOrNull { it.bandwidth }
                        if (probe != null) DownloadController.fetchText(a, probe.url, requestHeaders(t, c), 1, t.isPrivateTab) { txt -> (txt?.let { Hls.parse(it, probe.url) } as? Hls.MediaPlaylist)?.media?.let { noteLook(t, c, it, heights) } }
                    }
                    null -> {}
                }
            }
        }
    }
    private fun noteLook(t: Tab, c: MediaCandidate, media: Hls.Media, heights: IntArray) {
        val ads = AdRuns.find(media)
        noteStream(t, c, media.problem, media.durationMs, ads.skippedMs, ads.breaks, heights)
    }
    private fun noteStream(t: Tab, c: MediaCandidate, problem: Hls.Problem?, durationMs: Long, adMs: Long, breaks: Int, heights: IntArray) {
        val live = problem == Hls.Problem.LIVE
        val drm = problem == Hls.Problem.DRM || problem == Hls.Problem.SAMPLE_AES
        if (t.media.applyStream(c.url, live, drm, durationMs, adMs, breaks, heights)) { a.refreshDownloadButton(); refresh(t) }
        if (!drm && durationMs >= WebHandOver.FILM_MS) lateHandOver(t, c)       // ANS54e: a film-length stream the sniffer saw right after a press that was refused for want of one
        Health.log("media", "look", "${c.kind} live=$live drm=$drm length=${durationMs / 1000}s heights=${heights.size} advertising=${adMs / 1000}s in $breaks breaks counted=${t.media.usableCount()} of ${t.media.size} playing=${t.media.primary()?.let { (it.durationMs / 1000).toString() + "s" } ?: "none"}")
    }

    // ---- RX3-V2 (research/FINDER_LAB.md section 2): an address found only in the page's text was never requested; on a page with nothing verified, up to three of them get ONE cheap question at idle
    private val verifyPending = HashSet<Long>()
    private fun verifySoon(t: Tab) {
        if (!verifyPending.add(t.id)) return
        Io.main.postDelayed({ verifyNow(t, 0) }, 2_500)
    }
    /** "How big are you?" as a one-byte Range request (the answer is the end of Content-Range). Never while the page is still loading, never for a tab that moved on, at most three per check. */
    private fun verifyNow(t: Tab, tries: Int) {
        if (t.loading && tries < 6) { Io.main.postDelayed({ verifyNow(t, tries + 1) }, 2_000); return }
        verifyPending.remove(t.id)
        if (t.isNewTab || t.loading) return
        var asked = 0
        for (c in t.media.unverifiedToCheck(3)) {
            if (c.length >= 0 || !sized.add(c.key)) continue
            asked++
            DownloadController.fetchBytes(a, c.url, 0L, 0L, requestHeaders(t, c), 1, t.isPrivateTab, onTotal = { n -> if (t.media.setLength(c.url, n)) { a.refreshDownloadButton(); refresh(t); Health.log("media", "verified", "length=${n / 1024} KB counted=${t.media.usableCount()} of ${t.media.size}") } }) { }
        }
        if (asked > 0) Health.log("media", "verify", "asked=$asked counted=${t.media.usableCount()} of ${t.media.size}")
    }

    /** "tab id|document|address" already read, so a player that asks for the same playlist again does not make us read it again. Per DOCUMENT: another server (another document) that asks for the same address is news ([gone] forgets the replaced document's entries, [clearTab] a tab's). */
    private val maybeAsked = HashSet<String>()

    /**
     * The extension saw a response that says nothing about being media (text/plain, octet-stream, a "picture" or "stylesheet" a script fetched): stream sites disguise their playlists that way. Read its first
     * bytes with the headers the browser sent (then the other guesses) and offer it when it is a playlist or a video. At most twelve per page come here (the extension rations them). Worker thread.
     */
    fun maybe(m: org.json.JSONObject) = Io.ui {
        val url = m.optString("u"); val page = m.optString("p")
        if (!(url.startsWith("http://", true) || url.startsWith("https://", true))) return@ui
        val t = tabOf(page) ?: return@ui
        if (t.isNewTab || t.media.find(url) != null) return@ui
        val frame = m.optString("f").takeIf { it != page } ?: ""
        if (maybeAsked.size > 500) maybeAsked.clear()
        if (!maybeAsked.add(t.id.toString() + "|" + frame + "|" + MediaCandidate.keyOf(url))) return@ui
        val seed = MediaCandidate(url, MediaKind.FILE, "", m.optLong("l", -1L), page, frame, System.currentTimeMillis())
        seed.referer = MediaHeaders.refererOf(m.optString("rf")); seed.origin = MediaHeaders.originOf(m.optString("og"))
        val sets = MediaHeaders.sets(seed, t.url)
        val host = UrlParts.hostOf(url) ?: ""
        Health.log("media", "maybe", "$host type=${m.optString("c").substringBefore(';').take(32)} len=${seed.length} kind=${m.optString("ty")}")
        fun attempt(i: Int, last: Int) {
            if (i >= sets.size) { Health.log("media", "maybe-refused", "$host http=$last"); return }
            var status = -1
            DownloadController.fetchBytes(a, url, 0L, 4095L, sets[i], 1, t.isPrivateTab, onStatus = { status = it }) { bytes ->
                if (bytes == null) { if (status == 401 || status == 403 || status == -1) attempt(i + 1, status.takeIf { it > 0 } ?: last) else Health.log("media", "maybe-refused", "$host http=$status"); return@fetchBytes }
                val s = MediaSniff.classifyBytes(bytes)
                if (s == null) { Health.log("media", "maybe-no", "$host first=${bytes.size}"); return@fetchBytes }
                val c = MediaCandidate(url, s.kind, s.mime, if (s.kind == MediaKind.FILE) seed.length else -1L, page, frame, System.currentTimeMillis())
                c.referer = seed.referer; c.origin = seed.origin; c.viaHeaders = sets[i]
                Health.log("media", "maybe-hit", "$host ${s.kind} ext=${s.ext} set=${i + 1}/${sets.size}")
                if (t.media.add(c)) a.refreshDownloadButton()
            }
        }
        attempt(0, 0)
    }

    /** Called on the Shields worker thread. */
    fun info(page: String, url: String, width: Int, height: Int, durationMs: Long, label: String?, facts: app.kestrel.core.media.VideoFacts? = null, played: Boolean = false, element: Int = 0) = Io.ui {
        val t = tabOf(page) ?: return@ui
        if (width > 0 && height > 0 && (played || !t.gecko.picturePlayed) && (played || t.gecko.pictureW == 0 || width.toLong() * height >= t.gecko.pictureW.toLong() * t.gecko.pictureH)) { // FS1: what shape the picture is decides whether full screen starts sideways
            val first = t.gecko.pictureW == 0
            t.gecko.pictureW = width; t.gecko.pictureH = height; if (played) t.gecko.picturePlayed = true
            if (first || played) a.fullscreenPictureKnown()
        }
        // RX3-V1: a hover preview, a hero loop or an ad that loaded its metadata is not "a video the owner may want": the page's own element says what it is for
        val role = if (facts == null) null else app.kestrel.core.media.VideoRoleRules.decide(facts, durationMs)
        val area = if (facts == null) 0L else facts.widthPx.toLong() * facts.heightPx * facts.viewPercent / 100   // the part of its picture that is on screen
        if (url == MediaList.BLOB) { // RX3-V4: a player that feeds a blob: address says only its length and picture height: matched to the playlist of its length (now, or when that playlist has been read)
            val changed = t.media.applyBlob(height, durationMs, role, played, element, area)
            Health.log("video", "info-blob", "${if (played) "PLAYED " else ""}el=$element role=${role ?: "none"} length=${durationMs / 1000}s h=$height matched=${t.media.matchedBy(element)} counted=${t.media.usableCount()} of ${t.media.size}")
            a.refreshDownloadButton()
            if (changed) refresh(t)
            return@ui
        }
        t.media.applyInfo(url, width, height, durationMs, label, role, played, element, area)
        app.kestrel.browser.diag.Health.log("video", "info", "${if (played) "PLAYED " else ""}el=$element role=${role ?: "none"} length=${durationMs / 1000}s counted=${t.media.usableCount()} of ${t.media.size}")   // no address
        a.refreshDownloadButton()
    }

    /** Anything worth offering on this tab's page? (the menu shows its entry only then) */
    fun available(t: Tab?): Boolean = t != null && !a.onHomeScreen(t) && t.media.usableCount() > 0   // ANS53e: not under the Home screen either

    /**
     * A long press on a video or audio element goes straight to the sheet, aimed at THAT file: its own address (when the page gave one) is the exact match, everything else the page loaded
     * is ranked behind it (MediaRank). "Copy video address" and "Open link" are small buttons under the Download button instead of a menu in front of it.
     */
    fun longPress(tab: Tab, srcUri: String?, linkUri: String?, onLink: (String) -> Unit) {
        val direct = srcUri?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        // The element's own address is always worth offering, even when the sniffer has not seen the request (cached, or it ran before the extension was ready).
        if (direct != null && tab.media.find(direct) == null) tab.media.add(MediaCandidate(direct, MediaKind.FILE, "", -1, tab.url, "", System.currentTimeMillis()))
        if (direct != null) tab.media.touch(direct) // the owner pressed it: it is kept and shown whatever its size (MediaList, EY-17)
        val extras = ArrayList<Pair<String, () -> Unit>>()
        if (direct != null) extras.add("Copy video address" to { a.copyText(direct) })
        if (linkUri != null) extras.add("Open link in new tab" to { a.sheet.dismiss(); onLink(linkUri) })
        show(tab, direct, extras, fromPress = true)
    }

    /** The sheet. [pressedUrl]: the address of the element the owner long-pressed, when known. [fromPress]: it opened because of a long press on a video (the words of the empty answer say so). */
    fun show(tab: Tab, pressedUrl: String? = null, extras: List<Pair<String, () -> Unit>> = emptyList(), fromPress: Boolean = false) {
        val ranked = MediaRank.rank(candidates(tab, pressedUrl), pressedUrl, tab.media.primary())
        if (ranked.main.isEmpty() && ranked.other.isEmpty()) {
            if (!tab.isPrivateTab) Health.log("video", "sheet-empty")
            a.toast(if (fromPress) "No video file found on this page yet. Some sites only show it once the video plays: press play, then long press the video again." else "No video found on this page yet. Some sites only show it once you press play: press play, then tap this icon again.")
            return
        }
        val p = VideoPanel(a, a.pal, object : VideoPanel.Listener {
            override fun download(choice: VideoPanel.Choice, stem: String) { when (val t = choice.tag) { is MediaCandidate -> pick(tab, t, stem); is StreamPick -> pickStream(tab, t, stem) } }
            override fun canPlay(choice: VideoPanel.Choice): Boolean = nativeTarget(choice) != null
            override fun play(choice: VideoPanel.Choice) { playNative(tab, choice) }
        })
        panel = p; panelTab = tab; panelPressed = pressedUrl; panelExtras = extras
        render(p, tab)
        a.sheet.show("Save video", p)
        if (!tab.isPrivateTab) Health.log("video", "sheet-shown", "main=${ranked.main.size} other=${ranked.other.size}")
        for (c in ranked.main + listOfNotNull(ranked.exact)) { // only what is on top is looked into: a few hundred KB each, never the files tucked away
            if (c.kind == MediaKind.FILE && !c.audioOnly && (c.knownHeight == 0 || c.durationMs == 0L) && looksMp4(c) && probed.add(c.key)) probe(tab, c, 0L, 256 * 1024, 0)
            // a file found in the page's own markup has not been requested yet, so nobody has seen its length: one byte asked for gives it back as "Content-Range: bytes 0-0/TOTAL"
            else if (c.kind == MediaKind.FILE && c.length <= 0 && sized.add(c.key)) DownloadController.fetchBytes(a, c.url, 0L, 0L, requestHeaders(tab, c), 1, tab.isPrivateTab, onTotal = { n -> if (c.length <= 0) { tab.media.setLength(c.url, n); a.refreshDownloadButton(); refresh(tab) } }) { }
        }
    }

    // ---- ANS52i (research/VIDEO_ACCELERATION.md plan item 6): "Play in Kestrel's player": the film's own address goes to NativePlayerActivity (Media3 on a SurfaceView, the hardware decoder), the page is left alone and stops
    // with its screen. What can be handed over: a plain video file, or an HLS stream (its master playlist: the player picks the quality, capped at the row's height, 720 p when the row has none). Never DASH (no module in the
    // APK), never audio only, never what has no address (an MSE blob): those rows have no second button.
    private class NativeTarget(val url: String, val cand: MediaCandidate?, val maxH: Int, val w: Int, val h: Int)
    private val PLAYABLE_FILES = setOf("mp4", "m4v", "mov", "webm", "mkv")

    private fun nativeTarget(choice: VideoPanel.Choice): NativeTarget? {
        return when (val t = choice.tag) {
            is MediaCandidate -> {
                if (t.kind != MediaKind.FILE || t.audioOnly) return null
                val ext = MediaSniff.extension(t.url)
                if (ext !in PLAYABLE_FILES && !t.mime.startsWith("video/")) return null
                NativeTarget(t.url, t, 1080, t.width, t.height)
            }
            is StreamPick -> {
                val c = t.cand ?: return null
                if (c.kind != MediaKind.HLS || t.audioOnly) return null
                val h = Regex("(\\d{3,4})\\s*p", RegexOption.IGNORE_CASE).find(t.label)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                NativeTarget(c.url, c, if (h > 0) h else 720, 0, h)
            }
            else -> null
        }
    }

    private fun playNative(tab: Tab, choice: VideoPanel.Choice) {
        val target = nativeTarget(choice) ?: return
        val headers = target.cand?.let { MediaHeaders.sets(it, tab.url).firstOrNull() }?.map { "${it.key}: ${it.value}" }.orEmpty()
        try { tab.gecko.mediaSession?.pause() } catch (e: Throwable) { /* the page's own film may not have a media session (it is silent): then nothing is playing sound */ }
        a.sheet.dismiss()
        if (!tab.isPrivateTab) Health.log("video", "native-play", "${if (target.cand?.kind == MediaKind.HLS) "HLS" else "file"} cap ${target.maxH}")
        NativePlayerActivity.launch(a, target.url, headers, 0L, target.maxH, tab.isPrivateTab, target.w, target.h)
    }

    // ---- ANS53d.2 (owner message 53: "put a setting for video quality in the player controls for as many videos as possible"; research/ANS53_DESIGN.md): the quality list of a film that plays in the PAGE. The page's own
    // player cannot be told to change its picture, so the list is a hand-over to Kestrel's own player, which can: the pictures the film's HLS master playlist offers, a pick starts it at the page's position with that picture.
    class PageFilm(val cand: MediaCandidate, val rungs: List<QualityLabel.Rung>)

    /** A master playlist was read: remember the pictures it offers; the Quality button of an open full screen follows. */
    private fun noteMaster(c: MediaCandidate, master: Hls.Master) {
        val r = PageQuality.rungs(master.variants)
        if (masterRungs.put(c.key, r) != r) a.refreshPlayerQuality()
    }

    /**
     * The film the page plays and the pictures it offers; null when no film is known, it is no HLS stream, or the stream offers one picture only (nothing to choose). The film is the one that really plays
     * ([MediaList.primary]), else the longest counted HLS stream of the page. A playlist the app has not read yet is asked for once (the early look reads the first few, a page with more may not get to it). UI thread.
     */
    fun pageFilm(tab: Tab): PageFilm? {
        if (tab.isNewTab || a.onHomeScreen(tab)) return null
        val c = tab.media.primary()?.takeIf { it.kind == MediaKind.HLS } ?: tab.media.usable().filter { it.kind == MediaKind.HLS }.maxByOrNull { it.durationMs } ?: return null   // (clip-sized streams are not counted: the longest counted one is the film)
        val r = masterRungs[c.key]
        if (r == null) {
            if (masterAsked.add(c.key)) fetchPlaylist(tab, c, c.url, { Hls.parse(it, c.url) != null }) { text, _ -> (text?.let { Hls.parse(it, c.url) } as? Hls.MasterPlaylist)?.let { noteMaster(c, it.master) } }
            return null
        }
        return if (PageQuality.hasChoice(r)) PageFilm(c, r) else null
    }

    // ---- ANS54e (owner message 54: "I want the web video players to use this"; research/ANS54_DESIGN.md): the film a page is about to play is played by Kestrel's own player when it can. The page's gate asks (`vh`: the
    // element, what the person did), WebHandOver (core, tested) decides, and a yes starts NativePlayerActivity; the page keeps its element closed. Catch-alls: a film that went full screen in the page, and a playlist the
    // sniffer sees within 6 s after a press that had to be refused for want of a stream. UI thread.
    private class Refused(val tabId: Long, val page: String, val at: Long, val frame: String, val el: WebHandOver.Element)   // (the page too: a link tapped a moment later is not the page whose press was refused)
    private var lastRefused: Refused? = null

    private fun elementOf(m: JSONObject) = WebHandOver.Element(m.optString("s"), m.optInt("b") == 1, m.optString("m"), m.optInt("l") == 1, m.optInt("w"), m.optInt("h"), m.optDouble("t", 0.0), m.optDouble("d", 0.0), m.optInt("a") == 1)

    /** The streams the sniffer saw in [frame] ("" = the page itself) that a hand-over may use: playlists of films (the variants listed in a master are no films of their own), not DRM, not flagged as decoys. */
    private fun knownIn(tab: Tab, frame: String): List<WebHandOver.Known> =
        tab.media.usable().filter { it.frame == frame && it.kind == MediaKind.HLS && it.url !in children }.map {
            // ANS55: a MASTER list is the film (the early look read it as one; before that the address may say master), a bare list is one rendition (the picture, the sound, the subtitles: Brightcove / Yahoo)
            WebHandOver.Known(it.url, it.kind, it.durationMs, !it.drm && !it.decoy, it.master || (!it.looked && WebHandOver.looksLikeMaster(it.url)), it.lastSeenAt)
        }

    /** The page's gate asks whether Kestrel's own player takes the film the person pressed: true = it has been started (or is up already), the page keeps its element closed. */
    fun handOver(m: JSONObject): Boolean {
        val tab = tabOf(m.optString("p")) ?: return decline(null, "page not found")
        val why = when (m.optString("y")) { "press" -> WebHandOver.Why.PRESS; "kestrel" -> WebHandOver.Why.KESTREL; else -> WebHandOver.Why.PLAY }
        if (!tab.isPrivateTab) WebHandStats.asked++
        return start(tab, why, elementOf(m), m.optString("f"), pageOf(m))
    }

    /** ANS55f: where the pressed element's box is (the gate's eleven numbers; only for a film in the top document that moves with the page's own scroll), or null: the film then plays in the full-screen player. */
    private fun pageOf(m: JSONObject): app.kestrel.core.media.InlineBox.Page? {
        val g = m.optJSONArray("g") ?: return null
        if (g.length() != 11 || m.optInt("tp") != 1) return null
        val v = DoubleArray(11) { g.optDouble(it, Double.NaN) }
        if (v.any { it.isNaN() || it.isInfinite() }) return null
        return app.kestrel.core.media.InlineBox.Page(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], follows = v[10] == 0.0)
    }

    private fun decline(tab: Tab?, why: String): Boolean {
        if (tab == null || !tab.isPrivateTab) { WebHandStats.declined++; WebHandStats.lastDeclined = why }
        return false
    }

    private fun start(tab: Tab, why: WebHandOver.Why, el: WebHandOver.Element, frame: String, geometry: app.kestrel.core.media.InlineBox.Page? = null): Boolean {
        val mode = a.prefs.webVideoMode
        if (mode == WebHandOver.NEVER) return decline(tab, "the setting says the page's own player")
        if (tab !== a.current || tab.isNewTab || a.onHomeScreen(tab)) return decline(tab, "not the page in front")
        if (!a.pageInFront()) return decline(tab, "the app is not in front or a screen lies over the page")      // a late answer after Home or the power button, or a page script's question, starts nothing
        val now = SystemClock.uptimeMillis()
        if (now < tab.gecko.handOverOffUntil) return decline(tab, "Kestrel's player failed on this page a moment ago")
        if (NativePlayerStats.active || now - NativePlayerStats.launchedAt < 3000L) { if (!tab.isPrivateTab) WebHandStats.repeats++; return true }   // it is up (or just started): the page keeps its element closed, no second player
        val pressed = a.lastPressAt != 0L && now - a.lastPressAt <= WebHandOver.pressWindowMs(why)
        if (a.inline.active && !pressed) { if (!tab.isPrivateTab) WebHandStats.repeats++; return true }                 // ANS55f: the film plays inside the page: a question without a tap of the person (the page's own play() again) keeps the element closed
        if (!pressed) return decline(tab, "no tap of the person just before")   // a page's script can fake the question, not a finger (the tap counts once: it is used up below)
        val known = knownIn(tab, frame).let { k -> if (why == WebHandOver.Why.LATE) k.filter { it.durationMs >= WebHandOver.FILM_MS } else k }   // (a playlist that comes late must be known to be as long as a film: it may be an advertisement)
        val d = WebHandOver.decide(mode, why, el, known)
        // ANS61 (owner message 61: "it played an advertisement video and I couldn't get to the real video"): an advertisement is never handed over, and a clip Kestrel's player played to its end is not handed over a second
        // time (the same ad for ever): the page plays it itself, so its ad plugin can finish and go on to the film. The finder's own verdict on that very file (an ad box / decoration) counts like the page's ad state.
        val adWhy = when {
            d == null -> null
            tab.media.find(d.url)?.decoy == true -> "the finder marked this file as an advertisement or decoration"
            !tab.isPrivateTab && WebHandOver.playedThrough(d.url, NativePlayerActivity.lastUrl, NativePlayerActivity.lastPositionMs, NativePlayerActivity.lastDurationMs,
                if (NativePlayerActivity.lastStoppedAt == 0L) -1L else now - NativePlayerActivity.lastStoppedAt) -> "Kestrel's player played this clip to its end a moment ago (an advertisement the page has to see finished)"
            else -> null
        }
        if (adWhy != null) { lastRefused = null; return decline(tab, adWhy) }
        if (d == null) {
            // (a playlist that came late and was refused is one more list, not the end of the press that is waiting for its film: that memory stays until its ten seconds are over)
            if (why != WebHandOver.Why.LATE) lastRefused = if (el.blob && why != WebHandOver.Why.FULLSCREEN && mode == WebHandOver.ALWAYS && !tab.isPrivateTab) Refused(tab.id, tab.url.substringBefore('#'), now, frame, el) else null
            return decline(tab, if (mode == WebHandOver.FULLSCREEN && why != WebHandOver.Why.FULLSCREEN) "the setting says full screen only" else if (el.loop) "a loop clip" else if (el.ad) "an advertisement plays in the page's player" else if (WebHandOver.looksLikeAdAddress(el.src)) "the address names an advertisement" else if (el.blob) "no stream known (yet)" else "not a file or stream Kestrel's player plays")
        }
        lastRefused = null
        if (a.inline.active && a.inline.url == d.url) { if (!tab.isPrivateTab) WebHandStats.repeats++; a.lastPressAt = 0L; return true }   // ANS55f: the same film is up inside the page already
        val seed = tab.media.find(d.url) ?: MediaCandidate(d.url, d.kind, "", -1, tab.url, frame, System.currentTimeMillis())
        val headers = MediaHeaders.sets(seed, tab.url).firstOrNull()?.map { "${it.key}: ${it.value}" }.orEmpty()
        try { tab.gecko.mediaSession?.pause() } catch (e: Throwable) { /* the page's own film may have no media session */ }
        // pressed again on the same page (Back from Kestrel's player, play once more): it continues where the person stopped, not from the page's own start (RAM only, never for a private tab)
        val startMs = if (tab.isPrivateTab) d.startMs else WebHandOver.resumeMs(d.startMs, d.url, NativePlayerActivity.lastUrl, NativePlayerActivity.lastPositionMs, NativePlayerActivity.lastDurationMs,
            if (NativePlayerActivity.lastStoppedAt == 0L) -1L else now - NativePlayerActivity.lastStoppedAt)
        if (!tab.isPrivateTab) {
            when (why) { WebHandOver.Why.PLAY -> WebHandStats.byPlay++; WebHandOver.Why.PRESS -> WebHandStats.byPress++; WebHandOver.Why.KESTREL -> WebHandStats.byKestrel++; WebHandOver.Why.FULLSCREEN -> WebHandStats.byFullscreen++; WebHandOver.Why.LATE -> WebHandStats.late++ }
            WebHandStats.lastTaken = "${if (d.kind == MediaKind.HLS) "a stream" else "a file"}, ${d.note}, from ${startMs / 1000} s"
            Health.log("video", "hand-over", "${why.name.lowercase()} ${if (d.kind == MediaKind.HLS) "HLS" else "file"} from ${startMs / 1000} s (${d.note}) on ${UrlParts.hostOf(d.url) ?: "?"} ${d.url.substringBefore('?').substringBefore('#').trimEnd('/').substringAfterLast('/').take(40)}")   // (the host and the last piece of the address name which list was chosen: the Diagnostics of owner message 55 could not tell)
        }
        // ANS55f (owner message 55): INSIDE the page when the box can be told (the person's choice, Settings > Play web videos inside the page): the film plays over the page's own video box; else (a frame, a
        // fixed or scrolling box, a zoomed page, a small thumbnail, a film that came late or went full screen) the full-screen screen takes it as before
        if (a.prefs.webInline && geometry == null && why != WebHandOver.Why.LATE && why != WebHandOver.Why.FULLSCREEN && !tab.isPrivateTab) Health.log("inline", "not-inline", "the page told no box (a film in a frame, or the question carried no numbers): full-screen player")
        if (a.prefs.webInline && geometry != null && why != WebHandOver.Why.LATE && why != WebHandOver.Why.FULLSCREEN && !a.isFullscreenOf(tab)) {
            val box = a.inline.boxFor(geometry)
            if (box != null && a.inline.start(tab, d.url, headers, startMs, geometry, box)) { a.lastPressAt = 0L; return true }
            if (!tab.isPrivateTab) Health.log("inline", "not-inline", if (box == null) "the box cannot be placed (frame, fixed or scrolling box, zoomed page, small or off screen): full-screen player; page box ${geometry.w.toInt()}x${geometry.h.toInt()} at ${geometry.x.toInt()},${geometry.y.toInt()}, visual viewport scale ${"%.3f".format(geometry.vvScale)} width ${geometry.vvW.toInt()}, scroll ${geometry.pageX.toInt()},${geometry.pageY.toInt()}, follows ${geometry.follows}, view ${a.inline.viewSize()}" else "the inline player did not start: full-screen player")
        }
        NativePlayerActivity.launch(a, d.url, headers, startMs, if (d.kind == MediaKind.HLS) 720 else 1080, tab.isPrivateTab, 0, 0, fromPage = true)
        a.lastPressAt = 0L
        if (a.isFullscreenOf(tab)) a.exitFullscreenFromControls()                  // a film that was full screen in the page: the page's full screen ends, Kestrel's player takes the screen
        return true
    }

    /** A page film went full screen in the page and was not handed over before (its stream was not known at the press): at the first report of the film (its position is known now) it moves to Kestrel's player. */
    fun handOverFullscreen(tab: Tab, timeS: Double, lengthS: Double): Boolean {
        if (a.prefs.webVideoMode == WebHandOver.NEVER) return false
        val p = tab.media.primary()
        val src = if (p != null && p.kind == MediaKind.FILE) p.url else ""
        return start(tab, WebHandOver.Why.FULLSCREEN, WebHandOver.Element(src, false, p?.mime.orEmpty(), false, 0, 0, timeS, lengthS), p?.frame.orEmpty())
    }

    /** A film-length playlist the sniffer saw within 10 s after a press that was refused for want of a stream (a player that asks for its playlist only after the click): the film moves over now, the page's media session is paused. */
    private fun lateHandOver(tab: Tab, c: MediaCandidate) {
        val r = lastRefused ?: return
        if (r.tabId != tab.id || r.page != tab.url.substringBefore('#') || SystemClock.uptimeMillis() - r.at > 10_000L || c.frame != r.frame) return
        lastRefused = null
        if (start(tab, WebHandOver.Why.LATE, r.el, c.frame)) try { tab.gecko.mediaSession?.pause() } catch (e: Throwable) { /* none */ }
    }

    /** Kestrel's own player ended with an error before its first picture for a film of this page: the page's own player plays its films for two minutes, and the person is told. */
    fun nativeFailed() {
        val t = a.current ?: return
        t.gecko.handOverOffUntil = SystemClock.uptimeMillis() + 120_000L
        NativePlayerStats.launchedAt = 0L                                              // (it is over: a press now is a new one, not the same launch)
        if (!t.isPrivateTab) WebHandStats.failed++
        a.toast("Kestrel's own player could not play this film. Press play again: the page's own player will.")
    }

    /** A row of the page's quality list was picked ([rung] null = Automatic): the page's film stops and Kestrel's own player goes on from [startMs] with that picture. */
    fun playPicked(tab: Tab, film: PageFilm, rung: QualityLabel.Rung?, startMs: Long) {
        val c = film.cand
        val headers = MediaHeaders.sets(c, tab.url).firstOrNull()?.map { "${it.key}: ${it.value}" }.orEmpty()
        try { tab.gecko.mediaSession?.pause() } catch (e: Throwable) { /* a page film without a media session is silent: nothing plays sound */ }
        if (!tab.isPrivateTab) Health.log("video", "native-quality", "${if (rung == null) "Automatic" else QualityLabel.text(rung)} from ${startMs / 1000} s")
        NativePlayerActivity.launch(a, c.url, headers, startMs, 720, tab.isPrivateTab, tab.gecko.pictureW, tab.gecko.pictureH, rung, rung == null)
    }

    /** What the page loaded that is worth considering, plus the pressed element's own file even when the usual size rules would hide it. */
    private fun candidates(tab: Tab, pressedUrl: String?): List<MediaCandidate> {
        val items = ArrayList(tab.media.usable())
        pressedUrl?.let { u -> tab.media.find(u)?.let { c -> if (items.none { it === c }) items.add(c) } }
        return items
    }

    private fun looksMp4(c: MediaCandidate): Boolean = MediaSniff.extension(c.url) in setOf("mp4", "m4v", "mov") || c.mime == "video/mp4" || c.mime == "video/quicktime"

    /** Reads the start of an MP4 (and, when its index sits at the end, that box) to name its picture size and length: a few hundred KB, only when the sheet is open. */
    private fun probe(tab: Tab, c: MediaCandidate, from: Long, length: Int, depth: Int) {
        DownloadController.fetchBytes(a, c.url, from, from + length - 1, requestHeaders(tab, c), 1, tab.isPrivateTab, onTotal = { n -> if (c.length <= 0) c.length = n }) { bytes ->
            if (bytes == null || depth > 3) return@fetchBytes
            when (val r = Mp4Probe.probe(bytes, bytes.size, from)) {
                is Mp4Probe.Result.Found -> { if (c.height == 0 && r.info.height > 0) { c.width = r.info.width; c.height = r.info.height }; if (c.durationMs == 0L) c.durationMs = r.info.durationMs; refresh(tab) }
                is Mp4Probe.Result.NeedMore -> if (c.length <= 0 || r.offset < c.length) probe(tab, c, r.offset, r.length.coerceIn(16 * 1024, 4 * 1024 * 1024), depth + 1)
                Mp4Probe.Result.NotMp4 -> {}
            }
        }
    }

    private fun render(p: VideoPanel, tab: Tab) { p.show(model(tab)) }

    /** A playlist finished loading: redraw the sheet if it is still open on this tab. */
    private fun refresh(tab: Tab) {
        val p = panel ?: return
        if (panelTab !== tab || !a.sheet.isShowing || p.parent == null) return
        render(p, tab)
    }

    /** The default name (without its extension) of a stream's quality: the page's title and the quality, the same text the download itself uses. */
    private fun stemFor(tab: Tab, label: String): String = MediaNames.streamStem(tab.title, tab.url, UrlParts.hostOf(tab.url) ?: "", label)

    /** One line under the format (owner request X17): file type, size, length - whatever is known, in that order. */
    private fun rowDetail(vararg parts: String): String = parts.filter { it.isNotEmpty() && it != "size unknown" }.joinToString("  \u00B7  ")

    /** A row for one quality of a stream. [key] keeps it the same row when the sheet is drawn again. */
    private fun streamChoice(tab: Tab, c: MediaCandidate, pick: StreamPick, title: String, detail: String, ext: String): VideoPanel.Choice =
        VideoPanel.Choice("s|" + c.key + "|" + (pick.repId ?: pick.url) + "|" + pick.label, title, detail, stemFor(tab, pick.label), ext, pick)

    /** What the sheet shows: the ranked files (the long-pressed one first), the streams' quality lists as they arrive, and the small or odd files tucked under "Other files". */
    private fun model(tab: Tab): VideoPanel.Model {
        val ranked = MediaRank.rank(candidates(tab, panelPressed), panelPressed, tab.media.primary())
        val host = UrlParts.hostOf(tab.url) ?: ""
        val rows = ArrayList<VideoPanel.Choice>(); val other = ArrayList<VideoPanel.Choice>()
        var note: String? = null; var busy: String? = null
        val names = HashMap<String, Int>()
        fun addAll(into: MutableList<VideoPanel.Choice>, list: List<MediaCandidate>) {
            for (c in list) {
                when {
                    c.kind == MediaKind.HLS && c.key in children -> {}
                    c.kind == MediaKind.HLS -> when (val st = streamInfo[c.key] ?: loadStream(tab, c)) {
                        is Loaded.Loading -> busy = "Reading the quality list\u2026"
                        is Loaded.Failed -> note = note ?: st.message
                        is Loaded.Ready -> { note = note ?: st.note; for ((ch, _) in st.rows) into.add(ch) }
                    }
                    c.kind == MediaKind.DASH -> when (val st = streamInfo[c.key] ?: loadDash(tab, c)) {
                        is Loaded.Loading -> busy = "Reading the quality list\u2026"
                        is Loaded.Failed -> note = note ?: st.message
                        is Loaded.Ready -> { note = note ?: st.note; for ((ch, _) in st.rows) into.add(ch) }
                    }
                    else -> {
                        val q = MediaLabels.quality(c.knownHeight)
                        val ext = MediaSniff.saveExtension(c.url, c.mime)
                        val title = if (c.audioOnly) "Audio only" else q.ifEmpty { "Video file" }
                        val detail = rowDetail(MediaLabels.format(c), MediaLabels.size(c.length), MediaLabels.duration(c.durationMs))
                        val stem = MediaNames.defaultStem(c, tab.title, host, if (c.audioOnly) "audio" else q, tab.url)
                        into.add(VideoPanel.Choice("f|" + c.key, title, detail, stem, ext, c, badge = if (c === ranked.exact) "THIS VIDEO" else null))
                    }
                }
            }
        }
        addAll(rows, ranked.main); addAll(other, ranked.other)
        // two rows that read the same (two files of one quality) get the file's name so they can be told apart
        for (ch in rows) names.merge(ch.title + "|" + ch.detail, 1, Int::plus)
        fun withName(ch: VideoPanel.Choice, c: MediaCandidate) = VideoPanel.Choice(ch.id, ch.title, ch.detail + "  \u00B7  " + shortName(c.url), ch.stem, ch.ext, ch.tag, ch.enabled, ch.badge)
        fun named(list: List<VideoPanel.Choice>) = list.map { ch -> if ((names[ch.title + "|" + ch.detail] ?: 0) > 1 && ch.tag is MediaCandidate) withName(ch, ch.tag) else ch }
        // under "Other files" every row reads alike ("Video file, MP4, 300 KB"): the address's file name ("getit.php") is what tells the owner which one it is
        fun namedOther(list: List<VideoPanel.Choice>) = list.map { ch -> if (ch.tag is MediaCandidate && shortName(ch.tag.url).isNotEmpty()) withName(ch, ch.tag) else ch }
        return VideoPanel.Model(
            source = tab.displayTitle.takeIf { it.isNotBlank() } ?: host, rows = named(rows), other = namedOther(other), note = note, busy = busy, extras = panelExtras,
            sizeText = { ch -> when (val t = ch.tag) { is MediaCandidate -> if (t.length > 0) MediaLabels.size(t.length) else ""; is StreamPick -> if (t.estimate > 0) MediaLabels.size(t.estimate, estimate = true) else ""; else -> "" } },
        )
    }

    private fun shortName(url: String): String = url.substringBefore('?').substringAfterLast('/').take(28)

    // ---- HLS: read the playlist, list the qualities

    /**
     * The headers a candidate is fetched with: the ones that were seen to work for it, else the first guess ([MediaHeaders.sets]: what the browser itself sent, the player's page, the web page).
     * A video server often answers 403 unless the request names ITS player as the asker (owner: a movie from an embedded "vidsrc" server could not be saved).
     */
    private fun requestHeaders(tab: Tab, c: MediaCandidate): Map<String, String> = MediaHeaders.sets(c, tab.url).first()

    /** The words for a refused or failed read of a playlist: what the server said, and what to try. */
    private fun whyNotRead(status: Int): String = when {
        status == 401 || status == 403 -> "The site refused to show the stream's list (HTTP $status), even with the player's own headers. Reload the page, press play, and try again: the link may have expired."
        status == 404 || status == 410 -> "The stream's list is gone (HTTP $status). Reload the page, press play, and try again."
        status >= 400 -> "The site answered the stream's list with an error (HTTP $status)."
        else -> "The stream's list could not be read (no answer from the site)."
    }

    /**
     * Reads a playlist, trying the request headers one set after the other ([MediaHeaders.sets]) until the site answers with something [valid]. The set that worked is remembered for the candidate, so every
     * later request of this download (qualities, sizes, the parts, the keys) repeats it. [done]: the text and the HTTP status of the last try.
     */
    private fun fetchPlaylist(tab: Tab, c: MediaCandidate, url: String, valid: (String) -> Boolean, done: (String?, Int) -> Unit) {
        val sets = MediaHeaders.sets(c, tab.url)
        val host = UrlParts.hostOf(url) ?: ""
        fun attempt(i: Int, last: Int) {
            if (i >= sets.size) { Health.log("media", "list-failed", "$host http=$last tried=${sets.size}"); done(null, last); return }
            DownloadController.fetchTextStatus(a, url, sets[i], 1, tab.isPrivateTab) { text, status ->
                if (text != null && valid(text)) {
                    if (c.viaHeaders != sets[i]) c.viaHeaders = sets[i]
                    Health.log("media", "list-ok", "$host http=$status set=${i + 1}/${sets.size} referer=${UrlParts.hostOf(sets[i]["Referer"] ?: "") ?: "none"}")
                    done(text, status)
                } else attempt(i + 1, if (status > 0) status else last)
            }
        }
        attempt(0, 0)
    }

    private fun loadStream(tab: Tab, c: MediaCandidate): Loaded {
        streamInfo[c.key] = Loaded.Loading
        fetchPlaylist(tab, c, c.url, { Hls.parse(it, c.url) != null }) { text, status ->
            val pl = text?.let { Hls.parse(it, c.url) }
            when (pl) {
                null -> { streamInfo[c.key] = Loaded.Failed(whyNotRead(status)); refresh(tab) }
                is Hls.MediaPlaylist -> { streamInfo[c.key] = ready(tab, c, null, pl.media, text, null); refresh(tab); sizeBareStream(tab, c, pl.media, text) }
                is Hls.MasterPlaylist -> { c.master = true; noteMaster(c, pl.master); loadVariants(tab, c, pl.master, requestHeaders(tab, c)) }
            }
        }
        return Loaded.Loading
    }

    /** One variant's playlist tells how long the video is (all variants are equally long); with that, every variant's size is its bit rate times it. */
    private fun loadVariants(tab: Tab, c: MediaCandidate, master: Hls.Master, headers: Map<String, String>) {
        val vs = master.variants
        for (v in vs) children.add(v.url)
        for (r in master.renditions) r.url?.let { children.add(it) }
        childUrls[c.key] = vs.map { it.url } + master.renditions.mapNotNull { it.url }
        if (vs.isEmpty()) { streamInfo[c.key] = Loaded.Failed("This stream lists no qualities."); refresh(tab); return }
        val probe = vs.filter { !it.audioOnly }.minByOrNull { it.bandwidth } ?: vs.minByOrNull { it.bandwidth }!!
        DownloadController.fetchText(a, probe.url, headers, 1, tab.isPrivateTab) { text ->
            val media = (text?.let { Hls.parse(it, probe.url) } as? Hls.MediaPlaylist)?.media
            streamInfo[c.key] = if (media == null) Loaded.Failed("The stream's list could not be read.") else ready(tab, c, master, media, text, probe)
            refresh(tab)
            if (media != null && text != null) calibrateMaster(tab, c, master, media, text, probe)
        }
    }

    /**
     * The qualities of a master playlist are sized by the rate each one declares, and a declared rate is the PEAK, often 20 to 50 % above what the file averages (AO2: "the video downloader is inaccurate at
     * estimating file size"). Measure a few parts of the one quality whose list was read (a one-byte Range request each: only the headers of the reply matter, so it costs almost no data), see how far its
     * declared rate was off, and scale every quality by that ([SizeEstimate]). Muxed streams only: with the sound in a list of its own the declared rate covers both and the measured parts only the picture.
     * The list is redrawn when the answers are in.
     */
    private fun calibrateMaster(tab: Tab, c: MediaCandidate, master: Hls.Master, media: Hls.Media, text: String, probe: Hls.Variant) {
        if (master.audioFor(probe) != null) return
        val points = ArrayList<Pair<Long, Double>>()
        fun redraw() { if (points.isNotEmpty() && streamInfo[c.key] is Loaded.Ready) { streamInfo[c.key] = ready(tab, c, master, media, text, probe, points = points.toList()); refresh(tab) } }
        measureQuality(tab, c, media, probe) { cal -> if (cal != null) { points.add(cal); redraw() } }
        // the sharpest quality too: its peak sits further above its average than the lowest one's, and it is the file most worth sizing right
        val top = master.variants.filter { !it.audioOnly && master.audioFor(it) == null }.maxByOrNull { it.bandwidth }
        if (top != null && top !== probe && top.bandwidth > probe.bandwidth) {
            DownloadController.fetchText(a, top.url, requestHeaders(tab, c), 1, tab.isPrivateTab) { t ->
                val topMedia = (t?.let { Hls.parse(it, top.url) } as? Hls.MediaPlaylist)?.media
                if (topMedia != null) measureQuality(tab, c, topMedia, top) { cal -> if (cal != null) { points.add(cal); redraw() } }
            }
        }
    }

    /** Measures [SizeEstimate.SAMPLES] parts of one quality; [done] gets (declared rate, calibration) on the UI thread, or null when it could not be measured. */
    private fun measureQuality(tab: Tab, c: MediaCandidate, media: Hls.Media, v: Hls.Variant, done: (Pair<Long, Double>?) -> Unit) {
        val n = media.segments.size
        if (media.exactBytes >= 0 || n == 0 || v.bytesPerSecond <= 0) { done(null); return }
        val idx = SizeEstimate.sampleIndexes(n)
        val sizes = LongArray(idx.size) { -1L }
        var answered = 0
        idx.forEachIndexed { k, i ->
            DownloadController.fetchBytes(a, media.segments[i].url, 0L, 0L, requestHeaders(tab, c), 1, tab.isPrivateTab, onTotal = { len -> sizes[k] = len }) {
                if (++answered == idx.size) {
                    val rate = SizeEstimate.measuredRate(idx.indices.map { sizes[it] }, idx.map { media.segments[it].durationMs })
                    val cal = SizeEstimate.calibration(rate, v.bytesPerSecond)
                    Health.log("media", "size-calibrated", "${v.label} samples=${idx.size} measured=${rate.toLong()} B/s declared=${v.bytesPerSecond} B/s factor=${String.format(java.util.Locale.US, "%.2f", cal)}")
                    done(if (rate > 0) v.bytesPerSecond to cal else null)
                }
            }
        }
    }

    /**
     * A playlist that is not part of a master has no bit rate to size it by. Ask the server how long two of its segments are (a one-byte Range request each: only the headers matter) and
     * multiply the average by the number of segments: good enough for "about 6 MB". The list is redrawn when the answers are in.
     */
    private fun sizeBareStream(tab: Tab, c: MediaCandidate, media: Hls.Media, text: String) {
        val n = media.segments.size
        if (media.exactBytes >= 0 || n == 0 || media.segments[0].len >= 0) return
        val idx = SizeEstimate.sampleIndexes(n)                     // spread over the timeline, weighted by each part's length (AO2)
        val sizes = LongArray(idx.size) { -1L }
        var answered = 0
        idx.forEachIndexed { k, i ->
            DownloadController.fetchBytes(a, media.segments[i].url, 0L, 0L, requestHeaders(tab, c), 1, tab.isPrivateTab, onTotal = { len -> sizes[k] = len }) {
                if (++answered == idx.size) {
                    val rate = SizeEstimate.measuredRate(idx.indices.map { sizes[it] }, idx.map { media.segments[it].durationMs })
                    val guess = SizeEstimate.total(rate, media.durationMs, media.container) // an MP4 made of a transport stream is a little smaller than the stream
                    val cur = streamInfo[c.key]
                    if (guess > 0 && cur is Loaded.Ready) { streamInfo[c.key] = ready(tab, c, null, media, text, null, guess); refresh(tab) }
                }
            }
        }
    }

    private fun ready(tab: Tab, c: MediaCandidate, master: Hls.Master?, media: Hls.Media, mediaText: String?, probe: Hls.Variant?, sizeGuess: Long = -1L, points: List<Pair<Long, Double>> = emptyList()): Loaded {
        when (media.problem) {
            Hls.Problem.LIVE -> return Loaded.Failed("This is a live stream. Live streams cannot be saved.")
            Hls.Problem.DRM -> return Loaded.Failed("This video is copy-protected (DRM). Kestrel cannot save it.")
            Hls.Problem.SAMPLE_AES, Hls.Problem.UNSUPPORTED -> return Loaded.Failed("This stream uses protection Kestrel cannot save yet.")
            Hls.Problem.EMPTY -> return Loaded.Failed("This stream has no parts to save.")
            null -> {}
        }
        val dur = MediaLabels.duration(media.durationMs)
        val rows = ArrayList<Pair<VideoPanel.Choice, StreamPick>>()
        if (master == null) {
            val audio = media.container == "aac" || media.container == "mp3" || media.container == "m4a"
            val est = if (media.exactBytes >= 0) media.exactBytes else sizeGuess
            val pick = StreamPick(if (audio) "audio" else "", c.url, mediaText, est, null, audio, media.container, headers = requestHeaders(tab, c), cand = c)
            val ext = if (audio) media.container else "mp4"          // a video stream is saved as one MP4 file
            rows.add(streamChoice(tab, c, pick, if (audio) "Audio only" else "Video", rowDetail(ext.uppercase(), MediaLabels.size(est, estimate = media.exactBytes < 0 && est > 0), dur), ext) to pick)
            return Loaded.Ready(rows, null)
        }
        val seen = HashSet<String>()
        for (v in master.variants.sortedWith(compareBy<Hls.Variant> { it.audioOnly }.thenByDescending { it.height }.thenByDescending { it.bandwidth })) {
            if (!seen.add("${v.height}|${v.bandwidth / 100_000}|${v.audioOnly}")) continue
            val est = SizeEstimate.calibrated(v.bytesPerSecond, SizeEstimate.factorFor(v.bytesPerSecond, points), media.durationMs, media.container) // the declared rate is a peak: scaled by what the measured parts weighed (calibrateMaster)
            val sepAudio = master.audioFor(v)
            val text = if (v === probe) mediaText else null
            val title = if (v.audioOnly) "Audio only" else v.label
            val pick = StreamPick(if (v.audioOnly) "audio" else v.label, v.url, text, est, sepAudio?.url, v.audioOnly, media.container, headers = requestHeaders(tab, c), cand = c)
            val ext = if (v.audioOnly) media.container else "mp4"
            val detail = rowDetail(ext.uppercase(), MediaLabels.size(est, estimate = true), dur)
            rows.add(streamChoice(tab, c, pick, title, detail, ext) to pick)
        }
        return Loaded.Ready(rows, null)
    }

    // ---- DASH: the manifest lists picture and sound representations; the chosen picture is downloaded together with the best AAC sound and merged into one MP4

    private fun loadDash(tab: Tab, c: MediaCandidate): Loaded {
        streamInfo[c.key] = Loaded.Loading
        fetchPlaylist(tab, c, c.url, { Dash.parse(it, c.url) != null }) { text, status ->
            val man = text?.let { Dash.parse(it, c.url) }
            streamInfo[c.key] = if (man == null || text == null) Loaded.Failed(whyNotRead(status)) else dashReady(tab, c, man, text)
            refresh(tab)
        }
        return Loaded.Loading
    }

    private fun dashReady(tab: Tab, c: MediaCandidate, man: Dash.Manifest, text: String): Loaded {
        when (man.problem) {
            Hls.Problem.LIVE -> return Loaded.Failed("This is a live stream. Live streams cannot be saved.")
            Hls.Problem.DRM -> return Loaded.Failed("This video is copy-protected (DRM). Kestrel cannot save it.")
            Hls.Problem.UNSUPPORTED, Hls.Problem.SAMPLE_AES -> return Loaded.Failed("This stream is split into several parts that Kestrel cannot join yet.")
            Hls.Problem.EMPTY -> return Loaded.Failed("This stream has no parts to save.")
            null -> {}
        }
        val dur = MediaLabels.duration(man.durationMs)
        val sound = man.audio.firstOrNull { it.mp4Friendly && it.media != null }
        val rows = ArrayList<Pair<VideoPanel.Choice, StreamPick>>()
        var skipped = 0
        val seen = HashSet<String>()
        for (v in man.video) {
            if (v.media == null || !v.mp4Friendly) { skipped++; continue }
            if (!seen.add("${v.height}|${v.bandwidth / 100_000}")) continue
            val est = (v.bytesPerSecond + (sound?.bytesPerSecond ?: 0L)) * (man.durationMs / 1000)
            val pick = StreamPick(v.label, c.url, text, est, null, false, "mp4", repId = v.id, audioRepId = sound?.id, headers = requestHeaders(tab, c), cand = c)
            rows.add(streamChoice(tab, c, pick, v.label, rowDetail("MP4", MediaLabels.size(est, estimate = true), dur), "mp4") to pick)
        }
        for (au in man.audio.filter { it.mp4Friendly && it.media != null }.take(2)) {
            val est = au.bytesPerSecond * (man.durationMs / 1000)
            val pick = StreamPick("audio", c.url, text, est, null, true, "m4a", repId = au.id, ext = "m4a", headers = requestHeaders(tab, c), cand = c)
            rows.add(streamChoice(tab, c, pick, "Audio only", rowDetail("M4A", MediaLabels.size(est, estimate = true), dur), "m4a") to pick)
        }
        if (rows.isEmpty()) return Loaded.Failed("This stream's qualities use a format (WebM / VP9) that Kestrel cannot save yet.")
        return Loaded.Ready(rows, if (skipped > 0) "Some qualities use a format (WebM / VP9) that Kestrel cannot save yet." else null)
    }

    private fun pickStream(tab: Tab, p: StreamPick, stem: String) {
        a.sheet.dismiss()
        val host = UrlParts.hostOf(tab.url) ?: ""
        val size = MediaLabels.size(p.estimate, estimate = true)
        val go = {
            val h = p.headers ?: MediaHeaders.fromAddress(tab.url)
            val page = tab.url.takeIf { it.isNotEmpty() }
            val soundStem = "$stem (sound)"
            // the sound of a picture that comes separately is downloaded alongside and merged into one MP4 when both are in (if the merge is refused, both files stay)
            // EY-8: what the engine needs to find its way back when a link is refused: the address the owner picked from, the player's document, the header ladder (the first is [h])
            val c = p.cand
            val master = c?.url; val frame = c?.frame?.takeIf { it.isNotEmpty() }
            val ladder = if (c != null) listOf(h) + MediaHeaders.sets(c, tab.url).filter { it != h }.take(5) else emptyList()
            val made = System.currentTimeMillis()
            val companion = when {
                p.audioRepId != null -> StreamSource(p.url, p.text, page, soundStem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, repId = p.audioRepId, ext = "m4a", masterUrl = master, frame = frame, headerSets = ladder, madeAt = made)
                p.audioUrl != null -> StreamSource(p.audioUrl, null, page, soundStem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, masterUrl = master, frame = frame, headerSets = ladder, madeAt = made)
                else -> null
            }
            DownloadController.enqueueStream(a, StreamSource(p.url, p.text, page, stem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, estimateBytes = p.estimate, repId = p.repId, ext = p.ext, companion = companion, masterUrl = master, frame = frame, headerSets = ladder, madeAt = made))
            a.askNotificationPermissionOnce() // the banner at the top says "Download started" (DownloadPopup)
        }
        val limit = a.prefs.dataSaver.confirmDownloadBytes(NetworkMonitor.metered)
        val big = limit >= 0 && (p.estimate <= 0 || p.estimate >= limit)
        val cleartext = p.url.startsWith("http://", true)
        val msg = buildString {
            append(stem).append("\nFrom: ").append(host.ifEmpty { "unknown site" }).append("\nSize: ").append(size)
            if (cleartext) append("\n\nThis stream is not encrypted: your mobile carrier or anyone on the network could read or change the file.")
            if (big) append("\n\nYou're on a metered connection (your phone's hotspot). This may use a lot of mobile data.")
            if (tab.isPrivateTab) append("\n\nIt is saved in your Downloads folder and stays there after you close the private tab. Kestrel keeps no record of it.")
        }
        if (big || cleartext || tab.isPrivateTab) a.askYesNo("Download this video?", msg, "Download", "Cancel") { ok -> if (ok) go() } else go()
    }

    /** "MP4 - 48.2 MB - 1:02" */
    private fun detail(c: MediaCandidate): String = listOf(MediaLabels.format(c), MediaLabels.size(c.length), MediaLabels.duration(c.durationMs)).filter { it.isNotEmpty() }.joinToString("  ·  ")

    /**
     * SOAK1 (the emulator lab, debug builds only, BrowserActivity.labIntent "videodl"): what the "Save video" sheet would offer on this tab, and what one tap on its first row does.
     * One Health line says what was found; a plain file is then saved exactly as the sheet would save it.
     */
    fun labDownloadBest(tab: Tab) {
        val ranked = MediaRank.rank(candidates(tab, null), null, tab.media.primary())
        val c = ranked.main.firstOrNull() ?: ranked.exact
        Health.log("lab", "videodl", "main=${ranked.main.size} other=${ranked.other.size} usable=${tab.media.usableCount()} of ${tab.media.size} pick=${if (c == null) "none" else "${c.kind} ${c.height}p ${c.length / 1024}KB"}")
        if (c != null && c.kind == MediaKind.FILE) pick(tab, c, "lab-found-video")
        else if (c != null) labStream(tab, c, 0)                  // VS1: a playlist (HLS / DASH) goes the way a tap on its row goes: read, list the pictures, download the first (best) one
    }

    /** VS1 lab: reads the stream's list, waits (at most 30 s) for the pictures and starts the first row's download; every step is a `lab videodl-stream` line the emulator lab reads. */
    private fun labStream(tab: Tab, c: MediaCandidate, tries: Int) {
        val st = streamInfo[c.key]
        if (st == null) {
            if (c.kind == MediaKind.DASH) loadDash(tab, c) else loadStream(tab, c)
            Io.main.postDelayed({ labStream(tab, c, tries + 1) }, 1000)
            return
        }
        when (st) {
            is Loaded.Loading -> if (tries < 30) Io.main.postDelayed({ labStream(tab, c, tries + 1) }, 1000) else Health.log("lab", "videodl-stream", "gave up: the list was still loading after 30 s")
            is Loaded.Failed -> Health.log("lab", "videodl-stream", "failed: ${st.message.take(140)}")
            is Loaded.Ready -> {
                val first = st.rows.firstOrNull()
                Health.log("lab", "videodl-stream", "ready rows=${st.rows.size} first=${first?.second?.label ?: "none"}")
                if (first != null) pickStream(tab, first.second, "lab-found-video")
            }
        }
    }

    /** LAB (ANS52i): the best row of the Save video sheet, played in Kestrel's own player (the lab cannot press the sheet's button). */
    fun labPlayBest(tab: Tab) {
        val ranked = MediaRank.rank(candidates(tab, null), null, tab.media.primary())
        val c = ranked.main.firstOrNull() ?: ranked.exact
        Health.log("lab", "nativeplay", "main=${ranked.main.size} other=${ranked.other.size} usable=${tab.media.usableCount()} of ${tab.media.size} pick=${if (c == null) "none" else "${c.kind} ${c.height}p"}")
        if (c != null && c.kind == MediaKind.FILE) {
            val headers = MediaHeaders.sets(c, tab.url).firstOrNull()?.map { "${it.key}: ${it.value}" }.orEmpty()
            NativePlayerActivity.launch(a, c.url, headers, 0L, 1080, tab.isPrivateTab, c.width, c.height)
        }
    }

    private fun pick(tab: Tab, c: MediaCandidate, stem: String) {
        a.sheet.dismiss()
        tab.media.touch(c.url)
        val name = stem + "." + MediaSniff.saveExtension(c.url, c.mime)
        Health.log("media", "file", "${UrlParts.hostOf(c.url) ?: ""} referer=${UrlParts.hostOf(requestHeaders(tab, c)["Referer"] ?: "") ?: "none"}")
        a.download(tab, c.url, null, null, c.mime.ifEmpty { null }, c.length, nameOverride = name, headers = requestHeaders(tab, c))
    }
}
