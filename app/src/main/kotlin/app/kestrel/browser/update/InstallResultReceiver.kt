package app.kestrel.browser.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/** Android's PackageInstaller reports back here: the confirmation screen to show, success, or why the installation failed. Not exported; only our own PendingIntent reaches it. */
class InstallResultReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // The system's "Do you want to update this app?" screen. The user just tapped Install, so this app is in front and may start it.
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) {
                    // On screen: open it at once. In the background (an automatic install attempt): a screen cannot be started from there on Android 10+, so the notification carries it.
                    if (app.kestrel.browser.ProcessLife.uiVisible) try { ctx.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { UpdateManager.installFailed(ctx, "the installer screen could not be opened") }
                    else UpdateNotifier.confirm(ctx, confirm)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> { UpdateNotifier.clear(ctx) /* the app is replaced by Android; UpdateManager.init() tidies the old file on the next start */ }
            PackageInstaller.STATUS_FAILURE_ABORTED -> UpdateManager.installCancelled(ctx)
            PackageInstaller.STATUS_FAILURE_STORAGE -> UpdateManager.installNeedsSpace(ctx)
            PackageInstaller.STATUS_FAILURE_BLOCKED -> UpdateManager.installFailed(ctx, "Android or a security setting on this tablet blocked the installation")
            PackageInstaller.STATUS_FAILURE_CONFLICT -> UpdateManager.installFailed(ctx, "it conflicts with the app that is installed (usually a different signing key)")
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> UpdateManager.installFailed(ctx, "this version is not compatible with this tablet")
            PackageInstaller.STATUS_FAILURE_INVALID -> UpdateManager.installFailed(ctx, "the downloaded file is not a valid app; delete it and download it again")
            else -> UpdateManager.installFailed(ctx, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }
}
