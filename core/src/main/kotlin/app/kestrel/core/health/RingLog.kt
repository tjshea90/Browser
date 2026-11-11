package app.kestrel.core.health

/**
 * Fixed-size, thread-safe ring buffer of timeline lines such as "+12.345s ui STALL 2100ms". The freeze watchdog, the lifecycle hooks and the engine
 * delegates write here; the Diagnostics screen reads it so a report from the tablet carries evidence (research/FREEZE.md 5.3).
 * Memory is bounded by construction: old lines are overwritten.
 */
class RingLog(capacity: Int = 512) {
    private val cap = capacity.coerceAtLeast(1)
    private val lines = arrayOfNulls<String>(cap)
    private var next = 0
    private var count = 0
    private var total = 0L

    @Synchronized
    fun add(line: String) {
        lines[next] = line
        next = (next + 1) % cap
        if (count < cap) count++
        total++
    }

    /** The last [n] lines, oldest first. */
    @Synchronized
    fun last(n: Int = cap): List<String> {
        val m = n.coerceIn(0, count)
        val out = ArrayList<String>(m)
        var i = (next - m + cap) % cap
        repeat(m) { out.add(lines[i] ?: ""); i = (i + 1) % cap }
        return out
    }

    /** Lines currently held. */
    @Synchronized
    fun size(): Int = count

    /** Lines ever added, including those the ring has since overwritten. */
    @Synchronized
    fun totalAdded(): Long = total

    @Synchronized
    fun clear() {
        java.util.Arrays.fill(lines, null)
        next = 0
        count = 0
    }
}
