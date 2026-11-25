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
            // RVW1 S9: for the web schemes a backslash ends the authority exactly like a slash (the engine reads "https://evil.com\@good.com/" as the host evil.com; this parser read userinfo "evil.com\" and the host good.com,
            // so the address bar, Shields and the HTTPS policy judged a different site than the one being loaded)
            val special = scheme == "http" || scheme == "https" || scheme == "ws" || scheme == "wss" || scheme == "ftp" || scheme == "file"
            while (j < n) {
                val c = url[j]
                if (c == '/' || c == '?' || c == '#' || (special && c == '\\')) break
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
    /**
     * RVW1 S3: may the APP itself (not the page) send a request to [targetHost] on behalf of a page on [pageHost]? The finder reads the first bytes of addresses a page only NAMES (a `<video src>`, an og tag, a script
     * response): with the profile's cookies, with no click. A page on the public internet must not be able to aim that at a device on the owner's own network (the router at 192.168.1.1, a NAS, a camera): refused.
     * A page that is itself on a local host (the hotspot gateway, a NAS, a dev server) may name local files. No page host known = public.
     */
    @JvmStatic
    fun probeAllowed(targetHost: String, pageHost: String): Boolean =
        !reachesLocalNetwork(targetHost) || (pageHost.isNotEmpty() && reachesLocalNetwork(pageHost))

    /**
     * RVW3 SC2: can a request to [host] end up on a device of the owner's own network, however the host is spelled? [isLocal] stays what the address bar and the HTTPS policy need (strict dotted decimal and the plain
     * IPv6 prefixes: "10.5" must still be a search). This one is for the places where the APP sends a request a page only NAMED (the finder's probes, a hand-over to Kestrel's own player), where the engine's local
     * network blocking does not apply: it also reads what the system's resolver reads as an address ("127.1", "0x7f.0.0.1", "0177.0.0.1", "2130706433", "192.168.1") and every IPv6 spelling of a v4 address
     * (IPv4-mapped "::ffff:192.168.1.1" and "::ffff:c0a8:101", IPv4-compatible, NAT64 "64:ff9b::c0a8:101", 6to4), site-local, link-local, unique-local and multicast. A name is judged by its spelling only (it may still
     * resolve to a private address: the guard of the fetcher checks that at connect time). Anything that looks like an IPv6 literal but does not parse counts as local (closed, never open).
     */
    @JvmStatic
    fun reachesLocalNetwork(host: String): Boolean {
        var h = host.trim().lowercase()
        if (h.length > 1 && h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length - 1)
        val zone = h.indexOf('%')
        if (zone >= 0) h = h.substring(0, zone)
        while (h.endsWith(".")) h = h.substring(0, h.length - 1)
        if (isLocal(h)) return true
        if (h.indexOf(':') >= 0) {
            val g = ipv6Groups(h) ?: return true
            return ipv6Local(g)
        }
        val v4 = inetAton(h) ?: return false           // not a number: a name
        return ipv4Local(v4)
    }

    private fun ipv4Local(v: Long): Boolean {
        val a = ((v shr 24) and 255L).toInt()
        val b = ((v shr 16) and 255L).toInt()
        return a == 10 || a == 127 || a == 0 || a >= 224 ||            // (224 and up: multicast, reserved, broadcast: never a film server)
            (a == 169 && b == 254) || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 100 && b in 64..127)
    }

    private fun digitOf(c: Char): Int = when (c) { in '0'..'9' -> c - '0'; in 'a'..'f' -> c - 'a' + 10; else -> -1 }

    /** One part of an address the way the C library's inet_aton reads it: decimal, 0x hex, or 0 octal. ASCII only. */
    private fun inetNumber(p: String): Long? {
        val radix: Int
        val digits: String
        when {
            p.length > 2 && p.startsWith("0x") -> { radix = 16; digits = p.substring(2) }
            p.length > 1 && p.startsWith("0") -> { radix = 8; digits = p.substring(1) }
            else -> { radix = 10; digits = p }
        }
        if (digits.isEmpty() || digits.length > 11) return null
        var n = 0L
        for (c in digits) {
            val d = digitOf(c)
            if (d < 0 || d >= radix) return null
            n = n * radix + d
        }
        return n
    }

    /** 1 to 4 parts, every part but the last one byte, the last fills the rest (inet_aton). null = this is not a numeric address. */
    private fun inetAton(h: String): Long? {
        if (h.isEmpty() || h.length > 45) return null
        val parts = h.split('.')
        if (parts.size > 4) return null
        val nums = LongArray(parts.size)
        for (i in parts.indices) nums[i] = inetNumber(parts[i]) ?: return null
        var v = 0L
        for (i in 0 until nums.size - 1) {
            if (nums[i] > 255L) return null
            v = v or (nums[i] shl (24 - 8 * i))
        }
        val last = nums[nums.size - 1]
        if (last >= (1L shl (8 * (4 - (nums.size - 1))))) return null
        return v or last
    }

    /** The eight 16-bit groups of an IPv6 literal (with "::" and an embedded dotted v4 tail), or null when it does not parse. */
    private fun ipv6Groups(literal: String): IntArray? {
        var s = literal
        val lastColon = s.lastIndexOf(':')
        if (s.indexOf('.', lastColon + 1) >= 0) {                       // "::ffff:192.168.1.1": the tail is four decimal bytes = two groups
            val o = s.substring(lastColon + 1).split('.')
            if (o.size != 4) return null
            var hi = 0; var lo = 0
            for (i in 0 until 4) {
                val t = o[i]
                if (t.isEmpty() || t.length > 3) return null
                var n = 0
                for (c in t) { if (c !in '0'..'9') return null; n = n * 10 + (c - '0') }
                if (n > 255) return null
                if (i < 2) hi = hi * 256 + n else lo = lo * 256 + n
            }
            s = s.substring(0, lastColon + 1) + Integer.toHexString(hi) + ":" + Integer.toHexString(lo)
        }
        val dbl = s.indexOf("::")
        if (dbl >= 0 && s.indexOf("::", dbl + 1) >= 0) return null
        fun split(t: String): List<String>? { if (t.isEmpty()) return emptyList(); val l = t.split(':'); return if (l.any { it.isEmpty() }) null else l }
        val head = split(if (dbl >= 0) s.substring(0, dbl) else s) ?: return null
        val tail = if (dbl >= 0) (split(s.substring(dbl + 2)) ?: return null) else emptyList()
        if (dbl < 0 && head.size != 8) return null
        if (dbl >= 0 && head.size + tail.size > 7) return null
        val out = IntArray(8)
        fun put(from: Int, l: List<String>): Boolean {
            for (i in l.indices) {
                val t = l[i]
                if (t.isEmpty() || t.length > 4) return false
                var n = 0
                for (c in t) { val d = digitOf(c); if (d < 0) return false; n = n * 16 + d }
                out[from + i] = n
            }
            return true
        }
        if (!put(0, head)) return null
        if (!put(8 - tail.size, tail)) return null
        return out
    }

    private fun ipv6Local(g: IntArray): Boolean {
        fun v4(hi: Int, lo: Int): Boolean = ipv4Local(((hi.toLong() and 0xffffL) shl 16) or (lo.toLong() and 0xffffL))
        val zeros5 = g[0] == 0 && g[1] == 0 && g[2] == 0 && g[3] == 0 && g[4] == 0
        return when {
            zeros5 && g[5] == 0 && g[6] == 0 && (g[7] == 0 || g[7] == 1) -> true                    // :: and ::1
            zeros5 && g[5] == 0xffff -> v4(g[6], g[7])                                              // IPv4-mapped
            zeros5 && g[5] == 0 -> v4(g[6], g[7])                                                   // IPv4-compatible (::127.0.0.1)
            g[0] == 0x64 && g[1] == 0xff9b -> if (g[2] == 0 && g[3] == 0 && g[4] == 0 && g[5] == 0) v4(g[6], g[7]) else true     // NAT64 (64:ff9b:1::/48 is local-use)
            g[0] == 0x2002 -> v4(g[1], g[2])                                                        // 6to4
            (g[0] and 0xffc0) == 0xfe80 || (g[0] and 0xffc0) == 0xfec0 -> true                      // link-local, site-local
            (g[0] and 0xfe00) == 0xfc00 -> true                                                     // unique-local
            (g[0] and 0xff00) == 0xff00 -> true                                                     // multicast
            else -> false
        }
    }

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
