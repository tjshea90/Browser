package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingParamsTest {
    private fun s(u: String) = TrackingParams.strip(u)

    @Test
    fun stripsAndKeepsOrder() {
        assertEquals("https://a.com/p?id=5", s("https://a.com/p?utm_source=x&id=5"))
        assertEquals("https://a.com/p?id=5", s("https://a.com/p?id=5&fbclid=abc"))
        assertEquals("https://a.com/p?a=1&c=3", s("https://a.com/p?a=1&gclid=2&c=3"))
    }

    @Test
    fun dropsQuestionMarkWhenNothingLeft() {
        assertEquals("https://a.com/p", s("https://a.com/p?utm_source=x&utm_medium=y"))
        assertEquals("https://a.com/", s("https://a.com/?fbclid"))
    }

    @Test
    fun preservesFragment() {
        assertEquals("https://a.com/p#top", s("https://a.com/p?gclid=1#top"))
        assertEquals("https://a.com/p?q=1#a?b=c", s("https://a.com/p?q=1&utm_x=2#a?b=c"))
    }

    @Test
    fun noChangeReturnsNull() {
        for (u in listOf("https://a.com/p?id=5", "https://a.com/p", "https://a.com/p?", "https://a.com/p?#x", "https://a.com/")) {
            assertNull("unchanged: $u", s(u))
        }
    }

    @Test
    fun caseInsensitiveKeysButExactPrefixes() {
        assertEquals("https://a.com/?k=1", s("https://a.com/?UTM_Source=1&k=1"))
        assertNull(s("https://a.com/?utmx=1"))
        assertNull(s("https://a.com/?autm_source=1"))
        assertNull(s("https://a.com/?my_gclid_value=1"))
    }

    @Test
    fun keepsEncodedValuesAndHandlesEmptySegments() {
        assertEquals("https://a.com/?q=hello%20world", s("https://a.com/?q=hello%20world&fbclid=1"))
        assertEquals("https://a.com/?a=1&b=2", s("https://a.com/?a=1&&utm_x=1&b=2"))
        assertEquals("https://a.com/?next=/x?y=z", s("https://a.com/?next=/x?y=z&utm_source=1"))
    }
}
