package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniboxTest {
    private val tpl = "https://search.example/?q=%s"
    private fun r(x: String) = Omnibox.resolve(x, tpl)
    private fun go(x: String) = (r(x) as? Nav.Go)?.url
    private fun search(x: String) = (r(x) as? Nav.Search)?.url

    @Test
    fun emptyInput() {
        assertEquals(Nav.Empty, r(""))
        assertEquals(Nav.Empty, r("   \t"))
    }

    @Test
    fun domainsAreHttpsFirst() {
        assertEquals("https://example.com", go("example.com"))
        assertEquals("https://example.com/path?x=1", go("example.com/path?x=1"))
        assertEquals("https://example.com:8080", go("example.com:8080"))
        assertEquals("https://www.bbc.co.uk", go("  www.bbc.co.uk "))
        assertEquals("https://EXAMPLE.COM", go("EXAMPLE.COM"))
    }

    @Test
    fun explicitHttpUrlsPassThroughUntouched() {
        assertEquals("http://example.com", go("http://example.com"))
        assertEquals("https://example.com/a?b=c#d", go("https://example.com/a?b=c#d"))
    }

    @Test
    fun localAndIpHostsUseHttp() {
        assertEquals("http://localhost", go("localhost"))
        assertEquals("http://localhost:8080/x", go("localhost:8080/x"))
        assertEquals("http://192.168.43.1", go("192.168.43.1"))
        assertEquals("http://8.8.8.8", go("8.8.8.8"))
        assertEquals("http://[::1]:8080", go("[::1]:8080"))
        assertEquals("http://printer.local", go("printer.local"))
        assertEquals("http://intranet:8080", go("intranet:8080"))
    }

    @Test
    fun plainTextBecomesSearch() {
        assertEquals("https://search.example/?q=hello+world", search("hello world"))
        assertEquals("https://search.example/?q=hello", search("hello"))
        assertEquals("https://search.example/?q=intranet", search("intranet"))
        assertEquals("https://search.example/?q=a%26b%3Dc+d", search("a&b=c d"))
        assertEquals("https://search.example/?q=what+is+c%2B%2B", search("what is c++"))
        assertEquals("https://search.example/?q=https%3A%2F%2Fexample.com%2Fa+b", search("https://example.com/a b"))
    }

    @Test
    fun emailsNumbersAndFileNamesAreNotHosts() {
        assertTrue(r("me@example.com") is Nav.Search)
        assertTrue(r("mailto:a@b.c") is Nav.Search)
        assertTrue(r("3.14") is Nav.Search)
        assertTrue(r("1.5") is Nav.Search)
        assertTrue(r("v1.2.3") is Nav.Search)
        assertTrue(r("main.py") is Nav.Search)
        assertTrue(r("readme.txt") is Nav.Search)
        assertTrue(r("notes.pdf") is Nav.Search)
        assertEquals("https://main.py/x", go("main.py/x")) // with a path it is clearly a site
    }

    @Test
    fun dangerousSchemesAreRejected() {
        for (x in listOf(
            "javascript:alert(1)", "JAVASCRIPT:alert(1)", "file:///sdcard/x.html", "data:text/html,hi", "intent://x#Intent;end",
            "ftp://example.com", "about:config", "content://media/x", "blob:https://a/b", "view-source:https://a.com", "chrome://flags",
        )) assertTrue("must reject: $x -> ${r(x)}", r(x) is Nav.Rejected)
        assertEquals("about:blank", go("about:blank"))
    }

    @Test
    fun hostPortLooksLikeSchemeButIsNot() {
        assertEquals("http://localhost:3000", go("localhost:3000"))
        assertEquals("https://example.com:443/x", go("example.com:443/x"))
    }
}
