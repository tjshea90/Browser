package app.kestrel.core.media

import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DlState
import app.kestrel.core.download.FakeNet
import app.kestrel.core.download.FakeSinks
import app.kestrel.core.download.testBytes
import org.junit.Test
import java.util.concurrent.Executors

class DebugTmpTest {
    @Test fun estimate() {
        val cdn = FakeCdn(); val store = MemStreamStore(); cdn.chunkDelayMs = 5; cdn.chunk = 4096
        val sb = StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:4\n#EXT-X-MEDIA-SEQUENCE:0\n")
        for (i in 0 until 16) { cdn.files["https://cdn.test/v/s$i.ts"] = testBytes(40_000, 100L + i); sb.append("#EXTINF:4.0,\ns$i.ts\n") }
        sb.append("#EXT-X-ENDLIST\n")
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r).also { it.isDaemon = true } }
        val engine = StreamEngine({ cdn }, FakeSinks(), store, FakeNet(), exec, Clock.System, Config(maxActive = 2, maxConnsPerDownload = 3, checkpointMs = 40, backoffBaseMs = 1, backoffCapMs = 4))
        val id = engine.enqueue(StreamSource("https://cdn.test/v/index.m3u8", sb.toString(), "https://site.test/watch", "My clip"))
        val end = System.currentTimeMillis() + 20000
        var last = -1L
        while (System.currentTimeMillis() < end) {
            val v = engine.view(id) ?: run { Thread.sleep(10); continue }
            if (v.state != DlState.QUEUED) println("T ${v.state} done=${v.done} total=${v.total} est=${v.estimated} ratio=${v.total.toDouble() / 640000}")
            last = v.done
            if (v.state == DlState.COMPLETED) break
            Thread.sleep(40)
        }
        exec.shutdownNow()
    }
}
