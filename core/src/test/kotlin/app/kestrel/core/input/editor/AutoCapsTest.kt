package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/** KB-104: the field's own capitalisation request decides whether Shift is on when typing starts and after Enter (per-keystroke auto-capitalisation is KB-203). */
class AutoCapsTest {
    @Test fun aSentenceFieldStartsWithShiftOnAndTheFirstLetterSpendsIt() {
        val r = Rig(caps = CapFlags.SENTENCES); assertEquals(ShiftState.ONCE, r.c.shift)
        r.typeAll("h"); assertEquals("H", r.text); assertEquals(ShiftState.OFF, r.c.shift)
    }
    @Test fun aWordsFieldStartsWithShiftOnAtTheStart() { assertEquals(ShiftState.ONCE, Rig(caps = CapFlags.WORDS).c.shift) }
    @Test fun aCharactersFieldAlwaysAsksForCapitals() { assertEquals(ShiftState.ONCE, Rig("abc", caps = CapFlags.CHARACTERS).c.shift) }
    @Test fun aFieldThatAskedForNothingAndIsNotATextBoxStartsLowercase() { assertEquals(ShiftState.OFF, Rig(kind = FieldKind.SEARCH, sentenceCaps = true).c.shift) }
    @Test fun inTheMiddleOfASentenceShiftStaysOff() { assertEquals(ShiftState.OFF, Rig("hello", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun afterAFullStopAndASpaceShiftIsOn() { assertEquals(ShiftState.ONCE, Rig("Hello. ", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun afterANewlineShiftIsOn() { assertEquals(ShiftState.ONCE, Rig("one\n", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun enterStartsANewParagraphSoShiftComesBack() {
        val r = Rig(caps = CapFlags.SENTENCES).typeAll("hi"); assertEquals(ShiftState.OFF, r.c.shift)
        r.act { enter() }; assertEquals("Hi\n", r.text); assertEquals(ShiftState.ONCE, r.c.shift)
    }
    @Test fun capsLockIsNeverSwitchedOffByTheField() { val r = Rig(caps = CapFlags.SENTENCES); r.act { lockShift() }; r.act { autoCaps() }; assertEquals(ShiftState.LOCK, r.c.shift) }
    @Test fun aBoxThatAskedForNothingStillCapitalisesAfterASentenceEnd() {      // owner 2026-11-17: by default
        assertEquals(ShiftState.ONCE, Rig("Hello there. ", sentenceCaps = true).c.shift)
        assertEquals(ShiftState.ONCE, Rig("Really? ", sentenceCaps = true).c.shift)
        assertEquals(ShiftState.ONCE, Rig("Stop! ", sentenceCaps = true).c.shift)
        assertEquals(ShiftState.OFF, Rig("Hello there ", sentenceCaps = true).c.shift)
        assertEquals(ShiftState.OFF, Rig("Hello there.", sentenceCaps = true).c.shift)           // no space yet: "3.5", "e.g." and "www.x" stay lowercase
        assertEquals("an empty text box starts with a capital too (owner R18b)", ShiftState.ONCE, Rig("", sentenceCaps = true).c.shift)
    }
    @Test fun aTextBoxThatAskedForNothingStartsAndEndsSentencesWithCapitals() {            // owner 2026-11-18: "auto capitalize words at the beginning of sentences"
        assertEquals(ShiftState.ONCE, Rig("", sentenceCaps = true).c.shift); assertEquals(ShiftState.ONCE, Rig("hello\n", sentenceCaps = true).c.shift); assertEquals(ShiftState.OFF, Rig("hello", sentenceCaps = true).c.shift)
        assertEquals("through a closing quote", ShiftState.ONCE, Rig("He said \"stop.\" ", sentenceCaps = true).c.shift); assertEquals("through a bracket", ShiftState.ONCE, Rig("I left (it was late.) ", sentenceCaps = true).c.shift)
        assertEquals(ShiftState.ONCE, Rig("What… ", sentenceCaps = true).c.shift); assertEquals(ShiftState.ONCE, Rig("Why?! ", sentenceCaps = true).c.shift); assertEquals(ShiftState.ONCE, Rig("I said no. ", sentenceCaps = true).c.shift); assertEquals(ShiftState.ONCE, Rig("It costs 5. ", sentenceCaps = true).c.shift)
        for (t in listOf("see e.g. ", "that is, i.e. ", "Mr. ", "dr. ", "J. ", "the u.s. ", "vs. ", "approx. ")) assertEquals("not after '$t'", ShiftState.OFF, Rig(t, sentenceCaps = true).c.shift)
        for (k in listOf(FieldKind.SEARCH, FieldKind.OMNIBOX, FieldKind.EMAIL, FieldKind.URL, FieldKind.PASSWORD, FieldKind.NUMBER, FieldKind.PHONE)) assertEquals("never in $k", ShiftState.OFF, Rig("", kind = k, sentenceCaps = true).c.shift)
    }
    @Test fun startsSentenceIsTheOneRule() {
        for (t in listOf("", "   ", "Hello. ", "Hello.  ", "Hello!\t", "Hello?\n", "a\nb\n", "\"Hi.\" ", "Done.) ")) assertEquals("'$t'", true, InputController.startsSentence(t, if (t.isEmpty()) 0 else -1))
        for (t in listOf("Hello", "Hello.", "Hello, ", "3.5", "www.x", "Hello:\u00A0".trimEnd(), "e.g. ", "x".repeat(70) + " ")) assertEquals("'$t'", false, InputController.startsSentence(t, 99))
        assertEquals("text exists before the window: unknown", false, InputController.startsSentence("", 40))
    }
    @Test fun movingTheCursorToTheStartOfASentenceTurnsShiftOnAndAwayTurnsItOff() {
        val r = Rig("Hello there. how are you", sentenceCaps = true); assertEquals(ShiftState.OFF, r.c.shift)
        r.ed.userMove(13); r.sync(); assertEquals("the cursor stands where a sentence starts (after the full stop and the space)", ShiftState.ONCE, r.c.shift)
        r.ed.userMove(18); r.sync(); assertEquals("and in the middle of the sentence it does not", ShiftState.OFF, r.c.shift)
    }
    @Test fun typingAFullStopAndASpaceTurnsShiftOnAndALetterSpendsIt() {
        val r = Rig("", sentenceCaps = true).typeAll("hi.")
        assertEquals("Hi.", r.text); assertEquals(ShiftState.OFF, r.c.shift); r.typeAll(" "); assertEquals(ShiftState.ONCE, r.c.shift)
        r.typeAll("a"); assertEquals("Hi. A", r.text); assertEquals(ShiftState.OFF, r.c.shift)
    }
    @Test fun theSettingTurnsItOffAndEmailUrlAndPasswordFieldsNeverAsk() {
        val off = Rig("Hi. ", kind = FieldKind.TEXT, sentenceCaps = true); off.c.autoCapsOn = false; off.act { setShift(ShiftState.OFF) }; off.c.autoCaps(); assertEquals(ShiftState.OFF, off.c.shift)
        for (k in listOf(FieldKind.EMAIL, FieldKind.URL, FieldKind.PASSWORD, FieldKind.NUMBER)) assertEquals(k.name, ShiftState.OFF, Rig("Hi. ", kind = k).c.shift)
    }
    @Test fun aDeadEditorMeansNoCapitals() { val r = Rig(caps = CapFlags.SENTENCES); r.act { setShift(ShiftState.OFF) }; r.ed.failing = true; r.c.autoCaps(); assertEquals(ShiftState.OFF, r.c.shift) }
}

class InputStatsTest {
    @Test fun countersCountPressesAndRepairsAndHoldNoText() {
        val r = Rig("abc"); val s = r.c.stats
        r.typeAll("de"); r.act { backspace() }; r.act { commitGlide("hello") }; r.c.swapGlide("world"); r.sync(); r.act { enter() }; r.act { moveCursor(-1) }
        assertEquals(r.firstForeignInfo, "keys 2, backspaces 1, enters 1, glides 1, picks 1, cursor moves 1, foreign changes 0, resyncs 0", s.line())
        r.ed.userMove(0); r.sync(); assertEquals(1, s.foreignReports)
        r.ed.failing = true; r.c.type("x"); assertEquals(1, s.resyncs)
        val line = s.line(); assertEquals(false, line.contains("hello") || line.contains("abc") || line.contains("world"))
    }
}
