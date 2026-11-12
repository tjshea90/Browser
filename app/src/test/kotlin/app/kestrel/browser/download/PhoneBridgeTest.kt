package app.kestrel.browser.download

import android.app.Application
import org.robolectric.RuntimeEnvironment
import app.kestrel.browser.data.Prefs
import app.kestrel.core.courier.LinkReport
import app.kestrel.core.courier.Pairing
import app.kestrel.core.courier.PhoneMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PhoneBridgeTest {
    private val ctx: Application get() = RuntimeEnvironment.getApplication()

    @Test fun theModeDefaultsToOffAndFollowsTheSetting() {
        val prefs = Prefs.get(ctx)
        prefs.phoneMode = 0
        assertEquals(PhoneMode.OFF, PhoneBridge.mode(ctx))
        prefs.phoneMode = 1; assertEquals(PhoneMode.WHEN_THERE, PhoneBridge.mode(ctx))
        prefs.phoneMode = 2; assertEquals(PhoneMode.ALWAYS, PhoneBridge.mode(ctx))
        prefs.phoneMode = 9; assertEquals("out of range values are clamped", PhoneMode.ALWAYS, PhoneBridge.mode(ctx))
        prefs.phoneMode = 0
    }

    @Test fun aTypedCodeIsKeptInItsCleanFormOrRefused() {
        PhoneBridge.forgetPairing(ctx)
        assertFalse(PhoneBridge.paired(ctx))
        assertFalse(PhoneBridge.savePairing(ctx, "hello")); assertFalse(PhoneBridge.savePairing(ctx, "")); assertFalse(PhoneBridge.savePairing(ctx, "ABCD-EFGH-JKLM-NPQ0"))
        assertFalse("nothing was stored", PhoneBridge.paired(ctx))
        val code = Pairing.format(byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0))
        assertTrue(PhoneBridge.savePairing(ctx, "  " + code.lowercase().replace("-", " ") + " "))
        assertTrue(PhoneBridge.paired(ctx)); assertEquals(code, Prefs.get(ctx).phoneCode)
        PhoneBridge.forgetPairing(ctx)
        assertFalse(PhoneBridge.paired(ctx)); assertEquals("", Prefs.get(ctx).phoneCode)
    }

    @Test fun withoutAPairingOrWifiThereIsNoClient() {
        PhoneBridge.forgetPairing(ctx)
        assertNull(PhoneBridge.clientFor(ctx))
        PhoneBridge.savePairing(ctx, Pairing.format(ByteArray(10) { 3 }))
        assertNull("no Wi-Fi in the test: nowhere to look for the phone", PhoneBridge.clientFor(ctx))
        PhoneBridge.forgetPairing(ctx)
    }

    @Test fun theStatusLineTellsTheOwnerWhatToDoNext() {
        PhoneBridge.forgetPairing(ctx)
        assertTrue(PhoneBridge.statusLine(ctx).startsWith("Not paired"))
        PhoneBridge.savePairing(ctx, Pairing.format(ByteArray(10) { 5 }))
        assertTrue(PhoneBridge.statusLine(ctx), PhoneBridge.statusLine(ctx).contains("hotspot"))
        PhoneBridge.forgetPairing(ctx)
    }

    @Test fun theReportReadsAsPlainLines() {
        val text = PhoneBridge.describe(LinkReport(20_000_000, 8_000_000, 0, "Worth it: test.", listOf("tablet internet: dns")))
        assertTrue(text, text.contains("Tablet <-> phone (wifi): 20.0 MB/s"))
        assertTrue(text.contains("Phone's internet: 8.0 MB/s")); assertTrue(text.contains("This tablet's internet: not measured")); assertTrue(text.contains("Worth it: test.")); assertTrue(text.contains("tablet internet: dns"))
    }

    @Test fun diagnosticsHaveAPhoneLine() {
        val l = PhoneBridge.diagLines(ctx)
        assertEquals(2, l.size); assertTrue(l[0], l[0].startsWith("Pages: ")); assertTrue(l[1], l[1].startsWith("Phone link: mode=")); assertNotNull(PhoneBridge.FIRST_ID)
    }
}
