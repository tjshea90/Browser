package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HttpsPolicyTest {
    @Test
    fun upgradesPublicHttp() {
        assertEquals("https://example.com/a?b#c", HttpsPolicy.upgradeOf("http://example.com/a?b#c"))
        assertEquals("https://Example.com/", HttpsPolicy.upgradeOf("HTTP://Example.com/"))
        assertEquals("https://example.com", HttpsPolicy.upgradeOf("http://example.com"))
        assertEquals("https://u:p@example.com/x", HttpsPolicy.upgradeOf("http://u:p@example.com/x"))
    }

    @Test
    fun dropsExplicitPort80ButKeepsNothingElseOdd() {
        assertEquals("https://example.com/x", HttpsPolicy.upgradeOf("http://example.com:80/x"))
        assertNull(HttpsPolicy.upgradeOf("http://example.com:8080/x")) // dev servers etc. are left alone
    }

    @Test
    fun neverUpgradesLocalOrIpOrNonHttp() {
        for (u in listOf(
            "http://localhost/", "http://192.168.43.1/", "http://router/", "http://printer.local/", "http://8.8.8.8/",
            "http://[::1]/", "https://example.com/", "ftp://example.com", "about:blank", "example.com",
        )) assertNull("must not upgrade: $u", HttpsPolicy.upgradeOf(u))
    }

    @Test
    fun downgradeIsTheExplicitUserChoice() {
        assertEquals("http://example.com/x", HttpsPolicy.downgradeOf("https://example.com/x"))
        assertNull(HttpsPolicy.downgradeOf("http://example.com/x"))
    }
}
