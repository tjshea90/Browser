package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * AX1 (owner message 38): "I often type the word for yet Gboard often inputs the word fir ... I always change fir to for but it still glide types fir. I want this keyboard to learn intuitively".
 * The simulated hand lands the o of "for" a key toward i, the same way every time (a hairpin turn that stops short); the keyboard learns from what the person does after each glide.
 */
class GlideShapeLearningTest {
    private val lex by lazy { GlideEval.loadLexicon() }
    private val keys = GlideEval.keysFor()

    private fun accuracy(outcomes: List<GlideHabitEval.Outcome>, from: Int, to: Int) = outcomes.subList(from, to).count { it.firstRight } * 100.0 / (to - from)

    @Test fun theOldKeyboardKeepsGettingAHabitualWordWrongAndTheNewOneLearnsItInAFewCorrections() {
        var offSum = 0.0; var onSum = 0.0; var onEarly = 0.0; val hands = 14
        for (seed in 1..hands) {
            val habit = GlideHabitEval.forHabit(keys, 1.2f)
            val off = GlideHabitEval.repeatWord(lex, keys, GlideHabitEval.Hand(keys, SimNoise.NORMAL, seed.toLong(), 0f, habit), "for", 30, patterns = false)
            val on = GlideHabitEval.repeatWord(lex, keys, GlideHabitEval.Hand(keys, SimNoise.NORMAL, seed.toLong(), 0f, habit), "for", 30, patterns = true)
            offSum += accuracy(off, 6, 30); onSum += accuracy(on, 6, 30); onEarly += accuracy(on, 0, 3)
        }
        val off = offSum / hands; val on = onSum / hands
        assertTrue("without shapes the glides still miss (first guess right $off % of glides 7-30)", off < 90.0)
        assertTrue("with shapes, after a few corrections the word is right ($on % of glides 7-30)", on >= 96.0)
        assertTrue("the first three glides (nothing learned yet) are as good as the old keyboard's: ${onEarly / 1.0} %", onEarly >= 60.0)
    }

    @Test fun oneCorrectionIsEnoughForTheSameShapeAgain() {
        val habit = GlideHabitEval.forHabit(keys, 1.2f)
        var better = 0; var tried = 0
        for (seed in 1..20) {
            val hand = GlideHabitEval.Hand(keys, SimNoise.CALM, seed.toLong(), 0f, habit)
            val dec = GlideDecoder(keys, GlideParams()); val model = PersonalModel(); dec.personal = model
            val p1 = hand.path("for")!!
            val c1 = dec.decode(p1.xs, p1.ys, p1.xs.size, listOf(lex), null, 3)
            if (c1.isEmpty() || c1[0].text == "for" || c1.none { it.text == "for" }) continue // the old keyboard got it right, or the word was not even in the strip: nothing to correct
            tried++
            // the person picks "for" from the strip: its shape is remembered and what was guessed loses its look-alikes
            model.add(null, "for", 3f); model.addConfusion(c1[0].text, "for"); model.patterns.add("for", dec.lastPrint!!, 3f); model.patterns.contradict("for", dec.lastPrint!!, 2f)
            val p2 = hand.path("for")!!
            val c2 = dec.decode(p2.xs, p2.ys, p2.xs.size, listOf(lex), null, 3)
            if (c2.isNotEmpty() && c2[0].text == "for") better++
        }
        assertTrue("there were wrong first guesses to correct ($tried)", tried >= 3)
        assertTrue("the next glide of the same word came out right after one correction in $better of $tried cases", better * 100 >= tried * 80)
    }

    @Test fun aShapeNeverMakesAWordAppearThatTheFingerWentNowhereNear() {
        // "world" was confirmed twelve times for the shape of a glide that is really the shape of "hello": a glide like that must not turn into "world" (its letters are far from the path and its first letter differs)
        val g = GestureSim(keys, Random(2)).path("hello", SimNoise.CALM)!!
        val plain = GlideDecoder(keys, GlideParams(patGain = 0f)).decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3)
        val dec = GlideDecoder(keys, GlideParams()); val model = PersonalModel(); dec.personal = model
        model.patterns.add("world", Fingerprint.of(g.xs, g.ys, g.xs.size, keys)!!, 12f)
        val c = dec.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3)
        assertEquals(plain.map { it.text }, c.map { it.text })
        assertFalse(c.any { it.text == "world" })
    }

    @Test fun anEmptyMemoryChangesNothingAndCostsNothingMeasurable() {
        val g = GestureSim(keys, Random(5)).path("together", SimNoise.NORMAL)!!
        val plain = GlideDecoder(keys, GlideParams(patGain = 0f)); val with = GlideDecoder(keys, GlideParams()); with.personal = PersonalModel()
        val a = plain.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3); val b = with.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3)
        assertEquals(a.map { it.text }, b.map { it.text })
        assertFalse(with.lastPatternLifted)
    }

    @Test fun learningTheShapesNeverCostsAccuracyOverALongStretch() {
        // a person whose hand has a habit for each word (about a third of a key): with shapes, more first guesses are mended than spoiled, and the accuracy is not lower
        val c = GlideHabitEval.habitCurve(lex, keys, SimNoise.NORMAL, 900, 3, words = true, shapes = true, habitDp = 12f, seed = 5L)
        val without = GlideHabitEval.habitCurve(lex, keys, SimNoise.NORMAL, 900, 3, words = true, shapes = false, habitDp = 12f, seed = 5L)
        assertTrue("mended ${c.good}, spoiled ${c.bad}", c.good >= c.bad)
        val last = c.blocks.last(); val lastWithout = without.blocks.last()
        assertTrue("last third: with shapes $last %, without $lastWithout %", last >= lastWithout - 1.0)
    }

    @Test fun aWordTheQuickPassWouldDropIsStillConsideredWhenTheMemoryHasItsShape() {
        // the quick pass keeps only the best word of two ("fur" is far more common and lies one key from the path); the person's memory has the shape of "for"
        val small = ArrayLexicon(listOf("fur" to 9000L, "for" to 10L))
        val g = GestureSim(keys, Random(1)).path("for", SimNoise.CALM)!!
        val plain = GlideDecoder(keys, GlideParams(keep = 1, patGain = 0f))
        assertEquals("fur", plain.decode(g.xs, g.ys, g.xs.size, listOf(small), null, 3).first().text)
        val dec = GlideDecoder(keys, GlideParams(keep = 1)); val model = PersonalModel(); dec.personal = model
        assertEquals("nothing remembered: the same as before", "fur", dec.decode(g.xs, g.ys, g.xs.size, listOf(small), null, 3).first().text)
        model.patterns.add("for", dec.lastPrint!!, 3f)
        val c = dec.decode(g.xs, g.ys, g.xs.size, listOf(small), null, 3)
        assertEquals("for", c.first().text)
        assertTrue(dec.lastPatternFlipped)
        // a remembered word that does not fit the ends of this glide is not brought in
        val other = PersonalModel(); other.patterns.add("fur", dec.lastPrint!!, 3f); other.patterns.add("our", dec.lastPrint!!, 3f)
        val d2 = GlideDecoder(keys, GlideParams(keep = 1)); d2.personal = other
        assertFalse(d2.decode(g.xs, g.ys, g.xs.size, listOf(ArrayLexicon(listOf("fur" to 9000L, "our" to 10L))), null, 3).any { it.text == "our" })
    }

    @Test fun rememberedWordsGetOnlyAFewExtraPlacesAndTheDecoderStaysFast() {
        // 3000 words with a shape each (a deliberately full memory): one decode is not slower than twice a decode without the memory (plus a small allowance for the very first, cold run)
        val model = PersonalModel(); val rnd = Random(4)
        val g = GestureSim(keys, Random(5)).path("together", SimNoise.NORMAL)!!
        val pr = Fingerprint.of(g.xs, g.ys, g.xs.size, keys)!!
        for (i in 0 until 1200) { val w = lex.text(rnd.nextInt(lex.size)); if (PersonalModel.norm(w) != null) model.patterns.add(w, FloatArray(pr.size) { pr[it] + rnd.nextFloat() * 0.3f }, 2f) }
        val withMemory = GlideDecoder(keys, GlideParams()); withMemory.personal = model
        val without = GlideDecoder(keys, GlideParams(patGain = 0f))
        repeat(3) { withMemory.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3); without.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3) } // warm up
        var a = 0L; var b = 0L
        repeat(15) { val t0 = System.nanoTime(); withMemory.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3); val t1 = System.nanoTime(); without.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3); val t2 = System.nanoTime(); a += t1 - t0; b += t2 - t1 }
        assertTrue("with the memory ${a / 15 / 1000} us, without ${b / 15 / 1000} us", a <= 2 * b + 3_000_000L * 15)
        assertTrue("never more than the survivors plus the extra places: ${withMemory.lastCandidates}", withMemory.lastCandidates <= GlideParams().keep + GlideDecoder.MAX_INJECT)
    }
}
