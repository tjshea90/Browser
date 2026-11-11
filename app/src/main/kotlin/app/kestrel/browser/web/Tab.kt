package app.kestrel.browser.web

import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import app.kestrel.core.shield.ShieldSettings
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLong

/** What the native error screen should say. Shown over the page area while [Tab.error] is set. */
sealed class ErrorScreen {
    class Network(val failingUrl: String, val code: Int, val description: String) : ErrorScreen()
    class Ssl(val url: String, val host: String, val summary: String, val issuer: String) : ErrorScreen()
    class HttpsUnavailable(val httpUrl: String, val host: String) : ErrorScreen()
    class Dangerous(val url: String, val host: String, val category: Int) : ErrorScreen()
    class Crashed(val url: String) : ErrorScreen()
}

/**
 * One browser tab. UI-thread state is plain; the few fields read by WebView IO threads inside shouldInterceptRequest
 * ([topHost], [shields], [pageAllowed], counters) are volatile/atomic.
 */
class Tab(@JvmField val id: Long) {
    // ---- UI-thread state
    var url: String = "" // "" = new-tab page
    var title: String = ""
    var webView: WebView? = null // WebView build only
    var engineData: Any? = null // Gecko build only: its per-tab session state (this shared class must not name Gecko types)
    var savedState: Bundle? = null // WebView state while hibernated
    var progress = 0
    var loading = false
    var canGoBack = false
    var canGoForward = false
    var parentId = 0L
    var desktop = false
    var lastActive = System.currentTimeMillis()
    var favicon: Bitmap? = null
    var error: ErrorScreen? = null
    var pendingLoad: String? = null // restored from the session but not loaded yet (lazy: no network at startup)
    var cacheFirstOnce = false // next load prefers the HTTP cache (restoring / hibernated tabs cost no data)
    var injectToken = 0 // per-page-load random token naming the page-side idempotence marker (0 = nothing injected yet); see ScriptInjector
    var savedCopyOnce = false // next load may ONLY use the HTTP cache ("Open saved copy" on the hotspot-has-no-internet page)
    var isFreshPopup = false // opened by a page: its first navigation is checked against $popup rules
    var openerHost: String? = null
    var upgradedFrom: String? = null // http URL we rewrote to https (needed for the strict fallback screen)
    var upgradedTo: String? = null
    var crashCount = 0
    var lastCrash = 0L
    var imagesAllowedOnce = false // STRICT data saver: user tapped "Load images" for this page
    val sslBypassHosts = HashSet<String>()
    val httpOkHosts = HashSet<String>()
    val dangerOkHosts = HashSet<String>()
    var findOpen = false
    var showHome = false // the Home screen is laid over this tab's page (the Home button); the page's session stays alive underneath
    var thumb: Bitmap? = null // small snapshot of the page for the tab grid (RGB_565, about 240 px wide); never written to disk for a private tab
    var thumbAt = 0L // uptime millis of that snapshot
    var dialogCount = 0 // JS alert/confirm/prompt count for this page load (spam guard)
    var dialogsSuppressed = false

    val isNewTab: Boolean get() = url.isEmpty() && pendingLoad == null
    val displayTitle: String get() = if (title.isNotBlank()) title else if (url.isEmpty()) "New tab" else url

    // ---- shared with WebView IO threads
    @Volatile @JvmField var topHost: String? = null
    @Volatile @JvmField var mainUrl: String? = null // URL of the most recent main-frame request (to tell main-frame SSL errors from subresource ones)
    @Volatile @JvmField var shields: ShieldSettings = ShieldSettings()
    @Volatile @JvmField var pageAllowed = false
    @JvmField val blocked = AtomicIntegerArray(4) // per Category, this page
    @JvmField val pendingLifetime = AtomicLong(0) // blocked since last flush to preferences
    @JvmField val shieldDirty = AtomicBoolean(false)

    fun blockedTotal(): Int { var n = 0; for (i in 0 until 4) n += blocked.get(i); return n }
    fun resetPageCounters() { for (i in 0 until 4) blocked.set(i, 0) }
}
