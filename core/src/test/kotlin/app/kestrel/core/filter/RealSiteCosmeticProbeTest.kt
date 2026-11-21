package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** VD2c probe (owner 2026-11-21: with Shields on, the loading circle over a pornhub video is cut off; with Shields off it is whole): prints what the REAL bundled lists hide and run on a host. Output only; no assertion on the lists. */
class RealSiteCosmeticProbeTest {
    @Test fun printWhatTheRealListsDoOnTheHost() {
        var d: File? = File("").absoluteFile; var fs: FilterSet? = null
        while (d != null) { val f = File(d, "app/src/main/assets/filters/default.kfs"); if (f.isFile) { fs = FilterSetCodec.read(f.readBytes()); break }; d = d.parentFile }
        assumeTrue("default.kfs not found", fs != null)
        fs!!
        for (host in listOf("www.pornhub.com", "pornhub.com")) {
            val css = fs.cosmeticCss(host, includeGeneric = false)
            println("PROBE host=$host specific-css-bytes=${css.length}")
            println("PROBE css[$host]=" + css.replace("\n", " ").take(6000))
            println("PROBE scriptlets[$host]=" + fs.scriptletsFor(host).joinToString(" ; ").take(4000))
            println("PROBE procedural[$host]=" + fs.proceduralFor(host).joinToString(" ; ").take(2000))
        }
    }
}
