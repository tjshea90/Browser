package app.kestrel.browser

import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.JsResult
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.web.ErrorScreen
import app.kestrel.browser.web.Tab
import app.kestrel.core.shield.DataSaver
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
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ActivityLogicTest {
    private val controllers = ArrayList<ActivityController<MainActivity>>()

    @Before fun reset() { Proc.isPrivate = false }
    @After fun tearDown() { for (c in controllers) try { c.pause().stop().destroy() } catch (e: Exception) {}; Proc.isPrivate = false }

    private fun launch(intent: Intent? = null): MainActivity {
        val c = if (intent == null) Robolectric.buildActivity(MainActivity::class.java) else Robolectric.buildActivity(MainActivity::class.java, intent)
        controllers.add(c)
        c.create().start().resume().visible()
        ShadowLooper.idleMainLooper()
        return c.get()
    }

    private fun views(v: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) views(v.getChildAt(i), out); return out
    }
    private fun texts(a: Activity) = views(a.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }

    private fun go(a: Activity, text: String) {
        val f = views(a.window.decorView).filterIsInstance<EditText>().first { it.hint?.toString()?.contains("address") == true }
        f.requestFocus(); f.setText(text); f.onEditorAction(EditorInfo.IME_ACTION_GO); ShadowLooper.idleMainLooper()
    }

    private fun waitFor(timeoutMs: Long = 3000, cond: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) { ShadowLooper.idleMainLooper(); if (cond()) return true; Thread.sleep(25) }
        ShadowLooper.idleMainLooper(); return cond()
    }

    // ------------------------------------------------------------------ memory policy

    @Test
    fun onlyTwoWebViewsStayAliveOnALowRamDevice() {
        val a = launch()
        shadowOf(a.getSystemService(android.app.ActivityManager::class.java)).setIsLowRamDevice(true)
        for (n in 1..4) {
            if (n > 1) a.newTabAndShow()
            ShadowLooper.idleMainLooper()
            go(a, "site$n.example")
        }
        val live = a.allTabs.count { it.webView != null }
        assertEquals("4 tabs, but at most 2 live WebViews", 4, a.allTabs.size)
        assertTrue("live WebViews: $live", live <= 2)
        assertNotNull("the selected tab must stay live", a.currentTab!!.webView)
        // the hibernated tabs keep what is needed to come back
        val sleeping = a.allTabs.filter { it.webView == null }
        assertTrue(sleeping.isNotEmpty() && sleeping.all { it.url.startsWith("https://site") })
    }

    // ------------------------------------------------------------------ session

    @Test
    fun sessionSurvivesARelaunchWithoutLoadingBackgroundTabs() {
        val a = launch()
        go(a, "first.example")
        a.newTabAndShow()
        go(a, "second.example")
        val file = File(a.filesDir, "session.txt")
        // saving is deliberately refused until the previous session was restored (it could overwrite it with a one-tab placeholder)
        assertTrue("restore ran", waitFor(20_000) { org.robolectric.shadows.ShadowLooper.idleMainLooper(); a.sessionRestored }) // the single IO thread may still be busy with an earlier test (engine load) on a loaded machine
        a.saveSessionNow()
        assertTrue("session file written", waitFor(20_000) { file.isFile && file.readText().contains("second.example") })
        val text = file.readText()
        assertTrue(text, text.contains("https://first.example") && text.contains("https://second.example"))

        val b = launch()
        assertTrue("tabs restored", waitFor(20_000) { b.allTabs.size == 2 })
        val restored = b.allTabs
        assertEquals(listOf("https://first.example", "https://second.example"), restored.map { it.url })
        // only the SELECTED tab has a WebView: no network traffic at startup for the other tab
        assertEquals(1, restored.count { it.webView != null })
        assertNotNull(b.currentTab!!.webView)
        assertTrue("selected tab came back", b.currentTab!!.url == "https://second.example")
        assertNotNull("the other tab waits for its turn", restored.first { it.url.contains("first") }.pendingLoad)
    }

    @Test
    fun privateWindowNeverWritesASession() {
        Proc.isPrivate = true
        val c = Robolectric.buildActivity(PrivateActivity::class.java).create().start().resume().visible()
        val a = c.get()
        go(a, "secret.example")
        c.pause().stop()
        Thread.sleep(200)
        assertFalse("private windows leave no tab list", File(a.filesDir, "session.txt").exists())
        c.destroy()
    }

    // ------------------------------------------------------------------ crash recovery

    @Test
    fun rendererLossRecoversFromCacheAndGivesUpAfterRepeatedCrashes() {
        val a = launch()
        go(a, "crashy.example")
        val tab = a.currentTab!!
        val dead = tab.webView!!
        a.rendererGone(tab, dead, true)
        ShadowLooper.idleMainLooper()
        assertNotNull("a fresh WebView replaces the dead one", tab.webView)
        assertTrue(tab.webView !== dead)
        assertNull(tab.error)
        a.rendererGone(tab, tab.webView!!, false)
        a.rendererGone(tab, tab.webView!!, false)
        ShadowLooper.idleMainLooper()
        assertTrue("third crash in 15 s shows the error screen instead of looping", tab.error is ErrorScreen.Crashed)
        assertTrue(texts(a).any { it.contains("This page stopped working") })
    }

    // ------------------------------------------------------------------ dialogs

    @Test
    fun javaScriptDialogSpamGetsAStopCheckbox() {
        val a = launch()
        go(a, "spam.example")
        val tab = a.currentTab!!
        fun fire(onCancel: () -> Unit = {}): AlertDialog {
            a.jsDialog(tab, "https://spam.example/", "Hello", null, false, { }, onCancel); ShadowLooper.idleMainLooper()
            return ShadowAlertDialog.getLatestAlertDialog()
        }
        fire().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        fire().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        val third = fire()
        val box = views(third.window!!.decorView).filterIsInstance<CheckBox>()
        assertEquals("the third dialog offers to stop the page", 1, box.size)
        assertTrue(box[0].text.toString().contains("Stop this page"))
        box[0].isChecked = true
        third.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        ShadowLooper.idleMainLooper() // button clicks reach the listener through the (paused) main looper
        var cancelled = false
        a.jsDialog(tab, "https://spam.example/", "again", null, false, { }, { cancelled = true })
        assertTrue("further dialogs are suppressed silently", cancelled)
    }

    @Test
    fun dialogsFromBackgroundTabsAreCancelled() {
        val a = launch()
        go(a, "front.example")
        var cancelled = false
        a.jsDialog(Tab(555), "https://back.example/", "hi", null, false, { }, { cancelled = true })
        assertTrue(cancelled)
    }

    // ------------------------------------------------------------------ downloads and external launches

    @Test
    fun bigDownloadsOnAMeteredHotspotAskFirst() {
        val a = launch()
        go(a, "files.example")
        Prefs.get(a).dataSaver = DataSaver.STANDARD
        a.download(a.currentTab!!, "https://files.example/big.zip", null, null, "application/zip", 200L * 1024 * 1024)
        ShadowLooper.idleMainLooper()
        val d = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(d)
        val t = views(d.window!!.decorView).filterIsInstance<TextView>().joinToString(" | ") { it.text }
        assertTrue(t, t.contains("Download this file?") && t.contains("metered") && t.contains("big.zip"))
    }

    @Test
    fun pagesCannotLaunchAppsOnTheirOwn() {
        val a = launch()
        go(a, "evil.example")
        val tab = a.currentTab!!
        assertTrue("handled (swallowed)", a.externalLaunch(tab, "intent://x#Intent;scheme=myapp;end", userGesture = false))
        ShadowLooper.idleMainLooper()
        assertNull("no dialog and no app launch without a gesture", ShadowAlertDialog.getLatestAlertDialog())
        assertNull(shadowOf(a).nextStartedActivity)
        assertTrue(a.externalLaunch(tab, "intent://x#Intent;scheme=myapp;end", userGesture = true))
        ShadowLooper.idleMainLooper()
        assertNotNull("with a tap, the user is asked first", ShadowAlertDialog.getLatestAlertDialog())
        assertNull(shadowOf(a).nextStartedActivity)
    }

    // ------------------------------------------------------------------ back handling

    @Test
    fun backWalksOutOfPanelsThenPagesThenToTheNewTabPage() {
        val a = launch()
        go(a, "example.com")
        assertNotNull(a.currentTab!!.webView)
        a.onBackPressed() // no history in the shadow WebView: a loaded page with nowhere to go returns to the new-tab page
        ShadowLooper.idleMainLooper()
        assertTrue(a.currentTab!!.isNewTab)
        assertTrue(texts(a).any { it.contains("Search or type address") })
    }

    @Test
    fun newIntentWhileRunningOpensInTheRunningBrowser() {
        val a = launch()
        controllers.last().newIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://later.example/p")))
        ShadowLooper.idleMainLooper()
        assertEquals("https://later.example/p", a.currentTab!!.url)
    }
}
