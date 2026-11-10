package app.kestrel.browser

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs the real Activity on the JVM (Android 10 framework): catches crashes in UI construction and lifecycle that lint cannot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BrowserSmokeTest {
    private fun texts(v: View, out: MutableList<String> = ArrayList()): List<String> {
        if (v is TextView && v.text.isNotEmpty()) out.add(v.text.toString())
        if (v is ViewGroup) for (i in 0 until v.childCount) texts(v.getChildAt(i), out)
        return out
    }

    @Test
    fun launchesToNewTabPage() {
        val c = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible()
        val all = texts(c.get().window.decorView)
        assertNotNull(c.get())
        assertTrue("NTP brand missing: $all", all.any { it.contains("Kestrel") })
        assertTrue("search box missing: $all", all.any { it.contains("Search or type address") })
    }
}
