package app.kestrel.core.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CpuUseTest {
    // a real line from an Android 10 content process (name with a space and parentheses inside it)
    private val line = "4242 (Gecko (main) x) S 1234 1234 0 0 -1 4194624 52321 0 12 0 1500 700 0 0 20 0 80 0 98765 1234567 22222 18446744073709551615 1 1 0 0 0 0 0 0 0 0 0 0 17 2 0 0 0 0 0"

    @Test fun ticksAreUserPlusSystemTimeAfterTheLastParenthesis() {
        assertEquals(2200L, ProcStat.ticks(line))
        assertEquals("Gecko (main) x", ProcStat.comm(line))
        assertEquals(1500L + 700, ProcStat.ticks("1 (init) S 0 0 0 0 -1 0 0 0 0 0 1500 700 0 0 20 0 1 0 1 1 1 1"))
    }

    @Test fun textThatIsNotAStatLineGivesNothing() {
        assertNull(ProcStat.ticks("")); assertNull(ProcStat.ticks("garbage")); assertNull(ProcStat.ticks("1 (a) S 0 0"))
        assertNull(ProcStat.comm("no parentheses"))
        assertNull(ProcStat.ticks("1 (a)"))
    }

    @Test fun percentIsOfOneCore() {
        assertEquals("100 ticks in a second at 100 Hz = one whole core", 100.0, ProcStat.percent(100, 1000), 1e-9)
        assertEquals(50.0, ProcStat.percent(250, 5000), 1e-9)
        assertEquals("a process can use more than one core", 300.0, ProcStat.percent(1500, 5000), 1e-9)
        assertEquals(0.0, ProcStat.percent(10, 0), 0.0); assertEquals(0.0, ProcStat.percent(-5, 1000), 0.0)
    }

    @Test fun theSeriesKeepsTheLatestReadingsAndReportsMeanAndPeak() {
        val s = CpuSeries(keep = 4)
        assertNull(s.peak()); assertEquals(0.0, s.mean(0, 1000), 0.0)
        for (i in 1..6) s.add(i * 1000L, i * 10.0)       // keeps 30, 40, 50, 60 (at 3 s .. 6 s)
        assertEquals(4, s.size)
        assertEquals(60.0, s.peak()!!.second, 0.0); assertEquals(6000L, s.peak()!!.first)
        assertEquals("the last two readings only", 55.0, s.mean(6000, 1000), 1e-9)
        assertEquals(45.0, s.mean(6000, 10_000), 1e-9)
    }
}
