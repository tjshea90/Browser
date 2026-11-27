package app.kestrel.core.ui

/**
 * MT1 (research/MT1_MINI_TABS.md): the geometry of the tab tray inside the floating navigation panel, in dp, without any view. A list of [count] rows of [rowH] dp (at most [maxRows] show, the rest scroll by
 * dragging), then one footer row ("New tab") that is always visible. A press on a row's right edge ([closeW] dp) closes that tab, anywhere else on the row switches to it.
 */
class TabTrayModel(val rowH: Float = 40f, val maxRows: Int = 5, val footerH: Float = 40f, val closeW: Float = 40f) {
    sealed class Hit {
        object None : Hit()
        class Row(val index: Int) : Hit()
        class Close(val index: Int) : Hit()
        object New : Hit()
    }

    var count = 0; private set
    var scroll = 0f; private set

    val visibleRows: Int get() = Math.min(count, maxRows)
    /** Height of the list part. */
    val listH: Float get() = visibleRows * rowH
    /** Height of the whole tray: the list and the footer. */
    val height: Float get() = listH + footerH
    val maxScroll: Float get() = Math.max(0f, count * rowH - listH)

    fun setCount(n: Int) { count = Math.max(0, n); scroll = scroll.coerceIn(0f, maxScroll) }

    /** Drag the list: a finger moving UP by [dy] (negative) shows later rows. @return true when the list moved. */
    fun scrollBy(dy: Float): Boolean { val old = scroll; scroll = (scroll - dy).coerceIn(0f, maxScroll); return scroll != old }

    /** Scroll just enough that row [index] is fully in view. */
    fun ensureVisible(index: Int) {
        if (index < 0 || index >= count) return
        val top = index * rowH; val bottom = top + rowH
        if (top < scroll) scroll = top else if (bottom > scroll + listH) scroll = bottom - listH
        scroll = scroll.coerceIn(0f, maxScroll)
    }

    /** What is under the point [x], [y] (dp, inside the tray, [width] dp wide). */
    fun hit(x: Float, y: Float, width: Float): Hit {
        if (x < 0f || x > width || y < 0f || y > height) return Hit.None
        if (y >= listH) return Hit.New
        val i = ((y + scroll) / rowH).toInt()
        if (i < 0 || i >= count) return Hit.None
        return if (x >= width - closeW) Hit.Close(i) else Hit.Row(i)
    }

    /** The top of row [index] inside the tray (dp, scroll applied; may be negative or beyond the list when the row is scrolled out). */
    fun rowTop(index: Int): Float = index * rowH - scroll
}
