package app.kestrel.browser

import app.kestrel.browser.shield.FilterSources
import app.kestrel.browser.shield.ListSource
import app.kestrel.core.filter.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** RX3-B1: which downloaded lists may carry `trusted-*` scriptlets (they rewrite what a site's own API answers and set cookies). Only the uBlock Origin lists, whose maintainers also write the scriptlets. */
class FilterSourcesTrustTest {
    @Test fun onlyTheUboListsAreTrusted() {
        val trusted = FilterSources.DEFAULT.filter { it.trusted }.map { it.id }.toSet()
        assertEquals(setOf("ubo-filters", "ubo-privacy", "ubo-removeparam", "ubo-badware", "ubo-unbreak", "ubo-quick-fixes"), trusted)
    }

    @Test fun easyListEasyPrivacyAndFanboyAreNotTrusted() {
        for (id in listOf("easylist", "easyprivacy", "easylist-cookies")) assertFalse(id, FilterSources.DEFAULT.first { it.id == id }.trusted)
    }

    @Test fun aSourceAddedLaterIsUntrustedUntilSomeoneSaysOtherwise() {
        assertFalse(ListSource("new-list", "https://example.test/list.txt", Category.ADS).trusted)
    }
}
