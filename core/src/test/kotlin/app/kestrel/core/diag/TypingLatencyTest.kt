package app.kestrel.core.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** PERF3c (owner 2026-11-22: "typing is very laggy"): the account of how long typed characters waited. */
class TypingLatencyTest {
    @Test fun theAccountSaysHowManyKeysTheMeanTheWorstAndHowManyWaitedAtLeast100Ms() {
        val t = TypingLatency()
        assertNull("no key, no line", t.take())
        for (ms in listOf(10L, 20L, 410L, 100L, 10L)) t.add(ms)
        assertEquals("keys=5 avg=110ms worst=410ms over100=2", t.take())
        assertNull("taking starts again", t.take())
    }

    @Test fun aWildNumberIsClampedAndNegativeIsZero() {
        val t = TypingLatency(); t.add(-5); t.add(10_000_000)
        assertEquals("keys=2 avg=30000ms worst=60000ms over100=1", t.take())
    }
}
