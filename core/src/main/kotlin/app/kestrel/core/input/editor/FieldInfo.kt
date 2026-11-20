package app.kestrel.core.input.editor

/**
 * What kind of text the field holds (decided in :ime from EditorInfo.inputType; KB-104 owns the table).
 * OMNIBOX = the address-and-search box of the Kestrel browser (it says so in privateImeOptions, docs/KESTREL-LINK.md): the URL keys (slash, .com) and no automatic capitals, spaces or fixes, but words,
 * glide, guesses and learning as in a search box.
 */
enum class FieldKind { TEXT, PASSWORD, EMAIL, URL, NUMBER, PHONE, SEARCH, OMNIBOX }

/**
 * The facts about the field being edited, as plain data. [imeAction] is one of [ImeActions]. [noPersonalizedLearning] is IME_FLAG_NO_PERSONALIZED_LEARNING
 * (the app asked the keyboard to learn nothing from this field, e.g. incognito); a password field also learns nothing, whatever the flag says.
 */
class FieldInfo(
    val kind: FieldKind = FieldKind.TEXT,
    val imeAction: Int = ImeActions.UNSPECIFIED,
    val noEnterAction: Boolean = false,
    val noPersonalizedLearning: Boolean = false,
    val noSuggestions: Boolean = false,
    /** The automatic capitalisation the field asked for ([CapFlags] bits of its input type); 0 = none (passwords, e-mail, URLs ask for none). */
    val capsFlags: Int = 0,
    val initialSelStart: Int = -1,
    val initialSelEnd: Int = -1,
) {
    /** The keyboard may learn words, pairs and glide shapes from this field. */
    val learningAllowed: Boolean get() = kind != FieldKind.PASSWORD && !noPersonalizedLearning
    /** Tapped letters are shown as composing text (the app underlines the word; autocorrect works on it). Never in fields where a word is not a word. */
    val composingAllowed: Boolean get() = !noSuggestions && (kind == FieldKind.TEXT || kind == FieldKind.SEARCH || kind == FieldKind.OMNIBOX)
}
