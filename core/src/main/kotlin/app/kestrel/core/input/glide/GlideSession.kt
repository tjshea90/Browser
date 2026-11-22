package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.InputController

/**
 * What a glide MEANS to the keyboard's memory, on top of the text it put in (ported from the browser's MiniNav glue, with the editor now behind [InputController]).
 * Rules (research/kestrel-ref/GLIDE_TYPING.md): a glide that is KEPT (the next thing typed is not Backspace) teaches the engine the word, the pair, the shape and where this thumb lands;
 * a word picked from the strip counts three times and remembers what it was picked over; a Backspace right after a glide takes the word back, and the word typed (or glided) in its place
 * is what that glide meant; a retry that gives the deleted word first again is demoted. NOTHING is learned when the field says so ([app.kestrel.core.input.editor.FieldInfo.learningAllowed]).
 * Single-threaded: the UI thread. The engine does its own work on its worker thread.
 */
class GlideSession(
    private val controller: InputController,
    private val engine: () -> GlideTyping?,
    private val now: () -> Long,
    private val habitChanged: (FloatArray) -> Unit = {},
) {
    interface Ui {
        fun showCandidates(words: List<String>)
        fun clearCandidates()
        fun showNoWord()
    }
    var ui: Ui? = null

    private var pendingWord: String? = null
    private var pendingWeight = 1f
    private var pendingGuessed: String? = null
    private var pendingPrev: String? = null
    private var pendingPrev2: String? = null
    private var afterUndo = false
    private var lastDeleted: String? = null
    private var deletedAt = 0L
    private var topGuess: String? = null
    private var retypeWatch = false
    private var retypeLetters = 0
    private var retypeAt = 0L
    private var liftAt = 0L

    private var shown: List<String> = emptyList()
    private fun show(words: List<String>) { shown = words; ui?.showCandidates(words) }

    private fun learnAllowed() = controller.field.learningAllowed

    /** The context for the next glide (and its live previews): the two words before the cursor. */
    fun context(): Pair<String?, String?> = controller.previousTwo()

    /** The finger lifted after a glide: remember when, so Diagnostics can say how long the word took to show. */
    fun glideLifted() { liftAt = now() }

    /** A live guess for the glide still in progress (UI thread). */
    fun onPreview(res: GlideResult) {
        if (res.candidates.isEmpty()) { shown = emptyList(); ui?.clearCandidates() } else show(res.candidates.map { it.text })
    }

    /** The decoder's answer for the glide that ended (UI thread): the best guess goes in, the strip shows the others. */
    fun onDecoded(res: GlideResult, prev: String?, prev2: String?) {
        val g = engine()
        if (g != null && liftAt != 0L) { g.stats.onShown(now() - liftAt); liftAt = 0L }
        var cands = res.candidates
        if (cands.isEmpty()) { ui?.showNoWord(); return }
        // a retry: the word they just deleted comes up first again, so it is wrong again for them: the next one goes first and the deleted one stays in the strip, second
        val rejected = lastDeleted
        if (afterUndo && rejected != null && cands.size >= 2 && cands[0].text == rejected && now() - deletedAt <= RETRY_MS && cands[1].perLetter - cands[0].perLetter <= RETRY_GAP) {
            cands = listOf(cands[1], cands[0]) + cands.drop(2)
            g?.stats?.onRetryDemoted()
        }
        val best = cands[0]
        retypeWatch = false                       // a new glide: its own confirmation handles the deleted one
        confirmPrevious()                         // the glide before this one stays: learn from it first (one thread, in order)
        controller.commitGlide(best.text)
        pendingWord = best.text; pendingWeight = 1f; pendingGuessed = null
        topGuess = best.text
        pendingPrev = prev; pendingPrev2 = prev2
        show(cands.map { it.text })
    }

    /** True while the strip shows the guesses of a glide (a tap on one replaces the glided word). */
    fun hasCandidates(): Boolean = shown.isNotEmpty()

    /** A candidate in the strip was tapped: it replaces the word the glide put in. */
    fun pick(word: String): Boolean {
        val was = pendingWord ?: topGuess
        // the glide's own bookkeeping first; when the app rewrote the field and it was lost, the word that was put in is replaced by what the text before the cursor still shows (verified)
        if (!controller.swapGlide(word) && !(was != null && controller.replaceRecent(was, word))) return false
        pendingWord = word; pendingWeight = 3f                           // the person CHOSE this word over the first guess: it counts three times, once it is kept
        pendingGuessed = topGuess?.takeIf { it != word }                 // ... and the keyboard remembers it guessed the other one
        afterUndo = false
        val g = engine()
        g?.stats?.onPicked()
        g?.accepted(word) { h -> habitChanged(h) }
        show(listOf(word) + shown.filter { it != word }.take(2))
        return true
    }

    /** Call BEFORE a key reaches the controller: it decides what the key means for the glide that is on screen. */
    fun beforeKey(key: Key) {
        when (key) {
            Key.Backspace -> if (controller.lastGlide != null) {        // right after a glide: the word was wrong
                lastDeleted = pendingWord ?: topGuess; deletedAt = now(); pendingWord = null; pendingGuessed = null; afterUndo = true
                engine()?.stats?.onUndone()
                if (learnAllowed()) { engine()?.glideDeleted(lastDeleted); retypeWatch = true; retypeLetters = 0; retypeAt = deletedAt }
            }
            Key.Shift, Key.ToLetters -> {}                                // the text does not change
            else -> { confirmPrevious(); if (key is Key.Ch) afterUndo = false }   // typing on, the symbols page or Go: the glided word stays
        }
        if (retypeWatch && key is Key.Ch && key.text.length == 1 && key.text[0].isLetter()) retypeLetters++
    }

    /** Call AFTER the controller handled the key. */
    fun afterKey(key: Key) {
        if (retypeWatch && key is Key.Ch && key.text == " ") finishRetype()
        if (controller.lastGlide == null) { shown = emptyList(); ui?.clearCandidates() }
    }

    /** The cursor was moved on purpose, the field changed or the keyboard closed: the glide that is on screen stays. */
    fun settle() { confirmPrevious(); pendingWord = null; afterUndo = false; retypeWatch = false; shown = emptyList(); ui?.clearCandidates() }

    /** The glide that was just put in stays: the decoder learns from it where this thumb lands, and the memory learns the word. */
    private fun confirmPrevious() {
        val w = pendingWord ?: return
        pendingWord = null
        val g = engine() ?: return
        g.stats.onKept()
        g.accepted(w) { h -> habitChanged(h) }
        if (learnAllowed()) {
            // kept: the keyboard hears it (a word the person chose, or one that replaced a deleted one, counts more); a correction remembers what it was picked over (a pick) or put in the place of (a deletion)
            val guessed = pendingGuessed ?: if (afterUndo) lastDeleted else null
            g.learn(pendingPrev, w, maxOf(pendingWeight, if (afterUndo) 3f else 1f), pendingPrev2, guessed?.takeIf { it != w }, if (pendingGuessed != null) 1f else 0.5f, fromGlide = true, afterUndo = afterUndo)
        }
        pendingWeight = 1f; pendingGuessed = null
        afterUndo = false
    }

    /**
     * The person deleted a glided word and typed another one letter by letter: the word just typed (at the space) may be what the deleted glide meant, and the keyboard remembers the glide's shape
     * for it when the straight line through the word's letters is not far from it. Only the first word after the deletion, within [RETYPE_MS], after at least two letters.
     */
    private fun finishRetype() {
        if (!retypeWatch) return
        retypeWatch = false
        if (retypeLetters < 2 || now() - retypeAt > RETYPE_MS || !learnAllowed()) return
        controller.previousWord()?.let { engine()?.retyped(it) }
    }

    companion object {
        /** A glide within this long after a deleted glide that gives the deleted word first again is a retry (the deleted word moves to second place), when the next candidate fits the path about as well. */
        const val RETRY_MS = 6000L
        /** A word typed letter by letter this soon after a glided word was deleted is taken as what the deleted glide meant. */
        const val RETYPE_MS = 30_000L
        const val RETRY_GAP = 1.5f
    }
}
