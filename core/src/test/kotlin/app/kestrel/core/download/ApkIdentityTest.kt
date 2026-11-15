package app.kestrel.core.download

import app.kestrel.core.download.ApkIdentity.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkIdentityTest {
    private val own = "app.kestrel.browser.gecko"

    @Test fun anotherAppIsShownWithFacts() { assertEquals(Verdict.SHOW_FACTS, ApkIdentity.verdict("com.example.player", own, false)) }
    @Test fun anAppOfOursThatDidNotComeFromTheUpdaterIsRefused() {
        assertEquals(Verdict.REFUSE_LOOKALIKE, ApkIdentity.verdict(own, own, false))
        assertEquals(Verdict.REFUSE_LOOKALIKE, ApkIdentity.verdict("app.kestrel.browser.gecko.debug", own, false))
        assertEquals(Verdict.REFUSE_LOOKALIKE, ApkIdentity.verdict("app.kestrel.courier", own, false))
        assertEquals(Verdict.REFUSE_LOOKALIKE, ApkIdentity.verdict("app.kestrel.anything.new", own, false))
    }
    @Test fun theUpdatersOwnCopyStillOpens() { assertEquals(Verdict.OWN_UPDATE_OK, ApkIdentity.verdict(own, own, true)) }
    @Test fun theDebugTwinCountsAsTheSameApp() {
        assertEquals(Verdict.REFUSE_LOOKALIKE, ApkIdentity.verdict("app.kestrel.browser.gecko", "app.kestrel.browser.gecko.debug", false))
        assertTrue(ApkIdentity.claimsToBeKestrel("app.kestrel.browser.gecko.debug", "app.kestrel.browser.gecko"))
    }
    @Test fun aNameThatOnlyLooksLikeOursIsNotOurs() {
        assertFalse(ApkIdentity.claimsToBeKestrel("app.kestrelx.browser", own))
        assertFalse(ApkIdentity.claimsToBeKestrel("com.kestrel.browser", own))
    }

    @Test fun powerfulPermissionsAreNamedInPlainWordsWithoutRepeats() {
        val w = ApkIdentity.dangerousWords(listOf("android.permission.INTERNET", "android.permission.RECEIVE_SMS", "android.permission.READ_SMS", "android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.CAMERA"))
        assertEquals(listOf("read and tap the screen (accessibility)", "read text messages", "use the camera"), w)
    }
    @Test fun anAppWithOnlyOrdinaryPermissionsListsNone() { assertTrue(ApkIdentity.dangerousWords(listOf("android.permission.INTERNET", "android.permission.VIBRATE")).isEmpty()) }

    @Test fun theFactsNameTheAppItsPermissionsAndWhatItWouldReplace() {
        val t = ApkIdentity.facts("Super Player", "com.example.player", 14, listOf("read your contacts", "use the camera"), "", null)
        assertTrue(t, t.startsWith("Super Player (com.example.player) is an app installer. It asks for 14 permissions, among them: read your contacts, use the camera."))
        assertTrue(t, t.contains("It is not installed on this tablet."))
        val upd = ApkIdentity.facts("", "com.example.player", 1, emptyList(), "Super Player", true)
        assertTrue(upd, upd.contains("1 permission.") && upd.contains("It would update Super Player"))
        val other = ApkIdentity.facts("X", "com.x", 3, emptyList(), "X", false)
        assertTrue(other, other.contains("signed by somebody else"))
    }

    @Test fun aVideoOrPictureNameCanLieButAnApkNameCannot() {
        assertTrue(ApkIdentity.extensionCanLie("movie.MP4")); assertTrue(ApkIdentity.extensionCanLie("scan.pdf"))
        assertFalse(ApkIdentity.extensionCanLie("player.apk")); assertFalse(ApkIdentity.extensionCanLie("archive.zip")); assertFalse(ApkIdentity.extensionCanLie("noextension"))
    }
}
