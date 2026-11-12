package app.kestrel.core.backup

import app.kestrel.core.courier.Json
import app.kestrel.core.courier.JsonException

/** The file is not a usable backup; [message] is a plain sentence for the person holding the tablet. */
class BackupException(message: String) : Exception(message)

class Mark(val url: String, val title: String, val added: Long)
class Visit(val url: String, val title: String, val visits: Int, val last: Long)
class TabRef(val url: String, val title: String)

/**
 * Everything a backup file carries (owner request U1 / AA3-7: "so an uninstall-first loses nothing"): the bookmarks, the history, the choices made in Settings, the per-site Shields choices
 * and the open tabs. No cookies, logins, passwords, pairing codes or private tabs: the file is plain text that ends up in Downloads, where anything can read it.
 *
 * @property settings preference key -> Boolean | Long | String, exactly as the app stored it (the app decides which keys it takes back)
 * @property siteShields the per-site Shields store as text (core/shield/SiteShieldStore), "" when there is none
 * @property skipped entries of a file that were dropped as unusable (a `javascript:` address, an overlong one, a duplicate): shown to the person, never an error
 */
class BackupData(
    val createdAt: Long,
    val app: String,
    val settings: Map<String, Any>,
    val siteShields: String,
    val bookmarks: List<Mark>,
    val history: List<Visit>,
    val tabs: List<TabRef>,
    val skipped: Int = 0,
)

/**
 * The backup file: one JSON object, written by [encode] and read by [decode]. A backup is UNTRUSTED input (it may have been edited, damaged, or handed over by someone else), so reading it is strict:
 * size and counts are capped, only http(s) addresses are taken (never `javascript:`, `file:`, `content:`, `intent:`, `data:`), titles are clipped, duplicates are dropped, and anything of a wrong type
 * is skipped instead of trusted.
 */
object BackupCodec {
    const val FORMAT = "kestrel-backup"
    const val VERSION = 1
    const val MAX_BYTES = 12 * 1024 * 1024
    const val MAX_BOOKMARKS = 20_000
    const val MAX_HISTORY = 20_000
    const val MAX_TABS = 200
    const val MAX_SETTINGS = 200
    const val MAX_URL = 2048
    const val MAX_TITLE = 300
    const val MAX_SETTING_TEXT = 200
    const val MAX_SHIELDS_TEXT = 512 * 1024

    fun encode(d: BackupData): String {
        val o = LinkedHashMap<String, Any?>()
        o["format"] = FORMAT
        o["version"] = VERSION.toLong()
        o["createdAt"] = d.createdAt
        o["app"] = d.app
        o["settings"] = LinkedHashMap<String, Any?>(d.settings)
        if (d.siteShields.isNotEmpty()) o["siteShields"] = d.siteShields
        o["bookmarks"] = d.bookmarks.map { mapOf("u" to it.url, "t" to it.title, "a" to it.added) }
        o["history"] = d.history.map { mapOf("u" to it.url, "t" to it.title, "n" to it.visits.toLong(), "l" to it.last) }
        o["tabs"] = d.tabs.map { mapOf("u" to it.url, "t" to it.title) }
        return Json.write(o)
    }

    /** @throws BackupException when [text] is not a backup this version can read at all; single bad entries are skipped and counted in [BackupData.skipped]. */
    fun decode(text: String): BackupData {
        if (text.length > MAX_BYTES) throw BackupException("This file is too big to be a Kestrel backup.")
        val root = try { Json.parse(text, maxDepth = 8) } catch (e: JsonException) { throw BackupException("This is not a Kestrel backup file (it could not be read).") }
        if (root !is Map<*, *>) throw BackupException("This is not a Kestrel backup file.")
        if (root["format"] != FORMAT) throw BackupException("This is not a Kestrel backup file.")
        val version = (root["version"] as? Number)?.toLong() ?: throw BackupException("This backup has no version number.")
        if (version < 1) throw BackupException("This backup file is damaged.")
        if (version > VERSION) throw BackupException("This backup was made by a newer Kestrel. Update Kestrel first, then restore it.")
        var skipped = 0

        val settings = LinkedHashMap<String, Any>()
        (root["settings"] as? Map<*, *>)?.let { m ->
            for ((k, v) in m) {
                if (settings.size >= MAX_SETTINGS) { skipped++; continue }
                val key = k as? String
                if (key == null || key.isEmpty() || key.length > 64) { skipped++; continue }
                when (v) {
                    is Boolean -> settings[key] = v
                    is Number -> settings[key] = v.toLong()
                    is String -> if (v.length <= MAX_SETTING_TEXT) settings[key] = v else skipped++
                    else -> skipped++
                }
            }
        }
        val shields = (root["siteShields"] as? String)?.takeIf { it.length <= MAX_SHIELDS_TEXT } ?: ""

        val seenMarks = HashSet<String>(); val marks = ArrayList<Mark>()
        for (e in list(root["bookmarks"])) {
            if (marks.size >= MAX_BOOKMARKS) { skipped++; continue }
            val m = e as? Map<*, *>; val u = (m?.get("u") as? String)?.trim()
            if (m == null || u == null || !okUrl(u) || !seenMarks.add(u)) { skipped++; continue }
            marks.add(Mark(u, title(m["t"]), (m["a"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L))
        }
        val seenVisits = HashSet<String>(); val visits = ArrayList<Visit>()
        for (e in list(root["history"])) {
            if (visits.size >= MAX_HISTORY) { skipped++; continue }
            val m = e as? Map<*, *>; val u = (m?.get("u") as? String)?.trim()
            if (m == null || u == null || !okUrl(u) || !seenVisits.add(u)) { skipped++; continue }
            visits.add(Visit(u, title(m["t"]), ((m["n"] as? Number)?.toLong() ?: 1L).coerceIn(1L, 1_000_000L).toInt(), (m["l"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L))
        }
        val seenTabs = HashSet<String>(); val tabs = ArrayList<TabRef>()
        for (e in list(root["tabs"])) {
            if (tabs.size >= MAX_TABS) { skipped++; continue }
            val m = e as? Map<*, *>; val u = (m?.get("u") as? String)?.trim()
            if (m == null || u == null || !okUrl(u) || !seenTabs.add(u)) { skipped++; continue }
            tabs.add(TabRef(u, title(m["t"])))
        }
        val created = (root["createdAt"] as? Number)?.toLong() ?: 0L
        val app = (root["app"] as? String)?.take(80) ?: ""
        return BackupData(created, app, settings, shields, marks, visits, tabs, skipped)
    }

    private fun list(v: Any?): List<Any?> = (v as? List<*>) ?: emptyList<Any?>()

    private fun title(v: Any?): String {
        val s = (v as? String) ?: return ""
        val t = s.filter { it.code >= 0x20 && it.code != 0x7f }.trim()
        return if (t.length > MAX_TITLE) t.substring(0, MAX_TITLE) else t
    }

    /** An address a backup may bring back: http or https, a host, no spaces or control characters, bounded. */
    fun okUrl(u: String): Boolean {
        if (u.length < 8 || u.length > MAX_URL) return false
        val low = u.substring(0, 8).lowercase()
        val rest = when {
            low.startsWith("https://") -> u.substring(8)
            low.startsWith("http://") -> u.substring(7)
            else -> return false
        }
        if (rest.isEmpty() || rest[0] == '/' || rest[0] == '?' || rest[0] == '#') return false
        for (c in u) if (c.code <= 0x20 || c.code == 0x7f) return false
        return true
    }
}
