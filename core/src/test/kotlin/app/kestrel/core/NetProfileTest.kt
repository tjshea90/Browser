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
}
