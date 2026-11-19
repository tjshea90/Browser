package app.kestrel.core.input.glide

import app.kestrel.core.input.glide.UserDictionary.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDictionaryTest {
    @Test fun addedWordsAreStoredLowercaseAndSorted() {
        val d = UserDictionary(); assertEquals(Result.OK, d.add("  Zelda ")); assertEquals(Result.OK, d.add("Mario")); assertEquals(listOf("mario", "zelda"), d.addedWords()); assertTrue(d.isAdded("ZELDA"))
    }
    @Test fun anApostropheIsDroppedAndNonLettersAreRefused() {
        val d = UserDictionary(); assertEquals(Result.OK, d.add("o'neil")); assertEquals(listOf("oneil"), d.addedWords())
        assertEquals(Result.NOT_A_WORD, d.add("abc1")); assertEquals(Result.NOT_A_WORD, d.add("")); assertEquals(Result.NOT_A_WORD, d.add("two words")); assertEquals(Result.TOO_SHORT, d.add("a")); assertEquals(Result.TOO_LONG, d.add("a".repeat(25)))
    }
    @Test fun blockingRemovesAnAddedWordAndAddingUnblocks() {
        val d = UserDictionary(); d.add("tui"); assertEquals(Result.OK, d.block("tui"))
        assertTrue(d.isBlocked("TUI")); assertFalse(d.isAdded("tui")); assertEquals(listOf("tui"), d.blockedWords())
        d.add("tui"); assertFalse(d.isBlocked("tui")); assertTrue(d.isAdded("tui"))
        d.block("tui"); assertTrue(d.unblock("tui")); assertFalse(d.isBlocked("tui")); assertFalse(d.unblock("tui"))
    }
    @Test fun roundTripKeepsBothLists() {
        val d = UserDictionary(); d.add("alpha"); d.add("beta"); d.block("tui"); d.block("gamma")
        val back = UserDictionary.parse(d.serialize()); assertEquals(d.addedWords(), back.addedWords()); assertEquals(d.blockedWords(), back.blockedWords())
        assertEquals("", UserDictionary().serialize())
    }
    @Test fun aForeignOrDamagedFileLoadsAsEmptyOrSkipsBadLines() {
        assertEquals(0, UserDictionary.parse("not ours").addedCount); assertEquals(0, UserDictionary.parse(null).blockedCount)
        val d = UserDictionary.parse(UserDictionary.HEADER + "\n+ok\n+1234\n-bad word\n-fine\n\n?x\n"); assertEquals(listOf("ok"), d.addedWords()); assertEquals(listOf("fine"), d.blockedWords())
    }
    @Test fun theListsHaveALimit() { val d = UserDictionary(); for (i in 0 until UserDictionary.MAX_WORDS) assertEquals(Result.OK, d.add(wordNo(i))); assertEquals(Result.FULL, d.add("overflow")) }
    private fun wordNo(i: Int): String { var n = i; val sb = StringBuilder("zz"); do { sb.append('a' + n % 26); n /= 26 } while (n > 0); return sb.toString() }
}
