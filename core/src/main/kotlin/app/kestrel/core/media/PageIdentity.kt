package app.kestrel.core.media

import app.kestrel.core.url.UrlParts

/**
 * VS2: which PAGE an address is, for deciding whether the tab is still on the same page: the host (without "www."), the path without a trailing slash, lower case; never the query or the fragment (a server
 * switch, a "?season=1&episode=2" or a "#player" is the same page). Empty for anything that is not http(s): no page, nothing is kept for it.
 */
object PageIdentity {
    fun of(url: String): String {
        val p = UrlParts.parse(url) ?: return ""
        if (!p.scheme.equals("http", true) && !p.scheme.equals("https", true)) return ""
        val host = p.host.lowercase().removePrefix("www.")
        if (host.isEmpty()) return ""
        val path = p.path.substringBefore('?').substringBefore('#').trimEnd('/').lowercase()
        return host + path
    }
    fun same(a: String, b: String): Boolean { val x = of(a); return x.isNotEmpty() && x == of(b) }
}
