package app.kestrel.core.filter

/**
 * Request-type bit flags (ABP/uBO vocabulary). A request carries a MASK of possible types because
 * `shouldInterceptRequest` does not tell us the real resource type: we infer it, and when we cannot tell
 * we say "could be script OR fetch OR other" instead of guessing wrong.
 */
object RequestType {
    const val DOCUMENT = 1
    const val SUBDOCUMENT = 2
    const val STYLESHEET = 4
    const val SCRIPT = 8
    const val IMAGE = 16
    const val FONT = 32
    const val MEDIA = 64
    const val XHR = 128
    const val WEBSOCKET = 256
    const val PING = 512
    const val OBJECT = 1024
    const val OTHER = 2048

    /** A navigation inside a window the page opened (`$popup` rules). Not part of ALL / ALL_SUBRESOURCES: only explicit rules use it. */
    const val POPUP = 4096

    /** Everything except the main document: what a rule without type options applies to. */
    const val ALL_SUBRESOURCES = 0xFFE
    const val ALL = 0xFFF

    /**
     * Ambiguous fetch-like request whose real type we could not determine. Deliberately WITHOUT PING: the real lists contain a
     * catch-all `$ping` rule (block every third-party ping), so including PING here would block every request whose type we
     * cannot infer (e.g. `https://js.stripe.com/v3/`). We never infer PING at all.
     */
    const val UNKNOWN = SCRIPT or XHR or OTHER

    /** Type option name -> bit, or 0 when the name is not a (supported) request type. */
    @JvmStatic
    fun fromName(name: String): Int = when (name) {
        "document", "doc" -> DOCUMENT
        "subdocument", "frame" -> SUBDOCUMENT
        "stylesheet", "css" -> STYLESHEET
        "script" -> SCRIPT
        "image" -> IMAGE
        "font" -> FONT
        "media" -> MEDIA
        "xmlhttprequest", "xhr", "fetch" -> XHR
        "websocket" -> WEBSOCKET
        "ping", "beacon" -> PING
        "object", "object-subrequest" -> OBJECT
        "other" -> OTHER
        "popup" -> POPUP
        else -> 0
    }

    /**
     * Best-effort type mask for an intercepted request.
     * @param secFetchDest value of `Sec-Fetch-Dest` if the WebView exposes it (usually it does not)
     * @param accept the `Accept` header
     * @param path URL path without query (used for the file extension)
     */
    @JvmStatic
    fun infer(isMainFrame: Boolean, secFetchDest: String?, accept: String?, path: String, requestedWith: String?): Int {
        if (isMainFrame) return DOCUMENT
        if (!secFetchDest.isNullOrEmpty()) {
            when (secFetchDest.lowercase()) {
                "document", "iframe", "frame", "fencedframe" -> return SUBDOCUMENT
                "script", "worker", "sharedworker", "serviceworker", "audioworklet", "paintworklet" -> return SCRIPT
                "style" -> return STYLESHEET
                "image" -> return IMAGE
                "font" -> return FONT
                "audio", "video", "track" -> return MEDIA
                "object", "embed" -> return OBJECT
                "empty" -> return XHR or OTHER
                "websocket" -> return WEBSOCKET
            }
        }
        val dot = path.lastIndexOf('.')
        if (dot >= 0 && path.length - dot <= 6 && path.indexOf('/', dot) < 0) {
            when (path.substring(dot + 1).lowercase()) {
                "js", "mjs" -> return SCRIPT
                "css" -> return STYLESHEET
                "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "avif", "bmp", "apng", "cur" -> return IMAGE
                "woff", "woff2", "ttf", "otf", "eot" -> return FONT
                "mp4", "webm", "m4a", "m4v", "mp3", "ogg", "oga", "ogv", "wav", "aac", "flac", "m3u8", "mpd", "ts", "m4s" -> return MEDIA
                "html", "htm" -> return SUBDOCUMENT
            }
        }
        if (accept != null) {
            val a = accept.lowercase()
            if (a.startsWith("text/css")) return STYLESHEET
            if (a.startsWith("image/")) return IMAGE
            if (a.contains("text/html") || a.contains("application/xhtml")) return SUBDOCUMENT
            if (a.startsWith("application/json") || a.startsWith("text/plain")) return XHR or OTHER
            if (a.startsWith("audio/") || a.startsWith("video/")) return MEDIA
        }
        if (requestedWith != null && requestedWith.equals("XMLHttpRequest", ignoreCase = true)) return XHR
        return UNKNOWN
    }
}
