package app.kestrel.core.url

import java.util.Base64

/**
 * "Debouncing": when a link goes through a well-known tracking redirector, jump straight to the real target so the
 * redirector never sees the click (Brave does the same). Table-driven; only http(s) targets are accepted.
 */
object RedirectUnwrapper {
    private class Rule(val hosts: (String) -> Boolean, val path: String, val params: Array<String>, val bingStyle: Boolean = false)

    private fun googleHost(h: String): Boolean =
        h == "google.com" || h == "www.google.com" ||
            (h.startsWith("www.google.") && h.length <= 18) || (h.startsWith("google.") && h.length <= 12)

    private fun eq(vararg names: String): (String) -> Boolean = { h -> names.contains(h) }

    private val RULES = arrayOf(
        Rule(::googleHost, "/url", arrayOf("q", "url")),
        Rule(eq("l.facebook.com", "lm.facebook.com", "l.messenger.com"), "/l.php", arrayOf("u")),
        Rule(eq("l.instagram.com"), "/", arrayOf("u")),
        Rule(eq("www.youtube.com", "youtube.com", "m.youtube.com"), "/redirect", arrayOf("q")),
        Rule(eq("out.reddit.com"), "", arrayOf("url")), // path is /<token>; matched by prefix below
        Rule(eq("duckduckgo.com", "html.duckduckgo.com", "lite.duckduckgo.com"), "/l/", arrayOf("uddg")),
        Rule(eq("steamcommunity.com"), "/linkfilter/", arrayOf("u", "url")),
        Rule(eq("www.linkedin.com", "linkedin.com"), "/safety/go", arrayOf("url")),
        Rule(eq("slack-redir.net"), "/link", arrayOf("url")),
        Rule(eq("vk.com", "m.vk.com"), "/away.php", arrayOf("to")),
        Rule(eq("disq.us"), "/url", arrayOf("url")),
        Rule(eq("t.umblr.com"), "/redirect", arrayOf("z")),
        Rule(eq("www.bing.com", "bing.com"), "/ck/a", arrayOf("u"), bingStyle = true),
    )

    /** @return the unwrapped target URL, or null if [url] is not a known redirector link. Applied at most twice by callers. */
    @JvmStatic
    fun unwrap(url: String): String? {
        val p = UrlParts.parse(url) ?: return null
        if (!p.isHttp) return null
        val query = p.query ?: return null
        for (r in RULES) {
            if (!r.hosts(p.host)) continue
            val pathOk = if (r.path.isEmpty()) true else p.path == r.path || (r.path.endsWith("/") && p.path.startsWith(r.path))
            if (!pathOk) continue
            for (name in r.params) {
                val raw = queryValue(query, name) ?: continue
                var target = percentDecode(raw) ?: continue
                if (r.bingStyle) target = decodeBing(target) ?: continue
                val tp = UrlParts.parse(target) ?: continue
                if (!tp.isHttp || tp.host == p.host) continue
                if (target.any { it.code < 0x20 }) continue
                return target
            }
        }
        return null
    }

    private fun queryValue(query: String, name: String): String? {
        var pos = 0
        while (pos <= query.length) {
            var amp = query.indexOf('&', pos)
            if (amp < 0) amp = query.length
            val eq = query.indexOf('=', pos)
            if (eq in pos until amp && eq - pos == name.length && query.regionMatches(pos, name, 0, name.length)) {
                return query.substring(eq + 1, amp)
            }
            pos = amp + 1
        }
        return null
    }

    /** Percent-decoding (UTF-8). Deliberately leaves '+' alone: it is a literal plus inside URLs. */
    @JvmStatic
    fun percentDecode(s: String): String? {
        if (s.indexOf('%') < 0) return s
        val out = java.io.ByteArrayOutputStream(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%') {
                if (i + 2 >= s.length) return null // truncated escape
                val hi = Character.digit(s[i + 1], 16)
                val lo = Character.digit(s[i + 2], 16)
                if (hi < 0 || lo < 0) return null
                out.write(hi * 16 + lo)
                i += 3
            } else {
                val b = c.toString().toByteArray(Charsets.UTF_8)
                out.write(b, 0, b.size)
                i++
            }
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    /** Bing wraps targets as `u=a1<base64url(url)>`. */
    private fun decodeBing(v: String): String? {
        if (!v.startsWith("a1") || v.length < 4) return null
        return try {
            String(Base64.getUrlDecoder().decode(v.substring(2)), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
