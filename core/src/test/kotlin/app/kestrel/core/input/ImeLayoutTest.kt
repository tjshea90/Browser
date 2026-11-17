package app.kestrel.core.input

import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions
import app.kestrel.core.input.glide.GlideKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeLayoutTest {
    private fun keys(rows: List<List<KeySpec>>) = rows.flatten().mapNotNull { it.key }

    @Test fun everyLetterPageHasAllTwentySixLettersSoTheDecoderCanUseIt() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.URL, FieldKind.EMAIL, FieldKind.PASSWORD)) for (shift in listOf(false, true)) {
            val rows = ImeLayout.rows(Page.LETTERS, shift, kind)
            val boxes = MiniKeyboardGeometry.boxes(rows, 564f, 220f, 3f, 0f, ImeLayout.rowWeights(rows))
            val gk = GlideKeys.from(rows, boxes)          // throws when a letter is missing
            assertEquals(26, gk.cx.size); assertTrue(gk.pitch > 40f)
        }
    }

    @Test fun theLettersAreInTheUsualOrderUnderTheNumberRow() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)
        assertEquals("the number row is on top", "1234567890", rows[0].joinToString("") { it.label })
        assertEquals("qwertyuiop", rows[1].filter { it.label.length == 1 && it.label[0].isLetter() }.joinToString("") { it.label })
        assertEquals("asdfghjkl", rows[2].filter { it.label.length == 1 && it.label[0].isLetter() }.joinToString("") { it.label })
        assertEquals("zxcvbnm", rows[3].filter { it.label.length == 1 && it.label[0].isLetter() }.joinToString("") { it.label })
    }

    @Test fun backspaceEndsTheFirstLetterRowTheActionKeyEndsTheHomeRowAndShiftStandsAtBothEndsOfTheLastLetterRow() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, "Go")
        assertEquals(Key.Backspace, rows[1].last().key); assertEquals(Key.Go, rows[2].last().key); assertEquals("Go", rows[2].last().label)
        assertEquals(Key.Shift, rows[3].first().key); assertEquals(Key.Shift, rows[3].last().key)
        assertTrue("nothing but the bottom row has the special characters key", rows.dropLast(1).none { r -> r.any { it.key === Key.ToSymbols } })
    }

    @Test fun shiftMakesCapitalLetters() {
        val k = keys(ImeLayout.rows(Page.LETTERS, true, FieldKind.TEXT)).filterIsInstance<Key.Ch>().map { it.text }
        assertTrue("Q" in k && "M" in k && "q" !in k)
    }

    @Test fun theBottomRowIsCommaSpaceFullStopAndTheSpecialCharactersKeyAtTheBottomRight() {
        fun bottom(kind: FieldKind) = ImeLayout.rows(Page.LETTERS, false, kind, "Go").last().map { it.label }
        assertEquals(listOf(",", "space", ".", "?123"), bottom(FieldKind.TEXT))
        assertEquals(listOf("/", "space", ".", ".com", "?123"), bottom(FieldKind.URL))
        assertEquals(listOf("@", "space", ".", ".com", "?123"), bottom(FieldKind.EMAIL))
        for (kind in listOf(FieldKind.TEXT, FieldKind.URL, FieldKind.EMAIL)) assertEquals(Key.ToSymbols, ImeLayout.rows(Page.LETTERS, false, kind).last().last().key)
        assertEquals(Key.ToLetters, ImeLayout.rows(Page.SYMBOLS, false, FieldKind.TEXT).last().last().key)
    }

    @Test fun nothingSitsUnderTheSpaceBarAndThereIsNoEmojiVoiceOrGlobeKey() {
        val labels = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT).flatten().map { it.label }
        assertEquals(5, ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT).size)
        for (bad in listOf("globe", "\uD83D\uDE00", "mic", "voice", "emoji")) assertTrue(bad, bad !in labels)
        assertTrue(keys(ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)).none { it === Key.NextKeyboard })
    }

    @Test fun theOneTwoThreeKeyIsBackAndTheSymbolsPageAndTheHoldGridHaveEverySymbol() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.PASSWORD, FieldKind.URL, FieldKind.EMAIL)) {
            assertTrue("$kind has 123", keys(ImeLayout.rows(Page.LETTERS, false, kind)).any { it === Key.ToSymbols })
            assertTrue("$kind symbols page has abc", keys(ImeLayout.rows(Page.SYMBOLS, false, kind)).any { it === Key.ToLetters })
        }
        val page = keys(ImeLayout.rows(Page.SYMBOLS, false, FieldKind.TEXT)).filterIsInstance<Key.Ch>().map { it.text }.toSet()
        val grid = ImeLayout.symbolRows.flatten().toSet()
        for (c in "@#$%&-+()/*\"':;!?_=~<>[]{}\\|\u20AC\u00A3\u00A5") { assertTrue("'$c' on the symbols page", c.toString() in page); assertTrue("'$c' in the hold grid", c.toString() in grid) }
    }

    @Test fun theSpaceBarTakesEverythingTheOtherKeysLeave() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.PASSWORD, FieldKind.URL, FieldKind.EMAIL)) for (globe in listOf(false, true)) {
            val row = ImeLayout.rows(Page.LETTERS, false, kind, showGlobe = globe).last()
            val space = row.first { (it.key as? Key.Ch)?.text == " " }
            assertEquals("$kind: the row is exactly full", ImeLayout.ROW_UNITS.toDouble(), row.sumOf { it.weight.toDouble() }, 0.001)
            assertTrue("$kind: the space bar is the widest key (${space.weight})", row.all { it === space || it.weight < space.weight })
        }
        val plain = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT).last().first { it.label == "space" }
        assertEquals(6.0f, plain.weight, 0.001f)                       // 11.5 - comma 1.5 - full stop 1.5 - special characters 2.5
    }

    @Test fun everyPageHasASpaceAnActionKeyAndBackspaceAndNoRowIsWiderThanTheRowUnits() {
        for (kind in FieldKind.values()) for (page in Page.values()) {
            val rows = ImeLayout.rows(page, false, kind)
            val ks = keys(rows)
            if (kind != FieldKind.PHONE) assertTrue("$kind/$page needs a space", ks.any { it is Key.Ch && it.text == " " })   // a phone number has no spaces
            assertTrue("$kind/$page needs the action key", ks.any { it === Key.Go })
            assertTrue("$kind/$page needs backspace", ks.any { it === Key.Backspace })
            if (kind != FieldKind.NUMBER && kind != FieldKind.PHONE) for (r in rows) assertTrue(r.sumOf { it.weight.toDouble() } <= ImeLayout.ROW_UNITS + 0.001)
        }
    }

    @Test fun theGlobeKeyIsOffByDefaultAndComesBackWhenAskedOrInSafeMode() {
        for (kind in FieldKind.values()) {
            assertTrue("$kind off by default", keys(ImeLayout.rows(Page.LETTERS, false, kind)).none { it === Key.NextKeyboard })
            val rows = ImeLayout.rows(Page.LETTERS, false, kind, showGlobe = true)
            assertTrue("$kind on when asked", keys(rows).any { it === Key.NextKeyboard }); assertTrue("$kind: at the left end of the bottom row", rows.last().take(2).any { it.key === Key.NextKeyboard })
            if (kind != FieldKind.NUMBER && kind != FieldKind.PHONE) assertEquals("$kind: still a full row", ImeLayout.ROW_UNITS.toDouble(), rows.last().sumOf { it.weight.toDouble() }, 0.001)
        }
    }

    @Test fun theNumberRowIsOnEveryTextPageAndCanBeLeftOut() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.URL, FieldKind.EMAIL, FieldKind.PASSWORD)) for (page in Page.values()) {
            val rows = ImeLayout.rows(page, false, kind); assertEquals("$kind/$page", 5, rows.size); assertEquals("1234567890", rows[0].joinToString("") { it.label })
        }
        assertEquals(4, ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, "⏎", numberRow = false).size)
    }

    @Test fun rowWeightsMakeTheNumberRowThinnerAndTheBoxesFillTheHeight() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT); val b = MiniKeyboardGeometry.boxes(rows, 564f, 300f, 3f, 0f, ImeLayout.rowWeights(rows))
        assertTrue(b[0][0].height < b[1][0].height * 0.8f); assertEquals(b[1][0].height, b[4][0].height, 0.01f)
        assertEquals(300f, b[4][0].bottom, 0.01f); assertEquals(0f, b[0][0].top, 0.01f)
        for (i in 0 until 4) assertEquals(3f, b[i + 1][0].top - b[i][0].bottom, 0.01f)
    }

    @Test fun numberAndPhoneFieldsGetAKeypadWhateverThePage() {
        val n = ImeLayout.rows(Page.LETTERS, true, FieldKind.NUMBER); assertEquals(4, n.size)
        assertEquals(listOf("1", "2", "3", "⌫"), n[0].map { it.label }); assertEquals("0", n[3][0].label)
        val p = ImeLayout.rows(Page.LETTERS, false, FieldKind.PHONE); assertEquals(listOf("0", ",", "⏎"), p[3].map { it.label })
        assertEquals(listOf("globe", "0", ",", "⏎"), ImeLayout.rows(Page.LETTERS, false, FieldKind.PHONE, showGlobe = true)[3].map { it.label })
        assertTrue(keys(n).filterIsInstance<Key.Ch>().none { it.text.length == 1 && it.text[0].isLetter() })
    }

    @Test fun theActionKeyIsLabelledForWhatEnterDoes() {
        assertEquals("Go", ImeLayout.actionLabel(ImeActions.GO, false)); assertEquals("Search", ImeLayout.actionLabel(ImeActions.SEARCH, false))
        assertEquals("Send", ImeLayout.actionLabel(ImeActions.SEND, false)); assertEquals("Next", ImeLayout.actionLabel(ImeActions.NEXT, false))
        assertEquals("Done", ImeLayout.actionLabel(ImeActions.DONE, false)); assertEquals("⏎", ImeLayout.actionLabel(ImeActions.UNSPECIFIED, false))
        assertEquals("⏎", ImeLayout.actionLabel(ImeActions.SEND, true)); assertNotNull(ImeLayout.actionLabel(99, false))
    }
}
