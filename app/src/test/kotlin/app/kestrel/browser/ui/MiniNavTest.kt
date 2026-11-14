package app.kestrel.browser.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** The one-hand mini navigation panel (AR2): toggle and slide, the buttons, the bar, the small keyboard, the light-up. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MiniNavTest {
    private class Rec : MiniNav.Listener {
        val calls = ArrayList<String>()
        val opened = ArrayList<Boolean>()
        override fun navBack() { calls.add("back") }
        override fun navHome() { calls.add("home") }
        override fun navForward() { calls.add("forward") }
        override fun navSubmit(text: String) { calls.add("go:$text") }
        override fun navOpenChanged(open: Boolean) { opened.add(open) }
    }

    private lateinit var activity: Activity
    private lateinit var frame: FrameLayout
    private lateinit var nav: MiniNav
    private val rec = Rec()
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Before fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        frame = FrameLayout(activity)
        activity.setContentView(frame)
        nav = MiniNav(activity, Pal(activity), rec)
        frame.addView(nav, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))
        relayout()
    }

    /** The system's "Animator duration scale" (developer options) for this process: hidden API, so by reflection; the owner runs with it at 0. */
    private fun animatorScale(v: Float) {
        try { ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, v) } catch (e: Exception) { throw AssertionError("cannot set the animator scale: $e") }
    }

    @After fun resetAnimatorScale() { animatorScale(1f) }

    private fun relayout() {
        frame.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
        frame.layout(0, 0, 600, 900)
    }

    /** The slide's clock and frames, stepped by hand (Robolectric's own Choreographer jumps the clock). */
    private var now = 1000L
    private var pending: Runnable? = null
    private fun stepBy(ms: Long) { now += ms; val r = pending ?: return; pending = null; r.run() }
    private fun manualSlide() { nav.clock = { now }; nav.nextFrame = { pending = it } }

    @Test fun theToggleSlidesThePanelDownAndBackUpOnItsOwnClock() {
        animatorScale(0f) // the owner runs with animations OFF: a system animator would jump to its end at once
        manualSlide()
        assertFalse(nav.isOpen); assertFalse(nav.panelShownForTest)
        nav.pressToggleForTest()
        assertTrue(nav.isOpen); assertTrue(nav.panelShownForTest)
        val hidden = nav.panelOffsetForTest
        assertTrue("starts above the screen ($hidden)", hidden < 0f)
        stepBy(0); assertEquals("first frame: still hidden", hidden, nav.panelOffsetForTest, 0.5f)
        stepBy(60); val a = nav.panelOffsetForTest
        assertTrue("on its way ($a between $hidden and 0)", a > hidden && a < 0f)
        stepBy(60); val b = nav.panelOffsetForTest
        assertTrue("further on ($b after $a)", b > a && b < 0f)
        stepBy(200); assertEquals("arrived", 0f, nav.panelOffsetForTest, 0.5f)
        assertNull("no more frames wanted", pending)
        assertEquals(listOf(true), rec.opened)
        nav.pressToggleForTest()
        stepBy(0); stepBy(80); assertTrue("sliding back up", nav.panelOffsetForTest < 0f && nav.panelShownForTest)
        stepBy(200); assertFalse("hidden again", nav.panelShownForTest)
        assertEquals(listOf(true, false), rec.opened)
    }

    @Test fun theSlideHasItsOwnClockWhateverAndroidsAnimationScaleIs() {
        animatorScale(0f)
        nav.pressToggleForTest() // the real Choreographer this time
        assertTrue("a frame is wanted: the slide is not skipped when animations are off", nav.panelShownForTest && nav.panelOffsetForTest < 0f)
        idle(600)
        assertEquals(0f, nav.panelOffsetForTest, 0.5f)
    }

    @Test fun startingOpenOrClosedNeedsNoAnimation() {
        nav.setOpen(true, animate = false, notify = false)
        assertTrue(nav.isOpen && nav.panelShownForTest); assertEquals(0f, nav.panelOffsetForTest, 0f)
        assertTrue("restoring the remembered state is not a new choice", rec.opened.isEmpty())
        nav.setOpen(false, animate = false, notify = false)
        assertFalse(nav.panelShownForTest)
    }

    @Test fun theThreeButtonsDoWhatTheDockButtonsDo() {
        nav.setOpen(true, animate = false); relayout()
        nav.tapNavForTest("back"); nav.tapNavForTest("home"); nav.tapNavForTest("forward")
        assertEquals(listOf("back", "home", "forward"), rec.calls)
    }

    @Test fun aPressedButtonLightsUpForAMomentOnly() {
        nav.setOpen(true, animate = false); relayout()
        assertFalse(nav.backLit())
        nav.tapNavForTest("back")
        assertTrue("lit at once", nav.backLit())
        idle(100); assertTrue("still lit a moment later", nav.backLit())
        idle(60); assertFalse("and gone: a flash, not a state", nav.backLit())
        assertEquals(130L, MiniNav.FLASH_MS)
    }

    @Test fun buttonsDimWhereThereIsNothingToGoBackOrForwardTo() {
        nav.sync(canBack = false, canForward = false, homeShown = false, address = "")
        assertTrue(nav.backDim() && nav.forwardDim() && !nav.homeActive())
        nav.sync(canBack = true, canForward = true, homeShown = true, address = "https://a.example/")
        assertFalse(nav.backDim() || nav.forwardDim()); assertTrue(nav.homeActive())
    }

    @Test fun theBarSelectsTheWholeAddressAndOpensTheKeyboardAndTheFirstKeyReplacesIt() {
        nav.setOpen(true, animate = false); relayout()
        nav.sync(true, false, false, "https://doraby.com/movie/")
        assertEquals("what shows while the keyboard is shut: the address, tidied", "doraby.com/movie", nav.barTextForTest)
        nav.pressBarForTest()
        assertTrue(nav.keyboardShowing); assertTrue("everything selected", nav.selectedForTest)
        assertEquals("https://doraby.com/movie/", nav.barTextForTest)
        for (k in listOf("n", "e", "w")) assertTrue(nav.pressKeyForTest(k))
        assertEquals("new", nav.barTextForTest); assertFalse(nav.selectedForTest)
        assertTrue(nav.pressKeyForTest(".com")); assertEquals("new.com", nav.barTextForTest)
        assertTrue(nav.pressKeyForTest("⌫")); assertEquals("new.co", nav.barTextForTest)
        assertTrue(nav.pressKeyForTest("m")); assertTrue(nav.pressKeyForTest("Go"))
        assertEquals(listOf("go:new.com"), rec.calls)
        assertFalse("Go closes the keyboard", nav.keyboardShowing)
    }

    @Test fun shiftAndTheSymbolPage() {
        nav.setOpen(true, animate = false); relayout()
        nav.pressBarForTest()
        assertTrue(nav.pressKeyForTest("⇧")); assertTrue("the keys are upper case now", nav.pressKeyForTest("Q"))
        assertTrue(nav.pressKeyForTest("123")); assertTrue(nav.pressKeyForTest("7")); assertTrue(nav.pressKeyForTest("?"))
        assertTrue(nav.pressKeyForTest("abc")); assertTrue(nav.pressKeyForTest("q"))
        assertEquals("Q7?q", nav.barTextForTest)
    }

    @Test fun theBackKeyClosesTheKeyboardBeforeAnythingElse() {
        nav.setOpen(true, animate = false); relayout()
        assertFalse("nothing to close yet", nav.onBackPressed())
        nav.pressBarForTest(); assertTrue(nav.keyboardShowing)
        assertTrue(nav.onBackPressed()); assertFalse(nav.keyboardShowing)
        assertFalse(nav.onBackPressed())
    }

    @Test fun pressingTheBarAgainClosesTheKeyboardAndHidingThePanelDoesToo() {
        nav.setOpen(true, animate = false); relayout()
        nav.pressBarForTest(); nav.pressBarForTest(); assertFalse(nav.keyboardShowing)
        nav.pressBarForTest(); assertTrue(nav.keyboardShowing)
        nav.setOpen(false, animate = false)
        assertFalse("a hidden panel has no keyboard up", nav.keyboardShowing)
    }

    @Test fun anEmptyBarOpensWithNothingSelectedAndGoWithNothingTypedDoesNothing() {
        nav.setOpen(true, animate = false); relayout()
        nav.sync(false, false, true, "")
        nav.pressBarForTest()
        assertFalse(nav.selectedForTest)
        nav.pressKeyForTest("Go")
        assertTrue(rec.calls.isEmpty()); assertTrue(nav.keyboardShowing)
    }

    @Test fun whenTheFeatureIsUnavailableNothingShows() {
        nav.available = false
        assertEquals(View.GONE, nav.visibility)
        nav.available = true
        assertEquals(View.VISIBLE, nav.visibility)
    }

    @Test fun theBarDoesNotCatchTouchesOutsideItsViews() {
        // the panel is small and sits in the corner; the rest of MiniNav's area must stay the page's
        assertFalse("MiniNav itself is not clickable", nav.isClickable)
    }
}
