package app.kestrel.browser.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import app.kestrel.core.input.glide.GlideTyping
import app.kestrel.core.input.glide.LexiconFile
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.concurrent.Executor

/** What the small keyboard really PAINTS (native graphics, real pixels): the preview bubble above a pressed key and over the held top-row key, which the state-only tests in MiniNavTest cannot see (AV3). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29])
class MiniNavLookTest {
    private class Rec : MiniNav.Listener {
        override fun navBack() {}
        override fun navHome() {}
        override fun navForward() {}
        override fun navSubmit(text: String) {}
        override fun navOpenChanged(open: Boolean) {}
    }

    private lateinit var activity: Activity
    private lateinit var frame: FrameLayout
    private lateinit var nav: MiniNav
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun relayout() {
        frame.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
        frame.layout(0, 0, 600, 900)
    }

    @Before fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        frame = FrameLayout(activity)
        activity.setContentView(frame)
        nav = MiniNav(activity, Pal(activity), Rec())
        frame.addView(nav, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        relayout()
        val bytes = LexiconFile.build(listOf("hello" to 500L, "world" to 900L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        nav.glideFactory = { GlideTyping({ bytes }, Executor { it.run() }) }
        nav.setGlideEnabled(true)
        nav.setOpen(true, animate = false); relayout()
        nav.pressBarForTest(); relayout(); idle(50)
    }

    private fun render(): Bitmap {
        val v = nav.keysViewForTest()
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
        return b
    }

    /** How many pixels of the rectangle around ([cx], [cy]) differ between two pictures. */
    private fun changed(a: Bitmap, b: Bitmap, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        var n = 0
        for (x in x0.coerceAtLeast(0)..x1.coerceAtMost(a.width - 1)) for (y in y0.coerceAtLeast(0)..y1.coerceAtMost(a.height - 1)) if (a.getPixel(x, y) != b.getPixel(x, y)) n++
        return n
    }

    @Test fun aKeyUnderTheFingerIsPaintedLargeAboveItAfterTheLightFlashHasGone() {
        val before = render()
        val (x, y) = nav.keyCenterForTest("h")!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, x, y)
        idle(400) // the 130 ms light-up of the key itself is over: only the bubble can still differ
        val after = render()
        val kb = nav.keysViewForTest().height
        // the bubble sits right above the key's top edge, over the row above it
        val n = changed(before, after, (x - 40).toInt(), (y - 80).toInt(), (x + 40).toInt(), (y - 22).toInt())
        assertTrue("the bubble is painted above the key ($n pixels differ, keys view height $kb, key at $x,$y)", n > 60)
        nav.touchKeysForTest(MotionEvent.ACTION_UP, x, y); idle(60)
        val gone = render()
        assertTrue("it is gone once the finger lifts (${changed(before, gone, (x - 40).toInt(), (y - 80).toInt(), (x + 40).toInt(), (y - 22).toInt())} pixels differ)", changed(before, gone, (x - 40).toInt(), (y - 80).toInt(), (x + 40).toInt(), (y - 22).toInt()) < 20)
    }

    @Test fun aHeldTopRowLetterShowsItsDigitLargeOverTheStrip() {
        val before = render()
        val (x, y) = nav.keyCenterForTest("e")!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, x, y)
        idle(700)
        val after = render()
        val n = changed(before, after, (x - 40).toInt(), 0, (x + 40).toInt(), 36)
        assertTrue("the digit bubble is painted over the strip above the key ($n pixels differ)", n > 40)
        nav.touchKeysForTest(MotionEvent.ACTION_UP, x, y)
    }
}
