package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UserAgentsTest {
    private val wv = "Mozilla/5.0 (Linux; Android 10; alps 711 Build/QP1A.190711.020; wv) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.230 Mobile Safari/537.36"

    @Test
    fun mobileLooksLikeReducedChromeAndLeaksNothing() {
        val ua = UserAgents.mobile(wv)
        assertEquals("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36", ua)
        assertFalse(ua.contains("wv")); assertFalse(ua.contains("alps")); assertFalse(ua.contains("Version/"))
    }

    @Test
    fun desktopUa() {
        assertEquals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36", UserAgents.desktop(wv))
    }

    @Test
    fun unknownShapeFallsBackToStrippingMarkers() {
        assertEquals("Foo/1.0 (Linux) Bar", UserAgents.mobile("Foo/1.0 (Linux; wv) Version/4.0 Bar"))
        assertNull(UserAgents.chromeMajor("Foo/1.0"))
        assertEquals(120, UserAgents.chromeMajor(wv))
    }
}
