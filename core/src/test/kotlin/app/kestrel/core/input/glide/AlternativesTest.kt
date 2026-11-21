package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** R18a: the words offered in place of a word that is already in the text, on the shipped list: close glide patterns, ranked by the words around. Run with `-i` for the table. */
class AlternativesTest {
    private val lex = GlideEval.loadLexicon()
    private val keys = GlideEval.keysFor(564f, 160f)
    private val decoder = GlideDecoder(keys)

    private class Case(val typed: String, val meant: String, val prev2: String?, val prev: String?, val next: String?)
    private fun c(typed: String, meant: String, ctx: String, next: String? = null): Case { val p = ctx.split(' ').filter { it.isNotEmpty() }; return Case(typed, meant, p.getOrNull(p.size - 2), p.lastOrNull(), next) }

    private val cases = listOf(
        c("teh", "the", "went to", "store"), c("adn", "and", "you", "me"), c("hte", "the", "in", "house"), c("thier", "their", "lost", "keys"), c("recieve", "receive", "to", "mail"),
        c("wierd", "weird", "so", "right"), c("helo", "hello", "said", "there"), c("wrold", "world", "the", "is"), c("frm", "from", "email", "john"), c("form", "from", "an email", "john"),
        c("quite", "quiet", "please be", "please"), c("there", "their", "lost", "keys"), c("than", "then", "and", "we"), c("hear", "here", "come", "now"), c("thing", "think", "i", "so"),
        c("gello", "hello", "said", "there"), c("jappy", "happy", "so", "to"), c("wprk", "work", "hard", "today"), c("thw", "the", "to", "store"), c("meetinf", "meeting", "the", "tomorrow"),
        c("tomorow", "tomorrow", "see you", "morning"), c("definately", "definitely", "i will", "be there"), c("seperate", "separate", "two", "rooms"), c("untill", "until", "wait", "friday"),
        c("wich", "which", "one", "is"), c("becuase", "because", "late", "i"), c("goood", "good", "very", "morning"), c("pleese", "please", "yes", "help"), c("thnaks", "thanks", "many", "again"),
        c("wokr", "work", "hard", "today"), c("yuo", "you", "thank", "so"), c("rigth", "right", "all", "now"), c("firend", "friend", "my", "is"), c("beleive", "believe", "i", "you"),
        c("hapy", "happy", "so", "birthday"), c("tuesdya", "tuesday", "next", "morning"), c("whta", "what", "see", "you"), c("woudl", "would", "you", "like"), c("shoud", "should", "we", "go"),
        c("freind", "friend", "best", "from"),
    )

    private fun run(pm: PersonalModel? = null, limit: Int = 3): List<Pair<Case, List<String>>> = cases.map { it to Alternatives.find(lex, null, pm, decoder, keys, it.typed, it.prev2, it.prev, it.next, limit) }

    @Test fun theMeantWordIsAmongTheFirstThreeMostOfTheTime() {
        val r = run()
        var first = 0; var three = 0
        for ((k, got) in r) {
            val i = got.indexOf(k.meant); if (i == 0) first++; if (i in 0..2) three++
            println("%-11s -> %-12s %s %s".format(k.typed, k.meant, if (i in 0..2) "OK(${i + 1})" else "MISS", got))
        }
        println("first $first of ${cases.size}, in the first three $three of ${cases.size}")
        assertTrue("the meant word in the first three: $three of ${cases.size}", three >= cases.size * 0.90)
        assertTrue("the meant word first: $first of ${cases.size}", first >= cases.size * 0.85)
    }

    @Test fun theWordItselfIsNeverOfferedAndNothingIsOfferedTwice() {
        for ((k, got) in run(limit = 6)) { assertFalse("${k.typed} offered itself: $got", k.typed in got); assertEquals("${k.typed}: $got", got.size, got.toSet().size) }
    }

    @Test fun theOffersLookLikeTheWordWhenGlided() {
        // every offer is a close glide pattern of the word (a shape within about three keys: two swapped letters far apart on the keyboard are the furthest a typo fix goes), whatever the language says
        for ((k, got) in run()) {
            val a = Fingerprint.ofWord(k.typed, keys) ?: continue
            for (w in got) { val b = Fingerprint.ofWord(w, keys) ?: continue; assertTrue("${k.typed} -> $w is ${Fingerprint.distance(a, b)} keys away", Fingerprint.distance(a, b) <= 3.2f) }
        }
    }

    @Test fun theWordAfterBreaksATie() {
        // "form" or "from" in front of "john"?  An email comes FROM John; the pair (from, john) is far likelier than (form, john)
        val r = Alternatives.find(lex, null, null, decoder, keys, "form", "an", "email", "john", 3)
        assertEquals("from", r.first())
        // the same word after "the" is not a typo of anything that comes before "john"
        val r2 = Alternatives.find(lex, null, null, decoder, keys, "tehy", "if", "i", "go", 3)
        assertTrue("they among $r2", "they" in r2)
    }

    @Test fun whatThePersonCorrectedBeforeComesFirstAgain() {
        val pm = PersonalModel(); repeat(3) { pm.addConfusion("wich", "witch", 1f) }
        val plain = Alternatives.find(lex, null, null, decoder, keys, "wich", "the", "old", "lived", 3)
        val learned = Alternatives.find(lex, null, pm, decoder, keys, "wich", "the", "old", "lived", 3)
        assertTrue("a learned correction is on the strip: $learned (was $plain)", "witch" in learned)
    }

    @Test fun aBlockedWordIsNeverOffered() {
        val r = Alternatives.find(lex, null, null, decoder, keys, "teh", "went", "to", "store", 5, blocked = { it == "the" })
        assertFalse(r.toString(), "the" in r)
    }

    @Test fun aWordWithoutLettersOrWithOneKeyHasNoOffers() {
        assertTrue(Alternatives.find(lex, null, null, decoder, keys, "a", null, null, null, 3).isEmpty())
        assertTrue(Alternatives.find(lex, null, null, decoder, keys, "12ab", null, null, null, 3).isEmpty())
        assertTrue(Alternatives.find(lex, null, null, decoder, keys, "x1", null, null, null, 3).isEmpty())
    }
}
