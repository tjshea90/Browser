package app.kestrel.core.input

/**
 * Owner R23a (2026-11-18): "if i long press the delete key, progressively delete from slow to fast, and eventually to deleting entire words at once."
 * What a held Backspace does, as a pure schedule (shared with the Kestrel browser's keyboard): the first repeat comes [FIRST_MS] after the press; the characters then go at [SLOW_MS] apart and speed up in a straight line to
 * [FAST_MS] over [RAMP_MS]; after [WORDS_AFTER_MS] of holding, every repeat deletes a WHOLE WORD, first [WORD_SLOW_MS] apart and speeding up to [WORD_FAST_MS] over [WORD_RAMP_MS]. It never gets slower again.
 */
object BackspaceRepeat {
    const val FIRST_MS = 380L
    const val SLOW_MS = 190L
    const val FAST_MS = 45L
    const val RAMP_MS = 1800L
    const val WORDS_AFTER_MS = 2600L
    const val WORD_SLOW_MS = 230L
    const val WORD_FAST_MS = 110L
    const val WORD_RAMP_MS = 2500L

    /** True when a repeat that fires [heldMs] after the press deletes a whole word. */
    fun wordMode(heldMs: Long): Boolean = heldMs >= WORDS_AFTER_MS

    /** The wait until the next repeat, when one has just fired [heldMs] after the press. */
    fun delayAfter(heldMs: Long): Long {
        if (heldMs < WORDS_AFTER_MS) {
            val t = ((heldMs - FIRST_MS).toFloat() / RAMP_MS).coerceIn(0f, 1f)
            return Math.round(SLOW_MS - (SLOW_MS - FAST_MS) * t).toLong()
        }
        val u = ((heldMs - WORDS_AFTER_MS).toFloat() / WORD_RAMP_MS).coerceIn(0f, 1f)
        return Math.round(WORD_SLOW_MS - (WORD_SLOW_MS - WORD_FAST_MS) * u).toLong()
    }
}
