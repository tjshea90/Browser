package app.kestrel.core.filter

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** AA3-3b on the real lists: how many `:style()` rules of the lists the app uses become restyling rules (needs the local uAssets checkout; skipped without it). */
class RealStyleRulesTest {
    private fun uassets(): File? {
        var d: File? = File("").absoluteFile
        while (d != null) { val f = File(d, "ublockorigin/uassets"); if (File(f, "filters").isDirectory) return f; d = d.parentFile }
        return null
    }

    @Test fun mostRealStyleRulesAreAcceptedAndNoneAcceptedCarriesAnythingUnsafe() {
        val root = uassets()
        assumeTrue("uAssets checkout not found", root != null)
        val files = listOf("filters/filters.txt", "filters/filters-general.txt") + (2020..2026).map { "filters/filters-$it.txt" } +
            listOf("filters/unbreak.txt", "filters/badware.txt", "filters/privacy.txt", "thirdparties/easylist/easylist.txt", "thirdparties/easylist/easyprivacy.txt", "thirdparties/easylist/easylist-cookies.txt")
        val parser = RuleParser()
        var total = 0; var ok = 0
        val why = HashMap<Skip, Int>(); val sample = HashMap<Skip, ArrayList<String>>()
        for (f in files) {
            val file = File(root, f)
            if (!file.isFile) continue
            for (raw in file.readLines(Charsets.UTF_8)) {
                val line = raw.trim()
                if (!line.contains(":style(") || line.startsWith("!") || !RuleParser.looksCosmetic(line)) continue
                total++
                val r = parser.parseCosmetic(line)
                if (r != null) {
                    ok++
                    val i = r.selector.indexOf(CosmeticSelectors.STYLE_SEP)
                    assertTrue(r.selector, i > 0)
                    val decls = r.selector.substring(i + 1)
                    assertTrue(decls, decls.none { it == '{' || it == '}' || it == '"' || it == '\'' || it == '\\' || it == '@' || it == '<' } && !decls.lowercase().contains("url("))
                } else {
                    why.merge(parser.skip, 1, Int::plus)
                    val l = sample.getOrPut(parser.skip) { ArrayList() }; if (l.size < 6) l.add(line.take(140))
                }
            }
        }
        println("STYLE real: $ok of $total accepted; left out: $why")
        for ((k, v) in sample) println("STYLE left out $k e.g. " + v.joinToString("  ||  "))
        assertTrue("most site restyling rules should be understood ($ok of $total)", total == 0 || ok * 2 >= total)
    }
}
