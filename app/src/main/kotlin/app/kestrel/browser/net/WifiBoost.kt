package app.kestrel.browser.net

import android.content.Context
import android.net.wifi.WifiManager

/**
 * Network lab "Keep Wi-Fi awake while Kestrel is open" (research/REVIEW-EXTERNAL-1.md 4.1, EX-7): a hint, not a requirement. In 802.11 power save the tablet's radio sleeps between beacons and the
 * phone buffers frames for it, so a tap after a few seconds of reading starts from a sleeping radio. WIFI_MODE_FULL_HIGH_PERF is documented to keep Wi-Fi out of power save for low packet latency;
 * it is deprecated only from API 34 (where the system turns it into the low-latency lock, which works with the screen on and the app in front: exactly the case here). It costs battery, so it is
 * an opt-in switch, held only while the browser is on screen. The download service has its own lock (screen-off downloads).
 */
object WifiBoost {
    private var lock: WifiManager.WifiLock? = null

    /** Is the lock held right now (Diagnostics). */
    val held: Boolean get() = lock?.isHeld == true

    /** How many times it was taken since the process started (Diagnostics). */
    @Volatile var takes = 0
        private set

    @Suppress("DEPRECATION")
    fun set(ctx: Context, on: Boolean) {
        try {
            if (on) {
                val l = lock ?: (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                    ?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "kestrel:front")?.also { it.setReferenceCounted(false); lock = it }
                if (l != null && !l.isHeld) { l.acquire(); takes++ }
            } else {
                lock?.let { if (it.isHeld) it.release() }
            }
        } catch (e: Exception) { /* a hint: never worth a crash (no Wi-Fi service, or the permission is missing) */ }
    }

    fun line(): String = "Keep Wi-Fi awake in front: " + (if (held) "held now" else "not held") + ", taken $takes time(s) this run"
}
