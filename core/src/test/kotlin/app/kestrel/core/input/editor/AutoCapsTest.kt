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
    @Test fun aFieldThatAskedForNothingStartsLowercase() { assertEquals(ShiftState.OFF, Rig().c.shift) }
    @Test fun inTheMiddleOfASentenceShiftStaysOff() { assertEquals(ShiftState.OFF, Rig("hello", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun afterAFullStopAndASpaceShiftIsOn() { assertEquals(ShiftState.ONCE, Rig("Hello. ", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun afterANewlineShiftIsOn() { assertEquals(ShiftState.ONCE, Rig("one\n", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun enterStartsANewParagraphSoShiftComesBack() {
        val r = Rig(caps = CapFlags.SENTENCES).typeAll("hi"); assertEquals(ShiftState.OFF, r.c.shift)
        r.act { enter() }; assertEquals("Hi\n", r.text); assertEquals(ShiftState.ONCE, r.c.shift)
    }
    @Test fun capsLockIsNeverSwitchedOffByTheField() { val r = Rig(caps = CapFlags.SENTENCES); r.act { lockShift() }; r.act { autoCaps() }; assertEquals(ShiftState.LOCK, r.c.shift) }
    @Test fun aBoxThatAskedForNothingStillCapitalisesAfterASentenceEnd() {      // owner 2026-11-17: by default
        assertEquals(ShiftState.ONCE, Rig("Hello there. ").c.shift)
        assertEquals(ShiftState.ONCE, Rig("Really? ").c.shift)
        assertEquals(ShiftState.ONCE, Rig("Stop! ").c.shift)
        assertEquals(ShiftState.OFF, Rig("Hello there ").c.shift)
        assertEquals(ShiftState.OFF, Rig("Hello there.").c.shift)           // no space yet: "3.5", "e.g." and "www.x" stay lowercase
        assertEquals(ShiftState.OFF, Rig("").c.shift)                      // an empty box did not ask
    }
    @Test fun typingAFullStopAndASpaceTurnsShiftOnAndALetterSpendsIt() {
        val r = Rig("").typeAll("hi.")
        assertEquals(ShiftState.OFF, r.c.shift); r.typeAll(" "); assertEquals(ShiftState.ONCE, r.c.shift)
        r.typeAll("a"); assertEquals("hi. A", r.text); assertEquals(ShiftState.OFF, r.c.shift)
    }
    @Test fun theSettingTurnsItOffAndEmailUrlAndPasswordFieldsNeverAsk() {
        val off = Rig("Hi. ", kind = FieldKind.TEXT); off.c.autoCapsOn = false; off.act { setShift(ShiftState.OFF) }; off.c.autoCaps(); assertEquals(ShiftState.OFF, off.c.shift)
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
