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
        LiveDiag.observe("page", "slow", "load of https://www.example.com/watch/42?t=30s took 9.1 s, session=abc123def456, mail me at someone@example.org")
        val j = LiveDiag.journalForTest()
        assertEquals(1, j.size)
        assertTrue(j[0], j[0].contains("example.com"))
        assertFalse(j[0], j[0].contains("/watch/42"))
        assertFalse(j[0], j[0].contains("abc123def456"))
        assertFalse(j[0], j[0].contains("someone@example.org"))
    }

    @Test fun aSensitiveSiteIsNeverNamed() {
        LiveDiag.observe("page", "slow", "load of https://www.example.com/account/42 took 9.1 s")
        LiveDiag.observe("page", "slow", "load of https://login.examplebank.com/home took 4 s")
        val j = LiveDiag.journalForTest()
        assertEquals(2, j.size)
        for (l in j) { assertTrue(l, l.contains("<sensitive-site>")); assertFalse(l, l.contains("example")) }
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

    // ---- LD2: asleep when off, the tables, the settings snapshot, the advisor

    private fun appPrefs() = ctx.getSharedPreferences("kestrel", Context.MODE_PRIVATE)

    @Test fun offIsAsleepEveryEntryPointDoesNothingAndNothingStaysWaiting() {
        LiveDiag.saveToken("github_pat_FAKE" + "A".repeat(40))
        var asked = 0
        var extra = 0
        LiveDiag.setExtraFacts { extra++; mapOf("slowFrames" to 99L) }
        LiveDiag.onFront { asked++; "" }
        assertTrue("on: a send is waiting for its 2 minutes", LiveDiag.queuedForTest())
        LiveDiag.setEnabled(false)
        assertFalse("off: nothing is left waiting", LiveDiag.queuedForTest())
        org.robolectric.shadows.ShadowLooper.idleMainLooper(130, TimeUnit.SECONDS)
        assertEquals("the delayed send never ran", 0, asked)
        LiveDiag.onFront { asked++; "" }
        assertFalse(LiveDiag.queuedForTest())
        var said = ""
        LiveDiag.sendNow({ asked++; "" }) { said = it }
        assertTrue(said, said.contains("off"))
        assertEquals(emptyList<Any>(), LiveDiag.advice())
        assertEquals(emptyMap<String, Long>(), LiveDiag.facts()); assertEquals(emptyMap<String, String>(), LiveDiag.settingsMap())
        LiveDiag.observe("page", "loaded", "+1.0s page loaded host=example.org paint=10ms done=20ms blocked=0 visit=1 ok=true")
        assertTrue(LiveDiag.statsForTest().isEmpty())
        assertEquals("no supplier was asked", 0, asked); assertEquals("the activity's numbers were not read", 0, extra)
        assertFalse(LiveDiag.applyAdvice(app.kestrel.core.livediag.Advice("x", 1, "w", "s", "a", "b", "tabs_kept", 1, "y")))
        assertFalse("the setting was not touched", appPrefs().contains("tabs_kept"))
    }

    @Test fun finishedLoadsAndEventKindsAreTalliedWhileOn() {
        LiveDiag.observe("page", "loaded", "+1.0s page loaded host=news.example.org paint=500ms done=9000ms blocked=2 visit=1 ok=true")
        LiveDiag.observe("gecko", "onKill", "+2.0s gecko onKill example.org")
        assertEquals("news.example.org", LiveDiag.statsForTest().sites().single().site)
        assertEquals(1L, LiveDiag.statsForTest().count("gecko onKill"))
    }

    @Test fun turningOffErasesTheTablesAndTheirFileToo() {
        repeat(30) { LiveDiag.observe("page", "loaded", "+1.0s page loaded host=a.example.org paint=1ms done=2ms blocked=0 visit=$it ok=true") }
        ioDone()
        val f = File(ctx.filesDir, "livediag.stats")
        assertTrue("the tables are on disk", f.isFile)
        LiveDiag.setEnabled(false)
        assertFalse(f.exists()); assertTrue(LiveDiag.statsForTest().isEmpty())
    }

    @Test fun theSettingsSnapshotHasSwitchesAndNumbersOnlyNeverCodesAddressesOrCounters() {
        appPrefs().edit().putBoolean("sh_ads", false).putInt("tabs_kept", 1).putString("search", "google").putString("phone_code", "ABCD-EFGH-JKLM-NPQR").putInt("phone_port", 5000)
            .putString("glide_words", "secret words here").putLong("blocked_total", 99L).putString("upd_note", "free text").putString("home_url", "https://x.example/").commit()
        val m = LiveDiag.settingsMap()
        assertEquals("false", m["sh_ads"]); assertEquals("1", m["tabs_kept"]); assertEquals("google", m["search"])
        for (bad in listOf("phone_code", "phone_port", "glide_words", "blocked_total", "upd_note", "home_url")) assertFalse(bad, m.containsKey(bad))
        assertFalse(m.values.any { it.contains("ABCD") || it.contains("secret") })
    }

    @Test fun theAdvisorSeesTheTalliesAndATapChangesOnlyAWhitelistedSetting() {
        repeat(2) { LiveDiag.observe("gecko", "onKill", "+2.0s gecko onKill example.org") }
        val a = LiveDiag.advice().first { it.id == "kills-tabs" }
        assertTrue(LiveDiag.applyAdvice(a))
        assertEquals(1, appPrefs().getInt("tabs_kept", 0))
        assertFalse(LiveDiag.applyAdvice(app.kestrel.core.livediag.Advice("x", 1, "w", "s", "a", "b", "phone_code", "x", "y")))
        assertFalse(LiveDiag.applyAdvice(app.kestrel.core.livediag.Advice("x", 1, "w", "s", "a", "b", "tabs_kept", 9, "y")))
        assertFalse("no key: nothing to apply", LiveDiag.applyAdvice(app.kestrel.core.livediag.Advice("x", 1, "w", "s", "a", "b", null, null, "y")))
    }

    @Test fun theFactsCarryNumbersOnlyAndSayWhetherThePhoneIsPairedWithoutItsCode() {
        appPrefs().edit().putString("phone_code", "ABCD-EFGH-JKLM-NPQR").commit()
        LiveDiag.setExtraFacts { mapOf("slowFrames" to 7L) }
        val f = LiveDiag.facts()
        assertEquals(1L, f["phonePaired"]); assertEquals(7L, f["slowFrames"]); assertTrue(f.containsKey("storageFreeMb"))
    }

    @Test fun sleepingAndTheLastCheckBeforeTheSendArePinnedInTheSource() {
        val s = src("diag/LiveDiag.kt")
        assertTrue("off cancels the delayed send", s.substringAfter("private fun sleep()").substringBefore("}\n").contains("removeCallbacks(sendTask)"))
        val up = s.substringAfter("private fun upload(")
        assertTrue("the switch is looked at again just before the bytes go", up.indexOf("if (!enabled) return", up.indexOf("DiagBatch(")) in 0 until up.indexOf("put(DiagBatch.path"))
        for (entry in listOf("fun facts()", "fun settingsMap()", "fun advice()")) assertTrue(entry, s.substringAfter(entry).trimStart().let { it.startsWith("{") || it.startsWith(":") }.and(s.substringAfter(entry).substringAfter("{").trimStart().startsWith("if (!enabled)")))
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

    // ---- LD3 (owner 2026-11-21): "make sure each diagnostic upload is not deleting or overwriting the last upload", and what the first real uploads showed

    private fun kept() = LiveDiag.journalForTest().filterNot { it.contains("s run ") }

    @Test fun everyRunStartsWithADatedMarkerSoClaudeCanTellRunsApartAndNeverThinsItAway() {
        val m = LiveDiag.journalForTest().filter { it.contains("s run start utc=") }
        assertEquals(1, m.size)
        assertTrue(m[0], Regex("""^\+\d+\.\d{3}s run start utc=\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ build=\S+ code=\d+$""").matches(m[0]))
        repeat(200) { LiveDiag.observe("video", "quality", "video quality m.example.com frames=$it dropped=0") }
        assertTrue("the marker is still there after a flood", LiveDiag.journalForTest().any { it.contains("s run start utc=") })
    }

    @Test fun aNoisyKindIsThinnedInTheJournalYetEveryOneIsCountedInTheTable() {
        repeat(200) { LiveDiag.observe("video", "quality", "video quality m.example.com frames=$it dropped=0") }
        repeat(5) { LiveDiag.observe("gecko", "onKill", "gecko onKill the page engine was killed $it") }
        assertEquals(40, kept().count { it.contains("video quality") })
        assertEquals("trouble is never thinned", 5, kept().count { it.contains("onKill") })
        assertEquals(200L, LiveDiag.statsForTest().count("video quality"))
    }

    @Test fun theAdvisorReadsAWeekNotOneUpload() {
        fun load(site: String, ms: Int) = LiveDiag.observe("page", "loaded", "page loaded host=$site paint=900ms done=${ms}ms blocked=1 visit=1 ok=true")
        appPrefs().edit().putString("phone_code", "ABCD-EFGH-JKLM-NPQR").commit()
        repeat(2) { load("slow-one.example.com", 20000); load("slow-two.example.com", 25000) }
        assertTrue("two loads per site are too few for a verdict", LiveDiag.advice().none { it.id == "slow-phone" })
        LiveDiag.foldForTest()                                         // what a successful send does: this upload's table joins the week
        assertTrue("the table of the next upload starts empty", LiveDiag.statsForTest().isEmpty())
        repeat(2) { load("slow-one.example.com", 22000); load("slow-two.example.com", 24000) }
        assertTrue("four loads per site over two uploads are enough", LiveDiag.advice().any { it.id == "slow-phone" })
    }

    @Test fun turningItOffErasesTheWeekToo() {
        LiveDiag.observe("page", "loaded", "page loaded host=a.example.com paint=1ms done=2ms blocked=0 visit=1 ok=true")
        LiveDiag.foldForTest()
        LiveDiag.observe("gecko", "onKill", "gecko onKill x")
        ioDone()
        LiveDiag.setEnabled(false)
        assertTrue(LiveDiag.weekForTest().isEmpty())
        assertFalse(File(ctx.filesDir, "livediag.week").exists())
        assertEquals(emptyList<String>(), LiveDiag.journalForTest())
    }

    @Test fun anUploadIsAlwaysANewFileNeverAReplacementOrADelete() {
        val s = src("diag/LiveDiag.kt")
        val put = s.substringAfter("private fun put(").substringBefore("// ---- what is stored")
        assertTrue(put.contains("requestMethod = \"PUT\""))
        assertFalse("no old-file sha is ever sent: with one GitHub would REPLACE the file", put.contains("sha"))
        assertFalse("the only call that can change a file is the PUT", Regex("\"(DELETE|PATCH|POST)\"").containsMatchIn(s))
        assertTrue("a taken name gets the next free one", s.contains("put(DiagBatch.path(at, installId(p), attempt)") && s.contains("code == 422 && attempt < 2"))
        assertFalse("nothing in the feature removes a file from the repository", Regex("contents/.*DELETE|deleteFile|\"DELETE\"").containsMatchIn(s))
    }

    @Test fun oneUploadRunsAtATimeAndTheOwnButtonDoesNotUseUpTheAutomaticDay() {
        val s = src("diag/LiveDiag.kt")
        assertTrue(s.contains("uploading.compareAndSet(false, true)"))
        assertTrue("the guard is let go however the upload ends", s.contains("finally { uploading.set(false) }"))
        assertTrue(s.contains("if (!manual) e.putString(\"day\", day).putInt(\"today\""))
    }

    @Test fun aSendRefusedOnlyBecauseOfTimeIsAskedAgainWhileTheAppIsInFrontAndNotWhenItLeft() {
        val s = src("diag/LiveDiag.kt")
        assertTrue(s.contains("rearm(DiagSchedule.waitMs(st, now), snapshotOnMain)"))
        assertTrue("no re-arm unless the app is in front", s.substringAfter("private fun rearm(").substringBefore("}\n").contains("!inFront"))
        assertTrue("leaving the front is told to LiveDiag", src("../../../../../../../geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt").contains("LiveDiag.onBack()"))
    }

    @Test fun theJournalIsOnlyEverTrimmedThroughTheCoreJournalSoAGapIsAlwaysCounted() {
        val s = src("diag/LiveDiag.kt")
        assertFalse(s.contains("removeFirst"))
        assertTrue(s.contains("journal.cut(DiagSchedule.MAX_EVENTS)") && s.contains("journal.commit(cut)"))
        assertTrue("the file says how many lines were left out", s.contains("cut.lost") && s.contains("cut.thinned"))
    }
}
