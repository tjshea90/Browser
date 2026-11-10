package app.kestrel.core.url

/**
 * Minimal, allocation-light, never-throwing URL splitter for hierarchical URLs (`scheme://authority/...`).
 * Used on the hot request path, so it avoids java.net.URI (strict, slow) and android.net.Uri (not in :core).
 *
 * Normalisation that matters for security:
 *  - host is lower-cased and TRAILING DOTS ARE REMOVED (`ads.example.com.` must not dodge host rules);
 *  - IPv6 hosts are returned without brackets;
 *  - `user:pass@` is split off so it can never be mistaken for the host (`http://good.com@evil.com/`).
 */
class UrlParts private constructor(
    val scheme: String,
    val userInfo: String?,
    val host: String,
    val port: Int,
    val path: String,
    val query: String?,
    val fragment: String?,
) {
    val isHttp: Boolean get() = scheme == "http" || scheme == "https"
    val isIpLiteral: Boolean get() = NetHosts.isIpLiteral(host)

    /** Host as written in an authority (IPv6 re-bracketed). */
    val hostForAuthority: String get() = if (host.indexOf(':') >= 0) "[$host]" else host

    /** Rebuild a URL string from the (normalised) parts. */
    fun toUrl(): String {
        val sb = StringBuilder(64 + path.length + (query?.length ?: 0))
        sb.append(scheme).append("://")
        if (userInfo != null) sb.append(userInfo).append('@')
        sb.append(hostForAuthority)
        if (port >= 0) sb.append(':').append(port)
        sb.append(path)
        if (query != null) sb.append('?').append(query)
        if (fragment != null) sb.append('#').append(fragment)
        return sb.toString()
    }

    companion object {
        @JvmStatic
        fun parse(url: String): UrlParts? {
            val n = url.length
            var i = 0
            while (i < n) {
                val c = url[i]
                val ok = (c in 'a'..'z') || (c in 'A'..'Z') || (i > 0 && ((c in '0'..'9') || c == '+' || c == '-' || c == '.'))
                if (!ok) break
                i++
            }
            if (i == 0 || !url.startsWith("://", i)) return null
            val scheme = url.substring(0, i).lowercase()
            val authStart = i + 3
            var j = authStart
            while (j < n) {
                val c = url[j]
                if (c == '/' || c == '?' || c == '#') break
                j++
            }
            val authority = url.substring(authStart, j)
            val at = authority.lastIndexOf('@')
            val userInfo = if (at >= 0) authority.substring(0, at) else null
            val hostPort = if (at >= 0) authority.substring(at + 1) else authority

            var host: String
            var port = -1
            if (hostPort.startsWith("[")) {
                val close = hostPort.indexOf(']')
                if (close < 0) return null
                host = hostPort.substring(1, close)
                val rest = hostPort.substring(close + 1)
                if (rest.isNotEmpty()) {
                    if (rest[0] != ':') return null
                    port = parsePort(rest, 1) ?: return null
                }
            } else {
                val colon = hostPort.lastIndexOf(':')
                if (colon >= 0) {
                    port = parsePort(hostPort, colon + 1) ?: return null
                    host = hostPort.substring(0, colon)
                } else {
                    host = hostPort
                }
                if (host.indexOf(':') >= 0) return null
            }
            host = host.lowercase()
            var end = host.length
            while (end > 0 && host[end - 1] == '.') end--
            if (end != host.length) host = host.substring(0, end)
            if (host.isEmpty()) return null

            var path = ""
            var query: String? = null
            var fragment: String? = null
            if (j < n) {
                val rest = url.substring(j)
                val hash = rest.indexOf('#')
                val beforeHash = if (hash >= 0) rest.substring(0, hash) else rest
                if (hash >= 0) fragment = rest.substring(hash + 1)
                val q = beforeHash.indexOf('?')
                if (q >= 0) {
                    path = beforeHash.substring(0, q)
                    query = beforeHash.substring(q + 1)
                } else {
                    path = beforeHash
                }
            }
            return UrlParts(scheme, userInfo, host, port, path, query, fragment)
        }

        /** Host of a hierarchical URL, or null. */
        @JvmStatic
        fun hostOf(url: String): String? = parse(url)?.host

        private fun parsePort(s: String, from: Int): Int? {
            if (from >= s.length) return -1 // "host:" with empty port is tolerated
            var v = 0
            for (k in from until s.length) {
                val d = s[k] - '0'
                if (d < 0 || d > 9) return null
                v = v * 10 + d
                if (v > 65535) return null
            }
            return v
        }
    }
}

/** Host classification helpers shared by the HTTPS policy, omnibox and filter engine. */
object NetHosts {
    @JvmStatic
    fun isIpv4(h: String): Boolean {
        var parts = 0
        var value = -1
        for (k in 0..h.length) {
            val c = if (k < h.length) h[k] else '.'
            if (c == '.') {
                if (value < 0 || value > 255) return false
                parts++
                value = -1
            } else if (c in '0'..'9') {
                value = (if (value < 0) 0 else value) * 10 + (c - '0')
                if (value > 255) return false
            } else {
                return false
            }
        }
        return parts == 4
    }

    @JvmStatic
    fun isIpLiteral(h: String): Boolean = h.indexOf(':') >= 0 || isIpv4(h)

    /**
     * True for hosts that are not on the public internet: loopback, RFC1918, link-local, CGNAT, `.local`-style
     * names and single-label (intranet) names. The HTTPS upgrade and its warnings never apply to these
     * (e.g. the phone-hotspot gateway page, a NAS, a dev server).
     */
    @JvmStatic
    fun isLocal(h: String): Boolean {
        if (h.isEmpty()) return true
        if (h.indexOf(':') >= 0) { // IPv6 literal
            return h == "::1" || h == "::" || h.startsWith("fe80:") || h.startsWith("fc") || h.startsWith("fd")
        }
        if (isIpv4(h)) {
            val p = h.split('.')
            val a = p[0].toInt()
            val b = p[1].toInt()
            return a == 10 || a == 127 || a == 0 ||
                (a == 169 && b == 254) ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) ||
                (a == 100 && b in 64..127)
        }
        if (h.indexOf('.') < 0) return true
        return h == "localhost" || h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".lan") ||
            h.endsWith(".home") || h.endsWith(".internal") || h.endsWith(".localdomain") || h.endsWith(".home.arpa") ||
            h.endsWith(".intranet") || h.endsWith(".corp") || h.endsWith(".private")
    }
}
