package app.kestrel.core.input.glide

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KB-406: how well the strip predicts while the person TAPS, on the shipped list with no personal model (the day the keyboard is installed). The floors are the numbers of 2026-11-17 (list with the
 * casual pairs and the shorthand words) minus a margin: they catch a change that makes prediction or the automatic fix worse, they are not targets. Every run prints the whole table
 * (Gradle: `--tests '*PredictionYardstickTest' -i`); the history of the numbers is research/LEARNING_RESULTS.md.
 */
class PredictionYardstickTest {
    private val lex = GlideEval.loadLexicon()
    private val keys = GlideEval.keysFor(564f, 160f)

    @Test fun theStripPredictsEverydayEnglish() {
        val r = PredictionEval.run(lex, null, keys, EvalCorpus.sentences, EvalCorpus.searches, verbose = System.getProperty("eval.verbose") != null)
        println("sentences ${EvalCorpus.sentences.size}, words ${EvalCorpus.sentences.sumOf { it.size }}, searches ${EvalCorpus.searches.size}")
        println(PredictionEval.report(r))
        for (n in r.notes) println(n)
        assertTrue("a word typed right is never changed by a space: ${r.rightWordChanged} of ${r.rightWords}", r.rightWordChanged == 0)
        assertTrue("next word in the three places: ${r.next}", r.next.r3 >= 0.26)
        assertTrue("on the strip after two letters: ${r.shown2}", r.shown2.r3 >= 0.72)
        assertTrue("keystrokes saved: ${r.ksr}", r.ksr >= 0.49)
        val f = r.typoFixedBySpace
        assertTrue("typos put right by the space: ${f[0]} of ${f[3]}", f[0].toDouble() / f[3] >= 0.91)
        assertTrue("a neighbouring key / two letters swapped come first: ${r.typoNeighbour} ${r.typoSwap}", r.typoNeighbour.r1 >= 0.95 && r.typoSwap.r1 >= 0.95)
    }

    @Test fun theStripNeverOffersAVulgarWordOnItsOwn() {
        val (hits, shown) = PredictionEval.vulgarNextGuesses(lex)
        assertTrue("a vulgar word among the three next-word guesses after the 5000 commonest words: $hits $shown", hits == 0)
    }

    @Test fun theAutomaticFixHelpsWithRealMisspellingsAndLeavesShorthandAndRareWordsAlone() {
        val f = PredictionEval.fixes(lex, keys)
        println(PredictionEval.fixReport(f, verbose = true))
        assertTrue("misspellings put right: ${f.fixed} of ${f.misN}", f.fixed.toDouble() / f.misN >= 0.80)
        assertTrue("a wrong word put in: ${f.wrong}", f.wrong <= 5)
        assertTrue("the meant word is on the strip or put in: ${f.inStrip} of ${f.misN}", f.inStrip.toDouble() / f.misN >= 0.90)
        assertTrue("shorthand changed by a space: ${f.abbrChanged}", f.abbrChanged.size <= 2)
        assertTrue("rare real words changed by a space: ${f.rareChanged}", f.rareChanged.size <= 3)
    }
}
