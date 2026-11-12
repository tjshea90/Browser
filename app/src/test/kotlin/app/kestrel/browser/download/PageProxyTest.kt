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

    @Test fun typingThePairingCodeNeverSwitchesAnythingOn() {
        val p = Prefs.get(ctx)
        PhoneBridge.forgetPairing(ctx)
        ctx.getSharedPreferences("kestrel", 0).edit().remove("phone_mode").remove("pages_via_phone").commit()
        assertEquals("OFF by default", PhoneMode.OFF, PhoneBridge.mode(ctx)); assertFalse(p.pagesViaPhone)
        assertTrue(PhoneBridge.savePairing(ctx, Pairing.format(byteArrayOf(9, 9, 9, 9, 9, 9, 9, 9, 9, 9))))
        assertEquals("pairing only stores the code", PhoneMode.OFF, PhoneBridge.mode(ctx))
        assertFalse(PageProxy.wanted(ctx))
        PhoneBridge.forgetPairing(ctx)
    }

    @Test fun theMenuSwitchTogglesTheWholeHelper() {
        val p = Prefs.get(ctx)
        p.phoneMode = PhoneMode.OFF.ordinal; p.pagesViaPhone = false
        assertTrue(PhoneBridge.toggleHelper(ctx)); assertEquals(PhoneMode.WHEN_THERE, PhoneBridge.mode(ctx)); assertTrue("everything comes on, pages included", p.pagesViaPhone)
        assertFalse(PhoneBridge.toggleHelper(ctx)); assertEquals(PhoneMode.OFF, PhoneBridge.mode(ctx)); assertFalse("and everything goes off, pages included", p.pagesViaPhone)
    }

    @Test fun oneSwitchTurnsEveryFeatureOffAndOn() {
        val p = Prefs.get(ctx)
        PhoneBridge.setMaster(ctx, true)
        assertTrue(PhoneBridge.masterOn(ctx)); assertTrue(PhoneBridge.pagesOn(ctx)); assertEquals(PhoneMode.WHEN_THERE, PhoneBridge.mode(ctx))
        p.phoneMode = PhoneMode.ALWAYS.ordinal
        PhoneBridge.setMaster(ctx, true)
        assertEquals("a stronger choice the owner made is kept when the switch is pressed on again", PhoneMode.ALWAYS, PhoneBridge.mode(ctx))
        PhoneBridge.setMaster(ctx, false)
        assertFalse(PhoneBridge.masterOn(ctx)); assertFalse(PhoneBridge.pagesOn(ctx)); assertFalse("Settings must not show pages on under an off helper", p.pagesViaPhone)
        assertEquals(PhoneMode.OFF, PhoneBridge.mode(ctx))
    }

    @Test fun aStalePagesChoiceUnderAnOffHelperNeverShowsOrActs() {
        val p = Prefs.get(ctx)
        PhoneBridge.forgetPairing(ctx)
        PhoneBridge.savePairing(ctx, Pairing.format(byteArrayOf(5, 5, 5, 5, 5, 5, 5, 5, 5, 5)))
        p.phoneMode = PhoneMode.OFF.ordinal; p.pagesViaPhone = true // what an older build left behind after the menu switch was turned off
        assertFalse(PhoneBridge.pagesOn(ctx)); assertFalse(PageProxy.wanted(ctx))
        assertTrue(PhoneBridge.toggleHelper(ctx)); assertTrue(PageProxy.wanted(ctx))
        assertFalse(PhoneBridge.toggleHelper(ctx)); assertFalse(PageProxy.wanted(ctx)); assertFalse(p.pagesViaPhone)
        PhoneBridge.forgetPairing(ctx)
    }

    @Test fun theDataSaverIsOffByDefaultSoNothingActsMetered() {
        val prefs = Prefs.get(ctx)                                           // a singleton: clear the very file it holds (an earlier test may have set a level)
        val sp = Prefs::class.java.getDeclaredField("sp").also { it.isAccessible = true }.get(prefs) as android.content.SharedPreferences
        sp.edit().remove("data_saver").commit()
        assertEquals(DataSaver.OFF, prefs.dataSaver)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class VpnProfileTest {
    private fun caps(vpn: Boolean): android.net.NetworkCapabilities {
        val c = org.robolectric.shadows.ShadowNetworkCapabilities.newInstance()
        val sc = org.robolectric.Shadows.shadowOf(c)
        sc.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET); sc.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        sc.addTransportType(if (vpn) android.net.NetworkCapabilities.TRANSPORT_VPN else android.net.NetworkCapabilities.TRANSPORT_WIFI)
        return c
    }

    @Test fun aVpnAsTheDefaultNetworkIsSeenAndForgottenAgain() {
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = false)); assertFalse(app.kestrel.browser.net.NetworkMonitor.vpn)
        var calls = 0
        val l: () -> Unit = { calls++ }
        app.kestrel.browser.net.NetworkMonitor.addListener(l)
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = true))
        assertTrue("Fast Wi-Fi (a VPN) is the active network", app.kestrel.browser.net.NetworkMonitor.vpn); assertEquals("listeners hear about it", 1, calls)
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = true)); assertEquals("no change, no call", 1, calls)
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = false))
        assertFalse(app.kestrel.browser.net.NetworkMonitor.vpn); assertEquals(2, calls)
        app.kestrel.browser.net.NetworkMonitor.removeListener(l)
    }

    @Test fun linkMetersOwnTunnelIsRecognisedFromTheLinkAddressesAndForgottenWithTheVpn() {
        val nm = app.kestrel.browser.net.NetworkMonitor
        nm.update(caps(vpn = false)); nm.updateLink(listOf("192.168.43.2", "192.168.43.1")); assertFalse(nm.vpn); assertFalse(nm.fastTunnel)
        var calls = 0
        val l: () -> Unit = { calls++ }
        nm.addListener(l)
        nm.update(caps(vpn = true))
        assertTrue(nm.vpn); assertFalse("the VPN's own link has not arrived yet: only a VPN so far", nm.fastTunnel); assertEquals(1, calls)
        nm.updateLink(listOf("10.111.222.2", "10.111.222.53"))
        assertTrue("its fixed addresses: LinkMeter's tunnel", nm.fastTunnel); assertEquals("listeners hear about the tunnel too", 2, calls)
        nm.updateLink(listOf("10.111.222.2")); assertEquals("no change, no call", 2, calls)
        nm.update(caps(vpn = false))
        assertFalse(nm.vpn); assertFalse("no VPN, no tunnel, whatever the old link said", nm.fastTunnel)
        nm.updateLink(listOf("192.168.43.2"))
        nm.update(caps(vpn = true)); nm.updateLink(listOf("10.8.0.2", "10.8.0.1"))
        assertTrue(nm.vpn); assertFalse("another VPN is not LinkMeter's", nm.fastTunnel)
        nm.update(null)
        assertFalse(nm.vpn); assertFalse(nm.fastTunnel)
        nm.removeListener(l)
        nm.update(caps(vpn = false)); nm.updateLink(null)
    }

    @Test fun theTunnelFlagReachesTheEngineProfileAndGoesAgain() {
        val nm = app.kestrel.browser.net.NetworkMonitor
        val ctx = RuntimeEnvironment.getApplication()
        nm.update(caps(vpn = true)); nm.updateLink(listOf("10.111.222.2"))
        val i = PhoneBridge.profileInput(ctx)
        assertTrue(i.vpn && i.fastTunnel)
        val o = app.kestrel.core.net.NetProfile.effective(i)
        assertTrue("IPv6 is blocked in the tunnel: the engine must not ask for it", o.disableIPv6)
        assertEquals(app.kestrel.core.net.NetProfile.FAST_PAGE_YIELD_KBPS, o.pageYieldKBps)
        nm.update(caps(vpn = false)); nm.updateLink(listOf("192.168.43.2"))
        val back = app.kestrel.core.net.NetProfile.effective(PhoneBridge.profileInput(ctx))
        assertFalse(back.disableIPv6); assertEquals(app.kestrel.core.net.NetProfile.PAGE_YIELD_KBPS, back.pageYieldKBps); assertFalse(back.fastWifi)
        nm.updateLink(null)
    }

    @Test fun theCourierRestsWhileTheVpnIsUpWhateverTheSettingsSay() {
        val ctx = RuntimeEnvironment.getApplication()
        val p = Prefs.get(ctx)
        p.phoneMode = PhoneMode.ALWAYS.ordinal; p.pagesViaPhone = true
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = false))
        assertEquals(PhoneMode.ALWAYS, PhoneBridge.effectiveMode(ctx))
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = true))
        assertEquals("no second relay on top of the VPN", PhoneMode.OFF, PhoneBridge.effectiveMode(ctx))
        assertEquals("the owner's own choice is kept", PhoneMode.ALWAYS, PhoneBridge.mode(ctx))
        assertFalse(PageProxy.wanted(ctx))
        app.kestrel.browser.net.NetworkMonitor.update(caps(vpn = false))
        assertEquals(PhoneMode.ALWAYS, PhoneBridge.effectiveMode(ctx))
        p.phoneMode = PhoneMode.OFF.ordinal; p.pagesViaPhone = false
    }
}
