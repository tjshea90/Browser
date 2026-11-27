package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LD4 (owner 2026-11-21: "Claude.Ai and YouTube load slow, even on fast wifi ... diagnostics ... tells claude a lot of information"): what a page's own timing adds, and how the advisor tells the three kinds of slow load apart. */
class LoadAnalysisTest {
    private fun timing(host: String, ttfb: Int, fcp: Int, load: Int, rn: Int, lagTot: Int, hid: Int = 0) =
        "+90.0s page timing host=$host nt=0 dns=40 con=300 ttfb=$ttfb fcp=$fcp lcp=${fcp + 500} dcl=${load / 2} load=$load rn=$rn rc=${rn * 9 / 10} kb=4100 r1=12 r3=3 rsum=95000 h2=150 h1=12 h3=0 js=40 css=3 img=80 xhr=60 lag=2400/$lagTot/60 nodes=3400 scr=45 fr=3 hid=$hid slow=cdn.example.com:js:9800 | shields=on net=fast-wifi avail=420 cpu=88 tabs=1"

    private fun stats(vararg lines: String): DiagStats { val d = DiagStats(); for (l in lines) d.observe("page", "timing", l); return d }

    @Test fun aTimingLineFeedsItsSiteWithTheBusyShareAndTheRequestCount() {
        val d = stats(timing("claude.ai", 800, 14000, 28000, 1000, 16800), timing("claude.ai", 900, 15000, 30000, 1200, 18000), timing("claude.ai", 700, 13000, 26000, 1100, 15600))
        val s = d.sites().single()
        assertEquals("claude.ai", s.site)
        assertEquals(3, s.timed); assertEquals(800L, s.avgTtfbMs); assertEquals(14000L, s.avgFcpMs)
        assertEquals("(16800+18000+15600) of (28000+30000+26000) = 60 %", 60, s.lagPct)
        assertEquals(1100, s.avgReqs)
    }

    @Test fun aLoadTheePersonLookedAwayFromSaysNothingAboutSpeed() {
        val d = stats(timing("claude.ai", 800, 14000, 28000, 1000, 16800, hid = 1))
        assertTrue(d.sites().isEmpty())
    }

    @Test fun aSensitiveSiteIsOneBucketAndNeverNamedInTheTable() {
        val d = stats(timing("login.examplebank.com", 800, 14000, 28000, 100, 2000))
        assertEquals("<sensitive-site>", d.sites().single().site)
    }

    @Test fun timingSurvivesSaveLoadAndMerge() {
        val a = stats(timing("claude.ai", 800, 14000, 28000, 1000, 16800))
        val b = DiagStats(); b.load(a.save())
        assertEquals(a.sites().single().lagPct, b.sites().single().lagPct)
        val c = stats(timing("claude.ai", 600, 10000, 20000, 800, 2000))
        val week = DiagStats(); week.mergeFrom(b); week.mergeFrom(c)
        val s = week.sites().single()
        assertEquals(2, s.timed); assertEquals(700L, s.avgTtfbMs); assertEquals(39, s.lagPct)             // (16800+2000)/(28000+20000)
        assertEquals(900, s.avgReqs)
    }

    @Test fun anOldTableWithoutTimingColumnsStillLoads() {
        val d = DiagStats(); d.load("S\tclaude.ai\t3\t0\t35000\t2700\t3\t28000,30000,26000\n")
        assertEquals(3, d.sites().single().loads); assertEquals(0, d.sites().single().timed); assertEquals(-1, d.sites().single().lagPct)
    }

    @Test fun theBatchCarriesTheTimingOfASiteAndLeavesItOutWhenThereIsNone() {
        val x = SiteStat("claude.ai", 3, 0, 28000, 35000, 14000, 3, 800, 14000, 60, 1100)
        val j = DiagBatch("i", "v", 1, 29, "m", "t", emptyList(), emptyList(), 0, sites = listOf(x, SiteStat("a.example.com", 1, 0, 1, 1, 1))).toJson()
        assertTrue(j, j.contains("\"timed\": 3, \"ttfbMs\": 800, \"fcpMs\": 14000, \"lagPct\": 60, \"reqs\": 1100"))
        assertEquals("only the site that has timing says so", 1, Regex("\"lagPct\"").findAll(j).count())
    }

    @Test fun aTimingLineIsNeverThinnedAwayByRoutineNoise() {
        val j = DiagJournal(800, 3)
        repeat(100) { j.add("video quality", "+$it.0s video quality x") }
        repeat(20) { j.add("page timing", timing("claude.ai", 1, 1, 1, 1, 1)) }
        assertEquals(20, j.toList().count { it.contains("page timing") })
    }

    @Test fun anEventLineKeepsFourHundredEightyCharactersAndASnapshotLineSixteenHundred() {
        val line = "+90.0s page timing host=claude.ai " + (1..70).joinToString(" ") { "k$it=$it" }
        val e = DiagScrub.line(line)!!
        assertTrue(e.length > 400 && e.length <= DiagScrub.MAX_LINE + 3)
        val snap = "CPU by process, % of ONE core: " + (1..120).joinToString("; ") { "$it min ago parent ${it % 100} % [Gecko $it, RenderThread 5]" }
        val k = DiagScrub.line(snap, DiagScrub.MAX_SNAPSHOT_LINE)!!
        assertTrue(k.length > 480 && k.length <= DiagScrub.MAX_SNAPSHOT_LINE + 3)
        assertTrue(DiagScrub.line(snap)!!.length <= DiagScrub.MAX_LINE + 3)
    }

    @Test fun aSensitiveThirdPartyInTheSlowListIsNotNamed() {
        val t = DiagScrub.line("+9.0s page timing host=claude.ai slow=cdn.example.com:js:9800,login.examplebank.com:xhr:5200 | shields=on")!!
        assertTrue(t, t.contains("cdn.example.com:js:9800"))
        assertFalse(t, t.contains("examplebank"))
    }

    // ---- the advisor

    private fun claude(lagPct: Int, reqs: Int = 1100, timed: Int = 3) = SiteStat("claude.ai", 3, 0, 28000, 35000, 14000, timed, 800, 14000, lagPct, reqs)
    private val ext = mapOf("extReqCalls" to 2425L, "extReqUs" to 3_948_000L)
    private val paired = mapOf("phonePaired" to 1L)

    @Test fun aBusyPageIsToldAsBusyAndTheFasterLinkIsNotSuggestedForIt() {
        val a = Advisor.advise(emptyMap(), emptyMap(), listOf(claude(60), SiteStat("m.youtube.com", 3, 0, 20000, 28000, 5000, 3, 700, 5000, 55, 300)), paired + ext)
        val busy = a.single { it.id == "busy-claude.ai" }
        assertNull(busy.key)
        assertTrue(busy.why, busy.why.contains("60 %") && busy.why.contains("1100 requests"))
        assertTrue("the phone's own connection does not help a busy page", a.none { it.id == "slow-phone" })
    }

    @Test fun aNetworkBoundSlowSitePairIsStillToldToUseThePhone() {
        val a = Advisor.advise(emptyMap(), emptyMap(), listOf(claude(10), SiteStat("m.youtube.com", 3, 0, 20000, 28000, 5000, 3, 700, 5000, 8, 300)), paired)
        assertNotNull(a.singleOrNull { it.id == "slow-phone" })
        assertTrue(a.none { it.id.startsWith("busy-") })
    }

    @Test fun theBlockerRuleStaysQuietForTheRealClaudeNumbersBecauseShieldsAreAboutSixPercentOfThatLoad() {
        val a = Advisor.advise(emptyMap(), emptyMap(), listOf(claude(60, reqs = 1000)), ext)
        assertTrue("1000 requests x 1.6 ms = 1.6 s of 28 s", a.none { it.id.startsWith("shields-heavy-") })
    }

    @Test fun theBlockerRuleSpeaksWhenItIsALargeShareOfTheLoad() {
        val a = Advisor.advise(emptyMap(), emptyMap(), listOf(SiteStat("heavy.example.org", 3, 0, 20000, 30000, 5000, 3, 700, 5000, 20, 2500)), ext)
        val h = a.single { it.id == "shields-heavy-heavy.example.org" }
        assertNull(h.key)
        assertTrue(h.why, h.why.contains("2500 requests") && h.why.contains("4.0 s"))
    }

    @Test fun noTimingMeansNoNewVerdict() {
        val a = Advisor.advise(emptyMap(), emptyMap(), listOf(SiteStat("claude.ai", 4, 0, 28000, 35000, 14000)), ext)
        assertTrue(a.none { it.id.startsWith("busy-") || it.id.startsWith("shields-heavy-") })
    }

    // ---- the numbers the app adds

    @Test fun theExtensionsListenerTextBecomesNumbersAndTheJudgesAverage() {
        val n = ExtCost.numbers("req 2425 (3948000 us), rcv 711 (303000 us), snd 710 (102000 us), done 1637 (197000 us), err 341 (62000 us)", 2423L, 3_011_000L, 53_000L, 279L)
        assertEquals(2425L, n["extReqCalls"]); assertEquals(3_948_000L, n["extReqUs"]); assertEquals(303_000L, n["extRcvUs"]); assertEquals(197_000L, n["extDoneUs"]); assertEquals(62_000L, n["extErrUs"])
        assertEquals(1242L, n["judgeAvgUs"]); assertEquals(53_000L, n["judgeMaxUs"]); assertEquals(279L, n["judgeOver2ms"]); assertEquals(2423L, n["judged"])
        assertTrue("nothing at all before the extension reports", ExtCost.numbers("", 0, 0, 0, 0).isEmpty())
        assertEquals(2, ExtCost.numbers("req 10 (500 us), junk, rcv x (y us)", 0, 0, 0, 0).size)
    }

    @Test fun thePageTimingLineHasEveryKeyInOrderAndTheThreeSlowestRequests() {
        val v = mapOf("nt" to 0L, "ttfb" to 700L, "fcp" to 14000L, "load" to 28000L, "rn" to 1044L, "hid" to 0L, "pt" to 0L, "at" to 31000L, "nodes" to 3400L)
        val line = PageTimingLine.build("claude.ai", v, listOf(2400L, 11000L, 60L), listOf(PageTimingLine.Slow("a.example.com", "js", 9800), PageTimingLine.Slow("b.example.com", "xhr", 5200)), "shields=up net=fast-wifi avail=420 cpu=88 tabs=1")
        assertTrue(line, line.startsWith("host=claude.ai nt=0 dns=-1 con=-1 tls=-1 fb=-1 ttfb=700 fcp=14000 lcp=-1 dcl=-1 load=28000 rn=1044 rc=-1"))
        assertTrue(line, line.contains(" lag=2400/11000/60 nodes=3400 scr=-1 fr=-1 rd=-1 hid=0 pt=0 at=31000 slow=a.example.com:js:9800,b.example.com:xhr:5200 | shields=up net=fast-wifi avail=420 cpu=88 tabs=1"))
        assertTrue("short enough for one journal line", line.length <= DiagScrub.MAX_LINE)
        val d = DiagStats(); d.observe("page", "timing", "+9.0s page timing $line")
        val s = d.sites().single()
        assertEquals("claude.ai", s.site); assertEquals(1, s.timed); assertEquals(14000L, s.avgFcpMs); assertEquals(1044, s.avgReqs); assertEquals("11000 ms late of a 28000 ms load", 39, s.lagPct)
    }
}
