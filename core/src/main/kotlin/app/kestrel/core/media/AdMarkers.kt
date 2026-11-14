package app.kestrel.core.media

import java.util.Base64

/**
 * RX3-V5 (research/REVIEW-EXTERNAL-3.md part V, research/FINDER_LAB.md section 3): the markers a stream playlist uses to say "advertising from here to there" - `#EXT-X-CUE-OUT` (with a duration), `#EXT-X-CUE-OUT-CONT`,
 * `#EXT-X-CUE-IN`, `#EXT-OATCLS-SCTE35` (a base64 SCTE-35 splice_info_section) and `#EXT-X-DATERANGE` with SCTE35-OUT / SCTE35-IN. Pure, bounded, never throws: a marker that cannot be read says nothing.
 */
object AdMarkers {
    /** A break longer than this is taken for a mistake. */
    const val MAX_BREAK_MS = 3_600_000L

    /** What a splice_insert says: this is the start of a break ([out]) or the return to the program, and how long the break lasts ([durationMs], -1 when it does not say). */
    class Splice(val out: Boolean, val durationMs: Long)

    /** The duration of a `#EXT-X-CUE-OUT:` argument: "30", "30.5", "DURATION=30", "DURATION=15.0,CUE=..."; -1 when it names none (or an absurd one). */
    fun cueOutMs(arg: String): Long {
        val a = arg.trim()
        if (a.isEmpty()) return -1
        val v = if (a.contains('=')) Regex("""(?i)(?:^|,)\s*duration\s*=\s*"?([0-9.]+)""").find(a)?.groupValues?.get(1) else a.substringBefore(',').trim()
        return seconds(v)
    }

    /** The time left of a break from a `#EXT-X-CUE-OUT-CONT:` argument ("ElapsedTime=6.0,Duration=30.0" or "6/30"); -1 when it cannot be told, 0 when it is over. */
    fun cueOutContLeftMs(arg: String): Long {
        val a = arg.trim()
        val elapsed: String?; val total: String?
        if (a.contains('/') && !a.contains('=')) { elapsed = a.substringBefore('/'); total = a.substringAfter('/').substringBefore(',') }
        else {
            elapsed = Regex("""(?i)(?:^|,)\s*elapsedtime\s*=\s*"?([0-9.]+)""").find(a)?.groupValues?.get(1)
            total = Regex("""(?i)(?:^|,)\s*duration\s*=\s*"?([0-9.]+)""").find(a)?.groupValues?.get(1)
        }
        val e = seconds(elapsed); val t = seconds(total)
        if (e < 0 || t < 0) return -1
        return (t - e).coerceAtLeast(0)
    }

    private fun seconds(v: String?): Long {
        val d = v?.trim()?.toDoubleOrNull() ?: return -1
        if (d < 0 || d * 1000 > MAX_BREAK_MS) return -1
        return (d * 1000).toLong()
    }

    /**
     * A SCTE-35 splice_info_section carrying a splice_insert (command 5): out of network = a break begins, its break_duration (90 kHz ticks) when present; not out of network = back to the program. A cancelled
     * splice, any other command (a time_signal keeps its meaning in a descriptor that is not read here), a short or damaged payload: null. The checksum is not verified.
     */
    fun scte35(text: String): Splice? {
        val b = try { Base64.getDecoder().decode(text.trim()) } catch (e: IllegalArgumentException) { return null }
        if (b.size < 21 || (b[0].toInt() and 0xFF) != 0xFC) return null       // table_id of a splice_info_section
        if ((b[13].toInt() and 0xFF) != 0x05) return null                      // splice_command_type: splice_insert
        var p = 14 + 4                                                         // after splice_event_id
        if ((b[p++].toInt() and 0x80) != 0) return null                        // splice_event_cancel_indicator
        val f = b[p++].toInt() and 0xFF
        val out = f and 0x80 != 0; val program = f and 0x40 != 0; val hasDuration = f and 0x20 != 0; val immediate = f and 0x10 != 0
        if (!program) return Splice(out, -1)                                   // component splices: the break length is not read
        if (!immediate) { if (p >= b.size) return Splice(out, -1); p += if ((b[p].toInt() and 0x80) != 0) 5 else 1 } // splice_time()
        var ms = -1L
        if (hasDuration && p + 5 <= b.size) {
            var ticks = (b[p].toLong() and 1L) shl 32
            for (k in 1..4) ticks = ticks or ((b[p + k].toLong() and 0xFF) shl (8 * (4 - k)))
            ms = ticks / 90
            if (ms > MAX_BREAK_MS) ms = -1
        }
        return Splice(out, ms)
    }
}
