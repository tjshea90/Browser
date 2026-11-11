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
            .automaticFontSizeAdjustment(false)
            .fontSizeFactor(prefs.textZoom / 100f)
            .preferredColorScheme(colorScheme(prefs))
            .build()
        return GeckoRuntime.create(ctx, settings)
    }

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
