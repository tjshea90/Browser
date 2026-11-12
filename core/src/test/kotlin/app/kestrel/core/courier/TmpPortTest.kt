package app.kestrel.core.courier

import org.junit.Test
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket

class TmpPortTest {
    @Test fun loop() {
        var bad = 0
        val n = 300
        for (i in 0 until n) {
            val srv = CourierServer(0, InetAddress.getByName("127.0.0.1"), { it.isLoopbackAddress }, { _, _ -> HttpResponse.json(200, mapOf("ok" to true)) })
            val port = srv.start()
            val s = Socket("127.0.0.1", port)
            s.getOutputStream().write("GET /x HTTP/1.1\r\n\r\n".toByteArray()); s.getOutputStream().flush()
            val buf = ByteArray(4096); s.getInputStream().read(buf)
            srv.stop()
            try {
                val x = Socket("127.0.0.1", port)
                bad++
                // who answers?
                x.soTimeout = 500
                x.getOutputStream().write("GET /x HTTP/1.1\r\n\r\n".toByteArray()); x.getOutputStream().flush()
                val r = try { x.getInputStream().read(buf) } catch (e: Exception) { -99 }
                println("iteration $i: connected after stop, port=$port local=${x.localPort} answer=$r running=${srv.running}")
                x.close()
            } catch (e: ConnectException) { }
            s.close()
        }
        println("TMPPORT bad=$bad of $n")
    }
}
