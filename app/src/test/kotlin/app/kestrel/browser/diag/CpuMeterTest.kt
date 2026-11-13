package app.kestrel.browser.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CpuMeterTest {
    @Test fun processNamesAreShownShort() {
        assertEquals("parent", CpuMeter.short("app.kestrel.browser.gecko"))
        assertEquals("tab", CpuMeter.short("app.kestrel.browser.gecko.debug:sandboxed_tab_disable_art_image_39"))
        assertEquals("crashhelper", CpuMeter.short("app.kestrel.browser.gecko.debug:crashhelper_disable_art_image_"))
        assertEquals("gpu", CpuMeter.short("app.kestrel.browser.gecko:gpu"))
        assertEquals("socket", CpuMeter.short("app.kestrel.browser.gecko:socket"))
    }

    @Test fun theLineSaysSoBeforeAnyReading() {
        val l = CpuMeter.line()
        assertTrue(l, l.startsWith("CPU by process, % of ONE core") && l.contains("no reading yet"))
    }
}
