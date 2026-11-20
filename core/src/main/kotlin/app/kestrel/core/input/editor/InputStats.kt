package app.kestrel.core.input.editor

/**
 * Counters only (plan 6.8 and 8.5 layer 1): how the keyboard was used and how often its beliefs about the editor had to be repaired. NEVER any text, word or
 * position. Read by the Diagnostics screen; single-threaded (the UI thread).
 */
class InputStats {
    @JvmField var keys = 0
    @JvmField var backspaces = 0
    @JvmField var enters = 0
    @JvmField var glides = 0
    @JvmField var swaps = 0
    @JvmField var cursorMoves = 0
    /** Selection reports that were not the keyboard's own edits: the user or the app moved or changed the text. */
    @JvmField var foreignReports = 0
    /** Times an editor call failed or surprised the keyboard and it re-read the editor. */
    @JvmField var resyncs = 0

    fun line(): String = "keys $keys, backspaces $backspaces, enters $enters, glides $glides, picks $swaps, cursor moves $cursorMoves, foreign changes $foreignReports, resyncs $resyncs"
}
