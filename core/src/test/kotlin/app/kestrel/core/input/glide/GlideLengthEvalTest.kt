package app.kestrel.core.input.glide

import java.util.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner R23c (2026-11-18): "'the' often becomes 'there'. 'The' uses a shorter gesture than there, so it shouldn't confuse them."
 * The decoder weighs the length of the gesture against the length of each word's ideal path ([GlideParams.lengthW]). This measures it on pairs where one word is the start (or most of the letters) of the other,
 * gliding each word of a pair many times with the simulated finger, and on the ordinary yardstick, so the term can neither fail to help nor cost accuracy elsewhere.
 * Run for numbers: ./gradlew --offline :core:test --tests '*GlideLengthEvalTest' -i
 */
class GlideLengthEvalTest {
    private val lex by lazy { GlideEval.loadLexicon() }
    private val keys = GlideEval.keysFor()
    private val pairs = listOf("the" to "there", "he" to "her", "we" to "were", "to" to "top", "an" to "and", "in" to "into", "on" to "one", "is" to "its", "for" to "form", "of" to "off", "or" to "ore", "at" to "ate", "us" to "use", "so" to "some", "me" to "men", "no" to "now", "be" to "bed", "do" to "does", "how" to "show", "all" to "also")

    /** Share of glides of [word] whose first guess is [word], with the given parameters and finger. */
    private fun first(word: String, params: GlideParams, noise: SimNoise, n: Int): Double {
        val id = lex.idOf(word); if (id < 0) return Double.NaN
        val r = GlideEval.run(lex, keys, params, noise, List(n) { GlideEval.Pick(id, -1) }, 5L + word.hashCode())
        return r.pct(r.top1)
    }

    private fun table(params: GlideParams, noise: SimNoise, n: Int): Pair<Double, Double> {
        var s = 0.0; var l = 0.0; var k = 0
        for ((a, b) in pairs) { val x = first(a, params, noise, n); val y = first(b, params, noise, n); if (!x.isNaN() && !y.isNaN()) { s += x; l += y; k++ } }
        return s / k to l / k
    }

    @Test fun measureTheLengthTermOnLookAlikePairs() {
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) {
            val off = GlideParams(lengthW = 0f); val on = GlideParams()
            val (so, lo) = table(off, noise, 50); val (sn, ln) = table(on, noise, 50)
            println("LENGTH ${noise.name}: short word first  off %.1f%% -> on %.1f%%   long word first  off %.1f%% -> on %.1f%%".format(so, sn, lo, ln))
            val the = first("the", off, noise, 150) to first("the", on, noise, 150); val there = first("there", off, noise, 150) to first("there", on, noise, 150)
            println("LENGTH ${noise.name}: the %.1f -> %.1f   there %.1f -> %.1f".format(the.first, the.second, there.first, there.second))
            assertTrue("the short word of a pair is found at least as often: off %.1f on %.1f".format(so, sn), sn >= so - 0.5)
            assertTrue("and the long word is not given up for it: off %.1f on %.1f".format(lo, ln), ln >= lo - 1.5)
        }
    }

    @Test fun measureTheYardstickWithAndWithoutTheLengthTerm() {
        for (noise in listOf(SimNoise.CALM, SimNoise.NORMAL, SimNoise.SLOPPY)) {
            val picks = GlideEval.frequencyPick(lex, 700, Random(21))
            val off = GlideEval.run(lex, keys, GlideParams(lengthW = 0f), noise, picks, 21L); val on = GlideEval.run(lex, keys, GlideParams(), noise, picks, 21L)
            println("LENGTH yardstick ${noise.name}: off $off | on $on")
            assertTrue("never worse on top choice: off ${off.pct(off.top1)} on ${on.pct(on.top1)}", on.pct(on.top1) >= off.pct(off.top1) - 0.3)
            assertTrue("never worse on top three: off ${off.pct(off.top3)} on ${on.pct(on.top3)}", on.pct(on.top3) >= off.pct(off.top3) - 0.3)
        }
    }
}
