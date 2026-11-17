package app.kestrel.core.input.editor

/**
 * What the keyboard needs from the text field it types into. The Android implementation (in :ime) sits on an InputConnection; the tests use
 * FakeEditor, which behaves like one. Every call can FAIL (false / null): the editor can vanish or die at any moment, web views implement only
 * parts of the interface, so no caller may assume success. Offsets are UTF-16 units, exactly as in android.view.inputmethod.InputConnection.
 */
interface EditorBridge {
    /** Up to [max] units before the cursor (before the selection start); "" at the start of the field; null when the editor is gone. */
    fun textBefore(max: Int): String?
    /** Up to [max] units after the cursor (after the selection end). */
    fun textAfter(max: Int): String?
    /** Replaces the composing region (else the selection) with [text] and ends composition; the cursor goes after the text. */
    fun commitText(text: String): Boolean
    /** Replaces the composing region (else the selection) with [text] and marks it as the composing region (the app underlines it); the cursor goes after it. */
    fun setComposingText(text: String): Boolean
    /** Keeps the composing text as it is and ends composition. */
    fun finishComposing(): Boolean
    /** Deletes [before] units before the selection and [after] units after it. */
    fun deleteSurrounding(before: Int, after: Int): Boolean
    /** Selects [start, end) in the editor's own absolute offsets (start == end puts the cursor there). */
    fun setSelection(start: Int, end: Int): Boolean
    /** Press and release the key [code] (a [KeyCodes] value): the editor deals with selections, surrogate pairs and web-page key handlers. */
    fun sendKey(code: Int): Boolean
    /** Runs the field's own action (IME_ACTION_GO, SEARCH, SEND, DONE, NEXT, PREVIOUS). */
    fun performAction(action: Int): Boolean
    /** Which of the capitalisation modes in [reqModes] (the CAP_* flags of the field's input type) apply at the cursor right now: 0 = none. Android's own TextUtils rules; 0 when the editor is gone. */
    fun cursorCapsMode(reqModes: Int): Int
    /** Edits between begin and end are applied as one (the app sees one change). They nest. */
    fun beginBatch(): Boolean
    fun endBatch(): Boolean
}

/** The same numbers as android.view.KeyEvent, kept here so :core needs no Android class. */
object KeyCodes {
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val ENTER = 66
    const val DEL = 67
}

/** The same numbers as android.text.InputType.TYPE_TEXT_FLAG_CAP_*: which automatic capitalisation a field asked for. */
object CapFlags {
    const val CHARACTERS = 0x1000
    const val WORDS = 0x2000
    const val SENTENCES = 0x4000
    const val ALL = CHARACTERS or WORDS or SENTENCES
}

/** The same numbers as android.view.inputmethod.EditorInfo.IME_ACTION_*. */
object ImeActions {
    const val UNSPECIFIED = 0
    const val NONE = 1
    const val GO = 2
    const val SEARCH = 3
    const val SEND = 4
    const val NEXT = 5
    const val DONE = 6
    const val PREVIOUS = 7
}
