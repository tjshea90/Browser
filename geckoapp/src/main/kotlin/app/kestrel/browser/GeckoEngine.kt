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
            .trustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_FIRST)
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
        }
    }

    /** What the engine really has for the preferences we set (Diagnostics): proves assets/gecko-prefs.txt reached it. Call on the UI thread. */
    @Volatile var prefReadback: String = "Engine prefs: (not read yet)"
    fun readBackPrefs(done: () -> Unit) {
        val names = listOf("kestrel.config.version", "media.av1.enabled", "dom.ipc.processCount", "ui.prefersReducedMotion", "network.prefetch-next",
            "image.mem.surfacecache.max_size_kb", "browser.cache.disk.capacity", "dom.push.enabled")
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

    /** Settings that can change while running (text size, dark mode). */
    fun applyPrefs(ctx: Context) {
        val r = rt ?: return
        val prefs = Prefs.get(ctx)
        r.settings.fontSizeFactor = prefs.textZoom / 100f
        r.settings.preferredColorScheme = colorScheme(prefs)
    }
}
