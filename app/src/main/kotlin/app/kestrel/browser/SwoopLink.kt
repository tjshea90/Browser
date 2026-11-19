package app.kestrel.browser

import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * The link to the Swoop keyboard, the owner's own keyboard app (tjshea90/Keyboard). The navigation panel opens it when it is the chosen system keyboard: Android's own keyboard cannot be moved by an
 * app, but Swoop floats by itself and understands one private command ([ACTION_FLOAT]), which Android delivers only from the app whose text box is being edited and only to the keyboard that is current.
 * That one word is all that passes between the two apps: nothing learned crosses (docs/KEYBOARD-LINK.md says why, and how the learning stays ONE).
 */
object SwoopLink {
    /** The private commands Swoop's KeysService understands (the same strings are in its source). */
    const val ACTION_FLOAT = "app.swoop.keyboard.FLOAT"
    const val ACTION_DOCK = "app.swoop.keyboard.DOCK"
    /** The release build's package; the debug build sits beside it under the same name plus ".debug". */
    private const val PKG = "app.swoop.keyboard"

    /** True for the id of a keyboard service that is Swoop (Android writes an input method's id as "package/class"). */
    fun isSwoopId(id: String?): Boolean {
        if (id.isNullOrEmpty()) return false
        val pkg = id.substringBefore('/')
        return pkg == PKG || pkg == "$PKG.debug"
    }

    /** Swoop is the keyboard the person has chosen right now. */
    fun isChosen(ctx: Context): Boolean = try { isSwoopId(Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)) } catch (e: Exception) { false }

    /** Swoop is installed and switched on (it may still not be the chosen one). */
    fun isEnabled(imm: InputMethodManager): Boolean = try { imm.enabledInputMethodList.any { isSwoopId(it.id) } } catch (e: Exception) { false }

    /** Asks the current keyboard (Swoop) to float over the page. [view] must be the view whose text box is being edited, or Android drops the command. */
    fun sendFloat(imm: InputMethodManager, view: View) { try { imm.sendAppPrivateCommand(view, ACTION_FLOAT, null) } catch (e: Exception) { /* a keyboard that does not know the command ignores it */ } }
}
