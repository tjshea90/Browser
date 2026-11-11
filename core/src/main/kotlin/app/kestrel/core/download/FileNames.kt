package app.kestrel.core.download

import java.net.URLDecoder
import java.text.Normalizer
import java.util.Locale

/** File names from servers: RFC 6266 / 8187 Content-Disposition, then the URL path, then the type; cleaned so they are safe on any file system (research/DOWNLOADS.md section 7). */
object FileNames {
    private val RESERVED = setOf("CON", "PRN", "AUX", "NUL") + (1..9).map { "COM$it" } + (1..9).map { "LPT$it" }

    private val MIME_EXT = mapOf(
        "application/pdf" to "pdf", "application/zip" to "zip", "application/vnd.android.package-archive" to "apk", "application/x-7z-compressed" to "7z",
        "application/vnd.rar" to "rar", "application/x-rar-compressed" to "rar", "application/gzip" to "gz", "application/x-gzip" to "gz", "application/x-tar" to "tar",
        "application/json" to "json", "application/xml" to "xml", "text/xml" to "xml", "text/plain" to "txt", "text/html" to "html", "text/csv" to "csv",
        "application/epub+zip" to "epub", "application/x-iso9660-image" to "iso", "application/msword" to "doc",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx", "application/vnd.ms-excel" to "xls",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx", "application/vnd.ms-powerpoint" to "ppt",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "pptx",
        "image/jpeg" to "jpg", "image/png" to "png", "image/gif" to "gif", "image/webp" to "webp", "image/svg+xml" to "svg", "image/avif" to "avif",
        "video/mp4" to "mp4", "video/webm" to "webm", "video/x-matroska" to "mkv", "audio/mpeg" to "mp3", "audio/mp4" to "m4a", "audio/ogg" to "ogg",
        "audio/wav" to "wav", "audio/x-wav" to "wav", "audio/flac" to "flac", "audio/aac" to "aac",
    )

    /** The name to save under: Content-Disposition, else the URL's last segment, else [hint], else "download"; extension added from [mime] when missing. */
    fun choose(contentDisposition: String?, url: String, mime: String?, hint: String? = null): String {
        val raw = fromContentDisposition(contentDisposition) ?: fromUrl(url) ?: hint ?: "download"
        return withExtensionFor(mime, sanitize(raw))
    }

    fun fromContentDisposition(cd: String?): String? {
        val s = cd ?: return null
        var plain: String? = null
        var star: String? = null
        for ((k, v) in params(s)) {
            when (k.lowercase(Locale.ROOT)) {
                "filename*" -> star = decodeExt(v) ?: star
                "filename" -> plain = plain ?: v
            }
        }
        return (star ?: plain)?.takeIf { it.isNotBlank() }
    }

    /** `name=value` pairs after the disposition type; values may be quoted strings with backslash escapes. */
    private fun params(s: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        var i = s.indexOf(';')
        while (i >= 0 && i < s.length) {
            i++
            while (i < s.length && (s[i] == ' ' || s[i] == '\t')) i++
            val eq = s.indexOf('=', i)
            if (eq < 0) break
            val semiBefore = s.indexOf(';', i)
            if (semiBefore in 0 until eq) { i = semiBefore; continue }
            val key = s.substring(i, eq).trim()
            var j = eq + 1
            while (j < s.length && (s[j] == ' ' || s[j] == '\t')) j++
            val value = StringBuilder()
            if (j < s.length && s[j] == '"') {
                j++
                while (j < s.length && s[j] != '"') { if (s[j] == '\\' && j + 1 < s.length) j++; value.append(s[j]); j++ }
                j++
            } else {
                while (j < s.length && s[j] != ';') { value.append(s[j]); j++ }
            }
            out.add(key to value.toString().trim())
            i = s.indexOf(';', j.coerceAtMost(s.length))
        }
        return out
    }

    /** RFC 8187: charset'language'percent-encoded. UTF-8 and ISO-8859-1 only. */
    private fun decodeExt(v: String): String? {
        val a = v.indexOf('\'')
        val b = if (a >= 0) v.indexOf('\'', a + 1) else -1
        if (a < 0 || b < 0) return null
        val cs = v.substring(0, a).uppercase(Locale.ROOT)
        val enc = v.substring(b + 1)
        val charset = when (cs) { "UTF-8" -> Charsets.UTF_8; "ISO-8859-1" -> Charsets.ISO_8859_1; else -> return null }
        return try { pctDecode(enc, charset) } catch (e: Exception) { null }
    }

    private fun pctDecode(s: String, cs: java.nio.charset.Charset): String {
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length + 0 && i + 2 <= s.length - 1 + 0) {
                val hex = s.substring(i + 1, i + 3)
                bytes.write(hex.toInt(16)); i += 3
            } else { bytes.write(c.toString().toByteArray(Charsets.UTF_8)); i++ }
        }
        return String(bytes.toByteArray(), cs)
    }

    fun fromUrl(url: String): String? {
        val noQuery = url.substringBefore('#').substringBefore('?')
        val path = noQuery.substringAfter("://", noQuery).let { p -> val slash = p.indexOf('/'); if (slash < 0) "" else p.substring(slash) }
        val last = path.trimEnd('/').substringAfterLast('/')
        if (last.isEmpty()) return null
        return try { URLDecoder.decode(last.replace("+", "%2B"), "UTF-8") } catch (e: Exception) { last }.takeIf { it.isNotBlank() }
    }

    /** Safe on FAT / Windows / MediaStore: no control, bidi or zero-width characters, no path or reserved characters, no leading dot, <= 255 UTF-8 bytes keeping the extension. */
    fun sanitize(name: String, maxBytes: Int = 255): String {
        val sb = StringBuilder()
        for (ch in Normalizer.normalize(name, Normalizer.Form.NFC)) {
            val cp = ch.code
            val bad = Character.isISOControl(ch) || cp in 0x200B..0x200F || cp in 0x202A..0x202E || cp in 0x2066..0x2069 || cp == 0xFEFF || cp == 0x2028 || cp == 0x2029
            if (bad) continue
            sb.append(if (ch in "\\/:*?\"<>|") '_' else ch)
        }
        var s = sb.toString().trim().trimStart('.').trimEnd('.', ' ')
        if (s.isEmpty()) return "download"
        val dot = s.lastIndexOf('.')
        val base0 = if (dot > 0) s.substring(0, dot) else s
        val ext = if (dot > 0 && s.length - dot <= 16) s.substring(dot) else ""
        val base = if (dot > 0 && ext.isEmpty()) s else base0
        val safeBase = if (base.uppercase(Locale.ROOT) in RESERVED) "_$base" else base
        s = safeBase + ext
        if (s.toByteArray(Charsets.UTF_8).size > maxBytes) {
            val room = maxBytes - ext.toByteArray(Charsets.UTF_8).size
            val cut = StringBuilder()
            var used = 0
            var i = 0
            while (i < safeBase.length) {
                val cp = safeBase.codePointAt(i)
                val piece = String(Character.toChars(cp))
                val n = piece.toByteArray(Charsets.UTF_8).size
                if (used + n > room) break
                cut.append(piece); used += n; i += Character.charCount(cp)
            }
            s = (if (cut.isEmpty()) "download" else cut.toString()) + ext
        }
        return s
    }

    fun withExtensionFor(mime: String?, name: String): String {
        val ext = MIME_EXT[mime?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)] ?: return name
        val dot = name.lastIndexOf('.')
        val has = dot > 0 && name.length - dot - 1 in 1..8
        return if (has) name else "$name.$ext"
    }

    /** "name.ext" -> "name (1).ext", "name (2).ext" ... until [exists] says the name is free. */
    fun dedupe(name: String, exists: (String) -> Boolean): String {
        if (!exists(name)) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 1
        while (n < 10_000) { val c = "$base ($n)$ext"; if (!exists(c)) return c; n++ }
        return "$base (${System.currentTimeMillis()})$ext"
    }
}
