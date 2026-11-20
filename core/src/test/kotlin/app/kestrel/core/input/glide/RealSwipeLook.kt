package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * R24b step 1: LOOK at real swipes. Skipped unless REAL_SWIPES=1. Fetches a slice (REAL_SWIPES_MB, default 8) of the corpus to REAL_SWIPES_FILE (default build/real-swipes.jsonl)
 * if it is not there yet, then prints what the data looks like (canvas sizes, orientations, points per swipe, the mean start / end point of each letter) so the key
 * geometry can be CALIBRATED from the data itself (the dataset card does not say where its keys are).
 */
class RealSwipeLook {
    @Test fun look() {
        assumeTrue("set REAL_SWIPES=1", System.getenv("REAL_SWIPES") == "1")
        val f = File(System.getenv("REAL_SWIPES_FILE") ?: "build/real-swipes.jsonl")
        if (!f.exists()) println("fetched " + RealSwipe.fetch(System.getenv("REAL_SWIPES_URL") ?: RealSwipe.DEV_URL, (System.getenv("REAL_SWIPES_MB")?.toLongOrNull() ?: 8L) * 1_000_000, f) + " bytes")
        val sw = RealSwipe.read(f)
        println("swipes: ${sw.size}")
        println("orientations: " + sw.groupingBy { it.orientation }.eachCount())
        println("canvas (w x h) top: " + sw.groupingBy { "${it.canvasW.toInt()}x${it.canvasH.toInt()}" }.eachCount().entries.sortedByDescending { it.value }.take(8))
        println("points per swipe: avg %.1f, min %d, max %d".format(sw.map { it.xs.size }.average(), sw.minOf { it.xs.size }, sw.maxOf { it.xs.size }))
        println("x range %.3f..%.3f  y range %.3f..%.3f".format(sw.minOf { it.xs.min() }, sw.maxOf { it.xs.max() }, sw.minOf { it.ys.min() }, sw.maxOf { it.ys.max() }))
        println("duration ms avg %.0f".format(sw.map { (it.ts.last() - it.ts.first()).toDouble() }.average()))
        val sx = HashMap<Char, MutableList<FloatArray>>(); val ex = HashMap<Char, MutableList<FloatArray>>()
        for (s in sw) { val l = RealSwipe.letters(s.word) ?: continue; sx.getOrPut(l.first()) { ArrayList() }.add(floatArrayOf(s.xs.first(), s.ys.first())); ex.getOrPut(l.last()) { ArrayList() }.add(floatArrayOf(s.xs.last(), s.ys.last())) }
        for (c in "qwertyuiopasdfghjklzxcvbnm") {
            val a = sx[c].orEmpty(); val b = ex[c].orEmpty()
            fun m(l: List<FloatArray>, i: Int) = if (l.isEmpty()) Float.NaN else l.map { it[i] }.average().toFloat()
            println("%c start n=%4d (%.3f, %.3f)   end n=%4d (%.3f, %.3f)".format(c, a.size, m(a, 0), m(a, 1), b.size, m(b, 0), m(b, 1)))
        }
        for (s in sw.take(5)) println("sample '${s.word}' sentence='${s.sentence.take(50)}' pts=${s.xs.size} first=(${s.xs[0]}, ${s.ys[0]}) last=(${s.xs.last()}, ${s.ys.last()}) canvas=${s.canvasW}x${s.canvasH} ${s.orientation}")
    }
}
