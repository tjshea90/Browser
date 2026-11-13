package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** TEMPORARY probe (not committed): what the bundled snapshot says about every request of one reported page. */
class ZzDoraTest {
    @Test fun probe() {
        val kfs = File("../app/src/main/assets/filters/default.kfs"); val pslFile = File("../app/src/main/assets/psl.txt")
        val urls = File(System.getProperty("dora.urls") ?: "/tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad/dora/urls.tsv")
        assumeTrue(kfs.isFile && pslFile.isFile && urls.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes()); val psl = Psl.parse(pslFile.readLines().asSequence())
        for (line in urls.readLines()) {
            val tab = line.indexOf('\t'); if (tab < 0) continue
            val t = line.substring(0, tab).toInt(); val u = line.substring(tab + 1)
            val v = fs.check(u, "doraby.com", t, psl)
            println("DORA " + (if (Verdict.isBlocked(v)) "BLOCK" else if ((v and Verdict.EXCEPTED) != 0) "excpt" else "allow") + " t=" + t + " " + u.take(150))
        }
    }
}
