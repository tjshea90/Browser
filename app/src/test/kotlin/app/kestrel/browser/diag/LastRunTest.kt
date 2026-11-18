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
import org.robolectric.shadows.ShadowLooper

/** CR1: the note that says how the last run ended (on screen / in the background / on purpose) and why, written only on notable events. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LastRunTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()

    @Before fun clean() { LastRun.resetForTest(); ctx.filesDir.deleteRecursively(); ctx.filesDir.mkdirs() }

    /** The note is a process-wide singleton: leave it switched off so no later test in the same JVM writes files into a folder that is gone (and none shares the IO lane with it). */
    @org.junit.After fun off() { ioDoneQuiet(); LastRun.resetForTest() }
    private fun ioDoneQuiet() { try { val l = CountDownLatch(1); Io.exec { l.countDown() }; l.await(5, TimeUnit.SECONDS) } catch (e: Exception) { /* ignore */ } }

    /** The IO lane is one thread in order: when this latch opens, every earlier write is on disk. */
    private fun ioDone() { val l = CountDownLatch(1); Io.exec { l.countDown() }; assertTrue("the IO lane answers", l.await(10, TimeUnit.SECONDS)) }

    /** The lazy flush waits 1.5 s on the main looper: advance its clock, then wait for the write. */
    private fun lazyFlush() { ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS); ioDone() }

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
        assertFalse(LastRun.wants("ui", "STALL")); assertFalse(LastRun.wants("touch", "down")); assertFalse(LastRun.wants("mem", "heap")); assertFalse(LastRun.wants("page", "painted"))
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

    @Test fun theFileHoldsAtMostTwentyFourEventsAndNothingBeforeBegin() {
        LastRun.note("life", "resume", "+0.1s life resume")                    // before begin(): ignored, no file
        assertFalse(File(ctx.filesDir, "last-run.txt").exists())
        LastRun.begin(ctx, 4_000_000L); ioDone()
        for (i in 1..60) LastRun.note("page", "loaded", "+$i.0s page loaded host=h$i.example", 4_000_000L + i * 1000)
        lazyFlush()
        val text = File(ctx.filesDir, "last-run.txt").readText()
        assertEquals(24, text.lines().count { it.startsWith("ev=") })
        assertTrue("the newest lines win: $text", text.contains("host=h60.example") && !text.contains("host=h1.example"))
        assertTrue(text.length < 6000)
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
}
