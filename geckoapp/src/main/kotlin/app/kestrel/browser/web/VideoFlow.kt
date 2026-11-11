package app.kestrel.browser.web

import android.app.AlertDialog
import app.kestrel.browser.BrowserActivity
import app.kestrel.browser.Io
import app.kestrel.browser.ui.VideoPanel
import app.kestrel.browser.download.DownloadController
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.core.media.Hls
import app.kestrel.core.media.MediaCandidate
import app.kestrel.core.media.MediaKind
import app.kestrel.core.media.MediaLabels
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
    private class StreamPick(val label: String, val url: String, val text: String?, val estimate: Long, val audioUrl: String?, val audioOnly: Boolean, val container: String)
    private sealed class Loaded {
        object Loading : Loaded()
        class Failed(val message: String) : Loaded()
        class Ready(val rows: List<Pair<VideoPanel.Choice, StreamPick>>, val note: String?) : Loaded()
    }
    private val streamInfo = HashMap<String, Loaded>()   // candidate key -> what its playlist turned out to be
    private val probed = HashSet<String>()              // files whose first bytes were already read to learn their picture size
    private val children = HashSet<String>()            // addresses of the variant / audio playlists of masters we have read: they are not rows of their own
    private var panel: VideoPanel? = null
    private var panelTab: Tab? = null

    /** Called on the Shields worker thread. */
    fun seen(c: MediaCandidate) = Io.ui {
        val t = a.tabWithUrl(c.page) ?: a.tabForPage(c.page) ?: a.current ?: return@ui // the tab on exactly that page first; the same host or the selected tab only as a fallback
        t.media.add(c)
    }

    /** Called on the Shields worker thread. */
    fun info(page: String, url: String, width: Int, height: Int, durationMs: Long, label: String?) = Io.ui {
        val t = (if (page.isNotEmpty()) a.tabWithUrl(page) ?: a.tabForPage(page) else null) ?: a.current ?: return@ui
        t.media.applyInfo(url, width, height, durationMs, label)
    }

    /** Anything worth offering on this tab's page? (the menu shows its entry only then) */
    fun available(t: Tab?): Boolean = t != null && !t.isNewTab && t.media.usable().isNotEmpty()

    /** The long-press menu of a video or audio element. */
    fun longPress(tab: Tab, srcUri: String?, linkUri: String?, onLink: (String) -> Unit) {
        val labels = ArrayList<String>(); val acts = ArrayList<() -> Unit>()
        val direct = srcUri?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        // The element's own address is always worth offering, even when the sniffer has not seen the request (cached, or it ran before the extension was ready).
        if (direct != null && tab.media.usable().none { it.url == direct }) tab.media.add(MediaCandidate(direct, MediaKind.FILE, "", -1, tab.url, "", System.currentTimeMillis()))
        labels.add("Download video"); acts.add { show(tab) }
        if (direct != null) { labels.add("Copy video address"); acts.add { a.copyText(direct) } }
        if (linkUri != null) { labels.add("Open link in new tab"); acts.add { onLink(linkUri) } }
        AlertDialog.Builder(a).setTitle((tab.displayTitle).take(80)).setItems(labels.toTypedArray()) { _, i -> acts[i]() }.show()
    }

    /** The sheet. */
    fun show(tab: Tab) {
        val items = tab.media.usable()
        if (items.isEmpty()) {
            a.toast("No video found on this page yet. Start playing it for a few seconds, then try again.")
            return
        }
        val p = VideoPanel(a, a.pal) { choice -> when (val t = choice.tag) { is MediaCandidate -> pick(tab, t); is StreamPick -> pickStream(tab, t) } }
        panel = p; panelTab = tab
        render(p, tab, items)
        a.sheet.show("Download video", p)
        for (c in items) if (c.kind == MediaKind.FILE && !c.audioOnly && (c.knownHeight == 0 || c.durationMs == 0L) && looksMp4(c) && probed.add(c.key)) probe(tab, c, 0L, 256 * 1024, 0)
    }

    private fun looksMp4(c: MediaCandidate): Boolean = MediaSniff.extension(c.url) in setOf("mp4", "m4v", "mov") || c.mime == "video/mp4" || c.mime == "video/quicktime"

    /** Reads the start of an MP4 (and, when its index sits at the end, that box) to name its picture size and length: a few hundred KB, only when the sheet is open. */
    private fun probe(tab: Tab, c: MediaCandidate, from: Long, length: Int, depth: Int) {
        DownloadController.fetchBytes(a, c.url, from, from + length - 1, requestHeaders(tab), 1, tab.isPrivateTab) { bytes ->
            if (bytes == null || depth > 3) return@fetchBytes
            when (val r = Mp4Probe.probe(bytes, bytes.size, from)) {
                is Mp4Probe.Result.Found -> { if (c.height == 0 && r.info.height > 0) { c.width = r.info.width; c.height = r.info.height }; if (c.durationMs == 0L) c.durationMs = r.info.durationMs; refresh(tab) }
                is Mp4Probe.Result.NeedMore -> if (c.length <= 0 || r.offset < c.length) probe(tab, c, r.offset, r.length.coerceIn(16 * 1024, 4 * 1024 * 1024), depth + 1)
                Mp4Probe.Result.NotMp4 -> {}
            }
        }
    }

    private fun render(p: VideoPanel, tab: Tab, items: List<MediaCandidate>) {
        val (sections, note) = sections(tab, items)
        p.show(tab.displayTitle, sections, note)
    }

    /** A playlist finished loading: redraw the sheet if it is still open on this tab. */
    private fun refresh(tab: Tab) {
        val p = panel ?: return
        if (panelTab !== tab || !a.sheet.isShowing || p.parent == null) return
        render(p, tab, tab.media.usable())
    }

    private fun sections(tab: Tab, items: List<MediaCandidate>): Pair<List<VideoPanel.Section>, String?> {
        val video = ArrayList<VideoPanel.Choice>()
        val audio = ArrayList<VideoPanel.Choice>()
        val streams = ArrayList<VideoPanel.Choice>()
        val streamAudio = ArrayList<VideoPanel.Choice>()
        var note: String? = null
        val names = HashMap<String, Int>()
        for (c in items) {
            when {
                c.kind == MediaKind.HLS && c.key in children -> {}
                c.kind == MediaKind.HLS -> when (val st = streamInfo[c.key] ?: loadStream(tab, c)) {
                    is Loaded.Loading -> streams.add(VideoPanel.Choice("Stream", "Reading the quality list\u2026", null, enabled = false))
                    is Loaded.Failed -> note = note ?: st.message
                    is Loaded.Ready -> for ((ch, pick) in st.rows) (if (pick.audioOnly) streamAudio else streams).add(ch)
                }
                c.kind == MediaKind.DASH -> note = note ?: "This page streams in a format Kestrel cannot save yet (DASH)."
                c.audioOnly -> audio.add(VideoPanel.Choice("Audio only", detail(c), c))
                else -> video.add(VideoPanel.Choice(MediaLabels.quality(c.knownHeight).ifEmpty { "Video" }, detail(c), c))
            }
        }
        // two rows that read the same (two files of one quality) get the file's name so they can be told apart
        for (list in listOf(video, audio)) for (ch in list) names.merge(ch.title + "|" + ch.detail, 1, Int::plus)
        fun named(list: List<VideoPanel.Choice>) = list.map { ch -> if ((names[ch.title + "|" + ch.detail] ?: 0) > 1 && ch.tag is MediaCandidate) VideoPanel.Choice(ch.title, ch.detail + "  \u00B7  " + shortName(ch.tag.url), ch.tag) else ch }
        return listOf(VideoPanel.Section("Video", named(video)), VideoPanel.Section("Stream", streams), VideoPanel.Section("Audio", named(audio) + streamAudio)) to note
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
                is Hls.MediaPlaylist -> { streamInfo[c.key] = ready(tab, c, null, pl.media, text, null); refresh(tab) }
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

    private fun ready(tab: Tab, c: MediaCandidate, master: Hls.Master?, media: Hls.Media, mediaText: String?, probe: Hls.Variant?): Loaded {
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
            val est = media.exactBytes
            val pick = StreamPick(if (audio) "audio" else "", c.url, mediaText, est, null, audio, media.container)
            rows.add(VideoPanel.Choice(if (audio) "Audio only" else "Video", listOf("HLS", MediaLabels.size(est), dur).filter { it.isNotEmpty() && !(it == "size unknown") }.joinToString("  \u00B7  "), pick) to pick)
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
            val detail = listOf("HLS", MediaLabels.size(est, estimate = true), dur, if (sepAudio != null) "picture and sound as two files" else "").filter { it.isNotEmpty() }.joinToString("  \u00B7  ")
            rows.add(VideoPanel.Choice(title, detail, pick) to pick)
        }
        return Loaded.Ready(rows, null)
    }

    private fun pickStream(tab: Tab, p: StreamPick) {
        a.sheet.dismiss()
        val host = UrlParts.hostOf(tab.url) ?: ""
        val stem = MediaLabels.fileStem(tab.title, host, p.label)
        val size = MediaLabels.size(p.estimate, estimate = true)
        val go = {
            val h = requestHeaders(tab)
            DownloadController.enqueueStream(a, StreamSource(p.url, p.text, tab.url.takeIf { it.isNotEmpty() }, stem, h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true, estimateBytes = p.estimate))
            if (p.audioUrl != null) DownloadController.enqueueStream(a, StreamSource(p.audioUrl, null, tab.url.takeIf { it.isNotEmpty() }, MediaLabels.fileStem(tab.title, host, "sound"), h, fetcher = 1, priv = tab.isPrivateTab, meteredOk = true))
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

    private fun pick(tab: Tab, c: MediaCandidate) {
        a.sheet.dismiss()
        val q = MediaLabels.quality(c.knownHeight)
        val stem = MediaLabels.fileStem(tab.title, UrlParts.hostOf(tab.url) ?: "", if (c.audioOnly) "audio" else q)
        val name = stem + "." + MediaSniff.saveExtension(c.url, c.mime)
        a.download(tab, c.url, null, null, c.mime.ifEmpty { null }, c.length, nameOverride = name)
    }
}
