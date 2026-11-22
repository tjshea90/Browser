package app.kestrel.core.input.glide

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * "The word predictions should get much better over time based on my patterns" (N6), measured: a person with their own words and phrases types a few hundred sentences; the keyboard is asked, before it
 * has learned anything and again after, for the next word (nothing typed) and for the word after its first letter. The hit rate must rise a lot. (Fixed seed; a small shipped list on a realistic scale.)
 */
class SuggesterLearningTest {
    private val common = listOf("the", "to", "and", "a", "of", "in", "is", "it", "you", "that", "for", "on", "with", "are", "this", "have", "not", "but", "we", "they", "at", "be", "was", "will", "can", "all", "so", "if", "my", "me", "good", "see", "go", "now", "out", "up", "get", "just", "time", "day", "morning", "night", "today", "tomorrow", "coming", "home", "work", "back", "later", "soon", "ready", "here", "there", "what", "when", "where", "why", "how")
    private val own = listOf("kestrel", "stables", "paddock", "farrier", "hay", "saddle", "bridle", "gallop", "trailer", "dressage")
    private fun lex(): BinaryLexicon {
        val rnd = Random(2)
        val e = ArrayList<LexiconFile.Entry>()
        for ((i, w) in common.withIndex()) e.add(LexiconFile.Entry(w, (60_000_000_000L / (i + 3))))
        for (w in own) e.add(LexiconFile.Entry(w, 1_000_000L + rnd.nextInt(1_000_000)))                       // all there, but rare: nobody types them all day
        e.add(LexiconFile.Entry("zzfiller", 300_000_000_000L))
        return LexiconFile.read(LexiconFile.build(e, emptyList()))
    }
    private val main = lex()
    private val keys = GlideEval.keysFor(564f, 160f)

    /** What THIS person writes: the same dozen phrases over and over, with a little variation. */
    private fun sentences(n: Int, seed: Long): List<List<String>> {
        val rnd = Random(seed)
        val phrases = listOf(
            "good morning kestrel is ready", "see you at the stables", "kestrel needs hay today", "the farrier is coming tomorrow", "ready for the paddock now",
            "saddle the horse and go", "trailer is back at the stables", "dressage later today", "gallop in the paddock soon", "bridle and saddle are here",
        ).map { it.split(" ") }
        return List(n) { val p = phrases[rnd.nextInt(phrases.size)]; if (rnd.nextInt(4) == 0) p + listOf(common[rnd.nextInt(common.size)]) else p }
    }

    private class Score(var nextHits: Int = 0, var nextTotal: Int = 0, var compHits: Int = 0, var compTotal: Int = 0) {
        val next get() = nextHits.toFloat() / nextTotal; val comp get() = compHits.toFloat() / compTotal
    }

    private fun measure(pm: PersonalModel?, test: List<List<String>>): Score {
        val sc = Score()
        for (s in test) for ((i, w) in s.withIndex()) {
            if (i == 0) continue
            val p1 = s[i - 1]; val p2 = if (i >= 2) s[i - 2] else null
            sc.nextTotal++; if (Suggester.suggest(main, pm, keys, "", p1, p2, 3).words.any { it.equals(w, true) }) sc.nextHits++
            if (w.length >= 4) { sc.compTotal++; if (Suggester.suggest(main, pm, keys, w.take(1), p1, p2, 3).words.any { it.equals(w, true) }) sc.compHits++ }
        }
        return sc
    }

    @Test fun predictionsGetMuchBetterAsTheKeyboardLearnsThePerson() {
        val test = sentences(60, 99)
        val before = measure(null, test)
        val pm = PersonalModel()
        for (s in sentences(300, 1)) for ((i, w) in s.withIndex()) pm.add(s.getOrNull(i - 1), w, 1f, s.getOrNull(i - 2))
        val after = measure(pm, test)
        println("next word top-3: %.0f %% -> %.0f %%; completion after the first letter: %.0f %% -> %.0f %%".format(100 * before.next, 100 * after.next, 100 * before.comp, 100 * after.comp))
        assertTrue("next word: ${before.next} -> ${after.next}", after.next >= before.next + 0.35f && after.next >= 0.7f)
        assertTrue("completions: ${before.comp} -> ${after.comp}", after.comp >= before.comp + 0.15f && after.comp >= 0.8f)
    }

    @Test fun aFewSentencesAlreadyHelpAndTheKeyboardKeepsGettingBetter() {
        val test = sentences(60, 99)
        val rates = ArrayList<Float>()
        val pm = PersonalModel()
        var fed = 0
        for (n in listOf(5, 30, 150)) {
            for (s in sentences(n, 1).drop(fed)) for ((i, w) in s.withIndex()) pm.add(s.getOrNull(i - 1), w, 1f, s.getOrNull(i - 2))
            fed = n
            rates.add(measure(pm, test).next)
        }
        println("next word top-3 after 5 / 30 / 150 sentences: " + rates.joinToString(" / ") { "%.0f %%".format(100 * it) })
        assertTrue("never worse as it learns: $rates", rates[1] >= rates[0] - 0.02f && rates[2] >= rates[1] - 0.02f && rates[2] > rates[0])
    }

    @Test fun aSlipThePersonKeepsMakingIsFixedFirstOnceItWasCorrected() {
        val pm = PersonalModel(); for (s in sentences(60, 1)) for ((i, w) in s.withIndex()) pm.add(s.getOrNull(i - 1), w, 1f, s.getOrNull(i - 2))
        repeat(4) { pm.addConfusion("stabels", "stables", 2f) }
        val r = Suggester.suggest(main, pm, keys, "stabels", "at", "the", 3)
        assertTrue("stables first: ${r.words}", r.words.first() == "stables")
    }
}
