package app.kestrel.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which installer files are used up once an update is installed (AO4). */
class ApkFilesTest {
    private val prefix = "kestrel-g-v"

    @Test fun aReleaseFileIsNamedAfterItsTag() {
        assertEquals("kestrel-g-v0.3.0-alpha32", ApkFiles.tagOf("kestrel-g-0.3.0-alpha32.apk", prefix))
        assertEquals("a second copy of the same file", "kestrel-g-v0.3.0-alpha32", ApkFiles.tagOf("kestrel-g-0.3.0-alpha32 (1).apk", prefix))
        assertEquals("upper case from a file manager", "kestrel-g-v0.3.0-alpha32", ApkFiles.tagOf("Kestrel-G-0.3.0-alpha32.APK", prefix))
    }

    @Test fun otherFilesAreNeverThisAppsInstallers() {
        assertNull(ApkFiles.tagOf("kestrel-0.3.0-alpha32.apk", prefix))                 // the other build (system WebView)
        assertNull(ApkFiles.tagOf("kestrel-courier-0.1.0.apk", prefix))
        assertNull(ApkFiles.tagOf("some-game-1.2.apk", prefix))
        assertNull(ApkFiles.tagOf("kestrel-g-0.3.0-alpha32.apk.part", prefix))
        assertNull(ApkFiles.tagOf("kestrel-g-notes.apk", prefix))
        assertNull(ApkFiles.tagOf("", prefix))
    }

    @Test fun theInstalledVersionAndOlderOnesAreUsedUp() {
        val installed = "0.3.0-alpha33"
        assertTrue("the one that was just installed", ApkFiles.isUsedUp("kestrel-g-0.3.0-alpha33.apk", prefix, installed))
        assertTrue(ApkFiles.isUsedUp("kestrel-g-0.3.0-alpha32.apk", prefix, installed))
        assertTrue("alpha9 is older than alpha33 (numbers compare as numbers)", ApkFiles.isUsedUp("kestrel-g-0.3.0-alpha9.apk", prefix, installed))
        assertTrue(ApkFiles.isUsedUp("kestrel-g-0.2.0-alpha40.apk", prefix, installed))
    }

    @Test fun aNewerInstallerIsAnUpdateTheOwnerMayStillWant() {
        assertFalse(ApkFiles.isUsedUp("kestrel-g-0.3.0-alpha34.apk", "kestrel-g-v", "0.3.0-alpha33"))
        assertFalse(ApkFiles.isUsedUp("kestrel-g-0.3.0-alpha100.apk", "kestrel-g-v", "0.3.0-alpha33"))
        assertFalse(ApkFiles.isUsedUp("kestrel-g-0.4.0-alpha1.apk", "kestrel-g-v", "0.3.0-alpha33"))
    }

    @Test fun nothingIsDecidedWithoutKnowingTheInstalledVersionOrForOtherFiles() {
        assertFalse(ApkFiles.isUsedUp("kestrel-g-0.3.0-alpha32.apk", prefix, ""))
        assertFalse(ApkFiles.isUsedUp("holiday.apk", prefix, "0.3.0-alpha33"))
    }
}
