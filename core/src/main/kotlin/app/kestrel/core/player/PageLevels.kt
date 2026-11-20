package app.kestrel.core.player

/**
 * QL1 (owner message 2026-11-18: "When i put YouTube video in full screen, it doesn't let me choose the video quality. I want to be able to choose the video quality on as many sites and videos as possible"):
 * the quality list the PAGE's own player offers (YouTube, JW Player, Video.js, hls.js, dash.js, Shaka, <source> lists: assets/kestrel-shields/quality.js), as the app takes it. The page is untrusted: ids and
 * labels are cleaned here once, the list is cut to a few rows, "auto" comes first, and a pick can only name an id from the list the app itself holds. Pure Kotlin, tested without a screen.
 */
object PageLevels {
    const val MAX_ROWS = 12
    const val MAX_ID = 24
    const val MAX_LABEL = 22
    const val AUTO = "auto"

    class Level(val id: String, val label: String) {
        override fun equals(other: Any?) = other is Level && other.id == id && other.label == label
        override fun hashCode() = id.hashCode() * 31 + label.hashCode()
        override fun toString() = "$id=$label"
    }

    private val ID = Regex("^[A-Za-z0-9_.:\\-]{1,$MAX_ID}$")

    private fun cleanLabel(s: String): String = s.replace(Regex("[\\u0000-\\u001f<>]"), " ").replace(Regex("\\s+"), " ").trim().take(MAX_LABEL).trim()

    /**
     * The (id, label) pairs the page sent -> the rows to show: a row needs a plain id and a label; a repeated id is dropped; "auto" is always called Automatic and comes first; at most [MAX_ROWS].
     * A list of fewer than two rows is no choice at all (empty): no Quality button for it.
     */
    fun clean(raw: List<Pair<String, String>>): List<Level> {
        val seen = HashSet<String>()
        val rows = ArrayList<Level>()
        var auto: Level? = null
        for ((id0, label0) in raw) {
            val id = id0.trim()
            if (!ID.matches(id) || !seen.add(id)) continue
            if (id == AUTO) { auto = Level(AUTO, "Automatic"); continue }
            val label = cleanLabel(label0)
            if (label.isEmpty()) continue
            rows.add(Level(id, label))
        }
        val out = ArrayList<Level>(rows.size + 1)
        auto?.let { out.add(it) }
        out.addAll(rows)
        val cut = if (out.size > MAX_ROWS) out.subList(0, MAX_ROWS).toList() else out
        return if (cut.size >= 2) cut else emptyList()
    }

    /** The row a pick names, only from the list the app holds (null = not on it: nothing is sent to the page). */
    fun valid(id: String, list: List<Level>): Level? = list.firstOrNull { it.id == id }

    /** The row that carries the check mark: what the person picked in Kestrel's list this full screen, else what the page's player says it uses. */
    fun selectedId(picked: String?, current: String, list: List<Level>): String? =
        (picked ?: current).takeIf { id -> list.any { it.id == id } }
}
