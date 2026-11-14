package app.kestrel.core.input

/** One key's box in the keyboard view's own coordinates (pixels). */
class KeyBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
}

/**
 * Where the keys are. ONE function lays the rows out, and both the view that draws and touches them and the glide decoder (AT1: it needs the centre of every letter key) call it, so what the
 * decoder believes is under the finger is exactly what is drawn.
 */
object MiniKeyboardGeometry {
    /**
     * The boxes of [rows] in a view [width] x [height] wide: [top] pixels stay free above the first row (the candidate strip), [gap] pixels lie between keys and between rows. A row's keys
     * share the width by their weights (a spec with a null key is just a gap of its weight).
     */
    fun boxes(rows: List<List<KeySpec>>, width: Float, height: Float, gap: Float, top: Float = 0f): List<List<KeyBox>> {
        if (rows.isEmpty()) return emptyList()
        val rowH = (height - top - gap * (rows.size - 1)) / rows.size
        val out = ArrayList<List<KeyBox>>(rows.size)
        for ((i, row) in rows.withIndex()) {
            val total = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = (width - gap * (row.size - 1)) / total
            var x = 0f
            val y = top + i * (rowH + gap)
            val line = ArrayList<KeyBox>(row.size)
            for (k in row) { val w = k.weight * unit; line.add(KeyBox(x, y, x + w, y + rowH)); x += w + gap }
            out.add(line)
        }
        return out
    }
}
