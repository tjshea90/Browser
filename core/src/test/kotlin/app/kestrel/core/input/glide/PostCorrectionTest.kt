package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/**
 * R26a (2026-11-19): the word a glide gives can show that the glided word BEFORE it was a look-alike. Scores are set by hand here ("form" fits the path a little better than "from"); the list says that "the"
 * follows "from" and never follows "form". Measured on real swipes this fixes about eight words for each one it spoils; these tests pin when it must act and, as important, when it must NOT.
 */
class PostCorrectionTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    private val bytes = LexiconFile.build(
        listOf("form" to 600L, "from" to 700L, "the" to 300L, "world" to 800L, "hello" to 500L).map { LexiconFile.Entry(it.first, it.second) },
        listOf(LexiconFile.Pair("from", "the", 3000L), LexiconFile.Pair("from", "world", 300L), LexiconFile.Pair("hello", "world", 400L))
    )
    private fun cand(text: String, score: Float) = GlideCandidate(text, score, 0.2f, 0f, text.length)

    private inner class Env {
        val rig = Rig("")
        val saved = ArrayList<String>()
        val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { saved.add(it) })
        val s = GlideSession(rig.c, { engine }, { 1_000L }, { })
        init { engine.prepare(keys, null, null); assertTrue(engine.ready); s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() {} } }
        fun glide(vararg c: GlideCandidate, lm: Float = 1.0f, more: List<GlideCandidate> = emptyList()) {
            val (p2, p) = s.context()      // (like the presenter: a glide sends no key to the session, only its decoded answer)
            s.onDecoded(GlideResult(c.toList(), 1, true, lm, more), p, p2); rig.sync(); rig.check()
        }
    }

    @Test fun theNextWordFixesAGlidedLookAlike() {
        val e = Env()
        e.engine.stats.onDecoded(1, true)                  // (the Diagnostics line only appears once a real glide was counted)
        e.glide(cand("form", 5.0f), cand("from", 4.8f))
        assertEquals("form ", e.rig.text)
        e.glide(cand("the", 5.0f))
        assertEquals("the next word shows it was \"from\"", "from the ", e.rig.text)
        assertEquals(1, e.engine.stats.line().let { Regex("fixed by the next word (\\d+) times").find(it)?.groupValues?.get(1)?.toInt() ?: 0 })
    }

    @Test fun theFixedWordIsWhatIsLearned() {
        val e = Env()
        e.glide(cand("form", 5.0f), cand("from", 4.8f)); e.glide(cand("the", 5.0f))
        e.glide(cand("world", 5.0f))                      // the glide after settles the one before
        e.engine.saveLearned()
        assertEquals("from the world ", e.rig.text)
        val pm = PersonalModel.parse(e.saved.last())
        assertTrue(pm.weightOf("from") > 0f)
        assertEquals(0f, pm.weightOf("form"), 0f)
    }

    @Test fun nothingChangesWithoutEvidenceFromTheNextWord() {
        val e = Env()
        e.glide(cand("form", 5.0f), cand("from", 4.8f))
        e.glide(cand("hello", 5.0f))                       // "hello" follows neither: the first choice stays
        assertEquals("form hello ", e.rig.text)
    }

    @Test fun aMarginOfEvidenceIsNeededNotJustAnyDifference() {
        val e = Env()
        e.glide(cand("form", 5.0f), cand("from", 3.0f))     // "from" fits the path much worse: the pair cannot make up that much
        e.glide(cand("the", 5.0f))
        assertEquals("form the ", e.rig.text)
    }

    @Test fun aWordThePersonChoseInTheStripIsNeverTouched() {
        val e = Env()
        e.glide(cand("from", 5.0f), cand("form", 4.9f))
        assertTrue(e.s.pick("form"))                       // theirs
        e.glide(cand("the", 5.0f))
        assertEquals("form the ", e.rig.text)
    }

    @Test fun whenTheSettingIsOffNothingIsFixed() {
        val e = Env(); e.s.reviseBefore = false
        e.glide(cand("form", 5.0f), cand("from", 4.8f)); e.glide(cand("the", 5.0f))
        assertEquals("form the ", e.rig.text)
    }

    @Test fun aWordTypedInBetweenIsNotTheLastGlideSoItIsLeftAlone() {
        val e = Env()
        e.glide(cand("form", 5.0f), cand("from", 4.8f))
        e.s.beforeKey(Key.Ch("x")); e.rig.c.type("x"); e.rig.sync(); e.s.afterKey(Key.Ch("x"))
        e.glide(cand("the", 5.0f))
        assertTrue(e.rig.text, e.rig.text.startsWith("form x"))
    }

    @Test fun theFourthAndFifthCandidatesCanWin() {
        val e = Env()
        e.glide(cand("hello", 5.0f), cand("world", 4.9f), more = listOf(cand("from", 4.8f)))
        e.glide(cand("the", 5.0f))
        assertEquals("from the ", e.rig.text)
    }
}
