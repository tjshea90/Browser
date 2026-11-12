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
import app.kestrel.browser.diag.Health
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.ProcessLife
import app.kestrel.core.download.Clock
import app.kestrel.core.download.Config
import app.kestrel.core.download.DEST_APP_PRIVATE
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Engine
import app.kestrel.core.download.FetchRequest
import app.kestrel.core.download.Fetcher
import app.kestrel.core.download.NetGate
import app.kestrel.core.download.RateLimiter
import app.kestrel.core.download.Source
import app.kestrel.core.download.Spawner
import app.kestrel.core.media.StreamEngine
import app.kestrel.core.media.StreamSource
import java.io.File
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
    /** The one speed limit of every download (Settings > Downloads); shared by both engines. */
    @Volatile private var limiter: RateLimiter? = null

    /** Settings: KB/s over all downloads together, 0 = no limit. Takes effect at once, also for downloads in progress. */
    fun setSpeedLimit(kbps: Int) { limiter?.bytesPerSec = kbps.coerceAtLeast(0) * 1024L }
    /** The second engine: segmented streams (HLS) into one file. Same list, same notifications; its ids live above 1e9 ([StreamEngine.owns]). */
    private var streams: StreamEngine? = null
    private var streamStore: StreamStoreFiles? = null
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
    private fun threadPrio(): Int = if (ProcessLife.uiVisible) Process.THREAD_PRIORITY_BACKGROUND else Process.THREAD_PRIORITY_DEFAULT

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
        val prefs = Prefs.get(app)
        val cfg = Config(maxActive = prefs.dlActive, maxConnsTotal = if (low) 3 else 6, maxConnsPerDownload = if (prefs.dlConns in 1..3) prefs.dlConns else if (low) 3 else 4)
        val lim = RateLimiter().also { it.bytesPerSec = prefs.dlLimitKBps * 1024L }
        limiter = lim
        val e = Engine(
            { fetchers[it] ?: fetchers[0] }, AppSinks(MediaStoreSinks(app), FileSinks(File(app.filesDir, "updates"))), SqliteStore(app), net, control, Clock.System, cfg,
            Spawner { name, task ->
                Thread({
                    val tid = Process.myTid(); engineTids.add(tid)
                    try { Process.setThreadPriority(threadPrio()); task.run() } finally { engineTids.remove(tid) }
                }, name).also { it.isDaemon = true; it.start() }
            },
            limiter = lim,
        )
        e.addListener(object : Engine.Listener { override fun onChanged(id: Long) { uiChanged() } })
        engine = e
        val store = StreamStoreFiles(File(app.filesDir, "streams"), File(app.cacheDir, "streams-private")).also { streamStore = it; it.wipePrivate() }
        val streamSinks = AppSinks(MediaStoreSinks(app), FileSinks(File(app.filesDir, "updates")))
        val st = StreamEngine(
            { fetchers[it] ?: fetchers[0] }, streamSinks, store, net, control, Clock.System, cfg,
            Spawner { name, task ->
                Thread({
                    val tid = Process.myTid(); engineTids.add(tid)
                    try { Process.setThreadPriority(threadPrio()); task.run() } finally { engineTids.remove(tid) }
                }, name).also { it.isDaemon = true; it.start() }
            },
            Remuxer(app, streamSinks),
            { msg, t -> android.util.Log.w(Io.TAG, msg, t); Health.log("stream", msg.take(140)) },
            limiter = lim,
        )
        st.addListener(object : StreamEngine.Listener { override fun onChanged(id: Long) { uiChanged() } })
        streams = st
        control.execute { e.restore(); st.restore(); e.resumeInterrupted(); st.resumeInterrupted(); uiChanged() } // what the last run was in the middle of carries on by itself
        return e
    }

    // ---- commands (all asynchronous; the engine serialises them on its control thread)

    /** Queue a download and make sure the foreground service is running. Called from the UI thread; nothing here touches disk or network on it. */
    fun enqueue(ctx: Context, src: Source, onId: ((Long) -> Unit)? = null) {
        val e = init(ctx)
        handler.post { val id = e.enqueue(src); onId?.invoke(id) } // after restore() on the same thread, so new ids never clash with stored ones
        DownloadService.start(ctx)
    }

    /** Queue a video stream (an HLS playlist the user picked a quality of) and make sure the foreground service is running. */
    fun enqueueStream(ctx: Context, src: StreamSource, onId: ((Long) -> Unit)? = null) {
        init(ctx)
        val s = streams ?: return
        handler.post { val id = s.enqueue(src); onId?.invoke(id) }
        DownloadService.start(ctx)
    }

    /**
     * Fetch a small text (a playlist) through the same network stack the downloads use (Firefox's, so the page's cookies apply) and hand it to [onDone] on the UI thread;
     * null = it could not be read. At most 4 MB.
     */
    fun fetchText(ctx: Context, url: String, headers: Map<String, String>, kind: Int, priv: Boolean, onDone: (String?) -> Unit) {
        init(ctx)
        Io.exec {
            var text: String? = null
            try {
                val f = fetchers[kind] ?: fetchers[0]!!
                val resp = f.open(FetchRequest(url, -1, -1, null, headers, 15_000, 20_000, priv))
                try {
                    if (resp.status in 200..299) {
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(16 * 1024)
                        val body = resp.body
                        while (body != null) { val n = body.read(buf); if (n < 0) break; out.write(buf, 0, n); if (out.size() > 4 * 1024 * 1024) { out.reset(); break } }
                        if (out.size() > 0) text = out.toString("UTF-8")
                    }
                } finally { try { resp.close() } catch (e: Exception) { /* ignore */ } }
            } catch (e: Exception) { /* null: the caller says so */ }
            Io.ui { onDone(text) }
        }
    }

    /** A byte range of a file (a media file's first bytes, its `moov` box), at most [to]-[from]+1 bytes, handed to [onDone] on the UI thread; null = it could not be read. */
    fun fetchBytes(ctx: Context, url: String, from: Long, to: Long, headers: Map<String, String>, kind: Int, priv: Boolean, onTotal: ((Long) -> Unit)? = null, onDone: (ByteArray?) -> Unit) {
        init(ctx)
        Io.exec {
            var data: ByteArray? = null
            var total = -1L
            try {
                val f = fetchers[kind] ?: fetchers[0]!!
                val resp = f.open(FetchRequest(url, from, to, null, headers, 15_000, 20_000, priv))
                try {
                    // the size of the whole file: the end of "Content-Range: bytes 0-0/48213", or the plain length of a reply that ignored Range
                    total = (if (resp.status == 206) resp.header("Content-Range")?.substringAfterLast('/')?.trim()?.toLongOrNull() else if (resp.status == 200) resp.header("Content-Length")?.trim()?.toLongOrNull() else null) ?: -1L
                    if (resp.status == 200 || resp.status == 206) {
                        val want = (to - from + 1).coerceAtMost(8L * 1024 * 1024).toInt()
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(16 * 1024)
                        val body = resp.body
                        var skip = if (resp.status == 200) from else 0L // a server that ignored Range sent the file from its start
                        while (body != null && out.size() < want) {
                            val n = body.read(buf); if (n < 0) break
                            var off = 0; var len = n
                            if (skip > 0) { val s = minOf(skip, len.toLong()).toInt(); skip -= s; off += s; len -= s }
                            if (len > 0) out.write(buf, off, minOf(len, want - out.size()))
                        }
                        if (out.size() > 0) data = out.toByteArray()
                    }
                } finally { try { resp.abort() } catch (e: Exception) { /* ignore */ }; try { resp.close() } catch (e: Exception) { /* ignore */ } }
            } catch (e: Exception) { /* null: the caller carries on without it */ }
            Io.ui { if (total > 0 && onTotal != null) onTotal(total); onDone(data) }
        }
    }

    private fun isStream(id: Long) = streams?.owns(id) == true

    /** The downloads the user sees (the Downloads screen, "Delete old installers"): not the app's own update file. */
    fun views(): List<DownloadView> = allViews().filter { it.kind != DEST_APP_PRIVATE }
    /** Everything the engines hold, update downloads included (the service notification, the updater). */
    fun allViews(): List<DownloadView> {
        // the notification, the Downloads screen and the updater all ask within the same tick: one snapshot (each is a pass over every download under the engine's lock) serves them all
        val now = android.os.SystemClock.uptimeMillis()
        val c = viewCache
        if (c != null && now - viewCacheAt < 150) return c
        val v = ((engine?.views() ?: emptyList()) + (streams?.views() ?: emptyList())).sortedByDescending { it.created }
        viewCache = v; viewCacheAt = now
        return v
    }
    @Volatile private var viewCache: List<DownloadView>? = null
    @Volatile private var viewCacheAt = 0L
    fun view(id: Long): DownloadView? = if (isStream(id)) streams?.view(id) else engine?.view(id)
    fun pause(id: Long) { if (isStream(id)) streams?.pause(id) else engine?.pause(id) }
    fun resume(ctx: Context, id: Long) { init(ctx); if (isStream(id)) streams?.resume(id) else engine?.resume(id); DownloadService.start(ctx) }
    fun cancel(id: Long) { if (isStream(id)) streams?.cancel(id) else engine?.cancel(id) }
    fun restart(ctx: Context, id: Long) { init(ctx); if (isStream(id)) streams?.restart(id) else engine?.restart(id); DownloadService.start(ctx) }
    fun remove(id: Long, deleteFile: Boolean) { if (isStream(id)) streams?.remove(id, deleteFile) else engine?.remove(id, deleteFile) }
    /** "Make MP4" on a finished stream that was kept as a .ts. */
    fun makeMp4(ctx: Context, id: Long) { init(ctx); if (isStream(id)) streams?.makeMp4(id); DownloadService.start(ctx) }   // the service keeps the process alive while the file is converted
    fun clearFinished() { engine?.clearFinished(); streams?.clearFinished() }

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
    fun pauseAll() { engine?.pauseAll(); streams?.pauseAll() }
    fun resumeAll(ctx: Context) { init(ctx).resumeAll(); streams?.resumeAll(); DownloadService.start(ctx) }
    fun tick() { engine?.tick() }
    fun needsService(): Boolean = (engine?.needsService() ?: false) || (streams?.needsService() ?: false)
    fun counts(): IntArray { val a = engine?.counts() ?: intArrayOf(0, 0, 0); val b = streams?.counts() ?: intArrayOf(0, 0, 0); return intArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2]) }

    fun addListener(l: () -> Unit) { uiListeners.add(l) }
    fun removeListener(l: () -> Unit) { uiListeners.remove(l) }

    /** At most 4 UI updates a second, whatever the engine reports. Also raises the "finished" / "failed" notifications. */
    private fun uiChanged() {
        if (!dirty.compareAndSet(false, true)) return
        Io.main.postDelayed({
            dirty.set(false)
            val now = allViews()
            for (v in now) {
                val before = lastStates.put(v.id, v.state)
                // the update file has its own notifications (UpdateNotifier)
                if (v.kind != DEST_APP_PRIVATE && before != null && before != v.state && (v.state == DlState.COMPLETED || v.state == DlState.FAILED)) Notifier.finished(app, v)
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
            private fun changed() { refreshNet(); engine?.onNetwork(); streams?.onNetwork() }
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
