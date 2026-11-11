package app.kestrel.core.media

/**
 * HLS playlist parser (RFC 8216): master playlists (quality variants) and media playlists (the segment list). Pure Kotlin, tolerant of what real servers send, and strict only about what
 * cannot be saved: live streams, DRM and SAMPLE-AES. Design and limits: research/VIDEO_DOWNLOADER.md section 4.
 */
object Hls {
    class Variant(
        val url: String,
        val bandwidth: Long,
        val avgBandwidth: Long,
        val width: Int,
        val height: Int,
        val codecs: String,
        val frameRate: Double,
        val audioGroup: String?,
    ) {
        private fun codecList(): List<String> = codecs.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        /** No picture: every codec listed is an audio one (and the playlist says no resolution). */
        val audioOnly: Boolean get() = height == 0 && width == 0 && codecList().let { it.isNotEmpty() && it.all(::isAudioCodec) }
        /** "H.264", "H.265", "VP9", "AV1" or "" when unknown. */
        val videoCodec: String get() = codecList().firstNotNullOfOrNull { c ->
            when {
                c.startsWith("avc1") || c.startsWith("avc3") -> "H.264"
                c.startsWith("hvc1") || c.startsWith("hev1") -> "H.265"
                c.startsWith("vp09") || c.startsWith("vp9") -> "VP9"
                c.startsWith("av01") -> "AV1"
                else -> null
            }
        } ?: ""
        /** Bytes per second the playlist promises (average if it says so, else the peak). */
        val bytesPerSecond: Long get() = (if (avgBandwidth > 0) avgBandwidth else bandwidth) / 8
        /** "1080p", "720p60" (frame rate >= 50), "Audio only". */
        val label: String get() = when {
            audioOnly -> "Audio only"
            height > 0 -> "${height}p" + (if (frameRate >= 50.0) frameRate.toInt().toString() else "") + (if (height >= 2000) "  \u00B7  4K" else "")
            width > 0 -> "${width}px wide"
            else -> "Stream"
        }
        private fun isAudioCodec(c: String) = c.startsWith("mp4a") || c.startsWith("ac-3") || c.startsWith("ec-3") || c.startsWith("opus") || c.startsWith("flac") || c.startsWith("vorbis") || c.startsWith("alac")
    }

    class Rendition(val type: String, val groupId: String, val name: String, val url: String?, val isDefault: Boolean, val language: String?)
    class Master(val variants: List<Variant>, val renditions: List<Rendition>) {
        /** The separate audio playlist a variant needs (its AUDIO group's default rendition with its own URI), or null when the audio is inside the variant. */
        fun audioFor(v: Variant): Rendition? {
            val g = v.audioGroup ?: return null
            val all = renditions.filter { it.type == "AUDIO" && it.groupId == g && it.url != null }
            return all.firstOrNull { it.isDefault } ?: all.firstOrNull()
        }
    }

    /** The initialization section of fragmented MP4 (EXT-X-MAP): written once, before the first segment. */
    class Init(val url: String, val from: Long, val len: Long)
    class Seg(val url: String, val durationMs: Long, val from: Long, val len: Long, val key: Int, val seq: Long, val discontinuity: Boolean)
    /** An AES-128 key: its address and the IV the playlist names (null = derive it from the segment's sequence number). */
    class Key(val url: String, val iv: ByteArray?)

    enum class Problem { LIVE, SAMPLE_AES, DRM, UNSUPPORTED, EMPTY }

    class Media(
        val segments: List<Seg>,
        val keys: List<Key>,
        val init: Init?,
        val endList: Boolean,
        val targetMs: Long,
        val mediaSequence: Long,
        val problem: Problem?,
    ) {
        val durationMs: Long = segments.sumOf { it.durationMs }
        val discontinuities: Int = segments.count { it.discontinuity }
        /** The size of everything when every segment names its byte range (exact), else -1. */
        val exactBytes: Long = if (segments.isNotEmpty() && segments.all { it.len >= 0 }) segments.sumOf { it.len } + (init?.len?.takeIf { it >= 0 } ?: 0) else -1L
        /** What to save it as: "mp4" for fragmented MP4, "aac" / "mp3" for packed audio, else "ts". */
        val container: String get() {
            if (init != null) return "mp4"
            val first = segments.firstOrNull()?.url ?: return "ts"
            return when (MediaSniff.extension(first)) { "aac" -> "aac"; "mp3" -> "mp3"; "mp4", "m4s", "m4v", "cmfv" -> "mp4"; "m4a", "cmfa" -> "m4a"; else -> "ts" }
        }
    }

    sealed class Playlist
    class MasterPlaylist(val master: Master) : Playlist()
    class MediaPlaylist(val media: Media) : Playlist()

    /** @return null when [text] is not an HLS playlist at all (an HTML error page, an empty reply). */
    fun parse(text: String, baseUrl: String): Playlist? {
        val lines = text.removePrefix("﻿").lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) return null
        return if (lines.any { it.startsWith("#EXT-X-STREAM-INF") }) MasterPlaylist(parseMaster(lines, baseUrl)) else MediaPlaylist(parseMedia(lines, baseUrl))
    }

    private fun parseMaster(lines: List<String>, base: String): Master {
        val variants = ArrayList<Variant>(); val renditions = ArrayList<Rendition>()
        var i = 0
        while (i < lines.size) {
            val l = lines[i]
            when {
                l.startsWith("#EXT-X-STREAM-INF:") -> {
                    val a = attrs(l.substringAfter(':'))
                    var j = i + 1
                    while (j < lines.size && lines[j].startsWith("#")) j++ // other tags between the attributes and the address
                    if (j < lines.size) {
                        val (w, h) = a["RESOLUTION"]?.split('x', 'X')?.let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) } ?: (0 to 0)
                        variants.add(Variant(resolve(base, lines[j]), a["BANDWIDTH"]?.toLongOrNull() ?: 0, a["AVERAGE-BANDWIDTH"]?.toLongOrNull() ?: 0, w, h, a["CODECS"] ?: "", a["FRAME-RATE"]?.toDoubleOrNull() ?: 0.0, a["AUDIO"]))
                        i = j
                    }
                }
                l.startsWith("#EXT-X-MEDIA:") -> {
                    val a = attrs(l.substringAfter(':'))
                    renditions.add(Rendition(a["TYPE"] ?: "", a["GROUP-ID"] ?: "", a["NAME"] ?: "", a["URI"]?.let { resolve(base, it) }, a["DEFAULT"] == "YES", a["LANGUAGE"]))
                }
            }
            i++
        }
        return Master(variants, renditions)
    }

    private fun parseMedia(lines: List<String>, base: String): Media {
        val segs = ArrayList<Seg>(); val keys = ArrayList<Key>()
        var init: Init? = null
        var target = 0L; var mediaSeq = 0L
        var endList = false; var vod = false
        var problem: Problem? = null
        var curKey = -1
        var dur = 0L; var pendingFrom = -1L; var pendingLen = -1L; var disc = false; var gap = false
        var lastUrl: String? = null; var lastEnd = 0L
        var seq = 0L
        var seqSet = false
        for (l in lines) {
            when {
                l.startsWith("#EXT-X-TARGETDURATION:") -> target = ((l.substringAfter(':').trim().toDoubleOrNull() ?: 0.0) * 1000).toLong()
                l.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> { mediaSeq = l.substringAfter(':').trim().toLongOrNull() ?: 0; seq = mediaSeq; seqSet = true }
                l.startsWith("#EXT-X-PLAYLIST-TYPE:") -> if (l.substringAfter(':').trim().equals("VOD", true)) vod = true
                l.startsWith("#EXT-X-ENDLIST") -> endList = true
                l.startsWith("#EXT-X-DISCONTINUITY-SEQUENCE") -> {}
                l.startsWith("#EXT-X-DISCONTINUITY") -> disc = true
                l.startsWith("#EXT-X-GAP") -> gap = true
                l.startsWith("#EXT-X-KEY:") -> {
                    val a = attrs(l.substringAfter(':'))
                    val method = (a["METHOD"] ?: "NONE").uppercase()
                    val format = (a["KEYFORMAT"] ?: "identity").lowercase()
                    when {
                        method == "NONE" -> curKey = -1
                        format != "identity" -> { problem = problem ?: Problem.DRM; curKey = -1 }
                        method == "AES-128" -> {
                            val uri = a["URI"]
                            if (uri == null) { problem = problem ?: Problem.UNSUPPORTED; curKey = -1 }
                            else { keys.add(Key(resolve(base, uri), a["IV"]?.let { parseIv(it) })); curKey = keys.size - 1 }
                        }
                        else -> { problem = problem ?: Problem.SAMPLE_AES; curKey = -1 } // SAMPLE-AES, SAMPLE-AES-CTR
                    }
                }
                l.startsWith("#EXT-X-MAP:") -> {
                    val a = attrs(l.substringAfter(':'))
                    val uri = a["URI"]
                    if (uri != null) {
                        val (len, off) = parseRange(a["BYTERANGE"], null, 0)
                        val m = Init(resolve(base, uri), if (len >= 0) off else -1, len)
                        if (init == null) init = m else if (init.url != m.url || init.from != m.from) problem = problem ?: Problem.UNSUPPORTED
                    }
                }
                l.startsWith("#EXTINF:") -> dur = ((l.substringAfter(':').substringBefore(',').trim().toDoubleOrNull() ?: 0.0) * 1000).toLong()
                l.startsWith("#EXT-X-BYTERANGE:") -> { val (len, off) = parseRange(l.substringAfter(':'), lastUrl, lastEnd); pendingLen = len; pendingFrom = if (len >= 0) off else -1 }
                l.startsWith("#") -> {}
                else -> {
                    val url = resolve(base, l)
                    if (gap) { gap = false; dur = 0; pendingFrom = -1; pendingLen = -1; seq++; continue } // a hole the server marked: nothing to fetch
                    segs.add(Seg(url, dur, pendingFrom, pendingLen, curKey, seq, disc))
                    if (pendingLen >= 0) { lastUrl = url; lastEnd = pendingFrom + pendingLen } else { lastUrl = null; lastEnd = 0 }
                    seq++; dur = 0; pendingFrom = -1; pendingLen = -1; disc = false
                }
            }
        }
        if (!seqSet) mediaSeq = 0
        if (problem == null && !endList && !vod) problem = Problem.LIVE
        if (problem == null && segs.isEmpty()) problem = Problem.EMPTY
        return Media(segs, keys, init, endList || vod, target, mediaSeq, problem)
    }

    /** "n@o" or "n" -> (length, offset); a missing offset continues after the previous range of the same resource ([prevEnd]); (-1, -1) when unusable. */
    private fun parseRange(v: String?, prevUrl: String?, prevEnd: Long): Pair<Long, Long> {
        if (v == null) return -1L to -1L
        val s = v.trim().trim('"')
        val n = s.substringBefore('@').toLongOrNull() ?: return -1L to -1L
        val o = if (s.contains('@')) s.substringAfter('@').toLongOrNull() ?: return -1L to -1L else (if (prevUrl != null || prevEnd > 0) prevEnd else 0L)
        return n to o
    }

    /** IV="0x..." -> 16 bytes (left-padded with zeros if shorter); null if it is not hex. */
    private fun parseIv(v: String): ByteArray? {
        val h = v.trim().removePrefix("0x").removePrefix("0X")
        if (h.isEmpty() || h.length > 32 || !h.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        val p = h.padStart(32, '0')
        return ByteArray(16) { p.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    /** The IV for a segment of a key whose playlist gave none: the media sequence number as 16 big-endian bytes (RFC 8216 5.2). */
    fun sequenceIv(seq: Long): ByteArray {
        val b = ByteArray(16)
        var v = seq
        for (i in 15 downTo 8) { b[i] = (v and 0xFF).toByte(); v = v ushr 8 }
        return b
    }

    /** KEY=VALUE,KEY="quoted, value",... */
    internal fun attrs(s: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        var i = 0
        val n = s.length
        while (i < n) {
            while (i < n && (s[i] == ',' || s[i] == ' ')) i++
            val eq = s.indexOf('=', i)
            if (eq < 0) break
            val key = s.substring(i, eq).trim().uppercase()
            i = eq + 1
            val value: String
            if (i < n && s[i] == '"') {
                val end = s.indexOf('"', i + 1).let { if (it < 0) n else it }
                value = s.substring(i + 1, end); i = minOf(end + 1, n)
            } else {
                val end = s.indexOf(',', i).let { if (it < 0) n else it }
                value = s.substring(i, end).trim(); i = end
            }
            if (key.isNotEmpty()) out.putIfAbsent(key, value)
        }
        return out
    }

    /** [ref] relative to [base] (RFC 3986): absolute, "//host/x", "/x", "x/y", "../y", "?q". */
    fun resolve(base: String, ref: String): String {
        val r = ref.trim()
        if (r.startsWith("http://", true) || r.startsWith("https://", true)) return r
        try { return java.net.URI(base).resolve(java.net.URI(r)).toString() } catch (e: Exception) { /* a space or other character URI rejects: do it by hand */ }
        val scheme = base.substringBefore("://", "https")
        val rest = base.substringAfter("://", base).substringBefore('#')
        val host = rest.substringBefore('/')
        val path = rest.substring(host.length).substringBefore('?')
        return when {
            r.startsWith("//") -> "$scheme:$r"
            r.startsWith("/") -> "$scheme://$host$r"
            r.startsWith("?") -> "$scheme://$host$path$r"
            else -> {
                val dir = path.substringBeforeLast('/', "")
                val parts = ArrayList<String>(dir.split('/').filter { it.isNotEmpty() })
                val tail = ArrayList<String>()
                val rel = r.substringBefore('?'); val q = if (r.contains('?')) "?" + r.substringAfter('?') else ""
                for (seg in rel.split('/')) when (seg) { "." -> {}; ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1); else -> tail.add(seg) }
                "$scheme://$host/" + (parts + tail).joinToString("/") + q
            }
        }
    }
}
