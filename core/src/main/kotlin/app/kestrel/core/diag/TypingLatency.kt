package app.kestrel.core.diag

/**
 * PERF3c (owner 2026-11-22: "typing is very laggy"): how long a typed character waits before the screen shows it, as numbers: keys counted, the mean and the worst wait in milliseconds, and how many waited 100 ms or more.
 * Used for the address bar and the Home field (the wait from the text change to the next frame the app's UI thread starts). No text, no address: only the numbers. [take] returns the one-line account and starts again.
 */
class TypingLatency {
    private var n = 0; private var sum = 0L; private var worst = 0L; private var over = 0

    @Synchronized fun add(ms: Long) { val v = ms.coerceIn(0L, 60_000L); n++; sum += v; if (v > worst) worst = v; if (v >= 100) over++ }

    /** "keys=12 avg=35ms worst=410ms over100=2" and a fresh start, or null when no key was counted. */
    @Synchronized fun take(): String? {
        if (n == 0) return null
        val s = "keys=$n avg=${sum / n}ms worst=${worst}ms over100=$over"
        n = 0; sum = 0; worst = 0; over = 0
        return s
    }
}
