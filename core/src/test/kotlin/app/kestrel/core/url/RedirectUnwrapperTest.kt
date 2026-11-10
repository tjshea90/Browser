package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RedirectUnwrapperTest {
    private fun u(x: String) = RedirectUnwrapper.unwrap(x)

    @Test
    fun google() {
        assertEquals(
            "https://example.org/page?a=1",
            u("https://www.google.com/url?sa=t&url=https%3A%2F%2Fexample.org%2Fpage%3Fa%3D1&usg=x"),
        )
        assertEquals("https://example.org/", u("https://www.google.com/url?q=https://example.org/"))
        assertEquals("https://example.org/", u("https://www.google.co.uk/url?q=https://example.org/"))
    }

    @Test
    fun socialAndSearchRedirectors() {
        assertEquals("https://example.org/", u("https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.org%2F&h=AT0"))
        assertEquals("https://example.org/", u("https://www.youtube.com/redirect?event=video_description&q=https%3A%2F%2Fexample.org%2F&redir_token=x"))
        assertEquals("https://example.org/", u("https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.org%2F&rut=x"))
        assertEquals("https://example.org/", u("https://out.reddit.com/t3_abc?url=https%3A%2F%2Fexample.org%2F&token=1"))
    }

    @Test
    fun bingBase64() {
        assertEquals("https://example.org/", u("https://www.bing.com/ck/a?!&&p=x&u=a1aHR0cHM6Ly9leGFtcGxlLm9yZy8&ntb=1"))
    }

    @Test
    fun onlyHttpTargetsAndNoSelfLoops() {
        assertNull(u("https://www.google.com/url?q=javascript%3Aalert(1)"))
        assertNull(u("https://www.google.com/url?q=ftp://example.org/"))
        assertNull(u("https://www.google.com/url?q=https://www.google.com/x"))
        assertNull(u("https://www.google.com/url?q=%0Ahttps://example.org/")) // control char
    }

    @Test
    fun ignoresEverythingElse() {
        assertNull(u("https://example.com/url?q=https://a.com"))
        assertNull(u("https://www.google.com/search?q=https://a.com"))
        assertNull(u("https://www.google.com/url?q=%zz"))
        assertNull(u("https://www.google.com/url"))
        assertNull(u("not a url"))
        assertNull(u("https://www.google.evil.com/url?q=https://a.com"))
    }
}
