package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.InputController
import app.kestrel.core.input.editor.ShiftState

/**
 * What TAP typing teaches the keyboard (N6, owner: "the learning is the most important part. it must work well and learn at all times regardless of the app i'm in"). It listens to the keys the person
 * presses and to the text before the cursor, never to the app, so it works the same everywhere a keyboard works (the only fields it ignores are the ones that must not be learned from: passwords,
 * incognito requests, and fields where a word is not a word: addresses, e-mail, numbers). What it learns:
 *  - every word that was typed and finished (space, punctuation, Enter): the word, the pair with the word before it and the triple with the two before it (a short word counts once);
 *  - a word picked from the strip counts double, and when it replaced a typo the keyboard remembers the typo and what was meant (so the next time that typo is fixed first);
 *  - ERASE AND RETYPE: when a finished word is taken back (Backspace into it) it is unlearned, and the word typed in its place (a similar one, within half a minute) was what the person meant: it counts
 *    triple and the pair (erased word, new word) is remembered as a correction;
 *  - an automatic fix that Backspace takes back: the typed word is the person's now (it is never fixed again) and the fix is unlearned.
 * Words glided are learned by [GlideSession]; this class leaves those alone. Single-threaded: the UI thread; the engine does the work on its own thread.
 */
class TypingLearner(
    private val controller: InputController,
    private val engine: () -> GlideTyping?,
    private val now: () -> Long,
) {
    private class Done(val word: String, val prev: String?, val prev2: String?, val weight: Float)

    private var last: Done? = null          // the word finished most recently (already learned)
    private var erased: Done? = null        // a finished word the person is taking back right now (already unlearned)
    private var erasedAt = 0L
    private var retypeLetters = 0

    private fun active(): Boolean { val f = controller.field; return f.learningAllowed && (f.kind == FieldKind.TEXT || f.kind == FieldKind.SEARCH) }

    private fun isWordChar(text: String): Boolean {
        if (text.isEmpty() || text.codePointCount(0, text.length) != 1) return false
        val c = text.codePointAt(0)
        return Character.isLetter(c) || ((text == "'" || Character.isDigit(c)) && controller.composing != null)
    }

    /** Call BEFORE a key reaches the controller. [fix] is the word an automatic correction is about to put in the place of the one typed (null = none). */
    fun beforeKey(key: Key, fix: String? = null) {
        val g = engine() ?: return
        if (!active()) return
        when (key) {
            is Key.Ch -> if (isWordChar(key.text)) { if (erased != null) retypeLetters++ } else finishWord(g, fix)
            Key.Go -> finishWord(g, fix)
            Key.Backspace -> takeBackCheck(g)
            else -> {}
        }
    }

    /** Call AFTER the controller handled the key: a Backspace may just have taken an automatic fix back. */
    fun afterKey() {
        val fix = controller.revertedCorrection ?: return
        val g = engine() ?: return
        if (!active()) return
        if (fix.typed.equals(fix.word, ignoreCase = true)) {                                                    // only a capital was put in (a name, a day): the person writes it small
            g.learnCase(fix.typed.lowercase(), 2f); g.stats.onAutoCorrectUndone(); last = null; erased = null; return
        }
        g.learnFix(true)                                                                                        // (R18d: this person takes fixes back: the keyboard gets stricter)
        val l = last
        if (l != null && l.word == fix.word.lowercase()) g.unlearnWord(l.prev, l.word, l.weight + 1f, l.prev2)   // the fix and what it earned go
        g.learnWord(l?.prev, fix.typed, 2f, l?.prev2)                                                           // what was typed is the person's word: twice, so it is never "fixed" again
        g.stats.onAutoCorrectUndone()
        last = null; erased = null
    }

    /** A word from the strip was tapped (or the typed word itself, to keep it): the controller puts it in and it is learned (double). False when the controller could not. */
    fun pick(word: String): Boolean {
        val g = engine()
        val typed = controller.wordBeforeCursor()
        val (p2, p1) = if (typed != null) controller.contextBefore(typed) else controller.previousTwo()
        val atStart = typed == null && InputController.startsSentence(controller.before, controller.selStart)
        val mine = typed != null && controller.typedWord() == typed                                                // (R23b: asked before the pick, which ends the typed text)
        if (!controller.pickWord(word)) return false
        if (g == null || !active()) return true
        if (atStart) g.learnStart(word)
        g.stats.onSuggestionPicked()
        val w = word.lowercase()
        val fixed = typed != null && mine && !w.startsWith(typed.lowercase())          // it replaced a typo the person typed (R23b: not letters that were pasted), it did not finish the word
        g.learnWord(p1, w, 2f, p2, if (fixed) typed!!.lowercase() else null, 2f)
        last = Done(w, p1, p2, 2f); erased = null
        return true
    }

    /**
     * R18a: a word that was already in the text ([old]) was replaced by [new] from the strip, after the person tapped it. [old] was learned when it was typed: if it is a slip of [new] (a few letters differ) it is taken
     * back and remembered as a correction (the next time it is typed, [new] comes first); a different word the person simply preferred leaves [old] alone. [new] counts double: the person chose it. [prev2] / [prev] are the
     * words before the replaced word.
     */
    fun replaced(old: String, new: String, prev2: String?, prev: String?) {
        val g = engine() ?: return
        if (!active()) return
        val o = old.lowercase(); val n = new.lowercase()
        if (o == n) return
        g.stats.onSuggestionPicked()
        val slip = similar(o, n)
        if (slip) g.unlearnWord(prev, o, 1f, prev2)
        g.learnWord(prev, n, 2f, prev2, if (slip) o else null, 2f)
        last = null; erased = null
    }

    /** The cursor moved, the field changed or the keyboard closed: nothing here may still refer to the old text. */
    fun settle() { last = null; erased = null }

    private fun finishWord(g: GlideTyping, fix: String?) {
        val typed = controller.wordBeforeCursor() ?: run { return }
        // R23b (owner: "never at any time allow the keyboard to learn and save anything from the clipboard"): only a word typed here, key by key, is ever learned - not one that was pasted, put in by an app, or only stands before the cursor
        if (controller.typedWord() != typed) return
        val (p2, p1) = controller.contextBefore(typed)
        val realFix = fix?.takeIf { !it.equals(typed, ignoreCase = true) }      // ("i" -> "I" only changes the capital: not a correction)
        var word = (realFix ?: typed).lowercase()
        var weight = 1f
        var guessed: String? = null
        val e = erased
        if (e != null && now() - erasedAt <= RETYPE_MS && retypeLetters >= 1 && word.length >= 2 && word != e.word) {
            if (similar(e.word, word)) { weight = 3f; guessed = e.word; g.stats.onRetyped() }   // erased, retyped: the new word is what was meant
        }
        if (realFix != null) { guessed = typed.lowercase(); g.stats.onAutoCorrected(); g.learnFix(false) }   // the keyboard fixed a typo and it was not taken back (yet)
        erased = null
        g.learnWord(p1, word, weight, p2, guessed, if (weight > 1f) 2f else 1f)
        last = Done(word, p1, p2, weight)
        val head = controller.before.takeIf { it.endsWith(typed) }?.dropLast(typed.length)
        if (head != null && InputController.startsSentence(head, controller.selStart - typed.length)) g.learnStart(word)      // (R18d: what this person begins sentences with)
        learnCaseOf(g, typed, fix)
    }

    /**
     * R18b: how the person writes this word INSIDE a sentence: with a capital (a name, a place, iPhone) or small. A capital at the start of a sentence says nothing, the keyboard's own capital ([fix] set) says nothing,
     * and a word shouted in capitals is not a habit.
     */
    private fun learnCaseOf(g: GlideTyping, typed: String, fix: String?) {
        if (fix != null || controller.shift == ShiftState.LOCK) return
        if (typed.length > 4 && typed == typed.uppercase()) return
        val b = controller.before
        if (!b.endsWith(typed)) return
        val head = b.dropLast(typed.length)
        if (InputController.startsSentence(head, controller.selStart - typed.length)) return
        g.learnCase(typed)
    }

    /** A Backspace that reaches into the word just finished: the person is taking it back. */
    private fun takeBackCheck(g: GlideTyping) {
        if (controller.lastGlide != null || controller.lastCorrection != null || controller.composing != null) return   // a glided word (the glide session's), an automatic fix (afterKey's) or a word still being tapped
        val l = last ?: return
        val b = controller.before
        val core = b.trimEnd { !it.isLetter() && it != '\'' }
        if (b.length - core.length > 2 || !core.lowercase().endsWith(l.word)) return
        val start = core.length - l.word.length
        if (start > 0 && (core[start - 1].isLetter() || core[start - 1] == '\'')) return
        g.unlearnWord(l.prev, l.word, l.weight, l.prev2)
        erased = l; erasedAt = now(); retypeLetters = 0; last = null
    }

    companion object {
        /** A word typed this soon after another was erased is taken as what the erased one was meant to be. */
        const val RETYPE_MS = 30_000L

        /** Two words are "the same word slipped" when a few letters differ: within a third of the longer word (at least two). */
        fun similar(a: String, b: String): Boolean {
            val max = maxOf(a.length, b.length)
            if (max == 0 || Math.abs(a.length - b.length) > 3) return false
            return distance(a, b) <= maxOf(2, max / 3)
        }

        fun distance(a: String, b: String): Int {
            var prev = IntArray(b.length + 1) { it }
            var cur = IntArray(b.length + 1)
            for (i in 1..a.length) {
                cur[0] = i
                for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
                val t = prev; prev = cur; cur = t
            }
            return prev[b.length]
        }
    }
}
