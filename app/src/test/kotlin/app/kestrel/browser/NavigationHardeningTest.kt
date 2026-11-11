package app.kestrel.browser

import android.content.ComponentCallbacks2
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import app.kestrel.browser.data.BrowserDb
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.web.BrowserWebViewClient
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.browser.web.NavPolicy
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSetBuilder
import app.kestrel.core.session.Session
import app.kestrel.core.session.SessionCodec
import app.kestrel.core.session.TabRecord
import app.kestrel.core.url.HttpsPolicy
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File

/** Regression tests for navigation / lifecycle findings of the independent reviews (docs/handoff/REVIEW-2026-11-11.md). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class NavigationHardeningTest {
    private var controller: ActivityController<MainActivity>? = null
    private lateinit var savedEngine: app.kestrel.core.filter.FilterSet

    @Before fun setUp() {
        Proc.isPrivate = false
        val ctx = RuntimeEnvironment.getApplication()
        Engine.start(ctx)
        savedEngine = Engine.filters()
        File(ctx.filesDir, "session.txt").delete()
    }

    @After fun tearDown() {
        controller?.pause()?.stop()?.destroy(); Proc.isPrivate = false
        Engine.swap(savedEngine, "restored")
        Prefs.get(RuntimeEnvironment.getApplication()).httpsMode = HttpsPolicy.Mode.STRICT
    }

    // ------------------------------------------------------------------ helpers

    private class Req(val u: String, val main: Boolean = true, val redirect: Boolean = false, val gesture: Boolean = false) : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(u)
        override fun isForMainFrame() = main
        override fun isRedirect() = redirect
        override fun hasGesture() = gesture
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): MutableMap<String, String> = HashMap()
    }

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

    private fun urlField(a: MainActivity): EditText = views(a.window.decorView).filterIsInstance<EditText>().first { it.hint?.toString()?.contains("address") == true }

    private fun open(a: MainActivity, text: String): WebView {
        val f = urlField(a)
        f.requestFocus(); f.setText(text); f.onEditorAction(EditorInfo.IME_ACTION_GO)
        ShadowLooper.idleMainLooper()
        return views(a.window.decorView).filterIsInstance<WebView>().first()
    }

    private fun client(wv: WebView) = shadowOf(wv).webViewClient as BrowserWebViewClient

    // ------------------------------------------------------------------ C1: redirect targets must be classified like any other main-frame URL

    @Test
    fun aServerRedirectRefreshesTheHostAndShieldsOfThePage() {
        val a = launch(); val wv = open(a, "short.example"); val tab = a.currentTab!!
        assertFalse(client(wv).shouldOverrideUrlLoading(wv, Req("https://www.landing.example/article", redirect = true)))
        assertEquals("subresources of the landing page must be judged against the landing host, not the redirector", "www.landing.example", tab.topHost)
        assertEquals("https://www.landing.example/article", tab.mainUrl)
    }

    @Test
    fun aRedirectToAMalwareHostIsStopped() {
        val a = launch(); val wv = open(a, "short.example"); val tab = a.currentTab!!
        val b = FilterSetBuilder(); b.addAbpList("m", Category.MALWARE, sequenceOf("||landing-malware.example^"))
        Engine.swap(b.build(1L), "test")
        assertTrue(client(wv).shouldOverrideUrlLoading(wv, Req("https://landing-malware.example/x", redirect = true)))
        ShadowLooper.idleMainLooper()
        assertTrue(tab.error.toString(), tab.error is ErrorScreen.Dangerous)
    }

    // ------------------------------------------------------------------ C2: HTTPS upgrade state machine

    @Test
    fun aSiteThatRedirectsHttpsBackToHttpIsDetectedInsteadOfLoopingForever() {
        val a = launch(); val wv = open(a, "example.com"); val tab = a.currentTab!!
        tab.upgradedFrom = "http://legacy.example/"; tab.upgradedTo = "https://legacy.example/"
        assertTrue(client(wv).shouldOverrideUrlLoading(wv, Req("http://legacy.example/", redirect = true)))
        assertTrue("STRICT: ask instead of looping: ${tab.error}", tab.error is ErrorScreen.HttpsUnavailable)
    }

    @Test
    fun inUpgradeModeTheLoopIsBrokenByLoadingTheHttpPage() {
        val a = launch(); val wv = open(a, "example.com"); val tab = a.currentTab!!
        Prefs.get(a).httpsMode = HttpsPolicy.Mode.UPGRADE
        tab.upgradedFrom = "http://legacy.example/"; tab.upgradedTo = "https://legacy.example/"
        assertTrue(client(wv).shouldOverrideUrlLoading(wv, Req("http://legacy.example/", redirect = true)))
        assertTrue(tab.httpOkHosts.contains("legacy.example"))
        assertEquals("http://legacy.example/", shadowOf(wv).lastLoadedUrl)
    }

    @Test
    fun aSuccessfullyUpgradedPageForgetsItWasUpgraded() {
        val a = launch(); val wv = open(a, "example.com"); val tab = a.currentTab!!
        tab.upgradedFrom = "http://x.example/"; tab.upgradedTo = "https://x.example/"
        client(wv).onPageCommitVisible(wv, "https://x.example/")
        assertNull(tab.upgradedTo); assertNull(tab.upgradedFrom)
        // so a later flaky Reload is an ordinary network error, not "this site has no HTTPS"
        client(wv).mainFrameFailed(wv, "https://x.example/", WebViewClient.ERROR_TIMEOUT, "net::ERR_TIMED_OUT")
        assertTrue(tab.error is ErrorScreen.Network)
    }

    // ------------------------------------------------------------------ S3: the address bar only claims what is really loaded, and shows the port

    @Test
    fun aNavigationThatNeverCommitsDoesNotChangeTheAddressBar() {
        val a = launch(); val wv = open(a, "good.example"); val tab = a.currentTab!!
        client(wv).doUpdateVisitedHistory(wv, "https://good.example/", false)
        client(wv).onPageStarted(wv, "https://accounts.lookalike.example:81/", null) // started by the page, black-holed: never commits
        assertEquals("https://good.example/", tab.url)
        client(wv).doUpdateVisitedHistory(wv, "https://accounts.lookalike.example:81/", false) // only now is it the page being shown
        assertEquals("https://accounts.lookalike.example:81/", tab.url)
    }

    @Test
    fun nonDefaultPortsAreShownInTheAddressBar() {
        val a = launch(); val wv = open(a, "10.0.2.2:8000/x"); val tab = a.currentTab!!
        client(wv).doUpdateVisitedHistory(wv, "http://10.0.2.2:8000/x", false)
        urlField(a).clearFocus(); a.tabChanged(tab); ShadowLooper.idleMainLooper()
        assertTrue("shown as '${urlField(a).text}'", urlField(a).text.toString().contains("10.0.2.2:8000"))
        client(wv).doUpdateVisitedHistory(wv, "https://bank.example:8443/login", false)
        a.tabChanged(tab); ShadowLooper.idleMainLooper()
        assertTrue("shown as '${urlField(a).text}'", urlField(a).text.toString().startsWith("bank.example:8443"))
        client(wv).doUpdateVisitedHistory(wv, "https://bank.example:443/login", false)
        a.tabChanged(tab); ShadowLooper.idleMainLooper()
        assertTrue("default port stays hidden: '${urlField(a).text}'", urlField(a).text.toString().startsWith("bank.example/login"))
    }

    // ------------------------------------------------------------------ S13: menus must not feed non-web addresses to a tab

    @Test
    fun onlyWebAddressesAreNavigable() {
        for (ok in listOf("http://a.example/", "https://a.example/x", "HTTPS://A.EXAMPLE/")) assertTrue(ok, NavPolicy.isWebUrl(ok))
        for (bad in listOf("javascript:alert(1)", "data:text/html,hi", "file:///android_asset/x", "content://x", "intent:#Intent;end", "about:blank", "ftp://x", "")) assertFalse(bad, NavPolicy.isWebUrl(bad))
    }

    // ------------------------------------------------------------------ C3: the opener of a live popup must survive memory trimming

    @Test
    fun trimMemoryDoesNotFreezeTheOpenerOfALivePopup() {
        val a = launch(); open(a, "opener.example"); val opener = a.currentTab!!
        a.newPopupTab(opener)
        a.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertNotNull("window.opener would be lost (login / payment popups)", opener.webView)
    }

    // ------------------------------------------------------------------ C7: failed loads are not history

    @Test
    fun onlySuccessfulLoadsAreRecordedInHistory() {
        val a = launch(); open(a, "typo.example"); val tab = a.currentTab!!
        tab.url = "https://typo.example/"
        tab.error = ErrorScreen.Network("https://typo.example/", WebViewClient.ERROR_HOST_LOOKUP, "net::ERR_NAME_NOT_RESOLVED")
        a.pageFinished(tab); Thread.sleep(300)
        assertTrue("a DNS failure is not a visit", BrowserDb(a).history(50).none { it.url.contains("typo.example") })
        tab.error = null
        a.pageFinished(tab); Thread.sleep(300)
        assertTrue(BrowserDb(a).history(50).any { it.url.contains("typo.example") })
    }

    // ------------------------------------------------------------------ C8 / S13: the session file

    @Test
    fun savingBeforeTheSavedSessionWasRestoredCannotOverwriteIt() {
        val ctx = RuntimeEnvironment.getApplication()
        val file = File(ctx.filesDir, "session.txt")
        file.writeText(SessionCodec.encode(Session(listOf(TabRecord(1, "https://one.example/", "One", 1L, 0, false), TabRecord(2, "https://two.example/", "Two", 2L, 0, false)), 2)))
        val before = file.readText()
        val c = Robolectric.buildActivity(MainActivity::class.java).create() // restore() is posted to the main looper and not run yet
        controller = c
        c.get().saveSessionNow(); Thread.sleep(300)
        assertEquals("a one-tab placeholder session must not replace the real one", before, file.readText())
    }

    @Test
    fun credentialsInAUrlAreNotWrittenToTheSessionFile() {
        val ctx = RuntimeEnvironment.getApplication()
        val a = launch(); Thread.sleep(300); ShadowLooper.idleMainLooper() // let restore() run
        a.currentTab!!.url = "https://user:s3cretpass@host.example/x?y=1"
        a.saveSessionNow(); Thread.sleep(300)
        val text = File(ctx.filesDir, "session.txt").readText()
        assertFalse(text, text.contains("s3cretpass"))
        assertTrue(text, text.contains("host.example/x?y=1"))
    }
}
