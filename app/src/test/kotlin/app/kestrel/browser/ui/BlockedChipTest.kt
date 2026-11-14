package app.kestrel.browser.ui

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** The one-second icon for a blocked page (AR1). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BlockedChipTest {
    private val ctx get() = RuntimeEnvironment.getApplication()
    private val pal by lazy { Pal(ctx) }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test fun theIconStaysForOneSecondAfterTheBlockAndThenGoes() {
        val chip = BlockedChip(ctx)
        assertFalse(chip.showing)
        chip.show(pal, "https://spam.example/x") {}
        assertTrue(chip.showing)
        idle(900); assertTrue("still there before the second is up", chip.showing)
        idle(150); assertFalse("gone after one second", chip.showing)
        assertNull(chip.pendingUrl)
    }

    @Test fun aSecondBlockInsideTheSecondKeepsTheIconAndTheNewestAddressWins() {
        val chip = BlockedChip(ctx); val opened = ArrayList<String>()
        chip.show(pal, "https://one.example/") { opened.add(it) }
        idle(700)
        chip.show(pal, "https://two.example/") { opened.add(it) }
        idle(700); assertTrue("700 ms after the second block: the clock started again", chip.showing)
        assertEquals("https://two.example/", chip.pendingUrl)
        chip.performClick()
        assertEquals(listOf("https://two.example/"), opened)
        assertFalse(chip.showing)
    }

    @Test fun aPressOpensTheAddressOnceAndTheIconIsGone() {
        val chip = BlockedChip(ctx); val opened = ArrayList<String>()
        chip.show(pal, "https://spam.example/ad?x=1") { opened.add(it) }
        chip.performClick()
        assertEquals(listOf("https://spam.example/ad?x=1"), opened)
        assertFalse(chip.showing)
        chip.performClick()
        idle(1500)
        assertEquals("a second press (or the old timer) opens nothing more", 1, opened.size)
    }

    @Test fun anAddressThatCannotBeOpenedStillShowsTheIconButOpensNothing() {
        val chip = BlockedChip(ctx); val opened = ArrayList<String>()
        chip.show(pal, "javascript:alert(1)") { opened.add(it) }
        assertTrue(chip.showing); assertNull(chip.pendingUrl)
        chip.performClick()
        assertTrue(opened.isEmpty()); assertFalse(chip.showing)
        chip.show(pal, null) { opened.add(it) }
        assertTrue(chip.showing); chip.performClick(); assertTrue(opened.isEmpty())
    }

    @Test fun theWholeLifeIsOneSecond() {
        assertEquals(1000L, BlockedChip.LIFETIME_MS)
        assertTrue("the part that takes a press is bigger than the icon", BlockedChip.TOUCH_DP > 28)
    }
}
