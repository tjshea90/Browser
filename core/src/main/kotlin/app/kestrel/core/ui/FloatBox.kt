package app.kestrel.core.ui

/**
 * FD1 (owner message 2026-11-18: "make the download manager float and be resizeable and moved around the screen just like the keyboard"): the geometry of a small window that floats over the page, moved by a
 * finger and resized from a corner, the way the mini keyboard's panel does it. Pure arithmetic in dp, kept apart from any view so it is tested without a screen. The window hangs from the TOP RIGHT of the
 * area (like the panel): [Geo.right] is the gap to the right edge, [Geo.top] the gap to the top edge, [Geo.w] the width and [Geo.h] the height the CONTENT may take before it scrolls (0 = the default).
 */
object FloatBox {
    class Geo(val right: Float, val top: Float, val w: Float, val h: Float) {
        override fun equals(other: Any?) = other is Geo && Math.abs(other.right - right) < 0.01f && Math.abs(other.top - top) < 0.01f && Math.abs(other.w - w) < 0.01f && Math.abs(other.h - h) < 0.01f
        override fun hashCode() = (right * 31 + top * 17 + w * 7 + h).toInt()
        override fun toString() = "right=$right top=$top w=$w h=$h"
    }

    /** What a window may be: the smallest and biggest width, the smallest and biggest content height, and the gap it keeps to every edge. */
    class Limits(val minW: Float, val maxW: Float, val minH: Float, val maxH: Float, val edge: Float = 4f)

    /** The size and place it has without a stored choice. */
    class Default(val right: Float, val top: Float, val w: Float, val h: Float) { fun geo() = Geo(right, top, w, h) }

    /** The window inside an area of [areaW] x [areaH] dp: its size within the limits (and never wider / taller than the area), its place so that all of it is on screen. [boxH] = the whole window's height now (content + header). */
    fun clamp(g: Geo, areaW: Float, areaH: Float, boxH: Float, lim: Limits): Geo {
        val maxW = Math.max(lim.minW, Math.min(lim.maxW, areaW - 2 * lim.edge))
        val w = g.w.coerceIn(lim.minW, maxW)
        val maxH = Math.max(lim.minH, Math.min(lim.maxH, areaH - 2 * lim.edge - (boxH - Math.min(g.h, boxH).coerceAtLeast(0f))))
        val h = g.h.coerceIn(lim.minH, maxH)
        val right = g.right.coerceIn(lim.edge, Math.max(lim.edge, areaW - w - lim.edge))
        val top = g.top.coerceIn(lim.edge, Math.max(lim.edge, areaH - Math.min(boxH, areaH - lim.edge) - lim.edge))
        return Geo(right, top, w, h)
    }

    /** The finger moved by [dx], [dy] dp from where the drag began (right = positive): the window follows it; the gap to the right edge shrinks when the finger goes right. */
    fun moved(start: Geo, dx: Float, dy: Float, areaW: Float, areaH: Float, boxH: Float, lim: Limits): Geo =
        clamp(Geo(start.right - dx, start.top + dy, start.w, start.h), areaW, areaH, boxH, lim)

    /** The bottom LEFT corner was dragged by [dx], [dy] dp: the window is anchored at the top right, so it gets wider when the corner goes left and taller when it goes down. */
    fun resizedFromBottomLeft(start: Geo, dx: Float, dy: Float, areaW: Float, areaH: Float, boxH: Float, lim: Limits): Geo =
        clamp(Geo(start.right, start.top, start.w - dx, start.h + dy), areaW, areaH, boxH, lim)

    /** The bottom RIGHT corner was dragged: the right edge moves with the finger (the gap shrinks / grows) and the width follows it. */
    fun resizedFromBottomRight(start: Geo, dx: Float, dy: Float, areaW: Float, areaH: Float, boxH: Float, lim: Limits): Geo =
        clamp(Geo(start.right - dx, start.top, start.w + dx, start.h + dy), areaW, areaH, boxH, lim)

    /** True when the window is where and as big as it starts (nothing to remember: the stored choice is dropped). */
    fun isDefault(g: Geo, d: Default): Boolean = g == d.geo()

    /** The four whole numbers a preference keeps (dp), or null for the default. */
    fun store(g: Geo, d: Default): IntArray? = if (isDefault(g, d)) null else intArrayOf(Math.round(g.right), Math.round(g.top), Math.round(g.w), Math.round(g.h))

    /** What a preference kept, or the default when nothing was kept or the numbers are not four. */
    fun load(a: IntArray?, d: Default): Geo = if (a != null && a.size == 4) Geo(a[0].toFloat(), a[1].toFloat(), a[2].toFloat(), a[3].toFloat()) else d.geo()
}
