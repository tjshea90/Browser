package app.kestrel.core.input.glide

import java.util.concurrent.Executor

/** What glide typing did, counted on the tablet itself (no words are kept) so a Diagnostics paste says how accurate it was in real use (AT1g). */
class GlideStats {
    private var glides = 0; private var noMatch = 0; private var kept = 0; private var picked = 0; private var undone = 0; private var revised = 0
    private var shadowDiffer = 0; private var shadowLearnedKept = 0; private var shadowPlainPicked = 0; private var planeDp = -1f
    private var ms = 0L; private var msMax = 0L
    private var pending = false
    private var loadMs = -1L; private var loadWords = 0; private var warmMs = -1L
    private var habitX = 0f; private var habitY = 0f
    private var own = 0
    private var learnedWords = 0; private var learnedPairs = 0; private var learnedTop = 0; private var learnedCorrections = 0; private var learnedTriples = 0
    private var shapes = 0; private var shapeWords = 0; private var shapeLifts = 0; private var shapeFlips = 0
    private var previews = 0; private var previewMs = 0L; private var shown = 0; private var shownMs = 0L; private var shownMax = 0L
    private var retries = 0
    private var suggested = 0; private var suggestMs = 0L; private var suggestMax = 0L; private var suggPicked = 0; private var typedLearned = 0; private var autoFixed = 0; private var autoUndone = 0; private var retyped = 0

    @Synchronized fun onDecoded(millis: Long, found: Boolean) { glides++; if (!found) noMatch++; ms += millis; if (millis > msMax) msMax = millis; if (found) pending = true }
    /** Whatever the person did next that was not a correction: the word stays. */
    @Synchronized fun onKept() { if (pending) { kept++; pending = false } }
    @Synchronized fun onPicked() { if (pending) { picked++; pending = false } }
    @Synchronized fun onUndone() { if (pending) { undone++; pending = false } }
    /** R27a: a second decode WITHOUT any personal learning (no thumb habit, no touch plane, no own words, pairs or shapes) gave another first word than the real one. */
    @Synchronized fun onShadow(differ: Boolean) { if (differ) shadowDiffer++ }
    /** ... and the person then kept the word learning chose (a glide they did not change). */
    @Synchronized fun onShadowLearnedKept() { shadowLearnedKept++ }
    /** ... and the person picked, in the strip, the word the plain decoder had given first. */
    @Synchronized fun onShadowPlainPicked() { shadowPlainPicked++ }
    /** How far the learned touch plane moves a touch somewhere on the keys, in dp (the largest of the four corners). */
    @Synchronized fun onPlane(maxDp: Float) { planeDp = maxDp }
    /** R26a: the word before was changed because the next glide showed it was probably another look-alike word. */
    @Synchronized fun onRevised() { revised++ }
    @Synchronized fun onLoaded(millis: Long, words: Int) { loadMs = millis; loadWords = words }
    @Synchronized fun onWarm(millis: Long) { warmMs = millis }
    @Synchronized fun onHabit(xDp: Float, yDp: Float) { habitX = xDp; habitY = yDp }
    @Synchronized fun onOwnWords(n: Int) { own = n }
    @Synchronized fun onLearned(words: Int, pairs: Int, corrections: Int = 0, triples: Int = 0, shapes: Int = 0, shapeWords: Int = 0) { learnedWords = words; learnedPairs = pairs; learnedCorrections = corrections; learnedTriples = triples; this.shapes = shapes; this.shapeWords = shapeWords }
    /** A remembered glide shape lifted a word in this glide; [flipped]: it changed which word came first. */
    @Synchronized fun onShape(flipped: Boolean) { shapeLifts++; if (flipped) shapeFlips++ }
    /** A live preview was decoded while the finger was still moving. */
    @Synchronized fun onPreview(millis: Long) { previews++; previewMs += millis }
    /** The word appeared [millis] after the finger lifted (the decode plus the wait for its turn plus drawing). */
    @Synchronized fun onShown(millis: Long) { shown++; shownMs += millis; if (millis > shownMax) shownMax = millis }
    /** A word the person had used before was the keyboard's first guess for this glide. */
    @Synchronized fun onLearnedTop() { learnedTop++ }
    /** The glide was a retry of a deleted word that came up first again: the deleted word was moved to second place. */
    @Synchronized fun onRetryDemoted() { retries++ }
    /** The strip was filled for tapped letters (or for the next word) in [millis]. */
    @Synchronized fun onSuggested(millis: Long) { suggested++; suggestMs += millis; if (millis > suggestMax) suggestMax = millis }
    /** A word from the strip was tapped while typing with taps. */
    @Synchronized fun onSuggestionPicked() { suggPicked++ }
    /** A word typed with taps was learned. */
    @Synchronized fun onTypedLearned() { typedLearned++ }
    /** A space put in the keyboard's fix for a typo; [undone]: the person took it back with Backspace right away. */
    @Synchronized fun onAutoCorrected() { autoFixed++ }
    @Synchronized fun onAutoCorrectUndone() { autoUndone++ }
    /** A word was erased and another typed in its place (the new one was meant). */
    @Synchronized fun onRetyped() { retyped++ }

    @Synchronized fun line(): String {
        if (loadMs < 0 && glides == 0 && typedLearned == 0 && suggested == 0) return "Glide typing: not used yet this run"
        val sb = StringBuilder("Glide typing: ")
        if (glides == 0) sb.append("no glide yet")
        else {
            val judged = kept + picked + undone
            sb.append("$glides glides")
            if (judged > 0) sb.append(": best word kept $kept (${100 * kept / judged} %), another candidate picked $picked, deleted right away $undone")
            if (revised > 0) sb.append(", word before fixed by the next word $revised times")
            if (shadowDiffer > 0) sb.append("; learning changed the first guess $shadowDiffer times (you kept the learned word $shadowLearnedKept times, you picked the plain decoder's word $shadowPlainPicked times)")
            if (noMatch > 0) sb.append(", no word fitted $noMatch")
            sb.append("; decode avg ${ms / glides} ms, worst $msMax ms")
        }
        if (shown > 0) sb.append("; word shown ${shownMs / shown} ms after the lift (worst $shownMax ms)")
        if (previews > 0) sb.append("; live preview $previews times, avg ${previewMs / previews} ms")
        if (loadMs >= 0) sb.append("; word list $loadWords words ready after $loadMs ms")
        if (warmMs >= 0) sb.append(", first decode (warm-up) $warmMs ms")
        sb.append("; thumb habit x %.1f y %.1f dp".format(habitX, habitY))
        if (planeDp >= 0f) sb.append(", touch plane up to %.1f dp".format(planeDp))
        if (own > 0) sb.append("; own words $own")
        if (learnedWords > 0 || learnedTop > 0) sb.append("; learned $learnedWords words and $learnedPairs word pairs from your typing, a learned word was the first guess $learnedTop times")
        if (learnedCorrections > 0 || learnedTriples > 0) sb.append("; $learnedCorrections corrections and $learnedTriples three-word phrases remembered")
        if (shapes > 0 || shapeLifts > 0) sb.append("; $shapes glide shapes of $shapeWords words remembered, a remembered shape helped $shapeLifts times and changed the first guess $shapeFlips times")
        if (typedLearned > 0 || suggested > 0) sb.append("; tap typing: $typedLearned words learned, suggestions made $suggested times (avg ${if (suggested > 0) suggestMs / suggested else 0} ms, worst $suggestMax ms), $suggPicked picked from the strip, $retyped erased and retyped")
        if (autoFixed > 0) sb.append("; automatic fixes $autoFixed, taken back $autoUndone")
        if (retries > 0) sb.append("; a deleted word came up first again and went to second place $retries times")
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

    /** Forgets every word (Settings > Forget what the keyboard learned). */
    @Synchronized fun clear() { counts.clear() }

    /** The list to glide into: [total] is the shipped list's total count, so the probabilities are on the same scale. */
    @Synchronized fun lexicon(total: Double): ArrayLexicon = lexicon(total, emptyCollection())
    private fun emptyCollection(): Collection<String> = emptyList()

    /** As above, and the words of the person's dictionary join at the level of a word used a dozen times: they are meant to be found. */
    @Synchronized fun lexicon(total: Double, extra: Collection<String>): ArrayLexicon {
        val m = LinkedHashMap<String, Long>()
        for ((w, n) in counts) m[w] = 30_000_000L + 25_000_000L * minOf(n, 12)
        for (w in extra) m[w] = maxOf(m[w] ?: 0L, DICTIONARY_COUNT)
        return ArrayLexicon(m.map { (w, c) -> w to c }, total)
    }

    @Synchronized fun serialize(): String = counts.entries.joinToString("\n") { "${it.key} ${it.value}" }

    companion object {
        /** What a word of the person's dictionary counts for (the level of a word used twelve times). */
        const val DICTIONARY_COUNT = 30_000_000L + 25_000_000L * 12
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
/** [ready] is false when the engine had no word list / key positions yet (the glide could not be decoded at all: "still starting", not "no word fits"). */
class GlideResult(
    val candidates: List<GlideCandidate>, val millis: Long, val ready: Boolean = true,
    /** The language weight this decode used ([GlideDecoder.lastLm]) and the 4th and 5th candidates: what the next word needs to re-judge this one (R26a). */
    val lm: Float = 0f, val more: List<GlideCandidate> = emptyList(),
    /** Which decode this was (for the shadow comparison, R27a). */
    val serial: Int = 0
)

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
    /** Stores the person's own words (the words typed that the shipped list lacks), on the executor's thread. */
    private val saveOwn: (String) -> Unit = {},
    /** Reads / stores the person's dictionary (the text [UserDictionary.serialize] wrote). */
    private val loadDictionary: () -> String? = { null },
    private val saveDictionary: (String) -> Unit = {},
) {
    /** The person's dictionary: words added (predicted when their pattern is glided) and words removed (never offered). Loaded on first use. */
    @Volatile var dictionary = UserDictionary(); private set
    @Volatile private var dictionaryLoaded = false
    private fun ensureDictionary() {
        if (dictionaryLoaded) return
        dictionaryLoaded = true
        try { dictionary = UserDictionary.parse(loadDictionary()) } catch (e: Exception) { /* a damaged file is just forgotten */ }
    }

    /** Words the keyboard must not offer are taken out of an answer (the engine asked for a few more than it shows, so three remain). */
    private fun cleaned(r: List<GlideCandidate>, max: Int): List<GlideCandidate> { val d = dictionary; return if (d.blockedCount == 0) r.take(max) else r.filter { !d.isBlocked(it.text) }.take(max) }
    private fun cleaned(r: Suggestions, limit: Int): Suggestions {
        val d = dictionary
        if (d.blockedCount == 0) return if (r.words.size <= limit) r else Suggestions(r.words.take(limit), r.typedKnown, r.autoCorrect, r.typed)
        return Suggestions(r.words.filter { !d.isBlocked(it) }.take(limit), r.typedKnown, r.autoCorrect?.takeIf { !d.isBlocked(it) }, r.typed)
    }

    /** Never offer [word] again (a long press on a guess; Settings > My dictionary). [done] gets the result on the engine thread. */
    fun blockWord(word: String, done: (UserDictionary.Result) -> Unit = {}) { executor.execute { ensureDictionary(); val r = dictionary.block(word); if (r == UserDictionary.Result.OK) { rebuildUserLexicon(); persistDictionary() }; done(r) } }
    /** Offer [word] again after it was removed. */
    fun unblockWord(word: String, done: (Boolean) -> Unit = {}) { executor.execute { ensureDictionary(); val ok = dictionary.unblock(word); if (ok) persistDictionary(); done(ok) } }
    /** Adds [word] to the dictionary: it is predicted when its pattern is glided. */
    fun addDictionaryWord(word: String, done: (UserDictionary.Result) -> Unit = {}) { executor.execute { ensureDictionary(); val r = dictionary.add(word); if (r == UserDictionary.Result.OK) { rebuildUserLexicon(); persistDictionary() }; done(r) } }
    fun removeDictionaryWord(word: String, done: (Boolean) -> Unit = {}) { executor.execute { ensureDictionary(); val ok = dictionary.removeAdded(word); if (ok) { rebuildUserLexicon(); persistDictionary() }; done(ok) } }
    /** The two lists, for the settings screen. */
    fun dictionarySnapshot(done: (added: List<String>, blocked: List<String>) -> Unit) { executor.execute { ensureDictionary(); done(dictionary.addedWords(), dictionary.blockedWords()) } }
    private fun persistDictionary() { try { saveDictionary(dictionary.serialize()) } catch (e: Exception) { /* a full disk must never break typing */ } }
    private fun rebuildUserLexicon() { ownRef?.let { setOwn(it) } ?: run { userLex = if (dictionary.addedCount == 0) null else UserWords(1).lexicon(main?.totalCount ?: 5.4e11, dictionary.addedWords()) } }

    /** Loads the shipped word list if no keyboard has done it yet (the settings screen can search the dictionary before the keyboard was ever opened). [done] runs on the engine thread. */
    fun ensureListLoaded(done: () -> Unit = {}) {
        executor.execute {
            try { if (main == null && !loading) { loading = true; val t0 = System.nanoTime(); val b = loadMain(); if (b != null) { main = LexiconFile.read(b); stats.onLoaded((System.nanoTime() - t0) / 1_000_000, main!!.size) }; loading = false } } catch (e: Exception) { loading = false }
            ensureDictionary(); done()
        }
    }

    /** What the dictionary search of the settings screen shows: whether [word] is in the shipped list, and the most common words that start with [prefix]. Runs on the preview thread. */
    fun lookup(prefix: String, limit: Int, done: (known: Boolean, words: List<String>) -> Unit) {
        previewExecutor.execute {
            val m = main
            val p = prefix.trim().lowercase()
            if (m == null || p.isEmpty() || !p.all { it in 'a'..'z' }) { done(false, emptyList()); return@execute }
            val range = m.prefixIds(p)
            val top = range.sortedByDescending { m.logProb(it) }.take(limit * 2).map { m.text(it) }
            done(m.idOf(p) >= 0, top.take(limit))
        }
    }
    @Volatile private var main: BinaryLexicon? = null
    @Volatile private var userLex: ArrayLexicon? = null
    @Volatile private var decoder: GlideDecoder? = null
    @Volatile private var previewDecoder: GlideDecoder? = null // its own scratch space: the real decoder's state (the path of the last glide) is never touched by a preview
    private val previewBusy = java.util.concurrent.atomic.AtomicBoolean(false)
    private var keys: GlideKeys? = null
    private var bx = 0f; private var by = 0f
    private var loading = false
    private var lastUserWords = 0
    @Volatile private var personal = PersonalModel()
    private var personalLoaded = false
    private var ownRef: UserWords? = null
    private val suggestSeq = java.util.concurrent.atomic.AtomicInteger()

    /** What was saved is read once, before anything is learned, exported or imported (on the executor's thread): an import must add to what is on disk, not replace it with an empty table that was never loaded. */
    private fun ensurePersonal() {
        if (personalLoaded) return
        personalLoaded = true
        try { loadPersonal()?.let { personal = PersonalModel.parse(it) } } catch (e: Exception) { /* a damaged file is just forgotten */ }
        stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount, personal.patterns.patternCount, personal.patterns.wordCount)
    }
    private var personalDirty = false
    private var lastSaveAt = 0L
    private var lastPrint: FloatArray? = null     // the shape of the last real glide (executor thread): what a kept, picked or corrected word is remembered with
    private var deletedPrint: FloatArray? = null  // the shape of the glide whose word was just deleted: what the word typed in its place may have been meant by
    private var deletedGuess: String? = null

    /** The best-known state of the thumb habit in pitches (read on the executor thread; copied out for saving). */
    @Volatile var habitX = 0f; private set
    @Volatile var habitY = 0f; private set

    /** True once a glide can be decoded. */
    @Volatile var ready = false; private set

    /** R27a: the same decoder with nothing learned (no habit, plane, own words, pairs or shapes): its first word is compared with the real one, after the real answer was handed out. */
    private var plainDecoder: GlideDecoder? = null
    private var serial = 0
    class Shadow(val serial: Int, val plain: String?, val learned: String?)
    @Volatile private var shadow: Shadow? = null
    /** The comparison for decode [serial], once it has run (null while it is still running, or when learning could not matter). */
    fun shadowFor(serial: Int): Shadow? = shadow?.takeIf { it.serial == serial }

    /** Load the list (once) and set the decoder up for [k] (again when the keyboard's size changed). [habit] is the saved thumb habit in pitches. */
    fun prepare(k: GlideKeys, habit: FloatArray?, own: UserWords?) {
        executor.execute {
            try {
                if (decoder == null && habit != null && habit.size >= 2) { bx = habit[0]; by = habit[1]; if (habit.size > 2) touchSaved = habit.copyOfRange(2, habit.size) } // the saved habit (and the touch map after it) starts the engine; later it keeps what it learned itself
                if (main == null && !loading) {
                    loading = true
                    val t0 = System.nanoTime()
                    val bytes = loadMain()
                    if (bytes != null) { main = LexiconFile.read(bytes); stats.onLoaded((System.nanoTime() - t0) / 1_000_000, main!!.size) }
                    loading = false
                }
                ensurePersonal()
                keys = k
                decoder = GlideDecoder(k, GlideParams.forPitchDp(k.pitchDp)).also { it.biasX = bx; it.biasY = by; it.personal = personal; touchSaved?.let { a -> it.field.fromArray(a) } }
                plainDecoder = GlideDecoder(k, GlideParams.forPitchDp(k.pitchDp, touchField = false))
                previewDecoder = GlideDecoder(k, GlideParams.forPitchDp(k.pitchDp)).also { it.biasX = bx; it.biasY = by; it.personal = personal; touchSaved?.let { a -> it.field.fromArray(a) } }
                ensureDictionary()
                if (own != null) { ownRef = own; setOwn(own) } else rebuildUserLexicon()
                ready = main != null
                if (!warmed && ready) { warmed = true; warmUp(k) }
            } catch (e: Exception) { loading = false; ready = false }
        }
    }

    private var warmed = false
    /** R20d: the learned touch map (see [TouchField.toArray]), kept here so a new decoder (the keyboard changed size) starts with it; null = nothing learned. */
    private var touchSaved: FloatArray? = null

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
        ensureDictionary()
        val dict = dictionary.addedWords()
        userLex = if (own.size == 0 && dict.isEmpty()) null else own.lexicon(m?.totalCount ?: 5.4e11, dict)
        lastUserWords = own.size
        stats.onOwnWords(own.size)
    }

    /** Decode the path of [count] touch points; [done] is called on the executor's thread. */
    fun decode(xs: FloatArray, ys: FloatArray, count: Int, prev: String?, prev2: String? = null, done: (GlideResult) -> Unit) {
        val px = xs.copyOf(count); val py = ys.copyOf(count)
        executor.execute {
            val d = decoder; val m = main
            if (d == null || m == null) { done(GlideResult(emptyList(), 0, ready = false)); return@execute }
            val t0 = System.nanoTime()
            val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(m); userLex?.let { lexicons.add(it) }
            val my = ++serial
            val five = try { cleaned(d.decode(px, py, count, lexicons, prev, 5 + EXTRA_FOR_BLOCKED, prev2), 5) } catch (e: Exception) { emptyList() }
            val r = five.take(3)
            val lm = d.lastLm
            val ms = (System.nanoTime() - t0) / 1_000_000
            stats.onDecoded(ms, r.isNotEmpty())
            if (r.isNotEmpty() && personal.weightOf(r[0].text) > 0f) stats.onLearnedTop()
            lastPrint = d.lastPrint
            if (d.lastPatternLifted) stats.onShape(d.lastPatternFlipped)
            done(GlideResult(r, ms, true, lm, five.drop(3), my))
            // R27a: AFTER the answer is out (no added delay), the same glide through a decoder that has learned nothing: how often does learning change the first word?
            val pd = plainDecoder
            if (pd != null && r.isNotEmpty() && (!personal.isEmpty || d.biasX != 0f || d.biasY != 0f || !d.field.isEmpty)) {
                val plain = try { cleaned(pd.decode(px, py, count, lexicons, prev, 3 + EXTRA_FOR_BLOCKED, prev2), 3) } catch (e: Exception) { emptyList() }
                val p0 = plain.firstOrNull()?.text
                shadow = Shadow(my, p0, r[0].text)
                stats.onShadow(p0 != null && p0 != r[0].text)
            }
        }
    }

    /**
     * R26a: ln P([next] | each of [cands] as the word before it); null when the engine is not ready or [next] is unknown. Synchronous, cheap (a few table look-ups), called on the UI thread between decodes.
     */
    fun nextWordLnProbs(cands: List<String>, next: String, candPrev: String?): FloatArray? {
        val d = decoder; val m = main ?: return null
        if (d == null) return null
        val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(m); userLex?.let { lexicons.add(it) }
        val out = FloatArray(cands.size) { d.nextLnProb(lexicons, cands[it], next, candPrev) }
        return if (out.any { it.isNaN() }) null else out
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
                    decoder?.let { d.biasX = it.biasX; d.biasY = it.biasY; d.field.copyFrom(it.field) }
                    val t0 = System.nanoTime()
                    val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(m); userLex?.let { lexicons.add(it) }
                    val r = try { cleaned(d.decode(px, py, count, lexicons, prev, 3 + EXTRA_FOR_BLOCKED, prev2), 3) } catch (e: Exception) { emptyList() }
                    val ms = (System.nanoTime() - t0) / 1_000_000
                    stats.onPreview(ms)
                    done(GlideResult(r, ms))
                }
            } finally { previewBusy.set(false) }
        }
    }

    /**
     * Tap typing (N4): the words to offer for the letters [typed] so far (completions and typo fixes), or the likely next words when [typed] is empty. [done] is called on a worker thread, unless a newer
     * request has been made by then (the latest letter always wins: an answer for an older word is dropped, never shown late).
     */
    fun suggest(typed: String, prev: String?, prev2: String?, limit: Int, done: (Suggestions) -> Unit) {
        val my = suggestSeq.incrementAndGet()
        previewExecutor.execute {
            if (my != suggestSeq.get()) return@execute
            val m = main
            if (m == null || !ready) return@execute
            val t0 = System.nanoTime()
            val r = try { cleaned(Suggester.suggest(m, personal, keys, typed, prev, prev2, limit + EXTRA_FOR_BLOCKED), limit) } catch (e: Exception) { Suggestions.NONE }
            stats.onSuggested((System.nanoTime() - t0) / 1_000_000)
            if (my == suggestSeq.get()) done(r)
        }
    }

    /**
     * R18a: the words to offer in place of [word], which is already in the text ([Alternatives]): the ones whose glide pattern is close to it, ranked with the words before it ([prev2], [prev]) and the word after it
     * ([next]). [done] is called on a worker thread, unless a newer request (of any kind) has been made by then.
     */
    fun alternatives(word: String, prev2: String?, prev: String?, next: String?, limit: Int, done: (List<String>) -> Unit) {
        val my = suggestSeq.incrementAndGet()
        previewExecutor.execute {
            if (my != suggestSeq.get()) return@execute
            val m = main; val k = keys
            if (m == null || k == null || !ready) return@execute
            val t0 = System.nanoTime()
            val d = dictionary
            val r = try { Alternatives.find(m, userLex, personal, previewDecoder, k, word, prev2, prev, next, limit + EXTRA_FOR_BLOCKED) { d.isBlocked(it) }.take(limit) } catch (e: Exception) { emptyList() }
            stats.onSuggested((System.nanoTime() - t0) / 1_000_000)
            if (my == suggestSeq.get()) done(r)
        }
    }

    /** What the strip shows is no longer wanted (a glide started, the field changed): an answer on its way is dropped. */
    fun cancelSuggest() { suggestSeq.incrementAndGet() }

    /**
     * A word the person TYPED was completed (space, punctuation, Enter) or chosen from the strip: it is learned like a glided one (the word, the pair, the triple). [weight] 1 for an ordinary word, more for a
     * chosen or retyped one; [guessed] is what the keyboard had (or the person had typed first) when they meant [word]. A word the shipped list lacks that was used twice joins the person's own words
     * (so glides can find it too).
     */
    fun learnWord(prev: String?, word: String, weight: Float, prev2: String? = null, guessed: String? = null, guessedWeight: Float = 1f) {
        executor.execute {
            ensurePersonal(); ensureDictionary()
            if (dictionary.isBlocked(word)) return@execute                        // a word the person removed is not learned again
            personal.add(prev, word, weight, prev2)
            if (guessed != null) personal.addConfusion(guessed, word, guessedWeight)
            val n = PersonalModel.norm(word)
            val m = main; val o = ownRef
            if (n != null && m != null && o != null && personal.weightOf(n) >= Suggester.OWN_WORD_USES && !o.contains(n) && m.idOf(n).let { it < 0 || m.text(it).lowercase() != n } && UserWords.glideable(n)) {
                o.add(n); setOwn(o); try { saveOwn(o.serialize()) } catch (e: Exception) { /* a full disk must never break typing */ }
            }
            stats.onTypedLearned()
            personalDirty = true
            stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount, personal.patterns.patternCount, personal.patterns.wordCount)
            if (System.nanoTime() / 1_000_000 - lastSaveAt > SAVE_EVERY_MS) saveNow()
        }
    }

    /** R18d: [word] began a sentence (the first word of the box, or the first after . ! ? or a new line): the strip learns what this person starts with. */
    fun learnStart(word: String, weight: Float = 1f) {
        executor.execute {
            ensurePersonal(); ensureDictionary()
            if (dictionary.isBlocked(word.lowercase())) return@execute
            personal.addStart(word, weight); personalDirty = true
            if (System.nanoTime() / 1_000_000 - lastSaveAt > SAVE_EVERY_MS) saveNow()
        }
    }

    /** R18d: an automatic fix was made ([undone] false) or taken back with Backspace ([undone] true): the keyboard gets stricter for a person who undoes many, bolder for one who undoes none ([PersonalModel.autoMarginShift]). */
    fun learnFix(undone: Boolean) {
        executor.execute { ensurePersonal(); personal.addFix(undone); personalDirty = true }
    }

    /**
     * R18b: how [lower] is written inside a sentence (this person's own habit first, then the built-in names and date words after [prev]); null = as it was typed. Cheap and safe on any thread (the keyboard asks it on the UI thread).
     */
    fun caseFormOf(lower: String, prev: String? = null): String? = try { CaseBook.form(lower, personal, prev) } catch (e: Exception) { null }

    /**
     * R18b: [shown] is the way the person wrote a word inside a sentence (a capital at the start of a sentence says nothing and is never sent here). A capital form counts toward "this word is written with a capital";
     * a small one only counts for a word whose capital is in question (the keyboard's list has it, or the person wrote it with a capital before). [weight] 2 = the person took the keyboard's capital back.
     */
    fun learnCase(shown: String, weight: Float = 1f) {
        executor.execute {
            ensurePersonal(); ensureDictionary()
            val lower = shown.lowercase()
            if (dictionary.isBlocked(lower)) return@execute
            if (shown == lower && !(CaseBook.hasBuiltin(lower) || personal.hasCase(lower))) return@execute
            personal.addCase(shown, weight)
            personalDirty = true
            if (System.nanoTime() / 1_000_000 - lastSaveAt > SAVE_EVERY_MS) saveNow()
        }
    }

    /** A word the person deleted after it was learned: what it earned is taken back (a slip that was typed once and erased must not become a favourite). */
    fun unlearnWord(prev: String?, word: String, weight: Float, prev2: String? = null) {
        executor.execute {
            ensurePersonal()
            personal.unlearn(prev, word, weight, prev2)
            personalDirty = true
            stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount, personal.patterns.patternCount, personal.patterns.wordCount)
            if (System.nanoTime() / 1_000_000 - lastSaveAt > SAVE_EVERY_MS) saveNow()
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
    fun learn(prev: String?, word: String, weight: Float, prev2: String? = null, guessed: String? = null, guessedWeight: Float = 1f, fromGlide: Boolean = false, afterUndo: Boolean = false) {
        executor.execute {
            ensurePersonal(); ensureDictionary()
            if (dictionary.isBlocked(word)) return@execute
            personal.add(prev, word, weight, prev2)
            if (guessed != null) personal.addConfusion(guessed, word, guessedWeight) // the keyboard guessed [guessed] and the person meant [word]: a correction
            if (fromGlide) learnShape(word, weight, guessed != null, afterUndo)
            personalDirty = true
            stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount, personal.patterns.patternCount, personal.patterns.wordCount)
            val now = System.nanoTime() / 1_000_000
            if (now - lastSaveAt > SAVE_EVERY_MS) saveNow()
        }
    }

    /**
     * The shape of the glide that just put [word] in is remembered for it (AX1). A correction ([corrected]: a pick from the strip, or a glide that replaced a deleted one) makes the words that were
     * drawn like this before less sure; after a deletion the shape of the DELETED glide is remembered for [word] too when it could have been meant for it (the first try was just sloppier).
     * Runs on the executor's thread.
     */
    private fun learnShape(word: String, weight: Float, corrected: Boolean, afterUndo: Boolean) {
        val pr = lastPrint
        if (pr != null) { personal.patterns.add(word, pr, weight); personal.patterns.contradict(word, pr, if (corrected) 2f else 0.7f) }
        if (afterUndo) shapeOfDeleted(word)
    }

    /** The deleted glide's shape is [word]'s too when the word could have been drawn like that (the straight line through its letters is not far from it). */
    private fun shapeOfDeleted(word: String): Boolean {
        val dp = deletedPrint ?: return false
        deletedPrint = null; deletedGuess = null
        val k = keys ?: return false
        val ideal = Fingerprint.ofWord(word, k) ?: return false
        if (Fingerprint.distance(ideal, dp) > FIT_D) return false
        personal.patterns.add(word, dp, 3f); personal.patterns.contradict(word, dp, 2f)
        return true
    }

    /** The glided word was deleted (Backspace right after the glide): its shape waits for the word that is typed instead ([retyped], or the next glide that is kept). */
    fun glideDeleted(guess: String?) { executor.execute { deletedPrint = lastPrint; deletedGuess = guess } }

    /** [word] was typed letter by letter after a glided word was deleted: if the deleted glide could have meant it, its shape is remembered for it (the person corrected the keyboard's guess). */
    fun retyped(word: String) {
        executor.execute {
            if (shapeOfDeleted(word)) {
                personalDirty = true
                stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount, personal.patterns.patternCount, personal.patterns.wordCount)
                if (System.nanoTime() / 1_000_000 - lastSaveAt > SAVE_EVERY_MS) saveNow()
            }
        }
    }

    /** What the keyboard has learned, as the text of the export file (see [LearnedExport]), for the Settings export: it is built on the executor's thread and handed to [done] there. [habit] null = the thumb habit the engine holds now. */
    fun exportText(habit: FloatArray?, own: UserWords?, done: (String) -> Unit) {
        executor.execute { ensurePersonal(); done(LearnedExport.write(personal, habit ?: (floatArrayOf(bx, by) + (touchSaved ?: FloatArray(0))), own)) }
    }

    /**
     * An export file was picked (Settings > Import): its words, pairs, corrections and shapes are ADDED to what is learned ([replace] false) or replace it, and the thumb habit and own words come along. [done] gets
     * a one-line description of what was read, or null when the text is not an export of this keyboard. Runs on the executor's thread.
     */
    fun importText(text: String, replace: Boolean, own: UserWords, habit: (FloatArray) -> Unit, done: (LearnedExport.Summary?) -> Unit) {
        executor.execute {
            val parsed = LearnedExport.read(text)
            if (parsed == null) { done(null); return@execute }
            ensurePersonal()
            if (replace) personal.clear()
            personal.mergeFrom(parsed.model)
            for ((w, n) in parsed.ownWords) if (!own.contains(w)) own.add(w, n)
            // the thumb habit of a file is taken only while this keyboard has not learned one of its own (what it learned here is newer than any file)
            if (parsed.touch != null && touchSaved == null) { touchSaved = parsed.touch; decoder?.field?.fromArray(parsed.touch); previewDecoder?.field?.fromArray(parsed.touch); habit(floatArrayOf(bx, by) + parsed.touch) }
            if (parsed.habit != null && Math.abs(bx) + Math.abs(by) < 0.01f) { bx = parsed.habit[0]; by = parsed.habit[1]; habitX = bx; habitY = by; decoder?.let { it.biasX = bx; it.biasY = by }; habit(floatArrayOf(bx, by) + (touchSaved ?: FloatArray(0))); keys?.let { stats.onHabit(bx * it.pitch, by * it.pitch) } }
            setOwn(own)
            personalDirty = true
            stats.onLearned(personal.wordCount, personal.pairCount, personal.confusionCount, personal.tripleCount, personal.patterns.patternCount, personal.patterns.wordCount)
            saveNow()
            done(parsed.summary)
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
            personal.clear(); personalDirty = false; lastPrint = null; deletedPrint = null; deletedGuess = null; stats.onLearned(0, 0, 0, 0)
            userLex = null; bx = 0f; by = 0f; habitX = 0f; habitY = 0f
            decoder?.let { it.biasX = 0f; it.biasY = 0f; it.field.clear() }
            previewDecoder?.field?.clear(); touchSaved = null
            stats.onHabit(0f, 0f)
            try { savePersonal("") } catch (e: Exception) { /* nothing left to save */ }
            done()
        }
    }

    /** The largest move the learned touch plane makes at any corner of the letter keys, in dp (Diagnostics). */
    private fun reportPlane(d: GlideDecoder) {
        val k = keys ?: return
        val o = FloatArray(2); var m = 0f
        for (u in floatArrayOf(0f, 1f)) for (v in floatArrayOf(0f, 1f)) { d.field.at(u, v, o); m = Math.max(m, Math.hypot(o[0].toDouble(), o[1].toDouble()).toFloat()) }
        stats.onPlane(m * k.pitchDp)
    }

    /**
     * R27a: forget only WHERE THE THUMB LANDS (the single shift and the touch plane), keep every word, pair and correction. For a keyboard that misreads a clear glide in a systematic way: if that was learned
     * from words it got wrong, this takes it away. [habit] stores the cleared habit; [done] is called on the engine's thread.
     */
    fun forgetThumb(habit: (FloatArray) -> Unit, done: () -> Unit = {}) {
        executor.execute {
            bx = 0f; by = 0f; habitX = 0f; habitY = 0f
            decoder?.let { it.biasX = 0f; it.biasY = 0f; it.field.clear(); reportPlane(it) }
            previewDecoder?.let { it.biasX = 0f; it.biasY = 0f; it.field.clear() }
            touchSaved = null
            stats.onHabit(0f, 0f)
            habit(floatArrayOf(0f, 0f))
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
            val t = d.field.toArray(); touchSaved = t
            reportPlane(d)
            habit(floatArrayOf(bx, by) + t)
        }
    }

    companion object {
        private const val SAVE_EVERY_MS = 10_000L
        /** The engine is asked for this many more guesses than are shown, so that removing the words the person blocked still leaves a full strip. */
        private const val EXTRA_FOR_BLOCKED = 4
        /** A deleted glide can have meant the word typed in its place when the straight line through that word's letters is within this many key units of it (a careful glide is about 0.3 away, a sloppy one 0.6 to 0.9, another word more than 1.3). */
        const val FIT_D = 1.15f
    }
}
