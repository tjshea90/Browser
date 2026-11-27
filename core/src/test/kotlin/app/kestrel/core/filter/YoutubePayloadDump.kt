package app.kestrel.core.filter

import java.io.File
import org.junit.Test

/**
 * A DEVELOPER TOOL, not a test (it does nothing unless KESTREL_DUMP_PAYLOAD names a folder): writes the scriptlet payload the app would hand the page library for www.youtube.com and m.youtube.com,
 * compiled from the real list lines in src/test/resources/youtube-scriptlets.txt as a trusted list. tools/sitelab/yt_live.js feeds these files to the real library on the real site (RX3-Y3).
 *   KESTREL_DUMP_PAYLOAD=<folder> ./gradlew :core:cleanTest :core:test --no-build-cache --tests 'app.kestrel.core.filter.YoutubePayloadDump'
 */
class YoutubePayloadDump {
    @Test fun dump() {
        val dir = System.getenv("KESTREL_DUMP_PAYLOAD") ?: return
        val lines = YoutubePayloadDump::class.java.getResourceAsStream("/youtube-scriptlets.txt")!!.bufferedReader(Charsets.US_ASCII).readLines().filter { it.isNotBlank() && !it.startsWith("!") }
        val b = FilterSetBuilder(); b.addAbpList("ubo-youtube", Category.ADS, lines.asSequence(), trusted = true)
        val fs = b.build(1L)
        File(dir, "yt-payload-www.json").writeText(fs.scriptletPayload("www.youtube.com"))
        File(dir, "yt-payload-m.json").writeText(fs.scriptletPayload("m.youtube.com"))
    }
}
