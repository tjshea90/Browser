package app.kestrel.core.input

import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions
import app.kestrel.core.input.glide.GlideKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The layout of the photo (owner M1): four rows, ?123 at the bottom left, Enter at the bottom right, Shift and Backspace on the bottom letter row, every row ten units wide. */
class ImeLayoutTest {
    private fun keys(rows: List<List<KeySpec>>) = rows.flatten().mapNotNull { it.key }
    private fun letterText(row: List<KeySpec>) = row.filter { it.label.length == 1 && it.label[0].isLetter() }.joinToString("") { it.label }
    private fun boxes(rows: List<List<KeySpec>>, w: Float = 564f, h: Float = 260f, gx: Float = 8f, gy: Float = 12f) = MiniKeyboardGeometry.slots(rows, w, h, gx, gy, 0f, ImeLayout.rowWeights(rows))

    @Test fun everyLetterPageHasAllTwentySixLettersSoTheDecoderCanUseIt() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.URL, FieldKind.EMAIL, FieldKind.PASSWORD)) for (shift in listOf(false, true)) for (numberRow in listOf(false, true)) {
            val rows = ImeLayout.rows(Page.LETTERS, shift, kind, numberRow = numberRow)
            val gk = GlideKeys.from(rows, boxes(rows))          // throws when a letter is missing
            assertEquals(26, gk.cx.size); assertTrue(gk.pitch > 40f)
        }
    }

    @Test fun theLettersAreInTheUsualOrderAndThereIsNoNumberRowUnlessSwitchedOn() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)
        assertEquals("four rows like the photo", 4, rows.size)
        assertEquals("qwertyuiop", letterText(rows[0])); assertEquals("asdfghjkl", letterText(rows[1])); assertEquals("zxcvbnm", letterText(rows[2]))
        val withRow = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, numberRow = true)
        assertEquals(5, withRow.size); assertEquals("the number row is on top when asked for", "1234567890", withRow[0].joinToString("") { it.label })
    }

    @Test fun shiftIsAtTheLeftAndBackspaceAtTheRightOfTheBottomLetterRowAndTheActionKeyEndsTheBottomRow() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, "Go")
        assertEquals(Key.Shift, rows[2].first().key); assertEquals(Key.Backspace, rows[2].last().key)
        assertEquals(ImeLayout.WIDE_KEY, rows[2].first().weight, 0.001f); assertEquals(ImeLayout.WIDE_KEY, rows[2].last().weight, 0.001f)
        assertEquals(Key.Go, rows[3].last().key); assertEquals("Go", rows[3].last().label)
        assertTrue("no Backspace and no action key on the first two rows", rows.take(2).none { r -> r.any { it.key === Key.Backspace || it.key === Key.Go || it.key === Key.Shift } })
        assertTrue("nothing but the bottom row has the special characters key", rows.dropLast(1).none { r -> r.any { it.key === Key.ToSymbols } })
    }

    @Test fun theThreeLetterRowsShareOneKeyPitchAndLineUpLikeThePhoto() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)
        val b = boxes(rows)
        fun cx(ch: String): Float { for ((i, r) in rows.withIndex()) for ((j, k) in r.withIndex()) if (k.label == ch) return b[i][j].centerX; throw AssertionError(ch) }
        val pitch = cx("w") - cx("q")
        for ((a, c) in listOf("w" to "e", "s" to "d", "x" to "c", "o" to "p")) assertEquals("$a-$c is one pitch", pitch, cx(c) - cx(a), 0.01f)
        assertEquals("the home row is indented half a key", pitch / 2f, cx("a") - cx("q"), 0.01f)
        // the photo: Z stands under S, X under D, C under F, V under G, B under H, N under J, M under K
        for ((low, high) in listOf("z" to "s", "x" to "d", "c" to "f", "v" to "g", "b" to "h", "n" to "j", "m" to "k")) assertEquals("$low under $high", cx(high), cx(low), 0.01f)
        // the row is full: Q starts at the left edge, P ends at the right edge
        assertEquals(0f, b[0][0].left, 0.01f); assertEquals(564f, b[0].last().right, 0.01f)
        for (r in b.indices) assertEquals("row $r ends at the right edge", 564f, b[r].last().right, 0.01f)
    }

    @Test fun shiftMakesCapitalLetters() {
        val k = keys(ImeLayout.rows(Page.LETTERS, true, FieldKind.TEXT)).filterIsInstance<Key.Ch>().map { it.text }
        assertTrue("Q" in k && "M" in k && "q" !in k)
    }

    @Test fun theBottomRowIsTheSpecialCharactersKeyCommaSpaceFullStopAndEnterLikeThePhoto() {
        fun bottom(kind: FieldKind) = ImeLayout.rows(Page.LETTERS, false, kind, "Go").last().map { it.label }
        assertEquals(listOf("?123", ",", "space", ".", "Go"), bottom(FieldKind.TEXT))
        assertEquals(listOf("?123", "/", "space", ".", ".com", "Go"), bottom(FieldKind.URL))
        assertEquals("the Kestrel address-and-search box has the URL keys", listOf("?123", "/", "space", ".", ".com", "Go"), bottom(FieldKind.OMNIBOX))
        assertEquals(listOf("?123", "@", "space", ".", ".com", "Go"), bottom(FieldKind.EMAIL))
        for (kind in listOf(FieldKind.TEXT, FieldKind.URL, FieldKind.EMAIL)) {
            val row = ImeLayout.rows(Page.LETTERS, false, kind).last()
            assertEquals("$kind: ?123 at the LEFT", Key.ToSymbols, row.first().key); assertEquals("$kind: Enter at the RIGHT", Key.Go, row.last().key)
        }
        assertEquals("abc is the first key of the symbols pages", Key.ToLetters, ImeLayout.rows(Page.SYMBOLS, false, FieldKind.TEXT).last().first().key)
        assertEquals(listOf(1.5f, 1f, 5f, 1f, 1.5f), ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT).last().map { it.weight })
    }

    @Test fun nothingSitsUnderTheSpaceBarAndThereIsNoEmojiVoiceOrGlobeKey() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)
        val labels = rows.flatten().map { it.label }
        assertEquals(4, rows.size); assertEquals("space", rows.last()[2].label)
        for (bad in listOf("globe", "😀", "mic", "voice", "emoji")) assertTrue(bad, bad !in labels)
        assertTrue(keys(rows).none { it === Key.NextKeyboard })
    }

    @Test fun theSymbolsPagesAndTheHoldGridHaveEverySymbolAndThePagesCycle() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.PASSWORD, FieldKind.URL, FieldKind.EMAIL)) {
            assertTrue("$kind has ?123", keys(ImeLayout.rows(Page.LETTERS, false, kind)).any { it === Key.ToSymbols })
            for (p in listOf(Page.SYMBOLS, Page.SYMBOLS2)) assertTrue("$kind $p has abc", keys(ImeLayout.rows(p, false, kind)).any { it === Key.ToLetters })
        }
        for (numberRow in listOf(false, true)) {
            val page1 = keys(ImeLayout.rows(Page.SYMBOLS, false, FieldKind.TEXT, numberRow = numberRow)).filterIsInstance<Key.Ch>().map { it.text }.toSet()
            val page2 = keys(ImeLayout.rows(Page.SYMBOLS2, false, FieldKind.TEXT, numberRow = numberRow)).filterIsInstance<Key.Ch>().map { it.text }.toSet()
            val grid = ImeLayout.symbolRows.flatten().toSet()
            for (c in "@#$%&-+()/*\"':;!?_=~<>[]{}\\|€£¥") {
                assertTrue("'$c' on a symbols page (number row $numberRow)", c.toString() in page1 || c.toString() in page2)
                assertTrue("'$c' in the hold grid", c.toString() in grid)
            }
            for (d in "1234567890") assertTrue("digit $d reachable on the first symbols page (number row $numberRow)", d.toString() in page1 || numberRow)
        }
        val s1 = ImeLayout.rows(Page.SYMBOLS, false, FieldKind.TEXT); val s2 = ImeLayout.rows(Page.SYMBOLS2, false, FieldKind.TEXT)
        assertTrue("page 1 has the =\\< key to page 2", s1.flatten().any { it.label == ImeLayout.MORE_SYMBOLS && it.key === Key.ToSymbols })
        assertTrue("page 2 has the ?123 key back to page 1", s2.flatten().any { it.label == "?123" && it.key === Key.ToSymbols })
        assertEquals("1234567890", s1[0].joinToString("") { it.label })
    }

    @Test fun everyPageOfATextFieldHasTheSameNumberOfRowsSoTheKeyboardKeepsItsHeight() {
        for (numberRow in listOf(false, true)) for (p in Page.values()) assertEquals("$p number row $numberRow", if (numberRow) 5 else 4, ImeLayout.rows(p, false, FieldKind.TEXT, numberRow = numberRow).size)
    }

    @Test fun theSpaceBarTakesEverythingTheOtherKeysLeave() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.PASSWORD, FieldKind.URL, FieldKind.EMAIL)) for (globe in listOf(false, true)) {
            val row = ImeLayout.rows(Page.LETTERS, false, kind, showGlobe = globe).last()
            val space = row.first { (it.key as? Key.Ch)?.text == " " }
            assertEquals("$kind: the row is exactly full", ImeLayout.ROW_UNITS.toDouble(), row.sumOf { it.weight.toDouble() }, 0.001)
            assertTrue("$kind: the space bar is the widest key (${space.weight})", row.all { it === space || it.weight < space.weight })
        }
        val plain = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT).last().first { it.label == "space" }
        assertEquals(5.0f, plain.weight, 0.001f)                       // 10 - ?123 1.5 - comma 1 - full stop 1 - Enter 1.5
    }

    @Test fun everyPageHasASpaceAnActionKeyAndBackspaceAndEveryTextRowIsExactlyTenUnitsWide() {
        for (kind in FieldKind.values()) for (page in Page.values()) for (numberRow in listOf(false, true)) {
            val rows = ImeLayout.rows(page, false, kind, numberRow = numberRow)
            val ks = keys(rows)
            if (kind != FieldKind.PHONE) assertTrue("$kind/$page needs a space", ks.any { it is Key.Ch && it.text == " " })   // a phone number has no spaces
            assertTrue("$kind/$page needs the action key", ks.any { it === Key.Go })
            assertTrue("$kind/$page needs backspace", ks.any { it === Key.Backspace })
            if (kind != FieldKind.NUMBER && kind != FieldKind.PHONE) for (r in rows) assertEquals("$kind/$page row ${r.joinToString("") { it.label }}", ImeLayout.ROW_UNITS.toDouble(), r.sumOf { it.weight.toDouble() }, 0.001)
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

    @Test fun rowWeightsMakeTheNumberRowThinnerAndTheBoxesFillTheHeight() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, numberRow = true); val b = boxes(rows, 564f, 300f, 8f, 12f)
        assertTrue(b[0][0].height < b[1][0].height * 0.8f); assertEquals(b[1][0].height, b[4][0].height, 0.01f)
        assertEquals(300f, b[4][0].bottom, 0.01f); assertEquals(0f, b[0][0].top, 0.01f)
        for (i in 0 until 4) assertEquals(12f, b[i + 1][0].top - b[i][0].bottom, 0.01f)
        val plain = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT); val p = boxes(plain, 564f, 300f, 8f, 12f)
        assertEquals("without the number row all four rows are alike", p[0][0].height, p[3][0].height, 0.01f)
    }

    @Test fun slotsKeepTheGapBetweenKeysAndTheOuterEdgesFlush() {
        val rows = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT); val b = boxes(rows)
        for (i in 0 until b[0].size - 1) assertEquals(8f, b[0][i + 1].left - b[0][i].right, 0.01f)
        val bottom = b[3]; assertEquals(0f, bottom.first().left, 0.01f); assertEquals(564f, bottom.last().right, 0.01f)
        for (i in 0 until bottom.size - 1) assertEquals(8f, bottom[i + 1].left - bottom[i].right, 0.01f)
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
