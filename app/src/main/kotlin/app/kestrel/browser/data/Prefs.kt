package app.kestrel.browser.data

import android.content.Context
import android.content.SharedPreferences
import app.kestrel.core.shield.DataSaver
import app.kestrel.core.shield.ShieldSettings
import app.kestrel.core.url.HttpsPolicy

class SearchEngine(val id: String, val name: String, val template: String)

/** Typed wrapper over SharedPreferences. Defaults encode the product stance: private, HTTPS-first, data-frugal. */
class Prefs private constructor(ctx: Context) {
    private val sp: SharedPreferences = ctx.applicationContext.getSharedPreferences("kestrel", Context.MODE_PRIVATE)

    private fun bool(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun put(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    private fun put(k: String, v: String) = sp.edit().putString(k, v).apply()
    private fun put(k: String, v: Long) = sp.edit().putLong(k, v).apply()
    private fun put(k: String, v: Int) = sp.edit().putInt(k, v).apply()

    var searchId: String
        get() = sp.getString("search", ENGINES[0].id) ?: ENGINES[0].id
        set(v) = put("search", v)
    val searchTemplate: String get() = (ENGINES.firstOrNull { it.id == searchId } ?: ENGINES[0]).template

    var httpsMode: HttpsPolicy.Mode
        get() = runCatching { HttpsPolicy.Mode.valueOf(sp.getString("https", "STRICT")!!) }.getOrDefault(HttpsPolicy.Mode.STRICT)
        set(v) = put("https", v.name)
    var shieldAds: Boolean get() = bool("sh_ads", true); set(v) = put("sh_ads", v)
    var shieldCosmetic: Boolean get() = bool("sh_cos", true); set(v) = put("sh_cos", v)
    var shieldCookies: Boolean get() = bool("sh_cookies", true); set(v) = put("sh_cookies", v)
    var shieldFingerprint: Boolean get() = bool("sh_fp", true); set(v) = put("sh_fp", v)

    var dataSaver: DataSaver
        get() = DataSaver.parse(sp.getString("data_saver", "STANDARD"))
        set(v) = put("data_saver", v.name)

    /** 0 = follow page / system, 1 = force dark pages, 2 = never. */
    var webDark: Int get() = sp.getInt("web_dark", 0); set(v) = put("web_dark", v)
    var textZoom: Int get() = sp.getInt("text_zoom", 100); set(v) = put("text_zoom", v.coerceIn(50, 200))
    var searchSuggestions: Boolean get() = bool("suggest", false); set(v) = put("suggest", v) // off: each keystroke would cost data + privacy
    var clearOnExit: Boolean get() = bool("clear_exit", false); set(v) = put("clear_exit", v)

    /** 0 weekly, 1 monthly, 2 manual. */
    var updatePolicy: Int get() = sp.getInt("upd_policy", 0); set(v) = put("upd_policy", v)
    var updateUnmeteredOnly: Boolean get() = bool("upd_unmetered", false); set(v) = put("upd_unmetered", v)
    var lastUpdateMs: Long get() = sp.getLong("upd_last", 0L); set(v) = put("upd_last", v)
    var updateJobSig: String get() = sp.getString("upd_sig", "") ?: ""; set(v) = put("upd_sig", v)
    var lastUpdateNote: String get() = sp.getString("upd_note", "") ?: ""; set(v) = put("upd_note", v)

    var blockedTotal: Long get() = sp.getLong("blocked_total", 0L); set(v) = put("blocked_total", v)
    var dataBaseline: Long get() = sp.getLong("data_base", 0L); set(v) = put("data_base", v)
    var dataBaselineBoot: Long get() = sp.getLong("data_base_boot", 0L); set(v) = put("data_base_boot", v)
    var dataAccumulated: Long get() = sp.getLong("data_acc", 0L); set(v) = put("data_acc", v)

    fun globalShields() = ShieldSettings(
        shieldsUp = true, blockAds = shieldAds, cosmetic = shieldCosmetic, https = httpsMode,
        blockThirdPartyCookies = shieldCookies, blockFingerprinting = shieldFingerprint, blockScripts = false,
    )

    companion object {
        @Volatile private var inst: Prefs? = null
        fun get(ctx: Context): Prefs = inst ?: synchronized(this) { inst ?: Prefs(ctx).also { inst = it } }

        /** The FIRST entry is the default: anything typed in the address bar that is not an address is searched on Google (owner's requirement). */
        val ENGINES = listOf(
            SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
            SearchEngine("brave", "Brave Search", "https://search.brave.com/search?q=%s"),
            SearchEngine("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
            SearchEngine("startpage", "Startpage", "https://www.startpage.com/do/search?q=%s"),
            SearchEngine("qwant", "Qwant", "https://www.qwant.com/?q=%s"),
            SearchEngine("mojeek", "Mojeek", "https://www.mojeek.com/search?q=%s"),
            SearchEngine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s"),
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
        )
    }
}
