package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiMapTest {
    private val m = EmojiMap("clown\t🤡\npizza\t🍕\nlove\t❤️\n")

    @Test fun aWordWithAnEmojiGetsIt() { assertEquals("🤡", m.forWord("clown")); assertEquals("🤡", m.forWord("Clown")); assertEquals(3, m.size) }
    @Test fun pluralsFindTheSingular() { assertEquals("🍕", m.forWord("pizzas")); assertEquals("❤️", m.forWord("loves")) }
    @Test fun aWordWithoutOneGetsNone() { assertNull(m.forWord("table")); assertNull(m.forWord("ab")); assertNull(m.forWord("")); assertNull(m.forWord("don't")) }
    @Test fun anEmojiTheFontCannotDrawIsNotOffered() { assertNull(m.forWord("clown") { false }) }
    @Test fun isEmojiTellsWordsFromEmoji() {
        assertTrue(EmojiMap.isEmoji("🤡")); assertTrue(EmojiMap.isEmoji("❤️")); assertTrue(EmojiMap.isEmoji("🧑‍🚀"))
        assertFalse(EmojiMap.isEmoji("clown")); assertFalse(EmojiMap.isEmoji("a1")); assertFalse(EmojiMap.isEmoji("")); assertFalse(EmojiMap.isEmoji("?!")); assertFalse(EmojiMap.isEmoji("“clown”"))
    }
}
