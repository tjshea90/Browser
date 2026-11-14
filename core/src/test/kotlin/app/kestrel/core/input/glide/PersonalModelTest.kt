package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/** What the keyboard learns from one person's choices and typing (AU2). */
class PersonalModelTest {
    private val keys = GlideEval.keysFor()
    private val direct = Executor { it.run() }

    @Test fun aWordGrowsMoreLikelyTheMoreItIsUsedButOnlyUpToACeiling() {
        val m = PersonalModel()
        val total = 5.4e11
        assertEquals(0.0, m.unigramMass("doraby", total), 0.0)
        m.add(null, "doraby")
        val once = m.unigramMass("doraby", total)
        repeat(9) { m.add(null, "doraby") }
        val ten = m.unigramMass("doraby", total)
        repeat(1000) { m.add(null, "doraby") }
        val many = m.unigramMass("doraby", total)
        assertTrue("ten uses beat one", ten > once * 5)
        assertTrue("a thousand uses count as at most ${PersonalModel.MAX_USES}", many <= PersonalModel.UNIGRAM_COUNT * PersonalModel.MAX_USES / total + 1e-12)
        assertTrue("one use is about as likely as an ordinary rare word (ln $once)", Math.log(once) in -11.0..-9.0)
    }

    @Test fun pairsGrowTrustSlowlyAndNeverMoreThanTheCeiling() {
        val m = PersonalModel()
        assertEquals(0f, m.pairWeight("best"), 0f)
        m.add("best", "pizza"); m.add("best", "pizza"); m.add("best", "burger")
        assertEquals(2f / 3f, m.pairProb("best", "pizza"), 0.01f)
        assertEquals(0f, m.pairProb("best", "salad"), 0f)
        val few = m.pairWeight("best")
        repeat(500) { m.add("best", "pizza") }
        assertTrue(m.pairWeight("best") > few)
        assertEquals(PersonalModel.MAX_PAIR_TRUST, m.pairWeight("best"), 1e-4f)
    }

    @Test fun aChosenWordCountsMoreThanAKeptOne() {
        val m = PersonalModel()
        m.add(null, "wield", 3f); m.add(null, "world", 1f)
        assertEquals(3f, m.weightOf("wield"), 0f); assertEquals(1f, m.weightOf("world"), 0f)
    }

    @Test fun onlyRealWordsAreKept() {
        val m = PersonalModel()
        for (bad in listOf("", "a", "doraby.com", "x9", "Don't's'", "pelis juanita", "x".repeat(40))) m.add("hello", bad)
        assertTrue(m.isEmpty); assertEquals(0, m.pairCount)
        m.add("Hello", "World"); assertEquals(1f, m.weightOf("world"), 0f); assertTrue(m.pairProb("hello", "world") > 0f)
        m.add(null, "don't"); assertTrue(m.weightOf("don't") > 0f)
    }

    @Test fun oldHabitsFadeAndTheTablesStaySmall() {
        val m = PersonalModel(capWords = 50, capPairs = 80)
        for (i in 0 until 400) m.add("w${i % 7}x", "wo${i}rd".filter { it in 'a'..'z' }.ifEmpty { "abc" } + ('a' + i % 26) + ('a' + (i / 26) % 26))
        assertTrue("words ${m.wordCount}", m.wordCount <= 50)
        assertTrue("pairs ${m.pairCount}", m.pairCount <= 80)
        val d = PersonalModel()
        repeat(7000) { d.add(null, "often") } // the total passes the decay point: everything halves
        assertTrue(d.weightOf("often") < 4000f)
    }

    @Test fun whatWasLearnedSurvivesSavingAndLoading() {
        val m = PersonalModel()
        m.add("best", "pizza", 3f); m.add("best", "pizza"); m.add(null, "doraby")
        val back = PersonalModel.parse(m.serialize())
        assertEquals(m.wordCount, back.wordCount); assertEquals(m.pairCount, back.pairCount)
        assertEquals(m.weightOf("pizza"), back.weightOf("pizza"), 0.01f)
        assertEquals(m.pairProb("best", "pizza"), back.pairProb("best", "pizza"), 0.01f)
        assertEquals(1, PersonalModel.parse("garbage\nw x\nw ok 0\nw fine 2.5\n").wordCount)
        m.clear(); assertTrue(m.isEmpty)
    }

    @Test fun aLearnedWordCanOutrankAMoreCommonOneThatLooksTheSame() {
        // "its" and "it's" have the same keys: only the language can choose, and what the person uses decides
        val scale = 5.4e11
        val lex = ArrayLexicon(listOf("its" to 400_000_000L, "it's" to 100_000_000L, "the" to 20_000_000_000L), scale)
        val dec = GlideDecoder(keys)
        val p = GestureSim(keys, Random(4)).path("its", SimNoise.CALM)!!
        val before = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 2)
        assertEquals("its", before[0].text)
        val pm = PersonalModel(); repeat(30) { pm.add(null, "it's", 3f) }
        dec.personal = pm
        val after = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 2)
        assertEquals("it's", after[0].text)
        dec.personal = null
        assertEquals("its", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 1)[0].text)
    }

    @Test fun aPairTheyWriteOftenMakesTheSecondWordLikelyAfterTheFirst() {
        val scale = 5.4e11
        val lex = ArrayLexicon(listOf("world" to 900_000_000L, "word" to 1_200_000_000L, "hello" to 500_000_000L), scale)
        val dec = GlideDecoder(keys, GlideParams(lmA = 0f, lmW = 4f)) // a heavy language weight so the effect is plain
        val p = GestureSim(keys, Random(3)).path("word", SimNoise.NORMAL)!!
        assertEquals("word", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), "hello", 1)[0].text)
        val pm = PersonalModel(); repeat(12) { pm.add("hello", "world", 1f) }
        dec.personal = pm
        assertEquals("world", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), "hello", 1)[0].text)
        assertEquals("another word before: unchanged", "word", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), "the", 1)[0].text)
    }

    @Test fun theEngineSavesLoadsAndForgets() {
        val bytes = LexiconFile.build(listOf("hello" to 500L, "world" to 900L, "word" to 1200L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        var saved = ""
        val g = GlideTyping({ bytes }, direct, loadPersonal = { null }, savePersonal = { saved = it })
        g.prepare(keys, null, null)
        g.learn("hello", "world", 3f); g.learn(null, "doraby", 1f)
        g.saveLearned()
        assertTrue(saved, saved.contains("w world 3.00") && saved.contains("p hello world 3.00") && saved.contains("w doraby 1.00"))
        // a new engine reads it back and says so
        val g2 = GlideTyping({ bytes }, direct, loadPersonal = { saved }, savePersonal = { })
        g2.prepare(keys, null, null)
        assertTrue(g2.stats.line(), g2.stats.line().contains("learned 2 words and 1 word pairs"))
        g2.forgetAll()
        assertFalse(g2.stats.line().contains("learned 2 words"))
        var after = "x"; val g3 = GlideTyping({ bytes }, direct, loadPersonal = { saved }, savePersonal = { after = it })
        g3.prepare(keys, null, null); g3.forgetAll(); assertEquals("forgetting also empties the stored file", "", after)
    }

    @Test fun aWordTheyHadUsedBeforeIsCountedWhenItIsTheFirstGuess() {
        // a list at the real scale (one huge word sets the total), three words whose paths differ clearly
        val bytes = LexiconFile.build(listOf("the" to 530_000_000_000L, "hello" to 500_000_000L, "world" to 900_000_000L, "pizza" to 300_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val g = GlideTyping({ bytes }, direct)
        g.prepare(keys, null, null)
        g.learn(null, "hello", 1f)
        val sim = GestureSim(keys, Random(2))
        fun glide(word: String): String {
            val p = sim.path(word, SimNoise.CALM)!!
            var r: GlideResult? = null
            g.decode(p.xs, p.ys, p.xs.size, null) { r = it }
            return r!!.candidates[0].text
        }
        assertEquals("world", glide("world"))
        assertTrue(g.stats.line(), g.stats.line().contains("a learned word was the first guess 0 times"))
        assertEquals("hello", glide("hello"))
        assertTrue(g.stats.line(), g.stats.line().contains("a learned word was the first guess 1 times"))
    }

    @Test fun learningMakesAPersonWithHabitsMoreAccurateOverTime() {
        val lex = GlideEval.loadLexicon()
        val off = GlideEval.learningCurve(lex, keys, GlideParams(), SimNoise.NORMAL, 900, 3, false, 21L)
        val on = GlideEval.learningCurve(lex, keys, GlideParams(), SimNoise.NORMAL, 900, 3, true, 21L)
        assertTrue("last third: with learning ${on.blocks.last()} %, without ${off.blocks.last()} %", on.blocks.last() >= off.blocks.last() + 3.0)
        assertTrue("own favourites in the last third: ${on.favouriteBlocks.last()} % against ${off.favouriteBlocks.last()} %", on.favouriteBlocks.last() >= off.favouriteBlocks.last() + 5.0)
        assertTrue("it knows what was used", on.known > 100 && on.pairs > 50)
    }

    // ------------------------------------------------------------------ AV2: corrections, the last two words, the short memory

    private fun ambiguous(): Triple<ArrayLexicon, GlideDecoder, GestureSim.Path> {
        // "world" is the commoner word; the path is drawn for "wield" (i and o, e and r are neighbours): find a finger that makes the keyboard guess "world"
        val lex = ArrayLexicon(listOf("world" to 900_000_000L, "wield" to 40_000_000L, "word" to 500_000_000L, "the" to 20_000_000_000L), 5.4e11)
        val dec = GlideDecoder(keys)
        for (seed in 1L..300L) {
            val p = GestureSim(keys, Random(seed)).path("wield", SimNoise.NORMAL) ?: continue
            val r = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)
            if (r.size >= 2 && r[0].text == "world" && r.any { it.text == "wield" }) return Triple(lex, dec, p)
        }
        error("no seed makes the keyboard guess world for a wield path")
    }

    @Test fun aCorrectionLiftsWhatThePersonMeantWhenThatGuessComesUpAgainAndOnlyThen() {
        val (lex, dec, p) = ambiguous()
        val pm = PersonalModel(); repeat(3) { pm.addConfusion("world", "wield") }
        dec.personal = pm
        assertEquals("the person meant wield every time the keyboard guessed world: now wield is first", "wield", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)[0].text)
        val other = PersonalModel(); repeat(3) { other.addConfusion("word", "wield") } // a correction of ANOTHER guess does not change this one
        dec.personal = other
        assertEquals("world", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)[0].text)
        dec.personal = PersonalModel().also { it.addConfusion("world", "wield", 0.0f) }
        assertEquals("a correction with no weight is nothing", "world", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)[0].text)
    }

    @Test fun theLastTwoWordsCanDecideBetweenTwoWordsThatLookAlike() {
        val (lex, dec, p) = ambiguous()
        val pm = PersonalModel(); repeat(8) { pm.add("whole", "wield", 1f, "the") }
        dec.personal = pm
        assertEquals("the whole wield", "wield", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), "whole", 3, "the")[0].text)
        assertEquals("after another pair of words nothing changes", "world", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), "big", 3, "a")[0].text)
        assertEquals("with only one word before, the three-word habit is not used", "world", dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), "whole", 3, null)[0].text)
    }

    @Test fun aWordUsedJustNowIsLikelierThanOneUsedLongAgoAndItFades() {
        val total = 5.4e11
        val m = PersonalModel(); m.add(null, "doraby")
        val fresh = m.recentMass("doraby", total)
        assertTrue("used just now", fresh > 0.0)
        repeat(300) { m.add(null, "other" + ('a' + it % 20) + ('a' + it / 20)) }
        val faded = m.recentMass("doraby", total)
        assertTrue("300 other words later: $fresh -> $faded", faded < fresh * 0.2)
        val off = PersonalModel(recency = false); off.add(null, "doraby")
        assertEquals("with the short memory off there is none", 0.0, off.recentMass("doraby", total), 0.0)
    }

    @Test fun correctionsTriplesAndRecencySurviveSavingAndAnOldFileStillLoads() {
        val m = PersonalModel()
        m.add("best", "pizza", 3f, "the"); m.add("best", "pizza", 1f, "the"); m.addConfusion("world", "wield", 2f)
        val back = PersonalModel.parse(m.serialize())
        assertEquals(m.confusionWeight("world", "wield"), back.confusionWeight("world", "wield"), 0.01f)
        assertEquals(m.tripleProb("the", "best", "pizza"), back.tripleProb("the", "best", "pizza"), 0.01f)
        assertTrue(back.tripleWeight("the", "best") > 0f)
        assertTrue(back.recentMass("pizza", 5.4e11) > 0.0)
        val old = PersonalModel.parse("w hello 3.00\nw world 1.00\np hello world 2.00\n") // a file from before AV2
        assertEquals(2, old.wordCount); assertEquals(1, old.pairCount); assertEquals(0, old.confusionCount)
        assertEquals(0, PersonalModel.parse("c a a 3.0\nc x\nt a b 2.0\n").confusionCount) // a correction of a word by itself and a broken line are nothing
    }

    @Test fun theEngineCountsCorrectionsAndSaysSo() {
        val bytes = LexiconFile.build(listOf("the" to 530_000_000_000L, "hello" to 500_000_000L, "world" to 900_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val g = GlideTyping({ bytes }, direct); g.prepare(keys, null, null)
        g.learn("big", "wield", 3f, "a", "world")
        assertTrue(g.stats.line(), g.stats.line().contains("1 corrections and 1 three-word phrases remembered"))
    }
}
