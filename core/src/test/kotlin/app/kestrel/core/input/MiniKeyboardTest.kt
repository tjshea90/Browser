package app.kestrel.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The small address keyboard (AR2): what each key does, and the keys themselves. */
class MiniKeyboardTest {
    private fun typed(m: MiniKeyboardModel, s: String) { for (c in s) m.press(Key.Ch(c.toString())) }

    @Test fun theFirstKeyReplacesTheSelectedAddress() {
        val m = MiniKeyboardModel(); m.open("https://doraby.com/")
        assertTrue(m.allSelected)
        typed(m, "ab")
        assertEquals("ab", m.text); assertFalse(m.allSelected)
        typed(m, "c"); assertEquals("abc", m.text)
    }

    @Test fun backspaceClearsASelectionAtOnceThenRemovesOneKey() {
        val m = MiniKeyboardModel(); m.open("example.org")
        m.press(Key.Backspace); assertEquals("", m.text); assertFalse(m.allSelected)
        typed(m, "xyz"); m.press(Key.Backspace); assertEquals("xy", m.text)
        m.press(Key.Backspace); m.press(Key.Backspace); m.press(Key.Backspace); assertEquals("nothing left, nothing breaks", "", m.text)
    }

    @Test fun anEmptyBarHasNothingSelected() {
        val m = MiniKeyboardModel(); m.open("")
        assertFalse(m.allSelected); typed(m, "a"); assertEquals("a", m.text)
    }

    @Test fun shiftCapitalisesOneLetterOnly() {
        val m = MiniKeyboardModel(); m.open("")
        m.press(Key.Shift); assertTrue(m.shift)
        m.press(Key.Ch("k")); m.press(Key.Ch("e")); assertEquals("Ke", m.text); assertFalse(m.shift)
        m.press(Key.Shift); m.press(Key.Shift); assertFalse("pressed twice = off again", m.shift)
        m.press(Key.Shift); m.press(Key.Ch(".com")); assertEquals("a longer piece such as .com is never changed", "Ke.com", m.text)
    }

    @Test fun theSymbolPageComesAndGoes() {
        val m = MiniKeyboardModel(); m.open("")
        m.press(Key.ToSymbols); assertTrue(m.symbols); m.press(Key.ToLetters); assertFalse(m.symbols)
        m.press(Key.Shift); m.press(Key.ToSymbols); assertFalse("the page change drops a pending shift", m.shift)
        m.open("x"); m.press(Key.ToSymbols); m.open("y"); assertFalse("a new session starts on the letters", m.symbols)
    }

    @Test fun goSubmitsTheTrimmedTextAndIgnoresAnEmptyOne() {
        val m = MiniKeyboardModel(); m.open("")
        assertTrue(m.press(Key.Go) is KeyResult.None)
        typed(m, "  hello world "); val r = m.press(Key.Go)
        assertEquals("hello world", (r as KeyResult.Submit).text)
        m.open("https://a.example/"); assertEquals("Go with the old address still selected opens that address", "https://a.example/", (m.press(Key.Go) as KeyResult.Submit).text)
    }

    @Test fun theTextIsCapped() {
        val m = MiniKeyboardModel(); m.open("")
        repeat(MiniKeyboardModel.MAX_CHARS + 50) { m.press(Key.Ch("a")) }
        assertEquals(MiniKeyboardModel.MAX_CHARS, m.text.length)
        m.open("b".repeat(5000)); assertEquals(MiniKeyboardModel.MAX_CHARS, m.text.length)
    }

    @Test fun everyRowAddsUpToTheSameWidthAndEveryKeyHasAnAction() {
        for (symbols in listOf(false, true)) for (shift in listOf(false, true)) {
            val rows = MiniKeyboardLayout.rows(symbols, shift)
            assertEquals(4, rows.size)
            for (r in rows) assertEquals("row of $symbols/$shift: ${r.joinToString { it.label }}", MiniKeyboardLayout.ROW_UNITS, r.sumOf { it.weight.toDouble() }.toFloat(), 0.001f)
            for (r in rows) for (k in r) assertTrue("a labelled key has an action: '${k.label}'", k.label.isEmpty() == (k.key == null))
        }
        val flat = MiniKeyboardLayout.rows(false).flatten().mapNotNull { (it.key as? Key.Ch)?.text }
        for (c in "abcdefghijklmnopqrstuvwxyz") assertTrue("letter $c", c.toString() in flat)
        for (c in listOf("/", ".", ".com", " ")) assertTrue("'$c' is on the letter page: an address is typed with them", c in flat)
        val sym = MiniKeyboardLayout.rows(true).flatten().mapNotNull { (it.key as? Key.Ch)?.text }
        for (c in "0123456789-_:?=&%#@+") assertTrue("symbol $c", c.toString() in sym)
        assertEquals("upper case letters on the shifted page", "Q", MiniKeyboardLayout.rows(false, true)[0][0].label)
        assertNull("gaps carry no action", MiniKeyboardLayout.rows(false)[1].first().key)
    }

    @Test fun everyRowHasTheWayBackToTheOtherPageAndGo() {
        val bottom = MiniKeyboardLayout.rows(false)[3]; val bottomSym = MiniKeyboardLayout.rows(true)[3]
        assertTrue(bottom.any { it.key === Key.ToSymbols } && bottomSym.any { it.key === Key.ToLetters })
        assertTrue(bottom.any { it.key === Key.Go } && bottomSym.any { it.key === Key.Go })
    }
}
