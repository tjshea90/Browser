package app.kestrel.core.ui

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalStampTest {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.US)
    private fun reference(utcMs: Long, offsetMs: Int): String =
        fmt.format(Instant.ofEpochMilli(utcMs).atOffset(ZoneOffset.ofTotalSeconds(offsetMs / 1000)))

    @Test fun knownMoments() {
        assertEquals("1970-01-01 00:00", LocalStamp.format(0L, 0))
        assertEquals("2026-11-27 04:58", LocalStamp.format(1_795_755_520_000L, 0))                    // 2026-11-27T04:58:40Z, the upload that caught the Settings freeze
        assertEquals("2026-11-26 23:58", LocalStamp.format(1_795_755_520_000L, -5 * 3_600_000))       // the owner's evening: the day goes back with the zone
        assertEquals("2026-11-27 10:28", LocalStamp.format(1_795_755_520_000L, 5 * 3_600_000 + 30 * 60_000))   // a half-hour zone
        assertEquals("2024-02-29 12:00", LocalStamp.format(1_709_208_000_000L, 0))                    // a leap day
        assertEquals("2100-02-28 23:59", LocalStamp.format(4_107_542_340_000L, 0))                    // 2100 is not a leap year ...
        assertEquals("2100-03-01 00:00", LocalStamp.format(4_107_542_400_000L, 0))                    // ... so the next minute is March 1st
        assertEquals("2000-02-29 00:00", LocalStamp.format(951_782_400_000L, 0))                      // 2000 is
        assertEquals("1969-12-31 23:59", LocalStamp.format(-60_000L, 0))                              // before the epoch
    }

    @Test fun agreesWithTheJdkOnThousandsOfRandomMomentsAndZones() {
        val r = Random(2711)
        repeat(20_000) {
            val ms = (r.nextLong() % 4_102_444_800_000L)                       // +-130 years around the epoch
            val off = (r.nextInt(27 * 4) - 12 * 4) * 15 * 60_000               // -12:00 .. +14:45 in quarter hours
            assertEquals("ms=$ms offset=$off", reference(ms, off), LocalStamp.format(ms, off))
        }
    }

    @Test fun everyMinuteAroundTheEndOfAYearAndALeapDay() {
        var ms = 1_735_686_600_000L                                            // 2024-12-31T23:10:00Z
        repeat(120) { assertEquals(reference(ms, 0), LocalStamp.format(ms, 0)); ms += 60_000L }
        ms = 1_709_164_800_000L                                                // 2024-02-29T00:00:00Z
        repeat(24 * 60) { assertEquals(reference(ms, 3_600_000), LocalStamp.format(ms, 3_600_000)); ms += 60_000L }
    }
}
