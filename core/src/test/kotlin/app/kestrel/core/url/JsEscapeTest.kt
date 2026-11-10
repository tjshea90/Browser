package app.kestrel.core.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JsEscapeTest {
    @Test
    fun escapesEverythingThatCouldBreakOutOfTheLiteral() {
        assertEquals("\"a\\\"b\"", JsEscape.quote("a\"b"))
        assertEquals("\"a\\\\b\"", JsEscape.quote("a\\b"))
        assertEquals("\"l1\\nl2\\r\\t\"", JsEscape.quote("l1\nl2\r\t"))
        assertEquals("\"\\u2028\\u2029\"", JsEscape.quote("  "))
        assertEquals("\"\\u0000\\u001f\"", JsEscape.quote("\u0000\u001f"))
        assertEquals("\"<\\/style><\\/script>\"", JsEscape.quote("</style></script>"))
        assertEquals("\"plain .css > a[href^='x']{}\"", JsEscape.quote("plain .css > a[href^='x']{}"))
        assertEquals("\"\"", JsEscape.quote(""))
    }

    @Test
    fun hostileInputStaysInsideOneLiteral() {
        val hostile = "\";alert(1);//\n</script><script>evil()</script>\\\" "
        val q = JsEscape.quote(hostile)
        // strip the outer quotes; every remaining double quote must be preceded by a backslash
        val inner = q.substring(1, q.length - 1)
        var i = 0
        while (i < inner.length) {
            if (inner[i] == '\\') { i += 2; continue }
            assertFalse("unescaped quote at $i in $inner", inner[i] == '"')
            assertFalse(inner[i] == '\n' || inner[i] == ' ')
            i++
        }
        assertFalse(q.contains("</"))
    }
}
