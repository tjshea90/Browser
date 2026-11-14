package app.kestrel.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditTest {
    private fun edit(a: String, b: String) = TextEdit.between(a, b).let { it.remove to it.insert }

    @Test fun typingAddsToTheEnd() { assertEquals(0 to "c", edit("ab", "abc")); assertEquals(0 to ".com", edit("doraby", "doraby.com")) }
    @Test fun backspaceTakesOffTheEnd() { assertEquals(1 to "", edit("abc", "ab")); assertEquals(7 to "", edit("hello world ", "hello")) }
    @Test fun aSwappedWordReplacesTheTail() { assertEquals(5 to "ield ", edit("hello world ", "hello wield ")) // the shortest edit: only what differs }
    @Test fun nothingChangedIsEmpty() { assertTrue(TextEdit.between("same", "same").isEmpty); assertTrue(TextEdit.between("", "").isEmpty) }
    @Test fun aFreshTextIsAllInsert() { assertEquals(0 to "hi", edit("", "hi")); assertEquals(2 to "", edit("hi", "")) }
    @Test fun aCapitalLetterReplacesNothingBeforeIt() { assertEquals(0 to "A", edit("", "A")); assertEquals(1 to "B", edit("A", "B")) }
    @Test fun aSurrogatePairIsNeverSplit() {
        val a = "x😀"; val b = "x😁"           // two different emoji sharing their first half
        assertEquals(2 to "😁", edit(a, b))
    }
}
