package app.kestrel.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * NL1 (research/REVIEW_2026-11-22_NEWLEVERS.md): Mozilla's compatibility fixes ride in Kestrel G. The engine module has no JVM test source set, so the rules that keep the feature light and safe are pinned by reading
 * the source: a later edit that moves the install onto the cold-start path, adds background network or breaks the pin fails here.
 */
class SiteFixesRulesTest {
    private fun source(path: String): String {
        val f = listOf(path, "../$path").map { File(it) }.firstOrNull { it.exists() } ?: throw IllegalStateException("$path not found (run from the repository root or core/)")
        return f.readText()
    }

    private val gradle get() = source("geckoapp/build.gradle.kts")
    private val fixes get() = source("geckoapp/src/main/kotlin/app/kestrel/browser/web/SiteFixes.kt")
    private val activity get() = source("geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt")

    @Test fun theFixesComeFromMozillasMavenAtTheEnginesMajorVersionAndNoCodeOfItEntersTheApp() {
        val engine = Regex("""val geckoVersion = "(\d+)\.""").find(gradle)!!.groupValues[1]
        val fixesMajor = Regex("""val webcompatVersion = "(\d+)\.""").find(gradle)!!.groupValues[1]
        assertEquals("feature-webcompat must be the build made for this engine major: bump both together (HANDOFF.md monthly engine step)", engine, fixesMajor)
        assertTrue(gradle.contains("""webcompatAar("org.mozilla.components:feature-webcompat:${'$'}webcompatVersion@aar")"""))
        assertTrue("only assets are used: the library is not transitive and is not on the app's class path", gradle.contains("isTransitive = false") && !gradle.contains("implementation(\"org.mozilla.components"))
        val settings = source("settings.gradle.kts")
        assertTrue("one module of that group, not the whole group", settings.contains("""includeModule("org.mozilla.components", "feature-webcompat")"""))
    }

    @Test fun theRemoteSettingsListenerIsReplacedSoNoNewBackgroundTrafficStarts() {
        assertTrue(gradle.contains("remote_settings_update_check.js"))
        assertTrue(gradle.contains("function listenForRemoteSettingsUpdates() {}"))
    }

    @Test fun theInstallStartsLateAfterTheFirstRealPageNeverOnTheColdStartPath() {
        assertTrue("the delay is at least five seconds", (Regex("""DELAY_MS = (\d[\d_]*)L""").find(fixes)!!.groupValues[1].replace("_", "").toLong()) >= 5_000L)
        assertTrue(fixes.contains("Io.main.postDelayed({ apply(app) }, DELAY_MS)"))
        val installs = Regex("""SiteFixes\.afterFirstPage""").findAll(activity).count()
        assertEquals("the timer is started from exactly one place: pageFinished", 1, installs)
        val at = activity.indexOf("SiteFixes.afterFirstPage")
        val fn = activity.lastIndexOf("internal fun pageFinished(", at)
        assertTrue("that place is pageFinished", fn >= 0 && at - fn < 700)
        assertTrue("never started by a private tab", activity.substring(fn, at).contains("!tab.isPrivateTab"))
        assertFalse("the engine start burst does not install it", activity.substringBefore("val burst4").contains("SiteFixes"))
    }

    @Test fun aFailureIsOnlyLoggedAndTheSwitchStopsAnExtensionThatIsAlreadyThere() {
        assertTrue(fixes.contains("pages load without them"))
        assertTrue("off = disable what is there, never install just to stop it", fixes.contains("controller.list()") && fixes.contains("EnableSource.APP"))
        assertTrue(fixes.contains("""EXTENSION_ID = "webcompat@mozilla.org""""))
        assertTrue(fixes.contains("""ASSET_URI = "resource://android/assets/webcompat/""""))
        assertTrue("Diagnostics shows the line", activity.contains("SiteFixes.statusLine()"))
    }

    @Test fun theSwitchIsOnByDefaultAndPartOfBackups() {
        assertTrue(source("app/src/main/kotlin/app/kestrel/browser/data/Prefs.kt").contains("""var siteFixes: Boolean get() = bool("site_fixes", true)"""))
        assertTrue(source("app/src/main/kotlin/app/kestrel/browser/data/BackupManager.kt").contains("""Spec("site_fixes", T.BOOL)"""))
    }
}
