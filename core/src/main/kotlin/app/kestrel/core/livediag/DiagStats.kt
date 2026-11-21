package app.kestrel.core.livediag

/** One site's page loads since the last upload: how many, how many failed, the median and the worst time to "done", the average time to first paint. The site is a host name, or "<sensitive-site>" (never named). */
class SiteStat(
    val site: String, val loads: Int, val failed: Int, val medianDoneMs: Long, val worstDoneMs: Long, val avgPaintMs: Long,
    /** LD4 (from the page's own timing, see `page timing` lines): how many loads had one, the average time to the first byte and to the first paint, the share of the load during which the page was too busy to answer (event-loop lateness; 0..100) and the average number of requests. -1 = no data. */
    val timed: Int = 0, val avgTtfbMs: Long = -1, val avgFcpMs: Long = -1, val lagPct: Int = -1, val avgReqs: Int = -1,
)

/**
 * LD2a: what the journal's lines add up to, kept in small bounded tables so a report is complete even when the journal itself lost old lines. Two tables:
 *  - the HISTOGRAM: how often each kind of event ("gecko onKill", "tabs hibernate", ...) happened and the longest time any of them mentioned;
 *  - the SITES: the finished page loads ("page loaded host=... paint=..ms done=..ms ... ok=..") per site.
 * Only code-made names, numbers and host names go in (a sensitive host is counted under "<sensitive-site>"); no text of a page, a title or an address. Saved as plain lines and cleared after a successful send or when the
 * owner turns the feature off.
 */
class DiagStats(private val maxKinds: Int = 150, private val maxSites: Int = 60) {
    private class Kind { var n = 0L; var maxMs = 0L }
    private class Site {
        var n = 0; var failed = 0; var worst = 0L; var paintSum = 0L; var paintN = 0
        var tn = 0; var ttfbSum = 0L; var ttfbN = 0; var fcpSum = 0L; var fcpN = 0; var lagSum = 0L; var lagLoadSum = 0L; var rnSum = 0L
        val recent = LongArray(RECENT); var filled = 0
        fun add(done: Long) { recent[filled % RECENT] = done; filled++ }
        fun median(): Long { val c = minOf(filled, RECENT); if (c == 0) return 0L; val a = recent.copyOf(c); a.sort(); return a[c / 2] }
    }

    private val kinds = LinkedHashMap<String, Kind>()
    private val sites = LinkedHashMap<String, Site>()
    private var other = 0L

    /** Called for every event the whitelist let through. [line] is the whole Health line ("+12.3s layer event detail"). */
    @Synchronized fun observe(layer: String, event: String, line: String) {
        if (!LAYER.matches(layer) || !EVENT.matches(event)) return
        val key = "$layer $event"
        var k = kinds[key]
        if (k == null && kinds.size < maxKinds) { k = Kind(); kinds[key] = k }
        if (k == null) other++                               // the table is full: counted, not named (the site table below does not depend on it)
        else {
            k.n++
            MS.find(line.substringAfter(event, ""))?.let { m -> val v = m.groupValues[1].toLongOrNull() ?: 0L; if (v > k.maxMs) k.maxMs = v }
        }
        if (layer == "page" && event == "loaded") loaded(line)
        else if (layer == "page" && event == "timing") timed(line)
    }

    /** LD4: "page timing host=... ttfb=820 fcp=14100 load=28000 rn=212 lag=2400/11000/60 ... hid=0": the numbers of one load go to its site. A load the person looked away from (hid=1) says nothing about speed and is left out. */
    private fun timed(line: String) {
        val host = (TIMING_HOST.find(line)?.groupValues?.get(1) ?: return).lowercase()
        if (TIMING_HID.find(line)?.groupValues?.get(1) == "1") return
        val site = if (!HOST.matches(host)) "<other>" else if (SensitiveSites.isSensitive(host)) "<sensitive-site>" else host
        var s = sites[site]
        if (s == null) { if (sites.size >= maxSites) return; s = Site(); sites[site] = s }
        s.tn++
        TIMING_TTFB.find(line)?.groupValues?.get(1)?.toLongOrNull()?.let { if (it >= 0) { s.ttfbSum += it; s.ttfbN++ } }
        TIMING_FCP.find(line)?.groupValues?.get(1)?.toLongOrNull()?.let { if (it >= 0) { s.fcpSum += it; s.fcpN++ } }
        val load = TIMING_LOAD.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: -1L
        val lag = TIMING_LAG.find(line)?.groupValues?.get(2)?.toLongOrNull() ?: -1L
        if (load > 0 && lag >= 0) { s.lagSum += minOf(lag, load); s.lagLoadSum += load }
        TIMING_RN.find(line)?.groupValues?.get(1)?.toLongOrNull()?.let { s.rnSum += it }
    }

    private fun loaded(line: String) {
        val m = LOADED.find(line) ?: return
        val host = m.groupValues[1].lowercase()
        val site = if (!HOST.matches(host)) "<other>" else if (SensitiveSites.isSensitive(host)) "<sensitive-site>" else host
        var s = sites[site]
        if (s == null) { if (sites.size >= maxSites) return; s = Site(); sites[site] = s }
        val paint = m.groupValues[2].toLong(); val done = m.groupValues[3].toLong(); val ok = m.groupValues[4] == "true"
        s.n++
        if (!ok) s.failed++
        if (done >= 0) { s.add(done); if (done > s.worst) s.worst = done }
        if (paint >= 0) { s.paintSum += paint; s.paintN++ }
    }

    @Synchronized fun count(key: String): Long = kinds[key]?.n ?: 0L
    @Synchronized fun kindCounts(): Map<String, Long> { val o = LinkedHashMap<String, Long>(); for ((k, v) in kinds) o[k] = v.n; if (other > 0) o["other kinds"] = other; return o }
    @Synchronized fun kindMaxMs(): Map<String, Long> { val o = LinkedHashMap<String, Long>(); for ((k, v) in kinds) if (v.maxMs > 0) o[k] = v.maxMs; return o }
    @Synchronized fun sites(): List<SiteStat> = sites.entries.map { (k, s) ->
        SiteStat(k, s.n, s.failed, s.median(), s.worst, if (s.paintN > 0) s.paintSum / s.paintN else -1L,
            s.tn, if (s.ttfbN > 0) s.ttfbSum / s.ttfbN else -1L, if (s.fcpN > 0) s.fcpSum / s.fcpN else -1L,
            if (s.lagLoadSum > 0) (s.lagSum * 100 / s.lagLoadSum).toInt().coerceIn(0, 100) else -1, if (s.tn > 0) (s.rnSum / s.tn).toInt() else -1)
    }.sortedByDescending { maxOf(it.loads, it.timed) }
    @Synchronized fun isEmpty(): Boolean = kinds.isEmpty() && sites.isEmpty()
    @Synchronized fun reset() { kinds.clear(); sites.clear(); other = 0L }

    /**
     * LD3: add another table's numbers to this one (counts add, the worst time is the larger, the recent load times are appended). The tablet keeps a ROLLING WEEK this way: the table of one upload is cleared after
     * every send (the file carries only what is new), but the advisor needs more than three page loads to say "this site is slow", so it reads the week = the past weeks' total + the current table.
     */
    @Synchronized fun mergeFrom(o: DiagStats) {
        if (o === this) return
        synchronized(o) {
            for ((key, v) in o.kinds) {
                var k = kinds[key]
                if (k == null && kinds.size < maxKinds) { k = Kind(); kinds[key] = k }
                if (k == null) other += v.n else { k.n += v.n; if (v.maxMs > k.maxMs) k.maxMs = v.maxMs }
            }
            other += o.other
            for ((key, x) in o.sites) {
                var s = sites[key]
                if (s == null) { if (sites.size >= maxSites) continue; s = Site(); sites[key] = s }
                s.n += x.n; s.failed += x.failed; if (x.worst > s.worst) s.worst = x.worst; s.paintSum += x.paintSum; s.paintN += x.paintN
                s.tn += x.tn; s.ttfbSum += x.ttfbSum; s.ttfbN += x.ttfbN; s.fcpSum += x.fcpSum; s.fcpN += x.fcpN; s.lagSum += x.lagSum; s.lagLoadSum += x.lagLoadSum; s.rnSum += x.rnSum
                val c = minOf(x.filled, RECENT)
                val from = if (x.filled > RECENT) x.filled % RECENT else 0           // oldest first
                for (i in 0 until c) s.add(x.recent[(from + i) % RECENT])
            }
        }
    }

    /** Plain lines, tab separated: K kind n maxMs / S site n failed worst paintSum paintN recent,recent / O other. */
    @Synchronized fun save(): String {
        val sb = StringBuilder()
        for ((k, v) in kinds) sb.append("K\t").append(k).append('\t').append(v.n).append('\t').append(v.maxMs).append('\n')
        for ((k, s) in sites) { sb.append("S\t").append(k).append('\t').append(s.n).append('\t').append(s.failed).append('\t').append(s.worst).append('\t').append(s.paintSum).append('\t').append(s.paintN).append('\t')
            val c = minOf(s.filled, RECENT); for (i in 0 until c) { if (i > 0) sb.append(','); sb.append(s.recent[i]) }
            sb.append('\t').append(s.tn).append('\t').append(s.ttfbSum).append('\t').append(s.ttfbN).append('\t').append(s.fcpSum).append('\t').append(s.fcpN).append('\t').append(s.lagSum).append('\t').append(s.lagLoadSum).append('\t').append(s.rnSum).append('\n') }
        if (other > 0) sb.append("O\t").append(other).append('\n')
        return sb.toString()
    }

    /** Reads what [save] wrote; a damaged line is skipped. Replaces the tables. */
    @Synchronized fun load(text: String) {
        reset()
        for (line in text.lineSequence()) {
            val p = line.split('\t')
            try {
                when (p.firstOrNull()) {
                    "K" -> if (p.size >= 4 && kinds.size < maxKinds) { val k = Kind(); k.n = p[2].toLong(); k.maxMs = p[3].toLong(); kinds[p[1]] = k }
                    "S" -> if (p.size >= 8 && sites.size < maxSites) {
                        val s = Site(); s.n = p[2].toInt(); s.failed = p[3].toInt(); s.worst = p[4].toLong(); s.paintSum = p[5].toLong(); s.paintN = p[6].toInt()
                        for (r in p[7].split(',')) r.toLongOrNull()?.let { s.add(it) }
                        if (p.size >= 16) { s.tn = p[8].toInt(); s.ttfbSum = p[9].toLong(); s.ttfbN = p[10].toInt(); s.fcpSum = p[11].toLong(); s.fcpN = p[12].toInt(); s.lagSum = p[13].toLong(); s.lagLoadSum = p[14].toLong(); s.rnSum = p[15].toLong() }
                        sites[p[1]] = s
                    }
                    "O" -> if (p.size >= 2) other = p[1].toLong()
                }
            } catch (e: NumberFormatException) { /* skip the damaged line */ }
        }
    }

    companion object {
        private const val RECENT = 24
        private val LAYER = Regex("[a-z0-9._-]{1,32}")
        private val EVENT = Regex("[A-Za-z0-9._-]{1,40}")
        private val HOST = Regex("[a-z0-9.-]{1,80}")
        private val MS = Regex("""(\d{1,7})\s?ms\b""")
        private val TIMING_HOST = Regex("""host=(\S+)""")
        private val TIMING_HID = Regex("""\bhid=(\d)""")
        private val TIMING_TTFB = Regex("""\bttfb=(-?\d+)""")
        private val TIMING_FCP = Regex("""\bfcp=(-?\d+)""")
        private val TIMING_LOAD = Regex("""\bload=(-?\d+)""")
        private val TIMING_LAG = Regex("""\blag=(-?\d+)/(-?\d+)/(-?\d+)""")
        private val TIMING_RN = Regex("""\brn=(\d+)""")
        private val LOADED = Regex("""host=(\S+) paint=(-?\d+)ms done=(-?\d+)ms blocked=\d+ visit=\d+ ok=(true|false)""")
    }
}
