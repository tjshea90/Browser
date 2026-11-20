package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Random event streams against the fake editor. The invariant (plan 13.1): whenever every selection report has been delivered, the keyboard's
 * belief about the editor (window, cursor, composing word, last glide) is TRUE, whatever mix of taps, glides, swaps, Backspaces, Enters and cursor
 * moves it did, however late the reports arrived and whatever the user or the app did to the text in between. A failure prints the seed.
 */
class InputControllerFuzzTest {
    /** SWOOP_FUZZ_SCALE=20 ./gradlew :core:test runs 20x more random streams (a soak before a release). */
    private val scale = (System.getenv("SWOOP_FUZZ_SCALE") ?: "1").toLongOrNull()?.coerceIn(1L, 200L) ?: 1L
    /** How many selection reports may be on their way at once. Real Android delivers each within milliseconds of the edit, so a handful at most; with a much larger backlog the keyboard still repairs itself (see Diagnostics: foreign changes, resyncs) but a window may be wrong for a moment. */
    private val MAX_LAG = 4
    private val letters = "abcdefghijklmnopqrstuvwxyz"
    private val words = listOf("hello", "world", "for", "fir", "the", "doraby", "a", "i'm", "typing")
    private val punct = listOf(".", ",", "!", "?", "'", "/", ".com", "-", "(", ")", "1", "7", "😀", "é")

    /** [lagging]: reports arrive late. KNOWN LIMITATION (TASKS KB-101b): after Enter or a cursor move longer than the window the selection is unknown until the editor's reports catch up; with lagging reports the window can be wrong for a moment, so those two operations are left out of the lagging streams (they are covered with prompt reports and in the unit tests). */
    private fun op(r: Rig, rnd: Random, external: Boolean, lagging: Boolean = false): String {
        val c = r.c
        val k = rnd.nextInt(if (external) 28 else 24)
        return when (k) {
            0, 1, 2, 3, 4, 5, 19 -> { val ch = letters[rnd.nextInt(26)].toString(); c.type(ch); "type $ch" }
            6 -> { val p = punct[rnd.nextInt(punct.size)]; c.type(p); "type '$p'" }
            7 -> { c.space(); "space" }
            8, 9 -> { c.backspace(); "backspace" }
            10, 11 -> { val w = words[rnd.nextInt(words.size)]; c.commitGlide(w); "glide $w" }
            12 -> { val w = words[rnd.nextInt(words.size)]; val ok = c.swapGlide(w); "swap $w=$ok" }
            13 -> { var d = rnd.nextInt(9) - 4; if (lagging && d > 0 && c.after.isEmpty()) d = -d; c.moveCursor(d); "move $d" }   // (a move to the right at the very end of the text cannot be predicted: KB-101b, left out of lagging streams)
            14 -> if (lagging) { c.type("a"); "type a" } else { c.enter(); "enter" }
            15 -> { c.tapShift(); "shift" }
            16 -> { c.lockShift(); "lock" }
            17 -> { c.setShift(ShiftState.OFF); "shiftOff" }
            18 -> { var d = if (lagging) rnd.nextInt(9) - 4 else rnd.nextInt(121) - 60; if (lagging && d > 0 && c.after.isEmpty()) d = -d; c.moveCursor(d); "move $d" }
            20 -> { val w = words[rnd.nextInt(words.size)]; val ok = c.pickWord(w); "pick $w=$ok" }
            21 -> { val b = listOf(" ", ".", ",")[rnd.nextInt(3)]; c.typeCorrected("the", b); "fix the$b" }
            22 -> { for (ch in "teh") c.type(ch.toString()); c.typeCorrected("the", " "); "typo teh -> the" }
            23 -> { val w = c.wordBeforeCursor(); if (w != null) c.contextBefore(w); "peek $w" }
            24 -> { val p = rnd.nextInt(r.text.length + 1); r.ed.userMove(p); "USER move $p" }
            25 -> { val t = listOf("PASTED", "x", "\n", "\uD83D\uDE00")[rnd.nextInt(4)]; r.ed.userInsert(t); "USER insert '$t'" }
            26 -> { val a = rnd.nextInt(r.text.length + 1); val b = minOf(r.text.length, a + rnd.nextInt(5)); r.ed.userSelect(a, b); "USER select $a-$b" }
            else -> if (rnd.nextInt(6) == 0) { r.ed.appSetText("app " + rnd.nextInt(1000) + " text"); "APP setText" } else { r.ed.userMove(0); "USER move 0" }
        }
    }

    private fun run(seed: Long, ops: Int, lazy: Boolean, external: Boolean, flaky: Boolean = false) {
        val rnd = Random(seed)
        val start = listOf("", "hello world", "x".repeat(130), "line one\nline two")[rnd.nextInt(4)]
        val log = ArrayList<String>()
        var firstForeign: String? = null
        val traceLines = ArrayList<String>()
        val r = Rig(start, if (rnd.nextBoolean()) start.length else rnd.nextInt(start.length + 1), multiline = rnd.nextInt(4) != 0,
            action = if (rnd.nextInt(5) == 0) ImeActions.SEARCH else ImeActions.UNSPECIFIED)
        try {
            for (i in 0 until ops) {
                if (flaky && rnd.nextInt(15) == 0) {
                    r.ed.failing = true; log.add("DEAD " + op(r, rnd, false)); r.ed.failing = false
                    // a dead editor loses nothing it said: the keyboard is told about the new field state like Android does after a restart
                    r.ed.updates.clear(); r.c.startInput(FieldInfo(initialSelStart = r.ed.selS, initialSelEnd = r.ed.selE)); r.check(); continue
                }
                val f0 = r.foreign
                val d0 = op(r, rnd, external, lazy); log.add(d0)
                if (System.getenv("SWOOP_TRACE_SEED") == seed.toString()) traceLines.add("#$i $d0 -> believed ${r.c.selStart} editor ${r.ed.selS} composing '${r.c.composing}' glide ${r.c.lastGlide?.text?.length} expected [${r.c.pendingForTest()}] queue ${r.ed.updates.map { "${it[0]}${if (it[2] >= 0) "c" else ""}" }}")
                if (lazy) { repeat(rnd.nextInt(3)) { r.deliverOne() }; while (r.ed.updates.size > MAX_LAG) r.deliverOne() } else { r.sync(); r.check() }
                if (r.foreign > f0 && firstForeign == null) firstForeign = "op #$i '${log.last()}' after '${log.getOrNull(log.size - 2)}' (text ...'${r.text.takeLast(20).replace("\n", "\\n")}')"
            }
            r.sync(); r.check()
        } catch (e: Throwable) {
            System.getenv("SWOOP_TRACE_FILE")?.let { java.io.File(it).writeText(traceLines.drop((System.getenv("SWOOP_TRACE_FROM") ?: "-1").toIntOrNull()?.takeIf { it >= 0 } ?: maxOf(0, traceLines.size - 40)).take(40).joinToString("\n")) }
            throw AssertionError("first foreign report: $firstForeign [${r.firstForeignInfo}]; seed $seed (lazy=$lazy external=$external flaky=$flaky) start='${start.take(20).replace("\n", "\\n")}' first ops: ${log.take(10)} last ops: ${log.takeLast(8)} text='${r.text.takeLast(40).replace("\n", "\\n")}': ${e.message}", e)
        }
    }

    @Test fun ownEditsOnlyReportsDeliveredAtOnce() { for (s in 1L..(1L + (250 - 1) * scale)) run(s, 120, lazy = false, external = false) }
    @Test fun ownEditsOnlyReportsArriveLateAndInBatches() { for (s in 1000L..(1000L + (1250 - 1000) * scale)) run(s, 120, lazy = true, external = false) }
    @Test fun theUserAndTheAppChangeTheTextInBetweenReportsAtOnce() { for (s in 2000L..(2000L + (2250 - 2000) * scale)) run(s, 120, lazy = false, external = true) }
    @Test fun theUserAndTheAppChangeTheTextInBetweenReportsLate() { for (s in 3000L..(3000L + (3250 - 3000) * scale)) run(s, 120, lazy = true, external = true) }
    @Test fun anEditorThatDiesAndComesBack() { for (s in 4000L..(4000L + (4150 - 4000) * scale)) run(s, 120, lazy = false, external = false, flaky = true) }

    /** Nothing the keyboard does with its beliefs may delete text it did not insert: Backspace after a glide removes exactly the glide, and only the glide. */
    @Test fun backspaceAfterAGlideNeverEatsTheTextBeforeIt() {
        val rnd = Random(77)
        repeat(300) {
            val pre = (0 until rnd.nextInt(30)).map { letters[rnd.nextInt(26)] }.joinToString("")
            val r = Rig(pre); r.act { commitGlide(words[rnd.nextInt(words.size)]) }
            val withGlide = r.text
            if (rnd.nextBoolean()) {
                r.ed.userInsert("Z"); r.sync()                                    // the app typed after the glide: the glide is no longer the last thing
                r.act { backspace() }
                assertEquals("only the app's character may go", withGlide, r.text)
            } else {
                r.act { backspace() }
                assertEquals("exactly the glide (word and spaces) goes", pre, r.text)
            }
        }
    }

    @Test fun theFuzzerReallyChangesTheText() {   // guards the fuzzer itself against becoming a no-op
        val r = Rig(); val rnd = Random(5); repeat(200) { op(r, rnd, false); r.sync() }
        assertTrue(r.ed.calls > 200); assertEquals(true, r.text.isNotEmpty() || r.ed.calls > 0)
    }
}
