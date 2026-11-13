package app.kestrel.browser.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.kestrel.browser.web.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Owner (2026-11-13): "when I switch to the home screen then look at the active tabs, it still shows the tab as being on the last web site I went to instead of the home screen".
 * A tab with the Home screen laid over its page must be called Home in the tab switcher and in the tab list, and must not show the title, address or avatar of the page behind it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TabGridHomeTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    private val none = object : TabGrid.Listener {
        override fun select(tab: Tab) {}
        override fun close(tab: Tab) {}
        override fun newTab(privateTab: Boolean) {}
        override fun closeAll(privateTab: Boolean) {}
        override fun dismiss() {}
        override fun paneChanged(privateTab: Boolean) {}
    }

    private fun page(id: Long, url: String, title: String, home: Boolean) = Tab(id).apply { this.url = url; this.title = title; showHome = home }

    private fun texts(v: View, out: MutableList<String> = ArrayList()): List<String> {
        if (v is TextView && v.text.isNotEmpty()) out.add(v.text.toString())
        if (v.contentDescription != null) out.add(v.contentDescription.toString())
        if (v is ViewGroup) for (i in 0 until v.childCount) texts(v.getChildAt(i), out)
        return out
    }

    private fun grid(vararg tabs: Tab, selected: Tab): TabGrid {
        val g = TabGrid(ctx, Pal(ctx, false), { it.isPrivateTab }, none)
        g.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(973, View.MeasureSpec.EXACTLY))
        g.layout(0, 0, 600, 973)
        g.open(tabs.toList(), selected)
        return g
    }

    @Test fun aTabOnTheHomeScreenIsCalledHomeNotItsLastPage() {
        val onHome = page(1, "https://example.com/some/movie", "A long movie page title", home = true)
        val other = page(2, "https://example.org/", "Another page", home = false)
        val t = texts(grid(onHome, other, selected = onHome))
        assertTrue(t.toString(), "Home" in t)
        assertFalse("the page behind Home must not be named: $t", t.any { it.contains("A long movie page title") })
        assertTrue("an ordinary tab keeps its title: $t", "Another page" in t)
    }

    @Test fun theCardsOfTheTabsAreWhatTheyAreToldToBe() {
        val blank = Tab(3)
        val onHome = page(1, "https://example.com/x", "Page X", home = true)
        assertEquals("New tab", blank.cardTitle); assertFalse(blank.homeOverPage)
        assertEquals("Home", onHome.cardTitle); assertTrue(onHome.homeOverPage)
        assertEquals("Page X", page(4, "https://example.com/x", "Page X", home = false).cardTitle)
        onHome.showHome = false
        assertEquals("Page X", onHome.cardTitle) // Back from Home: the page's own name again
    }

    @Test fun theTabListNamesAHomeTabHomeToo() {
        val onHome = page(1, "https://example.com/some/movie", "A long movie page title", home = true)
        val panel = TabSwitcher(ctx, Pal(ctx, false), object : TabSwitcher.Listener {
            override fun select(tab: Tab) {}
            override fun close(tab: Tab) {}
            override fun newTab() {}
            override fun dismiss() {}
        })
        panel.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(973, View.MeasureSpec.EXACTLY))
        panel.layout(0, 0, 600, 973)
        panel.bind(listOf(onHome), onHome)
        val t = texts(panel)
        assertTrue(t.toString(), "Home" in t)
        assertFalse(t.toString(), t.any { it.contains("A long movie page title") || it.contains("example.com") })
    }
}
