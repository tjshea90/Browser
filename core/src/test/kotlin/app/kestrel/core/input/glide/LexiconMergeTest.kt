package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mixing a second pair corpus (everyday sentences) into an existing compiled list keeps its words and moves the pair chances the way the weights say (docs/DICTIONARY.md, "Casual pairs"). */
class LexiconMergeTest {
    private val words = listOf(
        LexiconFile.Entry("the", 100_000), LexiconFile.Entry("my", 40_000), LexiconFile.Entry("way", 5_000), LexiconFile.Entry("own", 4_000), LexiconFile.Entry("life", 6_000),
        LexiconFile.Entry("on", 30_000), LexiconFile.Entry("don't", 9_000), LexiconFile.Entry("know", 12_000), LexiconFile.Entry("zzfiller", 5_000_000),
    )
    private val books = listOf(
        LexiconFile.Pair("my", "own", 600), LexiconFile.Pair("my", "life", 400), LexiconFile.Pair("on", "the", 900), LexiconFile.Pair("on", "my", 100), LexiconFile.Pair("don't", "know", 50),
    )
    private fun lex() = LexiconFile.read(LexiconFile.build(words, books))

    @Test fun aListCompilesAgainFromItsOwnWordsAndPairs() {
        val a = lex()
        val b = LexiconFile.read(LexiconFile.build(LexiconFile.entriesOf(a), LexiconFile.pairsOf(a)))
        assertEquals(a.size, b.size); assertEquals(a.bigramCount, b.bigramCount)
        for (id in 0 until a.size) {
            assertEquals(a.text(id), b.text(id))
            assertEquals("word ${a.text(id)}", a.logProb(id).toDouble(), b.logProb(id).toDouble(), 0.02)
            val sa = a.successors(id); val sb = b.successors(id)
            assertEquals(sa.toList(), sb.toList())
            for (s in sa) assertEquals(a.bigramLogProb(id, s).toDouble(), b.bigramLogProb(id, s).toDouble(), 0.02)
        }
    }

    @Test fun theCorpusMovesThePairsInProportionToTheWeight() {
        val base = LexiconFile.pairsOf(lex())
        val extra = listOf(LexiconFile.Pair("my", "way", 80), LexiconFile.Pair("my", "life", 20))     // 100 sightings of "my": the corpus says way 80 %, life 20 %
        val m = LexiconFile.mergePairs(base, extra, maxWeight = 1.0, k = 100.0)                           // w = 1.0 * 100 / (100 + 100) = 0.5
        val l = LexiconFile.read(LexiconFile.build(LexiconFile.entriesOf(lex()), m))
        val my = l.idOf("my")
        fun p(second: String) = Math.exp(l.bigramLogProb(my, l.idOf(second)).toDouble())
        // books: own 0.6, life 0.4. corpus: way 0.8, life 0.2. Half and half: own 0.3, life 0.3, way 0.4
        assertEquals(0.3, p("own"), 0.01); assertEquals(0.3, p("life"), 0.01); assertEquals(0.4, p("way"), 0.01)
        assertEquals("a word the corpus did not mention keeps the books' pairs", 0.9, Math.exp(l.bigramLogProb(l.idOf("on"), l.idOf("the")).toDouble()), 0.01)
    }

    @Test fun aWordTheCorpusHardlySawKeepsTheBooksAndAnUnknownOneTakesTheCorpus() {
        val base = LexiconFile.pairsOf(lex())
        val m = LexiconFile.mergePairs(base, listOf(LexiconFile.Pair("my", "way", 1), LexiconFile.Pair("know", "my", 40)), maxWeight = 0.9, k = 1000.0)
        val l = LexiconFile.read(LexiconFile.build(LexiconFile.entriesOf(lex()), m))
        val my = l.idOf("my")
        assertTrue("one sighting changes next to nothing", Math.exp(l.bigramLogProb(my, l.idOf("own")).toDouble()) > 0.59)
        // "know" had no pairs in the books at all: the corpus alone decides
        assertEquals(1.0, Math.exp(l.bigramLogProb(l.idOf("know"), l.idOf("my")).toDouble()), 0.01)
        assertFalse(m.isEmpty())
    }
}
