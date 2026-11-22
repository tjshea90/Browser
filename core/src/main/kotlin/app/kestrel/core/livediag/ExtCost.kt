package app.kestrel.core.livediag

/**
 * LD4: what the built-in extension costs, as plain numbers for the live diagnostics. The extension counts its five web-request listeners ("req 2425 (3948000 us), rcv 711 (303000 us), snd 710 (102000 us),
 * done 1637 (197000 us), err 341 (62000 us)": calls and the microseconds they held the engine's main thread) and the blocker's own judging (requests judged, their total and worst microseconds, how many took over 2 ms).
 * The Diagnostics screen shows them as text; the real uploads of 2026-11-21 had them only in a line cut at 400 characters. Here they become numbers an advisor can use (the cost of one request x the requests of a page).
 */
object ExtCost {
    private val ONE = Regex("""(req|rcv|snd|done|err)\s+(\d+)\s+\((\d+)\s*us\)""")

    fun numbers(listeners: String, checks: Long, totalUs: Long, maxUs: Long, over2ms: Long): Map<String, Long> {
        val out = LinkedHashMap<String, Long>()
        for (m in ONE.findAll(listeners)) {
            val k = m.groupValues[1].replaceFirstChar { it.uppercase() }
            out["ext${k}Calls"] = m.groupValues[2].toLongOrNull() ?: 0L
            out["ext${k}Us"] = m.groupValues[3].toLongOrNull() ?: 0L
        }
        if (checks > 0) {
            out["judged"] = checks; out["judgeAvgUs"] = totalUs / checks; out["judgeMaxUs"] = maxUs; out["judgeOver2ms"] = over2ms
        }
        return out
    }
}
