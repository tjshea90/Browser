package app.kestrel.core.input.glide

import app.kestrel.core.input.MiniKeyboardGeometry
import app.kestrel.core.input.MiniKeyboardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/** The glide engine's parts: key layout, the person's own words, the counters, and the engine run on a direct executor (AT1e-g). */
class GlideSupportTest {
    private val direct = Executor { it.run() }
    private val keys = GlideEval.keysFor()

    @Test fun theLayoutFunctionGivesEveryKeyABoxInsideTheViewAndKeepsTheStripFree() {
        val rows = MiniKeyboardLayout.rows(false)
        val boxes = MiniKeyboardGeometry.boxes(rows, 292f, 191f, 3f, top = 30f)
        assertEquals(rows.size, boxes.size)
        for ((i, row) in boxes.withIndex()) { assertEquals(rows[i].size, row.size); for (b in row) { assertTrue(b.left >= 0f && b.right <= 292.01f && b.top >= 30f - 0.01f && b.bottom <= 191.01f && b.width > 0f && b.height > 0f) } }
        assertEquals(30f, boxes[0][0].top, 0.01f)
        // q and p sit at the two ends of the top row, a letter is about as wide as the pitch minus the gap
        assertTrue(boxes[0][0].left < 0.5f && boxes[0][9].right > 291.5f)
        val k = GlideKeys.from(rows, boxes)
        assertTrue("pitch ${k.pitch}", k.pitch in 27f..31f)
        assertTrue(k.cx['q' - 'a'] < k.cx['w' - 'a'] && k.cy['q' - 'a'] < k.cy['a' - 'a'] && k.cy['a' - 'a'] < k.cy['z' - 'a'])
    }

    @Test fun ownWordsAreWordsAFingerCanDraw() {
        assertTrue(UserWords.glideable("doraby")); assertTrue(UserWords.glideable("kestrel"))
        for (bad in listOf("ab", "aaa", "doraby9", "Do", "pelis juan", "x".repeat(30), "")) assertTrue("'$bad'", !UserWords.glideable(bad))
        val u = UserWords(cap = 3)
        u.add("alpha"); u.add("alpha"); u.add("bravo"); u.add("charlie"); u.add("delta") // over the cap: the least used goes
        assertEquals(3, u.size)
        assertTrue(u.contains("alpha")) // used twice: stays
        val round = UserWords.parse(u.serialize())
        assertEquals(u.size, round.size); assertTrue(round.contains("alpha"))
    }

    @Test fun aSiteNameBecomesAWord() {
        assertEquals("doraby", UserWords.wordOfHost("www.doraby.com"))
        assertEquals("bbc", UserWords.wordOfHost("news.bbc.co.uk"))
        assertEquals("pelisjuanita", UserWords.wordOfHost("pelisjuanita.com"))
        assertNull(UserWords.wordOfHost("localhost")); assertNull(UserWords.wordOfHost("10.0.2.2")); assertNull(UserWords.wordOfHost("a.com"))
    }

    @Test fun ownWordsAreWeighedOnTheScaleOfTheShippedList() {
        val u = UserWords(); u.add("doraby"); for (i in 0 until 20) u.add("kestrel")
        val lex = u.lexicon(5.4e11)
        val once = lex.logProb(lex.idOf("doraby")); val often = lex.logProb(lex.idOf("kestrel"))
        assertTrue("a word used often outranks one seen once", often > once)
        assertTrue("on the shipped scale a word seen once is an ordinary word, not the most common one ($once)", once < -8f && once > -13f)
    }

    @Test fun theCountersTellHowTheGlidesWent() {
        val s = GlideStats()
        assertTrue(s.line().contains("not used yet"))
        s.onLoaded(140, 82971)
        s.onDecoded(8, true); s.onKept()
        s.onDecoded(12, true); s.onPicked()
        s.onDecoded(30, true); s.onUndone()
        s.onDecoded(5, false)
        s.onKept() // nothing is pending after a miss: not counted
        s.onHabit(2.1f, -1.4f); s.onOwnWords(12)
        val l = s.line()
        assertTrue(l, l.contains("4 glides") && l.contains("best word kept 1 (33 %)") && l.contains("another candidate picked 1") && l.contains("deleted right away 1") && l.contains("no word fitted 1"))
        assertTrue(l, l.contains("decode avg 13 ms, worst 30 ms") && l.contains("82971 words ready after 140 ms") && l.contains("x 2.1 y -1.4") && l.contains("own words 12"))
    }

    @Test fun theEngineLoadsDecodesAndLearnsOnItsExecutor() {
        val bytes = LexiconFile.build(
            listOf("hello" to 500L, "help" to 800L, "world" to 900L, "word" to 1200L, "the" to 9000L, "she" to 2000L, "good" to 2000L).map { LexiconFile.Entry(it.first, it.second) },
            emptyList(),
        )
        val g = GlideTyping({ bytes }, direct)
        g.prepare(keys, null, null)
        assertTrue(g.ready)
        val sim = GestureSim(keys, Random(3))
        var got: GlideResult? = null
        val p = sim.path("hello", SimNoise.CALM)!!
        g.decode(p.xs, p.ys, p.xs.size, null) { got = it }
        assertEquals("hello", got!!.candidates.first().text)
        var habit: FloatArray? = null
        g.accepted("hello") { habit = it }
        assertNotNull(habit); assertEquals(2, habit!!.size)
        assertTrue(g.stats.line().contains("1 glides"))
    }

    @Test fun ownWordsCanBeGlidedAfterTheEngineKnowsThem() {
        val bytes = LexiconFile.build(listOf("hello" to 500L, "help" to 800L, "world" to 900L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val g = GlideTyping({ bytes }, direct)
        val own = UserWords(); own.add("kestrel")
        g.prepare(keys, null, own)
        val sim = GestureSim(keys, Random(9))
        val p = sim.path("kestrel", SimNoise.CALM)!!
        var got: GlideResult? = null
        g.decode(p.xs, p.ys, p.xs.size, null) { got = it }
        assertEquals("kestrel", got!!.candidates.firstOrNull()?.text)
    }

    @Test fun beforeThePrepareOrWithoutAListAGlideGivesNothingInsteadOfFailing() {
        val g = GlideTyping({ null }, direct)
        var got: GlideResult? = null
        g.decode(floatArrayOf(1f, 50f), floatArrayOf(1f, 50f), 2, null) { got = it }
        assertTrue(got!!.candidates.isEmpty())
        g.prepare(keys, null, null)
        assertTrue(!g.ready)
    }
}
