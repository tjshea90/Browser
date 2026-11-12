package app.kestrel.browser.download

import android.app.Application
import app.kestrel.browser.data.Prefs
import app.kestrel.core.courier.Pairing
import app.kestrel.core.courier.PhoneMode
import app.kestrel.core.shield.DataSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PageProxyTest {
    private val ctx: Application get() = RuntimeEnvironment.getApplication()

    @Test fun theAutoConfigSendsOnlyHttpsAndSecureSocketsToThePhoneWithADirectFallback() {
        val pac = PageProxy.pac("192.168.43.1", 8781)
        assertTrue(pac, pac.contains("return \"PROXY 192.168.43.1:8781; DIRECT\";"))
        assertTrue("plain http stays direct", pac.contains("p !== \"https:\" && url.substring(0, 4) !== \"wss:\") return \"DIRECT\""))
        assertTrue("the local network stays direct", pac.contains("192\\.168\\.") && pac.contains("127\\.") && pac.contains("169\\.254\\."))
        assertTrue("single-label names stay direct", pac.contains("host.indexOf(\".\") < 0"))
        assertTrue("the regex end anchor survived the Kotlin string", pac.contains("localdomain)$/.test(host)"))
    }

    @Test fun theDataUrlDecodesBackToTheSameScript() {
        val url = PageProxy.pacUrl("192.168.43.1", 8781)
        assertTrue(url.startsWith("data:application/x-ns-proxy-autoconfig;charset=utf-8,"))
        assertFalse("no raw spaces, newlines or hashes in a URL", url.any { it == ' ' || it == '\n' || it == '#' })
        assertEquals(PageProxy.pac("192.168.43.1", 8781), URLDecoder.decode(url.substringAfter(','), "UTF-8"))
    }

    @Test fun pagesGoThroughThePhoneOnlyWhileTheHelperIsOnAndAPhoneIsPaired() {
        val p = Prefs.get(ctx)
        PhoneBridge.forgetPairing(ctx)
        p.phoneMode = PhoneMode.WHEN_THERE.ordinal; p.pagesViaPhone = true
        assertFalse("nothing paired", PageProxy.wanted(ctx))
        PhoneBridge.savePairing(ctx, Pairing.format(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        assertTrue(PageProxy.wanted(ctx))
        p.pagesViaPhone = false; assertFalse("the pages switch", PageProxy.wanted(ctx))
        p.pagesViaPhone = true; p.phoneMode = PhoneMode.OFF.ordinal; assertFalse("the helper switch turns pages off too", PageProxy.wanted(ctx))
        p.phoneMode = PhoneMode.ALWAYS.ordinal; assertTrue(PageProxy.wanted(ctx))
        PhoneBridge.forgetPairing(ctx)
    }

    @Test fun typingThePairingCodeSwitchesTheHelperOnOnlyIfTheOwnerNeverChoseAMode() {
        val p = Prefs.get(ctx)
        PhoneBridge.forgetPairing(ctx)
        // a fresh install: no choice stored -> pairing means "use my phone"
        ctx.getSharedPreferences("kestrel", 0).edit().remove("phone_mode").apply()
        if (!p.hasPhoneModeChoice()) {
            assertTrue(PhoneBridge.savePairing(ctx, Pairing.format(byteArrayOf(9, 9, 9, 9, 9, 9, 9, 9, 9, 9))))
            assertEquals(PhoneMode.WHEN_THERE, PhoneBridge.mode(ctx))
        }
        // an owner who switched it off keeps it off when they pair again
        p.phoneMode = PhoneMode.OFF.ordinal
        assertTrue(PhoneBridge.savePairing(ctx, Pairing.format(byteArrayOf(8, 8, 8, 8, 8, 8, 8, 8, 8, 8))))
        assertEquals(PhoneMode.OFF, PhoneBridge.mode(ctx))
        PhoneBridge.forgetPairing(ctx)
    }

    @Test fun theMenuSwitchTogglesTheWholeHelper() {
        val p = Prefs.get(ctx)
        p.phoneMode = PhoneMode.OFF.ordinal
        assertTrue(PhoneBridge.toggleHelper(ctx)); assertEquals(PhoneMode.WHEN_THERE, PhoneBridge.mode(ctx))
        assertFalse(PhoneBridge.toggleHelper(ctx)); assertEquals(PhoneMode.OFF, PhoneBridge.mode(ctx))
    }

    @Test fun theDataSaverIsOffByDefaultSoNothingActsMetered() {
        val prefs = Prefs.get(ctx)                                           // a singleton: clear the very file it holds (an earlier test may have set a level)
        val sp = Prefs::class.java.getDeclaredField("sp").also { it.isAccessible = true }.get(prefs) as android.content.SharedPreferences
        sp.edit().remove("data_saver").commit()
        assertEquals(DataSaver.OFF, prefs.dataSaver)
    }
}
