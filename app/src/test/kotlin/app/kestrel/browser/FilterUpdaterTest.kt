package app.kestrel.browser

import android.content.Context
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.shield.Engine
import app.kestrel.browser.shield.FetchResult
import app.kestrel.browser.shield.Fetcher
import app.kestrel.browser.shield.FilterSources
import app.kestrel.browser.shield.FilterUpdater
import app.kestrel.browser.shield.ListSource
import app.kestrel.core.filter.Category
import app.kestrel.core.filter.RequestType
import app.kestrel.core.filter.Verdict
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class FilterUpdaterTest {
    private lateinit var ctx: Context
    private lateinit var original: app.kestrel.core.filter.FilterSet

    private val sources = listOf(
        ListSource("big", "https://lists.example/big.txt", Category.ADS),
        ListSource("small", "https://lists.example/small.txt", Category.PRIVACY),
    )

    @Before
    fun setUp() {
        ctx = org.robolectric.RuntimeEnvironment.getApplication()
        Engine.start(ctx)
        original = Engine.filters()
        File(ctx.filesDir, "filters").let { if (it.exists()) it.deleteRecursively() }
        Prefs.get(ctx).updateUnmeteredOnly = false
    }

    @After fun restore() { Engine.swap(original, "restored") }

    private fun listText(prefix: String, n: Int) = buildString {
        append("! Title: test list\n[Adblock Plus 2.0]\n")
        for (i in 0 until n) append("||").append(prefix).append(i).append(".example^\n")
    }

    private class FakeFetcher(val bodies: MutableMap<String, String>, val notModified: MutableSet<String> = HashSet(), val failing: MutableSet<String> = HashSet()) : Fetcher {
        var calls = 0
        override fun fetch(url: String, etag: String?, lastModified: String?, maxBytes: Long, out: File): FetchResult {
            calls++
            if (url in failing) return FetchResult.Fail("HTTP 503")
            if (url in notModified && etag != null) return FetchResult.NotModified
            val body = bodies[url] ?: return FetchResult.Fail("HTTP 404")
            out.writeText(body)
            return FetchResult.Ok("\"etag-${body.length}\"", "Mon, 01 Jan 2026 00:00:00 GMT", body.length.toLong())
        }
    }

    private fun fresh() = FakeFetcher(mutableMapOf("https://lists.example/big.txt" to listText("adhost", 25_000), "https://lists.example/small.txt" to listText("trk", 500)))

    private fun run(f: Fetcher, manual: Boolean = true, online: Boolean = true, metered: Boolean = true, src: List<ListSource> = sources) =
        FilterUpdater.run(ctx, manual, src, f, { online }, { metered })

    @Test
    fun firstRunDownloadsCompilesInstallsAndSwapsTheEngine() {
        val r = run(fresh())
        assertTrue(r.message, r is FilterUpdater.Result.Updated)
        assertTrue(File(ctx.filesDir, "filters/current.kfs").isFile)
        assertTrue(Verdict.isBlocked(Engine.filters().check("https://adhost7.example/x.js", "page.example", RequestType.SCRIPT)))
        assertEquals("updated list", Engine.sourceDescription)
    }

    @Test
    fun secondRunWithNothingNewIsACheapNoOp() {
        val f = fresh()
        assertTrue(run(f) is FilterUpdater.Result.Updated)
        val kfs = File(ctx.filesDir, "filters/current.kfs")
        val stamp = kfs.lastModified()
        f.notModified.addAll(sources.map { it.url })
        val r = run(f)
        assertTrue(r.message, r is FilterUpdater.Result.Unchanged)
        assertEquals("no recompile when nothing changed", stamp, kfs.lastModified())
    }

    @Test
    fun neverReplacesTheCurrentSetUnlessEverySourceIsAvailable() {
        val f = fresh(); f.failing.add("https://lists.example/small.txt")
        val r = run(f)
        assertTrue(r.message, r is FilterUpdater.Result.Failed)
        assertFalse("no partial set may be installed", File(ctx.filesDir, "filters/current.kfs").exists())
        assertFalse(Engine.sourceDescription == "updated list")
    }

    @Test
    fun rejectsWebPagesPretendingToBeLists() {
        val f = fresh()
        f.bodies["https://lists.example/small.txt"] = "<!DOCTYPE html><html><body>" + "Rate limited. ".repeat(300) + "</body></html>"
        val r = run(f)
        assertTrue(r.message, r is FilterUpdater.Result.Failed && r.message.contains("web page"))
        assertFalse(File(ctx.filesDir, "filters/current.kfs").exists())
    }

    @Test
    fun aTruncatedOrPoisonedUpdateCannotWeakenProtection() {
        val f = fresh()
        assertTrue(run(f) is FilterUpdater.Result.Updated)
        val good = File(ctx.filesDir, "filters/current.kfs").readBytes()
        // the "big" list comes back with only 5 rules (plus padding to look list-sized): total drops far below 60 % of before
        f.bodies["https://lists.example/big.txt"] = listText("adhost", 5) + "! padding\n".repeat(400)
        val r = run(f)
        assertTrue(r.message, r is FilterUpdater.Result.Failed)
        assertTrue("the previous compiled set stays untouched", good.contentEquals(File(ctx.filesDir, "filters/current.kfs").readBytes()))
        assertTrue("and stays active", Verdict.isBlocked(Engine.filters().check("https://adhost7.example/x.js", "page.example", RequestType.SCRIPT)))
    }

    @Test
    fun respectsOfflineAndTheUnmeteredOnlySetting() {
        assertTrue(run(fresh(), online = false) is FilterUpdater.Result.Skipped)
        Prefs.get(ctx).updateUnmeteredOnly = true
        val f = fresh()
        val scheduled = run(f, manual = false, metered = true)
        assertTrue(scheduled.message, scheduled is FilterUpdater.Result.Skipped)
        assertEquals("a skipped scheduled run must not touch the network", 0, f.calls)
        assertTrue("the user tapping Update now overrides it", run(f, manual = true, metered = true) is FilterUpdater.Result.Updated)
    }

    @Test
    fun refusesNonHttpsSources() {
        val bad = listOf(ListSource("plain", "http://lists.example/big.txt", Category.ADS))
        val f = fresh(); f.bodies["http://lists.example/big.txt"] = listText("adhost", 25_000)
        val r = run(f, src = bad)
        assertTrue(r.message, r is FilterUpdater.Result.Failed)
        assertEquals("a cleartext source must never even be requested", 0, f.calls)
    }

    @Test
    fun defaultSourcesAreAllHttps() {
        assertTrue(FilterSources.DEFAULT.all { it.url.startsWith("https://") })
        assertEquals(FilterSources.DEFAULT.size, FilterSources.DEFAULT.map { it.id }.toSet().size)
    }
}
