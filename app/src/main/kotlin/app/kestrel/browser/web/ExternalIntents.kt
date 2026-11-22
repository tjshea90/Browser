package app.kestrel.browser.web

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Turns a non-web URL a page tried to open (`intent:`, `mailto:`, custom schemes) into a SAFE explicit plan, or refuses it.
 * Web pages must never be able to start arbitrary components, grant URI permissions, or reach local files.
 */
object ExternalIntents {
    class Plan(val intent: Intent, val appLabel: String?, val fallbackUrl: String?, val needsConfirm: Boolean)

    private val BLOCKED = setOf("file", "content", "javascript", "data", "vbscript", "view-source", "filesystem", "blob", "about", "chrome", "ftp")
    private val DIRECT = setOf("mailto", "tel", "sms", "smsto", "geo")

    /** The only actions a page may ask an app for through an `intent:` link: open it, write to it, dial a number, share text. Anything else (settings screens, delete / install, call, ...) is refused. */
    private val ACTIONS = setOf(Intent.ACTION_VIEW, Intent.ACTION_SENDTO, Intent.ACTION_DIAL, Intent.ACTION_SEND)
    private const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

    /**
     * Never throws: [Intent.parseUri] lets NumberFormatException and friends escape for crafted input like `intent:#Intent;i.x=zz;end`,
     * and this runs on the UI thread for URLs chosen by a web page. Anything unparseable is simply refused.
     */
    fun plan(ctx: Context, url: String): Plan? = try { planUnchecked(ctx, url) } catch (e: Exception) { null }

    private fun planUnchecked(ctx: Context, url: String): Plan? {
        val scheme = url.substringBefore(':', "").lowercase()
        if (scheme.isEmpty() || scheme in BLOCKED) return null
        val intent: Intent
        var fallback: String? = null
        if (scheme == "intent") {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME) // URISyntaxException / NumberFormatException are caught by plan()
            fallback = intent.getStringExtra("browser_fallback_url")?.takeIf { it.startsWith("https://") }
            val action = intent.action
            if (action != null && action !in ACTIONS) return null
            // never let a page choose the exact component or smuggle a selector intent
            intent.component = null
            intent.selector = null
            intent.sourceBounds = null
            val dataScheme = intent.data?.scheme?.lowercase()
            if (dataScheme != null && dataScheme in BLOCKED) return null
            // RVW1 S8: an `intent:` link is for OTHER apps. Aimed at Kestrel itself it could drive its own screens through extras (the extra names of the Updates sheet and its install button are public constants in this open
            // repository): refused when it names this app as its package, or carries any extra in this app's own namespace.
            if (intent.`package` == ctx.packageName) return null
            val keys = intent.extras?.keySet()
            if (keys != null && keys.any { it.startsWith("app.kestrel.") }) return null
        } else {
            intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK   // RVW1 S8: the page's own launch flags (CLEAR_TASK, CLEAR_TOP, NO_HISTORY ...) are not honoured, only a new task; grant flags were already removed here
        val label = try {
            ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.loadLabel(ctx.packageManager)?.toString()
        } catch (e: Exception) { null }
        return Plan(intent, label, fallback, needsConfirm = scheme !in DIRECT)
    }
}
