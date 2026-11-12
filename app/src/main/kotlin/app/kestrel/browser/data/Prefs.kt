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
    /** Kestrel G: refuse windows and tab redirects the user did not ask for (movie-site spam). A site with Shields off is exempt. */
    var blockPopups: Boolean get() = bool("block_popups", true); set(v) = put("block_popups", v)
    var shieldCookies: Boolean get() = bool("sh_cookies", true); set(v) = put("sh_cookies", v)
    var shieldFingerprint: Boolean get() = bool("sh_fp", true); set(v) = put("sh_fp", v)

    var dataSaver: DataSaver
        get() = DataSaver.parse(sp.getString("data_saver", "STANDARD"))
        set(v) = put("data_saver", v.name)

    /** 0 = follow page / system, 1 = force dark pages, 2 = never. */
    var webDark: Int get() = sp.getInt("web_dark", 0); set(v) = put("web_dark", v)
    var textZoom: Int get() = sp.getInt("text_zoom", 100); set(v) = put("text_zoom", v.coerceIn(50, 200))
    /** The blue neon glow around the dock, the Home field and the selected tab: 0 off, 1 steady, 2 subtle pulse (default). */
    var neon: Int get() = sp.getInt("neon", 2); set(v) = put("neon", v.coerceIn(0, 2))
    /** Gecko build: how far ahead of the finger the page is pre-drawn. 0 = the engine's own values, 1 = wider margin + shorter touch wait, 2 = also slower top fling speed. */
    var scrollFeel: Int get() = sp.getInt("scroll_feel", 1); set(v) = put("scroll_feel", v.coerceIn(0, 2))
    /** Network experiments (Kestrel G, Settings > Network lab): encrypted DNS through Cloudflare (on = private; off = the system resolver, which can be quicker on a hotspot) and HTTP/3 over UDP. Both on by default. */
    var dohOn: Boolean get() = bool("net_doh", true); set(v) = put("net_doh", v)
    var http3On: Boolean get() = bool("net_h3", true); set(v) = put("net_h3", v)
    /** Warm up the connection (DNS, TCP, TLS) to the site the top address-bar suggestion names, so its first bytes do not wait for the handshakes when the owner presses Go. History and bookmarks only, never in a private tab. */
    var preconnect: Boolean get() = bool("net_preconnect", true); set(v) = put("net_preconnect", v)

    /** Downloads (Settings > Downloads): one speed limit for all of them in KB/s (0 = none), how many run at once (1-3, default 2) and connections per download (0 = automatic, else 1-3). The last two apply the next time the browser starts. */
    var dlLimitKBps: Int get() = sp.getInt("dl_limit_kbps", 0); set(v) = put("dl_limit_kbps", v.coerceAtLeast(0))
    var dlActive: Int get() = sp.getInt("dl_active", 2).coerceIn(1, 3); set(v) = put("dl_active", v.coerceIn(1, 3))
    var dlConns: Int get() = sp.getInt("dl_conns", 0).coerceIn(0, 3); set(v) = put("dl_conns", v.coerceIn(0, 3))
    /** Download on my phone (Settings > Downloads, research/PHONE_OFFLOAD.md): 0 off, 1 use the phone when it answers, 2 always wait for it. */
    var phoneMode: Int get() = sp.getInt("phone_mode", 0).coerceIn(0, 2); set(v) = put("phone_mode", v.coerceIn(0, 2))
    /** The pairing code the Courier app on the phone shows (16 characters, typed once); empty = not paired. It is the key to the phone link: private to this app (no backup). */
    var phoneCode: String get() = sp.getString("phone_code", "") ?: ""; set(v) = put("phone_code", v)
    var phonePort: Int get() = sp.getInt("phone_port", app.kestrel.core.courier.CourierPorts.API).coerceIn(1024, 65535); set(v) = put("phone_port", v.coerceIn(1024, 65535))
    /** The owner chose a phone mode at least once (typing the pairing code switches the helper on only if they never did). */
    fun hasPhoneModeChoice(): Boolean = sp.contains("phone_mode")
    /** Pages through the phone (research/SPEED2.md): while the phone helper is on, https page loads use the Courier's proxy on the phone (its own connection, not the capped hotspot). */
    var pagesViaPhone: Boolean get() = sp.getBoolean("pages_via_phone", true); set(v) = put("pages_via_phone", v)
    /** Save data: the old "metered connection" behaviour (small read-ahead, no speculative loading). The owner's hotspot is unmetered, so this is OFF unless they switch it on. */
    var dataSaver: Boolean get() = sp.getBoolean("data_saver", false); set(v) = put("data_saver", v)
    /** Kestrel G: the blocking engine runs inside the Gecko extension (fast). Off = compatibility mode, every request asks the app (the older, slower path). Diagnostics has the switch. */
    var engineInExtension: Boolean get() = bool("eng_ext", true); set(v) = put("eng_ext", v)
    var pullRefresh: Boolean get() = bool("pull_refresh", true); set(v) = put("pull_refresh", v)
    var searchSuggestions: Boolean get() = bool("suggest", false); set(v) = put("suggest", v) // off: each keystroke would cost data + privacy
    var clearOnExit: Boolean get() = bool("clear_exit", false); set(v) = put("clear_exit", v)

    /** 0 weekly, 1 monthly, 2 manual. */
    var updatePolicy: Int get() = sp.getInt("upd_policy", 0); set(v) = put("upd_policy", v)
    var updateUnmeteredOnly: Boolean get() = bool("upd_unmetered", false); set(v) = put("upd_unmetered", v)
    var lastUpdateMs: Long get() = sp.getLong("upd_last", 0L); set(v) = put("upd_last", v)
    var updateJobSig: String get() = sp.getString("upd_sig", "") ?: ""; set(v) = put("upd_sig", v)
    /** Look for a new app release (GitHub Releases) once a day, when the app opens. Off = only the "Check now" button. */
    var autoUpdateCheck: Boolean get() = bool("app_upd_auto", true); set(v) = put("app_upd_auto", v)
    /** Download a new release by itself (in the download service, screen off is fine) as soon as it is found. Owner request 8: on by default. */
    /** Off by default (owner request X2: a dialog with an Update button first; a 92 MB download over the hotspot is the owner's call). Switch on in Settings for fully automatic downloads. */
    var autoUpdateDownload: Boolean get() = bool("app_upd_dl", false); set(v) = put("app_upd_dl", v)
    /** Which set of update news the dialog showed last (see UpdatePlan.Notice.key): the same news is not shown twice. */
    var updateNoticeKey: String get() = sp.getString("upd_notice_key", "") ?: ""; set(v) = put("upd_notice_key", v)
    /** Start the installer by itself when the update is ready and Android allows it without a prompt (API 31+; Android 10 always asks). */
    var autoUpdateInstall: Boolean get() = bool("app_upd_inst", true); set(v) = put("app_upd_inst", v)
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
