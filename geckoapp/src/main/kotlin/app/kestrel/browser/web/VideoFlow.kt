package app.kestrel.browser.web

import android.app.AlertDialog
import app.kestrel.browser.BrowserActivity
import app.kestrel.browser.Io
import app.kestrel.browser.ui.VideoPanel
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.core.media.Dash
import app.kestrel.core.media.Hls
import app.kestrel.core.media.MediaCandidate
import app.kestrel.core.media.MediaKind
import app.kestrel.core.media.MediaLabels
import app.kestrel.core.media.MediaNames
import app.kestrel.core.media.MediaRank
import app.kestrel.core.media.MediaSniff
import app.kestrel.core.media.Mp4Probe
import app.kestrel.core.media.StreamSource
import app.kestrel.core.url.UrlParts

/**
 * "Download video": what the page's sniffed media becomes in the UI. The extension reports every media file / playlist a page loads ([seen]), the page's own players report what they
 * know about them ([info]); a long press on a video or the menu entry opens the sheet ([show]) and a tap on a row starts an ordinary download, so pause / resume / notifications / the
 * metered-link questions are the ones every download already has. Design and limits: research/VIDEO_DOWNLOADER.md.
 */
class VideoFlow(private val a: BrowserActivity) {
    /** One quality of an HLS stream, ready to download. */
    private class StreamPick(val label: String, val url: String, val text: String?, val estimate: Long, val audioUrl: String?, val audioOnly: Boolean, val container: String, val repId: String? = null, val audioRepId: String? = null, val ext: String? = null)
    private sealed class Loaded {
        object Loading : Loaded()
        class Failed(val message: String) : Loaded()
        class Ready(val rows: List<Pair<VideoPanel.Choice, StreamPick>>, val note: String?) : Loaded()
    }
    private val streamInfo = HashMap<String, Loaded>()   // candidate key -> what its playlist turned out to be
    private val probed = HashSet<String>()              // files whose first bytes were already read to learn their picture size
    private val sized = HashSet<String>()               // files whose length was already asked for (found in the page markup, never requested)
    private val children = HashSet<String>()            // addresses of the variant / audio playlists of masters we have read: they are not rows of their own
    private var panel: VideoPanel? = null
    private var panelTab: Tab? = null
    private var panelPressed: String? = null
    private var panelExtras: List<Pair<String, () -> Unit>> = emptyList()

    /** Called on the Shields worker thread. */
    fun seen(c: MediaCandidate) = Io.ui {
        val t = a.current?.takeIf { it.url == c.page } ?: a.tabWithUrl(c.page) ?: a.tabForPage(c.page) ?: a.current ?: return@ui // the selected tab when it is on exactly that page (two tabs on one page: the one being looked at), else the first tab on it; the same host or the selected tab only as a fallback
        if (t.media.add(c)) a.refreshDownloadButton()
    }

    /** Called on the Shields worker thread. */
    fun info(page: String, url: String, width: Int, height: Int, durationMs: Long, label: String?) = Io.ui {
        val t = (if (page.isNotEmpty()) a.current?.takeIf { it.url == page } ?: a.tabWithUrl(page) ?: a.tabForPage(page) else null) ?: a.current ?: return@ui
        t.media.applyInfo(url, width, height, durationMs, label)
        a.refreshDownloadButton()
    }

    /** Anything worth offering on this tab's page? (the menu shows its entry only then) */
    fun available(t: Tab?): Boolean = t != null && !t.isNewTab && t.media.usableCount() > 0

    /**
     * A long press on a video or audio element goes straight to the sheet, aimed at THAT file: its own address (when the page gave one) is the exact match, everything else the page loaded
     * is ranked behind it (MediaRank). "Copy video address" and "Open link" are small buttons under the Download button instead of a menu in front of it.
     */
    fun longPress(tab: Tab, srcUri: String?, linkUri: String?, onLink: (String) -> Unit) {
        val direct = srcUri?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        // The element's own address is always worth offering, even when the sniffer has not seen the request (cached, or it ran before the extension was ready).
        if (direct != null && tab.media.find(direct) == null) tab.media.add(MediaCandidate(direct, MediaKind.FILE, "", -1, tab.url, "", System.currentTimeMillis()))
        val extras = ArrayList<Pair<String, () -> Unit>>()
        if (direct != null) extras.add("Copy video address" to { a.copyText(direct) })
        if (linkUri != null) extras.add("Open link in new tab" to { a.sheet.dismiss(); onLink(linkUri) })
        show(tab, direct, extras)
    }

    /** The sheet. [pressedUrl]: the address of the element the owner long-pressed, when known. */
    fun show(tab: Tab, pressedUrl: String? = null, extras: List<Pair<String, () -> Unit>> = emptyList()) {
        val ranked = MediaRank.rank(candidates(tab, pressedUrl), pressedUrl)
        if (ranked.main.isEmpty() && ranked.other.isEmpty()) {
            a.toast("No video found on this page yet. Some sites only show it once you press play: press play, then tap this icon again.")
            return
        }
        val p = VideoPanel(a, a.pal, object : VideoPanel.Listener {
            override fun download(choice: VideoPanel.Choice, stem: String) { when (val t = choice.tag) { is MediaCandidate -> pick(tab, t, stem); is StreamPick -> pickStream(tab, t, stem) } }
        })
        panel = p; panelTab = tab; panelPressed = pressedUrl; panelExtras = extras
        render(p, tab)
        a.sheet.show("Save video", p)
        for (c in ranked.main + listOfNotNull(ranked.exact)) { // only what is on top is looked into: a few hundred KB each, never the files tucked away
            if (c.kind == MediaKind.FILE && !c.audioOnly && (c.knownHeight == 0 || c.durationMs == 0L) && looksMp4(c) && probed.add(c.key)) probe(tab, c, 0L, 256 * 1024, 0)
            // a file found in the page's own markup has not been requested yet, so nobody has seen its length: one byte asked for gives it back as "Content-Range: bytes 0-0/TOTAL"
            else if (c.kind == MediaKind.FILE && c.length <= 0 && sized.add(c.key)) DownloadController.fetchBytes(a, c.url, 0L, 0L, requestHeaders(tab), 1, tab.isPrivateTab, onTotal = { n -> if (c.length <= 0) { c.length = n; refresh(tab) } }) { }
        }
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
        DownloadController.fetchBytes(a, c.url, from, from + length - 1, requestHeaders(tab), 1, tab.isPrivateTab, onTotal = { n -> if (c.length <= 0) c.length = n }) { bytes ->
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
    private fun stemFor(tab: Tab, label: String): String = MediaNames.clean(MediaLabels.fileStem(tab.title, UrlParts.hostOf(tab.url) ?: "", label))

    /** One line under the format (owner request X17): file type, size, length - whatever is known, in that order. */
    private fun rowDetail(vararg parts: String): String = parts.filter { it.isNotEmpty() && it != "size unknown" }.joinToString("  \u00B7  ")

    /** A row for one quality of a stream. [key] keeps it the same row when the sheet is drawn again. */
    private fun streamChoice(tab: Tab, c: MediaCandidate, pick: StreamPick, title: String, detail: String, ext: String): VideoPanel.Choice =
        VideoPanel.Choice("s|" + c.key + "|" + (pick.repId ?: pick.url) + "|" + pick.label, title, detail, stemFor(tab, pick.label), ext, pick)

    /** What the sheet shows: the ranked files (the long-pressed one first), the streams' quality lists as they arrive, and the small or odd files tucked under "Other files". */
    private fun model(tab: Tab): VideoPanel.Model {
        val ranked = MediaRank.rank(candidates(tab, panelPressed), panelPressed)
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
                        val stem = MediaNames.defaultStem(c, tab.title, host, if (c.audioOnly) "audio" else q)
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

    private fun requestHeaders(tab: Tab): Map<String, String> {
        val h = HashMap<String, String>()
        UrlParts.parse(tab.url)?.let { h["Referer"] = "${it.scheme}://${it.hostForAuthority}/"; h["Origin"] = "${it.scheme}://${it.hostForAuthority}" } // the page's origin only, never its full address
        return h
    }

    private fun loadStream(tab: Tab, c: MediaCandidate): Loaded {
        streamInfo[c.key] = Loaded.Loading
        val headers = requestHeaders(tab)
        DownloadController.fetchText(a, c.url, headers, 1, tab.isPrivateTab) { text ->
            val pl = text?.let { Hls.parse(it, c.url) }
            when (pl) {
                null -> { streamInfo[c.key] = Loaded.Failed("The stream's list could not be read (the site may have refused it)."); refresh(tab) }
                is Hls.MediaPlaylist -> { streamInfo[c.key] = ready(tab, c, null, pl.media, text, null); refresh(tab); sizeBareStream(tab, c, pl.media, text) }
                is Hls.MasterPlaylist -> loadVariants(tab, c, pl.master, headers)
            }
        }
        return Loaded.Loading
    }

    /** One variant's playlist tells how long the video is (all variants are equally long); with that, every variant's size is its bit rate times it. */
    private fun loadVariants(tab: Tab, c: MediaCandidate, master: Hls.Master, headers: Map<String, String>) {
        val vs = master.variants
        for (v in vs) children.add(v.url)
        for (r in master.renditions) r.url?.let { children.add(it) }
        if (vs.isEmpty()) { streamInfo[c.key] = Loaded.Failed("This stream lists no qualities."); refresh(tab); return }
        val probe = vs.filter { !it.audioOnly }.minByOrNull { it.bandwidth } ?: vs.minByOrNull { it.bandwidth }!!
        DownloadController.fetchText(a, probe.url, headers, 1, tab.isPrivateTab) { text ->
            val media = (text?.let { Hls.parse(it, probe.url) } as? Hls.MediaPlaylist)?.media
            streamInfo[c.key] = if (media == null) Loaded.Failed("The stream's list could not be read.") else ready(tab, c, master, media, text, probe)
            refresh(tab)
        }
    }

    /**
     * A playlist that is not part of a master has no bit rate to size it by. Ask the server how long two of its segments are (a one-byte Range request each: only the headers matter) and
     * multiply the average by the number of segments: good enough for "about 6 MB". The list is redrawn when the answers are in.
     */
    private fun sizeBareStream(tab: Tab, c: MediaCandidate, media: Hls.Media, text: String) {
        val n = media.segments.size
        if (media.exactBytes >= 0 || n == 0 || media.segments[0].len >= 0) return
        val picks = if (n >= 3) listOf(0, n / 2) else listOf(0)
        val got = ArrayList<Long>()
        for (i in picks) DownloadController.fetchBytes(a, media.segments[i].url, 0L, 0L, requestHeaders(tab), 1, tab.isPrivateTab, onTotal = { len ->
            got.add(len)
            if (got.size == picks.size) {
                val factor = if (media.container == "ts") 0.95 else 1.0               // the MP4 made of a transport stream is a little smaller than the stream
                val guess = (got.average() * n * factor).toLong()
                val cur = streamInfo[c.key]
                if (cur is Loaded.Ready) { streamInfo[c.key] = ready(tab, c, null, media, text, null, guess); refresh(tab) }
            }
        }) { }
    }

    private fun ready(tab: Tab, c: MediaCandidate, master: Hls.Master?, media: Hls.Media, mediaText: String?, probe: Hls.Variant?, sizeGuess: Long = -1L): Loaded {
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
            val pick = StreamPick(if (audio) "audio" else "", c.url, mediaText, est, null, audio, media.container)
            val ext = if (audio) media.container else "mp4"          // a video stream is saved as one MP4 file
            rows.add(streamChoice(tab, c, pick, if (audio) "Audio only" else "Video", rowDetail(ext.uppercase(), MediaLabels.size(est, estimate = media.exactBytes < 0 && est > 0), dur), ext) to pick)
            return Loaded.Ready(rows, null)
        }
        val seen = HashSet<String>()
        for (v in master.variants.sortedWith(compareBy<Hls.Variant> { it.audioOnly }.thenByDescending { it.height }.thenByDescending { it.bandwidth })) {
            if (!seen.add("${v.height}|${v.bandwidth / 100_000}|${v.audioOnly}")) continue
            val est = v.bytesPerSecond * (media.durationMs / 1000)
            val sepAudio = master.audioFor(v)
            val text = if (v === probe) mediaText else null
            val title = if (v.audioOnly) "Audio only" else v.label
            val pick = StreamPick(if (v.audioOnly) "audio" else v.label, v.url, text, est, sepAudio?.url, v.audioOnly, media.container)
            val ext = if (v.audioOnly) media.container else "mp4"
            val detail = rowDetail(ext.uppercase(), MediaLabels.size(est, estimate = true), dur)
            rows.add(streamChoice(tab, c, pick, title, detail, ext) to pick)
        }
        return Loaded.Ready(rows, null)
    }

    // ---- DASH: the manifest lists picture and sound representations; the chosen picture is downloaded together with the best AAC sound and merged into one MP4

    private fun loadDash(tab: Tab, c: MediaCandidate): Loaded {
        streamInfo[c.key] = Loaded.Loading
        DownloadController.fetchText(a, c.url, requestHeaders(tab), 1, tab.isPrivateTab) { text ->
            val man = text?.let { Dash.parse(it, c.url) }
            streamInfo[c.key] = if (man == null || text == null) Loaded.Failed("The stream's list could not be read (the site may have refused it).") else dashReady(tab, c, man, text)
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
            val pick = StreamPick(v.label, c.url, text, est, null, false, "mp4", repId = v.id, audioRepId = sound?.id)
            rows.add(streamChoice(tab, c, pick, v.label, rowDetail("MP4", MediaLabels.size(est, estimate = true), dur), "mp4") to pick)
        }
        for (au in man.audio.filter { it.mp4Friendly && it.media != null }.take(2)) {
            val est = au.bytesPerSecond * (man.durationMs / 1000)
            val pick = StreamPick("audio", c.url, text, est, null, true, "m4a", repId = au.id, ext = "m4a")
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
            val h = requestHeaders(tab)
            val page = tab.url.takeIf { it.isNotEmpty() }
            val soundStem = "$stem (sound)"
            // the sound of a picture that comes separately is downloaded alongside and merged into one MP4 when both are in (if the merge is refused, both files stay)
            val companion = when {
                p.audioRepId != null -> StreamSource(p.url, p.text, page, soundStem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, repId = p.audioRepId, ext = "m4a")
                p.audioUrl != null -> StreamSource(p.audioUrl, null, page, soundStem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true)
                else -> null
            }
            DownloadController.enqueueStream(a, StreamSource(p.url, p.text, page, stem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, estimateBytes = p.estimate, repId = p.repId, ext = p.ext, companion = companion))
            a.askNotificationPermissionOnce()
            a.toast("Download started: Menu > Downloads")
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

    private fun pick(tab: Tab, c: MediaCandidate, stem: String) {
        a.sheet.dismiss()
        val name = stem + "." + MediaSniff.saveExtension(c.url, c.mime)
        a.download(tab, c.url, null, null, c.mime.ifEmpty { null }, c.length, nameOverride = name)
    }
}
