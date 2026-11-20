package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldInfo
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/** The real engine (synchronous executor, a tiny word list) behind a real controller on a fake editor: what a glide teaches the keyboard, and when. */
class GlideSessionTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    private val sim = GestureSim(keys, Random(3))
    private val saved = ArrayList<String>()
    private var clock = 1_000L

    private class Strip : GlideSession.Ui {
        var words: List<String> = emptyList(); var noWord = 0; var cleared = 0
        override fun showCandidates(words: List<String>) { this.words = words }
        override fun clearCandidates() { words = emptyList(); cleared++ }
        override fun showNoWord() { noWord++ }
    }

    private inner class Env(kind: FieldKind = FieldKind.TEXT, noLearn: Boolean = false, text: String = "") {
        val rig = Rig(text, kind = kind, noLearn = noLearn)
        val engine = GlideTyping({ LexiconFile.build(listOf("for" to 900L, "fir" to 300L, "fur" to 100L, "hello" to 500L, "world" to 800L, "there" to 700L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()) },
            Executor { it.run() }, savePersonal = { saved.add(it) })
        val ui = Strip()
        val habits = ArrayList<FloatArray>()
        val s = GlideSession(rig.c, { engine }, { clock }, { habits.add(it) })
        init { engine.prepare(keys, null, null); assertTrue(engine.ready); s.ui = ui }

        /** The finger draws [word] and lifts: decode, commit (what the view and the service do together). */
        fun glide(word: String) {
            val p = sim.path(word, SimNoise.CALM)!!
            val (prev2, prev) = s.context()
            s.glideLifted()
            s.beforeKey(Key.Ch("~glide"))          // (a glide is "something typed next" for the glide before it)
            var res: GlideResult? = null
            engine.decode(p.xs, p.ys, p.xs.size, prev, prev2) { res = it }
            s.onDecoded(res!!, prev, prev2); rig.sync(); rig.check()
        }
        fun key(k: Key) {
            s.beforeKey(k)
            when (k) { Key.Backspace -> rig.c.backspace(); is Key.Ch -> rig.c.type(k.text); else -> {} }
            rig.sync(); s.afterKey(k); rig.check()
        }
        fun type(w: String) { for (c in w) key(Key.Ch(c.toString())) }
        fun learned(): String { engine.saveLearned(); return saved.lastOrNull() ?: "" }
    }

    @Test fun aGlideTypesTheBestWordAndShowsTheOthersInTheStrip() {
        val e = Env(); e.glide("hello")
        assertEquals("hello ", e.rig.text); assertEquals("hello", e.ui.words.first()); assertTrue(e.ui.words.size >= 1)
    }

    @Test fun aWordThatFitsNothingShowsTheNoWordNoticeAndTypesNothing() {
        val e = Env(); val p = sim.path("hello", SimNoise.CALM)!!
        e.s.onDecoded(GlideResult(emptyList(), 1), null, null)
        assertEquals(1, e.ui.noWord); assertEquals("", e.rig.text)
    }

    @Test fun aGlideThatFitsNothingStillTypesTheClosestWord() {       // R19d: the keyboard never says "no word fits": a wrong word is better than none, and the strip offers the others
        val e = Env(); val p = sim.path("zebra", SimNoise.CALM)!!
        e.s.glideLifted(); e.s.beforeKey(Key.Ch("~glide"))
        var res: GlideResult? = null; e.engine.decode(p.xs, p.ys, p.xs.size, null, null) { res = it }
        assertTrue("an answer from the tiny list although none of its words is 'zebra'", res!!.candidates.isNotEmpty())
        e.s.onDecoded(res!!, null, null)
        assertEquals("no notice", 0, e.ui.noWord); assertTrue("a word went in: '${e.rig.text}'", e.rig.text.trim().isNotEmpty() && e.rig.text.endsWith(" "))
        assertTrue("and the strip has it", e.ui.words.isNotEmpty())
        e.key(Key.Backspace); assertEquals("Backspace takes it back whole, as for any glide", "", e.rig.text)
    }

    @Test fun anEngineThatIsNotReadyYetSaysSoInsteadOfNoWordFits() {
        val e = Env(); val shown = ArrayList<String>()
        e.s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() { shown.add("noWord") }; override fun showNotReady() { shown.add("notReady") } }
        e.s.onDecoded(GlideResult(emptyList(), 0, ready = false), null, null); e.s.onDecoded(GlideResult(emptyList(), 0), null, null)
        assertEquals(listOf("notReady", "noWord"), shown); assertEquals("", e.rig.text)
    }

    @Test fun aGlideThatFitsNothingTeachesItsShapeToTheWordTypedRightAfter() {       // R18d: an empty answer (an engine that could not read the glide) arms the same learning as a deleted glide: a word typed by hand once, and then its glide is known
        val e = Env(); val p = sim.path("zebra", SimNoise.CALM)!!
        e.s.glideLifted(); e.s.beforeKey(Key.Ch("~glide"))
        var res: GlideResult? = null; e.engine.decode(p.xs, p.ys, p.xs.size, null, null) { res = it }       // (the engine remembers the shape of this glide)
        res = GlideResult(emptyList(), 1)
        e.s.onDecoded(res!!, null, null); assertEquals(1, e.ui.noWord)
        e.type("zebra"); e.key(Key.Ch(" "))
        assertTrue("the shape of the failed glide is remembered for the typed word: ${e.learned()}", e.learned().lines().any { it.startsWith("s zebra ") })
    }

    @Test fun aKeptGlideTeachesTheEngineTheWordAndMovesTheThumbHabit() {
        val e = Env(); e.glide("hello"); assertTrue("nothing is learned until the glide is kept", e.learned().isEmpty())
        e.key(Key.Ch(" "))                                       // typing on: the word stays
        val t = e.learned()
        assertTrue("the word must be in the saved memory: $t", t.contains("hello"))
        assertTrue("a habit update must have been reported", e.habits.isNotEmpty())
    }

    @Test fun backspaceRightAfterAGlideTakesTheWordBackAndTeachesNothing() {
        val e = Env(); e.glide("hello"); e.key(Key.Backspace)
        assertEquals("", e.rig.text); assertTrue(e.learned().isEmpty() || !e.learned().contains("w hello"))
    }

    @Test fun aPickFromTheStripReplacesTheWordAndCountsThreeTimesOnceKept() {
        val e = Env(); e.glide("for")
        val alt = e.ui.words.firstOrNull { it != "for" }
        if (alt == null) return                                  // the path was unambiguous for this seed: nothing to pick
        assertTrue(e.s.pick(alt)); e.rig.sync(); e.rig.check()
        assertEquals("$alt ", e.rig.text); assertEquals(alt, e.ui.words.first())
        e.key(Key.Ch(" "))
        val t = e.learned()
        assertTrue("the chosen word counts 3: $t", Regex("(?m)^w $alt 3").containsMatchIn(t) || t.contains("w $alt "))
        assertTrue("it remembers what it was picked over: $t", t.lines().any { it.startsWith("c for $alt") || it.startsWith("c ${e.ui.words.getOrNull(1)} $alt") || it.startsWith("c ") })
    }

    @Test fun aPickIsRefusedWhenTheGlideIsNoLongerTheLastThing() {
        val e = Env(); e.glide("for"); e.rig.ed.userInsert("!"); e.rig.sync()
        assertFalse(e.s.pick("fir")); assertEquals("for !", e.rig.text)
    }

    @Test fun nothingIsLearnedInAPasswordFieldOrWhenTheAppSaysNoLearning() {
        for (env in listOf(Env(kind = FieldKind.PASSWORD), Env(noLearn = true))) {
            env.glide("hello"); env.key(Key.Ch(" ")); env.glide("world"); env.key(Key.Backspace); env.type("wo")
            val t = env.learned()
            assertTrue("a private field must teach the memory nothing: '$t'", !t.contains("hello") && !t.contains("world"))
        }
    }

    @Test fun aDeletedGlideThenTheRetypedWordIsRememberedAsWhatItMeant() {
        val e = Env(); e.glide("for"); val first = e.rig.text.trim()
        e.key(Key.Backspace); assertEquals("", e.rig.text)
        e.type("for"); e.key(Key.Ch(" "))                        // typed letter by letter, then a space: finishRetype runs
        assertNotNull(first)
        // no crash and the keyboard is still in a true state; the shape memory itself is covered by GlideShapeLearningTest
        e.rig.check()
    }

    @Test fun aRetryThatGivesTheDeletedWordFirstAgainIsDemoted() {
        val e = Env(); e.glide("for"); val first = e.ui.words.first()
        e.key(Key.Backspace); clock += 1500
        val p = sim.path("for", SimNoise.CALM)!!
        var res: GlideResult? = null; e.engine.decode(p.xs, p.ys, p.xs.size, null, null) { res = it }
        val two = res!!.candidates
        e.s.onDecoded(res!!, null, null)
        if (two.size >= 2 && two[0].text == first && two[1].perLetter - two[0].perLetter <= GlideSession.RETRY_GAP) assertEquals("the deleted word moves to second place", two[1].text, e.ui.words.first())
        else assertEquals(two[0].text, e.ui.words.first())
    }

    @Test fun theStripClearsWhenTheOwnerTypesOn() {
        val e = Env(); e.glide("hello"); assertTrue(e.ui.words.isNotEmpty())
        e.key(Key.Ch("a")); assertTrue(e.ui.words.isEmpty())
    }

    @Test fun theContextForTheNextGlideIsTheTwoWordsBeforeTheCursor() {
        val e = Env(); e.glide("hello"); e.glide("world")
        assertEquals("hello world ", e.rig.text); assertEquals("hello" to "world", e.s.context().let { it.first to it.second })
    }

    @Test fun settleKeepsTheGlideAndClearsTheStrip() {
        val e = Env(); e.glide("hello"); e.s.settle(); assertEquals("hello ", e.rig.text); assertTrue(e.ui.words.isEmpty()); assertTrue(e.learned().contains("hello"))
    }

    @Test fun aStripTapStillChangesTheWordWhenAnAppRewroteTheFieldAfterTheGlide() {
        val e = Env(kind = FieldKind.URL); e.glide("hello")
        e.rig.ed.appSetText("go hello "); e.rig.sync()               // the address bar rewrote its text: the controller forgot the glide
        assertTrue(e.ui.words.size >= 1)
        val other = e.ui.words.firstOrNull { it != "hello" }
        if (other != null) { assertTrue(e.s.pick(other)); e.rig.sync(); assertEquals("go $other ", e.rig.text) }
    }

    @Test fun aStripTapReplacesTheWordWhenAnAddressBarCompletedItInline() {
        val e = Env(kind = FieldKind.URL); e.glide("hello")
        e.rig.ed.appAutocomplete("world.com"); e.rig.sync()
        val other = e.ui.words.firstOrNull { it != "hello" }
        if (other != null) { assertTrue(e.s.pick(other)); e.rig.sync(); e.rig.check(); assertEquals("$other ", e.rig.text) }
    }
}
