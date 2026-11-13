package app.kestrel.browser.data

import android.content.SharedPreferences
import app.kestrel.browser.shield.Shields
import app.kestrel.core.backup.BackupData
import app.kestrel.core.backup.Mark
import app.kestrel.core.backup.TabRef
import app.kestrel.core.backup.Visit
import app.kestrel.core.shield.DataSaver
import app.kestrel.core.url.HttpsPolicy
import java.text.NumberFormat

/**
 * Settings > Back up and restore (owner U1 / AA3-7: "so an uninstall-first loses nothing"): what goes into the file and what comes back. ALL methods that touch the database block on disk: call
 * them from the IO thread. The file format and its checks are core/backup/BackupCodec.
 */
object BackupManager {
    private enum class T { BOOL, INT, STR }
    private class Spec(val key: String, val t: T, val min: Int = 0, val max: Int = 0, val oneOf: Set<String>? = null)

    /**
     * The choices a backup carries, and the only keys a backup may set (a file is untrusted: any other key, a wrong type, or a value out of range is ignored). Not in it, on purpose: the phone helper's
     * pairing code and choices (re-pair on the new install), counters, update bookkeeping, and the engine switches meant for debugging.
     */
    private val SPECS: List<Spec> = listOf(
        Spec("search", T.STR, oneOf = Prefs.ENGINES.map { it.id }.toSet()),
        Spec("https", T.STR, oneOf = HttpsPolicy.Mode.values().map { it.name }.toSet()),
        Spec("sh_ads", T.BOOL), Spec("sh_cos", T.BOOL), Spec("block_popups", T.BOOL), Spec("sh_cookies", T.BOOL), Spec("sh_fp", T.BOOL), Spec("sh_embeds", T.BOOL),
        Spec("data_saver", T.STR, oneOf = DataSaver.values().map { it.name }.toSet()),
        Spec("web_dark", T.INT, 0, 2), Spec("text_zoom", T.INT, 50, 200), Spec("neon", T.INT, 0, 2), Spec("scroll_feel", T.INT, 0, 2), Spec("tabs_kept", T.INT, 0, 2),
        Spec("net_doh", T.BOOL), Spec("net_h3", T.BOOL), Spec("net_preconnect", T.BOOL),
        Spec("dl_yield", T.BOOL), Spec("dl_limit_kbps", T.INT, 0, 1_000_000), Spec("dl_active", T.INT, 1, 3), Spec("dl_conns", T.INT, 0, 3),
        Spec("pull_refresh", T.BOOL), Spec("suggest", T.BOOL), Spec("clear_exit", T.BOOL), Spec("tap_zoom", T.BOOL), Spec("input_zoom", T.BOOL),
        Spec("lab_dns", T.BOOL), Spec("lab_noavif", T.BOOL), Spec("lab_buf", T.BOOL), Spec("lab_font", T.BOOL), Spec("lab_spec", T.INT, 0, 2), Spec("lab_wifi", T.BOOL), Spec("lab_helper_vpn", T.BOOL),
        Spec("upd_policy", T.INT, 0, 2), Spec("upd_unmetered", T.BOOL), Spec("app_upd_auto", T.BOOL), Spec("app_upd_dl", T.BOOL), Spec("app_upd_inst", T.BOOL),
    )

    /** The choices that were really made (a default nobody touched stays a default on the new install), as the file carries them. */
    fun settingsSnapshot(sp: SharedPreferences): Map<String, Any> {
        val all = sp.all
        val out = LinkedHashMap<String, Any>()
        for (s in SPECS) {
            val v = all[s.key] ?: continue
            val ok: Any? = when (s.t) {
                T.BOOL -> v as? Boolean
                T.INT -> (v as? Number)?.toLong()?.takeIf { it in s.min..s.max }
                T.STR -> (v as? String)?.takeIf { s.oneOf == null || it in s.oneOf }
            }
            if (ok != null) out[s.key] = ok
        }
        return out
    }

    /** Applies the keys of [m] that pass the checks; returns how many were applied. Everything else in the map is ignored. */
    fun applySettings(sp: SharedPreferences, m: Map<String, Any>): Int {
        val e = sp.edit(); var n = 0
        for (s in SPECS) {
            val v = m[s.key] ?: continue
            when (s.t) {
                T.BOOL -> if (v is Boolean) { e.putBoolean(s.key, v); n++ }
                T.INT -> if (v is Number && v.toLong() in s.min..s.max) { e.putInt(s.key, v.toInt()); n++ }
                T.STR -> if (v is String && (s.oneOf == null || v in s.oneOf)) { e.putString(s.key, v); n++ }
            }
        }
        e.apply()
        return n
    }

    /** Collects a backup: bookmarks, the most recent history, the choices, the per-site Shields and [tabs] (the caller leaves private tabs out). Blocks on disk. */
    fun build(db: BrowserDb, prefs: Prefs, tabs: List<TabRef>, appName: String, now: Long = System.currentTimeMillis()): BackupData {
        val marks = db.bookmarks().map { Mark(it.url, it.title, it.last) }
        val visits = db.history(HISTORY_LIMIT).map { Visit(it.url, it.title, it.visits, it.last) }
        return BackupData(now, appName, prefs.backupSnapshot(), Shields.exportText(), marks, visits, tabs)
    }

    /** What a restore added or applied, for the confirmation line. */
    class Counts(val bookmarks: Int, val history: Int, val settings: Int, val sites: Int, val tabs: Int)

    /** Brings [d] back, except the tabs (the activity opens those): bookmarks and history are added to what is there, the choices and per-site Shields are set from the backup. Blocks on disk. */
    fun restore(db: BrowserDb, prefs: Prefs, d: BackupData): Counts {
        val b = db.importBookmarks(d.bookmarks)
        val h = db.importHistory(d.history)
        val s = prefs.restoreBackup(d.settings)
        val sites = if (d.siteShields.isNotEmpty()) Shields.importText(d.siteShields) else 0
        return Counts(b, h, s, sites, d.tabs.size)
    }

    /** "12 bookmarks, 3,400 history entries, 18 choices, 5 site choices and 6 tabs" - what a file holds, for the question before a restore and the line after a backup. */
    fun describe(d: BackupData): String = describe(d.bookmarks.size, d.history.size, d.settings.size, siteCount(d.siteShields), d.tabs.size)

    fun describe(bookmarks: Int, history: Int, settings: Int, sites: Int, tabs: Int): String {
        val nf = NumberFormat.getIntegerInstance()
        fun part(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "${nf.format(n)} $many"
        val parts = ArrayList<String>()
        parts += part(bookmarks, "bookmark", "bookmarks")
        parts += part(history, "history entry", "history entries")
        parts += part(settings, "setting", "settings")
        if (sites > 0) parts += part(sites, "site choice", "site choices")
        parts += part(tabs, "open tab", "open tabs")
        return if (parts.size <= 1) parts.joinToString() else parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }

    private fun siteCount(text: String): Int = if (text.isEmpty()) 0 else app.kestrel.core.shield.SiteShieldStore.decode(text).size()

    /** How many of the most recent history entries a backup carries (the database itself keeps more). */
    const val HISTORY_LIMIT = 5000

    /** The file name offered when saving: "Kestrel backup 2026-11-12.json". */
    fun fileName(now: Long = System.currentTimeMillis()): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return "Kestrel backup " + f.format(java.util.Date(now)) + ".json"
    }
}
