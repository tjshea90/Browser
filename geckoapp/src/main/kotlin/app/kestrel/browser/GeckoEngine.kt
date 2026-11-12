package app.kestrel.browser

import android.content.Context
import app.kestrel.browser.data.Prefs
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * The one Firefox-engine runtime of this process. Privacy settings are built in here (Shields' Gecko half): strict Enhanced Tracking Protection
 * (Disconnect lists: ads, analytics, social, cryptominers, fingerprinters), total cookie protection (third-party cookies isolated per site),
 * query-parameter stripping, bounce-tracking protection, Global Privacy Control. HTTPS-first and the malware list stay in Kestrel's own code
 * (NavPolicy / filter engine) so the per-site switches and warning screens behave exactly as in the WebView build.
 */
object GeckoEngine {
    @Volatile private var rt: GeckoRuntime? = null
    @Volatile private var exec: org.mozilla.geckoview.GeckoWebExecutor? = null

    /** Firefox's network stack for the download manager; null until the engine runs in this process (the activity creates it right after the first frame). */
    fun executorOrNull(): org.mozilla.geckoview.GeckoWebExecutor? = exec ?: rt?.let { r -> synchronized(this) { exec ?: org.mozilla.geckoview.GeckoWebExecutor(r).also { exec = it } } }

    fun runtime(ctx: Context): GeckoRuntime = rt ?: synchronized(this) { rt ?: create(ctx.applicationContext).also { rt = it } }

    private fun create(ctx: Context): GeckoRuntime {
        val prefs = Prefs.get(ctx)
        val blocking = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .strictSocialTrackingProtection(true)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookiePurging(true)
            .queryParameterStrippingEnabled(true)
            .queryParameterStrippingPrivateBrowsingEnabled(true)
            .bounceTrackingProtectionMode(ContentBlocking.BounceTrackingProtectionMode.BOUNCE_TRACKING_PROTECTION_MODE_ENABLED)
            .build()
        val settings = GeckoRuntimeSettings.Builder()
            // The engine preferences (assets/gecko-prefs.txt: AV1 off, memory/cache budget, no animations, security pins) are applied before Gecko starts.
            .configFilePath(GeckoPrefs.writeConfig(ctx))
            .contentBlocking(blocking)
            .allowInsecureConnections(GeckoRuntimeSettings.ALLOW_ALL) // HTTPS-first is Kestrel's own NavPolicy (per-site, LAN-aware, own warning screen)
            .globalPrivacyControlEnabled(true)
            .remoteDebuggingEnabled(false)
            .consoleOutput(false)
            .aboutConfigEnabled(false)
            .loginAutofillEnabled(false)
            .extensionsProcessEnabled(false) // one process less on a 1.9 GB tablet
            .fissionEnabled(false)           // no per-site process isolation: too much RAM for this device
            .lowMemoryDetection(true)
            // Encrypted DNS on a carrier hotspot (carriers can read and rewrite plain DNS). MODE_FIRST = try DoH, fall back to the system resolver.
            .trustedRecursiveResolverMode(if (prefs.dohOn) GeckoRuntimeSettings.TRR_MODE_FIRST else GeckoRuntimeSettings.TRR_MODE_DISABLED)
            .trustedRecursiveResolverUri("https://mozilla.cloudflare-dns.com/dns-query")
            .enterpriseRootsEnabled(false)   // Gecko trusts Mozilla's root store, not this tablet's 2021-07 Android store (research/SECURITY.md 4.1)
            .setLnaEnabled(true).setLnaBlocking(true).setLnaBlockTrackers(true) // Local Network Access: pages cannot probe the LAN / router
            .translationsOfferPopup(false)   // never fetch translation models over the hotspot
            .automaticFontSizeAdjustment(false)
            .fontSizeFactor(prefs.textZoom / 100f)
            .preferredColorScheme(colorScheme(prefs))
            .build()
        return GeckoRuntime.create(ctx, settings).also { r ->
            r.settings.setFetchPriorityEnabled(true)                      // fetchpriority=high/low on img/script/link is honoured (GeckoView's Java default is false)
            r.settings.setFingerprintingProtection(true)                  // FPP in normal tabs too (the engine default is private tabs only)
            r.settings.setFingerprintingProtectionPrivateBrowsing(true)
            r.settings.setCertificateTransparencyMode(2)                  // enforce (= the engine default, pinned so a future default cannot weaken it)
            r.settings.setPostQuantumKeyExchangeEnabled(true)             // X25519MLKEM768 for TLS / HTTP-3 (= default, pinned)
            applyScrollFeel(prefs.scrollFeel)                             // live apz.* prefs of the "Scroll feel" setting
            applyNetwork(prefs)
            app.kestrel.browser.download.PageProxy.setApplier(PhoneProxyApplier)   // pages through the phone: the current state (proxy or direct) is applied now and on every change
        }
    }

    /**
     * "Pages through the phone": a PAC file (a data: URL) that sends https and wss to the Courier's proxy on the phone with DIRECT as the fallback, set live on the default branch (never
     * persisted, so every start begins direct until the phone is confirmed). The failover timeout is short so a phone that comes back is used again within seconds.
     */
    private object PhoneProxyApplier : app.kestrel.browser.download.PageProxy.Applier {
        override fun apply(host: String, port: Int) = set(app.kestrel.browser.download.PageProxy.pacUrl(host, port), 2)
        override fun clear() = set("", 5)
        private fun set(url: String, type: Int) {
            val d = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_DEFAULT
            try {
                org.mozilla.geckoview.GeckoPreferenceController.setGeckoPrefs(listOf(
                    org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference.setStringPref("network.proxy.autoconfig_url", url, d),
                    org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference.setIntPref("network.proxy.failover_timeout", 10, d),
                    org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference.setIntPref("network.proxy.type", type, d),
                ))
                app.kestrel.browser.diag.Health.log("engine", "phone-proxy", if (type == 2) "pages through the phone" else "pages direct")
            } catch (e: Exception) { app.kestrel.browser.diag.Health.log("engine", "phone-proxy-failed", e.toString().take(140)) }
        }
    }

    /** What the engine really has for the preferences we set (Diagnostics): proves assets/gecko-prefs.txt reached it. Call on the UI thread. */
    @Volatile var prefReadback: String = "Engine prefs: (not read yet)"
    /** The engine's own scroll / touch / network defaults on this device (what the tuning in applyScrollFeel starts from): shown in Diagnostics so the numbers come from the tablet, not from the source tree. */
    @Volatile var prefReadback2: String = "Engine scroll prefs: (not read yet)"
    private val scrollPrefs = listOf("apz.touch_start_tolerance", "apz.touch_move_tolerance", "apz.fling_stop_on_tap_threshold", "apz.fling_min_velocity_threshold", "apz.velocity_bias",
        "apz.displayport_expiry_ms", "apz.prefer_jank_minimal_displayports", "apz.min_skate_speed", "apz.allow_immediate_handoff", "apz.fling_friction",
        "apz.android.chrome_fling_physics.friction", "apz.android.chrome_fling_physics.inflexion", "apz.android.chrome_fling_physics.stop_threshold", "apz.paint_skipping.enabled",
        "layout.display-list.retain", "dom.ipc.processPriorityManager.enabled", "browser.cache.memory.capacity", "layout.frame_rate", "network.http.max-connections",
        "network.http.max-persistent-connections-per-server", "network.http.speculative-parallel-limit", "network.http.pacing.requests.enabled", "network.dnsCacheEntries",
        "network.dnsCacheExpiration", "layout.css.scroll-anchoring.enabled", "dom.lazy-loading.margin.top", "image.cache.size")
    fun readBackPrefs(done: () -> Unit) {
        try {
            org.mozilla.geckoview.GeckoPreferenceController.getGeckoPrefs(scrollPrefs).accept({ list ->
                prefReadback2 = "Engine scroll/network prefs: " + (list?.joinToString(", ") { "${it.pref.removePrefix("apz.").removePrefix("network.").removePrefix("android.")}=${it.value}" } ?: "none")
                done()
            }, { e -> prefReadback2 = "Engine scroll/network prefs: read failed ($e)"; done() })
        } catch (e: Exception) { prefReadback2 = "Engine scroll/network prefs: unavailable ($e)"; done() }
        val names = listOf("kestrel.config.version", "media.av1.enabled", "dom.ipc.processCount", "ui.prefersReducedMotion", "network.prefetch-next",
            "image.mem.surfacecache.size_factor", "image.mem.surfacecache.discard_factor", "image.mem.surfacecache.min_expiration_ms", "browser.cache.disk.capacity", "dom.push.enabled",
            "apz.y_skate_size_multiplier", "apz.y_stationary_size_multiplier", "apz.content_response_timeout", "apz.max_velocity_inches_per_ms",
            "javascript.options.gc_on_memory_pressure", "layers.gpu-process.enabled", "gfx.webrender.software")
        try {
            org.mozilla.geckoview.GeckoPreferenceController.getGeckoPrefs(names).accept({ list ->
                prefReadback = "Engine prefs: " + (list?.joinToString(", ") { "${it.pref.substringAfterLast('.')}=${it.value}" } ?: "none")
                done()
            }, { e -> prefReadback = "Engine prefs: read failed ($e)"; done() })
        } catch (e: Exception) { prefReadback = "Engine prefs: unavailable ($e)"; done() }
    }

    /** Launch the content process early ("speeds up the first page load"); idempotent. Call on the UI thread right after the runtime exists. */
    fun warmUp(ctx: Context) { try { runtime(ctx).warmUp() } catch (e: Exception) { /* optional speed-up only */ } }

    private fun colorScheme(prefs: Prefs): Int = when (prefs.webDark) {
        1 -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
        2 -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
    }

    /** Settings that can change while running (text size, dark mode, scroll feel). */
    fun applyPrefs(ctx: Context) {
        val r = rt ?: return
        val prefs = Prefs.get(ctx)
        r.settings.fontSizeFactor = prefs.textZoom / 100f
        r.settings.preferredColorScheme = colorScheme(prefs)
        applyScrollFeel(prefs.scrollFeel)
        applyNetwork(prefs)
    }

    /**
     * "Network lab" (Settings): DNS over HTTPS (network.trr.mode 2 = DoH first with a fall back to the system resolver, 0 = off) and HTTP/3. Both are observed prefs, so the next lookup / connection follows.
     * They exist because the hotspot's behaviour decides which is faster (research/GECKO_TUNING_2.md 2.3 and 3.2); Diagnostics > Page loads shows what each choice costs on the real tablet.
     */
    fun applyNetwork(prefs: Prefs) {
        val d = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_DEFAULT
        try {
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("network.trr.mode", if (prefs.dohOn) 2 else 0, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("network.http.http3.enable", prefs.http3On, d)
            app.kestrel.browser.diag.Health.log("engine", "network-lab", "doh=${prefs.dohOn} http3=${prefs.http3On}")
        } catch (e: Exception) { app.kestrel.browser.diag.Health.log("engine", "network-lab-failed", e.toString()) }
    }

    /**
     * "Scroll feel" (Settings): how far ahead of the finger the engine pre-draws the page and how long it waits for a page's touch handler. These apz.* prefs are
     * live (mirror: always). 0 = the engine's own values on Android; 1 = Smoother (the default): a much bigger pre-drawn area that is kept, biased toward the scroll direction, and a shorter wait for the page's
     * touch handlers; 2 = Smoothest: more of the same and a lower top fling speed (research/SCROLL.md B1-B5 and the R2 update).
     * Values are strings for the float prefs, exactly as GeckoView's own prefs file writes them.
     */
    val scrollFeelNames = listOf("Standard (Firefox's own)", "Smoother", "Smoothest")
    private class Feel(val skate: String, val still: String, val bias: String, val wait: Int, val expiry: Int, val vel: String)

    fun applyScrollFeel(level: Int) {
        val d = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_DEFAULT
        // skate/still = how much of the page is painted around the viewport, bias = how far that window is shifted toward the scroll direction, wait = how long a touch waits for the
        // page's own touch handlers, expiry = how long after scrolling stops the extra painted area is kept (0 = for ever), vel = the top fling speed.
        // The painted area is NOT the multiplier: Gecko scales (multiplier - 1) down by viewport pixels / 250 (this tablet: about 2.9 vertically), so Firefox's 1.5 is only
        // +16 % of the screen height (about 60 px each side) and 4.0 is +96 % (about 370 px each side). research/SCROLL.md "R2 update".
        val f = when (level) {
            1 -> Feel("4.0", "3.0", "1.0", 300, 180000, "0.07")
            2 -> Feel("6.0", "4.0", "2.0", 200, 0, "0.04")
            else -> Feel("1.5", "1.5", "0.0", 600, 15000, "0.07")
        }
        val skate = f.skate; val still = f.still; val bias = f.bias; val wait = f.wait; val expiry = f.expiry; val vel = f.vel
        try {
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("apz.y_skate_size_multiplier", skate, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("apz.y_stationary_size_multiplier", still, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("apz.velocity_bias", bias, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("apz.content_response_timeout", wait, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("apz.displayport_expiry_ms", expiry, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("apz.max_velocity_inches_per_ms", vel, d)
            app.kestrel.browser.diag.Health.log("engine", "scroll-feel", "level=$level skate=$skate still=$still bias=$bias wait=$wait expiry=$expiry vel=$vel")
        } catch (e: Exception) { app.kestrel.browser.diag.Health.log("engine", "scroll-feel-failed", e.toString()) }
    }
}
