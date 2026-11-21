package app.kestrel.core.filter

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** ATT1 (temporary probe): what do the REAL bundled lists say about the addresses Gemini's attachment flow uses? */
class GeminiProbeTest {
    @Test fun probe() {
        val kfs = File("../app/src/main/assets/filters/default.kfs"); val pslFile = File("../app/src/main/assets/psl.txt")
        assumeTrue(kfs.isFile && pslFile.isFile)
        val fs = FilterSetCodec.read(kfs.readBytes()); val psl = Psl.parse(pslFile.readLines().asSequence())
        val page = "gemini.google.com"
        val cases = listOf(
            "https://push.clients6.google.com/upload/" to RequestType.XHR,
            "https://content-push.googleapis.com/upload/" to RequestType.XHR,
            "https://clients6.google.com/upload/" to RequestType.XHR,
            "https://contribution.usercontent.google.com/upload" to RequestType.XHR,
            "https://gemini.google.com/_/BardChatUi/data/batchexecute?rpcids=x" to RequestType.XHR,
            "https://gemini.google.com/upload" to RequestType.XHR,
            "https://lh3.googleusercontent.com/gg/abc" to RequestType.IMAGE,
            "https://docs.google.com/picker?protocol=gadgets" to RequestType.SUBDOCUMENT,
            "https://apis.google.com/js/api.js" to RequestType.SCRIPT,
            "https://www.gstatic.com/_/mss/boq-bard-web/_/js/k=x/m=y" to RequestType.SCRIPT,
            "https://drive.google.com/picker" to RequestType.SUBDOCUMENT,
            "https://content.googleapis.com/drive/v3/files" to RequestType.XHR,
            "https://people-pa.clients6.google.com/\$rpc/x" to RequestType.XHR,
            "https://play.google.com/log?format=json" to RequestType.XHR,
            "https://ogads-pa.clients6.google.com/\$rpc/x" to RequestType.XHR,
        )
        for ((u, t) in cases) println("GEMINI-PROBE " + (if (Verdict.isBlocked(fs.check(u, page, t, psl))) "BLOCKED " else "allowed ") + u)
    }
}
