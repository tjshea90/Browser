package app.kestrel.courier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

/** The foreground service that keeps the phone listening for the tablet (and downloading for it). One ongoing notification, a partial wake lock only while a download moves. */
class CourierService : Service() {
    private val ui = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private var lastText = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            CourierPrefs(this).ready = false
            stopSelf()
            return START_NOT_STICKY
        }
        enterForeground(text())
        CourierPrefs(this).ready = true
        CourierRuntime.start(this)
        ui.removeCallbacks(refresh)
        ui.post(refresh)
        return START_STICKY
    }

    private val refresh = object : Runnable {
        override fun run() {
            val t = text()
            if (t != lastText) { lastText = t; getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(t)) }
            val busy = CourierRuntime.busy()
            if (busy && wake?.isHeld != true) {
                wake = (getSystemService(PowerManager::class.java)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kestrelcourier:downloading").also { it.acquire(15 * 60_000L) }
            } else if (!busy) { wake?.let { if (it.isHeld) it.release() }; wake = null }
            else if (busy) { wake?.acquire(15 * 60_000L) }                 // renewed while the download goes on
            ui.postDelayed(this, 2000)
        }
    }

    private fun text(): String {
        CourierRuntime.lastError?.let { return it }
        val n = CourierRuntime.views().count { !it.state.terminal }
        return if (n == 0) "Ready for the tablet" else if (n == 1) "Downloading 1 file for the tablet" else "Downloading $n files for the tablet"
    }

    private fun enterForeground(text: String) {
        val n = notification(text)
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, n)
    }

    private fun notification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Kestrel Courier", NotificationManager.IMPORTANCE_LOW).apply { description = "Shown while the phone is ready to download for the tablet"; setShowBadge(false) })
        val open = PendingIntent.getActivity(this, 0, Intent(this, CourierActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val off = PendingIntent.getService(this, 1, Intent(this, CourierService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat).setContentTitle("Kestrel Courier").setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "Turn off", off).build())
        if (Build.VERSION.SDK_INT >= 31) b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        return b.build()
    }

    override fun onDestroy() {
        ui.removeCallbacks(refresh)
        wake?.let { if (it.isHeld) it.release() }
        CourierRuntime.stop(this)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "courier"
        const val NOTIFICATION_ID = 7
        const val ACTION_STOP = "app.kestrel.courier.STOP"

        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, CourierService::class.java)) }
        fun stop(ctx: Context) { CourierPrefs(ctx).ready = false; ctx.stopService(Intent(ctx, CourierService::class.java)) }
    }
}
