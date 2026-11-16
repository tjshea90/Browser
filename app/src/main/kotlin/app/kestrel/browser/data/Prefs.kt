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
    /** Kestrel G (AW1b): the scriptlets of the lists run in the page (anti-adblock walls, pop-up openers, ad injectors, countdowns). A site with Shields off is exempt. */
    var shieldScripts: Boolean get() = bool("sh_scripts", true); set(v) = put("sh_scripts", v)
    /** Kestrel G (RX3-Y2): skip the ads inside video players (YouTube): mute, speed up, press Skip, close overlays, remove the "ad blockers are not allowed" box. A site with Shields off is exempt. */
    var skipVideoAds: Boolean get() = bool("skip_video_ads", true); set(v) = put("skip_video_ads", v)
    /** YS1: a tap on a video player's progress bar moves the video when the page does not (Kestrel G, seekbar.js). */
    var tapSeek: Boolean get() = bool("tap_seek", true); set(v) = put("tap_seek", v)
    /** RX3-B2: AdGuard Tracking Protection (its tracking sections) and HaGeZi Light as two more block lists: more cookie-sync and tracker requests stopped, a few pages may need Shields off for the site. Default ON (owner 2026-11-14). */
    var extraTrackerLists: Boolean get() = bool("extra_tracker_lists", true); set(v) = put("extra_tracker_lists", v)
    /** Kestrel G: refuse windows and tab redirects the user did not ask for (movie-site spam). A site with Shields off is exempt. */
    var blockPopups: Boolean get() = bool("block_popups", true); set(v) = put("block_popups", v)
    var shieldCookies: Boolean get() = bool("sh_cookies", true); set(v) = put("sh_cookies", v)
    var shieldFingerprint: Boolean get() = bool("sh_fp", true); set(v) = put("sh_fp", v)
    /** Kestrel G: video and social players wait for a tap (EX-10). */
    var shieldEmbeds: Boolean get() = bool("sh_embeds", true); set(v) = put("sh_embeds", v)

    /** OFF by default: the owner's hotspot is unmetered ("no data saving is necessary"); the levels stay as an option. Only a level above OFF makes the app treat a metered network as metered. */
    var dataSaver: DataSaver
        get() = DataSaver.parse(sp.getString("data_saver", "OFF"))
        set(v) { put("data_saver", v.name); app.kestrel.browser.net.NetworkMonitor.setSaver(v != DataSaver.OFF) }

    /** 0 = follow page / system, 1 = force dark pages, 2 = never. */
    var webDark: Int get() = sp.getInt("web_dark", 0); set(v) = put("web_dark", v)
    var textZoom: Int get() = sp.getInt("text_zoom", 100); set(v) = put("text_zoom", v.coerceIn(50, 200))
    /** The blue neon glow around the dock, the Home field and the selected tab: 0 off, 1 steady, 2 subtle pulse (default). */
    var neon: Int get() = sp.getInt("neon", 2); set(v) = put("neon", v.coerceIn(0, 2))
    /** Gecko build: how far ahead of the finger the page is pre-drawn. 0 = the engine's own values, 1 = wider margin + shorter touch wait, 2 = also slower top fling speed. */
    var scrollFeel: Int get() = sp.getInt("scroll_feel", 1); set(v) = put("scroll_feel", v.coerceIn(0, 2))
    /** Gecko build: how many tabs keep their page loaded in memory (Back and switching stay instant): 0 = automatic (follows the free memory), 1 = few (2), 2 = many (6). */
    var tabsKept: Int get() = sp.getInt("tabs_kept", 0); set(v) = put("tabs_kept", v.coerceIn(0, 2))
    /** [tabsKept] as the number LiveBudget wants: 0 = automatic. */
    val tabsKeptCount: Int get() = when (tabsKept) { 1 -> 2; 2 -> 6; else -> 0 }
    /** Network experiments (Kestrel G, Settings > Network lab): encrypted DNS through Cloudflare (on = private; off = the system resolver, which can be quicker on a hotspot) and HTTP/3 over UDP. Both on by default. */
    var dohOn: Boolean get() = bool("net_doh", true); set(v) = put("net_doh", v)
    var http3On: Boolean get() = bool("net_h3", true); set(v) = put("net_h3", v)
    /** Warm up the connection (DNS, TCP, TLS) to the site the owner is about to open, so its first bytes do not wait for the handshakes: the top address-bar suggestion (history and bookmarks only), the page in front after a few seconds away, a link a finger is on, the first search results. https only, never in a private tab, never with Data saver. */
    var preconnect: Boolean get() = bool("net_preconnect", true); set(v) = put("net_preconnect", v)

    /** Downloads (Settings > Downloads): one speed limit for all of them in KB/s (0 = none), how many run at once (1-3, default 2) and connections per download (0 = automatic, else 1-3). The last two apply the next time the browser starts. */
    /** Downloads slow down while the page in front loads (so the page gets the link). On by default. */
    var dlYield: Boolean get() = bool("dl_yield", true); set(v) = put("dl_yield", v)
    var dlLimitKBps: Int get() = sp.getInt("dl_limit_kbps", 0); set(v) = put("dl_limit_kbps", v.coerceAtLeast(0))
    var dlActive: Int get() = sp.getInt("dl_active", 2).coerceIn(1, 3); set(v) = put("dl_active", v.coerceIn(1, 3))
    var dlConns: Int get() = sp.getInt("dl_conns", 0).coerceIn(0, 3); set(v) = put("dl_conns", v.coerceIn(0, 3))
    /** Download on my phone (Settings > Downloads, research/PHONE_OFFLOAD.md): 0 off, 1 use the phone when it answers, 2 always wait for it. */
    var phoneMode: Int get() = sp.getInt("phone_mode", 0).coerceIn(0, 2); set(v) = put("phone_mode", v.coerceIn(0, 2))
    /** The pairing code the Courier app on the phone shows (16 characters, typed once); empty = not paired. It is the key to the phone link: private to this app (no backup). */
    var phoneCode: String get() = sp.getString("phone_code", "") ?: ""; set(v) = put("phone_code", v)
    var phonePort: Int get() = sp.getInt("phone_port", app.kestrel.core.courier.CourierPorts.API).coerceIn(1024, 65535); set(v) = put("phone_port", v.coerceIn(1024, 65535))
    /** Pages through the phone (research/SPEED2.md): while the phone helper is on, https page loads use the Courier's proxy on the phone (its own connection, not the capped hotspot). OFF by default (closed work, SPEED3.md). */
    var pagesViaPhone: Boolean get() = sp.getBoolean("pages_via_phone", false); set(v) = put("pages_via_phone", v)

    var pullRefresh: Boolean get() = bool("pull_refresh", true); set(v) = put("pull_refresh", v)
    // ---- Network lab additions of the external review (research/REVIEW-EXTERNAL-1.md). Since alpha26 the two with a clear gain and a small stated cost are ON (link look-ups, font fallback); the others stay OFF until measured with Diagnostics
    /** EX-6: let Firefox look up the hosts of the links on https pages before they are tapped (engine default: off for https pages). ON by default since alpha26; never with Data saver, never while a private tab is in front. */
    var labDnsPrefetch: Boolean get() = bool("lab_dns", true); set(v) = put("lab_dns", v)
    /** EX-16: network.buffer.cache.size 32768 instead of GeckoView's 16384 (half as many buffer hand-offs per megabyte at Fast Wi-Fi speeds). */
    var labBuffer: Boolean get() = bool("lab_buf", false); set(v) = put("lab_buf", v)
    /** EX-16: text in a web font shows after 0.5 s in the fallback font instead of staying invisible for up to 3 s. ON by default since alpha26 (readable text sooner on a slow link; the page may jump a little when the font arrives). */
    var labFontDelay: Boolean get() = bool("lab_font", true); set(v) = put("lab_font", v)
    /** EX-12: speculation rules (a site's own "fetch the next page early" hints): 0 off, 1 dry run (candidates recorded, nothing fetched), 2 on. A pre-release engine feature. */
    var labSpec: Int get() = sp.getInt("lab_spec", 0); set(v) = put("lab_spec", v.coerceIn(0, 2))
    /** EX-14: while LinkMeter's Fast Wi-Fi is on, ask websites for WebP / JPEG / PNG instead of AVIF (cheaper to decode on this CPU when bytes are cheap; image.http.accept on the USER branch, cleared when the profile ends). OFF: the owner's Decode lab numbers (AVIF 108 ms vs WebP 50 ms vs JPEG 27 ms per picture, research/EX_RESULTS.md) say AVIF still wins on the 5 Mbit/s hotspot; the lab page was removed afterwards (research/DIAG_TOOLS.md). */
    var labNoAvif: Boolean get() = bool("lab_noavif", false); set(v) = put("lab_noavif", v)
    /** EX-7: hold the Wi-Fi out of power save while Kestrel is on screen (lower, steadier round-trip time; costs battery). */
    var labWifiBoost: Boolean get() = bool("lab_wifi", false); set(v) = put("lab_wifi", v)
    /** EX-5: under LinkMeter's Fast Wi-Fi keep the phone helper (downloads and pages through the Courier) instead of resting it. */
    var labHelperUnderVpn: Boolean get() = bool("lab_helper_vpn", false); set(v) = put("lab_helper_vpn", v)

    // ---- Video (research/VIDEO_AUDIT.md, owner message 18)
    /** Muted videos may start by themselves, as in every other browser (a site's player shows its loading symbol until that muted start happens, so refusing it leaves the symbol up). Sound without a tap stays blocked. ON. */
    var videoMutedAutoplay: Boolean get() = bool("vid_muted_auto", true); set(v) = put("vid_muted_auto", v)
    /** ANS53a / ANS53b (owner message 53: "Never, on any site, ever, anywhere in the app, automatically load a video and start playing it" and "No data should ever be used downloading or buffering videos on web sites unless I press play"): no video loads or starts before the person presses it (kestrel-shields/gate-main.js, the engine's autoplay and preload preferences, no autoplay permission). ON; the switch is the way out for a site that breaks. While it is on [videoMutedAutoplay] does nothing. */
    var videoPressToPlay: Boolean get() = bool("vid_press_play", true); set(v) = put("vid_press_play", v)
    /** ANS54e (owner message 54: "I want the web video players to use this"): where a film of a web page plays: 0 = in Kestrel's own player whenever it can (default), 1 = there only in full screen, 2 = in the page's own player (core/media/WebHandOver). */
    var webVideoMode: Int get() = app.kestrel.core.media.WebHandOver.cleanMode(sp.getInt("web_in_kestrel", app.kestrel.core.media.WebHandOver.ALWAYS)); set(v) = put("web_in_kestrel", app.kestrel.core.media.WebHandOver.cleanMode(v))
    /** ANS55f (owner message 55): a film Kestrel's own player takes over from a page plays INSIDE the page, over the page's own video box (default), instead of in the full-screen player. */
    var webInline: Boolean get() = bool("web_inline", true); set(v) = put("web_inline", v)
    /** Protected video (Widevine DRM, Netflix-style players): 0 ask each time a site wants it (only when the device has Widevine), 1 never. */
    var videoDrm: Int get() = sp.getInt("vid_drm", 0).coerceIn(0, 1); set(v) = put("vid_drm", v.coerceIn(0, 1))
    /** Which codecs a site is offered: 0 automatic (not one that would run in software on this device), 1 H.264 only, 2 everything the engine can decode (core/media/VideoPolicy). */
    var videoCodecs: Int get() = sp.getInt("vid_codecs", 0).coerceIn(0, 2); set(v) = put("vid_codecs", v.coerceIn(0, 2))
    /** ANS52g: sites are told "no" to any video with a stated frame rate above 30 (the page-side policy, codec-main.js / core/media/VideoLimits): half the pictures to decode and draw on a slow tablet and half the data. OFF by default: the tablet's own decoder limits (Automatic above) already apply, this is the owner's A/B switch. */
    var videoFps30: Boolean get() = bool("vid_fps30", false); set(v) = put("vid_fps30", v)
    /** ANS52h: the picture size cap for HLS pages (core/media/PictureCap): 0 Automatic (no picture bigger than the screen can show, with a little to spare), 1 off, 2 480p, 3 720p, 4 1080p. A master playlist is answered without the rungs above it. */
    var videoCap: Int get() = sp.getInt("video_cap", 0).coerceIn(0, 4); set(v) = put("video_cap", v.coerceIn(0, 4))
    /** ANS53d: the picture class (shorter side in pixels: 480, 720, 1080) the owner picked last in Kestrel's own player; 0 = Automatic. The next film starts at no more than that. */
    var nativeMaxH: Int get() = sp.getInt("native_max_h", 0).coerceIn(0, 4320); set(v) = put("native_max_h", v.coerceIn(0, 4320))
    /** FS1 (owner message 43): how the screen turns while a page shows a video full screen: 0 follow the tablet (also with auto-rotate off) and turn a wide video sideways when the tablet is held upright, 1 follow the tablet only, 2 only what a site asks for (core/media/FullscreenTurn). */
    /** ANS54e.1 (owner messages 53 and 54: "even though I was holding my tablet in portrait orientation and didn't rotate it"): the key is `fs_turn2` and the default is FOLLOW THE TABLET, so nobody keeps the old default "a wide video turns the screen sideways by itself" in a saved value (the old key `fs_turn` is not read any more); Automatic stays one tap away in Settings. */
    var fullscreenTurn: Int get() = app.kestrel.core.media.FullscreenTurn.cleanMode(sp.getInt("fs_turn2", app.kestrel.core.media.FullscreenTurn.MODE_FOLLOW)); set(v) = put("fs_turn2", app.kestrel.core.media.FullscreenTurn.cleanMode(v))
    /** FB1 (owner message 47): what a full-screen page does with the system bars: 0 keep them (the page is laid out between the status bar and the navigation bar, nothing of a player is ever under them; the default: the owner's tablet kept drawing both bars over a page that had asked for them to go), 1 hide them (the picture fills the screen; only where the system really hides them). */
    var fullscreenBars: Int get() = sp.getInt("fs_bars", 0).coerceIn(0, 1); set(v) = put("fs_bars", v.coerceIn(0, 1))
    /** VP1 (owner message 49): 0 = Kestrel's own controls over a full-screen video (the default), 1 = the page's own controls. */
    var fsControls: Int get() = sp.getInt("fs_controls", 0).coerceIn(0, 1); set(v) = put("fs_controls", v.coerceIn(0, 1))
    /** Calm pages: sites see "reduce motion" and GIFs play once (the owner's "have no animations", now for pages too). OFF since alpha27: a site that hides its loading symbol from an animation, or whose reduce-motion style leaves one frozen, kept it on screen. */
    var calmPages: Boolean get() = bool("calm_pages", false); set(v) = put("calm_pages", v)
    /** RX4-M0e: count the late screen updates while a real page scrolls (a frame callback that runs only during a scroll) and show the totals in Diagnostics. OFF: nothing runs. */
    var measureScroll: Boolean get() = bool("measure_scroll", false); set(v) = put("measure_scroll", v)
    /** VC1 (owner message 44): Kestrel's own video bar for videos that have no controls: 0 automatic (only where the page shows none), 1 always, 2 off. */
    var videoControls: Int get() = sp.getInt("video_controls", 0).let { if (it in 0..2) it else 0 }; set(v) = put("video_controls", if (v in 0..2) v else 0)

    /** Kestrel G: double-tap to zoom a page. OFF by default: while it is allowed, Firefox holds every tap on a page wider than the screen (Desktop site, no viewport tag) for about a quarter of a second to see whether a second tap follows (research/REVIEW-EXTERNAL-1.md 3.1, EX-3). Pinch zoom is separate and always works. */
    var doubleTapZoom: Boolean get() = bool("tap_zoom", false); set(v) = put("tap_zoom", v)
    /** Kestrel G: zoom the page to a text box that gets focus (the engine's default, so ON). OFF removes an animated zoom + repaint at the moment typing starts (EX-8, an A/B for the typing lag). */
    var inputAutoZoom: Boolean get() = bool("input_zoom", true); set(v) = put("input_zoom", v)
    var searchSuggestions: Boolean get() = bool("suggest", false); set(v) = put("suggest", v) // off: each keystroke would cost data + privacy
    var clearOnExit: Boolean get() = bool("clear_exit", false); set(v) = put("clear_exit", v)
    /** EY-1: the browser opens showing a sharp picture of the page that was left, while the live page loads under it. Pictures stay on the tablet; switching this off deletes them. */
    var lastLook: Boolean get() = bool("last_look", true); set(v) = put("last_look", v)
    /** AR2: the one-hand mini navigation panel at the top right is shown (its toggle remembers the choice). */
    var miniNavOpen: Boolean get() = bool("mini_nav_open", false); set(v) = put("mini_nav_open", v)
    /** AR2: the small toggle for that panel is on screen at all (Settings can remove it from the page's top right corner). */
    var miniNavToggle: Boolean get() = bool("mini_nav_toggle", true); set(v) = put("mini_nav_toggle", v)
    /** AT1: glide typing on the mini keyboard (slide over the letters of a word instead of tapping each one). */
    var glideTyping: Boolean get() = bool("glide_typing", true); set(v) = put("glide_typing", v)
    /** A short tick on every key of the mini keyboard (the system's touch feedback still applies). */
    var keyHaptics: Boolean get() = bool("key_haptics", true); set(v) = put("key_haptics", v)
    /** AV1: where the person put the mini panel and how big the keyboard is (distance from the right edge, from the top, keyboard width, height of the keys, in dp); null = docked at the first size. */
    var miniGeom: IntArray?
        get() = sp.getString("mini_geom", null)?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.size == 4 }?.toIntArray()
        set(v) { if (v == null || v.size != 4) sp.edit().remove("mini_geom").apply() else put("mini_geom", v.joinToString(",")) }
    /** AT1: how this thumb lands beside the key it aims at (in key pitches, x then y), learned from the words that were kept. */
    var glideHabit: FloatArray get() = floatArrayOf(sp.getFloat("glide_bx", 0f), sp.getFloat("glide_by", 0f)); set(v) { if (v.size == 2) sp.edit().putFloat("glide_bx", v[0]).putFloat("glide_by", v[1]).apply() }
    /** AT1: words typed letter by letter that the word list does not have ("word count" per line), so they can be glided next time. */
    var glideWords: String get() = sp.getString("glide_words", "") ?: ""; set(v) = put("glide_words", v)

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

    /** Settings > Back up and restore: the choices that were really made, and taking a backup's choices back (only the keys BackupManager allows, each checked). */
    fun backupSnapshot(): Map<String, Any> = BackupManager.settingsSnapshot(sp)
    fun restoreBackup(m: Map<String, Any>): Int {
        val n = BackupManager.applySettings(sp, m)
        app.kestrel.browser.net.NetworkMonitor.setSaver(dataSaver != DataSaver.OFF) // the one choice that is also held in memory
        return n
    }

    fun globalShields() = ShieldSettings(
        shieldsUp = true, blockAds = shieldAds, cosmetic = shieldCosmetic, https = httpsMode,
        blockThirdPartyCookies = shieldCookies, blockFingerprinting = shieldFingerprint, blockScripts = false, waitEmbeds = shieldEmbeds,
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
