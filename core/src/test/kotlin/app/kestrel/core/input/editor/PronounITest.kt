package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner R20c (2026-11-18): "i just began this sentence ... and it didn't auto capitalize the word I. I also wrote i'm and it didn't capitalize the i in that word either."
 * The pronoun was only fixed on the tapped-letters path of boxes that allow composing text; a glided i'm (the decoder gives the lowercase list word), a strip word, and every box that asks for no suggestions got nothing.
 */
class PronounITest {
    private fun rig(text: String = "", noSuggestions: Boolean = false, kind: FieldKind = FieldKind.TEXT, on: Boolean = true): Rig {
        val r = Rig(text, kind = kind, noSuggestions = noSuggestions); r.c.capitalizeI = on; return r
    }

    @Test fun aLoneIBecomesACapitalInABoxThatAsksForNoSuggestions() {
        val r = rig("3) ", noSuggestions = true); r.typeAll("i just began"); assertEquals("3) I just began", r.text)
        val q = rig("so ", noSuggestions = true); q.typeAll("do i, or i. then i? yes i!"); assertEquals("so do I, or I. then I? yes I!", q.text)
    }

    @Test fun theContractionsAreFixedToo() {
        val r = rig("well ", noSuggestions = true); r.typeAll("i'm sure i'll go and i've seen it, i'd say"); assertEquals("well I'm sure I'll go and I've seen it, I'd say", r.text)
    }

    @Test fun aGlidedContractionAndAStripWordGetTheCapitalToo() {
        for (w in listOf("i'm", "i'll", "i've", "i'd")) { val r = rig("well "); r.act { commitGlide(w) }; assertEquals("well ${"I" + w.substring(1)} ", r.text) }
        val p = rig("well "); p.act { pickWord("i") }; assertEquals("well I ", p.text)
        val s = rig("well "); s.act { commitGlide("in") }; s.c.swapGlide("i'm"); s.sync(); assertEquals("a strip word that replaces the glide", "well I'm ", s.text)
    }

    @Test fun wordsThatMerelyStartWithIAreLeftAlone() {
        val r = rig("", noSuggestions = true); r.typeAll("in it is hi if id"); assertEquals("in it is hi if id", r.text)
        val q = rig("well "); q.act { commitGlide("in") }; assertEquals("well in ", q.text)
    }

    @Test fun aWordThatHasAnIInsideItIsNotTouched() {
        val r = rig("", noSuggestions = true); r.typeAll("wi-fi, a.i. and x-i "); assertEquals("wi-fi, a.i. and x-i ", r.text)
    }

    @Test fun itIsOffWhenTheSettingIsOffAndInBoxesWhereAWordIsNotAWord() {
        val off = rig("", noSuggestions = true, on = false); off.typeAll("i am "); assertEquals("i am ", off.text)
        val offGlide = rig("well ", on = false); offGlide.act { commitGlide("i'm") }; assertEquals("well i'm ", offGlide.text)
        for (k in listOf(FieldKind.EMAIL, FieldKind.URL, FieldKind.PASSWORD, FieldKind.NUMBER)) { val r = rig("", kind = k); r.typeAll("i am"); assertEquals("never in $k", "i am", r.text) }
    }

    @Test fun enterAlsoEndsTheWord() {
        val r = rig("so ", noSuggestions = true); r.typeAll("i"); r.act { enter() }; assertEquals("so I\n", r.text)
    }

    @Test fun backspaceRightAfterTheFixedWordStillEditsNormally() {
        val r = rig("so ", noSuggestions = true); r.typeAll("i "); r.act { backspace() }; r.act { backspace() }; assertEquals("so ", r.text)
    }

    @Test fun theRuleIsOneFunction() {
        for (w in listOf("i", "i'm", "i'll", "i've", "i'd", "i’m")) assertTrue(w, InputController.isPronounI(w))
        for (w in listOf("I", "in", "it", "i'z", "im", "i'mm", "", "ii", "id")) assertFalse(w, InputController.isPronounI(w))
        assertEquals("I’m", InputController.pronounI("i’m")); assertEquals("in", InputController.pronounI("in"))
    }
}
