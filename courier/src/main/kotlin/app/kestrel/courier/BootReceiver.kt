package app.kestrel.courier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a reboot: if the owner left "Ready for the tablet" on, listen again. Android may refuse to start a service from here; then the app simply waits to be opened. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!CourierPrefs(context).ready) return
        try { CourierService.start(context) } catch (e: Exception) { /* not allowed from the background on this phone */ }
    }
}
