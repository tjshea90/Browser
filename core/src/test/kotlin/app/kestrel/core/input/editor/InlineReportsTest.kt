package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An editor in the keyboard's OWN process (the Setup screen's try-it field, the Dictionary screen's field) runs the keyboard's calls INLINE on Android 10 and older: its selection report arrives INSIDE the edit
 * call, before the keyboard has noted the edit. The keyboard used to take that report for somebody else's change, re-read the text and then count its own edit a second time: the Android 10 emulator typed "H?a"
 * where Android 16 typed "H? A" (TASKS Q-SMARTSPACE; the trace showed the text window one character too long after every key). [FakeEditor.inlineSink] delivers its reports that way.
 */
class InlineReportsTest {
    private fun rig(text: String = "", caps: Int = CapFlags.SENTENCES): Rig = Rig(text, caps = caps, inlineReports = true).also { it.c.autoSpace = true }

    @Test fun theReportOfAnEditThatComesInsideTheCallIsTheEditsOwnAndNotSomebodyElses() {
        val r = rig(); r.typeAll("hello")
        assertEquals("no report was taken for somebody else's change", 0, r.foreign)
        assertEquals("Hello", r.text); assertEquals("Hello", r.c.before); assertEquals(5, r.c.selStart); assertEquals(5, r.c.selEnd)
    }

    @Test fun theEmulatorSequenceHiBackspaceQuestionMarkAGivesTheSpaceAndTheCapital() {
        val r = rig(); r.typeAll("hi"); r.act { backspace() }; r.typeAll("?a")
        assertEquals("H? A", r.text); assertEquals("the keyboard's picture of the text is the text", r.text, r.c.before)
        assertEquals(0, r.foreign)
        r.typeAll("."); r.act { space() }; r.typeAll("b")
        assertEquals("H? A. B", r.text); assertEquals(r.text, r.c.before)
    }

    @Test fun glideSwapAndBackspaceKeepTheBeliefsTrue() {
        val r = rig(caps = 0)
        r.act { commitGlide("hello") }; assertEquals("hello ", r.text); assertEquals("hello ", r.c.before); assertNotNull(r.c.lastGlide)
        assertTrue(r.c.swapGlide("world")); r.sync(); r.check(); assertEquals("world ", r.text); assertEquals("world ", r.c.before)
        r.act { backspace() }; assertEquals("", r.text); assertEquals("", r.c.before)
        assertEquals(0, r.foreign)
    }

    @Test fun aWordFromTheStripReplacesTheWordBeingTappedAndTheSpaceKeysStillWork() {
        val r = rig(caps = 0); r.typeAll("wor")
        assertTrue(r.c.pickWord("world")); r.sync(); r.check()
        assertEquals("world ", r.text); assertEquals("world ", r.c.before)
        r.typeAll("hi"); r.act { space() }; assertTrue(r.c.doubleSpace()); r.sync(); r.check()
        assertEquals("world hi. ", r.text); assertEquals(0, r.foreign)
    }

    @Test fun aTypoFixAndItsBackspaceAreNotConfusedByReportsInsideTheCalls() {
        val r = rig(caps = 0); r.typeAll("teh"); r.act { typeCorrected("the", " ") }
        assertEquals("the ", r.text); assertEquals("the ", r.c.before)
        r.act { backspace() }; assertEquals("teh", r.text); assertEquals("teh", r.c.composing)
        assertEquals(0, r.foreign)
    }

    @Test fun theCursorKeysAndEnterAreFollowedWhenTheReportsComeInsideTheCall() {
        val r = rig(caps = 0); r.typeAll("abc"); r.act { moveCursor(-2) }
        assertEquals(1, r.c.selStart); assertEquals("a", r.c.before); assertEquals("bc", r.c.after)
        r.act { moveCursor(1) }; assertEquals(2, r.c.selStart); r.typeAll("X"); assertEquals("abXc", r.text); assertEquals("abX", r.c.before)
        r.act { enter() }; r.typeAll("y"); assertEquals("abX\nyc", r.text); assertEquals("abX\ny", r.c.before)
    }

    @Test fun noEntryIsLeftWaitingForAReportThatAnInlineEditorNeverSends() {
        val r = rig("line one", caps = 0)                    // the cursor stands at the end
        r.ed.userMove(0); r.sync()                           // the person taps the start of the text
        r.act { backspace() }                                // nothing before the cursor: nothing changes and nothing is reported
        r.typeAll("ab"); r.ed.userMove(0); r.sync(); r.check()   // the person taps the start again: that is NOT the keyboard's own edit
        assertEquals("", r.c.before); assertEquals(0, r.c.selStart)
    }

    @Test fun aChangeNoEditExplainsInsideACallIsNoticedAtTheNextKey() {
        // a text watcher in the app appends "!" the moment "c" has been typed and the editor reports both states inside the call: the keyboard explains the first with its own edit, and the second one repairs its picture at the next key
        val r = rig(caps = 0); r.typeAll("ab")
        val sink = r.ed.inlineSink!!; var once = true
        r.ed.inlineSink = { u -> sink(u); if (once && r.text.endsWith("c")) { once = false; r.ed.userInsert("!") } }
        r.c.type("c"); r.c.type("d"); r.sync(); r.check()
        assertEquals("abc! D", r.text); assertEquals(r.text, r.c.before)        // (and the space and the capital after the "!" that the repaired picture shows)
    }
}
