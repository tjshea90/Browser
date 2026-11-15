package app.kestrel.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekStatsTest {
    @Test fun saysSoWhenNothingWasSeen() {
        assertEquals("Tap on a video bar: none seen yet", SeekStats().line())
    }

    @Test fun addsDifferencesAndNamesThePartiesPlainly() {
        val s = SeekStats()
        s.add(2, 1, 1, 0, "")
        s.add(1, 0, 0, 1, "live")
        assertEquals(3, s.taps)
        assertEquals("Tap on a video bar: 3 taps, the page jumped by itself 1, Kestrel jumped 1, left alone 1; last reason: live", s.line())
    }

    @Test fun oneTapIsSingular() {
        val s = SeekStats(); s.add(1, 0, 1, 0, "")
        assertEquals("Tap on a video bar: 1 tap, the page jumped by itself 0, Kestrel jumped 1", s.line())
    }

    @Test fun hugeOrNegativeNumbersAndOddTextAreClamped() {
        val s = SeekStats()
        s.add(1_000_000, -5, 5000, 0, "n\u00e9o http://secret.example/x".repeat(3))
        assertEquals(999, s.taps); assertEquals(0, s.page); assertEquals(999, s.ours)
        assertTrue(s.line().length < 200)
        assertTrue(s.line().all { it.code in 32..126 })
    }

    @Test fun resetForgetsEverything() {
        val s = SeekStats(); s.add(4, 1, 1, 2, "ad"); s.reset()
        assertEquals("Tap on a video bar: none seen yet", s.line())
    }
}
