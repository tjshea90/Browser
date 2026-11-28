package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** Owner R20d: where on the keyboard the thumb lands differently is learned (TouchField, an affine offset plane), measured with the simulated hand (GestureSim zones). research/R20-glide-personalization.md 4.1 */
class TouchFieldTest {
    private val keys = GlideEval.keysFor()
    private val lex by lazy { GlideEval.loadLexicon() }

    /** A person glides [glides] common words; the keyboard learns from every word that was among its guesses (what GlideEval does); the first guess is judged over the LAST third, after it has learned. */
    private fun lastThird(noise: SimNoise, params: GlideParams, glides: Int = 1200, seed: Long = 21): Double {
        val picks = GlideEval.frequencyPick(lex, glides, Random(seed))
        val sim = GestureSim(keys, Random(seed)); val dec = GlideDecoder(keys, params)
        var hit = 0; var n = 0
        for ((k, pk) in picks.withIndex()) {
            val word = lex.text(pk.id); val path = sim.path(word, noise) ?: continue
            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), if (pk.prev >= 0) lex.text(pk.prev) else null, 3)
            val key = String(GlideLetters.keyOf(word))
            if (k >= glides * 2 / 3) { n++; if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == key) hit++ }
            if (c.any { String(GlideLetters.keyOf(it.text)) == key }) dec.learnFrom(lex, pk.id)
        }
        return 100.0 * hit / n
    }
    private fun avg(noise: SimNoise, p: GlideParams) = (21L..26L).map { lastThird(noise, p, 1200, it) }.average()

    // ------------------------------------------------------------------ the plane itself

    @Test fun anEmptyFieldChangesNothingAndTrustGrowsWithEvidence() {
        val f = TouchField(); val o = FloatArray(2); assertTrue(f.isEmpty); f.at(0.3f, 0.7f, o); assertEquals(0f, o[0], 0f); assertEquals(0f, o[1], 0f)
        repeat(4) { f.add(0.5f, 0.5f, 0.3f, 0f) }; f.at(0.5f, 0.5f, o)
        assertTrue("four observations are not trusted yet: ${o[0]}", o[0] < 0.1f)
        repeat(400) { f.add(0.5f, 0.5f, 0.3f, 0f) }; f.at(0.5f, 0.5f, o)
        assertTrue("with plenty of evidence the offset is taken nearly whole: ${o[0]}", o[0] in 0.2f..0.31f)
    }

    @Test fun aPlaneIsLearnedFromNoisyObservations() {
        val f = TouchField(); val rnd = Random(3)
        repeat(600) { val u = rnd.nextFloat(); val v = rnd.nextFloat(); f.add(u, v, 0.30f - 0.60f * u + rnd.nextGaussian().toFloat() * 0.25f, 0.10f + 0.20f * v + rnd.nextGaussian().toFloat() * 0.25f) }
        val o = FloatArray(2)
        f.at(0f, 0.5f, o); assertEquals("left edge, x", 0.30f, o[0], 0.13f)
        f.at(1f, 0.5f, o); assertEquals("right edge, x", -0.30f, o[0], 0.13f)
        f.at(0.5f, 0f, o); assertEquals("top, y", 0.0f, o[1], 0.13f); f.at(0.5f, 1f, o); assertEquals("bottom, y", 0.20f, o[1], 0.13f)
    }

    @Test fun aWildObservationAndNoiseWithNoPatternDoNothing() {
        val f = TouchField(); f.add(0.5f, 0.5f, 5f, 0f); f.add(0.5f, 0.5f, Float.NaN, 0f); assertTrue("not used", f.isEmpty)
        val rnd = Random(9); repeat(800) { f.add(rnd.nextFloat(), rnd.nextFloat(), rnd.nextGaussian().toFloat() * 0.3f, rnd.nextGaussian().toFloat() * 0.3f) }
        val o = FloatArray(2); for (u in listOf(0f, 0.5f, 1f)) for (v in listOf(0f, 0.5f, 1f)) { f.at(u, v, o); assertTrue("no pattern, no correction at $u,$v: ${o.toList()}", Math.abs(o[0]) < 0.08f && Math.abs(o[1]) < 0.08f) }
    }

    @Test fun theStateSurvivesTheNumbersForThePrivateFileAndRejectsBadOnes() {
        val f = TouchField(); val rnd = Random(5); repeat(100) { val u = rnd.nextFloat(); f.add(u, 0.5f, 0.2f - 0.4f * u, 0f) }
        val g = TouchField(); assertTrue(g.fromArray(f.toArray())); val a = FloatArray(2); val b = FloatArray(2)
        for (u in listOf(0f, 0.4f, 1f)) { f.at(u, 0.3f, a); g.at(u, 0.3f, b); assertEquals(a[0], b[0], 1e-4f); assertEquals(a[1], b[1], 1e-4f) }
        val bad = f.toArray(); bad[3] = Float.NaN; assertFalse(TouchField().fromArray(bad)); assertFalse(TouchField().fromArray(FloatArray(5)))
        val neg = f.toArray(); neg[6] = -3f; assertFalse("a negative evidence count is no state", TouchField().fromArray(neg))
        g.clear(); assertTrue(g.isEmpty)
    }

    // ------------------------------------------------------------------ what it is worth (simulated hand; 6 hands, 1200 glides each, the last 400 judged)

    @Test fun aThumbThatLandsDifferentlyOnTheTwoSidesIsLearnedAndHelps() {
        val noise = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f)
        val off = avg(noise, GlideParams(touchField = false)); val on = avg(noise, GlideParams())
        assertTrue("single shift only $off %, with the plane $on %", on >= off + 2.0)
    }

    @Test fun aSloppyThumbWithSidesThatDifferAlsoGains() {
        val noise = SimNoise.SLOPPY.withZone(10f, 5f, -10f, 5f)
        val off = avg(noise, GlideParams(touchField = false)); val on = avg(noise, GlideParams())
        assertTrue("single shift only $off %, with the plane $on %", on >= off + 0.5)
    }

    @Test fun aThumbThatLandsTheSameEverywhereLosesNothing() {
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY, SimNoise.SLOPPY.withUserBias(10f))) {
            val off = avg(noise, GlideParams(touchField = false)); val on = avg(noise, GlideParams())
            assertTrue("${noise.name}: single shift only $off %, with the plane $on %", on >= off - 0.8)
        }
    }

    @Test fun theMapDoesNotCostTheDecoderAnyMeasurableTime() {
        val picks = GlideEval.frequencyPick(lex, 300, Random(4)); val sim = GestureSim(keys, Random(4))
        fun time(p: GlideParams): Long { val d = GlideDecoder(keys, p); repeat(80) { val w = lex.text(picks[it].id); sim.path(w, SimNoise.NORMAL)?.let { q -> d.decode(q.xs, q.ys, q.xs.size, listOf(lex), null, 3); d.learnFrom(lex, picks[it].id) } }
            val t0 = System.nanoTime(); for (k in 80 until 300) { val w = lex.text(picks[k].id); sim.path(w, SimNoise.NORMAL)?.let { q -> d.decode(q.xs, q.ys, q.xs.size, listOf(lex), null, 3) } }; return (System.nanoTime() - t0) / 1_000_000 }
        time(GlideParams()); val without = time(GlideParams(touchField = false)); val with = time(GlideParams())
        assertTrue("220 glides: $with ms with the map, $without ms without", with <= without * 1.5 + 200)
    }

    // ------------------------------------------------------------------ it is kept, exported and forgotten like the rest of what is learned

    @Test fun theExportCarriesTheMapAndAnOlderFileWithoutOneStillReads() {
        val f = TouchField(); repeat(60) { f.add(0.1f, 0.2f, 0.25f, -0.1f) }
        val habit = floatArrayOf(0.1f, -0.05f) + f.toArray()
        val text = LearnedExport.write(PersonalModel().also { it.add(null, "hello", 2f) }, habit, null)
        assertTrue(text.lines().any { it.startsWith("touch ") })
        val back = LearnedExport.read(text)!!
        assertEquals(0.1f, back.habit!![0], 0.001f); assertTrue("the map came back", back.touch != null && back.touch!!.size == TouchField.SIZE)
        val g = TouchField(); assertTrue(g.fromArray(back.touch!!)); val o1 = FloatArray(2); val o2 = FloatArray(2); f.at(0.1f, 0.2f, o1); g.at(0.1f, 0.2f, o2); assertEquals(o1[0], o2[0], 0.01f)
        val old = LearnedExport.write(PersonalModel().also { it.add(null, "hello", 2f) }, floatArrayOf(0.1f, -0.05f), null)
        assertTrue("a file without the map has no touch line", old.lines().none { it.startsWith("touch ") }); assertEquals(null, LearnedExport.read(old)!!.touch)
        val broken = text.lines().joinToString("\n") { if (it.startsWith("touch ")) "touch 1 2 3" else it }
        assertEquals("a map of the wrong size is ignored, the rest is read", null, LearnedExport.read(broken)!!.touch)
    }

    @Test fun theEngineRestoresTheMapAfterARestartAndForgetsItWhenAsked() {
        val saved = ArrayList<FloatArray>()
        fun engine(habit: FloatArray?): GlideTyping {
            val e = GlideTyping({ LexiconFile.build(listOf("for" to 900L, "fir" to 300L, "hello" to 500L, "world" to 800L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()) }, java.util.concurrent.Executor { it.run() })
            e.prepare(keys, habit, null); return e
        }
        val e = engine(null); val sim = GestureSim(keys, Random(2)); val noise = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f)
        repeat(120) { for (w in listOf("hello", "world", "for")) sim.path(w, noise)?.let { p -> e.decode(p.xs, p.ys, p.xs.size, null, null) { }; e.accepted(w) { h -> saved.add(h) } } }
        val last = saved.last(); assertEquals("two habit numbers and the map", 2 + TouchField.SIZE, last.size); assertTrue("it learned something: ${last.toList()}", last.drop(2).take(6).any { Math.abs(it) > 0.05f })
        val again = engine(last); var restored: FloatArray? = null
        again.exportText(null, null) { t -> restored = LearnedExport.read(t)?.touch }; assertTrue("the restarted engine has the map", restored != null && restored!!.take(6).any { Math.abs(it) > 0.05f })
        again.forgetAll(); var gone = false; again.exportText(null, null) { t -> gone = LearnedExport.read(t)?.touch == null }; assertTrue("forgetting clears it", gone)
    }
}
