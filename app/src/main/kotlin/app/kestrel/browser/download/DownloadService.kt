package app.kestrel.browser.download

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Build
import android.os.PowerManager
import android.net.wifi.WifiManager
import android.app.NotificationManager

/**
 * Keeps the process (and so the downloads) alive while anything is queued or moving: a foreground service of type dataSync with one quiet notification.
 * It owns nothing: the engine lives in [DownloadController]. It ticks the engine once a second, refreshes the notification, and stops itself when idle.
 * The service entry, FOREGROUND_SERVICE(_DATA_SYNC) and POST_NOTIFICATIONS are declared in the Kestrel G manifest (the only build with the download manager); lint on the
 * shared source tree of the WebView build cannot see that, hence the suppressions.
 */
@SuppressLint("NotificationPermission", "ForegroundServiceType")
class DownloadService : Service() {
    companion object {
        const val ACTION_PAUSE_ALL = "app.kestrel.browser.download.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "app.kestrel.browser.download.RESUME_ALL"
        const val ACTION_PAUSE_ID = "app.kestrel.browser.download.PAUSE_ID"
        const val ACTION_RESUME_ID = "app.kestrel.browser.download.RESUME_ID"
        const val ACTION_CANCEL_ID = "app.kestrel.browser.download.CANCEL_ID"
        const val EXTRA_ID = "id"

        fun start(ctx: Context) {
            try { ctx.startForegroundService(Intent(ctx, DownloadService::class.java)) }
            catch (e: Exception) { /* a background start can be refused on Android 12+: the downloads stay queued until the app is opened */ }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var idleTicks = 0
    private var wake: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null
    private val listener: () -> Unit = { refresh() }

    private val tick = object : Runnable {
        override fun run() {
            DownloadController.tick()
            if (DownloadController.needsService()) idleTicks = 0 else idleTicks++
            keepAwake(DownloadController.counts()[0] > 0)
            refresh()
            if (idleTicks >= 3) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); app.kestrel.browser.ProcessLife.maybeExit(); return }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.channels(this)
        DownloadController.init(this)
        DownloadController.startNetWatch()
        DownloadController.addListener(listener)
        startForeground(Notifier.ID_PROGRESS, Notifier.progress(this, DownloadController.allViews()), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        handler.postDelayed(tick, 1000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // every startForegroundService() call must be answered with startForeground() (also when the service is already running)
        startForeground(Notifier.ID_PROGRESS, Notifier.progress(this, DownloadController.allViews()), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        when (intent?.action) {
            ACTION_PAUSE_ALL -> DownloadController.pauseAll()
            ACTION_RESUME_ALL -> DownloadController.resumeAll(this)
            ACTION_PAUSE_ID -> DownloadController.pause(intent.getLongExtra(EXTRA_ID, 0L))
            ACTION_RESUME_ID -> DownloadController.resume(this, intent.getLongExtra(EXTRA_ID, 0L))
            ACTION_CANCEL_ID -> DownloadController.cancel(intent.getLongExtra(EXTRA_ID, 0L))
        }
        idleTicks = 0
        return START_STICKY
    }

    private fun refresh() {
        try { getSystemService(NotificationManager::class.java)?.notify(Notifier.ID_PROGRESS, Notifier.progress(this, DownloadController.allViews())) } catch (e: Exception) { /* ignore */ }
    }

    /**
     * Only while bytes move: a partial wake lock (10 minute timeout, renewed every tick) keeps the CPU running with the screen off, and a Wi-Fi lock keeps the radio from sleeping, so a download
     * runs at full speed on the lock screen. Both are released the moment nothing is moving; the screen never stays on.
     */
    private fun keepAwake(on: Boolean) {
        if (on) {
            if (wake == null) wake = getSystemService(PowerManager::class.java)?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kestrel:download")?.apply { setReferenceCounted(false) }
            wake?.acquire(10 * 60 * 1000L)
            if (wifi == null) wifi = try {
                @Suppress("DEPRECATION") // HIGH_PERF is deprecated from API 29 (the system then treats it as the basic lock); LOW_LATENCY only works with the screen on
                (applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.createWifiLock(if (Build.VERSION.SDK_INT >= 29) WifiManager.WIFI_MODE_FULL else WifiManager.WIFI_MODE_FULL_HIGH_PERF, "kestrel:download")?.apply { setReferenceCounted(false) }
            } catch (e: Exception) { null }
            if (wifi?.isHeld == false) try { wifi?.acquire() } catch (e: Exception) { /* no Wi-Fi service */ }
        } else {
            if (wake?.isHeld == true) wake?.release()
            if (wifi?.isHeld == true) try { wifi?.release() } catch (e: Exception) { /* ignore */ }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        DownloadController.removeListener(listener)
        DownloadController.stopNetWatch()
        if (wake?.isHeld == true) wake?.release()
        if (wifi?.isHeld == true) try { wifi?.release() } catch (e: Exception) { /* ignore */ }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
