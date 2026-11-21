package app.kestrel.core.livediag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagBatchTest {
    private val b = DiagBatch("ab12cd34", "0.3.0-alpha81", 182, 29, "Alps 711", "2026-11-21T06:40:12Z", listOf("Memory: total 1992 MB", "quote \" and back\\slash"), listOf("+1.0s page loaded news.example.org in 900 ms"), 3)

    @Test fun theFileIsPlainJsonWithEveryLineEscaped() {
        val j = b.toJson()
        assertTrue(j.startsWith("{\n \"v\": 1,")); assertTrue(j.contains("\"install\": \"ab12cd34\"")); assertTrue(j.contains("\"dropped\": 3"))
        assertTrue(j.contains("\"quote \\\" and back\\\\slash\""))
        assertEquals("\"a\\nb\\u0001\"", DiagBatch.q("a\nb\u0001"))
        assertTrue(DiagBatch("i", "v", 1, 29, "m", "t", emptyList(), emptyList(), 0).toJson().contains("\"snapshot\": []"))
    }

    @Test fun theFileGoesInADatedFolderWithTheInstallIdAndNeverOverwritesAnother() {
        assertEquals("diag/2026-11-21/064012-ab12cd34.json", DiagBatch.path("2026-11-21T06:40:12Z", "ab12cd34"))
        assertEquals("diag/undated/000000-x.json", DiagBatch.path("", "x"))
        assertEquals("diag/2026-11-21/064012-abcd.json", DiagBatch.path("2026-11-21T06:40:12Z", "ab/../cd"))      // an id cannot walk out of the folder
    }

    private fun st(enabled: Boolean = true, token: Boolean = true, last: Long = 0, today: Int = 0, fail: Long = 0, fails: Int = 0, pending: Int = 5, crash: Boolean = false) = DiagSchedule.State(enabled, token, last, today, fail, fails, pending, crash)

    @Test fun thereIsAReasonToWaitInEveryOtherCase() {
        val now = 100L * 3600_000L
        assertNull(DiagSchedule.whyNot(st(), now))
        assertEquals("off", DiagSchedule.whyNot(st(enabled = false), now))
        assertEquals("no token yet", DiagSchedule.whyNot(st(token = false), now))
        assertEquals("nothing new to send", DiagSchedule.whyNot(st(pending = 0), now))
        assertEquals("sent recently", DiagSchedule.whyNot(st(last = now - 3600_000L), now))
        assertNull("six hours later", DiagSchedule.whyNot(st(last = now - 7 * 3600_000L), now))
        assertEquals("enough for today", DiagSchedule.whyNot(st(today = 4), now))
        assertEquals("waiting after a failed send", DiagSchedule.whyNot(st(fail = now - 3600_000L, fails = 1), now))
        assertNull("a day after the failure", DiagSchedule.whyNot(st(fail = now - 25 * 3600_000L, fails = 1), now))
        assertEquals("a crash is worth a send even when nothing else is pending", null, DiagSchedule.whyNot(st(pending = 0, crash = true, last = now - 11 * 60_000L), now))
        assertEquals("sent recently", DiagSchedule.whyNot(st(crash = true, last = now - 5 * 60_000L), now))
    }

    @Test fun onlyTheNewestEventsAreKept() {
        val l = (1..700).map { "e$it" }
        val n = DiagSchedule.newest(l, DiagSchedule.MAX_EVENTS)
        assertEquals(600, n.size); assertEquals("e101", n.first()); assertEquals("e700", n.last())
        assertFalse(DiagSchedule.newest(listOf("a"), 5).isEmpty())
    }
}
