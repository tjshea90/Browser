package app.kestrel.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Swoop keyboard is recognised by its package, in Android's "package/class" form of an input method id, and only by that. */
class SwoopLinkTest {
    @Test fun theReleaseAndTheDebugBuildAreSwoopAndNothingElseIs() {
        assertTrue(SwoopLink.isSwoopId("app.swoop.keyboard/.KeysService"))
        assertTrue(SwoopLink.isSwoopId("app.swoop.keyboard/app.swoop.keyboard.KeysService"))
        assertTrue(SwoopLink.isSwoopId("app.swoop.keyboard.debug/app.swoop.keyboard.KeysService"))
        assertFalse(SwoopLink.isSwoopId("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME"))
        assertFalse("another app whose name only starts like it", SwoopLink.isSwoopId("app.swoop.keyboardx/.Service"))
        assertFalse("the package after the slash does not count", SwoopLink.isSwoopId("com.other/app.swoop.keyboard.KeysService"))
        assertFalse(SwoopLink.isSwoopId(null)); assertFalse(SwoopLink.isSwoopId(""))
    }

    @Test fun theCommandsAreTheOnesSwoopListensFor() {
        assertEquals("app.swoop.keyboard.FLOAT", SwoopLink.ACTION_FLOAT)
        assertEquals("app.swoop.keyboard.DOCK", SwoopLink.ACTION_DOCK)
    }
}
