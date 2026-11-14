package app.kestrel.core.input.glide

import java.util.concurrent.Executor

/** What glide typing did, counted on the tablet itself (no words are kept) so a Diagnostics paste says how accurate it was in real use (AT1g). */
class GlideStats {
    private var glides = 0; private var noMatch = 0; private var kept = 0; private var picked = 0; private var undone = 0
    private var ms = 0L; private var msMax = 0L
    private var pending = false
    private var loadMs = -1L; private var loadWords = 0; private var warmMs = -1L
    private var habitX = 0f; private var habitY = 0f
    private var own = 0
    private var learnedWords = 0; private var learnedPairs = 0; private var learnedTop = 0; private var learnedCorrections = 0; private var learnedTriples = 0
    private var previews = 0; private var previewMs = 0L; private var shown = 0; private var shownMs = 0L; private var shownMax = 0L

    @Synchronized fun onDecoded(millis: Long, found: Boolean) { glides++; if (!found) noMatch++; ms += millis; if (millis > msMax) msMax = millis; if (found) pending = true }
    /** Whatever the person did next that was not a correction: the word stays. */
    @Synchronized fun onKept() { if (pending) { kept++; pending = false } }
    @Synchronized fun onPicked() { if (pending) { picked++; pending = false } }
    @Synchronized fun onUndone() { if (pending) { undone++; pending = false } }
    @Synchronized fun onLoaded(millis: Long, words: Int) { loadMs = millis; loadWords = words }
    @Synchronized fun onWarm(millis: Long) { warmMs = millis }
    @Synchronized fun onHabit(xDp: Float, yDp: Float) { habitX = xDp; habitY = yDp }
    @Synchronized fun onOwnWords(n: Int) { own = n }
    @Synchronized fun onLearned(words: Int, pairs: Int, corrections: Int = 0, triples: Int = 0) { learnedWords = words; learnedPairs = pairs; learnedCorrections = corrections; learnedTriples = triples }
    /** A live preview was decoded while the finger was still moving. */
    @Synchronized fun onPreview(millis: Long) { previews++; previewMs += millis }
    /** The word appeared [millis] after the finger lifted (the decode plus the wait for its turn plus drawing). */
    @Synchronized fun onShown(millis: Long) { shown++; shownMs += millis; if (millis > shownMax) shownMax = millis }
    /** A word the person had used before was the keyboard's first guess for this glide. */
    @Synchronized fun onLearnedTop() { learnedTop++ }

    @Synchronized fun line(): String {
        if (loadMs < 0 && glides == 0) return "Glide typing: not used yet this run"
        val sb = StringBuilder("Glide typing: ")
        if (glides == 0) sb.append("no glide yet")
        else {
            val judged = kept + picked + undone
            sb.append("$glides glides")
            if (judged > 0) sb.append(": best word kept $kept (${100 * kept / judged} %), another candidate picked $picked, deleted right away $undone")
            if (noMatch > 0) sb.append(", no word fitted $noMatch")
            sb.append("; decode avg ${ms / glides} ms, worst $msMax ms")
        }
        if (shown > 0) sb.append("; word shown ${shownMs / shown} ms after the lift (worst $shownMax ms)")
        if (previews > 0) sb.append("; live preview $previews times, avg ${previewMs / previews} ms")
        if (loadMs >= 0) sb.append("; word list $loadWords words ready after $loadMs ms")
        if (warmMs >= 0) sb.append(", first decode (warm-up) $warmMs ms")
        sb.append("; thumb habit x %.1f y %.1f dp".format(habitX, habitY))
        if (own > 0) sb.append("; own words $own")
        if (learnedWords > 0 || learnedTop > 0) sb.append("; learned $learnedWords words and $learnedPairs word pairs from your typing, a learned word was the first guess $learnedTop times")
        if (learnedCorrections > 0 || learnedTriples > 0) sb.append("; $learnedCorrections corrections and $learnedTriples three-word phrases remembered")
        return sb.toString()
    }
}

/** The words the person typed or whose sites they visit: they join the shipped list, weighed on its scale (a word seen once ranks like an ordinary one, a word used often like a common one). */
class UserWords(private val cap: Int = 800) {
    private val counts = LinkedHashMap<String, Int>()
    val size: Int get() = counts.size

    @Synchronized fun add(word: String, times: Int = 1) {
        val w = word.lowercase()
        if (!glideable(w) || times <= 0) return
        counts[w] = (counts[w] ?: 0) + times
        while (counts.size > cap) { // drop the least used (the oldest of those)
            var victim: String? = null; var low = Int.MAX_VALUE
            for ((k, v) in counts) if (v < low) { low = v; victim = k }
            counts.remove(victim)
        }
    }

    @Synchronized fun contains(word: String) = counts.containsKey(word.lowercase())

    /** The list to glide into: [total] is the shipped list's total count, so the probabilities are on the same scale. */
    @Synchronized fun lexicon(total: Double): ArrayLexicon = ArrayLexicon(counts.map { (w, n) -> w to (30_000_000L + 25_000_000L * minOf(n, 12)) }, total)

    @Synchronized fun serialize(): String = counts.entries.joinToString("\n") { "${it.key} ${it.value}" }

    companion object {
        /** A word a finger can draw: lowercase letters, three to 24, with at least two different letters in a row, none of them three times in a row ("awwww" is noise). */
        fun glideable(w: String): Boolean {
            if (w.length !in 3..24 || !w.all { it in 'a'..'z' } || PersonalModel.stretched(w)) return false
            for (i in 1 until w.length) if (w[i] != w[i - 1]) return true
            return false
        }

        fun parse(text: String, cap: Int = 800): UserWords {
            val u = UserWords(cap)
            for (line in text.lineSequence()) { val p = line.trim().split(' '); if (p.size == 2) u.add(p[0], p[1].toIntOrNull() ?: 1) }
            return u
        }

        /** The plain words of a page title that a finger can draw: "The Godfather (1972) - Watch online" -> godfather, watch, online (lowercase letters, four to 20 long: shorter words are in the list anyway). */
        fun wordsOfTitle(title: String): List<String> = title.lowercase().split(Regex("[^a-z]+")).filter { it.length in 4..20 && glideable(it) }

        /** The word a site name stands for: "www.doraby.com" -> "doraby", "news.bbc.co.uk" -> "bbc", "pelisjuanita.com" -> "pelisjuanita". Null when there is no plain word in it. */
        fun wordOfHost(host: String): String? {
            val labels = host.lowercase().removePrefix("www.").split('.').filter { it.isNotEmpty() }
            if (labels.size < 2) return null
            val tlds = setOf("com", "net", "org", "co", "uk", "gov", "edu", "io", "tv", "me", "info", "biz", "app", "dev", "xyz", "online", "site", "us", "ca", "au", "de", "fr", "es", "it", "nl", "ru", "br", "mx", "ar", "cl", "in", "jp", "cn")
            var i = labels.size - 1
            while (i > 0 && labels[i] in tlds) i--
            val w = labels[i]
            return if (glideable(w)) w else null
        }
    }
}

/** The result of one glide as the keyboard gets it. */
class GlideResult(val candidates: List<GlideCandidate>, val millis: Long)

/**
 * The glide engine the keyboard talks to (AT1): it loads the word list, owns the decoder and the person's own words, and does every bit of that on [executor] (ONE thread, so nothing needs a lock
 * and the UI thread is never held): [prepare] first, then [decode] for each glide, in order. Results come back on the executor's thread; the caller moves them to the UI thread.
 */
class GlideTyping(
    private val loadMain: () -> ByteArray?,
    private val executor: Executor,
    val stats: GlideStats = GlideStats(),
    /** Reads what was learned (the text [PersonalModel.serialize] wrote), on the executor's thread; null = nothing saved. */
    private val loadPersonal: () -> String? = { null },
    /** Stores what was learned (on the executor's thread). */
    private val savePersonal: (String) -> Unit = {},
    /** Where the live previews are decoded (the finger is still moving): another thread than [executor], so a preview never holds up the real decode when the finger lifts. */
    private val previewExecutor: Executor = executor,
) {
    @Volatile private var main: BinaryLexicon? = null
    @Volatile private var userLex: ArrayLexicon? = null
    @Volatile private var decoder: GlideDecoder? = null
    @Volatile private var previewDecoder: GlideDecoder? = null // its own scratch space: the real decoder's state (the path of the last glide) is never touched by a preview
    private val previewBusy = java.util.concurrent.atomic.AtomicBoolean(false)
    private var keys: GlideKeys? = null
    private var bx = 0f; private var by = 0f
    private var loading = false
    private var lastUserWords = 0
    private var personal = PersonalModel()
    private var personalLoaded = false
    private var personalDirty = false
    private var lastSaveAt = 0L

    /** The best-known state of the thumb habit in pitches (read on the executor thread; copied out for saving). */
    @Volatile var habitX = 0f; private set
    @Volatile var habitY = 0f; private set

    /** True once a glide can be decoded. */
    @Volatile var ready = false; private set

    /** Load the list (once) and set the decoder up for [k] (again when the keyboard's size changed). [habit] is the saved thumb habit in pitches. */
    fun prepare(k: GlideKeys, habit: FloatArray?, own: UserWords?) {
        executor.execute {
            try {
                if (decoder == null && habit != null && habit.size == 2) { bx = habit[0]; by = habit[1] } // the saved habit starts the engine; later it keeps what it learned itself
                if (main == null && !loading) {
                    loading = true
                    val t0 = System.nanoTime()
                    val bytes = loadMain()
                    if (bytes != null) { main = LexiconFile.read(bytes); stats.onLoaded((System.nanoTime() - t0) / 1_000_000, main!!.size) }
                    loading = false
                }
                if (!personalLoaded) { personalLoaded = true; try { loadPersonal()?.let { personal = PersonalModel.parse(it) } } catch (e: Exception) { /* a damaged file is just forgotten */ }; stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount) }
                keys = k
                decoder = GlideDecoder(k).also { it.biasX = bx; it.biasY = by; it.personal = personal }
                previewDecoder = GlideDecoder(k).also { it.biasX = bx; it.biasY = by; it.personal = personal }
                if (own != null) setOwn(own)
                ready = main != null
                if (!warmed && ready) { warmed = true; warmUp(k) }
            } catch (e: Exception) { loading = false; ready = false }
        }
    }

    private var warmed = false

    /**
     * One glide of "the" is decoded and thrown away while the keyboard opens: the runtime compiles the decoder's hot loops on the first run (and runs them slowly until then), so the person's first
     * real glide does not pay for that. How long it took goes into the Diagnostics line.
     */
    private fun warmUp(k: GlideKeys) {
        val d = decoder ?: return
        val m = main ?: return
        val t0 = System.nanoTime()
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        val seq = "the"
        for (i in 0 until seq.length - 1) {
            val a = seq[i] - 'a'; val b = seq[i + 1] - 'a'
            for (s in 0 until 12) { val t = s / 12f; xs.add(k.cx[a] + (k.cx[b] - k.cx[a]) * t); ys.add(k.cy[a] + (k.cy[b] - k.cy[a]) * t) }
        }
        xs.add(k.cx[seq.last() - 'a']); ys.add(k.cy[seq.last() - 'a'])
        try { repeat(2) { d.decode(xs.toFloatArray(), ys.toFloatArray(), xs.size, listOf(m), null, 3) } } catch (e: Exception) { /* a warm-up must never matter */ }
        d.biasX = bx; d.biasY = by
        stats.onWarm((System.nanoTime() - t0) / 2 / 1_000_000)
    }

    /** The person's own words changed. */
    fun ownWords(own: UserWords) { executor.execute { setOwn(own) } }

    private fun setOwn(own: UserWords) {
        val m = main
        userLex = if (own.size == 0) null else own.lexicon(m?.totalCount ?: 5.4e11)
        lastUserWords = own.size
        stats.onOwnWords(own.size)
    }

    /** Decode the path of [count] touch points; [done] is called on the executor's thread. */
    fun decode(xs: FloatArray, ys: FloatArray, count: Int, prev: String?, prev2: String? = null, done: (GlideResult) -> Unit) {
        val px = xs.copyOf(count); val py = ys.copyOf(count)
        executor.execute {
            val d = decoder; val m = main
            if (d == null || m == null) { done(GlideResult(emptyList(), 0)); return@execute }
            val t0 = System.nanoTime()
            val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(m); userLex?.let { lexicons.add(it) }
            val r = try { d.decode(px, py, count, lexicons, prev, 3, prev2) } catch (e: Exception) { emptyList() }
            val ms = (System.nanoTime() - t0) / 1_000_000
            stats.onDecoded(ms, r.isNotEmpty())
            if (r.isNotEmpty() && personal.weightOf(r[0].text) > 0f) stats.onLearnedTop()
            done(GlideResult(r, ms))
        }
    }

    /**
     * A live guess while the finger is still on the glass (AV3a): the path so far is decoded as if it ended here, on [previewExecutor], one at a time (a call while one is running is dropped: the finger
     * is faster than the decoder). [done] gets the guesses on that thread. Nothing is counted as a glide and nothing is learned.
     */
    fun preview(xs: FloatArray, ys: FloatArray, count: Int, prev: String?, prev2: String? = null, done: (GlideResult) -> Unit) {
        if (!ready || count < 3) return
        if (!previewBusy.compareAndSet(false, true)) return
        val px = xs.copyOf(count); val py = ys.copyOf(count)
        previewExecutor.execute {
            try {
                val d = previewDecoder; val m = main
                if (d != null && m != null) {
                    decoder?.let { d.biasX = it.biasX; d.biasY = it.biasY }
                    val t0 = System.nanoTime()
                    val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(m); userLex?.let { lexicons.add(it) }
                    val r = try { d.decode(px, py, count, lexicons, prev, 3, prev2) } catch (e: Exception) { emptyList() }
                    val ms = (System.nanoTime() - t0) / 1_000_000
                    stats.onPreview(ms)
                    done(GlideResult(r, ms))
                }
            } finally { previewBusy.set(false) }
        }
    }

    /** Words the person typed letter by letter that the shipped list does not know are remembered; [saved] gets the text to store (it runs on the executor's thread). */
    fun learnTyped(words: List<String>, own: UserWords, saved: (String) -> Unit) {
        executor.execute {
            val m = main
            var changed = false
            for (w in words) { if (m != null && m.idOf(w) >= 0) continue; own.add(w); changed = true }
            if (changed) { setOwn(own); saved(own.serialize()) }
        }
    }

    /**
     * The person's own choice: [word] was meant (after [prev] when known). [weight] 1 = it was kept or typed, 3 = the person CHOSE it over the keyboard's first guess (or replaced a deleted word by it). The next
     * glides lean towards it; the table is saved now and then, and at [saveLearned].
     */
    fun learn(prev: String?, word: String, weight: Float, prev2: String? = null, guessed: String? = null, guessedWeight: Float = 1f) {
        executor.execute {
            personal.add(prev, word, weight, prev2)
            if (guessed != null) personal.addConfusion(guessed, word, guessedWeight) // the keyboard guessed [guessed] and the person meant [word]: a correction
            personalDirty = true
            stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount)
            val now = System.nanoTime() / 1_000_000
            if (now - lastSaveAt > SAVE_EVERY_MS) saveNow()
        }
    }

    /** Writes the learned table if anything changed (the keyboard closing, the browser stopping). */
    fun saveLearned() { executor.execute { saveNow() } }

    private fun saveNow() {
        if (!personalDirty) return
        personalDirty = false
        lastSaveAt = System.nanoTime() / 1_000_000
        try { savePersonal(personal.serialize()) } catch (e: Exception) { personalDirty = true }
    }

    /** Settings > "Forget what the keyboard learned": the own words, the pairs and the thumb habit are gone. */
    fun forgetAll(done: () -> Unit = {}) {
        executor.execute {
            personal.clear(); personalDirty = false; stats.onLearned(0, 0, 0, 0)
            userLex = null; bx = 0f; by = 0f; habitX = 0f; habitY = 0f
            decoder?.let { it.biasX = 0f; it.biasY = 0f }
            stats.onHabit(0f, 0f)
            try { savePersonal("") } catch (e: Exception) { /* nothing left to save */ }
            done()
        }
    }

    /** The person kept [word] (the best candidate: the decoder learns where their finger lands. [habit] gets the new habit in pitches. */
    fun accepted(word: String, habit: (FloatArray) -> Unit) {
        executor.execute {
            val d = decoder ?: return@execute
            val m = main ?: return@execute
            val u = userLex
            val lex: GlideLexicon; val id: Int
            val mi = m.idOf(word)
            if (mi >= 0) { lex = m; id = mi } else { val ui = u?.idOf(word) ?: -1; if (ui < 0 || u == null) return@execute; lex = u; id = ui }
            d.learnFrom(lex, id)
            bx = d.biasX; by = d.biasY; habitX = bx; habitY = by
            val k = keys
            if (k != null) stats.onHabit(bx * k.pitch, by * k.pitch)
            habit(floatArrayOf(bx, by))
        }
    }

    companion object { private const val SAVE_EVERY_MS = 30_000L }
}
