package app.kestrel.browser

import android.net.NetworkCapabilities
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.TextView
import app.kestrel.browser.net.NetworkMonitor
import app.kestrel.browser.net.NetworkMonitor.State
import app.kestrel.browser.web.BrowserWebViewClient
import app.kestrel.browser.web.ErrorScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * The tablet's only network is a phone hotspot. Android marks a Wi-Fi network "validated" only after its own connectivity check succeeds,
 * and that check fails on some perfectly working hotspots (carrier blocks the check URL) and for a few seconds after every reconnect.
 * So "not validated" must never change how pages load; only "no network at all" may.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ConnectivityPolicyTest {
    private var controller: ActivityController<MainActivity>? = null

    private fun caps(internet: Boolean, validated: Boolean): NetworkCapabilities {
        val c = ShadowNetworkCapabilities.newInstance()
        if (internet) shadowOf(c).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (validated) shadowOf(c).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return c
    }

    @Before fun healthyNetwork() { Proc.isPrivate = false; NetworkMonitor.update(caps(internet = true, validated = true)) }
    @After fun tearDown() { controller?.pause()?.stop()?.destroy(); Proc.isPrivate = false; NetworkMonitor.update(caps(internet = true, validated = true)) }

    // ------------------------------------------------------------------ helpers (same approach as BrowserFlowTest)

    private fun launch(): MainActivity {
        val c = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible()
        controller = c
        ShadowLooper.idleMainLooper()
        return c.get()
    }

    private fun views(v: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) views(v.getChildAt(i), out)
        return out
    }

    private fun texts(a: MainActivity): List<String> = views(a.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }.filter { it.isNotEmpty() }
    private fun webView(a: MainActivity): WebView = views(a.window.decorView).filterIsInstance<WebView>().first()

    private fun open(a: MainActivity, text: String): WebView {
        val f = views(a.window.decorView).filterIsInstance<EditText>().first { it.hint?.toString()?.contains("address") == true }
        f.requestFocus()
        f.setText(text)
        f.onEditorAction(EditorInfo.IME_ACTION_GO)
        ShadowLooper.idleMainLooper()
        return webView(a)
    }

    private fun showNetworkError(a: MainActivity, url: String = "https://example.com/") {
        val tab = a.currentTab!!
        tab.error = ErrorScreen.Network(url, WebViewClient.ERROR_CONNECT, "net::ERR_CONNECTION_TIMED_OUT")
        a.errorChanged(tab)
        ShadowLooper.idleMainLooper()
    }

    // ------------------------------------------------------------------ classification

    @Test
    fun onlyAMissingInternetCapabilityCountsAsOffline() {
        assertEquals(State.OFFLINE, NetworkMonitor.classify(null))
        assertEquals(State.OFFLINE, NetworkMonitor.classify(caps(internet = false, validated = false)))
        assertEquals("Wi-Fi up but never validated is a guess, not a verdict", State.NO_INTERNET, NetworkMonitor.classify(caps(internet = true, validated = false)))
        assertEquals(State.ONLINE, NetworkMonitor.classify(caps(internet = true, validated = true)))
    }

    // ------------------------------------------------------------------ cache policy

    @Test
    fun aHotspotThatFailsAndroidsConnectivityCheckStillLoadsLive() {
        val a = launch()
        NetworkMonitor.update(caps(internet = true, validated = false))
        assertEquals(State.NO_INTERNET, NetworkMonitor.state)
        assertFalse("an unvalidated link must not switch on stale-cache-first loading", NetworkMonitor.cacheFirst)
        val wv = open(a, "example.com")
        assertEquals(WebSettings.LOAD_DEFAULT, wv.settings.cacheMode)
    }

    @Test
    fun noNetworkAtAllServesSavedCopiesFirstAndGoesBackToLiveWhenItReturns() {
        val a = launch()
        val wv = open(a, "example.com")
        assertEquals(WebSettings.LOAD_DEFAULT, wv.settings.cacheMode)

        NetworkMonitor.update(null) // hotspot switched off: the activity reconfigures every live tab
        ShadowLooper.idleMainLooper()
        assertTrue(NetworkMonitor.cacheFirst)
        assertEquals(WebSettings.LOAD_CACHE_ELSE_NETWORK, wv.settings.cacheMode)

        NetworkMonitor.update(caps(internet = true, validated = true))
        ShadowLooper.idleMainLooper()
        assertEquals(WebSettings.LOAD_DEFAULT, wv.settings.cacheMode)
    }

    // ------------------------------------------------------------------ "Open saved copy"

    @Test
    fun savedCopyIsOfferedOnlyWhenAndroidClaimsThereIsNoInternet() {
        val a = launch()
        open(a, "example.com")

        NetworkMonitor.update(caps(internet = true, validated = false))
        showNetworkError(a)
        assertTrue(texts(a).toString(), texts(a).contains("Open saved copy"))
        assertTrue("wording must admit it is Android's claim", texts(a).any { it.contains("Android says") })

        NetworkMonitor.update(caps(internet = true, validated = true))
        showNetworkError(a)
        assertFalse("a validated link has nothing to guess about", texts(a).contains("Open saved copy"))

        NetworkMonitor.update(null)
        showNetworkError(a)
        assertFalse("offline pages were already tried cache-first", texts(a).contains("Open saved copy"))
        assertTrue(texts(a).contains("Wi-Fi settings"))
    }

    @Test
    fun openSavedCopyLoadsFromTheCacheOnlyForThatOneNavigation() {
        val a = launch()
        NetworkMonitor.update(caps(internet = true, validated = false))
        val wv = open(a, "example.com")
        val tab = a.currentTab!!
        showNetworkError(a)

        views(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Open saved copy" }.performClick()
        ShadowLooper.idleMainLooper()
        assertNull("the error page goes away while the saved copy loads", tab.error)
        assertTrue(tab.savedCopyOnce)
        assertEquals(WebSettings.LOAD_CACHE_ONLY, wv.settings.cacheMode)
        assertEquals("https://example.com/", shadowOf(wv).lastLoadedUrl)

        (shadowOf(wv).webViewClient as BrowserWebViewClient).onPageFinished(wv, "https://example.com/")
        assertFalse(tab.savedCopyOnce)
        assertEquals("one-shot: the next load is normal again", WebSettings.LOAD_DEFAULT, wv.settings.cacheMode)
    }

    @Test
    fun aCacheMissOnTheSavedCopyKeepsTheErrorPageAndRestoresNormalLoading() {
        val a = launch()
        NetworkMonitor.update(caps(internet = true, validated = false))
        val wv = open(a, "example.com")
        val tab = a.currentTab!!
        showNetworkError(a)
        views(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Open saved copy" }.performClick()
        ShadowLooper.idleMainLooper()

        (shadowOf(wv).webViewClient as BrowserWebViewClient).mainFrameFailed(wv, "https://example.com/", WebViewClient.ERROR_UNKNOWN, "net::ERR_CACHE_MISS")
        ShadowLooper.idleMainLooper()
        assertFalse(tab.savedCopyOnce)
        assertTrue("still an error page, now with the cache-miss details", tab.error is ErrorScreen.Network)
        assertEquals(WebSettings.LOAD_DEFAULT, wv.settings.cacheMode)
        assertTrue(texts(a).contains("Try again"))
    }

    @Test
    fun anotherNavigationNeverInheritsTheSavedCopyFlag() {
        val a = launch()
        NetworkMonitor.update(caps(internet = true, validated = false))
        val wv = open(a, "example.com")
        val tab = a.currentTab!!
        showNetworkError(a)
        views(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Open saved copy" }.performClick()
        ShadowLooper.idleMainLooper()
        assertEquals(WebSettings.LOAD_CACHE_ONLY, wv.settings.cacheMode)

        open(a, "other.example") // user gives up on the saved copy and types something else mid-load
        assertFalse(tab.savedCopyOnce)
        assertEquals(WebSettings.LOAD_DEFAULT, wv.settings.cacheMode)
    }

    // ------------------------------------------------------------------ HTTPS fallback must not depend on the validation guess

    @Test
    fun anUpgradedHttpSiteWithoutHttpsStillGetsTheWarningOnAnUnvalidatedHotspot() {
        val a = launch()
        NetworkMonitor.update(caps(internet = true, validated = false))
        val wv = open(a, "example.com")
        val tab = a.currentTab!!
        tab.upgradedFrom = "http://old.example/"
        tab.upgradedTo = "https://old.example/"
        (shadowOf(wv).webViewClient as BrowserWebViewClient).mainFrameFailed(wv, "https://old.example/", WebViewClient.ERROR_CONNECT, "net::ERR_CONNECTION_REFUSED")
        assertTrue(tab.error.toString(), tab.error is ErrorScreen.HttpsUnavailable)
    }

    @Test
    fun withNoLinkAtAllAFailedUpgradeIsReportedAsAnOrdinaryNetworkError() {
        val a = launch()
        val wv = open(a, "example.com")
        val tab = a.currentTab!!
        NetworkMonitor.update(null)
        tab.upgradedFrom = "http://old.example/"
        tab.upgradedTo = "https://old.example/"
        (shadowOf(wv).webViewClient as BrowserWebViewClient).mainFrameFailed(wv, "https://old.example/", WebViewClient.ERROR_CONNECT, "net::ERR_INTERNET_DISCONNECTED")
        assertTrue(tab.error.toString(), tab.error is ErrorScreen.Network)
    }
}
