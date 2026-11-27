package app.kestrel.browser

import android.annotation.SuppressLint
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
import android.os.SystemClock
import app.kestrel.browser.diag.Health
import app.kestrel.core.mem.KeepReady

/**
 * KR1 (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"; research/DIAG_2026-11-27.md section 2.3). While the app is away, this one quiet foreground service keeps
 * the process out of Android's "cached" class, so the memory killer of this Android 10 Go tablet takes other things first and the page is still there when the person comes back (the diagnostics show
 * the engine's content process killed 7 times in 17 hours, always while the app was away, and a 25 to 30 second reload of claude.ai after each).
 *
 * It does nothing else: no network, no timers except the one that ends it, no wake lock. It ends when the app comes back to the front ([stop], called from onStart), after the time the person chose
 * (Settings > Appearance > Keep the page ready when I leave), when the task is swiped away (the manifest keeps the default `stopWithTask`: Android stops the service, then [onDestroy] lets the process
 * go), or by the notification's "Let go" button. The notification is silent, has no status-bar icon (importance MIN), says nothing about any page and is hidden on the lock screen.
 */
@SuppressLint("NotificationPermission")
class KeepReadyService : Service() {
    companion object {
        const val ACTION_LET_GO = "app.kestrel.browser.KEEP_READY_LET_GO"
        private const val EXTRA_MINUTES = "minutes"
        private const val CHANNEL = "keep_ready"
        private const val ID = 4713

        /** Why the last hold ended, set by whoever ends it; empty when Android ended it (the task was swiped away, or the process was killed). */
        @Volatile private var endWhy = ""
        /** A start was asked for and no stop since: a quick leave-and-return can ask for the stop before the system has created the service (then [ProcessLife.keepReady] is still false), and it must still be stopped. */
        @Volatile private var requested = false

        fun start(ctx: Context, minutes: Int) {
            try { endWhy = ""; requested = true; ctx.startForegroundService(Intent(ctx, KeepReadyService::class.java).putExtra(EXTRA_MINUTES, minutes)) }
            catch (e: Exception) { requested = false; Health.count("keepready.refused"); Health.log("life", "keep-ready-refused", e.javaClass.simpleName) }   // Android 12 and newer can refuse a start from the background: the app is then simply cached like before
        }

        /** The app is back in front (or the person turned the choice off): the hold is no longer needed. */
        fun stop(ctx: Context, why: String = "returned") {
            if (!requested && !ProcessLife.keepReady) return
            requested = false
            endWhy = why
            try { ctx.stopService(Intent(ctx, KeepReadyService::class.java)) } catch (e: Exception) { /* already gone */ }
        }

        fun line(ctx: Context, choice: Int): String =
            "Keep the page ready when the app is left (KR1): " + KeepReady.words(choice) + "; held " + Health.counter("keepready.start") + " time(s) this run, ended by: returning " + Health.counter("keepready.end.returned") + ", the time " +
                Health.counter("keepready.end.time") + ", Let go " + Health.counter("keepready.end.letgo") + ", Android (task swiped away) " + Health.counter("keepready.end.removed") + "; refused by Android " + Health.counter("keepready.refused") +
                "; held right now: " + ProcessLife.keepReady + "; engine kills while the app was away " + Health.counter("gecko.kill.away") + " (" + Health.counter("gecko.kill.away.held") + " of them while held), in front " + Health.counter("gecko.kill.front")
    }

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { endWhy = "time"; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    private var startedAt = 0L

    override fun onCreate() {
        super.onCreate()
        startedAt = SystemClock.elapsedRealtime()
        ProcessLife.keepReady = true
        Health.count("keepready.start")
        if (!promoteSafely(60)) return
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_LET_GO) { endWhy = "letgo"; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        val minutes = (intent?.getIntExtra(EXTRA_MINUTES, 60) ?: 60).coerceIn(1, 24 * 60)
        if (!promoteSafely(minutes)) return START_NOT_STICKY   // every startForegroundService() call must be answered with startForeground(), also when the service already runs
        handler.removeCallbacks(timeout); handler.postDelayed(timeout, minutes * 60_000L)
        Health.log("life", "keep-ready", "minutes=$minutes")
        return START_NOT_STICKY                             // never brought back by Android after a kill: a hold only makes sense while the page is still in this process
    }

    /** Go foreground; if Android or the notification system refuses, say so and stop: a hold that cannot start must never take the browser down with it. */
    private fun promoteSafely(minutes: Int): Boolean = try { promote(minutes); true } catch (e: Throwable) {
        endWhy = "refused"; Health.count("keepready.refused"); Health.log("life", "keep-ready-refused", "promote " + e.javaClass.simpleName)
        try { stopSelf() } catch (x: Throwable) { /* gone */ }
        false
    }

    private fun promote(minutes: Int) {
        val n = notification(minutes)
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID, n)
    }

    private fun notification(minutes: Int): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(NotificationChannel(CHANNEL, "Keeping the page ready", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false); lockscreenVisibility = Notification.VISIBILITY_SECRET })
        val open = packageManager.getLaunchIntentForPackage(packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) } ?: Intent()
        val letGo = PendingIntent.getService(this, 7, Intent(this, KeepReadyService::class.java).setAction(ACTION_LET_GO), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tabs)
            .setOngoing(true).setShowWhen(false).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_SECRET)
            .setContentTitle("Kestrel is keeping your page ready")
            .setContentText("For up to " + (if (minutes >= 120) "${minutes / 60} hours" else if (minutes == 60) "1 hour" else "$minutes minutes") + ". Tap to open.")
            .setContentIntent(PendingIntent.getActivity(this, 8, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .addAction(Notification.Action.Builder(null, "Let go", letGo).build())
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        val why = endWhy.ifEmpty { "removed" }
        endWhy = ""
        ProcessLife.keepReady = false; requested = false
        Health.count("keepready.end.$why")
        Health.log("life", "keep-ready-end", "why=$why held=${(SystemClock.elapsedRealtime() - startedAt) / 1000}s")
        super.onDestroy()
        if (!ProcessLife.uiAlive) ProcessLife.maybeExit()      // nothing is on screen and nothing needs the process any more: the app is closed (a swipe, or the time ran out)
    }
}
