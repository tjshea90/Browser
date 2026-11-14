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

    @Test fun aLivePreviewGuessesFromThePathSoFarWithoutCountingAGlideOrDisturbingTheRealDecode() {
        val bytes = LexiconFile.build(listOf("the" to 530_000_000_000L, "hello" to 500_000_000L, "help" to 400_000_000L, "world" to 900_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val g = GlideTyping({ bytes }, direct, previewExecutor = direct)
        g.prepare(keys, null, null)
        val sim = GestureSim(keys, Random(5))
        val p = sim.path("hello", SimNoise.CALM)!!
        var previewed: List<String> = emptyList()
        g.preview(p.xs, p.ys, p.xs.size * 3 / 4, null) { previewed = it.candidates.map { c -> c.text } } // three quarters of the path: the finger is still going
        assertTrue("a guess from the path so far: $previewed", previewed.isNotEmpty())
        assertTrue("a preview is not a glide: ${g.stats.line()}", !g.stats.line().contains("glides") && g.stats.line().contains("live preview 1 times"))
        var real: List<String> = emptyList()
        g.decode(p.xs, p.ys, p.xs.size, null) { real = it.candidates.map { c -> c.text } }
        assertEquals("the whole path still gives hello first", "hello", real[0])
        var moreThanOne = 0
        repeat(5) { g.preview(p.xs, p.ys, p.xs.size / 2, null) { moreThanOne++ } }
        assertEquals("on a direct executor every call runs to its end before the next", 5, moreThanOne)
    }

    @Test fun onlyOneLivePreviewRunsAtATimeTheRestAreDropped() {
        val bytes = LexiconFile.build(listOf("the" to 530_000_000_000L, "hello" to 500_000_000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val queued = ArrayList<Runnable>()
        val slow = Executor { queued.add(it) }
        val g = GlideTyping({ bytes }, direct, previewExecutor = slow)
        g.prepare(keys, null, null)
        val p = GestureSim(keys, Random(5)).path("hello", SimNoise.CALM)!!
        var done = 0
        g.preview(p.xs, p.ys, p.xs.size, null) { done++ }
        g.preview(p.xs, p.ys, p.xs.size, null) { done++ } // the first has not run yet: dropped
        g.preview(p.xs, p.ys, p.xs.size, null) { done++ }
        assertEquals(1, queued.size)
        queued[0].run()
        assertEquals(1, done)
        g.preview(p.xs, p.ys, p.xs.size, null) { done++ } // free again
        assertEquals(2, queued.size)
    }

    // ------------------------------------------------------------------ AX1: the shape of a glide is learned from what the person does next

    private fun shapeEngine(): GlideTyping {
        val bytes = LexiconFile.build(
            listOf("hello" to 500L, "help" to 800L, "held" to 300L, "hell" to 300L, "world" to 900L, "word" to 1200L, "the" to 9000L, "she" to 2000L, "good" to 2000L, "for" to 8000L, "fur" to 600L).map { LexiconFile.Entry(it.first, it.second) },
            emptyList(),
        )
        return GlideTyping({ bytes }, direct).also { it.prepare(keys, null, null) }
    }

    private fun glideOf(g: GlideTyping, word: String, seed: Long = 3): List<GlideCandidate> {
        val p = GestureSim(keys, Random(seed)).path(word, SimNoise.CALM)!!
        var out: List<GlideCandidate> = emptyList()
        g.decode(p.xs, p.ys, p.xs.size, null) { out = it.candidates }
        return out
    }

    private fun exported(g: GlideTyping): String { var t = ""; g.exportText(null, null) { t = it }; return t }

    @Test fun aKeptGlideLeavesItsShapeAndForgetRemovesIt() {
        val g = shapeEngine()
        val c = glideOf(g, "world")
        assertEquals("world", c.first().text)
        g.learn(null, "world", 1f, fromGlide = true)
        val text = exported(g)
        assertTrue(text, text.lines().any { it.startsWith("s world 1.00 ") })
        assertTrue(g.stats.line(), g.stats.line().contains("1 glide shapes of 1 words remembered"))
        g.forgetAll()
        assertFalse(exported(g).lines().any { it.startsWith("s ") })
        assertFalse(g.stats.line().contains("glide shapes"))
    }

    @Test fun aWordTypedLetterByLetterHasNoShape() {
        val g = shapeEngine()
        glideOf(g, "world")
        g.learn(null, "good", 1f) // typed, not glided (fromGlide stays false): the last glide's shape does not belong to it
        assertFalse(exported(g).lines().any { it.startsWith("s ") })
    }

    @Test fun aDeletedGlideAndTheWordTypedInItsPlaceKeepTheShapeOnlyWhenItCouldHaveMeantIt() {
        val g = shapeEngine()
        glideOf(g, "world"); g.glideDeleted("word")
        g.retyped("world")
        assertTrue(exported(g).lines().any { it.startsWith("s world 3.00 ") })
        // another word that this path never could have drawn: nothing is remembered for it
        glideOf(g, "she"); g.glideDeleted("the")
        g.retyped("good")
        assertFalse(exported(g).lines().any { it.startsWith("s good ") })
        // and a word typed without a deleted glide before it has no shape to take
        g.retyped("hello")
        assertFalse(exported(g).lines().any { it.startsWith("s hello ") })
    }

    @Test fun aCorrectionMakesWhatWasGuessedLessSureForTheSameShape() {
        val g = shapeEngine()
        val first = glideOf(g, "word", 7).first().text
        g.learn(null, first, 1f, fromGlide = true) // kept: its shape is remembered
        assertTrue(exported(g).lines().any { it.startsWith("s $first ") })
        glideOf(g, "word", 7)                       // the same path again, and now the person says it was another word
        g.learn(null, "world", 3f, guessed = first, guessedWeight = 1f, fromGlide = true)
        val lines = exported(g).lines()
        assertTrue(lines.any { it.startsWith("s world 3.00 ") })
        assertFalse("the guessed word's shape for that glide is gone: $lines", lines.any { it.startsWith("s $first ") })
    }

    @Test fun anExportFileIsTakenInAndTheThumbHabitOfThisKeyboardStays() {
        val a = shapeEngine(); glideOf(a, "world"); a.learn(null, "world", 3f, fromGlide = true)
        var file = ""; a.exportText(floatArrayOf(0.2f, 0.1f), UserWords().also { it.add("doraby", 2) }) { file = it }
        val b = shapeEngine()
        val own = UserWords(); var summary: LearnedExport.Summary? = null; var habit: FloatArray? = null
        b.importText(file, false, own, { habit = it }) { summary = it }
        assertNotNull(summary); assertTrue(own.contains("doraby"))
        assertEquals(0.2f, habit!![0], 0.001f) // this keyboard had no habit of its own yet
        assertTrue(exported(b).lines().any { it.startsWith("s world ") })
        // a second keyboard that already has a habit keeps it
        var habit2: FloatArray? = null
        b.importText(file, false, own, { habit2 = it }) { }
        assertNull(habit2)
        // something that is not an export is refused and changes nothing
        var bad: LearnedExport.Summary? = summary
        b.importText("not an export", false, own, { }) { bad = it }
        assertNull(bad)
    }

    @Test fun replacingStartsFromTheFile() {
        val a = shapeEngine(); a.learn(null, "hello", 5f)
        var file = ""; a.exportText(null, null) { file = it }
        val b = shapeEngine(); b.learn(null, "good", 7f)
        b.importText(file, true, UserWords(), { }) { }
        val t = exported(b)
        assertTrue(t.lines().any { it.startsWith("w hello ") }); assertFalse(t.lines().any { it.startsWith("w good ") })
    }
}
