package app.kestrel.browser.web

import android.os.SystemClock
import android.view.View
import app.kestrel.browser.Io
import app.kestrel.browser.diag.Health
import app.kestrel.core.player.PlayerModel
import app.kestrel.core.player.FilmHealth
import app.kestrel.core.player.PlayerRules
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * VP1 (owner message 49): the Android half of Kestrel's own full-screen video controls: gives the pure [PlayerModel] its clock and its timers (the UI thread's handler), draws it through
 * [PlayerControlsView], and talks to the page script (`player.js`) through the messages the extension relays: the page tells the film's state (`vst`) and the taps on the picture (`vtap`), and
 * the ANSWER to each carries what the controls want done and whether they are shown. So the page needs no second channel and the app never has to find the page.
 *
 * Threads: the messages arrive on a Shields worker thread; [onMessage] is called on the UI thread (the activity posts it), [reply] is called on the worker thread right away: it only reads
 * a volatile flag and drains a concurrent queue of ready JSON, both filled on the UI thread. Everything else is UI thread only.
 */
class PlayerController(
    private val view: PlayerControlsView,
    /** Leave full screen (the exit button). */
    private val exitFullscreen: () -> Unit,
    /** The rotate button follows the controls: shown with them, gone with them. */
    private val rotateButton: (Boolean) -> Unit,
    /** ANS53d.2: does the page's film offer pictures to pick from (the Quality button is drawn only then)? Asked when the controls start and when the activity says the media list changed ([refreshQuality]). */
    private val qualityAvailable: () -> Boolean = { false },
    /** ANS53d.2: the Quality button was pressed; the film's position and length in seconds as the page last said them (0 / -1 = not known). */
    private val qualityPressed: (Double, Double) -> Unit = { _, _ -> },
    /** ANS53d.2: a sheet is open over the controls: touches on the glass are its, not taps on the picture. */
    private val sheetOpen: () -> Boolean = { false },
) : PlayerModel.Host, PlayerControlsView.Listener {
    /** The full screen belongs to a private tab: nothing of it is logged or counted (the person's film is nobody's business, not even Diagnostics'). Set by the activity before [fullscreen]. */
    @Volatile var quiet = false
    private fun say(event: String, text: String) { if (!quiet) Health.log("player", event, text) }

    private val model = PlayerModel(this)
    private val controlsTick = Runnable { model.controlsTimer(); flush() }
    private val ticker = object : Runnable { override fun run() { tickerOn = false; refresh() } }
    private var tickerOn = false
    private val commands = ConcurrentLinkedQueue<String>()
    @Volatile private var shownFlag = false
    @Volatile private var activeFlag = false       // a film is under the controls (the page has reported it)
    @Volatile private var armed = false         // a page is full screen and the person wants Kestrel's controls
    private var started = 0L
    private var lastReportAt = 0L
    private val watchdog = object : Runnable { override fun run() { watchOn = false; checkAlive() } }
    private var watchOn = false

    // ANS53c (owner message 53: "zero delay"): a tap on the picture is seen on the glass itself by the activity and answered at once; the page's own message for the same tap (it comes through the page's script thread, the
    // extension and the app: a few tens of ms on a quiet page, seconds on a busy one) is matched by the time of the touch and dropped, and the tap that was on one of the page's own buttons is put back.
    private val appTapEpoch = LongArray(4)
    private var appTapIdx = 0
    private var lastAppTapUptime = 0L
    private var prevShown = false
    private var lastToggled = false
    private var appTaps = 0; private var dupTaps = 0; private var undoneTaps = 0; private var appTapMsTotal = 0L; private var appTapMsWorst = 0L

    // what Diagnostics says
    private var reports = 0
    private var taps = 0
    private var away = 0                           // page elements (its own player controls) put away right now
    private var awayMax = 0
    private var last = "no full-screen film under Kestrel's controls yet"
    /** ANS51e: how the page's script and the film did (stalls, the player script's own cost, frames shown and dropped), measured inside the page. */
    private val health = FilmHealth()
    private var lookPage = ""
    private var lookShadow = false     // the film sits in a web component's shadow tree: only then is its frame count new to "Real video" (the page script of the old video finder counts every other film already; review of alpha56: double counting)
    /** The frames of the film for the "Real video" statistics: (page address, frames, dropped, seconds, picture width, height); the activity turns the address into a host. Never called for a private tab. */
    var onLook: (String, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _ -> }
    private fun deliverLook(force: Boolean) {
        if (quiet) return
        val l = health.takeLook(force) ?: return
        if (lookShadow) onLook(lookPage, l.frames, l.dropped, l.seconds, l.w, l.h)
    }

    init { view.listener = this }

    // ---------------------------------------------------------------------------------------- the activity

    /** A page went full screen (or left it) and the person wants Kestrel's controls ([wanted]): nothing is drawn until the page reports a film (a full-screen game or slide show has none). */
    fun fullscreen(on: Boolean, wanted: Boolean = true) {
        if (on && wanted) { armed = true; return }
        armed = false
        if (activeFlag) end("full screen ended")
    }

    private fun begin() {
        activeFlag = true; started = SystemClock.uptimeMillis(); reports = 0; taps = 0; away = 0; awayMax = 0; lastReportAt = started
        java.util.Arrays.fill(appTapEpoch, 0L); lastAppTapUptime = 0L; lastToggled = false
        health.newSession()
        view.showQuality = qualityAvailable()
        if (view.showQuality) say("quality-button", "shown at the start: the film offers pictures to pick from")
        view.visibility = View.VISIBLE
        model.setHideAfter(accessibilityMs())
        model.start(true)
        scheduleWatchdog()
        say("start", "a full-screen film: Kestrel's controls")
    }

    private fun end(why: String) {
        deliverLook(true)
        activeFlag = false; shownFlag = false
        Io.main.removeCallbacks(controlsTick); Io.main.removeCallbacks(ticker); Io.main.removeCallbacks(watchdog); tickerOn = false; watchOn = false
        model.stop(); commands.clear()
        view.show(false, false); view.visibility = View.GONE
        rotateButton(false)
        last = if (quiet) "a private tab's full screen ended (nothing of it is kept)" else "$why after ${(SystemClock.uptimeMillis() - started) / 1000} s: $reports reports, $taps taps, ${model.seeks} seeks, ${model.commands} commands, the page's own controls put away: most $awayMax element(s)"
        say("stop", last)
    }

    /** The longest Android's accessibility settings ask a control to stay (API 29: "Time to take action"): the controls never hide sooner than that. */
    private fun accessibilityMs(): Long = try {
        val am = view.context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        if (am != null && android.os.Build.VERSION.SDK_INT >= 29) am.getRecommendedTimeoutMillis(PlayerRules.HIDE_AFTER_MS.toInt(), android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS).toLong() else PlayerRules.HIDE_AFTER_MS
    } catch (e: Throwable) { PlayerRules.HIDE_AFTER_MS }

    /** A page that stops reporting (its full-screen box lost the film, the script is gone) takes the controls with it. */
    private fun scheduleWatchdog() { if (!watchOn && activeFlag) { watchOn = true; Io.main.postDelayed(watchdog, 1000L) } }
    private fun checkAlive() {
        if (!activeFlag) return
        if (PlayerRules.pageGone(SystemClock.uptimeMillis() - lastReportAt)) { end("the page stopped reporting"); return }
        scheduleWatchdog()
    }

    val active: Boolean get() = activeFlag

    /** The screen turns (the cover is up) / the cover is gone. */
    fun turnStarted() { if (activeFlag) { model.turnStarted(); flush() } }
    fun turnEnded() { if (activeFlag) { model.turnEnded(); flush() } }

    /** The bars are hidden for the film (Hide mode) and a swipe can pull the navigation bar up over it: the controls' bottom row keeps [px] free (0 = the bars stay: nothing to keep free). */
    fun setBottomInset(px: Int) { if (px != lastInset) { lastInset = px; say("inset", if (px > 0) "the system bars are hidden: the controls keep $px px free at the bottom for a pulled-up navigation bar" else "the system bars stay: the controls use the whole page area") }; view.setBottomInset(px) }
    private var lastInset = 0

    // ---------------------------------------------------------------------------------------- the page (UI thread)

    /** `vst` (the film's state) or `vtap` (a tap on the picture) from the page that is full screen. */
    fun onMessage(m: JSONObject) {
        if (!armed) return
        if (!activeFlag) { if (m.optString("k") != "vst") return; begin() }
        lastReportAt = SystemClock.uptimeMillis()
        when (m.optString("k")) {
            "vst" -> {
                reports++
                val len = m.optDouble("d", -1.0)
                model.onSample(PlayerModel.Sample(m.optDouble("t", 0.0), len, m.optDouble("b", 0.0), m.optInt("pa") == 1, m.optInt("e") == 1, m.optInt("m") == 1, m.optInt("v", 100), m.optDouble("r", 1.0),
                    m.optInt("rs", 4), m.optInt("ms") == 1, m.optInt("l") == 1 || len == -2.0, m.optInt("w"), m.optInt("h"), SystemClock.uptimeMillis(), m.optInt("ak", 0), m.optInt("er") == 1))
                if (!quiet) {
                    health.onReport(m.optInt("lt"), m.optInt("pc"), m.optInt("tf", -1), m.optInt("df", -1), m.optInt("w"), m.optInt("h"), SystemClock.uptimeMillis())
                    lookPage = m.optString("p"); lookShadow = m.optInt("sh") == 1; deliverLook(false)
                    val lt = m.optInt("lt")
                    if (lt >= FilmHealth.STALL_MS) say("stall", "the page's script was busy for about $lt ms (its timer ran that late)")
                }
                if (reports == 1) say("first-report", "film ${m.optInt("w")}x${m.optInt("h")}, length ${m.optDouble("d")}, ${if (m.optInt("ms") == 1) "media source" else "plain file"}")
                val aw = m.optInt("aw", 0)
                if (aw != away) { away = aw; if (aw > awayMax) awayMax = aw; say("away", "the page's own player controls put away: $aw element(s)") }
            }
            "vtap" -> {
                val ts = m.optLong("ts", 0L)
                if (m.optInt("pg") == 1) {                                  // a tap on one of the page's own buttons (skip ad ...): what the app toggled for it at once is put back
                    if (matchesAppTap(ts) && lastToggled) { lastToggled = false; undoneTaps++; model.undoTap(prevShown); say("tap-undone", "the page said that tap was on one of its own buttons") }
                } else if (matchesAppTap(ts)) { dupTaps++ }                  // the same tap the app already answered on the glass
                else { taps++; say("tap", "x=${m.optInt("x", 500)} (the page's message)"); model.onTap(m.optInt("x", 500)) }
            }
        }
        flush()
    }

    /** ANS53c: the activity saw a tap on the glass (UI thread): [xPermille] across the picture, [epochMs] = when the finger lifted, [msSinceUp] = how long ago that was when this ran. The controls answer AT ONCE. */
    fun appTap(xPermille: Int, epochMs: Long, msSinceUp: Long) {
        if (!activeFlag || !armed) return
        appTapEpoch[appTapIdx and 3] = epochMs; appTapIdx++
        lastAppTapUptime = SystemClock.uptimeMillis()
        appTaps++; taps++; appTapMsTotal += msSinceUp.coerceAtLeast(0L); if (msSinceUp > appTapMsWorst) appTapMsWorst = msSinceUp
        prevShown = model.shown; val before = model.singles
        model.onTap(xPermille.coerceIn(0, 1000))
        lastToggled = model.singles != before
        say("tap", "x=$xPermille (on the screen itself, ${msSinceUp.coerceAtLeast(0L)} ms after the finger lifted)")
        flush()
    }

    /** The page's message names the time of the touch (`ts`, epoch ms); an older page script names none: then the tap just before it is meant. */
    private fun matchesAppTap(ts: Long): Boolean {
        if (ts > 0L) { for (e in appTapEpoch) if (e > 0L && Math.abs(e - ts) <= 450L) return true; return false }
        return SystemClock.uptimeMillis() - lastAppTapUptime < 900L
    }

    /** Is [wx], [wy] (window coordinates) on one of the shown controls? Such a touch is the controls' own: the activity does not count it as a tap on the picture. */
    fun controlAt(wx: Float, wy: Float): Boolean = sheetOpen() || view.controlAtWindow(wx, wy)

    /** ANS53d.2: the media list changed: the Quality button follows it (a film's playlist read a moment after the full screen began). */
    fun refreshQuality() {
        if (!activeFlag) return
        val v = qualityAvailable()
        if (v != view.showQuality) { view.showQuality = v; view.invalidate(); say("quality-button", if (v) "shown: the film offers pictures to pick from" else "gone") }
    }

    /** The lab presses the Quality button (debug intent): the same call the button makes. */
    fun pressQuality() { if (activeFlag && view.showQuality) onQuality() }

    /** ANS53d.2: keep the controls up while the quality list is open. */
    fun pinControls(on: Boolean) { model.pin(on) }

    /** ANS53d.2: the film is handed to Kestrel's own player: the page's copy stops (its pause command goes with the next answer; the activity also pauses the media session). */
    fun pausePage() { if (activeFlag && model.view().playing) { model.playPause(); flush() } }

    /** The answer for the page, from any thread: what the controls want done, whether they are shown. */
    fun reply(): String {
        if (!armed && !activeFlag) return "{\"s\":0,\"a\":0}"
        // a = 1 while the controls own this film: a page that hears a = 0 four times in a row (the app does not take its reports: another tab, an app that is not ready, a bug) puts its own bar and its taps back
        // instead of leaving a full-screen page with no controls at all (review of alpha55, F1)
        val sb = StringBuilder(64).append("{\"s\":").append(if (shownFlag) 1 else 0).append(",\"a\":").append(if (activeFlag) 1 else 0)
        var c = commands.poll()
        if (c != null) {
            sb.append(",\"c\":[")
            var n = 0
            while (c != null) {                                    // (at most 8 in one answer: the rest waits in the queue for the next one)
                if (n > 0) sb.append(','); sb.append(c); n++
                c = if (n < 8) commands.poll() else null
            }
            sb.append(']')
        }
        return sb.append('}').toString()
    }

    // ---------------------------------------------------------------------------------------- PlayerModel.Host

    override fun now() = SystemClock.uptimeMillis()
    override fun scheduleControls(ms: Long) { Io.main.removeCallbacks(controlsTick); Io.main.postDelayed(controlsTick, ms) }
    override fun cancelControls() { Io.main.removeCallbacks(controlsTick) }
    override fun applyControls(shown: Boolean, animate: Boolean) {
        if (shown != shownFlag) say(if (shown) "shown" else "hidden", if (animate) "fade" else "at once")
        shownFlag = shown
        view.show(shown, animate)
        rotateButton(shown)
        if (shown) ensureTicker()
    }
    override fun changed() { refresh() }
    override fun feedback(zone: PlayerRules.Zone, burst: Int) { view.flash(zone, burst) }
    override fun log(event: String, text: String) { say(event, text) }

    // ---------------------------------------------------------------------------------------- the view's listener (UI thread)

    override fun onPlayPause() { model.playPause(); flush() }
    override fun onSkip(forward: Boolean) { model.skipButton(forward); flush() }
    override fun onMute() { model.mute(); flush() }
    override fun onExit() { exitFullscreen() }
    override fun onSeek(fraction: Double) { model.seekFraction(fraction); flush() }
    override fun onPin(on: Boolean) { model.pin(on) }
    override fun onQuality() { val v = model.view(); say("quality-pressed", "at ${v.timeS.toInt()} s"); qualityPressed(v.timeS, v.lengthS) }

    // ---------------------------------------------------------------------------------------- inside

    /** Move what the model queued for the page into the thread-safe queue the worker thread reads. */
    private fun flush() { for (c in model.drain()) { val j = c.json(); commands.add(j); say("cmd", j) } }

    private fun refresh() {
        if (!activeFlag) return
        view.update(model.view())
        if (shownFlag) ensureTicker()
    }

    /** While the controls show and the film plays the position runs on between the page's reports: a light timer, only then. */
    private fun ensureTicker() {
        if (tickerOn || !activeFlag || !shownFlag || !model.view().playing) return
        tickerOn = true
        Io.main.postDelayed(ticker, 250L)
    }


    /** One Diagnostics line. */
    fun line(): String = "Full-screen video controls (Kestrel's own): ${model.sessions} full screens, taps on the picture: ${model.singles} single and ${model.seeks} double-tap seeks, ${model.commands} commands sent to the page; " +
        "the last full screen: $reports reports, $taps taps; taps answered on the screen itself $appTaps (average ${if (appTaps > 0) appTapMsTotal / appTaps else 0} ms from the finger lifting, worst $appTapMsWorst ms), the page's own copies dropped $dupTaps, put back after a page button $undoneTaps; ${health.line()}; last: $last"
}
