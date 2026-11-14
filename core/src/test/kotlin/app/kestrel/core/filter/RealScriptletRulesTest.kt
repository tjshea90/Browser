package app.kestrel.core.filter

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** AW1b on the real lists: how many `+js(...)` rules of the lists the app uses are understood, and that nothing accepted can hurt (needs the local uAssets checkout; skipped without it). */
class RealScriptletRulesTest {
    private fun uassets(): File? {
        var d: File? = File("").absoluteFile
        while (d != null) { val f = File(d, "ublockorigin/uassets"); if (File(f, "filters").isDirectory) return f; d = d.parentFile }
        return null
    }

    private fun lists(root: File): List<File> {
        val files = listOf("filters/filters.txt", "filters/filters-general.txt") + (2020..2026).map { "filters/filters-$it.txt" } +
            listOf("filters/ubo-link-shorteners.txt", "filters/filters-mobile.txt", "filters/unbreak.txt", "filters/privacy.txt", "thirdparties/easylist/easyprivacy.txt", "thirdparties/easylist/easylist-cookies.txt", "filters/quick-fixes.txt")
        return files.map { File(root, it) }.filter { it.isFile }
    }

    @Test fun mostRealScriptletRulesAreUnderstoodAndNoneAcceptedCarriesAnythingUnsafe() {
        val root = uassets()
        assumeTrue("uAssets checkout not found", root != null)
        val parser = RuleParser()
        var total = 0; var ok = 0; var entity = 0; var excl = 0
        val byName = HashMap<String, Int>(); val left = HashMap<String, Int>(); val sample = HashMap<String, ArrayList<String>>()
        for (f in lists(root!!)) for (raw in f.readLines(Charsets.UTF_8)) {
            val line = raw.trim()
            if (!line.contains("+js(") || line.startsWith("!")) continue
            if (RuleParser.looksCosmetic(line).not()) continue
            total++
            val r = parser.parseCosmetic(line)
            if (r != null) {
                ok++
                val c = ScriptletRules.decode(r.selector)
                if (c != null) {
                    byName.merge(c.name, 1, Int::plus)
                    assertTrue(r.selector, c.args.size <= ScriptletRules.MAX_ARGS && c.args.all { a -> a.length <= ScriptletRules.MAX_ARG && a.none { it.code < 0x20 || it.code == 0x7f } })
                } else assertTrue("only the 'all off' exception has no call: $line", r.selector == ScriptletRules.ALL_STORED && r.exception)
                if (r.domains.any { it.endsWith(".*") }) entity++
                if (r.excluded.isNotEmpty()) excl++
                assertTrue("never generic", r.domains.isNotEmpty())
            } else {
                val k = parser.skip.name + ":" + (ScriptletRules.splitArgs(line.substringAfter("+js(").removeSuffix(")"))?.firstOrNull()?.trim()?.lowercase() ?: "?")
                left.merge(k, 1, Int::plus)
                val l = sample.getOrPut(k) { ArrayList() }; if (l.size < 2) l.add(line.take(120))
            }
        }
        println("SCRIPTLETS real: $ok of $total accepted ($entity entity-domain, $excl with exclusions); by name: " + byName.entries.sortedByDescending { it.value }.take(25).joinToString(" ") { "${it.key}=${it.value}" })
        println("SCRIPTLETS real, left out (reason:name=count): " + left.entries.sortedByDescending { it.value }.take(25).joinToString("  ") { "${it.key}=${it.value}" })
        assertTrue("a good share of the scriptlet rules should be understood ($ok of $total)", total == 0 || ok * 100 >= total * 60)
    }

    @Test fun theWholeSnapshotBuildsWithScriptletsAndAnswersForRealSites() {
        val root = uassets()
        assumeTrue("uAssets checkout not found", root != null)
        val b = FilterSetBuilder()
        for (f in lists(root!!)) b.addAbpList(f.name, Category.ADS, f.readLines(Charsets.UTF_8).asSequence())
        val fs = b.build(1L)
        println("SCRIPTLETS build: " + b.report)
        val t0 = System.nanoTime()
        var n = 0
        for (h in listOf("www.youtube.com", "movielinkbd.com", "www.chip.de", "lookcam.com", "example.com", "www.bbc.co.uk", "doraby.com")) { val c = fs.scriptletsFor(h); n += c.size }
        val css = fs.cosmeticCss("www.youtube.com", tokenMode = true, scriptlets = true)
        println("SCRIPTLETS answers: $n calls for 7 hosts in ${(System.nanoTime() - t0) / 1000} us; youtube css head: " + css.take(220))
        assertTrue(b.report.scriptlets > 3000)
        assertTrue("a normal page with no rules has none", fs.scriptletsFor("example.com").isEmpty())
    }
}
