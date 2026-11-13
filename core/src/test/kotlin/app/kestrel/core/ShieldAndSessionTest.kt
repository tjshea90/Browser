package app.kestrel.core

import app.kestrel.core.session.Session
import app.kestrel.core.session.SessionCodec
import app.kestrel.core.session.TabRecord
import app.kestrel.core.shield.DataSaver
import app.kestrel.core.shield.ShieldFlag
import app.kestrel.core.shield.ShieldSettings
import app.kestrel.core.shield.SiteShieldStore
import app.kestrel.core.shield.SiteShields
import app.kestrel.core.shield.effective
import app.kestrel.core.url.HttpsPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldAndSessionTest {
    private val global = ShieldSettings()

    @Test
    fun defaultsAreProtective() {
        assertTrue(global.shieldsUp && global.blockAds && global.cosmetic && global.blockThirdPartyCookies && global.blockFingerprinting)
        assertFalse(global.blockScripts)
        assertEquals(HttpsPolicy.Mode.STRICT, global.https)
    }

    @Test
    fun siteOverridesOnlyChangeWhatTheyName() {
        val s = SiteShields(0, 0).with(ShieldFlag.ADS, false).with(ShieldFlag.SCRIPTS, true)
        val e = s.applyTo(global)
        assertFalse(e.blockAds); assertTrue(e.blockScripts)
        assertTrue(e.cosmetic && e.blockThirdPartyCookies && e.blockFingerprinting && e.shieldsUp)
        assertEquals(global.https, e.https)
        assertTrue(s.without(ShieldFlag.ADS).applyTo(global).blockAds)
        assertTrue(SiteShields(0, 0).isDefault)
    }

    @Test
    fun embedsWaitForATapByDefaultAndEachSiteCanOverrideIt() {
        assertTrue("on by default: video and social players wait for a tap (EX-10)", global.waitEmbeds)
        val off = SiteShields(0, 0).with(ShieldFlag.EMBEDS, false).applyTo(global)
        assertFalse(off.waitEmbeds)
        assertTrue("nothing else changes", off.blockAds && off.cosmetic && off.shieldsUp)
        assertTrue(SiteShields(0, 0).with(ShieldFlag.EMBEDS, true).applyTo(ShieldSettings(waitEmbeds = false)).waitEmbeds)
        assertFalse("the global switch off keeps them loading by themselves", SiteShields(0, 0).applyTo(ShieldSettings(waitEmbeds = false)).waitEmbeds)
        assertFalse("Shields down is the plain browser: embeds load", SiteShields(0, 0).with(ShieldFlag.SHIELDS, false).applyTo(global).effective().waitEmbeds)
        assertEquals("a flag of its own, no clash with the others", 128, ShieldFlag.EMBEDS)
        val st = SiteShieldStore(); st.set("blog.example", SiteShields(0, 0).with(ShieldFlag.EMBEDS, false))
        assertFalse(SiteShieldStore.decode(st.encode())["blog.example"]!!.applyTo(global).waitEmbeds)
    }

    @Test
    fun httpsOverrideRespectsGlobalMode() {
        assertEquals(HttpsPolicy.Mode.OFF, SiteShields(0, 0).with(ShieldFlag.HTTPS, false).applyTo(global).https)
        assertEquals(HttpsPolicy.Mode.STRICT, SiteShields(0, 0).with(ShieldFlag.HTTPS, true).applyTo(global).https)
        val off = ShieldSettings(https = HttpsPolicy.Mode.OFF)
        assertEquals(HttpsPolicy.Mode.UPGRADE, SiteShields(0, 0).with(ShieldFlag.HTTPS, true).applyTo(off).https)
        assertEquals(HttpsPolicy.Mode.OFF, SiteShields(0, 0).applyTo(off).https)
    }

    @Test
    fun shieldsDownMeansPlainBrowserButKeepsHttpsFirst() {
        val e = SiteShields(0, 0).with(ShieldFlag.SHIELDS, false).applyTo(global).effective()
        assertFalse(e.shieldsUp || e.blockAds || e.cosmetic || e.blockThirdPartyCookies || e.blockFingerprinting || e.blockScripts)
        assertEquals(HttpsPolicy.Mode.STRICT, e.https)
        assertEquals(global, global.effective())
    }

    @Test
    fun siteStoreRoundTripAndEviction() {
        val st = SiteShieldStore()
        st.set("a.example", SiteShields(0, 0).with(ShieldFlag.ADS, false))
        st.set("b.example", SiteShields(0, 0).with(ShieldFlag.SCRIPTS, true).with(ShieldFlag.COOKIES, false))
        val back = SiteShieldStore.decode(st.encode())
        assertEquals(st["a.example"], back["a.example"])
        assertEquals(st["b.example"], back["b.example"])
        assertNull(back["c.example"])
        st.set("a.example", SiteShields(0, 0)) // back to defaults -> entry removed
        assertNull(st["a.example"])
        for (i in 0 until SiteShieldStore.MAX_SITES + 50) st.set("h$i.example", SiteShields(0, 0).with(ShieldFlag.ADS, false))
        assertEquals(SiteShieldStore.MAX_SITES, st.size())
        assertNull("oldest evicted first", st["h0.example"])
        assertNotNull(st["h${SiteShieldStore.MAX_SITES + 49}.example"])
    }

    @Test
    fun siteStoreSurvivesGarbage() {
        val d = SiteShieldStore.decode("ok.example\t2\t0\nnot a record\n\t1\t1\nx.example\tabc\t1\ny.example\t999\t1\nz.example\t0\t0\n\n")
        assertNotNull(d["ok.example"]); assertEquals(1, d.size())
        assertEquals(0, SiteShieldStore.decode("").size())
    }

    @Test
    fun dataSaverLevels() {
        assertTrue(DataSaver.STANDARD.mediaPreloadNone && DataSaver.STANDARD.saveDataSignal)
        assertFalse(DataSaver.STANDARD.blockImages); assertTrue(DataSaver.STRICT.blockImages && DataSaver.STRICT.blockFonts)
        assertFalse(DataSaver.OFF.mediaPreloadNone || DataSaver.OFF.saveDataSignal)
        assertEquals(-1L, DataSaver.STANDARD.confirmDownloadBytes(metered = false))
        assertEquals(20L * 1024 * 1024, DataSaver.STANDARD.confirmDownloadBytes(metered = true))
        assertEquals(5L * 1024 * 1024, DataSaver.STRICT.confirmDownloadBytes(metered = true))
        assertEquals(-1L, DataSaver.OFF.confirmDownloadBytes(metered = true))
        assertEquals(DataSaver.STANDARD, DataSaver.parse("garbage")); assertEquals(DataSaver.STRICT, DataSaver.parse("strict"))
    }

    // ------------------------------------------------------------------ session

    private val tabs = listOf(
        TabRecord(1, "https://example.com/a?x=1#f", "Example é世", 1000L, 0, false),
        TabRecord(2, "http://192.168.43.1/", "Router\ttab\nwith%odd chars", 2000L, 1, true),
        TabRecord(3, "about:blank", "", 3000L, 0, false),
    )

    @Test
    fun sessionRoundTrip() {
        val back = SessionCodec.decode(SessionCodec.encode(Session(tabs, 2)))!!
        assertEquals(tabs, back.tabs)
        assertEquals(2L, back.selectedId)
    }

    @Test
    fun sessionDecodeIsForgivingAndSafe() {
        assertNull(SessionCodec.decode(null)); assertNull(SessionCodec.decode("")); assertNull(SessionCodec.decode("garbage"))
        assertNull(SessionCodec.decode("KSESSION 2 1\n")) // unknown version
        val good = SessionCodec.encode(Session(tabs, 1))
        // truncate mid-line: the last, damaged tab is dropped, the others survive
        val cut = good.substring(0, good.length - 25)
        val s = SessionCodec.decode(cut)!!
        assertTrue(s.tabs.size in 1..2)
        assertEquals("https://example.com/a?x=1#f", s.tabs[0].url)
        // dangerous or malformed URLs are never restored
        val evil = "KSESSION 1 1\n1\t1\t0\t0\tjavascript:alert(1)\tx\n2\t1\t0\t0\tfile:///sdcard/x\tx\n3\t1\t0\t0\thttps://ok.example/\tfine\n4\tbad\t0\t0\thttps://x.example/\tx\n"
        val e = SessionCodec.decode(evil)!!
        assertEquals(listOf(3L), e.tabs.map { it.id })
        assertEquals("selection falls back to a tab that exists", 3L, e.selectedId)
        // duplicate ids and bad escapes
        val dup = "KSESSION 1 1\n1\t1\t0\t0\thttps://a.example/\tt\n1\t1\t0\t0\thttps://b.example/\tt\n5\t1\t0\t0\thttps://c.example/%zz\tt\n"
        assertEquals(1, SessionCodec.decode(dup)!!.tabs.size)
    }

    @Test
    fun aTabThatWasOnHomeComesBackOnHome() {
        val t = listOf(
            TabRecord(1, "https://doraby.com/", "Doraby", 1000L, 0, false, homeOverPage = true),
            TabRecord(2, "https://a.example/", "A", 2000L, 0, true, homeOverPage = true),   // desktop site and Home at once
            TabRecord(3, "https://b.example/", "B", 3000L, 0, true),
        )
        val text = SessionCodec.encode(Session(t, 1))
        assertEquals(6, text.lines()[1].split('\t').size)                              // still six fields: an older app still finds every tab
        val back = SessionCodec.decode(text)!!
        assertEquals(t, back.tabs)
        assertEquals(listOf(true, true, false), back.tabs.map { it.homeOverPage })
        assertEquals(listOf(false, true, true), back.tabs.map { it.desktopSite })
    }

    @Test
    fun sessionFilesFromBeforeTheHomeFlagStillLoadAsNotOnHome() {
        val old = "KSESSION 1 2\n1\t1000\t0\t0\thttps://one.example/\tOne\n2\t2000\t1\t1\thttps://two.example/\tTwo\n"
        val s = SessionCodec.decode(old)!!
        assertEquals(listOf(false, false), s.tabs.map { it.homeOverPage })
        assertEquals(listOf(false, true), s.tabs.map { it.desktopSite })
        // a flag field this version does not know is read as "not desktop, not Home", never as a reason to drop the tab
        val odd = SessionCodec.decode("KSESSION 1 1\n1\t1\t0\tZ\thttps://one.example/\tOne\n")!!
        assertEquals(1, odd.tabs.size); assertFalse(odd.tabs[0].desktopSite || odd.tabs[0].homeOverPage)
    }

    @Test
    fun anEmptyTabCannotBeOnHomeOverAPage() {
        val s = SessionCodec.decode("KSESSION 1 1\n1\t1\t0\t0H\tabout:blank\t\n")!!
        assertFalse("a new tab has no page under Home", s.tabs[0].homeOverPage)
    }

    @Test
    fun sessionIsCapped() {
        val many = (1L..500L).map { TabRecord(it, "https://e.example/$it", "t", it, 0, false) }
        assertEquals(SessionCodec.MAX_TABS, SessionCodec.decode(SessionCodec.encode(Session(many, 1)))!!.tabs.size)
    }
}
