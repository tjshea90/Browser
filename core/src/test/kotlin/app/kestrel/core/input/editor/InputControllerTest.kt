package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputControllerTest {
    // ------------------------------------------------------------------ tapped words (composing)
    @Test fun lettersAreComposingTextSoTheAppCanUnderlineTheWord() {
        val r = Rig().typeAll("hel")
        assertEquals("hel", r.text); assertEquals("hel", r.c.composing); assertEquals(0, r.ed.compS); assertEquals(3, r.ed.compE)
    }
    @Test fun spaceEndsTheWordAndAddsASpace() {
        val r = Rig().typeAll("hello"); r.act { space() }
        assertEquals("hello ", r.text); assertNull(r.c.composing); assertEquals(-1, r.ed.compS)
    }
    @Test fun punctuationEndsTheWordAndStandsAfterIt() {
        val r = Rig().typeAll("hello,"); assertEquals("hello,", r.text); assertNull(r.c.composing)
    }
    @Test fun anApostropheInsideAWordStaysInTheWord() { val r = Rig().typeAll("don't"); assertEquals("don't", r.c.composing) }
    @Test fun aLeadingApostropheIsPlainText() { val r = Rig().typeAll("'a"); assertEquals("'a", r.text); assertEquals("a", r.c.composing) }
    @Test fun digitsAfterLettersStayInTheWord_aLeadingDigitIsPlain() {
        assertEquals("b4", Rig().typeAll("b4").c.composing)
        val r = Rig().typeAll("42"); assertNull(r.c.composing); assertEquals("42", r.text)
    }
    @Test fun shiftOnceCapitalisesOneLetter() {
        val r = Rig(); r.act { tapShift() }.typeAll("hi")
        assertEquals("Hi", r.text); assertEquals(ShiftState.OFF, r.c.shift)
    }
    @Test fun shiftLockKeepsCapitals() {
        val r = Rig(); r.act { lockShift() }.typeAll("ab"); assertEquals("AB", r.text); assertEquals(ShiftState.LOCK, r.c.shift)
        r.act { tapShift() }.typeAll("c"); assertEquals("ABc", r.text)
    }
    @Test fun aPasswordFieldNeverUsesComposingText() {
        val r = Rig(kind = FieldKind.PASSWORD).typeAll("pw1"); assertEquals("pw1", r.text); assertNull(r.c.composing); assertEquals(-1, r.ed.compS)
        assertFalse(r.c.field.learningAllowed)
    }
    @Test fun noSuggestionsFieldsAndNumberFieldsTypeDirectly() {
        assertNull(Rig(noSuggestions = true).typeAll("ab").c.composing)
        assertNull(Rig(kind = FieldKind.NUMBER).typeAll("12").c.composing)
        assertNull(Rig(kind = FieldKind.URL).typeAll("ab").c.composing)
    }
    @Test fun typingReplacesASelection() {
        val r = Rig("hello world", 0); r.ed.userSelect(6, 11); r.sync(); r.c.onSelectionUpdate(6, 11)
        r.act { type("x") }
        assertEquals("hello x", r.text)
    }
    @Test fun anEmojiIsOneCharacterToTheBackspaceKey() {
        val r = Rig(); r.act { type("😀") }; assertEquals("😀", r.text)
        r.act { backspace() }; assertEquals("", r.text)
    }
    @Test fun learningFlagsFollowTheField() {
        assertTrue(FieldInfo().learningAllowed)
        assertFalse(FieldInfo(noPersonalizedLearning = true).learningAllowed)
        assertFalse(FieldInfo(kind = FieldKind.PASSWORD).learningAllowed)
    }

    // ------------------------------------------------------------------ Backspace
    @Test fun backspaceShortensTheTappedWordAndAtTheLastLetterRemovesIt() {
        val r = Rig().typeAll("ab"); r.act { backspace() }; assertEquals("a", r.text); assertEquals("a", r.c.composing)
        r.act { backspace() }; assertEquals("", r.text); assertNull(r.c.composing)
    }
    @Test fun backspaceOnPlainTextDeletesOneCharacter() {
        val r = Rig("abc"); r.act { backspace() }; assertEquals("ab", r.text)
    }
    @Test fun backspaceWithASelectionDeletesTheSelection() {
        val r = Rig("hello world", 11); r.ed.userSelect(0, 5); r.sync()
        r.act { backspace() }; assertEquals(" world", r.text)
    }
    @Test fun backspaceAtTheStartOfTheFieldChangesNothing() { val r = Rig("x", 0); r.act { backspace() }; assertEquals("x", r.text) }
    @Test fun holdingBackspaceThroughALongTextKeepsTheWindowFilled() {
        val r = Rig("0123456789".repeat(20)); repeat(150) { r.act { backspace() } }
        assertEquals(50, r.text.length); assertTrue(r.c.before.length >= InputController.MIN_BEFORE)
    }

    // ------------------------------------------------------------------ glide
    @Test fun aGlideInAnEmptyFieldIsTheWordAndASpace() { val r = Rig(); r.act { commitGlide("hello") }; assertEquals("hello ", r.text) }
    @Test fun aGlideAfterTextAddsTheSpaceBeforeIt() { val r = Rig("hi"); r.act { commitGlide("there") }; assertEquals("hi there ", r.text) }
    @Test fun aGlideAfterASpaceAddsNoSecondSpace() { val r = Rig("hi "); r.act { commitGlide("there") }; assertEquals("hi there ", r.text) }
    @Test fun aGlideAfterAnOpeningBracketOrNewlineHasNoLeadingSpace() {
        assertEquals("(word ", Rig("(").also { it.act { commitGlide("word") } }.text)
        assertEquals("a\nword ", Rig("a\n").also { it.act { commitGlide("word") } }.text)
    }
    @Test fun shiftOnceCapitalisesAGlideAndShiftLockUppercasesIt() {
        val r = Rig(); r.act { tapShift(); commitGlide("hello") }; assertEquals("Hello ", r.text); assertEquals(ShiftState.OFF, r.c.shift)
        val s = Rig(); s.act { lockShift(); commitGlide("hello") }; assertEquals("HELLO ", s.text); assertEquals(ShiftState.LOCK, s.c.shift)
    }
    @Test fun oneBackspaceRemovesTheWholeGlideIncludingItsLeadingSpace() {
        val r = Rig("hi"); r.act { commitGlide("there") }; r.act { backspace() }; assertEquals("hi", r.text); assertNull(r.c.lastGlide)
    }
    @Test fun theSecondBackspaceAfterAGlideDeletesOneCharacter() {
        val r = Rig("hi"); r.act { commitGlide("there") }; r.act { backspace() }; r.act { backspace() }; assertEquals("h", r.text)
    }
    @Test fun aCandidateFromTheStripReplacesTheGlidedWord() {
        val r = Rig("hi"); r.act { commitGlide("for") }; assertTrue(r.c.swapGlide("fir")); r.sync(); r.check()
        assertEquals("hi fir ", r.text); assertEquals("fir", r.c.lastGlide?.word)
        assertTrue(r.c.swapGlide("fur")); r.sync(); assertEquals("hi fur ", r.text)
    }
    @Test fun aSwapKeepsCapitalisationAndAllCaps() {
        val r = Rig(); r.act { tapShift(); commitGlide("for") }; r.c.swapGlide("fir"); r.sync(); assertEquals("Fir ", r.text)
        val s = Rig(); s.act { lockShift(); commitGlide("for") }; s.c.swapGlide("fir"); s.sync(); assertEquals("FIR ", s.text)
    }
    @Test fun aSwapIsRefusedWhenTheAppChangedTheText() {
        val r = Rig(); r.act { commitGlide("for") }; r.ed.userInsert("!"); r.sync()
        assertFalse(r.c.swapGlide("fir")); assertEquals("for !", r.text)
    }
    @Test fun aSwapIsRefusedWhenTheCursorWentElsewhere() {
        val r = Rig("a"); r.act { commitGlide("for") }; r.ed.userMove(0); r.sync()
        assertFalse(r.c.swapGlide("fir")); assertEquals("a for ", r.text)
    }
    @Test fun aSwapOnAnEditorThatFailsLeavesNoStaleBelief() {
        val r = Rig(); r.act { commitGlide("for") }; r.ed.failing = true
        assertFalse(r.c.swapGlide("fir")); assertNull(r.c.lastGlide); r.ed.failing = false; r.c.backspace(); r.sync(); assertEquals("for", r.text)
    }
    @Test fun aFullStopAfterAGlideClosesTheWordInsteadOfStandingAfterTheSpace() {
        val r = Rig(); r.act { commitGlide("hello") }; r.act { type(".") }; assertEquals("hello.", r.text)
    }
    @Test fun dotComAfterAGlideJoinsTheWord() {
        val r = Rig(); r.act { commitGlide("doraby") }; r.act { type(".com") }; assertEquals("doraby.com", r.text)
    }
    @Test fun aLetterAfterAGlideKeepsTheSpace() {
        val r = Rig(); r.act { commitGlide("hello") }; r.typeAll("w"); assertEquals("hello w", r.text); assertNull(r.c.lastGlide)
    }
    @Test fun aGlideAfterATappedWordFinishesTheWordFirst() {
        val r = Rig().typeAll("hel"); r.act { commitGlide("world") }; assertEquals("hel world ", r.text); assertNull(r.c.composing)
    }
    @Test fun twoGlidesInARow() {
        val r = Rig(); r.act { commitGlide("good") }; r.act { commitGlide("morning") }; assertEquals("good morning ", r.text)
        r.act { backspace() }; assertEquals("good ", r.text)
    }

    // ------------------------------------------------------------------ cursor
    @Test fun movingTheCursorLeftMakesTypingGoThere() {
        val r = Rig("hello world"); r.act { moveCursor(-5) }; r.act { type("X") }; assertEquals("hello Xworld", r.text)
    }
    @Test fun movingRightAtTheEndOfTheTextChangesNothing() { val r = Rig("ab"); r.act { moveCursor(3) }; r.act { type("c") }; assertEquals("abc", r.text) }
    @Test fun movingOverAnEmojiMovesTwoUnits() {
        val r = Rig("a😀b"); r.act { moveCursor(-1) }; assertEquals(3, r.ed.selS); r.act { moveCursor(-1) }; assertEquals(1, r.ed.selS)
    }
    @Test fun theCursorCannotLeaveTheField() { val r = Rig("ab"); r.act { moveCursor(-50) }; assertEquals(0, r.ed.selS); r.act { type("x") }; assertEquals("xab", r.text) }
    @Test fun movingTheCursorFinishesTheTappedWord() {
        val r = Rig().typeAll("hel"); r.act { moveCursor(-1) }; assertNull(r.c.composing); assertEquals(-1, r.ed.compS)
        r.act { type("X") }; assertEquals("heXl", r.text)
    }
    @Test fun movingFarKeepsTheBeliefsTrue_andOneCallMovesAtMost64() {
        val r = Rig("x".repeat(300), 150); r.act { moveCursor(-40) }; assertEquals(110, r.ed.selS); r.act { moveCursor(70) }; assertEquals(174, r.ed.selS)
    }

    // ------------------------------------------------------------------ Enter
    @Test fun enterRunsTheFieldsAction() {
        val r = Rig(action = ImeActions.GO).typeAll("abc"); r.act { enter() }
        assertEquals(listOf(ImeActions.GO), r.ed.actions); assertEquals("abc", r.text); assertNull(r.c.composing)
    }
    @Test fun enterWithNoEnterActionFlagInsertsANewline() {
        val r = Rig(action = ImeActions.SEND, noEnter = true); r.act { enter() }; assertTrue(r.ed.actions.isEmpty()); assertEquals("\n", r.text)
    }
    @Test fun enterInAMultiLineFieldWithoutActionInsertsANewline() { val r = Rig("a"); r.act { enter() }; assertEquals("a\n", r.text) }
    @Test fun enterInASingleLineFieldWithoutActionDoesNotInventText() { val r = Rig("a", multiline = false); r.act { enter() }; assertEquals("a", r.text) }
    @Test fun typingContinuesNormallyAfterEnter() { val r = Rig(action = ImeActions.NONE).typeAll("ab"); r.act { enter() }; r.typeAll("c"); assertEquals("ab\nc", r.text) }

    // ------------------------------------------------------------------ selection reports: ours, late, merged, foreign
    @Test fun reportsOfOwnEditsAreRecognisedEvenWhenTheyArriveLate() {
        val r = Rig(); for (ch in "hello") r.c.type(ch.toString())          // five edits, no report delivered yet
        assertEquals(5, r.ed.updates.size); r.sync(); r.check(); assertEquals(0, r.foreign)
    }
    @Test fun mergedReportsAreRecognisedToo() {
        val r = Rig(); for (ch in "hello") r.c.type(ch.toString())
        val last = r.ed.updates.last(); r.ed.updates.clear()
        assertTrue(r.c.onSelectionUpdate(last[0], last[1], last[2], last[3])); r.check()
    }
    @Test fun aReportThatOnlyChangesTheComposingRegionIsIgnored() {
        val r = Rig().typeAll("ab"); assertTrue(r.c.onSelectionUpdate(2, 2, -1, -1)); assertEquals("ab", r.c.composing)
    }
    @Test fun theUserMovingTheCursorDropsComposingAndGlide() {
        val r = Rig().typeAll("hel"); r.ed.userMove(0); r.sync()
        assertNull(r.c.composing); assertEquals(1, r.foreign); r.check()
        val g = Rig(); g.act { commitGlide("hello") }; g.ed.userMove(2); g.sync(); assertNull(g.c.lastGlide)
        g.act { backspace() }; assertEquals("hllo ", g.text)                 // one character, not the glided word
    }
    @Test fun theAppInsertingTextBeforeTheCursorIsPickedUp() {
        val r = Rig("abc"); r.ed.userInsert("XYZ"); r.sync(); assertEquals("abcXYZ", r.c.before); r.act { backspace() }; assertEquals("abcXY", r.text)
    }
    @Test fun theAppReplacingAllTheTextIsPickedUp() { val r = Rig("old"); r.ed.appSetText("brand new"); r.sync(); assertEquals("brand new", r.c.before); r.check() }
    @Test fun startInputForgetsTheOldField() {
        val r = Rig().typeAll("hel"); r.act { tapShift() }
        r.c.startInput(FieldInfo(kind = FieldKind.PASSWORD)); assertNull(r.c.composing); assertEquals(ShiftState.OFF, r.c.shift); assertFalse(r.c.field.learningAllowed)
    }
    @Test fun anUnknownSelectionIsAdoptedFromTheFirstReport() {
        val ed = FakeEditor("abc"); val c = InputController(ed); c.startInput(FieldInfo(initialSelStart = -1, initialSelEnd = -1))
        assertEquals("abc", c.before); assertTrue(c.onSelectionUpdate(3, 3)); assertEquals(3, c.selStart)
    }

    @Test fun aGlideRightAfterATappedWordSurvivesTheLateReportOfEndingTheWord() {
        val r = Rig().typeAll("hel"); r.c.commitGlide("world")           // no report delivered in between: the report of ending the word arrives after the glide
        r.sync(); r.check(); assertEquals(0, r.foreign); assertTrue("the glide must still be the last thing, so a strip pick works", r.c.swapGlide("word"))
        r.sync(); assertEquals("hel word ", r.text)
    }
    @Test fun typingAfterATappedWordThenASpaceNeverLooksForeign() {
        val r = Rig(); for (ch in "hello world, ok") r.c.type(ch.toString()); r.sync(); r.check(); assertEquals(0, r.foreign)
    }

    // ------------------------------------------------------------------ a dead editor
    @Test fun anEditorThatFailsNeverCrashesTheKeyboardAndItRecovers() {
        val r = Rig().typeAll("ab"); r.ed.failing = true
        r.c.type("c"); r.c.backspace(); r.c.commitGlide("x"); r.c.enter(); r.c.moveCursor(-1); r.c.space()
        r.ed.failing = false; r.ed.updates.clear(); r.c.startInput(FieldInfo(initialSelStart = r.ed.selS, initialSelEnd = r.ed.selE)); r.check()
        r.typeAll("z"); assertTrue(r.text.endsWith("z"))
    }

    // ------------------------------------------------------------------ the language's context
    @Test fun thePreviousWordIsPlainOrNull() {
        assertEquals("hello", Rig("say hello ").c.previousWord())
        assertNull(Rig("call 555-1234 ").c.previousWord()); assertNull(Rig("").c.previousWord())
    }
}
