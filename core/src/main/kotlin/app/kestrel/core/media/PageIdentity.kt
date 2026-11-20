package app.kestrel.core.media

import app.kestrel.core.url.UrlParts

/**
 * VS2: which PAGE an address is, for deciding whether a load is the same page again (a reload): the host (without "www."), the path without a trailing slash (lower case) and the QUERY as written (a
 * "?season=1&episode=2" is another film), never the fragment ("#player"). Empty for anything that is not http(s): no page, nothing is kept for it.
 */
object PageIdentity {
    fun of(url: String): String {
        val p = UrlParts.parse(url) ?: return ""
        if (!p.scheme.equals("http", true) && !p.scheme.equals("https", true)) return ""
        val host = p.host.lowercase().removePrefix("www.")
        if (host.isEmpty()) return ""
        val path = p.path.substringBefore('?').substringBefore('#').trimEnd('/').lowercase()
        val q = p.query?.takeIf { it.isNotEmpty() }
        return host + path + (if (q != null) "?$q" else "")
    }
    fun same(a: String, b: String): Boolean { val x = of(a); return x.isNotEmpty() && x == of(b) }
}
