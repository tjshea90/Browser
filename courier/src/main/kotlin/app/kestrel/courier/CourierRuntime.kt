package app.kestrel.courier

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.StatFs
import android.util.Log
import app.kestrel.core.courier.ConnectProxy
import app.kestrel.core.courier.CourierHost
import app.kestrel.core.courier.CourierPorts
import app.kestrel.core.courier.CourierServer
import app.kestrel.core.courier.JsonRecStore
import app.kestrel.core.courier.JsonStreamStore
import app.kestrel.core.courier.PeerGuard
import app.kestrel.core.courier.GuardedDialer
import app.kestrel.core.courier.ProxyControl
import app.kestrel.core.courier.ProxyGate
import app.kestrel.core.courier.Sign
import app.kestrel.core.courier.Ssrf
import app.kestrel.core.courier.StagingSinks
import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Engine
import app.kestrel.core.download.NetGate
import app.kestrel.core.download.Spawner
import app.kestrel.core.download.UrlConnectionFetcher
import app.kestrel.core.media.StreamEngine
import app.kestrel.platform.Remuxer
import java.io.File
import java.io.IOException

/**
 * Everything the phone side needs while it is "Ready": the download engine with its files, the courier host that answers the tablet, and the socket server. Lives as long as the
 * foreground service runs. The wiring is the one the JVM tests exercise (core/src/test/.../CourierHarness.kt).
 */
object CourierRuntime {
    private const val TAG = "KestrelCourier"

    private class Running(val engine: Engine, val streams: StreamEngine, val proxy: ConnectProxy?, val host: CourierHost, val server: CourierServer, val control: HandlerThread, val handler: Handler, val port: Int, val netCallback: ConnectivityManager.NetworkCallback?)

    @Volatile private var running: Running? = null
    /** Why the last start failed (the screen shows it), or null. */
    @Volatile var lastError: String? = null
        private set

    val isRunning: Boolean get() = running != null
    val port: Int get() = running?.port ?: 0

    /** The downloads the phone holds right now, newest first (for the screen). */
    fun views(): List<DownloadView> = running?.host?.summaries()?.sortedByDescending { it.created } ?: emptyList()
    /** The Cancel button on the phone's own list: the tablet is told the job was stopped on the phone. */
    fun cancel(engineId: Long) { running?.host?.cancelLocal(engineId) }
    /** True while a download is moving: the service keeps the CPU awake then. */
    fun busy(): Boolean = running?.host?.needsService() == true
    fun jobCount(): Int = running?.host?.jobCount ?: 0

    /** What the page-load proxy has done since this run started; null = no proxy (not running, or its port was busy). */
    fun proxyStats(): ConnectProxy.Stats? = running?.proxy?.stats()

    @Synchronized fun start(ctx: Context): Boolean {
        if (running != null) return true
        val app = ctx.applicationContext
        val prefs = CourierPrefs(app)
        val debug = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // DEBUG builds only: the emulator rig's website is on a private address, which the real guard refuses on purpose
        val noGuard = debug && sysprop("debug.kestrel.courier.noguard") == "1"
        val fetcher = if (noGuard) UrlConnectionFetcher() else UrlConnectionFetcher(Ssrf.guard())
        val dir = File(app.filesDir, "courier").also { it.mkdirs() }
        val staging = File(dir, "staging").also { it.mkdirs() }
        val sinks = StagingSinks(staging)
        val control = HandlerThread("courier-control").also { it.start() }
        val handler = Handler(control.looper)
        val cm = app.getSystemService(ConnectivityManager::class.java)
        val net = object : NetGate {
            override val online: Boolean get() = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true
            override val metered: Boolean get() = false      // the owner chose to download; the tablet already asked (meteredOk) on its side
        }
        val engine = Engine(
            { fetcher }, sinks, JsonRecStore(File(dir, "downloads.json")) { Log.w(TAG, it) }, net, { r -> handler.post(r) }, Clock.System,
            Config(maxActive = 3, maxConnsTotal = 8, maxConnsPerDownload = 6),
            Spawner { name, task -> Thread(task, name).also { it.isDaemon = true; it.start() } },
        )
        // video streams are downloaded segment by segment and written straight into an MP4 by our own writer; what that cannot take (H.265, picture and sound kept apart in DASH) goes through the platform's own
        // converter, the same class the tablet uses (this phone's is newer and faster)
        val streams = StreamEngine(
            { fetcher }, sinks, JsonStreamStore(File(dir, "streams")) { Log.w(TAG, it) }, net, { r -> handler.post(r) }, Clock.System, Config(maxActive = 3, maxConnsTotal = 8, maxConnsPerDownload = 6),
            Spawner { name, task -> Thread(task, name).also { it.isDaemon = true; it.start() } },
            Remuxer(app, sinks, device = "phone", tag = TAG, roomDir = { dir }, note = { a, b, c -> Log.i(TAG, "$a: $b $c") }), { m, t -> Log.w(TAG, m, t) },
            keepApartOnMergeFailure = false,         // the phone never hands over a picture without its sound: the job fails and the tablet does it itself
        )
        engine.restore(); streams.restore()
        val resolver: (String) -> List<ByteArray> = if (noGuard) { { _ -> listOf(byteArrayOf(93, 184.toByte(), 216.toByte(), 34)) } } else { { h -> java.net.InetAddress.getAllByName(h).map { it.address } } }
        // PAGE LOADS for the tablet (research/SPEED2.md): an HTTP CONNECT proxy. The tablet's browser tunnels its https traffic through it and the phone opens the real connection with its OWN
        // network stack, which the carrier does not cap like tethered traffic. It listens only for a tablet that proved the pairing key in the last minutes ("armed") on a private non-cellular net.
        val gate = ProxyGate()
        val proxyPort = CourierPorts.proxyFor(prefs.port)
        // DEBUG builds only (the emulator rig): a host name ending in ".test" is the machine the emulator runs on (10.0.2.2), so a page at https://site.test:8443 can be loaded through the proxy
        val systemResolve: (String) -> List<ByteArray> = { h -> if (noGuard && h.endsWith(".test")) listOf(byteArrayOf(10, 0, 2, 2)) else java.net.InetAddress.getAllByName(h).map { it.address } }
        val proxy = ConnectProxy(
            proxyPort, null, { remote, local -> gate.isArmed(remote) && PeerGuard.allowed(remote.address, local.address, CourierNet.locals()) },
            if (noGuard) GuardedDialer(systemResolve, { false }) else GuardedDialer(systemResolve), log = { Log.i(TAG, it) }, verbose = debug,
        )
        val proxyBound = try { proxy.start() } catch (e: IOException) { Log.w(TAG, "page proxy: port $proxyPort is busy, pages are not offered"); 0 }
        val proxyControl = if (proxyBound != 0) ProxyControl(gate, proxyBound) { proxy.stats() } else null
        val host = CourierHost(
            Sign.deriveKey(prefs.secret()), engine, sinks, File(dir, "courier-jobs.json"), fetcher, Clock.System, { freeBytes(staging) }, Build.MODEL ?: "phone",
            log = { Log.w(TAG, it) }, resolve = resolver, streams = streams, proxy = proxyControl,
        )
        host.load()
        val server = CourierServer(prefs.port, null, { remote, local -> PeerGuard.allowed(remote.address, local.address, CourierNet.locals()) }, { req, peer -> host.handle(req, peer) }, log = { Log.w(TAG, it) })
        val bound = try { server.start() } catch (e: IOException) {
            lastError = "Port ${prefs.port} is already used by another app."
            proxy.stop()
            control.quitSafely()
            return false
        }
        var cb: ConnectivityManager.NetworkCallback? = null
        if (cm != null) {
            cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { engine.onNetwork(); streams.onNetwork() }
                override fun onLost(network: Network) { engine.onNetwork(); streams.onNetwork() }
            }
            try { cm.registerDefaultNetworkCallback(cb, handler) } catch (e: Exception) { cb = null }
        }
        handler.post { engine.resumeInterrupted(); streams.resumeInterrupted() }                       // what the last run was in the middle of carries on by itself
        val r = Running(engine, streams, if (proxyControl != null) proxy else null, host, server, control, handler, bound, cb)
        running = r
        lastError = null
        val tick = object : Runnable {
            var n = 0
            override fun run() {
                if (running !== r) return
                engine.tick()
                if (++n % 60 == 0) host.tick()
                handler.postDelayed(this, if (host.needsService()) 1000L else 5000L)        // nothing running: wake rarely (battery)
            }
        }
        handler.postDelayed(tick, 1000)
        Log.i(TAG, "ready on port $bound, ${host.jobCount} jobs known")
        return true
    }

    @Synchronized fun stop(ctx: Context) {
        val r = running ?: return
        running = null
        try { ctx.applicationContext.getSystemService(ConnectivityManager::class.java)?.let { cm -> r.netCallback?.let { cm.unregisterNetworkCallback(it) } } } catch (e: Exception) { /* already gone */ }
        r.server.stop(); r.proxy?.stop()
        r.engine.pauseAllSystem(); r.streams.pauseAllSystem()               // checkpoints every running download; they carry on at the next start
        r.handler.postDelayed({ r.control.quitSafely() }, 1500)
    }

    /** A new pairing code: stop answering the old tablet at once. */
    fun restart(ctx: Context) { if (running != null) { stop(ctx); start(ctx) } }

    private fun freeBytes(dir: File): Long = try { StatFs(dir.path).availableBytes } catch (e: Exception) { 0L }

    private fun sysprop(name: String): String? = try { Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as? String } catch (e: Throwable) { null }
}
