package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LD2a: the bounded tables the journal's lines add up to. */
class DiagStatsTest {
    private fun loaded(host: String, paint: Int, done: Int, ok: Boolean = true) = "+9.0s page loaded host=$host paint=${paint}ms done=${done}ms blocked=3 visit=1 ok=$ok"

    @Test fun finishedLoadsAddUpPerSiteWithMedianWorstAndFailures() {
        val s = DiagStats()
        for (d in listOf(2000, 4000, 30000, 3000, 5000)) s.observe("page", "loaded", loaded("news.example.org", 900, d))
        s.observe("page", "loaded", loaded("news.example.org", -1, -1, ok = false))
        val x = s.sites().single()
        assertEquals("news.example.org", x.site); assertEquals(6, x.loads); assertEquals(1, x.failed)
        assertEquals(4000, x.medianDoneMs); assertEquals(30000, x.worstDoneMs); assertEquals(900, x.avgPaintMs)
    }

    @Test fun aSensitiveSiteIsOneBucketAndNeverNamed() {
        val s = DiagStats()
        s.observe("page", "loaded", loaded("login.examplebank.com", 100, 2000))
        s.observe("page", "loaded", loaded("secure.example.org", 100, 2000))
        assertEquals(listOf("<sensitive-site>"), s.sites().map { it.site })
        assertEquals(2, s.sites().single().loads)
    }

    @Test fun theHistogramCountsEveryKindAndKeepsTheLongestTime() {
        val s = DiagStats()
        s.observe("gecko", "layout", "+1.0s gecko layout 1x1 took 120 ms (the engine view's own layout)")
        s.observe("gecko", "layout", "+2.0s gecko layout 1x1 took 450 ms")
        s.observe("gecko", "onKill", "+3.0s gecko onKill example.org")
        assertEquals(2L, s.count("gecko layout")); assertEquals(1L, s.count("gecko onKill"))
        assertEquals(450L, s.kindMaxMs()["gecko layout"])
    }

    @Test fun theTablesAreBounded() {
        val s = DiagStats(maxKinds = 5, maxSites = 3)
        for (i in 0 until 20) s.observe("life", "e$i", "+1.0s life e$i")
        assertEquals(5, s.kindCounts().count { it.key != "other kinds" }); assertEquals(15L, s.kindCounts()["other kinds"])
        for (i in 0 until 10) s.observe("page", "loaded", loaded("h$i.example.org", 1, 1))
        assertEquals(3, s.sites().size)
    }

    @Test fun anEventNameThatIsNotCodeMadeIsIgnored() {
        val s = DiagStats()
        s.observe("page", "Some Title With Spaces", "+1.0s page Some Title")
        s.observe("Page", "x", "+1.0s")
        assertTrue(s.isEmpty())
    }

    @Test fun saveAndLoadRoundTripAndADamagedLineIsSkipped() {
        val s = DiagStats()
        s.observe("gecko", "onKill", "+3.0s gecko onKill"); s.observe("page", "loaded", loaded("a.example.org", 500, 7000))
        val t = DiagStats(); t.load(s.save() + "K\tbroken\tx\ty\nS\tonly\n")
        assertEquals(1L, t.count("gecko onKill")); assertEquals(7000, t.sites().single().medianDoneMs)
        t.reset(); assertTrue(t.isEmpty()); assertFalse(t.save().contains("K\t"))
    }
}
