package app.kestrel.core.courier

import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.Engine
import app.kestrel.core.download.FakeNet
import app.kestrel.core.download.FakeServer
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.FetchResponse
import app.kestrel.core.download.Fetcher
import app.kestrel.core.download.SinkFactory
import app.kestrel.core.media.StreamFinisher
import app.kestrel.core.media.FakeCdn
import app.kestrel.core.media.StreamEngine
import java.io.File
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** The pairing secret and key every courier test uses. */
val TEST_SECRET = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
val TEST_KEY: ByteArray = Sign.deriveKey(TEST_SECRET)

/** Small, fast engine settings for tests (real threads, tiny back-offs). */
fun courierTestConfig() = Config(
    maxActive = 2, parallel = false, maxConnsPerDownload = 3, maxConnsTotal = 6, windowBytes = 1L shl 20, minGrowRemaining = 1L shl 20, minGrowEtaSec = 0,
    rampMs = 0, evalMs = 25, growGain = 0.0, checkpointMs = 40, checkpointBytes = 1L shl 20, stallMs = 60_000, backoffBaseMs = 1, backoffCapMs = 4,
)

/** Records the headers the engine sends to the origin. */
class Spy(val inner: FakeServer) : Fetcher {
    val headers = CopyOnWriteArrayList<Map<String, String>>()
    override val probeWindow: Long get() = inner.probeWindow
    override val windowBytes: Long get() = inner.windowBytes
    override fun open(req: FetchRequest): FetchResponse { headers.add(req.headers); return inner.open(req) }
}

/** A clock that can be moved forward (the engine and the phone host share one). */
class Skew : Clock { @Volatile var ms = 0L; override fun now() = System.currentTimeMillis() + ms }

/**
 * A whole phone: a real [CourierServer] on loopback, the real [CourierHost], the real download [Engine] with real files under [dir]; only the origin website is faked.
 * [cleanups] receives the stop action so the owning test can tear everything down.
 */
class Phone(
    val origin: FakeServer,
    root: File,
    cleanups: MutableList<() -> Unit>,
    hostKey: ByteArray = TEST_KEY,
    val dir: File = File(root, "phone"),
    val clock: Skew = Skew(),
    /** The fake video CDN: when given, the phone also has the segment downloader (video streams); null = plain files only. */
    val cdn: FakeCdn? = null,
    /** The phone's platform converter (picture + sound merge, H.265 ...): a fake in the JVM tests; null = none (the engine's own MP4 writer only). */
    finisherOf: ((SinkFactory) -> StreamFinisher)? = null,
    /** True = the phone also runs the page-load proxy on loopback (armed per tablet through `POST /v1/proxy/arm`); it dials any destination, like a phone on a good network. */
    withProxy: Boolean = false,
) {
    val proxyGate = ProxyGate(clock)
    val proxyServer: ConnectProxy? = if (withProxy) ConnectProxy(0, InetAddress.getByName("127.0.0.1"), { r, _ -> proxyGate.isArmed(r) }, GuardedDialer({ h -> listOf(InetAddress.getByName(h).address) }, { false }), clock = clock) else null
    val proxyPort: Int = proxyServer?.start() ?: 0
    val spy = Spy(origin)
    val sinks = StagingSinks(File(dir, "staging"))
    val store = JsonRecStore(File(dir, "downloads.json"))
    val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "phone-control").also { it.isDaemon = true } }
    val engine = Engine({ spy }, sinks, store, FakeNet(), exec, clock, courierTestConfig())
    @Volatile var free = 100L * 1024 * 1024 * 1024
    @Volatile var dns: List<ByteArray> = listOf(byteArrayOf(93.toByte(), 184.toByte(), 216.toByte(), 34))
    /** Host names that resolve differently from the default [dns] (a name that points at a private address). */
    val dnsFor = java.util.concurrent.ConcurrentHashMap<String, List<ByteArray>>()
    val streamStore = JsonStreamStore(File(dir, "streams"))
    val streams: StreamEngine? = cdn?.let { c -> StreamEngine({ c }, sinks, streamStore, FakeNet(), exec, clock, courierTestConfig(), finisher = finisherOf?.invoke(sinks), keepApartOnMergeFailure = false) }
    val host = CourierHost(hostKey, engine, sinks, File(dir, "courier-jobs.json"), spy, clock, { free }, "Test phone", resolve = { h -> dnsFor[h] ?: if (h.firstOrNull()?.isDigit() == true && h.contains('.')) listOf(InetAddress.getByName(h).address) else dns }, streams = streams,
        proxy = proxyServer?.let { ps -> ProxyControl(proxyGate, proxyPort) { ps.stats() } })
    val server = CourierServer(0, InetAddress.getByName("127.0.0.1"), { r, _ -> r.isLoopbackAddress }, { r, p -> host.handle(r, p) })
    @Volatile var ticking = true
    val port: Int
    val base: String

    init {
        engine.restore(); streams?.restore(); host.load(); engine.resumeInterrupted(); streams?.resumeInterrupted()
        Thread { while (ticking) { engine.tick(); try { Thread.sleep(20) } catch (e: InterruptedException) { return@Thread } } }.also { it.isDaemon = true; it.start() }
        port = server.start()
        base = "http://127.0.0.1:$port"
        cleanups.add { stop() }
    }

    fun stop() { ticking = false; server.stop(); proxyServer?.stop(); exec.shutdownNow() }
}
