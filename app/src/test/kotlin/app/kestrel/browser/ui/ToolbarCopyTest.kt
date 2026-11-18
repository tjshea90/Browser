package app.kestrel.browser.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import app.kestrel.browser.web.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** UC1 (owner 2026-11-18: "include a fast way to copy any url in the address bar. For example, a copy button pops up when i press on the url bar"). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ToolbarCopyTest {
    private class L : ToolbarListener {
        var focus = false
        override fun onBack() {}; override fun onForward() {}; override fun onReloadOrStop() {}; override fun onShields(anchor: View) {}
        override fun onTabs() {}; override fun onMenu(anchor: View) {}; override fun onSubmit(text: String) {}
        override fun onUrlFocus(focused: Boolean) { focus = focused }
        override fun onUrlText(text: String) {}
    }

    private val full = "https://www.shop.example/catalog/item?id=42&ref=mail#reviews"

    private fun open(): Pair<Toolbar, Activity> {
        val a = Robolectric.buildActivity(Activity::class.java).setup().get()
        val bar = Toolbar(a, Pal(a), L())
        val root = FrameLayout(a); a.setContentView(root)
        root.addView(bar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        val tab = Tab(1); tab.url = full
        bar.bind(tab, 1, false)
        return bar to a
    }

    private fun clip(a: Context): String? = a.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.text?.toString()

    @Test fun theCopyButtonIsNotThereWhileTheBarIsAtRest() {
        val (bar, _) = open()
        assertEquals(View.GONE, bar.copyButton.visibility)
    }

    @Test fun pressingTheBarShowsTheButtonAndOneTapCopiesTheWholeAddressNotTheShortText() {
        val (bar, a) = open()
        assertFalse("the bar shows the short form at rest", bar.urlField.text.toString() == full)
        bar.urlField.setText(full)                        // what BrowserActivity.onUrlFocus puts in: the whole address
        bar.urlField.requestFocus()
        assertEquals(View.VISIBLE, bar.copyButton.visibility)
        bar.copyButton.performClick()
        assertEquals(full, clip(a))
        assertTrue("the keyboard focus stays in the bar", bar.urlField.hasFocus())
    }

    @Test fun whatWasTypedOrPastedIsWhatIsCopied() {
        val (bar, a) = open()
        bar.urlField.requestFocus(); bar.urlField.setText("https://typed.example/x?y=1")
        bar.copyButton.performClick()
        assertEquals("https://typed.example/x?y=1", clip(a))
    }

    @Test fun theButtonGoesWhenTheBarLosesFocusOrHasNothingInIt() {
        val (bar, _) = open()
        bar.urlField.setText(full); bar.urlField.requestFocus()
        assertEquals(View.VISIBLE, bar.copyButton.visibility)
        bar.urlField.setText("")
        assertEquals("nothing to copy", View.GONE, bar.copyButton.visibility)
        bar.urlField.setText(full)
        assertEquals(View.VISIBLE, bar.copyButton.visibility)
        bar.urlField.clearFocus()
        assertEquals(View.GONE, bar.copyButton.visibility)
    }

    @Test fun theButtonSaysCopiedForAMomentAndThenCopyAgain() {
        val (bar, _) = open()
        bar.urlField.setText(full); bar.urlField.requestFocus()
        bar.copyButton.performClick()
        assertTrue(bar.copyButton.contentDescription.toString().contains("copied", true))
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1500))
        assertEquals("Copy the address", bar.copyButton.contentDescription.toString())
    }
}
