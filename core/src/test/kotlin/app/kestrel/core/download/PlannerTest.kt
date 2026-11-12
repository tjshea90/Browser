package app.kestrel.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    private val MiB = 1L shl 20

    private fun running(start: Long, end: Long, cursor: Long, reqEnd: Long) = Seg(0, start, end, cursor).also { it.worker = true; it.reqEnd = reqEnd }

    @Test fun windows() {
        assertEquals(8 * MiB, Planner.window(Seg(0, 0, 100 * MiB, 0), 8 * MiB))
        assertEquals(512L shl 10, Planner.window(Seg(0, 0, 100 * MiB, 99 * MiB), 8 * MiB))   // 1 MiB left -> 512 KiB
        assertEquals(100L shl 10, Planner.window(Seg(0, 0, 100 * MiB, 100 * MiB - (100L shl 10)), 8 * MiB)) // never more than what is left
        assertEquals(8 * MiB, Planner.window(Seg(0, 0, -1, 0), 8 * MiB))
    }

    @Test fun idleSegmentsFirstThenSteal() {
        val a = running(0, 32 * MiB, 1 * MiB, 8 * MiB)
        val idle = Seg(1, 32 * MiB, 40 * MiB, 32 * MiB)
        val segs = mutableListOf(a, idle)
        assertEquals(idle, Planner.nextWork(segs, 0))     // idle work before splitting anything
        idle.worker = true; idle.reqEnd = 33 * MiB
        val t = Planner.nextWork(segs, 0)!!
        assertEquals(3, segs.size)
        assertTrue(t.start > 8 * MiB)                      // beyond the victim's in-flight window
        assertEquals(0L, t.start % Planner.ALIGN)
        assertEquals(t.start, a.end)                       // the victim now ends where the thief starts
        assertNull(Planner.check(segs, 40 * MiB))
    }

    @Test fun smallTailsAreNotSplit() {
        val a = running(0, 5 * MiB, 1 * MiB, 2 * MiB)     // 3 MiB tail < 4 MiB
        assertNull(Planner.nextWork(mutableListOf(a), 0))
        val b = running(0, 6 * MiB, 1 * MiB, 2 * MiB)     // exactly 4 MiB: split in two 2 MiB halves
        assertNotNull(Planner.nextWork(mutableListOf(b), 0))
        assertNull(Planner.nextWork(mutableListOf(running(0, -1, 5, -1)), 0)) // unknown length: nothing to split
    }

    @Test fun openEndedReplyIsCutAheadOfItsCursor() {
        val live = running(0, 64 * MiB, 10 * MiB, -1)
        val segs = mutableListOf(live)
        val t = Planner.nextWork(segs, 3 * MiB)!!
        assertTrue(t.start >= 10 * MiB + 3 * MiB)         // keeps data in flight ahead of the cut
        assertEquals(t.start, live.end)
        assertNull(Planner.check(segs, 64 * MiB))
    }

    @Test fun undoSplitGivesTheRangeBack() {
        val a = running(0, 32 * MiB, 1 * MiB, 8 * MiB)
        val segs = mutableListOf(a)
        val t = Planner.nextWork(segs, 0)!!
        assertTrue(Planner.undoSplit(segs, t))
        assertEquals(32 * MiB, a.end); assertEquals(1, segs.size)
        val t2 = Planner.nextWork(segs, 0)!!
        t2.cursor += 10                                    // a thief that already wrote something cannot be undone
        assertFalse(Planner.undoSplit(segs, t2))
    }

    /** Random walks: whatever the workers do, the ranges stay disjoint, cover the file and keep cursors inside. */
    @Test fun invariantsHoldUnderRandomWork() {
        val rnd = kotlin.random.Random(42)
        repeat(300) {
            val total = (8 + rnd.nextInt(120)) * MiB
            val segs = mutableListOf(Seg(0, 0, total, 0).also { it.worker = true; it.reqEnd = if (rnd.nextBoolean()) -1 else minOf(total, 8 * MiB) })
            repeat(60) {
                when (rnd.nextInt(4)) {
                    0 -> Planner.nextWork(segs, rnd.nextInt(6) * MiB.toInt().toLong())?.let { it.worker = true; it.reqEnd = minOf(it.end, it.cursor + Planner.window(it, 8 * MiB)) }
                    1 -> segs.filter { it.worker }.randomOrNull(rnd)?.let { s -> // progress inside the window / up to the (possibly shortened) end
                        val limit = if (s.reqEnd >= 0) minOf(s.reqEnd, s.end) else s.end
                        if (limit > s.cursor) s.cursor += (rnd.nextDouble() * (limit - s.cursor)).toLong() }
                    2 -> segs.filter { it.worker && it.reqEnd >= 0 && it.cursor >= minOf(it.reqEnd, it.end) }.randomOrNull(rnd)?.let { s -> // window finished: ask for the next one
                        s.reqEnd = if (s.cursor >= s.end) s.end else minOf(s.end, s.cursor + Planner.window(s, 8 * MiB)) }
                    3 -> segs.filter { it.worker && it.cursor >= it.end }.randomOrNull(rnd)?.let { it.worker = false }
                }
                assertNull(Planner.check(segs, total))
            }
        }
    }

    @Test fun capRules() {
        val cfg = Config()
        val r = Rec(1).also { it.total = 100 * MiB; it.etag = "\"x\""; it.ranges = 1 }
        assertEquals(4, Planner.cap(r, cfg, 1_000_000.0, 5, 1).coerceAtMost(4))
        r.ranges = 0; assertEquals(1, Planner.cap(r, cfg, 1_000_000.0, 5, 1))                    // the server said no
        r.ranges = -1; r.etag = null; assertEquals(1, Planner.cap(r, cfg, 1_000_000.0, 5, 1))    // no validator: never split
        r.etag = "\"x\""; r.done = 99 * MiB; assertEquals(2, Planner.cap(r, cfg, 1_000_000.0, 5, 2)) // little left: keep what runs
        r.done = 0; assertEquals(1, Planner.cap(Rec(2).also { it.total = -1; it.etag = "\"x\"" }, cfg, 0.0, 5, 1))
        assertEquals(1, Planner.cap(r, Config(parallel = false), 0.0, 5, 1))
    }

    @Test fun hillClimbFindsTheLinkLimit() {
        // a link that gives each connection 1 MB/s up to 3 MB/s in total: should settle near 3 connections
        val cfg = Config(rampMs = 1000, evalMs = 4000, noGrowthMs = 30_000)
        val c = HillClimb(cfg)
        var now = 0L
        var n = 1
        repeat(40) { now += 1000; n = c.update(now, minOf(n * 1_000_000.0, 3_000_000.0), 6) }
        assertEquals(3, n) // grew to 4, saw no gain, stepped back to 3 and froze growth
        // a link that does not scale: stays at 1 and stops trying for a while
        val d = HillClimb(cfg)
        var m = 1; now = 0
        val seen = HashSet<Int>()
        repeat(12) { now += 1000; m = d.update(now, 1_000_000.0, 6); seen.add(m) }
        assertEquals(1, m)
        // with only one connection allowed it never grows
        val e = HillClimb(cfg); now = 0
        repeat(10) { now += 1000; assertEquals(1, e.update(now, 1_000_000.0, 1)) }
    }

    @Test fun aBigFileStartsWithTwoConnectionsAndSmallOnesDoNot() {
        val cfg = Config(rampMs = 1500, evalMs = 4000, fastStartMinBytes = 8L shl 20)
        val big = HillClimb(cfg)
        assertEquals("a 50 MB file wants two at once, before the ramp", 2, big.update(0, 0.0, 3, 50L shl 20))
        val small = HillClimb(cfg)
        assertEquals(1, small.update(0, 0.0, 3, 5L shl 20))
        val unknown = HillClimb(cfg)
        assertEquals(1, unknown.update(0, 0.0, 3, -1L))
        val capped = HillClimb(cfg)
        assertEquals("one allowed: one", 1, capped.update(0, 0.0, 1, 50L shl 20))
        val off = HillClimb(Config(fastStart = false, fastStartMinBytes = 8L shl 20))
        assertEquals(1, off.update(0, 0.0, 3, 50L shl 20))
        // and it carries on climbing from two when the link has room
        var now = 0L; var n = big.update(0, 0.0, 3, 50L shl 20)
        repeat(30) { now += 1000; n = big.update(now, n * 1_000_000.0, 3, 50L shl 20) }
        assertEquals(3, n)
    }

    @Test fun aRefusedConnectionHoldsTheCountDownForAWhileAndThenGrowthResumes() {
        val cfg = Config(rampMs = 1000, evalMs = 1000, noGrowthMs = 30_000, fastStart = false)
        val c = HillClimb(cfg)
        var now = 0L; var n = 1
        repeat(10) { now += 1000; n = c.update(now, n * 1_000_000.0, 4) }
        assertEquals(4, n)
        c.refused(now, 2, 600_000)                                                   // the server refused the third and fourth
        assertEquals(2, c.target)
        repeat(300) { now += 1000; n = c.update(now, 2_000_000.0, 4); assertTrue("held at 2 for 10 minutes, was $n at $now", n <= 2) }
        repeat(400) { now += 1000; n = c.update(now, n * 1_000_000.0, 4) }         // 700 s in all: past the 10-minute hold
        assertTrue("growth resumed after the hold: $n", n >= 3)
    }

    @Test fun meterAndText() {
        val m = Meter()
        for (s in 0 until 5) { m.add(1_000_000, s * 1000L + 10); m.add(0, s * 1000L + 990) }
        val v = m.bytesPerSec(5000L + 10)
        assertTrue(v in 900_000.0..1_100_000.0)
        assertEquals(0.0, Meter().bytesPerSec(10), 0.0)
        assertEquals("1.5 MB", Format.bytes(1_572_864)); assertEquals("512 KB", Format.bytes(524_288)); assertEquals("999 B", Format.bytes(999))
        assertEquals("unknown size", Format.bytes(-1)); assertEquals("2.00 GB", Format.bytes(2L shl 30))
        assertEquals("45 s left", Format.eta(45)); assertEquals("3 min left", Format.eta(200)); assertEquals("1 h 5 min left", Format.eta(3900)); assertEquals("", Format.eta(-1))
        val v1 = DownloadView(1, DlState.RUNNING, Reason.NONE, 0, "a", null, "h", 100, 37, 2_100_000, 180, 3, false, null, 0, 0, listOf(Action.PAUSE))
        assertEquals("37 B of 100 B  ·  37 %  ·  2.0 MB/s  ·  3 min left  ·  3 connections", Format.status(v1))
    }

    private fun view(state: DlState, reason: Reason = Reason.NONE, total: Long, done: Long, bps: Long = 0, eta: Long = -1, conns: Int = 1) =
        DownloadView(1, state, reason, 0, "a", null, "h", total, done, bps, eta, conns, false, null, 0, 0, listOf(Action.PAUSE))

    /** Every status line says how much of the file is on the tablet (owner request: MB downloaded next to the total). */
    @Test fun statusShowsMegabytesDownloaded() {
        val mb = 1_048_576L
        assertEquals("7.8 MB of 8.6 MB  ·  91 %  ·  144 KB/s  ·  5 s left", Format.status(view(DlState.RUNNING, total = 9_000_000, done = 8_200_000, bps = 147_456, eta = 5)))
        assertEquals("3.0 MB of 8.0 MB  ·  37 %  ·  1.0 MB/s  ·  5 s left", Format.status(view(DlState.RUNNING, total = 8 * mb, done = 3 * mb, bps = mb, eta = 5)))
        assertEquals("3.0 MB so far  ·  1.0 MB/s", Format.status(view(DlState.RUNNING, total = -1, done = 3 * mb, bps = mb)))
        assertEquals("Paused  ·  3.0 MB of 8.0 MB  ·  37 %", Format.status(view(DlState.PAUSED_USER, total = 8 * mb, done = 3 * mb)))
        assertEquals("Paused  ·  3.0 MB so far", Format.status(view(DlState.PAUSED_SYSTEM, total = -1, done = 3 * mb)))
        assertEquals("Waiting for the connection…  ·  3.0 MB of 8.0 MB", Format.status(view(DlState.WAITING_NETWORK, total = 8 * mb, done = 3 * mb)))
        assertEquals("Waiting for the connection…", Format.status(view(DlState.WAITING_NETWORK, total = 8 * mb, done = 0)))
        assertEquals("Waiting in line", Format.status(view(DlState.QUEUED, total = 8 * mb, done = 0)))
        assertEquals("The connection stalled  ·  3.0 MB of 8.0 MB downloaded", Format.status(view(DlState.FAILED, Reason.STALLED, total = 8 * mb, done = 3 * mb)))
        assertEquals("The connection stalled", Format.status(view(DlState.FAILED, Reason.STALLED, total = 8 * mb, done = 0)))
        assertEquals("Done  ·  8.0 MB", Format.status(view(DlState.COMPLETED, total = 8 * mb, done = 8 * mb)))
    }
}
