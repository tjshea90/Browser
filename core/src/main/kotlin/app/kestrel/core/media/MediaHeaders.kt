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

    /**
     * VS1e (owner 2026-11-18, doraby + vidfast: the finder's own request for a text/plain answer got 404 on its FIRST header set and gave up, the owner's other downloader finds it): a server that dislikes the
     * headers it got often answers 404 (not 403) so that nobody can tell the address is real. 401 / 403 / no answer try the next set as before; 404 and 406 try at most [MAX_NOT_FOUND_TRIES] more sets (a dead address
     * is the common 404, so the cost stays at two more tiny requests); everything else (200s, 5xx, 416 ...) is final.
     */
    const val MAX_NOT_FOUND_TRIES = 2
    fun tryNextSet(status: Int, notFoundTries: Int): Boolean = when (status) {
        401, 403, -1 -> true
        404, 406 -> notFoundTries < MAX_NOT_FOUND_TRIES
        else -> false
    }

    /** A request that a page made with another method than GET (an API call that returns a text/plain answer) cannot be asked again as a GET: it is not a stream. */
    fun replayable(method: String): Boolean = method.isEmpty() || method.equals("GET", true)
}
