package app.kestrel.browser.download

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import app.kestrel.browser.Io
import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Engine
import app.kestrel.core.download.Fetcher
import app.kestrel.core.download.NetGate
import app.kestrel.core.download.Source
import app.kestrel.core.download.Spawner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The app's one download engine and everything around it: its control thread, the network gate, notifications for finished files. The activities only talk to this
 * object (and never hold download state), so switching apps or rotating the screen cannot lose or restart anything; [DownloadService] keeps the process alive.
 */
object DownloadController {
    private var engine: Engine? = null
    private lateinit var app: Context
    private lateinit var handler: Handler
    private val fetchers = ConcurrentHashMap<Int, Fetcher>()
    private val uiListeners = CopyOnWriteArrayList<() -> Unit>()
    private val dirty = AtomicBoolean(false)
    @Volatile private var netOnline = true
    @Volatile private var netMetered = true
    private var netCallback: ConnectivityManager.NetworkCallback? = null
    private val lastStates = HashMap<Long, DlState>()
    /** Threads of the download engine (control thread + workers): at background priority while the browser is on screen (scrolling comes first), at normal priority otherwise (full speed with the screen off). */
    private val engineTids = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private fun threadPrio(): Int = if (app.kestrel.browser.ProcessLife.uiVisible) Process.THREAD_PRIORITY_BACKGROUND else Process.THREAD_PRIORITY_DEFAULT

    /** The browser came to the front / went away: re-prioritise every engine thread. */
    fun uiVisibilityChanged() {
        val p = threadPrio()
        for (tid in engineTids) try { Process.setThreadPriority(tid, p) } catch (e: Exception) { /* the thread has ended */ }
    }

    /** Another engine's fetcher (kind 1 = Firefox's network stack in Kestrel G). Kind 0 is always HttpURLConnection. */
    fun setFetcher(kind: Int, f: Fetcher) { fetchers[kind] = f }

    @Synchronized fun init(ctx: Context): Engine {
        engine?.let { return it }
        app = ctx.applicationContext
        val ht = HandlerThread("kestrel-downloads", threadPrio()).also { it.start(); engineTids.add(it.threadId) }
        handler = Handler(ht.looper)
        val control = Executor { r -> handler.post(r) }
        fetchers.putIfAbsent(0, UrlConnectionFetcher())
        refreshNet()
        val net = object : NetGate {
            override val online: Boolean get() = netOnline
            override val metered: Boolean get() = netMetered
        }
        val low = app.getSystemService(ActivityManager::class.java)?.isLowRamDevice != false
        val cfg = Config(maxConnsTotal = if (low) 3 else 6, maxConnsPerDownload = if (low) 3 else 4)
        val e = Engine(
            { fetchers[it] ?: fetchers[0] }, MediaStoreSinks(app), SqliteStore(app), net, control, Clock.System, cfg,
            Spawner { name, task ->
                Thread({
                    val tid = Process.myTid(); engineTids.add(tid)
                    try { Process.setThreadPriority(threadPrio()); task.run() } finally { engineTids.remove(tid) }
                }, name).also { it.isDaemon = true; it.start() }
            },
        )
        e.addListener(object : Engine.Listener { override fun onChanged(id: Long) { uiChanged() } })
        engine = e
        control.execute { e.restore(); uiChanged() }
        return e
    }

    // ---- commands (all asynchronous; the engine serialises them on its control thread)

    /** Queue a download and make sure the foreground service is running. Called from the UI thread; nothing here touches disk or network on it. */
    fun enqueue(ctx: Context, src: Source) {
        val e = init(ctx)
        handler.post { e.enqueue(src) } // after restore() on the same thread, so new ids never clash with stored ones
        DownloadService.start(ctx)
    }

    fun views(): List<DownloadView> = engine?.views() ?: emptyList()
    fun view(id: Long): DownloadView? = engine?.view(id)
    fun pause(id: Long) { engine?.pause(id) }
    fun resume(ctx: Context, id: Long) { init(ctx).resume(id); DownloadService.start(ctx) }
    fun cancel(id: Long) { engine?.cancel(id) }
    fun restart(ctx: Context, id: Long) { init(ctx).restart(id); DownloadService.start(ctx) }
    fun remove(id: Long, deleteFile: Boolean) { engine?.remove(id, deleteFile) }
    fun clearFinished() { engine?.clearFinished() }

    private val installerName = Regex("""^kestrel(-g)?[-_].*\.apk$""", RegexOption.IGNORE_CASE)

    /**
     * Kestrel's own installers that this app downloaded earlier (one per release from GitHub, about 92 MB each) and that still sit in Downloads: the easiest room to give back
     * when an update says "not enough storage". Only files the download manager itself knows are listed.
     */
    fun oldInstallers(): List<DownloadView> = views().filter { it.state == DlState.COMPLETED && installerName.matches(it.name) }

    /** Delete those files (and their entries); returns how many bytes they held. */
    fun deleteOldInstallers(): Long {
        val list = oldInstallers()
        var bytes = 0L
        for (v in list) { bytes += maxOf(v.total, v.done); engine?.remove(v.id, true) }
        return bytes
    }
    fun pauseAll() { engine?.pauseAll() }
    fun resumeAll(ctx: Context) { init(ctx).resumeAll(); DownloadService.start(ctx) }
    fun tick() { engine?.tick() }
    fun needsService(): Boolean = engine?.needsService() ?: false
    fun counts(): IntArray = engine?.counts() ?: intArrayOf(0, 0, 0)

    fun addListener(l: () -> Unit) { uiListeners.add(l) }
    fun removeListener(l: () -> Unit) { uiListeners.remove(l) }

    /** At most 4 UI updates a second, whatever the engine reports. Also raises the "finished" / "failed" notifications. */
    private fun uiChanged() {
        if (!dirty.compareAndSet(false, true)) return
        Io.main.postDelayed({
            dirty.set(false)
            val now = engine?.views() ?: emptyList()
            for (v in now) {
                val before = lastStates.put(v.id, v.state)
                if (before != null && before != v.state && (v.state == DlState.COMPLETED || v.state == DlState.FAILED)) Notifier.finished(app, v)
            }
            lastStates.keys.retainAll(now.map { it.id }.toSet())
            for (l in uiListeners) l()
        }, 250)
    }

    // ---- network (watched only while the service runs)

    fun startNetWatch() {
        if (netCallback != null) return
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { changed() }
            override fun onLost(network: Network) { changed() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { changed() }
            private fun changed() { refreshNet(); engine?.onNetwork() }
        }
        try { cm.registerDefaultNetworkCallback(cb, handler); netCallback = cb } catch (e: Exception) { /* the engine's own failure handling still works */ }
    }

    fun stopNetWatch() {
        val cb = netCallback ?: return
        netCallback = null
        try { app.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) } catch (e: Exception) { /* already gone */ }
    }

    private fun refreshNet() {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        netOnline = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // "validated" is only a guess on a hotspot: online unless there is no network at all
        netMetered = cm.isActiveNetworkMetered
    }
}
