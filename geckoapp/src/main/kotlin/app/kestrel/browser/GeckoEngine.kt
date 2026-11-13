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
    @Volatile private var appCtx: Context? = null
    @Volatile private var exec: org.mozilla.geckoview.GeckoWebExecutor? = null

    /** Firefox's network stack for the download manager; null until the engine runs in this process (the activity creates it right after the first frame). */
    fun executorOrNull(): org.mozilla.geckoview.GeckoWebExecutor? = exec ?: rt?.let { r -> synchronized(this) { exec ?: org.mozilla.geckoview.GeckoWebExecutor(r).also { exec = it } } }

    /**
     * Opens the connection (DNS, TCP, TLS) to [url]'s origin in Firefox's own network stack. GeckoView 160's public call, GeckoWebExecutor.speculativeConnect, does nothing once the engine
     * runs: its native side (widget/android/nsAppShell.cpp) returns at once unless it is called ON Gecko's main thread, and the Java wrapper calls it on the caller's thread. So the call is
     * dispatched there (research/REVIEW-EXTERNAL-1.md 2.2, EX-2). Both classes are GeckoView internals (org.mozilla.gecko): a newer GeckoView that moves them fails to compile here, which is the
     * signal; the monthly engine check (HANDOFF.md section 2 step 0) must confirm that "speculativeConnect still compiles and still connects" (each call is an `engine warm` line in the Diagnostics events). A head start only:
     * every failure is swallowed. Gecko itself skips it behind a proxy (the Courier PAC), by design.
     * @return true when the call was handed to the engine
     */
    fun speculativeConnect(url: String): Boolean {
        if (rt == null) return false
        return try {
            val gecko = org.mozilla.gecko.util.XPCOMEventTarget.mainThread() ?: return false
            gecko.execute { try { org.mozilla.gecko.GeckoThread.speculativeConnect(url) } catch (e: Throwable) { /* only a head start */ } }
            app.kestrel.browser.diag.Health.log("engine", "warm", url.take(80))
            true
        } catch (e: Throwable) { false } // engine not up yet, or an internal class moved in a newer GeckoView
    }

    /** True while a private tab is in front (BrowserActivity.applySkin): the look-ups of links ahead (EX-6) pause then. */
    @Volatile var frontPrivate = false
        private set
    /** True while pages go through the phone helper's proxy (set with the network profile): the engine skips warm-ups behind a proxy, so the app does not ask for them. */
    @Volatile var pagesViaProxy = false
        private set

    fun setFrontPrivate(priv: Boolean, prefs: Prefs) {
        if (frontPrivate == priv) return
        frontPrivate = priv
        if (rt != null) applyLab(prefs)
    }

    /** `https://host[:port]/` of an https address, the origin a warm-up should open (a suggestion with its own port is warmed on THAT port); null for anything else. */
    fun warmOrigin(url: String): String? {
        if (!url.startsWith("https://", true)) return null
        val host = app.kestrel.core.url.UrlParts.hostOf(url) ?: return null
        val port = try { android.net.Uri.parse(url).port } catch (e: Exception) { -1 }
        return "https://" + host + (if (port > 0 && port != 443) ":$port" else "") + "/"
    }

    /**
     * Before the engine's profile is ready GeckoView QUEUES a speculative connect and runs it on the Gecko thread itself, so it needs no dispatch (research/REVIEW-EXTERNAL-1.md 2.2 B): called while
     * the app starts, it lets DNS, TCP and TLS to the page that is about to load (a link that launched the app, the page that was in front) overlap with the content process starting. After the
     * profile is ready the engine drops a call made from any other thread, so this is harmless later. Only for pages that load anyway: it opens nothing the owner did not ask for.
     */
    fun warmAtStart(origin: String) {
        try { org.mozilla.gecko.GeckoThread.speculativeConnect(origin); app.kestrel.browser.diag.Health.log("engine", "warm-start", origin.take(80)) } catch (e: Throwable) { /* only a head start */ }
    }

    fun runtime(ctx: Context): GeckoRuntime = rt ?: synchronized(this) { rt ?: create(ctx.applicationContext).also { rt = it } }

    private fun create(ctx: Context): GeckoRuntime {
        appCtx = ctx
        val prefs = Prefs.get(ctx)
        // entries the engine dropped sit in cache2/doomed until something unlinks them, and a restart does not (seen on the emulator: 48 MB across a restart): nothing can hold them, so take the room back (off the UI thread)
        // AO4: once a day (and after an update) the engine's cache entries nobody has rewritten for three weeks and its old crash reports go too, before the engine reads them
        Io.exec { try { app.kestrel.browser.diag.StorageInfo.purgeDoomedCache(ctx); if (app.kestrel.browser.diag.StorageTidy.due(ctx)) app.kestrel.browser.diag.StorageTidy.pruneEngineFiles(ctx) } catch (e: Exception) { /* only housekeeping */ } }
        // Firefox's Strict set WITHOUT its CONTENT category (cdn.jsdelivr.net, unpkg.com, JW Player, Brightcove, Kaltura, Vimeo ...): those are what pages are built from, not trackers, and without Mozilla's allow-lists
        // blocking them broke doraby.com (no photos, no icons) and video players (research/DORABY.md, core/shield/EtpCategories). Debug builds only: `setprop debug.kestrel.etpcontent 1` brings the old setting back to compare.
        val antiTracking = if (app.kestrel.browser.diag.DebugProps.get(ctx, "debug.kestrel.etpcontent") == "1") ContentBlocking.AntiTracking.STRICT
            else ContentBlocking.AntiTracking.STRICT and ContentBlocking.AntiTracking.CONTENT.inv()
        val blocking = ContentBlocking.Settings.Builder()
            .antiTracking(antiTracking)
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
            .doubleTapZoomingEnabled(prefs.doubleTapZoom) // off: a tap reaches the page at finger-up instead of ~300 ms after touch-down on wide pages (EX-3)
            .inputAutoZoomEnabled(prefs.inputAutoZoom)    // the engine's own default (on); Settings can turn the animated zoom-to-field off (EX-8)
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
            applyLab(prefs)
            applyVideo(ctx, prefs)
            // DEBUG builds only (the emulator experiments): `setprop debug.kestrel.pref1 "name=value"` (also pref2, pref3) sets an engine preference on the USER branch at every start;
            // true / false, a whole number, anything else is a string. DebugProps answers null in a release build, so nothing here can reach the owner.
            for (k in listOf("debug.kestrel.pref1", "debug.kestrel.pref2", "debug.kestrel.pref3")) app.kestrel.browser.diag.DebugProps.get(ctx, k)?.let { kv ->
                val i = kv.indexOf('='); if (i <= 0) return@let
                val n = kv.substring(0, i); val v = kv.substring(i + 1); val u = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_USER
                try { when { v == "true" || v == "false" -> org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref(n, v == "true", u); v.toIntOrNull() != null -> org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref(n, v.toInt(), u); else -> org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref(n, v, u) } } catch (e: Exception) { /* debug only */ }
            }
            // the device's decoder list: from the stored answer at once (above), from Android (off the UI thread) when this system build has not been asked yet; a changed answer re-applies the codec rule
            if (DeviceVideo.hardware(ctx) == null) Io.exec { if (DeviceVideo.read(ctx)) applyVideo(ctx, Prefs.get(ctx)) }
            // DEBUG builds only (the emulator rig, research/TYPING.md): `setprop debug.kestrel.iwvisual 0` makes the keyboard resize the page's LAYOUT, `1` only the visible area (read back in Diagnostics)
            app.kestrel.browser.diag.DebugProps.get(ctx, "debug.kestrel.iwvisual")?.let { v ->
                if (v == "0" || v == "1") try { org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("dom.interactive_widget_default_resizes_visual", v == "1", org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_DEFAULT) } catch (e: Exception) { /* debug only */ }
            }
            app.kestrel.browser.download.PageProxy.setApplier(PhoneProxyApplier)   // pages through the phone: the current state (proxy or direct) is applied now and on every change
        }
    }

    /**
     * "Pages through the phone": a PAC file (a data: URL) that sends https and wss to the Courier's proxy on the phone with DIRECT as the fallback, set live on the default branch (never
     * persisted, so every start begins direct until the phone is confirmed). The failover timeout is short so a phone that comes back is used again within seconds.
     */
    private object PhoneProxyApplier : app.kestrel.browser.download.PageProxy.Applier {
        // DEBUG builds only: `setprop debug.kestrel.pac <PAC data: URL>` replaces the generated script (the emulator test rig checks the fallback to DIRECT with it)
        override fun apply(host: String, port: Int) = set(appCtx?.let { app.kestrel.browser.download.PhoneBridge.debugProp(it, "debug.kestrel.pac") } ?: app.kestrel.browser.download.PageProxy.pacUrl(host, port), 2)
        // DEBUG builds only: `setprop debug.kestrel.pac.off <PAC data: URL>` keeps a proxy script in force even while the helper is off (tests what the engine does when the phone refuses it)
        override fun clear() { val dbg = appCtx?.let { app.kestrel.browser.download.PhoneBridge.debugProp(it, "debug.kestrel.pac.off") }; if (dbg != null) set(dbg, 2) else set("", 5) }
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
            "javascript.options.gc_on_memory_pressure", "layers.gpu-process.enabled", "gfx.webrender.software",
            // the speed batch of config v4 (research/SPEED2.md section 6): name=value(type) so a wrong type shows up as an unchanged default
            "media.cache_readahead_limit", "media.cache_resume_threshold", "browser.sessionhistory.max_total_viewers", "browser.sessionhistory.contentViewerTimeout",
            "browser.cache.memory.capacity", "image.mem.decode_bytes_at_a_time", "layout.framevisibility.numscrollportheights", "layout.framevisibility.numscrollportwidths",
            "dom.lazy-loading.margin.top", "dom.lazy-loading.margin.bottom", "gfx.webrender.precache-shaders", "network.proxy.type", "network.proxy.failover_timeout", "network.proxy.autoconfig_url",
            // the Fast Wi-Fi profile (a VPN is the active network) and the back/forward cache blockers (research/SPEED3.md): their engine defaults are read here before any is changed
            "network.trr.mode", "network.http.http3.enable", "network.dns.disableIPv6", "dom.interactive_widget_default_resizes_visual", "docshell.shistory.bfcache.allow_unload_listeners", "docshell.shistory.bfcache.require_no_opener",
            "docshell.shistory.bfcache.ship_allow_beforeunload_listeners", "browser.sessionhistory.max_entries", "network.dnsCacheExpiration", "network.dnsCacheEntries",
            "javascript.options.mem.gc_parallel_marking", "dom.script_loader.bytecode_cache.strategy",
            // the external review's rows (research/REVIEW-EXTERNAL-1.md): JPEG XL off in the image Accept header, double-tap zoom and zoom-to-field as the app set them
            "image.jxl.enabled", "image.http.accept", "apz.allow_double_tap_zooming", "formhelper.autozoom", "dom.prefetch_dns_for_anchor_https_document", "network.buffer.cache.size", "gfx.downloadable_fonts.fallback_delay",
            "dom.speculation_rules.enabled", "dom.speculation_rules.prefetch.dry_run",
            // video, graphics and JavaScript (research/VIDEO_AUDIT.md): what the engine really runs with on this device; a name this engine does not have is simply left out of the line
            "media.mediasource.vp9.enabled", "media.hevc.enabled", "media.android-media-codec.enabled", "media.android-media-codec.preferred", "media.hls.enabled", "media.eme.enabled", "media.eme.require-app-approval",
            "media.autoplay.default", "media.autoplay.blocking_policy", "media.geckoview.autoplay.request", "media.rdd-process.enabled", "media.utility-process.enabled", "media.video-queue.default-size",
            "media.suspend-background-video.enabled", "ui.prefersReducedMotion", "image.animation_mode", "gfx.webrender.all", "gfx.webrender.compositor", "gfx.canvas.accelerated", "webgl.disabled",
            "javascript.options.ion", "javascript.options.baselinejit", "javascript.options.blinterp", "javascript.options.wasm", "javascript.options.wasm_optimizingjit", "network.early-hints.enabled",
            "network.dns.echconfig.enabled", "security.tls.enable_kyber", "network.http.http2.enabled",
            // Firefox's own tracking protection: which lists block (content-track-digest256 = the CDN / player category), and the allow-lists Mozilla's service would fill
            "urlclassifier.trackingTable", "urlclassifier.trackingAnnotationTable", "privacy.trackingprotection.enabled", "privacy.annotate_channels.strict_list.enabled",
            "privacy.trackingprotection.allow_list.baseline.enabled", "privacy.trackingprotection.allow_list.convenience.enabled")
        try {
            org.mozilla.geckoview.GeckoPreferenceController.getGeckoPrefs(names).accept({ list ->
                prefReadback = "Engine prefs: " + (list?.joinToString(", ") { "${shortPref(it.pref)}=${it.value.toString().take(if (it.pref.startsWith("urlclassifier")) 220 else 40)}(${when (it.type) { org.mozilla.geckoview.GeckoPreferenceController.PREF_TYPE_BOOL -> "b"; org.mozilla.geckoview.GeckoPreferenceController.PREF_TYPE_INT -> "i"; org.mozilla.geckoview.GeckoPreferenceController.PREF_TYPE_STRING -> "s"; else -> "?" }})" } ?: "none")
                done()
            }, { e -> prefReadback = "Engine prefs: read failed ($e)"; done() })
        } catch (e: Exception) { prefReadback = "Engine prefs: unavailable ($e)"; done() }
    }

    /** The last parts of a pref name for the read-back lines: "jxl.enabled", not the ambiguous "enabled". */
    private fun shortPref(name: String): String {
        val parts = name.split('.')
        return if (parts.size >= 2 && parts.last() in setOf("enabled", "size", "delay", "strategy", "capacity", "type", "mode")) parts.takeLast(2).joinToString(".") else parts.last()
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
        r.settings.setDoubleTapZoomingEnabled(prefs.doubleTapZoom)
        r.settings.setInputAutoZoomEnabled(prefs.inputAutoZoom)
        applyScrollFeel(prefs.scrollFeel)
        applyNetwork(prefs)
        applyLab(prefs)
        applyVideo(ctx, prefs)
    }

    /**
     * "Network lab" experiments of the external review (research/REVIEW-EXTERNAL-1.md 3.3, 3.5, 3.6): default-branch prefs, applied at every start and live from Settings. A default-branch pref cannot be
     * unset, so each one is set to the engine's own 160 value when its switch is off (checked against StaticPrefList.yaml / geckoview-prefs.js; the engine is pinned and the monthly bump re-checks them).
     *  - EX-6 `dom.prefetch_dns_for_anchor_https_document` (engine default false): resolve the hosts of links on https pages ahead. Never together with a Data saver level (it is a lookup for every link).
     *  - EX-16 `network.buffer.cache.size` (GeckoView 16384) and `gfx.downloadable_fonts.fallback_delay` (3000 ms): two cheap A/B rows.
     *  - EX-12 speculation rules (`dom.speculation_rules.enabled`, Nightly-only by default) with `prefetch.dry_run` first: 0 off, 1 dry run (candidates recorded, nothing fetched), 2 on.
     */
    fun applyLab(prefs: Prefs) {
        if (hopToMain { applyLab(prefs) }) return // engine prefs need a thread with a Looper (see hopToMain)
        val d = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_DEFAULT
        val saver = prefs.dataSaver != app.kestrel.core.shield.DataSaver.OFF
        try {
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("dom.prefetch_dns_for_anchor_https_document", prefs.labDnsPrefetch && !saver && !frontPrivate, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("network.buffer.cache.size", if (prefs.labBuffer) 32768 else 16384, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("gfx.downloadable_fonts.fallback_delay", if (prefs.labFontDelay) 500 else 3000, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("dom.speculation_rules.enabled", prefs.labSpec > 0, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("dom.speculation_rules.prefetch.dry_run", prefs.labSpec == 1, d)
        } catch (e: Exception) { app.kestrel.browser.diag.Health.log("engine", "lab-prefs-failed", e.toString().take(140)) }
    }

    /** What applyVideo last decided, in words (Diagnostics). */
    @Volatile var videoPolicyText: String = "Video: (not applied yet)"

    /**
     * The engine's preference calls (setGeckoPref / clearGeckoUserPref) send their message and THEN call `.map` on a GeckoResult, which needs a thread with a Looper: on the IO pool they throw
     * "Must have a Handler" after the first preference. alpha27 applied the video rule from the IO pool (right after the decoder probe), so only its first preference arrived and
     * Diagnostics kept saying "decoders unknown" (seen on the owner's tablet). Anything that sets engine prefs from another thread comes through here: true = it was handed to the main thread.
     */
    private fun hopToMain(block: () -> Unit): Boolean {
        if (android.os.Looper.myLooper() === android.os.Looper.getMainLooper()) return false
        Io.main.post(block)
        return true
    }

    /**
     * Video and page animation (research/VIDEO_AUDIT.md, owner message 18), all on the USER branch so "no opinion" is a real "clear" that gives the engine's own value back:
     *  - the codec rule (core/media/VideoPolicy): `media.mediasource.vp9.enabled`, `media.av1.enabled`, `media.hevc.enabled` say which codecs a site is OFFERED (a codec the tablet would decode in software is not);
     *  - "Calm pages": `ui.prefersReducedMotion` = 1 and `image.animation_mode` = "once" only while the switch is on (they were fixed on for every site until alpha26, which can leave a site's loading symbol frozen).
     * Live prefs: they apply to the next media query / the next page without a restart. Main thread only (see [hopToMain]); the Diagnostics text is written FIRST and each preference
     * has its own try, so one refusal can neither hide the decision nor stop the others.
     */
    fun applyVideo(ctx: Context, prefs: Prefs) {
        if (hopToMain { applyVideo(ctx, prefs) }) return
        val u = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_USER
        val hw = DeviceVideo.hardware(ctx)
        val d = app.kestrel.core.media.VideoPolicy.decide(prefs.videoCodecs, hw)
        videoPolicyText = "Video: codecs offered to sites: ${d.why} [hardware decoders ${hw?.encode() ?: "unknown"}]; page animations ${if (prefs.calmPages) "calm (reduce-motion, GIFs once)" else "normal"}; muted autoplay ${if (prefs.videoMutedAutoplay) "allowed" else "blocked"}; protected video ${if (prefs.videoDrm == 0) "ask" else "never"}"
        fun pref(name: String, set: () -> Unit) { try { set() } catch (e: Exception) { app.kestrel.browser.diag.Health.log("engine", "video-pref-failed", "$name ${e.toString().take(100)}") } }
        fun tri(name: String, v: Boolean?) = pref(name) { if (v == null) org.mozilla.geckoview.GeckoPreferenceController.clearGeckoUserPref(name) else org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref(name, v, u) }
        tri("media.mediasource.vp9.enabled", d.vp9)
        tri("media.av1.enabled", d.av1)
        tri("media.hevc.enabled", d.hevc)
        if (prefs.calmPages) {
            pref("ui.prefersReducedMotion") { org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("ui.prefersReducedMotion", 1, u) }
            pref("image.animation_mode") { org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("image.animation_mode", "once", u) }
        } else {
            pref("ui.prefersReducedMotion") { org.mozilla.geckoview.GeckoPreferenceController.clearGeckoUserPref("ui.prefersReducedMotion") }
            pref("image.animation_mode") { org.mozilla.geckoview.GeckoPreferenceController.clearGeckoUserPref("image.animation_mode") }
        }
    }

    /**
     * "Network lab" (Settings): DNS over HTTPS (network.trr.mode 2 = DoH first with a fall back to the system resolver, 0 = off) and HTTP/3. Both are observed prefs, so the next lookup / connection follows.
     * They exist because the hotspot's behaviour decides which is faster (research/GECKO_TUNING_2.md 2.3 and 3.2); Diagnostics > Page loads shows what each choice costs on the real tablet.
     */
    fun applyNetwork(prefs: Prefs) {
        if (hopToMain { applyNetwork(prefs) }) return // engine prefs need a thread with a Looper (see hopToMain)
        val d = org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_DEFAULT
        // the owner's switches, unless a VPN (LinkMeter's Fast Wi-Fi) is the active network: then lookups go to the phone's own resolver and HTTP/3 stays off; behind LinkMeter's OWN tunnel there is also no
        // IPv6 (it is blocked there) and downloads give a loading page more room (core/net/NetProfile.kt, research/SPEED4.md)
        val eff = app.kestrel.core.net.NetProfile.effective(app.kestrel.core.net.NetProfile.Input(dohOn = prefs.dohOn, http3On = prefs.http3On, vpn = app.kestrel.browser.net.NetworkMonitor.vpn,
            helperMode = prefs.phoneMode, pagesViaPhone = prefs.pagesViaPhone, fastTunnel = app.kestrel.browser.net.NetworkMonitor.fastTunnel, keepHelperUnderVpn = prefs.labHelperUnderVpn))
        app.kestrel.browser.download.DownloadController.setPageYieldKBps(eff.pageYieldKBps)
        try {
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("network.trr.mode", eff.trrMode, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("network.http.http3.enable", eff.http3, d)
            org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("network.dns.disableIPv6", eff.disableIPv6, d)
            networkProfile = eff.text
            pagesViaProxy = eff.pagesViaProxy
            // EX-14 (research/REVIEW-EXTERNAL-1.md 3.2): the engine honours `image.http.accept` only when it has a USER value, so this is the one pref set on that branch (and cleared when the profile ends).
            // Without AVIF, CDNs answer WebP / JPEG: bigger files, much less decode work for the four A53 cores; worth it only while bytes are cheap (Fast Wi-Fi).
            if (prefs.labNoAvif && (eff.fastWifi || eff.tunnel)) org.mozilla.geckoview.GeckoPreferenceController.setGeckoPref("image.http.accept", "image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5", org.mozilla.geckoview.GeckoPreferenceController.PREF_BRANCH_USER)
            else org.mozilla.geckoview.GeckoPreferenceController.clearGeckoUserPref("image.http.accept")
            app.kestrel.browser.diag.Health.log("engine", "network-profile", "${if (eff.tunnel) "fast-wifi-tunnel" else if (eff.fastWifi) "fast-wifi" else "standard"} trr=${eff.trrMode} http3=${eff.http3} noIPv6=${eff.disableIPv6} pageYield=${eff.pageYieldKBps}")
        } catch (e: Exception) { app.kestrel.browser.diag.Health.log("engine", "network-lab-failed", e.toString()) }
    }

    /** The profile now in force, in words (Diagnostics and Settings). */
    @Volatile var networkProfile: String = "Standard profile: your own switches apply."

    /**
     * "Scroll feel" (Settings): how far ahead of the finger the engine pre-draws the page and how long it waits for a page's touch handler. These apz.* prefs are
     * live (mirror: always). 0 = the engine's own values on Android; 1 = Smoother (the default): a much bigger pre-drawn area that is kept, biased toward the scroll direction, and a shorter wait for the page's
     * touch handlers; 2 = Smoothest: more of the same and a lower top fling speed (research/SCROLL.md B1-B5 and the R2 update).
     * Values are strings for the float prefs, exactly as GeckoView's own prefs file writes them.
     */
    val scrollFeelNames = listOf("Standard (Firefox's own)", "Smoother", "Smoothest")
    private class Feel(val skate: String, val still: String, val bias: String, val wait: Int, val expiry: Int, val vel: String)

    fun applyScrollFeel(level: Int) {
        if (hopToMain { applyScrollFeel(level) }) return // engine prefs need a thread with a Looper (see hopToMain)
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
