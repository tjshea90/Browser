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

    // ------------------------------------------------------------------ glide typing (AT1e)

    @Test fun aGlideAddsTheWordAndASpaceAndTheFirstOneReplacesTheSelectedAddress() {
        val m = MiniKeyboardModel(); m.open("old.example.com/page")
        assertTrue(m.allSelected)
        m.commitGlide("hello")
        assertEquals("hello ", m.text); assertFalse(m.allSelected); assertTrue(m.glideActive)
        m.commitGlide("world")
        assertEquals("hello world ", m.text)
    }

    @Test fun aGlideAfterTypedLettersGetsASpaceBeforeIt() {
        val m = MiniKeyboardModel(); m.open("")
        m.press(Key.Ch("a")); m.press(Key.Ch("b"))
        m.commitGlide("cat")
        assertEquals("ab cat ", m.text)
        assertEquals("cat", m.previousWord()) // the text ends in a space: the word before the next glide is the last one
    }

    @Test fun oneBackspaceTakesTheWholeGlidedWordAway() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("hello"); m.commitGlide("world")
        m.press(Key.Backspace)
        assertEquals("hello ", m.text); assertFalse(m.glideActive)
        m.press(Key.Backspace) // the glide is over: now it deletes a letter at a time
        assertEquals("hello", m.text)
        // with a separator space added by the glide, that goes too
        m.open(""); m.press(Key.Ch("a")); m.commitGlide("cat"); m.press(Key.Backspace)
        assertEquals("a", m.text)
    }

    @Test fun aTypedLetterEndsTheGlideSoBackspaceIsNormalAgain() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("hello"); m.press(Key.Ch("x"))
        assertFalse(m.glideActive)
        m.press(Key.Backspace)
        assertEquals("hello ", m.text)
    }

    @Test fun aCandidateFromTheStripReplacesTheGlidedWord() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("hello"); m.commitGlide("word")
        assertTrue(m.swapGlide("world"))
        assertEquals("hello world ", m.text); assertTrue(m.glideActive)
        assertTrue(m.swapGlide("wood"))
        assertEquals("hello wood ", m.text)
        m.press(Key.Ch("z"))
        assertFalse("only a glide can be swapped", m.swapGlide("nope"))
        assertEquals("hello wood z", m.text)
    }

    @Test fun shiftMakesAGlidedWordStartWithACapitalAndTheSwapKeepsIt() {
        val m = MiniKeyboardModel(); m.open("")
        m.press(Key.Shift); m.commitGlide("paris")
        assertEquals("Paris ", m.text); assertFalse(m.shift)
        assertTrue(m.swapGlide("parts"))
        assertEquals("Parts ", m.text)
    }

    @Test fun theWordBeforeIsOnlyAPlainWord() {
        val m = MiniKeyboardModel()
        m.open("Hello"); assertNull("selected address: nothing before", m.previousWord())
        m.press(Key.Backspace); m.press(Key.Ch("H")); m.press(Key.Ch("i")); assertEquals("hi", m.previousWord())
        m.open(""); m.press(Key.Ch("a")); m.press(Key.Ch(".")); m.press(Key.Ch("c")); assertNull("an address is not a word", m.previousWord())
        m.open(""); m.press(Key.Ch("d")); m.press(Key.Ch("o")); m.press(Key.Ch("n")); m.press(Key.Ch("'")); m.press(Key.Ch("t")); assertEquals("don't", m.previousWord())
    }

    @Test fun plainWordsAreWhatTheKeyboardMayRemember() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("Hello"); m.commitGlide("dorabyy"); m.press(Key.Ch("x")); m.press(Key.Ch("1"))
        m.open(""); for (c in "my site. doraby9 abc a-b ok") m.press(Key.Ch(c.toString()))
        assertEquals(listOf("abc"), m.plainWords().filter { it == "abc" }) // 'my', 'ok' are too short; 'site.' and 'doraby9' have other characters
        assertEquals(listOf("abc"), m.plainWords())
    }

    // ------------------------------------------------------------------ what the keyboard may learn from typing (AU2)

    @Test fun onlyTheWordsTypedLetterByLetterAreLeftToLearnAtGo() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("best")           // learned when it was kept
        for (c in "pizza") m.press(Key.Ch(c.toString()))
        m.press(Key.Ch(" "))
        m.commitGlide("near")
        for (c in "me") m.press(Key.Ch(c.toString()))
        assertEquals("best pizza near me", m.text)
        assertEquals(listOf<Pair<String?, String>>("best" to "pizza", "near" to "me"), m.typedPairs())
    }

    @Test fun aDeletedGlideIsNoLongerCountedAsGlided() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("wrong"); m.press(Key.Backspace)
        for (c in "right") m.press(Key.Ch(c.toString()))
        assertEquals(listOf<Pair<String?, String>>(null to "right"), m.typedPairs())
    }

    @Test fun aSwappedGlideIsTheOneThatIsCounted() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("word"); m.swapGlide("world")
        for (c in "peace") m.press(Key.Ch(c.toString()))
        assertEquals(listOf<Pair<String?, String>>("world" to "peace"), m.typedPairs().map { it.first to it.second }.filter { it.second == "peace" })
        assertEquals(1, m.typedPairs().size)
    }

    @Test fun anAddressIsNotWhatTheKeyboardLearnsFrom() {
        val m = MiniKeyboardModel(); m.open("")
        for (c in "doraby.com/movie") m.press(Key.Ch(c.toString()))
        assertTrue(m.typedPairs().isEmpty())
    }

    // ------------------------------------------------------------------ smart spacing after a glide (AV3b)

    @Test fun aGlidedWordThenDotComIsOneAddressNotTwoWords() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("doraby"); assertEquals("doraby ", m.text)
        m.press(Key.Ch(".com"))
        assertEquals("doraby.com", m.text)
    }

    @Test fun punctuationAfterAGlideTakesAwayItsSpaceAndTheNextGlideAddsOneBack() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("hello"); m.press(Key.Ch(","))
        assertEquals("hello,", m.text)
        m.commitGlide("world"); assertEquals("hello, world ", m.text)
        m.press(Key.Ch("!")); assertEquals("hello, world!", m.text)
    }

    @Test fun aSpaceTypedByHandIsNotEatenAndNeitherIsLetterTyping() {
        val m = MiniKeyboardModel(); m.open("")
        for (c in "go ") m.press(Key.Ch(c.toString()))
        m.press(Key.Ch(".")); assertEquals("go .", m.text) // a space the person typed stays
        val n = MiniKeyboardModel(); n.open("")
        n.commitGlide("hello"); n.press(Key.Ch("w")); assertEquals("hello w", n.text) // a letter after a glide keeps the space
    }

    @Test fun aGlideThenSlashOrApostropheJoinsTheWord() {
        val m = MiniKeyboardModel(); m.open("")
        m.commitGlide("movie"); m.press(Key.Ch("/")); assertEquals("movie/", m.text)
        val n = MiniKeyboardModel(); n.open("")
        n.commitGlide("don"); n.press(Key.Ch("'")); n.press(Key.Ch("t")); assertEquals("don't", n.text)
    }

    // ------------------------------------------------------------------ caps lock and the page-field bottom row (AV3b)

    @Test fun capsLockKeepsLettersAndGlidedWordsInCapitalsUntilShiftIsTappedAgain() {
        val m = MiniKeyboardModel(); m.open("")
        m.press(Key.Shift); m.lockShift()
        assertTrue(m.shift && m.shiftLock)
        m.press(Key.Ch("a")); m.press(Key.Ch("b"))
        assertEquals("AB", m.text); assertTrue("still locked after typing", m.shift && m.shiftLock)
        m.commitGlide("hello"); assertEquals("AB HELLO ", m.text)
        m.swapGlide("help"); assertEquals("a guess from the strip keeps the capitals", "AB HELP ", m.text)
        m.press(Key.Shift)
        assertFalse(m.shift || m.shiftLock)
        m.press(Key.Ch("c")); assertEquals("AB HELP c", m.text)
    }

    @Test fun oneTapOnShiftIsStillForOneLetterOnly() {
        val m = MiniKeyboardModel(); m.open("")
        m.press(Key.Shift); m.press(Key.Ch("a")); m.press(Key.Ch("b"))
        assertEquals("Ab", m.text); assertFalse(m.shiftLock)
    }

    @Test fun theLettersPageGetsACommaAnApostropheAndAQuestionMarkWhenTypingIntoAPageField() {
        val address = MiniKeyboardLayout.rows(false)[3].map { it.label }
        assertTrue(address.contains("/") && address.contains(".com"))
        val field = MiniKeyboardLayout.rows(false, forField = true)[3]
        assertEquals(listOf("123", ",", ".", "space", "'", "?", "Go"), field.map { it.label })
        assertEquals(MiniKeyboardLayout.ROW_UNITS, field.sumOf { it.weight.toDouble() }.toFloat(), 0.01f)
        assertEquals("the letters above are the same", MiniKeyboardLayout.rows(false)[0].map { it.label }, MiniKeyboardLayout.rows(false, forField = true)[0].map { it.label })
    }
}

