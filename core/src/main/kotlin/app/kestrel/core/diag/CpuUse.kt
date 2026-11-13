package app.kestrel.core.diag

/**
 * Reading the kernel's CPU counters (AO1, owner message 29: "the video player and web page animations ... are still slow and laggy"). The question a lag report needs answered is "which process, and which
 * thread, was working flat out?": a page whose content process sits near 100 % of a core is limited by its own JavaScript or layout (one thread cannot use the other three cores), a lag while every
 * process is low is the network or the player. Pure text parsing and arithmetic here; the Android side only reads the files.
 */
object ProcStat {
    /** `utime + stime` in clock ticks from the text of `/proc/<pid>/stat` or `/proc/<pid>/task/<tid>/stat`, null when the text is not a stat line. */
    fun ticks(stat: String): Long? {
        val rest = afterName(stat) ?: return null
        val f = rest.split(' ')
        // fields after the name start at field 3 (state): utime is field 14, stime field 15
        val ut = f.getOrNull(11)?.toLongOrNull() ?: return null
        val st = f.getOrNull(12)?.toLongOrNull() ?: return null
        return ut + st
    }

    /** The thread or process name: the text between the first "(" and the LAST ")" (it may hold spaces and parentheses), null when absent. */
    fun comm(stat: String): String? {
        val a = stat.indexOf('('); val b = stat.lastIndexOf(')')
        return if (a >= 0 && b > a) stat.substring(a + 1, b) else null
    }

    private fun afterName(stat: String): String? {
        val b = stat.lastIndexOf(')')
        return if (b < 0 || b + 2 > stat.length) null else stat.substring(b + 2).trim()
    }

    /** Percent of ONE core used over [dtMs] by a counter that advanced [deltaTicks] ticks at [clkTck] ticks a second. */
    fun percent(deltaTicks: Long, dtMs: Long, clkTck: Int = 100): Double =
        if (dtMs <= 0 || clkTck <= 0 || deltaTicks < 0) 0.0 else deltaTicks * 1000.0 * 100.0 / (clkTck.toDouble() * dtMs)
}

/** The last [keep] readings of one process: (time, percent of one core). */
class CpuSeries(private val keep: Int = 60) {
    private val at = ArrayList<Long>(); private val pct = ArrayList<Double>()

    fun add(atMs: Long, percent: Double) {
        at.add(atMs); pct.add(percent)
        while (at.size > keep) { at.removeAt(0); pct.removeAt(0) }
    }

    val size: Int get() = at.size

    /** The mean of the readings taken in the last [windowMs] before [nowMs]; 0 when there are none. */
    fun mean(nowMs: Long, windowMs: Long): Double {
        var sum = 0.0; var n = 0
        for (i in at.indices) if (nowMs - at[i] <= windowMs) { sum += pct[i]; n++ }
        return if (n == 0) 0.0 else sum / n
    }

    /** The highest reading kept, with when it was taken; null when empty. */
    fun peak(): Pair<Long, Double>? {
        var best = -1
        for (i in pct.indices) if (best < 0 || pct[i] > pct[best]) best = i
        return if (best < 0) null else at[best] to pct[best]
    }
}

/** The Diagnostics sentence about CPU use, built from numbers the Android side collected (kept here so it can be tested without a device). */
object CpuReport {
    class Row(val name: String, val avg: Int, val peak: Int)
    /** A reading above the busy mark: how long ago, which process, how busy, and the busiest threads in it. */
    class Moment(val ageMs: Long, val who: String, val pct: Int, val threads: String)

    fun line(cores: Int, everySec: Int, busyPct: Int, rows: List<Row>, moments: List<Moment>): String {
        val head = "CPU by process, % of ONE core (this tablet has $cores; read every $everySec s while the browser is on screen, last 5 min): "
        if (rows.isEmpty()) return head + "no reading yet"
        val body = rows.joinToString("; ") { "${it.name} avg ${it.avg} / peak ${it.peak}" }
        val busy = if (moments.isEmpty()) "no process over $busyPct % yet" else moments.joinToString("; ") { "${it.ageMs / 60_000} min ${(it.ageMs / 1000) % 60} s ago ${it.who} ${it.pct} % [${it.threads}]" }
        return "$head$body | busy moments: $busy | reading: a page that lags while 'tab' sits near 100 is limited by its own script or layout (one thread cannot use the other cores); a lag while every process is low is the network or the player"
    }
}
