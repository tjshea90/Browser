package app.kestrel.core.media

/** What a sniffed response is. Design and limits: research/VIDEO_DOWNLOADER.md. */
enum class MediaKind { FILE, HLS, DASH }

/**
 * Decides, from nothing but a response's URL, Content-Type and status, whether it is something the user could want to save: a playable file, an HLS playlist or a DASH manifest.
 * Segments of a stream (".ts", ".m4s") are NOT candidates: the playlist that lists them is. Pure and conservative: a false "yes" shows a useless row, a false "no" hides a video.
 */
object MediaSniff {
    private val hlsTypes = setOf("application/vnd.apple.mpegurl", "application/x-mpegurl", "application/mpegurl", "audio/mpegurl", "audio/x-mpegurl", "vnd.apple.mpegurl")
    private val fileExts = setOf("mp4", "m4v", "mov", "webm", "mkv", "avi", "wmv", "flv", "3gp", "mpg", "mpeg", "m4a", "mp3", "aac", "ogg", "oga", "ogv", "opus", "wav", "flac")
    private val segmentExts = setOf("ts", "m4s", "cmfv", "cmfa", "cmft", "m4f", "fmp4", "mts")
    private val segmentTypes = setOf("video/mp2t", "video/iso.segment", "audio/iso.segment", "video/vnd.dlna.mpeg-tts", "application/x-mpegts")
    private val genericTypes = setOf("application/octet-stream", "binary/octet-stream", "application/force-download", "application/x-download", "application/download", "application/unknown", "")
    private val audioExts = setOf("m4a", "mp3", "aac", "ogg", "oga", "opus", "wav", "flac")

    /** [contentType] may carry parameters ("video/mp4; codecs=..."); [status] 200 / 206 only (0 = unknown, accepted). */
    fun classify(url: String, contentType: String?, status: Int = 200): MediaKind? {
        if (status != 0 && status != 200 && status != 206) return null
        if (!(url.startsWith("http://", true) || url.startsWith("https://", true))) return null
        val ct = baseType(contentType)
        val ext = extension(url)
        if (ct.startsWith("text/html") || ct.startsWith("image/") || ct.startsWith("font/") || ct == "application/json" || ct.contains("javascript") || ct == "text/css") return null
        if (ct in hlsTypes || ext == "m3u8" || ext == "m3u") return if (ct.startsWith("text/") && ct != "text/plain" && ext != "m3u8") null else MediaKind.HLS
        if (ct == "application/dash+xml" || ext == "mpd") return MediaKind.DASH
        if (ct in segmentTypes || ext in segmentExts) return null
        if (ct.startsWith("video/") || ct.startsWith("audio/")) return MediaKind.FILE
        if (ct == "application/ogg" || ct == "application/mp4" || ct == "application/x-matroska") return MediaKind.FILE
        if (ct in genericTypes && ext in fileExts) return MediaKind.FILE
        return null
    }

    /** "video/mp4; codecs=avc1" -> "video/mp4" (lower case, no parameters). */
    fun baseType(contentType: String?): String = contentType?.substringBefore(';')?.trim()?.lowercase() ?: ""

    /** The lower-case extension of the URL's path ("clip.MP4?x=1" -> "mp4"), "" if none. */
    fun extension(url: String): String {
        val path = pathOf(url)
        val name = path.substringAfterLast('/')
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.length - 1 || name.length - dot > 8) return ""
        return name.substring(dot + 1).lowercase()
    }

    /** The file extension to save a [kind]/[contentType] under: from the URL when it has a media one, else from the type. */
    fun saveExtension(url: String, contentType: String?): String {
        val e = extension(url)
        if (e in fileExts) return e
        return when (baseType(contentType)) {
            "video/mp4", "application/mp4" -> "mp4"
            "video/webm" -> "webm"
            "video/quicktime" -> "mov"
            "video/x-matroska", "application/x-matroska" -> "mkv"
            "video/ogg", "application/ogg" -> "ogv"
            "video/3gpp" -> "3gp"
            "video/x-flv" -> "flv"
            "video/mpeg" -> "mpg"
            "video/x-msvideo" -> "avi"
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/aac", "audio/x-aac" -> "aac"
            "audio/ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/webm" -> "weba"
            else -> if (baseType(contentType).startsWith("audio/")) "m4a" else "mp4"
        }
    }

    /** Audio only (no picture): by type or extension. */
    fun isAudio(url: String, contentType: String?): Boolean {
        val ct = baseType(contentType)
        if (ct.startsWith("audio/")) return true
        if (ct.startsWith("video/")) return false
        return extension(url) in audioExts
    }

    /** Resolution hinted by the URL ("clip_720p.mp4", "/1080p/x.mp4", "x-480.mp4"): the picture height, or 0. */
    fun heightFromUrl(url: String): Int {
        val p = pathOf(url) + "?" + (url.substringAfter('?', "").substringBefore('#'))
        val m = heightP.find(p) ?: heightUnderscore.find(p) ?: return 0
        val h = m.groupValues[1].toIntOrNull() ?: return 0
        return if (h in knownHeights) h else 0
    }

    private val heightP = Regex("""(?<![0-9a-zA-Z])(\d{3,4})p(?![0-9a-zA-Z])""", RegexOption.IGNORE_CASE)
    private val heightUnderscore = Regex("""[_\-/](\d{3,4})[_\-.](?:mp4|webm|m4v|mov)""", RegexOption.IGNORE_CASE)
    private val knownHeights = setOf(144, 240, 360, 380, 404, 432, 480, 540, 576, 720, 1080, 1440, 2160, 4320)

    private fun pathOf(url: String): String {
        val noFrag = url.substringBefore('#')
        val afterScheme = noFrag.substringAfter("://", noFrag)
        val slash = afterScheme.indexOf('/')
        val rest = if (slash < 0) "" else afterScheme.substring(slash)
        return rest.substringBefore('?')
    }
}
