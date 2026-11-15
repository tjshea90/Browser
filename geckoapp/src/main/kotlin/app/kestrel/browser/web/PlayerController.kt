package app.kestrel.browser.web

import android.os.SystemClock
import android.view.View
import app.kestrel.browser.Io
import app.kestrel.browser.diag.Health
import app.kestrel.core.player.PlayerModel
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
) : PlayerModel.Host, PlayerControlsView.Listener {
    private val model = PlayerModel(this)
    private val controlsTick = Runnable { model.controlsTimer(); flush() }
    private val tapTick = Runnable { model.tapTimer(); flush() }
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

    // what Diagnostics says
    private var reports = 0
    private var taps = 0
    private var last = "no full-screen film under Kestrel's controls yet"

    init { view.listener = this }

    // ---------------------------------------------------------------------------------------- the activity

    /** A page went full screen (or left it) and the person wants Kestrel's controls ([wanted]): nothing is drawn until the page reports a film (a full-screen game or slide show has none). */
    fun fullscreen(on: Boolean, wanted: Boolean = true) {
        if (on && wanted) { armed = true; return }
        armed = false
        if (activeFlag) end("full screen ended")
    }

    private fun begin() {
        activeFlag = true; started = SystemClock.uptimeMillis(); reports = 0; taps = 0; lastReportAt = started
        view.visibility = View.VISIBLE
        model.setHideAfter(accessibilityMs())
        model.start(true)
        scheduleWatchdog()
        Health.log("player", "start", "a full-screen film: Kestrel's controls")
    }

    private fun end(why: String) {
        activeFlag = false; shownFlag = false
        Io.main.removeCallbacks(controlsTick); Io.main.removeCallbacks(tapTick); Io.main.removeCallbacks(ticker); Io.main.removeCallbacks(watchdog); tickerOn = false; watchOn = false
        model.stop(); commands.clear()
        view.show(false, false); view.visibility = View.GONE
        rotateButton(false)
        last = "$why after ${(SystemClock.uptimeMillis() - started) / 1000} s: $reports reports, $taps taps, ${model.seeks} seeks, ${model.commands} commands"
        Health.log("player", "stop", last)
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
        if (SystemClock.uptimeMillis() - lastReportAt > SILENT_END_MS) { end("the page stopped reporting"); return }
        scheduleWatchdog()
    }

    val active: Boolean get() = activeFlag

    /** The screen turns (the cover is up) / the cover is gone. */
    fun turnStarted() { if (activeFlag) { model.turnStarted(); flush() } }
    fun turnEnded() { if (activeFlag) { model.turnEnded(); flush() } }

    /** The navigation bar was pulled up over the film (Hide mode): the controls move up. */
    fun setBottomInset(px: Int) { Health.log("player", "inset", "the navigation bar pulled up over the film: controls ${if (px > 0) "move up by $px px" else "back down"}"); view.setBottomInset(px) }

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
                model.onSample(PlayerModel.Sample(m.optDouble("t", 0.0), len, m.optDouble("b", 0.0), m.optInt("p") == 1, m.optInt("e") == 1, m.optInt("m") == 1, m.optInt("v", 100), m.optDouble("r", 1.0),
                    m.optInt("rs", 4), m.optInt("ms") == 1, m.optInt("l") == 1 || len == -2.0, m.optInt("w"), m.optInt("h"), SystemClock.uptimeMillis()))
                if (reports == 1) Health.log("player", "first-report", "film ${m.optInt("w")}x${m.optInt("h")}, length ${m.optDouble("d")}, ${if (m.optInt("ms") == 1) "media source" else "plain file"}")
            }
            "vtap" -> { taps++; Health.log("player", "tap", "x=${m.optInt("x", 500)}"); model.onTap(m.optInt("x", 500)) }
        }
        flush()
    }

    /** The answer for the page, from any thread: what the controls want done, whether they are shown. */
    fun reply(): String {
        if (!armed && !activeFlag) return "{\"s\":0}"
        val sb = StringBuilder(64).append("{\"s\":").append(if (shownFlag) 1 else 0)
        var c = commands.poll()
        if (c != null) {
            sb.append(",\"c\":[")
            var n = 0
            while (c != null && n < 8) { if (n > 0) sb.append(','); sb.append(c); n++; c = commands.poll() }
            sb.append(']')
        }
        return sb.append('}').toString()
    }

    // ---------------------------------------------------------------------------------------- PlayerModel.Host

    override fun now() = SystemClock.uptimeMillis()
    override fun scheduleControls(ms: Long) { Io.main.removeCallbacks(controlsTick); Io.main.postDelayed(controlsTick, ms) }
    override fun cancelControls() { Io.main.removeCallbacks(controlsTick) }
    override fun scheduleTap(ms: Long) { Io.main.removeCallbacks(tapTick); Io.main.postDelayed(tapTick, ms) }
    override fun cancelTap() { Io.main.removeCallbacks(tapTick) }
    override fun applyControls(shown: Boolean, animate: Boolean) {
        if (shown != shownFlag) Health.log("player", if (shown) "shown" else "hidden", if (animate) "fade" else "at once")
        shownFlag = shown
        view.show(shown, animate)
        rotateButton(shown)
        if (shown) ensureTicker()
    }
    override fun changed() { refresh() }
    override fun feedback(zone: PlayerRules.Zone, burst: Int) { view.flash(zone, burst) }
    override fun log(event: String, text: String) { Health.log("player", event, text) }

    // ---------------------------------------------------------------------------------------- the view's listener (UI thread)

    override fun onPlayPause() { model.playPause(); flush() }
    override fun onSkip(forward: Boolean) { model.skipButton(forward); flush() }
    override fun onMute() { model.mute(); flush() }
    override fun onExit() { exitFullscreen() }
    override fun onSeek(fraction: Double) { model.seekFraction(fraction); flush() }
    override fun onPin(on: Boolean) { model.pin(on) }

    // ---------------------------------------------------------------------------------------- inside

    /** Move what the model queued for the page into the thread-safe queue the worker thread reads. */
    private fun flush() { for (c in model.drain()) { val j = c.json(); commands.add(j); Health.log("player", "cmd", j) } }

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

    private companion object { const val SILENT_END_MS = 3500L }

    /** One Diagnostics line. */
    fun line(): String = "Full-screen video controls (Kestrel's own): ${model.sessions} full screens, $taps taps on the picture (${model.singles} single, ${model.seeks} double-tap seeks), ${model.commands} commands to the page, $reports reports now; last: $last"
}
