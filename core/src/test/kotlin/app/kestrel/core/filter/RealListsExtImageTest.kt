package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Random

/**
 * Differential check of the extension's JS engine against the Kotlin engine on the REAL bundled lists (~120,000 network rules), not the small fixture set.
 * Off by default (it writes megabytes). Run:
 *   KESTREL_REAL_DIFF=/some/empty/dir ./gradlew :core:test --tests '*RealListsExtImageTest*'
 *   node web-tests/real-diff.js /some/empty/dir
 * Requests are synthesised from the compiled pattern table (its rule texts survive compilation): for every pattern rule a URL that should match it, a near miss and a plain neighbour,
 * with the rule's own request type and party; plus real-looking URLs on the hosts the table knows. The Kotlin verdict is recorded next to each request.
 */
class RealListsExtImageTest {
    private fun repoFile(rel: String): File {
        var d: File? = File("").absoluteFile
        while (d != null) { val f = File(d, rel); if (f.isFile) return f; d = d.parentFile }
        throw IllegalStateException("$rel not found above " + File("").absolutePath)
    }

    @Test
    fun writeTheRealListsImageAndACorpus() {
        val outDir = System.getenv("KESTREL_REAL_DIFF")
        assumeTrue("set KESTREL_REAL_DIFF to a directory to run this", !outDir.isNullOrEmpty())
        val dir = File(outDir!!).also { it.mkdirs() }
        val psl = Psl.parse(repoFile("app/src/main/assets/psl.txt").readLines().asSequence())
        val fs = FilterSetCodec.read(repoFile("app/src/main/assets/filters/default.kfs").readBytes())
        File(dir, "real.kex").writeBytes(ExtImage.encode(fs, psl, 1))

        val p = fs.patterns
        val rnd = Random(424242L)
        val types = intArrayOf(RequestType.SCRIPT, RequestType.IMAGE, RequestType.XHR, RequestType.STYLESHEET, RequestType.SUBDOCUMENT, RequestType.OTHER, RequestType.MEDIA, RequestType.FONT, RequestType.WEBSOCKET, RequestType.PING, RequestType.OBJECT)
        val pages = listOf("example.com", "www.nytimes.com", "news.bbc.co.uk", "en.wikipedia.org", "reddit.com", "twitch.tv", "github.com", "other-site.example", "forum.example.org")
        val out = File(dir, "real.corpus.tsv").bufferedWriter()
        var n = 0
        fun emit(url: String, type: Int, page: String?) {
            out.write(url); out.write("\t"); out.write(type.toString()); out.write("\t"); out.write(page ?: ""); out.write("\t")
            out.write(fs.check(url, page, type, psl).toString()); out.write("\n")
            n++
        }
        for (id in 0 until p.count) {
            val s = p.patOff[id]; val e = p.patOff[id + 1]
            val w = p.meta[id]
            val sb = StringBuilder()
            for (k in s until e) {
                val c = (p.patBytes[k].toInt() and 0xFF).toChar()
                sb.append(when (c) { '*' -> "x1"; '^' -> '/'; else -> c })
            }
            var body = sb.toString()
            val url = when {
                (w and Bits.LEFT_HOST) != 0 -> "https://" + (if (body.contains('/') || body.contains('?')) body else "$body/")
                (w and Bits.LEFT_ANCHOR) != 0 -> if (body.startsWith("http") || body.startsWith("ws")) body else "https://a.test/$body"
                else -> "https://a.test/" + body
            }
            val mask = w and Bits.TYPES
            val typeBits = types.filter { (mask and it) != 0 }
            val type = if (typeBits.isEmpty()) types[rnd.nextInt(types.size)] else typeBits[rnd.nextInt(typeBits.size)]
            val party = (w ushr Bits.PARTY_SHIFT) and 3
            val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':').substringAfterLast('@')
            val page = when (party) { 2 -> host; 1 -> "other-site.example"; else -> pages[rnd.nextInt(pages.size)] }
            emit(url, type, page)
            // a near miss: one character of the body changed
            if (body.length > 3) {
                val i = rnd.nextInt(body.length)
                val miss = url.replaceFirst(body, body.substring(0, i) + (if (body[i] == 'q') 'z' else 'q') + body.substring(i + 1))
                emit(miss, type, page)
            }
            // the same URL as a different type and as a page-less request
            emit(url, types[rnd.nextInt(types.size)], null)
        }
        // real-looking traffic that mostly passes
        val common = listOf("https://www.google.com/search?q=cats", "https://fonts.googleapis.com/css2?family=Roboto", "https://cdn.jsdelivr.net/npm/vue@3/dist/vue.global.js", "https://upload.wikimedia.org/wikipedia/commons/a/a9/x.png",
            "https://www.youtube.com/watch?v=abc", "https://i.ytimg.com/vi/abc/hqdefault.jpg", "https://static.xx.fbcdn.net/rsrc.php/v3/y.js", "https://www.googletagmanager.com/gtag/js?id=G-1", "https://www.google-analytics.com/collect?v=1",
            "https://securepubads.g.doubleclick.net/tag/js/gpt.js", "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "https://connect.facebook.net/en_US/fbevents.js", "https://platform.twitter.com/widgets.js",
            "https://js-agent.newrelic.com/nr-1.js", "https://cdn.segment.com/analytics.js/v1/x/analytics.min.js", "https://www.redditstatic.com/ads/pixel.js", "https://api.github.com/repos/x/y", "https://github.githubassets.com/assets/app.js")
        for (u in common) for (page in pages + listOf(null)) for (t in types) if (rnd.nextInt(3) == 0) emit(u, t, page)
        out.close()
        println("real-lists corpus: $n requests, image ${File(dir, "real.kex").length()} bytes in $dir")
    }
}
