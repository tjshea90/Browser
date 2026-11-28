package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/** What typing with taps teaches the keyboard: the real engine (synchronous executor) behind a real controller on a fake editor. */
class TypingLearnerTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    private var clock = 1_000L

    private inner class Env(kind: FieldKind = FieldKind.TEXT, noLearn: Boolean = false, text: String = "") {
        val saved = ArrayList<String>()
        val rig = Rig(text, kind = kind, noLearn = noLearn)
        val engine = GlideTyping({
            LexiconFile.build(listOf("hello" to 500_000L, "hellp" to 10L, "help" to 400_000L, "world" to 800_000L, "the" to 9_000_000L, "there" to 700_000L, "teh" to 1L, "zzfiller" to 500_000_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        }, Executor { it.run() }, savePersonal = { saved.add(it) })
        val learner = TypingLearner(rig.c, { engine }, { clock })
        init { engine.prepare(keys, null, null); assertTrue(engine.ready) }

        fun key(k: Key, fix: String? = null) {
            learner.beforeKey(k, fix)
            when (k) { Key.Backspace -> rig.c.backspace(); is Key.Ch -> if (fix != null) rig.c.typeCorrected(fix, k.text) else rig.c.type(k.text); Key.Go -> rig.c.enter(); else -> {} }
            rig.sync(); learner.afterKey(); rig.check()
        }
        fun type(w: String) { for (c in w) key(Key.Ch(c.toString())) }
        fun model(): PersonalModel { engine.saveLearned(); return PersonalModel.parse(saved.lastOrNull() ?: "") }
    }

    @Test fun everyFinishedWordIsLearnedWithItsPairs() {
        val e = Env(); e.type("hello world "); e.type("hello world ")
        val m = e.model()
        assertEquals(2f, m.weightOf("hello"), 0.01f); assertEquals(2f, m.weightOf("world"), 0.01f)
        assertTrue(m.pairProb("hello", "world") > 0.9f)
        assertEquals("the word still being typed is not learned yet", 0f, Env().also { it.type("hel") }.model().weightOf("hel"), 0f)
    }

    @Test fun aWordEndedByPunctuationOrEnterIsLearnedToo() {
        val e = Env(); e.type("hello"); e.key(Key.Ch(",")); e.type(" world"); e.key(Key.Go)
        val m = e.model(); assertEquals(1f, m.weightOf("hello"), 0.01f); assertEquals(1f, m.weightOf("world"), 0.01f)
    }

    @Test fun nothingIsLearnedWhereLearningIsOff() {
        val pw = Env(kind = FieldKind.PASSWORD); pw.type("hello world "); assertTrue(pw.model().isEmpty)
        val inc = Env(noLearn = true); inc.type("hello world "); assertTrue(inc.model().isEmpty)
        val url = Env(kind = FieldKind.URL); url.type("hello world "); assertTrue(url.model().isEmpty)
        val mail = Env(kind = FieldKind.EMAIL); mail.type("hello world "); assertTrue(mail.model().isEmpty)
    }

    @Test fun aWordTakenBackIsUnlearnedAndTheWordTypedInItsPlaceIsWhatWasMeant() {
        val e = Env(); e.type("hellp ")
        assertEquals(1f, e.model().weightOf("hellp"), 0.01f)
        e.key(Key.Backspace); e.key(Key.Backspace)                       // the space, then the p
        assertEquals("the slip it was is forgotten", 0f, e.model().weightOf("hellp"), 0.01f)
        e.type("o "); clock += 2_000
        val m = e.model()
        assertEquals("the retyped word counts triple", 3f, m.weightOf("hello"), 0.01f)
        assertTrue("and the slip is remembered as a correction", m.confusionWeight("hellp", "hello") > 0f)
        assertEquals("hello ", e.rig.text)
    }

    @Test fun aDifferentWordTypedAfterErasingIsNotACorrection() {
        val e = Env(); e.type("hello ")
        repeat(6) { e.key(Key.Backspace) }
        e.type("world "); val m = e.model()
        assertEquals("not a slip of the same word: counts once", 1f, m.weightOf("world"), 0.01f)
        assertEquals(0f, m.confusionWeight("hello", "world"), 0f)
        assertEquals("what was erased is forgotten", 0f, m.weightOf("hello"), 0.01f)
    }

    @Test fun aRetypeLongAfterwardsIsNotLinked() {
        val e = Env(); e.type("hellp "); e.key(Key.Backspace); e.key(Key.Backspace); clock += 60_000; e.type("o ")
        val m = e.model(); assertEquals(1f, m.weightOf("hello"), 0.01f); assertEquals(0f, m.confusionWeight("hellp", "hello"), 0f)
    }

    @Test fun aWordPickedFromTheStripCountsDoubleAndAFixedTypoIsRemembered() {
        val e = Env(); e.type("hel")
        assertTrue(e.learner.pick("hello")); e.rig.sync(); e.rig.check()
        assertEquals("hello ", e.rig.text)
        var m = e.model(); assertEquals(2f, m.weightOf("hello"), 0.01f); assertEquals(0f, m.confusionWeight("hel", "hello"), 0f)
        val f = Env(); f.type("helo"); assertTrue(f.learner.pick("hello")); f.rig.sync()
        assertEquals("hello ", f.rig.text); m = f.model(); assertTrue(m.confusionWeight("helo", "hello") > 0f)
    }

    @Test fun anAutomaticFixTakenBackWithBackspaceIsNeverMadeAgain() {
        val e = Env(); e.type("teh"); e.key(Key.Ch(" "), fix = "the")
        assertEquals("the ", e.rig.text)
        var m = e.model(); assertEquals(1f, m.weightOf("the"), 0.01f); assertTrue(m.confusionWeight("teh", "the") > 0f)
        e.key(Key.Backspace)
        assertEquals("one Backspace gives the typed word back", "teh", e.rig.text); assertEquals("teh", e.rig.c.composing)
        m = e.model(); assertEquals(0f, m.weightOf("the"), 0.01f); assertEquals("what they typed is theirs, twice", 2f, m.weightOf("teh"), 0.01f)
        assertTrue("so it is a known word now", Suggester.suggest(LexiconFile.read(LexiconFile.build(listOf(LexiconFile.Entry("the", 9_000_000L), LexiconFile.Entry("zzfiller", 500_000_000_000L)), emptyList())), m, keys, "teh", null, null).typedKnown)
    }

    @Test fun aGlideAndItsWordAreLeftToTheGlideSession() {
        val e = Env(); e.rig.act { commitGlide("hello") }
        e.key(Key.Backspace)                                              // takes the glide back: not a typed word
        assertTrue(e.model().isEmpty)
    }

    @Test fun learningNeverBreaksTheTextWhateverTheKeys() {
        val e = Env(); val rnd = java.util.Random(11)
        val pool = listOf("a", "e", "h", "l", "o", " ", ".", ",", "'", "1", "t", "w")
        repeat(600) { when (rnd.nextInt(10)) { 0, 1 -> e.key(Key.Backspace); 2 -> e.key(Key.Go); else -> e.key(Key.Ch(pool[rnd.nextInt(pool.size)])) } }
        assertNotNull(e.model())
    }
}
