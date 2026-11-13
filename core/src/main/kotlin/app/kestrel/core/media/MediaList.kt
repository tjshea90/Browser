package app.kestrel.core.media

import app.kestrel.core.update.UpdateLogic

/** One thing on the current page that could be saved (what the sniffer saw, plus what the page itself says about it). UI thread only. */
class MediaCandidate(
    val url: String,
    val kind: MediaKind,
    /** Content type without parameters, "" if unknown. */
    var mime: String,
    /** Bytes (the whole file, from Content-Length / Content-Range); -1 = unknown. Always -1 for a playlist. */
    var length: Long,
    /** The top-level page when it was seen. */
    val page: String,
    /** The document that asked for it (an embedded player's own page), "" if the same as [page]. */
    val frame: String = "",
    val seenAt: Long = 0,
) {
    /** The file name the SERVER gave it (Content-Disposition), "" when it gave none: the best name to offer when saving it. */
    var sourceName: String = ""
    var width = 0
    var height = 0
    var durationMs = 0L
    /** What the page calls it ("720p", "HD") when it said so. */
    var label: String? = null
    /**
     * What the browser itself sent when it loaded this address (the extension saw the request): its Referer and Origin. A video server that checks "who is asking" wants exactly these (the embedded
     * PLAYER's address, not the page the player sits in), so the downloads repeat them. "" = not seen (an address found in the page's markup, or a long-pressed element).
     */
    var referer: String = ""
    var origin: String = ""
    /** The request headers that were seen to WORK for this address (its playlist was read with them): every later request of the download repeats them. Null until one worked. */
    var viaHeaders: Map<String, String>? = null

    val key: String get() = keyOf(url)
    val audioOnly: Boolean get() = kind == MediaKind.FILE && MediaSniff.isAudio(url, mime)

    /** The picture height we know of: the page's own player, else the page's label, else a hint in the URL; 0 = unknown. */
    val knownHeight: Int get() = if (height > 0) height else heightOfLabel(label).takeIf { it > 0 } ?: MediaSniff.heightFromUrl(url)

    companion object {
        fun keyOf(url: String): String = url.substringBefore('#')

        /** "720p" / "HD 720" / "1080" -> 720 / 720 / 1080; 0 if the text names no resolution. */
        private val LABEL_HEIGHT = Regex("""(\d{3,4})\s*p?""", RegexOption.IGNORE_CASE) // compiled once: the sort below asks for it on every refresh of the download button

        fun heightOfLabel(label: String?): Int {
            if (label == null) return 0
            val m = LABEL_HEIGHT.find(label) ?: return 0
            val h = m.groupValues[1].toIntOrNull() ?: return 0
            return if (h in 144..4320) h else 0
        }
    }
}

/** What a tab has seen, newest first. The same URL is one entry however many Range requests the player made. */
class MediaList(private val max: Int = 24) {
    private val items = ArrayList<MediaCandidate>()

    val size: Int get() = items.size
    fun isEmpty(): Boolean = items.isEmpty()
    /** The candidate for exactly this address (the #fragment aside), or null. */
    fun find(url: String): MediaCandidate? = MediaCandidate.keyOf(url).let { k -> items.firstOrNull { it.key == k } }
    fun clear() { items.clear(); version++ }
    /** The addresses of everything in the list (for forgetting what was learned about them). */
    fun keys(): List<String> = items.map { it.key }
    /**
     * A player frame went to another document (the owner switched servers): what the documents in [docs] offered is gone. [MediaCandidate.frame] names the document that asked ("" = the page itself, never
     * dropped here: a new page clears the whole list). @return the addresses removed.
     */
    fun dropFrames(docs: Collection<String>): List<String> {
        if (docs.isEmpty()) return emptyList()
        val gone = items.filter { it.frame.isNotEmpty() && it.frame in docs }
        if (gone.isEmpty()) return emptyList()
        items.removeAll(gone.toSet()); version++
        return gone.map { it.key }
    }

    // The dock's download button asks "how many?" on every refresh of the screen (page progress, title, shield counter...): the sorted list is only rebuilt when the list changed.
    private var version = 0
    private var countVersion = -1
    private var countCache = 0
    fun usableCount(): Int {
        if (countVersion != version) { countCache = usableUnsorted().size; countVersion = version }
        return countCache
    }

    /** @return true if the list changed (a new URL, or a better size for a known one). */
    fun add(c: MediaCandidate): Boolean {
        val i = items.indexOfFirst { it.key == c.key }
        if (i >= 0) {
            val old = items[i]
            var changed = false
            if (old.length < 0 && c.length >= 0) { old.length = c.length; changed = true }
            if (old.mime.isEmpty() && c.mime.isNotEmpty()) { old.mime = c.mime; changed = true }
            if (old.referer.isEmpty() && c.referer.isNotEmpty()) { old.referer = c.referer; changed = true }
            if (old.origin.isEmpty() && c.origin.isNotEmpty()) { old.origin = c.origin; changed = true }
            if (changed) version++
            return changed
        }
        items.add(0, c)
        while (items.size > max) items.removeAt(items.size - 1)
        version++
        return true
    }

    /** What the page's own `<video>` / `<source>` told us about a URL (picture size, length in time, the label it shows). */
    fun applyInfo(url: String, width: Int, height: Int, durationMs: Long, label: String?): Boolean {
        val c = items.firstOrNull { it.key == MediaCandidate.keyOf(url) } ?: return false
        var changed = false
        if (width > 0 && c.width != width) { c.width = width; changed = true }
        if (height > 0 && c.height != height) { c.height = height; changed = true }
        if (durationMs > 0 && c.durationMs != durationMs) { c.durationMs = durationMs; changed = true }
        if (!label.isNullOrBlank() && c.label != label) { c.label = label.trim().take(24); changed = true }
        if (changed) version++
        return changed
    }

    /**
     * What is worth offering: playlists and big files first. Tiny files (sounds, tracking clips) and, when the page has a stream, small clips next to it (ads, previews) are left out.
     */
    fun usable(): List<MediaCandidate> = usableUnsorted().sortedWith(compareBy<MediaCandidate> { kindRank(it) }.thenByDescending { it.knownHeight }.thenByDescending { it.length })

    private fun usableUnsorted(): List<MediaCandidate> {
        val hasStream = items.any { it.kind != MediaKind.FILE }
        return items.filter { c ->
            when {
                c.kind != MediaKind.FILE -> true
                c.length in 0 until MIN_FILE -> false
                hasStream && c.length in 0 until MIN_WITH_STREAM -> false
                else -> true
            }
        }
    }

    private fun kindRank(c: MediaCandidate): Int = when {
        c.kind == MediaKind.HLS -> 0
        c.kind == MediaKind.DASH -> 1
        c.audioOnly -> 3
        else -> 2
    }

    companion object {
        const val MIN_FILE = 100L * 1024
        const val MIN_WITH_STREAM = 4L * 1024 * 1024
    }
}

/** The words next to a download button. */
object MediaLabels {
    /** "1080p", "4K", "Original quality" ... for a picture height (0 = unknown). */
    fun quality(height: Int): String = when {
        height >= 2000 -> "4K ($height" + "p)"
        height >= 1400 -> "1440p"
        height > 0 -> "$height" + "p"
        else -> ""
    }

    /** Size for a row: exact for a file ("48.2 MB"), "about 210 MB" for an estimate, "size unknown". */
    fun size(bytes: Long, estimate: Boolean = false): String = when {
        bytes <= 0 -> "size unknown"
        estimate -> "about " + UpdateLogic.formatSize(bytes)
        else -> UpdateLogic.formatSize(bytes)
    }

    /** "0:48", "12:34", "1:02:03" */
    fun duration(ms: Long): String {
        if (ms <= 0) return ""
        val total = (ms + 500) / 1000
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** The format word: "MP4", "WEBM", "MP3", "HLS stream"... */
    fun format(c: MediaCandidate): String = when (c.kind) {
        MediaKind.HLS -> "HLS stream"
        MediaKind.DASH -> "DASH stream"
        MediaKind.FILE -> MediaSniff.saveExtension(c.url, c.mime).uppercase()
    }

    /** A readable file name stem for a video: "Page title - 720p", falling back to the host. No extension, nothing a file system dislikes beyond what [app.kestrel.core.download.FileNames.sanitize] cleans. */
    fun fileStem(pageTitle: String, pageHost: String, qualityLabel: String): String {
        val base = pageTitle.trim().ifEmpty { pageHost.ifEmpty { "video" } }.replace(Regex("""\s+"""), " ").take(80).trim()
        return if (qualityLabel.isEmpty()) base else "$base - $qualityLabel"
    }
}
