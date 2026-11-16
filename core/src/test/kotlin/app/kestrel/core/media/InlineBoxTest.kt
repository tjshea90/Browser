package app.kestrel.core.media

import app.kestrel.core.media.InlineBox.Edge
import app.kestrel.core.media.InlineBox.Page
import app.kestrel.core.media.InlineBox.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** ANS55f: where Kestrel's own player sits over the page's video box, and what it does when the box moves. */
class InlineBoxTest {
    // the tablet: 600 px wide view = 564 CSS px at 1.0638 px per CSS px (the owner's page: Yahoo's player box 532 x 299 CSS px at x 16)
    private fun page(x: Double, y: Double, w: Double = 532.0, h: Double = 299.0, scale: Double = 1.0, offX: Double = 0.0, offY: Double = 0.0, vvW: Double = 564.0) = Page(x, y, w, h, scale, offX, offY, vvW)

    @Test fun theBoxIsPlacedInViewPixels() {
        val b = InlineBox.toView(page(16.0, 100.0), 600, 916)!!
        assertEquals(17, b.l); assertEquals(106, b.t); assertEquals(583, b.r); assertEquals(424, b.b)
    }

    @Test fun theVisualViewportOffsetIsTakenOut() {
        val b = InlineBox.toView(page(16.0, 300.0, offY = 200.0), 600, 916)!!
        assertEquals(106, b.t)
    }

    @Test fun aZoomedPageOrAnUnusableBoxIsNotPlaced() {
        assertNull("pinch-zoomed", InlineBox.toView(page(16.0, 100.0, scale = 1.6), 600, 916))
        assertNull("a thumbnail", InlineBox.toView(page(16.0, 100.0, w = 120.0, h = 68.0), 600, 916))
        assertNull("an element in a frame, under a fixed header or in a scroller of its own", InlineBox.toView(Page(16.0, 100.0, 532.0, 299.0, 1.0, 0.0, 0.0, 564.0, follows = false), 600, 916))
        assertNull("not on the screen", InlineBox.toView(page(16.0, 4000.0), 600, 916))
        assertNull("no view", InlineBox.toView(page(16.0, 100.0), 0, 916))
        assertNull("no viewport width", InlineBox.toView(page(16.0, 100.0, vvW = 0.0), 600, 916))
    }

    @Test fun aBoxHalfInViewStillCounts() {
        assertNotNull(InlineBox.toView(page(16.0, 700.0), 600, 916))               // 216 of 299 CSS px in view
    }

    @Test fun aPageThatScrollsDownMovesTheBoxUp() {
        val b = Rect(17, 106, 583, 424)
        val m = InlineBox.moved(b, 0, 80)
        assertEquals(26, m.t); assertEquals(344, m.b); assertEquals(17, m.l)
        assertEquals("and back", b, InlineBox.moved(m, 0, -80))
    }

    @Test fun whatIsInViewDecidesPlayPauseOrStop() {
        val inView = Rect(17, 106, 583, 424)
        assertEquals(Edge.PLAY, InlineBox.edgeFor(inView, 600, 916))
        assertEquals("most of it is out of the top", Edge.PAUSE, InlineBox.edgeFor(InlineBox.moved(inView, 0, 330), 600, 916))
        assertEquals("all of it is", Edge.STOP, InlineBox.edgeFor(InlineBox.moved(inView, 0, 500), 600, 916))
        assertEquals("off the bottom", Edge.STOP, InlineBox.edgeFor(Rect(17, 920, 583, 1238), 600, 916))
    }

    @Test fun thePictureIsLetterboxedNeverStretched() {
        val box = Rect(0, 0, 600, 338)                                              // 16:9 box
        val wide = InlineBox.letterbox(box, 640, 360)!!
        assertEquals(600, wide.w); assertEquals(338, wide.h)
        val tall = InlineBox.letterbox(box, 360, 640)!!                              // a portrait film inside a wide box: bars left and right
        assertEquals(338, tall.h); assertEquals(190, tall.w); assertEquals(205, tall.l)
        assertNull("the shape is not known yet", InlineBox.letterbox(box, 0, 0))
        val wider = InlineBox.letterbox(box, 2000, 500)!!                              // 4:1 film: bars top and bottom
        assertEquals(600, wider.w); assertEquals(150, wider.h); assertEquals(94, wider.t)
    }
    // the engine's compositor updates: CSS pixels and zoom = screen pixels per CSS pixel (the documented numbers of GeckoSession.ScrollPositionUpdate)
    @Test fun theCompositorsScrollMovesTheBoxByTheDeltaTimesZoom() {
        val t = InlineBox.Track(Rect(17, 106, 583, 424), 0.0, 100.0)
        val m = t.at(0.0, 180.0, 1.0638)!!                                          // the page scrolled 80 CSS px down = 85 px
        assertEquals(21, m.t); assertEquals(339, m.b); assertEquals(17, m.l)
        assertEquals("and back up past where it was", 126, t.at(0.0, 80.0, 1.0638)!!.t)
        assertEquals("the same place again", Rect(17, 106, 583, 424), t.at(0.0, 100.0, 1.0638))
    }

    @Test fun aSidewaysScrollMovesItSideways() {
        val t = InlineBox.Track(Rect(17, 106, 583, 424), 10.0, 0.0)
        val m = t.at(60.0, 0.0, 1.0)!!
        assertEquals(-33, m.l); assertEquals(106, m.t)
    }

    @Test fun aPinchAfterTheFirstUpdateEndsTheFollowing() {
        val t = InlineBox.Track(Rect(17, 106, 583, 424), 0.0, 0.0)
        assertNotNull(t.at(0.0, 10.0, 1.0638))
        assertNotNull("a little jitter in the zoom is no pinch", t.at(0.0, 20.0, 1.07))
        assertNull(t.at(0.0, 30.0, 1.6))
    }

    @Test fun nonsenseIsNotFollowed() {
        val t = InlineBox.Track(Rect(17, 106, 583, 424), 0.0, 0.0)
        assertNull(t.at(Double.NaN, 0.0, 1.0)); assertNull(t.at(0.0, 0.0, 0.0)); assertNull(t.at(0.0, 9e9, 1.0))
    }
}
