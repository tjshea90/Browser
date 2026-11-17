package app.kestrel.browser

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

/**
 * The link to the Swoop keyboard, the owner's own keyboard app (tjshea90/Keyboard). Owner request 17: Kestrel ALWAYS types through Swoop when it is installed. Every text box here (address bar, page fields,
 * find bar) already uses Android's own keyboard, so when Swoop is the chosen keyboard everything typed here feeds Swoop's ONE learned memory and there is nothing to copy between the apps. What this object does:
 * knows whether Swoop is installed / switched on / chosen; asks Android to let the person choose it when it is installed but not chosen (an app cannot switch keyboards silently); marks the address box so Swoop gives
 * it the browser layout ([OMNIBOX_OPTION]); and asks Swoop to float over the page with one private command ([ACTION_FLOAT], delivered by Android only from the app whose text box is edited, to the current keyboard).
 * The panel's own small keyboard stays only as the fallback when Swoop is not installed. docs/KEYBOARD-LINK.md.
 */
object SwoopLink {
    /** The private commands Swoop's KeysService understands (the same strings are in its source). */
    const val ACTION_FLOAT = "app.swoop.keyboard.FLOAT"
    const val ACTION_DOCK = "app.swoop.keyboard.DOCK"
    /** The release build's package; the debug build sits beside it under the same name plus ".debug". */
    private const val PKG = "app.swoop.keyboard"
    /** Set as privateImeOptions on the address box and the Home box: Swoop then gives them its browser layout (slash, .com, Go) with words, glide and learning kept (Swoop FieldKind.OMNIBOX). */
    const val OMNIBOX_OPTION = "app.kestrel.omnibox=1"

    /** True for the id of a keyboard service that is Swoop (Android writes an input method's id as "package/class"). */
    fun isSwoopId(id: String?): Boolean {
        if (id.isNullOrEmpty()) return false
        val pkg = id.substringBefore('/')
        return pkg == PKG || pkg == "$PKG.debug"
    }

    /** Swoop is the keyboard the person has chosen right now. */
    fun isChosen(ctx: Context): Boolean = try { isSwoopId(Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)) } catch (e: Exception) { false }

    /** Swoop (release or debug build) is installed on this device (needs the <queries> entries of the manifest on Android 11 and later). */
    fun isInstalled(ctx: Context): Boolean {
        val pm = ctx.packageManager
        for (p in arrayOf(PKG, "$PKG.debug")) { try { pm.getPackageInfo(p, 0); return true } catch (e: Exception) { /* not this one */ } }
        return false
    }

    /**
     * Swoop is installed but not the chosen keyboard: asks Android to let the person pick it. Not switched on yet: the system keyboard settings open (it must be switched on first); switched on: the keyboard
     * picker opens. An app cannot do more than that; after one choice nothing is asked again.
     */
    fun offerChoice(ctx: Context, imm: InputMethodManager) {
        try {
            if (!isEnabled(imm)) {
                ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                Toast.makeText(ctx, "Switch Swoop on, then tap the keyboard button again", Toast.LENGTH_LONG).show()
            } else {
                imm.showInputMethodPicker()
                Toast.makeText(ctx, "Choose Swoop: Kestrel types with it", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) { /* no picker on this device: the person can still choose it in Android's settings */ }
    }

    /** Swoop is installed and switched on (it may still not be the chosen one). */
    fun isEnabled(imm: InputMethodManager): Boolean = try { imm.enabledInputMethodList.any { isSwoopId(it.id) } } catch (e: Exception) { false }

    /** Asks the current keyboard (Swoop) to float over the page. [view] must be the view whose text box is being edited, or Android drops the command. */
    fun sendFloat(imm: InputMethodManager, view: View) { try { imm.sendAppPrivateCommand(view, ACTION_FLOAT, null) } catch (e: Exception) { /* a keyboard that does not know the command ignores it */ } }
}
