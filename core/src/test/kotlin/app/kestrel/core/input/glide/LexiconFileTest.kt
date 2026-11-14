package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/** The glide word list: what the compile tool writes is what the tablet reads (AT1b). */
class LexiconFileTest {
    private fun sample(): BinaryLexicon = LexiconFile.read(
        LexiconFile.build(
            listOf(
                LexiconFile.Entry("the", 1000), LexiconFile.Entry("hello", 50), LexiconFile.Entry("help", 40), LexiconFile.Entry("world", 60),
                LexiconFile.Entry("don't", 80), LexiconFile.Entry("dont", 2), LexiconFile.Entry("it's", 90), LexiconFile.Entry("its", 30),
                LexiconFile.Entry("Cat", 5), LexiconFile.Entry("a1", 7), LexiconFile.Entry("", 9), LexiconFile.Entry("zero", 0),
            ),
            listOf(LexiconFile.Pair("hello", "world", 30), LexiconFile.Pair("hello", "help", 10), LexiconFile.Pair("the", "world", 5), LexiconFile.Pair("the", "nothing", 5)),
        ),
    )

    @Test fun wordsRoundTripWithTheirLettersAndAlphabeticalIds() {
        val l = sample()
        // keys: a-z only (the apostrophe goes), lowercase; entries without a letter or with a zero count are dropped
        assertTrue(l.idOf("the") >= 0 && l.idOf("hello") >= 0 && l.idOf("world") >= 0)
        assertEquals(-1, l.idOf("zero")); assertEquals(-1, l.idOf("nothing"))
        for (w in listOf("the", "hello", "help", "world")) {
            val id = l.idOf(w)
            assertEquals(w, l.text(id)); assertEquals(w.length, l.length(id))
            for (i in w.indices) assertEquals(w[i] - 'a', l.letter(id, i))
        }
        // alphabetical by key: "cat" < "dont"/"don't" < "hello" < "help" < "its"/"it's" < "the" < "world"
        val order = (0 until l.size).map { l.text(it) }
        assertEquals(order.indexOf("hello") < order.indexOf("help"), true)
        assertEquals(order.indexOf("help") < order.indexOf("the"), true)
        assertEquals(order.indexOf("the") < order.indexOf("world"), true)
    }

    @Test fun anApostropheWordIsTypedWithItsApostropheButGlidedWithoutIt() {
        val l = sample()
        val id = l.idOf("don't")
        assertTrue(id >= 0)
        assertEquals("don't", l.text(id))
        assertEquals(4, l.length(id)) // d o n t
        assertEquals('t' - 'a', l.letter(id, 3))
        assertTrue(l.idOf("dont") >= 0 && l.idOf("dont") != id) // the plain spelling is its own word with the same keys
        assertTrue(l.idOf("it's") != l.idOf("its"))
    }

    @Test fun probabilitiesFollowTheCountsAndBucketsGroupByFirstAndLastLetter() {
        val l = sample()
        assertTrue(l.logProb(l.idOf("the")) > l.logProb(l.idOf("hello")))
        assertTrue(l.logProb(l.idOf("hello")) > l.logProb(l.idOf("help")))
        assertTrue(l.logProb(l.idOf("the")) < 0f)
        val h = l.bucket('h' - 'a', 'o' - 'a')
        assertEquals(listOf("hello"), h.map { l.text(it) })
        assertTrue(l.bucket('q' - 'a', 'q' - 'a').isEmpty())
    }

    @Test fun pairsGiveTheChanceOfAWordAfterAnother() {
        val l = sample()
        val hello = l.idOf("hello"); val world = l.idOf("world"); val help = l.idOf("help")
        val a = l.bigramLogProb(hello, world); val b = l.bigramLogProb(hello, help)
        assertTrue(a > b) // 30 against 10
        assertEquals(Math.log(30.0 / 40.0), a.toDouble(), 0.02)
        assertTrue(l.bigramLogProb(hello, l.idOf("the")).isNaN())
        assertEquals(listOf("help", "world"), l.successors(hello).map { l.text(it) }.sorted())
        assertEquals(1, l.successors(l.idOf("the")).size) // "the nothing" has no known second word
        assertEquals(3, l.bigramCount)
    }

    @Test fun anArrayLexiconAnswersTheSameQuestions() {
        val a = ArrayLexicon(listOf("hello" to 50L, "help" to 40L, "it's" to 9L, "x" to 0L))
        assertEquals(3, a.size)
        val id = a.idOf("it's")
        assertEquals(3, a.length(id)); assertEquals("it's", a.text(id))
        assertTrue(a.logProb(a.idOf("hello")) > a.logProb(a.idOf("help")))
        assertEquals(1, a.bucket('h' - 'a', 'o' - 'a').size)
        assertTrue(a.bigramLogProb(0, 1).isNaN())
        assertFalse(a.idOf("nothing") >= 0)
    }

    @Test(expected = IllegalArgumentException::class) fun aFileThatIsNotAWordListIsRefused() { LexiconFile.read(ByteArray(64)) }
}
