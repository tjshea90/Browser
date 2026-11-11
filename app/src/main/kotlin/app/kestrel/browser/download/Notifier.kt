package app.kestrel.browser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.kestrel.browser.R
import app.kestrel.core.download.Action
import app.kestrel.core.download.DlState
import app.kestrel.core.download.DownloadView
import app.kestrel.core.download.Format

/** Notifications: one quiet ongoing one while anything downloads (progress + Pause all / Resume all), one per finished or failed file. Private downloads are named generically. */
object Notifier {
    const val CH_PROGRESS = "dl_progress"
    const val CH_DONE = "dl_done"
    const val ID_PROGRESS = 4711
    const val EXTRA_OPEN_DOWNLOADS = "app.kestrel.browser.OPEN_DOWNLOADS"

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_PROGRESS, "Downloads in progress", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        nm.createNotificationChannel(NotificationChannel(CH_DONE, "Finished downloads", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun openApp(ctx: Context): PendingIntent {
        val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            putExtra(EXTRA_OPEN_DOWNLOADS, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: Intent()
        return PendingIntent.getActivity(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun serviceAction(ctx: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getService(ctx, code, Intent(ctx, DownloadService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** The ongoing notification of the foreground service. */
    fun progress(ctx: Context, views: List<DownloadView>): Notification {
        val running = views.filter { it.state == DlState.RUNNING || it.state == DlState.STARTING }
        val waiting = views.filter { it.state == DlState.QUEUED || it.state == DlState.WAITING_NETWORK }
        val paused = views.filter { it.state == DlState.PAUSED_USER || it.state == DlState.PAUSED_SYSTEM }
        val b = Notification.Builder(ctx, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(openApp(ctx))
        when {
            running.isNotEmpty() -> {
                val priv = running.all { it.priv }
                val total = running.filter { it.total > 0 }.sumOf { it.total }
                val done = running.filter { it.total > 0 }.sumOf { it.done }
                val speed = running.sumOf { it.bytesPerSec }
                b.setContentTitle(if (priv) "Downloading (private)" else if (running.size == 1) running[0].name else "Downloading ${running.size} files")
                val text = buildString {
                    if (total > 0) append((done * 100 / total).toInt()).append(" %")
                    if (speed > 0) append(if (isEmpty()) "" else "  ·  ").append(Format.speed(speed))
                    if (waiting.isNotEmpty()) append("  ·  ").append(waiting.size).append(" waiting")
                }
                b.setContentText(text.ifEmpty { "Starting…" })
                if (total > 0) b.setProgress(100, (done * 100 / total).toInt(), false) else b.setProgress(0, 0, true)
                if (running.any { it.actions.contains(Action.PAUSE) }) b.addAction(Notification.Action.Builder(null, "Pause all", serviceAction(ctx, DownloadService.ACTION_PAUSE_ALL, 2)).build())
            }
            waiting.isNotEmpty() -> {
                b.setContentTitle("Downloads waiting")
                b.setContentText(if (waiting.any { it.state == DlState.WAITING_NETWORK }) "Waiting for the connection…" else "${waiting.size} in line")
            }
            else -> {
                b.setContentTitle("Downloads paused")
                b.setContentText(if (paused.isEmpty()) "Nothing to do" else "${paused.size} paused")
                if (paused.isNotEmpty()) b.addAction(Notification.Action.Builder(null, "Resume all", serviceAction(ctx, DownloadService.ACTION_RESUME_ALL, 3)).build())
            }
        }
        return b.build()
    }

    /** One per finished or failed file; tap opens the Downloads screen, "Open" opens the file. */
    fun finished(ctx: Context, v: DownloadView) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        channels(ctx)
        val ok = v.state == DlState.COMPLETED
        val b = Notification.Builder(ctx, CH_DONE)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(if (ok) "Download complete" else "Download failed")
            .setContentText(if (v.priv) "A private download" else v.name + if (!ok) "  ·  " + Format.failure(v) else "")
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
        val uri = v.destUri
        if (ok && uri != null && !v.priv) {
            val open = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), v.mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            b.addAction(Notification.Action.Builder(null, "Open", PendingIntent.getActivity(ctx, (v.id and 0xffff).toInt() + 100, open, PendingIntent.FLAG_IMMUTABLE)).build())
        }
        try { nm.notify((v.id and 0x7fffffff).toInt() + 5000, b.build()) } catch (e: Exception) { /* notifications may be switched off */ }
    }
}
