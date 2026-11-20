package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Owner R21 (2026-11-18): "The keyboard is automatically putting spaces between a word inside quotation marks, like " this ". I don't like this. It shouldn't put spaces there."
 * A glide or a strip word brings a space before it (after a word or a closing mark) and one after it; neither belongs inside a pair of quotes.
 */
class QuotesTest {
    private fun rig(text: String = "") = Rig(text)

    @Test fun aGlideRightAfterAnOpeningQuoteHasNoSpaceBetweenThem() {
        val r = rig("he said "); r.typeAll("\""); r.act { commitGlide("this") }; assertEquals("he said \"this ", r.text)
        val s = rig(); s.typeAll("\""); s.act { commitGlide("this") }; assertEquals("a quote that opens the text", "\"this ", s.text)
        val p = rig("(see \""); p.act { commitGlide("this") }; assertEquals("(see \"this ", p.text)
    }

    @Test fun aClosingQuoteTakesAwayTheSpaceTheGlideAdded() {
        val r = rig("he said "); r.typeAll("\""); r.act { commitGlide("this") }; r.typeAll("\"")
        assertEquals("he said \"this\"", r.text)
    }

    @Test fun aWordFromTheStripBehavesTheSame() {
        val r = rig("he said \""); r.act { pickWord("this") }; assertEquals("he said \"this ", r.text); r.typeAll("\""); assertEquals("he said \"this\"", r.text)
    }

    @Test fun twoWordsInsideTheQuotesKeepTheSpaceBetweenThemButNotTheEnds() {
        val r = rig("he said "); r.typeAll("\""); r.act { commitGlide("this") }; r.act { commitGlide("works") }; r.typeAll("\"")
        assertEquals("he said \"this works\"", r.text)
    }

    @Test fun aWordAfterTheClosingQuoteGetsItsSpaceAgainAndAnOpeningQuoteAfterAWordKeepsIt() {
        val r = rig("he said "); r.typeAll("\""); r.act { commitGlide("this") }; r.typeAll("\""); r.act { commitGlide("loudly") }
        assertEquals("he said \"this\" loudly ", r.text)
        val o = rig(); o.act { commitGlide("said") }; o.typeAll("\""); assertEquals("an opening quote after a word keeps the space before it", "said \"", o.text)
        o.act { commitGlide("hello") }; o.typeAll("\""); assertEquals("said \"hello\"", o.text)
    }

    @Test fun aSpaceTheOwnerTypedHimselfIsNeverTakenAway() {
        val r = rig("he said \"this "); r.typeAll("\""); assertEquals("he said \"this \"", r.text)
        val s = rig("he said "); s.typeAll("\"this "); s.typeAll("\""); assertEquals("typed by hand: left as typed", "he said \"this \"", s.text)
    }

    @Test fun wordsTypedByTapsInsideQuotesAreNotTouched() {
        val r = rig("he said "); r.typeAll("\"this works\""); assertEquals("he said \"this works\"", r.text)
    }

    @Test fun theCurlyQuotesWork() {
        val r = rig("he said "); r.typeAll("“"); r.act { commitGlide("this") }; r.typeAll("”"); assertEquals("he said “this”", r.text)
    }

    @Test fun aQuoteThatHasNoOpeningInTheWindowIsLeftAlone() {
        val r = rig("x".repeat(70) + " said "); r.act { commitGlide("this") }; r.typeAll("\""); assertEquals("not known to be a closing quote: the space stays", "x".repeat(70) + " said this \"", r.text)
    }

    @Test fun theUndoAfterTheClosingQuoteStillWorks() {
        val r = rig("he said "); r.typeAll("\""); r.act { commitGlide("this") }; r.typeAll("\""); r.act { backspace() }; assertEquals("he said \"this", r.text)
    }

    @Test fun aWordJustInsideAQuoteOrABracketIsStillTheWordBefore() {
        val r = rig("he said \"hello "); assertEquals("hello", r.c.previousTwo().second); assertEquals("said", r.c.previousTwo().first)
        val b = rig("(see hello "); assertEquals("hello", b.c.previousTwo().second)
        assertEquals("hello", rig("x").c.wordEndingAt("so \u201Chello"))
        assertEquals("a closed quote ends in a mark, not a word", null, rig("x").c.previousTwo().second.let { rig("he said \"hello\" ").c.previousTwo().second })
    }
}
