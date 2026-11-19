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
    fun boxes(rows: List<List<KeySpec>>, width: Float, height: Float, gap: Float, top: Float = 0f, rowWeights: List<Float>? = null): List<List<KeyBox>> {
        if (rows.isEmpty()) return emptyList()
        // [rowWeights] make a row taller or thinner than the others (the number row is thinner); null = all rows alike. Drawing, touching and the glide decoder all pass the same weights.
        val weights = if (rowWeights != null && rowWeights.size == rows.size) rowWeights else List(rows.size) { 1f }
        val heightTotal = weights.sum()
        val usable = height - top - gap * (rows.size - 1)
        val out = ArrayList<List<KeyBox>>(rows.size)
        for ((i, row) in rows.withIndex()) {
            val rowTotal = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = (width - gap * (row.size - 1)) / rowTotal
            var x = 0f
            val y = top + i * gap + usable * weights.take(i).sum() / heightTotal
            val rowH = usable * weights[i] / heightTotal
            val line = ArrayList<KeyBox>(row.size)
            for (k in row) { val w = k.weight * unit; line.add(KeyBox(x, y, x + w, y + rowH)); x += w + gap }
            out.add(line)
        }
        return out
    }

    /**
     * The photo's geometry (owner M1): every row has the SAME key pitch, a key is a SLOT minus a gap, and an empty spec (null key) is an empty slot, so the half-key indent of the home row lines up
     * exactly with the rows above and below it. A key of weight w occupies w * unit pixels of its row ([unit] = (width + [gapX]) / row total) and is drawn [gapX] narrower than that; rows are
     * [gapY] apart and share [height] (less [top]) by [rowWeights]. [boxes] is kept as it was for the browser's address-bar keyboard.
     */
    fun slots(rows: List<List<KeySpec>>, width: Float, height: Float, gapX: Float, gapY: Float, top: Float = 0f, rowWeights: List<Float>? = null): List<List<KeyBox>> {
        if (rows.isEmpty()) return emptyList()
        val weights = if (rowWeights != null && rowWeights.size == rows.size) rowWeights else List(rows.size) { 1f }
        val heightTotal = weights.sum()
        val usable = height - top - gapY * (rows.size - 1)
        val out = ArrayList<List<KeyBox>>(rows.size)
        for ((i, row) in rows.withIndex()) {
            val rowTotal = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = (width + gapX) / rowTotal
            var x = 0f
            val y = top + i * gapY + usable * weights.take(i).sum() / heightTotal
            val rowH = usable * weights[i] / heightTotal
            val line = ArrayList<KeyBox>(row.size)
            for (k in row) { val slot = k.weight * unit; line.add(KeyBox(x, y, Math.max(x, x + slot - gapX), y + rowH)); x += slot }
            out.add(line)
        }
        return out
    }
}
