package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner, 2026-11-17: "I typed a word in brave browser and it predicted the wrong word. when i pressed the correct word in the box of suggested words, it didn't change the word in the url bar." An address bar completes the
 * typed text inline (rest of an address appended after the cursor, SELECTED), which the keyboard sees as a selection it did not make. The glide bookkeeping used to give up there, so a tap in the strip did nothing.
 */
class InlineAutocompleteTest {
    private fun url(text: String = "") = Rig(text, multiline = false, kind = FieldKind.URL)

    @Test fun aStripTapStillReplacesTheGlidedWordWhenTheAddressBarCompletedItInline() {
        val r = url(); r.act { commitGlide("hello") }
        assertEquals("hello ", r.text)
        r.ed.appAutocomplete("world.com"); r.sync(); r.check()
        assertNotNull("the glide is still known", r.c.lastGlide)
        assertTrue(r.c.swapGlide("hallo")); r.sync(); r.check()
        assertEquals("hallo ", r.text)                                 // the completion is replaced too: the person chose another word
        assertEquals(6, r.ed.selS); assertEquals(6, r.ed.selE)
        assertEquals("hallo", r.c.lastGlide!!.word)
    }

    @Test fun aSecondPickStillWorksAfterTheFirst() {
        val r = url(); r.act { commitGlide("hello") }; r.ed.appAutocomplete("world.com"); r.sync()
        assertTrue(r.c.swapGlide("hallo")); r.sync(); r.ed.appAutocomplete("x.org"); r.sync(); r.check()
        assertTrue(r.c.swapGlide("hullo")); r.sync(); r.check(); assertEquals("hullo ", r.text)
    }

    @Test fun aSelectionOrMoveSomewhereElseStillForgetsTheGlide() {
        val r = url(); r.act { commitGlide("hello") }; r.ed.appAutocomplete("world.com"); r.sync()
        r.ed.userMove(2); r.sync(); assertNull(r.c.lastGlide)
        assertFalse(r.c.swapGlide("hallo")); assertEquals("hello world.com", r.text)
        val q = url("abc "); q.act { commitGlide("hello") }; q.ed.userSelect(0, 3); q.sync(); assertNull(q.c.lastGlide)
    }

    @Test fun theFallbackReplacesTheLastWordWhenTheKeyboardLostTrackOfItsGlide() {
        val r = url(); r.act { commitGlide("hello") }
        r.ed.appSetText("go hello "); r.sync()                           // the app rewrote the field: the glide is forgotten
        assertNull(r.c.lastGlide); assertFalse(r.c.swapGlide("hallo"))
        assertTrue(r.c.replaceRecent("hello", "hallo")); r.sync(); r.check()
        assertEquals("go hallo ", r.text); assertEquals("hallo", r.c.lastGlide!!.word)
    }

    @Test fun theFallbackIsVerifiedAndNeverEatsOtherText() {
        val r = url("see you "); assertFalse(r.c.replaceRecent("hello", "hallo")); assertEquals("see you ", r.text)
        val q = url(); q.act { commitGlide("hello") }; q.typeAll("x"); assertFalse("the word is no longer the last thing", q.c.replaceRecent("hello", "hallo"))
        val c = Rig("Hello "); assertTrue(c.c.replaceRecent("hello", "hallo")); c.sync(); assertEquals("Hallo ", c.text)   // keeps the capital
        val u = Rig("go HELLO "); assertTrue(u.c.replaceRecent("hello", "hallo")); u.sync(); assertEquals("go HALLO ", u.text)
    }

    @Test fun backspaceAfterInlineCompletionTakesTheCompletionFirstThenTheWord() {
        val r = url(); r.act { commitGlide("hello") }; r.ed.appAutocomplete("world.com"); r.sync()
        r.act { backspace() }; assertEquals("hello ", r.text)            // the selected completion goes first (as in the browser's own box)
    }
}
