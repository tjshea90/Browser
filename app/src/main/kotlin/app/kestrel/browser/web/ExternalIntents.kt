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
            // never let a page choose the exact component or smuggle a selector intent
            intent.component = null
            intent.selector = null
            intent.sourceBounds = null
            val dataScheme = intent.data?.scheme?.lowercase()
            if (dataScheme != null && dataScheme in BLOCKED) return null
        } else {
            intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.flags = (intent.flags and GRANT_FLAGS.inv()) or Intent.FLAG_ACTIVITY_NEW_TASK
        val label = try {
            ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.loadLabel(ctx.packageManager)?.toString()
        } catch (e: Exception) { null }
        return Plan(intent, label, fallback, needsConfirm = scheme !in DIRECT)
    }
}
