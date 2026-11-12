package app.kestrel.core.media

import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DlState
import app.kestrel.core.download.FakeNet
import app.kestrel.core.download.FakeSinks
import app.kestrel.core.download.testBytes
import org.junit.Test
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DebugTmpTest {
    @Test fun wrongKey() {
        val cdn = FakeCdn(); val store = MemStreamStore()
        cdn.files["https://cdn.test/v/key.bin"] = testBytes(16, 1)
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(testBytes(16, 2), "AES"), IvParameterSpec(Hls.sequenceIv(0)))
        cdn.files["https://cdn.test/v/e0.ts"] = c.doFinal(testBytes(1000, 3))
        val exec = Executors.newSingleThreadExecutor { r -> Thread(r).also { it.isDaemon = true } }
        val engine = StreamEngine({ cdn }, FakeSinks(), store, FakeNet(), exec, Clock.System, Config(checkpointMs = 40, backoffBaseMs = 1, backoffCapMs = 4), log = { m, t -> println("LOG: $m"); t?.printStackTrace(System.out) })
        val id = engine.enqueue(StreamSource("https://cdn.test/v/index.m3u8", "#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXTINF:4,\ne0.ts\n#EXT-X-ENDLIST\n", "https://site.test/watch", "My clip"))
        val end = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < end && engine.view(id)?.state != DlState.FAILED) Thread.sleep(10)
        println("RESULT: ${engine.view(id)?.state} ${engine.view(id)?.reason}")
        exec.shutdownNow()
    }
}
