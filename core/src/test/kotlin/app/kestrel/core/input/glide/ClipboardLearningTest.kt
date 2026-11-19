package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/**
 * Owner R23b (2026-11-18): "never at any time allow the keyboard to learn and save anything from the clipboard. Just because i copy and paste something doesn't mean it is a word to learn for the keyboard."
 * Found: tap typing learned WHATEVER word stood before the cursor when Space, punctuation or Enter was pressed - a pasted word too (with its pair, its sentence-start and its capital).
 */
class ClipboardLearningTest {
    private val keys = GlideEval.keysFor(564f, 160f)

    private inner class Env(text: String = "") {
        val saved = ArrayList<String>()
        val rig = Rig(text)
        val engine = GlideTyping({
            LexiconFile.build(listOf("hello" to 500_000L, "world" to 800_000L, "zebra" to 20L, "pasted" to 10L, "typed" to 400_000L, "the" to 9_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        }, Executor { it.run() }, savePersonal = { saved.add(it) })
        val learner = TypingLearner(rig.c, { engine }, { 1_000L })
        init { engine.prepare(keys, null, null) }
        fun key(k: Key) {
            learner.beforeKey(k, null)
            when (k) { Key.Backspace -> rig.c.backspace(); is Key.Ch -> rig.c.type(k.text); Key.Go -> rig.c.enter(); else -> {} }
            rig.sync(); learner.afterKey(); rig.check()
        }
        fun type(w: String) { for (c in w) key(Key.Ch(c.toString())) }
        fun paste(t: String) { learner.settle(); rig.act { paste(t) } }
        fun model(): PersonalModel { engine.saveLearned(); return PersonalModel.parse(saved.lastOrNull() ?: "") }
    }

    @Test fun aPastedWordIsNotLearnedWhenSpacePunctuationOrEnterFollows() {
        for (end in listOf(Key.Ch(" "), Key.Ch("."), Key.Ch(","), Key.Go)) {
            val e = Env(); e.paste("zebra"); e.key(end); assertTrue("pasted, then $end: nothing learned: ${e.model().wordCount} words", e.model().isEmpty)
        }
        val many = Env(); many.paste("hello world the zebra pasted"); many.key(Key.Ch(" ")); assertTrue(many.model().isEmpty)
    }

    @Test fun aTypedWordIsStillLearnedAsBefore() {
        val e = Env(); e.type("typed "); assertEquals(1f, e.model().weightOf("typed"), 0.01f)
        val c = Env(); c.type("hello"); c.key(Key.Ch(",")); assertEquals(1f, c.model().weightOf("hello"), 0.01f)
    }

    @Test fun aTypoFixedWithBackspaceOnTheWayIsStillATypedWord() {
        val e = Env(); e.type("helx"); e.key(Key.Backspace); e.type("lo "); assertEquals(1f, e.model().weightOf("hello"), 0.01f); assertEquals(0f, e.model().weightOf("helx"), 0f)
    }

    @Test fun lettersTypedNextToAPasteAreNotATypedWord() {
        val e = Env(); e.type("wor"); e.paste("ld"); e.key(Key.Ch(" ")); assertTrue("part typed, part pasted: not learned", e.model().isEmpty)
        val f = Env(); f.paste("wor"); f.type("ld "); assertTrue("part pasted, part typed: not learned", f.model().isEmpty)
    }

    @Test fun theWordsOfAPasteAreNoContextForWhatIsTypedAfterIt() {
        val e = Env(); e.paste("hello world "); e.type("typed "); val m = e.model()
        assertEquals("the typed word is learned", 1f, m.weightOf("typed"), 0.01f); assertEquals("no pair with the pasted word", 0f, m.pairProb("world", "typed"), 0f); assertEquals(0, m.pairCount)
        val h = Env(); h.paste("hello world "); assertNull("the controller sees no word before the paste's end", h.rig.c.previousWord())
        val g = Env(); g.paste("hello world "); assertEquals(null to null, g.rig.c.previousTwo()); g.type("the "); g.type("typed "); assertTrue("what is typed after the paste does pair up", g.model().pairProb("the", "typed") > 0.5f)
    }

    @Test fun aSecondPasteMovesTheBarrierAndTheOlderOneStaysCounted() {
        val e = Env(); e.paste("hello "); e.type("the "); e.paste("world "); e.type("typed ")
        val m = e.model(); assertEquals(0, m.pairCount); assertTrue(m.weightOf("the") > 0f && m.weightOf("typed") > 0f)
    }

    @Test fun theUndoOfAnEditAfterAPasteStillWorks() {
        val e = Env(); e.paste("hello "); e.type("typed"); e.key(Key.Backspace); assertEquals("hello type", e.rig.text)
    }

    @Test fun typedWordSaysWhatWasTypedHere() {
        val e = Env(); e.type("typed"); assertEquals("typed", e.rig.c.typedWord())
        e.paste(" and"); assertNull(e.rig.c.typedWord())
        val f = Env("hello"); assertNull("text that was already in the field is nobody's typing", f.rig.c.typedWord())
        val g = Env(); g.rig.act { commitGlide("hello") }; assertNull(g.rig.c.typedWord())
    }

    @Test fun aCorrectionOfPastedTextIsRemembered_asPasted() {
        val e = Env(); e.paste("a pasted line"); assertTrue(e.rig.c.wasPasted("pasted")); assertFalse(e.rig.c.wasPasted("typed"))
    }
}
