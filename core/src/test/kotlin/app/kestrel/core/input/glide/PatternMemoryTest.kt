package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** The shape of a glide and the memory of the shapes this person draws (AX1, owner message 38). */
class PatternMemoryTest {
    private val keys = GlideEval.keysFor()

    private fun glide(word: String, noise: SimNoise = SimNoise.CALM, seed: Long = 1): GestureSim.Path = GestureSim(keys, Random(seed)).path(word, noise)!!
    private fun print(word: String, noise: SimNoise = SimNoise.CALM, seed: Long = 1): FloatArray = glide(word, noise, seed).let { Fingerprint.of(it.xs, it.ys, it.xs.size, keys)!! }
    private fun shifted(p: FloatArray, du: Float, dv: Float = 0f) = FloatArray(p.size) { if (it % 2 == 0) p[it] + du else p[it] + dv }

    // ------------------------------------------------------------------ the shape

    @Test fun aTapAndAShortWiggleHaveNoShape() {
        val cx = keys.cx['g' - 'a']; val cy = keys.cy['g' - 'a']
        assertNull(Fingerprint.of(floatArrayOf(cx), floatArrayOf(cy), 1, keys))
        assertNull(Fingerprint.of(floatArrayOf(cx, cx + 2f, cx + 1f), floatArrayOf(cy, cy + 1f, cy - 1f), 3, keys))
        assertNull(Fingerprint.of(floatArrayOf(cx, cx), floatArrayOf(cy, cy), 2, keys)) // the finger did not move
    }

    @Test fun theShapeHasSixteenPointsInKeyUnitsStartingWhereTheFingerWentDown() {
        val ideal = Fingerprint.ofWord("of", keys)!!
        assertEquals(2 * Fingerprint.K, ideal.size)
        // 'o' is the 9th key of the top row: 8 key widths right of the centre of Q, on Q's row; 'f' is on the home row, three keys right of 'a' (which sits half a key right of Q)
        assertEquals(8.0f, ideal[0], 0.05f); assertEquals(0.0f, ideal[1], 0.05f)
        assertEquals(3.5f, ideal[2 * Fingerprint.K - 2], 0.1f); assertEquals(1.0f, ideal[2 * Fingerprint.K - 1], 0.05f)
    }

    @Test fun theSameShapeComesOutOfAKeyboardOfAnotherSize() {
        val bigger = GlideEval.keysFor(450f, 120f) // wider and flatter, as the keyboard can be dragged to
        val a = Fingerprint.ofWord("for", keys)!!; val b = Fingerprint.ofWord("for", bigger)!!
        assertTrue("distance ${Fingerprint.distance(a, b)}", Fingerprint.distance(a, b) < 0.08f)
        // and a glide, not just the ideal line
        val g = GestureSim(keys, Random(4)).path("for", SimNoise.CALM)!!
        val sx = bigger.cx['q' - 'a'] / keys.cx['q' - 'a']
        assertNotNull(Fingerprint.of(g.xs, g.ys, g.xs.size, keys))
        assertTrue(sx > 0f)
    }

    @Test fun twoGlidesOfTheSameWordAreCloserThanGlidesOfTwoWords() {
        val sameA = print("for", SimNoise.NORMAL, 1); val sameB = print("for", SimNoise.NORMAL, 2)
        val other = print("fur", SimNoise.NORMAL, 3)
        assertTrue("same word ${Fingerprint.distance(sameA, sameB)} vs other ${Fingerprint.distance(sameA, other)}", Fingerprint.distance(sameA, sameB) < Fingerprint.distance(sameA, other) + 0.2f)
        assertEquals(0f, Fingerprint.distance(sameA, sameA), 1e-6f)
        assertEquals(Fingerprint.distance(sameA, other), Fingerprint.distance(other, sameA), 1e-6f)
    }

    @Test fun theSpacingOfThePointsFollowsTheLengthOfThePathNotTheSpeedOfTheFinger() {
        // the same line drawn with 3 touch points and with 300: the same shape
        val few = Fingerprint.of(floatArrayOf(keys.cx['f' - 'a'], keys.cx['i' - 'a'], keys.cx['r' - 'a']), floatArrayOf(keys.cy['f' - 'a'], keys.cy['i' - 'a'], keys.cy['r' - 'a']), 3, keys)!!
        val n = 300
        val xs = FloatArray(n); val ys = FloatArray(n)
        for (i in 0 until n) { val t = i / (n - 1f); if (t < 0.5f) { val u = t * 2; xs[i] = keys.cx['f' - 'a'] + (keys.cx['i' - 'a'] - keys.cx['f' - 'a']) * u; ys[i] = keys.cy['f' - 'a'] + (keys.cy['i' - 'a'] - keys.cy['f' - 'a']) * u } else { val u = (t - 0.5f) * 2; xs[i] = keys.cx['i' - 'a'] + (keys.cx['r' - 'a'] - keys.cx['i' - 'a']) * u; ys[i] = keys.cy['i' - 'a'] + (keys.cy['r' - 'a'] - keys.cy['i' - 'a']) * u } }
        val many = Fingerprint.of(xs, ys, n, keys)!!
        assertTrue("distance ${Fingerprint.distance(few, many)}", Fingerprint.distance(few, many) < 0.05f)
    }

    // ------------------------------------------------------------------ the memory

    @Test fun nearIdenticalShapesBecomeOneAndFarOnesStaySeparate() {
        val m = PatternMemory()
        val p = print("for")
        m.add("for", p, 1f); m.add("for", shifted(p, 0.05f), 3f)
        assertEquals(1, m.patternCount)
        m.add("for", shifted(p, 1.2f), 1f)
        assertEquals(2, m.patternCount); assertEquals(1, m.wordCount)
        // the merged shape counts both confirmations
        assertTrue("evidence ${m.evidence("for", p)}", m.evidence("for", p) > 3.5f)
    }

    @Test fun aWordKeepsAtMostFourShapesAndTheWeakestMakesRoom() {
        val m = PatternMemory()
        val p = print("for")
        for (i in 0 until 4) m.add("for", shifted(p, 1.5f * (i + 1)), if (i == 2) 1f else 5f)
        assertEquals(4, m.patternCount)
        m.add("for", shifted(p, 20f), 2f)
        assertEquals(4, m.patternCount)
        assertTrue(m.evidence("for", shifted(p, 20f)) > 1.5f) // the new one is in
        assertTrue(m.evidence("for", shifted(p, 4.5f)) < 0.1f) // the shape with one confirmation went
    }

    @Test fun evidenceIsLargeForALookAlikeAndNothingForADifferentGlideOrAnotherWord() {
        val m = PatternMemory()
        val p = print("for", SimNoise.NORMAL, 1)
        m.add("for", p, 3f)
        assertEquals(0f, m.evidence("fur", p), 0f)
        assertEquals(0f, m.evidence("xyz", p), 0f)
        assertTrue(m.evidence("for", shifted(p, 0.15f)) > 2f)
        assertTrue(m.evidence("for", shifted(p, 1.5f)) < 0.01f)
        // more confirmations, more evidence, but a ceiling
        val strong = PatternMemory(); strong.add("for", p, 50f)
        assertTrue(strong.evidence("for", p) <= PatternMemory.MAX_N + 0.001f)
    }

    @Test fun aCorrectionMakesTheLookAlikeShapesOfOtherWordsLessSure() {
        val m = PatternMemory()
        val p = print("fir")
        m.add("fir", p, 3f); m.add("fur", shifted(p, 3f), 3f)
        m.contradict("for", p, 2f)
        assertTrue("fir lost ${m.evidence("fir", p)}", m.evidence("fir", p) < 1.2f)
        assertEquals(3f, m.evidence("fur", shifted(p, 3f)), 0.01f) // far away: untouched
        m.contradict("for", p, 2f); m.contradict("for", p, 2f)
        assertEquals(0f, m.evidence("fir", p), 0f) // forgotten
        assertEquals(1, m.wordCount)
        // the meant word's own shapes are never weakened by its own correction
        m.add("for", p, 3f); m.contradict("for", p, 2f)
        assertTrue(m.evidence("for", p) > 2.9f)
    }

    @Test fun theMemoryStaysWithinItsCapsAndTheLeastConfirmedGoFirst() {
        val m = PatternMemory(capWords = 50, capPatterns = 80)
        val rnd = Random(9); val base = print("for")
        for (i in 0 until 400) { val w = "w" + ('a' + i % 26) + ('a' + (i / 26) % 26) + ('a' + (i / 676) % 26); m.add(w, shifted(base, rnd.nextFloat() * 6f, rnd.nextFloat() * 3f), 1f + (i % 7)) }
        assertTrue("words ${m.wordCount}", m.wordCount <= 50)
        assertTrue("shapes ${m.patternCount}", m.patternCount <= 80)
    }

    @Test fun oldHabitsFade() {
        val m = PatternMemory()
        val p = print("for")
        m.add("for", p, 4f); m.add("fir", shifted(p, 4f), 0.5f)
        m.decay()
        assertEquals(2f, m.evidence("for", p), 0.01f)
        assertEquals(0f, m.evidence("fir", shifted(p, 4f)), 0f) // fell under a third of a confirmation
        assertEquals(1, m.wordCount)
    }

    @Test fun aShapeIsWrittenAndReadBackWithinATenthOfAKey() {
        val m = PatternMemory(); val p = print("for")
        m.add("for", p, 2.5f)
        val text = m.serialize()
        val line = text.trim()
        assertTrue(line, line.startsWith("s for 2.50 "))
        assertEquals(3 + 2 * Fingerprint.K, line.split(' ').size)
        val back = PatternMemory()
        assertTrue(back.parseLine(line.split(' ')))
        assertEquals(1, back.patternCount)
        assertTrue("evidence ${back.evidence("for", p)}", back.evidence("for", p) > 2.2f)
    }

    @Test fun aBadShapeLineIsRefused() {
        val m = PatternMemory()
        val good = "s for 2.00 " + (0 until 2 * Fingerprint.K).joinToString(" ") { "10" }
        assertTrue(m.parseLine(good.split(' ')))
        for (bad in listOf(
            "s for 2.00 " + (0 until 2 * Fingerprint.K - 1).joinToString(" ") { "10" },          // too few numbers
            "s for 2.00 " + (0 until 2 * Fingerprint.K + 1).joinToString(" ") { "10" },          // too many
            "s for NaN " + (0 until 2 * Fingerprint.K).joinToString(" ") { "10" },               // a count that is not a number
            "s for -1.00 " + (0 until 2 * Fingerprint.K).joinToString(" ") { "10" },             // a count that is not positive
            "s for 2.00 " + (0 until 2 * Fingerprint.K).joinToString(" ") { "1.5" },             // a point that is not a whole number of tenths
            "s for 2.00 " + (0 until 2 * Fingerprint.K).joinToString(" ") { "99999" },           // a point far off the keyboard
            "s f0r 2.00 " + (0 until 2 * Fingerprint.K).joinToString(" ") { "10" },              // not a word
            "x for 2.00 " + (0 until 2 * Fingerprint.K).joinToString(" ") { "10" },              // not a shape line at all
        )) assertFalse(bad, PatternMemory().parseLine(bad.split(' ')))
    }

    @Test fun bringingAFileInTwiceChangesNothing() {
        val a = PatternMemory(); val b = PatternMemory()
        val p = print("for")
        a.add("for", p, 3f); a.add("for", shifted(p, 2f), 1f); b.add("our", p, 2f)
        b.mergeFrom(a); val once = b.serialize()
        b.mergeFrom(a)
        assertEquals(once, b.serialize())
        assertEquals(3, b.patternCount)
    }

    @Test fun theModelKeepsShapesWithItsOtherLearningAndForgetsThemTogether() {
        val pm = PersonalModel()
        val p = print("for")
        pm.add(null, "for", 3f); pm.patterns.add("for", p, 3f)
        val text = pm.serialize()
        assertTrue(text, text.contains("\nw for 3.00") || text.startsWith("w for 3.00"))
        assertTrue(text.lines().any { it.startsWith("s for 3.00 ") })
        val back = PersonalModel.parse(text)
        assertEquals(1, back.wordCount); assertEquals(1, back.patterns.patternCount)
        // an older keyboard reads the same text and skips the shapes; this one reads a text without shapes
        assertEquals(1, PersonalModel.parse(text.lines().filter { !it.startsWith("s ") }.joinToString("\n")).wordCount)
        assertEquals(0, PersonalModel.parse("w for 3.00\n").patterns.patternCount)
        pm.clear()
        assertEquals(0, pm.patterns.patternCount)
        assertTrue(pm.serialize().isEmpty())
    }
}
