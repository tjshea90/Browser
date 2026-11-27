package app.kestrel.browser

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.ui.Pal
import app.kestrel.browser.ui.SettingsListener
import app.kestrel.browser.ui.SettingsView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * ND1 (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"): the newest live-diagnostics upload said `ui STALL 2360ms` while Settings opened: every row of every section
 * (about 130, each with a long explanation) was built and measured on the UI thread of a 4 x Cortex-A53 tablet, on every open and on every rebuild after a switch. A section's rows are now built only while the
 * section is open. These tests pin that: a fresh Settings holds the headers only, a section builds when it is opened (and not before), a tap on the header opens and closes it, and what was open stays open
 * through a rebuild and for the next Settings screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsLazyTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private var filterStatusCalls = 0
    private var aboutCalls = 0

    private val listener = object : SettingsListener {
        override fun closeSettings() {}
        override fun prefsChanged() {}
        override fun clearBrowsingData() {}
        override fun resetSiteShields() {}
        override fun updateFiltersNow() {}
        override fun resetDataCounter() {}
        override fun filterStatus(): String { filterStatusCalls++; return "status" }
        override fun dataUsed() = ""
        override fun aboutLines(): List<Pair<String, String>> { aboutCalls++; return listOf("Kestrel" to "Version test") }
    }

    @Before fun fresh() { SettingsView.forgetOpenSections() }
    @After fun clean() { SettingsView.forgetOpenSections() }

    private fun view() = SettingsView(ctx, Pal(ctx, false), Prefs.get(ctx), listener)

    private fun texts(v: View, out: MutableList<String> = ArrayList()): List<String> {
        if (v is TextView && v.text.isNotEmpty()) out.add(v.text.toString())
        if (v is ViewGroup) for (i in 0 until v.childCount) texts(v.getChildAt(i), out)
        return out
    }

    private fun count(v: View): Int = 1 + (if (v is ViewGroup) (0 until v.childCount).sumOf { count(v.getChildAt(it)) } else 0)

    /** Visible as far as the view's own tree goes (the test view is never attached to a window, so View.isShown() would say no to everything). */
    private fun shown(v: View, top: View): Boolean {
        var c: View? = v
        while (c != null) { if (c.visibility != View.VISIBLE) return false; if (c === top) return true; c = c.parent as? View }
        return true
    }

    private fun find(v: View, pred: (View) -> Boolean): View? {
        if (pred(v)) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), pred)?.let { return it }
        return null
    }

    @Test fun aFreshSettingsHoldsTheHeadersOnlyAndTheSectionsAreAllThere() {
        val v = view()
        val titles = v.sectionTitles()
        for (t in listOf("Search", "Privacy & security (defaults for every site)", "Data saver (off: this hotspot is unmetered)", "Downloads", "Appearance", "Filter lists", "Secure DNS", "Back up & restore", "About"))
            assertTrue("section $t is listed", titles.contains(t))
        val t = texts(v)
        assertTrue("the headers show", t.contains("SEARCH") && t.contains("FILTER LISTS") && t.contains("ABOUT"))
        assertFalse("no row of a closed section is built", t.contains("Search engine") || t.contains("Update now") || t.contains("Reset data counter"))
        assertEquals("nothing of a closed section ran (no Filter lists status asked for)", 0, filterStatusCalls)
        assertEquals("nor the About lines", 0, aboutCalls)
    }

    @Test fun aClosedSettingsIsFarSmallerThanAnOpenOne() {
        val closed = count(view())
        val all = view().also { it.openAll() }
        val open = count(all)
        assertTrue("closed $closed views against $open when everything is open (the web build has the fewest rows; Kestrel G has about twice as many): at least 4 times smaller", closed * 4 < open)
        assertTrue("a closed Settings is a few dozen views, not hundreds ($closed)", closed < 150)
    }

    @Test fun aSectionBuildsWhenOpenedAndNotBefore() {
        val v = view()
        assertEquals(0, filterStatusCalls)
        v.openSection("Filter lists")
        assertEquals("the section's rows ran once", 1, filterStatusCalls)
        val t = texts(v)
        assertTrue(t.contains("Update now") && t.contains("Status"))
        assertFalse("the other sections stay unbuilt", t.contains("Search engine"))
    }

    @Test fun aTapOnTheHeaderOpensAndClosesItWithoutBuildingTwice() {
        val v = view()
        val head = find(v) { it.contentDescription?.toString()?.startsWith("Filter lists, closed") == true }
        assertNotNull("the header says it is closed and can be tapped", head)
        head!!.performClick()
        assertEquals(1, filterStatusCalls)
        val update = find(v) { it is TextView && it.text == "Update now" }
        assertNotNull(update)
        assertTrue("the rows show after the tap", shown(update!!, v))
        assertTrue(head.contentDescription.toString().startsWith("Filter lists, open"))
        head.performClick()
        assertFalse("and are hidden by the second tap", shown(update, v))
        head.performClick()
        assertTrue("and show again", shown(update, v))
        assertEquals("the rows were built once, not on every tap", 1, filterStatusCalls)
    }

    @Test fun whatWasOpenStaysOpenThroughARebuildAndForTheNextSettingsScreen() {
        val v = view()
        v.openSection("Search")
        assertTrue(texts(v).contains("Search engine"))
        v.rebuild()
        assertTrue("a rebuild after a switch keeps the section open", texts(v).contains("Search engine"))
        assertTrue("the next Settings screen of this run opens it too", texts(view()).contains("Search engine"))
        SettingsView.forgetOpenSections()
        assertFalse("and a new run starts closed", texts(view()).contains("Search engine"))
    }

    @Test fun everySectionOpensWithoutAnErrorAndHasRows() {
        val v = view()
        for (title in v.sectionTitles()) {
            v.closeAll(); v.openSection(title)
            val head = find(v) { it.contentDescription?.toString()?.startsWith("$title, open") == true }
            assertNotNull("the header of $title is open", head)
            assertTrue("$title has rows: ${texts(v).size} texts", texts(v).size > 2)
        }
    }

    @Test fun theKeepReadyChoiceShowsOnlyInTheBuildThatHasItAndDefaultsToOneHour() {
        val old = app.kestrel.browser.Flavor.hasKeepReady
        val p = Prefs.get(ctx)
        val kept = p.keepReady
        try {
            app.kestrel.browser.Flavor.hasKeepReady = false
            val without = view(); without.openSection("Appearance")
            assertFalse("the web build has no such service: no row", texts(without).contains("Keep the page ready when I leave"))
            app.kestrel.browser.Flavor.hasKeepReady = true
            val with = view(); with.openSection("Appearance")
            val t = texts(with)
            assertTrue("Kestrel G shows the row", t.contains("Keep the page ready when I leave"))
            p.keepReady = app.kestrel.core.mem.KeepReady.DEFAULT
            val shown = view().also { it.openSection("Appearance") }
            assertTrue("and says 1 hour by default", texts(shown).any { it.startsWith("1 hour (default)") })
            p.keepReady = 99
            assertEquals("a value outside the choices is kept inside them", 3, p.keepReady)
            p.keepReady = -5
            assertEquals(0, p.keepReady)
        } finally { app.kestrel.browser.Flavor.hasKeepReady = old; p.keepReady = kept }
    }

    @Test fun settingsMeasuresLaysOutAndDrawsClosedAndOpenWithoutAnError() {
        for (open in listOf(false, true)) {
            val v = view()
            if (open) v.openAll()
            v.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, v.measuredWidth, v.measuredHeight)
            v.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(600, 900, android.graphics.Bitmap.Config.ARGB_8888)))      // runs the chevrons' onDraw too
            assertEquals(600, v.measuredWidth)
        }
    }
}
