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
        fun scheduleTap(ms: Long)
        fun cancelTap()
        /** The controls show or hide ([animate] = fade). */
        fun applyControls(shown: Boolean, animate: Boolean)
        /** The numbers the controls draw changed: draw again. */
        fun changed()
        /** A double tap seeked: [zone] LEFT / RIGHT, [burst] 1, 2, 3 ... (the "10 s" feedback). */
        fun feedback(zone: Zone, burst: Int) {}
        fun log(event: String, text: String) {}
    }

    /** What the page told (see player.js: all numbers). [lengthS] -1 = no length, -2 = infinite (a live stream). */
    class Sample(val timeS: Double, val lengthS: Double, val bufferedS: Double, val paused: Boolean, val ended: Boolean, val muted: Boolean, val volume: Int, val rate: Double, val ready: Int,
                 val mse: Boolean, val live: Boolean, val width: Int, val height: Int, val atMs: Long)

    /** What the controls draw. */
    class View(val playing: Boolean, val ended: Boolean, val buffering: Boolean, val muted: Boolean, val timeS: Double, val kind: DurationView.Kind, val lengthS: Double,
               val bufferedS: Double, val rate: Double, val shown: Boolean)

    /** A command for the page (`player.js run()`): play, pause, seek (t), skip (d), mute (on), rate (r). */
    class Cmd(val a: String, val t: Double = 0.0, val d: Double = 0.0, val on: Boolean = false, val r: Double = 1.0) {
        /** Compact JSON for the answer to the page. */
        fun json(): String = when (a) {
            "seek" -> "{\"a\":\"seek\",\"t\":${num(t)}}"
            "skip" -> "{\"a\":\"skip\",\"d\":${num(d)}}"
            "mute" -> "{\"a\":\"mute\",\"on\":$on}"
            "rate" -> "{\"a\":\"rate\",\"r\":${num(r)}}"
            else -> "{\"a\":\"$a\"}"
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
        override fun schedule(delayMs: Long) = h.scheduleTap(delayMs)
        override fun cancel() = h.cancelTap()
        override fun single() { controls.tap(); singles++ }
        override fun seek(zone: Zone, burst: Int) { skip(if (zone == Zone.LEFT) -PlayerRules.SKIP_S else PlayerRules.SKIP_S); h.feedback(zone, burst); seeks++ }
        override fun playPause() { playPause() }
    })

    private val queue = ArrayList<Cmd>()
    private var last: Sample? = null
    private var shownLength: DurationView.Shown = DurationView.Shown(DurationView.Kind.UNKNOWN, 0.0)
    private var optPlaying: Boolean? = null
    private var optUntil = 0L
    private var optMuted: Boolean? = null
    private var active = false

    // what Diagnostics says
    var sessions = 0; private set
    var singles = 0; private set
    var seeks = 0; private set
    var commands = 0; private set
    var samples = 0; private set

    val shown: Boolean get() = controls.shown

    fun start(playingNow: Boolean) { active = true; sessions++; last = null; duration.reset(); queue.clear(); optPlaying = null; optMuted = null; taps.reset(); controls.start(playingNow) }
    fun stop() { active = false; taps.reset(); controls.stop(); queue.clear(); last = null; h.changed() }
    val running: Boolean get() = active

    /** A report of the page. */
    fun onSample(s: Sample) {
        if (!active) return
        last = s; samples++
        shownLength = duration.feed(s.lengthS, s.timeS, s.bufferedS, s.mse, s.live || s.lengthS == -2.0, s.atMs)
        val o = optPlaying
        if (o != null && (s.atMs >= optUntil || o == !(s.paused || s.ended))) optPlaying = null   // the film did what was asked (or the wait is over): its own state counts again
        if (optMuted != null && optMuted == s.muted) optMuted = null
        val v = view()
        controls.state(v.playing, v.buffering, v.ended)
        h.changed()
    }

    /** What the controls draw now. */
    fun view(): View {
        val s = last
        val now = h.now()
        val playing = optPlaying ?: (s != null && !(s.paused || s.ended))
        val buffering = s != null && playing && !s.ended && s.ready in 0..2
        val rate = s?.rate ?: 1.0
        val t = when {
            s == null -> 0.0
            playing && !buffering && s.rate > 0.0 -> s.timeS + (now - s.atMs).coerceAtLeast(0L) / 1000.0 * s.rate
            else -> s.timeS
        }
        val known = shownLength.kind == DurationView.Kind.KNOWN
        val shownT = if (known) t.coerceAtMost(shownLength.seconds) else t
        return View(playing, s?.ended == true, buffering, optMuted ?: (s?.muted == true), shownT, shownLength.kind, shownLength.seconds, s?.bufferedS ?: 0.0, rate, controls.shown)
    }

    // ---- the person
    /** A tap on the picture reached the app: [xPermille] = the place across the picture, 0..1000. */
    fun onTap(xPermille: Int) { if (active) taps.tap(xPermille.coerceIn(0, 1000) / 1000.0) }
    fun tapTimer() { taps.tick() }
    fun controlsTimer() { controls.tick() }

    fun playPause() {
        val v = view()
        val want = !v.playing
        optPlaying = want; optUntil = h.now() + 1200L
        send(Cmd(if (want) "play" else "pause"))
        controls.interacted(); h.changed()
    }
    fun skip(deltaS: Double) { send(Cmd("skip", d = deltaS)); controls.interacted() }
    fun skipButton(forward: Boolean) { skip(if (forward) PlayerRules.SKIP_S else -PlayerRules.SKIP_S); h.changed() }
    fun seekFraction(f: Double) {
        val v = view()
        if (v.kind != DurationView.Kind.KNOWN) return
        send(Cmd("seek", t = PlayerRules.timeAt(f, v.lengthS)))
        controls.interacted(); h.changed()
    }
    fun mute() { val v = view(); optMuted = !v.muted; send(Cmd("mute", on = !v.muted)); controls.interacted(); h.changed() }
    fun setRate(r: Double) { send(Cmd("rate", r = r)); controls.interacted() }
    /** A finger is on a control (or the track): the controls stay up until it lets go. */
    fun pin(on: Boolean) = controls.pin(on)

    // ---- the screen
    fun turnStarted() = controls.turnStarted()
    fun turnEnded() = controls.turnEnded()

    // ---- the page asks
    private fun send(c: Cmd) { queue.add(c); commands++; if (queue.size > 16) queue.removeAt(0) }
    /** The commands for the page, once. */
    fun drain(): List<Cmd> { if (queue.isEmpty()) return emptyList(); val out = ArrayList(queue); queue.clear(); return out }
}
