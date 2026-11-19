package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/** R27a: the shadow decode (what the plain decoder says, for Diagnostics) and "Forget where my thumb lands". */
class ShadowAndThumbTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    private val bytes = LexiconFile.build(listOf("for" to 900L, "fir" to 300L, "fur" to 100L, "hello" to 500L, "world" to 800L, "there" to 700L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
    private fun engine() = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { }).also { it.prepare(keys, null, null) }
    private fun decode(e: GlideTyping, sim: GestureSim, noise: SimNoise, word: String): GlideResult { val p = sim.path(word, noise)!!; var r: GlideResult? = null; e.decode(p.xs, p.ys, p.xs.size, null) { r = it }; return r!! }

    @Test fun beforeAnythingIsLearnedThereIsNothingToCompare() {
        val e = engine(); val sim = GestureSim(keys, Random(5))
        val r = decode(e, sim, SimNoise.CALM, "hello")
        assertTrue(r.candidates.isNotEmpty()); assertTrue(r.serial > 0)
        assertNull("no habit, no plane, no own words: the plain decoder would say the same", e.shadowFor(r.serial))
    }

    @Test fun afterLearningTheShadowHoldsBothFirstWords() {
        val e = engine(); val sim = GestureSim(keys, Random(5)); val noise = SimNoise.NORMAL.withUserBias(10f)
        for (i in 0 until 12) { val r = decode(e, sim, noise, "hello"); e.accepted(r.candidates[0].text) { } }
        val r = decode(e, sim, noise, "world")
        val sh = e.shadowFor(r.serial)
        assertNotNull(sh); assertEquals(r.candidates[0].text, sh!!.learned); assertNotNull(sh.plain)
        assertTrue(e.stats.line(), e.stats.line().contains("touch plane up to"))
    }

    @Test fun forgettingTheThumbClearsHabitAndPlaneButKeepsTheWords() {
        val e = engine(); val sim = GestureSim(keys, Random(7)); val noise = SimNoise.NORMAL.withUserBias(12f)
        e.stats.onDecoded(1, true)
        for (i in 0 until 15) { val r = decode(e, sim, noise, "hello"); e.accepted(r.candidates[0].text) { } }
        assertTrue("a habit was learned: ${e.stats.line()}", !e.stats.line().contains("thumb habit x 0.0 y 0.0"))
        var saved: FloatArray? = null; var done = false
        e.forgetThumb({ saved = it }) { done = true }
        assertTrue(done); assertEquals(2, saved!!.size); assertEquals(0f, saved!![0], 0f); assertEquals(0f, saved!![1], 0f)
        assertTrue(e.stats.line(), e.stats.line().contains("thumb habit x 0.0 y 0.0") && e.stats.line().contains("touch plane up to 0.0 dp"))
        // the words are still learned: a glide of the learned word still decodes
        assertEquals("hello", decode(e, sim, SimNoise.CALM, "hello").candidates[0].text)
    }
}
