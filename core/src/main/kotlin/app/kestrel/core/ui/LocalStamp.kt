package app.kestrel.core.ui

/**
 * ND1 (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"): the newest upload caught the UI thread blocked for 2.4 s while Settings opened, and the first
 * sample of that stall was `DateFormat.getDateTimeInstance()` (830 ms: it loads the phone's locale patterns from disk the first time it is called in a run) inside the "Last successful check" line
 * of the filter-list status. A fixed "2026-11-27 04:58" written by plain arithmetic needs no locale data at all: it costs microseconds on the first call and on every later one, and the same
 * code is a JVM test (the Android parts only pass the offset of the phone's time zone).
 */
object LocalStamp {
    /** "yyyy-MM-dd HH:mm" for [utcMs] shown in a zone that is [offsetMs] milliseconds ahead of UTC (the zone's offset AT that moment, as TimeZone.getOffset(utcMs) gives it). */
    fun format(utcMs: Long, offsetMs: Int): String {
        val local = utcMs + offsetMs
        val days = Math.floorDiv(local, DAY_MS)
        val msOfDay = Math.floorMod(local, DAY_MS)
        // civil date from the day count (Howard Hinnant's algorithm, proleptic Gregorian calendar, days since 1970-01-01)
        val z = days + 719_468L
        val era = Math.floorDiv(z, 146_097L)
        val doe = z - era * 146_097L                                              // [0, 146096]
        val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L    // [0, 399]
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)                       // [0, 365]
        val mp = (5L * doy + 2L) / 153L                                            // [0, 11], March first
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = if (mp < 10L) mp + 3L else mp - 9L
        val y = yoe + era * 400L + (if (m <= 2L) 1L else 0L)
        val hh = msOfDay / HOUR_MS
        val mm = (msOfDay % HOUR_MS) / MINUTE_MS
        val sb = StringBuilder(16)
        if (y < 0L) sb.append('-')
        pad(sb, Math.abs(y), 4).append('-'); pad(sb, m, 2).append('-'); pad(sb, d, 2).append(' ')
        pad(sb, hh, 2).append(':'); pad(sb, mm, 2)
        return sb.toString()
    }

    private fun pad(sb: StringBuilder, v: Long, width: Int): StringBuilder {
        val s = v.toString()
        for (i in s.length until width) sb.append('0')
        return sb.append(s)
    }

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L
}
