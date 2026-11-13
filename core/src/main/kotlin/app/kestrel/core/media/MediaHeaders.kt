package app.kestrel.core.media

import app.kestrel.core.url.UrlParts

/**
 * The request headers a stream or file is fetched with. Video servers (the CDN behind an embedded player) often answer 403 unless the request says it comes from THEIR player: the Referer / Origin of
 * the embedded player's own page, not of the web page the player is embedded in. The browser's own request tells which: [MediaCandidate.referer] / [MediaCandidate.origin] (seen by the extension).
 * When they were not seen, the document that asked for the file ([MediaCandidate.frame]) names the player; the web page's own address is the last guess.
 */
object MediaHeaders {
    /** Referer and Origin from an address: its origin only ("https://player.example/"), never a path or query (they can carry tokens). Empty map for an address that is not http(s). */
    fun fromAddress(address: String): Map<String, String> {
        val p = UrlParts.parse(address) ?: return emptyMap()
        if (!p.scheme.equals("http", true) && !p.scheme.equals("https", true)) return emptyMap()
        val origin = "${p.scheme}://${p.hostForAuthority}" + (if (p.port >= 0) ":${p.port}" else "")
        return linkedMapOf("Referer" to "$origin/", "Origin" to origin)
    }

    /** The Referer the browser sent, reduced to its origin ("https://player.test/embed/7?hash=x" -> "https://player.test/"): a path or query can carry a token and the replay goes to other hosts too. "" if not http(s). */
    fun refererOf(raw: String): String = fromAddress(raw)["Referer"] ?: ""

    /** The Origin the browser sent ("https://player.test"); "" for "null" (a sandboxed frame), a blank or anything that is not an http(s) origin. */
    fun originOf(raw: String): String = fromAddress(raw)["Origin"] ?: ""

    /**
     * The sets to try, best first, without repeats: (1) what the browser sent, (2) the player's page, (3) the web page, (4) the Referer alone (a server that dislikes an Origin it did not expect),
     * (5) nothing. A set that WORKED is remembered in [MediaCandidate.viaHeaders] and comes first from then on.
     */
    fun sets(c: MediaCandidate, topPage: String): List<Map<String, String>> {
        val out = ArrayList<Map<String, String>>()
        fun add(m: Map<String, String>) { if (out.none { it == m }) out.add(m) }
        c.viaHeaders?.let { add(it) }
        if (c.referer.isNotEmpty() || c.origin.isNotEmpty()) {
            val m = LinkedHashMap<String, String>()
            if (c.referer.isNotEmpty()) m["Referer"] = c.referer
            if (c.origin.isNotEmpty()) m["Origin"] = c.origin
            add(m)
        }
        if (c.frame.isNotEmpty()) add(fromAddress(c.frame))
        add(fromAddress(c.page.ifEmpty { topPage }))
        if (topPage.isNotEmpty() && topPage != c.page) add(fromAddress(topPage))
        for (m in ArrayList(out)) m["Referer"]?.let { r -> add(mapOf("Referer" to r)) }
        add(emptyMap())
        return out
    }
}
