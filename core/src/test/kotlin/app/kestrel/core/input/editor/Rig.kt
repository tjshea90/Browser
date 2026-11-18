package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** A controller wired to a [FakeEditor], with the checks every test shares. [sync] delivers every queued selection report. */
class Rig(text: String = "", sel: Int = text.length, multiline: Boolean = true,
          kind: FieldKind = FieldKind.TEXT, action: Int = ImeActions.UNSPECIFIED, noEnter: Boolean = false, noSuggestions: Boolean = false, noLearn: Boolean = false, caps: Int = 0,
          /** The editor reports INSIDE each call (an editor in the keyboard's own process on Android 10): see [FakeEditor.inlineSink]. */
          inlineReports: Boolean = false,
          /** The keyboard's own capitals (the start of an empty box, after . ! ?) in a box that asked for none. Off here so the many tests that type into an empty box stay about what they test; [AutoCapsTest] switches it on. */
          sentenceCaps: Boolean = false) {
    val ed = FakeEditor(text, sel, multiline)
    val c = InputController(ed).also { it.sentenceCaps = sentenceCaps }
    var foreign = 0; private set        // selection reports the controller judged NOT to be its own
    var firstForeignInfo: String? = null; private set

    init {
        c.startInput(FieldInfo(kind, action, noEnter, noLearn, noSuggestions, caps, sel, sel))
        if (inlineReports) ed.inlineSink = { u -> deliver(u) }
    }

    private fun deliver(u: IntArray) {
        val believed = "${c.selStart}-${c.selEnd}"; val queued = ed.updates.size
        if (!c.onSelectionUpdate(u[0], u[1], u[2], u[3])) { foreign++; if (firstForeignInfo == null) firstForeignInfo = "report sel ${u[0]}-${u[1]} comp ${u[2]}-${u[3]} but the keyboard believed $believed (queued behind it: $queued; composing='${c.composing}'; expected [${c.pendingForTest()}])" }
    }
    fun deliverOne(): Boolean {
        val u = ed.updates.removeFirstOrNull() ?: return false
        deliver(u)
        return true
    }
    fun sync() { while (deliverOne()) { } }
    /** Does [f], delivers the reports, and checks the keyboard's belief against the editor. */
    fun act(f: InputController.() -> Unit): Rig { c.f(); sync(); check(); return this }
    fun typeAll(s: String): Rig { for (ch in s) act { type(ch.toString()) }; return this }
    val text: String get() = ed.text

    /** The invariants of plan 6.4 at a quiet moment (nothing queued): the window is a true slice of the editor and the beliefs are true. */
    fun check() {
        assertTrue("reports still queued", ed.updates.isEmpty())
        val b = ed.beforeText(InputController.WINDOW_BEFORE)
        assertTrue("before window '${c.before}' is not the end of '$b'", b.endsWith(c.before))
        if (c.selStart >= 0) assertTrue("window too short (${c.before.length}) with ${ed.selS} units before the cursor", c.before.length >= minOf(InputController.MIN_BEFORE, ed.selS) || b == c.before)
        assertTrue("after window '${c.after}' is not the start of '${ed.afterText(InputController.WINDOW_AFTER)}'", ed.afterText(InputController.WINDOW_AFTER).startsWith(c.after))
        if (c.selStart >= 0) { assertEquals("selStart", ed.selS, c.selStart); assertEquals("selEnd", ed.selE, c.selEnd) }
        val comp = c.composing
        if (comp != null) {
            assertTrue("the editor has no composing region but the keyboard believes '$comp'", ed.compS >= 0)
            assertEquals(comp, ed.text.substring(ed.compS, ed.compE)); assertEquals("composing must end at the cursor", ed.compE, ed.selS)
        }
        val g = c.lastGlide
        if (g != null) assertEquals("glide tail is no longer before the cursor", g.text, ed.beforeText(g.text.length))     // (a selection starting right after it is an address bar's inline completion)
    }
}
