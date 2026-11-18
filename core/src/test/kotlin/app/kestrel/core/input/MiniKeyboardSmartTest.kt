package app.kestrel.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner R22 (2026-11-18, "put all the updates into the kestrel keyboard"): the text habits of the Swoop keyboard in Kestrel's own panel keyboard, for typing sentences into a web page's text field ([MiniKeyboardModel.smartText]).
 * The address bar leaves it off, and then nothing below happens.
 */
class MiniKeyboardSmartTest {
    private fun model(smart: Boolean = true) = MiniKeyboardModel().also { it.restartText(); it.smartText = smart }
    private fun MiniKeyboardModel.type(s: String): MiniKeyboardModel { for (c in s) press(Key.Ch(c.toString())); return this }

    @Test fun nothingHappensWhileTheSwitchIsOff() {
        val m = model(false); m.type("hello.world i am \"x\""); assertEquals("hello.world i am \"x\"", m.text); assertFalse(m.shift)
        m.commitGlide("word"); assertEquals("hello.world i am \"x\" word ", m.text)
    }

    @Test fun shiftComesOnByItselfAfterASentenceEndAndIsSpentByTheLetter() {
        val m = model(); m.type("hi."); assertFalse("no space yet", m.shift); m.type(" "); assertTrue(m.shift)
        m.type("there"); assertEquals("hi. There", m.text); assertFalse(m.shift)
        val q = model(); q.type("really?"); q.type(" "); assertTrue(q.shift); q.press(Key.Backspace); assertFalse("and off again where it no longer applies", q.shift)
    }

    @Test fun theStartOfAnEmptyTextIsNotASentenceStart() { val m = model(); assertFalse("what the page field already holds is unknown", m.shift); m.type("a"); assertEquals("a", m.text) }

    @Test fun aManualShiftIsLeftAlone() { val m = model(); m.type("ok"); m.press(Key.Shift); assertTrue(m.shift); m.type("X"); assertEquals("okX", m.text) }

    @Test fun aLoneIBecomesACapitalIAtAWordBoundary() {
        val m = model(); m.type("so do i, or i. then i'm sure i'll go and i've seen it, i'd say i "); assertEquals("so do I, or I. Then I'm sure I'll go and I've seen it, I'd say I ", m.text)
        val w = model(); w.type("wi-fi, a.i. and x-i "); assertEquals("wi-fi, a.i. and x-i ", w.text)
        val s = model(); s.type("in it is hi if id "); assertEquals("in it is hi if id ", s.text)
    }

    @Test fun aGlidedIMAndAGlideAfterAFullStopGetTheirCapitals() {
        val m = model(); m.type("well "); m.commitGlide("i'm"); assertEquals("well I'm ", m.text)
        val n = model(); n.type("we left."); n.commitGlide("then"); assertEquals("we left. Then ", n.text)
        val c = model(); c.type("we left,"); c.commitGlide("then"); assertEquals("we left, then ", c.text)
        val a = model(); a.type("see e.g."); a.commitGlide("this"); assertEquals("see e.g. this ", a.text)
        val sw = model(); sw.type("well "); sw.commitGlide("in"); assertTrue(sw.swapGlide("i'm")); assertEquals("well I'm ", sw.text)
    }

    @Test fun aLetterAfterPunctuationGetsItsSpaceAndACapital() {
        val m = model(); m.type("hello.world"); assertEquals("hello. World", m.text)
        val q = model(); q.type("what?no"); assertEquals("what? No", q.text)
        val c = model(); c.type("hello,world"); assertEquals("hello, world", c.text)
        for ((typed, want) in listOf("e.g" to "e.g", "u.s.a" to "u.s.a", "3.14" to "3.14", "1,000" to "1,000", "a@b.com" to "a@b.com", "10:30" to "10:30", "see/this.now" to "see/this.now")) { val x = model(); x.type(typed); assertEquals("typing $typed", want, x.text) }
    }

    @Test fun aSpaceDeletedOnPurposeAfterAFullStopStaysDeleted() {
        val m = model(); m.type("visit example.c"); assertEquals("visit example. C", m.text)
        m.press(Key.Backspace); m.press(Key.Backspace); assertEquals("visit example.", m.text)
        m.type("com"); assertEquals("visit example.com", m.text); assertFalse(m.shift)
        m.type(" and then.ok"); assertEquals("the next sentence still gets its space", "visit example.com and then. Ok", m.text)
        val g = model(); g.type("visit example.c"); g.press(Key.Backspace); g.press(Key.Backspace); g.commitGlide("com"); assertEquals("visit example.com ", g.text)
        val moved = model(); moved.type("visit example.c"); moved.press(Key.Backspace); moved.press(Key.Backspace); moved.moveCursor(-3); moved.moveCursor(3); moved.type("c"); assertEquals("moving away forgets it", "visit example. C", moved.text)
    }

    @Test fun noSpacesInsideQuotationMarks() {
        val m = model(); m.type("he said \""); m.commitGlide("this"); assertEquals("he said \"this ", m.text); m.type("\""); assertEquals("he said \"this\"", m.text)
        m.commitGlide("loudly"); assertEquals("he said \"this\" loudly ", m.text)
        val two = model(); two.type("he said \""); two.commitGlide("this"); two.commitGlide("works"); two.type("\""); assertEquals("he said \"this works\"", two.text)
        val o = model(); o.commitGlide("said"); o.type("\""); assertEquals("an opening quote after a word keeps the space before it", "said \"", o.text); o.commitGlide("hello"); o.type("\""); assertEquals("said \"hello\"", o.text)
        val own = model(); own.type("he said \"this "); own.type("\""); assertEquals("a space typed by hand stays", "he said \"this \"", own.text)
        val curly = model(); curly.type("he said “"); curly.commitGlide("this"); curly.type("”"); assertEquals("he said “this”", curly.text)
    }

    @Test fun theUndoOfAGlideAndTheStripSwapStillWork() {
        val m = model(); m.type("he said \""); m.commitGlide("this"); m.press(Key.Backspace); assertEquals("he said \"", m.text)
        val s = model(); s.type("he said "); s.commitGlide("this"); assertTrue(s.swapGlide("these")); assertEquals("he said these ", s.text)
    }

    @Test fun theSymbolsPageAndLockedCapitalsAreNotDisturbed() {
        val m = model(); m.type("hi. "); assertTrue(m.shift); m.press(Key.ToSymbols); assertFalse("the symbols page has no capitals", m.shift)
        m.press(Key.ToLetters); assertTrue("and back on the letters the sentence start asks for its capital again", m.shift); m.type("a"); assertEquals("hi. A", m.text)
        val l = model(); l.lockShift(); l.type("ab. "); assertEquals("AB. ", l.text); assertTrue(l.shiftLock)
    }

    @Test fun aPredictedNextWordGoesInAfterTheGlidedWordLikeAnyWord() {
        val m = model(); m.type("he said "); m.commitGlide("this"); assertTrue(m.insertWord("works")); assertEquals("he said this works ", m.text)
        m.press(Key.Backspace); assertEquals("it is not a glide: one Backspace takes one character", "he said this works", m.text)
        val c = model(); c.type("we left."); assertTrue(c.insertWord("then")); assertEquals("we left. Then ", c.text)
        val i = model(); i.type("well "); assertTrue(i.insertWord("i'm")); assertEquals("well I'm ", i.text)
        val off = model(false); off.type("say"); assertTrue(off.insertWord("hello")); assertEquals("say hello ", off.text)
        assertFalse(model().insertWord(""))
    }
}
