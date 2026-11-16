package app.kestrel.core.player

import app.kestrel.core.player.GestureMath.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ANS57 (owner message 57): VLC's numbers and the touch rules of the film (long press = precise seek, vertical drags = brightness / volume). */
class PlayerGesturesTest {
    // ---- the numbers (VLC for Android's VideoTouchDelegate)
    @Test fun theJumpGrowsWithTheFourthPowerOfTheDistance() {
        assertEquals("under 1 cm nothing", 0L, GestureMath.jumpMs(0.99, 1.0))
        assertEquals("1 cm: the 3 s base and a hair", 3146L, GestureMath.jumpMs(1.0, 1.0))
        assertEquals("2 cm", 5343L, GestureMath.jumpMs(2.0, 1.0))
        assertEquals("4 cm: about 40 s", 40500L, GestureMath.jumpMs(4.0, 1.0))
        assertEquals("8 cm: ten minutes", 603000L, GestureMath.jumpMs(8.0, 1.0))
        assertEquals("left is the same size backwards", -40500L, GestureMath.jumpMs(-4.0, 1.0))
        assertEquals("the fine factor divides it", 13500L, GestureMath.jumpMs(4.0, 3.0))
        assertTrue("always growing", (1..16).map { GestureMath.jumpMs(it / 2.0, 1.0) }.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun movingAlsoUpOrDownMakesTheSeekFiner() {
        assertEquals("on the line: not finer", 1.0, GestureMath.fineCoef(0.0, 160.0), 1e-9)
        assertEquals("half an inch: still 1.5 x 2 = 2", 2.0, GestureMath.fineCoef(80.0, 160.0), 1e-9)
        assertEquals("one inch up or down divides by 3", 3.0, GestureMath.fineCoef(160.0, 160.0), 1e-9)
        assertEquals("either way", 3.0, GestureMath.fineCoef(-160.0, 160.0), 1e-9)
    }

    @Test fun aJumpNeverLeavesTheFilm() {
        assertEquals(40500L, GestureMath.clampJump(40500L, 100_000L, 600_000L))
        assertEquals("at the end", 5_000L, GestureMath.clampJump(40500L, 595_000L, 600_000L))
        assertEquals("at the start", -7_000L, GestureMath.clampJump(-40500L, 7_000L, 600_000L))
        assertEquals("no length: only the start limits", 40500L, GestureMath.clampJump(40500L, 595_000L, 0L))
    }

    @Test fun theInfoTextIsVlcs() {
        assertEquals("+0:25 (1:23:45)", GestureMath.infoText(25_000L, 5_025_000L, 1.0))
        assertEquals("-1:10 (0:55) x0.3", GestureMath.infoText(-70_000L, 55_000L, 3.0))
        assertEquals("+0:03 (0:13)", GestureMath.infoText(3_100L, 13_100L, 1.0))
    }

    @Test fun leftIsBrightnessRightIsVolumeAndTheMiddleSeventhIsDead() {
        assertEquals(Level.BRIGHTNESS, GestureMath.levelAt(0.05)); assertEquals(Level.BRIGHTNESS, GestureMath.levelAt(0.42))
        assertNull(GestureMath.levelAt(0.5)); assertNull(GestureMath.levelAt(0.43)); assertNull(GestureMath.levelAt(0.57))
        assertEquals(Level.VOLUME, GestureMath.levelAt(0.58)); assertEquals(Level.VOLUME, GestureMath.levelAt(0.95))
        assertEquals("dragging up (negative dy) raises it, by 1.25 of the range over the whole range", 1.25, GestureMath.levelDelta(-600.0, 600.0), 1e-9)
        assertEquals(-0.625, GestureMath.levelDelta(300.0, 600.0), 1e-9)
    }

    // ---- the touch rules
    private class Host : PlayerGestures.Host {
        var clock = 0L; var holdAt = -1L
        var taps = ArrayList<Double>(); var armed = 0; var longHolds = 0; var passed = 0; var finished = 0
        var previews = ArrayList<Triple<Long, Long, Double>>(); var commits = ArrayList<Long>(); var cancels = 0; var unavailable = 0
        var levels = ArrayList<Pair<Level, Double>>()
        var pos = 100_000L; var len = 3_600_000L
        override fun now() = clock
        override fun scheduleHold(ms: Long) { holdAt = clock + ms }
        override fun cancelHold() { holdAt = -1L }
        override fun tap(xFraction: Double) { taps.add(xFraction) }
        override fun armed() { armed++ }
        override fun longHold() { longHolds++ }
        override fun positionMs() = pos
        override fun lengthMs() = len
        override fun seekPreview(jumpMs: Long, targetMs: Long, coef: Double) { previews.add(Triple(jumpMs, targetMs, coef)) }
        override fun seekCommit(jumpMs: Long) { commits.add(jumpMs) }
        override fun seekCancel() { cancels++ }
        override fun seekUnavailable() { unavailable++ }
        override fun level(level: Level, delta: Double) { levels.add(Pair(level, delta)) }
        override fun passThrough() { passed++ }
        override fun finished() { finished++ }
    }

    // a landscape full-screen view 1000 x 600 px at 160 dpi (1 cm = 63 px), slop 8, edge margin 24
    private fun full(hold: Boolean = false) = PlayerGestures.Config(1000f, 600f, 160f, 8f, 24f, 600f, hold)
    private fun PlayerGestures.wait(h: Host, ms: Long) { h.clock += ms; if (h.holdAt in 0..h.clock) { h.holdAt = -1L; hold() } }

    @Test fun aQuickTouchIsATapAndNothingElse() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(900f, 300f); h.clock += 80; g.up(902f, 301f)
        assertEquals(listOf(0.9), h.taps.map { Math.round(it * 10) / 10.0 }); assertEquals(0, h.armed)
        assertTrue(h.previews.isEmpty() && h.levels.isEmpty() && h.commits.isEmpty())
    }

    @Test fun aLongPressArmsAndLettingGoWithoutMovingIsNotATap() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360)
        assertEquals(1, h.armed); assertTrue(g.active)
        g.wait(h, 100); g.up(500f, 300f)
        assertTrue("a press that was held is no tap", h.taps.isEmpty()); assertEquals("the hint is cleaned up", 1, h.finished)
        assertTrue(h.commits.isEmpty())
    }

    @Test fun holdThenDragRightPreviewsAndTheFilmMovesOnlyWhenTheFingerLetsGo() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360)
        g.move(520f, 300f)                                             // 0.3 cm: under the 1 cm of VLC: nothing to show
        assertTrue(h.previews.isEmpty())
        g.move(500f + 252f, 300f)                                      // 4 cm right
        assertEquals(1, h.previews.size)
        val (jump, target, coef) = h.previews.last()
        assertEquals(40500L, jump); assertEquals(100_000L + 40500L, target); assertEquals(1.0, coef, 1e-9)
        assertTrue("nothing has moved yet", h.commits.isEmpty())
        g.move(500f + 126f, 300f)                                      // back to 2 cm: smaller
        assertEquals(5343L, h.previews.last().first)
        g.move(500f + 252f, 300f); g.up(500f + 252f, 300f)
        assertEquals(listOf(40500L), h.commits); assertEquals(1, h.finished)
    }

    @Test fun holdThenDragLeftRewindsAndTheFartherTheMore() {
        val h = Host(); val g = PlayerGestures(h, full()); h.pos = 2_000_000L
        g.down(500f, 300f); g.wait(h, 360)
        g.move(500f - 126f, 300f); val a = h.previews.last().first
        g.move(500f - 378f, 300f); val b = h.previews.last().first
        g.move(500f - 504f, 300f); val c = h.previews.last().first
        assertTrue("backwards: negative", a < 0 && b < 0 && c < 0); assertTrue("and the farther the more", a > b && b > c)
        g.up(500f - 504f, 300f); assertEquals(listOf(c), h.commits)
    }

    @Test fun movingUpOrDownWhileSeekingMakesItFiner() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360)
        g.move(500f + 252f, 300f); val plain = h.previews.last().first
        g.move(500f + 252f, 300f - 160f)                              // one inch up as well
        val fine = h.previews.last()
        assertEquals(plain / 3, fine.first, 2L); assertEquals(3.0, fine.third, 1e-9)
    }

    @Test fun theJumpIsClampedToTheFilmAndALiveStreamHasNoPreciseSeek() {
        val h = Host(); val g = PlayerGestures(h, full()); h.pos = 3_590_000L
        g.down(500f, 300f); g.wait(h, 360); g.move(500f + 504f, 300f)
        assertEquals("only ten seconds are left", 10_000L, h.previews.last().first)
        val h2 = Host(); h2.len = 0L; val g2 = PlayerGestures(h2, full())
        g2.down(500f, 300f); g2.wait(h2, 360); g2.move(700f, 300f); g2.move(800f, 300f); g2.up(800f, 300f)
        assertEquals("said once", 1, h2.unavailable); assertTrue(h2.previews.isEmpty() && h2.commits.isEmpty())
    }

    @Test fun aDragBackToNothingCancelsAndLettingGoThereMovesNothing() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360)
        g.move(500f + 252f, 300f); g.move(500f + 20f, 300f)
        assertEquals("the preview was withdrawn", 1, h.cancels)
        g.up(500f + 20f, 300f); assertTrue(h.commits.isEmpty())
    }

    @Test fun aSidewaysDragWithoutTheLongPressDoesNothingAndIsNoTap() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); h.clock += 50; g.move(560f, 302f); g.move(700f, 305f); h.clock += 100; g.up(700f, 305f)
        assertTrue(h.previews.isEmpty() && h.commits.isEmpty() && h.levels.isEmpty() && h.taps.isEmpty() && h.passed == 0)
    }

    @Test fun aSteepDragOnTheLeftIsBrightnessAndOnTheRightVolumeWithoutAnyHold() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(150f, 400f); h.clock += 30; g.move(152f, 380f)          // 20 px: under the 5 % (30 px of the 600 range): still waiting
        assertTrue(h.levels.isEmpty())
        g.move(153f, 350f)                                              // 50 px up: it began (from here on: no jump for the dead start)
        assertTrue(h.levels.isEmpty())
        g.move(153f, 290f)                                              // 60 px more up
        assertEquals(1, h.levels.size); assertEquals(Level.BRIGHTNESS, h.levels[0].first); assertEquals(0.125, h.levels[0].second, 1e-9)
        g.move(153f, 320f); assertTrue("down lowers it", h.levels.last().second < 0)
        g.up(153f, 320f); assertEquals(1, h.finished); assertTrue(h.taps.isEmpty())
        val h2 = Host(); val g2 = PlayerGestures(h2, full())
        g2.down(850f, 400f); g2.move(852f, 300f); g2.move(852f, 250f)
        assertEquals(Level.VOLUME, h2.levels.last().first); assertTrue(h2.levels.last().second > 0)
    }

    @Test fun theMiddleSeventhAndTheEdgesMakeNoLevelGesture() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 400f); g.move(500f, 300f); g.move(500f, 200f); g.up(500f, 200f)
        assertTrue("the dead zone", h.levels.isEmpty())
        val h2 = Host(); val g2 = PlayerGestures(h2, full())
        g2.down(10f, 400f); g2.move(10f, 300f); g2.move(10f, 200f); g2.up(10f, 200f)
        assertTrue("an edge start: the system's own gestures live there", h2.levels.isEmpty() && h2.taps.isEmpty())
        val h3 = Host(); val g3 = PlayerGestures(h3, full())
        g3.down(10f, 400f); h3.clock += 60; g3.up(10f, 400f); assertEquals("but a tap at the edge is a tap", 1, h3.taps.size)
        g3.down(10f, 400f); g3.wait(h3, 400); assertEquals("and no long press arms there", 0, h3.armed)
    }

    @Test fun holdThenSteepDragIsAlsoALevelAndStartsAtOnce() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(850f, 400f); g.wait(h, 360)
        g.move(851f, 380f)                                              // 20 px up, the finger had armed it: no 5 % wait
        assertEquals(Level.VOLUME, h.levels.last().first); assertEquals(1.25 * 20 / 600, h.levels.last().second, 1e-9)
    }

    @Test fun inThePlayerInsideThePageAPlainDragIsThePagesAndEverythingNeedsTheHold() {
        val h = Host(); val g = PlayerGestures(h, full(hold = true))
        g.down(150f, 400f); h.clock += 40; g.move(152f, 380f)
        assertEquals("handed over to the page at the first move", 1, h.passed)
        g.move(153f, 300f); g.up(153f, 300f)
        assertTrue("no level, no seek, no tap", h.levels.isEmpty() && h.previews.isEmpty() && h.taps.isEmpty())
        val h2 = Host(); val g2 = PlayerGestures(h2, full(hold = true))
        g2.down(150f, 400f); g2.wait(h2, 360); g2.move(151f, 360f)
        assertEquals("after the hold the same steep drag is the player's", Level.BRIGHTNESS, h2.levels.last().first); assertEquals(0, h2.passed)
        val h3 = Host(); val g3 = PlayerGestures(h3, full(hold = true))
        g3.down(500f, 300f); g3.wait(h3, 360); g3.move(700f, 300f); g3.up(700f, 300f)
        assertEquals("and a sideways one is the precise seek", 1, h3.commits.size); assertEquals(0, h3.passed)
    }

    @Test fun aFingerThatRestsOnGivesTheSecondStageOnceAndNoSeek() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360); assertEquals(0, h.longHolds)
        g.wait(h, 460); assertEquals(1, h.longHolds)
        g.wait(h, 2000); assertEquals("once", 1, h.longHolds)
        g.up(500f, 300f); assertTrue(h.commits.isEmpty() && h.taps.isEmpty())
    }

    @Test fun movingBeforeTheSecondStageWithdrawsIt() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360); g.move(600f, 300f)
        g.wait(h, 2000); assertEquals(0, h.longHolds)
    }

    @Test fun aCancelEndsEverythingCleanly() {
        val h = Host(); val g = PlayerGestures(h, full())
        g.down(500f, 300f); g.wait(h, 360); g.move(700f, 300f)
        g.cancel()
        assertEquals(1, h.cancels); assertEquals(1, h.finished); assertFalse(g.active); assertTrue(h.commits.isEmpty())
    }
}
