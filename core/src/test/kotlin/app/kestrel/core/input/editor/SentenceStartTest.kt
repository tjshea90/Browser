package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner R19a (2026-11-18): "sometimes it doesn't capitalize new sentences. once i put a period, it should add a space and capitalize the next letter for a new sentence."
 * Owner R19b: "if i put a period then deliberately backspace to do something like a website url which ends in .com without a space, do not let the keyboard automatically add a space if i deliberately delete the space."
 */
class SentenceStartTest {
    private fun rig(text: String = "", caps: Int = 0, kind: FieldKind = FieldKind.TEXT, sentenceCaps: Boolean = true, autoSpace: Boolean = true): Rig {
        val r = Rig(text, kind = kind, caps = caps, sentenceCaps = sentenceCaps); r.c.autoSpace = autoSpace; return r
    }

    // ------------------------------------------------------------------ R19a: the capital after a sentence end, whichever way the next word arrives

    @Test fun aGlideRightAfterAFullStopGetsItsSpaceAndACapital() {
        for (end in listOf(".", "!", "?")) {
            val r = rig("we left"); r.typeAll(end); r.act { commitGlide("then") }
            assertEquals("after '$end'", "we left$end Then ", r.text)
        }
    }

    @Test fun aWordFromTheStripRightAfterAFullStopGetsItsSpaceAndACapital() {
        val r = rig("we left"); r.typeAll("."); r.act { pickWord("then") }
        assertEquals("we left. Then ", r.text)
    }

    @Test fun aGlideAfterACommaOrAnAbbreviationStaysLowercase() {
        val r = rig("we left"); r.typeAll(","); r.act { commitGlide("then") }; assertEquals("we left, then ", r.text)
        val q = rig("see e.g"); q.typeAll("."); q.act { commitGlide("this") }; assertEquals("see e.g. this ", q.text)
        val u = rig("it costs 3"); u.typeAll(".5"); u.act { commitGlide("this") }; assertEquals("no capital after a number", "it costs 3.5 this ", u.text)
    }

    @Test fun theShiftStateAtTheNextWordIsCountedForTheSpaceThatWillBeAdded() {
        val r = rig("we left."); assertTrue("a glide here starts a sentence", r.c.nextWordStartsSentence())
        assertFalse(rig("we left,").c.nextWordStartsSentence())
        assertTrue(rig("we left. ").c.nextWordStartsSentence())
        assertFalse(rig("we left").c.nextWordStartsSentence())
    }

    @Test fun aLetterTappedRightAfterAFullStopStillGetsItsSpaceAndACapital() {
        val r = rig("we left"); r.typeAll(".then"); assertEquals("we left. Then", r.text)
        val q = rig("we left", caps = CapFlags.SENTENCES); q.typeAll(".then"); assertEquals("a field that asked for capitals too", "we left. Then", q.text)
    }

    @Test fun aFieldWhoseEditorAnswersCapsModeWrongStillGetsTheCapital() {
        val r = rig("we left. ", caps = CapFlags.SENTENCES); r.ed.capsModeBroken = true
        r.c.autoCaps(); assertEquals("the keyboard's own rule covers for the editor", ShiftState.ONCE, r.c.shift)
        r.typeAll("then"); assertEquals("we left. Then", r.text)
        val q = rig("we left", caps = CapFlags.SENTENCES); q.ed.capsModeBroken = true; q.typeAll(".then"); assertEquals("we left. Then", q.text)
        val g = rig("we left", caps = CapFlags.SENTENCES); g.ed.capsModeBroken = true; g.typeAll("."); g.act { commitGlide("then") }; assertEquals("we left. Then ", g.text)
    }

    @Test fun aFlaggedFieldNeverCapitalisesWhereTheKeyboardsRuleSaysNo() {
        val r = rig("see e.g. ", caps = CapFlags.SENTENCES); r.ed.capsModeBroken = true; r.c.autoCaps(); assertEquals(ShiftState.OFF, r.c.shift)
        val e = rig("a. ", caps = CapFlags.SENTENCES, kind = FieldKind.EMAIL); e.ed.capsModeBroken = true; e.c.autoCaps(); assertEquals(ShiftState.OFF, e.c.shift)
    }

    @Test fun theWordIEndsASentenceLikeAnyOtherWord() {
        assertEquals(ShiftState.ONCE, rig("so did I. ").c.shift)
        val r = rig("so did I"); r.typeAll(".then"); assertEquals("so did I. Then", r.text)
        assertEquals("a one-letter initial is still no sentence end", ShiftState.OFF, rig("by J. ").c.shift)
        assertEquals(ShiftState.OFF, rig("see u.s. ").c.shift)
    }

    @Test fun everyWayOfStartingASentenceGivesACapital() {
        val r = rig(); r.typeAll("hi."); r.act { space() }; r.typeAll("there"); assertEquals("Hi. There", r.text)
        val d = rig(); d.typeAll("hi"); d.act { space() }; d.c.doubleSpace(); d.sync(); d.typeAll("there"); assertEquals("Hi. There", d.text)
        val n = rig(); n.typeAll("hi"); n.act { enter() }; n.typeAll("there"); assertEquals("Hi\nThere", n.text)
        val b = rig(); b.typeAll("hi. there"); repeat(5) { b.act { backspace() } }; b.typeAll("a"); assertEquals("Hi. A", b.text)
    }

    // ------------------------------------------------------------------ R19b: a space deleted on purpose stays deleted

    @Test fun aSpaceTheOwnerDeletesAfterAFullStopIsNotAddedAgain() {
        val r = rig("visit "); r.typeAll("example.c"); assertEquals("visit example. C", r.text)       // the keyboard added the space (and the capital)
        r.act { backspace() }; r.act { backspace() }; assertEquals("visit example.", r.text)           // the owner takes the letter and the space away on purpose
        r.typeAll("com"); assertEquals("visit example.com", r.text); assertEquals("and no capital either", ShiftState.OFF, r.c.shift)
        r.typeAll(" "); r.typeAll("now"); assertEquals("visit example.com now", r.text)
    }

    @Test fun theDeletedSpaceIsRememberedForThatPlaceOnlyAndTheNextSentenceGetsItsSpaceAgain() {
        val r = rig("visit "); r.typeAll("example.c"); r.act { backspace() }; r.act { backspace() }; r.typeAll("com")
        r.typeAll(" and then.ok"); assertEquals("the next sentence gets its space and its capital as usual", "visit example.com and then. Ok", r.text)
        val q = rig("visit "); q.typeAll("example.c"); q.act { backspace() }; q.act { backspace() }; q.typeAll("c"); q.act { backspace() }; q.typeAll("com"); assertEquals("letters deleted again at the same place: still tight", "visit example.com", q.text)
    }

    @Test fun aGlideAfterADeletedSpaceAlsoStaysTight() {
        val r = rig("visit "); r.typeAll("example.c"); r.act { backspace() }; r.act { backspace() }
        r.act { commitGlide("com") }; assertEquals("visit example.com ", r.text)
    }

    @Test fun aStripWordAfterADeletedSpaceAlsoStaysTight() {
        val r = rig("visit "); r.typeAll("example.c"); r.act { backspace() }; r.act { backspace() }
        r.act { pickWord("com") }; assertEquals("visit example.com ", r.text)
    }

    @Test fun aSpaceTypedAndDeletedByHandAfterACommaAlsoStaysDeleted() {
        val r = rig("hello"); r.typeAll(", "); r.act { backspace() }; assertEquals("hello,", r.text)
        r.typeAll("world"); assertEquals("hello,world", r.text)
    }

    @Test fun movingAwayAndComingBackForgetsIt() {
        val r = rig("visit "); r.typeAll("example.c"); r.act { backspace() }; r.act { backspace() }       // "visit example."
        r.ed.userMove(0); r.sync(); r.ed.userMove(14); r.sync()
        r.typeAll("c"); assertEquals("the owner left and came back: the usual space again", "visit example. C", r.text)
    }

    @Test fun deletingAnyOtherSpaceChangesNothing() {
        val r = rig("hello world"); r.typeAll(" "); r.act { backspace() }; r.typeAll("a"); assertEquals("hello worlda", r.text)
        val q = rig("hello"); q.typeAll(".w"); assertEquals("without a deletion the space still comes", "hello. W", q.text)
    }
}
