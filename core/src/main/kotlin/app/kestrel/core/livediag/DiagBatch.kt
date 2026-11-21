package app.kestrel.core.livediag

/** One upload: who it comes from (a random id made on the tablet, never a device or account id), what build, a snapshot of the Diagnostics text and the journal of events since the last upload. Already scrubbed. */
class DiagBatch(
    val installId: String,
    val appVersion: String,
    val versionCode: Int,
    val androidApi: Int,
    val model: String,
    val atUtc: String,
    val snapshot: List<String>,
    val events: List<String>,
    /** Lines that were left out (too many, or dropped because they still looked like a secret): said, so a gap is not a mystery. */
    val dropped: Int,
) {
    /** The file's text: plain JSON built by hand (no library), one line per array element. */
    fun toJson(): String {
        val sb = StringBuilder(2048)
        sb.append("{\n \"v\": 1,\n \"install\": ").append(q(installId)).append(",\n \"app\": ").append(q(appVersion)).append(",\n \"code\": ").append(versionCode)
            .append(",\n \"android\": ").append(androidApi).append(",\n \"model\": ").append(q(model)).append(",\n \"at\": ").append(q(atUtc)).append(",\n \"dropped\": ").append(dropped)
        sb.append(",\n \"snapshot\": ["); list(sb, snapshot); sb.append("],\n \"events\": ["); list(sb, events); sb.append("]\n}\n")
        return sb.toString()
    }

    private fun list(sb: StringBuilder, l: List<String>) { for ((i, x) in l.withIndex()) { sb.append(if (i == 0) "\n  " else ",\n  ").append(q(x)) }; if (l.isNotEmpty()) sb.append('\n').append(' ') }

    companion object {
        /** A JSON string literal: quotes, backslashes and control characters escaped, everything else as is (non-ASCII too: the file is UTF-8). */
        fun q(s: String): String {
            val sb = StringBuilder(s.length + 2).append('"')
            for (c in s) when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
            return sb.append('"').toString()
        }

        /** Where the file goes in the private repo: diag/<date>/<time>-<install>.json (one file per upload, never overwritten). */
        fun path(atUtc: String, installId: String): String {
            val date = atUtc.substringBefore('T').filter { it.isDigit() || it == '-' }.ifEmpty { "undated" }
            val time = atUtc.substringAfter('T', "").filter { it.isDigit() }.take(6).ifEmpty { "000000" }
            return "diag/$date/$time-${installId.filter { it.isLetterOrDigit() }.take(12)}.json"
        }
    }
}

/**
 * When the tablet may send. The rules keep it small and quiet on a 5 Mbit/s hotspot: at most [MAX_PER_DAY] uploads a day, none less than [MIN_GAP_MS] after the last (except one soon after a crash), nothing to
 * send = nothing sent, a rejected token or a missing repo is not retried for [BACKOFF_MS], and the payload is cut to the newest [MAX_EVENTS] events.
 */
object DiagSchedule {
    const val MIN_GAP_MS = 6L * 3600_000L
    const val CRASH_GAP_MS = 10L * 60_000L
    const val BACKOFF_MS = 24L * 3600_000L
    const val MAX_PER_DAY = 4
    const val MAX_EVENTS = 600
    const val MAX_SNAPSHOT = 400

    class State(val enabled: Boolean, val hasToken: Boolean, val lastSentAt: Long, val sentToday: Int, val lastFailAt: Long, val failsInRow: Int, val pendingEvents: Int, val crashedLastRun: Boolean)

    /** Why not, or null = send now. The words go to the Settings status line. */
    fun whyNot(s: State, now: Long): String? = when {
        !s.enabled -> "off"
        !s.hasToken -> "no token yet"
        s.pendingEvents == 0 && !s.crashedLastRun -> "nothing new to send"
        s.failsInRow > 0 && now - s.lastFailAt < BACKOFF_MS * minOf(s.failsInRow, 3) -> "waiting after a failed send"
        s.sentToday >= MAX_PER_DAY -> "enough for today"
        s.lastSentAt > 0 && now - s.lastSentAt < (if (s.crashedLastRun) CRASH_GAP_MS else MIN_GAP_MS) -> "sent recently"
        else -> null
    }

    /** The newest [max] elements (the journal is oldest first). */
    fun newest(lines: List<String>, max: Int): List<String> = if (lines.size <= max) lines else lines.subList(lines.size - max, lines.size)
}
