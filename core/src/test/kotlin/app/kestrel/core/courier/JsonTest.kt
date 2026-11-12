package app.kestrel.core.courier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JsonTest {
    private fun bad(text: String, maxDepth: Int = 16) {
        try { Json.parse(text, maxDepth); fail("should have been refused: $text") } catch (_: JsonException) {}
    }

    @Test fun parsesEveryShape() {
        val v = Json.parse(""" {"a": 1, "b": [true, false, null, "x"], "c": {"d": -2.5e1}, "e": ""} """) as Map<*, *>
        assertEquals(1L, v["a"])
        assertEquals(listOf(true, false, null, "x"), v["b"])
        assertEquals(-25.0, (v["c"] as Map<*, *>)["d"])
        assertEquals("", v["e"])
        assertEquals(listOf("a", "b", "c", "e"), v.keys.toList())          // order is kept
    }

    @Test fun numbersAreLongWhenWholeAndInRange() {
        assertEquals(0L, Json.parse("-0"))
        assertEquals(12345678901234L, Json.parse("12345678901234"))
        assertEquals(Long.MAX_VALUE, Json.parse("9223372036854775807"))
        assertEquals(Long.MIN_VALUE, Json.parse("-9223372036854775808"))
        assertEquals(9.223372036854775808E18, Json.parse("9223372036854775808"))   // one past the end becomes a double
        assertEquals(1000.0, Json.parse("1e3"))
        assertEquals(1.5, Json.parse("1.5"))
        assertEquals(-0.0125, Json.parse("-1.25E-2"))
    }

    @Test fun stringEscapes() {
        assertEquals("a\"b\\c/d\b\u000C\n\r\t", Json.parse("\"a\\\"b\\\\c\\/d\\b\\f\\n\\r\\t\""))
        assertEquals("\u00e9", Json.parse("\"\\u00e9\""))
        assertEquals("\ud83d\ude00", Json.parse("\"\\ud83d\\ude00\""))                // a surrogate pair survives
        assertEquals("caf\u00e9 \u4e2d", Json.parse("\"caf\u00e9 \u4e2d\""))             // raw non-ASCII is fine
    }

    @Test fun refusesWhatIsNotJson() {
        for (t in listOf("", " ", "{", "}", "[", "[1,]", "[,1]", "{\"a\":1,}", "{\"a\" 1}", "{\"a\":1 \"b\":2}", "{a:1}", "{'a':1}", "truex", "nul", "tru", "NaN", "Infinity",
            "01", "1.", ".5", "+1", "-", "1e", "1e+", "--1", "\"abc", "\"a\\x\"", "\"\\u12\"", "\"\\u12G4\"", "1 2", "{} {}", "[] x", "// c\n1", "/* c */ 1", "{\"a\":1,\"a\":2}", "\"a\nb\"", "\"a\tb\"")) bad(t)
    }

    @Test fun depthIsLimited() {
        assertTrue(Json.parse("[".repeat(16) + "]".repeat(16)) is List<*>)
        bad("[".repeat(17) + "]".repeat(17))
        bad("[".repeat(100000))                                              // a depth bomb must not overflow the stack
        bad("{\"a\":".repeat(100000))
    }

    @Test fun writesAndEscapes() {
        assertEquals("""{"a":1,"b":[true,null,"x\"y\n\u0001"],"c":2.5,"d":3}""", Json.write(linkedMapOf("a" to 1, "b" to listOf(true, null, "x\"y\n\u0001"), "c" to 2.5, "d" to 3.0)))
        assertEquals("[]", Json.write(emptyList<Any>()))
        assertEquals("{}", Json.write(emptyMap<String, Any>()))
        assertEquals("\"\u00e9\u4e2d\"", Json.write("\u00e9\u4e2d"))
        assertEquals("9007199254740993", Json.write(9007199254740993L))
    }

    @Test fun refusesToWriteWhatCannotBeJson() {
        for (v in listOf<Any?>(Double.NaN, Double.POSITIVE_INFINITY, mapOf(1 to 2), object {})) {
            try { Json.write(v); fail("should refuse: $v") } catch (_: IllegalArgumentException) {}
        }
        val cycle = ArrayList<Any?>(); cycle.add(cycle)
        try { Json.write(cycle); fail("a cycle must not loop forever") } catch (_: IllegalArgumentException) {}
    }

    @Test fun roundTrips() {
        val v = linkedMapOf<String, Any?>("id" to "j1", "n" to 123456789012L, "neg" to -5L, "f" to 0.25, "list" to listOf(1L, "two", null, listOf(true)), "o" to linkedMapOf<String, Any?>("k" to "v\u00e9\"\\"), "s" to "tab\there")
        assertEquals(v, Json.parse(Json.write(v)))
    }

    @Test fun typedReads() {
        val m = Json.parse("""{"s":"x","l":5,"d":6.0,"f":6.5,"b":true,"o":{"k":1},"a":[1],"big":99999999999}""") as Map<*, *>
        assertEquals("x", m.str("s")); assertNull(m.str("l")); assertNull(m.str("missing"))
        assertEquals(5L, m.long("l")); assertEquals(6L, m.long("d")); assertNull(m.long("f")); assertNull(m.long("s"))
        assertEquals(5, m.int("l")); assertNull(m.int("big"))
        assertEquals(true, m.bool("b")); assertNull(m.bool("s"))
        assertEquals(1L, m.obj("o")!!["k"]); assertNull(m.obj("a"))
        assertEquals(listOf(1L), m.list("a")); assertNull(m.list("o"))
    }
}
