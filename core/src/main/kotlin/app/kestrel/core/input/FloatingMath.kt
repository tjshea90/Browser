package app.kestrel.core.input

/**
 * The arithmetic of the floating keyboard (N3, docs/FLOATING.md), kept apart from any view so it is tested on the JVM: where the panel may stand, how a drag of a corner changes its size, when a drop docks it.
 * All sizes are in the same unit (pixels or dp, whatever the caller uses) unless a name says dp.
 */
object FloatingMath {
    const val MIN_W_DP = 232f
    const val MAX_W_DP = 620f
    /** Owner R23e (2026-11-18, "can the keyboard be made to be even shorter than its current minimum vertical size?"): the rows may be dragged down to this share of their normal height (it was 0.7). */
    const val MIN_H_MUL = 0.45f
    const val MAX_H_MUL = 2.2f
    /** The docked keyboard's rows may be this much shorter / taller than the settings say (owner R19c: resize it both ways while it sits at the bottom). */
    const val DOCK_MIN_H_MUL = 0.45f
    const val DOCK_MAX_H_MUL = 1.8f
    /** ... and the docked keyboard never grows past this share of the screen's height (unless it already was taller: a phone held sideways). */
    const val DOCK_MAX_SCREEN_FRAC = 0.6f
    /** Dropping the panel with its bottom this close to the screen bottom docks it again (dp). */
    const val DOCK_NEAR_DP = 24f
    /** Dragging the grip up by this much detaches without waiting for the long press (dp). */
    const val DETACH_DRAG_DP = 28f

    fun clampWidthDp(w: Float, screenWDp: Float): Float = w.coerceIn(MIN_W_DP.coerceAtMost(screenWDp), MAX_W_DP.coerceAtMost(screenWDp))
    fun clampHeightMul(m: Float): Float = m.coerceIn(MIN_H_MUL, MAX_H_MUL)

    /** The width a panel gets the first time it floats: a comfortable phone / tablet size. */
    fun defaultWidthDp(screenWDp: Float): Float = clampWidthDp(if (screenWDp >= 600f) 460f else 360f, screenWDp)

    /** Left edge of a panel [panelW] wide whose position is [frac] (0..1000) of the free room across [screenW]. */
    fun left(frac: Int, screenW: Int, panelW: Int): Int = ((frac.coerceIn(0, 1000) / 1000f) * maxOf(0, screenW - panelW)).toInt()
    fun top(frac: Int, screenH: Int, panelH: Int): Int = ((frac.coerceIn(0, 1000) / 1000f) * maxOf(0, screenH - panelH)).toInt()

    /** The fraction (0..1000) that puts the panel at [pos] when [room] is the free space. */
    fun frac(pos: Int, room: Int): Int = if (room <= 0) 500 else ((pos.coerceIn(0, room) * 1000L) / room).toInt()

    /** The same without the rounding to a thousandth: a position as a fraction of the free room, and back. Going there and back gives the same pixel (a held finger must never move the keyboard). */
    fun fracF(pos: Int, room: Int): Float = if (room <= 0) 0.5f else pos.coerceIn(0, room).toFloat() / room
    fun posF(frac: Float, room: Int): Int = Math.round(frac.coerceIn(0f, 1f) * maxOf(0, room))

    /** [v] kept so that a panel [size] long stays fully inside 0..[limit]. */
    fun clampPos(v: Int, size: Int, limit: Int): Int = v.coerceIn(0, maxOf(0, limit - size))

    /** True when a drop with the panel's bottom edge at [bottom] on a screen [screenH] high (density [d]) docks it. */
    fun dropsToDock(bottom: Int, screenH: Int, d: Float): Boolean = bottom >= screenH - (DOCK_NEAR_DP * d)

    fun clampDockMul(m: Float): Float = m.coerceIn(DOCK_MIN_H_MUL, DOCK_MAX_H_MUL)

    /** The docked keyboard's rectangle while an end arrow of its bar is dragged: [left], [width] and [height] (it always stands on the bottom edge). */
    class DockSize(val left: Int, val width: Int, val height: Int)

    /**
     * Owner R19c: "when the keyboard is docked at the bottom of the screen, let me resize it both horizontally and vertically". The arrow at one end of the bar works like a top corner of a floating keyboard: [side] 0 is the left arrow
     * (the left edge follows the finger, the right edge stays), 1 the right arrow. The finger moved ([dx], [dy]) from where it went down, with the keyboard at [l] / [w] wide / [h] tall: sideways makes it narrower or wider
     * (kept between [minW] and the screen), UP makes it TALLER and down shorter (between [minH] and [maxH]); the bottom edge never leaves the bottom of the screen ([screenW] wide).
     */
    fun dockedResize(side: Int, l: Int, w: Int, h: Int, dx: Int, dy: Int, screenW: Int, minW: Int, minH: Int, maxH: Int): DockSize {
        val nw = (if (side == 0) w - dx else w + dx).coerceIn(minW, maxOf(minW, screenW))
        val right = l + w
        val nl = (if (side == 0) right - nw else l).coerceIn(0, maxOf(0, screenW - nw))
        val nh = (h - dy).coerceIn(minH, maxOf(minH, maxH))
        return DockSize(nl, nw, nh)
    }

    /** The vertical move of a drag after a dead zone of [dead] (a finger drifting a little while it squeezes sideways must not change the height): zero inside it, continuous outside. */
    fun softDead(v: Float, dead: Float): Float = if (v > dead) v - dead else if (v < -dead) v + dead else 0f

    /** Which corner of the panel was grabbed: 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right. */
    class Resize(val left: Int, val top: Int, val width: Int, val height: Int)

    /**
     * Dragging [corner] by ([dx], [dy]) from a panel at ([l], [t]) of size [w] x [h]: the opposite corner stays put (left corners keep the right edge, top corners keep the bottom). The new width is clamped to
     * [minW]..[maxW] and the new height to [minH]..[maxH]; the result is kept on a screen [screenW] x [screenH].
     */
    fun resize(corner: Int, l: Int, t: Int, w: Int, h: Int, dx: Int, dy: Int, minW: Int, maxW: Int, minH: Int, maxH: Int, screenW: Int, screenH: Int): Resize {
        val leftCorner = corner == 0 || corner == 2; val topCorner = corner == 0 || corner == 1
        val nw = (if (leftCorner) w - dx else w + dx).coerceIn(minW, maxOf(minW, maxW))
        val nh = (if (topCorner) h - dy else h + dy).coerceIn(minH, maxOf(minH, maxH))
        val right = l + w; val bottom = t + h
        val nl = clampPos(if (leftCorner) right - nw else l, nw, screenW)
        val nt = clampPos(if (topCorner) bottom - nh else t, nh, screenH)
        return Resize(nl, nt, nw, nh)
    }
}
