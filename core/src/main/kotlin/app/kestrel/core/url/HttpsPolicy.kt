package app.kestrel.core.url

/**
 * HTTPS-first policy. Why it matters here: the tablet's only uplink is a phone hotspot, i.e. a carrier network sits in
 * the path, and carriers are known to inject headers / recompress / tamper with cleartext HTTP.
 */
object HttpsPolicy {
    enum class Mode {
        /** Never rewrite. */
        OFF,

        /** Upgrade http:// navigations to https://; if that fails, silently load http:// (Brave "Standard"). */
        UPGRADE,

        /** Upgrade; if https fails, show a warning screen before ever loading http:// (Brave "Strict"). Default. */
        STRICT,
    }

    /**
     * The https:// equivalent of [url], or null when no upgrade should be attempted: not http, local/private/IP host,
     * or an explicit non-default port (dev servers, routers). `:80` is dropped.
     */
    @JvmStatic
    fun upgradeOf(url: String): String? {
        if (!url.regionMatches(0, "http://", 0, 7, ignoreCase = true)) return null
        val p = UrlParts.parse(url) ?: return null
        if (p.scheme != "http") return null
        if (p.isIpLiteral || NetHosts.isLocal(p.host)) return null
        if (p.port != -1 && p.port != 80) return null
        val upgraded = UrlParts.parse("https" + url.substring(4)) ?: return null
        return if (upgraded.port == 80) {
            // "http://host:80/x" -> "https://host/x" (a literal :80 would be wrong over TLS)
            "https://" + (upgraded.userInfo?.let { "$it@" } ?: "") + upgraded.hostForAuthority +
                upgraded.path + (upgraded.query?.let { "?$it" } ?: "") + (upgraded.fragment?.let { "#$it" } ?: "")
        } else {
            "https" + url.substring(4)
        }
    }

    /** The http:// twin of an https URL we upgraded (used for the explicit "continue to HTTP" choice). */
    @JvmStatic
    fun downgradeOf(url: String): String? =
        if (url.regionMatches(0, "https://", 0, 8, ignoreCase = true)) "http" + url.substring(5) else null
}
