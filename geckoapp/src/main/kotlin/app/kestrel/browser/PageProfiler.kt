package app.kestrel.browser

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import app.kestrel.browser.diag.Health
import app.kestrel.browser.diag.LiveDiag
import app.kestrel.browser.diag.PrivateGuard
import app.kestrel.browser.web.Tab
import app.kestrel.browser.web.isPrivateTab
import app.kestrel.browser.web.session
import app.kestrel.core.livediag.DiagScrub
import app.kestrel.core.livediag.ProfileRule
import org.json.JSONObject
import org.mozilla.geckoview.ProfilerController
import java.io.File

/**
 * ND1d (owner 2026-11-27, "Use the newest diagnostics and improve the browser as much as possible"; research/DIAG_2026-11-27.md section 4): records what the engine spends its time on while a page loads (or for
 * [ProfileRule.NOW_MS] of typing and scrolling), with GeckoView's own profiler, and sends the recording to the PRIVATE repository, so a page like claude.ai (12 to 25 seconds to a first paint, no setting helps) can be
 * looked at instead of guessed at. The page process is invisible to the app's own CPU meter on this tablet (Android 10 hides other users' processes), so only the engine can sample it.
 *
 * Started ONLY by the two Settings buttons (Live diagnostics section), never by itself, never in or next to a private tab, never for a sensitive or local page, only with the owner's switch and token
 * ([ProfileRule.whyNot]). The recording is the engine's own gzip JSON, sent as it is (the file holds the addresses of the page's own files and its script names, no passwords, no typing, no cookies); it
 * goes only to the repository the live diagnostics already use, as `profiles/<date>/<time>-<install>.json.gz` with a small `.meta.json` beside it. A copy of the last one stays in the app's cache.
 * Read with `python3 -B -I tools/profile_digest.py`.
 */
object PageProfiler {
    private class Rec(val tabId: Long, val load: Boolean, val site: String) {
        var sawStart = false; var loaded = false; var stopping = false
        val startedAt = SystemClock.uptimeMillis()
        var hard: Runnable? = null
        var release: Runnable? = null
        var releasedAtMs = -1L
    }

    @Volatile private var rec: Rec? = null

    /** The two buttons: [load] true = reload the page and record its load; false = record [ProfileRule.NOW_MS] of what the person does now. */
    fun start(a: BrowserActivity, tab: Tab?, load: Boolean) {
        val url = tab?.url ?: ""
        val why = ProfileRule.whyNot(tab?.isPrivateTab == true, PrivateGuard.active(), url, LiveDiag.isEnabled(), LiveDiag.hasToken(), rec != null)
        if (tab == null || why.isNotEmpty()) { a.toast(why.ifEmpty { "Open the page you want to record first." }); return }
        val ctx = a.applicationContext
        try {
            if (ProfilerController.isProfilerActive()) { a.toast("A recording is already running."); return }
            ProfilerController.startProfiler(ProfileRule.THREADS, ProfileRule.FEATURES)
        } catch (e: Throwable) {
            Health.log("life", "profile", "refused ${e.javaClass.simpleName}"); a.toast("This engine could not start a recording."); return
        }
        val r = Rec(tab.id, load, DiagScrub.site(url))
        rec = r
        Health.count("profile.start"); Health.log("life", "profile", "start mode=" + (if (load) "load" else "now"))
        val hard = Runnable { finish(ctx, r, "time") }
        r.hard = hard
        Io.main.postDelayed(hard, if (load) ProfileRule.LOAD_MAX_MS else ProfileRule.NOW_MS)
        if (load) {
            // the page process wrote nothing in two recordings (out of memory while it wrote): shortly before the end the engine is told to give memory back (Android's low-memory call, which the memory gate always passes on)
            val release = Runnable {
                if (rec === r && !r.stopping) {
                    r.releasedAtMs = SystemClock.uptimeMillis() - r.startedAt
                    Health.count("profile.release"); Health.log("life", "profile", "release")
                    try { a.application.onLowMemory() } catch (e: Throwable) { /* the recording goes on without it */ }
                }
            }
            r.release = release
            Io.main.postDelayed(release, ProfileRule.LOAD_MAX_MS - ProfileRule.RELEASE_BEFORE_END_MS)
        }
        if (load) {
            a.toast("Recording the first ${ProfileRule.LOAD_MAX_MS / 1000} seconds of this page's load: it reloads now. Please leave Kestrel open until it says it was sent. The page may feel slow for a minute afterwards (the engine is told to give memory back).")
            try { tab.session?.reload() } catch (e: Throwable) { /* the hard stop ends it */ }
        } else a.toast("Recording for ${ProfileRule.NOW_MS / 1000} seconds: use the page now (type, scroll).")
    }

    /** From the page delegate: the page of the recorded tab began loading (after the reload) / finished loading. */
    fun pageStarted(tab: Tab) { val r = rec ?: return; if (r.load && tab.id == r.tabId) r.sawStart = true }
    fun pageStopped(ctx: Context, tab: Tab) {
        val r = rec ?: return
        if (!r.load || tab.id != r.tabId || !r.sawStart || r.loaded) return
        r.loaded = true
        Io.main.postDelayed({ finish(ctx, r, "loaded") }, ProfileRule.LOAD_TAIL_MS)
    }

    private fun finish(ctx: Context, r: Rec, why: String) {
        if (rec !== r || r.stopping) return
        r.stopping = true
        r.hard?.let { Io.main.removeCallbacks(it) }
        r.release?.let { Io.main.removeCallbacks(it) }
        try {
            ProfilerController.stopProfiler().accept({ bytes -> Io.exec { done(ctx, r, bytes, why) } }, { e -> fail(ctx, r, "the engine said " + (e?.message ?: "nothing")) })
        } catch (e: Throwable) { fail(ctx, r, "it could not be stopped (" + e.javaClass.simpleName + ")") }
    }

    private fun fail(ctx: Context, r: Rec, what: String) {
        if (rec === r) rec = null
        Health.count("profile.fail"); Health.log("life", "profile", "failed")
        say(ctx, "No recording: $what.")
    }

    private fun done(ctx: Context, r: Rec, bytes: ByteArray?, why: String) {
        if (rec === r) rec = null
        if (bytes == null || bytes.size < 1000) { Health.count("profile.empty"); Health.log("life", "profile", "empty"); say(ctx, "The engine returned no recording."); return }
        // the rule (core ProfileRule) refused a private tab at the start; one opened (or closed less than 2 minutes ago) while it recorded means the recording covers it too: nothing of it is kept, on the tablet or anywhere
        if (PrivateGuard.active()) { Health.count("profile.discarded"); Health.log("life", "profile", "discarded"); say(ctx, "A private tab was open while recording: the recording was thrown away, nothing was saved or sent."); return }
        val kb = bytes.size / 1024
        Health.count("profile.done"); Health.log("life", "profile", "done why=$why kb=$kb mode=" + (if (r.load) "load" else "now"))
        try { File(ctx.cacheDir, "profile-last.json.gz").writeBytes(bytes) } catch (e: Exception) { /* the copy is a courtesy */ }
        if (bytes.size > ProfileRule.MAX_BYTES) { say(ctx, "The recording is $kb KB: too big to send. It stays on this tablet."); return }
        val missing = ProfileRule.pageProcessMissing(bytes)
        if (missing) { Health.count("profile.nopage"); say(ctx, "The page itself is missing from this recording (its process ran out of memory while it wrote its part). It is sent anyway: the rest still helps. Closing other tabs first may help.") }
        LiveDiag.sendProfile(System.currentTimeMillis(), bytes, meta(ctx, r, why, kb, missing)) { msg -> say(ctx, msg) }
    }

    /** The companion file: which build, which settings, which kind of recording, which site (a sensitive one is never named: it cannot be recorded). Nothing else. */
    private fun meta(ctx: Context, r: Rec, why: String, kb: Int, pageMissing: Boolean): String = JSONObject().apply {
        val pi = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0) } catch (e: Exception) { null }
        put("v", 1); put("kind", "gecko-profile"); put("app", pi?.versionName ?: "?"); @Suppress("DEPRECATION") put("code", pi?.versionCode ?: 0); put("android", Build.VERSION.SDK_INT); put("model", Build.MODEL ?: "?")
        put("mode", if (r.load) "load" else "now"); put("window_ms", if (r.load) ProfileRule.LOAD_MAX_MS else ProfileRule.NOW_MS); put("ended", why); put("site", r.site); put("kb", kb); put("ms", SystemClock.uptimeMillis() - r.startedAt)
        put("page_process", if (pageMissing) "missing" else "present"); if (r.releasedAtMs >= 0) put("release_at_ms", r.releasedAtMs)
        put("settings", JSONObject(LiveDiag.settingsMap()))
        put("features", ProfileRule.FEATURES.joinToString(",")); put("threads", ProfileRule.THREADS.joinToString(","))
    }.toString()

    private fun say(ctx: Context, msg: String) = Io.ui { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
}
