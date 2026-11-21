package app.kestrel.browser.diag

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.os.StatFs
import android.util.Base64
import app.kestrel.browser.Io
import app.kestrel.core.livediag.Advice
import app.kestrel.core.livediag.Advisor
import app.kestrel.core.livediag.DiagBatch
import app.kestrel.core.livediag.DiagJournal
import app.kestrel.core.livediag.DiagStats
import app.kestrel.core.livediag.DiagPolicy
import app.kestrel.core.livediag.DiagSchedule
import app.kestrel.core.livediag.DiagScrub
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicBoolean
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
 *  - how often: [DiagSchedule] (at most 4 AUTOMATIC files a day, 6 hours apart, a crash sooner, a failed send waits a day), once the app is in front for 2 minutes; a check that is refused only because of time is asked
 *    again later while the app stays in front (LD3: it was asked once, so a long session sent nothing). "Send now" is the owner's button: it does not use up the automatic day. No timer is re-armed while the app is not in front.
 *  - never an overwrite (LD3): every file is a NEW path, sent WITHOUT the `sha` of an old file (GitHub then refuses to replace one), a taken name gets the next free one, there is no delete, and one upload runs at a time.
 */
object LiveDiag {
    const val REPO = "tjshea90/kestrel-private"
    private const val MAX_JOURNAL = 800
    private const val FLUSH_EVERY = 25
    private const val SEND_DELAY_MS = 120_000L
    private const val RECHECK_MAX_MS = 30 * 60_000L          // while the app stays in front, a time-refused send is looked at again at least this often
    private const val WEEK_MS = 7L * 86_400_000L

    @Volatile private var enabled = true
    @Volatile private var hasToken = false
    private var sp: SharedPreferences? = null
    private var appSp: SharedPreferences? = null          // the browser's own settings ("kestrel"): read for the settings snapshot and the advisor, written only by an accepted suggestion
    private var appCtx: Context? = null
    private var dir: File? = null
    private val stats = DiagStats()
    @Volatile private var extraFacts: (() -> Map<String, Long>)? = null
    @Volatile private var snapshotSupplier: (() -> String)? = null
    private val journal = DiagJournal(MAX_JOURNAL)          // LD3: exact bookkeeping of what is waiting, what was sent, what was left out (core DiagJournal)
    private val roll = DiagStats()                          // LD3: the rolling week the advisor reads (every send folds the table of that upload into it)
    private var rollSince = 0L
    private val lock = Any()
    private var sinceFlush = 0
    private val uploading = AtomicBoolean(false)            // one upload at a time: a double tap or a timer meeting a button cannot send a window twice
    @Volatile private var inFront = false
    @Volatile private var sendQueued = false
    @Volatile private var repoChecked = 0L
    @Volatile private var status = "not started"

    /** Called once at start-up (the UI process only). */
    fun init(ctx: Context) {
        val c = ctx.applicationContext
        sp = c.getSharedPreferences("livediag", Context.MODE_PRIVATE)
        dir = c.filesDir
        appCtx = c
        appSp = c.getSharedPreferences("kestrel", Context.MODE_PRIVATE)
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
        if (!on) sleep() else Io.exec { markRun("recording-on") }
        status = describe()
    }

    /** LD2c: OFF means asleep. The delayed send is cancelled (nothing is left waiting to wake), the suppliers are let go, everything kept is erased. From here every entry point returns at its first line. */
    private fun sleep() {
        Io.main.removeCallbacks(sendTask)
        sendQueued = false
        inFront = false
        snapshotSupplier = null
        erase()
    }

    /** BrowserActivity hands over the numbers only it knows (screen-update counts, the player's counters); read on the UI thread, only while the switch is on. */
    fun setExtraFacts(f: () -> Map<String, Long>) { extraFacts = f }

    /** Health.log calls this for every event: the switch and the whitelist are checked before anything is done. */
    fun observe(layer: String, event: String, line: String) {
        if (!enabled) return
        if (!DiagPolicy.allows(layer, event)) return
        stats.observe(layer, event, line)                // numbers, code-made names and non-sensitive host names only (core DiagStats)
        val t = DiagScrub.line(line)
        if (t == null) { journal.lose(); return }
        journal.add(layer + " " + event, t)              // a noisy kind is thinned and counted, trouble never (core DiagJournal)
        var flush = false
        synchronized(lock) { if (++sinceFlush >= FLUSH_EVERY) { sinceFlush = 0; flush = true } }
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
        if (!enabled || !hasToken) return
        inFront = true
        if (sendQueued) return
        snapshotSupplier = snapshotOnMain
        sendQueued = true
        Io.main.postDelayed(sendTask, SEND_DELAY_MS)
    }

    /** The app left the front: no check is set again from now on (one that is already queued may still run and answer). */
    fun onBack() { inFront = false }

    private val sendTask = Runnable {
        sendQueued = false
        val sup = snapshotSupplier
        if (enabled && sup != null) request(false, sup, null)
    }

    /** A check refused only because of TIME is asked again later while the app is in front (at most every 30 minutes; the wait is re-computed each time). */
    private fun rearm(waitMs: Long, sup: () -> String) {
        if (!enabled || !hasToken || !inFront || waitMs < 0L || sendQueued) return
        snapshotSupplier = sup
        sendQueued = true
        Io.main.postDelayed(sendTask, minOf(waitMs, RECHECK_MAX_MS) + 5_000L)
    }

    /** "Send now": ignores the 6-hour gap, never the switch or the token. [done] gets one plain sentence. Does not use up the automatic day. */
    fun sendNow(snapshotOnMain: () -> String, done: (String) -> Unit) = request(true, snapshotOnMain, done)

    private fun request(manual: Boolean, snapshotOnMain: () -> String, done: ((String) -> Unit)?) {
        val p = sp ?: return
        if (!enabled) { done?.invoke("Live diagnostics is off: nothing is sent."); return }
        if (!hasToken) { done?.invoke("No token yet."); return }
        val now = System.currentTimeMillis()
        val pending = journal.size()
        if (!manual) {
            val day = dayOf(now)
            val st = DiagSchedule.State(true, true, p.getLong("lastSentAt", 0L), if (p.getString("day", "") == day) p.getInt("today", 0) else 0, p.getLong("lastFailAt", 0L), p.getInt("fails", 0), pending, LastRun.previousEndedBadly())
            val why = DiagSchedule.whyNot(st, now)
            if (why != null) { status = describe(why); rearm(DiagSchedule.waitMs(st, now), snapshotOnMain); return }
        }
        if (!uploading.compareAndSet(false, true)) { done?.invoke("A send is already running. Wait a moment."); return }
        try {
            val text = try { snapshotOnMain() } catch (e: Throwable) { "" }       // built on the UI thread by the caller
            val extras = Extras(settingsMap(), facts() + ("manualSend" to (if (manual) 1L else 0L)), stats.kindCounts(), stats.sites())
            val week = weekView()
            val advice = Advisor.advise(extras.settings, week.kindCounts(), week.sites(), extras.device).mapNotNull { DiagScrub.line(it.text()) }
            Io.exec { try { upload(text, extras, advice, now, manual, done) } finally { uploading.set(false) } }
        } catch (e: Throwable) { uploading.set(false); done?.invoke("Could not start the send.") }
    }

    private class Extras(val settings: Map<String, String>, val device: Map<String, Long>, val kinds: Map<String, Long>, val sites: List<app.kestrel.core.livediag.SiteStat>)

    private fun upload(snapshotText: String, x: Extras, advice: List<String>, now: Long, manual: Boolean, done: ((String) -> Unit)?) {
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
            val cut = journal.cut(DiagSchedule.MAX_EVENTS)                         // the newest lines + exactly where they end (LD3: what did not fit is counted, not forgotten)
            val events = cut.lines
            if (events.isEmpty() && lines.isEmpty() && x.kinds.isEmpty()) { done?.invoke("Nothing to send yet."); return }
            val at = utc(now)
            val batch = DiagBatch(installId(p), appVersion(), appCode(), Build.VERSION.SDK_INT, Build.MODEL ?: "?", at, lines, events, cut.lost, x.settings, x.device, x.kinds, x.sites, advice, cut.thinned)
            if (!enabled) return                                                   // switched off while the repository was being asked: not one byte goes
            val json = batch.toJson()
            var code: Int
            var attempt = 0
            var conflicts = 0
            while (true) {
                code = put(DiagBatch.path(at, installId(p), attempt), json, token)   // a NEW path, no `sha`: GitHub refuses to replace an existing file (422) and the next free name is tried
                if (code == 422 && attempt < 2) { attempt++; continue }
                if (code == 409 && conflicts < 1) { conflicts++; Thread.sleep(700); continue }       // the branch moved while the file was being written: once more, same name
                break
            }
            if (code in 200..299) {
                journal.commit(cut)                                                    // exactly what was sent (and what was counted as left out) is gone; newer lines stay
                foldIntoWeek()                                                         // the table of this upload joins the rolling week; the file carried only what was new
                save()
                val day = dayOf(now)
                val e = p.edit().putLong("lastSentAt", now).putInt("fails", 0).putInt("lastLines", lines.size + events.size)
                if (!manual) e.putString("day", day).putInt("today", (if (p.getString("day", "") == day) p.getInt("today", 0) else 0) + 1)   // the owner's own button does not use up the automatic day
                e.apply()
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
    private fun statsFile(): File? = dir?.let { File(it, "livediag.stats") }
    private fun weekFile(): File? = dir?.let { File(it, "livediag.week") }

    private fun load() {
        try {
            val f = file() ?: return
            if (f.isFile) journal.restore(f.readLines(Charsets.UTF_8).takeLast(MAX_JOURNAL).mapNotNull { DiagScrub.line(it) })   // a stored line is scrubbed again on the way in
        } catch (e: Exception) { /* a damaged journal is just empty */ }
        try { statsFile()?.takeIf { it.isFile }?.let { stats.load(it.readText(Charsets.UTF_8)) } } catch (e: Exception) { /* damaged tables are just empty */ }
        try {
            weekFile()?.takeIf { it.isFile }?.let { w ->
                val t = w.readText(Charsets.UTF_8)
                roll.load(t)
                rollSince = t.lineSequence().firstOrNull { it.startsWith("T\t") }?.substringAfter('\t')?.trim()?.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) { /* a damaged week is just empty */ }
        if (rollSince <= 0L) rollSince = System.currentTimeMillis()
        if (System.currentTimeMillis() - rollSince > WEEK_MS) { roll.reset(); rollSince = System.currentTimeMillis() }
        markRun("start")
        status = describe()
    }

    /** LD3: the journal's lines carry only seconds since the process started, and a file holds several runs one after the other. This line says WHEN a run began (real time, build), so Claude can tell the runs apart and date them. */
    private fun markRun(why: String) {
        if (!enabled) return
        val up = try { SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime() } catch (e: Throwable) { 0L }
        val stamp = String.format(Locale.US, "+%.3fs", maxOf(up, 0L) / 1000.0)
        DiagScrub.line("$stamp run $why utc=${utc(System.currentTimeMillis())} build=$versionName code=$versionCode")?.let { journal.add("run $why", it) }
    }

    private fun save() {
        if (!enabled) return
        try {
            val f = file() ?: return
            f.writeText(journal.toList().joinToString("\n"), Charsets.UTF_8)
            statsFile()?.writeText(stats.save(), Charsets.UTF_8)
            weekFile()?.writeText("T\t" + rollSince + "\n" + roll.save(), Charsets.UTF_8)
        } catch (e: Exception) { /* storage full: the next flush tries again */ }
    }

    /** What was sent joins the rolling week (a week old week starts again); the table of the next upload starts empty. */
    private fun foldIntoWeek() {
        val now = System.currentTimeMillis()
        if (rollSince <= 0L || now - rollSince > WEEK_MS) { roll.reset(); rollSince = now }
        roll.mergeFrom(stats)
        stats.reset()
    }

    /** The week the advisor reads: the past uploads of this week plus what has not been sent yet. A new table, nothing shared. */
    private fun weekView(): DiagStats = DiagStats().also { it.mergeFrom(roll); it.mergeFrom(stats) }

    /** Everything this feature keeps on the tablet is removed: the journal in memory and its file, the tables and the week. (The token is the person's to forget: [forgetToken].) */
    fun erase() {
        journal.clear()
        synchronized(lock) { sinceFlush = 0 }
        stats.reset(); roll.reset(); rollSince = 0L
        try { file()?.delete(); statsFile()?.delete(); weekFile()?.delete() } catch (e: Exception) { /* gone */ }
        status = describe()
    }

    // ---- LD2: what the tablet knows about itself, and what it would change

    /** Plain numbers about the device and the session (no text): memory, storage, heat, the link, and what BrowserActivity adds. Empty while the switch is off. */
    fun facts(): Map<String, Long> {
        if (!enabled) return emptyMap()
        val c = appCtx ?: return emptyMap()
        val m = LinkedHashMap<String, Long>()
        try { val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager; val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            m["ramTotalMb"] = mi.totalMem shr 20; m["ramAvailMb"] = mi.availMem shr 20; m["lowMemory"] = if (mi.lowMemory) 1L else 0L; m["lowRamDevice"] = if (am.isLowRamDevice) 1L else 0L } catch (e: Exception) { /* a fact is just missing */ }
        try { m["storageFreeMb"] = StatFs(c.filesDir.path).availableBytes shr 20 } catch (e: Exception) { /* missing */ }
        try { val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager; m["powerSave"] = if (pm.isPowerSaveMode) 1L else 0L; m["thermal"] = pm.currentThermalStatus.toLong() } catch (e: Exception) { /* missing */ }
        try { val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager; val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            if (caps != null) { m["wifi"] = if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) 1L else 0L; m["downKbps"] = caps.linkDownstreamBandwidthKbps.toLong() }
            m["metered"] = if (cm.isActiveNetworkMetered) 1L else 0L } catch (e: Exception) { /* missing */ }
        val a = appSp
        if (a != null) {
            m["phonePaired"] = if (!a.getString("phone_code", "").isNullOrEmpty()) 1L else 0L
            val last = a.getLong("upd_last", 0L); if (last > 0L) m["filterAgeDays"] = (System.currentTimeMillis() - last) / 86_400_000L
        }
        try { m["uptimeS"] = maxOf(SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime(), 0L) / 1000L } catch (e: Throwable) { /* missing */ }
        m["liveFailsInRow"] = (sp?.getInt("fails", 0) ?: 0).toLong()
        m["prevRunEndedBadly"] = if (LastRun.previousEndedBadly()) 1L else 0L
        try { extraFacts?.invoke()?.let { m.putAll(it) } } catch (e: Throwable) { /* the activity's numbers are optional */ }
        return m
    }

    private val SKIP = listOf("phone", "glide", "mini", "dl_geom", "data_base", "data_acc", "upd_sig", "upd_notice", "upd_note", "upd_last", "blocked")
    private val KEY = Regex("[a-z0-9_]{1,32}")
    private val WORD = Regex("[A-Za-z0-9_-]{1,24}")

    /** What the owner chose in Settings: switches and small numbers by name, plus the three named choices (search engine id, HTTPS mode, data saver level). Never an address, a code, a word list or a counter. */
    fun settingsMap(): Map<String, String> {
        if (!enabled) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for ((k, v) in appSp?.all ?: return out) {
            if (!KEY.matches(k) || SKIP.any { k.startsWith(it) }) continue
            when (v) { is Boolean -> out[k] = v.toString(); is Int -> out[k] = v.toString(); is String -> if ((k == "search" || k == "https" || k == "data_saver") && WORD.matches(v)) out[k] = v }
        }
        return out
    }

    /** The suggestions the tablet's advisor would make right now (Settings shows them). Nothing at all, and nothing computed, while the switch is off. */
    fun advice(): List<Advice> {
        if (!enabled) return emptyList()
        val week = weekView()                                  // LD3: a week, not one upload: "this site is slow" needs more than three page loads
        return Advisor.advise(settingsMap(), week.kindCounts(), week.sites(), facts())
    }

    /** One tap: make the change a suggestion names (only the keys the advisor may change, within their range). True when it was done. */
    fun applyAdvice(a: Advice): Boolean {
        if (!enabled) return false
        val k = a.key ?: return false
        val v = a.value
        if (!Advisor.canApply(k, v)) return false
        val e = appSp?.edit() ?: return false
        when (v) { is Boolean -> e.putBoolean(k, v); is Number -> e.putInt(k, v.toInt()); else -> return false }
        e.apply()
        return true
    }

    /** Tests only: what is kept right now, and a clean slate. */
    internal fun journalForTest(): List<String> = journal.toList()
    internal fun weekForTest(): DiagStats = weekView()
    internal fun resetForTest() { Io.main.removeCallbacks(sendTask); journal.clear(); synchronized(lock) { sinceFlush = 0 }; stats.reset(); roll.reset(); rollSince = 0L; enabled = true; hasToken = false; sendQueued = false; inFront = false; uploading.set(false); snapshotSupplier = null; extraFacts = null; sp = null; appSp = null; appCtx = null; dir = null }
    internal fun queuedForTest(): Boolean = sendQueued
    internal fun statsForTest(): DiagStats = stats

    // ---- words for Settings / Diagnostics

    fun statusLine(): String = describe()

    private fun describe(note: String? = null): String {
        val p = sp ?: return "Live diagnostics: not started"
        if (!enabled) return "Live diagnostics: OFF - nothing is recorded, nothing is sent, nothing is kept"
        val n = journal.size()
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
