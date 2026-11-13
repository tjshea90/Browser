package app.kestrel.browser.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What the page area shows while the live page loads (EY-1): the saved picture 1:1 and undimmed, the thin line at the top, the slim note at the bottom - or today's plain cover for a picture of another shape. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29])
class RestoreOverlayLookTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val red = 0xFFFF0000.toInt()

    private fun overlay(w: Int, h: Int): RestoreOverlay {
        val o = RestoreOverlay(ctx, Pal(ctx))
        o.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        o.layout(0, 0, w, h)
        return o
    }

    private fun render(o: View, w: Int, h: Int): Bitmap { val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); o.draw(Canvas(b)); return b }
    private fun picture(w: Int, h: Int): Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.eraseColor(red) }

    @Test fun aPictureOfTheSameWidthIsDrawnOneToOneUndimmedWithALineOnTop() {
        val o = overlay(600, 900)
        var drawn = 0
        o.showLook(picture(600, 800), "Opening news.test", onDrawn = { drawn++ })
        assertTrue(o.showingLook)
        val out = render(o, 600, 900)
        assertEquals("the picture's own colour, not dimmed", red, out.getPixel(300, 400))
        assertEquals(red, out.getPixel(5, 790))
        assertNotEquals("below the picture (it is shorter than the page area) the window colour shows, not the picture", red, out.getPixel(300, 880))
        assertEquals("the thin line along the top edge is the accent colour", Pal(ctx).accent, out.getPixel(300, 0))
        assertEquals("the mark is taken once, in the draw", 1, drawn)
        render(o, 600, 900)
        assertEquals(1, drawn)
    }

    @Test fun aNoteAppearsAtTheBottomWhenAsked() {
        val o = overlay(600, 900)
        o.showLook(picture(600, 900), "Opening news.test")
        val plain = render(o, 600, 900)
        o.setLookNote("Saved view - still loading")
        val noted = render(o, 600, 900)
        var changed = 0
        for (x in 150..450 step 3) for (y in 840..880 step 3) if (plain.getPixel(x, y) != noted.getPixel(x, y)) changed++
        assertTrue("the slim chip is drawn near the bottom edge ($changed pixels differ)", changed > 50)
        assertEquals("the picture above it is untouched", red, noted.getPixel(300, 400))
    }

    @Test fun aPictureOfAnotherShapeFallsBackToTheCoverWithTheOpeningText() {
        val o = overlay(973, 600)                                   // a landscape page area for a portrait picture
        o.showLook(picture(600, 900), "Opening news.test")
        val out = render(o, 973, 600)
        assertNotEquals("no red: the picture is not stretched onto a screen of another shape", red, out.getPixel(480, 300))
        assertNotEquals(red, out.getPixel(20, 20))
    }

    @Test fun aWiderScreenOfTheSameShapeScalesThePictureToTheWidth() {
        val o = overlay(1200, 1800)
        o.showLook(picture(600, 900), "Opening news.test")
        val out = render(o, 1200, 1800)
        assertEquals(red, out.getPixel(1190, 1790))
    }

    @Test fun hidingTakesTheLookDown() {
        val o = overlay(600, 900)
        o.showLook(picture(600, 900), "Opening news.test")
        assertTrue(o.showingLook)
        o.hide()
        assertFalse(o.showingLook)
        assertEquals(View.GONE, o.visibility)
    }
}
