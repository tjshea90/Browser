package app.kestrel.browser.web

import android.app.AlertDialog
import app.kestrel.browser.BrowserActivity
import app.kestrel.browser.Io
import app.kestrel.browser.ui.VideoPanel
import app.kestrel.core.media.MediaCandidate
import app.kestrel.core.media.MediaKind
import app.kestrel.core.media.MediaLabels
import app.kestrel.core.media.MediaSniff
import app.kestrel.core.url.UrlParts

/**
 * "Download video": what the page's sniffed media becomes in the UI. The extension reports every media file / playlist a page loads ([seen]), the page's own players report what they
 * know about them ([info]); a long press on a video or the menu entry opens the sheet ([show]) and a tap on a row starts an ordinary download, so pause / resume / notifications / the
 * metered-link questions are the ones every download already has. Design and limits: research/VIDEO_DOWNLOADER.md.
 */
class VideoFlow(private val a: BrowserActivity) {

    /** Called on the Shields worker thread. */
    fun seen(c: MediaCandidate) = Io.ui {
        val t = a.tabForPage(c.page) ?: a.current ?: return@ui
        t.media.add(c)
    }

    /** Called on the Shields worker thread. */
    fun info(page: String, url: String, width: Int, height: Int, durationMs: Long, label: String?) = Io.ui {
        val t = (if (page.isNotEmpty()) a.tabForPage(page) else null) ?: a.current ?: return@ui
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
        val panel = VideoPanel(a, a.pal) { choice -> (choice.tag as? MediaCandidate)?.let { pick(tab, it) } }
        panel.show(tab.displayTitle, sections(tab, items))
        a.sheet.show("Download video", panel)
    }

    private fun sections(tab: Tab, items: List<MediaCandidate>): List<VideoPanel.Section> {
        val video = ArrayList<VideoPanel.Choice>()
        val audio = ArrayList<VideoPanel.Choice>()
        val streams = ArrayList<VideoPanel.Choice>()
        for (c in items) {
            when {
                c.kind != MediaKind.FILE -> streams.add(VideoPanel.Choice(MediaLabels.format(c), "Streams are not downloadable yet", c, enabled = false))
                c.audioOnly -> audio.add(VideoPanel.Choice("Audio only", detail(c), c))
                else -> video.add(VideoPanel.Choice(MediaLabels.quality(c.knownHeight).ifEmpty { "Video" }, detail(c), c))
            }
        }
        return listOf(VideoPanel.Section("Video", video), VideoPanel.Section("Audio", audio), VideoPanel.Section("Streams", streams))
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
