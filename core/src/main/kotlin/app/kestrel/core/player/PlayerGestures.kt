package app.kestrel.core.player

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * ANS57 (owner message 57, 2026-11-16: "Build a feature similar to vlc media player which I can do a more accurate fast forward or rewind in larger and smaller increments by long pressing around the middle of the
 * screen and dragging right to fast forward and left to rewind. The farther I drag, the more it fast forwards or rewinds ... if I drag up or down on the left side of the video, it increases or decreases video
 * brightness, and if I drag up or down on the right side of the video, it increases or decreases volume. Model this off how vlc media player handles this"): the numbers and rules of VLC for Android's touch
 * handling (read from videolan/vlc-android VideoTouchDelegate.kt, research/ANS57_VLC_GESTURES.md; ideas and numbers only), pure and testable, no Android.
 */
object GestureMath {
    enum class Level { BRIGHTNESS, VOLUME }

    /** A horizontal move under this many centimetres on the glass is no seek (VLC: ignored under 1 cm). */
    const val SEEK_MIN_CM = 1.0
    /** The jump of a drag of [SEEK_REF_CM] is [SEEK_SCALE_MS] (ten minutes at 8 cm); every jump starts at [SEEK_BASE_MS] (three seconds). The curve between is the fourth power: small drags move in small steps, long drags run. */
    const val SEEK_BASE_MS = 3000.0
    const val SEEK_SCALE_MS = 600000.0
    const val SEEK_REF_CM = 8.0
    /** A move whose vertical size is more than this times its horizontal size is a vertical drag (VLC: coef > 2). */
    const val STEEP = 2.0
    /** A vertical drag only begins after the finger has gone this share of the range up or down (VLC: 5 %). */
    const val VERTICAL_START = 0.05
    /** A drag over the whole range changes a level by this much (VLC: 1.25 of the full range, for volume and for brightness). */
    const val LEVEL_FACTOR = 1.25
    /** The left 3/7 of the width is brightness, the right 3/7 volume, the 1/7 in the middle is a dead zone (VLC). */
    const val LEFT_END = 3.0 / 7.0
    const val RIGHT_START = 4.0 / 7.0
    const val CM_PER_INCH = 2.54

    fun cm(px: Double, dpi: Double): Double = if (dpi > 0.0) px / dpi * CM_PER_INCH else 0.0

    /** VLC's fine control: the further the finger is also moved up or down from where it began, the smaller the jump: the jump is divided by 2 x (inches + 0.5), at least 1 (one inch up or down = divided by 3). */
    fun fineCoef(dyPx: Double, dpi: Double): Double = if (dpi > 0.0) ((abs(dyPx) / dpi + 0.5) * 2.0).coerceAtLeast(1.0) else 1.0

    /** The jump in milliseconds for a horizontal drag of [xCm] (right = forward) divided by [coef]; 0 for a drag under [SEEK_MIN_CM]. */
    fun jumpMs(xCm: Double, coef: Double): Long {
        if (abs(xCm) < SEEK_MIN_CM) return 0L
        val c = if (coef < 1.0) 1.0 else coef
        return (sign(xCm) * (SEEK_SCALE_MS * (xCm / SEEK_REF_CM).pow(4) + SEEK_BASE_MS) / c).toLong()
    }

    /** The jump so that the film stays between its start and its end ([lengthMs] <= 0 = no length: the jump is not clamped by the end). */
    fun clampJump(jumpMs: Long, timeMs: Long, lengthMs: Long): Long {
        var j = jumpMs
        if (j > 0 && lengthMs > 0 && timeMs + j > lengthMs) j = (lengthMs - timeMs).coerceAtLeast(0L)
        if (j < 0 && timeMs + j < 0) j = -timeMs.coerceAtLeast(0L)
        return j
    }

    /** VLC's info text while dragging: the jump with its sign, where it lands, and the fine factor when it is not 1: `+0:25 (1:23:45)`, `-1:10 (0:55) x0.3`. */
    fun infoText(jumpMs: Long, targetMs: Long, coef: Double): String {
        val sign = if (jumpMs >= 0) "+" else "-"
        val fine = if (coef > 1.0) " x" + String.format(java.util.Locale.ROOT, "%.1g", 1.0 / coef) else ""
        return sign + PlayerRules.formatTime(abs(jumpMs) / 1000.0) + " (" + PlayerRules.formatTime(targetMs.coerceAtLeast(0L) / 1000.0) + ")" + fine
    }

    /** Which level a vertical drag that began at [xFraction] of the width changes (null = the dead zone in the middle). */
    fun levelAt(xFraction: Double): Level? = when {
        xFraction < LEFT_END -> Level.BRIGHTNESS
        xFraction > RIGHT_START -> Level.VOLUME
        else -> null
    }

    /** The change of a level (a share of its whole range, up = more) for a vertical move of [dyPx] over a range of [rangePx] (VLC: -dy / range x 1.25). */
    fun levelDelta(dyPx: Double, rangePx: Double): Double = if (rangePx > 0.0) -dyPx / rangePx * LEVEL_FACTOR else 0.0

    /** What a finger resting at [xFraction] of the width can do there: the left side changes the brightness, the right side the volume (both by sliding up or down), the middle seeks (by sliding sideways). */
    fun hintAt(xFraction: Double): GestureHint = when (levelAt(xFraction)) {
        Level.BRIGHTNESS -> GestureHint.BRIGHTNESS
        Level.VOLUME -> GestureHint.VOLUME
        null -> GestureHint.SEEK
    }

    /** ANS60b (owner message 60: "if I'm gesturing on the left and right sides of the screen, it should say slide up or down for brightness or volume respectively"): the words of each hint. */
    fun hintText(h: GestureHint): String = when (h) {
        GestureHint.BRIGHTNESS -> "Slide up or down for brightness"
        GestureHint.VOLUME -> "Slide up or down for volume"
        GestureHint.SEEK -> "Slide left or right to seek"
    }
}

/** The three things a finger can do on the film (which one a resting finger is told about depends on where it rests). */
enum class GestureHint { BRIGHTNESS, VOLUME, SEEK }


/**
 * The touch rules of the film, from one finger: a tap, the precise seek, and the two vertical level drags. The host (an Android view) feeds [down], [move], [up], [cancel] and calls [hold] when the timer it was asked
 * for ([Host.scheduleHold]) rings; everything it does comes back through [Host].
 *
 * ANS60a (owner message 60: "make the seek, brightness, and volume gestures in the video player work without a long press"): NO LONG PRESS is needed any more (VLC's own rules). From the moment the finger has gone past
 * the touch slop, every move is judged from where the touch began, and whichever rule is met first decides:
 *  - mostly vertical (the move is more than [GestureMath.STEEP] times higher than wide) and a vertical distance of [GestureMath.VERTICAL_START] of the range: the level of the side the touch began on (left third and a
 *    bit = brightness, right = volume); a touch that began in the middle strip makes no level gesture (nothing in a full-screen player; in the player inside the page [Config.middlePasses] gives it to the PAGE, which scrolls);
 *  - otherwise a sideways distance of [GestureMath.SEEK_MIN_CM] on the glass: the precise seek (the film moves when the finger lets go; [Host.seekPreview] says where it would land);
 *  - a finger that rests [HOLD_MS] without moving is told what it can do at that place ([Host.hint]: slide up or down for brightness / volume at the sides, left or right to seek in the middle); the hint goes the moment
 *    the finger moves or lifts; a finger that rests on until [LONG_HOLD_MS] gives [Host.longHold] (the player inside the page opens the download options with it); a short touch that goes nowhere is a [Host.tap];
 *  - [Config.holdFirst] = true is the older rule, kept as a Settings switch for the player inside the page ("Gestures inside the page need a long press"): a plain drag is the page's ([Host.passThrough]) and every
 *    gesture needs the rest first, which ARMS the gestures (the hint then comes with a tick: [Host.hint] with armed = true);
 *  - a touch that starts within the edge margin makes no gesture (system gestures live there) but is still a tap.
 */
class PlayerGestures(private val h: Host, var cfg: Config) {
    class Config(
        /** The size of the view the finger moves over, pixels. */
        val widthPx: Float, val heightPx: Float,
        /** Pixels per inch along x (Android's xdpi). */
        val xdpi: Float,
        /** The touch slop, pixels. */
        val slopPx: Float,
        /** Touches that begin this close to an edge make no gesture, pixels. */
        val edgePx: Float,
        /** The vertical range of a full level drag, pixels (VLC: the shorter side of the screen). */
        val rangePx: Float,
        /** true = a plain drag is the page's and every gesture needs the long press first (the optional rule inside the page). false = no long press needed. */
        val holdFirst: Boolean = false,
        /** No long press needed: a vertical drag that begins in the middle strip is the page's (the player inside the page) instead of nothing (the full-screen player). */
        val middlePasses: Boolean = false,
    )

    interface Host {
        fun now(): Long
        /** Call [hold] after this long (replaces an earlier request); [cancelHold] withdraws it. */
        fun scheduleHold(ms: Long)
        fun cancelHold()
        /** A short touch that went nowhere: [xFraction] across the view, 0..1. */
        fun tap(xFraction: Double)
        /** The finger rests: tell it what it can do here ([armed] = the gestures were ARMED by the rest, the optional long-press-first rule: a tick of the phone as well); null = the hint goes (the finger moved). */
        fun hint(hint: GestureHint?, armed: Boolean)
        /** The finger rested on without moving up to [LONG_HOLD_MS]. */
        fun longHold() {}
        /** Where the film is and how long it is, milliseconds ([lengthMs] <= 0 = no length: nothing to seek in). */
        fun positionMs(): Long
        fun lengthMs(): Long
        /** The precise seek would jump [jumpMs] (clamped) to [targetMs]; [coef] > 1 = the fine factor. Nothing has moved yet. */
        fun seekPreview(jumpMs: Long, targetMs: Long, coef: Double)
        /** The finger let go: move the film by [jumpMs] now. */
        fun seekCommit(jumpMs: Long)
        /** The drag came back to nothing, or was cancelled: forget the preview. */
        fun seekCancel()
        /** The precise seek was asked for in a film that has no length (a live stream). */
        fun seekUnavailable() {}
        /** A vertical drag changes [level] by [delta] of its whole range (up = more). */
        fun level(level: GestureMath.Level, delta: Double)
        /** The drag is the page's: give the touch to whoever is under the view. */
        fun passThrough()
        /** A gesture of ours has ended (or never became one): clean up what was shown. */
        fun finished()
    }

    private enum class S { IDLE, PENDING, UNDECIDED, ARMED, SCRUB, LEVEL, PASSED, IGNORED }

    private var s = S.IDLE
    private var startX = 0f
    private var startY = 0f
    private var downAt = 0L
    private var inBounds = true
    private var lastY = 0f
    private var levelKind: GestureMath.Level? = null
    private var lastJump = 0L
    private var longFired = false
    private var hinted = false
    private var armedAt = 0L

    /** A gesture of ours is going on (the host takes every touch until it ends); a finger that rests and has been told what it can do is ours too (a page that sees only a rest must not take it for a tap of its own). */
    val active: Boolean get() = s == S.SCRUB || s == S.LEVEL || s == S.ARMED || (s == S.PENDING && hinted)
    /** The finger is on the glass and the touch has not been given away. */
    val tracking: Boolean get() = s != S.IDLE && s != S.PASSED && s != S.IGNORED

    // [t] = the event's own time (a busy UI thread may handle a move before the rest timer rings: the finger rested long enough when the EVENTS say so)
    fun down(x: Float, y: Float, t: Long = h.now()) {
        resetState()
        startX = x; startY = y; lastY = y; downAt = t
        inBounds = x >= cfg.edgePx && x <= cfg.widthPx - cfg.edgePx && y >= cfg.edgePx && y <= cfg.heightPx - cfg.edgePx
        s = S.PENDING
        if (inBounds) h.scheduleHold(HOLD_MS)
    }

    fun move(x: Float, y: Float, t: Long = h.now()) {
        val dx = x - startX; val dy = y - startY
        when (s) {
            S.PENDING -> {
                if (abs(dx) < cfg.slopPx && abs(dy) < cfg.slopPx) return
                h.cancelHold()
                if (hinted) { hinted = false; h.hint(null, false) }                          // the finger moves: the hint has done its job
                if (!inBounds) { s = S.IGNORED; return }
                if (cfg.holdFirst) {
                    if (t - downAt >= HOLD_MS) { s = S.ARMED; armedAt = t; h.hint(GestureMath.hintAt(startX / cfg.widthPx.toDouble()), true); h.hint(null, false); move(x, y, t); return }   // it rested long enough before it moved: a long press (the timer is late)
                    s = S.PASSED; h.passThrough(); return                                    // the plain drag is the page's
                }
                s = S.UNDECIDED
                decide(x, y)
            }
            S.UNDECIDED -> decide(x, y)
            S.LEVEL -> vertical(x, y)
            S.ARMED -> {
                if (abs(dx) < cfg.slopPx && abs(dy) < cfg.slopPx) return
                h.cancelHold()
                if (hinted) { hinted = false; h.hint(null, false) }
                val coef = if (dx == 0f) Double.MAX_VALUE else abs(dy / dx).toDouble()
                if (coef > GestureMath.STEEP) { s = S.LEVEL; levelKind = GestureMath.levelAt(startX / cfg.widthPx.toDouble()); lastY = startY; vertical(x, y, armed = true) }
                else { s = S.SCRUB; scrub(x, y) }
            }
            S.SCRUB -> scrub(x, y)
            else -> {}
        }
    }

    /** No long press (VLC): both rules are checked on every move from where the touch began; the first one met decides, and the decision stays until the finger lets go. */
    private fun decide(x: Float, y: Float) {
        val dx = x - startX; val dy = y - startY
        val coef = if (dx == 0f) Double.MAX_VALUE else abs(dy / dx).toDouble()
        if (coef > GestureMath.STEEP) {
            if (abs(dy) / cfg.rangePx.toDouble() < GestureMath.VERTICAL_START) return                                     // (the first 5 % is the dead start, as in VLC; also in the middle strip: a sideways drag that begins with a small vertical wobble is still a seek, review of alpha61)
            val kind = GestureMath.levelAt(startX / cfg.widthPx.toDouble())
            if (kind == null) { if (cfg.middlePasses) { s = S.PASSED; h.passThrough() } else s = S.IGNORED; return }      // the middle strip: no level there
            levelKind = kind; s = S.LEVEL; lastY = y
        } else if (GestureMath.cm(abs(dx).toDouble(), cfg.xdpi.toDouble()) >= GestureMath.SEEK_MIN_CM) {
            s = S.SCRUB; scrub(x, y)
        }
    }

    fun up(x: Float, y: Float, t: Long = h.now()) {
        val st = s
        h.cancelHold()
        when (st) {
            S.PENDING -> {
                if (hinted) { hinted = false; h.hint(null, false) }                          // it rested and let go: no tap
                else {
                    val quick = t - downAt <= TAP_MS
                    if (quick && abs(x - startX) < cfg.slopPx && abs(y - startY) < cfg.slopPx && cfg.widthPx > 0f) h.tap((startX / cfg.widthPx).toDouble().coerceIn(0.0, 1.0))
                }
            }
            S.SCRUB -> {
                val (jump, _) = jumpNow(x, y)
                if (jump != 0L && h.lengthMs() > 0L) h.seekCommit(jump) else h.seekCancel()
            }
            else -> {}
        }
        if (st == S.SCRUB || st == S.LEVEL || st == S.ARMED) h.finished()
        s = S.IDLE
    }

    fun cancel() {
        val st = s
        h.cancelHold()
        if (hinted) { hinted = false; h.hint(null, false) }
        if (st == S.SCRUB) h.seekCancel()
        if (st == S.SCRUB || st == S.LEVEL || st == S.ARMED) h.finished()
        s = S.IDLE
    }

    /** The timer the host was asked for rang: the rest (the hint) and, a while later, the long rest. */
    fun hold() {
        when (s) {
            S.PENDING -> {
                if (!inBounds) return
                if (!hinted) {
                    hinted = true
                    if (cfg.holdFirst) { s = S.ARMED; armedAt = h.now() }
                    h.hint(GestureMath.hintAt(startX / cfg.widthPx.toDouble()), cfg.holdFirst)
                    h.scheduleHold(LONG_HOLD_MS - HOLD_MS)
                } else if (!longFired) { longFired = true; h.longHold() }
            }
            S.ARMED -> if (!longFired) { longFired = true; h.longHold() }
            else -> {}
        }
    }

    // ---------------------------------------------------------------------------------------- the two drags

    private fun jumpNow(x: Float, y: Float): Pair<Long, Double> {
        val xcm = GestureMath.cm((x - startX).toDouble(), cfg.xdpi.toDouble())
        val coef = GestureMath.fineCoef((y - startY).toDouble(), cfg.xdpi.toDouble())
        val raw = GestureMath.jumpMs(xcm, coef)
        return Pair(GestureMath.clampJump(raw, h.positionMs(), h.lengthMs()), coef)
    }

    private fun scrub(x: Float, y: Float) {
        if (h.lengthMs() <= 0L) { if (lastJump != Long.MIN_VALUE) { lastJump = Long.MIN_VALUE; h.seekUnavailable() }; return }
        val (jump, coef) = jumpNow(x, y)
        if (jump == 0L) { if (lastJump != 0L) { lastJump = 0L; h.seekCancel() }; return }
        lastJump = jump
        h.seekPreview(jump, h.positionMs() + jump, coef)
    }

    private fun vertical(x: Float, y: Float, armed: Boolean = false) {
        if (s != S.LEVEL) return
        val kind = levelKind ?: run { s = S.IGNORED; return }
        if (armed) lastY = startY
        val dy = y - lastY
        if (dy != 0f) { lastY = y; h.level(kind, GestureMath.levelDelta(dy.toDouble(), cfg.rangePx.toDouble())) }
    }

    private fun resetState() { s = S.IDLE; levelKind = null; lastJump = 0L; longFired = false; hinted = false; armedAt = 0L }

    companion object {
        /** A finger that rests this long without moving is told what it can do there (and, in the long-press-first rule, ARMS the gestures). */
        const val HOLD_MS = 350L
        /** ... and this long (counted from the touch) gives the second stage, [Host.longHold]. */
        const val LONG_HOLD_MS = 800L
        /** A touch this short that goes nowhere is a tap. */
        const val TAP_MS = 450L
    }
}
