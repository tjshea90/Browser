package app.kestrel.core.media

import app.kestrel.core.download.FileNames

/**
 * What a video is called when it is saved. The owner wants "the name of the file given by the source", with a way to rename it before the download starts. The source's name is the
 * Content-Disposition file name when the server sent one, else the last part of the address; a name that says nothing ("get.php", "master", "videoplayback", a long token) is not a
 * name, and then the page's title is the better one. The sheet pre-fills this and the owner can edit it. Pure: MediaNamesTest.
 */
object MediaNames {
    /** Words that name a role, not a video. */
    private val GENERIC = setOf(
        "index", "master", "playlist", "video", "videos", "videoplayback", "get", "get_file", "getfile", "download", "downloads", "dl", "stream", "file", "media", "play", "player", "manifest",
        "chunklist", "segment", "init", "embed", "source", "content", "data", "vod", "hls", "mp4", "watch", "view", "v", "audio", "track", "clip", "movie", "main", "default", "out", "output", "temp", "tmp",
    )
    private val EXT = Regex("""\.[A-Za-z0-9]{2,5}$""")

    /** The name the SOURCE gave, without its extension; "" when it gave none worth showing. */
    fun sourceStem(c: MediaCandidate): String {
        val fromHeader = c.sourceName.trim()
        if (fromHeader.isNotEmpty()) stemOf(fromHeader).takeIf { isRealName(it) }?.let { return clean(it) }
        val fromUrl = FileNames.fromUrl(c.url) ?: return ""
        return stemOf(fromUrl).takeIf { isRealName(it) }?.let { clean(it) } ?: ""
    }

    /** What the sheet pre-fills (no extension): the source's own name, else "Page title - 720p" (the host when the page has no title). */
    fun defaultStem(c: MediaCandidate, pageTitle: String, pageHost: String, qualityLabel: String): String {
        val s = sourceStem(c)
        return if (s.isNotEmpty()) s else clean(MediaLabels.fileStem(pageTitle, pageHost, qualityLabel))
    }

    /** True when [stem] reads like a name a person or a site chose, not a role word or a token. */
    fun isRealName(stem: String): Boolean {
        val s = stem.trim()
        if (s.length < 3 || s.none { it.isLetter() }) return false
        val lower = s.lowercase()
        if (lower in GENERIC) return false
        // "videoplayback", "get_file" with a number after it ("master2", "index_1"): still a role word
        if (lower.trimEnd { it.isDigit() || it == '_' || it == '-' } in GENERIC) return false
        // a long run of letters and digits with no separator at all is a token / hash, not a name
        if (s.length >= 20 && s.none { it == ' ' || it == '_' || it == '-' || it == '.' } && s.any { it.isDigit() }) return false
        if (s.length >= 12 && s.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) return false
        return true
    }

    /** "Movie_1080p.mp4" -> "Movie_1080p" (an extension of 2-5 letters or digits is cut off; a dot inside a name is kept). */
    fun stemOf(name: String): String {
        val n = name.substringAfterLast('/').substringAfterLast('\\').substringBefore('?')
        return if (EXT.containsMatchIn(n) && n.substringBeforeLast('.').isNotEmpty()) n.substringBeforeLast('.') else n
    }

    /** Safe and tidy for a file system, one line, no longer than a screen can show. */
    fun clean(stem: String): String = FileNames.sanitize(stem.replace(Regex("""\s+"""), " ").trim()).take(120).trimEnd('.', ' ')
}
