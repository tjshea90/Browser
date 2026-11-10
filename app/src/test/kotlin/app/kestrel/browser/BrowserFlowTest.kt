package app.kestrel.browser

import android.app.Activity
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.TextView
import app.kestrel.browser.ui.TabsButton
import app.kestrel.browser.web.Tab
import org.junit.After
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowWindowManagerGlobal

/** Drives the real BrowserActivity on the JVM (Android 10 framework + shadow WebView). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BrowserFlowTest {
    private var controller: ActivityController<MainActivity>? = null

    @Before fun resetProc() { Proc.isPrivate = false }
    @After fun tearDown() { controller?.pause()?.stop()?.destroy(); Proc.isPrivate = false }

    private fun launch(): MainActivity {
        val c = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible()
        controller = c
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        return c.get()
    }

    private fun views(v: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) views(v.getChildAt(i), out)
        return out
    }

    /** Everything shown in ANY window: the activity plus popups (menus, Shields panel). */
    private fun allWindowViews(): List<View> = ShadowWindowManagerGlobal.getWindowViews()
    private fun popupTexts(): List<String> = allWindowViews().flatMap { texts(it) }
    private fun popupViews(): List<View> = allWindowViews().flatMap { views(it) }

    private fun texts(root: View): List<String> = views(root).filterIsInstance<TextView>().map { it.text.toString() }.filter { it.isNotEmpty() }
    private fun urlField(a: Activity): EditText = views(a.window.decorView).filterIsInstance<EditText>().first { it.hint?.toString()?.contains("address") == true }
    private fun webViews(a: Activity): List<WebView> = views(a.window.decorView).filterIsInstance<WebView>()

    private fun typeAndGo(a: Activity, text: String) {
        val f = urlField(a)
        f.requestFocus()
        f.setText(text)
        f.onEditorAction(EditorInfo.IME_ACTION_GO)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    private fun loadedUrl(a: Activity): String? = webViews(a).firstOrNull()?.let { shadowOf(it).lastLoadedUrl }

    // ------------------------------------------------------------------ startup

    @Test
    fun startsOnNewTabPageWithoutCreatingAWebView() {
        val a = launch()
        assertTrue(texts(a.window.decorView).any { it.contains("Kestrel") })
        assertTrue("startup must not create a WebView (cold-start cost)", webViews(a).isEmpty())
    }

    @Test
    fun newTabPageWarnsWhenNoWebViewIsInstalled() {
        // Robolectric has no WebView provider package: the notice path must render rather than crash.
        val t = texts(launch().window.decorView)
        assertTrue(t.toString(), t.any { it.contains("No web engine found") })
    }

    // ------------------------------------------------------------------ omnibox navigation

    @Test
    fun typedDomainLoadsOverHttps() {
        val a = launch()
        typeAndGo(a, "example.com")
        assertEquals("https://example.com", loadedUrl(a))
    }

    @Test
    fun httpIsUpgradedAndTrackingParametersAreStripped() {
        val a = launch()
        typeAndGo(a, "http://example.com/x?utm_source=a&id=1&fbclid=zzz")
        assertEquals("https://example.com/x?id=1", loadedUrl(a))
    }

    @Test
    fun lanAddressesStayHttp() {
        val a = launch()
        typeAndGo(a, "192.168.43.1")
        assertEquals("http://192.168.43.1", loadedUrl(a))
    }

    @Test
    fun textBecomesASearch() {
        val a = launch()
        typeAndGo(a, "hello world")
        assertEquals("https://search.brave.com/search?q=hello+world", loadedUrl(a))
    }

    @Test
    fun javascriptUrlsAreRefused() {
        val a = launch()
        typeAndGo(a, "javascript:alert(1)")
        assertNull("nothing may be loaded", loadedUrl(a))
        assertTrue(webViews(a).isEmpty())
    }

    // ------------------------------------------------------------------ hardened WebView

    @Test
    fun webViewIsCreatedHardened() {
        val a = launch()
        typeAndGo(a, "example.com")
        val s = webViews(a).first().settings
        assertFalse(s.allowFileAccess)
        assertFalse(s.allowContentAccess)
        assertEquals(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW, s.mixedContentMode)
        assertTrue(s.safeBrowsingEnabled)
        assertTrue(s.mediaPlaybackRequiresUserGesture)
        assertFalse("window.open without a gesture stays blocked", s.javaScriptCanOpenWindowsAutomatically)
        assertTrue(s.javaScriptEnabled)
    }

    // ------------------------------------------------------------------ panels

    @Test
    fun tabSwitcherOpensAndCanAddATab() {
        val a = launch()
        val tabsButton = views(a.window.decorView).filterIsInstance<TabsButton>().first()
        assertEquals(1, tabsButton.count)
        tabsButton.performClick()
        assertTrue(texts(a.window.decorView).any { it == "1 tab" })
        views(a.window.decorView).filterIsInstance<TextView>().first { it.text == "New tab" }.performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(2, views(a.window.decorView).filterIsInstance<TabsButton>().first().count)
    }

    @Test
    fun menuOpensAndSettingsBuilds() {
        val a = launch()
        val menu = views(a.window.decorView).first { it.contentDescription == "Menu" }
        menu.performClick()
        val entries = popupTexts()
        assertTrue(entries.toString(), entries.any { it.contains("Settings") } && entries.any { it.contains("New private window") })
        popupViews().filterIsInstance<TextView>().first { it.text.contains("Settings") }.performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val t = texts(a.window.decorView)
        assertTrue(t.toString(), t.any { it == "Settings" } && t.any { it.contains("SECURE DNS") } && t.any { it.contains("Strict: warn before plain HTTP") })
    }

    @Test
    fun shieldsPanelOpensForALoadedPage() {
        val a = launch()
        typeAndGo(a, "example.com")
        val chip = views(a.window.decorView).first { it.contentDescription?.toString()?.startsWith("Shields") == true }
        chip.performClick()
        val t = popupTexts()
        assertTrue(t.toString(), t.any { it.contains("Shields for example.com") } && t.any { it == "Block ads & trackers" })
    }

    // ------------------------------------------------------------------ errors (hotspot-aware)

    @Test
    fun networkErrorShowsTheHotspotAwareScreen() {
        val a = launch()
        typeAndGo(a, "example.com")
        val tab = a.currentTab!!
        tab.error = app.kestrel.browser.web.ErrorScreen.Network("https://example.com/", WebViewClient.ERROR_HOST_LOOKUP, "net::ERR_NAME_NOT_RESOLVED")
        a.errorChanged(tab)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val t = texts(a.window.decorView)
        assertTrue(t.toString(), t.any { it.contains("Try again") })
        // Robolectric reports an unvalidated network, so the hotspot-specific wording (not the generic DNS one) is expected.
        assertTrue(t.toString(), t.any { it.contains("Can't find that website") || it.contains("No Wi-Fi") || it.contains("hotspot") })
    }

    @Test
    fun sslAndDangerousScreensOfferASafeExit() {
        val a = launch()
        typeAndGo(a, "example.com")
        val tab = a.currentTab!!
        tab.error = app.kestrel.browser.web.ErrorScreen.Ssl("https://example.com/", "example.com", "The certificate has expired.", "Test CA")
        a.errorChanged(tab); org.robolectric.shadows.ShadowLooper.idleMainLooper()
        var t = texts(a.window.decorView)
        assertTrue(t.toString(), t.any { it.contains("Your connection isn't private") } && t.any { it.contains("Back to safety") } && t.any { it.contains("Proceed to example.com (unsafe)") })
        tab.error = app.kestrel.browser.web.ErrorScreen.Dangerous("https://evil.example/", "evil.example", app.kestrel.core.filter.Category.MALWARE)
        a.errorChanged(tab); org.robolectric.shadows.ShadowLooper.idleMainLooper()
        t = texts(a.window.decorView)
        assertTrue(t.toString(), t.any { it.contains("Dangerous site blocked") } && t.any { it.contains("Back to safety") })
        tab.error = app.kestrel.browser.web.ErrorScreen.HttpsUnavailable("http://old.example/", "old.example")
        a.errorChanged(tab); org.robolectric.shadows.ShadowLooper.idleMainLooper()
        t = texts(a.window.decorView)
        assertTrue(t.toString(), t.any { it.contains("No secure connection") } && t.any { it.contains("Continue to the HTTP site (unsafe)") })
    }

    // ------------------------------------------------------------------ intents

    @Test
    fun viewIntentOpensTheUrlInTheBrowser() {
        val c = Robolectric.buildActivity(MainActivity::class.java, android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://example.org/page")))
        controller = c
        c.create().start().resume().visible()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        // the session restore runs on the IO thread; give it a moment then drain the main looper
        Thread.sleep(300)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals("https://example.org/page", loadedUrl(c.get()))
    }

    @Test
    fun nonWebIntentsAreIgnored() {
        val c = Robolectric.buildActivity(MainActivity::class.java, android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("file:///sdcard/secret.html")))
        controller = c
        c.create().start().resume().visible()
        Thread.sleep(300)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue("file: must never be opened", webViews(c.get()).isEmpty())
    }

    // ------------------------------------------------------------------ private window

    @Test
    fun privateWindowIsSecureAndDistinct() {
        Proc.isPrivate = true
        val c = Robolectric.buildActivity(PrivateActivity::class.java).create().start().resume().visible()
        val a = c.get()
        assertTrue("FLAG_SECURE must be set", a.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
        assertTrue(texts(a.window.decorView).any { it.contains("Private window") })
        c.pause().stop().destroy()
    }

    @Test
    fun tabModelKnowsNewTabsFromRestoredOnes() {
        val t = Tab(1)
        assertTrue(t.isNewTab)
        t.pendingLoad = "https://example.com"
        assertFalse("a restored, not-yet-loaded tab is not a new tab", t.isNewTab)
        assertNotNull(t.shields)
    }
}
