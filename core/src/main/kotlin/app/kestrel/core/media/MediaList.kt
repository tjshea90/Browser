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
    var page: String,
    /** The document that asked for it (an embedded player's own page), "" if the same as [page]. The newest sighting names it ([MediaList.add]). */
    var frame: String = "",
    val seenAt: Long = 0,
) {
    /** NM1: the addresses of the frames AROUND the one that asked (an embed inside an embed), nearest first, never the top page: each doraby server's embed address names the season and episode. */
    var frameChain: List<String> = emptyList()
    /** The file name the SERVER gave it (Content-Disposition), "" when it gave none: the best name to offer when saving it. */
    var sourceName: String = ""
    var width = 0
    var height = 0
    var durationMs = 0L
    /** What the page calls it ("720p", "HD") when it said so. */
    var label: String? = null
    /** Somebody showed interest in it (the page's player reported it, it was long-pressed, opened in the sheet or downloaded): it is never pushed out of a full list and never hidden as a preview ([MediaList]). */
    var touched = false
    /** RX3-V3: an element really PLAYED this file (3 s of playback, reported by the page): [element] is that element's number on its page, [areaPx] the part of its box that was on screen, [playedSeq] the order of such reports. */
    var played = false
    var element = 0
    var areaPx = 0L
    var playedSeq = 0
    /** RX3-V2: found only in the page's text (a link, an og tag, a string in a script): never requested, so nobody has seen what it is. See [verified]. */
    var fromMarkup = false
    /**
     * RX3-V2: worth counting for the dock badge. Seen on the wire (a request really happened, with a media type) = yes; found only in the page's text = not until somebody played it (a player
     * reported it), the owner showed interest, or its length is known and at least [MediaList.MIN_FILE]. Unverified entries are still offered, under "Other files".
     */
    val verified: Boolean get() = touched || !fromMarkup || (kind == MediaKind.FILE && length >= MediaList.MIN_FILE)
    /** RX3-V5: the early look at this stream's playlist happened. [live] and [drm] streams cannot be saved; [adMs] / [adBreaks] are the advertising its markers or foreign runs give away. */
    var looked = false
    /** ANS55: the early look (or the quality list) read this playlist as a MASTER (it lists variants), not as a media list of one rendition (video, sound or subtitles of the same film). */
    var master = false
    /** ANS55: when the page last asked for this address (a player that moves on to another video asks for that video's master last); [seenAt] is the first time. */
    var lastSeenAt = seenAt
    var live = false
    var drm = false
    var adMs = 0L
    var adBreaks = 0
    /** RX3-V4: the picture heights of the stream's variants (an HLS master's variants, a DASH manifest's video representations) the early look read; empty = not known. A player that feeds a blob: address is matched by them. */
    var heights: IntArray = IntArray(0)
    /** RX3-V5: a stream whose playlist was read and that cannot be saved (live, protected) or is only a clip (under 20 s: an ad, a preview) is not offered to the badge. Before the look nothing is known and it counts as before. */
    val unfit: Boolean get() = kind != MediaKind.FILE && looked && (live || drm || (durationMs in 1 until MediaList.MIN_STREAM_MS))
    /**
     * VS2 (owner 2026-11-18, doraby + vidsrc / 111movies: "it would show a video file can be downloaded in the list, then it would disappear after a few seconds ... is there a way to capture the video source when it is
     * found so it doesn't disappear"): once a film-sized candidate was OFFERED (the badge counted it) it is CAPTURED: a frame that reloads or goes away, a clip rule or an advertisement label that arrives later cannot
     * take it out of the list any more. Only the person leaving the page ([MediaList.onNewPage]) or a stream that cannot be saved (live, protected) ends it.
     */
    var captured = false
    /** VS2: a captured entry whose player frame went away (or whose page was loaded again): still offered, last in the list, until a new film-sized find of a live player takes its place ([MediaList.add]). */
    var orphan = false
    /** What the dock badge counts: not decoration or an advertisement, not an address nobody has seen, not a stream that cannot be saved. A captured one counts whatever is learned later (unless it cannot be saved). */
    fun counts(): Boolean = (captured && !live && !drm) || (!decoy && verified && !unfit)
    /** RX3-V1: what the page's own element says it is for ([VideoRole]); null = no element said anything yet (or an older extension). The latest report wins. */
    var role: VideoRole? = null
    /** The OWNER showed interest (long-pressed, opened in the sheet, downloaded): it counts and shows whatever the page's element says it is. */
    var interest = false
    /** Decoration or an advertisement by the page's own account, and the owner has not said otherwise: not counted, tucked under "Other files", the first to go from a full list. */
    val decoy: Boolean get() = (role == VideoRole.DECOR || role == VideoRole.AD) && !interest
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
class MediaList(private val max: Int = 48) {
    private val items = ArrayList<MediaCandidate>()
    /** What a page's element said about a file the sniffer has not reported yet (the element reports on loadedmetadata, the extension on the response: either can come first). Bounded; applied when the file is added. */
    private class Said(val width: Int, val height: Int, val durationMs: Long, val label: String?, val role: VideoRole?, val played: Boolean, val element: Int, val areaPx: Long)
    private var playSeq = 0
    private val pending = LinkedHashMap<String, Said>()
    /** RX3-V4: what a player that feeds a blob: address said last (picture height, length, role, whether it plays), by element number; newest last, bounded. A playlist the early look reads later is matched against it too. */
    private class Blob(val height: Int, val durationMs: Long, val role: VideoRole?, val played: Boolean, val areaPx: Long)
    private val blobs = LinkedHashMap<Int, Blob>()
    internal fun blobElements(): Int = blobs.size

    val size: Int get() = items.size
    fun isEmpty(): Boolean = items.isEmpty()
    /** The candidate for exactly this address (the #fragment aside), or null. */
    fun find(url: String): MediaCandidate? = MediaCandidate.keyOf(url).let { k -> items.firstOrNull { it.key == k } }
    fun clear() { items.clear(); pending.clear(); blobs.clear(); version++ }

    /**
     * VS2: what is offered right now AND looks like a film is captured. Film evidence = a stream whose playlist was read and runs [FILM_EVIDENCE_MS] or more, a file of [BIG_FILE] or more (or 4 MB with that length
     * of picture), or anything the person opened. A clip, a preview or an advertisement is NOT captured: the clip and advertisement rules keep working for them. @return how many were newly captured.
     */
    fun capture(): Int {
        var n = 0; var liveFilm = false
        for (c in items) {
            if (c.captured || c.live || c.drm || !c.counts()) continue
            if (!(c.interest || filmEvidence(c))) continue
            c.captured = true; n++
            if (!c.orphan) liveFilm = true
        }
        // a live player now offers a film of its own: the kept one of the player that went away has done its job (what the owner opened stays)
        if (liveFilm && items.any { it.orphan }) items.removeAll { it.orphan && !it.interest }
        if (n > 0) version++
        return n
    }

    private fun filmEvidence(c: MediaCandidate): Boolean {
        if (c.decoy) return false
        if (c.kind != MediaKind.FILE) return c.looked && c.durationMs >= FILM_EVIDENCE_MS
        return c.length >= BIG_FILE || (c.length >= MIN_WITH_STREAM && c.durationMs >= FILM_EVIDENCE_MS)
    }

    /**
     * VS2: the tab starts [url]. The same page again (a reload, the same address with another query, a server switch that reloads the page) keeps what was captured, a different page forgets everything as before.
     * The facts that belonged to the old document's elements (which element played, how much of it was on screen) are forgotten: the new document numbers its elements again. @return how many entries were dropped.
     */
    fun onNewPage(url: String): Int {
        capture()
        val id = PageIdentity.of(url)
        val keep = if (id.isEmpty()) emptyList() else items.filter { it.captured && !it.live && !it.drm && PageIdentity.of(it.page) == id }
        val dropped = items.size - keep.size
        for (c in keep) { c.played = false; c.element = 0; c.areaPx = 0L; c.playedSeq = 0; c.orphan = true }
        items.clear(); items.addAll(keep); pending.clear(); blobs.clear(); version++
        return dropped
    }
    /** The addresses of everything in the list (for forgetting what was learned about them). */
    fun keys(): List<String> = items.map { it.key }

    /** What the list holds, for the Diagnostics events: kind and the asking document (host and path only, never a query). */
    fun describe(): String = items.joinToString(" | ") { it.kind.name + " len=" + it.length + " f=" + (if (it.frame.isEmpty()) "-" else it.frame.substringAfter("://").substringBefore('?').take(40)) + " p=" + it.page.substringAfter("://").substringBefore('?').take(40) }
    /**
     * A player frame went to another document (the owner switched servers): what the documents in [docs] offered is gone. [MediaCandidate.frame] names the document that asked ("" = the page itself, never
     * dropped here: a new page clears the whole list). @return the addresses removed.
     */
    fun dropFrames(docs: Collection<String>): List<String> {
        if (docs.isEmpty()) return emptyList()
        capture()                                                       // (what the person could see a moment ago is kept: VS2)
        val inFrame = items.filter { it.frame.isNotEmpty() && it.frame in docs }
        if (inFrame.isEmpty()) return emptyList()
        // kept, last in the list, until a live player offers a film of its own: what was captured, and a stream nobody has read yet (it may well be the film; a clip is dropped as before)
        fun keeps(c: MediaCandidate) = c.captured || (c.kind != MediaKind.FILE && !c.looked && c.counts())
        val gone = inFrame.filter { !keeps(it) }
        for (c in inFrame) if (keeps(c)) c.orphan = true
        items.removeAll(gone.toSet()); version++
        return gone.map { it.key }
    }

    // The dock's download button asks "how many?" on every refresh of the screen (page progress, title, shield counter...): the sorted list is only rebuilt when the list changed.
    private var version = 0
    private var countVersion = -1
    private var countCache = 0
    /** True when this page loaded a stream playlist (HLS or DASH): it holds a video player, and a page with one cannot enter the engine's back/forward cache (RX3-BK2). */
    fun hasStream(): Boolean = items.any { it.kind != MediaKind.FILE }

    fun usableCount(): Int {
        if (countVersion != version) {
            capture()
            countCache = usableUnsorted().count { it.counts() && (it.captured || !clipNextToFilm(it, items)) }; countVersion = version
        }
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
            // the newest sighting names who asks NOW: an address found in a player's script, asked for later by the next server's player, belongs to the second one (a server switch drops by document)
            if (c.frame.isNotEmpty() && old.frame != c.frame) { old.frame = c.frame; old.page = c.page; changed = true }
            // RVW1 F2: an address first named in the page's text and then really REQUESTED (a request with a media type = the wire) is no longer "found only in the text": it stays unverified forever otherwise, so the badge
            // and the menu entry (VideoFlow.available) never lit for a stream a script named before the player asked for it (inline JSON, then hls.js)
            if (old.fromMarkup && !c.fromMarkup) { old.fromMarkup = false; changed = true }
            if (old.orphan) { old.orphan = false; changed = true }                        // VS2: the player asked for it again: it is live again
            if (c.seenAt > old.lastSeenAt) old.lastSeenAt = c.seenAt                       // (ANS55: not a change of the list, only of its order of news)
            if (changed) version++
            return changed
        }
        items.add(0, c)
        pending.remove(c.key)?.let { applyTo(c, it.width, it.height, it.durationMs, it.label, it.role, it.played, it.element, it.areaPx) }
        while (items.size > max) removeLeastValuable()
        version++
        return true
    }

    /**
     * EY-17 (research/REVIEW-EXTERNAL-2.md): a full list drops the entry worth LEAST, not the oldest: a page that loads thirty previews and ads before (or after) the real video must not push the real video out.
     * Never the one that was just added (index 0).
     */
    private fun removeLeastValuable() {
        var worst = -1; var worstV = Int.MAX_VALUE
        for (i in 1 until items.size) { val v = value(items[i], i); if (v < worstV) { worstV = v; worst = i } }
        items.removeAt(if (worst >= 0) worst else items.size - 1)
    }

    /** What an entry is worth keeping: playlists and manifests, anything somebody showed interest in, big and long files, a playlist that was really read. Equal worth: the newer stays. */
    private fun value(c: MediaCandidate, index: Int): Int {
        if (c.decoy) return -1_000_000 - index                      // RX3-V1: a hover preview or an ad goes before anything else
        var v = 0
        if (c.kind != MediaKind.FILE) v += 100
        if (c.touched) v += 80
        if (c.length >= MIN_WITH_STREAM) v += 40 else if (c.length >= MIN_FILE) v += 10
        if (c.durationMs >= 60_000) v += 20
        if (c.viaHeaders != null) v += 5
        return v * 100 - index
    }

    /** A one-byte Range request (or anything else) told the length of a file that was only found in the page's text. @return true if the list changed. */
    fun setLength(url: String, bytes: Long): Boolean {
        val c = find(url) ?: return false
        if (bytes < 0 || c.length == bytes) return false
        c.length = bytes; version++
        return true
    }

    /** RX3-V5: what the early look at a stream's playlist found. @return true if the list changed. */
    fun applyStream(url: String, live: Boolean, drm: Boolean, durationMs: Long, adMs: Long, breaks: Int, heights: IntArray = IntArray(0)): Boolean {
        val c = find(url) ?: return false
        c.looked = true; c.live = live; c.drm = drm
        if (durationMs > 0) c.durationMs = durationMs
        c.adMs = adMs; c.adBreaks = breaks
        if (heights.isNotEmpty()) c.heights = heights
        for ((e, b) in blobs) matchBlob(e, b)          // RX3-V4: a player that was already waiting for a playlist of its length
        version++
        capture()                                       // VS2: the look is the evidence that this is (or is not) a film
        return true
    }

    /**
     * RX3-V4: a player that feeds a blob: address (hls.js, shaka, dash.js: most embeds) cannot say which playlist it plays, but its LENGTH and picture HEIGHT can match one of the playlists the early look read:
     * the length within 1.5 s or 3 percent, then, among those, the ones whose variants list that height (a tie-breaker only: a picture size the master never listed does not lose the match). The matched playlists
     * get what the element says (its role, whether it really plays, the element number: the pre-roll rule of [markEarlierAds] works for them too). What the element said is kept, so a playlist that is read later is
     * matched when its look arrives. @return true if the list changed.
     */
    fun applyBlob(height: Int, durationMs: Long, role: VideoRole?, played: Boolean, element: Int, areaPx: Long): Boolean {
        if (durationMs <= 0) return false
        blobs.remove(element)
        val b = Blob(height, durationMs, role, played, areaPx)
        blobs[element] = b
        while (blobs.size > BLOB_MAX) blobs.remove(blobs.keys.first())
        val changed = matchBlob(element, b)
        if (changed) version++
        return changed
    }

    /** RX3-V4: how many stream playlists the player with this element number was matched to (for the Diagnostics events). */
    fun matchedBy(element: Int): Int = items.count { it.kind != MediaKind.FILE && it.element == element }

    private fun matchBlob(element: Int, b: Blob): Boolean {
        val tol = maxOf(BLOB_TOL_MS, b.durationMs * 3 / 100)
        var cands = items.filter { it.kind != MediaKind.FILE && it.looked && it.durationMs > 0 && Math.abs(it.durationMs - b.durationMs) <= tol }
        if (b.height > 0) cands.filter { b.height in it.heights }.takeIf { it.isNotEmpty() }?.let { cands = it }
        var changed = false
        for (c in cands) if (applyTo(c, 0, 0, 0L, null, b.role, b.played, element, b.areaPx)) changed = true
        return changed
    }

    /** RX3-V5: up to [n] stream playlists nobody has read yet, newest first. */
    fun streamsToLook(n: Int): List<MediaCandidate> = items.filter { it.kind != MediaKind.FILE && !it.looked }.take(n)

    /** RX3-V2: up to [n] addresses found only in the page's text, newest first, worth one cheap check - but only when the page has nothing verified yet. */
    fun unverifiedToCheck(n: Int): List<MediaCandidate> {
        if (items.any { it.verified && !it.decoy }) return emptyList()
        return items.filter { !it.verified && it.kind == MediaKind.FILE }.take(n)
    }

    /** Somebody showed interest in this address (long-pressed, opened in the sheet, downloaded): it is kept when the list is full and shown even when it looks like a preview. @return true if that changed the list. */
    fun touch(url: String): Boolean {
        val c = find(url) ?: return false
        if (c.touched && c.interest) return false
        c.touched = true; c.interest = true; version++
        return true
    }

    /** What the page's own `<video>` / `<source>` told us about a URL (picture size, length in time, the label it shows). */
    fun applyInfo(url: String, width: Int, height: Int, durationMs: Long, label: String?, role: VideoRole? = null, played: Boolean = false, element: Int = 0, areaPx: Long = 0L): Boolean {
        val key = MediaCandidate.keyOf(url)
        val c = items.firstOrNull { it.key == key }
        if (c == null) { // the file has not been reported yet: what the element said waits for it
            pending[key] = Said(width, height, durationMs, label, role, played, element, areaPx)
            while (pending.size > PENDING_MAX) pending.remove(pending.keys.first())
            return false
        }
        val changed = applyTo(c, width, height, durationMs, label, role, played, element, areaPx)
        if (changed) version++
        return changed
    }

    private fun applyTo(c: MediaCandidate, width: Int, height: Int, durationMs: Long, label: String?, role: VideoRole?, played: Boolean = false, element: Int = 0, areaPx: Long = 0L): Boolean {
        var changed = false
        if (width > 0 && c.width != width) { c.width = width; changed = true }
        if (height > 0 && c.height != height) { c.height = height; changed = true }
        if (durationMs > 0 && c.durationMs != durationMs) { c.durationMs = durationMs; changed = true }
        if (!label.isNullOrBlank() && c.label != label) { c.label = label.trim().take(24); changed = true }
        // RX3-V1: the page's element says what it is for. A player (or a report without facts, as before) makes it a real video; decoration and ads do not, however long or sharp they are. The latest report wins.
        if (role != null && c.role != role) { c.role = role; changed = true }
        if (role == null || role == VideoRole.PLAYER) { if (!c.touched) { c.touched = true; changed = true } }
        else if (!c.interest && c.touched) { c.touched = false; changed = true }
        // RX3-V3: which element really plays
        if (element > 0 && c.element != element) { c.element = element; changed = true }
        if (areaPx > 0 && c.areaPx != areaPx) { c.areaPx = areaPx; changed = true }
        if (played && !c.played) { c.played = true; c.playedSeq = ++playSeq; changed = true }
        if (played && c.element > 0 && markEarlierAds(c)) changed = true
        return changed
    }

    /**
     * RX3-V3: the SAME element that played a file under 90 s earlier and now plays one at least three times longer: the earlier one was an advertisement (a pre-roll, then the film). Never when the owner showed interest
     * in it. @return true if something changed.
     */
    private fun markEarlierAds(c: MediaCandidate): Boolean {
        var changed = false
        for (o in items) {
            if (o === c || o.element != c.element || !o.played || o.playedSeq >= c.playedSeq || o.interest) continue
            if (o.durationMs in 1 until AD_MAX_MS && c.durationMs >= 3 * o.durationMs && o.role != VideoRole.AD) { o.role = VideoRole.AD; o.touched = false; changed = true }
        }
        return changed
    }

    /** RX3-V3: the file that really plays: the played one with the largest part of its picture on screen, then the longest. Null when nothing played (or only decoration). */
    fun primary(): MediaCandidate? = items.filter { it.played && !it.decoy }.maxWithOrNull(compareBy<MediaCandidate>({ it.areaPx }, { it.durationMs }))

    /**
     * What is worth offering: playlists and big files first. Tiny files (sounds, tracking clips) and, when the page has a stream, small clips next to it (ads, previews) are left out.
     */
    fun usable(): List<MediaCandidate> = usableUnsorted().sortedWith(compareBy<MediaCandidate> { if (it.orphan) 1 else 0 }.thenBy { kindRank(it) }.thenByDescending { it.knownHeight }.thenByDescending { it.length })

    private fun usableUnsorted(): List<MediaCandidate> {
        val hasStream = items.any { it.kind != MediaKind.FILE }
        return items.filter { c ->
            when {
                c.captured && !c.live && !c.drm -> true                      // VS2
                c.kind != MediaKind.FILE -> true
                c.touched || (!c.decoy && (c.durationMs >= 20_000 || c.knownHeight >= 480)) -> true // played, long, or sharp: never hidden as a preview or an ad (unless the page itself calls it decoration or an ad)
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
        /** A stream shorter than this is a clip (an advertisement, a preview), not something to save. */
        const val MIN_STREAM_MS = 20_000L
        /** VS2: a stream of this length or more (its playlist was read) is a film or an episode, not an advertisement or a preview: it is captured when offered. */
        const val FILM_EVIDENCE_MS = 120_000L
        /** VS2: a file of this size is a film whatever else is known. */
        const val BIG_FILE = 40L * 1024 * 1024
        private const val PENDING_MAX = 32
        /** RX3-V3: a file that played under this long, then replaced on the same element by one three times longer, was an advertisement. */
        private const val AD_MAX_MS = 90_000L
        /** RX3-V4: a blob: player's length matches a playlist within this (or 3 percent of the player's length, whichever is more); at most this many players are remembered. */
        /** What a player that feeds a blob: address reports as its address (the address itself is never sent). */
        const val BLOB = "blob"
        private const val BLOB_TOL_MS = 1_500L
        private const val BLOB_MAX = 8
        /** RX3-V4: a stream under [CLIP_MS] next to one of at least [FILM_MS] is a clip (an advertisement, a trailer), not the film. */
        const val CLIP_MS = 60_000L
        const val FILM_MS = 300_000L

        /**
         * RX3-V4: [c] is a stream playlist, read by the early look, under a minute long, while [all] holds another readable stream of five minutes or more (a stream that can be saved, not decoration): it is not counted
         * and goes under "Other files". Never what really played, never what the owner showed interest in. Nothing is known about a playlist the look has not read.
         */
        fun clipNextToFilm(c: MediaCandidate, all: Collection<MediaCandidate>): Boolean =
            c.kind != MediaKind.FILE && c.looked && c.durationMs in 1 until CLIP_MS && !c.played && !c.interest &&
                all.any { it !== c && it.kind != MediaKind.FILE && it.looked && it.durationMs >= FILM_MS && !it.decoy && !it.unfit }
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
