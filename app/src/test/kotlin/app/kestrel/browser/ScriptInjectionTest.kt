package app.kestrel.browser

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.EditText
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.web.ScriptInjector
import app.kestrel.core.shield.DataSaver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ScriptInjectionTest {
    @Before fun reset() { Proc.isPrivate = false }
    @After fun after() { Proc.isPrivate = false }

    private fun views(v: View, out: MutableList<View> = ArrayList()): List<View> { out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) views(v.getChildAt(i), out); return out }

    private fun open(): Pair<MainActivity, WebView> {
        val a = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible().get()
        val f = views(a.window.decorView).filterIsInstance<EditText>().first { it.hint?.toString()?.contains("address") == true }
        f.requestFocus(); f.setText("example.com"); f.onEditorAction(EditorInfo.IME_ACTION_GO); ShadowLooper.idleMainLooper()
        return a to views(a.window.decorView).filterIsInstance<WebView>().first()
    }

    @Test
    fun configIsValidJavaScriptAndCarriesTheSwitches() {
        assertEquals("var __KST_CFG={\"fp\":1,\"sd\":0,\"pn\":1,\"seed\":-42,\"tok\":7};", ScriptInjector.config(true, false, true, -42, 7))
    }

    @Test
    fun scriptAssetIsShippedAndInjectedWithTheSessionConfigOnFirstPaint() {
        val (a, wv) = open()
        Prefs.get(a).dataSaver = DataSaver.STANDARD
        val client = shadowOf(wv).webViewClient
        client.onPageCommitVisible(wv, "https://example.com/")
        val js = shadowOf(wv).lastEvaluatedJavascript
        assertNotNull("nothing was injected", js)
        assertTrue("config must live inside a wrapper function, not leak as a page global", js!!.startsWith("(function(){var __KST_CFG={") && js.trimEnd().endsWith("})();"))
        assertTrue("a per-page marker token was chosen", a.currentTab!!.injectToken != 0)
        assertTrue("fingerprint protection on by default", js.contains("\"fp\":1"))
        assertTrue("data saver signal on in STANDARD", js.contains("\"sd\":1"))
        assertTrue("the real script follows the config", js.contains("farbleBytes") && js.contains("[native code]"))
    }

    @Test
    fun nothingIsInjectedWhenEveryProtectionIsOff() {
        val (a, wv) = open()
        Prefs.get(a).dataSaver = DataSaver.OFF
        Prefs.get(a).shieldFingerprint = false
        val tab = a.currentTab!!
        tab.shields = Prefs.get(a).globalShields()
        val before = shadowOf(wv).lastEvaluatedJavascript
        ScriptInjector.inject(wv, tab, Prefs.get(a))
        assertEquals("no script when fp, sd and pn are all off", before, shadowOf(wv).lastEvaluatedJavascript)
        assertFalse(Prefs.get(a).shieldFingerprint)
        Prefs.get(a).shieldFingerprint = true
    }
}
