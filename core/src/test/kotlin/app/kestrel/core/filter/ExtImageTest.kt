package app.kestrel.core.filter

import app.kestrel.core.url.UrlParts
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

/**
 * The fixtures the JavaScript engine (geckoapp/src/main/assets/kestrel-shields/engine.js) is tested against: web-tests/fixtures/mini.*
 *
 * The Kotlin engine is the oracle. This test (1) builds a small but complete filter set (every rule shape the builder knows: host rules, exceptions, token rules, 4-gram rules,
 * token-less rules, `$domain`, party, types, `$important`) and encodes it as a KEX1 image, (2) records the Kotlin verdict for a few thousand requests, hash values, Public Suffix List answers and host
 * extractions, and (3) fails if the committed files differ from what the code produces NOW - so a change to the matcher, the builder or the image layout cannot silently leave the JS engine behind.
 * Regenerate after an intended change: `KESTREL_REGEN=1 ./gradlew :core:test --tests '*ExtImageTest*'`, then run `cd web-tests && npm test`.
 */
class ExtImageTest {
    private val psl = Psl.parse(
        (
            "co.uk org.uk ac.uk com.au co.jp github.io blogspot.com appspot.com herokuapp.com co.za\n" +
                "ck *.ck !www.ck jp *.kobe.jp !city.kobe.jp *.sch.uk\n"
            ).split(' ', '\n').asSequence().filter { it.isNotBlank() },
    )

    private val curated = listOf(
        "||ads.example.com^", "||tracker.example^\$third-party", "||first.example^\$~third-party", "||pixel.example^\$image", "||both.example^\$script,xhr",
        "||imp.example^\$important", "@@||ads.example.com/ok/", "@@||tracker.example/safe.js", "@@||hostonly.example^", "||hostonly.example^",
        "/banner^", "/ads/*/img", "|https://t.", "|http://plain.", ".gif|", "||a.example/p*q", "/promo.js\$domain=a.com|~b.com", "||evil.example^\$important,script",
        "||zip^", "||sub.deep.example.co.uk^", "||x.example^\$domain=news.site.co.uk", "/track.php?", "&adslot=", "foo.js|", "adserver.", "q*z*y", "/x*y*z",
        "||cdn.example.org/ads/", "/pixel.gif?", "|https://*.beacon.", "/stat^\$ping", "||ws.example^\$websocket", "||frame.example^\$subdocument",
        "||font.example^\$font", "||media.example^\$media", "||obj.example^\$object", "||other.example^\$other", "||style.example^\$stylesheet",
        "||notimg.example^\$~image", "||dom.example^\$domain=example.com", "||excl.example^\$domain=~example.com", "/analytics.js", "/collect?v=",
        "/impban/*\$important", "||impath.example/ads/\$important", "@@||impath.example^", "|https://impanchor.\$important,script", "/AdBanner.", "||UPPER.example^", "||ipv4.example^", "||1.2.3.4^", "/\$ext/",
    )

    private val hosts = listOf(
        "ads.example.com", "example.com", "cdn.example.org", "track.net", "a.b.track.net", "news.site.co.uk", "img.site.co.uk", "page.example", "tracker.example",
        "first.example", "pixel.example", "both.example", "imp.example", "hostonly.example", "plain.example", "t.example", "a.example", "evil.example", "x.zip",
        "sub.deep.example.co.uk", "x.example", "ws.example", "frame.example", "font.example", "media.example", "obj.example", "other.example", "style.example",
        "notimg.example", "dom.example", "excl.example", "foo.beacon.example", "UPPER.example", "1.2.3.4", "[::1]", "xn--bcher-kva.example", "www.ck", "a.b.ck", "city.kobe.jp", "x.kobe.jp",
        "user.github.io", "a.b.blogspot.com", "localhost", "intranet", "cookie-banner.example", "impath.example", "impanchor.example", "malware.example.net", "phish.example.org", "bad.example", "www.malware.example.net",
    )
    private val pageHosts = listOf(
        "example.com", "www.example.com", "a.com", "b.com", "a.b.c.example.org", "news.site.co.uk", "other.co.uk", "page.example", "tracker.example", "track.net", "www.ck", "foo.ck",
        "city.kobe.jp", "x.y.kobe.jp", "user.github.io", "other.github.io", "1.2.3.4", "localhost",
    )
    private val words = listOf("ads", "banner", "track", "img", "x", "pixel", "v1", "api", "ad", "analytics", "promo", "stat", "collect", "p", "q", "z", "foo", "adserver", "beacon", "AdBanner", "y", "consent.js", "consent", "impban")
    private val rareWords = listOf("beacon", "adserver", "collect", "stat", "promo", "pixel", "AdBanner").map { it.lowercase() }
    private val neutralHosts = listOf(
        "www.wikipedia.org", "cdn.jsdelivr.net", "fonts.gstatic.com", "news.bbc.co.uk", "docs.example.net", "static.cdn-example.com", "shop.example.co.uk", "img.example.com.au", "api.service.dev",
        "files.example.io", "video.example.tv", "www.example.com",
    )
    private val types = listOf(
        RequestType.SCRIPT, RequestType.IMAGE, RequestType.XHR, RequestType.STYLESHEET, RequestType.SUBDOCUMENT, RequestType.UNKNOWN, RequestType.DOCUMENT, RequestType.WEBSOCKET,
        RequestType.OTHER, RequestType.MEDIA, RequestType.FONT, RequestType.PING, RequestType.OBJECT,
    )

    private fun generatedLines(rnd: Random): List<String> {
        fun pick(l: List<String>) = l[rnd.nextInt(l.size)]
        val out = ArrayList<String>()
        repeat(260) {
            val kind = rnd.nextInt(9)
            val pattern: String
            var prefix = ""
            var suffix = ""
            when (kind) {
                0 -> { prefix = "||"; pattern = pick(hosts).lowercase().let { h -> h.substring(h.indexOf('.').let { i -> if (rnd.nextBoolean() && i > 0) i + 1 else 0 }) } + "^" }
                1 -> { prefix = "||"; pattern = pick(hosts).lowercase() + "/" + pick(words).lowercase() }
                2 -> pattern = "/" + pick(words).lowercase() + "/" + pick(words).lowercase()
                3 -> pattern = pick(words).lowercase() + "/"
                4 -> { prefix = "|"; pattern = (if (rnd.nextBoolean()) "https://" else "http://") + pick(hosts).lowercase().substringBefore('.') }
                5 -> pattern = "/" + pick(words).lowercase() + "/*/" + pick(words).lowercase()
                6 -> pattern = "/" + pick(words).lowercase() + "^"
                7 -> { pattern = pick(words).lowercase() + "." + pick(listOf("gif", "js", "png")); suffix = "|" }
                else -> pattern = pick(rareWords) + "*" + pick(rareWords) + "*" + pick(rareWords)
            }
            val exception = rnd.nextInt(5) == 0
            val opts = ArrayList<String>()
            if (rnd.nextInt(3) == 0) opts.add(pick(listOf("script", "image", "xhr", "stylesheet", "subdocument", "font", "media", "websocket", "ping", "other", "object")))
            else if (rnd.nextInt(6) == 0) opts.add("~" + pick(listOf("image", "script", "xhr")))
            val party = rnd.nextInt(4)
            if (party == 0) opts.add("third-party") else if (party == 1) opts.add("~third-party")
            if (!exception && kind <= 1 && rnd.nextInt(10) == 0) opts.add("important") // host-anchored only: a broad $important rule would end most scans early and hide the paths behind it
            if (rnd.nextInt(5) == 0) {
                val d = ArrayList<String>()
                if (rnd.nextBoolean()) d.add(pick(pageHosts))
                if (rnd.nextBoolean()) d.add("~" + pick(pageHosts))
                if (d.isNotEmpty()) opts.add("domain=" + d.joinToString("|"))
            }
            out.add((if (exception) "@@" else "") + prefix + pattern + suffix + (if (opts.isNotEmpty()) "$" + opts.joinToString(",") else ""))
        }
        return out
    }

    private fun build(): FilterSet {
        val rnd = Random(20261111L)
        val b = FilterSetBuilder()
        b.addAbpList("curated", Category.ADS, curated.asSequence())
        b.addAbpList("generated", Category.PRIVACY, generatedLines(rnd).asSequence())
        b.addAbpList("annoyance", Category.ANNOYANCE, sequenceOf("||cookie-banner.example^", "/consent.js", "@@||cookie-banner.example/ok^"))
        b.addHostsList("malware", Category.MALWARE, sequenceOf("0.0.0.0 malware.example.net", "0.0.0.0 phish.example.org", "127.0.0.1 bad.example"))
        return b.build(1_700_000_000_000L)
    }

    /** (url, how many random type/page combinations to record for it) */
    private fun urls(rnd: Random): List<Pair<String, Int>> {
        fun pick(l: List<String>) = l[rnd.nextInt(l.size)]
        val out = ArrayList<Pair<String, Int>>()
        // one positive and a few near-misses per curated rule
        for (line in curated) {
            val core = line.removePrefix("@@").removePrefix("||").removePrefix("|").substringBefore('$').removeSuffix("|").replace("^", "/").replace("*", "x")
            if (core.isEmpty()) continue
            val withHost = if (core.contains('.') && !core.startsWith("/") && !core.startsWith(".") && !core.startsWith("&")) core else "ads.example.com/$core"
            out.add("https://$withHost" to 3)
            out.add("http://sub.$withHost" to 2)
            out.add("https://x.net/path/$core?k=v" to 2)
            out.add("https://x.net/a$core" to 2)
            out.add("https://" + withHost.uppercase() to 2)
        }
        repeat(800) {
            val sb = StringBuilder(pick(listOf("https://", "https://", "http://", "ws://", "wss://")))
            if (rnd.nextInt(15) == 0) sb.append("user:pw@")
            sb.append(if (rnd.nextInt(5) < 2) pick(neutralHosts) else pick(hosts))
            if (rnd.nextInt(8) == 0) sb.append(":8080")
            if (rnd.nextInt(30) == 0) sb.append('.')
            val segs = rnd.nextInt(4)
            for (i in 0 until segs) sb.append('/').append(pick(words))
            if (rnd.nextBoolean()) {
                if (segs == 0) sb.append('/').append(pick(words))
                sb.append('.').append(pick(listOf("gif", "js", "png", "html", "php")))
            }
            if (rnd.nextInt(3) == 0) sb.append("?q=").append(pick(words)).append('&').append(pick(words)).append('=').append(pick(hosts))
            if (rnd.nextInt(12) == 0) sb.append('#').append(pick(words))
            out.add(sb.toString() to 2)
        }
        // shapes that are not ordinary: non-network schemes, empty/odd hosts, hostile lengths
        out.addAll(listOf(
            "data:text/plain,hello", "blob:https://example.com/1", "about:blank", "file:///etc/hosts", "javascript:void(0)", "ftp://ads.example.com/x", "https://", "https:///x", "https://:80/x",
            "https://[::1]:8080/ads/x", "https://[2001:db8::1]/banner/1", "https://ads.example.com./x.js", "https://ads.example.com..", "HTTPS://ADS.EXAMPLE.COM/X.JS", "https://ads.example.com:/x",
            "https://ads.example.com:99999/x", "https://ads.example.com@evil.example/x", "https://evil.example@ads.example.com/x", "https://a@b@ads.example.com/x",
        ).map { it to 4 })
        val junk = "j".repeat(3000)
        out.add("https://$junk@ads.example.com/x.js" to 1)
        out.add("https://ads.example.com/" + "a/".repeat(1100) + "banner/1" to 1)
        out.add("https://x.net/" + "e/".repeat(1100) + "ads/y/img" to 1)
        out.add("https://" + "h".repeat(400) + ".example/x" to 1)
        out.add("https://x.net/?" + "event/".repeat(330) + "pixel.gif" to 1)
        out.add("https://x.net/" + "q".repeat(2100) + "/banner/" to 1)
        return out
    }

    private fun corpus(fs: FilterSet): String {
        val rnd = Random(7771L)
        val sb = StringBuilder()
        for ((u, n) in urls(rnd)) {
            repeat(n) {
                val t = types[rnd.nextInt(types.size)]
                val page = if (rnd.nextInt(8) == 0) null else pageHosts[rnd.nextInt(pageHosts.size)]
                sb.append(u).append('\t').append(t).append('\t').append(page ?: "").append('\t').append(fs.check(u, page, t, psl)).append('\n')
            }
        }
        return sb.toString()
    }

    private fun hashVectors(): String {
        val sb = StringBuilder()
        val strings = ArrayList<String>()
        strings.addAll(listOf("", "a", "ab", "abc", "com", "example.com", "ads.example.com", "a.b.c.d.e.f.g", "xn--bcher-kva.example", "UPPER", "café", "日本", "İ", "￿", "0123456789", "/ads/banner", "https://x.net/a?b=c&d=e#f"))
        val rnd = Random(99L)
        repeat(300) {
            val len = 1 + rnd.nextInt(70)
            val s = StringBuilder()
            repeat(len) { s.append("abcdefghijklmnopqrstuvwxyz0123456789.-_/?=&%".let { cs -> cs[rnd.nextInt(cs.length)] }) }
            strings.add(s.toString())
        }
        for (s in strings) {
            val h = Hash.h64(s)
            sb.append(s).append('\t').append((h ushr 32).toInt()).append('\t').append(h.toInt()).append('\n')
        }
        return sb.toString()
    }

    private fun pslVectors(): String {
        val sb = StringBuilder()
        val list = ArrayList<String>(hosts.map { it.lowercase() })
        list.addAll(listOf(
            "co.uk", "x.co.uk", "a.b.c.co.uk", "uk", "com", "example.com", "www.example.com", "a.b.example.org", "foo.ck", "bar.foo.ck", "www.ck", "a.www.ck", "ck", "kobe.jp", "a.kobe.jp", "b.a.kobe.jp", "city.kobe.jp",
            "www.city.kobe.jp", "jp", "x.jp", "github.io", "a.github.io", "b.a.github.io", "sch.uk", "x.sch.uk", "y.x.sch.uk", "localhost", "intranet", "1.2.3.4", "256.1.1.1", "1.2.3", "[::1]", "::1", "a..b", "x.", ".x", "",
            "blogspot.com", "a.blogspot.com", "xn--bcher-kva.example", "co.za", "x.co.za",
        ))
        for (h in list) {
            sb.append(h).append('\t').append(psl.siteOf(h) ?: "\u0001null").append('\n')
        }
        // same-site pairs
        for (i in 0 until list.size step 3) {
            val a = list[i]; val b = list[(i * 7 + 3) % list.size]
            sb.append("same\t").append(a).append('\t').append(b).append('\t').append(if (psl.isSameSite(a, b)) 1 else 0).append('\n')
        }
        return sb.toString()
    }

    private fun hostVectors(): String {
        val sb = StringBuilder()
        val list = listOf(
            "https://example.com/x", "https://EXAMPLE.com./x", "http://user:pw@host.example:8080/p?q#f", "https://a@b@host.example/", "https://[::1]:80/x", "https://[2001:db8::1]/x", "https://host.example:/x",
            "https://host.example:99999/x", "https://host.example:abc/x", "https://host.example..", "https:///x", "https://", "ws://sock.example/s", "wss://sock.example:9000/s", "data:text/plain,hi", "about:blank", "example.com",
            "https://xn--bcher-kva.example/", "https://a.b.c.d.e/", "HTTPS://UP.EXAMPLE/", "https://host.example?x=1", "https://host.example#top", "https://[::1", "https://h:1:2/",
        )
        for (u in list) sb.append(u).append('\t').append(UrlParts.hostOf(u) ?: "\u0001null").append('\n')
        return sb.toString()
    }

    private fun fixtureDir(): File {
        var d: File? = File("").absoluteFile
        while (d != null) {
            val f = File(d, "web-tests/fixtures")
            if (File(d, "web-tests").isDirectory) return f
            d = d.parentFile
        }
        throw IllegalStateException("web-tests directory not found above " + File("").absolutePath)
    }

    private fun check(name: String, expected: ByteArray) {
        val f = File(fixtureDir(), name)
        if (System.getenv("KESTREL_REGEN") == "1") {
            f.parentFile.mkdirs()
            f.writeBytes(expected)
            return
        }
        if (!f.isFile) fail("fixture ${f.path} is missing: run KESTREL_REGEN=1 ./gradlew :core:test --tests '*ExtImageTest*'")
        val committed = f.readBytes()
        if (!committed.contentEquals(expected)) {
            fail("fixture ${f.path} differs from what the Kotlin engine produces now (matcher, builder or KEX1 layout changed?). " +
                "Regenerate: KESTREL_REGEN=1 ./gradlew :core:test --tests '*ExtImageTest*' then run cd web-tests && npm test")
        }
    }

    @Test
    fun theFixturesTheJavaScriptEngineIsTestedAgainstAreCurrent() {
        val fs = build()
        val image = ExtImage.encode(fs, psl, 1)
        check("mini.kex", image)
        check("mini.corpus.tsv", corpus(fs).toByteArray(Charsets.UTF_8))
        check("mini.hash.tsv", hashVectors().toByteArray(Charsets.UTF_8))
        check("mini.psl.tsv", pslVectors().toByteArray(Charsets.UTF_8))
        check("mini.host.tsv", hostVectors().toByteArray(Charsets.UTF_8))
    }

    @Test
    fun theCorpusIsAMixNotAWallOfOneVerdict() {
        val fs = build()
        val rows = corpus(fs).lineSequence().filter { it.isNotEmpty() }.map { it.substringAfterLast('\t').toInt() }.toList()
        fun share(f: (Int) -> Boolean) = rows.count(f) * 100.0 / rows.size
        assertTrue("passes: ${share { it == 0 }}", share { it == 0 } >= 20.0)
        assertTrue("blocks (not important): ${share { it and 1 != 0 && it and 16 == 0 }}", share { it and 1 != 0 && it and 16 == 0 } >= 15.0)
        assertTrue("exceptions: ${share { it == 2 }}", share { it == 2 } >= 3.0)
        assertTrue("important: ${share { it and 16 != 0 }}", share { it and 16 != 0 } in 1.0..20.0)
        assertTrue("several categories", rows.filter { it and 1 != 0 }.map { (it shr 2) and 3 }.toSet().size >= 3)
    }

    @Test
    fun theMiniSetExercisesEveryTableTheEngineReads() {
        val p = build().patterns
        assertTrue("host rules", build().hosts.hashes.isNotEmpty())
        assertTrue("host exceptions", build().hostExceptions.hashes.isNotEmpty())
        assertTrue("token rules", p.ruleIds.isNotEmpty())
        assertTrue("4-gram rules (foo.js| and adserver.)", p.gramRuleIds.isNotEmpty())
        assertTrue("token-less rules (a*b*c)", p.noToken.isNotEmpty())
        assertTrue("domain pool", p.domPool.isNotEmpty())
        assertTrue("some rule is $" + "important", (0 until p.count).any { (p.meta[it] and Bits.IMPORTANT) != 0 })
        assertTrue("some rule is an exception", (0 until p.count).any { (p.meta[it] and Bits.EXCEPTION) != 0 })
        assertTrue("some rule is left-anchored", (0 until p.count).any { (p.meta[it] and Bits.LEFT_ANCHOR) != 0 })
        assertTrue("some rule is right-anchored", (0 until p.count).any { (p.meta[it] and Bits.RIGHT_ANCHOR) != 0 })
        assertTrue("some rule is host-anchored", (0 until p.count).any { (p.meta[it] and Bits.LEFT_HOST) != 0 })
    }

    @Test
    fun theImageIsWellFormedAndMatchesTheLoadedTables() {
        val fs = build()
        val img = ExtImage.encode(fs, psl, 42)
        val b = ByteBuffer.wrap(img).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(ExtImage.MAGIC, b.getInt(0))
        assertEquals(ExtImage.VERSION, b.getInt(4))
        assertEquals(42, b.getInt(8))
        assertEquals(ExtImage.SECTIONS, b.getInt(12))
        assertEquals("size is a whole number of words", 0, img.size % 4)
        assertEquals("trailer is the wrapping word sum", ExtImage.checksum(img), b.getInt(img.size - 4))
        fun off(id: Int) = b.getInt(16 + id * 8)
        fun cnt(id: Int) = b.getInt(16 + id * 8 + 4)
        var prevEnd = 16 + ExtImage.SECTIONS * 8
        for (id in 0 until ExtImage.SECTIONS) {
            assertEquals("section $id starts where the previous ended (aligned, in order)", prevEnd, off(id))
            val bytes = when (id) { ExtImage.PAT_BYTES -> (cnt(id) + 3) and 3.inv(); ExtImage.HOST_HASH, ExtImage.EXC_HASH, ExtImage.DOM_POOL, ExtImage.PSL_RULES, ExtImage.PSL_WILD, ExtImage.PSL_EXC -> cnt(id) * 8; else -> cnt(id) * 4 }
            prevEnd = off(id) + bytes
        }
        assertEquals("sections end right before the trailer", img.size - 4, prevEnd)
        // contents: the first host hash round-trips as [lo, hi]
        val p = fs.patterns
        assertEquals(fs.hosts.hashes.size, cnt(ExtImage.HOST_HASH))
        assertEquals(fs.hosts.hashes[0].toInt(), b.getInt(off(ExtImage.HOST_HASH)))
        assertEquals((fs.hosts.hashes[0] ushr 32).toInt(), b.getInt(off(ExtImage.HOST_HASH) + 4))
        assertEquals(p.count + 1, cnt(ExtImage.PAT_OFF))
        assertEquals(p.patBytes.size, cnt(ExtImage.PAT_BYTES))
        val patCopy = ByteArray(p.patBytes.size)
        System.arraycopy(img, off(ExtImage.PAT_BYTES), patCopy, 0, patCopy.size)
        assertArrayEquals(p.patBytes, patCopy)
        assertEquals(psl.tables()[0].size, cnt(ExtImage.PSL_RULES))
    }

    @Test
    fun anEmptySetEncodes() {
        val img = ExtImage.encode(FilterSet.EMPTY, Psl.FALLBACK, 1)
        assertEquals(ExtImage.checksum(img), ByteBuffer.wrap(img).order(ByteOrder.LITTLE_ENDIAN).getInt(img.size - 4))
    }
}
