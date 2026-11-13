package app.kestrel.core.filter

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** AD1b-4 on the real lists: how many procedural cosmetic rules (`:has-text()`, `:upward()`, `:remove()` ...) of the lists the app uses are understood (needs the local uAssets checkout; skipped without it). */
class RealProceduralRulesTest {
    private fun uassets(): File? {
        var d: File? = File("").absoluteFile
        while (d != null) { val f = File(d, "ublockorigin/uassets"); if (File(f, "filters").isDirectory) return f; d = d.parentFile }
        return null
    }

    @Test fun manyRealProceduralRulesAreAcceptedAndNoneAcceptedCarriesAnythingUnsafe() {
        val root = uassets()
        assumeTrue("uAssets checkout not found", root != null)
        val files = listOf("filters/filters.txt", "filters/filters-general.txt") + (2020..2026).map { "filters/filters-$it.txt" } +
            listOf("filters/unbreak.txt", "filters/badware.txt", "filters/privacy.txt", "thirdparties/easylist/easylist.txt", "thirdparties/easylist/easyprivacy.txt", "thirdparties/easylist/easylist-cookies.txt")
        val parser = RuleParser()
        var total = 0; var ok = 0; var proc = 0
        val why = HashMap<Skip, Int>(); val sample = HashMap<Skip, ArrayList<String>>()
        for (f in files) {
            val file = File(root, f)
            if (!file.isFile) continue
            for (raw in file.readLines(Charsets.UTF_8)) {
                val line = raw.trim()
                if (!(line.contains(":has-text(") || line.contains(":contains(") || line.contains(":-abp-contains(") || line.contains(":upward(") || line.contains(":remove(") || line.contains(":remove-attr(") || line.contains(":remove-class(")) || line.contains(":style(") || line.startsWith("!") || !RuleParser.looksCosmetic(line)) continue
                total++
                val r = parser.parseCosmetic(line)
                if (r != null) {
                    ok++
                    if (ProceduralRules.isStored(r.selector)) {
                        proc++
                        val rule = ProceduralRules.decode(r.selector)!!
                        assertTrue(r.selector, rule.ops.isNotEmpty() && rule.ops.size <= 4)
                        for (op in rule.ops) assertTrue(r.selector, op.arg.none { it == '<' || it == '`' || it.code < 0x20 })
                    }
                } else {
                    why.merge(parser.skip, 1, Int::plus)
                    val l = sample.getOrPut(parser.skip) { ArrayList() }; if (l.size < 6) l.add(line.take(140))
                }
            }
        }
        println("PROCEDURAL real: $ok of $total accepted ($proc procedural); left out: $why")
        for ((k, v) in sample) println("PROCEDURAL left out $k e.g. " + v.joinToString("  ||  "))
        assertTrue("a good share of the procedural rules should be understood ($ok of $total)", total == 0 || ok * 3 >= total)
    }
}
