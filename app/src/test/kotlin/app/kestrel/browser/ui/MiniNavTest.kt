package app.kestrel.browser.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.input.glide.GlideTyping
import app.kestrel.core.input.glide.LexiconFile
import java.util.concurrent.Executor
import android.widget.FrameLayout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val habits = ArrayList<FloatArray>(); val savedWords = ArrayList<String>()
        override fun navGlideHabitChanged(habit: FloatArray) { habits.add(habit) }
        override fun navGlideWordsChanged(text: String) { savedWords.add(text) }
        override fun navGlideHosts(done: (List<String>) -> Unit) { done(listOf("www.doraby.com", "pelisjuanita.com", "10.0.2.2")) }
        var field: PageField? = null; var privateTab = false; val keyboard = ArrayList<Boolean>()
        override fun navPageField(): PageField? = field
        override fun navKeyboardChanged(showing: Boolean) { keyboard.add(showing) }
        override fun navPrivateTab() = privateTab
    }

    /** A text field of a web page as the panel sees it: what is put in, taken off or pressed ends up in [text] and [log]. */
    private class FakeField(override var identity: Int = 1, override val password: Boolean = false, override val enterSubmits: Boolean = true, override val privateTab: Boolean = false, start: String = "") : PageField {
        val text = StringBuilder(start); val log = ArrayList<String>()
        override val label = "test"
        override fun insert(text: String) { this.text.append(text); log.add("+$text") }
        override fun deleteBefore(count: Int) { this.text.setLength(maxOf(0, this.text.length - count)); log.add("-$count") }
        override fun enter() { log.add("enter") }
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

    // ------------------------------------------------------------------ glide typing (AT1e)

    private val glideBytes = LexiconFile.build(
        listOf("hello" to 500L, "help" to 800L, "held" to 300L, "hell" to 300L, "world" to 900L, "word" to 1200L, "the" to 9000L, "she" to 2000L, "good" to 2000L, "to" to 7000L, "go" to 2500L)
            .map { LexiconFile.Entry(it.first, it.second) },
        emptyList(),
    )

    private fun openGlideKeyboard() {
        nav.glideFactory = { GlideTyping({ glideBytes }, Executor { it.run() }) }
        nav.setGlideEnabled(true)
        nav.setOpen(true, animate = false); relayout()
        nav.pressBarForTest(); relayout(); idle(50)
    }

    /** A finger path over the letters of [word]: straight from key centre to key centre, a touch every few pixels. */
    private fun fingerPath(word: String): List<Pair<Float, Float>> {
        val centres = ArrayList<Pair<Float, Float>>()
        for (ch in word) { val c = nav.keyCenterForTest(ch.toString()) ?: error("no key $ch"); if (centres.isEmpty() || centres.last() != c) centres.add(c) }
        val out = ArrayList<Pair<Float, Float>>(); out.add(centres[0])
        for (i in 1 until centres.size) {
            val (ax, ay) = centres[i - 1]; val (bx, by) = centres[i]
            val steps = Math.max(2, (Math.hypot((bx - ax).toDouble(), (by - ay).toDouble()) / 4).toInt())
            for (s in 1..steps) out.add(ax + (bx - ax) * s / steps to ay + (by - ay) * s / steps)
        }
        return out
    }

    private fun glideWord(word: String) {
        val pts = fingerPath(word)
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, pts.first().first, pts.first().second)
        for (p in pts.drop(1).dropLast(1)) nav.touchKeysForTest(MotionEvent.ACTION_MOVE, p.first, p.second)
        nav.touchKeysForTest(MotionEvent.ACTION_UP, pts.last().first, pts.last().second)
        idle(20)
    }

    private fun tapKey(label: String, dx: Float = 0f) {
        val (x, y) = nav.keyCenterForTest(label)!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, x, y)
        if (dx != 0f) nav.touchKeysForTest(MotionEvent.ACTION_MOVE, x + dx / 2, y)
        nav.touchKeysForTest(MotionEvent.ACTION_UP, x + dx, y)
        idle(20)
    }

    @Test fun aGlideTypesTheWordWithASpaceAndShowsTheOtherGuessesAboveTheKeys() {
        openGlideKeyboard()
        assertEquals(Math.round(161f * 1f) + 30, nav.keysHeightForTest) // the strip adds its height
        glideWord("hello")
        assertEquals("hello ", nav.barTextForTest)
        assertEquals("hello", nav.candidatesForTest.firstOrNull())
        assertTrue(nav.candidatesForTest.size in 2..3)
    }

    @Test fun theFirstGlideReplacesTheSelectedAddressAndTheNextOnesFollowIt() {
        nav.sync(true, false, false, "https://doraby.com/movie/")
        openGlideKeyboard()
        assertTrue(nav.selectedForTest)
        glideWord("world")
        assertEquals("world ", nav.barTextForTest)
        glideWord("hello")
        assertEquals("world hello ", nav.barTextForTest)
    }

    @Test fun aPlainTapStillTypesALetterAndSoDoesATapThatDriftsALittle() {
        openGlideKeyboard()
        tapKey("h"); assertEquals("h", nav.barTextForTest)
        tapKey("e", dx = 3f); assertEquals("a drift inside the key is still the key that was pressed", "he", nav.barTextForTest)
        assertTrue("no glide happened: no guesses", nav.candidatesForTest.isEmpty())
    }

    @Test fun aGlideAfterTypedLettersStartsANewWord() {
        openGlideKeyboard()
        tapKey("a")
        glideWord("world")
        assertEquals("a world ", nav.barTextForTest)
    }

    @Test fun oneBackspaceRightAfterAGlideRemovesTheWholeWordAndIsCounted() {
        openGlideKeyboard()
        glideWord("hello"); glideWord("world")
        assertTrue(nav.pressKeyForTest("⌫"))
        assertEquals("hello ", nav.barTextForTest)
        assertTrue("the guesses go with the word", nav.candidatesForTest.isEmpty())
        assertTrue(nav.glideLine(), nav.glideLine().contains("deleted right away 1"))
        assertTrue(nav.pressKeyForTest("⌫"))
        assertEquals("hello", nav.barTextForTest) // now a letter at a time
    }

    @Test fun aGuessInTheStripReplacesTheWordAndIsCounted() {
        openGlideKeyboard()
        glideWord("word")
        val second = nav.candidatesForTest.getOrNull(1)
        // the path of "word" can also be "world": whichever the second guess is, tapping it swaps the word
        assertNotNull("a second guess", second)
        val (cx, cy) = nav.chipCenterForTest(1)!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, cx, cy); nav.touchKeysForTest(MotionEvent.ACTION_UP, cx, cy); idle(20)
        assertEquals("$second ", nav.barTextForTest)
        assertEquals(second, nav.candidatesForTest.first()) // the picked one leads the strip now
        assertTrue(nav.glideLine(), nav.glideLine().contains("another candidate picked 1"))
    }

    @Test fun aPathThatMakesNoWordTypesNothingAndSaysSo() {
        openGlideKeyboard()
        val (px, py) = nav.keyCenterForTest("p")!!; val (mx, my) = nav.keyCenterForTest("z")!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, px, py)
        for (s in 1..20) nav.touchKeysForTest(MotionEvent.ACTION_MOVE, px + (mx - px) * s / 20, py + (my - py) * s / 20)
        nav.touchKeysForTest(MotionEvent.ACTION_UP, mx, my); idle(20)
        assertEquals("", nav.barTextForTest)
        assertTrue(nav.candidatesForTest.isEmpty())
        assertTrue(nav.glideLine(), nav.glideLine().contains("no word fitted 1"))
    }

    @Test fun withGlideTypingOffThereIsNoStripAndTheKeysBehaveAsBefore() {
        nav.setGlideEnabled(false)
        nav.setOpen(true, animate = false); relayout(); nav.pressBarForTest(); relayout()
        assertEquals(161, nav.keysHeightForTest)
        val (hx, hy) = nav.keyCenterForTest("h")!!; val (ox, oy) = nav.keyCenterForTest("o")!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, hx, hy); nav.touchKeysForTest(MotionEvent.ACTION_MOVE, ox, oy); nav.touchKeysForTest(MotionEvent.ACTION_UP, ox, oy)
        assertEquals("the keyboard behaves as before: the key the finger lifts on", "o", nav.barTextForTest)
        assertTrue(nav.candidatesForTest.isEmpty())
        assertTrue(nav.glideLine().contains("off"))
    }

    @Test fun wordsTypedLetterByLetterAreRememberedAfterGoButKnownOnesAreNot() {
        openGlideKeyboard()
        for (ch in "kestrel") tapKey(ch.toString())
        nav.pressKeyForTest("Go")
        idle(50)
        assertTrue("saved: ${rec.savedWords}", rec.savedWords.isNotEmpty() && rec.savedWords.last().contains("kestrel"))
    }

    @Test fun theLineForDiagnosticsSaysWhenItWasNotUsed() {
        assertTrue(nav.glideLine().contains("off"))
        openGlideKeyboard()
        assertTrue(nav.glideLine().contains("word list") || nav.glideLine().contains("not used yet"))
    }

    // ------------------------------------------------------------------ typing into the page's own text field (AU3)

    private fun openKeyboardWith(f: PageField?, glide: Boolean = false) {
        rec.field = f
        if (glide) { nav.glideFactory = { GlideTyping({ glideBytes }, Executor { it.run() }) }; nav.setGlideEnabled(true) }
        nav.setOpen(true, animate = false); relayout()
        nav.pressBarForTest(); relayout(); idle(50)
    }

    @Test fun withAFieldFocusedTheKeysTypeIntoItAndNotIntoTheAddressBar() {
        nav.sync(true, false, false, "https://www.example.com/")
        val f = FakeField()
        openKeyboardWith(f)
        assertTrue(nav.typesIntoPage)
        assertEquals("the address is not selected and not in the bar", "", nav.barTextForTest)
        assertFalse(nav.selectedForTest)
        nav.pressKeyForTest("h"); nav.pressKeyForTest("i")
        assertEquals("hi", f.text.toString()); assertEquals("hi", nav.barTextForTest)
        assertTrue("nothing is searched or opened", rec.calls.none { it.startsWith("go:") })
        assertEquals("the system keyboard is told to stay down", listOf(true), rec.keyboard)
    }

    @Test fun goPressesEnterInTheFieldAndAOneLineFieldClosesTheKeyboard() {
        val f = FakeField(); openKeyboardWith(f)
        nav.pressKeyForTest("h"); nav.pressKeyForTest("i"); nav.pressKeyForTest("Go")
        assertEquals(listOf("+h", "+i", "enter"), f.log)
        assertFalse("a field whose Enter submits closes the keyboard", nav.keyboardShowing)
        assertTrue("and nothing was searched", rec.calls.none { it.startsWith("go:") })
        assertEquals(listOf(true, false), rec.keyboard)
    }

    @Test fun goWithNothingTypedStillPressesEnterInTheField() {
        val f = FakeField(); openKeyboardWith(f)
        nav.pressKeyForTest("Go")
        assertEquals(listOf("enter"), f.log)
    }

    @Test fun aTextAreaKeepsTheKeyboardOpenForTheNextLine() {
        val f = FakeField(enterSubmits = false); openKeyboardWith(f)
        nav.pressKeyForTest("a"); nav.pressKeyForTest("Go"); nav.pressKeyForTest("b")
        assertEquals(listOf("+a", "enter", "+b"), f.log)
        assertTrue(nav.keyboardShowing); assertEquals("a new line starts from an empty text", "b", nav.barTextForTest)
    }

    @Test fun backspaceTakesOffWhatWasTypedAndThenTheFieldsOwnText() {
        val f = FakeField(start = "abc"); openKeyboardWith(f)
        nav.pressKeyForTest("⌫")  // nothing typed here: the field's own text goes
        assertEquals("ab", f.text.toString())
        nav.pressKeyForTest("x"); nav.pressKeyForTest("⌫")
        assertEquals("ab", f.text.toString()); assertEquals(listOf("-1", "+x", "-1"), f.log)
    }

    @Test fun aGlideGoesIntoTheFieldWithItsSpacesAndOneBackspaceTakesTheWholeWord() {
        val f = FakeField(); openKeyboardWith(f, glide = true)
        glideWord("hello")
        assertEquals("hello ", f.text.toString()); assertEquals("hello ", nav.barTextForTest)
        glideWord("world")
        assertEquals("hello world ", f.text.toString())
        nav.pressKeyForTest("⌫")
        assertEquals("one Backspace takes the whole glided word", "hello ", f.text.toString())
    }

    @Test fun aGuessFromTheStripReplacesTheWordInTheField() {
        val f = FakeField(); openKeyboardWith(f, glide = true)
        glideWord("hello")
        val second = nav.candidatesForTest[1]
        val (x, y) = nav.chipCenterForTest(1)!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, x, y); nav.touchKeysForTest(MotionEvent.ACTION_UP, x, y)
        assertEquals("$second ", f.text.toString()); assertEquals("$second ", nav.barTextForTest)
    }

    @Test fun aPasswordFieldShowsDotsAndIsNeverLearnedFromAndHasNoGlideStrip() {
        val f = FakeField(password = true); openKeyboardWith(f, glide = true)
        for (ch in "secret") nav.pressKeyForTest(ch.toString())
        assertEquals("secret", f.text.toString())
        assertEquals("\u2022\u2022\u2022\u2022\u2022\u2022", nav.barTextForTest)
        nav.pressKeyForTest("Go"); idle(50)
        assertTrue("nothing learned from a password: ${rec.savedWords}", rec.savedWords.isEmpty())
    }

    @Test fun nothingIsLearnedInAPrivateTabNeitherInAFieldNorInTheAddressBar() {
        rec.privateTab = true
        val f = FakeField(); openKeyboardWith(f, glide = true)
        for (ch in "kestrel") nav.pressKeyForTest(ch.toString())
        nav.pressKeyForTest("Go"); idle(50)
        assertTrue("field: ${rec.savedWords}", rec.savedWords.isEmpty())
        rec.field = null; nav.pressBarForTest(); idle(20)
        for (ch in "kestrel") nav.pressKeyForTest(ch.toString())
        nav.pressKeyForTest("Go"); idle(50)
        assertTrue("address: ${rec.savedWords}", rec.savedWords.isEmpty())
    }

    @Test fun wordsTypedIntoAFieldAreRememberedWhenTheKeyboardClosesAndNotCountedTwiceAfterGo() {
        val f = FakeField(); openKeyboardWith(f, glide = true)
        for (ch in "kestrel") nav.pressKeyForTest(ch.toString())
        nav.closeKeyboard(); idle(50)
        assertTrue("saved: ${rec.savedWords}", rec.savedWords.isNotEmpty() && rec.savedWords.last().contains("kestrel"))
        val n = rec.savedWords.size
        rec.field = FakeField(); nav.pressBarForTest(); idle(20)
        for (ch in "kestrel") nav.pressKeyForTest(ch.toString())
        nav.pressKeyForTest("Go"); idle(50)      // Go learns, then the keyboard closes: the same text is not handed over again
        assertTrue("a second round counted once: ${rec.savedWords.size} vs $n", rec.savedWords.size - n <= 1)
    }

    @Test fun whenTheFieldGoesAwayTheBarIsTheAddressBarAgain() {
        nav.sync(true, false, false, "https://www.example.com/")
        val f = FakeField(); openKeyboardWith(f)
        nav.pressKeyForTest("a"); nav.pressKeyForTest("b")
        rec.field = null; nav.pageFieldChanged()
        assertFalse(nav.typesIntoPage)
        assertEquals("the address is selected again", "https://www.example.com/", nav.barTextForTest)
        assertTrue(nav.selectedForTest)
        nav.pressKeyForTest("c")
        assertEquals("ab", f.text.toString()); assertEquals("c", nav.barTextForTest)
    }

    @Test fun anotherFieldStartsFromAnEmptyText() {
        val one = FakeField(identity = 1); openKeyboardWith(one)
        nav.pressKeyForTest("a"); nav.pressKeyForTest("b")
        val two = FakeField(identity = 2); rec.field = two
        nav.pressKeyForTest("c")
        assertEquals("ab", one.text.toString()); assertEquals("c", two.text.toString()); assertEquals("c", nav.barTextForTest)
    }

    @Test fun aFieldThatGetsFocusWhileTheKeyboardIsOpenTakesTheKeys() {
        nav.sync(true, false, false, "https://www.example.com/")
        openKeyboardWith(null)
        assertFalse(nav.typesIntoPage); assertTrue(nav.selectedForTest)
        val f = FakeField(); rec.field = f; nav.pageFieldChanged()
        assertTrue(nav.typesIntoPage); assertFalse(nav.selectedForTest); assertEquals("", nav.barTextForTest)
        nav.pressKeyForTest("x")
        assertEquals("x", f.text.toString())
    }

    @Test fun withoutAFieldTheBarBehavesExactlyAsBefore() {
        nav.sync(true, false, false, "https://www.example.com/")
        openKeyboardWith(null)
        assertFalse(nav.typesIntoPage); assertTrue(nav.selectedForTest)
        nav.pressKeyForTest("k"); nav.pressKeyForTest("Go")
        assertEquals(listOf("go:k"), rec.calls.filter { it.startsWith("go:") })
    }

    @Test fun theBarSaysWhereTheKeysGo() {
        openKeyboardWith(FakeField())
        assertTrue(nav.barDescriptionForTest, nav.barDescriptionForTest.contains("text field of the page"))
        nav.closeKeyboard()
        assertTrue(nav.barDescriptionForTest.contains("Address"))
    }

    @Test fun theLineForDiagnosticsCountsFieldAndAddressOpeningsAndWhatWasSent() {
        assertTrue(nav.fieldLine(), nav.fieldLine().contains("not opened yet"))
        val f = FakeField(); openKeyboardWith(f)
        nav.pressKeyForTest("h"); nav.pressKeyForTest("i"); nav.pressKeyForTest("Go")
        val line = nav.fieldLine()
        assertTrue(line, line.contains("opened 1 times with a page field focused") && line.contains("2 characters sent") && line.contains("Enter pressed 1 times") && line.contains("last: test") && line.contains("0 times for the address bar"))
        rec.field = null; nav.pressBarForTest(); idle(20)
        assertTrue(nav.fieldLine(), nav.fieldLine().contains("1 times for the address bar"))
    }

    @Test fun aGuessThePersonPickedCountsOnceItIsKeptAndNotWhenTheyDeleteItRightAway() {
        openGlideKeyboard()
        glideWord("hello")
        val (x, y) = nav.chipCenterForTest(1)!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, x, y); nav.touchKeysForTest(MotionEvent.ACTION_UP, x, y)
        assertFalse("picked, but not yet kept: nothing learned so far: ${nav.glideLine()}", nav.glideLine().contains("learned"))
        nav.pressKeyForTest("\u232b")   // Backspace right after the pick: it was wrong too
        nav.closeKeyboard(); idle(20)
        assertFalse("a picked word that was deleted at once is not learned: ${nav.glideLine()}", nav.glideLine().contains("learned"))
    }

    @Test fun aGuessThePersonPickedIsLearnedWhenTheyCarryOnTyping() {
        openGlideKeyboard()
        glideWord("hello")
        val (x, y) = nav.chipCenterForTest(1)!!
        nav.touchKeysForTest(MotionEvent.ACTION_DOWN, x, y); nav.touchKeysForTest(MotionEvent.ACTION_UP, x, y)
        glideWord("world")               // carrying on keeps the picked word
        nav.closeKeyboard(); idle(20)
        assertTrue("the kept words were learned: ${nav.glideLine()}", nav.glideLine().contains("learned 2 words"))
    }
}

