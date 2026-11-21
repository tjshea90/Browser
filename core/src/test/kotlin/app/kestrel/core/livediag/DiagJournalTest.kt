package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LD3 (owner 2026-11-21, "make sure each upload is not deleting or overwriting the last"): what the first real uploads showed, as tests. */
class DiagJournalTest {
    private fun fill(j: DiagJournal, n: Int, kind: String = "page slow", from: Int = 0) { for (i in from until from + n) j.add(kind, "+$i.0s $kind line $i") }

    @Test fun theRealFailureAJournalOfSixHundredAndNineSentSixHundredAndNothingIsSentTwiceOrLostSilently() {
        val j = DiagJournal(800, 40)
        fill(j, 609)
        val c = j.cut(600)
        assertEquals(600, c.lines.size)
        assertEquals("the newest 600 go", "+9.0s page slow line 9", c.lines.first())
        assertEquals("the 9 that did not fit are SAID (the old code reported 0)", 9, c.lost)
        j.commit(c)
        assertTrue("nothing is left to be sent again (the old code re-sent the 9 newest lines)", j.isEmpty())
        assertEquals(0, j.cut(600).lost)
    }

    @Test fun linesThatArriveWhileTheUploadRunsStay() {
        val j = DiagJournal(800, 40)
        fill(j, 10)
        val c = j.cut(600)
        fill(j, 3, from = 10)
        j.commit(c)
        assertEquals(listOf("+10.0s page slow line 10", "+11.0s page slow line 11", "+12.0s page slow line 12"), j.toList())
    }

    @Test fun linesThatFellOffTheFrontWhileTheUploadRanAreNotMistakenForSentOnes() {
        val j = DiagJournal(5, 40)
        fill(j, 5)
        val c = j.cut(5)                       // lines 0..4
        fill(j, 4, from = 5)                   // 5..8 arrive; 0..3 fall off the front (they WERE sent)
        j.commit(c)                            // only line 4 is left of the sent ones
        assertEquals(listOf(5, 6, 7, 8).map { "+$it.0s page slow line $it" }, j.toList())
    }

    @Test fun aFailedSendKeepsEverythingForTheNextTry() {
        val j = DiagJournal(800, 40)
        fill(j, 20)
        j.cut(600)                             // the upload failed: commit is never called
        assertEquals(20, j.size())
        assertEquals(20, j.cut(600).lines.size)
    }

    @Test fun theSizeLimitAndTheScrubRefusalsAreCountedNotHidden() {
        val j = DiagJournal(10, 40)
        fill(j, 14)                            // 4 fall off the front
        j.lose(); j.lose()                     // the scrub refused two lines
        val c = j.cut(600)
        assertEquals(6, c.lost)
        j.commit(c)
        assertEquals("after the file carried the number it is not carried again", 0, j.cut(600).lost)
    }

    @Test fun aNoisyKindIsThinnedAndSaidSoYetTroubleIsNeverThinned() {
        val j = DiagJournal(800, 5)
        fill(j, 200, kind = "video quality")
        fill(j, 50, kind = "gecko onKill")
        fill(j, 50, kind = "page loaded")
        fill(j, 50, kind = "ui late-beat")
        fill(j, 50, kind = "mem trim")
        val c = j.cut(600)
        assertEquals(5, c.lines.count { it.contains("video quality") })
        assertEquals(195, c.thinned)
        assertEquals(50, c.lines.count { it.contains("gecko onKill") })
        assertEquals(50, c.lines.count { it.contains("page loaded") })
        assertEquals(50, c.lines.count { it.contains("late-beat") })
        assertEquals(50, c.lines.count { it.contains("mem trim") })
    }

    @Test fun theRealSpamNoLongerPushesTheNewsOut() {
        val j = DiagJournal(800, 40)
        fill(j, 900, kind = "video quality")                         // a film watched for 3 hours
        j.add("gecko onKill", "+5000.0s gecko onKill the page engine was killed")
        assertTrue(j.toList().any { it.contains("onKill") })
        assertEquals(41, j.size())
    }

    @Test fun theWindowStartsAgainAfterASend() {
        val j = DiagJournal(800, 3)
        fill(j, 10, kind = "video quality")
        j.commit(j.cut(600))
        fill(j, 10, kind = "video quality", from = 100)
        assertEquals(3, j.size())
        assertEquals(0, j.cut(600).thinned.let { 0 } + (j.cut(600).thinned - 7))
    }

    @Test fun storedLinesComeBackAndKeepTheNoisyKindCount() {
        val j = DiagJournal(800, 3)
        j.restore((0 until 10).map { "+$it.0s video quality m.example.com frames=1" } + listOf("+11.0s gecko onKill x"))
        assertEquals(11, j.size())
        assertFalse("the window knows 3 were already shown... and the rest were stored before the limit existed", j.add("video quality", "+12.0s video quality x"))
        assertTrue(j.add("gecko onKill", "+13.0s gecko onKill y"))
    }

    @Test fun clearForgetsEverything() {
        val j = DiagJournal(10, 40)
        fill(j, 14); j.lose()
        j.clear()
        assertTrue(j.isEmpty()); assertEquals(0, j.cut(5).lost)
    }

    @Test fun mergingTwoTablesAddsCountsAndKeepsTheWorstTime() {
        val a = DiagStats(); val b = DiagStats()
        a.observe("gecko", "onKill", "+1.0s gecko onKill x"); b.observe("gecko", "onKill", "+2.0s gecko onKill x"); b.observe("gecko", "onKill", "+3.0s gecko onKill x")
        a.observe("page", "loaded", "+4.0s page loaded host=slow.example.net paint=900ms done=30000ms blocked=1 visit=1 ok=true")
        b.observe("page", "loaded", "+5.0s page loaded host=slow.example.net paint=700ms done=14000ms blocked=1 visit=1 ok=false")
        b.observe("page", "loaded", "+6.0s page loaded host=slow.example.net paint=800ms done=16000ms blocked=1 visit=1 ok=true")
        val week = DiagStats(); week.mergeFrom(a); week.mergeFrom(b)
        assertEquals(3L, week.count("gecko onKill"))
        val s = week.sites().single()
        assertEquals(3, s.loads); assertEquals(1, s.failed); assertEquals(30000L, s.worstDoneMs); assertEquals(16000L, s.medianDoneMs); assertEquals(800L, s.avgPaintMs)
        assertEquals("the sources are untouched", 1L, a.count("gecko onKill"))
        week.mergeFrom(week)                                         // merging into itself changes nothing
        assertEquals(3L, week.count("gecko onKill"))
    }

    @Test fun mergingRespectsTheTableLimits() {
        val a = DiagStats(maxKinds = 2, maxSites = 1); val b = DiagStats()
        for (k in listOf("a x", "b x", "c x")) b.observe("a", k.substringAfter(' '), "+1.0s $k")
        for (h in listOf("one.example.com", "two.example.com")) b.observe("page", "loaded", "+1.0s page loaded host=$h paint=1ms done=2ms blocked=0 visit=1 ok=true")
        a.mergeFrom(b)
        assertTrue(a.kindCounts().size <= 3)           // 2 named + "other kinds"
        assertEquals(1, a.sites().size)
    }
}

class DiagScheduleWaitTest {
    private fun st(enabled: Boolean = true, token: Boolean = true, last: Long = 0, today: Int = 0, fail: Long = 0, fails: Int = 0, pending: Int = 5, crash: Boolean = false) = DiagSchedule.State(enabled, token, last, today, fail, fails, pending, crash)
    private val now = 100L * 3600_000L

    @Test fun whenOnlyTimeIsInTheWayItSaysHowLongSoACheckCanBeSetAgain() {
        assertEquals(0L, DiagSchedule.waitMs(st(), now))
        assertEquals(3L * 3600_000L, DiagSchedule.waitMs(st(last = now - 3L * 3600_000L), now))
        assertEquals(5L * 60_000L, DiagSchedule.waitMs(st(crash = true, last = now - 5L * 60_000L), now))
        assertEquals(23L * 3600_000L, DiagSchedule.waitMs(st(fail = now - 1L * 3600_000L, fails = 1), now))
        assertEquals(0L, DiagSchedule.waitMs(st(last = now - 7L * 3600_000L), now))
    }

    @Test fun whenTimeWillNotFixItTheAnswerIsMinusOne() {
        assertEquals(-1L, DiagSchedule.waitMs(st(enabled = false), now))
        assertEquals(-1L, DiagSchedule.waitMs(st(token = false), now))
        assertEquals(-1L, DiagSchedule.waitMs(st(pending = 0), now))
        assertEquals(-1L, DiagSchedule.waitMs(st(today = 4), now))
    }

    @Test fun waitAgreesWithWhyNot() {
        for (last in listOf(0L, now - 60_000L, now - 7L * 3600_000L)) for (crash in listOf(false, true)) {
            val s = st(last = last, crash = crash)
            val w = DiagSchedule.waitMs(s, now)
            assertEquals("whyNot=" + DiagSchedule.whyNot(s, now) + " wait=" + w, DiagSchedule.whyNot(s, now) == null, w == 0L)
        }
    }

    @Test fun twoUploadsInTheSameSecondGetTwoNamesAndNeverTheSameOne() {
        val a = DiagBatch.path("2026-11-21T06:40:12Z", "ab12cd34")
        val b = DiagBatch.path("2026-11-21T06:40:12Z", "ab12cd34", 1)
        val c = DiagBatch.path("2026-11-21T06:40:12Z", "ab12cd34", 2)
        assertEquals("diag/2026-11-21/064012-ab12cd34.json", a)
        assertEquals("diag/2026-11-21/064012-ab12cd34-2.json", b)
        assertEquals("diag/2026-11-21/064012-ab12cd34-3.json", c)
        assertNotEquals(a, b); assertNotEquals(b, c)
    }
}
