package app.kestrel.core.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rules of the small download window (owner message 26, AL2): which downloads it lists and when "Download started" is due. */
class MiniTrackerTest {
    private fun v(id: Long, state: DlState, name: String = "file$id.bin") =
        DownloadView(id, state, Reason.NONE, 0, name, null, "h", 100, 10, 0, -1, 1, false, null, id, id, emptyList())

    private fun ids(u: MiniTracker.Update) = u.rows.map { it.id }

    @Test fun aNewDownloadIsAnnouncedOnceAndListed() {
        val t = MiniTracker(); t.begin(emptyList())
        val u1 = t.update(listOf(v(1, DlState.QUEUED)), 1000)
        assertEquals(listOf(1L), u1.announce.map { it.id }); assertEquals(listOf(1L), ids(u1))
        val u2 = t.update(listOf(v(1, DlState.RUNNING)), 1250)
        assertTrue("the same download is not announced again", u2.announce.isEmpty()); assertEquals(listOf(1L), ids(u2))
    }

    @Test fun downloadsRestoredFromTheLastRunAreNotAnnouncedOrListedUntilTheyMove() {
        val t = MiniTracker()
        t.begin(listOf(v(1, DlState.PAUSED_SYSTEM), v(2, DlState.COMPLETED), v(3, DlState.RUNNING)))
        val u = t.update(listOf(v(1, DlState.PAUSED_SYSTEM), v(2, DlState.COMPLETED), v(3, DlState.RUNNING)), 1000)
        assertTrue(u.announce.isEmpty())
        assertEquals("only the one that is moving (a rotation recreated the window)", listOf(3L), ids(u))
        val resumed = t.update(listOf(v(1, DlState.RUNNING), v(2, DlState.COMPLETED), v(3, DlState.RUNNING)), 1250)
        assertTrue("resuming is not a new download", resumed.announce.isEmpty())
        assertEquals(listOf(3L, 1L), ids(resumed))
    }

    @Test fun aPausedRowStaysSoItCanBeResumed() {
        val t = MiniTracker(); t.begin(emptyList())
        t.update(listOf(v(1, DlState.RUNNING)), 0)
        assertEquals(listOf(1L), ids(t.update(listOf(v(1, DlState.PAUSED_USER)), 100_000)))
    }

    @Test fun aFinishedRowStaysAWhileThenGoes() {
        val t = MiniTracker(lingerMs = 6_000); t.begin(emptyList())
        t.update(listOf(v(1, DlState.RUNNING)), 0)
        assertEquals(listOf(1L), ids(t.update(listOf(v(1, DlState.COMPLETED)), 1_000)))
        assertTrue(t.hasLingering())
        assertEquals(listOf(1L), ids(t.update(listOf(v(1, DlState.COMPLETED)), 6_900)))
        assertTrue("seven seconds after it finished it is gone", ids(t.update(listOf(v(1, DlState.COMPLETED)), 7_100)).isEmpty())
        assertFalse(t.hasLingering())
        assertTrue("and does not come back", ids(t.update(listOf(v(1, DlState.COMPLETED)), 9_000)).isEmpty())
    }

    @Test fun aFailedRowThatIsRetriedIsLiveAgain() {
        val t = MiniTracker(lingerMs = 6_000); t.begin(emptyList())
        t.update(listOf(v(1, DlState.RUNNING)), 0)
        assertEquals(listOf(1L), ids(t.update(listOf(v(1, DlState.FAILED)), 1_000)))
        assertEquals(listOf(1L), ids(t.update(listOf(v(1, DlState.RUNNING)), 5_000)))
        assertFalse(t.hasLingering())
        assertEquals("a retried row does not run out its old time", listOf(1L), ids(t.update(listOf(v(1, DlState.RUNNING)), 60_000)))
    }

    @Test fun aCanceledOrRemovedRowGoesAtOnce() {
        val t = MiniTracker(); t.begin(emptyList())
        t.update(listOf(v(1, DlState.RUNNING), v(2, DlState.RUNNING)), 0)
        assertEquals(listOf(2L), ids(t.update(listOf(v(2, DlState.RUNNING)), 100)))
        assertTrue(ids(t.update(listOf(v(2, DlState.CANCELED)), 200)).isEmpty())
    }

    @Test fun dismissingAFinishedRowHidesItUntilItMovesAgain() {
        val t = MiniTracker(); t.begin(emptyList())
        t.update(listOf(v(1, DlState.RUNNING)), 0)
        t.update(listOf(v(1, DlState.COMPLETED)), 1_000)
        t.dismiss(1)
        assertTrue(ids(t.update(listOf(v(1, DlState.COMPLETED)), 1_100)).isEmpty())
        assertEquals(listOf(1L), ids(t.update(listOf(v(1, DlState.RUNNING)), 2_000)))
    }

    @Test fun rowsKeepTheirOrderSoNothingJumps() {
        val t = MiniTracker(); t.begin(emptyList())
        t.update(listOf(v(1, DlState.RUNNING)), 0)
        t.update(listOf(v(1, DlState.RUNNING), v(2, DlState.QUEUED)), 250)
        val u = t.update(listOf(v(3, DlState.QUEUED), v(2, DlState.RUNNING), v(1, DlState.RUNNING)), 500) // the engine may list them in any order
        assertEquals(listOf(1L, 2L, 3L), ids(u))
        assertEquals(listOf(3L), u.announce.map { it.id })
    }

    @Test fun aDownloadThatFailedAtOnceIsListedButNotAnnounced() {
        val t = MiniTracker(lingerMs = 6_000); t.begin(emptyList())
        val u = t.update(listOf(v(1, DlState.FAILED, "bad.bin")), 0)
        assertTrue(u.announce.isEmpty()); assertEquals(listOf(1L), ids(u))
        assertTrue(ids(t.update(listOf(v(1, DlState.FAILED, "bad.bin")), 7_000)).isEmpty())
    }

    @Test fun twoDownloadsAtOnceAreBothAnnouncedAndTheLastNamesTheBanner() {
        val t = MiniTracker(); t.begin(emptyList())
        val u = t.update(listOf(v(1, DlState.QUEUED, "a.zip"), v(2, DlState.QUEUED, "b.zip")), 0)
        assertEquals(listOf("a.zip", "b.zip"), u.announce.map { it.name })
    }
}
