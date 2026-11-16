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
}

/**
 * The touch rules of the film, from one finger: a tap, a long press that arms the precise seek, the precise seek itself, and the two vertical level drags. The host (an Android view) feeds [down], [move], [up],
 * [cancel] and calls [hold] when the timer it was asked for ([Host.scheduleHold]) rings; everything it does comes back through [Host].
 *
 *  - a short touch that goes nowhere is a [Host.tap] (the controls toggle and the double tap seeks, TapMachine);
 *  - a finger that rests [HOLD_MS] without moving ARMS the precise seek ([Host.armed]: a tick and a hint); the next move decides: sideways = the precise seek (nothing moves until the finger lets go; [Host.seekPreview]
 *    says where it would land), steep = the level of the side it began on; a finger that rests on until [LONG_HOLD_MS] gives [Host.longHold] (the player inside the page opens the download options with it);
 *  - [Config.holdForVertical] = false (the full-screen player): a steep drag without any hold changes the level of its side at once (VLC); true (the player inside the page, where a plain drag scrolls the PAGE): a drag that
 *    starts moving before the hold is not ours ([Host.passThrough]) and the levels need the hold too;
 *  - a horizontal drag without the hold does nothing (the owner asked for the long press); a touch that starts within the edge margin makes no gesture (system gestures live there) but is still a tap.
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
        /** true = a plain drag is the page's (the player inside the page): every gesture needs the long press first. */
        val holdForVertical: Boolean,
    )

    interface Host {
        fun now(): Long
        /** Call [hold] after this long (replaces an earlier request); [cancelHold] withdraws it. */
        fun scheduleHold(ms: Long)
        fun cancelHold()
        /** A short touch that went nowhere: [xFraction] across the view, 0..1. */
        fun tap(xFraction: Double)
        /** The long press armed the precise seek (a tick of the phone, a hint). */
        fun armed()
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
        /** The drag is the page's (hold-first mode): give the touch to whoever is under the view. */
        fun passThrough()
        /** A gesture of ours has ended (or never became one): clean up what was shown. */
        fun finished()
    }

    private enum class S { IDLE, PENDING, ARMED, SCRUB, LEVEL, VWAIT, PASSED, IGNORED }

    private var s = S.IDLE
    private var startX = 0f
    private var startY = 0f
    private var downAt = 0L
    private var inBounds = true
    private var lastY = 0f
    private var levelKind: GestureMath.Level? = null
    private var lastJump = 0L
    private var longFired = false
    private var armedAt = 0L

    /** A gesture of ours is going on (the host takes every touch until it ends). */
    val active: Boolean get() = s == S.SCRUB || s == S.LEVEL || s == S.ARMED
    /** The finger is on the glass and the touch has not been given away. */
    val tracking: Boolean get() = s != S.IDLE && s != S.PASSED && s != S.IGNORED

    fun down(x: Float, y: Float) {
        resetState()
        startX = x; startY = y; lastY = y; downAt = h.now()
        inBounds = x >= cfg.edgePx && x <= cfg.widthPx - cfg.edgePx && y >= cfg.edgePx && y <= cfg.heightPx - cfg.edgePx
        s = S.PENDING
        if (inBounds) h.scheduleHold(HOLD_MS)
    }

    fun move(x: Float, y: Float) {
        val dx = x - startX; val dy = y - startY
        when (s) {
            S.PENDING -> {
                if (abs(dx) < cfg.slopPx && abs(dy) < cfg.slopPx) return
                h.cancelHold()
                if (!inBounds) { s = S.IGNORED; return }
                if (cfg.holdForVertical) { s = S.PASSED; h.passThrough(); return }       // the plain drag is the page's
                val coef = if (dx == 0f) Double.MAX_VALUE else abs(dy / dx).toDouble()
                if (coef > GestureMath.STEEP) { s = S.VWAIT; vertical(x, y) }
                else s = S.IGNORED                                                          // sideways without the long press: nothing (the owner's rule)
            }
            S.VWAIT -> vertical(x, y)
            S.LEVEL -> vertical(x, y)
            S.ARMED -> {
                if (abs(dx) < cfg.slopPx && abs(dy) < cfg.slopPx) return
                h.cancelHold()
                val coef = if (dx == 0f) Double.MAX_VALUE else abs(dy / dx).toDouble()
                if (coef > GestureMath.STEEP) { s = S.LEVEL; levelKind = GestureMath.levelAt(startX / cfg.widthPx.toDouble()); lastY = startY; vertical(x, y, armed = true) }
                else { s = S.SCRUB; scrub(x, y) }
            }
            S.SCRUB -> scrub(x, y)
            else -> {}
        }
    }

    fun up(x: Float, y: Float) {
        val st = s
        h.cancelHold()
        when (st) {
            S.PENDING -> {
                val quick = h.now() - downAt <= TAP_MS
                if (quick && abs(x - startX) < cfg.slopPx && abs(y - startY) < cfg.slopPx && cfg.widthPx > 0f) h.tap((startX / cfg.widthPx).toDouble().coerceIn(0.0, 1.0))
            }
            S.SCRUB -> {
                val (jump, _) = jumpNow(x, y)
                if (jump != 0L && h.lengthMs() > 0L) h.seekCommit(jump) else h.seekCancel()
            }
            else -> {}
        }
        if (st == S.SCRUB || st == S.LEVEL || st == S.ARMED || st == S.VWAIT) h.finished()
        s = S.IDLE
    }

    fun cancel() {
        val st = s
        h.cancelHold()
        if (st == S.SCRUB) h.seekCancel()
        if (st == S.SCRUB || st == S.LEVEL || st == S.ARMED || st == S.VWAIT) h.finished()
        s = S.IDLE
    }

    /** The timer the host was asked for rang. */
    fun hold() {
        when (s) {
            S.PENDING -> { if (!inBounds) return; s = S.ARMED; armedAt = h.now(); h.armed(); h.scheduleHold(LONG_HOLD_MS - HOLD_MS) }
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
        if (s == S.VWAIT) {
            if (abs(y - startY) / cfg.rangePx.toDouble() < GestureMath.VERTICAL_START) return
            levelKind = GestureMath.levelAt(startX / cfg.widthPx.toDouble())
            if (levelKind == null) { s = S.IGNORED; return }                             // the dead zone in the middle
            s = S.LEVEL; lastY = y; return                                                  // (from here on every move counts; the first 5 % is the dead start, as in VLC)
        }
        if (s == S.LEVEL) {
            val kind = levelKind ?: run { s = S.IGNORED; return }
            if (armed) lastY = startY
            val dy = y - lastY
            if (dy != 0f) { lastY = y; h.level(kind, GestureMath.levelDelta(dy.toDouble(), cfg.rangePx.toDouble())) }
        }
    }

    private fun resetState() { s = S.IDLE; levelKind = null; lastJump = 0L; longFired = false; armedAt = 0L }

    companion object {
        /** A finger that rests this long without moving arms the precise seek. */
        const val HOLD_MS = 350L
        /** ... and this long (counted from the touch) gives the second stage, [Host.longHold]. */
        const val LONG_HOLD_MS = 800L
        /** A touch this short that goes nowhere is a tap. */
        const val TAP_MS = 450L
    }
}
