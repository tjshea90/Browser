package app.kestrel.core.media

/**
 * ANS54e (owner message 54, on alpha57: "When I play a video from the web page, the video player is very slow and laggy ... when I press the download button and select play in kestrel's player (test), it is
 * nearly perfect ... I want the web video players to use this"): decides whether the film a page is about to play is played by KESTREL'S OWN PLAYER (Media3 on a SurfaceView) instead of the page's player.
 * Pure rules, no Android: the page script reports what the element looks like ([Element]), the app lists the streams its sniffer saw in that frame ([Known]), this says yes (a [Decision]: the address to
 * play and where to start) or no (null: the page plays it itself, as before).
 *
 * What is handed over: a direct file or HLS address of the element, or - for a player written in script (a blob: source) - the (longest) HLS stream that frame asked for. What never is: loop clips and thumbnails
 * (the page's own box is tiny), DASH (no module in the APK), DRM, a blob: source with no stream known (YouTube-style players), and a second film while one is handed over already (the app checks that).
 *
 * ANS61 (owner message 61: "it played an advertisement video and I couldn't get to the real video"): an ADVERTISEMENT is never handed over. A pre-roll plays on the page's own element, so the address the gate reports
 * at the press can be the ad's: [Element.ad] (the page's player says an ad plays), an address that looks like an ad file ([looksLikeAdAddress]) and a clip Kestrel's player just played to its end ([playedThrough]:
 * pressed again it would be the same ad for ever) all send the film back to the page, whose ad plugin can then finish and go on to the film. With a long film of the frame known, that film is handed over instead.
 */
object WebHandOver {
    /** The Settings choice "Web videos play in". */
    const val ALWAYS = 0       // Kestrel's player whenever it can play the film (default)
    const val FULLSCREEN = 1   // Kestrel's player only when the film goes full screen
    const val NEVER = 2        // the page's own player
    const val MODES = 3

    fun cleanMode(m: Int): Int = if (m in 0 until MODES) m else ALWAYS

    /** Why the question is asked: the page's play() after a press on the player, the engine's own controls pressed, Kestrel's own bar, the film went full screen, a playlist showed up late. */
    enum class Why { PLAY, PRESS, KESTREL, FULLSCREEN, LATE }

    /**
     * What the page script says about the element (the content script has checked the numbers): [src] = the address of the element's own source ("" = none or a blob:), [blob] = a MediaSource or a
     * blob: address, [mime] = the type a `<source>` named ("" = none), [w] x [h] = its box in CSS pixels (0 = not laid out), [timeS] / [durS] = where it is and how long it is (0 = not known),
     * [ad] = the page's player says an advertisement plays in it (an ad-state class on the element or an ancestor, or the element inside an ad container: gate-main.js adNow).
     */
    class Element(val src: String, val blob: Boolean, val mime: String, val loop: Boolean, val w: Int, val h: Int, val timeS: Double, val durS: Double, val ad: Boolean = false)

    /**
     * A stream or file the sniffer saw in the element's frame: [usable] = not DRM, not a decoy; [durationMs] 0 = the early look has not read it yet; [master] = a MASTER playlist (it lists variants: the pictures),
     * not the list of one rendition; [seenAt] = when the page last asked for it (milliseconds, any clock, only compared with each other).
     */
    class Known(val url: String, val kind: MediaKind, val durationMs: Long, val usable: Boolean, val master: Boolean = false, val seenAt: Long = 0L)

    /** Play [url] ([kind] FILE or HLS) from [startMs]. [note] says why, for Diagnostics. */
    class Decision(val url: String, val kind: MediaKind, val startMs: Long, val note: String)

    const val MIN_BOX_W = 200          // a hover preview, a thumbnail, a GIF-like clip is no film
    const val MIN_BOX_H = 112
    const val FILM_MS = 60_000L        // a stream this long is the film; shorter ones in the same frame are advertisements and previews
    const val AD_MAX_S = 40.0          // an element that says it is this short while a film is known is playing an advertisement

    private val FILE_EXTS = setOf("mp4", "m4v", "mov", "webm", "mkv")

    fun decide(mode: Int, why: Why, el: Element, known: List<Known>): Decision? {
        val m = cleanMode(mode)
        if (m == NEVER) return null
        if (m == FULLSCREEN && why != Why.FULLSCREEN) return null
        if (el.loop) return null
        if ((el.w in 1 until MIN_BOX_W) || (el.h in 1 until MIN_BOX_H)) return null
        val film = filmOf(known, why)
        val direct = directOf(el)
        val filmLong = film != null && (film.durationMs <= 0L || film.durationMs >= FILM_MS)
        return when {
            direct != null -> {
                val shortClip = el.durS > 0.0 && el.durS < AD_MAX_S
                val adLike = el.ad || looksLikeAdAddress(direct.url)
                when {
                    adLike && filmLong -> make(film!!, el, "an advertisement is in the element: the film of the frame")
                    adLike -> null                                                           // ANS61: an advertisement and no film known: the page plays it (its ad plugin has to see it finished)
                    shortClip && filmLong -> make(film!!, el, "an advertisement is in the element: the film of the frame")
                    else -> make(direct, el, "the element's own address")
                }
            }
            el.blob && film != null -> if (el.ad && !filmLong) null else make(film, el, "the stream the frame asked for" + (if (film.master) " (a master list)" else " (a bare list)"))
            !el.blob && el.src.isEmpty() && (why == Why.FULLSCREEN || why == Why.LATE) && film != null -> if (el.ad && !filmLong) null else make(film, el, "the stream the frame asked for (full screen)" + (if (film.master) " (a master list)" else " (a bare list)"))
            else -> null
        }
    }

    // ANS61: whole words of the PATH of a media address that name an advertisement ("/ads/spot.mp4", "preroll-30.mp4", "/advert/1.mp4"), and words that only an ad system writes anywhere in the address (a query too:
    // "?vast=1", "imasdk", "adunit", "adid"). The host never counts (a CDN host can be called anything), and a bare "ad=0" in a query is nothing.
    private val AD_PATH_WORDS = Regex("""(?i)(?:^|[/._\-])(advert\w*|adserv\w*|preroll|pre-roll)(?:$|[/._\-])""")
    private val AD_BARE_WORD = Regex("""(?i)(?:^|[/_])ads?(?:$|[/_.])""")        // "ad" / "ads" alone: a whole path piece or between underscores ("video_ad_15.mp4"), never between hyphens (a film called "ad-astra")
    private val AD_SYSTEM_WORDS = Regex("""(?i)(?:^|[/._\-=&?])(vast|vpaid|imasdk|adunit|adid)(?:$|[/._\-=&?])""")

    /** True when the address of a media file names an advertisement (see [AD_PATH_WORDS]): never handed to Kestrel's player, so the page's ad plugin can run its ad and go on to the film. */
    fun looksLikeAdAddress(url: String): Boolean {
        val rest = url.substringBefore('#').substringAfter("://", url).substringAfter('/', "")
        if (rest.isEmpty()) return false
        val path = rest.substringBefore('?')
        return AD_PATH_WORDS.containsMatchIn(path) || AD_BARE_WORD.containsMatchIn(path) || AD_SYSTEM_WORDS.containsMatchIn(rest)
    }

    const val LOOP_CLIP_MAX_MS = 120_000L          // a clip up to this long that was played to its end is "an advertisement or a short clip": a second press on it goes to the page
    const val LOOP_WINDOW_MS = 10 * 60_000L

    /**
     * ANS61 loop breaker: [url] is the address Kestrel's player played to its END a moment ago ([lastUrl], [lastPosMs] of [lastDurMs], stopped [sinceStopMs] ago; < 0 = never). A clip of at most two minutes that
     * played through and is pressed again on the same page is, far more often than not, the advertisement the page's ad plugin never got to see finished (its play() was refused, the element stayed closed): handing it
     * over again is an ad loop with no way to the film. So the page plays it itself this time. The queries do not count (an ad address carries a new token every time it is asked for).
     */
    fun playedThrough(url: String, lastUrl: String, lastPosMs: Long, lastDurMs: Long, sinceStopMs: Long): Boolean {
        if (url.isEmpty() || lastUrl.isEmpty() || sinceStopMs !in 0L..LOOP_WINDOW_MS) return false
        if (lastDurMs !in 1L..LOOP_CLIP_MAX_MS || lastPosMs < lastDurMs - 2_000L) return false
        return url.substringBefore('?').substringBefore('#') == lastUrl.substringBefore('?').substringBefore('#')
    }

    /**
     * The film of the frame (ANS55, owner message 55: Yahoo's player showed the time but a black picture). A player written in script loads a MASTER playlist and then the lists of the renditions it names (the picture,
     * the sound, the subtitles: Brightcove, which Yahoo uses, puts all three next to the master, every one as long as the film), and the sniffer sees them all. So: a master first (it offers pictures); among masters the
     * ones as long as a film (an advertisement is shorter); of those the one the page asked for LAST (a player that moves on to another video asks for that video's master last). With no master known a bare media
     * list is the film only when it is the only HLS list of the frame (renditions come in twos and threes), and never for a list that came late (it may be the first of several).
     */
    internal fun filmOf(known: List<Known>, why: Why): Known? {
        val hls = known.filter { it.usable && it.kind == MediaKind.HLS }
        val masters = hls.filter { it.master }
        if (masters.isNotEmpty()) {
            val long = masters.filter { it.durationMs <= 0L || it.durationMs >= FILM_MS }
            return (if (long.isNotEmpty()) long else masters).maxWithOrNull(compareBy<Known>({ it.seenAt }, { it.durationMs }, { -it.url.length }))
        }
        return if (hls.size == 1 && why != Why.LATE) hls[0] else null
    }

    /** The address of a playlist that has not been read yet says master ("master.m3u8", "/master/"): a hint for the moment before the early look, never against a list that was read. */
    fun looksLikeMaster(url: String): Boolean = Regex("(^|[/_.-])master", RegexOption.IGNORE_CASE).containsMatchIn(url.substringBefore('?').substringAfter("://").substringAfter('/', ""))

    private fun directOf(el: Element): Known? {
        val u = el.src
        if (!(u.startsWith("http://", true) || u.startsWith("https://", true)) || u.length > 2000) return null
        val ext = MediaSniff.extension(u)
        return when {
            ext == "m3u8" -> Known(u, MediaKind.HLS, 0L, true)
            ext == "mpd" -> null
            ext in FILE_EXTS -> Known(u, MediaKind.FILE, 0L, true)
            ext.isEmpty() && el.mime.lowercase().let { it.startsWith("video/mp4") || it.startsWith("video/webm") || it.startsWith("video/quicktime") || it.startsWith("video/x-matroska") } -> Known(u, MediaKind.FILE, 0L, true)
            else -> null
        }
    }

    private fun make(k: Known, el: Element, note: String) = Decision(k.url, k.kind, PageQuality.startMs(el.timeS, el.durS, k.durationMs), note)

    /**
     * A hand-over needs a tap of the person (or a key that plays) at most this long ago: a page's script can fake the gate's question, but not a finger (owner message 53: nothing starts unless the person presses).
     * Full screen and a playlist that comes late follow the tap by a while.
     */
    fun pressWindowMs(why: Why): Long = if (why == Why.FULLSCREEN || why == Why.LATE) 30_000L else 15_000L

    /**
     * RVW3 SC1: the film's address comes from the page (the element's own `src`, or a list the page asked for), and Kestrel's own player is the APP, not the engine: the engine's local-network blocking does not
     * apply to it. A page on the public internet must not be able to point it, after one innocent tap, at the router, a NAS or a camera of the owner's network ([NetHosts.reachesLocalNetwork]: every spelling).
     * A page that is itself on the local network (the hotspot gateway, a NAS) may play its own files. An address that has no host counts as refused.
     */
    fun hostRefused(pageUrl: String, filmUrl: String): Boolean =
        !NetHosts.probeAllowed(UrlParts.hostOf(filmUrl).orEmpty(), UrlParts.hostOf(pageUrl).orEmpty())

    const val RESUME_WINDOW_MS = 30 * 60_000L      // a film pressed again within this long after Kestrel's player ended continues where the person stopped

    /**
     * Where Kestrel's own player starts when a film is pressed AGAIN on its page (it was watched in Kestrel's player, Back, play on the page once more): two seconds before where the person stopped, when it is the same
     * address, the player ended no more than [RESUME_WINDOW_MS] ago, the page's own element is still near its start (under 5 s) and the film did not play to its end. Anything else starts where the page is
     * ([pageStartMs]). [sinceStopMs] < 0 = nothing was stopped yet. The numbers are kept in RAM only and never for a private tab.
     */
    fun resumeMs(pageStartMs: Long, url: String, lastUrl: String, lastPosMs: Long, lastDurMs: Long, sinceStopMs: Long): Long {
        if (url.isEmpty() || url != lastUrl || pageStartMs >= 5_000L || sinceStopMs !in 0L..RESUME_WINDOW_MS) return pageStartMs
        if (lastPosMs < 5_000L) return pageStartMs
        if (lastDurMs > 0L && lastPosMs > lastDurMs - 10_000L) return pageStartMs
        return lastPosMs - 2_000L
    }
}
