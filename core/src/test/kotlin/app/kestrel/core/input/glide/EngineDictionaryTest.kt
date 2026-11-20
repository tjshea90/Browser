package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/** The owner's dictionary in the engine: added words are found by their glide, removed words are never offered (glide, strip, next word) and never learned. */
class EngineDictionaryTest {
    private val direct = Executor { it.run() }
    private val keys = GlideEval.keysFor()
    private val bytes = LexiconFile.build(listOf("the" to 530_000_000_000L, "hello" to 500_000_000L, "help" to 400_000_000L, "tui" to 5_000L, "tue" to 4_000_000L, "world" to 900_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())

    private fun engine(saved: StringBuilder = StringBuilder(), start: String? = null): GlideTyping {
        val g = GlideTyping({ bytes }, direct, previewExecutor = direct, loadDictionary = { start }, saveDictionary = { saved.setLength(0); saved.append(it) })
        g.prepare(keys, null, null)
        return g
    }
    private fun glide(g: GlideTyping, word: String, seed: Long = 4): List<String> {
        val p = GestureSim(keys, Random(seed)).path(word, SimNoise.CALM)!!
        var out: List<String> = emptyList()
        g.decode(p.xs, p.ys, p.xs.size, null) { out = it.candidates.map { c -> c.text } }
        return out
    }

    @Test fun anAddedWordIsFoundByItsGlide() {
        val g = engine()
        assertFalse("zelda" in glide(g, "zelda"))
        var r: UserDictionary.Result? = null
        g.addDictionaryWord("Zelda") { r = it }; assertEquals(UserDictionary.Result.OK, r)
        assertEquals("zelda", glide(g, "zelda").firstOrNull())
    }

    @Test fun aRemovedWordIsNeverOffered() {
        val g = engine()
        assertTrue("tui" in glide(g, "tui") || "tue" in glide(g, "tui"))      // sanity: close words are candidates
        g.blockWord("tui"); g.blockWord("tue")
        for (seed in 1L..8L) { val r = glide(g, "tui", seed); assertFalse("tui" in r); assertFalse("tue" in r) }
    }

    @Test fun removedWordsAreAlsoLeftOutOfTheStripAndKeptOutOfLearning() {
        val g = engine()
        g.blockWord("help")
        var s: Suggestions? = null
        g.suggest("hel", null, null, 3) { s = it }
        assertFalse("help" in s!!.words); assertTrue("hello" in s!!.words)
        g.learnWord(null, "help", 3f)
        g.suggest("hel", null, null, 3) { s = it }
        assertFalse("help" in s!!.words)
    }

    @Test fun takingAWordBackRestoresIt() {
        val g = engine()
        g.blockWord("hello"); assertFalse("hello" in glide(g, "hello"))
        var ok = false; g.unblockWord("hello") { ok = it }; assertTrue(ok); assertEquals("hello", glide(g, "hello").first())
    }

    @Test fun theDictionaryIsSavedAndComesBackInANewEngine() {
        val saved = StringBuilder()
        val g = engine(saved); g.addDictionaryWord("zelda"); g.blockWord("tui")
        assertTrue(saved.toString().startsWith(UserDictionary.HEADER))
        val g2 = engine(start = saved.toString())
        var snap: Pair<List<String>, List<String>>? = null
        g2.dictionarySnapshot { a, b -> snap = a to b }
        assertEquals(listOf("zelda"), snap!!.first); assertEquals(listOf("tui"), snap!!.second)
        assertEquals("zelda", glide(g2, "zelda").firstOrNull()); assertFalse("tui" in glide(g2, "tui"))
    }

    @Test fun lookupListsTheCommonestWordsWithAPrefix() {
        val g = engine()
        var known = false; var words: List<String> = emptyList()
        g.lookup("hel", 5) { k, w -> known = k; words = w }
        assertFalse(known); assertEquals(listOf("hello", "help"), words)
        g.lookup("hello", 5) { k, _ -> known = k }; assertTrue(known)
    }
}
