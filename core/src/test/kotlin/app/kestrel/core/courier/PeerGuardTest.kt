package app.kestrel.core.courier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class PeerGuardTest {
    private fun ip(s: String): ByteArray = InetAddress.getByName(s).address            // literals only: no DNS
    private fun local(s: String, prefix: Int, name: String) = LocalAddr(ip(s), prefix, name)

    @Test fun privateRanges() {
        for (s in listOf("10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.0.1", "192.168.43.1", "169.254.1.1", "fd00::1", "fc00::1", "fe80::1", "febf::1", "172.20.10.1")) assertTrue(s, PeerGuard.isPrivate(ip(s)))
        for (s in listOf("172.15.255.255", "172.32.0.1", "192.169.0.1", "100.64.0.1", "100.127.255.255", "8.8.8.8", "1.1.1.1", "0.0.0.0", "fec0::1", "2001:db8::1", "2607:f8b0::1", "::")) assertFalse(s, PeerGuard.isPrivate(ip(s)))
    }

    @Test fun loopback() {
        for (s in listOf("127.0.0.1", "127.9.9.9", "::1")) assertTrue(s, PeerGuard.isLoopback(ip(s)))
        for (s in listOf("192.168.1.1", "::2", "126.0.0.1")) assertFalse(s, PeerGuard.isLoopback(ip(s)))
    }

    @Test fun mappedAddressesAreJudgedAsIpv4() {
        val mapped = ByteArray(16).also { it[10] = 0xFF.toByte(); it[11] = 0xFF.toByte(); it[12] = 192.toByte(); it[13] = 168.toByte(); it[14] = 43; it[15] = 5 }
        assertTrue(PeerGuard.isPrivate(mapped))
        assertTrue(PeerGuard.allowed(mapped, listOf(local("192.168.43.1", 24, "ap0"))))
        val mappedPublic = ByteArray(16).also { it[10] = 0xFF.toByte(); it[11] = 0xFF.toByte(); it[12] = 8; it[13] = 8; it[14] = 8; it[15] = 8 }
        assertFalse(PeerGuard.allowed(mappedPublic, listOf(local("192.168.43.1", 24, "ap0"))))
    }

    @Test fun subnets() {
        assertTrue(PeerGuard.sameSubnet(ip("192.168.43.5"), ip("192.168.43.1"), 24))
        assertFalse(PeerGuard.sameSubnet(ip("192.168.44.5"), ip("192.168.43.1"), 24))
        assertTrue(PeerGuard.sameSubnet(ip("192.168.44.5"), ip("192.168.43.1"), 23).not())           // 43 and 44 are not in one /23
        assertTrue(PeerGuard.sameSubnet(ip("192.168.43.200"), ip("192.168.42.1"), 23))
        assertTrue(PeerGuard.sameSubnet(ip("10.9.9.9"), ip("192.168.1.1"), 0))
        assertTrue(PeerGuard.sameSubnet(ip("10.0.0.7"), ip("10.0.0.7"), 32)); assertFalse(PeerGuard.sameSubnet(ip("10.0.0.8"), ip("10.0.0.7"), 32))
        assertFalse(PeerGuard.sameSubnet(ip("10.0.0.7"), ip("fe80::1"), 8)); assertFalse(PeerGuard.sameSubnet(ip("10.0.0.7"), ip("10.0.0.7"), 33)); assertFalse(PeerGuard.sameSubnet(ip("10.0.0.7"), ip("10.0.0.7"), -1))
        assertTrue(PeerGuard.sameSubnet(ip("fe80::1234"), ip("fe80::1"), 64)); assertFalse(PeerGuard.sameSubnet(ip("fe80:0:0:1::1"), ip("fe80::1"), 64))
    }

    @Test fun cellularInterfaceNames() {
        for (n in listOf("rmnet_data0", "rmnet0", "ccmni1", "wwan0", "v4-rmnet_data2", "clat4", "ppp0", "tun0")) assertTrue(n, PeerGuard.cellular(n))
        for (n in listOf("ap0", "swlan0", "wlan0", "wlan1", "rndis0", "bt-pan", "eth0")) assertFalse(n, PeerGuard.cellular(n))
    }

    @Test fun whoMayConnect() {
        val hotspot = listOf(local("192.168.43.1", 24, "ap0"), local("10.123.45.67", 8, "rmnet_data0"), local("fe80::1", 64, "ap0"))
        assertTrue("this phone", PeerGuard.allowed(ip("127.0.0.1"), emptyList()))
        assertTrue("the tablet on the hotspot", PeerGuard.allowed(ip("192.168.43.77"), hotspot))
        assertTrue("link-local on the hotspot", PeerGuard.allowed(ip("fe80::abcd"), hotspot))
        assertFalse("same /24, other number", PeerGuard.allowed(ip("192.168.44.77"), hotspot))
        assertFalse("the carrier's private space is behind the cellular interface", PeerGuard.allowed(ip("10.99.1.1"), hotspot))
        assertFalse("public addresses never", PeerGuard.allowed(ip("8.8.8.8"), hotspot))
        assertFalse("public IPv6 never", PeerGuard.allowed(ip("2607:f8b0::5"), hotspot))
        assertFalse("carrier-grade NAT never", PeerGuard.allowed(ip("100.64.1.2"), listOf(local("100.64.1.1", 10, "wlan1"))))
        assertFalse("no interfaces known yet", PeerGuard.allowed(ip("192.168.43.77"), emptyList()))
        assertFalse("a v4 peer against a v6-only interface list", PeerGuard.allowed(ip("192.168.43.77"), listOf(local("fe80::1", 64, "ap0"))))
        assertEquals(true, PeerGuard.allowed(ip("172.20.10.2"), listOf(local("172.20.10.1", 28, "bridge100"))))
    }
}

class PeerGuardLocalTest {
    private fun ip(s: String): ByteArray = java.net.InetAddress.getByName(s).address
    private fun local(s: String, prefix: Int, name: String) = LocalAddr(ip(s), prefix, name)

    @org.junit.Test fun theAddressTheTabletConnectedToDecides() {
        val ap = listOf(local("192.168.43.1", 24, "ap0"), local("10.123.45.67", 8, "rmnet_data0"))
        fun ok(peer: String, to: String, l: List<LocalAddr> = ap) = PeerGuard.allowed(ip(peer), ip(to), l)
        org.junit.Assert.assertTrue("the tablet on the hotspot", ok("192.168.43.77", "192.168.43.1"))
        org.junit.Assert.assertTrue("this phone itself", ok("127.0.0.1", "127.0.0.1"))
        org.junit.Assert.assertFalse("another /24", ok("192.168.44.77", "192.168.43.1"))
        org.junit.Assert.assertFalse("came in on the cellular address", ok("10.99.1.1", "10.123.45.67"))
        org.junit.Assert.assertFalse("a public address on either side", ok("8.8.8.8", "192.168.43.1"))
        org.junit.Assert.assertFalse(ok("192.168.43.77", "100.64.1.2"))
        org.junit.Assert.assertFalse(ok("192.168.43.77", "8.8.4.4"))
        org.junit.Assert.assertFalse("mixed families", ok("fe80::2", "192.168.43.1"))
    }

    @org.junit.Test fun whenAndroidHidesTheInterfacesTheSocketsOwnAddressIsTrustedWithinTheSameSubnet() {
        val none = emptyList<LocalAddr>()
        org.junit.Assert.assertTrue(PeerGuard.allowed(ip("192.168.43.77"), ip("192.168.43.1"), none))
        org.junit.Assert.assertTrue(PeerGuard.allowed(ip("172.20.10.2"), ip("172.20.10.1"), none))
        org.junit.Assert.assertFalse(PeerGuard.allowed(ip("192.168.44.77"), ip("192.168.43.1"), none))
        org.junit.Assert.assertFalse(PeerGuard.allowed(ip("10.0.0.5"), ip("10.0.1.5"), none))
        org.junit.Assert.assertTrue(PeerGuard.allowed(ip("fe80::2"), ip("fe80::1"), none))
        org.junit.Assert.assertFalse(PeerGuard.allowed(ip("fe80:0:0:1::2"), ip("fe80::1"), none))
    }
}
