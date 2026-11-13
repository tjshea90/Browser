package app.kestrel.core.shield

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtpCategoriesTest {
    @Test fun kestrelKeepsEverythingStrictBlocksExceptTheCdnAndPlayerCategory() {
        assertEquals("STRICT as shipped in GeckoView 160 is the sum of nine categories", 766, EtpCategories.STRICT)
        assertEquals(EtpCategories.STRICT - EtpCategories.CONTENT, EtpCategories.KESTREL)
        assertEquals(0, EtpCategories.KESTREL and EtpCategories.CONTENT)
        for (c in listOf(EtpCategories.AD, EtpCategories.ANALYTIC, EtpCategories.SOCIAL, EtpCategories.TEST, EtpCategories.CRYPTOMINING, EtpCategories.FINGERPRINTING, EtpCategories.STP, EtpCategories.EMAIL))
            assertTrue("category $c stays on", EtpCategories.KESTREL and c != 0)
    }

    @Test fun namesAreReadable() {
        assertEquals("ads, content (CDN / player)", EtpCategories.names(EtpCategories.AD or EtpCategories.CONTENT))
        assertTrue(EtpCategories.names(EtpCategories.STRICT).contains("content"))
        assertFalse(EtpCategories.names(EtpCategories.KESTREL).contains("content"))
        assertEquals("other", EtpCategories.names(0))
    }
}
