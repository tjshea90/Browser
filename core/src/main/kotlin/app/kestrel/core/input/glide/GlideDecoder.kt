package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.KeyBox
import app.kestrel.core.input.KeySpec

/** Where the 26 letter keys are, in the pixels of the keyboard view; [pitch] is the distance between two neighbouring keys of a row (the unit the decoder measures in). */
class GlideKeys(val cx: FloatArray, val cy: FloatArray, val pitch: Float, val keyW: Float, val keyH: Float) {
    companion object {
        /** From the letter page's [rows] and their [boxes] (see MiniKeyboardGeometry). */
        fun from(rows: List<List<KeySpec>>, boxes: List<List<KeyBox>>): GlideKeys {
            val cx = FloatArray(26) { Float.NaN }; val cy = FloatArray(26) { Float.NaN }
            var w = 0f; var h = 0f
            for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) {
                val c = (k.key as? Key.Ch)?.text ?: continue
                if (c.length != 1) continue
                val ch = c[0].lowercaseChar()
                if (ch !in 'a'..'z') continue
                cx[ch - 'a'] = boxes[i][j].centerX; cy[ch - 'a'] = boxes[i][j].centerY
                w = boxes[i][j].width; h = boxes[i][j].height
            }
            require(cx.none { it.isNaN() }) { "the letter page has no key for every letter" }
            // neighbours on the top row: q w e r t y u i o p
            val top = "qwertyuiop"
            var sum = 0f
            for (i in 0 until top.length - 1) sum += cx[top[i + 1] - 'a'] - cx[top[i] - 'a']
            return GlideKeys(cx, cy, sum / (top.length - 1), w, h)
        }
    }
}

/** Every number the decoder is tuned by, in one place (the accuracy test and the tuning run read and change them). Distances are in key pitches, costs in pitch squared. */
class GlideParams(
    /** Points the finger path is resampled to. */
    val samples: Int = 48,
    /** A word's first / last letter must lie within this many pitches (squared here) of where the finger went down / lifted. */
    val endReach2: Float = 3.2f,
    /** A letter further than this (squared pitches) from the whole path counts as this much, so one missed key does not rule a word out alone. */
    val visitCap: Float = 3.0f,
    /** A letter further than this from the path (squared) was not visited at all: the word is dropped. */
    val missLimit2: Float = 6.0f,
    /** How many words go from the quick pass to the careful one. */
    val keep: Int = 150,
    val quickEnds: Float = 0.7f,
    val quickPrior: Float = 0.12f,
    /** The careful pass: weight of the path between two letters straying from the straight line, and of the first / last letter. */
    val shapeW: Float = 1.0f,
    val endW: Float = 2.0f,
    /** The language: nats of word probability that are worth one unit of path cost. */
    val lmW: Float = 0.30f,
    /** How much of the probability comes from the word before (when the pair is known). */
    val bigramMix: Float = 0.4f,
    /** A path shorter than this many pitches is a tap, not a glide. */
    val minLength: Float = 1.2f,
    /** First / last letter may be matched this many samples away from the very end of the path. */
    val slack: Int = 2,
)

class GlideCandidate(val text: String, val score: Float, val cost: Float, val prior: Float)

/**
 * Turns a finger path over the letter keys into words (AT1). The path is cut into equal steps; every word whose first and last letter are near where the finger went down and lifted is
 * judged in two passes: a quick one that asks only "was each letter of the word visited" (one table of 26 distances answers that for every word), then a careful one that finds, letter by letter, the
 * place along the path where the finger was at that letter, and how far the path between two letters strays from the straight line a word would draw there. The language weighs in the same way a
 * glide keyboard's does: how common the word is, and how likely it is after the word before. No allocation per word, no Android types.
 */
class GlideDecoder(private val keys: GlideKeys, private val p: GlideParams = GlideParams()) {
    private val n = p.samples
    private val kx = FloatArray(26) { keys.cx[it] / keys.pitch }
    private val ky = FloatArray(26) { keys.cy[it] / keys.pitch }
    private val qx = FloatArray(n); private val qy = FloatArray(n)
    private val dKey = FloatArray(26)
    // scratch for the careful pass: prefix sums of squared distance to each segment, and two DP rows
    private val pre = FloatArray(MAX_LETTERS * (n + 1))
    private var fPrev = FloatArray(n); private var fCur = FloatArray(n)
    private val letters = IntArray(MAX_LETTERS)
    // the survivors of the quick pass (a small max-heap by quick score, kept as parallel arrays)
    private val heapScore = FloatArray(p.keep); private val heapLex = IntArray(p.keep); private val heapId = IntArray(p.keep); private var heapN = 0

    /** Last decode's cost of the best candidate, for the diagnostics and the tests. */
    var lastCandidates = 0; private set

    /**
     * [xs]/[ys] are the first [count] touch points in view pixels. [prev] is the word before (lowercase letters) or null. Returns up to [max] words, best first; empty when the path is a tap or
     * nothing fits.
     */
    fun decode(xs: FloatArray, ys: FloatArray, count: Int, lexicons: List<GlideLexicon>, prev: String?, max: Int = 3): List<GlideCandidate> {
        lastCandidates = 0
        if (count < 2 || !resample(xs, ys, count)) return emptyList()
        // how close does the path come to each key
        for (k in 0 until 26) {
            var best = Float.MAX_VALUE
            for (i in 0 until n) { val dx = qx[i] - kx[k]; val dy = qy[i] - ky[k]; val d = dx * dx + dy * dy; if (d < best) best = d }
            dKey[k] = best
        }
        val sx = qx[0]; val sy = qy[0]; val ex = qx[n - 1]; val ey = qy[n - 1]
        val startD = FloatArray(26); val endD = FloatArray(26)
        for (k in 0 until 26) { startD[k] = d2(sx - kx[k], sy - ky[k]); endD[k] = d2(ex - kx[k], ey - ky[k]) }

        // ---- quick pass
        heapN = 0
        for ((li, lex) in lexicons.withIndex()) {
            for (f in 0 until 26) {
                if (startD[f] > p.endReach2) continue
                for (l in 0 until 26) {
                    if (endD[l] > p.endReach2) continue
                    val ids = lex.bucket(f, l)
                    for (id in ids) quick(lex, li, id, startD[f] + endD[l])
                }
            }
        }
        lastCandidates = heapN
        if (heapN == 0) return emptyList()

        // ---- careful pass, then the language
        val ids = prevIds(lexicons, prev)
        val out = ArrayList<GlideCandidate>(heapN)
        for (h in 0 until heapN) {
            val lex = lexicons[heapLex[h]]; val id = heapId[h]
            val cost = careful(lex, id)
            val prior = prior(lex, id, ids[heapLex[h]])
            out.add(GlideCandidate(lex.text(id), -cost + p.lmW * prior, cost, prior))
        }
        out.sortByDescending { it.score }
        val seen = HashSet<String>()
        val res = ArrayList<GlideCandidate>(max)
        for (c in out) { if (seen.add(c.text)) res.add(c); if (res.size >= max) break }
        return res
    }

    /** The words that went through to the careful pass in the last decode (for the accuracy tests). */
    internal fun survivorTexts(lexicons: List<GlideLexicon>): List<String> = (0 until heapN).map { lexicons[heapLex[it]].text(heapId[it]) }

    private fun d2(dx: Float, dy: Float) = dx * dx + dy * dy

    /** Cut the path into [n] equal steps in pitches; false when it is too short to be a glide. */
    private fun resample(xs: FloatArray, ys: FloatArray, count: Int): Boolean {
        val inv = 1f / keys.pitch
        // drop repeated points
        var m = 0
        val px = FloatArray(count); val py = FloatArray(count)
        for (i in 0 until count) {
            val x = xs[i] * inv; val y = ys[i] * inv
            if (m == 0 || d2(x - px[m - 1], y - py[m - 1]) > 1e-6f) { px[m] = x; py[m] = y; m++ }
        }
        if (m < 2) return false
        var len = 0f
        for (i in 1 until m) len += Math.sqrt(d2(px[i] - px[i - 1], py[i] - py[i - 1]).toDouble()).toFloat()
        if (len < p.minLength) return false
        var seg = 1; var segStart = 0f
        var segLen = Math.sqrt(d2(px[1] - px[0], py[1] - py[0]).toDouble()).toFloat()
        for (k in 0 until n) {
            val target = len * k / (n - 1)
            while (seg < m - 1 && target > segStart + segLen) {
                segStart += segLen; seg++
                segLen = Math.sqrt(d2(px[seg] - px[seg - 1], py[seg] - py[seg - 1]).toDouble()).toFloat()
            }
            val t = if (segLen > 0f) ((target - segStart) / segLen).coerceIn(0f, 1f) else 0f
            qx[k] = px[seg - 1] + (px[seg] - px[seg - 1]) * t
            qy[k] = py[seg - 1] + (py[seg] - py[seg - 1]) * t
        }
        return true
    }

    // ------------------------------------------------------------------ quick pass

    private fun quick(lex: GlideLexicon, li: Int, id: Int, ends: Float) {
        val len = lex.length(id)
        if (len < 2 || len > MAX_LETTERS) return
        var cov = 0f; var m = 0; var prevL = -1
        for (i in 0 until len) {
            val l = lex.letter(id, i)
            if (l == prevL) continue
            prevL = l
            val d = dKey[l]
            if (d > p.missLimit2) return
            cov += if (d < p.visitCap) d else p.visitCap
            m++
        }
        if (m < 2) return
        val score = cov / m + p.quickEnds * ends * 0.5f - p.quickPrior * lex.logProb(id) // lower is better
        if (heapN < p.keep) { heapPush(score, li, id) }
        else if (score < heapScore[0]) { heapReplaceTop(score, li, id) }
    }

    // a max-heap on the quick score (the worst survivor sits on top and is replaced by a better word)
    private fun heapPush(s: Float, li: Int, id: Int) {
        var i = heapN++
        heapScore[i] = s; heapLex[i] = li; heapId[i] = id
        while (i > 0) { val par = (i - 1) / 2; if (heapScore[par] >= heapScore[i]) break; swap(par, i); i = par }
    }
    private fun heapReplaceTop(s: Float, li: Int, id: Int) {
        heapScore[0] = s; heapLex[0] = li; heapId[0] = id
        var i = 0
        while (true) {
            val l = 2 * i + 1; val r = l + 1; var big = i
            if (l < heapN && heapScore[l] > heapScore[big]) big = l
            if (r < heapN && heapScore[r] > heapScore[big]) big = r
            if (big == i) break
            swap(big, i); i = big
        }
    }
    private fun swap(a: Int, b: Int) {
        val s = heapScore[a]; heapScore[a] = heapScore[b]; heapScore[b] = s
        val l = heapLex[a]; heapLex[a] = heapLex[b]; heapLex[b] = l
        val d = heapId[a]; heapId[a] = heapId[b]; heapId[b] = d
    }

    // ------------------------------------------------------------------ careful pass

    /** The cost of the path as the word [id]: where the finger was at each letter and how far the path strays between letters. Lower is better. */
    private fun careful(lex: GlideLexicon, id: Int): Float {
        val len = lex.length(id)
        var m = 0; var prevL = -1
        for (i in 0 until len) { val l = lex.letter(id, i); if (l != prevL) { letters[m++] = l; prevL = l } }
        // squared distance of every sample to each straight segment between two letters, as prefix sums
        for (j in 0 until m - 1) {
            val ax = kx[letters[j]]; val ay = ky[letters[j]]; val bx = kx[letters[j + 1]]; val by = ky[letters[j + 1]]
            val vx = bx - ax; val vy = by - ay; val vv = vx * vx + vy * vy
            val base = j * (n + 1)
            pre[base] = 0f
            for (i in 0 until n) {
                var t = if (vv > 0f) ((qx[i] - ax) * vx + (qy[i] - ay) * vy) / vv else 0f
                if (t < 0f) t = 0f else if (t > 1f) t = 1f
                val dx = qx[i] - (ax + vx * t); val dy = qy[i] - (ay + vy * t)
                pre[base + i + 1] = pre[base + i] + dx * dx + dy * dy
            }
        }
        val inf = Float.MAX_VALUE / 4
        // the weights of the letters add up to this: the two end letters count [endW], the others 1 (the final cost is the average over them)
        val wTotal = (m - 2) + 2f * p.endW
        // first letter near the start of the path
        val k0 = letters[0]
        for (i in 0 until n) fPrev[i] = if (i <= p.slack) p.endW * visit(k0, i) else inf
        for (j in 1 until m) {
            val kj = letters[j]
            val last = j == m - 1
            val w = if (last) p.endW else 1f
            val base = (j - 1) * (n + 1)
            for (i in 0 until n) {
                if (i < j || (last && i < n - 1 - p.slack)) { fCur[i] = inf; continue }
                var best = inf
                for (a in j - 1 until i) {
                    val fa = fPrev[a]
                    if (fa >= inf) continue
                    val c = fa + p.shapeW * (pre[base + i] - pre[base + a]) * (wTotal / n) // how far the path strays between the two letters, as an average over all steps of the path
                    if (c < best) best = c
                }
                fCur[i] = if (best >= inf) inf else best + w * visit(kj, i)
            }
            val t = fPrev; fPrev = fCur; fCur = t
        }
        var best = inf
        for (i in 0 until n) if (fPrev[i] < best) best = fPrev[i]
        return best / wTotal
    }

    private fun visit(k: Int, i: Int): Float { val d = d2(qx[i] - kx[k], qy[i] - ky[k]); return if (d < p.visitCap) d else p.visitCap }

    // ------------------------------------------------------------------ the language

    private fun prevIds(lexicons: List<GlideLexicon>, prev: String?): IntArray {
        val ids = IntArray(lexicons.size) { -1 }
        if (prev.isNullOrEmpty()) return ids
        for (i in lexicons.indices) ids[i] = lexicons[i].idOf(prev)
        return ids
    }

    /** ln P(word | the word before): the word's own share, mixed with the pair's share when the pair is known. */
    private fun prior(lex: GlideLexicon, id: Int, prevId: Int): Float {
        val u = lex.logProb(id)
        if (prevId < 0) return u
        val b = lex.bigramLogProb(prevId, id)
        if (b.isNaN()) return u + Math.log((1.0 - p.bigramMix)).toFloat()
        return Math.log((1.0 - p.bigramMix) * Math.exp(u.toDouble()) + p.bigramMix * Math.exp(b.toDouble())).toFloat()
    }

    companion object { const val MAX_LETTERS = 24 }
}
