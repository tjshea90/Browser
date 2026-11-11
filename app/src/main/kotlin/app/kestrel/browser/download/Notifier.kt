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
import app.kestrel.core.download.DEST_APP_PRIVATE
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

    // PendingIntents are built once and reused: each build is several calls into system services, and the ongoing notification used to rebuild them at every refresh.
    @Volatile private var openAppPi: PendingIntent? = null
    private val actionPis = java.util.concurrent.ConcurrentHashMap<String, PendingIntent>()

    private fun openApp(ctx: Context): PendingIntent {
        openAppPi?.let { return it }
        val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            putExtra(EXTRA_OPEN_DOWNLOADS, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: Intent()
        return PendingIntent.getActivity(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE).also { openAppPi = it }
    }

    private fun serviceAction(ctx: Context, action: String, code: Int, id: Long = 0L): PendingIntent =
        actionPis.getOrPut("$action|$code|$id") { PendingIntent.getService(ctx, code, Intent(ctx, DownloadService::class.java).setAction(action).putExtra(DownloadService.EXTRA_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }

    private fun title(v: DownloadView) = if (v.kind == DEST_APP_PRIVATE) "Downloading the Kestrel update" else v.name

    /** What the ongoing notification says. Two equal models need no new post: the service compares them before it builds anything. */
    data class ProgressModel(val title: String, val text: String, val percent: Int, val action: Int, val id: Long, val canPause: Boolean) {
        companion object { const val NONE = 0; const val ONE = 1; const val PAUSE_ALL = 2; const val RESUME_ALL = 3; const val INDETERMINATE = -1; const val NO_BAR = -2 }
    }

    fun progressModel(views: List<DownloadView>): ProgressModel {
        val running = views.filter { it.state == DlState.RUNNING || it.state == DlState.STARTING }
        val waiting = views.filter { it.state == DlState.QUEUED || it.state == DlState.WAITING_NETWORK }
        val paused = views.filter { it.state == DlState.PAUSED_USER || it.state == DlState.PAUSED_SYSTEM }
        return when {
            running.isNotEmpty() -> {
                val priv = running.all { it.priv }
                val total = running.filter { it.total > 0 }.sumOf { it.total }
                val done = running.filter { it.total > 0 }.sumOf { it.done }
                val speed = running.sumOf { it.bytesPerSec }
                val pct = if (total > 0) (done * 100 / total).toInt() else -1
                val text = buildString {
                    if (total > 0) append(pct).append(" %")
                    if (speed > 0) append(if (isEmpty()) "" else "  ·  ").append(Format.speed(speed))
                    if (waiting.isNotEmpty()) append("  ·  ").append(waiting.size).append(" waiting")
                }
                val title = if (priv) "Downloading (private)" else if (running.size == 1) title(running[0]) else "Downloading ${running.size} files"
                val one = if (running.size == 1) running[0] else null
                ProgressModel(title, text.ifEmpty { "Starting…" }, if (total > 0) pct else ProgressModel.INDETERMINATE, if (one != null) ProgressModel.ONE else if (running.any { it.actions.contains(Action.PAUSE) }) ProgressModel.PAUSE_ALL else ProgressModel.NONE,
                    one?.id ?: 0L, one?.actions?.contains(Action.PAUSE) == true)
            }
            waiting.isNotEmpty() -> ProgressModel("Downloads waiting", if (waiting.any { it.state == DlState.WAITING_NETWORK }) "Waiting for the connection…" else "${waiting.size} in line", ProgressModel.NO_BAR, ProgressModel.NONE, 0L, false)
            else -> ProgressModel("Downloads paused", if (paused.isEmpty()) "Nothing to do" else "${paused.size} paused", ProgressModel.NO_BAR, if (paused.isNotEmpty()) ProgressModel.RESUME_ALL else ProgressModel.NONE, 0L, false)
        }
    }

    /** The ongoing notification of the foreground service. */
    fun progress(ctx: Context, views: List<DownloadView>): Notification = progress(ctx, progressModel(views))

    fun progress(ctx: Context, m: ProgressModel): Notification {
        val b = Notification.Builder(ctx, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(openApp(ctx))
            .setContentTitle(m.title).setContentText(m.text)
        when {
            m.percent >= 0 -> b.setProgress(100, m.percent, false)
            m.percent == ProgressModel.INDETERMINATE -> b.setProgress(0, 0, true)
        }
        when (m.action) {
            ProgressModel.ONE -> {
                if (m.canPause) b.addAction(Notification.Action.Builder(null, "Pause", serviceAction(ctx, DownloadService.ACTION_PAUSE_ID, 5, m.id)).build())
                b.addAction(Notification.Action.Builder(null, "Cancel", serviceAction(ctx, DownloadService.ACTION_CANCEL_ID, 6, m.id)).build())
            }
            ProgressModel.PAUSE_ALL -> b.addAction(Notification.Action.Builder(null, "Pause all", serviceAction(ctx, DownloadService.ACTION_PAUSE_ALL, 2)).build())
            ProgressModel.RESUME_ALL -> b.addAction(Notification.Action.Builder(null, "Resume all", serviceAction(ctx, DownloadService.ACTION_RESUME_ALL, 3)).build())
        }
        return b.build()
    }

    /** One per finished or failed file; tap opens the Downloads screen, "Open" opens the file. */
    @android.annotation.SuppressLint("NotificationPermission") // POST_NOTIFICATIONS is declared in the Kestrel G manifest; a denied permission just means no notification (caught below)
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
        if (!ok && !v.priv) b.addAction(Notification.Action.Builder(null, "Retry", serviceAction(ctx, DownloadService.ACTION_RESUME_ID, 7 + (v.id and 0xff).toInt(), v.id)).build())
        val uri = v.destUri
        if (ok && uri != null && !v.priv) {
            val open = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), v.mime ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            b.addAction(Notification.Action.Builder(null, "Open", PendingIntent.getActivity(ctx, (v.id and 0xffff).toInt() + 100, open, PendingIntent.FLAG_IMMUTABLE)).build())
        }
        try { nm.notify((v.id and 0x7fffffff).toInt() + 5000, b.build()) } catch (e: Exception) { /* notifications may be switched off */ }
    }
}
