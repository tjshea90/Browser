package app.kestrel.core.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PullTrackerTest {
    private val slop = 10f
    private val log = ArrayList<String>()
    private val tracker = PullTracker(slop, object : PullTracker.Listener {
        override fun start() { log.add("start") }
        override fun move(dy: Float) { log.add("move ${dy.toInt()}") }
        override fun end(released: Boolean) { log.add("end $released") }
    })

    private fun atTop() = tracker.answer(canScrollUp = false, pageHandlesTouch = false, overscrollAllowed = true)

    @Test fun pullsWhenAtTopAndDraggedDown() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 105f)                  // inside the slop: nothing yet
        assertEquals(emptyList<String>(), log)
        tracker.move(100f, 125f)                  // 25 below the start = 15 past the slop
        tracker.move(100f, 160f)
        tracker.up(100f, 160f)
        assertEquals(listOf("start", "move 15", "move 50", "end true"), log)
    }

    @Test fun answerThatArrivesLateStartsThePullWithCatchUp() {
        tracker.down(100f, 100f, true)
        tracker.move(100f, 140f)                  // the engine has not answered yet: the finger is already 40 down
        assertEquals(emptyList<String>(), log)
        atTop()
        assertEquals(listOf("start", "move 30"), log)
        tracker.up(100f, 140f)
        assertEquals("end true", log.last())
    }

    @Test fun noAnswerMeansNoPull() {
        tracker.down(100f, 100f, true)
        tracker.move(100f, 200f); tracker.up(100f, 200f)
        assertEquals(emptyList<String>(), log)
    }

    @Test fun pageThatCanStillScrollUpNeverPulls() {
        tracker.down(100f, 100f, true)
        tracker.answer(canScrollUp = true, pageHandlesTouch = false, overscrollAllowed = true)
        assertFalse(tracker.alive)
        tracker.move(100f, 300f); tracker.up(100f, 300f)
        assertEquals(emptyList<String>(), log)
    }

    @Test fun pageThatHandlesTheTouchOrForbidsOverscrollNeverPulls() {
        tracker.down(0f, 0f, true); tracker.answer(canScrollUp = false, pageHandlesTouch = true, overscrollAllowed = true)
        tracker.move(0f, 200f); tracker.up(0f, 200f)
        tracker.down(0f, 0f, true); tracker.answer(canScrollUp = false, pageHandlesTouch = false, overscrollAllowed = false)
        tracker.move(0f, 200f); tracker.up(0f, 200f)
        assertEquals(emptyList<String>(), log)
    }

    @Test fun lateNegativeAnswerEndsAPullThatAlreadyStarted() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 150f)
        assertTrue(tracker.isPulling)
        // (a second answer for the same touch-down cannot happen in the app, but a kill by a second finger can)
        tracker.pointerDown()
        assertEquals(listOf("start", "move 40", "end false"), log)
        tracker.move(100f, 300f); tracker.up(100f, 300f)
        assertEquals(3, log.size)
    }

    @Test fun draggingUpIsAScrollNotAPull() {
        tracker.down(100f, 300f, true); atTop()
        tracker.move(100f, 280f)                  // 20 up, past the slop
        tracker.move(100f, 400f)                  // coming back down does not revive it
        tracker.up(100f, 400f)
        assertEquals(emptyList<String>(), log)
    }

    @Test fun mostlySidewaysIsASwipeNotAPull() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(160f, 112f)                  // 60 right, 12 down
        tracker.move(160f, 250f)
        assertEquals(emptyList<String>(), log)
    }

    @Test fun aSecondFingerEndsThePullAndTheGestureIsOver() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 160f)
        tracker.pointerDown()
        tracker.move(100f, 260f); tracker.up(100f, 260f)
        assertEquals(listOf("start", "move 50", "end false"), log)
    }

    @Test fun movingBackAboveTheStartEndsThePullWithoutRelease() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 180f)
        tracker.move(100f, 85f)                   // 15 above the start: more than the slop
        tracker.up(100f, 85f)
        assertEquals(listOf("start", "move 70", "end false"), log)
    }

    @Test fun backNearTheStartKeepsThePullAtZero() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 180f)
        tracker.move(100f, 105f)                  // inside the slop band: distance 0, still the same pull
        tracker.up(100f, 105f)
        assertEquals(listOf("start", "move 70", "move 0", "end true"), log)
    }

    @Test fun cancelIsNotARelease() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 150f)
        tracker.cancel()
        assertEquals(listOf("start", "move 40", "end false"), log)
    }

    @Test fun aDisabledGestureNeverPullsAndNeedsNoAnswer() {
        tracker.down(100f, 100f, false)
        assertFalse(tracker.alive)
        atTop(); tracker.move(100f, 300f); tracker.up(100f, 300f)
        assertEquals(emptyList<String>(), log)
    }

    @Test fun everyGestureStartsFresh() {
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 50f)                   // dies
        tracker.up(100f, 50f)
        tracker.down(100f, 100f, true); atTop()
        tracker.move(100f, 130f); tracker.up(100f, 130f)
        assertEquals(listOf("start", "move 20", "end true"), log)
    }
}
