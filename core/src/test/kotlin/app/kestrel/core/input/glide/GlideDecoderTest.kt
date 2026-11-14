package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** The glide decoder on a tiny word list and exact paths, then on the real word list with a simulated finger (AT1c, AT1d). */
class GlideDecoderTest {
    private val keys = GlideEval.keysFor()

    private fun glide(word: String, noise: SimNoise = SimNoise.CALM, seed: Long = 1): GestureSim.Path = GestureSim(keys, Random(seed)).path(word, noise)!!

    private fun decode(dec: GlideDecoder, lex: GlideLexicon, p: GestureSim.Path, prev: String? = null) = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), prev, 3)

    private val small = ArrayLexicon(listOf("hello" to 500L, "help" to 800L, "held" to 300L, "hell" to 300L, "world" to 900L, "word" to 1200L, "the" to 9000L, "he" to 3000L, "she" to 2000L, "to" to 7000L, "go" to 2500L, "good" to 2000L))

    @Test fun aCleanGlideFindsItsWord() {
        val dec = GlideDecoder(keys)
        for (w in listOf("hello", "world", "word", "the", "good", "she")) {
            val r = decode(dec, small, glide(w))
            assertEquals("glide of $w", w, r.firstOrNull()?.text)
        }
    }

    @Test fun theBestThreeComeBestFirstAndAreDistinct() {
        val dec = GlideDecoder(keys)
        val r = dec.decode(glide("hello").let { it.xs }, glide("hello").ys, glide("hello").xs.size, listOf(small), null, 3)
        assertTrue(r.size in 1..3)
        assertEquals(r.map { it.text }.distinct(), r.map { it.text })
        for (i in 1 until r.size) assertTrue(r[i - 1].score >= r[i].score)
    }

    @Test fun aTapOrAShortWiggleIsNotAGlide() {
        val dec = GlideDecoder(keys)
        val cx = keys.cx['g' - 'a']; val cy = keys.cy['g' - 'a']
        assertTrue(dec.decode(floatArrayOf(cx), floatArrayOf(cy), 1, listOf(small), null).isEmpty())
        assertTrue(dec.decode(floatArrayOf(cx, cx + 2f, cx + 1f, cx - 1f), floatArrayOf(cy, cy + 1f, cy - 2f, cy + 1f), 4, listOf(small), null).isEmpty())
    }

    @Test fun theFirstAndLastLetterMustBeWhereTheFingerWentDownAndLifted() {
        val dec = GlideDecoder(keys)
        // a path that starts on 'p' and ends on 'm' can be none of the words that start with h / g / s / w / t
        val r = dec.decode(floatArrayOf(keys.cx['p' - 'a'], keys.cx['m' - 'a']), floatArrayOf(keys.cy['p' - 'a'], keys.cy['m' - 'a']), 2, listOf(small), null)
        assertTrue(r.isEmpty())
    }

    @Test fun theWordBeforeSwingsAClosecall() {
        // "world" and "word" differ by one letter that a sloppy finger skips: with the pair "hello world" known, the context decides
        val withPair = LexiconFile.read(LexiconFile.build(
            listOf("hello", "world", "word").zip(listOf(500L, 900L, 1200L)).map { LexiconFile.Entry(it.first, it.second) },
            listOf(LexiconFile.Pair("hello", "world", 400L), LexiconFile.Pair("hello", "word", 1L)),
        ))
        val dec = GlideDecoder(keys, GlideParams(lmA = 0f, lmW = 6f)) // a heavy language weight so the effect is plain
        val p = glide("word", SimNoise.NORMAL, 3)
        val plain = decode(dec, withPair, p, null).first().text
        val ctx = decode(dec, withPair, p, "hello").first().text
        assertEquals("word", plain)
        assertEquals("world", ctx)
    }

    @Test fun theThumbHabitIsLearnedFromAcceptedWords() {
        val noise = SimNoise.NORMAL.withUserBias(12f)
        val sim = GestureSim(keys, Random(5))
        val dec = GlideDecoder(keys)
        val words = listOf("hello", "world", "word", "the", "good", "she", "help", "held", "hell", "go", "to", "he")
        repeat(120) { i ->
            val w = words[i % words.size]
            val p = sim.path(w, noise) ?: return@repeat
            val r = dec.decode(p.xs, p.ys, p.xs.size, listOf(small), null)
            if (r.any { it.text == w }) dec.learnFrom(small, small.idOf(w))
        }
        val learned = Math.hypot(dec.biasX.toDouble(), dec.biasY.toDouble()) * keys.pitch
        assertTrue("the learned habit ($learned dp) is near the real one (12 dp)", learned in 8.0..15.0)
        assertTrue(Math.abs(dec.biasX) <= GlideDecoder.MAX_BIAS && Math.abs(dec.biasY) <= GlideDecoder.MAX_BIAS)
    }

    @Test fun theLanguageWeightFollowsHowCleanThePathIs() {
        val dec = GlideDecoder(keys)
        decode(dec, small, glide("hello", SimNoise.CALM, 2)); val calm = dec.lastLm
        decode(dec, small, glide("hello", SimNoise.BAD, 2)); val bad = dec.lastLm
        assertTrue("a clean glide trusts the path more ($calm) than a bad one ($bad)", calm <= bad)
    }

    // ------------------------------------------------------------------ the real word list, a simulated finger

    private val lex by lazy { GlideEval.loadLexicon() }

    @Test fun theShippedWordListIsAsBigAsItShouldBe() {
        assertTrue("words ${lex.size}", lex.size > 80000)
        assertTrue("pairs ${lex.bigramCount}", lex.bigramCount > 200000)
        for (w in listOf("the", "youtube", "gmail", "it's", "don't", "weather", "restaurants")) assertTrue("$w is in the list", lex.idOf(w) >= 0)
    }

    private fun accuracy(noise: SimNoise, n: Int = 700): GlideEval.Result =
        GlideEval.run(lex, keys, GlideParams(), noise, GlideEval.frequencyPick(lex, n, Random(11)), 11L)

    @Test fun commonWordsAreFoundByACarefulFinger() {
        val r = accuracy(SimNoise.CALM)
        assertTrue("calm finger: $r", r.pct(r.top1) >= 90.0 && r.pct(r.top3) >= 97.0)
    }

    @Test fun commonWordsAreFoundByATypicalFinger() {
        val r = accuracy(SimNoise.NORMAL)
        assertTrue("normal finger: $r", r.pct(r.top1) >= 83.0 && r.pct(r.top3) >= 94.0)
    }

    @Test fun aSloppyFingerStillGetsTheWordInTheStrip() {
        val r = accuracy(SimNoise.SLOPPY)
        assertTrue("sloppy finger: $r", r.pct(r.top1) >= 70.0 && r.pct(r.top3) >= 86.0)
    }

    @Test fun theWordBeforeMakesItBetter() {
        val picks = GlideEval.contextPick(lex, 700, Random(13))
        val withCtx = GlideEval.run(lex, keys, GlideParams(), SimNoise.SLOPPY, picks, 13L)
        val without = GlideEval.run(lex, keys, GlideParams(), SimNoise.SLOPPY, picks.map { GlideEval.Pick(it.id, -1) }, 13L)
        assertTrue("with the word before ${withCtx.pct(withCtx.top1)} % first, without ${without.pct(without.top1)} %", withCtx.top1 > without.top1)
    }

    @Test fun aThumbThatLandsBesideTheKeyIsCorrectedWithoutLearning() {
        val noHabit = accuracy(SimNoise.NORMAL, 600)
        val habit = accuracy(SimNoise.NORMAL.withUserBias(10f), 600)
        assertTrue("habit 10 dp: ${habit.pct(habit.top1)} % vs ${noHabit.pct(noHabit.top1)} %", habit.pct(habit.top1) >= 75.0)
    }

    @Test fun decodingIsFast() {
        val r = accuracy(SimNoise.NORMAL, 300)
        // a CI machine is slower than a laptop and the tablet slower still: this is only a guard against an accidental 10x
        assertTrue("decode avg ${r.millis} ms", r.millis < 40.0)
    }
}
