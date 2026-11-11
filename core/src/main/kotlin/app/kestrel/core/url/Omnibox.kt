package app.kestrel.core.url

import java.net.URLEncoder

/** What a string typed/pasted into the address bar means. */
sealed class Nav {
    /** Load this URL (always http/https or about:blank). */
    data class Go(val url: String) : Nav()

    /** A web search; [url] is already the search-engine URL. */
    data class Search(val url: String, val query: String) : Nav()

    /** Input we refuse to navigate to (javascript:, file:, data:, intent:, ...). */
    data class Rejected(val reason: String) : Nav()

    object Empty : Nav()
}

/**
 * Address-bar interpretation. Security rule: only http(s) and about:blank can ever come out as [Nav.Go]; scripting and
 * local-file schemes are rejected outright (no `javascript:` bookmarklets, no `file:`/`content:` probing).
 */
object Omnibox {
    private val BLOCKED_SCHEMES = setOf(
        "javascript", "data", "file", "content", "intent", "blob", "vbscript", "view-source", "chrome", "ftp", "filesystem",
        "android-app", "market", "about", // "about:blank" is handled before this check
    )

    /**
     * Suffixes that people far more often mean as FILE extensions when typed bare ("main.py", "notes.txt"): some are real
     * ccTLDs (py, sh, rs, md, pl, cs), others are not TLDs at all. A bare "name.ext" with one of these is a search.
     */
    private val FILE_LOOKALIKES = setOf(
        "py", "sh", "rs", "md", "pl", "cs", "rb", "so", "ps", "pm", "pp", "kt",
        "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "7z", "jpg", "jpeg", "png", "gif", "webp",
        "svg", "mp3", "mp4", "mkv", "avi", "mov", "html", "htm", "css", "js", "json", "xml", "yaml", "yml", "java", "exe",
        "apk", "bat", "log", "csv", "jar", "gz", "tar", "tgz", "ini", "cfg", "conf", "toml", "tmp", "bak", "php", "asp", "aspx",
    )

    @JvmStatic
    fun resolve(input: String, searchTemplate: String): Nav {
        val s = input.trim()
        if (s.isEmpty()) return Nav.Empty
        if (s.equals("about:blank", ignoreCase = true)) return Nav.Go("about:blank")

        // explicit scheme?
        val schemeEnd = schemeEnd(s)
        if (schemeEnd > 0) {
            val scheme = s.substring(0, schemeEnd).lowercase()
            if (scheme == "http" || scheme == "https") {
                if (s.length <= schemeEnd + 3 || s.any { it.isWhitespace() }) return search(s, searchTemplate)
                if (UrlParts.parse(s) == null) return search(s, searchTemplate) // malformed authority -> treat as text
                return Nav.Go(s) // a valid typed URL goes through exactly as typed
            }
            if (scheme in BLOCKED_SCHEMES && !looksLikeHostPort(s)) {
                return Nav.Rejected("Blocked address type: $scheme:")
            }
        }

        if (s.any { it.isWhitespace() }) return search(s, searchTemplate)

        val hostEnd = s.indexOfAny(charArrayOf('/', '?', '#'))
        val hostPort = if (hostEnd >= 0) s.substring(0, hostEnd) else s
        val rest = if (hostEnd >= 0) s.substring(hostEnd) else ""
        if (hostPort.indexOf('@') >= 0) return search(s, searchTemplate) // "me@example.com" is an email, not a login URL
        val parsed = UrlParts.parse("http://$hostPort") ?: return search(s, searchTemplate)
        val host = parsed.host

        val plausible = when {
            parsed.isIpLiteral -> true
            host == "localhost" -> true
            NetHosts.isLocal(host) && host.indexOf('.') >= 0 -> true // foo.local, nas.lan
            // "intranet:8080" yes, "hello" no, and "3:1" / "10:30" (scores, times, ratios: a host label with no letter in it) no
            host.indexOf('.') < 0 -> hostPort.contains(':') && parsed.port > 0 && host.any { it.isLetter() }
            else -> tldPlausible(host) && !(rest.isEmpty() && parsed.port < 0 && isFileLookalike(host))
        }
        if (!plausible) return search(s, searchTemplate)

        // Local/LAN/IP hosts speak http (router pages, dev servers; certificates for bare IPs are practically nonexistent);
        // everything else is https-first.
        val scheme = if (parsed.isIpLiteral || NetHosts.isLocal(host)) "http" else "https"
        return Nav.Go("$scheme://$s")
    }

    @JvmStatic
    fun searchUrl(query: String, template: String): String =
        template.replace("%s", URLEncoder.encode(query, "UTF-8"))

    private fun search(q: String, template: String): Nav = Nav.Search(searchUrl(q, template), q)

    /** Index of ':' when the string starts with `scheme://` or `scheme:` (non-numeric after), else -1. */
    private fun schemeEnd(s: String): Int {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val ok = (c in 'a'..'z') || (c in 'A'..'Z') || (i > 0 && ((c in '0'..'9') || c == '+' || c == '-' || c == '.'))
            if (!ok) break
            i++
        }
        if (i == 0 || i >= s.length || s[i] != ':') return -1
        return i
    }

    /** "localhost:8080", "example.com:443/x": colon followed by digits is a port, not a scheme. */
    private fun looksLikeHostPort(s: String): Boolean {
        val colon = s.indexOf(':')
        if (colon < 0 || colon + 1 >= s.length) return false
        var k = colon + 1
        if (!s[k].isDigit()) return false
        while (k < s.length && s[k].isDigit()) k++
        return k == s.length || s[k] == '/' || s[k] == '?' || s[k] == '#'
    }

    private fun tldPlausible(host: String): Boolean {
        val tld = host.substring(host.lastIndexOf('.') + 1)
        if (tld.startsWith("xn--") && tld.length > 4) return true
        if (tld.length < 2 || tld.length > 24) return false
        return tld.all { it in 'a'..'z' }
    }

    private fun isFileLookalike(host: String): Boolean =
        host.indexOf('.') == host.lastIndexOf('.') && host.substring(host.lastIndexOf('.') + 1) in FILE_LOOKALIKES
}
