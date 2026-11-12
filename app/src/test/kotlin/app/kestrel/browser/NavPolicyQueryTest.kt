package app.kestrel.browser

import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.Shields
import app.kestrel.browser.web.NavPolicy
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSet
import app.kestrel.core.filter.FilterSetBuilder
import app.kestrel.core.shield.ShieldFlag
import app.kestrel.core.shield.SiteShields
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** AA3-3b: the lists' `$removeparam` rules reach the address of every top-level navigation, next to the built-in tracking names. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class NavPolicyQueryTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private lateinit var saved: FilterSet

    @Before fun setUp() {
        Shields.init(ctx); Shields.clearAll()
        Engine.start(ctx)
        saved = Engine.filters()
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.PRIVACY, sequenceOf("\$removeparam=pjclid", "||auto.de^\$removeparam=source", "@@||shop.example^\$removeparam=pjclid"))
        Engine.swap(b.build(1L), "test")
    }

    @After fun tearDown() {
        Engine.swap(saved, "restored")
        Shields.clearAll()
    }

    private fun prepared(url: String) = NavPolicy.prepare(Prefs.get(ctx), url, null).url

    @Test fun theListsRulesStripAnAddressAndTheBuiltInNamesStillGo() {
        assertEquals("https://example.com/p?a=2", prepared("https://example.com/p?pjclid=1&a=2"))
        assertEquals("https://www.auto.de/x", prepared("https://www.auto.de/x?source=ad"))
        assertEquals("both kinds in one address", "https://auto.de/x?k=1", prepared("https://auto.de/x?utm_source=n&source=ad&k=1&fbclid=z"))
    }

    @Test fun anExceptionOfTheListsIsRespectedByTheListsRules() {
        assertEquals("https://shop.example/?pjclid=1", prepared("https://shop.example/?pjclid=1"))
    }

    @Test fun anAddressWithNothingToStripComesBackUnchanged() {
        assertEquals("https://example.com/p?a=2", prepared("https://example.com/p?a=2"))
        assertEquals("https://example.com/p", prepared("https://example.com/p"))
    }

    @Test fun aSiteWithAdBlockingSwitchedOffKeepsItsAddress() {
        Shields.setOverride("auto.de", SiteShields(ShieldFlag.ADS, 0))
        assertEquals("https://auto.de/x?source=ad&utm_source=n", prepared("https://auto.de/x?source=ad&utm_source=n"))
    }

    @Test fun beforeTheListsAreLoadedOnlyTheBuiltInNamesGo() {
        // an empty set stands for "no query rules": the built-in names still work and nothing else changes
        Engine.swap(FilterSetBuilder().build(2L), "empty")
        assertEquals("https://example.com/p?pjclid=1", prepared("https://example.com/p?pjclid=1&utm_medium=x"))
    }
}
