package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Random

/**
 * Metamorphic / differential test: random rule sets are rendered to filter-list TEXT, compiled by the real pipeline
 * (parser -> builder -> token index -> matcher) and compared against an independent, deliberately naive oracle that
 * evaluates the same rules as java.util.regex expressions. Any disagreement means a bug in parsing, token selection,
 * anchoring, exception precedence, party/type/domain handling or the host fast path.
 */
class FilterSetMetamorphicTest {
    private val psl = Psl.FALLBACK
    private val hosts = listOf("ads.example.com", "example.com", "cdn.example.org", "track.net", "a.b.track.net", "news.site.co.uk", "img.site.co.uk", "page.example")
    private val pageHosts = listOf("example.com", "www.example.com", "track.net", "news.site.co.uk", "other.co.uk", "page.example", "a.b.c.example.org")
    private val words = listOf("ads", "banner", "track", "img", "x", "pixel", "v1", "api", "ad", "analytics")

    /** URL-only words that CONTAIN rule words as prefix/suffix/infix: they punish any index that wrongly assumes rule text is a whole URL token. */
    private val urlWords = words + listOf("uploads", "headads", "tracker", "retrack", "xbanner", "bannerx", "pixels", "xpixel", "myapi", "apis", "analytics2", "grad", "adsx", "bigimg", "imgs")
    private val types = listOf(RequestType.SCRIPT, RequestType.IMAGE, RequestType.XHR, RequestType.STYLESHEET, RequestType.SUBDOCUMENT, RequestType.UNKNOWN, RequestType.DOCUMENT)

    private class R(
        val text: String, val pattern: String, val leftHost: Boolean, val leftAnchor: Boolean, val rightAnchor: Boolean,
        val exception: Boolean, val important: Boolean, val party: Int, val typeMask: Int,
        val include: List<String>, val exclude: List<String>,
    ) {
        val regex: Regex = run {
            val sb = StringBuilder()
            sb.append(if (leftHost) "^[a-z][a-z0-9+.-]*://(?:[^/?#@]*@)?(?:[^/?#:]*\\.)?" else if (leftAnchor) "^" else "")
            for (c in pattern) when (c) {
                '*' -> sb.append(".*")
                '^' -> sb.append("(?:[^a-z0-9_.%-]|$)")
                else -> sb.append(Regex.escape(c.toString()))
            }
            if (rightAnchor) sb.append("$")
            Regex(sb.toString())
        }
    }

    private fun genRule(rnd: Random): R {
        fun pick(l: List<String>) = l[rnd.nextInt(l.size)]
        val kind = rnd.nextInt(8)
        var pattern: String
        var leftHost = false; var leftAnchor = false; var rightAnchor = false
        when (kind) {
            0 -> { leftHost = true; pattern = pick(hosts).let { h -> h.substring(h.indexOf('.').let { if (rnd.nextBoolean() && it > 0) it + 1 else 0 }) } + "^" }
            1 -> { leftHost = true; pattern = pick(hosts) + "/" + pick(words) }
            2 -> pattern = "/" + pick(words) + "/" + pick(words)
            3 -> pattern = pick(words) + "/"
            4 -> { leftAnchor = true; pattern = (if (rnd.nextBoolean()) "https://" else "http://") + pick(hosts).substringBefore('.') }
            5 -> { pattern = "/" + pick(words) + "/*/" + pick(words) }
            6 -> { pattern = "/" + pick(words) + "^" }
            else -> { pattern = pick(words) + "." + pick(listOf("gif", "js", "png")); rightAnchor = rnd.nextBoolean() }
        }
        val exception = rnd.nextInt(5) == 0
        val important = !exception && rnd.nextInt(8) == 0
        val party = if (rnd.nextInt(4) == 0) 1 + rnd.nextInt(2) else 0
        var typeMask = RequestType.ALL_SUBRESOURCES
        val opts = ArrayList<String>()
        if (rnd.nextInt(3) == 0) {
            val t = pick(listOf("script", "image", "xhr", "stylesheet", "subdocument"))
            opts.add(t); typeMask = RequestType.fromName(t)
        } else if (rnd.nextInt(6) == 0) {
            opts.add("~image"); typeMask = RequestType.ALL_SUBRESOURCES and RequestType.IMAGE.inv()
        } else if (rnd.nextInt(10) == 0) {
            opts.add("document"); typeMask = RequestType.DOCUMENT
        }
        if (party == 1) opts.add("third-party"); if (party == 2) opts.add("~third-party")
        if (important) opts.add("important")
        val include = ArrayList<String>(); val exclude = ArrayList<String>()
        if (rnd.nextInt(5) == 0) {
            val d = ArrayList<String>()
            if (rnd.nextBoolean()) { val h = pick(pageHosts); include.add(h); d.add(h) }
            if (rnd.nextBoolean()) { val h = pick(pageHosts); exclude.add(h); d.add("~$h") }
            if (d.isNotEmpty()) opts.add("domain=" + d.joinToString("|"))
        }
        // `$document` on an exception means "page allow", a different feature: keep generated exceptions subresource-only
        val finalTypeMask = if (exception && typeMask == RequestType.DOCUMENT) RequestType.ALL_SUBRESOURCES else typeMask
        if (exception && typeMask == RequestType.DOCUMENT) opts.remove("document")
        val text = (if (exception) "@@" else "") + (if (leftHost) "||" else if (leftAnchor) "|" else "") + pattern +
            (if (rightAnchor) "|" else "") + (if (opts.isNotEmpty()) "$" + opts.joinToString(",") else "")
        return R(text, pattern, leftHost, leftAnchor, rightAnchor, exception, important, party, finalTypeMask, include, exclude)
    }

    private fun genUrl(rnd: Random): String {
        fun pick(l: List<String>) = l[rnd.nextInt(l.size)]
        val sb = StringBuilder(if (rnd.nextInt(4) == 0) "http://" else "https://")
        if (rnd.nextInt(15) == 0) sb.append("user@")
        sb.append(pick(hosts))
        if (rnd.nextInt(8) == 0) sb.append(":8080")
        val segs = rnd.nextInt(4)
        for (i in 0 until segs) sb.append('/').append(pick(urlWords))
        if (rnd.nextBoolean()) {
            if (segs == 0) sb.append('/').append(pick(urlWords)) // an extension needs a path; "host:8080.png" is not a valid URL
            sb.append('.').append(pick(listOf("gif", "js", "png", "html")))
        }
        if (rnd.nextInt(3) == 0) sb.append("?q=").append(pick(urlWords)).append('&').append(pick(urlWords)).append('=').append(pick(hosts))
        return sb.toString()
    }

    private fun suffixes(host: String): List<String> {
        val out = ArrayList<String>(); var h = host
        while (true) { out.add(h); val d = h.indexOf('.'); if (d < 0) break; h = h.substring(d + 1) }
        return out
    }

    private fun oracle(rules: List<R>, url: String, page: String?, typeMask: Int): Int {
        val u = url.lowercase()
        val host = Regex("^[a-z][a-z0-9+.-]*://(?:[^/?#@]*@)?([^/?#:]*)").find(u)!!.groupValues[1]
        val third = typeMask != RequestType.DOCUMENT && page != null && !psl.isSameSite(host, page)
        var blocked = false; var important = false; var excepted = false
        for (r in rules) {
            if ((r.typeMask and typeMask) == 0) continue
            if (r.party == 1 && !third) continue
            if (r.party == 2 && third) continue
            if (r.include.isNotEmpty() || r.exclude.isNotEmpty()) {
                val ps = if (page == null) emptyList() else suffixes(page)
                if (r.include.isNotEmpty() && r.include.none { it in ps }) continue
                if (r.exclude.any { it in ps }) continue
            }
            if (!r.regex.containsMatchIn(u)) continue
            if (r.exception) excepted = true else { blocked = true; if (r.important) important = true }
        }
        return when {
            !blocked -> 0
            important -> 1
            excepted -> 2
            else -> 1
        }
    }

    private fun engine(v: Int): Int = when {
        Verdict.isBlocked(v) -> 1
        v == Verdict.EXCEPTED -> 2
        else -> 0
    }

    @Test
    fun optimizedMatcherAgreesWithNaiveOracle() {
        var comparisons = 0
        var blockedSeen = 0
        for (seed in 1..300) {
            val rnd = Random(seed.toLong())
            val rules = List(1 + rnd.nextInt(40)) { genRule(rnd) }
            val b = FilterSetBuilder()
            b.addAbpList("t", Category.ADS, rules.map { it.text }.asSequence())
            val fs = b.build(1L)
            repeat(200) {
                val url = genUrl(rnd)
                val page = if (rnd.nextInt(10) == 0) null else pageHosts[rnd.nextInt(pageHosts.size)]
                val type = types[rnd.nextInt(types.size)]
                val expected = oracle(rules, url, page, type)
                val got = engine(fs.check(url, page, type, psl))
                if (expected != got) {
                    val sb = StringBuilder("MISMATCH seed=$seed expected=$expected got=$got\nurl=$url page=$page type=$type\nrules:\n")
                    for (r in rules) sb.append("  ").append(r.text).append("   regex=").append(r.regex.pattern).append('\n')
                    throw AssertionError(sb.toString())
                }
                comparisons++
                if (expected == 1) blockedSeen++
                // case-insensitivity: the engine must give the same answer for an upper-cased URL
                assertEquals("upper-case url differs: $url", got, engine(fs.check(url.uppercase(), page, type, psl)))
            }
        }
        // sanity: the generator really exercises both outcomes
        assert(blockedSeen > 500) { "generator too weak: only $blockedSeen blocked of $comparisons" }
        assert(blockedSeen < comparisons - 500) { "generator too aggressive: $blockedSeen of $comparisons blocked" }
    }
}
