package app.kestrel.core.optimizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizerPartsTest {
    private fun sample(scale: Double = 1.0, compat: Double = 25.0, mem: Double = 600.0, ok: Boolean = true): Sample {
        val m = HashMap<String, Double>()
        for (x in Metrics.balanced) m[x.id] = (if (x.higherIsBetter) 100.0 * scale else 100.0 / scale)
        m[Metrics.COMPAT] = compat; m[Metrics.MEM] = mem
        return Sample(m, ok)
    }
    private fun Sample.withGl(v: Double) = Sample(values + (Metrics.GL to v), ok)
    private val noisy = listOf(sample(1.00), sample(1.01), sample(0.99), sample(1.02), sample(0.98), sample(1.0))
    private val metrics = Metrics.balanced

    @Test fun weightsSumToOneForEveryPriority() {
        for (p in Priority.values()) assertEquals(p.name, 1.0, Metrics.forPriority(p).sumOf { it.weight }, 1e-9)
        assertTrue("battery shifts weight to processor time", Metrics.forPriority(Priority.BATTERY).first { it.id == Metrics.CPU }.weight > Metrics.balanced.first { it.id == Metrics.CPU }.weight)
    }

    @Test fun aMetricGainIsSignedByItsDirection() {
        val fps = Metrics.balanced.first { it.id == Metrics.FPS_CSS }; val ms = Metrics.balanced.first { it.id == Metrics.SCRIPT }
        assertTrue(fps.gain(30.0, 36.0) > 0); assertTrue(fps.gain(30.0, 24.0) < 0)
        assertTrue(ms.gain(2000.0, 1800.0) > 0); assertTrue(ms.gain(2000.0, 2300.0) < 0)
        assertEquals("a value at zero stays defined", 0.0, Metrics.balanced.first { it.id == Metrics.LATE }.gain(0.0, 0.0), 1e-12)
    }

    @Test fun aClearGainIsAcceptedACleatLossRejectedASmallWobbleIsLeftAlone() {
        assertEquals(Verdict.ACCEPT, Decider.decide(noisy, listOf(sample(1.10), sample(1.11)), metrics, Guards()).verdict)
        assertEquals(Verdict.REJECT, Decider.decide(noisy, listOf(sample(0.90), sample(0.91)), metrics, Guards()).verdict)
        assertEquals(Verdict.INCONCLUSIVE, Decider.decide(noisy, listOf(sample(1.012), sample(1.008)), metrics, Guards()).verdict)
    }

    @Test fun theGuardsHoldWhateverTheScore() {
        assertEquals("a feature test that stopped passing", Verdict.REJECT, Decider.decide(noisy, listOf(sample(1.2, compat = 24.0), sample(1.2, compat = 24.0)), metrics, Guards()).verdict)
        assertEquals("WebGL that stopped working", Verdict.REJECT, Decider.decide(listOf(sample(1.00).withGl(1.0), sample(1.01).withGl(1.0), sample(0.99).withGl(1.0)), listOf(sample(1.2).withGl(0.0), sample(1.2).withGl(0.0)), metrics, Guards()).verdict)
        assertEquals("memory over the limit", Verdict.REJECT, Decider.decide(noisy, listOf(sample(1.2, mem = 700.0), sample(1.2, mem = 700.0)), metrics, Guards(maxMemMb = 60.0)).verdict)
        assertEquals("memory under a higher limit", Verdict.ACCEPT, Decider.decide(noisy, listOf(sample(1.2, mem = 700.0), sample(1.2, mem = 700.0)), metrics, Guards(maxMemMb = 140.0)).verdict)
        assertEquals("a failed page", Verdict.FAILED, Decider.decide(noisy, listOf(sample(1.2), sample(1.2, ok = false)), metrics, Guards()).verdict)
        assertEquals("the owner saw a fault", Verdict.FAILED, Decider.decide(noisy, listOf(sample(1.2), sample(1.2)), metrics, Guards(), ownerOk = false).verdict)
        val oneBad = Sample(sample(1.15).values + (Metrics.INPUT to 100.0 * 1.6), true)
        assertEquals("one thing much worse even if the total is better", Verdict.REJECT, Decider.decide(noisy, listOf(oneBad, oneBad), metrics, Guards()).verdict)
    }

    @Test fun tooFewBaselinesCannotJudge() {
        assertEquals(Verdict.INCONCLUSIVE, Decider.decide(listOf(sample()), listOf(sample(1.3)), metrics, Guards()).verdict)
    }

    @Test fun aNoisyTabletRaisesTheBarAndAQuietOneLowersIt() {
        val loud = listOf(sample(1.0), sample(1.25), sample(0.8), sample(1.15), sample(0.85), sample(1.0))
        assertTrue(Decider.noise(loud, metrics) > Decider.noise(noisy, metrics) * 3)
        assertEquals("a 6 % gain is lost in that noise", Verdict.INCONCLUSIVE, Decider.decide(loud, listOf(sample(1.06), sample(1.06)), metrics, Guards()).verdict)
    }

    @Test fun aBorderlineResultAsksForOneMoreSample() {
        val thr = Decider.threshold(noisy, 2, metrics)
        val borderline = listOf(sample(1.0 + thr * 1.2), sample(1.0 + thr * 1.2))
        assertTrue(Decider.borderline(noisy, borderline, metrics))
        assertFalse(Decider.borderline(noisy, listOf(sample(1.3), sample(1.3)), metrics))
        assertFalse(Decider.borderline(noisy, listOf(sample(1.0), sample(1.0)), metrics))
    }

    @Test fun configsCompareAsText() {
        val a = Config(mapOf("x.y" to "1", "a.b" to "true"), mapOf("scroll_feel" to 0))
        assertEquals(a, Config.parse(a.canonical()))
        assertEquals("p:a.b=true;p:x.y=1;s:scroll_feel=0", a.canonical())
        assertEquals("p:a.b=true;p:x.y=1", a.enginePart())
        assertEquals(Config(), Config.parse(""))
    }

    @Test fun aFlipTakesTheOppositeOfTheEnginesOwnValueAndNeedsOne() {
        val base = Config()
        val flip = listOf<Change>(Change.Flip("gfx.webrender.paths"))
        assertEquals("true", base.with(flip, mapOf("gfx.webrender.paths" to "false"))!!.engine["gfx.webrender.paths"])
        assertEquals("false", base.with(flip, mapOf("gfx.webrender.paths" to "true"))!!.engine["gfx.webrender.paths"])
        assertNull("no readable default: the experiment cannot be set up", base.with(flip, emptyMap()))
    }

    @Test fun theCatalogIsSaneAndNeverTouchesPrivacyOrSecurity() {
        val ids = Catalog.all.map { it.id }
        assertEquals("ids are unique", ids.size, ids.toSet().size)
        for (e in Catalog.all) { assertTrue(e.id + " has changes", e.changes.isNotEmpty()); for (c in e.changes) assertTrue("${e.id}: ${c.key} is allowed", Catalog.isAllowed(c)) }
        for (bad in listOf("privacy.trackingprotection.enabled", "network.trr.mode", "network.dns.disableIPv6", "security.tls.version.min", "network.cookie.cookieBehavior", "privacy.resistFingerprinting",
            "dom.security.https_only_mode", "geo.enabled", "datareporting.healthreport.uploadEnabled", "browser.safebrowsing.malware.enabled"))
            assertFalse("$bad must be refused", Catalog.isAllowed(Change.Pref(bad, "false")))
        assertTrue(Catalog.isAllowed(Change.Pref("layout.css.stylo-threads", "2")))
        assertEquals("every app-setting key is typed", Catalog.settingKeys().keys, Catalog.all.flatMap { it.settingChanges() }.map { it.prefKey }.toSet())
        assertTrue("the hardware trial is an app setting the DrawTrial rule understands", Catalog.byId("hw1")!!.changes.any { it is Change.Setting && it.prefKey == "draw_trial" && it.value == 1 })
        assertTrue("only experiments that change the drawing ask the owner about glitches", Catalog.all.filter { it.askGlitch }.all { it.group == Group.DRAW })
    }

    @Test fun thePlanOpensAndClosesWithBaselinesAndSpreadsThemThrough() {
        val steps = PlanBuilder.build(Catalog.all)
        assertEquals(StepKind.BASELINE, steps.first().kind); assertEquals(2, steps.first().samples)
        assertEquals(StepKind.BASELINE, steps.last().kind); assertEquals(StepKind.COMBINED, steps[steps.size - 2].kind)
        assertEquals(Catalog.all.size, steps.count { it.kind == StepKind.EXPERIMENT })
        assertTrue("baselines also sit between the experiments (drift)", steps.count { it.kind == StepKind.BASELINE } >= 4)
        assertTrue("it fits an evening", steps.sumOf { it.estMin } in 40.0..120.0)
        assertEquals("hardware first: the biggest expected gain", "hw1", steps.first { it.kind == StepKind.EXPERIMENT }.experimentId)
    }
}
