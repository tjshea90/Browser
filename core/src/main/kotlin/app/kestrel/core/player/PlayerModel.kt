package app.kestrel.core.player

import app.kestrel.core.player.PlayerRules.Zone

/**
 * VP1 (owner message 49): everything the full-screen controls know and decide, pure and testable with a fake host and a fake clock: the film's state as the page last told it, what the controls show of it
 * (position extrapolated between reports, a length only when it is believed, buffering), what a tap and every control do, and the commands the page is to carry out. It owns the four small machines
 * of this package ([ControlsMachine] when the controls show, [TapMachine] single / double taps, [DurationView] the length, the command queue). The Android side is `web/PlayerController` (clock, views,
 * the messages with the page); nothing of Android is needed here.
 */
class PlayerModel(private val h: Host) {
    interface Host {
        fun now(): Long
        fun scheduleControls(ms: Long)
        fun cancelControls()
        /** The controls show or hide ([animate] = fade). */
        fun applyControls(shown: Boolean, animate: Boolean)
        /** The numbers the controls draw changed: draw again. */
        fun changed()
        /** A double tap seeked: [zone] LEFT / RIGHT, [burst] 1, 2, 3 ... (the "10 s" feedback). */
        fun feedback(zone: Zone, burst: Int) {}
        fun log(event: String, text: String) {}
    }

    /**
     * What the page told (see player.js: all numbers). [lengthS] -1 = no length, -2 = infinite (a live stream). [ack] = the number of the last command the page had carried out when it took this sample (0 = none yet):
     * a report that comes later than a command but says a lower number was taken BEFORE the film did what was asked (lab run 43776782679: a second press on the big button, 1.5 s after the first, asked for
     * "pause" again, because a stale report had ended the wait), so it must not undo what the person just saw happen.
     */
    class Sample(val timeS: Double, val lengthS: Double, val bufferedS: Double, val paused: Boolean, val ended: Boolean, val muted: Boolean, val volume: Int, val rate: Double, val ready: Int,
                 val mse: Boolean, val live: Boolean, val width: Int, val height: Int, val atMs: Long, val ack: Int = 0, val error: Boolean = false)

    /** What the controls draw. */
    class View(val playing: Boolean, val ended: Boolean, val failed: Boolean, val buffering: Boolean, val muted: Boolean, val timeS: Double, val kind: DurationView.Kind, val lengthS: Double,
               val bufferedS: Double, val rate: Double, val shown: Boolean)

    /** A command for the page (`player.js run()`): play, pause, seek (t), skip (d), mute (on), rate (r). */
    class Cmd(val a: String, val t: Double = 0.0, val d: Double = 0.0, val on: Boolean = false, val r: Double = 1.0) {
        /** The command's number (1, 2, 3 ... for as long as the app runs): the page says in its reports which was the last one it carried out ([Sample.ack]). */
        var id = 0
        /** Compact JSON for the answer to the page. */
        fun json(): String {
            val body = when (a) {
                "seek" -> "{\"a\":\"seek\",\"t\":${num(t)}}"
                "skip" -> "{\"a\":\"skip\",\"d\":${num(d)}}"
                "mute" -> "{\"a\":\"mute\",\"on\":$on}"
                "rate" -> "{\"a\":\"rate\",\"r\":${num(r)}}"
                else -> "{\"a\":\"$a\"}"
            }
            return if (id > 0) body.substring(0, body.length - 1) + ",\"i\":$id}" else body
        }
        private fun num(x: Double) = if (x == Math.floor(x) && Math.abs(x) < 1e9) x.toLong().toString() else "%.2f".format(java.util.Locale.ROOT, x)
    }

    private val duration = DurationView()
    private val controls = ControlsMachine(object : ControlsMachine.Host {
        override fun now() = h.now()
        override fun schedule(delayMs: Long) = h.scheduleControls(delayMs)
        override fun cancel() = h.cancelControls()
        override fun apply(shown: Boolean, animate: Boolean) { h.applyControls(shown, animate); h.changed() }
    })
    private val taps = TapMachine(object : TapMachine.Host {
        override fun now() = h.now()
        override fun single() { controls.tap(); singles++ }
        override fun seek(zone: Zone, burst: Int) { skip(if (zone == Zone.LEFT) -PlayerRules.SKIP_S else PlayerRules.SKIP_S); controls.seekGesture(); h.feedback(zone, burst); seeks++ }   // ANS57a: the controls go, the film stays clear
    })

    private val queue = ArrayList<Cmd>()
    private var last: Sample? = null
    private var shownLength: DurationView.Shown = DurationView.Shown(DurationView.Kind.UNKNOWN, 0.0)
    private var seq = 0                       // the number of the last command made (never reset: the page's own count of what it did only grows too)
    private var optPlaying: Boolean? = null   // what a play / pause press asked for, shown at once and held until the page says it did it (its report names the command) or OPT_WAIT_MS pass
    private var optSeq = 0
    private var optUntil = 0L
    private var optMuted: Boolean? = null
    private var optMutedSeq = 0
    private var optMutedUntil = 0L
    private var optPos: Double? = null        // where a seek (the track, a 10 s button, a double tap) asked to go: shown at once and held until the page says it did it (review of alpha55: the thumb sprang back for a moment)
    private var optPosSeq = 0
    private var optPosUntil = 0L
    private var active = false

    // what Diagnostics says
    var sessions = 0; private set
    var singles = 0; private set
    var seeks = 0; private set
    var commands = 0; private set
    var samples = 0; private set

    val shown: Boolean get() = controls.shown

    /** The accessibility setting's recommended time for controls to stay (never less than [PlayerRules.HIDE_AFTER_MS]). */
    fun setHideAfter(ms: Long) { controls.hideAfterMs = maxOf(PlayerRules.HIDE_AFTER_MS, ms) }

    fun start(playingNow: Boolean) { active = true; sessions++; last = null; duration.reset(); queue.clear(); optPlaying = null; optMuted = null; optPos = null; taps.reset(); controls.start(playingNow) }
    fun stop() { active = false; taps.reset(); controls.stop(); queue.clear(); last = null; h.changed() }
    val running: Boolean get() = active

    /** A report of the page. */
    fun onSample(s: Sample) {
        if (!active) return
        last = s; samples++
        shownLength = duration.feed(s.lengthS, s.timeS, s.bufferedS, s.mse, s.live || s.lengthS == -2.0, s.atMs)
        // the page has carried out the command (this report was taken after it: the film's own state counts again) or the wait is over (a page that never answers must not hold a wrong picture for ever)
        if (optPlaying != null && (s.ack >= optSeq || s.atMs >= optUntil)) optPlaying = null
        if (optMuted != null && (s.ack >= optMutedSeq || s.atMs >= optMutedUntil || optMuted == s.muted)) optMuted = null
        if (optPos != null && (s.ack >= optPosSeq || s.atMs >= optPosUntil)) optPos = null
        val v = view()
        controls.state(v.playing, v.buffering, v.ended || v.failed)                       // a film that FAILED is like one that ended: the controls come and stay (the research: Media3, Shaka, Chromium)
        h.changed()
    }

    /** What the controls draw now. */
    fun view(): View {
        val s = last
        val now = h.now()
        val failed = s?.error == true
        val playing = optPlaying ?: (s != null && !(s.paused || s.ended || failed))
        val buffering = s != null && playing && !s.ended && !failed && s.ready in 0..2
        val rate = s?.rate ?: 1.0
        val t = when {
            s == null -> 0.0
            optPos != null -> optPos!!                                                       // a seek the page has not carried out yet: where it was asked to go
            playing && !buffering && s.rate > 0.0 -> s.timeS + (now - s.atMs).coerceAtLeast(0L) / 1000.0 * s.rate
            else -> s.timeS
        }
        val known = shownLength.kind == DurationView.Kind.KNOWN
        val shownT = if (known) t.coerceAtMost(shownLength.seconds) else t
        return View(playing, s?.ended == true, failed, buffering, optMuted ?: (s?.muted == true), shownT, shownLength.kind, shownLength.seconds, s?.bufferedS ?: 0.0, rate, controls.shown)
    }

    // ---- the person
    /** A tap on the picture reached the app: [xPermille] = the place across the picture, 0..1000. */
    fun onTap(xPermille: Int) { if (active) taps.tap(xPermille.coerceIn(0, 1000) / 1000.0) }
    fun controlsTimer() { controls.tick() }

    /** ANS53c: the tap that just toggled the controls was on one of the page's own buttons: put them back as they were ([was] = shown before the tap). */
    fun undoTap(was: Boolean) { if (active) controls.restore(was) }

    fun playPause() {
        val v = view()
        val want = !v.playing
        send(Cmd(if (want) "play" else "pause"))
        optPlaying = want; optSeq = seq; optUntil = h.now() + OPT_WAIT_MS
        controls.interacted(); h.changed()
    }
    fun skip(deltaS: Double) {
        val v = view()
        val target = (v.timeS + deltaS).coerceAtLeast(0.0).let { if (v.kind == DurationView.Kind.KNOWN) it.coerceAtMost(v.lengthS) else it }
        send(Cmd("skip", d = deltaS))
        if (last != null) { optPos = target; optPosSeq = seq; optPosUntil = h.now() + OPT_WAIT_MS }
        controls.interacted()
    }
    fun skipButton(forward: Boolean) { skip(if (forward) PlayerRules.SKIP_S else -PlayerRules.SKIP_S); h.changed() }
    fun seekFraction(f: Double) {
        val v = view()
        if (v.kind != DurationView.Kind.KNOWN) return
        val to = PlayerRules.timeAt(f, v.lengthS)
        send(Cmd("seek", t = to))
        optPos = to; optPosSeq = seq; optPosUntil = h.now() + OPT_WAIT_MS
        controls.interacted(); h.changed()
    }
    fun mute() { val v = view(); send(Cmd("mute", on = !v.muted)); optMuted = !v.muted; optMutedSeq = seq; optMutedUntil = h.now() + OPT_WAIT_MS; controls.interacted(); h.changed() }
    fun setRate(r: Double) { send(Cmd("rate", r = r)); controls.interacted() }
    /** A finger is on a control (or the track): the controls stay up until it lets go. */
    fun pin(on: Boolean) = controls.pin(on)

    // ---- the screen
    fun turnStarted() = controls.turnStarted()
    fun turnEnded() = controls.turnEnded()

    // ---- the page asks
    private fun send(c: Cmd) { c.id = ++seq; queue.add(c); commands++; if (queue.size > 16) queue.removeAt(0) }
    /** The commands for the page, once. */
    fun drain(): List<Cmd> { if (queue.isEmpty()) return emptyList(); val out = ArrayList(queue); queue.clear(); return out }

    private companion object {
        /** How long a play / pause / mute press is shown as done while the page has not said it did it (the report that says so is a few hundred ms away on a tablet that is busy). */
        const val OPT_WAIT_MS = 3000L
    }
}
