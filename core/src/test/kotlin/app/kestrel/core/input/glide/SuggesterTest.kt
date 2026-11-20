package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The words the strip offers while tapping: completions, typo fixes, next words, and how the person's own use changes them. */
class SuggesterTest {
    private val keys = GlideEval.keysFor(564f, 160f)

    /** A small list on a realistic scale: a huge filler word makes the total about as big as the shipped list's, so a learned word's boost is the real size. */
    private fun lex(): BinaryLexicon {
        val e = listOf(
            "the" to 90_000_000L, "that" to 40_000_000L, "this" to 35_000_000L, "they" to 30_000_000L, "them" to 14_000_000L, "then" to 16_000_000L, "there" to 18_000_000L,
            "hello" to 9_000_000L, "world" to 6_000_000L, "work" to 9_500_000L, "word" to 5_000_000L, "worth" to 2_000_000L, "from" to 20_000_000L, "form" to 6_000_000L,
            "something" to 4_000_000L, "of" to 70_000_000L, "off" to 8_000_000L, "you" to 60_000_000L, "your" to 25_000_000L, "thank" to 3_000_000L, "don't" to 12_000_000L, "dont" to 100_000L, "i'm" to 30_000_000L, "ill" to 400_000L, "i'll" to 9_000_000L,
        ).map { it.first to it.second * 200L } + listOf("zzfiller" to 400_000_000_000L)
        val pairs = listOf(LexiconFile.Pair("hello", "world", 400_000L), LexiconFile.Pair("hello", "there", 300_000L), LexiconFile.Pair("thank", "you", 900_000L), LexiconFile.Pair("thank", "your", 20_000L))
        return LexiconFile.read(LexiconFile.build(e.map { LexiconFile.Entry(it.first, it.second) }, pairs))
    }
    private val main = lex()
    private fun s(typed: String, prev: String? = null, prev2: String? = null, pm: PersonalModel? = null, n: Int = 3) = Suggester.suggest(main, pm, keys, typed, prev, prev2, n)

    @Test fun aVulgarWordIsNeverOfferedAsTheNextWordUnlessThePersonUsesIt() {
        val l = LexiconFile.read(LexiconFile.build(listOf("pink", "floyd", "pussy", "cat", "zzfiller").mapIndexed { i, w -> LexiconFile.Entry(w, if (w == "zzfiller") 400_000_000_000L else 50_000_000L - i * 1_000_000L) },
            listOf(LexiconFile.Pair("pink", "pussy", 900_000L), LexiconFile.Pair("pink", "floyd", 500_000L), LexiconFile.Pair("pink", "cat", 100_000L))))
        val plain = Suggester.nextWords(l, null, "pink", null, 3).words
        assertEquals(listOf("floyd", "cat"), plain)
        // letters the person typed still complete it, and a word they use themselves is theirs
        assertTrue(Suggester.suggest(l, null, keys, "pus", null, null, 3).words.contains("pussy"))
        val pm = PersonalModel(); repeat(3) { pm.add("pink", "pussy", 1f, null) }
        assertTrue(Suggester.nextWords(l, pm, "pink", null, 3).words.contains("pussy"))
    }

    @Test fun completionsAreTheMostCommonWordsThatStartWithTheLetters() {
        val r = s("th")
        assertEquals("the", r.words.first()); assertTrue(r.words.containsAll(listOf("that", "this")))
        assertFalse("th is not a word", r.typedKnown)
    }

    @Test fun theWordBeforeDecides() {
        assertEquals("work", s("wor").words.first())
        assertEquals("world", s("wor", prev = "hello").words.first())
        assertEquals("you", s("y", prev = "thank").words.first())
    }

    @Test fun aWordTheTypedLettersAreAlreadyIsKnown() {
        val r = s("hello"); assertTrue(r.typedKnown); assertEquals("hello", r.words.first()); assertNull(r.autoCorrect)
    }

    @Test fun aTypoIsFixedAndOnlyWhenNothingElseCouldBeMeant() {
        val r = s("teh"); assertFalse(r.typedKnown)
        assertEquals("the", r.words.first()); assertEquals("the", r.autoCorrect)
        assertEquals("hello", s("helo").words.first()); assertEquals("hello", s("helo").autoCorrect)
        assertEquals("world", s("wrld").words.first())
    }

    @Test fun aWordThatIsKnownIsNeverFixed() {
        val r = s("form"); assertTrue(r.typedKnown); assertNull("form is a word: from must not replace it", r.autoCorrect)
        assertNull(s("from").autoCorrect)
    }

    @Test fun aNeighbouringKeyIsACheapSlip() {
        val r = s("thw"); assertEquals("the", r.words.first())     // w is next to e
    }

    @Test fun aMissingApostropheIsRestoredWhenTheContractionIsFarMoreLikelyThanTheWordSpelledTheSame() {
        assertEquals("don't", s("dont").autoCorrect)                  // dont is in the list but is 40 times rarer than don't
        assertEquals("I'm", s("im").autoCorrect)                      // the key 'im' is only known as the contraction; I is always a capital
        assertEquals("I'll", s("ill").autoCorrect)                    // ill is a word, but I'll is about 22 times more likely
        assertNull("a word that is not a contraction is left alone", s("form").autoCorrect)
    }

    @Test fun capitalsAreKept() {
        assertEquals("The", s("Teh").words.first()); assertEquals("THE", s("TEH").words.first()); assertEquals("The", s("Th").words.first())
    }

    @Test fun theNextWordIsWhatUsuallyFollows() {
        assertEquals("world", Suggester.suggest(main, null, keys, "", "hello", null, 3).words.first())
        assertTrue(Suggester.suggest(main, null, keys, "", "hello", null, 3).words.contains("there"))
        val start = Suggester.suggest(main, null, keys, "", null, null, 3)
        assertTrue("a start of a sentence offers starters", start.words.isNotEmpty())
    }

    @Test fun whatThePersonUsesRisesAndOneUseDoesNot() {
        val pm = PersonalModel()
        pm.add(null, "worth")
        assertEquals("one use changes nothing much", "work", s("wor", pm = pm).words.first())
        repeat(150) { pm.add(null, "worth") }
        assertEquals("a word used all the time comes first", "worth", s("wor", pm = pm).words.first())
    }

    @Test fun aWordTheListLacksNeedsTwoUsesAndThenIsOffered() {
        val pm = PersonalModel()
        pm.add(null, "kestrel")
        assertFalse(s("kes", pm = pm).words.contains("kestrel"))
        pm.add(null, "kestrel")
        assertEquals("kestrel", s("kes", pm = pm).words.first())
        assertTrue("used twice, it is the person's word: never fixed", s("kestrel", pm = pm).typedKnown)
        assertNull(s("kestrel", pm = pm).autoCorrect)
    }

    @Test fun thePairsThePersonWritesLeadTheNextWords() {
        val pm = PersonalModel()
        repeat(6) { pm.add("good", "morning"); pm.add("morning", "kestrel") ; pm.add("morning", "kestrel") }
        val r = Suggester.suggest(main, pm, keys, "", "morning", "good", 3)
        assertEquals("kestrel", r.words.first())
    }

    @Test fun aCorrectionTheyMadeBeforeLiftsTheWordTheyMeant() {
        val pm = PersonalModel(); pm.add(null, "hello")
        assertFalse(s("tha", pm = pm).words.contains("the"))
        repeat(5) { pm.addConfusion("tha", "the", 1f) }
        assertTrue("tha was corrected to the five times: the is on offer", s("tha", pm = pm).words.contains("the"))
    }

    @Test fun nothingBreaksOnOddInput() {
        for (t in listOf("a", "'", "x'", "don'", "zzzzzzzzzzzz", "qqqq", "é", "1a", "a".repeat(40))) s(t)
        s("", "hello", "world"); s("", "unknownword", null)
        assertEquals(0, s("xyzxyzxyz").words.size)
    }
}
