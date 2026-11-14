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
        // "world" was confirmed for a shape that is really the shape of "hello": a glide of "hello" must not turn into "world" (the letters are far from the path and the first letter differs anyway)
        val dec = GlideDecoder(keys, GlideParams()); val model = PersonalModel(); dec.personal = model
        val g = GestureSim(keys, Random(2)).path("hello", SimNoise.CALM)!!
        model.patterns.add("world", Fingerprint.of(g.xs, g.ys, g.xs.size, keys)!!, 12f)
        val c = dec.decode(g.xs, g.ys, g.xs.size, listOf(lex), null, 3)
        assertEquals("hello", c.first().text)
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
}
