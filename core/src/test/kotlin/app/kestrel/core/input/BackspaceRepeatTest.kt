package app.kestrel.core.input

import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Owner R23a (2026-11-18): "if i long press the delete key, progressively delete from slow to fast, and eventually to deleting entire words at once." */
class BackspaceRepeatTest {
    /** The times (ms after the press) at which repeats fire when the key is held for [total] ms, and whether each deletes a word. */
    private fun schedule(total: Long): List<Pair<Long, Boolean>> {
        val out = ArrayList<Pair<Long, Boolean>>(); var t = BackspaceRepeat.FIRST_MS
        while (t <= total) { out.add(t to BackspaceRepeat.wordMode(t)); t += BackspaceRepeat.delayAfter(t) }
        return out
    }

    @Test fun theFirstRepeatsAreSlowAndGetFasterNeverSlower() {
        val s = schedule(BackspaceRepeat.WORDS_AFTER_MS - 1).map { it.first }
        val gaps = s.zipWithNext { a, b -> b - a }
        assertEquals("the first repeat comes after the usual wait", BackspaceRepeat.FIRST_MS, s.first())
        assertTrue("starts slow: ${gaps.first()} ms", gaps.first() >= BackspaceRepeat.SLOW_MS - 15); assertTrue("ends fast: ${gaps.last()} ms", gaps.last() <= BackspaceRepeat.FAST_MS + 15)
        for (i in 1 until gaps.size) assertTrue("never slower again: ${gaps[i - 1]} then ${gaps[i]}", gaps[i] <= gaps[i - 1])
    }

    @Test fun afterALongHoldEveryRepeatIsAWholeWordAndGetsFasterButStaysWordSized() {
        val all = schedule(8000)
        assertFalse(all.first().second); assertTrue("it ends in words", all.last().second)
        val firstWord = all.indexOfFirst { it.second }
        assertTrue("once words start they stay words", all.drop(firstWord).all { it.second })
        val gaps = all.drop(firstWord).map { it.first }.zipWithNext { a, b -> b - a }
        for (i in 1 until gaps.size) assertTrue("word repeats never slow down: ${gaps[i - 1]} then ${gaps[i]}", gaps[i] <= gaps[i - 1])
        assertTrue("word repeats start slower than the fastest characters", gaps.first() > BackspaceRepeat.FAST_MS * 3); assertTrue(gaps.last() >= BackspaceRepeat.WORD_FAST_MS - 15)
    }

    @Test fun aSensibleNumberOfCharactersGoBeforeTheWordsStart() {
        val chars = schedule(BackspaceRepeat.WORDS_AFTER_MS - 1).size
        assertTrue("between 12 and 40 characters before the first word: $chars", chars in 12..40)
        assertEquals("a tap or a short hold never gets to words", 0, schedule(BackspaceRepeat.FIRST_MS - 1).size)
    }

    // ------------------------------------------------------------------ what a word-sized delete does

    private fun rig(text: String, sel: Int = text.length) = Rig(text, sel)

    @Test fun deleteWordTakesTheWordAndTheSpacesAfterIt() {
        val r = rig("hello big world"); r.act { deleteWord() }; assertEquals("hello big ", r.text)
        r.act { deleteWord() }; assertEquals("hello ", r.text); r.act { deleteWord() }; assertEquals("", r.text); r.act { deleteWord() }; assertEquals("nothing left: nothing happens", "", r.text)
        val s = rig("hello world   "); s.act { deleteWord() }; assertEquals("hello ", s.text)
    }

    @Test fun deleteWordHandlesMarksDigitsAndApostrophes() {
        val r = rig("see you, then"); r.act { deleteWord() }; assertEquals("see you, ", r.text); r.act { deleteWord() }; assertEquals("one mark at a time: see you", "see you", r.text)
        val d = rig("call 555 now"); d.act { deleteWord() }; d.act { deleteWord() }; assertEquals("call ", d.text)
        val a = rig("say don't"); a.act { deleteWord() }; assertEquals("say ", a.text)
        val e = rig("ok 😀"); e.act { deleteWord() }; assertEquals("an emoji goes whole, never half", "ok ", e.text)
    }

    @Test fun deleteWordTakesAWordBeingTappedAndAGlideWholeAndASelectionAsOne() {
        val t = rig("a "); t.typeAll("hel"); t.act { deleteWord() }; assertEquals("a ", t.text)
        val g = rig("a "); g.act { commitGlide("hello") }; g.act { deleteWord() }; assertEquals("the glide goes whole, like one Backspace", "a ", g.text); g.act { deleteWord() }; assertEquals("and then the word before", "", g.text)
        val s = rig("one two three", 13); s.ed.userSelect(4, 7); s.sync(); s.act { deleteWord() }; assertEquals("one  three", s.text)
    }

    @Test fun deleteWordKeepsTheEditorsBeliefsTrueAndCapitalsComeBack() {
        val r = Rig("", sentenceCaps = true); r.typeAll("hi. there"); r.act { deleteWord() }; assertEquals("Hi. ", r.text); assertEquals(app.kestrel.core.input.editor.ShiftState.ONCE, r.c.shift)
        val w = Rig("x ".repeat(40) + "word"); w.act { deleteWord() }; assertTrue(w.text.endsWith("x "))
    }
}
