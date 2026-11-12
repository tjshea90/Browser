package app.kestrel.browser

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.download.PhoneBridge
import app.kestrel.browser.ui.Pal
import app.kestrel.browser.ui.SettingsListener
import app.kestrel.browser.ui.SettingsView
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Owner, 2026-11-12: the menu switch was off but Settings still showed "Load pages through the phone" switched on. One switch, one truth. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsPhoneSwitchTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()

    private val listener = object : SettingsListener {
        override fun closeSettings() {}
        override fun prefsChanged() {}
        override fun clearBrowsingData() {}
        override fun resetSiteShields() {}
        override fun updateFiltersNow() {}
        override fun resetDataCounter() {}
        override fun filterStatus() = ""
        override fun dataUsed() = ""
        override fun aboutLines() = emptyList<Pair<String, String>>()
    }

    private fun texts(v: View, out: MutableList<String> = ArrayList()): List<String> {
        if (v is TextView && v.text.isNotEmpty()) out.add(v.text.toString())
        if (v is ViewGroup) for (i in 0 until v.childCount) texts(v.getChildAt(i), out)
        return out
    }

    @After fun reset() { PhoneBridge.setMaster(ctx, false) }

    @Test fun underAnOffHelperTheSubSwitchesAreNotThere() {
        val p = Prefs.get(ctx)
        p.phoneMode = 0; p.pagesViaPhone = true // the state an older build left behind after the menu switch was turned off
        val t = texts(SettingsView(ctx, Pal(ctx, false), p, listener))
        assertTrue("the one switch is there", t.contains("Phone helper"))
        assertFalse("no pages switch under an off helper", t.contains("Load pages through the phone"))
        assertFalse(t.contains("Downloads and videos"))
    }

    @Test fun underAnOnHelperTheFineChoicesAppear() {
        PhoneBridge.setMaster(ctx, true)
        val t = texts(SettingsView(ctx, Pal(ctx, false), Prefs.get(ctx), listener))
        assertTrue(t.contains("Phone helper")); assertTrue(t.contains("Load pages through the phone")); assertTrue(t.contains("Downloads and videos"))
    }
}
