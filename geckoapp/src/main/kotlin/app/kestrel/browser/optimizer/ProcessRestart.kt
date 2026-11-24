package app.kestrel.browser.optimizer

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process

/** Restarts the whole app (new engine process) and comes back to [target]. A notification is posted first as the way back if the system ever refuses the automatic start; the new process removes it. */
object ProcessRestart {
    private const val CHANNEL = "optimizer"; const val NOTE_ID = 7301

    fun restartInto(ctx: Context, target: Intent) {
        postFallback(ctx, target)
        val i = Intent(ctx, RestartActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("target", target).putExtra("pid", Process.myPid())
        ctx.startActivity(i)
        app.kestrel.browser.Io.main.postDelayed({ Process.killProcess(Process.myPid()) }, 400)
    }

    fun clearFallback(ctx: Context) { try { ctx.getSystemService(NotificationManager::class.java)?.cancel(NOTE_ID) } catch (e: Exception) { /* none */ } }

    private fun postFallback(ctx: Context, target: Intent) {
        try {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Auto-tune", NotificationManager.IMPORTANCE_LOW))
            val pi = PendingIntent.getActivity(ctx, 1, Intent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = android.app.Notification.Builder(ctx, CHANNEL).setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Auto-tune is restarting Kestrel").setContentText("If it does not come back by itself, tap here.").setContentIntent(pi).setAutoCancel(true).build()
            nm.notify(NOTE_ID, n)
        } catch (e: Exception) { /* the automatic start is the main way */ }
    }
}
