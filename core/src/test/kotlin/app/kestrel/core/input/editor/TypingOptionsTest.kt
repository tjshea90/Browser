package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** N7: auto space after punctuation, double space for a full stop, capitals after a sentence end, and the switches for them. */
class TypingOptionsTest {
    private fun rig(text: String = "", kind: FieldKind = FieldKind.TEXT, caps: Int = CapFlags.SENTENCES, autoSpace: Boolean = true): Rig { val r = Rig(text, kind = kind, caps = caps); r.c.autoSpace = autoSpace; return r }

    @Test fun aLetterRightAfterPunctuationGetsASpaceFirst() {
        val r = rig(); r.typeAll("hello,world"); assertEquals("Hello, world", r.text)
        val q = rig(); q.typeAll("what?no"); assertEquals("What? No", q.text)       // and the new sentence starts with a capital
    }

    @Test fun nothingIsAddedWhenTheSwitchIsOff() {
        val r = rig(autoSpace = false); r.typeAll("hello,world"); assertEquals("Hello,world", r.text)
    }

    /** (A bare address typed in a message, "example.com", cannot be told from "ok.thanks" by a keyboard: it gets its space; the Address fields and anything with @ or a slash are left alone, and the switch is in Settings.) */
    @Test fun addressesNumbersAndAbbreviationsAreLeftAlone() {
        for ((typed, want) in listOf("e.g" to "E.g", "u.s.a" to "U.s.a", "3.14" to "3.14", "1,000" to "1,000", "a@b.com" to "A@b.com", "10:30" to "10:30", "see/this.now" to "See/this.now")) {
            val r = rig(); r.typeAll(typed); assertEquals("typing $typed", want, r.text)
        }
        val url = rig(kind = FieldKind.URL, caps = 0); url.typeAll("a,b"); assertEquals("no spaces inside an address field", "a,b", url.text)
    }

    @Test fun capitalsFollowASentenceEndTypedByHand() {
        val r = rig(); r.typeAll("hi."); r.act { space() }; assertEquals(ShiftState.ONCE, r.c.shift)
        r.typeAll("there"); assertEquals("Hi. There", r.text)
    }

    @Test fun capitalsCanBeSwitchedOff() {
        val r = rig(); r.c.autoCapsOn = false; r.c.setShift(ShiftState.OFF); r.typeAll("hi."); r.act { space() }; assertEquals(ShiftState.OFF, r.c.shift)
        r.typeAll("there"); assertEquals("hi. there", r.text)
    }

    @Test fun backspacingToTheStartOfASentenceBringsTheCapitalBack() {
        val r = rig(caps = CapFlags.SENTENCES); r.typeAll("hi"); assertEquals(ShiftState.OFF, r.c.shift)
        r.act { backspace() }; r.act { backspace() }; assertEquals("", r.text); assertEquals(ShiftState.ONCE, r.c.shift)
    }

    @Test fun twoSpacesAfterAWordMakeAFullStop() {
        val r = rig(); r.typeAll("hello"); r.act { space() }
        assertTrue(r.c.doubleSpace()); r.sync(); r.check(); assertEquals("Hello. ", r.text); assertEquals(ShiftState.ONCE, r.c.shift)
        assertFalse("not again after a full stop", r.c.doubleSpace()); assertEquals("Hello. ", r.text)
    }

    @Test fun twoSpacesDoNothingSpecialWhereTheyAreNotAfterAWord() {
        val r = rig(); assertFalse(r.c.doubleSpace())
        val s = rig("hello, "); assertFalse(s.c.doubleSpace()); assertEquals("hello, ", s.text)
        val u = rig(kind = FieldKind.URL, caps = 0); u.typeAll("abc "); assertFalse(u.c.doubleSpace())
    }

    @Test fun theOptionsNeverBreakTheEditorBeliefs() {
        val r = rig(); val rnd = java.util.Random(5)
        val keys = listOf("a", "b", "h", "e", ".", ",", "?", " ", " ", "1", "!", "'", "w")
        repeat(1500) {
            when (rnd.nextInt(9)) { 0 -> r.act { backspace() }; 1 -> r.act { doubleSpace() }; 2 -> r.act { commitGlide("hello") }; 3 -> r.act { pickWord("world") }; else -> r.act { type(keys[rnd.nextInt(keys.size)]) } }
        }
    }
}
