package app.kestrel.browser.diag

import android.content.Context
import app.kestrel.browser.Io
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

/** CR1: the note that says how the last run ended (on screen / in the background / on purpose) and why, written only on notable events. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LastRunTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()

    @Before fun clean() { LastRun.resetForTest(); LastRun.lazyMs = 30; ctx.filesDir.deleteRecursively(); ctx.filesDir.mkdirs() }

    /** The note is a process-wide singleton: leave it switched off so no later test in the same JVM writes files into a folder that is gone (and none shares the IO lane with it). */
    @org.junit.After fun off() { ioDoneQuiet(); LastRun.resetForTest(); LastRun.lazyMs = 1500; LastRun.buildOverride = null; LastRun.installAtOverride = null }
    private fun ioDoneQuiet() { try { val l = CountDownLatch(1); Io.exec { l.countDown() }; l.await(5, TimeUnit.SECONDS) } catch (e: Exception) { /* ignore */ } }

    /** The IO lane is one thread in order: when this latch opens, every earlier write is on disk. */
    private fun ioDone() { val l = CountDownLatch(1); Io.exec { l.countDown() }; assertTrue("the IO lane answers", l.await(10, TimeUnit.SECONDS)) }

    /** CR1f: the lazy flush runs on the note's own thread (30 ms in the tests), never on the main looper: wait for it, then for the IO lane. The main looper is NOT idled here on purpose. */
    private fun lazyFlush() { Thread.sleep(200); ioDone() }

    private fun runThatEnds(state: String?, startAt: Long) {
        LastRun.resetForTest()
        LastRun.begin(ctx, startAt)
        ioDone()
        if (state == "fg") LastRun.note("life", "resume", "+1.0s life resume", startAt + 1000)
        if (state == "bg") { LastRun.note("life", "resume", "+1.0s life resume", startAt + 1000); LastRun.note("life", "pause", "+60.0s life pause", startAt + 60_000) }
        if (state == "exit") LastRun.markExit(startAt + 90_000)
        ioDone()
    }

    @Test fun onlyTheLinesThatSayHowARunEndedAreKept() {
        assertTrue(LastRun.wants("life", "resume")); assertTrue(LastRun.wants("life", "destroy"))
        assertTrue(LastRun.wants("mem", "trim")); assertTrue(LastRun.wants("page", "loaded")); assertTrue(LastRun.wants("page", "error"))
        assertTrue(LastRun.wants("gecko", "onKill")); assertTrue(LastRun.wants("gecko", "onCrash")); assertTrue(LastRun.wants("gecko", "recover-after-kill"))
        assertFalse("sizes arrive at every turn", LastRun.wants("gecko", "size"))
        assertFalse(LastRun.wants("ui", "late-beat")); assertFalse(LastRun.wants("touch", "down")); assertFalse(LastRun.wants("mem", "heap")); assertFalse(LastRun.wants("page", "painted"))
    }

    @Test fun aRunThatEndedOnScreenIsReportedAsNotAnOrdinaryCloseWithTheCauseUnknownWhenNothingElseWasLeft() {
        runThatEnds("fg", 1_000_000L)
        LastRun.note("page", "loaded", "+12.0s page loaded host=m.youtube.com paint=900ms done=2400ms blocked=3 visit=1 ok=true", 1_012_000L)
        LastRun.note("mem", "trim", "+20.0s mem trim level=15 avail=212MB thr=144MB low=false trim=15@0s", 1_020_000L)
        lazyFlush()                                                        // the lazy flush of the two lines above
        LastRun.resetForTest()                                              // the process is gone; the next one starts
        LastRun.begin(ctx, 1_600_000L); ioDone()
        val lines = LastRun.reportLines(ctx, 1_700_000L)
        val text = lines.joinToString("\n")
        assertTrue(text, text.contains("WHILE ON SCREEN"))
        assertTrue(text, text.contains("NO crash note and NO engine crash report"))
        assertTrue("the last events ride along: $text", text.contains("host=m.youtube.com") && text.contains("mem trim level=15"))
        assertTrue("kept for a week: $text", File(ctx.filesDir, "last-run-ended.txt").isFile)
    }

    @Test fun aRunThatWasInTheBackgroundOrClosedOnPurposeIsAnOrdinaryEndAndIsNotKept() {
        runThatEnds("bg", 2_000_000L)
        LastRun.resetForTest(); LastRun.begin(ctx, 2_500_000L); ioDone()
        val bg = LastRun.reportLines(ctx, 2_600_000L).joinToString("\n")
        assertTrue(bg, bg.contains("in the background when it ended") && bg.contains("not a crash"))
        assertFalse(File(ctx.filesDir, "last-run-ended.txt").exists())

        runThatEnds("exit", 3_000_000L)
        LastRun.resetForTest(); LastRun.begin(ctx, 3_500_000L); ioDone()
        val exit = LastRun.reportLines(ctx, 3_600_000L).joinToString("\n")
        assertTrue(exit, exit.contains("ended on purpose"))
        assertFalse(File(ctx.filesDir, "last-run-ended.txt").exists())
    }

    @Test fun anUncaughtJavaExceptionDuringTheRunIsNamedAsTheCause() {
        runThatEnds("fg", System.currentTimeMillis() - 60_000L)
        CrashNote.write(ctx, Thread.currentThread(), RuntimeException("boom"))        // written now = inside the run's window
        LastRun.resetForTest(); LastRun.begin(ctx, System.currentTimeMillis()); ioDone()
        val text = LastRun.reportLines(ctx).joinToString("\n")
        assertTrue(text, text.contains("an uncaught Java exception"))
        assertFalse(text, text.contains("NO crash note"))
    }

    @Test fun anEngineCrashReportDuringTheRunPointsAtTheEngineAndARestartOfTheTabletIsToldApart() {
        val started = System.currentTimeMillis() - 120_000L
        runThatEnds("fg", started)
        val dump = File(ctx.filesDir, "mozilla/abc.default/minidumps").also { it.mkdirs() }
        File(dump, "x.extra").writeText("{\"ProcessType\":\"default\"}")
        LastRun.resetForTest(); LastRun.begin(ctx, System.currentTimeMillis()); ioDone()
        assertTrue(LastRun.reportLines(ctx).joinToString("\n").contains("probably the engine"))

        val note = "kestrel-last-run 1\nbuild=x\nstarted=1000\nstate=fg\nstateAt=2000\nsaved=5000\navailMb=300\nnextStart=900000\nbootAt=700000\ncrashAt=0\nengineAt=0\n"
        val r = LastRun.read(note)
        assertTrue(r.abnormal)
        assertTrue(r.lines.joinToString("\n"), r.lines.joinToString("\n").contains("the whole tablet restarted"))
    }

    @Test fun theFileHoldsAtMostFortyEventsAndNothingBeforeBegin() {
        LastRun.note("life", "resume", "+0.1s life resume")                    // before begin(): ignored, no file
        assertFalse(File(ctx.filesDir, "last-run.txt").exists())
        LastRun.begin(ctx, 4_000_000L); ioDone()
        for (i in 1..60) LastRun.note("page", "loaded", "+$i.0s page loaded host=h$i.example", 4_000_000L + i * 1000)
        lazyFlush()
        val text = File(ctx.filesDir, "last-run.txt").readText()
        assertEquals(40, text.lines().count { it.startsWith("ev=") })
        assertTrue("the newest lines win: $text", text.contains("host=h60.example") && !text.contains("host=h1.example"))
        assertTrue(text.length < 9000)
    }

    @Test fun aNoteIsForgottenAfterAWeekAndTheDiagnosticsReportCarriesTheLines() {
        runThatEnds("fg", 5_000_000L)
        LastRun.resetForTest(); LastRun.begin(ctx, 5_500_000L); ioDone()
        val kept = File(ctx.filesDir, "last-run-ended.txt"); assertTrue(kept.isFile)
        runThatEnds("bg", 6_000_000L)
        LastRun.resetForTest(); LastRun.begin(ctx, 6_500_000L); ioDone()
        assertTrue("the earlier unquiet run is still told while it is young", LastRun.reportLines(ctx).joinToString("\n").contains("did not end quietly"))
        kept.setLastModified(System.currentTimeMillis() - 8L * 86_400_000)
        assertFalse(LastRun.reportLines(ctx).joinToString("\n").contains("did not end quietly"))
        assertNotNull(Health.report(ctx).takeIf { it.contains("Last run") })
    }

    @Test fun theNoteIsWrittenEvenWhileTheMainThreadIsFrozenAndAStallIsNamedAsTheCause() {
        LastRun.begin(ctx, 7_000_000L); ioDone()
        LastRun.note("life", "resume", "+1.0s life resume", 7_001_000L)
        LastRun.note("page", "loaded", "+9.0s page loaded host=doraby.com paint=1700ms done=6300ms blocked=1 visit=1 ok=true", 7_009_000L)
        LastRun.note("ui", "STALL", "+12.0s ui STALL 2600ms: the UI thread is blocked at", 7_012_000L)
        LastRun.note("ui", "  at", "+12.0s ui   at java.lang.Object.wait(Native Method)", 7_012_000L)
        lazyFlush()                                                   // (the main looper was never idled: a frozen screen thread cannot stop the write)
        val text = File(ctx.filesDir, "last-run.txt").readText()
        assertTrue(text, text.contains("host=doraby.com") && text.contains("ui STALL") && text.contains("java.lang.Object.wait"))
        LastRun.resetForTest(); LastRun.lazyMs = 30
        LastRun.begin(ctx, 7_100_000L); ioDone()
        val report = LastRun.reportLines(ctx, 7_200_000L).joinToString("\n")
        assertTrue(report, report.contains("SCREEN THREAD WAS FROZEN") && report.contains("a freeze, not a crash"))
        assertFalse(report, report.contains("NO crash note"))
    }

    @Test fun theStartAndTheStallLinesAreKeptButTouchesAndFramesAreNot() {
        assertTrue(LastRun.wants("ui", "STALL")); assertTrue(LastRun.wants("ui", "  at")); assertFalse(LastRun.wants("ui", "late-beat")); assertFalse(LastRun.wants("ui", "slow"))
        assertTrue(LastRun.wants("start", "paint")); assertTrue(LastRun.wants("tabs", "early-cover")); assertTrue(LastRun.wants("engine", "warm-start"))
        assertFalse(LastRun.wants("start", "frame")); assertFalse(LastRun.wants("tabs", "look-saved"))
        assertTrue("DG1: whether the app was compiled rides along in the note of a run that died early", LastRun.wants("start", "oat") && LastRun.wants("start", "art"))
    }

    @Test fun theThreeNewestRunsThatDidNotEndQuietlyAreKeptAndAFourthDropsTheOldest() {
        val starts = longArrayOf(8_000_000L, 8_500_000L, 9_000_000L, 9_500_000L)
        for (st in starts) { runThatEnds("fg", st); LastRun.resetForTest(); LastRun.lazyMs = 30; LastRun.begin(ctx, st + 100_000L); ioDone() }
        val text = LastRun.reportLines(ctx, 9_700_000L).joinToString("\n")
        assertTrue(text, text.contains("EARLIER run") && text.contains("EVEN EARLIER"))
        val stamps = Regex("started (\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d)").findAll(text).map { it.groupValues[1] }.toList()
        assertEquals("the newest three, each told once: $text", 3, stamps.size)
        assertEquals(3, stamps.toSet().size)
        assertTrue(File(ctx.filesDir, "last-run-ended-3.txt").isFile)
    }

    private fun note(avail: Int, mem: String) = "kestrel-last-run 1\nbuild=x\nstarted=1000\nstate=fg\nstateAt=2000\nsaved=5000\navailMb=$avail\n" + (if (mem.isNotEmpty()) "mem=$mem\n" else "") + "nextStart=900000\nbootAt=0\ncrashAt=0\nengineAt=0\nev=+1.0s life resume\n"

    @Test fun aKillWithLowMemoryIsNamedAsTheMemoryKillerAndTheMemoryOfEveryProcessIsShown() {
        val low = LastRun.read(note(210, "free=40MB avail=210MB cached=300MB swapfree=0MB psi{some avg10=31.50 avg60=20.10 avg300=9.00 t=123 full avg10=12.00 avg60=7.00 avg300=3.00 t=456} app{main=310MB sandboxed_tab=640MB gpu=250MB utility=120MB sum=1320MB}"))
        val t = low.lines.joinToString("\n")
        assertTrue(t, t.contains("memory at the last note: free=40MB") && t.contains("sandboxed_tab=640MB") && t.contains("sum=1320MB"))
        assertTrue(t, t.contains("memory was LOW") && t.contains("memory killer"))
        val fine = LastRun.read(note(900, "free=500MB avail=900MB cached=300MB swapfree=0MB app{main=310MB sum=310MB}")).lines.joinToString("\n")
        assertTrue(fine, fine.contains("ended from outside") && fine.contains("held about 310 MB"))
        assertFalse(fine, fine.contains("memory was LOW"))
    }

    @Test fun theNoteGetsAMemorySampleAtMostEveryFifteenSecondsWhileTheAppIsInFront() {
        LastRun.begin(ctx, 20_000_000L); ioDone()
        val t0 = System.currentTimeMillis()
        LastRun.tick(t0 + 100)                                                       // still "starting": nothing
        ioDone(); assertFalse(File(ctx.filesDir, "last-run.txt").readText().contains("state=fg"))
        LastRun.note("life", "resume", "+1.0s life resume", t0); ioDone()
        val first = File(ctx.filesDir, "last-run.txt").readText()
        assertTrue(first, first.contains("state=fg") && first.contains("mem=free="))
        LastRun.tick(t0 + 1_000); ioDone()
        assertEquals("too soon: not written again", first, File(ctx.filesDir, "last-run.txt").readText())
        LastRun.tick(t0 + 20_000); ioDone()
        assertTrue("15 s later it is", File(ctx.filesDir, "last-run.txt").readText().contains("saved=${t0 + 20_000}"))
    }

    /** DG1: a stored note of a run that was on screen, with the facts the NEXT start adds (its build and the time that build was installed). */
    private fun runNote(build: String = "0.3.0-alpha70 (code 171)", started: Long, installAt: Long, stateAt: Long, saved: Long, nextStart: Long, nextBuild: String, nextInstallAt: Long, crashAt: Long = 0L, extra: String = "") =
        "kestrel-last-run 1\npid=1\nbuild=$build\nstarted=$started\ninstallAt=$installAt\nstate=fg\nstateAt=$stateAt\nsaved=$saved\navailMb=959\nmem=free=51MB avail=959MB cached=966MB swapfree=1473MB app{main=300MB sum=300MB}\n$extra" +
            "ev=+1.2s life resume\nnextStart=$nextStart\nnextBuild=$nextBuild\nnextInstallAt=$nextInstallAt\nbootAt=0\ncrashAt=$crashAt\nengineAt=0\n"

    @Test fun aRunThatWasOnScreenWhenTheNextBuildWasInstalledOverItIsAnUpdateAndNotACrash() {
        val r = LastRun.read(runNote(started = 1_000_000L, installAt = 900_000L, stateAt = 1_001_000L, saved = 1_500_000L, nextStart = 1_507_000L, nextBuild = "0.3.0-alpha71 (code 172)", nextInstallAt = 1_503_000L))
        val t = r.lines.joinToString("\n")
        assertFalse("an update is not an abnormal end, so it is not kept as one: $t", r.abnormal)
        assertTrue(t, t.contains("ended by an UPDATE") && t.contains("not a crash"))
        assertFalse(t, t.contains("WHILE ON SCREEN") || t.contains("NO crash note"))
    }

    @Test fun aNewerBuildThatWasInstalledMuchLaterOrAfterACrashDoesNotHideTheEnd() {
        val later = LastRun.read(runNote(started = 1_000_000L, installAt = 900_000L, stateAt = 1_001_000L, saved = 1_500_000L, nextStart = 9_000_000L, nextBuild = "0.3.0-alpha71 (code 172)", nextInstallAt = 8_990_000L))
        assertTrue("the install came hours after the last note: nothing says the update ended it", later.abnormal)
        val crash = LastRun.read(runNote(started = 1_000_000L, installAt = 900_000L, stateAt = 1_001_000L, saved = 1_500_000L, nextStart = 1_507_000L, nextBuild = "0.3.0-alpha71 (code 172)", nextInstallAt = 1_503_000L, crashAt = 1_400_000L))
        assertTrue("a crash note inside the run wins over the update", crash.abnormal && crash.lines.joinToString("\n").contains("an uncaught Java exception"))
        val same = LastRun.read(runNote(started = 1_000_000L, installAt = 900_000L, stateAt = 1_001_000L, saved = 1_500_000L, nextStart = 1_507_000L, nextBuild = "0.3.0-alpha70 (code 171)", nextInstallAt = 1_503_000L))
        assertTrue("the same build started again: no update", same.abnormal)
    }

    @Test fun aRunThatLivedSecondsAfterTheFirstStartFollowingAnInstallIsToldAsThatWithItsRealLifeInSeconds() {
        val r = LastRun.read(runNote(started = 2_000_000L, installAt = 1_996_000L, stateAt = 2_001_000L, saved = 2_009_000L, nextStart = 9_000_000L, nextBuild = "0.3.0-alpha70 (code 171)", nextInstallAt = 1_996_000L))
        val t = r.lines.joinToString("\n")
        assertTrue(t, r.abnormal)
        assertTrue("seconds, never '0 min': $t", t.contains("ran about 9 s"))
        assertTrue(t, t.contains("FIRST START after its build was installed") && t.contains("4 s after the install"))
        assertTrue(t, t.contains("look at the install itself") && t.contains("held about 300 MB"))
        val longRun = LastRun.read(runNote(started = 2_000_000L, installAt = 100_000L, stateAt = 2_001_000L, saved = 2_000_000L + 27 * 60_000L, nextStart = 9_000_000L, nextBuild = "0.3.0-alpha70 (code 171)", nextInstallAt = 100_000L)).lines.joinToString("\n")
        assertTrue(longRun, longRun.contains("ran about 27 min") && !longRun.contains("FIRST START") && longRun.contains("ended from outside"))
    }

    @Test fun theNextStartWritesItsOwnBuildAndInstallTimeIntoTheNoteAndAnUpdateIsRecognisedEndToEnd() {
        LastRun.buildOverride = "0.3.0-alpha70 (code 171)"; LastRun.installAtOverride = System.currentTimeMillis() - 3_600_000L
        val t0 = System.currentTimeMillis() - 30_000L
        LastRun.begin(ctx, t0); ioDone()
        LastRun.note("life", "resume", "+1.0s life resume", t0 + 1000); ioDone()
        assertTrue(File(ctx.filesDir, "last-run.txt").readText().let { it.contains("build=0.3.0-alpha70 (code 171)") && it.contains("installAt=") })
        LastRun.resetForTest(); LastRun.lazyMs = 30
        LastRun.buildOverride = "0.3.0-alpha71 (code 172)"; LastRun.installAtOverride = System.currentTimeMillis() - 1_000L
        LastRun.begin(ctx, System.currentTimeMillis()); ioDone()
        val text = LastRun.reportLines(ctx).joinToString("\n")
        assertTrue(text, text.contains("ended by an UPDATE"))
        assertFalse("an update is not kept as an abnormal end: $text", File(ctx.filesDir, "last-run-ended.txt").exists())
    }

    @Test fun theFirstMinuteOfARunIsSampledEveryTwoSecondsAndTheLaterOnesEveryFifteen() {
        val t0 = System.currentTimeMillis()
        LastRun.begin(ctx, t0); ioDone()
        LastRun.note("life", "resume", "+1.0s life resume", t0); ioDone()
        LastRun.tick(t0 + 1_000); ioDone()
        assertFalse("1 s after the flush: too soon even in the first minute", File(ctx.filesDir, "last-run.txt").readText().contains("saved=${t0 + 1_000}"))
        LastRun.tick(t0 + 2_600); ioDone()
        assertTrue("2 s later it is written while the run is young", File(ctx.filesDir, "last-run.txt").readText().contains("saved=${t0 + 2_600}"))
        LastRun.tick(t0 + 70_000); ioDone()
        LastRun.tick(t0 + 73_000); ioDone()
        assertFalse("after the first minute it is every 15 s again", File(ctx.filesDir, "last-run.txt").readText().contains("saved=${t0 + 73_000}"))
    }
}
