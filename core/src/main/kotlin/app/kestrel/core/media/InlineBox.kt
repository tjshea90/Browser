package app.kestrel.core.media

/**
 * ANS55f (owner message 55: "Can the player be inside the website so the video plays but I still see the website around it?"): where Kestrel's own player sits over the page when it plays INSIDE the page.
 * Pure rules, no Android. The page says where the video's box is ([Page], CSS pixels, from getBoundingClientRect and visualViewport); this says where that is in the app's view ([Rect], pixels), how
 * the box moves when the page scrolls, and how much of it is in view. A page this cannot place (zoomed, an unusable box, the box far outside the view) gets null: the film then plays in the full-screen player.
 */
object InlineBox {
    /**
     * The box of the element in the layout viewport and what the visual viewport is (all CSS pixels). [pageX] / [pageY] = where the visual viewport sits in the document (the scroll position at the press).
     * [follows] = the box moves with the page's own (root) scroll: false for an element in a frame, under a fixed or sticky ancestor, or in a scroller of its own (nothing can place or follow it).
     */
    class Page(val x: Double, val y: Double, val w: Double, val h: Double, val vvScale: Double, val vvOffX: Double, val vvOffY: Double, val vvW: Double,
               val pageX: Double = 0.0, val pageY: Double = 0.0, val follows: Boolean = true)

    /** A box in view pixels (left, top, right, bottom). */
    class Rect(val l: Int, val t: Int, val r: Int, val b: Int) {
        val w: Int get() = r - l
        val h: Int get() = b - t
        override fun equals(other: Any?) = other is Rect && l == other.l && t == other.t && r == other.r && b == other.b
        override fun hashCode() = ((l * 31 + t) * 31 + r) * 31 + b
        override fun toString() = "[$l,$t ${w}x$h]"
    }

    const val MIN_W = 200            // a box smaller than this is a thumbnail, not a film
    const val MIN_H = 112
    const val SCALE_OK_LOW = 0.97    // the page is not pinch-zoomed (the visual viewport scale is 1)
    const val SCALE_OK_HIGH = 1.03

    /** The element's box in view pixels, or null when the numbers cannot be trusted. [viewW] x [viewH] = the page view in pixels. */
    fun toView(p: Page, viewW: Int, viewH: Int): Rect? {
        if (viewW <= 0 || viewH <= 0 || p.vvW <= 0.0 || !p.follows) return null
        if (p.vvScale < SCALE_OK_LOW || p.vvScale > SCALE_OK_HIGH) return null
        val k = viewW / p.vvW                                   // view pixels per CSS pixel
        val l = Math.round((p.x - p.vvOffX) * k).toInt()
        val t = Math.round((p.y - p.vvOffY) * k).toInt()
        val r = Math.round((p.x - p.vvOffX + p.w) * k).toInt()
        val b = Math.round((p.y - p.vvOffY + p.h) * k).toInt()
        val box = Rect(l, t, r, b)
        if (box.w < MIN_W * k.coerceAtMost(1.0) || box.h < MIN_H * k.coerceAtMost(1.0) || box.w < 4 || box.h < 4) return null
        if (visible(box, viewW, viewH) < 0.5) return null       // it was not on the screen: the press came from somewhere the person cannot see
        return box
    }

    /** View pixels per CSS pixel for this page (the visual viewport is [Page.vvW] CSS pixels wide in a view [viewW] pixels wide). */
    fun perCss(p: Page, viewW: Int): Double = if (p.vvW > 0.0) viewW / p.vvW else 0.0

    /** What the numbers of the engine's compositor scroll updates mean. CSS = the page's own CSS pixels (what window.scrollY says); SCREEN = pixels of the screen at the update's zoom. Settled by the emulator lab (ANS55f). */
    enum class ScrollUnit { CSS, SCREEN }
    @JvmField var scrollUnit = ScrollUnit.CSS

    /**
     * Follows the page's scroll with the engine's compositor updates (no message from the page: a starved page script is exactly what makes the page's own player lag). [box0] is the box at the press (view
     * pixels), [baseX] / [baseY] the visual viewport's place in the document at that moment (CSS pixels), [k0] the view pixels per CSS pixel then. The first update's zoom is remembered; a zoom that differs
     * from it by more than 3 % (a pinch) makes [at] return null: the box can no longer be told, the overlay closes.
     */
    class Track(val box0: Rect, private val baseX: Double, private val baseY: Double, private val k0: Double) {
        private var zoomRef = 0.0
        /** The box for the compositor's scroll position ([sx], [sy]) at [zoom], or null when it cannot be told (a pinch, nonsense numbers). */
        fun at(sx: Double, sy: Double, zoom: Double): Rect? {
            if (!(zoom > 0.0) || k0 <= 0.0 || sx.isNaN() || sy.isNaN()) return null
            if (zoomRef == 0.0) zoomRef = zoom
            else if (Math.abs(zoom - zoomRef) > zoomRef * 0.03) return null
            val cssX = if (scrollUnit == ScrollUnit.CSS) sx else sx / zoom
            val cssY = if (scrollUnit == ScrollUnit.CSS) sy else sy / zoom
            val dx = Math.round((cssX - baseX) * k0).toInt()
            val dy = Math.round((cssY - baseY) * k0).toInt()
            if (Math.abs(dx) > 200_000 || Math.abs(dy) > 200_000) return null
            return moved(box0, dx, dy)
        }
    }

    /** The box after the page scrolled: [dx] / [dy] are the change of the scroll position in view pixels (a page that scrolls DOWN has a positive dy and its content moves UP). */
    fun moved(r: Rect, dx: Int, dy: Int) = Rect(r.l - dx, r.t - dy, r.r - dx, r.b - dy)

    /** The part of the box inside the view, 0.0 to 1.0. */
    fun visible(r: Rect, viewW: Int, viewH: Int): Double {
        val area = r.w.toLong() * r.h.toLong()
        if (area <= 0L) return 0.0
        val iw = (minOf(r.r, viewW) - maxOf(r.l, 0)).coerceAtLeast(0)
        val ih = (minOf(r.b, viewH) - maxOf(r.t, 0)).coerceAtLeast(0)
        return (iw.toLong() * ih.toLong()).toDouble() / area
    }

    /** What the overlay does as the box moves: keep playing, pause (most of it is out of view), or stop (all of it is). */
    enum class Edge { PLAY, PAUSE, STOP }

    fun edgeFor(r: Rect, viewW: Int, viewH: Int): Edge {
        val v = visible(r, viewW, viewH)
        return when { v >= 0.5 -> Edge.PLAY; v > 0.0 -> Edge.PAUSE; else -> Edge.STOP }
    }

    /** The picture inside its box, letterboxed: [picW] x [picH] (pixels, 0 = not known yet) fitted in [box] without stretching. A film of unknown shape fills nothing yet (null). */
    fun letterbox(box: Rect, picW: Int, picH: Int, pixelRatio: Float = 1f): Rect? {
        if (picW <= 0 || picH <= 0 || box.w <= 0 || box.h <= 0) return null
        val ar = picW * (if (pixelRatio > 0f) pixelRatio else 1f) / picH.toFloat()
        val bw: Int; val bh: Int
        if (box.w / box.h.toFloat() > ar) { bh = box.h; bw = Math.round(bh * ar) } else { bw = box.w; bh = Math.round(bw / ar) }
        val l = box.l + (box.w - bw) / 2; val t = box.t + (box.h - bh) / 2
        return Rect(l, t, l + bw, t + bh)
    }
}
