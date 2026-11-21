package app.kestrel.browser.shield

import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.core.shield.ShieldFlag
import app.kestrel.core.shield.SiteShields
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** PV1 (owner 2026-11-21: nothing saved from a private tab): the Shields panel opened from a private tab changes a site for the session only; the saved file and the backup never name that site. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ShieldsPrivateTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val canary = "canary-private-site.example.com"
    @Before fun start() { Shields.resetForTest(); ctx.filesDir.mkdirs(); Shields.init(ctx); ioDone() }
    @After fun end() { ioDone(); Shields.resetForTest() }
    private fun ioDone() { val l = CountDownLatch(1); Io.exec { l.countDown() }; assertTrue(l.await(10, TimeUnit.SECONDS)) }
    private fun saved(): String = File(ctx.filesDir, "site_shields.txt").let { if (it.isFile) it.readText() else "" }

    @Test fun aChangeFromAPrivateTabIsAppliedButNeverWrittenDownNorBackedUp() {
        val prefs = Prefs.get(ctx)
        Shields.setOverride("regular-site.example.com", SiteShields(ShieldFlag.ADS, 0)); ioDone()
        Shields.setOverride(canary, SiteShields(ShieldFlag.SHIELDS, 0), fromPrivate = true); ioDone()
        assertFalse("the page behaves as it was set while the private tab is open", Shields.effective(prefs, canary).shieldsUp)
        assertTrue(saved(), saved().contains("regular-site.example.com"))
        assertFalse(saved(), saved().contains("canary"))
        assertFalse(Shields.exportText(), Shields.exportText().contains("canary"))
        assertTrue(Shields.exportText().contains("regular-site.example.com"))
        Shields.setOverride("other.example.com", SiteShields(ShieldFlag.COSMETIC, 0)); ioDone()
        assertFalse("a later saved change must not carry the private one along: ${saved()}", saved().contains("canary"))
        assertTrue(saved().contains("other.example.com"))
        assertEquals(1, Shields.endPrivate())
        assertTrue("taken back when the last private tab is gone", Shields.effective(prefs, canary).shieldsUp)
        assertEquals("a second call has nothing to take back", 0, Shields.endPrivate())
    }

    @Test fun whenThePrivateWindowEndsWhatWasSavedBeforeComesBack() {
        val prefs = Prefs.get(ctx)
        Shields.setOverride("site.example.com", SiteShields(ShieldFlag.ADS, 0)); ioDone()                 // saved: ads blocking off for the site
        Shields.setOverride("site.example.com", SiteShields(ShieldFlag.ADS or ShieldFlag.SHIELDS, 0), fromPrivate = true)
        assertFalse(Shields.effective(prefs, "site.example.com").shieldsUp)
        assertTrue("the saved line is still the old one", Shields.exportText().contains("site.example.com\t${ShieldFlag.ADS}\t0"))
        Shields.endPrivate()
        val e = Shields.effective(prefs, "site.example.com")
        assertTrue(e.shieldsUp); assertFalse(e.blockAds)
    }

    @Test fun aChangeFromARegularTabIsASavedChoiceAndReplacesAPrivateOneForThatSite() {
        Shields.setOverride("site.example.com", SiteShields(ShieldFlag.SHIELDS, 0), fromPrivate = true)
        Shields.setOverride("site.example.com", SiteShields(ShieldFlag.ADS, 0)); ioDone()
        assertTrue(saved(), saved().contains("site.example.com"))
        assertEquals("nothing is left to take back for it", 0, Shields.endPrivate())
        assertFalse(Shields.effective(Prefs.get(ctx), "site.example.com").blockAds)
    }

    @Test fun thePanelAndTheEndOfThePrivateWindowAreWiredInTheSource() {
        val panel = File("src/main/kotlin/app/kestrel/browser/ui/ShieldsPanel.kt").readText()
        assertTrue(panel.contains("Shields.setOverride(host, current, fromPrivate = pal.isPrivate)"))
        val b = File("../geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt").readText()
        assertTrue(b.substringAfter("private fun syncPrivateAnchor()").substringBefore("if (need && privateAnchor == null)").contains("if (!need) Shields.endPrivate()"))
    }
}
