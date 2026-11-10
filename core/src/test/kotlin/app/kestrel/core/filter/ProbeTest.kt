package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class ProbeTest {
    @Test
    fun probe() {
        val kfs = File("../app/src/main/assets/filters/default.kfs"); val pslF = File("../app/src/main/assets/psl.txt")
        assumeTrue(kfs.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes()); val psl = Psl.parse(pslF.readLines().asSequence())
        val names = mapOf("DOCUMENT" to RequestType.DOCUMENT, "STYLESHEET" to RequestType.STYLESHEET, "SCRIPT" to RequestType.SCRIPT, "XHR" to RequestType.XHR,
            "OTHER" to RequestType.OTHER, "PING" to RequestType.PING, "FONT" to RequestType.FONT, "UNKNOWN" to RequestType.UNKNOWN)
        for (u in listOf("https://fonts.googleapis.com/css2?family=Roboto", "https://fonts.googleapis.com/css?family=Open+Sans", "https://js.stripe.com/v3/")) {
            val row = names.entries.joinToString(" ") { (n, t) -> n + "=" + (if (Verdict.isBlocked(fs.check(u, "news.example", t, psl))) "BLOCK" else "ok") }
            println("PROBE $u -> $row")
        }
    }
}
