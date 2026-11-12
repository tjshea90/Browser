package app.kestrel.core.download

/**
 * "Is this what the link said it was?" A link that expired, a site that wants a sign-in or a carrier's captive portal answers 200 with a WEB PAGE, and without this check the page is saved as
 * "movie.mp4" (or, worse, its text lands inside a video made of segments). Pure functions: the engines call them on the first bytes of a download and on every stream segment.
 */
object ContentSniff {
    private val BINARY_EXT = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "avi", "ts", "m4s", "m4a", "mp3", "aac", "ogg", "opus", "flac", "wav", "3gp",
        "zip", "rar", "7z", "gz", "tgz", "xz", "bz2", "tar", "apk", "aab", "exe", "msi", "iso", "dmg", "bin", "img",
        "pdf", "epub", "jpg", "jpeg", "png", "gif", "webp", "avif", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    )

    /**
     * True when the caller (a video the page was playing, a file name with a binary extension) says this is not a web page. [nameOrUrl] is the file name or the address; [mimeHint] the type the caller
     * expects. Anything else (no extension, no hint, .html, .php, a text type) expects nothing and is never second-guessed.
     */
    fun expectsBinary(nameOrUrl: String?, mimeHint: String?): Boolean {
        val m = mimeHint?.substringBefore(';')?.trim()?.lowercase()
        if (m != null && (m.startsWith("video/") || m.startsWith("audio/") || m.startsWith("image/") || m == "application/zip" || m == "application/pdf" || m == "application/vnd.android.package-archive")) return true
        val path = nameOrUrl?.substringBefore('#')?.substringBefore('?')?.lowercase() ?: return false
        val dot = path.lastIndexOf('.')
        if (dot < 0 || path.length - dot > 6 || path.indexOf('/', dot) >= 0) return false
        return path.substring(dot + 1) in BINARY_EXT
    }

    /** A web page by its content type (the server told us honestly). */
    fun isHtmlType(contentType: String?): Boolean {
        val c = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return false
        return c == "text/html" || c == "application/xhtml+xml"
    }

    /** The first bytes are an HTML document: after an optional byte order mark and white space, a doctype or one of the tags a page starts with. */
    fun looksLikeHtml(b: ByteArray, off: Int, len: Int): Boolean {
        var i = off
        val end = minOf(off + len, b.size)
        if (end - i >= 3 && b[i] == 0xEF.toByte() && b[i + 1] == 0xBB.toByte() && b[i + 2] == 0xBF.toByte()) i += 3
        while (i < end && (b[i] == ' '.code.toByte() || b[i] == '\n'.code.toByte() || b[i] == '\r'.code.toByte() || b[i] == '\t'.code.toByte())) i++
        if (i >= end || b[i] != '<'.code.toByte()) return false
        val head = StringBuilder()
        var j = i
        while (j < end && head.length < 24) { head.append((b[j].toInt() and 0xFF).toChar()); j++ }
        val h = head.toString().lowercase()
        return h.startsWith("<!doctype html") || h.startsWith("<html") || h.startsWith("<head") || h.startsWith("<body") || h.startsWith("<script") || h.startsWith("<title") || h.startsWith("<meta") ||
            h.startsWith("<!--") && String(b, i, minOf(end - i, 512), Charsets.ISO_8859_1).lowercase().contains("<html")
    }
}
