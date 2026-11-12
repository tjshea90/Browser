package app.kestrel.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {
    /** A clock the "sleeper" advances: the waiting is simulated, so the arithmetic is exact and instant. */
    private class Sim : Clock { var t = 1_000_000L; var slept = 0L; override fun now() = t }
    private fun limiter(sim: Sim) = RateLimiter(sim) { ms -> sim.t += ms; sim.slept += ms }

    @Test fun noLimitNeverWaits() {
        val sim = Sim(); val l = limiter(sim)
        repeat(100) { l.acquire(1 shl 20) }
        assertEquals(0L, sim.slept)
    }

    @Test fun aLimitSpreadsTheBytesOverTime() {
        val sim = Sim(); val l = limiter(sim).also { it.bytesPerSec = 1_000_000 }
        repeat(100) { l.acquire(50_000) }                    // 5 MB at 1 MB/s
        val elapsed = sim.slept
        assertTrue("took about 5 s (minus the 200 ms burst), was $elapsed ms", elapsed in 4_700L..5_050L)
    }

    @Test fun aSmallBurstPassesAtOnce() {
        val sim = Sim(); val l = limiter(sim).also { it.bytesPerSec = 1_000_000 }
        l.acquire(150_000)                                   // 150 ms worth of data: inside the burst
        assertEquals(0L, sim.slept)
    }

    @Test fun idleTimeIsNotSavedUp() {
        val sim = Sim(); val l = limiter(sim).also { it.bytesPerSec = 1_000_000 }
        l.acquire(100_000)
        sim.t += 60_000                                      // a minute of silence
        val before = sim.slept
        repeat(20) { l.acquire(100_000) }                    // 2 MB right after: still limited to 1 MB/s
        assertTrue("the minute of idling did not buy a free burst: ${sim.slept - before}", sim.slept - before >= 1_700)
    }

    @Test fun theLimitCanChangeAtAnyTimeAndStopEndsTheWait() {
        val sim = Sim(); val l = limiter(sim)
        l.bytesPerSec = 100_000
        var stop = false
        l.acquire(1_000_000) { stop }                        // would wait 10 s
        assertTrue(sim.slept in 9_700L..10_050L)
        val before = sim.slept
        stop = true
        l.acquire(1_000_000) { stop }
        assertEquals("a stop request ends the wait at once", before, sim.slept)
        l.bytesPerSec = 0
        l.acquire(1_000_000)
        assertEquals(before, sim.slept)
    }
}
