package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** TEMPORARY probe (not committed): the cosmetic CSS the extension would inject on one reported page. */
class ZzDoraCosTest {
    @Test fun probe() {
        val S = "/tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad/dora"
        val kfs = File("../app/src/main/assets/filters/default.kfs"); val pslFile = File("../app/src/main/assets/psl.txt"); val tok = File("$S/tokens.txt")
        assumeTrue(kfs.isFile && pslFile.isFile && tok.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes())
        val tokens = tok.readLines().filter { it.isNotEmpty() }
        val site = fs.cosmeticCss("doraby.com", true, tokenMode = true)
        File("$S/css_site.css").writeText(site)
        var all = StringBuilder(); var n = 0
        var i = 0
        while (i < tokens.size) { val batch = tokens.subList(i, minOf(i + 200, tokens.size)); all.append(fs.genericCssForTokens("doraby.com", batch)); i += 200; n++ }
        File("$S/css_generic.css").writeText(all.toString())
        println("DORACOS site=" + site.length + " generic=" + all.length + " procedural=" + fs.proceduralFor("doraby.com").size)
    }
}
