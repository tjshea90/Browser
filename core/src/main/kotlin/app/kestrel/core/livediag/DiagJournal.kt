package app.kestrel.core.livediag

/**
 * LD3: the lines waiting for the next upload. It replaces a plain list whose trimming was wrong in the real data (owner's first uploads, 2026-11-21): the file carried the newest 600 of up to 800 lines, and after
 * the send the OLDEST 600 were removed, so a journal of 609 lines re-sent its 9 newest lines in the next file and silently lost its 9 oldest, with `dropped` still 0.
 *
 * Rules, each one a test (DiagJournalTest):
 *  - [cut] takes the newest `maxSend` lines for a file and remembers the exact position it reached ([Cut.upTo], a running line number that never goes back);
 *  - [commit] after a successful send removes exactly the lines up to that position: lines that arrived while the upload was running stay, nothing is sent twice, and lines that fell off the front
 *    while it ran are not mistaken for sent ones;
 *  - every line that could not go in a file is counted ([Cut.lost]: cut by the size limit, refused by the scrub), so a gap is said in the file instead of being a mystery;
 *  - [add] thins a NOISY kind (a kind that repeats all day, like one "video quality" line every 12 seconds) to [perKind] lines per upload window and counts the rest ([Cut.thinned]); the counts of those are in
 *    the histogram anyway. Kinds that mean trouble (crash, kill, fail, error, stall, slow, page loaded, memory trims ...) are never thinned, so a flood of routine lines cannot push the news out.
 */
class DiagJournal(private val max: Int = 800, private val perKind: Int = 40) {
    /** What one upload takes: the lines, the position reached, and the lines that were left out (lost) or thinned on purpose. */
    class Cut(val lines: List<String>, val upTo: Long, val lost: Int, val thinned: Int)

    private val lines = ArrayDeque<String>()
    private var base = 0L                       // how many lines have left the front since the start (sent, trimmed or erased)
    private var lost = 0
    private var thinned = 0
    private val shown = HashMap<String, Int>()  // lines kept per noisy kind in this upload window

    /** Adds a line of kind "layer event". Returns true when it was kept. */
    @Synchronized fun add(kind: String, line: String): Boolean {
        if (!important(kind) && !troubleLine(kind, line)) {
            val key = if (shown.size >= MAX_KINDS && !shown.containsKey(kind)) "other" else kind
            val n = (shown[key] ?: 0) + 1
            shown[key] = n
            if (n > (if (bookkeeping(kind)) minOf(perKind, BOOKKEEPING_KEEP) else perKind)) { thinned++; return false }
        }
        lines.addLast(line)
        while (lines.size > max) { lines.removeFirst(); base++; lost++ }
        return true
    }

    /** A line the scrub refused to keep: said in the next file. */
    @Synchronized fun lose() { lost++ }

    @Synchronized fun size(): Int = lines.size
    @Synchronized fun toList(): List<String> = lines.toList()
    @Synchronized fun isEmpty(): Boolean = lines.isEmpty()

    /** The newest [maxSend] lines, and where they end. Nothing is removed yet. */
    @Synchronized fun cut(maxSend: Int): Cut {
        val all = lines.toList()
        val send = if (all.size <= maxSend) all else all.subList(all.size - maxSend, all.size)
        return Cut(send.toList(), base + all.size, lost + (all.size - send.size), thinned)
    }

    /** The send worked: everything up to [c].upTo is gone (sent, or reported as left out); later lines stay; the per-kind window starts again. */
    @Synchronized fun commit(c: Cut) {
        while (base < c.upTo && lines.isNotEmpty()) { lines.removeFirst(); base++ }
        lost = (lost - c.lost).coerceAtLeast(0)
        thinned = (thinned - c.thinned).coerceAtLeast(0)
        shown.clear()
    }

    /** Stored lines read back after a restart (the newest [max]); the per-kind window is rebuilt from them. */
    @Synchronized fun restore(stored: List<String>) {
        lines.clear(); shown.clear(); base = 0L
        for (l in stored.takeLast(max)) { lines.addLast(l); kindOf(l)?.let { if (!important(it)) shown[it] = (shown[it] ?: 0) + 1 } }
    }

    /** Everything is erased (the owner turned the feature off, or pressed Erase). */
    @Synchronized fun clear() { lines.clear(); shown.clear(); lost = 0; thinned = 0; base = 0L }

    companion object {
        private const val MAX_KINDS = 300
        private val IMPORTANT = Regex("crash|kill|fail|error|exception|fatal|anr|stall|oom|dead|died|freez|hang|hung|slow|late|timeout|refus|denied|abnormal|warn|critical|page loaded|page timing|trim|decoder|corrupt|lost|dropped|loop|^sec |^video (gate|codec)|^run ")
        private val HEAD = Regex("""^\+\d+(?:\.\d+)?s (\S+) (\S+)""")

        /** Is this kind one whose lines must always be kept? */
        fun important(kind: String): Boolean { val k = kind.lowercase(); return k !in ROUTINE && IMPORTANT.containsMatchIn(k) }

        /** PERF12: kinds whose NAME looks like trouble ("trim", "dropped") but whose lines are routine (a memory trim of level 20 is only "the app went to the background"; a dropped tab thumbnail is a normal clean-up): the alpha95 night upload lost 602 lines
         *  and 170 of the 600 it kept were these two. They are thinned like any noisy kind, except the lines that DO mean trouble ([troubleLine]). */
        private val ROUTINE = setOf("mem trim", "tabs thumb-dropped")
        private val TRIM_TROUBLE = Regex("""low=true|level=(?!20\b)\d+""")
        /**
         * PERF16 (owner 2026-11-24: "Focus future diagnostics on speed of app, page loading speed, video playback, page compatibility, and privacy and security"): lines that only say that the screen redrew, the window gained focus, a
         * picture of a tab was saved or a warm-up connection was made are counted in the histogram but kept to a few examples, so the 600 lines of an upload are spent on the five areas (their lines stay at the usual number).
         */
        private val BOOKKEEPING = Regex("""^(gecko (first-composite|size|layout|view-attached|paint-reset|view)|win focus|life |tabs (look-|cover-|curtain-|thumb-|leave-shot|early-cover|private-anchor|back-|state-)|engine (warm|network-profile|burst|runtime|config-warm|scroll-feel|phone-proxy|etp)|cosmetic |shields ext-state|storage |start (frame|look|open|art|oat|heap|activity|stop|runtime)|media (maybe|verif)|video info)""")
        const val BOOKKEEPING_KEEP = 8
        fun bookkeeping(kind: String): Boolean = BOOKKEEPING.containsMatchIn(kind.lowercase())

        fun troubleLine(kind: String, line: String): Boolean = kind.lowercase() == "mem trim" && TRIM_TROUBLE.containsMatchIn(line)

        /** "layer event" of a stored "+12.3s layer event ..." line, or null. */
        fun kindOf(line: String): String? = HEAD.find(line)?.let { it.groupValues[1] + " " + it.groupValues[2] }
    }
}
