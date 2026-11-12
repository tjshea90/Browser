package app.kestrel.core.media

/**
 * Which of the files a page loaded is the video the owner means. A page loads many media responses: the video, its other qualities, a short preview clip, tracking "videos"
 * from script endpoints (`track.php?id=3` answering `video/mp4`), ad clips. Owner, 2026-11-12: "a couple files are usually small files from php web addresses that are obviously not
 * the video I long pressed". This decides what goes to the top of the sheet ([Result.main]) and what is tucked away under "Other files on this page" ([Result.other]); nothing is
 * deleted, so a wrong guess costs one tap. Pure logic, no Android: MediaRankTest.
 */
object MediaRank {
    /** A file under this is a clip, not the video, when the page also has something much bigger (in bytes). */
    const val SMALL_ABS = 1L shl 20
    /** ... or when it is under this share of the biggest video file. */
    const val SMALL_REL = 0.12
    /** A script endpoint ("get.php?...") is believed to be the real video only when it is this big. */
    const val SCRIPT_BIG = 8L shl 20

    class Result(
        /** What to offer first, in the order to show it (the exact one first). */
        val main: List<MediaCandidate>,
        /** Small, tracker-looking or unrelated files: shown only when asked for. */
        val other: List<MediaCandidate>,
        /** The file whose address is the long-pressed element's own, or null. */
        val exact: MediaCandidate?,
    )

    private val SCRIPT_EXTS = setOf("php", "php3", "php4", "php5", "asp", "aspx", "jsp", "jspx", "cgi", "pl", "do", "action")
    private val HINT_WORDS = Regex("""(?i)(?:^|[/._\-=&?])(track|tracking|tracker|pixel|beacon|analytics|telemetry|metrics?|adserv\w*|advert\w*|ads?|banner|preroll|pre-roll|sponsor\w*|promo|teaser|preview|thumb\w*|sprite|ping|stat|stats)(?:$|[/._\-=&?])""")
    private val PIECE_PARAMS = setOf("range", "bytestart", "byteend", "start", "end", "sq", "seg", "segment", "rn", "rbuf", "offset", "byte_range", "bytes")

    fun rank(items: List<MediaCandidate>, pressedUrl: String? = null): Result {
        val all = dedupe(items)
        val pressedKey = pressedUrl?.takeIf { it.startsWith("http", true) }?.let { MediaCandidate.keyOf(it) }
        val exact = pressedKey?.let { k -> all.firstOrNull { it.key == k } ?: all.firstOrNull { normKey(it.url) == normKey(k) } }
        return if (exact != null) byExact(all, exact) else byRank(all)
    }

    // ---- the element the owner pressed is known: that file, its other qualities, nothing else on top

    private fun byExact(all: List<MediaCandidate>, exact: MediaCandidate): Result {
        val main = ArrayList<MediaCandidate>(); val other = ArrayList<MediaCandidate>()
        for (c in all) {
            when {
                c === exact -> {}
                sameVideo(exact, c) -> main.add(c)
                else -> other.add(c)
            }
        }
        main.sortWith(order()); other.sortWith(order())
        return Result(listOf(exact) + main, other, exact)
    }

    /** Another quality (or container) of what [a] is: the same length of time, or the same file name apart from its resolution, in the same place. */
    private fun sameVideo(a: MediaCandidate, c: MediaCandidate): Boolean {
        if (c.kind != MediaKind.FILE || a.kind != MediaKind.FILE) return false
        if (c.audioOnly != a.audioOnly) return false
        if (a.durationMs > 0 && c.durationMs > 0) return Math.abs(a.durationMs - c.durationMs) <= 1500
        val da = dirOf(a.url); val dc = dirOf(c.url)
        if (stemWithoutResolution(a.url).isNotEmpty() && stemWithoutResolution(a.url) == stemWithoutResolution(c.url) && da == dc) return true
        return false
    }

    // ---- nothing pressed (the download icon, a player that hands out a blob: address): the best guess

    private fun byRank(all: List<MediaCandidate>): Result {
        val files = all.filter { it.kind == MediaKind.FILE && !it.audioOnly }
        val biggest = files.maxOfOrNull { it.length } ?: -1L
        val topFrame = files.filter { it.frame.isEmpty() }.maxOfOrNull { it.length } ?: -1L
        val hasVideo = all.any { it.kind != MediaKind.FILE || !it.audioOnly }
        val main = ArrayList<MediaCandidate>(); val other = ArrayList<MediaCandidate>()
        for (c in all) {
            if (c.kind != MediaKind.FILE) { main.add(c); continue }
            if (junk(c, biggest, topFrame, hasVideo)) other.add(c) else main.add(c)
        }
        // nothing but small files: the biggest one is still the best guess, never an empty sheet
        if (main.none { it.kind != MediaKind.FILE || !it.audioOnly } && other.isNotEmpty()) {
            val best = other.filter { !it.audioOnly }.maxByOrNull { it.length } ?: other.maxByOrNull { it.length }
            if (best != null) { other.remove(best); main.add(best) }
        }
        main.sortWith(order()); other.sortWith(order())
        return Result(main, other, null)
    }

    private fun junk(c: MediaCandidate, biggest: Long, topFrame: Long, hasVideo: Boolean): Boolean {
        val len = c.length
        if (c.audioOnly) return hasVideo && (len in 0 until 3L * SMALL_ABS)                      // a beep or a jingle next to a video
        val relSmall = biggest > 0 && len in 0 until maxOf(SMALL_ABS, (biggest * SMALL_REL).toLong())
        if (relSmall) return true
        val script = MediaSniff.extension(c.url) in SCRIPT_EXTS || scriptPath(c.url)
        val hint = HINT_WORDS.containsMatchIn(pathAndQuery(c.url))
        if (script && (len < 0 || len < SCRIPT_BIG)) return true                              // "get.php" is a video only when it is big
        if (hint && len in 0 until maxOf(SMALL_ABS, (biggest * 0.4).toLong())) return true      // "preview.mp4", "teaser", "ad": a clip, not the video
        if (hint && len < 0 && biggest > 0) return true
        if (c.frame.isNotEmpty() && topFrame > 0 && len in 0 until (topFrame * 0.25).toLong()) return true   // asked for by another frame (an advert) and small next to the page's own
        return false
    }

    /** Best first: pictures before sound, files before streams' leftovers, tall before short, big before small. */
    private fun order(): Comparator<MediaCandidate> = compareBy<MediaCandidate> { rank(it) }.thenByDescending { it.knownHeight }.thenByDescending { it.length }

    private fun rank(c: MediaCandidate): Int = when {
        c.kind == MediaKind.HLS -> 0
        c.kind == MediaKind.DASH -> 1
        c.audioOnly -> 3
        else -> 2
    }

    // ---- small helpers

    /** One entry per file: the same address with a different piece-of-a-file parameter, or a different token, is the same file; keep the one that knows most. */
    private fun dedupe(items: List<MediaCandidate>): List<MediaCandidate> {
        val best = LinkedHashMap<String, MediaCandidate>()
        for (c in items) {
            val k = normKey(c.url)
            val old = best[k]
            if (old == null || c.length > old.length || (c.length == old.length && c.height > old.height)) best[k] = c
        }
        return best.values.toList()
    }

    internal fun normKey(url: String): String {
        val base = url.substringBefore('#')
        val q = base.indexOf('?')
        if (q < 0) return base
        val keep = base.substring(q + 1).split('&').filter { p -> p.isNotEmpty() && p.substringBefore('=').lowercase() !in PIECE_PARAMS }
        return base.substring(0, q) + if (keep.isEmpty()) "" else "?" + keep.joinToString("&")
    }

    private fun pathAndQuery(url: String): String = url.substringAfter("://", url).substringAfter('/', "").substringBefore('#')

    private fun scriptPath(url: String): Boolean {
        val name = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        return name.contains(".php") || name.contains(".asp") || name.contains(".cgi")
    }

    private fun dirOf(url: String): String = url.substringBefore('?').substringBefore('#').substringBeforeLast('/')

    /** "movie_1080p.mp4" and "movie_720p.mp4" -> "movie" (a resolution token and the extension removed). */
    internal fun stemWithoutResolution(url: String): String {
        val name = url.substringBefore('?').substringBefore('#').substringAfterLast('/').substringBeforeLast('.')
        return name.replace(Regex("""(?i)[_\-. ]?\(?\b\d{3,4}p?\b\)?"""), "").replace(Regex("""(?i)[_\-. ](hd|sd|fhd|uhd|high|low|medium|mobile)\b"""), "").trim('_', '-', '.', ' ')
    }
}
