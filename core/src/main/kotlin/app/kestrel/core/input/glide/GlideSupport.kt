package app.kestrel.core.input.glide

import java.util.concurrent.Executor

/** What glide typing did, counted on the tablet itself (no words are kept) so a Diagnostics paste says how accurate it was in real use (AT1g). */
class GlideStats {
    private var glides = 0; private var noMatch = 0; private var kept = 0; private var picked = 0; private var undone = 0
    private var ms = 0L; private var msMax = 0L
    private var pending = false
    private var loadMs = -1L; private var loadWords = 0
    private var habitX = 0f; private var habitY = 0f
    private var own = 0

    @Synchronized fun onDecoded(millis: Long, found: Boolean) { glides++; if (!found) noMatch++; ms += millis; if (millis > msMax) msMax = millis; if (found) pending = true }
    /** Whatever the person did next that was not a correction: the word stays. */
    @Synchronized fun onKept() { if (pending) { kept++; pending = false } }
    @Synchronized fun onPicked() { if (pending) { picked++; pending = false } }
    @Synchronized fun onUndone() { if (pending) { undone++; pending = false } }
    @Synchronized fun onLoaded(millis: Long, words: Int) { loadMs = millis; loadWords = words }
    @Synchronized fun onHabit(xDp: Float, yDp: Float) { habitX = xDp; habitY = yDp }
    @Synchronized fun onOwnWords(n: Int) { own = n }

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
        if (loadMs >= 0) sb.append("; word list $loadWords words ready after $loadMs ms")
        sb.append("; thumb habit x %.1f y %.1f dp".format(habitX, habitY))
        if (own > 0) sb.append("; own words $own")
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
        /** A word a finger can draw: lowercase letters, three to 24, with at least two different letters in a row. */
        fun glideable(w: String): Boolean {
            if (w.length !in 3..24 || !w.all { it in 'a'..'z' }) return false
            for (i in 1 until w.length) if (w[i] != w[i - 1]) return true
            return false
        }

        fun parse(text: String, cap: Int = 800): UserWords {
            val u = UserWords(cap)
            for (line in text.lineSequence()) { val p = line.trim().split(' '); if (p.size == 2) u.add(p[0], p[1].toIntOrNull() ?: 1) }
            return u
        }

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
class GlideTyping(private val loadMain: () -> ByteArray?, private val executor: Executor, val stats: GlideStats = GlideStats()) {
    private var main: BinaryLexicon? = null
    private var userLex: ArrayLexicon? = null
    private var decoder: GlideDecoder? = null
    private var keys: GlideKeys? = null
    private var bx = 0f; private var by = 0f
    private var loading = false
    private var lastUserWords = 0

    /** The best-known state of the thumb habit in pitches (read on the executor thread; copied out for saving). */
    @Volatile var habitX = 0f; private set
    @Volatile var habitY = 0f; private set

    /** True once a glide can be decoded. */
    @Volatile var ready = false; private set

    /** Load the list (once) and set the decoder up for [k] (again when the keyboard's size changed). [habit] is the saved thumb habit in pitches. */
    fun prepare(k: GlideKeys, habit: FloatArray?, own: UserWords?) {
        executor.execute {
            try {
                if (habit != null && habit.size == 2) { bx = habit[0]; by = habit[1] }
                if (main == null && !loading) {
                    loading = true
                    val t0 = System.nanoTime()
                    val bytes = loadMain()
                    if (bytes != null) { main = LexiconFile.read(bytes); stats.onLoaded((System.nanoTime() - t0) / 1_000_000, main!!.size) }
                    loading = false
                }
                keys = k
                decoder = GlideDecoder(k).also { it.biasX = bx; it.biasY = by }
                if (own != null) setOwn(own)
                ready = main != null
            } catch (e: Exception) { loading = false; ready = false }
        }
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
    fun decode(xs: FloatArray, ys: FloatArray, count: Int, prev: String?, done: (GlideResult) -> Unit) {
        val px = xs.copyOf(count); val py = ys.copyOf(count)
        executor.execute {
            val d = decoder; val m = main
            if (d == null || m == null) { done(GlideResult(emptyList(), 0)); return@execute }
            val t0 = System.nanoTime()
            val lexicons = ArrayList<GlideLexicon>(2); lexicons.add(m); userLex?.let { lexicons.add(it) }
            val r = try { d.decode(px, py, count, lexicons, prev, 3) } catch (e: Exception) { emptyList() }
            val ms = (System.nanoTime() - t0) / 1_000_000
            stats.onDecoded(ms, r.isNotEmpty())
            done(GlideResult(r, ms))
        }
    }

    /** The person kept [word] (the best candidate, or one picked from the strip) for the glide decoded last: the decoder learns where their finger lands. [habit] gets the new habit in pitches. */
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
}
