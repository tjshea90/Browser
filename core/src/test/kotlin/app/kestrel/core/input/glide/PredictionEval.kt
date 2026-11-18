package app.kestrel.core.input.glide

import java.util.Random

/**
 * The word-prediction yardstick for TAP typing (KB-406, owner request 17): it types the sentences of [EvalCorpus] the way a person does - letter after letter, glancing at the strip - against the SHIPPED word
 * list, and counts
 *  - next word: the right word is among the 3 offered before a letter is typed,
 *  - completion: the right word is on the strip after 1, 2, 3 letters (the strip shows the typed text as one of its three places while it is not a word, exactly like KeyboardPresenter.applySuggestions),
 *  - keystroke saving: letters not typed because the word was tapped on the strip at the first moment it showed,
 *  - typo repair: a word typed with one realistic slip (a neighbouring key, a letter left out, a letter doubled, two letters swapped) is on the strip / put in by the space, and a word typed RIGHT is never
 *    changed (the failure people hate most).
 * Run by [PredictionYardstickTest] (with stated floors) and by hand: java -cp <core main>:<core test>:<kotlin stdlib> app.kestrel.core.input.glide.PredictionEval [verbose] [klx=<file>] [corpus=<file> [limit=N]] [glide [glides=N]] [notypos] [probe=word,word@prev] [profanity]
 */
object PredictionEval {
    class Tally(val name: String) {
        var n = 0; var top1 = 0; var top3 = 0
        fun add(first: Boolean, any: Boolean) { n++; if (first) top1++; if (any) top3++ }
        val r1 get() = if (n == 0) 0.0 else top1.toDouble() / n
        val r3 get() = if (n == 0) 0.0 else top3.toDouble() / n
        override fun toString() = "%-34s n=%5d  first %5.1f %%  in 3 %5.1f %%".format(name, n, 100 * r1, 100 * r3)
    }

    class Result(
        val next: Tally, val comp1: Tally, val comp2: Tally, val comp3: Tally,
        val shown1: Tally, val shown2: Tally, val shown3: Tally,
        val ksr: Double, val keysTyped: Int, val keysSaved: Int,
        val typoNeighbour: Tally, val typoMissing: Tally, val typoDouble: Tally, val typoSwap: Tally,
        val typoFixedBySpace: IntArray,           // [right fixes, wrong fixes, no fix, total] over typos that are not another word
        val rightWordChanged: Int, val rightWords: Int,
        val searchNext: Tally, val searchComp2: Tally, val searchKsr: Double,
        val nextEmpty: Int, val nextShort: Int,
        val notes: List<String>,
    )

    /** What the strip shows for [typed] (the same rule as KeyboardPresenter.applySuggestions); the typed text itself is not counted as a suggestion. */
    fun strip(res: Suggestions): List<String> {
        val out = ArrayList<String>(3)
        var literal = 0
        if (res.typed.isNotEmpty() && !res.typedKnown) { val fix = res.autoCorrect; if (fix != null) out.add(fix); literal = 1 }
        for (w in res.words) if (out.size + literal < 3 && out.none { it.equals(w, true) }) out.add(w)
        return out
    }

    private fun contextOf(s: List<String>, i: Int): Pair<String?, String?> = (if (i >= 1) s[i - 1] else null) to (if (i >= 2) s[i - 2] else null)

    fun run(main: BinaryLexicon, pm: PersonalModel?, keys: GlideKeys, sentences: List<List<String>>, searches: List<List<String>>, verbose: Boolean = false, withTypos: Boolean = true): Result {
        val notes = ArrayList<String>()
        val next = Tally("next word (nothing typed)")
        val comp = arrayOf(Tally("completion after 1 letter"), Tally("completion after 2 letters"), Tally("completion after 3 letters"))
        val shown = arrayOf(Tally("on the strip after 1 letter"), Tally("on the strip after 2 letters"), Tally("on the strip after 3 letters"))
        var typedTotal = 0; var saved = 0; var allKeys = 0
        var nextEmpty = 0; var nextShort = 0
        for (s in sentences) for ((i, w) in s.withIndex()) {
            val (p1, p2) = contextOf(s, i)
            val rn = Suggester.suggest(main, pm, keys, "", p1, p2, 3)
            next.add(rn.words.firstOrNull().equals(w, true), rn.words.any { it.equals(w, true) })
            if (rn.words.isEmpty()) nextEmpty++ else if (rn.words.size < 3) nextShort++
            var foundAt = -1
            if (rn.words.any { it.equals(w, true) }) foundAt = 0
            for (k in 1..3) {
                if (w.length <= k) break
                val r = Suggester.suggest(main, pm, keys, w.take(k), p1, p2, 3)
                comp[k - 1].add(r.words.firstOrNull().equals(w, true), r.words.any { it.equals(w, true) })
                val st = strip(r)
                shown[k - 1].add(st.firstOrNull().equals(w, true), st.any { it.equals(w, true) })
            }
            if (foundAt < 0) for (k in 1 until w.length) {
                val st = strip(Suggester.suggest(main, pm, keys, w.take(k), p1, p2, 3))
                if (st.any { it.equals(w, true) }) { foundAt = k; break }
            }
            val cost = w.length + 1                                 // the letters and the space
            allKeys += cost
            if (foundAt >= 0 && foundAt < w.length) saved += (w.length - foundAt) else typedTotal += 0
            typedTotal += cost
            if (verbose && rn.words.none { it.equals(w, true) } && i > 0 && (i % 3 == 0)) notes.add("next miss: ${s.take(i).takeLast(2).joinToString(" ")} _ [$w]  got ${rn.words}")
        }
        val ksr = if (allKeys == 0) 0.0 else saved.toDouble() / allKeys

        // typos: one realistic slip in a word of four or more letters
        val rnd = Random(4242)
        val tn = Tally("typo: a neighbouring key"); val tm = Tally("typo: a letter left out"); val td = Tally("typo: a letter doubled"); val ts = Tally("typo: two letters swapped")
        val fix = IntArray(4)
        var changed = 0; var rights = 0
        if (withTypos) for (s in sentences) for ((i, w) in s.withIndex()) {
            val (p1, p2) = contextOf(s, i)
            if (w.length >= 3 && w.all { it in 'a'..'z' }) {
                val r = Suggester.suggest(main, pm, keys, w, p1, p2, 3)
                rights++
                if (r.autoCorrect != null && !r.autoCorrect!!.equals(w, true)) { changed++; if (verbose) notes.add("RIGHT WORD CHANGED: ${s.take(i).takeLast(2).joinToString(" ")} _ [$w] -> ${r.autoCorrect}") }
            }
            if (w.length < 4 || !w.all { it in 'a'..'z' }) continue
            for (kind in 0..3) {
                val typo = slip(w, kind, keys, rnd) ?: continue
                if (typo == w) continue
                val id = main.idOf(typo)
                val isWord = id >= 0 && main.text(id).lowercase() == typo && main.logProb(id) >= Suggester.DEEP_TAIL
                if (isWord) continue                                  // it became another real word: nothing could repair it, and nothing should
                val r = Suggester.suggest(main, pm, keys, typo, p1, p2, 3)
                val list = r.words
                val tally = when (kind) { 0 -> tn; 1 -> tm; 2 -> td; else -> ts }
                tally.add(list.firstOrNull().equals(w, true), list.any { it.equals(w, true) })
                fix[3]++
                val a = r.autoCorrect
                if (a == null) fix[2]++ else if (a.equals(w, true)) fix[0]++ else { fix[1]++; if (verbose) notes.add("WRONG FIX: ${s.take(i).takeLast(2).joinToString(" ")} _ [$w] typed $typo -> $a  strip=$list") }
                if (verbose && list.none { it.equals(w, true) }) notes.add("typo miss: [$w] typed $typo ($kind) prev=$p1  got $list fix=$a")
            }
        }

        // searches: words typed lower case into the address box, no context beyond the line
        val sNext = Tally("search: next word"); val sComp = Tally("search: on the strip after 2 letters")
        var sSaved = 0; var sKeys = 0
        for (s in searches) for ((i, w) in s.withIndex()) {
            val (p1, p2) = contextOf(s, i)
            if (i > 0) { val rn = Suggester.suggest(main, pm, keys, "", p1, p2, 3); sNext.add(rn.words.firstOrNull().equals(w, true), rn.words.any { it.equals(w, true) }) }
            if (w.length > 2) { val st = strip(Suggester.suggest(main, pm, keys, w.take(2), p1, p2, 3)); sComp.add(st.firstOrNull().equals(w, true), st.any { it.equals(w, true) }) }
            var foundAt = -1
            for (k in 1 until w.length) { if (strip(Suggester.suggest(main, pm, keys, w.take(k), p1, p2, 3)).any { it.equals(w, true) }) { foundAt = k; break } }
            sKeys += w.length + 1
            if (foundAt >= 0) sSaved += w.length - foundAt
        }
        return Result(next, comp[0], comp[1], comp[2], shown[0], shown[1], shown[2], ksr, allKeys, saved, tn, tm, td, ts, fix, changed, rights, sNext, sComp, if (sKeys == 0) 0.0 else sSaved.toDouble() / sKeys, nextEmpty, nextShort, notes)
    }

    /**
     * Gliding running text: every word of the sentences is glided by the simulated thumb ([noise]) with the words before it as context, the way a person glides a message. Counts the first guess and the first three.
     * (GlideEval draws its contexts out of the list's own pairs, which proves nothing about pairs that were added from other text; this one uses sentences nobody trained on.)
     */
    fun glideInContext(lex: BinaryLexicon, keys: GlideKeys, sentences: List<List<String>>, noise: SimNoise, seed: Long = 5, maxSentences: Int = 600): Tally {
        val sim = GestureSim(keys, Random(seed))
        val dec = GlideDecoder(keys, GlideParams())
        val lexicons = listOf<GlideLexicon>(lex)
        val t = Tally("glide in a sentence, ${noise.name}")
        for (s in sentences.take(maxSentences)) for ((i, w) in s.withIndex()) {
            if (w.length < 2 || lex.idOf(w) < 0) continue
            val path = sim.path(w, noise) ?: continue
            val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, if (i >= 1) s[i - 1] else null, 3, if (i >= 2) s[i - 2] else null)
            val key = String(GlideLetters.keyOf(w))
            t.add(c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == key, c.any { String(GlideLetters.keyOf(it.text)) == key })
        }
        return t
    }

    class FixResult(val misN: Int, val fixed: Int, val wrong: Int, val none: Int, val inStrip: Int, val abbrChanged: List<String>, val abbrN: Int, val rareChanged: List<String>, val rareN: Int, val misDetail: List<String>)

    /** What a space does to words people really mistype, to shorthand, and to rare real words (docs/DICTIONARY.md): the autocorrect policy in numbers. */
    fun fixes(lex: BinaryLexicon, keys: GlideKeys): FixResult {
        var fixed = 0; var wrong = 0; var none = 0; var inStrip = 0
        val detail = ArrayList<String>()
        for ((typed, meant) in EvalCorpus.misspellings) {
            if (typed == meant) continue
            val r = Suggester.suggest(lex, null, keys, typed, null, null, 3)
            val a = r.autoCorrect
            if (a != null && a.equals(meant, true)) fixed++ else if (a != null) { wrong++; detail.add("WRONG $typed -> $a (meant $meant)") } else { none++; detail.add("none  $typed (meant $meant) strip=${r.words}") }
            if (r.words.any { it.equals(meant, true) } || (a != null && a.equals(meant, true))) inStrip++
        }
        val abbr = EvalCorpus.abbreviations.filter { Suggester.suggest(lex, null, keys, it, null, null, 3).autoCorrect.let { a -> a != null && !a.equals(it, true) } }
            .map { it + "->" + Suggester.suggest(lex, null, keys, it, null, null, 3).autoCorrect }
        val rare = EvalCorpus.rareWords.filter { Suggester.suggest(lex, null, keys, it, null, null, 3).autoCorrect.let { a -> a != null && !a.equals(it, true) } }
            .map { it + "->" + Suggester.suggest(lex, null, keys, it, null, null, 3).autoCorrect }
        return FixResult(fixed + wrong + none, fixed, wrong, none, inStrip, abbr, EvalCorpus.abbreviations.size, rare, EvalCorpus.rareWords.size, detail)
    }

    fun fixReport(f: FixResult, verbose: Boolean = false): String {
        val b = StringBuilder()
        b.appendLine("misspellings: put right by the space %d of %d (%.0f %%), a WRONG word %d, left alone %d; the meant word on the strip or put in: %d".format(f.fixed, f.misN, 100.0 * f.fixed / maxOf(1, f.misN), f.wrong, f.none, f.inStrip))
        b.appendLine("shorthand changed by a space: %d of %d %s".format(f.abbrChanged.size, f.abbrN, f.abbrChanged))
        b.appendLine("rare real words changed by a space: %d of %d %s".format(f.rareChanged.size, f.rareN, f.rareChanged))
        if (verbose) for (d in f.misDetail) b.appendLine("  " + d)
        return b.toString()
    }

    /** How often the strip offers a vulgar word UNPROMPTED: the three next-word guesses after each of the 5000 commonest words (the count, and the first 25 cases). */
    fun vulgarNextGuesses(lex: BinaryLexicon): Pair<Int, List<String>> {
        val ranked = (0 until lex.size).sortedByDescending { lex.logProb(it) }.take(5000)
        var hits = 0
        val shown = ArrayList<String>()
        for (id in ranked) {
            val prev = lex.text(id)
            val words = Suggester.nextWords(lex, null, prev, null, 3).words
            if (words.any { it.lowercase() in Suggester.VULGAR }) { hits++; if (shown.size < 25) shown.add("$prev -> $words") }
        }
        return hits to shown
    }

    /** One realistic slip: 0 a neighbouring key instead of an inner letter, 1 an inner letter left out, 2 an inner letter doubled, 3 two inner letters swapped. Null when the word cannot take that slip. */
    fun slip(w: String, kind: Int, keys: GlideKeys, rnd: Random): String? {
        val n = w.length
        fun near(a: Char): List<Char> = ('a'..'z').filter { c -> c != a && run { val dx = keys.cx[a - 'a'] - keys.cx[c - 'a']; val dy = keys.cy[a - 'a'] - keys.cy[c - 'a']; dx * dx + dy * dy <= 2.25f * keys.pitch * keys.pitch } }
        return when (kind) {
            0 -> { val i = 1 + rnd.nextInt(n - 1); val ns = near(w[i]); if (ns.isEmpty()) null else w.substring(0, i) + ns[rnd.nextInt(ns.size)] + w.substring(i + 1) }
            1 -> { val i = 1 + rnd.nextInt(n - 2); w.removeRange(i, i + 1) }
            2 -> { val i = 1 + rnd.nextInt(n - 2); w.substring(0, i + 1) + w[i] + w.substring(i + 1) }
            else -> { val i = 1 + rnd.nextInt(n - 2); if (i + 1 >= n || w[i] == w[i + 1]) null else w.substring(0, i) + w[i + 1] + w[i] + w.substring(i + 2) }
        }
    }

    fun report(r: Result): String {
        val b = StringBuilder()
        b.appendLine(r.next.toString())
        b.appendLine("next word: nothing offered %d times, fewer than three %d times (of %d)".format(r.nextEmpty, r.nextShort, r.next.n))
        for (t in listOf(r.comp1, r.comp2, r.comp3)) b.appendLine(t.toString())
        for (t in listOf(r.shown1, r.shown2, r.shown3)) b.appendLine(t.toString())
        b.appendLine("keystrokes saved by tapping the strip: %.1f %% (%d of %d)".format(100 * r.ksr, r.keysSaved, r.keysTyped))
        for (t in listOf(r.typoNeighbour, r.typoMissing, r.typoDouble, r.typoSwap)) b.appendLine(t.toString())
        val f = r.typoFixedBySpace
        b.appendLine("typos put right by the space: %d of %d (%.1f %%), wrong fix %d, no fix %d".format(f[0], f[3], 100.0 * f[0] / maxOf(1, f[3]), f[1], f[2]))
        b.appendLine("words typed RIGHT that a space would change: %d of %d (%.2f %%)".format(r.rightWordChanged, r.rightWords, 100.0 * r.rightWordChanged / maxOf(1, r.rightWords)))
        b.appendLine(r.searchNext.toString()); b.appendLine(r.searchComp2.toString())
        b.appendLine("search keystrokes saved: %.1f %%".format(100 * r.searchKsr))
        return b.toString()
    }

    @JvmStatic fun main(args: Array<String>) {
        val verbose = args.contains("verbose")
        val klx = args.firstOrNull { it.startsWith("klx=") }?.substring(4)           // klx=<file>: measure another compiled list than the shipped one
        val lex = if (klx != null) LexiconFile.read(java.io.File(klx).readBytes()) else GlideEval.loadLexicon()
        val keys = GlideEval.keysFor(564f, 160f)
        val probe = args.firstOrNull { it.startsWith("probe=") }?.substring(6)              // probe=helo,wierd or probe=helo@prev: what the strip would offer for these typed words (and the word before, after @)
        if (probe != null) {
            for (item in probe.split(',')) {
                val w = item.substringBefore('@'); val prev = item.substringAfter('@', "").ifEmpty { null }
                val id = lex.idOf(w)
                val r = Suggester.suggest(lex, null, keys, w, prev, null, 5)
                println("%-12s prev=%-8s inList=%-5s lp=%-6s typedKnown=%-5s autoCorrect=%-10s strip=%s".format(w, prev, id >= 0 && lex.text(id) == w, if (id >= 0) "%.1f".format(lex.logProb(id)) else "-", r.typedKnown, r.autoCorrect, r.words))
            }
            return
        }
        if (args.contains("profanity")) {
            val (hits, shown) = vulgarNextGuesses(lex)
            println("vulgar word among the three next-word guesses after the 5000 commonest words: $hits of 5000")
            for (x in shown) println("  $x")
            return
        }
        val corpus = args.firstOrNull { it.startsWith("corpus=") }?.substring(7)       // corpus=<file>: one sentence per line instead of the built-in everyday sentences (e.g. a held-out sample)
        val limit = args.firstOrNull { it.startsWith("limit=") }?.substring(6)?.toInt() ?: 5000
        val sentences = if (corpus != null) EvalCorpus.parse(java.io.File(corpus).readLines().take(limit).joinToString("\n")) else EvalCorpus.sentences
        val searches = if (corpus != null) emptyList() else EvalCorpus.searches
        val r = run(lex, null, keys, sentences, searches, verbose, withTypos = !args.contains("notypos"))
        println("sentences: ${sentences.size}, words: ${sentences.sumOf { it.size }}, searches: ${searches.size}")
        println(report(r))
        println(fixReport(fixes(lex, keys), verbose))
        if (args.contains("glide")) for (nz in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) println(glideInContext(lex, keys, sentences, nz, maxSentences = args.firstOrNull { it.startsWith("glides=") }?.substring(7)?.toInt() ?: 600).toString())
        if (verbose) for (n in r.notes) println(n)
    }
}
