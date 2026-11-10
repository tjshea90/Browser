package app.kestrel.core.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PslAndTypeTest {
    private val psl = Psl.FALLBACK

    @Test
    fun registrableDomains() {
        assertEquals("example.com", psl.siteOf("example.com"))
        assertEquals("example.com", psl.siteOf("a.b.example.com"))
        assertEquals("example.co.uk", psl.siteOf("www.example.co.uk"))
        assertEquals("example.co.uk", psl.siteOf("a.b.example.co.uk"))
        assertEquals("foo.github.io", psl.siteOf("a.foo.github.io")) // private suffix: separate parties
        assertEquals("localhost", psl.siteOf("localhost"))
        assertEquals("192.168.1.1", psl.siteOf("192.168.1.1"))
        assertEquals("2001:db8::1", psl.siteOf("2001:db8::1"))
        assertNull("a public suffix has no registrable domain", psl.siteOf("co.uk"))
    }

    @Test
    fun sameSite() {
        assertTrue(psl.isSameSite("www.example.com", "cdn.example.com"))
        assertTrue(psl.isSameSite("example.com", "example.com"))
        assertFalse(psl.isSameSite("example.com", "example.org"))
        assertFalse("different github.io projects are different parties", psl.isSameSite("a.github.io", "b.github.io"))
        assertFalse(psl.isSameSite("a.co.uk", "b.co.uk"))
    }

    @Test
    fun wildcardAndExceptionRules() {
        val p = Psl.parse(sequenceOf("// comment", "", "com", "*.ck", "!www.ck", "uk", "co.uk"))
        assertEquals("a.b.ck", p.siteOf("a.b.ck")) // "*.ck": b.ck is a public suffix
        assertNull(p.siteOf("b.ck"))
        assertEquals("www.ck", p.siteOf("www.ck")) // exception: www.ck is registrable
        assertEquals("www.ck", p.siteOf("x.www.ck")) // deeper hosts collapse onto the exception's registrable domain
        assertEquals("example.co.uk", p.siteOf("a.example.co.uk"))
    }

    @Test
    fun idnRulesAreMatchedInPunycode() {
        // A two-label IDN suffix: with the rule the suffix is "café.example" (2 labels); without it, just "example".
        val p = Psl.parse(sequenceOf("example", "café.example"))
        val label = java.net.IDN.toASCII("café")
        assertEquals("b.$label.example", p.siteOf("a.b.$label.example"))
        assertNull("the IDN suffix itself is not registrable", p.siteOf("$label.example"))
    }

    @Test
    fun requestTypeNames() {
        assertEquals(RequestType.SCRIPT, RequestType.fromName("script"))
        assertEquals(RequestType.XHR, RequestType.fromName("xmlhttprequest"))
        assertEquals(RequestType.XHR, RequestType.fromName("fetch"))
        assertEquals(RequestType.SUBDOCUMENT, RequestType.fromName("frame"))
        assertEquals(RequestType.POPUP, RequestType.fromName("popup"))
        assertEquals(0, RequestType.fromName("csp"))
        assertEquals(0, RequestType.fromName("redirect"))
        assertEquals(0, RequestType.fromName("nonsense"))
    }

    @Test
    fun typeInference() {
        fun inf(main: Boolean = false, dest: String? = null, accept: String? = null, path: String = "/x", xrw: String? = null) =
            RequestType.infer(main, dest, accept, path, xrw)
        assertEquals(RequestType.DOCUMENT, inf(main = true))
        assertEquals(RequestType.SCRIPT, inf(path = "/a/b.js"))
        assertEquals(RequestType.SCRIPT, inf(path = "/a/b.JS"))
        assertEquals(RequestType.STYLESHEET, inf(path = "/a.css"))
        assertEquals(RequestType.IMAGE, inf(path = "/a.png"))
        assertEquals(RequestType.IMAGE, inf(accept = "image/avif,image/webp,*/*"))
        assertEquals(RequestType.STYLESHEET, inf(accept = "text/css,*/*;q=0.1"))
        assertEquals(RequestType.FONT, inf(path = "/f.woff2"))
        assertEquals(RequestType.MEDIA, inf(path = "/v.mp4"))
        assertEquals(RequestType.SUBDOCUMENT, inf(accept = "text/html,application/xhtml+xml"))
        assertEquals(RequestType.XHR, inf(accept = "*/*", xrw = "XMLHttpRequest"))
        assertEquals(RequestType.UNKNOWN, inf(accept = "*/*"))
        assertEquals(RequestType.UNKNOWN, inf())
        assertEquals(RequestType.SCRIPT, inf(dest = "script"))
        assertEquals(RequestType.IMAGE, inf(dest = "image", path = "/weird.js"))
        assertEquals(RequestType.SUBDOCUMENT, inf(dest = "iframe"))
        assertTrue((inf(dest = "empty") and RequestType.XHR) != 0)
        assertEquals("a dot in a directory name is not an extension", RequestType.UNKNOWN, inf(path = "/v1.2/data"))
        assertEquals("ambiguous requests must never be treated as pings (catch-all \$ping rules would block everything)", 0, RequestType.UNKNOWN and RequestType.PING)
        assertEquals(0, inf(dest = "empty") and RequestType.PING)
    }
}
