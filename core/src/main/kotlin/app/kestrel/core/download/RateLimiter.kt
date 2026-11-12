package app.kestrel.core.download

/**
 * One shared speed limit for every download of the app (Settings > Downloads > "Speed limit"): a hotspot has one radio, so a big download that takes all of it makes browsing crawl. Readers call [acquire]
 * after each read from the network; when the limit is on they are made to wait, which also slows the sender down (the socket buffer fills and TCP's flow control does the rest), so no byte is ever
 * dropped or fetched twice. Thread-safe, cheap when the limit is off (one volatile read). [sleeper] and [clock] are injectable so the arithmetic is tested without real waiting.
 */
class RateLimiter(
    private val clock: Clock = Clock.System,
    private val sleeper: (Long) -> Unit = { ms -> try { Thread.sleep(ms) } catch (e: InterruptedException) { Thread.currentThread().interrupt() } },
) {
    /** Bytes per second over all downloads together; 0 = no limit. */
    @Volatile var bytesPerSec: Long = 0

    private val lock = Any()
    private var next = 0L // the clock time at which the bytes handed out so far have "used up" the allowed rate

    /** Waits (in slices of at most 50 ms, so a pause or cancel is noticed quickly) until [n] more bytes fit the limit. [stop] is polled between slices. */
    fun acquire(n: Int, stop: () -> Boolean = { false }) {
        val rate = bytesPerSec
        if (rate <= 0 || n <= 0) return
        var wait: Long
        synchronized(lock) {
            val now = clock.now()
            if (next < now) next = now                  // idle time is not saved up: a limit means "at most", not "on average over the day"
            next += n * 1000L / rate
            wait = next - now - BURST_MS                // a short burst (a fifth of a second of data) passes at once, which keeps reads smooth
        }
        while (wait > 0 && !stop()) {
            val slice = minOf(wait, 50L)
            sleeper(slice)
            wait -= slice
        }
    }

    private companion object { const val BURST_MS = 200L }
}
