package app.kestrel.browser

import app.kestrel.core.filter.Category
import app.kestrel.core.filter.FilterSetBuilder
import app.kestrel.core.filter.FilterSetCodec
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test

/** One-off memory measurement for RX3-B2 (KESTREL_LISTS_DIR; skipped otherwise). */
class ExtraListsMemTest {
    private fun used(): Long { for (i in 0 until 3) { System.gc(); Thread.sleep(50) }; val r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory() }
    @Test fun measure() {
        val dir = System.getenv("KESTREL_LISTS_DIR"); assumeTrue(dir != null)
        fun build(files: List<Pair<String, Int>>): Pair<Any, Long> {
            val b = FilterSetBuilder()
            var peak = 0L
            for ((name, cat) in files) { File(dir, name).bufferedReader().use { r -> b.addAbpList(name, cat, r.lineSequence(), false) }; peak = maxOf(peak, used()) }
            val fs = b.build()
            return fs to peak
        }
        val base = listOf("easylist.txt" to Category.ADS, "easyprivacy.txt" to Category.PRIVACY, "fanboy-cookiemonster.txt" to Category.ANNOYANCE)
        val extra = listOf("hagezi-light-raw.txt" to Category.PRIVACY, "adg_sections_tracking_servers.txt" to Category.PRIVACY, "adg_sections_tracking_servers_firstparty.txt" to Category.PRIVACY, "adg_sections_general_url.txt" to Category.PRIVACY, "adg_sections_specific.txt" to Category.PRIVACY)
        val u0 = used()
        val (a, peakA) = build(base)
        val ua = used()
        println("MEM base: before=${u0 / 1024 / 1024} MB, builder before build() ~${peakA / 1024 / 1024} MB, retained after build=${ua / 1024 / 1024} MB")
        val bytesA = FilterSetCodec.write(a as app.kestrel.core.filter.FilterSet).size
        val (c, peakC) = build(base + extra)
        val uc = used()
        println("MEM extras: builder before build() ~${peakC / 1024 / 1024} MB, retained after build=${uc / 1024 / 1024} MB (a still held: ${ua / 1024 / 1024})")
        println("MEM image bytes: base=${bytesA / 1024} KB extras=${FilterSetCodec.write(c as app.kestrel.core.filter.FilterSet).size / 1024} KB")
    }
}
