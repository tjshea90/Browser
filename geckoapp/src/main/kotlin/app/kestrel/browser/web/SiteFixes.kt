package app.kestrel.browser.web

import android.content.Context
import android.os.SystemClock
import app.kestrel.browser.GeckoEngine
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.diag.Health
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

/**
 * Mozilla's "Web Compatibility Interventions" extension (webcompat@mozilla.org, MPL-2.0): the per-site fixes stock Firefox for Android carries (User-Agent overrides, small CSS / script
 * patches for unsupported-browser banners, desktop layouts on phones, broken sign-in scripts; 321 cases for Android in the 160 build) and the engine itself does not. research/REVIEW_2026-11-22_NEWLEVERS.md NL1.
 *
 * The extension's files are fetched from Mozilla's Maven at build time (geckoapp/build.gradle.kts, task prepareWebcompat) into assets/webcompat/. Two choices keep the browser light:
 *  - it is started [DELAY_MS] after the FIRST page of the run has finished loading, never on the cold-start path (the owner's tablet is busy enough then);
 *  - its Remote Settings listener is replaced at build time by an empty one, so no new background network starts (the fixes are renewed with the monthly engine bump).
 * A failure to start is only logged: pages then simply load as they did before this feature existed.
 */
object SiteFixes {
    const val ASSET_URI = "resource://android/assets/webcompat/"
    const val EXTENSION_ID = "webcompat@mozilla.org"
    const val DELAY_MS = 12_000L

    @Volatile private var scheduled = false
    @Volatile private var line = "Site fixes: not started yet (they start ${DELAY_MS / 1000} s after the first page has loaded)"

    /** One line for Diagnostics. */
    fun statusLine(): String = line

    /** Called from the main thread when a page finishes loading; the first call per process starts the timer, later calls do nothing. */
    fun afterFirstPage(ctx: Context) {
        if (scheduled) return
        scheduled = true
        val app = ctx.applicationContext
        Io.main.postDelayed({ apply(app) }, DELAY_MS)
    }

    /** Starts or stops the extension to match Settings; also what the switch calls. Main thread. */
    fun apply(ctx: Context) {
        val on = Prefs.get(ctx).siteFixes
        val controller = try { GeckoEngine.runtime(ctx).webExtensionController } catch (e: Throwable) { line = "Site fixes: engine not ready"; return }
        if (on) install(controller) else stopIfRunning(controller)
    }

    private fun install(controller: WebExtensionController) {
        val t0 = SystemClock.uptimeMillis()
        try {
            controller.ensureBuiltIn(ASSET_URI, EXTENSION_ID).accept(
                { ext ->
                    if (ext == null) { line = "Site fixes: the engine returned no extension"; Health.log("sitefixes", "no-extension"); return@accept }
                    val took = SystemClock.uptimeMillis() - t0
                    if (!ext.metaData.enabled) controller.enable(ext, WebExtensionController.EnableSource.APP) // switched off earlier and on again now
                    line = "Site fixes: Mozilla's compatibility fixes ${ext.metaData.version} running (started in $took ms, ${DELAY_MS / 1000} s after the first page)"
                    Health.log("sitefixes", "running", "v=${ext.metaData.version} took=${took}ms")
                },
                { e -> line = "Site fixes: could not start (${e.toString().take(100)}); pages load without them"; Health.log("sitefixes", "failed", e.toString().take(140)) },
            )
        } catch (e: Throwable) { line = "Site fixes: could not start (${e.toString().take(100)})"; Health.log("sitefixes", "failed", e.toString().take(140)) }
    }

    /** The switch is off: only an extension that is already there (kept from an earlier run) is stopped; nothing is installed just to be stopped. */
    private fun stopIfRunning(controller: WebExtensionController) {
        try {
            controller.list().accept(
                { all ->
                    val ext: WebExtension? = all?.firstOrNull { it.id == EXTENSION_ID }
                    if (ext != null && ext.metaData.enabled) { controller.disable(ext, WebExtensionController.EnableSource.APP); Health.log("sitefixes", "stopped") }
                    line = "Site fixes: off (Settings)"
                },
                { e -> line = "Site fixes: off (Settings; could not check: ${e.toString().take(80)})" },
            )
        } catch (e: Throwable) { line = "Site fixes: off (Settings)" }
    }
}
