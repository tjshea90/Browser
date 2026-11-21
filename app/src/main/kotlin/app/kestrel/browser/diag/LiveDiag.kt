package app.kestrel.browser.diag

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import app.kestrel.browser.Io
import app.kestrel.core.livediag.DiagBatch
import app.kestrel.core.livediag.DiagPolicy
import app.kestrel.core.livediag.DiagSchedule
import app.kestrel.core.livediag.DiagScrub
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * LD1 (owner 2026-11-21): "a smart live diagnostics engine ... logs and sends regular diagnostic data to github for claude to review ... only sending website data to the private repo and not logging passwords or
 * payment methods etc ... an off switch that turns all logging completely off and stops all sending". research/LIVE_DIAG.md has the design; the rules that matter:
 *  - ONE master switch, [enabled]. Off = [observe] returns before it does anything, nothing is kept or sent, and what was kept is erased at once.
 *  - what is recorded: only the app's own Health events of the whitelisted layers ([DiagPolicy]) and, at send time, the Diagnostics text; every line goes through [DiagScrub] (links become hosts, sensitive sites are
 *    not named, passwords, tokens, card numbers, e-mail and phone numbers are removed). Private tabs never name a site. Nothing is read from a page, a form or a download.
 *  - where it goes: ONLY the private repo [REPO], through GitHub's contents API with the owner's own fine-grained token (typed into Settings once, kept in this app's private storage, never in the APK, never in a
 *    backup, never in a log). Before the first send of a run the repo is asked whether it is PRIVATE; if it is not, nothing is sent.
 *  - how often: [DiagSchedule] (at most 4 files a day, 6 hours apart, a crash sooner, a failed send waits a day), once the app is in front for 2 minutes. No timer runs while the app is not in use.
 */
object LiveDiag {
    const val REPO = "tjshea90/kestrel-private"
    private const val MAX_JOURNAL = 800
    private const val FLUSH_EVERY = 25
    private const val SEND_DELAY_MS = 120_000L

    @Volatile private var enabled = true
    @Volatile private var hasToken = false
    private var sp: SharedPreferences? = null
    private var dir: File? = null
    private val journal = ArrayDeque<String>()
    private val lock = Any()
    private var dropped = 0
    private var sinceFlush = 0
    @Volatile private var sendQueued = false
    @Volatile private var repoChecked = 0L
    @Volatile private var status = "not started"

    /** Called once at start-up (the UI process only). */
    fun init(ctx: Context) {
        val c = ctx.applicationContext
        sp = c.getSharedPreferences("livediag", Context.MODE_PRIVATE)
        dir = c.filesDir
        enabled = sp!!.getBoolean("on", true)
        hasToken = !sp!!.getString("token", "").isNullOrEmpty()
        try { @Suppress("DEPRECATION") val pi = c.packageManager.getPackageInfo(c.packageName, 0); versionName = pi.versionName ?: "?"; versionCode = pi.versionCode } catch (e: Exception) { /* the batch then says ? */ }
        if (enabled) Io.exec { load() } else erase()
        status = describe()
    }

    fun isEnabled(): Boolean = enabled
    fun hasToken(): Boolean = hasToken

    /** THE off switch. Off: nothing is recorded or sent from this moment and everything stored is erased now. On: recording starts again. */
    fun setEnabled(on: Boolean) {
        enabled = on
        sp?.edit()?.putBoolean("on", on)?.apply()
        if (!on) erase()
        status = describe()
    }

    /** Health.log calls this for every event: the switch and the whitelist are checked before anything is done. */
    fun observe(layer: String, event: String, line: String) {
        if (!enabled) return
        if (!DiagPolicy.allows(layer, event)) return
        val t = DiagScrub.line(line)
        var flush = false
        synchronized(lock) {
            if (t == null) { dropped++; return }
            journal.addLast(t)
            while (journal.size > MAX_JOURNAL) { journal.removeFirst(); dropped++ }
            if (++sinceFlush >= FLUSH_EVERY) { sinceFlush = 0; flush = true }
        }
        if (flush) Io.exec { save() }
    }

    // ---- the token (owner's own, fine-grained, one repo, contents: read and write)

    /** Format check only: a GitHub token is one word of letters, digits and underscores, 20-255 characters. */
    fun tokenLooksRight(t: String): Boolean = t.length in 20..255 && t.all { it.isLetterOrDigit() || it == '_' || it == '-' } && (t.startsWith("github_pat_") || t.startsWith("ghp_") || t.startsWith("gho_"))

    fun saveToken(t: String): Boolean {
        val x = t.trim()
        if (!tokenLooksRight(x)) return false
        sp?.edit()?.putString("token", x)?.putInt("fails", 0)?.putLong("lastFailAt", 0L)?.apply()
        hasToken = true; repoChecked = 0L; status = describe()
        return true
    }

    fun forgetToken() { sp?.edit()?.remove("token")?.apply(); hasToken = false; status = describe() }

    // ---- sending

    /** The app is in front: if the schedule allows, a send follows 2 minutes later (one pending at a time). */
    fun onFront(snapshotOnMain: () -> String) {
        if (!enabled || !hasToken || sendQueued) return
        sendQueued = true
        Io.main.postDelayed({ sendQueued = false; request(false, snapshotOnMain, null) }, SEND_DELAY_MS)
    }

    /** "Send now": ignores the 6-hour gap, never the switch or the token. [done] gets one plain sentence. */
    fun sendNow(snapshotOnMain: () -> String, done: (String) -> Unit) = request(true, snapshotOnMain, done)

    private fun request(manual: Boolean, snapshotOnMain: () -> String, done: ((String) -> Unit)?) {
        val p = sp ?: return
        if (!enabled) { done?.invoke("Live diagnostics is off: nothing is sent."); return }
        if (!hasToken) { done?.invoke("No token yet."); return }
        val now = System.currentTimeMillis()
        val pending = synchronized(lock) { journal.size }
        if (!manual) {
            val day = dayOf(now)
            val st = DiagSchedule.State(true, true, p.getLong("lastSentAt", 0L), if (p.getString("day", "") == day) p.getInt("today", 0) else 0, p.getLong("lastFailAt", 0L), p.getInt("fails", 0), pending, LastRun.previousEndedBadly())
            val why = DiagSchedule.whyNot(st, now)
            if (why != null) { status = describe(why); return }
        }
        val text = try { snapshotOnMain() } catch (e: Throwable) { "" }       // built on the UI thread by the caller
        Io.exec { upload(text, now, done) }
    }

    private fun upload(snapshotText: String, now: Long, done: ((String) -> Unit)?) {
        val p = sp ?: return
        val token = p.getString("token", "") ?: ""
        var msg: String
        try {
            if (!enabled || token.isEmpty()) return
            if (now - repoChecked > 24 * 3600_000L) {
                when (repoState(token)) {
                    RepoState.PRIVATE -> repoChecked = now
                    RepoState.PUBLIC -> { fail(p, now, "the repo is PUBLIC: nothing sent"); done?.invoke("The repository is public. Kestrel sends diagnostics to a PRIVATE repository only: nothing was sent."); return }
                    RepoState.NO_ACCESS -> { fail(p, now, "the token cannot see the repo"); done?.invoke("GitHub says the token cannot see $REPO. Make the token for that one repository with Contents: read and write."); return }
                    RepoState.ERROR -> { fail(p, now, "GitHub not reachable"); done?.invoke("Could not reach GitHub. It will try again later."); return }
                }
            }
            val lines = snapshotText.lineSequence().mapNotNull { DiagScrub.line(it) }.take(DiagSchedule.MAX_SNAPSHOT).toList()
            val (events, drop) = synchronized(lock) { DiagSchedule.newest(journal.toList(), DiagSchedule.MAX_EVENTS) to dropped }
            if (events.isEmpty() && lines.isEmpty()) { done?.invoke("Nothing to send yet."); return }
            val at = utc(now)
            val batch = DiagBatch(installId(p), appVersion(), appCode(), Build.VERSION.SDK_INT, Build.MODEL ?: "?", at, lines, events, drop)
            val code = put(DiagBatch.path(at, installId(p)), batch.toJson(), token)
            if (code in 200..299) {
                synchronized(lock) { repeat(events.size) { if (journal.isNotEmpty()) journal.removeFirst() }; dropped = 0 }
                save()
                val day = dayOf(now)
                p.edit().putLong("lastSentAt", now).putString("day", day).putInt("today", (if (p.getString("day", "") == day) p.getInt("today", 0) else 0) + 1).putInt("fails", 0).putInt("lastLines", lines.size + events.size).apply()
                status = describe(); msg = "Sent ${lines.size + events.size} lines to the private repository."
            } else {
                fail(p, now, "GitHub answered $code"); msg = if (code == 401 || code == 403) "GitHub refused the token ($code): it may have expired or lacks Contents: read and write." else "GitHub answered $code: nothing was sent; it will try again later."
            }
        } catch (e: Exception) { fail(p, now, "could not send"); msg = "Could not send (no connection?). It will try again later." }
        done?.invoke(msg)
    }

    private fun fail(p: SharedPreferences, now: Long, why: String) { p.edit().putLong("lastFailAt", now).putInt("fails", p.getInt("fails", 0) + 1).apply(); status = describe(why) }

    private enum class RepoState { PRIVATE, PUBLIC, NO_ACCESS, ERROR }

    private fun repoState(token: String): RepoState = try {
        val c = URL("https://api.github.com/repos/$REPO").openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 20_000; headers(c, token)
        val code = c.responseCode
        val body = if (code == 200) c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) } else ""
        when {
            code == 200 && Regex("\"private\"\\s*:\\s*true").containsMatchIn(body) -> RepoState.PRIVATE
            code == 200 -> RepoState.PUBLIC
            code == 401 || code == 403 || code == 404 -> RepoState.NO_ACCESS
            else -> RepoState.ERROR
        }
    } catch (e: Exception) { RepoState.ERROR }

    private fun headers(c: HttpURLConnection, token: String) {
        c.setRequestProperty("Authorization", "Bearer $token")
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        c.setRequestProperty("User-Agent", "Kestrel-LiveDiag")
    }

    private fun put(path: String, json: String, token: String): Int {
        val c = URL("https://api.github.com/repos/$REPO/contents/$path").openConnection() as HttpURLConnection
        c.requestMethod = "PUT"; c.connectTimeout = 15_000; c.readTimeout = 30_000; c.doOutput = true
        headers(c, token); c.setRequestProperty("Content-Type", "application/json")
        val content = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        c.outputStream.use { it.write("{\"message\":\"diag\",\"content\":\"$content\"}".toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        try { (if (code in 200..299) c.inputStream else c.errorStream)?.close() } catch (e: Exception) { /* nothing to read */ }
        return code
    }

    // ---- what is stored on the tablet

    private fun file(): File? = dir?.let { File(it, "livediag.journal") }

    private fun load() {
        try {
            val f = file() ?: return
            if (!f.isFile) return
            val lines = f.readLines(Charsets.UTF_8).takeLast(MAX_JOURNAL)
            synchronized(lock) { journal.clear(); for (l in lines) DiagScrub.line(l)?.let { journal.addLast(it) } }   // a stored line is scrubbed again on the way in
        } catch (e: Exception) { /* a damaged journal is just empty */ }
        status = describe()
    }

    private fun save() {
        if (!enabled) return
        try {
            val f = file() ?: return
            val text = synchronized(lock) { journal.joinToString("\n") }
            f.writeText(text, Charsets.UTF_8)
        } catch (e: Exception) { /* storage full: the next flush tries again */ }
    }

    /** Everything this feature keeps on the tablet is removed: the journal in memory and its file. (The token is the person's to forget: [forgetToken].) */
    fun erase() {
        synchronized(lock) { journal.clear(); dropped = 0; sinceFlush = 0 }
        try { file()?.delete() } catch (e: Exception) { /* gone */ }
        status = describe()
    }

    // ---- words for Settings / Diagnostics

    fun statusLine(): String = describe()

    private fun describe(note: String? = null): String {
        val p = sp ?: return "Live diagnostics: not started"
        if (!enabled) return "Live diagnostics: OFF - nothing is recorded, nothing is sent, nothing is kept"
        val n = synchronized(lock) { journal.size }
        val last = p.getLong("lastSentAt", 0L)
        return "Live diagnostics: ON, " + (if (hasToken) "token saved" else "no token yet (nothing leaves the tablet without one)") + ", $n events waiting" +
            (if (last > 0) ", last sent " + utc(last) + " (" + p.getInt("lastLines", 0) + " lines)" else ", nothing sent yet") + (if (note != null) "; $note" else "")
    }

    private fun utc(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))
    private fun dayOf(ms: Long): String = utc(ms).substringBefore('T')
    private fun installId(p: SharedPreferences): String = p.getString("install", null) ?: UUID.randomUUID().toString().replace("-", "").take(12).also { p.edit().putString("install", it).apply() }
    private var versionName = "?"; private var versionCode = 0
    private fun appVersion() = versionName
    private fun appCode() = versionCode
}
