package app.kestrel.browser.ui

/**
 * The text field of the web page that has focus, as the mini keyboard sees it (AU3, owner message 35: "if I already have a text field open on a web site, make the mini keyboard enter text into that field
 * instead of searching Google"). The panel knows nothing about the engine: the activity hands it one of these when a field is there, and the keys are sent through it.
 */
interface PageField {
    /** Changes whenever focus moves to another field (or leaves): a new number means the text typed so far belongs to a field that is gone. */
    val identity: Int
    /** A password field: the bar shows dots and the keyboard learns nothing from it. */
    val password: Boolean
    /** The page marks this field's Enter as a submit (Go / Search / Send / Done): the keyboard closes after it. Otherwise (a text area) it stays open for the next line. */
    val enterSubmits: Boolean
    /** A private tab: the keyboard learns nothing from it. */
    val privateTab: Boolean
    /** Puts [text] in at the caret (what the system keyboard does when it commits text). */
    fun insert(text: String)
    /** Takes [count] characters off before the caret (Backspace). */
    fun deleteBefore(count: Int)
    /** Presses Enter in the field. */
    fun enter()
}
