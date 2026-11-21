package app.kestrel.browser.diag

import android.content.Context
import app.kestrel.browser.Io
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** LD1 (owner 2026-11-21): the live diagnostics. The master switch, scrub-before-store, the token, and the source rules that keep the data private. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LiveDiagTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()

    @Before fun clean() { LiveDiag.resetForTest(); ctx.filesDir.deleteRecursively(); ctx.filesDir.mkdirs(); ctx.getSharedPreferences("livediag", Context.MODE_PRIVATE).edit().clear().commit(); LiveDiag.init(ctx); ioDone() }
    @After fun off() { ioDone(); LiveDiag.resetForTest() }

    private fun ioDone() { val l = CountDownLatch(1); Io.exec { l.countDown() }; assertTrue("the IO lane answers", l.await(10, TimeUnit.SECONDS)) }
    private fun src(rel: String): String = File("src/main/kotlin/app/kestrel/browser/$rel").readText()

    @Test fun theMasterSwitchOffRecordsNothingAndSaysSo() {
        LiveDiag.setEnabled(false)
        LiveDiag.observe("page", "slow", "page slow on example.com took 9 s")
        assertEquals(emptyList<String>(), LiveDiag.journalForTest())
        assertTrue(LiveDiag.statusLine(), LiveDiag.statusLine().contains("OFF"))
        assertTrue(LiveDiag.statusLine(), LiveDiag.statusLine().contains("nothing is sent"))
        assertFalse(LiveDiag.isEnabled())
    }

    @Test fun theSwitchSurvivesARestart() {
        LiveDiag.setEnabled(false)
        LiveDiag.resetForTest(); LiveDiag.init(ctx); ioDone()
        assertFalse("the off switch is stored", LiveDiag.isEnabled())
        LiveDiag.setEnabled(true)
        LiveDiag.resetForTest(); LiveDiag.init(ctx); ioDone()
        assertTrue(LiveDiag.isEnabled())
    }

    @Test fun anEventIsScrubbedBeforeItIsKept() {
        LiveDiag.observe("page", "slow", "load of https://www.example.com/account/42?token=abc123def456 took 9.1 s, mail me at someone@example.org")
        val j = LiveDiag.journalForTest()
        assertEquals(1, j.size)
        assertTrue(j[0], j[0].contains("example.com"))
        assertFalse(j[0], j[0].contains("/account/42"))
        assertFalse(j[0], j[0].contains("abc123def456"))
        assertFalse(j[0], j[0].contains("someone@example.org"))
    }

    @Test fun aLayerOrEventTheOwnerNeverWantedIsNotKept() {
        LiveDiag.observe("secretlayer", "x", "anything at all")
        LiveDiag.observe("media", "name-clues", "Some Film Title (2024) 1080p")
        LiveDiag.observe("tabs", "title-changed", "My Private Document Title")
        assertEquals(emptyList<String>(), LiveDiag.journalForTest())
    }

    @Test fun aWhitelistedEventIsKept() {
        LiveDiag.observe("video", "stall", "stall 4.2 s on cdn.example.net")
        assertEquals(1, LiveDiag.journalForTest().size)
    }

    @Test fun turningItOffErasesTheJournalAndItsFile() {
        repeat(30) { LiveDiag.observe("page", "slow", "load $it on example.com took 9 s") }      // 25 lines flush the file
        ioDone()
        val f = File(ctx.filesDir, "livediag.journal")
        assertTrue("the journal is on disk", f.isFile)
        LiveDiag.setEnabled(false)
        assertEquals(emptyList<String>(), LiveDiag.journalForTest())
        assertFalse("the file is gone", f.exists())
    }

    @Test fun theJournalIsBounded() {
        repeat(1000) { LiveDiag.observe("page", "slow", "load $it on example.com took 9 s") }
        assertTrue(LiveDiag.journalForTest().size <= 800)
    }

    @Test fun aTokenMustLookLikeAGitHubTokenAndIsKeptOutOfTheStatus() {
        val good = "github_pat_FAKE" + "A".repeat(40)
        assertFalse(LiveDiag.tokenLooksRight("hello world, this is not a token at all"))
        assertFalse(LiveDiag.tokenLooksRight("github_pat_FAKE short"))
        assertFalse(LiveDiag.tokenLooksRight(good + " extra"))
        assertFalse(LiveDiag.saveToken("nope"))
        assertFalse(LiveDiag.hasToken())
        assertTrue(LiveDiag.saveToken(good))
        assertTrue(LiveDiag.hasToken())
        assertFalse(LiveDiag.statusLine().contains(good))
        LiveDiag.forgetToken()
        assertFalse(LiveDiag.hasToken())
    }

    // ---- the rules, pinned by reading the source (a later edit that breaks one fails here)

    @Test fun observeLooksAtTheSwitchBeforeAnythingElse() {
        val s = src("diag/LiveDiag.kt")
        val body = s.substringAfter("fun observe(layer: String, event: String, line: String) {").substringBefore("}\n\n")
        assertTrue(body.trimStart().startsWith("if (!enabled) return"))
        assertTrue("the whitelist comes before the scrub", body.indexOf("DiagPolicy.allows") in 0 until body.indexOf("DiagScrub.line"))
    }

    @Test fun diagnosticsOnlyEverGoToThePrivateRepoAndOnlyIfItIsPrivate() {
        val s = src("diag/LiveDiag.kt")
        assertTrue(s.contains("const val REPO = \"tjshea90/kestrel-private\""))
        assertTrue("the repo is asked whether it is private", s.contains("\"private\\\"\\\\s*:\\\\s*true"))
        assertTrue(s.contains("RepoState.PUBLIC -> {"))
        val hosts = Regex("https?://[a-zA-Z0-9.\\-]+").findAll(s).map { it.value }.toSet()
        assertEquals("the only server this feature talks to", setOf("https://api.github.com"), hosts)
    }

    @Test fun theTokenIsNeverInABackupOrALogOrTheRepo() {
        val backup = src("data/BackupManager.kt") + src("ui/BackupFlow.kt")
        assertFalse(backup.contains("livediag"))
        assertFalse(backup.contains("LiveDiag"))
        val s = src("diag/LiveDiag.kt")
        assertFalse("the token never goes into Health.log", Regex("Health\\.log\\([^)]*token", RegexOption.IGNORE_CASE).containsMatchIn(s))
        assertTrue("a token is kept in the app's own private preferences", s.contains("Context.MODE_PRIVATE"))
        val manifest = File("src/main/AndroidManifest.xml").takeIf { it.isFile }?.readText() ?: ""
        assertFalse(manifest.contains("allowBackup=\"true\""))
    }

    @Test fun healthFeedsTheRecorderAndTheSwitchIsNotBypassed() {
        val h = src("diag/Health.kt")
        assertTrue(h.contains("LiveDiag.observe(layer, event, line)"))
        // nothing else in the app writes to the journal
        val others = File("src/main/kotlin").walkTopDown().filter { it.isFile && it.name.endsWith(".kt") && it.name != "LiveDiag.kt" }.filter { it.readText().contains("LiveDiag.journal") }
        assertEquals(emptyList<File>(), others.toList())
    }
}
