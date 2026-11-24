package app.kestrel.browser.data

import android.content.Context
import app.kestrel.core.optimizer.Catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** OPT1: Auto-tune can read and write exactly the app settings its catalog names, typed the way the app stores them, and nothing else. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class OptimizerPrefsTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val p get() = Prefs.get(app)

    @Test fun everyCatalogSettingKeyCanBeReadAndWrittenWithEveryValueTheCatalogUses() {
        for (k in Catalog.settingKeys().keys) assertNotNull("$k is readable", p.optGet(k))
        for (e in Catalog.all) for (c in e.settingChanges()) {
            val before = p.optGet(c.prefKey)!!
            assertTrue("${c.prefKey} is writable", p.optSet(c.prefKey, c.value))
            assertEquals("${e.id}: ${c.prefKey} round-trips", c.value, p.optGet(c.prefKey))
            p.optSet(c.prefKey, before)
        }
    }

    @Test fun theCatalogsTypeMatchesTheStoredType() {
        val sp = app.getSharedPreferences("kestrel", Context.MODE_PRIVATE)
        for ((key, isBool) in Catalog.settingKeys()) {
            val v = p.optGet(key)!!
            p.optSet(key, v)
            val stored = sp.all[key]
            if (stored != null) assertTrue("$key is stored as ${if (isBool) "a boolean" else "an int"}, was ${stored.javaClass.simpleName}", if (isBool) stored is Boolean else stored is Int)
        }
    }

    @Test fun aKeyOutsideTheCatalogIsRefused() {
        assertNull(p.optGet("tabs_kept")); assertFalse(p.optSet("tabs_kept", 1)); assertFalse(p.optSet("dark", 1)); assertNull(p.optGet("token"))
    }

    @Test fun theEnginePreferencesOfARunAreKeptAsText() {
        p.optPrefs = "layout.css.stylo-threads=2"
        assertEquals("layout.css.stylo-threads=2", p.optPrefs)
        p.optPrefs = ""
    }
}
