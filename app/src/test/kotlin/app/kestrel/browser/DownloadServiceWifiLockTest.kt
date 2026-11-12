package app.kestrel.browser

import android.net.wifi.WifiManager
import app.kestrel.browser.download.DownloadService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** EX-1 (research/REVIEW-EXTERNAL-1.md 2.1): a download in the background holds a Wi-Fi lock that WORKS on Android 10. */
class DownloadServiceWifiLockTest {
    @Test @Suppress("DEPRECATION") fun theLockIsTheHighPerformanceOneNotTheBasicOneThatDoesNothingFromApi29() {
        assertEquals(WifiManager.WIFI_MODE_FULL_HIGH_PERF, DownloadService.WIFI_LOCK_MODE)
        assertNotEquals("WIFI_MODE_FULL is documented as non-functional since API 29", WifiManager.WIFI_MODE_FULL, DownloadService.WIFI_LOCK_MODE)
    }

    @Test fun diagnosticsSaysWhichLockIsUsedAndWhetherItIsHeld() {
        val line = DownloadService.wifiLockLine()
        assertTrue(line, line.startsWith("Wi-Fi lock: HIGH_PERF") && line.contains("held now=") && line.contains("taken "))
    }
}
