package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.Random

/** The export file (AX1, owner message 38: "save this learned data in an exportable format ... I want to import what it learned"). The format is written down in docs/KEYBOARD-EXPORT.md and pinned here. */
class LearnedExportTest {
    private val keys = GlideEval.keysFor()

    private fun printOf(word: String, seed: Long = 1): FloatArray = GestureSim(keys, Random(seed)).path(word, SimNoise.NORMAL)!!.let { Fingerprint.of(it.xs, it.ys, it.xs.size, keys)!! }

    private fun learned(): PersonalModel {
        val pm = PersonalModel()
        pm.add("so", "hello", 2f, "oh"); pm.add("hello", "world", 3f, "so"); pm.add(null, "for", 5f)
        pm.addConfusion("fir", "for", 4f)
        pm.patterns.add("for", printOf("for"), 3f); pm.patterns.add("hello", printOf("hello"), 1f)
        return pm
    }

    private fun own(): UserWords = UserWords().also { it.add("doraby", 3); it.add("kestrel", 8) }

    @Test fun whatWasLearnedComesBackFromAnExport() {
        val pm = learned()
        val text = LearnedExport.write(pm, floatArrayOf(0.12f, -0.05f), own())
        val back = LearnedExport.read(text)!!
        assertEquals(pm.wordCount, back.model.wordCount); assertEquals(pm.pairCount, back.model.pairCount); assertEquals(pm.tripleCount, back.model.tripleCount)
        assertEquals(pm.confusionCount, back.model.confusionCount)
        assertEquals(2, back.model.patterns.patternCount)
        assertEquals(5f, back.model.weightOf("for"), 0.01f)
        assertEquals(4f, back.model.confusionWeight("fir", "for"), 0.01f)
        assertEquals(0.12f, back.habit!![0], 0.001f); assertEquals(-0.05f, back.habit!![1], 0.001f)
        assertEquals(setOf("doraby" to 3, "kestrel" to 8), back.ownWords.toSet())
        assertTrue(back.summary.complete)
        val d = back.summary.describe()
        assertTrue(d, d.contains("words") && d.contains("glide shapes") && d.contains("2 own words") && d.contains("thumb habit") && !d.contains("cut off"))
    }

    @Test fun theFileStartsWithItsNameAndVersionAndEndsWithTheNumberOfEntries() {
        val text = LearnedExport.write(learned(), floatArrayOf(0f, 0f), own())
        val lines = text.lines().filter { it.isNotEmpty() }
        assertEquals("KESTREL-KEYBOARD-LEARNING 1", lines.first())
        val entries = lines.count { !it.startsWith("#") && !it.startsWith("KESTREL-") && !it.startsWith("end ") }
        assertEquals("end $entries", lines.last())
        assertTrue(lines[1].startsWith("#") && lines[2].startsWith("#")) // a reader who opens the file sees what it is
        for (l in lines) assertTrue("only these kinds of line: $l", l.startsWith("#") || l.split(' ')[0] in setOf("KESTREL-KEYBOARD-LEARNING", "habit", "own", "w", "p", "t", "c", "r", "s", "end"))
    }

    @Test fun itIsPlainAsciiWithDotsWhateverLanguageTheTabletIsIn() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY) // a comma for a decimal point
            val text = LearnedExport.write(learned(), floatArrayOf(0.125f, -0.5f), own())
            assertTrue("ASCII only", text.all { it.code < 128 })
            assertFalse("no decimal commas: " + Regex("[0-9],[0-9]").find(text)?.value, Regex("[0-9],[0-9]").containsMatchIn(text))
            val back = LearnedExport.read(text)!!
            assertEquals(5f, back.model.weightOf("for"), 0.01f)
            assertEquals(0.125f, back.habit!![0], 0.001f)
            // and the file the keyboard keeps for itself reads back too
            assertEquals(5f, PersonalModel.parse(learned().serialize()).weightOf("for"), 0.01f)
        } finally { Locale.setDefault(saved) }
    }

    @Test fun whatIsNotAnExportOfThisKeyboardIsRefused() {
        assertNull(LearnedExport.read(""))
        assertNull(LearnedExport.read("hello world"))
        assertNull(LearnedExport.read("{\"backup\": 1}"))
        assertNull(LearnedExport.read("KESTREL-KEYBOARD-LEARNING\nw for 3.0\n"))      // no version
        assertNull(LearnedExport.read("KESTREL-KEYBOARD-LEARNING x\nw for 3.0\n"))    // a version that is no number
        assertNull(LearnedExport.read("KESTREL-KEYBOARD-LEARNING 0\nw for 3.0\n"))
        assertNull(LearnedExport.read("KESTREL-KEYBOARD-LEARNING 2\nw for 3.0\n"))    // a later version this one does not understand
        assertNull(LearnedExport.read("OTHER-KEYBOARD 1\nw for 3.0\n"))
        assertNull(LearnedExport.read("w for 3.0\nKESTREL-KEYBOARD-LEARNING 1\n"))   // the name must come first
        assertNull(LearnedExport.read("KESTREL-KEYBOARD-LEARNING 1\n" + "x".repeat(LearnedExport.MAX_CHARS)))
        assertNotNull(LearnedExport.read("\uFEFFKESTREL-KEYBOARD-LEARNING 1\nw for 3.0\nend 1\n")) // a text editor's byte order mark does not matter
    }

    @Test fun linesItDoesNotKnowAreSkippedAndACutOffFileIsStillUsedAndSaysSo() {
        val text = LearnedExport.write(learned(), null, null)
        val extra = text.replace("end ", "zz a line of a later version\nq 1 2 3\nend ")
        val back = LearnedExport.read(extra)!!
        assertEquals(5f, back.model.weightOf("for"), 0.01f); assertTrue(back.summary.complete)
        val cut = text.lines().filter { !it.startsWith("end ") }.joinToString("\n")
        val b2 = LearnedExport.read(cut)!!
        assertFalse(b2.summary.complete); assertEquals(5f, b2.model.weightOf("for"), 0.01f)
        assertTrue(b2.summary.describe().contains("cut off"))
        // a count that does not match the lines: not complete either
        assertFalse(LearnedExport.read(text.replace(Regex("end \\d+"), "end 9999"))!!.summary.complete)
    }

    @Test fun numbersMustBeFinitePositiveAndNotAbsurd() {
        val text = listOf("KESTREL-KEYBOARD-LEARNING 1", "w nan NaN", "w inf Infinity", "w neg -3", "w big 1e30", "w zero 0", "w fine 2", "p fine inf NaN", "habit NaN 0.1", "habit 9 -9", "own bad -4", "own okay 5").joinToString("\n")
        val back = LearnedExport.read(text)!!
        assertEquals(1, back.model.wordCount); assertEquals(2f, back.model.weightOf("fine"), 0.001f)
        assertEquals(0, back.model.pairCount)
        assertEquals(GlideDecoder.MAX_BIAS, back.habit!![0], 1e-6f); assertEquals(-GlideDecoder.MAX_BIAS, back.habit!![1], 1e-6f) // clamped to what the decoder ever uses
        assertEquals(listOf("okay" to 5), back.ownWords)
    }

    @Test fun aHostileFileCannotFillTheTablesPastTheirCaps() {
        val sb = StringBuilder("KESTREL-KEYBOARD-LEARNING 1\n")
        val rnd = Random(3)
        fun w(): String { val n = 3 + rnd.nextInt(5); return String(CharArray(n) { 'a' + rnd.nextInt(26) }) }
        for (i in 0 until 40000) { sb.append("w ").append(w()).append(" 2\n"); sb.append("p ").append(w()).append(' ').append(w()).append(" 2\n"); sb.append("c ").append(w()).append(' ').append(w()).append(" 2\n"); sb.append("t ").append(w()).append(' ').append(w()).append(' ').append(w()).append(" 2\n") }
        for (i in 0 until 3000) sb.append("own ").append(w()).append(" 2\n")
        val back = LearnedExport.read(sb.toString())
        assertNotNull(back) // 40000 x 4 lines is about 2 MB: within the file limit, so it is read, but only as far as the tables go
        assertTrue("words ${back!!.model.wordCount}", back.model.wordCount <= 4000)
        assertTrue("pairs ${back.model.pairCount}", back.model.pairCount <= 8000)
        assertTrue("triples ${back.model.tripleCount}", back.model.tripleCount <= 6000)
        assertTrue("corrections ${back.model.confusionCount}", back.model.confusionCount <= 3000)
        assertTrue("own ${back.ownWords.size}", back.ownWords.size <= LearnedExport.MAX_OWN)
    }

    @Test fun bringingTheSameFileInTwiceChangesNothingAndAFileOnlyAddsWhatIsNew() {
        val mine = PersonalModel(); mine.add(null, "for", 9f); mine.add(null, "only", 4f)
        val file = LearnedExport.read(LearnedExport.write(learned(), null, null))!!
        mine.mergeFrom(file.model); val once = mine.serialize()
        mine.mergeFrom(file.model)
        assertEquals(once, mine.serialize())
        assertEquals(9f, mine.weightOf("for"), 0.01f)          // what was learned here stays when it is larger
        assertEquals(2f, mine.weightOf("hello"), 0.01f)         // what only the file knew comes in
        assertEquals(4f, mine.weightOf("only"), 0.01f)
        assertEquals(2, mine.patterns.patternCount)
    }

    @Test fun theExampleInTheFormatDocumentReadsAsDescribed() {
        val doc = listOf("docs/KEYBOARD-EXPORT.md", "../docs/KEYBOARD-EXPORT.md").map { File(it) }.firstOrNull { it.exists() } ?: throw IllegalStateException("docs/KEYBOARD-EXPORT.md not found (run from the repository root or core/)")
        val md = doc.readText()
        val start = md.indexOf("```text kestrel-export-example\n"); val end = md.indexOf("```", start + 10)
        assertTrue("the document has an example block", start >= 0 && end > start)
        val example = md.substring(start + "```text kestrel-export-example\n".length, end)
        val back = LearnedExport.read(example)
        assertNotNull("the document's example is a valid export", back)
        assertTrue(back!!.summary.complete)
        assertTrue(back.model.weightOf("for") > 0f && back.model.patterns.patternCount == 1 && back.habit != null && back.ownWords.isNotEmpty() && back.model.confusionCount == 1 && back.model.pairCount == 1 && back.model.tripleCount == 1)
    }
}
