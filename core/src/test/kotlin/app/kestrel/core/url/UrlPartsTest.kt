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
}
