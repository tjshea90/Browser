package app.kestrel.browser

import android.net.Uri
import android.webkit.WebResourceRequest
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.net.Downloads
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.Shields
import app.kestrel.browser.web.ExternalIntents
import app.kestrel.browser.web.Interceptor
import app.kestrel.browser.web.NavPolicy
import app.kestrel.browser.web.Tab
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSetBuilder
import app.kestrel.core.shield.DataSaver
import app.kestrel.core.shield.ShieldFlag
import app.kestrel.core.shield.ShieldSettings
import app.kestrel.core.shield.SiteShields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InterceptorAndStoresTest {
    private lateinit var activity: MainActivity
    private lateinit var prefs: Prefs
    private lateinit var interceptor: Interceptor

    @Before
    fun setUp() {
        Proc.isPrivate = false
        activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible().get()
        prefs = Prefs.get(activity)
        prefs.dataSaver = DataSaver.STANDARD
        interceptor = Interceptor(activity, prefs)
        Engine.filters() // wait for the bundled snapshot
    }

    private fun req(url: String, main: Boolean = false, accept: String? = "*/*") = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = main
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): MutableMap<String, String> = HashMap<String, String>().also { m -> accept?.let { m["Accept"] = it } }
    }

    private fun tab(host: String = "news.example") = Tab(7).apply { topHost = host; shields = ShieldSettings() }

    // ------------------------------------------------------------------ interception with the REAL bundled lists

    @Test
    fun blocksAnAdScriptAndCountsIt() {
        val t = tab()
        val r = interceptor.intercept(t, req("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertNotNull(r)
        assertEquals("application/javascript", r!!.mimeType)
        assertEquals(200, r.statusCode)
        assertEquals(0, r.data.available())
        assertEquals(1, t.blockedTotal())
    }

    @Test
    fun blockedImagesBecomeATransparentPixel() {
        val r = interceptor.intercept(tab(), req("https://static.criteo.net/images/x.png", accept = "image/png,*/*"))
        assertNotNull(r)
        assertEquals("image/gif", r!!.mimeType)
        assertEquals(43, r.data.available())
    }

    @Test
    fun leavesEssentialInfrastructureAlone() {
        val t = tab()
        for (u in listOf("https://cdn.jsdelivr.net/npm/jquery@3/dist/jquery.min.js", "https://fonts.googleapis.com/css2?family=Roboto", "https://js.stripe.com/v3/")) {
            assertNull(u, interceptor.intercept(t, req(u)))
        }
        assertEquals(0, t.blockedTotal())
    }

    @Test
    fun nonHttpSchemesAreIgnored() {
        assertNull(interceptor.intercept(tab(), req("data:text/html,hi")))
        assertNull(interceptor.intercept(tab(), req("blob:https://news.example/abc")))
    }

    @Test
    fun shieldsDownMeansNoBlocking() {
        val t = tab().apply { shields = ShieldSettings(shieldsUp = false, blockAds = false) }
        assertNull(interceptor.intercept(t, req("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")))
        val off = tab().apply { shields = ShieldSettings(blockAds = false) }
        assertNull(interceptor.intercept(off, req("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")))
    }

    @Test
    fun strictDataSaverBlocksWebFonts() {
        prefs.dataSaver = DataSaver.STRICT
        val r = interceptor.intercept(tab(), req("https://cdn.example/fonts/x.woff2"))
        assertNotNull("fonts are skipped in strict mode", r)
        assertEquals(204, r!!.statusCode)
        prefs.dataSaver = DataSaver.STANDARD
        assertNull(interceptor.intercept(tab(), req("https://cdn.example/fonts/x.woff2")))
    }

    @Test
    fun mainFrameLearnsTheHostAndAppliesPerSiteShields() {
        Shields.setOverride("custom.example", SiteShields(0, 0).with(ShieldFlag.ADS, false))
        val t = Tab(8)
        assertNull(interceptor.intercept(t, req("https://custom.example/page", main = true)))
        assertEquals("custom.example", t.topHost)
        assertFalse("per-site override applied", t.shields.blockAds)
        assertNull(interceptor.intercept(t, req("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")))
        Shields.setOverride("custom.example", SiteShields(0, 0))
    }

    @Test
    fun malwareDocumentsAreBlockedWithAPlaceholderAndPopupsAreClosed() {
        val b = FilterSetBuilder()
        b.addAbpList("t", Category.MALWARE, sequenceOf("||evil.example^"))
        b.addAbpList("t", Category.ADS, sequenceOf("||pop.example^\$popup"))
        val original = Engine.filters()
        Engine.swap(b.build(1L), "test")
        try {
            val t = Tab(9)
            val r = interceptor.intercept(t, req("https://evil.example/", main = true))
            assertNotNull(r); assertEquals(403, r!!.statusCode)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            assertTrue("native warning screen requested", t.error is app.kestrel.browser.web.ErrorScreen.Dangerous)

            val popup = Tab(10).apply { isFreshPopup = true; openerHost = "opener.example" }
            val pr = interceptor.intercept(popup, req("https://pop.example/ad", main = true))
            assertNotNull(pr); assertEquals(403, pr!!.statusCode)
            assertFalse("only the first navigation is checked", popup.isFreshPopup)
            val ok = interceptor.intercept(Tab(11).apply { isFreshPopup = true; openerHost = "opener.example" }, req("https://fine.example/", main = true))
            assertNull(ok)
        } finally { Engine.swap(original, "restored") }
    }

    // ------------------------------------------------------------------ navigation policy

    @Test
    fun navPolicyHonoursPerSiteHttpsOverride() {
        assertEquals("https://a.example/", NavPolicy.prepare(prefs, "http://a.example/", null).url)
        Shields.setOverride("b.example", SiteShields(0, 0).with(ShieldFlag.HTTPS, false))
        assertEquals("http://b.example/", NavPolicy.prepare(prefs, "http://b.example/", null).url)
        Shields.setOverride("b.example", SiteShields(0, 0))
        val tab = Tab(1).apply { httpOkHosts.add("c.example") }
        assertEquals("the user's explicit HTTP choice sticks", "http://c.example/", NavPolicy.prepare(prefs, "http://c.example/", tab).url)
    }

    // ------------------------------------------------------------------ external intents

    @Test
    fun externalIntentsAreSanitised() {
        val p = ExternalIntents.plan(activity, "intent://scan/#Intent;scheme=zxing;package=com.example.app;component=com.evil/.Steal;S.browser_fallback_url=https%3A%2F%2Ffallback.example%2F;end")!!
        assertNull("a page must not choose the component", p.intent.component)
        assertNull(p.intent.selector)
        assertTrue(p.intent.categories.contains(android.content.Intent.CATEGORY_BROWSABLE))
        assertEquals("https://fallback.example/", p.fallbackUrl)
        assertTrue(p.needsConfirm)
        for (bad in listOf("file:///sdcard/x", "content://media/x", "javascript:alert(1)", "data:text/html,x", "intent://x#Intent;scheme=file;end")) {
            assertNull("must refuse $bad", ExternalIntents.plan(activity, bad))
        }
        assertFalse("mailto needs no confirmation", ExternalIntents.plan(activity, "mailto:a@b.example")!!.needsConfirm)
    }

    // ------------------------------------------------------------------ downloads

    @Test
    fun downloadNamesAreSanitisedAndRiskyTypesFlagged() {
        assertEquals("evil_.._name.txt".length > 0, true)
        val n = Downloads.fileName("https://x.example/a/b/..%2F..%2Fpasswd", "attachment; filename=\"../../etc/pa:ss*wd.txt\"", "text/plain")
        assertFalse(n, n.contains("/") || n.contains("\\") || n.contains(":") || n.contains("*") || n.startsWith("."))
        assertTrue(Downloads.isRisky("setup.apk")); assertTrue(Downloads.isRisky("run.SH")); assertFalse(Downloads.isRisky("photo.jpg"))
    }

    // ------------------------------------------------------------------ history / bookmarks

    @Test
    fun historyBookmarksAndSuggestions() {
        val db = BrowserDb(activity)
        db.recordVisit("https://example.com/a", "Example A")
        db.recordVisit("https://example.com/a", "Example A")
        db.recordVisit("https://example.com/b", "Example B")
        db.recordVisit("https://news.example/x", "News")
        db.recordVisit("javascript:alert(1)", "bad"); db.recordVisit("file:///x", "bad")
        assertEquals(3, db.history(10).size)
        assertEquals(2, db.history(10).first { it.url.endsWith("/a") }.visits)
        assertEquals("example.com", db.topSites(5).first().first)
        db.addBookmark("https://bm.example/", "My bookmark")
        assertTrue(db.isBookmarked("https://bm.example/"))
        val s = db.suggest("exam", 6)
        assertTrue(s.toString(), s.any { it.url == "https://example.com/a" })
        assertEquals("bookmarks rank first", "https://bm.example/", db.suggest("bookmark", 6).first().url)
        db.removeBookmark("https://bm.example/"); assertFalse(db.isBookmarked("https://bm.example/"))
        db.deleteHistory("https://example.com/b"); assertEquals(2, db.history(10).size)
        db.purge(1, 90); assertEquals(1, db.history(10).size)
        db.clearHistory(); assertEquals(0, db.history(10).size)
        db.close()
    }
}
