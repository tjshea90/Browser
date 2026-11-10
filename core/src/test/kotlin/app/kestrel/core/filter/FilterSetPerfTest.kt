package app.kestrel.core.filter

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Performance guard-rails (loose bounds so they don't flake on a loaded CI box; actual numbers are printed).
 * The tablet's Cortex-A53 cores are several times slower than this machine, hence the generous headroom budget.
 */
class FilterSetPerfTest {
    private val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    private fun word(r: Random, n: Int) = String(CharArray(n) { alphabet[r.nextInt(alphabet.length)] })

    @Test
    fun largeListBuildsAndMatchesFast() {
        val r = Random(42)
        val b = FilterSetBuilder()
        val t0 = System.nanoTime()
        val lines = ArrayList<String>(160_000)
        repeat(100_000) { lines.add("||" + word(r, 4 + r.nextInt(8)) + "." + listOf("com", "net", "io", "org")[r.nextInt(4)] + "^") }
        repeat(40_000) {
            val w1 = word(r, 4 + r.nextInt(6)); val w2 = word(r, 3 + r.nextInt(6))
            lines.add(when (r.nextInt(4)) {
                0 -> "/$w1/$w2."
                1 -> "||" + word(r, 6) + ".com/$w1^"
                2 -> "/$w1/*/$w2"
                else -> "$w1$w2.js|"
            })
        }
        repeat(20_000) { lines.add("##.x" + word(r, 6)) }
        b.addAbpList("big", Category.ADS, lines.asSequence())
        val fs = b.build(1L)
        val buildMs = (System.nanoTime() - t0) / 1_000_000

        val urls = List(20_000) {
            "https://" + word(r, 3 + r.nextInt(6)) + "." + listOf("com", "net", "org")[r.nextInt(3)] + "/" + word(r, 5) + "/" + word(r, 6) + "." + listOf("js", "png", "html")[r.nextInt(3)] + "?a=" + word(r, 8)
        }
        repeat(2) { for (u in urls) fs.check(u, "page.example", RequestType.SCRIPT) } // warm up the JIT
        val t1 = System.nanoTime()
        var blocked = 0
        for (u in urls) if (Verdict.isBlocked(fs.check(u, "page.example", RequestType.SCRIPT))) blocked++
        val perUs = (System.nanoTime() - t1) / 1000.0 / urls.size

        val bytes = FilterSetCodec.write(fs)
        val t2 = System.nanoTime()
        FilterSetCodec.read(bytes)
        val loadMs = (System.nanoTime() - t2) / 1_000_000

        println("PERF rules: host=${fs.meta.hostRules} pattern=${fs.meta.patternRules}  build=${buildMs}ms  match=${"%.2f".format(perUs)}us/req  blockedProbes=$blocked  kfs=${bytes.size / 1024}KB  load=${loadMs}ms")
        assertTrue("match too slow: ${perUs}us/request", perUs < 150.0)
        assertTrue("load too slow: ${loadMs}ms", loadMs < 1500)
        assertTrue("file unexpectedly large: ${bytes.size / 1024}KB", bytes.size < 8 * 1024 * 1024)
    }
}
