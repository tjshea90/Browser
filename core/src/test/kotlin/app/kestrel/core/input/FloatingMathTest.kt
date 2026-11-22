package app.kestrel.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingMathTest {
    @Test fun widthIsKeptBetweenTheLimitsAndWithinTheScreen() {
        assertEquals(232f, FloatingMath.clampWidthDp(100f, 412f), 0f); assertEquals(412f, FloatingMath.clampWidthDp(900f, 412f), 0f); assertEquals(620f, FloatingMath.clampWidthDp(900f, 1000f), 0f)
        assertEquals(200f, FloatingMath.clampWidthDp(500f, 200f), 0f)                    // a tiny screen: never wider than the screen
        assertEquals(360f, FloatingMath.defaultWidthDp(412f), 0f); assertEquals(460f, FloatingMath.defaultWidthDp(800f), 0f); assertEquals(300f, FloatingMath.defaultWidthDp(300f), 0f)
        assertEquals(0.45f, FloatingMath.clampHeightMul(0.1f), 0f); assertEquals(2.2f, FloatingMath.clampHeightMul(9f), 0f)
    }

    @Test fun positionRoundTripsAndStaysOnScreen() {
        assertEquals(0, FloatingMath.left(0, 1000, 400)); assertEquals(600, FloatingMath.left(1000, 1000, 400)); assertEquals(300, FloatingMath.left(500, 1000, 400)); assertEquals(0, FloatingMath.left(500, 300, 400))
        for (pos in listOf(0, 137, 300, 600)) assertEquals(pos.toFloat(), FloatingMath.left(FloatingMath.frac(pos, 600), 1000, 400).toFloat(), 1f)
        assertEquals(500, FloatingMath.frac(5, 0)); assertEquals(600, FloatingMath.clampPos(900, 400, 1000)); assertEquals(0, FloatingMath.clampPos(-5, 400, 1000)); assertEquals(0, FloatingMath.clampPos(10, 1200, 1000))
    }

    @Test fun aPositionGoesThereAndBackToTheSamePixelWhateverTheRoom() {
        for (room in listOf(1, 7, 156, 700, 1999, 2745)) for (pos in 0..room step maxOf(1, room / 37)) { var p = pos; repeat(50) { p = FloatingMath.posF(FloatingMath.fracF(p, room), room) }; assertEquals("room $room pos $pos", pos, p) }
    }

    @Test fun droppingAtTheBottomDocks() {
        assertTrue(FloatingMath.dropsToDock(1990, 2000, 2f)); assertTrue(FloatingMath.dropsToDock(2000, 2000, 2f)); assertFalse(FloatingMath.dropsToDock(1900, 2000, 2f))
    }

    @Test fun aCornerDragKeepsTheOppositeCornerStill() {
        // panel at 100,200 size 400x300 on a 1000x2000 screen
        val br = FloatingMath.resize(3, 100, 200, 400, 300, 50, 40, 232, 620, 150, 800, 1000, 2000); assertEquals(listOf(100, 200, 450, 340), listOf(br.left, br.top, br.width, br.height))
        val tl = FloatingMath.resize(0, 100, 200, 400, 300, -50, -40, 232, 620, 150, 800, 1000, 2000); assertEquals(listOf(50, 160, 450, 340), listOf(tl.left, tl.top, tl.width, tl.height)); assertEquals(500, tl.left + tl.width)
        val tr = FloatingMath.resize(1, 100, 200, 400, 300, 30, -20, 232, 620, 150, 800, 1000, 2000); assertEquals(listOf(100, 180, 430, 320), listOf(tr.left, tr.top, tr.width, tr.height)); assertEquals(500, tr.top + tr.height)
        val bl = FloatingMath.resize(2, 100, 200, 400, 300, -30, 20, 232, 620, 150, 800, 1000, 2000); assertEquals(listOf(70, 200, 430, 320), listOf(bl.left, bl.top, bl.width, bl.height)); assertEquals(500, bl.left + bl.width)
    }

    @Test fun aResizeStopsAtTheLimitsAndTheScreenEdge() {
        val small = FloatingMath.resize(3, 100, 200, 400, 300, -900, -900, 232, 620, 150, 800, 1000, 2000); assertEquals(232, small.width); assertEquals(150, small.height)
        val big = FloatingMath.resize(3, 100, 200, 400, 300, 900, 900, 232, 620, 150, 800, 1000, 2000); assertEquals(620, big.width); assertEquals(800, big.height); assertTrue(big.left + big.width <= 1000)
        val edge = FloatingMath.resize(3, 600, 200, 400, 300, 200, 0, 232, 620, 150, 800, 1000, 2000); assertTrue("never off the right edge", edge.left + edge.width <= 1000)
    }

    // ---- owner R19c: the docked keyboard is resized both ways from the arrows of its bar

    @Test fun anArrowDragSidewaysAndUpResizesBothWaysAndTheOtherEdgeStays() {
        // a 1000 px wide screen, the keyboard full width and 400 tall
        val right = FloatingMath.dockedResize(1, 0, 1000, 400, -300, -120, 1000, 500, 280, 600)
        assertEquals(0, right.left); assertEquals(700, right.width); assertEquals("up = taller", 520, right.height)
        val left = FloatingMath.dockedResize(0, 0, 1000, 400, 300, 60, 1000, 500, 280, 600)
        assertEquals("the left arrow moves the left edge, the right edge stays at 1000", 300, left.left); assertEquals(700, left.width); assertEquals("down = shorter", 340, left.height)
    }

    @Test fun theDockedSizeStopsAtItsLimitsAndStaysOnTheScreen() {
        val a = FloatingMath.dockedResize(1, 0, 1000, 400, -900, -900, 1000, 500, 280, 600)
        assertEquals(500, a.width); assertEquals(600, a.height)
        val b = FloatingMath.dockedResize(0, 200, 800, 400, 5000, 5000, 1000, 500, 280, 600)
        assertEquals(500, b.width); assertEquals("the right edge (1000) stays", 500, b.left); assertEquals(280, b.height)
        val c = FloatingMath.dockedResize(1, 300, 600, 400, 300, 0, 1000, 500, 280, 600)
        assertEquals(900, c.width); assertEquals("a keyboard that would run off the right edge is moved left, never cut", 100, c.left)
        // (a keyboard that already is taller than the cap, a phone held sideways, is not cut by a mere touch: KeyboardRoot passes maxOf(its height when the drag began, the cap) as maxH)
        val d = FloatingMath.dockedResize(1, 0, 1000, 700, 0, 0, 1000, 500, 280, maxOf(700, 600))
        assertEquals(700, d.height)
        assertEquals("but it cannot grow any more", 700, FloatingMath.dockedResize(1, 0, 1000, 700, 0, -50, 1000, 500, 280, maxOf(700, 600)).height)
    }

    @Test fun aFingerHeldStillResizesNothing() {
        val r = FloatingMath.dockedResize(0, 100, 800, 400, 0, 0, 1000, 500, 280, 600)
        assertEquals(100, r.left); assertEquals(800, r.width); assertEquals(400, r.height)
    }

    @Test fun theVerticalDeadZoneIsContinuous() {
        assertEquals(0f, FloatingMath.softDead(8f, 10f), 0f); assertEquals(0f, FloatingMath.softDead(-10f, 10f), 0f)
        assertEquals(5f, FloatingMath.softDead(15f, 10f), 0f); assertEquals(-5f, FloatingMath.softDead(-15f, 10f), 0f)
    }

    @Test fun dockedRowMultipliersAreKeptInTheirRange() {
        assertEquals(FloatingMath.DOCK_MIN_H_MUL, FloatingMath.clampDockMul(0.1f), 0f); assertEquals(FloatingMath.DOCK_MAX_H_MUL, FloatingMath.clampDockMul(9f), 0f); assertEquals(1f, FloatingMath.clampDockMul(1f), 0f)
    }
}
