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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** UC1 (owner 2026-11-18): the Copy button in the address bar the app really has (the bottom dock). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DockCopyTest {
    private class L : DockListener {
        override fun onBack() {}; override fun onForward() {}; override fun onHome() {}; override fun onNewTab() {}; override fun onTabs() {}; override fun onDownload() {}
        override fun onReloadOrStop() {}; override fun onShields(anchor: View) {}; override fun onMenu(anchor: View) {}; override fun onSubmit(text: String) {}
        override fun onUrlFocus(focused: Boolean) {}; override fun onUrlText(text: String) {}
    }

    private val full = "https://www.shop.example/catalog/item?id=42&ref=mail#reviews"

    private fun open(): Pair<BottomDock, Activity> {
        val a = Robolectric.buildActivity(Activity::class.java).setup().get()
        val dock = BottomDock(a, Pal(a), L())
        val root = FrameLayout(a); a.setContentView(root)
        root.addView(dock, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        val tab = Tab(1); tab.url = full
        dock.bind(tab, 1, false, false)
        return dock to a
    }

    private fun clip(a: Context): String? = a.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.text?.toString()

    @Test fun theButtonIsNotThereWhileTheBarIsAtRest() {
        val (dock, _) = open()
        assertEquals(View.GONE, dock.copyChip.visibility)
    }

    @Test fun pressingTheBarShowsTheButtonAndOneTapCopiesTheWholeAddress() {
        val (dock, a) = open()
        assertFalse("at rest the bar shows the short form", dock.urlField.text.toString() == full)
        dock.urlField.setText(full)                      // what BrowserActivity.onUrlFocus puts in: the whole address
        dock.urlField.requestFocus()
        assertEquals(View.VISIBLE, dock.copyChip.visibility)
        dock.copyChip.performClick()
        assertEquals(full, clip(a))
        assertTrue("the keyboard focus stays in the bar", dock.urlField.hasFocus())
    }

    @Test fun whatWasTypedOrPastedIsWhatIsCopied() {
        val (dock, a) = open()
        dock.urlField.requestFocus(); dock.urlField.setText("https://typed.example/x?y=1")
        dock.copyChip.performClick()
        assertEquals("https://typed.example/x?y=1", clip(a))
    }

    @Test fun theButtonGoesWhenTheBarLosesFocusOrHasNothingInIt() {
        val (dock, _) = open()
        dock.urlField.setText(full); dock.urlField.requestFocus()
        assertEquals(View.VISIBLE, dock.copyChip.visibility)
        dock.urlField.setText("")
        assertEquals("nothing in the bar: nothing to copy", View.GONE, dock.copyChip.visibility)
        dock.urlField.setText(full)
        assertEquals(View.VISIBLE, dock.copyChip.visibility)
        val other = android.widget.EditText(dock.context); (dock.parent as FrameLayout).addView(other)
        other.requestFocus()                              // the finger went elsewhere: the bar lost the focus
        assertFalse(dock.urlField.hasFocus())
        assertEquals(View.GONE, dock.copyChip.visibility)
    }

    @Test fun theButtonSaysCopiedForAMomentAndThenCopyAgain() {
        val (dock, _) = open()
        dock.urlField.setText(full); dock.urlField.requestFocus()
        dock.copyChip.performClick()
        assertTrue(dock.copyChip.contentDescription.toString().contains("copied", true))
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1500))
        assertEquals("Copy the address", dock.copyChip.contentDescription.toString())
    }

    @Test fun theButtonIsInsideTheAddressPillNextToTheShieldChip() {
        val (dock, _) = open()
        val parent = dock.copyChip.parent as android.view.ViewGroup
        assertTrue("same row as the address text", (0 until parent.childCount).any { parent.getChildAt(it) === dock.urlField })
    }
}
