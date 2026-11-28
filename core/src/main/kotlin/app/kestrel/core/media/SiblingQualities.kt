package app.kestrel.core.media

/**
 * PERF5b (owner 2026-11-22: "with the kestrel player running it there is no video quality selection at all"): some sites (pornhub) give the page ONE HLS master list PER quality, each with a single picture, and the
 * player of the page switches quality by loading another list. Kestrel's own player reads the list it was given, sees one picture and offers no choice. The finder already knows the other lists of the same page and what
 * each offers (the early look read them). This is the pure rule that decides which of them are the SAME film in another quality: the same length (within [LENGTH_SLACK_MS], a few seconds for the differing last segment), each
 * offering exactly ONE picture size (a list with several rungs chooses by itself and is no sibling), each a different size. Pure so it is tested without a phone.
 */
object SiblingQualities {
    const val LENGTH_SLACK_MS = 3_000L
    const val MAX = 6

    /** One list the finder knows: its address, its length (ms, 0 = not known), the pictures it offers as (width, height) and when the page last asked for it. */
    class Known(val url: String, val durationMs: Long, val sizes: List<Pair<Int, Int>>, val seenAt: Long = 0L)

    /** One quality of the film: a width, a height and the list that plays it. */
    class Alt(val url: String, val w: Int, val h: Int)

    /**
     * The other qualities of the film [chosen] is, biggest picture first, [chosen] itself included, or an empty list when there is nothing to choose between (fewer than two sizes). Only lists of the same film are used:
     * the length is known on both sides and within the slack. A list that offers more than one picture is the film's own ABR list and is never a sibling.
     */
    fun alternates(chosen: Known, all: List<Known>): List<Alt> {
        val size = chosen.sizes.singleOrNull() ?: return emptyList()
        if (chosen.durationMs <= 0L) return emptyList()
        val byHeight = LinkedHashMap<Int, Alt>()
        byHeight[longSide(size)] = Alt(chosen.url, size.first, size.second)
        for (k in all.sortedByDescending { it.seenAt }) {
            if (k.url == chosen.url) continue
            val s = k.sizes.singleOrNull() ?: continue
            if (k.durationMs <= 0L || Math.abs(k.durationMs - chosen.durationMs) > LENGTH_SLACK_MS) continue
            val key = longSide(s)
            if (!byHeight.containsKey(key)) byHeight[key] = Alt(k.url, s.first, s.second)
            if (byHeight.size >= MAX) break
        }
        if (byHeight.size < 2) return emptyList()
        return byHeight.values.sortedByDescending { longSide(it.w to it.h) }
    }

    /**
     * PERF5b: the quality to START with when a film has several lists: the biggest whose picture class (the shorter side) is at most [capH] (the tablet's smooth size: 720 in the full-screen screen, 480 in a small
     * box, or what the person picked last time); when every list is bigger than that, the smallest one; null when there is nothing to choose between. [alts] is the list [alternates] made (biggest first).
     */
    fun startFor(alts: List<Alt>, capH: Int): Alt? {
        if (alts.size < 2) return null
        val fit = alts.filter { QualityLabel.pClass(it.w, it.h) <= capH }
        return fit.maxByOrNull { QualityLabel.pClass(it.w, it.h) } ?: alts.minByOrNull { QualityLabel.pClass(it.w, it.h) }
    }

    private fun longSide(s: Pair<Int, Int>): Int = maxOf(s.first, s.second)

    /** The extras of the player: one line per quality "w|h|url" (addresses cannot contain a line break). */
    fun encode(alts: List<Alt>): String = alts.joinToString("\n") { "${it.w}|${it.h}|${it.url}" }

    fun decode(text: String?): List<Alt> {
        if (text.isNullOrEmpty()) return emptyList()
        val out = ArrayList<Alt>()
        for (line in text.split('\n')) {
            val a = line.indexOf('|'); val b = if (a > 0) line.indexOf('|', a + 1) else -1
            if (a <= 0 || b <= a) continue
            val w = line.substring(0, a).toIntOrNull() ?: continue
            val h = line.substring(a + 1, b).toIntOrNull() ?: continue
            val u = line.substring(b + 1)
            if (w <= 0 || h <= 0 || !(u.startsWith("https://") || u.startsWith("http://")) || u.length > 4000) continue
            out.add(Alt(u, w, h))
            if (out.size >= MAX) break
        }
        return out
    }
}
