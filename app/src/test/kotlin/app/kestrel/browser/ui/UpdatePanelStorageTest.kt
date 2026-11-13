package app.kestrel.browser.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.update.UpdateManager
import app.kestrel.browser.update.UpdateManager.Phase
import app.kestrel.core.update.UpdateLogic
import app.kestrel.core.update.UpdateOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The "not enough free storage" sheet with the numbers of the owner's tablet (alpha27 Diagnostics, 2026-11-13: 1.62 GB free of 27.75 GB, this app 94.4 + 131.7 libraries + 62.3 data + 228.1 cache MB):
 * the in-place update needs about 3.1 GB, an uninstall gives back only about 0.5 GB, so "install the other way" must NOT be offered (it would leave no browser on the tablet).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UpdatePanelStorageTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val mib = 1L shl 20
    private fun m(x: Double) = (x * mib).toLong()
    private val offer = UpdateOffer("kestrel-g-v0.3.0-alpha28", "0.3.0-alpha28", 127, "kestrel-g-0.3.0-alpha28.apk", "https://example.invalid/a.apk", m(94.4), null, "", "")

    private class Acts : UpdatePanel.Actions {
        var cleared = 0; var downloads = 0; var storage = 0
        override fun openPage(url: String) {}
        override fun download() {}
        override fun openInstallSettings() {}
        override fun openStorageSettings() { storage++ }
        override fun clearCache(done: (Long) -> Unit) { cleared++; done(0L) }
        override fun openDownloads() { downloads++ }
    }

    private fun views(v: View, out: MutableList<TextView> = ArrayList()): List<TextView> {
        if (v is TextView) out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) views(v.getChildAt(i), out)
        return out
    }
    private fun texts(p: UpdatePanel) = views(p).map { it.text.toString() }
    private fun button(p: UpdatePanel, startsWith: String): TextView? = views(p).firstOrNull { it.isClickable && it.text.toString().startsWith(startsWith) }

    private fun state(otherFreeAfter: Long) = UpdateManager.State(
        Phase.NEEDS_SPACE, offer, shortBy = m(1460.0), freeNow = m(1.62 * 1024), fileKept = false, installedBytes = m(370.0), reserveBytes = m(2.775 * 1024),
        needInPlace = m(3116.0), ownBytes = m(516.5), cacheBytes = m(228.1), otherFreeAfter = otherFreeAfter, otherNeed = m(3210.0),
    )

    private fun panel(s: UpdateManager.State, acts: Acts): UpdatePanel {
        UpdateManager.setStateForTest(s)
        return UpdatePanel(ctx, Pal(ctx), Prefs.get(ctx), acts).also { it.renderForTest() }
    }

    @Test fun theOwnersNumbersAreShownAndTheCacheCanBeCleared() {
        val acts = Acts()
        val p = panel(state(otherFreeAfter = m(2081.0)), acts)
        val t = texts(p)
        assertTrue(t.toString(), t.any { it == "Not enough free storage to update" })
        assertTrue(t.toString(), t.any { it.startsWith("Free now: " + UpdateLogic.formatSize(m(1.62 * 1024))) && it.contains(UpdateLogic.formatSize(m(3116.0))) && it.contains("more than you have") })
        val clear = button(p, "Clear cache (" + UpdateLogic.formatSize(m(228.1)))
        assertNotNull("Clear cache button: $t", clear)
        clear!!.performClick()
        assertEquals(1, acts.cleared)
        assertNotNull(button(p, "Try again")); assertNotNull(button(p, "Storage settings"))
    }

    @Test fun installingTheOtherWayIsNotOfferedWhenTheUninstallWouldNotFreeEnough() {
        val p = panel(state(otherFreeAfter = m(2081.0)), Acts())     // 2081 MB free after an uninstall against 3210 needed
        val t = texts(p)
        assertTrue(t.toString(), t.any { it.startsWith("Uninstalling first would not help yet") })
        assertEquals("no way to start an uninstall that cannot work", null, button(p, "Install the other way"))
    }

    @Test fun theOtherWayIsOfferedAndExplainedWhenItFits() {
        val p = panel(state(otherFreeAfter = m(3300.0)), Acts())     // enough after the uninstall
        assertTrue(texts(p).toString(), texts(p).any { it.startsWith("Another way that works now") })
        val way = button(p, "Install the other way")
        assertNotNull(way)
        way!!.performClick()
        val t = texts(p)
        assertTrue(t.toString(), t.any { it.contains("Back up & restore") })        // the uninstall loses the browser's data unless it was backed up first
        assertNotNull(button(p, "Save and uninstall"))
        assertFalse(t.toString(), t.any { it.startsWith("Not yet.") })
    }
}
