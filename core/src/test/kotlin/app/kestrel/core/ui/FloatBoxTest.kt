package app.kestrel.core.ui

import app.kestrel.core.ui.FloatBox.Default
import app.kestrel.core.ui.FloatBox.Geo
import app.kestrel.core.ui.FloatBox.Limits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FD1: the floating download window's place and size, the way the keyboard panel's are. */
class FloatBoxTest {
    private val lim = Limits(minW = 200f, maxW = 420f, minH = 90f, maxH = 600f)
    private val def = Default(right = 8f, top = 8f, w = 300f, h = 260f)
    private val areaW = 564f; private val areaH = 780f
    private val boxH = 200f

    @Test fun theDefaultStaysTheDefault() {
        val g = FloatBox.clamp(def.geo(), areaW, areaH, boxH, lim)
        assertEquals(def.geo(), g); assertNull(FloatBox.store(g, def))
    }

    @Test fun draggingRightShrinksTheGapToTheRightEdgeAndDraggingDownGrowsTheTopGap() {
        val g = FloatBox.moved(def.geo(), dx = 5f, dy = 40f, areaW, areaH, boxH, lim)
        assertEquals(4f, g.right, 0.01f)             // 8 - 5 = 3, kept 4 dp inside the edge
        assertEquals(48f, g.top, 0.01f)
        assertEquals(300f, g.w, 0.01f)
        val l = FloatBox.moved(def.geo(), dx = -200f, dy = 0f, areaW, areaH, boxH, lim)
        assertEquals(208f, l.right, 0.01f)
    }

    @Test fun theWindowNeverLeavesTheScreen() {
        val g = FloatBox.moved(def.geo(), dx = -9999f, dy = 9999f, areaW, areaH, boxH, lim)
        assertEquals(areaW - 300f - 4f, g.right, 0.01f)
        assertEquals(areaH - boxH - 4f, g.top, 0.01f)
        val h = FloatBox.moved(def.geo(), dx = 9999f, dy = -9999f, areaW, areaH, boxH, lim)
        assertEquals(4f, h.right, 0.01f); assertEquals(4f, h.top, 0.01f)
    }

    @Test fun theBottomLeftCornerMakesItWiderGoingLeftAndTallerGoingDown() {
        val g = FloatBox.resizedFromBottomLeft(def.geo(), dx = -60f, dy = 50f, areaW, areaH, boxH, lim)
        assertEquals(360f, g.w, 0.01f); assertEquals(310f, g.h, 0.01f); assertEquals(8f, g.right, 0.01f); assertEquals(8f, g.top, 0.01f)
    }

    @Test fun theBottomRightCornerMovesTheRightEdgeWithTheFinger() {
        val g = FloatBox.resizedFromBottomRight(Geo(60f, 8f, 300f, 260f), dx = 30f, dy = 0f, areaW, areaH, boxH, lim)
        assertEquals(30f, g.right, 0.01f); assertEquals(330f, g.w, 0.01f)
    }

    @Test fun sizeStaysBetweenTheLimitsAndInsideTheArea() {
        val tiny = FloatBox.resizedFromBottomLeft(def.geo(), dx = 500f, dy = -500f, areaW, areaH, boxH, lim)
        assertEquals(200f, tiny.w, 0.01f); assertEquals(90f, tiny.h, 0.01f)
        val huge = FloatBox.resizedFromBottomLeft(def.geo(), dx = -9999f, dy = 9999f, areaW, areaH, boxH, lim)
        assertEquals(420f, huge.w, 0.01f)
        assertTrue("never taller than the area: ${huge.h}", huge.h <= areaH)
        // a narrow screen: the window is never wider than the screen
        val narrow = FloatBox.clamp(Geo(8f, 8f, 420f, 200f), 300f, 700f, boxH, lim)
        assertTrue(narrow.w <= 300f - 8f)
    }

    @Test fun aScreenThatIsSmallerForAMomentDoesNotChangeWhatWasStored() {
        val stored = Geo(300f, 600f, 300f, 260f)
        val shown = FloatBox.clamp(stored, 400f, 500f, boxH, lim)
        assertTrue(shown.top <= 500f - boxH); assertEquals(stored, Geo(300f, 600f, 300f, 260f))
    }

    @Test fun storeAndLoadRoundTripAndTheDefaultIsNothing() {
        val g = Geo(40f, 120.4f, 260f, 180f)
        val a = FloatBox.store(g, def)!!
        assertEquals(listOf(40, 120, 260, 180), a.toList())
        assertEquals(Geo(40f, 120f, 260f, 180f), FloatBox.load(a, def))
        assertEquals(def.geo(), FloatBox.load(null, def)); assertEquals(def.geo(), FloatBox.load(intArrayOf(1, 2, 3), def))
        assertNull(FloatBox.store(def.geo(), def))
    }
}
