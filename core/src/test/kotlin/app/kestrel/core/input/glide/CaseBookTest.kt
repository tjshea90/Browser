package app.kestrel.core.input.glide

import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/** R18b: "auto capitalize ... where it makes sense": the words that are written with a capital wherever they stand, built in and learned. */
class CaseBookTest {
    private val keys = GlideEval.keysFor(564f, 160f)

    @Test fun daysMonthsNamesAndBrandsHaveTheirCapital() {
        for ((lower, form) in listOf("monday" to "Monday", "december" to "December", "sarah" to "Sarah", "mexico" to "Mexico", "spanish" to "Spanish", "iphone" to "iPhone", "youtube" to "YouTube", "christmas" to "Christmas", "california" to "California"))
            assertEquals(lower, form, CaseBook.form(lower, null))
        assertTrue("a few hundred words, not a few: ${CaseBook.builtinSize()}", CaseBook.builtinSize() > 500)
    }

    @Test fun everydayWordsAreNeverCapitalisedOnTheirOwn() {
        for (w in listOf("will", "mark", "rose", "grace", "may", "march", "turkey", "polish", "windows", "zoom", "hunter", "bill", "april".takeIf { false } ?: "the", "hello", "monday".takeIf { false } ?: "world"))
            assertNull(w, CaseBook.form(w, null))
    }

    @Test fun mayAndMarchAreMonthsOnlyAfterTheWordsThatMakeThemDates() {
        assertEquals("May", CaseBook.form("may", null, "in")); assertEquals("March", CaseBook.form("march", null, "last")); assertEquals("May", CaseBook.form("may", null, "early"))
        assertNull(CaseBook.form("may", null, "you")); assertNull(CaseBook.form("may", null, null)); assertNull(CaseBook.form("march", null, "the"))
    }

    @Test fun aNameTypedWithItsCapitalTwiceIsTheirsFromThenOn() {
        val pm = PersonalModel()
        assertNull(CaseBook.form("zorblax", pm))
        pm.addCase("Zorblax"); assertNull("one use is not a habit", CaseBook.form("zorblax", pm))
        pm.addCase("Zorblax"); assertEquals("Zorblax", CaseBook.form("zorblax", pm)); assertEquals("Zorblax", pm.caseForm("zorblax"))
        pm.addCase("iPhone"); pm.addCase("iPhone"); assertEquals("iPhone", CaseBook.form("iphone", pm))
    }

    @Test fun aCapitalTheKeyboardPutInAndThePersonTookBackIsGivenUp() {
        val pm = PersonalModel()
        assertEquals("Monday", CaseBook.form("monday", pm))
        pm.addCase("monday", 2f); assertEquals("they write it small now", "", pm.caseForm("monday")); assertNull(CaseBook.form("monday", pm))
        pm.addCase("Monday"); pm.addCase("Monday"); pm.addCase("Monday"); pm.addCase("Monday"); pm.addCase("Monday")
        assertEquals("and then a lot of capitals win it back", "Monday", CaseBook.form("monday", pm))
    }

    @Test fun whatWasLearnedIsSavedMergedAndExported() {
        val pm = PersonalModel(); repeat(2) { pm.addCase("Zorblax") }; pm.addCase("monday", 2f)
        val back = PersonalModel.parse(pm.serialize())
        assertEquals("Zorblax", back.caseForm("zorblax")); assertEquals("", back.caseForm("monday")); assertEquals(2, back.caseCount)
        val other = PersonalModel(); other.mergeFrom(back); other.mergeFrom(back)
        assertEquals("importing twice changes nothing", 2, other.caseCount); assertEquals("Zorblax", other.caseForm("zorblax"))
        val file = LearnedExport.write(pm.also { it.add(null, "hello", 1f) }, null, null)
        assertTrue(file.lines().any { it.startsWith("k zorblax Zorblax ") })
        assertEquals("Zorblax", LearnedExport.read(file)!!.model.caseForm("zorblax"))
        pm.clear(); assertEquals(0, pm.caseCount); assertNull(pm.caseForm("zorblax"))
    }

    @Test fun aDamagedCaseLineIsSkipped() {
        val m = PersonalModel.parse("k sarah Sarah 3.00\nk sarah Bob 3.00\nk x X 2.00\nk monday Monday -4\nk dog DOG notanumber\nk tom Tom 1e99\nw hello 1.0\n")
        assertEquals("Sarah", m.caseForm("sarah")); assertNull(m.caseForm("x")); assertNull(m.caseForm("monday")); assertNull(m.caseForm("tom")); assertEquals(1f, m.weightOf("hello"), 0.01f)
    }

    // ---- the strip

    private fun lex(): BinaryLexicon = LexiconFile.read(LexiconFile.build(listOf("monday" to 5_000_000L, "money" to 6_000_000L, "on" to 90_000_000L, "see" to 40_000_000L, "you" to 60_000_000L, "sarah" to 700_000L, "sar" to 10L, "zzfiller" to 400_000_000_000L).map { LexiconFile.Entry(it.first, it.second) },
        listOf(LexiconFile.Pair("on", "monday", 900_000L), LexiconFile.Pair("see", "you", 800_000L))))

    @Test fun theStripShowsANameWithItsCapital() {
        val l = lex()
        val r = Suggester.suggest(l, null, keys, "mond", null, null, 3)
        assertEquals("Monday", r.words.first()); assertTrue("money", r.words.none { it == "Money" })
        assertEquals(listOf("Monday"), Suggester.nextWords(l, null, "on", null, 3).words.take(1))
        assertEquals("what was typed in capitals keeps its own shape", "MONDAY", Suggester.suggest(l, null, keys, "MOND", null, null, 3).words.first())
    }

    // ---- learning from typing

    private inner class Env(text: String = "") {
        val saved = ArrayList<String>()
        val rig = Rig(text)
        val engine = GlideTyping({ lex().let { LexiconFile.build(LexiconFile.entriesOf(it), LexiconFile.pairsOf(it)) } }, Executor { it.run() }, savePersonal = { saved.add(it) })
        val learner = TypingLearner(rig.c, { engine }, { 1_000L })
        init { engine.prepare(keys, null, null) }
        fun key(k: app.kestrel.core.input.Key.Ch) { learner.beforeKey(k, null); rig.c.type(k.text); rig.sync(); learner.afterKey() }
        fun type(w: String) { for (c in w) key(app.kestrel.core.input.Key.Ch(c.toString())) }
    }

    @Test fun aCapitalInsideASentenceIsLearnedButACapitalAtItsStartIsNot() {
        val e = Env(); e.type("hello "); e.type("Zorblax "); e.type("and "); e.type("Zorblax ")
        assertEquals("seen twice inside sentences", "Zorblax", e.engine.caseFormOf("zorblax", null))
        val s = Env(); s.type("Zorblax "); s.type("Zorblax "); s.rig.act { enter() }; s.type("Zorblax ")
        assertNull("at the start of a sentence or a paragraph a capital says nothing", s.engine.caseFormOf("zorblax", null))
    }

    @Test fun shoutingAndCapsLockAreNotAHabit() {
        val e = Env(); e.type("hello "); e.type("STOPPING "); e.type("and "); e.type("STOPPING "); assertNull(e.engine.caseFormOf("stopping", null))
        val l = Env(); l.rig.act { lockShift() }; l.type("hello "); l.type("Zorblax "); l.type("Zorblax "); assertNull(l.engine.caseFormOf("zorblax", null))
    }
}
