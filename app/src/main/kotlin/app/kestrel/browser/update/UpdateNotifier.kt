package app.kestrel.browser.update

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.kestrel.browser.R
import app.kestrel.core.update.UpdateLogic
import app.kestrel.core.update.UpdateOffer

/**
 * Notifications about the app's own update (owner request 8): found, ready to install (the one that matters: on Android 10 the system's "Install?" screen can only be opened by a tap),
 * failed, and the install handshake. The download's progress is the download service's ongoing notification. Tapping any of them opens the app on the Updates sheet; "Install" also starts the install.
 */
@SuppressLint("NotificationPermission") // POST_NOTIFICATIONS is declared in the Kestrel G manifest; a denied permission just means no notification (caught below)
object UpdateNotifier {
    const val CH_UPDATE = "update_status"
    const val CH_READY = "update_ready"
    const val ID = 4712
    const val EXTRA_OPEN_UPDATES = "app.kestrel.browser.OPEN_UPDATES"
    const val EXTRA_INSTALL_UPDATE = "app.kestrel.browser.INSTALL_UPDATE"

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_UPDATE, "App update status", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        nm.createNotificationChannel(NotificationChannel(CH_READY, "App update ready to install", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun open(ctx: Context, install: Boolean): PendingIntent {
        val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            putExtra(EXTRA_OPEN_UPDATES, true)
            if (install) putExtra(EXTRA_INSTALL_UPDATE, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: Intent()
        return PendingIntent.getActivity(ctx, if (install) 11 else 10, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun post(ctx: Context, b: Notification.Builder) {
        try { ctx.getSystemService(NotificationManager::class.java)?.notify(ID, b.build()) } catch (e: Exception) { /* notifications may be switched off */ }
    }

    /** A new release exists and will not be downloaded by itself. */
    fun available(ctx: Context, o: UpdateOffer) {
        channels(ctx)
        post(ctx, Notification.Builder(ctx, CH_UPDATE).setSmallIcon(R.drawable.ic_update).setAutoCancel(true)
            .setContentTitle("Kestrel update ${o.versionName} is available")
            .setContentText("${UpdateLogic.formatSize(o.size)} - tap to download it")
            .setContentIntent(open(ctx, false)))
    }

    /** The file is downloaded and checked: one tap installs it. */
    fun ready(ctx: Context, o: UpdateOffer) {
        channels(ctx)
        post(ctx, Notification.Builder(ctx, CH_READY).setSmallIcon(R.drawable.ic_update).setAutoCancel(true).setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setContentTitle("Kestrel ${o.versionName} is ready to install")
            .setContentText("Tap to install. Android asks you to confirm.")
            .setContentIntent(open(ctx, true))
            .addAction(Notification.Action.Builder(null, "Install", open(ctx, true)).build()))
    }

    /** The system installer wants a tap and the app is not on screen: carry its confirmation screen in a notification. */
    fun confirm(ctx: Context, confirmIntent: Intent) {
        channels(ctx)
        val pi = PendingIntent.getActivity(ctx, 12, confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        post(ctx, Notification.Builder(ctx, CH_READY).setSmallIcon(R.drawable.ic_update).setAutoCancel(true)
            .setContentTitle("Finish installing the Kestrel update")
            .setContentText("Tap to open Android's installer")
            .setContentIntent(pi))
    }

    fun failed(ctx: Context, message: String) {
        channels(ctx)
        post(ctx, Notification.Builder(ctx, CH_UPDATE).setSmallIcon(R.drawable.ic_update).setAutoCancel(true)
            .setContentTitle("Kestrel update did not work")
            .setContentText(message.take(120))
            .setContentIntent(open(ctx, false)))
    }

    fun needsSpace(ctx: Context, shortBy: Long) {
        channels(ctx)
        post(ctx, Notification.Builder(ctx, CH_UPDATE).setSmallIcon(R.drawable.ic_update).setAutoCancel(true)
            .setContentTitle("Not enough free storage for the Kestrel update")
            .setContentText("Free up about ${UpdateLogic.formatSize(shortBy)} - tap for details")
            .setContentIntent(open(ctx, false)))
    }

    fun clear(ctx: Context) { try { ctx.getSystemService(NotificationManager::class.java)?.cancel(ID) } catch (e: Exception) { /* ignore */ } }
}
