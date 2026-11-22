package app.kestrel.core.input

/**
 * The arithmetic of the floating keyboard (N3, docs/FLOATING.md), kept apart from any view so it is tested on the JVM: where the panel may stand, how a drag of a corner changes its size, when a drop docks it.
 * All sizes are in the same unit (pixels or dp, whatever the caller uses) unless a name says dp.
 */
object FloatingMath {
    const val MIN_W_DP = 232f
    const val MAX_W_DP = 620f
    const val MIN_H_MUL = 0.7f
    const val MAX_H_MUL = 2.2f
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
