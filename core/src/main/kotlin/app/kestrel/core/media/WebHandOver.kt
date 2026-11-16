package app.kestrel.core.media

/**
 * ANS54e (owner message 54, on alpha57: "When I play a video from the web page, the video player is very slow and laggy ... when I press the download button and select play in kestrel's player (test), it is
 * nearly perfect ... I want the web video players to use this"): decides whether the film a page is about to play is played by KESTREL'S OWN PLAYER (Media3 on a SurfaceView) instead of the page's player.
 * Pure rules, no Android: the page script reports what the element looks like ([Element]), the app lists the streams its sniffer saw in that frame ([Known]), this says yes (a [Decision]: the address to
 * play and where to start) or no (null: the page plays it itself, as before).
 *
 * What is handed over: a direct file or HLS address of the element, or - for a player written in script (a blob: source) - the (longest) HLS stream that frame asked for. What never is: loop clips and thumbnails
 * (the page's own box is tiny), DASH (no module in the APK), DRM, a blob: source with no stream known (YouTube-style players), and a second film while one is handed over already (the app checks that).
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
     * blob: address, [mime] = the type a `<source>` named ("" = none), [w] x [h] = its box in CSS pixels (0 = not laid out), [timeS] / [durS] = where it is and how long it is (0 = not known).
     */
    class Element(val src: String, val blob: Boolean, val mime: String, val loop: Boolean, val w: Int, val h: Int, val timeS: Double, val durS: Double)

    /** A stream or file the sniffer saw in the element's frame: [usable] = not DRM, not a decoy; [durationMs] 0 = the early look has not read it yet. */
    class Known(val url: String, val kind: MediaKind, val durationMs: Long, val usable: Boolean)

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
        // the film of the frame: the LONGEST usable HLS stream (one whose length is not read yet counts as a film); a lone short stream is taken too: there is nothing else to choose
        val film = known.filter { it.usable && it.kind == MediaKind.HLS }.maxWithOrNull(compareBy<Known>({ if (it.durationMs <= 0L) FILM_MS else it.durationMs }, { -it.url.length }))
        val direct = directOf(el)
        return when {
            direct != null -> {
                val shortClip = el.durS > 0.0 && el.durS < AD_MAX_S
                val filmLong = film != null && (film.durationMs <= 0L || film.durationMs >= FILM_MS)
                if (shortClip && filmLong) make(film!!, el, "an advertisement is in the element: the film of the frame") else make(direct, el, "the element's own address")
            }
            el.blob && film != null -> make(film, el, "the stream the frame asked for")
            !el.blob && el.src.isEmpty() && (why == Why.FULLSCREEN || why == Why.LATE) && film != null -> make(film, el, "the stream the frame asked for (full screen)")
            else -> null
        }
    }

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
