package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The strip must offer common words before rare ones, and the words the person uses before both (owner: the keyboard kept offering "tui"). */
class SuggesterRareTest {
    private val keys = GlideEval.keysFor(564f, 160f)

    private fun scaled(entries: List<Pair<String, Long>>, pairs: List<LexiconFile.Pair> = emptyList()): BinaryLexicon {
        val sum = entries.sumOf { it.second }
        val filler = if (sum < 541_808_760_578L) listOf("zzfiller" to 541_808_760_578L - sum) else emptyList()
        return LexiconFile.read(LexiconFile.build((entries + filler).map { LexiconFile.Entry(it.first, it.second) }, pairs))
    }

    private val main = scaled(listOf(
        "turn" to 3_000_000_000L, "tuesday" to 1_000_000_000L, "tube" to 400_000_000L, "tune" to 300_000_000L, "tulip" to 20_000_000L,
        "tui" to 390_000L, "tuk" to 200_000L, "tung" to 150_000L, "tuba" to 900_000L,
        "bat" to 500_000_000L, "bot" to 300_000L, "the" to 28_000_000_000L, "some" to 5_000_000_000L, "sums" to 100_000L,
        "quixotic" to 200_000L, "quite" to 2_000_000_000L,
    ))

    private fun s(typed: String, pm: PersonalModel? = null, n: Int = 3, prev: String? = null) = Suggester.suggest(main, pm, keys, typed, prev, null, n)

    @Test fun commonCompletionsComeBeforeRareOnes() {
        val r = s("tu", n = 12).words
        assertEquals("turn", r.first())
        assertTrue("$r", r.indexOf("tuesday") < r.indexOf("tui") && r.indexOf("tube") < r.indexOf("tui") && r.indexOf("tune") < r.indexOf("tui"))
        assertFalse("the strip of three shows common words only: ${s("tu").words}", s("tu").words.any { it in setOf("tui", "tuk", "tung", "tuba") })
    }

    @Test fun aRareWordIsStillOfferedWhenItIsAllThereIs() {
        assertEquals("tui", s("tui").words.first())
        assertEquals(listOf("quixotic"), s("quixo").words)
    }

    @Test fun aRareWordTheTypedLettersAreIsStillKnownAndNeverFixed() {
        val r = s("tui"); assertTrue(r.typedKnown); assertNull(r.autoCorrect)
    }

    @Test fun theWordsThePersonUsesComeAboveTheRareOnes() {
        val pm = PersonalModel()
        repeat(40) { pm.add(null, "tui", 1f) }
        val withUse = s("tu", pm).words
        assertTrue("a word used 40 times is offered: $withUse", "tui" in withUse)
        assertTrue("and above the rare ones nobody uses: ${s("tu", pm, 12).words}", s("tu", pm, 12).words.let { it.indexOf("tui") < it.indexOf("tuk") && it.indexOf("tui") < it.indexOf("tuba") })
        assertFalse("without the use it is not on the strip", "tui" in s("tu").words)
    }

    @Test fun aFewUsesLiftARareWordPastOtherRareWords() {
        val pm = PersonalModel()
        repeat(3) { pm.add(null, "tuba", 1f) }
        val r = s("tu", pm, 12).words
        assertTrue("$r", r.indexOf("tuba") < r.indexOf("tuk") && r.indexOf("tuba") < r.indexOf("tung"))
    }

    @Test fun aTypoIsFixedToTheCommonWordNotToARareLookalike() {
        // "bt": a letter left out; "bat" and "bot" are equally far, "bat" is what people mean
        val r = s("bt")
        assertEquals("bat", r.words.first())
        assertFalse("bot" in s("bt").words.take(1))
        // "sme" is one letter short of "some" (common) and one slip from "sums"? only the common word is offered first
        assertEquals("some", s("sme").words.first())
    }

    @Test fun theNextWordIsTheCommonOneAfterTheWordBefore() {
        val l = scaled(
            listOf("my" to 4_000_000_000L, "turn" to 3_000_000_000L, "tui" to 390_000L, "the" to 28_000_000_000L, "tune" to 300_000_000L),
            listOf(LexiconFile.Pair("my", "turn", 900_000L), LexiconFile.Pair("my", "tune", 300_000L), LexiconFile.Pair("my", "tui", 3_000L)),
        )
        val r = Suggester.suggest(l, null, keys, "", "my", null, 3)
        assertEquals(listOf("turn", "tune", "tui"), r.words)
        val learned = PersonalModel(); repeat(10) { learned.add("my", "tui", 1f) }
        assertEquals("what the person writes after 'my' comes first", "tui", Suggester.suggest(l, learned, keys, "", "my", null, 3).words.first())
    }
}
