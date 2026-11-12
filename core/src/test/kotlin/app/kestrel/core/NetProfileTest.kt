package app.kestrel.core

import app.kestrel.core.net.NetProfile
import app.kestrel.core.net.NetProfile.Input
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetProfileTest {
    @Test fun withoutAVpnTheOwnersSwitchesApplyExactly() {
        for (doh in listOf(true, false)) for (h3 in listOf(true, false)) {
            val o = NetProfile.effective(Input(dohOn = doh, http3On = h3, vpn = false))
            assertEquals("doh=$doh", if (doh) 2 else 0, o.trrMode)
            assertEquals("h3=$h3", h3, o.http3)
            assertFalse(o.fastWifi)
            assertTrue(o.text, o.text.startsWith("Standard profile"))
        }
    }

    @Test fun aVpnPausesDohAndHttp3WhateverTheOwnerChose() {
        for (doh in listOf(true, false)) for (h3 in listOf(true, false)) {
            val o = NetProfile.effective(Input(dohOn = doh, http3On = h3, vpn = true))
            assertEquals("the system resolver (the phone's, through the VPN)", 0, o.trrMode)
            assertFalse("no QUIC attempts", o.http3)
            assertTrue(o.fastWifi)
            assertTrue(o.text, o.text.contains("phone's own resolver") && o.text.contains("HTTP/3 is off"))
        }
    }

    @Test fun theCourierRestsWhileAVpnIsUpAndComesBackWithoutAnyReconfiguration() {
        val on = Input(dohOn = true, http3On = true, vpn = false, helperMode = 2, pagesViaPhone = true)
        assertEquals(2, NetProfile.effective(on).helperMode); assertTrue(NetProfile.effective(on).pagesViaProxy)
        val vpn = Input(dohOn = true, http3On = true, vpn = true, helperMode = 2, pagesViaPhone = true)
        assertEquals("no second relay on top of the VPN", 0, NetProfile.effective(vpn).helperMode); assertFalse(NetProfile.effective(vpn).pagesViaProxy)
        assertEquals("the owner's choice is untouched: the same input without the VPN gives the Courier back", 2, NetProfile.effective(on).helperMode)
    }

    @Test fun theNetworkLabSwitchKeepsTheCourierGoingUnderAVpnAndChangesNothingElse() {
        val rest = Input(dohOn = true, http3On = true, vpn = true, helperMode = 2, pagesViaPhone = true, fastTunnel = true)
        val keep = Input(dohOn = true, http3On = true, vpn = true, helperMode = 2, pagesViaPhone = true, fastTunnel = true, keepHelperUnderVpn = true)
        val r = NetProfile.effective(rest); val k = NetProfile.effective(keep)
        assertEquals(0, r.helperMode); assertFalse(r.pagesViaProxy); assertFalse(r.helperKept)
        assertEquals("the switch keeps the owner's helper mode", 2, k.helperMode); assertTrue("and pages through the phone", k.pagesViaProxy); assertTrue(k.helperKept)
        // everything else of the Fast Wi-Fi profile is the same either way
        assertEquals(r.trrMode, k.trrMode); assertEquals(r.http3, k.http3); assertEquals(r.disableIPv6, k.disableIPv6); assertEquals(r.pageYieldKBps, k.pageYieldKBps); assertEquals(r.tunnel, k.tunnel)
        assertTrue(k.text, k.text.contains("keeps working")); assertTrue(r.text, r.text.contains("rests"))
    }

    @Test fun theSwitchStillNeedsTheOwnersHelperChoicesAndMeansNothingWithoutAVpn() {
        val off = Input(dohOn = true, http3On = true, vpn = true, helperMode = 0, pagesViaPhone = true, keepHelperUnderVpn = true)
        assertEquals("the helper is off in the owner's own settings", 0, NetProfile.effective(off).helperMode); assertFalse(NetProfile.effective(off).pagesViaProxy)
        val downloadsOnly = Input(dohOn = true, http3On = true, vpn = true, helperMode = 1, pagesViaPhone = false, keepHelperUnderVpn = true)
        assertEquals(1, NetProfile.effective(downloadsOnly).helperMode); assertFalse("pages need their own switch", NetProfile.effective(downloadsOnly).pagesViaProxy)
        val plain = NetProfile.effective(Input(dohOn = true, http3On = true, vpn = false, helperMode = 2, pagesViaPhone = true, keepHelperUnderVpn = true))
        assertFalse("no VPN: the standard profile, nothing 'kept'", plain.helperKept); assertEquals(2, plain.helperMode)
        val clamped = NetProfile.effective(Input(true, true, true, helperMode = 9, keepHelperUnderVpn = true))
        assertEquals(2, clamped.helperMode)
    }

    @Test fun theCourierIsOffByDefaultAndPagesNeedTheHelperOn() {
        val d = Input(dohOn = true, http3On = true, vpn = false)
        assertEquals(0, NetProfile.effective(d).helperMode); assertFalse(NetProfile.effective(d).pagesViaProxy)
        assertFalse("pages switch alone does nothing", NetProfile.effective(Input(true, true, false, helperMode = 0, pagesViaPhone = true)).pagesViaProxy)
        assertFalse("helper alone (downloads only) does not send pages", NetProfile.effective(Input(true, true, false, helperMode = 1, pagesViaPhone = false)).pagesViaProxy)
        assertTrue(NetProfile.effective(Input(true, true, false, helperMode = 1, pagesViaPhone = true)).pagesViaProxy)
    }

    @Test fun outOfRangeHelperModesAreClamped() {
        assertEquals(0, NetProfile.effective(Input(true, true, false, helperMode = -5)).helperMode)
        assertEquals(2, NetProfile.effective(Input(true, true, false, helperMode = 99)).helperMode)
    }

    // ---- AB3: behind LinkMeter's own tunnel the link is the phone's connection, not the capped hotspot

    @Test fun onlyLinkMetersOwnTunnelGetsIpv6OffAndTheBiggerPageYield() {
        val other = NetProfile.effective(Input(dohOn = true, http3On = true, vpn = true, fastTunnel = false))
        assertFalse("another VPN: nothing is known about IPv6 on it", other.disableIPv6)
        assertEquals("another VPN keeps the careful 150 KB/s", NetProfile.PAGE_YIELD_KBPS, other.pageYieldKBps)
        assertFalse(other.tunnel)
        assertTrue(other.text, other.text.contains("a VPN is the active network"))
        val own = NetProfile.effective(Input(dohOn = true, http3On = true, vpn = true, fastTunnel = true))
        assertTrue("the tunnel blocks IPv6: never ask for it", own.disableIPv6)
        assertEquals(NetProfile.FAST_PAGE_YIELD_KBPS, own.pageYieldKBps)
        assertTrue(own.pageYieldKBps > 4 * NetProfile.PAGE_YIELD_KBPS)
        assertTrue(own.tunnel && own.fastWifi)
        assertTrue(own.text, own.text.contains("LinkMeter's tunnel") && own.text.contains("IPv6") && own.text.contains("1000 KB/s"))
    }

    @Test fun theTunnelKeepsEverythingTheGenericVpnProfileDoes() {
        val own = NetProfile.effective(Input(dohOn = true, http3On = true, vpn = true, helperMode = 2, pagesViaPhone = true, fastTunnel = true))
        assertEquals("the phone's resolver", 0, own.trrMode)
        assertFalse("no QUIC attempts", own.http3)
        assertEquals("the Courier rests", 0, own.helperMode)
        assertFalse(own.pagesViaProxy)
    }

    @Test fun withoutAVpnTheTunnelFlagChangesNothingAndTheStandardValuesStay() {
        for (doh in listOf(true, false)) for (h3 in listOf(true, false)) {
            val a = NetProfile.effective(Input(dohOn = doh, http3On = h3, vpn = false, fastTunnel = false))
            val b = NetProfile.effective(Input(dohOn = doh, http3On = h3, vpn = false, fastTunnel = true))
            assertEquals(a.trrMode, b.trrMode); assertEquals(a.http3, b.http3)
            assertFalse("IPv6 stays on the normal hotspot", b.disableIPv6)
            assertEquals(NetProfile.PAGE_YIELD_KBPS, b.pageYieldKBps)
            assertFalse(b.fastWifi); assertFalse(b.tunnel)
            assertTrue(b.text, b.text.startsWith("Standard profile"))
        }
    }

    @Test fun theTunnelIsRecognisedByItsFixedAddressesOnly() {
        assertTrue("its own address", NetProfile.isFastTunnel(listOf("10.111.222.2")))
        assertTrue("its DNS address alone", NetProfile.isFastTunnel(listOf("fe80::1", "10.111.222.53")))
        assertTrue("any entry of the list", NetProfile.isFastTunnel(listOf("192.168.43.1", null, "10.111.222.9")))
        assertFalse("the hotspot network", NetProfile.isFastTunnel(listOf("192.168.43.2", "192.168.43.1")))
        assertFalse("a neighbouring private network", NetProfile.isFastTunnel(listOf("10.111.221.2", "10.111.22.2", "10.111.223.2")))
        assertFalse("a longer third number is not the prefix", NetProfile.isFastTunnel(listOf("10.111.2222.1")))
        assertFalse("a prefix with no host part", NetProfile.isFastTunnel(listOf("10.111.222.")))
        assertFalse("IPv6 text", NetProfile.isFastTunnel(listOf("fe80::1", "2001:db8::1")))
        assertFalse("nothing at all", NetProfile.isFastTunnel(emptyList()))
        assertFalse("only unknowns", NetProfile.isFastTunnel(listOf(null, null)))
    }

    @Test fun theBiggerYieldNeverExceedsWhatThePageRuleCanUse() {
        // one number each, both positive, the fast one larger: the downloader clamps further (PageYieldTest)
        assertTrue(NetProfile.PAGE_YIELD_KBPS in 50..4000 && NetProfile.FAST_PAGE_YIELD_KBPS in 50..4000)
        assertTrue(NetProfile.FAST_PAGE_YIELD_KBPS > NetProfile.PAGE_YIELD_KBPS)
    }
}
