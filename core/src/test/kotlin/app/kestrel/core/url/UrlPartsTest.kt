package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPartsTest {
    @Test
    fun parsesAllComponents() {
        val p = UrlParts.parse("https://Example.COM:8443/a/b?x=1&y=2#frag")!!
        assertEquals("https", p.scheme)
        assertEquals("example.com", p.host)
        assertEquals(8443, p.port)
        assertEquals("/a/b", p.path)
        assertEquals("x=1&y=2", p.query)
        assertEquals("frag", p.fragment)
        assertEquals("https://example.com:8443/a/b?x=1&y=2#frag", p.toUrl())
    }

    @Test
    fun userInfoCanNeverBeMistakenForHost() {
        val p = UrlParts.parse("http://good.com@evil.com/login")!!
        assertEquals("evil.com", p.host)
        assertEquals("good.com", p.userInfo)
        assertEquals("host.example", UrlParts.parse("https://user:pw@host.example/")!!.host)
        assertEquals("user:pw", UrlParts.parse("https://user:pw@host.example/")!!.userInfo)
    }

    @Test
    fun trailingDotsAreStrippedSoTheyCannotDodgeHostRules() {
        assertEquals("ads.example.com", UrlParts.parse("https://ads.example.com./x")!!.host)
        assertEquals("ads.example.com", UrlParts.parse("https://ads.example.com.../x")!!.host)
    }

    @Test
    fun ipv6() {
        val p = UrlParts.parse("https://[2001:db8::1]:8080/p")!!
        assertEquals("2001:db8::1", p.host)
        assertEquals(8080, p.port)
        assertEquals("[2001:db8::1]", p.hostForAuthority)
        assertTrue(p.isIpLiteral)
        assertEquals("https://[2001:db8::1]:8080/p", p.toUrl())
    }

    @Test
    fun missingPieces() {
        val a = UrlParts.parse("https://example.com")!!
        assertEquals("", a.path); assertNull(a.query); assertNull(a.fragment)
        val b = UrlParts.parse("https://example.com?x=1")!!
        assertEquals("", b.path); assertEquals("x=1", b.query)
        val c = UrlParts.parse("https://example.com#f")!!
        assertEquals("", c.path); assertEquals("f", c.fragment)
        assertEquals(-1, UrlParts.parse("http://host:/x")!!.port) // empty port tolerated
    }

    @Test
    fun schemeAndHostAreCaseInsensitive() {
        val p = UrlParts.parse("HTTP://EXAMPLE.com/")!!
        assertEquals("http", p.scheme)
        assertEquals("example.com", p.host)
        assertTrue(p.isHttp)
    }

    @Test
    fun rejectsNonHierarchicalAndMalformed() {
        for (bad in listOf(
            "example.com", "mailto:a@b.c", "about:blank", "https://", "http://:80/", "http://host:99999/",
            "http://host:12ab/", "http://[::1/", "/relative/path", "", "://x", "1http://x", "http://a:b:c/",
        )) {
            assertNull("should reject: $bad", UrlParts.parse(bad))
        }
    }

    @Test
    fun hostOfShortcut() {
        assertEquals("a.example", UrlParts.hostOf("https://A.example/x"))
        assertNull(UrlParts.hostOf("about:blank"))
    }

    @Test
    fun ipv4Detection() {
        assertTrue(NetHosts.isIpv4("192.168.1.1"))
        assertTrue(NetHosts.isIpv4("0.0.0.0"))
        for (bad in listOf("256.1.1.1", "1.2.3", "1.2.3.4.5", "a.b.c.d", "", "1..2.3", "1.2.3.", ".1.2.3")) {
            assertFalse("not ipv4: $bad", NetHosts.isIpv4(bad))
        }
    }

    @Test
    fun localHostClassification() {
        for (h in listOf(
            "localhost", "foo.localhost", "printer.local", "nas", "192.168.43.1", "10.1.2.3", "172.16.0.1", "172.31.255.1",
            "100.64.1.1", "127.0.0.1", "169.254.1.1", "::1", "fe80::1", "fd12::1", "router.lan", "x.home.arpa", "",
        )) assertTrue("local: $h", NetHosts.isLocal(h))
        for (h in listOf("example.com", "8.8.8.8", "172.32.0.1", "100.128.0.1", "2001:db8::1", "a.b.co.uk", "172.15.0.1")) {
            assertFalse("public: $h", NetHosts.isLocal(h))
        }
        assertNotNull(UrlParts.parse("http://192.168.43.1/"))
    }

    @Test fun aBackslashEndsTheAuthorityOfAWebAddressLikeTheEngineReadsIt() {
        // RVW1 S9: "https://evil.com\\@good.com/" is the site evil.com to the engine
        assertEquals("evil.com", UrlParts.parse("https://evil.com\\@good.com/")!!.host)
        assertEquals("evil.com", UrlParts.parse("http://evil.com\\path")!!.host)
        assertEquals("good.com", UrlParts.parse("https://user@good.com/a\\b")!!.host)
        assertEquals("a backslash later in the path changes nothing", "good.com", UrlParts.parse("https://good.com/a\\@evil.com")!!.host)
        assertEquals("another scheme keeps its own rules", "evil.com\\@good.com", UrlParts.parse("foo://evil.com\\@good.com/")!!.let { it.userInfo?.plus("@")?.plus(it.host) ?: it.host })
    }

    @Test fun aPublicPageCannotMakeTheAppProbeAddressesOnTheOwnersNetwork() {
        // RVW1 S3
        assertTrue(NetHosts.probeAllowed("cdn.example.com", "site.example"))
        assertTrue(NetHosts.probeAllowed("cdn.example.com", ""))
        for (h in listOf("192.168.1.1", "10.0.0.5", "172.16.4.4", "127.0.0.1", "169.254.169.254", "localhost", "nas.local", "router", "100.64.0.7", "[::1]".trim('[', ']'), "fd12::1"))
            assertFalse("$h from a public page", NetHosts.probeAllowed(h, "site.example"))
        assertFalse("no page known counts as public", NetHosts.probeAllowed("192.168.1.1", ""))
        assertTrue("a page on the router itself may name its own files", NetHosts.probeAllowed("192.168.1.1", "192.168.1.1"))
        assertTrue("a NAS page may name NAS files", NetHosts.probeAllowed("nas.local", "media.local"))
    }

    @Test fun everySpellingOfAnAddressOnTheOwnersNetworkIsRefusedFromAPublicPage() {
        // RVW3 SC2: the string classifier read these as public
        val local = listOf(
            "::ffff:192.168.1.1", "::ffff:c0a8:101", "::ffff:7f00:1", "::ffff:10.0.0.5", "[::ffff:192.168.1.1]", "::FFFF:C0A8:0101",
            "64:ff9b::c0a8:101", "64:ff9b::10.0.0.1", "64:ff9b:1::1",
            "::127.0.0.1", "::7f00:1", "::192.168.0.1",
            "2002:c0a8:101::1", "2002:7f00:1::",
            "fec0::1", "fe90::1", "febf::1", "fe80::1%wlan0", "fc00::1", "fdff::1", "ff02::1", "::", "::1", "[::1]", "0:0:0:0:0:0:0:1",
            "127.1", "0x7f.0.0.1", "0177.0.0.1", "2130706433", "0x7f000001", "192.168.1", "192.168.257", "10.1", "172.16.5", "0xa.0.0.5", "169.254.169.254",
            "100.64.0.1", "224.0.0.1", "255.255.255.255", "0", "0.0.0.0", "localhost.", "printer.local", "nas",
            "not:an:ipv6", "1:2:3", "::g", "1::2::3", ":1::2", "12345::1",           // an IPv6-looking literal that does not parse: closed
        )
        for (h in local) {
            assertTrue("reaches the local network: $h", NetHosts.reachesLocalNetwork(h))
            assertFalse("refused from a public page: $h", NetHosts.probeAllowed(h, "site.example"))
        }
        val publicHosts = listOf(
            "8.8.8.8", "1.1.1.1", "1.1", "11.0.0.1", "172.32.0.1", "172.15.255.255", "100.128.0.1", "100.63.255.255", "193.168.1.1", "223.255.255.255",
            "example.com", "cdn.example.com", "123.example.com", "1e3.com", "0x.com", "0xabc.com", "a.b.co.uk", "999.1.1.1", "1.2.3.4.5.example",
            "2001:db8::1", "2606:4700:4700::1111", "::ffff:8.8.8.8", "::ffff:808:808", "64:ff9b::808:808", "2002:808:808::1", "2a00:1450:4001::1",
        )
        for (h in publicHosts) {
            assertFalse("a public host: $h", NetHosts.reachesLocalNetwork(h))
            assertTrue("allowed from a public page: $h", NetHosts.probeAllowed(h, "site.example"))
        }
        assertTrue("a page on the router itself may still name a mapped spelling of its own network", NetHosts.probeAllowed("::ffff:192.168.1.1", "192.168.1.1"))
        assertFalse("and a public page may not", NetHosts.probeAllowed("::ffff:192.168.1.1", "site.example"))
    }

    @Test fun theAddressBarAndTheHttpsPolicyKeepTheirOwnStricterReading() {
        // RVW3 SC2: isLocal did not change meaning ("10.5" is still not an address to it)
        for (h in listOf("10.5", "127.1", "192.168.1", "::ffff:192.168.1.1", "0x7f.0.0.1")) assertFalse("isLocal is strict: $h", NetHosts.isLocal(h))
        for (h in listOf("10.1.2.3", "::1", "fe80::1", "localhost", "nas")) assertTrue("isLocal: $h", NetHosts.isLocal(h))
    }
}
