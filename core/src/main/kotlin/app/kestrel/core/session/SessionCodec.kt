package app.kestrel.core.session

/** One persisted tab. The page itself is NOT stored here (WebView state goes elsewhere); this is just enough to list and reload it. */
data class TabRecord(
    val id: Long,
    val url: String,
    val title: String,
    val lastActiveMillis: Long,
    val parentId: Long, // 0 = none; closing a child returns to its parent
    val desktopSite: Boolean,
)

data class Session(val tabs: List<TabRecord>, val selectedId: Long)

/**
 * Crash-tolerant tab session format. The file is written atomically by the app (temp file + rename), and decoding is
 * forgiving anyway: a damaged line costs one tab, never the whole session, and nothing here can throw.
 *
 *   KSESSION 1 <selectedId>
 *   <id> \t <lastActive> \t <parent> \t <desktop 0|1> \t <url> \t <title>      one line per tab, tab-separated;
 *                                                                              '%', TAB, LF and CR are escaped as %XX
 */
object SessionCodec {
    const val MAX_TABS = 100
    private const val HEADER = "KSESSION"

    fun encode(session: Session): String = buildString {
        append(HEADER).append(" 1 ").append(session.selectedId).append('\n')
        for (t in session.tabs.take(MAX_TABS)) {
            append(t.id).append('\t').append(t.lastActiveMillis).append('\t').append(t.parentId).append('\t')
                .append(if (t.desktopSite) '1' else '0').append('\t').append(esc(t.url)).append('\t').append(esc(t.title)).append('\n')
        }
    }

    fun decode(text: String?): Session? {
        if (text.isNullOrEmpty()) return null
        val lines = text.split('\n')
        val head = lines[0].split(' ')
        if (head.size != 3 || head[0] != HEADER || head[1] != "1") return null
        val selected = head[2].toLongOrNull() ?: 0L
        val tabs = ArrayList<TabRecord>()
        val seen = HashSet<Long>()
        for (i in 1 until lines.size) {
            if (tabs.size >= MAX_TABS) break
            val f = lines[i].split('\t')
            if (f.size != 6) continue
            val id = f[0].toLongOrNull() ?: continue
            val last = f[1].toLongOrNull() ?: continue
            val parent = f[2].toLongOrNull() ?: continue
            val url = unesc(f[4]) ?: continue
            if (!acceptableUrl(url) || !seen.add(id)) continue
            tabs.add(TabRecord(id, url, (unesc(f[5]) ?: "").take(200), last, parent, f[3] == "1"))
        }
        return Session(tabs, if (tabs.any { it.id == selected }) selected else (tabs.lastOrNull()?.id ?: 0L))
    }

    /** Only things we would ever navigate to; never restore javascript:/file:/data: from a damaged or tampered file. */
    private fun acceptableUrl(u: String): Boolean =
        u.length <= 4096 && (u.startsWith("https://") || u.startsWith("http://") || u == "about:blank")

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) when (c) {
            '%' -> sb.append("%25")
            '\t' -> sb.append("%09")
            '\n' -> sb.append("%0A")
            '\r' -> sb.append("%0D")
            else -> sb.append(c)
        }
        return sb.toString()
    }

    private fun unesc(s: String): String? {
        if (s.indexOf('%') < 0) return s
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%') {
                if (i + 2 >= s.length) return null // truncated escape
                val v = s.substring(i + 1, i + 3).toIntOrNull(16) ?: return null
                sb.append(v.toChar()); i += 3
            } else { sb.append(c); i++ }
        }
        return sb.toString()
    }
}
