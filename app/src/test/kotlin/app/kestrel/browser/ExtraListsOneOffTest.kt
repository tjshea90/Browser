package app.kestrel.browser

import app.kestrel.browser.shield.FilterUpdater
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSetBuilder
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test

/** One-off measurement for RX3-B2 (set KESTREL_LISTS_DIR to a folder with the downloaded list files; skipped otherwise). Prints rule counts, compile time and the canary verdict. */
class ExtraListsOneOffTest {
    @Test fun measure() {
        val dir = System.getenv("KESTREL_LISTS_DIR")
        assumeTrue(dir != null)
        fun build(files: List<Pair<String, Int>>): app.kestrel.core.filter.FilterSet {
            val b = FilterSetBuilder()
            for ((name, cat) in files) File(dir, name).bufferedReader().use { r -> b.addAbpList(name, cat, r.lineSequence(), false) }
            return b.build()
        }
        val base = listOf("easylist.txt" to Category.ADS, "easyprivacy.txt" to Category.PRIVACY, "fanboy-cookiemonster.txt" to Category.ANNOYANCE)
        val extra = listOf("hagezi-light-raw.txt" to Category.PRIVACY, "adg_sections_tracking_servers.txt" to Category.PRIVACY, "adg_sections_tracking_servers_firstparty.txt" to Category.PRIVACY,
            "adg_sections_general_url.txt" to Category.PRIVACY, "adg_sections_specific.txt" to Category.PRIVACY)
        var t = System.currentTimeMillis()
        val a = build(base); val ta = System.currentTimeMillis() - t
        t = System.currentTimeMillis()
        val c = build(base + extra); val tc = System.currentTimeMillis() - t
        println("BASE   network=${a.meta.totalNetwork} in ${ta} ms")
        println("EXTRAS network=${c.meta.totalNetwork} in ${tc} ms (+${c.meta.totalNetwork - a.meta.totalNetwork} rules)")
        println("CANARY base=${FilterUpdater.Canaries.problem(null, a)} extras=${FilterUpdater.Canaries.problem(a, c)}")
    }
}
