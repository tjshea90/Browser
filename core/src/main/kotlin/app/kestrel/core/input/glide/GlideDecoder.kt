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
    val keep: Int = 200,
    val quickEnds: Float = 0.7f,
    val quickPrior: Float = 0.12f,
    /** The careful pass: weight of the path between two letters straying from the straight line, and of the first / last letter. */
    val shapeW: Float = 2.0f,
    val endW: Float = 3.0f,
    /** The language: nats of word probability that are worth one unit of path cost. */
    val lmW: Float = 0.50f,
    /** The language weight is not fixed: a clean glide (every candidate fits well) trusts the path, a sloppy one trusts the language. It is [lmA] times the best fit's cost per letter, kept between [lmMin] and [lmMax]; [lmA] 0 = always [lmW]. */
    val lmA: Float = 1.5f,
    val lmMin: Float = 0.12f,
    val lmMax: Float = 1.2f,
    /** How much of the probability comes from the word before (when the pair is known). */
    val bigramMix: Float = 0.4f,
    /** A path shorter than this many pitches is a tap, not a glide. */
    val minLength: Float = 1.2f,
    /** First / last letter may be matched this many samples away from the very end of the path. */
    val slack: Int = 1,
    /** A thumb lands a little beside where it aims, the same way along the whole path: the best [biasKeep] words are judged a second time with the path moved by [biasGain] of its mean offset from the word's letters. */
    val biasKeep: Int = 40,
    val biasGain: Float = 0.8f,
    /** Cost of each pitch squared of that move, so a word cannot buy a good fit by sliding the whole path. */
    val biasW: Float = 3.0f,
    /** A word whose path is further than this from the finger's (squared pitches per letter) is no answer: a glide that fits nothing says so instead of typing the nearest word. */
    val maxPerLetter: Float = 3.4f,
    /** A correction the person made before (the keyboard guessed A, they meant B) lifts B by this many nats times ln(1 + how often) when A is the first guess again (AV2). 0 = no learning from corrections. */
    val confGain: Float = 0.9f,
    /** The lift from a correction never exceeds this many nats, however often it was made (a habit helps decide a near-tie, it never overrules the finger). */
    val confCap: Float = 1.6f,
    /** ... and only for a word whose path fit is close to the first guess's: its cost per letter may be at most this much higher (pitch squared). A correction settles lookalike words, not a clear miss. */
    val confMaxGap: Float = 0.9f,
    /**
     * Glide shapes the person drew before (AX1): a word whose remembered shapes look like this glide is lifted by [patGain] times ln(1 + how much it looks like them, counted in confirmations), never
     * more than [patCap]. Only for words whose own path fit is within [patMaxGap] (pitch squared per letter) of the best fit's: a remembered shape settles a sloppy glide, it does not make a word
     * the finger never went near. [patGain] 0 = no pattern learning.
     */
    val patGain: Float = 2.5f,
    val patCap: Float = 4.5f,
    val patMaxGap: Float = 2.2f,
)

class GlideCandidate(val text: String, val score: Float, val cost: Float, val prior: Float, val letters: Int = 1) {
    /** How far the path was from the word, per letter (pitch squared): the smaller the better. */
    val perLetter: Float get() = cost / letters
}

/**
 * Turns a finger path over the letter keys into words (AT1). The path is cut into equal steps; every word whose first and last letter are near where the finger went down and lifted is
 * judged in passes: a quick one that asks only "was each letter of the word visited" (one table of 26 distances answers that for every word), then a careful one that finds, letter by letter, the
 * place along the path where the finger was at that letter, and how far the path between two letters strays from the straight line a word would draw there; the best words get that look a second time with
 * the whole path moved by the way the finger was off. The language weighs in the same way a glide keyboard's does: how common the word is, and how likely it is after the word before. No allocation
 * per word, no Android types.
 */
class GlideDecoder(private val keys: GlideKeys, private val p: GlideParams = GlideParams()) {
    private val n = p.samples
    private val kx = FloatArray(26) { keys.cx[it] / keys.pitch }
    private val ky = FloatArray(26) { keys.cy[it] / keys.pitch }
    private val qx = FloatArray(n); private val qy = FloatArray(n)     // the path as it was drawn (moved by the learned habit)
    private val wx = FloatArray(n); private val wy = FloatArray(n)     // the path as judged (moved a bit more for the second look)
    private val dKey = FloatArray(26)
    // scratch for the careful pass: prefix sums of squared distance to each segment, two DP rows, the choices
    private val pre = FloatArray(MAX_LETTERS * (n + 1))
    private var fPrev = FloatArray(n); private var fCur = FloatArray(n)
    private val from = IntArray(MAX_LETTERS * n)
    private val assign = IntArray(MAX_LETTERS)
    private val letters = IntArray(MAX_LETTERS)
    private var gestureLen = 0f
    // the survivors of the quick pass (a small max-heap by quick score, kept as parallel arrays)
    private val heapScore = FloatArray(p.keep); private val heapLex = IntArray(p.keep); private val heapId = IntArray(p.keep); private var heapN = 0

    /** What the person's own words and pairs add to the language (AU2); null = nothing learned yet. Set by whoever owns the model; read on the decode thread. */
    @Volatile var personal: PersonalModel? = null

    /** The offset the learned finger habit adds to every path (pitches): what the finger usually lands beside the key it aims at. Set by [learnFrom]. */
    var biasX = 0f; var biasY = 0f

    /** How many words the last decode looked at closely, and the language weight it used (for the diagnostics and the tests). */
    var lastCandidates = 0; private set
    var lastLm = 0f; private set

    /** The shape of the last glide as [Fingerprint] numbers (null for a tap): what is remembered when the person confirms or corrects the word. */
    var lastPrint: FloatArray? = null; private set
    /** The last decode: a remembered shape lifted at least one word / changed which word came first (for the Diagnostics line and the tests). */
    var lastPatternLifted = false; private set
    var lastPatternFlipped = false; private set

    /**
     * [xs]/[ys] are the first [count] touch points in view pixels. [prev] is the word before (lowercase letters) or null. Returns up to [max] words, best first; empty when the path is a tap or
     * nothing fits.
     */
    fun decode(xs: FloatArray, ys: FloatArray, count: Int, lexicons: List<GlideLexicon>, prev: String?, max: Int = 3, prev2: String? = null): List<GlideCandidate> {
        lastCandidates = 0; lastPatternLifted = false; lastPatternFlipped = false
        lastPrint = if (p.patGain > 0f) Fingerprint.of(xs, ys, count, keys) else null
        if (count < 2 || !resample(xs, ys, count)) return emptyList()
        // the path moved by the learned habit; everything after judges this one
        for (i in 0 until n) { qx[i] -= biasX; qy[i] -= biasY }
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

        // ---- careful pass
        val ids = prevIds(lexicons, prev)
        val costs = FloatArray(heapN); val priors = FloatArray(heapN)
        var bestPer = Float.MAX_VALUE
        for (h in 0 until heapN) {
            val lex = lexicons[heapLex[h]]; val id = heapId[h]
            costs[h] = careful(lex, id)
            priors[h] = prior(lex, id, ids[heapLex[h]], prev, prev2)
            val per = costs[h] / distinctLetters(lex, id)
            if (per < bestPer) bestPer = per
        }
        var lm = if (p.lmA > 0f) (p.lmA * bestPer).coerceIn(p.lmMin, p.lmMax) else p.lmW
        // ---- the best ones again, with the path moved by the way the finger was off
        if (p.biasKeep > 0 && p.biasGain > 0f) {
            val order = (0 until heapN).sortedByDescending { -costs[it] + lm * priors[it] }.take(p.biasKeep)
            var best2 = Float.MAX_VALUE
            for (h in order) {
                val lex = lexicons[heapLex[h]]; val id = heapId[h]
                costs[h] = refine(lex, id, costs[h])
                val per = costs[h] / distinctLetters(lex, id)
                if (per < best2) best2 = per
            }
            if (p.lmA > 0f && best2 < bestPer) lm = (p.lmA * best2).coerceIn(p.lmMin, p.lmMax)
        }
        lastLm = lm
        val out = ArrayList<GlideCandidate>(heapN)
        for (h in 0 until heapN) {
            val lex = lexicons[heapLex[h]]; val id = heapId[h]
            val letters = distinctLetters(lex, id)
            if (costs[h] / letters > p.maxPerLetter) continue
            out.add(GlideCandidate(lex.text(id), -costs[h] + lm * priors[h], costs[h], priors[h], letters))
        }
        out.sortByDescending { it.score }
        applyPatterns(out)
        applyCorrections(out)
        val seen = HashSet<String>()
        val res = ArrayList<GlideCandidate>(max)
        for (c in out) { if (seen.add(c.text)) res.add(c); if (res.size >= max) break }
        return res
    }

    /**
     * How far the finger was off the letters of word [id] on the path decoded last, on average, in pitches (x, y): what the finger tends to land beside. The words the person accepted go to
     * [learnFrom] to make the decoder expect it. Null when the word cannot be laid on the path.
     */
    fun offsetFor(lex: GlideLexicon, id: Int): FloatArray? {
        if (!loadLetters(lex, id)) return null
        setWork(0f, 0f)
        if (dp(mCount) >= Float.MAX_VALUE / 8) return null
        return meanOffset(mCount)
    }

    /** The habit learns from a glide whose word was accepted: the decoder expects the finger to land [rate] of the way more towards where that glide was off. */
    fun learnFrom(lex: GlideLexicon, id: Int, rate: Float = 0.12f) {
        val o = offsetFor(lex, id) ?: return
        // the offset measured now is what is LEFT after the habit so far was taken off, so it is added to the habit
        biasX = (biasX + rate * o[0]).coerceIn(-MAX_BIAS, MAX_BIAS); biasY = (biasY + rate * o[1]).coerceIn(-MAX_BIAS, MAX_BIAS)
    }

    // ------------------------------------------------------------------ for the tests

    /** The words that went through to the careful pass in the last decode (for the accuracy tests). */
    internal fun survivorTexts(lexicons: List<GlideLexicon>): List<String> = (0 until heapN).map { lexicons[heapLex[it]].text(heapId[it]) }

    /** The careful-pass cost of word [id] for the path decoded last (both looks). */
    internal fun debugCost(lex: GlideLexicon, id: Int): Float = refine(lex, id, careful(lex, id))

    /** Where the careful pass put every letter of word [id] on the path, and what it cost. */
    internal fun debugTrace(lex: GlideLexicon, id: Int): String {
        if (!loadLetters(lex, id)) return "?"
        setWork(0f, 0f)
        val c = dp(mCount)
        val sb = StringBuilder("cost %.3f len=%.1f pitches  ".format(c, gestureLen))
        for (j in 0 until mCount) sb.append("%c@%d(v=%.2f) ".format('a' + letters[j], assign[j], visit(letters[j], assign[j])))
        return sb.toString()
    }

    private fun d2(dx: Float, dy: Float) = dx * dx + dy * dy

    private fun distinctLetters(lex: GlideLexicon, id: Int): Int { var m = 0; var p0 = -1; for (i in 0 until lex.length(id)) { val l = lex.letter(id, i); if (l != p0) { m++; p0 = l } }; return maxOf(m, 1) }

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
        gestureLen = len
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

    private var mCount = 0

    /** Puts the letters of word [id] (a letter that repeats counts once) into [letters]; false when there are fewer than two. */
    private fun loadLetters(lex: GlideLexicon, id: Int): Boolean {
        val len = lex.length(id)
        if (len > MAX_LETTERS) return false
        var m = 0; var prevL = -1
        for (i in 0 until len) { val l = lex.letter(id, i); if (l != prevL) { letters[m++] = l; prevL = l } }
        mCount = m
        return m >= 2
    }

    private fun setWork(dx: Float, dy: Float) { for (i in 0 until n) { wx[i] = qx[i] - dx; wy[i] = qy[i] - dy } }

    /** The cost of the path as the word [id] (first look: the path as drawn). */
    private fun careful(lex: GlideLexicon, id: Int): Float {
        if (!loadLetters(lex, id)) return Float.MAX_VALUE / 8
        setWork(0f, 0f)
        return dp(mCount)
    }

    /** The second look: move the whole path by [GlideParams.biasGain] of how far it sat off the word's letters, and judge again (the better of the two, with the move charged). */
    private fun refine(lex: GlideLexicon, id: Int, cost1: Float): Float {
        if (!loadLetters(lex, id)) return cost1
        setWork(0f, 0f)
        if (dp(mCount) >= Float.MAX_VALUE / 8) return cost1
        val o = meanOffset(mCount)
        val mx = o[0] * p.biasGain; val my = o[1] * p.biasGain
        setWork(mx, my)
        val c2 = dp(mCount) + p.biasW * (mx * mx + my * my) * mCount
        return if (c2 < cost1) c2 else cost1
    }

    /** Mean (x, y) of the judged path's points minus the letters they were assigned to, in the last [dp]. */
    private fun meanOffset(m: Int): FloatArray {
        var sx = 0f; var sy = 0f
        for (j in 0 until m) { val i = assign[j]; sx += wx[i] - kx[letters[j]]; sy += wy[i] - ky[letters[j]] }
        return floatArrayOf(sx / m, sy / m)
    }

    private fun visit(k: Int, i: Int): Float { val d = d2(wx[i] - kx[k], wy[i] - ky[k]); return if (d < p.visitCap) d else p.visitCap }

    /**
     * The best way to put the [m] letters on the path, in order: letter j at step i_j, the first at the start and the last at the end, minimising how far each step is from its letter plus how far
     * the path strays from the straight line between two neighbouring letters. Fills [assign]; returns the cost.
     */
    private fun dp(m: Int): Float {
        val shapeScale = gestureLen / n // every step stands for this many pitches of path
        // squared distance of every step to each straight segment between two letters, as prefix sums
        for (j in 0 until m - 1) {
            val ax = kx[letters[j]]; val ay = ky[letters[j]]; val bx = kx[letters[j + 1]]; val by = ky[letters[j + 1]]
            val vx = bx - ax; val vy = by - ay; val vv = vx * vx + vy * vy
            val base = j * (n + 1)
            pre[base] = 0f
            for (i in 0 until n) {
                var t = if (vv > 0f) ((wx[i] - ax) * vx + (wy[i] - ay) * vy) / vv else 0f
                if (t < 0f) t = 0f else if (t > 1f) t = 1f
                val dx = wx[i] - (ax + vx * t); val dy = wy[i] - (ay + vy * t)
                pre[base + i + 1] = pre[base + i] + dx * dx + dy * dy
            }
        }
        val inf = Float.MAX_VALUE / 4
        val k0 = letters[0]
        for (i in 0 until n) fPrev[i] = if (i <= p.slack) p.endW * visit(k0, i) else inf
        for (j in 1 until m) {
            val kj = letters[j]
            val last = j == m - 1
            val w = if (last) p.endW else 1f
            val base = (j - 1) * (n + 1)
            for (i in 0 until n) {
                if (i < j || (last && i < n - 1 - p.slack)) { fCur[i] = inf; from[j * n + i] = -1; continue }
                var best = inf; var arg = -1
                for (a in j - 1 until i) {
                    val fa = fPrev[a]
                    if (fa >= inf) continue
                    val c = fa + p.shapeW * (pre[base + i] - pre[base + a]) * shapeScale
                    if (c < best) { best = c; arg = a }
                }
                from[j * n + i] = arg
                fCur[i] = if (best >= inf) inf else best + w * visit(kj, i)
            }
            val t = fPrev; fPrev = fCur; fCur = t
        }
        var best = inf; var bi = -1
        for (i in 0 until n) if (fPrev[i] < best) { best = fPrev[i]; bi = i }
        if (bi < 0) return inf
        var cur = bi
        for (j in m - 1 downTo 0) { assign[j] = cur; if (j > 0) cur = from[j * n + cur] }
        return best
    }

    // ------------------------------------------------------------------ the language

    private fun prevIds(lexicons: List<GlideLexicon>, prev: String?): IntArray {
        val ids = IntArray(lexicons.size) { -1 }
        if (prev.isNullOrEmpty()) return ids
        for (i in lexicons.indices) ids[i] = lexicons[i].idOf(prev)
        return ids
    }

    /**
     * ln P(word | the word before): the word's own share (plus what the person's own use adds), mixed with the pair's share when the pair is known (the shipped list's, and the person's own pairs
     * weighted by how often the person wrote something after that word).
     */
    private fun prior(lex: GlideLexicon, id: Int, prevId: Int, prevText: String?, prev2Text: String? = null): Float {
        val pm = personal?.takeIf { !it.isEmpty }
        var pu = Math.exp(lex.logProb(id).toDouble())
        var text: String? = null
        if (pm != null) { text = lex.text(id); pu += pm.unigramMass(text, lex.totalCount()) + pm.recentMass(text, lex.totalCount()) }
        var pb = 0.0
        var known = false
        if (prevId >= 0) { val b = lex.bigramLogProb(prevId, id); known = true; if (!b.isNaN()) pb = Math.exp(b.toDouble()) }
        if (pm != null && prevText != null) {
            val lam = pm.pairWeight(prevText)
            if (lam > 0f) { val w = text ?: lex.text(id); pb = (1.0 - lam) * pb + lam * pm.pairProb(prevText, w); known = true }
            if (prev2Text != null) { // the last TWO words: what the person wrote after this pair before
                val lt = pm.tripleWeight(prev2Text, prevText)
                if (lt > 0f) { val w = text ?: lex.text(id); pb = (1.0 - lt) * pb + lt * pm.tripleProb(prev2Text, prevText, w); known = true }
            }
        }
        if (!known) return Math.log(pu).toFloat()
        return Math.log((1.0 - p.bigramMix) * pu + p.bigramMix * pb).toFloat()
    }

    /**
     * What this person's glides looked like before (AX1): every candidate whose remembered shapes look like this glide gets a lift that grows with how often they were confirmed and how close this
     * one is; a word nobody ever drew like this gets nothing, so it can only lose to the words the person actually means by this shape.
     */
    private fun applyPatterns(out: ArrayList<GlideCandidate>) {
        val pm = personal?.patterns ?: return
        val print = lastPrint ?: return
        if (p.patGain <= 0f || out.isEmpty() || pm.isEmpty) return
        var bestPer = Float.MAX_VALUE
        for (c in out) if (c.perLetter < bestPer) bestPer = c.perLetter
        val firstBefore = out[0].text
        var changed = false
        for (i in out.indices) {
            val c = out[i]
            if (c.perLetter - bestPer > p.patMaxGap) continue
            val e = pm.evidence(c.text, print)
            if (e < 0.05f) continue
            val lift = Math.min(p.patCap, p.patGain * Math.log(1.0 + e).toFloat())
            out[i] = GlideCandidate(c.text, c.score + lift, c.cost, c.prior, c.letters); changed = true
        }
        if (changed) { out.sortByDescending { it.score }; lastPatternLifted = true; lastPatternFlipped = out[0].text != firstBefore }
    }

    /**
     * What the person corrected before (AV2): when the best word is one the keyboard got wrong for them earlier and the word they then meant is among the other candidates, that word gets a lift
     * of [GlideParams.confGain] times ln(1 + how often). Only the pair "guessed the first, meant the second" counts, so one correction never makes a common word worse everywhere.
     */
    private fun applyCorrections(out: ArrayList<GlideCandidate>) {
        val pm = personal ?: return
        if (p.confGain <= 0f || out.size < 2 || pm.confusionCount == 0) return
        val first = out[0]
        var changed = false
        for (i in 1 until out.size) {
            val c = out[i]
            if (c.perLetter - first.perLetter > p.confMaxGap) continue // the path fits this word clearly worse: a habit does not overrule the finger
            val n = pm.confusionWeight(first.text, c.text)
            if (n > 0f) {
                val lift = Math.min(p.confCap, p.confGain * Math.log(1.0 + n).toFloat())
                out[i] = GlideCandidate(c.text, c.score + lift, c.cost, c.prior, c.letters); changed = true
            }
        }
        if (changed) out.sortByDescending { it.score }
    }

    companion object {
        const val MAX_LETTERS = 24
        /** The learned habit never moves a path by more than this many pitches. */
        const val MAX_BIAS = 0.6f
    }
}
