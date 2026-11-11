package app.kestrel.core.health

import org.junit.Assert.assertEquals
import org.junit.Test

class RingLogTest {
    @Test fun keepsTheNewestLinesInOrder() {
        val r = RingLog(3)
        listOf("a", "b", "c", "d").forEach { r.add(it) }
        assertEquals(listOf("b", "c", "d"), r.last())
        assertEquals(listOf("c", "d"), r.last(2))
        assertEquals(3, r.size())
        assertEquals(4L, r.totalAdded())
    }

    @Test fun lessThanCapacityAndEmptyAndClear() {
        val r = RingLog(5)
        assertEquals(emptyList(), r.last())
        r.add("x"); r.add("y")
        assertEquals(listOf("x", "y"), r.last())
        assertEquals(listOf("x", "y"), r.last(99))
        r.clear()
        assertEquals(0, r.size())
        assertEquals(emptyList(), r.last())
        r.add("z")
        assertEquals(listOf("z"), r.last())
    }

    @Test fun capacityOneAndNegativeRequests() {
        val r = RingLog(0) // coerced to 1
        r.add("p"); r.add("q")
        assertEquals(listOf("q"), r.last())
        assertEquals(emptyList(), r.last(-4))
    }

    @Test fun survivesConcurrentWriters() {
        val r = RingLog(64)
        val threads = (1..4).map { t -> Thread { repeat(1000) { r.add("t$t-$it") } } }
        threads.forEach { it.start() }; threads.forEach { it.join() }
        assertEquals(64, r.size())
        assertEquals(4000L, r.totalAdded())
        assertEquals(64, r.last().size)
    }
}
