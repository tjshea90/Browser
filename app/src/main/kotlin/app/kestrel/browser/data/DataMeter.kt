package app.kestrel.browser.data

import android.net.TrafficStats
import android.os.Process

/**
 * "How much of the hotspot's data has this browser used?" Reads the app UID's cumulative byte counters (which include the
 * WebView network service and the private-window process) and accumulates deltas, so a reboot (counters restart at 0)
 * does not lose the total. Returns -1 when the kernel does not expose per-UID stats.
 */
object DataMeter {
    fun totalBytes(prefs: Prefs): Long {
        val uid = Process.myUid()
        val rx = TrafficStats.getUidRxBytes(uid)
        val tx = TrafficStats.getUidTxBytes(uid)
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return -1L
        val now = rx + tx
        val last = prefs.dataBaseline
        val acc = prefs.dataAccumulated + (if (now >= last) now - last else now)
        prefs.dataBaseline = now
        prefs.dataAccumulated = acc
        return acc
    }

    fun reset(prefs: Prefs) {
        val uid = Process.myUid()
        val now = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid)
        prefs.dataBaseline = if (now > 0) now else 0
        prefs.dataAccumulated = 0
    }
}
