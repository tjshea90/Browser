package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** R18d, owner 2026-11-18: "make the word typing learning as smart as possible." What the keyboard learns about HOW this person writes, beyond which words: how they begin sentences, how much they trust automatic fixes. */
class AdaptiveLearningTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    private val common = listOf("the", "to", "and", "a", "i", "you", "it", "is", "are", "so", "just", "well", "can", "we", "good", "see", "thanks", "hey", "hi", "ok", "yes", "no", "please", "later", "tomorrow", "morning", "sure", "sorry")
    private fun lex(): BinaryLexicon {
        val e = ArrayList<LexiconFile.Entry>()
        for ((i, w) in common.withIndex()) e.add(LexiconFile.Entry(w, 60_000_000_000L / (i + 3)))
        e.add(LexiconFile.Entry("zzfiller", 300_000_000_000L))
        return LexiconFile.read(LexiconFile.build(e, emptyList()))
    }
    private val main = lex()

    // ---- how the person begins a sentence

    /** This person's messages: they start with "hey", "thanks", "see" or "ok" and say something short. */
    private fun starts(n: Int, seed: Long): List<String> { val rnd = Random(seed); val s = listOf("hey", "hey", "hey", "hey", "thanks", "thanks", "thanks", "see", "see", "ok"); return List(n) { s[rnd.nextInt(s.size)] } }

    private fun hitRate(pm: PersonalModel?, test: List<String>): Float = test.count { w -> Suggester.suggest(main, pm, keys, "", null, null, 3).words.any { it.equals(w, true) } }.toFloat() / test.size

    @Test fun theKeyboardLearnsWhatThisPersonBeginsSentencesWith() {
        val test = starts(200, 9)
        val before = hitRate(null, test)
        val pm = PersonalModel(); for (w in starts(60, 1)) { pm.add(null, w); pm.addStart(w) }          // (what the learner does for every finished word)
        val after = hitRate(pm, test)
        println("sentence starters in the first three: before learning %.2f, after 60 sentences %.2f".format(before, after))
        assertTrue("before: $before", before < 0.15f); assertTrue("after learning the start of the next sentence is on the strip (three slots, four starters: 90 % is the most there is): $after", after >= 0.85f)
    }

    @Test fun fewStartsAreTrustedLessThanManyAndNothingIsOfferedBeforeThereAreAny() {
        assertTrue(PersonalModel().startWords(5).isEmpty())
        val few = PersonalModel(); few.add(null, "hey"); few.addStart("hey"); val many = PersonalModel(); repeat(40) { many.add(null, "hey"); many.addStart("hey") }
        assertTrue(few.startWords(1)[0].second < 0.25f); assertTrue(many.startWords(1)[0].second > 0.8f)
        // one start of a word that is neither common nor theirs does not push the usual guesses off the strip
        val w = Suggester.suggest(main, few, keys, "", null, null, 3).words
        assertTrue("the usual guesses survive a single start: $w", w.size == 3)
    }

    @Test fun startsAreSavedMergedAndExported() {
        val pm = PersonalModel(); repeat(5) { pm.addStart("hey") }; pm.addStart("thanks", 2f)
        val back = PersonalModel.parse(pm.serialize()); assertEquals("hey", back.startWords(2)[0].first); assertEquals(2, back.startCount)
        val other = PersonalModel(); other.mergeFrom(back); other.mergeFrom(back); assertEquals(2, other.startCount); assertEquals("hey", other.startWords(1)[0].first)
        val file = LearnedExport.write(pm.also { it.add(null, "hello", 1f) }, null, null)
        assertTrue(file.lines().any { it.startsWith("b hey 5.00") }); assertEquals(2, LearnedExport.read(file)!!.model.startCount)
        pm.clear(); assertEquals(0, pm.startCount)
    }

    // ---- how much the person trusts automatic fixes

    @Test fun theMarginFollowsHowOftenFixesAreTakenBack() {
        val fresh = PersonalModel(); assertEquals("nothing known: the margin is the standard one", 0f, fresh.autoMarginShift(), 0.01f)
        val strict = PersonalModel(); repeat(40) { strict.addFix(false) }; repeat(14) { strict.addFix(true) }
        assertTrue("a person who takes back a third of the fixes gets stricter: ${strict.autoMarginShift()}", strict.autoMarginShift() > 1.0f)
        val bold = PersonalModel(); repeat(100) { bold.addFix(false) }
        assertTrue("a person who never takes one back gets a little bolder: ${bold.autoMarginShift()}", bold.autoMarginShift() < -0.2f && bold.autoMarginShift() >= -0.3f)
        val back = PersonalModel.parse(strict.serialize()); assertEquals(strict.autoMarginShift(), back.autoMarginShift(), 0.01f)
        assertTrue(PersonalModel.parse("f -3 4\nf x y\nf 1e30 2").autoMarginShift() <= 1.5f)
    }

    @Test fun aPersonWhoUndoesFixesHasFewerAutomaticChangesAndOneWhoNeverDoesKeepsTheStandardOnes() {
        // "vat" is one neighbouring key from "bat" and from "cat"; "jit" from "hit" and "kit": a fix that is only a little likelier than the next word is where the margin decides
        val l = LexiconFile.read(LexiconFile.build(listOf("bat" to 40_000_000L, "cat" to 10_800_000L, "hit" to 70_000_000L, "kit" to 30_000_000L, "zzfiller" to 300_000_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()))
        val strict = PersonalModel(); strict.add(null, "hello"); repeat(40) { strict.addFix(false) }; repeat(16) { strict.addFix(true) }
        val bold = PersonalModel(); bold.add(null, "hello"); repeat(100) { bold.addFix(false) }
        fun fix(typed: String, pm: PersonalModel?) = Suggester.suggest(l, pm, keys, typed, null, null, 3).autoCorrect
        assertEquals("standard: a fix 1.3 nats ahead is made", "bat", fix("vat", null)); assertNull("strict: not any more", fix("vat", strict)); assertEquals("bold keeps it", "bat", fix("vat", bold))
        assertNull("standard: a fix 0.85 nats ahead is not made", fix("jit", null)); assertEquals("bold: made", "hit", fix("jit", bold)); assertNull(fix("jit", strict))
        // and over a whole list of typos the strict person never gets MORE fixes, the bold one never fewer
        val typos = ArrayList<String>()
        for (w in common) if (w.length >= 4) { for (i in 1 until w.length - 1) typos.add(w.removeRange(i, i + 1)); typos.add(w.substring(0, w.length - 1) + (if (w.last() == 'e') 'r' else 'e')) }
        fun count(pm: PersonalModel?): Int = typos.count { Suggester.suggest(main, pm, keys, it, null, null, 3).autoCorrect != null }
        assertTrue(count(strict) <= count(null)); assertTrue(count(bold) >= count(null))
    }

    // ---- the learner feeds them

    private val saved = ArrayList<String>()
    private fun env(): Pair<app.kestrel.core.input.editor.Rig, Pair<GlideTyping, TypingLearner>> {
        val rig = app.kestrel.core.input.editor.Rig("")
        val engine = GlideTyping({ LexiconFile.build(listOf("hello" to 500L, "world" to 800L, "there" to 700L, "the" to 9_000L, "teh" to 1L, "zzfiller" to 500_000_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()) }, java.util.concurrent.Executor { it.run() }, savePersonal = { saved.add(it) })
        engine.prepare(keys, null, null)
        return rig to (engine to TypingLearner(rig.c, { engine }, { 1_000L }))
    }
    private fun type(rig: app.kestrel.core.input.editor.Rig, learner: TypingLearner, text: String) { for (c in text) { val k = app.kestrel.core.input.Key.Ch(c.toString()); learner.beforeKey(k, null); rig.c.type(k.text); rig.sync(); learner.afterKey() } }

    @Test fun theWordsTypedFirstInASentenceAreLearnedAsStarters() {
        val (rig, p) = env(); val (engine, learner) = p
        type(rig, learner, "hello world. there "); engine.saveLearned()
        val m = PersonalModel.parse(saved.last())
        assertEquals(setOf("hello", "there"), m.startWords(5).map { it.first }.toSet())
    }

    @Test fun anAutomaticFixAndItsBackspaceAreCounted() {
        val (rig, p) = env(); val (engine, learner) = p
        val t = app.kestrel.core.input.Key.Ch("t"); val e = app.kestrel.core.input.Key.Ch("e"); val h = app.kestrel.core.input.Key.Ch("h"); val sp = app.kestrel.core.input.Key.Ch(" ")
        for (k in listOf(t, e, h)) { learner.beforeKey(k, null); rig.c.type(k.text); rig.sync(); learner.afterKey() }
        learner.beforeKey(sp, "the"); rig.c.typeCorrected("the", " "); rig.sync(); learner.afterKey()
        assertEquals("the ", rig.text)
        val bs = app.kestrel.core.input.Key.Backspace; learner.beforeKey(bs, null); rig.c.backspace(); rig.sync(); learner.afterKey()
        engine.saveLearned(); val m = PersonalModel.parse(saved.last())
        assertTrue("one fix made, one taken back: ${saved.last()}", saved.last().lines().any { it == "f 1.00 1.00" })
        assertTrue(m.autoMarginShift() > 0.2f)
    }
}
