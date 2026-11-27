package app.kestrel.browser

import android.content.pm.PackageInfo
import android.text.InputType
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Owner request 17: Kestrel always types through Swoop when it is installed. It must know whether it is (release or debug build) and mark its address box so Swoop gives it the browser layout. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SwoopInstalledTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private fun install(pkg: String) { shadowOf(app.packageManager).installPackage(PackageInfo().also { it.packageName = pkg; it.versionName = "1" }) }

    @Test fun swoopIsInstalledWhenItsReleaseOrItsDebugBuildIs() {
        assertFalse("nothing is installed in a fresh device", SwoopLink.isInstalled(app))
        install("app.swoop.keyboard.debug"); assertTrue(SwoopLink.isInstalled(app))
    }

    @Test fun theReleaseBuildCountsToo() { install("app.swoop.keyboard"); assertTrue(SwoopLink.isInstalled(app)) }

    @Test fun anotherKeyboardAppDoesNotCount() { install("com.google.android.inputmethod.latin"); install("app.swoop.keyboardx"); assertFalse(SwoopLink.isInstalled(app)) }

    /** The same string is Swoop's FieldInfos.OMNIBOX_OPTION: the two repositories must agree (docs/KEYBOARD-LINK.md). */
    @Test fun theAddressBoxOptionIsTheOneSwoopLooksFor() {
        assertEquals("app.kestrel.omnibox=1", SwoopLink.OMNIBOX_OPTION)
        val f = EditText(app).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI; privateImeOptions = SwoopLink.OMNIBOX_OPTION }
        assertTrue(f.privateImeOptions.split(',').contains("app.kestrel.omnibox=1"))
    }
}
