package app.kestrel.core.livediag

/** One site's page loads since the last upload: how many, how many failed, the median and the worst time to "done", the average time to first paint. The site is a host name, or "<sensitive-site>" (never named). */
class SiteStat(val site: String, val loads: Int, val failed: Int, val medianDoneMs: Long, val worstDoneMs: Long, val avgPaintMs: Long)

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
    @Synchronized fun sites(): List<SiteStat> = sites.entries.map { (k, s) -> SiteStat(k, s.n, s.failed, s.median(), s.worst, if (s.paintN > 0) s.paintSum / s.paintN else -1L) }.sortedByDescending { it.loads }
    @Synchronized fun isEmpty(): Boolean = kinds.isEmpty() && sites.isEmpty()
    @Synchronized fun reset() { kinds.clear(); sites.clear(); other = 0L }

    /** Plain lines, tab separated: K kind n maxMs / S site n failed worst paintSum paintN recent,recent / O other. */
    @Synchronized fun save(): String {
        val sb = StringBuilder()
        for ((k, v) in kinds) sb.append("K\t").append(k).append('\t').append(v.n).append('\t').append(v.maxMs).append('\n')
        for ((k, s) in sites) { sb.append("S\t").append(k).append('\t').append(s.n).append('\t').append(s.failed).append('\t').append(s.worst).append('\t').append(s.paintSum).append('\t').append(s.paintN).append('\t')
            val c = minOf(s.filled, RECENT); for (i in 0 until c) { if (i > 0) sb.append(','); sb.append(s.recent[i]) }; sb.append('\n') }
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
        private val LOADED = Regex("""host=(\S+) paint=(-?\d+)ms done=(-?\d+)ms blocked=\d+ visit=\d+ ok=(true|false)""")
    }
}
